package com.example.b07project;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import com.example.b07project.models.PEFReading;
import com.example.b07project.models.PersonalBest;
import com.example.b07project.repository.PEFRepository;
import com.google.firebase.auth.FirebaseAuth;

import java.util.Date;

public class PEFEntryActivity extends AppCompatActivity {

    private EditText etPEFValue, etNotes;
    private RadioGroup rgMedicationContext;
    private RadioButton rbNoContext, rbPreMedication, rbPostMedication;
    private CardView cardZoneDisplay, cardPersonalBestInfo;
    private TextView tvZoneResult, tvZonePercentage, tvZoneGuidance;
    private Button btnSave, btnCancel;
    
    private PEFRepository pefRepository;
    private PersonalBest userPersonalBest;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pef_entry);

        pefRepository = new PEFRepository();
        initializeViews();
        loadPersonalBest();
        setupListeners();
    }

    private void initializeViews() {
        etPEFValue = findViewById(R.id.etPEFValue);
        etNotes = findViewById(R.id.etNotes);
        rgMedicationContext = findViewById(R.id.rgMedicationContext);
        rbNoContext = findViewById(R.id.rbNoContext);
        rbPreMedication = findViewById(R.id.rbPreMedication);
        rbPostMedication = findViewById(R.id.rbPostMedication);
        cardZoneDisplay = findViewById(R.id.cardZoneDisplay);
        cardPersonalBestInfo = findViewById(R.id.cardPersonalBestInfo);
        tvZoneResult = findViewById(R.id.tvZoneResult);
        tvZonePercentage = findViewById(R.id.tvZonePercentage);
        tvZoneGuidance = findViewById(R.id.tvZoneGuidance);
        btnSave = findViewById(R.id.btnSave);
        btnCancel = findViewById(R.id.btnCancel);
    }

    private void loadPersonalBest() {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        
        pefRepository.getPersonalBest(userId, new PEFRepository.LoadCallback<PersonalBest>() {
            @Override
            public void onSuccess(PersonalBest pb) {
                userPersonalBest = pb;
                if (pb == null) {
                    cardPersonalBestInfo.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onFailure(String error) {
                userPersonalBest = null;
                cardPersonalBestInfo.setVisibility(View.VISIBLE);
            }
        });
    }

    private void setupListeners() {
        // Real-time zone calculation as user types
        etPEFValue.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateZonePreview();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        btnSave.setOnClickListener(v -> savePEFReading());
        btnCancel.setOnClickListener(v -> finish());
    }

    private void updateZonePreview() {
        String valueStr = etPEFValue.getText().toString().trim();
        
        if (valueStr.isEmpty()) {
            cardZoneDisplay.setVisibility(View.GONE);
            return;
        }

        try {
            int pefValue = Integer.parseInt(valueStr);
            
            if (pefValue <= 0 || pefValue > 800) {
                cardZoneDisplay.setVisibility(View.GONE);
                return;
            }

            if (userPersonalBest != null && userPersonalBest.getValue() > 0) {
                String zone = PersonalBest.calculateZone(pefValue, userPersonalBest.getValue());
                int percentage = (pefValue * 100) / userPersonalBest.getValue();
                
                displayZone(zone, percentage);
                cardZoneDisplay.setVisibility(View.VISIBLE);
            } else {
                cardZoneDisplay.setVisibility(View.GONE);
            }
        } catch (NumberFormatException e) {
            cardZoneDisplay.setVisibility(View.GONE);
        }
    }

    private void displayZone(String zone, int percentage) {
        String zoneLabel = PersonalBest.getZoneLabel(zone);
        int zoneColor = PersonalBest.getZoneColor(zone);
        
        tvZoneResult.setText(zoneLabel);
        tvZoneResult.setTextColor(ContextCompat.getColor(this, zoneColor));
        tvZonePercentage.setText(percentage + "% of your Personal Best");
        
        // Set guidance based on zone
        String guidance;
        switch (zone) {
            case "green":
                guidance = "Your asthma is well controlled. Continue your usual medications.";
                break;
            case "yellow":
                guidance = "Caution: Your asthma may not be well controlled. Follow your action plan or contact your healthcare provider.";
                break;
            case "red":
                guidance = "Medical Alert: This is a low reading. Follow your emergency action plan and seek medical attention if needed.";
                break;
            default:
                guidance = "";
        }
        tvZoneGuidance.setText(guidance);
    }

    private void savePEFReading() {
        String valueStr = etPEFValue.getText().toString().trim();
        
        if (valueStr.isEmpty()) {
            Toast.makeText(this, "Please enter a peak flow value", Toast.LENGTH_SHORT).show();
            return;
        }

        int pefValue;
        try {
            pefValue = Integer.parseInt(valueStr);
            
            if (pefValue <= 0 || pefValue > 800) {
                Toast.makeText(this, "Please enter a valid peak flow value (1-800 L/min)", Toast.LENGTH_SHORT).show();
                return;
            }
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Please enter a valid number", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        String notes = etNotes.getText().toString().trim();
        
        boolean isPreMed = rbPreMedication.isChecked();
        boolean isPostMed = rbPostMedication.isChecked();

        PEFReading reading = new PEFReading(userId, pefValue, isPreMed, isPostMed, notes);

        btnSave.setEnabled(false);
        btnSave.setText("Saving...");

        pefRepository.savePEFReading(reading, new PEFRepository.SaveCallback() {
            @Override
            public void onSuccess(String documentId) {
                Toast.makeText(PEFEntryActivity.this, "Peak flow reading saved", Toast.LENGTH_SHORT).show();
                finish();
            }

            @Override
            public void onFailure(String error) {
                Toast.makeText(PEFEntryActivity.this, "Error: " + error, Toast.LENGTH_SHORT).show();
                btnSave.setEnabled(true);
                btnSave.setText("Save Peak Flow Reading");
            }
        });
    }
}
