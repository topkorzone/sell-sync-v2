-- 네이버 스마트스토어 발주확인 상태를 저장하기 위한 컬럼 추가
ALTER TABLE orders ADD COLUMN IF NOT EXISTS place_order_status VARCHAR(20);

-- 인덱스 추가 (상태 조회 성능 향상)
CREATE INDEX IF NOT EXISTS idx_orders_place_order_status ON orders(place_order_status);

COMMENT ON COLUMN orders.place_order_status IS '발주확인 상태 (네이버: NOT_YET/OK)';
