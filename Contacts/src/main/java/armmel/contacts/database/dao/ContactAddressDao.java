package armmel.contacts.database.dao;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.util.ArrayList;
import java.util.List;
import armmel.contacts.database.DBHelper;
import armmel.contacts.database.entity.ContactAddress;

public class ContactAddressDao {
    private final DBHelper dbHelper;

    public ContactAddressDao(Context context) {
        this.dbHelper = new DBHelper(context);
    }

    public long insert(ContactAddress address) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("contact_id", address.getContactId());
        values.put("address", address.getAddress());
        values.put("type", address.getType());

        long id = db.insert("contact_addresses", null, values);
        db.close();
        return id;
    }

    public List<ContactAddress> getByContactId(Long contactId) {
        List<ContactAddress> list = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query("contact_addresses", null, "contact_id = ?",
                new String[]{String.valueOf(contactId)}, null, null, null);

        if (cursor.moveToFirst()) {
            do {
                ContactAddress ca = new ContactAddress();
                ca.setId(cursor.getInt(cursor.getColumnIndexOrThrow("id")));
                ca.setContactId(cursor.getLong(cursor.getColumnIndexOrThrow("contact_id")));
                ca.setAddress(cursor.getString(cursor.getColumnIndexOrThrow("address")));
                ca.setType(cursor.getString(cursor.getColumnIndexOrThrow("type")));
                list.add(ca);
            } while (cursor.moveToNext());
        }

        cursor.close();
        db.close();
        return list;
    }

    public int update(ContactAddress address) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("address", address.getAddress());
        values.put("type", address.getType());

        int rows = db.update("contact_addresses", values, "id = ?", new String[]{String.valueOf(address.getId())});
        db.close();
        return rows;
    }

    public int delete(int id) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        int rows = db.delete("contact_addresses", "id = ?", new String[]{String.valueOf(id)});
        db.close();
        return rows;
    }
}
