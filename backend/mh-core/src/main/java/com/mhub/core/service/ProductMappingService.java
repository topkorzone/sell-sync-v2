package com.mhub.core.service;

import com.mhub.common.exception.BusinessException;
import com.mhub.common.exception.ErrorCodes;
import com.mhub.core.domain.entity.ErpItem;
import com.mhub.core.domain.entity.Order;
import com.mhub.core.domain.entity.OrderItem;
import com.mhub.core.domain.entity.ProductMapping;
import com.mhub.core.domain.enums.MarketplaceType;
import com.mhub.core.domain.repository.ErpItemRepository;
import com.mhub.core.domain.repository.OrderItemRepository;
import com.mhub.core.domain.repository.ProductMappingRepository;
import com.mhub.core.service.dto.ProductMappingRequest;
import com.mhub.core.service.dto.ProductMappingResponse;
import com.mhub.core.service.dto.UnmappedProductResponse;
import com.mhub.core.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductMappingService {

    private final ProductMappingRepository productMappingRepository;
    private final ErpItemRepository erpItemRepository;
    private final OrderItemRepository orderItemRepository;
    private final CoupangCommissionRateService coupangCommissionRateService;

    /**
     * 주문의 상품 정보를 product_mapping에 등록 또는 업데이트 (UPSERT)
     * - 신규 상품: INSERT (미매핑 상태로 등록)
     * - 기존 상품: 상품명/옵션명 UPDATE (erp_prod_cd는 유지)
     * @return 저장된 상품 수 (신규 + 업데이트)
     */
    @Transactional
    public int registerProductsFromOrder(Order order, UUID tenantId, MarketplaceType marketplaceType) {
        if (order.getItems() == null || order.getItems().isEmpty()) {
            return 0;
        }

        List<ProductMapping> toSave = new ArrayList<>();
        int newCount = 0;
        int updateCount = 0;

        for (OrderItem item : order.getItems()) {
            if (!StringUtils.hasText(item.getMarketplaceProductId())) {
                continue;
            }

            // 기존 매핑 여부 확인
            Optional<ProductMapping> existingMapping = findMappingIncludingUnmapped(
                    tenantId,
                    marketplaceType,
                    item.getMarketplaceProductId(),
                    item.getMarketplaceSku()
            );

            if (existingMapping.isEmpty()) {
                // INSERT: 신규 상품 등록 (미매핑 상태)
                String sku = StringUtils.hasText(item.getMarketplaceSku()) ? item.getMarketplaceSku() : null;
                ProductMapping newMapping = ProductMapping.builder()
                        .tenantId(tenantId)
                        .marketplaceType(marketplaceType)
                        .marketplaceProductId(item.getMarketplaceProductId())
                        .marketplaceSku(sku)
                        .marketplaceProductName(item.getProductName())
                        .marketplaceOptionName(item.getOptionName())
                        .erpItemId(null)
                        .erpProdCd(null)  // 미매핑 상태
                        .autoCreated(true)
                        .useCount(0)
                        .build();
                toSave.add(newMapping);
                newCount++;
            } else {
                // UPDATE: 기존 상품 정보 업데이트 (erp_prod_cd는 유지)
                ProductMapping existing = existingMapping.get();
                boolean updated = false;

                // 상품명 변경 시 업데이트
                if (!Objects.equals(existing.getMarketplaceProductName(), item.getProductName())) {
                    existing.setMarketplaceProductName(item.getProductName());
                    updated = true;
                }
                // 옵션명 변경 시 업데이트
                if (!Objects.equals(existing.getMarketplaceOptionName(), item.getOptionName())) {
                    existing.setMarketplaceOptionName(item.getOptionName());
                    updated = true;
                }

                if (updated) {
                    toSave.add(existing);
                    updateCount++;
                }
            }
        }

        // 배치 UPSERT
        if (!toSave.isEmpty()) {
            productMappingRepository.saveAll(toSave);
            log.info("Batch registered/updated products for order: marketplaceOrderId={}, new={}, updated={}",
                    order.getMarketplaceOrderId(), newCount, updateCount);
        }

        return toSave.size();
    }

    /**
     * 주문의 모든 항목에 자동 매핑 적용
     * @return 매핑이 적용된 항목 수
     */
    @Transactional
    public int applyAutoMapping(Order order, UUID tenantId, MarketplaceType marketplaceType) {
        if (order.getItems() == null || order.getItems().isEmpty()) {
            return 0;
        }

        // 먼저 주문의 상품 정보를 product_mapping에 등록
        registerProductsFromOrder(order, tenantId, marketplaceType);

        List<ProductMapping> mappingsToUpdate = new ArrayList<>();
        int mappedCount = 0;

        for (OrderItem item : order.getItems()) {
            // 이미 매핑된 항목은 스킵
            if (StringUtils.hasText(item.getErpProdCd())) {
                continue;
            }

            Optional<ProductMapping> mapping = findMapping(
                    tenantId,
                    marketplaceType,
                    item.getMarketplaceProductId(),
                    item.getMarketplaceSku()
            );

            // 매핑이 존재하고 erp_prod_cd가 설정된 경우에만 적용
            if (mapping.isPresent() && StringUtils.hasText(mapping.get().getErpProdCd())) {
                ProductMapping pm = mapping.get();
                item.setErpItemId(pm.getErpItemId());
                item.setErpProdCd(pm.getErpProdCd());
                pm.recordUsage();
                mappingsToUpdate.add(pm);
                mappedCount++;
                log.debug("Auto-mapped order item: productId={}, sku={} -> erpProdCd={}",
                        item.getMarketplaceProductId(), item.getMarketplaceSku(), pm.getErpProdCd());
            }
        }

        // 배치로 매핑 사용 횟수 업데이트
        if (!mappingsToUpdate.isEmpty()) {
            productMappingRepository.saveAll(mappingsToUpdate);
            log.info("Auto-mapped {} items for order: marketplaceOrderId={}",
                    mappedCount, order.getMarketplaceOrderId());
        }

        return mappedCount;
    }

    /**
     * 매핑 조회 (2단계: SKU 포함 조회 → 상품 레벨 fallback)
     * 미매핑 상태(erp_prod_cd=null)인 매핑도 포함하여 조회
     */
    @Transactional(readOnly = true)
    public Optional<ProductMapping> findMappingIncludingUnmapped(UUID tenantId, MarketplaceType marketplaceType,
                                                                  String marketplaceProductId, String marketplaceSku) {
        if (!StringUtils.hasText(marketplaceProductId)) {
            return Optional.empty();
        }

        // 1단계: SKU가 있는 경우 정확한 매핑 조회
        if (StringUtils.hasText(marketplaceSku)) {
            Optional<ProductMapping> exactMatch = productMappingRepository
                    .findByTenantIdAndMarketplaceTypeAndMarketplaceProductIdAndMarketplaceSku(
                            tenantId, marketplaceType, marketplaceProductId, marketplaceSku);
            if (exactMatch.isPresent()) {
                return exactMatch;
            }
        }

        // 2단계: 상품 레벨 fallback (SKU 없는 매핑)
        return productMappingRepository
                .findByTenantIdAndMarketplaceTypeAndMarketplaceProductIdWithoutSku(
                        tenantId, marketplaceType, marketplaceProductId);
    }

    /**
     * 매핑 조회 (2단계: SKU 포함 조회 → 상품 레벨 fallback)
     * erp_prod_cd가 설정된 매핑만 반환 (자동 매핑용)
     */
    @Transactional(readOnly = true)
    public Optional<ProductMapping> findMapping(UUID tenantId, MarketplaceType marketplaceType,
                                                 String marketplaceProductId, String marketplaceSku) {
        Optional<ProductMapping> mapping = findMappingIncludingUnmapped(tenantId, marketplaceType,
                marketplaceProductId, marketplaceSku);

        // erp_prod_cd가 설정된 경우에만 반환 (미매핑 상태는 제외)
        return mapping.filter(pm -> StringUtils.hasText(pm.getErpProdCd()));
    }

    /**
     * 매핑 생성 또는 수정
     */
    @Transactional
    public ProductMappingResponse createOrUpdateMapping(ProductMappingRequest request) {
        UUID tenantId = TenantContext.requireTenantId();
        log.info("[DEBUG] createOrUpdateMapping called: marketplaceType={}, productId={}, sku={}, erpProdCd={}",
                request.getMarketplaceType(), request.getMarketplaceProductId(),
                request.getMarketplaceSku(), request.getErpProdCd());

        // ERP 품목 검증
        ErpItem erpItem = null;
        if (request.getErpItemId() != null) {
            erpItem = erpItemRepository.findById(request.getErpItemId())
                    .orElseThrow(() -> new BusinessException(ErrorCodes.ERP_ITEM_NOT_FOUND,
                            "ERP item not found: " + request.getErpItemId()));
            if (!erpItem.getTenantId().equals(tenantId)) {
                throw new BusinessException(ErrorCodes.ERP_ITEM_NOT_FOUND,
                        "ERP item not found: " + request.getErpItemId());
            }
        }

        String erpProdCd = erpItem != null ? erpItem.getProdCd() : request.getErpProdCd();
        if (!StringUtils.hasText(erpProdCd)) {
            throw new BusinessException(ErrorCodes.PRODUCT_MAPPING_INVALID_REQUEST, "ERP product code is required");
        }

        // 기존 매핑 조회 또는 새로 생성
        String sku = StringUtils.hasText(request.getMarketplaceSku()) ? request.getMarketplaceSku() : null;
        Optional<ProductMapping> existingMapping;

        if (sku != null) {
            existingMapping = productMappingRepository
                    .findByTenantIdAndMarketplaceTypeAndMarketplaceProductIdAndMarketplaceSku(
                            tenantId, request.getMarketplaceType(),
                            request.getMarketplaceProductId(), sku);
        } else {
            existingMapping = productMappingRepository
                    .findByTenantIdAndMarketplaceTypeAndMarketplaceProductIdWithoutSku(
                            tenantId, request.getMarketplaceType(),
                            request.getMarketplaceProductId());
        }

        ProductMapping mapping;
        if (existingMapping.isPresent()) {
            // 기존 매핑 수정
            mapping = existingMapping.get();
            mapping.setErpItemId(erpItem != null ? erpItem.getId() : null);
            mapping.setErpProdCd(erpProdCd);
            mapping.setMarketplaceProductName(request.getMarketplaceProductName());
            mapping.setMarketplaceOptionName(request.getMarketplaceOptionName());
            mapping.setAutoCreated(false); // 수동 수정 시 auto_created는 false로
            log.info("Updated product mapping: id={}, productId={}, sku={} -> erpProdCd={}",
                    mapping.getId(), request.getMarketplaceProductId(), sku, erpProdCd);
        } else {
            // 새 매핑 생성
            mapping = ProductMapping.builder()
                    .tenantId(tenantId)
                    .marketplaceType(request.getMarketplaceType())
                    .marketplaceProductId(request.getMarketplaceProductId())
                    .marketplaceSku(sku)
                    .marketplaceProductName(request.getMarketplaceProductName())
                    .marketplaceOptionName(request.getMarketplaceOptionName())
                    .erpItemId(erpItem != null ? erpItem.getId() : null)
                    .erpProdCd(erpProdCd)
                    .autoCreated(false)
                    .useCount(0)
                    .build();
            log.info("Created product mapping: productId={}, sku={} -> erpProdCd={}",
                    request.getMarketplaceProductId(), sku, erpProdCd);
        }

        mapping = productMappingRepository.save(mapping);

        // 해당 상품의 모든 OrderItem에 매핑 적용 및 수수료 계산
        UUID erpItemId = erpItem != null ? erpItem.getId() : null;
        int mappedCount = applyMappingToOrderItems(tenantId, request.getMarketplaceType(),
                request.getMarketplaceProductId(), sku, erpItemId, erpProdCd);

        // 매핑된 아이템 수만큼 사용횟수 증가
        if (mappedCount > 0) {
            for (int i = 0; i < mappedCount; i++) {
                mapping.recordUsage();
            }
            mapping = productMappingRepository.save(mapping);
        }

        return ProductMappingResponse.from(mapping);
    }

    /**
     * 해당 상품의 모든 OrderItem에 매핑 및 수수료 계산 적용
     * - 미매핑 항목: ERP 코드 설정 + 수수료 계산
     * - 매핑됐지만 수수료 미계산 항목: 수수료 계산만 수행
     * @return 처리된 아이템 수 (매핑 또는 수수료 계산)
     */
    private int applyMappingToOrderItems(UUID tenantId, MarketplaceType marketplaceType,
                                          String productId, String sku, UUID erpItemId, String erpProdCd) {
        log.info("[DEBUG] applyMappingToOrderItems called: tenantId={}, marketplaceType={}, productId={}, sku={}",
                tenantId, marketplaceType, productId, sku);

        // 미매핑 또는 수수료 미계산 OrderItem 조회
        List<OrderItem> itemsToProcess = orderItemRepository.findUnmappedOrNoCommissionByProduct(
                tenantId, marketplaceType, productId, sku);

        log.info("[DEBUG] Found {} items to process (unmapped or no commission)", itemsToProcess.size());

        if (itemsToProcess.isEmpty()) {
            return 0;
        }

        int mappedCount = 0;
        int commissionCount = 0;
        for (OrderItem item : itemsToProcess) {
            log.info("[DEBUG] Processing item: id={}, erpProdCd={}, commissionRate={}",
                    item.getId(), item.getErpProdCd(), item.getCommissionRate());

            // 미매핑 항목은 ERP 코드 설정
            if (!StringUtils.hasText(item.getErpProdCd())) {
                item.setErpItemId(erpItemId);
                item.setErpProdCd(erpProdCd);
                mappedCount++;
                log.info("[DEBUG] Set ERP code for item: id={}", item.getId());
            }

            // 쿠팡인 경우 수수료 계산 (수수료가 없는 경우만)
            if (marketplaceType == MarketplaceType.COUPANG && item.getCommissionRate() == null) {
                log.info("[DEBUG] Calculating commission for item: id={}, productId={}",
                        item.getId(), item.getMarketplaceProductId());
                calculateAndApplyCommission(item, tenantId);
                commissionCount++;
                log.info("[DEBUG] After commission calculation: rate={}, settlement={}",
                        item.getCommissionRate(), item.getExpectedSettlementAmount());
            }
        }

        if (mappedCount > 0 || commissionCount > 0) {
            orderItemRepository.saveAll(itemsToProcess);
            log.info("Applied mapping/commission to order items: productId={}, sku={}, mapped={}, commission={}",
                    productId, sku, mappedCount, commissionCount);
        }

        return mappedCount;
    }

    /**
     * 쿠팡 상품의 수수료율과 정산예정금 계산
     */
    private void calculateAndApplyCommission(OrderItem item, UUID tenantId) {
        try {
            Long productId = Long.parseLong(item.getMarketplaceProductId());
            log.info("[DEBUG] calculateAndApplyCommission: productId={}, tenantId={}", productId, tenantId);

            BigDecimal commissionRate = coupangCommissionRateService.findCommissionRateByProductId(tenantId, productId);
            log.info("[DEBUG] Commission rate from service: {}", commissionRate);

            if (commissionRate != null && item.getTotalPrice() != null) {
                item.setCommissionRate(commissionRate);

                // 정산예정금 계산: 상품금액 - (상품금액 × 수수료율% × 1.10)
                BigDecimal commission = item.getTotalPrice()
                        .multiply(commissionRate)
                        .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(1.10))
                        .setScale(0, RoundingMode.HALF_UP);
                BigDecimal settlementAmount = item.getTotalPrice().subtract(commission);
                item.setExpectedSettlementAmount(settlementAmount);

                log.info("[DEBUG] Commission calculated: productId={}, totalPrice={}, rate={}%, commission={}, settlement={}",
                        productId, item.getTotalPrice(), commissionRate, commission, settlementAmount);
            } else {
                log.warn("[DEBUG] Commission not calculated: commissionRate={}, totalPrice={}",
                        commissionRate, item.getTotalPrice());
            }
        } catch (NumberFormatException e) {
            log.warn("[DEBUG] Invalid productId format: {}", item.getMarketplaceProductId());
        } catch (Exception e) {
            log.warn("[DEBUG] Failed to calculate commission: {}", e.getMessage(), e);
        }
    }

    /**
     * 주문 항목 매핑 시 마스터 테이블에도 저장
     */
    @Transactional
    public void saveToMasterFromOrderItem(UUID tenantId, MarketplaceType marketplaceType,
                                           OrderItem item) {
        if (!StringUtils.hasText(item.getMarketplaceProductId()) ||
            !StringUtils.hasText(item.getErpProdCd())) {
            return;
        }

        String sku = StringUtils.hasText(item.getMarketplaceSku()) ? item.getMarketplaceSku() : null;
        Optional<ProductMapping> existingMapping;

        if (sku != null) {
            existingMapping = productMappingRepository
                    .findByTenantIdAndMarketplaceTypeAndMarketplaceProductIdAndMarketplaceSku(
                            tenantId, marketplaceType, item.getMarketplaceProductId(), sku);
        } else {
            existingMapping = productMappingRepository
                    .findByTenantIdAndMarketplaceTypeAndMarketplaceProductIdWithoutSku(
                            tenantId, marketplaceType, item.getMarketplaceProductId());
        }

        ProductMapping mapping;
        if (existingMapping.isPresent()) {
            mapping = existingMapping.get();
            // 기존 매핑의 ERP 정보가 다르면 업데이트
            if (!item.getErpProdCd().equals(mapping.getErpProdCd())) {
                mapping.setErpItemId(item.getErpItemId());
                mapping.setErpProdCd(item.getErpProdCd());
                mapping.setAutoCreated(true);
                log.info("Updated master mapping from order item: productId={}, sku={} -> erpProdCd={}",
                        item.getMarketplaceProductId(), sku, item.getErpProdCd());
            }
            mapping.recordUsage();
        } else {
            mapping = ProductMapping.builder()
                    .tenantId(tenantId)
                    .marketplaceType(marketplaceType)
                    .marketplaceProductId(item.getMarketplaceProductId())
                    .marketplaceSku(sku)
                    .marketplaceProductName(item.getProductName())
                    .marketplaceOptionName(item.getOptionName())
                    .erpItemId(item.getErpItemId())
                    .erpProdCd(item.getErpProdCd())
                    .autoCreated(true)
                    .useCount(1)
                    .build();
            mapping.recordUsage();
            log.info("Created master mapping from order item: productId={}, sku={} -> erpProdCd={}",
                    item.getMarketplaceProductId(), sku, item.getErpProdCd());
        }

        productMappingRepository.save(mapping);
    }

    /**
     * 매핑 목록 조회 (필터, 검색, 페이징)
     * erp_prod_cd가 있는 완료된 매핑만 조회
     */
    @Transactional(readOnly = true)
    public Page<ProductMappingResponse> getMappings(MarketplaceType marketplaceType,
                                                     String keyword, Pageable pageable) {
        UUID tenantId = TenantContext.requireTenantId();
        Page<ProductMapping> mappings;

        boolean hasKeyword = StringUtils.hasText(keyword);
        boolean hasMarketplace = marketplaceType != null;

        if (hasKeyword && hasMarketplace) {
            mappings = productMappingRepository.searchByMarketplaceTypeAndKeyword(
                    tenantId, marketplaceType, keyword, pageable);
        } else if (hasKeyword) {
            mappings = productMappingRepository.searchByKeyword(tenantId, keyword, pageable);
        } else if (hasMarketplace) {
            mappings = productMappingRepository.findByTenantIdAndMarketplaceTypeAndErpProdCdIsNotNull(
                    tenantId, marketplaceType, pageable);
        } else {
            mappings = productMappingRepository.findByTenantIdAndErpProdCdIsNotNull(tenantId, pageable);
        }

        return mappings.map(ProductMappingResponse::from);
    }

    /**
     * 매핑 상세 조회
     */
    @Transactional(readOnly = true)
    public ProductMappingResponse getMapping(UUID id) {
        UUID tenantId = TenantContext.requireTenantId();
        ProductMapping mapping = productMappingRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new BusinessException(ErrorCodes.PRODUCT_MAPPING_NOT_FOUND,
                        "Product mapping not found: " + id));
        return ProductMappingResponse.from(mapping);
    }

    /**
     * 매핑 삭제
     */
    @Transactional
    public void deleteMapping(UUID id) {
        UUID tenantId = TenantContext.requireTenantId();
        ProductMapping mapping = productMappingRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new BusinessException(ErrorCodes.PRODUCT_MAPPING_NOT_FOUND,
                        "Product mapping not found: " + id));
        productMappingRepository.delete(mapping);
        log.info("Deleted product mapping: id={}, productId={}, erpProdCd={}",
                id, mapping.getMarketplaceProductId(), mapping.getErpProdCd());
    }

    /**
     * 테넌트의 완료된 매핑 수 조회 (erp_prod_cd가 있는 것만)
     */
    @Transactional(readOnly = true)
    public long getMappingCount() {
        UUID tenantId = TenantContext.requireTenantId();
        return productMappingRepository.countByTenantIdAndErpProdCdIsNotNull(tenantId);
    }

    /**
     * 미매핑 상품 목록 조회 (주문 데이터에서 추출)
     * 주문에 포함된 상품 중 ERP 매핑이 없는 상품을 productId+SKU로 그룹화하여 반환
     */
    @Transactional(readOnly = true)
    public Page<UnmappedProductResponse> getUnmappedProducts(MarketplaceType marketplaceType,
                                                               Pageable pageable) {
        UUID tenantId = TenantContext.requireTenantId();

        if (marketplaceType != null) {
            return orderItemRepository.findUnmappedProductsGroupedByMarketplace(
                    tenantId, marketplaceType, pageable);
        } else {
            return orderItemRepository.findUnmappedProductsGrouped(tenantId, pageable);
        }
    }

    /**
     * 미매핑 상품 수 조회
     */
    @Transactional(readOnly = true)
    public long getUnmappedProductCount(MarketplaceType marketplaceType) {
        UUID tenantId = TenantContext.requireTenantId();

        if (marketplaceType != null) {
            return orderItemRepository.countUnmappedProductsByMarketplace(tenantId, marketplaceType);
        } else {
            return orderItemRepository.countUnmappedProducts(tenantId);
        }
    }
}
