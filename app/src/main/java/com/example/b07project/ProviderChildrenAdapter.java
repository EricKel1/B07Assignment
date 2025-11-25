package com.example.b07project;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;
import java.util.Map;

public class ProviderChildrenAdapter extends RecyclerView.Adapter<ProviderChildrenAdapter.ChildViewHolder> {

    private List<Map<String, Object>> children;
    private OnChildClickListener listener;

    public interface OnChildClickListener {
        void onChildClick(Map<String, Object> child);
    }

    public ProviderChildrenAdapter(List<Map<String, Object>> children, OnChildClickListener listener) {
        this.children = children;
        this.listener = listener;
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
        
        String childName = (String) child.get("name");
        String childDob = (String) child.get("dob");
        
        holder.tvChildName.setText(childName != null ? childName : "Unknown Child");
        holder.tvChildDob.setText("DOB: " + (childDob != null ? childDob : "N/A"));
        
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onChildClick(child);
            }
        });
    }

    @Override
    public int getItemCount() {
        return children.size();
    }

    public static class ChildViewHolder extends RecyclerView.ViewHolder {
        TextView tvChildName;
        TextView tvChildDob;

        public ChildViewHolder(@NonNull View itemView) {
            super(itemView);
            tvChildName = itemView.findViewById(R.id.tvChildName);
            tvChildDob = itemView.findViewById(R.id.tvChildDob);
        }
    }
}
