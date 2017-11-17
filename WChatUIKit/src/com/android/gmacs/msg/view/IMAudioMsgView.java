package com.android.gmacs.msg.view;

import android.content.Intent;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.Drawable;
import android.media.MediaPlayer;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.gmacs.R;
import com.android.gmacs.activity.GmacsChatActivity;
import com.android.gmacs.downloader.RequestManager;
import com.android.gmacs.downloader.Response;
import com.android.gmacs.downloader.VolleyError;
import com.android.gmacs.downloader.audio.AudioRequest;
import com.android.gmacs.logic.CommandLogic;
import com.android.gmacs.logic.MessageLogic;
import com.android.gmacs.sound.SoundPlayer;
import com.android.gmacs.utils.GmacsUiUtil;
import com.android.gmacs.view.GmacsDialog;
import com.common.gmacs.core.GmacsConstant;
import com.common.gmacs.msg.IMMessage;
import com.common.gmacs.msg.data.IMAudioMsg;
import com.common.gmacs.msg.data.IMCallMsg;
import com.common.gmacs.parse.message.Message;
import com.common.gmacs.utils.GmacsUtils;
import com.common.gmacs.utils.ToastUtil;

import java.lang.ref.WeakReference;

public class IMAudioMsgView extends IMMessageView {

    /**
     * 声音view每一份的增长长度，声音view的宽度是与声音时长有关系的
     */
    private static int sWidthOnce = -1;
    // 声音view的最大最小宽度
    private static int sVoiceViewMinWidth = -1, sVoiceViewMaxWidth = -1;
    private final String FIRST_CONVERT_TEXT = "isFirstConvertText";
    private ImageView downFailed;
    private ImageView msgVoiceIv;
    private ImageView noRead;
    private TextView duration;
    private View playImageLayout;

    public IMAudioMsgView(IMMessage mIMMessage) {
        super(mIMMessage);
    }

