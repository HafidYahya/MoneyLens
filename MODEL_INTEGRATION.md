# MODEL INTEGRATION GUIDE - MoneyLens

Panduan lengkap untuk mengintegrasikan model CNN TFLite Anda ke aplikasi MoneyLens.

---

## 🎯 Objective

Setelah membaca guide ini, Anda akan bisa:
1. ✅ Prepare model TFLite dengan format yang benar
2. ✅ Create labels file yang sesuai
3. ✅ Integrate ke aplikasi MoneyLens
4. ✅ Test dan verify functionality

---

## 📋 Checklist Pre-Integration

SEBELUM integrasi, pastikan:

- [ ] Model CNN sudah di-train sempurna
- [ ] Accuracy model acceptable (>90% untuk production)
- [ ] Model dalam format SavedModel atau H5 (atau sudah TFLite)
- [ ] Tahu jumlah output classes/labels
- [ ] Tahu input size (224x224, 416x416, dll)
- [ ] Punya list of labels dalam urutan output indices

---

## 📁 STEP 1: Convert Model ke TFLite Format

### Jika model Anda dalam format SavedModel:

**File: `convert_model.py`**

```python
import tensorflow as tf
import os

def convert_saved_model_to_tflite(saved_model_path, output_path):
    """Convert SavedModel to TFLite format"""
    
    # Load model
    converter = tf.lite.TFLiteConverter.from_saved_model(saved_model_path)
    
    # Optional: Enable optimization untuk file lebih kecil
    converter.optimizations = [tf.lite.Optimize.DEFAULT]
    converter.target_spec.supported_ops = [
        tf.lite.OpsSet.TFLITE_BUILTINS,
        tf.lite.OpsSet.SELECT_TF_OPS
    ]
    
    # Convert
    tflite_model = converter.convert()
    
    # Save
    with open(output_path, 'wb') as f:
        f.write(tflite_model)
    
    print(f"✅ Model berhasil diconvert ke: {output_path}")
    print(f"   File size: {os.path.getsize(output_path) / (1024*1024):.2f} MB")

# Usage
convert_saved_model_to_tflite(
    "path/to/saved_model",
    "money_model.tflite"
)
```

### Jika model dalam format H5/Keras:

```python
import tensorflow as tf
import os

def convert_keras_to_tflite(keras_model_path, output_path):
    """Convert Keras H5 model to TFLite format"""
    
    # Load model
    model = tf.keras.models.load_model(keras_model_path)
    
    # Convert
    converter = tf.lite.TFLiteConverter.from_keras_model(model)
    
    # Optimize
    converter.optimizations = [tf.lite.Optimize.DEFAULT]
    
    tflite_model = converter.convert()
    
    # Save
    with open(output_path, 'wb') as f:
        f.write(tflite_model)
    
    print(f"✅ Model converted: {output_path}")
    print(f"   Size: {os.path.getsize(output_path) / (1024*1024):.2f} MB")

# Usage
convert_keras_to_tflite("model.h5", "money_model.tflite")
```

### Jika model dalam format PyTorch:

```python
# Install: pip install torch onnx onnx-simplifier tflite-support
import torch
import onnx
from onnxruntime.quantization import quantize_dynamic, QuantType
import tensorflow as tf

def convert_pytorch_to_tflite(pytorch_model_path, output_path, input_shape=(1, 3, 224, 224)):
    """Convert PyTorch model to TFLite via ONNX"""
    
    # Step 1: PyTorch → ONNX
    model = torch.load(pytorch_model_path)
    dummy_input = torch.randn(*input_shape)
    
    onnx_path = "model.onnx"
    torch.onnx.export(model, dummy_input, onnx_path,
                      input_names=['input'],
                      output_names=['output'])
    
    # Step 2: ONNX → TFLite (requires onnx-tf)
    # Or use online converter: https://github.com/onnx/onnx-tensorflow
    
    print(f"✅ Model converted to: {output_path}")

# Usage
convert_pytorch_to_tflite("model.pt", "money_model.tflite")
```

### Validation Conversion:

```python
import tensorflow as tf
import numpy as np

# Load converted model
interpreter = tf.lite.Interpreter("money_model.tflite")
interpreter.allocate_tensors()

# Get model details
input_details = interpreter.get_input_details()
output_details = interpreter.get_output_details()

print(f"✅ Model Input:")
print(f"   Shape: {input_details[0]['shape']}")
print(f"   Type: {input_details[0]['dtype']}")

print(f"✅ Model Output:")
print(f"   Shape: {output_details[0]['shape']}")
print(f"   Type: {output_details[0]['dtype']}")

# Test inference
test_image = np.random.random(input_details[0]['shape']).astype(np.float32)
interpreter.set_tensor(input_details[0]['index'], test_image)
interpreter.invoke()
output = interpreter.get_tensor(output_details[0]['index'])

print(f"✅ Test inference successful!")
print(f"   Output shape: {output.shape}")
print(f"   Output values: {output}")
```

