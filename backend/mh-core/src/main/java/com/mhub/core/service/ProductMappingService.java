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

import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductMappingService {

    private final ProductMappingRepository productMappingRepository;
    private final ErpItemRepository erpItemRepository;
    private final OrderItemRepository orderItemRepository;

    /**
     * 주문의 모든 항목에 자동 매핑 적용
     * @return 매핑이 적용된 항목 수
     */
    @Transactional
    public int applyAutoMapping(Order order, UUID tenantId, MarketplaceType marketplaceType) {
        if (order.getItems() == null || order.getItems().isEmpty()) {
            return 0;
        }

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

            if (mapping.isPresent()) {
                ProductMapping pm = mapping.get();
                item.setErpItemId(pm.getErpItemId());
                item.setErpProdCd(pm.getErpProdCd());
                pm.recordUsage();
                productMappingRepository.save(pm);
                mappedCount++;
                log.debug("Auto-mapped order item: productId={}, sku={} -> erpProdCd={}",
                        item.getMarketplaceProductId(), item.getMarketplaceSku(), pm.getErpProdCd());
            }
        }

        if (mappedCount > 0) {
            log.info("Auto-mapped {} items for order: marketplaceOrderId={}",
                    mappedCount, order.getMarketplaceOrderId());
        }

        return mappedCount;
    }

    /**
     * 매핑 조회 (2단계: SKU 포함 조회 → 상품 레벨 fallback)
     */
    @Transactional(readOnly = true)
    public Optional<ProductMapping> findMapping(UUID tenantId, MarketplaceType marketplaceType,
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
     * 매핑 생성 또는 수정
     */
    @Transactional
    public ProductMappingResponse createOrUpdateMapping(ProductMappingRequest request) {
        UUID tenantId = TenantContext.requireTenantId();

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
        return ProductMappingResponse.from(mapping);
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
            mappings = productMappingRepository.findByTenantIdAndMarketplaceType(
                    tenantId, marketplaceType, pageable);
        } else {
            mappings = productMappingRepository.findByTenantId(tenantId, pageable);
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
     * 테넌트의 매핑 수 조회
     */
    @Transactional(readOnly = true)
    public long getMappingCount() {
        UUID tenantId = TenantContext.requireTenantId();
        return productMappingRepository.countByTenantId(tenantId);
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
