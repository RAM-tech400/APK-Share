package com.ramapps.apkshare;

/*
 * This class contains some useful methods for making easy the working on project.
 * All methods in this class should be public and static.
 */

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Parcelable;
import android.view.animation.LinearInterpolator;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public class Utils {

    public static final String ACTION_RESHARE = "com.ramapps.apkshare.Utils.ACTION_RESHARE";

    public static void copyFile(File source, File destination) {
        try {
            if (!destination.getParentFile().exists()) destination.getParentFile().mkdir();
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
            e.printStackTrace();
        }
    }

    public static void shareCachedApks(Context context) {
        File cachedApksDir = new File(context.getCacheDir() + "/ApkFiles/");
        List<Uri> cachedApksUri = new ArrayList<>();
        if (cachedApksDir.listFiles() != null) {
            for (File file : cachedApksDir.listFiles()) {
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
            for (File child : fileOrDirectory.listFiles())
                deleteRecursive(child);

        fileOrDirectory.delete();
    }

    public static class AnimatedGradientDrawable extends Drawable {

        private int[] colors = new int[] {0xffff8000, 0xff00ff00, 0xff0080ff, 0xffff00ff};
        private PointF point1 = new PointF(0,0);
        private Rect bounds;
        private int alpha = 100;
        private ColorFilter colorFilter;
        private ValueAnimator animator;

        @Override
        public void draw(@NonNull Canvas canvas) {
            PointF startSpot = new PointF(point1.x, point1.y);
            PointF endSpot = new PointF(point1.x - bounds.width(), point1.y - bounds.height());
            PointF center = new PointF(bounds.width() / 2, bounds.height() / 2);
            LinearGradient gradient = new LinearGradient(
                    startSpot.x,
                    startSpot.y,
                    endSpot.x,
                    endSpot.y,
                    colors,
                    null,
                    Shader.TileMode.MIRROR);

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
            animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(@NonNull ValueAnimator animation) {
                    point1.x = (float) animation.getAnimatedValue();
                    point1.y = (float) animation.getAnimatedValue();
                    invalidateSelf();
                }
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
            return 0;
        }
    }
}
