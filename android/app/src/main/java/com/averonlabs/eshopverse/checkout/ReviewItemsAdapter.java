package com.averonlabs.eshopverse.checkout;

import android.annotation.SuppressLint;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.averonlabs.eshopverse.R;
import com.averonlabs.eshopverse.core.model.CartItem;
import com.averonlabs.eshopverse.core.money.MoneyFormat;
import com.averonlabs.eshopverse.databinding.ItemReviewLineBinding;
import com.bumptech.glide.Glide;

/**
 * Adapter for displaying ordered item snapshots on review screen M-12.
 */
public class ReviewItemsAdapter extends ListAdapter<CartItem, ReviewItemsAdapter.ReviewViewHolder> {

    private static final DiffUtil.ItemCallback<CartItem> DIFF_CALLBACK = new DiffUtil.ItemCallback<CartItem>() {
        @Override
        public boolean areItemsTheSame(@NonNull CartItem oldItem, @NonNull CartItem newItem) {
            return oldItem.getProductId() == newItem.getProductId();
        }

        @Override
        public boolean areContentsTheSame(@NonNull CartItem oldItem, @NonNull CartItem newItem) {
            return oldItem.equals(newItem);
        }
    };

    public ReviewItemsAdapter() {
        super(DIFF_CALLBACK);
    }

    @NonNull
    @Override
    public ReviewViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemReviewLineBinding binding = ItemReviewLineBinding.inflate(
                LayoutInflater.from(parent.getContext()),
                parent,
                false
        );
        return new ReviewViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ReviewViewHolder holder, int position) {
        holder.bind(getItem(position));
    }

    static class ReviewViewHolder extends RecyclerView.ViewHolder {
        private final ItemReviewLineBinding binding;

        ReviewViewHolder(@NonNull ItemReviewLineBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        @SuppressLint("SetTextI18n")
        void bind(@NonNull CartItem item) {
            binding.tvReviewItemName.setText(item.getName());

            String formattedUnit = MoneyFormat.format(item.getUnitPriceMoney());
            binding.tvReviewItemQtyPrice.setText("Qty: " + item.getQuantity() + " × " + formattedUnit);
            binding.tvReviewItemLineTotal.setFormattedPrice(MoneyFormat.format(item.getLineTotalMoney()));

            Glide.with(binding.ivReviewItemImage.getContext())
                    .load(item.getImageUrl())
                    .placeholder(R.drawable.ic_image_placeholder)
                    .error(R.drawable.ic_image_placeholder)
                    .into(binding.ivReviewItemImage);
        }
    }
}
