package com.alaruss.verbs.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.provider.BaseColumns;
import android.util.Log;

import com.alaruss.verbs.models.Verb;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.zip.InflaterInputStream;

public class VerbDAO {
    private static final String LOG_TAG = VerbDAO.class.getSimpleName();
    public static final String TABLE = "verbs";
    public static final String COLUMN_ID = BaseColumns._ID;
    public static final String COLUMN_INFINITIVE = "infinitive";
    public static final String COLUMN_LAST_ACCESS = "last_access";
    public static final String COLUMN_ACCESS_COUNT = "access_count";
    public static final String COLUMN_IS_FAVORITE = "is_favorite";
    public static final String COLUMN_DATA = "data";
    public static final String COLUMN_TRANS_EN = "en";
    public static final String COLUMN_TRANS_ES = "es";

    private final int TOTAL_VERBS_COUNT = 8494;

    private SQLiteDatabase mDB;

    public VerbDAO(SQLiteDatabase db) {
        setDB(db);
    }

    public boolean isDbOpen() {
        return mDB != null && mDB.isOpen();
    }

    public void setDB(SQLiteDatabase db) {
        mDB = db;
    }

    public List<Verb> getAllVerbs() {
        return getAllVerbs(null);
    }

    public List<Verb> getAllVerbs(String query) {
        List<Verb> verbs = new ArrayList<>();
        String[] columns = {COLUMN_ID, COLUMN_INFINITIVE, COLUMN_LAST_ACCESS, COLUMN_IS_FAVORITE, COLUMN_ACCESS_COUNT};
        Cursor cursor = mDB.query(TABLE, columns, query, null, null, null, COLUMN_INFINITIVE + " ASC");

        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            Verb comment = cursorToVerb(cursor);
            verbs.add(comment);
            cursor.moveToNext();
        }
        // make sure to close the cursor
        cursor.close();
        return verbs;
    }

    public List<Verb> getFavoritesVerbs() {
        return getAllVerbs(COLUMN_IS_FAVORITE + "!=0");
    }

    public Verb getVerb(int id) {
        String[] columns = {COLUMN_ID, COLUMN_INFINITIVE, COLUMN_LAST_ACCESS, COLUMN_IS_FAVORITE,
                COLUMN_ACCESS_COUNT, COLUMN_DATA, COLUMN_TRANS_EN, COLUMN_TRANS_ES};
        Cursor cursor = mDB.query(TABLE, columns, COLUMN_ID + " = " + id, null, null, null, null);
        cursor.moveToFirst();
        Verb verb = cursorToVerb(cursor);
        cursor.close();
        return verb;
    }

    public void updateLastAccess(Verb verb) {
        int time = (int) new Date().getTime() / 1000;
        verb.setLastAccess(time);
        ContentValues values = new ContentValues();
        values.put(COLUMN_LAST_ACCESS, time);
        values.put(COLUMN_ACCESS_COUNT, verb.getAccessCount() + 1);
        mDB.update(TABLE, values, COLUMN_ID + " = " + verb.getId(), null);
    }

    public void setFavorite(Verb verb, boolean favorite) {
        verb.setFavorite(favorite);
        ContentValues values = new ContentValues();
        values.put(COLUMN_IS_FAVORITE, favorite);
        mDB.update(TABLE, values, COLUMN_ID + " = " + verb.getId(), null);
    }

    public void setTranslationEn(Verb verb, String text) {
        verb.setTranslationEn(text);
        ContentValues values = new ContentValues();
        values.put(COLUMN_TRANS_EN, text);
        mDB.update(TABLE, values, COLUMN_ID + " = " + verb.getId(), null);
    }

    public void setTranslationEs(Verb verb, String text) {
        verb.setTranslationEs(text);
        ContentValues values = new ContentValues();
        values.put(COLUMN_TRANS_ES, text);
        mDB.update(TABLE, values, COLUMN_ID + " = " + verb.getId(), null);
    }

    private Verb cursorToVerb(Cursor cursor) {
        Verb verb = new Verb();
        verb.setId(cursor.getInt(0));
        verb.setInfinitive(cursor.getString(1));
        verb.setLastAccess(cursor.getInt(2));
        verb.setFavorite(cursor.getInt(3));
        verb.setAccessCount(cursor.getInt(4));
        if (cursor.getColumnCount() > 5) {
            verb.setData(cursor.getString(5));
            verb.setTranslationEn(cursor.getString(6));
            verb.setTranslationEs(cursor.getString(7));
        }
        return verb;
    }


    public interface ImportProgressCallback {
        void onProgress(Integer progress);
    }

    public void dataMigration01(Context context, ImportProgressCallback progressCallback) {
        try {
            InflaterInputStream is = new InflaterInputStream(context.getAssets().open("verbs.csv"));
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            mDB.beginTransaction();
            try {
                String line;

                int count = 0;
                int prevProgress = -1;
                int progress;
                while ((line = reader.readLine()) != null) {
                    String[] rowData = line.split(";");
                    ContentValues values = new ContentValues();
                    values.put(COLUMN_INFINITIVE, rowData[0]);
                    values.put(COLUMN_DATA, rowData[1]);
                    values.put(COLUMN_IS_FAVORITE, rowData[2]);
                    if (rowData.length > 3) {
                        values.put(COLUMN_TRANS_EN, rowData[3]);
                        if (rowData.length > 4) {
                            values.put(COLUMN_TRANS_ES, rowData[4]);
                        }
                    }
                    mDB.insert(TABLE, null, values);
                    count++;
                    progress = (int) ((count / (float) TOTAL_VERBS_COUNT) * 100);
                    if (progressCallback != null && prevProgress != progress) {
                        prevProgress = progress;
                        progressCallback.onProgress(progress);
                    }
                }
                mDB.setTransactionSuccessful();
            } catch (IOException e) {
                Log.e(LOG_TAG, "IO" + e);
            } finally {
                try {
                    is.close();
                } catch (IOException e) {
                    Log.e(LOG_TAG, "IO" + e);
                }
                mDB.endTransaction();
            }
        } catch (IOException e) {
            Log.e(LOG_TAG, "IO" + e);
        }
    }


    public void dataMigration02(Context context, ImportProgressCallback progressCallback) {
        try {
            InflaterInputStream is = new InflaterInputStream(context.getAssets().open("verbs.csv"));
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            mDB.beginTransaction();
            try {
                String line;
                int count = 0;
                int prevProgress = -1;
                int progress;
                while ((line = reader.readLine()) != null) {
                    String[] rowData = line.split(";");
                    if (rowData.length > 3) {
                        ContentValues values = new ContentValues();
                        values.put(COLUMN_TRANS_EN, rowData[3]);
                        if (rowData.length > 4) {
                            values.put(COLUMN_TRANS_ES, rowData[4]);
                        }
                        mDB.update(TABLE, values, COLUMN_INFINITIVE + " = ?", new String[]{rowData[0]});
                        count++;
                        progress = (int) ((count / (float) TOTAL_VERBS_COUNT) * 100);
                        if (progressCallback != null && prevProgress != progress) {
                            prevProgress = progress;
                            progressCallback.onProgress(progress);
                        }
                    }
                }
                mDB.setTransactionSuccessful();
            } catch (IOException e) {
                Log.e(LOG_TAG, "IO" + e);
            } finally {
                try {
                    is.close();
                } catch (IOException e) {
                    Log.e(LOG_TAG, "IO" + e);
                }
                mDB.endTransaction();
            }
        } catch (IOException e) {
            Log.e(LOG_TAG, "IO" + e);
        }
    }
}
