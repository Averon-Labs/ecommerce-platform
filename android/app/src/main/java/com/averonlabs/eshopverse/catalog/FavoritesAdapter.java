package com.averonlabs.eshopverse.catalog;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.averonlabs.eshopverse.R;
import com.averonlabs.eshopverse.core.model.ProductSummary;
import com.averonlabs.eshopverse.core.money.MoneyFormat;
import com.averonlabs.eshopverse.core.ui.ImageLoaderHelper;
import com.averonlabs.eshopverse.core.ui.view.StatusBadgeView;
import com.averonlabs.eshopverse.databinding.ItemProductCardBinding;
import com.averonlabs.eshopverse.databinding.ItemProductLinearCardBinding;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Adapter presenting saved favorite products with support for both
 * Grid (2-column) and Linear (1-column list) display modes.
 */
public class FavoritesAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public static final int VIEW_TYPE_GRID = 0;
    public static final int VIEW_TYPE_LINEAR = 1;

    public interface OnProductClickListener {
        void onProductClick(@NonNull ProductSummary product);
    }

    private final List<ProductSummary> items = new ArrayList<>();
    @Nullable private final OnProductClickListener clickListener;
    private boolean isGridMode = true;

    public FavoritesAdapter(@Nullable OnProductClickListener clickListener) {
        this.clickListener = clickListener;
    }

    public void setGridMode(boolean gridMode) {
        if (this.isGridMode != gridMode) {
            this.isGridMode = gridMode;
            notifyItemRangeChanged(0, items.size());
        }
    }

    public boolean isGridMode() {
        return isGridMode;
    }

    public void submitList(@Nullable List<ProductSummary> newItems) {
        final List<ProductSummary> safeList = newItems != null ? newItems : Collections.emptyList();
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new DiffUtil.Callback() {
            @Override
            public int getOldListSize() {
                return items.size();
            }

            @Override
            public int getNewListSize() {
                return safeList.size();
            }

            @Override
            public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
                return items.get(oldItemPosition).getId() == safeList.get(newItemPosition).getId();
            }

            @Override
            public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
                return Objects.equals(items.get(oldItemPosition), safeList.get(newItemPosition));
            }
        });
        items.clear();
        items.addAll(safeList);
        diffResult.dispatchUpdatesTo(this);
    }

    @Override
    public int getItemViewType(int position) {
        return isGridMode ? VIEW_TYPE_GRID : VIEW_TYPE_LINEAR;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == VIEW_TYPE_GRID) {
            ItemProductCardBinding binding = ItemProductCardBinding.inflate(inflater, parent, false);
            return new GridViewHolder(binding);
        } else {
            ItemProductLinearCardBinding binding = ItemProductLinearCardBinding.inflate(inflater, parent, false);
            return new LinearViewHolder(binding);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ProductSummary product = items.get(position);
        if (holder instanceof GridViewHolder) {
            ((GridViewHolder) holder).bind(product);
        } else if (holder instanceof LinearViewHolder) {
            ((LinearViewHolder) holder).bind(product);
        }
    }

    @Override
    public void onViewRecycled(@NonNull RecyclerView.ViewHolder holder) {
        super.onViewRecycled(holder);
        if (holder instanceof GridViewHolder) {
            ImageLoaderHelper.clear(((GridViewHolder) holder).binding.ivProductImage);
        } else if (holder instanceof LinearViewHolder) {
            ImageLoaderHelper.clear(((LinearViewHolder) holder).binding.ivProductImage);
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    class GridViewHolder extends RecyclerView.ViewHolder {
        private final ItemProductCardBinding binding;

        GridViewHolder(@NonNull ItemProductCardBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(@NonNull ProductSummary product) {
            binding.tvProductName.setText(product.getName());
            String formattedPrice = MoneyFormat.format(product.getPriceMoney());
            binding.tvProductPrice.setFormattedPrice(formattedPrice);
            binding.tvProductPrice.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.color_botanical_green));

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

            ImageLoaderHelper.loadImage(binding.ivProductImage, product.getImageUrl());

            View.OnClickListener onProductClickAction = v -> {
                if (clickListener != null) {
                    clickListener.onProductClick(product);
                }
            };
            binding.cardProduct.setOnClickListener(onProductClickAction);
            binding.ivProductImage.setOnClickListener(onProductClickAction);

            binding.ivCardFavorite.setImageResource(R.drawable.ic_heart_filled);
            binding.ivCardFavorite.setImageTintList(ContextCompat.getColorStateList(itemView.getContext(), R.color.favorite_red));

            binding.btnCardFavorite.setOnClickListener(v -> {
                FavoritesRepository.getInstance(itemView.getContext()).toggleFavorite(product);
            });
        }
    }

    class LinearViewHolder extends RecyclerView.ViewHolder {
        private final ItemProductLinearCardBinding binding;

        LinearViewHolder(@NonNull ItemProductLinearCardBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(@NonNull ProductSummary product) {
            binding.tvProductName.setText(product.getName());
            binding.tvProductCategory.setText(product.getCategoryName() != null ? product.getCategoryName() : "");
            String formattedPrice = MoneyFormat.format(product.getPriceMoney());
            binding.tvProductPrice.setFormattedPrice(formattedPrice);

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

            ImageLoaderHelper.loadImage(binding.ivProductImage, product.getImageUrl());

            View.OnClickListener onProductClickAction = v -> {
                if (clickListener != null) {
                    clickListener.onProductClick(product);
                }
            };
            binding.cardProduct.setOnClickListener(onProductClickAction);
            binding.ivProductImage.setOnClickListener(onProductClickAction);

            binding.ivCardFavorite.setImageResource(R.drawable.ic_heart_filled);
            binding.ivCardFavorite.setImageTintList(ContextCompat.getColorStateList(itemView.getContext(), R.color.favorite_red));

            binding.btnCardFavorite.setOnClickListener(v -> {
                FavoritesRepository.getInstance(itemView.getContext()).toggleFavorite(product);
            });
        }
    }
}
