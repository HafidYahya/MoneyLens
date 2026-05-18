# 🧪 LOGIN & CAMERA - QUICK TEST GUIDE

Panduan cepat untuk testing aplikasi MoneyLens dengan login + camera.

---

## 🎯 PRE-REQUISITES

- ✅ Firebase setup done (FIREBASE_SETUP_CHECKLIST.md completed)
- ✅ APK built & installed
- ✅ Device punya internet connection
- ✅ Google account for testing (bisa akun personal)
- ✅ Uang rupiah fisik untuk test detection

---

## 📱 TEST 1: First Time Login (5 menit)

### Test Objective
Verify aplikasi dapat login dengan Google account dan sync ke database.

### Steps
```
1. Buka app MoneyLens
   └─ Lihat: Splash screen "MoneyLens" dgn loading
   └─ Duration: 2 detik

2. Auto redirect ke Login Screen
   └─ Lihat: Button "Login dengan Google"

3. Click button "Login dengan Google"
   └─ Pop-up Google Sign-In muncul

4. Select Google Account
   └─ Pilih akun Google Anda
   └─ Atau login dgn akun baru

5. Review Google Permissions
   └─ Click "Allow" / "Approve"

6. Wait for sync to database
   └─ Lihat status: "Menyimpan data ke server..."
   └─ Duration: ~3-5 detik

7. Auto redirect to MainActivity
   └─ Lihat: Camera live preview
   └─ Lihat: Flash button di bawah
   └─ Lihat: Status text di atas
   └─ Lihat: Confidence text

✅ TEST PASS: Jika langsung ke camera, tidak ada error
```

### Expected Behavior
- Splash 2 detik (normal)
- Login smooth, no crashes
- Sync success, tidak error message
- Camera langsung hidup
- Ready to detect

### Failure Scenarios
- ❌ Crash at login: Check FirebaseUtility
- ❌ API error: Check internet, check base URL
- ❌ Stuck at "Menyimpan...": Check logcat → AuthRepository
- ❌ Camera tidak hidup: Check CAMERA permission

---

## 🔄 TEST 2: Session Persistence (5 menit)

### Test Objective
Verify aplikasi remember session setelah close & reopen.

### Steps
```
1. App already logged in (dari TEST 1)
   └─ Camera preview visible

2. Close app
   └─ Swipe dari recent apps, atau tutup manual

3. Tunggu 5 detik

4. Buka app lagi
   └─ Lihat: Splash 2 detik (sama seperti startup)

5. After splash
   └─ Should go DIRECTLY to MainActivity (camera)
   └─ NO login screen
   └─ Camera langsung live

✅ TEST PASS: Jika langsung ke camera, session persisted OK
```

### Expected Behavior
- Splash visible (sebagai transition)
- Auto redirect to camera, no login needed
- Camera langsung functional

### Failure Scenarios
- ❌ Login screen muncul: Session tidak tersimpan di SharedPreferences
- ❌ Crash: Check SessionManager
- ❌ Slow redirect: Check MainActivity.onCreate delay

---

## 🔐 TEST 3: Session Clear & Re-login (5 menit)

### Test Objective
Verify session dapat di-clear & user bisa login ulang dengan akun berbeda.

### Steps
```
1. Device Settings → Apps → MoneyLens
   └─ Di search bar, type "MoneyLens"
   └─ Click app

2. Click "Storage or Permissions"
   └─ Find option "Clear Data" atau "Clear Storage"

3. Click "Clear Data"
   └─ Confirm dialog
   └─ App akan close otomatis

4. Buka MoneyLens app lagi
   └─ Lihat: Splash screen

5. Should redirect to LoginActivity (NOT camera)
   └─ Session cleared, back to login

6. Login dengan akun Google BERBEDA (atau same)
   └─ Ikuti steps TEST 1

✅ TEST PASS: Jika kembali ke login screen, session management OK
```

### Expected Behavior
- Session cleared after "Clear Data"
- Login screen muncul (fresh start)
- Bisa login dengan akun berbeda

### Failure Scenarios
- ❌ Camera muncul: Session tidak ter-clear
- ❌ Crash: Check SessionManager.clearSession()

---

## 📸 TEST 4: Camera & Detection (10 menit)

### Test Objective
Verify camera & money detection bekerja (voting system + TTS).

### Prerequisites
- Punya uang rupiah kertas (Rp 10000, Rp 50000, atau nilai lain)

### Steps
```
1. Already at MainActivity (camera view)

2. Prepare uang rupiah di tangan

3. AIMING mode (bukan deteksi yet)
   - Point camera ke uang
   - Tunggu voting system collect 4 dari 5 frame
   - Status text: "Memindai... (3/5 frame)" atau similar
   - Confidence text: menampilkan confidence value

4. DETECTION mode (voting complete)
   - Tunggu sampai 4 frame sama
   - Lihat status text change ke "Terdeteksi: Rp [nominal]"
   - Dengarkan: TTS output "Terdeteksi Rp [nominal]"
   - Duration: sekali saja, tidak spam

5. Move camera away / change angle
   - Status reset: kembali ke "Memindai..."
   - Tunjukkan uang lain
   - Repeat detection

✅ TEST PASS: Jika bisa deteksi multiple times tanpa spam
```

### Expected Behavior
- Camera real-time, smooth
- Voting system accumulate 4/5 frames
- Detection output suara 1x, tidak spam
- Dapat deteksi multiple object
- Flash button responsive

### Failure Scenarios
- ❌ Camera tidak live: Check CameraManager
- ❌ Voting system tidak bekerja: Model tidak loading
- ❌ TTS spam: debounce mechanism error
- ❌ Flash tidak hidup: Check torch permission

---

## 🔦 TEST 5: Flash Control (3 menit)

