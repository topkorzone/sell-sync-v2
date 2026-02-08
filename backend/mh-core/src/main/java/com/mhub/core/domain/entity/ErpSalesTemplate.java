package com.mhub.core.domain.entity;

import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Type;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "erp_sales_template")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ErpSalesTemplate extends BaseEntity {

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "erp_config_id", nullable = false)
    private UUID erpConfigId;

    @Type(JsonType.class)
    @Column(name = "marketplace_headers", columnDefinition = "jsonb")
    @Builder.Default
    private Map<String, Object> marketplaceHeaders = Map.of();

    @Type(JsonType.class)
    @Column(name = "default_header", columnDefinition = "jsonb")
    @Builder.Default
    private Map<String, Object> defaultHeader = Map.of();

    @Type(JsonType.class)
    @Column(name = "line_product_sale", columnDefinition = "jsonb")
    @Builder.Default
    private Map<String, Object> lineProductSale = Map.of();

    @Type(JsonType.class)
    @Column(name = "line_delivery_fee", columnDefinition = "jsonb")
    @Builder.Default
    private Map<String, Object> lineDeliveryFee = Map.of();

    @Type(JsonType.class)
    @Column(name = "line_sales_commission", columnDefinition = "jsonb")
    @Builder.Default
    private Map<String, Object> lineSalesCommission = Map.of();

    @Type(JsonType.class)
    @Column(name = "line_delivery_commission", columnDefinition = "jsonb")
    @Builder.Default
    private Map<String, Object> lineDeliveryCommission = Map.of();

    /**
     * 추가 항목 라인 템플릿 목록
     * 전표 생성 시 자동으로 포함되는 추가 라인들
     */
    @Type(JsonType.class)
    @Column(name = "additional_lines", columnDefinition = "jsonb")
    @Builder.Default
    private List<Map<String, Object>> additionalLines = List.of();

    /**
     * 글로벌 필드 매핑 목록
     * 모든 라인에 공통으로 적용되는 ECount 추가 필드 설정
     * 각 매핑에는 lineTypes 필드로 적용 대상 라인 타입을 지정할 수 있음
     */
    @Type(JsonType.class)
    @Column(name = "global_field_mappings", columnDefinition = "jsonb")
    @Builder.Default
    private List<Map<String, Object>> globalFieldMappings = List.of();

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;
}
