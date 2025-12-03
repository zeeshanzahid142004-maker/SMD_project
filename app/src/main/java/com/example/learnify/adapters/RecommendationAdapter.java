package com.example.learnify.adapters;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.learnify.R;
import com.example.learnify.modelclass.RecommendationItem;

import java.util.List;

public class RecommendationAdapter extends RecyclerView.Adapter<RecommendationAdapter.ViewHolder> {

    private static final String TAG = "RecommendationAdapter";
    private final List<RecommendationItem> items;
    private final OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(RecommendationItem item);
    }

    public RecommendationAdapter(List<RecommendationItem> items, OnItemClickListener listener) {
        this.items = items;
        this.listener = listener;
        Log.d(TAG, "Adapter created with " + items.size() + " items");
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_recommendation, parent, false);
        Log.d(TAG, "ViewHolder created");
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        RecommendationItem item = items.get(position);
        Log.d(TAG, "Binding item " + position + ": " + item.title);

        // Set title (question text)
        holder.tvTitle.setText(item.title);

        // Set type badge
        if (item.type != null && item.type.equals("CODING")) {
            holder.tvType.setText("💻 Coding Exercise");
            holder.tvType.setVisibility(View.VISIBLE);
            // Use video play icon for now, or create ic_code if you want
            holder.ivIcon.setImageResource(R.drawable.ic_video_play);
        } else {
            holder.tvType.setText("📖 Concept Review");
            holder.tvType.setVisibility(View.VISIBLE);
            holder.ivIcon.setImageResource(R.drawable.ic_video_play);
        }

        // Click listener
        holder.itemView.setOnClickListener(v -> {
            Log.d(TAG, "Item clicked: " + item.title);
            listener.onItemClick(item);
        });
    }

    @Override
    public int getItemCount() {
        int count = items.size();
        Log.d(TAG, "getItemCount: " + count);
        return count;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivIcon;
        TextView tvTitle, tvType;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivIcon = itemView.findViewById(R.id.iv_recommendation_icon);
            tvTitle = itemView.findViewById(R.id.tv_recommendation_title);
            tvType = itemView.findViewById(R.id.tv_recommendation_type);

            Log.d(TAG, "ViewHolder views bound - " +
                    "icon: " + (ivIcon != null) +
                    ", title: " + (tvTitle != null) +
                    ", type: " + (tvType != null));
        }
    }
}