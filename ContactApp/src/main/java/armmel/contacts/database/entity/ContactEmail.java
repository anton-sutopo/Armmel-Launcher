package armmel.contacts.database.entity;

import armmel.contacts.orm.Column;
import armmel.contacts.orm.Entity;

@Entity(table = "contact_emails")
public class ContactEmail implements ContactDetail {
    @Column(name= "id", id = true)
    private int id;
    @Column(name= "contact_id")
    private Long contactId;
    @Column(name= "email")
    private String email;
    @Column(name= "type")
    private String type;

    @Override
    public String getLabel() {
        return type+": "+email;
    }

    public ContactEmail() {}
    public ContactEmail(String email, String type) {
        this.email = email;
        this.type = type;
    }
    @Override
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    @Override
    public Long getContactId() { return contactId; }
    public void setContactId(Long contactId) { this.contactId = contactId; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    @Override
    public String toString() {
        return type+": "+ email; 
    }

}
