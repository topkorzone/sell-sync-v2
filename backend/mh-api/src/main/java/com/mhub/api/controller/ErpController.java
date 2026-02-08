package com.mhub.api.controller;

import com.mhub.common.dto.ApiResponse;
import com.mhub.common.dto.PageResponse;
import com.mhub.core.domain.entity.ErpItem;
import com.mhub.core.domain.entity.ErpSyncLog;
import com.mhub.core.domain.entity.Order;
import com.mhub.core.domain.entity.TenantErpConfig;
import com.mhub.core.domain.enums.SyncStatus;
import com.mhub.core.domain.repository.ErpItemRepository;
import com.mhub.core.domain.repository.ErpSyncLogRepository;
import com.mhub.core.domain.repository.TenantErpConfigRepository;
import com.mhub.core.tenant.TenantContext;
import com.mhub.core.erp.dto.ErpItemResponse;
import com.mhub.core.erp.dto.InventoryBalanceDto;
import com.mhub.core.erp.dto.InventoryBalanceResponse;
import com.mhub.core.erp.dto.ErpSalesDocumentResponse;
import com.mhub.core.erp.dto.ErpBatchSendResult;
import com.mhub.core.erp.dto.ErpFieldMappingRequest;
import com.mhub.core.erp.dto.ErpFieldMappingResponse;
import com.mhub.core.domain.enums.ErpDocumentStatus;
import com.mhub.erp.service.ErpFieldMappingService;
import com.mhub.erp.service.ErpInventoryService;
import com.mhub.erp.service.ErpSalesDocumentService;
import com.mhub.erp.service.ErpSyncService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Tag(name = "ERP")
@RestController
@RequestMapping("/api/v1/erp")
@RequiredArgsConstructor
public class ErpController {
    private final ErpSyncLogRepository erpSyncLogRepository;
    private final ErpSyncService erpSyncService;
    private final ErpItemRepository erpItemRepository;
    private final TenantErpConfigRepository erpConfigRepository;
    private final ErpInventoryService erpInventoryService;
    private final ErpSalesDocumentService erpSalesDocumentService;
    private final ErpFieldMappingService erpFieldMappingService;

    // ================== 판매전표 관리 API ==================

    @Operation(summary = "전표 목록 조회")
    @GetMapping("/documents")
    public ApiResponse<PageResponse<ErpSalesDocumentResponse>> getDocuments(
            @RequestParam(required = false) ErpDocumentStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<ErpSalesDocumentResponse> documents = erpSalesDocumentService.getDocuments(status, page, size);
        return ApiResponse.ok(PageResponse.of(documents.getContent(), documents.getNumber(),
                documents.getSize(), documents.getTotalElements()));
    }

    @Operation(summary = "전표 상세 조회")
    @GetMapping("/documents/{documentId}")
    public ApiResponse<ErpSalesDocumentResponse> getDocument(@PathVariable UUID documentId) {
        return ApiResponse.ok(erpSalesDocumentService.getDocument(documentId));
    }

    @Operation(summary = "전표 삭제 (취소)")
    @DeleteMapping("/documents/{documentId}")
    public ApiResponse<Void> cancelDocument(@PathVariable UUID documentId) {
        erpSalesDocumentService.cancelDocument(documentId);
        return ApiResponse.ok();
    }

    @Operation(summary = "개별 전표 전송")
    @PostMapping("/documents/{documentId}/send")
    public ApiResponse<ErpSalesDocumentResponse> sendDocument(@PathVariable UUID documentId) {
        return ApiResponse.ok(erpSalesDocumentService.sendDocument(documentId));
    }

    @Operation(summary = "미전송 전표 일괄 전송")
    @PostMapping("/documents/send-all")
    public ApiResponse<ErpBatchSendResult> sendAllPending() {
        return ApiResponse.ok(erpSalesDocumentService.sendAllPending());
    }

    @Operation(summary = "선택된 전표 일괄 전송")
    @PostMapping("/documents/send-selected")
    public ApiResponse<ErpBatchSendResult> sendSelected(@RequestBody List<UUID> documentIds) {
        return ApiResponse.ok(erpSalesDocumentService.sendSelected(documentIds));
    }

    @Operation(summary = "전표 재생성")
    @PostMapping("/documents/regenerate/{orderId}")
    public ApiResponse<ErpSalesDocumentResponse> regenerateDocument(@PathVariable UUID orderId) {
        var doc = erpSalesDocumentService.regenerateDocument(orderId);
        return ApiResponse.ok(erpSalesDocumentService.getDocument(doc.getId()));
    }

