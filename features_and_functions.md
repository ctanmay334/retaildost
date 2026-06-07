# KiranaOS — Features & Functions Reference

> **Living document.** Every feature is mapped to its Supabase backend, AI model, Android architecture components, and offline behaviour.  
> Platform: Android (Kotlin + Jetpack Compose) · Backend: Supabase · AI: Google Gemini

---

## 🗺️ Feature Map

| # | Feature | AI Required | Edge Function | Offline-First | Plan Gate |
|---|---|---|---|---|---|
| F1 | Authentication & Profile | ✗ | ✗ | Partial | Free |
| F2 | Inventory — Invoice OCR (Stock In) | ✅ Gemini 3.5 Flash / 3.1 / 2.5 | `ocr-invoice` | ✅ Queue | Free (5/mo) |
| F3 | Sale Record (Manual Stock Out) | ✗ | ✗ | ✅ Queue | Free |
| F4 | Inventory — Diary OCR (Stock Out) | ✅ Gemini 3.5 Flash / 3.1 / 2.5 | `ocr-diary` | ✅ Queue | Free (5/mo) |
| F5 | Khata Entry — NLP Voice/Text | ✅ Gemini 3.5 Flash | `khata-nlp` | ✅ Queue | Free |
| F6 | Khata Book — Balance View | ✗ | ✗ | ✅ Cache | Free |
| F7 | Expiry & Low-Stock Alerts | ✗ | `alert-checker` | ✅ Cache | Free |
| F8 | Distributor Marketplace | ✗ | ✗ | Read cache | Free |
| F9 | Analytics Dashboard | ✅ Gemini 3.5 Flash | ✗ (in-app client) | ✗ | **Pro** |
| F10 | Offline-First Sync | ✗ | `sync-batch` | Core infra | Free |
| F11 | Onboarding | ✗ | ✗ | DataStore | Free |
| F12 | Plan / Paywall | ✗ | (server-side checks) | ✗ | Free/Pro |

---

## Feature 1 — Authentication & Profile

### Functions
| ID | Description |
|---|---|
| F1.1 | Email & Password login / signup via Supabase Auth |
| F1.2 | First-login profile setup: `owner_name`, `store_name`, `pincode`, `city`, `state` |
| F1.3 | Auto-create `profiles` row via DB trigger on `auth.users` insert |
| F1.4 | JWT stored securely in `EncryptedSharedPreferences` |
| F1.5 | Session auto-refresh via Supabase Kotlin SDK |
| F1.6 | Logout: clear Room DB + revoke token |

### Backend
- **Supabase Auth** — `signUpWith(Email)` / `signInWith(Email)`
- **Table:** `profiles` (auto-created by `handle_new_user()` trigger)
- **RLS:** `profiles_select_own`, `profiles_update_own`

### Android Architecture
```
AuthViewModel (HiltViewModel)
  └── LoginUseCase
  └── SignupUseCase
  └── ForgotPasswordUseCase
       └── AuthRepository (interface)
            └── AuthRepositoryImpl
                 ├── SupabaseClient.auth (Supabase SDK)
                 └── EncryptedSharedPreferences (JWT)
```

### Screens
- `LoginScreen` — Email + password input fields
- `SignupScreen` — Email + password registration fields
- `OnboardingShopDetailsScreen` — Store name, owner name, pincode

---

## Feature 2 — Inventory: Invoice OCR (Stock In)

### Functions
| ID | Description |
|---|---|
| F2.1 | "Add Stock" → "Scan Invoice" → native CameraX capture |
| F2.2 | Image uploaded to `invoice-images` bucket: `{store_id}/{timestamp}_{filename}` |
| F2.3 | Edge Function `ocr-invoice` called with image path + idempotency key |
| F2.4 | Gemini Flash Vision API processes image and returns extracted items |
| F2.5 | Extract per line: `item_name`, `quantity`, `unit_label`, `cost_price`, `mrp`, `batch_no`, `expiry_date` |
| F2.6 | Category-based expiry defaults: biscuits/snacks +6 months, dairy +15 days |
| F2.7 | Confirmation list returned; low-confidence fields highlighted in amber |
| F2.8 | User confirms / edits / removes individual line items |
| F2.9 | Confirmed items **upserted** into `inventory` (idempotency prevents double-processing) |
| F2.10 | `ocr_jobs` row created for audit + retry tracking |
| F2.11 | OCR counter incremented on `profiles.ocr_scans_this_month`; 429 if free tier ≥ 5/month |

