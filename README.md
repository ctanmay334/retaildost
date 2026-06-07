# RetailDost (KiranaOS)

RetailDost (KiranaOS) is an AI-powered, offline-first operating system designed specifically to empower local grocery and convenience stores (*kirana* stores) across India. It features automated invoice OCR scans, a smart voice-enabled Khata ledger, and proactive stock and credit alerts.

## Key Features

- **Voice-Enabled Khata Ledger**: Log transactions using natural language (e.g., Hinglish voice inputs) which are parsed to automatically update credit/debit balances.
- **Automated Invoice OCR**: Scan wholesaler invoices using the device camera to automatically extract, categorize, and log items into the inventory cache.
- **Offline-First Cache**: Dual-layered local Room caching allows full operations during network drops, with background queue processing to sync changes to Supabase when connectivity is restored.
- **Marketplace Connection**: Seamless regional distributor lookup and order placement via WhatsApp integration.

---

## Run Locally

### Prerequisites

- [Android Studio](https://developer.android.com/studio) (Koala 2024.1.1 or higher recommended)
- Android SDK 31+ (Compile SDK is 35)

### Setup Steps

1. **Clone & Open Project**:
   - Open Android Studio, select **Open**, and select this project directory.
   - Allow Android Studio to index and resolve dependencies.

2. **Environment Configuration**:
   - Create a file named `.env` in the root project directory.
   - Add your API keys and endpoints as shown in `.env.example`:
     ```env
     GEMINI_API_KEY=your_gemini_api_key_here
     SUPABASE_URL=your_supabase_url_here
     SUPABASE_ANON_KEY=your_supabase_anon_key_here
     ```

3. **Build & Run**:
   - Connect an Android emulator or physical device (with Developer Options enabled).
   - Press **Run** in Android Studio to build and launch the application.
