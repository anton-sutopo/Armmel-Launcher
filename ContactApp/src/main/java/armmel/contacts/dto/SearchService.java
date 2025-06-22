package armmel.contacts.dto;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import armmel.contacts.database.DBHelper;
import armmel.contacts.database.dao.ContactPhoneDao;
import armmel.contacts.database.entity.ContactPhone;
import armmel.contacts.utils.Utils;
import android.content.Context;
import armmel.contacts.database.dao.ContactDao;
import armmel.contacts.database.entity.Contact;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


public class SearchService {
    private Context context;
    private ContactDao contactDao;
    private final DBHelper dbHelper;
    public SearchService(Context context) {
        this.context = context;
        this.contactDao = new ContactDao(context);
        this.dbHelper = new DBHelper(context);
    }
    public List<Search> findSearchByEmailTerm(String term) {
        Map<Long, Search> searchMap = new LinkedHashMap<>();


        SQLiteDatabase db = dbHelper.getReadableDatabase();

        String sql = "SELECT c.id, c.name, p.email " +
            "FROM contacts c " +
            "JOIN contact_emails p ON c.id = p.contact_id " +
            "WHERE p.email LIKE ? " +
            "ORDER BY c.name ASC";

        String[] selectionArgs = new String[] { "%" + term.trim() + "%" };

        Cursor cursor = db.rawQuery(sql, selectionArgs);

        if (cursor != null) {
            while (cursor.moveToNext()) {
                long id = cursor.getLong(cursor.getColumnIndexOrThrow("id"));
                String name = cursor.getString(cursor.getColumnIndexOrThrow("name"));
                String email = cursor.getString(cursor.getColumnIndexOrThrow("email"));

                Search s = searchMap.get(id);
                if (s == null) {
                    s = new Search(id, name);
                    searchMap.put(id, s);
                }

                s.getResult().add(email);
            }
            cursor.close();
        }

        return new ArrayList<>(searchMap.values());
    }
    public List<Search> findSearchByPhoneTerm(String term) {
        Map<Long, Search> searchMap = new LinkedHashMap<>();


        SQLiteDatabase db = dbHelper.getReadableDatabase();

        String sql = "SELECT c.id, c.name, p.phone " +
            "FROM contacts c " +
            "JOIN contact_phones p ON c.id = p.contact_id " +
            "WHERE p.phone LIKE ? " +
            "ORDER BY c.name ASC";

        String[] selectionArgs = new String[] { "%" + term.trim() + "%" };

        Cursor cursor = db.rawQuery(sql, selectionArgs);

        if (cursor != null) {
            while (cursor.moveToNext()) {
                long id = cursor.getLong(cursor.getColumnIndexOrThrow("id"));
                String name = cursor.getString(cursor.getColumnIndexOrThrow("name"));
                String phone = cursor.getString(cursor.getColumnIndexOrThrow("phone"));

                Search s = searchMap.get(id);
                if (s == null) {
                    s = new Search(id, name);
                    searchMap.put(id, s);
                }

                s.getResult().add(phone);
            }
            cursor.close();
        }

        return new ArrayList<>(searchMap.values());
    }

    public List<Search> getAllFiltered(String term) {
        List<Search> result = new ArrayList<>(); 
        if(Utils.isPartialPhoneNumber(term)){
            term = Utils.removeForPhone(term);
            result = findSearchByPhoneTerm(term); 
        } else if(term.startsWith("@")) {
            String s = term.length() > 1? term.substring(1):"";
            result = findSearchByEmailTerm(s); 
        } else {
            List<Contact> rs = contactDao.getAllFiltered(term);
            for(Contact r: rs) {
                result.add(new Search(r.getId(), r.getName())); 
            }
        }
        return result;

    }
}
