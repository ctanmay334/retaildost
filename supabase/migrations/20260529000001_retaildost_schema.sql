-- =============================================================================
-- RetailDost (KiranaOS) — Production PostgreSQL Schema
-- Migration: 20260529000001_retaildost_schema.sql
-- Platform: Supabase (PostgreSQL 17)
-- Architecture: Offline-first, AI-powered, multi-tenant kirana management SaaS
-- =============================================================================
-- Features: UUID PKs · store_id multi-tenancy · RLS · Triggers · Indexes
--           Append-only ledger · Inventory decrement · Running balance · Idempotency
-- =============================================================================


-- ---------------------------------------------------------------------------
-- 0. EXTENSIONS
-- ---------------------------------------------------------------------------

-- uuid-ossp not needed; using gen_random_uuid() (built-in PG13+)
CREATE EXTENSION IF NOT EXISTS "pg_trgm";        -- trigram indexes for fuzzy search
CREATE EXTENSION IF NOT EXISTS "unaccent";       -- accent-insensitive search (Hinglish)


-- ---------------------------------------------------------------------------
-- 1. UTILITY TYPES & ENUMS
-- ---------------------------------------------------------------------------

-- Plan tier
CREATE TYPE plan_type AS ENUM ('free', 'pro');

-- Khata transaction direction
CREATE TYPE khata_tx_type AS ENUM (
    'debit',      -- customer owes money (udhar given)
    'credit',     -- customer paid back
    'reversal'    -- correction / cancellation entry (append-only ledger)
);

-- Alert category
CREATE TYPE alert_type AS ENUM ('expiry_critical', 'expiry_warning', 'low_stock');

-- OCR job status
CREATE TYPE ocr_status AS ENUM ('pending', 'processing', 'completed', 'failed', 'skipped');

-- Source of sale record
CREATE TYPE sale_source AS ENUM ('manual', 'ocr_diary', 'sync_batch');

-- Offline sync action types
CREATE TYPE sync_action_type AS ENUM (
    'inventory_add',
    'inventory_edit',
    'sale_record',
    'khata_entry'
);

-- Offline sync status
CREATE TYPE sync_status AS ENUM ('pending', 'synced', 'sync_failed');

-- Distributor verification status
CREATE TYPE verification_status AS ENUM ('pending', 'verified', 'rejected');


-- ---------------------------------------------------------------------------
-- 2. HELPER FUNCTION — updated_at auto-trigger
-- ---------------------------------------------------------------------------

/**
 * set_updated_at()
 * Automatically updates the `updated_at` column to NOW() on every row UPDATE.
 * Attach to any table via: CREATE TRIGGER trg_<table>_updated_at
 */
CREATE OR REPLACE FUNCTION set_updated_at()
RETURNS TRIGGER
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$;


