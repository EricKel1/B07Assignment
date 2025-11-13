package com.example.b07project;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.b07project.models.RescueInhalerLog;
import com.example.b07project.repository.RescueInhalerRepository;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class LogRescueInhalerActivity extends AppCompatActivity {
    
    private TextView tvTimestamp, tvDoseCount, tvMessage;
    private EditText etNotes;
    private Button btnDecrease, btnIncrease, btnSave;
    private ProgressBar progress;
    
    private int doseCount = 1;
    private Date timestamp;
    private RescueInhalerRepository repository;
    private SimpleDateFormat dateFormat;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_log_rescue_inhaler);
        
        initializeViews();
        repository = new RescueInhalerRepository();
        dateFormat = new SimpleDateFormat("MMM dd, yyyy 'at' hh:mm a", Locale.getDefault());
        
        timestamp = new Date();
        updateTimestampDisplay();
        updateDoseCountDisplay();
        
        setupListeners();
    }
    
    private void initializeViews() {
        tvTimestamp = findViewById(R.id.tvTimestamp);
        tvDoseCount = findViewById(R.id.tvDoseCount);
        tvMessage = findViewById(R.id.tvMessage);
        etNotes = findViewById(R.id.etNotes);
        btnDecrease = findViewById(R.id.btnDecrease);
        btnIncrease = findViewById(R.id.btnIncrease);
        btnSave = findViewById(R.id.btnSave);
        progress = findViewById(R.id.progress);
    }
    
    private void setupListeners() {
        btnDecrease.setOnClickListener(v -> {
            if (doseCount > 1) {
                doseCount--;
                updateDoseCountDisplay();
            }
        });
        
        btnIncrease.setOnClickListener(v -> {
            if (doseCount < 10) {
                doseCount++;
                updateDoseCountDisplay();
            }
        });
        
        btnSave.setOnClickListener(v -> saveLog());
    }
    
    private void updateTimestampDisplay() {
        tvTimestamp.setText(dateFormat.format(timestamp));
    }
    
    private void updateDoseCountDisplay() {
        tvDoseCount.setText(String.valueOf(doseCount));
    }
    
    private void saveLog() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            showMessage("Please sign in to save logs", true);
            return;
        }
        
        showLoading(true);
        
        String notes = etNotes.getText().toString().trim();
        RescueInhalerLog log = new RescueInhalerLog(
            currentUser.getUid(),
            timestamp,
            doseCount,
            notes.isEmpty() ? null : notes
        );
        
        repository.saveLog(log, new RescueInhalerRepository.SaveCallback() {
            @Override
            public void onSuccess(String documentId) {
                showLoading(false);
                Toast.makeText(LogRescueInhalerActivity.this, 
                    "Rescue inhaler use logged successfully!", 
                    Toast.LENGTH_LONG).show();
                finish();
            }
            
            @Override
            public void onFailure(String error) {
                showLoading(false);
                showMessage("Failed to save log: " + error, true);
            }
        });
    }
    
    private void showLoading(boolean show) {
        progress.setVisibility(show ? View.VISIBLE : View.GONE);
        btnSave.setEnabled(!show);
        btnIncrease.setEnabled(!show);
        btnDecrease.setEnabled(!show);
    }
    
    private void showMessage(String message, boolean isError) {
        tvMessage.setText(message);
        tvMessage.setTextColor(getColor(isError ? android.R.color.holo_red_dark : android.R.color.holo_green_dark));
        tvMessage.setVisibility(View.VISIBLE);
    }
}
