package com.example.mulberrydiseaseclassifier;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Base64;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Service for handling Google Gemini API calls for leaf disease analysis.
 * Uses Gemini 2.5 Flash model for fast, free analysis.
 * Implements smart API key rotation to handle rate limits during thesis defense.
 */
public class GeminiApiService {
    private static final String TAG = "GeminiApiService";
    private static final String API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent";

    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private final OkHttpClient client;
    private final Gson gson;
    private final Context context;
    private final String[] apiKeys;

    /**
     * Analysis prompt for Gemini to analyze mulberry leaf images.
     */
    private static final String ANALYSIS_PROMPT =
        "You are an expert plant pathologist analyzing an image.\n\n" +
        "FIRST, determine if the image contains a mulberry leaf. If NOT, return \"Not a Leaf\".\n\n" +
        "CLASSIFICATION RULES:\n" +
        "- \"Not a Leaf\": Image does NOT contain a mulberry leaf (e.g., other objects, non-plant items, other plants)\n" +
        "- \"Healthy\": Green mulberry leaf with NO visible spots, lesions, discoloration, or rust pustules\n" +
        "- \"Leaf Spot\": Mulberry leaf with circular/irregular brown, black, or tan spots\n" +
        "- \"Leaf Rust\": Mulberry leaf with orange, yellow, or reddish-brown powdery pustules\n\n" +
        "IMPORTANT: If the image is NOT a mulberry leaf, always return \"Not a Leaf\" with stage -1.\n\n" +
        "Disease severity stages (only for actual leaves):\n" +
        "- Stage -1: Not a leaf\n" +
        "- Stage 0: Healthy, no disease\n" +
        "- Stage 1-2: Early/mild infection\n" +
        "- Stage 3-4: Moderate infection\n" +
        "- Stage 5-6: Severe infection\n\n" +
        "Respond ONLY with valid JSON:\n" +
        "{\n" +
        "  \"status\": \"Not a Leaf\" | \"Healthy\" | \"Leaf Spot\" | \"Leaf Rust\",\n" +
        "  \"stage\": -1 to 6,\n" +
        "  \"confidence\": \"High\" | \"Medium\" | \"Low\",\n" +
        "  \"explanation\": \"brief 1-2 sentence explanation\"\n" +
        "}";

    /**
     * Callback interface for async API calls
     */
    public interface GeminiCallback {
        void onSuccess(ClaudeAnalysisResult result, long processingTimeMs);
        void onError(String errorMessage);
        void onRateLimitExhausted();  // Called when all API keys are exhausted
    }

    public GeminiApiService(Context context) {
        this.context = context;
        this.client = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build();
        this.gson = new Gson();
        this.apiKeys = getApiKeys();
    }

    /**
     * Get API keys from BuildConfig for smart rotation
     * @return Array of API keys (6 keys from gradle.properties)
     */
    private static String[] getApiKeys() {
        // Collect all non-empty API keys
        String[] allKeys = {
            BuildConfig.GEMINI_API_KEY_1,
            BuildConfig.GEMINI_API_KEY_2,
            BuildConfig.GEMINI_API_KEY_3,
            BuildConfig.GEMINI_API_KEY_4,
            BuildConfig.GEMINI_API_KEY_5,
            BuildConfig.GEMINI_API_KEY_6
        };

        // Count valid keys
        int validCount = 0;
        for (String key : allKeys) {
            if (key != null && !key.isEmpty()) {
                validCount++;
            }
        }

        // Return only valid keys
        String[] validKeys = new String[validCount];
        int index = 0;
        for (String key : allKeys) {
            if (key != null && !key.isEmpty()) {
                validKeys[index++] = key;
            }
        }

        return validKeys;
    }

    /**
     * Get current API key based on rotation index stored in preferences
     * @return Current API key to use
     */
    private String getCurrentApiKey() {
        int index = AppPreferences.getCurrentApiKeyIndex(context);
        if (index >= 0 && index < apiKeys.length) {
            return apiKeys[index];
        }
        if (apiKeys.length > 0) {
            return apiKeys[0];
        }
        return "";
    }

    /**
     * Check if there are more API keys available to try
     * @return true if more keys available
     */
    private boolean hasMoreKeys() {
        int currentIndex = AppPreferences.getCurrentApiKeyIndex(context);
        return currentIndex < apiKeys.length - 1;
    }

    /**
     * Switch to the next API key
     * @return true if successfully switched, false if no more keys
     */
    private boolean switchToNextKey() {
        int currentIndex = AppPreferences.getCurrentApiKeyIndex(context);
        int nextIndex = currentIndex + 1;

        if (nextIndex < apiKeys.length) {
            AppPreferences.setCurrentApiKeyIndex(context, nextIndex);
            Log.d(TAG, "Switching to API key index: " + nextIndex);
            return true;
        }
        return false;
    }

