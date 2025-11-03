package com.example.mulberrydiseaseclassifier;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;

import java.io.ByteArrayOutputStream;

public class MainActivity extends AppCompatActivity {

    private static final int CAMERA_REQUEST = 1888;
    private static final int MY_CAMERA_PERMISSION_CODE = 100;
    private MaterialButton btnScanPlant, btnLoadImages;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Make status bar transparent
        getWindow().setStatusBarColor(ContextCompat.getColor(this, android.R.color.transparent));
        
        btnScanPlant = findViewById(R.id.btnScanPlant);
        btnLoadImages = findViewById(R.id.loadImages);

        btnLoadImages.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, MulberryDiseaseClassifierActivity.class);
            startActivity(intent);
        });

        btnScanPlant.setOnClickListener(v -> {
            if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.CAMERA}, MY_CAMERA_PERMISSION_CODE);
            } else {
                Intent cameraIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
                startActivityForResult(cameraIntent, CAMERA_REQUEST);
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == MY_CAMERA_PERMISSION_CODE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Camera permission granted!", Toast.LENGTH_LONG).show();
                Intent cameraIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
                startActivityForResult(cameraIntent, CAMERA_REQUEST);
            } else {
                Toast.makeText(this, "Camera permission denied!", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CAMERA_REQUEST && resultCode == Activity.RESULT_OK && data != null && data.getExtras() != null) {
            Bitmap photo = (Bitmap) data.getExtras().get("data");
            if (photo != null) {
                Toast.makeText(this, "Scan successful!", Toast.LENGTH_LONG).show();
                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                photo.compress(Bitmap.CompressFormat.PNG, 100, stream);
                byte[] byteArray = stream.toByteArray();
                Intent intent = new Intent(MainActivity.this, MulberryScannerClassifierActivity.class);
                intent.putExtra("image", byteArray);
                startActivity(intent);
            } else {
                Toast.makeText(this, "Failed to capture image!", Toast.LENGTH_LONG).show();
            }
        } else {
            Toast.makeText(this, "Scan unsuccessful!", Toast.LENGTH_LONG).show();
        }
    }
}