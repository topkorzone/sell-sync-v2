package com.mhub.api.service;

import com.mhub.common.exception.BusinessException;
import com.mhub.common.exception.ErrorCodes;
import com.mhub.core.domain.entity.CoupangCategory;
import com.mhub.core.domain.entity.CoupangCommissionRate;
import com.mhub.core.domain.entity.TenantMarketplaceCredential;
import com.mhub.core.domain.enums.MarketplaceType;
import com.mhub.core.domain.repository.CoupangCategoryRepository;
import com.mhub.core.domain.repository.CoupangCommissionRateRepository;
import com.mhub.core.domain.repository.TenantMarketplaceCredentialRepository;
import com.mhub.core.tenant.TenantContext;
import com.mhub.marketplace.adapter.coupang.CoupangAdapter;
import com.mhub.marketplace.adapter.coupang.dto.CoupangCategoryDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
public class CoupangCategoryService {

    private static final BigDecimal DEFAULT_COMMISSION_RATE = new BigDecimal("10.8");

    private final CoupangCategoryRepository categoryRepository;
    private final CoupangCommissionRateRepository commissionRateRepository;
    private final TenantMarketplaceCredentialRepository credentialRepository;
    private final CoupangAdapter coupangAdapter;
    private final JdbcTemplate jdbcTemplate;

