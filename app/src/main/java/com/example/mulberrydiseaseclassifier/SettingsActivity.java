package com.example.mulberrydiseaseclassifier;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.switchmaterial.SwitchMaterial;

/**
 * Settings activity for configuring app preferences.
 * Provides UI controls for toggling disease staging feature.
 */
public class SettingsActivity extends AppCompatActivity {

    private SwitchMaterial stagingSwitch;
    private ImageView backButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        // Initialize views
        stagingSwitch = findViewById(R.id.settings_staging_switch);
        backButton = findViewById(R.id.settings_back_button);

        // Load current preference value
        boolean stagingEnabled = AppPreferences.isStagingEnabled(this);
        stagingSwitch.setChecked(stagingEnabled);

        // Set up switch listener
        stagingSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            // Save preference
            AppPreferences.setStagingEnabled(SettingsActivity.this, isChecked);

            // Show confirmation toast
            String message = isChecked
                ? getString(R.string.settings_staging_enabled_toast)
                : getString(R.string.settings_staging_disabled_toast);
            Toast.makeText(SettingsActivity.this, message, Toast.LENGTH_SHORT).show();
        });

        // Set up back button
        backButton.setOnClickListener(v -> onBackPressed());
    }
}
