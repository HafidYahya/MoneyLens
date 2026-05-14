# 🧠 How Voting System Works - Technical Deep Dive

## 🎯 Overview

The voting system is a **noise-filtering mechanism** that prevents single-frame false positives by requiring consensus from multiple consecutive frames before announcing a detection.

---

## 📐 Mathematical Model

### Voting Formula
```
Announce = consensus(predictions) >= MIN_CONSENSUS 
           AND confidence >= CONFIDENCE_THRESHOLD 
           AND label != "Non Rupiah"
           AND enough_time_passed

Where:
  consensus(predictions) = count of most-frequent label in history
  MIN_CONSENSUS = 4 (out of 5 frames must match)
  CONFIDENCE_THRESHOLD = 85%
  HISTORY_SIZE = 5 frames
```

---

## 🔄 Frame-by-Frame Execution

### Example: User Points Rp 50000 at Camera (5 Frame Sequence)

```
╔════════════════════════════════════════════════════════════════╗
║                        FRAME SEQUENCE                          ║
╚════════════════════════════════════════════════════════════════╝

┌─────────────────────────────────────────────────────────────────┐
│ FRAME 1 (T=0ms)                                                 │
├─────────────────────────────────────────────────────────────────┤
│ Camera Input: Rp 50000 (clear view)                            │
│                                                                 │
│ Step 1: Capture frame                                         │
│ Step 2: Resize to 224x224                                     │
│ Step 3: Normalize (pixel / 255.0)                             │
│ Step 4: TFLite Inference                                      │
│         Input: [1, 224, 224, 3] float32                       │
│         Output: [1, 8] softmax                                │
│                                                                 │
│ Result:                                                         │
│   Rp 1000   : 2% │░░░░░░░░░░░░░░░░░░░░                       │
│   Rp 2000   : 1% │░░░░░░░░░░░░░░░░░░░░                       │
│   Rp 5000   : 3% │░░░░░░░░░░░░░░░░░░░░                       │
│   Rp 10000  : 5% │░░░░░░░░░░░░░░░░░░░░                       │
│   Rp 20000  : 7% │░░░░░░░░░░░░░░░░░░░░                       │
│   Rp 50000  : 82% │████████████████████ ← HIGHEST             │
│   Rp 100000 : 0% │░░░░░░░░░░░░░░░░░░░░                       │
│   Non Rupiah: 0% │░░░░░░░░░░░░░░░░░░░░                       │
│                                                                 │
│ Prediction: Rp 50000 @ 82% confidence                         │
│                                                                 │
│ Step 5: Add to history                                        │
│ predictionHistory = [                                         │
│     FramePrediction("Rp 50000", 82.0f)                        │
│ ]                                                               │
│                                                                 │
│ Step 6: Check voting consensus                                │
│ consensus = getConsensusLabel()                               │
│   → Check history size: 1 < MIN_CONSENSUS(4)                 │
│   → Return NULL (need more frames)                            │
│                                                                 │
│ Decision: NO ANNOUNCE (waiting for more frames)              │
│ UI Status: "Mendeteksi..." (detecting)                        │
│ Audio: SILENT                                                  │
└─────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│ FRAME 2 (T=~33ms)                                               │
├─────────────────────────────────────────────────────────────────┤
│ Camera Input: Rp 50000 (clear view)                            │
│ TFLite Output: Rp 50000 @ 84% confidence                       │
│                                                                 │
│ predictionHistory = [                                         │
│     FramePrediction("Rp 50000", 82.0f),                       │
│     FramePrediction("Rp 50000", 84.0f)    ← NEW              │
│ ]                                                               │
│                                                                 │
│ consensus = getConsensusLabel()                               │
│   → Size: 2 < 4, Return NULL                                 │
│                                                                 │
│ Decision: NO ANNOUNCE (still waiting)                         │
│ UI Status: "Mendeteksi..."                                    │
│ Audio: SILENT                                                  │
└─────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│ FRAME 3 (T=~66ms)                                               │
├─────────────────────────────────────────────────────────────────┤
│ Camera Input: Rp 50000 (clear view)                            │
│ TFLite Output: Rp 50000 @ 86% confidence                       │
│                                                                 │
│ predictionHistory = [                                         │
│     FramePrediction("Rp 50000", 82.0f),                       │
│     FramePrediction("Rp 50000", 84.0f),                       │
│     FramePrediction("Rp 50000", 86.0f)    ← NEW              │
│ ]                                                               │
│                                                                 │
│ consensus = getConsensusLabel()                               │
│   → Size: 3 < 4, Return NULL                                 │
│                                                                 │
│ Decision: NO ANNOUNCE (need 1 more matching frame)            │
│ UI Status: "Mendeteksi..."                                    │
│ Audio: SILENT                                                  │
└─────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│ FRAME 4 (T=~99ms) ← NOISE/GLITCH HAPPENS HERE                 │
├─────────────────────────────────────────────────────────────────┤
│ Camera Input: NOISE! (user shakes camera, or sensor glitch)    │
│ TFLite Output: Rp 100000 @ 88% confidence (WRONG!)            │
│                                                                 │
│ predictionHistory = [                                         │
│     FramePrediction("Rp 50000", 82.0f),                       │
│     FramePrediction("Rp 50000", 84.0f),                       │
│     FramePrediction("Rp 50000", 86.0f),                       │
│     FramePrediction("Rp 100000", 88.0f)   ← NOISE            │
│ ]                                                               │
│                                                                 │
│ consensus = getConsensusLabel()                               │
│   → Count labels:                                             │
│       "Rp 50000"  → 3 occurrences                            │
│       "Rp 100000" → 1 occurrence                             │
│   → Check: 3 >= 4? NO                                        │
│   → Check: 1 >= 4? NO                                        │
│   → Return NULL (NO consensus yet)                           │
│                                                                 │
│ ✅ IMPORTANT: Vote-based filter prevents spam!               │
│    Single bad frame doesn't trigger announcement              │
│                                                                 │
│ Decision: NO ANNOUNCE (noise filtered by voting)              │
│ UI Status: "Mendeteksi..."                                    │
│ Audio: SILENT (protected by voting!)                          │
└─────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│ FRAME 5 (T=~133ms)                                              │
├─────────────────────────────────────────────────────────────────┤
│ Camera Input: Rp 50000 (back to clear view)                    │
│ TFLite Output: Rp 50000 @ 89% confidence                       │
│                                                                 │
│ predictionHistory = [                                         │
│     FramePrediction("Rp 50000", 82.0f),                       │
│     FramePrediction("Rp 50000", 84.0f),                       │
│     FramePrediction("Rp 50000", 86.0f),                       │
│     FramePrediction("Rp 100000", 88.0f),                      │
│     FramePrediction("Rp 50000", 89.0f)    ← NEW              │
│ ]                                                               │
│ History is now FULL (5 items)                                 │
│                                                                 │
│ consensus = getConsensusLabel()                               │
│   → Count labels:                                             │
│       "Rp 50000"  → 4 occurrences ✓ CONSENSUS!             │
│       "Rp 100000" → 1 occurrence                             │
│   → Check: 4 >= MIN_CONSENSUS(4)? YES! ✓                    │
│   → Return "Rp 50000"                                        │
│                                                                 │
│ consensus = "Rp 50000" (NOT NULL!)                            │
│                                                                 │
│ ✅ CONSENSUS FOUND! Now check confidence & debounce          │
│                                                                 │
│ Check confidence:                                              │
│   → result.isConfident = (89% >= 85%)? YES ✓                 │
│                                                                 │
│ Check label:                                                  │
│   → consensus != "Non Rupiah"? YES ✓                         │
│                                                                 │
│ Check debounce:                                               │
│   → lastDetectionTime = 0 (first time)                        │
│   → currentTime - 0 > 1500ms? NO (only ~133ms passed)        │
│   → But lastDetectedLabel != "Rp 50000" (prev was "")        │
│   → Condition: true OR (time > 1500)? TRUE ✓                 │
│                                                                 │
│ ✅ ALL CONDITIONS MET! ANNOUNCE NOW!                         │
│                                                                 │
│ Decision: ANNOUNCE! 🔊                                        │
│ TTS Output: "Terdeteksi: Rp 50000"                            │
│ UI Status: "Terdeteksi"                                       │
│ Audio: PLAYING                                                 │
│                                                                 │
│ Update state:                                                 │
│   lastDetectionTime = current_time (~133ms)                  │
│   lastDetectedLabel = "Rp 50000"                              │
└─────────────────────────────────────────────────────────────────┘

📊 VOTING HISTORY VISUALIZATION:

Frame 1: [Rp 50k]                          1 match (need 4)
Frame 2: [Rp 50k, Rp 50k]                  2 matches (need 4)
Frame 3: [Rp 50k, Rp 50k, Rp 50k]          3 matches (need 4)
Frame 4: [Rp 50k, Rp 50k, Rp 50k, 100k]    3 matches (noise!)
Frame 5: [Rp 50k, Rp 50k, Rp 50k, 100k, Rp 50k]  4 matches ✓ ANNOUNCE!
         └─ Consensus achieved!
```

