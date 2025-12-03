package com.ramapps.apkshare;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PermissionInfo;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ApkDetailsModalBottomSheet extends BottomSheetDialogFragment {

    public static final String TAG = "ApkDetailsModalBottomSheet";

    private Context context;
    private PackageInfo packageInfo;

    private ImageView imageViewLauncherIcon;
    private TextView textViewAppLabel, textViewAppPackageName, textViewVersionCode, textViewVersionName, textViewSize, textViewInstallationTime, textViewLastUpdateTime, textViewPermissions;
    private Button buttonViewAppSettings;

    public ApkDetailsModalBottomSheet(Context context, PackageInfo packageInfo) {
        this.context = context;
        this.packageInfo = packageInfo;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        Log.d(TAG, "onCreateDialog()");
        View contentView = LayoutInflater.from(requireContext()).inflate(R.layout.apk_details_layout, null);
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(requireContext());
        bottomSheetDialog.setContentView(contentView);
        initComponents(contentView);
        loadDetails(context, packageInfo);
        return bottomSheetDialog;
    }

    private void initComponents(View parentView) {
        imageViewLauncherIcon = parentView.findViewById(R.id.apk_details_layout_image_view_launcher_icon);
        textViewAppLabel = parentView.findViewById(R.id.apk_details_layout_text_view_app_name);
        textViewAppPackageName = parentView.findViewById(R.id.apk_details_layout_text_view_package_name);
        textViewSize = parentView.findViewById(R.id.apk_details_layout_text_view_label_size);
        textViewVersionCode = parentView.findViewById(R.id.apk_details_layout_text_view_label_version_code);
        textViewVersionName = parentView.findViewById(R.id.apk_details_layout_text_view_label_version_name);
        textViewInstallationTime = parentView.findViewById(R.id.apk_details_layout_text_view_label_installation_time);
        textViewLastUpdateTime = parentView.findViewById(R.id.apk_details_layout_text_view_label_last_update_time);
        textViewPermissions = parentView.findViewById(R.id.apk_details_layout_text_view_content_permissions);
        buttonViewAppSettings = parentView.findViewById(R.id.apk_details_layout_button_app_settings);
    }

    private void loadDetails(Context context, PackageInfo packageInfo) {
        textViewAppLabel.setText(context.getPackageManager().getApplicationLabel(packageInfo.applicationInfo));
        textViewAppPackageName.setText(packageInfo.packageName);
        imageViewLauncherIcon.setImageDrawable(context.getPackageManager().getApplicationIcon(packageInfo.applicationInfo));
        textViewSize.setText(context.getString(R.string.size) + ": " + Utils.getHumanReadableFileSize(context, new File(packageInfo.applicationInfo.publicSourceDir).length()));
        textViewVersionName.setText(context.getString(R.string.version_name) + ": " + packageInfo.versionName);
        textViewInstallationTime.setText(context.getString(R.string.installation_time) + ": " + Utils.epocTimeToHumanReadableFormat(packageInfo.firstInstallTime));
        textViewLastUpdateTime.setText(context.getString(R.string.last_update_time) + ": " + Utils.epocTimeToHumanReadableFormat(packageInfo.lastUpdateTime));
        textViewPermissions.setText(Utils.getPackagePermissionsList(context, packageInfo.packageName).toString());
        buttonViewAppSettings.setOnClickListener((v -> {
            Intent intentOpenAppInTheSettings = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            intentOpenAppInTheSettings.setData(Uri.fromParts("package", packageInfo.packageName, null));
            startActivity(intentOpenAppInTheSettings);
        }));
        try {
            textViewVersionCode.setText(context.getString(R.string.version_code) + ": " + packageInfo.versionCode);
        } catch (Resources.NotFoundException e) {
            Log.e(TAG, "Cannot find the resource id for version code!" + e);
        }

        if (packageInfo.firstInstallTime == packageInfo.lastUpdateTime) {
            textViewLastUpdateTime.setVisibility(View.GONE);
        }

        if (Utils.getPackagePermissionsList(context, packageInfo.packageName).isEmpty()) {
            textViewPermissions.setText(context.getString(R.string.this_app_has_not_need_any_permissions));
        }
    }
}
