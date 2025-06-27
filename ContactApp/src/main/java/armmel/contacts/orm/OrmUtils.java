package armmel.contacts.orm;

import android.content.ContentValues;
import android.database.Cursor;
import java.lang.reflect.Field;

public class OrmUtils {
  public static <T> T mapCursorToEntity(Cursor cursor, Class<T> clazz) throws Exception {
    T obj = clazz.getDeclaredConstructor().newInstance();
    for (Field field : clazz.getDeclaredFields()) {
      if (field.isAnnotationPresent(Column.class)) {
        String colName = field.getAnnotation(Column.class).name();
        int index = cursor.getColumnIndex(colName);
        if (index >= 0) {
          field.setAccessible(true);
          Class<?> type = field.getType();
          if (type == long.class || type == Long.class) field.set(obj, cursor.getLong(index));
          else if (type == int.class) field.set(obj, cursor.getInt(index));
          else if (type == String.class) field.set(obj, cursor.getString(index));
          else if (type == byte[].class) field.set(obj, cursor.getBlob(index));
        }
      }
    }
    return obj;
  }

  public static ContentValues toContentValues(Object entity) throws Exception {
    ContentValues values = new ContentValues();
    for (Field field : entity.getClass().getDeclaredFields()) {
      if (field.isAnnotationPresent(Column.class)) {
        Column col = field.getAnnotation(Column.class);
        if (col.id()) continue; // Skip ID field during
        field.setAccessible(true);
        Object value = field.get(entity);
        String colName = field.getAnnotation(Column.class).name();
        if (value instanceof String) values.put(colName, (String) value);
        else if (value instanceof Integer) values.put(colName, (Integer) value);
        else if (value instanceof Long) values.put(colName, (Long) value);
        else if (value instanceof byte[]) values.put(colName, (byte[]) value);
      }
    }
    return values;
  }

  public static String getTableNameFromQuery(String query) {
    System.out.println("query" + query);
    String upper = query.toUpperCase();
    if (upper.contains(" FROM ")) {
      String[] parts = upper.split(" FROM ");
      String[] afterFrom = parts[1].trim().split("[ \n\r\t]");
      System.out.println("tableName: " + afterFrom[0]);
      return afterFrom[0];
    }
    throw new IllegalArgumentException("Cannot extract table from query: " + query);
  }
}
