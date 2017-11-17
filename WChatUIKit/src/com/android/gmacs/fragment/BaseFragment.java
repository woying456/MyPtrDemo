package com.android.gmacs.fragment;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public abstract class BaseFragment extends Fragment {
    protected String TAG = this.getClass().getSimpleName();
    protected int layoutResID;

    /**
     * 缓存view 避免重复创建view
     */
    protected View rootView;

    /**
     * 标识view是不是第一次创建
     */
    private boolean isFirstCreated = true;

    /**
     * 设置显示的布局
     */
    protected abstract void setContentView();

    /**
     * 初始化view
     */
    protected abstract void initView();

    /**
     * 初始化数据
     */
    protected abstract void initData();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (null == this.rootView) {
            setContentView();
            this.rootView = inflater.inflate(layoutResID, container, false);
        }
        return this.rootView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (isFirstCreated) {
            initView();
            initData();
            isFirstCreated = false;
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        rootView = null;
    }

}
