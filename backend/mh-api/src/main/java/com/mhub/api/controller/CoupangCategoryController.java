package com.mhub.api.controller;

import com.mhub.api.service.CoupangCategoryService;
import com.mhub.common.dto.ApiResponse;
import com.mhub.core.domain.entity.CoupangCategory;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "Coupang Category", description = "쿠팡 카테고리 관리")
@RestController
@RequestMapping("/api/coupang/categories")
@RequiredArgsConstructor
public class CoupangCategoryController {

    private final CoupangCategoryService categoryService;

    @Operation(summary = "카테고리 동기화", description = "쿠팡 API에서 전체 카테고리를 동기화합니다")
    @PostMapping("/sync")
    public ResponseEntity<ApiResponse<Map<String, Object>>> syncCategories() {
        Map<String, Object> result = categoryService.syncCategories();
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @Operation(summary = "대분류 카테고리 목록 조회", description = "대분류(depth=1) 카테고리 목록을 조회합니다")
    @GetMapping("/root")
    public ResponseEntity<ApiResponse<List<CoupangCategory>>> getRootCategories() {
        List<CoupangCategory> categories = categoryService.getRootCategories();
        return ResponseEntity.ok(ApiResponse.ok(categories));
    }

    @Operation(summary = "카테고리 수 조회", description = "전체 카테고리 수를 조회합니다")
    @GetMapping("/count")
    public ResponseEntity<ApiResponse<Long>> getCategoryCount() {
        long count = categoryService.getCategoryCount();
        return ResponseEntity.ok(ApiResponse.ok(count));
    }
}
