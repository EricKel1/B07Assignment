package com.example.b07project;

import android.app.AlertDialog;
import android.app.TimePickerDialog;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
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
import com.example.b07project.models.Badge;
import com.example.b07project.models.ControllerMedicineLog;
import com.example.b07project.models.MedicineLog;
import com.example.b07project.models.RescueInhalerLog;
import com.example.b07project.repository.ControllerMedicineRepository;
import com.example.b07project.repository.InventoryRepository;
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
import nl.dionsegijn.konfetti.xml.KonfettiView;
import nl.dionsegijn.konfetti.core.Party;
import nl.dionsegijn.konfetti.core.PartyFactory;
import nl.dionsegijn.konfetti.core.emitter.Emitter;
import nl.dionsegijn.konfetti.core.emitter.EmitterConfig;
import nl.dionsegijn.konfetti.core.models.Shape;
import nl.dionsegijn.konfetti.core.models.Size;

import android.widget.ArrayAdapter;
import android.widget.Spinner;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.HashMap;
import java.util.Map;

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
    private Spinner spUserSelector;
    
    private int doseCount = 1;
    private Date timestamp;
    private Date scheduledTime = null;
    private RescueInhalerRepository rescueRepository;
    private ControllerMedicineRepository controllerRepository;
    private InventoryRepository inventoryRepository;
    private MotivationService motivationService;
    private SimpleDateFormat dateFormat;
    private SimpleDateFormat timeFormat;
    private KonfettiView konfettiView;
    
    private Map<String, String> childrenMap = new HashMap<>();
    private List<String> childIds = new ArrayList<>();
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_log_rescue_inhaler);
        
        initializeViews();
        rescueRepository = new RescueInhalerRepository();
        controllerRepository = new ControllerMedicineRepository();
        inventoryRepository = new InventoryRepository(this);
        motivationService = new MotivationService(this);
        dateFormat = new SimpleDateFormat("MMM dd, yyyy 'at' hh:mm a", Locale.getDefault());
        timeFormat = new SimpleDateFormat("hh:mm a", Locale.getDefault());
        
        // Setup badge earned callback
        motivationService.setBadgeEarnedCallback(badge -> {
            runOnUiThread(() -> showBadgeEarnedNotification(badge));
        });
        
        timestamp = new Date();
        updateTimestampDisplay();
        updateDoseCountDisplay();
        
        setupListeners();
        fetchChildren();
    }
    
    private void initializeViews() {
        spUserSelector = findViewById(R.id.spUserSelector);
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

    private void fetchChildren() {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        FirebaseFirestore.getInstance().collection("children")
                .whereEqualTo("parentId", userId)
                .get()
                .addOnSuccessListener(snapshots -> {
                    List<String> names = new ArrayList<>();
                    names.add("Log for: Me (Parent)");
                    childIds.clear();
                    childIds.add(null);
                    
                    for (QueryDocumentSnapshot doc : snapshots) {
                        names.add("Log for: " + doc.getString("name"));
                        childIds.add(doc.getId());
                    }
                    
                    ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, names);
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    spUserSelector.setAdapter(adapter);
                });
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
        
        // Get selected child ID
        String selectedChildId = null;
        if (spUserSelector.getAdapter() != null && !childIds.isEmpty()) {
            selectedChildId = childIds.get(spUserSelector.getSelectedItemPosition());
        }
        final String childIdToUse = selectedChildId;

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
                    // Decrement inventory
                    inventoryRepository.decrementDose(currentUser.getUid(), childIdToUse, "Controller", doseCount, new InventoryRepository.SaveCallback() {
                        @Override
                        public void onSuccess() {
                            // Continue with existing flow
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
                            // Log error but don't stop the flow
                            android.util.Log.e("Inventory", "Failed to decrement: " + error);
                            motivationService.updateControllerStreak(() -> {
                                showLoading(false);
                                Toast.makeText(LogRescueInhalerActivity.this, 
                                    "Controller medicine logged successfully! Streak updated.", 
                                    Toast.LENGTH_LONG).show();
                                finish();
                            });
                        }
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
                    // Decrement inventory
                    inventoryRepository.decrementDose(currentUser.getUid(), childIdToUse, "Rescue", doseCount, new InventoryRepository.SaveCallback() {
                        @Override
                        public void onSuccess() {
                            showLoading(false);
                            // Check test badge after logging
                            motivationService.checkFirstRescueBadge(() -> {
                                // Check low rescue badge after test badge
                                motivationService.checkLowRescueBadge(() -> {
                                    // Delay finish to allow dialog to show
                                });
                            });
                        }
                        @Override
                        public void onFailure(String error) {
                            android.util.Log.e("Inventory", "Failed to decrement: " + error);
                            showLoading(false);
                            motivationService.checkFirstRescueBadge(() -> {
                                motivationService.checkLowRescueBadge(() -> {});
                            });
                        }
                    });
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
    
    private void showBadgeEarnedNotification(Badge badge) {
        android.util.Log.d("ConfettiService", "showBadgeEarnedNotification - Starting");
        
        // Create confetti view programmatically
        ViewGroup rootView = findViewById(android.R.id.content);
        android.util.Log.d("ConfettiService", "Root view: " + rootView);
        
        konfettiView = new KonfettiView(this);
        ConstraintLayout.LayoutParams params = new ConstraintLayout.LayoutParams(
            ConstraintLayout.LayoutParams.MATCH_PARENT,
            ConstraintLayout.LayoutParams.MATCH_PARENT
        );
        konfettiView.setLayoutParams(params);
        konfettiView.setZ(1000f); // Bring to front
        konfettiView.setElevation(1000f); // Ensure it's on top
        konfettiView.bringToFront();
        rootView.addView(konfettiView);
        android.util.Log.d("ConfettiService", "KonfettiView added to root");
        
        // Start confetti animation - Big, fast confetti raining down the full screen
        EmitterConfig emitterConfig = new Emitter(5L, java.util.concurrent.TimeUnit.SECONDS).max(200);
        Party party = new PartyFactory(emitterConfig)
            .angle(90)  // 90 degrees = straight down (0° is right, clockwise)
            .spread(45)  // Spread angle
            .timeToLive(5000L)  // Live for 5 seconds to reach bottom
            .fadeOutEnabled(true)
            .setDamping(0.97f)  // Slow down less (closer to 1.0 = less slowdown)
            .shapes(Shape.Circle.INSTANCE, new Shape.Rectangle(0.5f), Shape.Square.INSTANCE)
            .sizes(new Size(30, 100f, 0.1f))  // Much bigger confetti: 30-100dp
            .position(0.0, 0.0, 1.0, 0.0)  // Spawn across entire top width
            .build();
        
        android.util.Log.d("ConfettiService", "Party created, starting confetti");
        konfettiView.start(party);
        android.util.Log.d("ConfettiService", "Confetti started");
        
        // Show dialog
        android.util.Log.d("ConfettiService", "Showing dialog");
        
        // Create Material Design styled dialog
        AlertDialog dialog = new AlertDialog.Builder(this, com.google.android.material.R.style.ThemeOverlay_Material3_MaterialAlertDialog)
            .setTitle("🎉 Congratulations!")
            .setMessage("You earned a new badge!\n\n" + badge.getName() + "\n" + badge.getDescription())
            .setPositiveButton("Awesome!", (dialogInterface, which) -> {
                android.util.Log.d("ConfettiService", "Awesome button clicked");
                // Remove confetti view
                if (konfettiView != null && konfettiView.getParent() != null) {
                    ((ViewGroup) konfettiView.getParent()).removeView(konfettiView);
                    android.util.Log.d("ConfettiService", "KonfettiView removed");
                }
                Toast.makeText(LogRescueInhalerActivity.this, 
                    "Rescue inhaler use logged successfully!", 
                    Toast.LENGTH_SHORT).show();
                android.util.Log.d("ConfettiService", "Finishing activity");
                finish();
            })
            .setOnDismissListener(dialogInterface -> {
                android.util.Log.d("ConfettiService", "Dialog dismissed");
                // Remove confetti view on dismiss
                if (konfettiView != null && konfettiView.getParent() != null) {
                    ((ViewGroup) konfettiView.getParent()).removeView(konfettiView);
                    android.util.Log.d("ConfettiService", "KonfettiView removed on dismiss");
                }
                Toast.makeText(LogRescueInhalerActivity.this, 
                    "Rescue inhaler use logged successfully!", 
                    Toast.LENGTH_SHORT).show();
                finish();
            })
            .setCancelable(false)
            .create();
        
        // Show and style the dialog
        dialog.show();
        
        // Set rounded corners background
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(R.drawable.dialog_rounded_background);
        }
        
        // Customize button appearance
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(getColor(R.color.primary_blue));
        
        android.util.Log.d("ConfettiService", "Dialog shown");
    }
}
