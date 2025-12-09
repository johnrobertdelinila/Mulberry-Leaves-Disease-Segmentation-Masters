package com.example.mulberrydiseaseclassifier;

/**
 * Utility class for calculating disease progression stages based on classification results.
 * Stage calculation is internal logic and not exposed to end users.
 */
public class DiseaseStageCalculator {

    // Class name constants (must match labels.txt exactly)
    public static final String CLASS_EARLY_SPOT = "Early Spot Detected";
    public static final String CLASS_LEAF_SPOT = "Potential Leaf Spot";
    public static final String CLASS_LEAF_RUST = "Potential Leaf Rust";

    // Epsilon for floating point comparisons
    private static final float EPSILON = 0.0001f;

    /**
     * Calculates disease stage based on class name and confidence score.
     * This logic is internal and should not be exposed to end users.
     *
     * @param className The disease classification name
     * @param confidence The model confidence score (0.0 - 1.0)
     * @return Stage number (0-6), or -1 if unknown class
     */
    public static int calculateStage(String className, float confidence) {
        if (className == null) {
            return -1;
        }

        // Normalize for comparison
        String normalized = className.trim();

        if (normalized.equals(CLASS_EARLY_SPOT)) {
            return calculateEarlySpotDetectedStage(confidence);
        } else if (normalized.equals(CLASS_LEAF_SPOT)) {
            return calculateLeafSpotStage(confidence);
        } else if (normalized.equals(CLASS_LEAF_RUST)) {
            return calculateLeafRustStage(confidence);
        }

        return -1; // Unknown class
    }

    /**
     * Early Spot Detected staging:
     * - 1.0 → Stage 0 (Super Healthy)
     * - 0.80-0.99 → Stage 1 (very mild / beginning indicators)
     * - <0.80 → Stage 2 (mild but more pronounced)
     */
    private static int calculateEarlySpotDetectedStage(float confidence) {
        if (isEqual(confidence, 1.0f)) {
            return 0;  // Perfect confidence = Healthy
        } else if (confidence >= 0.80f) {
            return 1;  // 0.80-0.99 = Stage 1 (very mild)
        }
        return 2;      // < 0.80 = Stage 2 (more pronounced)
    }

    /**
     * Potential Leaf Spot staging (same as Leaf Rust):
     * - 1.0 → Stage 6 (severe)
     * - 0.95-0.99 → Stage 5 (high likelihood serious)
     * - 0.85-0.94 → Stage 4 (moderate)
     * - <0.85 → Stage 3 (early/developing)
     */
    private static int calculateLeafSpotStage(float confidence) {
        if (isEqual(confidence, 1.0f)) {
            return 6;  // 1.0 = Stage 6 (severe)
        } else if (confidence >= 0.95f) {
            return 5;  // 0.95-0.99 = Stage 5 (serious)
        } else if (confidence >= 0.85f) {
            return 4;  // 0.85-0.94 = Stage 4 (moderate)
        }
        return 3;      // < 0.85 = Stage 3 (early/developing)
    }

    /**
     * Potential Leaf Rust staging:
     * - 1.0 → Stage 6 (severe)
     * - 0.95-0.99 → Stage 5 (high likelihood serious)
     * - 0.85-0.94 → Stage 4 (moderate)
     * - <0.85 → Stage 3 (early/developing)
     */
    private static int calculateLeafRustStage(float confidence) {
        if (isEqual(confidence, 1.0f)) {
            return 6;  // 1.0 = Stage 6 (severe)
        } else if (confidence >= 0.95f) {
            return 5;  // 0.95-0.99 = Stage 5 (serious)
        } else if (confidence >= 0.85f) {
            return 4;  // 0.85-0.94 = Stage 4 (moderate)
        }
        return 3;      // < 0.85 = Stage 3 (early/developing)
    }

    /**
     * Compare floats with epsilon tolerance for floating point precision issues
     */
    private static boolean isEqual(float a, float b) {
        return Math.abs(a - b) < EPSILON;
    }
}
