package com.mhub.core.domain.entity;

import com.mhub.core.domain.enums.ErpType;
import com.mhub.core.crypto.EncryptedStringConverter;
import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Type;

import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "tenant_erp_config")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TenantErpConfig extends BaseEntity {

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Enumerated(EnumType.STRING)
    @Column(name = "erp_type", nullable = false)
    private ErpType erpType;

    @Convert(converter = EncryptedStringConverter.class)
    @Column(name = "api_key")
    private String apiKey;

    @Column(name = "company_code")
    private String companyCode;

    @Column(name = "user_id")
    private String userId;

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    @Type(JsonType.class)
    @Column(name = "field_mapping", columnDefinition = "jsonb")
    private Map<String, Object> fieldMapping;

    @Type(JsonType.class)
    @Column(name = "extra_config", columnDefinition = "jsonb")
    private Map<String, Object> extraConfig;
}
