package com.example.b07project.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.b07project.R;
import java.util.List;
import java.util.Map;

import com.google.android.material.button.MaterialButtonToggleGroup;

public class ChildAdapter extends RecyclerView.Adapter<ChildAdapter.ChildViewHolder> {

    private List<Map<String, String>> children;
    private OnChildActionListener listener;

    public interface OnChildActionListener {
        void onGenerateCode(String childName, String childId);
        void onViewReports(String childName, String childId);
        void onSharingSettings(String childName, String childId);
        void onRangeChanged(String childId, int days);
    }

    public ChildAdapter(List<Map<String, String>> children, OnChildActionListener listener) {
        this.children = children;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ChildViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_child_dashboard, parent, false);
        return new ChildViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChildViewHolder holder, int position) {
        Map<String, String> child = children.get(position);
        String name = child.get("name");
        String id = child.get("id");

        holder.tvChildName.setText(name);
        
        // Bind stats
        String zone = child.getOrDefault("zone", "Unknown");
        holder.tvCurrentZone.setText(zone);
        
        // Color code zone
        int color = android.graphics.Color.BLACK;
        if (zone.equalsIgnoreCase("Green")) color = android.graphics.Color.parseColor("#4CAF50");
        else if (zone.equalsIgnoreCase("Yellow")) color = android.graphics.Color.parseColor("#FFC107");
        else if (zone.equalsIgnoreCase("Red")) color = android.graphics.Color.parseColor("#F44336");
        holder.tvCurrentZone.setTextColor(color);

        holder.tvLastRescue.setText(child.getOrDefault("lastRescue", "None"));
        holder.tvWeeklyRescues.setText(child.getOrDefault("weeklyRescues", "0"));

        holder.btnGenerateCode.setOnClickListener(v -> listener.onGenerateCode(name, id));
        holder.btnViewReports.setOnClickListener(v -> listener.onViewReports(name, id));
        holder.btnSharing.setOnClickListener(v -> listener.onSharingSettings(name, id));
        
        holder.toggleGroupRange.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                int days = (checkedId == R.id.btn30Days) ? 30 : 7;
                listener.onRangeChanged(id, days);
            }
        });
    }

    @Override
    public int getItemCount() {
        return children.size();
    }

    static class ChildViewHolder extends RecyclerView.ViewHolder {
        TextView tvChildName;
        TextView tvCurrentZone;
        TextView tvLastRescue;
        TextView tvWeeklyRescues;
        Button btnGenerateCode;
        Button btnViewReports;
        Button btnSharing;
        MaterialButtonToggleGroup toggleGroupRange;

        public ChildViewHolder(@NonNull View itemView) {
            super(itemView);
            tvChildName = itemView.findViewById(R.id.tvChildName);
            tvCurrentZone = itemView.findViewById(R.id.tvCurrentZone);
            tvLastRescue = itemView.findViewById(R.id.tvLastRescue);
            tvWeeklyRescues = itemView.findViewById(R.id.tvWeeklyRescues);
            btnGenerateCode = itemView.findViewById(R.id.btnGenerateCode);
            btnViewReports = itemView.findViewById(R.id.btnViewReports);
            btnSharing = itemView.findViewById(R.id.btnSharing);
            toggleGroupRange = itemView.findViewById(R.id.toggleGroupRange);
        }
    }
}
