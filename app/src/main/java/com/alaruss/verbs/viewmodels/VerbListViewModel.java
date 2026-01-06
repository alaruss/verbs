package com.alaruss.verbs.viewmodels;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;

import com.alaruss.verbs.db.VerbRepository;
import com.alaruss.verbs.models.Verb;

import java.util.ArrayList;
import java.util.List;

public class VerbListViewModel extends AndroidViewModel {
    private final VerbRepository repository;
    private final LiveData<List<Verb>> allVerbs;
    private final MutableLiveData<String> searchQuery;
    private final MediatorLiveData<List<Verb>> filteredVerbs;

    public VerbListViewModel(@NonNull Application application) {
        super(application);
        repository = new VerbRepository(application);
        allVerbs = repository.getAllVerbs();
        searchQuery = new MutableLiveData<>("");
        filteredVerbs = new MediatorLiveData<>();

        // Set up filtering logic
        filteredVerbs.addSource(allVerbs, verbs -> {
            filteredVerbs.setValue(filterVerbs(verbs, searchQuery.getValue()));
        });

        filteredVerbs.addSource(searchQuery, query -> {
            filteredVerbs.setValue(filterVerbs(allVerbs.getValue(), query));
        });
    }

    public LiveData<List<Verb>> getFilteredVerbs() {
        return filteredVerbs;
    }

    public void setSearchQuery(String query) {
        searchQuery.setValue(query != null ? query : "");
    }

    public String getSearchQuery() {
        return searchQuery.getValue();
    }

    private List<Verb> filterVerbs(List<Verb> verbs, String query) {
        if (verbs == null) {
            return new ArrayList<>();
        }

        List<Verb> filtered = new ArrayList<>();

        if (query != null && !query.isEmpty()) {
            // Filter by search query
            for (Verb verb : verbs) {
                if (verb.containsWord(query)) {
                    filtered.add(verb);
                }
            }
        } else {
            // Show only favorites when no search query
            for (Verb verb : verbs) {
                if (verb.isFavorite()) {
                    filtered.add(verb);
                }
            }
        }

        return filtered;
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        repository.shutdown();
    }
}
