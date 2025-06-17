package armmel.contacts.database.dao;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.util.ArrayList;
import java.util.List;
import armmel.contacts.database.DBHelper;
import armmel.contacts.database.entity.ContactPhone;
public class ContactPhoneDao {
    private final DBHelper dbHelper;

    public ContactPhoneDao(Context context) {
        this.dbHelper = new DBHelper(context);
    }
    
    public List<ContactPhone> getAllFiltered(String term) {
        List<ContactPhone> list = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query("contact_phones", null,"phone like ?" , new String[] {"%"+term+"%"} , null, null, "phone ASC");

        if (cursor.moveToFirst()) {
            do {
                ContactPhone contact = new ContactPhone();
                contact.setId(cursor.getInt(cursor.getColumnIndexOrThrow("id")));
                contact.setPhone(cursor.getString(cursor.getColumnIndexOrThrow("phone")));
                contact.setType(cursor.getString(cursor.getColumnIndexOrThrow("type")));
                contact.setContactId(cursor.getLong(cursor.getColumnIndexOrThrow("contact_id")));
                list.add(contact);
            } while (cursor.moveToNext());
        }

        cursor.close();
        db.close();
        list.sort((a,b)-> {return a.getPhone().compareToIgnoreCase(b.getPhone());});
        return list;
    }

    public long insert(ContactPhone phone) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("contact_id", phone.getContactId());
        values.put("phone", phone.getPhone());
        values.put("type", phone.getType());

        long id = db.insert("contact_phones", null, values);
        db.close();
        return id;
    }

    public List<ContactPhone> getByContactId(Long contactId) {
        List<ContactPhone> list = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        Cursor cursor = db.query("contact_phones", null, "contact_id = ?",
                new String[]{String.valueOf(contactId)}, null, null, null);

        if (cursor.moveToFirst()) {
            do {
                ContactPhone cp = new ContactPhone();
                cp.setId(cursor.getInt(cursor.getColumnIndexOrThrow("id")));
                cp.setContactId(cursor.getLong(cursor.getColumnIndexOrThrow("contact_id")));
                cp.setPhone(cursor.getString(cursor.getColumnIndexOrThrow("phone")));
                cp.setType(cursor.getString(cursor.getColumnIndexOrThrow("type")));
                list.add(cp);
            } while (cursor.moveToNext());
        }

        cursor.close();
        db.close();
        return list;
    }

    public int update(ContactPhone phone) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("phone", phone.getPhone());
        values.put("type", phone.getType());

        int rows = db.update("contact_phones", values, "id = ?", new String[]{String.valueOf(phone.getId())});
        db.close();
        return rows;
    }

    public int delete(int id) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        int rows = db.delete("contact_phones", "id = ?", new String[]{String.valueOf(id)});
        db.close();
        return rows;
    }
}
