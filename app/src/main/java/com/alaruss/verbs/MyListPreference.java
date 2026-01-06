package com.alaruss.verbs;

import android.content.Context;
import android.util.AttributeSet;

import androidx.preference.ListPreference;

public class MyListPreference extends ListPreference {
    public MyListPreference(Context context) {
        super(context);
    }

    public MyListPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public MyListPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public MyListPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    public void setValue(String value) {
        super.setValue(value);
        setSummary(getEntry());
    }
}