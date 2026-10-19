package com.averonlabs.eshopverse.catalog;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.averonlabs.eshopverse.R;
import com.averonlabs.eshopverse.core.model.ProductImage;
import com.averonlabs.eshopverse.core.ui.ImageLoaderHelper;
import com.averonlabs.eshopverse.databinding.ItemProductImageBinding;

import java.util.ArrayList;
import java.util.List;

/**
 * ViewPager2 adapter displaying the ordered product image gallery per docs/android/PHASE-1-SPEC.md §E.5.
 */
public class ImageCarouselAdapter extends RecyclerView.Adapter<ImageCarouselAdapter.ImageViewHolder> {

    private final List<ProductImage> images = new ArrayList<>();

    public void submitList(@NonNull List<ProductImage> newImages) {
        images.clear();
        images.addAll(newImages);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ImageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemProductImageBinding binding = ItemProductImageBinding.inflate(
                LayoutInflater.from(parent.getContext()),
                parent,
                false
        );
        return new ImageViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ImageViewHolder holder, int position) {
        holder.bind(images.get(position), position, getItemCount());
    }

    @Override
    public int getItemCount() {
        return images.size();
    }

    static class ImageViewHolder extends RecyclerView.ViewHolder {

        private final ItemProductImageBinding binding;

        ImageViewHolder(@NonNull ItemProductImageBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(@NonNull ProductImage image, int position, int total) {
            binding.ivCarouselImage.setContentDescription(
                    itemView.getContext().getString(R.string.catalog_cd_product_image, position + 1, total)
            );

            ImageLoaderHelper.loadImage(binding.ivCarouselImage, image.getUrl());
        }
    }
}
