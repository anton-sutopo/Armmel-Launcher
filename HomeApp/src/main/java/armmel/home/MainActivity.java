package armmel.home;

import android.Manifest;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.LauncherActivityInfo;
import android.content.pm.LauncherApps;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.UserHandle;
import android.os.UserManager;
import android.view.View;
import android.view.WindowInsets;
import android.widget.Toast;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Properties;

public class MainActivity extends Activity {

  public static int height;
  public static final String CLEAR = "arm.home.clear";
  public static final String ADD = "arm.home.add";
  protected ArrayList<ApplicationInfo> mApplications;
  private static final String LOG_TAG = "Home";
  ViewPagerD view = null;
  protected final int appCount = GridLayout.numRows * GridLayout.numColumns;
  MyAdapter myAdapter = null;
  protected LauncherApps launcherApps;
  private BroadcastReceiver br;
  private BroadcastReceiver tm;
  CustomImageView clock;
  CustomImageView calendar;

  public MainActivity() {}

  private static final int PICK_PROPERTIES_FILE = 101;

  public void openFilePicker() {
    Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
    intent.setType("*/*"); // or "text/plain"
    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
    startActivityForResult(intent, PICK_PROPERTIES_FILE);
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    View rootView = findViewById(R.id.root);
    rootView.setOnApplyWindowInsetsListener(
        new View.OnApplyWindowInsetsListener() {
          @Override
          public WindowInsets onApplyWindowInsets(View v, WindowInsets insets) {

            // Get system bar insets
            int statusBarHeight = insets.getInsets(WindowInsets.Type.statusBars()).top;
            int navBarHeight = insets.getInsets(WindowInsets.Type.navigationBars()).bottom;

            // Apply padding to root layout
            v.setPadding(v.getPaddingLeft(), statusBarHeight, v.getPaddingRight(), navBarHeight);

            return insets; // must return insets
          }
        });

    view = (ViewPagerD) findViewById(R.id.viewPager);

    loadApplications(true);
    br =
        new BroadcastReceiver() {
          @Override
          public void onReceive(Context context, Intent intent) {
            loadApplications(false);
            myAdapter.notifyDataSetChanged();
          }
        };
    tm =
        new BroadcastReceiver() {
          @Override
          public void onReceive(Context context, Intent intent) {
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



  @Override
  protected void onResume() {
    super.onResume(); // To change body of generated methods, choose Tools | Templates.
    loadApplications(true);
  }

  @Override
  public void onWindowFocusChanged(boolean hasFocus) {
    super.onWindowFocusChanged(
        hasFocus); // To change body of generated methods, choose Tools | Templates.

    height = view.getHeight() - view.getTop();
    if (myAdapter == null) {
      loadApplications(false);
    }
    view.setAdapter(myAdapter);
    myAdapter.notifyDataSetChanged();

  }

  protected void loadApplications(boolean isLaunching) {
    loadApplications(isLaunching, false);
  }

  private String stripComponent(String componentName) {
    int start = componentName.indexOf("{") + 1;
    int end = componentName.indexOf("}", start);
    if (end > start) {
      return componentName.substring(start, end).toLowerCase(Locale.getDefault()).replace("/", "_");
    }
    return "";
  }

  protected void loadApplications(boolean isLaunching, boolean showNo) {
    if (isLaunching && mApplications != null) {
      return;
    }
    if (myAdapter == null) {
      LauncherApps launcherApps = (LauncherApps) getSystemService(Context.LAUNCHER_APPS_SERVICE);
      myAdapter = new MyAdapter(this, launcherApps);
    }

    // PackageManager for personal profile fallback
    PackageManager manager = getPackageManager();

    // Load properties
    Properties p = Utils.loadProperties(this, "armel.properties");
    String theme = p.getProperty("theme");
    IconPack ipack = new IconPack((theme != null ? theme : ""), manager);

    if (mApplications == null) {
      mApplications = new ArrayList<>();
    }
    mApplications.clear();

    // --- NEW: get apps from all profiles ---
    UserManager userManager = (UserManager) getSystemService(Context.USER_SERVICE);
    launcherApps = myAdapter.launcherapps;
    List<UserHandle> profiles = userManager.getUserProfiles();

    for (UserHandle user : profiles) {
      List<LauncherActivityInfo> apps = launcherApps.getActivityList(null, user);

      for (LauncherActivityInfo info : apps) {
        String packageName = info.getApplicationInfo().packageName;
        ComponentName componentName = info.getComponentName();
        String className =
            stripComponent(componentName.toString())
                + ((user != null) ? "_" + user.hashCode() : "_personal");

        String stat = p.getProperty(className);
        boolean isShow = true;
        String label = "";
        String condition = "";
        if (stat == null) {
          p.put(className, "Yes");
        } else {
          String[] statPart = stat.split(";");
          isShow = statPart[0].equalsIgnoreCase("Yes");
          switch (statPart.length) {
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
        if (!isShow && !showNo) continue;

        ApplicationInfo application = new ApplicationInfo(this);
        application.setIsShortcut(false);
        application.setPackageName(packageName);

        if (packageName.equalsIgnoreCase("com.android.settings")
            && Utils.isStringEmpty(condition)) {
          application.setCondition(ApplicationInfo.PROPERTIES);
        } else {
          application.setCondition(condition);
        }
        application.setComponentName(componentName);
        application.setUserHandle(user);
        application.title = !label.isEmpty() ? label : info.getLabel();
        application.isSystem =
            (info.getApplicationInfo().flags & android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0;
        application.setActivity(
            componentName,
            Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);

        Bitmap icon = ipack.getBitmap(this, className);
        application.icon =
            icon != null
                ? icon
                : ipack.getIcon(
                    this,
                    getResources(),
                    className,
                    componentName.toString(),
                    info.getIcon(0)); // default icon

        mApplications.add(application);
      }
    }

    // Sort applications
    mApplications.sort((a, b) -> ((String) a.title).compareToIgnoreCase((String) b.title));

    // Save updated properties
    Utils.writeProperties(this, p, "armel.properties", "showAll Reload");
  }

  @Override
  protected void onDestroy() {
    unregisterReceiver(tm);
    unregisterReceiver(br);
    super.onDestroy(); // To change body of generated methods, choose Tools | Templates.
  }

  @Override
  public void onBackPressed() {
    view.setCurrentItem(0);

  }

  private void copyFileToAppDir(Uri sourceUri) {
    File destFile = new File(getExternalFilesDir("/"), "armel.properties");

    try (InputStream in = getContentResolver().openInputStream(sourceUri);
        OutputStream out = new FileOutputStream(destFile)) {

      byte[] buffer = new byte[4096];
      int read;
      while ((read = in.read(buffer)) != -1) {
        out.write(buffer, 0, read);
      }

      Toast.makeText(this, "File uploaded successfully!", Toast.LENGTH_SHORT).show();
      loadApplications(false);
    } catch (IOException e) {
      e.printStackTrace();
      Toast.makeText(this, "Failed to upload file", Toast.LENGTH_SHORT).show();
    }
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    if (requestCode == PICK_PROPERTIES_FILE && resultCode == RESULT_OK) {
      Uri uri = data.getData();
      if (uri != null) {
        try {
          getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
        } catch (Exception e) {
          // some providers don't allow persistable permissions; ignore
        }
        copyFileToAppDir(uri);
      }
    }
  }
}
