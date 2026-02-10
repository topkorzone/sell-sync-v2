package com.mhub.core.security;

import com.mhub.common.exception.BusinessException;
import com.mhub.common.exception.ErrorCodes;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.Map;

@Slf4j
@Component
public class SupabaseAuthClient {

    private final RestClient restClient;
    private final String serviceRoleKey;

    public SupabaseAuthClient(SupabaseProperties properties) {
        this.serviceRoleKey = properties.getServiceRoleKey();
        this.restClient = RestClient.builder()
                .baseUrl(properties.getUrl())
                .defaultHeader("apikey", properties.getServiceRoleKey())
                .build();
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> signInWithPassword(String email, String password) {
        try {
            return restClient.post()
                    .uri("/auth/v1/token?grant_type=password")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("email", email, "password", password))
                    .retrieve()
                    .body(Map.class);
        } catch (RestClientResponseException e) {
            log.warn("Supabase login failed: {} {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new BusinessException(ErrorCodes.AUTH_INVALID_CREDENTIALS, "이메일 또는 비밀번호가 올바르지 않습니다");
        }
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> refreshToken(String refreshToken) {
        try {
            return restClient.post()
                    .uri("/auth/v1/token?grant_type=refresh_token")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("refresh_token", refreshToken))
                    .retrieve()
                    .body(Map.class);
        } catch (RestClientResponseException e) {
            log.warn("Supabase token refresh failed: {} {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new BusinessException(ErrorCodes.AUTH_REFRESH_FAILED, "토큰 갱신에 실패했습니다");
        }
    }

    public void logout(String accessToken) {
        try {
            restClient.post()
                    .uri("/auth/v1/logout")
                    .header("Authorization", "Bearer " + accessToken)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException e) {
            log.warn("Supabase logout failed: {} {}", e.getStatusCode(), e.getResponseBodyAsString());
        }
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> createUser(String email, String password, String tenantId, String role) {
        Map<String, Object> appMetadata = Map.of(
                "tenant_id", tenantId,
                "role", role
        );

        Map<String, Object> requestBody = Map.of(
                "email", email,
                "password", password,
                "email_confirm", false,
                "app_metadata", appMetadata
        );

        try {
            Map<String, Object> result = restClient.post()
                    .uri("/auth/v1/admin/users")
                    .header("Authorization", "Bearer " + serviceRoleKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .body(Map.class);

            // Send confirmation email after user creation
            sendConfirmationEmail(email);

            return result;
        } catch (RestClientResponseException e) {
            log.error("Supabase user creation failed: {} {}", e.getStatusCode(), e.getResponseBodyAsString());
            String responseBody = e.getResponseBodyAsString();
            if (responseBody.contains("already been registered") || responseBody.contains("duplicate")) {
                throw new BusinessException(ErrorCodes.AUTH_EMAIL_ALREADY_EXISTS, "이미 등록된 이메일입니다");
            }
            throw new BusinessException(ErrorCodes.AUTH_SIGNUP_FAILED, "회원가입에 실패했습니다: " + e.getMessage());
        }
    }

    public void sendConfirmationEmail(String email) {
        Map<String, Object> requestBody = Map.of(
                "type", "signup",
                "email", email
        );

        try {
            restClient.post()
                    .uri("/auth/v1/resend")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .toBodilessEntity();
            log.info("Confirmation email sent to: {}", email);
        } catch (RestClientResponseException e) {
            log.warn("Failed to send confirmation email: {} {}", e.getStatusCode(), e.getResponseBodyAsString());
            // Don't throw exception - user is already created, just log the warning
        }
    }
}
