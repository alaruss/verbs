package com.alaruss.verbs.fragments;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ListView;
import android.widget.TextView;

import com.alaruss.verbs.MyApplication;
import com.alaruss.verbs.R;
import com.alaruss.verbs.db.VerbDAO;
import com.alaruss.verbs.models.Verb;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


public class VerbListFragment extends Fragment implements AbsListView.OnItemClickListener {
    private VerbListFragmentListener mListener;

    private AbsListView mListView;
    private EditText mSearchETView;

    private VerbListAdapter mAdapter;
    private String filterQuery;
    private VerbDAO mVerbDAO;
    private boolean needRefreshFavorite = false;

    public VerbListFragment() {
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        MenuItem searchItem = menu.findItem(R.id.action_search);
        MenuItem favoriteItem = menu.findItem(R.id.action_favorite);
        searchItem.setVisible(false);
        favoriteItem.setVisible(false);
    }

    private class VerbListAdapter extends BaseAdapter implements Filterable {
        private Context mContext;
        private LayoutInflater mInflater = null;
        private List<Verb> mAllVerbs;
        private List<Verb> mFilteredVerbs;

        public VerbListAdapter(Context context) {
            mContext = context;
            mInflater = LayoutInflater.from(mContext);
        }


        public void setVerbs(List<Verb> Verbs) {
            mAllVerbs = Verbs;
            mFilteredVerbs = Verbs;
        }

        public List<Verb> getAllVerbs() {
            return mAllVerbs;
        }

        @Override
        public int getCount() {
            return mFilteredVerbs != null ? mFilteredVerbs.size() : 0;
        }

        @Override
        public Object getItem(int position) {
            return mFilteredVerbs.get(position);
        }

        @Override
        public long getItemId(int position) {
            Verb Verb = (Verb) getItem(position);
            return Verb.getId();
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            TextView text;

            if (convertView == null) {
                convertView = mInflater.inflate(R.layout.verb_list_item, parent, false);
                convertView.setTag(R.id.titleTextView, convertView.findViewById(R.id.titleTextView));
            }

            text = (TextView) convertView.getTag(R.id.titleTextView);
            //get the current item and set the text
//            Verb item = (Verb) getItem(position);
            Verb item = (Verb) getItem(position);
            text.setText(item.getInfinitive());

            return convertView;
        }

        private ValueFilter mValueFilter;

        @Override
        public Filter getFilter() {
            if (mValueFilter == null) {

                mValueFilter = new ValueFilter();
            }

            return mValueFilter;
        }

        private class ValueFilter extends Filter {
            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                FilterResults results = new FilterResults();
                List<Verb> filteredResult = new ArrayList<>();
                if (constraint != null && constraint.length() > 0) {
                    for (Verb i : mAllVerbs) {
                        if (i.getInfinitive().startsWith((String) constraint)) {
                            filteredResult.add(i);
                        }
                    }
                } else {
                    for (Verb i : mAllVerbs) {
                        if (i.isFavorite()) {
                            filteredResult.add(i);
                        }
                    }
                }
                results.count = filteredResult.size();
                results.values = filteredResult;
                return results;
            }

            @SuppressWarnings("unchecked")
            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {
                mFilteredVerbs = (List<Verb>) results.values;
                notifyDataSetChanged();
            }
        }

    }

    @Override
    public void onResume() {
        super.onResume();
        getActivity().setTitle(R.string.app_name);
        refreshFavorites();
    }

    void refreshFavorites() {
        if (needRefreshFavorite) {
            HashMap<Integer, Verb> favorites = new HashMap<>();
            for (Verb verb : mVerbDAO.getFavoritesVerbs()) {
                favorites.put(verb.getId(), verb);
            }
            for (Verb verb : mAdapter.getAllVerbs()) {
                boolean isFavorite = favorites.containsKey(verb.getId());
                if (verb.isFavorite() != isFavorite) {
                    verb.setFavorite(isFavorite);
                }
            }
            needRefreshFavorite = false;
            mAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        mAdapter = new VerbListAdapter(getActivity());
        mVerbDAO = ((MyApplication) getActivity().getApplication()).getDBHelper().getVerbDAO();
        filterQuery = getArguments() != null ? getArguments().getString(getString(R.string.EXTRA_QUERY)) : null;
        mAdapter.setVerbs(mVerbDAO.getAllVerbs());
        mAdapter.getFilter().filter(filterQuery);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_verb_list, container, false);

        // Set the adapter
        mListView = (AbsListView) view.findViewById(android.R.id.list);
        ((ListView) mListView).setAdapter(mAdapter);

        // Set OnItemClickListener so we can be notified on item clicks
        mListView.setOnItemClickListener(this);

        mSearchETView = (EditText) view.findViewById(R.id.searchList);
        mSearchETView.addTextChangedListener(new TextWatcher() {

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                mAdapter.getFilter().filter(s);
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
        mSearchETView.setOnTouchListener(new View.OnTouchListener() {
            final int DRAWABLE_LEFT = 0;
            final int DRAWABLE_TOP = 1;
            final int DRAWABLE_RIGHT = 2;
            final int DRAWABLE_BOTTOM = 3;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    int leftEdgeOfRightDrawable = mSearchETView.getRight()
                            - mSearchETView.getCompoundDrawables()[DRAWABLE_RIGHT].getBounds().width();
                    // when EditBox has padding, adjust leftEdge like
                    // leftEdgeOfRightDrawable -= getResources().getDimension(R.dimen.edittext_padding_left_right);
                    if (event.getRawX() >= leftEdgeOfRightDrawable) {
                        mSearchETView.setText("");
                        return true;
                    }
                }
                return false;
            }
        });
        mSearchETView.requestFocus();
        return view;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (VerbListFragmentListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement VerbListFragmentListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        if (null != mListener) {
            Verb verb = (Verb) parent.getItemAtPosition(position);
            needRefreshFavorite = true;
            mListener.onVerbListSelected(verb.getId());
        }
    }

    public interface VerbListFragmentListener {
        void onVerbListSelected(int verbId);
    }
}
