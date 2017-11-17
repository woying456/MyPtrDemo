package com.android.gmacs.msg.view;

import android.content.Intent;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.android.gmacs.R;
import com.android.gmacs.logic.MessageLogic;
import com.android.gmacs.view.GmacsDialog;
import com.android.gmacs.view.NetworkImageView;
import com.common.gmacs.msg.IMMessage;
import com.common.gmacs.msg.data.IMImageMsg;
import com.common.gmacs.utils.GmacsEnvi;
import com.common.gmacs.utils.ImageUtil;


public class IMImageMsgView extends IMMessageView {

    public static final int ImgResize = GmacsEnvi.appContext.getResources().getDimensionPixelOffset(R.dimen.im_chat_msg_pic_msg_width);
    public static final int MinResize = GmacsEnvi.appContext.getResources().getDimensionPixelOffset(R.dimen.im_chat_msg_pic_min_size);
    private IMImageMsg imageMsg;
    private NetworkImageView picImage;
    private TextView uploadProgress;
    private boolean doubleClickDelegate;

    public IMImageMsgView(IMMessage mIMMessage) {
        super(mIMMessage);
    }

    @Override
    protected View initView(LayoutInflater inflater, ViewGroup parentView, int maxWidth) {
        if (mIMMessage.message.mIsSelfSendMsg) {
            mContentView = inflater.inflate(R.layout.gmacs_adapter_msg_content_right_picture, parentView, false);
        } else {
            mContentView = inflater.inflate(R.layout.gmacs_adapter_msg_content_left_picture, parentView, false);
        }

        picImage = (NetworkImageView) mContentView.findViewById(R.id.pic);
        uploadProgress = (TextView) mContentView.findViewById(R.id.tv_load_progress);
        picImage.setOnLongClickListener(new View.OnLongClickListener() {
            public boolean onLongClick(View v) {
                final GmacsDialog.Builder dialog = new GmacsDialog.Builder(getContentView().getContext(), GmacsDialog.Builder.DIALOG_TYPE_LIST_NO_BUTTON);
                dialog.setListTexts(new String[]{mChatActivity.getString(R.string.delete_message)}).initDialog(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                        switch (position) {
                            case 0:// 删除消息
                                deleteIMMessageView();
                                dialog.dismiss();
                        }
                    }
                }).create().show();
                return true;
            }
        });

        picImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                // 点击查看图片
                if (mChatActivity.sendMsgLayout.inputSoftIsShow) {
                    mChatActivity.sendMsgLayout.hideInputSoft();
                    v.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            prepareIntentToLaunch(v);
                        }
                    }, 100);
                } else {
                    prepareIntentToLaunch(v);
                }
            }
        });
        return mContentView;
    }

    @Override
    public void setDataForView(IMMessage imMessage) {
        super.setDataForView(imMessage);
        imageMsg = (IMImageMsg) mIMMessage;
        int[] scaleSize = ImageUtil.getScaleSize(imageMsg.mWidth, imageMsg.mHeight, ImgResize, ImgResize, MinResize, MinResize);
        int mWidth = scaleSize[0];
        int mHeight = scaleSize[1];
        int requestWidth = scaleSize[2];
        int requestHeight = scaleSize[3];
        RelativeLayout.LayoutParams para = (RelativeLayout.LayoutParams) picImage.getLayoutParams();
        para.width = mWidth;
        para.height = mHeight;
        picImage.setLayoutParams(para);
        picImage.setViewHeight(mHeight).setViewWidth(mWidth);

        // 是自己发送的消息
        if (imageMsg.message.mIsSelfSendMsg) {
            picImage.setDefaultImageResId(R.drawable.gmacs_img_msg_default_right)
                    .setErrorImageResId(R.drawable.gmacs_img_msg_default_right);
            if (!TextUtils.isEmpty(imageMsg.mLocalUrl)) {
                picImage.setImageUrl(imageMsg.mLocalUrl);
            } else {
                if (imageMsg.mUrl.startsWith("/")) {
                    picImage.setImageUrl(imageMsg.mUrl);
                } else {
                    picImage.setImageUrl(ImageUtil.makeUpUrl(imageMsg.mUrl, requestHeight, requestWidth));
                }
            }
            if (uploadProgress != null) {
                // 消息处于发送中显示进度条
                if (imageMsg.message.isMsgSending()) {
                    imageMsg.setUploadImageListener(MessageLogic.getInstance());
                    uploadProgress.setText((imageMsg.progress > 99 ? 99 : imageMsg.progress) + "%");
                    uploadProgress.setVisibility(View.VISIBLE);
                } else {
                    imageMsg.setUploadImageListener(null);
                    uploadProgress.setVisibility(View.GONE);
                }
            }
        } else {
            picImage.setDefaultImageResId(R.drawable.gmacs_img_msg_default_left)
                    .setErrorImageResId(R.drawable.gmacs_img_msg_default_left)
                    .setImageUrl(ImageUtil.makeUpUrl(imageMsg.mUrl, requestHeight, requestWidth));
        }
    }

    private void prepareIntentToLaunch(View v) {
        if (!doubleClickDelegate) {
            int[] location = new int[2];
            v.getLocationOnScreen(location);
            Intent intent = new Intent();
            intent.putExtra("x", location[0]);
            intent.putExtra("y", location[1]);
            intent.putExtra("width", v.getWidth());
            intent.putExtra("height", v.getHeight());
            doubleClickDelegate = true;
            mChatActivity.launchImageActivity(intent, imageMsg);
            v.postDelayed(new Runnable() {
                @Override
                public void run() {
                    doubleClickDelegate = false;
                }
            }, 500);
        }
    }

}
