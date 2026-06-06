-- 20260606000000_fix_store_id_key_alignment.sql
-- ---------------------------------------------------------------------------
-- Migration: Align incoming store_id with profiles(store_id) to prevent foreign key and RLS errors
-- ---------------------------------------------------------------------------

CREATE OR REPLACE FUNCTION public.align_store_id()
RETURNS TRIGGER
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
    v_correct_store_id UUID;
BEGIN
    -- Only align if auth.uid() is available (meaning request is from a client session)
    IF auth.uid() IS NOT NULL THEN
        SELECT store_id INTO v_correct_store_id
        FROM public.profiles
        WHERE id = auth.uid();

        -- Overwrite NEW.store_id with the correct tenant key
        IF v_correct_store_id IS NOT NULL THEN
            NEW.store_id := v_correct_store_id;
        END IF;
    END IF;

    RETURN NEW;
END;
$$;

COMMENT ON FUNCTION public.align_store_id() IS 'Automatically aligns incoming store_id to correct tenant profile store_id before insert or update.';

-- Attach triggers to all tenant tables
CREATE OR REPLACE TRIGGER trg_inventory_align_store_id
    BEFORE INSERT OR UPDATE ON public.inventory
    FOR EACH ROW EXECUTE FUNCTION public.align_store_id();

CREATE OR REPLACE TRIGGER trg_sale_records_align_store_id
    BEFORE INSERT OR UPDATE ON public.sale_records
    FOR EACH ROW EXECUTE FUNCTION public.align_store_id();

CREATE OR REPLACE TRIGGER trg_sale_record_items_align_store_id
    BEFORE INSERT OR UPDATE ON public.sale_record_items
    FOR EACH ROW EXECUTE FUNCTION public.align_store_id();

CREATE OR REPLACE TRIGGER trg_khata_customers_align_store_id
    BEFORE INSERT OR UPDATE ON public.khata_customers
    FOR EACH ROW EXECUTE FUNCTION public.align_store_id();

CREATE OR REPLACE TRIGGER trg_khata_transactions_align_store_id
    BEFORE INSERT OR UPDATE ON public.khata_transactions
    FOR EACH ROW EXECUTE FUNCTION public.align_store_id();

CREATE OR REPLACE TRIGGER trg_alerts_align_store_id
    BEFORE INSERT OR UPDATE ON public.alerts
    FOR EACH ROW EXECUTE FUNCTION public.align_store_id();

CREATE OR REPLACE TRIGGER trg_ocr_jobs_align_store_id
    BEFORE INSERT OR UPDATE ON public.ocr_jobs
    FOR EACH ROW EXECUTE FUNCTION public.align_store_id();

CREATE OR REPLACE TRIGGER trg_ocr_name_mappings_align_store_id
    BEFORE INSERT OR UPDATE ON public.ocr_name_mappings
    FOR EACH ROW EXECUTE FUNCTION public.align_store_id();

CREATE OR REPLACE TRIGGER trg_idempotency_keys_align_store_id
    BEFORE INSERT OR UPDATE ON public.idempotency_keys
    FOR EACH ROW EXECUTE FUNCTION public.align_store_id();

CREATE OR REPLACE TRIGGER trg_distributors_align_store_id
    BEFORE INSERT OR UPDATE ON public.distributors
    FOR EACH ROW EXECUTE FUNCTION public.align_store_id();

CREATE OR REPLACE TRIGGER trg_offline_sync_queue_align_store_id
    BEFORE INSERT OR UPDATE ON public.offline_sync_queue
    FOR EACH ROW EXECUTE FUNCTION public.align_store_id();

CREATE OR REPLACE TRIGGER trg_ai_usage_logs_align_store_id
    BEFORE INSERT OR UPDATE ON public.ai_usage_logs
    FOR EACH ROW EXECUTE FUNCTION public.align_store_id();
