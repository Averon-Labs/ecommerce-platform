package com.averonlabs.eshopverse.catalog.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.averonlabs.eshopverse.R;
import com.averonlabs.eshopverse.catalog.ProductParams;
import com.averonlabs.eshopverse.databinding.BottomSheetFilterBinding;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Material 3 Filter & Sort Bottom Sheet Dialog Fragment per FLT-001.
 * Supports single-select sort chips, dynamic RangeSlider price filtering,
 * in-stock availability toggle, and quick reset / apply actions.
 */
public class FilterBottomSheetDialogFragment extends BottomSheetDialogFragment {

    public static final String TAG = "FilterBottomSheetDialogFragment";

    public interface OnFilterAppliedListener {
        void onFilterApplied(
                @NonNull String sort,
                @Nullable Double minPrice,
                @Nullable Double maxPrice,
                @Nullable Boolean inStock
        );
    }

    private BottomSheetFilterBinding binding;
    private OnFilterAppliedListener listener;
    private String initialSort = ProductParams.SORT_NEWEST;
    private Double initialMinPrice = null;
    private Double initialMaxPrice = null;
    private Boolean initialInStock = null;

    public static FilterBottomSheetDialogFragment newInstance(@NonNull ProductParams params) {
        FilterBottomSheetDialogFragment fragment = new FilterBottomSheetDialogFragment();
        Bundle args = new Bundle();
        args.putString("sort", params.getSort());
        if (params.getMinPrice() != null) {
            args.putDouble("minPrice", params.getMinPrice());
        }
        if (params.getMaxPrice() != null) {
            args.putDouble("maxPrice", params.getMaxPrice());
        }
        if (params.getInStock() != null) {
            args.putBoolean("inStock", params.getInStock());
        }
        fragment.setArguments(args);
        return fragment;
    }

    public void setOnFilterAppliedListener(@Nullable OnFilterAppliedListener listener) {
        this.listener = listener;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args = getArguments();
        if (args != null) {
            initialSort = args.getString("sort", ProductParams.SORT_NEWEST);
            if (args.containsKey("minPrice")) {
                initialMinPrice = args.getDouble("minPrice");
            }
            if (args.containsKey("maxPrice")) {
                initialMaxPrice = args.getDouble("maxPrice");
            }
            if (args.containsKey("inStock")) {
                initialInStock = args.getBoolean("inStock");
            }
        }
    }

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        binding = BottomSheetFilterBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setupInitialState();
        setupListeners();
    }

    private void setupInitialState() {
        // Setup Sort Selection
        if (ProductParams.SORT_PRICE_ASC.equals(initialSort)) {
            binding.chipSortPriceAsc.setChecked(true);
        } else if (ProductParams.SORT_PRICE_DESC.equals(initialSort)) {
            binding.chipSortPriceDesc.setChecked(true);
        } else if (ProductParams.SORT_NAME_ASC.equals(initialSort)) {
            binding.chipSortNameAsc.setChecked(true);
        } else {
            binding.chipSortNewest.setChecked(true);
        }

        // Setup Price Range
        float min = initialMinPrice != null ? initialMinPrice.floatValue() : 0.0f;
        float max = initialMaxPrice != null ? initialMaxPrice.floatValue() : 200.0f;
        min = Math.max(0.0f, Math.min(min, 200.0f));
        max = Math.max(min, Math.min(max, 200.0f));

        binding.sliderPriceRange.setValues(min, max);
        updatePriceRangeText(min, max);

        // Setup In Stock Switch
        binding.switchInStock.setChecked(Boolean.TRUE.equals(initialInStock));
    }

    private void setupListeners() {
        // Range slider value changes
        binding.sliderPriceRange.addOnChangeListener((slider, value, fromUser) -> {
            List<Float> values = slider.getValues();
            if (values.size() >= 2) {
                updatePriceRangeText(values.get(0), values.get(1));
            }
        });

        // Clear All button
        binding.btnClearAll.setOnClickListener(v -> {
            binding.chipSortNewest.setChecked(true);
            binding.sliderPriceRange.setValues(0.0f, 200.0f);
            updatePriceRangeText(0.0f, 200.0f);
            binding.switchInStock.setChecked(false);
        });

        // Apply Filters button
        binding.btnApplyFilter.setOnClickListener(v -> {
            String selectedSort = ProductParams.SORT_NEWEST;
            int checkedId = binding.chipGroupSort.getCheckedChipId();
            if (checkedId == R.id.chip_sort_price_asc) {
                selectedSort = ProductParams.SORT_PRICE_ASC;
            } else if (checkedId == R.id.chip_sort_price_desc) {
                selectedSort = ProductParams.SORT_PRICE_DESC;
            } else if (checkedId == R.id.chip_sort_name_asc) {
                selectedSort = ProductParams.SORT_NAME_ASC;
            }

            List<Float> values = binding.sliderPriceRange.getValues();
            Double selectedMin = null;
            Double selectedMax = null;
            if (values.size() >= 2) {
                float minVal = values.get(0);
                float maxVal = values.get(1);
                if (minVal > 0.0f) {
                    selectedMin = (double) minVal;
                }
                if (maxVal < 200.0f) {
                    selectedMax = (double) maxVal;
                }
            }

            Boolean selectedInStock = binding.switchInStock.isChecked() ? Boolean.TRUE : null;

            if (listener != null) {
                listener.onFilterApplied(selectedSort, selectedMin, selectedMax, selectedInStock);
            }
            dismiss();
        });
    }

    private void updatePriceRangeText(float min, float max) {
        binding.tvPriceRange.setText(String.format(Locale.US, "$%d — $%d", Math.round(min), Math.round(max)));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
