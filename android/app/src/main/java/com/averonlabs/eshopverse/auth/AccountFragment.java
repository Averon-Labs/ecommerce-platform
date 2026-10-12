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
import com.averonlabs.eshopverse.core.model.User;
import com.averonlabs.eshopverse.core.state.UiState;
import com.averonlabs.eshopverse.core.ui.view.StateContainerView;
import com.averonlabs.eshopverse.databinding.FragmentAccountBinding;

/**
 * Screen M-07 / M-17: Customer Account & Profile.
 * Displays customer profile, supports name updates and sign-out per docs/android/PHASE-1-SPEC.md §D.
 */
public class AccountFragment extends Fragment {

    private FragmentAccountBinding binding;
    private AuthViewModel authViewModel;
    private SessionViewModel sessionViewModel;

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

        binding.stateContainer.setContentView(binding.scrollContent);

        setupTextWatchers();
        setupClickListeners();
        observeViewModels();
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
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        };

        binding.etFirstName.addTextChangedListener(clearErrorWatcher);
        binding.etLastName.addTextChangedListener(clearErrorWatcher);
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
            NavController navController = NavHostFragment.findNavController(this);
            Bundle args = new Bundle();
            args.putInt("returnToNavId", R.id.accountFragment);
            navController.navigate(R.id.nav_auth, args);
        });

        binding.btnGuestRegister.setOnClickListener(v -> {
            NavController navController = NavHostFragment.findNavController(this);
            Bundle args = new Bundle();
            args.putInt("returnToNavId", R.id.accountFragment);
            navController.navigate(R.id.nav_auth, args);
        });
    }

    private void observeViewModels() {
        sessionViewModel.getIsSignedIn().observe(getViewLifecycleOwner(), isSignedIn -> {
            if (binding == null) return;
            boolean signedIn = Boolean.TRUE.equals(isSignedIn);
            binding.layoutSignedIn.setVisibility(signedIn ? View.VISIBLE : View.GONE);
            binding.layoutGuestState.setVisibility(signedIn ? View.GONE : View.VISIBLE);
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

        authViewModel.getProfileUpdateState().observe(getViewLifecycleOwner(), this::handleProfileUpdateState);
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

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
