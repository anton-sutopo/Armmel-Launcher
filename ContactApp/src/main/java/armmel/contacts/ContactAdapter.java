package armmel.contacts;

import android.content.Context;
import android.graphics.Color;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import armmel.contacts.dto.Search;
import armmel.contacts.dto.SearchService;
import armmel.contacts.utils.Utils;
import java.util.List;

public class ContactAdapter extends BaseAdapter {
  private final Context context;
  private final List<Search> contacts;
  private final SearchService searchService;
  private String keyword;

  public ContactAdapter(Context context, List<Search> contacts) {
    this.context = context;
    this.contacts = contacts;
    this.searchService = new SearchService(context);
  }

  @Override
  public int getCount() {
    return contacts.size();
  }

  @Override
  public Object getItem(int position) {
    return contacts.get(position);
  }

  @Override
  public long getItemId(int position) {
    return contacts.get(position).getContactId();
  }

  @Override
  public View getView(int position, View convertView, ViewGroup parent) {
    Search contact = contacts.get(position);

    if (convertView == null) {
      convertView = LayoutInflater.from(context).inflate(R.layout.contact_detail, parent, false);
    }

    TextView txtName = convertView.findViewById(R.id.name);
    if (Utils.isEmpty(keyword)) {
      txtName.setText(contact.getShow());
    } else {
      txtName.setText(coloring(contact.getShow(), keyword));
    }
    // Optionally load photo_uri if used
    // Example: use Glide or BitmapFactory for real image loading

    return convertView;
  }

  private SpannableString coloring(String fullText, String partial) {
    SpannableString string = new SpannableString(fullText);
    fullText = fullText.toLowerCase();
    partial = partial.toLowerCase();
    int index = fullText.indexOf(partial);
    while (index >= 0) {
      int end = index + partial.length();
      string.setSpan(
          new ForegroundColorSpan(Color.RED), index, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

      index = fullText.indexOf(partial, end); // find next
    }

    return string;
  }

  public void updateData(String keyword) {
    List<Search> filtered = searchService.getAllFiltered(keyword);
    this.keyword = keyword.startsWith("@") ? keyword.substring(1) : keyword;
    this.keyword =
        Utils.isPartialPhoneNumber(this.keyword)
            ? Utils.removeForPhone(this.keyword)
            : this.keyword;
    contacts.clear();
    contacts.addAll(filtered);
    notifyDataSetChanged();
  }
}
