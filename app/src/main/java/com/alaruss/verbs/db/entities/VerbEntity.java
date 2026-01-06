package com.alaruss.verbs.db.entities;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "verbs")
public class VerbEntity {
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "_id")
    private Integer id;

    @NonNull
    @ColumnInfo(name = "infinitive")
    private String infinitive;

    @NonNull
    @ColumnInfo(name = "data")
    private String data;

    @ColumnInfo(name = "is_favorite", defaultValue = "0")
    private int isFavorite;

    @ColumnInfo(name = "last_access")
    private Integer lastAccess;

    @ColumnInfo(name = "access_count", defaultValue = "0")
    private int accessCount;

    @NonNull
    @ColumnInfo(name = "en", defaultValue = "''")
    private String translationEn;

    @NonNull
    @ColumnInfo(name = "es", defaultValue = "''")
    private String translationEs;

    // Constructors
    public VerbEntity() {
    }

    // Getters and setters
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getInfinitive() {
        return infinitive;
    }

    public void setInfinitive(String infinitive) {
        this.infinitive = infinitive;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public int getIsFavorite() {
        return isFavorite;
    }

    public void setIsFavorite(int isFavorite) {
        this.isFavorite = isFavorite;
    }

    public Integer getLastAccess() {
        return lastAccess;
    }

    public void setLastAccess(Integer lastAccess) {
        this.lastAccess = lastAccess;
    }

    public int getAccessCount() {
        return accessCount;
    }

    public void setAccessCount(int accessCount) {
        this.accessCount = accessCount;
    }

    @NonNull
    public String getTranslationEn() {
        return translationEn;
    }

    public void setTranslationEn(@NonNull String translationEn) {
        this.translationEn = translationEn;
    }

    @NonNull
    public String getTranslationEs() {
        return translationEs;
    }

    public void setTranslationEs(@NonNull String translationEs) {
        this.translationEs = translationEs;
    }
}
