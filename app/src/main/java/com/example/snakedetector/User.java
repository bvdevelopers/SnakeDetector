package com.example.snakedetector;


import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.media.Image;
import android.os.Bundle;
import android.provider.MediaStore;
import android.telephony.SmsManager;
import android.telephony.SubscriptionManager;
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
import com.example.snakedetector.Interface.UrlShortenerCallback;
import com.example.snakedetector.model.CaptureResponse;
import com.example.snakedetector.model.DetectionResponse;
import com.example.snakedetector.model.ImageRequest;
import com.example.snakedetector.model.SpecDetectionResponse;
import com.google.common.util.concurrent.ListenableFuture;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;



import androidx.annotation.NonNull;
//import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
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
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import android.location.Location;
import com.google.android.gms.tasks.OnSuccessListener;

import org.json.JSONObject;


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
    FusedLocationProviderClient fusedLocationClient;
    String phone;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user);
        sharedPreferences = getSharedPreferences("UserSession", MODE_PRIVATE);
        editor = sharedPreferences.edit();

        phone = sharedPreferences.getString("phone", "");

         fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

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
    private MultipartBody.Part bitmapToMultipartBody(Bitmap bitmap) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
        byte[] byteArray = stream.toByteArray();

        RequestBody requestBody = RequestBody.create(MediaType.parse("image/jpeg"), byteArray);
        return MultipartBody.Part.createFormData("file", "image.jpg", requestBody);
    }
    private void analyzeImage(@NonNull ImageProxy image) {
        if (isRequestInProgress) {
            image.close();
            return;
        }

        Bitmap bitmap = imageProxyToBitmap(image);
        if (bitmap == null) {
            image.close(); // Ensure image is released
            return;
        }

        String base64Image = getBase64FromBitmap(bitmap);
        isRequestInProgress = true;
        ImageRequest img = new ImageRequest(base64Image);

        Retrofit retrofit5000 = new Retrofit.Builder()
                .baseUrl("http://192.168.29.187:5000/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        SnakeDetectionAPI api5000 = retrofit5000.create(SnakeDetectionAPI.class);
        MultipartBody.Part imagePart = bitmapToMultipartBody(bitmap);

        api.detectSnake(img).enqueue(new Callback<DetectionResponse>() {
            @Override
            public void onResponse(Call<DetectionResponse> call, Response<DetectionResponse> response) {
                isRequestInProgress = false;

                if (response.isSuccessful() && response.body() != null && !response.body().getDetections().isEmpty()) {
                    // Play beep sound before calling second API
                    toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, 200);
                    detectionMessage.setText("Snake Detected");
                    detectionMessage.setVisibility(View.VISIBLE);
                    StringBuilder alertMessage=new StringBuilder();
                    api5000.predictSnake(imagePart).enqueue(new Callback<SpecDetectionResponse>() {
                        @Override
                        public void onResponse(Call<SpecDetectionResponse> call, Response<SpecDetectionResponse> response) {
                            isRequestInProgress = false;
                            image.close(); // Ensure image is closed

                            if (!response.isSuccessful() || response.body() == null) {
                                Log.e("analyzeImage", "Species prediction API call failed. Code: " + response.code());
                                runOnUiThread(() -> Toast.makeText(User.this, "Failed to predict snake species!", Toast.LENGTH_SHORT).show());
                                return;
                            }

                            String species = response.body().getSpecies();
                            String status = response.body().getStatus();
                            Log.d("Prediction", "Species: " + species + ", Status: " + status);

                            runOnUiThread(() -> {


//                                SmsManager smsManager = SmsManager.getDefault();
                                 alertMessage.append("⚠️ Warning! A snake was detected on device! that might be" + species + " (" + status + ") detected!");
//                                smsManager.sendTextMessage(phone, null, alertMessage, null, null);

                                if (ActivityCompat.checkSelfPermission(User.this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(User.this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                                    // TODO: Consider calling
                                    //    ActivityCompat#requestPermissions
                                    // here to request the missing permissions, and then overriding
                                    //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                    //                                          int[] grantResults)
                                    // to handle the case where the user grants the permission. See the documentation
                                    // for ActivityCompat#requestPermissions for more details.
                                    return;
                                }
                                fusedLocationClient.getLastLocation().addOnSuccessListener(new OnSuccessListener<Location>() {
                                    @Override
                                    public void onSuccess(Location location) {
                                        if (location != null) {
                                            double latitude = location.getLatitude();
                                            double longitude = location.getLongitude();
                                            String mapsLink = "https://www.google.com/maps?q=" + latitude + "," + longitude;

                                            shortenWithBitly(mapsLink, new UrlShortenerCallback() {
                                                @Override
                                                public void onSuccess(String shortUrl) {
                                                    alertMessage.append("\nLocation: ").append(shortUrl);

//                                                    String phoneNumber = "+916384557903"; // Include country code

                                                    Log.d("SMS", "Sending SMS to: " + phone);
                                                    Log.d("SMS", "Message: " + alertMessage.toString());

                                                    SmsManager smsManager = SmsManager.getDefault();
                                                    ArrayList<String> messageParts = smsManager.divideMessage(alertMessage.toString());
                                                    smsManager.sendMultipartTextMessage(phone, null, messageParts, null, null);

                                                    runOnUiThread(() -> {
                                                        Toast.makeText(User.this, "SMS Sent with Shortened Link!", Toast.LENGTH_SHORT).show();
                                                    });
                                                }

                                                @Override
                                                public void onError(String error) {
                                                    runOnUiThread(() -> Toast.makeText(User.this, "Error shortening URL: " + error, Toast.LENGTH_SHORT).show());
                                                }
                                            });

                                        } else {
                                            runOnUiThread(() -> Toast.makeText(User.this, "Failed to get location", Toast.LENGTH_SHORT).show());
                                        }
                                    }
                                });

                                 });
                        }

                        @Override
                        public void onFailure(Call<SpecDetectionResponse> call, Throwable t) {
                            isRequestInProgress = false;
                            image.close();
                            Log.e("analyzeImage", "API call error: " + t.getMessage());
                            runOnUiThread(() -> Toast.makeText(User.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show());
                        }
                    });
                } else {
                    runOnUiThread(() -> detectionMessage.setVisibility(TextView.GONE));
                    image.close(); // Close image if no snake detected
                }
            }

            @Override
            public void onFailure(Call<DetectionResponse> call, Throwable t) {
                isRequestInProgress = false;
                image.close();
                Log.e("Error", t.getMessage());
                Toast.makeText(User.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
    public void shortenUrl(String longUrl, UrlShortenerCallback callback) {
        new Thread(() -> {
            try {
                URL url = new URL("https://tinyurl.com/api-create.php?url=" + URLEncoder.encode(longUrl, "UTF-8"));
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");

                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String shortUrl = reader.readLine();
                reader.close();

                callback.onSuccess(shortUrl);
            } catch (Exception e) {
                callback.onError(e.getMessage());
            }
        }).start();
    }

    public void shortenWithBitly(String longUrl, UrlShortenerCallback callback) {
        new Thread(() -> {
            try {
                URL url = new URL("https://api-ssl.bitly.com/v4/shorten");
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Authorization", "0592eb3bebc4a8d1fcd8ced3c6fb1c7e7d98f63c");
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setDoOutput(true);

                String jsonInput = "{\"long_url\": \"" + longUrl + "\"}";
                OutputStream os = connection.getOutputStream();
                os.write(jsonInput.getBytes());
                os.close();

                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();

                JSONObject jsonResponse = new JSONObject(response.toString());
                String shortUrl = jsonResponse.getString("link");

                callback.onSuccess(shortUrl);
            } catch (Exception e) {
                callback.onError(e.getMessage());
            }
        }).start();
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
