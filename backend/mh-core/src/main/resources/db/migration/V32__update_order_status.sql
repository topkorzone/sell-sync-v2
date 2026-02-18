-- V32__update_order_status.sql
-- 주문 상태 체계 단순화: 9개 → 8개 상태로 변경
-- COLLECTED → PAYMENT_COMPLETE (결제완료)
-- CONFIRMED → PREPARING (상품준비중)
-- READY_TO_SHIP → SHIPPING_READY (배송지시)
-- EXCHANGED → RETURNED (교환을 반품으로 통합)

-- orders 테이블 상태 변환
UPDATE orders SET status = 'PAYMENT_COMPLETE' WHERE status = 'COLLECTED';
UPDATE orders SET status = 'PREPARING' WHERE status = 'CONFIRMED';
UPDATE orders SET status = 'SHIPPING_READY' WHERE status = 'READY_TO_SHIP';
UPDATE orders SET status = 'RETURNED' WHERE status = 'EXCHANGED';

-- order_status_log 테이블도 동일하게 변환 (존재하는 경우)
DO $$
BEGIN
    IF EXISTS (SELECT FROM information_schema.tables WHERE table_name = 'order_status_log') THEN
        UPDATE order_status_log SET from_status = 'PAYMENT_COMPLETE' WHERE from_status = 'COLLECTED';
        UPDATE order_status_log SET to_status = 'PAYMENT_COMPLETE' WHERE to_status = 'COLLECTED';
        UPDATE order_status_log SET from_status = 'PREPARING' WHERE from_status = 'CONFIRMED';
        UPDATE order_status_log SET to_status = 'PREPARING' WHERE to_status = 'CONFIRMED';
        UPDATE order_status_log SET from_status = 'SHIPPING_READY' WHERE from_status = 'READY_TO_SHIP';
        UPDATE order_status_log SET to_status = 'SHIPPING_READY' WHERE to_status = 'READY_TO_SHIP';
        UPDATE order_status_log SET from_status = 'RETURNED' WHERE from_status = 'EXCHANGED';
        UPDATE order_status_log SET to_status = 'RETURNED' WHERE to_status = 'EXCHANGED';
    END IF;
END $$;
