package com.example.learnify;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.ViewHolder> {

    private Context context;
    private List<HistoryItem> items;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());

    public HistoryAdapter(Context context, List<HistoryItem> items) {
        this.context = context;
        this.items = items;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.list_item_history, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        HistoryItem item = items.get(position);

        holder.tvTitle.setText(item.getTitle());
        holder.tvScore.setText(item.getScore() + "/" + item.getTotalMarks());

        if (item.getDateTaken() != null) {
            holder.tvDate.setText(dateFormat.format(item.getDateTaken()));
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvScore, tvDate;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            // Make sure these IDs exist in list_item_history.xml
            tvTitle = itemView.findViewById(R.id.history_item_title);
            tvScore = itemView.findViewById(R.id.history_item_score);
            tvDate = itemView.findViewById(R.id.history_item_date);
        }
    }
}