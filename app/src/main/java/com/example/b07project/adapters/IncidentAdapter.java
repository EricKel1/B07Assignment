package com.example.b07project.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.example.b07project.R;
import com.example.b07project.models.PersonalBest;
import com.example.b07project.models.TriageSession;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class IncidentAdapter extends RecyclerView.Adapter<IncidentAdapter.ViewHolder> {

    private List<TriageSession> incidents;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy h:mm a", Locale.getDefault());

    public IncidentAdapter(List<TriageSession> incidents) {
        this.incidents = incidents;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_incident, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        TriageSession session = incidents.get(position);
        
        // Decision badge
        String decision = session.getDecision() != null ? session.getDecision() : "unknown";
        switch (decision) {
            case "emergency":
                holder.tvDecisionBadge.setText("🚨 EMERGENCY");
                holder.tvDecisionBadge.setBackgroundColor(
                    ContextCompat.getColor(holder.itemView.getContext(), android.R.color.holo_red_dark));
                break;
            case "home_steps":
                holder.tvDecisionBadge.setText("⚠️ MONITOR");
                holder.tvDecisionBadge.setBackgroundColor(
                    ContextCompat.getColor(holder.itemView.getContext(), android.R.color.holo_orange_dark));
                break;
            case "monitor":
                holder.tvDecisionBadge.setText("🏠 HOME");
                holder.tvDecisionBadge.setBackgroundColor(
                    ContextCompat.getColor(holder.itemView.getContext(), android.R.color.holo_blue_dark));
                break;
            default:
                holder.tvDecisionBadge.setText("UNKNOWN");
                holder.tvDecisionBadge.setBackgroundColor(
                    ContextCompat.getColor(holder.itemView.getContext(), android.R.color.darker_gray));
        }
        
        // Timestamp
        if (session.getStartTime() != null) {
            holder.tvTimestamp.setText(dateFormat.format(session.getStartTime()));
        }
        
        // Red flags
        if (session.hasCriticalFlags()) {
            holder.tvRedFlagsLabel.setVisibility(View.VISIBLE);
            holder.chipGroupRedFlags.removeAllViews();
            
            List<String> flags = session.getCriticalFlagsList();
            for (String flag : flags) {
                Chip chip = new Chip(holder.itemView.getContext());
                chip.setText(flag);
                chip.setChipBackgroundColorResource(android.R.color.holo_red_light);
                chip.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), android.R.color.white));
                chip.setClickable(false);
                holder.chipGroupRedFlags.addView(chip);
            }
        } else {
            holder.tvRedFlagsLabel.setVisibility(View.GONE);
            holder.chipGroupRedFlags.removeAllViews();
        }
        
        // Rescue attempts
        if (session.getRescueAttemptsLast3Hours() > 0) {
            holder.tvRescueAttempts.setVisibility(View.VISIBLE);
            holder.tvRescueAttempts.setText("Rescue inhaler uses (3 hrs): " + session.getRescueAttemptsLast3Hours());
            
            if (session.getRescueAttemptsLast3Hours() >= 3) {
                holder.tvRescueAttempts.setTextColor(
                    ContextCompat.getColor(holder.itemView.getContext(), android.R.color.holo_red_dark));
            }
        } else {
            holder.tvRescueAttempts.setVisibility(View.GONE);
        }
        
        // PEF reading
        if (session.getCurrentPEF() != null && session.getCurrentPEF() > 0) {
            holder.tvPEFReading.setVisibility(View.VISIBLE);
            
            String zoneInfo = "";
            if (session.getCurrentZone() != null) {
                String zoneLabel = PersonalBest.getZoneLabel(session.getCurrentZone());
                zoneInfo = " (" + zoneLabel + ")";
            }
            
            holder.tvPEFReading.setText("Peak Flow: " + session.getCurrentPEF() + " L/min" + zoneInfo);
        } else {
            holder.tvPEFReading.setVisibility(View.GONE);
        }
        
        // Outcome
        if (session.getUserResponse() != null && !session.getUserResponse().isEmpty()) {
            holder.tvOutcome.setVisibility(View.VISIBLE);
            
            String outcomeText;
            int outcomeColor;
            
            if ("improved".equals(session.getUserResponse())) {
                outcomeText = "✓ User improved";
                outcomeColor = ContextCompat.getColor(holder.itemView.getContext(), android.R.color.holo_green_dark);
            } else if ("still_trouble".equals(session.getUserResponse())) {
                outcomeText = "⚠️ Continued difficulty";
                outcomeColor = ContextCompat.getColor(holder.itemView.getContext(), android.R.color.holo_orange_dark);
            } else {
                outcomeText = "Outcome: " + session.getUserResponse();
                outcomeColor = ContextCompat.getColor(holder.itemView.getContext(), android.R.color.darker_gray);
            }
            
            if (session.isEscalated()) {
                outcomeText += " → Escalated to emergency";
                outcomeColor = ContextCompat.getColor(holder.itemView.getContext(), android.R.color.holo_red_dark);
            }
            
            holder.tvOutcome.setText(outcomeText);
            holder.tvOutcome.setTextColor(outcomeColor);
        } else {
            holder.tvOutcome.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return incidents.size();
    }

    public void updateData(List<TriageSession> newIncidents) {
        this.incidents = newIncidents;
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvDecisionBadge, tvTimestamp, tvRedFlagsLabel;
        ChipGroup chipGroupRedFlags;
        TextView tvRescueAttempts, tvPEFReading, tvOutcome;

        ViewHolder(View itemView) {
            super(itemView);
            tvDecisionBadge = itemView.findViewById(R.id.tvDecisionBadge);
            tvTimestamp = itemView.findViewById(R.id.tvTimestamp);
            tvRedFlagsLabel = itemView.findViewById(R.id.tvRedFlagsLabel);
            chipGroupRedFlags = itemView.findViewById(R.id.chipGroupRedFlags);
            tvRescueAttempts = itemView.findViewById(R.id.tvRescueAttempts);
            tvPEFReading = itemView.findViewById(R.id.tvPEFReading);
            tvOutcome = itemView.findViewById(R.id.tvOutcome);
        }
    }
}
