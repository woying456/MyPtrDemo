package com.android.gmacs.activity;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.android.gmacs.adapter.UserInfoAdapter;
import com.android.gmacs.event.GroupsEvent;
import com.android.gmacs.logic.ContactLogic;
import com.android.gmacs.utils.GmacsUiUtil;
import com.android.gmacs.view.FastLetterIndexView;
import com.android.gmacs.view.GmacsDialog;
import com.android.gmacs.view.PinnedHeaderListView;
import com.common.gmacs.core.ClientManager;
import com.common.gmacs.core.Gmacs;
import com.common.gmacs.core.GroupManager;
import com.common.gmacs.core.RecentTalkManager;
import com.common.gmacs.parse.contact.UserInfo;
import com.common.gmacs.utils.GLog;
import com.common.gmacs.utils.StringUtil;
import com.common.gmacs.utils.ToastUtil;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.List;


public class GmacsGroupListActivity extends BaseActivity implements AdapterView.OnItemClickListener {

    protected PinnedHeaderListView mLvContactList;
    private FastLetterIndexView mFastLetterIndexView;
    private TextView mTvToastIndex, mTvNoContact;
    private UserInfoAdapter mUserInfoAdapter;
    private List<UserInfo> groups = new ArrayList<>();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EventBus.getDefault().register(this);
        setContentView(com.android.gmacs.R.layout.gmacs_contact_list);
    }

    @Override
    protected void initView() {
        mTitleBar.setTitle("群聊");
        mLvContactList = (PinnedHeaderListView) findViewById(com.android.gmacs.R.id.pinnedheaderlistview_contacts);
        mFastLetterIndexView = (FastLetterIndexView) findViewById(com.android.gmacs.R.id.fastLetterIndexView);
        mTvToastIndex = (TextView) findViewById(com.android.gmacs.R.id.tv_toast_index);
        mTvNoContact = (TextView) findViewById(com.android.gmacs.R.id.tv_no_contact);
        LayoutInflater inflater = LayoutInflater.from(this);
        mLvContactList.setPinnedHeaderView(inflater.inflate(com.android.gmacs.R.layout.gmacs_item_list_separators, mLvContactList, false));
        mLvContactList.setEnabledPinnedHeaderDynamicAlphaEffect(true);
        mLvContactList.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                if (mLvContactList != null) {
                    mLvContactList.onPinnedHeaderScroll(firstVisibleItem - mLvContactList.getHeaderViewsCount());
                }
            }
        });

        mLvContactList.setOnItemClickListener(this);
        mUserInfoAdapter = new UserInfoAdapter(this, groups);
        mLvContactList.setAdapter(mUserInfoAdapter);

        mLvContactList.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                final int realPosition = position - mLvContactList.getHeaderViewsCount();
                if (groups == null || realPosition >= groups.size() || position < mLvContactList.getHeaderViewsCount()) {
                    return false;
                }
                final GmacsDialog.Builder dialog = new GmacsDialog.Builder(view.getContext(), GmacsDialog.Builder.DIALOG_TYPE_LIST_NO_BUTTON);
                dialog.initDialog(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                        switch (position) {
                            case 0:
                                GroupManager.quitGroup(groups.get(realPosition).getId(), groups.get(realPosition).getSource(), new ClientManager.CallBack() {
                                    @Override
                                    public void done(int errorCode, String errorMessage) {
                                        if (errorCode != 0) {
                                            ToastUtil.showToast(errorMessage);
                                        }
                                    }
                                });
                                RecentTalkManager.getInstance().deleteTalkByIdAsync(groups.get(realPosition).getId(), groups.get(realPosition).getSource(), null);
                                dialog.dismiss();
                        }
                    }
                }).setListTexts(new String[]{getString(com.android.gmacs.R.string.quit_group)}).create().show();
                return true;
            }
        });

        //右侧字母滑动事件
        mFastLetterIndexView.setOnTouchLetterListener(new FastLetterIndexView.OnTouchLetterListener() {

            @Override
            public void onTouchLetter(MotionEvent event, int index, String letterIndex) {

                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        mTvToastIndex.setVisibility(View.VISIBLE);
                        break;
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        mTvToastIndex.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                mTvToastIndex.setVisibility(View.GONE);
                            }
                        }, 500);
                        break;
                }
                if (View.VISIBLE == mTvToastIndex.getVisibility()) {//显示中间检索大字母
                    mTvToastIndex.setText(letterIndex);
                }

                /*
                  检索字母条与首字母相对应
                 */
                for (int i = 0; i < groups.size(); i++) {
                    UserInfo group = groups.get(i);

                    String mNameSpell;
                    if (!TextUtils.isEmpty(group.remark.remark_spell)) {
                        mNameSpell = group.remark.remark_spell;
                    } else {
                        mNameSpell = group.getNameSpell();
                    }

                    if (StringUtil.getAlpha(mNameSpell).equals(letterIndex)) {
                        mLvContactList.setSelection(i + mLvContactList.getHeaderViewsCount());
                        break;
                    }
                }

            }
        });

    }


    @Override
    protected void initData() {
        ContactLogic.getInstance().getGroups();
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        int realPosition = position - mLvContactList.getHeaderViewsCount();
        if (position == mLvContactList.getHeaderViewsCount() - 1) {
            startActivity(new Intent(this, GmacsNewFriendsActivity.class));
            return;
        }
        if (realPosition < 0 || realPosition >= groups.size()) {
            return;
        }
        Intent intent = GmacsUiUtil.createToChatActivity(this
                , Gmacs.TalkType.TALKTYPE_NORMAL.getValue()
                , groups.get(realPosition).getId()
                , groups.get(realPosition).getSource());
        if (intent != null) {
            startActivity(intent);
        }
    }

    /**
     * 判断列表是否有数据
     */
    private void isHavePerson() {
        GLog.d(TAG, "groups.size:" + groups.size());
        if (groups.size() > 0) {
            mTvNoContact.setVisibility(View.GONE);
            mFastLetterIndexView.setVisibility(View.VISIBLE);
            mLvContactList.getLayoutParams().height = RelativeLayout.LayoutParams.MATCH_PARENT;
            mLvContactList.requestLayout();
        } else {
            mLvContactList.getLayoutParams().height = RelativeLayout.LayoutParams.WRAP_CONTENT;
            mLvContactList.requestLayout();
            mTvNoContact.setVisibility(View.VISIBLE);
            mFastLetterIndexView.setVisibility(View.GONE);
        }
    }

    @Override
    public void onDestroy() {
        EventBus.getDefault().unregister(this);
        super.onDestroy();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onGroupListChanged(GroupsEvent event) {
        groups.clear();
        if (event.getGroups() != null) {
            groups.addAll(event.getGroups());
        }
        isHavePerson();
        mUserInfoAdapter.notifyDataSetChanged();
        ArrayList<String> lettersArray = new ArrayList<>();
        String current = "";
        for (UserInfo group : groups) {
            String temp = group.getFirstLetter();
            if (!current.equals(temp)) {
                current = temp;
                lettersArray.add(current);
            }
        }
        mFastLetterIndexView.setLetter(lettersArray);
    }
}
