package com.example.b07project;

import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.b07project.adapters.SymptomCheckInAdapter;
import com.example.b07project.models.SymptomCheckIn;
import com.example.b07project.repository.SymptomCheckInRepository;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.List;

public class SymptomHistoryActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private ProgressBar progress;
    private TextView tvEmptyMessage;

    private SymptomCheckInAdapter adapter;
    private SymptomCheckInRepository repository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_symptom_history);

        initializeViews();
        setupRecyclerView();

        repository = new SymptomCheckInRepository();
        loadCheckIns();
    }

    private void initializeViews() {
        recyclerView = findViewById(R.id.recyclerView);
        progress = findViewById(R.id.progress);
        tvEmptyMessage = findViewById(R.id.tvEmptyMessage);
    }

    private void setupRecyclerView() {
        adapter = new SymptomCheckInAdapter();
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
    }

    private void loadCheckIns() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "Please sign in to view history", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        showLoading(true);

        repository.getCheckInsForUser(currentUser.getUid(), new SymptomCheckInRepository.LoadCallback() {
            @Override
            public void onSuccess(List<SymptomCheckIn> checkIns) {
                showLoading(false);
                if (checkIns.isEmpty()) {
                    showEmptyMessage(true);
                } else {
                    showEmptyMessage(false);
                    adapter.setCheckIns(checkIns);
                }
            }

            @Override
            public void onFailure(String error) {
                showLoading(false);
                Toast.makeText(SymptomHistoryActivity.this,
                    "Failed to load history: " + error,
                    Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showLoading(boolean show) {
        progress.setVisibility(show ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(show ? View.GONE : View.VISIBLE);
    }

    private void showEmptyMessage(boolean show) {
        tvEmptyMessage.setVisibility(show ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(show ? View.GONE : View.VISIBLE);
    }
}
