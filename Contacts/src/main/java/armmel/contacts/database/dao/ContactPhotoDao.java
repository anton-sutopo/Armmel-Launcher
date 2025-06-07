package armmel.contacts.database.dao;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import armmel.contacts.database.DBHelper;
import armmel.contacts.database.entity.ContactPhoto;
public class ContactPhotoDao {
    private final DBHelper dbHelper;

    public ContactPhotoDao(Context context) {
        this.dbHelper = new DBHelper(context);
    }

    public long insert(ContactPhoto photo) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("contact_id", photo.getContactId());
        values.put("photo", photo.getPhoto());
        values.put("mime_type", photo.getMimeType());

        long id = db.insert("contact_photos", null, values);
        db.close();
        return id;
    }

    public ContactPhoto getByContactId(Long contactId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query("contact_photos", null, "contact_id = ?",
                new String[]{String.valueOf(contactId)}, null, null, null);

        ContactPhoto cp = null;
        if (cursor.moveToFirst()) {
            cp = new ContactPhoto();
            cp.setId(cursor.getInt(cursor.getColumnIndexOrThrow("id")));
            cp.setContactId(cursor.getLong(cursor.getColumnIndexOrThrow("contact_id")));
            cp.setPhoto(cursor.getBlob(cursor.getColumnIndexOrThrow("photo")));
            cp.setMimeType(cursor.getString(cursor.getColumnIndexOrThrow("mime_type")));
        }

        cursor.close();
        db.close();
        return cp;
    }

    public int deleteByContactId(int contactId) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        int rows = db.delete("contact_photos", "contact_id = ?", new String[]{String.valueOf(contactId)});
        db.close();
        return rows;
    }
}
