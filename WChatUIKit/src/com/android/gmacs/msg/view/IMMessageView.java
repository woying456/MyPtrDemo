package com.android.gmacs.msg.view;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.android.gmacs.activity.GmacsChatActivity;
import com.common.gmacs.msg.IMMessage;

public abstract class IMMessageView {

    protected GmacsChatActivity mChatActivity;
    protected IMMessage mIMMessage;
    protected View mContentView;

    public IMMessageView(IMMessage mIMMessage) {
        this.mIMMessage = mIMMessage;
    }

    /**
     * 模板方法
     *
     * @param parentView
     * @param inflater
     * @param activity
     */
    public View createIMView(ViewGroup parentView, LayoutInflater inflater, GmacsChatActivity activity, int maxWidth) {
        mChatActivity = activity;
        parentView.addView(initView(inflater, parentView, maxWidth));
        return mContentView;
    }

    public View getContentView() {
        return mContentView;
    }

    /**
     * 初始化一个消息view，由子类来实现
     *
     * @param inflater
     * @return
     */
    protected View initView(LayoutInflater inflater, ViewGroup parentView, int maxWidth) {
        return mContentView;
    }

    /**
     * 为view设置数据
     */
    public void setDataForView(IMMessage imMessage) {
        mIMMessage = imMessage;
    }

    /**
     * 删除一条消息
     */
    protected void deleteIMMessageView() {
        if (mIMMessage != null && mIMMessage.message != null) {
            mChatActivity.deleteMessageByLocalId(mIMMessage.message.mLocalId);
        }
    }
}
