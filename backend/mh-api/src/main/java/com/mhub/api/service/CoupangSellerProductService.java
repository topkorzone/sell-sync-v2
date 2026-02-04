package com.mhub.api.service;

import com.mhub.common.exception.BusinessException;
import com.mhub.common.exception.ErrorCodes;
import com.mhub.core.domain.entity.CoupangSellerProduct;
import com.mhub.core.domain.entity.TenantMarketplaceCredential;
import com.mhub.core.domain.enums.MarketplaceType;
import com.mhub.core.domain.repository.CoupangSellerProductRepository;
import com.mhub.core.domain.repository.TenantMarketplaceCredentialRepository;
import com.mhub.core.service.dto.CoupangSellerProductResponse;
import com.mhub.core.service.dto.CoupangSellerProductSyncResponse;
import com.mhub.core.tenant.TenantContext;
import com.mhub.marketplace.adapter.coupang.CoupangAdapter;
import com.mhub.marketplace.adapter.coupang.dto.CoupangSellerProductDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Service
public class CoupangSellerProductService {

    private static final int SAVE_BATCH_SIZE = 100;  // DB 저장 시 배치 크기

    private final CoupangSellerProductRepository sellerProductRepository;
    private final TenantMarketplaceCredentialRepository credentialRepository;
    private final CoupangAdapter coupangAdapter;
    private final JdbcTemplate jdbcTemplate;
    private final TransactionTemplate transactionTemplate;

