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
import com.averonlabs.eshopverse.core.money.MoneyFormat;
import com.averonlabs.eshopverse.core.net.ApiClient;
import com.averonlabs.eshopverse.core.state.UiState;
import com.averonlabs.eshopverse.databinding.FragmentPaymentChoiceBinding;
import com.google.android.material.snackbar.Snackbar;

/**
 * Screen M-13: Simulated payment choice.
 * Per docs/android/PHASE-2-SPEC.md §E.5:
 * Outcome is a demo selector; NO card data is requested.
 * Two explicit actions: Simulate success and Simulate failure.
 */
public class PaymentChoiceFragment extends Fragment {

    private FragmentPaymentChoiceBinding binding;
    private CheckoutViewModel viewModel;

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        binding = FragmentPaymentChoiceBinding.inflate(inflater, container, false);
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
        binding.btnSimulateSuccess.setOnClickListener(v -> viewModel.submitPaymentChoice("success"));
        binding.btnSimulateFailure.setOnClickListener(v -> viewModel.submitPaymentChoice("failure"));
    }

    private void observeViewModel() {
        viewModel.getQuoteState().observe(getViewLifecycleOwner(), this::renderQuoteSummary);

        viewModel.getIsOperationInFlight().observe(getViewLifecycleOwner(), inFlight -> {
            boolean busy = Boolean.TRUE.equals(inFlight);
            binding.btnSimulateSuccess.setEnabled(!busy);
            binding.btnSimulateFailure.setEnabled(!busy);
            binding.progressPayment.setVisibility(busy ? View.VISIBLE : View.GONE);
        });

        viewModel.getNavigateToResultEvent().observe(getViewLifecycleOwner(), event -> {
            if (event.getContentIfNotHandled() != null) {
                NavController navController = NavHostFragment.findNavController(this);
                navController.navigate(R.id.orderResultFragment);
            }
        });

        viewModel.getErrorNoticeEvent().observe(getViewLifecycleOwner(), event -> {
            String notice = event.getContentIfNotHandled();
            if (notice != null && getView() != null) {
                Snackbar.make(getView(), notice, Snackbar.LENGTH_LONG).show();
            }
        });

        viewModel.getErrorResEvent().observe(getViewLifecycleOwner(), event -> {
            Integer resId = event.getContentIfNotHandled();
            if (resId != null && getView() != null) {
                Snackbar.make(getView(), resId, Snackbar.LENGTH_SHORT).show();
            }
        });
    }

    private void renderQuoteSummary(@Nullable UiState<CheckoutQuote> state) {
        if (state != null && state.getData() != null) {
            binding.tvPaymentAmountDue.setFormattedPrice(MoneyFormat.format(state.getData().getTotalMoney()));
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
