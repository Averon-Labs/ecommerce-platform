package com.averonlabs.eshopverse.cart;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.averonlabs.eshopverse.MainActivity;
import com.averonlabs.eshopverse.R;
import com.averonlabs.eshopverse.core.model.Cart;
import com.averonlabs.eshopverse.core.model.CartItem;
import com.averonlabs.eshopverse.core.money.MoneyFormat;
import com.averonlabs.eshopverse.core.state.UiState;
import com.averonlabs.eshopverse.core.ui.view.StateContainerView;
import com.averonlabs.eshopverse.databinding.FragmentCartBinding;
import com.google.android.material.snackbar.Snackbar;

/**
 * Screen M-10: Cart Experience.
 * Displays customer cart lines, server-confirmed totals, quantity updates, line removal,
 * and stock/price issue recovery per docs/android/PHASE-2-SPEC.md §D.
 */
public class CartFragment extends Fragment {

    private FragmentCartBinding binding;
    private CartViewModel viewModel;
    private CartAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        binding = FragmentCartBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        CartViewModelFactory factory = new CartViewModelFactory(requireContext());
        viewModel = new ViewModelProvider(this, factory).get(CartViewModel.class);

        binding.stateContainerCart.setContentView(binding.scrollCartContent);

        setupRecyclerView();
        setupClickListeners();
        observeViewModel();

        viewModel.loadCart();
    }

    private void setupRecyclerView() {
        adapter = new CartAdapter(
                (productId, newQuantity) -> viewModel.updateQuantity(productId, newQuantity),
                productId -> viewModel.removeItem(productId)
        );
        binding.rvCartItems.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvCartItems.setAdapter(adapter);
    }

    private void setupClickListeners() {
        binding.btnCartCheckout.setOnClickListener(v -> {
            NavController navController = NavHostFragment.findNavController(this);
            navController.navigate(R.id.nav_checkout);
        });
    }

    private void observeViewModel() {
        viewModel.getCartState().observe(getViewLifecycleOwner(), this::renderCartState);

        viewModel.getMutatingProductId().observe(getViewLifecycleOwner(), mutatingId -> {
            if (adapter != null) {
                adapter.setMutatingProductId(mutatingId);
            }
        });

        viewModel.getItemCount().observe(getViewLifecycleOwner(), count -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).setCartBadgeCount(count != null ? count : 0);
            }
        });

        viewModel.getNoticeEvent().observe(getViewLifecycleOwner(), event -> {
            String notice = event.getContentIfNotHandled();
            if (notice != null && getView() != null) {
                showAnchoredSnackbar(notice);
            }
        });

        viewModel.getErrorResEvent().observe(getViewLifecycleOwner(), event -> {
            Integer errorRes = event.getContentIfNotHandled();
            if (errorRes != null && getView() != null) {
                showAnchoredSnackbar(errorRes);
            }
        });

        viewModel.getAuthRequiredEvent().observe(getViewLifecycleOwner(), event -> {
            Boolean authRequired = event.getContentIfNotHandled();
            if (Boolean.TRUE.equals(authRequired)) {
                Bundle args = new Bundle();
                args.putInt("returnToNavId", R.id.cartFragment);
                NavHostFragment.findNavController(this).navigate(R.id.nav_auth, args);
            }
        });
    }

    private void renderCartState(@Nullable UiState<Cart> state) {
        if (state == null) {
            return;
        }

        switch (state.getStatus()) {
            case LOADING:
                binding.stateContainerCart.setState(StateContainerView.State.LOADING, null, null, null);
                break;
            case CONTENT:
                binding.stateContainerCart.setState(StateContainerView.State.CONTENT, null, null, null);
                if (state.getData() != null) {
                    bindCart(state.getData());
                }
                break;
            case EMPTY:
                binding.stateContainerCart.setState(
                        StateContainerView.State.EMPTY,
                        getString(R.string.cart_empty_title),
                        getString(R.string.cart_action_explore),
                        () -> {
                            NavHostFragment.findNavController(this).navigate(R.id.exploreFragment);
                        }
                );
                binding.btnCartCheckout.setEnabled(false);
                break;
            case ERROR:
                binding.stateContainerCart.setState(
                        StateContainerView.State.ERROR,
                        getString(state.getMessageRes() != 0 ? state.getMessageRes() : R.string.cart_error_loading),
                        getString(R.string.cart_action_retry),
                        viewModel::retry
                );
                binding.btnCartCheckout.setEnabled(false);
                break;
        }
    }

    private void bindCart(@NonNull Cart cart) {
        adapter.submitList(cart.getItems());

        // Has Issues Notice Banner (§D.3)
        binding.cardCartIssues.setVisibility(cart.hasIssues() ? View.VISIBLE : View.GONE);

        // Server-authoritative Money Totals (§D.4)
        binding.tvCartSubtotal.setFormattedPrice(MoneyFormat.format(cart.getSubtotalMoney()));
        binding.tvCartShipping.setFormattedPrice(MoneyFormat.format(cart.getShippingMoney()));
        binding.tvCartTotal.setFormattedPrice(MoneyFormat.format(cart.getTotalMoney()));
        binding.tvCartTotal.setTextColor(ContextCompat.getColor(requireContext(), R.color.color_botanical_green));

        // Continue to checkout enabled ONLY when cart is non-empty and no line is unavailable (§D.4)
        boolean hasUnavailable = false;
        for (CartItem item : cart.getItems()) {
            if (item.isUnavailable()) {
                hasUnavailable = true;
                break;
            }
        }
        boolean canCheckout = !cart.getItems().isEmpty() && !hasUnavailable;
        binding.btnCartCheckout.setEnabled(canCheckout);
    }

    private void showAnchoredSnackbar(@androidx.annotation.StringRes int messageRes) {
        if (!isAdded() || getView() == null) return;
        Snackbar snackbar = Snackbar.make(getView(), messageRes, Snackbar.LENGTH_SHORT);
        if (getActivity() != null) {
            View bottomNav = getActivity().findViewById(R.id.card_bottom_nav);
            if (bottomNav != null && bottomNav.getVisibility() == View.VISIBLE) {
                snackbar.setAnchorView(bottomNav);
            }
        }
        snackbar.show();
    }

    private void showAnchoredSnackbar(@NonNull String message) {
        if (!isAdded() || getView() == null) return;
        Snackbar snackbar = Snackbar.make(getView(), message, Snackbar.LENGTH_LONG);
        if (getActivity() != null) {
            View bottomNav = getActivity().findViewById(R.id.card_bottom_nav);
            if (bottomNav != null && bottomNav.getVisibility() == View.VISIBLE) {
                snackbar.setAnchorView(bottomNav);
            }
        }
        snackbar.show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
