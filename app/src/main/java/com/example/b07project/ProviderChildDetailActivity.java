package com.example.b07project;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.LinearLayout;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import com.example.b07project.repository.ProviderRepository;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.Map;

public class ProviderChildDetailActivity extends AppCompatActivity {

    private String childId;
    private String childName;
    private FirebaseFirestore db;
    private String providerId;
    private ProviderRepository providerRepository;
    private LinearLayout containerData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_provider_child_detail);

        db = FirebaseFirestore.getInstance();
        providerId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        providerRepository = new ProviderRepository();

        // Get child info from intent
        childId = getIntent().getStringExtra("CHILD_ID");
        childName = getIntent().getStringExtra("CHILD_NAME");

        TextView tvTitle = findViewById(R.id.tvTitle);
        tvTitle.setText("Patient: " + (childName != null ? childName : "Unknown"));

        Button btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        containerData = findViewById(R.id.containerData);

        loadSharingPermissions();
    }

    private void loadSharingPermissions() {
        // Query Firestore for sharing permissions
        db.collection("providerSharing")
                .whereEqualTo("providerId", providerId)
                .whereEqualTo("childId", childId)
                .limit(1)
                .addSnapshotListener((querySnapshot, error) -> {
                    if (error != null) {
                        return;
                    }

                    if (querySnapshot != null && !querySnapshot.isEmpty()) {
                        Map<String, Object> sharingData = querySnapshot.getDocuments()
                                .get(0).getData();
                        setupDataDisplay(sharingData);
                    }
                });
    }

    private void setupDataDisplay(Map<String, Object> sharingData) {
        containerData.removeAllViews();

        // Rescue Logs
        if (isShared(sharingData, "rescueLogs")) {
            addSection(containerData, "Rescue Inhaler Logs", 
                    "View rescue medicine usage logs", "#E3F2FD", "#1565C0");
        }

        // Controller Adherence
        if (isShared(sharingData, "controllerAdherence")) {
            addSection(containerData, "Controller Adherence", 
                    "View daily controller medicine compliance", "#FFF3E0", "#E65100");
        }

        // Symptoms
        if (isShared(sharingData, "symptoms")) {
            addSection(containerData, "Symptoms Log", 
                    "View daily symptom check-ins", "#F3E5F5", "#6A1B9A");
        }

        // Triggers
        if (isShared(sharingData, "triggers")) {
            addSection(containerData, "Trigger Patterns", 
                    "View identified symptom triggers", "#E8F5E9", "#2E7D32");
        }

        // PEF (Peak Flow)
        if (isShared(sharingData, "peakFlow")) {
            addSection(containerData, "Peak Flow (PEF)", 
                    "View peak flow readings and zone status", "#FFEBEE", "#C62828");
        }

        // Triage Incidents
        if (isShared(sharingData, "triageIncidents")) {
            addSection(containerData, "Triage Incidents", 
                    "View emergency incidents and responses", "#FCE4EC", "#880E4F");
        }

        // Summary Charts
        if (isShared(sharingData, "summaryCharts")) {
            addSection(containerData, "Summary Reports", 
                    "View charts and statistics", "#FFF9C4", "#F57F17");
        }
    }

    private boolean isShared(Map<String, Object> sharingData, String permission) {
        return sharingData != null && 
               Boolean.TRUE.equals(sharingData.getOrDefault(permission, false));
    }

    private void addSection(LinearLayout container, String title, String description, 
                           String bgColor, String titleColor) {
        CardView card = new CardView(this);
        
        // Parse color strings
        int bgColorInt = android.graphics.Color.parseColor(bgColor);
        int titleColorInt = android.graphics.Color.parseColor(titleColor);
        
        card.setCardBackgroundColor(bgColorInt);
        card.setRadius(12);
        card.setCardElevation(4);
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        cardParams.setMargins(0, 12, 0, 0);
        card.setLayoutParams(cardParams);

        LinearLayout cardContent = new LinearLayout(this);
        cardContent.setOrientation(LinearLayout.VERTICAL);
        cardContent.setPadding(20, 20, 20, 20);

        TextView tvTitle = new TextView(this);
        tvTitle.setText(title);
        tvTitle.setTextSize(18);
        tvTitle.setTextColor(titleColorInt);
        tvTitle.setTypeface(null, android.graphics.Typeface.BOLD);
        cardContent.addView(tvTitle);

        TextView tvDesc = new TextView(this);
        tvDesc.setText(description);
        tvDesc.setTextSize(14);
        tvDesc.setTextColor(0xFF424242);
        LinearLayout.LayoutParams descParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        descParams.topMargin = 8;
        tvDesc.setLayoutParams(descParams);
        cardContent.addView(tvDesc);

        // Add "Shared with Provider" badge
        TextView tvBadge = new TextView(this);
        tvBadge.setText("✓ Shared with Provider");
        tvBadge.setTextSize(12);
        tvBadge.setTextColor(0xFF2E7D32);
        LinearLayout.LayoutParams badgeParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        badgeParams.topMargin = 12;
        tvBadge.setLayoutParams(badgeParams);
        cardContent.addView(tvBadge);

        // Add tap to view text
        TextView tvTap = new TextView(this);
        tvTap.setText("Tap to view →");
        tvTap.setTextSize(12);
        tvTap.setTextColor(titleColorInt);
        LinearLayout.LayoutParams tapParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        tapParams.topMargin = 12;
        tvTap.setLayoutParams(tapParams);
        cardContent.addView(tvTap);

        card.addView(cardContent);
        
        // Set click listener based on section title
        card.setOnClickListener(v -> navigateToDetail(title));
        
        container.addView(card);
    }

    private void navigateToDetail(String sectionTitle) {
        Intent intent = null;
        
        switch (sectionTitle) {
            case "Rescue Inhaler Logs":
                intent = new Intent(this, ProviderRescueLogsActivity.class);
                break;
            case "Controller Adherence":
                // intent = new Intent(this, ProviderControllerActivity.class);
                break;
            case "Symptoms Log":
                intent = new Intent(this, ProviderSymptomsActivity.class);
                break;
            case "Trigger Patterns":
                // intent = new Intent(this, ProviderTriggersActivity.class);
                break;
            case "Peak Flow (PEF)":
                intent = new Intent(this, ProviderPEFActivity.class);
                break;
            case "Triage Incidents":
                intent = new Intent(this, ProviderTriageActivity.class);
                break;
            case "Summary Reports":
                // intent = new Intent(this, ProviderReportsActivity.class);
                break;
        }
        
        if (intent != null) {
            intent.putExtra("CHILD_ID", childId);
            intent.putExtra("CHILD_NAME", childName);
            startActivity(intent);
        }
    }
}
