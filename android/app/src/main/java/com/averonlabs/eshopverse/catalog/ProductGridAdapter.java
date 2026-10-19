package com.averonlabs.eshopverse.catalog;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.averonlabs.eshopverse.MainActivity;
import com.averonlabs.eshopverse.R;
import com.averonlabs.eshopverse.auth.AuthViewModelFactory;
import com.averonlabs.eshopverse.core.model.ProductSummary;
import com.averonlabs.eshopverse.core.money.MoneyFormat;
import com.averonlabs.eshopverse.core.ui.ImageLoaderHelper;
import com.averonlabs.eshopverse.core.ui.view.StatusBadgeView;
import com.averonlabs.eshopverse.databinding.ItemProductCardBinding;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.Collections;
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
    private final boolean isCarousel;

    public ProductGridAdapter(@Nullable OnProductClickListener clickListener) {
        this(clickListener, false);
    }

    public ProductGridAdapter(@Nullable OnProductClickListener clickListener, boolean isCarousel) {
        this.clickListener = clickListener;
        this.isCarousel = isCarousel;
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

    @NonNull
    @Override
    public ProductViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemProductCardBinding binding = ItemProductCardBinding.inflate(
                LayoutInflater.from(parent.getContext()),
                parent,
                false
        );
        if (isCarousel) {
            ViewGroup.LayoutParams lp = binding.getRoot().getLayoutParams();
            if (lp != null) {
                lp.width = parent.getContext().getResources().getDimensionPixelSize(R.dimen.product_carousel_item_width);
                binding.getRoot().setLayoutParams(lp);
            }
        }
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

    @NonNull
    public List<ProductSummary> getItems() {
        return Collections.unmodifiableList(items);
    }

    @Nullable
    public ProductSummary getItem(int position) {
        if (position >= 0 && position < items.size()) {
            return items.get(position);
        }
        return null;
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
            binding.tvProductPrice.setTextColor(androidx.core.content.ContextCompat.getColor(itemView.getContext(), R.color.color_botanical_green));

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

            // Favorites binding & toggle
            FavoritesRepository favRepo = FavoritesRepository.getInstance(itemView.getContext());
            boolean isFav = favRepo.isFavorite(product.getId());
            updateFavoriteVisual(isFav);

            binding.btnCardFavorite.setOnClickListener(v -> {
                boolean signedIn = AuthViewModelFactory.getDefaultRepository(itemView.getContext()).isSignedIn();
                if (signedIn) {
                    favRepo.toggleFavorite(product);
                    boolean newFav = favRepo.isFavorite(product.getId());
                    updateFavoriteVisual(newFav);
                } else {
                    new MaterialAlertDialogBuilder(itemView.getContext())
                            .setTitle(R.string.favorites_login_required_title)
                            .setMessage(R.string.favorites_login_required_message)
                            .setPositiveButton(R.string.favorites_action_sign_in, (dialog, which) -> {
                                try {
                                    NavController navController = Navigation.findNavController(itemView);
                                    navController.navigate(R.id.accountFragment);
                                } catch (Exception e) {
                                    if (itemView.getContext() instanceof MainActivity) {
                                        NavController nc = ((MainActivity) itemView.getContext()).getNavController();
                                        if (nc != null) {
                                            nc.navigate(R.id.accountFragment);
                                        }
                                    }
                                }
                            })
                            .setNegativeButton(R.string.favorites_action_cancel, null)
                            .show();
                }
            });
        }

        private void updateFavoriteVisual(boolean isFavorite) {
            if (isFavorite) {
                binding.ivCardFavorite.setImageResource(R.drawable.ic_heart_filled);
                binding.ivCardFavorite.setImageTintList(ContextCompat.getColorStateList(itemView.getContext(), R.color.favorite_red));
            } else {
                binding.ivCardFavorite.setImageResource(R.drawable.ic_heart_outline);
                binding.ivCardFavorite.setImageTintList(ContextCompat.getColorStateList(itemView.getContext(), R.color.icon_favorite_outline));
            }
        }
    }
}

