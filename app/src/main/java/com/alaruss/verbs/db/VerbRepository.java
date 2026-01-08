package com.alaruss.verbs.db;

import android.content.Context;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.Transformations;

import com.alaruss.verbs.db.dao.VerbRoomDao;
import com.alaruss.verbs.db.entities.VerbEntity;
import com.alaruss.verbs.models.Verb;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.zip.InflaterInputStream;

import com.google.firebase.crashlytics.FirebaseCrashlytics;

public class VerbRepository {
    private static final String LOG_TAG = VerbRepository.class.getSimpleName();
    private static final int TOTAL_VERBS_COUNT = 8494;

    private final VerbDatabase database;
    private final VerbRoomDao verbDao;
    private final ExecutorService executor;
    private final Context context;

    public VerbRepository(Context context) {
        this.context = context.getApplicationContext();
        database = VerbDatabase.getInstance(context);
        verbDao = database.verbDao();
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
        entity.setLastAccess(verb.getLastAccess() != null ? (int)(verb.getLastAccess().getTime() / 1000) : null);
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

    public int getFavoritesCountSync() {
        return verbDao.getFavoritesCount();
    }

    // Async update methods (run on background thread)

    public void updateLastAccess(Verb verb, Runnable onComplete) {
        executor.execute(() -> {
            Integer lastAccessTS = verb.getLastAccess() != null ? (int)(verb.getLastAccess().getTime() / 1000) : null;
            verbDao.updateLastAccess(verb.getId(), lastAccessTS, verb.getAccessCount());
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

    public LiveData<Integer> getFavoritesCount() {
        return verbDao.getFavoritesCountLive();
    }

    // Data migration methods using Room

    public interface ImportProgressCallback {
        void onProgress(Integer progress);
    }

    /**
     * Migration 1: Initial data population from assets
     * Reads verbs.csv and inserts all verbs into the database
     * Uses transaction to ensure atomicity
     */
    public void runDataMigration01(Context context, ImportProgressCallback progressCallback) {
        try {
            InflaterInputStream is = new InflaterInputStream(context.getAssets().open("verbs.csv"));
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));

            try {
                // Collect all entities first, then insert in transaction
                List<VerbEntity> entities = new ArrayList<>();
                String line;
                int count = 0;

                while ((line = reader.readLine()) != null) {
                    String[] rowData = line.split(";");
                    if (rowData.length < 3) {
                        Log.w(LOG_TAG, "Skipping malformed line: " + line);
                        continue;
                    }

                    VerbEntity entity = new VerbEntity();
                    entity.setInfinitive(rowData[0]);
                    entity.setData(rowData[1]);
                    try {
                        entity.setIsFavorite(Integer.parseInt(rowData[2]));
                    } catch (NumberFormatException e) {
                        entity.setIsFavorite(0);
                    }

                    if (rowData.length > 3) {
                        entity.setTranslationEn(rowData[3]);
                        if (rowData.length > 4) {
                            entity.setTranslationEs(rowData[4]);
                        }
                    }

                    entities.add(entity);
                    count++;

                    // Report progress during parsing
                    if (progressCallback != null) {
                        int progress = (int) ((count / (float) TOTAL_VERBS_COUNT) * 50);
                        progressCallback.onProgress(progress);
                    }
                }

                // Insert all entities in a transaction
                final int totalEntities = entities.size();
                final int[] insertCount = {0};
                database.runInTransaction(() -> {
                    for (VerbEntity entity : entities) {
                        verbDao.insertVerb(entity);
                        insertCount[0]++;
                        if (progressCallback != null && insertCount[0] % 100 == 0) {
                            int progress = 50 + (int) ((insertCount[0] / (float) totalEntities) * 50);
                            progressCallback.onProgress(progress);
                        }
                    }
                });

                if (progressCallback != null) {
                    progressCallback.onProgress(100);
                }
            } catch (IOException e) {
                Log.e(LOG_TAG, "IO error during migration 01", e);
                FirebaseCrashlytics.getInstance().recordException(e);
            } finally {
                try {
                    is.close();
                } catch (IOException e) {
                    Log.e(LOG_TAG, "Error closing stream", e);
                }
            }
        } catch (IOException e) {
            Log.e(LOG_TAG, "Error opening assets", e);
            FirebaseCrashlytics.getInstance().recordException(e);
        }
    }

    /**
     * Migration 2: Translation data update
     * Reads verbs.csv and updates translation columns for all verbs
     * Uses transaction to ensure atomicity
     */
    public void runDataMigration02(Context context, ImportProgressCallback progressCallback) {
        try {
            InflaterInputStream is = new InflaterInputStream(context.getAssets().open("verbs.csv"));
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));

            try {
                // Collect all updates first
                List<String[]> updates = new ArrayList<>();
                String line;
                int count = 0;

                while ((line = reader.readLine()) != null) {
                    String[] rowData = line.split(";");

                    if (rowData.length > 3) {
                        String infinitive = rowData[0];
                        String translationEn = rowData[3];
                        String translationEs = rowData.length > 4 ? rowData[4] : "";
                        updates.add(new String[]{infinitive, translationEn, translationEs});
                        count++;

                        // Report progress during parsing
                        if (progressCallback != null) {
                            int progress = (int) ((count / (float) TOTAL_VERBS_COUNT) * 50);
                            progressCallback.onProgress(progress);
                        }
                    }
                }

                // Apply all updates in a transaction
                final int totalUpdates = updates.size();
                final int[] updateCount = {0};
                database.runInTransaction(() -> {
                    for (String[] update : updates) {
                        verbDao.updateTranslationEnByInfinitive(update[0], update[1]);
                        verbDao.updateTranslationEsByInfinitive(update[0], update[2]);
                        updateCount[0]++;
                        if (progressCallback != null && updateCount[0] % 100 == 0) {
                            int progress = 50 + (int) ((updateCount[0] / (float) totalUpdates) * 50);
                            progressCallback.onProgress(progress);
                        }
                    }
                });

                if (progressCallback != null) {
                    progressCallback.onProgress(100);
                }
            } catch (IOException e) {
                Log.e(LOG_TAG, "IO error during migration 02", e);
                FirebaseCrashlytics.getInstance().recordException(e);
            } finally {
                try {
                    is.close();
                } catch (IOException e) {
                    Log.e(LOG_TAG, "Error closing stream", e);
                }
            }
        } catch (IOException e) {
            Log.e(LOG_TAG, "Error opening assets", e);
            FirebaseCrashlytics.getInstance().recordException(e);
        }
    }

    public void shutdown() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
