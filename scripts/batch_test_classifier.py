"""
Batch Test Mulberry Disease Classifier

Tests HEIC images against the TFLite model and generates a results table.

This script mirrors the exact preprocessing and classification pipeline used in
the Android app (MulberryDiseaseClassifierActivity.java and
MulberryScannerClassifierActivity.java) to ensure consistent results.

Android Pipeline (from Java code):
1. ResizeWithCropOrPadOp(cropSize, cropSize) - Center crop to square
2. ResizeOp(imageSizeX, imageSizeY, NEAREST_NEIGHBOR) - Resize to model input
3. NormalizeOp(IMAGE_MEAN=0.0, IMAGE_STD=255.0) - Normalize: pixel / 255.0
"""

import os
import sys
import numpy as np
from PIL import Image

# Register HEIC support with Pillow
import pillow_heif
pillow_heif.register_heif_opener()

# Use TensorFlow Lite interpreter
import tensorflow as tf


# Constants matching Java DiseaseStageCalculator.java (commit 61e8cf7)
CLASS_DISEASE_FREE = "Disease Free Leaves"
CLASS_LEAF_SPOT = "Potential Leaf Spot"
CLASS_LEAF_RUST = "Potential Leaf Rust"

# Epsilon for floating point comparisons (matching Java: EPSILON = 0.0001f)
EPSILON = 0.0001

# Normalization constants (matching Java: IMAGE_MEAN = 0.0f, IMAGE_STD = 255.0f)
IMAGE_MEAN = 0.0
IMAGE_STD = 255.0


def load_model(model_path):
    """Load the TFLite model and get interpreter."""
    interpreter = tf.lite.Interpreter(model_path=model_path)
    interpreter.allocate_tensors()

    input_details = interpreter.get_input_details()
    output_details = interpreter.get_output_details()

    # Get model input dimensions (matching Java: imageSizeY = inputImageShape[1], imageSizeX = inputImageShape[2])
    input_shape = input_details[0]['shape']
    height, width = input_shape[1], input_shape[2]

    return interpreter, input_details, output_details, (height, width)


def preprocess_image(image_path, target_size):
    """
    Preprocess image matching Android pipeline exactly:

    Java code (MulberryDiseaseClassifierActivity.java lines 240-244):
        int cropSize = Math.min(bitmap.getWidth(), bitmap.getHeight());
        ImageProcessor imageProcessor = new ImageProcessor.Builder()
            .add(new ResizeWithCropOrPadOp(cropSize, cropSize))
            .add(new ResizeOp(imageSizeX, imageSizeY, ResizeOp.ResizeMethod.NEAREST_NEIGHBOR))
            .add(new NormalizeOp(IMAGE_MEAN, IMAGE_STD))
            .build();

    Steps:
    1. ResizeWithCropOrPadOp - Center crop to square (min dimension)
    2. ResizeOp - Resize to model input using NEAREST_NEIGHBOR
    3. NormalizeOp - Normalize: (pixel - 0.0) / 255.0 = pixel / 255.0
    """
    # Load image
    img = Image.open(image_path)

    # Convert to RGB if necessary (handles RGBA, HEIC, etc.)
    if img.mode != 'RGB':
        img = img.convert('RGB')

    width, height = img.size

    # Step 1: ResizeWithCropOrPadOp(cropSize, cropSize)
    # This operation center-crops (or pads) to make the image square
    # cropSize = Math.min(bitmap.getWidth(), bitmap.getHeight())
    crop_size = min(width, height)
    left = (width - crop_size) // 2
    top = (height - crop_size) // 2
    right = left + crop_size
    bottom = top + crop_size
    img = img.crop((left, top, right, bottom))

    # Step 2: ResizeOp(imageSizeX, imageSizeY, NEAREST_NEIGHBOR)
    target_height, target_width = target_size
    img = img.resize((target_width, target_height), Image.NEAREST)

    # Step 3: NormalizeOp(IMAGE_MEAN=0.0, IMAGE_STD=255.0)
    # Formula: (pixel - IMAGE_MEAN) / IMAGE_STD = pixel / 255.0
    img_array = np.array(img, dtype=np.float32)
    img_array = (img_array - IMAGE_MEAN) / IMAGE_STD

    # Add batch dimension: shape becomes (1, height, width, 3)
    img_array = np.expand_dims(img_array, axis=0)

    return img_array


