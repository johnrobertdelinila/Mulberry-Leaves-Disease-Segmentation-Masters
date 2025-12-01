package com.example.mulberrydiseaseclassifier;

/**
 * Utility class for calculating disease progression stages based on classification results.
 * Stage calculation is internal logic and not exposed to end users.
 */
public class DiseaseStageCalculator {

    // Class name constants (must match labels.txt exactly)
    public static final String CLASS_DISEASE_FREE = "Disease Free Leaves";
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

        if (normalized.equals(CLASS_DISEASE_FREE)) {
            return calculateDiseaseFreeLeavesStage(confidence);
        } else if (normalized.equals(CLASS_LEAF_SPOT)) {
            return calculateLeafSpotStage(confidence);
        } else if (normalized.equals(CLASS_LEAF_RUST)) {
            return calculateLeafRustStage(confidence);
        }

        return -1; // Unknown class
    }

    /**
     * Disease Free Leaves staging:
     * - 1.0 → Stage 0 (Super Healthy)
     * - <1.0 → Stage 1
     */
    private static int calculateDiseaseFreeLeavesStage(float confidence) {
        if (isEqual(confidence, 1.0f)) {
            return 0;
        }
        return 1;
    }

    /**
     * Potential Leaf Spot staging:
     * - 1.0 → Stage 6
     * - 0.99 → Stage 5
     * - 0.98 - 0.95 → Stage 4
     * - 0.94 - 0.90 → Stage 3
     * - <0.90 → Stage 2
     */
    private static int calculateLeafSpotStage(float confidence) {
        if (isEqual(confidence, 1.0f)) {
            return 6;
        } else if (isEqual(confidence, 0.99f)) {
            return 5;
        } else if (confidence >= 0.95f) {
            return 4;
        } else if (confidence >= 0.90f) {
            return 3;
        }
        return 2;
    }

    /**
     * Potential Leaf Rust staging:
     * - 1.0 → Stage 6
     * - 0.99 - 0.95 → Stage 5
     * - 0.94 - 0.90 → Stage 4
     * - 0.89 - 0.76 → Stage 3
     * - <0.76 → Stage 2
     */
    private static int calculateLeafRustStage(float confidence) {
        if (isEqual(confidence, 1.0f)) {
            return 6;
        } else if (confidence >= 0.95f) {
            return 5;
        } else if (confidence >= 0.90f) {
            return 4;
        } else if (confidence >= 0.76f) {
            return 3;
        }
        return 2;
    }

    /**
     * Compare floats with epsilon tolerance for floating point precision issues
     */
    private static boolean isEqual(float a, float b) {
        return Math.abs(a - b) < EPSILON;
    }
}
