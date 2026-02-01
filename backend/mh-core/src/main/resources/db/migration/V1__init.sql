-- ============================================================
-- Marketplace Hub - Initial Schema
-- ============================================================

-- Enable UUID extension
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- ============================================================
-- TENANT
-- ============================================================
CREATE TABLE tenant (
    id          UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    company_name    VARCHAR(255) NOT NULL,
    business_number VARCHAR(20) UNIQUE,
    contact_name    VARCHAR(100),
    contact_email   VARCHAR(255),
    contact_phone   VARCHAR(20),
    active          BOOLEAN NOT NULL DEFAULT TRUE,
    settings        JSONB,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW()
);

-- ============================================================
-- TENANT MARKETPLACE CREDENTIAL
-- ============================================================
CREATE TABLE tenant_marketplace_credential (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id       UUID NOT NULL REFERENCES tenant(id) ON DELETE CASCADE,
    marketplace_type VARCHAR(30) NOT NULL,
    seller_id       VARCHAR(255),
    client_id       VARCHAR(2000),
    client_secret   VARCHAR(2000),
    access_token    VARCHAR(4000),
    refresh_token   VARCHAR(4000),
    token_expires_at TIMESTAMP,
    active          BOOLEAN NOT NULL DEFAULT TRUE,
    extra_config    JSONB,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE(tenant_id, marketplace_type)
);

CREATE INDEX idx_tmc_tenant_active ON tenant_marketplace_credential(tenant_id, active);

-- ============================================================
-- TENANT COURIER CONFIG
-- ============================================================
CREATE TABLE tenant_courier_config (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id       UUID NOT NULL REFERENCES tenant(id) ON DELETE CASCADE,
    courier_type    VARCHAR(20) NOT NULL,
    api_key         VARCHAR(2000),
    api_secret      VARCHAR(2000),
    contract_code   VARCHAR(100),
    sender_name     VARCHAR(100),
    sender_phone    VARCHAR(20),
    sender_address  VARCHAR(500),
    sender_zipcode  VARCHAR(10),
    active          BOOLEAN NOT NULL DEFAULT TRUE,
    extra_config    JSONB,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE(tenant_id, courier_type)
);

-- ============================================================
-- TENANT ERP CONFIG
-- ============================================================
CREATE TABLE tenant_erp_config (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id       UUID NOT NULL REFERENCES tenant(id) ON DELETE CASCADE,
    erp_type        VARCHAR(20) NOT NULL,
    api_key         VARCHAR(2000),
    company_code    VARCHAR(100),
    active          BOOLEAN NOT NULL DEFAULT TRUE,
    field_mapping   JSONB,
    extra_config    JSONB,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE(tenant_id, erp_type)
);

-- ============================================================
-- ORDERS (Partitioned by month)
-- ============================================================
CREATE TABLE orders (
    id                          UUID NOT NULL DEFAULT uuid_generate_v4(),
    tenant_id                   UUID NOT NULL,
    marketplace_type            VARCHAR(30) NOT NULL,
    marketplace_order_id        VARCHAR(255) NOT NULL,
    marketplace_product_order_id VARCHAR(255),
    status                      VARCHAR(30) NOT NULL,
    marketplace_status          VARCHAR(100),
    buyer_name                  VARCHAR(100),
    buyer_phone                 VARCHAR(20),
    receiver_name               VARCHAR(100),
    receiver_phone              VARCHAR(20),
    receiver_address            VARCHAR(500),
    receiver_zipcode            VARCHAR(10),
    total_amount                NUMERIC(15,2),
    delivery_fee                NUMERIC(10,2),
    ordered_at                  TIMESTAMP,
    erp_synced                  BOOLEAN NOT NULL DEFAULT FALSE,
    raw_data                    JSONB,
    created_at                  TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at                  TIMESTAMP NOT NULL DEFAULT NOW(),
    PRIMARY KEY (id, created_at)
) PARTITION BY RANGE (created_at);

