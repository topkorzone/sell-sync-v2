package com.mhub.scheduler.worker;

public record OrderSyncMessage(
        String tenantId,
        String marketplaceType,
        String credentialId,
        SyncType syncType
) {
    /**
     * 기존 메시지와의 하위 호환성을 위한 기본값 생성자
     */
    public OrderSyncMessage(String tenantId, String marketplaceType, String credentialId) {
        this(tenantId, marketplaceType, credentialId, SyncType.NEW_ORDERS);
    }
}
