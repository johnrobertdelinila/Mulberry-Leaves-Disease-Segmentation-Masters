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
    private static final String KEY_HAS_SEEN_ONBOARDING = "has_seen_onboarding";
    private static final boolean DEFAULT_STAGING_ENABLED = true;
    private static final boolean DEFAULT_HAS_SEEN_ONBOARDING = false;

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
     * @return true if onboarding has been seen, false otherwise
     */
    public static boolean hasSeenOnboarding(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(KEY_HAS_SEEN_ONBOARDING, DEFAULT_HAS_SEEN_ONBOARDING);
    }

    /**
     * Mark onboarding as seen
     * @param context Application context
     */
    public static void setOnboardingSeen(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit()
             .putBoolean(KEY_HAS_SEEN_ONBOARDING, true)
             .apply();
    }
}
