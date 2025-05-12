package com.example.videosharinggood.models;

public class Video {
    private String title;
    private String uploadDate;
    private String uploaderUsername;
    private String uploaderEmail;
    private String videoUrl;

    public Video() {} // Firestore-hoz kell

    public Video(String title, String uploadDate, String uploaderUsername, String uploaderEmail, String videoUrl) {
        this.title = title;
        this.uploadDate = uploadDate;
        this.uploaderUsername = uploaderUsername;
        this.uploaderEmail = uploaderEmail;
        this.videoUrl = videoUrl;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getUploadDate() {
        return uploadDate;
    }

    public void setUploadDate(String uploadDate) {
        this.uploadDate = uploadDate;
    }

    public String getUploaderUsername() {
        return uploaderUsername;
    }

    public void setUploaderUsername(String uploaderUsername) {
        this.uploaderUsername = uploaderUsername;
    }

    public String getUploaderEmail() {
        return uploaderEmail;
    }

    public void setUploaderEmail(String uploaderEmail) {
        this.uploaderEmail = uploaderEmail;
    }

    public String getVideoUrl() {
        return videoUrl;
    }

    public void setVideoUrl(String videoUrl) {
        this.videoUrl = videoUrl;
    }
}
