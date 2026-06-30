package com.ramapps.apkshare;

import android.Manifest;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.core.widget.NestedScrollView;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.divider.MaterialDivider;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.single.PermissionListener;

import java.io.File;
import java.util.Objects;

public class ApkDetailsModalBottomSheet extends BottomSheetDialogFragment {

    public static final String TAG = "ApkDetailsBottomSheet";

    private final Context context;
    private final PackageInfo packageInfo;

    private ImageView imageViewLauncherIcon;
    private TextView textViewAppLabel, textViewAppPackageName, textViewVersionCode, textViewVersionName, textViewSize, textViewInstallationTime, textViewLastUpdateTime, textViewPermissions;
    private MaterialButton buttonSend, buttonBackup, buttonPlay, buttonUninstall, buttonViewAppSettings;
    private MaterialDivider dividerActionButtonsAndDetailsSection;
    private NestedScrollView nestedScrollDetailsSection;

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
        addListeners(context, packageInfo);
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
        buttonSend = parentView.findViewById(R.id.apk_details_button_share);
        buttonBackup = parentView.findViewById(R.id.apk_details_button_backup);
        buttonPlay = parentView.findViewById(R.id.apk_details_button_open);
        buttonUninstall = parentView.findViewById(R.id.apk_details_button_uninstall);
        buttonViewAppSettings = parentView.findViewById(R.id.apk_details_button_view_app_in_settings);
        dividerActionButtonsAndDetailsSection = parentView.findViewById(R.id.apk_details_divider_top_and_details);
        nestedScrollDetailsSection = parentView.findViewById(R.id.apk_details_nested_scroll_view_details);
    }

    private void addListeners(Context context, PackageInfo packageInfo) {
        buttonPlay.setOnClickListener(v -> {
            ApkUtils.launchApp(context, packageInfo.packageName);
        });

        buttonBackup.setOnClickListener(v -> {
            Utils.takeBackupApkFile(context, packageInfo);
        });

        buttonUninstall.setOnClickListener(view -> {
            if (packageInfo.packageName.equals(context.getPackageName())){
                Toast.makeText(context, context.getString(R.string.delete_own_error_msg), Toast.LENGTH_SHORT).show();
            } else {
                ApkUtils.uninstallApp(context, packageInfo);
            }
        });

        buttonSend.setOnClickListener(view -> {
            // TODO: Add send apk file codes to separated method. Because needed many places in the code.
            if (packageInfo.applicationInfo == null) {
                Log.e(TAG, "Application info is null. Can not get information of apk file from null application info.");
                return;
            }
            File file = new File(packageInfo.applicationInfo.publicSourceDir);
            ApkUtils.shareApkFile(context, file, context.getPackageManager().getApplicationLabel(packageInfo.applicationInfo).toString());
        });

        buttonViewAppSettings.setOnClickListener((v -> {
            // TODO: Add this code to separated method for cleaner code.
            Intent intentOpenAppInTheSettings = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            intentOpenAppInTheSettings.setData(Uri.fromParts("package", packageInfo.packageName, null));
            startActivity(intentOpenAppInTheSettings);
        }));
        nestedScrollDetailsSection.setOnScrollChangeListener((NestedScrollView.OnScrollChangeListener) (v, scrollX, scrollY, oldScrollX, oldScrollY) -> {
            if (scrollY == 0) {
                dividerActionButtonsAndDetailsSection.setVisibility(View.INVISIBLE);
            } else {
                dividerActionButtonsAndDetailsSection.setVisibility(View.VISIBLE);
            }
        });
    }

    private void loadDetails(Context context, PackageInfo packageInfo) {
        if (packageInfo.applicationInfo == null) {
            Log.e(TAG, "Application info is null. Can not get information of apk file from null application info.");
            return;
        }
        textViewAppLabel.setText(context.getPackageManager().getApplicationLabel(packageInfo.applicationInfo));
        textViewAppPackageName.setText(packageInfo.packageName);
        imageViewLauncherIcon.setImageDrawable(context.getPackageManager().getApplicationIcon(packageInfo.applicationInfo));
        textViewSize.setText(context.getString(R.string.size) + ": " + Utils.getHumanReadableFileSize(context, new File(packageInfo.applicationInfo.publicSourceDir).length()));
        textViewVersionName.setText(context.getString(R.string.version_name) + ": " + packageInfo.versionName);
        textViewInstallationTime.setText(context.getString(R.string.installation_time) + ": " + Utils.epocTimeToHumanReadableFormat(packageInfo.firstInstallTime));
        textViewLastUpdateTime.setText(context.getString(R.string.last_update_time) + ": " + Utils.epocTimeToHumanReadableFormat(packageInfo.lastUpdateTime));
        textViewPermissions.setText(Utils.getPackagePermissionsList(context, packageInfo.packageName).toString());
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
