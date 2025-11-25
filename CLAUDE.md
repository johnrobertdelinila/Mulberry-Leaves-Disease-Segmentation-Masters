# Mulberry Disease Classifier - Android App

Android app for mulberry leaf disease detection using TensorFlow Lite MobileNet. Classifies into 3 categories: Disease Free Leaves, Potential Leaf Rust, Potential Leaf Spot.

## Cross-Platform Setup (Mac & Windows)

**Initial Setup:**
1. Clone repo and copy `gradle.properties.template` to `gradle.properties`
   ```bash
   cp gradle.properties.template gradle.properties  # Mac/Linux
   copy gradle.properties.template gradle.properties  # Windows
   ```
2. Open in Android Studio (auto-creates `local.properties`)

**Important:** Never commit `local.properties` or `gradle.properties` - they're machine-specific and gitignored.

**Troubleshooting:** If SDK errors occur after pull, run "Sync Project with Gradle Files" in Android Studio.

## Architecture

### App Flow
```
SplashActivity → MainActivity → Camera/Gallery → Classification
```

### Key Files
- **SplashActivity.java**: Entry point, 2s animation
- **MainActivity.java**: Navigation hub, camera permissions
- **MulberryScannerClassifierActivity.java**: Camera workflow, PNG 100% quality transfer
- **MulberryDiseaseClassifierActivity.java**: Gallery workflow, smart downsampling (max 1024px)

### Image Processing Differences

**Camera Workflow:**
- Captures image, compresses to PNG at 100% quality
- Passes as byte array via Intent
- TensorFlow processes at original resolution

**Gallery Workflow:**
- Loads from MediaStore
- Smart downsampling: limits to 1024px max dimension while preserving aspect ratio
- Prevents OOM errors with high-res images while maintaining quality
- TensorFlow processes downsampled image

Both use identical TensorFlow pipeline: ResizeWithCropOrPadOp → ResizeOp (NEAREST_NEIGHBOR) → NormalizeOp (mean=0.0, std=255.0)

## ML Model

**Files:** `app/src/main/ml/FinalMobilenetFold1.tflite`, `labels.txt`

**Processing:**
1. Model queries its own input dimensions dynamically
2. ImageProcessor: crop to square → resize to model size → normalize to [0,1]
3. GPU acceleration (falls back to XNNPACK)
4. Returns 3-class probabilities

**Classes:** Disease Free Leaves, Potential Leaf Rust, Potential Leaf Spot

## Build Configuration

- **Min SDK:** 26 | **Target SDK:** 34 | **Compile SDK:** 34
- **Gradle:** 8.5 | **AGP:** 8.2.2 | **Java:** 17
- **Key Dependencies:** TensorFlow Lite 0.3.0/2.3.0, Dexter 6.2.2

## Common Commands

```bash
./gradlew build           # Build app
./gradlew installDebug    # Install debug to device
./gradlew clean           # Clean build artifacts
./gradlew test            # Run tests
```

## Known Issues & Optimizations

- Model reloads on each classification (could cache in memory)
- Duplicate inference code in both classifier activities
- Extensive debug logging for tensor values
