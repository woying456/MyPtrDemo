package com.android.gmacs.logic;

import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;

import com.android.gmacs.event.RecentTalksEvent;
import com.android.gmacs.event.UnreadTotalEvent;
import com.common.gmacs.core.ClientManager;
import com.common.gmacs.core.ContactsManager;
import com.common.gmacs.core.Gmacs;
import com.common.gmacs.core.RecentTalkManager;
import com.common.gmacs.parse.Pair;
import com.common.gmacs.parse.contact.Contact;
import com.common.gmacs.parse.contact.GmacsUser;
import com.common.gmacs.parse.contact.Group;
import com.common.gmacs.parse.contact.GroupMember;
import com.common.gmacs.parse.contact.UserInfo;
import com.common.gmacs.parse.message.Message;
import com.common.gmacs.parse.talk.Talk;
import com.common.gmacs.utils.GLog;
import com.common.gmacs.utils.ImageUtil;

import org.greenrobot.eventbus.EventBus;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class TalkLogic extends BaseLogic implements RecentTalkManager.TalkChangeListener {

    public static final int MAX_GROUP_MEMBER_COUNT = 6;
    private static volatile TalkLogic instance;
    public final int[] msgTypeList;
    private final Handler handler;
    private HashSet<Pair> fetchingUsers;
    private volatile int unreadMsgCount;
    private List<Talk> recentTalks;

    private TalkLogic() {
        msgTypeList = new int[]{
                Gmacs.TalkType.TALKTYPE_SYSTEM.getValue(),
                Gmacs.TalkType.TALKTYPE_NORMAL.getValue(),
                Gmacs.TalkType.TALKTYPE_OFFICIAL.getValue(),
//                Gmacs.TalkType.TALKTYPE_POSTINGS.getValue(),
                Gmacs.TalkType.TALKTYPE_GROUP.getValue()};
        handler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(android.os.Message msg) {
                super.handleMessage(msg);
                getRecentTalks();
                MessageLogic.getInstance().getUnreadFriendCount();
            }
        };
    }

    public static TalkLogic getInstance() {
        if (null == instance) {
            synchronized (TalkLogic.class) {
                if (null == instance) {
                    instance = new TalkLogic();
                }
            }
        }
        return instance;
    }

    @Override
    public void init() {
        RecentTalkManager.getInstance().registerTalkListChangeListener(this);
    }

    /**
     * 根据类型获取最近会话
     *
     * @param msgTypeList
     */
    private void getRecentTalks(int[] msgTypeList) {
        RecentTalkManager.getInstance().getTalkByMsgTypeAsync(msgTypeList,
                new RecentTalkManager.GetTalkByMsgTypeCb() {
                    @Override
                    public void done(int errorCode, String errorMessage, List<Talk> talks, int unreadCount) {
                        if (errorCode != 0) {
                            EventBus.getDefault().post(errorMessage);
                        } else {
                            fillDetailGroupMemberInfoForGroupTalk(talks, MAX_GROUP_MEMBER_COUNT);
                            unreadMsgCount = unreadCount;
                            recentTalks = talks;
                            EventBus.getDefault().post(new RecentTalksEvent(talks));
                            EventBus.getDefault().post(new UnreadTotalEvent(unreadCount));
                        }
                    }
                });
    }

    public void getRecentTalks() {
        GLog.d(TAG, "getRecentTalks");
        getRecentTalks(msgTypeList);
    }

    /**
     * 删除会话
     *
     * @param otherId
     * @param otherSource
     */
    public void deleteTalk(String otherId, int otherSource) {
        RecentTalkManager.getInstance().deleteTalkByIdAsync(otherId,
                otherSource, new ClientManager.CallBack() {
                    @Override
                    public void done(int errorCode, String errorMessage) {
                        if (errorCode != 0) {
                            EventBus.getDefault().post(errorMessage);
                        }
                    }
                });
    }

    @Override
    public void destroy() {
        if (fetchingUsers != null) {
            fetchingUsers = null;
        }
    }

    @Override
    public void onTalkListChanged() {
        handler.removeMessages(1);
        handler.sendEmptyMessageDelayed(1, 20);
    }

    private void fillDetailGroupMemberInfoForGroupTalk(final List<Talk> talks, final int maxGroupMemberCount) {
        if (talks != null) {
            final HashSet<Pair> users = new HashSet<>();
            for (final Talk talk : talks) {
                if (talk.mTalkOtherUserInfo instanceof Group) {
                    Group group = (Group) talk.mTalkOtherUserInfo;
                    if (group.getMembers() != null) {
                        for (int i = 0; i < group.getMembers().size() && i < maxGroupMemberCount; i++) {
                            GroupMember member = group.getMembers().get(i);
                            users.add(new Pair(member.getId(), member.getSource()));
                        }
                        Message lastMessage = talk.getLastMessage();
                        //需要拉取最后一条消息的发送方信息。
                        if (lastMessage != null
                                && lastMessage.getMsgContent().isShowSenderName()
                                && !lastMessage.mIsSelfSendMsg) {
                            users.add(new Pair(lastMessage.mSenderInfo.mUserId, lastMessage.mSenderInfo.mUserSource));
                        }
                        //需要拉取最后一条消息的@信息。排除@所有人和@自己。
                        if (lastMessage != null
                                && lastMessage.atInfoArray != null) {
                            for (Message.AtInfo atInfo : lastMessage.atInfoArray) {
                                if (atInfo.userSource >= 10000) {
                                    atInfo.setNameToShow("所有人");
                                } else if (TextUtils.equals(atInfo.userId, GmacsUser.getInstance().getUserId())
                                        && atInfo.userSource == GmacsUser.getInstance().getSource()) {
                                    atInfo.setNameToShow(ClientManager.getInstance().getGmacsUserInfo().userName);
                                } else {
                                    users.add(new Pair(atInfo.userId, atInfo.userSource));
                                }
                            }
                        }
                    }
                }
            }

            if (users.size() != 0) {
                ContactsManager.getInstance().getLocalUserInfoBatchAsync(users, new ContactsManager.UserInfoBatchCb() {
                    @Override
                    public void onGetUserInfoBatch(int errorCode, String errorMessage, List<UserInfo> userInfoList) {
                        synchronized (TalkLogic.this) {
                            if (errorCode == 0 && !userInfoList.isEmpty()) {
                                for (UserInfo userInfo : userInfoList) {
                                    for (final Talk talk : talks) {
                                        if (talk.mTalkOtherUserInfo instanceof Group) {
                                            Group group = (Group) talk.mTalkOtherUserInfo;
                                            if (group.getMembers() != null) {
                                                Message lastMessage = talk.getLastMessage();
                                                //用户信息命中了最后一条消息的发送方。
                                                boolean hitLastMessageSenderName
                                                        = talk.mLastMessageSenderName == null
                                                        && lastMessage != null
                                                        && lastMessage.getMsgContent().isShowSenderName()
                                                        && !lastMessage.mIsSelfSendMsg
                                                        && TextUtils.equals(lastMessage.mSenderInfo.mUserId, userInfo.getId())
                                                        && lastMessage.mSenderInfo.mUserSource == userInfo.getSource();

                                                //用户信息命中了最后一条消息的At字段。
                                                boolean hitLastMessageAtInfo = false;
                                                if (lastMessage != null
                                                        && lastMessage.atInfoArray != null) {
                                                    for (Message.AtInfo atInfo : lastMessage.atInfoArray) {
                                                        if (atInfo.getNameToShow() == null
                                                                && atInfo.userSource == userInfo.getSource()
                                                                && TextUtils.equals(atInfo.userId, userInfo.getId())) {
                                                            hitLastMessageAtInfo = true;
                                                            break;
                                                        }
                                                    }
                                                }

                                                GroupMember hitGroupMember = null;
                                                for (int i = 0; i < group.getMembers().size() && (hitLastMessageSenderName || hitLastMessageAtInfo || i < maxGroupMemberCount); ++i) {
                                                    GroupMember groupMember = group.getMembers().get(i);
                                                    if (TextUtils.equals(groupMember.getId(), userInfo.getId())
                                                            && groupMember.getSource() == userInfo.getSource()) {
                                                        groupMember.updateFromContact((Contact) userInfo);
                                                        hitGroupMember = groupMember;
                                                        break;
                                                    }
                                                }
                                                if (hitLastMessageSenderName) {
                                                    if (hitGroupMember == null) {
                                                        talk.mLastMessageSenderName = userInfo.getNameToShow();
                                                    } else {
                                                        talk.mLastMessageSenderName = hitGroupMember.getNameToShow();
                                                    }
                                                }
                                                if (hitLastMessageAtInfo) {
                                                    for (Message.AtInfo atInfo : lastMessage.atInfoArray) {
                                                        if (atInfo.getNameToShow() == null) {
                                                            if (TextUtils.equals(atInfo.userId, userInfo.getId())
                                                                    && atInfo.userSource == userInfo.getSource()) {
                                                                if (hitGroupMember == null) {
                                                                    atInfo.setNameToShow(userInfo.getNameToShow());
                                                                } else {
                                                                    atInfo.setNameToShow(hitGroupMember.getNameToShow());
                                                                }
                                                            }
                                                        }
                                                    }
                                                }

                                                talk.mTalkOtherName = getGroupTalkName(group, maxGroupMemberCount);
                                            }
                                        }
                                    }
                                    users.remove(new Pair(userInfo.getId(), userInfo.getSource()));
                                }
                            }
                            TalkLogic.this.notifyAll();
                        }
                    }
                });
            }
            synchronized (this) {
                if (users.size() != 0) {
                    try {
                        this.wait(5000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                if (users.size() != 0) {
                    if (fetchingUsers == null) {
                        fetchingUsers = users;
                    } else {
                        users.removeAll(fetchingUsers);
                        fetchingUsers.addAll(users);
                    }
                    if (users.size() != 0) {
                        getLatestUserInfoBatchAsync(users);
                    }
                }
            }
        }
    }

    public void fillGroupMemberInfoFromLocal(final Group group, final HashSet<Pair> users) {
        synchronized (group) {
            ContactsManager.getInstance().getLocalUserInfoBatchAsync(users, new ContactsManager.UserInfoBatchCb() {
                @Override
                public void onGetUserInfoBatch(int errorCode, String errorMessage, List<UserInfo> userInfoList) {
                    synchronized (group) {
                        if (errorCode == 0 && !userInfoList.isEmpty()) {
                            for (UserInfo userInfo : userInfoList) {
                                boolean findInGroup = false;
                                for (GroupMember groupMember : group.getMembers()) {
                                    if (TextUtils.equals(groupMember.getId(), userInfo.getId())
                                            && groupMember.getSource() == userInfo.getSource()) {
                                        groupMember.updateFromContact((Contact) userInfo);
                                        findInGroup = true;
                                        break;
                                    }
                                }
                                if (!findInGroup) {
                                    GroupMember member = new GroupMember((Contact) userInfo, GroupMember.AUTHORITY_STRANGER);
                                    group.getMembers().add(member);
                                }
                                users.remove(new Pair(userInfo.getId(), userInfo.getSource()));
                            }
                        }
                        group.notifyAll();
                    }
                }
            });

            if (users.size() != 0) {
                try {
                    group.wait(5000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public String getGroupMessageSenderName(Group group, Message message) {
        if (group != null && message != null && group.getMembers() != null) {
            for (GroupMember groupMember : group.getMembers()) {
                if (TextUtils.equals(groupMember.getId(), message.mSenderInfo.mUserId)
                        && message.mSenderInfo.mUserSource == groupMember.getSource()) {
                    return groupMember.getNameToShow();
                }
            }
        }
        return "";
    }

    public String getGroupTalkName(Group group, int maxGroupMemberCount) {
        if (group.remark.remark_name != null && !TextUtils.isEmpty(group.remark.remark_name.trim())) {
            return group.remark.remark_name;
        } else if (group.name != null && !TextUtils.isEmpty(group.name.trim())) {
            return group.name;
        } else {
            StringBuilder stringBuilder = new StringBuilder();
            if (group.getMembers() != null) {
                if (group.getMembers().size() > 0) {
                    stringBuilder.append(group.getMembers().get(0).getNameToShow());
                }
                if (group.getMembers().size() > 1) {
                    for (int i = 1, j = 1; i < group.getMembers().size() && j < maxGroupMemberCount; i++) {
                        if (group.getMembers().get(i).getAuthority() != GroupMember.AUTHORITY_STRANGER) {
                            String name = group.getMembers().get(i).getNameToShow();
                            if (!TextUtils.isEmpty(name)) {
                                stringBuilder.append("、").append(name);
                            }
                            ++j;
                        }
                    }
                }
            }
            return stringBuilder.toString();
        }
    }

    private void getLatestUserInfoBatchAsync(final HashSet<Pair> users) {
        ContactsManager.getInstance().getLatestUserInfoBatchAsync(users, new ContactsManager.UserInfoBatchCb() {
            @Override
            public void onGetUserInfoBatch(int errorCode, String errorMessage, List<UserInfo> userInfoList) {
                if (errorCode == 0 && !userInfoList.isEmpty()) {
                    onTalkListChanged();
                }
                synchronized (TalkLogic.this) {
                    fetchingUsers.removeAll(users);
                }
            }
        });
    }

    public String[] getGroupTalkAvatar(Group group, int avatarSize) {
        String[] urls = null;
        if (group != null && group.getMembers() != null) {
            ArrayList<GroupMember> members = group.getMembers();
            ArrayList<String> avatars = new ArrayList<>(4);
            for (int i = 0, j = 0; i < members.size() && j < 4; i++) {
                if (group.getMembers().get(i).getAuthority() != GroupMember.AUTHORITY_STRANGER) {
                    avatars.add(ImageUtil.makeUpUrl(members.get(i).getAvatar(), avatarSize, avatarSize));
                    ++j;
                }
            }
            if (avatars.size() != 0) {
                urls = new String[avatars.size()];
                for (int i = 0; i < urls.length; ++i) {
                    urls[i] = avatars.get(i);
                }
            }
        }
        return urls;
    }

    public int getUnreadMsgCount() {
        return unreadMsgCount;
    }

    public List<Pair> getSimplePairForRecentTalks() {
        List<Pair> pairs = null;
        if (recentTalks != null) {
            pairs = new ArrayList<>();
            for (Talk talk : recentTalks) {
                pairs.add(new Pair(talk.mTalkOtherUserId, talk.mTalkOtherUserSource));
            }
        }
        return pairs;
    }

}