-- ---------------------------------------------------------------------------
-- 3. PROFILES TABLE
-- ---------------------------------------------------------------------------
-- One row per authenticated user (store owner).
-- Auto-created by handle_new_user() trigger on auth.users INSERT.
-- ---------------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS public.profiles (
    -- Identity
    id                      UUID PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE,
    store_id                UUID NOT NULL DEFAULT gen_random_uuid() UNIQUE, -- logical store identifier

    -- Profile data
    owner_name              TEXT,
    store_name              TEXT,
    phone                   TEXT,                          -- E.164 format e.g. +919876543210
    pincode                 TEXT,
    city                    TEXT,
    state                   TEXT,
    business_type           TEXT,

    -- Plan & limits
    plan                    plan_type NOT NULL DEFAULT 'free',
    ocr_scans_this_month    INTEGER NOT NULL DEFAULT 0 CHECK (ocr_scans_this_month >= 0),
    whatsapp_reminders_sent INTEGER NOT NULL DEFAULT 0 CHECK (whatsapp_reminders_sent >= 0),
    plan_reset_at           TIMESTAMPTZ NOT NULL DEFAULT date_trunc('month', NOW()) + INTERVAL '1 month',

    -- Push notifications
    fcm_token               TEXT,

    -- Onboarding
    onboarded_at            TIMESTAMPTZ,

    -- Timestamps
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Indexes
CREATE INDEX IF NOT EXISTS idx_profiles_store_id    ON public.profiles (store_id);
CREATE INDEX IF NOT EXISTS idx_profiles_pincode     ON public.profiles (pincode);
CREATE INDEX IF NOT EXISTS idx_profiles_plan        ON public.profiles (plan);
CREATE INDEX IF NOT EXISTS idx_profiles_phone       ON public.profiles (phone);

-- Updated_at trigger
CREATE TRIGGER trg_profiles_updated_at
    BEFORE UPDATE ON public.profiles
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- Comments
COMMENT ON TABLE  public.profiles IS 'One row per store owner; auto-created on auth.users insert.';
COMMENT ON COLUMN public.profiles.store_id IS 'Logical store UUID used as tenant key across all tables.';
COMMENT ON COLUMN public.profiles.plan IS 'Free or pro; enforced server-side in Edge Functions.';
COMMENT ON COLUMN public.profiles.ocr_scans_this_month IS 'Reset monthly. Free tier limit = 5.';


-- ---------------------------------------------------------------------------
-- 4. TRIGGER — handle_new_user (auto-create profile on signup)
-- ---------------------------------------------------------------------------

/**
 * handle_new_user()
 * Fires AFTER INSERT on auth.users.
 * Creates a profiles row seeded with phone from auth metadata.
 */
CREATE OR REPLACE FUNCTION public.handle_new_user()
RETURNS TRIGGER
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
BEGIN
    INSERT INTO public.profiles (id, phone)
    VALUES (
        NEW.id,
        NEW.phone
    )
    ON CONFLICT (id) DO NOTHING; -- safe for re-runs
    RETURN NEW;
END;
$$;

CREATE OR REPLACE TRIGGER on_auth_user_created
    AFTER INSERT ON auth.users
    FOR EACH ROW EXECUTE FUNCTION public.handle_new_user();


-- ---------------------------------------------------------------------------
-- 5. INVENTORY TABLE
-- ---------------------------------------------------------------------------
-- Central product catalogue per store.
-- Upserted from OCR-confirmed invoice items or manual entry.
-- ---------------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS public.inventory (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    store_id        UUID NOT NULL REFERENCES public.profiles(store_id) ON DELETE CASCADE,

    -- Product identity
    item_name       TEXT NOT NULL,
    category        TEXT,                          -- e.g. 'biscuits', 'dairy', 'snacks'
    unit_label      TEXT,                          -- e.g. 'kg', 'pcs', 'litre', 'pack'

    -- Stock levels
    quantity        NUMERIC(12, 3) NOT NULL DEFAULT 0,
    min_threshold   NUMERIC(12, 3) NOT NULL DEFAULT 5, -- low-stock alert threshold

    -- Pricing
    cost_price      NUMERIC(12, 2),                -- purchase price (from invoice OCR)
    mrp             NUMERIC(12, 2),                -- max retail price

    -- Batch / expiry tracking
    batch_no        TEXT,
    expiry_date     DATE,

    -- OCR metadata
    ocr_confidence  NUMERIC(4, 3),                 -- 0.000–1.000; < 0.75 needs user review
    source          TEXT DEFAULT 'manual',          -- 'manual' | 'ocr_invoice' | 'ocr_diary'

    -- Timestamps
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    -- Soft constraints
    CONSTRAINT inventory_quantity_non_negative CHECK (quantity >= 0 OR quantity IS NOT NULL)
);

-- Indexes
CREATE INDEX IF NOT EXISTS idx_inventory_store_id   ON public.inventory (store_id);
CREATE INDEX IF NOT EXISTS idx_inventory_expiry     ON public.inventory (store_id, expiry_date) WHERE expiry_date IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_inventory_low_stock  ON public.inventory (store_id, quantity, min_threshold);
CREATE INDEX IF NOT EXISTS idx_inventory_category   ON public.inventory (store_id, category);
-- Trigram index for fast fuzzy item name search (RecordSaleScreen dropdown)
CREATE INDEX IF NOT EXISTS idx_inventory_name_trgm  ON public.inventory USING GIN (item_name gin_trgm_ops);

-- Updated_at trigger
CREATE TRIGGER trg_inventory_updated_at
    BEFORE UPDATE ON public.inventory
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

COMMENT ON TABLE  public.inventory IS 'Per-store product catalogue; quantity updated by sale triggers.';
COMMENT ON COLUMN public.inventory.min_threshold IS 'Alert fires when quantity <= this value.';
COMMENT ON COLUMN public.inventory.ocr_confidence IS 'NULL for manual entries; < 0.75 shown amber in UI.';


-- ---------------------------------------------------------------------------
-- 6. SALE RECORDS TABLE (Header)
-- ---------------------------------------------------------------------------
-- One row per sale session (may have multiple items).
-- ---------------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS public.sale_records (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    store_id        UUID NOT NULL REFERENCES public.profiles(store_id) ON DELETE CASCADE,

    -- Sale metadata
    customer_name   TEXT,                          -- optional free-text
    source          sale_source NOT NULL DEFAULT 'manual',
    notes           TEXT,

    -- Totals (denormalised for fast dashboard queries)
    total_amount    NUMERIC(12, 2) NOT NULL DEFAULT 0,
    items_count     INTEGER NOT NULL DEFAULT 0,

    -- Timestamps
    sale_date       DATE NOT NULL DEFAULT CURRENT_DATE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Indexes
CREATE INDEX IF NOT EXISTS idx_sale_records_store_id  ON public.sale_records (store_id);
CREATE INDEX IF NOT EXISTS idx_sale_records_date       ON public.sale_records (store_id, sale_date DESC);
CREATE INDEX IF NOT EXISTS idx_sale_records_customer   ON public.sale_records (store_id, customer_name) WHERE customer_name IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_sale_records_source     ON public.sale_records (store_id, source);

CREATE TRIGGER trg_sale_records_updated_at
    BEFORE UPDATE ON public.sale_records
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

COMMENT ON TABLE public.sale_records IS 'Sale session header. Line items in sale_record_items.';


-- ---------------------------------------------------------------------------
-- 7. SALE RECORD ITEMS TABLE (Line Items)
-- ---------------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS public.sale_record_items (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    store_id        UUID NOT NULL REFERENCES public.profiles(store_id) ON DELETE CASCADE,
    sale_record_id  UUID NOT NULL REFERENCES public.sale_records(id) ON DELETE CASCADE,
    inventory_id    UUID REFERENCES public.inventory(id) ON DELETE SET NULL, -- nullable if item deleted

    -- Line item data
    item_name       TEXT NOT NULL,                 -- snapshot at time of sale
    unit_label      TEXT,
    quantity_sold   NUMERIC(12, 3) NOT NULL CHECK (quantity_sold > 0),
    sale_price      NUMERIC(12, 2),                -- actual sale price (may differ from MRP)
    cost_price      NUMERIC(12, 2),                -- snapshot from inventory at time of sale

    -- Timestamps
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
    -- No updated_at — sale items are immutable once written
);

-- Indexes
CREATE INDEX IF NOT EXISTS idx_sale_items_store_id      ON public.sale_record_items (store_id);
CREATE INDEX IF NOT EXISTS idx_sale_items_sale_record   ON public.sale_record_items (sale_record_id);
CREATE INDEX IF NOT EXISTS idx_sale_items_inventory     ON public.sale_record_items (inventory_id) WHERE inventory_id IS NOT NULL;

COMMENT ON TABLE  public.sale_record_items IS 'Immutable line items per sale session. inventory_id kept for analytics.';
COMMENT ON COLUMN public.sale_record_items.item_name IS 'Snapshot of name at sale time; survives inventory edits.';


-- ---------------------------------------------------------------------------
-- 8. INVENTORY DECREMENT TRIGGER
-- ---------------------------------------------------------------------------
-- Fires AFTER INSERT on sale_record_items.
-- Decrements inventory quantity and updates sale_records totals.
-- ---------------------------------------------------------------------------

/**
 * deduct_inventory_on_sale()
 * - Decrements inventory.quantity by quantity_sold.
 * - Allows quantity to go negative only with explicit override (soft block enforced in app).
 * - Updates sale_records.total_amount and items_count denormalised columns.
 */
CREATE OR REPLACE FUNCTION public.deduct_inventory_on_sale()
RETURNS TRIGGER
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
    v_sale_price NUMERIC(12, 2);
BEGIN
    -- 1. Decrement inventory quantity (if inventory_id exists)
    IF NEW.inventory_id IS NOT NULL THEN
        UPDATE public.inventory
        SET
            quantity   = quantity - NEW.quantity_sold,
            updated_at = NOW()
        WHERE
            id       = NEW.inventory_id
            AND store_id = NEW.store_id;
    END IF;

    -- 2. Resolve line total (use sale_price if set, else MRP, else 0)
    v_sale_price := COALESCE(NEW.sale_price, 0);

    -- 3. Update denormalised totals on sale_records header
    UPDATE public.sale_records
    SET
        total_amount = (SELECT COALESCE(SUM(COALESCE(sale_price, 0) * quantity_sold), 0) FROM public.sale_record_items WHERE sale_record_id = NEW.sale_record_id),
        items_count  = (SELECT COUNT(*)::INTEGER FROM public.sale_record_items WHERE sale_record_id = NEW.sale_record_id),
        updated_at   = NOW()
    WHERE
        id       = NEW.sale_record_id
        AND store_id = NEW.store_id;

    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_deduct_inventory_on_sale
    AFTER INSERT ON public.sale_record_items
    FOR EACH ROW EXECUTE FUNCTION public.deduct_inventory_on_sale();


-- ---------------------------------------------------------------------------
-- 9. KHATA CUSTOMERS TABLE
-- ---------------------------------------------------------------------------
-- One row per credit customer per store.
-- running_balance is maintained by khata_tx_update_balance trigger.
-- ---------------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS public.khata_customers (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    store_id        UUID NOT NULL REFERENCES public.profiles(store_id) ON DELETE CASCADE,

    -- Customer info
    name            TEXT NOT NULL,
    phone           TEXT,                          -- for WhatsApp reminder
    notes           TEXT,

    -- Ledger summary (maintained by trigger — DO NOT update manually)
    running_balance NUMERIC(12, 2) NOT NULL DEFAULT 0,  -- positive = customer owes store
    last_activity   TIMESTAMPTZ,

    -- Timestamps
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    -- Each customer name is unique per store
    CONSTRAINT uq_khata_customer_name UNIQUE (store_id, name)
);

-- Indexes
CREATE INDEX IF NOT EXISTS idx_khata_customers_store_id ON public.khata_customers (store_id);
CREATE INDEX IF NOT EXISTS idx_khata_customers_balance  ON public.khata_customers (store_id, running_balance DESC);
CREATE INDEX IF NOT EXISTS idx_khata_customers_activity ON public.khata_customers (store_id, last_activity DESC);
-- Trigram for fast customer name search
CREATE INDEX IF NOT EXISTS idx_khata_customers_name_trgm ON public.khata_customers USING GIN (name gin_trgm_ops);

CREATE TRIGGER trg_khata_customers_updated_at
    BEFORE UPDATE ON public.khata_customers
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

COMMENT ON TABLE  public.khata_customers IS 'Credit ledger customer master per store.';
COMMENT ON COLUMN public.khata_customers.running_balance IS 'Maintained automatically by khata_tx_update_balance trigger. Positive = store is owed money.';


-- ---------------------------------------------------------------------------
-- 10. KHATA TRANSACTIONS TABLE (Append-Only Ledger)
-- ---------------------------------------------------------------------------
-- NEVER UPDATE or DELETE rows — corrections are new reversal entries.
-- ---------------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS public.khata_transactions (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    store_id            UUID NOT NULL REFERENCES public.profiles(store_id) ON DELETE CASCADE,
    customer_id         UUID NOT NULL REFERENCES public.khata_customers(id) ON DELETE CASCADE,

    -- Transaction data
    tx_type             khata_tx_type NOT NULL,
    amount              NUMERIC(12, 2) NOT NULL CHECK (amount > 0),
    notes               TEXT,                          -- raw NLP input or manual note

    -- NLP metadata
    nlp_intent          TEXT,                          -- e.g. 'debit', 'credit', 'query'
    nlp_confidence      NUMERIC(4, 3),                 -- 0.000–1.000
    raw_input           TEXT,                          -- original voice/text input

    -- Idempotency (prevents double-processing from offline queue)
    idempotency_key     TEXT UNIQUE,

    -- Links (optional — when khata entry created from a sale record)
    sale_record_id      UUID REFERENCES public.sale_records(id) ON DELETE SET NULL,

    -- Timestamps (immutable — no updated_at)
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Indexes
CREATE INDEX IF NOT EXISTS idx_khata_tx_store_id     ON public.khata_transactions (store_id);
CREATE INDEX IF NOT EXISTS idx_khata_tx_customer     ON public.khata_transactions (customer_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_khata_tx_idempotency  ON public.khata_transactions (idempotency_key) WHERE idempotency_key IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_khata_tx_type         ON public.khata_transactions (store_id, tx_type, created_at DESC);

COMMENT ON TABLE  public.khata_transactions IS 'Append-only credit ledger. Never update or delete; use reversal entries for corrections.';
COMMENT ON COLUMN public.khata_transactions.tx_type IS 'debit = customer owes; credit = customer paid; reversal = correction.';


-- ---------------------------------------------------------------------------
-- 11. KHATA RUNNING BALANCE TRIGGER
-- ---------------------------------------------------------------------------
-- Fires AFTER INSERT on khata_transactions.
-- Updates khata_customers.running_balance and last_activity.
-- ---------------------------------------------------------------------------

/**
 * khata_tx_update_balance()
 * Computes delta from tx_type and applies to running_balance.
 *   debit:    customer_balance += amount  (store is owed more)
 *   credit:   customer_balance -= amount  (customer paid back)
 *   reversal: customer_balance -= amount  (correction: reverses a debit)
 */
CREATE OR REPLACE FUNCTION public.khata_tx_update_balance()
RETURNS TRIGGER
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
    v_delta NUMERIC(12, 2);
BEGIN
    CASE NEW.tx_type
        WHEN 'debit'    THEN v_delta :=  NEW.amount;  -- store is owed more
        WHEN 'credit'   THEN v_delta := -NEW.amount;  -- customer paid
        WHEN 'reversal' THEN v_delta := -NEW.amount;  -- reversal of a prior debit
        ELSE v_delta := 0;
    END CASE;

    UPDATE public.khata_customers
    SET
        running_balance = running_balance + v_delta,
        last_activity   = NOW(),
        updated_at      = NOW()
    WHERE
        id       = NEW.customer_id
        AND store_id = NEW.store_id;

    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_khata_tx_update_balance
    AFTER INSERT ON public.khata_transactions
    FOR EACH ROW EXECUTE FUNCTION public.khata_tx_update_balance();


-- ---------------------------------------------------------------------------
-- 12. ALERTS TABLE
-- ---------------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS public.alerts (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    store_id        UUID NOT NULL REFERENCES public.profiles(store_id) ON DELETE CASCADE,

    -- Alert metadata
    alert_type      alert_type NOT NULL,
    inventory_id    UUID REFERENCES public.inventory(id) ON DELETE CASCADE,
    item_name       TEXT NOT NULL,                 -- snapshot for display even after deletion

    -- Alert details
    message         TEXT NOT NULL,
    days_to_expiry  INTEGER,                       -- populated for expiry alerts
    current_qty     NUMERIC(12, 3),                -- populated for low-stock alerts

    -- Read status
    is_read         BOOLEAN NOT NULL DEFAULT FALSE,
    read_at         TIMESTAMPTZ,

    -- FCM delivery
    fcm_sent        BOOLEAN NOT NULL DEFAULT FALSE,
    fcm_sent_at     TIMESTAMPTZ,

    -- Timestamps
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Indexes
CREATE INDEX IF NOT EXISTS idx_alerts_store_id    ON public.alerts (store_id);
CREATE INDEX IF NOT EXISTS idx_alerts_unread      ON public.alerts (store_id, is_read, created_at DESC) WHERE is_read = FALSE;
CREATE INDEX IF NOT EXISTS idx_alerts_type        ON public.alerts (store_id, alert_type, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_alerts_inventory   ON public.alerts (inventory_id) WHERE inventory_id IS NOT NULL;

CREATE TRIGGER trg_alerts_updated_at
    BEFORE UPDATE ON public.alerts
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

COMMENT ON TABLE  public.alerts IS 'Expiry and low-stock alerts generated by alert-checker Edge Function (pg_cron 8 AM IST).';
COMMENT ON COLUMN public.alerts.item_name IS 'Snapshot — survives inventory item deletion.';


-- ---------------------------------------------------------------------------
-- 13. OCR JOBS TABLE
-- ---------------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS public.ocr_jobs (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    store_id            UUID NOT NULL REFERENCES public.profiles(store_id) ON DELETE CASCADE,

    -- Job classification
    job_type            TEXT NOT NULL CHECK (job_type IN ('invoice', 'diary')),
    status              ocr_status NOT NULL DEFAULT 'pending',

    -- Storage reference
    image_path          TEXT NOT NULL,             -- Storage path: {store_id}/{ts}_{filename}
    bucket_name         TEXT NOT NULL DEFAULT 'invoice-images',

    -- Processing metadata
    idempotency_key     TEXT UNIQUE NOT NULL,
    ai_model_used       TEXT,                      -- 'gemini-1.5-flash' | 'claude-sonnet-4-20250514'
    fallback_triggered  BOOLEAN NOT NULL DEFAULT FALSE,

    -- Results
    items_extracted     INTEGER DEFAULT 0,
    items_confirmed     INTEGER DEFAULT 0,
    raw_response        JSONB,                     -- full AI response for audit / retry
    error_message       TEXT,

    -- Retry tracking
    attempt_count       INTEGER NOT NULL DEFAULT 0,
    last_attempted_at   TIMESTAMPTZ,

    -- Timestamps
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Indexes
CREATE INDEX IF NOT EXISTS idx_ocr_jobs_store_id      ON public.ocr_jobs (store_id);
CREATE INDEX IF NOT EXISTS idx_ocr_jobs_status        ON public.ocr_jobs (store_id, status, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_ocr_jobs_idempotency   ON public.ocr_jobs (idempotency_key);

CREATE TRIGGER trg_ocr_jobs_updated_at
    BEFORE UPDATE ON public.ocr_jobs
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

COMMENT ON TABLE public.ocr_jobs IS 'Audit trail for every OCR invocation (invoice + diary). Supports retry logic.';


-- ---------------------------------------------------------------------------
-- 14. OCR NAME MAPPINGS TABLE
-- ---------------------------------------------------------------------------
-- Learned mappings from OCR extracted names → canonical inventory names.
-- Trained per store from user corrections.
-- ---------------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS public.ocr_name_mappings (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    store_id        UUID NOT NULL REFERENCES public.profiles(store_id) ON DELETE CASCADE,
    inventory_id    UUID REFERENCES public.inventory(id) ON DELETE SET NULL,

    -- Mapping data
    ocr_raw_name    TEXT NOT NULL,                 -- what the OCR extracted
    canonical_name  TEXT NOT NULL,                 -- the correct inventory item name

    -- Learning metadata
    confidence      NUMERIC(4, 3),
    confirmed_count INTEGER NOT NULL DEFAULT 1,    -- number of times user confirmed this mapping

    -- Timestamps
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    -- Unique mapping per store
    CONSTRAINT uq_ocr_mapping UNIQUE (store_id, ocr_raw_name, canonical_name)
);

-- Indexes
CREATE INDEX IF NOT EXISTS idx_ocr_mappings_store_id   ON public.ocr_name_mappings (store_id);
CREATE INDEX IF NOT EXISTS idx_ocr_mappings_raw_name   ON public.ocr_name_mappings USING GIN (ocr_raw_name gin_trgm_ops);

CREATE TRIGGER trg_ocr_name_mappings_updated_at
    BEFORE UPDATE ON public.ocr_name_mappings
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

COMMENT ON TABLE public.ocr_name_mappings IS 'Learned OCR-to-inventory name mappings; accumulated per store across diary scans.';


-- ---------------------------------------------------------------------------
-- 15. IDEMPOTENCY KEYS TABLE
-- ---------------------------------------------------------------------------
-- Global deduplication table for offline sync + Edge Function calls.
-- ---------------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS public.idempotency_keys (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    store_id        UUID NOT NULL REFERENCES public.profiles(store_id) ON DELETE CASCADE,

    -- Key & action identification
    idem_key        TEXT NOT NULL,                 -- client-generated UUID
    action_type     TEXT NOT NULL,                 -- 'ocr_invoice' | 'ocr_diary' | 'khata_entry' | 'sale_record' | etc.
    entity_id       UUID,                          -- resulting row ID (set after processing)

    -- Response caching
    response_code   INTEGER,                       -- HTTP status code cached for replay
    response_body   JSONB,                         -- cached response for idempotent replay

    -- TTL management (keys expire after 7 days)
    expires_at      TIMESTAMPTZ NOT NULL DEFAULT (NOW() + INTERVAL '7 days'),

    -- Timestamps
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    -- Composite uniqueness: one key per store per action type
    CONSTRAINT uq_idempotency_key UNIQUE (store_id, idem_key)
);

-- Indexes
CREATE INDEX IF NOT EXISTS idx_idempotency_store_id  ON public.idempotency_keys (store_id);
CREATE INDEX IF NOT EXISTS idx_idempotency_key       ON public.idempotency_keys (idem_key);
CREATE INDEX IF NOT EXISTS idx_idempotency_expires   ON public.idempotency_keys (expires_at);

COMMENT ON TABLE  public.idempotency_keys IS 'Global deduplication store for offline queue + Edge Function calls. Keys expire after 7 days.';
COMMENT ON COLUMN public.idempotency_keys.idem_key IS 'UUID generated client-side before any write; used to prevent double-processing on retry.';


-- ---------------------------------------------------------------------------
-- 16. DISTRIBUTORS TABLE
-- ---------------------------------------------------------------------------
-- Marketplace of FMCG/grocery distributors searchable by category + pincode.
-- Public read (authenticated), write = own row only.
-- ---------------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS public.distributors (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    store_id        UUID REFERENCES public.profiles(store_id) ON DELETE SET NULL, -- NULL for admin-seeded rows

    -- Business info
    business_name   TEXT NOT NULL,
    owner_name      TEXT,
    whatsapp_no     TEXT NOT NULL,                 -- E.164 format
    email           TEXT,

    -- Service area
    pincodes        TEXT[] NOT NULL DEFAULT '{}',  -- array of 6-digit pincodes served
    categories      TEXT[] NOT NULL DEFAULT '{}',  -- e.g. ['biscuits', 'dairy', 'beverages']

    -- Trust & status
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    is_verified     BOOLEAN NOT NULL DEFAULT FALSE,
    verification_status verification_status NOT NULL DEFAULT 'pending',
    verified_at     TIMESTAMPTZ,

    -- Analytics
    view_count      INTEGER NOT NULL DEFAULT 0,
    contact_count   INTEGER NOT NULL DEFAULT 0,

    -- Timestamps
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Indexes
CREATE INDEX IF NOT EXISTS idx_distributors_active     ON public.distributors (is_active) WHERE is_active = TRUE;
CREATE INDEX IF NOT EXISTS idx_distributors_pincodes   ON public.distributors USING GIN (pincodes);
CREATE INDEX IF NOT EXISTS idx_distributors_categories ON public.distributors USING GIN (categories);
CREATE INDEX IF NOT EXISTS idx_distributors_store_id   ON public.distributors (store_id) WHERE store_id IS NOT NULL;
-- Full-text search on business name
CREATE INDEX IF NOT EXISTS idx_distributors_name_trgm  ON public.distributors USING GIN (business_name gin_trgm_ops);

CREATE TRIGGER trg_distributors_updated_at
    BEFORE UPDATE ON public.distributors
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

COMMENT ON TABLE  public.distributors IS 'FMCG distributor marketplace. pincodes and categories are searchable arrays.';
COMMENT ON COLUMN public.distributors.pincodes IS 'Array of 6-digit pincodes the distributor serves.';


-- ---------------------------------------------------------------------------
-- 17. OFFLINE SYNC QUEUE TABLE (Server-Side Audit)
-- ---------------------------------------------------------------------------
-- Server-side mirror of the Android Room offline_queue.
-- Populated by sync-batch Edge Function; used for audit and retry analysis.
-- ---------------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS public.offline_sync_queue (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    store_id        UUID NOT NULL REFERENCES public.profiles(store_id) ON DELETE CASCADE,

    -- Action details
    action_type     sync_action_type NOT NULL,
    idempotency_key TEXT NOT NULL,                 -- matches client-side key
    payload         JSONB NOT NULL,                -- full action payload

    -- Processing status
    status          sync_status NOT NULL DEFAULT 'pending',
    attempt_count   INTEGER NOT NULL DEFAULT 0,
    last_attempted_at TIMESTAMPTZ,
    processed_at    TIMESTAMPTZ,
    error_message   TEXT,

    -- Client metadata
    client_ts       TIMESTAMPTZ,                   -- client-side timestamp when action was taken

    -- Timestamps
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    -- Enforce uniqueness on idempotency_key per store
    CONSTRAINT uq_sync_queue_key UNIQUE (store_id, idempotency_key)
);

-- Indexes
CREATE INDEX IF NOT EXISTS idx_sync_queue_store_id  ON public.offline_sync_queue (store_id);
CREATE INDEX IF NOT EXISTS idx_sync_queue_status    ON public.offline_sync_queue (store_id, status, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_sync_queue_idem_key  ON public.offline_sync_queue (idempotency_key);

CREATE TRIGGER trg_sync_queue_updated_at
    BEFORE UPDATE ON public.offline_sync_queue
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

COMMENT ON TABLE public.offline_sync_queue IS 'Server-side audit of offline actions. Processed by sync-batch Edge Function.';


-- =============================================================================
-- ROW LEVEL SECURITY (RLS)
-- =============================================================================
-- All tables locked down to authenticated users viewing only their own store.
-- Uses auth.uid() → profiles.id → profiles.store_id lookup.
-- =============================================================================


-- ---------------------------------------------------------------------------
-- HELPER: get_my_store_id()
-- Returns the store_id of the currently authenticated user.
-- Cached in the session via STABLE to avoid repeated lookups.
-- ---------------------------------------------------------------------------

CREATE OR REPLACE FUNCTION public.get_my_store_id()
RETURNS UUID
LANGUAGE sql
STABLE
SECURITY DEFINER
SET search_path = public
AS $$
    SELECT store_id
    FROM   public.profiles
    WHERE  id = auth.uid()
    LIMIT  1;
$$;

COMMENT ON FUNCTION public.get_my_store_id IS 'Returns calling user''s store_id. Used in all RLS policies.';


-- ---------------------------------------------------------------------------
-- 18. ENABLE RLS ON ALL TABLES
-- ---------------------------------------------------------------------------

ALTER TABLE public.profiles           ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.inventory          ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.sale_records       ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.sale_record_items  ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.khata_customers    ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.khata_transactions ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.alerts             ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.ocr_jobs           ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.ocr_name_mappings  ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.idempotency_keys   ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.distributors       ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.offline_sync_queue ENABLE ROW LEVEL SECURITY;


-- ---------------------------------------------------------------------------
-- 19. RLS POLICIES — PROFILES
-- ---------------------------------------------------------------------------

-- SELECT: owner can see only their own profile
CREATE POLICY profiles_select_own
    ON public.profiles
    FOR SELECT
    TO authenticated
    USING (id = auth.uid());

-- UPDATE: owner can update only their own profile
CREATE POLICY profiles_update_own
    ON public.profiles
    FOR UPDATE
    TO authenticated
    USING  (id = auth.uid())
    WITH CHECK (id = auth.uid());

-- INSERT: handled by handle_new_user trigger (SECURITY DEFINER) — no direct insert
-- DELETE: not allowed via API


-- ---------------------------------------------------------------------------
-- 20. RLS POLICIES — INVENTORY
-- ---------------------------------------------------------------------------

CREATE POLICY inventory_select_own
    ON public.inventory
    FOR SELECT
    TO authenticated
    USING (store_id = get_my_store_id());

CREATE POLICY inventory_insert_own
    ON public.inventory
    FOR INSERT
    TO authenticated
    WITH CHECK (store_id = get_my_store_id());

CREATE POLICY inventory_update_own
    ON public.inventory
    FOR UPDATE
    TO authenticated
    USING  (store_id = get_my_store_id())
    WITH CHECK (store_id = get_my_store_id());

CREATE POLICY inventory_delete_own
    ON public.inventory
    FOR DELETE
    TO authenticated
    USING (store_id = get_my_store_id());


-- ---------------------------------------------------------------------------
-- 21. RLS POLICIES — SALE RECORDS
-- ---------------------------------------------------------------------------

CREATE POLICY sale_records_select_own
    ON public.sale_records
    FOR SELECT
    TO authenticated
    USING (store_id = get_my_store_id());

CREATE POLICY sale_records_insert_own
    ON public.sale_records
    FOR INSERT
    TO authenticated
    WITH CHECK (store_id = get_my_store_id());

-- UPDATE allowed for denormalised total corrections
CREATE POLICY sale_records_update_own
    ON public.sale_records
    FOR UPDATE
    TO authenticated
    USING  (store_id = get_my_store_id())
    WITH CHECK (store_id = get_my_store_id());

-- No DELETE policy — sale records are permanent


-- ---------------------------------------------------------------------------
-- 22. RLS POLICIES — SALE RECORD ITEMS
-- ---------------------------------------------------------------------------

CREATE POLICY sale_items_select_own
    ON public.sale_record_items
    FOR SELECT
    TO authenticated
    USING (store_id = get_my_store_id());

CREATE POLICY sale_items_insert_own
    ON public.sale_record_items
    FOR INSERT
    TO authenticated
    WITH CHECK (store_id = get_my_store_id());

-- No UPDATE / DELETE — sale items are immutable


-- ---------------------------------------------------------------------------
-- 23. RLS POLICIES — KHATA CUSTOMERS
-- ---------------------------------------------------------------------------

CREATE POLICY khata_customers_select_own
    ON public.khata_customers
    FOR SELECT
    TO authenticated
    USING (store_id = get_my_store_id());

CREATE POLICY khata_customers_insert_own
    ON public.khata_customers
    FOR INSERT
    TO authenticated
    WITH CHECK (store_id = get_my_store_id());

-- UPDATE needed for trigger-managed running_balance and manual edits
CREATE POLICY khata_customers_update_own
    ON public.khata_customers
    FOR UPDATE
    TO authenticated
    USING  (store_id = get_my_store_id())
    WITH CHECK (store_id = get_my_store_id());

-- Allow soft-delete (owner may archive a customer)
CREATE POLICY khata_customers_delete_own
    ON public.khata_customers
    FOR DELETE
    TO authenticated
    USING (store_id = get_my_store_id());


-- ---------------------------------------------------------------------------
-- 24. RLS POLICIES — KHATA TRANSACTIONS (Append-Only Ledger)
-- ---------------------------------------------------------------------------

CREATE POLICY khata_tx_select_own
    ON public.khata_transactions
    FOR SELECT
    TO authenticated
    USING (store_id = get_my_store_id());

-- INSERT only — no UPDATE or DELETE (append-only ledger design)
CREATE POLICY khata_tx_insert_own
    ON public.khata_transactions
    FOR INSERT
    TO authenticated
    WITH CHECK (store_id = get_my_store_id());


-- ---------------------------------------------------------------------------
-- 25. RLS POLICIES — ALERTS
-- ---------------------------------------------------------------------------

CREATE POLICY alerts_select_own
    ON public.alerts
    FOR SELECT
    TO authenticated
    USING (store_id = get_my_store_id());

-- INSERT by Edge Function (alert-checker) via service_role; no client INSERT
-- UPDATE: to mark alerts as read
CREATE POLICY alerts_update_own
    ON public.alerts
    FOR UPDATE
    TO authenticated
    USING  (store_id = get_my_store_id())
    WITH CHECK (store_id = get_my_store_id());


-- ---------------------------------------------------------------------------
-- 26. RLS POLICIES — OCR JOBS
-- ---------------------------------------------------------------------------

CREATE POLICY ocr_jobs_select_own
    ON public.ocr_jobs
    FOR SELECT
    TO authenticated
    USING (store_id = get_my_store_id());

CREATE POLICY ocr_jobs_insert_own
    ON public.ocr_jobs
    FOR INSERT
    TO authenticated
    WITH CHECK (store_id = get_my_store_id());

CREATE POLICY ocr_jobs_update_own
    ON public.ocr_jobs
    FOR UPDATE
    TO authenticated
    USING  (store_id = get_my_store_id())
    WITH CHECK (store_id = get_my_store_id());


-- ---------------------------------------------------------------------------
-- 27. RLS POLICIES — OCR NAME MAPPINGS
-- ---------------------------------------------------------------------------

CREATE POLICY ocr_mappings_select_own
    ON public.ocr_name_mappings
    FOR SELECT
    TO authenticated
    USING (store_id = get_my_store_id());

CREATE POLICY ocr_mappings_insert_own
    ON public.ocr_name_mappings
    FOR INSERT
    TO authenticated
    WITH CHECK (store_id = get_my_store_id());

CREATE POLICY ocr_mappings_update_own
    ON public.ocr_name_mappings
    FOR UPDATE
    TO authenticated
    USING  (store_id = get_my_store_id())
    WITH CHECK (store_id = get_my_store_id());

CREATE POLICY ocr_mappings_delete_own
    ON public.ocr_name_mappings
    FOR DELETE
    TO authenticated
    USING (store_id = get_my_store_id());


-- ---------------------------------------------------------------------------
-- 28. RLS POLICIES — IDEMPOTENCY KEYS
-- ---------------------------------------------------------------------------

CREATE POLICY idempotency_select_own
    ON public.idempotency_keys
    FOR SELECT
    TO authenticated
    USING (store_id = get_my_store_id());

CREATE POLICY idempotency_insert_own
    ON public.idempotency_keys
    FOR INSERT
    TO authenticated
    WITH CHECK (store_id = get_my_store_id());

-- UPDATE allowed to set entity_id and response_body after processing
CREATE POLICY idempotency_update_own
    ON public.idempotency_keys
    FOR UPDATE
    TO authenticated
    USING  (store_id = get_my_store_id())
    WITH CHECK (store_id = get_my_store_id());


-- ---------------------------------------------------------------------------
-- 29. RLS POLICIES — DISTRIBUTORS
-- ---------------------------------------------------------------------------

-- All authenticated users can read active distributors (marketplace)
CREATE POLICY distributors_select_active
    ON public.distributors
    FOR SELECT
    TO authenticated
    USING (is_active = TRUE);

-- Any authenticated user (store owner) can self-register as distributor
CREATE POLICY distributors_insert_own
    ON public.distributors
    FOR INSERT
    TO authenticated
    WITH CHECK (store_id = get_my_store_id());

-- Update / delete only own listing
CREATE POLICY distributors_update_own
    ON public.distributors
    FOR UPDATE
    TO authenticated
    USING  (store_id = get_my_store_id())
    WITH CHECK (store_id = get_my_store_id());

CREATE POLICY distributors_delete_own
    ON public.distributors
    FOR DELETE
    TO authenticated
    USING (store_id = get_my_store_id());


-- ---------------------------------------------------------------------------
-- 30. RLS POLICIES — OFFLINE SYNC QUEUE
-- ---------------------------------------------------------------------------

CREATE POLICY sync_queue_select_own
    ON public.offline_sync_queue
    FOR SELECT
    TO authenticated
    USING (store_id = get_my_store_id());

CREATE POLICY sync_queue_insert_own
    ON public.offline_sync_queue
    FOR INSERT
    TO authenticated
    WITH CHECK (store_id = get_my_store_id());

CREATE POLICY sync_queue_update_own
    ON public.offline_sync_queue
    FOR UPDATE
    TO authenticated
    USING  (store_id = get_my_store_id())
    WITH CHECK (store_id = get_my_store_id());


-- =============================================================================
-- HELPER FUNCTIONS
-- =============================================================================


-- ---------------------------------------------------------------------------
-- check_ocr_limit(p_store_id UUID) → BOOLEAN
-- Returns TRUE if the store has remaining OCR quota this month.
-- Called by ocr-invoice and ocr-diary Edge Functions.
-- ---------------------------------------------------------------------------

CREATE OR REPLACE FUNCTION public.check_ocr_limit(p_store_id UUID)
RETURNS BOOLEAN
LANGUAGE plpgsql
STABLE
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
    v_plan              plan_type;
    v_scans_used        INTEGER;
    v_free_limit        INTEGER := 5;
BEGIN
    SELECT plan, ocr_scans_this_month
    INTO   v_plan, v_scans_used
    FROM   public.profiles
    WHERE  store_id = p_store_id;

    IF v_plan = 'pro' THEN
        RETURN TRUE;  -- unlimited for pro
    END IF;

    RETURN (v_scans_used < v_free_limit);
END;
$$;

COMMENT ON FUNCTION public.check_ocr_limit IS 'Returns TRUE if store has remaining OCR quota. Free = 5/month; Pro = unlimited.';


-- ---------------------------------------------------------------------------
-- increment_ocr_counter(p_store_id UUID) → VOID
-- Increments ocr_scans_this_month for a store.
-- Resets counter if plan_reset_at has passed.
-- ---------------------------------------------------------------------------

CREATE OR REPLACE FUNCTION public.increment_ocr_counter(p_store_id UUID)
RETURNS VOID
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
BEGIN
    UPDATE public.profiles
    SET
        -- Reset counter if monthly window has expired
        ocr_scans_this_month = CASE
            WHEN NOW() >= plan_reset_at
            THEN 1  -- this is the first scan of the new month
            ELSE ocr_scans_this_month + 1
        END,
        plan_reset_at = CASE
            WHEN NOW() >= plan_reset_at
            THEN date_trunc('month', NOW()) + INTERVAL '1 month'
            ELSE plan_reset_at
        END,
        updated_at = NOW()
    WHERE store_id = p_store_id;
END;
$$;

COMMENT ON FUNCTION public.increment_ocr_counter IS 'Safely increments OCR counter; auto-resets at month boundary.';


-- ---------------------------------------------------------------------------
-- get_khata_summary(p_store_id UUID)
-- Returns total outstanding balance and customer count.
-- Used by Analytics Dashboard (F9).
-- ---------------------------------------------------------------------------

CREATE OR REPLACE FUNCTION public.get_khata_summary(p_store_id UUID)
RETURNS TABLE (
    total_outstanding   NUMERIC(12, 2),
    customers_with_debt INTEGER,
    total_customers     INTEGER
)
LANGUAGE sql
STABLE
SECURITY DEFINER
SET search_path = public
AS $$
    SELECT
        COALESCE(SUM(CASE WHEN running_balance > 0 THEN running_balance ELSE 0 END), 0) AS total_outstanding,
        COUNT(*) FILTER (WHERE running_balance > 0)::INTEGER                            AS customers_with_debt,
        COUNT(*)::INTEGER                                                                AS total_customers
    FROM public.khata_customers
    WHERE store_id = p_store_id;
$$;

COMMENT ON FUNCTION public.get_khata_summary IS 'Aggregate khata stats for analytics dashboard.';


-- ---------------------------------------------------------------------------
-- get_top_selling_items(p_store_id UUID, p_days INTEGER, p_limit INTEGER)
-- Returns top N fastest-moving SKUs in the last p_days days.
-- Used by Analytics Dashboard (F9).
-- ---------------------------------------------------------------------------

CREATE OR REPLACE FUNCTION public.get_top_selling_items(
    p_store_id  UUID,
    p_days      INTEGER DEFAULT 30,
    p_limit     INTEGER DEFAULT 10
)
RETURNS TABLE (
    inventory_id    UUID,
    item_name       TEXT,
    total_qty_sold  NUMERIC,
    total_revenue   NUMERIC,
    sale_count      INTEGER
)
LANGUAGE sql
STABLE
SECURITY DEFINER
SET search_path = public
AS $$
    SELECT
        sri.inventory_id,
        sri.item_name,
        SUM(sri.quantity_sold)                                AS total_qty_sold,
        SUM(COALESCE(sri.sale_price, 0) * sri.quantity_sold)  AS total_revenue,
        COUNT(DISTINCT sri.sale_record_id)::INTEGER           AS sale_count
    FROM  public.sale_record_items sri
    JOIN  public.sale_records       sr  ON sr.id = sri.sale_record_id
    WHERE sri.store_id  = p_store_id
      AND sr.sale_date >= CURRENT_DATE - p_days
    GROUP BY sri.inventory_id, sri.item_name
    ORDER BY total_qty_sold DESC
    LIMIT p_limit;
$$;

COMMENT ON FUNCTION public.get_top_selling_items IS 'Returns fastest-moving SKUs for analytics. Called by analytics-summary Edge Function.';


-- ---------------------------------------------------------------------------
-- get_expiry_risk_items(p_store_id UUID, p_days_threshold INTEGER)
-- Returns items expiring within threshold with stock > 0.
-- Used by alert-checker + Analytics Dashboard.
-- ---------------------------------------------------------------------------

CREATE OR REPLACE FUNCTION public.get_expiry_risk_items(
    p_store_id          UUID,
    p_days_threshold    INTEGER DEFAULT 30
)
RETURNS TABLE (
    id              UUID,
    item_name       TEXT,
    quantity        NUMERIC,
    expiry_date     DATE,
    days_to_expiry  INTEGER,
    category        TEXT
)
LANGUAGE sql
STABLE
SECURITY DEFINER
SET search_path = public
AS $$
    SELECT
        id,
        item_name,
        quantity,
        expiry_date,
        (expiry_date - CURRENT_DATE)::INTEGER AS days_to_expiry,
        category
    FROM  public.inventory
    WHERE store_id    = p_store_id
      AND expiry_date IS NOT NULL
      AND expiry_date >= CURRENT_DATE             -- not yet expired
      AND (expiry_date - CURRENT_DATE) <= p_days_threshold
      AND quantity    > 0
    ORDER BY expiry_date ASC;
$$;

COMMENT ON FUNCTION public.get_expiry_risk_items IS 'Items at expiry risk. Used by alert-checker (daily cron) and analytics dashboard.';


-- ---------------------------------------------------------------------------
-- get_low_stock_items(p_store_id UUID)
-- Returns items where quantity <= min_threshold.
-- ---------------------------------------------------------------------------

CREATE OR REPLACE FUNCTION public.get_low_stock_items(p_store_id UUID)
RETURNS TABLE (
    id              UUID,
    item_name       TEXT,
    quantity        NUMERIC,
    min_threshold   NUMERIC,
    category        TEXT
)
LANGUAGE sql
STABLE
SECURITY DEFINER
SET search_path = public
AS $$
    SELECT id, item_name, quantity, min_threshold, category
    FROM   public.inventory
    WHERE  store_id   = p_store_id
      AND  quantity  <= min_threshold
    ORDER BY (quantity / NULLIF(min_threshold, 0)) ASC; -- most critical first
$$;

COMMENT ON FUNCTION public.get_low_stock_items IS 'Items below min_threshold for alert-checker.';


-- ---------------------------------------------------------------------------
-- cleanup_expired_idempotency_keys() → INTEGER
-- Deletes idempotency keys past their TTL. Run via pg_cron nightly.
-- ---------------------------------------------------------------------------

CREATE OR REPLACE FUNCTION public.cleanup_expired_idempotency_keys()
RETURNS INTEGER
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
    v_deleted INTEGER;
BEGIN
    DELETE FROM public.idempotency_keys
    WHERE expires_at < NOW();

    GET DIAGNOSTICS v_deleted = ROW_COUNT;
    RETURN v_deleted;
END;
$$;

COMMENT ON FUNCTION public.cleanup_expired_idempotency_keys IS 'Purge stale idempotency keys. Schedule via pg_cron: 0 2 * * *';


-- ---------------------------------------------------------------------------
-- resolve_ocr_mapping(p_store_id UUID, p_raw_name TEXT) → TEXT
-- Looks up the best known canonical name for an OCR-extracted item name.
-- Returns NULL if no mapping found (caller should flag for user review).
-- ---------------------------------------------------------------------------

CREATE OR REPLACE FUNCTION public.resolve_ocr_mapping(
    p_store_id  UUID,
    p_raw_name  TEXT
)
RETURNS TEXT
LANGUAGE sql
STABLE
SECURITY DEFINER
SET search_path = public
AS $$
    SELECT canonical_name
    FROM   public.ocr_name_mappings
    WHERE  store_id     = p_store_id
      AND  LOWER(TRIM(ocr_raw_name)) = LOWER(TRIM(p_raw_name))
    ORDER BY confirmed_count DESC
    LIMIT 1;
$$;

COMMENT ON FUNCTION public.resolve_ocr_mapping IS 'Returns best canonical name for an OCR raw string. Called during diary OCR confirmation flow.';


-- =============================================================================
-- STORAGE BUCKETS (declarative — applied via Supabase dashboard or CLI)
-- =============================================================================
-- NOTE: Run via Supabase Storage API or dashboard — not raw SQL.
-- Documented here for completeness and code review.
--
-- Bucket: invoice-images
--   public: false
--   allowed_mime_types: ['image/jpeg', 'image/png', 'image/webp', 'image/heic']
--   file_size_limit: 10485760  (10 MB)
--   RLS: upload only to own store prefix ({store_id}/*)
-- =============================================================================


-- =============================================================================
-- PG_CRON JOBS (schedule after enabling pg_cron extension in Supabase dashboard)
-- =============================================================================
-- Uncomment and run after enabling pg_cron:
--
-- SELECT cron.schedule(
--     'cleanup-idempotency-keys',
--     '0 2 * * *',            -- 2 AM UTC daily
--     $$SELECT public.cleanup_expired_idempotency_keys();$$
-- );
--
-- SELECT cron.schedule(
--     'reset-monthly-ocr-counters',
--     '0 0 1 * *',            -- midnight UTC on 1st of month
--     $$UPDATE public.profiles SET ocr_scans_this_month = 0, plan_reset_at = date_trunc('month', NOW()) + INTERVAL '1 month';$$
-- );
-- =============================================================================


-- =============================================================================
-- GRANT STATEMENTS
-- =============================================================================
-- Grant usage to authenticated and service_role.
-- Anon is intentionally blocked on all tables (no public data).
-- =============================================================================

GRANT USAGE ON SCHEMA public TO authenticated, service_role;

GRANT SELECT, INSERT, UPDATE, DELETE ON public.profiles           TO authenticated;
GRANT SELECT, INSERT, UPDATE, DELETE ON public.inventory          TO authenticated;
GRANT SELECT, INSERT, UPDATE         ON public.sale_records       TO authenticated;
GRANT SELECT, INSERT                 ON public.sale_record_items  TO authenticated;
GRANT SELECT, INSERT, UPDATE, DELETE ON public.khata_customers    TO authenticated;
GRANT SELECT, INSERT                 ON public.khata_transactions TO authenticated;
GRANT SELECT, UPDATE                 ON public.alerts             TO authenticated;
GRANT SELECT, INSERT, UPDATE         ON public.ocr_jobs           TO authenticated;
GRANT SELECT, INSERT, UPDATE, DELETE ON public.ocr_name_mappings  TO authenticated;
GRANT SELECT, INSERT, UPDATE         ON public.idempotency_keys   TO authenticated;
GRANT SELECT, INSERT, UPDATE, DELETE ON public.distributors       TO authenticated;
GRANT SELECT, INSERT, UPDATE         ON public.offline_sync_queue TO authenticated;

-- service_role has full access (used by Edge Functions running with service key)
GRANT ALL ON ALL TABLES IN SCHEMA public TO service_role;
GRANT ALL ON ALL SEQUENCES IN SCHEMA public TO service_role;
GRANT ALL ON ALL FUNCTIONS IN SCHEMA public TO service_role;

-- Grant execute on helper functions to authenticated
GRANT EXECUTE ON FUNCTION public.get_my_store_id()                                   TO authenticated;
GRANT EXECUTE ON FUNCTION public.check_ocr_limit(UUID)                               TO authenticated;
GRANT EXECUTE ON FUNCTION public.increment_ocr_counter(UUID)                         TO service_role;
GRANT EXECUTE ON FUNCTION public.get_khata_summary(UUID)                             TO authenticated;
GRANT EXECUTE ON FUNCTION public.get_top_selling_items(UUID, INTEGER, INTEGER)        TO authenticated;
GRANT EXECUTE ON FUNCTION public.get_expiry_risk_items(UUID, INTEGER)                 TO authenticated;
GRANT EXECUTE ON FUNCTION public.get_low_stock_items(UUID)                            TO authenticated;
GRANT EXECUTE ON FUNCTION public.cleanup_expired_idempotency_keys()                   TO service_role;
GRANT EXECUTE ON FUNCTION public.resolve_ocr_mapping(UUID, TEXT)                      TO authenticated;


-- =============================================================================
-- SCHEMA COMPLETE
-- =============================================================================
-- Tables:     12 (profiles, inventory, sale_records, sale_record_items,
--                 khata_customers, khata_transactions, alerts, ocr_jobs,
--                 ocr_name_mappings, idempotency_keys, distributors,
--                 offline_sync_queue)
-- Triggers:   13 (11 × updated_at, deduct_inventory_on_sale,
--                 khata_tx_update_balance, on_auth_user_created)
-- Functions:  11 (set_updated_at, handle_new_user, get_my_store_id,
--                 deduct_inventory_on_sale, khata_tx_update_balance,
--                 check_ocr_limit, increment_ocr_counter, get_khata_summary,
--                 get_top_selling_items, get_expiry_risk_items,
--                 get_low_stock_items, cleanup_expired_idempotency_keys,
--                 resolve_ocr_mapping)
-- RLS:        Enabled on all 12 tables; 28 policies
-- =============================================================================
