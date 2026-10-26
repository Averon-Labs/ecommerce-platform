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
import com.averonlabs.eshopverse.catalog.FavoritesRepository;
import com.averonlabs.eshopverse.catalog.ProductGridAdapter;
import com.averonlabs.eshopverse.core.model.AuthSession;
import com.averonlabs.eshopverse.core.model.ProductSummary;
import com.averonlabs.eshopverse.core.model.User;
import com.averonlabs.eshopverse.core.state.UiState;
import com.averonlabs.eshopverse.core.ui.ThemeManager;
import com.averonlabs.eshopverse.core.ui.view.StateContainerView;
import com.averonlabs.eshopverse.databinding.FragmentAccountBinding;
import com.google.android.material.snackbar.Snackbar;

import java.util.Collections;

/**
 * Screen M-07 / M-17: Customer Account & Profile.
 * Displays customer profile when signed in, or direct sign-in form for guests,
 * supporting instant login, registration navigation, name updates, and sign-out.
 */
public class AccountFragment extends Fragment {

    private FragmentAccountBinding binding;
    private AuthViewModel authViewModel;
    private SessionViewModel sessionViewModel;
    private GoogleAuthManager googleAuthManager;

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        binding = FragmentAccountBinding.inflate(inflater, container, false);
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
        setupFavoritesSection();
        setupThemeSection();
        observeViewModels();
    }

    private void setupTextWatchers() {
        TextWatcher profileErrorWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (binding != null) {
                    binding.tilFirstName.setError(null);
                    binding.tilLastName.setError(null);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        };

        binding.etFirstName.addTextChangedListener(profileErrorWatcher);
        binding.etLastName.addTextChangedListener(profileErrorWatcher);

        TextWatcher guestErrorWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (binding != null) {
                    binding.tilGuestEmail.setError(null);
                    binding.tilGuestPassword.setError(null);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        };

        binding.etGuestEmail.addTextChangedListener(guestErrorWatcher);
        binding.etGuestPassword.addTextChangedListener(guestErrorWatcher);
    }

    private void setupClickListeners() {
        binding.btnSaveProfile.setOnClickListener(v -> {
            String firstName = binding.etFirstName.getText() != null ? binding.etFirstName.getText().toString() : "";
            String lastName = binding.etLastName.getText() != null ? binding.etLastName.getText().toString() : "";
            authViewModel.updateProfile(firstName, lastName);
        });

        binding.btnSignOut.setOnClickListener(v -> {
            sessionViewModel.signOut();
        });

        binding.btnGuestSignIn.setOnClickListener(v -> {
            String email = binding.etGuestEmail.getText() != null ? binding.etGuestEmail.getText().toString() : "";
            String password = binding.etGuestPassword.getText() != null ? binding.etGuestPassword.getText().toString() : "";
            authViewModel.login(email, password);
        });

        binding.btnGuestGoogleSignIn.setOnClickListener(v -> {
            if (getActivity() == null) return;
            googleAuthManager.startSignIn(requireActivity(), new GoogleAuthManager.GoogleAuthCallback() {
                @Override
                public void onSuccess(@NonNull String idToken) {
                    authViewModel.loginWithGoogle(idToken);
                }

                @Override
                public void onCancelled() {
                    // User dismissed credential picker
                }

                @Override
                public void onError(@NonNull String errorMessage) {
                    showAnchoredSnackbar(errorMessage);
                }
            });
        });

        binding.btnGuestForgotPassword.setOnClickListener(v -> {
            try {
                NavController navController = NavHostFragment.findNavController(this);
                navController.navigate(R.id.action_accountFragment_to_requestPasswordResetFragment);
            } catch (Exception e) {
                try {
                    NavHostFragment.findNavController(this).navigate(R.id.requestPasswordResetFragment);
                } catch (Exception ignored) {}
            }
        });

        binding.btnGuestRegister.setOnClickListener(v -> {
            try {
                NavController navController = NavHostFragment.findNavController(this);
                Bundle args = new Bundle();
                args.putInt("returnToNavId", R.id.accountFragment);
                navController.navigate(R.id.action_accountFragment_to_registerFragment, args);
            } catch (Exception e) {
                try {
                    NavHostFragment.findNavController(this).navigate(R.id.registerFragment);
                } catch (Exception ignored) {}
            }
        });
    }

    private void showAnchoredSnackbar(@androidx.annotation.StringRes int messageRes) {
        if (!isAdded() || getView() == null) return;
        Snackbar snackbar = Snackbar.make(binding.getRoot(), messageRes, Snackbar.LENGTH_LONG);
        if (getActivity() != null) {
            View bottomNav = getActivity().findViewById(R.id.card_bottom_nav);
            if (bottomNav != null && bottomNav.getVisibility() == View.VISIBLE) {
                snackbar.setAnchorView(bottomNav);
            }
        }
        snackbar.show();
    }

    private void showAnchoredSnackbar(@NonNull String message) {
        if (!isAdded() || getView() == null) return;
        Snackbar snackbar = Snackbar.make(binding.getRoot(), message, Snackbar.LENGTH_LONG);
        if (getActivity() != null) {
            View bottomNav = getActivity().findViewById(R.id.card_bottom_nav);
            if (bottomNav != null && bottomNav.getVisibility() == View.VISIBLE) {
                snackbar.setAnchorView(bottomNav);
            }
        }
        snackbar.show();
    }

    private void observeViewModels() {
        sessionViewModel.getIsSignedIn().observe(getViewLifecycleOwner(), isSignedIn -> {
            if (binding == null) return;
            boolean signedIn = Boolean.TRUE.equals(isSignedIn);
            binding.layoutSignedIn.setVisibility(signedIn ? View.VISIBLE : View.GONE);
            binding.layoutSignedInActions.setVisibility(signedIn ? View.VISIBLE : View.GONE);
            binding.layoutGuestState.setVisibility(signedIn ? View.GONE : View.VISIBLE);
            binding.tvAccountTitle.setText(signedIn ? R.string.auth_account_title : R.string.auth_sign_in_title);
        });

        sessionViewModel.getCurrentUser().observe(getViewLifecycleOwner(), user -> {
            if (binding == null || user == null) return;
            String fullName = user.getFirstName() + " " + user.getLastName();
            binding.tvUserName.setText(fullName.trim());
            binding.tvUserEmail.setText(user.getEmail());
            binding.tvUserRole.setText(user.getRole() != null ? user.getRole().name() : "CUSTOMER");

            // Update edit fields if currently empty
            if (binding.etFirstName.getText() == null || binding.etFirstName.getText().length() == 0) {
                binding.etFirstName.setText(user.getFirstName());
            }
            if (binding.etLastName.getText() == null || binding.etLastName.getText().length() == 0) {
                binding.etLastName.setText(user.getLastName());
            }
        });

        authViewModel.getFirstNameErrorRes().observe(getViewLifecycleOwner(), resId -> {
            if (binding != null) binding.tilFirstName.setError(resId != null ? getString(resId) : null);
        });

        authViewModel.getLastNameErrorRes().observe(getViewLifecycleOwner(), resId -> {
            if (binding != null) binding.tilLastName.setError(resId != null ? getString(resId) : null);
        });

        authViewModel.getEmailErrorRes().observe(getViewLifecycleOwner(), resId -> {
            if (binding != null) binding.tilGuestEmail.setError(resId != null ? getString(resId) : null);
        });

        authViewModel.getPasswordErrorRes().observe(getViewLifecycleOwner(), resId -> {
            if (binding != null) binding.tilGuestPassword.setError(resId != null ? getString(resId) : null);
        });

        authViewModel.getLockoutSeconds().observe(getViewLifecycleOwner(), seconds -> {
            if (binding == null) return;
            if (seconds != null && seconds > 0) {
                binding.tvGuestLockoutBanner.setText(getString(R.string.auth_error_account_locked, seconds));
                binding.tvGuestLockoutBanner.setVisibility(View.VISIBLE);
                binding.btnGuestSignIn.setEnabled(false);
            } else {
                binding.tvGuestLockoutBanner.setVisibility(View.GONE);
                binding.btnGuestSignIn.setEnabled(true);
            }
        });

        authViewModel.getLoginState().observe(getViewLifecycleOwner(), this::handleLoginState);
        authViewModel.getProfileUpdateState().observe(getViewLifecycleOwner(), this::handleProfileUpdateState);
    }

    private void handleLoginState(@Nullable UiState<AuthSession> state) {
        if (state == null || binding == null) return;

        if (state.isLoading()) {
            binding.progressGuestSignIn.setVisibility(View.VISIBLE);
            binding.btnGuestSignIn.setEnabled(false);
            binding.stateContainer.setState(StateContainerView.State.CONTENT, null, null, null);
        } else if (state.isContent()) {
            binding.progressGuestSignIn.setVisibility(View.GONE);
            binding.btnGuestSignIn.setEnabled(true);

            AuthSession session = state.getData();
            if (session != null) {
                sessionViewModel.setSession(session);
            }
            binding.etGuestPassword.setText("");
        } else if (state.isError()) {
            binding.progressGuestSignIn.setVisibility(View.GONE);
            binding.btnGuestSignIn.setEnabled(true);

            String errorMsg = state.getMessageRes() != 0 ? getString(state.getMessageRes()) : "";
            if (state.isRetryable()) {
                binding.stateContainer.setState(
                        StateContainerView.State.ERROR,
                        errorMsg,
                        getString(R.string.auth_action_retry),
                        () -> {
                            binding.stateContainer.setState(StateContainerView.State.CONTENT, null, null, null);
                            String email = binding.etGuestEmail.getText() != null ? binding.etGuestEmail.getText().toString() : "";
                            String password = binding.etGuestPassword.getText() != null ? binding.etGuestPassword.getText().toString() : "";
                            authViewModel.login(email, password);
                        }
                );
            } else {
                binding.stateContainer.setState(StateContainerView.State.CONTENT, null, null, null);
                binding.etGuestPassword.setText("");
                binding.tilGuestPassword.setError(errorMsg);
            }
        }
    }

    private void handleProfileUpdateState(@Nullable UiState<User> state) {
        if (state == null || binding == null) return;

        if (state.isLoading()) {
            binding.progressProfile.setVisibility(View.VISIBLE);
            binding.btnSaveProfile.setEnabled(false);
            binding.stateContainer.setState(StateContainerView.State.CONTENT, null, null, null);
        } else if (state.isContent()) {
            binding.progressProfile.setVisibility(View.GONE);
            binding.btnSaveProfile.setEnabled(true);
            User updatedUser = state.getData();
            if (updatedUser != null) {
                sessionViewModel.updateCurrentUser(updatedUser);
            }
            showAnchoredSnackbar(R.string.auth_feedback_profile_updated);
        } else if (state.isError()) {
            binding.progressProfile.setVisibility(View.GONE);
            binding.btnSaveProfile.setEnabled(true);

            String errorMsg = state.getMessageRes() != 0 ? getString(state.getMessageRes()) : "";
            if (state.isRetryable()) {
                binding.stateContainer.setState(
                        StateContainerView.State.ERROR,
                        errorMsg,
                        getString(R.string.auth_action_retry),
                        () -> {
                            binding.stateContainer.setState(StateContainerView.State.CONTENT, null, null, null);
                            binding.btnSaveProfile.performClick();
                        }
                );
            } else {
                binding.stateContainer.setState(StateContainerView.State.CONTENT, null, null, null);
            }
        }
    }

    private void setupFavoritesSection() {
        binding.cardFavoritesNav.setOnClickListener(v -> {
            try {
                NavHostFragment.findNavController(this).navigate(R.id.action_accountFragment_to_favoritesFragment);
            } catch (Exception e) {
                try {
                    NavHostFragment.findNavController(this).navigate(R.id.favoritesFragment);
                } catch (Exception ignored) {}
            }
        });

        FavoritesRepository.getInstance(requireContext()).getFavorites().observe(getViewLifecycleOwner(), list -> {
            if (binding == null) return;
            int count = list != null ? list.size() : 0;
            binding.tvFavoritesCountBadge.setText(count + " " + (count == 1 ? "item" : "items"));
        });
    }

    private void setupThemeSection() {
        updateThemeVisuals();
        binding.cardThemeToggle.setOnClickListener(v -> {
            ThemeManager.toggleTheme(requireContext());
            updateThemeVisuals();
        });
    }

    private void updateThemeVisuals() {
        if (binding == null || !isAdded()) return;
        boolean isDark = ThemeManager.isDarkMode(requireContext());
        binding.switchThemeMode.setChecked(isDark);
        binding.ivAccountThemeIcon.setImageResource(isDark ? R.drawable.ic_light_mode : R.drawable.ic_dark_mode);
        binding.tvThemeModeSubtitle.setText(isDark ? "Dark appearance active" : "Light appearance active");
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
