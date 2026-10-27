package com.simplecallblocker;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapter to display recently blocked calls.
 */
public class BlockedCallsAdapter extends RecyclerView.Adapter<BlockedCallsAdapter.ViewHolder> {
    private final List<BlockedCallItem> items = new ArrayList<>();

    public void updateData(List<BlockedCallItem> newItems) {
        items.clear();
        if (newItems != null) {
            items.addAll(newItems);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_blocked_call, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        BlockedCallItem item = items.get(position);
        holder.tvPhoneNumber.setText(item.getPhoneNumber());
        holder.tvTimestamp.setText(item.getFormattedDate());
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView tvPhoneNumber;
        final TextView tvTimestamp;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvPhoneNumber = itemView.findViewById(R.id.tvPhoneNumber);
            tvTimestamp = itemView.findViewById(R.id.tvTimestamp);
        }
    }
}
