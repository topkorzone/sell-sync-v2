package com.mhub.core.domain.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Type;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "erp_item")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ErpItem extends BaseEntity {

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "erp_config_id", nullable = false)
    private TenantErpConfig erpConfig;

    @Column(name = "prod_cd", nullable = false, length = 100)
    private String prodCd;

    @Column(name = "prod_des", nullable = false, length = 500)
    private String prodDes;

    @Column(name = "size_des", length = 255)
    private String sizeDes;

    @Column(name = "unit", length = 50)
    private String unit;

    @Column(name = "prod_type", length = 10)
    private String prodType;

    @Column(name = "in_price", precision = 15, scale = 2)
    private BigDecimal inPrice;

    @Column(name = "out_price", precision = 15, scale = 2)
    private BigDecimal outPrice;

    @Column(name = "bar_code", length = 100)
    private String barCode;

    @Column(name = "class_cd", length = 50)
    private String classCd;

    @Column(name = "class_cd2", length = 50)
    private String classCd2;

    @Column(name = "class_cd3", length = 50)
    private String classCd3;

    @Column(name = "set_flag")
    @Builder.Default
    private Boolean setFlag = false;

    @Column(name = "bal_flag")
    @Builder.Default
    private Boolean balFlag = true;

    @Column(name = "last_synced_at", nullable = false)
    private LocalDateTime lastSyncedAt;

    @Type(JsonType.class)
    @Column(name = "raw_data", columnDefinition = "jsonb")
    private Map<String, Object> rawData;
}
