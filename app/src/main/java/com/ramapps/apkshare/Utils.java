package com.ramapps.apkshare;

/*
 * This class contains some useful methods for making easy the working on project.
 * All methods in this class should be public and static.
 */

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PermissionInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Parcelable;
import android.provider.Settings;
import android.util.Log;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.single.PermissionListener;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class Utils {
    private static final String TAG = "Utils";

    public static final int KILO_BYTE_SCALE = 1024;
    public static final int MEGA_BYTE_SCALE = 1024 * 1024;
    public static final int GIGA_BYTE_SCALE = 1024 * 1024 * 1024;

    public static final String ACTION_RESHARE = "com.ramapps.apkshare.Utils.ACTION_RESHARE";

    public static void copyFile(File source, File destination) {
        try {
            if (!Objects.requireNonNull(destination.getParentFile()).exists()) {
                boolean result = destination.getParentFile().mkdir();
                Log.i(TAG, "The mkdir() returns: " + result);
            }
            InputStream inputStream = new FileInputStream(source);
            OutputStream outputStream = new FileOutputStream(destination);
            int length;
            byte[] buffer = new byte[1024];
            while ((length = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, length);
            }
            inputStream.close();
            outputStream.flush();
            outputStream.close();
        } catch (IOException e) {
            Log.e(TAG, "I/O Excepted Error: " + e);
        }
    }

    public static void shareCachedApks(Context context) {
        File cachedApksDir = new File(context.getCacheDir() + "/ApkFiles/");
        List<Uri> cachedApksUri = new ArrayList<>();
        if (cachedApksDir.listFiles() != null) {
            for (File file : Objects.requireNonNull(cachedApksDir.listFiles())) {
                Uri uri = FileProvider.getUriForFile(context, ".provider", file);
                cachedApksUri.add(uri);
            }
            Intent intent = new Intent();
            intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.setType("text/plain");
            if (cachedApksUri.size() == 1) {
                intent.setAction(Intent.ACTION_SEND);
                intent.putExtra(Intent.EXTRA_STREAM, cachedApksUri.get(0));
            } else {
                intent.setAction(Intent.ACTION_SEND_MULTIPLE);
                intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, (ArrayList<? extends Parcelable>) cachedApksUri);
            }
            context.startActivity(intent);
        }
    }

    public static void copyFileAsyncOnUi(Context context, File sourceFile, File destinationFile, String destinationFileName, Runnable postWorks) {
        List<File> sourceFileList = new ArrayList<>();
        List<String> destinationFileNamesList = new ArrayList<>();
        sourceFileList.add(sourceFile);
        destinationFileNamesList.add(
                destinationFileName == null?
                        destinationFile.getName() :
                        destinationFileName
        );
        copyFilesAsyncOnUi(context, sourceFileList, destinationFileNamesList, destinationFile.getParentFile(), postWorks);
    }

    public static void copyFilesAsyncOnUi(Context context, List<File> filesList, List<String> destinationDirectoryNames, File destinationDirectory, Runnable postWorks) {
        AtomicBoolean isContinue = new AtomicBoolean(true);
        LinearProgressIndicator progressIndicator = new LinearProgressIndicator(context);
        progressIndicator.setLayoutParams(new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        progressIndicator.setWaveAmplitude(pixelToDp(context, 4));
        progressIndicator.setWavelength(pixelToDp(context, 28));
        progressIndicator.setIndicatorTrackGapSize(pixelToDp(context, 4));
        FrameLayout frameLayout = new FrameLayout(context);
        frameLayout.setPadding(pixelToDp(context, 24), pixelToDp(context, 12), pixelToDp(context, 24), pixelToDp(context, 12));
        frameLayout.addView(progressIndicator);
        AlertDialog progressDialog = new MaterialAlertDialogBuilder(context)
                .setTitle(R.string.copy_files)
                .setMessage(context.getResources().getQuantityString(R.plurals.copy_proccess_files_count, filesList.size(), filesList.size()))
                .setPositiveButton(R.string.cancel, (dia, witch) -> {
                    isContinue.set(false);
                    dia.dismiss();
                })
                .setCancelable(false)
                .create();
        progressDialog.setView(frameLayout);
        progressDialog.show();
        Executor executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());
        final long[] progress = {0};
        executor.execute(() -> {
            if (!destinationDirectory.exists()) {
                Log.w(TAG, "The directory that we want to copy files into it is not exists! Making it...");
                boolean result = destinationDirectory.mkdir();
                if (result) {
                    Log.d(TAG, "Destination directory successfully created!");
                } else {
                    Log.e(TAG, "Destination directory creation goes failed!");
                    return;
                }
            }
            int i = 0;
            for (File file : filesList) {
                try {
                    InputStream inputStream = new FileInputStream(file);
                    File destinationFile = new File(destinationDirectory + "/" + destinationDirectoryNames.get(i));
                    OutputStream outputStream = new FileOutputStream(destinationFile);
                    Log.d(TAG, "Beginning copy source file: " + file + " to: " + destinationFile);
                    byte[] buffer = new byte[1024 * 64];
                    int length = 0;
                    while ((length = inputStream.read(buffer)) != -1) {
                        if (!isContinue.get()) break;
                        outputStream.write(buffer, 0, length);
                        progress[0] += length;
                        handler.post(() -> {
                            int percentage = (int) ((float) progress[0] / (float) getFilesSize(filesList) * 100);
                            Log.d(TAG, "Setting the progress of copying files to: " + progress[0] + " | Formatted to: " + percentage);
                            progressDialog.setMessage(String.format(context.getString(R.string.formatable_msg_copy_files_progress), file.getName(), destinationFile.getName(), percentage));
                            progressIndicator.setProgress(percentage);
                        });
                    }
                    if (isContinue.get()) outputStream.flush();
                    inputStream.close();
                    outputStream.close();
                } catch (FileNotFoundException e) {
                    Log.e(TAG, "File not found! Error details: " + e);
                } catch (IOException e) {
                    Log.e(TAG, "There is an I/O exception while copying file! Error details: " + e);
                }
                i += 1;
            }
            if (isContinue.get()) handler.post(() -> {
                progressDialog.dismiss();
                if (postWorks != null)
                    postWorks.run();
            });
        });
    }

    private static long getFilesSize(List<File> fileList) {
        long filesSize = 0;
        for (File file :
                fileList) {
            filesSize += file.length();
        }
        Log.d(TAG, "All files size: " + filesSize);
        return filesSize;
    }

    public static void deleteRecursive(File fileOrDirectory) {
        if (fileOrDirectory.isDirectory())
            for (File child : Objects.requireNonNull(fileOrDirectory.listFiles()))
                deleteRecursive(child);

        boolean result = fileOrDirectory.delete();
        Log.i(TAG, "delete() for \"" + fileOrDirectory + "\" returns: " + result);
    }

    public static int pixelToDp(Context context, int pixelSize) {
        return (int) (pixelSize * context.getResources().getDisplayMetrics().density);
    }

    public static List<String> getPackagePermissionsList(Context context, String packageName) {
        Log.d(TAG, "Get the list of the permissions for this package: " + packageName);
        try {
            PackageInfo packageInfo = context.getPackageManager().getPackageInfo(packageName, PackageManager.GET_PERMISSIONS);
            List<String> permissionsList = new ArrayList<>(Arrays.asList(packageInfo.requestedPermissions));
            Log.d(TAG, permissionsList.size() + " permissions did found: " + permissionsList.toString());
            return permissionsList;
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "The given package not found for getting it's permissions! Returning an empty list instead...");
            return new ArrayList<>();
        } catch (NullPointerException e) {
            Log.e(TAG, "NullPointerError for <" + packageName + "> is null! Returning an empty list instead...");
            return new ArrayList<>();
        }
    }

    public static String epocTimeToHumanReadableFormat(Long milliseconds) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Instant instant = Instant.ofEpochMilli(milliseconds);
            DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.MEDIUM);
            ZonedDateTime zonedDateTime = ZonedDateTime.ofInstant(instant, ZoneId.systemDefault());
            String formattedTime = zonedDateTime.format(dateTimeFormatter);
            Log.d(TAG, "Formatting <" + milliseconds + "> in epoc into <" + formattedTime + "> human-readable on API version O and higher!");
            return formattedTime;
        } else {
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("EEEE MMM dd yyyy HH:mm:ss", Locale.getDefault());
            simpleDateFormat.toLocalizedPattern();
            String formattedTime = simpleDateFormat.format(new Date(milliseconds));
            Log.d(TAG, "Formatting <" + milliseconds + "> in epoc into <" + formattedTime + "> human-readable on API version lower than O!");
            return formattedTime;
        }
    }

    public static String getHumanReadableFileSize(Context context, long fileSize) {
        String humanReadableSize = "";
        if (fileSize < KILO_BYTE_SCALE) {
            humanReadableSize = fileSize + " " + context.getString(R.string.byte_label);
        } else if (fileSize < MEGA_BYTE_SCALE) {
            humanReadableSize = new DecimalFormat("#.##").format((((float) fileSize) / KILO_BYTE_SCALE)) + " " + context.getString(R.string.kilobyte_label);
        } else if (fileSize < GIGA_BYTE_SCALE) {
            humanReadableSize = new DecimalFormat("#.##").format((((float) fileSize) / MEGA_BYTE_SCALE)) + " " + context.getString(R.string.megabyte_label);
        } else {
            humanReadableSize = new DecimalFormat("#.##").format((((float) fileSize) / GIGA_BYTE_SCALE)) + " " + context.getString(R.string.gigabyte_label);
        }
        Log.d(TAG, "Format file size <" + fileSize + "> into human-readable <" + humanReadableSize + ">!");
        return humanReadableSize;
    }

    public static void takeBackupApkFile(Context context, PackageInfo packageInfo) {
        if (packageInfo.applicationInfo == null) {
            Log.e(TAG, "PackageInfo.ApplicationInfo is null. Method not able to continue, so exit...");
            return;
        }
        Log.d(TAG, "Getting apk file from package info...");
        File apkFile = new File(packageInfo.applicationInfo.publicSourceDir);
        File backupApkFile = new File(
                Environment.getExternalStorageDirectory() + "/" + Environment.DIRECTORY_DOWNLOADS
                        + "/APK-backups/" + context.getPackageManager().getApplicationLabel(packageInfo.applicationInfo) + ".apk");
        if (!Objects.requireNonNull(backupApkFile.getParentFile()).exists()) {
            Log.w(TAG, "The directory that should contains backup file is not exist. Try to make it by mkdir()...");
            boolean resultMkdir = backupApkFile.getParentFile().mkdir();
            Log.d(TAG, backupApkFile.getParentFile() + " mkdir() returns " + resultMkdir);
        }
        doStorageAccessRequiredTask(context, () -> copyFileAsyncOnUi(
                context,
                apkFile,
                backupApkFile,
                context.getPackageManager().getApplicationLabel(packageInfo.applicationInfo) + ".apk",
                null));
    }

    public static void doStorageAccessRequiredTask(Context context, Runnable task) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Log.d(TAG, "Checking permission access in API R and higher...");
            if (Environment.isExternalStorageManager()) {
                Log.d(TAG, "Checking storage permission access in older APIs than R...");
                task.run();
            } else {
                Log.w(TAG, "Storage permission is not granted yet.");
                Log.d(TAG, "Ask user to grant storage permission...");
                Intent intentGetAccessAllFiles = new Intent();
                intentGetAccessAllFiles.setAction(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                intentGetAccessAllFiles.setData(Uri.fromParts("package", context.getPackageName(), null));
                context.startActivity(intentGetAccessAllFiles);
            }
        } else {
            Log.d(TAG, "Checking storage permission access in older APIs than R...");
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                Log.i(TAG, "Storage permission is granted. Doing runnable task...");
                task.run();
            } else {
                Log.w(TAG, "Storage permission is not granted yet.");
                Log.d(TAG, "Ask user to grant storage permission (Using the Dexter)...");
                Dexter.withContext(context)
                        .withPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        .withListener(new PermissionListener() {
                            @Override
                            public void onPermissionGranted(PermissionGrantedResponse permissionGrantedResponse) {
                                Log.d(TAG, "Dexter permission listener onPermissionGranted(); Do runnable task...");
                                task.run();
                            }

                            @Override
                            public void onPermissionDenied(PermissionDeniedResponse permissionDeniedResponse) {
                                Log.d(TAG, "Dexter permission listener onPermissionDenied()...");
                                Toast.makeText(context, R.string.msg_app_needs_storage_permission, Toast.LENGTH_SHORT).show();
                            }

                            @Override
                            public void onPermissionRationaleShouldBeShown(PermissionRequest permissionRequest, PermissionToken permissionToken) {
                                Log.d(TAG, "Dexter permission listener onPermissionRationaleShouldBeShown()...");
                                Log.d(TAG, "Show an alert dialog to ask user for grant permission again.");
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
    }

}
