package com.ramapps.apkshare;

import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.Locale;

public class SettingsActivity extends AppCompatActivity {

    private LinearLayout llLongPressAction, llQuickInfo, llLanguage, llNightMode, llAppTheme, llHelp, llAbout;
    private TextView textViewLongPressAction, textViewQuickInfo, textViewLanguage, textViewNightMode, textViewAppTheme, textViewHelp, textViewAbout;

    private SharedPreferences preferences;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //set app theme
        if (getSharedPreferences(MainActivity.PREFERENCES_SETTINGS, MODE_PRIVATE).getInt(MainActivity.PREFERENCES_SETTINGS_THEME, 0) == 0 && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            setTheme(R.style.dynamic_color_theme);
        } else {
            setTheme(R.style.AppTheme);
        }
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_settings);
        init();
        addListeners();
        loadSettings();
    }

    private void loadSettings() {
        textViewLongPressAction.setText(getResources().getStringArray(R.array.longPressActionOptions)[preferences.getInt(MainActivity.PREFERENCES_SETTINGS_LONG_PRESS_ACTON, 0)]);
        textViewQuickInfo.setText(getResources().getStringArray(R.array.quickInfoOptions)[preferences.getInt(MainActivity.PREFERENCES_SETTINGS_QUICK_INFO, 1)]);

        int lang = 0;
        if (preferences.getString(MainActivity.PREFERENCES_SETTINGS_LANGUAGE, "").equals("en")) {
            lang = 1;
        } else if (preferences.getString(MainActivity.PREFERENCES_SETTINGS_LANGUAGE, "").equals("fa")) {
            lang = 2;
        }
        textViewLanguage.setText(getResources().getStringArray(R.array.languageOptions)[lang]);

        int night = 0;

        if (preferences.getInt(MainActivity.PREFERENCES_SETTINGS_NIGHT_MODE, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM) == AppCompatDelegate.MODE_NIGHT_YES) {
            night = 1;
        } else if (preferences.getInt(MainActivity.PREFERENCES_SETTINGS_NIGHT_MODE, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM) == AppCompatDelegate.MODE_NIGHT_NO) {
            night = 2;
        }
        textViewNightMode.setText(getResources().getStringArray(R.array.nightModeOptions)[night]);
        textViewAppTheme.setText(getResources().getStringArray(R.array.themeOptions)[preferences.getInt(MainActivity.PREFERENCES_SETTINGS_THEME, 0)]);
    }

    private void init() {
        preferences = getSharedPreferences(MainActivity.PREFERENCES_SETTINGS, MODE_PRIVATE);

        llLongPressAction = findViewById(R.id.settingsLinearLayoutLonPressAction);
        llQuickInfo = findViewById(R.id.settingsLinearLayoutQuickInfo);
        llLanguage = findViewById(R.id.settingsLinearLayoutLanguage);
        llNightMode = findViewById(R.id.settingsLinearLayoutNightMode);
        llAppTheme = findViewById(R.id.settingsLinearLayoutTheme);
        llHelp = findViewById(R.id.settingsLinearLayoutHelpAndFeedback);
        llAbout = findViewById(R.id.settingsLinearLayoutAbout);

        textViewLongPressAction = findViewById(R.id.settingsTextViewLongPressActionPreview);
        textViewQuickInfo = findViewById(R.id.settingsTextViewQuickInfoPreview);
        textViewLanguage = findViewById(R.id.settingsTextViewLanguagePreview);
        textViewNightMode = findViewById(R.id.settingsTextViewNightModePreview);
        textViewAppTheme = findViewById(R.id.settingsTextViewThemePreview);

        //Check for dynamic colors.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S){
            llAppTheme.setVisibility(View.GONE);
        }
    }

    private void addListeners() {
        llLongPressAction.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                AlertDialog dialog = new MaterialAlertDialogBuilder(SettingsActivity.this)
                        .setTitle(R.string.long_press_action)
                        .setSingleChoiceItems(R.array.longPressActionOptions, preferences.getInt(MainActivity.PREFERENCES_SETTINGS_LONG_PRESS_ACTON, 0), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                textViewLongPressAction.setText(getResources().getStringArray(R.array.longPressActionOptions)[which]);
                                preferences.edit().putInt(MainActivity.PREFERENCES_SETTINGS_LONG_PRESS_ACTON, which).apply();
                                dialog.dismiss();
                            }
                        })
                        .create();
                dialog.show();
            }
        });
        llQuickInfo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                AlertDialog dialog = new MaterialAlertDialogBuilder(SettingsActivity.this)
                        .setTitle(R.string.quick_info)
                        .setSingleChoiceItems(R.array.quickInfoOptions, preferences.getInt(MainActivity.PREFERENCES_SETTINGS_QUICK_INFO, 1), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                textViewQuickInfo.setText(getResources().getStringArray(R.array.quickInfoOptions)[which]);
                                preferences.edit().putInt(MainActivity.PREFERENCES_SETTINGS_QUICK_INFO, which).apply();
                                dialog.dismiss();
                            }
                        })
                        .create();
                dialog.show();
            }
        });
        llLanguage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int selected = 0;
                if (preferences.getString(MainActivity.PREFERENCES_SETTINGS_LANGUAGE, "").equals("en")) {
                    selected = 1;
                } else if (preferences.getString(MainActivity.PREFERENCES_SETTINGS_LANGUAGE, "").equals("fa")) {
                    selected = 2;
                }

                AlertDialog dialog = new MaterialAlertDialogBuilder(SettingsActivity.this)
                        .setTitle(R.string.language)
                        .setSingleChoiceItems(R.array.languageOptions, selected, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                String[] locales = new String[] {"", "en", "fa"};
                                textViewLanguage.setText(getResources().getStringArray(R.array.languageOptions)[which]);
                                preferences.edit().putString(MainActivity.PREFERENCES_SETTINGS_LANGUAGE, locales[which]).apply();
                                dialog.dismiss();
                                startActivity(new Intent(getApplicationContext(), MainActivity.class).setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
                            }
                        })
                        .create();
                dialog.show();
            }
        });
        llNightMode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int selected = 0;

                if (preferences.getInt(MainActivity.PREFERENCES_SETTINGS_NIGHT_MODE, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM) == AppCompatDelegate.MODE_NIGHT_YES) {
                    selected = 1;
                } else if (preferences.getInt(MainActivity.PREFERENCES_SETTINGS_NIGHT_MODE, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM) == AppCompatDelegate.MODE_NIGHT_NO) {
                    selected = 2;
                }

                AlertDialog dialog = new MaterialAlertDialogBuilder(SettingsActivity.this)
                        .setTitle(R.string.night_mode)
                        .setSingleChoiceItems(R.array.nightModeOptions, selected, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                textViewNightMode.setText(getResources().getStringArray(R.array.nightModeOptions)[which]);
                                int mode = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
                                if (which == 1) {
                                    mode = AppCompatDelegate.MODE_NIGHT_YES;
                                } else if (which == 2) {
                                    mode = AppCompatDelegate.MODE_NIGHT_NO;
                                }

                                preferences.edit().putInt(MainActivity.PREFERENCES_SETTINGS_NIGHT_MODE, mode).apply();
                                AppCompatDelegate.setDefaultNightMode(mode);
                                dialog.dismiss();
                            }
                        })
                        .create();
                dialog.show();
            }
        });
        llAppTheme.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                MaterialAlertDialogBuilder dialogBuilder = new MaterialAlertDialogBuilder(SettingsActivity.this);
                dialogBuilder.setTitle(R.string.app_theme);
                dialogBuilder.setSingleChoiceItems(R.array.themeOptions, preferences.getInt(MainActivity.PREFERENCES_SETTINGS_THEME, 0), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        textViewAppTheme.setText(getResources().getStringArray(R.array.themeOptions)[which]);
                        preferences.edit().putInt(MainActivity.PREFERENCES_SETTINGS_THEME, which).apply();
                        dialog.dismiss();
                        startActivity(new Intent(getApplicationContext(), MainActivity.class).setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
                    }
                });
                AlertDialog dialog = dialogBuilder
                        .create();
                dialog.show();
            }
        });
        llHelp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try{
                    Intent selectorIntent = new Intent(Intent.ACTION_SENDTO);
                    selectorIntent.setData(Uri.parse("mailto:"));

                    Intent emailIntent = new Intent(Intent.ACTION_SEND);
                    emailIntent.putExtra(Intent.EXTRA_EMAIL, new String[] {"newram098@gmail.com"});
                    emailIntent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.help_and_feedback_mail_subject));
                    emailIntent.putExtra(Intent.EXTRA_TEXT, getString(R.string.help_and_feedback_mail_hint));
                    emailIntent.setSelector(selectorIntent);
                    startActivity(Intent.createChooser(emailIntent, "Send mail"));
                } catch (ActivityNotFoundException e){
                    e.printStackTrace();
                }
            }
        });
        llAbout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getApplicationContext(), AboutActivity.class));
            }
        });
    }
}