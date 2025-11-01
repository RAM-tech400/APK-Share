package com.ramapps.apkshare;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.single.PermissionListener;

import java.io.File;
import java.util.List;

public class MainRecyclerViewAdapter extends RecyclerView.Adapter<MainRecyclerViewAdapter.CustomViewHolder> {

    private final Context context;
    private final List<PackageInfo> packagesInfo;
    private final List<Boolean> selectionTracker;
    private final int columnCount;

    public MainRecyclerViewAdapter(Context context, List<PackageInfo> packagesInfo, List<Boolean> selectionTracker) {
        this.context = context;
        this.packagesInfo = packagesInfo;
        this.selectionTracker = selectionTracker;
        columnCount = context.getSharedPreferences(MainActivity.PREFERENCES_SETTINGS, Context.MODE_PRIVATE).getInt(MainActivity.PREFERENCES_SETTINGS_COLUMN_COUNT, 2) + 1;
    }
    @NonNull
    @Override
    public CustomViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (columnCount == 1) {
            return new CustomViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.main_list_single_column_item, parent, false));
        }
        return new CustomViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.main_list_multi_column_item, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull CustomViewHolder holder, int position) {
        final int index = position;
        holder.bind(packagesInfo.get(index));
        holder.getCardViewContainer().setChecked(selectionTracker.get(index));
        holder.getCardViewContainer().setOnClickListener(v -> {
            int selectedCount = 0;
            for (Boolean b : selectionTracker) {
                if(b) selectedCount += 1;
            }
            if(!selectionTracker.get(index)) {
                if(selectedCount < 9){
                    selectionTracker.set(index, true);
                    selectedCount++;
                } else {
                    Toast.makeText(context, context.getResources().getQuantityString(R.plurals.msg_selection_limit, 0, 9), Toast.LENGTH_SHORT).show();
                }
            } else {
                selectionTracker.set(index, false);
                selectedCount--;
            }
            if(selectedCount > 0) {
                MainActivity.fabSend.show();
                MainActivity.fabSendSearchView.show();
            } else {
                MainActivity.fabSend.hide();
                MainActivity.fabSendSearchView.hide();
            }
            ((MaterialCardView) v).setChecked(selectionTracker.get(index));
        });
        holder.getCardViewContainer().setOnLongClickListener(v -> {
            int action = context.getSharedPreferences(MainActivity.PREFERENCES_SETTINGS, Context.MODE_PRIVATE).getInt(MainActivity.PREFERENCES_SETTINGS_LONG_PRESS_ACTON, 0);
            if (action == 1) {
                if (packagesInfo.get(index).packageName.equals(context.getPackageName())){
                    Toast.makeText(context, context.getString(R.string.delete_own_error_msg), Toast.LENGTH_SHORT).show();
                } else {
                    new AlertDialog.Builder(context)
                            .setTitle("What would you like to do?")
                            .setMessage("Choose an action for this app:")
                            .setNegativeButton("Uninstall", (dialog, which) -> {
                                //Uninstall the app
                                Intent intent = new Intent(Intent.ACTION_DELETE);
                                intent.setData(Uri.fromParts("package", packagesInfo.get(index).packageName, null));
                                context.startActivity(intent);
                            })
                            .setPositiveButton("Backup and Uninstall", (dialog, which) -> {
                                //Backup the APK and uninstall
                                File file = new File(packagesInfo.get(index).applicationInfo.publicSourceDir);
                                File backupFile = new File(Environment.getExternalStorageDirectory() + "/Download/APK-backups/" +
                                        context.getPackageManager().getApplicationLabel(packagesInfo.get(index).applicationInfo) + ".apk");
                                Utils.copyFile(file, backupFile);
                                Toast.makeText(context, "Backup complete:\n" + backupFile.getAbsolutePath(),
                                        Toast.LENGTH_LONG).show();
                                Intent intent = new Intent(Intent.ACTION_DELETE);
                                intent.setData(Uri.fromParts("package", packagesInfo.get(index).packageName, null));
                                context.startActivity(intent);
                            })
                            .setNeutralButton("Cancel", null)
                            .show();
                }
            } else if (action == 2) {
                File file = new File(packagesInfo.get(index).applicationInfo.publicSourceDir);
                File cacheApkFile = new File(context.getCacheDir() + "/ApkFiles/" + context.getPackageManager().getApplicationLabel(packagesInfo.get(index).applicationInfo) + ".apk");
                Utils.deleteRecursive(cacheApkFile.getParentFile());
                Utils.copyFile(file, cacheApkFile);
                Utils.shareCachedApks(context);
            } else if (action == 3) {
                File file = new File(packagesInfo.get(index).applicationInfo.publicSourceDir);
                File backupFile = new File(Environment.getExternalStorageDirectory() + "/" + Environment.DIRECTORY_DOWNLOADS + "/APK-backups/" + context.getPackageManager().getApplicationLabel(packagesInfo.get(index).applicationInfo) + ".apk");
                if (!backupFile.getParentFile().exists()) backupFile.getParentFile().mkdir();
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    if (Environment.isExternalStorageManager()) {
                        Utils.copyFile(file, backupFile);
                        showSuccessfulBackupMessage();
                    } else {
                        Intent intentGetAccessAllFiles = new Intent();
                        intentGetAccessAllFiles.setAction(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                        intentGetAccessAllFiles.setData(Uri.fromParts("package", context.getPackageName(), null));
                        context.startActivity(intentGetAccessAllFiles);
                    }
                } else {
                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                        Utils.copyFile(file, backupFile);
                        showSuccessfulBackupMessage();
                    } else {
                        Dexter.withContext(context)
                                .withPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                                .withListener(new PermissionListener() {
                                    @Override
                                    public void onPermissionGranted(PermissionGrantedResponse permissionGrantedResponse) {
                                        Utils.copyFile(file, backupFile);
                                        showSuccessfulBackupMessage();
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
                                                .setPositiveButton(R.string.grant, new DialogInterface.OnClickListener() {
                                                    @Override
                                                    public void onClick(DialogInterface dialog, int which) {
                                                        permissionToken.continuePermissionRequest();
                                                    }
                                                })
                                                .setNegativeButton(R.string.deny, null)
                                                .create();
                                        alertDialog.show();
                                    }
                                }).check();
                    }
                }
            } else {
                try {
                    context.startActivity(context.getPackageManager().getLaunchIntentForPackage(packagesInfo.get(index).packageName));
                } catch (NullPointerException e) {
                    Toast.makeText(context, context.getString(R.string.msg_openning_app_error), Toast.LENGTH_SHORT).show();
                }
            }
            return false;
        });
        // Adding navigationBar size margin to the last items
        if (packagesInfo.size() % columnCount == 0) {
            if (position >= packagesInfo.size() - columnCount) {
                RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) holder.getCardViewContainer().getLayoutParams();
                layoutParams.setMargins(
                        layoutParams.leftMargin,
                        layoutParams.topMargin,
                        layoutParams.rightMargin,
                        layoutParams.bottomMargin + MainActivity.systemBars.bottom
                );
            }
        } else {
            if (position >= packagesInfo.size() - (packagesInfo.size() % columnCount)) {
                RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) holder.getCardViewContainer().getLayoutParams();
                layoutParams.setMargins(
                        layoutParams.leftMargin,
                        layoutParams.topMargin,
                        layoutParams.rightMargin,
                        layoutParams.bottomMargin + MainActivity.systemBars.bottom
                );
            }
        }
    }

    private void showSuccessfulBackupMessage() {
        Snackbar.make(MainActivity.fabSend, R.string.msg_creating_backup_file_successful, Snackbar.LENGTH_SHORT)
                .setAction(R.string.view, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent intentOpenBackupFolder = new Intent();
                        intentOpenBackupFolder.setAction(Intent.ACTION_GET_CONTENT);
                        intentOpenBackupFolder.setDataAndType(Uri.parse(
                                Environment.getExternalStorageDirectory() + File.separator
                                        + Environment.DIRECTORY_DOWNLOADS + "/APK-backups/"
                        ), "*/*");
                        context.startActivity(intentOpenBackupFolder);
                    }
                })
                .show();
    }

    @Override
    public int getItemCount() {
        return packagesInfo.size();
    }

    public class CustomViewHolder extends RecyclerView.ViewHolder {

        private final MaterialCardView cardViewContainer;
        private final ImageView imageViewIcon;
        private final TextView textViewAppName;
        private final TextView textViewAppDetail;

        public CustomViewHolder(@NonNull View itemView) {
            super(itemView);
            cardViewContainer = itemView.findViewById(R.id.listItemCardViewContainer);
            imageViewIcon = itemView.findViewById(R.id.listItemImageViewAppIcon);
            textViewAppName = itemView.findViewById(R.id.mainListTextViewAppName);
            textViewAppDetail = itemView.findViewById(R.id.mainListTextViewAppDetail);
        }

        @SuppressLint("SetTextI18n")
        public void bind(PackageInfo packageInfo){
            textViewAppName.setSelected(true);
            try {
                imageViewIcon.setImageDrawable(context.getPackageManager().getApplicationIcon(packageInfo.packageName));
                textViewAppName.setText(context.getPackageManager().getApplicationLabel(packageInfo.applicationInfo));
                int quickInfoSettings = context.getSharedPreferences(MainActivity.PREFERENCES_SETTINGS, Context.MODE_PRIVATE).getInt(MainActivity.PREFERENCES_SETTINGS_QUICK_INFO, 1);
                if (quickInfoSettings == 1) {
                    textViewAppDetail.setText(packageInfo.packageName);
                } else if (quickInfoSettings == 2) {
                    textViewAppDetail.setText(packageInfo.versionCode + "");
                } else if (quickInfoSettings == 3) {
                    textViewAppDetail.setText(packageInfo.versionName);
                } else {
                    textViewAppDetail.setVisibility(View.GONE);
                }
            } catch (PackageManager.NameNotFoundException e) {
                throw new RuntimeException(e);
            }
        }

        public MaterialCardView getCardViewContainer() {
            return cardViewContainer;
        }
    }
}