-- Create initial partitions (6 months)
CREATE TABLE orders_2025_09 PARTITION OF orders FOR VALUES FROM ('2025-09-01') TO ('2025-10-01');
CREATE TABLE orders_2025_10 PARTITION OF orders FOR VALUES FROM ('2025-10-01') TO ('2025-11-01');
CREATE TABLE orders_2025_11 PARTITION OF orders FOR VALUES FROM ('2025-11-01') TO ('2025-12-01');
CREATE TABLE orders_2025_12 PARTITION OF orders FOR VALUES FROM ('2025-12-01') TO ('2026-01-01');
CREATE TABLE orders_2026_01 PARTITION OF orders FOR VALUES FROM ('2026-01-01') TO ('2026-02-01');
CREATE TABLE orders_2026_02 PARTITION OF orders FOR VALUES FROM ('2026-02-01') TO ('2026-03-01');
CREATE TABLE orders_2026_03 PARTITION OF orders FOR VALUES FROM ('2026-03-01') TO ('2026-04-01');
CREATE TABLE orders_2026_04 PARTITION OF orders FOR VALUES FROM ('2026-04-01') TO ('2026-05-01');
CREATE TABLE orders_2026_05 PARTITION OF orders FOR VALUES FROM ('2026-05-01') TO ('2026-06-01');
CREATE TABLE orders_2026_06 PARTITION OF orders FOR VALUES FROM ('2026-06-01') TO ('2026-07-01');
CREATE TABLE orders_2026_07 PARTITION OF orders FOR VALUES FROM ('2026-07-01') TO ('2026-08-01');
CREATE TABLE orders_2026_08 PARTITION OF orders FOR VALUES FROM ('2026-08-01') TO ('2026-09-01');

-- Indexes on orders (applied to all partitions)
CREATE UNIQUE INDEX idx_orders_marketplace_unique
    ON orders(tenant_id, marketplace_type, marketplace_order_id, marketplace_product_order_id, created_at);
CREATE INDEX idx_orders_tenant_status ON orders(tenant_id, status);
CREATE INDEX idx_orders_tenant_erp ON orders(tenant_id, erp_synced) WHERE erp_synced = FALSE;
CREATE INDEX idx_orders_ordered_at ON orders(tenant_id, ordered_at);

