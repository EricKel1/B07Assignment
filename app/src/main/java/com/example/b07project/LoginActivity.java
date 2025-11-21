package com.example.b07project;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.b07project.auth.AuthRepo;
import com.example.b07project.auth.FirebaseAuthRepo;
import com.example.b07project.auth.LoginContract;
import com.example.b07project.auth.LoginPresenter;
import com.example.b07project.main.WelcomeActivity;
import com.google.firebase.auth.FirebaseAuth;

public class LoginActivity extends AppCompatActivity implements LoginContract.View {

    private EditText etEmail, etPassword;
    private ProgressBar progress;
    private TextView tvError, tvSignUp;
    private LoginContract.Presenter presenter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        progress = findViewById(R.id.progress);
        tvError = findViewById(R.id.tvError);
        tvSignUp = findViewById(R.id.tvSignUp);
        Button btnLogin = findViewById(R.id.btnLogin);

        presenter = new LoginPresenter(this, new FirebaseAuthRepo());

        btnLogin.setOnClickListener(v -> presenter.onLoginClicked());

        tvSignUp.setOnClickListener(v -> {
            startActivity(new Intent(this, WelcomeActivity.class));
        });
    }

    @Override
    public void showLoading(boolean show) {
        progress.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    @Override
    public void showError(String message) {
        tvError.setText(message);
        tvError.setVisibility(View.VISIBLE);
    }

    @Override
    public void navigateToHome() {
        tvError.setVisibility(View.GONE);
        startActivity(new Intent(this, HomeActivity.class));
        finish();

    }

    @Override
    public void navigateToProviderHome() {
        tvError.setVisibility(View.GONE);
        startActivity(new Intent(this, ProviderHomeActivity.class));
        finish();
    }

    @Override
    public void navigateToDeviceChooser() {
        tvError.setVisibility(View.GONE);
        startActivity(new Intent(this, DeviceChooserActivity.class));
        finish();

    }

    @Override
    public void showEmailNotVerifiedDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Email not verified")
                .setMessage("Please verify your email using the link we sent before logging in.")
                .setPositiveButton("OK", (dialog, which) -> {
                    FirebaseAuth.getInstance().signOut();
                })
                .setCancelable(false)
                .show();
    }

    @Override
    public String getEmailInput() {
        return etEmail.getText().toString().trim();
    }

    @Override
    public String getPasswordInput() {
        return etPassword.getText().toString();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        presenter.onDestroy();
    }
}
