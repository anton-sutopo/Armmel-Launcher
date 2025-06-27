package armmel.contacts.orm;

import android.database.sqlite.SQLiteDatabase;
import java.lang.reflect.Proxy;

public class DaoFactory {
  public static <T> T create(Class<T> daoInterface, SQLiteDatabase db) {
    return (T)
        Proxy.newProxyInstance(
            daoInterface.getClassLoader(),
            new Class[] {daoInterface},
            new DaoInvocationHandler(db));
  }
}
