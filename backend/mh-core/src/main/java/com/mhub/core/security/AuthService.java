package com.mhub.core.security;

import com.mhub.core.domain.entity.Tenant;
import com.mhub.core.domain.repository.TenantRepository;
import com.mhub.core.security.dto.AuthResponse;
import com.mhub.core.security.dto.AuthUser;
import com.mhub.core.security.dto.SignupRequest;
import com.mhub.core.security.dto.SignupResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final SupabaseAuthClient supabaseAuthClient;
    private final TenantRepository tenantRepository;

    public AuthResponse login(String email, String password) {
        Map<String, Object> result = supabaseAuthClient.signInWithPassword(email, password);
        return toAuthResponse(result);
    }

    public AuthResponse refresh(String refreshToken) {
        Map<String, Object> result = supabaseAuthClient.refreshToken(refreshToken);
        return toAuthResponse(result);
    }

    public void logout(String accessToken) {
        supabaseAuthClient.logout(accessToken);
    }

    @Transactional
    public SignupResponse signup(SignupRequest request) {
        log.info("Starting signup for email: {}", request.getEmail());

        // 1. Create Tenant
        Tenant tenant = Tenant.builder()
                .companyName(request.getCompanyName())
                .businessNumber(request.getBusinessNumber())
                .contactName(request.getContactName())
                .contactEmail(request.getEmail())
                .contactPhone(request.getContactPhone())
                .active(true)
                .build();
        tenant = tenantRepository.save(tenant);
        log.info("Tenant created with id: {}", tenant.getId());

        // 2. Create Supabase user with Admin API
        Map<String, Object> supabaseUser = supabaseAuthClient.createUser(
                request.getEmail(),
                request.getPassword(),
                tenant.getId().toString(),
                "admin"  // First user of tenant is admin
        );

        String userId = (String) supabaseUser.get("id");
        log.info("Supabase user created with id: {}", userId);

        return SignupResponse.builder()
                .userId(userId)
                .email(request.getEmail())
                .tenantId(tenant.getId().toString())
                .message("회원가입이 완료되었습니다. 이메일 인증 후 로그인해주세요.")
                .emailConfirmationRequired(true)
                .build();
    }

    @SuppressWarnings("unchecked")
    private AuthResponse toAuthResponse(Map<String, Object> result) {
        Map<String, Object> user = (Map<String, Object>) result.get("user");
        Map<String, Object> appMetadata = user != null
                ? (Map<String, Object>) user.get("app_metadata")
                : Map.of();

        String role = appMetadata != null && appMetadata.get("role") != null
                ? appMetadata.get("role").toString()
                : "tenant_user";

        String tenantId = appMetadata != null && appMetadata.get("tenant_id") != null
                ? appMetadata.get("tenant_id").toString()
                : null;

        return AuthResponse.builder()
                .accessToken((String) result.get("access_token"))
                .refreshToken((String) result.get("refresh_token"))
                .expiresIn(((Number) result.get("expires_in")).longValue())
                .expiresAt(((Number) result.get("expires_at")).longValue())
                .user(AuthUser.builder()
                        .id(user != null ? (String) user.get("id") : null)
                        .email(user != null ? (String) user.get("email") : null)
                        .role(role)
                        .tenantId(tenantId)
                        .build())
                .build();
    }
}
