-- V8: 쿠팡 카테고리별 수수료율 데이터 추가
-- 출처: https://cloud.mkt.coupang.com/Fee-Table (2019.11.25 기준)

-- 기존 DEFAULT 수수료율 유지하고, 카테고리별 수수료율 추가

-- 주요 카테고리 수수료율
INSERT INTO coupang_commission_rate (category_id, category_name, commission_rate, effective_from)
VALUES
    ('ELECTRONICS', '가전디지털', 7.80, '2019-11-25'),
    ('GAME', '게임', 6.80, '2019-11-25'),
    ('FURNITURE', '가구/홈인테리어', 10.80, '2019-11-25'),
    ('BOOK', '도서', 10.80, '2019-11-25'),
    ('MUSIC', '음반', 10.80, '2019-11-25'),
    ('OFFICE', '문구/사무용품', 10.80, '2019-11-25'),
    ('BABY', '출산/유아', 10.00, '2019-11-25'),
    ('SPORTS', '스포츠/레저용품', 10.80, '2019-11-25'),
    ('BEAUTY', '뷰티', 9.60, '2019-11-25'),
    ('LIVING', '생활용품', 7.80, '2019-11-25'),
    ('FOOD', '식품', 10.60, '2019-11-25'),
    ('TOY', '완구/취미', 10.80, '2019-11-25'),
    ('AUTO', '자동차용품', 10.00, '2019-11-25'),
    ('KITCHEN', '주방용품', 10.80, '2019-11-25'),
    ('FASHION', '패션', 10.50, '2019-11-25'),
    ('PET', '반려/애완용품', 10.80, '2019-11-25')
ON CONFLICT (category_id) DO UPDATE SET
    category_name = EXCLUDED.category_name,
    commission_rate = EXCLUDED.commission_rate,
    updated_at = NOW();

-- 세부 카테고리 수수료율 (특수 수수료 적용 품목)
INSERT INTO coupang_commission_rate (category_id, category_name, commission_rate, effective_from)
VALUES
    ('AIRCON', '냉난방에어컨', 5.80, '2019-11-25'),
    ('TABLET', '태블릿PC', 5.00, '2019-11-25'),
    ('COMPUTER', '컴퓨터', 5.00, '2019-11-25'),
    ('MONITOR', '모니터', 4.50, '2019-11-25'),
    ('DIAPER', '기저귀(일회용)', 6.40, '2019-11-25'),
    ('GOLF', '골프용품', 7.60, '2019-11-25'),
    ('FORMULA', '분유', 6.40, '2019-11-25'),
    ('GOLD', '순금/골드바/돌반지', 4.00, '2019-11-25')
ON CONFLICT (category_id) DO UPDATE SET
    category_name = EXCLUDED.category_name,
    commission_rate = EXCLUDED.commission_rate,
    updated_at = NOW();

-- 인덱스 추가 (카테고리명으로 검색 시 성능 향상)
CREATE INDEX IF NOT EXISTS idx_coupang_commission_rate_category_name
ON coupang_commission_rate (category_name);

-- 유효기간 기반 검색을 위한 인덱스
CREATE INDEX IF NOT EXISTS idx_coupang_commission_rate_effective
ON coupang_commission_rate (effective_from, effective_to);
