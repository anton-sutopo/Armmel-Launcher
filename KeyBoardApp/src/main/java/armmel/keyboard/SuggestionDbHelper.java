package armmel.keyboard;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

final class SuggestionDbHelper extends SQLiteOpenHelper {

  static final String DATABASE_NAME = "keyboard_suggestions.db";
  static final int DATABASE_VERSION = 1;

  static final String TABLE_WORD_RANKINGS = "word_rankings";
  static final String COLUMN_WORD = "word";
  static final String TABLE_EMOJI_RANKINGS = "emoji_rankings";
  static final String COLUMN_EMOJI = "emoji";
  static final String COLUMN_PICK_COUNT = "pick_count";
  static final String COLUMN_LAST_PICKED = "last_picked";

  SuggestionDbHelper(Context context) {
    super(context, DATABASE_NAME, null, DATABASE_VERSION);
  }

  @Override
  public void onCreate(SQLiteDatabase db) {
    db.execSQL(
        "CREATE TABLE "
            + TABLE_WORD_RANKINGS
            + " ("
            + COLUMN_WORD
            + " TEXT PRIMARY KEY,"
            + COLUMN_PICK_COUNT
            + " INTEGER NOT NULL,"
            + COLUMN_LAST_PICKED
            + " INTEGER NOT NULL"
            + ")");
    db.execSQL(
        "CREATE TABLE "
            + TABLE_EMOJI_RANKINGS
            + " ("
            + COLUMN_EMOJI
            + " TEXT PRIMARY KEY,"
            + COLUMN_PICK_COUNT
            + " INTEGER NOT NULL,"
            + COLUMN_LAST_PICKED
            + " INTEGER NOT NULL"
            + ")");
  }

  @Override
  public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    // No upgrade path yet.
  }
}
