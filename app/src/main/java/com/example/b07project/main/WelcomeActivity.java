package com.example.b07project.main;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

import com.example.b07project.HomeActivity;
import com.example.b07project.LoginActivity;
import com.example.b07project.ParentSignupActivity;
import com.example.b07project.ProviderHomeActivity;
import com.example.b07project.ProviderSignupActivity;
import com.example.b07project.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import android.content.SharedPreferences;

public class WelcomeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Check if user is signed in (non-null) and update UI accordingly.
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
            FirebaseFirestore.getInstance().collection("users").document(uid).get()
                .addOnSuccessListener(documentSnapshot -> {
                    String role = documentSnapshot.getString("role");
                    if ("provider".equals(role)) {
                        startActivity(new Intent(WelcomeActivity.this, ProviderHomeActivity.class));
                    } else if ("parent".equals(role)) {
                        // Check preferences for last selected profile
                        SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
                        String lastRole = prefs.getString("last_role", null);
                        
                        if ("parent".equals(lastRole)) {
                            startActivity(new Intent(WelcomeActivity.this, com.example.b07project.ParentDashboardActivity.class));
                        } else if ("child".equals(lastRole)) {
                            String childId = prefs.getString("last_child_id", null);
                            String childName = prefs.getString("last_child_name", null);
                            if (childId != null) {
                                Intent intent = new Intent(WelcomeActivity.this, HomeActivity.class);
                                intent.putExtra("EXTRA_CHILD_ID", childId);
                                intent.putExtra("EXTRA_CHILD_NAME", childName);
                                startActivity(intent);
                            } else {
                                startActivity(new Intent(WelcomeActivity.this, com.example.b07project.DeviceChooserActivity.class));
                            }
                        } else {
                            startActivity(new Intent(WelcomeActivity.this, com.example.b07project.DeviceChooserActivity.class));
                        }
                    } else {
                        startActivity(new Intent(WelcomeActivity.this, HomeActivity.class));
                    }
                    finish();
                })
                .addOnFailureListener(e -> {
                    // Fallback to HomeActivity or stay here?
                    // For now, fallback to HomeActivity
                    startActivity(new Intent(WelcomeActivity.this, HomeActivity.class));
                    finish();
                });
            return;
        }

        // If no user is signed in, show the welcome layout
        setContentView(R.layout.activity_welcome);

        // Set up button listeners
        findViewById(R.id.btnJoinAsParent).setOnClickListener(v -> {
            startActivity(new Intent(WelcomeActivity.this, ParentSignupActivity.class));
        });

        findViewById(R.id.btnJoinAsProvider).setOnClickListener(v -> {
            startActivity(new Intent(WelcomeActivity.this, ProviderSignupActivity.class));
        });

        findViewById(R.id.tvLogin).setOnClickListener(v -> {
            startActivity(new Intent(WelcomeActivity.this, LoginActivity.class));
        });
    }
}
