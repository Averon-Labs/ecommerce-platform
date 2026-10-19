package com.averonlabs.eshopverse.catalog.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.averonlabs.eshopverse.R;
import com.averonlabs.eshopverse.catalog.FavoritesAdapter;
import com.averonlabs.eshopverse.catalog.FavoritesRepository;
import com.averonlabs.eshopverse.core.model.ProductSummary;
import com.averonlabs.eshopverse.databinding.FragmentFavoritesBinding;

import java.util.List;

/**
 * Screen presenting all favorited/saved products.
 * Supports switching between 2-column Grid and single-column Linear list views.
 */
public class FavoritesFragment extends Fragment {

    private FragmentFavoritesBinding binding;
    private FavoritesAdapter favoritesAdapter;
    private boolean isGridMode = true;

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        binding = FragmentFavoritesBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setupRecyclerView();
        setupClickListeners();
        observeFavorites();
    }

    private void setupRecyclerView() {
        favoritesAdapter = new FavoritesAdapter(this::onProductClick);
        updateLayoutManager();
        binding.rvFavoritesList.setAdapter(favoritesAdapter);
    }

    private void updateLayoutManager() {
        if (isGridMode) {
            binding.rvFavoritesList.setLayoutManager(new GridLayoutManager(requireContext(), 2));
            binding.ivToggleLayoutIcon.setImageResource(R.drawable.ic_view_list);
            binding.btnToggleLayout.setContentDescription(getString(R.string.favorites_switch_to_list));
        } else {
            binding.rvFavoritesList.setLayoutManager(new LinearLayoutManager(requireContext()));
            binding.ivToggleLayoutIcon.setImageResource(R.drawable.ic_view_module);
            binding.btnToggleLayout.setContentDescription(getString(R.string.favorites_switch_to_grid));
        }
        favoritesAdapter.setGridMode(isGridMode);
    }

    private void setupClickListeners() {
        binding.btnBack.setOnClickListener(v -> {
            NavController navController = NavHostFragment.findNavController(this);
            if (!navController.popBackStack()) {
                navController.navigate(R.id.accountFragment);
            }
        });

        binding.btnToggleLayout.setOnClickListener(v -> {
            isGridMode = !isGridMode;
            updateLayoutManager();
        });

        binding.btnEmptyExplore.setOnClickListener(v -> {
            NavController navController = NavHostFragment.findNavController(this);
            navController.navigate(R.id.exploreFragment);
        });
    }

    private void observeFavorites() {
        FavoritesRepository repo = FavoritesRepository.getInstance(requireContext());
        repo.getFavorites().observe(getViewLifecycleOwner(), this::updateFavoritesUi);
    }

    private void updateFavoritesUi(@Nullable List<ProductSummary> items) {
        if (binding == null) return;

        int count = items != null ? items.size() : 0;
        binding.tvFavoritesCount.setText(count == 1 ? "1 item saved" : count + " items saved");

        if (count == 0) {
            binding.rvFavoritesList.setVisibility(View.GONE);
            binding.layoutEmptyState.setVisibility(View.VISIBLE);
            favoritesAdapter.submitList(null);
        } else {
            binding.rvFavoritesList.setVisibility(View.VISIBLE);
            binding.layoutEmptyState.setVisibility(View.GONE);
            favoritesAdapter.submitList(items);
        }
    }

    private void onProductClick(@NonNull ProductSummary product) {
        NavController navController = NavHostFragment.findNavController(this);
        Bundle args = new Bundle();
        args.putLong("productId", product.getId());
        try {
            navController.navigate(R.id.action_favoritesFragment_to_productDetailFragment, args);
        } catch (Exception e) {
            navController.navigate(R.id.nav_catalog, args);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
