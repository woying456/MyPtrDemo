package com.android.gmacs.adapter;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.android.gmacs.R;
import com.android.gmacs.activity.GmacsChatActivity;
import com.android.gmacs.logic.ContactLogic;
import com.android.gmacs.logic.MessageLogic;
import com.android.gmacs.msg.IMViewFactory;
import com.android.gmacs.msg.view.IMMessageView;
import com.android.gmacs.utils.GmacsUiUtil;
import com.android.gmacs.view.GmacsDialog;
import com.android.gmacs.view.NetworkImageView;
import com.common.gmacs.core.ClientManager;
import com.common.gmacs.core.ContactsManager;
import com.common.gmacs.core.Gmacs;
import com.common.gmacs.core.GmacsConstant;
import com.common.gmacs.msg.IMMessage;
import com.common.gmacs.msg.MsgContentType;
import com.common.gmacs.msg.data.IMGroupNotificationMsg;
import com.common.gmacs.parse.Pair;
import com.common.gmacs.parse.contact.Contact;
import com.common.gmacs.parse.contact.Group;
import com.common.gmacs.parse.contact.GroupMember;
import com.common.gmacs.parse.contact.UserInfo;
import com.common.gmacs.parse.message.GmacsUserInfo;
import com.common.gmacs.parse.message.Message;
import com.common.gmacs.parse.talk.Talk;
import com.common.gmacs.parse.talk.TalkType;
import com.common.gmacs.utils.GmacsEnvi;
import com.common.gmacs.utils.ImageUtil;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.TimeZone;

import static com.android.gmacs.view.NetworkImageView.IMG_RESIZE;

public class GmacsChatAdapter extends BaseAdapter implements View.OnClickListener, View.OnLongClickListener {

    // 5分钟显示一下时间
    private static final int FIVE_MINUTE = 60 * 5 * 1000;
    private static final int ONE_MINUTE = 60 * 1000;
    public static int BASE_NUM = 0;
    public static final int ITEM_TYPE_EMPTY = BASE_NUM++;
    public static final int ITEM_TYPE_TIP = BASE_NUM++;
    public static final int ITEM_TYPE_GROUP_NOTIFICATION = BASE_NUM++;
    public static final int ITEM_TYPE_RIGHT_TEXT = BASE_NUM++;
    public static final int ITEM_TYPE_RIGHT_AUDIO = BASE_NUM++;
    public static final int ITEM_TYPE_RIGHT_PIC = BASE_NUM++;
    public static final int ITEM_TYPE_RIGHT_CALL = BASE_NUM++;
    public static final int ITEM_TYPE_RIGHT_UNIVERSAL_CARD1 = BASE_NUM++;
    public static final int ITEM_TYPE_RIGHT_UNIVERSAL_CARD2 = BASE_NUM++;
    public static final int ITEM_TYPE_RIGHT_UNIVERSAL_CARD3 = BASE_NUM++;
    public static final int ITEM_TYPE_RIGHT_UNIVERSAL_CARD4 = BASE_NUM++;
    public static final int ITEM_TYPE_RIGHT_MAP = BASE_NUM++;

    public static final int ITEM_TYPE_LEFT_TEXT = BASE_NUM++;
    public static final int ITEM_TYPE_LEFT_AUDIO = BASE_NUM++;
    public static final int ITEM_TYPE_LEFT_PIC = BASE_NUM++;
    public static final int ITEM_TYPE_LEFT_CALL = BASE_NUM++;
    public static final int ITEM_TYPE_LEFT_UNIVERSAL_CARD1 = BASE_NUM++;
    public static final int ITEM_TYPE_LEFT_UNIVERSAL_CARD2 = BASE_NUM++;
    public static final int ITEM_TYPE_LEFT_UNIVERSAL_CARD3 = BASE_NUM++;
    public static final int ITEM_TYPE_LEFT_UNIVERSAL_CARD4 = BASE_NUM++;
    public static final int ITEM_TYPE_LEFT_MAP = BASE_NUM++;
    public HashMap<String, GroupMember> groupMemberInfoCache;
    // 所有消息
    protected ArrayList<Message> mAllMessage = new ArrayList<>();
    private IMViewFactory mIMViewFactory = new IMViewFactory();
    private GmacsChatActivity mActivity;
    protected LayoutInflater mInflater;
    private Talk mTalk;
    private SimpleDateFormat mSimpleDateFormat = (SimpleDateFormat) SimpleDateFormat.getDateTimeInstance();
    private long lastTime;
    private View.OnClickListener leftAvatarClickListener;
    private View.OnClickListener rightAvatarClickListener;
    private View.OnLongClickListener leftAvatarLongClickListener;

