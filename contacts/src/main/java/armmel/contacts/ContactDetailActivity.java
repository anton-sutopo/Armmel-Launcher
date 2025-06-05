package armmel.contacts;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.ImageView;
import android.widget.Toast;
import java.util.List;
import android.app.Activity;
import armmel.contacts.database.dao.ContactDao;
import armmel.contacts.database.dao.ContactPhoneDao;
import armmel.contacts.database.dao.ContactEmailDao;
import armmel.contacts.database.dao.ContactAddressDao;
import armmel.contacts.database.dao.ContactPhoneDao;
import armmel.contacts.database.dao.ContactPhotoDao;
import armmel.contacts.database.entity.Contact;
import armmel.contacts.database.entity.ContactPhone;
import armmel.contacts.database.entity.ContactEmail;
import armmel.contacts.database.entity.ContactAddress;
import armmel.contacts.database.entity.ContactDetail;
import armmel.contacts.database.entity.ContactPhoto;
import android.text.SpannableStringBuilder;
import android.text.style.ClickableSpan;
import android.view.View;
import android.content.Intent;
import android.net.Uri;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.widget.Toast;
import android.widget.LinearLayout;
import android.graphics.Color;
import android.graphics.Paint;
import android.widget.Button;
import android.widget.ImageButton;
import android.view.LayoutInflater;
import android.widget.EditText;
import android.app.AlertDialog;
import android.view.Gravity;
import java.util.function.Consumer;
import android.text.InputType;
import android.graphics.BitmapFactory;
import android.graphics.Bitmap;
import android.widget.PopupMenu;
public class ContactDetailActivity extends Activity {

