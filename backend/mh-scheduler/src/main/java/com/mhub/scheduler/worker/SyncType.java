package com.mhub.scheduler.worker;

/**
 * 주문 동기화 유형
 */
public enum SyncType {
    /**
     * 신규 주문 수집 (당일 주문만)
     */
    NEW_ORDERS,

    /**
     * 미완료 주문 상태 업데이트
     */
    STATUS_UPDATE,

    /**
     * 건별 정산 데이터 수집
     */
    SETTLEMENT_COLLECTION
}
