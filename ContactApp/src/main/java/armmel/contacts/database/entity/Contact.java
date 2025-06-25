package armmel.contacts.database.entity;
import armmel.contacts.orm.Column;
import armmel.contacts.orm.Entity;
import java.util.List;
import java.util.ArrayList;

@Entity(table = "contacts")
public class Contact {
    @Column(name= "id", id = true)
    private Long id;
    @Column(name= "name")
    private String name;
    @Column(name= "org")
    private String org;
    @Column(name= "title")
    private String title;
    @Column(name= "created_at")
    private String createdAt;
    private List<ContactPhone> phones;
    private List<ContactEmail> emails;
    private List<ContactAddress> addresses;
    private ContactPhoto photo;
    
    public Contact() {}

    public Contact(Long id, String name,String org, String title, String createdAt) {
        this.id = id;
        this.name = name;
        this.org = org;
        this.title = title;
        this.createdAt = createdAt;
    }
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getOrg() { return org; }
    public void setOrg(String org) { this.org = org; }
    
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    
    public List<ContactPhone> getPhones() { if(phones == null){phones = new ArrayList<>();} return phones; }
    public void setPhones(List<ContactPhone> phones) { this.phones = phones; }

    public List<ContactEmail> getEmails() { if(emails == null){emails = new ArrayList<>();} return emails; }
    public void setEmails(List<ContactEmail> emails) { this.emails = emails; }
    
    public List<ContactAddress> getAddresses() { if(addresses == null){addresses = new ArrayList<>();} return addresses; }
    public void setAddresses(List<ContactAddress> addresses) { this.addresses = addresses; }
    
    public ContactPhoto getPhoto() { return photo; }
    public void setPhoto(ContactPhoto photo) { this.photo = photo; }
    
}
