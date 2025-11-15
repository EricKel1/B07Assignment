package com.example.b07project;

import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.b07project.adapters.MedicineLogAdapter;
import com.example.b07project.models.ControllerMedicineLog;
import com.example.b07project.models.MedicineLog;
import com.example.b07project.models.RescueInhalerLog;
import com.example.b07project.repository.ControllerMedicineRepository;
import com.example.b07project.repository.RescueInhalerRepository;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import java.util.List;

public class RescueInhalerHistoryActivity extends AppCompatActivity {
    
    private RadioGroup rgHistoryType;
    private RadioButton rbRescueHistory, rbControllerHistory;
    private RecyclerView recyclerView;
    private ProgressBar progress;
    private TextView tvEmptyMessage;
    
    private MedicineLogAdapter adapter;
    private RescueInhalerRepository rescueRepository;
    private ControllerMedicineRepository controllerRepository;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rescue_inhaler_history);
        
        initializeViews();
        setupRecyclerView();
        
        rescueRepository = new RescueInhalerRepository();
        controllerRepository = new ControllerMedicineRepository();
        
        rgHistoryType.setOnCheckedChangeListener((group, checkedId) -> loadLogs());
        
        loadLogs();
    }
    
    private void initializeViews() {
        rgHistoryType = findViewById(R.id.rgHistoryType);
        rbRescueHistory = findViewById(R.id.rbRescueHistory);
        rbControllerHistory = findViewById(R.id.rbControllerHistory);
        recyclerView = findViewById(R.id.recyclerView);
        progress = findViewById(R.id.progress);
        tvEmptyMessage = findViewById(R.id.tvEmptyMessage);
    }
    
    private void setupRecyclerView() {
        adapter = new MedicineLogAdapter();
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
        
        boolean isController = rbControllerHistory.isChecked();
        
        if (isController) {
            controllerRepository.getLogsForUser(currentUser.getUid(), new ControllerMedicineRepository.LoadCallback() {
                @Override
                public void onSuccess(List<ControllerMedicineLog> logs) {
                    showLoading(false);
                    if (logs.isEmpty()) {
                        showEmptyMessage(true);
                    } else {
                        showEmptyMessage(false);
                        adapter.setLogs(logs, true);
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
        } else {
            rescueRepository.getLogsForUser(currentUser.getUid(), new RescueInhalerRepository.LoadCallback() {
                @Override
                public void onSuccess(List<RescueInhalerLog> logs) {
                    showLoading(false);
                    if (logs.isEmpty()) {
                        showEmptyMessage(true);
                    } else {
                        showEmptyMessage(false);
                        adapter.setLogs(logs, false);
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