    public CoupangCategoryService(
            CoupangCategoryRepository categoryRepository,
            CoupangCommissionRateRepository commissionRateRepository,
            TenantMarketplaceCredentialRepository credentialRepository,
            CoupangAdapter coupangAdapter,
            JdbcTemplate jdbcTemplate) {
        this.categoryRepository = categoryRepository;
        this.commissionRateRepository = commissionRateRepository;
        this.credentialRepository = credentialRepository;
        this.coupangAdapter = coupangAdapter;
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 쿠팡 카테고리 동기화
     * 쿠팡 API에서 전체 카테고리를 조회하여 DB에 저장
     *
     * @return 동기화된 카테고리 수
     */
    public Map<String, Object> syncCategories() {
        UUID tenantId = TenantContext.requireTenantId();
        LocalDateTime startedAt = LocalDateTime.now();

        log.info("Starting Coupang category sync for tenant {}", tenantId);

        try {
            // 쿠팡 인증 정보 조회
            TenantMarketplaceCredential credential = credentialRepository
                    .findByTenantIdAndMarketplaceTypeAndActiveTrue(tenantId, MarketplaceType.COUPANG)
                    .orElseThrow(() -> new BusinessException(ErrorCodes.MARKETPLACE_CREDENTIAL_NOT_FOUND,
                            "쿠팡 마켓플레이스 인증 정보가 없습니다."));

            // API에서 카테고리 수집
            List<CoupangCategoryDto> categories = coupangAdapter.collectCategories(credential);

            int totalCount = categories.size();
            LocalDateTime syncedAt = LocalDateTime.now();

            // 기존 카테고리 코드 조회
            Set<Long> existingCodes = new HashSet<>();
            List<CoupangCategory> existingCategories = categoryRepository.findAll();
            for (CoupangCategory c : existingCategories) {
                existingCodes.add(c.getDisplayCategoryCode());
            }

            int insertedCount = 0;
            int updatedCount = 0;
            for (CoupangCategoryDto dto : categories) {
                if (existingCodes.contains(dto.getDisplayCategoryCode())) {
                    updatedCount++;
                } else {
                    insertedCount++;
                }
            }

            // 배치 upsert 실행
            batchUpsert(categories, syncedAt);

            LocalDateTime completedAt = LocalDateTime.now();

            log.info("Coupang category sync completed: total={}, inserted={}, updated={}",
                    totalCount, insertedCount, updatedCount);

            // 대분류 카테고리를 수수료 테이블에 자동 추가
            int commissionRateCount = syncRootCategoriesToCommissionRate();

            Map<String, Object> result = new HashMap<>();
            result.put("commissionRateCount", commissionRateCount);
            result.put("success", true);
            result.put("totalCount", totalCount);
            result.put("insertedCount", insertedCount);
            result.put("updatedCount", updatedCount);
            result.put("startedAt", startedAt);
            result.put("completedAt", completedAt);

            return result;

        } catch (BusinessException e) {
            log.error("Coupang category sync failed for tenant {}: {}", tenantId, e.getMessage());
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("errorMessage", e.getMessage());
            result.put("startedAt", startedAt);
            return result;
        } catch (Exception e) {
            log.error("Coupang category sync error for tenant {}", tenantId, e);
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("errorMessage", "동기화 중 오류가 발생했습니다: " + e.getMessage());
            result.put("startedAt", startedAt);
            return result;
        }
    }

    /**
     * JdbcTemplate을 사용한 배치 upsert
     */
    private void batchUpsert(List<CoupangCategoryDto> categories, LocalDateTime syncedAt) {
        if (categories.isEmpty()) {
            return;
        }

        String sql = "INSERT INTO coupang_category " +
                "(id, display_category_code, display_category_name, parent_category_code, " +
                "depth_level, root_category_code, root_category_name, synced_at, created_at, updated_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?) " +
                "ON CONFLICT (display_category_code) " +
                "DO UPDATE SET " +
                "display_category_name = EXCLUDED.display_category_name, " +
                "parent_category_code = EXCLUDED.parent_category_code, " +
                "depth_level = EXCLUDED.depth_level, " +
                "root_category_code = EXCLUDED.root_category_code, " +
                "root_category_name = EXCLUDED.root_category_name, " +
                "synced_at = EXCLUDED.synced_at, " +
                "updated_at = NOW()";

        Timestamp now = Timestamp.valueOf(LocalDateTime.now());
        Timestamp syncedTs = Timestamp.valueOf(syncedAt);

        List<Object[]> batchArgs = new ArrayList<>();
        for (CoupangCategoryDto dto : categories) {
            Object[] args = new Object[] {
                    UUID.randomUUID(),
                    dto.getDisplayCategoryCode(),
                    dto.getDisplayCategoryName(),
                    dto.getParentCategoryCode(),
                    dto.getDepthLevel(),
                    dto.getRootCategoryCode(),
                    dto.getRootCategoryName(),
                    syncedTs,
                    now,
                    now
            };
            batchArgs.add(args);
        }

        // 100개씩 배치 INSERT
        int batchSize = 100;
        for (int i = 0; i < batchArgs.size(); i += batchSize) {
            int end = Math.min(i + batchSize, batchArgs.size());
            List<Object[]> batch = batchArgs.subList(i, end);
            jdbcTemplate.batchUpdate(sql, batch);
            log.debug("Category batch {} upserted: {} records", (i / batchSize) + 1, batch.size());
        }

        log.info("Total {} categories upserted", batchArgs.size());
    }

    /**
     * 대분류 카테고리 목록 조회
     */
    @Transactional(readOnly = true)
    public List<CoupangCategory> getRootCategories() {
        return categoryRepository.findAllRootCategories();
    }

    /**
     * 카테고리 코드로 대분류 코드 조회
     */
    @Transactional(readOnly = true)
    public Optional<Long> getRootCategoryCode(Long displayCategoryCode) {
        return categoryRepository.findRootCategoryCodeByDisplayCategoryCode(displayCategoryCode);
    }

    /**
     * 전체 카테고리 수 조회
     */
    @Transactional(readOnly = true)
    public long getCategoryCount() {
        return categoryRepository.count();
    }

    /**
     * 대분류 카테고리를 수수료 테이블에 자동 추가
     * 이미 존재하는 카테고리는 건너뛰고, 없는 카테고리만 기본 수수료율(10.8%)로 추가
     *
     * @return 추가된 수수료 레코드 수
     */
    @Transactional
    public int syncRootCategoriesToCommissionRate() {
        List<CoupangCategory> rootCategories = categoryRepository.findAllRootCategories();

        if (rootCategories.isEmpty()) {
            log.info("No root categories found to sync to commission rate table");
            return 0;
        }

        int addedCount = 0;
        LocalDateTime now = LocalDateTime.now();

        for (CoupangCategory category : rootCategories) {
            Long displayCategoryCode = category.getDisplayCategoryCode();

            // 이미 수수료 테이블에 존재하는지 확인
            Optional<CoupangCommissionRate> existing = commissionRateRepository
                    .findByDisplayCategoryCodeAndEffectiveDate(displayCategoryCode, java.time.LocalDate.now());

            if (existing.isEmpty()) {
                // 새로운 수수료 레코드 생성
                CoupangCommissionRate rate = CoupangCommissionRate.builder()
                        .categoryId("CAT_" + displayCategoryCode)
                        .categoryName(category.getDisplayCategoryName())
                        .displayCategoryCode(displayCategoryCode)
                        .commissionRate(DEFAULT_COMMISSION_RATE)
                        .effectiveFrom(java.time.LocalDate.of(2020, 1, 1))
                        .effectiveTo(null) // 종료일 없음 (무기한 유효)
                        .build();

                commissionRateRepository.save(rate);
                addedCount++;

                log.debug("Added commission rate for category: {} ({})",
                        category.getDisplayCategoryName(), displayCategoryCode);
            }
        }

        log.info("Synced {} new root categories to commission rate table", addedCount);
        return addedCount;
    }
}
