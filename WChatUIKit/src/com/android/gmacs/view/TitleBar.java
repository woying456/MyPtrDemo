package com.android.gmacs.view;

import android.content.Context;
import android.content.res.ColorStateList;
import android.support.annotation.ColorInt;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.android.gmacs.R;

public class TitleBar extends RelativeLayout {
    /**
     * 标题view
     */
    public TextView mTitleView;
    /**
     * 返回按钮
     */
    public ImageView mBackView;

    public TextView mLeftTextView;

    /**
     * 右边的TextView
     */
    public TextView mRightTextView;
    /**
     * 右边的ImageView
     */
    public ImageView mRightImageView;
    public ImageView mRightImageView2;
    private ImageView mCenterIcon;


    public TitleBar(Context context) {
        super(context);
    }

    public TitleBar(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public TitleBar(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        initView();
    }

    protected void initView() {
        mBackView = (ImageView) findViewById(R.id.left_image_btn);
        mBackView.setVisibility(View.VISIBLE);
        mLeftTextView = (TextView) findViewById(R.id.left_text);
        mTitleView = (TextView) findViewById(R.id.center_text);
        mCenterIcon = (ImageView) findViewById(R.id.title_icon);
        mRightTextView = (TextView) findViewById(R.id.right_text_btn);
        mRightImageView = (ImageView) findViewById(R.id.right_image_view);
        mRightImageView2 = (ImageView) findViewById(R.id.right_image_view1);
    }

    /**
     * 设置返回视图的可见性
     *
     * @param visibility
     */
    public void setBackViewVisibility(int visibility) {
        mBackView.setVisibility(visibility);
    }

    /**
     * 返回事件
     *
     * @param listener
     */
    public void setBackListener(OnClickListener listener) {
        mBackView.setOnClickListener(listener);
    }

    /**
     * 设置标题栏
     *
     * @param title
     */
    public void setTitle(CharSequence title) {
        mTitleView.setText(title);
    }

    /**
     * @param resId 文字的id
     */
    public void setTitle(int resId) {
        setTitle(getResources().getText(resId));
    }


    public void setCenterIcon(int resId) {
        if (resId == 0) {
            mCenterIcon.setVisibility(GONE);
        } else {
            mCenterIcon.setVisibility(VISIBLE);
        }
        mCenterIcon.setImageResource(resId);
    }

    /**
     * 设置右边文字文本,文字为空是控件隐藏
     *
     * @param text
     */
    public void setRightText(CharSequence text) {
        mRightTextView.setVisibility(TextUtils.isEmpty(text) ? View.GONE : View.VISIBLE);
        mRightTextView.setText(text);
    }

    public void setRightTextColor(@ColorInt int color) {
        mRightTextView.setTextColor(color);
    }

    public void setRightTextColor(ColorStateList color) {
        mRightTextView.setTextColor(color);
    }

    /**
     * 设置右边文字按键事件
     *
     * @param listener
     */
    public void setRightTextListener(OnClickListener listener) {
        mRightTextView.setOnClickListener(listener);
    }

    /**
     * @param resId
     */
    public void setRightImageView(int resId) {
        mRightImageView.setVisibility(resId == 0 ? View.GONE : View.VISIBLE);
        mRightImageView.setImageResource(resId);
    }

    public void setRightImageViewListener(OnClickListener listener) {
        mRightImageView.setOnClickListener(listener);
    }

    /**
     * @param resId
     */
    public void setSubRightImageView(int resId) {
        mRightImageView2.setVisibility(resId == 0 ? View.GONE : View.VISIBLE);
        mRightImageView2.setImageResource(resId);
    }

    public void setSubRightImageViewListener(OnClickListener listener) {
        mRightImageView2.setOnClickListener(listener);
    }


}
