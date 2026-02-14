-- 배송 테이블에 출력 횟수 컬럼 추가
ALTER TABLE shipment ADD COLUMN IF NOT EXISTS print_count INTEGER NOT NULL DEFAULT 0;

COMMENT ON COLUMN shipment.print_count IS '전표 출력 횟수 (0: 미출력, 1이상: 출력됨)';
