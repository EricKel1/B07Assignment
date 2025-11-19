package com.example.b07project.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.b07project.R;
import com.example.b07project.models.ControllerMedicineLog;
import com.example.b07project.models.MedicineLog;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MedicineLogAdapter extends RecyclerView.Adapter<MedicineLogAdapter.ViewHolder> {
    
    private List<? extends MedicineLog> logs;
    private SimpleDateFormat dateFormat;
    private SimpleDateFormat timeFormat;
    private boolean isControllerMedicine;
    
    public MedicineLogAdapter() {
        this.logs = new ArrayList<>();
        this.dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
        this.timeFormat = new SimpleDateFormat("hh:mm a", Locale.getDefault());
    }
    
    public void setLogs(List<? extends MedicineLog> logs, boolean isControllerMedicine) {
        this.logs = logs;
        this.isControllerMedicine = isControllerMedicine;
        notifyDataSetChanged();
    }
    
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_rescue_inhaler_log, parent, false);
        return new ViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        MedicineLog log = logs.get(position);
        
        String doseText = log.getDoseCount() == 1 ? "1 puff" : log.getDoseCount() + " puffs";
        holder.tvDoseCount.setText(doseText);
        
        if (log.getTimestamp() != null) {
            holder.tvDate.setText(dateFormat.format(log.getTimestamp()));
            holder.tvTime.setText(timeFormat.format(log.getTimestamp()));
        }
        
        // Show controller-specific info if applicable
        if (isControllerMedicine && log instanceof ControllerMedicineLog) {
            ControllerMedicineLog controllerLog = (ControllerMedicineLog) log;
            if (controllerLog.getScheduledTime() != null) {
                String scheduledInfo = "Scheduled: " + timeFormat.format(controllerLog.getScheduledTime());
                if (controllerLog.isTakenOnTime()) {
                    scheduledInfo += " ✓ On Time";
                } else {
                    scheduledInfo += " ⚠ Late";
                }
                holder.tvScheduledInfo.setText(scheduledInfo);
                holder.tvScheduledInfo.setVisibility(View.VISIBLE);
            } else {
                holder.tvScheduledInfo.setVisibility(View.GONE);
            }
        } else {
            holder.tvScheduledInfo.setVisibility(View.GONE);
        }
        
        // Display triggers
        if (log.getTriggers() != null && !log.getTriggers().isEmpty()) {
            holder.chipGroupTriggers.removeAllViews();
            for (String trigger : log.getTriggers()) {
                Chip chip = new Chip(holder.itemView.getContext());
                chip.setText(trigger);
                chip.setChipBackgroundColorResource(R.color.trigger_chip);
                chip.setTextColor(holder.itemView.getContext().getColor(android.R.color.white));
                chip.setClickable(false);
                holder.chipGroupTriggers.addView(chip);
            }
            holder.chipGroupTriggers.setVisibility(View.VISIBLE);
        } else {
            holder.chipGroupTriggers.setVisibility(View.GONE);
        }
        
        if (log.getNotes() != null && !log.getNotes().isEmpty()) {
            holder.tvNotes.setText(log.getNotes());
            holder.tvNotes.setVisibility(View.VISIBLE);
        } else {
            holder.tvNotes.setVisibility(View.GONE);
        }
    }
    
    @Override
    public int getItemCount() {
        return logs.size();
    }
    
    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvDoseCount, tvDate, tvTime, tvScheduledInfo, tvNotes;
        ChipGroup chipGroupTriggers;
        
        ViewHolder(View itemView) {
            super(itemView);
            tvDoseCount = itemView.findViewById(R.id.tvDoseCount);
            tvDate = itemView.findViewById(R.id.tvDate);
            tvTime = itemView.findViewById(R.id.tvTime);
            tvScheduledInfo = itemView.findViewById(R.id.tvScheduledInfo);
            chipGroupTriggers = itemView.findViewById(R.id.chipGroupTriggers);
            tvNotes = itemView.findViewById(R.id.tvNotes);
        }
    }
}
