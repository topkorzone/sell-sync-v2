package com.mhub.core.domain.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ShipmentStatus {
    PENDING("대기"),
    RESERVED("예약완료"),
    PICKED_UP("집화완료"),
    IN_TRANSIT("배송중"),
    DELIVERED("배송완료"),
    CANCELLED("취소");

    private final String displayName;
}
