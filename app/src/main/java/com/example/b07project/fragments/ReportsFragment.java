package com.example.b07project.fragments;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.b07project.R;
import com.example.b07project.adapters.ReportAdapter;
import com.example.b07project.models.Report;
import com.example.b07project.utils.ReportGenerator;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.List;

public class ReportsFragment extends Fragment {

    private RecyclerView rvReports;
    private TextView tvEmptyState;
    private FloatingActionButton fabCreateReport;
    private ReportAdapter reportAdapter;
    private FirebaseFirestore db;
    private String userId;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_reports, container, false);

        rvReports = view.findViewById(R.id.rvReports);
        tvEmptyState = view.findViewById(R.id.tvEmptyState);
        fabCreateReport = view.findViewById(R.id.fabCreateReport);

        db = FirebaseFirestore.getInstance();
        userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        setupRecyclerView();
        setupListeners();
        loadReports();

        return view;
    }

    private void setupRecyclerView() {
        reportAdapter = new ReportAdapter(new ArrayList<>(), new ReportAdapter.OnReportActionListener() {
            @Override
            public void onShareReport(Report report) {
                shareReport(report);
            }

            @Override
            public void onDownloadReport(Report report) {
                downloadReport(report);
            }
        }, this::confirmDeleteReport);
        rvReports.setLayoutManager(new LinearLayoutManager(getContext()));
        rvReports.setAdapter(reportAdapter);
    }

    private void setupListeners() {
        fabCreateReport.setOnClickListener(v -> showCreateReportDialog());
    }

    private void showCreateReportDialog() {
        LinearLayout layout = new LinearLayout(requireContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 40, 50, 10);

        TextView tvLabel = new TextView(requireContext());
        tvLabel.setText("Select Time Period:");
        layout.addView(tvLabel);

        Spinner spinner = new Spinner(requireContext());
        String[] options = {"7 days", "30 days", "90 days (3 months)", "180 days (6 months)"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, options);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        layout.addView(spinner);

        CheckBox cbTriage = new CheckBox(requireContext());
        cbTriage.setText("Include Notable Triage Incidents");
        cbTriage.setChecked(true);
        layout.addView(cbTriage);

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Create Report")
                .setView(layout)
                .setPositiveButton("Generate", (dialog, which) -> {
                    int days = 7;
                    int position = spinner.getSelectedItemPosition();
                    switch (position) {
                        case 0: days = 7; break;
                        case 1: days = 30; break;
                        case 2: days = 90; break;
                        case 3: days = 180; break;
                    }
                    createReport(days, cbTriage.isChecked());
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void createReport(int days, boolean includeTriage) {
        Toast.makeText(getContext(), "Generating " + days + "-day report...", Toast.LENGTH_SHORT).show();
        
        ReportGenerator generator = new ReportGenerator(getContext());
        generator.generateReport(userId, days, includeTriage, new ReportGenerator.ReportCallback() {
            @Override
            public void onSuccess(Report report) {
                saveReportToFirestore(report);
                Toast.makeText(getContext(), "Report generated successfully!", Toast.LENGTH_SHORT).show();
                loadReports();
            }

            @Override
            public void onFailure(String error) {
                Toast.makeText(getContext(), "Failed to generate report: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void saveReportToFirestore(Report report) {
        db.collection("reports")
                .add(report)
                .addOnSuccessListener(documentReference -> {
                    // Report saved successfully
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Failed to save report", Toast.LENGTH_SHORT).show();
                });
    }

    private void loadReports() {
        android.util.Log.d("RescueServiceChart", "Loading reports for userId: " + userId);
        
        db.collection("reports")
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    android.util.Log.d("RescueServiceChart", "Reports query SUCCESS: Found " + queryDocumentSnapshots.size() + " reports");
                    List<Report> reports = new ArrayList<>();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Report report = document.toObject(Report.class);
                        report.setId(document.getId());
                        reports.add(report);
                        android.util.Log.d("RescueServiceChart", "  - Report: " + report.getDays() + " days, generated: " + report.getGeneratedDate());
                    }

                    if (reports.isEmpty()) {
                        android.util.Log.d("RescueServiceChart", "No reports found, showing empty state");
                        tvEmptyState.setVisibility(View.VISIBLE);
                        rvReports.setVisibility(View.GONE);
                    } else {
                        android.util.Log.d("RescueServiceChart", "Displaying " + reports.size() + " reports");
                        tvEmptyState.setVisibility(View.GONE);
                        rvReports.setVisibility(View.VISIBLE);
                        reportAdapter.updateReports(reports);
                    }
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e("RescueServiceChart", "Reports query FAILED: " + e.getMessage(), e);
                    Toast.makeText(getContext(), "Failed to load reports: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void shareReport(Report report) {
        ReportGenerator generator = new ReportGenerator(getContext());
        generator.shareReport(report);
    }

    private void downloadReport(Report report) {
        ReportGenerator generator = new ReportGenerator(getContext());
        generator.downloadReport(report);
    }

    private void confirmDeleteReport(Report report, int position) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Delete Report?")
                .setMessage("Are you sure you want to delete this report? This cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> deleteReport(report, position))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteReport(Report report, int position) {
        db.collection("reports")
                .document(report.getId())
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), "Report deleted successfully", Toast.LENGTH_SHORT).show();
                    loadReports(); // Refresh the list
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Failed to delete report: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}
