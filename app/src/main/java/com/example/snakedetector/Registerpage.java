package com.example.snakedetector;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.snakedetector.model.UserEntity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseUser;

import android.widget.ProgressBar;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;


public class Registerpage extends AppCompatActivity {

    private EditText etName, etEmail, etPassword;
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
        btnRegister = findViewById(R.id.btnRegister);
        progressBar = findViewById(R.id.progressBar);


        // Set click listener for the register button
        btnRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                registerUser();
            }
        });
    }

    private void registerUser() {
        String name = etName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        // Input validation
        if (TextUtils.isEmpty(name)) {
            etName.setError("Name is required");
            return;
        }
        if (TextUtils.isEmpty(email)) {
            etEmail.setError("Email is required");
            return;
        }
        if (TextUtils.isEmpty(password)) {
            etPassword.setError("Password is required");
            return;
        }
        // Firebase user registration
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    progressBar.setVisibility(View.GONE);
                    if (task.isSuccessful()) {
                        // Registration successful
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            saveUserToDatabase(user.getUid(), name, email);
                            saveLoginState(email);
                            Intent intent = new Intent(Registerpage.this, User.class);
                            startActivity(intent);
                            finish();
                        }
                        Toast.makeText(Registerpage.this,
                                "User registered successfully: " + user.getEmail(),
                                Toast.LENGTH_LONG).show();
                    } else {
                        // Handle errors
                        if (task.getException() instanceof FirebaseAuthUserCollisionException) {
                            Toast.makeText(Registerpage.this,
                                    "This email is already registered",
                                    Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(Registerpage.this,
                                    "Registration failed: " + task.getException().getMessage(),
                                    Toast.LENGTH_LONG).show();
                        }
                    }
                });


    }
    private void saveLoginState(String userId) {
        SharedPreferences sharedPreferences = getSharedPreferences("UserSession", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean("isLoggedIn", true);  // Login status
        editor.putString("userId", userId);    // Save user ID
        editor.apply();  // Commit changes
    }
    private void saveUserToDatabase(String userId, String name, String email) {
        // Create a user object
        UserEntity user = new UserEntity();
        user.setUserName(name);
        user.setEmail(email);

        // Save the user data in Realtime Database under "Users/{userId}"
        databaseReference.child(userId).setValue(user)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(Registerpage.this,
                                "User data saved successfully",
                                Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(Registerpage.this,
                                "Failed to save user data: " + task.getException().getMessage(),
                                Toast.LENGTH_LONG).show();
                    }
                });
    }
}
