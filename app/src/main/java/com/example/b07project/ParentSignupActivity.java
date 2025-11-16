package com.example.b07project;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

public class ParentSignupActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_parent_signup);

        findViewById(R.id.btnBack).setOnClickListener(v -> {
            finish(); // Go back to the previous activity
        });
    }
}
