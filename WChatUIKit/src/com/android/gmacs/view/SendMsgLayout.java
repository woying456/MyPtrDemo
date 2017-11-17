package com.android.gmacs.view;

import android.Manifest;
import android.content.Context;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.android.gmacs.R;
import com.android.gmacs.activity.GmacsChatActivity;
import com.android.gmacs.emoji.EmojiManager;
import com.android.gmacs.emoji.FaceLinearLayout;
import com.android.gmacs.emoji.IEmojiParser;
import com.android.gmacs.logic.CommandLogic;
import com.android.gmacs.observer.OnInputSoftListener;
import com.android.gmacs.sound.SoundRecord;
import com.common.gmacs.core.GmacsConstant;
import com.common.gmacs.msg.data.IMCallMsg;
import com.common.gmacs.parse.message.Message;
import com.common.gmacs.parse.talk.Talk;
import com.common.gmacs.utils.GLog;
import com.common.gmacs.utils.GmacsUtils;
import com.common.gmacs.utils.PermissionUtil;
import com.common.gmacs.utils.ToastUtil;

/**
 * 聊天页面底部工具view
 */
public class SendMsgLayout extends LinearLayout implements OnClickListener, OnInputSoftListener, TextWatcher, OnTouchListener {

    public SendMoreLayout mSendMoreLayout;
    public boolean inputSoftIsShow = false;
    public ImageView mPublicAccountMenuBtn;
    protected QuickMsgAdapter mQuickMsgAdapter;
    private AtEditText sendMessageEditText;
    private TextView sendTextButton;
    private ImageView mSendVoice;
    private ImageView mSendMoreButton;
    private ImageView mSendEmojiButton;
    private ImageView mQuickButton;
    private TextView mRecordVoice;
    private FaceLinearLayout mEmojiLayout;
    private ListView mQuickMsgListView;
    private View mQuickMsgLayout;
    private GmacsChatActivity gmacsChatActivity;
    private SoundRecord mRecord;
    private boolean needShowSendMoreLayoutInOnHide;
    private boolean needShowEmojiLayoutInOnHide;

    public SendMsgLayout(Context context) {
        super(context);
    }

