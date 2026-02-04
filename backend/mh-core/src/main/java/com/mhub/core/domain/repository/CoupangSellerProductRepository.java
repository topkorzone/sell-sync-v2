package com.mhub.core.domain.repository;

import com.mhub.core.domain.entity.CoupangSellerProduct;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CoupangSellerProductRepository extends JpaRepository<CoupangSellerProduct, UUID> {

    /**
     * 테넌트 + sellerProductId로 조회 (중복 체크용)
     */
    Optional<CoupangSellerProduct> findByTenantIdAndSellerProductId(UUID tenantId, Long sellerProductId);

    /**
     * 테넌트별 목록 조회 (페이징)
     */
    Page<CoupangSellerProduct> findByTenantId(UUID tenantId, Pageable pageable);

    /**
     * 테넌트별 전체 목록 조회
     */
    List<CoupangSellerProduct> findByTenantId(UUID tenantId);

    /**
     * 테넌트별 상품 수 조회
     */
    long countByTenantId(UUID tenantId);

    /**
     * 키워드 검색 (상품명, 브랜드)
     */
    @Query("SELECT p FROM CoupangSellerProduct p " +
           "WHERE p.tenantId = :tenantId " +
           "AND (LOWER(p.sellerProductName) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "OR LOWER(p.brand) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<CoupangSellerProduct> searchByKeyword(
            @Param("tenantId") UUID tenantId,
            @Param("keyword") String keyword,
            Pageable pageable);

    /**
     * 상태별 조회
     */
    Page<CoupangSellerProduct> findByTenantIdAndStatusName(UUID tenantId, String statusName, Pageable pageable);

    /**
     * 브랜드별 조회
     */
    Page<CoupangSellerProduct> findByTenantIdAndBrand(UUID tenantId, String brand, Pageable pageable);

    /**
     * 벌크 Upsert (Native Query)
     * ON CONFLICT시 기존 데이터 업데이트
     */
    @Modifying
    @Query(value = "INSERT INTO coupang_seller_product " +
           "(id, tenant_id, seller_product_id, seller_product_name, display_category_code, " +
           "category_id, product_id, vendor_id, sale_started_at, sale_ended_at, brand, status_name, " +
           "synced_at, created_at, updated_at) " +
           "VALUES (:id, :tenantId, :sellerProductId, :sellerProductName, :displayCategoryCode, " +
           ":categoryId, :productId, :vendorId, :saleStartedAt, :saleEndedAt, :brand, :statusName, " +
           ":syncedAt, :createdAt, :updatedAt) " +
           "ON CONFLICT (tenant_id, seller_product_id) " +
           "DO UPDATE SET " +
           "seller_product_name = EXCLUDED.seller_product_name, " +
           "display_category_code = EXCLUDED.display_category_code, " +
           "category_id = EXCLUDED.category_id, " +
           "product_id = EXCLUDED.product_id, " +
           "vendor_id = EXCLUDED.vendor_id, " +
           "sale_started_at = EXCLUDED.sale_started_at, " +
           "sale_ended_at = EXCLUDED.sale_ended_at, " +
           "brand = EXCLUDED.brand, " +
           "status_name = EXCLUDED.status_name, " +
           "synced_at = EXCLUDED.synced_at, " +
           "updated_at = NOW()",
           nativeQuery = true)
    void upsert(
            @Param("id") UUID id,
            @Param("tenantId") UUID tenantId,
            @Param("sellerProductId") Long sellerProductId,
            @Param("sellerProductName") String sellerProductName,
            @Param("displayCategoryCode") Long displayCategoryCode,
            @Param("categoryId") Long categoryId,
            @Param("productId") Long productId,
            @Param("vendorId") String vendorId,
            @Param("saleStartedAt") LocalDateTime saleStartedAt,
            @Param("saleEndedAt") LocalDateTime saleEndedAt,
            @Param("brand") String brand,
            @Param("statusName") String statusName,
            @Param("syncedAt") LocalDateTime syncedAt,
            @Param("createdAt") LocalDateTime createdAt,
            @Param("updatedAt") LocalDateTime updatedAt);

    /**
     * 테넌트별 브랜드 목록 조회 (DISTINCT)
     */
    @Query("SELECT DISTINCT p.brand FROM CoupangSellerProduct p " +
           "WHERE p.tenantId = :tenantId AND p.brand IS NOT NULL " +
           "ORDER BY p.brand")
    List<String> findDistinctBrandsByTenantId(@Param("tenantId") UUID tenantId);

    /**
     * 테넌트별 상태 목록 조회 (DISTINCT)
     */
    @Query("SELECT DISTINCT p.statusName FROM CoupangSellerProduct p " +
           "WHERE p.tenantId = :tenantId AND p.statusName IS NOT NULL " +
           "ORDER BY p.statusName")
    List<String> findDistinctStatusesByTenantId(@Param("tenantId") UUID tenantId);

    /**
     * 테넌트 + productId로 조회 (주문 수집 시 수수료 매핑용)
     * 같은 productId에 여러 옵션이 있을 수 있어 첫 번째 결과만 반환
     */
    @Query(value = "SELECT * FROM coupang_seller_product " +
           "WHERE tenant_id = :tenantId AND product_id = :productId " +
           "LIMIT 1",
           nativeQuery = true)
    Optional<CoupangSellerProduct> findByTenantIdAndProductId(
            @Param("tenantId") UUID tenantId,
            @Param("productId") Long productId);
}
