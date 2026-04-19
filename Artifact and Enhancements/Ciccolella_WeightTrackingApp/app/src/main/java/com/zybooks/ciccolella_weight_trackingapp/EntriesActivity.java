package com.zybooks.ciccolella_weight_trackingapp;

import android.Manifest;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.telephony.SmsManager;

import androidx.core.content.ContextCompat;

import android.content.Intent;

import android.view.Gravity;

import android.database.Cursor;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

/**
 * Shows the logged-in user's weight entries in a simple grid layout.
 * Supports create, read, update (via row tap), and delete, and checks for goal-weight SMS alerts.
 */
public class EntriesActivity extends AppCompatActivity {

    private String username;
    private WeightRepository weightRepository;

    private LinearLayout layoutEntriesList;
    private EditText editTextEntryDate;
    private EditText editTextEntryWeight;
    private Button buttonSaveEntry;
    // Tracks which entry (by database ID) is currently being edited.
    // If null, the Save button will insert a new row instead of updating.
    private Long editingEntryId = null;

    private static final String PREFS_NAME = "WeightTrackerPrefs";
    private static final String KEY_SMS_ENABLED = "sms_enabled";
    private static final String KEY_NOTIFY_ON_GOAL = "notify_on_goal";
    private static final String KEY_GOAL_WEIGHT = "goal_weight";
    private static final String KEY_PHONE_NUMBER = "phone_number";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_entries);

        // Get the username passed from MainActivity (login)
        username = getIntent().getStringExtra("username");
        if (username == null || username.trim().isEmpty()) {
            // Fallback, should not normally happen if login flows correctly
            username = "localuser";
        }

        weightRepository = new WeightRepository(this);

        layoutEntriesList = findViewById(R.id.layoutEntriesList);
        editTextEntryDate = findViewById(R.id.editTextEntryDate);
        editTextEntryWeight = findViewById(R.id.editTextEntryWeight);
        buttonSaveEntry = findViewById(R.id.buttonSaveEntry);

        // Notifications button
        Button buttonNotifications = findViewById(R.id.buttonNotifications);
        buttonNotifications.setOnClickListener(v -> {
            Intent intent = new Intent(EntriesActivity.this, NotificationsActivity.class);
            // Optional: pass username if you ever want per-user settings
            intent.putExtra("username", username);
            startActivity(intent);
        });

        buttonSaveEntry.setOnClickListener(v -> handleSaveEntry());

        // Load existing entries from the database
        loadEntries();
    }


    /**
     * Handles the Save button: validates input, then inserts or updates a weight entry.
     */
    private void handleSaveEntry() {
        String date = editTextEntryDate.getText().toString().trim();
        String weightText = editTextEntryWeight.getText().toString().trim();

        if (date.isEmpty() || weightText.isEmpty()) {
            Toast.makeText(this, "Please enter a date and a weight", Toast.LENGTH_SHORT).show();
            return;
        }

        double weightValue;
        try {
            weightValue = Double.parseDouble(weightText);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Weight must be a number", Toast.LENGTH_SHORT).show();
            return;
        }

        if (editingEntryId != null) {
            // We are in "edit" mode: update the existing row instead of inserting a new one.
            weightRepository.updateWeightById(editingEntryId, username, date, weightValue);
            Toast.makeText(this, "Entry updated", Toast.LENGTH_SHORT).show();

            editingEntryId = null;
            buttonSaveEntry.setText("Save Entry");
        } else {
            // No row selected: create a new entry (or update matching date if it already exists).
            weightRepository.addOrUpdateWeight(username, date, weightValue);
            Toast.makeText(this, "Entry saved", Toast.LENGTH_SHORT).show();
        }

        // Check for goal + SMS
        checkForGoalAndSendSmsIfNeeded(weightValue);

        // Clear text fields
        editTextEntryDate.setText("");
        editTextEntryWeight.setText("");

        // Reload list to reflect changes
        loadEntries();
    }

    /**
     * Loads all entries for the current user and repopulates the grid.
     */
    private void loadEntries() {
        // Keep the header row (index 0), wipe any existing data rows
        int childCount = layoutEntriesList.getChildCount();
        if (childCount > 1) {
            layoutEntriesList.removeViews(1, childCount - 1);
        }

        Cursor cursor = weightRepository.getWeightsForUser(username);
        try {
            int idColIndex = cursor.getColumnIndexOrThrow(WeightTrackerDbHelper.COLUMN_WEIGHT_ID);
            int dateColIndex = cursor.getColumnIndexOrThrow(WeightTrackerDbHelper.COLUMN_WEIGHT_DATE);
            int valueColIndex = cursor.getColumnIndexOrThrow(WeightTrackerDbHelper.COLUMN_WEIGHT_VALUE);

            while (cursor.moveToNext()) {
                long id = cursor.getLong(idColIndex);
                String date = cursor.getString(dateColIndex);
                double weight = cursor.getDouble(valueColIndex);

                addRowForEntry(id, date, weight);
            }
        } finally {
            cursor.close();
        }
    }

    /**
     * Reads the SMS and goal settings from SharedPreferences and,
     * if enabled and permission is granted, sends an SMS when the user
     * reaches or goes below their goal weight.
     */
    private void checkForGoalAndSendSmsIfNeeded(double newWeight) {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        boolean smsEnabled = prefs.getBoolean(KEY_SMS_ENABLED, false);
        boolean notifyOnGoal = prefs.getBoolean(KEY_NOTIFY_ON_GOAL, false);
        float goalWeight = prefs.getFloat(KEY_GOAL_WEIGHT, -1f);

        // If SMS is off, no goal notification, or no valid goal, exit quietly
        if (!smsEnabled || !notifyOnGoal || goalWeight <= 0f) {
            return;
        }

        // For a weight-loss app, treat the goal as "reached" when current weight is <= goal.
        if (newWeight <= goalWeight) {

            // Double-check permission before sending
            int permissionState = ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.SEND_SMS
            );

            if (permissionState != PackageManager.PERMISSION_GRANTED) {
                // No permission: do not send. App still works without SMS.
                return;
            }

            // For this project, we'll use a hard-coded test number.
            // On an emulator, we can adjust this as needed (e.g., "5554").
            String phoneNumber = prefs.getString(KEY_PHONE_NUMBER, "");

            // If no phone number is set, silently skip SMS
            if (phoneNumber.isEmpty()) {
                return;
            }

            String message = "Congratulations! You reached your goal weight of "
                    + goalWeight + " lb.";

            try {
                SmsManager smsManager = SmsManager.getDefault();
                smsManager.sendTextMessage(phoneNumber, null, message, null, null);
                Toast.makeText(this, "Goal reached! SMS alert sent.", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Toast.makeText(this, "Could not send SMS: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**
     * Dynamically builds a horizontal row containing:
     *  - the entry date,
     *  - the weight value,
     *  - a delete icon.
     * Tapping the row puts the values into the input fields for editing.
     */
    private void addRowForEntry(long id, String date, double weight) {
        // Parent row
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);   // <-- keep everything vertically centered
        row.setPadding(dpToPx(16), dpToPx(12), dpToPx(16), dpToPx(12));
        row.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        // Date TextView
        TextView dateTextView = new TextView(this);
        LinearLayout.LayoutParams dateParams = new LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                2f
        );
        dateParams.gravity = Gravity.CENTER_VERTICAL;
        dateTextView.setLayoutParams(dateParams);
        dateTextView.setText(date);
        dateTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);

        // Weight TextView
        TextView weightTextView = new TextView(this);
        LinearLayout.LayoutParams weightParams = new LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1f
        );
        weightParams.gravity = Gravity.CENTER_VERTICAL;
        weightTextView.setLayoutParams(weightParams);
        weightTextView.setText(String.valueOf(weight) + " lb");
        weightTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);

        // Delete button
        ImageButton deleteButton = new ImageButton(this);
        LinearLayout.LayoutParams deleteParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        deleteParams.gravity = Gravity.CENTER_VERTICAL;
        deleteParams.setMarginStart(dpToPx(8));
        deleteButton.setLayoutParams(deleteParams);

        // Makes the icon behave like a simple image, no extra padding
        deleteButton.setImageResource(android.R.drawable.ic_menu_delete);
        deleteButton.setBackground(null);          // remove default button background
        deleteButton.setPadding(0, 0, 0, 0);       // no extra padding
        deleteButton.setAdjustViewBounds(true);    // keep icon nicely sized
        deleteButton.setScaleType(ImageButton.ScaleType.CENTER_INSIDE);
        deleteButton.setContentDescription("Delete entry");

        deleteButton.setOnClickListener(v -> {
            weightRepository.deleteWeight(id);
            Toast.makeText(this, "Entry deleted", Toast.LENGTH_SHORT).show();
            loadEntries();
        });

        // Tap row to edit
        row.setOnClickListener(v -> {
            editTextEntryDate.setText(date);
            editTextEntryWeight.setText(String.valueOf(weight));
            editingEntryId = id;
            buttonSaveEntry.setText("Update Entry");
        });

        // Add views to row
        row.addView(dateTextView);
        row.addView(weightTextView);
        row.addView(deleteButton);

        // Add row under the header in layoutEntriesList
        layoutEntriesList.addView(row);
    }

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round((float) dp * density);
    }
}
