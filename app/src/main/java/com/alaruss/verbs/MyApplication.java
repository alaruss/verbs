package com.alaruss.verbs;

import android.app.Application;
import android.util.Log;

import com.alaruss.verbs.db.DBHelper;
import com.crashlytics.android.Crashlytics;

import io.fabric.sdk.android.Fabric;

public class MyApplication extends Application {
    private static final String LOG_TAG = MyApplication.class.getSimpleName();
    private DBHelper mDBHelper;


    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(LOG_TAG, "App.create");
        if (BuildConfig.enableCrashlytics) {
            Fabric.with(this, new Crashlytics());
        }
    }

    public DBHelper getDBHelper() {
        if (mDBHelper == null) {
            mDBHelper = new DBHelper(this);
        }
        return mDBHelper;
    }
}
