# Rule 07: Java Implementation & Architecture Guide

This rulebook provides robust, production-tested Java patterns for manipulating and wiring modern Material Design 3 and Dribbble UI components without memory leaks or spaghetti code.

---

## 1. Clean, Leak-Free ViewBinding in Java

### In `AppCompatActivity`
```java
package com.yourapp.ui;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.yourapp.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupBentoGrid();
    }

    private void setupBentoGrid() {
        binding.tvHeroTitle.setText("$45,210.00");
    }
}
```

### In `Fragment` (Mandatory Nulling in `onDestroyView`)
```java
package com.yourapp.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.yourapp.databinding.FragmentDashboardBinding;

public class DashboardFragment extends Fragment {

    // Must be nullable to avoid memory leaks when view is detached!
    private FragmentDashboardBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentDashboardBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // Bind UI and click listeners here
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null; // CRITICAL: Prevents Fragment view leaks
    }
}
```

---

## 2. Programmatic Shape Styling (Material Shape System)

When you need to adjust corner radii or cut corners dynamically in Java without creating 20 different XML files:

```java
import com.google.android.material.shape.CornerFamily;
import com.google.android.material.shape.ShapeAppearanceModel;
import com.google.android.material.card.MaterialCardView;

public static void applyCustomCornerRadius(@NonNull MaterialCardView card, float topRadiusDp, float bottomRadiusDp) {
    float density = card.getContext().getResources().getDisplayMetrics().density;
    
    ShapeAppearanceModel shapeModel = card.getShapeAppearanceModel()
        .toBuilder()
        .setTopLeftCorner(CornerFamily.ROUNDED, topRadiusDp * density)
        .setTopRightCorner(CornerFamily.ROUNDED, topRadiusDp * density)
        .setBottomLeftCorner(CornerFamily.ROUNDED, bottomRadiusDp * density)
        .setBottomRightCorner(CornerFamily.ROUNDED, bottomRadiusDp * density)
        .build();

    card.setShapeAppearanceModel(shapeModel);
}
```

---

## 3. Resolving Theme Colors Safely in Java

Never hardcode hex values in Java code. Use `MaterialColors.getColor()`:

```java
import com.google.android.material.color.MaterialColors;
import android.content.Context;

// Safely gets colorPrimary, or falls back to black if missing
int primaryColor = MaterialColors.getColor(context, com.google.android.material.R.attr.colorPrimary, android.graphics.Color.BLACK);

// Get surface variant for borders
int outlineVariant = MaterialColors.getColor(context, com.google.android.material.R.attr.colorOutlineVariant, android.graphics.Color.GRAY);
```

---

## 4. High-Performance RecyclerView in Java (DiffUtil)

To ensure smooth 120fps scrolling on long feeds, never call `notifyDataSetChanged()`. Always use `DiffUtil`:

```java
package com.yourapp.ui.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import com.yourapp.databinding.ItemTransactionBinding;
import com.yourapp.model.Transaction;

public class TransactionAdapter extends ListAdapter<Transaction, TransactionAdapter.ViewHolder> {

    public TransactionAdapter() {
        super(DIFF_CALLBACK);
    }

    private static final DiffUtil.ItemCallback<Transaction> DIFF_CALLBACK = 
        new DiffUtil.ItemCallback<Transaction>() {
            @Override
            public boolean areItemsTheSame(@NonNull Transaction oldItem, @NonNull Transaction newItem) {
                return oldItem.getId().equals(newItem.getId());
            }

            @Override
            public boolean areContentsTheSame(@NonNull Transaction oldItem, @NonNull Transaction newItem) {
                return oldItem.equals(newItem);
            }
        };

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemTransactionBinding binding = ItemTransactionBinding.inflate(
            LayoutInflater.from(parent.getContext()), parent, false
        );
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(getItem(position));
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemTransactionBinding binding;

        ViewHolder(ItemTransactionBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(Transaction item) {
            binding.tvTitle.setText(item.getTitle());
            binding.tvAmount.setText(item.getFormattedAmount());
        }
    }
}
```
