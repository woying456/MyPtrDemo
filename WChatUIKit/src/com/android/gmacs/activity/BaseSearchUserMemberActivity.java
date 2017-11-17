package com.android.gmacs.activity;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.android.gmacs.R;
import com.android.gmacs.view.NetworkImageView;
import com.common.gmacs.parse.contact.GmacsUser;
import com.common.gmacs.parse.contact.Group;
import com.common.gmacs.parse.contact.GroupMember;
import com.common.gmacs.parse.contact.UserInfo;
import com.common.gmacs.utils.GmacsUtils;
import com.common.gmacs.utils.ImageUtil;
import com.common.gmacs.utils.PinyinComparator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.android.gmacs.R.id.tv_search_cancel;
import static com.android.gmacs.view.NetworkImageView.IMG_RESIZE;

public class BaseSearchUserMemberActivity extends UserInfoBaseActivity implements TextWatcher, AdapterView.OnItemClickListener, View.OnClickListener {
    protected List<GroupMember> groupMembersInfoList = new ArrayList<>();
    protected List<GroupMember> searchResultList = new ArrayList<>();

    protected EditText searchBar;
    protected ListView resultContainer;
    protected ImageView searchBack;
    protected TextView emptyView;
    protected ImageView clearAll;

    private SearchGroupMemberResultAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowNoTitle(true);
        setBackEnable(false);
        setContentView(R.layout.gmacs_activity_global_search);
    }

    @Override
    protected void initView() {
        searchBar = (EditText) findViewById(R.id.et_search_bar);
        searchBar.addTextChangedListener(this);
        searchBar.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (!TextUtils.isEmpty(searchBar.getText().toString().trim())
                        && actionId == EditorInfo.IME_ACTION_SEARCH) {
                    GmacsUtils.hideSoftInputMethod(v);
                }
                return true;
            }
        });

        resultContainer = (ListView) findViewById(R.id.detail_search_result);

        resultContainer.setOnItemClickListener(this);

        searchBack = (ImageView) findViewById(R.id.search_back);
        searchBack.setOnClickListener(this);

        emptyView = (TextView) findViewById(R.id.empty_view);

        clearAll = (ImageView) findViewById(R.id.iv_clear_all);
        clearAll.setOnClickListener(this);

        TextView cancel = (TextView) findViewById(tv_search_cancel);
        cancel.setOnClickListener(this);
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
        }
    }

    public void search() {
        if (searchBar != null) {
            String keyword = searchBar.getText().toString().trim();

            if (TextUtils.isEmpty(keyword)) {
                searchResultList.clear();
                clearAll.setVisibility(View.GONE);
                resultContainer.setEmptyView(null);
                emptyView.setVisibility(View.GONE);
                refreshUI();
            } else {
                searchInGroup(keyword.toLowerCase());
                clearAll.setVisibility(View.VISIBLE);
            }
        }
    }

    private void searchInGroup(String keyword) {
        searchResultList.clear();
        for (int i = 0; i < groupMembersInfoList.size(); i++) {
            GroupMember userInfo = groupMembersInfoList.get(i);
            if (!TextUtils.isEmpty(userInfo.remark.remark_name)) {
                if (userInfo.getRemarkName().toLowerCase().contains(keyword) || userInfo.getRemarkSpell().contains(keyword)) {
                    searchResultList.add(userInfo);
                }
            } else if (!TextUtils.isEmpty(userInfo.getGroupNickName())) {
                if (userInfo.getGroupNickName().toLowerCase().contains(keyword) || userInfo.getGroupNickNameSpell().contains(keyword)) {
                    searchResultList.add(userInfo);
                }
            } else if (!TextUtils.isEmpty(userInfo.getName())) {
                if (userInfo.getName().toLowerCase().contains(keyword) || userInfo.getNameSpell().contains(keyword)) {
                    searchResultList.add(userInfo);
                }
            }
        }
        refreshUI();
    }

    private void refreshUI() {
        if (adapter == null) {
            adapter = new SearchGroupMemberResultAdapter();
            resultContainer.setAdapter(adapter);
        } else {
            adapter.notifyDataSetChanged();
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
    public void afterTextChanged(Editable s) {
        search();
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.iv_clear_all) {
            searchBar.setText(null);
            searchResultList.clear();
            refreshUI();
        } else if (v.getId() == R.id.tv_search_cancel) {
            finish();
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

    }

    @Override
    protected void onDestroy() {
        groupMembersInfoList.clear();
        groupMembersInfoList = null;
        super.onDestroy();
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {

    }

    @Override
    protected void initData() {
        super.initData();
    }

    private class SearchGroupMemberResultAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            return searchResultList.size();
        }

        @Override
        public Object getItem(int position) {
            return searchResultList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            UserInfo userInfo = searchResultList.get(position);
            ViewHolder holder;
            if (convertView == null) {
                convertView = View.inflate(BaseSearchUserMemberActivity.this, R.layout.wchat_group_search_result_item, null);
                holder = new ViewHolder();
                holder.iv_avatar = (NetworkImageView) convertView.findViewById(R.id.iv_avatar);
                holder.tv_name = (TextView) convertView.findViewById(R.id.tv_contact_name);
                convertView.setTag(holder);
            }
            holder = (ViewHolder) convertView.getTag();
            holder.tv_name.setText(userInfo.getNameToShow());
            holder.iv_avatar.setDefaultImageResId(R.drawable.gmacs_ic_default_avatar)
                    .setErrorImageResId(R.drawable.gmacs_ic_default_avatar)
                    .setImageUrl(ImageUtil.makeUpUrl(userInfo.getAvatar(), IMG_RESIZE, IMG_RESIZE));
            return convertView;
        }

        private class ViewHolder {
            public NetworkImageView iv_avatar;
            public TextView tv_name;
        }
    }
}
