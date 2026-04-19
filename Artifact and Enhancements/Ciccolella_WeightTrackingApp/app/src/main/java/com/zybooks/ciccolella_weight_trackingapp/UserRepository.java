package com.zybooks.ciccolella_weight_trackingapp;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

/**
 * Repository for user login data.
 * Handles creating new users and validating login credentials.
 */
public class UserRepository {

    private final WeightTrackerDbHelper dbHelper;

    public UserRepository(Context context) {
        dbHelper = new WeightTrackerDbHelper(context);
    }

    /**
     * Creates a new user. Returns true if created, false if username exists.
     */
    public boolean createUser(String username, String password) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(WeightTrackerDbHelper.COLUMN_USER_USERNAME, username);
        values.put(WeightTrackerDbHelper.COLUMN_USER_PASSWORD, password);

        long result = db.insert(WeightTrackerDbHelper.TABLE_USERS, null, values);

        return result != -1; // -1 = username duplicate
    }

    /**
     * Validates login. Returns true if username/password match.
     */
    public boolean validateLogin(String username, String password) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        String[] columns = { WeightTrackerDbHelper.COLUMN_USER_ID };
        String selection = WeightTrackerDbHelper.COLUMN_USER_USERNAME + "=? AND " +
                WeightTrackerDbHelper.COLUMN_USER_PASSWORD + "=?";
        String[] selectionArgs = { username, password };

        Cursor cursor = db.query(
                WeightTrackerDbHelper.TABLE_USERS,
                columns,
                selection,
                selectionArgs,
                null, null, null
        );

        boolean isValid = cursor.getCount() > 0;
        cursor.close();
        return isValid;
    }
}
