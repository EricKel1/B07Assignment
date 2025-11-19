package com.example.b07project.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.b07project.R;
import com.example.b07project.repository.TriggerAnalyticsRepository.TriggerStats;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class TriggerStatsAdapter extends RecyclerView.Adapter<TriggerStatsAdapter.ViewHolder> {

    private List<TriggerStats> stats;

    public TriggerStatsAdapter() {
        this.stats = new ArrayList<>();
    }

    public void setStats(List<TriggerStats> stats) {
        this.stats = stats;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_trigger_stat, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        TriggerStats stat = stats.get(position);

        // Capitalize first letter
        String displayName = stat.triggerName.substring(0, 1).toUpperCase() + stat.triggerName.substring(1);
        holder.tvTriggerName.setText(displayName);
        holder.tvTriggerCount.setText(String.valueOf(stat.totalCount));

        // Build details text
        StringBuilder details = new StringBuilder();
        details.append(stat.totalCount).append(stat.totalCount == 1 ? " occurrence" : " occurrences");
        
        if (stat.symptomCheckInCount > 0 && stat.averageSeverity > 0) {
            details.append(" • Avg severity: ").append(String.format(Locale.getDefault(), "%.1f", stat.averageSeverity));
        }
        
        if (stat.medicineUseCount > 0) {
            details.append(" • ").append(stat.medicineUseCount).append(" medicine uses");
        }

        holder.tvTriggerDetails.setText(details.toString());

        // Color code by severity if available
        if (stat.averageSeverity > 0) {
            int color;
            if (stat.averageSeverity >= 4) {
                color = 0xFFF44336; // Red
            } else if (stat.averageSeverity >= 3) {
                color = 0xFFFF9800; // Orange
            } else {
                color = 0xFF4CAF50; // Green
            }
            holder.tvTriggerCount.setTextColor(color);
        }
    }

    @Override
    public int getItemCount() {
        return stats.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTriggerName, tvTriggerDetails, tvTriggerCount;

        ViewHolder(View itemView) {
            super(itemView);
            tvTriggerName = itemView.findViewById(R.id.tvTriggerName);
            tvTriggerDetails = itemView.findViewById(R.id.tvTriggerDetails);
            tvTriggerCount = itemView.findViewById(R.id.tvTriggerCount);
        }
    }
}
