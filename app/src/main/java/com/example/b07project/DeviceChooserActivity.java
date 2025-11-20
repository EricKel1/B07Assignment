package com.example.b07project;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.b07project.main.WelcomeActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

public class DeviceChooserActivity extends AppCompatActivity {

    private ProgressBar progressBar;
    private Button btnParent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_chooser);

        progressBar = findViewById(R.id.progressBar);
        btnParent = findViewById(R.id.btnParent);

        btnParent.setVisibility(View.GONE);
        progressBar.setVisibility(View.VISIBLE);

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            // This should not happen if the login flow is correct
            navigateToLogin();
            return;
        }

        String uid = currentUser.getUid();
        DocumentReference userDocRef = FirebaseFirestore.getInstance().collection("users").document(uid);

        userDocRef.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                String displayName = documentSnapshot.getString("displayName");
                if (displayName == null || displayName.isEmpty()) {
                    // Fallback to "name" field if "displayName" does not exist
                    displayName = documentSnapshot.getString("name");
                }
                if (displayName == null || displayName.isEmpty()) {
                    // Fallback to email if no name is present
                    displayName = currentUser.getEmail();
                }

                btnParent.setText(getString(R.string.im_the_parent, displayName));
                progressBar.setVisibility(View.GONE);
                btnParent.setVisibility(View.VISIBLE);
            } else {
                Toast.makeText(this, "User data not found.", Toast.LENGTH_SHORT).show();
                logout();
            }
        }).addOnFailureListener(e -> {
            Toast.makeText(this, "Failed to load user data.", Toast.LENGTH_SHORT).show();
            logout();
        });

        btnParent.setOnClickListener(v -> {
            startActivity(new Intent(DeviceChooserActivity.this, ParentDashboardActivity.class));
        });
    }

    private void logout() {
        FirebaseAuth.getInstance().signOut();
        navigateToLogin();
    }

    private void navigateToLogin() {
        Intent intent = new Intent(DeviceChooserActivity.this, WelcomeActivity.class); // Or your main login entry point
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
