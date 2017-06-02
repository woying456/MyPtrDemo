/**
 * @file XFooterView.java
 * @create Mar 31, 2012 9:33:43 PM
 * @author Maxwin
 * @description XListView's footer
 */
package in.srain.cube.views.ptr;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import in.srain.cube.views.ptr.indicator.PtrIndicator;

public class XListViewFooter extends LinearLayout implements PtrUIHandler{
    public final static int STATE_NORMAL = 0;
    public final static int STATE_READY = 1;
    public final static int STATE_LOADING = 2;

    private Context mContext;

    private View     mContentView;
    private View     mProgressBar;
    private TextView mHintView;

    private View line;

    public XListViewFooter(Context context) {
        super(context);
        initView(context);
    }

    public XListViewFooter(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView(context);
    }

    public void setState(int state) {
        mHintView.setVisibility(View.INVISIBLE);
        mProgressBar.setVisibility(View.INVISIBLE);
        mHintView.setVisibility(View.INVISIBLE);
        if (state == STATE_READY) {
            mHintView.setVisibility(View.VISIBLE);
            mHintView.setText("松开载入更多");
        } else if (state == STATE_LOADING) {
            mProgressBar.setVisibility(View.VISIBLE);
        } else {
            mHintView.setVisibility(View.VISIBLE);
            mHintView.setText("查看更多");
        }
    }

    public void setBottomMargin(int height) {
        if (height < 0)
            return;
        LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) mContentView
                .getLayoutParams();
        lp.bottomMargin = height;
        mContentView.setLayoutParams(lp);
    }

    public int getBottomMargin() {
        LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) mContentView
                .getLayoutParams();
        return lp.bottomMargin;
    }

    /**
     * normal status
     */
    public void normal() {
        mHintView.setVisibility(View.VISIBLE);
        mProgressBar.setVisibility(View.GONE);
    }

    /**
     * loading status
     */
    public void loading() {
        mHintView.setVisibility(View.GONE);
        mProgressBar.setVisibility(View.VISIBLE);
    }

    /**
     * hide footer when disable pull load more
     */
    public void hide() {
        LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) mContentView
                .getLayoutParams();
        lp.height = 0;
        mContentView.setLayoutParams(lp);
    }

    /**
     * show footer
     */
    public void show() {
        LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) mContentView
                .getLayoutParams();
        lp.height = LayoutParams.WRAP_CONTENT;
        mContentView.setLayoutParams(lp);
    }

    private void initView(Context context) {
        mContext = context;
        LinearLayout moreView = (LinearLayout) LayoutInflater.from(mContext)
                .inflate(R.layout.xlistview_footer, null);
        addView(moreView);
        moreView.setLayoutParams(new LinearLayout.LayoutParams(
                LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));

        mContentView = moreView.findViewById(R.id.xlistview_footer_content);
        mProgressBar = moreView.findViewById(R.id.xlistview_footer_progressbar);
        mHintView = (TextView) moreView
                .findViewById(R.id.xlistview_footer_hint_textview);
        line = moreView.findViewById(R.id.line);
    }

    public void hideLine() {
        line.setVisibility(View.GONE);
    }

    public void showLine() {
        line.setVisibility(View.VISIBLE);
    }

    public void unMarginLine() {
        LinearLayout.LayoutParams layoutParams = (LayoutParams) line.getLayoutParams();
        layoutParams.leftMargin = 0;
        layoutParams.rightMargin = 0;
    }

    @Override
    public void onUIReset(PtrFrameLayout frame) {

    }

    @Override
    public void onUIRefreshPrepare(PtrFrameLayout frame) {
        mHintView.setVisibility(View.VISIBLE);
        mHintView.setText("松开载入更多");
    }

    @Override
    public void onUIRefreshBegin(PtrFrameLayout frame) {
        mHintView.setVisibility(View.INVISIBLE);
        mProgressBar.setVisibility(View.VISIBLE);
    }

    @Override
    public void onUIRefreshComplete(PtrFrameLayout frame, boolean isHeader) {
        mProgressBar.setVisibility(View.INVISIBLE);
        mHintView.setVisibility(View.VISIBLE);
        mHintView.setText("查看更多");
    }

    @Override
    public void onUIPositionChange(PtrFrameLayout frame, boolean isUnderTouch, byte status, PtrIndicator ptrIndicator) {

    }
}
