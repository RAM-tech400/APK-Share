package com.ramapps.apkshare;

import static com.ramapps.apkshare.GlobalVariables.*;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
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
import java.util.ArrayList;
import java.util.List;

public class MainRecyclerViewAdapter extends RecyclerView.Adapter<MainRecyclerViewAdapter.CustomViewHolder> {
    private static final String TAG = "MainRecyclerViewAdapter";
    private final Context context;
    private final List<AndroidPackageSimpleModel> packagesList;
    private final List<Integer> selectionTracker = new ArrayList<>();
    private final int columnCount;

    public MainRecyclerViewAdapter(Context context, List<AndroidPackageSimpleModel> packagesList) {
        this.context = context;
        this.packagesList = packagesList;
        columnCount = ApkShareApplication.preferences.getInt(PREFERENCES_SETTINGS_COLUMN_COUNT, 2) + 1;
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
    public void onBindViewHolder(@NonNull CustomViewHolder holder, @SuppressLint("RecyclerView") int position) {
        holder.bind(packagesList.get(position));
        holder.getCardViewContainer().setChecked(selectionTracker.contains(position));
        holder.getCardViewContainer().setOnClickListener(v -> {
            if(!selectionTracker.contains(position)) {
                if(selectionTracker.size() < 9){
                    selectionTracker.add(position);
                } else {
                    Toast.makeText(context, context.getResources().getQuantityString(R.plurals.msg_selection_limit, 0, 9), Toast.LENGTH_SHORT).show();
                }
            } else {
                // Use the Integer object to avoid using the remove(int index) and using remove(Object item) instead.
                selectionTracker.remove(Integer.valueOf(position));
            }
            if(!selectionTracker.isEmpty()) {
                MainActivity.fabSend.show();
                MainActivity.fabSendSearchView.show();
            } else {
                MainActivity.fabSend.hide();
                MainActivity.fabSendSearchView.hide();
            }
            ((MaterialCardView) v).setChecked(selectionTracker.contains(position));
        });
        holder.getCardViewContainer().setOnLongClickListener(v -> {
            int action = ApkShareApplication.preferences.getInt(PREFERENCES_SETTINGS_LONG_PRESS_ACTON, 0);
            if (action == 1) {
                // TODO: Move to separate method. Here is so messy.
                if (packagesList.get(position).getPackageName().equals(context.getPackageName())){
                    Toast.makeText(context, context.getString(R.string.delete_own_error_msg), Toast.LENGTH_SHORT).show();
                } else {
                    new AlertDialog.Builder(context)
                            .setTitle("What would you like to do?")
                            .setMessage("Choose an action for this app:")
                            .setNegativeButton("Uninstall", (dialog, which) -> {
                                //Uninstall the app
                                Intent intent = new Intent(Intent.ACTION_DELETE);
                                intent.setData(Uri.fromParts("package", packagesList.get(position).getPackageName(), null));
                                context.startActivity(intent);
                            })
                            .setPositiveButton("Backup and Uninstall", (dialog, which) -> {
                                //Backup the APK and uninstall
                                File file = packagesList.get(position).getApkFile();
                                File backupFile = new File(Environment.getExternalStorageDirectory() + "/Download/APK-backups/" +
                                        context.getPackageManager().getApplicationLabel(packagesList.get(position).getApplicationInfo()) + ".apk");
                                Utils.copyFileAsyncOnUi(context, file, backupFile, null, () -> {
                                    Toast.makeText(context, "Backup complete:\n" + backupFile.getAbsolutePath(),
                                            Toast.LENGTH_LONG).show();
                                    Intent intent = new Intent(Intent.ACTION_DELETE);
                                    intent.setData(Uri.fromParts("package", packagesList.get(position).getPackageName(), null));
                                    context.startActivity(intent);
                                });
                            })
                            .setNeutralButton("Cancel", null)
                            .show();
                }
            } else if (action == 2) {
                // TODO: Move to separate method. Here is so messy.
                File file = packagesList.get(position).getApkFile();
                File cacheApkFile = new File(context.getCacheDir() + "/ApkFiles/" + packagesList.get(position).getLabel() + ".apk");
                Utils.deleteRecursive(cacheApkFile.getParentFile());
                Utils.copyFileAsyncOnUi(context, file, cacheApkFile, null, () -> Utils.shareCachedApks(context));
            } else if (action == 3) {
                Utils.takeBackupApkFile(context, packagesList.get(position).getPackageInfo());
            } else if (action == 4) {
                // TODO: Move to separate method. Here is so messy.
                ApkDetailsModalBottomSheet apkDetailsModalBottomSheet = new ApkDetailsModalBottomSheet(context, packagesList.get(position).getPackageInfo());
                apkDetailsModalBottomSheet.show(((AppCompatActivity) context).getSupportFragmentManager(), ApkDetailsModalBottomSheet.TAG);
            } else {
                ApkUtils.launchApp(context, packagesList.get(position).getPackageName());
            }
            return false;
        });
    }

    private void showSuccessfulBackupMessage() {
        Snackbar.make(MainActivity.fabSend, R.string.msg_creating_backup_file_successful, Snackbar.LENGTH_SHORT)
                .setAction(R.string.view, (View v) -> {
                    Log.d(TAG, "Opening the backup directory to see backed-up files...");
                    String folderName = Environment.getExternalStorageDirectory() + "/" + Environment.DIRECTORY_DOWNLOADS + "/APK-backups/";
                    Uri uri = Uri.parse(folderName);
                    Intent intentOpenBackupFolder = new Intent(Intent.ACTION_VIEW);
                    intentOpenBackupFolder.setDataAndType(uri, DocumentsContract.Document.MIME_TYPE_DIR);
                    intentOpenBackupFolder.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    if (intentOpenBackupFolder.resolveActivityInfo(context.getPackageManager(), 0) != null) {
                        Log.d(TAG, "Opening the backup folder...");
                        context.startActivity(intentOpenBackupFolder);
                    } else {
                        Toast.makeText(context, context.getString(R.string.msg_no_any_app_for_intent), Toast.LENGTH_SHORT).show();
                        Log.e(TAG, "There is no any application or activity that can do opening this folder!");
                    }
                })
                .show();
    }

    @Override
    public int getItemCount() {
        return packagesList.size();
    }

    public List<PackageInfo> getSelectedItems() {
        List<PackageInfo> selectedItems = new ArrayList<>();
        for (int i :
                selectionTracker) {
            selectedItems.add(packagesList.get(i).getPackageInfo());
        }
        return selectedItems;
    }

    public static class CustomViewHolder extends RecyclerView.ViewHolder {
        private static final String TAG = "CustomViewHolder";
        private final MaterialCardView cardViewContainer;
        private final ImageView imageViewIcon;
        private final TextView textViewAppName;

        public CustomViewHolder(@NonNull View itemView) {
            super(itemView);
            cardViewContainer = itemView.findViewById(R.id.listItemCardViewContainer);
            imageViewIcon = itemView.findViewById(R.id.listItemImageViewAppIcon);
            textViewAppName = itemView.findViewById(R.id.mainListTextViewAppName);
        }

        @SuppressLint("SetTextI18n")
        public void bind(AndroidPackageSimpleModel androidPackageSimpleModel){
            textViewAppName.setSelected(true);
            imageViewIcon.setImageDrawable(androidPackageSimpleModel.getIcon());
            textViewAppName.setText(androidPackageSimpleModel.getLabel());
        }

        public MaterialCardView getCardViewContainer() {
            return cardViewContainer;
        }
    }
}
