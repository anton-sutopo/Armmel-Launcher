package armmel.contacts.database.dao;

import armmel.contacts.database.entity.ContactPhoto;
import armmel.contacts.orm.Dao;
import armmel.contacts.orm.Insert;
import armmel.contacts.orm.Param;
import armmel.contacts.orm.Query;
import armmel.contacts.orm.Update;

@Dao
public interface ContactPhotoDao {
  @Insert
  long insert(ContactPhoto contactPhone);

  @Update
  int update(ContactPhoto contactPhone);

  @Query("DELETE FROM contact_photos WHERE id = :id")
  int delete(@Param("id") int id);

  @Query("SELECT * FROM contact_photos WHERE contact_id = :contactId")
  public ContactPhoto getByContactId(@Param("contactId") Long contactId);
}