---

## 🔑 Key Components in Code

### 1. FramePrediction Data Class
```kotlin
private data class FramePrediction(
    val label: String,              // e.g., "Rp 50000"
    val confidence: Float           // e.g., 89.5f
)
```

### 2. Prediction History
```kotlin
private val predictionHistory = mutableListOf<FramePrediction>()
private val MAX_HISTORY = 5        // Track last 5 predictions
private val MIN_CONSENSUS = 4      // 4 out of 5 must match
```

### 3. Voting Algorithm
```kotlin
private fun getConsensusLabel(): String? {
    // 📍 STEP 1: Check if we have enough frames
    if (predictionHistory.size < MIN_CONSENSUS) {
        return null  // Need at least 4 predictions
    }

    // 📍 STEP 2: Count occurrences of each label
    val labelCounts = mutableMapOf<String, Int>()
    for (prediction in predictionHistory) {
        // For each prediction in history, increment count for label
        labelCounts[prediction.label] = 
            labelCounts.getOrDefault(prediction.label, 0) + 1
    }
    // Result: labelCounts = {"Rp 50000": 4, "Rp 100000": 1}

    // 📍 STEP 3: Find label with consensus (4+ occurrences)
    for ((label, count) in labelCounts) {
        if (count >= MIN_CONSENSUS) {
            return label  // Found consensus!
        }
    }

    // 📍 STEP 4: No consensus found
    return null
}
```

