package com.zybooks.ciccolella_weight_trackingapp;

import android.Manifest;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

/**
 * Screen for configuring SMS alerts and goal weight.
 * Prompts the user for SMS permission when needed.
 */
public class NotificationsActivity extends AppCompatActivity {

    private static final int REQUEST_SEND_SMS = 1001;

    private static final String PREFS_NAME = "WeightTrackerPrefs";
    private static final String KEY_SMS_ENABLED = "sms_enabled";
    private static final String KEY_NOTIFY_ON_GOAL = "notify_on_goal";
    private static final String KEY_GOAL_WEIGHT = "goal_weight";
    private static final String KEY_PHONE_NUMBER = "phone_number";

    private Switch switchEnableSms;
    private EditText editTextGoalWeight;
    private CheckBox checkBoxNotifyOnGoal;
    private TextView textViewSmsStatus;

    private SharedPreferences prefs;
    private EditText editTextPhoneNumber;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notifications);

        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        editTextPhoneNumber = findViewById(R.id.editTextPhoneNumber);
        switchEnableSms = findViewById(R.id.switchEnableSms);
        editTextGoalWeight = findViewById(R.id.editTextGoalWeight);
        checkBoxNotifyOnGoal = findViewById(R.id.checkBoxNotifyOnGoal);
        textViewSmsStatus = findViewById(R.id.textViewSmsStatus);
        Button buttonSaveNotifications = findViewById(R.id.buttonSaveNotifications);

        // Load saved settings
        boolean smsEnabled = prefs.getBoolean(KEY_SMS_ENABLED, false);
        boolean notifyOnGoal = prefs.getBoolean(KEY_NOTIFY_ON_GOAL, false);
        float savedGoal = prefs.getFloat(KEY_GOAL_WEIGHT, -1f);

        switchEnableSms.setChecked(smsEnabled);
        checkBoxNotifyOnGoal.setChecked(notifyOnGoal);
        if (savedGoal > 0f) {
            editTextGoalWeight.setText(String.valueOf(savedGoal));
        }

        String savedPhone = prefs.getString(KEY_PHONE_NUMBER, "");
        editTextPhoneNumber.setText(savedPhone);

        updateStatusText();

        // When the switch is turned OFF, immediately save and update status
        switchEnableSms.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (!isChecked) {
                prefs.edit().putBoolean(KEY_SMS_ENABLED, false).apply();
                updateStatusText();
            }
        });

        buttonSaveNotifications.setOnClickListener(v -> saveSettingsAndMaybeRequestPermission());
    }

    /**
     * Validates and saves the notification settings, then requests SMS permission
     * if needed. Ensures the app still works even if the user denies permission.
     */
    private void saveSettingsAndMaybeRequestPermission() {
        String phoneNumber = editTextPhoneNumber.getText().toString().trim();
        boolean smsEnabled = switchEnableSms.isChecked();
        boolean notifyOnGoal = checkBoxNotifyOnGoal.isChecked();

        float goalWeight = -1f;
        String goalText = editTextGoalWeight.getText().toString().trim();

        // If alerts depend on the goal, require a goal value
        if (smsEnabled && notifyOnGoal) {
            if (goalText.isEmpty()) {
                Toast.makeText(this, "Please enter a goal weight", Toast.LENGTH_SHORT).show();
                return;
            }
            try {
                goalWeight = Float.parseFloat(goalText);
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Goal weight must be a number", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        // Save settings
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(KEY_SMS_ENABLED, smsEnabled);
        editor.putBoolean(KEY_NOTIFY_ON_GOAL, notifyOnGoal);

        if (goalWeight > 0f) {
            editor.putFloat(KEY_GOAL_WEIGHT, goalWeight);
        }

        editor.putString(KEY_PHONE_NUMBER, phoneNumber);
        editor.apply();


        if (smsEnabled && notifyOnGoal) {
            // Check or request permission
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS)
                    != PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(
                        this,
                        new String[]{Manifest.permission.SEND_SMS},
                        REQUEST_SEND_SMS
                );
            } else {
                Toast.makeText(this, "SMS alerts enabled", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "Notification settings saved", Toast.LENGTH_SHORT).show();
        }

        updateStatusText();
    }

    private void updateStatusText() {
        boolean smsEnabled = prefs.getBoolean(KEY_SMS_ENABLED, false);
        boolean notifyOnGoal = prefs.getBoolean(KEY_NOTIFY_ON_GOAL, false);
        int permissionState = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.SEND_SMS
        );

        if (!smsEnabled) {
            textViewSmsStatus.setText("SMS alerts are currently turned off.");
        } else if (permissionState != PackageManager.PERMISSION_GRANTED) {
            textViewSmsStatus.setText("SMS permission not granted. The app cannot send text alerts.");
        } else if (notifyOnGoal) {
            textViewSmsStatus.setText("SMS alerts are ON for reaching your goal weight.");
        } else {
            textViewSmsStatus.setText("SMS permission granted, but no alert condition is selected.");
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_SEND_SMS) {
            if (grantResults.length > 0 &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // User granted SMS permission: keep the SMS setting enabled.
                Toast.makeText(this, "SMS permission granted", Toast.LENGTH_SHORT).show();
            } else {
                // User denied SMS permission: automatically turn off SMS alerts in settings.
                Toast.makeText(this,
                        "SMS permission denied. Alerts will be disabled.",
                        Toast.LENGTH_SHORT).show();

                prefs.edit().putBoolean(KEY_SMS_ENABLED, false).apply();
                switchEnableSms.setChecked(false);
            }
            updateStatusText();
        }
    }
}
