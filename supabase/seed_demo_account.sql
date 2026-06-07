-- =============================================================================
-- RetailDost — Judge Demo Account Seeding Script
-- File: supabase/seed_demo_account.sql
-- Description: Run this script in the Supabase SQL Editor to populate
--              realistic, rich, and high-fidelity demo data for a single account.
-- Instructions:
--   1. Replace the 'judge@retaildost.com' email at the top of the block with 
--      the actual email of the registered user you wish to seed.
--   2. Click "Run" in your Supabase SQL Editor.
-- =============================================================================

DO $$
DECLARE
    -- CHANGE THIS EMAIL TO THE JUDGE'S SIGNED-UP EMAIL
    v_user_email TEXT := 'judge@retaildost.com';
    
    -- Variables to hold user and store identifiers
    v_user_id UUID;
    v_store_id UUID;
    v_store_pincode TEXT;
    
    -- UUID variables for inventory items
    v_inv_salt UUID := gen_random_uuid();
    v_inv_butter UUID := gen_random_uuid();
    v_inv_atta UUID := gen_random_uuid();
    v_inv_maggi UUID := gen_random_uuid();
    v_inv_parle UUID := gen_random_uuid();
    v_inv_oil UUID := gen_random_uuid();
    v_inv_handwash UUID := gen_random_uuid();
    v_inv_surf UUID := gen_random_uuid();
    v_inv_marie UUID := gen_random_uuid();
    v_inv_dairy_milk UUID := gen_random_uuid();
    v_inv_tea UUID := gen_random_uuid();
    v_inv_thumsup UUID := gen_random_uuid();
    
    -- UUID variables for khata customers (lenders and givers)
    v_cust_ramesh UUID := gen_random_uuid();
    v_cust_sunita UUID := gen_random_uuid();
    v_cust_amit UUID := gen_random_uuid();
    v_cust_rajesh UUID := gen_random_uuid();
    v_cust_preeti UUID := gen_random_uuid();
    v_cust_vikram UUID := gen_random_uuid();
    v_cust_suresh UUID := gen_random_uuid();
    v_cust_deepa UUID := gen_random_uuid();

    -- UUID variables for sales records (last 4-5 days)
    v_sale_1 UUID := gen_random_uuid();
    v_sale_2 UUID := gen_random_uuid();
    v_sale_3 UUID := gen_random_uuid();
    v_sale_4 UUID := gen_random_uuid();
    v_sale_5 UUID := gen_random_uuid();
    v_sale_6 UUID := gen_random_uuid();
    v_sale_7 UUID := gen_random_uuid();
    v_sale_8 UUID := gen_random_uuid();
    v_sale_9 UUID := gen_random_uuid();
    v_sale_10 UUID := gen_random_uuid();
    v_sale_11 UUID := gen_random_uuid();
    v_sale_12 UUID := gen_random_uuid();
    v_sale_13 UUID := gen_random_uuid();
    v_sale_14 UUID := gen_random_uuid();
    v_sale_15 UUID := gen_random_uuid();
    v_sale_16 UUID := gen_random_uuid();
    v_sale_17 UUID := gen_random_uuid();
