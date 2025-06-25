package armmel.contacts.database.dao;

import armmel.contacts.database.entity.ContactEmail;
import armmel.contacts.database.entity.ContactPhone;
import armmel.contacts.orm.Dao;
import armmel.contacts.orm.Insert;
import armmel.contacts.orm.Param;
import armmel.contacts.orm.Query;
import armmel.contacts.orm.Update;
import java.util.List;
@Dao
public interface ContactEmailDao {
    @Insert
    long insert(ContactEmail contactPhone);
    @Update
    int  update(ContactEmail contactPhone);
    
    @Query("DELETE FROM contact_emails WHERE id = :id")
    int delete(@Param("id") int id);

    @Query("SELECT * FROM contact_emails WHERE contact_id = :contactId")  
    public List<ContactEmail> getByContactId(@Param("contactId") Long contactId);
}
