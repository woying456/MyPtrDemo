package com.android.gmacs.album;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.annotation.TargetApi;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.RectF;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.android.gmacs.R;
import com.android.gmacs.activity.BaseActivity;
import com.android.gmacs.downloader.RequestManager;
import com.android.gmacs.downloader.VolleyError;
import com.android.gmacs.downloader.image.ImageLoader;
import com.android.gmacs.downloader.image.ImageRequest;
import com.android.gmacs.utils.GmacsUiUtil;
import com.android.gmacs.view.GmacsDialog;
import com.android.gmacs.view.photoview.PhotoView;
import com.android.gmacs.view.photoview.PhotoViewAttacher;
import com.common.gmacs.core.ClientManager;
import com.common.gmacs.core.GmacsConstant;
import com.common.gmacs.core.MessageManager;
import com.common.gmacs.msg.MsgContentType;
import com.common.gmacs.msg.data.IMImageMsg;
import com.common.gmacs.parse.message.Message;
import com.common.gmacs.utils.CloseUtil;
import com.common.gmacs.utils.FileUtil;
import com.common.gmacs.utils.GmacsEnvi;
import com.common.gmacs.utils.ImageUtil;
import com.common.gmacs.utils.PermissionUtil;
import com.common.gmacs.utils.StringUtil;
import com.common.gmacs.utils.ToastUtil;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.android.gmacs.R.string.permission_storage_write;
import static com.android.gmacs.album.AlbumConstant.LAUNCHED_FROM_ALBUM;
import static com.android.gmacs.msg.view.IMImageMsgView.ImgResize;
import static com.android.gmacs.msg.view.IMImageMsgView.MinResize;

public class GmacsImageActivity extends BaseActivity {

    private final int animationDuration = 200;
    private AlbumViewPager mImageViewPager;
    private List<Message> mImageMsgs = new ArrayList<>();
    private RelativeLayout mLayout;
    private ImageView mAlbumLauncher;
    private GmacsDialog.Builder mDialog;
    private boolean mFirstShown = true;
    private boolean mLaunchedFromAlbum;
    private long mInitialLocalID;
    private int mCurrentIndex, mEnterIndex;
    private int mBitmapVisibleWidth;
    private int mBitmapVisibleHeight;
    private int mUserSource;
    private String mUserId;

