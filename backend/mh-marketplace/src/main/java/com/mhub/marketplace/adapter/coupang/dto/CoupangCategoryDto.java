package com.mhub.marketplace.adapter.coupang.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CoupangCategoryDto {

    private Long displayCategoryCode;
    private String displayCategoryName;
    private Long parentCategoryCode;
    private Integer depthLevel;

    // 최상위 카테고리 코드 (수수료 매핑용)
    private Long rootCategoryCode;
    private String rootCategoryName;
}
