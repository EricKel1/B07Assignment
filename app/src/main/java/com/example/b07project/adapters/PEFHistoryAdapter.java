package com.example.b07project.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;
import com.example.b07project.R;
import com.example.b07project.models.PEFReading;
import com.example.b07project.models.PersonalBest;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class PEFHistoryAdapter extends RecyclerView.Adapter<PEFHistoryAdapter.ViewHolder> {

    private List<PEFReading> readings;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
    private SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm a", Locale.getDefault());

    public PEFHistoryAdapter(List<PEFReading> readings) {
        this.readings = readings;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_pef_history, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        PEFReading reading = readings.get(position);

        holder.tvDate.setText(dateFormat.format(reading.getTimestamp()));
        holder.tvTime.setText(timeFormat.format(reading.getTimestamp()));
        holder.tvValue.setText(reading.getValue() + " L/min");

        // Zone
        String zone = reading.getZone();
        if (zone != null) {
            holder.tvZone.setText(PersonalBest.getZoneLabel(zone));
            holder.tvZone.setTextColor(PersonalBest.getZoneColor(zone));
        } else {
            holder.tvZone.setText("Zone Unknown");
            holder.tvZone.setTextColor(0xFF757575); // Grey
        }

        // Context (Pre/Post Med)
        if (reading.isPreMedication()) {
            holder.tvContext.setText("Pre-Medication");
            holder.tvContext.setVisibility(View.VISIBLE);
            holder.tvContext.setBackgroundResource(R.drawable.bg_context_pill_blue);
        } else if (reading.isPostMedication()) {
            holder.tvContext.setText("Post-Medication");
            holder.tvContext.setVisibility(View.VISIBLE);
            holder.tvContext.setBackgroundResource(R.drawable.bg_context_pill_green);
        } else {
            holder.tvContext.setVisibility(View.GONE);
        }

        // Notes
        if (reading.getNotes() != null && !reading.getNotes().isEmpty()) {
            holder.tvNotes.setText(reading.getNotes());
            holder.tvNotes.setVisibility(View.VISIBLE);
        } else {
            holder.tvNotes.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return readings.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvDate, tvTime, tvValue, tvZone, tvContext, tvNotes;
        CardView cardView;

        ViewHolder(View itemView) {
            super(itemView);
            tvDate = itemView.findViewById(R.id.tvDate);
            tvTime = itemView.findViewById(R.id.tvTime);
            tvValue = itemView.findViewById(R.id.tvValue);
            tvZone = itemView.findViewById(R.id.tvZone);
            tvContext = itemView.findViewById(R.id.tvContext);
            tvNotes = itemView.findViewById(R.id.tvNotes);
            cardView = (CardView) itemView;
        }
    }
}
