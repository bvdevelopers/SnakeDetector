package com.example.snakedetector;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.Map;
import java.util.Set;

public class Login extends AppCompatActivity {

    private EditText etEmail, etPassword;
    private Button btnLogin, btnRegister;
    private FirebaseAuth mAuth;



    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);


        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // Bind UI elements
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        btnRegister = findViewById(R.id.btnRegister);

        // Register Button Listener
        btnRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                String email = etEmail.getText().toString().trim();
//                String password = etPassword.getText().toString().trim();
                Intent intent = new Intent(Login.this, Registerpage.class);
                startActivity(intent);

//                if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
//                    Toast.makeText(Login.this, "Please fill all fields", Toast.LENGTH_SHORT).show();
//                    return;
//                }
//
//                mAuth.createUserWithEmailAndPassword(email, password)
//                        .addOnCompleteListener(task -> {
//                            if (task.isSuccessful()) {
//                                Toast.makeText(Login.this, "Registration Successful!", Toast.LENGTH_SHORT).show();
//                            } else {
//                                Toast.makeText(Login.this, "Registration Failed: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
//                            }
//                        });
            }
        });

        // Login Button Listener
        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = etEmail.getText().toString().trim();
                String password = etPassword.getText().toString().trim();

                if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
                    Toast.makeText(Login.this, "Please fill all fields", Toast.LENGTH_SHORT).show();
                    return;
                }

                mAuth.signInWithEmailAndPassword(email, password)
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                FirebaseUser user = mAuth.getCurrentUser();
                                if(user != null)
                                {
                                 saveLoginState(email);
                                }
                                Toast.makeText(Login.this, "Login Successful! Welcome, " + user.getEmail(), Toast.LENGTH_SHORT).show();
                                Intent intent = new Intent(Login.this, User.class);
                                startActivity(intent);
                                finish();
                            } else {
                                Toast.makeText(Login.this, "Login Failed: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        });
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
}
