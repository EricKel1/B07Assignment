package com.example.b07project;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.List;
import java.util.Map;

public class ProviderChildrenAdapter extends RecyclerView.Adapter<ProviderChildrenAdapter.ChildViewHolder> {

    private List<Map<String, Object>> children;
    private OnChildClickListener listener;
    private FirebaseFirestore db;

    public interface OnChildClickListener {
        void onChildClick(Map<String, Object> child);
    }

    public ProviderChildrenAdapter(List<Map<String, Object>> children, OnChildClickListener listener) {
        this.children = children;
        this.listener = listener;
        this.db = FirebaseFirestore.getInstance();
    }

    @NonNull
    @Override
    public ChildViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_provider_child, parent, false);
        return new ChildViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChildViewHolder holder, int position) {
        Map<String, Object> child = children.get(position);
        
        String childId = (String) child.get("childId");
        String childName = (String) child.get("name");
        String childDob = (String) child.get("dob");
        
        holder.tvChildName.setText(childName != null ? childName : "Unknown Child");
        holder.tvChildDob.setText("DOB: " + (childDob != null ? childDob : "N/A"));
        
        // Check if peakFlow is shared
        Boolean peakFlowShared = (Boolean) child.get("peakFlow");
        Boolean triageShared = (Boolean) child.get("triageIncidents");
        
        if (peakFlowShared != null && peakFlowShared) {
            loadLatestPEFReading(childId, holder);
        } else {
            // Show "Not shared" zone indicator
            holder.tvPEFValue.setText("—");
            holder.tvZoneStatus.setText("Not shared");
            holder.cardZoneCircle.setCardBackgroundColor(Color.parseColor("#EEEEEE"));
            holder.tvZoneLabel.setTextColor(Color.parseColor("#9E9E9E"));
            holder.tvPEFUnit.setTextColor(Color.parseColor("#9E9E9E"));
        }
        
        // Check for recent triage escalation
        if (triageShared != null && triageShared) {
            checkRecentTriageEscalation(childId, holder);
        } else {
            holder.chipTriageEscalation.setVisibility(View.GONE);
        }
        
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onChildClick(child);
            }
        });
    }

    private void loadLatestPEFReading(String childId, ChildViewHolder holder) {
        // Query the latest PEF reading for this child
        db.collection("pef_readings")
                .whereEqualTo("userId", childId)
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(1)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        Map<String, Object> reading = querySnapshot.getDocuments().get(0).getData();
                        
                        Long valueLong = (Long) reading.get("value");
                        int pefValue = valueLong != null ? valueLong.intValue() : 0;
                        String zone = (String) reading.get("zone");
                        
                        holder.tvPEFValue.setText(String.valueOf(pefValue));
                        updateZoneIndicator(holder, zone);
                    }
                })
                .addOnFailureListener(e -> {
                    holder.tvPEFValue.setText("—");
                    holder.tvZoneStatus.setText("No data");
                });
    }

    private void updateZoneIndicator(ChildViewHolder holder, String zone) {
        int bgColor;
        int textColor;
        int strokeColor;
        
        // Normalize zone to lowercase to match PersonalBest.calculateZone() output
        String normalizedZone = zone != null ? zone.toLowerCase() : "unknown";
        
        if ("green".equals(normalizedZone)) {
            bgColor = Color.parseColor("#E8F5E9");
            textColor = Color.parseColor("#1B5E20");
            strokeColor = Color.parseColor("#4CAF50");
            holder.tvZoneStatus.setText("GREEN");
        } else if ("yellow".equals(normalizedZone)) {
            bgColor = Color.parseColor("#FFF9C4");
            textColor = Color.parseColor("#F57F17");
            strokeColor = Color.parseColor("#FBC02D");
            holder.tvZoneStatus.setText("YELLOW");
        } else if ("red".equals(normalizedZone)) {
            bgColor = Color.parseColor("#FFEBEE");
            textColor = Color.parseColor("#C62828");
            strokeColor = Color.parseColor("#F44336");
            holder.tvZoneStatus.setText("RED");
        } else {
            bgColor = Color.parseColor("#F5F5F5");
            textColor = Color.parseColor("#757575");
            strokeColor = Color.parseColor("#BDBDBD");
            holder.tvZoneStatus.setText("UNKNOWN");
        }
        
        holder.cardZoneCircle.setCardBackgroundColor(bgColor);
        holder.cardZoneCircle.setStrokeColor(strokeColor);
        holder.tvPEFValue.setTextColor(textColor);
        holder.tvZoneStatus.setTextColor(textColor);
        holder.tvZoneLabel.setTextColor(textColor);
        holder.tvPEFUnit.setTextColor(textColor);
    }

    private void checkRecentTriageEscalation(String childId, ChildViewHolder holder) {
        // Query for recent triage escalations in the last 7 days
        long sevenDaysAgo = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000);
        
        db.collection("triage_sessions")
                .whereEqualTo("userId", childId)
                .whereEqualTo("escalated", true)
                .whereGreaterThanOrEqualTo("startTime", new java.util.Date(sevenDaysAgo))
                .orderBy("startTime", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(1)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        holder.chipTriageEscalation.setVisibility(View.VISIBLE);
                    } else {
                        holder.chipTriageEscalation.setVisibility(View.GONE);
                    }
                })
                .addOnFailureListener(e -> {
                    holder.chipTriageEscalation.setVisibility(View.GONE);
                });
    }

    @Override
    public int getItemCount() {
        return children.size();
    }

    public static class ChildViewHolder extends RecyclerView.ViewHolder {
        TextView tvChildName;
        TextView tvChildDob;
        Chip chipTriageEscalation;
        MaterialCardView cardZoneCircle;
        TextView tvZoneLabel;
        TextView tvPEFValue;
        TextView tvPEFUnit;
        TextView tvZoneStatus;

        public ChildViewHolder(@NonNull View itemView) {
            super(itemView);
            tvChildName = itemView.findViewById(R.id.tvChildName);
            tvChildDob = itemView.findViewById(R.id.tvChildDob);
            chipTriageEscalation = itemView.findViewById(R.id.chipTriageEscalation);
            cardZoneCircle = itemView.findViewById(R.id.cardZoneCircle);
            tvZoneLabel = itemView.findViewById(R.id.tvZoneLabel);
            tvPEFValue = itemView.findViewById(R.id.tvPEFValue);
            tvPEFUnit = itemView.findViewById(R.id.tvPEFUnit);
            tvZoneStatus = itemView.findViewById(R.id.tvZoneStatus);
        }
    }
}
