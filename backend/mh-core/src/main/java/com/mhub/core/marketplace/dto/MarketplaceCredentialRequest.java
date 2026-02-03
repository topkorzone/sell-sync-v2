package com.mhub.core.marketplace.dto;

import com.mhub.core.domain.enums.MarketplaceType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MarketplaceCredentialRequest {

    @NotNull(message = "마켓플레이스 타입은 필수입니다")
    private MarketplaceType marketplaceType;

    @NotBlank(message = "판매자 ID는 필수입니다")
    private String sellerId;

    @NotBlank(message = "Client ID는 필수입니다")
    private String clientId;

    @NotBlank(message = "Client Secret은 필수입니다")
    private String clientSecret;
}
