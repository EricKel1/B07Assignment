package com.example.b07project;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProviderHomeActivity extends AppCompatActivity {

    private RecyclerView rvChildren;
    private ProviderChildrenAdapter adapter;
    private List<Map<String, Object>> childrenList;
    private FirebaseFirestore db;
    private String providerId;
    private TextView tvEmptyState;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_provider_home);

        db = FirebaseFirestore.getInstance();
        providerId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        childrenList = new ArrayList<>();

        TextView tvWelcome = findViewById(R.id.tvWelcome);
        tvWelcome.setText("Provider Dashboard");

        tvEmptyState = findViewById(R.id.tvEmptyState);

        rvChildren = findViewById(R.id.rvChildren);
        rvChildren.setLayoutManager(new LinearLayoutManager(this));
        
        adapter = new ProviderChildrenAdapter(childrenList, child -> {
            Intent intent = new Intent(ProviderHomeActivity.this, ProviderChildDetailActivity.class);
            intent.putExtra("CHILD_ID", (String) child.get("childId"));
            intent.putExtra("CHILD_NAME", (String) child.get("childName"));
            startActivity(intent);
        });
        rvChildren.setAdapter(adapter);

        Button btnSignOut = findViewById(R.id.btnSignOut);
        btnSignOut.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });

        loadLinkedChildren();
    }

    private void loadLinkedChildren() {
        // Query Firestore for children shared with this provider
        db.collection("providerSharing")
                .whereEqualTo("providerId", providerId)
                .addSnapshotListener((querySnapshot, error) -> {
                    if (error != null) {
                        return;
                    }

                    if (querySnapshot != null) {
                        childrenList.clear();
                        for (QueryDocumentSnapshot doc : querySnapshot) {
                            Map<String, Object> sharingData = doc.getData();
                            String childId = (String) sharingData.get("childId");
                            
                            // Fetch child details
                            db.collection("children").document(childId).get()
                                    .addOnSuccessListener(childDoc -> {
                                        if (childDoc.exists()) {
                                            Map<String, Object> childData = new HashMap<>(childDoc.getData());
                                            childData.put("childId", childId);
                                            childData.putAll(sharingData); // Include sharing settings
                                            childrenList.add(childData);
                                            adapter.notifyDataSetChanged();
                                            updateEmptyState();
                                        }
                                    });
                        }
                        // Show empty state if no children shared
                        if (querySnapshot.isEmpty()) {
                            updateEmptyState();
                        }
                    }
                });
    }

    private void updateEmptyState() {
        if (childrenList.isEmpty()) {
            tvEmptyState.setVisibility(View.VISIBLE);
            rvChildren.setVisibility(View.GONE);
        } else {
            tvEmptyState.setVisibility(View.GONE);
            rvChildren.setVisibility(View.VISIBLE);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadLinkedChildren();
    }

    private void loadPatients() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        progressBar.setVisibility(View.VISIBLE);
        tvNoPatients.setVisibility(View.GONE);

        db.collection("users").document(user.getUid()).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        List<String> childIds = (List<String>) documentSnapshot.get("childIds");
                        if (childIds != null && !childIds.isEmpty()) {
                            fetchChildrenDetails(childIds);
                        } else {
                            progressBar.setVisibility(View.GONE);
                            tvNoPatients.setVisibility(View.VISIBLE);
                        }
                    } else {
                        progressBar.setVisibility(View.GONE);
                        tvNoPatients.setVisibility(View.VISIBLE);
                    }
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Failed to load profile: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void fetchChildrenDetails(List<String> childIds) {
        patientsList.clear();
        adapter.notifyDataSetChanged(); // Clear UI immediately
        
        // Simple counter to know when all are fetched
        final int[] completedCount = {0};
        final int total = childIds.size();

        for (String childId : childIds) {
            db.collection("children").document(childId).get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            Map<String, Object> patient = new HashMap<>();
                            patient.put("id", documentSnapshot.getId());
                            patient.put("name", documentSnapshot.getString("name"));
                            // Add other fields if needed
                            patientsList.add(patient);
                        }
                        checkLoadComplete(++completedCount[0], total);
                    })
                    .addOnFailureListener(e -> {
                        checkLoadComplete(++completedCount[0], total);
                    });
        }
    }

    private void checkLoadComplete(int current, int total) {
        if (current == total) {
            progressBar.setVisibility(View.GONE);
            if (patientsList.isEmpty()) {
                tvNoPatients.setVisibility(View.VISIBLE);
            } else {
                adapter.notifyDataSetChanged();
            }
        }
    }

    private void onPatientClick(Map<String, Object> patient) {
        String childId = (String) patient.get("id");
        String childName = (String) patient.get("name");
        
        Intent intent = new Intent(this, HomeActivity.class);
        intent.putExtra("EXTRA_CHILD_ID", childId);
        intent.putExtra("EXTRA_CHILD_NAME", childName);
        startActivity(intent);
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        loadPatients(); // Refresh list when returning from adding a patient
    }
}
