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
import com.averonlabs.eshopverse.databinding.FragmentRequestPasswordResetBinding;

/**
 * Screen M-08: Request Password Reset.
 * Submits email for password reset instructions and displays neutral confirmation per docs/android/PHASE-1-SPEC.md §D.
 */
public class RequestPasswordResetFragment extends Fragment {

    private FragmentRequestPasswordResetBinding binding;
    private AuthViewModel authViewModel;

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        binding = FragmentRequestPasswordResetBinding.inflate(inflater, container, false);
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
        binding.etEmail.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (binding != null) {
                    binding.tilEmail.setError(null);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void setupClickListeners() {
        binding.btnSendReset.setOnClickListener(v -> {
            String email = binding.etEmail.getText() != null ? binding.etEmail.getText().toString() : "";
            authViewModel.requestPasswordReset(email);
        });

        binding.btnEnterToken.setOnClickListener(v -> {
            try {
                NavController navController = NavHostFragment.findNavController(this);
                navController.navigate(R.id.action_requestPasswordResetFragment_to_setNewPasswordFragment);
            } catch (Exception e) {
                try {
                    NavHostFragment.findNavController(this).navigate(R.id.setNewPasswordFragment);
                } catch (Exception ignored) {}
            }
        });

        binding.btnToSignIn.setOnClickListener(v -> {
            NavController navController = NavHostFragment.findNavController(this);
            navController.popBackStack();
        });
    }

    private void observeViewModel() {
        authViewModel.getEmailErrorRes().observe(getViewLifecycleOwner(), resId -> {
            if (binding != null) {
                binding.tilEmail.setError(resId != null ? getString(resId) : null);
            }
        });

        authViewModel.getResetRequestState().observe(getViewLifecycleOwner(), this::handleResetRequestState);
    }

    private void handleResetRequestState(@Nullable UiState<Void> state) {
        if (state == null || binding == null) return;

        if (state.isLoading()) {
            binding.progressBar.setVisibility(View.VISIBLE);
            binding.btnSendReset.setEnabled(false);
            binding.stateContainer.setState(StateContainerView.State.CONTENT, null, null, null);
        } else if (state.isContent()) {
            binding.progressBar.setVisibility(View.GONE);
            binding.btnSendReset.setEnabled(true);
            binding.layoutRequestForm.setVisibility(View.GONE);
            binding.cardConfirmation.setVisibility(View.VISIBLE);
        } else if (state.isError()) {
            binding.progressBar.setVisibility(View.GONE);
            binding.btnSendReset.setEnabled(true);

            String errorMsg = state.getMessageRes() != 0 ? getString(state.getMessageRes()) : "";
            if (state.isRetryable()) {
                binding.stateContainer.setState(
                        StateContainerView.State.ERROR,
                        errorMsg,
                        getString(R.string.auth_action_retry),
                        () -> {
                            binding.stateContainer.setState(StateContainerView.State.CONTENT, null, null, null);
                            binding.btnSendReset.performClick();
                        }
                );
            } else {
                binding.stateContainer.setState(StateContainerView.State.CONTENT, null, null, null);
                binding.tilEmail.setError(errorMsg);
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
