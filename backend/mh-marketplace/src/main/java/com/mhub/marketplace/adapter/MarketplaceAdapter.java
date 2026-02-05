package com.mhub.marketplace.adapter;

import com.mhub.core.domain.entity.Order;
import com.mhub.core.domain.entity.OrderSettlement;
import com.mhub.core.domain.entity.TenantMarketplaceCredential;
import com.mhub.core.domain.enums.MarketplaceType;
import com.mhub.marketplace.adapter.dto.OrderStatusInfo;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

public interface MarketplaceAdapter {
    MarketplaceType getMarketplaceType();
    List<Order> collectOrders(TenantMarketplaceCredential credential, LocalDateTime from, LocalDateTime to);
    List<Order> getChangedOrders(TenantMarketplaceCredential credential, LocalDateTime since);
    void confirmShipment(TenantMarketplaceCredential credential, String marketplaceOrderId, String trackingNumber, String courierCode);
    void refreshToken(TenantMarketplaceCredential credential);
    boolean testConnection(TenantMarketplaceCredential credential);

    /**
     * 주문 상태 배치 조회
     * - 네이버: POST /v1/pay-order/seller/product-orders/query (최대 300개)
     * - 미지원 마켓플레이스는 빈 리스트 반환
     *
     * @param credential 마켓플레이스 인증 정보
     * @param productOrderIds 조회할 상품주문 ID 목록
     * @return 각 주문의 현재 상태 정보
     */
    default List<OrderStatusInfo> getOrderStatuses(TenantMarketplaceCredential credential, List<String> productOrderIds) {
        return Collections.emptyList();
    }

    /**
     * 건별 정산 데이터 수집
     * - 미지원 마켓플레이스는 빈 리스트 반환
     *
     * @param credential 마켓플레이스 인증 정보
     * @param from 조회 시작일
     * @param to 조회 종료일
     * @return 정산 데이터 목록
     */
    default List<OrderSettlement> collectSettlements(TenantMarketplaceCredential credential, LocalDate from, LocalDate to) {
        return Collections.emptyList();
    }
}
