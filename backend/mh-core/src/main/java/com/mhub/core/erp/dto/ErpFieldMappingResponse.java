package com.mhub.core.erp.dto;

import com.mhub.core.domain.entity.ErpFieldMapping;
import com.mhub.core.domain.enums.ErpFieldPosition;
import com.mhub.core.domain.enums.ErpFieldValueType;
import com.mhub.core.domain.enums.ErpLineType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ErpFieldMappingResponse {

    private UUID id;
    private UUID erpConfigId;
    private String fieldName;
    private ErpFieldPosition fieldPosition;
    private ErpLineType lineType;
    private ErpFieldValueType valueType;
    private String fixedValue;
    private Map<String, String> marketplaceValues;
    private String orderFieldTemplate;
    private Integer displayOrder;
    private String description;
    private Boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static ErpFieldMappingResponse from(ErpFieldMapping entity) {
        return ErpFieldMappingResponse.builder()
                .id(entity.getId())
                .erpConfigId(entity.getErpConfigId())
                .fieldName(entity.getFieldName())
                .fieldPosition(entity.getFieldPosition())
                .lineType(entity.getLineType())
                .valueType(entity.getValueType())
                .fixedValue(entity.getFixedValue())
                .marketplaceValues(entity.getMarketplaceValues())
                .orderFieldTemplate(entity.getOrderFieldTemplate())
                .displayOrder(entity.getDisplayOrder())
                .description(entity.getDescription())
                .active(entity.getActive())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
