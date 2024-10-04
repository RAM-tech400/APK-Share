package com.ramapps.apkshare;

/*
 * This class contains some useful methods for making easy the working on project.
 * All methods in this class should be public and static.
 */

import android.Manifest;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Parcelable;
import android.provider.Settings;
import android.util.Log;
import android.view.animation.LinearInterpolator;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.karumi.dexter.Dexter;
import com.karumi.dexter.listener.single.PermissionListener;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Utils {

    public static void runApplication(Context context, String packageName) {
        try {
            context.startActivity(context.getPackageManager().getLaunchIntentForPackage(packageName));
            Log.v(".Utils", "<" + packageName + "> did run successfully");
        } catch (NullPointerException e) {
            Toast.makeText(context, context.getString(R.string.msg_openning_app_error), Toast.LENGTH_SHORT).show();
            Log.e(".Utils", "Cannot run <" + packageName + ">. More error details:\n" + e);
        }
    }

    public static void uninstallApplication(Context context, String packageName) {
        Intent intent = new Intent(Intent.ACTION_DELETE);
        intent.setData(Uri.fromParts("package", packageName, null));
        context.startActivity(intent);
    }

    public static boolean checkExternalStoragePermission(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return Environment.isExternalStorageManager();
        } else {
            return (ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED);
        }
    }

    public static void requestForExternalStoragePermission(Context context, PermissionListener permissionListener) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Intent intentGetAccessAllFiles = new Intent();
            intentGetAccessAllFiles.setAction(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
            intentGetAccessAllFiles.setData(Uri.fromParts("package", context.getPackageName(), null));
            context.startActivity(intentGetAccessAllFiles);
        } else {
            Dexter.withContext(context)
                    .withPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    .withListener(permissionListener)
                    .check();
        }
    }

    public static void copyFile(File source, File destination) {
        try {
            // Making directory for the destination file if it is not exist and logging the result.
            if (!Objects.requireNonNull(destination.getParentFile()).exists()) {
                Log.d(
                        ".Utils" ,
                        "Making <" + destination.getParent() + "> directory was "
                                + (destination.getParentFile().mkdir()? "successful" : "failed"));
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
            Log.e(".Utils", "There is a problem with Input/Output stream! More error details: \n" + e);
        }
    }

    /**
     * This method will start a sharing process for the all apk files that saved in the app cache directory.
     **/
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

    public static void deleteRecursive(File fileOrDirectory) {
        if (fileOrDirectory.isDirectory())
            for (File child : Objects.requireNonNull(fileOrDirectory.listFiles()))
                deleteRecursive(child);

        Log.d(".Utils", fileOrDirectory.delete()?
                fileOrDirectory.getAbsolutePath() + "successfully deleted!" :
                "Deleting " + fileOrDirectory.getAbsolutePath() + " was failed!"
        );
    }

    public static class AnimatedGradientDrawable extends Drawable {

        private final int[] colors = new int[] {0xffff8000, 0xff00ff00, 0xff0080ff, 0xffff00ff};
        private final PointF point1 = new PointF(0,0);
        private Rect bounds;
        private int alpha = 100;
        private ColorFilter colorFilter;
        private ValueAnimator animator;

        @Override
        public void draw(@NonNull Canvas canvas) {
            PointF startSpot = new PointF(point1.x, point1.y);
            PointF endSpot = new PointF(point1.x - bounds.width(), point1.y - bounds.height());
            LinearGradient gradient = new LinearGradient(
                    startSpot.x,
                    startSpot.y,
                    endSpot.x,
                    endSpot.y,
                    colors,
                    null,
                    Shader.TileMode.MIRROR
            );

            Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
            paint.setShader(gradient);
            paint.setAlpha(alpha);
            paint.setColorFilter(colorFilter);

            canvas.drawRect(bounds, paint);
        }

        @Override
        protected void onBoundsChange(@NonNull Rect bounds) {
            super.onBoundsChange(bounds);
            this.bounds = bounds;
            initAnimations();
            if (animator != null) animator.start();
        }

        private void initAnimations() {
            animator = ValueAnimator.ofFloat(Math.max(bounds.height(), bounds.width()), -(Math.max(bounds.height(), bounds.width())));
            animator.setDuration(3000);
            animator.setInterpolator(new LinearInterpolator());
            animator.setRepeatMode(ValueAnimator.RESTART);
            animator.setRepeatCount(ValueAnimator.INFINITE);
            animator.addUpdateListener(animation -> {
                point1.x = (float) animation.getAnimatedValue();
                point1.y = (float) animation.getAnimatedValue();
                invalidateSelf();
            });
        }

        @Override
        public void setAlpha(int alpha) {
            this.alpha = alpha;
            invalidateSelf();
        }

        @Override
        public void setColorFilter(@Nullable ColorFilter colorFilter) {
            this.colorFilter = colorFilter;
        }

        @Override
        public int getOpacity() {
            return PixelFormat.UNKNOWN;
        }
    }
}
