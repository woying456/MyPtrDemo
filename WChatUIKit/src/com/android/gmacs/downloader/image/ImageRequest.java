

package com.android.gmacs.downloader.image;

import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.widget.ImageView.ScaleType;

import com.android.gmacs.downloader.DefaultRetryPolicy;
import com.android.gmacs.downloader.HttpHeaderParser;
import com.android.gmacs.downloader.NetworkResponse;
import com.android.gmacs.downloader.ParseError;
import com.android.gmacs.downloader.Request;
import com.android.gmacs.downloader.Response;
import com.android.gmacs.downloader.VolleyError;
import com.android.gmacs.downloader.VolleyLog;
import com.common.gmacs.utils.BitmapUtil;

/**
 * A canned request for getting an image at a given URL and calling
 * back with a decoded Bitmap.
 */
public class ImageRequest extends Request<Bitmap> {
    /**
     * Decorate bitmap when doParse
     */
    public static final int DRAW_SHAPE_RECT = 0;
    public static final int DRAW_SHAPE_CIRCLE = 1;
    public static final int DRAW_SHAPE_ROUND_RECT = 2;
    /**
     * Socket timeout in milliseconds for image requests
     */
    private static final int IMAGE_TIMEOUT_MS = 1000;
    /**
     * Default number of retries for image requests
     */
    private static final int IMAGE_MAX_RETRIES = 2;
    /**
     * Default backoff multiplier for image requests
     */
    private static final float IMAGE_BACKOFF_MULT = 2f;
    /**
     * Decoding lock so that we don't decode more than one image at a time (to avoid OOM's)
     */
    private static final Object sDecodeLock = new Object();
    private final Response.Listener<Bitmap> mListener;
    private final Config mDecodeConfig;
    private final int mMaxWidth;
    private final int mMaxHeight;
    private final int mDrawShape;
    private final int mDecorate;
    private ScaleType mScaleType;

    /**
     * Creates a new image request, decoding to a maximum specified width and
     * height. If both width and height are zero, the image will be decoded to
     * its natural size. If one of the two is nonzero, that dimension will be
     * clamped and the other one will be set to preserve the image's aspect
     * ratio. If both width and height are nonzero, the image will be decoded to
     * be fit in the rectangle of dimensions width x height while keeping its
     * aspect ratio.
     *
     * @param url           URL of the image
     * @param listener      Listener to receive the decoded bitmap
     * @param maxWidth      Maximum width to decode this bitmap to, or zero for none
     * @param maxHeight     Maximum height to decode this bitmap to, or zero for
     *                      none
     * @param scaleType     The ImageViews ScaleType used to calculate the needed image size.
     * @param decodeConfig  Format to decode the bitmap to
     * @param errorListener Error listener, or null to ignore errors
     */
    public ImageRequest(String url, Response.Listener<Bitmap> listener, int maxWidth, int maxHeight,
                        ScaleType scaleType, Config decodeConfig, int drawShape, int decorate, Response.ErrorListener errorListener) {
        super(Method.GET, url, errorListener);
        setRetryPolicy(
                new DefaultRetryPolicy(IMAGE_TIMEOUT_MS, IMAGE_MAX_RETRIES, IMAGE_BACKOFF_MULT));
        mListener = listener;
        mDecodeConfig = decodeConfig;
        mMaxWidth = maxWidth;
        mMaxHeight = maxHeight;
        mScaleType = scaleType;
        mDrawShape = drawShape;
        mDecorate = decorate;
    }

    /**
     * For API compatibility with the pre-ScaleType variant of the constructor. Equivalent to
     * the normal constructor with {@code ScaleType.CENTER_INSIDE}.
     */
    @Deprecated
    public ImageRequest(String url, Response.Listener<Bitmap> listener, int maxWidth, int maxHeight,
                        Config decodeConfig, Response.ErrorListener errorListener) {
        this(url, listener, maxWidth, maxHeight,
                ScaleType.CENTER_INSIDE, decodeConfig, DRAW_SHAPE_RECT, 0, errorListener);
    }