### Backend
- **Storage:** `invoice-images` (private bucket, store-namespaced RLS)
- **Edge Function:** `ocr-invoice` (Gemini API)
- **Tables:** `inventory` (upsert), `ocr_jobs` (audit), `idempotency_keys`, `profiles` (counter)
- **AI:**
  - **Model:** Gemini 3.5 Flash / 3.1 Flash Lite / 2.5 Flash — structured JSON extraction

### Android Architecture
```
InventoryViewModel (HiltViewModel)
  └── ScanInvoiceUseCase
  │    └── InventoryRepository
  │         ├── SupabaseStorage.upload() → returns image_path
  │         └── EdgeFunctionApi.callOcrInvoice(image_path, idempotency_key)
  └── ConfirmOcrItemsUseCase
       └── InventoryRepository
            ├── InventoryDao.upsert() [Room — offline-first]
            └── OfflineQueueDao.enqueue(action_type="inventory_add")
```

### Screens
- `CameraScreen` — CameraX preview + capture button
- `ReviewInvoiceScreen` — OCR line items, amber highlights, edit/remove per row

### Confidence Flags
- `confidence < 0.75` → amber highlight on that field
- User **must** tap to approve each flagged field before confirming

---

## Feature 3 — Sale Record (Manual Stock Out)

### Functions
| ID | Description |
|---|---|
| F3.1 | "Record Sale" button always visible on home screen (primary action) |
| F3.2 | Sale entry: searchable dropdown (fuzzy match on `inventory`), quantity, optional sale price, optional customer name |
| F3.3 | Multi-line sale session (simple bill with multiple items) |
| F3.4 | On confirm: `sale_records` + `sale_record_items` inserted; inventory `quantity` decremented |
| F3.5 | Quantity-below-zero warning (soft block — override allowed with confirmation) |
| F3.6 | If customer name matches `khata_customers` → prompt "Add to Ramesh's Khata?" → one-tap creates `khata_transactions` debit |
| F3.7 | Low-stock check after each sale; local notification if item drops below `min_threshold` |
| F3.8 | Offline: sale written to Room + enqueued; synced on reconnect |
| F3.9 | Sales Log screen: chronological history with daily totals |

### Backend
- **Tables:** `sale_records` (header), `sale_record_items` (lines), `inventory` (auto-decremented by DB trigger `deduct_inventory_on_sale`)
- **No Edge Function** — direct Supabase SDK calls from Android
- **RLS:** `sale_records_all_own`, `sale_items_all_own`

### Android Architecture
```
SaleViewModel (HiltViewModel)
  └── RecordSaleUseCase
  │    └── SaleRepository
  │         ├── SaleRecordDao.insert()       [Room]
  │         ├── SaleRecordItemDao.insertAll() [Room]
  │         ├── InventoryDao.decrementQty()  [Room]
  │         └── OfflineQueueDao.enqueue(action_type="sale_record")
  └── GetSalesHistoryUseCase
       └── SaleRecordDao.getAllWithItems()   [Room Flow]
```

### Screens
- `RecordSaleScreen` — fuzzy search inventory, add line items, optional customer/price
- `SalesLogScreen` — date-grouped history with daily revenue totals

---

## Feature 4 — Inventory: Diary OCR (Stock Out via Handwriting)

