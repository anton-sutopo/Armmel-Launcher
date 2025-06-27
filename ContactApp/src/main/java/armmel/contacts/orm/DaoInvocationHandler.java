package armmel.contacts.orm;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;
import java.lang.reflect.*;
import java.util.*;

public class DaoInvocationHandler implements InvocationHandler {
  private final SQLiteDatabase db;

  public DaoInvocationHandler(SQLiteDatabase db) {
    this.db = db;
  }

  @Override
  public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
    if (method.isAnnotationPresent(Query.class)) {
      Query query = method.getAnnotation(Query.class);
      String rawSql = query.value().trim();
      String rawSqlUpper = rawSql.toUpperCase();
      Parameter[] parameters = method.getParameters();
      for (int i = 0; i < parameters.length; i++) {
        Param param = parameters[i].getAnnotation(Param.class);
        if (param != null) {
          rawSql = rawSql.replace(":" + param.value(), "'" + args[i].toString() + "'");
        }
      }

      Log.d(DaoInvocationHandler.class.toString(), "Executing SQL: " + rawSql);
      if (rawSqlUpper.startsWith("DELETE")) {
        String tableName = OrmUtils.getTableNameFromQuery(rawSql);
        String whereClause = null;
        List<String> whereArgs = new ArrayList<>();

        if (rawSql.contains("WHERE")) {
          whereClause = rawSql.substring(rawSqlUpper.indexOf("WHERE") + 5).trim();
          // Optional: parse and replace args for security instead of raw SQL
        }

        int deleted = db.delete(tableName, whereClause, null);
        return deleted;
      }

      Cursor cursor = db.rawQuery(rawSql, null);

      Class<?> returnType = method.getReturnType();
      Type genericReturnType = method.getGenericReturnType();

      if (returnType == List.class && genericReturnType instanceof ParameterizedType) {
        Type listType = ((ParameterizedType) genericReturnType).getActualTypeArguments()[0];
        Class<?> elementType = (Class<?>) listType;
        List<Object> results = new ArrayList<>();
        while (cursor.moveToNext()) {
          if (elementType == String.class) results.add(cursor.getString(0));
          else if (elementType == Integer.class || elementType == int.class)
            results.add(cursor.getInt(0));
          else if (elementType == Long.class || elementType == long.class)
            results.add(cursor.getLong(0));
          else results.add(OrmUtils.mapCursorToEntity(cursor, elementType));
        }
        cursor.close();
        return results;
      } else if (!cursor.isClosed() && cursor.moveToFirst()) {
        Object result;
        if (returnType == String.class) result = cursor.getString(0);
        else if (returnType == Integer.class || returnType == int.class) result = cursor.getInt(0);
        else if (returnType == Long.class || returnType == long.class) result = cursor.getLong(0);
        else result = OrmUtils.mapCursorToEntity(cursor, returnType);
        cursor.close();
        return result;
      }

      cursor.close();
      return null;

    } else if (method.isAnnotationPresent(Insert.class)) {
      Object entity = args[0];
      Class<?> clazz = entity.getClass();
      String tableName = clazz.getAnnotation(Entity.class).table();
      ContentValues values = OrmUtils.toContentValues(entity);
      return db.insert(tableName, null, values);
    } else if (method.isAnnotationPresent(Update.class)) {
      Object entity = args[0];
      Class<?> clazz = entity.getClass();
      String tableName = clazz.getAnnotation(Entity.class).table();

      ContentValues values = OrmUtils.toContentValues(entity);

      // Find primary key field
      Field idField = null;
      String idColumn = null;
      for (Field field : clazz.getDeclaredFields()) {
        if (field.isAnnotationPresent(Column.class)) {
          Column col = field.getAnnotation(Column.class);
          if (col.id()) {
            idField = field;
            idColumn = col.name();
            break;
          }
        }
      }
      if (idField == null) throw new IllegalStateException("No @Column marked as id found.");
      idField.setAccessible(true);
      Object idValue = idField.get(entity);

      return db.update(
          tableName, values, idColumn + " = ?", new String[] {String.valueOf(idValue)});
    }

    throw new UnsupportedOperationException("Unsupported method: " + method.getName());
  }
}
