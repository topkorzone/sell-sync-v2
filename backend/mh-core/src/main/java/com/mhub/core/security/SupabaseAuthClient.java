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

    public SupabaseAuthClient(SupabaseProperties properties) {
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
}
