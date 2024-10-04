package com.ramapps.apkshare;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.view.menu.MenuBuilder;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.search.SearchView;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

public class MainActivity extends AppCompatActivity {

    private SearchView searchView;
    private RecyclerView recyclerView, recyclerViewSearchResults;
    private TextView textViewSearchResultCount;

    private List<PackageInfo> installedPackagesInfo, searchedPackagesInfo;
    private List<Boolean> selectionTracker, selectionTrackerForSearchResults;
    private SharedPreferences preferences;
    private Parcelable recyclerViewState;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Getting language settings and set it for android 12 and below. In the Android 13+ this setting automatically (App language feature).
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            String langCode = getSharedPreferences(GlobalVariables.PREFERENCES_SETTINGS, MODE_PRIVATE).getString(GlobalVariables.PREFERENCES_SETTINGS_LANGUAGE, "");
            Configuration configuration = getResources().getConfiguration();
            DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
            if (langCode.isEmpty()) {
                configuration.setLocale(Locale.getDefault());
            } else {
                configuration.setLocale(new Locale(langCode));
            }
            getResources().updateConfiguration(configuration, displayMetrics);
        }

        // Load theme settings and set that for activity.
        if (getSharedPreferences(GlobalVariables.PREFERENCES_SETTINGS, MODE_PRIVATE).getInt(GlobalVariables.PREFERENCES_SETTINGS_THEME, 0) == 0 && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            setTheme(R.style.dynamic_color_theme);
        } else {
            setTheme(R.style.AppTheme);
        }

        // Load night mode settings and set that for app.
        AppCompatDelegate.setDefaultNightMode(getSharedPreferences(GlobalVariables.PREFERENCES_SETTINGS, MODE_PRIVATE).getInt(GlobalVariables.PREFERENCES_SETTINGS_NIGHT_MODE, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM));

        EdgeToEdge.enable(this);

        setContentView(R.layout.activity_main);
        init();
        addListeners();
        setSupportActionBar(findViewById(R.id.mainSearchBar));

        // Check for reshare shortcut ond share cached apk files
        if (Objects.equals(getIntent().getAction(), GlobalVariables.ACTION_RESHARE)) {
            Utils.shareCachedApks(this);
        }
    }

    private void addListeners() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            GlobalVariables.systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            GlobalVariables.displayCutouts = insets.getInsets(WindowInsetsCompat.Type.displayCutout());
            GlobalVariables.imeInsets = insets.getInsets(WindowInsetsCompat.Type.ime());

            findViewById(R.id.mainAppBarLayout).setPadding(
                    GlobalVariables.displayCutouts.left,
                    findViewById(R.id.mainAppBarLayout).getPaddingTop(),
                    GlobalVariables.displayCutouts.right,
                    findViewById(R.id.mainAppBarLayout).getPaddingBottom());

            recyclerView.setPadding(
                    GlobalVariables.displayCutouts.left,
                    recyclerView.getPaddingTop(),
                    GlobalVariables.displayCutouts.right,
                    recyclerView.getPaddingBottom());

            searchView.setPadding(
                    searchView.getPaddingLeft(),
                    searchView.getPaddingTop(),
                    searchView.getPaddingRight(),
                    GlobalVariables.imeInsets.bottom);

            RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) GlobalVariables.fabSend.getLayoutParams();
            layoutParams.bottomMargin = layoutParams.bottomMargin + GlobalVariables.systemBars.bottom;


            return insets;
        });

        GlobalVariables.fabSend.setOnClickListener(v -> {
            File cachedApksDir = new File(getCacheDir() + "/ApkFiles/");
            Utils.deleteRecursive(cachedApksDir);
            for (int i = 0; i < installedPackagesInfo.size(); i++) {
                if (selectionTracker.get(i)) {
                    File file = new File(installedPackagesInfo.get(i).applicationInfo.publicSourceDir);
                    Utils.copyFile(file, new File(cachedApksDir.getPath() + "/" + getPackageManager().getApplicationLabel(installedPackagesInfo.get(i).applicationInfo) + ".apk"));
                }
            }
            Utils.shareCachedApks(MainActivity.this);
        });

        GlobalVariables.fabSendSearchView.setOnClickListener(v -> {
            File cachedApksDir = new File(getCacheDir() + "/ApkFiles/");
            Utils.deleteRecursive(cachedApksDir);
            for (int i = 0; i < searchedPackagesInfo.size(); i++) {
                if (selectionTrackerForSearchResults.get(i)) {
                    File file = new File(searchedPackagesInfo.get(i).applicationInfo.publicSourceDir);
                    Utils.copyFile(file, new File(cachedApksDir.getPath() + "/" + getPackageManager().getApplicationLabel(searchedPackagesInfo.get(i).applicationInfo) + ".apk"));
                }
            }
            Utils.shareCachedApks(MainActivity.this);
        });

        // Search when enter key press
        searchView.getEditText().setOnEditorActionListener((v, actionId, event) -> {
            if (!v.getText().toString().isEmpty()) {
                searchedPackagesInfo = searchForApps(v.getText().toString());
                selectionTrackerForSearchResults = new ArrayList<>();
                for (PackageInfo ignored : searchedPackagesInfo) {
                    selectionTrackerForSearchResults.add(false);
                }

                showAppsInRecyclerView(recyclerViewSearchResults, searchedPackagesInfo, selectionTrackerForSearchResults);

                if (!searchedPackagesInfo.isEmpty()) {
                    textViewSearchResultCount.setText(getResources().getQuantityString(R.plurals.search_result_count, searchedPackagesInfo.size(), v.getText(), searchedPackagesInfo.size()));
                } else {
                    textViewSearchResultCount.setText(getResources().getQuantityString(R.plurals.msg_not_found, searchedPackagesInfo.size(), v.getText()));
                }
                textViewSearchResultCount.setVisibility(View.VISIBLE);
            }
            return true;
        });

        // Clear search results when search keyword change
        searchView.getEditText().addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {/* Implementing this method not needed */}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {/* Implementing this method not needed */}

            @Override
            public void afterTextChanged(Editable s) {
                searchedPackagesInfo = new ArrayList<>();
                showAppsInRecyclerView(recyclerViewSearchResults, searchedPackagesInfo, selectionTrackerForSearchResults);
            }
        });

        searchView.addTransitionListener((searchView, transitionState, transitionState1) -> {
            if (transitionState == SearchView.TransitionState.SHOWN) {
                searchedPackagesInfo = new ArrayList<>();
                selectionTrackerForSearchResults = new ArrayList<>();
                showAppsInRecyclerView(recyclerViewSearchResults, searchedPackagesInfo, selectionTrackerForSearchResults);
                GlobalVariables.fabSendSearchView.hide();
                textViewSearchResultCount.setVisibility(View.GONE);
            }
        });

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (searchView.getCurrentTransitionState() == SearchView.TransitionState.SHOWN) {
                    searchView.hide();
                } else {
                    finish();
                }
            }
        });
    }

    private List<PackageInfo> searchForApps(String keyword) {
        List<PackageInfo> results = new ArrayList<>();

        for (PackageInfo packageInfo : installedPackagesInfo) {
            String appName = getPackageManager().getApplicationLabel(packageInfo.applicationInfo).toString();
            if (appName.toLowerCase().contains(keyword.toLowerCase()))
                results.add(packageInfo);
        }

        return results;
    }

    @Override
    protected void onPause() {
        super.onPause();
        recyclerViewState = Objects.requireNonNull(recyclerView.getLayoutManager()).onSaveInstanceState();
    }

    @Override
    protected void onResume() {
        super.onResume();

        getInstalledApps();
        sortPackageInfoList();
        showAppsInRecyclerView(recyclerView, installedPackagesInfo, selectionTracker);
        GlobalVariables.fabSend.hide();
        Objects.requireNonNull(recyclerView.getLayoutManager()).onRestoreInstanceState(recyclerViewState);
    }

    private void sortPackageInfoList() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            boolean reverseSort = preferences.getBoolean(GlobalVariables.PREFERENCES_SETTINGS_REVERSE_SORT, false);

            installedPackagesInfo.sort((o1, o2) -> {
                int sortType = preferences.getInt(GlobalVariables.PREFERENCES_SETTINGS_SORT_BY, GlobalVariables.FLAG_SORT_BY_NAME);
                if (sortType == GlobalVariables.FLAG_SORT_BY_NAME) {
                    String name1 = getPackageManager().getApplicationLabel(o1.applicationInfo) + "";
                    String name2 = getPackageManager().getApplicationLabel(o2.applicationInfo) + "";
                    return reverseSort ? name2.compareTo(name1) : name1.compareTo(name2);
                } else if (sortType == GlobalVariables.FLAG_SORT_BY_INSTALL_DATE) {
                    String date1 = o1.firstInstallTime + "";
                    String date2 = o2.firstInstallTime + "";
                    return reverseSort ? date2.compareTo(date1) : date1.compareTo(date2);
                } else if (sortType == GlobalVariables.FLAG_SORT_BY_SIZE) {
                    String size1 = new File(o1.applicationInfo.sourceDir).length() + "";
                    String size2 = new File(o2.applicationInfo.sourceDir).length() + "";
                    return reverseSort ? size2.compareTo(size1) : size1.compareTo(size2);
                }
                return 0;
            });
        }
    }

    private void showAppsInRecyclerView(RecyclerView recyclerView, List<PackageInfo> installedPackagesInfo, List<Boolean> selectionTracker) {
        MainRecyclerViewAdapter adapter = new MainRecyclerViewAdapter(this, installedPackagesInfo, selectionTracker);
        recyclerView.setLayoutManager(new GridLayoutManager(this, preferences.getInt(GlobalVariables.PREFERENCES_SETTINGS_COLUMN_COUNT, 2) + 1));
        recyclerView.setAdapter(adapter);
    }

    private void getInstalledApps() {
        installedPackagesInfo = new ArrayList<>();
        selectionTracker = new ArrayList<>();
        for (PackageInfo pi : getPackageManager().getInstalledPackages(PackageManager.GET_META_DATA)) {
            if ((pi.applicationInfo.flags & (ApplicationInfo.FLAG_SYSTEM | ApplicationInfo.FLAG_UPDATED_SYSTEM_APP)) <= 0) {
                installedPackagesInfo.add(pi);
                selectionTracker.add(false);
            }
        }
    }

    private void init() {
        preferences = getSharedPreferences(GlobalVariables.PREFERENCES_SETTINGS, MODE_PRIVATE);

        GlobalVariables.fabSendSearchView = findViewById(R.id.mainSearchViewFloatingActionBarSend);
        GlobalVariables.fabSend = findViewById(R.id.mainFloatingActionBarSend);

        searchView = findViewById(R.id.mainSearchView);
        recyclerView = findViewById(R.id.mainRecyclerView);
        recyclerViewSearchResults = findViewById(R.id.mainRecyclerViewSearchResults);
        textViewSearchResultCount = findViewById(R.id.mainSearchViewTextViewResultCount);
    }

    @SuppressLint("RestrictedApi")
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        ((MenuBuilder) menu).setOptionalIconsVisible(true);
        if (preferences.getInt(GlobalVariables.PREFERENCES_SETTINGS_COLUMN_COUNT, 2) + 1 == 1) {
            menu.getItem(2).setIcon(R.drawable.ic_list);
        } else {
            menu.getItem(2).setIcon(R.drawable.ic_grid_view);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.mainMenuItemSort) {
            AtomicInteger choice = new AtomicInteger(preferences.getInt(GlobalVariables.PREFERENCES_SETTINGS_SORT_BY, GlobalVariables.FLAG_SORT_BY_NAME));
            CheckBox cbReverseSort = new CheckBox(this);
            cbReverseSort.setText(R.string.reverse_sort);
            cbReverseSort.setChecked(preferences.getBoolean(GlobalVariables.PREFERENCES_SETTINGS_REVERSE_SORT, false));
            AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.sort_by)
                    .setSingleChoiceItems(R.array.sortOptions, preferences.getInt(GlobalVariables.PREFERENCES_SETTINGS_SORT_BY, GlobalVariables.FLAG_SORT_BY_NAME), (dialog1, which) -> choice.set(which))
                    .setPositiveButton(R.string.apply, (dialog13, which) -> {
                        preferences.edit().putInt(GlobalVariables.PREFERENCES_SETTINGS_SORT_BY, choice.get()).apply();
                        preferences.edit().putBoolean(GlobalVariables.PREFERENCES_SETTINGS_REVERSE_SORT, cbReverseSort.isChecked()).apply();
                        sortPackageInfoList();
                        showAppsInRecyclerView(recyclerView, installedPackagesInfo, selectionTracker);
                        dialog13.dismiss();
                    })
                    .setNegativeButton(R.string.cancel, null)
                    .setView(cbReverseSort)
                    .create();
            dialog.show();
        } else if (item.getItemId() == R.id.mainMenuItemType) {
            Toast.makeText(this, getString(R.string.coming_soon), Toast.LENGTH_SHORT).show();
        } else if (item.getItemId() == R.id.mainMenuItemColumnCount) {
            AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.column_count)
                    .setSingleChoiceItems(new CharSequence[]{"1", "2", "3", "4", "5", "6"}, preferences.getInt(GlobalVariables.PREFERENCES_SETTINGS_COLUMN_COUNT, 2), (dialog12, which) -> {
                        preferences.edit().putInt(GlobalVariables.PREFERENCES_SETTINGS_COLUMN_COUNT, which).apply();
                        showAppsInRecyclerView(recyclerView, installedPackagesInfo, selectionTracker);
                        dialog12.dismiss();
                        if (preferences.getInt(GlobalVariables.PREFERENCES_SETTINGS_COLUMN_COUNT, 2) + 1 == 1) {
                            item.setIcon(R.drawable.ic_list);
                        } else {
                            item.setIcon(R.drawable.ic_grid_view);
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