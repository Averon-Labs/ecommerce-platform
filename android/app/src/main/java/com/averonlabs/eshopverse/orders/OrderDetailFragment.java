package com.averonlabs.eshopverse.orders;

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
import com.averonlabs.eshopverse.core.model.Order;
import com.averonlabs.eshopverse.core.model.ShippingAddress;
import com.averonlabs.eshopverse.core.money.MoneyFormat;
import com.averonlabs.eshopverse.core.state.UiState;
import com.averonlabs.eshopverse.core.ui.view.StateContainerView;
import com.averonlabs.eshopverse.databinding.FragmentOrderDetailBinding;

/**
 * Screen M-16: Order Details.
 * Renders immutable snapshots: item names/unit prices as purchased, shipping address,
 * subtotal, shipping, total, payment status, and timeline events.
 */
public class OrderDetailFragment extends Fragment {

    private FragmentOrderDetailBinding binding;
    private OrderDetailViewModel viewModel;
    private OrderLineAdapter lineAdapter;
    private OrderTimelineAdapter timelineAdapter;
    private long orderId = -1;

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        binding = FragmentOrderDetailBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (getArguments() != null) {
            orderId = getArguments().getInt("orderId", -1);
            if (orderId == -1) {
                orderId = getArguments().getLong("orderId", -1);
            }
        }

        viewModel = new ViewModelProvider(this, new OrderViewModelFactory(requireContext())).get(OrderDetailViewModel.class);

        lineAdapter = new OrderLineAdapter();
        binding.rvDetailItems.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvDetailItems.setAdapter(lineAdapter);

        timelineAdapter = new OrderTimelineAdapter();
        binding.rvDetailTimeline.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvDetailTimeline.setAdapter(timelineAdapter);

        binding.stateContainerOrderDetail.setContentView(binding.scrollOrderDetailContent);

        observeViewModel();

        if (orderId > 0) {
            viewModel.loadOrder(orderId);
        } else {
            binding.stateContainerOrderDetail.setState(
                    StateContainerView.State.EMPTY,
                    getString(R.string.orders_not_found_message),
                    null,
                    null
            );
        }
    }

    private void observeViewModel() {
        viewModel.getOrderState().observe(getViewLifecycleOwner(), this::renderOrderState);

        viewModel.getAuthRequiredEvent().observe(getViewLifecycleOwner(), event -> {
            if (event != null && event.getContentIfNotHandled() != null) {
                NavController navController = NavHostFragment.findNavController(this);
                Bundle args = new Bundle();
                args.putInt("returnToNavId", R.id.nav_orders);
                navController.navigate(R.id.nav_auth, args);
            }
        });
    }

    private void renderOrderState(@Nullable UiState<Order> state) {
        if (state == null) return;

        switch (state.getStatus()) {
            case LOADING:
                binding.stateContainerOrderDetail.setState(StateContainerView.State.LOADING, null, null, null);
                break;
            case EMPTY:
                // Not-found or not-owned order state
                binding.stateContainerOrderDetail.setState(
                        StateContainerView.State.EMPTY,
                        getString(R.string.orders_not_found_message),
                        null,
                        null
                );
                break;
            case ERROR:
                int msgRes = state.getMessageRes() != 0 ? state.getMessageRes() : R.string.orders_error_heading;
                binding.stateContainerOrderDetail.setState(
                        StateContainerView.State.ERROR,
                        getString(msgRes),
                        state.isRetryable() ? getString(R.string.order_retry_button) : null,
                        state.isRetryable() ? () -> viewModel.loadOrder(orderId) : null
                );
                break;
            case CONTENT:
                binding.stateContainerOrderDetail.setState(StateContainerView.State.CONTENT, null, null, null);
                if (state.getData() != null) {
                    bindOrderDetails(state.getData());
                }
                break;
        }
    }

    private void bindOrderDetails(@NonNull Order order) {
        binding.tvDetailReference.setText(order.getReference());
        binding.tvDetailDate.setText(getString(R.string.order_date_label) + " " + order.getCreatedAt());

        binding.badgeDetailStatus.setText(
                getString(OrderStatusFormatter.getStatusTextRes(order.getStatus()))
        );
        binding.badgeDetailStatus.setStatus(OrderStatusFormatter.getBadgeStatus(order.getStatus()));

        binding.badgeDetailPaymentStatus.setText(
                getString(OrderStatusFormatter.getPaymentStatusTextRes(order.getPaymentStatus()))
        );
        binding.badgeDetailPaymentStatus.setStatus(
                OrderStatusFormatter.getPaymentBadgeStatus(order.getPaymentStatus())
        );

        // Items
        lineAdapter.setItems(order.getItems());

        // Shipping address
        ShippingAddress address = order.getShippingAddress();
        if (address != null) {
            binding.tvDetailShippingName.setText(address.getRecipientName());
            String addrText = address.getLine1();
            if (address.getLine2() != null && !address.getLine2().trim().isEmpty()) {
                addrText += ", " + address.getLine2();
            }
            addrText += "\n" + address.getCity() + ", " + address.getRegion() + " " + address.getPostalCode() + ", " + address.getCountry();
            binding.tvDetailShippingAddress.setText(addrText);
        }

        // Summary
        binding.tvDetailSubtotal.setFormattedPrice(MoneyFormat.format(order.getSubtotalMoney()));
        binding.tvDetailShipping.setFormattedPrice(MoneyFormat.format(order.getShippingMoney()));
        binding.tvDetailTotal.setFormattedPrice(MoneyFormat.format(order.getTotalMoney()));

        // Timeline
        if (order.getTimeline() != null && !order.getTimeline().isEmpty()) {
            binding.cardTimeline.setVisibility(View.VISIBLE);
            timelineAdapter.setEvents(order.getTimeline());
        } else {
            binding.cardTimeline.setVisibility(View.GONE);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
