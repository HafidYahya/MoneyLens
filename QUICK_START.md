# QUICK START - MoneyLens

Panduan cepat untuk memulai menggunakan MoneyLens.

## ⚡ 5 Langkah Cepat

### Langkah 1: Persiapkan Model
✅ Model sudah ada: `app/src/main/assets/rupiah_float32.tflite`

**Spesifikasi Model:**
- Input: 224x224 RGB (float32, normalized 0-1)
- Output: 7 kelas (Rp 1000 - Rp 100000)
- Framework: MobileNetV2 Transfer Learning
- Format: TFLite (float32)

### Langkah 2: Siapkan Labels
✅ Labels sudah ada: `app/src/main/assets/labels.txt`
```
Rp 1000
Rp 2000
Rp 5000
Rp 10000
Rp 20000
Rp 50000
Rp 100000
```

**VERIFIED:** Urutan label sesuai dengan output indices model!

### Langkah 3: Build APK
```bash
cd D:\SKRIPSI\APLIKASI\MoneyLens
.\gradlew clean build

# Generate APK untuk testing
.\gradlew assembleDebug
```

APK akan tersimpan di: `app/build/outputs/apk/debug/app-debug.apk`

### Langkah 4: Install ke Device/Emulator
```bash
# Install debug APK
adb install app/build/outputs/apk/debug/app-debug.apk

# Atau dari Android Studio:
# Run → Select Device → OK
```

### Langkah 5: Testing
1. Buka aplikasi MoneyLens
2. Beri izin Camera saat diminta
3. Tunjukkan uang ke kamera
4. Dengarkan hasil deteksi via speaker

---

## 🔧 Verify Build Success

Jika build berhasil, Anda akan melihat:
```
BUILD SUCCESSFUL in X.XXs
```

Jika ada error, check:
- ✅ Model file ada di `app/src/main/assets/rupiah_float32.tflite` ✓ VERIFIED
- ✅ Labels file ada di `app/src/main/assets/labels.txt` ✓ VERIFIED
- ✅ Gradle sync berhasil
- ✅ Android SDK version compatible

---

## 📱 Device Requirements

- **OS:** Android 10 (API 29) atau lebih tinggi
- **RAM:** Minimum 2GB (recommended 3GB+)
- **Storage:** Minimum 100MB
- **Camera:** Rear camera wajib

**Test Devices:**
```
✅ Pixel 4+
✅ Samsung Galaxy A10+
✅ Redmi Note 9+
✅ Any Android 10+ device dengan Camera2 API
```

---

## 🎙️ Testing TTS & Accessibility

### Test Text-to-Speech:
```bash
# Open app dan perhatikan voice output ini:
# 1. "Aplikasi siap. Arahkan kamera ke uang untuk mendeteksi"
# 2. "Terdeteksi: [hasil deteksi]" (saat confidence >= 85%)
# 3. "Flash On/Flash Off" (saat klik tombol flash)
```

### Test TalkBack:
1. Go to Settings → Accessibility → TalkBack
2. Turn ON TalkBack
3. Use MoneyLens dengan screen reader:
   - 3-finger swipe right = next
   - 3-finger swipe left = previous
   - Double tap = activate

---

## 📊 Model Requirements Check

✅ **Model Anda sudah verified:**

```
Model: Rupiah Classifier v1.0
Architecture: MobileNetV2 + Transfer Learning

Input:
- Shape: [1, 224, 224, 3]
- Format: float32
- Normalization: pixel / 255.0 (range 0-1)
- Color Space: RGB

Output:
- Shape: [1, 7]
- Type: Softmax probabilities
- Classes: 7 (Rp 1000 - Rp 100000)
- Threshold Recommended: 85%

Preprocessing:
1. Resize image to 224x224
2. Convert to RGB
3. Normalize pixel values: pixel / 255.0f
4. Extract RGB channels in order
```

**Aplikasi sudah dikonfigurasi untuk model ini!**

---

## 🚀 Run on Emulator

