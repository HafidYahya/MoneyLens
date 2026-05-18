# 🔐 LOGIN GOOGLE INTEGRATION - SETUP GUIDE

## 📋 Prerequisites

1. **Firebase Project**
   - Google Firebase Account (https://console.firebase.google.com)
   - Project baru atau existing project

2. **Android Keystore**
   - Untuk production signing key SHA-1
   - Untuk debug: gunakan debug keystore

## 🚀 STEP 1: Setup Firebase Project

### 1.1 Create Firebase Project
```
1. Buka https://console.firebase.google.com
2. Click "Create Project"
3. Enter project name: "MoneyLens"
4. Accept terms, click Create
5. Tunggu project ready (~2 menit)
```

### 1.2 Register Android App
```
1. Di Firebase Console, click "Add app" → "Android"
2. Enter package name: com.app.moneylens
3. Enter SHA-1 fingerprint (lihat di bawah)
4. Click "Register app"
5. Download google-services.json
```

### 1.3 Get SHA-1 Fingerprint

**Debug (untuk testing):**
```bash
# Windows - di PowerShell
cd C:\Users\[USERNAME]\.android
Get-Content debug.keystore | certutil -encode -
```

Atau gunakan Android Studio:
- Open Android Studio
- Go to `Tools` → `App Signing` → `KeyStore` → browse debug.keystore
- Copy SHA-1 hash

**Release (untuk production):**
```bash
# Jika sudah punya release keystore
keytool -list -v -keystore [path_to_keystore] -alias [alias_name] -storepass [password]
```

Copy SHA-1, paste ke Firebase Console.

## 📥 STEP 2: Enable Google Sign-In

### 2.1 Enable Authentication Method
```
1. Di Firebase Console, buka "Authentication"
2. Click "Sign-in method"
3. Click "Google"
4. Toggle "Enable"
5. Pilih support email
6. Click "Save"
```

### 2.2 Download google-services.json
```
1. Di Firebase Console, settings (gear icon)
2. Click "Project settings"
3. Tab "General"
4. Download google-services.json
5. Copy ke: app/google-services.json
```

## 🔧 STEP 3: Setup Kotlin Code

### 3.1 Dapatkan Web Client ID
```
1. Di Firebase Console → Project Settings
2. Tab "Service Accounts"
3. Click "Generate New Private Key"
4. Di file JSON yg di-download, cari "client_id" dengan "type": "web"
5. Copy client_id value
```

### 3.2 Update strings.xml
```xml
<!-- app/src/main/res/values/strings.xml -->
<string name="default_web_client_id">PASTE_WEB_CLIENT_ID_DISINI</string>
```

Contoh:
```xml
<string name="default_web_client_id">123456789-abcdefgh.apps.googleusercontent.com</string>
```

## 📱 STEP 4: Setup Device untuk Testing

### 4.1 Install google-services.json
```
1. Download google-services.json dari Firebase
2. Copy ke: D:\SKRIPSI\APLIKASI\MoneyLens\app\
3. File path: app/google-services.json (WAJIB di app folder, bukan root)
```

### 4.2 Install APK ke Device
```bash
# Build & install
cd D:\SKRIPSI\APLIKASI\MoneyLens
./gradlew clean build
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## ✅ STEP 5: Testing

### 5.1 Test Login Flow
```
1. Buka app MoneyLens
2. Lihat Splash Screen (2 detik)
3. Redirect ke Login Screen
4. Click "Login dengan Google"
5. Pilih akun Google
6. Approve permissions
7. Tunggu sync ke database
8. Should redirect ke MainActivity (camera screen)
```

### 5.2 Test Session Persistence
```
1. Sudah login, tutup app
2. Buka app lagi
3. Should go directly to MainActivity (NO login screen)
4. Session persisten selama tidak uninstall/clear data
```

### 5.3 Test Session Clearing
```
1. Di device, buka Settings → Apps → MoneyLens
2. Click "Clear data"
3. Buka app lagi
4. Should show login screen again
5. Memory cleared = session cleared
```

## 🐛 Troubleshooting

### Error: "Google Sign-In configuration failed"
- ❌ Pastikan SHA-1 sesuai di Firebase Console
- ❌ Pastikan google-services.json di app/ folder
- ✅ Clean build: `./gradlew clean`

### Error: "10: developer_error"
- ❌ Web Client ID salah di strings.xml
- ✅ Cross-check dengan Firebase Console

### Error: "API not enabled"
- ❌ Google Sign-In API belum diaktifkan
- ✅ Firebase setup sudah auto-enable, tapi bisa manual:
  - Go to Google Cloud Console
  - Enable "Google+ API" atau "Identity Toolkit API"

### API Sync Failed
- ❌ Network tidak connect
- ❌ Base URL salah
- ✅ Check logcat: `adb logcat | grep AuthRepository`

## 📊 Database Integration

### User Data yang Disimpan
```json
{
  "firebase_uid": "abc123...",
  "email": "user@gmail.com",
  "display_name": "John Doe",
  "photo_url": "https://...",
  "device_brand": "samsung",
  "device_model": "SM-A546E",
  "android_version": "14",
  "app_version": "1.1",
  "last_sign_in_at": "2026-05-17 10:30:00",
  "last_active_at": "2026-05-17 10:35:00"
}
```

### API Endpoints yang Digunakan
```
POST   /api/moneylens-users          → Create/sync user
PUT    /api/moneylens-users/{id}     → Update last_active_at
```

## 🎯 Architecture Flow

```
SplashActivity
    ↓ (check session)
    ├─→ User logged in? → MainActivity (Camera)
    └─→ Not logged in? → LoginActivity
                ↓
            Google Sign-In
                ↓
            Firebase Auth
                ↓
            Save SessionManager
                ↓
            Sync to API Database
                ↓
            MainActivity
```

## 📚 Documentation Files

- ✅ LOGIN_INTEGRATION.md (this file)
- ✅ API.md (dari server, dokumentasi endpoint)
- ✅ Kode di: auth/SessionManager.kt, auth/AuthRepository.kt

## ⚡ Quick Reference

| Task | Command |
|------|---------|
| Build | `./gradlew clean build` |
| Install | `adb install -r app-debug.apk` |
| Check SHA-1 | Android Studio → Tools → App Signing |
| View Logs | `adb logcat \| grep LoginActivity` |
| Clear Cache | `adb shell pm clear com.app.moneylens` |

---

**Status: ✅ INTEGRATION COMPLETE**

Next: Follow STEP 1-5 di atas, harusnya 30 menit selesai!

