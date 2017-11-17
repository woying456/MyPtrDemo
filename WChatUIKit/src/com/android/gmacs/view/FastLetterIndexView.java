package com.android.gmacs.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import com.android.gmacs.R;
import com.common.gmacs.utils.GmacsUtils;

import java.util.ArrayList;

public class FastLetterIndexView extends View {

    private final int LETTER_COLOR_NORMAL;
    private final float FIRST_INDEX_PADDING_TOP = 4; // 第一个定位符与阴影背景最上面的间距
    private final float LAST_INDEX_PADDING_BOTTOM = 4; // 最后一个定位符与阴影背景最下面的间距

    private Paint mPaint;
    private Rect mRectScrollerBg;
    private boolean isTouchingDown;
    private OnTouchLetterListener mOnTouchLetterListener;
    private ArrayList<String> lettersList;
    private int lettersLength;
    private int maxDisplayHeight = 0;
    private float lastIndexY;


    public FastLetterIndexView(Context context) {
        this(context, null);
    }

    public FastLetterIndexView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public FastLetterIndexView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        LETTER_COLOR_NORMAL = context.getResources().getColor(R.color.gray_dark);
        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setColor(LETTER_COLOR_NORMAL);
        mPaint.setStyle(Paint.Style.FILL);
        mPaint.setTextAlign(Paint.Align.CENTER);
        mRectScrollerBg = new Rect();
    }

    public void setMaxDisplayHeight(int height) {
        this.maxDisplayHeight = height;
    }

    public void setLetter(ArrayList<String> letters) {
        lettersList = letters;
        lettersLength = lettersList.size();
        if (lettersLength < 2) {
            setVisibility(GONE);
        } else {
            setVisibility(VISIBLE);
            requestLayout();
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (maxDisplayHeight > 0) {
            if (lettersLength * GmacsUtils.dipToPixel(25) >= maxDisplayHeight) {
                setMeasuredDimension(getDefaultSize(getSuggestedMinimumWidth(), widthMeasureSpec),
                        maxDisplayHeight - GmacsUtils.dipToPixel(25 * 4));
                return;
            }
        }
        setMeasuredDimension(getDefaultSize(getSuggestedMinimumWidth(), widthMeasureSpec),
                lettersLength * GmacsUtils.dipToPixel(25));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (isTouchingDown) {
            mRectScrollerBg.set(0, 0, getWidth(), getHeight());
            invalidate(mRectScrollerBg);
            mPaint.setColor(LETTER_COLOR_NORMAL);
        }

        float finalHeight = getHeight() - getPaddingTop() - getPaddingBottom();
        float textSize = getWidth() - getPaddingRight() - getPaddingLeft();
        float spacingHeight = (finalHeight - FIRST_INDEX_PADDING_TOP - LAST_INDEX_PADDING_BOTTOM
                - (textSize * lettersLength)) / lettersLength;
        float textX = getWidth() / 2;
        float textY;
        mPaint.setTextSize(textSize);

        for (int i = 0; i < lettersLength; i++) {
            textY = (getPaddingTop() + FIRST_INDEX_PADDING_TOP) + (spacingHeight + textSize) * (i + 1);
            if (lettersLength - 2 == i) {
                lastIndexY = textY + (spacingHeight / 2);
            }
            canvas.drawText(lettersList.get(i), textX, textY, mPaint);
        }
        mRectScrollerBg.setEmpty();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                isTouchingDown = true;
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                isTouchingDown = false;
                break;
            case MotionEvent.ACTION_MOVE:
                invalidate();
                break;
        }
        if (null != mOnTouchLetterListener) {
            float y = event.getY();
            int pos;
            if (y >= lastIndexY) {
                pos = lettersLength - 1;
            } else {
                float itemHeight = lastIndexY / (lettersLength - 1);
                pos = (int) (y / itemHeight);
                if (pos <= 0) {
                    pos = 0;
                } else if (pos >= lettersLength - 1) {
                    pos = lettersLength - 1;
                }
            }
            mOnTouchLetterListener.onTouchLetter(event, pos, lettersList.get(pos));
        }
        return true;
    }

    public void setOnTouchLetterListener(OnTouchLetterListener onTouchLetterListener) {
        mOnTouchLetterListener = onTouchLetterListener;
    }

    public interface OnTouchLetterListener {
        void onTouchLetter(MotionEvent event, int index, String letterIndex);
    }

}
