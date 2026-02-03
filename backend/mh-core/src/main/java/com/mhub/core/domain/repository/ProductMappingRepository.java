package com.mhub.core.domain.repository;

import com.mhub.core.domain.entity.ProductMapping;
import com.mhub.core.domain.enums.MarketplaceType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProductMappingRepository extends JpaRepository<ProductMapping, UUID> {

    /**
     * 정확한 매핑 조회 (상품ID + SKU)
     */
    Optional<ProductMapping> findByTenantIdAndMarketplaceTypeAndMarketplaceProductIdAndMarketplaceSku(
            UUID tenantId,
            MarketplaceType marketplaceType,
            String marketplaceProductId,
            String marketplaceSku);

    /**
     * 상품 레벨 매핑 조회 (SKU 없이 상품ID만으로)
     */
    @Query("SELECT pm FROM ProductMapping pm WHERE pm.tenantId = :tenantId " +
           "AND pm.marketplaceType = :marketplaceType " +
           "AND pm.marketplaceProductId = :marketplaceProductId " +
           "AND (pm.marketplaceSku IS NULL OR pm.marketplaceSku = '')")
    Optional<ProductMapping> findByTenantIdAndMarketplaceTypeAndMarketplaceProductIdWithoutSku(
            @Param("tenantId") UUID tenantId,
            @Param("marketplaceType") MarketplaceType marketplaceType,
            @Param("marketplaceProductId") String marketplaceProductId);

    /**
     * 테넌트의 모든 매핑 조회 (페이징)
     */
    Page<ProductMapping> findByTenantId(UUID tenantId, Pageable pageable);

    /**
     * 테넌트 + 마켓플레이스 타입별 매핑 조회 (페이징)
     */
    Page<ProductMapping> findByTenantIdAndMarketplaceType(
            UUID tenantId,
            MarketplaceType marketplaceType,
            Pageable pageable);

    /**
     * 키워드 검색 (상품명, 옵션명, ERP 품목코드)
     */
    @Query("SELECT pm FROM ProductMapping pm WHERE pm.tenantId = :tenantId " +
           "AND (LOWER(pm.marketplaceProductName) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "OR LOWER(pm.marketplaceOptionName) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "OR LOWER(pm.erpProdCd) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "OR LOWER(pm.marketplaceProductId) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<ProductMapping> searchByKeyword(
            @Param("tenantId") UUID tenantId,
            @Param("keyword") String keyword,
            Pageable pageable);

    /**
     * 마켓플레이스 타입 + 키워드 검색
     */
    @Query("SELECT pm FROM ProductMapping pm WHERE pm.tenantId = :tenantId " +
           "AND pm.marketplaceType = :marketplaceType " +
           "AND (LOWER(pm.marketplaceProductName) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "OR LOWER(pm.marketplaceOptionName) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "OR LOWER(pm.erpProdCd) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "OR LOWER(pm.marketplaceProductId) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<ProductMapping> searchByMarketplaceTypeAndKeyword(
            @Param("tenantId") UUID tenantId,
            @Param("marketplaceType") MarketplaceType marketplaceType,
            @Param("keyword") String keyword,
            Pageable pageable);

    /**
     * ERP 품목 코드로 매핑 목록 조회
     */
    List<ProductMapping> findByTenantIdAndErpProdCd(UUID tenantId, String erpProdCd);

    /**
     * 테넌트의 매핑 수 조회
     */
    long countByTenantId(UUID tenantId);

    /**
     * ID와 테넌트로 매핑 조회 (소유권 확인용)
     */
    Optional<ProductMapping> findByIdAndTenantId(UUID id, UUID tenantId);

    /**
     * 특정 마켓플레이스의 모든 매핑 조회 (주문 조회 시 매핑 상태 확인용)
     */
    List<ProductMapping> findByTenantIdAndMarketplaceType(UUID tenantId, MarketplaceType marketplaceType);
}
