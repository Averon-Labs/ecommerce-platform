package com.averonlabs.eshopverse.checkout;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.averonlabs.eshopverse.R;
import com.averonlabs.eshopverse.core.model.CheckoutQuote;
import com.averonlabs.eshopverse.core.model.ShippingAddress;
import com.averonlabs.eshopverse.core.money.MoneyFormat;
import com.averonlabs.eshopverse.core.net.ApiClient;
import com.averonlabs.eshopverse.core.state.UiState;
import com.averonlabs.eshopverse.core.ui.view.StateContainerView;
import com.averonlabs.eshopverse.databinding.FragmentOrderReviewBinding;
import com.google.android.material.snackbar.Snackbar;

/**
 * Screen M-12: Order review before payment.
 * Displays server-confirmed items, address snapshot, and server totals.
 */
public class OrderReviewFragment extends Fragment {

    private FragmentOrderReviewBinding binding;
    private CheckoutViewModel viewModel;
    private ReviewItemsAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        binding = FragmentOrderReviewBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        CheckoutViewModelFactory factory = new CheckoutViewModelFactory(requireContext());
        viewModel = new ViewModelProvider(requireActivity(), factory).get(CheckoutViewModel.class);

        setupRecyclerView();
        setupListeners();
        observeViewModel();
    }

    private void setupRecyclerView() {
        adapter = new ReviewItemsAdapter();
        binding.rvReviewItems.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvReviewItems.setAdapter(adapter);
    }

    private void setupListeners() {
        binding.btnProceedToPayment.setOnClickListener(v -> viewModel.proceedToPayment());
    }

    private void observeViewModel() {
        viewModel.getQuoteState().observe(getViewLifecycleOwner(), this::renderQuoteState);

        viewModel.getShippingAddress().observe(getViewLifecycleOwner(), this::renderAddressSnapshot);

        viewModel.getIsOperationInFlight().observe(getViewLifecycleOwner(), inFlight -> {
            binding.btnProceedToPayment.setEnabled(!Boolean.TRUE.equals(inFlight));
        });

        viewModel.getNavigateToPaymentEvent().observe(getViewLifecycleOwner(), event -> {
            if (Boolean.TRUE.equals(event.getContentIfNotHandled())) {
                NavController navController = NavHostFragment.findNavController(this);
                navController.navigate(R.id.paymentChoiceFragment);
            }
        });

        viewModel.getCartConflictEvent().observe(getViewLifecycleOwner(), event -> {
            String message = event.getContentIfNotHandled();
            if (message != null && getView() != null) {
                Snackbar.make(getView(), message, Snackbar.LENGTH_LONG).show();
                NavHostFragment.findNavController(this).popBackStack(R.id.cartFragment, false);
            }
        });

        viewModel.getErrorResEvent().observe(getViewLifecycleOwner(), event -> {
            Integer resId = event.getContentIfNotHandled();
            if (resId != null && getView() != null) {
                Snackbar.make(getView(), resId, Snackbar.LENGTH_SHORT).show();
            }
        });
    }

    private void renderQuoteState(@Nullable UiState<CheckoutQuote> state) {
        if (state == null) {
            return;
        }

        switch (state.getStatus()) {
            case LOADING:
                binding.stateContainerReview.setState(StateContainerView.State.LOADING, null, null, null);
                break;
            case CONTENT:
                binding.stateContainerReview.setState(StateContainerView.State.CONTENT, null, null, null);
                if (state.getData() != null) {
                    bindQuote(state.getData());
                }
                break;
            case EMPTY:
                binding.stateContainerReview.setState(
                        StateContainerView.State.EMPTY,
                        getString(R.string.checkout_error_cart_empty),
                        getString(R.string.cart_action_explore),
                        () -> NavHostFragment.findNavController(this).popBackStack(R.id.cartFragment, false)
                );
                break;
            case ERROR:
                binding.stateContainerReview.setState(
                        StateContainerView.State.ERROR,
                        getString(state.getMessageRes() != 0 ? state.getMessageRes() : R.string.checkout_error_loading_quote),
                        getString(R.string.cart_action_retry),
                        () -> {
                            ShippingAddress address = viewModel.getShippingAddress().getValue();
                            if (address != null) {
                                viewModel.loadQuote(address);
                            }
                        }
                );
                break;
        }
    }

    private void bindQuote(@NonNull CheckoutQuote quote) {
        adapter.submitList(quote.getItems());
        binding.tvReviewSubtotal.setFormattedPrice(MoneyFormat.format(quote.getSubtotalMoney()));
        binding.tvReviewShipping.setFormattedPrice(MoneyFormat.format(quote.getShippingMoney()));
        binding.tvReviewTotal.setFormattedPrice(MoneyFormat.format(quote.getTotalMoney()));
    }

    private void renderAddressSnapshot(@Nullable ShippingAddress address) {
        if (address == null) {
            binding.tvReviewShippingAddress.setText("");
            return;
        }

        StringBuilder sb = new StringBuilder();
        sb.append(address.getRecipientName()).append("\n");
        sb.append(address.getLine1());
        if (address.getLine2() != null && !address.getLine2().isEmpty()) {
            sb.append(", ").append(address.getLine2());
        }
        sb.append("\n");
        sb.append(address.getCity()).append(", ")
                .append(address.getRegion()).append(" ")
                .append(address.getPostalCode()).append(", ")
                .append(address.getCountry());

        binding.tvReviewShippingAddress.setText(sb.toString());
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
