package com.example.b07project.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.b07project.R;
import com.example.b07project.models.Report;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ReportAdapter extends RecyclerView.Adapter<ReportAdapter.ReportViewHolder> {

    private List<Report> reports;
    private OnReportClickListener listener;
    private OnReportDeleteListener deleteListener;
    private SimpleDateFormat dateFormat;

    public interface OnReportClickListener {
        void onReportClick(Report report);
    }

    public interface OnReportDeleteListener {
        void onReportDelete(Report report, int position);
    }

    public ReportAdapter(List<Report> reports, OnReportClickListener listener, OnReportDeleteListener deleteListener) {
        this.reports = reports;
        this.listener = listener;
        this.deleteListener = deleteListener;
        this.dateFormat = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());
    }

    @NonNull
    @Override
    public ReportViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_report, parent, false);
        return new ReportViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ReportViewHolder holder, int position) {
        Report report = reports.get(position);
        
        holder.tvReportTitle.setText("Usage report - " + report.getDays() + " days");
        holder.tvReportDate.setText("Generated on " + dateFormat.format(new Date(report.getGeneratedDate())));
        
        holder.btnViewReport.setOnClickListener(v -> {
            if (listener != null) {
                listener.onReportClick(report);
            }
        });

        holder.btnDeleteReport.setOnClickListener(v -> {
            if (deleteListener != null) {
                deleteListener.onReportDelete(report, holder.getAdapterPosition());
            }
        });
    }

    @Override
    public int getItemCount() {
        return reports.size();
    }

    public void updateReports(List<Report> newReports) {
        this.reports = newReports;
        notifyDataSetChanged();
    }

    static class ReportViewHolder extends RecyclerView.ViewHolder {
        TextView tvReportTitle;
        TextView tvReportDate;
        Button btnViewReport;
        Button btnDeleteReport;

        ReportViewHolder(View itemView) {
            super(itemView);
            tvReportTitle = itemView.findViewById(R.id.tvReportTitle);
            tvReportDate = itemView.findViewById(R.id.tvReportDate);
            btnViewReport = itemView.findViewById(R.id.btnViewReport);
            btnDeleteReport = itemView.findViewById(R.id.btnDeleteReport);
        }
    }
}
