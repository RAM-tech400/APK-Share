package com.ramapps.apkshare;

import androidx.core.graphics.Insets;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class GlobalVariables {
    public static final String PREFERENCES_SETTINGS = "Settings";
    public static final String PREFERENCES_SETTINGS_SORT_BY = "Sort by";
    public static final String PREFERENCES_SETTINGS_REVERSE_SORT = "Reverse sort";
    public static final String PREFERENCES_SETTINGS_VIBRATION = "Vibrate";
    public static final String PREFERENCES_SETTINGS_COLUMN_COUNT = "Column count";
    public static final String PREFERENCES_SETTINGS_LONG_PRESS_ACTON = "Long press action";
    public static final String PREFERENCES_SETTINGS_QUICK_INFO = "Quick info";
    public static final String PREFERENCES_SETTINGS_LANGUAGE = "Language";
    public static final String PREFERENCES_SETTINGS_NIGHT_MODE = "Night mode";
    public static final String PREFERENCES_SETTINGS_THEME = "App theme";

    public static final int FLAG_SORT_BY_NAME = 0;
    public static final int FLAG_SORT_BY_INSTALL_DATE = 1;
    public static final int FLAG_SORT_BY_SIZE = 2;

    public static final String ACTION_RESHARE = "com.ramapps.apkshare.ACTION_RESHARE";

    public static Insets systemBars;
    public static Insets imeInsets;
    public static Insets displayCutouts;

    public static FloatingActionButton fabSend, fabSendSearchView;
}
