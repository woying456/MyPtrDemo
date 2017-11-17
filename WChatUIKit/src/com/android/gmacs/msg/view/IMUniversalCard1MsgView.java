package com.android.gmacs.msg.view;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.android.gmacs.R;
import com.android.gmacs.activity.GmacsWebViewActivity;
import com.android.gmacs.utils.GmacsUiUtil;
import com.android.gmacs.view.GmacsDialog;
import com.common.gmacs.msg.IMMessage;
import com.common.gmacs.msg.data.IMUniversalCard1Msg;

public class IMUniversalCard1MsgView extends IMMessageView implements View.OnClickListener, View.OnLongClickListener {
    private TextView mTvTitle;
    private TextView mTvContent;
    private View mLlContentDetail;

    public IMUniversalCard1MsgView(IMMessage mIMMessage) {
        super(mIMMessage);
    }

    @Override
    protected View initView(LayoutInflater inflater, ViewGroup parentView, int maxWidth) {
        if (mIMMessage.message.mIsSelfSendMsg) {
            mContentView = inflater.inflate(R.layout.gmacs_universal_card1_right, parentView, false);
        } else {
            mContentView = inflater.inflate(R.layout.gmacs_universal_card1_left, parentView, false);
        }
        mContentView.setLayoutParams(new LinearLayout.LayoutParams(maxWidth, LinearLayout.LayoutParams.WRAP_CONTENT));
        mTvTitle = (TextView) mContentView.findViewById(R.id.tv_card_content_title);
        mTvContent = (TextView) mContentView.findViewById(R.id.tv_card_content);
        mLlContentDetail = mContentView.findViewById(R.id.rl_card_content_detail);
        mLlContentDetail.setOnClickListener(this);
        mLlContentDetail.setOnLongClickListener(this);
        mContentView.setOnLongClickListener(this);
        return mContentView;

    }

    @Override
    public void setDataForView(IMMessage imMessage) {
        super.setDataForView(imMessage);
        final IMUniversalCard1Msg cardContentMsg = (IMUniversalCard1Msg) mIMMessage;
        mTvTitle.setText(cardContentMsg.mCardTitle);
        mTvContent.setText(cardContentMsg.mCardContent);
        mLlContentDetail.setTag(cardContentMsg);
    }

    @Override
    public void onClick(View v) {
        IMUniversalCard1Msg cardContentMsg = (IMUniversalCard1Msg) v.getTag();
        if (cardContentMsg != null) {
            Bundle bundle = new Bundle();
            bundle.putString(GmacsWebViewActivity.EXTRA_URL, cardContentMsg.mCardActionUrl);
            bundle.putString(GmacsWebViewActivity.EXTRA_TITLE, cardContentMsg.mCardTitle);
            GmacsUiUtil.startBrowserActivity(mChatActivity,bundle);

//            Intent intent = null;
//            try {
//                intent = new Intent(mChatActivity, Class.forName(GmacsUiUtil.getBrowserClassName()));
//            } catch (ClassNotFoundException e) {
//                e.printStackTrace();
//            }
//            if (intent != null) {
//                intent.putExtra(GmacsWebViewActivity.EXTRA_URL, cardContentMsg.mCardActionUrl);
//                intent.putExtra(GmacsWebViewActivity.EXTRA_TITLE, cardContentMsg.mCardTitle);
//                mChatActivity.startActivity(intent);
//            }
        }

    }

    @Override
    public boolean onLongClick(View v) {
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
        }).setListTexts(new String[]{mChatActivity.getString(R.string.delete_message)}).create().show();
        return true;
    }
}
