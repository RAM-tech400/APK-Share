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

    private ImageView imageViewLauncherIcon;
    private TextView textViewAppLabel, textViewAppPackageName, textViewVersionCode, textViewVersionName, textViewSize, textViewInstallationTime, textViewLastUpdateTime, textViewPermissions;
    private Button buttonViewAppSettings;

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        Log.d(TAG, "onCreateDialog()");
        View contentView = LayoutInflater.from(requireContext()).inflate(R.layout.apk_details_layout, null);
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(requireContext());
        bottomSheetDialog.setContentView(contentView);
        initComponents(contentView);
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
}
