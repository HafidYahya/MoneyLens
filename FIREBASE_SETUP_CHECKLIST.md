# 📋 LOGIN SETUP CHECKLIST

Gunakan checklist ini untuk memastikan semua step setup Firebase selesai dengan benar.

## ✅ STEP 1: Firebase Project Setup

- [ ] Buka https://console.firebase.google.com
- [ ] Click "Add project" atau go to existing project
- [ ] Project name: "MoneyLens"
- [ ] Accept terms
- [ ] Wait for project ready (~2 minutes)
- [ ] Verify project bisa di-akses

## ✅ STEP 2: Get SHA-1 Fingerprint

- [ ] Buka Android Studio
- [ ] Menu: Tools → App Signing → KeyStore
- [ ] Browse: C:\Users\[USERNAME]\.android\debug.keystore
- [ ] Password: "android" (default)
- [ ] Pilih alias terakhir (biasanya "androiddebugkey")
- [ ] Copy SHA-1 hash
- [ ] Paste di text file untuk referensi

## ✅ STEP 3: Register Android App di Firebase

- [ ] Di Firebase Console, click "Add app" → "Android"
- [ ] Package name: **com.app.moneylens** (exact!)
- [ ] Paste SHA-1 dari STEP 2
- [ ] App nickname: MoneyLens (optional)
- [ ] Click "Register app"
- [ ] Download google-services.json
- [ ] Save ke text file local path untuk reference

## ✅ STEP 4: Download google-services.json

- [ ] Ada file google-services.json sudah di-download (STEP 3)
- [ ] Copy file ke: **D:\SKRIPSI\APLIKASI\MoneyLens\app\google-services.json**
- [ ] (Ganti file dummy yang sudah ada)
- [ ] Verify file ada: `dir D:\SKRIPSI\APLIKASI\MoneyLens\app\google-services.json`

## ✅ STEP 5: Enable Google Sign-In

- [ ] Masih di Firebase Console project MoneyLens
- [ ] Menu sebelah kiri: "Authentication"
- [ ] Click tab "Sign-in method"
- [ ] Find "Google" di list
- [ ] Click "Google"
- [ ] Toggle "Enable" (switch ON/blue)
- [ ] Pilih support email (bisa any email)
- [ ] Click "Save"
- [ ] Verify status: "Google" sekarang "Enabled"

## ✅ STEP 6: Get Web Client ID

- [ ] Di Firebase Console, click gear icon (Settings)
- [ ] Click "Project settings"
- [ ] Tab "Service Accounts"
- [ ] Click "Generate New Private Key"
- [ ] File JSON akan di-download otomatis
- [ ] Buka file dengan text editor
- [ ] Cari field yang ada:
  ```
  "type": "web",
  "client_id": "XXXXXXXXXXXX-XXXXXXXXXXXX.apps.googleusercontent.com",
  ```
- [ ] Copy value client_id (string panjangnya)
- [ ] Paste di text file untuk step berikutnya

## ✅ STEP 7: Update strings.xml

- [ ] Buka file: `D:\SKRIPSI\APLIKASI\MoneyLens\app\src\main\res\values\strings.xml`
- [ ] Find line: `<string name="default_web_client_id">...`
- [ ] Replace value dengan Web Client ID dari STEP 6
- [ ] Save file
- [ ] Example (BUKAN actual nilai):
  ```xml
  <string name="default_web_client_id">123456789-abcdefghijklmnop.apps.googleusercontent.com</string>
  ```

## ✅ STEP 8: Build APK

- [ ] Buka PowerShell/Terminal
- [ ] Navigate ke: `D:\SKRIPSI\APLIKASI\MoneyLens`
- [ ] Run command:
  ```bash
  .\gradlew clean build
  ```
- [ ] Wait ~3-5 minutes untuk build selesai
- [ ] Verify output: "BUILD SUCCESSFUL" di akhir
- [ ] APK file di: `app/build/outputs/apk/debug/app-debug.apk`

## ✅ STEP 9: Install ke Device

- [ ] Connect Android device ke laptop via USB
- [ ] Enable "USB Debugging" di device:
  - Settings → Developer Options → USB Debugging (toggle ON)
- [ ] Verify device terbaca: `adb devices` di terminal
- [ ] Run command:
  ```bash
  adb install -r app/build/outputs/apk/debug/app-debug.apk
  ```
- [ ] Wait install selesai (~1 minute)
- [ ] Verify: app muncul di device

## ✅ STEP 10: First Time Test

- [ ] Buka app MoneyLens di device
- [ ] Verify: Splash screen 2 detik
- [ ] Should redirect to: LoginActivity
- [ ] Click "Login dengan Google"
- [ ] Pilih akun Google Anda
- [ ] Verify: App asking permissions & consent
- [ ] Click approve/continue untuk semua
- [ ] Wait loading... "Menyimpan data ke server..."
- [ ] Should redirect to: MainActivity (camera)
- [ ] Verify: Camera preview hidup

## ✅ STEP 11: Session Persistence Test

- [ ] App sudah login (STEP 10 selesai)
- [ ] Kill app (swipe from recent, atau tutup)
- [ ] Open app lagi
- [ ] Verify: Splash 2 detik
- [ ] Should go DIRECTLY to MainActivity
- [ ] NO login screen (session persisted)
- [ ] Success! ✅

## ✅ STEP 12: Database Verification (Optional)

- [ ] Buka MySQL client (phpMyAdmin atau MySQL Workbench)
- [ ] Connect ke database: moneylens_users table
- [ ] Run query:
  ```sql
  SELECT * FROM moneylens_users ORDER BY created_at DESC LIMIT 1;
  ```
- [ ] Verify data:
  - firebase_uid = login user ID
  - email = login email
  - display_name = login name
  - device_brand, model, android_version = device Anda
  - last_sign_in_at = recent
- [ ] Success! ✅

---

## 🔗 Reference Links

- Firebase Console: https://console.firebase.google.com
- Google Cloud Console: https://console.cloud.google.com
- API Docs: http://moneylens.datanex.org/api
- Android Studio: https://developer.android.com/studio

---

## 🚨 Common Issues & Fixes

| Issue | Solution |
|-------|----------|
| SHA-1 mismatch error | Pastikan SHA-1 dari debug.keystore yang sama |
| Build failed: google-services.json | Copy file keras ke app/ folder, jangan nested |
| "No matching signature" | Check Web Client ID di strings.xml, exact copy |
| API sync failed | Check internet connection, base URL moneylens.datanex.org |
| App crash at login | Check INTERNET permission AndroidManifest.xml |

---

## ⏱️ Estimated Time

- Firebase Setup: 15 menit
- SHA-1 & Web Client ID: 10 menit
- Build: 5 menit
- Install: 2 menit
- Testing: 5 menit
- Total: **~37 menit**

---

**Start from STEP 1 sekarang! Semua step wajib dilakukan untuk login berfungsi.**

Last Updated: May 17, 2026

