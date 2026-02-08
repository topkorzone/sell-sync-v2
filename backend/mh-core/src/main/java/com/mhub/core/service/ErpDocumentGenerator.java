package com.mhub.core.service;

import java.util.UUID;

/**
 * ERP 전표 생성 인터페이스
 * mh-shipping에서 mh-erp 의존성 없이 전표 생성 트리거
 */
public interface ErpDocumentGenerator {

    /**
     * 전표 생성 가능 여부 확인
     */
    boolean shouldGenerateDocument(UUID orderId);

    /**
     * 전표 생성 시도 (실패해도 예외 발생하지 않음)
     */
    void tryGenerateDocument(UUID orderId);
}
