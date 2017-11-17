package com.andview.refreshview;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.andview.refreshview.callback.IHeaderCallBack;

public class XRefreshViewHeader extends LinearLayout implements IHeaderCallBack {
    private ViewGroup mContent;
    private ImageView mArrowImageView;
    private ProgressBar mProgressBar;
    private TextView mHintTextView;
    private TextView mHeaderTimeTextView;
    private Animation mRotateUpAnim;
    private Animation mRotateDownAnim;
    private final int ROTATE_ANIM_DURATION = 180;

    public XRefreshViewHeader(Context context) {
        super(context);
        initView(context);
    }

    /**
     * @param context
     * @param attrs
     */
    public XRefreshViewHeader(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView(context);
    }

    private void initView(Context context) {
        mContent = (ViewGroup) LayoutInflater.from(context).inflate(
                R.layout.xlistview_header, this);
        mArrowImageView = (ImageView) findViewById(R.id.xlistview_header_arrow);
        mHintTextView = (TextView) findViewById(R.id.xlistview_header_hint_textview);
        mProgressBar = (ProgressBar) findViewById(R.id.xlistview_header_progressbar);
        mHeaderTimeTextView = (TextView) findViewById(R.id.xlistview_header_time);

        mRotateUpAnim = new RotateAnimation(0.0f, -180.0f,
                Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        mRotateUpAnim.setDuration(ROTATE_ANIM_DURATION);
        mRotateUpAnim.setFillAfter(true);
        mRotateDownAnim = new RotateAnimation(-180.0f, 0.0f,
                Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF,
                0.5f);
        mRotateDownAnim.setDuration(180);
        mRotateDownAnim.setFillAfter(true);
    }

    public void setRefreshTime(String lastRefreshTime) {
        mHeaderTimeTextView.setText(lastRefreshTime);
    }

    /**
     * hide footer when disable pull refresh
     */
    public void hide() {
        setVisibility(View.GONE);
    }

    public void show() {
        setVisibility(View.VISIBLE);
    }

    @Override
    public void onStateNormal() {
        mProgressBar.setVisibility(View.GONE);
        mArrowImageView.setVisibility(View.VISIBLE);
        mArrowImageView.startAnimation(mRotateDownAnim);
        mArrowImageView.setImageResource(R.drawable.arrow_down);
        mHintTextView.setText("下拉刷新");
    }

    @Override
    public void onStateReady() {
        mProgressBar.setVisibility(View.GONE);
        mArrowImageView.setVisibility(View.VISIBLE);
        mArrowImageView.clearAnimation();
        mArrowImageView.startAnimation(mRotateUpAnim);
        mArrowImageView.setImageResource(R.drawable.arrow_down);
        mHintTextView.setText("松开刷新");
    }

    @Override
    public void onStateRefreshing() {
        mArrowImageView.clearAnimation();
        mArrowImageView.setVisibility(View.GONE);
        mProgressBar.setVisibility(View.VISIBLE);
        mHintTextView.setText("正在加载");
    }

    @Override
    public void onStateFinish(boolean success) {
        mArrowImageView.setVisibility(View.VISIBLE);
        mArrowImageView.setImageResource(R.drawable.arrow_ok);
        mProgressBar.setVisibility(View.GONE);
        mHintTextView.setText("刷新成功");
    }

    @Override
    public void onHeaderMove(double headerMovePercent, int offsetY, int deltaY) {

    }

    @Override
    public int getHeaderHeight() {
        return getMeasuredHeight();
    }
}
