package com.mhub.marketplace.service;

import com.mhub.common.exception.BusinessException;
import com.mhub.common.exception.ErrorCodes;
import com.mhub.core.domain.entity.TenantMarketplaceCredential;
import com.mhub.core.domain.repository.TenantMarketplaceCredentialRepository;
import com.mhub.core.marketplace.dto.ConnectionTestResponse;
import com.mhub.core.marketplace.dto.MarketplaceCredentialRequest;
import com.mhub.core.marketplace.dto.MarketplaceCredentialResponse;
import com.mhub.core.tenant.TenantContext;
import com.mhub.marketplace.adapter.MarketplaceAdapter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class MarketplaceCredentialService {

    private final TenantMarketplaceCredentialRepository credentialRepository;
    private final MarketplaceAdapterFactory adapterFactory;

    @Transactional(readOnly = true)
    public List<MarketplaceCredentialResponse> getCredentials() {
        UUID tenantId = TenantContext.requireTenantId();
        return credentialRepository.findByTenantIdAndActiveTrue(tenantId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public MarketplaceCredentialResponse getCredential(UUID id) {
        return toResponse(findCredentialOrThrow(id));
    }

    @Transactional
    public MarketplaceCredentialResponse createCredential(MarketplaceCredentialRequest request) {
        UUID tenantId = TenantContext.requireTenantId();

        credentialRepository.findByTenantIdAndMarketplaceType(tenantId, request.getMarketplaceType())
                .filter(TenantMarketplaceCredential::getActive)
                .ifPresent(existing -> {
                    throw new BusinessException(ErrorCodes.MARKETPLACE_CREDENTIAL_DUPLICATE,
                            "이미 등록된 마켓플레이스입니다: " + request.getMarketplaceType());
                });

        TenantMarketplaceCredential credential = TenantMarketplaceCredential.builder()
                .tenantId(tenantId)
                .marketplaceType(request.getMarketplaceType())
                .sellerId(request.getSellerId())
                .clientId(request.getClientId())
                .clientSecret(request.getClientSecret())
                .active(true)
                .build();

        return toResponse(credentialRepository.save(credential));
    }

    @Transactional
    public MarketplaceCredentialResponse updateCredential(UUID id, MarketplaceCredentialRequest request) {
        TenantMarketplaceCredential credential = findCredentialOrThrow(id);

        credential.setSellerId(request.getSellerId());
        credential.setClientId(request.getClientId());
        credential.setClientSecret(request.getClientSecret());

        return toResponse(credentialRepository.save(credential));
    }

    @Transactional
    public void deleteCredential(UUID id) {
        TenantMarketplaceCredential credential = findCredentialOrThrow(id);
        credential.setActive(false);
        credentialRepository.save(credential);
    }

    public ConnectionTestResponse testConnection(MarketplaceCredentialRequest request) {
        TenantMarketplaceCredential credential = TenantMarketplaceCredential.builder()
                .marketplaceType(request.getMarketplaceType())
                .sellerId(request.getSellerId())
                .clientId(request.getClientId())
                .clientSecret(request.getClientSecret())
                .build();

        return doTestConnection(credential);
    }

    public ConnectionTestResponse testConnectionById(UUID id) {
        TenantMarketplaceCredential credential = findCredentialOrThrow(id);
        return doTestConnection(credential);
    }

    private ConnectionTestResponse doTestConnection(TenantMarketplaceCredential credential) {
        try {
            MarketplaceAdapter adapter = adapterFactory.getAdapter(credential.getMarketplaceType());
            adapter.testConnection(credential);
            return ConnectionTestResponse.builder()
                    .connected(true)
                    .message("연결 성공")
                    .build();
        } catch (BusinessException e) {
            return ConnectionTestResponse.builder()
                    .connected(false)
                    .message(e.getMessage())
                    .build();
        } catch (Exception e) {
            log.error("Connection test failed for {}", credential.getMarketplaceType(), e);
            return ConnectionTestResponse.builder()
                    .connected(false)
                    .message("연결 실패: " + e.getMessage())
                    .build();
        }
    }

    private TenantMarketplaceCredential findCredentialOrThrow(UUID id) {
        UUID tenantId = TenantContext.requireTenantId();
        return credentialRepository.findById(id)
                .filter(c -> c.getTenantId().equals(tenantId))
                .filter(TenantMarketplaceCredential::getActive)
                .orElseThrow(() -> new BusinessException(ErrorCodes.MARKETPLACE_CREDENTIAL_NOT_FOUND,
                        "마켓플레이스 자격증명을 찾을 수 없습니다"));
    }

    private MarketplaceCredentialResponse toResponse(TenantMarketplaceCredential credential) {
        return MarketplaceCredentialResponse.builder()
                .id(credential.getId())
                .marketplaceType(credential.getMarketplaceType())
                .sellerId(credential.getSellerId())
                .active(credential.getActive())
                .hasClientId(credential.getClientId() != null && !credential.getClientId().isEmpty())
                .hasClientSecret(credential.getClientSecret() != null && !credential.getClientSecret().isEmpty())
                .createdAt(credential.getCreatedAt())
                .updatedAt(credential.getUpdatedAt())
                .build();
    }
}
