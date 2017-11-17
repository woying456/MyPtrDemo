package com.android.gmacs.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.android.gmacs.R;
import com.android.gmacs.activity.GlobalSearchActivity;
import com.android.gmacs.adapter.ConversationListAdapter;
import com.android.gmacs.event.RecentTalksEvent;
import com.android.gmacs.logic.TalkLogic;
import com.android.gmacs.utils.GmacsUiUtil;
import com.android.gmacs.view.GmacsDialog;
import com.common.gmacs.core.ClientManager;
import com.common.gmacs.core.GmacsConstant;
import com.common.gmacs.core.RecentTalkManager;
import com.common.gmacs.parse.talk.Talk;
import com.common.gmacs.utils.GLog;
import com.common.gmacs.utils.NetworkUtil;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.List;

public class ConversationListFragment extends BaseFragment implements AdapterView.OnItemClickListener, AdapterView.OnItemLongClickListener, ClientManager.ConnectListener {

    protected ListView mListView;
    /**
     * 为其添加子视图, 在会话列表为空时, 可以显示友好提示
     */
    protected LinearLayout mTalkListEmptyPromptView;
    /**
     * Connection status bar
     */
    protected LinearLayout mConnectionStatusHeaderView;
    protected TextView mConnectionStatusTextView;
    protected ImageView mConnectionStatusImageView;
    protected ArrayList<Talk> mTalks = new ArrayList<>();
    private ConversationListAdapter mTalkAdapter;
    private LinearLayout mConnectionStatusHeaderViewContainer;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EventBus.getDefault().register(this);
    }

    @Override
    public void setContentView() {
        layoutResID = R.layout.gmacs_conversation_list;
    }

    protected boolean showDisconnectedHeader() {
        return true;
    }

    @Override
    protected void initView() {
        mListView = (ListView) getView().findViewById(R.id.lv_conversation_list);
        mTalkListEmptyPromptView = (LinearLayout) getView().findViewById(R.id.ll_conversation_list_empty_prompt);
        RelativeLayout searchHeader = (RelativeLayout) LayoutInflater.from(getActivity()).inflate(R.layout.wchat_search_entry, mListView, false);
        searchHeader.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getActivity(), GlobalSearchActivity.class));
            }
        });
        mListView.addHeaderView(searchHeader);
        if (showDisconnectedHeader()) {
            ClientManager.getInstance().regConnectListener(this);
            mConnectionStatusHeaderViewContainer = new LinearLayout(getActivity());
            mConnectionStatusHeaderViewContainer.setLayoutParams(new AbsListView.LayoutParams(AbsListView.LayoutParams.MATCH_PARENT,
                    AbsListView.LayoutParams.WRAP_CONTENT));
            mConnectionStatusHeaderView = (LinearLayout) LayoutInflater.from(getActivity()).inflate(R.layout.gmacs_conversation_connection_status_header, mConnectionStatusHeaderViewContainer, false);

            mConnectionStatusTextView = (TextView) mConnectionStatusHeaderView.findViewById(R.id.gmacs_connection_status_text);
            mConnectionStatusImageView = (ImageView) mConnectionStatusHeaderView.findViewById(R.id.gmacs_connection_status_img);
            mConnectionStatusHeaderViewContainer.addView(mConnectionStatusHeaderView);
            showOrHideDisconnectHeader(ClientManager.getInstance().getConnectionStatus());
        }
        if (null != mConnectionStatusHeaderViewContainer) {
            mListView.addHeaderView(mConnectionStatusHeaderViewContainer);
        }
    }

    @Override
    protected void initData() {
        mTalkAdapter = new ConversationListAdapter(getActivity(), mTalks);
        mListView.setAdapter(mTalkAdapter);
        mListView.setOnItemClickListener(this);
        mListView.setOnItemLongClickListener(this);
        TalkLogic.getInstance().getRecentTalks();
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

        int realPosition = position - mListView.getHeaderViewsCount();
        if (realPosition < 0 || realPosition >= mTalks.size()) {
            return;
        }
        Talk talk = mTalks.get(realPosition);
        if (!onItemClickDelegate(talk)) {
            Intent intent = GmacsUiUtil.createToChatActivity(getActivity(), talk.mTalkType, talk.mTalkOtherUserId, talk.mTalkOtherUserSource);
            if (intent != null) {
                startActivity(intent);
            }
        }

    }

    /**
     * Override this method to delegate OnItemClickListener of conversation list.
     * You must declare which Activity you'd like to jump to by passing contents of talk as parameters.
     *
     * @param talk The assential information supporting your chat Activity's launching.
     * @return
     */
    protected boolean onItemClickDelegate(Talk talk) {
        return false;
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {

        final GmacsDialog.Builder dialog = new GmacsDialog.Builder(getActivity(), GmacsDialog.Builder.DIALOG_TYPE_LIST_NO_BUTTON);
        int realPosition = position - mListView.getHeaderViewsCount();
        if (realPosition < 0 || realPosition >= mTalks.size()) {
            return false;
        }
        final Talk talk = mTalks.get(realPosition);
        if (talk.mNoReadMsgCount > 0) {
            dialog.setListTexts(new String[]{getString(R.string.mark_as_read), getString(R.string.delete_talk)});
        } else {
            dialog.setListTexts(new String[]{getString(R.string.delete_talk)});
        }
        dialog.initDialog(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (talk.mNoReadMsgCount > 0) {
                    if (position == 0) {
                        RecentTalkManager.getInstance().ackTalkShow(talk.mTalkOtherUserId, talk.mTalkOtherUserSource);
                    } else if (position == 1) {
                        TalkLogic.getInstance().deleteTalk(talk.mTalkOtherUserId, talk.mTalkOtherUserSource);
                    }
                } else {
                    if (position == 0) {
                        TalkLogic.getInstance().deleteTalk(talk.mTalkOtherUserId, talk.mTalkOtherUserSource);
                    }
                }
                dialog.dismiss();
            }
        }).create().show();

        return true;
    }

    @Override
    public void onDestroy() {
        EventBus.getDefault().unregister(this);
        if (showDisconnectedHeader()) {
            ClientManager.getInstance().unRegConnectListener(this);
        }
        super.onDestroy();
    }

    /**
     * The method that can be invoked to update adapter associated with mListView.
     *
     * @param newAdapter
     */
    protected void setAdapter(ConversationListAdapter newAdapter) {
        mTalkAdapter = newAdapter;
        mListView.setAdapter(mTalkAdapter);
    }


    private void specialTalksPreparedWrapper(boolean isTalkListEmpty) {
        if (isTalkListEmpty) {
            mListView.getLayoutParams().height = RelativeLayout.LayoutParams.WRAP_CONTENT;
        } else {
            mListView.getLayoutParams().height = RelativeLayout.LayoutParams.MATCH_PARENT;
        }
        mListView.requestLayout();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onTalkListChanged(RecentTalksEvent event) {
        List<Talk> talks = event.getTalks();
        mTalks.clear();
        if (talks != null) {
            mTalks.addAll(talks);
        }
        if (mTalkAdapter != null) {
            mTalkAdapter.notifyDataSetChanged();
            GLog.d(TAG, "talks.size=" + mTalks.size());
            boolean isTalkListEmpty = mTalks.size() == 0;
            specialTalksPreparedWrapper(isTalkListEmpty);
        }
    }

    @Override
    public void connectStatusChanged(final int status) {
        if (getActivity() != null && !getActivity().isFinishing()) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    showOrHideDisconnectHeader(status);
                }
            });
        }
    }

    @Override
    public void connectionTokenInvalid(String errorMsg) {
    }

    /**
     * Handle the visibility of ConnectionStatusBar and change StatusBar's appearance
     * according to connection status by overriding this method.
     *
     * @param status Connection status
     */
    protected void showOrHideDisconnectHeader(int status) {
        if (null == mConnectionStatusHeaderViewContainer) {
            return;
        }
        switch (status) {
            case GmacsConstant.STATUS_DISCONNECTED:
            default:
                mConnectionStatusHeaderView.setVisibility(View.VISIBLE);
                if (NetworkUtil.isNetworkAvailable()) {
                    mConnectionStatusTextView.setText(R.string.connection_status_disconnected);
                } else {
                    mConnectionStatusTextView.setText(R.string.network_unavailable);
                }
                break;
            case GmacsConstant.STATUS_WAITING:
                mConnectionStatusHeaderView.setVisibility(View.VISIBLE);
                mConnectionStatusTextView.setText(R.string.connection_status_connecting);
                break;
            case GmacsConstant.STATUS_CONNECTING:
                mConnectionStatusHeaderView.setVisibility(View.VISIBLE);
                mConnectionStatusTextView.setText(R.string.connection_status_connecting);
                break;
            case GmacsConstant.STATUS_CONNECTED:
                mConnectionStatusHeaderView.setVisibility(View.GONE);
                break;
            case GmacsConstant.STATUS_KICK_OFF:
                mConnectionStatusHeaderView.setVisibility(View.VISIBLE);
                mConnectionStatusTextView.setText(R.string.connection_status_kickedoff);
                break;
        }
    }

    @Override
    public void onResume() {
        super.onResume();
    }
}