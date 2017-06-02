//package com.example.zhouying18.myptrdemo.view;
//
//import android.content.Context;
//import android.util.AttributeSet;
//import android.view.MotionEvent;
//import android.view.View;
//import android.view.ViewGroup;
//import android.widget.LinearLayout;
//
//import in.srain.cube.views.ptr.PtrDefaultHandler2;
//import in.srain.cube.views.ptr.PtrFrameLayout;
//import in.srain.cube.views.ptr.PtrUIHandler;
//
//public abstract class PullToRefreshBaseView extends LinearLayout {
//
//    private final float HEADER_REFRESH_POSITION = 55f;     //刷新Header停留距离
//    private float REAL_HEADER_REFRESH_POSITION;
//
//    {
//        float scale = getResources().getDisplayMetrics().density;
//        REAL_HEADER_REFRESH_POSITION = HEADER_REFRESH_POSITION * scale + 0.5f;
//    }
//
//    //下拉、上拉刷新控件（Ultra-Pull-To-Refresh）
//    private PtrFrameLayout ptrLayout;
//    private View           mContent;
//
//    private boolean hadGetWindowFocus = false;
//    private int currentHeaderHeight = 0;
//
//
//    public PullToRefreshBaseView(Context context) {
//        super(context);
//        onInitMainView();
//        mContent = onInitContent();
//        initView();
//    }
//
//    public PullToRefreshBaseView(Context context, AttributeSet attrs) {
//        super(context, attrs);
//        onInitMainView();
//        mContent = onInitContent();
//        initView();
//    }
//
//    /**
//     * 初始化 PullToRefresh 同层级的View（如：异常提示View）
//     * +++++++++++++++++++++++++++++++++++++++++
//     * +           必须写在该方法内            +
//     * +++++++++++++++++++++++++++++++++++++++++
//     */
//    public abstract void onInitMainView();
//    /**
//     * 初始化 PullToRefresh 的 Context
//     */
//    public abstract View onInitContent();
//
//
//
//    private void initView() {
//        if (mContent == null) {
//            throw new NullPointerException("Content is null : onInitContent() must called and return valid View");
//        }
//
//        setOrientation(VERTICAL);
//
//        ptrLayout = new PtrFrameLayout(getContext());
//        ptrLayout.setDurationToCloseHeader(500);
//        ptrLayout.setDurationToCloseFooter(500);
//        ptrLayout.setKeepHeaderWhenRefresh(true);
//        ptrLayout.setPullToRefresh(false);
//        ptrLayout.setResistance(1.7f);
//        ptrLayout.disableWhenHorizontalMove(true);
//        ptrLayout.addView(mContent);
//        setMode(Mode.BOTH);
//
//        //布局
//        super.addView(ptrLayout, new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
//        //初始化PtrFrameLayout的HeaderView、mContent、FooterView
//        ptrLayout.doFinishInflate();
//
//        if (ptrLayout != null) {
//            ptrLayout.setPtrHandler(new PtrDefaultHandler2() {
//                @Override
//                public void onRefreshBegin(PtrFrameLayout frame) {
//                    if (mOnPullRefreshListener != null) {
//                        mOnPullRefreshListener.onRefresh();
//                    }
//                    if (mOnPullBothListener != null) {
//                        mOnPullBothListener.onPullDownToRefresh();
//                    }
//                }
//
//                @Override
//                public void onLoadMoreBegin(PtrFrameLayout frame) {
//                    if (mOnPullBothListener != null) {
//                        mOnPullBothListener.onPullUpToRefresh();
//                    }
//                }
//
//            });
//        }
//    }
//
//    @Override
//    public void addView(View child, int index, ViewGroup.LayoutParams params) {
//        if(mContent != null && mContent instanceof ViewGroup && !(child instanceof PtrFrameLayout)) {
//            ((ViewGroup) mContent).addView(child, index, params);
//        }else {
//            super.addView(child, index, params);
//        }
//    }
//
//
//
//    /**
//     * 显示下拉的顶部图片
//     */
//    public void showLoadingHeaderImg() {
//        if (ptrLayout != null && ptrLayout.getHeaderView() != null) {
//            if (ptrLayout.getHeaderView() instanceof CommonPtrDefaultHeader) {
//                ((CommonPtrDefaultHeader) ptrLayout.getHeaderView()).showIvTDPtrLoadingHeader();
//            }
//        }
//    }
//
//
//    @Override
//    public void onWindowFocusChanged(boolean hasWindowFocus) {
//        super.onWindowFocusChanged(hasWindowFocus);
//        if (!hadGetWindowFocus && ptrLayout != null) {
//            hadGetWindowFocus = true;
//            currentHeaderHeight = ptrLayout.getHeaderHeight();
//            ptrLayout.setRatioOfHeaderHeightToRefresh(REAL_HEADER_REFRESH_POSITION / currentHeaderHeight);
//            ptrLayout.setOffsetToKeepHeaderWhileLoading((int) REAL_HEADER_REFRESH_POSITION);
//        }
//    }
//
//    /**
//     * 设置默认头部
//     */
//    public void setDefaultLoadingHeaderView() {
//        CommonPtrDefaultHeader myPtrDefaultHeader = new CommonPtrDefaultHeader(getContext());
//        myPtrDefaultHeader.setheaderHeightUpdateListener(new CommonPtrDefaultHeader.HeaderHeightUpdateListener() {
//            @Override
//            public void headerHeightUpdate() {
//                if (ptrLayout != null && ptrLayout.getHeaderHeight() != currentHeaderHeight) {
//                    currentHeaderHeight = ptrLayout.getHeaderHeight();
//                    ptrLayout.setRatioOfHeaderHeightToRefresh(REAL_HEADER_REFRESH_POSITION / currentHeaderHeight);
//                    ptrLayout.setOffsetToKeepHeaderWhileLoading((int) REAL_HEADER_REFRESH_POSITION);
//                }
//            }
//        });
//        setLoadingHeaderView(myPtrDefaultHeader);
//    }
//
//    /**
//     * 设置默认底部
//     */
//    public void setDefaultLoadingFooterView() {
//        setLoadingFooterView(new CommonPtrDefaultFooter(getContext()));
//    }
//
//    /**
//     * 设置头部HeaderView
//     */
//    public void setLoadingHeaderView(PtrUIHandler ptrUIHandler) {
//        if (ptrUIHandler != null) {
//            if (ptrUIHandler instanceof View) {
//                if (ptrLayout != null) {
//                    ptrLayout.setHeaderView((View) ptrUIHandler);
//                    ptrLayout.addPtrUIHandler(ptrUIHandler);
//                }
//            } else {
//                throw new UnsupportedOperationException("ptrUIHandler is not a View so can't setHeaderView");
//            }
//        }
//    }
//
//    /**
//     * 设置底部FooterView
//     */
//    public void setLoadingFooterView(PtrUIHandler ptrUIHandler) {
//        if (ptrUIHandler != null) {
//            if (ptrUIHandler instanceof View) {
//                if (ptrLayout != null) {
//                    ptrLayout.setFooterView((View) ptrUIHandler);
//                    ptrLayout.addPtrUIHandler(ptrUIHandler);
//                }
//            } else {
//                throw new UnsupportedOperationException("ptrUIHandler is not a View so can't setFooterView");
//            }
//        }
//    }
//
//    /**
//     * 自动刷新
//     */
//    public void autoRefresh() {
//        if (ptrLayout != null) {
//            ptrLayout.autoRefresh();
//        }
//    }
//
//    /**
//     * 刷新完成，回弹
//     */
//    public void onRefreshComplete() {
//        if (ptrLayout != null) {
//            ptrLayout.refreshComplete();
//        }
//    }
//
//
//    private OnPullRefreshListener mOnPullRefreshListener;
//    private OnPullBothListener mOnPullBothListener;
//
//    public void setOnRefreshListener(OnPullRefreshListener onPullRefreshListener) {
//        mOnPullRefreshListener = onPullRefreshListener;
//    }
//
//    public void setOnRefreshListener(OnPullBothListener onPullBothListener) {
//        mOnPullBothListener = onPullBothListener;
//    }
//
//    /**
//     * 仅下拉刷新接口
//     */
//    public interface OnPullRefreshListener {
//
//        void onRefresh();
//    }
//
//    /**
//     * 下拉、上拉刷新接口
//     */
//    public interface OnPullBothListener {
//
//        void onPullDownToRefresh();
//
//        void onPullUpToRefresh();
//
//    }
//
//    /**
//     * 上拉到最后一页的提示回调
//     */
//    public interface OnLastPageHintListener {
//
//        void onLastPageHint();
//    }
//
//
//    /**
//     * 自定义封装Mode转第三方库Mode
//     *
//     * @param mode PullToRefreshBaseView.Mode
//     *             <p>
//     *             默认：Mode.BOTH
//     */
//    public void setMode(byte mode) {
//        switch (mode) {
//            case Mode.REFRESH:
//                setMode(PtrFrameLayout.Mode.REFRESH);
//                break;
//            case Mode.LOAD_MORE:
//                setMode(PtrFrameLayout.Mode.LOAD_MORE);
//                break;
//            case Mode.BOTH:
//                setMode(PtrFrameLayout.Mode.BOTH);
//                break;
//            default:
//                setMode(PtrFrameLayout.Mode.NONE);
//                break;
//
//        }
//    }
//
//    private void setMode(PtrFrameLayout.Mode mode) {
//        if (ptrLayout != null) {
//            ptrLayout.setMode(mode);
//        }
//    }
//
//    /**
//     * @return ptrLayout内部可滑动子控件是否滑动到顶部
//     */
//    public boolean checkContentViewScrollTop() {
//        return ptrLayout != null && ptrLayout.checkContentViewScrollTop();
//    }
//
//    /**
//     * @return ptrLayout内部可滑动子控件是否滑动到底部
//     */
//    public boolean checkContentViewScrollBottom() {
//        return ptrLayout != null && ptrLayout.checkContentViewScrollBottom();
//    }
//
//    private float eventX, eventY;
//
//    /**
//     * 处理滑动事件冲突
//     *
//     * @param event
//     * @return
//     */
//    @Override
//    public boolean dispatchTouchEvent(MotionEvent event) {
//        if (ptrLayout != null) {
//            switch (event.getAction()) {
//                case MotionEvent.ACTION_DOWN:
//                    getParent().requestDisallowInterceptTouchEvent(true);
//                    eventX = event.getX();
//                    eventY = event.getY();
//                    break;
//                case MotionEvent.ACTION_MOVE:
//                    if (Math.abs(eventX - event.getX()) > Math.abs(eventY - event.getY())) {
//                        // 横向滑动事件时 滑动事件交予父容器处理
//                        getParent().requestDisallowInterceptTouchEvent(false);
//                    } else if (ptrLayout.getMode() == PtrFrameLayout.Mode.LOAD_MORE && eventY < event.getY() && checkContentViewScrollTop()) {
//                        // 当加载模式为加载更多模式下  可滑动子布局滑动到顶部时 滑动事件交予父容器处理
//                        getParent().requestDisallowInterceptTouchEvent(false);
//                    } else if (ptrLayout.getMode() == PtrFrameLayout.Mode.REFRESH && eventY > event.getY() && checkContentViewScrollBottom()) {
//                        // 当加载模式为下拉刷新模式下  可滑动子布局滑动到底部时 滑动事件交予父容器处理
//                        getParent().requestDisallowInterceptTouchEvent(false);
//                    } else {
//                        getParent().requestDisallowInterceptTouchEvent(true);
//                    }
//                    eventX = event.getX();
//                    eventY = event.getY();
//                    break;
//                case MotionEvent.ACTION_UP:
//                case MotionEvent.ACTION_CANCEL:
//                    getParent().requestDisallowInterceptTouchEvent(false);
//                    break;
//            }
//        }
//        return super.dispatchTouchEvent(event);
//    }
//
//    /**
//     * 刷新类型
//     */
//    public class Mode {
//        /**
//         * 不能刷新
//         */
//        public static final byte NONE = 1;
//
//        /**
//         * 下拉
//         */
//        public static final byte REFRESH = 2;
//
//        /**
//         * 上拉
//         */
//        public static final byte LOAD_MORE = 3;
//
//        /**
//         * 下拉、上拉
//         */
//        public static final byte BOTH = 4;
//
//    }
//}