def is_equal(a, b):
    """
    Compare floats with epsilon tolerance for floating point precision issues.
    Matches Java: Math.abs(a - b) < EPSILON
    """
    return abs(a - b) < EPSILON


def calculate_disease_stage(class_name, confidence):
    """
    Calculate disease stage based on classification and confidence.

    Exactly mirrors Java DiseaseStageCalculator.java logic (commit 61e8cf7):

    Disease Free Leaves (Healthy):
        - 1.0 (exact) → Stage 0 (Super Healthy)
        - <1.0 → Stage 1 (Healthy)

    Potential Leaf Spot (Diseased):
        - 1.0 (exact) → Stage 6
        - 0.99 (exact) → Stage 5
        - 0.95-0.98 → Stage 4
        - 0.90-0.94 → Stage 3
        - <0.90 → Stage 2

    Potential Leaf Rust (Diseased - different thresholds):
        - 1.0 (exact) → Stage 6
        - 0.95-0.99 → Stage 5
        - 0.90-0.94 → Stage 4
        - 0.76-0.89 → Stage 3
        - <0.76 → Stage 2
    """
    if class_name is None:
        return -1

    normalized = class_name.strip()

    if normalized == CLASS_DISEASE_FREE:
        # Disease Free Leaves: 1.0 = Stage 0, <1.0 = Stage 1
        if is_equal(confidence, 1.0):
            return 0  # 1.0 = Stage 0 (Super Healthy)
        return 1  # <1.0 = Stage 1 (Healthy)
    elif normalized == CLASS_LEAF_SPOT:
        # Leaf Spot staging
        if is_equal(confidence, 1.0):
            return 6
        elif is_equal(confidence, 0.99):
            return 5
        elif confidence >= 0.95:
            return 4
        elif confidence >= 0.90:
            return 3
        return 2
    elif normalized == CLASS_LEAF_RUST:
        # Leaf Rust staging (different thresholds)
        if is_equal(confidence, 1.0):
            return 6
        elif confidence >= 0.95:
            return 5
        elif confidence >= 0.90:
            return 4
        elif confidence >= 0.76:
            return 3
        return 2

    return -1  # Unknown class


def get_display_result(class_name, confidence):
    """
    Get the classification result display text.
    Mirrors how the Android app displays the result to users.
    """
    if class_name == CLASS_DISEASE_FREE:
        if is_equal(confidence, 1.0):
            return "Healthy"
        else:
            return "Early Spot Detected"
    elif class_name == CLASS_LEAF_RUST:
        if confidence < 0.79:
            return "Early Leaf Rust Detected"
    # Add more conditions as needed
    return ""  # Default: no special display


def run_inference(interpreter, input_details, output_details, image_array):
    """Run inference on preprocessed image."""
    interpreter.set_tensor(input_details[0]['index'], image_array)
    interpreter.invoke()
    output = interpreter.get_tensor(output_details[0]['index'])
    return output[0]  # Remove batch dimension


def classify_image(interpreter, input_details, output_details, target_size, image_path, labels):
    """
    Classify a single image and return results.
    Mirrors the complete Android app classification pipeline (commit 61e8cf7).
    Note: No remapping at this version - Leaf Rust and Leaf Spot are separate.
    """
    # Preprocess image (matches Java ImageProcessor pipeline)
    img_array = preprocess_image(image_path, target_size)

    # Run inference
    probabilities = run_inference(interpreter, input_details, output_details, img_array)

    # Get prediction (matches Java: Collections.max and entry iteration)
    predicted_idx = np.argmax(probabilities)
    confidence = float(probabilities[predicted_idx])
    classification = labels[predicted_idx]

    # Calculate disease stage (matches Java DiseaseStageCalculator)
    stage = calculate_disease_stage(classification, confidence)

    return {
        'classification': classification,
        'confidence': confidence,
        'stage': stage,
        'probabilities': probabilities
    }


