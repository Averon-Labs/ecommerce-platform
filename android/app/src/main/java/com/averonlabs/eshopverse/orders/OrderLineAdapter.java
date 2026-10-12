package com.averonlabs.eshopverse.orders;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.averonlabs.eshopverse.core.model.OrderItem;
import com.averonlabs.eshopverse.core.money.MoneyFormat;
import com.averonlabs.eshopverse.databinding.ItemOrderLineBinding;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapter for rendering immutable OrderItem lines in Screen M-16.
 */
public class OrderLineAdapter extends RecyclerView.Adapter<OrderLineAdapter.LineViewHolder> {

    private final List<OrderItem> items = new ArrayList<>();

    public void setItems(@NonNull List<OrderItem> newItems) {
        items.clear();
        items.addAll(newItems);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public LineViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemOrderLineBinding binding = ItemOrderLineBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false
        );
        return new LineViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull LineViewHolder holder, int position) {
        holder.bind(items.get(position));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class LineViewHolder extends RecyclerView.ViewHolder {
        private final ItemOrderLineBinding binding;

        LineViewHolder(@NonNull ItemOrderLineBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(@NonNull OrderItem item) {
            binding.tvLineProductName.setText(item.getProductName());
            String details = "Qty: " + item.getQuantity() + " × " + MoneyFormat.format(item.getUnitPriceMoney());
            binding.tvLineDetails.setText(details);
            binding.tvLineTotal.setFormattedPrice(MoneyFormat.format(item.getLineTotalMoney()));
        }
    }
}
