# Code Changes Summary - Voting System v1.1

## 📂 Files Modified

### 1. MainActivity.kt
**Location:** `app/src/main/java/com/app/moneylens/MainActivity.kt`

#### Changes Made:

**A. Added Voting System Variables**
```kotlin
// Line 32-39: NEW variables for voting system
private var isProcessing = false
private var lastDetectedLabel = ""
private var lastConfidence = 0f
private var lastIsConfident = false
private var lastDetectionTime = 0L
private val detectionDebounceMs = 1500L

// NEW: Voting system data class
private data class FramePrediction(val label: String, val confidence: Float)

// NEW: History tracking
private val predictionHistory = mutableListOf<FramePrediction>()
private val MAX_HISTORY = 5
private val MIN_CONSENSUS = 4  // 4 out of 5 must match
```

**B. Refactored processFrame() Method**
- **Old:** Direct check on each frame prediction
- **New:** Add prediction to history, check consensus voting

```kotlin
// OLD LOGIC (Line 110-158 in v1.0):
if (result.isConfident) {
    if (lastDetectedLabel != result.label || time check) {
        ttsManager?.speak("Terdeteksi: ${result.label}")
    }
}

// NEW LOGIC (Line 110-185 in v1.1):
// 1. Add to history
predictionHistory.add(FramePrediction(result.label, result.confidence))
if (predictionHistory.size > MAX_HISTORY) {
    predictionHistory.removeAt(0)
}

// 2. Get consensus
val consensus = getConsensusLabel()

// 3. Announce only if:
// - Consensus found (4/5 match)
// - Confidence >= 85%
// - NOT "Non Rupiah"
if (consensus != null && result.isConfident && consensus != "Non Rupiah") {
    if (time check) {
        ttsManager?.speak("Terdeteksi: $consensus")
    }
}
```

**C. Added getConsensusLabel() Function**
```kotlin
// NEW: Line 186-212 in v1.1
private fun getConsensusLabel(): String? {
    if (predictionHistory.size < MIN_CONSENSUS) {
        return null
    }

    val labelCounts = mutableMapOf<String, Int>()
    for (prediction in predictionHistory) {
        labelCounts[prediction.label] = labelCounts.getOrDefault(prediction.label, 0) + 1
    }

    for ((label, count) in labelCounts) {
        if (count >= MIN_CONSENSUS) {
            return label
        }
    }

    return null
}
```

**Summary of Changes:**
- Lines added: ~50
- Lines modified: ~30
- Total changes: 80 lines

---

### 2. TFLiteModelManager.kt
**Location:** `app/src/main/java/com/app/moneylens/ml/TFLiteModelManager.kt`

#### Changes Made:

**A. Updated Class Documentation**
```kotlin
// OLD (Line 14-20):
/**
 * TensorFlow Lite Model Manager for Rupiah Classifier
 * Model Specs:
 * - Input: 224x224 RGB images (float32, normalized 0-1)
 * - Output: 7 softmax probabilities (Rp 1000-100000)
 * - Threshold: 85% confidence for reliable detection
 */

// NEW (Line 14-21):
/**
 * TensorFlow Lite Model Manager for Rupiah Classifier
 * Model Specs:
 * - Input: 224x224 RGB images (float32, normalized 0-1)
 * - Output: 8 softmax probabilities (Rp 1000-100000 + Non Rupiah)
 * - Threshold: 85% confidence for reliable detection
 * - Voting System: Requires 4 out of 5 frames to match for confirmation
 */
```

**B. Updated Confidence Threshold**
```kotlin
// OLD (Line 186):
private const val CONFIDENCE_THRESHOLD = 94f

// NEW (Line 186):
private const val CONFIDENCE_THRESHOLD = 85f
```

**C. Updated Output Buffer Size Comment & Code**
```kotlin
// OLD (Line 156-158):
// Prepare output buffer for 7 classes (Rp 1000-100000)
val outputSize = outputShape.getOrNull(1) ?: 7
val outputBuffer = Array(1) { FloatArray(outputSize) }

// NEW (Line 156-158):
// Prepare output buffer for 8 classes (Rp 1000-100000 + Non Rupiah)
val outputSize = outputShape.getOrNull(1) ?: 8
val outputBuffer = Array(1) { FloatArray(outputSize) }
```

