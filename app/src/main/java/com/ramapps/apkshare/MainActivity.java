package com.ramapps.apkshare;

import static com.ramapps.apkshare.GlobalVariables.*;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Parcelable;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
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
import androidx.appcompat.view.menu.MenuBuilder;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.loadingindicator.LoadingIndicator;
import com.google.android.material.search.SearchBar;
import com.google.android.material.search.SearchView;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class MainActivity extends AppCompatActivity {
    public static final String TAG = "MainActivity";

    private SearchView searchView;
    private RecyclerView recyclerView, recyclerViewSearchResults;
    public static FloatingActionButton fabSend, fabSendSearchView;
    private TextView textViewSearchResultCount;
    private LoadingIndicator loadingIndicator;

    private List<AndroidPackageSimpleModel> installedPackagesList, searchedPackagesInfo;
    private SharedPreferences preferences;

    private Parcelable recyclerViewState;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        initViews();
        initReceivers();
        setSupportActionBar(findViewById(R.id.mainSearchBar));
        addListeners();
        loadPackagesListAsync();
        if (Objects.equals(getIntent().getAction(), Utils.ACTION_RESHARE)) {
            Utils.shareCachedApks(this);
        }
    }

    private void initViews() {
        // TODO: Move preferences into the Utils class.
        preferences = getSharedPreferences(PREFERENCES_SETTINGS, MODE_PRIVATE);

        searchView = findViewById(R.id.mainSearchView);
        recyclerView = findViewById(R.id.mainRecyclerView);
        recyclerViewSearchResults = findViewById(R.id.mainRecyclerViewSearchResults);
        fabSendSearchView = findViewById(R.id.mainSearchViewFloatingActionBarSend);
        fabSend = findViewById(R.id.mainFloatingActionBarSend);
        textViewSearchResultCount = findViewById(R.id.mainSearchViewTextViewResultCount);
        loadingIndicator = findViewById(R.id.mainLoadingIndicator);

        // Set FAB bottom margin
        int fabBottomMargin = (int) (24 * getResources().getDisplayMetrics().density);
        @SuppressLint({"InternalInsetResource", "DiscouragedApi"}) int navigationBarHeightId = getResources().getIdentifier("navigation_bar_height", "dimen", "android");
        int navigationBarHeight = navigationBarHeightId > 0 ? getResources().getDimensionPixelOffset(navigationBarHeightId) : 0;
        RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) fabSend.getLayoutParams();
        layoutParams.bottomMargin = fabBottomMargin + navigationBarHeight;
    }

    private void initReceivers() {
        // Initialize BroadcastReceiver for the notice when a package got changed.
        IntentFilter packageChangesIntentFilter = new IntentFilter();
        packageChangesIntentFilter.addAction(Intent.ACTION_PACKAGE_ADDED);
        packageChangesIntentFilter.addAction(Intent.ACTION_PACKAGE_REMOVED);
        packageChangesIntentFilter.addAction(Intent.ACTION_PACKAGE_FULLY_REMOVED);
        packageChangesIntentFilter.addAction(Intent.ACTION_PACKAGE_CHANGED);
        packageChangesIntentFilter.addDataScheme("package");
        BroadcastReceiver packageChangesReceiver = new PackageDatabaseChangedReceiver();
        ContextCompat.registerReceiver(MainActivity.this, packageChangesReceiver, packageChangesIntentFilter, ContextCompat.RECEIVER_EXPORTED);
    }

    private void addListeners() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                Insets displayCutouts = insets.getInsets(WindowInsetsCompat.Type.displayCutout());
                Insets imeInsets = insets.getInsets(WindowInsetsCompat.Type.ime());

                int leftInset = Math.max(systemBars.left, displayCutouts.left);
                int rightInset = Math.max(systemBars.right, displayCutouts.right);

                findViewById(R.id.mainAppBarLayout).setPadding(
                        leftInset,
                        findViewById(R.id.mainAppBarLayout).getPaddingTop(),
                        rightInset,
                        findViewById(R.id.mainAppBarLayout).getPaddingBottom());
                recyclerView.setPadding(
                        leftInset,
                        recyclerView.getPaddingTop(),
                        rightInset,
                        recyclerView.getPaddingBottom());
                searchView.setPadding(searchView.getPaddingLeft(), searchView.getPaddingTop(), searchView.getPaddingRight(), imeInsets.bottom);
                return insets;
            });
        fabSend.setOnClickListener(v -> {
            MainRecyclerViewAdapter adapter = (MainRecyclerViewAdapter) recyclerView.getAdapter();
            if (adapter == null) {
                Log.e(TAG, "The recycler view provide null as its adapter!");
                return;
            }
            shareSelectedPackages(adapter.getSelectedItems());
        });

        fabSendSearchView.setOnClickListener(v -> {
            MainRecyclerViewAdapter adapter = (MainRecyclerViewAdapter) recyclerViewSearchResults.getAdapter();
            if (adapter == null) {
                Log.e(TAG, "The recycler view (At search view section) provide null as its adapter!");
                return;
            }
            shareSelectedPackages(adapter.getSelectedItems());
        });

        searchView.getEditText().addTextChangedListener(new TextWatcher() {

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                Runnable doSearchingRunnable = new Runnable() {
                    @Override
                    public void run() {
                        String keyword = s.toString();
                        if (keyword == null) {
                            Log.e(TAG, "NullPointerError: The search keyword is null!");
                            return;
                        }
                        if (keyword.trim().equals("")) {
                            Log.w(TAG, "The search keyword is empty (the search field was cleared!).");
                            recyclerViewSearchResults.setAdapter(null);
                            return;
                        }
                        MainRecyclerViewAdapter searchResultAdapter = new MainRecyclerViewAdapter(MainActivity.this, searchForApps(keyword));
                        recyclerViewSearchResults.setLayoutManager(new GridLayoutManager(MainActivity.this, preferences.getInt(PREFERENCES_SETTINGS_COLUMN_COUNT, 2) + 1));
                        recyclerViewSearchResults.setAdapter(searchResultAdapter);
                    }
                };
                Handler waitForEditHandler = new Handler();
                waitForEditHandler.removeCallbacks(doSearchingRunnable);
                waitForEditHandler.postDelayed(doSearchingRunnable, 300);
            }
        });

        searchView.addTransitionListener((@NonNull SearchView searchView, @NonNull SearchView.TransitionState transitionState, @NonNull SearchView.TransitionState transitionState1) -> {
                if (transitionState == SearchView.TransitionState.SHOWN) {
                    searchedPackagesInfo = new ArrayList<>();
                    MainRecyclerViewAdapter adapter = new MainRecyclerViewAdapter(MainActivity.this, searchedPackagesInfo);
                    recyclerViewSearchResults.setLayoutManager(new GridLayoutManager(MainActivity.this, preferences.getInt(PREFERENCES_SETTINGS_COLUMN_COUNT, 2) + 1));
                    recyclerViewSearchResults.setAdapter(adapter);
                    fabSendSearchView.hide();
                    textViewSearchResultCount.setVisibility(View.GONE);
                }
        });

        getOnBackPressedDispatcher().addCallback(new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (searchView.getCurrentTransitionState() == SearchView.TransitionState.SHOWN) {
                    searchView.hide();
                } else if (!((MainRecyclerViewAdapter) recyclerView.getAdapter()).getSelectedItems().isEmpty()) {
                    recyclerView.setAdapter(new MainRecyclerViewAdapter(MainActivity.this, installedPackagesList));
                } else {
                    finish();
                }
            }
        });

    }

    private void shareSelectedPackages(List<PackageInfo> selectedPackages) {
        File cachedApksDir = new File(getCacheDir() + "/ApkFiles/");
        Utils.deleteRecursive(cachedApksDir);
        Utils.copyFilesAsyncOnUi(
                MainActivity.this,
                getApkFilesFromPackageInfoList(selectedPackages),
                getApkNamedFilesFromPackageInfoList(selectedPackages),
                cachedApksDir,
                () -> Utils.shareCachedApks(MainActivity.this)
        );
    }

    private List<File> getApkFilesFromPackageInfoList(List<PackageInfo> packageInfos) {
        List<File> apkFiles = new ArrayList<>();
        for (PackageInfo packageInfo : packageInfos) {
            if (packageInfo.applicationInfo == null) {
                Log.e(TAG, "This package info provides a null application info object!");
                continue;
            }
            apkFiles.add(new File(packageInfo.applicationInfo.publicSourceDir));
        }
        return apkFiles;
    }

    private List<String> getApkNamedFilesFromPackageInfoList(List<PackageInfo> packageInfos) {
        List<String> apkNamedFiles = new ArrayList<>();
        for (PackageInfo packageInfo : packageInfos) {
            if (packageInfo.applicationInfo == null) {
                Log.e(TAG, "This package info provides a null application info object!");
                continue;
            }
            apkNamedFiles.add(getPackageManager().getApplicationLabel(packageInfo.applicationInfo) + ".apk");
        }
        return apkNamedFiles;
    }

    private List<AndroidPackageSimpleModel> searchForApps(String keyword) {
        List<AndroidPackageSimpleModel> results = new ArrayList<>();
        for (AndroidPackageSimpleModel androidPackageSimpleModel : installedPackagesList) {
            if (Objects.isNull(androidPackageSimpleModel.getApplicationInfo())) {
                Log.e(TAG, "Null ApplicationInfo object error for: " + androidPackageSimpleModel.getPackageName());
                continue;
            }
            if (androidPackageSimpleModel.getLabel().toLowerCase().contains(keyword.toLowerCase()))
                results.add(androidPackageSimpleModel);
        }
        return results;
    }

    private void loadPackagesListAsync() {
        Executor executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());
        recyclerView.setAdapter(null);
        loadingIndicator.setVisibility(View.VISIBLE);
        fabSend.hide();
        SearchBar searchBar = findViewById(R.id.mainSearchBar);
        searchBar.setEnabled(false);
        searchBar.setHint(R.string.preparing_packages_list);
        executor.execute(() -> {
            getInstalledApps();
            sortPackageInfoList();
//          showAppsOnScreen(); another implementation in below:
            MainRecyclerViewAdapter adapter = new MainRecyclerViewAdapter(MainActivity.this, installedPackagesList);
            handler.post(() -> {
                recyclerView.setLayoutManager(new GridLayoutManager(this, preferences.getInt(PREFERENCES_SETTINGS_COLUMN_COUNT, 2) + 1));
                recyclerView.setAdapter(adapter);
                loadingIndicator.setVisibility(View.GONE);
                searchBar.setEnabled(true);
                searchBar.setHint(R.string.search_for_apps);
            });
        });
    }

    private void getInstalledApps() {
        installedPackagesList = new ArrayList<>();
        for (PackageInfo pi : getPackageManager().getInstalledPackages(PackageManager.GET_META_DATA)) {
            try {
                if ((pi.applicationInfo.flags & (ApplicationInfo.FLAG_SYSTEM | ApplicationInfo.FLAG_UPDATED_SYSTEM_APP)) <= 0) {
                    AndroidPackageSimpleModel androidPackageSimpleModel = new AndroidPackageSimpleModel(this);
                    androidPackageSimpleModel.setPackageName(pi.packageName);
                    androidPackageSimpleModel.setLabel(getPackageManager().getApplicationLabel(pi.applicationInfo).toString());
                    androidPackageSimpleModel.setIcon(getPackageManager().getApplicationIcon(pi.applicationInfo));
                    installedPackagesList.add(androidPackageSimpleModel);
                }
            } catch (NullPointerException e) {
                Log.e(TAG, "Null pointer error occurred when getting an ApplicationInfo object for: " + pi.packageName);
            }
        }
    }

    private void sortPackageInfoList() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            boolean reverseSort = preferences.getBoolean(PREFERENCES_SETTINGS_REVERSE_SORT, false);
            installedPackagesList.sort((o1, o2) -> {
                int sortType = preferences.getInt(PREFERENCES_SETTINGS_SORT_BY, FLAG_SORT_BY_NAME);
                if (sortType == FLAG_SORT_BY_NAME) {
                    return reverseSort ? o2.getLabel().compareTo(o1.getLabel()) : o1.getLabel().compareTo(o2.getLabel());
                } else if (sortType == FLAG_SORT_BY_INSTALL_DATE) {
                    String date1 = o1.getPackageInfo().firstInstallTime + "";
                    String date2 = o2.getPackageInfo().firstInstallTime + "";
                    return reverseSort ? date2.compareTo(date1) : date1.compareTo(date2);
                } else if (sortType == FLAG_SORT_BY_SIZE) {
                    String size1 = o1.getApkFile().length() + "";
                    String size2 = o2.getApkFile().length() + "";
                    return reverseSort ? size2.compareTo(size1) : size1.compareTo(size2);
                }
                return 0;
            });
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        try {
            Objects.requireNonNull(recyclerView.getLayoutManager()).onRestoreInstanceState(recyclerViewState);
        } catch (NullPointerException e) {
            Log.e(TAG, "Null pointer exception error for saving the state instance for recycler view: " + e);
        } catch (Exception e) {
            Log.e(TAG, "There is occurred an exception error! Details: " + e);
        }
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
            AtomicInteger choice = new AtomicInteger(preferences.getInt(PREFERENCES_SETTINGS_SORT_BY, FLAG_SORT_BY_NAME));
            CheckBox cbReverseSort = new CheckBox(this);
            cbReverseSort.setText(R.string.reverse_sort);
            cbReverseSort.setChecked(preferences.getBoolean(PREFERENCES_SETTINGS_REVERSE_SORT, false));
            AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.sort_by)
                    .setSingleChoiceItems(R.array.sortOptions, preferences.getInt(PREFERENCES_SETTINGS_SORT_BY, FLAG_SORT_BY_NAME), (dialog1, which) -> choice.set(which))
                    .setPositiveButton(R.string.apply, (DialogInterface dia, int which) -> {
                            preferences.edit().putInt(PREFERENCES_SETTINGS_SORT_BY, choice.get()).apply();
                            preferences.edit().putBoolean(PREFERENCES_SETTINGS_REVERSE_SORT, cbReverseSort.isChecked()).apply();
                            sortPackageInfoList();
                            showAppsOnScreen();
                            dia.dismiss();
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
                    .setSingleChoiceItems(new CharSequence[]{"1", "2", "3", "4", "5", "6"}, preferences.getInt(PREFERENCES_SETTINGS_COLUMN_COUNT, 2), (dialog12, which) -> {
                        preferences.edit().putInt(PREFERENCES_SETTINGS_COLUMN_COUNT, which).apply();
                        recyclerView.setLayoutManager(new GridLayoutManager(MainActivity.this, which + 1));
                        recyclerView.setAdapter(new MainRecyclerViewAdapter(MainActivity.this, installedPackagesList));
                        dialog12.dismiss();
                        if (preferences.getInt(PREFERENCES_SETTINGS_COLUMN_COUNT, 2) + 1 == 1) {
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

    private void showAppsOnScreen() {
        MainRecyclerViewAdapter adapter = new MainRecyclerViewAdapter(MainActivity.this, installedPackagesList);
        recyclerView.setLayoutManager(new GridLayoutManager(this, preferences.getInt(PREFERENCES_SETTINGS_COLUMN_COUNT, 2) + 1));
        recyclerView.setAdapter(adapter);
    }

    @Override
    protected void onPause() {
        super.onPause();
        try {
            recyclerViewState = recyclerView.getLayoutManager().onSaveInstanceState();
        } catch (NullPointerException e) {
            Log.e(TAG, "Null pointer exception error for saving the state instance for recycler view: " + e);
        } catch (Exception e) {
            Log.e(TAG, "There is occurred an exception error! Details: " + e);
        }
    }

    class PackageDatabaseChangedReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "PackageDatabaseChangedReceiver was received somethings...");
            if (Objects.isNull(intent)) {
                Log.e(TAG, "The intent provided by onReceive() is null!");
            }
            if (Objects.isNull(intent.getAction()) || Objects.equals(intent.getAction(), "")) {
                Log.e(TAG, "The intent action that provided by onReceive() is null or empty!");
            }
            switch (intent.getAction()) {
                case Intent.ACTION_PACKAGE_ADDED:
                    Log.d(TAG, "A package added: " + intent.getData());
                    loadPackagesListAsync();
                    break;
                case Intent.ACTION_PACKAGE_REMOVED:
                    Log.d(TAG, "A package removed: " + intent.getData());
                    loadPackagesListAsync();
                    break;
                case Intent.ACTION_PACKAGE_FULLY_REMOVED:
                    Log.d(TAG, "A package fully removed: " + intent.getData());
                    loadPackagesListAsync();
                    break;
                case Intent.ACTION_PACKAGE_CHANGED:
                    Log.d(TAG, "A package changed: " + intent.getData());
                    break;
                default:
                    Log.w(TAG, "The provided intent action is not match to any expected cases: " + intent.getAction());
            }
        }
    }
}
