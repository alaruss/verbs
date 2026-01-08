package com.alaruss.verbs.premium;

import android.content.Context;
import android.content.SharedPreferences;

public class PremiumManager {
    private static final String PREF_NAME = "premium_prefs";
    public static final String PREF_IS_PREMIUM = "is_premium";
    public static final String PREF_FAVORITES_LIMIT = "favorites_limit";
    public static final String PREF_PREMIUM_INITIALIZED = "premium_initialized";

    private static final int DEFAULT_FAVORITES_LIMIT = 42;

    private final SharedPreferences prefs;

    public PremiumManager(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public boolean isPremium() {
        return prefs.getBoolean(PREF_IS_PREMIUM, false);
    }

    public void setPremium(boolean isPremium) {
        prefs.edit().putBoolean(PREF_IS_PREMIUM, isPremium).apply();
    }

    public int getFavoritesLimit() {
        return prefs.getInt(PREF_FAVORITES_LIMIT, DEFAULT_FAVORITES_LIMIT);
    }

    public boolean canAddFavorite(int currentFavoritesCount) {
        if (isPremium()) {
            return true;
        }
        return currentFavoritesCount < getFavoritesLimit();
    }

    public boolean isInitialized() {
        return prefs.getBoolean(PREF_PREMIUM_INITIALIZED, false);
    }

    public void initializeFavoritesLimit(int currentFavoritesCount) {
        if (isInitialized()) {
            return;
        }

        int limit = Math.max(currentFavoritesCount, DEFAULT_FAVORITES_LIMIT);

        prefs.edit()
                .putInt(PREF_FAVORITES_LIMIT, limit)
                .putBoolean(PREF_PREMIUM_INITIALIZED, true)
                .apply();
    }
}
