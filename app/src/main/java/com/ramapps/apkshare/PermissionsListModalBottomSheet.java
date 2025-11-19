package com.ramapps.apkshare;

import android.app.Dialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

public class PermissionsListModalBottomSheet extends BottomSheetDialogFragment {
    public static final String TAG = "PermissionsListModalBottomSheet";

    private RelativeLayout relativeLayoutStoragePermission;

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        View contentView = LayoutInflater.from(requireContext()).inflate(R.layout.bottom_sheet_permissions_list, null);
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(requireContext());
        bottomSheetDialog.setContentView(contentView);
        initComponents(contentView);
        addListeners();
        return bottomSheetDialog;
    }

    private void initComponents(View contentView) {
        relativeLayoutStoragePermission = contentView.findViewById(R.id.permissionsBottomSheetRelativeLayoutStoragePermission);
    }

    private void addListeners() {
        relativeLayoutStoragePermission.setOnClickListener((v) -> {
            Intent intentStoragePermissionDetails = new Intent();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                intentStoragePermissionDetails.setAction(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
            } else {
                intentStoragePermissionDetails.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            }
            intentStoragePermissionDetails.setData(Uri.fromParts("package", requireContext().getPackageName(), null));
            startActivity(intentStoragePermissionDetails);
        });
    }

}
