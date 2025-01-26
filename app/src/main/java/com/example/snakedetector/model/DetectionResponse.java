package com.example.snakedetector.model;

import java.util.List;

public class DetectionResponse {
    private List<Detection> detections;

    public List<Detection> getDetections() {
        return detections;
    }

    public void setDetections(List<Detection> detections) {
        this.detections = detections;
    }

    public static class Detection {
        private String className;
        private double confidence;
        private int[] box;

        public String getClassName() {
            return className;
        }

        public void setClassName(String className) {
            this.className = className;
        }

        public double getConfidence() {
            return confidence;
        }

        public void setConfidence(double confidence) {
            this.confidence = confidence;
        }

        public int[] getBox() {
            return box;
        }

        public void setBox(int[] box) {
            this.box = box;
        }
    }
}
