package com.mhub.erp.service;

import com.mhub.common.exception.BusinessException;
import com.mhub.common.exception.ErrorCodes;
import com.mhub.core.domain.entity.*;
import com.mhub.core.domain.enums.ErpDocumentStatus;
import com.mhub.core.domain.enums.OrderStatus;
import com.mhub.core.domain.repository.*;
import com.mhub.core.erp.dto.ErpBatchSendResult;
import com.mhub.core.erp.dto.ErpSalesDocumentResponse;
import com.mhub.core.service.ErpDocumentGenerator;
import com.mhub.core.tenant.TenantContext;
import com.mhub.erp.adapter.ErpAdapter;
import com.mhub.erp.adapter.ecount.ECountAdapter;
import com.mhub.erp.adapter.ecount.ECountSalesDocumentBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ErpSalesDocumentService implements ErpDocumentGenerator {

    private final ErpSalesDocumentRepository documentRepository;
    private final OrderRepository orderRepository;
    private final OrderSettlementRepository orderSettlementRepository;
    private final TenantErpConfigRepository erpConfigRepository;
    private final ErpSalesTemplateRepository erpSalesTemplateRepository;
    private final ECountSalesDocumentBuilder documentBuilder;
    private final ErpAdapterFactory erpAdapterFactory;

    /**
     * 전표 생성 (출고 완료 시 호출)
     */
    @Transactional
    public ErpSalesDocument generateDocument(UUID orderId) {
        UUID tenantId = TenantContext.requireTenantId();

        // 이미 활성 전표가 있는지 확인
        if (documentRepository.existsActiveByOrderId(orderId)) {
            log.debug("Active document already exists for order {}", orderId);
            return documentRepository.findActiveByOrderId(orderId).orElseThrow();
        }

        Order order = orderRepository.findByIdWithItems(orderId)
                .filter(o -> o.getTenantId().equals(tenantId))
                .orElseThrow(() -> new BusinessException(ErrorCodes.ORDER_NOT_FOUND, "주문을 찾을 수 없습니다"));

        TenantErpConfig config = getActiveErpConfig(tenantId);
        ErpSalesTemplate template = getActiveTemplate(tenantId, config.getId());
        List<OrderSettlement> settlements = orderSettlementRepository.findByOrderId(orderId);

        // ECount 전표 라인 생성
        Map<String, Object> requestBody = documentBuilder.buildSaveSaleRequest(order, settlements, template);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> saleList = (List<Map<String, Object>>) requestBody.get("SaleList");

        // BulkDatas 내용만 추출하여 documentLines에 저장 (UI 표시용)
        List<Map<String, Object>> documentLines = new ArrayList<>();
        if (saleList != null) {
            for (Map<String, Object> wrapper : saleList) {
                @SuppressWarnings("unchecked")
                Map<String, Object> bulkDatas = (Map<String, Object>) wrapper.get("BulkDatas");
                if (bulkDatas != null) {
                    documentLines.add(bulkDatas);
                }
            }
        }

        // 거래처 정보 추출 및 전표 라인 총액 계산
        String customerCode = null;
        String customerName = null;
        BigDecimal totalAmount = BigDecimal.ZERO;
        if (!documentLines.isEmpty()) {
            Map<String, Object> firstLine = documentLines.get(0);
            customerCode = (String) firstLine.get("CUST");
            customerName = (String) firstLine.get("CUST_DES");

            // 전표 라인의 PRICE 합계 계산
            for (Map<String, Object> line : documentLines) {
                Object priceObj = line.get("PRICE");
                if (priceObj != null) {
                    try {
                        BigDecimal price = new BigDecimal(priceObj.toString());
                        totalAmount = totalAmount.add(price);
                    } catch (NumberFormatException ignored) {
                        // 숫자가 아닌 경우 무시
                    }
                }
            }
        }

        ErpSalesDocument document = ErpSalesDocument.builder()
                .tenantId(tenantId)
                .orderId(orderId)
                .erpConfigId(config.getId())
                .status(ErpDocumentStatus.PENDING)
                .documentDate(order.getOrderedAt() != null ? order.getOrderedAt().toLocalDate() : LocalDate.now())
                .marketplaceType(order.getMarketplaceType())
                .customerCode(customerCode)
                .customerName(customerName)
                .totalAmount(totalAmount)
                .documentLines(documentLines)
                .build();

        document = documentRepository.save(document);
        log.info("Generated ERP document {} for order {}", document.getId(), orderId);

        return document;
    }

    /**
     * 전표 목록 조회
     */
    @Transactional(readOnly = true)
    public Page<ErpSalesDocumentResponse> getDocuments(ErpDocumentStatus status, int page, int size) {
        UUID tenantId = TenantContext.requireTenantId();
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<ErpSalesDocument> documents;
        if (status != null) {
            documents = documentRepository.findByTenantIdAndStatus(tenantId, status, pageable);
        } else {
            documents = documentRepository.findByTenantId(tenantId, pageable);
        }

        // 주문 정보 조회
        List<UUID> orderIds = documents.getContent().stream()
                .map(ErpSalesDocument::getOrderId)
                .distinct()
                .toList();
        Map<UUID, Order> orderMap = orderRepository.findAllById(orderIds).stream()
                .collect(Collectors.toMap(Order::getId, Function.identity()));

        return documents.map(doc -> {
            Order order = orderMap.get(doc.getOrderId());
            String marketplaceOrderId = order != null ? order.getMarketplaceOrderId() : null;
            return ErpSalesDocumentResponse.from(doc, marketplaceOrderId);
        });
    }

    /**
     * 전표 상세 조회
     */
    @Transactional(readOnly = true)
    public ErpSalesDocumentResponse getDocument(UUID documentId) {
        UUID tenantId = TenantContext.requireTenantId();

        ErpSalesDocument doc = documentRepository.findById(documentId)
                .filter(d -> d.getTenantId().equals(tenantId))
                .orElseThrow(() -> new BusinessException(ErrorCodes.ERP_DOCUMENT_NOT_FOUND, "전표를 찾을 수 없습니다"));

        Order order = orderRepository.findById(doc.getOrderId()).orElse(null);
        String marketplaceOrderId = order != null ? order.getMarketplaceOrderId() : null;

        return ErpSalesDocumentResponse.from(doc, marketplaceOrderId);
    }

    /**
     * 전표 삭제 (취소)
     */
    @Transactional
    public void cancelDocument(UUID documentId) {
        UUID tenantId = TenantContext.requireTenantId();

        ErpSalesDocument doc = documentRepository.findById(documentId)
                .filter(d -> d.getTenantId().equals(tenantId))
                .orElseThrow(() -> new BusinessException(ErrorCodes.ERP_DOCUMENT_NOT_FOUND, "전표를 찾을 수 없습니다"));

        if (!doc.canCancel()) {
            throw new BusinessException(ErrorCodes.ERP_DOCUMENT_CANNOT_CANCEL,
                    "전송 완료된 전표는 취소할 수 없습니다");
        }

        doc.cancel();
        documentRepository.save(doc);
        log.info("Cancelled ERP document {}", documentId);
    }

    /**
     * 개별 전표 전송
     */
    @Transactional
    public ErpSalesDocumentResponse sendDocument(UUID documentId) {
        UUID tenantId = TenantContext.requireTenantId();

        ErpSalesDocument doc = documentRepository.findById(documentId)
                .filter(d -> d.getTenantId().equals(tenantId))
                .orElseThrow(() -> new BusinessException(ErrorCodes.ERP_DOCUMENT_NOT_FOUND, "전표를 찾을 수 없습니다"));

        if (!doc.canRetry()) {
            throw new BusinessException(ErrorCodes.ERP_DOCUMENT_ALREADY_SENT,
                    "이미 전송 완료된 전표입니다");
        }

        TenantErpConfig config = erpConfigRepository.findById(doc.getErpConfigId())
                .orElseThrow(() -> new BusinessException(ErrorCodes.ERP_CONFIG_NOT_FOUND, "ERP 설정을 찾을 수 없습니다"));

        try {
            ECountAdapter adapter = (ECountAdapter) erpAdapterFactory.getAdapter(config.getErpType());
            // documentLines를 BulkDatas로 감싸서 ECount API 형식에 맞게 변환
            List<Map<String, Object>> saleList = wrapWithBulkDatas(doc.getDocumentLines());
            Map<String, Object> requestBody = Map.of("SaleList", saleList);
            ErpAdapter.DocumentResult result = adapter.createSaveSale(config, requestBody);

            if (result.success()) {
                doc.markAsSent(result.documentId());

                // Order 엔티티도 업데이트
                orderRepository.findById(doc.getOrderId()).ifPresent(order -> {
                    order.setErpSynced(true);
                    order.setErpDocumentId(result.documentId());
                    orderRepository.save(order);
                });

                log.info("Sent ERP document {} with ERP document ID {}", documentId, result.documentId());
            } else {
                doc.markAsFailed(result.errorMessage());
                log.warn("Failed to send ERP document {}: {}", documentId, result.errorMessage());
            }

            documentRepository.save(doc);

        } catch (Exception e) {
            log.error("Error sending ERP document {}", documentId, e);
            doc.markAsFailed(e.getMessage());
            documentRepository.save(doc);
        }

        Order order = orderRepository.findById(doc.getOrderId()).orElse(null);
        String marketplaceOrderId = order != null ? order.getMarketplaceOrderId() : null;
        return ErpSalesDocumentResponse.from(doc, marketplaceOrderId);
    }

    /**
     * 전표 전송 (내부용 - TenantContext 없이 사용, 배치 처리용)
     */
    private void sendDocumentInternal(ErpSalesDocument doc) {
        if (!doc.canRetry()) {
            log.debug("Document {} already sent or cancelled, skipping", doc.getId());
            return;
        }

        TenantErpConfig config = erpConfigRepository.findById(doc.getErpConfigId())
                .orElseThrow(() -> new BusinessException(ErrorCodes.ERP_CONFIG_NOT_FOUND, "ERP 설정을 찾을 수 없습니다"));

        try {
            ECountAdapter adapter = (ECountAdapter) erpAdapterFactory.getAdapter(config.getErpType());
            List<Map<String, Object>> saleList = wrapWithBulkDatas(doc.getDocumentLines());
            Map<String, Object> requestBody = Map.of("SaleList", saleList);
            ErpAdapter.DocumentResult result = adapter.createSaveSale(config, requestBody);

            if (result.success()) {
                doc.markAsSent(result.documentId());
                orderRepository.findById(doc.getOrderId()).ifPresent(order -> {
                    order.setErpSynced(true);
                    order.setErpDocumentId(result.documentId());
                    orderRepository.save(order);
                });
                log.info("[AutoErpBatch] Sent document {} -> ERP ID {}", doc.getId(), result.documentId());
            } else {
                doc.markAsFailed(result.errorMessage());
                log.warn("[AutoErpBatch] Failed to send document {}: {}", doc.getId(), result.errorMessage());
            }
            documentRepository.save(doc);
        } catch (Exception e) {
            log.error("[AutoErpBatch] Error sending document {}", doc.getId(), e);
            doc.markAsFailed(e.getMessage());
            documentRepository.save(doc);
            throw e;
        }
    }

    /**
     * 일괄 전송
     */
    @Transactional
    public ErpBatchSendResult sendAllPending() {
        UUID tenantId = TenantContext.requireTenantId();

        List<ErpSalesDocument> pendingDocs = documentRepository.findByTenantIdAndStatusIn(
                tenantId, List.of(ErpDocumentStatus.PENDING, ErpDocumentStatus.FAILED));

        if (pendingDocs.isEmpty()) {
            return new ErpBatchSendResult(0, 0, 0, List.of());
        }

        TenantErpConfig config = getActiveErpConfig(tenantId);
        ECountAdapter adapter = (ECountAdapter) erpAdapterFactory.getAdapter(config.getErpType());

        List<ErpBatchSendResult.SendItemResult> results = new ArrayList<>();
        int successCount = 0;
        int failCount = 0;

        for (ErpSalesDocument doc : pendingDocs) {
            try {
                List<Map<String, Object>> saleList = wrapWithBulkDatas(doc.getDocumentLines());
                Map<String, Object> requestBody = Map.of("SaleList", saleList);
                ErpAdapter.DocumentResult result = adapter.createSaveSale(config, requestBody);

                if (result.success()) {
                    doc.markAsSent(result.documentId());

                    // Order 엔티티도 업데이트
                    orderRepository.findById(doc.getOrderId()).ifPresent(order -> {
                        order.setErpSynced(true);
                        order.setErpDocumentId(result.documentId());
                        orderRepository.save(order);
                    });

                    results.add(new ErpBatchSendResult.SendItemResult(
                            doc.getId(), doc.getOrderId(), true, result.documentId(), null));
                    successCount++;
                } else {
                    doc.markAsFailed(result.errorMessage());
                    results.add(new ErpBatchSendResult.SendItemResult(
                            doc.getId(), doc.getOrderId(), false, null, result.errorMessage()));
                    failCount++;
                }

            } catch (Exception e) {
                doc.markAsFailed(e.getMessage());
                results.add(new ErpBatchSendResult.SendItemResult(
                        doc.getId(), doc.getOrderId(), false, null, e.getMessage()));
                failCount++;
            }

            documentRepository.save(doc);
        }

        log.info("Batch send completed: total={}, success={}, fail={}", pendingDocs.size(), successCount, failCount);

        return new ErpBatchSendResult(pendingDocs.size(), successCount, failCount, results);
    }

    /**
     * 선택된 전표 일괄 전송
     */
    @Transactional
    public ErpBatchSendResult sendSelected(List<UUID> documentIds) {
        UUID tenantId = TenantContext.requireTenantId();

        List<ErpSalesDocument> docs = documentRepository.findAllById(documentIds).stream()
                .filter(d -> d.getTenantId().equals(tenantId))
                .filter(ErpSalesDocument::canRetry)
                .toList();

        if (docs.isEmpty()) {
            return new ErpBatchSendResult(0, 0, 0, List.of());
        }

        TenantErpConfig config = getActiveErpConfig(tenantId);
        ECountAdapter adapter = (ECountAdapter) erpAdapterFactory.getAdapter(config.getErpType());

        List<ErpBatchSendResult.SendItemResult> results = new ArrayList<>();
        int successCount = 0;
        int failCount = 0;

        for (ErpSalesDocument doc : docs) {
            try {
                List<Map<String, Object>> saleList = wrapWithBulkDatas(doc.getDocumentLines());
                Map<String, Object> requestBody = Map.of("SaleList", saleList);
                ErpAdapter.DocumentResult result = adapter.createSaveSale(config, requestBody);

                if (result.success()) {
                    doc.markAsSent(result.documentId());

                    orderRepository.findById(doc.getOrderId()).ifPresent(order -> {
                        order.setErpSynced(true);
                        order.setErpDocumentId(result.documentId());
                        orderRepository.save(order);
                    });

                    results.add(new ErpBatchSendResult.SendItemResult(
                            doc.getId(), doc.getOrderId(), true, result.documentId(), null));
                    successCount++;
                } else {
                    doc.markAsFailed(result.errorMessage());
                    results.add(new ErpBatchSendResult.SendItemResult(
                            doc.getId(), doc.getOrderId(), false, null, result.errorMessage()));
                    failCount++;
                }

            } catch (Exception e) {
                doc.markAsFailed(e.getMessage());
                results.add(new ErpBatchSendResult.SendItemResult(
                        doc.getId(), doc.getOrderId(), false, null, e.getMessage()));
                failCount++;
            }

            documentRepository.save(doc);
        }

        log.info("Selected batch send completed: total={}, success={}, fail={}", docs.size(), successCount, failCount);

        return new ErpBatchSendResult(docs.size(), successCount, failCount, results);
    }

    /**
     * 전표 재생성 (기존 취소 후 새로 생성)
     */
    @Transactional
    public ErpSalesDocument regenerateDocument(UUID orderId) {
        UUID tenantId = TenantContext.requireTenantId();

        // 기존 활성 전표가 있으면 취소
        documentRepository.findActiveByOrderId(orderId)
                .filter(d -> d.getTenantId().equals(tenantId))
                .ifPresent(doc -> {
                    if (doc.canCancel()) {
                        doc.cancel();
                        documentRepository.save(doc);
                        log.info("Cancelled existing document {} for regeneration", doc.getId());
                    } else {
                        throw new BusinessException(ErrorCodes.ERP_DOCUMENT_CANNOT_CANCEL,
                                "전송 완료된 전표는 재생성할 수 없습니다");
                    }
                });

        // 새 전표 생성
        return generateDocument(orderId);
    }

    /**
     * 전표 생성 가능 여부 확인
     */
    @Transactional(readOnly = true)
    public boolean shouldGenerateDocument(UUID orderId) {
        UUID tenantId = TenantContext.requireTenantId();

        // 이미 활성 전표가 있는지 확인
        if (documentRepository.existsActiveByOrderId(orderId)) {
            return false;
        }

        // ERP 설정 확인
        List<TenantErpConfig> configs = erpConfigRepository.findByTenantIdAndActiveTrue(tenantId);
        if (configs.isEmpty()) {
            return false;
        }

        // 템플릿 확인
        TenantErpConfig config = configs.get(0);
        Optional<ErpSalesTemplate> template = erpSalesTemplateRepository
                .findByTenantIdAndErpConfigId(tenantId, config.getId());

        return template.isPresent() && template.get().getActive();
    }

    /**
     * 상태별 전표 수 조회
     */
    @Transactional(readOnly = true)
    public Map<String, Long> getDocumentCounts() {
        UUID tenantId = TenantContext.requireTenantId();

        Map<String, Long> counts = new HashMap<>();
        for (ErpDocumentStatus status : ErpDocumentStatus.values()) {
            counts.put(status.name(), documentRepository.countByTenantIdAndStatus(tenantId, status));
        }

        // 전표 미생성 주문 수도 포함
        List<OrderStatus> eligibleStatuses = List.of(OrderStatus.SHIPPING, OrderStatus.DELIVERED);
        counts.put("NEED_DOCUMENT", orderRepository.countOrdersWithoutErpDocument(tenantId, eligibleStatuses));

        return counts;
    }

    /**
     * 전표 미생성 주문 목록 조회 (배송중/배송완료)
     */
    @Transactional(readOnly = true)
    public Page<Order> getOrdersWithoutDocument(int page, int size) {
        UUID tenantId = TenantContext.requireTenantId();
        List<OrderStatus> eligibleStatuses = List.of(OrderStatus.SHIPPING, OrderStatus.DELIVERED);
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "orderedAt"));

        return orderRepository.findOrdersWithoutErpDocument(tenantId, eligibleStatuses, pageable);
    }

    /**
     * 선택된 주문에 대해 일괄 전표 생성
     */
    @Transactional
    public Map<String, Object> generateDocumentsForOrders(List<UUID> orderIds) {
        UUID tenantId = TenantContext.requireTenantId();

        int successCount = 0;
        int failCount = 0;
        List<Map<String, Object>> results = new ArrayList<>();

        for (UUID orderId : orderIds) {
            try {
                // 이미 전표가 있는지 확인
                if (documentRepository.existsActiveByOrderId(orderId)) {
                    results.add(Map.of("orderId", orderId, "success", false, "error", "이미 전표가 존재합니다"));
                    failCount++;
                    continue;
                }

                ErpSalesDocument doc = generateDocument(orderId);
                results.add(Map.of("orderId", orderId, "success", true, "documentId", doc.getId()));
                successCount++;
            } catch (Exception e) {
                log.warn("Failed to generate document for order {}: {}", orderId, e.getMessage());
                results.add(Map.of("orderId", orderId, "success", false, "error", e.getMessage()));
                failCount++;
            }
        }

        log.info("Bulk document generation completed: total={}, success={}, fail={}",
                orderIds.size(), successCount, failCount);

        return Map.of(
                "totalCount", orderIds.size(),
                "successCount", successCount,
                "failCount", failCount,
                "results", results
        );
    }

    /**
     * 자동 전표생성/전송 배치 처리 (스케줄러에서 호출)
     * - autoGenerateDocument가 활성화된 경우: 전표 미생성 주문에 대해 자동 전표 생성
     * - autoSendToErp가 활성화된 경우: 미전송 전표를 ERP로 자동 전송
     *
     * @param tenantId 테넌트 ID
     * @return 처리 결과 (생성 건수, 전송 건수 등)
     */
    @Transactional
    public Map<String, Object> processAutoErpBatch(UUID tenantId) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("tenantId", tenantId.toString());

        // 활성 ERP 설정 조회
        List<TenantErpConfig> configs = erpConfigRepository.findByTenantIdAndActiveTrue(tenantId);
        if (configs.isEmpty()) {
            result.put("skipped", true);
            result.put("reason", "활성화된 ERP 설정 없음");
            return result;
        }

        TenantErpConfig config = configs.get(0);
        boolean autoGenerate = Boolean.TRUE.equals(config.getAutoGenerateDocument());
        boolean autoSend = Boolean.TRUE.equals(config.getAutoSendToErp());

        result.put("autoGenerateDocument", autoGenerate);
        result.put("autoSendToErp", autoSend);

        if (!autoGenerate && !autoSend) {
            result.put("skipped", true);
            result.put("reason", "자동 전표생성/전송 모두 비활성화");
            return result;
        }

        int generatedCount = 0;
        int generateFailCount = 0;
        int sentCount = 0;
        int sendFailCount = 0;

        // 1. 자동 전표생성
        if (autoGenerate) {
            List<OrderStatus> eligibleStatuses = List.of(OrderStatus.SHIPPING, OrderStatus.DELIVERED);
            List<Order> ordersWithoutDoc = orderRepository.findOrdersWithoutErpDocument(tenantId, eligibleStatuses);

            log.info("[AutoErpBatch] tenant={}, ordersWithoutDocument={}", tenantId, ordersWithoutDoc.size());

            for (Order order : ordersWithoutDoc) {
                try {
                    generateDocument(order.getId());
                    generatedCount++;
                } catch (Exception e) {
                    log.warn("[AutoErpBatch] Document generation failed for order {}: {}", order.getId(), e.getMessage());
                    generateFailCount++;
                }
            }
        }

        result.put("generatedCount", generatedCount);
        result.put("generateFailCount", generateFailCount);

        // 2. 자동 ERP 전송
        if (autoSend) {
            List<ErpSalesDocument> pendingDocs = documentRepository.findByTenantIdAndStatusIn(
                    tenantId, List.of(ErpDocumentStatus.PENDING));

            log.info("[AutoErpBatch] tenant={}, pendingDocuments={}", tenantId, pendingDocs.size());

            for (ErpSalesDocument doc : pendingDocs) {
                try {
                    sendDocumentInternal(doc);
                    sentCount++;
                } catch (Exception e) {
                    log.warn("[AutoErpBatch] Document send failed for doc {}: {}", doc.getId(), e.getMessage());
                    sendFailCount++;
                }
            }
        }

        result.put("sentCount", sentCount);
        result.put("sendFailCount", sendFailCount);

        log.info("[AutoErpBatch] tenant={} completed: generated={}, sent={}", tenantId, generatedCount, sentCount);
        return result;
    }

    /**
     * ErpDocumentGenerator 인터페이스 구현 - 전표 생성 시도 (실패해도 예외 발생하지 않음)
     */
    @Override
    @Transactional
    public void tryGenerateDocument(UUID orderId) {
        try {
            if (shouldGenerateDocument(orderId)) {
                generateDocument(orderId);
                log.info("Auto-generated ERP document for order {}", orderId);
            }
        } catch (Exception e) {
            log.warn("Failed to auto-generate ERP document for order {}: {}", orderId, e.getMessage());
            // 전표 생성 실패해도 예외 발생하지 않음
        }
    }

    private TenantErpConfig getActiveErpConfig(UUID tenantId) {
        List<TenantErpConfig> configs = erpConfigRepository.findByTenantIdAndActiveTrue(tenantId);
        if (configs.isEmpty()) {
            throw new BusinessException(ErrorCodes.ERP_CONFIG_NOT_FOUND, "활성화된 ERP 설정이 없습니다");
        }
        return configs.get(0);
    }

    private ErpSalesTemplate getActiveTemplate(UUID tenantId, UUID erpConfigId) {
        return erpSalesTemplateRepository.findByTenantIdAndErpConfigId(tenantId, erpConfigId)
                .filter(ErpSalesTemplate::getActive)
                .orElseThrow(() -> new BusinessException(ErrorCodes.ERP_TEMPLATE_NOT_FOUND,
                        "전표 템플릿이 설정되지 않았습니다. 설정 > ERP에서 전표 템플릿을 먼저 설정해주세요."));
    }

    /**
     * documentLines를 ECount API 형식 (BulkDatas 래퍼)으로 변환
     */
    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> wrapWithBulkDatas(List<Map<String, Object>> documentLines) {
        List<Map<String, Object>> saleList = new ArrayList<>();
        if (documentLines != null) {
            for (Map<String, Object> line : documentLines) {
                Map<String, Object> wrapper = new LinkedHashMap<>();
                wrapper.put("BulkDatas", line);
                saleList.add(wrapper);
            }
        }
        return saleList;
    }
}
