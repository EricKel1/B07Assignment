package com.example.b07project;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.LinearLayout;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import com.example.b07project.repository.ProviderRepository;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
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
    private TextView tvEmptyState;
    private MaterialButton btnViewReport;

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
        tvEmptyState = findViewById(R.id.tvEmptyState);
        
        btnViewReport = findViewById(R.id.btnViewReport);
        btnViewReport.setOnClickListener(v -> openProviderReport());

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
        
        boolean hasAnySharedData = false;

        // Rescue Logs
        if (isShared(sharingData, "rescueLogs")) {
            addSection(containerData, "Rescue Inhaler Logs", 
                    "View rescue medicine usage logs", "#E3F2FD", "#1565C0",
                    ProviderRescueLogsActivity.class);
            hasAnySharedData = true;
        }

        // Controller Adherence
        if (isShared(sharingData, "controllerAdherence")) {
            addSection(containerData, "Controller Adherence", 
                    "View daily controller medicine compliance", "#FFF3E0", "#E65100",
                    null); // TODO: Create ProviderControllerActivity
            hasAnySharedData = true;
        }

        // Symptoms
        if (isShared(sharingData, "symptoms")) {
            addSection(containerData, "Symptoms Log", 
                    "View daily symptom check-ins", "#F3E5F5", "#6A1B9A",
                    ProviderSymptomsActivity.class);
            hasAnySharedData = true;
        }

        // Triggers
        if (isShared(sharingData, "triggers")) {
            addSection(containerData, "Trigger Patterns", 
                    "View identified symptom triggers", "#E8F5E9", "#2E7D32",
                    null); // TODO: Create ProviderTriggersActivity
            hasAnySharedData = true;
        }

        // PEF (Peak Flow)
        if (isShared(sharingData, "peakFlow")) {
            addSection(containerData, "Peak Flow (PEF)", 
                    "View peak flow readings and zone status", "#FFEBEE", "#C62828",
                    ProviderPEFActivity.class);
            hasAnySharedData = true;
        }

        // Triage Incidents
        if (isShared(sharingData, "triageIncidents")) {
            addSection(containerData, "Triage Incidents", 
                    "View emergency incidents and responses", "#FCE4EC", "#880E4F",
                    ProviderTriageActivity.class);
            hasAnySharedData = true;
        }

        // Summary Charts
        if (isShared(sharingData, "summaryCharts")) {
            addSection(containerData, "Summary Reports", 
                    "View charts and statistics", "#FFF9C4", "#F57F17",
                    null); // TODO: Create ProviderReportsActivity
            hasAnySharedData = true;
        }
        
        // Show empty state if no data is shared
        if (!hasAnySharedData) {
            tvEmptyState.setVisibility(View.VISIBLE);
            containerData.setVisibility(View.GONE);
        } else {
            tvEmptyState.setVisibility(View.GONE);
            containerData.setVisibility(View.VISIBLE);
        }
    }

    private boolean isShared(Map<String, Object> sharingData, String permission) {
        return sharingData != null && 
               Boolean.TRUE.equals(sharingData.getOrDefault(permission, false));
    }

    private void addSection(LinearLayout container, String title, String description, 
                           String bgColor, String titleColor, Class<?> activityClass) {
        MaterialCardView card = new MaterialCardView(this);
        
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
        
        // Set click listener based on activity class
        if (activityClass != null) {
            card.setOnClickListener(v -> navigateToActivity(activityClass));
        } else {
            card.setClickable(false);
            card.setAlpha(0.7f); // Dim unavailable sections
        }
        
        container.addView(card);
    }

    private void navigateToActivity(Class<?> activityClass) {
        Intent intent = new Intent(this, activityClass);
        intent.putExtra("CHILD_ID", childId);
        intent.putExtra("CHILD_NAME", childName);
        startActivity(intent);
    }
    
    private void openProviderReport() {
        Intent intent = new Intent(this, ProviderReportActivity.class);
        intent.putExtra("CHILD_ID", childId);
        intent.putExtra("CHILD_NAME", childName);
        intent.putExtra("PROVIDER_ID", providerId);
        startActivity(intent);
    }
}
