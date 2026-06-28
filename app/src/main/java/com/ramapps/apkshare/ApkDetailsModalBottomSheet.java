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
            // TODO: Add launch app codes to separated method. Because needed many places in the code.
            Intent launcherIntent = context.getPackageManager().getLaunchIntentForPackage(packageInfo.packageName);
            try {
                startActivity(launcherIntent);
            } catch (NullPointerException e) {
                Toast.makeText(context, R.string.msg_openning_app_error, Toast.LENGTH_SHORT).show();
                Log.e(TAG, "App con not be launch, because NullPointerException: " + e);
            }

        });

        buttonBackup.setOnClickListener(v -> {
            // TODO: Add backup apk file codes to separated method. Because needed many palaces in the code.
            if (packageInfo.applicationInfo == null) {
                Log.e(TAG, "Application info is null. Can not get information of apk file from null application info.");
                return;
            }
            File apkFile = new File(packageInfo.applicationInfo.publicSourceDir);
            File backupFile = new File(Environment.getExternalStorageDirectory() + "/" + Environment.DIRECTORY_DOWNLOADS + "/APK-backups/" + context.getPackageManager().getApplicationLabel(packageInfo.applicationInfo) + ".apk");
            if (backupFile.getParentFile() == null) {
                Log.e(TAG, "The parent file that backup file will save into it is null. For null safety reason the method will stop here.");
                return;
            }
            if (!backupFile.getParentFile().exists()) backupFile.getParentFile().mkdir();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                if (Environment.isExternalStorageManager()) {
                    Utils.copyFileAsyncOnUi(context, apkFile, backupFile, null, null);
                } else {
                    Intent intentGetAccessAllFiles = new Intent();
                    intentGetAccessAllFiles.setAction(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                    intentGetAccessAllFiles.setData(Uri.fromParts("package", context.getPackageName(), null));
                    context.startActivity(intentGetAccessAllFiles);
                }
            } else {
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                    Utils.copyFileAsyncOnUi(context, apkFile, backupFile, null, null);
                } else {
                    Dexter.withContext(context)
                            .withPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                            .withListener(new PermissionListener() {
                                @Override
                                public void onPermissionGranted(PermissionGrantedResponse permissionGrantedResponse) {
                                    Utils.copyFileAsyncOnUi(context, apkFile, backupFile, null, null);
                                }

                                @Override
                                public void onPermissionDenied(PermissionDeniedResponse permissionDeniedResponse) {
                                    Toast.makeText(context, R.string.msg_app_needs_storage_permission, Toast.LENGTH_SHORT).show();
                                }

                                @Override
                                public void onPermissionRationaleShouldBeShown(PermissionRequest permissionRequest, PermissionToken permissionToken) {
                                    AlertDialog alertDialog = new MaterialAlertDialogBuilder(context, com.google.android.material.R.style.ThemeOverlay_Material3_MaterialAlertDialog_Centered)
                                            .setIcon(R.drawable.outline_folder_24)
                                            .setTitle(R.string.storage_permission)
                                            .setMessage(R.string.msg_app_needs_storage_permission)
                                            .setPositiveButton(R.string.grant, (DialogInterface dialog, int which) -> permissionToken.continuePermissionRequest())
                                            .setNegativeButton(R.string.deny, null)
                                            .create();
                                    alertDialog.show();
                                }
                            }).check();
                }
            }
        });

        buttonUninstall.setOnClickListener(view -> {
            // TODO: Add uninstall app codes to separated method. Because needed many places in the code.
            if (packageInfo.packageName.equals(context.getPackageName())){
                Toast.makeText(context, context.getString(R.string.delete_own_error_msg), Toast.LENGTH_SHORT).show();
            } else {
                new AlertDialog.Builder(context)
                        .setTitle("What would you like to do?")
                        .setMessage("Choose an action for this app:")
                        .setNegativeButton("Uninstall", (dialog, which) -> {
                            //Uninstall the app
                            Intent intent = new Intent(Intent.ACTION_DELETE);
                            intent.setData(Uri.fromParts("package", packageInfo.packageName, null));
                            context.startActivity(intent);
                        })
                        .setPositiveButton("Backup and Uninstall", (dialog, which) -> {
                            if (packageInfo.applicationInfo == null) {
                                Log.e(TAG, "Application info is null. Can not get information of apk file from null application info.");
                                return;
                            }
                            //Backup the APK and uninstall
                            File file = new File(packageInfo.applicationInfo.publicSourceDir);
                            File backupFile = new File(Environment.getExternalStorageDirectory() + "/Download/APK-backups/" +
                                    context.getPackageManager().getApplicationLabel(packageInfo.applicationInfo) + ".apk");
                            Utils.copyFileAsyncOnUi(context, file, backupFile, null, () -> {
                                Toast.makeText(context, "Backup complete:\n" + backupFile.getAbsolutePath(),
                                        Toast.LENGTH_LONG).show();
                                Intent intent = new Intent(Intent.ACTION_DELETE);
                                intent.setData(Uri.fromParts("package", packageInfo.packageName, null));
                                context.startActivity(intent);
                            });
                        })
                        .setNeutralButton("Cancel", null)
                        .show();
            }
        });

        buttonSend.setOnClickListener(view -> {
            // TODO: Add send apk file codes to separated method. Because needed many places in the code.
            if (packageInfo.applicationInfo == null) {
                Log.e(TAG, "Application info is null. Can not get information of apk file from null application info.");
                return;
            }
            File file = new File(packageInfo.applicationInfo.publicSourceDir);
            File cacheApkFile = new File(context.getCacheDir() + "/ApkFiles/" + context.getPackageManager().getApplicationLabel(packageInfo.applicationInfo) + ".apk");
            Utils.deleteRecursive(Objects.requireNonNull(cacheApkFile.getParentFile()));
            Utils.copyFileAsyncOnUi(context, file, cacheApkFile, null, () -> Utils.shareCachedApks(context));
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
