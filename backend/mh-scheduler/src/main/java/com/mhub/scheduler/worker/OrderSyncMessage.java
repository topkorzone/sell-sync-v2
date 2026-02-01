package com.mhub.scheduler.worker;

public record OrderSyncMessage(String tenantId, String marketplaceType, String credentialId) {}
