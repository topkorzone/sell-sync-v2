package com.mhub.api.controller;

import com.mhub.common.dto.ApiResponse;
import com.mhub.common.dto.PageResponse;
import com.mhub.core.domain.enums.MarketplaceType;
import com.mhub.core.service.ProductMappingService;
import com.mhub.core.service.dto.ProductMappingRequest;
import com.mhub.core.service.dto.ProductMappingResponse;
import com.mhub.core.service.dto.UnmappedProductResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Slf4j
@Tag(name = "Product Mappings", description = "상품 매핑 관리 API")
@RestController
@RequestMapping("/api/v1/product-mappings")
@RequiredArgsConstructor
public class ProductMappingController {

    private final ProductMappingService productMappingService;

    @Operation(summary = "상품 매핑 목록 조회", description = "마켓플레이스 상품과 ERP 품목 간 매핑 목록을 조회합니다.")
    @GetMapping
    public ApiResponse<PageResponse<ProductMappingResponse>> listMappings(
            @RequestParam(required = false) MarketplaceType marketplace,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        Sort sort = sortDir.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        Page<ProductMappingResponse> mappings = productMappingService.getMappings(
                marketplace, keyword, PageRequest.of(page, size, sort));

        return ApiResponse.ok(PageResponse.of(
                mappings.getContent(),
                mappings.getNumber(),
                mappings.getSize(),
                mappings.getTotalElements()));
    }

    @Operation(summary = "상품 매핑 상세 조회")
    @GetMapping("/{id}")
    public ApiResponse<ProductMappingResponse> getMapping(@PathVariable UUID id) {
        return ApiResponse.ok(productMappingService.getMapping(id));
    }

    @Operation(summary = "상품 매핑 생성/수정",
            description = "마켓플레이스 상품ID + SKU 조합으로 기존 매핑이 있으면 수정, 없으면 새로 생성합니다.")
    @PostMapping
    public ApiResponse<ProductMappingResponse> createOrUpdateMapping(
            @RequestBody ProductMappingRequest request) {
        return ApiResponse.ok(productMappingService.createOrUpdateMapping(request));
    }

    @Operation(summary = "상품 매핑 삭제")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteMapping(@PathVariable UUID id) {
        productMappingService.deleteMapping(id);
        return ApiResponse.ok(null);
    }

    @Operation(summary = "매핑 수 조회")
    @GetMapping("/count")
    public ApiResponse<Long> getMappingCount() {
        return ApiResponse.ok(productMappingService.getMappingCount());
    }

    @Operation(summary = "미매핑 상품 목록 조회",
            description = "주문 데이터에서 ERP 매핑이 없는 상품을 productId+SKU로 그룹화하여 조회합니다.")
    @GetMapping("/unmapped")
    public ApiResponse<PageResponse<UnmappedProductResponse>> listUnmappedProducts(
            @RequestParam(required = false) MarketplaceType marketplace,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Page<UnmappedProductResponse> unmappedProducts = productMappingService.getUnmappedProducts(
                marketplace, PageRequest.of(page, size));

        return ApiResponse.ok(PageResponse.of(
                unmappedProducts.getContent(),
                unmappedProducts.getNumber(),
                unmappedProducts.getSize(),
                unmappedProducts.getTotalElements()));
    }

    @Operation(summary = "미매핑 상품 수 조회")
    @GetMapping("/unmapped/count")
    public ApiResponse<Long> getUnmappedProductCount(
            @RequestParam(required = false) MarketplaceType marketplace) {
        return ApiResponse.ok(productMappingService.getUnmappedProductCount(marketplace));
    }
}
