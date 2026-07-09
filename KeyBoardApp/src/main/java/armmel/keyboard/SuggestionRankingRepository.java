package armmel.keyboard;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

final class SuggestionRankingRepository {

  private static final int MAX_WORD_RANKINGS = 4000;
  private static final int MAX_EMOJI_RANKINGS = 256;

  private final SuggestionDbHelper mDbHelper;

  SuggestionRankingRepository(Context context) {
    mDbHelper = new SuggestionDbHelper(context);
  }

  void recordWordPick(String word) {
    String normalizedWord = normalizeWord(word);
    if (normalizedWord.isEmpty()) {
      return;
    }
    recordPick(
        SuggestionDbHelper.TABLE_WORD_RANKINGS, SuggestionDbHelper.COLUMN_WORD, normalizedWord);
    pruneToLimit(
        SuggestionDbHelper.TABLE_WORD_RANKINGS, SuggestionDbHelper.COLUMN_WORD, MAX_WORD_RANKINGS);
  }

  void recordEmojiPick(String emoji) {
    if (emoji == null || emoji.trim().isEmpty()) {
      return;
    }
    recordPick(SuggestionDbHelper.TABLE_EMOJI_RANKINGS, SuggestionDbHelper.COLUMN_EMOJI, emoji);
    pruneToLimit(
        SuggestionDbHelper.TABLE_EMOJI_RANKINGS,
        SuggestionDbHelper.COLUMN_EMOJI,
        MAX_EMOJI_RANKINGS);
  }

  List<String> getRankedWords(String prefix, int limit) {
    if (limit <= 0) {
      return Collections.emptyList();
    }

    String normalizedPrefix = normalizeWord(prefix);
    if (normalizedPrefix.isEmpty()) {
      return Collections.emptyList();
    }

    SQLiteDatabase db = mDbHelper.getReadableDatabase();
    String likePrefix = normalizedPrefix + "%";
    List<String> rankedWords = new ArrayList<String>(limit);
    try (Cursor cursor =
        db.query(
            SuggestionDbHelper.TABLE_WORD_RANKINGS,
            new String[] {SuggestionDbHelper.COLUMN_WORD},
            SuggestionDbHelper.COLUMN_WORD + " LIKE ?",
            new String[] {likePrefix},
            null,
            null,
            "CASE WHEN "
                + SuggestionDbHelper.COLUMN_WORD
                + " = "
                + quote(normalizedPrefix)
                + " THEN 0 ELSE 1 END, "
                + SuggestionDbHelper.COLUMN_PICK_COUNT
                + " DESC, "
                + SuggestionDbHelper.COLUMN_LAST_PICKED
                + " DESC, LENGTH("
                + SuggestionDbHelper.COLUMN_WORD
                + ") ASC, "
                + SuggestionDbHelper.COLUMN_WORD
                + " ASC",
            Integer.toString(limit))) {
      while (cursor.moveToNext()) {
        rankedWords.add(cursor.getString(0));
      }
    }
    return rankedWords;
  }

  List<String> getRankedEmojis(int limit) {
    if (limit <= 0) {
      return Collections.emptyList();
    }

    SQLiteDatabase db = mDbHelper.getReadableDatabase();
    List<String> rankedEmojis = new ArrayList<String>(limit);
    try (Cursor cursor =
        db.query(
            SuggestionDbHelper.TABLE_EMOJI_RANKINGS,
            new String[] {SuggestionDbHelper.COLUMN_EMOJI},
            null,
            null,
            null,
            null,
            SuggestionDbHelper.COLUMN_PICK_COUNT
                + " DESC, "
                + SuggestionDbHelper.COLUMN_LAST_PICKED
                + " DESC, "
                + SuggestionDbHelper.COLUMN_EMOJI
                + " ASC",
            Integer.toString(limit))) {
      while (cursor.moveToNext()) {
        rankedEmojis.add(cursor.getString(0));
      }
    }
    return rankedEmojis;
  }

  private void recordPick(String tableName, String valueColumn, String value) {
    SQLiteDatabase db = mDbHelper.getWritableDatabase();
    long now = System.currentTimeMillis();
    db.beginTransaction();
    try {
      int updatedRows = 0;
      try (Cursor cursor =
          db.query(
              tableName,
              new String[] {SuggestionDbHelper.COLUMN_PICK_COUNT},
              valueColumn + " = ?",
              new String[] {value},
              null,
              null,
              null,
              "1")) {
        if (cursor.moveToFirst()) {
          ContentValues existingValues = new ContentValues();
          existingValues.put(SuggestionDbHelper.COLUMN_PICK_COUNT, cursor.getInt(0) + 1);
          existingValues.put(SuggestionDbHelper.COLUMN_LAST_PICKED, now);
          updatedRows =
              db.update(tableName, existingValues, valueColumn + " = ?", new String[] {value});
        }
      }
      if (updatedRows == 0) {
        ContentValues insertValues = new ContentValues();
        insertValues.put(valueColumn, value);
        insertValues.put(SuggestionDbHelper.COLUMN_PICK_COUNT, 1);
        insertValues.put(SuggestionDbHelper.COLUMN_LAST_PICKED, now);
        db.insertOrThrow(tableName, null, insertValues);
      }
      db.setTransactionSuccessful();
    } finally {
      db.endTransaction();
    }
  }

  private void pruneToLimit(String tableName, String valueColumn, int limit) {
    SQLiteDatabase db = mDbHelper.getWritableDatabase();
    db.execSQL(
        "DELETE FROM "
            + tableName
            + " WHERE "
            + valueColumn
            + " NOT IN (SELECT "
            + valueColumn
            + " FROM "
            + tableName
            + " ORDER BY "
            + SuggestionDbHelper.COLUMN_PICK_COUNT
            + " DESC, "
            + SuggestionDbHelper.COLUMN_LAST_PICKED
            + " DESC, "
            + valueColumn
            + " ASC LIMIT "
            + limit
            + ")");
  }

  private static String normalizeWord(String word) {
    return word == null ? "" : word.trim().toLowerCase(Locale.ROOT);
  }

  private static String quote(String value) {
    return "'" + value.replace("'", "''") + "'";
  }
}
