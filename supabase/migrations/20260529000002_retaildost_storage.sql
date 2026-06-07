-- =============================================================================
-- RetailDost — Storage Buckets + Policies
-- Migration: 20260529000002_retaildost_storage.sql
-- =============================================================================
-- Automates PART 4 STORAGE SETUP — no manual dashboard clicks required.
--
-- Buckets:
--   retaildost-invoice-images  →  Private (store-namespaced, 10 MB max)
--   retaildost-diary-images    →  Private (store-namespaced, 10 MB max)
--   retaildost-profile-images  →  Public  (owner-namespaced, 2 MB max)
-- =============================================================================


-- ---------------------------------------------------------------------------
-- 1. CREATE BUCKETS
-- ---------------------------------------------------------------------------
-- Inserts into storage.buckets (Supabase internal table).
-- ON CONFLICT DO UPDATE → idempotent, safe to re-run.
-- ---------------------------------------------------------------------------

INSERT INTO storage.buckets (id, name, public, file_size_limit, allowed_mime_types)
VALUES
    (
        'retaildost-invoice-images',
        'retaildost-invoice-images',
        FALSE,                                              -- private
        10485760,                                           -- 10 MB
        ARRAY['image/jpeg','image/png','image/webp','image/heic','image/heif']
    ),
    (
        'retaildost-diary-images',
        'retaildost-diary-images',
        FALSE,                                              -- private
        10485760,                                           -- 10 MB
        ARRAY['image/jpeg','image/png','image/webp','image/heic','image/heif']
    ),
    (
        'retaildost-profile-images',
        'retaildost-profile-images',
        TRUE,                                               -- public read
        2097152,                                            -- 2 MB
        ARRAY['image/jpeg','image/png','image/webp']
    )
ON CONFLICT (id) DO UPDATE
    SET
        public             = EXCLUDED.public,
        file_size_limit    = EXCLUDED.file_size_limit,
        allowed_mime_types = EXCLUDED.allowed_mime_types;


-- ---------------------------------------------------------------------------
-- 2. NOTE: RLS on storage.objects is managed by Supabase internally.
-- We only need to create the per-bucket policies below.
-- ---------------------------------------------------------------------------


-- ---------------------------------------------------------------------------
-- 3. RLS POLICIES — retaildost-invoice-images (PRIVATE)
-- ---------------------------------------------------------------------------
-- Path convention: {store_id}/{timestamp}_{filename}
-- Only the owning store can upload, view, or delete.
-- ---------------------------------------------------------------------------

-- SELECT (download)
CREATE POLICY "invoice_images_select_own"
    ON storage.objects
    FOR SELECT
    TO authenticated
    USING (
        bucket_id = 'retaildost-invoice-images'
        AND (storage.foldername(name))[1] = (
            SELECT store_id::TEXT
            FROM   public.profiles
            WHERE  id = auth.uid()
            LIMIT  1
        )
    );

-- INSERT (upload)
CREATE POLICY "invoice_images_insert_own"
    ON storage.objects
    FOR INSERT
    TO authenticated
    WITH CHECK (
        bucket_id = 'retaildost-invoice-images'
        AND (storage.foldername(name))[1] = (
            SELECT store_id::TEXT
            FROM   public.profiles
            WHERE  id = auth.uid()
            LIMIT  1
        )
    );

-- DELETE (cleanup)
CREATE POLICY "invoice_images_delete_own"
    ON storage.objects
    FOR DELETE
    TO authenticated
    USING (
        bucket_id = 'retaildost-invoice-images'
        AND (storage.foldername(name))[1] = (
            SELECT store_id::TEXT
            FROM   public.profiles
            WHERE  id = auth.uid()
            LIMIT  1
        )
    );


-- ---------------------------------------------------------------------------
-- 4. RLS POLICIES — retaildost-diary-images (PRIVATE)
-- ---------------------------------------------------------------------------
-- Same path convention as invoice-images.
-- ---------------------------------------------------------------------------

CREATE POLICY "diary_images_select_own"
    ON storage.objects
    FOR SELECT
    TO authenticated
    USING (
        bucket_id = 'retaildost-diary-images'
        AND (storage.foldername(name))[1] = (
            SELECT store_id::TEXT
            FROM   public.profiles
            WHERE  id = auth.uid()
            LIMIT  1
        )
    );

CREATE POLICY "diary_images_insert_own"
    ON storage.objects
    FOR INSERT
    TO authenticated
    WITH CHECK (
        bucket_id = 'retaildost-diary-images'
        AND (storage.foldername(name))[1] = (
            SELECT store_id::TEXT
            FROM   public.profiles
            WHERE  id = auth.uid()
            LIMIT  1
        )
    );

CREATE POLICY "diary_images_delete_own"
    ON storage.objects
    FOR DELETE
    TO authenticated
    USING (
        bucket_id = 'retaildost-diary-images'
        AND (storage.foldername(name))[1] = (
            SELECT store_id::TEXT
            FROM   public.profiles
            WHERE  id = auth.uid()
            LIMIT  1
        )
    );


-- ---------------------------------------------------------------------------
-- 5. RLS POLICIES — retaildost-profile-images (PUBLIC READ)
-- ---------------------------------------------------------------------------
-- Path convention: {user_id}/{filename}
-- Public SELECT (no auth needed to view avatars).
-- INSERT / UPDATE / DELETE restricted to the owning user.
-- ---------------------------------------------------------------------------

-- Public read — no auth required (bucket is public)
CREATE POLICY "profile_images_select_public"
    ON storage.objects
    FOR SELECT
    TO public
    USING (bucket_id = 'retaildost-profile-images');

-- Upload: only to your own user_id prefix
CREATE POLICY "profile_images_insert_own"
    ON storage.objects
    FOR INSERT
    TO authenticated
    WITH CHECK (
        bucket_id = 'retaildost-profile-images'
        AND (storage.foldername(name))[1] = auth.uid()::TEXT
    );

-- Update (replace): only own files
CREATE POLICY "profile_images_update_own"
    ON storage.objects
    FOR UPDATE
    TO authenticated
    USING (
        bucket_id = 'retaildost-profile-images'
        AND (storage.foldername(name))[1] = auth.uid()::TEXT
    );

-- Delete: only own files
CREATE POLICY "profile_images_delete_own"
    ON storage.objects
    FOR DELETE
    TO authenticated
    USING (
        bucket_id = 'retaildost-profile-images'
        AND (storage.foldername(name))[1] = auth.uid()::TEXT
    );


-- =============================================================================
-- STORAGE SETUP COMPLETE
-- =============================================================================
-- Bucket                      │ Access  │ Max Size │ Policies
-- ────────────────────────────┼─────────┼──────────┼──────────────
-- retaildost-invoice-images   │ Private │ 10 MB    │ SELECT, INSERT, DELETE (own store)
-- retaildost-diary-images     │ Private │ 10 MB    │ SELECT, INSERT, DELETE (own store)
-- retaildost-profile-images   │ Public  │  2 MB    │ SELECT (public), INSERT/UPDATE/DELETE (own user)
-- =============================================================================
