package com.mhub.core.erp.dto;

import com.mhub.core.domain.enums.ErpFieldPosition;
import com.mhub.core.domain.enums.ErpFieldValueType;
import com.mhub.core.domain.enums.ErpLineType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ErpFieldMappingRequest {

    private UUID id;

    /**
     * ECount API 변수명 (WH_CD, CUST, TTL_CTT 등)
     */
    private String fieldName;

    /**
     * 필드 위치: HEADER or LINE
     */
    private ErpFieldPosition fieldPosition;

    /**
     * LINE일 경우 적용 대상
     */
    private ErpLineType lineType;

    /**
     * 값 유형: FIXED, MARKETPLACE, ORDER_FIELD
     */
    private ErpFieldValueType valueType;

    /**
     * 고정값
     */
    private String fixedValue;

    /**
     * 마켓별 값 {"COUPANG":"C01","NAVER":"C02"}
     */
    private Map<String, String> marketplaceValues;

    /**
     * 주문정보 템플릿 "{marketplaceName}-{orderId}"
     */
    private String orderFieldTemplate;

    /**
     * 표시 순서
     */
    private Integer displayOrder;

    /**
     * 설명
     */
    private String description;

    /**
     * 활성 여부
     */
    private Boolean active;
}
