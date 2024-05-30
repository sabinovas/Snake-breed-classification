package com.example.venomvision;

public class SnakeData {
    private String title;
    private String confidence;
    private String imageUrl;

    public SnakeData() {
    }

    public SnakeData(String title, String confidence, String imageUrl) {
        this.title = title;
        this.confidence = confidence;
        this.imageUrl = imageUrl;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getConfidence() {
        return confidence;
    }

    public void setConfidence(String confidence) {
        this.confidence = confidence;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }
}
