package armmel.contacts.utils;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import armmel.contacts.R;

public class ThemeUtils {

    public static boolean isNightMode(Context context) {
        int nightModeFlags = context.getResources().getConfiguration().uiMode
            & Configuration.UI_MODE_NIGHT_MASK;
        return nightModeFlags == Configuration.UI_MODE_NIGHT_YES; 
    }

    public static void applyTheme(Activity activity) {
        if (isNightMode(activity)) {
            activity.setTheme(R.style.AppTheme_Dark); // uses Holo
        } else {
            activity.setTheme(R.style.AppTheme_Light); // uses Holo.Light
        }
    }
}

