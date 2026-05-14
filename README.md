# MoneyLens - Pendeteksi Uang untuk Tunanetra

Aplikasi Android untuk mendeteksi nilai uang menggunakan Computer Vision dan Text-to-Speech, dirancang khusus untuk pengguna tunanetra.

## Fitur Utama

✅ **Deteksi Uang Realtime** - Kamera berjalan terus-menerus tanpa perlu klik tombol  
✅ **Output Suara TTS** - Informasi disalurkan melalui Text-to-Speech dalam bahasa Indonesia  
✅ **Support TalkBack** - Kompatibel penuh dengan accessibility screen reader  
✅ **Lampu Flash** - Fitur lampu berkedip untuk kondisi cahaya rendah  
✅ **Confidence Threshold** - Hanya mengeluarkan suara ketika confidence >= 85%  

## Requirement Sistem

- Android 10 (API 29) atau lebih tinggi
- TensorFlow Lite Model (.tflite format)
- Labels file untuk model

## Setup dan Instalasi

### 1. Persiapan Model TensorFlow Lite

Model CNN Anda harus dalam format TFLite. Jika Anda memiliki model dalam format lain, gunakan TensorFlow Lite Converter.

**File yang dibutuhkan:**

```
app/src/main/assets/
├── money_model.tflite          # Model CNN Anda
└── labels.txt                   # File labels (satu label per baris)
```

#### Cara membuat `labels.txt`:
```
Uang 1000
Uang 2000
Uang 5000
Uang 10000
Uang 20000
Uang 50000
Uang 100000
```

### 2. Men-copy Model dan Labels ke Project

```bash
# Buat folder assets jika belum ada
mkdir -p app/src/main/assets

# Copy model dan labels
cp your_model.tflite app/src/main/assets/money_model.tflite
cp your_labels.txt app/src/main/assets/labels.txt
```

### 3. Build dan Run

```bash
# Sync gradle
./gradlew build

# Install ke device/emulator
./gradlew installDebug

# Atau jalankan melalui Android Studio
```

## Arsitektur Aplikasi

```
com.app.moneylens/
├── MainActivity.kt                 # Activity utama
├── ml/
│   └── TFLiteModelManager.kt       # Manager untuk TensorFlow Lite inference
├── camera/
│   └── CameraManager.kt            # Manager untuk kamera dengan CameraX
├── tts/
│   └── TextToSpeechManager.kt      # Manager untuk Text-to-Speech
└── utils/
    └── PermissionManager.kt        # Utility untuk permission handling
```

## Alur Kerja Aplikasi

1. **Initialization**
   - Request permissions (Camera, Microphone, Flashlight)
   - Load TensorFlow Lite model
   - Initialize Camera2 API dengan CameraX
   - Initialize Text-to-Speech engine

2. **Real-time Detection**
   - Kamera capture frame secara kontinyu
   - Setiap frame diproses oleh model TFLite
   - Hasil deteksi disimpan (label & confidence)

3. **Confidence-based Output**
   - Jika confidence >= 85%: Suara mengeluarkan hasil deteksi
   - Jika confidence < 85%: Aplikasi tetap silent (hanya mendeteksi)

4. **Accessibility**
   - Output teks di layar (untuk screen reader)
   - TalkBack support pada semua elemen interaktif
   - Deskripsi konten untuk semua view

## Kontrol Aplikasi

### Tombol Flash
- **Tekan**: Nyalakan/Matikan lampu flash
- **Audio Feedback**: Aplikasi akan mengumumkan status flash melalui TTS

### Fitur Aksesibilitas
- **Double Tap**: Pada area mana pun untuk mengaktifkan flash dengan jari
- **TalkBack**: Swipe ke kanan untuk navigasi, ke bawah untuk aksi

## Modifikasi Model Input

Jika model Anda memiliki input size yang berbeda dari default (224x224), edit file `TFLiteModelManager.kt`:

```kotlin
private const val IMAGE_INPUT_SIZE = 224  // Ubah sesuai model Anda
private const val CONFIDENCE_THRESHOLD = 85f
```

Jika model Anda menggunakan normalisasi yang berbeda:

```kotlin
private const val IMAGE_MEAN = 127.5f      // Ubah sesuai model Anda
private const val IMAGE_STD = 127.5f       // Ubah sesuai model Anda
```

## Troubleshooting

### Kamera Tidak Muncul
- Pastikan izin CAMERA diberikan
- Cek apakah device memiliki kamera belakang
- Lihat log: `adb logcat | grep MainActivity`

### Model Tidak Terbaca
- Pastikan file `.tflite` berada di `app/src/main/assets/`
- Periksa nama file sesuai dengan kode (default: `money_model.tflite`)
- Verifikasi format model: harus TensorFlow Lite format, bukan SavedModel

### TTS Tidak Berbunyi
- Pastikan izin RECORD_AUDIO diberikan
- Cek level volume device
- Pastikan TTS engine tersedia di system settings

### Confidence Score Terlalu Rendah
- Model perlu di-train ulang dengan data yang lebih baik
- Pastikan preprocessing input image sesuai dengan training
- Cek normalisasi image di TFLiteModelManager

## Konfigurasi Advanced

### Mengubah Bahasa TTS
Edit `TextToSpeechManager.kt`:

```kotlin
tts?.setLanguage(Locale("en", "US"))  // Untuk English
```

### Mengubah Confidence Threshold
Edit `TFLiteModelManager.kt`:

```kotlin
private const val CONFIDENCE_THRESHOLD = 85f  // Ubah nilai di sini
```

### Mengubah Model Input Resolution
Edit `TFLiteModelManager.kt`:

```kotlin
// Bandingkan dengan output shape model Anda
inputImageSize = 416  // Jika model menggunakan 416x416
```

## Debug Mode

Untuk melihat detail deteksi di logcat:

```bash
adb logcat | grep TAG:MainActivity
```

Setiap deteksi akan mencatat:
- Label yang terdeteksi
- Confidence score
- Timestamp

## Performance Tips

1. **GPU Acceleration** (opsional):
   - Uncomment `tensorflow-lite-gpu` di build.gradle.kts
   
2. **Model Quantization**:
   - Gunakan quantized model untuk kecepatan lebih baik
   
3. **Thread Management**:
   - Analisis image di background thread (sudah implemented)

## Dependencies

- AndroidX: Core, AppCompat, Camera, Lifecycle
- TensorFlow Lite: v2.14.0
- Material: v1.13.0

## License

MIT License

## Support untuk Tunanetra

Aplikasi ini dirancang dengan mempertimbangkan:
- ✅ TalkBack compatibility
- ✅ Voice guidance untuk semua fungsi
- ✅ Accessible click targets (minimum 64dp)
- ✅ Clear content descriptions
- ✅ No reliance on color alone for information
- ✅ Large touch targets untuk mudah dipencet

