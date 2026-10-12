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
import com.averonlabs.eshopverse.core.model.OrderSummary;
import com.averonlabs.eshopverse.core.state.UiState;
import com.averonlabs.eshopverse.core.ui.view.StateContainerView;
import com.averonlabs.eshopverse.databinding.FragmentOrderListBinding;

import java.util.List;

/**
 * Screen M-15: My Orders (Order History).
 * Displays paginated list of customer-owned orders with status and USD totals.
 */
public class OrderListFragment extends Fragment implements OrderAdapter.OnOrderClickListener {

    private FragmentOrderListBinding binding;
    private OrderListViewModel viewModel;
    private OrderAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        binding = FragmentOrderListBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this, new OrderViewModelFactory()).get(OrderListViewModel.class);

        adapter = new OrderAdapter(this);
        binding.rvOrders.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvOrders.setAdapter(adapter);

        binding.stateContainerOrders.setContentView(binding.swipeRefresh);

        binding.swipeRefresh.setOnRefreshListener(() -> viewModel.refresh());

        observeViewModel();
        viewModel.loadOrders();
    }

    private void observeViewModel() {
        viewModel.getOrdersState().observe(getViewLifecycleOwner(), this::renderOrdersState);

        viewModel.getAuthRequiredEvent().observe(getViewLifecycleOwner(), v -> {
            NavController navController = NavHostFragment.findNavController(this);
            Bundle args = new Bundle();
            args.putInt("returnToNavId", R.id.nav_orders);
            navController.navigate(R.id.nav_auth, args);
        });
    }

    private void renderOrdersState(@Nullable UiState<List<OrderSummary>> state) {
        if (state == null) return;

        binding.swipeRefresh.setRefreshing(false);

        switch (state.getStatus()) {
            case LOADING:
                binding.stateContainerOrders.setState(StateContainerView.State.LOADING, null, null, null);
                break;
            case CONTENT:
                binding.stateContainerOrders.setState(StateContainerView.State.CONTENT, null, null, null);
                if (state.getData() != null) {
                    adapter.submitList(state.getData());
                }
                break;
            case EMPTY:
                binding.stateContainerOrders.setState(
                        StateContainerView.State.EMPTY,
                        getString(R.string.orders_empty_heading),
                        getString(R.string.orders_empty_action),
                        () -> NavHostFragment.findNavController(this).navigate(R.id.exploreFragment)
                );
                break;
            case ERROR:
                int msgRes = state.getMessageRes() != 0 ? state.getMessageRes() : R.string.orders_error_heading;
                binding.stateContainerOrders.setState(
                        StateContainerView.State.ERROR,
                        getString(msgRes),
                        state.isRetryable() ? getString(R.string.order_retry_button) : null,
                        state.isRetryable() ? () -> viewModel.loadOrders() : null
                );
                break;
        }
    }

    @Override
    public void onOrderClick(@NonNull OrderSummary order) {
        NavController navController = NavHostFragment.findNavController(this);
        Bundle args = new Bundle();
        args.putInt("orderId", order.getId());
        navController.navigate(R.id.action_orderList_to_orderDetail, args);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
