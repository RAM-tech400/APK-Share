package com.ramapps.apkshare;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

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

}