def main():
    # Paths
    project_root = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
    model_path = os.path.join(project_root, "app", "src", "main", "ml", "FinalMobilenetFold1.tflite")

    # Use command line argument if provided, otherwise use default
    if len(sys.argv) > 1:
        images_dir = sys.argv[1]
    else:
        images_dir = r"C:\Users\DICT\Downloads\MULBERRY PLANT (PICTURES)\Stage 2"

    # Labels (must match labels.txt exactly - same order as Android app)
    labels = [
        CLASS_DISEASE_FREE,  # Index 0 - "Disease Free Leaves" (healthy)
        CLASS_LEAF_RUST,     # Index 1 - "Potential Leaf Rust"
        CLASS_LEAF_SPOT      # Index 2 - "Potential Leaf Spot"
    ]

    # Check paths exist
    if not os.path.exists(model_path):
        print(f"Error: Model not found at {model_path}")
        sys.exit(1)

    if not os.path.exists(images_dir):
        print(f"Error: Images directory not found at {images_dir}")
        sys.exit(1)

    # Load model
    print("Loading TFLite model...")
    interpreter, input_details, output_details, target_size = load_model(model_path)
    print(f"Model input size: {target_size[0]}x{target_size[1]}")
    print()

    # Get list of HEIC files
    heic_files = sorted([f for f in os.listdir(images_dir) if f.upper().endswith('.HEIC')])

    if not heic_files:
        print("No HEIC files found in the directory.")
        sys.exit(1)

    print(f"Found {len(heic_files)} HEIC files to process.\n")

    # Results storage
    results = []

    # Process each image
    for filename in heic_files:
        image_path = os.path.join(images_dir, filename)

        try:
            # Classify image using complete Android-matching pipeline
            result = classify_image(
                interpreter, input_details, output_details,
                target_size, image_path, labels
            )

            results.append({
                'filename': filename,
                'classification': result['classification'],
                'confidence': result['confidence'],
                'stage': result['stage'],
                'probabilities': result['probabilities']
            })

            print(f"Processed: {filename}")

        except Exception as e:
            print(f"Error processing {filename}: {e}")
            results.append({
                'filename': filename,
                'classification': 'ERROR',
                'confidence': 0.0,
                'stage': -1,
                'error': str(e)
            })

    # Print results table
    print("\n" + "=" * 100)
    print("RESULTS TABLE (Mirroring Android App Pipeline)")
    print("=" * 100)
    print()
    print(f"| {'Filename':<15} | {'Classification':<22} | {'Conf':<8} | {'Stage':<5} | {'Result Display':<20} |")
    print(f"|{'-'*17}|{'-'*24}|{'-'*10}|{'-'*7}|{'-'*22}|")

    for r in results:
        conf_str = f"{r['confidence']:.4f}" if r['confidence'] > 0 else "N/A"
        stage_str = str(r['stage']) if r['stage'] >= 0 else "N/A"
        cls = r['classification'][:22]
        display_result = get_display_result(r['classification'], r['confidence'])
        print(f"| {r['filename']:<15} | {cls:<22} | {conf_str:<8} | {stage_str:<5} | {display_result:<20} |")

    # Summary statistics
    print()
    print("=" * 80)
    print("SUMMARY")
    print("=" * 80)

    valid_results = [r for r in results if r['stage'] >= 0]

    if valid_results:
        # Count by classification
        class_counts = {}
        for r in valid_results:
            cls = r['classification']
            class_counts[cls] = class_counts.get(cls, 0) + 1

        print("\nClassification Distribution:")
        for cls, count in sorted(class_counts.items()):
            percentage = (count / len(valid_results)) * 100
            print(f"  {cls}: {count} ({percentage:.1f}%)")

        # Count by stage
        stage_counts = {}
        for r in valid_results:
            stage = r['stage']
            stage_counts[stage] = stage_counts.get(stage, 0) + 1

        print("\nStage Distribution:")
        for stage in sorted(stage_counts.keys()):
            count = stage_counts[stage]
            percentage = (count / len(valid_results)) * 100
            print(f"  Stage {stage}: {count} ({percentage:.1f}%)")

        # Average confidence
        avg_confidence = np.mean([r['confidence'] for r in valid_results])
        print(f"\nAverage Confidence: {avg_confidence:.4f}")

        # Count healthy vs diseased
        healthy_count = sum(1 for r in valid_results if r['classification'] == CLASS_DISEASE_FREE)
        diseased_count = len(valid_results) - healthy_count
        print(f"\nHealthy: {healthy_count}, Diseased: {diseased_count}")


if __name__ == "__main__":
    main()
