package com.android.gmacs.view.photoview;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.animation.Interpolator;
import android.widget.ImageView;

public class PhotoView extends ImageView implements IPhotoView {

    private final PhotoViewAttacher mAttacher;

    private OnDetachedFromWindowListener detachedFromWindowListener;

    private ScaleType mPendingScaleType;
    private Anim anim;
    private RectF clipRect;

    public PhotoView(Context context) {
        this(context, null);
    }

    public PhotoView(Context context, AttributeSet attr) {
        this(context, attr, 0);
    }

    public PhotoView(Context context, AttributeSet attr, int defStyle) {
        super(context, attr, defStyle);
        super.setScaleType(ScaleType.MATRIX);
        mAttacher = new PhotoViewAttacher(this);

        if (null != mPendingScaleType) {
            setScaleType(mPendingScaleType);
            mPendingScaleType = null;
        }
    }

    /**
     * @deprecated use {@link #setRotationTo(float)}
     */
    @Override
    public void setPhotoViewRotation(float rotationDegree) {
        mAttacher.setRotationTo(rotationDegree);
    }

    @Override
    public void setRotationTo(float rotationDegree) {
        mAttacher.setRotationTo(rotationDegree);
    }

    @Override
    public void setRotationBy(float rotationDegree) {
        mAttacher.setRotationBy(rotationDegree);
    }

    @Override
    public boolean canZoom() {
        return mAttacher.canZoom();
    }

    @Override
    public RectF getDisplayRect() {
        return mAttacher.getDisplayRect();
    }

    @Override
    public Matrix getDisplayMatrix() {
        return mAttacher.getDrawMatrix();
    }

    @Override
    public boolean setDisplayMatrix(Matrix finalRectangle) {
        return mAttacher.setDisplayMatrix(finalRectangle);
    }

    @Override
    @Deprecated
    public float getMinScale() {
        return getMinimumScale();
    }

    @Override
    @Deprecated
    public void setMinScale(float minScale) {
        setMinimumScale(minScale);
    }

    @Override
    public float getMinimumScale() {
        return mAttacher.getMinimumScale();
    }

    @Override
    public void setMinimumScale(float minimumScale) {
        mAttacher.setMinimumScale(minimumScale);
    }

    @Override
    @Deprecated
    public float getMidScale() {
        return getMediumScale();
    }

    @Override
    @Deprecated
    public void setMidScale(float midScale) {
        setMediumScale(midScale);
    }

    @Override
    public float getMediumScale() {
        return mAttacher.getMediumScale();
    }

    @Override
    public void setMediumScale(float mediumScale) {
        mAttacher.setMediumScale(mediumScale);
    }

    @Override
    @Deprecated
    public float getMaxScale() {
        return getMaximumScale();
    }

    @Override
    @Deprecated
    public void setMaxScale(float maxScale) {
        setMaximumScale(maxScale);
    }

    @Override
    public float getMaximumScale() {
        return mAttacher.getMaximumScale();
    }

    @Override
    public void setMaximumScale(float maximumScale) {
        mAttacher.setMaximumScale(maximumScale);
    }

    @Override
    public float getScale() {
        return mAttacher.getScale();
    }

    @Override
    public void setScale(float scale) {
        mAttacher.setScale(scale);
    }

    @Override
    public ScaleType getScaleType() {
        return mAttacher.getScaleType();
    }

    @Override
    public void setScaleType(ScaleType scaleType) {
        if (null != mAttacher) {
            mAttacher.setScaleType(scaleType);
        } else {
            mPendingScaleType = scaleType;
        }
    }

    @Override
    public void setAllowParentInterceptOnEdge(boolean allow) {
        mAttacher.setAllowParentInterceptOnEdge(allow);
    }

    @Override
    // setImageBitmap calls through to this method
    public void setImageDrawable(Drawable drawable) {
        super.setImageDrawable(drawable);
        if (null != mAttacher) {
            mAttacher.update();
        }
    }

    @Override
    public void setImageResource(int resId) {
        super.setImageResource(resId);
        if (null != mAttacher) {
            mAttacher.update();
        }
    }

    @Override
    public void setImageURI(Uri uri) {
        super.setImageURI(uri);
        if (null != mAttacher) {
            mAttacher.update();
        }
    }

    @Override
    public void setOnMatrixChangeListener(PhotoViewAttacher.OnMatrixChangedListener listener) {
        mAttacher.setOnMatrixChangeListener(listener);
    }

    @Override
    public void setOnLongClickListener(OnLongClickListener l) {
        mAttacher.setOnLongClickListener(l);
    }

    @Override
    public PhotoViewAttacher.OnPhotoTapListener getOnPhotoTapListener() {
        return mAttacher.getOnPhotoTapListener();
    }

