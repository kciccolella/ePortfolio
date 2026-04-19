package com.zybooks.ciccolella_weight_trackingapp;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import android.util.Log;  // Import Log class

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import org.mindrot.jbcrypt.BCrypt;  // Import for bcrypt hashing

/**
 * Main entry screen for the app.
 * Allows the user to log in or create a new account before viewing weight entries.
 */
public class MainActivity extends AppCompatActivity {

    private EditText editTextUsername;
    private EditText editTextPassword;

    private UserRepository userRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // DB repo
        userRepository = new UserRepository(this);

        // UI references
        editTextUsername = findViewById(R.id.editTextUsername);
        editTextPassword = findViewById(R.id.editTextPassword);

        Button buttonLogin = findViewById(R.id.buttonLogin);
        Button buttonCreateAccount = findViewById(R.id.buttonCreateAccount);

        // Handle Login
        buttonLogin.setOnClickListener(v -> handleLogin());

        // Handle Create Account
        buttonCreateAccount.setOnClickListener(v -> handleCreateAccount());
    }

    /**
     * Validates the login credentials against the users table.
     * On success, navigates to EntriesActivity for the logged-in user.
     */
    private void handleLogin() {
        String username = editTextUsername.getText().toString().trim();
        String password = editTextPassword.getText().toString().trim();

        if (username.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please enter username and password", Toast.LENGTH_SHORT).show();
            return;
        }

        // Validate login using the userRepository
        boolean valid = userRepository.validateLogin(username, password);
        if (valid) {
            Toast.makeText(this, "Login successful!", Toast.LENGTH_SHORT).show();

            Intent intent = new Intent(this, EntriesActivity.class);
            intent.putExtra("username", username); // pass user
            startActivity(intent);

        } else {
            Toast.makeText(this, "Invalid login", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Attempts to create a new user with the given username and password.
     * Shows a message if the username is already taken.
     */
    private void handleCreateAccount() {
        String username = editTextUsername.getText().toString().trim();
        String password = editTextPassword.getText().toString().trim();

        if (username.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please enter username and password", Toast.LENGTH_SHORT).show();
            return;
        }

        // Hash the password before saving
        String hashedPassword = hashPassword(password);

        // Log the hashed password
        Log.d("Password Hash", "Hashed Password: " + hashedPassword);

        boolean created = userRepository.createUser(username, hashedPassword);

        if (created) {
            Toast.makeText(this, "Account created!", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Username already exists", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Hashes the password using bcrypt before storing it.
     */
    private String hashPassword(String plainTextPassword) {
        return BCrypt.hashpw(plainTextPassword, BCrypt.gensalt());
    }
}