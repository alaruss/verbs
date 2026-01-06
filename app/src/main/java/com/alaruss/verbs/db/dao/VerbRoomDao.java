package com.alaruss.verbs.db.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.alaruss.verbs.db.entities.VerbEntity;

import java.util.List;

@Dao
public interface VerbRoomDao {

    // Get all verbs (with LiveData for reactive updates)
    @Query("SELECT * FROM verbs ORDER BY infinitive ASC")
    LiveData<List<VerbEntity>> getAllVerbs();

    // Get all verbs synchronously (for backward compatibility)
    @Query("SELECT * FROM verbs ORDER BY infinitive ASC")
    List<VerbEntity> getAllVerbsSync();

    // Get favorite verbs (with LiveData)
    @Query("SELECT * FROM verbs WHERE is_favorite != 0 ORDER BY infinitive ASC")
    LiveData<List<VerbEntity>> getFavoriteVerbs();

    // Get favorite verbs synchronously
    @Query("SELECT * FROM verbs WHERE is_favorite != 0 ORDER BY infinitive ASC")
    List<VerbEntity> getFavoriteVerbsSync();

    // Get verb by ID (synchronous)
    @Query("SELECT * FROM verbs WHERE _id = :id")
    VerbEntity getVerbById(int id);

    // Get verb by ID (with LiveData)
    @Query("SELECT * FROM verbs WHERE _id = :id")
    LiveData<VerbEntity> getVerbByIdLive(int id);

    // Update last access time and count (parameterized to prevent SQL injection)
    @Query("UPDATE verbs SET last_access = :lastAccess, access_count = :accessCount WHERE _id = :id")
    void updateLastAccess(int id, int lastAccess, int accessCount);

    // Update favorite status (parameterized to prevent SQL injection)
    @Query("UPDATE verbs SET is_favorite = :isFavorite WHERE _id = :id")
    void updateFavorite(int id, int isFavorite);

    // Update English translation (parameterized to prevent SQL injection)
    @Query("UPDATE verbs SET en = :translation WHERE _id = :id")
    void updateTranslationEn(int id, String translation);

    // Update Spanish translation (parameterized to prevent SQL injection)
    @Query("UPDATE verbs SET es = :translation WHERE _id = :id")
    void updateTranslationEs(int id, String translation);

    // Insert verb
    @Insert
    long insertVerb(VerbEntity verb);

    // Update verb
    @Update
    void updateVerb(VerbEntity verb);

    // Get verbs by query (for search functionality)
    @Query("SELECT * FROM verbs WHERE infinitive LIKE :query || '%' ORDER BY infinitive ASC")
    List<VerbEntity> searchVerbs(String query);

    // Update English translation by infinitive (for data migration)
    @Query("UPDATE verbs SET en = :translationEn WHERE infinitive = :infinitive")
    void updateTranslationEnByInfinitive(String infinitive, String translationEn);

    // Update Spanish translation by infinitive (for data migration)
    @Query("UPDATE verbs SET es = :translationEs WHERE infinitive = :infinitive")
    void updateTranslationEsByInfinitive(String infinitive, String translationEs);
}
