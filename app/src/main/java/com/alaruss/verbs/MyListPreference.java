package com.alaruss.verbs;

import android.content.Context;
import android.preference.ListPreference;
import android.util.AttributeSet;

public class MyListPreference extends ListPreference {
    public MyListPreference(Context context) { super(context); }
    public MyListPreference(Context context, AttributeSet attrs) { super(context, attrs); }
    @Override
    public void setValue(String value) {
        super.setValue(value);
        setSummary(getEntry());
    }
}