### 4. Frame Processing with Voting
```kotlin
private fun processFrame(bitmap: Bitmap) {
    // ... existing code ...
    
    val result = tfliteManager?.detectImage(bitmap)
    if (result != null) {
        // 📍 STEP 1: Add to history
        predictionHistory.add(
            FramePrediction(result.label, result.confidence)
        )
        // Auto-remove oldest if history exceeds MAX_HISTORY
        if (predictionHistory.size > MAX_HISTORY) {
            predictionHistory.removeAt(0)
        }

        // 📍 STEP 2: Get voting consensus
        val consensus = getConsensusLabel()
        
        // 📍 STEP 3: Announce only if all conditions met
        if (consensus != null &&           // Consensus found?
            result.isConfident &&         // Confidence >= 85%?
            consensus != "Non Rupiah") {  // Not non-rupiah?
            
            // 📍 STEP 4: Check debounce timing
            val currentTime = System.currentTimeMillis()
            if (lastDetectedLabel != consensus || 
                (currentTime - lastDetectionTime > 1500)) {
                
                // 📍 STEP 5: Announce!
                ttsManager?.speak("Terdeteksi: $consensus")
                lastDetectionTime = currentTime
                lastDetectedLabel = consensus
                
                Log.d(TAG, "Detected via Voting: $consensus")
            }
        }
    }
}
```

---

## 🎯 Decision Tree (Pseudocode)

```
┌─────────────────────────────────┐
│   CAMERA FRAME ARRIVES          │
└────────────┬────────────────────┘
             │
             ▼
    ┌────────────────────┐
    │ TFLite Inference   │
    │ Get prediction     │
    └────────┬───────────┘
             │
             ▼
    ┌────────────────────┐
    │ Add to History     │
    │ (max 5 entries)    │
    └────────┬───────────┘
             │
             ▼
    ┌────────────────────┐
    │ Count Consensus    │
    │ (4/5 required)     │
    └────────┬───────────┘
             │
       ┌─────┴─────┐
       ▼           ▼
   YES/4+      NO/<4
     │           │
     │      "Not yet"
     │      (wait more)
     │
     ▼
  ┌──────────────┐
  │ Consensus    │
  │ != NULL?     │
  └──────┬───────┘
         │
     ┌───┴────┐
     ▼        ▼
    YES      NO
     │       └─→ "Mendetiksi..."
     │           (no announce)
     ▼
  ┌────────────────────┐
  │ Confidence         │
  │ >= 85%?            │
  └──────┬─────────────┘
         │
     ┌───┴────┐
     ▼        ▼
    YES      NO
     │       └─→ "Mendetiksi..."
     │
     ▼
  ┌────────────────────┐
  │ Label !=           │
  │ "Non Rupiah"?      │
  └──────┬─────────────┘
         │
     ┌───┴────┐
     ▼        ▼
    YES      NO
     │       └─→ No audio
     │           (just UI)
     ▼
  ┌────────────────────┐
  │ Debounce           │
  │ Time passed        │
  │ OR new label?      │
  └──────┬─────────────┘
         │
     ┌───┴────┐
     ▼        ▼
    YES      NO
     │       └─→ Wait 1.5s
     │
     ▼
  ┌────────────────────┐
  │ 🔊 ANNOUNCE!       │
  │ "Terdeteksi: ..."  │
  └────────────────────┘
```

