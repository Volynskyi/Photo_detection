package zhdan.photo_detection;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.List;

class DBHelper extends SQLiteOpenHelper {

    private static final int DATABASE_VERSION = 1;
    private static final String DATABASE_NAME = "tagDb";
    static final String TABLE_NAME = "Tags";

    // Labels Table Columns names
    static final String KEY_ID = "_id";
    private static final String KEY_NAME = "name";
    static final String KEY_PATH = "path";

    DBHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("create table " + TABLE_NAME + "(" + KEY_ID
                + " integer primary key," + KEY_NAME + " text," + KEY_PATH + " text" + ")");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("drop table if exists " + TABLE_NAME);
        onCreate(db);
    }

    void insertTagsIntoDB(List<String> tags, String currentPhotoPath) {
        SQLiteDatabase database = getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        for (String tag : tags) {
            contentValues.put(DBHelper.KEY_NAME, tag);
            contentValues.put(DBHelper.KEY_PATH, currentPhotoPath);
            database.insert(DBHelper.TABLE_NAME, null, contentValues);
        }
        database.close();
    }

    void removeFromDB(int id, SQLiteDatabase database) {
        database.delete(DBHelper.TABLE_NAME, DBHelper.KEY_ID + "=" + id, null);
    }
}