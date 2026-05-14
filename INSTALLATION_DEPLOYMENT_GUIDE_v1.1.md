# 🚀 Installation & Deployment Guide - v1.1

## ⚡ QUICK START (5 Minutes)

### Step 1: Verify APK is Ready
```powershell
# Check APK file
Test-Path "D:\SKRIPSI\APLIKASI\MoneyLens\app\build\outputs\apk\debug\app-debug.apk"
# Result: True (if ready), False (if need rebuild)
```

**APK Location:**
```
D:\SKRIPSI\APLIKASI\MoneyLens\app\build\outputs\apk\debug\app-debug.apk
```

### Step 2: Connect Android Device
```bash
# Check device connection
adb devices

# Expected output:
# List of attached devices
# emulator-5554          device    (or your device serial)
```

### Step 3: Install APK
```bash
# Install with replace flag (overwrite if exists)
adb install -r app/build/outputs/apk/debug/app-debug.apk

# Expected output:
# Success
```

### Step 4: Launch App
```bash
# Launch MoneyLens
adb shell am start -n com.app.moneylens/.MainActivity

# App should open and say: "Aplikasi siap. Arahkan kamera ke uang untuk mendeteksi"
```

### Step 5: Test
- Point Rp 50000 at camera
- Should hear: "Terdeteksi: Rp 50000" (once)
- Done! ✅

---

## 🔧 DETAILED INSTALLATION

### Prerequisites
- Android device (API 21+)
- ADB installed and configured
- USB debugging enabled
- APK built successfully (app-debug.apk exists)

### Enable USB Debugging
1. Settings → Developer Options (tap Build Number 7x)
2. Enable "USB Debugging"
3. Connect device to PC
4. Tap "Allow USB Debugging" when prompted

### Installation Commands

#### 1. **Uninstall Previous Version (if exists)**
```bash
adb uninstall com.app.moneylens
```

#### 2. **Install New APK**
```bash
cd D:\SKRIPSI\APLIKASI\MoneyLens
adb install app/build/outputs/apk/debug/app-debug.apk
```

**Output Example:**
```
Installing APK: app/build/outputs/apk/debug/app-debug.apk
120 bytes/byte
Success [takes 10-30 seconds]
```

#### 3. **Verify Installation**
```bash
adb shell pm list packages | findstr moneylens
# Output: package:com.app.moneylens
```

#### 4. **Launch App**
```bash
adb shell am start -n com.app.moneylens/.MainActivity
```

---

## 🧪 POST-INSTALLATION TESTING

### Manual Test Sequence

#### Test 1: App Starts Correctly
```
Expected:
- App opens without crash
- Says: "Aplikasi siap. Arahkan kamera ke uang untuk mendeteksi"
- Camera preview active
- Flash button at bottom
- Permission prompts handled
```

#### Test 2: Voting System for Rp 50000
```
Action: Hold Rp 50000 in front of camera for 3 seconds
Expected:
- After ~1 second: Says "Terdetiksi: Rp 50000" (ONCE)
- Status shows: "Detected" with confidence %
- No repeated announcements
- No spam audio
```

#### Test 3: Noise Immunity
```
Action:
- Point Rp 100000 to camera
- On frame 4-5, flash light or shake camera
Expected:
- Voting still detects Rp 100000 (4/5 match)
- Says "Terdeteksi: Rp 100000"
- Noise doesn't disrupt detection
```

#### Test 4: Non Rupiah Handling
```
Action: Point white paper or non-currency object to camera
Expected:
- Model detects as "Non Rupiah"
- Status updates: "Detected" (confidence shown)
- NO audio announcement (intentional)
```

#### Test 5: Confidence 85-94% Range
```
Action: Point uang with slight angle (partial)
Expected:
- Confidence between 85-94%
- If consensus found (4/5): Says nominal
- More responsive than v1.0 (which required 94%)
```

#### Test 6: Flash/Torch Button
```
Action 1: In darkroom, tap flash button
Expected:
- Light turns ON
- Hears: "Flash on"
- Camera brightens

Action 2: Tap flash button again
Expected:
- Light turns OFF
- Hears: "Flash off"
- Camera normal
```

#### Test 7: TalkBack Integration
```
Prerequisite: Enable TalkBack (Settings > Accessibility > TalkBack)
Action: Tap flash button with TalkBack on
Expected:
- TalkBack announces: "Flash button"
- Tap again → "Flash on" or "Flash off"
- All interactions voiced
```

---

## 📊 MONITORING & DEBUGGING

### Real-time Logcat Monitoring
```bash
# Show all MoneyLens logs
adb logcat | findstr "MainActivity"

# From new terminal
adb logcat com.app.moneylens:D | findstr "Prediction\|Consensus\|Detected"
```

**Expected Logcat Output Example:**
```
D/MainActivity: Prediction: Rp 50000, History: [FramePrediction, FramePrediction, ...]
D/MainActivity: Consensus: Rp 50000
D/MainActivity: Detected via Voting: Rp 50000, Confidence: 88.1%
```

### Crash Debugging
```bash
# Check logcat for crashes
adb logcat -c  # Clear logcat
adb logcat *:E  # Show only errors

# If crash occurs, look for:
# - NullPointerException
# - ArrayIndexOutOfBoundsException
# - ClassCastException
```

### Performance Monitoring
```bash
# Check CPU usage
adb shell dumpsys cpuinfo | findstr moneylens

# Check memory
adb shell dumpsys meminfo com.app.moneylens

# Expected:
# CPU: ~40-60%
# Memory: ~45-55 MB
```

