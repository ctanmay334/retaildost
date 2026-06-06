-- 20260606000001_fix_khata_balance_trigger.sql
-- ---------------------------------------------------------------------------
-- Migration: Upgrade khata_tx_update_balance to be idempotent and support soft deletes
-- ---------------------------------------------------------------------------

CREATE OR REPLACE FUNCTION public.khata_tx_update_balance()
RETURNS TRIGGER
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
    v_customer_id UUID;
    v_balance NUMERIC(12, 2);
BEGIN
    IF TG_OP = 'DELETE' THEN
        v_customer_id := OLD.customer_id;
    ELSE
        v_customer_id := NEW.customer_id;
    END IF;

    -- Aggregate the running balance absolutely from all active (non-deleted) transactions
    SELECT COALESCE(SUM(
        CASE tx_type
            WHEN 'debit'    THEN amount
            WHEN 'credit'   THEN -amount
            WHEN 'reversal' THEN -amount
            ELSE 0
        END
    ), 0)
    INTO v_balance
    FROM public.khata_transactions
    WHERE customer_id = v_customer_id
      AND deleted_at IS NULL;

    -- Update the customer's running balance
    UPDATE public.khata_customers
    SET
        running_balance = v_balance,
        last_activity   = NOW(),
        updated_at      = NOW()
    WHERE
        id       = v_customer_id;

    IF TG_OP = 'DELETE' THEN
        RETURN OLD;
    ELSE
        RETURN NEW;
    END IF;
END;
$$;

-- Drop and re-create trigger to fire on INSERT, UPDATE, and DELETE
DROP TRIGGER IF EXISTS trg_khata_tx_update_balance ON public.khata_transactions;

CREATE TRIGGER trg_khata_tx_update_balance
    AFTER INSERT OR UPDATE OR DELETE ON public.khata_transactions
    FOR EACH ROW EXECUTE FUNCTION public.khata_tx_update_balance();