---

## 🏷️ STEP 2: Create Labels File

### Format Requirements

File: `labels.txt`
```
[Label untuk class 0]
[Label untuk class 1]
[Label untuk class 2]
...
[Label untuk class N]
```

### Example untuk Deteksi Uang Rupiah

```
Uang 1000 Rupiah
Uang 2000 Rupiah
Uang 5000 Rupiah
Uang 10000 Rupiah
Uang 20000 Rupiah
Uang 50000 Rupiah
Uang 100000 Rupiah
```

### ⚠️ PENTING: Label Order

**Labels HARUS diurutkan sesuai dengan output indices model!**

Jika model Anda training dengan:
```python
class_labels = ['Uang 1000', 'Uang 2000', 'Uang 5000', ...]
model.compile(..., metrics=['accuracy'])
```

MAKA `labels.txt` harus exactly the same order:
```
Uang 1000
Uang 2000
Uang 5000
```

### Cara Verify Label Order:

```python
import tensorflow as tf
import numpy as np

# Load model
interpreter = tf.lite.Interpreter("money_model.tflite")
interpreter.allocate_tensors()

input_details = interpreter.get_input_details()
output_details = interpreter.get_output_details()

# Get output size = number of labels
num_classes = output_details[0]['shape'][1]
print(f"Model expects {num_classes} labels")

# Test dengan known image dari setiap class
for class_idx in range(num_classes):
    # Load test image dari class class_idx
    test_image = load_test_image(f"class_{class_idx}.jpg")
    
    interpreter.set_tensor(input_details[0]['index'], test_image)
    interpreter.invoke()
    output = interpreter.get_tensor(output_details[0]['index'])
    
    # Output[0][class_idx] should be highest
    predicted_idx = np.argmax(output[0])
    print(f"Class {class_idx}: Predicted as {predicted_idx}")
```

---

## 📦 STEP 3: Copy Files ke Project

### Option A: Manual Copy (Windows)

```batch
REM Navigate ke project
cd D:\SKRIPSI\APLIKASI\MoneyLens

REM Copy model
copy money_model.tflite app\src\main\assets\

REM Copy labels
copy labels.txt app\src\main\assets\

REM Verify
dir app\src\main\assets\
```

Expected output:
```
money_model.tflite  (10-100 MB)
labels.txt          (< 1 KB)
README.txt          (existing)
```

### Option B: Manual Copy (Linux/Mac)

```bash
cd ~/path/to/MoneyLens

cp money_model.tflite app/src/main/assets/
cp labels.txt app/src/main/assets/

ls -lh app/src/main/assets/
```

### Option C: Android Studio GUI

1. Open Project in Android Studio
2. Right-click `app/src/main/assets/`
3. Reveal in Explorer/Finder
4. Copy `money_model.tflite` dan `labels.txt` ke folder
5. Sync Gradle

---

## 🔨 STEP 4: Build & Test

### Clean Build

```bash
cd D:\SKRIPSI\APLIKASI\MoneyLens

# Clean cache
.\gradlew clean

# Full rebuild
.\gradlew build

# Expected output:
# BUILD SUCCESSFUL in Xm XXs
```

### Debug APK Generation

```bash
# Generate debug APK
.\gradlew assembleDebug

# APK location:
# D:\SKRIPSI\APLIKASI\MoneyLens\app\build\outputs\apk\debug\app-debug.apk
```

### Install ke Device/Emulator

```bash
# Via adb
adb install app/build/outputs/apk/debug/app-debug.apk

# Via Android Studio
# Run → Select Device → OK
```

---

## ✅ STEP 5: Verify Integration

### Testing On Device

1. **Start aplikasi**
   - Should see: "Aplikasi siap. Arahkan kamera ke uang untuk mendeteksi"

2. **Show money to camera**
   - Should see detection results
   - If confidence >= 85%: Should hear TTS output

3. **Check console logs**
   ```bash
   adb logcat | grep "Detected:"
   ```
   Expected: `Detected: Uang 100000 Rupiah, Confidence: 92.5%`

4. **Test Flash**
   - Tap Flash button
   - Should hear: "Flash On" atau "Flash Off"
   - Flash torch should toggle

### Debugging if Issues

#### Issue: "Could not load model"

**Possible causes:**
1. File bukan .tflite format
2. File corrupt
3. Path salah

**Solution:**
```python
# Verify file
import tensorflow as tf
try:
    interpreter = tf.lite.Interpreter("money_model.tflite")
    print("✅ Model OK")
except Exception as e:
    print(f"❌ Model Error: {e}")
```

#### Issue: "Labels mismatch"

