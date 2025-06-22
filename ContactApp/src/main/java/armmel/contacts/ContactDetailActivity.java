package armmel.contacts;
import armmel.contacts.utils.ThemeUtils;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.ImageView;
import android.widget.Toast;
import armmel.contacts.utils.Utils;
import java.util.ArrayList;
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
    public static final String[] PHONE_TYPES = {
        "Mobile",
        "Home",
        "Work",
        "Main",
        "Work Fax",
        "Home Fax",
        "Pager",
        "Other"
    };
    public static final String[] EMAIL_TYPES = {
        "Home",       // TYPE_HOME
        "Work",       // TYPE_WORK
        "Other",      // TYPE_OTHER
        "Mobile"      // TYPE_MOBILE
    };
    public static final String[] ADDRESS_TYPES = {
        "Home",       // TYPE_HOME
        "Work",       // TYPE_WORK
        "Other"       // TYPE_OTHER
    };
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeUtils.applyTheme(this);
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
            showEditPhoneDialog(PHONE_TYPES, null, newPhone -> {
                newPhone.setPhone(Utils.removeForPhone(newPhone.getPhone()));
                newPhone.setContactId(contactId);
                phoneDao.insert(newPhone);
                loadAndDisplay(phonesContainer, phoneDao.getByContactId(contactId),this::formatPhoneList );
            });
        });
        addEmail.setOnClickListener(v -> {
            showEditEmailDialog(EMAIL_TYPES, null, newEmail -> {
                newEmail.setContactId(contactId);
                emailDao.insert(newEmail);
                loadAndDisplay(emailContainer, emailDao.getByContactId(contactId),this::formatEmailList );
            });
        });
        addAddress.setOnClickListener(v -> {
            showEditAddressDialog(ADDRESS_TYPES, null, newAddress -> {
                newAddress.setContactId(contactId);
                addressDao.insert(newAddress);
                loadAndDisplay(addressContainer, addressDao.getByContactId(contactId),this::formatAddressList );
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
                        () -> showEditAddressDialog( ADDRESS_TYPES, address, newAddress -> {
                            int row = addressDao.update(newAddress);

                            if (row >= 1) {
                                Toast.makeText(this, "Updated", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(this, "Update failed", Toast.LENGTH_SHORT).show();
                            }
                            // Run callback to update the UI
                            loadAndDisplay(addressContainer, addressDao.getByContactId(address.getContactId()),this::formatAddressList );
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
                                        loadAndDisplay(addressContainer, addressDao.getByContactId(address.getContactId()),this::formatAddressList );
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
                        () -> showEditEmailDialog(EMAIL_TYPES, email, newEmail -> {
                            int row = emailDao.update(newEmail);

                            if (row >= 1) {
                                Toast.makeText(this, "Updated", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(this, "Update failed", Toast.LENGTH_SHORT).show();
                            }
                            // Run callback to update the UI
                            loadAndDisplay(emailContainer, emailDao.getByContactId(email.getContactId()),this::formatEmailList );
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
                                        loadAndDisplay(emailContainer, emailDao.getByContactId(email.getContactId()),this::formatEmailList );
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
                        () -> showEditPhoneDialog(PHONE_TYPES,phone, newPhone -> {
                            newPhone.setPhone(Utils.removeForPhone(newPhone.getPhone()));
                            int row = phoneDao.update(newPhone);
                            if (row >= 1) {
                                Toast.makeText(this, "Updated", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(this, "Update failed", Toast.LENGTH_SHORT).show();
                            }
                            // Run callback to update the UI

                            loadAndDisplay(phonesContainer, phoneDao.getByContactId(phone.getContactId()),this::formatPhoneList );
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
                                        loadAndDisplay(phonesContainer, phoneDao.getByContactId(phone.getContactId()),this::formatPhoneList );
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

        buttonLayout.addView(deleteBtn);

        rowLayout.addView(phoneTextView);
        rowLayout.addView(buttonLayout);

        return rowLayout;

    }
    private <T> void loadAndDisplay(ViewGroup container, List<T> dataList, Consumer<List<T>> formatter) {
        container.removeAllViews();
        formatter.accept(dataList);
    }


    private void showEditAddressDialog(String[] addressType, ContactAddress contactAddress, Consumer<ContactAddress> onSaveCallback) {
        LayoutInflater inflater = LayoutInflater.from(this);
        View view = inflater.inflate(R.layout.dialog_edit_phone, null);
        EditText editPhone = view.findViewById(R.id.editPhone);
        editPhone.setInputType(InputType.TYPE_TEXT_VARIATION_POSTAL_ADDRESS);
        Spinner spinnerType = view.findViewById(R.id.spinnerType);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, // use getContext() if in fragment
                android.R.layout.simple_spinner_item,
                addressType
                );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerType.setAdapter(adapter);
        if(contactAddress != null) {
            editPhone.setText(contactAddress.getAddress());
            setSpinnerToValue(spinnerType, contactAddress.getType());
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(contactAddress != null? "Edit Address": "Add Address")
            .setView(view)
            .setPositiveButton("Save", (dialog, which) -> {
                ContactAddress address = contactAddress == null? new ContactAddress(): contactAddress; 
                address.setAddress(editPhone.getText().toString().trim());
                String selectedLabel = spinnerType.getSelectedItem().toString();
                address.setType(selectedLabel);
                onSaveCallback.accept(address);
            })
        .setNegativeButton("Cancel", null)
            .show();
    }


    private void showEditEmailDialog(String[] emailTypes, ContactEmail contactEmail, Consumer<ContactEmail> onSaveCallback) {
        LayoutInflater inflater = LayoutInflater.from(this);
        View view = inflater.inflate(R.layout.dialog_edit_phone, null);
        EditText editPhone = view.findViewById(R.id.editPhone);
        editPhone.setInputType(InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        Spinner spinnerType = view.findViewById(R.id.spinnerType);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, // use getContext() if in fragment
                android.R.layout.simple_spinner_item,
                emailTypes
                );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerType.setAdapter(adapter);
        if(contactEmail != null) {
            editPhone.setText(contactEmail.getEmail());
            setSpinnerToValue(spinnerType, contactEmail.getType());
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(contactEmail != null? "Edit Email": "Add Email")
            .setView(view)
            .setPositiveButton("Save", (dialog, which) -> {
                ContactEmail email = contactEmail == null? new ContactEmail(): contactEmail; 
                email.setEmail(editPhone.getText().toString().trim());
                String selectedLabel = spinnerType.getSelectedItem().toString();
                email.setType(selectedLabel);
                onSaveCallback.accept(email);
            })
        .setNegativeButton("Cancel", null)
            .show();
    }

    private void showEditPhoneDialog(String[]  phoneTypes, ContactPhone contactPhone, Consumer<ContactPhone> onSaveCallback) {
        LayoutInflater inflater = LayoutInflater.from(this);
        View view = inflater.inflate(R.layout.dialog_edit_phone, null);

        EditText editPhone = view.findViewById(R.id.editPhone);
        editPhone.setInputType(InputType.TYPE_CLASS_PHONE);
        Spinner spinnerType = view.findViewById(R.id.spinnerType);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, // use getContext() if in fragment
                android.R.layout.simple_spinner_item,
                phoneTypes
                );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerType.setAdapter(adapter);

        if(contactPhone != null) {
            editPhone.setText(contactPhone.getPhone());
            setSpinnerToValue(spinnerType, mapFromVcfType(contactPhone.getType()));
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(contactPhone !=null ?"Edit Phone": "Add Phone")
            .setView(view)
            .setPositiveButton("Save", (dialog, which) -> {
                ContactPhone phone = contactPhone == null? new ContactPhone(): contactPhone; 
                phone.setPhone(editPhone.getText().toString().trim());
                String selectedLabel = spinnerType.getSelectedItem().toString();
                String vcfType = mapToVcfType(selectedLabel);
                phone.setType(vcfType);
                onSaveCallback.accept(phone);
            })
        .setNegativeButton("Cancel", null)
            .show();
    }
    void setSpinnerToValue(Spinner spinner, String label) {
        ArrayAdapter adapter = (ArrayAdapter) spinner.getAdapter();
        for (int i = 0; i < adapter.getCount(); i++) {
            if (adapter.getItem(i).toString().equalsIgnoreCase(label)) {
                spinner.setSelection(i);
                break;
            }
        }
    }
    String mapFromVcfType(String vcfType) {
        switch (vcfType.toUpperCase()) {
            case "CELL":
                return "Mobile";
            case "HOME":
                return "Home";
            case "WORK":
                return "Work";
            case "MAIN":
                return "Main";
            case "WORK,FAX":
                return "Work Fax";
            case "HOME,FAX":
                return "Home Fax";
            case "PAGER":
                return "Pager";
            case "OTHER":
                return "Other";
            default:
                return "Mobile"; // fallback
        }
    }

    String mapToVcfType(String label) {
        switch (label) {
            case "Mobile":
                return "CELL";
            case "Home":
                return "HOME";
            case "Work":
                return "WORK";
            case "Main":
                return "MAIN";
            case "Work Fax":
                return "WORK,FAX";
            case "Home Fax":
                return "HOME,FAX";
            case "Pager":
                return "PAGER";
            case "Other":
                return "OTHER";
            default:
                return "VOICE";
        }
    }

}
