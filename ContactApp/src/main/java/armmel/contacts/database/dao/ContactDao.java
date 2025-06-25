package armmel.contacts.database.dao;

import armmel.contacts.database.entity.Contact;
import armmel.contacts.dto.SearchDto;
import armmel.contacts.orm.Dao;
import armmel.contacts.orm.Insert;
import armmel.contacts.orm.Param;
import armmel.contacts.orm.Query;
import armmel.contacts.orm.Update;
import java.util.List;
@Dao
public interface  ContactDao {
    @Insert
    long insert(Contact contact);
    @Update
    int  update(Contact contact);

    @Query("select * from contacts where id = :id")
    Contact getById(@Param("id") Long id);

    @Query("DELETE FROM contacts WHERE id = :id")
    int delete(@Param("id") Long id);

    @Query("DELETE FROM contacts")
    int deleteAll();

    @Query("SELECT * FROM contacts WHERE name LIKE :name ORDER BY name ASC")
    List<Contact> getAllFiltered(@Param("name") String name);

    @Query("SELECT c.id AS contact_id, c.name AS name, p.phone AS result " +
    "FROM contacts c LEFT JOIN contact_phones p ON c.id = p.contact_id WHERE p.phone like :phone ORDER BY c.name ASC")
        List<SearchDto> getContactWithPhones(@Param("phone") String phone);
    
    @Query("SELECT c.id AS contact_id, c.name AS name, p.email AS result " +
    "FROM contacts c LEFT JOIN contact_emails p ON c.id = p.contact_id WHERE p.email like :email ORDER BY c.name ASC")
        List<SearchDto> getContactWithEmail(@Param("email") String phone);
}
