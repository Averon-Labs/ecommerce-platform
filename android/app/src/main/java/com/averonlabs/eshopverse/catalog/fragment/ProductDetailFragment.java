package com.averonlabs.eshopverse.catalog.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.viewpager2.widget.ViewPager2;

import com.averonlabs.eshopverse.R;
import com.averonlabs.eshopverse.cart.CartRepository;
import com.averonlabs.eshopverse.cart.CartViewModelFactory;
import com.averonlabs.eshopverse.catalog.CatalogViewModelFactory;
import com.averonlabs.eshopverse.catalog.ImageCarouselAdapter;
import com.averonlabs.eshopverse.catalog.ProductDetailViewModel;
import com.averonlabs.eshopverse.core.model.Cart;
import com.averonlabs.eshopverse.core.model.ProductDetail;
import com.averonlabs.eshopverse.core.model.ProductImage;
import com.averonlabs.eshopverse.core.money.MoneyFormat;
import com.averonlabs.eshopverse.core.state.UiState;
import com.averonlabs.eshopverse.core.ui.view.StateContainerView;
import com.averonlabs.eshopverse.core.ui.view.StatusBadgeView;
import com.averonlabs.eshopverse.databinding.FragmentProductDetailBinding;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * M-05 Product detail screen per docs/android/PHASE-1-SPEC.md §E.4.
 */
public class ProductDetailFragment extends Fragment {

    private FragmentProductDetailBinding binding;
    private ProductDetailViewModel viewModel;
    private ImageCarouselAdapter imageAdapter;
    private long productId = -1;

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        binding = FragmentProductDetailBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        CatalogViewModelFactory factory = new CatalogViewModelFactory(requireContext());
        viewModel = new ViewModelProvider(this, factory).get(ProductDetailViewModel.class);

        if (getArguments() != null) {
            productId = getArguments().getLong("productId", -1);
        }

        setupBackButton();
        setupImageGallery();
        setupQuantityStepper();
        setupAddToCartAction();
        observeViewModel();

