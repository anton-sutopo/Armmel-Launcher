package armmel.contacts;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.Toast;
import armmel.contacts.database.entity.Contact;
import armmel.contacts.database.repository.ContactRepository;
import armmel.contacts.dto.Search;
import armmel.contacts.dto.SearchService;
import armmel.contacts.utils.ThemeUtils;
import armmel.contacts.utils.Utils;
import armmel.contacts.utils.VcfExporter;
import armmel.contacts.utils.VcfParser;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.LinkedList;
import java.util.List;

// Demo do see how to use content provider
public class MainActivity extends Activity {
  private static final int REQUEST_OPEN_VCF = 1000;
  private static final int CREATE_VCF_FILE_REQUEST_CODE = 1001;
  private static final String TAG = "MainActivity";
  private ListView contactNames;
  ImageButton fab = null;
  private ContactAdapter contactAdapter;
  private SearchService searchService;
  private ContactRepository contactDao;
  private String query = "";

  @Override
  public void onResume() {
    super.onResume();
    contactAdapter.updateData(query); // Re-fetch current data
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    ThemeUtils.applyTheme(this);
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    contactDao = new ContactRepository(this);
    contactNames = (ListView) findViewById(R.id.contact_names);
    searchService = new SearchService(this);
    List<Search> contacts = new LinkedList<>();
    contactAdapter = new ContactAdapter(this, contacts);
    contactNames.setAdapter(contactAdapter);
    contactNames.setOnItemClickListener(
        (parent, view, position, id) -> {
          Search selectedContact = (Search) contactAdapter.getItem(position);
          Intent intent = new Intent(MainActivity.this, ContactDetailActivity.class);
          intent.putExtra("contact_id", selectedContact.getContactId()); // Pass contact ID
          startActivity(intent);
        });

    fab = (ImageButton) findViewById(R.id.fab);
    fab.setOnClickListener(
        new View.OnClickListener() {
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
    if (requestCode == REQUEST_OPEN_VCF && resultCode == RESULT_OK) {
      Uri uri = data.getData();
      if (uri != null) {
        try {
          InputStream inputStream = getContentResolver().openInputStream(uri);
          List<Contact> contacts = VcfParser.parse(inputStream);
          ContactRepository cd = new ContactRepository(this);
          for (Contact ct : contacts) {
            cd.insert(ct);
          }
          contactAdapter.updateData(""); // Re-fetch current data
        } catch (Exception e) {
        }
      }
    } else if (requestCode == CREATE_VCF_FILE_REQUEST_CODE
        && resultCode == RESULT_OK
        && data != null) {
      Uri uri = data.getData(); // user-chosen URI

      try (OutputStream out = getContentResolver().openOutputStream(uri)) {
        // export to memory, not file
        VcfExporter exporter = new VcfExporter(this);
        String vcfContent = exporter.exportToVcf(VcfExporter.VCardVersion.VCARD_30);

        out.write(vcfContent.getBytes());
        out.flush();
        Toast.makeText(this, "Exported to: " + uri.getLastPathSegment(), Toast.LENGTH_LONG).show();
      } catch (IOException e) {
        e.printStackTrace();
        Toast.makeText(this, "Failed to save vCard", Toast.LENGTH_SHORT).show();
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
    searchView.setOnQueryTextListener(
        new SearchView.OnQueryTextListener() {

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
    contactAdapter.updateData(keyword);
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
      startActivityForResult(Intent.createChooser(intent, "Select a file"), REQUEST_OPEN_VCF);

      return true;
    } else if (id == R.id.action_delete_all) {
      contactDao.deleteAll();
      contactAdapter.updateData(""); // Re-fetch current data
    } else if (id == R.id.action_export) {
      Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
      intent.addCategory(Intent.CATEGORY_OPENABLE);
      intent.setType("text/x-vcard");
      intent.putExtra(Intent.EXTRA_TITLE, "contacts.vcf"); // default name
      startActivityForResult(intent, CREATE_VCF_FILE_REQUEST_CODE);
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
        .setPositiveButton(
            "Save",
            (dialog, which) -> {
              String name = editName.getText().toString().trim();
              if (!Utils.isEmpty(name)) {

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
