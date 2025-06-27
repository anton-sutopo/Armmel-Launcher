package armmel.contacts.database.entity;

import armmel.contacts.orm.Column;
import armmel.contacts.orm.Entity;

@Entity(table = "contact_photos")
public class ContactPhoto {
  @Column(name = "id", id = true)
  private int id;

  @Column(name = "contact_id")
  private Long contactId;

  @Column(name = "photo")
  private byte[] photo;

  @Column(name = "mime_type")
  private String mimeType;

  public int getId() {
    return id;
  }

  public void setId(int id) {
    this.id = id;
  }

  public Long getContactId() {
    return contactId;
  }

  public void setContactId(Long contactId) {
    this.contactId = contactId;
  }

  public byte[] getPhoto() {
    return photo;
  }

  public void setPhoto(byte[] photo) {
    this.photo = photo;
  }

  public String getMimeType() {
    return mimeType;
  }

  public void setMimeType(String mimeType) {
    this.mimeType = mimeType;
  }
}
