package com.mhub.core.marketplace.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Builder
public class ConnectionTestResponse {

    private boolean connected;
    private String message;
}
