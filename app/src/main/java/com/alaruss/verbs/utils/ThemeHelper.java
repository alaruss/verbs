package com.alaruss.verbs.utils;

import androidx.appcompat.app.AppCompatDelegate;

public class ThemeHelper {

    public static void applyTheme(String themeValue) {
        if (themeValue == null) {
            themeValue = "light";
        }

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
}
