
package com.android.gmacs.activity;

import android.Manifest;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.WorkerThread;
import android.support.v4.content.FileProvider;
import android.text.InputFilter;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.text.style.ForegroundColorSpan;
import android.text.style.ReplacementSpan;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AbsListView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.android.gmacs.R;
import com.android.gmacs.adapter.GmacsChatAdapter;
import com.android.gmacs.album.AlbumConstant;
import com.android.gmacs.album.GmacsAlbumActivity;
import com.android.gmacs.album.GmacsImageActivity;
import com.android.gmacs.album.GmacsPhotoBrowserActivity;
import com.android.gmacs.album.ImageUrlArrayListWrapper;
import com.android.gmacs.core.GmacsManager;
import com.android.gmacs.event.DeleteContactEvent;
import com.android.gmacs.event.GetCaptchaEvent;
import com.android.gmacs.event.KickedOutOfGroupEvent;
import com.android.gmacs.event.RemarkEvent;
import com.android.gmacs.event.UnreadTotalEvent;
import com.android.gmacs.event.UpdateInsertChatMsgEvent;
import com.android.gmacs.event.ValidateCaptchaEvent;
import com.android.gmacs.event.WChatAlbumImagesDeletedEvent;
import com.android.gmacs.logic.ContactLogic;
import com.android.gmacs.logic.MessageLogic;
import com.android.gmacs.logic.TalkLogic;
import com.android.gmacs.msg.view.IMLocationMsgView;
import com.android.gmacs.sound.SoundPlayer;
import com.android.gmacs.sound.SoundRecord;
import com.android.gmacs.sound.SoundRecordUtil;
import com.android.gmacs.utils.FileProviderUtil;
import com.android.gmacs.utils.GmacsUiUtil;
import com.android.gmacs.view.GmacsDialog;
import com.android.gmacs.view.PublicAccountMenu;
import com.android.gmacs.view.ResizeLayout;
import com.android.gmacs.view.SendMoreLayout;
import com.android.gmacs.view.SendMsgLayout;
import com.android.gmacs.view.listview.GmacsChatListView;
import com.common.gmacs.core.ClientManager;
import com.common.gmacs.core.ContactsManager;
import com.common.gmacs.core.Gmacs;
import com.common.gmacs.core.GmacsConstant;
import com.common.gmacs.core.MessageManager;
import com.common.gmacs.core.RecentTalkManager;
import com.common.gmacs.msg.IMMessage;
import com.common.gmacs.msg.data.IMAudioMsg;
import com.common.gmacs.msg.data.IMImageMsg;
import com.common.gmacs.msg.data.IMLocationMsg;
import com.common.gmacs.msg.data.IMTextMsg;
import com.common.gmacs.msg.data.IMTipMsg;
import com.common.gmacs.parse.captcha.Captcha;
import com.common.gmacs.parse.contact.Contact;
import com.common.gmacs.parse.contact.GmacsUser;
import com.common.gmacs.parse.contact.Group;
import com.common.gmacs.parse.contact.GroupMember;
import com.common.gmacs.parse.contact.UserInfo;
import com.common.gmacs.parse.message.Message;
import com.common.gmacs.parse.pubcontact.PAFunctionConfig;
import com.common.gmacs.parse.talk.Talk;
import com.common.gmacs.parse.talk.TalkType;
import com.common.gmacs.utils.FileUtil;
import com.common.gmacs.utils.GmacsConfig;
import com.common.gmacs.utils.GmacsEnvi;
import com.common.gmacs.utils.GmacsUtils;
import com.common.gmacs.utils.NetworkUtil;
import com.common.gmacs.utils.PermissionUtil;
import com.common.gmacs.utils.ToastUtil;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import static com.android.gmacs.album.AlbumConstant.RESULT_CODE_IMAGE_DELETED;

