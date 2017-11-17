package com.android.gmacs.activity;

import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.android.gmacs.R;
import com.android.gmacs.adapter.UserInfoAdapter;
import com.android.gmacs.view.FastLetterIndexView;
import com.android.gmacs.view.PinnedHeaderListView;
import com.common.gmacs.parse.contact.GmacsUser;
import com.common.gmacs.parse.contact.Group;
import com.common.gmacs.parse.contact.GroupMember;
import com.common.gmacs.parse.contact.UserInfo;
import com.common.gmacs.utils.GmacsEnvi;
import com.common.gmacs.utils.PinyinComparator;
import com.common.gmacs.utils.StringUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

public abstract class BaseSelectUserMemberActivity extends UserInfoBaseActivity implements AdapterView.OnItemClickListener, View.OnClickListener {

    protected PinnedHeaderListView mGroupMemberListView;
    protected FastLetterIndexView mFastLetterIndexView;
    protected TextView mTvToastIndex;
    protected UserInfoAdapter userInfoAdapter;
    protected List<UserInfo> groupMembersInfoList = new ArrayList<>();
//    protected TextView tvTitle;

    protected SearchBarClickListener searchBarClickListener = null;
    /**
     * 存储需要过滤的人
     */
    protected HashSet<String> filterUsers = new HashSet<>();
    protected LinearLayout mHeaderContainer;
    private RelativeLayout searchHeader;

    protected abstract void setSearchBarClickListener();

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
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setBackEnable(false);
        requestWindowNoTitle(true);
        ActionBar actionBar = getSupportActionBar();
        if (null != actionBar) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
//        setTitleBarDelegateResId(R.layout.wchat_group_add_from_contacts_title);
        setContentView(R.layout.wchat_activity_select_group_member);
        setSearchBarClickListener();
    }

    @Override
    protected void initView() {
//        tvTitle = (TextView) mTitleBarDelegate.findViewById(R.id.tv_title);
//        mTitleBarDelegate.findViewById(R.id.title_bar_confirm).setVisibility(View.GONE);

        mGroupMemberListView = (PinnedHeaderListView) findViewById(R.id.pinnedheaderlistview_contacts);

        searchHeader = (RelativeLayout) LayoutInflater.from(BaseSelectUserMemberActivity.this).inflate(R.layout.wchat_search_entry, mGroupMemberListView, false);
        searchHeader.setOnClickListener(this);
        mGroupMemberListView.addHeaderView(searchHeader);
        mHeaderContainer = new LinearLayout(this);
        mGroupMemberListView.addHeaderView(mHeaderContainer);

        mFastLetterIndexView = (FastLetterIndexView) findViewById(R.id.fastLetterIndexView);
        mTvToastIndex = (TextView) findViewById(R.id.tv_toast_index);
        final LayoutInflater inflater = LayoutInflater.from(BaseSelectUserMemberActivity.this);
        mGroupMemberListView.setPinnedHeaderView(inflater.inflate(R.layout.gmacs_item_list_separators, mGroupMemberListView, false));
        mGroupMemberListView.setEnabledPinnedHeaderDynamicAlphaEffect(true);
        mGroupMemberListView.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                if (mGroupMemberListView != null) {
                    mGroupMemberListView.onPinnedHeaderScroll(firstVisibleItem - mGroupMemberListView.getHeaderViewsCount());
                }
            }
        });

        mGroupMemberListView.setOnItemClickListener(this);
        userInfoAdapter = new UserInfoAdapter(BaseSelectUserMemberActivity.this, groupMembersInfoList);
        mGroupMemberListView.setAdapter(userInfoAdapter);

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
                 * 检索字母条与首字母相对应
                 */
                for (int i = 0; i < (groupMembersInfoList).size(); i++) {
                    GroupMember userInfo = (GroupMember) groupMembersInfoList.get(i);
                    String spell;
                    if (!TextUtils.isEmpty(userInfo.remark.remark_name)) {
                        if (!TextUtils.isEmpty(userInfo.remark.remark_spell)) {
                            spell = userInfo.remark.remark_spell;
                        } else {
                            spell = "#" + userInfo.remark.remark_name;
                        }
                    } else {
                        if (!TextUtils.isEmpty(userInfo.getGroupNickName())) {
                            if (!TextUtils.isEmpty(userInfo.getGroupNickNameSpell())) {
                                spell = userInfo.getGroupNickNameSpell();
                            } else {
                                spell = "#" + userInfo.getGroupNickName();
                            }
                        } else {
                            if (!TextUtils.isEmpty(userInfo.nameSpell)) {
                                spell = userInfo.nameSpell;
                            } else {
                                spell = "#" + userInfo.name;
                            }
                        }
                    }

                    if (StringUtil.getAlpha(spell).equalsIgnoreCase(letterIndex)) {
                        mGroupMemberListView.setSelection(i + mGroupMemberListView.getHeaderViewsCount());
                        break;
                    }
                }
            }
        });
    }


    @Override
    protected void updateUI() {
        if (info instanceof Group) {
            if (groupMembersInfoList.size() > 0) {
                groupMembersInfoList.clear();
            }

            groupMembersInfoList.addAll(((Group) info).getMembers());
            filterUser((Group) info);
            Collections.sort(groupMembersInfoList, new PinyinComparator());

            ArrayList<String> lettersArray = new ArrayList<>();
            String current = "";
            for (UserInfo member : groupMembersInfoList) {
                String temp = member.getFirstLetter();
                if (!current.equals(temp)) {
                    current = temp;
                    lettersArray.add(current);
                }
            }

            int maxHeight = (int) (GmacsEnvi.screenHeight - getResources().getDimension(R.dimen.titlebar_height));
            mFastLetterIndexView.setMaxDisplayHeight(maxHeight);
            mFastLetterIndexView.setLetter(lettersArray);

            if (userInfoAdapter == null) {
                userInfoAdapter = new UserInfoAdapter(BaseSelectUserMemberActivity.this, groupMembersInfoList);
                mGroupMemberListView.setAdapter(userInfoAdapter);
            } else {
                userInfoAdapter.notifyDataSetChanged();
            }
        }
    }

    private void filterUser(Group group) {
        String id = GmacsUser.getInstance().getUserId();
        int source = GmacsUser.getInstance().getSource();
        for (GroupMember m : group.getMembers()) {
            if (TextUtils.equals(m.getId(), id) && m.getSource() == source) {
                groupMembersInfoList.remove(m);
                break;
            }
        }

    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
    }

    @Override
    public void onClick(View v) {
        if (v == searchHeader) {
            searchBarClickListener.onClick();
        }
    }

    @Override
    protected void onDestroy() {
        groupMembersInfoList.clear();
        groupMembersInfoList = null;
        filterUsers.clear();
        filterUsers = null;
        super.onDestroy();
    }

    public interface SearchBarClickListener {
        void onClick();
    }
}
