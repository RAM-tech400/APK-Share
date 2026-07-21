package com.ramapps.apkshare;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.content.res.AppCompatResources;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class ApkUtils {
    public static final String TAG = "ApkUtils";

    public enum AppCategory {
        GAME,
        AUDIO,
        VIDEO,
        IMAGE,
        SOCIAL,
        NEWS,
        MAPS,
        PRODUCTIVITY,
        ACCESSIBILITY,
        SHOPPING,
        FINANCE,
        HEALTH,
        EDUCATION,
        COMMUNICATION,
        BROWSER,
        FOOD,
        TRAVEL,
        TOOLS,
        SYSTEM,
        LAUNCHER,
        UNKNOWN
    }

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

    public static void openAppInSystemSettings(Context context, String packageName) {
        Intent intentOpenAppInTheSettings = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        intentOpenAppInTheSettings.setData(Uri.fromParts("package", packageName, null));
        context.startActivity(intentOpenAppInTheSettings);
    }

    public static void shareApkFile(Context context, File apkFile, String apkName) {
        File cacheApkFile = new File(context.getCacheDir() + "/ApkFiles/" + apkName + ".apk");
        Utils.deleteRecursive(Objects.requireNonNull(cacheApkFile.getParentFile()));
        Utils.copyFileAsyncOnUi(context, apkFile, cacheApkFile, apkName + ".apk", () -> Utils.shareCachedApks(context));
    }

    public static void sharePackageInfoListAsApkFiles(Context context, List<PackageInfo> selectedPackages) {
        File cachedApksDir = new File(context.getCacheDir() + "/ApkFiles/");
        Utils.deleteRecursive(cachedApksDir);
        Utils.copyFilesAsyncOnUi(
                context,
                ApkUtils.getApkFilesFromPackageInfoList(selectedPackages),
                ApkUtils.getApkNamesFromPackageInfoList(context, selectedPackages),
                cachedApksDir,
                () -> Utils.shareCachedApks(context)
        );
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

    // TODO: Improve this method and other required methods by this to making better categorize android apps.
    public static AppCategory getAppCategory(Context context, PackageInfo packageInfo) {
        Log.d(TAG, "getAppCategory() is beginning. Params: " + context + ", " + packageInfo);
        ApplicationInfo applicationInfo = packageInfo.applicationInfo;

        // Pass 1: API 26+ native category
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            AppCategory fromNative = appCategoryFromNativeCategory(applicationInfo.category);
            Log.d(TAG, "Result from android native app category: " + fromNative);
            if (fromNative != null && fromNative != AppCategory.UNKNOWN) return fromNative;
        }
        Log.w(TAG, "The result of android native app category is not detectable. Trying another method...");

        // Pass 2: Deprecated FLAG_IS_GAME (Cover pre-26 games)
        if ((applicationInfo.flags & applicationInfo.FLAG_IS_GAME) != 0) {
            Log.d(TAG, "Deprecated game flag is detected.");
            return AppCategory.GAME;
        }

        // Pass 3: Package name + app name keyword heuristics.
        String packageName = applicationInfo.packageName;
        String appName = context.getPackageManager().getApplicationLabel(applicationInfo).toString();
        AppCategory fromKeywords = appCategoryFromKeywords(packageName, appName);
        Log.d(TAG, "Result from keywords matching method: " + fromKeywords);
        if (fromKeywords != null && fromKeywords != AppCategory.UNKNOWN)  return fromKeywords;
        Log.w(TAG, "The result of keywords matching method is not detectable. Trying another method...");

        // Pass 4: Intent capability probing
        AppCategory fromIntent = appCategoryFromIntentProbe(context.getPackageManager(), packageInfo.packageName);
        Log.d(TAG, "Result from intent probing method: " + fromIntent);
        if (fromIntent != null && fromIntent != AppCategory.UNKNOWN) return fromIntent;
        Log.w(TAG, "The result of intent probing method is not detectable. Trying another method...");

        if ((applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0) {
            Log.d(TAG, "System app flag is detected return this as " + AppCategory.SYSTEM);
            return AppCategory.SYSTEM;
        }

        Log.w(TAG, "This method could not find any known category for this package/app. Returning as " + AppCategory.UNKNOWN);
        return AppCategory.UNKNOWN;
    }

    public static AppCategory appCategoryFromNativeCategory(int category) {
        switch (category) {
            case ApplicationInfo.CATEGORY_GAME:
                return AppCategory.GAME;
            case ApplicationInfo.CATEGORY_AUDIO:
                return AppCategory.AUDIO;
            case ApplicationInfo.CATEGORY_VIDEO:
                return AppCategory.VIDEO;
            case ApplicationInfo.CATEGORY_IMAGE:
                return AppCategory.IMAGE;
            case ApplicationInfo.CATEGORY_SOCIAL:
                return AppCategory.SOCIAL;
            case ApplicationInfo.CATEGORY_NEWS:
                return AppCategory.NEWS;
            case ApplicationInfo.CATEGORY_MAPS:
                return AppCategory.MAPS;
            case ApplicationInfo.CATEGORY_PRODUCTIVITY:
                return AppCategory.PRODUCTIVITY;
            case ApplicationInfo.CATEGORY_ACCESSIBILITY:
                return AppCategory.ACCESSIBILITY;
            default:
                return AppCategory.UNKNOWN;
        }
    }

    public static AppCategory appCategoryFromKeywords(String packageName, String appName) {
        HashMap<AppCategory, List<String>> keywordMap = new HashMap<>();
        keywordMap.put(AppCategory.GAME, List.of("game", "puzzle", "rpg", "chess", "sudoku", "arcade", "casino", "ball", "soccer", "minecraft"));
        keywordMap.put(AppCategory.AUDIO, List.of("music", "audio", "spotify", "podcast", "radio", "soundcloud", "shazam", "palyer", "tune", "beat"));
        keywordMap.put(AppCategory.VIDEO, List.of("video", "youtube", "aparat", "netflix", "bolboljan", "filimo", "vlc", "hulu", "disneyplus",
                "movie", "tv", "twitch", "stream"));
        keywordMap.put(AppCategory.IMAGE, List.of("camera", "photo", "gallery", "snapseed", "snapseed", "screenshot", "scanner", "wallpaper", "background", "image"));
        keywordMap.put(AppCategory.SOCIAL, List.of("social", "instagram", "tiktok", "facebook", "twitter", "reddit", "discord", "linkedin", "mastodon"));
        keywordMap.put(AppCategory.COMMUNICATION, List.of("message", "communication", "telegram", "messenger", "sms", "chat", "call", "viber",
                "skype", "signal", "mail", "email", "outlook"));
        keywordMap.put(AppCategory.BROWSER, List.of("browser", "chrome", "firefox", "internet", "vivaldi", "web", "opera"));
        keywordMap.put(AppCategory.SHOPPING, List.of("digikala", "emalls", "shop", "divar", "market"));
        keywordMap.put(AppCategory.FINANCE, List.of("finance", "bank", "wallet", "pay", "cash", "stock", "trade", "currency", "crypto", "coin"));
        keywordMap.put(AppCategory.HEALTH, List.of("health", "fitness", "body", "workout", "care", "heart", "blood", "hospital", "emergency",
                "calorie", "run", "yoga", "sleep", "gym", "diet"));
        keywordMap.put(AppCategory.EDUCATION, List.of("learn", "edu", "school", "course", "tutor", "study", "duolingo", "read", "class", "teach",
                "book", "quiz", "language", "taaghche", "fidiboo", "ketab"));
        keywordMap.put(AppCategory.FOOD, List.of("food", "delivery", "recipe", "restaurant", "grocery", "snack", "hungry", "meal", "cook",
                "drink", "eat", "meal", "kebab", "eat"));
        keywordMap.put(AppCategory.TRAVEL, List.of("travel", "flight", "train", "destination", "place", "hotel", "room", "jabama", "reserve",
                "trip", "tour", "visa", "walk", "journey", "booking", "tapsi", "maxim", "snap", "taxi", "bus", "line", "brt", "explore", "uber", "car"));
        keywordMap.put(AppCategory.MAPS, List.of("map", "navigation", "gps", "waze", "neshan", "balad", "location", "compass", "route", "road", "track"));
        keywordMap.put(AppCategory.NEWS, List.of("news", "headline", "feed", "rss", "magazine", "article", "press", "report", "discover"));
        keywordMap.put(AppCategory.PRODUCTIVITY, List.of("productivity", "note", "todo", "task", "calendar", "office", "doc", "present", "powerpoint",
                "pdf", "sheet", "reminder", "planner", "obsidian", "notion", "slack", "group", "meet", "manage", "org", "sort", "helper", "assist"));
        keywordMap.put(AppCategory.LAUNCHER, List.of("launcher", "home", "desktop"));
        keywordMap.put(AppCategory.ACCESSIBILITY, List.of("accessibility", "talkback", "magnif", "braille", "reader"));
        keywordMap.put(AppCategory.TOOLS, List.of("tool", "util", "cleaner", "vpn", "file", "convert", "terminal", "term", "ssh", "secure", "edit", "make", "create"));

        for (Map.Entry<AppCategory, List<String>> entry : keywordMap.entrySet()) {
            for (String keyword : entry.getValue()) {
                if (packageName.contains(keyword) || appName.contains(keyword)) {
                    return entry.getKey();
                }
            }
        }
        return AppCategory.UNKNOWN;
    }

    public static AppCategory appCategoryFromIntentProbe(PackageManager packageManager, String packageName) {
        HashMap<AppCategory, Intent> probes = new HashMap<>();
        probes.put(AppCategory.AUDIO, new Intent(Intent.ACTION_VIEW).setDataAndType(Uri.parse("file://a.mp3"), "audio/*"));
        probes.put(AppCategory.VIDEO, new Intent(Intent.ACTION_VIEW).setDataAndType(Uri.parse("file://a.mp4"), "video/*"));
        probes.put(AppCategory.IMAGE, new Intent(Intent.ACTION_VIEW).setDataAndType(Uri.parse("file://a.jpg"), "image/*"));
        probes.put(AppCategory.BROWSER, new Intent(Intent.ACTION_VIEW).setData(Uri.parse("https//www.example.com")));

        for (Map.Entry<AppCategory, Intent> entry : probes.entrySet()) {
            List<ResolveInfo> matches = packageManager.queryIntentActivities(entry.getValue(), 0);
            for (ResolveInfo resolveInfo : matches) {
                if (resolveInfo.activityInfo.packageName.equals(packageName)) {
                    return entry.getKey();
                }
            }
        }

        return AppCategory.UNKNOWN;
    }

    public static String getAppCategoryLabel(Context context, AppCategory appCategory) {
        HashMap <AppCategory, String> appCategoryStringHashMap = new HashMap<>();
        appCategoryStringHashMap.put(AppCategory.GAME, context.getString(R.string.app_category_game));
        appCategoryStringHashMap.put(AppCategory.AUDIO, context.getString(R.string.app_category_audio));
        appCategoryStringHashMap.put(AppCategory.VIDEO, context.getString(R.string.app_category_video));
        appCategoryStringHashMap.put(AppCategory.IMAGE, context.getString(R.string.app_category_image));
        appCategoryStringHashMap.put(AppCategory.SOCIAL, context.getString(R.string.app_category_social));
        appCategoryStringHashMap.put(AppCategory.NEWS, context.getString(R.string.app_category_news));
        appCategoryStringHashMap.put(AppCategory.MAPS, context.getString(R.string.app_category_maps));
        appCategoryStringHashMap.put(AppCategory.PRODUCTIVITY, context.getString(R.string.app_category_productivity));
        appCategoryStringHashMap.put(AppCategory.ACCESSIBILITY, context.getString(R.string.app_category_accessibility));
        appCategoryStringHashMap.put(AppCategory.SHOPPING, context.getString(R.string.app_category_shoping));
        appCategoryStringHashMap.put(AppCategory.FINANCE, context.getString(R.string.app_category_finance));
        appCategoryStringHashMap.put(AppCategory.HEALTH, context.getString(R.string.app_category_health));
        appCategoryStringHashMap.put(AppCategory.EDUCATION, context.getString(R.string.app_category_education));
        appCategoryStringHashMap.put(AppCategory.COMMUNICATION, context.getString(R.string.app_category_communication));
        appCategoryStringHashMap.put(AppCategory.BROWSER, context.getString(R.string.app_category_browser));
        appCategoryStringHashMap.put(AppCategory.FOOD, context.getString(R.string.app_category_food));
        appCategoryStringHashMap.put(AppCategory.TRAVEL, context.getString(R.string.app_category_travel));
        appCategoryStringHashMap.put(AppCategory.TOOLS, context.getString(R.string.app_category_tools));
        appCategoryStringHashMap.put(AppCategory.SYSTEM, context.getString(R.string.app_category_system));
        appCategoryStringHashMap.put(AppCategory.LAUNCHER, context.getString(R.string.app_category_launcher));
        appCategoryStringHashMap.put(AppCategory.UNKNOWN, context.getString(R.string.app_category_unknown));

        return appCategoryStringHashMap.get(appCategory);
    }

}
