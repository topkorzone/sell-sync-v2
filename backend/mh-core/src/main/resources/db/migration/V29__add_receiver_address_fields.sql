-- 수령인 주소 분리 필드 및 배송메모 추가
-- 택배예약 시 기본주소/상세주소를 분리하여 전송해야 함

-- 기본주소 (도로명/지번 주소)
ALTER TABLE orders ADD COLUMN IF NOT EXISTS receiver_address_base VARCHAR(300);
COMMENT ON COLUMN orders.receiver_address_base IS '수령인 기본주소';

-- 상세주소 (동/호수 등)
ALTER TABLE orders ADD COLUMN IF NOT EXISTS receiver_address_detail VARCHAR(200);
COMMENT ON COLUMN orders.receiver_address_detail IS '수령인 상세주소';

-- 배송메모/요청사항
ALTER TABLE orders ADD COLUMN IF NOT EXISTS delivery_memo VARCHAR(500);
COMMENT ON COLUMN orders.delivery_memo IS '배송메모/요청사항';

-- 기존 데이터 마이그레이션은 신규 주문 수집 시 자동으로 채워짐
-- 필요시 수동으로 실행: UPDATE orders SET receiver_address_base = receiver_address WHERE receiver_address_base IS NULL;
