package com.zybooks.ciccolella_weight_trackingapp;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * SQLite helper for the Weight Tracking app.
 * Creates and upgrades the database containing user logins
 * and daily weight entries.
 */
public class WeightTrackerDbHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "weight_tracker.db";
    private static final int DATABASE_VERSION = 1;

    // Users table
    public static final String TABLE_USERS = "users";
    public static final String COLUMN_USER_ID = "_id";
    public static final String COLUMN_USER_USERNAME = "username";
    public static final String COLUMN_USER_PASSWORD = "password";

    // Weight entries table
    public static final String TABLE_WEIGHTS = "weights";
    public static final String COLUMN_WEIGHT_ID = "_id";
    public static final String COLUMN_WEIGHT_USERNAME = "username";
    public static final String COLUMN_WEIGHT_DATE = "entry_date";
    public static final String COLUMN_WEIGHT_VALUE = "weight_value";

    // Table for storing login credentials (one row per username).
    private static final String SQL_CREATE_USERS =
            "CREATE TABLE " + TABLE_USERS + " (" +
                    COLUMN_USER_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COLUMN_USER_USERNAME + " TEXT UNIQUE NOT NULL, " +
                    COLUMN_USER_PASSWORD + " TEXT NOT NULL);";

    // Table for storing daily weight entries (multiple rows per user).
    private static final String SQL_CREATE_WEIGHTS =
            "CREATE TABLE " + TABLE_WEIGHTS + " (" +
                    COLUMN_WEIGHT_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COLUMN_WEIGHT_USERNAME + " TEXT NOT NULL, " +
                    COLUMN_WEIGHT_DATE + " TEXT NOT NULL, " +
                    COLUMN_WEIGHT_VALUE + " REAL NOT NULL);";

    public WeightTrackerDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_USERS);
        db.execSQL(SQL_CREATE_WEIGHTS);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_USERS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_WEIGHTS);
        onCreate(db);
    }
}
