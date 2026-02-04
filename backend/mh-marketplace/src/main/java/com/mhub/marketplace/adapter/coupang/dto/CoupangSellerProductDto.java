package com.mhub.marketplace.adapter.coupang.dto;

import lombok.*;

import java.time.LocalDateTime;

/**
 * 쿠팡 등록상품조회 API 응답 DTO
 */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CoupangSellerProductDto {

    /**
     * 셀러 상품 ID
     */
    private Long sellerProductId;

    /**
     * 상품명
     */
    private String sellerProductName;

    /**
     * 전시 카테고리 코드
     */
    private Long displayCategoryCode;

    /**
     * 카테고리 ID
     */
    private Long categoryId;

    /**
     * 쿠팡 상품 ID
     */
    private Long productId;

    /**
     * 벤더 ID
     */
    private String vendorId;

    /**
     * 판매 시작일
     */
    private LocalDateTime saleStartedAt;

    /**
     * 판매 종료일
     */
    private LocalDateTime saleEndedAt;

    /**
     * 브랜드명
     */
    private String brand;

    /**
     * 상품 상태 (APPROVED, REJECTED 등)
     */
    private String statusName;

    /**
     * 등록일시
     */
    private LocalDateTime createdAt;
}
