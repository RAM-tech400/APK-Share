package com.ramapps.apkshare;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.util.Log;

import java.io.File;

public class AndroidPackageSimpleModel {
    public static final String TAG = "AndroidPackageSimpleModel";
    private Context context;
    private String label;
    private String packageName;
    private Drawable icon;

    public AndroidPackageSimpleModel(Context context) {
        this.context = context;
    }

    public AndroidPackageSimpleModel(Context context, String label, String packageName, Drawable icon) {
        this.context = context;
        this.label = label;
        this.packageName = packageName;
        this.icon = icon;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public Drawable getIcon() {
        return icon;
    }

    public void setIcon(Drawable icon) {
        this.icon = icon;
    }

    public PackageInfo getPackageInfo() {
        try {
            return  context.getPackageManager().getPackageInfo(this.packageName, PackageManager.GET_META_DATA);
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "Error Not found package name: " + e);
            return null;
        }
    }

    public ApplicationInfo getApplicationInfo() {
        return getPackageInfo().applicationInfo;
    }

    public File getApkFile() {
        return new File(getPackageInfo().applicationInfo.publicSourceDir);
    }
}
