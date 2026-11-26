# Aya Market — أيا ماركت

A complete, bilingual (**Arabic RTL** / **English LTR**), dual-currency (**USD `$`** / **Lebanese Lira `LBP`**) supermarket e-commerce ecosystem built with **Kotlin**, **Jetpack Compose (Material 3)**, **CameraX + ML Kit Barcode Scanning**, and **Firebase Firestore + Google Sign-In**.

## Three Separate Installable Applications (Unique Packages, Unified Real-Time Backend)

Each role lives in its own **independent Android application module and package (`applicationId`)**, so customers, store owners, and drivers each install **only** their own dedicated app without installing the other two:

1. **Aya Market (`:app` — `com.aistudio.ayamarket.kxmpzq`) — Customer App**
   - Header switchers for **Arabic (RTL) / English (LTR)** and **USD / LBP** (calculated dynamically from the live Admin exchange rate).
   - GPS & interactive tap-to-pin delivery map + saved addresses.
   - Category browsing (`Fruits & Veg`, `Dairy`, `Bakery`, `Beverages`, `Household`), bilingual search, and CameraX + ML Kit barcode scanner.
   - Smooth 3-step checkout (`Cart` → `Address & Notes` → `Cash on Delivery / Whish Money / Card`).
   - Live order status tracker (`Pending` → `Preparing` → `Out for Delivery` → `Delivered`) with one-tap Call & WhatsApp to the assigned driver.

2. **Aya Admin (`:admin` — `com.aistudio.ayamarket.admin.kxmpzq`) — Owner-Only Management App**
   - **Strict Owner Lock**: Admin access is restricted strictly to the verified store owner Google account (`ADMIN_OWNER_EMAILS` in `AyaMarketRepository.kt` and `isBootstrappedAdmin()` in `firestore.rules`: `ayasmarket44@gmail.com`). Public admin registration is disabled.
   - Live USD-to-LBP exchange rate & delivery fee control widget.
   - Analytics overview cards (Daily Sales in USD & LBP, Active Orders, Top-Selling Items).
   - Live incoming order board with notification sound chime, Accept/Reject, Driver Assignment, and Android `PrintManager` HTML invoice printing.
   - Bilingual Inventory CRUD (with Android Photo Picker), Promo Banners, and Driver roster management.

3. **Aya Driver (`:driver` — `com.aistudio.ayamarket.driver.kxmpzq`) — Delivery Partner App**
   - High-contrast road UI supporting Arabic & English.
   - Availability toggle (`Online / Offline`) & copyable Driver ID for Admin onboarding.
   - Real-time assigned delivery cards displaying items and exact total to collect in the customer's chosen currency (`USD` or `LBP`).
   - One-tap navigation via **Google Maps** & **Waze**, Call/WhatsApp customer, and large status actions (`Mark Picked Up`, `Confirm Cash & Delivered`).

---

## Firestore Database Schema

- `/store_config/main` — Live `exchangeRateLbp`, `deliveryFeeUsd`, `freeDeliveryThresholdUsd`
- `/products/{productId}` — Bilingual names/descriptions/units, `category`, `priceUsd`, `discountPercent`, `stock`, `barcode`, `imageKey`
- `/promos/{promoId}` — Bilingual promotional banners
- `/users/{userId}/addresses/{addressId}` — User-isolated saved delivery addresses & coordinates
- `/drivers/{driverId}` — Verified delivery drivers roster
- `/orders/{orderId}` — Real-time orders synchronized across Customer, Admin, and Driver

Security rules are defined in [`firestore.rules`](firestore.rules) with zero-trust attribute-based access control and validated in [`firestore.test.js`](firestore.test.js).

---

## Project Structure

```text
├── shared/                   # Shared Kotlin domain, localization, Firebase repo & Compose UI
├── app/                      # Customer App (com.aistudio.ayamarket.kxmpzq)
├── admin/                    # Owner Admin App (com.aistudio.ayamarket.admin.kxmpzq)
├── driver/                   # Delivery Driver App (com.aistudio.ayamarket.driver.kxmpzq)
├── firebase-blueprint.json
├── firestore.rules
└── firestore.test.js
```

## Build & Test

### Building on GitHub Actions
The repository includes `.github/workflows/android.yml`, which restores `debug.keystore` from `debug.keystore.base64`, configures Firebase for all 3 modules (`app`, `admin`, `driver` from the module `google-services` configs or the optional `GOOGLE_SERVICES_JSON` Repository Secret), runs unit tests, and builds 3 separate APKs (`aya-customer-debug-apk`, `aya-admin-debug-apk`, `aya-driver-debug-apk`). You can trigger builds automatically on `push` or manually via **Actions → Aya Market Android CI → Run workflow**.

```bash
gradle :app:assembleDebug :admin:assembleDebug :driver:assembleDebug
gradle :app:testDebugUnitTest --tests "com.example.ExampleUnitTest"
```
