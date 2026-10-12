package com.averonlabs.eshopverse.orders;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.averonlabs.eshopverse.R;
import com.averonlabs.eshopverse.core.model.OrderSummary;
import com.averonlabs.eshopverse.core.money.MoneyFormat;
import com.averonlabs.eshopverse.databinding.ItemOrderCardBinding;

/**
 * Adapter for rendering OrderSummary cards on Screen M-15.
 */
public class OrderAdapter extends ListAdapter<OrderSummary, OrderAdapter.OrderViewHolder> {

    public interface OnOrderClickListener {
        void onOrderClick(@NonNull OrderSummary order);
    }

    private final OnOrderClickListener listener;

    public OrderAdapter(@NonNull OnOrderClickListener listener) {
        super(DIFF_CALLBACK);
        this.listener = listener;
    }

    @NonNull
    @Override
    public OrderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemOrderCardBinding binding = ItemOrderCardBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false
        );
        return new OrderViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull OrderViewHolder holder, int position) {
        holder.bind(getItem(position));
    }

    class OrderViewHolder extends RecyclerView.ViewHolder {
        private final ItemOrderCardBinding binding;

        OrderViewHolder(@NonNull ItemOrderCardBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(@NonNull OrderSummary order) {
            binding.tvOrderReference.setText(order.getReference());
            binding.tvOrderDate.setText(binding.getRoot().getContext().getString(
                    R.string.order_date_label) + " " + order.getCreatedAt()
            );

            String itemCountText = order.getItemCount() + " " +
                    (order.getItemCount() == 1 ? "item" : "items");
            binding.tvOrderItemsCount.setText(itemCountText);

            binding.tvOrderTotal.setFormattedPrice(MoneyFormat.format(order.getTotalMoney()));

            binding.badgeOrderStatus.setText(
                    binding.getRoot().getContext().getString(OrderStatusFormatter.getStatusTextRes(order.getStatus()))
            );
            binding.badgeOrderStatus.setStatus(OrderStatusFormatter.getBadgeStatus(order.getStatus()));

            binding.getRoot().setOnClickListener(v -> listener.onOrderClick(order));
        }
    }

    private static final DiffUtil.ItemCallback<OrderSummary> DIFF_CALLBACK = new DiffUtil.ItemCallback<OrderSummary>() {
        @Override
        public boolean areItemsTheSame(@NonNull OrderSummary oldItem, @NonNull OrderSummary newItem) {
            return oldItem.getId() == newItem.getId();
        }

        @Override
        public boolean areContentsTheSame(@NonNull OrderSummary oldItem, @NonNull OrderSummary newItem) {
            return oldItem.getReference().equals(newItem.getReference())
                    && oldItem.getStatus() == newItem.getStatus()
                    && oldItem.getPaymentStatus() == newItem.getPaymentStatus()
                    && oldItem.getTotal().equals(newItem.getTotal());
        }
    };
}
