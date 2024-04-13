package com.ramapps.apkshare;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Insets;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.widget.RelativeLayout;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.view.menu.MenuBuilder;
import androidx.core.content.FileProvider;
import androidx.core.view.OnApplyWindowInsetsListener;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    public static final String PREFERENCES_SETTINGS = "Settings";
    public static final String PREFERENCES_SETTINGS_SORT_BY = "Sort by";
    public static final String PREFERENCES_SETTINGS_COLUMN_COUNT = "Column count";
    public static final String PREFERENCES_SETTINGS_LONG_PRESS_ACTON = "Long press action";
    public static final String PREFERENCES_SETTINGS_QUICK_INFO = "Quick info";
    public static final String PREFERENCES_SETTINGS_LANGUAGE = "Language";
    public static final String PREFERENCES_SETTINGS_NIGHT_MODE = "Night mode";
    public static final String PREFERENCES_SETTINGS_THEME = "App theme";

    public static final int FLAG_SORT_BY_NAME = 0;
    public static final int FLAG_SORT_BY_INSTALL_DATE = 1;
    public static final int FLAG_SORT_BY_SIZE = 2;

    private MaterialToolbar toolbar;
    private RecyclerView recyclerView;
    public static FloatingActionButton fabSend;

    private List<PackageInfo> installedPackagesInfo;
    private List<Boolean> selectionTracker;
    private SharedPreferences preferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // set language
        Configuration configuration = getResources().getConfiguration();
        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
        configuration.setLocale(new Locale(getSharedPreferences(PREFERENCES_SETTINGS, MODE_PRIVATE).getString(PREFERENCES_SETTINGS_LANGUAGE, "")));
        getResources().updateConfiguration(configuration, displayMetrics);
        //set app theme
        if (getSharedPreferences(MainActivity.PREFERENCES_SETTINGS, MODE_PRIVATE).getInt(MainActivity.PREFERENCES_SETTINGS_THEME, 0) == 0 && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            setTheme(R.style.dynamic_color_theme);
        } else {
            setTheme(R.style.AppTheme);
        }
        //set nightMode
        AppCompatDelegate.setDefaultNightMode(getSharedPreferences(PREFERENCES_SETTINGS, MODE_PRIVATE).getInt(PREFERENCES_SETTINGS_NIGHT_MODE, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM));
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) getWindow().setNavigationBarColor(0x88FFFFFF);
        init();
        addListeners();
        setSupportActionBar(toolbar);
    }

    private void addListeners() {
        fabSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                List<Uri> pickedAppsUri = new ArrayList<Uri>();
                for(int i = 0; i < installedPackagesInfo.size(); i++) {
                    if(selectionTracker.get(i)) {
                        File file = new File(installedPackagesInfo.get(i).applicationInfo.publicSourceDir);
                        Uri uri = FileProvider.getUriForFile(getApplicationContext(), ".provider", file);
                        pickedAppsUri.add(uri);
                    }
                }
                Intent intent = new Intent();
                intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                intent.setType("text/plain");
                if (pickedAppsUri.size() == 1) {
                    intent.setAction(Intent.ACTION_SEND);
                    intent.putExtra(Intent.EXTRA_STREAM, pickedAppsUri.get(0));
                } else {
                    intent.setAction(Intent.ACTION_SEND_MULTIPLE);
                    intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, (ArrayList<? extends Parcelable>) pickedAppsUri);
                }
                startActivity(intent);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        getInstalledApps();
        sortPackageInfoList();
        showAppsOnScreen();
    }

    private void sortPackageInfoList() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            installedPackagesInfo.sort(new Comparator<PackageInfo>() {
                @Override
                public int compare(PackageInfo o1, PackageInfo o2) {
                    int sortType = preferences.getInt(PREFERENCES_SETTINGS_SORT_BY, FLAG_SORT_BY_NAME);
                    if (sortType == FLAG_SORT_BY_NAME){
                        String name1 = getPackageManager().getApplicationLabel(o1.applicationInfo) + "";
                        String name2 = getPackageManager().getApplicationLabel(o2.applicationInfo) + "";
                        return name1.compareTo(name2);
                    } else if (sortType == FLAG_SORT_BY_INSTALL_DATE){
                        String date1 = o1.firstInstallTime + "";
                        String date2 = o2.firstInstallTime + "";
                        return date1.compareTo(date2);
                    } else if (sortType == FLAG_SORT_BY_SIZE) {
                        String size1 = new File(o1.applicationInfo.sourceDir).length() + "";
                        String size2 = new File(o2.applicationInfo.sourceDir).length() + "";
                        return size1.compareTo(size2);
                    }
                    return 0;
                }
            });
        }
    }

    private void showAppsOnScreen() {
        MainRecyclerViewAdapter adapter = new MainRecyclerViewAdapter(this, installedPackagesInfo, selectionTracker);
        recyclerView.setLayoutManager(new GridLayoutManager(this, preferences.getInt(PREFERENCES_SETTINGS_COLUMN_COUNT, 2) + 1));
        recyclerView.setAdapter(adapter);
    }

    private void getInstalledApps() {
        installedPackagesInfo = new ArrayList<PackageInfo>();
        selectionTracker = new ArrayList<Boolean>();
        for (PackageInfo pi : getPackageManager().getInstalledPackages(PackageManager.GET_META_DATA)) {
            if ((pi.applicationInfo.flags & (ApplicationInfo.FLAG_SYSTEM | ApplicationInfo.FLAG_UPDATED_SYSTEM_APP)) <= 0){
                installedPackagesInfo.add(pi);
                selectionTracker.add(false);
            }
        }
    }

    private void init() {
        preferences = getSharedPreferences(PREFERENCES_SETTINGS, MODE_PRIVATE);

        toolbar = findViewById(R.id.mainToolbar);
        recyclerView = findViewById(R.id.mainRecyclerView);
        fabSend = findViewById(R.id.mainFloatingActionBarSend);
        fabSend.setOnApplyWindowInsetsListener(new View.OnApplyWindowInsetsListener() {
            @NonNull
            @Override
            public WindowInsets onApplyWindowInsets(@NonNull View v, @NonNull WindowInsets insets) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                    RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) v.getLayoutParams();
                    layoutParams.bottomMargin += systemBars.bottom;
                }
                return insets;
            }
        });
    }

    @SuppressLint("RestrictedApi")
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        ((MenuBuilder) menu).setOptionalIconsVisible(true);
        if (preferences.getInt(PREFERENCES_SETTINGS_COLUMN_COUNT, 2) + 1 == 1) {
            menu.getItem(2).setIcon(R.drawable.ic_list);
        } else {
            menu.getItem(2).setIcon(R.drawable.ic_grid_view);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.mainMenuItemSort) {
            AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.sort_by)
                    .setSingleChoiceItems(R.array.sortOptions, preferences.getInt(PREFERENCES_SETTINGS_SORT_BY, FLAG_SORT_BY_NAME), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            preferences.edit().putInt(PREFERENCES_SETTINGS_SORT_BY, which).apply();
                            sortPackageInfoList();
                            showAppsOnScreen();
                            dialog.dismiss();
                        }
                    })
                    .create();
            dialog.show();
        } else if (item.getItemId() == R.id.mainMenuItemType) {
            //TODO: implement here.
        } else if (item.getItemId() == R.id.mainMenuItemColumnCount) {
            AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.column_count)
                    .setSingleChoiceItems(new CharSequence[]{"1", "2", "3", "4", "5", "6"}, preferences.getInt(PREFERENCES_SETTINGS_COLUMN_COUNT, 0), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            preferences.edit().putInt(PREFERENCES_SETTINGS_COLUMN_COUNT, which).apply();
                            recyclerView.setLayoutManager(new GridLayoutManager(getApplicationContext(), which + 1));
                            recyclerView.setAdapter(new MainRecyclerViewAdapter(getApplicationContext(), installedPackagesInfo, selectionTracker));
                            dialog.dismiss();
                            if (preferences.getInt(PREFERENCES_SETTINGS_COLUMN_COUNT, 2) + 1 == 1) {
                                item.setIcon(R.drawable.ic_list);
                            } else {
                                item.setIcon(R.drawable.ic_grid_view);
                            }
                        }
                    })
                    .create();
            dialog.show();
        } else if (item.getItemId() == R.id.mainMenuItemSettings) {
            startActivity(new Intent(this, SettingsActivity.class));
        }
        return true;
    }
}