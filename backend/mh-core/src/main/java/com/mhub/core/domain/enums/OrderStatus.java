package com.mhub.core.domain.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum OrderStatus {
    PAYMENT_COMPLETE("결제완료"),
    PREPARING("상품준비중"),
    SHIPPING_READY("배송지시"),
    SHIPPING("배송중"),
    DELIVERED("배송완료"),
    CANCELLED("취소"),
    RETURNED("반품"),
    PURCHASE_CONFIRMED("구매확정");

    private final String displayName;
}
