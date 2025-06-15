package armmel.contacts.database;
import android.database.sqlite.SQLiteOpenHelper;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

public class DBHelper extends SQLiteOpenHelper {
    private static final String DB_NAME = "contacts.db";
    private static final int DB_VERSION = 1;

    public DBHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onConfigure(SQLiteDatabase db) {
        super.onConfigure(db);
        db.setForeignKeyConstraintsEnabled(true);
    } 
    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE contacts (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "name TEXT NOT NULL," +
                "org TEXT," +
                "title TEXT," +
                "created_at DATETIME DEFAULT CURRENT_TIMESTAMP" +
                ")");

        db.execSQL("CREATE TABLE contact_phones (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "contact_id INTEGER NOT NULL," +
                "phone TEXT NOT NULL," +
                "type TEXT," +
                "FOREIGN KEY(contact_id) REFERENCES contacts(id) ON DELETE CASCADE" +
                ")");

        db.execSQL("CREATE TABLE contact_emails (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "contact_id INTEGER NOT NULL," +
                "email TEXT NOT NULL," +
                "type TEXT," +
                "FOREIGN KEY(contact_id) REFERENCES contacts(id) ON DELETE CASCADE" +
                ")");

        db.execSQL("CREATE TABLE contact_addresses (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "contact_id INTEGER NOT NULL," +
                "address TEXT NOT NULL," +
                "type TEXT," +
                "FOREIGN KEY(contact_id) REFERENCES contacts(id) ON DELETE CASCADE" +
                ")");
        db.execSQL("CREATE TABLE contact_photos (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "contact_id INTEGER NOT NULL, " +
                "photo BLOB, " +
                "mime_type TEXT, " +
                "FOREIGN KEY(contact_id) REFERENCES contacts(id) ON DELETE CASCADE" +
                ")"); 
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVer, int newVer) {
        // handle upgrades
    }
}
