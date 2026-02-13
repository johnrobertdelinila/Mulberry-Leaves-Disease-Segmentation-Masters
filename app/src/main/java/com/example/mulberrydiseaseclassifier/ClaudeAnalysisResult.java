package com.example.mulberrydiseaseclassifier;

/**
 * Data class for Claude API analysis response.
 * Represents the parsed JSON response from Claude's leaf analysis.
 */
public class ClaudeAnalysisResult {
    private String status;      // "Healthy", "Leaf Spot", or "Leaf Rust"
    private int stage;          // 0-6 disease severity
    private String confidence;  // "High", "Medium", or "Low"
    private String explanation; // Brief explanation from Claude

    public ClaudeAnalysisResult() {
        // Default constructor for Gson
    }

    public ClaudeAnalysisResult(String status, int stage, String confidence, String explanation) {
        this.status = status;
        this.stage = stage;
        this.confidence = confidence;
        this.explanation = explanation;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getStage() {
        return stage;
    }

    public void setStage(int stage) {
        this.stage = stage;
    }

    public String getConfidence() {
        return confidence;
    }

    public void setConfidence(String confidence) {
        this.confidence = confidence;
    }

    public String getExplanation() {
        return explanation;
    }

    public void setExplanation(String explanation) {
        this.explanation = explanation;
    }

    /**
     * Convert Claude status to ML model class name format for consistency
     * @return Class name matching ML model output format
     */
    public String toClassName() {
        if (status == null) return "Unknown";
        switch (status) {
            case "Healthy":
                return "Disease Free Leaves";
            case "Leaf Spot":
                return "Potential Leaf Spot";
            case "Leaf Rust":
                return "Potential Leaf Rust";
            case "Not a Leaf":
                return "Not a Leaf";
            default:
                return status;
        }
    }

    /**
     * Convert to ClassificationResult for unified UI handling
     * @param processingTimeMs Time taken for the API call
     * @return ClassificationResult compatible with existing UI
     */
    public ClassificationResult toClassificationResult(long processingTimeMs) {
        return new ClassificationResult(
            toClassName(),
            mapConfidenceToNumeric(confidence),
            processingTimeMs,
            stage,
            explanation,
            true // fromClaude flag
        );
    }

    private float mapConfidenceToNumeric(String textConfidence) {
        if (textConfidence == null) return 0.50f;
        switch (textConfidence) {
            case "High":   return 0.95f;
            case "Medium": return 0.75f;
            case "Low":    return 0.50f;
            default:       return 0.50f;
        }
    }
}
