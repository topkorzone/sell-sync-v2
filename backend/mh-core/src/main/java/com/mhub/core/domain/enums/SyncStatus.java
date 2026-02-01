package com.mhub.core.domain.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum SyncStatus {
    PENDING("대기"),
    IN_PROGRESS("처리중"),
    SUCCESS("성공"),
    FAILED("실패");

    private final String displayName;
}
