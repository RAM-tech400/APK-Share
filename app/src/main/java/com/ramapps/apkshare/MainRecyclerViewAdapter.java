package com.ramapps.apkshare;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.PopupMenu;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.single.PermissionListener;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainRecyclerViewAdapter extends RecyclerView.Adapter<MainRecyclerViewAdapter.CustomViewHolder> {

    private final Context context;
    private final List<PackageInfo> packagesInfo;
    private final int columnCount;
    private final List<Integer> selectedItemsPositions = new ArrayList<>();

    private String searchKeyword;

    public MainRecyclerViewAdapter(Context context, List<PackageInfo> packagesInfo) {
        this.context = context;
        this.packagesInfo = packagesInfo;
        columnCount = context.getSharedPreferences(GlobalVariables.PREFERENCES_SETTINGS, Context.MODE_PRIVATE).getInt(GlobalVariables.PREFERENCES_SETTINGS_COLUMN_COUNT, 2) + 1;
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
        holder.bind(packagesInfo.get(position));
        holder.getCardViewContainer().setChecked(selectedItemsPositions.contains(position));
        addHolderComponentsListeners(holder, position);
        // Adding navigationBar size margin to the last items
        if (packagesInfo.size() % columnCount == 0) {
            if (position >= packagesInfo.size() - columnCount) {
                RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) holder.getCardViewContainer().getLayoutParams();
                layoutParams.setMargins(
                        layoutParams.leftMargin,
                        layoutParams.topMargin,
                        layoutParams.rightMargin,
                        layoutParams.bottomMargin + GlobalVariables.systemBars.bottom
                );
            }
        } else {
            if (position >= packagesInfo.size() - (packagesInfo.size() % columnCount)) {
                RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) holder.getCardViewContainer().getLayoutParams();
                layoutParams.setMargins(
                        layoutParams.leftMargin,
                        layoutParams.topMargin,
                        layoutParams.rightMargin,
                        layoutParams.bottomMargin + GlobalVariables.systemBars.bottom
                );
            }
        }
    }

    private void addHolderComponentsListeners(CustomViewHolder holder, int holderPosition) {
        holder.getCardViewContainer().setOnClickListener(v -> {
            if (!selectedItemsPositions.contains(holderPosition)) {
                if (selectedItemsPositions.size() < 9) {
                    selectedItemsPositions.add(holderPosition);
                } else {
                    // Notice user to selection limit by toast text, animation and vibration (If vibration is enabled in settings)
                    Toast.makeText(context, context.getResources().getQuantityString(R.plurals.msg_selection_limit, 0, 9), Toast.LENGTH_SHORT).show();

                    // TODO: This animation is not good, please improve it
                    ObjectAnimator oa1 = ObjectAnimator.ofFloat(holder.getCardViewContainer(), View.X, 0, context.getResources().getDisplayMetrics().density * -12);
                    oa1.setDuration(40);

                    ObjectAnimator oa2 = ObjectAnimator.ofFloat(holder.getCardViewContainer(), View.X, 0, context.getResources().getDisplayMetrics().density * -12, 0);
                    oa1.setDuration(120);
                    oa2.setStartDelay(80);
                    oa2.setInterpolator(new OvershootInterpolator(4));

                    AnimatorSet animatorSet = new AnimatorSet();
                    animatorSet.playTogether(oa1, oa2);
                    animatorSet.start();

                    if (context.getSharedPreferences(GlobalVariables.PREFERENCES_SETTINGS, Context.MODE_PRIVATE).getBoolean(GlobalVariables.PREFERENCES_SETTINGS_VIBRATION, true)) {
                        Vibrator vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            vibrator.vibrate(VibrationEffect.createOneShot(86, VibrationEffect.DEFAULT_AMPLITUDE));
                        } else {
                            vibrator.vibrate(86);
                        }
                    }
                }
            } else {
                selectedItemsPositions.remove((Object) holderPosition);
            }

            if (!selectedItemsPositions.isEmpty()) {
                GlobalVariables.fabSend.show();
                GlobalVariables.fabSendSearchView.show();
            } else {
                GlobalVariables.fabSend.hide();
                GlobalVariables.fabSendSearchView.hide();
            }

            ((MaterialCardView) v).setChecked(selectedItemsPositions.contains(holderPosition));
        });

        holder.getCardViewContainer().setOnLongClickListener(v -> {
            /* Get log press action from settings
             * 0 (default): Show pop-up menu
             * 1: Open application
             * 2: Uninstall application
             * 3: Direct share
             * 4: Create backup file
             */
            int action = context.getSharedPreferences(GlobalVariables.PREFERENCES_SETTINGS, Context.MODE_PRIVATE).getInt(GlobalVariables.PREFERENCES_SETTINGS_LONG_PRESS_ACTON, 0);
            if (action == 1) {
                Utils.runApplication(context, packagesInfo.get(holderPosition).packageName);
            } else if (action == 2) {
                if (packagesInfo.get(holderPosition).packageName.equals(context.getPackageName())) {
                    Toast.makeText(context, context.getString(R.string.delete_own_error_msg), Toast.LENGTH_SHORT).show();
                } else {
                    Utils.uninstallApplication(context, packagesInfo.get(holderPosition).packageName);
                }
            } else if (action == 3) {
                File file = new File(Objects.requireNonNull(packagesInfo.get(holderPosition).applicationInfo).publicSourceDir);
                File cacheApkFile = new File(context.getCacheDir() + "/ApkFiles/" + context.getPackageManager().getApplicationLabel(Objects.requireNonNull(packagesInfo.get(holderPosition).applicationInfo)) + ".apk");
                Utils.deleteRecursive(Objects.requireNonNull(cacheApkFile.getParentFile()));
                Utils.copyFile(file, cacheApkFile);
                Utils.shareCachedApks(context);
            } else if (action == 4) {
                File file = new File(Objects.requireNonNull(packagesInfo.get(holderPosition).applicationInfo).publicSourceDir);
                File backupFile = new File(Environment.getExternalStorageDirectory() + "/" + Environment.DIRECTORY_DOWNLOADS + "/APK-backups/" + context.getPackageManager().getApplicationLabel(Objects.requireNonNull(packagesInfo.get(holderPosition).applicationInfo)) + ".apk");
                if (Utils.checkExternalStoragePermission(context)) {
                    Utils.copyFile(file, backupFile);
                    showSuccessfulBackupMessage();
                } else {
                    Utils.requestForExternalStoragePermission(context, new PermissionListener() {
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
                                    .setPositiveButton(R.string.grant, (dialog, which) -> permissionToken.continuePermissionRequest())
                                    .setNegativeButton(R.string.deny, null)
                                    .create();
                            alertDialog.show();
                        }
                    });
                }
            } else {
                PopupMenu popupMenu = new PopupMenu(context, holder.getCardViewContainer());
                popupMenu.getMenuInflater().inflate(R.menu.long_press_options_menu, popupMenu.getMenu());
                popupMenu.setOnMenuItemClickListener(item -> {
                    if (item.getItemId() == R.id.itemLongPressOpen) {
                        Utils.runApplication(context, packagesInfo.get(holderPosition).packageName);
                        return true;
                    } else if (item.getItemId() == R.id.itemLongPressUninstall) {
                        Utils.uninstallApplication(context, packagesInfo.get(holderPosition).packageName);
                        return true;
                    } else if (item.getItemId() == R.id.itemLongPressDirectShare) {
                        File file = new File(Objects.requireNonNull(packagesInfo.get(holderPosition).applicationInfo).publicSourceDir);
                        File cacheApkFile = new File(context.getCacheDir() + "/ApkFiles/" + context.getPackageManager().getApplicationLabel(Objects.requireNonNull(packagesInfo.get(holderPosition).applicationInfo)) + ".apk");
                        Utils.deleteRecursive(Objects.requireNonNull(cacheApkFile.getParentFile()));
                        Utils.copyFile(file, cacheApkFile);
                        Utils.shareCachedApks(context);
                        return true;
                    } else if (item.getItemId() == R.id.itemLongPressCreateBackup) {
                        File file = new File(Objects.requireNonNull(packagesInfo.get(holderPosition).applicationInfo).publicSourceDir);
                        File backupFile = new File(Environment.getExternalStorageDirectory() + "/" + Environment.DIRECTORY_DOWNLOADS + "/APK-backups/" + context.getPackageManager().getApplicationLabel(Objects.requireNonNull(packagesInfo.get(holderPosition).applicationInfo)) + ".apk");
                        if (Utils.checkExternalStoragePermission(context)) {
                            Utils.copyFile(file, backupFile);
                            showSuccessfulBackupMessage();
                        } else {
                            Utils.requestForExternalStoragePermission(context, new PermissionListener() {
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
                                            .setPositiveButton(R.string.grant, (dialog, which) -> permissionToken.continuePermissionRequest())
                                            .setNegativeButton(R.string.deny, null)
                                            .create();
                                    alertDialog.show();
                                }
                            });
                        }
                        return true;
                    }
                    return false;
                });
                popupMenu.setForceShowIcon(true);
                popupMenu.show();
            }
            return false;
        });
    }

    private void showSuccessfulBackupMessage() {
        Snackbar.make(GlobalVariables.fabSend, R.string.msg_creating_backup_file_successful, Snackbar.LENGTH_SHORT)
                .setAction(R.string.view, v -> {
                    Intent intentOpenBackupFolder = new Intent();
                    intentOpenBackupFolder.setAction(Intent.ACTION_GET_CONTENT);
                    intentOpenBackupFolder.setDataAndType(Uri.parse(
                            Environment.getExternalStorageDirectory() + File.separator
                                    + Environment.DIRECTORY_DOWNLOADS + "/APK-backups/"
                    ), "*/*");
                    context.startActivity(intentOpenBackupFolder);
                })
                .show();
    }

    @Override
    public int getItemCount() {
        return packagesInfo.size();
    }

    public boolean isSelected(int itemPosition) {
        return selectedItemsPositions.contains(itemPosition);
    }

    public void setSearchKeyword(String searchKeyword) {
        this.searchKeyword = searchKeyword;
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

        /**
         * This method setup the all data that should be show into item view component like as label or icons and etc.
         *
         * @param packageInfo Receiving application data from this given PackageInfo object
         */
        @SuppressLint("SetTextI18n")
        public void bind(@NonNull PackageInfo packageInfo) {
            SpannableString spannableString = new SpannableString(context.getPackageManager().getApplicationLabel(Objects.requireNonNull(packageInfo.applicationInfo)));
            if (searchKeyword != null && !searchKeyword.isEmpty()) {
                Matcher matcher = Pattern.compile(searchKeyword, Pattern.CASE_INSENSITIVE).matcher(spannableString);
                while (matcher.find()) {
                    spannableString.setSpan(new ForegroundColorSpan(context.getResources().getColor(android.R.color.system_accent1_500)), matcher.start(), matcher.end(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    spannableString.setSpan(new StyleSpan(Typeface.BOLD), matcher.start(), matcher.end(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
            }
            textViewAppName.setSelected(true);
            textViewAppName.setText(spannableString);

            imageViewIcon.setImageDrawable(GlobalVariables.appIcons.get(packageInfo.packageName));
            int quickInfoSettings = context.getSharedPreferences(GlobalVariables.PREFERENCES_SETTINGS, Context.MODE_PRIVATE).getInt(GlobalVariables.PREFERENCES_SETTINGS_QUICK_INFO, 1);
            if (quickInfoSettings == 1) {
                textViewAppDetail.setText(packageInfo.packageName);
            } else if (quickInfoSettings == 2) {
                textViewAppDetail.setText(packageInfo.versionCode + "");
            } else if (quickInfoSettings == 3) {
                textViewAppDetail.setText(packageInfo.versionName);
            } else {
                textViewAppDetail.setVisibility(View.GONE);
            }
        }

        public MaterialCardView getCardViewContainer() {
            return cardViewContainer;
        }
    }
}
