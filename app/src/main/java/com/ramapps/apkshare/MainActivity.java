package com.ramapps.apkshare;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private FloatingActionButton fabSend;

    private List<PackageInfo> installedPackagesInfo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        init();
    }

    @Override
    protected void onResume() {
        super.onResume();
        getInstalledApps();
        showAppsOnScreen();
    }

    private void showAppsOnScreen() {
        MainRecyclerViewAdapter adapter = new MainRecyclerViewAdapter(this, installedPackagesInfo);
        recyclerView.setLayoutManager(new GridLayoutManager(this, 3));
        recyclerView.setAdapter(adapter);
    }

    private void getInstalledApps() {
        installedPackagesInfo = new ArrayList<PackageInfo>();
        PackageManager pm = getPackageManager();
        installedPackagesInfo = pm.getInstalledPackages(PackageManager.GET_META_DATA);
    }

    private void init() {
        recyclerView = findViewById(R.id.mainRecyclerView);
        fabSend = findViewById(R.id.mainFloatingActionBarSend);
    }
}