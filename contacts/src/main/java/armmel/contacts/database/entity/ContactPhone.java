package armmel.contacts.database.entity;

public class ContactPhone implements ContactDetail{
    private int id;
    private Long contactId;
    private String phone;
    private String type;

    @Override
    public String getLabel() {
        return type +": "+ phone;
    }
    public ContactPhone(){}
    public ContactPhone(String phone, String type) {
        this.phone = phone;
        this.type = type;
    }

    @Override
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    @Override
    public Long getContactId() { return contactId; }
    public void setContactId(Long contactId) { this.contactId = contactId; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    @Override
    public String toString() {
        return type+": "+ phone; 
    }

}
