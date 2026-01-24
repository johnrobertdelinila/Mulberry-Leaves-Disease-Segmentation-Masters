package com.example.mulberrydiseaseclassifier;

/**
 * Data class encapsulating classification results including disease stage.
 * Contains all information needed to display results to the user.
 */
public class ClassificationResult {
    private String className;
    private float confidence;
    private long processingTimeMs;
    private int stage;

    /**
     * Constructor for classification result
     * @param className The predicted disease class name
     * @param confidence The model's confidence score (0.0 - 1.0)
     * @param processingTimeMs Time taken for inference in milliseconds
     * @param stage Disease stage (0-6), or -1 if staging disabled
     */
    public ClassificationResult(String className, float confidence,
                               long processingTimeMs, int stage) {
        this.className = className;
        this.confidence = confidence;
        this.processingTimeMs = processingTimeMs;
        this.stage = stage;
    }

    // Getters
    public String getClassName() {
        return className;
    }

    public float getConfidence() {
        return confidence;
    }

    public long getProcessingTimeMs() {
        return processingTimeMs;
    }

    public int getStage() {
        return stage;
    }

    /**
     * Get formatted button text based on staging preference
     * @param includeStaging Whether to include stage in the text
     * @return Formatted text for button display
     */
    public String getFormattedButtonText(boolean includeStaging) {
        if (includeStaging && stage >= 0) {
            return className + " - Stage " + stage;
        }
        return className;
    }

    /**
     * Get formatted accuracy display text
     * @return Formatted accuracy string
     */
    public String getFormattedAccuracy() {
        return "Accuracy: " + confidence;
    }

    /**
     * Get formatted processing time display text
     * @return Formatted time string
     */
    public String getFormattedTime() {
        return "Time: " + processingTimeMs + " ms";
    }
}
