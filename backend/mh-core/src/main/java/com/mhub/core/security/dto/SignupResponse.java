package com.mhub.core.security.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SignupResponse {

    private String userId;

    private String email;

    private String tenantId;

    private String message;

    private boolean emailConfirmationRequired;
}
