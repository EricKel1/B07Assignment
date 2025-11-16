package com.example.b07project;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import com.example.b07project.models.PersonalBest;
import com.example.b07project.repository.PEFRepository;
import com.google.firebase.auth.FirebaseAuth;

public class HomeActivity extends AppCompatActivity {
    
    private Button btnLogRescueInhaler, btnViewHistory, btnDailyCheckIn, btnViewSymptomHistory, btnViewPatterns, btnSignOut;
    private Button btnEmergencyTriage, btnEnterPEF, btnViewIncidents;
    private TextView tvCurrentZone, tvZonePercentage;
    private PEFRepository pefRepository;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        
        pefRepository = new PEFRepository();
        initializeViews();
        setupListeners();
        loadZoneStatus();
    }
    
    private void initializeViews() {
        btnLogRescueInhaler = findViewById(R.id.btnLogRescueInhaler);
        btnViewHistory = findViewById(R.id.btnViewHistory);
        btnDailyCheckIn = findViewById(R.id.btnDailyCheckIn);
        btnViewSymptomHistory = findViewById(R.id.btnViewSymptomHistory);
        btnViewPatterns = findViewById(R.id.btnViewPatterns);
        btnEmergencyTriage = findViewById(R.id.btnEmergencyTriage);
        btnEnterPEF = findViewById(R.id.btnEnterPEF);
        btnViewIncidents = findViewById(R.id.btnViewIncidents);
        btnSignOut = findViewById(R.id.btnSignOut);
        
        tvCurrentZone = findViewById(R.id.tvCurrentZone);
        tvZonePercentage = findViewById(R.id.tvZonePercentage);
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
        
        btnViewPatterns.setOnClickListener(v -> {
            startActivity(new Intent(this, TriggerPatternsActivity.class));
        });
        
        btnEmergencyTriage.setOnClickListener(v -> {
            startActivity(new Intent(this, TriageActivity.class));
        });
        
        btnEnterPEF.setOnClickListener(v -> {
            startActivity(new Intent(this, PEFEntryActivity.class));
        });
        
        btnViewIncidents.setOnClickListener(v -> {
            startActivity(new Intent(this, IncidentHistoryActivity.class));
        });
        
        btnSignOut.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });
    }
    
    private void loadZoneStatus() {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        
        pefRepository.getLastPEFReading(userId, new PEFRepository.LoadCallback<com.example.b07project.models.PEFReading>() {
            @Override
            public void onSuccess(com.example.b07project.models.PEFReading reading) {
                if (reading != null && reading.getZone() != null && !reading.getZone().equals("unknown")) {
                    updateZoneDisplay(reading.getZone(), reading.getPercentageOfPB());
                } else {
                    tvCurrentZone.setText("No data yet");
                    tvZonePercentage.setText("");
                }
            }
            
            @Override
            public void onFailure(String error) {
                tvCurrentZone.setText("No data yet");
                tvZonePercentage.setText("");
            }
        });
    }
    
    private void updateZoneDisplay(String zone, int percentage) {
        String zoneLabel = PersonalBest.getZoneLabel(zone);
        tvCurrentZone.setText(zoneLabel);
        tvZonePercentage.setText("(" + percentage + "% of PB)");
        
        int color = PersonalBest.getZoneColor(zone);
        tvCurrentZone.setTextColor(ContextCompat.getColor(this, color));
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        loadZoneStatus(); // Refresh zone when returning to home
    }
}
