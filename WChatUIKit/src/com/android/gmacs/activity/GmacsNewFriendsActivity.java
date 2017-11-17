package com.android.gmacs.activity;

import android.os.Bundle;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.android.gmacs.R;
import com.android.gmacs.adapter.GmacsNewFriendsListAdapter;
import com.android.gmacs.event.AddContactMsgEvent;
import com.android.gmacs.event.LoadAddFriendRequestsEvent;
import com.android.gmacs.logic.MessageLogic;
import com.android.gmacs.view.GmacsDialog;
import com.common.gmacs.core.RecentTalkManager;
import com.common.gmacs.msg.data.IMReqFriendMsg;
import com.common.gmacs.parse.message.Message;
import com.common.gmacs.parse.talk.TalkType;
import com.common.gmacs.utils.GmacsEnvi;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.List;


public class GmacsNewFriendsActivity extends BaseActivity {

    private ArrayList<Message> mRequestArray = new ArrayList<>();
    private GmacsNewFriendsListAdapter mAdapter;
    private long lastMsgLocalId;
    private boolean hasMore;
    private ListView mListView;
    private TextView mEmptyView;

    @Override
    protected void initView() {
        EventBus.getDefault().register(this);
        setTitle(getText(R.string.new_friends));
        mListView = (ListView) findViewById(R.id.new_friends_list);
        mAdapter = new GmacsNewFriendsListAdapter(this, mRequestArray);
        mListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                final int messagePosition = position;
                final GmacsDialog.Builder dialog = new GmacsDialog.Builder(view.getContext(),
                        GmacsDialog.Builder.DIALOG_TYPE_LIST_NO_BUTTON);
                dialog.initDialog(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                        switch (position) {
                            case 0:// 删除消息
                                Message message = mRequestArray.get(messagePosition);
                                message.isDeleted = true;
                                mRequestArray.remove(messagePosition);
                                if (mRequestArray.size() == 0) {
                                    mListView.setEmptyView(mEmptyView);
                                }
                                mAdapter.notifyDataSetChanged();
                                Message.MessageUserInfo otherInfo = message.getTalkOtherUserInfo();
                                MessageLogic.getInstance().deleteMsgByLocalId(otherInfo.mUserId, otherInfo.mUserSource, message.mLocalId);
                                dialog.dismiss();
                                break;
                        }
                        dialog.dismiss();
                    }
                }).setListTexts(new String[]{view.getContext().getString(R.string.delete_message)}).create().show();
                return true;
            }
        });
        mListView.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
//                isScrollEnd = view.getChildAt(view.getChildCount() - 1).getBottom() <= view.getHeight();
                if (mRequestArray.size() > 0 && mRequestArray.get(mRequestArray.size() - 1).mLinkMsgId != -3 &&
                        scrollState == SCROLL_STATE_IDLE && view.getLastVisiblePosition() == view.getCount() - 1) {
                    mListView.setTranscriptMode(ListView.TRANSCRIPT_MODE_DISABLED);
                    loadRequestMessages();
                }
            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
            }
        });
        mListView.setAdapter(mAdapter);
        mEmptyView = (TextView) findViewById(R.id.empty_view);
        ((LinearLayout.LayoutParams) mEmptyView.getLayoutParams()).topMargin = (int) (GmacsEnvi.screenHeight * 0.2);
        RecentTalkManager.getInstance().ackTalkShow("SYSTEM_FRIEND", 1999);
    }

    @Override
    protected void initData() {
        loadRequestMessages();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.gmacs_activity_new_friends);
    }

    private void loadRequestMessages() {
        if (mRequestArray.size() > 0) {
            lastMsgLocalId = mRequestArray.get(mRequestArray.size() - 1).mLocalId;
        } else {
            lastMsgLocalId = -1;
        }
        MessageLogic.getInstance().getRequestFriendMessages(lastMsgLocalId, 20);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onLoadRequestFriendHistoryMessage(LoadAddFriendRequestsEvent event) {
        List<Message> messages = event.getMessages();
        if (messages != null) {
            int size = messages.size();
            if (size > 0) {
                mRequestArray.addAll(messages);
                hasMore = messages.get(size - 1).mLinkMsgId != -3 && lastMsgLocalId == -1;
                if (hasMore) {
                    loadRequestMessages();
                } else {
                    mAdapter.notifyDataSetChanged();
                }
                return;
            } else {
                mListView.setEmptyView(mEmptyView);
            }
        }
        hasMore = false;
        mAdapter.notifyDataSetChanged();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onNewFriendRequestReceived(Message message) {
        if (message != null && TalkType.isUserRequestTalk(message)) {
            Message.MessageUserInfo otherInfo = message.mSenderInfo;
            if (otherInfo != null && "SYSTEM_FRIEND".equals(otherInfo.mUserId) && otherInfo.mUserSource == 1999) {
                mListView.setEmptyView(null);
                mAdapter.addNewMsgToTop(message);
                mListView.setTranscriptMode(ListView.TRANSCRIPT_MODE_ALWAYS_SCROLL);
                RecentTalkManager.getInstance().ackTalkShow("SYSTEM_FRIEND", 1999);
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onNewFriendsAcceptReceived(AddContactMsgEvent event) {
        IMReqFriendMsg imReqFriendMsg;
        String msgId;
        for (Message message : mRequestArray) {
            imReqFriendMsg = (IMReqFriendMsg) message.getMsgContent();
            msgId = String.valueOf(message.mMsgId);
            if (event.getContactId().equals(imReqFriendMsg.reqId) &&
                    event.getContactSource() == imReqFriendMsg.reqSource &&
                    event.getMsgId().equals(msgId)) {
                imReqFriendMsg.acceptTime = event.getAcceptFriendMessage().acceptTime;
                break;
            }
        }
        if (mListView.getAdapter() == null) {
            mListView.setAdapter(mAdapter);
        } else {
            mAdapter.notifyDataSetChanged();
        }
    }

}