    @Operation(summary = "주문에 대한 전표 수동 생성")
    @PostMapping("/documents/generate/{orderId}")
    public ApiResponse<ErpSalesDocumentResponse> generateDocument(@PathVariable UUID orderId) {
        var doc = erpSalesDocumentService.generateDocument(orderId);
        return ApiResponse.ok(erpSalesDocumentService.getDocument(doc.getId()));
    }

    @Operation(summary = "상태별 전표 수 조회")
    @GetMapping("/documents/counts")
    public ApiResponse<Map<String, Long>> getDocumentCounts() {
        return ApiResponse.ok(erpSalesDocumentService.getDocumentCounts());
    }

    @Operation(summary = "전표 미생성 주문 목록 조회 (배송중/배송완료)")
    @GetMapping("/documents/pending-orders")
    public ApiResponse<PageResponse<Map<String, Object>>> getOrdersWithoutDocument(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<Order> orders = erpSalesDocumentService.getOrdersWithoutDocument(page, size);

        List<Map<String, Object>> content = orders.getContent().stream()
                .map(order -> Map.<String, Object>of(
                        "id", order.getId(),
                        "marketplaceType", order.getMarketplaceType(),
                        "marketplaceOrderId", order.getMarketplaceOrderId(),
                        "status", order.getStatus(),
                        "receiverName", order.getReceiverName() != null ? order.getReceiverName() : "",
                        "totalAmount", order.getTotalAmount() != null ? order.getTotalAmount() : 0,
                        "orderedAt", order.getOrderedAt() != null ? order.getOrderedAt().toString() : ""
                ))
                .collect(Collectors.toList());

        return ApiResponse.ok(PageResponse.of(content, orders.getNumber(),
                orders.getSize(), orders.getTotalElements()));
    }

    @Operation(summary = "선택된 주문에 대해 일괄 전표 생성")
    @PostMapping("/documents/generate-batch")
    public ApiResponse<Map<String, Object>> generateDocumentsBatch(@RequestBody List<UUID> orderIds) {
        return ApiResponse.ok(erpSalesDocumentService.generateDocumentsForOrders(orderIds));
    }

    // ================== 재고 관리 API ==================

    @Operation(summary = "ERP 창고별 재고현황 조회 (실시간)")
    @GetMapping("/inventory-balance")
    public ApiResponse<InventoryBalanceResponse> getInventoryBalance(
            @RequestParam List<String> prodCds) {
        return ApiResponse.ok(erpInventoryService.getInventoryBalance(prodCds));
    }

    @Operation(summary = "ERP 재고 동기화 - 특정 품목 (ERP -> DB)")
    @PostMapping("/inventory-balance/sync")
    public ApiResponse<InventoryBalanceResponse> syncInventoryBalance(
            @RequestParam List<String> prodCds) {
        return ApiResponse.ok(erpInventoryService.syncInventoryBalance(prodCds));
    }

    @Operation(summary = "ERP 재고 동기화 - 전체 품목 (ERP -> DB)")
    @PostMapping("/inventory-balance/sync-all")
    public ApiResponse<InventoryBalanceResponse> syncAllInventoryBalance() {
        return ApiResponse.ok(erpInventoryService.syncAllInventory());
    }

