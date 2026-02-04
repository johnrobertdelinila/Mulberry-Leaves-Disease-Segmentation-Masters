package com.example.mulberrydiseaseclassifier;

import static java.lang.Math.min;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.RectF;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.os.Trace;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.google.android.material.card.MaterialCardView;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import org.tensorflow.lite.DataType;
import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.gpu.CompatibilityList;
import org.tensorflow.lite.gpu.GpuDelegate;
import org.tensorflow.lite.support.common.FileUtil;
import org.tensorflow.lite.support.common.TensorOperator;
import org.tensorflow.lite.support.common.TensorProcessor;
import org.tensorflow.lite.support.common.ops.NormalizeOp;
import org.tensorflow.lite.support.image.ImageProcessor;
import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.image.ops.ResizeOp;
import org.tensorflow.lite.support.image.ops.ResizeWithCropOrPadOp;
import org.tensorflow.lite.support.label.TensorLabel;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

public class MulberryScannerClassifierActivity extends Activity {
    private static final String TAG = "MulberryScannerActivity";
    private static final int MY_CAMERA_PERMISSION_CODE = 100;
    private static final int CAMERA_REQUEST = 1888;

    // Track if analysis is complete (for button navigation)
    private boolean analysisComplete = false;

    private TensorImage inputTensorImage;
    private  int imageSizeX;
    private  int imageSizeY;
    private TensorBuffer probabilityImageBuffer;
    private TensorProcessor probabilityProcessor;
    private static final float IMAGE_MEAN = 0.0f;
    private static final float IMAGE_STD = 255.0f;
    private static final float PROBABILITY_MEAN = 0.0f;
    private static final float PROBABILITY_STD = 1.0f;
    private Bitmap bitmap;
    private List<String> labels;
    ImageView imageView;
    Uri imageuri;
    Button classifierBtn;
    TextView classText, accuracyText, timeText, selectImageText, stageValue, actionText;
    private ImageView backBtn, uploadIcon;
    private View placeholderOverlay;
    private MaterialCardView resultsCard, imageCard;
    private FrameLayout classIconBg;
    private LinearLayout stageRow, accuracySection, timeSection, metricsRow;
    private View metricsDivider;
    private ProgressBar progressBar;
    private GeminiApiService geminiApiService;

