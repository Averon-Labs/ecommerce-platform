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
import com.averonlabs.eshopverse.core.net.ApiClient;
import com.averonlabs.eshopverse.databinding.FragmentShippingAddressBinding;
import com.google.android.material.snackbar.Snackbar;

import java.util.Map;

/**
 * Screen M-11: Shipping Address input for checkout.
 * Per docs/android/PHASE-2-SPEC.md §E.3:
 * Validates fields before proceeding. Addresses are not stored locally.
 */
public class ShippingAddressFragment extends Fragment {

    private FragmentShippingAddressBinding binding;
    private CheckoutViewModel viewModel;

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        binding = FragmentShippingAddressBinding.inflate(inflater, container, false);
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
        binding.btnContinueToReview.setOnClickListener(v -> {
            String recipient = binding.etRecipientName.getText() != null
                    ? binding.etRecipientName.getText().toString() : "";
            String line1 = binding.etLine1.getText() != null
                    ? binding.etLine1.getText().toString() : "";
            String line2 = binding.etLine2.getText() != null
                    ? binding.etLine2.getText().toString() : "";
            String city = binding.etCity.getText() != null
                    ? binding.etCity.getText().toString() : "";
            String region = binding.etRegion.getText() != null
                    ? binding.etRegion.getText().toString() : "";
            String postal = binding.etPostalCode.getText() != null
                    ? binding.etPostalCode.getText().toString() : "";
            String country = binding.etCountry.getText() != null
                    ? binding.etCountry.getText().toString() : "";

            viewModel.submitShippingAddress(recipient, line1, line2, city, region, postal, country);
        });
    }

    private void observeViewModel() {
        viewModel.getAddressErrors().observe(getViewLifecycleOwner(), this::renderAddressErrors);

        viewModel.getIsOperationInFlight().observe(getViewLifecycleOwner(), inFlight -> {
            binding.btnContinueToReview.setEnabled(!Boolean.TRUE.equals(inFlight));
        });

        viewModel.getNavigateToReviewEvent().observe(getViewLifecycleOwner(), event -> {
            if (Boolean.TRUE.equals(event.getContentIfNotHandled())) {
                NavController navController = NavHostFragment.findNavController(this);
                navController.navigate(R.id.orderReviewFragment);
            }
        });

        viewModel.getAuthRequiredEvent().observe(getViewLifecycleOwner(), event -> {
            if (Boolean.TRUE.equals(event.getContentIfNotHandled())) {
                Bundle args = new Bundle();
                args.putInt("returnToNavId", R.id.nav_checkout);
                NavHostFragment.findNavController(this).navigate(R.id.nav_auth, args);
            }
        });

        viewModel.getCartConflictEvent().observe(getViewLifecycleOwner(), event -> {
            String message = event.getContentIfNotHandled();
            if (message != null && getView() != null) {
                Snackbar.make(getView(), message, Snackbar.LENGTH_LONG).show();
                NavHostFragment.findNavController(this).popBackStack();
            }
        });

        viewModel.getErrorResEvent().observe(getViewLifecycleOwner(), event -> {
            Integer resId = event.getContentIfNotHandled();
            if (resId != null && getView() != null) {
                Snackbar.make(getView(), resId, Snackbar.LENGTH_SHORT).show();
            }
        });
    }

    private void renderAddressErrors(@Nullable Map<String, Integer> errors) {
        clearFieldErrors();
        if (errors == null || errors.isEmpty()) {
            return;
        }

        if (errors.containsKey(AddressValidator.FIELD_RECIPIENT_NAME)) {
            binding.tilRecipientName.setError(getString(errors.get(AddressValidator.FIELD_RECIPIENT_NAME)));
        }
        if (errors.containsKey(AddressValidator.FIELD_LINE1)) {
            binding.tilLine1.setError(getString(errors.get(AddressValidator.FIELD_LINE1)));
        }
        if (errors.containsKey(AddressValidator.FIELD_LINE2)) {
            binding.tilLine2.setError(getString(errors.get(AddressValidator.FIELD_LINE2)));
        }
        if (errors.containsKey(AddressValidator.FIELD_CITY)) {
            binding.tilCity.setError(getString(errors.get(AddressValidator.FIELD_CITY)));
        }
        if (errors.containsKey(AddressValidator.FIELD_REGION)) {
            binding.tilRegion.setError(getString(errors.get(AddressValidator.FIELD_REGION)));
        }
        if (errors.containsKey(AddressValidator.FIELD_POSTAL_CODE)) {
            binding.tilPostalCode.setError(getString(errors.get(AddressValidator.FIELD_POSTAL_CODE)));
        }
        if (errors.containsKey(AddressValidator.FIELD_COUNTRY)) {
            binding.tilCountry.setError(getString(errors.get(AddressValidator.FIELD_COUNTRY)));
        }
    }

    private void clearFieldErrors() {
        binding.tilRecipientName.setError(null);
        binding.tilLine1.setError(null);
        binding.tilLine2.setError(null);
        binding.tilCity.setError(null);
        binding.tilRegion.setError(null);
        binding.tilPostalCode.setError(null);
        binding.tilCountry.setError(null);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
