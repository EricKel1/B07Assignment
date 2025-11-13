package com.example.b07project;

import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.b07project.adapters.RescueInhalerLogAdapter;
import com.example.b07project.models.RescueInhalerLog;
import com.example.b07project.repository.RescueInhalerRepository;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import java.util.List;

public class RescueInhalerHistoryActivity extends AppCompatActivity {
    
    private RecyclerView recyclerView;
    private ProgressBar progress;
    private TextView tvEmptyMessage;
    
    private RescueInhalerLogAdapter adapter;
    private RescueInhalerRepository repository;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rescue_inhaler_history);
        
        initializeViews();
        setupRecyclerView();
        
        repository = new RescueInhalerRepository();
        loadLogs();
    }
    
    private void initializeViews() {
        recyclerView = findViewById(R.id.recyclerView);
        progress = findViewById(R.id.progress);
        tvEmptyMessage = findViewById(R.id.tvEmptyMessage);
    }
    
    private void setupRecyclerView() {
        adapter = new RescueInhalerLogAdapter();
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
    }
    
    private void loadLogs() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "Please sign in to view logs", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        
        showLoading(true);
        
        repository.getLogsForUser(currentUser.getUid(), new RescueInhalerRepository.LoadCallback() {
            @Override
            public void onSuccess(List<RescueInhalerLog> logs) {
                showLoading(false);
                if (logs.isEmpty()) {
                    showEmptyMessage(true);
                } else {
                    showEmptyMessage(false);
                    adapter.setLogs(logs);
                }
            }
            
            @Override
            public void onFailure(String error) {
                showLoading(false);
                Toast.makeText(RescueInhalerHistoryActivity.this, 
                    "Failed to load logs: " + error, 
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
