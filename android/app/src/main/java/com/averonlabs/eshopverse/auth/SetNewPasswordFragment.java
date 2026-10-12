package com.averonlabs.eshopverse.auth;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
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
import com.averonlabs.eshopverse.core.state.UiState;
import com.averonlabs.eshopverse.core.ui.view.StateContainerView;
import com.averonlabs.eshopverse.databinding.FragmentSetNewPasswordBinding;

/**
 * Screen M-09: Set New Password.
 * Submits reset token and new password, handles invalid token recovery per docs/android/PHASE-1-SPEC.md §D.
 */
public class SetNewPasswordFragment extends Fragment {

    private FragmentSetNewPasswordBinding binding;
    private AuthViewModel authViewModel;

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        binding = FragmentSetNewPasswordBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        AuthViewModelFactory factory = new AuthViewModelFactory(requireContext());
        authViewModel = new ViewModelProvider(this, factory).get(AuthViewModel.class);

        binding.stateContainer.setContentView(binding.scrollContent);

        setupTextWatchers();
        setupClickListeners();
        observeViewModel();
    }

    private void setupTextWatchers() {
        TextWatcher clearErrorWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (binding != null) {
                    binding.tilResetToken.setError(null);
                    binding.tilPassword.setError(null);
                    binding.tilConfirmPassword.setError(null);
                    binding.cardInvalidToken.setVisibility(View.GONE);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        };

        binding.etResetToken.addTextChangedListener(clearErrorWatcher);
        binding.etPassword.addTextChangedListener(clearErrorWatcher);
        binding.etConfirmPassword.addTextChangedListener(clearErrorWatcher);
    }

    private void setupClickListeners() {
        binding.btnSubmitNewPassword.setOnClickListener(v -> {
            String token = binding.etResetToken.getText() != null ? binding.etResetToken.getText().toString() : "";
            String password = binding.etPassword.getText() != null ? binding.etPassword.getText().toString() : "";
            String confirmPassword = binding.etConfirmPassword.getText() != null ? binding.etConfirmPassword.getText().toString() : "";

            authViewModel.confirmPasswordReset(token, password, confirmPassword);
        });

        binding.btnBackToRequestReset.setOnClickListener(v -> {
            NavController navController = NavHostFragment.findNavController(this);
            navController.navigate(R.id.action_setNewPasswordFragment_to_requestPasswordResetFragment);
        });

        binding.btnGoToSignIn.setOnClickListener(v -> {
            NavController navController = NavHostFragment.findNavController(this);
            navController.navigate(R.id.action_setNewPasswordFragment_to_signInFragment);
        });
    }

    private void observeViewModel() {
        authViewModel.getResetTokenErrorRes().observe(getViewLifecycleOwner(), resId -> {
            if (binding == null) return;
            if (resId != null) {
                binding.tilResetToken.setError(getString(resId));
                // Show recovery card linking back to M-08
                binding.cardInvalidToken.setVisibility(View.VISIBLE);
            }
        });

        authViewModel.getPasswordErrorRes().observe(getViewLifecycleOwner(), resId -> {
            if (binding != null) {
                binding.tilPassword.setError(resId != null ? getString(resId) : null);
            }
        });

        authViewModel.getConfirmPasswordErrorRes().observe(getViewLifecycleOwner(), resId -> {
            if (binding != null) {
                binding.tilConfirmPassword.setError(resId != null ? getString(resId) : null);
            }
        });

        authViewModel.getResetConfirmState().observe(getViewLifecycleOwner(), this::handleResetConfirmState);
    }

    private void handleResetConfirmState(@Nullable UiState<Void> state) {
        if (state == null || binding == null) return;

        if (state.isLoading()) {
            binding.progressBar.setVisibility(View.VISIBLE);
            binding.btnSubmitNewPassword.setEnabled(false);
            binding.stateContainer.setState(StateContainerView.State.CONTENT, null, null, null);
        } else if (state.isContent()) {
            binding.progressBar.setVisibility(View.GONE);
            binding.btnSubmitNewPassword.setEnabled(true);
            binding.layoutSetPasswordForm.setVisibility(View.GONE);
            binding.cardInvalidToken.setVisibility(View.GONE);
            binding.cardSuccess.setVisibility(View.VISIBLE);
        } else if (state.isError()) {
            binding.progressBar.setVisibility(View.GONE);
            binding.btnSubmitNewPassword.setEnabled(true);

            String errorMsg = state.getMessageRes() != 0 ? getString(state.getMessageRes()) : "";
            if (state.isRetryable()) {
                binding.stateContainer.setState(
                        StateContainerView.State.ERROR,
                        errorMsg,
                        getString(R.string.auth_action_retry),
                        () -> {
                            binding.stateContainer.setState(StateContainerView.State.CONTENT, null, null, null);
                            binding.btnSubmitNewPassword.performClick();
                        }
                );
            } else {
                binding.stateContainer.setState(StateContainerView.State.CONTENT, null, null, null);
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
