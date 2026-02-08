package com.mhub.core.domain.enums;

/**
 * ERP 라인 타입 (LINE 위치일 때 적용 대상)
 */
public enum ErpLineType {
    /**
     * 모든 라인에 적용
     */
    ALL,

    /**
     * 상품판매 라인
     */
    PRODUCT_SALE,

    /**
     * 배송비 라인
     */
    DELIVERY_FEE,

    /**
     * 판매수수료 라인
     */
    SALES_COMMISSION,

    /**
     * 배송수수료 라인
     */
    DELIVERY_COMMISSION
}
