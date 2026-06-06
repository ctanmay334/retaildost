-- 20260529000005_soft_deletes_and_idempotency.sql
-- ---------------------------------------------------------------------------
-- Migration: Add Soft Delete, Idempotency, and AI Cost Protection
-- ---------------------------------------------------------------------------

-- 1. ADD SOFT DELETE (deleted_at) TO TABLES
ALTER TABLE public.inventory ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMPTZ DEFAULT NULL;
ALTER TABLE public.sale_records ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMPTZ DEFAULT NULL;
ALTER TABLE public.khata_customers ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMPTZ DEFAULT NULL;
ALTER TABLE public.khata_transactions ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMPTZ DEFAULT NULL;

-- 2. CREATE PARTIAL INDEXES TO IGNORE SOFT-DELETED RECORDS
CREATE INDEX IF NOT EXISTS idx_inventory_not_deleted ON public.inventory (store_id) WHERE deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_sales_not_deleted     ON public.sale_records (store_id, sale_date DESC) WHERE deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_khata_cust_not_deleted ON public.khata_customers (store_id) WHERE deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_khata_tx_not_deleted  ON public.khata_transactions (store_id, customer_id) WHERE deleted_at IS NULL;

-- 3. ENSURE IDEMPOTENCY KEY OR REQUEST_ID IS ENFORCED
ALTER TABLE public.sale_records ADD COLUMN IF NOT EXISTS request_id UUID DEFAULT NULL;
ALTER TABLE public.khata_transactions ADD COLUMN IF NOT EXISTS request_id UUID DEFAULT NULL;
ALTER TABLE public.inventory ADD COLUMN IF NOT EXISTS request_id UUID DEFAULT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uq_sales_request_id ON public.sale_records (request_id) WHERE request_id IS NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS uq_khata_tx_request_id ON public.khata_transactions (request_id) WHERE request_id IS NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS uq_inventory_request_id ON public.inventory (request_id) WHERE request_id IS NOT NULL;

-- 4. AI COST PROTECTION (OCR/NLP Daily Quotas & Payload Limits)
CREATE TABLE IF NOT EXISTS public.ai_usage_logs (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    store_id            UUID NOT NULL REFERENCES public.profiles(store_id) ON DELETE CASCADE,
    api_endpoint        TEXT NOT NULL,                 -- 'ocr-invoice' | 'ocr-diary' | 'khata-nlp' | 'analytics'
    tokens_used         INTEGER DEFAULT 0,
    payload_size_bytes  INTEGER NOT NULL,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Index for fast daily aggregation
CREATE INDEX IF NOT EXISTS idx_ai_usage_created_at ON public.ai_usage_logs (store_id, api_endpoint, created_at);

-- PostgreSQL Function to atomically check and increment the daily AI usage quotas
CREATE OR REPLACE FUNCTION public.check_and_increment_ai_quota(
    p_store_id            UUID,
    p_endpoint            TEXT,
    p_max_daily           INTEGER,
    p_payload_size_bytes  INTEGER,
    p_max_payload_size    INTEGER
)
RETURNS JSONB
LANGUAGE plpgsql
SECURITY DEFINER
AS $$
DECLARE
    v_daily_count INTEGER;
    v_result JSONB;
BEGIN
    -- 1. Validate payload size limit
    IF p_payload_size_bytes > p_max_payload_size THEN
        RETURN jsonb_build_object(
            'allowed', FALSE,
            'reason', 'PAYLOAD_TOO_LARGE',
            'message', 'Payload size (' || (p_payload_size_bytes / 1024) || ' KB) exceeds the maximum allowed limit of (' || (p_max_payload_size / 1024) || ' KB).'
        );
    END IF;

    -- 2. Count requests made by this store on this endpoint today (UTC time matching server clock)
    SELECT COALESCE(COUNT(*), 0)
    INTO v_daily_count
    FROM public.ai_usage_logs
    WHERE store_id = p_store_id
      AND api_endpoint = p_endpoint
      AND created_at >= date_trunc('day', NOW());

    -- 3. Check quota exhaustion
    IF v_daily_count >= p_max_daily THEN
        RETURN jsonb_build_object(
            'allowed', FALSE,
            'reason', 'QUOTA_EXHAUSTED',
            'message', 'Daily quota (' || p_max_daily || ' requests) for ' || p_endpoint || ' has been fully exhausted for today.'
        );
    END IF;

    -- 4. Allowed! Log usage record
    INSERT INTO public.ai_usage_logs (
        store_id,
        api_endpoint,
        payload_size_bytes
    ) VALUES (
        p_store_id,
        p_endpoint,
        p_payload_size_bytes
    );

    RETURN jsonb_build_object(
        'allowed', TRUE,
        'daily_usage', v_daily_count + 1,
        'remaining', p_max_daily - (v_daily_count + 1)
    );
END;
$$;

-- Grant permissions to executing accounts
GRANT EXECUTE ON FUNCTION public.check_and_increment_ai_quota(UUID, TEXT, INTEGER, INTEGER, INTEGER) TO authenticated, service_role;

-- Comments for documentation
COMMENT ON TABLE  public.ai_usage_logs IS 'Tracks and audits all store AI requests (Gemini API tokens/payloads) for pricing rate limiting.';
COMMENT ON COLUMN public.inventory.deleted_at IS 'Soft delete timestamp. If non-null, the item is removed from active catalog.';
COMMENT ON COLUMN public.sale_records.deleted_at IS 'Soft delete timestamp for sales session audit.';
COMMENT ON COLUMN public.khata_customers.deleted_at IS 'Soft delete timestamp for ledger customer retention.';
COMMENT ON COLUMN public.khata_transactions.deleted_at IS 'Soft delete timestamp for ledger transactions audit.';