    private View.OnLongClickListener mPhotoOnLongClickListener = new View.OnLongClickListener() {
        @Override
        public boolean onLongClick(View v) {
            if (mDialog == null) {
                LinearLayout layout = (LinearLayout) LayoutInflater.from(GmacsImageActivity.this)
                        .inflate(R.layout.wchat_bottom_dialog_layout, null);
                (layout.findViewById(R.id.message)).setVisibility(View.GONE);
                layout.findViewById(R.id.divider1).setVisibility(View.GONE);
                TextView ok = (TextView) layout.findViewById(R.id.button1);
                ok.setText(R.string.save);
                ok.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        String url = ((IMImageMsg) mImageMsgs.get(mCurrentIndex).getMsgContent()).mUrl;
                        if (new File(GmacsUiUtil.SAVE_IMAGE_FILE_DIR, StringUtil.MD5(url) + ".jpg").exists()) {
                            ToastUtil.showToast(getResources().getString(R.string.picture_already_exists));
                        } else {
                            PermissionUtil.requestPermissions(GmacsImageActivity.this,
                                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                                    GmacsConstant.REQUEST_CODE_WRITE_EXTERNAL_STORAGE,
                                    new PermissionUtil.PermissionCallBack() {
                                        @Override
                                        public void onCheckedPermission(boolean isGranted) {
                                            if (isGranted) {
                                                saveImageToLocal();
                                            } else {
                                                ToastUtil.showToast(R.string.permission_storage_write);
                                            }
                                        }
                                    });
                        }
                        mDialog.dismiss();
                        mDialog = null;
                    }
                });
                layout.findViewById(R.id.divider2).setVisibility(View.VISIBLE);
                TextView delete = (TextView) layout.findViewById(R.id.button2);
                delete.setVisibility(View.VISIBLE);
                delete.setText(R.string.delete);
                delete.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        final Message message = mImageMsgs.get(mCurrentIndex);
                        final Message.MessageUserInfo messageUserInfo = message.getTalkOtherUserInfo();
                        MessageManager.getInstance().deleteMsgByLocalIdAsync(messageUserInfo.mUserId,
                                messageUserInfo.mUserSource, message.mLocalId, new ClientManager.CallBack() {
                                    @Override
                                    public void done(int errorCode, String errorMessage) {
                                        if (errorCode == 0) {
                                            runOnUiThread(new Runnable() {
                                                @Override
                                                public void run() {
                                                    Intent intent = new Intent();
                                                    intent.putExtra(AlbumConstant.DELETING_MSG_LOCAL_ID, message.mLocalId);
                                                    intent.putExtra(GmacsConstant.EXTRA_USER_ID, messageUserInfo.mUserId);
                                                    intent.putExtra(GmacsConstant.EXTRA_USER_SOURCE, messageUserInfo.mUserSource);
                                                    setResult(AlbumConstant.RESULT_CODE_IMAGE_DELETED, intent);
                                                    finish();
                                                }
                                            });
                                        } else {
                                            ToastUtil.showToast(errorMessage);
                                        }
                                    }
                                });
                        mDialog.dismiss();
                        mDialog = null;
                    }
                });
                TextView cancel = (TextView) layout.findViewById(R.id.cancel);
                cancel.setText(R.string.cancel);
                cancel.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mDialog.cancel();
                    }
                });
                mDialog = new GmacsDialog.Builder(GmacsImageActivity.this, GmacsDialog.Builder.DIALOG_TYPE_CUSTOM_CONTENT_VIEW)
                        .initDialog(layout).setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
                mDialog.create().setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT)
                        .setWindowAnimations(R.style.popupwindow_anim);
            }

            if (null != mDialog && !mDialog.isShowing()) {
                mDialog.show();
            }
            return true;
        }
    };
    private PhotoViewAttacher.OnViewTapListener mViewOnTapListener = new PhotoViewAttacher.OnViewTapListener() {
        @Override
        public void onViewTap(View view, float x, float y) {
            animateExit();
        }
    };
    private PhotoViewAttacher.OnPhotoTapListener mPhotoOnTapListener = new PhotoViewAttacher.OnPhotoTapListener() {
        @Override
        public void onPhotoTap(View view, float x, float y) {
            animateExit();
        }
    };

    @Override
    protected void initView() {
        mAlbumLauncher = (ImageView) findViewById(R.id.album_launcher);
        if (!mLaunchedFromAlbum) {
            mAlbumLauncher.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(GmacsImageActivity.this, WChatAlbumBrowserActivity.class);
                    intent.putExtra(GmacsConstant.EXTRA_USER_ID, mUserId);
                    intent.putExtra(GmacsConstant.EXTRA_USER_SOURCE, mUserSource);
                    intent.putExtra(AlbumConstant.ALBUM_TITLE, getIntent().getStringExtra(AlbumConstant.ALBUM_TITLE));
                    startActivity(intent);
                    finish();
                }
            });
        }

        mLayout = (RelativeLayout) findViewById(R.id.activity_image_layout);
        mImageViewPager = (AlbumViewPager) findViewById(R.id.vp_content_image);
        mImageViewPager.setDrawingCacheEnabled(false);
        mImageViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageSelected(int i) {
                mCurrentIndex = i;
            }

            @Override
            public void onPageScrolled(int i, float v, int i1) {
            }

            @Override
            public void onPageScrollStateChanged(int i) {
            }
        });
    }

    @Override
    protected void initData() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissionOnNeed(Manifest.permission.READ_EXTERNAL_STORAGE,
                    GmacsConstant.REQUEST_CODE_READ_EXTERNAL_STORAGE);
        }
        Intent intent = getIntent();
        mInitialLocalID = intent.getLongExtra(AlbumConstant.IMAGE_LOCAL_ID, 0);
        mLaunchedFromAlbum = intent.getBooleanExtra(LAUNCHED_FROM_ALBUM, false);
        mUserId = intent.getStringExtra(GmacsConstant.EXTRA_USER_ID);
        mUserSource = intent.getIntExtra(GmacsConstant.EXTRA_USER_SOURCE, -1);
        long beginLocalId = intent.getLongExtra(AlbumConstant.BEGIN_LOCAL_ID, 0);
        int count = intent.getIntExtra(AlbumConstant.IMAGE_COUNT, 0);
        if (TextUtils.isEmpty(mUserId) || mUserSource < 0 || beginLocalId == 0) {
            finish();
            return;
        }
        fetchImageMessages(beginLocalId, count);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setBackEnable(false);
        requestWindowNoTitle(true);
        setContentView(R.layout.gmacs_activity_image);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        }
        requestFitSystemWindow(false);
    }

    private void fetchImageMessages(final long beginLocalId, final int count) {
        MessageManager.getInstance().getMessagesAsync(mUserId, mUserSource, new long[]{beginLocalId},
                new MessageManager.GetHistoryMsgCb() {
                    @Override
                    public void done(int errorCode, String errorMessage, List<Message> msgList) {
                        if (errorCode == 0 && !msgList.isEmpty()) {
                            int number = count;
                            Message message = msgList.get(0);
                            if (message.getMsgContent() instanceof IMImageMsg && !message.isDeleted) {
                                mImageMsgs.add(msgList.get(0));
                                --number;
                            }
                            if (number == 0) {
                                setAdapter();
                            } else {
                                MessageManager.getInstance().getMessagesByShowTypeForSingleTalk(
                                        mUserId,
                                        mUserSource,
                                        new String[]{MsgContentType.TYPE_IMAGE},
                                        beginLocalId,
                                        number,
                                        new MessageManager.GetMsgsWithTypeCb() {
                                            @Override
                                            public void done(int errorCode, String errorMsg, List<Message> msgList) {
                                                if (errorCode == 0 && !msgList.isEmpty()) {
                                                    mImageMsgs.addAll(msgList);
                                                    if (!mLaunchedFromAlbum) {
                                                        Collections.reverse(mImageMsgs);
                                                    }
                                                    for (int i = 0; i < mImageMsgs.size(); ++i) {
                                                        if (mImageMsgs.get(i).mLocalId == mInitialLocalID) {
                                                            mCurrentIndex = i;
                                                            mEnterIndex = mCurrentIndex;
                                                            break;
                                                        }
                                                    }
                                                    setAdapter();
                                                } else {
                                                    exit();
                                                }
                                            }
                                        });
                            }
                        } else {
                            exit();
                        }
                    }

                    private void exit() {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                finish();
                            }
                        });
                    }

                    private void setAdapter() {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                mImageViewPager.setAdapter(new ImageViewPagerAdapter());
                                if (mCurrentIndex != 0) {
                                    mImageViewPager.setCurrentItem(mCurrentIndex);
                                }
                            }
                        });
                    }
                });
    }

    private void animateEnter(final Bitmap bitmap, final PhotoView photoView) {
        if (!mFirstShown) {
            photoView.setImageBitmap(bitmap);
            return;
        }
        mFirstShown = false;
        final Intent intent = getIntent();

        photoView.post(new Runnable() {
            @TargetApi(Build.VERSION_CODES.HONEYCOMB)
            @Override
            public void run() {
                mBitmapVisibleWidth = photoView.getWidth();
                if (bitmap.getHeight() * photoView.getWidth() >= bitmap.getWidth() * photoView.getHeight()) {
                    mBitmapVisibleHeight = photoView.getHeight();
                } else {
                    mBitmapVisibleHeight = Math.round(bitmap.getHeight() * 1f / bitmap.getWidth() * photoView.getWidth());
                }

                int chatCardWidth = intent.getIntExtra("width", 0);
                int chatCardHeight = intent.getIntExtra("height", 0);
                int chatCardX = intent.getIntExtra("x", 0);
                int chatCardY = intent.getIntExtra("y", 0);

                ObjectAnimator animatorScaleX = ObjectAnimator.ofFloat(photoView, "scaleX", chatCardWidth * 1f / mBitmapVisibleWidth, 1);
                ObjectAnimator animatorScaleY = ObjectAnimator.ofFloat(photoView, "scaleY", chatCardHeight * 1f / mBitmapVisibleHeight, 1);
                ObjectAnimator animatorTranslateX = ObjectAnimator.ofFloat(photoView, "translationX", -((GmacsEnvi.screenWidth - chatCardWidth) / 2f - chatCardX), 0);
                ObjectAnimator animatorTranslateY;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    animatorTranslateY = ObjectAnimator.ofFloat(photoView, "translationY", -((GmacsEnvi.screenHeight - chatCardHeight) / 2f - chatCardY), 0);
                } else {
                    animatorTranslateY = ObjectAnimator.ofFloat(photoView, "translationY", (chatCardHeight - GmacsEnvi.screenHeight - GmacsEnvi.statusBarHeight) / 2f + chatCardY, 0);
                }

                AnimatorSet animatorSet = new AnimatorSet();
                animatorSet.play(animatorScaleX)
                        .with(animatorScaleY)
                        .with(animatorTranslateX)
                        .with(animatorTranslateY);
                animatorSet.setDuration(animationDuration)
                        .addListener(new Animator.AnimatorListener() {
                            @Override
                            public void onAnimationStart(Animator animation) {
                                photoView.setImageBitmap(bitmap);
                            }

                            @Override
                            public void onAnimationEnd(Animator animation) {
                                mLayout.setBackgroundColor(Color.BLACK);
                                if (!mLaunchedFromAlbum) {
                                    mAlbumLauncher.setVisibility(View.VISIBLE);
                                }
                            }

                            @Override
                            public void onAnimationCancel(Animator animation) {
                            }

                            @Override
                            public void onAnimationRepeat(Animator animation) {
                            }
                        });
                animatorSet.setInterpolator(new DecelerateInterpolator());
                animatorSet.start();
            }
        });
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private void animateExit() {
        Intent intent = getIntent();
        if (mCurrentIndex == mEnterIndex) {
            mLayout.setBackgroundColor(Color.TRANSPARENT);

            final int chatCardWidth = intent.getIntExtra("width", 0);
            final int chatCardHeight = intent.getIntExtra("height", 0);
            int chatCardX = intent.getIntExtra("x", 0);
            int chatCardY = intent.getIntExtra("y", 0);

            PhotoView photoView = null;
            for (int i = 0; i < mImageViewPager.getChildCount(); ++i) {
                View view = mImageViewPager.getChildAt(i);
                int index = (int) view.getTag();
                if (index == mCurrentIndex) {
                    photoView = (PhotoView) view;
                    break;
                }
            }
            if (photoView != null) {
                final float scaleX = chatCardWidth * 1f / mBitmapVisibleWidth;
                final float scaleY = chatCardHeight * 1f / mBitmapVisibleHeight;
                ObjectAnimator animatorScaleX;
                ObjectAnimator animatorScaleY;
                if (scaleX < scaleY) {
                    if (mBitmapVisibleHeight < mBitmapVisibleWidth) {
                        animatorScaleX = ObjectAnimator.ofFloat(photoView, "scaleX", scaleY);
                        animatorScaleY = ObjectAnimator.ofFloat(photoView, "scaleY", scaleY);
                    } else {
                        animatorScaleX = ObjectAnimator.ofFloat(photoView, "scaleX", scaleX);
                        animatorScaleY = ObjectAnimator.ofFloat(photoView, "scaleY", scaleY);
                    }
                } else {
                    animatorScaleX = ObjectAnimator.ofFloat(photoView, "scaleX", scaleX);
                    animatorScaleY = ObjectAnimator.ofFloat(photoView, "scaleY", scaleX);
                }
                ObjectAnimator animatorTranslateX = ObjectAnimator.ofFloat(photoView, "translationX", -((GmacsEnvi.screenWidth - chatCardWidth) / 2f - chatCardX));
                ObjectAnimator animatorTranslateY;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    animatorTranslateY = ObjectAnimator.ofFloat(photoView, "translationY", -((GmacsEnvi.screenHeight - chatCardHeight) / 2f - chatCardY));
                } else {
                    animatorTranslateY = ObjectAnimator.ofFloat(photoView, "translationY", (chatCardHeight - GmacsEnvi.screenHeight - GmacsEnvi.statusBarHeight) / 2f + chatCardY);
                }
                AnimatorSet animatorSet = new AnimatorSet();
                final Interpolator interpolator = new DecelerateInterpolator();
                animatorSet.play(animatorScaleX)
                        .with(animatorScaleY)
                        .with(animatorTranslateX)
                        .with(animatorTranslateY);
                final PhotoView finalPhotoView = photoView;
                animatorSet.setDuration(animationDuration)
                        .addListener(new Animator.AnimatorListener() {
                            @Override
                            public void onAnimationStart(Animator animation) {
                                if (!mLaunchedFromAlbum) {
                                    mAlbumLauncher.setVisibility(View.GONE);
                                }
                                if (Math.abs(scaleX - scaleY) > 0.001) {
                                    if (scaleX < scaleY) {
                                        if (mBitmapVisibleHeight < mBitmapVisibleWidth) {
                                            float dx = (mBitmapVisibleWidth - chatCardWidth / scaleY + finalPhotoView.getWidth() - mBitmapVisibleWidth) / 2;
                                            finalPhotoView.animateClip(new RectF(dx, 0, finalPhotoView.getWidth() - dx, finalPhotoView.getHeight()))
                                                    .withDuration(animationDuration).withInterpolator(interpolator).start();
                                        }
                                    } else if (scaleY < scaleX) {
                                        float dy = (mBitmapVisibleHeight - chatCardHeight / scaleX + finalPhotoView.getHeight() - mBitmapVisibleHeight) / 2;
                                        finalPhotoView.animateClip(new RectF(0, dy, finalPhotoView.getWidth(), finalPhotoView.getHeight() - dy))
                                                .withDuration(animationDuration).withInterpolator(interpolator).start();
                                    }
                                }
                            }

                            @Override
                            public void onAnimationEnd(Animator animation) {
                                finish();
                                overridePendingTransition(0, 0);
                            }

                            @Override
                            public void onAnimationCancel(Animator animation) {
                            }

                            @Override
                            public void onAnimationRepeat(Animator animation) {
                            }
                        });
                animatorSet.setInterpolator(interpolator);
                animatorSet.start();
            } else {
                finish();
                overridePendingTransition(0, R.anim.gmacs_photo_exit);
            }
        } else {
            finish();
            overridePendingTransition(0, R.anim.gmacs_photo_exit);
        }
    }

    private void saveImageToLocal() {
        final String url = ((IMImageMsg) mImageMsgs.get(mCurrentIndex).getMsgContent()).mUrl;
        if (!url.startsWith("/")) {
            RequestManager.getInstance().getNoL1CacheImageLoader().get(url, new ImageLoader.ImageListener() {
                        @Override
                        public void onResponse(final ImageLoader.ImageContainer response, boolean isImmediate) {
                            if (response.getBitmap() != null) {
                                ClientManager.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        if (saveImageToSdCard(response.getBitmap(), url)) {
                                            ToastUtil.showToast(getResources().getString(R.string.picture_save_ok));
                                        } else {
                                            ToastUtil.showToast(R.string.picture_save_failed);
                                        }
                                    }
                                });
                            }
                        }

                        @Override
                        public void onErrorResponse(VolleyError error) {
                        }
                    },
                    GmacsEnvi.screenWidth, GmacsEnvi.screenHeight,
                    ImageView.ScaleType.CENTER_INSIDE, ImageRequest.DRAW_SHAPE_RECT, 0);
        } else {
            ToastUtil.showToast(getResources().getString(R.string.picture_already_exists));
        }
    }

    /**
     * 保存图片到SD卡
     *
     * @param bitmap
     * @param url
     * @return
     */
    private boolean saveImageToSdCard(Bitmap bitmap, String url) {
        if (null == bitmap) {
            return false;
        }
        String filename = StringUtil.MD5(url);
        File captureFile;
        BufferedOutputStream bufferOs = null;
        try {
            if (!FileUtil.sdcardAvailable()) {
                return false;
            }
            File file = new File(GmacsUiUtil.SAVE_IMAGE_FILE_DIR);
            if (!file.exists()) {
                file.mkdirs();
            }
            if (bitmap.hasAlpha()) {
                captureFile = new File(file, filename + ".png");
                bufferOs = new BufferedOutputStream(new FileOutputStream(captureFile));
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, bufferOs);
            } else {
                captureFile = new File(file, filename + ".jpg");
                bufferOs = new BufferedOutputStream(new FileOutputStream(captureFile));
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bufferOs);
            }
            bufferOs.flush();
            Intent scanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
            scanIntent.setData(Uri.fromFile(captureFile));
            GmacsEnvi.appContext.sendBroadcast(scanIntent);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        } finally {
            CloseUtil.closeQuietly(bufferOs);
        }
    }

    @Override
    public void onBackPressed() {
        animateExit();
    }

    @Override
    public void finish() {
        super.finish();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (grantResults.length == 0) {
            return;
        }
        if (requestCode == GmacsConstant.REQUEST_CODE_WRITE_EXTERNAL_STORAGE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                saveImageToLocal();
            } else {
                ToastUtil.showToast(permission_storage_write);
            }
        }
    }

    private class ImageViewPagerAdapter extends PagerAdapter {

        @Override
        public int getCount() {
            return mImageMsgs.size();
        }

        @Override
        public Object instantiateItem(ViewGroup container, final int position) {
            final PhotoView photoView = (PhotoView) LayoutInflater.from(container.getContext())
                    .inflate(R.layout.gmacs_adapter_image_pager_item, container, false);
            final IMImageMsg imageMsg = (IMImageMsg) mImageMsgs.get(position).getMsgContent();
            photoView.setTag(position);
            String requestUrl;
            String tempUrl;
            int[] scaleSize = ImageUtil.getScaleSize(imageMsg.mWidth, imageMsg.mHeight, ImgResize, ImgResize, MinResize, MinResize);
            final int imageInCacheWidth = scaleSize[0];
            final int imageInCacheHeight = scaleSize[1];
            final int requestWidth = scaleSize[2];
            final int requestHeight = scaleSize[3];
            if (!TextUtils.isEmpty(imageMsg.mLocalUrl)) {
                requestUrl = imageMsg.mLocalUrl;
                tempUrl = imageMsg.mLocalUrl;
            } else if (imageMsg.mUrl.startsWith("/")) {
                requestUrl = imageMsg.mUrl;
                tempUrl = imageMsg.mUrl;
            } else {
                requestUrl = ImageUtil.makeUpUrl(imageMsg.mUrl, GmacsEnvi.screenHeight, GmacsEnvi.screenWidth);
                tempUrl = ImageUtil.makeUpUrl(imageMsg.mUrl, requestHeight, requestWidth);
            }
            final String finalTempUrl = tempUrl;
            final ImageLoader.ImageContainer imageContainer =
                    RequestManager.getInstance().getNoL1CacheImageLoader().get(requestUrl
                            , new ImageLoader.ImageListener() {
                                @Override
                                public void onResponse(ImageLoader.ImageContainer response, boolean isImmediate) {
                                    Bitmap bitmap = response.getBitmap();
                                    if (bitmap == null) {
                                        RequestManager.getInstance().getImageLoader().get(finalTempUrl
                                                , new ImageLoader.ImageListener() {
                                                    @Override
                                                    public void onResponse(final ImageLoader.ImageContainer response, boolean isImmediate) {
                                                        Bitmap bitmap1 = response.getBitmap();
                                                        if (bitmap1 != null) {
                                                            if (position == mEnterIndex) {
                                                                animateEnter(bitmap1, photoView);
                                                            } else {
                                                                photoView.setImageBitmap(bitmap1);
                                                            }
                                                        }
                                                    }

                                                    @Override
                                                    public void onErrorResponse(VolleyError error) {
                                                        if (position == mEnterIndex && mFirstShown) {
                                                            finish();
                                                        }
                                                    }
                                                }, imageInCacheWidth
                                                , imageInCacheHeight
                                                , ImageView.ScaleType.CENTER_CROP, ImageRequest.DRAW_SHAPE_RECT
                                                , 0);
                                    } else {
                                        photoView.setImageBitmap(bitmap);
                                    }
                                }

                                @Override
                                public void onErrorResponse(VolleyError error) {
                                }
                            }, GmacsEnvi.screenWidth, GmacsEnvi.screenHeight
                            , ImageView.ScaleType.CENTER_CROP, ImageRequest.DRAW_SHAPE_RECT
                            , 0);
            container.addView(photoView);
            photoView.setOnLongClickListener(mPhotoOnLongClickListener);
            photoView.setOnPhotoTapListener(mPhotoOnTapListener);
            photoView.setOnViewTapListener(mViewOnTapListener);
            photoView.setOnDetachedFromWindow(new PhotoView.OnDetachedFromWindowListener() {
                @Override
                public void onDetachedFromWindow() {
                    imageContainer.cancelRequest();
                    photoView.setImageBitmap(null);
                }
            });
            return photoView;
        }

        @Override
        public boolean isViewFromObject(View view, Object o) {
            return view == o;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            container.removeView((View) object);
            unbindDrawables((View) object);
        }

        private void unbindDrawables(View view) {
            if (view.getBackground() != null) {
                view.getBackground().setCallback(null);
            }
            if (view instanceof ViewGroup) {
                for (int i = 0; i < ((ViewGroup) view).getChildCount(); i++) {
                    unbindDrawables(((ViewGroup) view).getChildAt(i));
                }
                ((ViewGroup) view).removeAllViews();
            }
        }
    }

}
