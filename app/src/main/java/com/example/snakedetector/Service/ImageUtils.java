package com.example.snakedetector.Service;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.MediaStore;

import java.io.ByteArrayOutputStream;
import java.io.File;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
public class ImageUtils {
    public static MultipartBody.Part prepareImageForUpload(Bitmap bitmap, String name) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
        byte[] byteArray = stream.toByteArray();

        RequestBody requestBody = RequestBody.create(MediaType.parse("image/png"), byteArray);
        return MultipartBody.Part.createFormData("file", name, requestBody);
    }
}
