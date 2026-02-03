package com.mhub.erp.service;

import com.mhub.common.exception.BusinessException;
import com.mhub.common.exception.ErrorCodes;
import com.mhub.core.domain.entity.TenantErpConfig;
import com.mhub.core.domain.repository.TenantErpConfigRepository;
import com.mhub.core.erp.dto.ErpConfigRequest;
import com.mhub.core.erp.dto.ErpConfigResponse;
import com.mhub.core.erp.dto.ErpConnectionTestRequest;
import com.mhub.core.marketplace.dto.ConnectionTestResponse;
import com.mhub.core.tenant.TenantContext;
import com.mhub.erp.adapter.ErpAdapter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ErpConfigService {

    private final TenantErpConfigRepository erpConfigRepository;
    private final ErpAdapterFactory adapterFactory;

    @Transactional(readOnly = true)
    public List<ErpConfigResponse> getConfigs() {
        UUID tenantId = TenantContext.requireTenantId();
        return erpConfigRepository.findByTenantIdAndActiveTrue(tenantId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ErpConfigResponse getConfig(UUID id) {
        return toResponse(findConfigOrThrow(id));
    }

    @Transactional
    public ErpConfigResponse createConfig(ErpConfigRequest request) {
        UUID tenantId = TenantContext.requireTenantId();

        erpConfigRepository.findByTenantIdAndErpType(tenantId, request.getErpType())
                .filter(TenantErpConfig::getActive)
                .ifPresent(existing -> {
                    throw new BusinessException(ErrorCodes.ERP_CONFIG_DUPLICATE,
                            "이미 등록된 ERP입니다: " + request.getErpType());
                });

        TenantErpConfig config = TenantErpConfig.builder()
                .tenantId(tenantId)
                .erpType(request.getErpType())
                .companyCode(request.getCompanyCode())
                .userId(request.getUserId())
                .apiKey(request.getApiKey())
                .fieldMapping(request.getFieldMapping())
                .extraConfig(request.getExtraConfig())
                .active(true)
                .build();

        return toResponse(erpConfigRepository.save(config));
    }

    @Transactional
    public ErpConfigResponse updateConfig(UUID id, ErpConfigRequest request) {
        TenantErpConfig config = findConfigOrThrow(id);

        config.setCompanyCode(request.getCompanyCode());
        config.setUserId(request.getUserId());
        config.setApiKey(request.getApiKey());
        config.setFieldMapping(request.getFieldMapping());
        config.setExtraConfig(request.getExtraConfig());

        return toResponse(erpConfigRepository.save(config));
    }

    @Transactional
    public void deleteConfig(UUID id) {
        TenantErpConfig config = findConfigOrThrow(id);
        config.setActive(false);
        erpConfigRepository.save(config);
    }

    public ConnectionTestResponse testConnection(ErpConnectionTestRequest request) {
        TenantErpConfig config = TenantErpConfig.builder()
                .erpType(request.getErpType())
                .companyCode(request.getCompanyCode())
                .userId(request.getUserId())
                .apiKey(request.getApiKey())
                .build();

        return doTestConnection(config);
    }

    public ConnectionTestResponse testConnectionById(UUID id) {
        TenantErpConfig config = findConfigOrThrow(id);
        return doTestConnection(config);
    }

    private ConnectionTestResponse doTestConnection(TenantErpConfig config) {
        try {
            ErpAdapter adapter = adapterFactory.getAdapter(config.getErpType());
            adapter.testConnection(config);
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
            log.error("ERP connection test failed for {}", config.getErpType(), e);
            return ConnectionTestResponse.builder()
                    .connected(false)
                    .message("연결 실패: " + e.getMessage())
                    .build();
        }
    }

    private TenantErpConfig findConfigOrThrow(UUID id) {
        UUID tenantId = TenantContext.requireTenantId();
        return erpConfigRepository.findByIdAndTenantId(id, tenantId)
                .filter(TenantErpConfig::getActive)
                .orElseThrow(() -> new BusinessException(ErrorCodes.ERP_CONFIG_NOT_FOUND,
                        "ERP 설정을 찾을 수 없습니다"));
    }

    private ErpConfigResponse toResponse(TenantErpConfig config) {
        return ErpConfigResponse.builder()
                .id(config.getId())
                .erpType(config.getErpType())
                .companyCode(config.getCompanyCode())
                .userId(config.getUserId())
                .active(config.getActive())
                .hasApiKey(config.getApiKey() != null && !config.getApiKey().isEmpty())
                .fieldMapping(config.getFieldMapping())
                .extraConfig(config.getExtraConfig())
                .createdAt(config.getCreatedAt())
                .updatedAt(config.getUpdatedAt())
                .build();
    }
}
