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

import com.averonlabs.eshopverse.R;
import com.averonlabs.eshopverse.core.model.CheckoutQuote;
import com.averonlabs.eshopverse.core.model.Order;
import com.averonlabs.eshopverse.core.model.OrderStatus;
import com.averonlabs.eshopverse.core.model.PaymentResult;
import com.averonlabs.eshopverse.core.model.PaymentStatus;
import com.averonlabs.eshopverse.core.money.MoneyFormat;
import com.averonlabs.eshopverse.core.net.ApiClient;
import com.averonlabs.eshopverse.core.state.UiState;
import com.averonlabs.eshopverse.core.ui.view.StatusBadgeView;
import com.averonlabs.eshopverse.databinding.FragmentOrderResultBinding;
import com.google.android.material.snackbar.Snackbar;

/**
 * Screen M-14: Payment / Order Result screen.
 * Renders server-confirmed states. Does not claim terminal states server did not return.
 * Offers safe retry for failed payments and order retrieval for ambiguous states.
 */
public class OrderResultFragment extends Fragment {

    private FragmentOrderResultBinding binding;
    private CheckoutViewModel viewModel;

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        binding = FragmentOrderResultBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        CheckoutViewModelFactory factory = new CheckoutViewModelFactory(requireContext());
        viewModel = new ViewModelProvider(requireActivity(), factory).get(CheckoutViewModel.class);

        setupListeners();
        observeViewModel();
    }

    private void setupListeners() {
        binding.btnContinueShopping.setOnClickListener(v -> {
            NavController navController = NavHostFragment.findNavController(this);
            navController.popBackStack(R.id.homeFragment, false);
        });

        binding.btnViewOrders.setOnClickListener(v -> {
            NavController navController = NavHostFragment.findNavController(this);
            navController.navigate(R.id.nav_orders);
        });

        binding.btnRetryPayment.setOnClickListener(v -> {
            NavController navController = NavHostFragment.findNavController(this);
            navController.popBackStack(R.id.paymentChoiceFragment, false);
        });

        binding.btnCheckStatus.setOnClickListener(v -> {
            Order order = viewModel.getCurrentOrder().getValue();
            if (order != null) {
                viewModel.checkOrderStatus(order.getId());
            }
        });
    }

    private void observeViewModel() {
        viewModel.getPaymentState().observe(getViewLifecycleOwner(), this::renderPaymentState);
        viewModel.getOrderDetailState().observe(getViewLifecycleOwner(), this::renderOrderDetailState);

        viewModel.getErrorNoticeEvent().observe(getViewLifecycleOwner(), event -> {
            String notice = event.getContentIfNotHandled();
            if (notice != null && getView() != null) {
                Snackbar.make(getView(), notice, Snackbar.LENGTH_LONG).show();
            }
        });
    }

    private void renderPaymentState(@Nullable UiState<PaymentResult> state) {
        if (state == null || state.getData() == null) {
            return;
        }

        PaymentResult result = state.getData();
        Order order = viewModel.getCurrentOrder().getValue();
        CheckoutQuote quote = viewModel.getQuoteState().getValue() != null
                ? viewModel.getQuoteState().getValue().getData() : null;

        String ref = order != null ? order.getReference() : "ORD-" + result.getOrderId();
        binding.tvResultOrderReference.setText(ref);

        if (quote != null) {
            binding.tvResultTotal.setFormattedPrice(MoneyFormat.format(quote.getTotalMoney()));
        } else if (order != null) {
            binding.tvResultTotal.setFormattedPrice(MoneyFormat.format(order.getTotalMoney()));
        }

        if (result.getPaymentStatus() == PaymentStatus.COMPLETED
                || result.getOrderStatus() == OrderStatus.CONFIRMED) {
            // Success state
            binding.tvResultHeading.setText(R.string.checkout_result_success_heading);
            binding.tvResultHeading.setTextColor(requireContext().getColor(R.color.text_primary_ink));
            binding.tvResultMessage.setText(R.string.checkout_result_success_message);
            setBadgeForOrderStatus(OrderStatus.CONFIRMED);

            binding.btnRetryPayment.setVisibility(View.GONE);
            binding.btnCheckStatus.setVisibility(View.GONE);
            binding.btnContinueShopping.setVisibility(View.VISIBLE);
            binding.btnViewOrders.setVisibility(View.VISIBLE);
        } else {
            // Failure state (§E.5)
            binding.tvResultHeading.setText(R.string.checkout_result_failure_heading);
            binding.tvResultHeading.setTextColor(requireContext().getColor(R.color.m3_error));
            binding.tvResultMessage.setText(R.string.checkout_result_failure_message);
            setBadgeForOrderStatus(OrderStatus.PENDING_PAYMENT);

            binding.btnRetryPayment.setVisibility(View.VISIBLE);
            binding.btnCheckStatus.setVisibility(View.GONE);
            binding.btnContinueShopping.setVisibility(View.VISIBLE);
            binding.btnViewOrders.setVisibility(View.VISIBLE);
        }
    }

    private void renderOrderDetailState(@Nullable UiState<Order> state) {
        if (state == null || state.getData() == null) {
            return;
        }

        Order order = state.getData();
        binding.tvResultOrderReference.setText(order.getReference());
        binding.tvResultTotal.setFormattedPrice(MoneyFormat.format(order.getTotalMoney()));
        setBadgeForOrderStatus(order.getStatus());

        if (order.getStatus() == OrderStatus.CONFIRMED) {
            binding.tvResultHeading.setText(R.string.checkout_result_success_heading);
            binding.tvResultHeading.setTextColor(requireContext().getColor(R.color.text_primary_ink));
            binding.tvResultMessage.setText(R.string.checkout_result_success_message);
            binding.btnRetryPayment.setVisibility(View.GONE);
            binding.btnCheckStatus.setVisibility(View.GONE);
        }
    }

    private void setBadgeForOrderStatus(@NonNull OrderStatus status) {
        binding.badgeResultStatus.setText(status.name());
        switch (status) {
            case CONFIRMED:
                binding.badgeResultStatus.setStatus(StatusBadgeView.Status.SUCCESS);
                break;
            case PENDING_PAYMENT:
                binding.badgeResultStatus.setStatus(StatusBadgeView.Status.WARNING);
                break;
            case CANCELLED:
                binding.badgeResultStatus.setStatus(StatusBadgeView.Status.ERROR);
                break;
            default:
                binding.badgeResultStatus.setStatus(StatusBadgeView.Status.NEUTRAL);
                break;
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
