package com.mhub.marketplace.adapter.dto;

import com.mhub.core.domain.enums.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 주문 상태 정보 (배치 상태 조회 응답용)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderStatusInfo {
    /**
     * 마켓플레이스별 상품주문 ID (네이버: productOrderId)
     */
    private String productOrderId;

    /**
     * 마켓플레이스 원본 상태 코드
     */
    private String marketplaceStatus;

    /**
     * 매핑된 내부 상태
     */
    private OrderStatus status;
}
