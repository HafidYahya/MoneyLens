# 🧪 QUICK TEST GUIDE - Bug Fixes v1.0.1

**Installation & Testing dalam 10 menit**

---

## ⚡ INSTALL UPDATED APK

```bash
# Hapus app lama
adb uninstall com.app.moneylens

# Install APK baru (dengan fixes)
adb install app/build/outputs/apk/debug/app-debug.apk

# Verify installed
adb shell pm list packages | grep moneylens
```

---

## 🧪 TEST BUG #1: TTS SPAM DETECTION FIX

### What to do:
1. Buka app MoneyLens
2. Tunjukkan Rp 100000 ke kamera
3. **Tahan** uang di depan kamera selama 10 detik **TANPA digerakkan**

### Expected Result:
```
First time (0-1 sec):    "Terdeteksi: Rp 100000" ✓
Time 1-1.5 sec:          SILENSE (waiting for cooldown)
Time 1.5+ sec:           "Terdeteksi: Rp 100000" (if still held) ✓
```

### Success Criteria:
- ✅ Announcement hanya sekali, not spamming
- ✅ Cooldown jelas (1.5 second wait)
- ✅ Audio jelas dan tidak garbled
- ✅ Tidak ada overlap detections

### Fail Indicators:
- ❌ Spam "Terdeteksi Rp 100000" berkali-kali
- ❌ Audio tercampur atau tidak jelas
- ❌ Jika digerakkan sedikit: langsung announce lagi

---

## 🧪 TEST BUG #2: FLASH/TORCH FIX

### What to do:
1. Buka app MoneyLens di area **gelap** atau malam hari
2. **Klik Flash button** di aplikasi
3. Perhatikan lampu belakang device

### Expected Result - PRIMERA:
```
Initial State:     Lampu OFF (torch false)
After 1st click:   Lampu ON + audio "Flash On"
After 2nd click:   Lampu OFF + audio "Flash Off"
After 3rd click:   Lampu ON + audio "Flash On"
```

### Success Criteria:
- ✅ Lampu nyala/mati sesuai klik
- ✅ Audio "Flash On/Off" jelas
- ✅ Tidak ada delay/lag
- ✅ Bisa rapid-click tanpa crash
- ✅ Preview tetap smooth

### Fail Indicators:
- ❌ Lampu tidak menyala
- ❌ Audio muncul tapi lampu tidak
- ❌ Crash setelah beberapa klik
- ❌ Camera preview frozen

---

## 📱 ADVANCED TESTING

### Test Spam Detection dengan Rapid Money Change:
```
1. Show Rp 100000
2. Wait 0.5 sec (no announce yet - too fast)
3. Move away camera
4. Show Rp 50000 immediately
5. Expected: "Terdeteksi: Rp 50000" (different denomination, no spam)
6. Show Rp 50000 lagi immediately
7. Expected: Silence (same denomination within cooldown)
```

### Test Flash + Detection Together:
```
1. Dim lighting, Flash OFF
2. Try detect money - might be low confidence
3. Click Flash ON
4. Try detect again - should have better confidence
5. Audio should be clear "Terdeteksi: Rp [nominal]"
```

### Crash Test:
```
1. Rapid clicking Flash button (20x)
2. While TTS speaking, change camera angle
3. Try rapid denomination change
Expected: No crash, app still responsive
```

---

## 📊 LOGGING & VERIFICATION

### View Logs untuk Bug #1 (Spam):
```bash
adb logcat -v threadtime | grep "MainActivity"

# Harusnya pattern seperti:
# [Time] MainActivity: Detect: Rp 100000, Confidence: 92.5%    ← Detection 1
# [Time] MainActivity: Detect: Rp 100000, Confidence: 91.8%    ← Detection 2 (no TTS)
# [Time] MainActivity: Detected: Rp 100000, Confidence: 93.2%  ← TTS output after cooldown
```

### View Logs untuk Bug #2 (Flash):
```bash
adb logcat -v threadtime | grep "CameraManager"

# Harusnya pattern seperti:
# [Time] CameraManager: Torch: ON
# [Time] CameraManager: Torch: OFF
# [Time] CameraManager: Torch: ON
# (tanpa error messages)
```

---

## ✅ SIGN-OFF CHECKLIST

Print atau screenshot hasil testing:

```
TESTER: ________________    DATE: ________
DEVICE: ________________    

Bug #1 - Spam Detection:
[ ] No spam when holding same denomination
[ ] Cooldown works (1.5 sec between repeat)
[ ] Different denomination: announces correctly
[ ] Audio clear and synchronized
Status: PASS / FAIL

Bug #2 - Flash:
[ ] Flash turns ON with "Flash On" message
[ ] Flash turns OFF with "Flash Off" message
[ ] Multiple rapid clicks work fine
[ ] No crashes
[ ] Preview stays smooth
Status: PASS / FAIL

Overall:
[ ] App stable for 5 minutes continuous use
[ ] No crashes or hangs
[ ] TalkBack still working
[ ] All features responsive
Status: PASS / FAIL

Notes:
_________________________________________

Signed: ____________________
```

---

## 🚀 IF TESTS PASS:

Congratulations! Aplikasi siap untuk:
1. Extended testing
2. Production deployment
3. User distribution

---

## 🐛 IF TESTS FAIL:

**Bug #1 Fails Still:</**
- Spam happening: Check logcat untuk timing issue
- Audio quality: Check device TTS settings
- Cooldown not working: Report exact timing

**Bug #2 Fails Still:**
- Flash not working: Check logcat untuk exception
- Crash on flash click: Report stack trace
- Permission issue: Check device camera settings

Report details ke: [Support Channel]

---

**Next Step:** Run tests dan report hasil! ✅