    // For full-resolution camera capture
    private Uri photoUri;
    private String currentPhotoPath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mulberry_disease_classifier);

        // Bind views
        imageView = findViewById(R.id.plant_upload_image);
        classifierBtn = findViewById(R.id.plant_check_button);
        classText = findViewById(R.id.plant_classify_text);
        accuracyText = findViewById(R.id.plant_classify_text_2);
        timeText = findViewById(R.id.plant_classify_text_3);
        selectImageText = findViewById(R.id.plant_select_image_text);
        stageValue = findViewById(R.id.stage_value);
        backBtn = findViewById(R.id.plant_back_button);
        uploadIcon = findViewById(R.id.upload_icon);
        placeholderOverlay = findViewById(R.id.placeholder_overlay);
        resultsCard = findViewById(R.id.results_card);
        imageCard = findViewById(R.id.image_card);
        classIconBg = findViewById(R.id.class_icon_bg);
        stageRow = findViewById(R.id.stage_row);
        accuracySection = findViewById(R.id.accuracy_section);
        timeSection = findViewById(R.id.time_section);
        metricsRow = findViewById(R.id.metrics_row);
        metricsDivider = findViewById(R.id.metrics_divider);
        progressBar = findViewById(R.id.plant_progressbar);
        actionText = findViewById(R.id.action_text);

        // Initialize Gemini API service with context for smart key rotation
        geminiApiService = new GeminiApiService(this);

        // Initial state - hide results card
        resultsCard.setVisibility(View.GONE);

        // Update instruction text for camera
        selectImageText.setText(R.string.classifier_tap_to_capture);

        // Check if photo path was passed from MainActivity camera capture
        String photoPath = getIntent().getStringExtra("photoPath");
        if (photoPath != null) {
            // Load bitmap from file path (avoids TransactionTooLargeException)
            Bitmap bmp = loadAndDownsampleImage(photoPath, 1024);
            if (bmp != null) {
                bitmap = bmp;
                Log.d(TAG, "Loaded image from path: " + photoPath + " Size: " + bmp.getWidth() + "x" + bmp.getHeight());

                // Show image and hide placeholder
                imageView.setVisibility(View.VISIBLE);
                imageView.setImageBitmap(bmp);
                placeholderOverlay.setVisibility(View.GONE);
                uploadIcon.setVisibility(View.GONE);

                selectImageText.setText(R.string.classifier_image_ready);
            } else {
                Toast.makeText(this, "Failed to load image!", Toast.LENGTH_LONG).show();
            }
        } else {
            Toast.makeText(this, "No image provided!", Toast.LENGTH_LONG).show();
        }

        classifierBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // If analysis is already complete, go back to Home
                if (analysisComplete) {
                    finish();
                    return;
                }

                if (bitmap == null) {
                    Toast.makeText(MulberryScannerClassifierActivity.this,
                        "Please capture an image first", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (AppPreferences.isClaudeEnabled(MulberryScannerClassifierActivity.this)) {
                    // Use Gemini AI analysis (Advanced Mode)
                    performGeminiAnalysis();
                } else {
                    // Use local ML model
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        imageClassifierWithSupportLibrary();
                    }
                }
            }
        });

        backBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MulberryScannerClassifierActivity.super.onBackPressed();
            }
        });

        // Image card click to capture new image
        imageCard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Reset UI for new capture
                resetUIForNewCapture();

                if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                    requestPermissions(new String[]{Manifest.permission.CAMERA}, MY_CAMERA_PERMISSION_CODE);
                } else {
                    launchCamera();
                }
            }
        });
    }

    /**
     * Launch camera with full-resolution capture using FileProvider
     */
    private void launchCamera() {
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        // Create file for full-resolution photo
        File photoFile = null;
        try {
            photoFile = createImageFile();
        } catch (IOException ex) {
            Log.e(TAG, "Error creating image file", ex);
            Toast.makeText(this, "Error creating image file", Toast.LENGTH_LONG).show();
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

    /**
     * Reset UI when capturing a new image
     */
    private void resetUIForNewCapture() {
        analysisComplete = false;
        classifierBtn.setText(R.string.classifier_check);
        resultsCard.setVisibility(View.GONE);
        selectImageText.setText(R.string.classifier_tap_to_capture);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults)
    {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == MY_CAMERA_PERMISSION_CODE)
        {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED)
            {
                Toast.makeText(this, "Camera permission granted!", Toast.LENGTH_LONG).show();
                launchCamera();
            }
            else
            {
                Toast.makeText(this, "Camera permission denied!", Toast.LENGTH_LONG).show();
            }
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CAMERA_REQUEST && resultCode == Activity.RESULT_OK) {
            // Load full-resolution image from file and downsample to 1024px
            if (currentPhotoPath != null) {
                Bitmap photo = loadAndDownsampleImage(currentPhotoPath, 1024);
                if (photo != null) {
                    Toast.makeText(this, "Scan successful!", Toast.LENGTH_LONG).show();
                    Log.d(TAG, "Re-captured image size: " + photo.getWidth() + "x" + photo.getHeight());
                    bitmap = photo;

                    // Show image and hide placeholder
                    imageView.setVisibility(View.VISIBLE);
                    imageView.setImageBitmap(photo);
                    placeholderOverlay.setVisibility(View.GONE);
                    uploadIcon.setVisibility(View.GONE);

                    selectImageText.setText(R.string.classifier_image_ready);
                } else {
                    Toast.makeText(this, "Failed to load captured image!", Toast.LENGTH_LONG).show();
                    showPlaceholder();
                }
            } else {
                Toast.makeText(this, "Failed to capture image!", Toast.LENGTH_LONG).show();
                showPlaceholder();
            }
        } else if (requestCode == CAMERA_REQUEST) {
            // User cancelled - don't show error if bitmap already exists
            if (bitmap == null) {
                showPlaceholder();
            }
        }
    }

    /**
     * Show placeholder when no image is available
     */
    private void showPlaceholder() {
        imageView.setVisibility(View.GONE);
        placeholderOverlay.setVisibility(View.VISIBLE);
        uploadIcon.setVisibility(View.VISIBLE);
        selectImageText.setText(R.string.classifier_tap_to_capture);
    }




    @RequiresApi(api = Build.VERSION_CODES.O)
    private void imageClassifierWithSupportLibrary(){
        // Note: bitmap null check is done in onClick handler
        try{

//            AssetFileDescriptor fileDescriptor = XRayActivity.this.getAssets().openFd("cxr17.tflite");
//            FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
//            FileChannel fileChannel = inputStream.getChannel();
//            long startoffset = fileDescriptor.getStartOffset();
//            long declaredLength=fileDescriptor.getDeclaredLength();
//            MappedByteBuffer tfliteModel = fileChannel.map(FileChannel.MapMode.READ_ONLY, startoffset, declaredLength);


            Interpreter.Options tfliteOptions = new Interpreter.Options();

            CompatibilityList compatList = new CompatibilityList();
            if(compatList.isDelegateSupportedOnThisDevice()){
                // if the device has a supported GPU, add the GPU delegate
                GpuDelegate.Options delegateOptions = compatList.getBestOptionsForThisDevice();
                GpuDelegate gpuDelegate = new GpuDelegate(delegateOptions);
                tfliteOptions.addDelegate(gpuDelegate);
                Log.d(TAG, "GPU supported. GPU delegate created and added to options");
            } else {
                tfliteOptions.setUseXNNPACK(true);
                Log.d(TAG, "GPU not supported. Default to CPU.");
            }


            //Loads model from the model file.
            MappedByteBuffer tfliteModel = FileUtil.loadMappedFile(MulberryScannerClassifierActivity.this,"FinalMobilenetFold1.tflite");
            Interpreter tflite = new Interpreter(tfliteModel, tfliteOptions);

            // Loads labels out from the label file.
            labels = FileUtil.loadLabels(MulberryScannerClassifierActivity.this,"labels.txt");


            // Reads type and shape of input and output tensors, respectively.
            int firstTensorImageIndex = 0;

            int[] inputImageShape = tflite.getInputTensor(firstTensorImageIndex).shape(); // {1, height, width, 3}
            DataType inputImageDataType = tflite.getInputTensor(firstTensorImageIndex).dataType();

            int[] probabilityImageShape = tflite.getOutputTensor(firstTensorImageIndex).shape(); // {1, NUM_CLASSES}
            DataType probabilityImageDataType = tflite.getOutputTensor(firstTensorImageIndex).dataType();

            imageSizeY = inputImageShape[1];
            imageSizeX = inputImageShape[2];



            // Creates the input tensor.
            TensorImage inputTensorImage = new TensorImage(inputImageDataType);
            // Loads bitmap into a TensorImage.
            inputTensorImage.load(bitmap);

            for(int i=0;i<10;i++){
                Log.d(TAG, "Without Normalization: "+inputTensorImage.getTensorBuffer().getFloatValue(i));
            }

            // Creates processor for the TensorImage.
            int cropSize = Math.min(bitmap.getWidth(), bitmap.getHeight());


            //Without normalization, what are the channel values
            int xw = bitmap.getWidth();
            int xh = bitmap.getHeight();

            int [] arr = new int[xw*xh];
            inputTensorImage.getBitmap().getPixels(arr, 0, xw, 0, 0, xw, xh);

            for(int i=0;i<arr.length;i++){

                int red, green, blue;

                red = (arr[i] >> 16) & 0xFF;
                green = (arr[i] >> 8) & 0xFF;
                blue = arr[i] & 0xFF;

                arr[i] = 0xFF << 24 | red << 16 | green << 8 | blue;


                if(i <= 10)
                    Log.d(TAG, "red["+i+"]: "+red + " green["+i+"]: "+green + " blue["+i+"]: "+blue);

            }

            Bitmap b2 = Bitmap.createBitmap(xw, xh, Bitmap.Config.ARGB_8888);
            b2.setPixels(arr, 0, xw, 0, 0, xw, xh);
            imageView.setImageBitmap(b2);


            ImageProcessor imageProcessor = new ImageProcessor.Builder()
                    .add(new ResizeWithCropOrPadOp(cropSize, cropSize))
                    .add(new ResizeOp(imageSizeX, imageSizeY, ResizeOp.ResizeMethod.NEAREST_NEIGHBOR))
                    .add(new NormalizeOp(IMAGE_MEAN, IMAGE_STD))
                    .build();

            inputTensorImage = imageProcessor.process(inputTensorImage);

            for(int i=0;i<10;i++){
                Log.d(TAG, "After Normalization: (Float) "+inputTensorImage.getTensorBuffer().getFloatValue(i));
                Log.d(TAG, "After Normalization: (Int) "+inputTensorImage.getTensorBuffer().getIntValue(i));

            }

            int [] bitmapArray = new int[imageSizeX*imageSizeY];
            inputTensorImage.getBitmap().getPixels(bitmapArray,0, imageSizeX, 0, 0, imageSizeX, imageSizeY);

            float [] tensorArray = inputTensorImage.getTensorBuffer().getFloatArray();
            int [] bitmapArray2 = new int[imageSizeX*imageSizeY];
            int j=0;

            for (int i=0;i<imageSizeX*imageSizeY*3;i=i+3){
//
//                int r = (int) (bitmapArray[i]*128) >> 16;
//                int G = (int) (bitmapArray[i]*128) >> 8;
//                int B = (int) bitmapArray[i]*128;

                int r = (int) tensorArray[i]*128;
                int G = (int) tensorArray[i+1]*128;
                int B = (int) tensorArray[i+2]*128;

                bitmapArray2[j] = 0xFF << 24 | r << 16 | G << 8 | B;
                j++;


                if(i <= 10)
                    Log.d(TAG, "r["+i+"]: "+r + " g["+i+"]: "+G + " b["+i+"]: "+B);

            }

//            Bitmap b3 = Bitmap.createBitmap(imageSizeX, imageSizeY, Bitmap.Config.ARGB_8888);
//            b3.setPixels(bitmapArray2, 0, imageSizeX, 0, 0, imageSizeX, imageSizeY);
//            imageView.setImageBitmap(b3);


            Log.d(TAG, "Byte Array: "+inputTensorImage.getTensorBuffer().getBuffer().array().length);
            Log.d(TAG, "Float Array: "+inputTensorImage.getTensorBuffer().getFloatArray().length);
            Log.d(TAG, "Int Array: "+inputTensorImage.getTensorBuffer().getIntArray().length);


            for(int i=0;i<10;i++){
                Log.d(TAG, "tensorArray["+i+"]: "+tensorArray[i] + " bitmapArray["+i+"]: "+bitmapArray[i] + " bitmapArray2["+i+"]: "+bitmapArray2[i]);

            }

            for(int i=0;i<10;i++){
                Log.d(TAG, "tensorArray["+i+"]: "+tensorArray[i] + " bitmapArray["+i+"]: "+bitmapArray[i] + " bitmapArray2["+i+"]: "+bitmapArray2[i]);

            }
            Log.d(TAG, "tensorArray size "+tensorArray.length + " bitmapArray size: "+bitmapArray.length + " bitmapArray2 size: "+bitmapArray2.length);




            // Creates the output tensor and its processor.
            TensorBuffer probabilityImageBuffer = TensorBuffer.createFixedSize(probabilityImageShape, probabilityImageDataType);


            // Creates the post processor for the output probability.
            TensorProcessor probabilityProcessor = new TensorProcessor.Builder()
                    .add(new NormalizeOp(PROBABILITY_MEAN, PROBABILITY_STD))
                    .build();


//
//            ByteBuffer inputBuffer = ByteBuffer.allocate(4 * inputTensorImage.getTensorBuffer().getFloatArray().length);
//
//            for (float value : inputTensorImage.getTensorBuffer().getFloatArray()){
//                inputBuffer.putFloat(value);
//            }
//
//
//            probabilityImageBuffer.getBuffer().rewind();
//
//            ByteBuffer probabilityBuffer = ByteBuffer.allocate(4 * probabilityImageBuffer.getFloatArray().length);
//
//            for (float value : probabilityImageBuffer.getFloatArray()){
//                probabilityBuffer.putFloat(value);
//            }


            // Runs the inference call.
            Trace.beginSection("runInference");
            long startTimeForReference = SystemClock.uptimeMillis();


            tflite.run(inputTensorImage.getBuffer(), probabilityImageBuffer.getBuffer());


            long endTimeForReference = SystemClock.uptimeMillis();
            Trace.endSection();
            Log.v(TAG, "Timecost to run model inference: " + (endTimeForReference - startTimeForReference) + " ms");


            for(int i=0; i< probabilityImageBuffer.getFloatArray().length;i++)
                Log.d(TAG, "Probability Image Buffer ["+i + "] :"+ String.format("%.8f", probabilityImageBuffer.getFloatValue(i)));





            // Gets the map of label and probability.
            Map<String, Float> labeledProbability = new TensorLabel(labels, probabilityProcessor.process(probabilityImageBuffer)).getMapWithFloatValue();

            // Process results and calculate stage
            ClassificationResult result = processClassificationResults(
                labeledProbability, endTimeForReference - startTimeForReference);

            // Update UI with results
            updateUIWithResults(result);

//            for(int i=0; i<1000; i++){
//
//                Log.d(TAG, "Input Tensor Image ["+i+"]: "+inputTensorImage.getTensorBuffer().getFloatValue(i));
//
//            }
//
//            for(int i=0;i<1000;i++){
//                Log.d(TAG, "Output Tensor Buffer ["+i+"]: "+probabilityImageBuffer.getFloatValue(i));
//
//            }
//
//            Log.d(TAG, "Buffer Length: "+inputTensorImage.getTensorBuffer().getBuffer().array().length);
//

            Trace.endSection();



            Log.d(TAG, "Input Tensor Image Size: "+ inputTensorImage.getTensorBuffer().getFloatArray().length + " DataType: "+ inputTensorImage.getTensorBuffer().getDataType());
            Log.d(TAG, "Input Tensor Buffer Size: "+ tflite.getInputTensor(0).numBytes());
            Log.d(TAG, "Output Tensor Buffer Size: "+ tflite.getOutputTensor(0).numBytes());

            Log.d(TAG, "Bitmap Byte Count: "+ this.bitmap.getByteCount());
            Log.d(TAG, "Input Tensor Image Byte Count: "+ inputTensorImage.getTensorBuffer().getFloatArray().length);
            Log.d(TAG, "Input Image Shape: "+ inputImageShape[2] +  " "+ inputImageShape[1]);
            Log.d(TAG, "Input Image DataType: "+ inputImageDataType);
            Log.d(TAG, "Input Image Tensor[0]: "+ tflite.getInputTensor(0).asReadOnlyBuffer().getInt(0));
            Log.d(TAG, "Output Image Shape: "+ probabilityImageShape[1] +  " "+ probabilityImageShape[0]);
            Log.d(TAG, "Output Image DataType: "+ probabilityImageDataType);
            Log.d(TAG, "Output Image Tensor[0]: "+ tflite.getOutputTensor(0).asReadOnlyBuffer().getInt(0));

            Log.d("TensorBuffer","Input TensorBuffer Length: "+ inputTensorImage.getBuffer().toString().length());
            Log.d("TensorBuffer","Probability TensorBuffer Length: "+ probabilityImageBuffer.getBuffer().toString().length());






        }

        catch (Exception e) {
            e.printStackTrace();
        }



    }

    /**
     * Process classification results and calculate disease stage
     * @param labeledProbability Map of class labels to confidence scores
     * @param processingTime Time taken for classification in milliseconds
     * @return ClassificationResult object with class, confidence, time, and stage
     */
    private ClassificationResult processClassificationResults(
            Map<String, Float> labeledProbability, long processingTime) {

        // Find the classification with highest confidence
        float maxConfidence = Collections.max(labeledProbability.values());
        String predictedClass = null;

        for (Map.Entry<String, Float> entry : labeledProbability.entrySet()) {
            if (entry.getValue() == maxConfidence) {
                predictedClass = entry.getKey();
                break;
            }
        }

        // Check if staging is enabled
        boolean stagingEnabled = AppPreferences.isStagingEnabled(this);

        // Calculate stage if enabled
        int stage = -1;
        if (stagingEnabled && predictedClass != null) {
            stage = DiseaseStageCalculator.calculateStage(predictedClass, maxConfidence);
        }

        // Return result object
        return new ClassificationResult(predictedClass, maxConfidence, processingTime, stage);
    }

    /**
     * Update UI with classification results
     * @param result Classification result to display
     */
    private void updateUIWithResults(ClassificationResult result) {
        // Check settings
        boolean stagingEnabled = AppPreferences.isStagingEnabled(this);
        boolean timeEnabled = AppPreferences.isTimeEnabled(this);
        boolean accuracyEnabled = AppPreferences.isAccuracyEnabled(this);

        // Show results card
        resultsCard.setVisibility(View.VISIBLE);

        // Update classification text with friendly name
        classText.setText(result.getDisplayClassName());

        // Update icon background based on status
        if (result.isNotLeaf()) {
            classIconBg.setBackgroundResource(R.drawable.result_icon_bg_warning);
        } else {
            classIconBg.setBackgroundResource(result.isHealthy()
                ? R.drawable.result_icon_bg_success
                : R.drawable.result_icon_bg_warning);
        }

        // Stage row visibility and value
        stageRow.setVisibility(stagingEnabled ? View.VISIBLE : View.GONE);
        if (stagingEnabled) {
            stageValue.setText(result.getStageDisplayText());
        }

        // Metrics section visibility
        boolean showMetrics = timeEnabled || accuracyEnabled;
        metricsDivider.setVisibility(showMetrics ? View.VISIBLE : View.GONE);
        metricsRow.setVisibility(showMetrics ? View.VISIBLE : View.GONE);

        // Accuracy section
        accuracySection.setVisibility(accuracyEnabled ? View.VISIBLE : View.GONE);
        if (accuracyEnabled) {
            accuracyText.setText(String.format("%.2f", result.getConfidence()));
        }

        // Time section
        timeSection.setVisibility(timeEnabled ? View.VISIBLE : View.GONE);
        if (timeEnabled) {
            timeText.setText(result.getProcessingTimeMs() + " ms");
        }

        // Always show "Done" after analysis (stage is already displayed in the results card)
        classifierBtn.setText("Done");

        // Mark analysis as complete for button navigation
        analysisComplete = true;

        // Hide instruction text after classification
        selectImageText.setVisibility(View.GONE);

        // Update recommended action
        updateRecommendedAction(result);
    }

    /**
     * Update the recommended action text based on classification result
     * @param result The classification result
     */
    private void updateRecommendedAction(ClassificationResult result) {
        String displayName = result.getDisplayClassName();

        if (displayName == null) {
            actionText.setText(R.string.action_healthy);
            return;
        }

        switch (displayName) {
            case "Not a Leaf":
                actionText.setText(R.string.action_not_leaf);
                break;
            case "No Visible Leaf Spot Detected":
                actionText.setText(R.string.action_healthy);
                break;
            case "Potential Leaf Spot":
                actionText.setText(R.string.action_leaf_spot);
                break;
            case "Early Spot Detected":
                // Determine if it's leaf rust or early detection based on className
                String className = result.getClassName();
                if (className != null && className.contains("Rust")) {
                    actionText.setText(R.string.action_leaf_rust);
                } else {
                    actionText.setText(R.string.action_early_spot);
                }
                break;
            default:
                actionText.setText(R.string.action_early_spot);
                break;
        }
    }

    /**
     * Perform leaf analysis using Gemini AI
     */
    private void performGeminiAnalysis() {
        // Check network connectivity
        if (!GeminiApiService.isNetworkAvailable(this)) {
            Toast.makeText(this, R.string.gemini_no_internet, Toast.LENGTH_LONG).show();
            return;
        }

        // Check API key
        if (!geminiApiService.isApiKeyConfigured()) {
            Toast.makeText(this, R.string.gemini_api_key_missing, Toast.LENGTH_LONG).show();
            return;
        }

        // Show loading state
        progressBar.setVisibility(View.VISIBLE);
        classifierBtn.setEnabled(false);
        classifierBtn.setText(R.string.gemini_analyzing);
        resultsCard.setVisibility(View.GONE);

        // Call Gemini API with smart key rotation
        geminiApiService.analyzeLeafImage(bitmap, new GeminiApiService.GeminiCallback() {
            @Override
            public void onSuccess(ClaudeAnalysisResult result, long processingTimeMs) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    classifierBtn.setEnabled(true);

                    // Convert to ClassificationResult and update UI
                    ClassificationResult classResult = result.toClassificationResult(processingTimeMs);
                    updateUIWithResults(classResult);
                });
            }

            @Override
            public void onError(String errorMessage) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    classifierBtn.setEnabled(true);
                    classifierBtn.setText(R.string.classifier_check);
                    Toast.makeText(MulberryScannerClassifierActivity.this,
                        R.string.gemini_error, Toast.LENGTH_LONG).show();
                    Log.e(TAG, "Gemini API error: " + errorMessage);
                });
            }

            @Override
            public void onRateLimitExhausted() {
                runOnUiThread(() -> {
                    Log.w(TAG, "All API keys exhausted, falling back to local ML model");
                    // Reset key index for next session
                    AppPreferences.resetApiKeyIndex(MulberryScannerClassifierActivity.this);
                    // Hide progress bar but keep button disabled during local processing
                    progressBar.setVisibility(View.GONE);
                    classifierBtn.setText(R.string.classifier_check);
                    // Use local ML model as fallback
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        imageClassifierWithSupportLibrary();
                    }
                    classifierBtn.setEnabled(true);
                });
            }
        });
    }

    //    private void imageClassifierwithTaskLibrary() throws IOException {
