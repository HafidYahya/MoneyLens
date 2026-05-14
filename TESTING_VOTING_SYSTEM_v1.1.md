# Quick Test Guide - Voting System v1.1

## 🎯 Testing Instructions

### 1. Install APK
```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### 2. Test Scenarios

#### ✅ Test 1: Normal Money Detection
1. Buka aplikasi MoneyLens
2. Arahkan **Rp 50000** ke kamera (steady hold 2 detik)
3. **Expected:** 
   - "Terdeteksi: Rp 50000" (1x announcement saja)
   - Tidak spam meski tetap arah ke kamera

**Pass Criteria:** Announcement 1x, tepat

---

#### ✅ Test 2: Confidence 85-94% Range (NEW)
1. Arahkan uang dengan **angle sedikit miring** (45°)
2. Confidence akan antara 85-94%
3. **Expected:**
   - Hasil: ANNOUNCE (sebelum skip di 94%)
   - Toggle flash untuk lihat confidence score

**Pass Criteria:** Lebih responsif dari v1.0

---

#### ✅ Test 3: Noise Immunity (NEW VOTING)
1. Arahkan **Rp 100000**
2. Pada frame ke-4 atau 5, flash light atau shake kamera (simulate noise)
3. **Expected:**
   - Voting system: masih detect Rp 100000 (4/5 match)
   - ANNOUNCE "Terdeteksi: Rp 100000"

**Pass Criteria:** Noise pada 1-2 frame tidak mengganggu hasil

---

#### ✅ Test 4: Non Rupiah Detection (NEW CLASS)
1. Arahkan **kertas putih kosong** atau objek non-currency
2. Model detect sebagai "Non Rupiah"
3. **Expected:**
   - UI update: "Confidence: XX%"
   - Status: "Mendeteksi..." atau "Terdeteksi"
   - **NO SOUND** (intentional: non-currency gak perlu diumumkan)

**Pass Criteria:** Non Rupiah tidak trigger TTS

---

#### ✅ Test 5: False Positive Avoidance
1. Arahkan **tepi uang saja** (partial)
2. Confidence <85% pada sebagian besar frames
3. **Expected:**
   - Status: "Mendeteksi..." (no confident announcement)
   - Tidak ada TTS announcement

**Pass Criteria:** Tetap hening sampai confidence cukup

---

#### ✅ Test 6: TalkBack Verification
1. Aktifkan TalkBack (Settings > Accessibility > TalkBack)
2. Tap tombol flash
3. **Expected:**
   - "Flash ON" atau "Flash OFF" (TTS)
   - TalkBack announces state changes

**Pass Criteria:** TalkBack properly integrated

---

#### ✅ Test 7: Flash/Torch Button
1. Di area gelap, tap **Fullsize Flash Button**
2. **Expected:**
   - Lampu menyala/mati sesuai tap
   - "Flash on" / "Flash off" announcement
   - Tombol fullsize (membentang end-to-end)

**Pass Criteria:** Flash responsive, no crashes

---

### 📊 Monitoring Output

**Logcat untuk diagnosis:**
```bash
adb logcat | grep "MainActivity"
```

**Output contoh:**
```
D/MainActivity: Prediction: Rp 50000, History: [FramePrediction(Rp 50000, 87.5), FramePrediction(Rp 50000, 88.2), ...]
D/MainActivity: Consensus: Rp 50000
D/MainActivity: Detected via Voting: Rp 50000, Confidence: 88.1%
```

---

### 🐛 Troubleshooting

**Problem:** Deteksi spam (multiple announce)  
**Solution:** Voting system should prevent ini. Jika masih terjadi:
- Check logcat untuk prediksi history
- Verify MIN_CONSENSUS = 4
- Verify debounceMs timing

**Problem:** Terlalu lama untuk announce  
**Solution:** Voting system needs ~5 frames (~170ms @ 30fps):
- Normal behavior saat awal app start
- Jika >1 detik, cek frame rate camera

**Problem:** Non Rupiah announce suara  
**Solution:** Check line di processFrame():
```kotlin
if (consensus != null && result.isConfident && consensus != "Non Rupiah") {
```
Pastikan ada `&& consensus != "Non Rupiah"`

---

### ✨ Performance Expectations

| Metrik | Expected |
|--------|----------|
| First detection | ~1-2 detik (5 frame voting) |
| Repeat detection | ~1.5 detik (debounce) |
| Flash response | <100ms |
| TTS latency | ~100-200ms |
| CPU usage | ~40-60% |

---

### 📝 Test Report Template

```
Date: _______________
Device: _______________
Android Version: _______________

✓ Test 1 - Normal Detection: [PASS/FAIL]
✓ Test 2 - 85-94% Confidence: [PASS/FAIL]
✓ Test 3 - Noise Immunity: [PASS/FAIL]
✓ Test 4 - Non Rupiah: [PASS/FAIL]
✓ Test 5 - False Positive: [PASS/FAIL]
✓ Test 6 - TalkBack: [PASS/FAIL]
✓ Test 7 - Flash Button: [PASS/FAIL]

Notes:
_______________________________________________________________________________

Issues Found:
_______________________________________________________________________________
```

---

## 🎉 Success Criteria

✅ All 7 tests PASS  
✅ No crashes during testing  
✅ Audio output clear  
✅ Flash button works  
✅ TalkBack integrated  

**→ Ready for production deployment!**

---

**Version:** 1.1  
**Last Updated:** May 13, 2026  
**Status:** Ready for QA Testing

