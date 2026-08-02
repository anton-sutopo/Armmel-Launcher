package armmel.contacts.dto;

import armmel.contacts.orm.Column;

public class SearchDto {
  @Column(name = "contact_id")
  private Long contactId;

  @Column(name = "system_contact_id")
  private Long systemContactId;

  @Column(name = "name")
  private String name;

  @Column(name = "result")
  private String result;

  public Long getContactId() {
    return contactId;
  }

  public void setContactId(Long contactId) {
    this.contactId = contactId;
  }

  public Long getSystemContactId() {
    return systemContactId;
  }

  public void setSystemContactId(Long systemContactId) {
    this.systemContactId = systemContactId;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getResult() {
    return result;
  }

  public void setResult(String result) {
    this.result = result;
  }
}
