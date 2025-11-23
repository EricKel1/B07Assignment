package com.example.b07project;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import com.example.b07project.models.MedicationSchedule;
import com.example.b07project.models.PersonalBest;
import com.example.b07project.repository.ControllerMedicineRepository;
import com.example.b07project.repository.PEFRepository;
import com.example.b07project.repository.ScheduleRepository;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;

public class ParentChildDashboardActivity extends AppCompatActivity {

    private String childId;
    private String childName;
    private TextView tvChildNameHeader, tvCurrentZoneHeader;
    private TextView tvAdherencePercentage, tvAdherenceDetails;
    private ProgressBar progressAdherence;
    private ImageView btnConfigureSchedule;
    
    private PEFRepository pefRepository;
    private ScheduleRepository scheduleRepository;
    private ControllerMedicineRepository controllerRepository;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_parent_child_dashboard);

        db = FirebaseFirestore.getInstance();
        childId = getIntent().getStringExtra("EXTRA_CHILD_ID");
        childName = getIntent().getStringExtra("EXTRA_CHILD_NAME");
        
        pefRepository = new PEFRepository();
        scheduleRepository = new ScheduleRepository();
        controllerRepository = new ControllerMedicineRepository();

        initializeViews();
        setupListeners();
        loadChildData();
        loadSharingSettings();
        loadAdherenceData();
    }

    private void initializeViews() {
        tvChildNameHeader = findViewById(R.id.tvChildNameHeader);
        tvCurrentZoneHeader = findViewById(R.id.tvCurrentZoneHeader);
        
        tvAdherencePercentage = findViewById(R.id.tvAdherencePercentage);
        tvAdherenceDetails = findViewById(R.id.tvAdherenceDetails);
        progressAdherence = findViewById(R.id.progressAdherence);
        btnConfigureSchedule = findViewById(R.id.btnConfigureSchedule);
        
        if (childName != null) {
            tvChildNameHeader.setText(childName);
        }
    }

    private void setupListeners() {
        // Quick Actions
        setupCardListener(R.id.cardLogMedicine, LogRescueInhalerActivity.class);
        setupCardListener(R.id.cardDailyCheckIn, DailySymptomCheckInActivity.class);
        setupCardListener(R.id.cardEnterPEF, PEFEntryActivity.class);

        // Analytics
        setupCardListener(R.id.cardStats, StatisticsReportsActivity.class);
        setupCardListener(R.id.cardPatterns, TriggerPatternsActivity.class);

        // History
        setupCardListener(R.id.cardHistoryMedicine, RescueInhalerHistoryActivity.class);
        setupCardListener(R.id.cardHistoryCheckIn, SymptomHistoryActivity.class);
        setupCardListener(R.id.cardHistoryPEF, PEFHistoryActivity.class);
        setupCardListener(R.id.cardHistoryIncidents, IncidentHistoryActivity.class);
        
        // Schedule Config
        View.OnClickListener configListener = v -> {
            Intent intent = new Intent(this, ConfigureScheduleActivity.class);
            intent.putExtra("EXTRA_CHILD_ID", childId);
            startActivity(intent);
        };
        
        btnConfigureSchedule.setOnClickListener(configListener);
        findViewById(R.id.cardAdherence).setOnClickListener(configListener);
    }

    private void loadAdherenceData() {
        if (childId == null) return;

        scheduleRepository.getSchedule(childId, new ScheduleRepository.LoadCallback() {
            @Override
            public void onSuccess(MedicationSchedule schedule) {
                if (schedule == null) {
                    tvAdherenceDetails.setText("No schedule configured");
                    tvAdherencePercentage.setText("--%");
                    progressAdherence.setProgress(0);
                    return;
                }

                calculateAdherence(schedule);
            }

            @Override
            public void onFailure(String error) {
                tvAdherenceDetails.setText("Error loading schedule: " + error);
            }
        });
    }

    private void calculateAdherence(MedicationSchedule schedule) {
        // Calculate for last 7 days
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, -7);
        Date startDate = cal.getTime();

        controllerRepository.getLogsSince(childId, startDate, new ControllerMedicineRepository.LoadCallback() {
            @Override
            public void onSuccess(java.util.List<com.example.b07project.models.ControllerMedicineLog> logs) {
                int expectedDoses = schedule.getFrequency() * 7;
                int actualDoses = logs.size();
                
                int percentage = (expectedDoses > 0) ? (int) ((actualDoses / (float) expectedDoses) * 100) : 0;
                if (percentage > 100) percentage = 100; // Cap at 100%

                // Update UI
                tvAdherencePercentage.setText(percentage + "%");
                progressAdherence.setProgress(percentage);
                tvAdherenceDetails.setText(actualDoses + "/" + expectedDoses + " doses taken (Last 7 days)");
                
                // Color coding
                if (percentage >= 80) {
                    progressAdherence.getProgressDrawable().setTint(getColor(android.R.color.holo_green_dark));
                } else if (percentage >= 50) {
                    progressAdherence.getProgressDrawable().setTint(getColor(android.R.color.holo_orange_dark));
                } else {
                    progressAdherence.getProgressDrawable().setTint(getColor(android.R.color.holo_red_dark));
                }
            }

            @Override
            public void onFailure(String error) {
                tvAdherenceDetails.setText("Error calculating adherence");
            }
        });
    }

    private void setupCardListener(int cardId, Class<?> activityClass) {
        CardView card = findViewById(cardId);
        if (card != null) {
            card.setOnClickListener(v -> {
                Intent intent = new Intent(this, activityClass);
                intent.putExtra("EXTRA_CHILD_ID", childId);
                startActivity(intent);
            });
        }
    }

    private void loadChildData() {
        if (childId == null) return;

        pefRepository.getLastPEFReading(childId, new PEFRepository.LoadCallback<com.example.b07project.models.PEFReading>() {
            @Override
            public void onSuccess(com.example.b07project.models.PEFReading reading) {
                if (reading != null && reading.getZone() != null && !reading.getZone().equals("unknown")) {
                    updateZoneDisplay(reading.getZone());
                } else {
                    tvCurrentZoneHeader.setText("Current Zone: No data");
                }
            }

            @Override
            public void onFailure(String error) {
                tvCurrentZoneHeader.setText("Current Zone: Unavailable");
            }
        });
    }

    private void updateZoneDisplay(String zone) {
        String zoneLabel = PersonalBest.getZoneLabel(zone);
        tvCurrentZoneHeader.setText("Current Zone: " + zoneLabel);

        int color = PersonalBest.getZoneColor(zone);
        tvCurrentZoneHeader.setTextColor(color);
    }

    private void loadSharingSettings() {
        if (childId == null) return;

        db.collection("children").document(childId)
            .get()
            .addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    Map<String, Boolean> sharingSettings = (Map<String, Boolean>) documentSnapshot.get("sharingSettings");
                    if (sharingSettings != null) {
                        updateBadges(sharingSettings);
                    }
                }
            })
            .addOnFailureListener(e -> {
                // Handle error or just leave badges hidden
            });
    }

    private void updateBadges(Map<String, Boolean> settings) {
        // Medication
        boolean shareMedication = Boolean.TRUE.equals(settings.get("medication"));
        setViewVisibility(R.id.badgeLogMedicine, shareMedication);
        setViewVisibility(R.id.badgeHistoryMedicine, shareMedication);

        // Symptoms
        boolean shareSymptoms = Boolean.TRUE.equals(settings.get("symptoms"));
        setViewVisibility(R.id.badgeDailyCheckIn, shareSymptoms);
        setViewVisibility(R.id.badgeHistoryCheckIn, shareSymptoms);

        // PEF / Safety
        boolean sharePEF = Boolean.TRUE.equals(settings.get("pef"));
        setViewVisibility(R.id.badgeEnterPEF, sharePEF);
        setViewVisibility(R.id.badgeHistoryPEF, sharePEF);
        setViewVisibility(R.id.badgeHistoryIncidents, sharePEF);

        // Patterns
        boolean sharePatterns = Boolean.TRUE.equals(settings.get("patterns"));
        setViewVisibility(R.id.badgePatterns, sharePatterns);

        // Stats
        boolean shareStats = Boolean.TRUE.equals(settings.get("stats"));
        setViewVisibility(R.id.badgeStats, shareStats);
    }

    private void setViewVisibility(int viewId, boolean visible) {
        View view = findViewById(viewId);
        if (view != null) {
            view.setVisibility(visible ? View.VISIBLE : View.GONE);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadChildData(); // Refresh zone on return
        loadSharingSettings();
        loadAdherenceData(); // Refresh adherence
    }
}