//
//        // Initialization
//        ImageClassifier.ImageClassifierOptions options =
//                ImageClassifier.ImageClassifierOptions.builder()
//                        .setMaxResults(1)
//                        .build();
//
//        ImageClassifier imageClassifier = ImageClassifier.createFromFileAndOptions(
//                        XRayActivity.this, "cxr17.tflite", options);
//
//        Trace.beginSection("recognizeImage");
//
//        imageSizeX = 124;
//        imageSizeY = 124;
//
//        TensorImage inputImage = TensorImage.fromBitmap(normalizeBitmap(bitmap));
//        int width = bitmap.getWidth();
//        int height = bitmap.getHeight();
//
//        width = 124;
//        height = 124;
//
//        int cropSize = min(width, height);
//
//        ImageProcessingOptions imageOptions = ImageProcessingOptions.builder()
//                        // Set the ROI to the center of the image.
//                        .setRoi(
//                                new Rect(
//                                        /*left=*/ (width - cropSize) / 2,
//                                        /*top=*/ (height - cropSize) / 2,
//                                        /*right=*/ (width + cropSize) / 2,
//                                        /*bottom=*/ (height + cropSize) / 2))
//                        .build();
//
//        // Runs the inference call.
//        Trace.beginSection("runInference");
//        long startTimeForReference = SystemClock.uptimeMillis();
//
//        List<Classifications> results = imageClassifier.classify(inputImage, imageOptions);
//        long endTimeForReference = SystemClock.uptimeMillis();
//        Trace.endSection();
//        Log.v(TAG, "Timecost to run model inference: " + (endTimeForReference - startTimeForReference));
//
//
//        for(int i=0;i<results.size(); i++)
//            Log.d(TAG, "Run complete. Label: " + results.get(i).getCategories().get(0).getLabel() + " DisplayName: " + results.get(i).getCategories().get(0).getDisplayName() + " Score: " + results.get(i).getCategories().get(0).getScore() +" Index: " + results.get(i).getCategories().get(0).getIndex());
//
//        List<Recognition> result = getRecognitions(results);
//
//        for(int i=0;i<result.size();i++) {
//            classitext.setText("Class: "+result.get(i).getTitle());
//            accuracyText.setText("Accuracy: "+result.get(i).getConfidence() + "%");
//            accuracyText.setVisibility(View.VISIBLE);
//
//            imageView.setImageBitmap(normalizeBitmap(bitmap));
//        }
//        Trace.endSection();
//
//
//    }
//    private static List<Recognition> getRecognitions(List<Classifications> classifications) {
//
//        final ArrayList<Recognition> recognitions = new ArrayList<>();
//        // All the demo models are single head models. Get the first Classifications in the results.
//        for (Category category : classifications.get(0).getCategories()) {
//            recognitions.add(new Recognition("" + category.getLabel(), category.getLabel(), category.getScore(), null));
//        }
//        return recognitions;
//    }



    private ByteBuffer convertBitmapToByteBuffer(Bitmap bp) {
        ByteBuffer imgData = ByteBuffer.allocateDirect(Float.BYTES*imageSizeX*imageSizeY*3);
        imgData.order(ByteOrder.nativeOrder());
        Bitmap bitmap = Bitmap.createScaledBitmap(bp,imageSizeX,imageSizeY,true);
        int [] intValues = new int[imageSizeX*imageSizeY];

        bitmap.getPixels(intValues, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());

        // Convert the image to floating point.
        int pixel = 0;

        for (int i = 0; i < imageSizeX; ++i) {
            for (int j = 0; j < imageSizeY; ++j) {
                final int val = intValues[pixel++];

                imgData.putFloat(((val>> 16) & 0xFF) / 255.f);
                imgData.putFloat(((val>> 8) & 0xFF) / 255.f);
                imgData.putFloat((val & 0xFF) / 255.f);
            }
        }

        Log.d(TAG, "ByteBuffer Length: "+imgData.array().length);

        Bitmap bitmap1 = Bitmap.createBitmap(32, 32, Bitmap.Config.ARGB_8888);
        bitmap1.copyPixelsFromBuffer(imgData);
        imageView.setImageBitmap(bitmap1);

        return imgData;
    }

    private Bitmap normalizeBitmap(Bitmap bp) {
        ByteBuffer imgData = ByteBuffer.allocateDirect(Float.BYTES*imageSizeX*imageSizeY*3);
        imgData.order(ByteOrder.nativeOrder());
        Bitmap bitmap = Bitmap.createScaledBitmap(bp,imageSizeX,imageSizeY,true);
        int [] intValues = new int[imageSizeX*imageSizeY];
        bitmap.getPixels(intValues, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());

        // Convert the image to floating point.
        int pixel = 0;

        for (int i = 0; i < imageSizeX; ++i) {
            for (int j = 0; j < imageSizeY; ++j) {
                final int val = intValues[pixel++];

                imgData.putFloat(((val>> 16) & 0xFF) / 255.f);
                imgData.putFloat(((val>> 8) & 0xFF) / 255.f);
                imgData.putFloat((val & 0xFF) / 255.f);


            }
        }

//        int j = 0;
//        for(int i=0;i<intValues.length;i++){
//
//            intValues[i] = imgData.get(j);
//            j = j+3;
//            Log.d(TAG, "byteBuffer["+i+"]"+" : "+imgData.get(i));
//        }


        Log.d(TAG, "Bitmap Type: "+bitmap.getConfig().toString());
        Log.d(TAG, "Bitmap Size: "+bitmap.getByteCount());
        Log.d(TAG, "Buffer Size: "+imgData.array().length);
        Log.d(TAG, "Array Size: "+intValues.length);

        //Bitmap bp2 = Bitmap.createBitmap(imageSizeX,imageSizeY, Bitmap.Config.ARGB_8888);

        //int j=intValues.length-1;

        for(int i=0;i<intValues.length;i++){

//            Log.d(TAG, "IntValues["+i+"]"+" : "+intValues[i]);
            intValues[i] = intValues[i] / 255;
//            intValues[i] = intValues[j];
//            j--;

        }

        imgData.rewind();
        Bitmap bp2 = Bitmap.createBitmap(imageSizeX, imageSizeY, Bitmap.Config.ARGB_8888);
        bp2.setPixels(intValues,0,imageSizeX,0,0,imageSizeX,imageSizeY);

        Log.d(TAG, "Bitmap Size: "+ bitmap.getByteCount());

        return bp2;
    }

    /** Gets the top-k results. */
    private static List<Recognition> getTopKProbability(Map<String, Float> labelProb) {
        // Find the best classifications.
        PriorityQueue<Recognition> pq =
                new PriorityQueue<>(
                        3,
                        new Comparator<Recognition>() {
                            @Override
                            public int compare(Recognition lhs, Recognition rhs) {
                                // Intentionally reversed to put high confidence at the head of the queue.
                                return Float.compare(rhs.getConfidence(), lhs.getConfidence());
                            }
                        });

        for (Map.Entry<String, Float> entry : labelProb.entrySet()) {
            pq.add(new Recognition("" + entry.getKey(), entry.getKey(), entry.getValue(), null));
        }

        final ArrayList<Recognition> recognitions = new ArrayList<>();
        int recognitionsSize = min(pq.size(), 3);
        for (int i = 0; i < recognitionsSize; ++i) {
            recognitions.add(pq.poll());
        }
        return recognitions;
    }

    private TensorImage loadImage(final Bitmap bitmap) {
        // Loads bitmap into a TensorImage.
        inputTensorImage.load(bitmap);

        // Creates processor for the TensorImage.
        int cropSize = Math.min(bitmap.getWidth(), bitmap.getHeight());
        // TODO(b/143564309): Fuse ops inside ImageProcessor.
        ImageProcessor imageProcessor =
                new ImageProcessor.Builder()
                        .add(new ResizeWithCropOrPadOp(cropSize, cropSize))
                        .add(new ResizeOp(imageSizeX, imageSizeY, ResizeOp.ResizeMethod.NEAREST_NEIGHBOR))
                        .add(getPreprocessNormalizeOp())

                        .build();
        return imageProcessor.process(inputTensorImage);
    }

    private MappedByteBuffer loadmodelfile(Activity activity) throws IOException {
        AssetFileDescriptor fileDescriptor = activity.getAssets().openFd("cxr17.tflite");
        FileInputStream inputStream=new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel=inputStream.getChannel();
        long startoffset = fileDescriptor.getStartOffset();
        long declaredLength=fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY,startoffset,declaredLength);
    }

    private TensorOperator getPreprocessNormalizeOp() {
        return new NormalizeOp(IMAGE_MEAN, IMAGE_STD);
    }
    private TensorOperator getPostprocessNormalizeOp(){
        return new NormalizeOp(PROBABILITY_MEAN, PROBABILITY_STD);
    }

    private void showResult(){

        try{
            labels = FileUtil.loadLabels(this,"labels.txt");
        }catch (Exception e){
            e.printStackTrace();
        }
        Map<String, Float> labeledProbability =
                new TensorLabel(labels, probabilityProcessor.process(probabilityImageBuffer))
                        .getMapWithFloatValue();
        float maxValueInMap =(Collections.max(labeledProbability.values()));

        for (Map.Entry<String, Float> entry : labeledProbability.entrySet()) {
            if (entry.getValue()==maxValueInMap) {
                classText.setText(entry.getKey());
            }
        }
    }


}

