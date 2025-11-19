package com.example.b07project;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.Objects;

public class ParentInfoFragment extends Fragment {

    private ParentSignupViewModel viewModel;
    private TextInputLayout tilFullName, tilEmail, tilPassword;
    private TextInputEditText tietFullName, tietEmail, tietPassword;
    private MaterialButton btnNext;
    private MaterialButton btnBack;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_parent_info, container, false);

        viewModel = new ViewModelProvider(requireActivity()).get(ParentSignupViewModel.class);

        tilFullName = view.findViewById(R.id.tilFullName);
        tilEmail = view.findViewById(R.id.tilEmail);
        tilPassword = view.findViewById(R.id.tilPassword);
        tietFullName = view.findViewById(R.id.tietFullName);
        tietEmail = view.findViewById(R.id.tietEmail);
        tietPassword = view.findViewById(R.id.tietPassword);
        btnNext = view.findViewById(R.id.btnNext);
        btnBack = view.findViewById(R.id.btnBack);
        btnNext.setEnabled(false); // Disable button initially

        TextWatcher textWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                validateInputs();
            }
        };

        tietFullName.addTextChangedListener(textWatcher);
        tietEmail.addTextChangedListener(textWatcher);
        tietPassword.addTextChangedListener(textWatcher);

        btnNext.setOnClickListener(v -> {
            viewModel.parentName.setValue(tietFullName.getText().toString());
            viewModel.email.setValue(tietEmail.getText().toString());
            viewModel.password.setValue(tietPassword.getText().toString());

            requireActivity().getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container_view, new ChildrenCountFragment())
                    .addToBackStack(null)
                    .commit();
        });

        btnBack.setOnClickListener(v -> {
            // Finish the activity to go back to the welcome screen
            requireActivity().finish();
        });

        return view;
    }

    private void validateInputs() {
        String fullName = Objects.requireNonNull(tietFullName.getText()).toString().trim();
        String email = Objects.requireNonNull(tietEmail.getText()).toString().trim();
        String password = Objects.requireNonNull(tietPassword.getText()).toString().trim();

        boolean isFullNameValid = !fullName.isEmpty();
        if (!isFullNameValid) {
            tilFullName.setError("Full name cannot be empty");
        } else {
            tilFullName.setError(null);
        }

        boolean isEmailValid = Patterns.EMAIL_ADDRESS.matcher(email).matches();
        if (!isEmailValid) {
            tilEmail.setError("Please enter a valid email address");
        } else {
            tilEmail.setError(null);
        }

        boolean isPasswordValid = password.length() >= 8;
        if (!isPasswordValid) {
            tilPassword.setError("Password must be at least 8 characters");
        } else {
            tilPassword.setError(null);
        }

        btnNext.setEnabled(isFullNameValid && isEmailValid && isPasswordValid);
    }
}
