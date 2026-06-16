package com.example.travelin;

public class StepMemoryPhoto {
    private final String photoUri;
    private final String stepName;
    private final String date;
    private final String tripName;
    private final String destination;

    public StepMemoryPhoto(String photoUri, String stepName, String date, String tripName, String destination) {
        this.photoUri = photoUri;
        this.stepName = stepName;
        this.date = date;
        this.tripName = tripName;
        this.destination = destination;
    }

    public String getPhotoUri() {
        return photoUri;
    }

    public String getStepName() {
        return stepName;
    }

    public String getDate() {
        return date;
    }

    public String getTripName() {
        return tripName;
    }

    public String getDestination() {
        return destination;
    }
}
