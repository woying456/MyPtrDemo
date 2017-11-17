package com.android.gmacs.downloader;

import android.app.ActivityManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.support.v4.util.LruCache;

import com.android.gmacs.downloader.image.ImageLoader;
import com.common.gmacs.utils.FileUtil;
import com.common.gmacs.utils.GmacsEnvi;

import java.io.File;

public class RequestManager {
    private volatile static RequestManager ourInstance;
    private RequestQueue requestQueue;
    private ImageLoader imageLoader;
    private ImageLoader noL1CacheImageLoader;
    private LruCache<String, Bitmap> imageCache;
    private ImageLoader.ImageCache imageCacheWrapper;

    private RequestManager() {
        requestQueue = newRequestQueue();
        requestQueue.start();
    }

    public static RequestManager getInstance() {
        if (null == ourInstance) {
            synchronized (RequestManager.class) {
                if (null == ourInstance) {
                    ourInstance = new RequestManager();
                }
            }
        }
        return ourInstance;
    }

    private static RequestQueue newRequestQueue() {
        File cacheDir = FileUtil.getCacheDir("WChat");

        Network network = new BasicNetwork(new HurlStack());

        return new RequestQueue(new DiskBasedCache(cacheDir), network);
    }

    public ImageLoader getImageLoader() {
        if (imageCache == null) {
            int memClass = ((ActivityManager) GmacsEnvi.appContext
                    .getSystemService(Context.ACTIVITY_SERVICE))
                    .getMemoryClass();
            final int cacheSize = 1024 * 1024 * memClass / 4;
            imageCache = new LruCache<String, Bitmap>(cacheSize) {
                protected int sizeOf(String key, Bitmap bitmap) {
                    return bitmap.getRowBytes() * bitmap.getHeight();
                }
            };
            imageCacheWrapper = new ImageLoader.ImageCache() {
                @Override
                public Bitmap getBitmap(String cacheKey) {
                    return imageCache.get(cacheKey);
                }

                @Override
                public void putBitmap(String cacheKey, Bitmap bitmap) {
                    imageCache.put(cacheKey, bitmap);
                }
            };
        }
        if (imageLoader == null) {
            imageLoader = new ImageLoader(requestQueue, imageCacheWrapper);
        }
        return imageLoader;
    }

    public ImageLoader getNoL1CacheImageLoader() {
        if (noL1CacheImageLoader == null) {
            noL1CacheImageLoader = new ImageLoader(requestQueue, null);
        }
        return noL1CacheImageLoader;
    }

    public void postRequest(Request request) {
        if (requestQueue != null) {
            requestQueue.add(request);
        }
    }
}
