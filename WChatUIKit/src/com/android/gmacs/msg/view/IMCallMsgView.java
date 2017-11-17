package com.android.gmacs.msg.view;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.android.gmacs.R;
import com.android.gmacs.logic.CommandLogic;
import com.android.gmacs.logic.MessageLogic;
import com.android.gmacs.view.GmacsDialog;
import com.common.gmacs.core.GmacsConstant;
import com.common.gmacs.msg.IMMessage;
import com.common.gmacs.msg.data.IMCallMsg;
import com.common.gmacs.parse.command.CallCommand;
import com.common.gmacs.parse.contact.UserInfo;
import com.common.gmacs.parse.message.Message;
import com.common.gmacs.utils.TimeUtil;

public class IMCallMsgView extends IMMessageView {

    private TextView mCallContent;
    private ImageView mCallPic;
    private Message.MessageUserInfo senderInfo;
    private Message.MessageUserInfo receiverInfo;

    public IMCallMsgView(IMMessage mIMMessage) {
        super(mIMMessage);
    }

    @Override
    protected View initView(LayoutInflater inflater, ViewGroup parentView, int maxWidth) {
        if (mIMMessage.message.mIsSelfSendMsg) {
            mContentView = inflater.inflate(R.layout.gmacs_adapter_msg_content_right_call, parentView, false);
        } else {
            mContentView = inflater.inflate(R.layout.gmacs_adapter_msg_content_left_call, parentView, false);
        }
        mCallContent = (TextView) mContentView.findViewById(R.id.tv_msg_call);
        mCallPic = (ImageView) mContentView.findViewById(R.id.iv_call);
        RelativeLayout mCallRoot = (RelativeLayout) mContentView.findViewById(R.id.rl_talk_item_call);

        mCallRoot.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!mIMMessage.message.mIsSelfSendMsg && mIMMessage.message.getMsgPlayStatus() == GmacsConstant.MSG_NOT_PLAYED) {
                    mIMMessage.message.setMsgPlayStatus(GmacsConstant.MSG_PLAYED);
                    Message message = mIMMessage.message;
                    Message.MessageUserInfo otherInfo = message.getTalkOtherUserInfo();
                    MessageLogic.getInstance().updatePlayStatusByLocalId(otherInfo.mUserId, otherInfo.mUserSource, message.mLocalId, GmacsConstant.MSG_PLAYED);
                }
                CallCommand callCommand;
                String otherId;
                int otherSource;
                String otherAvatar = null;
                String otherName = null;
                if (mIMMessage.message.mIsSelfSendMsg) {
                    otherId = receiverInfo.mUserId;
                    otherSource = receiverInfo.mUserSource;
                } else {
                    otherId = senderInfo.mUserId;
                    otherSource = senderInfo.mUserSource;
                }
                UserInfo info = mChatActivity.getOtherUserInfo();
                if (info != null) {
                    otherAvatar = info.avatar;
                    otherName = info.getNameToShow();
                }