    /**
     * Scales one side of a rectangle to fit aspect ratio.
     *
     * @param maxPrimary      Maximum size of the primary dimension (i.e. width for
     *                        max width), or zero to maintain aspect ratio with secondary
     *                        dimension
     * @param maxSecondary    Maximum size of the secondary dimension, or zero to
     *                        maintain aspect ratio with primary dimension
     * @param actualPrimary   Actual size of the primary dimension
     * @param actualSecondary Actual size of the secondary dimension
     * @param scaleType       The ScaleType used to calculate the needed image size.
     */
    public static int getResizedDimension(int maxPrimary, int maxSecondary, int actualPrimary,
                                          int actualSecondary, ScaleType scaleType) {

        // If no dominant value at all, just return the actual.
        if ((maxPrimary == 0) && (maxSecondary == 0)) {
            return actualPrimary;
        }

        // If ScaleType.FIT_XY fill the whole rectangle, ignore ratio.
        if (scaleType == ScaleType.FIT_XY) {
            if (maxPrimary == 0) {
                return actualPrimary;
            }
            return maxPrimary;
        }

        // If primary is unspecified, scale primary to match secondary's scaling ratio.
        if (maxPrimary == 0) {
            double ratio = (double) maxSecondary / (double) actualSecondary;
            return (int) (actualPrimary * ratio);
        }

        if (maxSecondary == 0) {
            return maxPrimary;
        }

        double ratio = (double) actualSecondary / (double) actualPrimary;
        int resized = maxPrimary;

        // If ScaleType.CENTER_CROP fill the whole rectangle, preserve aspect ratio.
        if (scaleType == ScaleType.CENTER_CROP) {
            if ((resized * ratio) < maxSecondary) {
                resized = (int) (maxSecondary / ratio);
            }
            return resized;
        }

        if ((resized * ratio) > maxSecondary) {
            resized = (int) (maxSecondary / ratio);
        }
        return resized;
    }


    @Override
    public Priority getPriority() {
        return Priority.LOW;
    }

    @Override
    protected Response<Bitmap> parseNetworkResponse(NetworkResponse response) {
        // Serialize all decode on a global lock to reduce concurrent heap usage.
        synchronized (sDecodeLock) {
            try {
                return doParse(response);
            } catch (OutOfMemoryError e) {
                VolleyLog.e("Caught OOM for %d byte image, url=%s", response.data.length, getUrl());
                return Response.error(new ParseError(e));
            }
        }
    }

    @Override
    protected Response<Bitmap> parseLocalResponse(String file) {
        synchronized (sDecodeLock) {
            try {
                BitmapFactory.Options decodeOptions = new BitmapFactory.Options();
                Bitmap bitmap;
                decodeOptions.inPreferredConfig = mDecodeConfig;
                if (mMaxWidth == 0 && mMaxHeight == 0) {
                    bitmap = BitmapFactory.decodeFile(file, decodeOptions);
                } else {
                    // If we have to resize this image, first get the natural bounds.
                    decodeOptions.inJustDecodeBounds = true;
                    BitmapFactory.decodeFile(file, decodeOptions);
                    int actualWidth = decodeOptions.outWidth;
                    int actualHeight = decodeOptions.outHeight;

                    // Then compute the dimensions we would ideally like to decode to.
                    int desiredWidth = getResizedDimension(mMaxWidth, mMaxHeight,
                            actualWidth, actualHeight, mScaleType);
                    int desiredHeight = getResizedDimension(mMaxHeight, mMaxWidth,
                            actualHeight, actualWidth, mScaleType);

                    // Decode to the nearest power of two scaling factor.
                    decodeOptions.inJustDecodeBounds = false;
                    // TODO(ficus): Do we need this or is it okay since API 8 doesn't support it?
                    // decodeOptions.inPreferQualityOverSpeed = PREFER_QUALITY_OVER_SPEED;
                    decodeOptions.inSampleSize =
                            BitmapUtil.findBestSampleSize(actualWidth, actualHeight, desiredWidth, desiredHeight);
                    if (desiredWidth < actualWidth) {
                        decodeOptions.inScaled = true;
                        decodeOptions.inDensity = actualWidth;
                        decodeOptions.inTargetDensity = desiredWidth * decodeOptions.inSampleSize;
                    }
                    bitmap = BitmapFactory.decodeFile(file, decodeOptions);
                }
                if (bitmap == null) {
                    return Response.error(new VolleyError());
                } else {
                    return Response.success(bitmap);
                }
            } catch (OutOfMemoryError e) {
                return Response.error(new ParseError(e));
            }
        }
    }

