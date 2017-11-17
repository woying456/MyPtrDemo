package com.android.gmacs.album;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.RelativeLayout;

import com.common.gmacs.utils.GmacsEnvi;

public class WChatAlbumImageLayout extends RelativeLayout {

    private OnImageClickListener mImageClickListener;
    private int mRowPosition;
    private int mLoadMorePosition;

    public WChatAlbumImageLayout(Context context) {
        super(context);
    }

    public WChatAlbumImageLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public WChatAlbumImageLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void setOnImageClickListener(OnImageClickListener l) {
        mImageClickListener = l;
    }

    public void setRowPosition(int position) {
        mRowPosition = position;
    }

    public void setLoadMorePosition(int position) {
        mLoadMorePosition = position;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        super.onInterceptTouchEvent(ev);
        return true;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_UP) {
            float singleChildrenViewWidth = GmacsEnvi.screenWidth * 1f / AlbumConstant.MAX_IMAGE_AMOUNT_IN_ROW;
            float x = event.getX();
            for (int i = 1; i <= AlbumConstant.MAX_IMAGE_AMOUNT_IN_ROW; i++) {
                if (x <= i * singleChildrenViewWidth) {
                    if (mImageClickListener != null) {
                        int[] location = new int[2];
                        getLocationInWindow(location);
                        mImageClickListener.onImageClick(
                                mRowPosition,
                                i - 1,
                                (int) (singleChildrenViewWidth * (i - 1)),
                                location[1],
                                i - 1 == mLoadMorePosition);
                        return true;
                    }
                }
            }
        }
        return super.onTouchEvent(event);
    }

    public interface OnImageClickListener {
        void onImageClick(int rowPosition, int colPosition, int x, int y, boolean isLoadMoreClicked);
    }

}
