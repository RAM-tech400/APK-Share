package com.ramapps.apkshare;

import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Insets;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.widget.RelativeLayout;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
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

public class MainActivity extends AppCompatActivity {

    public static final String PREFERENCES_SETTINGS = "Settings";
    public static final String PREFERENCES_SETTINGS_SORT_BY = "Sort by";

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
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) getWindow().setNavigationBarColor(0x88FFFFFF);
        init();
        setSupportActionBar(toolbar);
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
        recyclerView.setLayoutManager(new GridLayoutManager(this, 3));
        recyclerView.setAdapter(adapter);
    }

    private void getInstalledApps() {
        installedPackagesInfo = new ArrayList<PackageInfo>();
        selectionTracker = new ArrayList<Boolean>();
        for (PackageInfo pi : getPackageManager().getInstalledPackages(PackageManager.GET_META_DATA)) {
            installedPackagesInfo.add(pi);
            selectionTracker.add(false);
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
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
            //TODO: implement here.
        } else if (item.getItemId() == R.id.mainMenuItemSettings) {
            //TODO: implement here.
        }
        return super.onOptionsItemSelected(item);
    }
}