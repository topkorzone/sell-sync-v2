package com.mhub.core.security;

import com.mhub.core.security.dto.AuthResponse;
import com.mhub.core.security.dto.AuthUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final SupabaseAuthClient supabaseAuthClient;

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
