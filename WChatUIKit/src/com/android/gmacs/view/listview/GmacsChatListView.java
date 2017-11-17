package com.android.gmacs.view.listview;

import android.content.Context;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.Scroller;

import com.android.gmacs.R;

/**
 * 自定义ListView
 */
public class GmacsChatListView extends ListView implements OnScrollListener {

    private final static int SCROLL_DURATION = 300;
    public GmacsChatListViewHeader mHeaderView;
    public GmacsChatListViewHeader mFooterView;
    private Scroller mHeaderScroller;
    private OnScrollListener mScrollListener;
    private int finalHeight = getResources().getDimensionPixelOffset(R.dimen.max_loading_progress_bar_height);
    private ListViewListener mListViewListener;
    private LinearLayout mHeaderViewContent;
    private LinearLayout mFooterViewContent;

    public GmacsChatListView(Context context) {
        super(context);
        initWithContext(context);
    }

    public GmacsChatListView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initWithContext(context);
    }

    public GmacsChatListView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initWithContext(context);
    }

    private void initWithContext(Context context) {
        mHeaderScroller = new Scroller(context, new DecelerateInterpolator());
        super.setOnScrollListener(this);

        mHeaderView = new GmacsChatListViewHeader(context);
        mHeaderView.setProgressbarGravity(Gravity.CENTER_HORIZONTAL | Gravity.BOTTOM);
        mFooterView = new GmacsChatListViewHeader(context);
        mFooterView.setProgressbarGravity(Gravity.CENTER_HORIZONTAL);
        mFooterView.setVisibleHeight(finalHeight);
        mHeaderViewContent = (LinearLayout) mHeaderView.findViewById(R.id.gmacs_chat_listview_header_content);
        mFooterViewContent = (LinearLayout) mFooterView.findViewById(R.id.gmacs_chat_listview_header_content);
        addHeaderView(mHeaderView);
        addFooterView(mFooterView);
    }

    @Override
    public void setAdapter(ListAdapter adapter) {
        super.setAdapter(adapter);
    }

    public void setPullRefreshEnable(boolean enable) {
        if (!enable) { // disable, hide the content
            mHeaderViewContent.setVisibility(View.INVISIBLE);
        } else {
            mHeaderViewContent.setVisibility(View.VISIBLE);
        }
    }

    public void stopLoadMore() {
        int height = mHeaderView.getVisibleHeight();
        if (height == 0) {
            mHeaderViewContent.setVisibility(GONE);
            return;
        }
        mHeaderScroller.startScroll(0, height - 1, 0, -height + 1, SCROLL_DURATION);
        invalidate();
    }

    public void startLoadMore() {
        mHeaderViewContent.setVisibility(VISIBLE);
        int height = mHeaderView.getVisibleHeight();
        mHeaderScroller.startScroll(0, height, 0, finalHeight - height, SCROLL_DURATION);
        invalidate();
    }

    public void showFooterView() {
        mFooterViewContent.setVisibility(View.VISIBLE);
    }

    public void hideFooterView() {
        mFooterViewContent.setVisibility(View.GONE);
    }

    @Override
    public void computeScroll() {
        if (mHeaderScroller.computeScrollOffset()) {
            mHeaderView.setVisibleHeight(mHeaderScroller.getCurrY());
            if (mHeaderScroller.getCurrY() == finalHeight) {
                mListViewListener.onLoadMore();
            }
            postInvalidate();
        }
        super.computeScroll();
    }

    @Override
    public void setOnScrollListener(OnScrollListener l) {
        mScrollListener = l;
    }

    @Override
    public void onScrollStateChanged(AbsListView view, int scrollState) {
        if (mScrollListener != null) {
            mScrollListener.onScrollStateChanged(view, scrollState);
        }
    }

    @Override
    public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
        if (mScrollListener != null) {
            mScrollListener.onScroll(view, firstVisibleItem, visibleItemCount, totalItemCount);
        }
    }

    public void setListViewListener(ListViewListener l) {
        mListViewListener = l;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                setTranscriptMode(TRANSCRIPT_MODE_DISABLED);
                break;
        }
        return super.onInterceptTouchEvent(event);
    }

    public interface ListViewListener {
        void onLoadMore();
    }

}
