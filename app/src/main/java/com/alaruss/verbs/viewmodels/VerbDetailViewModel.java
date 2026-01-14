package com.alaruss.verbs.viewmodels;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.alaruss.verbs.db.VerbRepository;
import com.alaruss.verbs.models.Verb;

import java.util.Date;

public class VerbDetailViewModel extends AndroidViewModel {
    private static final String TAG = "VerbDetailViewModel";
    private final VerbRepository repository;
    private final MutableLiveData<Integer> verbId;
    private final LiveData<Verb> verb;

    public VerbDetailViewModel(@NonNull Application application) {
        super(application);
        repository = new VerbRepository(application);
        verbId = new MutableLiveData<>();
        verb = Transformations.switchMap(verbId, id -> {
            if (id != null && id > 0) {
                return repository.getVerb(id);
            } else {
                return new MutableLiveData<>(null);
            }
        });
    }

    public int getVerbId() {
        Integer id = verbId.getValue();
        return id != null ? id : 0;
    }

    public void setVerbId(int id) {
        verbId.setValue(id);
    }

    public LiveData<Verb> getVerb() {
        return verb;
    }

    public void updateFavorite(Verb verb, boolean isFavorite) {
        if (verb != null) {
            verb.setFavorite(isFavorite);
            repository.updateFavorite(verb, isFavorite, () -> {
                Log.d(TAG, "Favorite updated for verb: " + verb.getInfinitive() + " -> " + isFavorite);
            });
        }
    }

    public void updateLastAccess(Verb verb) {
        if (verb != null) {
            verb.setLastAccess(new Date());
            verb.incAccessCount();
            repository.updateLastAccess(verb, () -> {
                Log.d(TAG, "Last access updated for verb: " + verb.getInfinitive() + " -> " + verb.getLastAccess() + " total -> " + verb.getAccessCount());
            });
        }
    }

    public void updateTranslationEn(Verb verb, String translation) {
        if (verb != null) {
            verb.setTranslationEn(translation);
            repository.updateTranslationEn(verb, translation, () -> {
                Log.d(TAG, "English translation updated for verb: " + verb.getInfinitive());
            });
        }
    }

    public void updateTranslationEs(Verb verb, String translation) {
        if (verb != null) {
            verb.setTranslationEs(translation);
            repository.updateTranslationEs(verb, translation, () -> {
                Log.d(TAG, "Spanish translation updated for verb: " + verb.getInfinitive());
            });
        }
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        repository.shutdown();
    }
}
