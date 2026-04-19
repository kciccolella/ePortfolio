package com.zybooks.ciccolella_weight_trackingapp;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import org.mindrot.jbcrypt.BCrypt;

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
     public boolean validateLogin(String username, String enteredPassword) {
        // Fetch the hashed password from the database based on the username
        String storedHashedPassword = getStoredHashedPasswordForUser(username);

        // Check if the entered password matches the stored hashed password
        if (storedHashedPassword == null) {
            return false;  // Username doesn't exist
        }

        // Compare the entered password with the stored hashed password
        return BCrypt.checkpw(enteredPassword, storedHashedPassword);
    }

    /**
     * This method retrieves the stored hashed password for the given username.
     * You should replace this with your actual method for querying the database.
     */
    private String getStoredHashedPasswordForUser(String username) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String[] projection = {
                WeightTrackerDbHelper.COLUMN_USER_PASSWORD  // Column name for the password
        };
        String selection = WeightTrackerDbHelper.COLUMN_USER_USERNAME + " = ?";
        String[] selectionArgs = { username };

        // Query the database for the user entry
        Cursor cursor = db.query(
                WeightTrackerDbHelper.TABLE_USERS,
                projection,
                selection,
                selectionArgs,
                null,
                null,
                null
        );

        String hashedPassword = null;

        // Check if the cursor contains results
        if (cursor != null && cursor.moveToFirst()) {
            int passwordColumnIndex = cursor.getColumnIndex(WeightTrackerDbHelper.COLUMN_USER_PASSWORD);

            if (passwordColumnIndex != -1) {  // Ensure the column exists
                hashedPassword = cursor.getString(passwordColumnIndex);  // Get the stored hashed password
            }
        }

        // Close the cursor
        if (cursor != null) {
            cursor.close();
        }

        return hashedPassword;  // Return the stored hashed password or null if not found
    }
}
