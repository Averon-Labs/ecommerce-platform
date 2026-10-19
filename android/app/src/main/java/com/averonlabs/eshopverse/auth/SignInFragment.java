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
import com.averonlabs.eshopverse.core.model.AuthSession;
import com.averonlabs.eshopverse.core.state.UiState;
import com.averonlabs.eshopverse.core.ui.view.StateContainerView;
import com.averonlabs.eshopverse.databinding.FragmentSignInBinding;
import com.google.android.material.snackbar.Snackbar;

/**
 * Screen M-06: Customer Sign In.
 * Supports email/password authentication, lockout handling, inline error mapping,
 * and return navigation per docs/android/PHASE-1-SPEC.md §D.
 */
public class SignInFragment extends Fragment {

    private FragmentSignInBinding binding;
    private AuthViewModel authViewModel;
    private SessionViewModel sessionViewModel;
    private int returnToNavId = R.id.homeFragment;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            returnToNavId = getArguments().getInt("returnToNavId", R.id.homeFragment);
        }
    }

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        binding = FragmentSignInBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        AuthViewModelFactory factory = new AuthViewModelFactory(requireContext());
        authViewModel = new ViewModelProvider(this, factory).get(AuthViewModel.class);
        sessionViewModel = new ViewModelProvider(requireActivity(), factory).get(SessionViewModel.class);

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
                    binding.tilEmail.setError(null);
                    binding.tilPassword.setError(null);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        };

        binding.etEmail.addTextChangedListener(clearErrorWatcher);
        binding.etPassword.addTextChangedListener(clearErrorWatcher);
    }

    private void setupClickListeners() {
        binding.btnSignIn.setOnClickListener(v -> {
            String email = binding.etEmail.getText() != null ? binding.etEmail.getText().toString() : "";
            String password = binding.etPassword.getText() != null ? binding.etPassword.getText().toString() : "";
            authViewModel.login(email, password);
        });

        binding.btnForgotPassword.setOnClickListener(v -> {
            NavController navController = NavHostFragment.findNavController(this);
            navController.navigate(R.id.action_signInFragment_to_requestPasswordResetFragment);
        });

        binding.btnGoogleSignIn.setOnClickListener(v -> {
            authViewModel.loginWithGoogle("mock_google_id_token_demo");
        });

        binding.btnToRegister.setOnClickListener(v -> {
            NavController navController = NavHostFragment.findNavController(this);
            Bundle args = new Bundle();
            args.putInt("returnToNavId", returnToNavId);
            navController.navigate(R.id.action_signInFragment_to_registerFragment, args);
        });
    }

    private void observeViewModel() {
        authViewModel.getEmailErrorRes().observe(getViewLifecycleOwner(), resId -> {
            if (binding != null) {
                binding.tilEmail.setError(resId != null ? getString(resId) : null);
            }
        });

        authViewModel.getPasswordErrorRes().observe(getViewLifecycleOwner(), resId -> {
            if (binding != null) {
                binding.tilPassword.setError(resId != null ? getString(resId) : null);
            }
        });

        authViewModel.getLockoutSeconds().observe(getViewLifecycleOwner(), seconds -> {
            if (binding == null) return;
            if (seconds != null && seconds > 0) {
                binding.tvLockoutBanner.setText(getString(R.string.auth_error_account_locked, seconds));
                binding.tvLockoutBanner.setVisibility(View.VISIBLE);
                binding.btnSignIn.setEnabled(false);
            } else {
                binding.tvLockoutBanner.setVisibility(View.GONE);
                binding.btnSignIn.setEnabled(true);
            }
        });

        authViewModel.getLoginState().observe(getViewLifecycleOwner(), this::handleLoginState);
    }

    private void handleLoginState(@Nullable UiState<AuthSession> state) {
        if (state == null || binding == null) return;

        if (state.isLoading()) {
            binding.progressBar.setVisibility(View.VISIBLE);
            binding.btnSignIn.setEnabled(false);
            binding.stateContainer.setState(StateContainerView.State.CONTENT, null, null, null);
        } else if (state.isContent()) {
            binding.progressBar.setVisibility(View.GONE);
            binding.btnSignIn.setEnabled(true);

            // Update app-wide session
            AuthSession session = state.getData();
            if (session != null) {
                sessionViewModel.setSession(session);
            }

            // Return navigation
            navigateAfterSuccess();
        } else if (state.isError()) {
            binding.progressBar.setVisibility(View.GONE);
            binding.btnSignIn.setEnabled(true);

            String errorMsg = state.getMessageRes() != 0 ? getString(state.getMessageRes()) : "";
            if (state.isRetryable()) {
                binding.stateContainer.setState(
                        StateContainerView.State.ERROR,
                        errorMsg,
                        getString(R.string.auth_action_retry),
                        () -> {
                            binding.stateContainer.setState(StateContainerView.State.CONTENT, null, null, null);
                            String email = binding.etEmail.getText() != null ? binding.etEmail.getText().toString() : "";
                            String password = binding.etPassword.getText() != null ? binding.etPassword.getText().toString() : "";
                            authViewModel.login(email, password);
                        }
                );
            } else {
                binding.stateContainer.setState(StateContainerView.State.CONTENT, null, null, null);
                // Keep email, clear password per D.4
                binding.etPassword.setText("");
                binding.tilPassword.setError(errorMsg);
            }
        }
    }

    private void navigateAfterSuccess() {
        NavController navController = NavHostFragment.findNavController(this);
        if (returnToNavId != 0 && returnToNavId != R.id.signInFragment) {
            navController.popBackStack(R.id.signInFragment, true);
            navController.navigate(returnToNavId);
        } else {
            navController.popBackStack(R.id.signInFragment, true);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