### Functions
| ID | Description |
|---|---|
| F4.1 | "Record Sales" → "Scan Diary Page" |
| F4.2 | Photo uploaded to Storage; `ocr-diary` Edge Function called |
| F4.3 | AI reads Devanagari + English + mixed handwriting |
| F4.4 | Per-line extraction: `product_name`, `quantity_sold`, `price` (if written) |
| F4.5 | Low-confidence entries flagged — user must explicitly approve each |
| F4.6 | Unknown item names flagged for mapping to existing inventory SKUs; mapping saved in `ocr_name_mappings` |
| F4.7 | Confirmed: inventory quantities decremented; `sale_records` inserted with `source = 'ocr_diary'` |
| F4.8 | Usage counted against same free-tier OCR counter as invoice OCR |

### Backend
- **Storage:** `invoice-images` (same bucket)
- **Edge Function:** `ocr-diary` (Devanagari-aware prompt)
- **Tables:** `inventory`, `sale_records`, `sale_record_items`, `ocr_jobs`, `ocr_name_mappings`
- **AI:** Gemini 3.5 Flash / 3.1 Flash Lite / 2.5 Flash

### Android Architecture
```
InventoryViewModel
  └── ScanDiaryUseCase
       └── InventoryRepository
            ├── SupabaseStorage.upload()
            └── EdgeFunctionApi.callOcrDiary()
  └── ConfirmDiaryEntriesUseCase
       ├── SaleRecordDao.insert() [source=ocr_diary]
       ├── InventoryDao.decrementQty()
       └── OcrNameMappingDao.save()    ← learned mappings
```

---

## Feature 5 — Khata Entry (NLP Credit Ledger)

### Functions
| ID | Description |
|---|---|
| F5.1 | Persistent Khata FAB on home screen |
| F5.2 | Input: typed text OR mic (Android `SpeechRecognizer` → STT → text) |
| F5.3 | Text sent to `khata-nlp` Edge Function |
| F5.4 | NLP returns `{ intent, customer_name, amount, confidence, raw_input }` |
| F5.5 | If `confidence < 0.80` → confirmation card: "Did you mean: ₹500 Udhar for Ramesh?" |
| F5.6 | On confirm: upsert `khata_customers`; insert `khata_transactions` with idempotency key |
| F5.7 | DB trigger auto-updates `khata_customers.running_balance` |
| F5.8 | Toast: "✓ ₹500 Udhar added for Ramesh. Balance: ₹1,200 pending." |
| F5.9 | **Append-only** ledger — corrections = new reversal entry (`type = 'reversal'`) |
| F5.10 | Every entry idempotency-keyed (UUID generated client-side before call) |

### Backend
- **Edge Function:** `khata-nlp` (Gemini API with Hinglish system prompt)
- **Tables:** `khata_customers` (upsert), `khata_transactions` (insert), `idempotency_keys`
- **DB Trigger:** `khata_tx_update_balance` auto-updates customer's `running_balance`
- **AI:** Gemini 3.5 Flash — Hinglish intent classification

### Hinglish Intent Reference
| Input | Intent | Confidence |
|---|---|---|
| "Ramesh ka 500 ka udhar" | debit | ~0.97 |
| "Sunita ko 200 diya" | debit | ~0.92 |
| "Priya ka 500 clear" | credit | ~0.95 |
| "Ramesh ne 200 diye" | credit | ~0.93 |
| "Ramesh ka kitna baaki hai" | query | ~0.99 |
| "Total kitna milna hai" | query | ~0.96 |

### Android Architecture
```
KhataViewModel (HiltViewModel)
  └── AddKhataEntryUseCase
  │    ├── EdgeFunctionApi.callKhataNlp(raw_input)
  │    ├── KhataCustomerDao.upsert()         [Room]
  │    ├── KhataTransactionDao.insert()      [Room]
  │    └── OfflineQueueDao.enqueue(action_type="khata_entry")
  └── SpeechRecognizerManager               [STT → text]
```

### Screens
- `KhataEntryBottomSheet` — mic button + text field + NLP result confirmation card

---

## Feature 6 — Khata Book (Balance View)

