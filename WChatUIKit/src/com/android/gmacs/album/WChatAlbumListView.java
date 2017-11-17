package com.android.gmacs.album;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.AbsListView;
import android.widget.ListView;


public class WChatAlbumListView extends ListView {

    private OnScrollListener mOnScrollListener;

    public WChatAlbumListView(Context context) {
        super(context);
    }

    public WChatAlbumListView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public WChatAlbumListView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    protected void onFinishInflate() {
        super.onFinishInflate();
        setOnScrollListener(new OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
                if (mOnScrollListener != null) {
                    mOnScrollListener.onScrollStateChanged(view, scrollState);
                }
            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                if (mOnScrollListener != null) {
                    mOnScrollListener.onScroll(view, firstVisibleItem, firstVisibleItem, firstVisibleItem);
                }
            }
        });
    }

    public void setScrollListener(OnScrollListener l) {
        mOnScrollListener = l;
    }

}
