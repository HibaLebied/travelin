package com.example.travelin;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {
    public static final String DATABASE_NAME = "travelin.db";
    public static final int DATABASE_VERSION = 3;

    public static final String TABLE_TRIPS = "trips";
    public static final String COL_ID = "id";
    public static final String COL_USER_ID = "user_id";
    public static final String COL_TRIP_TYPE = "trip_type";
    public static final String COL_DESTINATION = "destination";
    public static final String COL_TRIP_NAME = "trip_name";
    public static final String COL_START_DATE = "start_date";
    public static final String COL_END_DATE = "end_date";
    public static final String COL_HOTEL_NAME = "hotel_name";
    public static final String COL_HOTEL_ADDRESS = "hotel_address";
    public static final String COL_HOTEL_PHONE = "hotel_phone";
    public static final String COL_NOTES = "notes";
    public static final String COL_COVER_PHOTO_PATH = "cover_photo_path";
    public static final String COL_CREATED_AT = "created_at";
    public static final String TABLE_STEPS = "steps";
    public static final String TABLE_STEP_PHOTOS = "step_photos";
    public static final String TABLE_NOTIFICATIONS = "notifications";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_TRIPS + " ("
                + COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COL_USER_ID + " TEXT, "
                + COL_TRIP_TYPE + " TEXT NOT NULL, "
                + COL_DESTINATION + " TEXT NOT NULL, "
                + COL_TRIP_NAME + " TEXT, "
                + COL_START_DATE + " TEXT, "
                + COL_END_DATE + " TEXT, "
                + COL_HOTEL_NAME + " TEXT, "
                + COL_HOTEL_ADDRESS + " TEXT, "
                + COL_HOTEL_PHONE + " TEXT, "
                + COL_NOTES + " TEXT, "
                + COL_COVER_PHOTO_PATH + " TEXT, "
                + COL_CREATED_AT + " TEXT NOT NULL"
                + ")");
        createStepTables(db);
        createNotificationsTable(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            createStepTables(db);
        }
        if (oldVersion < 3) {
            createNotificationsTable(db);
        }
    }

    private void createStepTables(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_STEPS + " ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "trip_id INTEGER NOT NULL, "
                + "location_name TEXT NOT NULL, "
                + "description TEXT, "
                + "date TEXT, "
                + "time TEXT, "
                + "latitude REAL, "
                + "longitude REAL, "
                + "created_at INTEGER NOT NULL, "
                + "FOREIGN KEY(trip_id) REFERENCES " + TABLE_TRIPS + "(" + COL_ID + ") ON DELETE CASCADE"
                + ")");

        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_STEP_PHOTOS + " ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "step_id INTEGER NOT NULL, "
                + "photo_uri TEXT NOT NULL, "
                + "created_at INTEGER NOT NULL, "
                + "FOREIGN KEY(step_id) REFERENCES " + TABLE_STEPS + "(id) ON DELETE CASCADE"
                + ")");
    }

    private void createNotificationsTable(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_NOTIFICATIONS + " ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "title TEXT, "
                + "message TEXT, "
                + "type TEXT, "
                + "related_id INTEGER, "
                + "created_at TEXT, "
                + "is_read INTEGER DEFAULT 0"
                + ")");
    }
}
