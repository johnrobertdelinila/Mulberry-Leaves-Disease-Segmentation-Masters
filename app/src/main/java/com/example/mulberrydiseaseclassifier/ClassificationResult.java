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
    private String explanation;  // Claude's reasoning (null for ML model)
    private boolean fromClaude;  // Flag to identify source

    /**
     * Constructor for ML model classification result
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
        this.explanation = null;
        this.fromClaude = false;
    }

    /**
     * Constructor for Claude API classification result
     * @param className The predicted disease class name
     * @param confidence The model's confidence score (not applicable for Claude, use -1)
     * @param processingTimeMs Time taken for API call in milliseconds
     * @param stage Disease stage (0-6)
     * @param explanation Claude's reasoning explanation
     * @param fromClaude Flag indicating this result is from Claude API
     */
    public ClassificationResult(String className, float confidence,
                               long processingTimeMs, int stage,
                               String explanation, boolean fromClaude) {
        this.className = className;
        this.confidence = confidence;
        this.processingTimeMs = processingTimeMs;
        this.stage = stage;
        this.explanation = explanation;
        this.fromClaude = fromClaude;
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

    public String getExplanation() {
        return explanation;
    }

    public boolean isFromClaude() {
        return fromClaude;
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
     * Get stage-only display text for the button
     * Shows descriptive message for healthy leaves, "Stage X" for stages 1-6
     * @return Stage display text
     */
    public String getStageDisplayText() {
        if (stage < 0) return "Check";
        if (stage == 0) return "The uploaded image shows no visible symptoms of leaf spot disease based on image analysis";
        return "Stage " + stage;
    }

    /**
     * Get user-friendly display name for the classification
     * @return Friendly class name
     */
    public String getDisplayClassName() {
        if (className == null) return "Unknown";
        switch (className) {
            case "Disease Free Leaves":
                return "No Visible Leaf Spot Detected";
            case "Potential Leaf Rust":
                return "Early Spot Detected";
            case "Potential Leaf Spot":
                return "Potential Leaf Spot";
            default:
                return className;
        }
    }

    /**
     * Check if the classification indicates a healthy leaf
     * @return true if stage is 0 (healthy)
     */
    public boolean isHealthy() {
        return stage == 0;
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
