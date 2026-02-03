package com.mhub.core.erp.dto;

import com.mhub.core.domain.enums.ErpType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ErpConfigRequest {

    @NotNull(message = "ERP 타입은 필수입니다")
    private ErpType erpType;

    @NotBlank(message = "회사 코드는 필수입니다")
    private String companyCode;

    @NotBlank(message = "사용자 ID는 필수입니다")
    private String userId;

    @NotBlank(message = "API 인증키는 필수입니다")
    private String apiKey;

    private Map<String, Object> fieldMapping;

    private Map<String, Object> extraConfig;
}
