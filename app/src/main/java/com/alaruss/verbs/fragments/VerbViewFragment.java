package com.alaruss.verbs.fragments;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import com.alaruss.verbs.MyApplication;
import com.alaruss.verbs.R;
import com.alaruss.verbs.db.VerbDAO;
import com.alaruss.verbs.models.Verb;


public class VerbViewFragment extends Fragment {
    Verb mVerb;
    VerbViewFragmentListener mListener;
    private TextView mTranslateView;
    private int starDrawable, starInactiveDrawable;

    public VerbViewFragment() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            starDrawable = R.drawable.ic_star;
            starInactiveDrawable = R.drawable.ic_star_inactive;
        } else {
            starDrawable = android.R.drawable.star_big_on;
            starInactiveDrawable = android.R.drawable.star_big_off;
        }
    }


    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onResume() {
        super.onResume();
        getActivity().setTitle(mVerb.getInfinitive());
//        mTranslateView.setText(getTranslationText());
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (VerbViewFragmentListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement VerbViewFragmentListener");
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        int verbId = getArguments() != null ? getArguments().getInt(getString(R.string.EXTRA_ID)) : 0;
        if (verbId == 0) { // didn found verb
            mListener.onVerbViewFinished();
        } else {
            VerbDAO verbDAO = ((MyApplication) getActivity().getApplication()).getDBHelper().getVerbDAO();
            mVerb = verbDAO.getVerb(verbId);

            verbDAO.updateLastAccess(mVerb);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_verb_view, container, false);
        TabLayout tabLayout = (TabLayout) view.findViewById(R.id.tab_layout);
        ViewPager viewPager = (ViewPager) view.findViewById(R.id.tab_pager);
        viewPager.setAdapter(new SectionPagerAdapter());
        tabLayout.setupWithViewPager(viewPager);
        mTranslateView = (TextView) view.findViewById(R.id.translation_text);
        mTranslateView.setText(getTranslationText());
        mTranslateView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                int action = event.getAction();
                if (action == MotionEvent.ACTION_DOWN) {
                    int leftEdgeOfRightDrawable = mTranslateView.getRight() - mTranslateView.getTotalPaddingRight();
                    if (event.getRawX() >= leftEdgeOfRightDrawable) {

                        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                        LayoutInflater inflater = getActivity().getLayoutInflater();
                        View dialogView = inflater.inflate(R.layout.dialog_translation_edit, null);
                        final EditText translationEdit = (EditText) dialogView.findViewById(R.id.translation_edit);
                        final String currentTranslation = getTranslationText();
                        translationEdit.setText(currentTranslation);
                        translationEdit.setSelection(currentTranslation.length());
                        builder.setView(dialogView)
                                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                        String newTranslation = translationEdit.getText().toString();
                                        if (!newTranslation.equals(currentTranslation)) {
                                            setTranslationText(newTranslation);
                                        }
                                    }
                                });
                        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                // Nothing to do
                            }
                        });
                        AlertDialog dialog = builder.create();
                        dialog.show();


                        return true;
                    }
                }
                return false;
            }
        });
        return view;
    }

    private String getTranslationText() {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getActivity());
        String translationLanguage = sharedPref.getString(getString(R.string.pref_translationLanguage), getString(R.string.pref_translationLanguage_default));
        String translation;
        if (translationLanguage.equals("en")) {
            translation = mVerb.getTranslationEn();
        } else {
            translation = mVerb.getTranslationEs();
        }
        return translation;
    }

    private void setTranslationText(String text) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getActivity());
        String translationLanguage = sharedPref.getString(getString(R.string.pref_translationLanguage), getString(R.string.pref_translationLanguage_default));
        if (translationLanguage.equals("en")) {
            ((MyApplication) getActivity().getApplication()).getDBHelper().getVerbDAO().setTranslationEn(mVerb, text);
        } else {
            ((MyApplication) getActivity().getApplication()).getDBHelper().getVerbDAO().setTranslationEs(mVerb, text);
        }
        mTranslateView.setText(text);
    }

    private class SectionPagerAdapter extends PagerAdapter {

        SectionPagerAdapter() {
            super();
        }

        @Override
        public Object instantiateItem(ViewGroup collection, int position) {
            LayoutInflater inflater = getActivity().getLayoutInflater();
            View view;
            switch (position) {
                case 0:
                    view = inflater.inflate(R.layout.fragment_verb_ind_view, collection, false);
                    ((TextView) view.findViewById(R.id.verbViewIndPresent)).setText(mVerb.getIndPresent().toString());
                    ((TextView) view.findViewById(R.id.verbViewIndParticipi)).setText(mVerb.getParticipi());
                    ((TextView) view.findViewById(R.id.verbViewIndImperfet)).setText(mVerb.getIndImperfet().toString());
                    ((TextView) view.findViewById(R.id.verbViewIndGerundi)).setText(mVerb.getGerundi());
                    ((TextView) view.findViewById(R.id.verbViewIndFutur)).setText(mVerb.getIndFutur().toString());
                    ((TextView) view.findViewById(R.id.verbViewIndCondicional)).setText(mVerb.getIndCondicional().toString());
                    ((TextView) view.findViewById(R.id.verbViewIndPassatSimple)).setText(mVerb.getIndPassatSimple().toString());
                    break;
                case 1:
                    view = inflater.inflate(R.layout.fragment_verb_sub_view, collection, false);
                    ((TextView) view.findViewById(R.id.verbViewSubPresent)).setText(mVerb.getSubPresent().toString());
                    ((TextView) view.findViewById(R.id.verbViewSubImperfet)).setText(mVerb.getSubImperfet().toString());
                    ((TextView) view.findViewById(R.id.verbViewSubImperatiu)).setText(mVerb.getImperatiu().toString());
                    break;
                case 2:
                default:
                    view = inflater.inflate(R.layout.fragment_verb_com_view, collection, false);
                    ((TextView) view.findViewById(R.id.verbViewComPassatPerifrastic)).setText(mVerb.getComPassatPerifrastic().toString());
                    ((TextView) view.findViewById(R.id.verbViewComPerfet)).setText(mVerb.getComPerfet().toString());
                    ((TextView) view.findViewById(R.id.verbViewComPlusquamperfet)).setText(mVerb.getComPlusquamperfet().toString());
                    ((TextView) view.findViewById(R.id.verbViewComPassatAnterior)).setText(mVerb.getComPassatAnterior().toString());
                    ((TextView) view.findViewById(R.id.verbViewComPassatAnteriorPerifrastic)).setText(mVerb.getComPassatAnteriorPerifrastic().toString());
                    ((TextView) view.findViewById(R.id.verbViewComFuturPerfet)).setText(mVerb.getComFuturPerfet().toString());
                    ((TextView) view.findViewById(R.id.verbViewComCondicionalPerfet)).setText(mVerb.getComCondicionalPerfet().toString());
                    ((TextView) view.findViewById(R.id.verbViewComSubPassatPerifrastic)).setText(mVerb.getComSubPassatPerifrastic().toString());
                    ((TextView) view.findViewById(R.id.verbViewComSubPerfet)).setText(mVerb.getComSubPerfet().toString());
                    ((TextView) view.findViewById(R.id.verbViewComSubPlusquamperfet)).setText(mVerb.getComSubPlusquamperfet().toString());
                    ((TextView) view.findViewById(R.id.verbViewComSubPassatAnteriorPerifrastic)).setText(mVerb.getComSubPassaAnteriorPerifrastic().toString());
                    break;

            }
            collection.addView(view, 0);
            return view;
        }

        @Override
        public void destroyItem(ViewGroup collection, int position, Object view) {
            collection.removeView((View) view);
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return object == view;
        }

        @Override
        public int getCount() {
            return 3;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case 0:
                    return "Indicatiu";
                case 1:
                    return "Subjuntiu";
                case 2:
                default:
                    return "Compostes";
            }
        }
    }

    public interface VerbViewFragmentListener {
        void onVerbViewFinished();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        MenuItem searchItem = menu.findItem(R.id.action_search);
        searchItem.setVisible(true);
        MenuItem favoriteItem = menu.findItem(R.id.action_favorite);
        if (mVerb.isFavorite()) {
            favoriteItem.setIcon(starDrawable);
        } else {
            favoriteItem.setIcon(starInactiveDrawable);
        }
        favoriteItem.setVisible(true);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_favorite) {
            ((MyApplication) getActivity().getApplication()).getDBHelper().getVerbDAO().setFavorite(mVerb, !mVerb.isFavorite());
            if (mVerb.isFavorite()) {
                item.setIcon(starInactiveDrawable);
            } else {
                item.setIcon(starDrawable);
            }
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

}
