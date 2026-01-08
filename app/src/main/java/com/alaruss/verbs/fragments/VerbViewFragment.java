package com.alaruss.verbs.fragments;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.preference.PreferenceManager;
import com.google.android.material.tabs.TabLayout;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;
import androidx.appcompat.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.alaruss.verbs.R;
import com.alaruss.verbs.models.Verb;
import com.alaruss.verbs.premium.BillingManager;
import com.alaruss.verbs.premium.PremiumManager;
import com.alaruss.verbs.premium.PurchaseDialogHelper;
import com.alaruss.verbs.viewmodels.VerbDetailViewModel;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;


public class VerbViewFragment extends Fragment {
    Verb mVerb;
    VerbViewFragmentListener mListener;
    VerbDetailViewModel viewModel;
    private TextView mTranslateView;
    private int starDrawable, starInactiveDrawable;
    private SectionPagerAdapter mAdapter;
    private ViewPager mViewPager;
    private AdView mAdView;

    public VerbViewFragment() {
        starDrawable = R.drawable.ic_star;
        starInactiveDrawable = R.drawable.ic_star_inactive;
    }


    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mVerb != null && getActivity() != null) {
            getActivity().setTitle(mVerb.getInfinitive());
        }
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
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        try {
            mListener = (VerbViewFragmentListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString()
                    + " must implement VerbViewFragmentListener");
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        int verbId = getArguments() != null ? getArguments().getInt(getString(R.string.EXTRA_ID)) : 0;

        // Initialize ViewModel
        viewModel = new ViewModelProvider(this).get(VerbDetailViewModel.class);

        if (verbId == 0) { // didn't find verb
            mListener.onVerbViewFinished();
        } else {
            viewModel.setVerbId(verbId);
        }
    }

    private void setupVerbObserver() {
        viewModel.getVerb().observe(getViewLifecycleOwner(), verb -> {
            if (verb != null) {
                mVerb = verb;
                // Update last access when verb is loaded
                viewModel.updateLastAccess(verb);
                if (mTranslateView != null) {
                    mTranslateView.setText(getTranslationText());
                }
                if (mAdapter != null) {
                    mAdapter.notifyDataSetChanged();
                }
                Activity activity = getActivity();
                if (activity != null && mVerb != null) {
                    activity.setTitle(mVerb.getInfinitive());
                    activity.invalidateOptionsMenu();
                }
            }
        });
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_verb_view, container, false);
        TabLayout tabLayout = (TabLayout) view.findViewById(R.id.tab_layout);
        mViewPager = (ViewPager) view.findViewById(R.id.tab_pager);
        mAdapter = new SectionPagerAdapter();
        mViewPager.setAdapter(mAdapter);
        tabLayout.setupWithViewPager(mViewPager);
        mTranslateView = (TextView) view.findViewById(R.id.translation_text);
        mTranslateView.setText(getTranslationText());
        mTranslateView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                final int DRAWABLE_RIGHT = 2;
                int action = event.getAction();

                if (action == MotionEvent.ACTION_DOWN) {
                    if (mVerb == null) return false;
                    Activity activity = getActivity();
                    if (activity == null) return false;
                    int leftEdgeOfRightDrawable = mTranslateView.getRight()
                            - mTranslateView.getCompoundDrawables()[DRAWABLE_RIGHT].getBounds().width();
                    if (event.getRawX() >= leftEdgeOfRightDrawable) {

                        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
                        LayoutInflater dialogInflater = activity.getLayoutInflater();
                        View dialogView = dialogInflater.inflate(R.layout.dialog_translation_edit, null);
                        final EditText translationEdit = (EditText) dialogView.findViewById(R.id.translation_edit);
                        final String currentTranslation = getTranslationText();
                        translationEdit.setText(currentTranslation);
                        translationEdit.setSelection(currentTranslation.length());
                        builder.setView(dialogView)
                                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                        String newTranslation = translationEdit.getText().toString();
                                        if (!newTranslation.equals(currentTranslation)) {
                                            setTranslationText(newTranslation);
                                        }
                                    }
                                });
                        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                // Nothing to do
                            }
                        });
                        AlertDialog dialog = builder.create();
                        dialog.show();


                        return true;
                    }
                }
                return false;
            }
        });

        // Initialize and load ads (only for non-premium users)
        mAdView = view.findViewById(R.id.adView);
        loadAd();

        if (viewModel.getVerbId() > 0) {
            setupVerbObserver();
        }

        return view;
    }

    private void loadAd() {
        if (mListener == null) return;
        PremiumManager premiumManager = mListener.getPremiumManager();
        if (premiumManager.isPremium()) {
            mAdView.setVisibility(View.GONE);
        } else {
            mAdView.setVisibility(View.VISIBLE);
            AdRequest adRequest = new AdRequest.Builder().build();
            mAdView.loadAd(adRequest);
        }
    }

    private String getTranslationText() {
        if (mVerb == null || getContext() == null) {
            return "";
        }
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(requireContext());
        String translationLanguage = sharedPref.getString(getString(R.string.pref_translationLanguage), getString(R.string.pref_translationLanguage_default));
        String translation;
        if (translationLanguage.equals("en")) {
            translation = mVerb.getTranslationEn();
        } else {
            translation = mVerb.getTranslationEs();
        }
        return translation;
    }

    private void setTranslationText(String text) {
        if (mVerb == null || getContext() == null) {
            return;
        }
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(requireContext());
        String translationLanguage = sharedPref.getString(getString(R.string.pref_translationLanguage), getString(R.string.pref_translationLanguage_default));
        if (translationLanguage.equals("en")) {
            viewModel.updateTranslationEn(mVerb, text);
        } else {
            viewModel.updateTranslationEs(mVerb, text);
        }
        if (mTranslateView != null) {
            mTranslateView.setText(text);
        }
    }

    private class SectionPagerAdapter extends PagerAdapter {

        SectionPagerAdapter() {
            super();
        }

        @Override
        public Object instantiateItem(ViewGroup collection, int position) {
            Activity activity = getActivity();
            if (activity == null) {
                return new View(requireContext()); // Safeguard with context
            }
            LayoutInflater inflater = activity.getLayoutInflater();
            View view;
            if (mVerb == null) {
                // This shouldn't be called if getCount is 0 but as a safeguard.
                return new View(activity);
            }
            switch (position) {
                case 0:
                    view = inflater.inflate(R.layout.fragment_verb_ind_view, collection, false);
                    ((TextView) view.findViewById(R.id.verbViewIndPresent)).setText(mVerb.getIndPresent().toString());
                    ((TextView) view.findViewById(R.id.verbViewIndParticipi)).setText(mVerb.getParticipi());
                    ((TextView) view.findViewById(R.id.verbViewIndImperfet)).setText(mVerb.getIndImperfet().toString());
                    ((TextView) view.findViewById(R.id.verbViewIndGerundi)).setText(mVerb.getGerundi());
                    ((TextView) view.findViewById(R.id.verbViewIndFutur)).setText(mVerb.getIndFutur().toString());
                    ((TextView) view.findViewById(R.id.verbViewIndCondicional)).setText(mVerb.getIndCondicional().toString());
                    ((TextView) view.findViewById(R.id.verbViewIndPassatSimple)).setText(mVerb.getIndPassatSimple().toString());
                    break;
                case 1:
                    view = inflater.inflate(R.layout.fragment_verb_sub_view, collection, false);
                    ((TextView) view.findViewById(R.id.verbViewSubPresent)).setText(mVerb.getSubPresent().toString());
                    ((TextView) view.findViewById(R.id.verbViewSubImperfet)).setText(mVerb.getSubImperfet().toString());
                    ((TextView) view.findViewById(R.id.verbViewSubImperatiu)).setText(mVerb.getImperatiu().toString());
                    break;
                case 2:
                default:
                    view = inflater.inflate(R.layout.fragment_verb_com_view, collection, false);
                    ((TextView) view.findViewById(R.id.verbViewComPassatPerifrastic)).setText(mVerb.getComPassatPerifrastic().toString());
                    ((TextView) view.findViewById(R.id.verbViewComPerfet)).setText(mVerb.getComPerfet().toString());
                    ((TextView) view.findViewById(R.id.verbViewComPlusquamperfet)).setText(mVerb.getComPlusquamperfet().toString());
                    ((TextView) view.findViewById(R.id.verbViewComPassatAnterior)).setText(mVerb.getComPassatAnterior().toString());
                    ((TextView) view.findViewById(R.id.verbViewComPassatAnteriorPerifrastic)).setText(mVerb.getComPassatAnteriorPerifrastic().toString());
                    ((TextView) view.findViewById(R.id.verbViewComFuturPerfet)).setText(mVerb.getComFuturPerfet().toString());
                    ((TextView) view.findViewById(R.id.verbViewComCondicionalPerfet)).setText(mVerb.getComCondicionalPerfet().toString());
                    ((TextView) view.findViewById(R.id.verbViewComSubPassatPerifrastic)).setText(mVerb.getComSubPassatPerifrastic().toString());
                    ((TextView) view.findViewById(R.id.verbViewComSubPerfet)).setText(mVerb.getComSubPerfet().toString());
                    ((TextView) view.findViewById(R.id.verbViewComSubPlusquamperfet)).setText(mVerb.getComSubPlusquamperfet().toString());
                    ((TextView) view.findViewById(R.id.verbViewComSubPassatAnteriorPerifrastic)).setText(mVerb.getComSubPassaAnteriorPerifrastic().toString());
                    break;

            }
            collection.addView(view, 0);
            return view;
        }

        @Override
        public void destroyItem(ViewGroup collection, int position, Object view) {
            collection.removeView((View) view);
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return object == view;
        }

        @Override
        public int getCount() {
            return mVerb == null ? 0 : 3;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case 0:
                    return "Indicatiu";
                case 1:
                    return "Subjuntiu";
                case 2:
                default:
                    return "Compostes";
            }
        }
    }

    public interface VerbViewFragmentListener {
        void onVerbViewFinished();
        BillingManager getBillingManager();
        PremiumManager getPremiumManager();
        int getFavoritesCount();
        void onFavoriteChanged(boolean added);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        MenuItem searchItem = menu.findItem(R.id.action_search);
        searchItem.setVisible(true);
        MenuItem favoriteItem = menu.findItem(R.id.action_favorite);
        if (mVerb != null && mVerb.isFavorite()) {
            favoriteItem.setIcon(starDrawable);
        } else {
            favoriteItem.setIcon(starInactiveDrawable);
        }
        favoriteItem.setVisible(true);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_favorite) {
            if (mVerb != null) {
                boolean newFavoriteState = !mVerb.isFavorite();

                if (newFavoriteState) {
                    // Adding favorite - check limit
                    PremiumManager premiumManager = mListener.getPremiumManager();
                    int currentCount = mListener.getFavoritesCount();

                    if (!premiumManager.canAddFavorite(currentCount)) {
                        // Show purchase dialog
                        Activity activity = getActivity();
                        if (activity == null || mListener == null) return true;
                        PurchaseDialogHelper.showFavoritesLimitDialog(
                                activity,
                                mListener.getBillingManager(),
                                premiumManager.getFavoritesLimit(),
                                new PurchaseDialogHelper.PurchaseDialogCallback() {
                                    @Override
                                    public void onBuyClicked() {
                                        Activity currentActivity = getActivity();
                                        if (currentActivity == null || mListener == null) return;
                                        BillingManager billingManager = mListener.getBillingManager();
                                        if (billingManager != null) {
                                            billingManager.launchPurchaseFlow(currentActivity,
                                                    new BillingManager.PurchaseCallback() {
                                                        @Override
                                                        public void onPurchaseSuccess() {
                                                            // After purchase, add the favorite
                                                            toggleFavorite(item, true);
                                                        }

                                                        @Override
                                                        public void onPurchaseFailed(String error) {
                                                            if (getContext() != null) {
                                                                Toast.makeText(getContext(), error, Toast.LENGTH_SHORT).show();
                                                            }
                                                        }

                                                        @Override
                                                        public void onPurchaseCancelled() {
                                                            // Do nothing
                                                        }
                                                    });
                                        }
                                    }

                                    @Override
                                    public void onCancelled() {
                                        // Do nothing
                                    }
                                });
                        return true;
                    }
                }

                // Proceed with toggle
                toggleFavorite(item, newFavoriteState);
            }
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void toggleFavorite(MenuItem item, boolean newFavoriteState) {
        viewModel.updateFavorite(mVerb, newFavoriteState);
        if (newFavoriteState) {
            item.setIcon(starDrawable);
        } else {
            item.setIcon(starInactiveDrawable);
        }
        // Notify listener to update favorites count
        if (mListener != null) {
            mListener.onFavoriteChanged(newFavoriteState);
        }
    }

    @Override
    public void onDestroyView() {
        if (mAdView != null) {
            mAdView.destroy();
            mAdView = null;
        }
        // Nullify view references
        mTranslateView = null;
        mViewPager = null;
        mAdapter = null;
        super.onDestroyView();
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

}
