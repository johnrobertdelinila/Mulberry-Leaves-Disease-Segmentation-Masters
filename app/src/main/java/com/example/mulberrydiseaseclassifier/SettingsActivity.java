package com.example.mulberrydiseaseclassifier;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.switchmaterial.SwitchMaterial;

/**
 * Settings activity for configuring app preferences.
 * Provides UI controls for toggling disease staging, processing time, and confidence score display.
 */
public class SettingsActivity extends AppCompatActivity {

    private SwitchMaterial stagingSwitch;
    private SwitchMaterial timeSwitch;
    private SwitchMaterial accuracySwitch;
    private SwitchMaterial advancedModeSwitch;
    private View backButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        // Initialize views
        stagingSwitch = findViewById(R.id.settings_staging_switch);
        timeSwitch = findViewById(R.id.settings_time_switch);
        accuracySwitch = findViewById(R.id.settings_accuracy_switch);
        advancedModeSwitch = findViewById(R.id.settings_advanced_mode_switch);
        backButton = findViewById(R.id.settings_back_button);

        // Load current preference values
        stagingSwitch.setChecked(AppPreferences.isStagingEnabled(this));
        timeSwitch.setChecked(AppPreferences.isTimeEnabled(this));
        accuracySwitch.setChecked(AppPreferences.isAccuracyEnabled(this));
        advancedModeSwitch.setChecked(AppPreferences.isClaudeEnabled(this));

        // Set up staging switch listener
        stagingSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            AppPreferences.setStagingEnabled(SettingsActivity.this, isChecked);
            String message = isChecked
                ? getString(R.string.settings_staging_enabled_toast)
                : getString(R.string.settings_staging_disabled_toast);
            Toast.makeText(SettingsActivity.this, message, Toast.LENGTH_SHORT).show();
        });

        // Set up time switch listener
        timeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            AppPreferences.setTimeEnabled(SettingsActivity.this, isChecked);
            String message = isChecked
                ? getString(R.string.settings_time_enabled_toast)
                : getString(R.string.settings_time_disabled_toast);
            Toast.makeText(SettingsActivity.this, message, Toast.LENGTH_SHORT).show();
        });

        // Set up accuracy switch listener
        accuracySwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            AppPreferences.setAccuracyEnabled(SettingsActivity.this, isChecked);
            String message = isChecked
                ? getString(R.string.settings_accuracy_enabled_toast)
                : getString(R.string.settings_accuracy_disabled_toast);
            Toast.makeText(SettingsActivity.this, message, Toast.LENGTH_SHORT).show();
        });

        // Set up advanced mode switch listener
        advancedModeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            AppPreferences.setClaudeEnabled(SettingsActivity.this, isChecked);
            String message = isChecked
                ? getString(R.string.settings_advanced_mode_enabled_toast)
                : getString(R.string.settings_advanced_mode_disabled_toast);
            Toast.makeText(SettingsActivity.this, message, Toast.LENGTH_SHORT).show();
        });

        // Set up back button
        backButton.setOnClickListener(v -> onBackPressed());
    }
}
