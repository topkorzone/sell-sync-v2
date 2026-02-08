package com.mhub.core.domain.entity;

import com.mhub.core.domain.enums.ErpFieldPosition;
import com.mhub.core.domain.enums.ErpFieldValueType;
import com.mhub.core.domain.enums.ErpLineType;
import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Type;

import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "erp_field_mapping")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ErpFieldMapping extends BaseEntity {

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "erp_config_id", nullable = false)
    private UUID erpConfigId;

    /**
     * ECount API 변수명 (WH_CD, CUST, TTL_CTT, U_MEMO1 등)
     */
    @Column(name = "field_name", nullable = false, length = 50)
    private String fieldName;

    /**
     * 필드 위치: HEADER(상단) or LINE(하단)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "field_position", nullable = false, length = 20)
    private ErpFieldPosition fieldPosition;

    /**
     * LINE일 경우 적용 대상 라인 타입
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "line_type", length = 30)
    private ErpLineType lineType;

    /**
     * 값 유형: FIXED, MARKETPLACE, ORDER_FIELD
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "value_type", nullable = false, length = 20)
    private ErpFieldValueType valueType;

    /**
     * 고정값 (value_type=FIXED)
     */
    @Column(name = "fixed_value", length = 500)
    private String fixedValue;

    /**
     * 마켓별 값 (value_type=MARKETPLACE)
     * {"COUPANG":"C01","NAVER":"C02"}
     */
    @Type(JsonType.class)
    @Column(name = "marketplace_values", columnDefinition = "jsonb")
    private Map<String, String> marketplaceValues;

    /**
     * 주문정보 템플릿 (value_type=ORDER_FIELD)
     * "{marketplaceName}-{orderId}"
     */
    @Column(name = "order_field_template", length = 500)
    private String orderFieldTemplate;

    @Column(name = "display_order")
    @Builder.Default
    private Integer displayOrder = 0;

    @Column(name = "description", length = 200)
    private String description;

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;
}
