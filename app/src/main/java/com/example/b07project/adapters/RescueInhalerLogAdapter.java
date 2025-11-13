package com.example.b07project.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.b07project.R;
import com.example.b07project.models.RescueInhalerLog;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class RescueInhalerLogAdapter extends RecyclerView.Adapter<RescueInhalerLogAdapter.ViewHolder> {
    
    private List<RescueInhalerLog> logs;
    private SimpleDateFormat dateFormat;
    private SimpleDateFormat timeFormat;
    
    public RescueInhalerLogAdapter() {
        this.logs = new ArrayList<>();
        this.dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
        this.timeFormat = new SimpleDateFormat("hh:mm a", Locale.getDefault());
    }
    
    public void setLogs(List<RescueInhalerLog> logs) {
        this.logs = logs;
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
        RescueInhalerLog log = logs.get(position);
        
        String doseText = log.getDoseCount() == 1 ? "1 puff" : log.getDoseCount() + " puffs";
        holder.tvDoseCount.setText(doseText);
        
        if (log.getTimestamp() != null) {
            holder.tvDate.setText(dateFormat.format(log.getTimestamp()));
            holder.tvTime.setText(timeFormat.format(log.getTimestamp()));
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
        TextView tvDoseCount, tvDate, tvTime, tvNotes;
        
        ViewHolder(View itemView) {
            super(itemView);
            tvDoseCount = itemView.findViewById(R.id.tvDoseCount);
            tvDate = itemView.findViewById(R.id.tvDate);
            tvTime = itemView.findViewById(R.id.tvTime);
            tvNotes = itemView.findViewById(R.id.tvNotes);
        }
    }
}
