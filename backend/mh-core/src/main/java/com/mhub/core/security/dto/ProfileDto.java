package com.mhub.core.security.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ProfileDto {
    private String userId;
    private String email;
    private String role;
    private String tenantId;
    private String companyName;
    private String businessNumber;
    private String contactName;
    private String contactEmail;
    private String contactPhone;
}
