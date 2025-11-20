package com.example.b07project;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.example.b07project.auth.*;

public class LoginActivity extends AppCompatActivity implements LoginContract.View {

    private EditText etEmail, etPassword;
    private ProgressBar progress;
    private TextView tvError, tvSignUp;
    private LoginContract.Presenter presenter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        etEmail   = findViewById(R.id.etEmail);
        etPassword= findViewById(R.id.etPassword);
        progress  = findViewById(R.id.progress);
        tvError   = findViewById(R.id.tvError);
        tvSignUp  = findViewById(R.id.tvSignUp);
        Button btnLogin = findViewById(R.id.btnLogin);

        presenter = new LoginPresenter(this, new FirebaseAuthRepo());

        btnLogin.setOnClickListener(v -> presenter.onLoginClicked());
        
        tvSignUp.setOnClickListener(v -> {
            startActivity(new Intent(this, SignUpActivity.class));
        });
    }

    // --- View impl ---
    @Override public void showLoading(boolean show) { progress.setVisibility(show ? View.VISIBLE : View.GONE); }

    @Override public void showError(String message) {
        tvError.setText(message);
        tvError.setVisibility(View.VISIBLE);
    }

    @Override public void navigateToHome() {
        tvError.setVisibility(View.GONE);
        startActivity(new Intent(this, HomeActivity.class));
        finish();
    }

    @Override public void navigateToProviderHome() {
        tvError.setVisibility(View.GONE);
        startActivity(new Intent(this, ProviderHomeActivity.class));
        finish();
    }

    @Override public String getEmailInput() { return etEmail.getText().toString().trim(); }
    @Override public String getPasswordInput() { return etPassword.getText().toString(); }

    @Override protected void onDestroy() {
        super.onDestroy();
        presenter.onDestroy();
    }
}
