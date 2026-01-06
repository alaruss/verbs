package com.alaruss.verbs.db;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.alaruss.verbs.db.dao.VerbRoomDao;
import com.alaruss.verbs.db.entities.VerbEntity;

@Database(entities = {VerbEntity.class}, version = 2, exportSchema = false)
public abstract class VerbDatabase extends RoomDatabase {

    public abstract VerbRoomDao verbDao();

    private static volatile VerbDatabase INSTANCE;

    public static VerbDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (VerbDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                            context.getApplicationContext(),
                            VerbDatabase.class,
                            "verbs.db"  // MUST match existing database name
                    )
                    .addMigrations(MIGRATION_1_2)
                    .build();
                }
            }
        }
        return INSTANCE;
    }

    // Migration from version 1 to 2 (matches existing schema)
    // This migration adds the "en" and "es" columns which were already added by DBHelper
    static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            // Schema already updated in VerbRepository.runDataMigration02()
        }
    };
}
