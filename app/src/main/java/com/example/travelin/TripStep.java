package com.example.travelin;

public class TripStep {
    private final long id;
    private final String locationName;
    private final String description;
    private final String date;
    private final String time;

    public TripStep(long id, String locationName, String description, String date, String time) {
        this.id = id;
        this.locationName = locationName;
        this.description = description;
        this.date = date;
        this.time = time;
    }

    public long getId() {
        return id;
    }

    public String getLocationName() {
        return locationName;
    }

    public String getDescription() {
        return description;
    }

    public String getDate() {
        return date;
    }

    public String getTime() {
        return time;
    }
}
