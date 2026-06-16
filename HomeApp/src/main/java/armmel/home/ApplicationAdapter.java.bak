package armmel.home;

import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import java.util.ArrayList;

public class ApplicationAdapter extends ArrayAdapter<ApplicationInfo> {
  private final MainActivity context;
  private final ArrayList<ApplicationInfo> mobileValues;
  public final int startPos;

  private static class ViewHolder {
    CustomImageView iv;
    OutlineTextView textView;
  }

  public ApplicationAdapter(MainActivity context, ArrayList<ApplicationInfo> apps, int startPos) {
    super(context, 0, apps);
    this.context = context;
    this.mobileValues = apps;
    this.startPos = startPos;
  }

  //	public ApplicationAdapter(Context context, String[] mobileValues) {
  //		this.context = context;
  //		this.mobileValues = mobileValues;
  //	}
  @Override
  public View getView(int position, View convertView, ViewGroup parent) {

    final ApplicationInfo info = mobileValues.get(position);

    ViewHolder holder;
    if (convertView == null) {
      final LayoutInflater inflater = LayoutInflater.from(context);
      convertView = inflater.inflate(R.layout.application, parent, false);
      convertView.setLayoutParams(
          new ViewGroup.LayoutParams(
              ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
      holder = new ViewHolder();
      holder.iv = (CustomImageView) convertView.findViewById(R.id.icon_image);
      holder.textView = (OutlineTextView) convertView.findViewById(R.id.icon_text);
      convertView.setTag(holder);
    } else {
      holder = (ViewHolder) convertView.getTag();
    }

    Bitmap icon = info.icon;
    int userHash = info.getUserHandle() != null ? info.getUserHandle().hashCode() : 0;
    holder.iv.setUserHash(userHash);
    if (info.isCalendar()) {
      holder.iv.setImageResource(R.drawable.ios);
      context.calendar = holder.iv;
      holder.iv.setCalendar(true);
    } else if (info.isClock()) {
      holder.iv.setImageResource(R.drawable.clock);
      context.clock = holder.iv;
      holder.iv.setClock(true);
    } else {
      holder.iv.setImage(icon);
    }

    holder.textView.setText(info.title);
    return convertView;
  }

  @Override
  public int getCount() {
    return mobileValues.size();
  }
}
