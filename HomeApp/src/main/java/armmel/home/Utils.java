package armmel.home;

import android.os.Environment;
import android.content.Context;
import java.util.Properties;
import java.io.IOException;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.File;
public class Utils {    
    private static String root = "";

    public static String getRoot(Context c) {
        if(isStringEmpty(root)) {
            root = c.getExternalFilesDir("/").getAbsolutePath();        
        }
        return root;
    }

    public static boolean isStringEmpty(String s) {
        return s == null || s.isEmpty();
    }

    public static Properties loadProperties(Context c, String fileName) {
        Properties configFile = new Properties();
        try {
            configFile.load(new FileInputStream(getRoot(c)+File.separator+fileName));
        } catch (IOException e) {
            e.printStackTrace();
            configFile = new Properties();
        }
        return configFile;
    }
    public static Properties cleanPropertiesHasValue(Properties p, String value) {
        Properties p1 = new Properties();
        for(String key : p.stringPropertyNames()) {
            if(!p.getProperty(key).equalsIgnoreCase(value)) {
                p1.put(key,p.getProperty(key));
            }  
        }

        return p1;
    }
    public static boolean writeProperties(Context c, Properties p, String fileName) {
        try {
            p.store(new FileOutputStream(getRoot(c)+File.separator+fileName),null);
        } catch(Exception e) {
            e.printStackTrace(); 
        }
        return true;
    }
}
