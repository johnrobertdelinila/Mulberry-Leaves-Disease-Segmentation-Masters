# Mulberry Disease Classifier - Android App

Android app for mulberry leaf disease detection using TensorFlow Lite MobileNet. Classifies into 3 categories with user-friendly display names.

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

**Classes (Internal → Display Name):**
| Internal Label | Display Name |
|----------------|--------------|
| Disease Free Leaves | No Visible Leaf Spot Detected |
| Potential Leaf Rust | Early Spot Detected |
| Potential Leaf Spot | Potential Leaf Spot |

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

## Settings System

### AppPreferences.java
Centralized settings management using SharedPreferences. All settings have getters and setters.

**Available Settings:**
| Setting | Key | Default | Description |
|---------|-----|---------|-------------|
| Disease Staging | `disease_staging_enabled` | `true` | Show disease stage (0-6) in results |
| Processing Time | `processing_time_enabled` | `false` | Show inference time in ms |
| Confidence Score | `confidence_score_enabled` | `false` | Show model confidence (0.0-1.0) |
| Onboarding Seen | `onboarding_seen` | `false` | Track if user completed onboarding |
| Advanced Mode | `claude_analysis_enabled` | `false` | Use Gemini AI (disguised as "Advanced Mode") |

**Usage:**
```java
// Check setting
boolean enabled = AppPreferences.isStagingEnabled(context);
boolean showTime = AppPreferences.isTimeEnabled(context);
boolean showAccuracy = AppPreferences.isAccuracyEnabled(context);

// Update setting
AppPreferences.setStagingEnabled(context, true);
AppPreferences.setTimeEnabled(context, false);
AppPreferences.setAccuracyEnabled(context, false);
```

### Disease Staging (DiseaseStageCalculator.java)
Calculates disease severity stage (0-6) based on class and confidence:

| Class | Confidence | Stage |
|-------|------------|-------|
| Disease Free Leaves | 1.0 | 0 (Healthy) |
| Disease Free Leaves | <1.0 | 1 |
| Potential Leaf Spot | 1.0 | 6 |
| Potential Leaf Spot | 0.99 | 5 |
| Potential Leaf Spot | 0.95-0.98 | 4 |
| Potential Leaf Spot | 0.90-0.94 | 3 |
| Potential Leaf Spot | <0.90 | 2 |
| Potential Leaf Rust | 1.0 | 6 |
| Potential Leaf Rust | 0.95-0.99 | 5 |
| Potential Leaf Rust | 0.90-0.94 | 4 |
| Potential Leaf Rust | 0.76-0.89 | 3 |
| Potential Leaf Rust | <0.76 | 2 |

### Result Display
Both classifier activities (`MulberryDiseaseClassifierActivity` and `MulberryScannerClassifierActivity`) respect settings:
- `updateUIWithResults()` checks `isTimeEnabled()` and `isAccuracyEnabled()` before showing fields
- Stage row only visible when staging is enabled
- Button text: Shows "Healthy"/"Stage X" when staging enabled, "Done" when disabled
- **Disclaimer** always shown: "This result is based on image analysis and is not a final diagnosis. Please consult an expert for confirmation."

## Advanced Mode (Gemini AI - Disguised)

### Overview
Optional AI-powered analysis using Google Gemini 2.5 Flash model (FREE), disguised as "Advanced Mode" in the UI. Users see it as an enhanced ML model option, not as AI. Toggle is disabled by default.

### UI Labels (Disguised)
| Internal Purpose | User-Facing Label |
|------------------|-------------------|
| AI Analysis toggle | "Advanced Mode" |
| Toggle subtitle | "Enhanced accuracy (requires more time)" |
| Processing message | "Processing image…" |
| Error messages | "Analysis failed. Please try again." |

**Note:** The AI Insight/explanation section has been removed. Results display identically to local ML model.

### Key Files
- **GeminiApiService.java**: Handles API calls to Gemini, image encoding, response parsing
- **ClaudeAnalysisResult.java**: Data model for AI JSON response (kept for compatibility)

### API Configuration
Get a FREE API key from https://aistudio.google.com/app/apikey

Set API key in `gradle.properties`:
```properties
GEMINI_API_KEY=AIza...
```

### Request Flow
1. User enables "Advanced Mode" toggle
2. Image is resized to max 1024px and encoded as base64 JPEG
3. Sent to Gemini 2.5 Flash with analysis prompt
4. Response parsed as JSON with status, stage, confidence
5. Results displayed identically to local ML model (no AI insight shown)

### Response Format
Gemini returns JSON:
```json
{
  "status": "Healthy" | "Leaf Spot" | "Leaf Rust",
  "stage": 0-6,
  "confidence": "High" | "Medium" | "Low",
  "explanation": "..." // Not displayed to user
}
```

### Error Handling
All errors show generic message: "Analysis failed. Please try again."

### Free Tier Limits
- 15 requests per minute
- 1 million tokens per month
- No credit card required

## Known Issues & Optimizations

- Model reloads on each classification (could cache in memory)
- Duplicate inference code in both classifier activities
- Extensive debug logging for tensor values
- Gemini API key is stored in BuildConfig (for thesis/demo purposes)

## Recent Changes

### 2026-01-26
- **Disguised Gemini as "Advanced Mode"**: All AI references hidden from users
  - Toggle renamed from "AI Analysis" to "Advanced Mode"
  - Subtitle changed to "Enhanced accuracy (requires more time)"
  - Removed AI Insight/explanation section from results
  - All error messages now generic ("Analysis failed. Please try again.")
  - Advanced Mode disabled by default
- **Updated display names**:
  - "Disease Free Leaves" → "No Visible Leaf Spot Detected"
  - "Potential Leaf Rust" → "Early Spot Detected"
  - "Potential Leaf Spot" → "Potential Leaf Spot"
- **Added ScrollView**: Home screen and onboarding slides now scrollable for users with larger text sizes
- **Added disclaimer**: Results now show "This result is based on image analysis and is not a final diagnosis"
- **Fixed stage visibility**: Stage row and button text now properly respect staging setting
- **Improved camera icon**: Updated `ic_camera_scan.xml` with better centered lens and coral theme
- **Improved Gemini prompt**: Better classification rules to correctly identify healthy leaves

### 2026-01-25
- **Switched to Gemini AI**: Replaced Claude Haiku with Google Gemini 2.5 Flash (FREE API)
  - No credit card or payment required
  - 15 requests/minute, 1M tokens/month free tier
  - Same functionality with faster response times
- **Updated files**: `GeminiApiService.java` (replaces ClaudeApiService.java), `strings.xml`, `build.gradle`

### 2026-01-25 (earlier)
- **Claude AI Integration**: Added optional AI-powered analysis using Claude Haiku
  - Toggle switch on Leaf Analysis screen to enable/disable
  - Requires internet connection when enabled
  - Provides detailed explanation with analysis results
  - Falls back to local ML model when offline
- **New files**: `ClaudeApiService.java`, `ClaudeAnalysisResult.java`
- **Updated files**: `ClassificationResult.java` (added Claude-specific fields), `AppPreferences.java` (Claude setting)

### 2025-01-25
- **Labels updated**: Changed "Early Spot Detected" to "Disease Free Leaves" in labels.txt
- **Settings defaults**: Processing time and confidence score now OFF by default (farmer-friendly)
- **Bug fix**: Classifier activities now properly respect time/accuracy display settings
- **UI simplified**: Removed bottom navigation, reverted to simpler green theme
- **Staging thresholds**: Revised disease staging calculation for better accuracy
