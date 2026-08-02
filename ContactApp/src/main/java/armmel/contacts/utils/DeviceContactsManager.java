package armmel.contacts.utils;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentUris;
import android.content.Context;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.os.RemoteException;
import android.provider.ContactsContract;
import armmel.contacts.database.dao.ContactAddressDao;
import armmel.contacts.database.dao.ContactEmailDao;
import armmel.contacts.database.dao.ContactPhoneDao;
import armmel.contacts.database.entity.Contact;
import armmel.contacts.database.entity.ContactAddress;
import armmel.contacts.database.entity.ContactEmail;
import armmel.contacts.database.entity.ContactPhone;
import armmel.contacts.database.repository.ContactRepository;
import java.util.ArrayList;

public class DeviceContactsManager {
  private static final String ACCOUNT_NAME = "Armmel Contacts";
  private static final String ACCOUNT_TYPE = "armmel.contacts.account";
  public enum ToggleResult {
    ADDED,
    REMOVED,
    REMOVAL_FAILED,
    CONTACT_NOT_FOUND
  }

  private final Context context;
  private final ContactRepository repository;
  private final ContactPhoneDao phoneDao;
  private final ContactEmailDao emailDao;
  private final ContactAddressDao addressDao;

  public DeviceContactsManager(Context context) {
    this.context = context;
    repository = new ContactRepository(context);
    phoneDao = repository.createDaoWriteable(ContactPhoneDao.class);
    emailDao = repository.createDaoWriteable(ContactEmailDao.class);
    addressDao = repository.createDaoWriteable(ContactAddressDao.class);
  }

  public ToggleResult toggle(Long contactId) throws RemoteException, OperationApplicationException {
    Contact contact = repository.getById(contactId);
    if (contact == null) {
      return ToggleResult.CONTACT_NOT_FOUND;
    }
    if (contact.getSystemContactId() != null && rawContactIsOwned(contact.getSystemContactId())) {
      return remove(contact) ? ToggleResult.REMOVED : ToggleResult.REMOVAL_FAILED;
    }
    if (contact.getSystemContactId() != null) {
      contact.setSystemContactId(null);
      repository.update(contact);
    }
    add(contact);
    return ToggleResult.ADDED;
  }

  private boolean rawContactExists(long rawContactId) {
    try (Cursor cursor =
        context
            .getContentResolver()
            .query(
                ContentUris.withAppendedId(ContactsContract.RawContacts.CONTENT_URI, rawContactId),
                new String[] {ContactsContract.RawContacts._ID},
                ContactsContract.RawContacts.DELETED + "=0",
                null,
                null)) {
      return cursor != null && cursor.moveToFirst();
    }
  }

  private boolean rawContactIsOwned(long rawContactId) {
    try (Cursor cursor =
        context
            .getContentResolver()
            .query(
                ContentUris.withAppendedId(ContactsContract.RawContacts.CONTENT_URI, rawContactId),
                new String[] {ContactsContract.RawContacts.ACCOUNT_TYPE},
                ContactsContract.RawContacts.DELETED + "=0",
                null,
                null)) {
      return cursor != null
          && cursor.moveToFirst()
          && ACCOUNT_TYPE.equals(cursor.getString(0));
    }
  }

  private void add(Contact contact) throws RemoteException, OperationApplicationException {
    AccountManager.get(context)
        .addAccountExplicitly(new Account(ACCOUNT_NAME, ACCOUNT_TYPE), null, null);
    ArrayList<ContentProviderOperation> operations = new ArrayList<>();
    operations.add(
        ContentProviderOperation.newInsert(ContactsContract.RawContacts.CONTENT_URI)
            .withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, ACCOUNT_TYPE)
            .withValue(ContactsContract.RawContacts.ACCOUNT_NAME, ACCOUNT_NAME)
            .build());
    operations.add(
        newDataOperation(ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
            .withValue(
                ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, contact.getName())
            .build());

    for (ContactPhone phone : phoneDao.getByContactId(contact.getId())) {
      operations.add(
          newDataOperation(ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
              .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, phone.getPhone())
              .withValue(
                  ContactsContract.CommonDataKinds.Phone.TYPE,
                  ContactsContract.CommonDataKinds.Phone.TYPE_CUSTOM)
              .withValue(ContactsContract.CommonDataKinds.Phone.LABEL, phone.getType())
              .build());
    }
    for (ContactEmail email : emailDao.getByContactId(contact.getId())) {
      operations.add(
          newDataOperation(ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE)
              .withValue(ContactsContract.CommonDataKinds.Email.ADDRESS, email.getEmail())
              .withValue(
                  ContactsContract.CommonDataKinds.Email.TYPE,
                  ContactsContract.CommonDataKinds.Email.TYPE_CUSTOM)
              .withValue(ContactsContract.CommonDataKinds.Email.LABEL, email.getType())
              .build());
    }
    for (ContactAddress address : addressDao.getByContactId(contact.getId())) {
      operations.add(
          newDataOperation(ContactsContract.CommonDataKinds.StructuredPostal.CONTENT_ITEM_TYPE)
              .withValue(
                  ContactsContract.CommonDataKinds.StructuredPostal.FORMATTED_ADDRESS,
                  address.getAddress())
              .withValue(
                  ContactsContract.CommonDataKinds.StructuredPostal.TYPE,
                  ContactsContract.CommonDataKinds.StructuredPostal.TYPE_CUSTOM)
              .withValue(
                  ContactsContract.CommonDataKinds.StructuredPostal.LABEL, address.getType())
              .build());
    }

    ContentProviderResult[] results =
        context.getContentResolver().applyBatch(ContactsContract.AUTHORITY, operations);
    contact.setSystemContactId(ContentUris.parseId(results[0].uri));
    repository.update(contact);
  }

  private ContentProviderOperation.Builder newDataOperation(String mimeType) {
    return ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
        .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
        .withValue(ContactsContract.Data.MIMETYPE, mimeType);
  }

  private boolean remove(Contact contact) {
    context
        .getContentResolver()
        .delete(
            ContentUris.withAppendedId(
                    ContactsContract.RawContacts.CONTENT_URI, contact.getSystemContactId())
                .buildUpon()
                .appendQueryParameter(ContactsContract.CALLER_IS_SYNCADAPTER, "true")
                .build(),
            null,
            null);
    if (rawContactExists(contact.getSystemContactId())) {
      return false;
    }
    contact.setSystemContactId(null);
    repository.update(contact);
    return true;
  }

}
