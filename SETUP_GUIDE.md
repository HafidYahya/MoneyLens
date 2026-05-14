# Setup Guide - Integrasi Model TensorFlow Lite

## Persyaratan Sebelum Memulai

Pastikan Anda memiliki:
1. Model CNN dalam format `.tflite`
2. File labels dalam format `.txt` (satu label per baris)
3. Informasi tentang input size model (default app: 224x224)
4. Informasi tentang preprocessing yang digunakan saat training

## Step-by-Step Setup

### Step 1: Siapkan Model dan Labels

Jika Anda memiliki model dalam format SavedModel atau H5:

```python
# Contoh konversi menggunakan TensorFlow
import tensorflow as tf

# Dari SavedModel
converter = tf.lite.TFLiteConverter.from_saved_model("path/to/saved_model")
tflite_model = converter.convert()

# Simpan
with open("money_model.tflite", "wb") as f:
    f.write(tflite_model)
```

### Step 2: Buat File Labels

Buat file `labels.txt` dengan format:

```
Uang 1000 Rupiah
Uang 2000 Rupiah
Uang 5000 Rupiah
Uang 10000 Rupiah
Uang 20000 Rupiah
Uang 50000 Rupiah
Uang 100000 Rupiah
```

**Penting:** Order label harus sesuai dengan output indices dari model!

### Step 3: Copy ke Project

#### Windows:
```batch
cd D:\SKRIPSI\APLIKASI\MoneyLens
mkdir app\src\main\assets
copy money_model.tflite app\src\main\assets\
copy labels.txt app\src\main\assets\
```

#### Linux/Mac:
```bash
cd /path/to/MoneyLens
mkdir -p app/src/main/assets
cp money_model.tflite app/src/main/assets/
cp labels.txt app/src/main/assets/
```

### Step 4: Validasi Model

Untuk memastikan model kompatibel, gunakan script ini di Python:

```python
import tensorflow as tf
import numpy as np

# Load model
interpreter = tf.lite.Interpreter(model_path="money_model.tflite")
interpreter.allocate_tensors()

# Get details
input_details = interpreter.get_input_details()
output_details = interpreter.get_output_details()

print("Input Shape:", input_details[0]['shape'])
print("Input Type:", input_details[0]['dtype'])
print("Output Shape:", output_details[0]['shape'])
print("Output Type:", output_details[0]['dtype'])
```

**Output yang diharapkan:**
```
Input Shape: [1, 224, 224, 3]    # Bisa berbeda jika model lain
Input Type: <class 'numpy.float32'>
Output Shape: [1, 7]             # 7 = jumlah kelas (labels)
Output Type: <class 'numpy.float32'>
```

### Step 5: Sesuaikan Kode (Jika Diperlukan)

Jika model Anda memiliki spesifikasi berbeda, edit `TFLiteModelManager.kt`:

**Jika input size berbeda:**
```kotlin
// Di method loadModel()
val inputShape = interpreter!!.getInputTensor(0).shape()
val inputImageSize = inputShape[1]  // Otomatis detected
```

**Jika preprocessing berbeda:**
```kotlin
// Di method detectImage()
private const val IMAGE_MEAN = 127.5f   // Ubah jika perlu
private const val IMAGE_STD = 127.5f    // Ubah jika perlu
```

**Jika jumlah label berbeda:**
Tidak perlu ubah - otomatis terdeteksi dari output tensor size.

### Step 6: Build dan Test

```bash
cd D:\SKRIPSI\APLIKASI\MoneyLens

# Clean build
./gradlew clean build

# Install ke emulator/device
./gradlew installDebug

# Atau dari Android Studio: Build → Build Bundle(s) / APK(s) → Build APK(s)
```

Lihat output di logcat:
```bash
adb logcat | grep "TFLiteModelManager\|MainActivity"
```

## Model Requirements

### Input
- **Format:** RGB Image
- **Shape:** [1, H, W, 3] (H, W bisa 224, 256, 416 dll)
- **Type:** Float32 (normalized 0-1 atau -1 to 1)
- **Preprocessing:** ResizeOp + NormalizeOp

### Output
- **Format:** Probability/Confidence scores
- **Shape:** [1, num_classes]
- **Type:** Float32
- **Range:** 0-1 (akan diperlakukan sebagai persentase)

## Troubleshooting

### Build Error: "Cannot find tflite model"
```
Solution:
- Pastikan file ada di app/src/main/assets/money_model.tflite
- Clean build: ./gradlew clean build
```

### Runtime Error: "Model incompatible"
```
Solution:
- Periksa input/output shape dengan script Python
- Pastikan model adalah .tflite, bukan format lain
```

### Low Confidence Scores
```
Solution:
- Periksa preprocessing: normalisasi harus sesuai training
- Pastikan label urutan benar
- Coba ubah threshold di CONFIDENCE_THRESHOLD
```

### No Output dari Kamera
```
Solution:
1. Cek permissions: adb shell pm list permissions
2. Berikan permissions: adb shell pm grant com.app.moneylens android.permission.CAMERA
3. Lihat logcat untuk error details
```

## Testing Model Offline

Untuk test model sebelum integrasi:

```python
import tensorflow as tf
import numpy as np
from PIL import Image

def test_model(model_path, image_path, labels_path):
    # Load model
    interpreter = tf.lite.Interpreter(model_path=model_path)
    interpreter.allocate_tensors()
    
    # Load image
    img = Image.open(image_path).resize((224, 224))
    img_array = np.array(img, dtype=np.float32) / 255.0
    input_data = np.expand_dims(img_array, axis=0)
    
    # Inference
    input_details = interpreter.get_input_details()
    output_details = interpreter.get_output_details()
    
    interpreter.set_tensor(input_details[0]['index'], input_data)
    interpreter.invoke()
    
    output_data = interpreter.get_tensor(output_details[0]['index'])
    
    # Load labels
    with open(labels_path, 'r') as f:
        labels = f.read().strip().split('\n')
    
    # Show results
    predictions = output_data[0]
    top_idx = np.argmax(predictions)
    confidence = predictions[top_idx] * 100
    
    print(f"Predicted: {labels[top_idx]}")
    print(f"Confidence: {confidence:.2f}%")
    
    return labels[top_idx], confidence

# Usage
test_model("money_model.tflite", "test_image.jpg", "labels.txt")
```

## Next Steps

Setelah setup berhasil:
1. ✅ Compile dan jalankan aplikasi
2. ✅ Test deteksi dengan berbagai uang
3. ✅ Adjust confidence threshold jika perlu
4. ✅ Fine-tune model jika akurasi kurang

## Referensi

- [TensorFlow Lite Guide](https://www.tensorflow.org/lite)
- [TensorFlow Lite Android Guide](https://www.tensorflow.org/lite/guide/android)
- [Model Conversion Guide](https://www.tensorflow.org/lite/convert)

