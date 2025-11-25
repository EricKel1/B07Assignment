package com.example.b07project;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.LinearLayout;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import com.example.b07project.models.SymptomCheckIn;
import com.example.b07project.repository.SymptomCheckInRepository;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class ProviderSymptomsActivity extends AppCompatActivity {

    private String childId;
    private String childName;
    private LinearLayout containerSymptoms;
    private SymptomCheckInRepository symptomRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_provider_symptoms);

        childId = getIntent().getStringExtra("CHILD_ID");
        childName = getIntent().getStringExtra("CHILD_NAME");

        TextView tvTitle = findViewById(R.id.tvTitle);
        tvTitle.setText("Symptom Check-Ins - " + (childName != null ? childName : "Patient"));

        Button btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        containerSymptoms = findViewById(R.id.containerSymptoms);
        symptomRepository = new SymptomCheckInRepository();

        loadSymptomEntries();
    }

    private void loadSymptomEntries() {
        symptomRepository.getCheckInsForUser(childId, new SymptomCheckInRepository.LoadCallback() {
            @Override
            public void onSuccess(List<SymptomCheckIn> entries) {
                displaySymptoms(entries);
            }

            @Override
            public void onFailure(String error) {
                TextView tvError = new TextView(ProviderSymptomsActivity.this);
                tvError.setText("Error loading symptoms: " + error);
                tvError.setTextColor(0xFFC62828);
                containerSymptoms.addView(tvError);
            }
        });
    }

    private void displaySymptoms(List<SymptomCheckIn> entries) {
        if (entries == null || entries.isEmpty()) {
            TextView tvNoData = new TextView(this);
            tvNoData.setText("No symptom entries found for this patient.");
            tvNoData.setTextSize(14);
            tvNoData.setTextColor(0xFF757575);
            containerSymptoms.addView(tvNoData);
            return;
        }

        SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.US);

        for (SymptomCheckIn entry : entries) {
            CardView card = new CardView(this);
            card.setCardBackgroundColor(0xFFF3E5F5);
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

            // Date
            TextView tvDate = new TextView(this);
            tvDate.setText(entry.getDate() != null ? dateFormat.format(entry.getDate()) : "Unknown date");
            tvDate.setTextSize(14);
            tvDate.setTypeface(null, android.graphics.Typeface.BOLD);
            tvDate.setTextColor(0xFF6A1B9A);
            cardContent.addView(tvDate);

            // Night Waking
            TextView tvNightWaking = new TextView(this);
            List<String> symptoms = entry.getSymptoms();
            boolean hasNightSymptoms = symptoms != null && symptoms.contains("nighttime symptoms");
            tvNightWaking.setText("Night Waking: " + (hasNightSymptoms ? "Yes" : "No"));
            tvNightWaking.setTextSize(13);
            tvNightWaking.setTextColor(0xFF424242);
            LinearLayout.LayoutParams nightParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            nightParams.topMargin = 8;
            tvNightWaking.setLayoutParams(nightParams);
            cardContent.addView(tvNightWaking);

            // Activity Limitation - Check if symptom level is high
            TextView tvActivityLimit = new TextView(this);
            tvActivityLimit.setText("Activity Limited: " + (entry.getSymptomLevel() >= 3 ? "Yes" : "No"));
            tvActivityLimit.setTextSize(13);
            tvActivityLimit.setTextColor(0xFF424242);
            LinearLayout.LayoutParams activityParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            activityParams.topMargin = 4;
            tvActivityLimit.setLayoutParams(activityParams);
            cardContent.addView(tvActivityLimit);

            // Cough/Wheeze
            TextView tvCoughWheeze = new TextView(this);
            List<String> syms = entry.getSymptoms();
            boolean hasCoughWheeze = (syms != null && (syms.contains("wheezing") || syms.contains("coughing")));
            tvCoughWheeze.setText("Cough/Wheeze: " + (hasCoughWheeze ? "Yes" : "No"));
            tvCoughWheeze.setTextSize(13);
            tvCoughWheeze.setTextColor(0xFF424242);
            LinearLayout.LayoutParams coughParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            coughParams.topMargin = 4;
            tvCoughWheeze.setLayoutParams(coughParams);
            cardContent.addView(tvCoughWheeze);

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
            containerSymptoms.addView(card);
        }
    }
}
