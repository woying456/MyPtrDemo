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
import com.common.gmacs.msg.data.IMUniversalCard3Msg;
import com.common.gmacs.utils.ImageUtil;


public class IMUniversalCard3MsgView extends IMMessageView implements View.OnClickListener, View.OnLongClickListener {

    private TextView tv_title;
    private NetworkImageView iv_pic;
    private TextView tv_content;

    private RelativeLayout rl_action;

    private int maxWidth;

    public IMUniversalCard3MsgView(IMMessage mIMMessage) {
        super(mIMMessage);
    }

    @Override
    protected View initView(LayoutInflater inflater, ViewGroup parentView, int maxWidth) {
        this.maxWidth = maxWidth;

        if (mIMMessage.message.mIsSelfSendMsg) {
            mContentView = inflater.inflate(R.layout.gmacs_universal_card3_right, parentView, false);
        } else {
            mContentView = inflater.inflate(R.layout.gmacs_universal_card3_left, parentView, false);
        }

        mContentView.setOnLongClickListener(this);

        mContentView.setLayoutParams(new LinearLayout.LayoutParams(maxWidth, LinearLayout.LayoutParams.WRAP_CONTENT));
        tv_title = (TextView) mContentView.findViewById(R.id.tv_title);
        iv_pic = (NetworkImageView) mContentView.findViewById(R.id.iv_pic);
        tv_content = (TextView) mContentView.findViewById(R.id.tv_content);
        rl_action = (RelativeLayout) mContentView.findViewById(R.id.rl_action);

        rl_action.setOnClickListener(this);
        rl_action.setOnLongClickListener(this);
        return mContentView;
    }

    @Override
    public void setDataForView(IMMessage imMessage) {
        super.setDataForView(imMessage);
        final IMUniversalCard3Msg msg = (IMUniversalCard3Msg) imMessage;
        tv_title.setText(msg.cardTitle);
        tv_content.setText(msg.cardContent);

        int width = maxWidth - mContentView.getPaddingLeft() - mContentView.getPaddingRight();
        int height = (int) (width * 1f * msg.cardPictureHeight / msg.cardPictureWidth);
        iv_pic.setViewHeight(height).setViewWidth(width);
        LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams) iv_pic.getLayoutParams();
        layoutParams.width = width;
        layoutParams.height = height;
        iv_pic.setLayoutParams(layoutParams);

        iv_pic.setDefaultImageResId(R.color.light_grey)
                .setErrorImageResId(R.color.light_grey)
                .setImageUrl(ImageUtil.makeUpUrl(msg.cardPictureUrl, height, width));
        rl_action.setTag(msg);

    }

    @Override
    public void onClick(View v) {
        IMUniversalCard3Msg msg = (IMUniversalCard3Msg) v.getTag();
        if (msg != null) {
            Bundle bundle = new Bundle();
            bundle.putString(GmacsWebViewActivity.EXTRA_URL,msg.cardActionUrl);
            bundle.putString(GmacsWebViewActivity.EXTRA_TITLE, msg.cardTitle);
            GmacsUiUtil.startBrowserActivity(mChatActivity,bundle);

//            Intent intent = null;
//            try {
//                intent = new Intent(mChatActivity, Class.forName(GmacsUiUtil.getBrowserClassName()));
//            } catch (ClassNotFoundException e) {
//                e.printStackTrace();
//            }
//            if (intent != null) {
//                intent.putExtra(GmacsWebViewActivity.EXTRA_URL, msg.cardActionUrl);
//                intent.putExtra(GmacsWebViewActivity.EXTRA_TITLE, msg.cardTitle);
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
