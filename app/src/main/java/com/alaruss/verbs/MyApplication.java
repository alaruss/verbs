package com.alaruss.verbs;

import android.app.Application;
import android.util.Log;

import com.alaruss.verbs.db.VerbRepository;

public class MyApplication extends Application {
    private static final String LOG_TAG = MyApplication.class.getSimpleName();
    private VerbRepository mVerbRepository;


    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(LOG_TAG, "App.create");
    }

    public VerbRepository getVerbRepository() {
        if (mVerbRepository == null) {
            mVerbRepository = new VerbRepository(this);
        }
        return mVerbRepository;
    }
}
