package armmel.home;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.LauncherActivityInfo;
import android.content.pm.LauncherApps;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.UserHandle;
import android.os.UserManager;
import android.view.View;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.json.JSONException;

public class MainActivity extends Activity {

  protected ArrayList<ApplicationInfo> mApplications;
  ViewPagerD view = null;
  protected final int appCount = GridLayout.numRows * GridLayout.numColumns;
  MyAdapter myAdapter = null;
  private boolean pagerAdapterAttached;
  protected LauncherApps launcherApps;
  private BroadcastReceiver br;
  private BroadcastReceiver tm;
  CustomImageView clock;
  CustomImageView calendar;

  public MainActivity() {}

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    configureTransparentSystemBars();
    setContentView(R.layout.activity_main);

    View rootView = findViewById(R.id.root);
    rootView.setOnApplyWindowInsetsListener(
        new View.OnApplyWindowInsetsListener() {
          @Override
          public WindowInsets onApplyWindowInsets(View v, WindowInsets insets) {
            int statusBarHeight;
            int navBarHeight;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
              statusBarHeight = insets.getInsets(WindowInsets.Type.statusBars()).top;
              navBarHeight = insets.getInsets(WindowInsets.Type.navigationBars()).bottom;
            } else {
              statusBarHeight = insets.getSystemWindowInsetTop();
              navBarHeight = insets.getSystemWindowInsetBottom();
            }
            v.setPadding(v.getPaddingLeft(), statusBarHeight, v.getPaddingRight(), navBarHeight);
            return insets;
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

  private void configureTransparentSystemBars() {
    Window window = getWindow();
    window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
    window.setStatusBarColor(Color.TRANSPARENT);
    window.setNavigationBarColor(Color.TRANSPARENT);
    window.setStatusBarContrastEnforced(false);
    window.setNavigationBarContrastEnforced(false);
    window
        .getDecorView()
        .setSystemUiVisibility(
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
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

    if (myAdapter == null) {
      loadApplications(false);
    }
    if (!pagerAdapterAttached) {
      view.setAdapter(myAdapter);
      pagerAdapterAttached = true;
    } else if (view.getCurrentItem() != 0) {
      view.setCurrentItem(0, false);
    }

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
    LauncherConfig configuration;
    try {
      configuration = Utils.loadConfiguration(this);
    } catch (IOException | JSONException e) {
      Toast.makeText(this, "Invalid armel.json configuration", Toast.LENGTH_LONG).show();
      return;
    }
    IconPack ipack = new IconPack(configuration.getTheme(), manager);
    boolean configurationChanged = false;

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

        LauncherConfig.App configuredApp = configuration.getApp(className);
        if (!configuredApp.exists()) {
          configuration.addDefaultApp(className);
          configuredApp = configuration.getApp(className);
          configurationChanged = true;
        }
        boolean isShow = configuredApp.isVisible();
        String label = configuredApp.getLabel();
        String condition = configuredApp.getAction();
        if (!isShow && !showNo) continue;

        ApplicationInfo application = new ApplicationInfo();
        application.setIsShortcut(false);
        application.setPackageName(packageName);
        application.setConfigurationId(className);

        application.setCondition(condition);
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

    if (configurationChanged && !Utils.writeConfiguration(this, configuration)) {
      Toast.makeText(this, "Failed to save launcher settings", Toast.LENGTH_SHORT).show();
    }
  }

  void reloadConfiguration() {
    LauncherConfig configuration;
    try {
      configuration = Utils.loadConfiguration(this);
    } catch (IOException | JSONException e) {
      Toast.makeText(this, "Invalid armel.json configuration", Toast.LENGTH_LONG).show();
      return;
    }
    configuration.removeDefaultApps();
    if (!Utils.writeConfiguration(this, configuration)) {
      Toast.makeText(this, "Failed to reset launcher settings", Toast.LENGTH_SHORT).show();
      return;
    }
    IconPack.clearGeneratedIcons(this);
    loadApplications(false, false);
  }

  void showApplicationMenu(ApplicationInfo application) {
    String[] actions =
        application.isShortcut() || application.isSystem
            ? new String[] {"Edit properties"}
            : new String[] {"Edit properties", "Uninstall"};
    new AlertDialog.Builder(this)
        .setTitle(application.title)
        .setItems(
            actions,
            (dialog, which) -> {
              if (which == 0) {
                showApplicationEditor(application);
              } else {
                Uri packageUri = Uri.parse("package:" + application.getPackageName());
                startActivity(new Intent(Intent.ACTION_DELETE, packageUri));
              }
            })
        .show();
  }

  void showApplicationEditor(ApplicationInfo application) {
    LauncherConfig configuration;
    try {
      configuration = Utils.loadConfiguration(this);
    } catch (IOException | JSONException e) {
      Toast.makeText(this, "Invalid armel.json configuration", Toast.LENGTH_LONG).show();
      return;
    }
    LauncherConfig.App configuredApp = configuration.editApp(application.getConfigurationId());
    int padding = (int) (16 * getResources().getDisplayMetrics().density);
    LinearLayout fields = new LinearLayout(this);
    fields.setOrientation(LinearLayout.VERTICAL);
    fields.setPadding(padding, 0, padding, 0);

    CheckBox visible = new CheckBox(this);
    visible.setText("Visible in launcher");
    visible.setChecked(configuredApp.isVisible());
    fields.addView(visible);

    EditText label = new EditText(this);
    label.setHint("Custom label");
    label.setSingleLine(true);
    label.setText(configuredApp.getLabel());
    fields.addView(label);

    EditText action = new EditText(this);
    action.setHint("Action (for example: calendar or clock)");
    action.setSingleLine(true);
    action.setText(configuredApp.getAction());
    fields.addView(action);

    new AlertDialog.Builder(this)
        .setTitle(application.title)
        .setView(fields)
        .setNegativeButton(android.R.string.cancel, null)
        .setPositiveButton(
            android.R.string.ok,
            (dialog, which) -> {
              configuredApp.setVisible(visible.isChecked());
              configuredApp.setLabel(label.getText().toString().trim());
              configuredApp.setAction(action.getText().toString().trim());
              if (!Utils.writeConfiguration(this, configuration)) {
                Toast.makeText(this, "Failed to save launcher settings", Toast.LENGTH_SHORT).show();
                return;
              }
              loadApplications(false);
              myAdapter.notifyDataSetChanged();
            })
        .show();
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

}
