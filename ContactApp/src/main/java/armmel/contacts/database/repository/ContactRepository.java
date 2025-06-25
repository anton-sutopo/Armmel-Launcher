package armmel.contacts.database.repository;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.icu.text.SimpleDateFormat;
import armmel.contacts.database.dao.ContactDao;
import armmel.contacts.database.dao.ContactEmailDao;
import armmel.contacts.database.dao.ContactPhoneDao;
import armmel.contacts.database.dao.ContactAddressDao;
import armmel.contacts.database.dao.ContactPhotoDao;
import armmel.contacts.orm.DaoFactory;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import armmel.contacts.database.DBHelper;
import armmel.contacts.database.entity.Contact;
import armmel.contacts.database.entity.ContactAddress;
import armmel.contacts.database.entity.ContactEmail;
import armmel.contacts.database.entity.ContactPhone;
import armmel.contacts.database.entity.ContactPhoto;
import java.util.Locale;
public class ContactRepository {
    private final DBHelper dbHelper;
    private final Context context;
    public ContactRepository(Context context) {
        this.context = context;
        this.dbHelper = new DBHelper(context);
    }

    public long insert(Contact contact) {
        ContactDao contactDao = DaoFactory.create(ContactDao.class, dbHelper.getWritableDatabase()); 
        Long id = contactDao.insert(contact);

        if(!contact.getPhones().isEmpty()) {
            ContactPhoneDao cpd = createDaoWriteable(ContactPhoneDao.class);
            for(ContactPhone ca : contact.getPhones()) {
                ca.setContactId(id);
                cpd.insert(ca); 
            }
        }
        if(!contact.getEmails().isEmpty()) {
            ContactEmailDao ced = createDaoWriteable(ContactEmailDao.class);
            for(ContactEmail ca : contact.getEmails()) {
                ca.setContactId(id);
                ced.insert(ca); 
            }
        }
        if(!contact.getAddresses().isEmpty()) {
            ContactAddressDao cad = createDaoWriteable(ContactAddressDao.class);
            for(ContactAddress ca : contact.getAddresses()) {
                ca.setContactId(id);
                cad.insert(ca); 
            }
        }
        if(contact.getPhoto() != null) {
            ContactPhoto cp = contact.getPhoto(); 
            ContactPhotoDao cpd = createDaoWriteable(ContactPhotoDao.class);
            cp.setContactId(id);
            cpd.insert(cp);
        }
        return id;
    }

    public List<Contact> getAllFiltered(String name) {
        ContactDao contactDao = DaoFactory.create(ContactDao.class, dbHelper.getReadableDatabase());
        List<Contact> list = contactDao.getAllFiltered("%"+name+"%");
        list.sort((a,b)-> {return a.getName().compareToIgnoreCase(b.getName());});
        return list;
    }


    public Contact getById(Long id) {
        ContactDao contactDao = DaoFactory.create(ContactDao.class, dbHelper.getReadableDatabase());
        Contact contact = contactDao.getById(id);
        return contact;
    }

    public int update(Contact contact) {
        ContactDao contactDao = DaoFactory.create(ContactDao.class, dbHelper.getReadableDatabase());
        return contactDao.update(contact); 
    }

    public int delete(Long id) {
        ContactDao contactDao = DaoFactory.create(ContactDao.class, dbHelper.getReadableDatabase());
        int rows = contactDao.delete(id); 
        return rows;
    }

    public int deleteAll() {
        ContactDao contactDao = DaoFactory.create(ContactDao.class, dbHelper.getReadableDatabase());
        int rows = contactDao.deleteAll(); 
        return rows;
    }
    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()); 
    public long insertContact(String name) {
        ContactDao contactDao = DaoFactory.create(ContactDao.class, dbHelper.getWritableDatabase());
        Contact contact = new Contact();    
        contact.setName(name);
        contact.setCreatedAt(sdf.format(new Date()));
        return contactDao.insert(contact);
    }
    public <T> T createDaoWriteable(Class<T> clazz) {
        return DaoFactory.create(clazz,dbHelper.getWritableDatabase()); 
    }
}
