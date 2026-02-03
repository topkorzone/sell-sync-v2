package com.mhub.core.marketplace.dto;

import com.mhub.core.domain.enums.MarketplaceType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@AllArgsConstructor
@Builder
public class MarketplaceCredentialResponse {

    private UUID id;
    private MarketplaceType marketplaceType;
    private String sellerId;
    private Boolean active;
    private boolean hasClientId;
    private boolean hasClientSecret;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
