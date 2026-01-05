package com.alaruss.verbs.db;

import android.content.Context;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.Transformations;

import com.alaruss.verbs.db.dao.VerbRoomDao;
import com.alaruss.verbs.db.entities.VerbEntity;
import com.alaruss.verbs.models.Verb;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class VerbRepository {
    private final VerbRoomDao verbDao;
    private final ExecutorService executor;

    public VerbRepository(Context context) {
        VerbDatabase db = VerbDatabase.getInstance(context);
        verbDao = db.verbDao();
        executor = Executors.newSingleThreadExecutor();
    }

    // Convert VerbEntity to Verb model
    public static Verb entityToModel(VerbEntity entity) {
        if (entity == null) {
            return null;
        }
        Verb verb = new Verb();
        verb.setId(entity.getId());
        verb.setInfinitive(entity.getInfinitive());
        verb.setLastAccess(entity.getLastAccess() != null ? entity.getLastAccess() : 0);
        verb.setFavorite(entity.getIsFavorite());
        verb.setAccessCount(entity.getAccessCount());
        verb.setData(entity.getData());
        verb.setTranslationEn(entity.getTranslationEn());
        verb.setTranslationEs(entity.getTranslationEs());
        return verb;
    }

    // Convert Verb model to VerbEntity
    public static VerbEntity modelToEntity(Verb verb) {
        if (verb == null) {
            return null;
        }
        VerbEntity entity = new VerbEntity();
        entity.setId(verb.getId());
        entity.setInfinitive(verb.getInfinitive());
        entity.setLastAccess(verb.getLastAccess() != null ? verb.getLastAccessTS() : null);
        entity.setIsFavorite(verb.isFavorite() ? 1 : 0);
        entity.setAccessCount(verb.getAccessCount());
        entity.setData(verb.getData());
        entity.setTranslationEn(verb.getTranslationEn());
        entity.setTranslationEs(verb.getTranslationEs());
        return entity;
    }

    // Helper method to convert list of entities to list of models
    private List<Verb> entitiesToModels(List<VerbEntity> entities) {
        List<Verb> verbs = new ArrayList<>();
        if (entities != null) {
            for (VerbEntity entity : entities) {
                verbs.add(entityToModel(entity));
            }
        }
        return verbs;
    }

    // Synchronous methods for backward compatibility

    public List<Verb> getAllVerbsSync() {
        List<VerbEntity> entities = verbDao.getAllVerbsSync();
        return entitiesToModels(entities);
    }

    public List<Verb> getFavoriteVerbsSync() {
        List<VerbEntity> entities = verbDao.getFavoriteVerbsSync();
        return entitiesToModels(entities);
    }

    public Verb getVerbSync(int id) {
        VerbEntity entity = verbDao.getVerbById(id);
        return entityToModel(entity);
    }

    public List<Verb> searchVerbsSync(String query) {
        List<VerbEntity> entities = verbDao.searchVerbs(query);
        return entitiesToModels(entities);
    }

    // Async update methods (run on background thread)

    public void updateLastAccess(Verb verb, Runnable onComplete) {
        executor.execute(() -> {
            verbDao.updateLastAccess(verb.getId(), verb.getLastAccessTS(), verb.getAccessCount());
            if (onComplete != null) {
                onComplete.run();
            }
        });
    }

    public void updateFavorite(Verb verb, boolean isFavorite, Runnable onComplete) {
        executor.execute(() -> {
            verbDao.updateFavorite(verb.getId(), isFavorite ? 1 : 0);
            if (onComplete != null) {
                onComplete.run();
            }
        });
    }

    public void updateTranslationEn(Verb verb, String translation, Runnable onComplete) {
        executor.execute(() -> {
            verbDao.updateTranslationEn(verb.getId(), translation);
            if (onComplete != null) {
                onComplete.run();
            }
        });
    }

    public void updateTranslationEs(Verb verb, String translation, Runnable onComplete) {
        executor.execute(() -> {
            verbDao.updateTranslationEs(verb.getId(), translation);
            if (onComplete != null) {
                onComplete.run();
            }
        });
    }

    public void insertVerb(VerbEntity verb, Runnable onComplete) {
        executor.execute(() -> {
            verbDao.insertVerb(verb);
            if (onComplete != null) {
                onComplete.run();
            }
        });
    }

    // LiveData methods for MVVM

    public LiveData<List<Verb>> getAllVerbs() {
        return Transformations.map(verbDao.getAllVerbs(), this::entitiesToModels);
    }

    public LiveData<List<Verb>> getFavoriteVerbs() {
        return Transformations.map(verbDao.getFavoriteVerbs(), this::entitiesToModels);
    }

    public LiveData<Verb> getVerb(int id) {
        return Transformations.map(verbDao.getVerbByIdLive(id), VerbRepository::entityToModel);
    }

    // Shutdown executor
    public void shutdown() {
        executor.shutdown();
    }
}