### Test Objective
Verify flash/torch button berfungsi di kondisi gelap.

### Steps
```
1. Already at MainActivity

2. Area gelap (tutup semua lampu, atau di malam hari)
   - Camera barely visible

3. Click "Flash" button di bawah
   - Lihat: Lampu belakang device nyala (bright)
   - Lihat: Camera preview jadi bright
   - Dengarkan: TTS "Flash dinyalakan"

4. Click "Flash" button lagi
   - Lihat: Lampu belakang device mati
   - Lihat: Camera preview kembali gelap
   - Dengarkan: TTS "Flash dimatikan"

5. Repeat on/off 3x

✅ TEST PASS: Jika flash toggle smooth & TTS output
```

### Expected Behavior
- Flash ON: Lampu fisik nyala, camera preview bright
- Flash OFF: Lampu fisik mati, camera preview normal
- TTS announce setiap toggle
- Button responsive

### Failure Scenarios
- ❌ Lampu tidak nyala: Check torch permission
- ❌ TTS tidak output: Check TextToSpeechManager
- ❌ Button unresponsive: Check click listener

---

## 🔊 TEST 6: TalkBack Accessibility (5 menit)

### Test Objective
Verify aplikasi compatible dengan TalkBack for blind users.

### Steps
```
1. Device Settings → Accessibility → TalkBack
   - Click "TalkBack"
   - Toggle "Enable" ON
   - Confirm dialog

2. Back to MoneyLens app
   - Same position (MainActivity or LoginActivity)

3. Point finger to elements
   - Status text → Should announce text content
   - Flash button → Should announce "Flash button, toggle flash"
   - Camera → Should announce "Live feed camera"

4. Triple tap (if enabled) → Perform action
   - Flash button → Toggle flash
   - Login button → Trigger login

✅ TEST PASS: Jika TalkBack announce semua element dengan jelas
```

### Expected Behavior
- TTS readout semua UI elements
- Button actions clear & accessible
- Status updates announced

### Failure Scenarios
- ❌ No announcement: contentDescription missing
- ❌ Announcement tidak jelas: Wrong text

---

## 📊 TEST 7: Database Verification (Optional, 5 menit)

### Test Objective
Verify user data tersimpan di database MySQL.

### Prerequisites
- Access ke MySQL server (phpMyAdmin atau client lain)
- Database moneylens_users exist

### Steps
```
1. Open MySQL client
   - Connect ke: http://moneylens.datanex.org (atau phpMyAdmin)

2. Query:
   SELECT * FROM moneylens_users 
   WHERE email = '[akun_login_anda]'
   ORDER BY created_at DESC LIMIT 1;

3. Verify columns:
   ✓ firebase_uid → Match dgn Firebase UID (dari app logs)
   ✓ email → Match dgn login account
   ✓ display_name → Match dgn Google account name
   ✓ device_brand → Match device (samsung, xiaomi, etc)
   ✓ device_model → Device model
   ✓ android_version → Android OS version (14, 13, etc)
   ✓ app_version → App version (1.1+)
   ✓ last_sign_in_at → Recent timestamp
   ✓ last_active_at → Recent timestamp

✅ TEST PASS: Jika semua data match & timestamp fresh
```

### Expected Behavior
- Record exist di database
- All fields populated correctly
- Timestamp recent (within last 5 minutes)

### Failure Scenarios
- ❌ Record not found: API sync failed
- ❌ Old timestamp: Update mechanism fail

---

## 📋 FINAL CHECKLIST

After all tests above, verify these:

- [ ] TEST 1: First login successful
- [ ] TEST 2: Session persistence works
- [ ] TEST 3: Session clear & re-login works
- [ ] TEST 4: Camera & detection working
- [ ] TEST 5: Flash toggle working
- [ ] TEST 6: TalkBack compatible (if applicable)
- [ ] TEST 7: Database data correct (optional)

---

## 📊 Summary

| Test | Duration | Status |
|------|----------|--------|
| First Login | 5 min | ✅ |
| Session Persistence | 5 min | ✅ |
| Session Clear | 5 min | ✅ |
| Camera Detection | 10 min | ✅ |
| Flash Control | 3 min | ✅ |
| TalkBack (optional) | 5 min | ✅ |
| DB Verification (optional) | 5 min | ✅ |
| **TOTAL** | **~38 min** | **✅** |

---

## 🚨 Troubleshooting

### Common Issues

**Crash at Startup**
```
Error: "Could not find org.jetbrains.kotlin"
Solution: ./gradlew clean build --no-daemon
```

**Camera tidak visible**
```
Error: Blank screen
Solution: Check CAMERA permission granted
Check logcat: adb logcat | grep CameraManager
```

**Login button unresponsive**
```
Error: Button click tidak work
Solution: Check google-services.json valid
Check Web Client ID di strings.xml correct
```

**TTS tidak output**
```
Error: No sound saat deteksi
Solution: Check device volume not muted
Check TextToSpeechManager initialized
```

**API sync failed**
```
Error: "Error sync database: Network error"
Solution: Check internet connected
Check API base URL correct: http://moneylens.datanex.org/api/
```

---

## 💡 Tips

1. **Always check logcat for errors:**
   ```bash
   adb logcat | grep "E/MoneyLens\|E/Login\|E/Camera"
   ```

2. **Clear app data between tests:**
   ```bash
   adb shell pm clear com.app.moneylens
   ```

3. **Reinstall APK if issues:**
   ```bash
   adb uninstall com.app.moneylens
   adb install app-debug.apk
   ```

4. **Check permissions:**
   ```bash
   adb shell pm list permissions -d
   ```

---

**Happy Testing! 🎉**

If all 7 tests pass, aplikasi siap untuk production deployment!

Last Updated: May 17, 2026

