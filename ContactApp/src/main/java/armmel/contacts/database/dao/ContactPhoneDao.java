package armmel.contacts.database.dao;

import armmel.contacts.database.entity.ContactPhone;
import armmel.contacts.orm.Dao;
import armmel.contacts.orm.Insert;
import armmel.contacts.orm.Param;
import armmel.contacts.orm.Query;
import armmel.contacts.orm.Update;
import java.util.List;
@Dao
public interface ContactPhoneDao {
    @Insert
    long insert(ContactPhone contactPhone);
    @Update
    int  update(ContactPhone contactPhone);
    
    @Query("DELETE FROM contact_phones WHERE id = :id")
    int delete(@Param("id") int id);

    @Query("SELECT * FROM contact_phones WHERE contact_id = :contactId")  
    public List<ContactPhone> getByContactId(@Param("contactId") Long contactId);
}