-- ============================================================
-- ORDER ITEM
-- ============================================================
CREATE TABLE order_item (
    id                      UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    order_id                UUID NOT NULL,
    tenant_id               UUID NOT NULL,
    product_name            VARCHAR(500) NOT NULL,
    option_name             VARCHAR(500),
    quantity                INTEGER NOT NULL,
    unit_price              NUMERIC(15,2) NOT NULL,
    total_price             NUMERIC(15,2) NOT NULL,
    marketplace_product_id  VARCHAR(255),
    marketplace_sku         VARCHAR(255),
    created_at              TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_order_item_order ON order_item(order_id);
CREATE INDEX idx_order_item_tenant ON order_item(tenant_id);

-- ============================================================
-- ORDER STATUS LOG
-- ============================================================
CREATE TABLE order_status_log (
    id          UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    order_id    UUID NOT NULL,
    tenant_id   UUID NOT NULL,
    from_status VARCHAR(30),
    to_status   VARCHAR(30) NOT NULL,
    reason      VARCHAR(500),
    changed_by  VARCHAR(100),
    created_at  TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_osl_order ON order_status_log(order_id, created_at);
CREATE INDEX idx_osl_tenant ON order_status_log(tenant_id);

-- ============================================================
-- SHIPMENT
-- ============================================================
CREATE TABLE shipment (
    id                      UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    order_id                UUID NOT NULL,
    tenant_id               UUID NOT NULL,
    courier_type            VARCHAR(20) NOT NULL,
    tracking_number         VARCHAR(50),
    status                  VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    reserved_at             TIMESTAMP,
    shipped_at              TIMESTAMP,
    delivered_at            TIMESTAMP,
    waybill_url             VARCHAR(1000),
    marketplace_notified    BOOLEAN NOT NULL DEFAULT FALSE,
    created_at              TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_shipment_order ON shipment(order_id);
CREATE INDEX idx_shipment_tenant ON shipment(tenant_id);
CREATE INDEX idx_shipment_tracking ON shipment(tracking_number);

-- ============================================================
-- TRACKING NUMBER POOL
-- ============================================================
CREATE TABLE tracking_number_pool (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id       UUID NOT NULL,
    courier_type    VARCHAR(20) NOT NULL,
    tracking_number VARCHAR(50) NOT NULL UNIQUE,
    used            BOOLEAN NOT NULL DEFAULT FALSE,
    used_by_order_id UUID,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_tnp_available ON tracking_number_pool(tenant_id, courier_type, used) WHERE used = FALSE;

-- ============================================================
-- SETTLEMENT
-- ============================================================
CREATE TABLE settlement (
    id                  UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id           UUID NOT NULL,
    marketplace_type    VARCHAR(30) NOT NULL,
    settlement_date     DATE NOT NULL,
    order_count         INTEGER NOT NULL,
    total_sales         NUMERIC(15,2) NOT NULL,
    total_commission    NUMERIC(15,2),
    total_delivery_fee  NUMERIC(15,2),
    net_amount          NUMERIC(15,2) NOT NULL,
    erp_synced          BOOLEAN NOT NULL DEFAULT FALSE,
    erp_document_id     VARCHAR(100),
    created_at          TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_settlement_tenant ON settlement(tenant_id, settlement_date);
CREATE INDEX idx_settlement_erp ON settlement(tenant_id, erp_synced) WHERE erp_synced = FALSE;

-- ============================================================
-- ERP SYNC LOG
-- ============================================================
CREATE TABLE erp_sync_log (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id       UUID NOT NULL,
    entity_type     VARCHAR(50) NOT NULL,
    entity_id       UUID NOT NULL,
    status          VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    retry_count     INTEGER NOT NULL DEFAULT 0,
    error_message   VARCHAR(2000),
    request_payload JSONB,
    response_payload JSONB,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_erp_sync_tenant ON erp_sync_log(tenant_id, status);
CREATE INDEX idx_erp_sync_entity ON erp_sync_log(entity_type, entity_id);

-- ============================================================
-- JOB EXECUTION LOG
-- ============================================================
CREATE TABLE job_execution_log (
    id                  UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    job_name            VARCHAR(100) NOT NULL,
    tenant_id           UUID,
    started_at          TIMESTAMP NOT NULL,
    finished_at         TIMESTAMP,
    status              VARCHAR(20) NOT NULL,
    records_processed   INTEGER,
    error_message       VARCHAR(2000),
    metadata            JSONB
);

CREATE INDEX idx_job_log_name ON job_execution_log(job_name, started_at);

-- ============================================================
-- SHEDLOCK TABLE (for distributed locking)
-- ============================================================
CREATE TABLE shedlock (
    name        VARCHAR(64) NOT NULL,
    lock_until  TIMESTAMP NOT NULL,
    locked_at   TIMESTAMP NOT NULL,
    locked_by   VARCHAR(255) NOT NULL,
    PRIMARY KEY (name)
);

-- ============================================================
-- ROW LEVEL SECURITY (Supabase RLS)
-- ============================================================

-- Orders RLS
ALTER TABLE orders ENABLE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation_orders ON orders
    USING (tenant_id::text = current_setting('app.current_tenant_id', true));

-- Order Items RLS
ALTER TABLE order_item ENABLE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation_order_item ON order_item
    USING (tenant_id::text = current_setting('app.current_tenant_id', true));

-- Order Status Log RLS
ALTER TABLE order_status_log ENABLE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation_osl ON order_status_log
    USING (tenant_id::text = current_setting('app.current_tenant_id', true));

-- Shipment RLS
ALTER TABLE shipment ENABLE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation_shipment ON shipment
    USING (tenant_id::text = current_setting('app.current_tenant_id', true));

-- Tracking Number Pool RLS
ALTER TABLE tracking_number_pool ENABLE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation_tnp ON tracking_number_pool
    USING (tenant_id::text = current_setting('app.current_tenant_id', true));

-- Settlement RLS
ALTER TABLE settlement ENABLE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation_settlement ON settlement
    USING (tenant_id::text = current_setting('app.current_tenant_id', true));

-- ERP Sync Log RLS
ALTER TABLE erp_sync_log ENABLE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation_erp_sync ON erp_sync_log
    USING (tenant_id::text = current_setting('app.current_tenant_id', true));

-- Credential tables RLS
ALTER TABLE tenant_marketplace_credential ENABLE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation_tmc ON tenant_marketplace_credential
    USING (tenant_id::text = current_setting('app.current_tenant_id', true));

ALTER TABLE tenant_courier_config ENABLE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation_tcc ON tenant_courier_config
    USING (tenant_id::text = current_setting('app.current_tenant_id', true));

ALTER TABLE tenant_erp_config ENABLE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation_tec ON tenant_erp_config
    USING (tenant_id::text = current_setting('app.current_tenant_id', true));

-- ============================================================
-- PARTITION MANAGEMENT FUNCTION
-- ============================================================
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
END;
$$ LANGUAGE plpgsql;

-- ============================================================
-- AUTO PARTITION CREATION (run monthly via scheduler)
-- ============================================================
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
$$ LANGUAGE plpgsql;
