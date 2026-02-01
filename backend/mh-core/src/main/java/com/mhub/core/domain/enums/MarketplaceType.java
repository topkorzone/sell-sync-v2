package com.mhub.core.domain.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum MarketplaceType {
    NAVER("네이버 스마트스토어"),
    COUPANG("쿠팡"),
    ELEVEN_ST("11번가"),
    GMARKET("G마켓"),
    AUCTION("옥션"),
    WEMAKEPRICE("위메프"),
    TMON("티몬");

    private final String displayName;
}
