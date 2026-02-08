package com.mhub.core.domain.entity;

import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Type;

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

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;
}