### Functions
| ID | Description |
|---|---|
| F6.1 | Screen: all `khata_customers` with `running_balance > 0` |
| F6.2 | Sort: highest balance / most recent activity / alphabetical |
| F6.3 | Tap customer → full immutable transaction history (chronological) |
| F6.4 | "Send Reminder" → WhatsApp Intent with pre-filled message |
| F6.5 | Query intent from NLP (F5) opens this screen filtered to customer |

### WhatsApp Message Template
```
Namaste {name} bhai, aapka {store_name} mein ₹{balance} baaki hai.
Kabhi bhi aa ke de sakta hai. 🙏
```

### Backend
- **Tables:** `khata_customers` (read), `khata_transactions` (read)
- **RLS:** `khata_tx_select_own`, `khata_customers_all_own`
- No Edge Function — direct Supabase queries

### Android Architecture
```
KhataViewModel
  └── GetKhataBookUseCase
  │    └── KhataCustomerDao.getAllWithBalanceFlow()  [Room Flow]
  └── GetCustomerTransactionsUseCase
  │    └── KhataTransactionDao.getByCustomerFlow()  [Room Flow]
  └── SendWhatsAppReminderUseCase
       └── WhatsAppHelper.buildIntent(phone, message)
```

### Screens
- `KhataBookScreen` — customer list with balance chips + sort controls
- `CustomerLedgerScreen` — immutable transaction history + reminder button

---

## Feature 7 — Expiry & Low-Stock Alerts

### Functions
| ID | Description |
|---|---|
| F7.1 | `alert-checker` Edge Function runs via pg_cron at **8 AM IST** daily |
| F7.2 | Expiry alert: `days_to_expiry ≤ 30 AND ≥ 0` |
| F7.3 | Low stock alert: `quantity ≤ min_threshold` |
| F7.4 | Rows inserted into `alerts` table |
| F7.5 | FCM push notification sent to `profiles.fcm_token` |
| F7.6 | In-app Alerts tab: unread alerts with badge count |
| F7.7 | Tapping an alert deep-links to the inventory item |
| F7.8 | From expiry alert: "Find Distributor" CTA → Marketplace filtered by category |

### Backend
- **Edge Function:** `alert-checker` (pg_cron scheduled)
- **Tables:** `alerts` (insert + read), `inventory` (read)
- **FCM:** Firebase Cloud Messaging V1 API
- **RLS:** `alerts_all_own`

### Android Architecture
```
AlertViewModel (HiltViewModel)
  └── GetAlertsUseCase
  │    └── AlertDao.getUnreadAlertsFlow()    [Room Flow]
  └── MarkAlertReadUseCase
  │    ├── AlertDao.markRead()               [Room]
  │    └── SupabaseClient.from("alerts").update()
  └── KiranaFCMService (FirebaseMessagingService)
       ├── onNewToken → updates profiles.fcm_token
       └── onMessageReceived → AlertDao.insert() + local notification
```

### Screens
- `AlertsScreen` — categorised list (expiry_critical / expiry_warning / low_stock), badge count

---

## Feature 8 — Distributor Marketplace