BEGIN
    -- 1. Locate the auth user and their corresponding store profile
    SELECT u.id, p.store_id, COALESCE(p.pincode, '400001')
    INTO v_user_id, v_store_id, v_store_pincode
    FROM auth.users u
    JOIN public.profiles p ON p.id = u.id
    WHERE u.email = v_user_email
    LIMIT 1;
    
    -- Validate that the user actually exists
    IF v_store_id IS NULL THEN
        RAISE EXCEPTION 'User with email % not found in auth.users or profiles. Please create/register the user account in the app or via the Supabase Auth Dashboard first.', v_user_email;
    END IF;

    RAISE NOTICE 'Seeding store_id % for user % (%)...', v_store_id, v_user_id, v_user_email;

    -- 2. Onboard/Update the profile metadata to look like a real store owner
    UPDATE public.profiles
    SET owner_name = 'Rajesh Kumar',
        store_name = 'Apna Kirana Store',
        phone = '+919876543210',
        pincode = COALESCE(pincode, '400001'),
        city = COALESCE(city, 'Mumbai'),
        state = COALESCE(state, 'Maharashtra'),
        onboarded_at = COALESCE(onboarded_at, NOW() - INTERVAL '5 days'),
        updated_at = NOW()
    WHERE store_id = v_store_id;

    -- 3. Clear existing data to allow safe, clean re-runs of this script
    DELETE FROM public.alerts WHERE store_id = v_store_id;
    DELETE FROM public.sale_record_items WHERE store_id = v_store_id;
    DELETE FROM public.sale_records WHERE store_id = v_store_id;
    DELETE FROM public.khata_transactions WHERE store_id = v_store_id;
    DELETE FROM public.khata_customers WHERE store_id = v_store_id;
    DELETE FROM public.inventory WHERE store_id = v_store_id;

    -- 4. Seed Inventory (exactly 12 products with varying stock statuses)
    -- Stock types: 2 Out of stock, 4 Low stock, 6 Normal stock
    INSERT INTO public.inventory (id, store_id, item_name, category, unit_label, quantity, min_threshold, cost_price, mrp, batch_no, expiry_date, source, created_at)
    VALUES
      -- Normal Stock Items (6 items)
      (v_inv_salt, v_store_id, 'Tata Salt 1kg', 'Groceries', 'kg', 45.000, 10.000, 22.00, 28.00, 'TS-99', CURRENT_DATE + INTERVAL '365 days', 'manual', NOW() - INTERVAL '5 days'),
      (v_inv_maggi, v_store_id, 'Maggi Instant Noodles 70g', 'Snacks', 'pcs', 120.000, 50.000, 11.50, 14.00, 'MAG-77', CURRENT_DATE + INTERVAL '270 days', 'manual', NOW() - INTERVAL '5 days'),
      (v_inv_parle, v_store_id, 'Parle-G Biscuits 100g', 'Snacks', 'pcs', 180.000, 100.000, 4.20, 5.00, 'PG-23', CURRENT_DATE + INTERVAL '240 days', 'manual', NOW() - INTERVAL '5 days'),
      (v_inv_dairy_milk, v_store_id, 'Cadbury Dairy Milk 13.2g', 'Snacks', 'pcs', 75.000, 30.000, 8.50, 10.00, 'DM-30', CURRENT_DATE + INTERVAL '150 days', 'manual', NOW() - INTERVAL '5 days'),
      (v_inv_handwash, v_store_id, 'Dettol Liquid Handwash 200ml', 'Household', 'bottle', 22.000, 15.000, 78.00, 99.00, 'DET-14', CURRENT_DATE + INTERVAL '540 days', 'manual', NOW() - INTERVAL '5 days'),
      (v_inv_thumsup, v_store_id, 'Thums Up 250ml', 'Beverages', 'bottle', 60.000, 24.000, 15.00, 20.00, 'TU-11', CURRENT_DATE + INTERVAL '180 days', 'manual', NOW() - INTERVAL '5 days'),
      
      -- Low Stock Items (4 items)
      (v_inv_atta, v_store_id, 'Aashirvaad Shudh Chakki Atta 10kg', 'Groceries', 'pack', 2.000, 5.000, 390.00, 460.00, 'ASH-12', CURRENT_DATE + INTERVAL '180 days', 'manual', NOW() - INTERVAL '5 days'),
      (v_inv_surf, v_store_id, 'Surf Excel Easy Wash 1kg', 'Household', 'pack', 3.000, 8.000, 125.00, 150.00, 'SE-09', CURRENT_DATE + INTERVAL '720 days', 'manual', NOW() - INTERVAL '5 days'),
      (v_inv_oil, v_store_id, 'Fortune Mustard Oil 1L', 'Groceries', 'bottle', 4.000, 12.000, 145.00, 175.00, 'FOR-88', CURRENT_DATE + INTERVAL '300 days', 'manual', NOW() - INTERVAL '5 days'),
      (v_inv_tea, v_store_id, 'Taj Mahal Tea 250g', 'Groceries', 'pack', 3.000, 10.000, 165.00, 195.00, 'TM-67', CURRENT_DATE + INTERVAL '365 days', 'manual', NOW() - INTERVAL '5 days'),
      
      -- Out of Stock Items (2 items)
      (v_inv_butter, v_store_id, 'Amul Butter 500g', 'Dairy', 'pack', 0.000, 10.000, 235.00, 275.00, 'AMU-45', CURRENT_DATE - INTERVAL '2 days', 'manual', NOW() - INTERVAL '5 days'),
      (v_inv_marie, v_store_id, 'Britannia Marie Gold 250g', 'Snacks', 'pack', 0.000, 15.000, 24.00, 30.00, 'BM-51', CURRENT_DATE + INTERVAL '120 days', 'manual', NOW() - INTERVAL '5 days');

    -- 5. Seed Khata Customers (exactly 8 customers)
    -- Note: running_balance starts at 0, updated by the transaction trigger automatically!
    INSERT INTO public.khata_customers (id, store_id, name, phone, email, notes, last_activity, created_at)
    VALUES
      (v_cust_ramesh, v_store_id, 'Ramesh Kumar', '+919876543210', 'ramesh@gmail.com', 'Regular customer, buys milk and bread daily.', NOW() - INTERVAL '1 day', NOW() - INTERVAL '5 days'),
      (v_cust_sunita, v_store_id, 'Sunita Sharma', '+919812345678', 'sunita@gmail.com', 'Neighbor, pays monthly on 10th.', NOW() - INTERVAL '2 days', NOW() - INTERVAL '5 days'),
      (v_cust_amit, v_store_id, 'Amit Patel', '+919012345678', 'amit@gmail.com', 'Buys tea packets and biscuits for office pantry.', NOW() - INTERVAL '3 days', NOW() - INTERVAL '5 days'),
      (v_cust_rajesh, v_store_id, 'Rajesh Verma', '+919123456789', 'rajesh@gmail.com', 'Heavy buyer of monthly groceries.', NOW() - INTERVAL '4 hours', NOW() - INTERVAL '5 days'),
      (v_cust_preeti, v_store_id, 'Preeti Singh', '+919234567890', 'preeti@gmail.com', 'Pays immediately, clear ledger history.', NOW() - INTERVAL '1 day', NOW() - INTERVAL '5 days'),
      (v_cust_vikram, v_store_id, 'Vikram Malhotra', '+919345678901', 'vikram@gmail.com', 'Small transactions, college student.', NOW() - INTERVAL '12 hours', NOW() - INTERVAL '5 days'),
      (v_cust_suresh, v_store_id, 'Suresh Gupta', '+919456789012', 'suresh@gmail.com', 'Distributor staff, keeps advance credit.', NOW() - INTERVAL '3 days', NOW() - INTERVAL '5 days'),
      (v_cust_deepa, v_store_id, 'Deepa Nair', '+919567890123', 'deepa@gmail.com', 'Buys organic items and handwash.', NOW() - INTERVAL '2 days', NOW() - INTERVAL '5 days');

    -- 6. Seed Khata Transactions (triggers automatic running_balance updates in background)
    INSERT INTO public.khata_transactions (id, store_id, customer_id, tx_type, amount, notes, created_at, due_date)
    VALUES
      -- Ramesh Kumar (Net owed: +450.00)
      (gen_random_uuid(), v_store_id, v_cust_ramesh, 'debit', 600.00, 'Monthly ration partial udhar', NOW() - INTERVAL '4 days', NOW() + INTERVAL '10 days'),
      (gen_random_uuid(), v_store_id, v_cust_ramesh, 'credit', 150.00, 'Paid cash', NOW() - INTERVAL '1 day', NULL),
      
      -- Sunita Sharma (Net owed: +1200.00)
      (gen_random_uuid(), v_store_id, v_cust_sunita, 'debit', 1500.00, 'Atta, Oil and Spices', NOW() - INTERVAL '4 days', NOW() + INTERVAL '15 days'),
      (gen_random_uuid(), v_store_id, v_cust_sunita, 'credit', 300.00, 'Paid via GPay', NOW() - INTERVAL '2 days', NULL),
      
      -- Amit Patel (Net owed: +350.00)
      (gen_random_uuid(), v_store_id, v_cust_amit, 'debit', 350.00, 'Office tea bags and biscuits', NOW() - INTERVAL '3 days', NOW() + INTERVAL '7 days'),
      
      -- Rajesh Verma (Net owed: +2150.00)
      (gen_random_uuid(), v_store_id, v_cust_rajesh, 'debit', 3000.00, 'Full monthly groceries', NOW() - INTERVAL '4 days', NOW() + INTERVAL '20 days'),
      (gen_random_uuid(), v_store_id, v_cust_rajesh, 'credit', 1000.00, 'Bank transfer partial payment', NOW() - INTERVAL '1 day', NULL),
      (gen_random_uuid(), v_store_id, v_cust_rajesh, 'debit', 150.00, 'Surf Excel 1kg', NOW() - INTERVAL '4 hours', NOW() + INTERVAL '14 days'),
      
      -- Preeti Singh (Net owed: 0.00)
      (gen_random_uuid(), v_store_id, v_cust_preeti, 'debit', 800.00, 'Dettol handwash and snacks', NOW() - INTERVAL '4 days', NOW() + INTERVAL '5 days'),
      (gen_random_uuid(), v_store_id, v_cust_preeti, 'credit', 800.00, 'Cleared full balance via cash', NOW() - INTERVAL '1 day', NULL),
      
      -- Vikram Malhotra (Net owed: +80.00)
      (gen_random_uuid(), v_store_id, v_cust_vikram, 'debit', 180.00, 'Cigarettes and cold drinks', NOW() - INTERVAL '12 hours', NOW() + INTERVAL '3 days'),
      (gen_random_uuid(), v_store_id, v_cust_vikram, 'credit', 100.00, 'Paid back', NOW() - INTERVAL '2 hours', NULL),
      
      -- Suresh Gupta (Net owed: -500.00 - representing Advance/Giver)
      (gen_random_uuid(), v_store_id, v_cust_suresh, 'credit', 500.00, 'Advance deposit for next delivery', NOW() - INTERVAL '3 days', NULL),
      
      -- Deepa Nair (Net owed: +950.00)
      (gen_random_uuid(), v_store_id, v_cust_deepa, 'debit', 950.00, 'Fortune oil and cosmetics', NOW() - INTERVAL '3 days', NOW() + INTERVAL '12 days');

    -- 7. Seed Sales History (17 Sessions, 32 Line Items)
    -- We temporarily disable the inventory deduction trigger so that our inventory counts remain exactly
    -- what we set them to (since sales are historical and already factored into current stock counts).
    ALTER TABLE public.sale_record_items DISABLE TRIGGER trg_deduct_inventory_on_sale;

    -- Day 1 (June 3, 2026 / 4 Days Ago) - 3 sales
    INSERT INTO public.sale_records (id, store_id, customer_name, source, total_amount, items_count, sale_date, created_at)
    VALUES (v_sale_1, v_store_id, 'Walk-in Customer', 'manual', 516.00, 2, CURRENT_DATE - 4, NOW() - INTERVAL '4 days');
    INSERT INTO public.sale_record_items (id, store_id, sale_record_id, inventory_id, item_name, unit_label, quantity_sold, sale_price, cost_price, created_at) VALUES
      (gen_random_uuid(), v_store_id, v_sale_1, v_inv_salt, 'Tata Salt 1kg', 'kg', 2.000, 28.00, 22.00, NOW() - INTERVAL '4 days'),
      (gen_random_uuid(), v_store_id, v_sale_1, v_inv_atta, 'Aashirvaad Shudh Chakki Atta 10kg', 'pack', 1.000, 460.00, 390.00, NOW() - INTERVAL '4 days');

    INSERT INTO public.sale_records (id, store_id, customer_name, source, total_amount, items_count, sale_date, created_at)
    VALUES (v_sale_2, v_store_id, 'Sunny', 'manual', 120.00, 2, CURRENT_DATE - 4, NOW() - INTERVAL '4 days' + INTERVAL '2 hours');
    INSERT INTO public.sale_record_items (id, store_id, sale_record_id, inventory_id, item_name, unit_label, quantity_sold, sale_price, cost_price, created_at) VALUES
      (gen_random_uuid(), v_store_id, v_sale_2, v_inv_maggi, 'Maggi Instant Noodles 70g', 'pcs', 5.000, 14.00, 11.50, NOW() - INTERVAL '4 days' + INTERVAL '2 hours'),
      (gen_random_uuid(), v_store_id, v_sale_2, v_inv_parle, 'Parle-G Biscuits 100g', 'pcs', 10.000, 5.00, 4.20, NOW() - INTERVAL '4 days' + INTERVAL '2 hours');

    INSERT INTO public.sale_records (id, store_id, customer_name, source, total_amount, items_count, sale_date, created_at)
    VALUES (v_sale_3, v_store_id, 'Walk-in Customer', 'manual', 60.00, 2, CURRENT_DATE - 4, NOW() - INTERVAL '4 days' + INTERVAL '5 hours');
    INSERT INTO public.sale_record_items (id, store_id, sale_record_id, inventory_id, item_name, unit_label, quantity_sold, sale_price, cost_price, created_at) VALUES
      (gen_random_uuid(), v_store_id, v_sale_3, v_inv_dairy_milk, 'Cadbury Dairy Milk 13.2g', 'pcs', 2.000, 10.00, 8.50, NOW() - INTERVAL '4 days' + INTERVAL '5 hours'),
      (gen_random_uuid(), v_store_id, v_sale_3, v_inv_thumsup, 'Thums Up 250ml', 'bottle', 2.000, 20.00, 15.00, NOW() - INTERVAL '4 days' + INTERVAL '5 hours');

    -- Day 2 (June 4, 2026 / 3 Days Ago) - 3 sales
    INSERT INTO public.sale_records (id, store_id, customer_name, source, total_amount, items_count, sale_date, created_at)
    VALUES (v_sale_4, v_store_id, 'Walk-in Customer', 'manual', 378.00, 2, CURRENT_DATE - 3, NOW() - INTERVAL '3 days');
    INSERT INTO public.sale_record_items (id, store_id, sale_record_id, inventory_id, item_name, unit_label, quantity_sold, sale_price, cost_price, created_at) VALUES
      (gen_random_uuid(), v_store_id, v_sale_4, v_inv_oil, 'Fortune Mustard Oil 1L', 'bottle', 2.000, 175.00, 145.00, NOW() - INTERVAL '3 days'),
      (gen_random_uuid(), v_store_id, v_sale_4, v_inv_salt, 'Tata Salt 1kg', 'kg', 1.000, 28.00, 22.00, NOW() - INTERVAL '3 days');

    INSERT INTO public.sale_records (id, store_id, customer_name, source, total_amount, items_count, sale_date, created_at)
    VALUES (v_sale_5, v_store_id, 'Mrs. Verma', 'manual', 249.00, 2, CURRENT_DATE - 3, NOW() - INTERVAL '3 days' + INTERVAL '3 hours');
    INSERT INTO public.sale_record_items (id, store_id, sale_record_id, inventory_id, item_name, unit_label, quantity_sold, sale_price, cost_price, created_at) VALUES
      (gen_random_uuid(), v_store_id, v_sale_5, v_inv_handwash, 'Dettol Liquid Handwash 200ml', 'bottle', 1.000, 99.00, 78.00, NOW() - INTERVAL '3 days' + INTERVAL '3 hours'),
      (gen_random_uuid(), v_store_id, v_sale_5, v_inv_surf, 'Surf Excel Easy Wash 1kg', 'pack', 1.000, 150.00, 125.00, NOW() - INTERVAL '3 days' + INTERVAL '3 hours');

    INSERT INTO public.sale_records (id, store_id, customer_name, source, total_amount, items_count, sale_date, created_at)
    VALUES (v_sale_6, v_store_id, 'Walk-in Customer', 'manual', 255.00, 2, CURRENT_DATE - 3, NOW() - INTERVAL '3 days' + INTERVAL '6 hours');
    INSERT INTO public.sale_record_items (id, store_id, sale_record_id, inventory_id, item_name, unit_label, quantity_sold, sale_price, cost_price, created_at) VALUES
      (gen_random_uuid(), v_store_id, v_sale_6, v_inv_marie, 'Britannia Marie Gold 250g', 'pack', 2.000, 30.00, 24.00, NOW() - INTERVAL '3 days' + INTERVAL '6 hours'),
      (gen_random_uuid(), v_store_id, v_sale_6, v_inv_tea, 'Taj Mahal Tea 250g', 'pack', 1.000, 195.00, 165.00, NOW() - INTERVAL '3 days' + INTERVAL '6 hours');

    -- Day 3 (June 5, 2026 / 2 Days Ago) - 4 sales
    INSERT INTO public.sale_records (id, store_id, customer_name, source, total_amount, items_count, sale_date, created_at)
    VALUES (v_sale_7, v_store_id, 'Rohan (Scanned Diary)', 'ocr_diary', 443.00, 2, CURRENT_DATE - 2, NOW() - INTERVAL '2 days');
    INSERT INTO public.sale_record_items (id, store_id, sale_record_id, inventory_id, item_name, unit_label, quantity_sold, sale_price, cost_price, created_at) VALUES
      (gen_random_uuid(), v_store_id, v_sale_7, v_inv_butter, 'Amul Butter 500g', 'pack', 1.000, 275.00, 235.00, NOW() - INTERVAL '2 days'),
      (gen_random_uuid(), v_store_id, v_sale_7, v_inv_maggi, 'Maggi Instant Noodles 70g', 'pcs', 12.000, 14.00, 11.50, NOW() - INTERVAL '2 days');

    INSERT INTO public.sale_records (id, store_id, customer_name, source, total_amount, items_count, sale_date, created_at)
    VALUES (v_sale_8, v_store_id, 'Walk-in Customer', 'manual', 635.00, 2, CURRENT_DATE - 2, NOW() - INTERVAL '2 days' + INTERVAL '2 hours');
    INSERT INTO public.sale_record_items (id, store_id, sale_record_id, inventory_id, item_name, unit_label, quantity_sold, sale_price, cost_price, created_at) VALUES
      (gen_random_uuid(), v_store_id, v_sale_8, v_inv_oil, 'Fortune Mustard Oil 1L', 'bottle', 1.000, 175.00, 145.00, NOW() - INTERVAL '2 days' + INTERVAL '2 hours'),
      (gen_random_uuid(), v_store_id, v_sale_8, v_inv_atta, 'Aashirvaad Shudh Chakki Atta 10kg', 'pack', 1.000, 460.00, 390.00, NOW() - INTERVAL '2 days' + INTERVAL '2 hours');

    INSERT INTO public.sale_records (id, store_id, customer_name, source, total_amount, items_count, sale_date, created_at)
    VALUES (v_sale_9, v_store_id, 'Party Order', 'manual', 220.00, 2, CURRENT_DATE - 2, NOW() - INTERVAL '2 days' + INTERVAL '4 hours');
    INSERT INTO public.sale_record_items (id, store_id, sale_record_id, inventory_id, item_name, unit_label, quantity_sold, sale_price, cost_price, created_at) VALUES
      (gen_random_uuid(), v_store_id, v_sale_9, v_inv_thumsup, 'Thums Up 250ml', 'bottle', 6.000, 20.00, 15.00, NOW() - INTERVAL '2 days' + INTERVAL '4 hours'),
      (gen_random_uuid(), v_store_id, v_sale_9, v_inv_parle, 'Parle-G Biscuits 100g', 'pcs', 20.000, 5.00, 4.20, NOW() - INTERVAL '2 days' + INTERVAL '4 hours');

    INSERT INTO public.sale_records (id, store_id, customer_name, source, total_amount, items_count, sale_date, created_at)
    VALUES (v_sale_10, v_store_id, 'Walk-in Customer', 'manual', 80.00, 2, CURRENT_DATE - 2, NOW() - INTERVAL '2 days' + INTERVAL '6 hours');
    INSERT INTO public.sale_record_items (id, store_id, sale_record_id, inventory_id, item_name, unit_label, quantity_sold, sale_price, cost_price, created_at) VALUES
      (gen_random_uuid(), v_store_id, v_sale_10, v_inv_dairy_milk, 'Cadbury Dairy Milk 13.2g', 'pcs', 5.000, 10.00, 8.50, NOW() - INTERVAL '2 days' + INTERVAL '6 hours'),
      (gen_random_uuid(), v_store_id, v_sale_10, v_inv_marie, 'Britannia Marie Gold 250g', 'pack', 1.000, 30.00, 24.00, NOW() - INTERVAL '2 days' + INTERVAL '6 hours');

    -- Day 4 (June 6, 2026 / 1 Day Ago) - 4 sales
    INSERT INTO public.sale_records (id, store_id, customer_name, source, total_amount, items_count, sale_date, created_at)
    VALUES (v_sale_11, v_store_id, 'Anil', 'manual', 498.00, 2, CURRENT_DATE - 1, NOW() - INTERVAL '1 day');
    INSERT INTO public.sale_record_items (id, store_id, sale_record_id, inventory_id, item_name, unit_label, quantity_sold, sale_price, cost_price, created_at) VALUES
      (gen_random_uuid(), v_store_id, v_sale_11, v_inv_surf, 'Surf Excel Easy Wash 1kg', 'pack', 2.000, 150.00, 125.00, NOW() - INTERVAL '1 day'),
      (gen_random_uuid(), v_store_id, v_sale_11, v_inv_handwash, 'Dettol Liquid Handwash 200ml', 'bottle', 2.000, 99.00, 78.00, NOW() - INTERVAL '1 day');

    INSERT INTO public.sale_records (id, store_id, customer_name, source, total_amount, items_count, sale_date, created_at)
    VALUES (v_sale_12, v_store_id, 'Walk-in Customer', 'manual', 498.00, 3, CURRENT_DATE - 1, NOW() - INTERVAL '1 day' + INTERVAL '2 hours');
    INSERT INTO public.sale_record_items (id, store_id, sale_record_id, inventory_id, item_name, unit_label, quantity_sold, sale_price, cost_price, created_at) VALUES
      (gen_random_uuid(), v_store_id, v_sale_12, v_inv_tea, 'Taj Mahal Tea 250g', 'pack', 1.000, 195.00, 165.00, NOW() - INTERVAL '1 day' + INTERVAL '2 hours'),
      (gen_random_uuid(), v_store_id, v_sale_12, v_inv_butter, 'Amul Butter 500g', 'pack', 1.000, 275.00, 235.00, NOW() - INTERVAL '1 day' + INTERVAL '2 hours'),
      (gen_random_uuid(), v_store_id, v_sale_12, v_inv_salt, 'Tata Salt 1kg', 'kg', 1.000, 28.00, 22.00, NOW() - INTERVAL '1 day' + INTERVAL '2 hours');

    INSERT INTO public.sale_records (id, store_id, customer_name, source, total_amount, items_count, sale_date, created_at)
    VALUES (v_sale_13, v_store_id, 'Karan', 'manual', 220.00, 2, CURRENT_DATE - 1, NOW() - INTERVAL '1 day' + INTERVAL '4 hours');
    INSERT INTO public.sale_record_items (id, store_id, sale_record_id, inventory_id, item_name, unit_label, quantity_sold, sale_price, cost_price, created_at) VALUES
      (gen_random_uuid(), v_store_id, v_sale_13, v_inv_maggi, 'Maggi Instant Noodles 70g', 'pcs', 10.000, 14.00, 11.50, NOW() - INTERVAL '1 day' + INTERVAL '4 hours'),
      (gen_random_uuid(), v_store_id, v_sale_13, v_inv_thumsup, 'Thums Up 250ml', 'bottle', 4.000, 20.00, 15.00, NOW() - INTERVAL '1 day' + INTERVAL '4 hours');

    INSERT INTO public.sale_records (id, store_id, customer_name, source, total_amount, items_count, sale_date, created_at)
    VALUES (v_sale_14, v_store_id, 'Walk-in Customer', 'manual', 920.00, 1, CURRENT_DATE - 1, NOW() - INTERVAL '1 day' + INTERVAL '6 hours');
    INSERT INTO public.sale_record_items (id, store_id, sale_record_id, inventory_id, item_name, unit_label, quantity_sold, sale_price, cost_price, created_at) VALUES
      (gen_random_uuid(), v_store_id, v_sale_14, v_inv_atta, 'Aashirvaad Shudh Chakki Atta 10kg', 'pack', 2.000, 460.00, 390.00, NOW() - INTERVAL '1 day' + INTERVAL '6 hours');

    -- Day 5 (June 7, 2026 / Today) - 3 sales
    INSERT INTO public.sale_records (id, store_id, customer_name, source, total_amount, items_count, sale_date, created_at)
    VALUES (v_sale_15, v_store_id, 'Walk-in Customer', 'manual', 112.00, 2, CURRENT_DATE, NOW() - INTERVAL '6 hours');
    INSERT INTO public.sale_record_items (id, store_id, sale_record_id, inventory_id, item_name, unit_label, quantity_sold, sale_price, cost_price, created_at) VALUES
      (gen_random_uuid(), v_store_id, v_sale_15, v_inv_salt, 'Tata Salt 1kg', 'kg', 1.000, 28.00, 22.00, NOW() - INTERVAL '6 hours'),
      (gen_random_uuid(), v_store_id, v_sale_15, v_inv_maggi, 'Maggi Instant Noodles 70g', 'pcs', 6.000, 14.00, 11.50, NOW() - INTERVAL '6 hours');

    INSERT INTO public.sale_records (id, store_id, customer_name, source, total_amount, items_count, sale_date, created_at)
    VALUES (v_sale_16, v_store_id, 'Shyam', 'manual', 120.00, 2, CURRENT_DATE, NOW() - INTERVAL '3 hours');
    INSERT INTO public.sale_record_items (id, store_id, sale_record_id, inventory_id, item_name, unit_label, quantity_sold, sale_price, cost_price, created_at) VALUES
      (gen_random_uuid(), v_store_id, v_sale_16, v_inv_parle, 'Parle-G Biscuits 100g', 'pcs', 12.000, 5.00, 4.20, NOW() - INTERVAL '3 hours'),
      (gen_random_uuid(), v_store_id, v_sale_16, v_inv_thumsup, 'Thums Up 250ml', 'bottle', 3.000, 20.00, 15.00, NOW() - INTERVAL '3 hours');

    INSERT INTO public.sale_records (id, store_id, customer_name, source, total_amount, items_count, sale_date, created_at)
    VALUES (v_sale_17, v_store_id, 'Walk-in Customer', 'manual', 274.00, 2, CURRENT_DATE, NOW() - INTERVAL '1 hour');
    INSERT INTO public.sale_record_items (id, store_id, sale_record_id, inventory_id, item_name, unit_label, quantity_sold, sale_price, cost_price, created_at) VALUES
      (gen_random_uuid(), v_store_id, v_sale_17, v_inv_handwash, 'Dettol Liquid Handwash 200ml', 'bottle', 1.000, 99.00, 78.00, NOW() - INTERVAL '1 hour'),
      (gen_random_uuid(), v_store_id, v_sale_17, v_inv_oil, 'Fortune Mustard Oil 1L', 'bottle', 1.000, 175.00, 145.00, NOW() - INTERVAL '1 hour');

    -- Re-enable the trigger
    ALTER TABLE public.sale_record_items ENABLE TRIGGER trg_deduct_inventory_on_sale;

    -- 8. Seed Active Alerts (exactly 3 items matching our stock counts and dates)
    INSERT INTO public.alerts (id, store_id, alert_type, inventory_id, item_name, message, days_to_expiry, current_qty, is_read, created_at)
    VALUES
      (gen_random_uuid(), v_store_id, 'low_stock', v_inv_atta, 'Aashirvaad Shudh Chakki Atta 10kg', 'Aashirvaad Shudh Chakki Atta 10kg is low in stock. Current quantity: 2.000 (threshold: 5.000)', NULL, 2.000, FALSE, NOW() - INTERVAL '12 hours'),
      (gen_random_uuid(), v_store_id, 'low_stock', v_inv_surf, 'Surf Excel Easy Wash 1kg', 'Surf Excel Easy Wash 1kg is low in stock. Current quantity: 3.000 (threshold: 8.000)', NULL, 3.000, FALSE, NOW() - INTERVAL '8 hours'),
      (gen_random_uuid(), v_store_id, 'expiry_critical', v_inv_butter, 'Amul Butter 500g', 'Amul Butter 500g has EXPIRED on ' || (CURRENT_DATE - INTERVAL '2 days')::TEXT || '.', -2, 0.000, FALSE, NOW() - INTERVAL '1 day');

    -- 9. Seed Marketplace Distributors
    -- These are static distributors who will deliver to the user's location (pincode)
    DELETE FROM public.distributors WHERE business_name IN ('Balaji Grocery Distributors', 'Krishna Dairy & Beverages', 'Super Clean Household Wholesalers');
    
    INSERT INTO public.distributors (id, name, business_name, category, phone, whatsapp_no, pincode, service_regions, address, min_order_value, is_verified)
    VALUES
      (gen_random_uuid(), 'Rajesh Balaji', 'Balaji Grocery Distributors', 'Staples', '+919876543001', '+919876543001', v_store_pincode, ARRAY[v_store_pincode, '400001', '110001'], '12, Balaji Complex, Dadar, Mumbai', 2000.00, TRUE),
      (gen_random_uuid(), 'Gopal Krishna', 'Krishna Dairy & Beverages', 'Dairy', '+919876543002', '+919876543002', v_store_pincode, ARRAY[v_store_pincode, '400001', '110001'], 'Shop 5, Krishna Lane, Kurla, Mumbai', 1500.00, TRUE),
      (gen_random_uuid(), 'Sunil Mehta', 'Super Clean Household Wholesalers', 'Cleaning', '+919876543003', '+919876543003', v_store_pincode, ARRAY[v_store_pincode, '400001', '110001'], 'Plot 4, Clean Industrial Area, Andheri, Mumbai', 1000.00, TRUE);

    RAISE NOTICE 'Demo account seeding completed successfully!';
END $$;