Jika tidak ada device fisik:

```bash
# Start Android Emulator terlebih dahulu
# Dari Android Studio: AVD Manager → launch emulator

# Build dan run
.\gradlew installDebug

# Atau gunakan Android Studio GUI:
# Run → Select emulator → OK
```

**Note:** Emulator camera menggunakan webcam komputer Anda.

---

## 📝 Model Conversion (Jika ada model format lain)

Jika Anda memiliki model dalam format selain TFLite, ikuti panduan ini untuk convert:

### Dari SavedModel ke TFLite
```python
import tensorflow as tf

# Load SavedModel
saved_model_dir = "path/to/saved_model"
converter = tf.lite.TFLiteConverter.from_saved_model(saved_model_dir)

# Optional: Enable optimization
converter.optimizations = [tf.lite.Optimize.DEFAULT]

# Convert
tflite_model = converter.convert()

    # Save
    with open("rupiah_float32.tflite", "wb") as f:
        f.write(tflite_model)

    print("✅ Model berhasil diconvert ke .tflite")
```

### Dari H5/Keras
```python
import tensorflow as tf

# Load model
model = tf.keras.models.load_model("model.h5")

# Convert
converter = tf.lite.TFLiteConverter.from_keras_model(model)
tflite_model = converter.convert()

    # Save
    with open("rupiah_float32.tflite", "wb") as f:
        f.write(tflite_model)
```

---

## 🔐 Permissions

Aplikasi memerlukan:

```xml
1. CAMERA          - Akses kamera device
2. RECORD_AUDIO    - Untuk TTS engine
3. INTERNET        (optional) - Jika ada online features
```

Permissions akan diminta saat first run. Berikan semua izin untuk full functionality.

---

## 🐛 Debug Mode

### Lihat detailed logs:
```bash
adb logcat -v threadtime | grep "MoneyLens"
```

### Check detection details:
```bash
adb logcat | grep "Detected:"
# Output: Detected: Uang 100000 Rupiah, Confidence: 92.5%
```

### Monitor Performance:
```bash
adb shell dumpsys meminfo com.app.moneylens
```

---

## 📦 APK Size

Expected APK size:
- Debug APK: ~150-200MB (termasuk TFLite + GPU)
- Release APK: ~100-150MB (dengan optimization)

Besar karena TensorFlow Lite libraries & model file.

---

## ✅ Checklist Sebelum Production

- [ ] Model file sudah ditesting (accuracy OK?)
- [ ] Labels sesuai dengan model output
- [ ] APK bisa install tanpa error
- [ ] Camera feed berjalan lancar
- [ ] TTS berbunyi dengan jelas
- [ ] Confidence threshold sesuai requirement (85%)
- [ ] Flash toggle berfungsi
- [ ] TalkBack compatible
- [ ] Tested di multiple devices
- [ ] Battery drain acceptable (<10% per hour)

---

## 🆘 Common Issues & Solutions

| Issue | Solution |
|-------|----------|
| APK won't install | Clear gradle cache: `./gradlew clean` |
| Camera shows black | Check permissions, try reboot device |
| TTS silent | Check volume settings, restart TTS engine |
| Model error at runtime | Check file paths in assets/ folder |
| Low FPS | Reduce model size atau use quantized version |
| App crash on startup | Check logcat untuk stack trace |

---

## 📞 Support

Untuk troubleshooting lebih lanjut, lihat:
- ➜ `README.md` - Full documentation
- ➜ `SETUP_GUIDE.md` - Detailed model setup
- ➜ `IMPLEMENTATION_DETAILS.md` - Architecture & implementation
- ➜ Logcat output - Debug information

---

## 🎉 You're Ready!

Selamat! Aplikasi Anda siap untuk:
✅ Deteksi uang secara realtime  
✅ Output hasil via Text-to-Speech  
✅ Support untuk tunanetra dengan TalkBack  
✅ Kontrol flashlight untuk kondisi gelap  

**Happy Money Detection! 💰🎤**

