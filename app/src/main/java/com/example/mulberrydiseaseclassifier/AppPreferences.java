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
}
