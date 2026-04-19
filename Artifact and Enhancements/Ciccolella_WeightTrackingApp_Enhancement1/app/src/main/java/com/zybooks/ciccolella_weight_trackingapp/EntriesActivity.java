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

import java.text.SimpleDateFormat;
import java.text.ParseException;

import androidx.appcompat.app.AppCompatActivity;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

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
    private Long editingEntryId = null;

    private static final String PREFS_NAME = "WeightTrackerPrefs";
    private static final String KEY_SMS_ENABLED = "sms_enabled";
    private static final String KEY_NOTIFY_ON_GOAL = "notify_on_goal";
    private static final String KEY_GOAL_WEIGHT = "goal_weight";
    private static final String KEY_PHONE_NUMBER = "phone_number";

    // For graphing
    private GraphView graph;
    private Button toggleGraphButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_entries);

        // Get the username passed from MainActivity (login)
        username = getIntent().getStringExtra("username");
        if (username == null || username.trim().isEmpty()) {
            username = "localuser";  // Default user for fallback
        }

        weightRepository = new WeightRepository(this);

        // UI references
        layoutEntriesList = findViewById(R.id.layoutEntriesList);
        editTextEntryDate = findViewById(R.id.editTextEntryDate);
        editTextEntryWeight = findViewById(R.id.editTextEntryWeight);
        buttonSaveEntry = findViewById(R.id.buttonSaveEntry);
        graph = findViewById(R.id.graph); // GraphView
        toggleGraphButton = findViewById(R.id.buttonToggleGraph);

        // Initially hide the graph
        graph.setVisibility(View.GONE);
        toggleGraphButton.setText("Show Graph");

        // Notifications button
        Button buttonNotifications = findViewById(R.id.buttonNotifications);
        buttonNotifications.setOnClickListener(v -> {
            Intent intent = new Intent(EntriesActivity.this, NotificationsActivity.class);
            intent.putExtra("username", username);
            startActivity(intent);
        });

        // Save entry button
        buttonSaveEntry.setOnClickListener(v -> handleSaveEntry());

        // Toggle graph visibility
        toggleGraphButton.setOnClickListener(v -> toggleGraphVisibility());

        // Load existing entries and set up the graph
        loadEntries();
    }

    /**
     * Handles the Save button: validates input, then inserts or updates a weight entry.
     */
    private void handleSaveEntry() {
        String date = editTextEntryDate.getText().toString().trim();
        String weightText = editTextEntryWeight.getText().toString().trim();

        // Validate the inputs (date and weight)
        if (!validateInputs(date, weightText)) {
            return;  // If validation fails, stop the save process
        }

        // If inputs are valid, proceed with saving or updating the entry
        double weightValue = Double.parseDouble(weightText);

        // Check if we are editing an existing entry
        if (editingEntryId != null) {
            weightRepository.updateWeightById(editingEntryId, username, date, weightValue);
            Toast.makeText(this, "Entry updated", Toast.LENGTH_SHORT).show();
            editingEntryId = null;  // Reset editing entry ID after update
            buttonSaveEntry.setText("Save Entry");  // Reset button text
        } else {
            weightRepository.addOrUpdateWeight(username, date, weightValue);
            Toast.makeText(this, "Entry saved", Toast.LENGTH_SHORT).show();
        }

        checkForGoalAndSendSmsIfNeeded(weightValue);
        editTextEntryDate.setText("");
        editTextEntryWeight.setText("");
        loadEntries();  // Reload entries and refresh the graph after adding/updating an entry
    }

    private boolean validateInputs(String date, String weightText) {
        // Validate the date format using SimpleDateFormat
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        dateFormat.setLenient(false);  // Disable lenient parsing to strictly validate the date

        try {
            dateFormat.parse(date);  // Try to parse the date
        } catch (ParseException e) {
            editTextEntryDate.setError("Invalid date format. Please use YYYY-MM-DD.");
            return false;
        }

        // Validate the weight (must be a positive number)
        double weightValue;
        try {
            weightValue = Double.parseDouble(weightText);
            if (weightValue <= 0) {
                editTextEntryWeight.setError("Weight must be a positive number.");
                return false;
            }
        } catch (NumberFormatException e) {
            editTextEntryWeight.setError("Weight must be a valid number.");
            return false;
        }

        return true;  // All validations passed
    }

    /**
     * Loads all entries for the current user and repopulates the grid and graph.
     */
    private void loadEntries() {
        int childCount = layoutEntriesList.getChildCount();
        if (childCount > 1) {
            layoutEntriesList.removeViews(1, childCount - 1);
        }

        // Clear the previous series from the graph
        graph.removeAllSeries();

        Cursor cursor = weightRepository.getWeightsForUser(username);
        try {
            int idColIndex = cursor.getColumnIndexOrThrow(WeightTrackerDbHelper.COLUMN_WEIGHT_ID);
            int dateColIndex = cursor.getColumnIndexOrThrow(WeightTrackerDbHelper.COLUMN_WEIGHT_DATE);
            int valueColIndex = cursor.getColumnIndexOrThrow(WeightTrackerDbHelper.COLUMN_WEIGHT_VALUE);

            // Prepare graph data series
            LineGraphSeries<DataPoint> series = new LineGraphSeries<>();
            int xValue = 0;

            while (cursor.moveToNext()) {
                long id = cursor.getLong(idColIndex);
                String date = cursor.getString(dateColIndex);
                double weight = cursor.getDouble(valueColIndex);

                addRowForEntry(id, date, weight);

                // Add the data point to the graph
                series.appendData(new DataPoint(xValue++, weight), true, 5);  // Limit to 5 data points
            }

            // Add series to graph
            graph.addSeries(series);
            graph.setTitle("Weight Progress");
            graph.getGridLabelRenderer().setVerticalLabelsVisible(true);
            graph.getGridLabelRenderer().setHorizontalLabelsVisible(false);  // Hide x-axis labels

            // Set the x-axis range to fit the number of entries (dynamically)
            graph.getViewport().setMinX(0);
            graph.getViewport().setMaxX(cursor.getCount());  // Dynamically set the x-axis max value based on entries count

        } finally {
            cursor.close();
        }
    }

    /**
     * Dynamically builds a horizontal row for each weight entry.
     */
    private void addRowForEntry(long id, String date, double weight) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dpToPx(16), dpToPx(12), dpToPx(16), dpToPx(12));
        row.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        // Date column
        TextView dateTextView = new TextView(this);
        dateTextView.setText(date);
        dateTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        LinearLayout.LayoutParams dateParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 2);
        dateTextView.setLayoutParams(dateParams);

        // Weight column
        TextView weightTextView = new TextView(this);
        weightTextView.setText(String.valueOf(weight) + " lb");
        weightTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        LinearLayout.LayoutParams weightParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 2);
        weightTextView.setLayoutParams(weightParams);

        // Delete button column
        ImageButton deleteButton = new ImageButton(this);
        deleteButton.setImageResource(android.R.drawable.ic_menu_delete);
        deleteButton.setBackground(null);
        deleteButton.setOnClickListener(v -> {
            weightRepository.deleteWeight(id);
            Toast.makeText(this, "Entry deleted", Toast.LENGTH_SHORT).show();
            loadEntries(); // Reload entries after deletion
        });
        LinearLayout.LayoutParams deleteParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
        deleteButton.setLayoutParams(deleteParams);

        // Make the row clickable for editing
        row.setOnClickListener(v -> {
            // Set the fields to the values of the selected entry
            editingEntryId = id;
            editTextEntryDate.setText(date);
            editTextEntryWeight.setText(String.valueOf(weight));
            buttonSaveEntry.setText("Update Entry");  // Change button text to "Update Entry"
        });

        // Add the views to the row
        row.addView(dateTextView);
        row.addView(weightTextView);
        row.addView(deleteButton);

        // Add the row to the layout
        layoutEntriesList.addView(row);
    }

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round((float) dp * density);
    }

    private void checkForGoalAndSendSmsIfNeeded(double newWeight) {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        boolean smsEnabled = prefs.getBoolean(KEY_SMS_ENABLED, false);
        boolean notifyOnGoal = prefs.getBoolean(KEY_NOTIFY_ON_GOAL, false);
        float goalWeight = prefs.getFloat(KEY_GOAL_WEIGHT, -1f);

        if (!smsEnabled || !notifyOnGoal || goalWeight <= 0f) {
            return;
        }

        if (newWeight <= goalWeight) {
            int permissionState = ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS);
            if (permissionState != PackageManager.PERMISSION_GRANTED) {
                return;
            }

            String phoneNumber = prefs.getString(KEY_PHONE_NUMBER, "");
            if (phoneNumber.isEmpty()) {
                return;
            }

            String message = "Congratulations! You reached your goal weight of " + goalWeight + " lb.";
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
     * Toggle the visibility of the graph and change the button text accordingly.
     */
    private void toggleGraphVisibility() {
        if (graph.getVisibility() == View.GONE) {
            graph.setVisibility(View.VISIBLE); // Show the graph
            toggleGraphButton.setText("Hide Graph"); // Change button text
        } else {
            graph.setVisibility(View.GONE); // Hide the graph
            toggleGraphButton.setText("Show Graph"); // Change button text
        }
    }
}