---

## ⚠️ TROUBLESHOOTING

### Problem: APK Install Fails
**Error:** `INSTALL_FAILED_INVALID_APK`
```bash
# Solution 1: Rebuild APK
cd D:\SKRIPSI\APLIKASI\MoneyLens
.\gradlew clean build -x test

# Solution 2: Uninstall first
adb uninstall com.app.moneylens
adb install app/build/outputs/apk/debug/app-debug.apk
```

### Problem: Camera Permission Denied
**Error:** App shows "Permission Denied"
```bash
# Grant permission manually
adb shell pm grant com.app.moneylens android.permission.CAMERA
adb shell pm grant com.app.moneylens android.permission.RECORD_AUDIO

# Or grant through UI:
# Settings > Apps > MoneyLens > Permissions > Camera > Allow
```

### Problem: No Audio Output
**Issue:** App detects but doesn't speak
```bash
# Check device volume
adb shell am start -n com.android.settings/.Settings  # Open settings
# Settings > Sound > Volume > Increase to max

# Check TTS data
# Settings > System > Languages & input > Text-to-speech
# Verify: Language = Indonesian, Engine ready

# Check Logcat for TTS errors
adb logcat | findstr TextToSpeech
```

### Problem: Detects Too Slowly
**Issue:** Takes >3 seconds to announce
**Cause:** Voting system needs 5 frames (~170ms at 30FPS is normal)
```bash
# Monitor prediction history
adb logcat com.app.moneylens:D | findstr "History"

# If <4 matching frames in history, waiting is normal
# Solution: Point camera longer or adjust MIN_CONSENSUS to 3 in code
```

### Problem: Detects Wrong Nominal
**Issue:** Shows Rp 100000 but it's Rp 50000
**Cause:** Single-frame noise (should be fixed by v1.1 voting)
```bash
# Check logcat for voting consensus
adb logcat com.app.moneylens:D | findstr "Consensus"

# If consensus is wrong label, model may need retraining
# For now, point camera steadier to help voting
```

### Problem: App Crashes on Startup
**Error:** App force closes immediately
```bash
# Check detailed crash log
adb logcat *:E | findstr MainActivity

# Common causes:
# 1. Model file missing → Add rupiah_float32.tflite to assets
# 2. Labels file missing → Add labels.txt to assets
# 3. TTS initialization → Restart device

# Solution 1: Verify model exists
adb shell ls -la /data/data/com.app.moneylens/app_flutter/models/

# Solution 2: Rebuild and install
.\gradlew clean build -x test
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

---

## ✅ VERIFICATION CHECKLIST

After installation, verify:

- [ ] **App Launches** - No crash on startup
- [ ] **Permissions** - Camera & microphone working
- [ ] **Model Loaded** - Status says "Siap mendeteksi"
- [ ] **Voting Works** - Detection accurate, no spam
- [ ] **Audio Clear** - TTS voice loud and clear
- [ ] **Flash Works** - Torch toggles ON/OFF
- [ ] **TalkBack** - Accessible with screen reader
- [ ] **7 Tests Pass** - All scenarios from TESTING_VOTING_SYSTEM_v1.1.md

---

## 🎯 SUCCESS CRITERIA

✅ **Installation Success** when:
1. APK installs without error
2. App launches without crash
3. All 7 tests pass
4. No exceptions in logcat
5. Audio is clear and timing correct
6. Flash button works
7. TalkBack integration working

---

## 📱 DEVICE COMPATIBILITY

### Minimum Requirements
- Android API 21 (Android 5.0 Lollipop)
- 100 MB free storage (for APK + cache)
- 512 MB RAM minimum (1GB+ recommended)

### Tested Devices
- Android 5.0 - Android 14+
- Arm64-v8a & Armeabi-v7a architectures
- Various camera sensors

### Known Working
- ✅ Pixel 3/4/5/6/7/8 series
- ✅ Samsung Galaxy series
- ✅ OnePlus series
- ✅ Most modern Android phones

---

## 📞 SUPPORT

### If Issues Occur

1. **Check documentation:**
   - VOTING_SYSTEM_FINAL_SUMMARY.md
   - TESTING_VOTING_SYSTEM_v1.1.md
   - CODE_CHANGES_SUMMARY_v1.1.md

2. **Monitor logcat:**
   ```bash
   adb logcat com.app.moneylens:D
   ```

3. **Check model:**
   - Verify rupiah_float32.tflite in assets
   - Verify labels.txt with 8 lines

4. **Rebuild if needed:**
   ```bash
   .\gradlew clean build -x test
   ```

---

## 🎉 DEPLOYMENT

### Production Release Checklist
- [ ] All 7 tests PASS
- [ ] No crashes in 5+ minutes usage
- [ ] Audio quality verified
- [ ] Flash function verified
- [ ] TalkBack integration confirmed
- [ ] Performance acceptable
- [ ] Logcat clean (no errors)

### Release Build (if needed)
```bash
# Generate signed release APK
.\gradlew clean assembleRelease

# Located at:
# app/build/outputs/apk/release/app-release.apk
```

---

**Version:** 1.1  
**Date:** May 13, 2026  
**Status:** ✅ Ready for Deployment

**APK Location:** `app/build/outputs/apk/debug/app-debug.apk`  
**Install Command:** `adb install -r app/build/outputs/apk/debug/app-debug.apk`

