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

        binding.swipeRefresh.setOnRefreshListener(() -> viewModel.refresh());

        binding.stateContainer.setOnEmptyActionListener(() -> {
            NavController navController = NavHostFragment.findNavController(this);
            navController.navigate(R.id.exploreFragment);
        });

        binding.stateContainer.setOnRetryClickListener(() -> viewModel.loadOrders());

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

        if (state.isLoading()) {
            binding.stateContainer.setVisibility(View.VISIBLE);
            binding.stateContainer.showLoading();
            binding.rvOrders.setVisibility(View.GONE);
        } else if (state.isEmpty()) {
            binding.stateContainer.setVisibility(View.VISIBLE);
            binding.stateContainer.showEmpty();
            binding.rvOrders.setVisibility(View.GONE);
        } else if (state.isError()) {
            binding.stateContainer.setVisibility(View.VISIBLE);
            int msgRes = state.getErrorMessageRes() != 0 ? state.getErrorMessageRes() : R.string.orders_error_heading;
            binding.stateContainer.showError(getString(msgRes));
            binding.rvOrders.setVisibility(View.GONE);
        } else if (state.isSuccess() && state.getData() != null) {
            binding.stateContainer.setVisibility(View.GONE);
            binding.rvOrders.setVisibility(View.VISIBLE);
            adapter.submitList(state.getData());
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
