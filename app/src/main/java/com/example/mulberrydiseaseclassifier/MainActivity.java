package com.example.mulberrydiseaseclassifier;

import android.Manifest;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.google.android.material.card.MaterialCardView;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    private static final int CAMERA_REQUEST = 1888;
    private static final int MY_CAMERA_PERMISSION_CODE = 100;

    private View settingsButton;
    private LinearLayout brandingSection;
    private LinearLayout heroSection;
    private MaterialCardView actionCard;
    private LinearLayout actionScan;
    private LinearLayout actionGallery;

    // For full-resolution camera capture
    private Uri photoUri;
    private String currentPhotoPath;

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
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        // Create file for full-resolution photo
        File photoFile = null;
        try {
            photoFile = createImageFile();
        } catch (IOException ex) {
            Log.e(TAG, "Error creating image file", ex);
            Toast.makeText(this, "Error creating image file", Toast.LENGTH_SHORT).show();
            return;
        }

        if (photoFile != null) {
            photoUri = FileProvider.getUriForFile(this,
                    getPackageName() + ".fileprovider",
                    photoFile);
            cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);

            // Grant URI permissions to camera app
            cameraIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            cameraIntent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);

            startActivityForResult(cameraIntent, CAMERA_REQUEST);
        }
    }

    /**
     * Create a temporary file for the camera image
     */
    private File createImageFile() throws IOException {
        // Create image file name with timestamp
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";

        // Use cache directory for temporary storage
        File storageDir = new File(getCacheDir(), "images");
        if (!storageDir.exists()) {
            storageDir.mkdirs();
        }

        File image = File.createTempFile(imageFileName, ".jpg", storageDir);
        currentPhotoPath = image.getAbsolutePath();
        Log.d(TAG, "Created image file: " + currentPhotoPath);
        return image;
    }

    /**
     * Load and downsample bitmap to prevent OOM errors
     * @param imagePath Path to the image file
     * @param maxDimension Maximum dimension (width or height)
     * @return Downsampled bitmap
     */
    private Bitmap loadAndDownsampleImage(String imagePath, int maxDimension) {
        // First, get the image dimensions without loading the full bitmap
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(imagePath, options);

        int originalWidth = options.outWidth;
        int originalHeight = options.outHeight;
        Log.d(TAG, "Original image size: " + originalWidth + "x" + originalHeight);

        // Calculate inSampleSize
        int inSampleSize = 1;
        if (originalWidth > maxDimension || originalHeight > maxDimension) {
            int halfWidth = originalWidth / 2;
            int halfHeight = originalHeight / 2;

            while ((halfWidth / inSampleSize) >= maxDimension
                    && (halfHeight / inSampleSize) >= maxDimension) {
                inSampleSize *= 2;
            }
        }

        // Decode with inSampleSize
        options.inJustDecodeBounds = false;
        options.inSampleSize = inSampleSize;
        Bitmap sampledBitmap = BitmapFactory.decodeFile(imagePath, options);

        if (sampledBitmap == null) {
            Log.e(TAG, "Failed to decode bitmap from: " + imagePath);
            return null;
        }

        // Further scale if needed to exact max dimension
        int width = sampledBitmap.getWidth();
        int height = sampledBitmap.getHeight();
        Log.d(TAG, "Sampled bitmap size: " + width + "x" + height);

        if (width > maxDimension || height > maxDimension) {
            float scale = Math.min(
                    (float) maxDimension / width,
                    (float) maxDimension / height
            );
            int newWidth = Math.round(width * scale);
            int newHeight = Math.round(height * scale);

            Bitmap scaledBitmap = Bitmap.createScaledBitmap(sampledBitmap, newWidth, newHeight, true);
            if (scaledBitmap != sampledBitmap) {
                sampledBitmap.recycle();
            }
            Log.d(TAG, "Final bitmap size: " + newWidth + "x" + newHeight);
            return scaledBitmap;
        }

        return sampledBitmap;
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
        if (requestCode == CAMERA_REQUEST && resultCode == Activity.RESULT_OK) {
            // Pass only the file path to avoid TransactionTooLargeException
            // The image will be loaded in MulberryScannerClassifierActivity
            if (currentPhotoPath != null) {
                Log.d(TAG, "Passing photo path to classifier: " + currentPhotoPath);
                Intent intent = new Intent(MainActivity.this, MulberryScannerClassifierActivity.class);
                intent.putExtra("photoPath", currentPhotoPath);
                startActivity(intent);
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
            } else {
                Toast.makeText(this, "Failed to capture image!", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
