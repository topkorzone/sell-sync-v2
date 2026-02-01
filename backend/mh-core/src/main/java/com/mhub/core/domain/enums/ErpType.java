package com.mhub.core.domain.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ErpType {
    ICOUNT("이카운트 ERP");

    private final String displayName;
}
