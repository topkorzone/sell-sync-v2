package com.mhub.core.domain.repository;

import com.mhub.core.domain.entity.Order;
import com.mhub.core.domain.enums.MarketplaceType;
import com.mhub.core.domain.enums.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrderRepository extends JpaRepository<Order, UUID> {
    Page<Order> findByTenantId(UUID tenantId, Pageable pageable);
    Page<Order> findByTenantIdAndStatus(UUID tenantId, OrderStatus status, Pageable pageable);
    Page<Order> findByTenantIdAndStatusIn(UUID tenantId, List<OrderStatus> statuses, Pageable pageable);
    Page<Order> findByTenantIdAndMarketplaceType(UUID tenantId, MarketplaceType marketplaceType, Pageable pageable);
    Page<Order> findByTenantIdAndStatusAndMarketplaceType(UUID tenantId, OrderStatus status, MarketplaceType marketplaceType, Pageable pageable);
    Page<Order> findByTenantIdAndStatusInAndMarketplaceType(UUID tenantId, List<OrderStatus> statuses, MarketplaceType marketplaceType, Pageable pageable);

    // ===================== 검색 쿼리 =====================

    /**
     * 검색 (주문번호 또는 수취인명)
     */
    @Query("SELECT o FROM Order o WHERE o.tenantId = :tenantId " +
           "AND (o.marketplaceOrderId LIKE CONCAT('%', :search, '%') OR o.receiverName LIKE CONCAT('%', :search, '%'))")
    Page<Order> searchByKeyword(
            @Param("tenantId") UUID tenantId,
            @Param("search") String search,
            Pageable pageable);

    /**
     * 검색 + 마켓플레이스 필터
     */
    @Query("SELECT o FROM Order o WHERE o.tenantId = :tenantId " +
           "AND o.marketplaceType = :marketplaceType " +
           "AND (o.marketplaceOrderId LIKE CONCAT('%', :search, '%') OR o.receiverName LIKE CONCAT('%', :search, '%'))")
    Page<Order> searchByKeywordAndMarketplace(
            @Param("tenantId") UUID tenantId,
            @Param("search") String search,
            @Param("marketplaceType") MarketplaceType marketplaceType,
            Pageable pageable);

    /**
     * 검색 + 상태 필터
     */
    @Query("SELECT o FROM Order o WHERE o.tenantId = :tenantId " +
           "AND o.status IN :statuses " +
           "AND (o.marketplaceOrderId LIKE CONCAT('%', :search, '%') OR o.receiverName LIKE CONCAT('%', :search, '%'))")
    Page<Order> searchByKeywordAndStatuses(
            @Param("tenantId") UUID tenantId,
            @Param("search") String search,
            @Param("statuses") List<OrderStatus> statuses,
            Pageable pageable);

    /**
     * 검색 + 상태 + 마켓플레이스 필터
     */
    @Query("SELECT o FROM Order o WHERE o.tenantId = :tenantId " +
           "AND o.status IN :statuses " +
           "AND o.marketplaceType = :marketplaceType " +
           "AND (o.marketplaceOrderId LIKE CONCAT('%', :search, '%') OR o.receiverName LIKE CONCAT('%', :search, '%'))")
    Page<Order> searchByKeywordAndStatusesAndMarketplace(
            @Param("tenantId") UUID tenantId,
            @Param("search") String search,
            @Param("statuses") List<OrderStatus> statuses,
            @Param("marketplaceType") MarketplaceType marketplaceType,
            Pageable pageable);

    // ===================== 날짜 범위 검색 쿼리 =====================

    /**
     * 날짜 범위만
     */
    @Query("SELECT o FROM Order o WHERE o.tenantId = :tenantId " +
           "AND o.orderedAt >= :startDate AND o.orderedAt <= :endDate")
    Page<Order> findByDateRange(
            @Param("tenantId") UUID tenantId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable);

    /**
     * 날짜 범위 + 상태
     */
    @Query("SELECT o FROM Order o WHERE o.tenantId = :tenantId " +
           "AND o.orderedAt >= :startDate AND o.orderedAt <= :endDate " +
           "AND o.status IN :statuses")
    Page<Order> findByDateRangeAndStatuses(
            @Param("tenantId") UUID tenantId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("statuses") List<OrderStatus> statuses,
            Pageable pageable);

    /**
     * 날짜 범위 + 마켓플레이스
     */
    @Query("SELECT o FROM Order o WHERE o.tenantId = :tenantId " +
           "AND o.orderedAt >= :startDate AND o.orderedAt <= :endDate " +
           "AND o.marketplaceType = :marketplaceType")
    Page<Order> findByDateRangeAndMarketplace(
            @Param("tenantId") UUID tenantId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("marketplaceType") MarketplaceType marketplaceType,
            Pageable pageable);

    /**
     * 날짜 범위 + 상태 + 마켓플레이스
     */
    @Query("SELECT o FROM Order o WHERE o.tenantId = :tenantId " +
           "AND o.orderedAt >= :startDate AND o.orderedAt <= :endDate " +
           "AND o.status IN :statuses " +
           "AND o.marketplaceType = :marketplaceType")
    Page<Order> findByDateRangeAndStatusesAndMarketplace(
            @Param("tenantId") UUID tenantId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("statuses") List<OrderStatus> statuses,
            @Param("marketplaceType") MarketplaceType marketplaceType,
            Pageable pageable);

    /**
     * 날짜 범위 + 검색어
     */
    @Query("SELECT o FROM Order o WHERE o.tenantId = :tenantId " +
           "AND o.orderedAt >= :startDate AND o.orderedAt <= :endDate " +
           "AND (o.marketplaceOrderId LIKE CONCAT('%', :search, '%') OR o.receiverName LIKE CONCAT('%', :search, '%'))")
    Page<Order> findByDateRangeAndKeyword(
            @Param("tenantId") UUID tenantId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("search") String search,
            Pageable pageable);

    /**
     * 날짜 범위 + 검색어 + 상태
     */
    @Query("SELECT o FROM Order o WHERE o.tenantId = :tenantId " +
           "AND o.orderedAt >= :startDate AND o.orderedAt <= :endDate " +
           "AND o.status IN :statuses " +
           "AND (o.marketplaceOrderId LIKE CONCAT('%', :search, '%') OR o.receiverName LIKE CONCAT('%', :search, '%'))")
    Page<Order> findByDateRangeAndKeywordAndStatuses(
            @Param("tenantId") UUID tenantId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("search") String search,
            @Param("statuses") List<OrderStatus> statuses,
            Pageable pageable);

    /**
     * 날짜 범위 + 검색어 + 마켓플레이스
     */
    @Query("SELECT o FROM Order o WHERE o.tenantId = :tenantId " +
           "AND o.orderedAt >= :startDate AND o.orderedAt <= :endDate " +
           "AND o.marketplaceType = :marketplaceType " +
           "AND (o.marketplaceOrderId LIKE CONCAT('%', :search, '%') OR o.receiverName LIKE CONCAT('%', :search, '%'))")
    Page<Order> findByDateRangeAndKeywordAndMarketplace(
            @Param("tenantId") UUID tenantId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("search") String search,
            @Param("marketplaceType") MarketplaceType marketplaceType,
            Pageable pageable);

    /**
     * 날짜 범위 + 검색어 + 상태 + 마켓플레이스
     */
    @Query("SELECT o FROM Order o WHERE o.tenantId = :tenantId " +
           "AND o.orderedAt >= :startDate AND o.orderedAt <= :endDate " +
           "AND o.status IN :statuses " +
           "AND o.marketplaceType = :marketplaceType " +
           "AND (o.marketplaceOrderId LIKE CONCAT('%', :search, '%') OR o.receiverName LIKE CONCAT('%', :search, '%'))")
    Page<Order> findByDateRangeAndKeywordAndStatusesAndMarketplace(
            @Param("tenantId") UUID tenantId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("search") String search,
            @Param("statuses") List<OrderStatus> statuses,
            @Param("marketplaceType") MarketplaceType marketplaceType,
            Pageable pageable);

    Optional<Order> findByTenantIdAndMarketplaceTypeAndMarketplaceOrderIdAndMarketplaceProductOrderId(UUID tenantId, MarketplaceType marketplaceType, String marketplaceOrderId, String marketplaceProductOrderId);
    @Query("SELECT COUNT(o) FROM Order o WHERE o.tenantId = :tenantId AND o.erpSynced = false")
    long countUnsynced(@Param("tenantId") UUID tenantId);

    List<Order> findByTenantIdAndErpSyncedFalse(UUID tenantId);

    @Query("SELECT o FROM Order o LEFT JOIN FETCH o.items WHERE o.id = :id")
    Optional<Order> findByIdWithItems(@Param("id") UUID id);

    /**
     * 수수료 미계산 주문 조회 (OrderItem에 commissionRate가 null인 주문)
     */
    @Query("SELECT DISTINCT o FROM Order o JOIN FETCH o.items i " +
           "WHERE o.tenantId = :tenantId AND o.marketplaceType = :marketplaceType " +
           "AND i.commissionRate IS NULL")
    List<Order> findOrdersWithPendingCommission(
            @Param("tenantId") UUID tenantId,
            @Param("marketplaceType") MarketplaceType marketplaceType);

    /**
     * 기존 주문 존재 여부를 배치로 조회 (marketplaceOrderId + marketplaceProductOrderId 기준)
     * @return 이미 존재하는 주문의 "marketplaceOrderId:marketplaceProductOrderId" 키 목록
     */
    @Query("SELECT CONCAT(o.marketplaceOrderId, ':', COALESCE(o.marketplaceProductOrderId, '')) " +
           "FROM Order o WHERE o.tenantId = :tenantId AND o.marketplaceType = :marketplaceType " +
           "AND CONCAT(o.marketplaceOrderId, ':', COALESCE(o.marketplaceProductOrderId, '')) IN :orderKeys")
    List<String> findExistingOrderKeys(
            @Param("tenantId") UUID tenantId,
            @Param("marketplaceType") MarketplaceType marketplaceType,
            @Param("orderKeys") List<String> orderKeys);

    /**
     * 미완료 주문 조회 (상태 업데이트 대상)
     * - 완료 상태(배송완료, 구매확정, 취소, 반품, 교환)가 아닌 주문
     * - 지정 기간 이후에 주문된 것만 (최근 주문만 추적)
     */
    @Query("SELECT o FROM Order o WHERE o.tenantId = :tenantId " +
           "AND o.marketplaceType = :marketplaceType " +
           "AND o.status NOT IN :completedStatuses " +
           "AND o.orderedAt > :since")
    List<Order> findPendingOrders(
            @Param("tenantId") UUID tenantId,
            @Param("marketplaceType") MarketplaceType marketplaceType,
            @Param("completedStatuses") List<OrderStatus> completedStatuses,
            @Param("since") LocalDateTime since);

    /**
     * 마켓플레이스 주문번호로 주문 조회 (정산 매칭용)
     */
    List<Order> findByTenantIdAndMarketplaceTypeAndMarketplaceOrderIdIn(
            UUID tenantId, MarketplaceType marketplaceType, List<String> marketplaceOrderIds);

    /**
     * 마켓플레이스 상품주문번호로 주문 조회 (네이버 정산 매칭용)
     */
    List<Order> findByTenantIdAndMarketplaceTypeAndMarketplaceProductOrderIdIn(
            UUID tenantId, MarketplaceType marketplaceType, List<String> marketplaceProductOrderIds);

    /**
     * 정산 수집 완료 표시
     */
    @Modifying
    @Query("UPDATE Order o SET o.settlementCollected = true WHERE o.id IN :orderIds")
    int markSettlementCollected(@Param("orderIds") List<UUID> orderIds);

    /**
     * 전표 미생성 주문 조회 (배송중/배송완료 상태, ERP 전표 없음, 상품매핑 완료)
     */
    @Query("SELECT o FROM Order o LEFT JOIN FETCH o.items " +
           "WHERE o.tenantId = :tenantId " +
           "AND o.status IN :statuses " +
           "AND NOT EXISTS (SELECT 1 FROM ErpSalesDocument d WHERE d.orderId = o.id AND d.status != 'CANCELLED') " +
           "AND NOT EXISTS (SELECT 1 FROM OrderItem oi WHERE oi.order = o AND oi.erpProdCd IS NULL)")
    List<Order> findOrdersWithoutErpDocument(
            @Param("tenantId") UUID tenantId,
            @Param("statuses") List<OrderStatus> statuses);

    /**
     * 전표 미생성 주문 조회 (페이징) - 상품매핑 완료된 주문만
     */
    @Query("SELECT o FROM Order o " +
           "WHERE o.tenantId = :tenantId " +
           "AND o.status IN :statuses " +
           "AND NOT EXISTS (SELECT 1 FROM ErpSalesDocument d WHERE d.orderId = o.id AND d.status != 'CANCELLED') " +
           "AND NOT EXISTS (SELECT 1 FROM OrderItem oi WHERE oi.order = o AND oi.erpProdCd IS NULL)")
    Page<Order> findOrdersWithoutErpDocument(
            @Param("tenantId") UUID tenantId,
            @Param("statuses") List<OrderStatus> statuses,
            Pageable pageable);

    /**
     * 전표 미생성 주문 수 조회 - 상품매핑 완료된 주문만
     */
    @Query("SELECT COUNT(o) FROM Order o " +
           "WHERE o.tenantId = :tenantId " +
           "AND o.status IN :statuses " +
           "AND NOT EXISTS (SELECT 1 FROM ErpSalesDocument d WHERE d.orderId = o.id AND d.status != 'CANCELLED') " +
           "AND NOT EXISTS (SELECT 1 FROM OrderItem oi WHERE oi.order = o AND oi.erpProdCd IS NULL)")
    long countOrdersWithoutErpDocument(
            @Param("tenantId") UUID tenantId,
            @Param("statuses") List<OrderStatus> statuses);

    // ===================== Dashboard 통계 쿼리 =====================

    /**
     * 오늘 주문 수 (주문일 기준)
     */
    @Query("SELECT COUNT(o) FROM Order o WHERE o.tenantId = :tenantId AND o.orderedAt >= :startOfDay")
    long countTodayOrders(@Param("tenantId") UUID tenantId, @Param("startOfDay") LocalDateTime startOfDay);

    /**
     * 오늘 발송 수 (배송중/배송완료 상태로 변경된 주문 - updatedAt 기준)
     */
    @Query("SELECT COUNT(o) FROM Order o WHERE o.tenantId = :tenantId " +
           "AND o.status IN :shippingStatuses AND o.updatedAt >= :startOfDay")
    long countTodayShipments(
            @Param("tenantId") UUID tenantId,
            @Param("shippingStatuses") List<OrderStatus> shippingStatuses,
            @Param("startOfDay") LocalDateTime startOfDay);

    /**
     * 미처리 주문 수 (수집완료, 확인, 발송준비 상태)
     */
    @Query("SELECT COUNT(o) FROM Order o WHERE o.tenantId = :tenantId AND o.status IN :pendingStatuses")
    long countPendingOrders(
            @Param("tenantId") UUID tenantId,
            @Param("pendingStatuses") List<OrderStatus> pendingStatuses);

    /**
     * 이번 달 매출 합계
     */
    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o " +
           "WHERE o.tenantId = :tenantId AND o.orderedAt >= :startOfMonth " +
           "AND o.status NOT IN :excludeStatuses")
    long sumMonthlyRevenue(
            @Param("tenantId") UUID tenantId,
            @Param("startOfMonth") LocalDateTime startOfMonth,
            @Param("excludeStatuses") List<OrderStatus> excludeStatuses);

    /**
     * 최근 주문 조회
     */
    @Query("SELECT o FROM Order o WHERE o.tenantId = :tenantId ORDER BY o.orderedAt DESC")
    List<Order> findRecentOrders(@Param("tenantId") UUID tenantId, Pageable pageable);

    /**
     * 마켓별 주문 수 (이번 달)
     */
    @Query("SELECT o.marketplaceType, COUNT(o) FROM Order o " +
           "WHERE o.tenantId = :tenantId AND o.orderedAt >= :startOfMonth " +
           "GROUP BY o.marketplaceType")
    List<Object[]> countOrdersByMarketplace(
            @Param("tenantId") UUID tenantId,
            @Param("startOfMonth") LocalDateTime startOfMonth);

    /**
     * 상태별 주문 수
     */
    @Query("SELECT o.status, COUNT(o) FROM Order o " +
           "WHERE o.tenantId = :tenantId AND o.orderedAt >= :since " +
           "GROUP BY o.status")
    List<Object[]> countOrdersByStatus(
            @Param("tenantId") UUID tenantId,
            @Param("since") LocalDateTime since);
}
