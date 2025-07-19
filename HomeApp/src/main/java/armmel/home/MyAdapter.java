package armmel.home;

import android.content.Intent;
import android.content.pm.LauncherApps;
import android.content.pm.LauncherApps.ShortcutQuery;
import android.content.pm.ShortcutInfo;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Process;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.Toast;
import armmel.library.PagerAdapter;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class MyAdapter extends PagerAdapter {

  MainActivity context;
  LauncherApps launcherapps;

  public MyAdapter(MainActivity context, LauncherApps launcherapps) {
    this.context = context;
    this.launcherapps = launcherapps;
  }

  @Override
  public int getCount() {
    return (int) Math.ceil((double) MainActivity.mApplications.size() / context.appCount) + 1;
  }

  private ArrayList<ApplicationInfo> getArrayByPosition(int position) {
    ArrayList<ApplicationInfo> result = new ArrayList<ApplicationInfo>();
    int y = position * context.appCount;
    for (int i = 0; i < context.appCount; i++) {
      if (i + y < MainActivity.mApplications.size()) {
        result.add(MainActivity.mApplications.get(i + y));
      } else {
        break;
      }
    }
    return result;
  }

  @Override
  public int getItemPosition(Object object) {
    return POSITION_NONE;
    // return super.getItemPosition(object); //To change body of generated methods, choose Tools |
    // Templates.
  }

  @Override
  public Object instantiateItem(ViewGroup container, int position) {
    LayoutInflater inflater = LayoutInflater.from(context);
    if (position != 0) {
      ViewGroup layout = (ViewGroup) inflater.inflate(R.layout.main, null);
      container.addView(layout);
      GridLayout gridView = (GridLayout) layout.findViewById(R.id.gridView1);
      gridView.setAdapter(
          new ApplicationAdapter(
              context, getArrayByPosition(position - 1), (position - 1) * context.appCount));
      gridView.setOnItemDoubleClickListener(
          new DoubleClick.DoubleClickListener() {
            @Override
            public void onDoubleClick(AdapterView<?> parent, View v, int position) {
              ApplicationInfo app = (ApplicationInfo) parent.getItemAtPosition(position);
              if (app.isShowAll) {
                context.loadApplications(false, true);
              } else if (app.isReload) {
                Properties p = Utils.loadProperties(context, "armel.properties");
                p = Utils.cleanPropertiesHasValue(p, "Yes");
                Utils.writeProperties(context, p, "armel.properties", "showall reload");
                context.loadApplications(false, false);
              } else {
                openShortcut(app, v);
              }
              MyAdapter.this.notifyDataSetChanged();
            }

            public void onSingleClick(AdapterView<?> parent, View v, int position) {
              ApplicationInfo app = (ApplicationInfo) parent.getItemAtPosition(position);
              if (!app.isShortcut()) {
                context.startActivity(app.intent);
              } else {
                launcherapps.startShortcut(
                    app.getShortcutInfo().getPackage(),
                    app.getShortcutInfo().getId(),
                    null,
                    null,
                    Process.myUserHandle());
              }
            }
          });
      gridView.setOnItemLongClickListener(
          new AdapterView.OnItemLongClickListener() {
            public boolean onItemLongClick(
                AdapterView<?> parent, View view, int position, long id) {
              ApplicationInfo app = (ApplicationInfo) parent.getItemAtPosition(position);
              if (!app.isSystem) {
                if (!app.isShortcut()) {
                  Uri packUri = Uri.parse("package:" + app.intent.getComponent().getPackageName());
                  Intent uninstallIntent = new Intent(Intent.ACTION_DELETE, packUri);
                  context.startActivity(uninstallIntent);
                }
              } else {
                Log.i("home1", app.intent.getComponent().getClassName());
                Toast.makeText(context, app.title + " is system app.", Toast.LENGTH_LONG).show();
              }
              return true;
              // throw new UnsupportedOperationException("Not supported yet."); //To change body of
              // generated methods, choose Tools | Templates.
            }
          });
      gridView.setLongClickable(true);
      return layout;
    } else {
      return null;
    }
  }

  private void openShortcut(ApplicationInfo app, View anchorView) {
    if (!launcherapps.hasShortcutHostPermission() || app == null) return;

    ShortcutQuery sq = new LauncherApps.ShortcutQuery();
    sq.setQueryFlags(
        ShortcutQuery.FLAG_MATCH_DYNAMIC
            | ShortcutQuery.FLAG_MATCH_MANIFEST
            | ShortcutQuery.FLAG_MATCH_PINNED);
    sq.setPackage(app.getPackageName());

    List<ShortcutInfo> shortcuts = launcherapps.getShortcuts(sq, Process.myUserHandle());
    if (shortcuts == null || shortcuts.isEmpty()) {
      Toast.makeText(context, "No shortcuts found", Toast.LENGTH_SHORT).show();
      return;
    }

    ListView listView = new ListView(context);
    ShortcutAdapter adapter = new ShortcutAdapter(context, shortcuts, launcherapps);
    listView.setAdapter(adapter);

    // Fixed width (in pixels)
    int width = (int) (200 * context.getResources().getDisplayMetrics().density);

    final PopupWindow popup =
        new PopupWindow(listView, width, WindowManager.LayoutParams.WRAP_CONTENT, true);
    popup.setOutsideTouchable(true);
    popup.setFocusable(true);
    popup.setBackgroundDrawable(
        new ColorDrawable(Color.WHITE)); // Required to dismiss on outside touch

    listView.setOnItemClickListener(
        (parent, view, position, id) -> {
          ShortcutInfo selected = shortcuts.get(position);
          launcherapps.startShortcut(
              selected.getPackage(), selected.getId(), null, null, Process.myUserHandle());
          popup.dismiss();
        });

    popup.showAsDropDown(anchorView);
  }

  @Override
  public void destroyItem(ViewGroup container, int position, Object obj) {
    container.removeView((View) obj);
  }

  @Override
  public boolean isViewFromObject(View container, Object obj) {
    return container == obj;
  }
}
