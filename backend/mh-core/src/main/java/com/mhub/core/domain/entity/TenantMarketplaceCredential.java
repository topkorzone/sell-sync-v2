package com.mhub.core.domain.entity;

import com.mhub.core.domain.enums.MarketplaceType;
import com.mhub.core.crypto.EncryptedStringConverter;
import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Type;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "tenant_marketplace_credential")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TenantMarketplaceCredential extends BaseEntity {

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Enumerated(EnumType.STRING)
    @Column(name = "marketplace_type", nullable = false)
    private MarketplaceType marketplaceType;

    @Column(name = "seller_id")
    private String sellerId;

    @Convert(converter = EncryptedStringConverter.class)
    @Column(name = "client_id")
    private String clientId;

    @Convert(converter = EncryptedStringConverter.class)
    @Column(name = "client_secret")
    private String clientSecret;

    @Convert(converter = EncryptedStringConverter.class)
    @Column(name = "access_token", length = 2000)
    private String accessToken;

    @Convert(converter = EncryptedStringConverter.class)
    @Column(name = "refresh_token", length = 2000)
    private String refreshToken;

    @Column(name = "token_expires_at")
    private LocalDateTime tokenExpiresAt;

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    @Type(JsonType.class)
    @Column(name = "extra_config", columnDefinition = "jsonb")
    private Map<String, Object> extraConfig;
}