        if (productId > 0) {
            viewModel.loadProduct(productId);
        } else {
            binding.stateContainer.setState(
                    StateContainerView.State.ERROR,
                    getString(R.string.catalog_error_load_details),
                    null,
                    null
            );
        }
    }

    private void setupBackButton() {
        binding.btnBack.setOnClickListener(v -> {
            if (isAdded()) {
                try {
                    if (!Navigation.findNavController(v).popBackStack()) {
                        requireActivity().getOnBackPressedDispatcher().onBackPressed();
                    }
                } catch (Exception e) {
                    requireActivity().getOnBackPressedDispatcher().onBackPressed();
                }
            }
        });
    }

    private void setupImageGallery() {
        imageAdapter = new ImageCarouselAdapter();
        binding.vpProductImages.setAdapter(imageAdapter);

        binding.vpProductImages.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                viewModel.setSelectedImageIndex(position);
                int total = imageAdapter.getItemCount();
                if (total > 1) {
                    binding.tvImageIndicator.setVisibility(View.VISIBLE);
                    binding.tvImageIndicator.setText(getString(R.string.catalog_cd_product_image, position + 1, total));
                } else {
                    binding.tvImageIndicator.setVisibility(View.GONE);
                }
            }
        });
    }

    private void setupQuantityStepper() {
        binding.stepperQuantity.setMin(1);
        binding.stepperQuantity.setMax(99);
        binding.stepperQuantity.setDecrementContentDescription(getString(R.string.catalog_cd_stepper_decrement));
        binding.stepperQuantity.setIncrementContentDescription(getString(R.string.catalog_cd_stepper_increment));

        binding.stepperQuantity.setOnQuantityChangedListener(quantity -> {
            viewModel.setQuantity(quantity);
        });
    }

    private void setupAddToCartAction() {
        binding.btnAddToCart.setOnClickListener(v -> {
            if (!isAdded() || productId <= 0) {
                return;
            }
            int quantity = binding.stepperQuantity.getQuantity();
            if (quantity <= 0) {
                quantity = 1;
            }
            binding.btnAddToCart.setEnabled(false);
            CartRepository cartRepository = CartViewModelFactory.getDefaultRepository(requireContext());
            final int finalQuantity = quantity;
            cartRepository.addItem(productId, finalQuantity, new CartRepository.CartCallback<Cart>() {
                @Override
                public void onSuccess(@NonNull Cart cart) {
                    if (!isAdded()) {
                        return;
                    }
                    binding.btnAddToCart.setEnabled(true);
                    Snackbar.make(
                            binding.getRoot(),
                            R.string.catalog_added_to_cart,
                            Snackbar.LENGTH_SHORT
                    ).show();
                }

                @Override
                public void onError(@NonNull Throwable error) {
                    if (!isAdded()) {
                        return;
                    }
                    binding.btnAddToCart.setEnabled(true);
                    Snackbar.make(
                            binding.getRoot(),
                            error.getMessage() != null ? error.getMessage() : getString(R.string.catalog_error_rate_limited),
                            Snackbar.LENGTH_SHORT
                    ).show();
                }
            });
        });
    }

    private void observeViewModel() {
        viewModel.getProductState().observe(getViewLifecycleOwner(), this::renderProductState);

        viewModel.getQuantity().observe(getViewLifecycleOwner(), quantity -> {
            if (quantity != null && binding.stepperQuantity.getQuantity() != quantity) {
                binding.stepperQuantity.setQuantity(quantity);
            }
        });
    }

    private void renderProductState(UiState<ProductDetail> state) {
        if (state == null) {
            return;
        }

        switch (state.getStatus()) {
            case LOADING:
                binding.stateContainer.setState(StateContainerView.State.LOADING, null, null, null);
                break;
            case CONTENT:
                binding.stateContainer.setState(StateContainerView.State.CONTENT, null, null, null);
                if (state.getData() != null) {
                    bindProduct(state.getData());
                }
                break;
            case EMPTY:
            case ERROR:
                binding.stateContainer.setState(
                        StateContainerView.State.ERROR,
                        getString(state.getMessageRes() != 0 ? state.getMessageRes() : R.string.catalog_error_load_details),
                        getString(R.string.catalog_action_retry),
                        viewModel::retry
                );
                break;
        }
    }

    private void bindProduct(@NonNull ProductDetail product) {
        if (product.getCategoryName() != null && !product.getCategoryName().trim().isEmpty()) {
            binding.tvProductCategory.setText(product.getCategoryName().toUpperCase());
            binding.tvProductCategory.setVisibility(View.VISIBLE);
        } else {
            binding.tvProductCategory.setVisibility(View.GONE);
        }

        binding.tvProductName.setText(product.getName());
        binding.tvProductPrice.setFormattedPrice(MoneyFormat.format(product.getPriceMoney()));
        binding.tvProductPrice.setTextColor(ContextCompat.getColor(requireContext(), R.color.color_botanical_green));

        // Description
        String description = product.getDescription();
        if (description != null && !description.isEmpty()) {
            binding.tvProductDescription.setText(description);
            binding.tvProductDescription.setVisibility(View.VISIBLE);
        } else {
            binding.tvProductDescription.setVisibility(View.GONE);
        }

        // Availability and Stepper Bounds
        boolean inStock = product.isInStock();
        int stockQuantity = product.getStockQuantity();

        if (inStock) {
            binding.badgeStock.setStatus(StatusBadgeView.Status.SUCCESS);
            if (stockQuantity > 0) {
                binding.badgeStock.setText(getString(R.string.catalog_stock_quantity, stockQuantity));
                binding.stepperQuantity.setMax(Math.min(99, stockQuantity));
            } else {
                binding.badgeStock.setText(R.string.catalog_in_stock);
                binding.stepperQuantity.setMax(99);
            }
            binding.layoutQuantity.setVisibility(View.VISIBLE);
            binding.btnAddToCart.setEnabled(true);
            binding.tvAddToCartNotice.setVisibility(View.GONE);
        } else {
            binding.badgeStock.setStatus(StatusBadgeView.Status.NEUTRAL);
            binding.badgeStock.setText(R.string.catalog_out_of_stock);
            binding.layoutQuantity.setVisibility(View.GONE);
            binding.btnAddToCart.setEnabled(false);
            binding.tvAddToCartNotice.setVisibility(View.VISIBLE);
            binding.tvAddToCartNotice.setText(R.string.catalog_out_of_stock);
        }

        // Image Gallery
        List<ProductImage> imageList = new ArrayList<>();
        if (product.getImages() != null) {
            imageList.addAll(product.getImages());
        }
        if (imageList.isEmpty() && product.getImageUrl() != null) {
            imageList.add(new ProductImage(product.getImageUrl(), 1));
        }

        Collections.sort(imageList, (a, b) -> Integer.compare(a.getSortOrder(), b.getSortOrder()));
        imageAdapter.submitList(imageList);

        if (imageList.size() > 1) {
            binding.tvImageIndicator.setVisibility(View.VISIBLE);
            binding.tvImageIndicator.setText(getString(R.string.catalog_cd_product_image, 1, imageList.size()));
        } else {
            binding.tvImageIndicator.setVisibility(View.GONE);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
