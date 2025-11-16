package com.example.b07project;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

public class ProviderSignupActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_provider_signup);

        findViewById(R.id.btnBack).setOnClickListener(v -> {
            finish(); // Go back to the previous activity
        });
    }
}
