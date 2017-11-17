package com.android.gmacs.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.android.gmacs.R;
import com.android.gmacs.view.GmacsDialog;
import com.common.gmacs.core.ClientManager;
import com.common.gmacs.core.GmacsConstant;
import com.common.gmacs.core.GroupManager;
import com.common.gmacs.parse.contact.GroupMember;
import com.common.gmacs.utils.GmacsEnvi;
import com.common.gmacs.utils.GmacsUtils;
import com.common.gmacs.utils.ToastUtil;

public class SearchUserMemberActivity extends BaseSearchUserMemberActivity {

    public static final String OPERATION_CHANGING_OWNER = "CHANGING_OWNER";
    public static final String OPERATION_AT = "AT";

    private GmacsDialog.Builder mConfirmDialog = null;
    private TextView tv_message = null;
    private String operation;

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
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle("搜索群成员");
        ActionBar actionBar = getSupportActionBar();
        if (null != actionBar) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
        Intent intent = getIntent();
        if (null != intent) {
            operation = intent.getStringExtra("operation");
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        if (!TextUtils.isEmpty(operation) && operation.equals(OPERATION_CHANGING_OWNER)) {
            final GroupMember member = searchResultList.get(position);
            if (tv_message != null) {
                tv_message.setText("确定选择" + member.getNameToShow() + "为新群主，你将自动放弃群主身份");
            }
            if (mConfirmDialog == null) {
                LinearLayout linearLayout = (LinearLayout) LayoutInflater.from(this).inflate(R.layout.wchat_group_authoritytransfer_dialog, null);
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams((int) (GmacsEnvi.screenWidth * 0.8), LinearLayout.LayoutParams.WRAP_CONTENT);
                params.gravity = Gravity.CENTER;
                linearLayout.setLayoutParams(params);

                View v = linearLayout.findViewById(R.id.separate);
                LinearLayout.LayoutParams vp = (LinearLayout.LayoutParams) v.getLayoutParams();
                vp.width = GmacsUtils.dipToPixel(1);
                vp.height = v.getHeight() + GmacsUtils.dipToPixel(30);
                v.setLayoutParams(vp);

                tv_message = (TextView) linearLayout.findViewById(R.id.tvMsg);
                tv_message.setText("确定选择" + member.getNameToShow() + "为新群主，你将自动放弃群主身份");

                TextView cancel = (TextView) linearLayout.findViewById(R.id.tvCancel);
                TextView ok = (TextView) linearLayout.findViewById(R.id.tvOk);

                cancel.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mConfirmDialog.dismiss();
                    }
                });
                ok.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mConfirmDialog.dismiss();
                        mConfirmDialog = null;

                        GroupManager.transferGroupAuthority(info.getId(), info.getSource(), member.getId(), member.getSource(), new ClientManager.CallBack() {
                            @Override
                            public void done(int errorCode, String errorMessage) {
                                if (errorCode == 0) {
                                    ToastUtil.showToast("转让成功");
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            setResult(Activity.RESULT_OK, new Intent());
                                            SearchUserMemberActivity.this.onBackPressed();
                                        }
                                    });
                                } else {
                                    ToastUtil.showToast("转让失败");
                                }
                            }
                        });
                    }
                });

                mConfirmDialog = new GmacsDialog.Builder(this, GmacsDialog.Builder.DIALOG_TYPE_CUSTOM_CONTENT_VIEW)
                        .initDialog(linearLayout).setCancelable(false);
                mConfirmDialog.create();
            }
            if (!mConfirmDialog.isShowing()) {
                mConfirmDialog.show();
            }
        } else {
            GroupMember groupMember = searchResultList.get(position);
            Intent intent = new Intent();
            intent.putExtra(GmacsConstant.EXTRA_NAME, groupMember.getNameToShow());
            intent.putExtra(GmacsConstant.EXTRA_USER_ID, groupMember.getId());
            intent.putExtra(GmacsConstant.EXTRA_USER_SOURCE, groupMember.getSource());
            intent.putExtra("realName", TextUtils.isEmpty(groupMember.getGroupNickName())
                    ? groupMember.getName() : groupMember.getGroupNickName());
            setResult(Activity.RESULT_OK, intent);
            onBackPressed();
        }
    }

}
