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

public class ChildAdapter extends RecyclerView.Adapter<ChildAdapter.ChildViewHolder> {

    private List<Map<String, String>> children;
    private OnChildActionListener listener;

    public interface OnChildActionListener {
        void onGenerateCode(String childName, String childId);
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
        holder.btnGenerateCode.setOnClickListener(v -> listener.onGenerateCode(name, id));
    }

    @Override
    public int getItemCount() {
        return children.size();
    }

    static class ChildViewHolder extends RecyclerView.ViewHolder {
        TextView tvChildName;
        Button btnGenerateCode;

        public ChildViewHolder(@NonNull View itemView) {
            super(itemView);
            tvChildName = itemView.findViewById(R.id.tvChildName);
            btnGenerateCode = itemView.findViewById(R.id.btnGenerateCode);
        }
    }
}
