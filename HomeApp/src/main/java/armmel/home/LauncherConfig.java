package armmel.home;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

class LauncherConfig {
  static final String FILE_NAME = "armel.json";
  private static final String APPS = "apps";
  private final JSONObject root;
  private JSONArray apps;

  LauncherConfig() {
    root = new JSONObject();
    apps = new JSONArray();
    try {
      root.put("version", 1);
      root.put(APPS, apps);
    } catch (JSONException e) {
      throw new IllegalStateException("Unable to create launcher configuration", e);
    }
  }

  LauncherConfig(JSONObject root) throws JSONException {
    this.root = root;
    JSONArray configuredApps = root.optJSONArray(APPS);
    if (configuredApps == null) {
      configuredApps = new JSONArray();
      root.put(APPS, configuredApps);
    }
    apps = configuredApps;
  }

  String getTheme() {
    return root.optString("theme", "");
  }

  App getApp(String id) {
    for (int index = 0; index < apps.length(); index++) {
      JSONObject app = apps.optJSONObject(index);
      if (app != null && id.equals(app.optString("id"))) {
        return new App(app);
      }
    }
    return new App(null);
  }

  void addDefaultApp(String id) {
    if (getApp(id).exists()) {
      return;
    }
    createApp(id);
  }

  boolean setDefaultAction(String id, String action) {
    App configuredApp = getApp(id);
    if (configuredApp.exists() && !configuredApp.getAction().isEmpty()) {
      return false;
    }
    if (!configuredApp.exists()) {
      configuredApp = createApp(id);
    }
    configuredApp.setAction(action);
    return true;
  }

  App editApp(String id) {
    App configuredApp = getApp(id);
    return configuredApp.exists() ? configuredApp : createApp(id);
  }

  private App createApp(String id) {
    try {
      JSONObject app = new JSONObject();
      app.put("id", id);
      app.put("visible", true);
      apps.put(app);
      return new App(app);
    } catch (JSONException e) {
      throw new IllegalStateException("Unable to add launcher app", e);
    }
  }

  void removeDefaultApps() {
    JSONArray retained = new JSONArray();
    for (int index = 0; index < apps.length(); index++) {
      JSONObject app = apps.optJSONObject(index);
      if (app == null
          || !app.optBoolean("visible", true)
          || app.has("action")
          || app.has("label")) {
        retained.put(app);
      }
    }
    try {
      root.put(APPS, retained);
      apps = retained;
    } catch (JSONException e) {
      throw new IllegalStateException("Unable to reset launcher configuration", e);
    }
  }

  JSONObject toJson() {
    return root;
  }

  static class App {
    private final JSONObject value;

    App(JSONObject value) {
      this.value = value;
    }

    boolean exists() {
      return value != null;
    }

    boolean isVisible() {
      return value == null || value.optBoolean("visible", true);
    }

    String getAction() {
      return value == null ? "" : value.optString("action", "");
    }

    String getLabel() {
      return value == null ? "" : value.optString("label", "");
    }

    void setAction(String action) {
      setString("action", action);
    }

    void setLabel(String label) {
      setString("label", label);
    }

    void setVisible(boolean visible) {
      try {
        value.put("visible", visible);
      } catch (JSONException e) {
        throw new IllegalStateException("Unable to set launcher app visibility", e);
      }
    }

    private void setString(String key, String valueToSet) {
      try {
        if (valueToSet.isEmpty()) {
          value.remove(key);
        } else {
          value.put(key, valueToSet);
        }
      } catch (JSONException e) {
        throw new IllegalStateException("Unable to set launcher app property", e);
      }
    }
  }
}