### Functions
| ID | Description |
|---|---|
| F8.1 | Search by category + pincode (defaults to store's pincode) |
| F8.2 | Results from `distributors` table (authenticated public read) |
| F8.3 | Each listing: `business_name`, `categories[]`, `pincodes[]`, verified badge |
| F8.4 | "Contact on WhatsApp" → WhatsApp Intent |
| F8.5 | No results → auto-expand to adjacent pincodes + "Invite a distributor" share link |
| F8.6 | Distributor self-registration: `business_name`, `whatsapp_no`, `pincodes[]`, `categories[]` |

### Backend
- **Table:** `distributors` (read: all authenticated, write: own row only)
- **RLS:** `distributors_select_active`, `distributors_write_own`, `distributors_update_own`
- No Edge Function — direct Supabase queries

### Android Architecture
```
MarketplaceViewModel (HiltViewModel)
  └── SearchDistributorsUseCase
  │    └── MarketplaceRepository
  │         └── SupabaseClient.from("distributors")
  │              .filter("pincodes", "cs", "{$pincode}")
  └── RegisterAsDistributorUseCase
       └── MarketplaceRepository
            └── SupabaseClient.from("distributors").insert()
```

### Screens
- `DistributorListScreen` — search bar + results list + WhatsApp CTAs
- `DistributorRegisterScreen` — multi-select categories + pincode input

---

## Feature 9 — Analytics Dashboard (Pro Tier)

### Functions
| ID | Description |
|---|---|
| F9.1 | Blurred preview on Free tier with upgrade CTA |
| F9.2 | Top 10 fastest-moving SKUs (last 30 days, by units sold) |
| F9.3 | Top 5 SKUs most at risk of expiry (≤30 days, quantity > 0) |
| F9.4 | Total Khata outstanding (sum of all positive `running_balance`) |
| F9.5 | Monthly revenue estimate: `sum(sale_price × quantity)` |
| F9.6 | AI-generated business insights (3-5 sentence Hinglish/English summary) via Gemini 3.5 Flash |
| F9.7 | CSV export of `sale_records` + `khata_transactions` (Pro only) |

### Backend
- **Client API Wrapper:** `GeminiClient` (Android in-app generative API call)
- **Tables:** `sale_records`, `sale_record_items`, `khata_customers`, `inventory`
- **AI:** Gemini 3.5 Flash — plain-language Hinglish business insights
- **Plan check:** Enforced client-side and verified via user profile tier

### Android Architecture
```
AnalyticsViewModel (HiltViewModel)
  └── GetAnalyticsSummaryUseCase
  │    └── EdgeFunctionApi.callAnalyticsSummary(store_id)
  └── ExportCsvUseCase  [Pro only]
       └── AnalyticsRepository
            └── SupabaseClient queries → CSV generation
```

### Screens
- `InsightsScreen` — KPI cards + top SKUs chart + AI insight card + blur gate

---

## Feature 10 — Offline-First Sync

### Functions
| ID | Description |
|---|---|
| F10.1 | All writes (Khata, Sale, Inventory) → Room DB first, then Supabase |
| F10.2 | `offline_queue` Room table: `action_type`, `payload_json`, `idempotency_key`, `client_ts`, `sync_status` |
| F10.3 | `SyncManager` (WorkManager PeriodicWork 15 min) processes queue when online |
| F10.4 | Queue batched to `POST /sync-batch` Edge Function |
| F10.5 | Idempotency key prevents duplicate processing if network drops mid-sync |
| F10.6 | Optimistic UI: pending entries show "⏳ syncing" badge; clears on success |
| F10.7 | After 3 retries → `sync_failed`; persistent badge + manual retry option |

### Backend
- **Edge Function:** `sync-batch` (batch processor with per-action idempotency)
- **Table:** `sync_queue` (server-side audit), `idempotency_keys`

### Android Architecture
```
SyncManager : CoroutineWorker (WorkManager, @HiltWorker)
  └── SyncOfflineQueueUseCase
       ├── OfflineQueueDao.getPendingActions()
       ├── EdgeFunctionApi.callSyncBatch(actions)
       └── OfflineQueueDao.markSynced(idempotency_keys)
NetworkObserver (ConnectivityManager Flow)
  └── triggers SyncManager.enqueueOneShot() on reconnect
```

### Offline Queue Action Types
| Action Type | Triggered By |
|---|---|
| `khata_entry` | F5 Khata entry confirm |
| `sale_record` | F3 Sale record confirm |
| `inventory_add` | F2/F11 New item confirmed |
| `inventory_edit` | Manual edit in AddProductScreen |

---

## Feature 11 — Onboarding

### Functions
| ID | Description |
|---|---|
| F11.1 | First-launch 3-screen walkthrough: Inventory → Khata → Alerts |
| F11.2 | After auth: prompt to add first inventory item (OCR or manual) |
| F11.3 | "Manual Add Item" form: name, category, quantity, cost price, MRP, expiry |

### Backend
- **Table:** `profiles` (update `onboarded_at` on completion)
- **DataStore:** persist `onboarding_complete` flag locally

---

## Feature 12 — Plan / Paywall

### Functions
| ID | Description |
|---|---|
| F12.1 | `profiles.plan`: `'free'` or `'pro'` |
| F12.2 | Free limits enforced server-side: 5 OCR scans/month, 10 WhatsApp reminders/month |
| F12.3 | Pro CTA: Razorpay link in Chrome Custom Tab |
| F12.4 | Post-payment: manual plan upgrade in Supabase dashboard (MVP) |

### Plan Limit Enforcement (client-side & edge validation)
| Limit | Free | Pro | Enforced In |
|---|---|---|---|
| OCR scans/month | 5 | Unlimited | `ocr-invoice`, `ocr-diary` (edge) |
| WhatsApp reminders | 10/month | Unlimited | Client-side counter |
| Analytics Dashboard | ✗ (blurred) | ✅ | Client-side (GeminiClient) |
| CSV Export | ✗ | ✅ | Client-side |

---

## 🧠 AI Model Reference

| Model | Provider | Used In | Purpose |
|---|---|---|---|
| `gemini-3.5-flash` | Google | F2, F4, F5, F9 | Invoice/diary OCR, Khata NLP intent parsing, & Business insights (Primary) |
| `gemini-3.1-flash-lite` | Google | F2, F4 | Invoice/diary OCR (Secondary fallback) |
| `gemini-2.5-flash` | Google | F2, F4 | Invoice/diary OCR (Tertiary fallback) |

---

## 🗃️ Supabase Table → Feature Matrix

| Table | Feature | Access |
|---|---|---|
| `profiles` | F1, F2, F7, F12 | Own row |
| `inventory` | F2, F3, F4, F7, F8 | Own store |
| `sale_records` | F3, F4, F9 | Own store |
| `sale_record_items` | F3, F4, F9 | Via sale_record |
| `khata_customers` | F5, F6 | Own store |
| `khata_transactions` | F5, F6, F9 | Append-only per store |
| `alerts` | F7 | Own store |
| `ocr_jobs` | F2, F4 | Own store |
| `ocr_name_mappings` | F4 | Own store |
| `distributors` | F8 | Public read (active) |
| `idempotency_keys` | F2, F4, F5, F10 | Own store |
| `sync_queue` | F10 | Own store |

---

## 📱 Screen → Feature → ViewModel Map

| Screen | Feature | ViewModel |
|---|---|---|
| `LoginScreen` | F1 | `AuthViewModel` |
| `OnboardingHighlightsScreen` | F11 | `AuthViewModel` |
| `OnboardingShopDetailsScreen` | F1, F11 | `AuthViewModel` |
| `DashboardScreen` | F3, F5, F7 | `HomeViewModel` |
| `CameraScreen` | F2, F4 | `InventoryViewModel` |
| `ReviewInvoiceScreen` | F2 | `InventoryViewModel` |
| `AddProductScreen` | F2, F11 | `InventoryViewModel` |
| `ViewAllProductsScreen` | F2 | `InventoryViewModel` |
| `ProductDetailScreen` | F2 | `InventoryViewModel` |
| `RecordSaleScreen` | F3 | `SaleViewModel` |
| `SalesLogScreen` | F3 | `SaleViewModel` |
| `KhataBookScreen` | F6 | `KhataViewModel` |
| `CustomerLedgerScreen` | F5, F6 | `KhataViewModel` |
| `AlertsScreen` | F7 | `AlertViewModel` |
| `DistributorListScreen` | F8 | `MarketplaceViewModel` |
| `DistributorRegisterScreen` | F8 | `MarketplaceViewModel` |
| `InsightsScreen` | F9 | `AnalyticsViewModel` |
| `SettingsScreen` | F1, F12 | `SettingsViewModel` |

---

*KiranaOS — Stratos Web Developers · Hykr Build Challenge*  
*"Build for Suresh Bhai first. The schema doesn't lie. Validate everything."*
