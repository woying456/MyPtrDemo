package com.android.gmacs.album;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.text.TextUtils;
import android.view.MenuItem;
import android.widget.AbsListView;

import com.android.gmacs.R;
import com.android.gmacs.activity.BaseActivity;
import com.android.gmacs.event.WChatAlbumImagesDeletedEvent;
import com.android.gmacs.logic.TalkLogic;
import com.common.gmacs.core.GmacsConstant;
import com.common.gmacs.core.GroupManager;
import com.common.gmacs.core.MessageManager;
import com.common.gmacs.msg.MsgContentType;
import com.common.gmacs.parse.Pair;
import com.common.gmacs.parse.contact.Group;
import com.common.gmacs.parse.contact.GroupMember;
import com.common.gmacs.parse.message.Message;
import com.common.gmacs.parse.search.SearchedTalk;
import com.common.gmacs.utils.GmacsEnvi;

import org.greenrobot.eventbus.EventBus;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;


public class WChatAlbumBrowserActivity extends BaseActivity {

    private final int IMAGE_MAX_SIZE = (GmacsEnvi.screenWidth
            - GmacsEnvi.appContext.getResources().getDimensionPixelOffset(R.dimen.album_image_padding)
            * (AlbumConstant.MAX_IMAGE_AMOUNT_IN_ROW - 1)) / AlbumConstant.MAX_IMAGE_AMOUNT_IN_ROW;
    private final int MAX_COUNT_PER_PAGE = (int) Math.ceil(GmacsEnvi.screenHeight / IMAGE_MAX_SIZE)
            * AlbumConstant.MAX_IMAGE_AMOUNT_IN_ROW;

    private boolean mEndOfMsgLoading;
    private boolean mImageDeleted;
    private int mLastVisiblePosition;
    private int mUserSource;
    private long mLastMsgLocalId;
    private String mUserId;
    private LinkedList<Long> mDeletedLocalIdList = new LinkedList<>();
    private WChatAlbumListView mListView;
    private WChatAlbumAdapter mAdapter;

    public void fetchNewPage(boolean deleteToFetch) {
        if (deleteToFetch) {
            mImageDeleted = true;
        }
        if (deleteToFetch && mListView.getLastVisiblePosition() < mLastVisiblePosition + MAX_COUNT_PER_PAGE / 2) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mAdapter.addNewMsgGroupsToAdapter(null);
                }
            });
        } else {
            MessageManager.getInstance().getMessagesByShowTypeForSingleTalk(
                    mUserId,
                    mUserSource,
                    new String[]{MsgContentType.TYPE_IMAGE},
                    mLastMsgLocalId,
                    MAX_COUNT_PER_PAGE,
                    new MessageManager.GetMsgsWithTypeCb() {
                        @Override
                        public void done(int errorCode, String errorMsg, final List<Message> msgList) {
                            if (msgList.size() > 0) {
                                mLastMsgLocalId = msgList.get(msgList.size() - 1).mLocalId;
                                mEndOfMsgLoading = msgList.size() < MAX_COUNT_PER_PAGE;
                            } else {
                                mEndOfMsgLoading = true;
                            }
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    mAdapter.addNewMsgGroupsToAdapter(msgList.size() > 0 ? new ArrayList<>(msgList) : null);
                                    mListView.smoothScrollBy(IMAGE_MAX_SIZE / 2, 800);
                                }
                            });
                        }
                    });
        }
    }

    @Override
    public void finish() {
        if (mImageDeleted) {
            EventBus.getDefault().post(new WChatAlbumImagesDeletedEvent(mDeletedLocalIdList, mUserId, mUserSource));
        }
        super.finish();
    }

    @Override
    protected void initView() {
        Intent intent = getIntent();
        mUserId = intent.getStringExtra(GmacsConstant.EXTRA_USER_ID);
        mUserSource = intent.getIntExtra(GmacsConstant.EXTRA_USER_SOURCE, -1);

        String title = getIntent().getStringExtra(AlbumConstant.ALBUM_TITLE);
        if (mUserSource < 10000 || !TextUtils.isEmpty(title)) {
            setTitle(title);
        } else {
            GroupManager.getGroupInfoAsync(mUserId, mUserSource, new GroupManager.GetGroupInfoCb() {
                @Override
                public void done(int errorCode, String errorMessage, final Group group) {
                    if (group != null) {
                        HashSet<Pair> users = new HashSet<>();
                        for (int i = 0; i < group.getMembers().size() && i < 4; i++) {
                            GroupMember member = group.getMembers().get(i);
                            users.add(new Pair(member.getId(), member.getSource()));
                        }
                        TalkLogic.getInstance().fillGroupMemberInfoFromLocal(group, users);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                setTitle(TalkLogic.getInstance().getGroupTalkName(group, TalkLogic.MAX_GROUP_MEMBER_COUNT));
                            }
                        });
                    }
                }
            });
        }
        mAdapter = new WChatAlbumAdapter(this, true);
        mListView = (WChatAlbumListView) findViewById(R.id.album_list);
        mListView.setAdapter(mAdapter);
    }

    @Override
    protected void initData() {
        MessageManager.getInstance().getMessagesByShowTypeForTalks(
                Collections.singletonList(new Pair(mUserId, mUserSource)),
                new String[]{MsgContentType.TYPE_IMAGE}, MAX_COUNT_PER_PAGE, new MessageManager.GetTalksWithTypeCb() {
                    @Override
                    public void done(int errorCode, String errorMsg, List<SearchedTalk> talkList) {
                        if (talkList != null && talkList.size() > 0) {
                            final SearchedTalk searchedTalk = talkList.get(0);
                            mEndOfMsgLoading = searchedTalk.getMessageList().size() < MAX_COUNT_PER_PAGE;
                            mLastMsgLocalId = searchedTalk.getMessageList().get(searchedTalk.getMessageList().size() - 1).mLocalId;
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    mAdapter.addNewMsgGroupsToAdapter(searchedTalk.getMessageList());
                                }
                            });
                        }
                    }
                });

        mListView.setScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
                if (scrollState == AbsListView.OnScrollListener.SCROLL_STATE_IDLE
                        && (mLastVisiblePosition = mListView.getLastVisiblePosition()) == mListView.getCount() - 1
                        && !mEndOfMsgLoading) {
                    fetchNewPage(false);
                }
            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
            }
        });

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowNoTitle(true);
        ActionBar actionBar = getSupportActionBar();
        if (null != actionBar) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
        setContentView(R.layout.wchat_activity_album);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (resultCode == AlbumConstant.RESULT_CODE_IMAGE_DELETED) {
            long deletedLocalId = intent.getLongExtra(AlbumConstant.DELETING_MSG_LOCAL_ID, -1);
            mAdapter.removeMsgFromAdapter(deletedLocalId);
            mDeletedLocalIdList.add(deletedLocalId);
        }
    }


}