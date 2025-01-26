package com.example.snakedetector;

import android.content.Context;
import android.graphics.Bitmap;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.ToneGenerator;
import android.util.Log;
import android.widget.Toast;


import java.util.List;

public class SnakeDetector {

////    private ObjectDetector detector;
////    private MediaPlayer beepPlayer;
//ToneGenerator toneGen;
//    private Context context;
//
//    public SnakeDetector(Context context) {
//        this.context = context;
//
//        // Load the TensorFlow Lite model
//        try {
//            ObjectDetectorOptions options = ObjectDetectorOptions.builder()
//                    .setScoreThreshold(0.5f) // Confidence threshold
//                    .setMaxResults(4) // Max results
//                    .build();
//            Log.e("SnakeDetection","Error while model options"+options.toString());
//            detector = ObjectDetector.createFromFileAndOptions(context, "model.tflite", options);
//
//        } catch (Exception e) {
//            Log.e("SnakeDetection", "Error initializing model: " + e.getMessage());
//            Toast.makeText(context, "Error loading model", Toast.LENGTH_SHORT).show();
//        }
//         toneGen = new ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100);
//
////        // Initialize the beep sound
////        beepPlayer = MediaPlayer.create(context, res.raw.); // Place a beep sound file in res/raw
//    }
//
//    public void detectSnake(Bitmap bitmap) {
//        List<Detection> results = detector.detect(TensorImage.fromBitmap(bitmap));
//        if (!results.isEmpty()) {
//            toneGen.startTone(ToneGenerator.TONE_PROP_BEEP, 200); // Beep for 200ms
//            Toast.makeText(context, "Snake detected!", Toast.LENGTH_SHORT).show();
//        }
//    }

//    public void release() {
//        if (beepPlayer != null) {
//            beepPlayer.release();
//        }
//    }
}
