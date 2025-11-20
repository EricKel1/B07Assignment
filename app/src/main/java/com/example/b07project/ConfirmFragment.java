package com.example.b07project;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.b07project.main.WelcomeActivity;
import com.example.b07project.models.ChildDraft;
import com.google.firebase.auth.FirebaseAuth;

public class ConfirmFragment extends Fragment {

    private ParentSignupViewModel viewModel;
    private ProgressBar progressBar;
    private Button btnCreateAccount;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_confirm, container, false);

        viewModel = new ViewModelProvider(requireActivity()).get(ParentSignupViewModel.class);

        TextView tvParentName = view.findViewById(R.id.tvParentName);
        TextView tvParentEmail = view.findViewById(R.id.tvParentEmail);
        LinearLayout llChildrenList = view.findViewById(R.id.llChildrenList);
        progressBar = view.findViewById(R.id.progressBar);
        btnCreateAccount = view.findViewById(R.id.btnCreateAccount);

        tvParentName.setText(viewModel.parentName.getValue());
        tvParentEmail.setText(viewModel.email.getValue());

        if (viewModel.children.isEmpty()) {
            TextView noChildrenView = new TextView(getContext());
            noChildrenView.setText("No children added.");
            llChildrenList.addView(noChildrenView);
        } else {
            for (ChildDraft child : viewModel.children) {
                TextView childView = new TextView(getContext());
                childView.setText(child.name + " - " + child.dob);
                childView.setTextSize(16);
                llChildrenList.addView(childView);
            }
        }

        btnCreateAccount.setOnClickListener(v -> {
            btnCreateAccount.setEnabled(false);
            viewModel.createParentAccount();
        });

        view.findViewById(R.id.btnBack).setOnClickListener(v -> {
            requireActivity().getSupportFragmentManager().popBackStack();
        });

        observeViewModel();

        return view;
    }

    private void observeViewModel() {
        viewModel.getSignupState().observe(getViewLifecycleOwner(), state -> {
            switch (state) {
                case LOADING:
                    progressBar.setVisibility(View.VISIBLE);
                    btnCreateAccount.setEnabled(false);
                    break;
                case SUCCESS:
                    progressBar.setVisibility(View.GONE);
                    showSuccessDialog();
                    break;
                case ERROR:
                    progressBar.setVisibility(View.GONE);
                    btnCreateAccount.setEnabled(true);
                    Toast.makeText(getContext(), "Error: " + viewModel.getErrorMessage().getValue(), Toast.LENGTH_LONG).show();
                    break;
                default:
                    progressBar.setVisibility(View.GONE);
                    btnCreateAccount.setEnabled(true);
                    break;
            }
        });
    }

    private void showSuccessDialog() {
        new AlertDialog.Builder(getContext())
                .setTitle("Verify your email")
                .setMessage("We\'ve sent a verification link to " + viewModel.email.getValue() + ". Please check your inbox and verify your email before logging in.")
                .setPositiveButton("OK", (dialog, which) -> {
                    FirebaseAuth.getInstance().signOut();
                    Intent intent = new Intent(getActivity(), WelcomeActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                })
                .setCancelable(false)
                .show();
    }
}
