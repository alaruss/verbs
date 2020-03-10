package com.alaruss.verbs;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import androidx.annotation.NonNull;
import com.google.android.material.navigation.NavigationView;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.text.method.LinkMovementMethod;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;

import com.alaruss.verbs.db.VerbDAO;
import com.alaruss.verbs.fragments.VerbListFragment;
import com.alaruss.verbs.fragments.VerbViewFragment;
import com.google.firebase.analytics.FirebaseAnalytics;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, VerbListFragment.VerbListFragmentListener,
        VerbViewFragment.VerbViewFragmentListener {
    private static final String LOG_TAG = MainActivity.class.getSimpleName();
    int mCurrentVerbId = -1;
    private final String FRAGMENT_LIST_VERB = "fragment_verb_list";
    private final String FRAGMENT_VIEW_VERB = "fragment_verb_view";
    private final String VERB_ID = "verb_id";
    private final String PREF_FIRST_RUN = "first_run";
    private final String PREF_DATA_MIGRATION = "data_migration";
    private MyApplication mApp;
    private FirebaseAnalytics mFirebaseAnalytics;


    ActionBarDrawerToggle mDrawerToggle;


    private abstract class MigrationDbTask extends AsyncTask<Void, Integer, Void> implements VerbDAO.ImportProgressCallback {
        ProgressDialog mProgressDialog;

        abstract public int getMigrationNumber();

        protected void onPreExecute() {
            super.onPreExecute();
            mProgressDialog = new ProgressDialog(MainActivity.this);
            mProgressDialog.setMax(100);
            mProgressDialog.setTitle("Updating data...");
            mProgressDialog.setProgress(0);
            mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            mProgressDialog.show();
        }

        public void onProgress(Integer progress) {
            publishProgress(progress);
        }

        protected void onProgressUpdate(Integer... progress) {
            mProgressDialog.setProgress(progress[0]);
        }

        @SuppressLint("ApplySharedPref")
        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
            SharedPreferences prefs = getSharedPreferences("com.alaruss.verbs", Activity.MODE_PRIVATE);
            prefs.edit().putInt(PREF_DATA_MIGRATION, this.getMigrationNumber()).commit();
            mProgressDialog.dismiss();
            migrateDataAndStart();
        }
    }

    private class MigrationDB1 extends MigrationDbTask {
        public int getMigrationNumber() {
            return 2;
        }

        @Override
        protected Void doInBackground(Void... params) {
            VerbDAO verbDAO = mApp.getDBHelper().getVerbDAO();
            verbDAO.dataMigration01(mApp, this);
            return null;
        }
    }

    private class MigrationDB2 extends MigrationDbTask {
        public int getMigrationNumber() {
            return 2;
        }

        @Override
        protected Void doInBackground(Void... params) {
            VerbDAO verbDAO = mApp.getDBHelper().getVerbDAO();
            verbDAO.dataMigration02(mApp, this);
            return null;
        }
    }

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
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerToggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);

        mDrawerToggle.setToolbarNavigationClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });
        drawer.setDrawerListener(mDrawerToggle);
        mDrawerToggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
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
        SharedPreferences prefs = getSharedPreferences("com.alaruss.verbs", Activity.MODE_PRIVATE);
        int lastMigration = prefs.getInt(PREF_DATA_MIGRATION, 0);
        if (lastMigration == 0 && !prefs.getBoolean(PREF_FIRST_RUN, true)) {
            lastMigration = 1;
        }
        if (lastMigration == 0) {
            new MigrationDB1().execute();
        } else if (lastMigration == 1) {
            new MigrationDB2().execute();
        } else {
            showList();
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putInt(VERB_ID, mCurrentVerbId);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
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
            onBackPressed();
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
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
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
        mApp.getDBHelper().open();
        super.onResume();
    }

}
