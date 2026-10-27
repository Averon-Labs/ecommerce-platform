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

import com.averonlabs.eshopverse.MainActivity;
import com.averonlabs.eshopverse.R;
import com.averonlabs.eshopverse.auth.AuthViewModelFactory;
import com.averonlabs.eshopverse.cart.CartRepository;
import com.averonlabs.eshopverse.cart.CartViewModelFactory;
import com.averonlabs.eshopverse.catalog.CatalogViewModelFactory;
import com.averonlabs.eshopverse.catalog.FavoritesRepository;
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
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import androidx.navigation.NavController;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.averonlabs.eshopverse.core.model.Currency;
import com.averonlabs.eshopverse.core.model.ProductSummary;
import com.averonlabs.eshopverse.core.money.Money;

/**
 * M-05 Product detail screen per docs/android/PHASE-1-SPEC.md §E.4.
 * Implements Text-First Immediate Rendering (Phase 11 — IMG-002).
 */
public class ProductDetailFragment extends Fragment {

    public static final String ARG_PRODUCT_ID = "productId";
    public static final String ARG_PRODUCT_NAME = "productName";
    public static final String ARG_PRODUCT_PRICE = "productPrice";
    public static final String ARG_PRODUCT_CURRENCY = "productCurrency";
    public static final String ARG_PRODUCT_CATEGORY = "productCategory";
    public static final String ARG_PRODUCT_IMAGE_URL = "productImageUrl";
    public static final String ARG_PRODUCT_IN_STOCK = "productInStock";
    public static final String ARG_PRODUCT_SUMMARY = "productSummary";

    @NonNull
    public static Bundle createArgs(@NonNull ProductSummary product) {
        return populateArgs(new Bundle(), product);
    }

    @NonNull
    public static Bundle populateArgs(@NonNull Bundle args, @NonNull ProductSummary product) {
        args.putLong(ARG_PRODUCT_ID, product.getId());
        args.putString(ARG_PRODUCT_NAME, product.getName());
        args.putString(ARG_PRODUCT_PRICE, product.getPrice());
        if (product.getCurrency() != null) {
            args.putString(ARG_PRODUCT_CURRENCY, product.getCurrency().name());
        }
        args.putString(ARG_PRODUCT_CATEGORY, product.getCategoryName());
        args.putString(ARG_PRODUCT_IMAGE_URL, product.getImageUrl());
        args.putBoolean(ARG_PRODUCT_IN_STOCK, product.isInStock());
        args.putSerializable(ARG_PRODUCT_SUMMARY, product);
        return args;
    }

    @NonNull
    public static Bundle createArgs(long productId) {
        return populateArgs(new Bundle(), productId);
    }

    @NonNull
    public static Bundle populateArgs(@NonNull Bundle args, long productId) {
        args.putLong(ARG_PRODUCT_ID, productId);
        return args;
    }

    private FragmentProductDetailBinding binding;
    private ProductDetailViewModel viewModel;
    private ImageCarouselAdapter imageAdapter;
    private long productId = -1;
    private boolean hasOptimisticSummary = false;
    private ProductSummary currentProduct = null;

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

        ProductSummary summary = null;
        if (getArguments() != null) {
            productId = getArguments().getLong(ARG_PRODUCT_ID, -1);
            if (getArguments().containsKey(ARG_PRODUCT_SUMMARY)) {
                try {
                    summary = (ProductSummary) getArguments().getSerializable(ARG_PRODUCT_SUMMARY);
                } catch (Exception ignored) {
                }
            }
            if (summary == null && getArguments().containsKey(ARG_PRODUCT_NAME)) {
                String name = getArguments().getString(ARG_PRODUCT_NAME);
                String price = getArguments().getString(ARG_PRODUCT_PRICE);
                String currencyStr = getArguments().getString(ARG_PRODUCT_CURRENCY);
                Currency currency = Currency.USD;
                if (currencyStr != null) {
                    try {
                        currency = Currency.valueOf(currencyStr);
                    } catch (Exception ignored) {
                    }
                }
                String category = getArguments().getString(ARG_PRODUCT_CATEGORY);
                String imageUrl = getArguments().getString(ARG_PRODUCT_IMAGE_URL);
                boolean inStock = getArguments().getBoolean(ARG_PRODUCT_IN_STOCK, true);
                if (name != null && price != null) {
                    summary = new ProductSummary((int) productId, name, price, currency, null, category, imageUrl, inStock, null);
                }
            }
        }

        if (summary != null) {
            viewModel.setOptimisticSummary(summary);
        } else if (viewModel.getOptimisticSummary().getValue() != null) {
            summary = viewModel.getOptimisticSummary().getValue();
        }

        setupBackButton();
        setupImageGallery();
        setupQuantityStepper();
        setupAddToCartAction();
        observeViewModel();

