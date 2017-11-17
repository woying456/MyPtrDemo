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
import com.android.gmacs.view.NetworkImageView;
import com.common.gmacs.msg.IMMessage;
import com.common.gmacs.msg.data.IMUniversalCard2Msg;
import com.common.gmacs.utils.ImageUtil;

import static com.android.gmacs.view.NetworkImageView.IMG_RESIZE;


public class IMUniversalCard2MsgView extends IMMessageView {
    private NetworkImageView mIvCardImg;
    private TextView mTvCardTitle;
    private TextView mTvCardContent;


    public IMUniversalCard2MsgView(IMMessage mIMMessage) {
        super(mIMMessage);
    }

    @Override
    protected View initView(LayoutInflater inflater, ViewGroup parentView, int maxWidth) {

        if (mIMMessage.message.mIsSelfSendMsg) {
            mContentView = inflater.inflate(R.layout.gmacs_universal_card2_right, parentView, false);
        } else {
            mContentView = inflater.inflate(R.layout.gmacs_universal_card2_left, parentView, false);
        }
        mContentView.setLayoutParams(new LinearLayout.LayoutParams(maxWidth, LinearLayout.LayoutParams.WRAP_CONTENT));
        mContentView.setOnLongClickListener(new View.OnLongClickListener() {
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
        });
        mIvCardImg = (NetworkImageView) mContentView.findViewById(R.id.iv_card_img);
        mTvCardTitle = (TextView) mContentView.findViewById(R.id.tv_card_title);
        mTvCardContent = (TextView) mContentView.findViewById(R.id.tv_card_content);
        mContentView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                IMUniversalCard2Msg cardImgContentMsg = (IMUniversalCard2Msg) v.getTag();
                if (cardImgContentMsg != null) {
                    Bundle bundle = new Bundle();
                    bundle.putString(GmacsWebViewActivity.EXTRA_URL, cardImgContentMsg.mCardActionUrl);
                    bundle.putString(GmacsWebViewActivity.EXTRA_TITLE, cardImgContentMsg.mCardTitle);
                    GmacsUiUtil.startBrowserActivity(mChatActivity,bundle);

//                    Intent intent = null;
//                    try {
//                        intent = new Intent(mChatActivity, Class.forName(GmacsUiUtil.getBrowserClassName()));
//                    } catch (ClassNotFoundException e) {
//                        e.printStackTrace();
//                    }
//                    if (intent != null) {
//                        intent.putExtra(GmacsWebViewActivity.EXTRA_URL, cardImgContentMsg.mCardActionUrl);
//                        intent.putExtra(GmacsWebViewActivity.EXTRA_TITLE, cardImgContentMsg.mCardTitle);
//                        mChatActivity.startActivity(intent);
//                    }
                }
            }
        });
        return mContentView;
    }

    @Override
    public void setDataForView(IMMessage imMessage) {
        super.setDataForView(imMessage);
        final IMUniversalCard2Msg cardImgContentMsg = (IMUniversalCard2Msg) mIMMessage;
        mTvCardTitle.setText(cardImgContentMsg.mCardTitle);
        mTvCardContent.setText(cardImgContentMsg.mCardContent);
        mIvCardImg.setDefaultImageResId(R.drawable.wchat_universal_card_img_default)
                .setErrorImageResId(R.drawable.wchat_universal_card_img_default)
                .setImageUrl(ImageUtil.makeUpUrl(cardImgContentMsg.mCardPictureUrl, IMG_RESIZE, IMG_RESIZE));
        mContentView.setTag(cardImgContentMsg);
    }
}
