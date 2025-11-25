package com.example.b07project;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.LinearLayout;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import com.example.b07project.models.TriageSession;
import com.example.b07project.repository.TriageRepository;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class ProviderTriageActivity extends AppCompatActivity {

    private String childId;
    private String childName;
    private LinearLayout containerTriage;
    private TriageRepository triageRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_provider_triage);

        childId = getIntent().getStringExtra("CHILD_ID");
        childName = getIntent().getStringExtra("CHILD_NAME");

        TextView tvTitle = findViewById(R.id.tvTitle);
        tvTitle.setText("Triage Incidents - " + (childName != null ? childName : "Patient"));

        Button btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        containerTriage = findViewById(R.id.containerTriage);
        triageRepository = new TriageRepository();

        loadTriageSessions();
    }

    private void loadTriageSessions() {
        triageRepository.getTriageSessions(childId, new TriageRepository.LoadCallback<List<TriageSession>>() {
            @Override
            public void onSuccess(List<TriageSession> sessions) {
                displayTriage(sessions);
            }

            @Override
            public void onFailure(String error) {
                TextView tvError = new TextView(ProviderTriageActivity.this);
                tvError.setText("Error loading triage incidents: " + error);
                tvError.setTextColor(0xFFC62828);
                containerTriage.addView(tvError);
            }
        });
    }

    private void displayTriage(List<TriageSession> sessions) {
        if (sessions == null || sessions.isEmpty()) {
            TextView tvNoData = new TextView(this);
            tvNoData.setText("No triage incidents recorded for this patient.");
            tvNoData.setTextSize(14);
            tvNoData.setTextColor(0xFF757575);
            containerTriage.addView(tvNoData);
            return;
        }

        SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.US);

        for (TriageSession session : sessions) {
            // Determine card color based on outcome
            int cardBgColor = 0xFFFFEBEE; // Red tinted
            int titleColor = 0xFF880E4F; // Deep pink
            
            CardView card = new CardView(this);
            card.setCardBackgroundColor(cardBgColor);
            card.setRadius(12);
            card.setCardElevation(4);
            LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            cardParams.setMargins(0, 8, 0, 0);
            card.setLayoutParams(cardParams);

            LinearLayout cardContent = new LinearLayout(this);
            cardContent.setOrientation(LinearLayout.VERTICAL);
            cardContent.setPadding(16, 16, 16, 16);

            // Timestamp
            TextView tvTimestamp = new TextView(this);
            tvTimestamp.setText(session.getStartTime() != null ? 
                    dateFormat.format(session.getStartTime()) : "Unknown time");
            tvTimestamp.setTextSize(14);
            tvTimestamp.setTypeface(null, android.graphics.Typeface.BOLD);
            tvTimestamp.setTextColor(titleColor);
            cardContent.addView(tvTimestamp);

            // Red flags present
            TextView tvFlags = new TextView(this);
            StringBuilder flags = new StringBuilder("Flags: ");
            if (session.isCantSpeakFullSentences()) flags.append("Can't speak full sentences, ");
            if (session.isChestPullingInRetractions()) flags.append("Chest pulling in, ");
            if (session.isBlueGrayLipsNails()) flags.append("Blue/gray lips");
            
            String flagsStr = flags.toString();
            if (flagsStr.endsWith(", ")) flagsStr = flagsStr.substring(0, flagsStr.length() - 2);
            
            tvFlags.setText(flagsStr);
            tvFlags.setTextSize(13);
            tvFlags.setTextColor(0xFF424242);
            LinearLayout.LayoutParams flagsParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            flagsParams.topMargin = 8;
            tvFlags.setLayoutParams(flagsParams);
            cardContent.addView(tvFlags);

            // Guidance given
            String guidanceText = session.getGuidanceShown();
            if (guidanceText != null && !guidanceText.isEmpty()) {
                TextView tvGuidance = new TextView(this);
                tvGuidance.setText("Guidance: " + guidanceText);
                tvGuidance.setTextSize(13);
                tvGuidance.setTextColor(0xFF424242);
                LinearLayout.LayoutParams guidanceParams = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT);
                guidanceParams.topMargin = 8;
                tvGuidance.setLayoutParams(guidanceParams);
                cardContent.addView(tvGuidance);
            }

            // Notes
            String notes = session.getUserResponse();
            if (notes != null && !notes.isEmpty()) {
                TextView tvNotes = new TextView(this);
                tvNotes.setText("Notes: " + notes);
                tvNotes.setTextSize(12);
                tvNotes.setTextColor(0xFF757575);
                LinearLayout.LayoutParams notesParams = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT);
                notesParams.topMargin = 8;
                tvNotes.setLayoutParams(notesParams);
                cardContent.addView(tvNotes);
            }

            // Shared badge
            TextView tvBadge = new TextView(this);
            tvBadge.setText("✓ Shared with Provider");
            tvBadge.setTextSize(11);
            tvBadge.setTextColor(0xFF2E7D32);
            LinearLayout.LayoutParams badgeParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            badgeParams.topMargin = 12;
            tvBadge.setLayoutParams(badgeParams);
            cardContent.addView(tvBadge);

            card.addView(cardContent);
            containerTriage.addView(card);
        }
    }
}
