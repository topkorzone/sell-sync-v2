package com.mhub.core.domain.entity;

import com.mhub.core.domain.enums.CourierType;
import com.mhub.core.crypto.EncryptedStringConverter;
import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Type;

import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "tenant_courier_config")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TenantCourierConfig extends BaseEntity {

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Enumerated(EnumType.STRING)
    @Column(name = "courier_type", nullable = false)
    private CourierType courierType;

    @Convert(converter = EncryptedStringConverter.class)
    @Column(name = "api_key")
    private String apiKey;

    @Convert(converter = EncryptedStringConverter.class)
    @Column(name = "api_secret")
    private String apiSecret;

    @Column(name = "contract_code")
    private String contractCode;

    @Column(name = "sender_name")
    private String senderName;

    @Column(name = "sender_phone")
    private String senderPhone;

    @Column(name = "sender_address")
    private String senderAddress;

    @Column(name = "sender_zipcode")
    private String senderZipcode;

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    @Type(JsonType.class)
    @Column(name = "extra_config", columnDefinition = "jsonb")
    private Map<String, Object> extraConfig;
}
