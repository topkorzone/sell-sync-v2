package com.mhub.core.tenant;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

@Slf4j
public class TenantFilter implements Filter {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof Jwt jwt) {
                Map<String, Object> appMetadata = jwt.getClaimAsMap("app_metadata");
                if (appMetadata != null && appMetadata.get("tenant_id") != null) {
                    try {
                        TenantContext.setTenantId(UUID.fromString(appMetadata.get("tenant_id").toString()));
                        log.info("Tenant context set: tenantId={}, email={}", appMetadata.get("tenant_id"), jwt.getClaimAsString("email"));
                    } catch (IllegalArgumentException e) {
                        log.error("Invalid tenant_id format in JWT app_metadata: value='{}', sub={}",
                                appMetadata.get("tenant_id"), jwt.getSubject());
                        sendError((HttpServletResponse) response, 403,
                                "AUTH_006", "JWT의 tenant_id 형식이 올바르지 않습니다.");
                        return;
                    }
                } else if (!isTenantOptionalPath(request)) {
                    log.warn("JWT에 tenant_id가 없습니다. sub={}, app_metadata={}", jwt.getSubject(), appMetadata);
                    sendError((HttpServletResponse) response, 403,
                            "AUTH_006", "JWT에 tenant_id가 포함되어 있지 않습니다. Supabase 사용자의 app_metadata에 tenant_id를 설정하세요.");
                    return;
                }
            }
            chain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }

    private boolean isTenantOptionalPath(ServletRequest request) {
        if (request instanceof HttpServletRequest httpRequest) {
            String path = httpRequest.getRequestURI();
            return path.startsWith("/api/v1/auth/");
        }
        return false;
    }

    private void sendError(HttpServletResponse response, int status, String code, String message) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getOutputStream(),
                Map.of("success", false, "error", Map.of("code", code, "message", message)));
    }
}