    private void setData(String url) {
        if (downFailed != null) {
            downFailed.setVisibility(View.GONE);
        }
        msgVoiceIv.setVisibility(View.VISIBLE);
        msgVoiceIv.setTag(url);
        mContentView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (CommandLogic.getInstance().isChatting()) {
                    int chattingType = CommandLogic.getInstance().getChattingType();
                    ToastUtil.showToast(v.getContext().getResources().getString(R.string.calling,
                            chattingType == IMCallMsg.CALL_TYPE_VIDEO ? "视频" : "音频"));
                    return;
                }

                // 把消息置为已经读过
                if (!mIMMessage.message.mIsSelfSendMsg && mIMMessage.message.getMsgPlayStatus() == GmacsConstant.MSG_NOT_PLAYED) {
                    noRead.setVisibility(View.GONE);
                    mIMMessage.message.setMsgPlayStatus(GmacsConstant.MSG_PLAYED);
                    Message message = mIMMessage.message;
                    Message.MessageUserInfo otherInfo = message.getTalkOtherUserInfo();
                    MessageLogic.getInstance().updatePlayStatusByLocalId(otherInfo.mUserId, otherInfo.mUserSource, message.mLocalId, GmacsConstant.MSG_PLAYED);
                }

                SoundPlayer.getInstance().startPlaying(msgVoiceIv.getTag().toString(),
                        new VoiceOnCompletionListener(mChatActivity, (IMAudioMsg) mIMMessage),
                        mIMMessage.message.mLocalId);
                playOrStopAnimate();
            }
        });
        playOrStopAnimate();
    }

    private void playOrStopAnimate() {
        // 若是该item播放，则进行播放动画
        if (SoundPlayer.getInstance().currentPlayId() == mIMMessage.message.mLocalId) {
            // 进行动画
            Drawable d = msgVoiceIv.getBackground();
            if (d != null) {
                final AnimationDrawable ani = (AnimationDrawable) d;
                ani.stop();
                ani.start();
            } else {
                if (mIMMessage.message.mIsSelfSendMsg) {
                    msgVoiceIv.setBackgroundResource(R.drawable.gmacs_anim_right_sound);
                } else {
                    msgVoiceIv.setBackgroundResource(R.drawable.gmacs_anim_left_sound);
                }

                msgVoiceIv.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
                    @Override
                    public boolean onPreDraw() {
                        final AnimationDrawable ani = (AnimationDrawable) msgVoiceIv.getBackground();
                        if (ani != null) {
                            ani.start();
                        }
                        return true;
                    }
                });

            }
            msgVoiceIv.setImageDrawable(null);
        } else {
            Drawable d = msgVoiceIv.getBackground();
            if (d != null) {
                final AnimationDrawable ani = (AnimationDrawable) d;
                ani.stop();
                msgVoiceIv.setBackgroundResource(0);
            }
            setResourceForVoiceView();
        }
    }

    private void setResourceForVoiceView() {
        if (mIMMessage.message.mIsSelfSendMsg) {
            msgVoiceIv.setImageResource(R.drawable.gmacs_ic_right_sound3);
        } else {
            msgVoiceIv.setImageResource(R.drawable.gmacs_ic_left_sound3);
        }
    }

    @Override
    protected View initView(LayoutInflater inflater, ViewGroup parentView, int maxWidth) {
        if (mIMMessage.message.mIsSelfSendMsg) {
            mContentView = inflater.inflate(R.layout.gmacs_adapter_msg_content_right_voice, parentView, false);
        } else {
            mContentView = inflater.inflate(R.layout.gmacs_adapter_msg_content_left_voice, parentView, false);
        }
        downFailed = (ImageView) mContentView.findViewById(R.id.left_failed_down);
        msgVoiceIv = (ImageView) mContentView.findViewById(R.id.play_img);
        noRead = (ImageView) mContentView.findViewById(R.id.voice_no_read);
        duration = (TextView) mContentView.findViewById(R.id.duration);
        playImageLayout = mContentView.findViewById(R.id.play_img_layout);
        mContentView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                final GmacsDialog.Builder dialog = new GmacsDialog.Builder(getContentView().getContext(),
                        GmacsDialog.Builder.DIALOG_TYPE_LIST_NO_BUTTON);
                dialog.initDialog(new AdapterView.OnItemClickListener() {

                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                        switch (position) {
                            case 0: // 删除消息
                                deleteIMMessageView();
                                dialog.dismiss();
                                break;
                            case 1: // 听筒/扬声器切换
                                if (!SoundPlayer.getInstance().isWiredHeadsetOn()) {
                                    boolean isSpeakerphoneOn = SoundPlayer.getInstance().isSpeakerphoneOn();
                                    SoundPlayer.getInstance().saveAudioMessageRoute(!isSpeakerphoneOn);
                                    ToastUtil.showToast(isSpeakerphoneOn ? mChatActivity
                                            .getText(R.string.switch_to_earphone) : mChatActivity
                                            .getText(R.string.switch_to_speaker));
                                }
                                break;
//                            case 2:
//                                boolean isFirstConvertText = (Boolean) GmacsConfig.ClientConfig.getParam(FIRST_CONVERT_TEXT, true);
//                                if (isFirstConvertText) {
//                                    final GmacsDialog.Builder dialog = new GmacsDialog.Builder(getContentView().getContext(),
//                                            GmacsDialog.Builder.DIALOG_TYPE_CUSTOM_CONTENT_VIEW);
//                                    View linearLayout = LayoutInflater.from(getContentView().getContext()).inflate(R.layout.gmacs_custom_dialog_layout, null);
//                                    TextView title = (TextView) linearLayout.findViewById(R.id.tv_title);
//                                    TextView message = (TextView) linearLayout.findViewById(R.id.tv_dialog_message);
//                                    TextView ok = (TextView) linearLayout.findViewById(R.id.tv_neu_btn);
//
//                                    title.setText(R.string.convert_to_text_title);
//                                    message.setText(R.string.convert_to_text_dialog);
//                                    ok.setVisibility(View.VISIBLE);
//                                    ok.setOnClickListener(new View.OnClickListener() {
//                                        @Override
//                                        public void onClick(View v) {
//                                            GmacsConfig.ClientConfig.setParam(FIRST_CONVERT_TEXT, false);
//                                            convertText(v);
//                                            dialog.dismiss();
//                                        }
//                                    });
//                                    dialog.initDialog(linearLayout);
//                                    dialog.setCancelable(false).create().show();
//                                } else {
//                                    convertText(view);
//                                }
//                                break;
                            default:
                                break;
                        }
                    }
                });
                if (!SoundPlayer.getInstance().isWiredHeadsetOn()) {
                    dialog.setListTexts(new String[]{mChatActivity.getString(R.string.delete_message),
                            SoundPlayer.getInstance().isSpeakerphoneOn() ? mChatActivity
                                    .getString(R.string.switch_to_earphone) : mChatActivity
                                    .getString(R.string.switch_to_speaker)/*, mChatActivity.getString(R.string.convert_to_text)*/});
                } else {
                    dialog.setListTexts(new String[]{mChatActivity.getString(R.string.delete_message)/*, mChatActivity.getString(R.string.convert_to_text)*/});
                }
                dialog.create().show();
                return true;
            }
        });

        if (sWidthOnce < 0) {
            if (sVoiceViewMaxWidth < 0) {
                sVoiceViewMaxWidth = mChatActivity.getResources().getDimensionPixelOffset(R.dimen.im_voice_max_width);
                if (sVoiceViewMaxWidth > maxWidth) {
                    sVoiceViewMaxWidth = maxWidth;
                }
            }
            if (sVoiceViewMinWidth < 0) {
                sVoiceViewMinWidth = mChatActivity.getResources().getDimensionPixelOffset(R.dimen.im_voice_min_width);
            }
            sWidthOnce = (sVoiceViewMaxWidth - sVoiceViewMinWidth) / 13;
        }

        setListenerForFailed();
        return mContentView;
    }

    @Override
    public void setDataForView(IMMessage imMessage) {
        super.setDataForView(imMessage);
        IMAudioMsg imAudioMsg = (IMAudioMsg) mIMMessage;
        if (mIMMessage.message.mIsSelfSendMsg) {
            if (!TextUtils.isEmpty(imAudioMsg.mLocalUrl)) {
                setData(imAudioMsg.mLocalUrl);
            } else if (!TextUtils.isEmpty(imAudioMsg.mUrl)) {
                if (imAudioMsg.mUrl.startsWith("/")) {
                    setData(imAudioMsg.mUrl);
                } else {
                    download(imAudioMsg.mUrl);
                }
            }
        } else if (!TextUtils.isEmpty(imAudioMsg.mUrl)) {
            download(imAudioMsg.mUrl);
        }
        if (noRead != null) {
            if (imAudioMsg.message.getMsgPlayStatus() == GmacsConstant.MSG_PLAYED) {
                noRead.setVisibility(View.GONE);
            } else {
                noRead.setVisibility(View.VISIBLE);
            }
        }
        setVoiceViewWidthByDuration(imAudioMsg);
        duration.setVisibility(View.VISIBLE);
        duration.setText(imAudioMsg.mDuration + "''");
    }

    /**
     * 根据时长设置声音view的宽度
     */
    private void setVoiceViewWidthByDuration(@NonNull IMAudioMsg imAudioMsg) {
        // 先计算下每一份应增长的宽度
        int voiceViewWidth;
        // 小于等于10以下的是没1s增长一份，1-2s定一个最小值
        if (imAudioMsg.mDuration <= 10) {
            if (imAudioMsg.mDuration > 0 && imAudioMsg.mDuration <= 2) {
                voiceViewWidth = sVoiceViewMinWidth;
            } else {
                voiceViewWidth = (int) (sVoiceViewMinWidth + ((imAudioMsg.mDuration - 2) * sWidthOnce));
            }
        } else if (imAudioMsg.mDuration > 10 && imAudioMsg.mDuration <= 60) {
            voiceViewWidth = (int) (sVoiceViewMinWidth + ((10 - 2) * sWidthOnce) + (imAudioMsg.mDuration / 10) * sWidthOnce);
        } else {
            voiceViewWidth = sVoiceViewMaxWidth;
        }

        ViewGroup.LayoutParams lp = playImageLayout.getLayoutParams();
        if (lp == null) {
            lp = new ViewGroup.LayoutParams(voiceViewWidth, mChatActivity.getResources().getDimensionPixelOffset(R.dimen.im_voice_height));
        } else {
            lp.width = voiceViewWidth;
        }
        playImageLayout.setLayoutParams(lp);
    }


    /**
     * 为下载失败设置views
     */
    private void setViewsForDownloadFailed() {
        msgVoiceIv.setVisibility(View.VISIBLE);
        msgVoiceIv.setTag("");
        duration.setVisibility(View.GONE);
        if (downFailed != null) {
            downFailed.setVisibility(View.VISIBLE);
        }
    }

    /**
     * 为失败按钮加监听
     */
    private void setListenerForFailed() {
        if (downFailed != null) {
            downFailed.setVisibility(View.GONE);
            downFailed.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View v) {
                    final GmacsDialog.Builder dialog = new GmacsDialog.Builder(getContentView().getContext(), GmacsDialog.Builder.DIALOG_TYPE_TEXT_NEG_POS_BUTTON);
                    dialog.initDialog(R.string.retry_download_or_not, R.string.no, R.string.yes,
                            new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    dialog.cancel();
                                }
                            },
                            new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    // 重新下载
                                    download(((IMAudioMsg) mIMMessage).mUrl);
                                    dialog.dismiss();
                                }
                            }
                    ).create().show();
                }
            });
        }
    }

    private void download(final String url) {
        RequestManager.getInstance().postRequest(new AudioRequest(url, new ErrorListener(this), new Listener(this)));
    }

    private void convertText(View v) {
        if (!mIMMessage.message.mIsSelfSendMsg && mIMMessage.message.getMsgPlayStatus() == GmacsConstant.MSG_NOT_PLAYED) {
            noRead.setVisibility(View.GONE);
            mIMMessage.message.setMsgPlayStatus(GmacsConstant.MSG_PLAYED);
            Message message = mIMMessage.message;
            Message.MessageUserInfo otherInfo = message.getTalkOtherUserInfo();
            MessageLogic.getInstance().updatePlayStatusByLocalId(otherInfo.mUserId, otherInfo.mUserSource, message.mLocalId, GmacsConstant.MSG_PLAYED);
        }
        if (!TextUtils.isEmpty(((IMAudioMsg) mIMMessage).mUrl) && !TextUtils.isEmpty(GmacsUiUtil.getConvertToTextActivityClassName())) {
            try {
                Intent intent = new Intent(v.getContext(), Class.forName
                        (GmacsUiUtil.getConvertToTextActivityClassName()));
                intent.putExtra("url", ((IMAudioMsg) mIMMessage).mUrl);
                v.getContext().startActivity(intent);
            } catch (ClassNotFoundException e) {
                e.printStackTrace();

            }
        } else {
            ToastUtil.showToast("不能转换语音");
        }
    }

    static class VoiceOnCompletionListener implements SoundPlayer.VoiceCompletion {
        private WeakReference<GmacsChatActivity> chatActivityWeakReference;
        private IMAudioMsg imAudioMsg;

        VoiceOnCompletionListener(GmacsChatActivity chatActivity, IMAudioMsg imAudioMsg) {
            chatActivityWeakReference = new WeakReference<>(chatActivity);
            this.imAudioMsg = imAudioMsg;
        }

        @Override
        public void onCompletion(MediaPlayer mp, final boolean isNormalEnd) {
            final GmacsChatActivity chatActivity = chatActivityWeakReference.get();
            if (chatActivity != null) {
                chatActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        chatActivity.updateSendStatusAndCardContentForSpecificMessage(imAudioMsg.message);
                        if (isNormalEnd) {
                            IMMessage imMessage = chatActivity.getNextIMMessage(imAudioMsg);
                            if (imMessage instanceof IMAudioMsg
                                    && !imMessage.message.mIsSelfSendMsg
                                    && imMessage.message.getMsgPlayStatus() == GmacsConstant.MSG_NOT_PLAYED) {
                                imAudioMsg = (IMAudioMsg) imMessage;
                                imAudioMsg.message.setMsgPlayStatus(GmacsConstant.MSG_PLAYED);
                                Message message = imAudioMsg.message;
                                Message.MessageUserInfo otherInfo = message.getTalkOtherUserInfo();
                                MessageLogic.getInstance().updatePlayStatusByLocalId(otherInfo.mUserId, otherInfo.mUserSource, message.mLocalId, GmacsConstant.MSG_PLAYED);
                                SoundPlayer.getInstance().autoStartPlaying(imAudioMsg.mUrl, VoiceOnCompletionListener.this, message.mLocalId);
                                chatActivity.updateSendStatusAndCardContentForSpecificMessage(message);
                            }
                        }
                    }
                });
            }
        }
    }

    static class ErrorListener implements Response.ErrorListener {
        WeakReference<IMAudioMsgView> weakReference;

        ErrorListener(IMAudioMsgView imAudioMsgView) {
            weakReference = new WeakReference<>(imAudioMsgView);
        }

        @Override
        public void onErrorResponse(VolleyError error) {
            final IMAudioMsgView imAudioMsgView = weakReference.get();
            if (imAudioMsgView != null) {
                GmacsUtils.getInstance().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        imAudioMsgView.setViewsForDownloadFailed();
                    }
                });
            }
        }
    }

    static class Listener implements Response.Listener<String> {
        WeakReference<IMAudioMsgView> weakReference;

        Listener(IMAudioMsgView imAudioMsgView) {
            weakReference = new WeakReference<>(imAudioMsgView);
        }

        @Override
        public void onResponse(final String response) {
            final IMAudioMsgView imAudioMsgView = weakReference.get();
            if (imAudioMsgView != null) {
                GmacsUtils.getInstance().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        imAudioMsgView.setData(response);
                    }
                });
            }
        }
    }
}