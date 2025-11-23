package com.example.b07project.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.b07project.R;
import com.example.b07project.models.SymptomCheckIn;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class SymptomCheckInAdapter extends RecyclerView.Adapter<SymptomCheckInAdapter.ViewHolder> {

    private List<SymptomCheckIn> checkIns;
    private SimpleDateFormat dateFormat;

    public SymptomCheckInAdapter() {
        this.checkIns = new ArrayList<>();
        this.dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
    }

    public void setCheckIns(List<SymptomCheckIn> checkIns) {
        this.checkIns = checkIns;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_symptom_checkin, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        SymptomCheckIn checkIn = checkIns.get(position);

        if (checkIn.getDate() != null) {
            holder.tvDate.setText(dateFormat.format(checkIn.getDate()));
        }

        int level = checkIn.getSymptomLevel();
        holder.tvSymptomLevel.setText("Level " + level);
        
        // Set color based on severity
        int color;
        String description;
        switch (level) {
            case 1:
                color = 0xFF4CAF50; // Green
                description = "No symptoms";
                break;
            case 2:
                color = 0xFF8BC34A; // Light green
                description = "Mild symptoms";
                break;
            case 3:
                color = 0xFFFF9800; // Orange
                description = "Moderate symptoms";
                break;
            case 4:
                color = 0xFFFF5722; // Deep orange
                description = "Severe symptoms";
                break;
            case 5:
                color = 0xFFF44336; // Red
                description = "Very severe symptoms";
                break;
            default:
                color = 0xFFFF9800;
                description = "Moderate symptoms";
        }
        holder.tvSymptomLevel.setTextColor(color);
        holder.tvSymptomDescription.setText(description);

        // Add symptom chips
        holder.chipGroupSymptoms.removeAllViews();
        if (checkIn.getSymptoms() != null && !checkIn.getSymptoms().isEmpty()) {
            holder.tvSymptomsLabel.setVisibility(View.VISIBLE);
            for (String symptom : checkIn.getSymptoms()) {
                Chip chip = new Chip(holder.itemView.getContext());
                chip.setText(symptom);
                chip.setChipBackgroundColorResource(R.color.primary_blue);
                chip.setTextColor(holder.itemView.getContext().getColor(android.R.color.white));
                chip.setClickable(false);
                chip.setTextSize(12f);
                holder.chipGroupSymptoms.addView(chip);
            }
            holder.chipGroupSymptoms.setVisibility(View.VISIBLE);
        } else {
            holder.tvSymptomsLabel.setVisibility(View.GONE);
            holder.chipGroupSymptoms.setVisibility(View.GONE);
        }

        // Add trigger chips
        holder.chipGroupTriggers.removeAllViews();
        if (checkIn.getTriggers() != null && !checkIn.getTriggers().isEmpty()) {
            holder.tvTriggersLabel.setVisibility(View.VISIBLE);
            for (String trigger : checkIn.getTriggers()) {
                Chip chip = new Chip(holder.itemView.getContext());
                chip.setText(trigger);
                chip.setChipBackgroundColorResource(android.R.color.transparent);
                chip.setChipStrokeColorResource(android.R.color.darker_gray);
                chip.setChipStrokeWidth(2f);
                chip.setClickable(false);
                chip.setTextSize(12f);
                holder.chipGroupTriggers.addView(chip);
            }
            holder.chipGroupTriggers.setVisibility(View.VISIBLE);
        } else {
            holder.tvTriggersLabel.setVisibility(View.GONE);
            holder.chipGroupTriggers.setVisibility(View.GONE);
        }

        // Show notes if available
        if (checkIn.getNotes() != null && !checkIn.getNotes().isEmpty()) {
            holder.tvNotes.setText("Notes: " + checkIn.getNotes());
            holder.tvNotes.setVisibility(View.VISIBLE);
        } else {
            holder.tvNotes.setVisibility(View.GONE);
        }
        
        if (checkIn.getEnteredBy() != null) {
            holder.tvEnteredBy.setText("Entered by: " + checkIn.getEnteredBy());
            holder.tvEnteredBy.setVisibility(View.VISIBLE);
        } else {
            holder.tvEnteredBy.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return checkIns.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvDate, tvSymptomLevel, tvSymptomDescription, tvSymptomsLabel, tvTriggersLabel, tvNotes, tvEnteredBy;
        ChipGroup chipGroupSymptoms, chipGroupTriggers;

        ViewHolder(View itemView) {
            super(itemView);
            tvDate = itemView.findViewById(R.id.tvDate);
            tvSymptomLevel = itemView.findViewById(R.id.tvSymptomLevel);
            tvSymptomDescription = itemView.findViewById(R.id.tvSymptomDescription);
            tvSymptomsLabel = itemView.findViewById(R.id.tvSymptomsLabel);
            tvTriggersLabel = itemView.findViewById(R.id.tvTriggersLabel);
            tvNotes = itemView.findViewById(R.id.tvNotes);
            tvEnteredBy = itemView.findViewById(R.id.tvEnteredBy);
            chipGroupSymptoms = itemView.findViewById(R.id.chipGroupSymptoms);
            chipGroupTriggers = itemView.findViewById(R.id.chipGroupTriggers);
        }
    }
}
