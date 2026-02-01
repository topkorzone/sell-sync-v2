package com.mhub.core.domain.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum CourierType {
    CJ("CJ대한통운", "04"),
    HANJIN("한진택배", "05"),
    LOGEN("로젠택배", "06"),
    LOTTE("롯데택배", "08"),
    POST("우체국택배", "01");

    private final String displayName;
    private final String code;
}