    /**
     * Check if API keys are configured
     * @return true if API keys are available
     */
    public boolean isApiKeyConfigured() {
        return apiKeys != null && apiKeys.length > 0 && apiKeys[0] != null && !apiKeys[0].isEmpty();
    }

    /**
     * Check if device has internet connectivity
     * @param context Application context
     * @return true if connected to internet
     */
    public static boolean isNetworkAvailable(Context context) {
        ConnectivityManager connectivityManager =
            (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager != null) {
            NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
            return activeNetworkInfo != null && activeNetworkInfo.isConnected();
        }
        return false;
    }

    /**
     * Analyze a leaf image using Gemini API with smart key rotation
     * @param bitmap The image to analyze
     * @param callback Callback for async result
     */
    public void analyzeLeafImage(Bitmap bitmap, GeminiCallback callback) {
        if (!isApiKeyConfigured()) {
            callback.onError("Gemini API keys not configured");
            return;
        }

        // Reset to first key at start of each analysis
        AppPreferences.resetApiKeyIndex(context);
        Log.d(TAG, "Starting analysis with " + apiKeys.length + " available API keys");

        long startTime = System.currentTimeMillis();

        // Convert bitmap to base64
        String base64Image = bitmapToBase64(bitmap);
        if (base64Image == null) {
            callback.onError("Failed to encode image");
            return;
        }

        // Build request body
        String requestJson = buildRequestJson(base64Image);

        // Start analysis with current key
        analyzeWithCurrentKey(requestJson, bitmap, callback, startTime);
    }

