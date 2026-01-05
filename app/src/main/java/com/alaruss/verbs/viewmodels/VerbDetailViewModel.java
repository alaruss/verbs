package com.alaruss.verbs.viewmodels;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.alaruss.verbs.db.VerbRepository;
import com.alaruss.verbs.models.Verb;

public class VerbDetailViewModel extends AndroidViewModel {
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

    public void setVerbId(int id) {
        verbId.setValue(id);
    }

    public LiveData<Verb> getVerb() {
        return verb;
    }

    public void updateFavorite(Verb verb, boolean isFavorite) {
        if (verb != null) {
            verb.setFavorite(isFavorite);
            repository.updateFavorite(verb, isFavorite, null);
        }
    }

    public void updateLastAccess(Verb verb) {
        if (verb != null) {
            repository.updateLastAccess(verb, null);
        }
    }

    public void updateTranslationEn(Verb verb, String translation) {
        if (verb != null) {
            verb.setTranslationEn(translation);
            repository.updateTranslationEn(verb, translation, null);
        }
    }

    public void updateTranslationEs(Verb verb, String translation) {
        if (verb != null) {
            verb.setTranslationEs(translation);
            repository.updateTranslationEs(verb, translation, null);
        }
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        repository.shutdown();
    }
}
