package com.averonlabs.eshopverse.cart;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.averonlabs.eshopverse.R;
import com.averonlabs.eshopverse.core.model.CartItem;
import com.averonlabs.eshopverse.core.money.MoneyFormat;
import com.averonlabs.eshopverse.databinding.ItemCartLineBinding;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * RecyclerView adapter for customer cart item lines per docs/android/PHASE-2-SPEC.md §D.
 */
public class CartAdapter extends RecyclerView.Adapter<CartAdapter.CartItemViewHolder> {

    public interface OnQuantityChangedListener {
        void onQuantityChanged(long productId, int newQuantity);
    }

    public interface OnRemoveItemListener {
        void onRemoveItem(long productId);
    }

    private final List<CartItem> items = new ArrayList<>();
    @Nullable private final OnQuantityChangedListener quantityChangedListener;
    @Nullable private final OnRemoveItemListener removeItemListener;
    @Nullable private Long mutatingProductId = null;

    public CartAdapter(
            @Nullable OnQuantityChangedListener quantityChangedListener,
            @Nullable OnRemoveItemListener removeItemListener
    ) {
        this.quantityChangedListener = quantityChangedListener;
        this.removeItemListener = removeItemListener;
    }

    public void submitList(@NonNull List<CartItem> newItems) {
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
                return items.get(oldItemPosition).getProductId() == newItems.get(newItemPosition).getProductId();
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

    public void setMutatingProductId(@Nullable Long mutatingProductId) {
        this.mutatingProductId = mutatingProductId;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public CartItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemCartLineBinding binding = ItemCartLineBinding.inflate(
                LayoutInflater.from(parent.getContext()),
                parent,
                false
        );
        return new CartItemViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull CartItemViewHolder holder, int position) {
        holder.bind(items.get(position));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    class CartItemViewHolder extends RecyclerView.ViewHolder {

        private final ItemCartLineBinding binding;

        CartItemViewHolder(@NonNull ItemCartLineBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(@NonNull CartItem item) {
            binding.tvCartItemName.setText(item.getName());

            // Unit Price and Line Total formatted with server-authoritative money
            binding.tvCartItemUnitPrice.setFormattedPrice(MoneyFormat.format(item.getUnitPriceMoney()));
            binding.tvCartLineTotal.setFormattedPrice(MoneyFormat.format(item.getLineTotalMoney()));
            binding.tvCartLineTotal.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.color_botanical_green));

            // Thumbnail Image
            Glide.with(itemView.getContext())
                    .load(item.getImageUrl())
                    .placeholder(R.drawable.ic_image_placeholder)
                    .error(R.drawable.ic_image_placeholder)
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .centerCrop()
                    .into(binding.ivCartItemImage);

            // Warnings handling: priceChanged, unavailable, stock limits
            if (item.isPriceChanged()) {
                binding.tvCartPriceChanged.setVisibility(View.VISIBLE);
                binding.tvCartPriceChanged.setText(itemView.getContext().getString(
                        R.string.cart_item_price_changed,
                        MoneyFormat.format(item.getUnitPriceMoney())
                ));
            } else {
                binding.tvCartPriceChanged.setVisibility(View.GONE);
            }

            if (item.isUnavailable()) {
                binding.tvCartUnavailable.setVisibility(View.VISIBLE);
                binding.tvCartUnavailable.setText(R.string.cart_item_unavailable);
            } else {
                binding.tvCartUnavailable.setVisibility(View.GONE);
            }

            if (!item.isInStock()) {
                binding.tvCartStockNotice.setVisibility(View.VISIBLE);
                binding.tvCartStockNotice.setText(R.string.cart_item_out_of_stock);
            } else if (item.getAvailableQuantity() > 0 && item.getAvailableQuantity() < item.getQuantity()) {
                binding.tvCartStockNotice.setVisibility(View.VISIBLE);
                binding.tvCartStockNotice.setText(itemView.getContext().getString(
                        R.string.cart_item_stock_limit,
                        item.getAvailableQuantity()
                ));
            } else {
                binding.tvCartStockNotice.setVisibility(View.GONE);
            }

            // Stepper configuration
            binding.stepperCartQuantity.setMin(1);
            if (item.getAvailableQuantity() > 0) {
                binding.stepperCartQuantity.setMax(item.getAvailableQuantity());
            } else {
                binding.stepperCartQuantity.setMax(99);
            }
            binding.stepperCartQuantity.setQuantity(item.getQuantity());

            // Disable stepper and remove button if item is currently mutating or unavailable
            boolean isThisItemMutating = mutatingProductId != null && mutatingProductId == (long) item.getProductId();
            boolean isStepperDisabled = isThisItemMutating || item.isUnavailable();

            binding.stepperCartQuantity.setEnabled(!isStepperDisabled);
            binding.btnCartRemove.setEnabled(!isThisItemMutating);

            binding.stepperCartQuantity.setOnQuantityChangedListener(newQuantity -> {
                if (quantityChangedListener != null && newQuantity != item.getQuantity()) {
                    quantityChangedListener.onQuantityChanged(item.getProductId(), newQuantity);
                }
            });

            binding.btnCartRemove.setContentDescription(itemView.getContext().getString(
                    R.string.cart_cd_remove_item,
                    item.getName()
            ));
            binding.btnCartRemove.setOnClickListener(v -> {
                if (removeItemListener != null) {
                    removeItemListener.onRemoveItem(item.getProductId());
                }
            });
        }
    }
}
