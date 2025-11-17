package com.example.b07project;

import android.app.TimePickerDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import com.example.b07project.models.ControllerMedicineLog;
import com.example.b07project.models.MedicineLog;
import com.example.b07project.models.RescueInhalerLog;
import com.example.b07project.repository.ControllerMedicineRepository;
import com.example.b07project.repository.RescueInhalerRepository;
import com.example.b07project.services.MotivationService;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class LogRescueInhalerActivity extends AppCompatActivity {
    
    private RadioGroup rgMedicineType;
    private RadioButton rbRescue, rbController;
    private ConstraintLayout controllerOnlySection;
    private TextView tvTimestamp, tvDoseCount, tvMessage, tvScheduledTime;
    private Button btnSelectTime;
    private CheckBox cbTakenOnTime;
    private CheckBox cbTriggerExercise, cbTriggerColdAir, cbTriggerPets, cbTriggerPollen;
    private CheckBox cbTriggerStress, cbTriggerSmoke, cbTriggerWeather, cbTriggerDust;
    private EditText etNotes;
    private Button btnDecrease, btnIncrease, btnSave;
    private ProgressBar progress;
    
    private int doseCount = 1;
    private Date timestamp;
    private Date scheduledTime = null;
    private RescueInhalerRepository rescueRepository;
    private ControllerMedicineRepository controllerRepository;
    private MotivationService motivationService;
    private SimpleDateFormat dateFormat;
    private SimpleDateFormat timeFormat;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_log_rescue_inhaler);
        
        initializeViews();
        rescueRepository = new RescueInhalerRepository();
        controllerRepository = new ControllerMedicineRepository();
        motivationService = new MotivationService(this);
        dateFormat = new SimpleDateFormat("MMM dd, yyyy 'at' hh:mm a", Locale.getDefault());
        timeFormat = new SimpleDateFormat("hh:mm a", Locale.getDefault());
        
        timestamp = new Date();
        updateTimestampDisplay();
        updateDoseCountDisplay();
        
        setupListeners();
    }
    
    private void initializeViews() {
        rgMedicineType = findViewById(R.id.rgMedicineType);
        rbRescue = findViewById(R.id.rbRescue);
        rbController = findViewById(R.id.rbController);
        controllerOnlySection = findViewById(R.id.controllerOnlySection);
        tvTimestamp = findViewById(R.id.tvTimestamp);
        tvDoseCount = findViewById(R.id.tvDoseCount);
        tvMessage = findViewById(R.id.tvMessage);
        tvScheduledTime = findViewById(R.id.tvScheduledTime);
        btnSelectTime = findViewById(R.id.btnSelectTime);
        cbTakenOnTime = findViewById(R.id.cbTakenOnTime);
        cbTriggerExercise = findViewById(R.id.cbTriggerExercise);
        cbTriggerColdAir = findViewById(R.id.cbTriggerColdAir);
        cbTriggerPets = findViewById(R.id.cbTriggerPets);
        cbTriggerPollen = findViewById(R.id.cbTriggerPollen);
        cbTriggerStress = findViewById(R.id.cbTriggerStress);
        cbTriggerSmoke = findViewById(R.id.cbTriggerSmoke);
        cbTriggerWeather = findViewById(R.id.cbTriggerWeather);
        cbTriggerDust = findViewById(R.id.cbTriggerDust);
        etNotes = findViewById(R.id.etNotes);
        btnDecrease = findViewById(R.id.btnDecrease);
        btnIncrease = findViewById(R.id.btnIncrease);
        btnSave = findViewById(R.id.btnSave);
        progress = findViewById(R.id.progress);
    }
    
    private void setupListeners() {
        rgMedicineType.setOnCheckedChangeListener((group, checkedId) -> {
            boolean isController = checkedId == R.id.rbController;
            controllerOnlySection.setVisibility(isController ? View.VISIBLE : View.GONE);
            if (!isController) {
                scheduledTime = null;
                tvScheduledTime.setText("Not selected");
                cbTakenOnTime.setChecked(false);
            }
        });
        
        btnSelectTime.setOnClickListener(v -> showTimePickerDialog());
        
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
    
    private void showTimePickerDialog() {
        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);
        
        TimePickerDialog timePickerDialog = new TimePickerDialog(
            this,
            (view, selectedHour, selectedMinute) -> {
                Calendar scheduledCal = Calendar.getInstance();
                scheduledCal.set(Calendar.HOUR_OF_DAY, selectedHour);
                scheduledCal.set(Calendar.MINUTE, selectedMinute);
                scheduledCal.set(Calendar.SECOND, 0);
                scheduledTime = scheduledCal.getTime();
                tvScheduledTime.setText(timeFormat.format(scheduledTime));
                
                // Auto-detect if taken on time (within 30 minutes)
                long timeDiff = Math.abs(timestamp.getTime() - scheduledTime.getTime());
                boolean onTime = timeDiff <= 30 * 60 * 1000; // 30 minutes in milliseconds
                cbTakenOnTime.setChecked(onTime);
            },
            hour,
            minute,
            false
        );
        timePickerDialog.show();
    }
    
    private void updateTimestampDisplay() {
        tvTimestamp.setText(dateFormat.format(timestamp));
    }
    
    private void updateDoseCountDisplay() {
        tvDoseCount.setText(String.valueOf(doseCount));
    }
    
    private List<String> getSelectedTriggers() {
        List<String> triggers = new ArrayList<>();
        if (cbTriggerExercise.isChecked()) triggers.add("exercise");
        if (cbTriggerColdAir.isChecked()) triggers.add("cold air");
        if (cbTriggerPets.isChecked()) triggers.add("pets");
        if (cbTriggerPollen.isChecked()) triggers.add("pollen");
        if (cbTriggerStress.isChecked()) triggers.add("stress");
        if (cbTriggerSmoke.isChecked()) triggers.add("smoke");
        if (cbTriggerWeather.isChecked()) triggers.add("weather change");
        if (cbTriggerDust.isChecked()) triggers.add("dust");
        return triggers;
    }
    
    private void saveLog() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            showMessage("Please sign in to save logs", true);
            return;
        }
        
        boolean isController = rbController.isChecked();
        
        if (isController && scheduledTime == null) {
            showMessage("Please select a scheduled time", true);
            return;
        }
        
        showLoading(true);
        
        String notes = etNotes.getText().toString().trim();
        List<String> triggers = getSelectedTriggers();
        
        if (isController) {
            ControllerMedicineLog log = new ControllerMedicineLog(
                currentUser.getUid(),
                timestamp,
                doseCount,
                scheduledTime,
                cbTakenOnTime.isChecked(),
                triggers,
                notes.isEmpty() ? null : notes
            );
            
            controllerRepository.saveLog(log, new ControllerMedicineRepository.SaveCallback() {
                @Override
                public void onSuccess(String documentId) {
                    // Update controller streak after successful save
                    motivationService.updateControllerStreak(() -> {
                        showLoading(false);
                        Toast.makeText(LogRescueInhalerActivity.this, 
                            "Controller medicine logged successfully! Streak updated.", 
                            Toast.LENGTH_LONG).show();
                        finish();
                    });
                }
                
                @Override
                public void onFailure(String error) {
                    showLoading(false);
                    showMessage("Failed to save log: " + error, true);
                }
            });
        } else {
            RescueInhalerLog log = new RescueInhalerLog(
                currentUser.getUid(),
                timestamp,
                doseCount,
                triggers,
                notes.isEmpty() ? null : notes
            );
            
            rescueRepository.saveLog(log, new RescueInhalerRepository.SaveCallback() {
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
