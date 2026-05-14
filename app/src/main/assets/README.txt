# Money Detection Model Assets

Folder ini untuk menyimpan model TensorFlow Lite dan label file.

## Required Files

1. `money_model.tflite` - Model CNN Anda dalam format TensorFlow Lite
2. `labels.txt` - File berisi daftar label (satu per baris)

## Persiapan

### Dari Model Existing:

Jika Anda sudah memiliki model, copy ke folder ini:
- Windows: Drag dan drop ke folder
- Linux/Mac: `cp money_model.tflite labels.txt .`

### Dari Model Training Baru:

1. Export model dari training framework:
   ```python
   # Untuk format SavedModel ke TFLite
   converter = tf.lite.TFLiteConverter.from_saved_model("path/to/model")
   tflite_model = converter.convert()
   ```

2. Simpan sebagai `money_model.tflite`

3. Buat `labels.txt`:
   ```
   Uang 1000 Rupiah
   Uang 2000 Rupiah
   Uang 5000 Rupiah
   Uang 10000 Rupiah
   Uang 20000 Rupiah
   Uang 50000 Rupiah
   Uang 100000 Rupiah
   ```

## Important Notes

⚠️ File harus SELALU bernama:
- `money_model.tflite` (case sensitive)
- `labels.txt` (case sensitive)

📁 Pastikan file berada di folder ini sebelum compile app!

Lihat README.md dan SETUP_GUIDE.md untuk instruksi lengkap.

