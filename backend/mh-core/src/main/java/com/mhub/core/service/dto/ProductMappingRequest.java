package com.mhub.core.service.dto;

import com.mhub.core.domain.enums.MarketplaceType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductMappingRequest {
    private MarketplaceType marketplaceType;
    private String marketplaceProductId;
    private String marketplaceSku;
    private String marketplaceProductName;
    private String marketplaceOptionName;
    private UUID erpItemId;
    private String erpProdCd;
    private String erpWhCd;
}
