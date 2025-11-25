package com.example.b07project;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.LinearLayout;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import com.example.b07project.models.PEFReading;
import com.example.b07project.repository.PEFRepository;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class ProviderPEFActivity extends AppCompatActivity {

    private String childId;
    private String childName;
    private LinearLayout containerPEF;
    private PEFRepository pefRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_provider_pef);

        childId = getIntent().getStringExtra("CHILD_ID");
        childName = getIntent().getStringExtra("CHILD_NAME");

        TextView tvTitle = findViewById(R.id.tvTitle);
        tvTitle.setText("Peak Flow (PEF) - " + (childName != null ? childName : "Patient"));

        Button btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        containerPEF = findViewById(R.id.containerPEF);
        pefRepository = new PEFRepository();

        loadPEFReadings();
    }

    private void loadPEFReadings() {
        pefRepository.getPEFReadingsForUser(childId, new PEFRepository.LoadCallback<List<PEFReading>>() {
            @Override
            public void onSuccess(List<PEFReading> readings) {
                displayPEF(readings);
            }

            @Override
            public void onFailure(String error) {
                TextView tvError = new TextView(ProviderPEFActivity.this);
                tvError.setText("Error loading peak flow data: " + error);
                tvError.setTextColor(0xFFC62828);
                containerPEF.addView(tvError);
            }
        });
    }

    private void displayPEF(List<PEFReading> readings) {
        if (readings == null || readings.isEmpty()) {
            TextView tvNoData = new TextView(this);
            tvNoData.setText("No peak flow readings found for this patient.");
            tvNoData.setTextSize(14);
            tvNoData.setTextColor(0xFF757575);
            containerPEF.addView(tvNoData);
            return;
        }

        SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.US);

        for (PEFReading reading : readings) {
            CardView card = new CardView(this);
            card.setCardBackgroundColor(0xFFFFEBEE);
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
            tvTimestamp.setText(reading.getTimestamp() != null ? 
                    dateFormat.format(reading.getTimestamp()) : "Unknown time");
            tvTimestamp.setTextSize(14);
            tvTimestamp.setTypeface(null, android.graphics.Typeface.BOLD);
            tvTimestamp.setTextColor(0xFFC62828);
            cardContent.addView(tvTimestamp);

            // PEF Value
            TextView tvPEFValue = new TextView(this);
            tvPEFValue.setText("PEF: " + reading.getValue() + " L/min");
            tvPEFValue.setTextSize(16);
            tvPEFValue.setTypeface(null, android.graphics.Typeface.BOLD);
            tvPEFValue.setTextColor(0xFF1565C0);
            LinearLayout.LayoutParams pefParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            pefParams.topMargin = 8;
            tvPEFValue.setLayoutParams(pefParams);
            cardContent.addView(tvPEFValue);

            // Zone
            String zone = reading.getZone();
            int zoneColor = 0xFF757575;
            if ("GREEN".equals(zone)) {
                zoneColor = 0xFF2E7D32;
            } else if ("YELLOW".equals(zone)) {
                zoneColor = 0xFFF57F17;
            } else if ("RED".equals(zone)) {
                zoneColor = 0xFFC62828;
            }

            TextView tvZone = new TextView(this);
            tvZone.setText("Zone: " + zone + " (" + reading.getPercentageOfPB() + "% of PB)");
            tvZone.setTextSize(13);
            tvZone.setTextColor(zoneColor);
            LinearLayout.LayoutParams zoneParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            zoneParams.topMargin = 8;
            tvZone.setLayoutParams(zoneParams);
            cardContent.addView(tvZone);

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
            containerPEF.addView(card);
        }
    }
}
