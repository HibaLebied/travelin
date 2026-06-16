package com.example.travelin;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class ProfileStatsDao {
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
    private final DatabaseHelper databaseHelper;

    public ProfileStatsDao(Context context) {
        databaseHelper = new DatabaseHelper(context);
    }

    public ProfileStats getStats(String userId) {
        SQLiteDatabase db = databaseHelper.getReadableDatabase();
        Cursor cursor = db.query(
                DatabaseHelper.TABLE_TRIPS,
                new String[]{
                        DatabaseHelper.COL_START_DATE,
                        DatabaseHelper.COL_END_DATE,
                        DatabaseHelper.COL_COVER_PHOTO_PATH
                },
                DatabaseHelper.COL_USER_ID + "=?",
                new String[]{userId},
                null,
                null,
                null
        );

        int trips = 0;
        int steps = 0;
        int photos = 0;
        int days = 0;
        try {
            while (cursor.moveToNext()) {
                trips++;
                steps++;
                String coverPhoto = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_COVER_PHOTO_PATH));
                if (!TextUtils.isEmpty(coverPhoto)) {
                    photos++;
                }
                days += countDays(
                        cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_START_DATE)),
                        cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_END_DATE))
                );
            }
        } finally {
            cursor.close();
        }

        return new ProfileStats(trips, steps, photos, days);
    }

    private int countDays(String startDate, String endDate) {
        if (TextUtils.isEmpty(startDate) || TextUtils.isEmpty(endDate)) {
            return 0;
        }
        try {
            Date start = DATE_FORMAT.parse(startDate);
            Date end = DATE_FORMAT.parse(endDate);
            if (start == null || end == null || end.before(start)) {
                return 0;
            }
            long diff = end.getTime() - start.getTime();
            return (int) TimeUnit.MILLISECONDS.toDays(diff) + 1;
        } catch (ParseException e) {
            return 0;
        }
    }

    public static class ProfileStats {
        public final int trips;
        public final int steps;
        public final int photos;
        public final int days;

        public ProfileStats(int trips, int steps, int photos, int days) {
            this.trips = trips;
            this.steps = steps;
            this.photos = photos;
            this.days = days;
        }
    }
}
