package com.example.b07project;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;

public class HomeActivity extends AppCompatActivity {
    
    private Button btnLogRescueInhaler, btnViewHistory, btnDailyCheckIn, btnViewSymptomHistory, btnSignOut;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        
        initializeViews();
        setupListeners();
    }
    
    private void initializeViews() {
        btnLogRescueInhaler = findViewById(R.id.btnLogRescueInhaler);
        btnViewHistory = findViewById(R.id.btnViewHistory);
        btnDailyCheckIn = findViewById(R.id.btnDailyCheckIn);
        btnViewSymptomHistory = findViewById(R.id.btnViewSymptomHistory);
        btnSignOut = findViewById(R.id.btnSignOut);
    }
    
    private void setupListeners() {
        btnLogRescueInhaler.setOnClickListener(v -> {
            startActivity(new Intent(this, LogRescueInhalerActivity.class));
        });
        
        btnViewHistory.setOnClickListener(v -> {
            startActivity(new Intent(this, RescueInhalerHistoryActivity.class));
        });
        
        btnDailyCheckIn.setOnClickListener(v -> {
            startActivity(new Intent(this, DailySymptomCheckInActivity.class));
        });
        
        btnViewSymptomHistory.setOnClickListener(v -> {
            startActivity(new Intent(this, SymptomHistoryActivity.class));
        });
        
        btnSignOut.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });
    }
}
