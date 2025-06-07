package armmel.contacts;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import armmel.contacts.database.entity.Contact;
import java.util.List;

public class ContactAdapter extends BaseAdapter {
    private final Context context;
    private final List<Contact> contacts;

    public ContactAdapter(Context context, List<Contact> contacts) {
        this.context = context;
        this.contacts = contacts;
    }

    @Override
    public int getCount() { return contacts.size(); }

    @Override
    public Object getItem(int position) { return contacts.get(position); }

    @Override
    public long getItemId(int position) { return contacts.get(position).getId(); }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        Contact contact = contacts.get(position);

        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.contact_detail, parent, false);
        }

        TextView txtName = convertView.findViewById(R.id.name);

        txtName.setText(contact.getName());

        // Optionally load photo_uri if used
        // Example: use Glide or BitmapFactory for real image loading

        return convertView;
    }
    public void updateData(List<Contact> newList) {
        contacts.clear();
        contacts.addAll(newList);
        notifyDataSetChanged();
    }
}

