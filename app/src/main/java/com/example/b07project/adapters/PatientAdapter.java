package com.example.b07project.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.b07project.R;
import java.util.List;
import java.util.Map;

public class PatientAdapter extends RecyclerView.Adapter<PatientAdapter.PatientViewHolder> {

    private List<Map<String, Object>> patients;
    private OnPatientClickListener listener;

    public interface OnPatientClickListener {
        void onPatientClick(Map<String, Object> patient);
    }

    public PatientAdapter(List<Map<String, Object>> patients, OnPatientClickListener listener) {
        this.patients = patients;
        this.listener = listener;
    }

    @NonNull
    @Override
    public PatientViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_patient, parent, false);
        return new PatientViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PatientViewHolder holder, int position) {
        Map<String, Object> patient = patients.get(position);
        String name = (String) patient.get("name");
        // You might want to calculate age from DOB if available, for now just name
        
        holder.tvPatientName.setText(name != null ? name : "Unknown");
        holder.tvPatientAge.setText("Tap to view details"); // Placeholder

        holder.itemView.setOnClickListener(v -> listener.onPatientClick(patient));
    }

    @Override
    public int getItemCount() {
        return patients.size();
    }

    static class PatientViewHolder extends RecyclerView.ViewHolder {
        TextView tvPatientName;
        TextView tvPatientAge;

        public PatientViewHolder(@NonNull View itemView) {
            super(itemView);
            tvPatientName = itemView.findViewById(R.id.tvPatientName);
            tvPatientAge = itemView.findViewById(R.id.tvPatientAge);
        }
    }
}
