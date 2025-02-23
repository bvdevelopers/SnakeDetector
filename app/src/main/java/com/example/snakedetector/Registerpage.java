package com.example.snakedetector;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;

public class Registerpage extends AppCompatActivity {

    private EditText etName, etEmail, etPassword, etPhone;
    private Button btnRegister;
    private FirebaseAuth mAuth;
    private ProgressBar progressBar;
    private DatabaseReference databaseReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registerpage);

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();
        databaseReference = FirebaseDatabase.getInstance("https://snakedetector-a99e1-default-rtdb.asia-southeast1.firebasedatabase.app/").getReference("Users");

        // Initialize views
        etName = findViewById(R.id.etName);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etPhone = findViewById(R.id.phno);
        btnRegister = findViewById(R.id.btnRegister);
        progressBar = findViewById(R.id.progressBar);

        // Set click listener for the register button
        btnRegister.setOnClickListener(v -> registerUser());
    }

    private void registerUser() {
        String name = etName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();

        // Input validation
        if (TextUtils.isEmpty(name)) {
            etName.setError("Name is required");
            etName.requestFocus();
            return;
        }
        if (!isValidEmail(email)) {
            etEmail.setError("Invalid email format");
            etEmail.requestFocus();
            return;
        }
        if (!isValidPassword(password)) {
            etPassword.setError("Password must be at least 6 characters");
            etPassword.requestFocus();
            return;
        }
        if (!isValidPhone(phone)) {
            etPhone.setError("Invalid phone number (10 digits required)");
            etPhone.requestFocus();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);

        // Firebase user registration
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    progressBar.setVisibility(View.GONE);
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            sendEmailVerification(user, name, email, phone);
                        }
                    } else {
                        if (task.getException() instanceof FirebaseAuthUserCollisionException) {
                            Toast.makeText(Registerpage.this, "This email is already registered", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(Registerpage.this, "Registration failed: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }

    private void sendEmailVerification(FirebaseUser user, String name, String email, String phone) {
        user.sendEmailVerification().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Toast.makeText(Registerpage.this, "Verification email sent. Please check your inbox.", Toast.LENGTH_LONG).show();
                saveUserToDatabase(user.getUid(), name, email, phone);
                saveLoginState(email, phone);
            } else {
                Toast.makeText(Registerpage.this, "Failed to send verification email: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private boolean isValidEmail(String email) {
        return !TextUtils.isEmpty(email) && Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    private boolean isValidPassword(String password) {
        return !TextUtils.isEmpty(password) && password.length() >= 6;
    }

    private boolean isValidPhone(String phone) {
        return !TextUtils.isEmpty(phone) && phone.length() == 10 && TextUtils.isDigitsOnly(phone);
    }

    private void saveLoginState(String email, String phone) {
        SharedPreferences sharedPreferences = getSharedPreferences("UserSession", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean("isLoggedIn", false);
        editor.putString("email", email);
        editor.putString("phone", phone);
        editor.apply();
    }

    private void saveUserToDatabase(String userId, String name, String email, String phone) {
        // Create a HashMap to store user data
        HashMap<String, Object> userData = new HashMap<>();
        userData.put("userId", userId);
        userData.put("userName", name);
        userData.put("email", email);
        userData.put("phoneNumber", phone);
        userData.put("timestamp", System.currentTimeMillis()); // Store registration time

        // Store the data in Firebase Realtime Database
        databaseReference.child(userId).setValue(userData)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(Registerpage.this, "User data stored successfully", Toast.LENGTH_LONG).show();
                        // Redirect to login or another activity
                        startActivity(new Intent(Registerpage.this, Login.class));
                        finish();
                    } else {
                        Toast.makeText(Registerpage.this, "Failed to store user data: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }
}
