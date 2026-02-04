package com.mhub.core.domain.repository;

import com.mhub.core.domain.entity.CoupangCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CoupangCategoryRepository extends JpaRepository<CoupangCategory, UUID> {

    Optional<CoupangCategory> findByDisplayCategoryCode(Long displayCategoryCode);

    List<CoupangCategory> findByDepthLevel(Integer depthLevel);

    List<CoupangCategory> findByParentCategoryCode(Long parentCategoryCode);

    List<CoupangCategory> findByRootCategoryCode(Long rootCategoryCode);

    @Query("SELECT DISTINCT c.rootCategoryCode FROM CoupangCategory c WHERE c.rootCategoryCode IS NOT NULL")
    List<Long> findDistinctRootCategoryCodes();

    @Query("SELECT c FROM CoupangCategory c WHERE c.depthLevel = 1 ORDER BY c.displayCategoryName")
    List<CoupangCategory> findAllRootCategories();

    @Query("SELECT c.rootCategoryCode FROM CoupangCategory c WHERE c.displayCategoryCode = :displayCategoryCode")
    Optional<Long> findRootCategoryCodeByDisplayCategoryCode(@Param("displayCategoryCode") Long displayCategoryCode);

    boolean existsByDisplayCategoryCode(Long displayCategoryCode);
}
