package com.averonlabs.eshopverse.catalog;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.averonlabs.eshopverse.R;
import com.averonlabs.eshopverse.core.model.ProductSummary;
import com.averonlabs.eshopverse.core.money.MoneyFormat;
import com.averonlabs.eshopverse.core.ui.view.StatusBadgeView;
import com.averonlabs.eshopverse.databinding.ItemProductCardBinding;
import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * RecyclerView adapter presenting products in a multi-column grid per docs/android/PHASE-1-SPEC.md §E.4.
 */
public class ProductGridAdapter extends RecyclerView.Adapter<ProductGridAdapter.ProductViewHolder> {

    public interface OnProductClickListener {
        void onProductClick(@NonNull ProductSummary product);
    }

    private final List<ProductSummary> items = new ArrayList<>();
    @Nullable private final OnProductClickListener clickListener;

    public ProductGridAdapter(@Nullable OnProductClickListener clickListener) {
        this.clickListener = clickListener;
    }

    public void submitList(@NonNull List<ProductSummary> newItems) {
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new DiffUtil.Callback() {
            @Override
            public int getOldListSize() {
                return items.size();
            }

            @Override
            public int getNewListSize() {
                return newItems.size();
            }

            @Override
            public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
                return items.get(oldItemPosition).getId() == newItems.get(newItemPosition).getId();
            }

            @Override
            public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
                return Objects.equals(items.get(oldItemPosition), newItems.get(newItemPosition));
            }
        });
        items.clear();
        items.addAll(newItems);
        diffResult.dispatchUpdatesTo(this);
    }

    @NonNull
    @Override
    public ProductViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemProductCardBinding binding = ItemProductCardBinding.inflate(
                LayoutInflater.from(parent.getContext()),
                parent,
                false
        );
        return new ProductViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ProductViewHolder holder, int position) {
        holder.bind(items.get(position));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    class ProductViewHolder extends RecyclerView.ViewHolder {

        private final ItemProductCardBinding binding;

        ProductViewHolder(@NonNull ItemProductCardBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(@NonNull ProductSummary product) {
            binding.tvProductName.setText(product.getName());
            String formattedPrice = MoneyFormat.format(product.getPriceMoney());
            binding.tvProductPrice.setFormattedPrice(formattedPrice);

            // Organic Pinterest masonry staggered heights based on product ID
            int[] staggeredHeights = {150, 195, 170, 220, 185};
            int heightDp = staggeredHeights[Math.abs(product.getId()) % staggeredHeights.length];
            int heightPx = (int) (heightDp * itemView.getContext().getResources().getDisplayMetrics().density);
            if (binding.containerImage.getLayoutParams() != null) {
                binding.containerImage.getLayoutParams().height = heightPx;
            }

            String stockLabel;
            if (!product.isInStock()) {
                binding.badgeOutOfStock.setVisibility(View.VISIBLE);
                binding.badgeOutOfStock.setStatus(StatusBadgeView.Status.NEUTRAL);
                binding.badgeOutOfStock.setText(R.string.catalog_out_of_stock);
                stockLabel = itemView.getContext().getString(R.string.catalog_out_of_stock);
            } else {
                binding.badgeOutOfStock.setVisibility(View.GONE);
                stockLabel = itemView.getContext().getString(R.string.catalog_in_stock);
            }

            binding.cardProduct.setContentDescription(itemView.getContext().getString(
                    R.string.catalog_cd_product_card,
                    product.getName(),
                    formattedPrice,
                    stockLabel
            ));

            Glide.with(itemView.getContext())
                    .load(product.getImageUrl())
                    .placeholder(R.drawable.ic_image_placeholder)
                    .error(R.drawable.ic_image_placeholder)
                    .centerCrop()
                    .into(binding.ivProductImage);

            binding.cardProduct.setOnClickListener(v -> {
                if (clickListener != null) {
                    clickListener.onProductClick(product);
                }
            });
        }
    }
}
