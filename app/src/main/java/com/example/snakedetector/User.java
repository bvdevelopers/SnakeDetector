package com.example.snakedetector;


import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.media.Image;
import android.os.Bundle;
import android.provider.MediaStore;
import android.telephony.SmsManager;
import android.telephony.gsm.SmsMessage;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.MediaController;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.OptIn;
import androidx.appcompat.app.AppCompatActivity;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Base64;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraControl;
import androidx.camera.core.ExperimentalGetImage;
import androidx.camera.core.Preview;
import androidx.camera.core.UseCaseGroup;
import androidx.camera.core.ViewPort;
import androidx.camera.view.PreviewView;

import com.bumptech.glide.Glide;
import com.example.snakedetector.Interface.SnakeDetectionAPI;
import com.example.snakedetector.model.CaptureResponse;
import com.example.snakedetector.model.DetectionResponse;
import com.example.snakedetector.model.ImageRequest;
import com.google.common.util.concurrent.ListenableFuture;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import android.media.Image;       // For the YUV_420_888 Image object
import java.nio.ByteBuffer;       // For accessing plane buffers
import android.graphics.YuvImage;        // For YUV-to-JPEG conversion
import android.graphics.BitmapFactory;   // To decode the JPEG data into a Bitmap
import android.graphics.Rect;            // For defining the region of the YUV image

import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Base64;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
//import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
//
//import com.example.snakedetector.api.SnakeDetectionAPI;
//import com.example.snakedetector.api.RetrofitClient;
//import com.example.snakedetector.model.ImageRequest;
//import com.example.snakedetector.model.DetectionResponse;
import android.media.ToneGenerator;
import android.media.AudioManager;
//import java.io.ByteArrayOutputStream;
//import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

//import retrofit2.Call;
//import retrofit2.Callback;
//import retrofit2.Response;

public class User extends AppCompatActivity {
    private PreviewView previewView;
    private TextView detectionMessage;
    private ExecutorService cameraExecutor;
    private boolean isRequestInProgress = false;
    private SnakeDetectionAPI api;
    private ToneGenerator toneGenerator;
    private ImageButton phone_btn, logout_btn;
    SharedPreferences.Editor editor;
    SharedPreferences sharedPreferences;

