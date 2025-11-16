package com.example.b07project;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.NumberPicker;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

public class ChildrenCountFragment extends Fragment {

    private ParentSignupViewModel viewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_children_count, container, false);

        viewModel = new ViewModelProvider(requireActivity()).get(ParentSignupViewModel.class);

        NumberPicker npChildrenCount = view.findViewById(R.id.npChildrenCount);
        npChildrenCount.setMinValue(0);
        npChildrenCount.setMaxValue(3);

        view.findViewById(R.id.btnNext).setOnClickListener(v -> {
            int count = npChildrenCount.getValue();
            viewModel.childrenCount.setValue(count);
            viewModel.children.clear(); // Clear any previously added children

            if (count > 0) {
                // If there are children to add, go to the first child details screen
                Bundle args = new Bundle();
                args.putInt("childNumber", 1);
                args.putInt("totalChildren", count);
                ChildDetailsFragment childDetailsFragment = new ChildDetailsFragment();
                childDetailsFragment.setArguments(args);

                requireActivity().getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container_view, childDetailsFragment)
                        .addToBackStack(null)
                        .commit();
            } else {
                // If no children, skip to the confirmation screen
                requireActivity().getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container_view, new ConfirmFragment())
                        .addToBackStack(null)
                        .commit();
            }
        });

        view.findViewById(R.id.btnBack).setOnClickListener(v -> {
            requireActivity().getSupportFragmentManager().popBackStack();
        });

        return view;
    }
}
