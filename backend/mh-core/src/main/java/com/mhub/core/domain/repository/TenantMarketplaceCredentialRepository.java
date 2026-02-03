package com.mhub.core.domain.repository;

import com.mhub.core.domain.entity.TenantMarketplaceCredential;
import com.mhub.core.domain.enums.MarketplaceType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TenantMarketplaceCredentialRepository extends JpaRepository<TenantMarketplaceCredential, UUID> {
    List<TenantMarketplaceCredential> findByTenantIdAndActiveTrue(UUID tenantId);
    Optional<TenantMarketplaceCredential> findByTenantIdAndMarketplaceType(UUID tenantId, MarketplaceType marketplaceType);
    Optional<TenantMarketplaceCredential> findByTenantIdAndMarketplaceTypeAndActiveTrue(UUID tenantId, MarketplaceType marketplaceType);
    List<TenantMarketplaceCredential> findByMarketplaceTypeAndActiveTrue(MarketplaceType marketplaceType);
}