    @Override
    public void setOnPhotoTapListener(PhotoViewAttacher.OnPhotoTapListener listener) {
        mAttacher.setOnPhotoTapListener(listener);
    }

    @Override
    public PhotoViewAttacher.OnViewTapListener getOnViewTapListener() {
        return mAttacher.getOnViewTapListener();
    }

    @Override
    public void setOnViewTapListener(PhotoViewAttacher.OnViewTapListener listener) {
        mAttacher.setOnViewTapListener(listener);
    }

    @Override
    public void setScale(float scale, boolean animate) {
        mAttacher.setScale(scale, animate);
    }

    @Override
    public void setScale(float scale, float focalX, float focalY, boolean animate) {
        mAttacher.setScale(scale, focalX, focalY, animate);
    }

    @Override
    public void setZoomable(boolean zoomable) {
        mAttacher.setZoomable(zoomable);
    }

    @Override
    public Bitmap getVisibleRectangleBitmap() {
        return mAttacher.getVisibleRectangleBitmap();
    }

    @Override
    public void setZoomTransitionDuration(int milliseconds) {
        mAttacher.setZoomTransitionDuration(milliseconds);
    }

    @Override
    public IPhotoView getIPhotoViewImplementation() {
        return mAttacher;
    }

    @Override
    public void setOnDoubleTapListener(GestureDetector.OnDoubleTapListener newOnDoubleTapListener) {
        mAttacher.setOnDoubleTapListener(newOnDoubleTapListener);
    }

    public void setOnDetachedFromWindow(OnDetachedFromWindowListener listener) {
        this.detachedFromWindowListener = listener;
    }
    @Override
    protected void onDetachedFromWindow() {
        if (detachedFromWindowListener != null) {
            detachedFromWindowListener.onDetachedFromWindow();
        }
        mAttacher.cleanup();
        super.onDetachedFromWindow();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (anim != null) {
            long scaleElapsed = System.currentTimeMillis() - anim.time;
            boolean finished = scaleElapsed > anim.duration;
            scaleElapsed = Math.min(scaleElapsed, anim.duration);
            if (anim.clipRectStart != null && anim.clipRectEnd != null) {
                float interpolation = anim.interpolator.getInterpolation(1f * scaleElapsed / anim.duration);
                float clipRectLeft = (anim.clipRectEnd.left - anim.clipRectStart.left) * interpolation + anim.clipRectStart.left;
                float clipRectRight = (anim.clipRectEnd.right - anim.clipRectStart.right) * interpolation + anim.clipRectStart.right;
                float clipRectTop = (anim.clipRectEnd.top - anim.clipRectStart.top) * interpolation + anim.clipRectStart.top;
                float clipRectBottom = (anim.clipRectEnd.bottom - anim.clipRectStart.bottom) * interpolation + anim.clipRectStart.bottom;
                if (clipRect == null) {
                    clipRect = new RectF(clipRectLeft, clipRectTop, clipRectRight, clipRectBottom);
                } else {
                    clipRect.set(clipRectLeft, clipRectTop, clipRectRight, clipRectBottom);
                }
            }
            if (finished) {
                anim = null;
            }
            invalidate();
        }
        if (clipRect != null) {
            canvas.clipRect(clipRect);
        }
        super.onDraw(canvas);
    }

    public AnimationBuilder animateClip(RectF targetClipRect) {
        return new AnimationBuilder(targetClipRect);
    }

    public interface OnDetachedFromWindowListener {
        void onDetachedFromWindow();
    }

    private static class Anim {
        private RectF clipRectStart;
        private RectF clipRectEnd;
        private long duration = 500; // How long the anim takes
        private long time = System.currentTimeMillis(); // Start time
        private Interpolator interpolator;
    }

    public final class AnimationBuilder {

        private Interpolator interpolator;
        private RectF targetClipRect;
        private long duration = 500;

        private AnimationBuilder(RectF targetClipRect) {
            this.targetClipRect = targetClipRect;
        }

        /**
         * Desired duration of the anim in milliseconds. Default is 500.
         *
         * @param duration duration in milliseconds.
         * @return this builder for method chaining.
         */
        public AnimationBuilder withDuration(long duration) {
            this.duration = duration;
            return this;
        }

        public AnimationBuilder withInterpolator(Interpolator interpolator) {
            this.interpolator = interpolator;
            return this;
        }


        /**
         * Starts the animation.
         */
        public void start() {
            anim = new Anim();
            anim.clipRectStart = clipRect == null ? new RectF(0, 0, getWidth(), getHeight()) : clipRect;
            anim.clipRectEnd = targetClipRect;
            anim.duration = duration;
            anim.interpolator = interpolator;
            anim.time = System.currentTimeMillis();
            invalidate();
        }

    }

}