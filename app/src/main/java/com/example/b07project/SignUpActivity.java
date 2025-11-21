package com.example.b07project;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;

public class SignUpActivity extends AppCompatActivity {
    
    private EditText etName, etEmail, etPassword, etConfirmPassword;
    private Button btnSignUp;
    private ProgressBar progress;
    private TextView tvError, tvSignIn;
    
    private FirebaseAuth auth;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);
        
        auth = FirebaseAuth.getInstance();
        
        initializeViews();
        setupListeners();
    }
    
    private void initializeViews() {
        etName = findViewById(R.id.etName);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        btnSignUp = findViewById(R.id.btnSignUp);
        progress = findViewById(R.id.progress);
        tvError = findViewById(R.id.tvError);
        tvSignIn = findViewById(R.id.tvSignIn);
    }
    
    private void setupListeners() {
        btnSignUp.setOnClickListener(v -> attemptSignUp());
        
        tvSignIn.setOnClickListener(v -> {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });
    }
    
    private void attemptSignUp() {
        String name = etName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString();
        String confirmPassword = etConfirmPassword.getText().toString();
        
        if (!validateInput(name, email, password, confirmPassword)) {
            return;
        }
        
        showLoading(true);
        
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this, task -> {
                if (task.isSuccessful()) {
                    FirebaseUser user = auth.getCurrentUser();
                    if (user != null) {
                        UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                            .setDisplayName(name)
                            .build();
                        
                        user.updateProfile(profileUpdates)
                            .addOnCompleteListener(profileTask -> {
                                showLoading(false);
                                if (profileTask.isSuccessful()) {
                                    Toast.makeText(SignUpActivity.this, 
                                        "Account created successfully!", 
                                        Toast.LENGTH_SHORT).show();
                                    navigateToHome();
                                } else {
                                    Toast.makeText(SignUpActivity.this, 
                                        "Account created, but profile update failed", 
                                        Toast.LENGTH_SHORT).show();
                                    navigateToHome();
                                }
                            });
                    }
                   } else {
                    showLoading(false);
                    String errorMessage = "Sign up failed";
                    if (task.getException() != null) {
                        String exceptionMessage = task.getException().getMessage();
                        if (exceptionMessage != null) {
                            if (exceptionMessage.contains("email address is already in use")) {
                                errorMessage = "This email is already registered";
                            } else if (exceptionMessage.contains("network error")) {
                                errorMessage = "Network error. Please check your connection";
                            } else {
                                errorMessage = exceptionMessage;
                            }
                        }
                    }
                    showError(errorMessage);
                }
            });
    }
    
    private boolean validateInput(String name, String email, String password, String confirmPassword) {
        if (TextUtils.isEmpty(name)) {
            showError("Please enter your name");
            etName.requestFocus();
            return false;
        }
        
        if (TextUtils.isEmpty(email)) {
            showError("Please enter your email");
            etEmail.requestFocus();
            return false;
        }
        
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            showError("Please enter a valid email address");
            etEmail.requestFocus();
            return false;
        }
        
        if (TextUtils.isEmpty(password)) {
            showError("Please enter a password");
            etPassword.requestFocus();
            return false;
        }
        
        if (password.length() < 6) {
            showError("Password must be at least 6 characters");
            etPassword.requestFocus();
            return false;
        }
        
        if (TextUtils.isEmpty(confirmPassword)) {
            showError("Please confirm your password");
            etConfirmPassword.requestFocus();
            return false;
        }
        
        if (!password.equals(confirmPassword)) {
            showError("Passwords do not match");
            etConfirmPassword.requestFocus();
            return false;
        }
        
        return true;
    }
    
    private void showLoading(boolean show) {
        progress.setVisibility(show ? View.VISIBLE : View.GONE);
        btnSignUp.setEnabled(!show);
        etName.setEnabled(!show);
        etEmail.setEnabled(!show);
        etPassword.setEnabled(!show);
        etConfirmPassword.setEnabled(!show);
    }
    
    private void showError(String message) {
        tvError.setText(message);
        tvError.setVisibility(View.VISIBLE);
    }
    
    private void navigateToHome() {
        Intent intent = new Intent(this, HomeActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
