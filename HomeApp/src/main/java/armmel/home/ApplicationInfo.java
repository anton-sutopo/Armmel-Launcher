/*
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package armmel.home;

import android.content.ComponentName;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.graphics.Bitmap;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.content.pm.ShortcutInfo;
import java.util.ArrayList;
/**
 * Represents a launchable application. An application is made of a name (or
 * title), an intent and an icon.
 */
class ApplicationInfo {
    public static final String CALENDAR="calendar"; 
    public static final String CLOCK="clock"; 
    public static final String SHOWALL="showall"; 
    public static final String RELOAD="reload"; 
    /**
     * The application name.
     */
    CharSequence title;
    TextView countTextView = null;
    /**
     * The intent used to start the application.
     */
    Intent intent;

    /**
     * The application icon.
     */
    Bitmap icon;

    boolean isSystem = false;
    boolean isShowAll = false;
    boolean isReload = false;
    private boolean isShortcut = false;
    private boolean calendar = false;
    private boolean clock = false; 
    public int Count = 0;
    private ShortcutInfo shortcutInfo;
    private ArrayList<ApplicationInfo> childList = new ArrayList<>(); 
    private String packageName;
    /**
     * When set to true, indicates that the icon has been resized.
     */
    boolean filtered;
    private final MainActivity m;

    /**
     * Creates the application intent based on a component name and various
     * launch flags.
     *
     * @param className the class name of the component representing the intent
     * @param launchFlags the launch flags
     */
    final void setIntent(Intent intent) {
        intent = intent;
    }
    final void setActivity(ComponentName className, int launchFlags) {
        intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        intent.setComponent(className);
        intent.setFlags(launchFlags);
    }

    public ApplicationInfo(MainActivity m) {
        this.m = m;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ApplicationInfo)) {
            return false;
        }

        ApplicationInfo that = (ApplicationInfo) o;
        return title.equals(that.title)
            && intent.getComponent().getClassName().equals(
                    that.intent.getComponent().getClassName());
    }

    public boolean equalsPackageName(String name, String classN) {
        String packageName = intent.getComponent().getPackageName();
        String className = intent.getComponent().getClassName();
        return packageName.equalsIgnoreCase(name) && className.equalsIgnoreCase(classN);
    }

    @Override
    public int hashCode() {
        int result;
        result = (title != null ? title.hashCode() : 0);
        final String name = intent.getComponent().getClassName();
        result = 31 * result + (name != null ? name.hashCode() : 0);
        return result;
    }

    void addCount(int c) {
        Count = c;
        Log.i("home1", "countText" + countTextView.toString() + ", " + Count);
        if (countTextView != null) {
            countTextView.setText(Count + "");
            countTextView.setVisibility(View.VISIBLE);
        }
    }

    void clearCount() {
        Count = 0;
        if (countTextView != null) {
            countTextView.setVisibility(View.INVISIBLE);
        }
    }
    public void setCondition(String condition) {
        this.calendar = condition.equalsIgnoreCase(CALENDAR);
        this.isShowAll = condition.equalsIgnoreCase(SHOWALL);
        this.isReload = condition.equalsIgnoreCase(RELOAD);
        this.clock = condition.equalsIgnoreCase(CLOCK);
    }
    public boolean isClock() {
        return clock;
    }
    public boolean isCalendar() {
        return calendar; 
    }
    public void setIsShortcut(boolean isShortcut) {
        this.isShortcut = isShortcut;
    }
    public boolean isShortcut() {
        return isShortcut;
    }
    public void setShortcutInfo(ShortcutInfo shortcutInfo) {
        this.shortcutInfo = shortcutInfo;
    }
    public ShortcutInfo getShortcutInfo() {
        return this.shortcutInfo;
    }
    public void AddChild(ApplicationInfo child) {
        childList.add(child); 
    }
    public ArrayList<ApplicationInfo> getChildList() {
        return childList;
    }
    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }
    public String getPackageName() {
        return this.packageName;
    }
}
