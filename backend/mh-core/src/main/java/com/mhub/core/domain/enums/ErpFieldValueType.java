package com.mhub.core.domain.enums;

/**
 * ERP 필드 값 유형
 */
public enum ErpFieldValueType {
    /**
     * 고정값
     */
    FIXED,

    /**
     * 마켓별 다른 값
     */
    MARKETPLACE,

    /**
     * 주문 정보에서 동적 추출
     */
    ORDER_FIELD
}
