package com.example.snakedetector;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import android.content.Intent;
import android.os.Handler;
import android.widget.TextView;
import android.widget.ImageView;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;

public class MainActivity extends AppCompatActivity {
    ImageView imageView;
    TextView textView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        imageView = (ImageView)findViewById(R.id.imageView);
        textView = (TextView)findViewById(R.id.textview1);

        imageView.animate().alpha(0f).setDuration(0);
        textView.animate().alpha(0f).setDuration(0);

        imageView.animate().alpha(1f).setDuration(1000).setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                textView.animate().alpha(1f).setDuration(800);
            }
        });

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
//                Intent intent = new Intent(MainActivity.this, Login.class);
//                startActivity(intent);
//                finish();
                checkLoginState();
            }
        },3000);


    }
    private void checkLoginState() {
        SharedPreferences sharedPreferences = getSharedPreferences("UserSession", MODE_PRIVATE);
        boolean isLoggedIn = sharedPreferences.getBoolean("isLoggedIn", false);

        if (isLoggedIn) {
            // User is logged in, navigate to the main screen
            String userId = sharedPreferences.getString("userId", null);
            navigateToMainScreen(userId);
        } else {
            // User is not logged in, show login/register screen
            navigateToLoginScreen();
        }
    }

    private void navigateToMainScreen(String userId) {
        Intent intent = new Intent(this, User.class);
        intent.putExtra("userId", userId);
        startActivity(intent);
        finish();
    }

    private void navigateToLoginScreen() {
        Intent intent = new Intent(this, Login.class);
        startActivity(intent);
        finish();
    }

    protected void onDestroy() {
        super.onDestroy();
        System.gc();
    }
}