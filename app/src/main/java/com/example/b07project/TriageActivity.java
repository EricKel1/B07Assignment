package com.example.b07project;

import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import com.example.b07project.models.PersonalBest;
import com.example.b07project.models.TriageSession;
import com.example.b07project.repository.PEFRepository;
import com.example.b07project.utils.NotificationHelper;

public class TriageActivity extends AppCompatActivity {

    private CheckBox cbCantSpeak, cbChestRetractions, cbBlueLips;
    private EditText etRescueAttempts, etCurrentPEF;
    private Button btnGetGuidance, btnCancel;
    private Button btnFeelingBetter, btnNotBetter, btnEmergencyNow;
    private CardView cardDecision;
    private TextView tvDecisionTitle, tvDecisionGuidance, tvActionSteps;
    private LinearLayout layoutTimer, layoutResponseButtons;
    private TextView tvTimerDisplay;

    private TriageRepository triageRepository;
    private PEFRepository pefRepository;
    private TriageSession currentSession;
    private CountDownTimer countDownTimer;
    private static final long TIMER_DURATION_MS = 10 * 60 * 1000; // 10 minutes

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_triage);

        triageRepository = new TriageRepository();
        pefRepository = new PEFRepository();
        
        initializeViews();
        setupListeners();
    }

    private void initializeViews() {
        cbCantSpeak = findViewById(R.id.cbCantSpeak);
        cbChestRetractions = findViewById(R.id.cbChestRetractions);
        cbBlueLips = findViewById(R.id.cbBlueLips);
        etRescueAttempts = findViewById(R.id.etRescueAttempts);
        etCurrentPEF = findViewById(R.id.etCurrentPEF);
        btnGetGuidance = findViewById(R.id.btnGetGuidance);
        btnCancel = findViewById(R.id.btnCancel);
        cardDecision = findViewById(R.id.cardDecision);
        tvDecisionTitle = findViewById(R.id.tvDecisionTitle);
        tvDecisionGuidance = findViewById(R.id.tvDecisionGuidance);
        tvActionSteps = findViewById(R.id.tvActionSteps);
        layoutTimer = findViewById(R.id.layoutTimer);
        layoutResponseButtons = findViewById(R.id.layoutResponseButtons);
        tvTimerDisplay = findViewById(R.id.tvTimerDisplay);
        btnFeelingBetter = findViewById(R.id.btnFeelingBetter);
        btnNotBetter = findViewById(R.id.btnNotBetter);
        btnEmergencyNow = findViewById(R.id.btnEmergencyNow);
    }

    private void setupListeners() {
        btnGetGuidance.setOnClickListener(v -> performTriage());
        btnCancel.setOnClickListener(v -> finish());
        
        btnFeelingBetter.setOnClickListener(v -> handleUserImproved());
        btnNotBetter.setOnClickListener(v -> handleStillTrouble());
        btnEmergencyNow.setOnClickListener(v -> escalateToEmergency());
    }

    private void performTriage() {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        
        // Gather data
        boolean cantSpeak = cbCantSpeak.isChecked();
        boolean chestRetractions = cbChestRetractions.isChecked();
        boolean blueLips = cbBlueLips.isChecked();
        
        String rescueStr = etRescueAttempts.getText().toString().trim();
        int rescueAttempts = rescueStr.isEmpty() ? 0 : Integer.parseInt(rescueStr);
        
        String pefStr = etCurrentPEF.getText().toString().trim();
        Integer currentPEF = pefStr.isEmpty() ? null : Integer.parseInt(pefStr);
        
        // Create session
        currentSession = new TriageSession();
        currentSession.setUserId(userId);
        currentSession.setStartTime(new Date());
        currentSession.setCantSpeakFullSentences(cantSpeak);
        currentSession.setChestPullingInRetractions(chestRetractions);
        currentSession.setBlueGrayLipsNails(blueLips);
        currentSession.setRescueAttemptsLast3Hours(rescueAttempts);
        currentSession.setCurrentPEF(currentPEF);
        
        // Get zone if PEF provided
        if (currentPEF != null) {
            pefRepository.getPersonalBest(userId, new PEFRepository.LoadCallback<PersonalBest>() {
                @Override
                public void onSuccess(PersonalBest pb) {
                    if (pb != null) {
                        String zone = PersonalBest.calculateZone(currentPEF, pb.getValue());
                        currentSession.setCurrentZone(zone);
                    }
                    makeTriageDecision();
                }

                @Override
                public void onFailure(String error) {
                    makeTriageDecision();
                }
            });
        } else {
            makeTriageDecision();
        }
    }

    private void makeTriageDecision() {
        // Check for critical red flags
        boolean hasCriticalFlags = currentSession.hasCriticalFlags();
        boolean highRescueUse = currentSession.getRescueAttemptsLast3Hours() >= 3;
        boolean redZone = "red".equals(currentSession.getCurrentZone());
        
        if (hasCriticalFlags) {
            // EMERGENCY: Any red flag = immediate emergency
            showEmergencyDecision();
        } else if (highRescueUse || redZone) {
            // MONITOR: High rescue use or red zone = home monitoring with timer
            showHomeMonitoringDecision();
        } else {
            // HOME STEPS: Low risk = home management
            showHomeStepsDecision();
        }
        
        // Save session
        triageRepository.saveTriageSession(currentSession, new TriageRepository.SaveCallback() {
            @Override
            public void onSuccess(String documentId) {
                currentSession.setId(documentId);
                
                // Send Parent Alert
                String alertTitle = "Triage Started";
                String alertMessage = "A triage session has been started. Status: " + currentSession.getDecision().replace("_", " ").toUpperCase();
                NotificationHelper.sendAlert(TriageActivity.this, currentSession.getUserId(), alertTitle, alertMessage);
            }

            @Override
            public void onFailure(String error) {
                Toast.makeText(TriageActivity.this, "Error saving session: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showEmergencyDecision() {
        currentSession.setDecision("emergency");
        
        List<String> steps = new ArrayList<>();
        steps.add("1. Call 911 immediately");
        steps.add("2. Take your rescue inhaler while waiting");
        steps.add("3. Sit upright and try to stay calm");
        steps.add("4. If you have oxygen, use it");
        
        currentSession.setGuidanceShown("Call 911 or go to the emergency room immediately.");
        currentSession.setActionSteps(steps);
        
        tvDecisionTitle.setText("🚨 EMERGENCY");
        tvDecisionTitle.setTextColor(getColor(android.R.color.holo_red_dark));
        tvDecisionGuidance.setText("You have emergency warning signs. Call 911 or go to the emergency room NOW.");
        
        StringBuilder stepsText = new StringBuilder();
        for (String step : steps) {
            stepsText.append(step).append("\n");
        }
        tvActionSteps.setText(stepsText.toString().trim());
        
        layoutTimer.setVisibility(View.GONE);
        layoutResponseButtons.setVisibility(View.GONE);
        
        cardDecision.setVisibility(View.VISIBLE);
        btnGetGuidance.setVisibility(View.GONE);
    }

    private void showHomeMonitoringDecision() {
        currentSession.setDecision("home_steps");
        
        List<String> steps = new ArrayList<>();
        steps.add("1. Take your rescue inhaler (2-4 puffs)");
        steps.add("2. Sit upright and breathe slowly");
        steps.add("3. We'll check in after 10 minutes");
        steps.add("4. If you feel worse, call 911");
        
        currentSession.setGuidanceShown("Use your rescue inhaler and monitor symptoms closely.");
        currentSession.setActionSteps(steps);
        currentSession.setTimerStarted(true);
        currentSession.setTimerEndTime(new Date(System.currentTimeMillis() + TIMER_DURATION_MS));
        
        tvDecisionTitle.setText("⚠️ MONITOR AT HOME");
        tvDecisionTitle.setTextColor(getColor(android.R.color.holo_orange_dark));
        tvDecisionGuidance.setText("Your symptoms need close monitoring. Follow these steps:");
        
        StringBuilder stepsText = new StringBuilder();
        for (String step : steps) {
            stepsText.append(step).append("\n");
        }
        tvActionSteps.setText(stepsText.toString().trim());
        
        layoutTimer.setVisibility(View.VISIBLE);
        startCountdownTimer();
        
        cardDecision.setVisibility(View.VISIBLE);
        btnGetGuidance.setVisibility(View.GONE);
    }

    private void showHomeStepsDecision() {
        currentSession.setDecision("monitor");
        
        List<String> steps = new ArrayList<>();
        steps.add("1. Take your rescue inhaler if needed");
        steps.add("2. Rest in a comfortable position");
        steps.add("3. Avoid triggers if known");
        steps.add("4. Monitor your symptoms");
        steps.add("5. Contact your doctor if symptoms persist");
        
        currentSession.setGuidanceShown("Follow your asthma action plan and monitor symptoms.");
        currentSession.setActionSteps(steps);
        
        tvDecisionTitle.setText("🏠 HOME MANAGEMENT");
        tvDecisionTitle.setTextColor(getColor(android.R.color.holo_blue_dark));
        tvDecisionGuidance.setText("Your symptoms can likely be managed at home. Follow these steps:");
        
        StringBuilder stepsText = new StringBuilder();
        for (String step : steps) {
            stepsText.append(step).append("\n");
        }
        tvActionSteps.setText(stepsText.toString().trim());
        
        layoutTimer.setVisibility(View.GONE);
        layoutResponseButtons.setVisibility(View.VISIBLE);
        
        cardDecision.setVisibility(View.VISIBLE);
        btnGetGuidance.setVisibility(View.GONE);
    }

    private void startCountdownTimer() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
        
        countDownTimer = new CountDownTimer(TIMER_DURATION_MS, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                long minutes = millisUntilFinished / 60000;
                long seconds = (millisUntilFinished % 60000) / 1000;
                tvTimerDisplay.setText(String.format("%02d:%02d", minutes, seconds));
            }

            @Override
            public void onFinish() {
                tvTimerDisplay.setText("00:00");
                layoutTimer.setVisibility(View.GONE);
                layoutResponseButtons.setVisibility(View.VISIBLE);
                Toast.makeText(TriageActivity.this, "Time's up! How are you feeling?", Toast.LENGTH_LONG).show();
            }
        };
        
        countDownTimer.start();
    }

    private void handleUserImproved() {
        currentSession.setUserImproved(true);
        currentSession.setUserResponse("improved");
        currentSession.setResponseTime(new Date());
        
        triageRepository.saveTriageSession(currentSession, null);
        
        Toast.makeText(this, "Great! Continue monitoring your symptoms.", Toast.LENGTH_LONG).show();
        finish();
    }

    private void handleStillTrouble() {
        currentSession.setUserImproved(false);
        currentSession.setUserResponse("still_trouble");
        currentSession.setResponseTime(new Date());
        
        escalateToEmergency();
    }

    private void escalateToEmergency() {
        currentSession.setEscalated(true);
        currentSession.setEscalationReason("User reported continued difficulty or requested emergency help");
        currentSession.setDecision("emergency");
        
        triageRepository.saveTriageSession(currentSession, null);
        
        // Send Parent Alert
        NotificationHelper.sendAlert(this, currentSession.getUserId(), "Triage Escalation", "Emergency assistance requested during triage.");
        
        // Show emergency guidance
        showEmergencyDecision();
        
        Toast.makeText(this, "Escalating to emergency guidance", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
    }
}
