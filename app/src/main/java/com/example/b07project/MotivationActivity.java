package com.example.b07project;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.b07project.adapters.BadgeAdapter;
import com.example.b07project.models.Badge;
import com.example.b07project.models.Streak;
import com.example.b07project.services.MotivationService;
import java.util.ArrayList;
import java.util.List;

public class MotivationActivity extends AppCompatActivity {

    private TextView tvControllerStreak, tvControllerLongest;
    private TextView tvTechniqueStreak, tvTechniqueLongest;
    private RecyclerView rvBadges;
    private Button btnSettings, btnClose;
    private BadgeAdapter badgeAdapter;
    private MotivationService motivationService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_motivation);

        motivationService = new MotivationService(this);

        initializeViews();
        setupRecyclerView();
        setupListeners();
        loadData();
    }

    private void initializeViews() {
        tvControllerStreak = findViewById(R.id.tvControllerStreak);
        tvControllerLongest = findViewById(R.id.tvControllerLongest);
        tvTechniqueStreak = findViewById(R.id.tvTechniqueStreak);
        tvTechniqueLongest = findViewById(R.id.tvTechniqueLongest);
        rvBadges = findViewById(R.id.rvBadges);
        btnSettings = findViewById(R.id.btnSettings);
        btnClose = findViewById(R.id.btnClose);
    }

    private void setupRecyclerView() {
        badgeAdapter = new BadgeAdapter(new ArrayList<>());
        rvBadges.setLayoutManager(new LinearLayoutManager(this));
        rvBadges.setAdapter(badgeAdapter);
    }

    private void setupListeners() {
        btnSettings.setOnClickListener(v -> openSettings());
        btnClose.setOnClickListener(v -> finish());
    }

    private void loadData() {
        loadStreaks();
        loadBadges();
    }

    private void loadStreaks() {
        // Load controller streak
        motivationService.getStreak("controller", new MotivationService.StreakCallback() {
            @Override
            public void onSuccess(Streak streak) {
                tvControllerStreak.setText(streak.getCurrentStreak() + " days");
                tvControllerLongest.setText("Longest: " + streak.getLongestStreak() + " days");
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(MotivationActivity.this, "Failed to load controller streak", Toast.LENGTH_SHORT).show();
            }
        });

        // Load technique streak
        motivationService.getStreak("technique", new MotivationService.StreakCallback() {
            @Override
            public void onSuccess(Streak streak) {
                tvTechniqueStreak.setText(streak.getCurrentStreak() + " days");
                tvTechniqueLongest.setText("Longest: " + streak.getLongestStreak() + " days");
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(MotivationActivity.this, "Failed to load technique streak", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadBadges() {
        motivationService.getAllBadges(new MotivationService.BadgeListCallback() {
            @Override
            public void onSuccess(List<Badge> badges) {
                badgeAdapter.updateBadges(badges);
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(MotivationActivity.this, "Failed to load badges", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void openSettings() {
        Intent intent = new Intent(this, MotivationSettingsActivity.class);
        startActivity(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh data when returning from settings
        loadData();
    }
}
