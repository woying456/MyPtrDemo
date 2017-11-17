package com.android.gmacs.view.listview;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.LinearLayout;

import com.android.gmacs.R;

/**
 * 下拉ListView顶部加载更多
 */
public class GmacsChatListViewHeader extends LinearLayout {
    private LinearLayout mContainer;

    public GmacsChatListViewHeader(Context context) {
        super(context);
        initView(context);
    }

    public GmacsChatListViewHeader(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView(context);
    }

    private void initView(Context context) {
        mContainer = (LinearLayout) LayoutInflater.from(context).inflate(
                R.layout.gmacs_chat_listview_header, this, false);
        addView(mContainer);
    }

    public void setProgressbarGravity(int gravity) {
        mContainer.setGravity(gravity);
    }

    public int getVisibleHeight() {
        return mContainer.getHeight();
    }

    public void setVisibleHeight(int height) {
        if (height < 0) {
            height = 0;
        }
        LayoutParams lp = (LayoutParams) mContainer.getLayoutParams();
        lp.height = height;
        mContainer.setLayoutParams(lp);
    }

}