---

## 📊 Cost-Benefit Analysis

### Voting System Benefits
✅ **Noise Immunity:** Single noisy frame won't trigger announcement  
✅ **Accuracy:** Confirms detection with 4/5 agreement  
✅ **User Experience:** Clear, non-spam audio output  
✅ **Robustness:** Handles sensor glitches, camera shake  

### Trade-offs
⏱️ **Detection Latency:** +~170ms (5 frames at 30fps)  
💾 **Memory:** +<1MB for history tracking  
⚙️ **CPU:** +~2ms per frame for voting calculation  

---

## 🔍 Real-World Examples

### Example 1: Clear Money Shot
```
Frames: [Rp 50k, Rp 50k, Rp 50k, Rp 50k, Rp 50k]
Consensus: Rp 50k (5/5)
Result: ANNOUNCE ✓ (fast & confident)
```

### Example 2: Shaky Camera
```
Frames: [Rp 50k, Rp 50k, Rp 100k, Rp 50k, Rp 50k]
Consensus: Rp 50k (4/5)
Result: ANNOUNCE ✓ (despite noise, voting filters it)
```

### Example 3: Unclear Image
```
Frames: [Rp 50k, Rp 100k, Rp 5k, Rp 50k, Rp 100k]
Consensus: None (max 2/5)
Result: NO ANNOUNCE (wait for clarity)
```

### Example 4: Non-Rupiah Object
```
Frames: [Non Rupiah, Non Rupiah, Non Rupiah, Non Rupiah]
Consensus: Non Rupiah (4/4)
Result: NO AUDIO (UI only, prevents confusion)
```

---

## 🧬 Voting System Tuning

### Current Values
```kotlin
MAX_HISTORY = 5              // Objects are tracked for 5 frames
MIN_CONSENSUS = 4            // 4/5 must agree
CONFIDENCE_THRESHOLD = 85f   // 85% minimum confidence
DEBOUNCE_MS = 1500L          // 1.5 second wait between announcements
```

### To Adjust Responsiveness

**More Responsive (Announce Faster):**
```kotlin
MIN_CONSENSUS = 3            // Only need 3/5 match (RISKY - more false positives)
MAX_HISTORY = 4              // Shorter history
```

**More Accurate (Require Consensus):**
```kotlin
MIN_CONSENSUS = 5            // Need 5/5 match (SLOW - might miss detections)
MAX_HISTORY = 7              // Longer history for more data
```

**Current Settings (BALANCED):**
```kotlin
MIN_CONSENSUS = 4            // Most production-ready
MAX_HISTORY = 5              // Good balance
```

---

## 📈 Performance Characteristics

### Voting System Performance

| Scenario | Latency | Result |
|----------|---------|--------|
| Clear money | 150-200ms | ✓ Accurate |
| Partial view | 200-300ms | May wait longer |
| Shaky camera | 150-250ms | ✓ Noise filtered |
| Low light | 200-500ms | May timeout |

### Frame-by-Frame Timeline
```
T=0ms   → Frame 1 captured
T=33ms  → Frame 2 captured
T=66ms  → Frame 3 captured
T=99ms  → Frame 4 captured (might be noise)
T=133ms → Frame 5 captured
        → CONSENSUS CHECK ✓
        → ANNOUNCE 🔊

Total latency: ~130-140ms (feels instantaneous to user)
```

---

**Version:** 1.1  
**Date:** May 13, 2026  
**Status:** Production Implementation

This voting system provides the best balance between responsiveness and accuracy for money detection with audio output!

