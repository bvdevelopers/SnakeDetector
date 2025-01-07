package com.example.snakedetector;


import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;

import java.nio.ByteBuffer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class User extends AppCompatActivity {

    private PreviewView previewView;
    private SnakeDetector snakeDetector;
    private ExecutorService cameraExecutor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user);

        previewView = findViewById(R.id.previewView);

        // Initialize the SnakeDetector
        snakeDetector = new SnakeDetector(this);

        // Set up the camera
        setupCamera();

        // Initialize camera executor
        cameraExecutor = Executors.newSingleThreadExecutor();
    }

    private void setupCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                // Bind Preview and Analysis Use Cases
                bindCameraUseCases(cameraProvider);

            } catch (Exception e) {
                Toast.makeText(this, "Error setting up camera: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void bindCameraUseCases(@NonNull ProcessCameraProvider cameraProvider) {
        // Camera Selector (Rear Camera)
        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build();

        // Preview Use Case
        androidx.camera.core.Preview preview = new androidx.camera.core.Preview.Builder().build();
        preview.setSurfaceProvider(previewView.getSurfaceProvider());

        // Image Analysis Use Case
        ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build();

        imageAnalysis.setAnalyzer(cameraExecutor, image -> {
            Bitmap bitmap = imageProxyToBitmap(image); // Convert frame to Bitmap
            if (bitmap != null) {
                snakeDetector.detectSnake(bitmap); // Detect snake in the frame
            }
            image.close();
        });

        // Bind Use Cases to CameraProvider
        cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis);
    }

    private Bitmap imageProxyToBitmap(ImageProxy imageProxy) {
        @SuppressLint("UnsafeOptInUsageError")
        ImageProxy.PlaneProxy[] planes = imageProxy.getPlanes();
        if (planes != null && planes.length > 0) {
            ByteBuffer buffer = planes[0].getBuffer();
            byte[] bytes = new byte[buffer.remaining()];
            buffer.get(bytes);
            return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        }
        return null;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (cameraExecutor != null) {
            cameraExecutor.shutdown();
        }
    }
}
