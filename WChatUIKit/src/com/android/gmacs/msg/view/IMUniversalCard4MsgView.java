package com.android.gmacs.msg.view;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.android.gmacs.R;
import com.android.gmacs.activity.GmacsWebViewActivity;
import com.android.gmacs.utils.GmacsUiUtil;
import com.android.gmacs.view.GmacsDialog;
import com.android.gmacs.view.NetworkImageView;
import com.common.gmacs.msg.IMMessage;
import com.common.gmacs.msg.data.IMUniversalCard4Msg;
import com.common.gmacs.utils.GmacsUtils;
import com.common.gmacs.utils.ImageUtil;

/**
 * Created by zhouwei on 2017/8/7.
 * <p>
 */

public class IMUniversalCard4MsgView extends IMMessageView implements View.OnClickListener, View.OnLongClickListener {
    private final int smallImageSize = GmacsUtils.dipToPixel(48);
    private NetworkImageView mainImageView;
    private TextView mainTitle;
    private LinearLayout itemContainer;
    private NetworkImageView[] itemImageViews;
    private TextView[] itemTextViews;
    private View[] itemSeparators;
    private int itemViewCount;
    private int contentWidth;

    public IMUniversalCard4MsgView(IMMessage mIMMessage) {
        super(mIMMessage);
    }

    @Override
    protected View initView(LayoutInflater inflater, ViewGroup parentView, int maxWidth) {
        if (mIMMessage.message.mIsSelfSendMsg) {
            mContentView = inflater.inflate(R.layout.gmacs_universal_card4_right, parentView, false);
        } else {
            mContentView = inflater.inflate(R.layout.gmacs_universal_card4_left, parentView, false);
        }
        mContentView.setLayoutParams(new LinearLayout.LayoutParams(maxWidth, LinearLayout.LayoutParams.WRAP_CONTENT));
        contentWidth = maxWidth - mContentView.getPaddingLeft() - mContentView.getPaddingRight();
        mainTitle = (TextView) mContentView.findViewById(R.id.tv_title);
        mainImageView = (NetworkImageView) mContentView.findViewById(R.id.iv_pic);

        mContentView.setOnLongClickListener(this);
        mainImageView.setOnClickListener(this);
        mainImageView.setOnLongClickListener(this);

        itemContainer = (LinearLayout) mContentView.findViewById(R.id.ll_container);
        IMUniversalCard4Msg msg = (IMUniversalCard4Msg) mIMMessage;
        if (msg.cardContentItems != null) {
            itemViewCount = msg.cardContentItems.length;
            itemImageViews = new NetworkImageView[itemViewCount];
            itemSeparators = new View[itemViewCount];
            itemTextViews = new TextView[itemViewCount];
            for (int i = 0; i < itemViewCount; ++i) {
                View item = addItemViewAndClickListener(inflater);
                itemImageViews[i] = (NetworkImageView) item.findViewById(R.id.iv_item);
                itemSeparators[i] = item.findViewById(R.id.separation);
                itemTextViews[i] = (TextView) item.findViewById(R.id.tv_item);
            }
        }
        return mContentView;
    }

    private View addItemViewAndClickListener(LayoutInflater inflater) {
        View item = inflater.inflate(R.layout.gmacs_card4_contentarray_item, itemContainer, false);
        itemContainer.addView(item);
        item.setOnClickListener(this);
        item.setOnLongClickListener(this);
        return item;
    }

    @Override
    public void setDataForView(IMMessage imMessage) {
        super.setDataForView(imMessage);

        final IMUniversalCard4Msg msg = (IMUniversalCard4Msg) imMessage;
        mainTitle.setText(msg.cardTitle);
        int height = (int) (contentWidth * 1f / msg.cardPictureWidth * msg.cardPictureHeight);
        mainImageView.setViewHeight(height).setViewWidth(contentWidth);
        RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) mainImageView.getLayoutParams();
        layoutParams.width = contentWidth;
        layoutParams.height = height;
        mainImageView.setLayoutParams(layoutParams);
        mainImageView.setDefaultImageResId(R.color.light_grey)
                .setErrorImageResId(R.color.light_grey)
                .setImageUrl(ImageUtil.makeUpUrl(msg.cardPictureUrl, height, contentWidth));
        mainImageView.setTag(msg);

