package com.mhub.api.controller;

import com.mhub.common.dto.ApiResponse;
import com.mhub.core.domain.entity.Tenant;
import com.mhub.core.security.dto.ProfileDto;
import com.mhub.core.security.dto.UpdateProfileRequest;
import com.mhub.core.service.TenantService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@Slf4j
@Tag(name = "Profile")
@RestController
@RequestMapping("/api/v1/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final TenantService tenantService;

    @Operation(summary = "Get current user profile")
    @GetMapping
    public ApiResponse<ProfileDto> getProfile(JwtAuthenticationToken authentication) {
        Jwt jwt = authentication.getToken();
        Map<String, Object> appMetadata = jwt.getClaimAsMap("app_metadata");

        log.info("getProfile called: sub={}, appMetadata={}", jwt.getSubject(), appMetadata);

        String role = appMetadata != null && appMetadata.get("role") != null
                ? appMetadata.get("role").toString()
                : "tenant_user";

        String tenantIdStr = appMetadata != null && appMetadata.get("tenant_id") != null
                ? appMetadata.get("tenant_id").toString()
                : null;

        ProfileDto.ProfileDtoBuilder builder = ProfileDto.builder()
                .userId(jwt.getSubject())
                .email(jwt.getClaimAsString("email"))
                .role(role)
                .tenantId(tenantIdStr);

        if (tenantIdStr != null) {
            try {
                UUID tenantId = UUID.fromString(tenantIdStr);
                log.info("Fetching tenant: {}", tenantId);
                Tenant tenant = tenantService.getTenant(tenantId);
                builder.companyName(tenant.getCompanyName())
                        .businessNumber(tenant.getBusinessNumber())
                        .contactName(tenant.getContactName())
                        .contactEmail(tenant.getContactEmail())
                        .contactPhone(tenant.getContactPhone());
            } catch (Exception e) {
                log.error("Failed to fetch tenant {}: {}", tenantIdStr, e.getMessage(), e);
                // Return basic profile without tenant info if fetch fails
            }
        }

        return ApiResponse.ok(builder.build());
    }

    @Operation(summary = "Update current user profile")
    @PutMapping
    public ApiResponse<ProfileDto> updateProfile(
            JwtAuthenticationToken authentication,
            @Valid @RequestBody UpdateProfileRequest request) {
        Jwt jwt = authentication.getToken();
        Map<String, Object> appMetadata = jwt.getClaimAsMap("app_metadata");

        String role = appMetadata != null && appMetadata.get("role") != null
                ? appMetadata.get("role").toString()
                : "tenant_user";

        String tenantIdStr = appMetadata != null && appMetadata.get("tenant_id") != null
                ? appMetadata.get("tenant_id").toString()
                : null;

        if (tenantIdStr == null) {
            return ApiResponse.error("TENANT_NOT_FOUND", "테넌트 정보를 찾을 수 없습니다");
        }

        UUID tenantId = UUID.fromString(tenantIdStr);
        Tenant tenant = tenantService.updateTenant(
                tenantId,
                request.getCompanyName(),
                request.getBusinessNumber(),
                request.getContactName(),
                request.getContactEmail(),
                request.getContactPhone()
        );

        ProfileDto profile = ProfileDto.builder()
                .userId(jwt.getSubject())
                .email(jwt.getClaimAsString("email"))
                .role(role)
                .tenantId(tenantIdStr)
                .companyName(tenant.getCompanyName())
                .businessNumber(tenant.getBusinessNumber())
                .contactName(tenant.getContactName())
                .contactEmail(tenant.getContactEmail())
                .contactPhone(tenant.getContactPhone())
                .build();

        return ApiResponse.ok(profile);
    }
}
