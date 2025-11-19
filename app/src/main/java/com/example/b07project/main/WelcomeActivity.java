package com.example.b07project.main;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

import com.example.b07project.HomeActivity;
import com.example.b07project.LoginActivity;
import com.example.b07project.ParentSignupActivity;
import com.example.b07project.ProviderSignupActivity;
import com.example.b07project.R;
import com.google.firebase.auth.FirebaseAuth;

public class WelcomeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Check if user is signed in (non-null) and update UI accordingly.
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            // User is signed in, so navigate to the HomeActivity
            startActivity(new Intent(WelcomeActivity.this, HomeActivity.class));
            finish(); // Prevent user from returning to this activity
            return;   // Skip the rest of the method
        }

        // If no user is signed in, show the welcome layout
        setContentView(R.layout.activity_welcome);

        // Set up button listeners
        findViewById(R.id.btnJoinAsParent).setOnClickListener(v -> {
            startActivity(new Intent(WelcomeActivity.this, ParentSignupActivity.class));
        });

        findViewById(R.id.btnJoinAsProvider).setOnClickListener(v -> {
            startActivity(new Intent(WelcomeActivity.this, ProviderSignupActivity.class));
        });

        findViewById(R.id.tvLogin).setOnClickListener(v -> {
            startActivity(new Intent(WelcomeActivity.this, LoginActivity.class));
        });
    }
}
