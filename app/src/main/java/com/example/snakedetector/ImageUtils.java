package com.example.snakedetector;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.media.Image;

import androidx.annotation.OptIn;
import androidx.camera.core.ExperimentalGetImage;
import androidx.camera.core.ImageProxy;

import java.nio.ByteBuffer;
public class ImageUtils {
    public static Bitmap imageProxyToBitmap(ImageProxy imageProxy) {
        @OptIn(markerClass = ExperimentalGetImage.class) Image image = imageProxy.getImage();
        if (image == null || image.getFormat() != ImageFormat.YUV_420_888) {
            return null;
        }
        ByteBuffer buffer = image.getPlanes()[0].getBuffer();
        byte[] bytes = new byte[buffer.remaining()];
        buffer.get(bytes);

        Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length, null);
        return bitmap.copy(Bitmap.Config.ARGB_8888, true);
    }
}
