package com.alaruss.verbs;

import android.app.Application;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.preference.PreferenceManager;

import com.alaruss.verbs.db.VerbRepository;
import com.alaruss.verbs.utils.ThemeHelper;
import com.google.android.gms.ads.MobileAds;

public class MyApplication extends Application {
    private static final String LOG_TAG = MyApplication.class.getSimpleName();
    private VerbRepository mVerbRepository;


    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(LOG_TAG, "App.create");
        applySavedTheme();
        initializeAds();
    }

    private void initializeAds() {
        MobileAds.initialize(this, initializationStatus -> {
            Log.d(LOG_TAG, "MobileAds initialized");
        });
    }

    private void applySavedTheme() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String theme = prefs.getString("pref_theme", "light");
        ThemeHelper.applyTheme(theme);
    }

    public VerbRepository getVerbRepository() {
        if (mVerbRepository == null) {
            mVerbRepository = new VerbRepository(this);
        }
        return mVerbRepository;
    }
}
