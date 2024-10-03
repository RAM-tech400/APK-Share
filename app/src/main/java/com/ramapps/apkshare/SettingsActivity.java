package com.ramapps.apkshare;

import android.app.LocaleManager;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.LocaleList;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.graphics.Insets;
import androidx.core.os.LocaleListCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.materialswitch.MaterialSwitch;

public class SettingsActivity extends AppCompatActivity {

    private LinearLayout llLongPressAction, llQuickInfo, llLanguage, llNightMode, llAppTheme, llPermissions, llHelp, llAbout;
    private RelativeLayout rlVibration;
    private MaterialSwitch switchVibration;
    private TextView textViewLongPressAction, textViewQuickInfo, textViewLanguage, textViewNightMode, textViewAppTheme;
    private AppBarLayout appBarLayout;
    private MaterialToolbar toolbar;

    private SharedPreferences preferences;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //set app theme
        if (getSharedPreferences(GlobalVariables.PREFERENCES_SETTINGS, MODE_PRIVATE).getInt(GlobalVariables.PREFERENCES_SETTINGS_THEME, 0) == 0 && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            setTheme(R.style.dynamic_color_theme);
        } else {
            setTheme(R.style.AppTheme);
        }
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_settings);
        init();
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.settings), (v, insets) -> {
            Insets displayCutouts = insets.getInsets(WindowInsetsCompat.Type.displayCutout());
            findViewById(R.id.settingsNestedScrollView).setPadding(
                    displayCutouts.left,
                    findViewById(R.id.settingsNestedScrollView).getPaddingTop(),
                    displayCutouts.right,
                    findViewById(R.id.settingsNestedScrollView).getPaddingBottom());
            toolbar.setPadding(
                    displayCutouts.left,
                    toolbar.getPaddingTop(),
                    displayCutouts.right,
                    toolbar.getPaddingBottom());
            return insets;
        });
        addListeners();
        loadSettings();
    }

    private void loadSettings() {
        textViewLongPressAction.setText(getResources().getStringArray(R.array.longPressActionOptions)[preferences.getInt(GlobalVariables.PREFERENCES_SETTINGS_LONG_PRESS_ACTON, 0)]);
        textViewQuickInfo.setText(getResources().getStringArray(R.array.quickInfoOptions)[preferences.getInt(GlobalVariables.PREFERENCES_SETTINGS_QUICK_INFO, 1)]);

        int lang = 0;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            LocaleList currentLocales = getSystemService(LocaleManager.class).getApplicationLocales(getPackageName());
            if ((currentLocales.get(0) + "").toLowerCase().contains("en")) {
                lang = 1;
            } else if ((currentLocales.get(0) + "").toLowerCase().contains("fa")) {
                lang = 2;
            }
        } else {
            if (preferences.getString(GlobalVariables.PREFERENCES_SETTINGS_LANGUAGE, "").equals("en")) {
                lang = 1;
            } else if (preferences.getString(GlobalVariables.PREFERENCES_SETTINGS_LANGUAGE, "").equals("fa")) {
                lang = 2;
            }
        }
        textViewLanguage.setText(getResources().getStringArray(R.array.languageOptions)[lang]);

        int night = 0;

        if (preferences.getInt(GlobalVariables.PREFERENCES_SETTINGS_NIGHT_MODE, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM) == AppCompatDelegate.MODE_NIGHT_YES) {
            night = 1;
        } else if (preferences.getInt(GlobalVariables.PREFERENCES_SETTINGS_NIGHT_MODE, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM) == AppCompatDelegate.MODE_NIGHT_NO) {
            night = 2;
        }
        textViewNightMode.setText(getResources().getStringArray(R.array.nightModeOptions)[night]);
        textViewAppTheme.setText(getResources().getStringArray(R.array.themeOptions)[preferences.getInt(GlobalVariables.PREFERENCES_SETTINGS_THEME, 0)]);

        toolbar.setNavigationOnClickListener(v -> {
            finish();
        });

        switchVibration.setChecked(preferences.getBoolean(GlobalVariables.PREFERENCES_SETTINGS_VIBRATION, true));
    }

    private void init() {
        preferences = getSharedPreferences(GlobalVariables.PREFERENCES_SETTINGS, MODE_PRIVATE);

        llLongPressAction = findViewById(R.id.settingsLinearLayoutLonPressAction);
        llQuickInfo = findViewById(R.id.settingsLinearLayoutQuickInfo);
        llLanguage = findViewById(R.id.settingsLinearLayoutLanguage);
        llNightMode = findViewById(R.id.settingsLinearLayoutNightMode);
        llAppTheme = findViewById(R.id.settingsLinearLayoutTheme);
        llPermissions = findViewById(R.id.settingsLinearLayoutAppPermissions);
        llHelp = findViewById(R.id.settingsLinearLayoutHelpAndFeedback);
        llAbout = findViewById(R.id.settingsLinearLayoutAbout);

        rlVibration = findViewById(R.id.settingsRelativeLayoutVibration);

        switchVibration = findViewById(R.id.settingsSwitchVibration);

        textViewLongPressAction = findViewById(R.id.settingsTextViewLongPressActionPreview);
        textViewQuickInfo = findViewById(R.id.settingsTextViewQuickInfoPreview);
        textViewLanguage = findViewById(R.id.settingsTextViewLanguagePreview);
        textViewNightMode = findViewById(R.id.settingsTextViewNightModePreview);
        textViewAppTheme = findViewById(R.id.settingsTextViewThemePreview);

        appBarLayout = findViewById(R.id.settingsAppBarLayout);
        toolbar = findViewById(R.id.settingsToolbar);

        //Check for dynamic colors.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S){
            llAppTheme.setVisibility(View.GONE);
        }
    }

    private void addListeners() {
        llLongPressAction.setOnClickListener(v -> {

            AlertDialog dialog = new MaterialAlertDialogBuilder(SettingsActivity.this)
                    .setTitle(R.string.long_press_action)
                    .setSingleChoiceItems(R.array.longPressActionOptions, preferences.getInt(GlobalVariables.PREFERENCES_SETTINGS_LONG_PRESS_ACTON, 0), (dialog1, which) -> {
                        textViewLongPressAction.setText(getResources().getStringArray(R.array.longPressActionOptions)[which]);
                        preferences.edit().putInt(GlobalVariables.PREFERENCES_SETTINGS_LONG_PRESS_ACTON, which).apply();
                        dialog1.dismiss();
                    })
                    .create();
            dialog.show();
        });
        llQuickInfo.setOnClickListener(v -> {

            AlertDialog dialog = new MaterialAlertDialogBuilder(SettingsActivity.this)
                    .setTitle(R.string.quick_info)
                    .setSingleChoiceItems(R.array.quickInfoOptions, preferences.getInt(GlobalVariables.PREFERENCES_SETTINGS_QUICK_INFO, 1), (dialog12, which) -> {
                        textViewQuickInfo.setText(getResources().getStringArray(R.array.quickInfoOptions)[which]);
                        preferences.edit().putInt(GlobalVariables.PREFERENCES_SETTINGS_QUICK_INFO, which).apply();
                        dialog12.dismiss();
                    })
                    .create();
            dialog.show();
        });
        llLanguage.setOnClickListener(v -> {
            int selected = 0;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                LocaleList currentLocales = getSystemService(LocaleManager.class).getApplicationLocales(getPackageName());
                if ((currentLocales.get(0) + "").toLowerCase().contains("en")) {
                    selected = 1;
                } else if ((currentLocales.get(0) + "").toLowerCase().contains("fa")) {
                    selected = 2;
                }
            } else {
                if (preferences.getString(GlobalVariables.PREFERENCES_SETTINGS_LANGUAGE, "").equals("en")) {
                    selected = 1;
                } else if (preferences.getString(GlobalVariables.PREFERENCES_SETTINGS_LANGUAGE, "").equals("fa")) {
                    selected = 2;
                }
            }

            AlertDialog dialog = new MaterialAlertDialogBuilder(SettingsActivity.this)
                    .setTitle(R.string.language)
                    .setSingleChoiceItems(R.array.languageOptions, selected, (dialog13, which) -> {
                        String[] locales = new String[] {"", "en", "fa"};
                        textViewLanguage.setText(getResources().getStringArray(R.array.languageOptions)[which]);
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            LocaleListCompat appLocale = LocaleListCompat.forLanguageTags(locales[which]);
                            AppCompatDelegate.setApplicationLocales(appLocale);
                        } else {
                            preferences.edit().putString(GlobalVariables.PREFERENCES_SETTINGS_LANGUAGE, locales[which]).apply();
                            dialog13.dismiss();
                            startActivity(new Intent(getApplicationContext(), MainActivity.class).setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
                        }
                    })
                    .create();
            dialog.show();
        });
        llNightMode.setOnClickListener(v -> {
            int selected = 0;

            if (preferences.getInt(GlobalVariables.PREFERENCES_SETTINGS_NIGHT_MODE, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM) == AppCompatDelegate.MODE_NIGHT_YES) {
                selected = 1;
            } else if (preferences.getInt(GlobalVariables.PREFERENCES_SETTINGS_NIGHT_MODE, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM) == AppCompatDelegate.MODE_NIGHT_NO) {
                selected = 2;
            }

            AlertDialog dialog = new MaterialAlertDialogBuilder(SettingsActivity.this)
                    .setTitle(R.string.night_mode)
                    .setSingleChoiceItems(R.array.nightModeOptions, selected, (dialog14, which) -> {
                        textViewNightMode.setText(getResources().getStringArray(R.array.nightModeOptions)[which]);
                        int mode = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
                        if (which == 1) {
                            mode = AppCompatDelegate.MODE_NIGHT_YES;
                        } else if (which == 2) {
                            mode = AppCompatDelegate.MODE_NIGHT_NO;
                        }

                        preferences.edit().putInt(GlobalVariables.PREFERENCES_SETTINGS_NIGHT_MODE, mode).apply();
                        AppCompatDelegate.setDefaultNightMode(mode);
                        dialog14.dismiss();
                    })
                    .create();
            dialog.show();
        });
        llAppTheme.setOnClickListener(v -> {

            MaterialAlertDialogBuilder dialogBuilder = new MaterialAlertDialogBuilder(SettingsActivity.this);
            dialogBuilder.setTitle(R.string.app_theme);
            dialogBuilder.setSingleChoiceItems(R.array.themeOptions, preferences.getInt(GlobalVariables.PREFERENCES_SETTINGS_THEME, 0), (dialog, which) -> {
                textViewAppTheme.setText(getResources().getStringArray(R.array.themeOptions)[which]);
                preferences.edit().putInt(GlobalVariables.PREFERENCES_SETTINGS_THEME, which).apply();
                dialog.dismiss();
                startActivity(new Intent(getApplicationContext(), MainActivity.class).setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
            });
            AlertDialog dialog = dialogBuilder
                    .create();
            dialog.show();
        });

        llPermissions.setOnClickListener(v -> {
            PermissionsListModalBottomSheet permissionsListModalBottomSheet = new PermissionsListModalBottomSheet();
            permissionsListModalBottomSheet.show(getSupportFragmentManager(), PermissionsListModalBottomSheet.class.getName());
        });

        llHelp.setOnClickListener(v -> {
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
                Toast.makeText(this, getString(R.string.activity_not_found_error_msg), Toast.LENGTH_SHORT).show();
            }
        });
        llAbout.setOnClickListener(v -> startActivity(new Intent(getApplicationContext(), AboutActivity.class)));

        rlVibration.setOnClickListener(v -> {
            switchVibration.toggle();
            preferences.edit().putBoolean(GlobalVariables.PREFERENCES_SETTINGS_VIBRATION, switchVibration.isChecked()).apply();
            if (switchVibration.isChecked()) {
                Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE));
                } else {
                    vibrator.vibrate(100);
                }
            }
        });
    }
}