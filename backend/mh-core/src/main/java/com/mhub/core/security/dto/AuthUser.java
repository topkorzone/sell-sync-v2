package com.mhub.core.security.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AuthUser {

    private String id;
    private String email;
    private String role;
    private String tenantId;
}
