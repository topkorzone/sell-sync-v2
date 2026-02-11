package com.mhub.core.service;

import com.mhub.core.domain.entity.Tenant;
import com.mhub.core.domain.entity.TenantMarketplaceCredential;
import com.mhub.core.domain.enums.MarketplaceType;
import com.mhub.core.domain.repository.TenantMarketplaceCredentialRepository;
import com.mhub.core.domain.repository.TenantRepository;
import com.mhub.common.exception.BusinessException;
import com.mhub.common.exception.ErrorCodes;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.UUID;

@Slf4j @Service @RequiredArgsConstructor
public class TenantService {
    private final TenantRepository tenantRepository;
    private final TenantMarketplaceCredentialRepository credentialRepository;

    @Transactional(readOnly = true)
    public Tenant getTenant(UUID tenantId) {
        return tenantRepository.findById(tenantId).orElseThrow(() -> new BusinessException(ErrorCodes.TENANT_NOT_FOUND, "Tenant not found: " + tenantId));
    }

    @Transactional(readOnly = true)
    public List<Tenant> getActiveTenants() { return tenantRepository.findByActiveTrue(); }

    @Transactional(readOnly = true)
    public List<TenantMarketplaceCredential> getActiveCredentials(UUID tenantId) { return credentialRepository.findByTenantIdAndActiveTrue(tenantId); }

    @Transactional(readOnly = true)
    public TenantMarketplaceCredential getCredential(UUID tenantId, MarketplaceType marketplaceType) {
        return credentialRepository.findByTenantIdAndMarketplaceType(tenantId, marketplaceType).orElseThrow(() -> new BusinessException(ErrorCodes.MARKETPLACE_AUTH_FAILED, "No credential for tenant " + tenantId + " marketplace " + marketplaceType));
    }

    @Transactional
    public Tenant updateTenant(UUID tenantId, String companyName, String businessNumber,
                               String contactName, String contactEmail, String contactPhone) {
        Tenant tenant = getTenant(tenantId);
        if (companyName != null) tenant.setCompanyName(companyName);
        if (businessNumber != null) tenant.setBusinessNumber(businessNumber);
        if (contactName != null) tenant.setContactName(contactName);
        if (contactEmail != null) tenant.setContactEmail(contactEmail);
        if (contactPhone != null) tenant.setContactPhone(contactPhone);
        return tenantRepository.save(tenant);
    }
}