    public GmacsChatAdapter(Context context, Talk talk, HashMap<String, GroupMember> groupMemberInfoCache) {
        this.mActivity = (GmacsChatActivity) context;
        this.mTalk = talk;
        this.groupMemberInfoCache = groupMemberInfoCache;
        mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    public void setTalk(Talk mTalk) {
        this.mTalk = mTalk;
    }

    public void clearData() {
        mAllMessage.clear();
        lastTime = 0;
        notifyDataSetChanged();
    }

    public void setIMViewFactory(IMViewFactory mIMViewFactory) {
        this.mIMViewFactory = mIMViewFactory;
    }

    /**
     * 把消息添加到结束位置
     *
     * @param msg
     */
    public void addMsgToEndPosition(Message msg, FillGroupMemberInfoCb cb) {
        if (msg != null) {
            ArrayList<Message> messages = new ArrayList<>(1);
            messages.add(msg);
            addMsgsToEndPosition(messages, cb);
        }
    }

    /**
     * 把消息们添加到起始位置
     *
     * @param msgs
     */
    public void addMsgsToStartPosition(final List<Message> msgs, final FillGroupMemberInfoCb cb) {
        if (msgs != null && msgs.size() > 0) {
            fillGroupMemberInfo(msgs, new FillGroupMemberInfoCb() {
                @Override
                public void done() {
                    for (Message message : msgs) {
                        mAllMessage.add(0, message);
                    }
                    notifyDataSetChanged();
                    if (cb != null) {
                        cb.done();
                    }
                }
            });
        }
    }

    /**
     * 把消息添加到结束位置
     *
     * @param msgs
     */
    public void addMsgsToEndPosition(final List<Message> msgs, final FillGroupMemberInfoCb cb) {
        if (msgs != null && msgs.size() > 0) {
            fillGroupMemberInfo(msgs, new FillGroupMemberInfoCb() {
                @Override
                public void done() {
                    int length = mAllMessage.size();
                    for (Message message : msgs) {
                        mAllMessage.add(length, message);
                    }
                    notifyDataSetChanged();
                    if (cb != null) {
                        cb.done();
                    }
                }
            });
        }
    }

    public List<Message> clearDirtyMsgs(int start) {
        List<Message> dirtyMsgs = null;
        if (start >= 0 && start < getCount()) {
            for (int i = getCount() - 1; i >= start; --i) {
                if (dirtyMsgs == null) {
                    dirtyMsgs = new ArrayList<>(getCount() - start);
                }
                dirtyMsgs.add(mAllMessage.remove(i));
            }
            notifyDataSetChanged();
        }
        return dirtyMsgs;
    }

    private void fillGroupMemberInfo(List<Message> msgList, final FillGroupMemberInfoCb cb) {
        HashSet<Pair> sparseArray = null;
        if (TalkType.isGroupTalk(mTalk)) {
            for (Message msg : msgList) {
                if (msg.atInfoArray != null) {
                    for (Message.AtInfo atInfo : msg.atInfoArray) {
                        if (atInfo.userSource < 10000) {
                            GroupMember groupMember = addToGroupMemberInfoCache(atInfo.userId, atInfo.userSource);
                            if (groupMember == null || groupMember.getName() == null) {
                                if (sparseArray == null) {
                                    sparseArray = new HashSet<>();
                                }
                                sparseArray.add(new Pair(atInfo.userId, atInfo.userSource));
                            }
                        }
                    }
                }
                if (msg.getMsgContent() instanceof IMGroupNotificationMsg) {
                    IMGroupNotificationMsg temp = ((IMGroupNotificationMsg) msg.getMsgContent());
                    if (IMGroupNotificationMsg.TRANFER_GROUP_OWNER.equals(temp.operationType)) {
                        for (int i = 0; i < temp.targets.size(); i++) {
                            if (temp.targets.get(i) == null) {
                                continue;
                            }
                            GroupMember groupMember = addToGroupMemberInfoCache(temp.targets.get(i)[0], Integer.parseInt(temp.targets.get(i)[1]));
                            if (groupMember == null || groupMember.getName() == null) {
                                if (sparseArray == null) {
                                    sparseArray = new HashSet<>();
                                }
                                sparseArray.add(new Pair(temp.targets.get(i)[0], Integer.parseInt(temp.targets.get(i)[1])));
                            }
                        }
                    }
                }
                if (!msg.mIsSelfSendMsg) {
                    GroupMember groupMember = addToGroupMemberInfoCache(msg.mSenderInfo.mUserId, msg.mSenderInfo.mUserSource);
                    if (groupMember == null || groupMember.getName() == null) {
                        if (sparseArray == null) {
                            sparseArray = new HashSet<>();
                        }
                        sparseArray.add(new Pair(msg.mSenderInfo.mUserId, msg.mSenderInfo.mUserSource));
                    }
                }
            }
            if (sparseArray != null) {
                ContactsManager.getInstance().getLocalUserInfoBatchAsync(sparseArray, new ContactsManager.UserInfoBatchCb() {
                    @Override
                    public void onGetUserInfoBatch(int errorCode, String errorMessage, final List<UserInfo> userInfoList) {
                        mActivity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (userInfoList != null) {
                                    for (UserInfo info : userInfoList) {
                                        updateGroupMemberInfoCache(info);
                                    }

                                }
                                cb.done();
                            }
                        });
                    }
                });
            }
        }
        if (sparseArray == null) {
            mActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    cb.done();
                }
            });
        }
    }

    private GroupMember addToGroupMemberInfoCache(String id, int source) {
        String talkId = Talk.getTalkId(source, id);
        GroupMember groupMember = null;
        if (!groupMemberInfoCache.containsKey(talkId)) {
            if (mTalk.mTalkOtherUserInfo instanceof Group) {
                Group group = (Group) mTalk.mTalkOtherUserInfo;
                for (GroupMember member : group.getMembers()) {
                    if (TextUtils.equals(id, member.getId())
                            && source == member.getSource()) {
                        groupMemberInfoCache.put(talkId, member);
                        return member;
                    }
                }
            }
            groupMember = new GroupMember(id, source, GroupMember.AUTHORITY_STRANGER);
            groupMemberInfoCache.put(talkId, groupMember);
        }
        return groupMember;
    }

    @Override
    public int getCount() {
        if (mAllMessage != null) {
            return mAllMessage.size();
        }
        return 0;
    }

    @Override
    public Message getItem(int position) {
        Message msg = null;
        if (position >= 0 && position <= mAllMessage.size() - 1) {
            msg = mAllMessage.get(position);
        }
        return msg;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getViewTypeCount() {
        return BASE_NUM;
    }

    @Override
    public int getItemViewType(int position) {
        int type = ITEM_TYPE_EMPTY;
        Message message = mAllMessage.get(position);
        String showType = message.getMsgContent().showType;
        if (MsgContentType.TYPE_TIP.equals(showType)) {
            type = ITEM_TYPE_TIP;
        } else if (MsgContentType.TYPE_GROUP_NOTIFICATION.equals(showType)) {
            type = ITEM_TYPE_GROUP_NOTIFICATION;
        } else if (message.mIsSelfSendMsg) {
            switch (showType) {
                case MsgContentType.TYPE_TEXT:
                    type = ITEM_TYPE_RIGHT_TEXT;
                    break;
                case MsgContentType.TYPE_AUDIO:
                    type = ITEM_TYPE_RIGHT_AUDIO;
                    break;
                case MsgContentType.TYPE_IMAGE:
                    type = ITEM_TYPE_RIGHT_PIC;
                    break;
                case MsgContentType.TYPE_LOCATION:
                    type = ITEM_TYPE_RIGHT_MAP;
                    break;
                case MsgContentType.TYPE_CALL:
                    type = ITEM_TYPE_RIGHT_CALL;
                    break;
                case MsgContentType.TYPE_UNIVERSAL_CARD1:
                    type = ITEM_TYPE_RIGHT_UNIVERSAL_CARD1;
                    break;
                case MsgContentType.TYPE_UNIVERSAL_CARD2:
                    type = ITEM_TYPE_RIGHT_UNIVERSAL_CARD2;
                    break;
                case MsgContentType.TYPE_UNIVERSAL_CARD3:
                    type = ITEM_TYPE_RIGHT_UNIVERSAL_CARD3;
                    break;
                case MsgContentType.TYPE_UNIVERSAL_CARD4:
                    type = ITEM_TYPE_RIGHT_UNIVERSAL_CARD4;
                    break;
            }
        } else {
            switch (showType) {
                case MsgContentType.TYPE_TEXT:
                    type = ITEM_TYPE_LEFT_TEXT;
                    break;
                case MsgContentType.TYPE_AUDIO:
                    type = ITEM_TYPE_LEFT_AUDIO;
                    break;
                case MsgContentType.TYPE_IMAGE:
                    type = ITEM_TYPE_LEFT_PIC;
                    break;
                case MsgContentType.TYPE_LOCATION:
                    type = ITEM_TYPE_LEFT_MAP;
                    break;
                case MsgContentType.TYPE_CALL:
                    type = ITEM_TYPE_LEFT_CALL;
                    break;
                case MsgContentType.TYPE_UNIVERSAL_CARD1:
                    type = ITEM_TYPE_LEFT_UNIVERSAL_CARD1;
                    break;
                case MsgContentType.TYPE_UNIVERSAL_CARD2:
                    type = ITEM_TYPE_LEFT_UNIVERSAL_CARD2;
                    break;
                case MsgContentType.TYPE_UNIVERSAL_CARD3:
                    type = ITEM_TYPE_LEFT_UNIVERSAL_CARD3;
                    break;
                case MsgContentType.TYPE_UNIVERSAL_CARD4:
                    type = ITEM_TYPE_LEFT_UNIVERSAL_CARD4;
                    break;
            }
        }
        return type;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        IMMessage imMessage = mAllMessage.get(position).getMsgContent();
        MsgViewHolder viewHolder;
        IMMessageView messageController;
        int viewType = getItemViewType(position);
        if (convertView == null) {
            int[] maxWidthForChild = new int[1];
            if (viewType == ITEM_TYPE_EMPTY) {
                convertView = new TextView(mActivity);
                convertView.setVisibility(View.GONE);
                return convertView;
            } else {
                convertView = getConvertParentView(viewType, parent, maxWidthForChild);
            }

            viewHolder = new MsgViewHolder();
            initConvertView(convertView, viewHolder);

            //根据类型判断加载哪种消息视图
            messageController = mIMViewFactory.createItemView(imMessage);
            View msgCardView = messageController.createIMView(viewHolder.contentItem, mInflater, mActivity, maxWidthForChild[0]);
            replaceAloneSendProgressBar(viewHolder, msgCardView);
            viewHolder.contentItem.setTag(messageController);
        } else {
            if (viewType == ITEM_TYPE_EMPTY) {
                return convertView;
            }
            viewHolder = (MsgViewHolder) convertView.getTag();
            messageController = (IMMessageView) viewHolder.contentItem.getTag();
        }
        messageController.setDataForView(imMessage);
        updateUIBySendStatus(imMessage, viewHolder);
        updateUIBySenderNameAndAvatar(viewHolder, position, viewType);
        updateUIByMessageTime(imMessage, viewHolder, position);

        return convertView;
    }

    private void updateUIByMessageTime(IMMessage imMessage, MsgViewHolder viewHolder, int position) {
        if (!TalkType.isSystemTalk(mTalk)) {
            Message preMessage = null;
            if (position - 1 >= 0) {
                preMessage = mAllMessage.get(position - 1);
            }
            String time = messageTimeFormat(imMessage, preMessage);
            if (TextUtils.isEmpty(time)) {
                viewHolder.time.setVisibility(View.GONE);
            } else {
                viewHolder.time.setText(time);
                viewHolder.time.setVisibility(View.VISIBLE);
            }
        } else {
            viewHolder.time.setVisibility(View.GONE);
        }
    }

    public void updateLeftUser(View convertView, int position) {
        if (convertView.getTag() instanceof MsgViewHolder) {
            MsgViewHolder holder = (MsgViewHolder) convertView.getTag();
            if (holder.leftHead != null && holder.leftName != null) {
                initLeftUser(holder, position);
            }
        }
    }

    public void updateRightUser(View convertView) {
        if (convertView.getTag() instanceof MsgViewHolder) {
            MsgViewHolder holder = (MsgViewHolder) convertView.getTag();
            if (holder.rightHead != null) {
                initRightUser(holder);
            }
        }
    }

    protected String messageTimeFormat(long messageTime) {
        Calendar calendar = Calendar.getInstance();
        int currentYear = calendar.get(Calendar.YEAR);
        int currentMonth = calendar.get(Calendar.MONTH);
        int currentDate = calendar.get(Calendar.DAY_OF_YEAR);
        int currentWeek = calendar.get(Calendar.WEEK_OF_MONTH) - 1;

        calendar.setTimeInMillis(messageTime);
        calendar.setTimeZone(TimeZone.getTimeZone("GMT+8"));

        int messageYear = calendar.get(Calendar.YEAR);
        int messageMonth = calendar.get(Calendar.MONTH);
        int messageDate = calendar.get(Calendar.DAY_OF_YEAR);
        int messageWeek = calendar.get(Calendar.WEEK_OF_MONTH) - 1;

        String formattedTime;
        if (currentYear == messageYear) {
            int delta = currentDate - messageDate;
            if (delta == 0) {
                mSimpleDateFormat.applyPattern("HH:mm");
                formattedTime = mSimpleDateFormat.format(calendar.getTime());
            } else {
                if (delta == 1) {
                    mSimpleDateFormat.applyPattern("HH:mm");
                    String accurateTime = mSimpleDateFormat.format(calendar.getTime());
                    formattedTime = "昨天" + accurateTime;
                } else if ((currentWeek == messageWeek || currentMonth - messageMonth == 1) && delta < 7) {
                    String[] weekOfDays = {"星期日", "星期一", "星期二", "星期三", "星期四", "星期五", "星期六"};
                    int week = calendar.get(Calendar.DAY_OF_WEEK) - 1;
                    mSimpleDateFormat.applyPattern("HH:mm");
                    formattedTime = weekOfDays[week >= 0 ? week : 0] + mSimpleDateFormat.format(calendar.getTime());
                } else {
                    mSimpleDateFormat.applyPattern("MM-dd HH:mm");
                    formattedTime = mSimpleDateFormat.format(calendar.getTime());
                }
            }
        } else {
            mSimpleDateFormat.applyPattern("yyyy-MM-dd HH:mm");
            formattedTime = mSimpleDateFormat.format(calendar.getTime());
        }

        return formattedTime;
    }

    private void initConvertView(View convertView, MsgViewHolder viewHolder) {
        viewHolder.time = (TextView) convertView.findViewById(R.id.time);
        viewHolder.contentItem = (ViewGroup) convertView.findViewById(R.id.content_item);
        viewHolder.leftHead = (NetworkImageView) convertView.findViewById(R.id.left_head);
        viewHolder.rightHead = (NetworkImageView) convertView.findViewById(R.id.right_head);
        viewHolder.leftName = (TextView) convertView.findViewById(R.id.left_name);
        viewHolder.sending = (ProgressBar) convertView.findViewById(R.id.send_progress);
        viewHolder.sendFailed = (ImageView) convertView.findViewById(R.id.right_failed_sendf);
        convertView.setTag(viewHolder);
    }

    protected View getConvertParentView(int viewType, ViewGroup parentView, int[] maxWidthForChild) {
        View convertView = null;
        if (viewType < ITEM_TYPE_RIGHT_TEXT) {
            convertView = mInflater.inflate(R.layout.gmacs_adapter_msg_container_notice, parentView, false);
            maxWidthForChild[0] = GmacsEnvi.screenWidth
                    - parentView.getResources().getDimensionPixelOffset(R.dimen.im_chat_notice_margin_right)
                    - parentView.getResources().getDimensionPixelOffset(R.dimen.im_chat_notice_margin_left);
        } else if (viewType < ITEM_TYPE_LEFT_TEXT) {
            convertView = mInflater.inflate(R.layout.gmacs_adapter_msg_container_right, parentView, false);
            maxWidthForChild[0] = GmacsEnvi.screenWidth
                    - (parentView.getResources().getDimensionPixelOffset(R.dimen.avatar_chat) + parentView.getResources().getDimensionPixelOffset(R.dimen.im_chat_content_item_margin_right)) * 2
                    - parentView.getResources().getDimensionPixelOffset(R.dimen.im_chat_send_failed_margin_right)
                    - parentView.getResources().getDimensionPixelOffset(R.dimen.im_chat_sending_icon_size);
        } else if (viewType <= ITEM_TYPE_LEFT_MAP) {
            convertView = mInflater.inflate(R.layout.gmacs_adapter_msg_container_left, parentView, false);
            maxWidthForChild[0] = GmacsEnvi.screenWidth
                    - (parentView.getResources().getDimensionPixelOffset(R.dimen.avatar_chat) + parentView.getResources().getDimensionPixelOffset(R.dimen.im_chat_content_item_margin_right)) * 2
                    - parentView.getResources().getDimensionPixelOffset(R.dimen.im_chat_send_failed_margin_right)
                    - parentView.getResources().getDimensionPixelOffset(R.dimen.im_chat_sending_icon_size);
        }
        return convertView;
    }

    protected boolean updateUIBySenderNameAndAvatar(MsgViewHolder viewHolder, int position, int viewType) {
        boolean notFindViewType = false;
        if (viewType < ITEM_TYPE_RIGHT_TEXT) {
        } else if (viewType < ITEM_TYPE_LEFT_TEXT) {
            initRightUser(viewHolder);
        } else if (viewType <= ITEM_TYPE_LEFT_MAP) {
            initLeftUser(viewHolder, position);
        } else {
            notFindViewType = true;
        }
        return notFindViewType;
    }

    private String messageTimeFormat(IMMessage imMessage, Message preMsg) {
        Message msg = imMessage.message;
        if (null == imMessage.chatTimeStyle) {
            // 发送失败的消息不显示时间
            if (msg.getSendStatus() == GmacsConstant.MSG_SEND_FAILED) {
                imMessage.chatTimeStyle = "";
            } else if (preMsg != null) {
                // 前一条消息发送失败，本条消息不显示时间
                if (preMsg.getSendStatus() == GmacsConstant.MSG_SEND_FAILED) {
                    imMessage.chatTimeStyle = "";
                } else {
                    // 比较本条消息与上一条消息的时间，若超过5分钟则显示时间
                    long preTime = preMsg.mMsgUpdateTime;
                    long time = msg.mMsgUpdateTime;
                    if (Math.abs(time - preTime) > FIVE_MINUTE) {
                        if (Math.abs(time - lastTime) <= ONE_MINUTE) {
                            lastTime = lastTime - 1000 * 60;
                        } else {
                            lastTime = msg.mMsgUpdateTime;
                        }
                        imMessage.chatTimeStyle = messageTimeFormat(lastTime);
                    }
                }
            } else {
                // 第一条成功的消息必显示时间
                long time = msg.mMsgUpdateTime;
                if (Math.abs(time - lastTime) <= ONE_MINUTE) {
                    lastTime = lastTime - 1000 * 60;
                } else {
                    lastTime = msg.mMsgUpdateTime;
                }
                imMessage.chatTimeStyle = messageTimeFormat(lastTime);
            }
        }
        return imMessage.chatTimeStyle;
    }

    public Message deleteMessageByLocalId(long localId) {
        if (localId > 0) {
            for (int i = mAllMessage.size() - 1; i >= 0; i--) {
                Message message = mAllMessage.get(i);
                if (message.mLocalId == localId) {
                    message.isDeleted = true;
                    mAllMessage.remove(message);
                    notifyDataSetChanged();
                    Message.MessageUserInfo otherInfo = message.getTalkOtherUserInfo();
                    if (otherInfo != null) {
                        MessageLogic.getInstance().deleteMsgByLocalId(otherInfo.mUserId, otherInfo.mUserSource, message.mLocalId);
                    }
                    return message;
                }
            }
        }
        return null;
    }

    public void removeMessageFromAdapter(@NonNull List<Long> localIdList) {
        for (int j = 0; j < localIdList.size(); ++j) {
            for (int i = mAllMessage.size() - 1; i >= 0; i--) {
                if (mAllMessage.get(i).mLocalId == localIdList.get(j)) {
                    mAllMessage.get(i).isDeleted = true;
                    mAllMessage.remove(i);
                    break;
                }
            }
        }
        notifyDataSetChanged();
    }

    public Message getBottomAndUnSentMessage() {
        if (!mAllMessage.isEmpty() && mAllMessage.get(mAllMessage.size() - 1).getSendStatus() != GmacsConstant.MSG_SENT) {
            return mAllMessage.get(mAllMessage.size() - 1);
        } else {
            return null;
        }
    }

    protected int defaultLeftAvatarRes() {
        return R.drawable.gmacs_ic_default_avatar;
    }

    protected int defaultRightAvatarRes() {
        return R.drawable.gmacs_ic_default_avatar;
    }

    public void initLeftUser(MsgViewHolder viewHolder, int position) {
        Message.MessageUserInfo info = getSenderInfoForReceiveMsg(position);
        String avatar = "";
        String name = "";
        if (TalkType.isGroupTalk(mTalk)) {
            String talkId = Talk.getTalkId(info.mUserSource, info.mUserId);
            GroupMember groupMember = groupMemberInfoCache.get(talkId);
            if (groupMember != null) {
                avatar = groupMember.getAvatar();
                name = groupMember.getNameToShow();
                if (TextUtils.isEmpty(groupMember.getName())) {
                    ContactLogic.getInstance().getUserInfo(groupMember.getId(), groupMember.getSource());
                }
            }
            if (TextUtils.isEmpty(name)) {
                viewHolder.leftName.setText(GmacsEnvi.appContext.getResources().getString(R.string.default_user_name));
            } else {
                viewHolder.leftName.setText(name);
            }
            viewHolder.leftName.setVisibility(View.VISIBLE);
        } else {
            if (mTalk.mTalkOtherUserInfo != null) {
                avatar = mTalk.mTalkOtherUserInfo.avatar;
            }
            viewHolder.leftName.setVisibility(View.GONE);
        }
        viewHolder.leftHead.setTag(info);
        viewHolder.leftHead
                .setDefaultImageResId(defaultLeftAvatarRes())
                .setErrorImageResId(defaultLeftAvatarRes())
                .setImageUrl(ImageUtil.makeUpUrl(avatar, IMG_RESIZE, IMG_RESIZE));
        if (leftAvatarClickListener != null) {
            viewHolder.leftHead.setOnClickListener(leftAvatarClickListener);
        } else {
            viewHolder.leftHead.setOnClickListener(this);
        }
        if (leftAvatarLongClickListener != null) {
            viewHolder.leftHead.setOnLongClickListener(leftAvatarLongClickListener);
        } else {
            viewHolder.leftHead.setOnLongClickListener(this);
        }
        viewHolder.leftHead.setHapticFeedbackEnabled(false);
    }

    public void initRightUser(MsgViewHolder viewHolder) {
        if (ClientManager.getInstance().getGmacsUserInfo() != null) {
            viewHolder.rightHead
                    .setDefaultImageResId(R.drawable.gmacs_ic_default_avatar)
                    .setErrorImageResId(R.drawable.gmacs_ic_default_avatar)
                    .setImageUrl(ImageUtil.makeUpUrl(ClientManager.getInstance().getGmacsUserInfo().avatar, IMG_RESIZE, IMG_RESIZE));
        }
        if (rightAvatarClickListener != null) {
            viewHolder.rightHead.setOnClickListener(rightAvatarClickListener);
        } else {
            viewHolder.rightHead.setOnClickListener(this);
        }
    }

    protected Message.MessageUserInfo getSenderInfoForReceiveMsg(int position) {
        return mAllMessage.get(position).mSenderInfo;
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.right_head) {
            try {
                Intent intent = new Intent(mActivity, Class.forName(GmacsUiUtil.getContactDetailActivityClassName()));
                GmacsUserInfo gmacsUserInfo = ClientManager.getInstance().getGmacsUserInfo();
                if (gmacsUserInfo != null) {
                    intent.putExtra(GmacsConstant.EXTRA_USER_ID, gmacsUserInfo.userId);
                    intent.putExtra(GmacsConstant.EXTRA_USER_SOURCE, gmacsUserInfo.userSource);
                    intent.putExtra(GmacsConstant.EXTRA_TALK_TYPE, Gmacs.TalkType.TALKTYPE_NORMAL.getValue());
                    mActivity.startActivity(intent);
                }
            } catch (ClassNotFoundException ignored) {
            }
        } else if (v.getId() == R.id.left_head) {
            Object o = v.getTag();
            if (o instanceof Message.MessageUserInfo) {
                Message.MessageUserInfo userInfo = (Message.MessageUserInfo) o;
                try {
                    Intent intent = new Intent(mActivity, Class.forName(GmacsUiUtil.getContactDetailActivityClassName()));
                    intent.putExtra(GmacsConstant.EXTRA_USER_ID, userInfo.mUserId);
                    intent.putExtra(GmacsConstant.EXTRA_USER_SOURCE, userInfo.mUserSource);
                    intent.putExtra(GmacsConstant.EXTRA_TALK_TYPE, Gmacs.TalkType.TALKTYPE_NORMAL.getValue());
                    mActivity.startActivity(intent);
                } catch (ClassNotFoundException ignored) {
                }
            }
        }
    }

    @Override
    public boolean onLongClick(View v) {
        if (v.getId() == R.id.left_head) {
            if (mTalk != null && TalkType.isGroupTalk(mTalk)) {
                Object o = v.getTag();
                if (o != null && o instanceof Message.MessageUserInfo) {
                    final Message.MessageUserInfo userInfo = (Message.MessageUserInfo) o;
                    GroupMember member = groupMemberInfoCache.get(Talk.getTalkId(userInfo.mUserSource, userInfo.mUserId));
                    mActivity.sendMsgLayout.insertAtText(false,
                            member.getNameToShow(),
                            userInfo.mUserId,
                            userInfo.mUserSource,
                            TextUtils.isEmpty(member.getGroupNickName()) ? member.getName() : member.getGroupNickName());
                }
            }
        }
        return true;
    }

    public void replaceSpecificPositionWithMessage(int position, Message message) {
        mAllMessage.remove(position);
        mAllMessage.add(position, message);
    }

    public void updateSendStatusAndCardContentForSpecificItem(int position, View convertView, Message message) {
        if (mAllMessage.get(position) != message) {
            replaceSpecificPositionWithMessage(position, message);
        }
        MsgViewHolder viewHolder = (MsgViewHolder) convertView.getTag();
        if (viewHolder != null) {
            IMMessageView imMessageView = (IMMessageView) viewHolder.contentItem.getTag();
            imMessageView.setDataForView(message.getMsgContent());
            updateUIBySendStatus(message.getMsgContent(), viewHolder);
        }
    }

    private void updateUIBySendStatus(IMMessage msg, MsgViewHolder viewHolder) {
        if (msg == null) {
            return;
        }
        if (viewHolder.sendFailed != null && viewHolder.sending != null) {
            switch (msg.message.getSendStatus()) {
                case GmacsConstant.MSG_SEND_FAILED:
                    viewHolder.sendFailed.setVisibility(View.VISIBLE);
                    viewHolder.sendFailed.setTag(msg.message);
                    viewHolder.sendFailed.setOnClickListener(new SendFailedRetryListener(viewHolder));
                    viewHolder.sending.setVisibility(View.GONE);
                    break;
                case GmacsConstant.MSG_SENDING:
                    viewHolder.sendFailed.setVisibility(View.GONE);
                    viewHolder.sending.setVisibility(View.VISIBLE);
                    break;
                case GmacsConstant.MSG_UNSENT:
                case GmacsConstant.MSG_SENT:
                case GmacsConstant.MSG_FAKE_MSG:
                default:
                    viewHolder.sendFailed.setVisibility(View.GONE);
                    viewHolder.sending.setVisibility(View.GONE);
                    break;
            }
        }
    }

    /**
     * 发送失败的消息
     */
    protected void sendFailedIMMsg(MsgViewHolder viewHolder) {
        final Message message = (Message) viewHolder.sendFailed.getTag();
        mActivity.reSendMsg(message);
    }

    /**
     * 替换成消息card中拥有的独立发送中视图
     *
     * @param viewHolder
     * @param msgCardView
     */
    private void replaceAloneSendProgressBar(MsgViewHolder viewHolder, View msgCardView) {
        ProgressBar aloneSendProgressBar = (ProgressBar) msgCardView.findViewById(R.id.send_progress);
        if (aloneSendProgressBar != null && viewHolder.sending != null) {
            viewHolder.sending.setVisibility(View.GONE);
            viewHolder.sending = aloneSendProgressBar;
        }
    }

    public void setOnLeftAvatarClickListener(View.OnClickListener l) {
        this.leftAvatarClickListener = l;
    }

    public void setOnRightAvatarClickListener(View.OnClickListener l) {
        this.rightAvatarClickListener = l;
    }

    public void setOnLeftAvatarLongClickListener(View.OnLongClickListener l) {
        leftAvatarLongClickListener = l;
    }

    public boolean updateGroupMemberInfoCache(UserInfo info) {
        String talkId = Talk.getTalkId(info.getSource(), info.getId());
        GroupMember groupMember = groupMemberInfoCache.get(talkId);
        if (groupMember != null) {
            if (!TextUtils.equals(groupMember.getName(), info.name)
                    || !TextUtils.equals(groupMember.getAvatar(), info.avatar)
                    || !TextUtils.equals(groupMember.getRemarkName(), info.remark.remark_name)) {
                groupMember.updateFromContact((Contact) info);
                return true;
            }
        }
        return false;
    }

    public IMMessage getNextIMMessage(IMMessage imMessage) {
        for (int i = mAllMessage.size() - 1; i >= 0; i--) {
            if (imMessage == mAllMessage.get(i).getMsgContent()) {
                if (i + 1 < mAllMessage.size()) {
                    return mAllMessage.get(i + 1).getMsgContent();
                }
                break;
            }
        }
        return null;
    }

    public List<IMMessage> getAllIMMessageForSpecificType(Class type) {
        List<IMMessage> list = new ArrayList<>();
        for (int i = 0; i < mAllMessage.size(); i++) {
            IMMessage imMessage = mAllMessage.get(i).getMsgContent();
            if (type.isInstance(imMessage)) {
                list.add(imMessage);
            }
        }
        return list;
    }

    public interface FillGroupMemberInfoCb {
        void done();
    }

    protected class MsgViewHolder {
        public TextView time;
        public TextView leftName;
        public NetworkImageView leftHead, rightHead;
        public ViewGroup contentItem;
        public ProgressBar sending;
        public ImageView sendFailed;
    }

    // 消息重新发送监听
    private class SendFailedRetryListener implements View.OnClickListener {

        private MsgViewHolder viewHolder;

        public SendFailedRetryListener(MsgViewHolder viewHolder) {
            this.viewHolder = viewHolder;
        }

        @Override
        public void onClick(View v) {
            final GmacsDialog.Builder dialog = new GmacsDialog.Builder(mActivity, GmacsDialog.Builder.DIALOG_TYPE_TEXT_NEG_POS_BUTTON);
            dialog.initDialog(mActivity.getText(R.string.retry_or_not), mActivity.getText(R.string.retry), mActivity.getText(R.string.cancel), new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    sendFailedIMMsg(viewHolder);
                    dialog.dismiss();
                }
            }, new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dialog.dismiss();
                }
            }).create().show();
        }
    }
}