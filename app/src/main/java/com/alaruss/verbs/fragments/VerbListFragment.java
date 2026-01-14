package com.alaruss.verbs.fragments;

import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.alaruss.verbs.R;
import com.alaruss.verbs.adapters.VerbRecyclerAdapter;
import com.alaruss.verbs.databinding.FragmentVerbListBinding;
import com.alaruss.verbs.models.Verb;
import com.alaruss.verbs.premium.PremiumManager;
import com.alaruss.verbs.viewmodels.VerbListViewModel;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;


public class VerbListFragment extends Fragment {
    private VerbListFragmentListener mListener;

    private FragmentVerbListBinding binding;
    private VerbRecyclerAdapter mAdapter;
    private String filterQuery;
    private VerbListViewModel viewModel;
    private int searchDrawable, closeActiveDrawable, closeInactiveDrawable;
    private AdView mAdView;
    private boolean adLoaded = false;
    private boolean lastKnownPremiumStatus = false;
    // Store listener references for cleanup
    private TextWatcher searchTextWatcher;
    private View.OnTouchListener searchTouchListener;
    private TextView.OnEditorActionListener searchEditorActionListener;

    public VerbListFragment() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            searchDrawable = R.drawable.ic_search_edit_text;
            closeActiveDrawable = R.drawable.ic_close_active;
            closeInactiveDrawable = R.drawable.ic_close_inactive;
        } else {
            searchDrawable = android.R.drawable.ic_menu_search;
            closeActiveDrawable = android.R.drawable.ic_delete;
            closeInactiveDrawable = android.R.drawable.ic_delete;
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        MenuItem searchItem = menu.findItem(R.id.action_search);
        MenuItem favoriteItem = menu.findItem(R.id.action_favorite);
        if (searchItem != null) {
            searchItem.setVisible(false);
        }
        if (favoriteItem != null) {
            favoriteItem.setVisible(false);
        }
    }


    @Override
    public void onResume() {
        super.onResume();
        if (getActivity() != null) {
            getActivity().setTitle(R.string.app_name);
        }
        // Favorites refresh is now handled automatically by LiveData

        // Resume ad and refresh visibility in case premium status changed
        if (mAdView != null) {
            mAdView.resume();
            loadAd();
        }
    }

    @Override
    public void onPause() {
        if (mAdView != null) {
            mAdView.pause();
        }
        super.onPause();
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        filterQuery = getArguments() != null ? getArguments().getString(getString(R.string.EXTRA_QUERY)) : null;

        // Initialize ViewModel
        viewModel = new ViewModelProvider(this).get(VerbListViewModel.class);
        if (filterQuery != null) {
            viewModel.setSearchQuery(filterQuery);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentVerbListBinding.inflate(inflater, container, false);

        // Set up RecyclerView (binding.list is actually a RecyclerView now)
        RecyclerView recyclerView = (RecyclerView) binding.list;
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setHasFixedSize(true);
        recyclerView.setVerticalScrollBarEnabled(true);

        // Initialize adapter with click listener
        mAdapter = new VerbRecyclerAdapter((verb, position) -> {
            onVerbSelected(position);
        });
        recyclerView.setAdapter(mAdapter);

        // Add divider between items
        DividerItemDecoration divider = new DividerItemDecoration(getContext(), LinearLayoutManager.VERTICAL);
        recyclerView.addItemDecoration(divider);

        // Observe filtered verbs from ViewModel
        viewModel.getFilteredVerbs().observe(getViewLifecycleOwner(), verbs -> {
            if (verbs != null) {
                mAdapter.setVerbs(verbs);
            }
        });
        searchTextWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() > 0) {
                    binding.searchList.setCompoundDrawablesWithIntrinsicBounds(
                            searchDrawable, 0,
                            closeActiveDrawable, 0
                    );
                } else {
                    binding.searchList.setCompoundDrawablesWithIntrinsicBounds(
                            searchDrawable, 0,
                            closeInactiveDrawable, 0
                    );
                }
                // Use ViewModel for filtering instead of adapter
                viewModel.setSearchQuery(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        };
        binding.searchList.addTextChangedListener(searchTextWatcher);

        searchTouchListener = new View.OnTouchListener() {
            final int DRAWABLE_LEFT = 0;
            final int DRAWABLE_RIGHT = 2;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    int leftEdgeOfRightDrawable = binding.searchList.getRight()
                            - binding.searchList.getCompoundDrawables()[DRAWABLE_RIGHT].getBounds().width();
                    int rightEdgeOfLeftDrawable = binding.searchList.getLeft()
                            + binding.searchList.getCompoundDrawables()[DRAWABLE_LEFT].getBounds().width();
                    if (event.getRawX() >= leftEdgeOfRightDrawable) {
                        binding.searchList.setText("");
                        return true;
                    } else if (event.getRawX() <= rightEdgeOfLeftDrawable) {
                        if (binding.searchList.getText().length() > 0 && mAdapter.getItemCount() > 0) {
                            onVerbSelected(0);
                        }
                        return true;
                    }
                }
                return false;
            }
        };
        binding.searchList.setOnTouchListener(searchTouchListener);

        searchEditorActionListener = new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    if (mAdapter.getItemCount() > 0) {
                        onVerbSelected(0);
                    }
                    return true;
                }
                return false;
            }
        };
        binding.searchList.setOnEditorActionListener(searchEditorActionListener);
        binding.searchList.requestFocus();

        // Initialize and load ads (only for non-premium users)
        mAdView = binding.adView;
        loadAd();

        return binding.getRoot();
    }

    private void loadAd() {
        if (mListener == null || mAdView == null) return;
        PremiumManager premiumManager = mListener.getPremiumManager();
        boolean isPremium = premiumManager.isPremium();

        if (isPremium) {
            mAdView.setVisibility(View.GONE);
            adLoaded = false;
        } else {
            mAdView.setVisibility(View.VISIBLE);
            // Only load if not already loaded or premium status changed
            if (!adLoaded || lastKnownPremiumStatus != isPremium) {
                AdRequest adRequest = new AdRequest.Builder().build();
                mAdView.loadAd(adRequest);
                adLoaded = true;
            }
        }
        lastKnownPremiumStatus = isPremium;
    }

    @Override
    public void onDestroyView() {
        if (binding != null && binding.searchList != null) {
            if (searchTextWatcher != null) {
                binding.searchList.removeTextChangedListener(searchTextWatcher);
                searchTextWatcher = null;
            }
            if (searchTouchListener != null) {
                binding.searchList.setOnTouchListener(null);
                searchTouchListener = null;
            }
            if (searchEditorActionListener != null) {
                binding.searchList.setOnEditorActionListener(null);
                searchEditorActionListener = null;
            }
        }
        // Clean up AdView
        if (mAdView != null) {
            mAdView.destroy();
            mAdView = null;
        }
        adLoaded = false;
        super.onDestroyView();
        binding = null;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        try {
            mListener = (VerbListFragmentListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString()
                    + " must implement VerbListFragmentListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    private void onVerbSelected(int position) {
        if (null != mListener && mAdapter.getVerbs().size() > position) {
            Verb verb = mAdapter.getVerbs().get(position);
            mListener.onVerbListSelected(verb.getId());
        }
    }

    public interface VerbListFragmentListener {
        void onVerbListSelected(int verbId);

        PremiumManager getPremiumManager();
    }
}
