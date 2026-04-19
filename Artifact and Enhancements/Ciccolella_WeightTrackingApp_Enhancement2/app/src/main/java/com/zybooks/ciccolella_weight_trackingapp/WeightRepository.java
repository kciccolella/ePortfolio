package com.zybooks.ciccolella_weight_trackingapp;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.Arrays;


/**
 * Handles CRUD operations for weight entries in the database.
 */
public class WeightRepository {

    private final WeightTrackerDbHelper dbHelper;

    public WeightRepository(Context context) {
        dbHelper = new WeightTrackerDbHelper(context);
    }

    /**
     * Inserts a new weight entry or updates the existing one
     * for the same username + date combination.
     */
    public void addOrUpdateWeight(String username, String date, double weightValue) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(WeightTrackerDbHelper.COLUMN_WEIGHT_USERNAME, username);
        values.put(WeightTrackerDbHelper.COLUMN_WEIGHT_DATE, date);
        values.put(WeightTrackerDbHelper.COLUMN_WEIGHT_VALUE, weightValue);

        String selection = WeightTrackerDbHelper.COLUMN_WEIGHT_USERNAME + "=? AND " +
                WeightTrackerDbHelper.COLUMN_WEIGHT_DATE + "=?";
        String[] selectionArgs = { username, date };

        int rowsUpdated = db.update(
                WeightTrackerDbHelper.TABLE_WEIGHTS,
                values,
                selection,
                selectionArgs
        );

        if (rowsUpdated == 0) {
            // No existing row, insert a new one
            db.insert(WeightTrackerDbHelper.TABLE_WEIGHTS, null, values);
        }
    }

    /**
     * Returns all weight entries for the given user, ordered by date (newest first).
     * Caller is responsible for closing the returned Cursor.
     */
    public Cursor getWeightsForUser(String username) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        String[] columns = {
                WeightTrackerDbHelper.COLUMN_WEIGHT_ID,
                WeightTrackerDbHelper.COLUMN_WEIGHT_DATE,
                WeightTrackerDbHelper.COLUMN_WEIGHT_VALUE
        };

        String selection = WeightTrackerDbHelper.COLUMN_WEIGHT_USERNAME + "=?";
        String[] selectionArgs = { username };

        String orderBy = WeightTrackerDbHelper.COLUMN_WEIGHT_DATE + " DESC";

        return db.query(
                WeightTrackerDbHelper.TABLE_WEIGHTS,
                columns,
                selection,
                selectionArgs,
                null,
                null,
                orderBy
        );
    }

    /**
     * Deletes the weight entry with the given primary key ID.
     */
    public void deleteWeight(long id) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        String whereClause = WeightTrackerDbHelper.COLUMN_WEIGHT_ID + "=?";
        String[] whereArgs = { String.valueOf(id) };

        db.delete(WeightTrackerDbHelper.TABLE_WEIGHTS, whereClause, whereArgs);
    }

    /**
     * Updates an existing weight entry identified by its ID.
     */
    public void updateWeightById(long id, String username, String date, double weightValue) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(WeightTrackerDbHelper.COLUMN_WEIGHT_USERNAME, username);
        values.put(WeightTrackerDbHelper.COLUMN_WEIGHT_DATE, date);
        values.put(WeightTrackerDbHelper.COLUMN_WEIGHT_VALUE, weightValue);

        String whereClause = WeightTrackerDbHelper.COLUMN_WEIGHT_ID + "=?";
        String[] whereArgs = { String.valueOf(id) };

        db.update(
                WeightTrackerDbHelper.TABLE_WEIGHTS,
                values,
                whereClause,
                whereArgs
        );
    }

    public void sortEntriesByDate(WeightEntry[] entries, boolean descending) {
        // Perform MergeSort
        mergeSort(entries, 0, entries.length - 1, descending);
    }

    private void mergeSort(WeightEntry[] array, int left, int right, boolean descending) {
        if (left < right) {
            int mid = (left + right) / 2;

            // Recursively split the array
            mergeSort(array, left, mid, descending);
            mergeSort(array, mid + 1, right, descending);

            // Merge the sorted halves
            merge(array, left, mid, right, descending);
        }
    }

    private void merge(WeightEntry[] array, int left, int mid, int right, boolean descending) {
        // Calculate the sizes of the two subarrays
        int leftSize = mid - left + 1;
        int rightSize = right - mid;

        // Create temporary arrays for the two halves
        WeightEntry[] leftArray = Arrays.copyOfRange(array, left, mid + 1);
        WeightEntry[] rightArray = Arrays.copyOfRange(array, mid + 1, right + 1);

        // Merge the two subarrays back into the original array
        int i = 0, j = 0, k = left;
        while (i < leftSize && j < rightSize) {
            if ((descending && leftArray[i].compareTo(rightArray[j]) > 0) ||
                    (!descending && leftArray[i].compareTo(rightArray[j]) < 0)) {
                array[k] = leftArray[i];
                i++;
            } else {
                array[k] = rightArray[j];
                j++;
            }
            k++;
        }

        // Copy the remaining elements if any
        while (i < leftSize) {
            array[k] = leftArray[i];
            i++;
            k++;
        }

        while (j < rightSize) {
            array[k] = rightArray[j];
            j++;
            k++;
        }
    }

}
