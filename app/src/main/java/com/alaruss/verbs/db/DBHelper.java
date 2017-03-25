package com.alaruss.verbs.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DBHelper extends SQLiteOpenHelper {
    private static final String LOG_TAG = DBHelper.class.getSimpleName();
    private static final String DATABASE_NAME = "verbs.db";
    private static final int DATABASE_VERSION = 2;

    private VerbDAO mVerbDAO;

    public DBHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + VerbDAO.TABLE + "(" + VerbDAO.COLUMN_ID +
                " integer primary key autoincrement, " + VerbDAO.COLUMN_INFINITIVE + " text not null, " +
                VerbDAO.COLUMN_DATA + " text not null, " + VerbDAO.COLUMN_IS_FAVORITE + " integer not null default 0, "
                + VerbDAO.COLUMN_LAST_ACCESS + " integer, " + VerbDAO.COLUMN_ACCESS_COUNT + " integer not null default 0);");
        onUpgrade(db, 1, DATABASE_VERSION);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE " + VerbDAO.TABLE + " ADD COLUMN " + VerbDAO.COLUMN_TRANS_EN + " text not null default ''");
            db.execSQL("ALTER TABLE " + VerbDAO.TABLE + " ADD COLUMN " + VerbDAO.COLUMN_TRANS_ES + " text not null default ''");
        }
    }

    public VerbDAO getVerbDAO() {
        if (mVerbDAO == null) {
            mVerbDAO = new VerbDAO(getWritableDatabase());
        } else if (!mVerbDAO.isDbOpen()) {
            mVerbDAO.setDB(getWritableDatabase());
        }
        return mVerbDAO;
    }

    public void open() {
        if (mVerbDAO != null) {
            mVerbDAO.setDB(getWritableDatabase());
        }
    }
}