    String phone;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user);
        sharedPreferences = getSharedPreferences("UserSession", MODE_PRIVATE);
        editor = sharedPreferences.edit();

        phone = sharedPreferences.getString("phone", "");


        previewView = findViewById(R.id.previewView);
        detectionMessage = findViewById(R.id.detectionMessage);
        phone_btn = findViewById(R.id.phno);
        logout_btn = findViewById(R.id.logout);

        api = RetrofitClient.getClient().create(SnakeDetectionAPI.class);
        cameraExecutor = Executors.newSingleThreadExecutor();
        toneGenerator = new ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100);

        phone_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showPopup();
            }
        });
        logout_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(User.this,Login.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

                editor.clear();
                editor.apply();
                startActivity(intent);
                finish();
            }
        });
        try {
            startCamera();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    private void bindPreview(@NonNull ProcessCameraProvider cameraProvider) {
        Preview preview = new Preview.Builder().build();
        preview.setSurfaceProvider(previewView.getSurfaceProvider());

        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build();

// Bind camera to lifecycle
        cameraProvider.bindToLifecycle(this, cameraSelector, preview);
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                Log.d("CameraX", "Camera provider initialized successfully"); // Use get() to retrieve the provider

                // Select the back camera (you can change to front camera if needed)
                CameraSelector cameraSelector = new CameraSelector.Builder()
                        .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                        .build();

                // Create a Preview use case
                Preview preview = new Preview.Builder()
                        .build();

                // Create an ImageAnalysis use case (for analyzing frames)
                ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();

                // Set up the analyzer
                imageAnalysis.setAnalyzer(cameraExecutor, this::analyzeImage);

                // Set up the surface provider to display the camera feed in PreviewView
                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                // Unbind any previously bound use cases
                cameraProvider.unbindAll();

                // Bind Preview and ImageAnalysis to the lifecycle
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis);

            } catch (Exception e) {
                Log.e("CameraX", "Camera initialization failed: " + e.getMessage());
                Toast.makeText(this, "Camera initialization failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }, ContextCompat.getMainExecutor(this));
    }
    private void showPopup() {
        // Inflate the popup layout
        LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        View popupView = inflater.inflate(R.layout.popup_layout, null);

        // Create the popup window
        PopupWindow popupWindow = new PopupWindow(
                popupView,
                RelativeLayout.LayoutParams.WRAP_CONTENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT,
                true
        );

        // Find elements inside the popup
        EditText editTextPopup = popupView.findViewById(R.id.editTextPopup);
        Button buttonPopup = popupView.findViewById(R.id.buttonPopup);
        Button close = popupView.findViewById(R.id.close);
        editTextPopup.setText(phone);
        // Set button click listener
        buttonPopup.setOnClickListener(v -> {
            phone = editTextPopup.getText().toString();

            editor.putString("phone", editTextPopup.getText().toString());
            editor.apply();
            Toast.makeText(this,"Alert Number Changed to "+editTextPopup.getText().toString(),Toast.LENGTH_LONG).show();
        });
        close.setOnClickListener(v -> {
            popupWindow.dismiss(); // Close the popup
        });

        // Show the popup window
        popupWindow.showAtLocation(findViewById(android.R.id.content), 0, 0, 0);
    }

    private void analyzeImage(@NonNull ImageProxy image) {
        if (isRequestInProgress) {
            image.close();
            return;
        }

        Bitmap bitmap = imageProxyToBitmap(image);
        String base64Image = getBase64FromBitmap(bitmap);

        isRequestInProgress = true;
        ImageRequest img = new ImageRequest(base64Image);
        api.detectSnake(img).enqueue(new Callback<DetectionResponse>() {
            @Override
            public void onResponse(Call<DetectionResponse> call, Response<DetectionResponse> response) {
                isRequestInProgress = false;
                if (response.isSuccessful() && response.body() != null) {
                    if (!response.body().getDetections().isEmpty()) {
                        Log.e("ers image : {}",img.getImage());
                        toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, 200);
                        runOnUiThread(() -> detectionMessage.setVisibility(TextView.VISIBLE));
                        SmsManager smsManager= SmsManager.getDefault();
                        smsManager.sendTextMessage(phone,null,"💀!Warning A 🐍snake detected in your Android Cam!",null,null);



                    } else {
                        runOnUiThread(() -> detectionMessage.setVisibility(TextView.GONE));
                    }
                }
                image.close();
            }

            @Override
            public void onFailure(Call<DetectionResponse> call, Throwable t) {
                isRequestInProgress = false;
                image.close();
                Log.e("Error : {}",t.getMessage());
                Toast.makeText(User.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private Bitmap imageProxyToBitmap(ImageProxy imageProxy) {
        @SuppressLint("UnsafeOptInUsageError")
        Image image = imageProxy.getImage();

        if (image != null) {
            return convertImageProxyToBitmap(imageProxy);
        }
        return null;
    }

//    private Bitmap convertImageProxyToBitmap(ImageProxy imageProxy) {
//        @SuppressLint("UnsafeOptInUsageError")
//        Image image = imageProxy.getImage();
//
//        if (image != null) {
//            // Get the YUV image data
//            ByteBuffer buffer = image.getPlanes()[0].getBuffer();
//            int bufferSize = buffer.remaining();
//
//            // Log the buffer size
//            Log.d("convertImageProxy", "Buffer size: " + bufferSize);
//
//            if (bufferSize == 0) {
//                Log.e("convertImageProxy", "Buffer is empty.");
//                return null;
//            }
//
//            byte[] bytes = new byte[bufferSize];
//            buffer.get(bytes);
//
//            // Convert YUV to NV21 or RGB format if necessary
//            Bitmap bitmap = yuvToBitmap(bytes, image.getWidth(), image.getHeight());
//
//            if (bitmap == null) {
//                Log.e("convertImageProxy", "Failed to decode YUV data.");
//            }
//
//            return bitmap;
//        }
//
//        Log.e("convertImageProxy", "Image is null.");
//        return null;
//    }
private Bitmap convertImageProxyToBitmap(ImageProxy imageProxy) {
    @OptIn(markerClass = ExperimentalGetImage.class) Image image = imageProxy.getImage();
    if (image != null) {
        // Convert ImageProxy to NV21 byte array
        byte[] nv21 = convertImageToNV21(image);
        if (nv21 != null) {
            return yuvToBitmap(nv21, image.getWidth(), image.getHeight());
        }
    }
    Log.e("convertImageProxy", "Failed to convert ImageProxy to Bitmap");
    return null;
}

    private byte[] convertImageToNV21(Image image) {
        // Ensure proper conversion from YUV_420_888 to NV21 format
        // Implementation can vary based on camera frame format
        return YUV_420_888toNV21(image); // Helper method
    }

    private byte[] YUV_420_888toNV21(Image image) {
        int width = image.getWidth();
        int height = image.getHeight();

        Image.Plane[] planes = image.getPlanes();
        ByteBuffer yBuffer = planes[0].getBuffer(); // Y
        ByteBuffer uBuffer = planes[1].getBuffer(); // U
        ByteBuffer vBuffer = planes[2].getBuffer(); // V

        int ySize = yBuffer.remaining();
        int uSize = uBuffer.remaining();
        int vSize = vBuffer.remaining();

        byte[] nv21 = new byte[ySize + uSize + vSize];

        // Copy Y data
        yBuffer.get(nv21, 0, ySize);

        // Interleave U and V data
        byte[] uBytes = new byte[uSize];
        byte[] vBytes = new byte[vSize];
        uBuffer.get(uBytes);
        vBuffer.get(vBytes);

        for (int i = 0; i < uSize; i++) {
            nv21[ySize + (i * 2)] = vBytes[i];
            nv21[ySize + (i * 2) + 1] = uBytes[i];
        }

        return nv21;
    }

    // Convert YUV to Bitmap (NV21 or RGB)
    private Bitmap yuvToBitmap(byte[] yuvData, int width, int height) {
        try {
            YuvImage yuvImage = new YuvImage(yuvData, ImageFormat.NV21, width, height, null);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            yuvImage.compressToJpeg(new Rect(0, 0, width, height), 100, out); // Ensure compression quality
            byte[] jpegData = out.toByteArray();
            return BitmapFactory.decodeByteArray(jpegData, 0, jpegData.length);
        } catch (Exception e) {
            Log.e("yuvToBitmap", "Error converting YUV to Bitmap: " + e.getMessage());
            return null;
        }
    }


    private String getBase64FromBitmap(Bitmap bitmap) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
        return Base64.encodeToString(outputStream.toByteArray(), Base64.DEFAULT);
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (cameraExecutor != null) {
            cameraExecutor.shutdown();
        }
        if (toneGenerator != null) {
            toneGenerator.release();
        }
    }
}