public class GmacsChatActivity extends BaseActivity implements SendMoreLayout.OnMoreItemClickListener,
        MessageManager.SendIMMsgListener, ClientManager.PopLogViewListener, ContactsManager.UserInfoChangeCb {

    public final static int REQUEST_AT_CODE = 304;          // @消息
    public final static int REQUEST_IMAGE_CODE = 305;       // 图片预览请求码
    protected final static String DEFAULT_BTN_TEXT_IMAGE = "图片";
    protected final static String DEFAULT_BTN_TEXT_CAMERA = "拍照";
    protected final static String DEFAULT_BTN_TEXT_LOCATION = "位置";
    private final static int REQUEST_GALLERY_CODE = 301;    // 图库请求码
    private final static int REQUEST_TAKE_PHOTO_CODE = 302; // 拍照返回请求码
    private final static int REQUEST_CAMERA_CODE = 3021;    // 拍照返回并预览后的发送请求码
    private final static int REQUEST_LOCATION_CODE = 303;   // 位置请求码
    private final int onePageCount = 12;
    private final int msgCountPerRequest = 20;
    private final int maxSpan = GmacsEnvi.appContext.getResources().getDimensionPixelOffset(R.dimen.show_input_method_max_span);

    public SendMsgLayout sendMsgLayout;
    public PublicAccountMenu publicAccountMenu;
    protected ResizeLayout resizeLayout;
    protected GmacsChatListView chatListView;
    protected GmacsChatAdapter chatAdapter;
    // 该视图为自定义顶部消息置顶视图控件，初始值状态是隐藏的，由上层宿主实例来填充和控制
    protected LinearLayout personTopView;
    // 当前会话的会话信息
    protected Talk mTalk;
    private TitleBar mTitleBar;
    private String picturePath;
    protected HashMap<String, GroupMember> userInfoCache = new HashMap<>();
    private boolean isScrollEnd = true;
    private volatile boolean isLoadingForward;
    private volatile boolean isLoadingBackward;
    private volatile long lastMsgLocalId = -1;

    private SoundRecord mRecord = new SoundRecord();
    private PowerManager mPowerManager;
    private PowerManager.WakeLock mWakeLock;
    private SensorManager mSensorManager;
    private Sensor mProximitySensor;
    private boolean isNear;
    private SensorEventListener mSensorEventListener;

    private boolean shouldShowInputSoftAuto = true;

    private volatile boolean backwardHasMore = true;
    private GmacsDialog.Builder captchaDialog;
    private Set<Message> msgsNeedIdentify;

    private long focusMessageLocalId;
    private List<Message> cachedNewestMessages;
    private volatile Message bottomMessage;
    private volatile Message bottomSuccessfulMessage;
    private volatile Message latestMessage;

    private TextView messageReminderBar;
    private List<Message> preFetchMessages;
    private Handler handler = new Handler();

    private Message.MessageUserInfo receiverInfo;

    private GmacsChatListView.ListViewListener mListViewListener = new GmacsChatListView.ListViewListener() {

        @Override
        public void onLoadMore() {
            if (backwardHasMore && !isLoadingBackward) {
                loadHistoryMsgs();
            }
        }
    };

    //发消息统一带上的extra字段信息
    protected String commonMsgExtra;
    //发消息统一带上的refer字段信息
    protected String commonMsgRefer;
    //是否已被踢出群
    protected boolean isKickedOutOfGroup = true;

    /**
     * 子类复写。列表adapter
     *
     * @return
     */
    protected GmacsChatAdapter getChatAdapter() {
        return new GmacsChatAdapter(this, mTalk, userInfoCache);
    }

    /**
     * 子类复写。自定义卡片消息点击事件
     *
     * @param contentType
     * @param imMessage
     * @param url
     * @param title
     */
    public void onCardMsgClick(String contentType, IMMessage imMessage, String url, String title) {
    }

    /**
     * 上层若复写此方法并返回true，则不会弹任何出错toast（除了灌水、敏感词）
     *
     * @param message
     * @param errorCode
     * @param errorMessage
     * @return
     */
    protected boolean didSendMessage(final Message message, int errorCode, String errorMessage) {
        return false;
    }

    /**
     * 子类复写。获取到新消息ui展示前
     *
     * @param mTalk
     * @param message
     * @return 是否插入了新消息
     */
    protected boolean parseNewMessage(Talk mTalk, Message message) {
        return false;
    }

    /**
     * 子类复写。拉取历史消息时ui展示前
     *
     * @param talk
     * @param messages
     */
    protected List<Message> parseHistoryMessages(Talk talk, List<Message> messages) {
        return messages;
    }

    /**
     * 子类复写。供统计点击微聊模版语句
     */
    public void clickQuickMsg(int position) {
    }

    public GroupMember getUserInfoFromCache(int userSource, String userId) {
        if (userInfoCache != null) {
            return userInfoCache.get(Talk.getTalkId(userSource, userId));
        } else {
            return null;
        }
    }

    public void sendTextMsg(String message, String extra) {
        IMTextMsg imTextMsg = new IMTextMsg(message, extra);
        sendMsg(imTextMsg);
    }

    public void sendAudioMsg(String filePath, int duration, String extra) {
        IMAudioMsg imAudioMsg = new IMAudioMsg(filePath, duration, extra);
        sendMsg(imAudioMsg);
    }

    public void sendImageMsg(String filePath, boolean sendRawImage, String extra) {
        IMImageMsg imImageMsg = new IMImageMsg(filePath, extra, sendRawImage);
        sendMsg(imImageMsg);
    }

    public void sendLocationMsg(double longitude, double latitude, String address, String extra) {
        IMLocationMsg imLocationMsg = new IMLocationMsg(longitude, latitude, address, extra);
        sendMsg(imLocationMsg);
    }

    /**
     * 重发消息
     *
     * @param message
     */
    public void reSendMsg(Message message) {
        if (message != null) {
            message.mMsgUpdateTime = System.currentTimeMillis();
            MessageManager.getInstance().sendIMMsg(message, MessageLogic.getInstance());
            updateSendStatusAndCardContentForSpecificMessage(message);
        }
    }

    /**
     * 所有消息的发送统一调用此方法
     *
     * @param imMessage
     */
    public void sendMsg(IMMessage imMessage) {
        if (TextUtils.isEmpty(imMessage.extra) && !TextUtils.isEmpty(commonMsgExtra)) {
            imMessage.extra = commonMsgExtra;
        }
        MessageManager.getInstance().sendIMMsg(mTalk.mTalkType, imMessage, null == commonMsgRefer ? "" : commonMsgRefer,
                getReceiverInfo(), sendMsgLayout.getAtInfo(), MessageLogic.getInstance());
    }

    public void setListViewTranscriptMode(int mode) {
        chatListView.setTranscriptMode(mode);
    }

    public void popMsgUpOfSendMsgLayout() {
        if (!isEnd()) {
            if (cachedNewestMessages != null) {
                chatAdapter.clearData();
                updateBottomMessage(cachedNewestMessages);
                chatAdapter.addMsgsToEndPosition(cachedNewestMessages, new GmacsChatAdapter.FillGroupMemberInfoCb() {
                    @Override
                    public void done() {
                        lastMsgLocalId = getLastMsgLocalId();
                        checkHasMoreBackwards(cachedNewestMessages.get(cachedNewestMessages.size() - 1));
                        chatListView.hideFooterView();
                        cachedNewestMessages = null;
                    }
                });
            }
        }
        setListViewTranscriptMode(ListView.TRANSCRIPT_MODE_ALWAYS_SCROLL);
    }

    public void hideSendMsgLayout() {
        setListViewTranscriptMode(ListView.TRANSCRIPT_MODE_DISABLED);
    }

    /**
     * You can modify buttons except for adding your buttons and using none of or part of or all of our 3 default buttons,
     * whose texts and imgIds are SendMsgLayout.DEFAULT_BTN_TEXT_IMAGE + R.drawable.gmacs_ic_send_image,
     * SendMsgLayout.DEFAULT_BTN_TEXT_CAMERA + R.drawable.gmacs_ic_send_camera and
     * SendMsgLayout.DEFAULT_BTN_TEXT_LOCATION + R.drawable.gmacs_ic_send_location in SendMsgLayout.mSendMoreView.
     */
    public void setSendMoreItemResources() {
        int[] btnImgResIds = new int[]{R.drawable.gmacs_ic_send_image,
                R.drawable.gmacs_ic_send_camera, R.drawable.gmacs_ic_send_location};
        String[] btnTexts = new String[]{DEFAULT_BTN_TEXT_IMAGE,
                DEFAULT_BTN_TEXT_CAMERA, DEFAULT_BTN_TEXT_LOCATION};
        sendMsgLayout.setSendMoreItemResources(btnImgResIds, btnTexts, false);
    }

    /**
     * Override this method and handle buttons' click action by yourself.
     *
     * @param position The max position is equal to the amount of all the buttons subtracts one.
     */
    @Override
    public void onMoreItemClick(int position) {
        switch (position) {
            case 0:
                openAlbumActivity();
                break;
            case 1:
                openCameraActivity();
                break;
            case 2:
                openLocationActivity();
                break;
        }
    }

    /**
     * 获取快捷回复内容，现在是空实现，若需要需要子类实现该方法
     */
    public String[] getQuickMsgContents() {
        return null;
    }

    /**
     * 返回true时点击快速回复消息紧紧会将消息填充到输入框，否则直接发送。供子类重写该方法
     *
     * @return
     */
    public boolean justPutQuickMsgToInput() {
        return false;
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        SoundRecordUtil.dispatchTouchEvent(ev, mRecord, sendMsgLayout.getRecordVoice());
        return super.dispatchTouchEvent(ev);
    }

    @Override
    public void onBackPressed() {
        if (!sendMsgLayout.onBackPress()) {
            super.onBackPressed();
        }
    }

    @Override
    public void onUserInfoChanged(final UserInfo info) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (info instanceof Group) {
                    onGetGroupInfo((Group) info);
                } else if (info instanceof Contact) {
                    onGetUserInfo((Contact) info);
                }
            }
        });
    }

    @Override
    public void onPreSaveMessage(Message message) {
    }

    @Override
    public void onAfterSaveMessage(final Message message, final int errorCode, String errorMessage) {
        if (mTalk != null && mTalk.hasTheSameTalkIdWith(message)) {
            runOnUiThread(new Runnable() {
                public void run() {
                    addMsgToAdapter(message);
                    if (errorCode != 0) {
                        ToastUtil.showToast("消息保存失败");
                    }
                }
            });
        }
    }

    @Override
    public void onSendMessageResult(final Message message, final int errorCode, final String errorMessage) {
        if (!isFinishing()) {
            if (mTalk != null && mTalk.hasTheSameTalkIdWith(message)) {
                runOnUiThread(new Runnable() {
                    public void run() {
                        if (errorCode == 40021) { // 灌水
                            if (msgsNeedIdentify == null) {
                                msgsNeedIdentify = new HashSet<>();
                            }
                            GmacsManager.getInstance().getCaptcha(message);
                            msgsNeedIdentify.add(message);
                            message.setMsgSendStatus(GmacsConstant.MSG_SENDING);
                        } else if (errorCode == 42001) { // 敏感词
                            IMTipMsg imTipMsg = new IMTipMsg();
                            imTipMsg.mText = errorMessage;
                            MessageManager.getInstance().insertLocalMessage(
                                    mTalk.mTalkType, message.mSenderInfo, message.mReceiverInfo,
                                    message.getRefer(), imTipMsg, true, true, true,
                                    new InsertLocalMessageCb(GmacsChatActivity.this));
                        } else if (errorCode == 44007) { // 被踢提示
                            ToastUtil.showToast("您已被踢出群，无法收发消息");
                        } else if (!didSendMessage(message, errorCode, errorMessage)) {
                            if (errorCode != 0) ToastUtil.showToast(errorMessage);
                        }
                        updateBottomMessage(message);
                        updateSendStatusAndCardContentForSpecificMessage(message);
                    }
                });
            }
        }
    }

    public void launchImageActivity(Intent intentFromImageMsgView, IMImageMsg imageMsg) {
        if (!isFinishing() && mTalk != null && bottomMessage != null && imageMsg != null && intentFromImageMsgView != null) {
            List<IMMessage> messages = getAllIMMessageForSpecificType(IMImageMsg.class);
            intentFromImageMsgView.putExtra(GmacsConstant.EXTRA_USER_ID, mTalk.mTalkOtherUserId);
            intentFromImageMsgView.putExtra(GmacsConstant.EXTRA_USER_SOURCE, mTalk.mTalkOtherUserSource);
            intentFromImageMsgView.putExtra(AlbumConstant.IMAGE_COUNT, messages.size());
            intentFromImageMsgView.putExtra(AlbumConstant.IMAGE_LOCAL_ID, imageMsg.message.mLocalId);
            intentFromImageMsgView.putExtra(AlbumConstant.BEGIN_LOCAL_ID, bottomMessage.mLocalId);
            if (mTalk.mTalkType == Gmacs.TalkType.TALKTYPE_NORMAL.getValue()) {
                intentFromImageMsgView.putExtra(AlbumConstant.ALBUM_TITLE, mTalk.getOtherName());
            }
            intentFromImageMsgView.setClass(this, GmacsImageActivity.class);
            startActivityForResult(intentFromImageMsgView, GmacsChatActivity.REQUEST_IMAGE_CODE);
        }
    }

    @Override
    protected void initView() {
        mTitleBar = new TitleBar();
        resizeLayout = (ResizeLayout) findViewById(R.id.resizeLayout);
        chatListView = (GmacsChatListView) findViewById(R.id.listview_chat);
        personTopView = (LinearLayout) findViewById(R.id.person_msg_layout);
        initSendMsgLayout();
        messageReminderBar = (TextView) findViewById(R.id.message_reminder_bar);
        publicAccountMenu = (PublicAccountMenu) findViewById(R.id.public_account_menu);
        chatListView.setOnTouchListener(new View.OnTouchListener() {
            float mPosY, mCurPosY;
            boolean isHidedInActionDown;

            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        mPosY = event.getY();
                        isHidedInActionDown = sendMsgLayout.inputSoftIsShow;
                        sendMsgLayout.collapseMoreAndInputMethod();
                        break;
                    case MotionEvent.ACTION_MOVE:
//                        if (sendMsgLayout.inputSoftIsShow && !isHidedInActionDown) {
//                            sendMsgLayout.collapseMoreAndInputMethod();
//                        }
                        mCurPosY = event.getY();
                        if (mPosY == 0) {
                            mPosY = mCurPosY;
                        }
//                        if (shouldShowInputSoftAuto && mPosY - mCurPosY > maxSpan
//                                && !isHidedInActionDown && !sendMsgLayout.inputSoftIsShow && isScrollEnd) {
//                            //向上滑动
//                            sendMsgLayout.showInputSoft();
//                            isHidedInActionDown = true;
//                        }
                        if (mCurPosY - mPosY > 0) {
                            messageReminderBar.setVisibility(View.GONE);
                        }
                        break;
                    case MotionEvent.ACTION_UP:
                        mPosY = 0;
                        mCurPosY = 0;
                        isHidedInActionDown = false;
                        break;
                    default:
                        isHidedInActionDown = false;
                        break;
                }
                return false;
            }
        });
        chatListView.setListViewListener(mListViewListener);
        resizeLayout.setInputSoftListener(sendMsgLayout);
        chatListView.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView absListView, int scrollState) {
                isScrollEnd = absListView.getChildAt(absListView.getChildCount() - 1).getBottom() <= absListView.getHeight();
                if (backwardHasMore && !isLoadingBackward && scrollState == SCROLL_STATE_IDLE && absListView.getFirstVisiblePosition() == 0) {
                    chatListView.startLoadMore();
                }
                if (scrollState == SCROLL_STATE_IDLE
                        && absListView.getLastVisiblePosition() - chatListView.getHeaderViewsCount()
                        - chatListView.getFooterViewsCount() == chatAdapter.getCount() - 1) {
                    if (!isEnd()) {
                        isScrollEnd = false;
                        if (!isLoadingForward) {
                            isLoadingForward = true;
                            MessageManager.getInstance().getHistoryAfterAsync(mTalk.mTalkOtherUserId, mTalk.mTalkOtherUserSource, bottomMessage.mLocalId, msgCountPerRequest, new LoadHistoryAfterCallBack(GmacsChatActivity.this, bottomMessage.mLocalId));
                        }
                    }
                }
            }

            @Override
            public void onScroll(AbsListView absListView, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
            }
        });
        MessageLogic.getInstance().setSendMessageListener(this);
    }

    @Override
    protected void initData() {
        EventBus.getDefault().register(this);
        mPowerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP
                && mPowerManager.isWakeLockLevelSupported(PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK)) {
            mWakeLock = mPowerManager.newWakeLock(PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK, "wakeLock");
            mWakeLock.setReferenceCounted(false);
            mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
            mProximitySensor = mSensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
            mSensorEventListener = new SensorEventListener() {
                @Override
                public void onSensorChanged(SensorEvent event) {
                    if (mProximitySensor == null) {
                        return;
                    }

                    float maxRange = mProximitySensor.getMaximumRange();
                    if (event.values[0] >= maxRange / 2) {
                        if (SoundPlayer.getInstance().isSpeakerphoneOn()) {
                            SoundPlayer.getInstance().setSpeakerphoneOn(true);
                        }
                        if (mWakeLock.isHeld()) {
                            mWakeLock.release();
                        }
                        isNear = false;
                    } else {
                        if (!SoundPlayer.getInstance().isSoundPlaying()) {
                            return;
                        }
                        if (!isNear) {
                            SoundPlayer.getInstance().setSpeakerphoneOn(false);
                        }
                        if (!mWakeLock.isHeld()) {
                            mWakeLock.acquire();
                        }
                        isNear = true;
                    }
                }

                @Override
                public void onAccuracyChanged(Sensor sensor, int accuracy) {
                }
            };
        } else {
            mWakeLock = null;
            mSensorManager = null;
            mPowerManager = null;
            mProximitySensor = null;
            mSensorEventListener = null;
        }
        if (!parseExtraObjects(getIntent())) {
            finish();
            return;
        }

        setChatAdapter(getChatAdapter());
        RecentTalkManager.getInstance().getTalkByIdAsync(mTalk.mTalkOtherUserId, mTalk.mTalkOtherUserSource, new GetTalkCallBack(this));

        if (focusMessageLocalId == -1) {
            loadHistoryMsgs();
            chatListView.hideFooterView();
        } else {
            focusToTargetMsg();
        }
        loadPAFunctionConfig();
        resetViewState();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissionOnNeed(Manifest.permission.READ_EXTERNAL_STORAGE,
                    GmacsConstant.REQUEST_CODE_READ_EXTERNAL_STORAGE);
        }
        clearNotice();
    }

    protected void setChatAdapter(GmacsChatAdapter chatAdapter) {
        this.chatAdapter = chatAdapter;
        chatListView.setAdapter(chatAdapter);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitleBarDelegateResId(R.layout.gmacs_activity_chat_titlebar_delegate);
        setContentView(R.layout.gmacs_activity_chat);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (mTalk != null) {
            mTalk.setTalkState(Talk.TALK_STATE_ING);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mSensorManager != null) {
            mSensorManager.registerListener(mSensorEventListener, mProximitySensor, SensorManager.SENSOR_DELAY_NORMAL);
        }
        ClientManager.getInstance().registerLogViewListener(this);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        lastMsgLocalId = -1;
        if (mTalk != null) {
            ContactsManager.getInstance().unRegisterUserInfoChange(mTalk.mTalkOtherUserId, mTalk.mTalkOtherUserSource, this);
            RecentTalkManager.getInstance().deactiveTalk(mTalk);
            RecentTalkManager.getInstance().ackTalkShow(mTalk.mTalkOtherUserId, mTalk.mTalkOtherUserSource);
            saveDraft();
        }
        if (!parseExtraObjects(intent)) {
            finish();
            return;
        }
        if (chatAdapter != null) {
            chatAdapter.clearData();
            chatAdapter.setTalk(mTalk);
            userInfoCache.clear();
        } else {
            setChatAdapter(new GmacsChatAdapter(this, mTalk, userInfoCache));
        }
        chatListView.hideFooterView();
        cachedNewestMessages = null;
        preFetchMessages = null;

        RecentTalkManager.getInstance().getTalkByIdAsync(mTalk.mTalkOtherUserId, mTalk.mTalkOtherUserSource, new GetTalkCallBack(this));
        loadHistoryMsgs();
        loadPAFunctionConfig();
        resetViewState();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissionOnNeed(Manifest.permission.READ_EXTERNAL_STORAGE,
                    GmacsConstant.REQUEST_CODE_READ_EXTERNAL_STORAGE);
        }
        clearNotice();
    }

    /**
     * 重新设置view状态，从onNewIntent方法进入时需要重置状态
     */
    protected void resetViewState() {
        setSendMoreItemResources();
        shouldShowInputSoftAuto = getShouldShowInputSoftAutoConfig();
        chatListView.setPullRefreshEnable(true);
        sendMsgLayout.switchSendText(false);
        // 特定会话隐藏底部控件
        if (TalkType.isSystemTalk(mTalk)) {
            sendMsgLayout.setVisibility(View.GONE);
        } else {
            sendMsgLayout.setVisibility(View.VISIBLE);
        }
        if (TalkType.isGroupTalk(mTalk)) {
            mTitleBar.talkDetailEntry.setImageResource(R.drawable.gmacs_ic_group);
            mTitleBar.talkDetailEntry.setVisibility(View.GONE);
        } else {
            mTitleBar.talkDetailEntry.setImageResource(R.drawable.gmacs_ic_user);
            mTitleBar.talkDetailEntry.setVisibility(View.VISIBLE);
        }
        mTitleBar.talkDetailEntry.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    Intent intent = new Intent(GmacsChatActivity.this,
                            Class.forName(GmacsUiUtil.getTalkDetailActivityClassName()));
                    intent.putExtra(GmacsConstant.EXTRA_USER_ID, mTalk.mTalkOtherUserId);
                    intent.putExtra(GmacsConstant.EXTRA_TALK_TYPE, Gmacs.TalkType.TALKTYPE_NORMAL.getValue());
                    intent.putExtra(GmacsConstant.EXTRA_USER_SOURCE, mTalk.mTalkOtherUserSource);
                    startActivity(intent);
                } catch (ClassNotFoundException ignored) {
                }
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mWakeLock != null && mWakeLock.isHeld()) {
            mWakeLock.release();
        }
        if (mSensorManager != null) {
            mSensorManager.unregisterListener(mSensorEventListener);
        }

        ClientManager.getInstance().unRegisterLogViewListener();
    }

    @Override
    protected void onStop() {
        super.onStop();
        sendMsgLayout.stopRecord();
        if (mTalk != null) {
            mTalk.setTalkState(Talk.TALK_STATE_PAUSE);
        }
        if (!isNear) {
            SoundPlayer.getInstance().stopPlayAndAnimation();
        }
    }

    /**
     * 初始化发送消息layout，子类可以重写该方法
     */
    protected void initSendMsgLayout() {
        sendMsgLayout = (SendMsgLayout) findViewById(R.id.send_msg_layout);
        sendMsgLayout.setRecord(mRecord);
        sendMsgLayout.setGmacsChatActivity(this);
        sendMsgLayout.registerOnMoreItemClick(this);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, final Intent data) {
        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_GALLERY_CODE) {
                if (data == null) {
                    return;
                }
                ImageUrlArrayListWrapper wrapper = data.getParcelableExtra(AlbumConstant.KEY_SELECTED_IMG_DATA);
                ArrayList<String> dataList = wrapper.mList;
                if (dataList == null) {
                    ToastUtil.showToast(R.string.no_file_selected);
                    return;
                }
                for (int i = 0; i < dataList.size(); i++) {
                    String filePath = dataList.get(i);
                    String tmp = filePath.toLowerCase();
                    boolean isPictureFile = tmp.endsWith(".png") || tmp.endsWith(".bmp") || tmp.endsWith(".jpeg") || tmp.endsWith(".jpg");
                    if (isPictureFile) {
                        sendImageMsg(filePath, data.getBooleanExtra(AlbumConstant.RAW, false), "");
                    } else {
                        ToastUtil.showToast(R.string.file_format_not_support);
                    }
                }
            } else if (requestCode == REQUEST_TAKE_PHOTO_CODE) {
                if (!TextUtils.isEmpty(picturePath)) {
                    File file = new File(picturePath);
                    if (file.exists()) {
                        Intent scanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                        scanIntent.setData(Uri.fromFile(file));
                        sendBroadcast(scanIntent);

                        Intent intent = new Intent(this, GmacsPhotoBrowserActivity.class);
                        ArrayList<String> arrayList = new ArrayList<>();
                        arrayList.add(picturePath);
                        intent.putExtra(AlbumConstant.KEY_SELECTED_IMG_DATA, new ImageUrlArrayListWrapper(arrayList));
                        intent.putExtra(AlbumConstant.IS_PREVIEW, true);
                        intent.putExtra(AlbumConstant.FROM_CAMERA, true);
                        startActivityForResult(intent, REQUEST_CAMERA_CODE);
                    }
                }
            } else if (requestCode == REQUEST_LOCATION_CODE) {
                if (data == null) {
                    return;
                }
                double longitude = data.getDoubleExtra(IMLocationMsgView.LONGITUDE, -1f);
                double latitude = data.getDoubleExtra(IMLocationMsgView.LATITUDE, -1f);

                if (longitude == -1 && latitude == -1) {
                    ToastUtil.showToast(R.string.locate_failed);
                } else {
                    sendLocationMsg(longitude, latitude, data.getStringExtra(IMLocationMsgView.ADDRESS), "");
                }
            } else if (requestCode == REQUEST_CAMERA_CODE) {
                sendImageMsg(picturePath, data.getBooleanExtra(AlbumConstant.RAW, false), "");
            } else if (requestCode == REQUEST_AT_CODE) {
                if (data != null) {
                    sendMsgLayout.insertAtText(
                            true,
                            data.getStringExtra(GmacsConstant.EXTRA_NAME),
                            data.getStringExtra(GmacsConstant.EXTRA_USER_ID),
                            data.getIntExtra(GmacsConstant.EXTRA_USER_SOURCE, -1),
                            data.getStringExtra("realName"));
                }
            }
        } else if (requestCode == REQUEST_AT_CODE) {
            sendMsgLayout.showInputSoft();
        } else if (requestCode == REQUEST_IMAGE_CODE && resultCode == RESULT_CODE_IMAGE_DELETED) {
            long localId = data.getLongExtra(AlbumConstant.DELETING_MSG_LOCAL_ID, 0);
            if (localId > 0) {
                List<Long> localIdList = new ArrayList<>();
                localIdList.add(localId);
                removeMessageFromAdapter(localIdList);
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onAlbumImagesDeleted(WChatAlbumImagesDeletedEvent event) {
        if (!isFinishing() && mTalk != null && TextUtils.equals(mTalk.mTalkOtherUserId, event.getUserId()) && mTalk.mTalkOtherUserSource == event.getUserSource()) {
            if (chatAdapter != null) {
                removeMessageFromAdapter(event.getDeletedLocalIdList());
            }
        }
    }

    private void removeMessageFromAdapter(List<Long> deletedLocalIdList) {
        if (deletedLocalIdList != null && deletedLocalIdList.size() > 0) {
            chatAdapter.removeMessageFromAdapter(deletedLocalIdList);
            for (long localId : deletedLocalIdList) {
                updateBottomMessage(localId);
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mRecord != null && mRecord.isRecording()) {
            mRecord.stopRecord();
        }
        SoundPlayer.getInstance().destroy();
        mWakeLock = null;
        mPowerManager = null;
        mSensorEventListener = null;
        mProximitySensor = null;
        mSensorManager = null;
        SoundRecordUtil.destroy();
        if (mTalk != null) {
            mTalk.setTalkState(Talk.TALK_STATE_END);
            RecentTalkManager.getInstance().deactiveTalk(mTalk);
            RecentTalkManager.getInstance().ackTalkShow(mTalk.mTalkOtherUserId, mTalk.mTalkOtherUserSource);
            ContactsManager.getInstance().unRegisterUserInfoChange(mTalk.mTalkOtherUserId, mTalk.mTalkOtherUserSource, this);
            saveDraft();
        }
        EventBus.getDefault().unregister(this);
        handler.removeCallbacksAndMessages(null);
        IMLocationMsgView.releaseLocationBitmapCache();
        MessageLogic.getInstance().setSendMessageListener(null);
    }

    /**
     * 收到新消息
     */
    protected void receivedNewMsg(Message message) {
        if (!isFinishing() && mTalk != null && mTalk.hasTheSameTalkIdWith(message)) {
            if (isEnd()) {
                updateBottomMessage(message);
                if (chatListView != null) {
                    int size = chatListView.getCount();
                    if (chatListView.getLastVisiblePosition() >= size - 1 - chatListView.getFooterViewsCount()) {
                        setListViewTranscriptMode(ListView.TRANSCRIPT_MODE_ALWAYS_SCROLL);
                    } else {
                        setListViewTranscriptMode(ListView.TRANSCRIPT_MODE_DISABLED);
                    }
                    chatAdapter.addMsgToEndPosition(message, new GmacsChatAdapter.FillGroupMemberInfoCb() {
                        @Override
                        public void done() {
                            lastMsgLocalId = getLastMsgLocalId();
                        }
                    });
                }
            } else if (cachedNewestMessages != null) {
                cachedNewestMessages.add(0, message);
                cachedNewestMessages.remove(cachedNewestMessages.size() - 1);
            }
            RecentTalkManager.getInstance().ackTalkShow(mTalk.mTalkOtherUserId, mTalk.mTalkOtherUserSource);
            updateLastMessage(message);
        }
    }

    /**
     * 从extra中解析一些数据
     *
     * @param intent
     */
    protected boolean parseExtraObjects(Intent intent) {
        int talkType = intent.getIntExtra(GmacsConstant.EXTRA_TALK_TYPE, 0);
        String id = intent.getStringExtra(GmacsConstant.EXTRA_USER_ID);
        int source = intent.getIntExtra(GmacsConstant.EXTRA_USER_SOURCE, -1);
        focusMessageLocalId = intent.getLongExtra(GmacsConstant.EXTRA_FOCUS_MESSAGE_LOCAL_ID, -1);
        if (talkType == 0 || TextUtils.isEmpty(id) || source == -1) {
            return false;
        }

        // 避免自己和自己聊天
        if (id.equals(GmacsUser.getInstance().getUserId())
                && source == GmacsUser.getInstance().getSource()) {
            return false;
        }

        mTalk = new Talk();
        mTalk.mTalkOtherUserId = id;
        mTalk.mTalkOtherUserSource = source;
        mTalk.mTalkType = talkType;
        mTalk.setTalkState(Talk.TALK_STATE_ING);
        sendMsgLayout.setTalk(mTalk);
        RecentTalkManager.getInstance().activeTalk(mTalk);
        ContactsManager.getInstance().registerUserInfoChange(mTalk.mTalkOtherUserId, mTalk.mTalkOtherUserSource, this);
        return true;
    }

    private boolean isEnd() {
        return latestMessage == null || bottomMessage == null || bottomMessage.mLocalId == latestMessage.mLocalId;
    }

    private void addMsgToAdapter(Message message) {
        updateBottomMessage(message);
        updateLastMessage(message);
        // 将信息先显示到列表中
        chatAdapter.addMsgToEndPosition(message, new GmacsChatAdapter.FillGroupMemberInfoCb() {
            @Override
            public void done() {
                chatListView.setSelection(chatAdapter.getCount() - 1);
                lastMsgLocalId = getLastMsgLocalId();
            }
        });
    }

    private void focusToTargetMsg() {
        getFocusMessages(focusMessageLocalId, new OnGetFocusMessagesCb() {
            @Override
            public void onGetFocusMessages(final int focusIndex, final List<Message> messages) {
                updateBottomMessage(messages);
                chatAdapter.addMsgsToStartPosition(messages, new GmacsChatAdapter.FillGroupMemberInfoCb() {
                    @Override
                    public void done() {
                        int selectionIndex = (messages.size() - 1 - focusIndex) + chatListView.getHeaderViewsCount();
                        chatListView.setSelection(selectionIndex);
                        checkHasMoreBackwards(messages.get(messages.size() - 1));
                        lastMsgLocalId = getLastMsgLocalId();
                        if (isEnd()) {
                            chatListView.hideFooterView();
                        } else {
                            MessageManager.getInstance().getHistoryAsync(mTalk.mTalkOtherUserId
                                    , mTalk.mTalkOtherUserSource
                                    , -1
                                    , msgCountPerRequest
                                    , new MessageManager.GetHistoryMsgCb() {
                                        @Override
                                        public void done(int errorCode, String errorMessage, List<Message> msgList) {
                                            if (msgList != null) {
                                                cachedNewestMessages = msgList;
                                                if (msgList.size() < onePageCount && msgList.get(msgList.size() - 1).mLinkMsgId != -3) {
                                                    MessageManager.getInstance().getHistoryAsync(mTalk.mTalkOtherUserId, mTalk.mTalkOtherUserSource
                                                            , cachedNewestMessages.get(cachedNewestMessages.size() - 1).mLocalId
                                                            , msgCountPerRequest, new MessageManager.GetHistoryMsgCb() {
                                                                @Override
                                                                public void done(int errorCode, String errorMessage, List<Message> msgList) {
                                                                    if (msgList != null) {
                                                                        cachedNewestMessages.addAll(0, msgList);
                                                                    }
                                                                }
                                                            });
                                                }
                                            }
                                        }
                                    });
                        }
                    }
                });
            }
        });
    }

    @Override
    public void onShowLogView(final String log) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (!isFinishing()) {
                    final GmacsDialog.Builder dialog = new GmacsDialog.Builder(GmacsChatActivity.this,
                            GmacsDialog.Builder.DIALOG_TYPE_LARGE_TEXT_NEU_BUTTON);
                    dialog.initDialog(log, getText(R.string.close), new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            dialog.dismiss();
                        }
                    }).create().show();
                }
            }
        });
    }

    protected Message.MessageUserInfo getReceiverInfo() {
        if (receiverInfo != null) {
            return receiverInfo;
        } else {
            receiverInfo = new Message.MessageUserInfo();
            receiverInfo.mUserId = mTalk.mTalkOtherUserId;
            receiverInfo.mUserSource = mTalk.mTalkOtherUserSource;
            receiverInfo.mDeviceId = "";
        }
        return receiverInfo;
    }

    /**
     * 拉取历史记录
     */
    private void loadHistoryMsgs() {
        isLoadingBackward = true;
        MessageManager.getInstance().getHistoryAsync(mTalk.mTalkOtherUserId,
                mTalk.mTalkOtherUserSource, lastMsgLocalId, msgCountPerRequest
                , new LoadHistoryMessageCallBack(this, lastMsgLocalId));
    }

    private long getLastMsgLocalId() {
        if (chatAdapter.getCount() > 0) {
            return chatAdapter.getItem(0).mLocalId;
        }
        return -1;
    }

    /**
     * Downloading public account function config.
     */
    private void loadPAFunctionConfig() {
        sendMsgLayout.mPublicAccountMenuBtn.setVisibility(View.GONE);
        publicAccountMenu.setVisibility(LinearLayout.GONE);
        if (TalkType.isOfficialTalk(mTalk)) {
            ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

            // Local cache first, whatever the Internet is available.
            String menuData = (String) GmacsConfig.ClientConfig.getParam(mTalk.getTalkId() +
                    GmacsConfig.ClientConfig.KEY_PA_FUNCTION_CONFIG, "");
            boolean isEmpty = publicAccountMenu.setConfig(GmacsChatActivity.this,
                    PAFunctionConfig.buildPAFunctionConfig(menuData));

            if (isEmpty) {
                publicAccountMenu.setVisibility(LinearLayout.GONE);
                sendMsgLayout.setVisibility(View.VISIBLE);
                sendMsgLayout.mPublicAccountMenuBtn.setVisibility(View.GONE);
            } else {
                publicAccountMenu.setVisibility(LinearLayout.VISIBLE);
                sendMsgLayout.setVisibility(View.GONE);
                sendMsgLayout.mPublicAccountMenuBtn.setVisibility(View.VISIBLE);
            }

            if (cm.getActiveNetworkInfo() != null) {
                ContactLogic.getInstance().getPAFunctionConfig(mTalk.mTalkOtherUserId, mTalk.mTalkOtherUserSource);
            }
        } else if (TalkType.isSystemTalk(mTalk)) {
            sendMsgLayout.setVisibility(View.GONE);
        } else {
            sendMsgLayout.setVisibility(View.VISIBLE);
        }
    }

    @WorkerThread
    private void addAndSelectionFromTopMsgs(final List<Message> msgs) {
        if (lastMsgLocalId == -1) {
            updateBottomMessage(msgs);
        }

        chatAdapter.addMsgsToStartPosition(msgs, new GmacsChatAdapter.FillGroupMemberInfoCb() {
            @Override
            public void done() {
                if (chatListView.getTranscriptMode() == ListView.TRANSCRIPT_MODE_DISABLED) {
                    chatListView.setSelectionFromTop(msgs.size() + chatListView.getHeaderViewsCount(),
                            chatListView.mHeaderView.getHeight());
                }
                checkHasMoreBackwards(msgs.get(msgs.size() - 1));
                final boolean needAutoFetch = backwardHasMore && -1 == lastMsgLocalId && msgs.size() < onePageCount;
                lastMsgLocalId = getLastMsgLocalId();
                isLoadingBackward = false;
                if (needAutoFetch) {
                    setListViewTranscriptMode(ListView.TRANSCRIPT_MODE_ALWAYS_SCROLL);
                    chatListView.startLoadMore();
                } else {
                    chatListView.stopLoadMore();
                }
            }
        });
    }

    private void checkHasMoreBackwards(Message topMessage) {
        backwardHasMore = topMessage.mLinkMsgId != -3;
    }

    private void showReminder() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                messageReminderBar.setVisibility(View.VISIBLE);
                messageReminderBar.setText(getResources().getString(R.string.wchat_new_message_count,
                        mTalk.mNoReadMsgCount > 99 ? "99+" : String.valueOf(mTalk.mNoReadMsgCount)));
                messageReminderBar.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        scrollToLastShowedMessage();
                        messageReminderBar.setVisibility(View.GONE);
                    }
                });
            }
        });
    }

    private void preFetchMessageReminder() {
        MessageManager.getInstance().getFirstUnreadPageAsync(mTalk.mTalkOtherUserId, mTalk.mTalkOtherUserSource, msgCountPerRequest, new MessageManager.GetHistoryMsgCb() {
            @Override
            public void done(int errorCode, String errorMessage, List<Message> msgList) {
                if (errorCode == 0 && msgList.size() > 0) {
                    Message remainderMessage = getReminderMessage(msgList.get(msgList.size() - 1));
                    msgList.add(msgList.size() - 1, remainderMessage);
                    preFetchMessages = msgList;
                    showReminder();
                }
            }
        });
    }

    private void getFocusMessages(final long localId, final OnGetFocusMessagesCb cb) {
        MessageManager.getInstance().getMessagesAsync(mTalk.mTalkOtherUserId, mTalk.mTalkOtherUserSource, new long[]{localId}, new MessageManager.GetHistoryMsgCb() {
            @Override
            public void done(int errorCode, String errorMessage, final List<Message> msgList1) {
                MessageManager.getInstance().getHistoryAsync(mTalk.mTalkOtherUserId, mTalk.mTalkOtherUserSource, localId, msgCountPerRequest, new MessageManager.GetHistoryMsgCb() {
                    @Override
                    public void done(int errorCode, String errorMessage, final List<Message> msgList2) {
                        MessageManager.getInstance().getHistoryAfterAsync(mTalk.mTalkOtherUserId, mTalk.mTalkOtherUserSource, localId, msgCountPerRequest, new MessageManager.GetHistoryMsgCb() {
                            @Override
                            public void done(int errorCode, String errorMessage, final List<Message> msgList3) {
                                final ArrayList<Message> messages = new ArrayList<>();
                                if (msgList3 != null) {
                                    messages.addAll(msgList3);
                                }
                                if (msgList1 != null) {
                                    messages.addAll(msgList1);
                                }
                                if (msgList2 != null) {
                                    messages.addAll(msgList2);
                                }
                                if (cb != null) {
                                    cb.onGetFocusMessages(msgList3 == null ? 0 : msgList3.size(), messages);
                                }

                            }
                        });

                    }
                });
            }
        });
    }

    @NonNull
    private Message getReminderMessage(Message lastShowedMessage) {
        IMTipMsg tipMsg = new IMTipMsg();
        SpannableString spannableString = new SpannableString("-      以下为新消息      -");
        TextView textView = new TextView(this);
        textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
        int drawableLength = (int) ((GmacsEnvi.screenWidth
                - textView.getPaint().measureText(spannableString.toString())
                - getResources().getDimensionPixelOffset(R.dimen.im_chat_notice_margin_left) * 2) / 2);
        Drawable drawable = new ColorDrawable(Color.parseColor("#C9C9C9"));
        drawable.setBounds(0, 0, drawableLength, GmacsUtils.dipToPixel(0.5f));
        DrawableSpan drawableSpanStart = new DrawableSpan(drawable);
        DrawableSpan drawableSpanEnd = new DrawableSpan(drawable);
        ForegroundColorSpan frontColorSpan = new ForegroundColorSpan(Color.parseColor("#999999"));
        spannableString.setSpan(drawableSpanStart, 0, 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        spannableString.setSpan(drawableSpanEnd, 19, 20, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        spannableString.setSpan(frontColorSpan, 7, 13, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        tipMsg.mText = spannableString;
        Message remainderMessage = new Message();
        remainderMessage.mLocalId = lastShowedMessage.mLocalId;
        remainderMessage.mMsgId = lastShowedMessage.mMsgId;
        remainderMessage.mSenderInfo = lastShowedMessage.mSenderInfo;
        remainderMessage.mReceiverInfo = lastShowedMessage.mReceiverInfo;
        remainderMessage.mTalkType = lastShowedMessage.mTalkType;
        remainderMessage.setMsgContent(tipMsg);
        remainderMessage.mMsgUpdateTime = lastShowedMessage.mMsgUpdateTime;
        return remainderMessage;
    }

    /**
     * 清除notice
     */
    private void clearNotice() {
        int notice = mTalk.getTalkId().hashCode();
        if (notice != 0) {
            ((NotificationManager) getSystemService(NOTIFICATION_SERVICE)).cancel(notice);
        }
    }

    protected void setSomethingByUserInfo(UserInfo info) {
        // 快捷回复，放在此处，宿主可能需要根据具体Talk以及userType重写不同内容
        sendMsgLayout.initQuickMsgView(getQuickMsgContents());
        // 是否需要发送语音、表情、更多 入口，放在此处，宿主可能需要根据具体Talk以及userType重写设置
        sendMsgLayout.setSendAudioEnable(true);
        sendMsgLayout.setSendEmojiEnable(true);
        sendMsgLayout.setSendMoreEnable(true);
    }

    public UserInfo getOtherUserInfo() {
        return mTalk.mTalkOtherUserInfo;
    }

    public void setShouldShowInputSoftAuto(boolean shouldShowInputSoftAuto) {
        this.shouldShowInputSoftAuto = shouldShowInputSoftAuto;
    }

    public boolean getShouldShowInputSoftAutoConfig() {
        return true;
    }

    public void stopScroll() {
        if (chatListView != null) {
            chatListView.smoothScrollBy(0, 0);
        }
    }

    @Override
    protected void updateData() {
        if (chatAdapter != null) {
            chatAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (grantResults.length == 0) {
            return;
        }

        switch (requestCode) {
            case GmacsConstant.REQUEST_CODE_READ_EXTERNAL_STORAGE:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    updateData();
                }
                break;
            case GmacsConstant.REQUEST_CODE_ALBUM:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    openAlbumActivity();
                    updateData();
                } else {
                    ToastUtil.showToast(R.string.permission_storage_read);
                }
                break;
            case GmacsConstant.REQUEST_CODE_CAMERA:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    openCameraActivity();
                } else {
                    ToastUtil.showToast(R.string.permission_camera);
                }
                break;
            case GmacsConstant.REQUEST_CODE_ACCESS_LOCATION:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    openLocationActivity();
                } else {
                    ToastUtil.showToast(R.string.permission_location);
                }
                break;
            case GmacsConstant.REQUEST_CODE_RECORD_AUDIO:
                if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    ToastUtil.showToast(R.string.permission_record_audio);
                }
                break;
        }
    }

    private void refreshSelfInfo() {
        if (chatAdapter != null) {
            int firstVisiblePosition = chatListView.getFirstVisiblePosition()
                    - chatListView.getHeaderViewsCount();
            int lastVisiblePosition = chatListView.getLastVisiblePosition()
                    - chatListView.getHeaderViewsCount();
            for (int i = firstVisiblePosition; i <= lastVisiblePosition; i++) {
                Message message = chatAdapter.getItem(i);
                if (message != null && message.mIsSelfSendMsg) {
                    chatAdapter.updateRightUser(chatListView.getChildAt(i - firstVisiblePosition));
                }
            }
        }
    }

    private void refreshMessageItemWithUserInfo(UserInfo userInfo) {
        if (chatAdapter != null) {
            int firstVisiblePosition = chatListView.getFirstVisiblePosition()
                    - chatListView.getHeaderViewsCount();
            int lastVisiblePosition = chatListView.getLastVisiblePosition()
                    - chatListView.getHeaderViewsCount();
            for (int i = firstVisiblePosition; i <= lastVisiblePosition; i++) {
                Message message = chatAdapter.getItem(i);
                if (message != null && !message.mIsSelfSendMsg) {
                    Message.MessageUserInfo messageUserInfo = message.mSenderInfo;
                    if (TextUtils.equals(userInfo.getId(), messageUserInfo.mUserId)
                            && userInfo.getSource() == messageUserInfo.mUserSource) {
                        chatAdapter.updateLeftUser(chatListView.getChildAt(i - firstVisiblePosition), i);
                    }
                }
            }
        }
    }

    private void refreshMessageItemWithGroupMember(GroupMember member) {
        if (chatAdapter != null) {
            int firstVisiblePosition = chatListView.getFirstVisiblePosition()
                    - chatListView.getHeaderViewsCount();
            int lastVisiblePosition = chatListView.getLastVisiblePosition()
                    - chatListView.getHeaderViewsCount();
            for (int i = firstVisiblePosition; i <= lastVisiblePosition; i++) {
                Message message = chatAdapter.getItem(i);
                if (message != null && !message.mIsSelfSendMsg) {
                    Message.MessageUserInfo messageUserInfo = message.mSenderInfo;
                    if (TextUtils.equals(member.getId(), messageUserInfo.mUserId)
                            && member.getSource() == messageUserInfo.mUserSource) {
                        chatAdapter.updateLeftUser(chatListView.getChildAt(i - firstVisiblePosition), i);
                    }
                }
            }
        }
    }

    private void refreshMessageItemWithGroupMembers(List<GroupMember> groupMembers) {
        if (chatAdapter != null) {
            int firstVisiblePosition = chatListView.getFirstVisiblePosition()
                    - chatListView.getHeaderViewsCount();
            int lastVisiblePosition = chatListView.getLastVisiblePosition()
                    - chatListView.getHeaderViewsCount();
            for (int i = firstVisiblePosition; i <= lastVisiblePosition; i++) {
                Message message = chatAdapter.getItem(i);
                if (message != null && !message.mIsSelfSendMsg) {
                    Message.MessageUserInfo messageUserInfo = message.mSenderInfo;
                    for (GroupMember groupMember : groupMembers) {
                        if (TextUtils.equals(groupMember.getId(), messageUserInfo.mUserId)
                                && groupMember.getSource() == messageUserInfo.mUserSource) {
                            chatAdapter.updateLeftUser(chatListView.getChildAt(i - firstVisiblePosition), i);
                            break;
                        }
                    }
                }
            }
        }
    }

    private void showHistory(String errorMessage, long beginMsgLocalId, List<Message> msgList) {
        if (null == mTalk || isFinishing()) {
            return;
        }
        if (msgList != null) {
            final int size = msgList.size();
            if (size > 0 && beginMsgLocalId == this.lastMsgLocalId) {
                Message message = msgList.get(0);
                Message.MessageUserInfo info = message.getTalkOtherUserInfo();
                if (!info.mUserId.equals(mTalk.mTalkOtherUserId) || info.mUserSource != mTalk.mTalkOtherUserSource) {
                    return;
                }
                msgList = parseHistoryMessages(mTalk, msgList);

                addAndSelectionFromTopMsgs(msgList);
            } else {
                stopLoadMore();
            }
        } else {
            stopLoadMore();
            ToastUtil.showToast(errorMessage);
        }
        if (beginMsgLocalId == -1 && !TextUtils.isEmpty(mTalk.mDraftBoxMsg)) {
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    sendMsgLayout.setMsgEditText(mTalk.mDraftBoxMsg);
                    sendMsgLayout.showInputSoft();
                }
            }, 100);
        }
    }

    private void showHistoryAfter(String errorMessage, long bottomMessageLocalId, final List<Message> msgList) {
        if (null == mTalk || isFinishing()) {
            return;
        }
        if (msgList != null) {
            if (msgList.size() > 0 && bottomMessage != null && bottomMessageLocalId == bottomMessage.mLocalId) {
                final int fromTop = chatListView.getChildAt(chatListView.getChildCount() - 1).getTop();
                final int index = chatListView.getLastVisiblePosition();
                updateBottomMessage(msgList);
                chatAdapter.addMsgsToEndPosition(msgList, new GmacsChatAdapter.FillGroupMemberInfoCb() {
                    @Override
                    public void done() {
                        chatListView.setSelectionFromTop(index, fromTop);
                        if (isEnd()) {
                            chatListView.hideFooterView();
                            cachedNewestMessages = null;
                        }
                        isLoadingForward = false;
                    }
                });
            } else {
                if (isEnd()) {
                    chatListView.hideFooterView();
                    cachedNewestMessages = null;
                }
                isLoadingForward = false;
            }
        } else {
            isLoadingForward = false;
            ToastUtil.showToast(errorMessage);
        }
        RecentTalkManager.getInstance().ackTalkShow(mTalk.mTalkOtherUserId, mTalk.mTalkOtherUserSource);
    }

    private void stopLoadMore() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                isLoadingBackward = false;
                chatListView.stopLoadMore();
            }
        });
    }

    /**
     * 打开图片选择页面
     */
    protected void openAlbumActivity() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            PermissionUtil.requestPermissions(this
                    , new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}
                    , GmacsConstant.REQUEST_CODE_ALBUM
                    , new PermissionUtil.PermissionCallBack() {
                        @Override
                        public void onCheckedPermission(boolean isGranted) {
                            if (isGranted) {
                                if (!FileUtil.sdcardAvailable()) {
                                    ToastUtil.showToast(R.string.sdcard_not_exist);
                                } else {
                                    Intent intent = new Intent(GmacsChatActivity.this, GmacsAlbumActivity.class);
                                    intent.putExtra(AlbumConstant.EXTRA_PHOTO_MAX_COUNT, 10);
                                    startActivityForResult(intent, REQUEST_GALLERY_CODE);
                                }
                            } else {
                                ToastUtil.showToast(R.string.permission_storage_read);
                            }
                        }
                    });
        } else {
            if (!FileUtil.sdcardAvailable()) {
                ToastUtil.showToast(R.string.sdcard_not_exist);
            } else {
                Intent intent = new Intent(GmacsChatActivity.this, GmacsAlbumActivity.class);
                intent.putExtra(AlbumConstant.EXTRA_PHOTO_MAX_COUNT, 10);
                startActivityForResult(intent, REQUEST_GALLERY_CODE);
            }
        }
    }

    /**
     * 打开拍照页面
     */
    protected void openCameraActivity() {
        PermissionUtil.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, GmacsConstant.REQUEST_CODE_CAMERA, new PermissionUtil.PermissionCallBack() {
            @Override
            public void onCheckedPermission(boolean isGranted) {
                if (isGranted) {
                    if (!FileUtil.sdcardAvailable()) {
                        ToastUtil.showToast(R.string.sdcard_not_exist);
                        return;
                    }

                    Intent openCameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                    String name = DateFormat.format("yyyyMMdd_hhmmss",
                            Calendar.getInstance(Locale.CHINA)) + ".jpg";

                    File dir = new File(GmacsUiUtil.SAVE_IMAGE_FILE_DIR);
//                    File dir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "newbroker");
                    if (!dir.exists()) {
                        dir.mkdirs();
                    }
                    File file = new File(dir, name);
                    Uri imageUri;
                    picturePath = file.getAbsolutePath();
                    if (Build.VERSION.SDK_INT >= 24) {
                        imageUri = FileProvider.getUriForFile(GmacsChatActivity.this, FileProviderUtil.getFileProviderAuthority(GmacsChatActivity.this), file);
                    } else {
                        imageUri = Uri.fromFile(file);
                    }
                    openCameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
                    openCameraIntent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    startActivityForResult(openCameraIntent, REQUEST_TAKE_PHOTO_CODE);
                } else {
                    ToastUtil.showToast(R.string.permission_camera);
                }
            }
        });
    }

    /**
     * 打开地图位置
     */
    protected void openLocationActivity() {
        PermissionUtil.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION}, GmacsConstant.REQUEST_CODE_ACCESS_LOCATION, new PermissionUtil.PermissionCallBack() {
            @Override
            public void onCheckedPermission(boolean isGranted) {
                if (isGranted) {
                    Intent intent = new Intent(GmacsChatActivity.this, GmacsMapActivity.class);
                    startActivityForResult(intent, REQUEST_LOCATION_CODE);
                } else {
                    ToastUtil.showToast(R.string.permission_location);
                }
            }
        });
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onRemark(RemarkEvent event) {
        if (mTalk != null && !isFinishing()) {
            if (TalkType.isGroupTalk(mTalk)) {
                GroupMember member = userInfoCache.get(Talk.getTalkId(event.getUserSource(), event.getUserId()));
                if (member != null) {
                    if (!TextUtils.equals(event.getRemark().remark_name, member.getRemarkName())) {
                        member.setRemark(event.getRemark());
                        refreshMessageItemWithGroupMember(member);
                    }
                }
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onReceivedNewMessage(Message message) {
        receivedNewMsg(message);
        if (parseNewMessage(mTalk, message)) {
            setListViewTranscriptMode(ListView.TRANSCRIPT_MODE_ALWAYS_SCROLL);
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onKickedOutOfGroupNotificationReceived(KickedOutOfGroupEvent event) {
        if (!isFinishing() && mTalk != null && mTalk.mTalkOtherUserId.equals(event.getOperatedGroupId())
                && mTalk.mTalkOtherUserSource == event.getOperatedGroupSource()) {
            String name = mTalk.mTalkOtherUserInfo.getNameToShow();
            if (TextUtils.isEmpty(name)) {
                name = String.format("群聊(%s)", ((Group) mTalk.mTalkOtherUserInfo).getMembers().size() - 1);
//                mTitleBar.setTitle(name);
                setTitle(name);
            }
//            mTitleBar.talkDetailEntry.setVisibility(View.GONE);
            isKickedOutOfGroup = true;
            supportInvalidateOptionsMenu();
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onGetGroupInfo(final Group group) {
        if (!isFinishing() && mTalk != null && mTalk.mTalkOtherUserId.equals(group.getId())
                && mTalk.mTalkOtherUserSource == group.getSource()) {
//            mTitleBar.talkDetailEntry.setVisibility(View.GONE);
            mTalk.mTalkOtherUserInfo = group;

            String name = group.getNameToShow();
            if (TextUtils.isEmpty(name)) {
                name = String.format("群聊(%s)", group.getMembers().size());
            } else {
                CharSequence ellipsizeStr =
                        TextUtils.ellipsize(name, mTitleBar.titleText.getPaint(),
                                GmacsEnvi.screenWidth - 2 * GmacsUtils.dipToPixel(50) - 6 * getResources().getDimensionPixelOffset(R.dimen.titlebar_text_size),
                                TextUtils.TruncateAt.END);
                name = ellipsizeStr + String.format("(%s)", group.getMembers().size());
            }
//            mTitleBar.setTitle(name);
//            mTitleBar.talkDetailEntry.setVisibility(group.isStranger() ? View.GONE : View.VISIBLE);
            setTitle(name);
            isKickedOutOfGroup = group.isStranger();
            supportInvalidateOptionsMenu();

            List<GroupMember> membersNeedToRefresh = null;
            for (GroupMember member : group.getMembers()) {
                String talkId = Talk.getTalkId(member.getSource(), member.getId());
                GroupMember cachedMember = userInfoCache.get(talkId);
                if (cachedMember != null
                        && (!TextUtils.equals(cachedMember.getGroupNickName(), member.getGroupNickName())
                        || cachedMember.getAuthority() != member.getAuthority())) {
                    cachedMember.setGroupNickName(member.getGroupNickName());
                    cachedMember.setGroupNickNameSpell(member.getGroupNickNameSpell());
                    cachedMember.setAuthority(member.getAuthority());
                    if (membersNeedToRefresh == null) {
                        membersNeedToRefresh = new ArrayList<>();
                    }
                    membersNeedToRefresh.add(cachedMember);
                }
            }
            if (membersNeedToRefresh != null) {
                refreshMessageItemWithGroupMembers(membersNeedToRefresh);
            }

            setSomethingByUserInfo(group);
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onGetUserInfo(Contact contact) {
        if (mTalk != null && !isFinishing()) {
            if (mTalk.mTalkOtherUserId.equals(contact.getId())
                    && mTalk.mTalkOtherUserSource == contact.getSource()) {
                if (mTalk.mTalkOtherUserInfo == null
                        || !TextUtils.equals(mTalk.mTalkOtherUserInfo.avatar, contact.avatar)) {
                    refreshMessageItemWithUserInfo(contact);
                }
                mTalk.mTalkOtherUserInfo = contact;
//                mTitleBar.setTitle(contact.getNameToShow());
                setTitle(contact.getNameToShow());
                setSomethingByUserInfo(contact);
            } else if (TextUtils.equals(contact.getId(), GmacsUser.getInstance().getUserId())
                    && contact.getSource() == GmacsUser.getInstance().getSource()) {
                refreshSelfInfo();
            } else if (TalkType.isGroupTalk(mTalk)) {
                if (chatAdapter.updateGroupMemberInfoCache(contact)) {
                    refreshMessageItemWithUserInfo(contact);
                }
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onPAFunctionConfigEvent(PAFunctionConfig config) {
        if (mTalk != null && !isFinishing()) {
            boolean isEmpty = publicAccountMenu.setConfig(GmacsChatActivity.this, config);
            if (isEmpty) {
                publicAccountMenu.setVisibility(LinearLayout.GONE);
                sendMsgLayout.setVisibility(View.VISIBLE);
                sendMsgLayout.mPublicAccountMenuBtn.setVisibility(View.GONE);
            } else {
                publicAccountMenu.setVisibility(LinearLayout.VISIBLE);
                sendMsgLayout.setVisibility(View.GONE);
                sendMsgLayout.mPublicAccountMenuBtn.setVisibility(View.VISIBLE);
                if (!TextUtils.isEmpty(config.getMenuData())) {
                    GmacsConfig.ClientConfig.setParam(mTalk.getTalkId() +
                            GmacsConfig.ClientConfig.KEY_PA_FUNCTION_CONFIG, config.getMenuData());
                }
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onDeleteContact(DeleteContactEvent event) {
        if (mTalk != null && event.getUserId().equals(mTalk.mTalkOtherUserId) &&
                event.getUserSource() == mTalk.mTalkOtherUserSource) {
            finish();
        }
    }

    /**
     * 获取验证码
     *
     * @param event
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onGetCaptcha(final GetCaptchaEvent event) {
        Captcha captcha = event.getCaptcha();
        if (!isFinishing() && mTalk != null && mTalk.hasTheSameTalkIdWith(event.getMessage())) {
            if (captcha != null) {
                if (captchaDialog == null) {
                    //首次弹出
                    LayoutInflater layoutInflater = LayoutInflater.from(this);
                    View captchaView = layoutInflater.inflate(R.layout.gmacs_captcha, null);
                    final EditText et_captcha = (EditText) captchaView.findViewById(R.id.et_captcha);
                    ImageView iv_captcha = (ImageView) captchaView.findViewById(R.id.iv_captcha);
                    final TextView tv_captcha_title = (TextView) captchaView.findViewById(R.id.tv_captcha_title);
                    final TextView ok = (TextView) captchaView.findViewById(R.id.tv_ok);
                    TextView cancel = (TextView) captchaView.findViewById(R.id.tv_cancel);
                    tv_captcha_title.setTextColor(getResources().getColor(R.color.captcha_text_default));
                    tv_captcha_title.setText(R.string.enter_captcha);
                    iv_captcha.setImageBitmap(captcha.bitmap);
                    iv_captcha.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            GmacsManager.getInstance().updateCaptcha(event.getMessage());
                            et_captcha.setText("");
                        }
                    });
                    et_captcha.setFilters(new InputFilter[]{new InputFilter() {
                        @Override
                        public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
                            if (source.toString().contains(" ")) {
                                return "";
                            } else {
                                return source;
                            }
                        }
                    }});
                    ok.setTag(event);
                    ok.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            final String captchaContent = et_captcha.getText().toString();
                            if (TextUtils.isEmpty(captchaContent)) {
                                tv_captcha_title.setText(R.string.nonnull_captcha);
                                tv_captcha_title.setTextColor(getResources().getColor(R.color.captcha_text_error));
                                return;
                            }
                            tv_captcha_title.setText(R.string.captcha_verifying);
                            tv_captcha_title.setTextColor(getResources().getColor(R.color.captcha_text_default));
                            GetCaptchaEvent event = (GetCaptchaEvent) ok.getTag();
                            GmacsManager.getInstance().validateCaptcha(event.getCaptcha().responseId,
                                    captchaContent, event.getMessage());
                        }
                    });
                    cancel.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            captchaDialog.dismiss();
                            captchaDialog = null;
                            if (msgsNeedIdentify != null) {
                                for (Message message : msgsNeedIdentify) {
                                    message.setMsgSendStatus(GmacsConstant.MSG_SEND_FAILED);
                                }
                                msgsNeedIdentify = null;
                            }
                            updateData();
                        }
                    });
                    captchaDialog = new GmacsDialog.Builder(this, GmacsDialog.Builder.DIALOG_TYPE_CUSTOM_CONTENT_VIEW);
                    GmacsDialog dialog = captchaDialog.initDialog(captchaView).setCancelable(false).create();
                    Window window = dialog.getWindow();
                    if (window != null) {
                        dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
                    }
                    popMsgUpOfSendMsgLayout();
                    dialog.show();
                } else {
                    //已经有Dialog，刷新验证码
                    View captchaView = captchaDialog.getContentView();
                    if (captchaView != null) {
                        ImageView iv_captcha = (ImageView) captchaView.findViewById(R.id.iv_captcha);
                        TextView ok = (TextView) captchaView.findViewById(R.id.tv_ok);
                        iv_captcha.setImageBitmap(captcha.bitmap);
                        ok.setTag(event);
                    }
                }
            } else if (captchaDialog == null) {
                //首次请求失败
                if (msgsNeedIdentify != null) {
                    for (Message message : msgsNeedIdentify) {
                        message.setMsgSendStatus(GmacsConstant.MSG_SEND_FAILED);
                    }
                    msgsNeedIdentify = null;
                    updateData();
                }
            } else if (!NetworkUtil.isNetworkAvailable()) {
                ToastUtil.showToast("网络不通,刷新失败");
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onValidateCaptchaEvent(ValidateCaptchaEvent event) {
        if (!isFinishing() && mTalk != null && mTalk.hasTheSameTalkIdWith(event.getMessage())) {
            if (event.isSuccess()) {
                ToastUtil.showToast(R.string.valid_captcha);
                if (msgsNeedIdentify != null) {
                    for (Message message : msgsNeedIdentify) {
                        reSendMsg(message);
                    }
                    msgsNeedIdentify = null;
                }
                if (captchaDialog != null) {
                    captchaDialog.dismiss();
                    captchaDialog = null;
                }
            } else if (captchaDialog != null) {
                //验证失败，更新Dialog视图
                View captchaView = captchaDialog.getContentView();
                if (captchaView != null) {
                    TextView tv_captcha_title = (TextView) captchaView.findViewById(R.id.tv_captcha_title);
                    EditText et_captcha = (EditText) captchaView.findViewById(R.id.et_captcha);
                    et_captcha.setText("");
                    tv_captcha_title.setText(R.string.invalid_captcha);
                    tv_captcha_title.setTextColor(getResources().getColor(R.color.captcha_text_error));
                    GmacsManager.getInstance().updateCaptcha(event.getMessage());
                }
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onUpdateInsertChatMsg(UpdateInsertChatMsgEvent event) {
        receivedNewMsg(event.getMessage());
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onUnreadMsgCountChanged(UnreadTotalEvent event) {
        if (!isFinishing()) {
            updateTotalUnreadCount(event.getTotal());
        }
    }

    private void onGetTalk(final Talk talk) {
        if (mTalk != null) {
            if (TextUtils.equals(talk.mTalkOtherUserId, mTalk.mTalkOtherUserId) && talk.mTalkOtherUserSource == mTalk.mTalkOtherUserSource) {
                if (talk.mTalkOtherUserInfo == null) {
                    ContactLogic.getInstance().getUserInfo(mTalk.mTalkOtherUserId, mTalk.mTalkOtherUserSource);
                } else {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (talk.mTalkOtherUserInfo instanceof Contact) {
                                onGetUserInfo((Contact) talk.mTalkOtherUserInfo);
                            } else if (talk.mTalkOtherUserInfo instanceof Group) {
                                onGetGroupInfo((Group) talk.mTalkOtherUserInfo);
                            }
                        }
                    });
                }
                mTalk.mDraftBoxMsg = talk.mDraftBoxMsg;
                mTalk.mNoReadMsgCount = talk.mNoReadMsgCount;
                if (talk.getLastMessage() != null) {
                    latestMessage = talk.getLastMessage();
                }
                if (focusMessageLocalId == -1 && talk.mNoReadMsgCount >= onePageCount) {
                    preFetchMessageReminder();
                }
                RecentTalkManager.getInstance().ackTalkShow(mTalk.mTalkOtherUserId, mTalk.mTalkOtherUserSource);
                updateTotalUnreadCount(TalkLogic.getInstance().getUnreadMsgCount() - mTalk.mNoReadMsgCount);
            }
        }
    }

    private void updateTotalUnreadCount(final long count) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (count == 0) {
                    mTitleBar.backText.setText("微聊");
                } else {
                    mTitleBar.backText.setText(getResources().getString(R.string.no_read_msg_count, count < 100 ? String.valueOf(count) : "99+"));
                }
            }
        });
    }

    private void scrollToLastShowedMessage() {
        sendMsgLayout.collapseMoreAndInputMethod();
        setListViewTranscriptMode(ListView.TRANSCRIPT_MODE_DISABLED);
        chatAdapter.addMsgsToStartPosition(preFetchMessages, new GmacsChatAdapter.FillGroupMemberInfoCb() {
            @Override
            public void done() {
                chatListView.smoothScrollToPosition(0);
                cachedNewestMessages = chatAdapter.clearDirtyMsgs(preFetchMessages.size());
                bottomMessage = preFetchMessages.get(0);
                for (Message message : preFetchMessages) {
                    if (message.getSendStatus() == GmacsConstant.MSG_SENT) {
                        bottomSuccessfulMessage = message;
                        break;
                    }
                }
                if (!isEnd()) {
                    chatListView.showFooterView();
                }
                preFetchMessages = null;
                lastMsgLocalId = getLastMsgLocalId();
            }
        });
    }

    private void saveDraft() {
        String newDraft = sendMsgLayout.getMsgEditText().trim();
        if (!TextUtils.equals(mTalk.mDraftBoxMsg, newDraft)) {
            RecentTalkManager.setDraftAsync(mTalk.mTalkOtherUserId, mTalk.mTalkOtherUserSource, mTalk.mTalkType, sendMsgLayout.getMsgEditText().trim(), null);
        }
    }

    private void updateLastMessage(Message message) {
        if (message != null) {
            if (latestMessage == null) {
                latestMessage = message;
            } else if (message.mMsgId > latestMessage.mMsgId) {
                latestMessage = message;
            }
        }
    }

    private void updateBottomMessage(List<Message> messageList) {
        if (messageList != null && !messageList.isEmpty()) {
            if (bottomMessage == null || messageList.get(0).mMsgId > bottomMessage.mMsgId) {
                bottomMessage = messageList.get(0);
            }
            for (Message message : messageList) {
                if (message.getSendStatus() == GmacsConstant.MSG_SENT) {
                    if (bottomSuccessfulMessage == null || message.mMsgId > bottomSuccessfulMessage.mMsgId) {
                        bottomSuccessfulMessage = message;
                        break;
                    }
                }
            }
        }
    }

    private void updateBottomMessage(Message message) {
        if (message != null) {
            if (bottomMessage == null || message.mMsgId > bottomMessage.mMsgId) {
                bottomMessage = message;
            }
            if (message.getSendStatus() == GmacsConstant.MSG_SENT) {
                if (bottomSuccessfulMessage == null || message.mMsgId > bottomSuccessfulMessage.mMsgId) {
                    bottomSuccessfulMessage = message;
                }
            }
        }
    }

    public void updateSendStatusAndCardContentForSpecificMessage(Message target) {
        if (chatAdapter != null) {
            int firstVisiblePosition = chatListView.getFirstVisiblePosition()
                    - chatListView.getHeaderViewsCount();
            int lastVisiblePosition = chatListView.getLastVisiblePosition()
                    - chatListView.getHeaderViewsCount();
            int count = chatAdapter.getCount();
            Message message;
            for (int i = count - 1; i >= 0; i--) {
                message = chatAdapter.getItem(i);
                if (message != null && target.mLocalId == message.mLocalId) {
                    if (i >= firstVisiblePosition && i <= lastVisiblePosition) {
                        chatAdapter.updateSendStatusAndCardContentForSpecificItem(i, chatListView.getChildAt(i - firstVisiblePosition), target);
                    } else if (target != message) {
                        chatAdapter.replaceSpecificPositionWithMessage(i, target);
                    }
                    break;
                }
            }
        }
    }

    public IMMessage getNextIMMessage(IMMessage imMessage) {
        if (!isFinishing() && chatAdapter != null) {
            return chatAdapter.getNextIMMessage(imMessage);
        } else {
            return null;
        }
    }

    public List<IMMessage> getAllIMMessageForSpecificType(Class type) {
        if (chatAdapter != null) {
            return chatAdapter.getAllIMMessageForSpecificType(type);
        } else {
            return new ArrayList<>();
        }
    }


    /**
     * MessageManager.getInstance().getHistoryAsync
     * 在请求范围内有发送失败的消息时，返回的消息至少会有一条成功的消息，即使返回的数量超过了请求的数量。
     * 利用这个机制更新bottomMessage和bottomSuccessfulMessage。
     */
    public void deleteMessageByLocalId(final long localId) {
        if (chatAdapter != null) {
            final Message message = chatAdapter.deleteMessageByLocalId(localId);
            if (message != null && !isFinishing() && mTalk != null) {
                updateBottomMessage(localId);
            }
        }
    }

    private void updateBottomMessage(final long localId) {
        //删除最后一条消息
        if (bottomMessage.mLocalId == localId) {
            //删除的消息不是发送成功的，即服务端没有入库，没有生成全局的MsgId
            if (bottomMessage.getSendStatus() != GmacsConstant.MSG_SENT) {
                //MsgId最大的发送失败的消息temp与MsgId最大的发送成功定的消息相比较，获取真正的bottomMessage
                Message temp = chatAdapter.getBottomAndUnSentMessage();
                if (temp != null) {
                    if (bottomSuccessfulMessage != null && bottomSuccessfulMessage.mMsgId > temp.mMsgId) {
                        bottomMessage = bottomSuccessfulMessage;
                    } else {
                        bottomMessage = temp;
                    }
                } else {
                    bottomMessage = bottomSuccessfulMessage;
                }
                if (latestMessage.mLocalId == localId) {
                    latestMessage = bottomMessage;
                }
            } else {
                //删除了一条发送成功的消息，以该消息的localId为基准拉取一条消息。更新bottomMessage和bottomSuccessfulMessage
                MessageManager.getInstance().getHistoryAsync(mTalk.mTalkOtherUserId, mTalk.mTalkOtherUserSource, localId, 1, new MessageManager.GetHistoryMsgCb() {
                    @Override
                    public void done(int errorCode, String errorMessage, List<Message> msgList) {
                        if (errorCode == 0 && !msgList.isEmpty()) {
                            bottomMessage = msgList.get(0);
                            if (latestMessage.mLocalId == localId) {
                                latestMessage = bottomMessage;
                            }
                            Message message1 = msgList.get(msgList.size() - 1);
                            if (message1.getSendStatus() == GmacsConstant.MSG_SENT) {
                                bottomSuccessfulMessage = message1;
                            }
                        }
                    }
                });
            }
        } else if (bottomSuccessfulMessage != null && bottomSuccessfulMessage.mLocalId == localId) {
            //删除了MsgId最大的那条发送成功的消息，需要更新bottomSuccessfulMessage
            MessageManager.getInstance().getHistoryAsync(mTalk.mTalkOtherUserId, mTalk.mTalkOtherUserSource, localId, 1, new MessageManager.GetHistoryMsgCb() {
                @Override
                public void done(int errorCode, String errorMessage, List<Message> msgList) {
                    if (errorCode == 0 && !msgList.isEmpty()) {
                        Message message1 = msgList.get(msgList.size() - 1);
                        if (message1.getSendStatus() == GmacsConstant.MSG_SENT) {
                            bottomSuccessfulMessage = message1;
                        }
                    }
                }
            });
        }
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        picturePath = savedInstanceState.getString("picture");
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (!TextUtils.isEmpty(picturePath)) {
            outState.putString("picture", picturePath);
        }
    }

    private interface OnGetFocusMessagesCb {
        void onGetFocusMessages(int focusIndex, List<Message> messages);
    }

    public static class InsertLocalMessageCb implements MessageManager.InsertLocalMessageCb {
        WeakReference<GmacsChatActivity> chatActivityWeakReference;

        InsertLocalMessageCb(GmacsChatActivity chatActivity) {
            this.chatActivityWeakReference = new WeakReference<>(chatActivity);
        }

        @Override
        public void onInsertLocalMessage(int errorCode, String errorMessage, final Message message) {
            final GmacsChatActivity chatActivity = chatActivityWeakReference.get();
            if (chatActivity != null && !chatActivity.isFinishing()) {
                chatActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        chatActivity.receivedNewMsg(message);
                    }
                });
            }
        }
    }

    private static class LoadHistoryMessageCallBack implements MessageManager.GetHistoryMsgCb {

        WeakReference<GmacsChatActivity> activityWeakReference;
        long beginMsgLocalId;

        LoadHistoryMessageCallBack(GmacsChatActivity activity, long beginMsgLocalId) {
            activityWeakReference = new WeakReference<>(activity);
            this.beginMsgLocalId = beginMsgLocalId;
        }

        @Override
        public void done(final int errorCode, final String errorMessage, final List<Message> msgList) {
            final GmacsChatActivity activity = activityWeakReference.get();
            if (activity != null) {
                activity.showHistory(errorMessage, beginMsgLocalId, msgList);
            }
        }
    }

    private static class LoadHistoryAfterCallBack implements MessageManager.GetHistoryMsgCb {

        WeakReference<GmacsChatActivity> activityWeakReference;
        long bottomMessageLocalId;

        LoadHistoryAfterCallBack(GmacsChatActivity activity, long bottomMessageLocalId) {
            activityWeakReference = new WeakReference<>(activity);
            this.bottomMessageLocalId = bottomMessageLocalId;
        }

        @Override
        public void done(final int errorCode, final String errorMessage, final List<Message> msgList) {
            final GmacsChatActivity activity = activityWeakReference.get();
            if (activity != null) {
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        activity.showHistoryAfter(errorMessage, bottomMessageLocalId, msgList);
                    }
                });
            }
        }
    }

    private static class GetTalkCallBack implements RecentTalkManager.GetTalkByIdCb {
        private WeakReference<GmacsChatActivity> activityWeakReference;

        GetTalkCallBack(GmacsChatActivity activity) {
            activityWeakReference = new WeakReference<>(activity);
        }

        @Override
        public void done(final int errorCode, String errorMessage, final Talk talk) {
            final GmacsChatActivity chatActivity = activityWeakReference.get();
            if (chatActivity != null && !chatActivity.isFinishing()) {
                if (talk != null) {
                    chatActivity.onGetTalk(talk);
                } else if (chatActivity.mTalk != null) {
                    ContactLogic.getInstance().getUserInfo(chatActivity.mTalk.mTalkOtherUserId, chatActivity.mTalk.mTalkOtherUserSource);
                    chatActivity.updateTotalUnreadCount(TalkLogic.getInstance().getUnreadMsgCount());
                }
            }
        }
    }

    private class TitleBar {

        ImageView backBtn, titleIcon, talkDetailEntry;
        TextView backText, titleText;

        public TitleBar() {
            (backBtn = (ImageView) mTitleBarDelegate.findViewById(R.id.back_btn)).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onBackPressed();
                    GmacsUtils.hideSoftInputMethod(v.getWindowToken());
                }
            });
            (backText = (TextView) mTitleBarDelegate.findViewById(R.id.back_text)).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onBackPressed();
                    GmacsUtils.hideSoftInputMethod(v.getWindowToken());
                }
            });
            ((RelativeLayout.LayoutParams) backText.getLayoutParams()).leftMargin = GmacsUtils.dipToPixel(-10);
            titleText = (TextView) mTitleBarDelegate.findViewById(R.id.title_text);
            titleIcon = (ImageView) mTitleBarDelegate.findViewById(R.id.title_icon);
            talkDetailEntry = (ImageView) mTitleBarDelegate.findViewById(R.id.talk_detail_entry);
        }

        public void setTitle(CharSequence title) {
            titleText.setText(title);
            if (mTalk != null && mTalk.mTalkOtherUserInfo instanceof Group && ((Group) mTalk.mTalkOtherUserInfo).isSilent()) {
                titleIcon.setImageResource(R.drawable.gmacs_ic_silent_title);
            } else {
                titleIcon.setImageResource(0);
            }
        }

    }

    private class DrawableSpan extends ReplacementSpan {

        Drawable drawable;

        DrawableSpan(Drawable drawable) {
            this.drawable = drawable;
        }

        @Override
        public int getSize(@NonNull Paint paint, CharSequence text,
                           int start, int end,
                           Paint.FontMetricsInt fm) {
            Rect rect = drawable.getBounds();

            if (fm != null) {
                fm.ascent = -rect.bottom;
                fm.descent = 0;

                fm.top = fm.ascent;
                fm.bottom = 0;
            }

            return rect.right;
        }

        @Override
        public void draw(@NonNull Canvas canvas, CharSequence text,
                         int start, int end, float x,
                         int top, int y, int bottom, @NonNull Paint paint) {
            canvas.save();
            int transY = bottom - drawable.getBounds().bottom;
            canvas.translate(x, transY / 2);
            drawable.draw(canvas);
            canvas.restore();
        }
    }

}