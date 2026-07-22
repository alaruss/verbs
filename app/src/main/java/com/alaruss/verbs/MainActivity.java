package com.alaruss.verbs;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.LiveData;
import androidx.preference.PreferenceManager;

import com.alaruss.verbs.databinding.ActivityMainBinding;
import com.alaruss.verbs.fragments.VerbListFragment;
import com.alaruss.verbs.fragments.VerbViewFragment;
import com.alaruss.verbs.premium.BillingManager;
import com.alaruss.verbs.premium.PremiumManager;
import com.alaruss.verbs.premium.PurchaseDialogHelper;
import com.alaruss.verbs.utils.BackgroundTaskExecutor;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.crashlytics.FirebaseCrashlytics;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, VerbListFragment.VerbListFragmentListener,
        VerbViewFragment.VerbViewFragmentListener {
    private static final String LOG_TAG = MainActivity.class.getSimpleName();
    private static final String PREF_MIGRATION_IN_PROGRESS = "migration_in_progress";
    private final String FRAGMENT_LIST_VERB = "fragment_verb_list";
    private final String FRAGMENT_VIEW_VERB = "fragment_verb_view";
    private final String VERB_ID = "verb_id";
    private final String PREF_DATA_MIGRATION = "data_migration";
    int mCurrentVerbId = -1;
    ActionBarDrawerToggle mDrawerToggle;
    private MyApplication mApp;
    private FirebaseAnalytics mFirebaseAnalytics;
    private BackgroundTaskExecutor taskExecutor;
    private AlertDialog mProgressDialog;
    private ProgressBar mProgressBar;
    private ActivityMainBinding binding;
    private BillingManager billingManager;
    private PremiumManager premiumManager;
    private LiveData<Integer> favoritesCountLiveData;
    private int currentFavoritesCount = 0;
    private FragmentManager.OnBackStackChangedListener
            mOnBackStackChangedListener = new FragmentManager.OnBackStackChangedListener() {
        @Override
        public void onBackStackChanged() {
            Fragment f = getSupportFragmentManager().findFragmentById(R.id.frame_container);
            if (f instanceof VerbListFragment) {
                setBackArrowVisible(false);
            } else {
                setBackArrowVisible(true);
            }
        }
    };

    @Override
    protected void onDestroy() {
        if (taskExecutor != null) {
            taskExecutor.shutdown();
        }
        if (billingManager != null) {
            billingManager.endConnection();
        }
        getSupportFragmentManager().removeOnBackStackChangedListener(mOnBackStackChangedListener);
        super.onDestroy();
    }

    private void setBackArrowVisible() {
        setBackArrowVisible(getSupportFragmentManager().getBackStackEntryCount() != 0);
    }

    private void setBackArrowVisible(boolean state) {
        if (!state) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(false);
            getSupportActionBar().setDisplayShowHomeEnabled(false);
            mDrawerToggle.setDrawerIndicatorEnabled(true);
        } else {
            mDrawerToggle.setDrawerIndicatorEnabled(false);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
        mApp = (MyApplication) getApplication();
        taskExecutor = new BackgroundTaskExecutor();

        // Initialize premium system
        premiumManager = PremiumManager.getInstance(this);
        billingManager = new BillingManager(this);
        billingManager.startConnection(this::updatePremiumMenuVisibility);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.appBarMain.toolbar);

        mDrawerToggle = new ActionBarDrawerToggle(
                this, binding.drawerLayout, binding.appBarMain.toolbar,
                R.string.navigation_drawer_open, R.string.navigation_drawer_close);

        mDrawerToggle.setToolbarNavigationClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
        binding.drawerLayout.addDrawerListener(mDrawerToggle);

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    binding.drawerLayout.closeDrawer(GravityCompat.START);
                } else {
                    setEnabled(false);
                    getOnBackPressedDispatcher().onBackPressed();
                }
            }
        });
        mDrawerToggle.syncState();

        binding.navView.setNavigationItemSelectedListener(this);
        getSupportFragmentManager().addOnBackStackChangedListener(mOnBackStackChangedListener);
        if (savedInstanceState != null) {
            int id = savedInstanceState.getInt(VERB_ID);
            if (id != -1) {
                showVerb(id, true);
                return;
            }
        }
        migrateDataAndStart();
    }

    private void migrateDataAndStart() {
        try {
            // Get package info to determine install/update status
            android.content.pm.PackageInfo packageInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            long firstInstallTime = packageInfo.firstInstallTime;
            long lastUpdateTime = packageInfo.lastUpdateTime;
            boolean isFirstRun = firstInstallTime == lastUpdateTime;

            SharedPreferences prefs = getSharedPreferences(getPackageName(), Activity.MODE_PRIVATE);
            int lastMigration = prefs.getInt(PREF_DATA_MIGRATION, 0);

            // Check if migration was interrupted
            int migrationInProgress = prefs.getInt(PREF_MIGRATION_IN_PROGRESS, 0);
            if (migrationInProgress > 0) {
                // Previous migration was interrupted - check if data exists
                int existingCount = mApp.getVerbRepository().getFavoritesCountSync();
                if (existingCount > 0 || hasVerbsInDatabase()) {
                    // Data exists, mark migration as complete
                    prefs.edit()
                            .putInt(PREF_DATA_MIGRATION, migrationInProgress)
                            .putInt(PREF_MIGRATION_IN_PROGRESS, 0)
                            .apply();
                    lastMigration = migrationInProgress;
                } else {
                    // No data, clear the in-progress flag and retry
                    prefs.edit().putInt(PREF_MIGRATION_IN_PROGRESS, 0).apply();
                }
            }

            if (lastMigration == 0 && !isFirstRun) {
                lastMigration = 1;
            }

            if (lastMigration == 0) {
                // This is a fresh install. Run the first migration.
                runMigration(1, 2);
            } else if (lastMigration == 1) {
                // The app was updated and the last migration was #1, so run #2.
                runMigration(2, 2);
            } else {
                // All migrations are complete, initialize premium and show the main list.
                initializePremiumSystem();
                showList();
            }

        } catch (Exception e) {
            // This should realistically never happen for your own package.
            FirebaseCrashlytics.getInstance().recordException(e);

            showList();
        }
    }

    // Helper method to check if database has data
    private boolean hasVerbsInDatabase() {
        try {
            return mApp.getVerbRepository().getAllVerbsSync().size() > 0;
        } catch (Exception e) {
            return false;
        }
    }

    @SuppressLint("ApplySharedPref")
    private void runMigration(int migrationType, int migrationNumber) {
        // Mark migration as in-progress before starting
        SharedPreferences prefs = getSharedPreferences(getPackageName(), Activity.MODE_PRIVATE);
        prefs.edit().putInt(PREF_MIGRATION_IN_PROGRESS, migrationNumber).commit(); // Use commit() for immediate write

        // Show progress dialog replacement
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_progress, null);
        mProgressBar = dialogView.findViewById(R.id.progress_bar);
        TextView titleView = dialogView.findViewById(R.id.progress_title);
        titleView.setText("Updating data...");

        mProgressDialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .setCancelable(false)
                .create();
        mProgressDialog.show();

        taskExecutor.execute(
                listener -> {
                    // Background task
                    if (migrationType == 1) {
                        mApp.getVerbRepository().runDataMigration01(mApp, listener::onProgress);
                    } else {
                        mApp.getVerbRepository().runDataMigration02(mApp, listener::onProgress);
                    }
                    return null;
                },
                progress -> {
                    // Progress update on main thread
                    if (mProgressBar != null) {
                        mProgressBar.setProgress(progress);
                    }
                },
                result -> {
                    // Completion on main thread: Clear in-progress flag and set completed
                    SharedPreferences completionPrefs = getSharedPreferences(getPackageName(), Activity.MODE_PRIVATE);
                    completionPrefs.edit()
                            .putInt(PREF_DATA_MIGRATION, migrationNumber)
                            .putInt(PREF_MIGRATION_IN_PROGRESS, 0)
                            .apply();
                    if (mProgressDialog != null) {
                        mProgressDialog.dismiss();
                        mProgressDialog = null;
                        mProgressBar = null;
                    }
                    migrateDataAndStart();
                }
        );
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putInt(VERB_ID, mCurrentVerbId);
        super.onSaveInstanceState(outState);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (mDrawerToggle.isDrawerIndicatorEnabled() &&
                mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        } else if (id == android.R.id.home) {
            getOnBackPressedDispatcher().onBackPressed();
            return true;
        } else {
            if (id == R.id.action_search) {
                showList();
                return true;
            }
        }
        return super.onOptionsItemSelected(item);

    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_about) {
            final AlertDialog d = new AlertDialog.Builder(this)
                    .setPositiveButton(android.R.string.ok, null)
                    .setTitle(R.string.aboutTitle)
                    .setMessage(R.string.aboutMessage)
                    .create();
            d.show();
            ((TextView) d.findViewById(android.R.id.message)).setMovementMethod(LinkMovementMethod.getInstance());
        } else if (id == R.id.nav_share) {
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("text/plain");
            intent.putExtra(Intent.EXTRA_TEXT, getString(R.string.share_text));
            if (intent.resolveActivity(getPackageManager()) != null) {
                startActivity(intent);
            }
        } else if (id == R.id.nav_preferences) {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
        } else if (id == R.id.nav_buy_premium) {
            PurchaseDialogHelper.showPurchaseDialog(this, billingManager, null,
                    premiumManager.getFavoritesLimit(),
                    new PurchaseDialogHelper.PurchaseDialogCallback() {
                        @Override
                        public void onBuyClicked() {
                            if (billingManager != null) {
                                billingManager.launchPurchaseFlow(MainActivity.this,
                                        new BillingManager.PurchaseCallback() {
                                            @Override
                                            public void onPurchaseSuccess() {
                                                updatePremiumMenuVisibility();
                                            }

                                            @Override
                                            public void onPurchaseFailed(String error) {
                                                Toast.makeText(MainActivity.this, error, Toast.LENGTH_SHORT).show();
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
        }

        binding.drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    private void showList() {
        mCurrentVerbId = -1;
        boolean isFirstRun = false;
        VerbListFragment fragment;
        fragment = (VerbListFragment) getSupportFragmentManager().findFragmentByTag(FRAGMENT_LIST_VERB);
        if (fragment == null) {
            fragment = new VerbListFragment();
            isFirstRun = true;
        }
        if (!isFinishing()) {
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction().
                    replace(R.id.frame_container, fragment, FRAGMENT_LIST_VERB);
            if (!isFirstRun) {
                transaction.addToBackStack(null);
            }
            transaction.commit();
        }
    }

    public void hideKB() {
        View view = getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    private void showVerb(int id) {
        showVerb(id, false);
    }

    private void showVerb(int id, boolean getSaved) {
        mCurrentVerbId = id;
        VerbViewFragment fragment = null;
        if (getSaved) {
            fragment = (VerbViewFragment) getSupportFragmentManager().findFragmentByTag(FRAGMENT_VIEW_VERB);
        }
        if (fragment == null) {
            fragment = new VerbViewFragment();
            if (id != -1) {
                Bundle bundle = new Bundle();
                bundle.putInt(getString(R.string.EXTRA_ID), id);
                fragment.setArguments(bundle);
            }
        }
        hideKB();
        if (!isFinishing()) {
            getSupportFragmentManager().beginTransaction().replace(R.id.frame_container, fragment, FRAGMENT_VIEW_VERB).
                    addToBackStack(null).commit();
        }
    }

    @Override
    public void onVerbListSelected(int verbId) {
        showVerb(verbId);
    }

    @Override
    public void onVerbViewFinished() {
        showList();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updatePremiumMenuVisibility();
    }

    // Premium system methods

    private void initializePremiumSystem() {
        // Set up LiveData observation for favorites count (only once)
        if (favoritesCountLiveData == null) {
            favoritesCountLiveData = mApp.getVerbRepository().getFavoritesCount();
            favoritesCountLiveData.observe(this, count -> {
                if (count != null) {
                    currentFavoritesCount = count;
                    // Initialize premium manager on first count if not already done
                    if (!premiumManager.isInitialized()) {
                        premiumManager.initializeFavoritesLimit(count);
                    }
                }
            });
        }
    }

    private void updatePremiumMenuVisibility() {
        MenuItem premiumItem = binding.navView.getMenu().findItem(R.id.nav_buy_premium);
        if (premiumItem != null) {
            premiumItem.setVisible(!premiumManager.isPremium());
        }
    }

    @Override
    public BillingManager getBillingManager() {
        return billingManager;
    }

    @Override
    public PremiumManager getPremiumManager() {
        return premiumManager;
    }

    @Override
    public int getFavoritesCount() {
        return currentFavoritesCount;
    }

    @Override
    public void onFavoriteChanged(boolean added) {
    }

}
