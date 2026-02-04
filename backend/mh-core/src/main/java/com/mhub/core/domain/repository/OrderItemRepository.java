package com.mhub.core.domain.repository;

import com.mhub.core.domain.entity.OrderItem;
import com.mhub.core.domain.enums.MarketplaceType;
import com.mhub.core.service.dto.UnmappedProductResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, UUID> {
    List<OrderItem> findByOrderId(UUID orderId);

    /**
     * 미매핑 상품 그룹 조회 (마켓플레이스 필터 적용)
     * 주문 데이터에서 product_mapping 테이블에 ERP 매핑이 없는 상품을 productId+SKU로 그룹화
     * erp_prod_cd가 NULL인 경우도 미매핑으로 간주
     */
    @Query("""
        SELECT new com.mhub.core.service.dto.UnmappedProductResponse(
            o.marketplaceType,
            oi.marketplaceProductId,
            oi.marketplaceSku,
            MAX(oi.productName),
            MAX(oi.optionName),
            COUNT(DISTINCT o.id)
        )
        FROM OrderItem oi
        JOIN oi.order o
        WHERE oi.tenantId = :tenantId
        AND o.marketplaceType = :marketplaceType
        AND oi.marketplaceProductId IS NOT NULL
        AND NOT EXISTS (
            SELECT 1 FROM ProductMapping pm
            WHERE pm.tenantId = oi.tenantId
            AND pm.marketplaceType = o.marketplaceType
            AND pm.marketplaceProductId = oi.marketplaceProductId
            AND pm.erpProdCd IS NOT NULL
            AND (
                (pm.marketplaceSku IS NULL AND oi.marketplaceSku IS NULL)
                OR (pm.marketplaceSku IS NULL AND oi.marketplaceSku = '')
                OR pm.marketplaceSku = oi.marketplaceSku
            )
        )
        GROUP BY o.marketplaceType, oi.marketplaceProductId, oi.marketplaceSku
        ORDER BY COUNT(DISTINCT o.id) DESC
        """)
    Page<UnmappedProductResponse> findUnmappedProductsGroupedByMarketplace(
            @Param("tenantId") UUID tenantId,
            @Param("marketplaceType") MarketplaceType marketplaceType,
            Pageable pageable);

    /**
     * 미매핑 상품 그룹 조회 (전체 마켓플레이스)
     * erp_prod_cd가 NULL인 경우도 미매핑으로 간주
     */
    @Query("""
        SELECT new com.mhub.core.service.dto.UnmappedProductResponse(
            o.marketplaceType,
            oi.marketplaceProductId,
            oi.marketplaceSku,
            MAX(oi.productName),
            MAX(oi.optionName),
            COUNT(DISTINCT o.id)
        )
        FROM OrderItem oi
        JOIN oi.order o
        WHERE oi.tenantId = :tenantId
        AND oi.marketplaceProductId IS NOT NULL
        AND NOT EXISTS (
            SELECT 1 FROM ProductMapping pm
            WHERE pm.tenantId = oi.tenantId
            AND pm.marketplaceType = o.marketplaceType
            AND pm.marketplaceProductId = oi.marketplaceProductId
            AND pm.erpProdCd IS NOT NULL
            AND (
                (pm.marketplaceSku IS NULL AND oi.marketplaceSku IS NULL)
                OR (pm.marketplaceSku IS NULL AND oi.marketplaceSku = '')
                OR pm.marketplaceSku = oi.marketplaceSku
            )
        )
        GROUP BY o.marketplaceType, oi.marketplaceProductId, oi.marketplaceSku
        ORDER BY COUNT(DISTINCT o.id) DESC
        """)
    Page<UnmappedProductResponse> findUnmappedProductsGrouped(
            @Param("tenantId") UUID tenantId,
            Pageable pageable);

    /**
     * 미매핑 상품 총 개수 조회 (마켓플레이스 필터 적용)
     * erp_prod_cd가 NULL인 경우도 미매핑으로 간주
     */
    @Query("""
        SELECT COUNT(DISTINCT CONCAT(o.marketplaceType, ':', oi.marketplaceProductId, ':', COALESCE(oi.marketplaceSku, '')))
        FROM OrderItem oi
        JOIN oi.order o
        WHERE oi.tenantId = :tenantId
        AND o.marketplaceType = :marketplaceType
        AND oi.marketplaceProductId IS NOT NULL
        AND NOT EXISTS (
            SELECT 1 FROM ProductMapping pm
            WHERE pm.tenantId = oi.tenantId
            AND pm.marketplaceType = o.marketplaceType
            AND pm.marketplaceProductId = oi.marketplaceProductId
            AND pm.erpProdCd IS NOT NULL
            AND (
                (pm.marketplaceSku IS NULL AND oi.marketplaceSku IS NULL)
                OR (pm.marketplaceSku IS NULL AND oi.marketplaceSku = '')
                OR pm.marketplaceSku = oi.marketplaceSku
            )
        )
        """)
    long countUnmappedProductsByMarketplace(
            @Param("tenantId") UUID tenantId,
            @Param("marketplaceType") MarketplaceType marketplaceType);

    /**
     * 미매핑 상품 총 개수 조회 (전체)
     * erp_prod_cd가 NULL인 경우도 미매핑으로 간주
     */
    @Query("""
        SELECT COUNT(DISTINCT CONCAT(o.marketplaceType, ':', oi.marketplaceProductId, ':', COALESCE(oi.marketplaceSku, '')))
        FROM OrderItem oi
        JOIN oi.order o
        WHERE oi.tenantId = :tenantId
        AND oi.marketplaceProductId IS NOT NULL
        AND NOT EXISTS (
            SELECT 1 FROM ProductMapping pm
            WHERE pm.tenantId = oi.tenantId
            AND pm.marketplaceType = o.marketplaceType
            AND pm.marketplaceProductId = oi.marketplaceProductId
            AND pm.erpProdCd IS NOT NULL
            AND (
                (pm.marketplaceSku IS NULL AND oi.marketplaceSku IS NULL)
                OR (pm.marketplaceSku IS NULL AND oi.marketplaceSku = '')
                OR pm.marketplaceSku = oi.marketplaceSku
            )
        )
        """)
    long countUnmappedProducts(@Param("tenantId") UUID tenantId);

    /**
     * 같은 상품(key)을 가진 미매핑 OrderItem 조회
     * 매핑 시 동일 상품의 다른 주문 항목도 일괄 매핑하기 위해 사용
     */
    @Query("""
        SELECT oi FROM OrderItem oi
        JOIN oi.order o
        WHERE oi.tenantId = :tenantId
        AND o.marketplaceType = :marketplaceType
        AND oi.marketplaceProductId = :productId
        AND (
            (:sku IS NULL AND (oi.marketplaceSku IS NULL OR oi.marketplaceSku = ''))
            OR oi.marketplaceSku = :sku
        )
        AND oi.erpProdCd IS NULL
        """)
    List<OrderItem> findUnmappedByProduct(
            @Param("tenantId") UUID tenantId,
            @Param("marketplaceType") MarketplaceType marketplaceType,
            @Param("productId") String productId,
            @Param("sku") String sku);

    /**
     * 같은 상품(key)을 가진 미매핑 또는 수수료 미계산 OrderItem 조회
     * 매핑 시 동일 상품의 다른 주문 항목도 일괄 매핑 및 수수료 계산하기 위해 사용
     */
    @Query("""
        SELECT oi FROM OrderItem oi
        JOIN oi.order o
        WHERE oi.tenantId = :tenantId
        AND o.marketplaceType = :marketplaceType
        AND oi.marketplaceProductId = :productId
        AND (
            (:sku IS NULL AND (oi.marketplaceSku IS NULL OR oi.marketplaceSku = ''))
            OR oi.marketplaceSku = :sku
        )
        AND (oi.erpProdCd IS NULL OR oi.commissionRate IS NULL)
        """)
    List<OrderItem> findUnmappedOrNoCommissionByProduct(
            @Param("tenantId") UUID tenantId,
            @Param("marketplaceType") MarketplaceType marketplaceType,
            @Param("productId") String productId,
            @Param("sku") String sku);
}
