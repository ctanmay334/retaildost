-- =============================================================================
-- RetailDost (KiranaOS) — Alerts Architecture Migration
-- Migration: 20260529000003_alerts_architecture.sql
-- =============================================================================

-- ---------------------------------------------------------------------------
-- 1. generate_daily_alerts()
-- Evaluates low-stock and expiry alerts across all store inventory.
-- Inserts fresh alerts while preventing duplicate spam (fatigue protection).
-- Returns generated alert details along with store owner's FCM token.
-- ---------------------------------------------------------------------------

CREATE OR REPLACE FUNCTION public.generate_daily_alerts()
RETURNS TABLE (
    alert_id        UUID,
    store_id        UUID,
    fcm_token       TEXT,
    alert_type      alert_type,
    item_name       TEXT,
    message         TEXT,
    inventory_id    UUID
)
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
    v_rec RECORD;
    v_inserted_id UUID;
BEGIN
    -- Temporary table to hold results during execution
    CREATE TEMP TABLE IF NOT EXISTS temp_generated_alerts (
        t_alert_id      UUID,
        t_store_id      UUID,
        t_fcm_token     TEXT,
        t_alert_type    alert_type,
        t_item_name     TEXT,
        t_message       TEXT,
        t_inventory_id  UUID
    ) ON COMMIT DROP;

    -- ==========================================
    -- PHASE 1: EVALUATE LOW STOCK ALERTS
    -- ==========================================
    FOR v_rec IN 
        SELECT 
            inv.id AS inv_id,
            inv.store_id AS st_id,
            inv.item_name AS name,
            inv.quantity AS qty,
            inv.unit_label AS unit,
            inv.min_threshold AS threshold,
            p.fcm_token AS token
        FROM public.inventory inv
        JOIN public.profiles p ON p.store_id = inv.store_id
        WHERE inv.quantity <= inv.min_threshold
    LOOP
        -- Check if an active (unread) low-stock alert or one generated in the last 3 days exists
        IF NOT EXISTS (
            SELECT 1 FROM public.alerts a
            WHERE a.inventory_id = v_rec.inv_id
              AND a.alert_type = 'low_stock'::alert_type
              AND (a.is_read = FALSE OR a.created_at > (NOW() - INTERVAL '3 days'))
        ) THEN
            INSERT INTO public.alerts (
                store_id,
                alert_type,
                inventory_id,
                item_name,
                message,
                current_qty,
                is_read
            ) VALUES (
                v_rec.st_id,
                'low_stock'::alert_type,
                v_rec.inv_id,
                v_rec.name,
                v_rec.name || ' is low in stock! Only ' || v_rec.qty || ' ' || COALESCE(v_rec.unit, 'pcs') || ' left.',
                v_rec.qty,
                FALSE
            ) RETURNING id INTO v_inserted_id;

            INSERT INTO temp_generated_alerts VALUES (
                v_inserted_id,
                v_rec.st_id,
                v_rec.token,
                'low_stock'::alert_type,
                v_rec.name,
                v_rec.name || ' is low in stock! Only ' || v_rec.qty || ' ' || COALESCE(v_rec.unit, 'pcs') || ' left.',
                v_rec.inv_id
            );
        END IF;
    END LOOP;

    -- ==========================================
    -- PHASE 2: EVALUATE EXPIRY ALERTS
    -- ==========================================
    FOR v_rec IN 
        SELECT 
            inv.id AS inv_id,
            inv.store_id AS st_id,
            inv.item_name AS name,
            inv.quantity AS qty,
            inv.expiry_date AS exp_date,
            (inv.expiry_date - CURRENT_DATE)::INTEGER AS days_left,
            p.fcm_token AS token
        FROM public.inventory inv
        JOIN public.profiles p ON p.store_id = inv.store_id
        WHERE inv.expiry_date IS NOT NULL
          AND inv.expiry_date >= CURRENT_DATE
          AND (inv.expiry_date - CURRENT_DATE) <= 30
          AND inv.quantity > 0
    LOOP
        DECLARE
            v_type alert_type;
            v_msg TEXT;
        BEGIN
            -- Classify urgency based on timeline
            IF v_rec.days_left <= 7 THEN
                v_type := 'expiry_critical'::alert_type;
                v_msg := v_rec.name || ' is expiring in ' || v_rec.days_left || ' days! Expiry: ' || to_char(v_rec.exp_date, 'YYYY-MM-DD') || '.';
            ELSE
                v_type := 'expiry_warning'::alert_type;
                v_msg := v_rec.name || ' is expiring in ' || v_rec.days_left || ' days. Expiry: ' || to_char(v_rec.exp_date, 'YYYY-MM-DD') || '.';
            END IF;

            -- Expiry alert fatigue protection: 7 days cooldown for warning, 3 days for critical
            IF NOT EXISTS (
                SELECT 1 FROM public.alerts a
                WHERE a.inventory_id = v_rec.inv_id
                  AND a.alert_type = v_type
                  AND (
                      a.is_read = FALSE OR 
                      (v_type = 'expiry_warning'::alert_type AND a.created_at > (NOW() - INTERVAL '7 days')) OR
                      (v_type = 'expiry_critical'::alert_type AND a.created_at > (NOW() - INTERVAL '3 days'))
                  )
            ) THEN
                INSERT INTO public.alerts (
                    store_id,
                    alert_type,
                    inventory_id,
                    item_name,
                    message,
                    days_to_expiry,
                    is_read
                ) VALUES (
                    v_rec.st_id,
                    v_type,
                    v_rec.inv_id,
                    v_rec.name,
                    v_msg,
                    v_rec.days_left,
                    FALSE
                ) RETURNING id INTO v_inserted_id;

                INSERT INTO temp_generated_alerts VALUES (
                    v_inserted_id,
                    v_rec.st_id,
                    v_rec.token,
                    v_type,
                    v_rec.name,
                    v_msg,
                    v_rec.inv_id
                );
            END IF;
        END;
    END LOOP;

    -- Return the contents of the temp table
    RETURN QUERY SELECT * FROM temp_generated_alerts;
END;
$$;

COMMENT ON FUNCTION public.generate_daily_alerts() IS 'Core alert generator running via alert-checker daily cron. Analyzes inventory stock & expiry and populates alerts.';

-- ---------------------------------------------------------------------------
-- 2. GRANTS
-- ---------------------------------------------------------------------------
GRANT EXECUTE ON FUNCTION public.generate_daily_alerts() TO service_role, authenticated;

-- ---------------------------------------------------------------------------
-- 3. RECOMMENDED PG_CRON SCHEDULING (DASHBOARD COMMAND REFERENCE)
-- ---------------------------------------------------------------------------
-- To schedule the edge function locally or on hosted Supabase, run the following SQL:
--
-- SELECT cron.schedule(
--     'daily-alert-checker-cron',
--     '0 8 * * *', -- Runs every day at 8:00 AM UTC (which corresponds to 1:30 PM IST)
--     $$
--     SELECT net.http_post(
--         url := 'https://your-project-ref.supabase.co/functions/v1/alert-checker',
--         headers := jsonb_build_object(
--             'Content-Type', 'application/json',
--             'Authorization', 'Bearer your-service-role-key-here'
--         ),
--         body := '{}'::jsonb
--     );
--     $$
-- );
