package com.example.b07project;

import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import java.util.HashMap;
import java.util.Map;

public class SharingSettingsActivity extends AppCompatActivity {

    private String childId;
    private FirebaseFirestore db;
    
    private SwitchMaterial switchRescueLogs;
    private SwitchMaterial switchControllerAdherence;
    private SwitchMaterial switchSymptoms;
    private SwitchMaterial switchTriggers;
    private SwitchMaterial switchPEF;
    private SwitchMaterial switchTriage;
    private SwitchMaterial switchSummaryCharts;
    private Button btnSave;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sharing_settings);

        childId = getIntent().getStringExtra("EXTRA_CHILD_ID");
        if (childId == null) {
            Toast.makeText(this, "Error: No child ID provided", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        db = FirebaseFirestore.getInstance();

        switchRescueLogs = findViewById(R.id.switchRescueLogs);
        switchControllerAdherence = findViewById(R.id.switchControllerAdherence);
        switchSymptoms = findViewById(R.id.switchSymptoms);
        switchTriggers = findViewById(R.id.switchTriggers);
        switchPEF = findViewById(R.id.switchPEF);
        switchTriage = findViewById(R.id.switchTriage);
        switchSummaryCharts = findViewById(R.id.switchSummaryCharts);
        btnSave = findViewById(R.id.btnSave);

        loadSettings();

        btnSave.setOnClickListener(v -> saveSettings());
    }

    private void loadSettings() {
        db.collection("children").document(childId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Map<String, Object> sharing = (Map<String, Object>) documentSnapshot.get("sharingSettings");
                        if (sharing != null) {
                            switchRescueLogs.setChecked(Boolean.TRUE.equals(sharing.get("rescueLogs")));
                            switchControllerAdherence.setChecked(Boolean.TRUE.equals(sharing.get("controllerAdherence")));
                            switchSymptoms.setChecked(Boolean.TRUE.equals(sharing.get("symptoms")));
                            switchTriggers.setChecked(Boolean.TRUE.equals(sharing.get("triggers")));
                            switchPEF.setChecked(Boolean.TRUE.equals(sharing.get("pef")));
                            switchTriage.setChecked(Boolean.TRUE.equals(sharing.get("triage")));
                            switchSummaryCharts.setChecked(Boolean.TRUE.equals(sharing.get("summaryCharts")));
                        } else {
                            // Default to false or true? Requirement says "By default nothing is shared".
                            // So false is correct.
                        }
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to load settings", Toast.LENGTH_SHORT).show());
    }

    private void saveSettings() {
        Map<String, Object> sharing = new HashMap<>();
        sharing.put("rescueLogs", switchRescueLogs.isChecked());
        sharing.put("controllerAdherence", switchControllerAdherence.isChecked());
        sharing.put("symptoms", switchSymptoms.isChecked());
        sharing.put("triggers", switchTriggers.isChecked());
        sharing.put("pef", switchPEF.isChecked());
        sharing.put("triage", switchTriage.isChecked());
        sharing.put("summaryCharts", switchSummaryCharts.isChecked());

        Map<String, Object> update = new HashMap<>();
        update.put("sharingSettings", sharing);

        db.collection("children").document(childId)
                .set(update, SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Settings saved", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to save settings", Toast.LENGTH_SHORT).show());
    }
}
