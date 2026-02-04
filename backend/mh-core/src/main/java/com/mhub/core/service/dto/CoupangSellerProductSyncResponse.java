package com.mhub.core.service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
@Builder
public class CoupangSellerProductSyncResponse {
    /**
     * 동기화 성공 여부
     */
    private boolean success;

    /**
     * API에서 조회한 총 상품 수
     */
    private int totalCount;

    /**
     * 신규 등록된 상품 수
     */
    private int insertedCount;

    /**
     * 기존 상품 업데이트 수
     */
    private int updatedCount;

    /**
     * 동기화 시작 시간
     */
    private LocalDateTime syncStartedAt;

    /**
     * 동기화 완료 시간
     */
    private LocalDateTime syncCompletedAt;

    /**
     * 동기화 소요 시간 (밀리초)
     */
    private long durationMs;

    /**
     * 오류 메시지 (실패 시)
     */
    private String errorMessage;

    public static CoupangSellerProductSyncResponse success(int totalCount, int insertedCount, int updatedCount,
                                                            LocalDateTime startedAt, LocalDateTime completedAt) {
        return CoupangSellerProductSyncResponse.builder()
                .success(true)
                .totalCount(totalCount)
                .insertedCount(insertedCount)
                .updatedCount(updatedCount)
                .syncStartedAt(startedAt)
                .syncCompletedAt(completedAt)
                .durationMs(java.time.Duration.between(startedAt, completedAt).toMillis())
                .build();
    }

    public static CoupangSellerProductSyncResponse failure(String errorMessage, LocalDateTime startedAt) {
        LocalDateTime completedAt = LocalDateTime.now();
        return CoupangSellerProductSyncResponse.builder()
                .success(false)
                .totalCount(0)
                .insertedCount(0)
                .updatedCount(0)
                .syncStartedAt(startedAt)
                .syncCompletedAt(completedAt)
                .durationMs(java.time.Duration.between(startedAt, completedAt).toMillis())
                .errorMessage(errorMessage)
                .build();
    }
}
