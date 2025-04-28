package armmel.home;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import armmel.library.PagerAdapter;
import android.Manifest;
import java.util.Properties;
import android.graphics.Bitmap;
import java.util.Locale;
import android.content.pm.LauncherApps;
import android.content.pm.LauncherApps.ShortcutQuery;
import android.os.Process;
import android.content.pm.ShortcutInfo;
import android.graphics.drawable.Drawable;
public class MainActivity extends Activity {
    // public static int width;

    public static int height;
    public static final String CLEAR = "arm.home.clear";
    public static final String ADD = "arm.home.add";
    // private static boolean mWallpaperChecked;
    protected static ArrayList<ApplicationInfo> mApplications;
    private static final String LOG_TAG = "Home";
    ViewPagerD view = null;
    protected final int appCount = 6 * 5;
    MyAdapter myAdapter = null;
    private BroadcastReceiver br;
    private BroadcastReceiver tm;
    //    private BroadcastReceiver notif;
    CustomImageView clock;
    CustomImageView calendar;
    //    ConcurrentHashMap<String, Maps> mapping;
    public final int MY_PERMISSIONS_REQUEST_READ_MEDIA = 01;
    public MainActivity() {
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        view = (ViewPagerD) findViewById(R.id.viewPager);
        processPermission();
        br = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                loadApplications(false);
                myAdapter.notifyDataSetChanged();
            }
        };
        tm = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                //Toast.makeText(MainActivity.this, "ticked", Toast.LENGTH_LONG).show();
                /*if (clock != null) {
                  clock.setClock(true);
                  clock.invalidate();
                  }*/
                if (calendar != null) {
                    calendar.setCalendar(true);
                    calendar.invalidate();
                }
            }
        };
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Intent.ACTION_PACKAGE_ADDED);
        intentFilter.addAction(Intent.ACTION_PACKAGE_REMOVED);
        intentFilter.addAction(Intent.ACTION_PACKAGE_CHANGED);
        intentFilter.addDataScheme("package");
        registerReceiver(br, intentFilter);
        IntentFilter timeFilter = new IntentFilter();
        timeFilter.addAction(Intent.ACTION_TIME_TICK);
        registerReceiver(tm, timeFilter);
    }
    private boolean processPermission() {
        int permissionCheck = this.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
            loadApplications(true);
        } else {
            this.requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},MY_PERMISSIONS_REQUEST_READ_MEDIA);
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int resultCode, String[] permissions, int[] grantResults) {
        if(resultCode == MY_PERMISSIONS_REQUEST_READ_MEDIA) {
            if(grantResults.length > 0) {
                loadApplications(true);
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume(); //To change body of generated methods, choose Tools | Templates. 
        loadApplications(true);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus); //To change body of generated methods, choose Tools | Templates.

        height = view.getHeight() - view.getTop();
        if(myAdapter == null) {
            loadApplications(false);
        }
        view.setAdapter(myAdapter);
        myAdapter.notifyDataSetChanged();
        // setDefaultWallpaper();

    }
    protected void loadApplications(boolean isLaunching) {
        loadApplications(isLaunching, false);
    }
    private String stripComponent(String componentName) {
        int start = componentName.indexOf("{")+1;
        int end = componentName.indexOf("}",  start);
        if (end > start)
        {
            return componentName.substring(start,end).toLowerCase(Locale.getDefault()).replace("/", "_");
        }
        return "";
    }
    /*private void loadApplications(boolean isLaunching, boolean showNo) {
      if(isLaunching && mApplications != null) {
      return; 
      }
      PackageManager manager = getPackageManager();
      List<android.content.pm.ApplicationInfo> packages = manager.getInstalledApplications(PackageManager.GET_META_DATA);
      for (android.content.pm.ApplicationInfo app : packages) {
      Log.i(LOG_TAG, "packages: "+ app.packageName);
      }
      }*/
    protected void loadApplications(boolean isLaunching, boolean showNo) {
        if (isLaunching && mApplications != null) {
            return;
        }
        if(myAdapter == null) {
            LauncherApps launcherApps = (LauncherApps) getSystemService(Context.LAUNCHER_APPS_SERVICE);
            myAdapter = new MyAdapter(this, launcherApps); 
        }
        PackageManager manager = getPackageManager();
        Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> apps = manager.queryIntentActivities(mainIntent, 0);
        Properties p = Utils.loadProperties(this,"armel.properties"); 
        String theme = p.getProperty("theme");
        IconPack ipack = new IconPack((theme != null? theme:""), manager);
        if (apps != null) {
            int count = apps.size();

            if (mApplications == null) {
                mApplications = new ArrayList<ApplicationInfo>(count);
            }
            mApplications.clear();

            for(ResolveInfo info: apps) {
                Intent launcher = manager.getLaunchIntentForPackage(info.activityInfo.applicationInfo.packageName);
                String componentName = launcher != null ? launcher.getComponent().toString(): info.activityInfo.applicationInfo.packageName;
                String className = launcher != null ? stripComponent(componentName): componentName;
                Log.i(LOG_TAG, className);
                String stat = p.getProperty(className);
                boolean isShow = true; 
                String label = "";
                String condition = "";
                if(stat == null) {
                    p.put(className,"Yes");
                } else {
                    String[] statPart = stat.split(";");
                    isShow = statPart[0].equalsIgnoreCase("Yes");
                    switch(statPart.length) {
                        case 2:
                            condition = statPart[1];                            
                            break;
                        case 3:
                            condition = statPart[1];                            
                            label = statPart[2];
                            break;
                        default:

                    }
                }
                if(!isShow && !showNo) continue; 
                ApplicationInfo application = new ApplicationInfo(this);
                application.setIsShortcut(false);
                application.setPackageName(info.activityInfo.packageName);
                application.setCondition(condition);
                application.title = !label.isEmpty() ?label:info.loadLabel(manager);
                application.isSystem = (info.activityInfo.applicationInfo.flags & android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0;
                application.setActivity(new ComponentName(
                            info.activityInfo.applicationInfo.packageName,
                            info.activityInfo.name),
                        Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
                Bitmap icon = ipack.getBitmap(this, className);
                application.icon = icon != null ? icon
                    :  ipack.getIcon(this, getResources(),className,componentName, info.activityInfo.loadIcon(manager));
                mApplications.add(application);

            }

            mApplications.sort((a,b)-> {return ((String) a.title).compareTo((String) b.title);});
            Utils.writeProperties(this, p, "armel.properties","showAll Reload"); 
        }
    }
    @Override
    protected void onDestroy() {
        //        unregisterReceiver(notif);
        unregisterReceiver(tm);
        unregisterReceiver(br);
        super.onDestroy(); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void onBackPressed() {
        view.setCurrentItem(0);
        // super.onBackPressed(); //To change body of generated methods, choose Tools | Templates.

    }


}
