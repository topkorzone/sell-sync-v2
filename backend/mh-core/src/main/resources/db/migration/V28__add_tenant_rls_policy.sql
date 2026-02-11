-- ============================================================
-- Security Fixes for Supabase Security Advisor
-- ============================================================

-- 1. Add RLS policy to tenant table to allow self-access
ALTER TABLE tenant ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS tenant_self_access ON tenant;
CREATE POLICY tenant_self_access ON tenant
    FOR ALL
    USING (id::text = current_setting('app.current_tenant_id', true));

-- 2. Enable RLS on order partition tables (inherit policy from parent)
ALTER TABLE orders_2025_09 ENABLE ROW LEVEL SECURITY;
ALTER TABLE orders_2025_10 ENABLE ROW LEVEL SECURITY;
ALTER TABLE orders_2025_11 ENABLE ROW LEVEL SECURITY;
ALTER TABLE orders_2025_12 ENABLE ROW LEVEL SECURITY;
ALTER TABLE orders_2026_01 ENABLE ROW LEVEL SECURITY;
ALTER TABLE orders_2026_02 ENABLE ROW LEVEL SECURITY;
ALTER TABLE orders_2026_03 ENABLE ROW LEVEL SECURITY;
ALTER TABLE orders_2026_04 ENABLE ROW LEVEL SECURITY;
ALTER TABLE orders_2026_05 ENABLE ROW LEVEL SECURITY;
ALTER TABLE orders_2026_06 ENABLE ROW LEVEL SECURITY;
ALTER TABLE orders_2026_07 ENABLE ROW LEVEL SECURITY;
ALTER TABLE orders_2026_08 ENABLE ROW LEVEL SECURITY;

-- 3. Enable RLS on system/internal tables (with permissive policies for backend)
-- These tables don't have tenant_id, so we allow all access from backend
ALTER TABLE job_execution_log ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS job_log_backend_access ON job_execution_log;
CREATE POLICY job_log_backend_access ON job_execution_log FOR ALL USING (true);

ALTER TABLE shedlock ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS shedlock_backend_access ON shedlock;
CREATE POLICY shedlock_backend_access ON shedlock FOR ALL USING (true);

ALTER TABLE flyway_schema_history ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS flyway_backend_access ON flyway_schema_history;
CREATE POLICY flyway_backend_access ON flyway_schema_history FOR ALL USING (true);

-- 4. Enable RLS on reference data tables (read-only for all)
ALTER TABLE coupang_commission_rate ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS commission_rate_read ON coupang_commission_rate;
CREATE POLICY commission_rate_read ON coupang_commission_rate FOR SELECT USING (true);

ALTER TABLE coupang_category ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS category_read ON coupang_category;
CREATE POLICY category_read ON coupang_category FOR SELECT USING (true);

-- 5. Fix function search_path security
CREATE OR REPLACE FUNCTION create_monthly_partition(table_name TEXT, year INT, month INT)
RETURNS VOID AS $$
DECLARE
    partition_name TEXT;
    start_date TEXT;
    end_date TEXT;
    next_month INT;
    next_year INT;
BEGIN
    partition_name := table_name || '_' || year || '_' || LPAD(month::TEXT, 2, '0');
    start_date := year || '-' || LPAD(month::TEXT, 2, '0') || '-01';

    IF month = 12 THEN
        next_month := 1;
        next_year := year + 1;
    ELSE
        next_month := month + 1;
        next_year := year;
    END IF;
    end_date := next_year || '-' || LPAD(next_month::TEXT, 2, '0') || '-01';

    EXECUTE format(
        'CREATE TABLE IF NOT EXISTS %I PARTITION OF %I FOR VALUES FROM (%L) TO (%L)',
        partition_name, table_name, start_date, end_date
    );

    -- Enable RLS on new partition
    EXECUTE format('ALTER TABLE %I ENABLE ROW LEVEL SECURITY', partition_name);
END;
$$ LANGUAGE plpgsql SECURITY DEFINER SET search_path = public;

CREATE OR REPLACE FUNCTION ensure_future_partitions()
RETURNS VOID AS $$
DECLARE
    target_date DATE;
    i INT;
BEGIN
    FOR i IN 0..3 LOOP
        target_date := DATE_TRUNC('month', NOW()) + (i || ' months')::INTERVAL;
        PERFORM create_monthly_partition('orders',
            EXTRACT(YEAR FROM target_date)::INT,
            EXTRACT(MONTH FROM target_date)::INT);
    END LOOP;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER SET search_path = public;
