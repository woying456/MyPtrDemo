package com.android.gmacs.activity;

import android.content.Intent;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.android.gmacs.R;
import com.android.gmacs.view.GmacsDialog;
import com.common.gmacs.core.ClientManager;
import com.common.gmacs.core.GmacsConstant;
import com.common.gmacs.core.GroupManager;
import com.common.gmacs.parse.contact.Group;
import com.common.gmacs.parse.contact.UserInfo;
import com.common.gmacs.utils.GmacsEnvi;
import com.common.gmacs.utils.GmacsUtils;
import com.common.gmacs.utils.ToastUtil;

public class SelectForChangingOwnerActivity extends BaseSelectUserMemberActivity {

    public final static int SELECT_FOR_CHANGING_OWNER_REQUEST_CODE = 1024;

    private GmacsDialog.Builder mConfirmDialog;
    private TextView tv_message;

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        final UserInfo member = (UserInfo) parent.getItemAtPosition(position);
        if (member != null) {
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
                        if (info instanceof Group) {
                            GroupManager.transferGroupAuthority(info.getId(), info.getSource(), member.getId(), member.getSource(), new ClientManager.CallBack() {
                                @Override
                                public void done(int errorCode, String errorMessage) {
                                    if (errorCode == 0) {
                                        ToastUtil.showToast("转让成功");
                                        runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                onBackPressed();
                                            }
                                        });
                                    } else {
                                        ToastUtil.showToast("转让失败");
                                    }
                                }
                            });
                        }
                    }
                });
                mConfirmDialog = new GmacsDialog.Builder(this, GmacsDialog.Builder.DIALOG_TYPE_CUSTOM_CONTENT_VIEW)
                        .initDialog(linearLayout).setCancelable(false);
                mConfirmDialog.create();
            }
            if (!mConfirmDialog.isShowing()) {
                mConfirmDialog.show();
            }
        }
    }

    @Override
    protected void setSearchBarClickListener() {
        searchBarClickListener = new SearchBarClickListener() {
            @Override
            public void onClick() {
                if (info instanceof Group) {
                    Intent intent = new Intent(SelectForChangingOwnerActivity.this, SearchUserMemberActivity.class);
                    intent.putExtra(GmacsConstant.EXTRA_USER_ID, info.getId());
                    intent.putExtra(GmacsConstant.EXTRA_USER_SOURCE, info.getSource());
                    intent.putExtra("operation", SearchUserMemberActivity.OPERATION_CHANGING_OWNER);
                    startActivityForResult(intent, SELECT_FOR_CHANGING_OWNER_REQUEST_CODE);
                }
            }
        };
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK && requestCode == SELECT_FOR_CHANGING_OWNER_REQUEST_CODE) {
            onBackPressed();
        }
    }

    @Override
    protected void updateUI() {
        setTitle("选择新群主");
//        tvTitle.setText("选择新群主");
        super.updateUI();
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
