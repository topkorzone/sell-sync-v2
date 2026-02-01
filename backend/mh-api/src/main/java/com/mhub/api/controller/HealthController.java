package com.mhub.api.controller;

import com.mhub.common.dto.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import java.time.LocalDateTime;
import java.util.Map;

@RestController
public class HealthController {
    @GetMapping("/api/v1/health")
    public ApiResponse<Map<String, Object>> health() { return ApiResponse.ok(Map.of("status", "UP", "timestamp", LocalDateTime.now().toString(), "version", "0.1.0")); }
}
