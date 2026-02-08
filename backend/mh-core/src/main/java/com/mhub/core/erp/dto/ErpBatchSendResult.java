package com.mhub.core.erp.dto;

import java.util.List;
import java.util.UUID;

public record ErpBatchSendResult(
        int totalCount,
        int successCount,
        int failCount,
        List<SendItemResult> results
) {
    public record SendItemResult(
            UUID documentId,
            UUID orderId,
            boolean success,
            String erpDocumentId,
            String errorMessage
    ) {}
}
