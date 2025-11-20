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

import android.widget.ImageButton;
import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import androidx.core.app.ActivityCompat;

public class HomeActivity extends AppCompatActivity {

    private Button btnLogRescueInhaler, btnViewHistory, btnDailyCheckIn, btnViewSymptomHistory, btnViewPatterns, btnSignOut;
    private Button btnEmergencyTriage, btnEnterPEF, btnViewIncidents, btnInhalerTechnique, btnMotivation, btnStatisticsReports;
    private Button btnInventory;
    private ImageButton btnNotifications;
    private TextView tvCurrentZone, tvZonePercentage, tvViewingChildNotice;
    private PEFRepository pefRepository;
    private String dataOwnerId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        pefRepository = new PEFRepository();
        initializeViews();
        setupListeners();

        String childId = getIntent().getStringExtra("EXTRA_CHILD_ID");
        String childName = getIntent().getStringExtra("EXTRA_CHILD_NAME");

        if (childId != null) {
            // Child Mode
            dataOwnerId = childId;
            tvViewingChildNotice.setText("Viewing child: " + (childName != null ? childName : "Child"));
            tvViewingChildNotice.setVisibility(View.VISIBLE);
        } else {
            // Parent Mode (or default)
            dataOwnerId = FirebaseAuth.getInstance().getCurrentUser().getUid();
            tvViewingChildNotice.setVisibility(View.GONE);
        }

        loadZoneStatus();
        checkNotificationPermission();
    }

    private void checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, 101);
            }
        }
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
        btnInhalerTechnique = findViewById(R.id.btnInhalerTechnique);
        btnMotivation = findViewById(R.id.btnMotivation);
        btnStatisticsReports = findViewById(R.id.btnStatisticsReports);
        btnInventory = findViewById(R.id.btnInventory);
        btnSignOut = findViewById(R.id.btnSignOut);
        btnNotifications = findViewById(R.id.btnNotifications);

        tvCurrentZone = findViewById(R.id.tvCurrentZone);
        tvZonePercentage = findViewById(R.id.tvZonePercentage);
        tvViewingChildNotice = findViewById(R.id.tvViewingChildNotice);
    }

    private void setupListeners() {
        btnLogRescueInhaler.setOnClickListener(v -> {
            startActivity(new Intent(this, LogRescueInhalerActivity.class));
        });

        if (btnInventory != null) {
            btnInventory.setOnClickListener(v -> {
                startActivity(new Intent(this, InventoryActivity.class));
            });
        }

        if (btnNotifications != null) {
            btnNotifications.setOnClickListener(v -> {
                startActivity(new Intent(this, NotificationCenterActivity.class));
            });
        }

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

        btnInhalerTechnique.setOnClickListener(v -> {
            startActivity(new Intent(this, InhalerTechniqueActivity.class));
        });

        btnMotivation.setOnClickListener(v -> {
            startActivity(new Intent(this, MotivationActivity.class));
        });

        btnStatisticsReports.setOnClickListener(v -> {
            startActivity(new Intent(this, StatisticsReportsActivity.class));
        });

        btnSignOut.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });
    }

    private void loadZoneStatus() {
        if (dataOwnerId == null) return; // Don't load if no user/child is set

        pefRepository.getLastPEFReading(dataOwnerId, new PEFRepository.LoadCallback<com.example.b07project.models.PEFReading>() {
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