**Summary of Changes:**
- Lines modified: 3
- Total changes: 3 lines

---

### 3. labels.txt
**Location:** `app/src/main/assets/labels.txt`

#### Changes Made:

```
# OLD (7 lines):
Rp 1000
Rp 2000
Rp 5000
Rp 10000
Rp 20000
Rp 50000
Rp 100000

# NEW (8 lines):
Rp 1000
Rp 2000
Rp 5000
Rp 10000
Rp 20000
Rp 50000
Rp 100000
Non Rupiah
```

**Summary of Changes:**
- Lines added: 1
- Total changes: 1 line

---

## 📊 Impact Summary

| Component | Old | New | Change |
|-----------|-----|-----|--------|
| Confidence Threshold | 94% | 85% | ↓9% more lenient |
| Model Output Classes | 7 | 8 | +1 Non Rupiah |
| Voting System | ✗ | ✓ 4/5 | New feature |
| Code Lines (MainActivity) | 190 | 227 | +37 |
| Code Lines (TFLiteModelManager) | 190 | 190 | +3 |
| Total Codebase | 616 lines | ~656 lines | +40 |

---

## 🔄 Behavior Changes

### Detection Flow (Before vs After)

**Before (v1.0):**
```
Frame → Inference → Confidence ≥ 94%? → YES → Announce (immediately)
                                      → NO → Status: "Mendeteksi..."
```

**After (v1.1):**
```
Frame → Inference → Add to History → Check Consensus
                                   → 4/5 Match? → YES → Confidence ≥ 85%? → YES → Announce
                                                      → NO → Status: "Mendeteksi..."
                                   → NO → Wait more frames
```

### Confidence Logic

| Confidence Score | v1.0 Behavior | v1.1 Behavior |
|------------------|---------------|---------------|
| 45-84% | "Mendeteksi..." | "Mendeteksi..." (same) |
| 85-93% | "Mendeteksi..." | ANNOUNCE ✓ (new) |
| 94-100% | ANNOUNCE | ANNOUNCE (same) |

---

## 🧪 Testing Changes Needed

### Before Testing v1.1
- ✓ Verify model has 8 output classes
- ✓ Verify labels.txt has 8 lines
- ✓ Rebuild APK with new code

### During Testing
- ✓ Test all 7 scenarios in TESTING_VOTING_SYSTEM_v1.1.md
- ✓ Monitor logcat for voting consensus
- ✓ Check for any crashes

---

## ⚙️ Technical Details

### Memory Impact
- **New object:** FramePrediction data class (small)
- **New list:** predictionHistory (max 5 items, ~500 bytes max)
- **Total overhead:** <1MB

### Performance Impact
- **Detection latency:** +~170ms (5 frames @ 30fps)
- **CPU impact:** Negligible (voting is just string comparison)
- **Memory impact:** <1MB

### Voting Algorithm Complexity
- **Time Complexity:** O(n) per frame, where n=predictionHistory.size (max 5)
- **Space Complexity:** O(m), where m=unique labels (max 8)

---

## 🔄 Version Comparison

| Feature | v1.0 | v1.1 |
|---------|------|------|
| Confidence Threshold | 94% | 85% |
| Output Classes | 7 | 8 |
| Voting System | ✗ | ✓ |
| Non Rupiah Support | ✗ | ✓ |
| Noise Immunity | Low | High |
| Build Status | ✅ | ✅ |

---

## 📝 Rollback Plan

If issues occur, can quickly rollback by:

1. **Revert confidence threshold:**
   - Change `85f` back to `94f` in TFLiteModelManager.kt

2. **Disable voting system:**
   - Comment out voting code in processFrame()
   - Use `result.label` directly instead of `consensus`

3. **Remove Non Rupiah:**
   - Remove last line from labels.txt
   - Change outputSize back to 7

---

**Version:** 1.1  
**Files Modified:** 3  
**Lines Added/Changed:** ~80  
**Build Status:** ✅ Successful  
**Date:** May 13, 2026

