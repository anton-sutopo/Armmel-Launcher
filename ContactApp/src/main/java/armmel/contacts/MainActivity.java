package armmel.contacts;

import armmel.contacts.utils.ThemeUtils;
import android.Manifest;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.Settings;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;
import android.app.Activity;
import java.util.ArrayList;
import java.util.List;
import java.util.MissingFormatArgumentException;
import android.widget.ImageButton;
import android.app.ActionBar;
import android.widget.Toast;
import java.io.IOException;
import java.io.InputStream;
import armmel.contacts.utils.VcfParser;
import armmel.contacts.database.entity.Contact;
import armmel.contacts.database.dao.ContactDao;
import java.util.LinkedList;
import android.view.LayoutInflater;
import android.app.AlertDialog;
import android.widget.EditText;
import android.widget.SearchView;

// Demo do see how to use content provider
public class MainActivity extends Activity {

    private static final String TAG = "MainActivity";
    private ListView contactNames;
    ImageButton fab = null;
    private ContactAdapter contactAdapter;
    private ContactDao contactDao;
    private String query="";

    @Override
    public void onResume() {
        super.onResume();
        contactAdapter.updateData(contactDao.getAllFiltered(query)); // Re-fetch current data
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeUtils.applyTheme(this);
        super.onCreate(savedInstanceState); 
        setContentView(R.layout.activity_main);

        contactNames = (ListView) findViewById(R.id.contact_names);
        contactDao = new ContactDao(this);
        List<Contact> contacts = new LinkedList<>();
        contactAdapter = new ContactAdapter(this, contacts);
        contactNames.setAdapter(contactAdapter);
        contactNames.setOnItemClickListener((parent, view, position, id) -> {
            Contact selectedContact = (Contact) contactAdapter.getItem(position);
            Intent intent = new Intent(MainActivity.this, ContactDetailActivity.class);
            intent.putExtra("contact_id", selectedContact.getId()); // Pass contact ID
            startActivity(intent); 
        }); 

        fab = (ImageButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "onClick: starts");
                showAddContactDialog();
            }
        });
        Log.d(TAG, "onCreate: ends");
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1 && resultCode == RESULT_OK) {
            Uri uri = data.getData();
            if (uri != null) {
                try {
                    InputStream inputStream = getContentResolver().openInputStream(uri);
                    List<Contact> contacts = VcfParser.parse(inputStream);
                    ContactDao cd = new ContactDao(this);
                    for(Contact ct: contacts) {
                        cd.insert(ct);
                    }
                    contactAdapter.updateData(contactDao.getAllFiltered("")); // Re-fetch current data
                } catch(Exception e) {
                }
            }
        }
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        MenuItem searchItem = menu.findItem(R.id.action_search);
        SearchView searchView = (SearchView) searchItem.getActionView();
        searchView.setQueryHint("searching...");
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {

            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }
            @Override
            public boolean onQueryTextChange(String newText) {
                query = newText;
                // Filter your list here
                filterContacts(newText);
                return true;
            }
        });
        return true;
    }
    private void filterContacts(String keyword) {
        List<Contact> filtered = contactDao.getAllFiltered(keyword);
        contactAdapter.updateData(filtered);
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_import) {
            Toast.makeText(this, "Import", Toast.LENGTH_LONG).show();
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("*/*"); // All file types
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            startActivityForResult(Intent.createChooser(intent, "Select a file"), 1);

            return true;
        } else if(id == R.id.action_delete_all) {
            contactDao.deleteAll();
            contactAdapter.updateData(contactDao.getAllFiltered("")); // Re-fetch current data
        }

        return super.onOptionsItemSelected(item);
    }

    private void showAddContactDialog() {
        // Inflate custom layout with EditText
        LayoutInflater inflater = LayoutInflater.from(this);
        View dialogView = inflater.inflate(R.layout.dialog_add_contact, null);
        EditText editName = dialogView.findViewById(R.id.editContactName);

        new AlertDialog.Builder(this)
            .setTitle("Add New Contact")
            .setView(dialogView)
            .setPositiveButton("Save", (dialog, which) -> {
                String name = editName.getText().toString().trim();
                if (!name.isEmpty()) {

                    long contactId = contactDao.insertContact(name);
                    openDetailActivity(contactId);
                } else {
                    Toast.makeText(this, "Name cannot be empty", Toast.LENGTH_SHORT).show();
                }
            })
        .setNegativeButton("Cancel", null)
            .show();
    }
    private void openDetailActivity(long contactId) {
        Intent intent = new Intent(MainActivity.this, ContactDetailActivity.class);
        intent.putExtra("contact_id", contactId);
        startActivity(intent);
    }
}
