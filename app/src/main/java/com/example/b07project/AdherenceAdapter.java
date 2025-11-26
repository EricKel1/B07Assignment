package com.example.b07project;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class AdherenceAdapter extends RecyclerView.Adapter<AdherenceAdapter.ViewHolder> {

    public static class DayStatus {
        String dayText;
        int color;

        public DayStatus(String dayText, int color) {
            this.dayText = dayText;
            this.color = color;
        }
    }

    private List<DayStatus> days;
    private Context context;

    public AdherenceAdapter(Context context, List<DayStatus> days) {
        this.context = context;
        this.days = days;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_calendar_day, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        DayStatus status = days.get(position);
        holder.tvDay.setText(status.dayText);
        holder.tvDay.setBackgroundTintList(ColorStateList.valueOf(status.color));
    }

    @Override
    public int getItemCount() {
        return days.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvDay;

        public ViewHolder(View itemView) {
            super(itemView);
            tvDay = itemView.findViewById(R.id.tvDay);
        }
    }
}
