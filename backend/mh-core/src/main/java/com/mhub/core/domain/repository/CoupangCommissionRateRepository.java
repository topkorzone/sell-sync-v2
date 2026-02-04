package com.mhub.core.domain.repository;

import com.mhub.core.domain.entity.CoupangCommissionRate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CoupangCommissionRateRepository extends JpaRepository<CoupangCommissionRate, UUID> {

    /**
     * 카테고리 ID와 적용일 기준으로 유효한 수수료율 조회
     */
    @Query("SELECT c FROM CoupangCommissionRate c " +
           "WHERE c.categoryId = :categoryId " +
           "AND c.effectiveFrom <= :targetDate " +
           "AND (c.effectiveTo IS NULL OR c.effectiveTo >= :targetDate)")
    Optional<CoupangCommissionRate> findByCategoryIdAndEffectiveDate(
            @Param("categoryId") String categoryId,
            @Param("targetDate") LocalDate targetDate);

    /**
     * 카테고리명으로 유효한 수수료율 조회 (부분 일치)
     */
    @Query("SELECT c FROM CoupangCommissionRate c " +
           "WHERE c.categoryName LIKE %:categoryName% " +
           "AND c.effectiveFrom <= :targetDate " +
           "AND (c.effectiveTo IS NULL OR c.effectiveTo >= :targetDate)")
    List<CoupangCommissionRate> findByCategoryNameContainingAndEffectiveDate(
            @Param("categoryName") String categoryName,
            @Param("targetDate") LocalDate targetDate);

    /**
     * 현재 유효한 모든 수수료율 조회
     */
    @Query("SELECT c FROM CoupangCommissionRate c " +
           "WHERE c.effectiveFrom <= :targetDate " +
           "AND (c.effectiveTo IS NULL OR c.effectiveTo >= :targetDate) " +
           "ORDER BY c.categoryName")
    List<CoupangCommissionRate> findAllEffectiveRates(@Param("targetDate") LocalDate targetDate);

    /**
     * 기본 수수료율 조회 (DEFAULT 카테고리)
     */
    default Optional<CoupangCommissionRate> findDefaultRate(LocalDate targetDate) {
        return findByCategoryIdAndEffectiveDate("DEFAULT", targetDate);
    }

    /**
     * 카테고리 ID로 현재 유효한 수수료율 조회 (없으면 기본 수수료율 반환)
     */
    default CoupangCommissionRate findRateOrDefault(String categoryId, LocalDate targetDate) {
        return findByCategoryIdAndEffectiveDate(categoryId, targetDate)
                .orElseGet(() -> findDefaultRate(targetDate)
                        .orElseThrow(() -> new IllegalStateException("기본 수수료율이 설정되지 않았습니다.")));
    }

    /**
     * displayCategoryCode로 유효한 수수료율 조회
     */
    @Query("SELECT c FROM CoupangCommissionRate c " +
           "WHERE c.displayCategoryCode = :displayCategoryCode " +
           "AND c.effectiveFrom <= :targetDate " +
           "AND (c.effectiveTo IS NULL OR c.effectiveTo >= :targetDate)")
    Optional<CoupangCommissionRate> findByDisplayCategoryCodeAndEffectiveDate(
            @Param("displayCategoryCode") Long displayCategoryCode,
            @Param("targetDate") LocalDate targetDate);

    /**
     * displayCategoryCode로 수수료율 조회 (없으면 기본 수수료율 반환)
     */
    default CoupangCommissionRate findRateByDisplayCategoryCodeOrDefault(Long displayCategoryCode, LocalDate targetDate) {
        if (displayCategoryCode == null) {
            return findRateOrDefault("DEFAULT", targetDate);
        }
        return findByDisplayCategoryCodeAndEffectiveDate(displayCategoryCode, targetDate)
                .orElseGet(() -> findDefaultRate(targetDate)
                        .orElseThrow(() -> new IllegalStateException("기본 수수료율이 설정되지 않았습니다.")));
    }

    /**
     * 카테고리명으로 유효한 수수료율 조회 (정확 일치, 가장 최근 것)
     */
    @Query(value = "SELECT * FROM coupang_commission_rate c " +
           "WHERE c.category_name = :categoryName " +
           "AND c.effective_from <= :targetDate " +
           "AND (c.effective_to IS NULL OR c.effective_to >= :targetDate) " +
           "ORDER BY c.effective_from DESC LIMIT 1",
           nativeQuery = true)
    Optional<CoupangCommissionRate> findByCategoryNameAndEffectiveDate(
            @Param("categoryName") String categoryName,
            @Param("targetDate") LocalDate targetDate);
}
