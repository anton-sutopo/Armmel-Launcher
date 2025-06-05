package armmel.contacts.database.entity;

public class ContactPhoto {
    private int id;
    private Long contactId;
    private byte[] photo;
    private String mimeType;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public Long getContactId() { return contactId; }
    public void setContactId(Long contactId) { this.contactId = contactId; }

    public byte[] getPhoto() { return photo; }
    public void setPhoto(byte[] photo) { this.photo = photo; }

    public String getMimeType() { return mimeType; }
    public void setMimeType(String mimeType) { this.mimeType = mimeType; }
}
