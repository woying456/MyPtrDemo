package com.android.gmacs.view;

import android.content.Context;
import android.database.DataSetObserver;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.android.gmacs.R;
import com.android.gmacs.adapter.ViewPagerAdapter;
import com.common.gmacs.utils.GLog;
import com.common.gmacs.utils.GmacsUtils;

import java.util.ArrayList;
import java.util.List;

public class SendMoreLayout extends LinearLayout implements ViewPager.OnPageChangeListener, AdapterView.OnItemClickListener {

    private int[] btnImgResIds;
    private String[] btnTexts;
    private boolean[] btnUnClickable;
    private ViewPager mViewPager;
    private List<View> mViewList;
    private LinearLayout mDotLayout;
    private ArrayList<ImageView> mDotList;
    private OnMoreItemClickListener mcListener;
    private int pageCount = 3;
    private int currentPage = 1;

    public SendMoreLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public SendMoreLayout(Context context) {
        super(context);
    }

    public SendMoreLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    /**
     * There are only 3 default btnImgResIds, <i>R.drawable.gmacs_ic_send_image</i>,
     * <i>R.drawable.gmacs_ic_send_camera</i> and <i>R.drawable.gmacs_ic_send_location</i>.
     * <br><b>We support more than eight buttons, there is a ViewPager will be shown if possible.</b></br>
     *
     * @param imgResIds The resource id of image of buttons.
     */
    public void setBtnImgResIds(int[] imgResIds) {
        btnImgResIds = imgResIds;
    }

    /**
     * There are only 3 default btnTexts, <i>DEFAULT_BTN_TEXT_IMAGE</i>,
     * <i>DEFAULT_BTN_TEXT_CAMERA</i> and <i>DEFAULT_BTN_TEXT_LOCATION</i>.
     * <br><b>We support more than eight buttons, there is a ViewPager will be shown if possible.</b></br>
     *
     * @param texts The unique texts of buttons, which can identify buttons definitely.
     */
    public void setBtnTexts(String[] texts) {
        btnTexts = texts;
        btnUnClickable = new boolean[btnTexts.length];
    }

    public void setUnclickableBtnsPosition(int[] unclickablePosition) {
        for (int position : unclickablePosition) {
            btnUnClickable[position] = true;
        }
    }

    /**
     * If true passed by param and the amount of items below 5, the SendMoreView height would be set as single line.
     * <br>Otherwise, double-line height as default value instead.
     *
     * @param isPreferred
     */
    public void showItemsSingleLinePreferred(boolean isPreferred) {
        //TODO
        if (isPreferred && btnImgResIds.length <= 4) {
            mViewPager.getLayoutParams().height = GmacsUtils.dipToPixel(209.25f / 2 + getResources().getDimension(R.dimen.size_b_b) / 2);
        }
    }

    /**
     * Add buttons' data for their adapter.
     *
     * @param data  The object which carries all the buttons' data.
     * @param start Add data starts from No.start button.
     * @param count Add data ends up to No.start+count button.
     */
    private void addBtnContent(ArrayList<SendMoreAdapterDataStruct> data, int start, int count) {
        for (int i = start; i < count; i++) {
            data.add(new SendMoreAdapterDataStruct(btnImgResIds[i], btnTexts[i], !btnUnClickable[i]));
        }
    }

