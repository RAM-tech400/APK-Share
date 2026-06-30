package com.ramapps.apkshare;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.net.Uri;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.content.res.AppCompatResources;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ApkUtils {
    public static final String TAG = "ApkUtils";

    public static void launchApp(Context context, String packageName) {
        Intent intentLauncher = context.getPackageManager().getLaunchIntentForPackage(packageName);
        try {
            context.startActivity(intentLauncher);
        } catch (NullPointerException e) {
            Toast.makeText(context, R.string.msg_openning_app_error, Toast.LENGTH_SHORT).show();
            Log.e(TAG, "App con not be launch, because NullPointerException: " + e);
        }
    }

    public static void uninstallApp(Context context, PackageInfo packageInfo) {
        String appName = "";
        if (packageInfo.applicationInfo == null) {
            Log.e(TAG, "PakcageInfo.ApplicationInfo object is null cannot use it to get app name. Use package name as app name instead.");
            appName = packageInfo.packageName;
        } else {
            Log.d(TAG, "Getting app name using PackageManager and ApplicationInfo object...");
            appName = context.getPackageManager().getApplicationLabel(packageInfo.applicationInfo).toString();
        }
        AlertDialog dialogUninstallAlert = new MaterialAlertDialogBuilder(context, com.google.android.material.R.style.ThemeOverlay_Material3_MaterialAlertDialog_Centered)
                .setTitle(String.format(context.getString(R.string.confirm_uninstall_app), appName))
                .setMessage(String.format(context.getString(R.string.msg_uninstall_app_alert_dialog), appName))
                .setIcon(AppCompatResources.getDrawable(context, R.drawable.outline_delete_24))
                .setPositiveButton(context.getString(R.string.uninstall), ((dialogInterface, i) -> {
                    Intent intentUninstall = new Intent(Intent.ACTION_DELETE);
                    intentUninstall.setData(Uri.fromParts("package", packageInfo.packageName, null));
                    context.startActivity(intentUninstall);
                }))
                .setNegativeButton(context.getString(R.string.backup_and_uninstall), ((dialogInterface, i) -> {
                    Utils.takeBackupApkFile(context, packageInfo);
                    Intent intentUninstall = new Intent(Intent.ACTION_DELETE);
                    intentUninstall.setData(Uri.fromParts("package", packageInfo.packageName, null));
                    context.startActivity(intentUninstall);
                }))
                .setNeutralButton(context.getString(R.string.cancel), (dialogInterface, i) -> {
                    dialogInterface.dismiss();
                })
                .create();
        dialogUninstallAlert.show();
    }

    public static void shareApkFile(Context context, File apkFile, String apkName) {
        File cacheApkFile = new File(context.getCacheDir() + "/ApkFiles/" + apkName + ".apk");
        Utils.deleteRecursive(Objects.requireNonNull(cacheApkFile.getParentFile()));
        Utils.copyFileAsyncOnUi(context, apkFile, cacheApkFile, apkName + ".apk", () -> Utils.shareCachedApks(context));
    }

    public static List<File> getApkFilesFromPackageInfoList(List<PackageInfo> packageInfos) {
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

    public static List<String> getApkNamesFromPackageInfoList(Context context, List<PackageInfo> packageInfos) {
        List<String> apkNamedFiles = new ArrayList<>();
        for (PackageInfo packageInfo : packageInfos) {
            if (packageInfo.applicationInfo == null) {
                Log.e(TAG, "This package info provides a null application info object!");
                continue;
            }
            apkNamedFiles.add(context.getPackageManager().getApplicationLabel(packageInfo.applicationInfo) + ".apk");
        }
        return apkNamedFiles;
    }

}
