package com.example.b07project;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

public class ConfirmFragment extends Fragment {

    private ParentSignupViewModel viewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_confirm, container, false);

        viewModel = new ViewModelProvider(requireActivity()).get(ParentSignupViewModel.class);

        TextView tvParentName = view.findViewById(R.id.tvParentName);
        TextView tvParentEmail = view.findViewById(R.id.tvParentEmail);
        LinearLayout llChildrenList = view.findViewById(R.id.llChildrenList);

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

        view.findViewById(R.id.btnCreateAccount).setOnClickListener(v -> {
            Toast.makeText(getContext(), "Will create account next", Toast.LENGTH_SHORT).show();
        });

        view.findViewById(R.id.btnBack).setOnClickListener(v -> {
            requireActivity().getSupportFragmentManager().popBackStack();
        });

        return view;
    }
}
