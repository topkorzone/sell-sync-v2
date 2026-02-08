package com.mhub.core.domain.enums;

public enum ErpDocumentStatus {
    PENDING("미전송"),
    SENT("전송완료"),
    FAILED("전송실패"),
    CANCELLED("취소");

    private final String displayName;

    ErpDocumentStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
