package com.ramapps.apkshare;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
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
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.search.SearchView;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class MainActivity extends AppCompatActivity {

    private SearchView searchView;
    private RecyclerView recyclerView, recyclerViewSearchResults;
    private TextView textViewSearchResultCount;
    private CircularProgressIndicator loadingIndicator;

    private List<PackageInfo> installedPackagesInfo, searchedPackagesInfo;
    private SharedPreferences preferences;
    private Parcelable recyclerViewState;
    private ReceiverPackageAddOrRemove receiverPackageAddOrRemove;

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

        getInstalledAppsAsync async = new getInstalledAppsAsync();
        async.executeOnExecutor(Executors.newSingleThreadExecutor());

        IntentFilter intentFilterPackageAddOrRemove = new IntentFilter();
        intentFilterPackageAddOrRemove.addAction(Intent.ACTION_PACKAGE_ADDED);
        intentFilterPackageAddOrRemove.addAction(Intent.ACTION_PACKAGE_REMOVED);
        intentFilterPackageAddOrRemove.addDataScheme("package");
        receiverPackageAddOrRemove = new ReceiverPackageAddOrRemove();
        registerReceiver(receiverPackageAddOrRemove, intentFilterPackageAddOrRemove);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (receiverPackageAddOrRemove != null) {
            unregisterReceiver(receiverPackageAddOrRemove);
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
            if(GlobalVariables.fabBottomMargin == 0){
                GlobalVariables.fabBottomMargin = layoutParams.bottomMargin + GlobalVariables.systemBars.bottom;
            }
            layoutParams.bottomMargin = GlobalVariables.fabBottomMargin;

            return insets;
        });

        GlobalVariables.fabSend.setOnClickListener(v -> {
            File cachedApksDir = new File(getCacheDir() + "/ApkFiles/");
            Utils.deleteRecursive(cachedApksDir);
            List<File> files = new ArrayList<>();
            List<String> filesName = new ArrayList<>();
            for (int i = 0; i < installedPackagesInfo.size(); i++) {
                if (((MainRecyclerViewAdapter) Objects.requireNonNull(recyclerView.getAdapter())).isSelected(i)) {
                    File file = new File(Objects.requireNonNull(installedPackagesInfo.get(i).applicationInfo).publicSourceDir);
                    files.add(file);
                    filesName.add(getPackageManager().getApplicationLabel(Objects.requireNonNull(installedPackagesInfo.get(i).applicationInfo)).toString());
                }
            }
            Utils.CopyFilesAsync copyFilesAsync = new Utils.CopyFilesAsync(MainActivity.this, files, filesName, new File(getCacheDir() + "/ApkFiles/"), new Runnable() {
                @Override
                public void run() {
                    Log.d("Reza", "This code was run!");
                    Utils.shareCachedApks(MainActivity.this);
                }
            });
            copyFilesAsync.execute();
        });

        GlobalVariables.fabSendSearchView.setOnClickListener(v -> {
            File cachedApksDir = new File(getCacheDir() + "/ApkFiles/");
            Utils.deleteRecursive(cachedApksDir);
            List<File> files = new ArrayList<>();
            List<String> filesName = new ArrayList<>();
            for (int i = 0; i < searchedPackagesInfo.size(); i++) {
                if (((MainRecyclerViewAdapter) Objects.requireNonNull(recyclerViewSearchResults.getAdapter())).isSelected(i)) {
                    File file = new File(Objects.requireNonNull(searchedPackagesInfo.get(i).applicationInfo).publicSourceDir);
                    files.add(file);
                    filesName.add(getPackageManager().getApplicationLabel(Objects.requireNonNull(installedPackagesInfo.get(i).applicationInfo)).toString());
                }
            }
            Utils.CopyFilesAsync copyFilesAsync = new Utils.CopyFilesAsync(MainActivity.this, files, filesName, new File(getCacheDir() + "/ApkFiles/"), new Runnable() {
                @Override
                public void run() {
                    Log.d("Reza", "This code was run!");
                    Utils.shareCachedApks(MainActivity.this);
                }
            });
            copyFilesAsync.execute();
        });

        // Clear search results when search keyword change
        searchView.getEditText().addTextChangedListener(new TextWatcher() {
            final Handler handler = new Handler();
            final Runnable runnable = new Runnable() {
                @Override
                public void run() {
                    String s = searchView.getText().toString();
                    if (!s.isEmpty()) {
                        searchedPackagesInfo = searchForApps(s);
                        showAppsInRecyclerView(recyclerViewSearchResults, searchedPackagesInfo);
                        ((MainRecyclerViewAdapter) Objects.requireNonNull(recyclerViewSearchResults.getAdapter())).setSearchKeyword(searchView.getText().toString());

                        if (!searchedPackagesInfo.isEmpty()) {
                            textViewSearchResultCount.setText(getResources().getQuantityString(R.plurals.search_result_count, searchedPackagesInfo.size(), s, searchedPackagesInfo.size()));
                        } else {
                            textViewSearchResultCount.setText(getResources().getQuantityString(R.plurals.msg_not_found, searchedPackagesInfo.size(), s));
                        }
                        textViewSearchResultCount.setVisibility(View.VISIBLE);
                    } else {
                        searchedPackagesInfo = new ArrayList<>();
                        showAppsInRecyclerView(recyclerViewSearchResults, searchedPackagesInfo);
                        textViewSearchResultCount.setVisibility(View.GONE);
                    }
                }
            };

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {/* Implementing this method not needed */}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {/* Implementing this method not needed */}

            @Override
            public void afterTextChanged(Editable s) {
                handler.removeCallbacks(runnable);
                handler.postDelayed(runnable, 500);
            }
        });

        searchView.addTransitionListener((searchView, transitionState, transitionState1) -> {
            if (transitionState == SearchView.TransitionState.SHOWN) {
                searchedPackagesInfo = new ArrayList<>();
                showAppsInRecyclerView(recyclerViewSearchResults, searchedPackagesInfo);
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
            String appName = getPackageManager().getApplicationLabel(Objects.requireNonNull(packageInfo.applicationInfo)).toString();
            if (appName.toLowerCase().contains(keyword.toLowerCase()))
                results.add(packageInfo);
        }

        return results;
    }

    @Override
    protected void onPause() {
        super.onPause();
        try {
            recyclerViewState = Objects.requireNonNull(recyclerView.getLayoutManager()).onSaveInstanceState();
        } catch (Exception e) {
            Log.e("MainActivity.onPause", e.toString());
        }
    }

    private void sortPackageInfoList() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            boolean reverseSort = preferences.getBoolean(GlobalVariables.PREFERENCES_SETTINGS_REVERSE_SORT, false);

            installedPackagesInfo.sort((o1, o2) -> {
                int sortType = preferences.getInt(GlobalVariables.PREFERENCES_SETTINGS_SORT_BY, GlobalVariables.FLAG_SORT_BY_NAME);
                if (sortType == GlobalVariables.FLAG_SORT_BY_NAME) {
                    String name1 = getPackageManager().getApplicationLabel(Objects.requireNonNull(o1.applicationInfo)) + "";
                    String name2 = getPackageManager().getApplicationLabel(Objects.requireNonNull(o2.applicationInfo)) + "";
                    return reverseSort ? name2.compareTo(name1) : name1.compareTo(name2);
                } else if (sortType == GlobalVariables.FLAG_SORT_BY_INSTALL_DATE) {
                    String date1 = o1.firstInstallTime + "";
                    String date2 = o2.firstInstallTime + "";
                    return reverseSort ? date2.compareTo(date1) : date1.compareTo(date2);
                } else if (sortType == GlobalVariables.FLAG_SORT_BY_SIZE) {
                    assert o1.applicationInfo != null;
                    String size1 = new File(o1.applicationInfo.sourceDir).length() + "";
                    assert o2.applicationInfo != null;
                    String size2 = new File(o2.applicationInfo.sourceDir).length() + "";
                    return reverseSort ? size2.compareTo(size1) : size1.compareTo(size2);
                }
                return 0;
            });
        }
    }

    private void showAppsInRecyclerView(RecyclerView recyclerView, List<PackageInfo> installedPackagesInfo) {
        MainRecyclerViewAdapter adapter = new MainRecyclerViewAdapter(this, installedPackagesInfo);
        recyclerView.setLayoutManager(new GridLayoutManager(this, preferences.getInt(GlobalVariables.PREFERENCES_SETTINGS_COLUMN_COUNT, 2) + 1));
        recyclerView.setAdapter(adapter);
    }

    private void init() {
        preferences = getSharedPreferences(GlobalVariables.PREFERENCES_SETTINGS, MODE_PRIVATE);

        GlobalVariables.fabSendSearchView = findViewById(R.id.mainSearchViewFloatingActionBarSend);
        GlobalVariables.fabSend = findViewById(R.id.mainFloatingActionBarSend);

        searchView = findViewById(R.id.mainSearchView);
        recyclerView = findViewById(R.id.mainRecyclerView);
        recyclerViewSearchResults = findViewById(R.id.mainRecyclerViewSearchResults);
        textViewSearchResultCount = findViewById(R.id.mainSearchViewTextViewResultCount);
        loadingIndicator = findViewById(R.id.mainCircularIndicatorLoading);
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
                        showAppsInRecyclerView(recyclerView, installedPackagesInfo);
                        dialog13.dismiss();
                    })
                    .setNegativeButton(R.string.cancel, null)
                    .setView(cbReverseSort)
                    .create();
            Objects.requireNonNull(dialog.getWindow()).getAttributes().gravity = Gravity.BOTTOM;
            dialog.show();
        } else if (item.getItemId() == R.id.mainMenuItemType) {
            Toast.makeText(this, getString(R.string.coming_soon), Toast.LENGTH_SHORT).show();
        } else if (item.getItemId() == R.id.mainMenuItemColumnCount) {
            AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.column_count)
                    .setSingleChoiceItems(new CharSequence[]{"1", "2", "3", "4", "5", "6"}, preferences.getInt(GlobalVariables.PREFERENCES_SETTINGS_COLUMN_COUNT, 2), (dialog12, which) -> {
                        preferences.edit().putInt(GlobalVariables.PREFERENCES_SETTINGS_COLUMN_COUNT, which).apply();
                        showAppsInRecyclerView(recyclerView, installedPackagesInfo);
                        dialog12.dismiss();
                        if (preferences.getInt(GlobalVariables.PREFERENCES_SETTINGS_COLUMN_COUNT, 2) + 1 == 1) {
                            item.setIcon(R.drawable.ic_list);
                        } else {
                            item.setIcon(R.drawable.ic_grid_view);
                        }
                    })
                    .create();
            Objects.requireNonNull(dialog.getWindow()).getAttributes().gravity = Gravity.BOTTOM;
            dialog.show();
        } else if (item.getItemId() == R.id.mainMenuItemSettings) {
            startActivity(new Intent(this, SettingsActivity.class));
        }
        return true;
    }

    private class getInstalledAppsAsync extends AsyncTask<Void, Void, String> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            loadingIndicator.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        }

        @Override
        protected String doInBackground(Void... voids) {
            installedPackagesInfo = new ArrayList<>();
            for (PackageInfo pi : getPackageManager().getInstalledPackages(PackageManager.GET_META_DATA)) {
                if ((pi.applicationInfo.flags & (ApplicationInfo.FLAG_SYSTEM | ApplicationInfo.FLAG_UPDATED_SYSTEM_APP)) <= 0) {
                    installedPackagesInfo.add(pi);
                    String appId = pi.packageName;
                    try {
                        Drawable appIcon = getPackageManager().getApplicationIcon(appId);
                        GlobalVariables.appIcons.put(appId, appIcon);
                    } catch (PackageManager.NameNotFoundException e) {
                        GlobalVariables.appIcons.put(appId, getResources().getDrawable(R.drawable.outline_apk_document_24));
                        Log.e("GetInstalledAppsAsync", "ERROR: Cannot get application icon for package: " + appId + ".");
                    }
                }
            }
            return "Success";
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            sortPackageInfoList();
            showAppsInRecyclerView(recyclerView, installedPackagesInfo);
            GlobalVariables.fabSend.hide();
            loadingIndicator.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
            Objects.requireNonNull(recyclerView.getLayoutManager()).onRestoreInstanceState(recyclerViewState);
        }
    }

    public class ReceiverPackageAddOrRemove extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d("ReceiverPackageChange", intent.getAction() + " | " + intent.getData());
            MainActivity.getInstalledAppsAsync async = new MainActivity.getInstalledAppsAsync();
            async.executeOnExecutor(Executors.newSingleThreadExecutor());
        }
    }
}