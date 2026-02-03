package com.mhub.core.erp.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
@Builder
public class ErpItemSyncResponse {
    private boolean success;
    private int totalCount;
    private int syncedCount;
    private int failedCount;
    private String message;
    private LocalDateTime syncedAt;
}
