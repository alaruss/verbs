package com.alaruss.verbs;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.SpannableString;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;

import com.alaruss.verbs.db.VerbDAO;
import com.alaruss.verbs.fragments.VerbListFragment;
import com.alaruss.verbs.fragments.VerbViewFragment;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, VerbListFragment.VerbListFragmentListener,
        VerbViewFragment.VerbViewFragmentListener {
    private static final String LOG_TAG = MainActivity.class.getSimpleName();
    int mCurrentVerbId = -1;
    private final String FRAGMENT_LIST_VERB = "fragment_verb_list";
    private final String FRAGMENT_VIEW_VERB = "fragment_verb_view";
    private final String VERB_ID = "verb_id";
    private MyApplication mApp;

    private class FillDbTask extends AsyncTask<Void, Integer, Void> implements VerbDAO.ImportProgressCallback {
        ProgressDialog mProgressDialog;

        protected void onPreExecute() {
            super.onPreExecute();
            mProgressDialog = new ProgressDialog(MainActivity.this);
            mProgressDialog.setMax(100);
            mProgressDialog.setMessage("Its just one time...");
            mProgressDialog.setTitle("Loading DB");
            mProgressDialog.setProgress(0);
            mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            mProgressDialog.show();
        }

        @SuppressLint("CommitPrefEdits")
        @Override
        protected Void doInBackground(Void... params) {
            VerbDAO verbDAO = mApp.getDBHelper().getVerbDAO();
            SharedPreferences prefs = getSharedPreferences("com.alaruss.verbs", Activity.MODE_PRIVATE);
            verbDAO.importVerbs(mApp, this);
            prefs.edit().putBoolean("first_run", false).commit();
            return null;
        }

        public void onProgress(Integer progress) {
            publishProgress(progress);
        }

        protected void onProgressUpdate(Integer... progress) {
            mProgressDialog.setProgress(progress[0]);
        }

        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
            mProgressDialog.dismiss();
            showList();
        }
    }

    ActionBarDrawerToggle mDrawerToggle;

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
        mApp = (MyApplication) getApplication();
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerToggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
//        {
//
//            public void onDrawerClosed(View view) {
////                setActionBarArrowDependingOnFragmentsBackStack();
//            }
//
//            public void onDrawerOpened(View drawerView) {
//                Log.d(LOG_TAG, "set:" + true );
//                mDrawerToggle.setDrawerIndicatorEnabled(true);
//            }
//        };
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
        SharedPreferences prefs = getSharedPreferences("com.alaruss.verbs", Activity.MODE_PRIVATE);
        if (prefs.getBoolean("first_run", true)) {
            new FillDbTask().execute();
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
            ((TextView)d.findViewById(android.R.id.message)).setMovementMethod(LinkMovementMethod.getInstance());
        } else if (id == R.id.nav_share) {
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("text/plain");
            intent.putExtra(Intent.EXTRA_TEXT, "Conjugació dels verbs catalans: http://play.google.com/store/apps/details?id=com.alaruss.verbs");
            if (intent.resolveActivity(getPackageManager()) != null) {
                startActivity(intent);
            }
        } else if (id == R.id.nav_send) {
            Intent intent = new Intent(Intent.ACTION_SENDTO);
            intent.setData(Uri.parse("mailto:"));
            intent.putExtra(Intent.EXTRA_EMAIL, new String[]{"verbs@alaruss.com"});
//            intent.putExtra(Intent.EXTRA_SUBJECT, "Hola");
            if (intent.resolveActivity(getPackageManager()) != null) {
                startActivity(intent);
            }
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
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction().
                replace(R.id.frame_container, fragment, FRAGMENT_LIST_VERB);
        if (!isFirstRun) {
            transaction.addToBackStack(null);
        }
        transaction.commit();
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
        getSupportFragmentManager().beginTransaction().replace(R.id.frame_container, fragment, FRAGMENT_VIEW_VERB).
                addToBackStack(null).commit();
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
//
//    @Override
//    protected void onPause() {
//        mApp.getDBHelper().close();
//        super.onPause();
//    }
}
