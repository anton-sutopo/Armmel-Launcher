package armmel.home;

import android.content.Context;
import android.util.Log;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.json.JSONException;
import org.json.JSONObject;

public class Utils {
  private static String root = "";

  public static String getRoot(Context c) {
    if (isStringEmpty(root)) {
      root = c.getExternalFilesDir("/").getAbsolutePath();
    }
    return root;
  }

  public static boolean isStringEmpty(String s) {
    return s == null || s.isEmpty();
  }

  public static LauncherConfig loadConfiguration(Context c) throws IOException, JSONException {
    File file = new File(getRoot(c), LauncherConfig.FILE_NAME);
    if (!file.exists()) {
      return new LauncherConfig();
    }
    byte[] bytes = new byte[(int) file.length()];
    try (FileInputStream input = new FileInputStream(file)) {
      int offset = 0;
      while (offset < bytes.length) {
        int count = input.read(bytes, offset, bytes.length - offset);
        if (count < 0) {
          throw new IOException("Unexpected end of configuration file");
        }
        offset += count;
      }
    }
    return new LauncherConfig(new JSONObject(new String(bytes, StandardCharsets.UTF_8)));
  }

  public static boolean writeConfiguration(Context c, LauncherConfig configuration) {
    try (FileOutputStream output =
        new FileOutputStream(getRoot(c) + File.separator + LauncherConfig.FILE_NAME)) {
      output.write(configuration.toJson().toString(2).getBytes(StandardCharsets.UTF_8));
      return true;
    } catch (IOException | JSONException e) {
      e.printStackTrace();
      return false;
    }
  }

  public static void clearDirectory(Context c, String directoryName) {
    File directory = new File(getRoot(c), directoryName);
    File[] files = directory.listFiles();
    if (files == null) {
      return;
    }
    for (File file : files) {
      if (!file.delete()) {
        Log.w("ArmHome", "Unable to delete cached icon: " + file.getAbsolutePath());
      }
    }
  }
}
