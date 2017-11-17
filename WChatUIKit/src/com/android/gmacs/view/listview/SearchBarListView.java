package com.android.gmacs.view.listview;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AbsListView;
import android.widget.ListView;


public class SearchBarListView extends ListView implements AbsListView.OnScrollListener {

    private View mSearchBar;
    private int searchBarHeight;
    private float preY;
    private boolean isSearchBarShowing;
    private boolean scrollStateChangedEnabled = true;
    private boolean hasSearchBarHeightMeasured;

    public SearchBarListView(Context context) {
        super(context);
    }

    public SearchBarListView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public SearchBarListView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        setOnScrollListener(this);
    }

    public void measureSearchBarHeight(View searchBar) {
        mSearchBar = searchBar;
        mSearchBar.measure(0, 0);
        searchBarHeight = mSearchBar.getMeasuredHeight();
        mSearchBar.setPadding(0, -searchBarHeight, 0, 0);
        hasSearchBarHeightMeasured = true;
    }

    @Override
    public void onScrollStateChanged(AbsListView view, int scrollState) {
        if (scrollState == OnScrollListener.SCROLL_STATE_IDLE) {
            if (!isSearchBarShowing && getFirstVisiblePosition() == 0) {
                if (getChildAt(0).getTop() == 0) {
                    if (scrollStateChangedEnabled) {
                        isSearchBarShowing = true;
                        mSearchBar.setPadding(0, 0, 0, 0);
                    } else {
                        isSearchBarShowing = false;
                    }
                } else if (getChildAt(0).getTop() <= searchBarHeight / 2) {
                    isSearchBarShowing = true;
                    mSearchBar.setPadding(0, 0, 0, 0);
                    smoothScrollToPosition(0);
                } else {
                    isSearchBarShowing = false;
                    mSearchBar.setPadding(0, -searchBarHeight, 0, 0);
                    smoothScrollToPosition(0);
                }
            } else if (isSearchBarShowing && getFirstVisiblePosition() != 0) {
                isSearchBarShowing = false;
                mSearchBar.setPadding(0, -searchBarHeight, 0, 0);
            }
        }
    }

    @Override
    public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        if (!hasSearchBarHeightMeasured) {
            return super.onTouchEvent(ev);
        }

        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN:
                preY = ev.getY();
                break;
            case MotionEvent.ACTION_MOVE:
                if (!isSearchBarShowing) {
                    if (ev.getY() > preY) {
                        mSearchBar.setPadding(0, ev.getY() - preY <= searchBarHeight ?
                                (int) -(searchBarHeight - (ev.getY() - preY)) : 0, 0, 0);
                    }
                } else {
                    if (ev.getY() < preY) {
                        mSearchBar.setPadding(0, preY - ev.getY() <= searchBarHeight ?
                                (int) -(preY - ev.getY()) : -searchBarHeight, 0, 0);
                    }
                }
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (getFirstVisiblePosition() != 0) {
                    return super.onTouchEvent(ev);
                }

                if (ev.getY() > preY) {
                    isSearchBarShowing = true;
                    mSearchBar.setPadding(0, 0, 0, 0);
                } else if (ev.getY() < preY) {
                    isSearchBarShowing = false;
                    scrollStateChangedEnabled = false;
                    mSearchBar.setPadding(0, -searchBarHeight, 0, 0);
                }
                smoothScrollToPosition(0);
                break;
        }
        return super.onTouchEvent(ev);
    }

}
