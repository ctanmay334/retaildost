-- 20260605000000_add_due_date.sql
-- ---------------------------------------------------------------------------
-- Migration: Add due_date to khata_transactions table for syncing
-- ---------------------------------------------------------------------------

ALTER TABLE public.khata_transactions 
ADD COLUMN IF NOT EXISTS due_date TIMESTAMPTZ DEFAULT NULL;

COMMENT ON COLUMN public.khata_transactions.due_date IS 'Payment due date for credit ledger entries.';