    /**
     * The real guts of parseNetworkResponse. Broken out for readability.
     */
    private Response<Bitmap> doParse(NetworkResponse response) {
        byte[] data = response.data;
        BitmapFactory.Options decodeOptions = new BitmapFactory.Options();
        Bitmap bitmap;
        decodeOptions.inPreferredConfig = mDecodeConfig;
        if (mMaxWidth == 0 && mMaxHeight == 0) {
            bitmap = BitmapFactory.decodeByteArray(data, 0, data.length, decodeOptions);
        } else {
            // If we have to resize this image, first get the natural bounds.
            decodeOptions.inJustDecodeBounds = true;
            BitmapFactory.decodeByteArray(data, 0, data.length, decodeOptions);
            int actualWidth = decodeOptions.outWidth;
            int actualHeight = decodeOptions.outHeight;

            // Then compute the dimensions we would ideally like to decode to.
            int desiredWidth = getResizedDimension(mMaxWidth, mMaxHeight,
                    actualWidth, actualHeight, mScaleType);
            int desiredHeight = getResizedDimension(mMaxHeight, mMaxWidth,
                    actualHeight, actualWidth, mScaleType);

            // Decode to the nearest power of two scaling factor.
            decodeOptions.inJustDecodeBounds = false;
            // TODO(ficus): Do we need this or is it okay since API 8 doesn't support it?
            // decodeOptions.inPreferQualityOverSpeed = PREFER_QUALITY_OVER_SPEED;
            decodeOptions.inSampleSize =
                    BitmapUtil.findBestSampleSize(actualWidth, actualHeight, desiredWidth, desiredHeight);
            if (desiredWidth < actualWidth) {
                decodeOptions.inScaled = true;
                decodeOptions.inDensity = actualWidth;
                decodeOptions.inTargetDensity = desiredWidth * decodeOptions.inSampleSize;
            }
            bitmap = BitmapFactory.decodeByteArray(data, 0, data.length, decodeOptions);

        }

        if (bitmap == null) {
            return Response.error(new ParseError(response));
        } else {
            return Response.success(bitmap, HttpHeaderParser.parseCacheHeaders(response));
        }
    }

    @Override
    protected void deliverResponse(Bitmap response) {
        if (response != null) {
            switch (mDrawShape) {
                case DRAW_SHAPE_RECT:
                    break;
                case DRAW_SHAPE_CIRCLE:
                case DRAW_SHAPE_ROUND_RECT:
                    Bitmap layer = Bitmap.createBitmap(mMaxWidth, mMaxHeight, Config.ARGB_8888);
                    RectF rect = new RectF(0.0f, 0.0f, mMaxWidth, mMaxHeight);
                    Canvas canvas = new Canvas(layer);
                    BitmapShader shader = new BitmapShader(response, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);
                    float scale;
                    float dx = 0, dy = 0;
                    if (response.getWidth() * mMaxHeight > mMaxWidth * response.getHeight()) {
                        scale = (float) mMaxHeight / (float) response.getHeight();
                        dx = (mMaxWidth - response.getWidth() * scale) * 0.5f;
                    } else {
                        scale = (float) mMaxWidth / (float) response.getWidth();
                        dy = (mMaxHeight - response.getHeight() * scale) * 0.5f;
                    }
                    Matrix matrix = new Matrix();
                    matrix.setScale(scale, scale);
                    matrix.postTranslate(dx, dy);
                    shader.setLocalMatrix(matrix);
                    Paint paint = new Paint();
                    paint.setAntiAlias(true);
                    paint.setShader(shader);
                    if (mDrawShape == DRAW_SHAPE_CIRCLE) {
                        canvas.drawRoundRect(rect, mMaxWidth / 2, mMaxHeight / 2, paint);
                    } else {
                        canvas.drawRoundRect(rect, mMaxWidth / 8, mMaxHeight / 8, paint);
                    }
                    response.recycle();
                    response = layer;
                    break;
            }
        }
        mListener.onResponse(response);
    }
}
