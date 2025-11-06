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
import android.view.KeyEvent;
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

    private List<PackageInfo> installedPackagesInfo, searchedPackagesInfo;
    private List<Boolean> selectionTracker, selectionTrackerForSearchResults;
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
            File cachedApksDir = new File(getCacheDir() + "/ApkFiles/");
            Utils.deleteRecursive(cachedApksDir);
            for (int i = 0; i < installedPackagesInfo.size(); i++) {
                if (selectionTracker.get(i)) {
                    try {
                        File file = new File(installedPackagesInfo.get(i).applicationInfo.publicSourceDir);
                        Utils.copyFile(file, new File(cachedApksDir.getPath() + "/" + getPackageManager().getApplicationLabel(installedPackagesInfo.get(i).applicationInfo) + ".apk"));
                    } catch (NullPointerException e) {
                        Log.e(TAG, "There is occurred a null pointer exception error when getting application info object for <" + installedPackagesInfo.get(i) + ">: " + e);
                    }
                }
            }
            Utils.shareCachedApks(MainActivity.this);
        });

        fabSendSearchView.setOnClickListener(v -> {
            File cachedApksDir = new File(getCacheDir() + "/ApkFiles/");
            Utils.deleteRecursive(cachedApksDir);
            for (int i = 0; i < searchedPackagesInfo.size(); i++) {
                if (selectionTrackerForSearchResults.get(i)) {
                    try {
                        File file = new File(installedPackagesInfo.get(i).applicationInfo.publicSourceDir);
                        Utils.copyFile(file, new File(cachedApksDir.getPath() + "/" + getPackageManager().getApplicationLabel(installedPackagesInfo.get(i).applicationInfo) + ".apk"));
                    } catch (NullPointerException e) {
                        Log.e(TAG, "There is occurred a null pointer exception error when getting application info object for <" + installedPackagesInfo.get(i) + ">: " + e);
                    }
                }
            }
            Utils.shareCachedApks(MainActivity.this);
        });

        searchView.getEditText().setOnEditorActionListener((TextView v, int actionId, KeyEvent event) -> {
                if (!v.getText().toString().isEmpty()) {
                    searchedPackagesInfo = searchForApps(v.getText().toString());
                    selectionTrackerForSearchResults = new ArrayList<>();
                    for (PackageInfo info : searchedPackagesInfo) {
                        selectionTrackerForSearchResults.add(false);
                    }
                    MainRecyclerViewAdapter adapter = new MainRecyclerViewAdapter(MainActivity.this, searchedPackagesInfo, selectionTrackerForSearchResults);
                    recyclerViewSearchResults.setLayoutManager(new GridLayoutManager(MainActivity.this, preferences.getInt(PREFERENCES_SETTINGS_COLUMN_COUNT, 2) + 1));
                    recyclerViewSearchResults.setAdapter(adapter);
                    if (adapter.getItemCount() > 0) {
                        textViewSearchResultCount.setText(getResources().getQuantityString(R.plurals.search_result_count, adapter.getItemCount(), v.getText(), adapter.getItemCount()));
                    } else {
                        textViewSearchResultCount.setText(getResources().getQuantityString(R.plurals.msg_not_found, adapter.getItemCount(), v.getText()));
                    }
                    textViewSearchResultCount.setVisibility(View.VISIBLE);
                }
                return true;
        });

        searchView.getEditText().addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                searchedPackagesInfo = new ArrayList<>();
                MainRecyclerViewAdapter adapter = new MainRecyclerViewAdapter(MainActivity.this, searchedPackagesInfo, selectionTrackerForSearchResults);
                recyclerViewSearchResults.setLayoutManager(new GridLayoutManager(MainActivity.this, preferences.getInt(PREFERENCES_SETTINGS_COLUMN_COUNT, 2) + 1));
                recyclerViewSearchResults.setAdapter(adapter);
            }
        });

        searchView.addTransitionListener((@NonNull SearchView searchView, @NonNull SearchView.TransitionState transitionState, @NonNull SearchView.TransitionState transitionState1) -> {
                if (transitionState == SearchView.TransitionState.SHOWN) {
                    searchedPackagesInfo = new ArrayList<>();
                    selectionTrackerForSearchResults = new ArrayList<>();
                    MainRecyclerViewAdapter adapter = new MainRecyclerViewAdapter(MainActivity.this, searchedPackagesInfo, selectionTrackerForSearchResults);
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
                } else {
                    finish();
                }
            }
        });

    }

    private List<PackageInfo> searchForApps(String keyword) {
        List<PackageInfo> results = new ArrayList<>();
        for (PackageInfo packageInfo : installedPackagesInfo) {
            if (Objects.isNull(packageInfo.applicationInfo)) {
                Log.e(TAG, "Null ApplicationInfo object error for: " + packageInfo.packageName);
                continue;
            }
            String appName = getPackageManager().getApplicationLabel(packageInfo.applicationInfo).toString();
            if (appName.toLowerCase().contains(keyword.toLowerCase()))
                results.add(packageInfo);
        }
        return results;
    }

    private void loadPackagesListAsync() {
        Executor executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());
        recyclerView.setAdapter(null);
        loadingIndicator.setVisibility(View.VISIBLE);
        fabSend.hide();
        executor.execute(() -> {
            getInstalledApps();
            sortPackageInfoList();
//          showAppsOnScreen(); another implementation in below:
            MainRecyclerViewAdapter adapter = new MainRecyclerViewAdapter(MainActivity.this, installedPackagesInfo, selectionTracker);
            handler.post(() -> {
                recyclerView.setLayoutManager(new GridLayoutManager(this, preferences.getInt(PREFERENCES_SETTINGS_COLUMN_COUNT, 2) + 1));
                recyclerView.setAdapter(adapter);
                loadingIndicator.setVisibility(View.GONE);
            });
        });
    }

    private void getInstalledApps() {
        installedPackagesInfo = new ArrayList<>();
        selectionTracker = new ArrayList<>();
        for (PackageInfo pi : getPackageManager().getInstalledPackages(PackageManager.GET_META_DATA)) {
            try {
                if ((pi.applicationInfo.flags & (ApplicationInfo.FLAG_SYSTEM | ApplicationInfo.FLAG_UPDATED_SYSTEM_APP)) <= 0) {
                    installedPackagesInfo.add(pi);
                    selectionTracker.add(false);
                }
            } catch (NullPointerException e) {
                Log.e(TAG, "Null pointer error occurred when getting an ApplicationInfo object for: " + pi.packageName);
            }
        }
    }

    private void sortPackageInfoList() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            boolean reverseSort = preferences.getBoolean(PREFERENCES_SETTINGS_REVERSE_SORT, false);
            installedPackagesInfo.sort((o1, o2) -> {
                int sortType = preferences.getInt(PREFERENCES_SETTINGS_SORT_BY, FLAG_SORT_BY_NAME);
                if (sortType == FLAG_SORT_BY_NAME) {
                    String name1 = getPackageManager().getApplicationLabel(o1.applicationInfo) + "";
                    String name2 = getPackageManager().getApplicationLabel(o2.applicationInfo) + "";
                    return reverseSort ? name2.compareTo(name1) : name1.compareTo(name2);
                } else if (sortType == FLAG_SORT_BY_INSTALL_DATE) {
                    String date1 = o1.firstInstallTime + "";
                    String date2 = o2.firstInstallTime + "";
                    return reverseSort ? date2.compareTo(date1) : date1.compareTo(date2);
                } else if (sortType == FLAG_SORT_BY_SIZE) {
                    String size1 = new File(o1.applicationInfo.sourceDir).length() + "";
                    String size2 = new File(o2.applicationInfo.sourceDir).length() + "";
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
                        recyclerView.setLayoutManager(new GridLayoutManager(getApplicationContext(), which + 1));
                        recyclerView.setAdapter(new MainRecyclerViewAdapter(getApplicationContext(), installedPackagesInfo, selectionTracker));
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
        MainRecyclerViewAdapter adapter = new MainRecyclerViewAdapter(this, installedPackagesInfo, selectionTracker);
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
                    loadPackagesListAsync();
                    break;
                default:
                    Log.w(TAG, "The provided intent action is not match to any expected cases: " + intent.getAction());
            }
        }
    }
}
