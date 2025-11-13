# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Android application for mulberry leaf disease detection using TensorFlow Lite and MobileNet. The app provides two classification workflows: real-time camera scanning and gallery image selection. Classifies into 3 disease categories: Disease Free Leaves, Potential Leaf Rust, and Potential Leaf Spot.

## Build & Development Commands

### Building the App
```bash
./gradlew build
```

### Running Tests
```bash
# Run unit tests
./gradlew test

# Run instrumented tests (requires connected device/emulator)
./gradlew connectedAndroidTest
```

### Installing to Device
```bash
# Debug build
./gradlew installDebug

# Release build
./gradlew installRelease
```

### Cleaning Build Artifacts
```bash
./gradlew clean
```

### Linting
```bash
./gradlew lint
```

## Architecture Overview

### Activity Flow
```
SplashActivity (entry point, 2s animation)
    ↓
MainActivity (home hub)
    ├─→ "Scan Plant" → Camera → MulberryScannerClassifierActivity
    └─→ "Load Images" → Gallery → MulberryDiseaseClassifierActivity
```

### Key Components

**SplashActivity** (`app/src/main/java/com/example/mulberrydiseaseclassifier/SplashActivity.java`)
- App entry point with animated splash screen
- 2-second delay before transitioning to MainActivity
- All activities locked to portrait orientation (screenOrientation="nosensor")

**MainActivity** (`app/src/main/java/com/example/mulberrydiseaseclassifier/MainActivity.java`)
- Primary navigation hub
- Handles camera permissions (CAMERA permission required)
- Routes to either camera capture or gallery selection

**MulberryScannerClassifierActivity** (`app/src/main/java/com/example/mulberrydiseaseclassifier/MulberryScannerClassifierActivity.java`)
- Receives Bitmap from camera as Intent extra ("image" byte array)
- Implements TensorFlow Lite inference pipeline
- Displays results: disease name, confidence score, inference time

**MulberryDiseaseClassifierActivity** (`app/src/main/java/com/example/mulberrydiseaseclassifier/MulberryDiseaseClassifierActivity.java`)
- Gallery-based classification workflow
- Scales images to 124x124 before processing
- Contains Recognition helper class for result formatting
- Identical inference logic to Scanner activity

### ML Model Integration

**Model Location:**
- Model: `app/src/main/ml/FinalMobilenetFold1.tflite`
- Labels: `app/src/main/ml/labels.txt`

**Architecture:** MobileNet (Fold 1 from k-fold cross-validation)

**Output Classes:**
1. Disease Free Leaves
2. Potential Leaf Rust
3. Potential Leaf Spot

**Image Processing Pipeline:**
1. ResizeWithCropOrPadOp - Center crops to square
2. ResizeOp - Resizes to model input size (NEAREST_NEIGHBOR)
3. NormalizeOp - Mean: 0.0, Std: 255.0 (normalizes to [0, 1])

**Inference Acceleration:**
- GPU acceleration via GpuDelegate when available
- Falls back to XNNPACK CPU acceleration
- Device compatibility checked via CompatibilityList

**Model Loading:**
- Currently loads on every "CHECK" button click (not cached)
- Uses FileUtil.loadMappedFile() for model
- Uses FileUtil.loadLabels() for label file

### Key Dependencies

TensorFlow Lite stack (from `app/build.gradle`):
```gradle
implementation 'org.tensorflow:tensorflow-lite-support:0.3.0'
implementation 'org.tensorflow:tensorflow-lite-metadata:0.3.0'
implementation 'org.tensorflow:tensorflow-lite-gpu:2.3.0'
implementation 'org.tensorflow:tensorflow-lite-gpu-delegate-plugin:0.3.0'
```

Runtime permissions:
```gradle
implementation 'com.karumi:dexter:6.2.2'
```

### Build Configuration

**Android Gradle Plugin:** 8.2.2
**Gradle Version:** 8.5
**Min SDK:** 26 (Android 8.0 Oreo)
**Target SDK:** 34 (Android 14)
**Compile SDK:** 34
**Java Version:** 17 (JetBrains Runtime)
**ML Model Binding:** Enabled in build.gradle
**Namespace:** Declared in build.gradle (com.example.mulberrydiseaseclassifier)

## Development Notes

### Model Inference Workflow
Both classifier activities follow the same pattern:
1. Load model file (.tflite) and labels (.txt) from assets
2. Create Interpreter with GPU/XNNPACK acceleration options
3. Query input tensor shape from model
4. Build ImageProcessor pipeline (crop, resize, normalize)
5. Convert Bitmap → TensorImage → TensorBuffer
6. Run interpreter.run() with input/output buffers
7. Post-process with TensorProcessor and TensorLabel
8. Extract max confidence prediction

### Data Passing Between Activities
- Camera images passed as byte arrays via Intent extras (key: "image")
- Bitmap converted: `bitmap.compress(ByteArrayOutputStream) → byteArray`
- Receiver reconstructs: `BitmapFactory.decodeByteArray(byteArray)`

### Current Implementation Considerations
- Extensive debug logging present (Log.d statements for tensor values)
- Model reloads on every classification (potential optimization: cache in memory)
- Both classifier activities have duplicate inference code (consider extracting to shared utility)
- Image preprocessing manually verified pixel-by-pixel in logs

### Testing the App
1. Requires physical device or emulator with camera capability
2. Grant camera permissions when prompted
3. Test both workflows: camera capture and gallery selection
4. Verify results show disease name, confidence (0.0-1.0), and inference time (ms)
5. Check GPU acceleration status in logs

### File Structure Notes
- Java source: `app/src/main/java/com/example/mulberrydiseaseclassifier/`
- Layouts: `app/src/main/res/layout/`
- Drawables: `app/src/main/res/drawable/`
- Animations: `app/src/main/res/anim/`
- ML assets: `app/src/main/ml/`