        if (summary != null) {
            hasOptimisticSummary = true;
            bindOptimisticSummary(summary);
        } else {
            hasOptimisticSummary = false;
            binding.skeletonProductDetail.getRoot().setVisibility(View.VISIBLE);
            binding.scrollProductContent.setVisibility(View.GONE);
            binding.cardBottomActions.setVisibility(View.GONE);
        }

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
                if (hasOptimisticSummary) {
                    binding.skeletonProductDetail.getRoot().setVisibility(View.GONE);
                    binding.scrollProductContent.setVisibility(View.VISIBLE);
                    binding.cardBottomActions.setVisibility(View.VISIBLE);
                    binding.skeletonDescription.setVisibility(View.VISIBLE);
                    binding.tvProductDescription.setVisibility(View.GONE);
                    binding.stateContainer.setState(StateContainerView.State.CONTENT, null, null, null);
                } else {
                    binding.skeletonProductDetail.getRoot().setVisibility(View.VISIBLE);
                    binding.scrollProductContent.setVisibility(View.GONE);
                    binding.cardBottomActions.setVisibility(View.GONE);
                    binding.stateContainer.setState(StateContainerView.State.CONTENT, null, null, null);
                }
                break;
            case CONTENT:
                binding.skeletonProductDetail.getRoot().setVisibility(View.GONE);
                binding.skeletonDescription.setVisibility(View.GONE);
                binding.scrollProductContent.setVisibility(View.VISIBLE);
                binding.cardBottomActions.setVisibility(View.VISIBLE);
                binding.stateContainer.setState(StateContainerView.State.CONTENT, null, null, null);
                if (state.getData() != null) {
                    bindProduct(state.getData());
                }
                break;
            case EMPTY:
            case ERROR:
                binding.skeletonProductDetail.getRoot().setVisibility(View.GONE);
                if (hasOptimisticSummary) {
                    // Summary already rendered optimistically — don't wipe out the screen
                    binding.skeletonDescription.setVisibility(View.GONE);
                    Snackbar.make(
                            binding.getRoot(),
                            getString(state.getMessageRes() != 0 ? state.getMessageRes() : R.string.catalog_error_load_details),
                            Snackbar.LENGTH_INDEFINITE
                    ).setAction(R.string.catalog_action_retry, v -> viewModel.retry()).show();
                } else {
                    binding.stateContainer.setState(
                            StateContainerView.State.ERROR,
                            getString(state.getMessageRes() != 0 ? state.getMessageRes() : R.string.catalog_error_load_details),
                            getString(R.string.catalog_action_retry),
                            viewModel::retry
                    );
                }
                break;
        }
    }

    private void bindOptimisticSummary(@NonNull ProductSummary product) {
        currentProduct = product;
        binding.skeletonProductDetail.getRoot().setVisibility(View.GONE);
        binding.scrollProductContent.setVisibility(View.VISIBLE);
        binding.cardBottomActions.setVisibility(View.VISIBLE);
        binding.stateContainer.setState(StateContainerView.State.CONTENT, null, null, null);

        if (product.getCategoryName() != null && !product.getCategoryName().trim().isEmpty()) {
            binding.tvProductCategory.setText(product.getCategoryName().toUpperCase());
            binding.tvProductCategory.setVisibility(View.VISIBLE);
        } else {
            binding.tvProductCategory.setVisibility(View.GONE);
        }

        binding.tvProductName.setText(product.getName());
        binding.tvProductPrice.setFormattedPrice(MoneyFormat.format(product.getPriceMoney()));
        binding.tvProductPrice.setTextColor(ContextCompat.getColor(requireContext(), R.color.color_botanical_green));

        // Description is still loading full detail -> show skeletonDescription, hide tvProductDescription
        binding.tvProductDescription.setVisibility(View.GONE);
        binding.skeletonDescription.setVisibility(View.VISIBLE);

        // Availability and Stepper Bounds
        boolean inStock = product.isInStock();
        if (inStock) {
            binding.badgeStock.setStatus(StatusBadgeView.Status.SUCCESS);
            binding.badgeStock.setText(R.string.catalog_in_stock);
            binding.stepperQuantity.setMax(99);
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

        // Hero image in gallery
        if (product.getImageUrl() != null && !product.getImageUrl().trim().isEmpty()) {
            List<ProductImage> imageList = new ArrayList<>();
            imageList.add(new ProductImage(product.getImageUrl(), 1));
            imageAdapter.submitList(imageList);
            binding.tvImageIndicator.setVisibility(View.GONE);
        }

        setupFavoriteButton(product);
    }

    private void bindProduct(@NonNull ProductDetail product) {
        currentProduct = product;
        binding.skeletonDescription.setVisibility(View.GONE);

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

        setupFavoriteButton(product);
    }

    private void setupFavoriteButton(@NonNull ProductSummary product) {
        FavoritesRepository favRepo = FavoritesRepository.getInstance(requireContext());
        boolean isFav = favRepo.isFavorite(product.getId());
        updateFavoriteVisual(isFav);

        binding.btnFavorite.setOnClickListener(v -> {
            boolean signedIn = AuthViewModelFactory.getDefaultRepository(requireContext()).isSignedIn();
            if (signedIn) {
                favRepo.toggleFavorite(product);
                boolean newFav = favRepo.isFavorite(product.getId());
                updateFavoriteVisual(newFav);
                Snackbar.make(binding.getRoot(), newFav ? R.string.favorites_added : R.string.favorites_removed, Snackbar.LENGTH_SHORT).show();
            } else {
                new MaterialAlertDialogBuilder(requireContext())
                        .setTitle(R.string.favorites_login_required_title)
                        .setMessage(R.string.favorites_login_required_message)
                        .setPositiveButton(R.string.favorites_action_sign_in, (dialog, which) -> {
                            try {
                                NavController navController = Navigation.findNavController(v);
                                navController.navigate(R.id.accountFragment);
                            } catch (Exception e) {
                                if (getActivity() instanceof MainActivity) {
                                    NavController nc = ((MainActivity) getActivity()).getNavController();
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
        if (binding == null || !isAdded()) return;
        if (isFavorite) {
            binding.ivFavoriteIcon.setImageResource(R.drawable.ic_heart_filled);
            binding.ivFavoriteIcon.setImageTintList(ContextCompat.getColorStateList(requireContext(), R.color.favorite_red));
        } else {
            binding.ivFavoriteIcon.setImageResource(R.drawable.ic_heart_outline);
            binding.ivFavoriteIcon.setImageTintList(ContextCompat.getColorStateList(requireContext(), R.color.icon_favorite_outline));
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
