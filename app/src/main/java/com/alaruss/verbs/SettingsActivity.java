package com.alaruss.verbs;

import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.alaruss.verbs.premium.BillingManager;
import com.alaruss.verbs.premium.PremiumManager;
import com.alaruss.verbs.premium.PurchaseDialogHelper;

public class SettingsActivity extends AppCompatActivity {

    private BillingManager billingManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeButtonEnabled(true);
        }

        billingManager = new BillingManager(this);
        billingManager.startConnection(null);

        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.settings_container, new SettingsFragment())
                    .commit();
        }
    }

    @Override
    protected void onDestroy() {
        if (billingManager != null) {
            billingManager.endConnection();
        }
        super.onDestroy();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public BillingManager getBillingManager() {
        return billingManager;
    }

    public static class SettingsFragment extends PreferenceFragmentCompat {
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.preferences, rootKey);

            ListPreference themePreference = findPreference("pref_theme");
            if (themePreference != null) {
                // Use entries without "(Premium)" suffix for premium users
                PremiumManager premiumManager = new PremiumManager(getContext());
                if (premiumManager.isPremium()) {
                    themePreference.setEntries(R.array.pref_theme_entries_premium);
                }

                themePreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                    @Override
                    public boolean onPreferenceChange(Preference preference, Object newValue) {
                        String themeValue = (String) newValue;
                        PremiumManager premiumManager = new PremiumManager(getContext());

                        // Light theme is always free
                        if ("light".equals(themeValue)) {
                            applyTheme(themeValue);
                            return true;
                        }

                        // Dark/System themes require premium
                        if (!premiumManager.isPremium()) {
                            SettingsActivity activity = (SettingsActivity) getActivity();
                            PurchaseDialogHelper.showThemePurchaseDialog(
                                    activity,
                                    activity.getBillingManager(),
                                    new PurchaseDialogHelper.PurchaseDialogCallback() {
                                        @Override
                                        public void onBuyClicked() {
                                            BillingManager billingManager = activity.getBillingManager();
                                            if (billingManager != null) {
                                                billingManager.launchPurchaseFlow(activity,
                                                        new BillingManager.PurchaseCallback() {
                                                            @Override
                                                            public void onPurchaseSuccess() {
                                                                // After purchase, apply the theme
                                                                themePreference.setValue(themeValue);
                                                                applyTheme(themeValue);
                                                            }

                                                            @Override
                                                            public void onPurchaseFailed(String error) {
                                                                Toast.makeText(activity, error, Toast.LENGTH_SHORT).show();
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
                            return false; // Don't change preference yet
                        }

                        applyTheme(themeValue);
                        return true;
                    }
                });
            }
        }

        private void applyTheme(String themeValue) {
            switch (themeValue) {
                case "dark":
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                    break;
                case "system":
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                    break;
                case "light":
                default:
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                    break;
            }
        }

        @Override
        public void onViewCreated(@androidx.annotation.NonNull android.view.View view, Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);
            setDivider(new android.graphics.drawable.ColorDrawable(getResources().getColor(R.color.colorBackgroundDarkTitle)));
            setDividerHeight(1);
        }
    }
}
