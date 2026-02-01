package com.mhub.api.controller;

import com.mhub.common.dto.ApiResponse;
import com.mhub.common.dto.PageResponse;
import com.mhub.core.domain.entity.Settlement;
import com.mhub.core.domain.repository.SettlementRepository;
import com.mhub.core.tenant.TenantContext;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Settlements") @RestController @RequestMapping("/api/v1/settlements") @RequiredArgsConstructor
public class SettlementController {
    private final SettlementRepository settlementRepository;
    @GetMapping
    public ApiResponse<PageResponse<Settlement>> listSettlements(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        Page<Settlement> s = settlementRepository.findByTenantId(TenantContext.requireTenantId(), PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "settlementDate")));
        return ApiResponse.ok(PageResponse.of(s.getContent(), s.getNumber(), s.getSize(), s.getTotalElements()));
    }
}