    public CoupangSellerProductService(
            CoupangSellerProductRepository sellerProductRepository,
            TenantMarketplaceCredentialRepository credentialRepository,
            CoupangAdapter coupangAdapter,
            JdbcTemplate jdbcTemplate,
            PlatformTransactionManager transactionManager) {
        this.sellerProductRepository = sellerProductRepository;
        this.credentialRepository = credentialRepository;
        this.coupangAdapter = coupangAdapter;
        this.jdbcTemplate = jdbcTemplate;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    /**
     * 쿠팡 등록상품 동기화 (스트리밍 방식)
     * API에서 페이지(100개) 단위로 수집하여 즉시 DB에 저장
     * Statement timeout 방지를 위해 짧은 트랜잭션으로 처리
     *
     * @return 동기화 결과
     */
    public CoupangSellerProductSyncResponse syncSellerProducts() {
        UUID tenantId = TenantContext.requireTenantId();
        LocalDateTime startedAt = LocalDateTime.now();
        LocalDateTime syncedAt = LocalDateTime.now();

        log.info("Starting Coupang seller products sync (streaming) for tenant {}", tenantId);

        try {
            // 쿠팡 인증 정보 조회
            TenantMarketplaceCredential credential = credentialRepository
                    .findByTenantIdAndMarketplaceTypeAndActiveTrue(tenantId, MarketplaceType.COUPANG)
                    .orElseThrow(() -> new BusinessException(ErrorCodes.MARKETPLACE_CREDENTIAL_NOT_FOUND,
                            "쿠팡 마켓플레이스 인증 정보가 없습니다."));

            // 기존 상품 ID 조회 (insert/update 카운트용)
            Set<Long> existingProductIds = new HashSet<>();
            List<CoupangSellerProduct> existingProducts = sellerProductRepository.findByTenantId(tenantId);
            for (CoupangSellerProduct p : existingProducts) {
                existingProductIds.add(p.getSellerProductId());
            }

            AtomicInteger totalCount = new AtomicInteger(0);
            AtomicInteger insertedCount = new AtomicInteger(0);
            AtomicInteger updatedCount = new AtomicInteger(0);

            // 스트리밍 방식: 페이지(100개) 단위로 즉시 DB 저장
            coupangAdapter.streamSellerProducts(credential, pageProducts -> {
                // insert/update 카운트
                for (CoupangSellerProductDto dto : pageProducts) {
                    if (existingProductIds.contains(dto.getSellerProductId())) {
                        updatedCount.incrementAndGet();
                    } else {
                        insertedCount.incrementAndGet();
                        existingProductIds.add(dto.getSellerProductId());
                    }
                }

                // 즉시 DB 저장 (짧은 트랜잭션)
                savePage(tenantId, pageProducts, syncedAt);
                totalCount.addAndGet(pageProducts.size());
                log.debug("Saved {} products (total: {})", pageProducts.size(), totalCount.get());
            });

            LocalDateTime completedAt = LocalDateTime.now();

            log.info("Coupang seller products sync completed for tenant {}: total={}, inserted={}, updated={}",
                    tenantId, totalCount.get(), insertedCount.get(), updatedCount.get());

            return CoupangSellerProductSyncResponse.success(totalCount.get(), insertedCount.get(), updatedCount.get(), startedAt, completedAt);

        } catch (BusinessException e) {
            log.error("Coupang seller products sync failed for tenant {}: {}", tenantId, e.getMessage());
            return CoupangSellerProductSyncResponse.failure(e.getMessage(), startedAt);
        } catch (Exception e) {
            log.error("Coupang seller products sync error for tenant {}", tenantId, e);
            return CoupangSellerProductSyncResponse.failure("동기화 중 오류가 발생했습니다: " + e.getMessage(), startedAt);
        }
    }

    /**
     * 페이지 단위 저장 (배치 INSERT 방식)
     * API에서 받은 100개를 한 번에 배치 INSERT
     */
    private void savePage(UUID tenantId, List<CoupangSellerProductDto> products, LocalDateTime syncedAt) {
        if (products.isEmpty()) {
            return;
        }

        String sql = "INSERT INTO coupang_seller_product " +
                "(id, tenant_id, seller_product_id, seller_product_name, display_category_code, " +
                "category_id, product_id, vendor_id, sale_started_at, sale_ended_at, brand, status_name, " +
                "synced_at, created_at, updated_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) " +
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
                "updated_at = NOW()";

        Timestamp now = Timestamp.valueOf(LocalDateTime.now());
        Timestamp syncedTs = Timestamp.valueOf(syncedAt);

        List<Object[]> batchArgs = new ArrayList<>();
        for (CoupangSellerProductDto dto : products) {
            Object[] args = new Object[] {
                    UUID.randomUUID(),
                    tenantId,
                    dto.getSellerProductId(),
                    dto.getSellerProductName(),
                    dto.getDisplayCategoryCode(),
                    dto.getCategoryId(),
                    dto.getProductId(),
                    dto.getVendorId(),
                    dto.getSaleStartedAt() != null ? Timestamp.valueOf(dto.getSaleStartedAt()) : null,
                    dto.getSaleEndedAt() != null ? Timestamp.valueOf(dto.getSaleEndedAt()) : null,
                    dto.getBrand(),
                    dto.getStatusName(),
                    syncedTs,
                    now,
                    now
            };
            batchArgs.add(args);
        }

        // 100개씩 배치 INSERT
        jdbcTemplate.batchUpdate(sql, batchArgs);
    }

    /**
     * 등록상품 목록 조회 (페이징)
     */
    @Transactional(readOnly = true)
    public Page<CoupangSellerProductResponse> getSellerProducts(String keyword, String statusName,
                                                                  String brand, Pageable pageable) {
        UUID tenantId = TenantContext.requireTenantId();
        Page<CoupangSellerProduct> products;

        // 키워드 검색이 있는 경우
        if (StringUtils.hasText(keyword)) {
            products = sellerProductRepository.searchByKeyword(tenantId, keyword, pageable);
        }
        // 상태 필터
        else if (StringUtils.hasText(statusName)) {
            products = sellerProductRepository.findByTenantIdAndStatusName(tenantId, statusName, pageable);
        }
        // 브랜드 필터
        else if (StringUtils.hasText(brand)) {
            products = sellerProductRepository.findByTenantIdAndBrand(tenantId, brand, pageable);
        }
        // 전체 조회
        else {
            products = sellerProductRepository.findByTenantId(tenantId, pageable);
        }

        return products.map(CoupangSellerProductResponse::from);
    }

    /**
     * 등록상품 단건 조회
     */
    @Transactional(readOnly = true)
    public CoupangSellerProductResponse getSellerProduct(UUID id) {
        UUID tenantId = TenantContext.requireTenantId();
        CoupangSellerProduct product = sellerProductRepository.findById(id)
                .filter(p -> p.getTenantId().equals(tenantId))
                .orElseThrow(() -> new BusinessException(ErrorCodes.ORDER_NOT_FOUND,
                        "등록상품을 찾을 수 없습니다: " + id));
        return CoupangSellerProductResponse.from(product);
    }

    /**
     * 등록상품 수 조회
     */
    @Transactional(readOnly = true)
    public long getProductCount() {
        UUID tenantId = TenantContext.requireTenantId();
        return sellerProductRepository.countByTenantId(tenantId);
    }

    /**
     * 브랜드 목록 조회
     */
    @Transactional(readOnly = true)
    public List<String> getBrands() {
        UUID tenantId = TenantContext.requireTenantId();
        return sellerProductRepository.findDistinctBrandsByTenantId(tenantId);
    }

    /**
     * 상태 목록 조회
     */
    @Transactional(readOnly = true)
    public List<String> getStatuses() {
        UUID tenantId = TenantContext.requireTenantId();
        return sellerProductRepository.findDistinctStatusesByTenantId(tenantId);
    }
}
