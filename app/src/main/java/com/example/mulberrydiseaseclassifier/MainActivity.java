package com.example.mulberrydiseaseclassifier;

import android.Manifest;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.card.MaterialCardView;

import java.io.ByteArrayOutputStream;

public class MainActivity extends AppCompatActivity {

    private static final int CAMERA_REQUEST = 1888;
    private static final int MY_CAMERA_PERMISSION_CODE = 100;

    private View settingsButton;
    private LinearLayout brandingSection;
    private LinearLayout heroSection;
    private MaterialCardView actionCard;
    private LinearLayout actionScan;
    private LinearLayout actionGallery;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Make status bar transparent with light icons
        getWindow().setStatusBarColor(ContextCompat.getColor(this, android.R.color.transparent));
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);

        // Initialize views
        settingsButton = findViewById(R.id.settingsButton);
        brandingSection = findViewById(R.id.brandingSection);
        heroSection = findViewById(R.id.heroSection);
        actionCard = findViewById(R.id.actionCard);
        actionScan = findViewById(R.id.actionScan);
        actionGallery = findViewById(R.id.actionGallery);

        // Set up click listeners
        setupClickListeners();

        // Play entrance animations
        playEntranceAnimations();
    }

    private void setupClickListeners() {
        // Settings button
        settingsButton.setOnClickListener(v -> {
            animateButtonPress(v);
            Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        });

        // Scan action (camera)
        actionScan.setOnClickListener(v -> {
            animateButtonPress(v);
            if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.CAMERA}, MY_CAMERA_PERMISSION_CODE);
            } else {
                launchCamera();
            }
        });

        // Gallery action
        actionGallery.setOnClickListener(v -> {
            animateButtonPress(v);
            Intent intent = new Intent(MainActivity.this, MulberryDiseaseClassifierActivity.class);
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        });
    }

    private void launchCamera() {
        Intent cameraIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(cameraIntent, CAMERA_REQUEST);
    }

    private void animateButtonPress(View view) {
        ObjectAnimator scaleDownX = ObjectAnimator.ofFloat(view, "scaleX", 1f, 0.95f);
        ObjectAnimator scaleDownY = ObjectAnimator.ofFloat(view, "scaleY", 1f, 0.95f);
        scaleDownX.setDuration(100);
        scaleDownY.setDuration(100);

        ObjectAnimator scaleUpX = ObjectAnimator.ofFloat(view, "scaleX", 0.95f, 1f);
        ObjectAnimator scaleUpY = ObjectAnimator.ofFloat(view, "scaleY", 0.95f, 1f);
        scaleUpX.setDuration(100);
        scaleUpY.setDuration(100);

        AnimatorSet scaleDown = new AnimatorSet();
        scaleDown.playTogether(scaleDownX, scaleDownY);

        AnimatorSet scaleUp = new AnimatorSet();
        scaleUp.playTogether(scaleUpX, scaleUpY);

        AnimatorSet bounce = new AnimatorSet();
        bounce.playSequentially(scaleDown, scaleUp);
        bounce.start();
    }

    private void playEntranceAnimations() {
        // Set initial state - invisible and translated down
        settingsButton.setAlpha(0f);
        brandingSection.setAlpha(0f);
        heroSection.setAlpha(0f);
        actionCard.setAlpha(0f);

        brandingSection.setTranslationY(30f);
        heroSection.setTranslationY(30f);
        actionCard.setTranslationY(50f);

        DecelerateInterpolator interpolator = new DecelerateInterpolator();
        int baseDuration = 500;
        int staggerDelay = 100;

        // Settings button fade in
        settingsButton.animate()
                .alpha(1f)
                .setDuration(baseDuration)
                .setInterpolator(interpolator)
                .start();

        // Branding section
        brandingSection.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(baseDuration)
                .setStartDelay(staggerDelay)
                .setInterpolator(interpolator)
                .start();

        // Hero section
        heroSection.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(baseDuration)
                .setStartDelay(staggerDelay * 2)
                .setInterpolator(interpolator)
                .start();

        // Action card
        actionCard.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(baseDuration)
                .setStartDelay(staggerDelay * 3)
                .setInterpolator(interpolator)
                .start();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == MY_CAMERA_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Camera permission granted!", Toast.LENGTH_SHORT).show();
                launchCamera();
            } else {
                Toast.makeText(this, "Camera permission denied!", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CAMERA_REQUEST && resultCode == Activity.RESULT_OK && data != null && data.getExtras() != null) {
            Bitmap photo = (Bitmap) data.getExtras().get("data");
            if (photo != null) {
                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                photo.compress(Bitmap.CompressFormat.PNG, 100, stream);
                byte[] byteArray = stream.toByteArray();
                Intent intent = new Intent(MainActivity.this, MulberryScannerClassifierActivity.class);
                intent.putExtra("image", byteArray);
                startActivity(intent);
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
            } else {
                Toast.makeText(this, "Failed to capture image!", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
