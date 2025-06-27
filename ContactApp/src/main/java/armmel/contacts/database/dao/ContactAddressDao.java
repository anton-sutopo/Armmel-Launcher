package armmel.contacts.database.dao;

import armmel.contacts.database.entity.ContactAddress;
import armmel.contacts.orm.Dao;
import armmel.contacts.orm.Insert;
import armmel.contacts.orm.Param;
import armmel.contacts.orm.Query;
import armmel.contacts.orm.Update;
import java.util.List;

@Dao
public interface ContactAddressDao {
  @Insert
  long insert(ContactAddress contactAddress);

  @Update
  int update(ContactAddress contactAddress);

  @Query("DELETE FROM contact_addresses WHERE id = :id")
  int delete(@Param("id") int id);

  @Query("SELECT * FROM contact_addresses WHERE contact_id = :contactId")
  public List<ContactAddress> getByContactId(@Param("contactId") Long contactId);
}
