package armmel.home;

import android.content.Context;
import android.content.pm.LauncherApps;
import android.content.pm.ShortcutInfo;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import java.util.List;

public class ShortcutAdapter extends BaseAdapter {

  private final Context context;
  private final List<ShortcutInfo> shortcutList;
  private final LauncherApps launcherApps;

  public ShortcutAdapter(Context context, List<ShortcutInfo> shortcuts, LauncherApps launcherApps) {
    this.context = context;
    this.shortcutList = shortcuts;
    this.launcherApps = launcherApps;
  }

  @Override
  public int getCount() {
    return shortcutList.size();
  }

  @Override
  public Object getItem(int position) {
    return shortcutList.get(position);
  }

  @Override
  public long getItemId(int position) {
    return position;
  }

  static class ViewHolder {
    ImageView icon;
    TextView label;
  }

  @Override
  public View getView(int position, View convertView, ViewGroup parent) {
    ViewHolder vh;
    if (convertView == null) {
      convertView =
          LayoutInflater.from(context).inflate(R.layout.item_shortcut_popup, parent, false);
      vh = new ViewHolder();
      vh.icon = convertView.findViewById(R.id.icon);
      vh.label = convertView.findViewById(R.id.label);
      convertView.setTag(vh);
    } else {
      vh = (ViewHolder) convertView.getTag();
    }

    ShortcutInfo shortcut = shortcutList.get(position);
    vh.label.setText(shortcut.getShortLabel());

    Drawable icon =
        launcherApps.getShortcutIconDrawable(
            shortcut, context.getResources().getDisplayMetrics().densityDpi);

    if (icon != null) {
      vh.icon.setImageDrawable(icon);
    }

    return convertView;
  }
}
