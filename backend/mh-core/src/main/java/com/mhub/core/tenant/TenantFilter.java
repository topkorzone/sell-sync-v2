package com.mhub.core.tenant;

import jakarta.servlet.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

@Slf4j
public class TenantFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getPrincipal() instanceof Jwt jwt) {
                Map<String, Object> appMetadata = jwt.getClaimAsMap("app_metadata");
                if (appMetadata != null && appMetadata.get("tenant_id") != null) {
                    TenantContext.setTenantId(UUID.fromString(appMetadata.get("tenant_id").toString()));
                }
            }
            chain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }
}
