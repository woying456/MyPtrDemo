/**
 * @file XListViewHeader.java
 * @create Apr 18, 2012 5:22:27 PM
 * @author Maxwin
 * @description XListView's header
 */
package in.srain.cube.views.ptr;

import android.content.Context;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import in.srain.cube.views.ptr.indicator.PtrIndicator;

public class XListViewHeader extends LinearLayout implements PtrUIHandler {
    private LinearLayout mContainer;
    private ImageView    mArrowImageView;
    private ProgressBar  mProgressBar;
    private TextView     mHintTextView;
    private int mState = STATE_NORMAL;

    private Animation mRotateUpAnim;
    private Animation mRotateDownAnim;

    private final int ROTATE_ANIM_DURATION = 180;

    public final static int STATE_NORMAL     = 0;
    public final static int STATE_READY      = 1;
    public final static int STATE_REFRESHING = 2;
    public final static int STATE_OK         = 3;

    private View    line;
    private boolean mShouldShowLastUpdate;

    public XListViewHeader(Context context) {
        super(context);
        initView(context);
    }

    /**
     * @param context
     * @param attrs
     */
    public XListViewHeader(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView(context);
    }

    private void initView(Context context) {
        mContainer = (LinearLayout) LayoutInflater.from(context).inflate(
                R.layout.xlistview_header, null);
        addView(mContainer);
        setGravity(Gravity.BOTTOM);

        mArrowImageView = (ImageView) findViewById(R.id.xlistview_header_arrow);
        mHintTextView = (TextView) findViewById(R.id.xlistview_header_hint_textview);
        mProgressBar = (ProgressBar) findViewById(R.id.xlistview_header_progressbar);

        mRotateUpAnim = new RotateAnimation(0.0f, -180.0f,
                Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF,
                0.5f);
        mRotateUpAnim.setDuration(ROTATE_ANIM_DURATION);
        mRotateUpAnim.setFillAfter(true);
        mRotateDownAnim = new RotateAnimation(-180.0f, 0.0f,
                Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF,
                0.5f);
        mRotateDownAnim.setDuration(ROTATE_ANIM_DURATION);
        mRotateDownAnim.setFillAfter(true);
        line = mContainer.findViewById(R.id.line);
    }

    public void setState(int state) {
        if (state == mState)
            return;

        if (state == STATE_REFRESHING) { // 显示进度
            mArrowImageView.clearAnimation();
            mArrowImageView.setVisibility(View.INVISIBLE);
            mProgressBar.setVisibility(View.VISIBLE);
        } else { // 显示箭头图片
            mArrowImageView.setVisibility(View.VISIBLE);
            mProgressBar.setVisibility(View.INVISIBLE);
        }

        switch (state) {
            case STATE_NORMAL:
                mArrowImageView.setImageResource(R.drawable.arrow_down);
                if (mState == STATE_READY) {
                    mArrowImageView.startAnimation(mRotateDownAnim);
                }
                if (mState == STATE_REFRESHING) {
                    mArrowImageView.clearAnimation();
                }
                mHintTextView.setText("下拉刷新");
                break;
            case STATE_READY:
                if (mState != STATE_READY) {
                    mArrowImageView.clearAnimation();
                    mArrowImageView.startAnimation(mRotateUpAnim);
                    mHintTextView.setText("松开刷新");
                }
                break;
            case STATE_REFRESHING:
                mHintTextView.setText("正在加载");
                break;
            case STATE_OK:
                mArrowImageView.setImageResource(R.drawable.arrow_ok);
                mHintTextView.setText("刷新成功");
                break;
            default:
        }

        mState = state;
    }

    public void setVisiableHeight(int height) {
        if (height < 0)
            height = 0;
        LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) mContainer
                .getLayoutParams();
        lp.height = height;
        mContainer.setLayoutParams(lp);
    }

    public int getVisiableHeight() {
        return mContainer.getHeight();
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
        hideRotateView();
        mProgressBar.setVisibility(INVISIBLE);
    }

    @Override
    public void onUIRefreshPrepare(PtrFrameLayout frame) {
        mProgressBar.setVisibility(INVISIBLE);

        mArrowImageView.setVisibility(VISIBLE);
        mArrowImageView.setImageResource(R.drawable.arrow_down);
        mHintTextView.setText("下拉刷新");
    }

    @Override
    public void onUIRefreshBegin(PtrFrameLayout frame) {
        hideRotateView();
        mProgressBar.setVisibility(VISIBLE);
        mHintTextView.setText("正在加载");

    }

    @Override
    public void onUIRefreshComplete(PtrFrameLayout frame, boolean isHeader) {
        if (!isHeader) {
            return;
        }
        mArrowImageView.setVisibility(VISIBLE);
        mArrowImageView.setImageResource(R.drawable.arrow_ok);
        mProgressBar.setVisibility(INVISIBLE);

        mHintTextView.setText("刷新成功");
    }

    @Override
    public void onUIPositionChange(PtrFrameLayout frame, boolean isUnderTouch, byte status, PtrIndicator ptrIndicator) {

        final int mOffsetToRefresh = frame.getOffsetToRefresh();
        final int currentPos = ptrIndicator.getCurrentPosY();
        final int lastPos = ptrIndicator.getLastPosY();

        if (currentPos < mOffsetToRefresh && lastPos >= mOffsetToRefresh) {
            if (isUnderTouch && status == PtrFrameLayout.PTR_STATUS_PREPARE) {
                crossRotateLineFromBottomUnderTouch(frame);
                if (mArrowImageView != null) {
                    mArrowImageView.clearAnimation();
                    mArrowImageView.startAnimation(mRotateDownAnim);
                }
            }
        } else if (currentPos > mOffsetToRefresh && lastPos <= mOffsetToRefresh) {
            if (isUnderTouch && status == PtrFrameLayout.PTR_STATUS_PREPARE) {
                crossRotateLineFromTopUnderTouch(frame);
                if (mArrowImageView != null) {
                    mArrowImageView.clearAnimation();
                    mArrowImageView.startAnimation(mRotateUpAnim);
                }
            }
        }
    }

    private void crossRotateLineFromTopUnderTouch(PtrFrameLayout frame) {
        if (!frame.isPullToRefresh()) {
            mHintTextView.setText("松开刷新");
        }
    }

    private void crossRotateLineFromBottomUnderTouch(PtrFrameLayout frame) {
        mHintTextView.setText("下拉刷新");
    }

    private void hideRotateView() {
        mArrowImageView.clearAnimation();
        mArrowImageView.setVisibility(INVISIBLE);
    }
}
