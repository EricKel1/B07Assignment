package com.example.b07project;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.LinearLayout;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import com.example.b07project.models.RescueInhalerLog;
import com.example.b07project.repository.RescueInhalerRepository;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class ProviderRescueLogsActivity extends AppCompatActivity {

    private String childId;
    private String childName;
    private LinearLayout containerLogs;
    private RescueInhalerRepository rescueRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_provider_rescue_logs);

        childId = getIntent().getStringExtra("CHILD_ID");
        childName = getIntent().getStringExtra("CHILD_NAME");

        TextView tvTitle = findViewById(R.id.tvTitle);
        tvTitle.setText("Rescue Inhaler Logs - " + (childName != null ? childName : "Patient"));

        Button btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        containerLogs = findViewById(R.id.containerLogs);
        rescueRepository = new RescueInhalerRepository();

        loadRescueLogs();
    }

    private void loadRescueLogs() {
        rescueRepository.getLogsForUser(childId, new RescueInhalerRepository.LoadCallback() {
            @Override
            public void onSuccess(List<RescueInhalerLog> logs) {
                displayLogs(logs);
            }

            @Override
            public void onFailure(String error) {
                TextView tvError = new TextView(ProviderRescueLogsActivity.this);
                tvError.setText("Error loading logs: " + error);
                tvError.setTextColor(0xFFC62828);
                containerLogs.addView(tvError);
            }
        });
    }

    private void displayLogs(List<RescueInhalerLog> logs) {
        if (logs == null || logs.isEmpty()) {
            TextView tvNoData = new TextView(this);
            tvNoData.setText("No rescue inhaler logs found for this patient.");
            tvNoData.setTextSize(14);
            tvNoData.setTextColor(0xFF757575);
            containerLogs.addView(tvNoData);
            return;
        }

        SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.US);

        for (RescueInhalerLog log : logs) {
            CardView card = new CardView(this);
            card.setCardBackgroundColor(0xFFE3F2FD);
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
            tvTimestamp.setText(log.getTimestamp() != null ? 
                    dateFormat.format(log.getTimestamp()) : "Unknown time");
            tvTimestamp.setTextSize(14);
            tvTimestamp.setTypeface(null, android.graphics.Typeface.BOLD);
            tvTimestamp.setTextColor(0xFF1565C0);
            cardContent.addView(tvTimestamp);

            // Dose count
            TextView tvDose = new TextView(this);
            tvDose.setText("Dose: " + log.getDoseCount() + " puff" + (log.getDoseCount() != 1 ? "s" : ""));
            tvDose.setTextSize(14);
            tvDose.setTextColor(0xFF424242);
            LinearLayout.LayoutParams doseParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            doseParams.topMargin = 8;
            tvDose.setLayoutParams(doseParams);
            cardContent.addView(tvDose);

            // Notes
            if (log.getNotes() != null && !log.getNotes().isEmpty()) {
                TextView tvNotes = new TextView(this);
                tvNotes.setText("Notes: " + log.getNotes());
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
            containerLogs.addView(card);
        }
    }
}