    public SendMsgLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public SendMsgLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    protected void onFinishInflate() {
        super.onFinishInflate();
        gmacsChatActivity = (GmacsChatActivity) getContext();
        // 输入框
        sendMessageEditText = (AtEditText) findViewById(R.id.send_msg_edit_text);
        sendMessageEditText.setChatActivity(gmacsChatActivity);
        sendMessageEditText.clearFocus();
        // 发送按钮
        sendTextButton = (TextView) findViewById(R.id.send_text);
        mSendVoice = (ImageView) findViewById(R.id.send_voice_button);
        // 发送更多按钮
        mSendMoreButton = (ImageView) findViewById(R.id.send_more_button);
        mSendEmojiButton = (ImageView) findViewById(R.id.send_emoji_button);
        mRecordVoice = (TextView) findViewById(R.id.record_voice);
        mSendMoreLayout = (SendMoreLayout) findViewById(R.id.send_more_layout);
        mEmojiLayout = (FaceLinearLayout) findViewById(R.id.face_container);
        // 快速发送
        mQuickButton = (ImageView) findViewById(R.id.send_quick_button);
        mQuickMsgListView = (ListView) findViewById(R.id.quick_msg);
        mQuickMsgLayout = findViewById(R.id.send_quick_msg_layout);
        // 公众号菜单切换
        mPublicAccountMenuBtn = (ImageView) findViewById(R.id.iv_public_account_keyboard_down);

        mEmojiLayout.setMessageEditView(sendMessageEditText);
        sendMessageEditText.addTextChangedListener(this);
        sendMessageEditText.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    mSendMoreLayout.setVisibility(View.GONE);
                    mEmojiLayout.hidden();
                    gmacsChatActivity.popMsgUpOfSendMsgLayout();
                }
                return false;
            }
        });
        mSendEmojiButton.setOnClickListener(this);
        mSendMoreButton.setOnClickListener(this);
        mSendVoice.setOnClickListener(this);
        sendTextButton.setOnClickListener(this);
        mRecordVoice.setOnTouchListener(this);
        mPublicAccountMenuBtn.setOnClickListener(this);
        switchSendText(false);
    }

    public void onClick(View v) {
        if (v == mSendEmojiButton) {
            switchSendEmoji();
        } else if (v == mSendMoreButton) {
            switchSendMore();
        } else if (v == mSendVoice) {
            switchSendVoice();
        } else if (v == mQuickButton) {
            switchSendQuickMsg();
        } else if (v == sendTextButton) {
            sendTextMsg();
        } else if (v == mPublicAccountMenuBtn) {
            if (gmacsChatActivity.publicAccountMenu.isShown()) {
                gmacsChatActivity.publicAccountMenu.setVisibility(GONE);
                setVisibility(VISIBLE);
            } else {
                if (inputSoftIsShow) {
                    hideInputSoft();
                }
                gmacsChatActivity.publicAccountMenu.setVisibility(VISIBLE);
                setVisibility(GONE);
            }
        }
    }

    public void setTalk(Talk talk) {
        sendMessageEditText.setTalk(talk);
    }

    public void insertAtText(boolean alreadyHasAt, String name, String id, int source, String realName) {
        showInputSoft();
        sendMessageEditText.insertAtText(alreadyHasAt, name, id, source, realName);
    }

    public Message.AtInfo[] getAtInfo() {
        return sendMessageEditText.getAllAtInfo();
    }

    /**
     * 点击发送按钮处理逻辑
     */
    private void sendTextMsg() {
        String message = sendMessageEditText.replaceWithRealNameToSend();
        if (TextUtils.isEmpty(message)) {
            ToastUtil.showToast(R.string.message_cannot_be_empty);
        } else if (isBlankOrCRLF(message)) {
            ToastUtil.showToast(R.string.message_cannot_be_space_or_enter);
        } else {
            gmacsChatActivity.sendTextMsg(message, "");
            setMsgEditText("");
        }
    }

    /**
     * 录音按钮触摸事件处理方法
     */
    public boolean onTouch(View v, MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                GLog.d("audio_msg", "down");
                // 开始录音
                mRecordVoice.setText(getContext().getString(R.string.record_stop));
                mRecordVoice.setSelected(true);
                if (CommandLogic.getInstance().isChatting()) {
                    ToastUtil.showToast(getResources().getString(R.string.calling,
                            CommandLogic.getInstance().getChattingType() == IMCallMsg.CALL_TYPE_VIDEO ? "视频" : "音频"));
                    return true;
                }

                PermissionUtil.requestPermissions(gmacsChatActivity
                        , new String[]{Manifest.permission.RECORD_AUDIO}, GmacsConstant.REQUEST_CODE_RECORD_AUDIO
                        , new PermissionUtil.PermissionCallBack() {
                            @Override
                            public void onCheckedPermission(boolean isGranted) {
                                if (isGranted) {
                                    gmacsChatActivity.popMsgUpOfSendMsgLayout();
                                    mRecord.startRecord(getContext(), true, new SoundRecord.RecordListener() {
                                        public void onSuccessRecord(final String filePath, final int duration) {
                                            post(new Runnable() {
                                                public void run() {
                                                    mRecordVoice.setText(getContext().getString(R.string.record_start));
                                                    mRecordVoice.setSelected(false);
                                                    gmacsChatActivity.sendAudioMsg(filePath, duration, "");
                                                }
                                            });
                                        }

                                        public void onFailedRecord() {
                                        }
                                    });
                                } else {
                                    ToastUtil.showToast(R.string.permission_record_audio);
                                }
                            }
                        });

                break;
            case MotionEvent.ACTION_MOVE:
                GLog.d("audio_msg", "move");
                break;
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                GLog.d("audio_msg", "up");
                mRecordVoice.setText(getContext().getString(R.string.record_start));
                mRecordVoice.setSelected(false);
                if (!mRecord.isUserCancelRecord()) {
                    mRecord.stopRecord();
                }
                break;
        }
        return true;
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
    }

    /**
     * 文本转换成表情
     */
    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        IEmojiParser iEmojiParser = EmojiManager.getInstance().getEmojiParser();
        if (iEmojiParser != null) {
            iEmojiParser.replaceAllEmoji(sendMessageEditText.getText(), 24);
        }
    }

    /**
     * 处理输入框右边按钮显示状态。
     */
    @Override
    public void afterTextChanged(Editable s) {
        if (!TextUtils.isEmpty(s) && sendTextButton.getVisibility() == View.GONE) {
            sendTextButton.setVisibility(View.VISIBLE);
            mSendMoreButton.setVisibility(View.GONE);
        } else if (TextUtils.isEmpty(s) && sendTextButton.getVisibility() == View.VISIBLE) {
            sendTextButton.setVisibility(View.GONE);
            mSendMoreButton.setVisibility(View.VISIBLE);
        }
    }

    /**
     * 停止录音
     */
    public void stopRecord() {
        if (mRecord != null && mRecord.isRecording()) {
            if (mRecordVoice != null) {
                mRecordVoice.setText(getContext().getString(R.string.record_start));
                mRecordVoice.setSelected(false);
            }
            mRecord.stopRecord();
        }
    }

    /**
     * 是否包含空格或者换行符
     *
     * @param message
     * @return
     */
    public boolean isBlankOrCRLF(String message) {
        return TextUtils.isEmpty(message.trim());
    }

    /**
     * 隐藏键盘，表情框，更多框，快捷消息框。
     */
    public void collapseMoreAndInputMethod() {
        if (mSendMoreLayout.isShown())
            mSendMoreLayout.setVisibility(View.GONE);

        if (mEmojiLayout.faceViewShown()) {
            mEmojiLayout.hidden();
        }

        mQuickMsgLayout.setVisibility(View.GONE);
        hideInputSoft();
    }

    /**
     * 当页面点击返回按钮处理逻辑
     *
     * @return 返回事件是否被处理过。
     */
    public boolean onBackPress() {
        if (mSendMoreLayout != null && mSendMoreLayout.isShown()) {
            mSendMoreLayout.setVisibility(View.GONE);
        } else if (mQuickMsgLayout != null && mQuickMsgLayout.isShown()) {
            mQuickMsgLayout.setVisibility(View.GONE);
        } else if (mEmojiLayout != null && mEmojiLayout.faceViewShown()) {
            mEmojiLayout.hidden();
        } else {
            return false;
        }
        return true;
    }

    public void setSendAudioEnable(boolean enable) {
        if (enable) {
            mSendVoice.setVisibility(View.VISIBLE);
        } else {
            mSendVoice.setVisibility(View.GONE);
        }
    }

    public void setSendEmojiEnable(boolean enable) {
        if (enable) {
            mSendEmojiButton.setVisibility(View.VISIBLE);
        } else {
            mSendEmojiButton.setVisibility(View.GONE);
        }
    }

    public void setSendMoreEnable(boolean enable) {
        if (enable) {
            mSendMoreButton.setVisibility(View.VISIBLE);
        } else {
            mSendMoreButton.setVisibility(View.GONE);
        }
    }

    public void switchSendText() {
        switchSendText(true);
    }

    /**
     * 转换为发送文本
     */
    public void switchSendText(boolean showInputsoft) {
        mSendVoice.setImageResource(R.drawable.gmacs_ic_voice);
        mRecordVoice.setVisibility(View.GONE);
        sendMessageEditText.setVisibility(View.VISIBLE);
        mSendMoreLayout.setVisibility(View.GONE);
        mQuickMsgLayout.setVisibility(View.GONE);
        String contents = sendMessageEditText.getText().toString();
        if (!TextUtils.isEmpty(contents)) {
            sendTextButton.setVisibility(View.VISIBLE);
            mSendMoreButton.setVisibility(View.GONE);
        }
        mEmojiLayout.hidden();
        sendMessageEditText.requestFocus();
        if (showInputsoft) {
            showInputSoft();
        }
    }

    /**
     * 转换为发送语音
     */
    public void switchSendVoice() {
        if (mRecordVoice.isShown()) {
            switchSendText();
        } else {
            mSendVoice.setImageResource(R.drawable.gmacs_ic_keyboard);
            mRecordVoice.setVisibility(View.VISIBLE);
            mSendVoice.setVisibility(View.VISIBLE);
            sendMessageEditText.setVisibility(View.GONE);
            sendTextButton.setVisibility(View.GONE);
            mSendMoreLayout.setVisibility(View.GONE);
            mQuickMsgLayout.setVisibility(View.GONE);
            mEmojiLayout.hidden();
            hideInputSoft();
        }
    }

    /**
     * 切换到表情发送模式
     */
    public void switchSendEmoji() {
        mSendVoice.setImageResource(R.drawable.gmacs_ic_voice);
        mRecordVoice.setVisibility(View.GONE);
        sendMessageEditText.setVisibility(View.VISIBLE);
        mSendMoreLayout.setVisibility(View.GONE);
        mQuickMsgLayout.setVisibility(View.GONE);
        String contents = sendMessageEditText.getText().toString();
        if (!TextUtils.isEmpty(contents)) {
            sendTextButton.setVisibility(View.VISIBLE);
            mSendMoreButton.setVisibility(View.GONE);
        }
        hideInputSoft();
        if (!mEmojiLayout.faceViewShown() && inputSoftIsShow) {
            needShowEmojiLayoutInOnHide = true;
        } else if (!mEmojiLayout.faceViewShown()) {
            mEmojiLayout.show();
            gmacsChatActivity.popMsgUpOfSendMsgLayout();
        } else {
            mEmojiLayout.hidden();
        }
    }

    /**
     * 切换到发送更多模式
     */
    public void switchSendMore() {
        sendMessageEditText.setVisibility(View.VISIBLE);
        mSendVoice.setImageResource(R.drawable.gmacs_ic_voice);
        mRecordVoice.setVisibility(View.GONE);
        mQuickMsgLayout.setVisibility(View.GONE);
        String contents = sendMessageEditText.getText().toString();
        if (!TextUtils.isEmpty(contents)) {
            sendTextButton.setVisibility(View.VISIBLE);
            mSendMoreButton.setVisibility(View.GONE);
        }
        mEmojiLayout.hidden();
        hideInputSoft();
        if (!mSendMoreLayout.isShown() && inputSoftIsShow) {
            needShowSendMoreLayoutInOnHide = true;
        } else if (!mSendMoreLayout.isShown()) {
            mSendMoreLayout.setVisibility(View.VISIBLE);
            gmacsChatActivity.popMsgUpOfSendMsgLayout();
        } else {
            mSendMoreLayout.setVisibility(View.GONE);
        }
    }

    /**
     * 切换到快捷回复模式
     */
    public void switchSendQuickMsg() {
        mRecordVoice.setVisibility(View.GONE);
        sendMessageEditText.setVisibility(View.VISIBLE);
        mSendMoreLayout.setVisibility(View.GONE);
        mEmojiLayout.hidden();
        String contents = sendMessageEditText.getText().toString();
        if (!TextUtils.isEmpty(contents)) {
            sendTextButton.setVisibility(View.VISIBLE);
            mSendMoreButton.setVisibility(View.GONE);
        }
        hideInputSoft();
        if (mQuickMsgLayout.getVisibility() == View.VISIBLE) {
            mQuickMsgLayout.setVisibility(View.GONE);
        } else {
            gmacsChatActivity.popMsgUpOfSendMsgLayout();
            mQuickMsgLayout.setVisibility(View.VISIBLE);
        }
    }


    /**
     * 隐藏键盘
     */
    public void hideInputSoft() {
        GmacsUtils.hideSoftInputMethod(sendMessageEditText.getApplicationWindowToken());
    }

    /**
     * 打开键盘
     */
    public void showInputSoft() {
        gmacsChatActivity.popMsgUpOfSendMsgLayout();
        InputMethodManager manager = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        manager.toggleSoftInput(0, InputMethodManager.HIDE_IMPLICIT_ONLY);
    }

    /**
     * 键盘弹出
     */
    public void onShow() {
        inputSoftIsShow = true;
        switchSendText(false);
    }

    /**
     * 键盘隐藏
     */
    public void onHide() {
        inputSoftIsShow = false;
        if (needShowSendMoreLayoutInOnHide) {
            mSendMoreLayout.setVisibility(VISIBLE);
            needShowSendMoreLayoutInOnHide = false;
        } else if (needShowEmojiLayoutInOnHide) {
            mEmojiLayout.show();
            needShowEmojiLayoutInOnHide = false;
        }
    }

    public void setRecord(SoundRecord record) {
        mRecord = record;
    }

    public void setGmacsChatActivity(GmacsChatActivity gmacsChatActivity) {
        this.gmacsChatActivity = gmacsChatActivity;
    }

    public String getMsgEditText() {
        return sendMessageEditText.getText().toString();
    }

    /**
     * 设置文本，同时屏蔽TextWatcher。
     * 例如：
     * 1、设置草稿(草稿里的@消息是普通文本，不应触发TextWatcher)。
     * 2、发消息清空输入框（发送@消息前会修改回不含备注名的名称，导致内部维护的SpannableStringBuilder与EditText内容不一致，此时触发TextWatcher会崩溃）。
     *
     * @param msg
     */
    public void setMsgEditText(String msg) {
        sendMessageEditText.disableTextWatcher(msg);
        sendMessageEditText.setText(msg);
        if (!TextUtils.isEmpty(msg)) {
            sendMessageEditText.setSelection(msg.length());
        }
    }

    public View getRecordVoice() {
        return mRecordVoice;
    }


    public void registerOnMoreItemClick(SendMoreLayout.OnMoreItemClickListener onMoreItemClickListener) {
        mSendMoreLayout.registerOnMoreItemClick(onMoreItemClickListener);
    }

    /**
     * There are only 3 default buttons.
     * <br><b>We support more than eight buttons, there is a ViewPager will be shown if possible.</b></br>
     *
     * @param imgResId                       The resource id of image of buttons.
     * @param text                           The unique texts of buttons, which can identify buttons definitely.
     * @param isShowItemsSingleLinePreferred
     */
    public void setSendMoreItemResources(int[] imgResId, String[] text, boolean isShowItemsSingleLinePreferred) {
        mSendMoreLayout.setBtnImgResIds(imgResId);
        mSendMoreLayout.setBtnTexts(text);
        mSendMoreLayout.showItemsSingleLinePreferred(isShowItemsSingleLinePreferred);
        mSendMoreLayout.notifyData();
    }

    /**
     * There are only 3 default buttons.
     * <br><b>We support more than eight buttons, there is a ViewPager will be shown if possible.</b></br>
     *
     * @param imgResId                       The resource id of image of buttons.
     * @param text                           The unique texts of buttons, which can identify buttons definitely.
     * @param unclickablePosition
     * @param isShowItemsSingleLinePreferred
     */
    public void setSendMoreItemResources(int[] imgResId, String[] text, int[] unclickablePosition, boolean isShowItemsSingleLinePreferred) {
        mSendMoreLayout.setBtnImgResIds(imgResId);
        mSendMoreLayout.setBtnTexts(text);
        mSendMoreLayout.setUnclickableBtnsPosition(unclickablePosition);
        mSendMoreLayout.showItemsSingleLinePreferred(isShowItemsSingleLinePreferred);
        mSendMoreLayout.notifyData();
    }

    /**
     * 初始化快捷回复view
     */
    public void initQuickMsgView(String[] quickMsgs) {
        // 快捷回复
        if (quickMsgs != null) {
            // 若超了3个限制高度
            if (quickMsgs.length > 3) {
                ViewGroup.LayoutParams lp = mQuickMsgLayout.getLayoutParams();
                if (lp == null) {
                    lp = new LayoutParams(LayoutParams.MATCH_PARENT, getResources().getDimensionPixelSize(R.dimen.msg_quick_view_max_height));
                } else {
                    lp.height = getResources().getDimensionPixelSize(R.dimen.msg_quick_view_max_height);
                }
                mQuickMsgLayout.setLayoutParams(lp);

            }
            mQuickButton.setOnClickListener(this);
            mQuickButton.setVisibility(View.VISIBLE);
            mQuickMsgAdapter = new QuickMsgAdapter();
            mQuickMsgListView.setAdapter(mQuickMsgAdapter);
            mQuickMsgAdapter.quickMsgContents = quickMsgs;
        } else {
            mQuickButton.setVisibility(View.GONE);
        }
        mQuickMsgLayout.setVisibility(View.GONE);
    }

    @Override
    public void setVisibility(int visibility) {
        super.setVisibility(visibility);
        if (visibility == VISIBLE) {
            gmacsChatActivity.setShouldShowInputSoftAuto(gmacsChatActivity.getShouldShowInputSoftAutoConfig());
        } else {
            gmacsChatActivity.setShouldShowInputSoftAuto(false);
        }
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN:
                gmacsChatActivity.stopScroll();
                break;
        }
        return false;
    }

    /**
     * 快捷回复adapter
     */
    protected class QuickMsgAdapter extends BaseAdapter {
        // 快捷回复内容
        String[] quickMsgContents;

        public int getCount() {
            if (quickMsgContents != null) {
                return quickMsgContents.length;
            }
            return 0;
        }

        public Object getItem(int arg0) {
            return arg0;
        }

        public long getItemId(int arg0) {
            return arg0;
        }

        public View getView(final int arg0, View contentView, ViewGroup arg2) {
            if (contentView == null) {
                contentView = inflate(getContext(), R.layout.gmacs_item_quick_msg, null);
                ViewHold hold = new ViewHold();
                hold.quickMsgTV = (TextView) contentView.findViewById(R.id.quick_content_tv);
                contentView.setTag(hold);
            }
            ViewHold hold = (ViewHold) contentView.getTag();
            // 设置数据
            hold.quickMsgTV.setText(quickMsgContents[arg0]);
            hold.quickMsgTV.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                    gmacsChatActivity.clickQuickMsg(arg0);
                    if (gmacsChatActivity.justPutQuickMsgToInput()) {
                        setMsgEditText(quickMsgContents[arg0]);
                    } else {
                        gmacsChatActivity.sendTextMsg(quickMsgContents[arg0], "");
                    }
                }
            });
            return contentView;
        }

        private class ViewHold {
            TextView quickMsgTV;
        }

    }
}
