package com.mhub.api.controller;

import com.mhub.common.dto.ApiResponse;
import com.mhub.common.dto.PageResponse;
import com.mhub.api.service.CoupangSellerProductService;
import com.mhub.core.service.dto.CoupangSellerProductResponse;
import com.mhub.core.service.dto.CoupangSellerProductSyncResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Slf4j
@Tag(name = "Coupang Seller Products", description = "쿠팡 등록상품 관리 API")
@RestController
@RequestMapping("/api/v1/coupang/seller-products")
@RequiredArgsConstructor
public class CoupangSellerProductController {

    private final CoupangSellerProductService sellerProductService;

    @Operation(summary = "등록상품 동기화",
            description = "쿠팡 API에서 등록상품 전체를 조회하여 DB에 동기화합니다.")
    @PostMapping("/sync")
    public ApiResponse<CoupangSellerProductSyncResponse> syncSellerProducts() {
        log.info("Starting Coupang seller products sync");
        CoupangSellerProductSyncResponse result = sellerProductService.syncSellerProducts();
        return ApiResponse.ok(result);
    }

    @Operation(summary = "등록상품 목록 조회",
            description = "동기화된 쿠팡 등록상품 목록을 조회합니다. 키워드, 상태, 브랜드로 필터링할 수 있습니다.")
    @GetMapping
    public ApiResponse<PageResponse<CoupangSellerProductResponse>> listSellerProducts(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String brand,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "sellerProductName") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {

        Sort sort = sortDir.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        Page<CoupangSellerProductResponse> products = sellerProductService.getSellerProducts(
                keyword, status, brand, PageRequest.of(page, size, sort));

        return ApiResponse.ok(PageResponse.of(
                products.getContent(),
                products.getNumber(),
                products.getSize(),
                products.getTotalElements()));
    }

    @Operation(summary = "등록상품 상세 조회")
    @GetMapping("/{id}")
    public ApiResponse<CoupangSellerProductResponse> getSellerProduct(@PathVariable UUID id) {
        return ApiResponse.ok(sellerProductService.getSellerProduct(id));
    }

    @Operation(summary = "등록상품 수 조회")
    @GetMapping("/count")
    public ApiResponse<Long> getProductCount() {
        return ApiResponse.ok(sellerProductService.getProductCount());
    }

    @Operation(summary = "브랜드 목록 조회",
            description = "등록상품에 있는 브랜드 목록을 조회합니다.")
    @GetMapping("/brands")
    public ApiResponse<List<String>> getBrands() {
        return ApiResponse.ok(sellerProductService.getBrands());
    }

    @Operation(summary = "상태 목록 조회",
            description = "등록상품에 있는 상태 목록을 조회합니다.")
    @GetMapping("/statuses")
    public ApiResponse<List<String>> getStatuses() {
        return ApiResponse.ok(sellerProductService.getStatuses());
    }
}
