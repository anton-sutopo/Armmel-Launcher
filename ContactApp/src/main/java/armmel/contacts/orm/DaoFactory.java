package armmel.contacts.orm;

import java.lang.reflect.Proxy;
import android.database.sqlite.SQLiteDatabase;

public class DaoFactory {
    public static <T> T create(Class<T> daoInterface, SQLiteDatabase db) {
        return (T) Proxy.newProxyInstance(
                daoInterface.getClassLoader(),
                new Class[]{daoInterface},
                new DaoInvocationHandler(db)
        );
    }
}
