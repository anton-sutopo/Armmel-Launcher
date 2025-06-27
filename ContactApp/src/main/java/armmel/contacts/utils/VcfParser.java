package armmel.contacts.utils;

import android.util.Base64;
import armmel.contacts.database.entity.Contact;
import armmel.contacts.database.entity.ContactAddress;
import armmel.contacts.database.entity.ContactEmail;
import armmel.contacts.database.entity.ContactPhone;
import armmel.contacts.database.entity.ContactPhoto;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;

public class VcfParser {

  public static List<Contact> parse(InputStream inputStream) throws Exception {
    List<Contact> contacts = new ArrayList<>();
    BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
    String line;
    Contact contact = null;
    StringBuilder photoBuilder = null;
    boolean readingPhoto = false;
    String photoMimeType = null;

    while ((line = reader.readLine()) != null) {
      line = line.trim();

      if (line.equalsIgnoreCase("BEGIN:VCARD")) {
        contact = new Contact();
      } else if (line.startsWith("FN:") && contact != null) {
        contact.setName(line.substring(3));
      } else if (line.startsWith("ORG:") && contact != null) {
        contact.setOrg(line.substring(4));
      } else if (line.startsWith("TITLE:") && contact != null) {
        contact.setTitle(line.substring(6));
      } else if (line.startsWith("TEL") && contact != null) {
        int idx = line.indexOf(':');
        if (idx != -1) {
          String number = line.substring(idx + 1).trim();
          String type = "";

          // Extract type (between TEL; and :)
          int typeStart = line.indexOf("TYPE=");
          if (typeStart != -1 && typeStart < idx) {
            // Supports multiple types like TYPE=HOME,VOICE
            int typeEnd = line.indexOf(':');
            type = line.substring(typeStart + 5, typeEnd);
          }

          contact.getPhones().add(new ContactPhone(number, type));
        }
      } else if (line.startsWith("EMAIL") && contact != null) {
        int idx = line.indexOf(':');
        if (idx != -1) {
          String email = line.substring(idx + 1).trim();
          String type = "";

          int typeStart = line.indexOf("TYPE=");
          if (typeStart != -1 && typeStart < idx) {
            int typeEnd = line.indexOf(':');
            type = line.substring(typeStart + 5, typeEnd);
          }

          contact.getEmails().add(new ContactEmail(email, type));
        }
      } else if (line.startsWith("ADR") && contact != null) {
        int idx = line.indexOf(':');
        if (idx != -1) {
          String address = line.substring(idx + 1).replace(";", " ").trim();
          String type = "";

          int typeStart = line.indexOf("TYPE=");
          if (typeStart != -1 && typeStart < idx) {
            int typeEnd = line.indexOf(':');
            type = line.substring(typeStart + 5, typeEnd);
          }

          contact.getAddresses().add(new ContactAddress(address, type));
        }
      } else if (line.startsWith("PHOTO") && contact != null) {
        // Start reading base64 photo data
        int idx = line.indexOf("ENCODING=b");
        int mimeIdx = line.indexOf("TYPE=");
        photoMimeType =
            (mimeIdx != -1 && line.contains(":"))
                ? line.substring(mimeIdx + 5, line.indexOf(':'))
                : "image/jpeg";
        String base64 = line.substring(line.indexOf(':') + 1);
        photoBuilder = new StringBuilder(base64);
        readingPhoto = true;
      } else if (readingPhoto && !line.startsWith("END:VCARD")) {
        // Continuation of base64 photo
        photoBuilder.append(line);
      } else if (line.equalsIgnoreCase("END:VCARD") && contact != null) {
        if (readingPhoto && photoBuilder != null) {
          try {
            ContactPhoto photo = new ContactPhoto();
            photo.setPhoto(Base64.decode(photoBuilder.toString(), Base64.DEFAULT));
            photo.setMimeType(photoMimeType);
            contact.setPhoto(photo);
          } catch (IllegalArgumentException e) {
            contact.setPhoto(null);
          }
        }
        contacts.add(contact);
        contact = null;
        photoBuilder = null;
        readingPhoto = false;
      }
    }

    reader.close();
    return contacts;
  }
}