        if (null != msg.cardContentItems) {
            int itemCount = msg.cardContentItems.length;
            checkAndIncreaseItemCount(itemCount);
            int i;
            for (i = 0; i < itemCount; i++) {
                if (i == itemCount - 1) {
                    // 隐藏最后一个item下面的分割线
                    itemSeparators[i].setVisibility(View.INVISIBLE);
                } else {
                    itemSeparators[i].setVisibility(View.VISIBLE);
                }

                itemTextViews[i].setText(msg.cardContentItems[i].cardSubTitle);

                itemImageViews[i].setDefaultImageResId(R.color.light_grey)
                        .setErrorImageResId(R.color.light_grey)
                        .setImageUrl(ImageUtil.makeUpUrl(msg.cardContentItems[i].cardSubPictureUrl, smallImageSize, smallImageSize));

                itemContainer.getChildAt(i).setVisibility(View.VISIBLE);
                itemContainer.getChildAt(i).setTag(msg.cardContentItems[i]);
            }

            if (i < itemViewCount) {// 隐藏没有显示的剩下的item
                for (int j = i; j < itemViewCount; j++) {
                    itemContainer.getChildAt(j).setVisibility(View.GONE);
                }
            }
        } else {
            for (int i = 0; i < itemViewCount; ++i) {
                itemContainer.getChildAt(i).setVisibility(View.GONE);
            }
        }
    }

    private void checkAndIncreaseItemCount(int itemCount) {
        if (itemViewCount < itemCount) {
            itemImageViews = (NetworkImageView[]) increaseItemToCount(itemImageViews, new NetworkImageView[itemCount]);
            itemTextViews = (TextView[]) increaseItemToCount(itemTextViews, new TextView[itemCount]);
            itemSeparators = (View[]) increaseItemToCount(itemSeparators, new View[itemCount]);
            for (int j = itemViewCount; j < itemCount; ++j) {
                View item = addItemViewAndClickListener(LayoutInflater.from(mChatActivity));
                itemImageViews[j] = (NetworkImageView) item.findViewById(R.id.iv_item);
                itemSeparators[j] = item.findViewById(R.id.separation);
                itemTextViews[j] = (TextView) item.findViewById(R.id.tv_item);
            }
            itemViewCount = itemCount;
        }
    }

    private Object[] increaseItemToCount(Object[] src, Object[] des) {
        if (src != null) {
            System.arraycopy(src, 0, des, 0, src.length);
        }
        return des;
    }

    @Override
    public void onClick(View v) {
        Object o = v.getTag();
        if (o instanceof IMUniversalCard4Msg) {
            Bundle bundle = new Bundle();
            bundle.putString(GmacsWebViewActivity.EXTRA_URL,((IMUniversalCard4Msg) o).cardActionUrl);
            bundle.putString(GmacsWebViewActivity.EXTRA_TITLE, ((IMUniversalCard4Msg) o).cardTitle);
            GmacsUiUtil.startBrowserActivity(mChatActivity,bundle);

//            Intent intent = null;
//            try {
//                intent = new Intent(mChatActivity, Class.forName(GmacsUiUtil.getBrowserClassName()));
//            } catch (ClassNotFoundException e) {
//                e.printStackTrace();
//            }
//            if (intent != null) {
//                intent.putExtra(GmacsWebViewActivity.EXTRA_URL, ((IMUniversalCard4Msg) o).cardActionUrl);
//                intent.putExtra(GmacsWebViewActivity.EXTRA_TITLE, ((IMUniversalCard4Msg) o).cardTitle);
//                mChatActivity.startActivity(intent);
//            }
        } else if (o instanceof IMUniversalCard4Msg.CardContentItem) {
            Bundle bundle = new Bundle();
            bundle.putString(GmacsWebViewActivity.EXTRA_URL,((IMUniversalCard4Msg.CardContentItem) o).cardSubActionUrl);
            bundle.putString(GmacsWebViewActivity.EXTRA_TITLE,((IMUniversalCard4Msg.CardContentItem) o).cardSubTitle);
            GmacsUiUtil.startBrowserActivity(mChatActivity,bundle);

//            Intent intent = null;
//            try {
//                intent = new Intent(mChatActivity, Class.forName(GmacsUiUtil.getBrowserClassName()));
//            } catch (ClassNotFoundException e) {
//                e.printStackTrace();
//            }
//            if (intent != null) {
//                intent.putExtra(GmacsWebViewActivity.EXTRA_URL, ((IMUniversalCard4Msg.CardContentItem) o).cardSubActionUrl);
//                intent.putExtra(GmacsWebViewActivity.EXTRA_TITLE, ((IMUniversalCard4Msg.CardContentItem) o).cardSubTitle);
//                mChatActivity.startActivity(intent);
//            }
        }
    }




    @Override
    public boolean onLongClick(View v) {
        deleteMsg();
        return true;
    }

    private void deleteMsg() {
        final GmacsDialog.Builder dialog = new GmacsDialog.Builder(getContentView().getContext(), GmacsDialog.Builder.DIALOG_TYPE_LIST_NO_BUTTON);
        dialog.initDialog(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                switch (position) {
                    case 0:// 删除消息
                        deleteIMMessageView();
                        dialog.dismiss();
                        break;
                    default:
                        break;
                }
                dialog.dismiss();
            }
        }).setListTexts(new String[]{mChatActivity.getString(R.string.delete_message)})
                .create()
                .show();
    }
}
