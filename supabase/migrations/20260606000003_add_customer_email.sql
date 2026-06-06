-- 20260606000003_add_customer_email.sql
-- ---------------------------------------------------------------------------
-- Migration: Add email field to khata_customers table
-- ---------------------------------------------------------------------------

ALTER TABLE public.khata_customers ADD COLUMN IF NOT EXISTS email TEXT;
