package com.example.snakedetector.Interface;

public interface UrlShortenerCallback {
    void onSuccess(String shortUrl);
    void onError(String error);
}
