package com.android.gmacs.utils;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;

import com.android.gmacs.R;
import com.android.gmacs.downloader.image.ImageLoader;
import com.common.gmacs.utils.GmacsEnvi;
import com.common.gmacs.utils.GmacsUtils;

import java.util.ArrayList;

public class MultiImageComposer implements ImageLoader.MultiImageComposer {

    private int maxWidth;
    private int maxHeight;

    public MultiImageComposer(int maxWidth, int maxHeight) {
        this.maxWidth = maxWidth;
        this.maxHeight = maxHeight;
    }

    @Override
    public Bitmap onCompose(ArrayList<ImageLoader.ImageContainer> containers) {
        float halfMaxWidth = maxWidth / 2f;
        float halfMaxHeight = maxHeight / 2f;
        float gap = GmacsUtils.dipToPixel(1) / 2f;
        Bitmap defaultBitmap = null;
        for (ImageLoader.ImageContainer container : containers) {
            if (container.getBitmap() == null) {
                if (defaultBitmap == null) {
                    defaultBitmap = BitmapFactory.decodeResource(GmacsEnvi.appContext.getResources(), R.drawable.gmacs_ic_default_avatar);
                }
                container.setBitmap(defaultBitmap);
            }
        }
        Bitmap base = Bitmap.createBitmap(maxWidth, maxHeight, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(base);
        Bitmap bitmap;
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        RectF dst = new RectF();
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.WHITE);
//        canvas.drawCircle(halfMaxWidth, halfMaxHeight, halfMaxHeight, paint);
        canvas.drawRect(-halfMaxWidth, -halfMaxHeight, maxWidth, maxHeight, paint);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_ATOP));
        if (containers.size() == 4) {
            ImageLoader.ImageContainer container = containers.get(0);
            bitmap = container.getBitmap();
            dst.left = 0;
            dst.right = halfMaxWidth - gap;
            dst.top = 0;
            dst.bottom = halfMaxHeight - gap;
            canvas.drawBitmap(bitmap, getRect(bitmap, maxWidth, maxHeight), dst, paint);

            container = containers.get(1);
            bitmap = container.getBitmap();
            dst.left = halfMaxWidth + gap;
            dst.right = maxWidth;
            dst.top = 0;
            dst.bottom = halfMaxHeight - gap;
            canvas.drawBitmap(bitmap, getRect(bitmap, maxWidth, maxHeight), dst, paint);

            container = containers.get(2);
            bitmap = container.getBitmap();
            dst.left = 0;
            dst.right = halfMaxWidth - gap;
            dst.top = halfMaxHeight + gap;
            dst.bottom = maxHeight;
            canvas.drawBitmap(bitmap, getRect(bitmap, maxWidth, maxHeight), dst, paint);

            container = containers.get(3);
            bitmap = container.getBitmap();
            dst.left = halfMaxWidth + gap;
            dst.right = maxWidth;
            dst.top = halfMaxHeight + gap;
            dst.bottom = maxHeight;
            canvas.drawBitmap(bitmap, getRect(bitmap, maxWidth, maxHeight), dst, paint);
        } else if (containers.size() == 3) {
            ImageLoader.ImageContainer container = containers.get(0);
            bitmap = container.getBitmap();
            dst.left = (maxWidth * 1f / 4);
            dst.right = maxWidth - dst.left;
            dst.top = 0;
            dst.bottom = halfMaxHeight - gap;
            canvas.drawBitmap(bitmap, getRect(bitmap, maxWidth, maxHeight), dst, paint);

            container = containers.get(1);
            bitmap = container.getBitmap();
            dst.left = 0;
            dst.right = halfMaxWidth - gap;
            dst.top = halfMaxHeight + gap;
            dst.bottom = maxHeight;
            canvas.drawBitmap(bitmap, getRect(bitmap, maxWidth, maxHeight), dst, paint);

            container = containers.get(2);
            bitmap = container.getBitmap();
            dst.left = halfMaxWidth + gap;
            dst.right = maxWidth;
            dst.top = halfMaxHeight + gap;
            dst.bottom = maxHeight;
            canvas.drawBitmap(bitmap, getRect(bitmap, maxWidth, maxHeight), dst, paint);
        } else if (containers.size() == 2) {
            ImageLoader.ImageContainer container = containers.get(0);
            bitmap = container.getBitmap();
            dst.left = 0;
            dst.right = halfMaxWidth - gap;
            dst.top = maxHeight / 4f;
            dst.bottom = maxHeight - dst.top;
            canvas.drawBitmap(bitmap, getRect(bitmap, maxWidth, maxHeight), dst, paint);

            container = containers.get(1);
            bitmap = container.getBitmap();
            dst.left = halfMaxWidth + gap;
            dst.right = maxWidth;
            dst.top = maxHeight / 4f;
            dst.bottom = maxHeight - dst.top;
            canvas.drawBitmap(bitmap, getRect(bitmap, maxWidth, maxHeight), dst, paint);
        } else if (containers.size() == 1) {
            ImageLoader.ImageContainer container = containers.get(0);
            bitmap = container.getBitmap();
            dst.left = maxWidth / 4f;
            dst.right = maxWidth - dst.left;
            dst.top = maxHeight / 4f;
            dst.bottom = maxHeight - dst.top;
            canvas.drawBitmap(bitmap, getRect(bitmap, maxWidth, maxHeight), dst, paint);
        }
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(Color.parseColor("#ebebeb"));
        paint.setStrokeWidth(GmacsUtils.dipToPixel(0.5f));
//        canvas.drawCircle(halfMaxWidth, halfMaxHeight, halfMaxHeight, paint);
        canvas.drawRect(-halfMaxWidth, -halfMaxHeight, maxWidth, maxHeight, paint);
        return base;
    }

    private Rect getRect(Bitmap bitmap, int maxWidth, int maxHeight) {
        Rect rect = new Rect();
        if (bitmap.getWidth() * maxHeight > bitmap.getHeight() * maxWidth) {
            rect.left = (int) ((bitmap.getWidth() - bitmap.getHeight() * 1f * maxWidth / maxHeight) / 2);
            rect.right = bitmap.getWidth() - rect.left;
            rect.top = 0;
            rect.bottom = bitmap.getHeight();
        } else {
            rect.left = 0;
            rect.right = bitmap.getWidth();
            rect.top = (int) ((bitmap.getHeight() - bitmap.getWidth() * 1f * maxHeight / maxWidth) / 2);
            rect.bottom = bitmap.getHeight() - rect.top;
        }
        return rect;
    }
}
