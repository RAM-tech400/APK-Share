package com.ramapps.apkshare;

import static com.ramapps.apkshare.GlobalVariables.PREFERENCES_SETTINGS;
import static com.ramapps.apkshare.GlobalVariables.PREFERENCES_SETTINGS_LANGUAGE;
import static com.ramapps.apkshare.GlobalVariables.PREFERENCES_SETTINGS_NIGHT_MODE;
import static com.ramapps.apkshare.GlobalVariables.PREFERENCES_SETTINGS_THEME;

import android.app.Application;
import android.app.LocaleManager;
import android.content.res.Configuration;
import android.os.Build;
import android.os.LocaleList;
import android.util.DisplayMetrics;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;

import com.google.android.material.color.DynamicColors;

import java.util.Locale;

public class ApkShareApplication  extends Application {
    @Override
    public void onCreate() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            LocaleList currentLocales = getSystemService(LocaleManager.class).getApplicationLocales(getPackageName());
            if (!currentLocales.isEmpty()) {
                AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(currentLocales.get(0).toString()));
            }
        } else {
            String langCode = getSharedPreferences(PREFERENCES_SETTINGS, MODE_PRIVATE).getString(PREFERENCES_SETTINGS_LANGUAGE, "");
            Configuration configuration = getResources().getConfiguration();
            DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
            if (langCode.isEmpty()) {
                configuration.setLocale(Locale.getDefault());
            } else {
                configuration.setLocale(new Locale(langCode));
            }
            getResources().updateConfiguration(configuration, displayMetrics);
        }
        DynamicColors.applyToActivitiesIfAvailable(this);
        super.onCreate();
    }
}