    /**
     * Internal method to perform analysis with the current API key
     * Handles rate limit detection and automatic key switching
     */
    private void analyzeWithCurrentKey(String requestJson, Bitmap bitmap, GeminiCallback callback, long startTime) {
        String currentKey = getCurrentApiKey();
        String urlWithKey = API_URL + "?key=" + currentKey;

        Log.d(TAG, "Using API key index: " + AppPreferences.getCurrentApiKeyIndex(context));

        RequestBody body = RequestBody.create(requestJson, JSON);
        Request request = new Request.Builder()
            .url(urlWithKey)
            .addHeader("Content-Type", "application/json")
            .post(body)
            .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "API call failed", e);
                callback.onError("Network error: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                long processingTime = System.currentTimeMillis() - startTime;
                int responseCode = response.code();

                // Check for errors that should trigger key rotation
                // 429 = Rate limit, 400 = Bad request, 401 = Unauthorized, 403 = Forbidden/Quota exceeded
                if (responseCode == 429 || responseCode == 400 || responseCode == 401 || responseCode == 403) {
                    String errorBody = response.body() != null ? response.body().string() : "Unknown error";
                    Log.w(TAG, "API error (" + responseCode + ") on key index " +
                        AppPreferences.getCurrentApiKeyIndex(context) + ": " + errorBody);

                    // Try to switch to next key
                    if (switchToNextKey()) {
                        Log.d(TAG, "Retrying with next API key...");
                        // Retry with new key
                        analyzeWithCurrentKey(requestJson, bitmap, callback, startTime);
                        return;
                    } else {
                        // All keys exhausted - fall back to local ML
                        Log.w(TAG, "All API keys exhausted (last error: " + responseCode + "), invoking fallback");
                        callback.onRateLimitExhausted();
                        return;
                    }
                }

                if (!response.isSuccessful()) {
                    String errorBody = response.body() != null ? response.body().string() : "Unknown error";
                    Log.e(TAG, "API error: " + responseCode + " - " + errorBody);
                    // For other errors (500, 502, etc.), also try next key
                    if (switchToNextKey()) {
                        Log.d(TAG, "Server error, retrying with next API key...");
                        analyzeWithCurrentKey(requestJson, bitmap, callback, startTime);
                        return;
                    }
                    callback.onRateLimitExhausted();
                    return;
                }

                try {
                    String responseBody = response.body().string();
                    Log.d(TAG, "API Response: " + responseBody);

                    ClaudeAnalysisResult result = parseResponse(responseBody);
                    if (result != null) {
                        callback.onSuccess(result, processingTime);
                    } else {
                        // Parse failed - try next key in case response was malformed
                        Log.e(TAG, "Failed to parse response, trying next key");
                        if (switchToNextKey()) {
                            analyzeWithCurrentKey(requestJson, bitmap, callback, startTime);
                            return;
                        }
                        callback.onRateLimitExhausted();
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error parsing response", e);
                    // On exception, try next key
                    if (switchToNextKey()) {
                        analyzeWithCurrentKey(requestJson, bitmap, callback, startTime);
                        return;
                    }
                    callback.onRateLimitExhausted();
                }
            }
        });
    }

    /**
     * Convert bitmap to base64 encoded string
     * @param bitmap Image to encode
     * @return Base64 encoded string or null on error
     */
    private String bitmapToBase64(Bitmap bitmap) {
        try {
            // Resize if too large (max 1024px to reduce API costs)
            Bitmap resized = resizeBitmap(bitmap, 1024);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            resized.compress(Bitmap.CompressFormat.JPEG, 85, baos);
            byte[] imageBytes = baos.toByteArray();

            // Recycle resized bitmap if different from original
            if (resized != bitmap) {
                resized.recycle();
            }

            return Base64.encodeToString(imageBytes, Base64.NO_WRAP);
        } catch (Exception e) {
            Log.e(TAG, "Error encoding image", e);
            return null;
        }
    }

    /**
     * Resize bitmap to max dimension while maintaining aspect ratio
     */
    private Bitmap resizeBitmap(Bitmap original, int maxDimension) {
        int width = original.getWidth();
        int height = original.getHeight();

        if (width <= maxDimension && height <= maxDimension) {
            return original;
        }

        float scale = Math.min(
            (float) maxDimension / width,
            (float) maxDimension / height
        );

        int newWidth = Math.round(width * scale);
        int newHeight = Math.round(height * scale);

        return Bitmap.createScaledBitmap(original, newWidth, newHeight, true);
    }

    /**
     * Build the JSON request body for Gemini API
     * @param base64Image Base64 encoded image
     * @return JSON string for API request
     */
    private String buildRequestJson(String base64Image) {
        // Build the request JSON for Gemini API structure
        StringBuilder json = new StringBuilder();
        json.append("{");
        json.append("\"contents\":[{");
        json.append("\"parts\":[");
        // Image part first
        json.append("{\"inline_data\":{");
        json.append("\"mime_type\":\"image/jpeg\",");
        json.append("\"data\":\"").append(base64Image).append("\"");
        json.append("}},");
        // Text prompt
        json.append("{\"text\":\"").append(escapeJson(ANALYSIS_PROMPT)).append("\"}");
        json.append("]");
        json.append("}]");
        json.append("}");
        return json.toString();
    }

    /**
     * Escape special characters for JSON string
     */
    private String escapeJson(String text) {
        return text
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t");
    }

    /**
     * Parse Gemini API response and extract the analysis result
     * @param responseBody Raw JSON response from API
     * @return Parsed ClaudeAnalysisResult or null on error
     */
    private ClaudeAnalysisResult parseResponse(String responseBody) {
        try {
            // Parse the Gemini API response structure
            JsonObject response = JsonParser.parseString(responseBody).getAsJsonObject();

            // Get the candidates array
            if (!response.has("candidates") || response.getAsJsonArray("candidates").size() == 0) {
                Log.e(TAG, "No candidates in response");
                return null;
            }

            // Get the first candidate
            JsonObject candidate = response.getAsJsonArray("candidates").get(0).getAsJsonObject();

            // Get content object
            if (!candidate.has("content")) {
                Log.e(TAG, "No content in candidate");
                return null;
            }

            JsonObject content = candidate.getAsJsonObject("content");

            // Get parts array
            if (!content.has("parts") || content.getAsJsonArray("parts").size() == 0) {
                Log.e(TAG, "No parts in content");
                return null;
            }

            // Get the text from the first part
            JsonObject part = content.getAsJsonArray("parts").get(0).getAsJsonObject();
            if (!part.has("text")) {
                Log.e(TAG, "No text in part");
                return null;
            }

            String text = part.get("text").getAsString();
            Log.d(TAG, "Gemini response text: " + text);

            // Extract JSON from the response (Gemini might include some explanation text)
            String jsonStr = extractJson(text);
            if (jsonStr == null) {
                Log.e(TAG, "Could not extract JSON from response");
                return null;
            }

            // Parse the analysis result JSON
            return gson.fromJson(jsonStr, ClaudeAnalysisResult.class);

        } catch (Exception e) {
            Log.e(TAG, "Error parsing response", e);
            return null;
        }
    }

    /**
     * Extract JSON object from text that may contain other content
     * @param text Text potentially containing JSON
     * @return Extracted JSON string or null
     */
    private String extractJson(String text) {
        // Try to find JSON object in the text
        Pattern pattern = Pattern.compile("\\{[^{}]*\"status\"[^{}]*\\}");
        Matcher matcher = pattern.matcher(text);

        if (matcher.find()) {
            return matcher.group();
        }

        // If no match with simple pattern, try to find any JSON object
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');

        if (start >= 0 && end > start) {
            return text.substring(start, end + 1);
        }

        return null;
    }
}