    /**
     * Notify the data of adapter.
     */
    public void notifyData() {
        mViewPager = (ViewPager) findViewById(R.id.send_more_viewpager);
        mDotLayout = (LinearLayout) findViewById(R.id.send_more_view_dot);
        mDotLayout.removeAllViews();
        if (btnImgResIds != null && btnTexts != null
                && btnImgResIds.length == btnTexts.length
                && btnTexts.length > 0) {
            mViewList = new ArrayList<>();
            if (btnTexts.length >= 8) {
                mDotLayout.setVisibility(LinearLayout.VISIBLE);
            }
            mDotList = new ArrayList<>();
            pageCount = 2 + (btnTexts.length % 8 == 0 ? btnTexts.length / 8 : btnTexts.length / 8 + 1);
            mViewList.add(new View(getContext()));
            for (int i = 0; i < pageCount - 2; i++) {
                GridView gv = (GridView) LayoutInflater.from(getContext()).inflate(R.layout.gmacs_send_more_view, null);
                gv.setOnItemClickListener(this);
                // Add SendMoreView in every page of mViewPager
                mViewList.add(gv);
                ArrayList<SendMoreAdapterDataStruct> data = new ArrayList<>();
                for (int j = i * 8; j < (i + 1) * 8 && j < btnTexts.length; j++) {
                    data.add(new SendMoreAdapterDataStruct(btnImgResIds[j], btnTexts[j], !btnUnClickable[j]));
                }
                gv.setAdapter(new SendMoreAdapter(getContext(), data));
                // Add dot button associated with every page of mViewPager
                ImageView iv = new ImageView(getContext());
                LayoutParams layoutParams = new LayoutParams(
                        new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                layoutParams.leftMargin = GmacsUtils.dipToPixel(5);
                layoutParams.rightMargin = GmacsUtils.dipToPixel(5);
                layoutParams.width = GmacsUtils.dipToPixel(5);
                layoutParams.height = GmacsUtils.dipToPixel(5);
                mDotLayout.addView(iv, layoutParams);
                if (i == 0) {
                    iv.setImageResource(R.drawable.gmacs_ic_emoji_dot_selected);
                } else {
                    iv.setImageResource(R.drawable.gmacs_ic_emoji_dot_default);
                }
                mDotList.add(iv);
            }
            mViewList.add(new View(getContext()));
            mViewPager.setAdapter(new ViewPagerAdapter(mViewList));
            mViewPager.setCurrentItem(currentPage);
            mViewPager.addOnPageChangeListener(this);
        } else {
            GLog.e("SendMoreLayout", "The buttons' texts count is not equal to their imgResIds'.");
        }
    }

    public void registerOnMoreItemClick(OnMoreItemClickListener onMoreItemClickListener) {
        mcListener = onMoreItemClickListener;
    }

    @Override
    public void onPageScrolled(int i, float v, int i1) {

    }

    @Override
    public void onPageSelected(int i) {
        if (i == 0) {
            mViewPager.setCurrentItem(i + 1);
        } else if (i == pageCount - 1) {
            mViewPager.setCurrentItem(i - 1);
        } else {
            currentPage = i;
            if (pageCount > 3) {
                for (int j = 0; j < mDotList.size(); j++) {
                    mDotList.get(j).setImageResource(R.drawable.gmacs_ic_emoji_dot_default);
                }
                mDotList.get(i - 1).setImageResource(R.drawable.gmacs_ic_emoji_dot_selected);
            }
        }
    }

    @Override
    public void onPageScrollStateChanged(int i) {
    }

    /**
     * The Listener binding on SendMsgLayout.mSendMoreView.
     * <br>You can place the default buttons at the position where you'd like to.
     * Maybe exchange position and etc, by changing the default buttons' positions and their tags at the same time.</br>
     */
    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
        if (mcListener != null && !btnUnClickable[position]) {
            mcListener.onMoreItemClick(position + (currentPage - 1) * 8);
        }
    }

    public interface OnMoreItemClickListener {
        void onMoreItemClick(int position);
    }

    /**
     * Associated with SendMoreView.
     */
    private class SendMoreAdapter extends BaseAdapter {

        ArrayList<SendMoreAdapterDataStruct> mData;
        private LayoutInflater li;
        private ViewHolder vh;

        SendMoreAdapter(Context context, ArrayList<SendMoreAdapterDataStruct> data) {
            li = LayoutInflater.from(context);
            mData = data;
        }

        @Override
        public int getCount() {
            return mData.size();
        }

        @Override
        public Object getItem(int position) {
            return mData.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View view, ViewGroup viewGroup) {
            vh = null;
            if (view == null) {
                view = li.inflate(R.layout.gmacs_send_more_item, null);
                vh = new ViewHolder();
                vh.btnImg = (ImageView) view.findViewById(R.id.send_more_item_img);
                vh.btnText = (TextView) view.findViewById(R.id.send_more_item_text);
                view.setTag(vh);
            } else {
                vh = (ViewHolder) view.getTag();
            }

            SendMoreAdapterDataStruct dataStruct = mData.get(position);
            vh.btnImg.setImageResource(dataStruct.btnImgResId);
            vh.btnText.setText(dataStruct.btnTextName);
            vh.btnImg.setAlpha(dataStruct.clickable ? 1 : 0.5f);
            return view;
        }

        @Override
        public void unregisterDataSetObserver(DataSetObserver observer) {
            if (observer != null) {
                super.unregisterDataSetObserver(observer);
            }
        }

        private final class ViewHolder {
            ImageView btnImg;
            TextView btnText;
        }

    }

    private class SendMoreAdapterDataStruct {

        int btnImgResId;
        String btnTextName;
        boolean clickable;

        SendMoreAdapterDataStruct(int btnImgResId, String btnTextName, boolean clickable) {
            this.btnImgResId = btnImgResId;
            this.btnTextName = btnTextName;
            this.clickable = clickable;
        }

    }
}
