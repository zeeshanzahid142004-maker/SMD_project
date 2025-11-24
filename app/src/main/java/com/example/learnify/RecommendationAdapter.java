package com.example.learnify;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class RecommendationAdapter extends RecyclerView.Adapter<RecommendationAdapter.ViewHolder> {

    private final List<RecommendationItem> items;
    private final OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(RecommendationItem item);
    }

    public RecommendationAdapter(List<RecommendationItem> items, OnItemClickListener listener) {
        this.items = items;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_recommendation, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        RecommendationItem item = items.get(position);

        holder.tvTitle.setText(item.title);
        holder.tvDescription.setText(item.description);

        if (item.type.equals("CODING")) {
            holder.tvType.setVisibility(View.VISIBLE);
            holder.tvType.setText("💻 Coding");
        } else {
            holder.tvType.setVisibility(View.GONE);
        }

        holder.cardView.setOnClickListener(v -> listener.onItemClick(item));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        CardView cardView;
        TextView tvTitle, tvDescription, tvType;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = (CardView) itemView;
            tvTitle = itemView.findViewById(R.id.tv_recommendation_title);
            tvDescription = itemView.findViewById(R.id.tv_explanation);
            tvType = itemView.findViewById(R.id.tv_recommendation_type);
        }
    }
}
