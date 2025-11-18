package com.example.b07project.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.b07project.R;
import com.example.b07project.models.Badge;
import java.util.List;

public class BadgeAdapter extends RecyclerView.Adapter<BadgeAdapter.BadgeViewHolder> {

    private List<Badge> badges;

    public BadgeAdapter(List<Badge> badges) {
        this.badges = badges;
    }

    @NonNull
    @Override
    public BadgeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_badge, parent, false);
        return new BadgeViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BadgeViewHolder holder, int position) {
        Badge badge = badges.get(position);
        holder.bind(badge);
    }

    @Override
    public int getItemCount() {
        return badges.size();
    }

    public void updateBadges(List<Badge> newBadges) {
        this.badges = newBadges;
        notifyDataSetChanged();
    }

    static class BadgeViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvBadgeIcon;
        private final TextView tvBadgeName;
        private final TextView tvBadgeDescription;
        private final ProgressBar progressBadge;
        private final TextView tvBadgeProgress;
        private final TextView tvBadgeEarned;

        public BadgeViewHolder(@NonNull View itemView) {
            super(itemView);
            tvBadgeIcon = itemView.findViewById(R.id.tvBadgeIcon);
            tvBadgeName = itemView.findViewById(R.id.tvBadgeName);
            tvBadgeDescription = itemView.findViewById(R.id.tvBadgeDescription);
            progressBadge = itemView.findViewById(R.id.progressBadge);
            tvBadgeProgress = itemView.findViewById(R.id.tvBadgeProgress);
            tvBadgeEarned = itemView.findViewById(R.id.tvBadgeEarned);
        }

        public void bind(Badge badge) {
            // Set icon based on badge type
            String icon;
            switch (badge.getType()) {
                case "perfect_controller_week":
                    icon = "💊";
                    break;
                case "technique_sessions":
                    icon = "📚";
                    break;
                case "low_rescue_month":
                    icon = "🌟";
                    break;
                default:
                    icon = "🏆";
            }
            tvBadgeIcon.setText(icon);

            tvBadgeName.setText(badge.getName());
            tvBadgeDescription.setText(badge.getDescription());

            if (badge.isEarned()) {
                // Badge earned
                tvBadgeEarned.setVisibility(View.VISIBLE);
                progressBadge.setVisibility(View.GONE);
                tvBadgeProgress.setVisibility(View.GONE);
                itemView.setAlpha(1.0f);
            } else {
                // Badge not earned yet
                tvBadgeEarned.setVisibility(View.GONE);
                
                // Special display for low_rescue_month badge
                if ("low_rescue_month".equals(badge.getType()) && badge.getPeriodEndDate() > 0) {
                    progressBadge.setVisibility(View.GONE);
                    tvBadgeProgress.setVisibility(View.VISIBLE);
                    
                    long now = System.currentTimeMillis();
                    long daysLeft = (badge.getPeriodEndDate() - now) / (1000 * 60 * 60 * 24);
                    
                    String progressText;
                    if (badge.getProgress() > badge.getRequirement()) {
                        progressText = "⚠️ " + badge.getProgress() + " uses this month (limit: " + badge.getRequirement() + ")";
                    } else {
                        progressText = badge.getProgress() + " of " + badge.getRequirement() + " uses • " + daysLeft + " days left";
                    }
                    tvBadgeProgress.setText(progressText);
                } else {
                    progressBadge.setVisibility(View.VISIBLE);
                    tvBadgeProgress.setVisibility(View.VISIBLE);
                    
                    progressBadge.setMax(badge.getRequirement());
                    progressBadge.setProgress(badge.getProgress());
                    tvBadgeProgress.setText(badge.getProgress() + " / " + badge.getRequirement());
                }
                
                itemView.setAlpha(0.6f);
            }
        }
    }
}