    private TextView txtName, txtCreatedAt;
    private LinearLayout phonesContainer;
    private LinearLayout emailContainer;
    private LinearLayout addressContainer;
    private ImageButton addPhone; 
    private ImageButton addEmail; 
    private ImageButton addAddress; 
    private ImageView imgPhoto;
    private ContactDao contactDao;
    private ContactPhoneDao phoneDao;
    private ContactEmailDao emailDao;
    private ContactAddressDao addressDao;
    private ContactPhotoDao photoDao;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contact_detail);
        setTitle("Contact Detail");

        txtName = findViewById(R.id.txtName);
        txtCreatedAt = findViewById(R.id.txtCreatedAt);
        phonesContainer = findViewById(R.id.phonesContainer);
        emailContainer = findViewById(R.id.emailContainer);
        addressContainer = findViewById(R.id.addressContainer);
        imgPhoto = findViewById(R.id.imgPhoto);
        addPhone = findViewById(R.id.btnAddPhone);
        addEmail = findViewById(R.id.btnAddEmail);
        addAddress = findViewById(R.id.btnAddAddress);
        Long contactId = getIntent().getLongExtra("contact_id", -1);
        if (contactId == -1) {
            Toast.makeText(this, "Invalid contact ID", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        addPhone.setOnClickListener(v -> {
            showEditPhoneDialog(null, newPhone -> {
                newPhone.setContactId(contactId);
                phoneDao.insert(newPhone);
                loadAndDisplayPhones(contactId);
            });
        });
        addEmail.setOnClickListener(v -> {
            showEditEmailDialog(null, newEmail -> {
                newEmail.setContactId(contactId);
                emailDao.insert(newEmail);
                loadAndDisplayEmail(contactId);
            });
        });
        addAddress.setOnClickListener(v -> {
            showEditAddressDialog(null, newAddress -> {
                newAddress.setContactId(contactId);
                addressDao.insert(newAddress);
                loadAndDisplayAddresses(contactId);
            });
        });

        contactDao = new ContactDao(this);
        phoneDao = new ContactPhoneDao(this);
        emailDao = new ContactEmailDao(this);
        addressDao = new ContactAddressDao(this);
        photoDao = new ContactPhotoDao(this);

        Contact contact = contactDao.getById(contactId);
        if (contact == null) {
            Toast.makeText(this, "Contact not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        txtName.setText(contact.getName());
        txtCreatedAt.setText("Created: " + contact.getCreatedAt());

        txtName.setOnClickListener(v -> {
            final EditText input = new EditText(this);
            input.setText(txtName.getText().toString());
            input.setSelection(input.getText().length()); // move cursor to end

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Edit Contact Name")
                .setView(input)
                .setPositiveButton("Save", (dialog, which) -> {
                    String newName = input.getText().toString().trim();
                    if (!newName.isEmpty()) {
                        txtName.setText(newName);
                        contact.setName(newName);
                        contactDao.update(contact);
                    } else {
                        Toast.makeText(this, "Name cannot be empty", Toast.LENGTH_SHORT).show();
                    }
                })
            .setNegativeButton("Cancel", null)
                .show();
        });

        Button btnDelete = findViewById(R.id.btnDeleteContact);
        btnDelete.setOnClickListener(v -> {
            new AlertDialog.Builder(ContactDetailActivity.this)
                .setTitle("Delete Contact")
                .setMessage("Are you sure you want to delete this contact?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    // Delete from DB
                    contactDao.delete(contact.getId());


                    // Close detail and return to MainActivity
                    finish();
                })
            .setNegativeButton("Cancel", null)
                .show();
        });



        List<ContactPhone> phones = phoneDao.getByContactId(contactId);
        List<ContactEmail> emails = emailDao.getByContactId(contactId);
        List<ContactAddress> addresses = addressDao.getByContactId(contactId);
        ContactPhoto photo = photoDao.getByContactId(contactId);
        formatPhoneList(phones);
        formatEmailList(emails);
        formatAddressList(addresses);

        if (photo != null && photo.getPhoto() != null) {
            Bitmap bitmap = BitmapFactory.decodeByteArray(photo.getPhoto(), 0, photo.getPhoto().length);
            imgPhoto.setImageBitmap(bitmap);
        } else {
            imgPhoto.setImageResource(R.drawable.ic_launcher); // fallback image                                                           
        }
    }

    private void formatAddressList(List<ContactAddress> addresses) {

        for (ContactAddress address : addresses) {
            addressContainer.addView(createContactRow(
                        address,
                        () -> showEditAddressDialog(address, newAddress -> {
                            int row = addressDao.update(newAddress);

                            if (row >= 1) {
                                Toast.makeText(this, "Updated", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(this, "Update failed", Toast.LENGTH_SHORT).show();
                            }
                            // Run callback to update the UI
                            loadAndDisplayAddresses(address.getContactId());
                        }),
                        v -> {
                        },
                        () -> {
                            new AlertDialog.Builder(this)
                                .setTitle("Delete Address")
                                .setMessage("Are you sure you want to delete this Address?")
                                .setPositiveButton("Yes", (dialog, which) -> {
                                    if (addressDao.delete(address.getId()) >=1) {
                                        Toast.makeText(this, "Deleted", Toast.LENGTH_SHORT).show();
                                        loadAndDisplayAddresses(address.getContactId());
                                    } else {
                                        Toast.makeText(this, "Delete failed", Toast.LENGTH_SHORT).show();
                                    }
                                })
                            .setNegativeButton("No", null)
                                .show();
                        }
            ));
        }
    }
    private void formatEmailList(List<ContactEmail> emails) {

        for (ContactEmail email : emails) {
            emailContainer.addView(createContactRow(
                        email,
                        () -> showEditEmailDialog(email, newEmail -> {
                            int row = emailDao.update(newEmail);

                            if (row >= 1) {
                                Toast.makeText(this, "Updated", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(this, "Update failed", Toast.LENGTH_SHORT).show();
                            }
                            // Run callback to update the UI
                            loadAndDisplayEmail(email.getContactId());
                        }),
                        v -> {
                            Intent intent = new Intent(Intent.ACTION_SENDTO);
                            intent.setData(Uri.parse("mailto:" + email.getEmail()));
                            startActivity(intent);
                        },
                        () -> {
                            new AlertDialog.Builder(this)
                                .setTitle("Delete Email")
                                .setMessage("Are you sure you want to delete this Email?")
                                .setPositiveButton("Yes", (dialog, which) -> {
                                    if (emailDao.delete(email.getId()) >=1) {
                                        Toast.makeText(this, "Deleted", Toast.LENGTH_SHORT).show();
                                        loadAndDisplayEmail(email.getContactId());
                                    } else {
                                        Toast.makeText(this, "Delete failed", Toast.LENGTH_SHORT).show();
                                    }
                                })
                            .setNegativeButton("No", null)
                                .show();
                        }
            ));
        }
    }

    private void formatPhoneList(List<ContactPhone> phones) {

        for (ContactPhone phone : phones) {
            phonesContainer.addView(createContactRow(
                        phone,
                        () -> showEditPhoneDialog(phone, newPhone -> {
                            int row = phoneDao.update(newPhone);
                            if (row >= 1) {
                                Toast.makeText(this, "Updated", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(this, "Update failed", Toast.LENGTH_SHORT).show();
                            }
                            // Run callback to update the UI

                            loadAndDisplayPhones(phone.getContactId());
                        }),
                        v -> {
                            PopupMenu popup = new PopupMenu(this, v);
                            popup.getMenu().add("Whatsapp");
                            popup.getMenu().add("Message");
                            popup.getMenu().add("Call");
                            popup.setOnMenuItemClickListener(item -> {
                                String title = item.getTitle().toString();
                                if (title.equals("Message")) {
                                    Intent intent = new Intent(Intent.ACTION_VIEW);
                                    intent.setData(Uri.parse("sms:"+phone.getPhone())); // Replace with the phone number
                                    startActivity(intent);
                                } else if(title.equals("Call")) {
                                    Intent intent = new Intent(Intent.ACTION_DIAL);
                                    intent.setData(Uri.parse("tel:"+phone.getPhone())); // Replace with the phone number
                                    startActivity(intent);

                                } else if(title.equals("Whatsapp")) {
                                    String phoneNumber = normalizePhone(phone.getPhone()); // Indo number, no +
                                    Intent intent = new Intent(Intent.ACTION_VIEW);
                                    intent.setPackage("com.whatsapp");
                                    intent.setData(Uri.parse("https://wa.me/" + phoneNumber));
                                    if (intent.resolveActivity(getPackageManager()) != null) {
                                        startActivity(intent);
                                    } else {
                                        Toast.makeText(this, "WhatsApp not installed", Toast.LENGTH_SHORT).show();
                                    }

                                }
                                return true;
                            });
                            popup.show();
                        },
                        () -> {
                            new AlertDialog.Builder(this)
                                .setTitle("Delete Phone")
                                .setMessage("Are you sure you want to delete this phone number?")
                                .setPositiveButton("Yes", (dialog, which) -> {
                                    if (phoneDao.delete(phone.getId()) >=1) {
                                        Toast.makeText(this, "Deleted", Toast.LENGTH_SHORT).show();
                                        loadAndDisplayPhones(phone.getContactId());
                                    } else {
                                        Toast.makeText(this, "Delete failed", Toast.LENGTH_SHORT).show();
                                    }
                                })
                            .setNegativeButton("No", null)
                                .show();
                        }
            ));
        }
    }

    private String normalizePhone(String phone) {
        phone = phone.replaceAll("[^\\d+]", ""); // remove spaces, dashes, etc.

        if (phone.startsWith("0")) {
            return "62" + phone.substring(1); // local to intl (e.g., 08xx → 628xx)
        } else if (phone.startsWith("+")) {
            return phone.substring(1); // remove +
        } else {
            return phone; // assume already correct
        }
    }
    private View createContactRow(ContactDetail contactDetail, Runnable onEdit, Consumer<View> onProcess, Runnable onDelete ) {
        // Parent row layout
        LinearLayout rowLayout = new LinearLayout(this);
        rowLayout.setOrientation(LinearLayout.HORIZONTAL);
        rowLayout.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                    ));
        rowLayout.setPadding(0, 8, 0, 8);
        rowLayout.setGravity(Gravity.CENTER_VERTICAL);

        // Phone text
        TextView phoneTextView = new TextView(this);
        phoneTextView.setText(contactDetail.getLabel());
        phoneTextView.setTextSize(16);
        phoneTextView.setLayoutParams(new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
                    ));
        phoneTextView.setTextIsSelectable(true);
        // Button container
        LinearLayout buttonLayout = new LinearLayout(this);
        buttonLayout.setOrientation(LinearLayout.HORIZONTAL);

        // Style params for flat icon buttons
        int padding = 5;

        TextView editBtn = new TextView(this);
        editBtn.setText("✎");
        editBtn.setTextSize(18);
        editBtn.setPadding(padding, 0, padding, 0);
        editBtn.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                    ));
        editBtn.setOnClickListener(v -> onEdit.run());
        buttonLayout.addView(editBtn);
        boolean isPhone =contactDetail.getClass().equals(ContactPhone.class);
        boolean isEmail =contactDetail.getClass().equals(ContactEmail.class); 
        if(isPhone || isEmail) {
            TextView callBtn = new TextView(this);
            callBtn.setText(isPhone?"📞":"📧");
            callBtn.setTextSize(18);
            callBtn.setPadding(padding, 0, padding, 0);
            callBtn.setLayoutParams(new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                        ));
            callBtn.setOnClickListener(v -> {onProcess.accept(v);});
            buttonLayout.addView(callBtn);
        }
        TextView deleteBtn = new TextView(this);
        deleteBtn.setText("🗑");
        deleteBtn.setTextSize(18);
        deleteBtn.setPadding(padding, 0, padding, 0);
        deleteBtn.setTextColor(Color.RED);
        deleteBtn.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                    ));
        deleteBtn.setOnClickListener(v -> onDelete.run());

        // Add icons to button layout
        buttonLayout.addView(deleteBtn);

        // Add both views to row
        rowLayout.addView(phoneTextView);
        rowLayout.addView(buttonLayout);

        return rowLayout;

    }

    public void loadAndDisplayPhones(Long contactId) {
        phonesContainer.removeAllViews(); // clear current UI

        List<ContactPhone> phones = phoneDao.getByContactId(contactId); // your own method
        formatPhoneList(phones);
    }

    private void loadAndDisplayEmail(Long contactId) {
        emailContainer.removeAllViews(); // clear current UI

        List<ContactEmail> emails = emailDao.getByContactId(contactId); // your own method
        formatEmailList(emails);
    }

    private void loadAndDisplayAddresses(Long contactId) {
        addressContainer.removeAllViews(); // clear current UI

        List<ContactAddress> addresses = addressDao.getByContactId(contactId); // your own method
        formatAddressList(addresses);
    }

    private void showEditAddressDialog(ContactAddress contactAddress, Consumer<ContactAddress> onSaveCallback) {
        LayoutInflater inflater = LayoutInflater.from(this);
        View view = inflater.inflate(R.layout.dialog_edit_phone, null);
        EditText editPhone = view.findViewById(R.id.editPhone);
        EditText editType = view.findViewById(R.id.editType);
        editPhone.setInputType(InputType.TYPE_TEXT_VARIATION_POSTAL_ADDRESS);
        if(contactAddress != null) {
            editPhone.setText(contactAddress.getAddress());
            editType.setText(contactAddress.getType());
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(contactAddress != null? "Edit Address": "Add Address")
            .setView(view)
            .setPositiveButton("Save", (dialog, which) -> {
                // Update the object
                ContactAddress address = contactAddress == null? new ContactAddress(): contactAddress; 
                address.setAddress(editPhone.getText().toString().trim());
                address.setType(editType.getText().toString().trim());
                onSaveCallback.accept(address);
            })
        .setNegativeButton("Cancel", null)
            .show();
    }
    private void showEditEmailDialog(ContactEmail contactEmail, Consumer<ContactEmail> onSaveCallback) {
        LayoutInflater inflater = LayoutInflater.from(this);
        View view = inflater.inflate(R.layout.dialog_edit_phone, null);
        EditText editPhone = view.findViewById(R.id.editPhone);
        EditText editType = view.findViewById(R.id.editType);
        editPhone.setInputType(InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        if(contactEmail != null) {
            editPhone.setText(contactEmail.getEmail());
            editType.setText(contactEmail.getType());
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(contactEmail != null? "Edit Email": "Add Email")
            .setView(view)
            .setPositiveButton("Save", (dialog, which) -> {
                // Update the object
                ContactEmail email = contactEmail == null? new ContactEmail(): contactEmail; 
                email.setEmail(editPhone.getText().toString().trim());
                email.setType(editType.getText().toString().trim());
                onSaveCallback.accept(email);
            })
        .setNegativeButton("Cancel", null)
            .show();
    }

    private void showEditPhoneDialog(ContactPhone contactPhone, Consumer<ContactPhone> onSaveCallback) {
        LayoutInflater inflater = LayoutInflater.from(this);
        View view = inflater.inflate(R.layout.dialog_edit_phone, null);

        EditText editPhone = view.findViewById(R.id.editPhone);
        EditText editType = view.findViewById(R.id.editType);

        editPhone.setInputType(InputType.TYPE_CLASS_PHONE);
        // Pre-fill the fields
        if(contactPhone != null) {
            editPhone.setText(contactPhone.getPhone());
            editType.setText(contactPhone.getType());
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(contactPhone !=null ?"Edit Phone": "Add Phone")
            .setView(view)
            .setPositiveButton("Save", (dialog, which) -> {
                // Update the object
                ContactPhone phone = contactPhone == null? new ContactPhone(): contactPhone; 
                phone.setPhone(editPhone.getText().toString().trim());
                phone.setType(editType.getText().toString().trim());
                onSaveCallback.accept(phone);
            })
        .setNegativeButton("Cancel", null)
            .show();
    }

}
