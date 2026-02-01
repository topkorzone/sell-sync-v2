package com.mhub.core.domain.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum OrderStatus {
    COLLECTED("주문수집"),
    CONFIRMED("주문확인"),
    READY_TO_SHIP("발송대기"),
    SHIPPING("배송중"),
    DELIVERED("배송완료"),
    CANCELLED("취소"),
    RETURNED("반품"),
    EXCHANGED("교환"),
    PURCHASE_CONFIRMED("구매확정");

    private final String displayName;
}
