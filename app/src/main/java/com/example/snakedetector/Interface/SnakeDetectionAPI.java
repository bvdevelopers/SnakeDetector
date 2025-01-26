package com.example.snakedetector.Interface;
import com.example.snakedetector.model.CaptureResponse;
import com.example.snakedetector.model.DetectionResponse;
import com.example.snakedetector.model.ImageRequest;

import okhttp3.MultipartBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.http.Body;
import retrofit2.http.GET;

public interface SnakeDetectionAPI {
    @Multipart
    @POST("/predict")
    Call<ResponseBody> uploadImage(@Part MultipartBody.Part file);

    // Detect endpoint - POST request with base64 image
    @POST("/detect")
    Call<DetectionResponse> detectSnake(@Body ImageRequest imageRequest);

    // Capture endpoint - GET request to capture and detect
    @GET("/capture")
    Call<CaptureResponse> captureAndDetect();
}
