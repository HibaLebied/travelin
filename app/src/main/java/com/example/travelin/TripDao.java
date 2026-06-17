package com.example.travelin;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TripDao {
    private static final SimpleDateFormat STORAGE_FORMAT = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
    private static final SimpleDateFormat DISPLAY_FORMAT = new SimpleDateFormat("dd MMM yyyy", Locale.FRENCH);

    private final DatabaseHelper databaseHelper;

    public TripDao(Context context) {
        databaseHelper = new DatabaseHelper(context);
    }

    public long insertTrip(Trip trip) {
        SQLiteDatabase db = databaseHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.COL_USER_ID, trip.getUserId());
        values.put(DatabaseHelper.COL_TRIP_TYPE, trip.getTripType());
        values.put(DatabaseHelper.COL_DESTINATION, trip.getDestination());
        values.put(DatabaseHelper.COL_TRIP_NAME, trip.getName());
        values.put(DatabaseHelper.COL_START_DATE, trip.getStartDate());
        values.put(DatabaseHelper.COL_END_DATE, trip.getEndDate());
        values.put(DatabaseHelper.COL_HOTEL_NAME, trip.getHotelName());
        values.put(DatabaseHelper.COL_HOTEL_ADDRESS, trip.getHotelAddress());
        values.put(DatabaseHelper.COL_HOTEL_PHONE, trip.getHotelPhone());
        values.put(DatabaseHelper.COL_NOTES, trip.getNotes());
        values.put(DatabaseHelper.COL_COVER_PHOTO_PATH, trip.getCoverPhotoPath());
        values.put(DatabaseHelper.COL_CREATED_AT, trip.getCreatedAt());
        return db.insert(DatabaseHelper.TABLE_TRIPS, null, values);
    }

    public long insertStep(long tripId, String locationName, String description, String date, String time,
                           Double latitude, Double longitude) {
        SQLiteDatabase db = databaseHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("trip_id", tripId);
        values.put("location_name", locationName);
        values.put("description", description);
        values.put("date", date);
        values.put("time", time);
        if (latitude == null) {
            values.putNull("latitude");
        } else {
            values.put("latitude", latitude);
        }
        if (longitude == null) {
            values.putNull("longitude");
        } else {
            values.put("longitude", longitude);
        }
        values.put("created_at", System.currentTimeMillis());
        return db.insert(DatabaseHelper.TABLE_STEPS, null, values);
    }

    public int updateStep(long stepId, String locationName, String description, String date, String time,
                          Double latitude, Double longitude) {
        SQLiteDatabase db = databaseHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("location_name", locationName);
        values.put("description", description);
        values.put("date", date);
        values.put("time", time);
        if (latitude == null) {
            values.putNull("latitude");
        } else {
            values.put("latitude", latitude);
        }
        if (longitude == null) {
            values.putNull("longitude");
        } else {
            values.put("longitude", longitude);
        }
        return db.update(
                DatabaseHelper.TABLE_STEPS,
                values,
                "id=?",
                new String[]{String.valueOf(stepId)}
        );
    }

    public long insertStepPhoto(long stepId, String photoUri) {
        SQLiteDatabase db = databaseHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("step_id", stepId);
        values.put("photo_uri", photoUri);
        values.put("created_at", System.currentTimeMillis());
        return db.insert(DatabaseHelper.TABLE_STEP_PHOTOS, null, values);
    }

    public List<StepMemoryPhoto> getStepMemoryPhotos(String userId) {
        List<StepMemoryPhoto> photos = new ArrayList<>();
        SQLiteDatabase db = databaseHelper.getReadableDatabase();
        String query = "SELECT p.photo_uri, s.location_name, s.date, t.trip_name, t.destination "
                + "FROM " + DatabaseHelper.TABLE_STEP_PHOTOS + " p "
                + "INNER JOIN " + DatabaseHelper.TABLE_STEPS + " s ON p.step_id = s.id "
                + "INNER JOIN " + DatabaseHelper.TABLE_TRIPS + " t ON s.trip_id = t." + DatabaseHelper.COL_ID + " "
                + "WHERE t." + DatabaseHelper.COL_USER_ID + "=? "
                + "ORDER BY p.created_at DESC";
        Cursor cursor = db.rawQuery(query, new String[]{userId});
        try {
            while (cursor.moveToNext()) {
                photos.add(new StepMemoryPhoto(
                        cursor.getString(0),
                        cursor.getString(1),
                        cursor.getString(2),
                        cursor.getString(3),
                        cursor.getString(4)
                ));
            }
        } finally {
            cursor.close();
        }
        return photos;
    }

    public List<TripStep> getStepsForTrip(long tripId) {
        List<TripStep> steps = new ArrayList<>();
        SQLiteDatabase db = databaseHelper.getReadableDatabase();
        Cursor cursor = db.query(
                DatabaseHelper.TABLE_STEPS,
                new String[]{"id", "trip_id", "location_name", "description", "date", "time", "latitude", "longitude"},
                "trip_id=?",
                new String[]{String.valueOf(tripId)},
                null,
                null,
                "date, time"
        );
        try {
            while (cursor.moveToNext()) {
                steps.add(new TripStep(
                        cursor.getLong(0),
                        cursor.getLong(1),
                        cursor.getString(2),
                        cursor.getString(3),
                        cursor.getString(4),
                        cursor.getString(5),
                        cursor.isNull(6) ? null : cursor.getDouble(6),
                        cursor.isNull(7) ? null : cursor.getDouble(7)
                ));
            }
        } finally {
            cursor.close();
        }
        return steps;
    }

    public List<Long> getStepIdsForTrip(long tripId) {
        List<Long> stepIds = new ArrayList<>();
        SQLiteDatabase db = databaseHelper.getReadableDatabase();
        Cursor cursor = db.query(
                DatabaseHelper.TABLE_STEPS,
                new String[]{"id"},
                "trip_id=?",
                new String[]{String.valueOf(tripId)},
                null,
                null,
                null
        );
        try {
            while (cursor.moveToNext()) {
                stepIds.add(cursor.getLong(0));
            }
        } finally {
            cursor.close();
        }
        return stepIds;
    }

    public TripStep getStepById(long stepId) {
        SQLiteDatabase db = databaseHelper.getReadableDatabase();
        Cursor cursor = db.query(
                DatabaseHelper.TABLE_STEPS,
                new String[]{"id", "trip_id", "location_name", "description", "date", "time", "latitude", "longitude"},
                "id=?",
                new String[]{String.valueOf(stepId)},
                null,
                null,
                null
        );
        try {
            if (cursor.moveToFirst()) {
                return new TripStep(
                        cursor.getLong(0),
                        cursor.getLong(1),
                        cursor.getString(2),
                        cursor.getString(3),
                        cursor.getString(4),
                        cursor.getString(5),
                        cursor.isNull(6) ? null : cursor.getDouble(6),
                        cursor.isNull(7) ? null : cursor.getDouble(7)
                );
            }
        } finally {
            cursor.close();
        }
        return null;
    }

    public List<Trip> getTripsForHome(String userId) {
        List<Trip> upcoming = new ArrayList<>();
        List<Trip> past = new ArrayList<>();
        SQLiteDatabase db = databaseHelper.getReadableDatabase();
        Cursor cursor = db.query(
                DatabaseHelper.TABLE_TRIPS,
                null,
                DatabaseHelper.COL_USER_ID + "=?",
                new String[]{userId},
                null,
                null,
                DatabaseHelper.COL_CREATED_AT + " DESC"
        );

        try {
            while (cursor.moveToNext()) {
                Trip trip = fromCursor(cursor);
                if (Trip.TYPE_PAST.equals(trip.getTripType())) {
                    past.add(trip);
                } else {
                    upcoming.add(trip);
                }
            }
        } finally {
            cursor.close();
        }

        applySections(upcoming, "A VENIR");
        applySections(past, "VOYAGES PASSES");
        upcoming.addAll(past);
        return upcoming;
    }

    public Trip getTripById(long tripId) {
        SQLiteDatabase db = databaseHelper.getReadableDatabase();
        Cursor cursor = db.query(
                DatabaseHelper.TABLE_TRIPS,
                null,
                DatabaseHelper.COL_ID + "=?",
                new String[]{String.valueOf(tripId)},
                null,
                null,
                null
        );
        try {
            if (cursor.moveToFirst()) {
                return fromCursor(cursor);
            }
        } finally {
            cursor.close();
        }
        return null;
    }

    public boolean deleteTrip(long tripId) {
        if (tripId <= 0) {
            return false;
        }

        SQLiteDatabase db = databaseHelper.getWritableDatabase();
        db.beginTransaction();
        try {
            String[] stepIds = getStepIdsForTrip(db, tripId);
            if (stepIds.length > 0) {
                String placeholders = makePlaceholders(stepIds.length);
                db.delete(
                        DatabaseHelper.TABLE_STEP_PHOTOS,
                        "step_id IN (" + placeholders + ")",
                        stepIds
                );
                db.delete(
                        DatabaseHelper.TABLE_NOTIFICATIONS,
                        "(type IN (?, ?) AND related_id IN (" + placeholders + "))",
                        combineArgs(
                                new String[]{
                                        NotificationHelper.TYPE_STEP_TODAY,
                                        NotificationHelper.TYPE_ADD_STEP_PHOTOS
                                },
                                stepIds
                        )
                );
            }

            db.delete(
                    DatabaseHelper.TABLE_STEPS,
                    "trip_id=?",
                    new String[]{String.valueOf(tripId)}
            );
            db.delete(
                    DatabaseHelper.TABLE_NOTIFICATIONS,
                    "(type IN (?, ?, ?) AND related_id=?)",
                    new String[]{
                            NotificationHelper.TYPE_DEPARTURE_TOMORROW,
                            NotificationHelper.TYPE_TRIP_TODAY,
                            NotificationHelper.TYPE_TRIP_FINISHED,
                            String.valueOf(tripId)
                    }
            );
            int deletedTrips = db.delete(
                    DatabaseHelper.TABLE_TRIPS,
                    DatabaseHelper.COL_ID + "=?",
                    new String[]{String.valueOf(tripId)}
            );
            db.setTransactionSuccessful();
            return deletedTrips > 0;
        } finally {
            db.endTransaction();
        }
    }

    public List<String> getPhotoUrisForTrip(long tripId) {
        List<String> photoUris = new ArrayList<>();
        SQLiteDatabase db = databaseHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT p.photo_uri "
                        + "FROM " + DatabaseHelper.TABLE_STEP_PHOTOS + " p "
                        + "INNER JOIN " + DatabaseHelper.TABLE_STEPS + " s ON p.step_id = s.id "
                        + "WHERE s.trip_id=?",
                new String[]{String.valueOf(tripId)}
        );
        try {
            while (cursor.moveToNext()) {
                photoUris.add(cursor.getString(0));
            }
        } finally {
            cursor.close();
        }

        Trip trip = getTripById(tripId);
        if (trip != null && !TextUtils.isEmpty(trip.getCoverPhotoPath())) {
            photoUris.add(trip.getCoverPhotoPath());
        }
        return photoUris;
    }

    private String[] getStepIdsForTrip(SQLiteDatabase db, long tripId) {
        List<String> stepIds = new ArrayList<>();
        Cursor cursor = db.query(
                DatabaseHelper.TABLE_STEPS,
                new String[]{"id"},
                "trip_id=?",
                new String[]{String.valueOf(tripId)},
                null,
                null,
                null
        );
        try {
            while (cursor.moveToNext()) {
                stepIds.add(String.valueOf(cursor.getLong(0)));
            }
        } finally {
            cursor.close();
        }
        return stepIds.toArray(new String[0]);
    }

    private String makePlaceholders(int count) {
        StringBuilder placeholders = new StringBuilder();
        for (int i = 0; i < count; i++) {
            if (i > 0) {
                placeholders.append(",");
            }
            placeholders.append("?");
        }
        return placeholders.toString();
    }

    private String[] combineArgs(String[] first, String[] second) {
        String[] combined = new String[first.length + second.length];
        System.arraycopy(first, 0, combined, 0, first.length);
        System.arraycopy(second, 0, combined, first.length, second.length);
        return combined;
    }

    private Trip fromCursor(Cursor cursor) {
        Trip trip = new Trip();
        trip.setId(cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_ID)));
        trip.setUserId(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_USER_ID)));
        trip.setTripType(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_TRIP_TYPE)));
        trip.setDestination(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_DESTINATION)));
        trip.setName(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_TRIP_NAME)));
        trip.setStartDate(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_START_DATE)));
        trip.setEndDate(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_END_DATE)));
        trip.setHotelName(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_HOTEL_NAME)));
        trip.setHotelAddress(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_HOTEL_ADDRESS)));
        trip.setHotelPhone(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_HOTEL_PHONE)));
        trip.setNotes(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_NOTES)));
        trip.setCoverPhotoPath(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_COVER_PHOTO_PATH)));
        trip.setCreatedAt(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_CREATED_AT)));
        trip.setDates(formatDateRange(trip.getStartDate(), trip.getEndDate()));
        trip.setLocations("1 lieu");
        trip.setImageResId(Trip.TYPE_PAST.equals(trip.getTripType()) ? R.drawable.travel_balloons_bg : R.drawable.travel_beach_bg);
        if (TextUtils.isEmpty(trip.getName())) {
            trip.setName(trip.getDestination());
        }
        return trip;
    }

    private void applySections(List<Trip> trips, String section) {
        for (int i = 0; i < trips.size(); i++) {
            trips.get(i).setSection(i == 0 ? section : null);
        }
    }

    private String formatDateRange(String startDate, String endDate) {
        String start = formatDate(startDate);
        String end = formatDate(endDate);
        if (!TextUtils.isEmpty(start) && !TextUtils.isEmpty(end)) {
            return start + " - " + end;
        }
        if (!TextUtils.isEmpty(start)) {
            return start;
        }
        return "Dates a definir";
    }

    private String formatDate(String value) {
        if (TextUtils.isEmpty(value)) {
            return "";
        }
        try {
            Date date = STORAGE_FORMAT.parse(value);
            return date == null ? "" : DISPLAY_FORMAT.format(date);
        } catch (ParseException e) {
            return value;
        }
    }
}
