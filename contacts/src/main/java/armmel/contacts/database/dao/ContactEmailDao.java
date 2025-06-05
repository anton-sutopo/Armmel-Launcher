package armmel.contacts.database.dao;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.util.ArrayList;
import java.util.List;
import armmel.contacts.database.DBHelper;
import armmel.contacts.database.entity.ContactEmail;
public class ContactEmailDao {
    private final DBHelper dbHelper;

    public ContactEmailDao(Context context) {
        this.dbHelper = new DBHelper(context);
    }

    public long insert(ContactEmail email) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("contact_id", email.getContactId());
        values.put("email", email.getEmail());
        values.put("type", email.getType());
        long id = db.insert("contact_emails", null, values);
        db.close();
        return id;
    }

    public List<ContactEmail> getByContactId(Long contactId) {
        List<ContactEmail> list = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query("contact_emails", null, "contact_id = ?",
                new String[]{String.valueOf(contactId)}, null, null, null);

        if (cursor.moveToFirst()) {
            do {
                ContactEmail ce = new ContactEmail();
                ce.setId(cursor.getInt(cursor.getColumnIndexOrThrow("id")));
                ce.setContactId(cursor.getLong(cursor.getColumnIndexOrThrow("contact_id")));
                ce.setEmail(cursor.getString(cursor.getColumnIndexOrThrow("email")));
                ce.setType(cursor.getString(cursor.getColumnIndexOrThrow("type")));
                list.add(ce);
            } while (cursor.moveToNext());
        }

        cursor.close();
        db.close();
        return list;
    }

    public int update(ContactEmail email) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("email", email.getEmail());
        values.put("type", email.getType());
        int rows = db.update("contact_emails", values, "id = ?", new String[]{String.valueOf(email.getId())});
        db.close();
        return rows;
    }

    public int delete(int id) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        int rows = db.delete("contact_emails", "id = ?", new String[]{String.valueOf(id)});
        db.close();
        return rows;
    }
}
