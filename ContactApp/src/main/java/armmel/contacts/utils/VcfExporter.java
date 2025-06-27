package armmel.contacts.utils;

import android.content.Context;
import android.util.Base64;
import armmel.contacts.database.dao.ContactAddressDao;
import armmel.contacts.database.dao.ContactEmailDao;
import armmel.contacts.database.dao.ContactPhoneDao;
import armmel.contacts.database.dao.ContactPhotoDao;
import armmel.contacts.database.entity.Contact;
import armmel.contacts.database.entity.ContactAddress;
import armmel.contacts.database.entity.ContactEmail;
import armmel.contacts.database.entity.ContactPhone;
import armmel.contacts.database.entity.ContactPhoto;
import armmel.contacts.database.repository.ContactRepository;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class VcfExporter {
  private final Context context;
  private final ContactRepository contactRepository;
  private final ContactPhoneDao contactPhoneDao;
  private final ContactEmailDao contactEmailDao;
  private final ContactAddressDao contactAddressDao;
  private final ContactPhotoDao contactPhotoDao;

  public enum VCardVersion {
    VCARD_21,
    VCARD_30
  }

  public VcfExporter(Context context) {
    this.context = context;
    this.contactRepository = new ContactRepository(context);
    this.contactPhoneDao =
        contactRepository.createDaoWriteable(
            ContactPhoneDao.class); // new ContactPhoneDao(context);
    this.contactEmailDao = contactRepository.createDaoWriteable(ContactEmailDao.class);
    this.contactAddressDao = contactRepository.createDaoWriteable(ContactAddressDao.class);
    this.contactPhotoDao = contactRepository.createDaoWriteable(ContactPhotoDao.class);
  }

  public String exportToVcf(VCardVersion version) {
    StringBuilder writer = new StringBuilder();
    List<Contact> contacts = contactRepository.getAllFiltered("");
    for (Contact contact : contacts) {
      StringBuilder vCard = new StringBuilder();
      vCard.append("BEGIN:VCARD\n");
      vCard
          .append("VERSION:")
          .append(version == VCardVersion.VCARD_21 ? "2.1" : "3.0")
          .append("\n");

      vCard.append("FN:").append(contact.getName()).append("\n");
      if (contact.getOrg() != null) vCard.append("ORG:").append(contact.getOrg()).append("\n");
      if (contact.getTitle() != null)
        vCard.append("TITLE:").append(contact.getTitle()).append("\n");

      List<ContactPhone> phones = contactPhoneDao.getByContactId(contact.getId());
      for (ContactPhone phone : phones) {
        vCard
            .append("TEL;TYPE=")
            .append(phone.getType() != null ? phone.getType() : "CELL")
            .append(":")
            .append(phone.getPhone())
            .append("\n");
      }

      List<ContactEmail> emails = contactEmailDao.getByContactId(contact.getId());
      for (ContactEmail email : emails) {
        vCard
            .append("EMAIL;TYPE=")
            .append(email.getType() != null ? email.getType() : "HOME")
            .append(":")
            .append(email.getEmail())
            .append("\n");
      }

      List<ContactAddress> addresses = contactAddressDao.getByContactId(contact.getId());
      for (ContactAddress address : addresses) {
        vCard
            .append("ADR;TYPE=")
            .append(address.getType() != null ? address.getType() : "HOME")
            .append(":;;")
            .append(address.getAddress().replace("\n", "\\n"))
            .append(";;;;\n");
      }

      ContactPhoto photo = contactPhotoDao.getByContactId(contact.getId());
      if (photo != null && photo.getPhoto() != null) {
        String base64Photo = Base64.encodeToString(photo.getPhoto(), Base64.NO_WRAP);
        String mimeType = photo.getMimeType() != null ? photo.getMimeType() : "image/jpeg";
        if (version == VCardVersion.VCARD_21) {
          vCard.append("PHOTO;ENCODING=BASE64;TYPE=").append(mimeType).append(":\n");
          vCard.append(base64Photo).append("\n");
        } else {
          vCard
              .append("PHOTO;ENCODING=b;TYPE=")
              .append(mimeType)
              .append(":")
              .append(base64Photo)
              .append("\n");
        }
      }

      // created_at (REV and custom X-CREATED)
      if (contact.getCreatedAt() != null) {
        String utcFormatted = toUtcVCardDate(contact.getCreatedAt());
        vCard.append("REV:").append(utcFormatted).append("\n");

        vCard.append("X-CREATED:").append(contact.getCreatedAt()).append("\n");
      }

      vCard.append("END:VCARD\n");
      writer.append(vCard.toString());
    }

    return writer.toString();
  }

  private String toUtcVCardDate(String localDateTime) {
    try {
      // assuming localDateTime in "yyyy-MM-dd HH:mm:ss" format (from SQLite)
      SimpleDateFormat localFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
      SimpleDateFormat utcFormat = new SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'", Locale.US);
      utcFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
      return utcFormat.format(localFormat.parse(localDateTime));
    } catch (Exception e) {
      return "19700101T000000Z"; // fallback
    }
  }
}
