package com.mhub.core.service;

import com.mhub.core.domain.entity.CoupangCategory;
import com.mhub.core.domain.entity.CoupangCommissionRate;
import com.mhub.core.domain.entity.CoupangSellerProduct;
import com.mhub.core.domain.repository.CoupangCategoryRepository;
import com.mhub.core.domain.repository.CoupangCommissionRateRepository;
import com.mhub.core.domain.repository.CoupangSellerProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CoupangCommissionRateService {

    private final CoupangSellerProductRepository sellerProductRepository;
    private final CoupangCategoryRepository categoryRepository;
    private final CoupangCommissionRateRepository commissionRateRepository;

    /**
     * 주문의 marketplace_product_id(productId)로 수수료율 조회
     *
     * 매칭 순서:
     * 1. productId -> coupang_seller_product.product_id로 display_category_code 찾기
     * 2. display_category_code -> coupang_category에서 display_category_name 찾기
     * 3. display_category_name -> coupang_commission_rate에서 수수료율 찾기
     * 4. 없으면 parent_category_code로 올라가면서 반복
     * 5. 최상위까지 없으면 기본 수수료율 반환
     */
    public BigDecimal findCommissionRateByProductId(UUID tenantId, Long productId) {
        log.info("[DEBUG] findCommissionRateByProductId called: tenantId={}, productId={}", tenantId, productId);
        LocalDate today = LocalDate.now();

        // 1. productId로 seller product 조회
        Optional<CoupangSellerProduct> sellerProductOpt =
                sellerProductRepository.findByTenantIdAndProductId(tenantId, productId);
        log.info("[DEBUG] sellerProductOpt.isPresent={}", sellerProductOpt.isPresent());

        if (sellerProductOpt.isEmpty()) {
            log.warn("상품을 찾을 수 없습니다. tenantId={}, productId={}", tenantId, productId);
            return getDefaultCommissionRate(today);
        }

        CoupangSellerProduct sellerProduct = sellerProductOpt.get();
        Long displayCategoryCode = sellerProduct.getDisplayCategoryCode();
        log.info("[DEBUG] displayCategoryCode={}", displayCategoryCode);

        if (displayCategoryCode == null) {
            log.warn("상품의 카테고리 코드가 없습니다. productId={}", productId);
            return getDefaultCommissionRate(today);
        }

        // 2. 카테고리 계층을 따라가며 수수료율 찾기
        BigDecimal rate = findCommissionRateByCategory(displayCategoryCode, today);
        log.info("[DEBUG] Final commission rate for productId={}: {}", productId, rate);
        return rate;
    }

    /**
     * display_category_code로 수수료율 조회 (카테고리 계층을 따라 상위로 올라가며 검색)
     */
    public BigDecimal findCommissionRateByCategory(Long displayCategoryCode, LocalDate targetDate) {
        Long currentCategoryCode = displayCategoryCode;
        int maxDepth = 10; // 무한 루프 방지
        int depth = 0;

        while (currentCategoryCode != null && depth < maxDepth) {
            // 카테고리 조회
            Optional<CoupangCategory> categoryOpt =
                    categoryRepository.findByDisplayCategoryCode(currentCategoryCode);

            if (categoryOpt.isEmpty()) {
                log.warn("카테고리를 찾을 수 없습니다. displayCategoryCode={}", currentCategoryCode);
                break;
            }

            CoupangCategory category = categoryOpt.get();
            String categoryName = category.getDisplayCategoryName();

            // 카테고리명으로 수수료율 조회
            Optional<CoupangCommissionRate> rateOpt =
                    commissionRateRepository.findByCategoryNameAndEffectiveDate(categoryName, targetDate);

            if (rateOpt.isPresent()) {
                log.debug("수수료율 찾음. categoryName={}, rate={}",
                        categoryName, rateOpt.get().getCommissionRate());
                return rateOpt.get().getCommissionRate();
            }

            // 없으면 부모 카테고리로 이동
            currentCategoryCode = category.getParentCategoryCode();
            depth++;
        }

        // 최상위까지 찾지 못하면 기본 수수료율 반환
        log.info("카테고리 계층에서 수수료율을 찾지 못함. displayCategoryCode={}, 기본 수수료율 사용",
                displayCategoryCode);
        return getDefaultCommissionRate(targetDate);
    }

    /**
     * 기본 수수료율 조회
     */
    public BigDecimal getDefaultCommissionRate(LocalDate targetDate) {
        return commissionRateRepository.findDefaultRate(targetDate)
                .map(CoupangCommissionRate::getCommissionRate)
                .orElseGet(() -> {
                    log.error("기본 수수료율이 설정되지 않았습니다.");
                    return BigDecimal.ZERO;
                });
    }

    /**
     * 수수료율 정보 전체 조회 (displayCategoryCode 기준)
     */
    public Optional<CoupangCommissionRate> findCommissionRateDetailByProductId(UUID tenantId, Long productId) {
        LocalDate today = LocalDate.now();

        Optional<CoupangSellerProduct> sellerProductOpt =
                sellerProductRepository.findByTenantIdAndProductId(tenantId, productId);

        if (sellerProductOpt.isEmpty()) {
            return Optional.empty();
        }

        Long displayCategoryCode = sellerProductOpt.get().getDisplayCategoryCode();
        if (displayCategoryCode == null) {
            return commissionRateRepository.findDefaultRate(today);
        }

        return findCommissionRateDetailByCategory(displayCategoryCode, today);
    }

    /**
     * 수수료율 상세 정보 조회 (카테고리 계층을 따라 검색)
     */
    public Optional<CoupangCommissionRate> findCommissionRateDetailByCategory(Long displayCategoryCode, LocalDate targetDate) {
        Long currentCategoryCode = displayCategoryCode;
        int maxDepth = 10;
        int depth = 0;

        while (currentCategoryCode != null && depth < maxDepth) {
            Optional<CoupangCategory> categoryOpt =
                    categoryRepository.findByDisplayCategoryCode(currentCategoryCode);

            if (categoryOpt.isEmpty()) {
                break;
            }

            CoupangCategory category = categoryOpt.get();
            Optional<CoupangCommissionRate> rateOpt =
                    commissionRateRepository.findByCategoryNameAndEffectiveDate(
                            category.getDisplayCategoryName(), targetDate);

            if (rateOpt.isPresent()) {
                return rateOpt;
            }

            currentCategoryCode = category.getParentCategoryCode();
            depth++;
        }

        return commissionRateRepository.findDefaultRate(targetDate);
    }
}
