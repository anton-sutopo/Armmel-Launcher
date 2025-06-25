package armmel.contacts.database.entity;

import armmel.contacts.orm.Column;
import armmel.contacts.orm.Entity;

@Entity(table="contact_addresses")
public class ContactAddress implements ContactDetail{
    @Column(name= "id", id = true)
    private int id;
    @Column(name= "contact_id")
    private Long contactId;
    @Column(name= "address")
    private String address;
    @Column(name= "type")
    private String type;

    @Override
    public String getLabel() {
        return type+": "+ address; 
    }
    public ContactAddress() {}
    public ContactAddress(String address, String type) {
        this.address = address;
        this.type = type;
    }
    @Override
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    @Override
    public Long getContactId() { return contactId; }
    public void setContactId(Long contactId) { this.contactId = contactId; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    @Override
    public String toString() {
        return type+": "+ address; 
    }
}
