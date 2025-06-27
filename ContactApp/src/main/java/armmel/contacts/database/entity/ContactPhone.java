package armmel.contacts.database.entity;

import armmel.contacts.orm.Column;
import armmel.contacts.orm.Entity;

@Entity(table = "contact_phones")
public class ContactPhone implements ContactDetail {
  @Column(name = "id", id = true)
  private int id;

  @Column(name = "contact_id")
  private Long contactId;

  @Column(name = "phone")
  private String phone;

  @Column(name = "type")
  private String type;

  @Override
  public String getLabel() {
    return type + ": " + phone;
  }

  public ContactPhone() {}

  public ContactPhone(String phone, String type) {
    this.phone = phone;
    this.type = type;
  }

  @Override
  public int getId() {
    return id;
  }

  public void setId(int id) {
    this.id = id;
  }

  @Override
  public Long getContactId() {
    return contactId;
  }

  public void setContactId(Long contactId) {
    this.contactId = contactId;
  }

  public String getPhone() {
    return phone;
  }

  public void setPhone(String phone) {
    this.phone = phone;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  @Override
  public String toString() {
    return type + ": " + phone;
  }
}
