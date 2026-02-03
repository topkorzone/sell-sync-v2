package com.mhub.core.service.dto;

import com.mhub.core.domain.enums.MarketplaceType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 주문 데이터에서 추출한 미매핑 상품 그룹 응답 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UnmappedProductResponse {

    private MarketplaceType marketplaceType;
    private String marketplaceProductId;
    private String marketplaceSku;
    private String productName;
    private String optionName;
    private Long orderCount;
}
