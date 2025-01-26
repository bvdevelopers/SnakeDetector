package com.example.snakedetector.model;

import java.util.List;

public class CaptureResponse {
    private String image;
    private List<DetectionResponse.Detection> detections;

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public List<DetectionResponse.Detection> getDetections() {
        return detections;
    }

    public void setDetections(List<DetectionResponse.Detection> detections) {
        this.detections = detections;
    }
}
