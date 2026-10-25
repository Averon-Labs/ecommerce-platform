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
import com.averonlabs.eshopverse.databinding.FragmentRegisterBinding;
import com.google.android.material.snackbar.Snackbar;

import java.util.Map;

/**
 * Screen M-07: Customer Registration.
 * Handles customer sign-up with client & server validation per docs/android/PHASE-1-SPEC.md §D.
 */
public class RegisterFragment extends Fragment {

    private FragmentRegisterBinding binding;
    private AuthViewModel authViewModel;
    private SessionViewModel sessionViewModel;
    private GoogleAuthManager googleAuthManager;
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
        binding = FragmentRegisterBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        AuthViewModelFactory factory = new AuthViewModelFactory(requireContext());
        authViewModel = new ViewModelProvider(this, factory).get(AuthViewModel.class);
        sessionViewModel = new ViewModelProvider(requireActivity(), factory).get(SessionViewModel.class);
        googleAuthManager = new GoogleAuthManager(requireContext());

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
                    binding.tilFirstName.setError(null);
                    binding.tilLastName.setError(null);
                    binding.tilEmail.setError(null);
                    binding.tilPassword.setError(null);
                    binding.tilConfirmPassword.setError(null);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        };

        binding.etFirstName.addTextChangedListener(clearErrorWatcher);
        binding.etLastName.addTextChangedListener(clearErrorWatcher);
        binding.etEmail.addTextChangedListener(clearErrorWatcher);
        binding.etPassword.addTextChangedListener(clearErrorWatcher);
        binding.etConfirmPassword.addTextChangedListener(clearErrorWatcher);
    }

    private void setupClickListeners() {
        binding.btnRegister.setOnClickListener(v -> {
            String firstName = binding.etFirstName.getText() != null ? binding.etFirstName.getText().toString() : "";
            String lastName = binding.etLastName.getText() != null ? binding.etLastName.getText().toString() : "";
            String email = binding.etEmail.getText() != null ? binding.etEmail.getText().toString() : "";
            String password = binding.etPassword.getText() != null ? binding.etPassword.getText().toString() : "";
            String confirmPassword = binding.etConfirmPassword.getText() != null ? binding.etConfirmPassword.getText().toString() : "";

            authViewModel.register(email, password, confirmPassword, firstName, lastName);
        });

        binding.btnGoogleSignIn.setOnClickListener(v -> {
            if (getActivity() == null) return;
            googleAuthManager.startSignIn(requireActivity(), new GoogleAuthManager.GoogleAuthCallback() {
                @Override
                public void onSuccess(@NonNull String idToken) {
                    authViewModel.loginWithGoogle(idToken);
                }

                @Override
                public void onCancelled() {
                    // User dismissed the credential picker; keep form interactive
                }

                @Override
                public void onError(@NonNull String errorMessage) {
                    if (binding != null) {
                        Snackbar.make(binding.getRoot(), errorMessage, Snackbar.LENGTH_LONG).show();
                    }
                }
            });
        });

        binding.btnToSignIn.setOnClickListener(v -> {
            NavController navController = NavHostFragment.findNavController(this);
            boolean popped = navController.popBackStack(R.id.signInFragment, false);
            if (!popped) {
                Bundle args = new Bundle();
                args.putInt("returnToNavId", returnToNavId);
                navController.navigate(R.id.signInFragment, args);
            }
        });
    }

    private void observeViewModel() {
        authViewModel.getFirstNameErrorRes().observe(getViewLifecycleOwner(), resId -> {
            if (binding != null) binding.tilFirstName.setError(resId != null ? getString(resId) : null);
        });

        authViewModel.getLastNameErrorRes().observe(getViewLifecycleOwner(), resId -> {
            if (binding != null) binding.tilLastName.setError(resId != null ? getString(resId) : null);
        });

        authViewModel.getEmailErrorRes().observe(getViewLifecycleOwner(), resId -> {
            if (binding != null) binding.tilEmail.setError(resId != null ? getString(resId) : null);
        });

        authViewModel.getPasswordErrorRes().observe(getViewLifecycleOwner(), resId -> {
            if (binding != null) binding.tilPassword.setError(resId != null ? getString(resId) : null);
        });

        authViewModel.getConfirmPasswordErrorRes().observe(getViewLifecycleOwner(), resId -> {
            if (binding != null) binding.tilConfirmPassword.setError(resId != null ? getString(resId) : null);
        });

        authViewModel.getServerFieldErrors().observe(getViewLifecycleOwner(), this::mapServerFieldErrors);

        authViewModel.getRegisterState().observe(getViewLifecycleOwner(), this::handleRegisterState);
        authViewModel.getLoginState().observe(getViewLifecycleOwner(), this::handleRegisterState);
    }

    private void mapServerFieldErrors(@Nullable Map<String, String> fieldErrors) {
        if (fieldErrors == null || binding == null) return;
        if (fieldErrors.containsKey("first_name")) {
            binding.tilFirstName.setError(fieldErrors.get("first_name"));
        }
        if (fieldErrors.containsKey("last_name")) {
            binding.tilLastName.setError(fieldErrors.get("last_name"));
        }
        if (fieldErrors.containsKey("email")) {
            binding.tilEmail.setError(fieldErrors.get("email"));
        }
        if (fieldErrors.containsKey("password")) {
            binding.tilPassword.setError(fieldErrors.get("password"));
        }
    }

    private void handleRegisterState(@Nullable UiState<AuthSession> state) {
        if (state == null || binding == null) return;

        if (state.isLoading()) {
            binding.progressBar.setVisibility(View.VISIBLE);
            binding.btnRegister.setEnabled(false);
            binding.stateContainer.setState(StateContainerView.State.CONTENT, null, null, null);
        } else if (state.isContent()) {
            binding.progressBar.setVisibility(View.GONE);
            binding.btnRegister.setEnabled(true);

            AuthSession session = state.getData();
            if (session != null) {
                sessionViewModel.setSession(session);
            }

            NavController navController = NavHostFragment.findNavController(this);
            int targetId = (returnToNavId != 0 && returnToNavId != R.id.signInFragment && returnToNavId != R.id.registerFragment)
                    ? returnToNavId : R.id.accountFragment;
            boolean popped = navController.popBackStack(targetId, false);
            if (!popped) {
                if (!navController.popBackStack()) {
                    navController.navigate(targetId);
                }
            }
        } else if (state.isError()) {
            binding.progressBar.setVisibility(View.GONE);
            binding.btnRegister.setEnabled(true);

            String errorMsg = state.getMessageRes() != 0 ? getString(state.getMessageRes()) : "";
            if (state.isRetryable()) {
                binding.stateContainer.setState(
                        StateContainerView.State.ERROR,
                        errorMsg,
                        getString(R.string.auth_action_retry),
                        () -> {
                            binding.stateContainer.setState(StateContainerView.State.CONTENT, null, null, null);
                            binding.btnRegister.performClick();
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
