-- 20260606000002_allow_profile_insert_upsert.sql
-- ---------------------------------------------------------------------------
-- Migration: Allow authenticated users to insert/upsert their own profiles
-- ---------------------------------------------------------------------------

-- Create INSERT RLS policy on profiles
CREATE POLICY profiles_insert_own
    ON public.profiles
    FOR INSERT
    TO authenticated
    WITH CHECK (id = auth.uid());
