# Aya Market — أيا ماركت

A complete, bilingual (**Arabic RTL** / **English LTR**), dual-currency (**USD `$`** / **Lebanese Lira `LBP`**) supermarket e-commerce ecosystem built with **Kotlin**, **Jetpack Compose (Material 3)**, **CameraX + ML Kit Barcode Scanning**, and **Firebase Firestore + Google Sign-In**.

## Three Standalone Applications (Unified Real-Time Backend)

The Android project exposes **three independent launcher apps** sharing one real-time Firestore database, plus a **Pre-Login Section Selector** on the sign-in screen so users choose their section (`Customer`, `Admin`, or `Driver`) before authenticating:

1. **Aya Market (`MainActivity`) — Customer App**
   - Header switchers for **Arabic (RTL) / English (LTR)** and **USD / LBP** (calculated dynamically from the live Admin exchange rate).
   - GPS & interactive tap-to-pin delivery map + saved addresses.
   - Category browsing (`Fruits & Veg`, `Dairy`, `Bakery`, `Beverages`, `Household`), bilingual search, and CameraX + ML Kit barcode scanner.
   - Smooth 3-step checkout (`Cart` → `Address & Notes` → `Cash on Delivery / Whish Money / Card`).
   - Live order status tracker (`Pending` → `Preparing` → `Out for Delivery` → `Delivered`) with one-tap Call & WhatsApp to the assigned driver.

2. **Aya Admin (`AdminActivity`) — Store Management App (Owner-Only)**
   - **Strict Owner Lock**: Admin access is restricted strictly to the verified store owner Google account (`ADMIN_OWNER_EMAIL` in `AyaMarketRepository.kt` and `isBootstrappedAdmin()` in `firestore.rules`). Public admin account creation is disabled.
   - Live USD-to-LBP exchange rate & delivery fee control widget.
   - Analytics overview cards (Daily Sales in USD & LBP, Active Orders, Top-Selling Items).
   - Live incoming order board with notification sound chime, Accept/Reject, Driver Assignment, and Android `PrintManager` HTML invoice printing.
   - Bilingual Inventory CRUD (with Android Photo Picker), Promo Banners, and Driver roster management.

3. **Aya Driver (`DriverActivity`) — Delivery Partner App**
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

## Changing the Owner Admin Email (Before Deploying to Your Own Firebase Project)

1. Update `request.auth.token.email == 'your-owner-email@gmail.com'` inside `isBootstrappedAdmin()` in [`firestore.rules`](firestore.rules) and deploy rules with Firebase CLI (`firebase deploy --only firestore:rules`).
2. Update `ADMIN_OWNER_EMAIL` in [`AyaMarketRepository.kt`](app/src/main/java/com/example/data/remote/AyaMarketRepository.kt).

---

## Folder Structure

```text
├── app/src/main/
│   ├── AndroidManifest.xml          # 3 independent LAUNCHER activities + permissions
│   ├── java/com/example/
│   │   ├── MainActivity.kt          # Customer App entry
│   │   ├── AdminActivity.kt         # Owner Admin App entry
│   │   ├── DriverActivity.kt        # Delivery Driver App entry
│   │   ├── data/
│   │   │   ├── model/Models.kt
│   │   │   ├── preferences/UserPreferencesRepository.kt
│   │   │   └── remote/{AyaMarketRepository.kt, FirestoreErrorHandler.kt}
│   │   ├── localization/AppStrings.kt
│   │   └── ui/
│   │       ├── AyaMarketApp.kt
│   │       ├── auth/{AuthManager.kt, AuthScreen.kt}
│   │       ├── customer/{CustomerHomeScreen.kt, CustomerCartCheckoutScreen.kt, CustomerOrdersScreen.kt}
│   │       ├── admin/AdminDashboardScreen.kt
│   │       ├── driver/DriverAppScreen.kt
│   │       ├── components/{CommonComponents.kt, BarcodeScannerDialog.kt, LocationPickerDialog.kt}
│   │       ├── viewmodel/AyaMarketViewModel.kt
│   │       └── theme/{Color.kt, Theme.kt, Type.kt}
│   └── res/
│       ├── font/{cairo.ttf, inter.ttf, poppins.ttf}
│       └── drawable/ & mipmap-*/    # Distinct launcher icons for Customer, Admin, Driver
├── firebase-blueprint.json
├── firestore.rules
└── firestore.test.js
```

## Build & Test

```bash
gradle assembleDebug
gradle :app:testDebugUnitTest --tests "com.example.ExampleUnitTest"
```
