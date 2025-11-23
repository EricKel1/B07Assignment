package com.example.b07project;

import android.app.TimePickerDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.b07project.models.MedicationSchedule;
import com.example.b07project.models.MedicineInventory;
import com.example.b07project.repository.InventoryRepository;
import com.example.b07project.repository.ScheduleRepository;
import com.google.firebase.auth.FirebaseAuth;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ConfigureScheduleActivity extends AppCompatActivity {

    private AutoCompleteTextView etMedicationName;
    private EditText etDosage;
    private Spinner spinnerFrequency;
    private LinearLayout layoutTimePickers;
    private Button btnSaveSchedule;
    private ScheduleRepository scheduleRepository;
    private InventoryRepository inventoryRepository;
    private String childId;
    private List<String> selectedTimes = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_configure_schedule);

        childId = getIntent().getStringExtra("EXTRA_CHILD_ID");
        scheduleRepository = new ScheduleRepository();
        inventoryRepository = new InventoryRepository(this);

        initializeViews();
        setupFrequencySpinner();
        loadInventorySuggestions();
        loadExistingSchedule();
        
        btnSaveSchedule.setOnClickListener(v -> saveSchedule());
    }

    private void initializeViews() {
        etMedicationName = findViewById(R.id.etMedicationName);
        etDosage = findViewById(R.id.etDosage);
        spinnerFrequency = findViewById(R.id.spinnerFrequency);
        layoutTimePickers = findViewById(R.id.layoutTimePickers);
        btnSaveSchedule = findViewById(R.id.btnSaveSchedule);
    }

    private void loadInventorySuggestions() {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        inventoryRepository.getMedicines(userId, new InventoryRepository.LoadCallback() {
            @Override
            public void onSuccess(List<MedicineInventory> medicines) {
                List<String> medicineNames = new ArrayList<>();
                for (MedicineInventory med : medicines) {
                    if (!medicineNames.contains(med.getName())) {
                        medicineNames.add(med.getName());
                    }
                }
                ArrayAdapter<String> adapter = new ArrayAdapter<>(
                    ConfigureScheduleActivity.this,
                    android.R.layout.simple_dropdown_item_1line,
                    medicineNames
                );
                etMedicationName.setAdapter(adapter);
            }

            @Override
            public void onFailure(String error) {
                // Just ignore errors for suggestions
            }
        });
    }

    private void setupFrequencySpinner() {
        String[] frequencies = {"1 time per day", "2 times per day", "3 times per day", "4 times per day"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, frequencies);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerFrequency.setAdapter(adapter);

        spinnerFrequency.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                updateTimePickers(position + 1);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void updateTimePickers(int count) {
        layoutTimePickers.removeAllViews();
        List<String> oldTimes = new ArrayList<>(selectedTimes);
        selectedTimes.clear();
        
        // Preserve existing times if possible, otherwise default
        for (int i = 0; i < count; i++) {
            if (i < oldTimes.size()) {
                selectedTimes.add(oldTimes.get(i));
            } else {
                selectedTimes.add("08:00"); // Default
            }
            addTimePickerRow(i);
        }
    }

    private void addTimePickerRow(int index) {
        View view = getLayoutInflater().inflate(android.R.layout.simple_list_item_1, null);
        TextView tvTime = view.findViewById(android.R.id.text1);
        
        tvTime.setText("Dose " + (index + 1) + ": " + selectedTimes.get(index));
        tvTime.setCompoundDrawablesWithIntrinsicBounds(android.R.drawable.ic_menu_recent_history, 0, 0, 0);
        tvTime.setPadding(16, 24, 16, 24);
        
        tvTime.setOnClickListener(v -> {
            String[] parts = selectedTimes.get(index).split(":");
            int hour = Integer.parseInt(parts[0]);
            int minute = Integer.parseInt(parts[1]);

            new TimePickerDialog(this, (timePicker, h, m) -> {
                String time = String.format(Locale.getDefault(), "%02d:%02d", h, m);
                selectedTimes.set(index, time);
                tvTime.setText("Dose " + (index + 1) + ": " + time);
            }, hour, minute, true).show();
        });

        layoutTimePickers.addView(view);
    }

    private void loadExistingSchedule() {
        scheduleRepository.getSchedule(childId, new ScheduleRepository.LoadCallback() {
            @Override
            public void onSuccess(MedicationSchedule schedule) {
                if (schedule != null) {
                    etMedicationName.setText(schedule.getMedicationName());
                    etDosage.setText(String.valueOf(schedule.getDosagePerIntake()));
                    
                    int freqIndex = schedule.getFrequency() - 1;
                    if (freqIndex >= 0 && freqIndex < 4) {
                        spinnerFrequency.setSelection(freqIndex);
                    }
                    
                    // Wait for spinner to update pickers, then fill times
                    layoutTimePickers.post(() -> {
                        if (schedule.getScheduledTimes() != null && schedule.getScheduledTimes().size() == schedule.getFrequency()) {
                            selectedTimes = new ArrayList<>(schedule.getScheduledTimes());
                            // Refresh UI
                            layoutTimePickers.removeAllViews();
                            for (int i = 0; i < selectedTimes.size(); i++) {
                                addTimePickerRow(i);
                            }
                        }
                    });
                }
            }

            @Override
            public void onFailure(String error) {
                Toast.makeText(ConfigureScheduleActivity.this, "Error loading schedule", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void saveSchedule() {
        String name = etMedicationName.getText().toString().trim();
        String dosageStr = etDosage.getText().toString().trim();
        
        if (name.isEmpty() || dosageStr.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        int dosage = Integer.parseInt(dosageStr);
        int frequency = spinnerFrequency.getSelectedItemPosition() + 1;

        MedicationSchedule schedule = new MedicationSchedule(childId, name, dosage, frequency, selectedTimes);

        scheduleRepository.saveSchedule(childId, schedule, new ScheduleRepository.SaveCallback() {
            @Override
            public void onSuccess() {
                Toast.makeText(ConfigureScheduleActivity.this, "Schedule saved!", Toast.LENGTH_SHORT).show();
                finish();
            }

            @Override
            public void onFailure(String error) {
                Toast.makeText(ConfigureScheduleActivity.this, "Error saving: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
