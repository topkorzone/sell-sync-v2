package com.mhub.core.domain.repository;

import com.mhub.core.domain.entity.CoupangCommissionRate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
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
}
