package com.example.b07project;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.b07project.adapters.ChildAdapter;
import com.example.b07project.main.WelcomeActivity;
import com.example.b07project.models.PEFReading;
import com.example.b07project.models.PersonalBest;
import com.example.b07project.models.RescueInhalerLog;
import com.example.b07project.repository.PEFRepository;
import com.example.b07project.repository.RescueInhalerRepository;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ParentDashboardActivity extends AppCompatActivity {

    private RecyclerView rvChildren;
    private ProgressBar progressBar;
    private ChildAdapter adapter;
    private List<Map<String, String>> childrenList;
    private PEFRepository pefRepository;
    private RescueInhalerRepository rescueRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_parent_dashboard);

        rvChildren = findViewById(R.id.rvChildren);
        progressBar = findViewById(R.id.progressBar);
        Button btnLogout = findViewById(R.id.btnLogout);

        pefRepository = new PEFRepository();
        rescueRepository = new RescueInhalerRepository();

        childrenList = new ArrayList<>();
        adapter = new ChildAdapter(childrenList, new ChildAdapter.OnChildActionListener() {
            @Override
            public void onGenerateCode(String childName, String childId) {
                onGenerateCodeClicked(childName, childId);
            }

            @Override
            public void onViewReports(String childName, String childId) {
                onViewReportsClicked(childName, childId);
            }
        });
        rvChildren.setLayoutManager(new LinearLayoutManager(this));
        rvChildren.setAdapter(adapter);

        btnLogout.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            Intent intent = new Intent(ParentDashboardActivity.this, WelcomeActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });

        loadChildren();
    }

    private void loadChildren() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        progressBar.setVisibility(View.VISIBLE);
        FirebaseFirestore.getInstance().collection("children")
                .whereEqualTo("parentId", user.getUid())
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    childrenList.clear();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Map<String, String> child = new HashMap<>();
                        child.put("id", document.getId());
                        child.put("name", document.getString("name"));
                        child.put("zone", "Loading...");
                        child.put("lastRescue", "Loading...");
                        child.put("weeklyRescues", "Loading...");
                        childrenList.add(child);
                    }
                    adapter.notifyDataSetChanged();
                    
                    // Now fetch stats for each child
                    for (int i = 0; i < childrenList.size(); i++) {
                        fetchChildStats(childrenList.get(i), i);
                    }
                    
                    if (childrenList.isEmpty()) {
                        progressBar.setVisibility(View.GONE);
                    }
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Failed to load children: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void fetchChildStats(Map<String, String> child, int position) {
        String childId = child.get("id");
        
        // 1. Fetch Zone (PEF)
        pefRepository.getLastPEFReading(childId, new PEFRepository.LoadCallback<PEFReading>() {
            @Override
            public void onSuccess(PEFReading reading) {
                if (reading != null && reading.getZone() != null) {
                    child.put("zone", PersonalBest.getZoneLabel(reading.getZone()));
                } else {
                    child.put("zone", "Unknown");
                }
                adapter.notifyItemChanged(position);
                checkIfAllLoaded();
            }

            @Override
            public void onFailure(String error) {
                child.put("zone", "Unknown");
                adapter.notifyItemChanged(position);
                checkIfAllLoaded();
            }
        });

        // 2. Fetch Rescue Stats
        Calendar calendar = Calendar.getInstance();
        Date endDate = calendar.getTime();
        calendar.add(Calendar.DAY_OF_YEAR, -7);
        Date startDate = calendar.getTime();

        rescueRepository.getLogsForUserInDateRange(childId, startDate, endDate, new RescueInhalerRepository.LoadCallback() {
            @Override
            public void onSuccess(List<RescueInhalerLog> logs) {
                int weeklyCount = 0;
                Date lastRescueTime = null;
                
                for (RescueInhalerLog log : logs) {
                    weeklyCount += log.getDoseCount();
                    if (log.getTimestamp() != null) {
                        if (lastRescueTime == null || log.getTimestamp().after(lastRescueTime)) {
                            lastRescueTime = log.getTimestamp();
                        }
                    }
                }
                
                child.put("weeklyRescues", String.valueOf(weeklyCount));
                
                if (lastRescueTime != null) {
                    SimpleDateFormat sdf = new SimpleDateFormat("MMM d, h:mm a", Locale.getDefault());
                    child.put("lastRescue", sdf.format(lastRescueTime));
                } else {
                    child.put("lastRescue", "None");
                }
                
                adapter.notifyItemChanged(position);
                checkIfAllLoaded();
            }

            @Override
            public void onFailure(String error) {
                child.put("weeklyRescues", "Error");
                child.put("lastRescue", "Error");
                adapter.notifyItemChanged(position);
                checkIfAllLoaded();
            }
        });
    }

    private int loadedCount = 0;
    private void checkIfAllLoaded() {
        // This is a rough approximation since we have 2 async calls per child
        // Ideally we'd track exact requests, but for hiding the progress bar this is okay-ish
        // or we just hide it after the initial list load and let the items update individually.
        // I'll hide it immediately after list load in loadChildren actually.
        progressBar.setVisibility(View.GONE);
    }

    private void onGenerateCodeClicked(String childName, String childId) {
        Intent intent = new Intent(this, ParentChildLogsActivity.class);
        intent.putExtra("childName", childName);
        startActivity(intent);
    }

    private void onViewReportsClicked(String childName, String childId) {
        Intent intent = new Intent(this, StatisticsReportsActivity.class);
        intent.putExtra("EXTRA_CHILD_ID", childId);
        startActivity(intent);
    }
}
