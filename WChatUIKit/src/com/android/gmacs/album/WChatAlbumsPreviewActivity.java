package com.android.gmacs.album;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.ListView;

import com.android.gmacs.R;
import com.android.gmacs.activity.BaseActivity;
import com.android.gmacs.event.WChatAlbumImagesDeletedEvent;
import com.android.gmacs.logic.TalkLogic;
import com.common.gmacs.core.ContactsManager;
import com.common.gmacs.core.GmacsConstant;
import com.common.gmacs.core.MessageManager;
import com.common.gmacs.msg.MsgContentType;
import com.common.gmacs.parse.Pair;
import com.common.gmacs.parse.contact.Group;
import com.common.gmacs.parse.contact.GroupMember;
import com.common.gmacs.parse.contact.UserInfo;
import com.common.gmacs.parse.message.Message;
import com.common.gmacs.parse.search.SearchedTalk;
import com.common.gmacs.parse.talk.Talk;
import com.common.gmacs.utils.GLog;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import static com.android.gmacs.album.AlbumConstant.MSG_COUNT_PER_FETCHING;


public class WChatAlbumsPreviewActivity extends BaseActivity {

    private final Object lock = new Object();
    private WChatAlbumAdapter mAdapter;

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onAlbumImagesDeleted(final WChatAlbumImagesDeletedEvent event) {
        List<Long> list = event.getDeletedLocalIdList();
        if (list != null) {
            fetchImageMessages(Collections.singletonList(new Pair(event.getUserId(), event.getUserSource())),
                    new ImageTalksCallback() {
                        @Override
                        public void done(final List<WChatAlbumAdapter.MsgGroupInfo> msgGroupInfoList) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    int index = mAdapter.removeMsgFromAdapter(event.getUserId(), event.getUserSource());
                                    mAdapter.addNewMsgGroupsToAdapter(msgGroupInfoList, index);
                                }
                            });
                        }
                    });
        }
    }

    @Override
    protected void initView() {
        EventBus.getDefault().register(this);
        setTitle(R.string.album_preview);
        mAdapter = new WChatAlbumAdapter(this, false);
        ListView listView = (ListView) findViewById(R.id.album_list);
        listView.setAdapter(mAdapter);
    }

    @Override
    protected void initData() {
        getImageMessagesAfterSyncingRecentTalk(new ImageTalksCallback() {
            @Override
            public void done(final List<WChatAlbumAdapter.MsgGroupInfo> msgGroupInfoList) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mAdapter.addNewMsgGroupsToAdapter(msgGroupInfoList, -1);
                    }
                });
            }
        });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.wchat_activity_album);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (resultCode == AlbumConstant.RESULT_CODE_IMAGE_DELETED) {
            final String userId = intent.getStringExtra(GmacsConstant.EXTRA_USER_ID);
            final int userSource = intent.getIntExtra(GmacsConstant.EXTRA_USER_SOURCE, -1);
            fetchImageMessages(Collections.singletonList(new Pair(userId, userSource)),
                    new ImageTalksCallback() {
                        @Override
                        public void done(final List<WChatAlbumAdapter.MsgGroupInfo> msgGroupInfoList) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    int index = mAdapter.removeMsgFromAdapter(userId, userSource);
                                    mAdapter.addNewMsgGroupsToAdapter(msgGroupInfoList, index);
                                }
                            });
                        }
                    });
        }
    }

    @Override
    protected void onDestroy() {
        EventBus.getDefault().unregister(this);
        super.onDestroy();
    }

    private void fetchImageMessages(List<Pair> pairs, final ImageTalksCallback cb) {
        MessageManager.getInstance().getMessagesByShowTypeForTalks(pairs, new String[]{MsgContentType.TYPE_IMAGE},
                MSG_COUNT_PER_FETCHING, new MessageManager.GetTalksWithTypeCb() {
                    @Override
                    public void done(int errorCode, String errorMsg, List<SearchedTalk> talkList) {
                        if (cb == null) {
                            return;
                        }

                        final List<WChatAlbumAdapter.MsgGroupInfo> result = new ArrayList<>();

                        for (SearchedTalk searchedTalk : talkList) {
                            ArrayList<Message> messages = searchedTalk.getMessageList();
                            final Talk talk = searchedTalk.getTalk();
                            final int msgCount = searchedTalk.getMsgCount();
                            String userId = talk.mTalkOtherUserId;
                            int userSource = talk.mTalkOtherUserSource;

                            final ArrayList<ArrayList<Message>> resultList = new ArrayList<>();
                            WChatAlbumUtil.split(false, messages, resultList);

                            if (talk.mTalkOtherUserInfo == null) {
                                synchronized (lock) {
                                    ContactsManager.getInstance().getUserInfoAsync(userId, userSource,
                                            new ContactsManager.GetUserInfoCb() {
                                                @Override
                                                public void done(int errorCode, String errorMessage, UserInfo contactInfo) {
                                                    if (errorCode != 0) {
                                                        GLog.e(TAG, "getUserInfoAsync:errorCode " + errorCode +
                                                                " errorMessage " + errorMessage);
                                                    } else {
                                                        talk.mTalkOtherUserInfo = contactInfo;
                                                        if (contactInfo instanceof Group) {
                                                            fetchGroupMemberInfo(contactInfo);
                                                        }
                                                    }
                                                    result.add(new WChatAlbumAdapter.MsgGroupInfo(resultList,
                                                            talk.mTalkOtherUserInfo, talk.getTalkId(), msgCount));

                                                    synchronized (lock) {
                                                        lock.notifyAll();
                                                    }
                                                }
                                            });
                                    try {
                                        lock.wait();
                                    } catch (InterruptedException e) {
                                        e.printStackTrace();
                                    }
                                }
                            } else if (talk.mTalkOtherUserInfo instanceof Group) {
                                fetchGroupMemberInfo(talk.mTalkOtherUserInfo);
                                result.add(new WChatAlbumAdapter.MsgGroupInfo(resultList,
                                        talk.mTalkOtherUserInfo, talk.getTalkId(), msgCount));
                            } else {
                                result.add(new WChatAlbumAdapter.MsgGroupInfo(resultList,
                                        talk.mTalkOtherUserInfo, talk.getTalkId(), msgCount));
                            }
                        }

                        cb.done(result);
                    }
                });
    }

    private void fetchGroupMemberInfo(UserInfo contactInfo) {
        HashSet<Pair> users = new HashSet<>();
        List<GroupMember> members = ((Group) contactInfo).getMembers();
        boolean hasRealName = !TextUtils.isEmpty(contactInfo.getNameToShow());
        int maxCountToFetch = hasRealName ? 4 : TalkLogic.MAX_GROUP_MEMBER_COUNT;
        for (int i = 0; i < members.size() && i < maxCountToFetch; i++) {
            GroupMember member = members.get(i);
            users.add(new Pair(member.getId(), member.getSource()));
        }
        TalkLogic.getInstance().fillGroupMemberInfoFromLocal((Group) contactInfo, users);
        if (!hasRealName) {
            contactInfo.name = TalkLogic.getInstance().getGroupTalkName((Group) contactInfo, maxCountToFetch);
        }
    }

    private void getImageMessagesAfterSyncingRecentTalk(final ImageTalksCallback cb) {
        fetchImageMessages(TalkLogic.getInstance().getSimplePairForRecentTalks(), cb);
    }

    private interface ImageTalksCallback {
        void done(List<WChatAlbumAdapter.MsgGroupInfo> msgGroupInfoList);
    }

}
