package com.mhub.core.security.dto;

import jakarta.validation.constraints.Email;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateProfileRequest {
    private String companyName;
    private String businessNumber;
    private String contactName;

    @Email(message = "올바른 이메일 형식이 아닙니다")
    private String contactEmail;

    private String contactPhone;
}
