package com.mhub.erp.service;

import com.mhub.common.exception.BusinessException;
import com.mhub.common.exception.ErrorCodes;
import com.mhub.core.domain.entity.ErpSalesTemplate;
import com.mhub.core.domain.repository.ErpSalesTemplateRepository;
import com.mhub.core.domain.repository.TenantErpConfigRepository;
import com.mhub.core.erp.dto.AdditionalLineTemplateDto;
import com.mhub.core.erp.dto.ErpSalesTemplateRequest;
import com.mhub.core.erp.dto.ErpSalesTemplateResponse;
import com.mhub.core.erp.dto.SalesLineTemplateDto;
import com.mhub.core.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ErpSalesTemplateService {

    private final ErpSalesTemplateRepository templateRepository;
    private final TenantErpConfigRepository erpConfigRepository;

    @Transactional(readOnly = true)
    public ErpSalesTemplateResponse getTemplate(UUID erpConfigId) {
        UUID tenantId = TenantContext.requireTenantId();
        erpConfigRepository.findByIdAndTenantId(erpConfigId, tenantId)
                .orElseThrow(() -> new BusinessException(ErrorCodes.ERP_CONFIG_NOT_FOUND, "ERP 설정을 찾을 수 없습니다"));

        return templateRepository.findByTenantIdAndErpConfigId(tenantId, erpConfigId)
                .map(ErpSalesTemplateResponse::from)
                .orElse(null);
    }

    @Transactional
    public ErpSalesTemplateResponse saveTemplate(UUID erpConfigId, ErpSalesTemplateRequest request) {
        UUID tenantId = TenantContext.requireTenantId();
        erpConfigRepository.findByIdAndTenantId(erpConfigId, tenantId)
                .orElseThrow(() -> new BusinessException(ErrorCodes.ERP_CONFIG_NOT_FOUND, "ERP 설정을 찾을 수 없습니다"));

        ErpSalesTemplate template = templateRepository.findByTenantIdAndErpConfigId(tenantId, erpConfigId)
                .orElseGet(() -> ErpSalesTemplate.builder()
                        .tenantId(tenantId)
                        .erpConfigId(erpConfigId)
                        .build());

        template.setMarketplaceHeaders(toObjectMap(request.marketplaceHeaders()));
        template.setDefaultHeader(toStringKeyMap(request.defaultHeader()));
        template.setLineProductSale(lineToMap(request.lineProductSale()));
        template.setLineDeliveryFee(lineToMap(request.lineDeliveryFee()));
        template.setLineSalesCommission(lineToMap(request.lineSalesCommission()));
        template.setLineDeliveryCommission(lineToMap(request.lineDeliveryCommission()));
        template.setAdditionalLines(additionalLinesToMapList(request.additionalLines()));
        template.setGlobalFieldMappings(request.globalFieldMappings() != null ? request.globalFieldMappings() : List.of());
        template.setActive(request.active());

        templateRepository.save(template);
        log.info("Saved ERP sales template for tenant={}, erpConfig={}", tenantId, erpConfigId);
        return ErpSalesTemplateResponse.from(template);
    }

    @Transactional
    public void deleteTemplate(UUID erpConfigId) {
        UUID tenantId = TenantContext.requireTenantId();
        ErpSalesTemplate template = templateRepository.findByTenantIdAndErpConfigId(tenantId, erpConfigId)
                .orElseThrow(() -> new BusinessException(ErrorCodes.ERP_TEMPLATE_NOT_FOUND, "전표 템플릿이 설정되지 않았습니다"));
        templateRepository.delete(template);
        log.info("Deleted ERP sales template for tenant={}, erpConfig={}", tenantId, erpConfigId);
    }

    private Map<String, Object> toObjectMap(Map<String, Map<String, String>> source) {
        Map<String, Object> result = new HashMap<>();
        if (source != null) {
            source.forEach((k, v) -> result.put(k, new HashMap<>(v)));
        }
        return result;
    }

    private Map<String, Object> toStringKeyMap(Map<String, String> source) {
        Map<String, Object> result = new HashMap<>();
        if (source != null) {
            result.putAll(source);
        }
        return result;
    }

    private Map<String, Object> lineToMap(SalesLineTemplateDto dto) {
        Map<String, Object> map = new HashMap<>();
        map.put("prodCd", dto.prodCd());
        map.put("prodDes", dto.prodDes());
        map.put("qtySource", dto.qtySource());
        map.put("priceSource", dto.priceSource());
        map.put("vatCalculation", dto.vatCalculation());
        map.put("negateAmount", dto.negateAmount());
        map.put("skipIfZero", dto.skipIfZero());
        map.put("remarks", dto.remarks());
        map.put("extraFields", dto.extraFields());
        // 마켓별 품목코드 저장
        if (dto.marketplaceProdCds() != null && !dto.marketplaceProdCds().isEmpty()) {
            map.put("marketplaceProdCds", dto.marketplaceProdCds());
        }
        return map;
    }

    private List<Map<String, Object>> additionalLinesToMapList(List<AdditionalLineTemplateDto> additionalLines) {
        if (additionalLines == null || additionalLines.isEmpty()) {
            return List.of();
        }
        return additionalLines.stream()
                .map(dto -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("prodCd", dto.prodCd());
                    map.put("prodDes", dto.prodDes());
                    map.put("whCd", dto.whCd());
                    map.put("qty", dto.qty());
                    map.put("unitPrice", dto.unitPrice());
                    map.put("vatCalculation", dto.vatCalculation());
                    map.put("negateAmount", dto.negateAmount());
                    map.put("remarks", dto.remarks());
                    map.put("enabled", dto.enabled());
                    return map;
                })
                .collect(Collectors.toList());
    }
}
