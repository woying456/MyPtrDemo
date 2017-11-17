package com.android.gmacs.view;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.LinearLayout;

import com.android.gmacs.R;
import com.android.gmacs.observer.OnInputSoftListener;

public class ResizeLayout extends LinearLayout {


    // 定义默认的软键盘最小高度，这是为了避免onSizeChanged在某些下特殊情况下出现的问题
    private final int SOFTKEYPAD_MIN_HEIGHT = 2 * getResources().getDimensionPixelOffset(R.dimen.navigation_bar_height);

    private OnInputSoftListener onInputSoftListener;

    public ResizeLayout(Context context) {
        super(context);
    }

    public ResizeLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void setInputSoftListener(OnInputSoftListener listener) {
        onInputSoftListener = listener;
    }

    @Override
    protected void onSizeChanged(int w, final int h, int oldw, final int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        post(new Runnable() {
            @Override
            public void run() {
                if (oldh - h > SOFTKEYPAD_MIN_HEIGHT) {
                    onInputSoftListener.onShow();
                } else if (h - oldh > SOFTKEYPAD_MIN_HEIGHT) {
                    onInputSoftListener.onHide();
                }
            }
        });
    }
}
