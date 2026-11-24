# PaiseJar — India Round-Up Savings Companion (Android)

PaiseJar helps users build an automatic saving habit by calculating round-up spare change on everyday purchases (e.g., rounding a `₹180` coffee to `₹200` for `+₹20`, or a `₹470` food order to `₹500` for `+₹30`).

---

## Architecture & Connected Backend Integrations

### Genuinely Connected & Verified
- **Google Sign-In (Jetpack Credential Manager + Firebase Authentication)**: Real authentication (`GetSignInWithGoogleOption` + `GetGoogleIdOption`) with `NonCancellable` sign-out and Credential Manager state clearing. No anonymous auth or mock logins.
- **Cloud Firestore (`ai-studio-android-paisejar-d4fca9b6-7d0d-4f81-acc2-488b3a5ed511`)**: Real-time authenticated per-user sync (`users/{userId}`, `goals`, `transactions`, append-only `auditLogs`, and `supportTickets`) backed by zero-trust `firestore.rules` (verified against the Firestore emulator and deployed).
- **Deterministic Calculation Engine (`RoundUpCalculator`)**: Exact paise-level arithmetic supporting `₹10`, `₹50`, `₹100`, or custom `₹10` increments, per-purchase limits, weekly caps, monthly caps, minimum transaction thresholds, category/merchant exclusions, idempotency deduplication, and refund/reversal/delayed-feed/failed-instruction handling.
- **Explicit Ledger Separation**: Confirmed bank/partner ledger totals (`Available`, `Pending`, `Completed`) remain transparently at `₹0` while bank/payment integrations are disconnected, separated visually and structurally from opt-in **Sandbox Calculation Previews**.

---

## Integrations Still Missing (Required Before Live Money Movement)

1. **RBI-Licensed Account Aggregator (FIU) Integration**:
   - Required to read verified UPI/bank transactions and account balances under explicit RBI AA consent artefacts.
   - *Note*: Account Aggregators provide read-only transaction data and **do not move money**.
2. **NPCI UPI Autopay / Regulated Payment Partner**:
   - Required to execute actual round-up debits, handle real settlement webhooks, and update `confirmedBalanceRupees` / `pendingTransferRupees`.
   - Because no partner credentials or regulatory approvals are provisioned here, real-money transfer instructions remain disabled and the Low-Balance Bank Guard stays clearly marked unavailable.

---

## Required Steps Before Google Play Store Submission

1. **Financial Partner & Legal Disclosures**:
   - Complete contractual integration with licensed AA FIU & UPI/banking/escrow partners.
   - Publish hosted Privacy Policy, Terms of Service, Transparent Fee Schedule, and Grievance Redressal Officer contact pages.
2. **Security & Firebase Hardening**:
   - Enable **Firebase App Check** (Play Integrity provider) for production builds.
   - Register your Google Play App Signing / release upload certificate SHA-1 and SHA-256 fingerprints in Firebase Console.
3. **Play Console Compliance**:
   - Complete Google Play **Financial Features Declaration** and **Data Safety** form accurately reflecting Google Sign-In, Firestore storage, and account deletion support.
   - Generate a signed release `.aab` (`bundleRelease`) using your upload keystore (`KEYSTORE_PATH`, `STORE_PASSWORD`, `KEY_PASSWORD`).