    @Operation(summary = "ERP 품목 목록 조회 (재고 정보 포함)")
    @GetMapping("/items")
    public ApiResponse<PageResponse<ErpItemResponse>> listErpItems(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String keyword) {
        UUID tenantId = TenantContext.requireTenantId();

        // 테넌트의 활성 ERP 설정 찾기
        List<TenantErpConfig> configs = erpConfigRepository.findByTenantIdAndActiveTrue(tenantId);
        if (configs.isEmpty()) {
            return ApiResponse.ok(PageResponse.empty());
        }

        TenantErpConfig config = configs.get(0);
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "prodCd"));

        Page<ErpItem> items;
        if (keyword != null && !keyword.isBlank()) {
            items = erpItemRepository.findByTenantIdAndErpConfigIdAndKeyword(
                    tenantId, config.getId(), keyword, pageRequest);
        } else {
            items = erpItemRepository.findByTenantIdAndErpConfigId(tenantId, config.getId(), pageRequest);
        }

        // 품목코드 목록 추출
        List<String> prodCds = items.getContent().stream()
                .map(ErpItem::getProdCd)
                .collect(Collectors.toList());

        // DB에서 재고 정보 조회
        Map<String, List<InventoryBalanceDto>> inventoryMap = erpInventoryService.getInventoryFromDb(prodCds);

        // 품목 + 재고 정보 조합
        List<ErpItemResponse> responses = items.getContent().stream()
                .map(item -> ErpItemResponse.from(item, inventoryMap.getOrDefault(item.getProdCd(), List.of())))
                .collect(Collectors.toList());

        return ApiResponse.ok(PageResponse.of(responses, items.getNumber(),
                items.getSize(), items.getTotalElements()));
    }

    @GetMapping("/sync-status")
    public ApiResponse<List<ErpSyncLog>> getSyncStatus() {
        return ApiResponse.ok(erpSyncLogRepository.findByTenantIdAndStatus(
                TenantContext.requireTenantId(), SyncStatus.FAILED));
    }

    @PostMapping("/retry-failed")
    public ApiResponse<Void> retryFailed() {
        erpSyncService.syncUnsyncedSettlements(TenantContext.requireTenantId());
        return ApiResponse.ok();
    }

    @Operation(summary = "단건 주문 ERP 전표 등록")
    @PostMapping("/orders/{orderId}/sync")
    public ApiResponse<Map<String, Object>> syncOrderToErp(@PathVariable UUID orderId) {
        var result = erpSyncService.syncOrderToErp(orderId);
        return ApiResponse.ok(Map.of(
                "success", result.success(),
                "documentId", result.documentId() != null ? result.documentId() : "",
                "message", result.errorMessage() != null ? result.errorMessage() : "전표 등록 완료"
        ));
    }

    @Operation(summary = "전표 미리보기 (실제 전송 안함)")
    @GetMapping("/orders/{orderId}/preview")
    public ApiResponse<Map<String, Object>> previewOrderErpDocument(@PathVariable UUID orderId) {
        return ApiResponse.ok(erpSyncService.previewOrderErpDocument(orderId));
    }

    @Operation(summary = "미전송 주문 일괄 ERP 전표 등록")
    @PostMapping("/orders/sync-all")
    public ApiResponse<Map<String, Object>> syncAllOrders() {
        return ApiResponse.ok(erpSyncService.syncUnsyncedOrders());
    }

    // ================== 필드 매핑 API ==================

    @Operation(summary = "ERP 필드 매핑 목록 조회")
    @GetMapping("/configs/{configId}/field-mappings")
    public ApiResponse<List<ErpFieldMappingResponse>> getFieldMappings(@PathVariable UUID configId) {
        return ApiResponse.ok(erpFieldMappingService.getFieldMappings(configId));
    }

    @Operation(summary = "ERP 필드 매핑 생성")
    @PostMapping("/configs/{configId}/field-mappings")
    public ApiResponse<ErpFieldMappingResponse> createFieldMapping(
            @PathVariable UUID configId,
            @RequestBody ErpFieldMappingRequest request) {
        return ApiResponse.ok(erpFieldMappingService.createFieldMapping(configId, request));
    }

    @Operation(summary = "ERP 필드 매핑 수정")
    @PutMapping("/configs/{configId}/field-mappings/{mappingId}")
    public ApiResponse<ErpFieldMappingResponse> updateFieldMapping(
            @PathVariable UUID configId,
            @PathVariable UUID mappingId,
            @RequestBody ErpFieldMappingRequest request) {
        return ApiResponse.ok(erpFieldMappingService.updateFieldMapping(configId, mappingId, request));
    }

    @Operation(summary = "ERP 필드 매핑 삭제")
    @DeleteMapping("/configs/{configId}/field-mappings/{mappingId}")
    public ApiResponse<Void> deleteFieldMapping(
            @PathVariable UUID configId,
            @PathVariable UUID mappingId) {
        erpFieldMappingService.deleteFieldMapping(configId, mappingId);
        return ApiResponse.ok();
    }

    @Operation(summary = "ERP 필드 매핑 일괄 저장")
    @PostMapping("/configs/{configId}/field-mappings/batch")
    public ApiResponse<List<ErpFieldMappingResponse>> saveFieldMappings(
            @PathVariable UUID configId,
            @RequestBody List<ErpFieldMappingRequest> requests) {
        return ApiResponse.ok(erpFieldMappingService.saveFieldMappings(configId, requests));
    }

    @Operation(summary = "사용 가능한 플레이스홀더 목록")
    @GetMapping("/field-mappings/placeholders")
    public ApiResponse<List<ErpFieldMappingService.PlaceholderInfo>> getAvailablePlaceholders() {
        return ApiResponse.ok(erpFieldMappingService.getAvailablePlaceholders());
    }
}
