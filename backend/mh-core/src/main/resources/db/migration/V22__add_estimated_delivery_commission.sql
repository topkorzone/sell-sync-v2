-- 주문 수집 시 배송수수료 추정값 저장
ALTER TABLE orders ADD COLUMN estimated_delivery_commission NUMERIC(15,2);

COMMENT ON COLUMN orders.estimated_delivery_commission IS '배송수수료 추정값 (주문 수집 시 계산)';