                switch (((IMCallMsg) mIMMessage).callType) {
                    case IMCallMsg.CALL_TYPE_AUDIO:
                        callCommand = CallCommand.getInitiatorCallCommand(CallCommand.CALL_TYPE_AUDIO, otherId, otherSource, otherAvatar, otherName, CommandLogic.getInstance().getmWRTCExtend());
                        break;
                    case IMCallMsg.CALL_TYPE_VIDEO:
                        callCommand = CallCommand.getInitiatorCallCommand(CallCommand.CALL_TYPE_VIDEO, otherId, otherSource, otherAvatar, otherName, CommandLogic.getInstance().getmWRTCExtend());
                        break;
                    case IMCallMsg.CALL_TYPE_IP:
                        callCommand = CallCommand.getInitiatorCallCommand(CallCommand.CALL_TYPE_IP, otherId, otherSource, otherAvatar, otherName, CommandLogic.getInstance().getmWRTCExtend());
                        break;
                    case IMCallMsg.CALL_TYPE_MIX:
                    default:
                        callCommand = CallCommand.getInitiatorMixedAudioCallCommand(otherId, otherSource, otherAvatar, otherName, null);
                        break;
                }
                CommandLogic.getInstance().startCall(callCommand);
            }
        });
        mCallRoot.setOnLongClickListener(new View.OnLongClickListener() {
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
        return mContentView;
    }


    @Override
    public void setDataForView(IMMessage imMessage) {
        super.setDataForView(imMessage);
        receiverInfo = imMessage.message.mReceiverInfo;
        senderInfo = imMessage.message.mSenderInfo;
        IMCallMsg imCallMsg = (IMCallMsg) mIMMessage;
        if (imCallMsg.message.mIsSelfSendMsg) {
            if (imCallMsg.callType == IMCallMsg.CALL_TYPE_AUDIO) {
                mCallPic.setImageResource(R.drawable.gmacs_talk_item_audio_call);
            } else if (imCallMsg.callType == IMCallMsg.CALL_TYPE_VIDEO) {
                mCallPic.setImageResource(R.drawable.gmacs_talk_item_video_call);
            } else {
                mCallPic.setImageResource(R.drawable.gmacs_talk_item_ip_call);
            }
            switch (imCallMsg.finalState) {
                case IMCallMsg.CANCELED:
                    mCallContent.setText(R.string.finalState_self_cancel);
                    break;
                case IMCallMsg.HANG_UP:
                    mCallContent.setText(mCallContent.getContext().getString(R.string.finalState_self_chat_time, TimeUtil.secondsToChatTime(imCallMsg.durationInSeconds)));
                    break;
                case IMCallMsg.REFUSED:
                    mCallContent.setText(R.string.finalState_other_refuse);
                    break;
                case IMCallMsg.TIME_OUT:
                    if (imCallMsg.callType == IMCallMsg.CALL_TYPE_IP
                            || imCallMsg.callType == IMCallMsg.CALL_TYPE_MIX) {
                        mCallContent.setText(R.string.finalState_other_ip_call_no_answer);
                    } else {
                        mCallContent.setText(R.string.finalState_other_no_answer);
                    }
                    break;
                case IMCallMsg.BUSY:
                    mCallContent.setText(R.string.finalState_other_busy);
                    break;
                case IMCallMsg.FAILED:
                    mCallContent.setText(R.string.finalState_other_fail);
                    break;
            }
        } else {
            switch (imCallMsg.finalState) {
                case IMCallMsg.HANG_UP:
                    if (imCallMsg.callType == IMCallMsg.CALL_TYPE_AUDIO) {
                        mCallPic.setImageResource(R.drawable.gmacs_talk_item_audio_call);
                    } else if (imCallMsg.callType == IMCallMsg.CALL_TYPE_VIDEO) {
                        mCallPic.setImageResource(R.drawable.gmacs_talk_item_video_call);
                    } else {
                        mCallPic.setImageResource(R.drawable.gmacs_talk_item_ip_call);
                    }
                    mCallContent.setText(mCallContent.getContext().getString(R.string.finalState_self_chat_time, TimeUtil.secondsToChatTime(imCallMsg.durationInSeconds)));
                    mCallContent.setTextColor(0xff000000);
                    break;
                case IMCallMsg.REFUSED:
                    if (imCallMsg.callType == IMCallMsg.CALL_TYPE_AUDIO) {
                        mCallPic.setImageResource(R.drawable.gmacs_talk_item_audio_call);
                    } else if (imCallMsg.callType == IMCallMsg.CALL_TYPE_VIDEO) {
                        mCallPic.setImageResource(R.drawable.gmacs_talk_item_video_call);
                    } else {
                        mCallPic.setImageResource(R.drawable.gmacs_talk_item_ip_call);
                    }
                    mCallContent.setText(R.string.finalState_self_refuse);
                    mCallContent.setTextColor(0xff000000);
                    break;
                case IMCallMsg.TIME_OUT:
                case IMCallMsg.FAILED:
                case IMCallMsg.BUSY:
                case IMCallMsg.CANCELED:
                    switch (imCallMsg.callType) {
                        case IMCallMsg.CALL_TYPE_IP:
                        case IMCallMsg.CALL_TYPE_MIX:
                            mCallPic.setImageResource(R.drawable.gmacs_talk_item_ip_call_missed);
                            break;
                        case IMCallMsg.CALL_TYPE_AUDIO:
                            mCallPic.setImageResource(R.drawable.gmacs_talk_item_audio_missed);
                            break;
                        case IMCallMsg.CALL_TYPE_VIDEO:
                            mCallPic.setImageResource(R.drawable.gmacs_talk_item_video_missed);
                            break;
                    }
                    mCallContent.setText(R.string.finalState_other_missed_call);
                    mCallContent.setTextColor(0xfff43d32);
                    break;
            }
        }
    }
}
