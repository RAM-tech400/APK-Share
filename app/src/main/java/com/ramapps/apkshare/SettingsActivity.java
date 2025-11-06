package com.ramapps.apkshare;

import static com.ramapps.apkshare.GlobalVariables.*;

import android.app.LocaleManager;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.LocaleList;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
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

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class SettingsActivity extends AppCompatActivity {
    private static final String TAG = "SettingsActivity";
    private LinearLayout llLongPressAction, llQuickInfo, llLanguage, llNightMode, llPermissions, llHelp, llAbout;
    private TextView textViewLongPressAction, textViewQuickInfo, textViewLanguage, textViewNightMode;
    private MaterialToolbar toolbar;
    private SharedPreferences preferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_settings);
        initViews();
        addListeners();
        loadSettings();
    }

    private void initViews() {
        preferences = getSharedPreferences(PREFERENCES_SETTINGS, MODE_PRIVATE);
        llLongPressAction = findViewById(R.id.settingsLinearLayoutLonPressAction);
        llQuickInfo = findViewById(R.id.settingsLinearLayoutQuickInfo);
        llLanguage = findViewById(R.id.settingsLinearLayoutLanguage);
        llNightMode = findViewById(R.id.settingsLinearLayoutNightMode);
        llPermissions = findViewById(R.id.settingsLinearLayoutAppPermissions);
        llHelp = findViewById(R.id.settingsLinearLayoutHelpAndFeedback);
        llAbout = findViewById(R.id.settingsLinearLayoutAbout);
        textViewLongPressAction = findViewById(R.id.settingsTextViewLongPressActionPreview);
        textViewQuickInfo = findViewById(R.id.settingsTextViewQuickInfoPreview);
        textViewLanguage = findViewById(R.id.settingsTextViewLanguagePreview);
        textViewNightMode = findViewById(R.id.settingsTextViewNightModePreview);
        toolbar = findViewById(R.id.settingsToolbar);

    }

    private void addListeners() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.settings), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            Insets displayCutouts = insets.getInsets(WindowInsetsCompat.Type.displayCutout());
            int leftInset = Math.max(systemBars.left, displayCutouts.left);
            int rightInset = Math.max(systemBars.right, displayCutouts.right);
            findViewById(R.id.settingsNestedScrollView).setPadding(
                    leftInset,
                    findViewById(R.id.settingsNestedScrollView).getPaddingTop(),
                    rightInset,
                    findViewById(R.id.settingsNestedScrollView).getPaddingBottom());
            toolbar.setPadding(
                    leftInset,
                    toolbar.getPaddingTop(),
                    rightInset,
                    toolbar.getPaddingBottom());
            return insets;
        });
        llLongPressAction.setOnClickListener(v -> {

            AlertDialog dialog = new MaterialAlertDialogBuilder(SettingsActivity.this)
                    .setTitle(R.string.long_press_action)
                    .setSingleChoiceItems(R.array.longPressActionOptions, preferences.getInt(PREFERENCES_SETTINGS_LONG_PRESS_ACTON, 0), (dialog1, which) -> {
                        textViewLongPressAction.setText(getResources().getStringArray(R.array.longPressActionOptions)[which]);
                        preferences.edit().putInt(PREFERENCES_SETTINGS_LONG_PRESS_ACTON, which).apply();
                        dialog1.dismiss();
                    })
                    .create();
            dialog.show();
        });
        llQuickInfo.setOnClickListener(v -> {

            AlertDialog dialog = new MaterialAlertDialogBuilder(SettingsActivity.this)
                    .setTitle(R.string.quick_info)
                    .setSingleChoiceItems(R.array.quickInfoOptions, preferences.getInt(PREFERENCES_SETTINGS_QUICK_INFO, 1), (dialog12, which) -> {
                        textViewQuickInfo.setText(getResources().getStringArray(R.array.quickInfoOptions)[which]);
                        preferences.edit().putInt(PREFERENCES_SETTINGS_QUICK_INFO, which).apply();
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
                if (preferences.getString(PREFERENCES_SETTINGS_LANGUAGE, "").equals("en")) {
                    selected = 1;
                } else if (preferences.getString(PREFERENCES_SETTINGS_LANGUAGE, "").equals("fa")) {
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
                            preferences.edit().putString(PREFERENCES_SETTINGS_LANGUAGE, locales[which]).apply();
                            dialog13.dismiss();
                            startActivity(new Intent(getApplicationContext(), MainActivity.class).setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
                        }
                    })
                    .create();
            dialog.show();
        });
        llNightMode.setOnClickListener(v -> {
            int selected = 0;

            if (preferences.getInt(PREFERENCES_SETTINGS_NIGHT_MODE, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM) == AppCompatDelegate.MODE_NIGHT_YES) {
                selected = 1;
            } else if (preferences.getInt(PREFERENCES_SETTINGS_NIGHT_MODE, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM) == AppCompatDelegate.MODE_NIGHT_NO) {
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

                        preferences.edit().putInt(PREFERENCES_SETTINGS_NIGHT_MODE, mode).apply();
                        AppCompatDelegate.setDefaultNightMode(mode);
                        dialog14.dismiss();
                    })
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
                Log.w(TAG, "Not Found Error: The activity looking for is not found! Details: " + e);
            }
        });
        llAbout.setOnClickListener(v -> startActivity(new Intent(getApplicationContext(), AboutActivity.class)));
    }

    private void loadSettings() {
        textViewLongPressAction.setText(getResources().getStringArray(R.array.longPressActionOptions)[preferences.getInt(PREFERENCES_SETTINGS_LONG_PRESS_ACTON, 0)]);
        textViewQuickInfo.setText(getResources().getStringArray(R.array.quickInfoOptions)[preferences.getInt(PREFERENCES_SETTINGS_QUICK_INFO, 1)]);

        int lang = 0;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            LocaleList currentLocales = getSystemService(LocaleManager.class).getApplicationLocales(getPackageName());
            if ((currentLocales.get(0) + "").toLowerCase().contains("en")) {
                lang = 1;
            } else if ((currentLocales.get(0) + "").toLowerCase().contains("fa")) {
                lang = 2;
            }
        } else {
            if (preferences.getString(PREFERENCES_SETTINGS_LANGUAGE, "").equals("en")) {
                lang = 1;
            } else if (preferences.getString(PREFERENCES_SETTINGS_LANGUAGE, "").equals("fa")) {
                lang = 2;
            }
        }
        textViewLanguage.setText(getResources().getStringArray(R.array.languageOptions)[lang]);

        int night = 0;

        if (preferences.getInt(PREFERENCES_SETTINGS_NIGHT_MODE, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM) == AppCompatDelegate.MODE_NIGHT_YES) {
            night = 1;
        } else if (preferences.getInt(PREFERENCES_SETTINGS_NIGHT_MODE, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM) == AppCompatDelegate.MODE_NIGHT_NO) {
            night = 2;
        }
        textViewNightMode.setText(getResources().getStringArray(R.array.nightModeOptions)[night]);

        toolbar.setNavigationOnClickListener((View v) -> finish());
    }
}