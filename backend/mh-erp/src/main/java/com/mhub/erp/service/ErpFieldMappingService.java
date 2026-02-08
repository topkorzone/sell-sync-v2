package com.mhub.erp.service;

import com.mhub.common.exception.BusinessException;
import com.mhub.common.exception.ErrorCodes;
import com.mhub.core.domain.entity.ErpFieldMapping;
import com.mhub.core.domain.enums.ErpFieldPosition;
import com.mhub.core.domain.enums.ErpLineType;
import com.mhub.core.domain.repository.ErpFieldMappingRepository;
import com.mhub.core.erp.dto.ErpFieldMappingRequest;
import com.mhub.core.erp.dto.ErpFieldMappingResponse;
import com.mhub.core.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ErpFieldMappingService {

    private final ErpFieldMappingRepository fieldMappingRepository;

    /**
     * ERP 설정의 모든 필드 매핑 조회
     */
    @Transactional(readOnly = true)
    public List<ErpFieldMappingResponse> getFieldMappings(UUID erpConfigId) {
        UUID tenantId = TenantContext.requireTenantId();
        return fieldMappingRepository.findByTenantIdAndErpConfigIdOrderByDisplayOrder(tenantId, erpConfigId)
                .stream()
                .map(ErpFieldMappingResponse::from)
                .toList();
    }

    /**
     * 활성 필드 매핑만 조회 (전표 생성용)
     */
    @Transactional(readOnly = true)
    public List<ErpFieldMapping> getActiveFieldMappings(UUID erpConfigId) {
        UUID tenantId = TenantContext.requireTenantId();
        return fieldMappingRepository.findActiveByTenantAndConfig(tenantId, erpConfigId);
    }

    /**
     * 필드 매핑 생성
     */
    @Transactional
    public ErpFieldMappingResponse createFieldMapping(UUID erpConfigId, ErpFieldMappingRequest request) {
        UUID tenantId = TenantContext.requireTenantId();

        // 중복 체크
        if (fieldMappingRepository.existsByTenantIdAndErpConfigIdAndFieldNameAndFieldPositionAndActiveTrue(
                tenantId, erpConfigId, request.getFieldName(), request.getFieldPosition())) {
            throw new BusinessException(ErrorCodes.ERP_CONFIG_DUPLICATE,
                    "이미 동일한 필드가 등록되어 있습니다: " + request.getFieldName());
        }

        ErpFieldMapping mapping = ErpFieldMapping.builder()
                .tenantId(tenantId)
                .erpConfigId(erpConfigId)
                .fieldName(request.getFieldName())
                .fieldPosition(request.getFieldPosition())
                .lineType(request.getLineType())
                .valueType(request.getValueType())
                .fixedValue(request.getFixedValue())
                .marketplaceValues(request.getMarketplaceValues())
                .orderFieldTemplate(request.getOrderFieldTemplate())
                .displayOrder(request.getDisplayOrder() != null ? request.getDisplayOrder() : 0)
                .description(request.getDescription())
                .active(true)
                .build();

        ErpFieldMapping saved = fieldMappingRepository.save(mapping);
        log.info("Created ERP field mapping: {} for config {}", request.getFieldName(), erpConfigId);

        return ErpFieldMappingResponse.from(saved);
    }

    /**
     * 필드 매핑 수정
     */
    @Transactional
    public ErpFieldMappingResponse updateFieldMapping(UUID erpConfigId, UUID mappingId, ErpFieldMappingRequest request) {
        UUID tenantId = TenantContext.requireTenantId();

        ErpFieldMapping mapping = fieldMappingRepository.findById(mappingId)
                .orElseThrow(() -> new BusinessException(ErrorCodes.ERP_CONFIG_NOT_FOUND,
                        "필드 매핑을 찾을 수 없습니다: " + mappingId));

        if (!mapping.getTenantId().equals(tenantId)) {
            throw new BusinessException(ErrorCodes.AUTH_INSUFFICIENT_PERMISSION, "접근 권한이 없습니다.");
        }

        mapping.setFieldName(request.getFieldName());
        mapping.setFieldPosition(request.getFieldPosition());
        mapping.setLineType(request.getLineType());
        mapping.setValueType(request.getValueType());
        mapping.setFixedValue(request.getFixedValue());
        mapping.setMarketplaceValues(request.getMarketplaceValues());
        mapping.setOrderFieldTemplate(request.getOrderFieldTemplate());
        mapping.setDisplayOrder(request.getDisplayOrder() != null ? request.getDisplayOrder() : 0);
        mapping.setDescription(request.getDescription());
        if (request.getActive() != null) {
            mapping.setActive(request.getActive());
        }

        ErpFieldMapping saved = fieldMappingRepository.save(mapping);
        log.info("Updated ERP field mapping: {} for config {}", request.getFieldName(), erpConfigId);

        return ErpFieldMappingResponse.from(saved);
    }

    /**
     * 필드 매핑 삭제
     */
    @Transactional
    public void deleteFieldMapping(UUID erpConfigId, UUID mappingId) {
        UUID tenantId = TenantContext.requireTenantId();

        ErpFieldMapping mapping = fieldMappingRepository.findById(mappingId)
                .orElseThrow(() -> new BusinessException(ErrorCodes.ERP_CONFIG_NOT_FOUND,
                        "필드 매핑을 찾을 수 없습니다: " + mappingId));

        if (!mapping.getTenantId().equals(tenantId)) {
            throw new BusinessException(ErrorCodes.AUTH_INSUFFICIENT_PERMISSION, "접근 권한이 없습니다.");
        }

        fieldMappingRepository.delete(mapping);
        log.info("Deleted ERP field mapping: {} for config {}", mapping.getFieldName(), erpConfigId);
    }

    /**
     * 일괄 필드 매핑 저장 (기존 삭제 후 새로 생성)
     */
    @Transactional
    public List<ErpFieldMappingResponse> saveFieldMappings(UUID erpConfigId, List<ErpFieldMappingRequest> requests) {
        UUID tenantId = TenantContext.requireTenantId();

        // 기존 매핑 모두 삭제
        List<ErpFieldMapping> existing = fieldMappingRepository.findByTenantIdAndErpConfigIdOrderByDisplayOrder(tenantId, erpConfigId);
        fieldMappingRepository.deleteAll(existing);

        // 새로 생성
        List<ErpFieldMapping> mappings = requests.stream()
                .map(req -> ErpFieldMapping.builder()
                        .tenantId(tenantId)
                        .erpConfigId(erpConfigId)
                        .fieldName(req.getFieldName())
                        .fieldPosition(req.getFieldPosition())
                        .lineType(req.getLineType())
                        .valueType(req.getValueType())
                        .fixedValue(req.getFixedValue())
                        .marketplaceValues(req.getMarketplaceValues())
                        .orderFieldTemplate(req.getOrderFieldTemplate())
                        .displayOrder(req.getDisplayOrder() != null ? req.getDisplayOrder() : 0)
                        .description(req.getDescription())
                        .active(req.getActive() != null ? req.getActive() : true)
                        .build())
                .toList();

        List<ErpFieldMapping> saved = fieldMappingRepository.saveAll(mappings);
        log.info("Saved {} ERP field mappings for config {}", saved.size(), erpConfigId);

        return saved.stream().map(ErpFieldMappingResponse::from).toList();
    }

    /**
     * 사용 가능한 플레이스홀더 목록 반환 (UI 도움말용)
     */
    public List<PlaceholderInfo> getAvailablePlaceholders() {
        return List.of(
                new PlaceholderInfo("{orderId}", "마켓플레이스 주문번호"),
                new PlaceholderInfo("{productOrderId}", "상품주문번호"),
                new PlaceholderInfo("{marketplaceName}", "마켓명 (COUPANG, NAVER 등)"),
                new PlaceholderInfo("{marketplaceDisplayName}", "마켓 표시명 (쿠팡, 네이버 등)"),
                new PlaceholderInfo("{buyerName}", "주문자명"),
                new PlaceholderInfo("{receiverName}", "수령인명"),
                new PlaceholderInfo("{receiverPhone}", "수령인 연락처"),
                new PlaceholderInfo("{receiverAddress}", "배송지 주소"),
                new PlaceholderInfo("{orderDate}", "주문일자 (yyyyMMdd)"),
                new PlaceholderInfo("{productName}", "상품명 (라인 필드)"),
                new PlaceholderInfo("{optionName}", "옵션명 (라인 필드)"),
                new PlaceholderInfo("{quantity}", "수량 (라인 필드)"),
                new PlaceholderInfo("{totalAmount}", "총 주문금액"),
                new PlaceholderInfo("{deliveryFee}", "배송비")
        );
    }

    public record PlaceholderInfo(String placeholder, String description) {}
}
