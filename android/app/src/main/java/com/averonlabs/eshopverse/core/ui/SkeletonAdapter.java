package com.averonlabs.eshopverse.core.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

/**
 * Lightweight, zero-dependency skeleton adapter that inflates a fixed count of skeleton placeholders.
 * Eliminates blank screens and isolated loading spinners across Home, Explore, and Catalog grids
 * by delivering instantaneous visual structure from millisecond 0.
 */
public class SkeletonAdapter extends RecyclerView.Adapter<SkeletonAdapter.SkeletonViewHolder> {

    @LayoutRes
    private final int layoutResId;
    private final int itemCount;

    public SkeletonAdapter(@LayoutRes int layoutResId, int itemCount) {
        this.layoutResId = layoutResId;
        this.itemCount = Math.max(0, itemCount);
    }

    @NonNull
    @Override
    public SkeletonViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(layoutResId, parent, false);
        return new SkeletonViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SkeletonViewHolder holder, int position) {
        // Static shimmer placeholder, no binding required
    }

    @Override
    public int getItemCount() {
        return itemCount;
    }

    public static class SkeletonViewHolder extends RecyclerView.ViewHolder {
        public SkeletonViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }
}
