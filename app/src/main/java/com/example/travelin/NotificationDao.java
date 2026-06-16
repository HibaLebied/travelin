package com.example.travelin;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class NotificationDao {
    private static final SimpleDateFormat STORAGE_FORMAT =
            new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
    private static final SimpleDateFormat TIME_FORMAT =
            new SimpleDateFormat("HH:mm", Locale.FRANCE);
    private static final SimpleDateFormat DATE_FORMAT =
            new SimpleDateFormat("dd MMM yyyy", Locale.FRENCH);

    private final DatabaseHelper databaseHelper;

    public NotificationDao(Context context) {
        databaseHelper = new DatabaseHelper(context);
    }

    public long insertNotification(String title, String message, String type, long relatedId) {
        SQLiteDatabase db = databaseHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("title", title);
        values.put("message", message);
        values.put("type", type);
        if (relatedId > 0) {
            values.put("related_id", relatedId);
        } else {
            values.putNull("related_id");
        }
        values.put("created_at", STORAGE_FORMAT.format(new Date()));
        values.put("is_read", 0);
        return db.insert(DatabaseHelper.TABLE_NOTIFICATIONS, null, values);
    }

    public List<NotificationItem> getNotifications() {
        deleteUnsupportedNotifications();
        List<NotificationItem> items = new ArrayList<>();
        SQLiteDatabase db = databaseHelper.getReadableDatabase();
        Cursor cursor = db.query(
                DatabaseHelper.TABLE_NOTIFICATIONS,
                new String[]{"id", "title", "message", "type", "related_id", "created_at", "is_read"},
                null,
                null,
                null,
                null,
                "id DESC"
        );
        try {
            String previousSection = null;
            while (cursor.moveToNext()) {
                String createdAt = cursor.getString(5);
                String section = sectionFor(createdAt);
                boolean showSection = !TextUtils.equals(previousSection, section);
                previousSection = section;
                items.add(new NotificationItem(
                        cursor.getLong(0),
                        showSection ? section : null,
                        cursor.getString(1),
                        cursor.getString(2),
                        timeFor(createdAt),
                        iconFor(cursor.getString(3)),
                        cursor.getInt(6) == 0,
                        cursor.getString(3),
                        cursor.isNull(4) ? 0 : cursor.getLong(4)
                ));
            }
        } finally {
            cursor.close();
        }
        return items;
    }

    public void markAsRead(long notificationId) {
        if (notificationId <= 0) {
            return;
        }
        SQLiteDatabase db = databaseHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("is_read", 1);
        db.update(DatabaseHelper.TABLE_NOTIFICATIONS, values, "id=?", new String[]{String.valueOf(notificationId)});
    }

    public void deleteUnsupportedNotifications() {
        SQLiteDatabase db = databaseHelper.getWritableDatabase();
        db.delete(
                DatabaseHelper.TABLE_NOTIFICATIONS,
                "type NOT IN (?, ?, ?, ?, ?)",
                new String[]{
                        NotificationHelper.TYPE_DEPARTURE_TOMORROW,
                        NotificationHelper.TYPE_TRIP_TODAY,
                        NotificationHelper.TYPE_STEP_TODAY,
                        NotificationHelper.TYPE_ADD_STEP_PHOTOS,
                        NotificationHelper.TYPE_TRIP_FINISHED
                }
        );
    }

    private String sectionFor(String createdAt) {
        Date date = parse(createdAt);
        if (date == null) {
            return "Notifications";
        }
        SimpleDateFormat dayFormat = new SimpleDateFormat("yyyyMMdd", Locale.US);
        String today = dayFormat.format(new Date());
        String value = dayFormat.format(date);
        if (today.equals(value)) {
            return "Aujourd'hui";
        }
        return DATE_FORMAT.format(date);
    }

    private String timeFor(String createdAt) {
        Date date = parse(createdAt);
        return date == null ? "" : TIME_FORMAT.format(date);
    }

    private Date parse(String createdAt) {
        try {
            return TextUtils.isEmpty(createdAt) ? null : STORAGE_FORMAT.parse(createdAt);
        } catch (Exception e) {
            return null;
        }
    }

    private int iconFor(String type) {
        if (NotificationHelper.TYPE_DEPARTURE_TOMORROW.equals(type)) {
            return R.drawable.ic_notification_trip;
        }
        if (NotificationHelper.TYPE_TRIP_TODAY.equals(type)) {
            return R.drawable.ic_notification_bell;
        }
        if (NotificationHelper.TYPE_STEP_TODAY.equals(type)) {
            return R.drawable.ic_notification_location;
        }
        if (NotificationHelper.TYPE_ADD_STEP_PHOTOS.equals(type)) {
            return R.drawable.ic_notification_info;
        }
        if (NotificationHelper.TYPE_TRIP_FINISHED.equals(type)) {
            return R.drawable.ic_notification_sync;
        }
        return R.drawable.ic_notification;
    }
}