**Possible causes:**
1. Number of labels != model output size
2. Label order salah
3. File encoding wrong

**Solution:**
```bash
# Count labels
wc -l labels.txt

# Should equal model output shape[1]
```

#### Issue: Low Confidence Scores

**Possible causes:**
1. Model tidak trained properly
2. Input preprocessing berbeda
3. Model dan labels tidak match

**Solution:**
1. Retrain model dengan better data
2. Verify preprocessing sama seperti training
3. Test model standalone

---

## 🎯 Advanced: Custom Model Specifications

### If Model Input Size Different

Edit `TFLiteModelManager.kt`:

```kotlin
fun detectImage(bitmap: Bitmap): DetectionResult? {
    return try {
        // Auto-detect input size dari model
        val inputWidth = inputShape[2]   // [batch, height, width, channels]
        val inputHeight = inputShape[1]
        
        val resizedBitmap = Bitmap.createScaledBitmap(
            bitmap,
            inputWidth,   // Automatically use model's required size
            inputHeight,
            true
        )
        // ... rest of code
    }
}
```

### If Model Uses Different Normalization

Edit preprocessing di `TFLiteModelManager.kt`:

```kotlin
// For model that expects [-1, 1] range:
for (pixel in pixels) {
    val r = (((pixel shr 16) and 0xFF) / 255.0f) * 2 - 1
    val g = (((pixel shr 8) and 0xFF) / 255.0f) * 2 - 1
    val b = ((pixel and 0xFF) / 255.0f) * 2 - 1
    inputBuffer.putFloat(r)
    inputBuffer.putFloat(g)
    inputBuffer.putFloat(b)
}

// For model that expects [0, 1] range:
for (pixel in pixels) {
    val r = ((pixel shr 16) and 0xFF) / 255.0f
    val g = ((pixel shr 8) and 0xFF) / 255.0f
    val b = (pixel and 0xFF) / 255.0f
    inputBuffer.putFloat(r)
    inputBuffer.putFloat(g)
    inputBuffer.putFloat(b)
}
```

### If Model Output Not Probability

Jika output bukan probability (0-1), perlu scaling:

```kotlin
// If output in different range, normalize:
val maxValue = results.maxOrNull() ?: 1f
val normalizedResults = results.map { it / maxValue }

// Then use normalized values
val confidence = normalizedResults[maxIndex] * 100
```

---

## 🚀 Production Deployment

### Before Release:

- [ ] Model accuracy >90%
- [ ] Tested dengan >=10 device berbeda
- [ ] TTS output jelas dan tepat
- [ ] Flash toggle berfungsi sempurna
- [ ] TalkBack compatible tested
- [ ] Battery performance acceptable
- [ ] APK size < 250MB
- [ ] No memory leaks

### Build Release APK:

```bash
# Generate release APK (requires signing key)
.\gradlew assembleRelease

# Or sign in Android Studio:
# Build → Generate Signed App Bundle
```

### Performance Optimization:

```python
# Use quantization untuk faster inference:
converter.optimizations = [tf.lite.Optimize.DEFAULT]
converter.target_spec.supported_ops = [
    tf.lite.OpsSet.TFLITE_BUILTINS
]

# Result: ~4x faster, ~75% smaller
```

---

## 📊 Expected Performance

After successful integration:

| Metric | Expected |
|--------|----------|
| App Startup | < 3s |
| Detection FPS | 20-30 |
| Model Inference | 50-200ms |
| Memory | 100-300MB |
| Battery | < 10%/hr |
| TTS Latency | < 500ms |

---

## 🆘 Troubleshooting Reference

| Error | Cause | Solution |
|-------|-------|----------|
| File not found | Model path salah | Check assets folder |
| Shape mismatch | Labels != output | Count lines in labels.txt |
| Crash on inference | Model corrupt | Rerun conversion |
| Low confidence | Model accuracy | Retrain with better data |
| TTS not working | Locale error | Change TTS language |
| Memory error | Model terlalu besar | Use quantization |

---

## 📚 References

- TensorFlow Lite Conversion: https://www.tensorflow.org/lite/convert
- Android TFLite Guide: https://www.tensorflow.org/lite/guide/android
- CameraX: https://developer.android.com/training/camerax

---

## ✅ Integration Checklist

- [ ] Model converted to .tflite
- [ ] Labels file created (correct order)
- [ ] Files copied to `app/src/main/assets/`
- [ ] Gradle build successful
- [ ] APK installed on device
- [ ] App starts without crash
- [ ] Camera shows preview
- [ ] TTS announces ready message
- [ ] Detection works (confidence >= 85%)
- [ ] Flash toggle works
- [ ] Test dengan multiple money
- [ ] TalkBack navigation works

---

**Status:** ✅ Ready for Integration  
**Last Updated:** May 10, 2026  

