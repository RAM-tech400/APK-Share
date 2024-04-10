package com.ramapps.apkshare;

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
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.OnApplyWindowInsetsListener;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    public static FloatingActionButton fabSend;

    private List<PackageInfo> installedPackagesInfo;
    private List<Boolean> selectionTracker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) getWindow().setNavigationBarColor(0x88FFFFFF);
        init();
    }

    @Override
    protected void onResume() {
        super.onResume();
        getInstalledApps();
        showAppsOnScreen();
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
            //TODO: implement here.
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