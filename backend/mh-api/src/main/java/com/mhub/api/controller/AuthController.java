package com.mhub.api.controller;

import com.mhub.common.dto.ApiResponse;
import com.mhub.core.security.AuthService;
import com.mhub.core.security.dto.AuthResponse;
import com.mhub.core.security.dto.AuthUser;
import com.mhub.core.security.dto.LoginRequest;
import com.mhub.core.security.dto.RefreshRequest;
import com.mhub.core.security.dto.SignupRequest;
import com.mhub.core.security.dto.SignupResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "Auth")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "Signup")
    @PostMapping("/signup")
    public ApiResponse<SignupResponse> signup(@Valid @RequestBody SignupRequest request) {
        SignupResponse response = authService.signup(request);
        return ApiResponse.ok(response);
    }

    @Operation(summary = "Login")
    @PostMapping("/login")
    public ApiResponse<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request.getEmail(), request.getPassword());
        return ApiResponse.ok(response);
    }

    @Operation(summary = "Refresh token")
    @PostMapping("/refresh")
    public ApiResponse<AuthResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        AuthResponse response = authService.refresh(request.getRefreshToken());
        return ApiResponse.ok(response);
    }

    @Operation(summary = "Logout")
    @PostMapping("/logout")
    public ApiResponse<Void> logout(JwtAuthenticationToken authentication) {
        String accessToken = authentication.getToken().getTokenValue();
        authService.logout(accessToken);
        return ApiResponse.ok();
    }

    @SuppressWarnings("unchecked")
    @Operation(summary = "Get current user")
    @GetMapping("/me")
    public ApiResponse<AuthUser> me(JwtAuthenticationToken authentication) {
        Jwt jwt = authentication.getToken();
        Map<String, Object> appMetadata = jwt.getClaimAsMap("app_metadata");

        String role = appMetadata != null && appMetadata.get("role") != null
                ? appMetadata.get("role").toString()
                : "tenant_user";

        String tenantId = appMetadata != null && appMetadata.get("tenant_id") != null
                ? appMetadata.get("tenant_id").toString()
                : null;

        AuthUser user = AuthUser.builder()
                .id(jwt.getSubject())
                .email(jwt.getClaimAsString("email"))
                .role(role)
                .tenantId(tenantId)
                .build();

        return ApiResponse.ok(user);
    }
}
