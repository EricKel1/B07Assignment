package com.example.b07project;

import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.b07project.adapters.IncidentAdapter;
import com.example.b07project.models.TriageSession;
import com.example.b07project.repository.TriageRepository;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.List;

public class IncidentHistoryActivity extends AppCompatActivity {

    private RecyclerView rvIncidents;
    private TextView tvNoIncidents;
    private ProgressBar progressBar;
    private IncidentAdapter adapter;
    private TriageRepository triageRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_incident_history);

        triageRepository = new TriageRepository();
        
        initializeViews();
        loadIncidents();
    }

    private void initializeViews() {
        rvIncidents = findViewById(R.id.rvIncidents);
        tvNoIncidents = findViewById(R.id.tvNoIncidents);
        progressBar = findViewById(R.id.progressBar);

        rvIncidents.setLayoutManager(new LinearLayoutManager(this));
        adapter = new IncidentAdapter(new ArrayList<>());
        rvIncidents.setAdapter(adapter);
    }

    private void loadIncidents() {
        progressBar.setVisibility(View.VISIBLE);
        rvIncidents.setVisibility(View.GONE);
        tvNoIncidents.setVisibility(View.GONE);

        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        triageRepository.getTriageSessions(userId, new TriageRepository.LoadCallback<List<TriageSession>>() {
            @Override
            public void onSuccess(List<TriageSession> sessions) {
                progressBar.setVisibility(View.GONE);

                if (sessions.isEmpty()) {
                    tvNoIncidents.setVisibility(View.VISIBLE);
                    rvIncidents.setVisibility(View.GONE);
                } else {
                    tvNoIncidents.setVisibility(View.GONE);
                    rvIncidents.setVisibility(View.VISIBLE);
                    adapter.updateData(sessions);
                }
            }

            @Override
            public void onFailure(String error) {
                progressBar.setVisibility(View.GONE);
                tvNoIncidents.setVisibility(View.VISIBLE);
                tvNoIncidents.setText("Error loading incidents: " + error);
                Toast.makeText(IncidentHistoryActivity.this, "Error: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
