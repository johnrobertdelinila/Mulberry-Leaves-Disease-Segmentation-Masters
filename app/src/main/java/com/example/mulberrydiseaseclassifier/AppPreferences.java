package com.example.mulberrydiseaseclassifier;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Centralized application preferences management.
 * Handles storage and retrieval of app settings using SharedPreferences.
 */
public class AppPreferences {

    private static final String PREF_NAME = "MulberryDiseaseClassifierPrefs";
    private static final String KEY_STAGING_ENABLED = "disease_staging_enabled";
    private static final boolean DEFAULT_STAGING_ENABLED = true;
    private static final String KEY_ONBOARDING_SEEN = "onboarding_seen";
    private static final String KEY_TIME_ENABLED = "processing_time_enabled";
    private static final boolean DEFAULT_TIME_ENABLED = false;
    private static final String KEY_ACCURACY_ENABLED = "confidence_score_enabled";
    private static final boolean DEFAULT_ACCURACY_ENABLED = false;
    private static final String KEY_CLAUDE_ENABLED = "claude_analysis_enabled";
    private static final boolean DEFAULT_CLAUDE_ENABLED = false;
    private static final String KEY_CURRENT_API_KEY_INDEX = "current_api_key_index";

    /**
     * Check if disease staging is enabled
     * @param context Application context
     * @return true if staging enabled, false otherwise
     */
    public static boolean isStagingEnabled(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(KEY_STAGING_ENABLED, DEFAULT_STAGING_ENABLED);
    }

    /**
     * Enable or disable disease staging
     * @param context Application context
     * @param enabled true to enable staging, false to disable
     */
    public static void setStagingEnabled(Context context, boolean enabled) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit()
             .putBoolean(KEY_STAGING_ENABLED, enabled)
             .apply();
    }

    /**
     * Check if user has seen the onboarding flow
     * @param context Application context
     * @return true if onboarding has been seen
     */
    public static boolean hasSeenOnboarding(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(KEY_ONBOARDING_SEEN, false);
    }

    /**
     * Mark onboarding as seen
     * @param context Application context
     */
    public static void setOnboardingSeen(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit()
             .putBoolean(KEY_ONBOARDING_SEEN, true)
             .apply();
    }

    /**
     * Check if processing time display is enabled
     * @param context Application context
     * @return true if time display enabled, false otherwise
     */
    public static boolean isTimeEnabled(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(KEY_TIME_ENABLED, DEFAULT_TIME_ENABLED);
    }

    /**
     * Enable or disable processing time display
     * @param context Application context
     * @param enabled true to enable time display, false to disable
     */
    public static void setTimeEnabled(Context context, boolean enabled) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit()
             .putBoolean(KEY_TIME_ENABLED, enabled)
             .apply();
    }

    /**
     * Check if confidence score display is enabled
     * @param context Application context
     * @return true if accuracy display enabled, false otherwise
     */
    public static boolean isAccuracyEnabled(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(KEY_ACCURACY_ENABLED, DEFAULT_ACCURACY_ENABLED);
    }

    /**
     * Enable or disable confidence score display
     * @param context Application context
     * @param enabled true to enable accuracy display, false to disable
     */
    public static void setAccuracyEnabled(Context context, boolean enabled) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit()
             .putBoolean(KEY_ACCURACY_ENABLED, enabled)
             .apply();
    }

    /**
     * Check if Claude AI analysis is enabled
     * @param context Application context
     * @return true if Claude analysis enabled, false otherwise
     */
    public static boolean isClaudeEnabled(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(KEY_CLAUDE_ENABLED, DEFAULT_CLAUDE_ENABLED);
    }

    /**
     * Enable or disable Claude AI analysis
     * @param context Application context
     * @param enabled true to enable Claude analysis, false to disable
     */
    public static void setClaudeEnabled(Context context, boolean enabled) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit()
             .putBoolean(KEY_CLAUDE_ENABLED, enabled)
             .apply();
    }

    /**
     * Get current API key index for smart key rotation
     * @param context Application context
     * @return Current API key index (0, 1, or 2)
     */
    public static int getCurrentApiKeyIndex(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getInt(KEY_CURRENT_API_KEY_INDEX, 0);
    }

    /**
     * Set current API key index for smart key rotation
     * @param context Application context
     * @param index API key index to use (0, 1, or 2)
     */
    public static void setCurrentApiKeyIndex(Context context, int index) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit()
             .putInt(KEY_CURRENT_API_KEY_INDEX, index)
             .apply();
    }

    /**
     * Reset API key index to 0 (for fresh start after exhaustion)
     * @param context Application context
     */
    public static void resetApiKeyIndex(Context context) {
        setCurrentApiKeyIndex(context, 0);
    }
}
