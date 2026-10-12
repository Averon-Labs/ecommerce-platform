package com.averonlabs.eshopverse.orders;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.averonlabs.eshopverse.R;
import com.averonlabs.eshopverse.core.model.OrderStatusEvent;
import com.averonlabs.eshopverse.databinding.ItemOrderTimelineEventBinding;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapter for rendering order timeline events in Screen M-16.
 */
public class OrderTimelineAdapter extends RecyclerView.Adapter<OrderTimelineAdapter.TimelineViewHolder> {

    private final List<OrderStatusEvent> events = new ArrayList<>();

    public void setEvents(@NonNull List<OrderStatusEvent> newEvents) {
        events.clear();
        events.addAll(newEvents);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public TimelineViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemOrderTimelineEventBinding binding = ItemOrderTimelineEventBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false
        );
        return new TimelineViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull TimelineViewHolder holder, int position) {
        holder.bind(events.get(position));
    }

    @Override
    public int getItemCount() {
        return events.size();
    }

    static class TimelineViewHolder extends RecyclerView.ViewHolder {
        private final ItemOrderTimelineEventBinding binding;

        TimelineViewHolder(@NonNull ItemOrderTimelineEventBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(@NonNull OrderStatusEvent event) {
            binding.tvEventStatus.setText(
                    binding.getRoot().getContext().getString(
                            OrderStatusFormatter.getStatusTextRes(event.getStatus())
                    )
            );
            binding.tvEventTime.setText(event.getAt());
        }
    }
}
