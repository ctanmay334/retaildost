-- =============================================================================
-- RetailDost — Distributor Marketplace Architecture Migration
-- Migration: 20260529000004_distributor_marketplace.sql
-- =============================================================================

-- ---------------------------------------------------------------------------
-- 1. Table Creation: distributors
-- ---------------------------------------------------------------------------
DROP TABLE IF EXISTS public.distributors CASCADE;

CREATE TABLE IF NOT EXISTS public.distributors (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name                TEXT NOT NULL,
    business_name       TEXT NOT NULL,
    category            TEXT NOT NULL, -- e.g., "Beverages", "Staples", "Dairy", "Snacks", "Cleaning", etc.
    phone               TEXT NOT NULL,
    whatsapp_no         TEXT NOT NULL,
    pincode             TEXT NOT NULL,
    service_regions     TEXT[] NOT NULL DEFAULT '{}', -- Array of pincodes or service areas
    address             TEXT,
    min_order_value     NUMERIC(10, 2) DEFAULT 0.00,
    is_verified         BOOLEAN NOT NULL DEFAULT FALSE,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Indexing for optimized searching and filtering
CREATE INDEX IF NOT EXISTS idx_distributors_pincode ON public.distributors(pincode);
CREATE INDEX IF NOT EXISTS idx_distributors_category ON public.distributors(category);
CREATE INDEX IF NOT EXISTS idx_distributors_business ON public.distributors(business_name);

-- ---------------------------------------------------------------------------
-- 2. RLS configuration
-- ---------------------------------------------------------------------------
ALTER TABLE public.distributors ENABLE ROW LEVEL SECURITY;

-- Policy: Allow everyone (including anonymous store owners/leads) to read distributors
CREATE POLICY "Allow public read access to distributors"
    ON public.distributors
    FOR SELECT
    USING (TRUE);

-- Policy: Allow authenticated users to register distributors
CREATE POLICY "Allow authenticated users to insert distributors"
    ON public.distributors
    FOR INSERT
    WITH CHECK (auth.role() = 'authenticated');

-- Policy: Allow service role complete management
CREATE POLICY "Allow service_role full control of distributors"
    ON public.distributors
    USING (TRUE)
    WITH CHECK (TRUE);

-- ---------------------------------------------------------------------------
-- 3. Trigger for updated_at tracking
-- ---------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION public.handle_distributor_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_update_distributors_timestamp
    BEFORE UPDATE ON public.distributors
    FOR EACH ROW
    EXECUTE FUNCTION public.handle_distributor_updated_at();

-- ---------------------------------------------------------------------------
-- 4. Initial Seed Data (Marketplace bootstrap)
-- ---------------------------------------------------------------------------
INSERT INTO public.distributors (name, business_name, category, phone, whatsapp_no, pincode, service_regions, address, min_order_value, is_verified)
VALUES
    ('Arjun Sharma', 'Sharma Staples & Grains Wholesalers', 'Staples', '+919876543210', '+919876543210', '400001', ARRAY['400001', '400002', '400003'], '12, Grain Market, CST Road, Mumbai', 5000.00, TRUE),
    ('Karan Mehta', 'Mehta Dairy & Beverages', 'Dairy', '+919123456789', '+919123456789', '400002', ARRAY['400001', '400002', '400005'], 'G-4, Shiv Shakti Industrial Estate, Kurla, Mumbai', 3000.00, TRUE),
    ('Priya Patel', 'Patel Packaged Snacks & Confectionery', 'Snacks', '+919988776655', '+919988776655', '400003', ARRAY['400002', '400003', '400004'], 'Plot 89, MIDC Industrial Area, Andheri, Mumbai', 2500.00, FALSE),
    ('Rajesh Gupta', 'Gupta Cleaning & Personal Care Agencies', 'Cleaning', '+918877665544', '+918877665544', '400001', ARRAY['400001', '400003', '400005'], 'Shop 3, Commercial Complex, Dadar West, Mumbai', 1500.00, TRUE)
ON CONFLICT DO NOTHING;
