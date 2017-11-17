package com.android.gmacs.album;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.SQLException;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.common.gmacs.utils.GLog;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class GmacsImageDao {

    private static GmacsImageDao sInstance = null;
    private final String TAG = GmacsImageDao.class.getSimpleName();
    private final String _DCIM = "/" + Environment.DIRECTORY_DCIM + "/";
    private Context context;
    private ContentResolver contentResolver;
    private String mLocalExternalPath = Environment.getExternalStorageDirectory().getAbsoluteFile().getAbsolutePath();
    private List<ImgDir> mImgDirsCover = new ArrayList<>();
    private List<String> deleteList = new ArrayList<>();

    private GmacsImageDao(Context context) {
        this.context = context;
        contentResolver = getContentResolver();
    }

    public static GmacsImageDao instance(Context inCr) {
        if (sInstance == null) {
            sInstance = new GmacsImageDao(inCr.getApplicationContext());
        }
        return sInstance;
    }

    public static void destroy() {
        if (sInstance != null) {
            if (sInstance.mImgDirsCover != null) {
                sInstance.mImgDirsCover.clear();
            }
            if (sInstance.deleteList != null) {
                sInstance.deleteList.clear();
            }
            sInstance = null;
        }
    }

    public void setContext(Context context) {
        this.context = context;
    }

    public ContentResolver getContentResolver() {
        return (context == null) ? null : context.getContentResolver();
    }

    public ImgDir getImgListByDir(String dirPath) {
        if (mImgDirsCover.size() == 0 || getImgDirByPath(dirPath) == null) {
            scanLocalImageLib("");
        }
        return getImgDirByPath(dirPath);
    }

    public List<ImgDir> loadAllImgDirs() {
        if (mImgDirsCover.size() == 0) {
            scanLocalImageLib("");
        }
        return mImgDirsCover;
    }

    private void scanLocalImageLib(String dirPath) {
        deleteList.clear();
        if (contentResolver != null) {
            context.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_STARTED, Uri.parse("file://" + mLocalExternalPath)));
            ContentResolver cRes = contentResolver;
            Uri uri;
            uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
            String _data = MediaStore.Images.Media.DATA;

            StringBuilder where = new StringBuilder();
            //TODO  待优化
            where.append("(").append(_data).append(" LIKE '").append(dirPath).append("%jpg' OR ").append(_data).append(" LIKE '").append(dirPath)
                    .append("%jpeg' OR ").append(_data).append(" LIKE '").append(dirPath).append("%png' OR ").append(_data).append(" LIKE '").append(dirPath)
                    .append("%JPG' OR ").append(_data).append(" LIKE '").append(dirPath).append("%JPEG' OR ").append(_data).append(" LIKE '").append(dirPath)
                    .append("%PNG') AND ").append(_data).append(" IS NOT NULL ");
            StringBuilder orderby = new StringBuilder();
            orderby.append(MediaStore.Images.Media.DATE_MODIFIED).append(" DESC ").append(" , ").append(MediaStore.Images.Media.DATE_ADDED).append(" DESC ");
            Cursor cur = null;
            try {
                cur = cRes.query(uri, null, where.toString(), null, orderby.toString());
            } catch (Exception e) {
                GLog.e(TAG, e.getMessage(), e);
            }
            try {
                if (cur != null && cur.moveToFirst()) {
                    do {
                        genImageByLocalData(cur);
                    } while (cur.moveToNext());
                }
            } finally {
                if (cur != null) {
                    cur.close();
                }
            }
            if (deleteList.size() > 0) {
                StringBuilder args = new StringBuilder();
                args.append(MediaStore.Images.Media._ID).append(" in(");
                for (String id : deleteList) {
                    args.append(id).append(",");
                }
                if (args.toString().endsWith(",")) {
                    args.deleteCharAt(args.length() - 1);
                }
                args.append(") OR ").append(MediaStore.Images.Media.DATA).append(" IS NULL ");
                try {
                    cRes.delete(uri, args.toString(), null);
                } catch (SQLException e) {
                    GLog.e(TAG, e.getMessage(), e);
                }
            }
            mySort();
        }
    }

    /**
     * 本地扫描归类
     *
     * @param cur
     * @return
     */
    private void genImageByLocalData(Cursor cur) {
        String dirName;
        String dirPath;
        String imgPath;
        // 文件位置
        imgPath = cur.getString(cur.getColumnIndex(MediaStore.Images.Media.DATA));
        if (TextUtils.isEmpty(imgPath) || !(new File(imgPath).exists())) {
            // 地址可能不存在
            deleteList.add(cur.getString(cur.getColumnIndex(MediaStore.Images.Media._ID)));
            return;
        }
        // 文件大小
        int imgSize = cur.getInt(cur.getColumnIndex(MediaStore.Images.Media.SIZE));
        if (imgSize < 4 * 1024) {
            return;
        }
//		文件所在的目录路径
        dirPath = imgPath.substring(0, imgPath.lastIndexOf("/"));
        dirName = dirPath.substring(dirPath.lastIndexOf("/") + 1, dirPath.length());
        ImgDir imgDir = getImgDirByPath(dirPath);
        if (imgDir != null && !imgPath.toLowerCase().contains("/cache/")) {
            imgDir.dataList.add(imgPath);
        } else if (!imgPath.toLowerCase().contains("/cache/")) {
            imgDir = new ImgDir(dirName, dirPath, imgPath);
            imgDir.dataList.add(imgPath);
            mImgDirsCover.add(imgDir);
        }
    }

    private ImgDir getImgDirByPath(String path) {
        for (int i = 0; i < mImgDirsCover.size(); i++) {
            ImgDir imgDir = mImgDirsCover.get(i);
            if (path != null && path.equals(imgDir.dirPath)) {
                return imgDir;
            }
        }
        return null;
    }

    private void mySort() {
        Collections.sort(mImgDirsCover);
    }

    ImgDir getDefaultDir() {
        ImgDir last = null;
        ImgDir now;
        for (int i = 0; i < mImgDirsCover.size(); i++) {
            if (mImgDirsCover.get(i).dirPath.contains(_DCIM) && mImgDirsCover.get(i).dataList.size() > 0) {
                now = mImgDirsCover.get(i);
                if (last == null) {
                    last = now;
                } else if (last.dataList.size() < now.dataList.size()) {
                    last = now;
                }
            }
        }
        if (last == null) {
            for (int i = 0; i < mImgDirsCover.size(); i++) {
                now = mImgDirsCover.get(i);
                if (last == null) {
                    last = now;
                } else if (last.dataList.size() < now.dataList.size()) {
                    last = now;
                }
            }
        }
        return last;
    }

    public static class ImgDir implements Comparable<ImgDir> {
        public final List<String> dataList = new ArrayList<>();
        public String dirName;
        public String dirPath;
        public String coverPath;
        public long updateTime;

        public ImgDir(String dirName, String dirPath, String coverPath) {
            super();
            this.dirName = dirName;
            this.dirPath = dirPath;
            this.coverPath = coverPath;
            updateTime = System.currentTimeMillis();
        }

        @Override
        public String toString() {
            return "ImgDir [dirName=" + dirName + ", dirPath=" + dirPath + ", coverPath=" + coverPath + ", dataList=" + dataList + "]";
        }

        @Override
        public int compareTo(@NonNull ImgDir another) {
            if (another.updateTime > updateTime) {
                return -1;
            } else if (another.updateTime < updateTime) {
                return 1;
            } else {
                return 0;
            }
        }
    }
}
