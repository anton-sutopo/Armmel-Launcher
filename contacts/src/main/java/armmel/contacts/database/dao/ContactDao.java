package armmel.contacts.database.dao;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.util.ArrayList;
import java.util.List;
import armmel.contacts.database.DBHelper;
import armmel.contacts.database.entity.Contact;
import armmel.contacts.database.entity.ContactAddress;
import armmel.contacts.database.entity.ContactEmail;
import armmel.contacts.database.entity.ContactPhone;
import armmel.contacts.database.entity.ContactPhoto;
public class ContactDao {
    private final DBHelper dbHelper;
    private final Context context;
    public ContactDao(Context context) {
        this.context = context;
        this.dbHelper = new DBHelper(context);
    }

    public long insert(Contact contact) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("name", contact.getName());
        values.put("org", contact.getOrg());
        values.put("title", contact.getTitle());

        long id = db.insert("contacts", null, values);
        db.close();
        if(!contact.getPhones().isEmpty()) {
            ContactPhoneDao cpd = new ContactPhoneDao(context);
            for(ContactPhone ca : contact.getPhones()) {
                ca.setContactId(id);
                cpd.insert(ca); 
            }
        }
        if(!contact.getEmails().isEmpty()) {
            ContactEmailDao ced = new ContactEmailDao(context);
            for(ContactEmail ca : contact.getEmails()) {
                ca.setContactId(id);
                ced.insert(ca); 
            }
        }
        if(!contact.getAddresses().isEmpty()) {
            ContactAddressDao cad = new ContactAddressDao(context);
            for(ContactAddress ca : contact.getAddresses()) {
                ca.setContactId(id);
                cad.insert(ca); 
            }
        }
        if(contact.getPhoto() != null) {
            ContactPhoto cp = contact.getPhoto(); 
            ContactPhotoDao cpd = new ContactPhotoDao(context);
            cp.setContactId(id);
            cpd.insert(cp);
        }
        return id;
    }

    public List<Contact> getAllSortByName() {
        List<Contact> list = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query("contacts", null, null, null, null, null, "name asc");

        if (cursor.moveToFirst()) {
            do {
                Contact contact = new Contact();
                contact.setId(cursor.getLong(cursor.getColumnIndexOrThrow("id")));
                contact.setName(cursor.getString(cursor.getColumnIndexOrThrow("name")));
                contact.setOrg(cursor.getString(cursor.getColumnIndexOrThrow("org")));
                contact.setTitle(cursor.getString(cursor.getColumnIndexOrThrow("title")));
                contact.setCreatedAt(cursor.getString(cursor.getColumnIndexOrThrow("created_at")));
                list.add(contact);
            } while (cursor.moveToNext());
        }

        cursor.close();
        db.close();
        return list;
    }
    public List<Contact> getAllFiltered(String name) {
        List<Contact> list = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query("contacts", null,"name like ?" , new String[] {"%"+name+"%"} , null, null, "name ASC");

        if (cursor.moveToFirst()) {
            do {
                Contact contact = new Contact();
                contact.setId(cursor.getLong(cursor.getColumnIndexOrThrow("id")));
                contact.setName(cursor.getString(cursor.getColumnIndexOrThrow("name")));
                contact.setOrg(cursor.getString(cursor.getColumnIndexOrThrow("org")));
                contact.setTitle(cursor.getString(cursor.getColumnIndexOrThrow("title")));
                contact.setCreatedAt(cursor.getString(cursor.getColumnIndexOrThrow("created_at")));
                list.add(contact);
            } while (cursor.moveToNext());
        }

        cursor.close();
        db.close();
        list.sort((a,b)-> {return a.getName().compareToIgnoreCase(b.getName());});
        return list;
    }

    public List<Contact> getAll() {
        List<Contact> list = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query("contacts", null, null, null, null, null, "created_at DESC");

        if (cursor.moveToFirst()) {
            do {
                Contact contact = new Contact();
                contact.setId(cursor.getLong(cursor.getColumnIndexOrThrow("id")));
                contact.setName(cursor.getString(cursor.getColumnIndexOrThrow("name")));
                contact.setOrg(cursor.getString(cursor.getColumnIndexOrThrow("org")));
                contact.setTitle(cursor.getString(cursor.getColumnIndexOrThrow("title")));
                contact.setCreatedAt(cursor.getString(cursor.getColumnIndexOrThrow("created_at")));
                list.add(contact);
            } while (cursor.moveToNext());
        }

        cursor.close();
        db.close();
        return list;
    }

    public Contact getById(Long id) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query("contacts", null, "id = ?", new String[]{String.valueOf(id)}, null, null, null);

        Contact contact = null;
        if (cursor.moveToFirst()) {
            contact = new Contact();
            contact.setId(cursor.getLong(cursor.getColumnIndexOrThrow("id")));
            contact.setName(cursor.getString(cursor.getColumnIndexOrThrow("name")));
            contact.setOrg(cursor.getString(cursor.getColumnIndexOrThrow("org")));
            contact.setTitle(cursor.getString(cursor.getColumnIndexOrThrow("title")));
            contact.setCreatedAt(cursor.getString(cursor.getColumnIndexOrThrow("created_at")));
        }

        cursor.close();
        db.close();
        return contact;
    }

    public int update(Contact contact) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("name", contact.getName());
        values.put("org", contact.getOrg());
        values.put("title", contact.getTitle());

        int rows = db.update("contacts", values, "id = ?", new String[]{String.valueOf(contact.getId())});
        db.close();
        return rows;
    }

    public int delete(Long id) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        int rows = db.delete("contacts", "id = ?", new String[]{String.valueOf(id)});
        db.close();
        return rows;
    }

    public int deleteAll() {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        int rows = db.delete("contacts", null, null);
        db.close();
        return rows;
    }
    public long insertContact(String name) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("name", name);
        values.put("created_at", System.currentTimeMillis()); // optional
        return db.insert("contacts", null, values);
    }
}
