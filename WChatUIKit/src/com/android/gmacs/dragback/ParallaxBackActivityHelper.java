package com.android.gmacs.dragback;

import android.app.Activity;
import android.graphics.Canvas;
import android.support.annotation.MainThread;
import android.view.View;

import java.util.ArrayDeque;
import java.util.Iterator;

public class ParallaxBackActivityHelper {
    private static final ArrayDeque<ParallaxBackActivityHelper> sActivities = new ArrayDeque<>();
    private Activity mActivity;
    private ParallaxBackLayout mParallaxBackLayout;

    public ParallaxBackActivityHelper(Activity activity) {
        mActivity = activity;
        mParallaxBackLayout = new ParallaxBackLayout(mActivity);
        sActivities.push(this);
    }

    public static Activity getPeakActivity() {
        if (!sActivities.isEmpty()) {
            return sActivities.peek().getActivity();
        } else {
            return null;
        }
    }

    @MainThread
    public static void finishAllExceptPeek() {
        Iterator iterator = sActivities.descendingIterator();
        int i = 0;
        while (iterator.hasNext() && i < sActivities.size() - 1) {
            ((ParallaxBackActivityHelper) iterator.next()).getActivity().finish();
            i++;
        }
    }

    public Activity getActivity() {
        return mActivity;
    }

    public boolean hasSecondActivity() {
        return sActivities.size() >= 2;
    }

    public void onPostCreate() {
        mParallaxBackLayout.attachToActivity(this);
    }

    public void onActivityDestroy() {
        sActivities.remove(this);
    }

    public ParallaxBackActivityHelper getSecondActivity() {
        ParallaxBackActivityHelper pre = null, cur;
        Iterator descendingIterator = sActivities.descendingIterator();
        while (descendingIterator.hasNext()) {
            cur = (ParallaxBackActivityHelper) descendingIterator.next();
            if (cur.equals(this)) {
                return pre;
            } else {
                pre = cur;
            }
        }
        return null;
    }

    public void drawThumb(Canvas canvas) {
        View decorChild = getBackLayout().getContentView();
        decorChild.draw(canvas);
    }

    public View findViewById(int id) {
        if (mParallaxBackLayout != null) {
            return mParallaxBackLayout.findViewById(id);
        }
        return null;
    }

    public void scrollToFinishActivity() {
        getBackLayout().scrollToFinishActivity();
    }

    public void setBackEnable(boolean enable) {
        getBackLayout().setEnableGesture(enable);
    }

    public ParallaxBackLayout getBackLayout() {
        return mParallaxBackLayout;
    }
}
