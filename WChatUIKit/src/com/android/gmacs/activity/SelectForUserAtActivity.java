package com.android.gmacs.activity;

import android.content.Intent;
import android.text.TextUtils;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.gmacs.R;
import com.common.gmacs.core.GmacsConstant;
import com.common.gmacs.parse.contact.Group;
import com.common.gmacs.parse.contact.GroupMember;
import com.common.gmacs.parse.contact.UserInfo;

public class SelectForUserAtActivity extends BaseSelectUserMemberActivity {

    public final static int SELECT_FOR_GROUP_AT_REQUEST_CODE = 1025;

    @Override
    protected void updateUI() {
        setTitle("选择联系人");
        super.updateUI();
    }

    @Override
    protected void initView() {
        super.initView();
        if (info instanceof Group) {
            if (((Group) info).getSelfInfo().getAuthority() == GroupMember.AUTHORITY_OWNER) {
                View atAll = getLayoutInflater().inflate(R.layout.gmacs_new_friends, mHeaderContainer);
                ((ImageView) atAll.findViewById(R.id.image)).setImageResource(R.drawable.wchat_ic_group_at_all);
                ((TextView) atAll.findViewById(R.id.text)).setText("全部成员");
            }
        }
    }

    @Override
    protected void setSearchBarClickListener() {
        searchBarClickListener = new SearchBarClickListener() {
            @Override
            public void onClick() {
                if (info instanceof Group) {
                    Intent intent = new Intent(SelectForUserAtActivity.this, SearchUserMemberActivity.class);
                    intent.putExtra(GmacsConstant.EXTRA_USER_ID, info.getId());
                    intent.putExtra(GmacsConstant.EXTRA_USER_SOURCE, info.getSource());
                    intent.putExtra("operation", SearchUserMemberActivity.OPERATION_AT);
                    startActivityForResult(intent, SELECT_FOR_GROUP_AT_REQUEST_CODE);
                }
            }
        };
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && requestCode == SELECT_FOR_GROUP_AT_REQUEST_CODE) {
            setResult(RESULT_OK, data);
            onBackPressed();
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        if (info instanceof Group) {
            if (((Group) info).getSelfInfo().getAuthority() == GroupMember.AUTHORITY_OWNER ? position > 1 : position >= 0) {
                UserInfo member = groupMembersInfoList.get(position - mGroupMemberListView.getHeaderViewsCount());
                Intent intent = new Intent();
                intent.putExtra(GmacsConstant.EXTRA_NAME, member.getNameToShow());
                intent.putExtra(GmacsConstant.EXTRA_USER_ID, member.getId());
                intent.putExtra(GmacsConstant.EXTRA_USER_SOURCE, member.getSource());
                intent.putExtra("realName", TextUtils.isEmpty(((GroupMember) member).getGroupNickName())
                        ? member.getName() : ((GroupMember) member).getGroupNickName());
                setResult(RESULT_OK, intent);
            } else {
                Intent from = getIntent();
                Intent to = null;
                if (from != null) {
                    to = new Intent();
                    to.putExtra(GmacsConstant.EXTRA_NAME, "所有人");
                    to.putExtra(GmacsConstant.EXTRA_USER_ID, from.getStringExtra(GmacsConstant.EXTRA_USER_ID));
                    to.putExtra(GmacsConstant.EXTRA_USER_SOURCE, from.getIntExtra(GmacsConstant.EXTRA_USER_SOURCE, -1));
                    to.putExtra("realName", "所有人");
                }
                setResult(RESULT_OK, to);
            }
        }
        onBackPressed();
    }

    public void onTitleClick(View view) {
        onBackPressed();
    }

//    @Override
//    public void onBackPressed() {
//        super.onBackPressed();
//        overridePendingTransition(0, R.anim.gmacs_slide_out_to_bottom);
//    }
}
