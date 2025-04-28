package armmel.home;
import armmel.library.PagerAdapter;
import android.content.pm.LauncherApps;
import android.content.pm.LauncherApps.ShortcutQuery;
import android.os.Process;
import android.content.pm.ShortcutInfo;
import android.graphics.drawable.Drawable;
import java.util.ArrayList;
import android.view.ViewGroup;
import android.view.View;
import android.view.LayoutInflater;
import android.widget.AdapterView;
import java.util.Properties;
import java.util.List;
import android.net.Uri;
import android.content.Intent;
import android.widget.Toast;
import android.util.Log;
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
        //return super.getItemPosition(object); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Object instantiateItem(ViewGroup container, int position) {
        LayoutInflater inflater = LayoutInflater.from(context);
        if (position != 0) {
            ViewGroup layout = (ViewGroup) inflater.inflate(R.layout.main, null);
            container.addView(layout);
            GridLayout gridView = (GridLayout) layout.findViewById(R.id.gridView1);
            gridView.setAdapter(new ApplicationAdapter(context, getArrayByPosition(position - 1), (position -1)* context.appCount));
            gridView.setOnItemDoubleClickListener(new DoubleClick.DoubleClickListener() {
                @Override
                public void onDoubleClick(AdapterView<?> parent, View v, int position) {
                    ApplicationInfo app = (ApplicationInfo) parent.getItemAtPosition(position);
                    if(app.isShowAll) {
                        context.loadApplications(false,true);
                    } else if(app.isReload) {
                        Properties p = Utils.loadProperties(context,"armel.properties"); 
                        p = Utils.cleanPropertiesHasValue(p,"Yes"); 
                        Utils.writeProperties(context,p,"armel.properties", "showall reload");
                        context.loadApplications(false,false);
                    } else {
                        openShortcut(app);
                    }
                    MyAdapter.this.notifyDataSetChanged();

                }
                public void onSingleClick(AdapterView<?> parent, View v, int position) {
                    ApplicationInfo app = (ApplicationInfo) parent.getItemAtPosition(position);
                    if(!app.isShortcut()) {
                        context.startActivity(app.intent);
                    } else {
                        launcherapps.startShortcut(app.getShortcutInfo().getPackage(), app.getShortcutInfo().getId(),null,null,Process.myUserHandle());
                    }
                }

            });
            gridView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
                public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                    ApplicationInfo app = (ApplicationInfo) parent.getItemAtPosition(position);
                    if (!app.isSystem) {
                        if(!app.isShortcut()) {
                            Uri packUri = Uri.parse("package:" + app.intent.getComponent().getPackageName());
                            Intent uninstallIntent = new Intent(Intent.ACTION_DELETE, packUri);
                            context.startActivity(uninstallIntent);
                        }
                    } else {
                        Log.i("home1", app.intent.getComponent().getClassName());
                        Toast.makeText(context, app.title + " is system app.", Toast.LENGTH_LONG).show();
                    }
                    return true;
                    //throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
                }
            });
            gridView.setLongClickable(true);
            return layout;
        } else {
            return null;
        }
    }
    private void openShortcut(ApplicationInfo application) {
        if(launcherapps.hasShortcutHostPermission() && application != null) {
            IconPack ipack = new IconPack();
            ShortcutQuery sq = new LauncherApps.ShortcutQuery();
            sq.setQueryFlags(ShortcutQuery.FLAG_MATCH_DYNAMIC + ShortcutQuery.FLAG_MATCH_MANIFEST +  ShortcutQuery.FLAG_MATCH_PINNED);
            sq.setPackage(application.getPackageName());
            List<ShortcutInfo> si = launcherapps.getShortcuts(sq, Process.myUserHandle());
            for(ShortcutInfo sia: si) {
                ApplicationInfo applicationChild = new ApplicationInfo(context);
                applicationChild.setIsShortcut(true);
                applicationChild.setShortcutInfo(sia);
                applicationChild.title = sia.getShortLabel().toString();
                Drawable d = launcherapps.getShortcutIconDrawable(sia, context.getResources().getDisplayMetrics().densityDpi); 
                if(d != null)
                    applicationChild.icon = ipack.getIcon(context,context.getResources(),sia.getPackage(),sia.getPackage(),d);
            }
        }
        Toast.makeText(context,"open ShortcutList",Toast.LENGTH_LONG).show();
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

