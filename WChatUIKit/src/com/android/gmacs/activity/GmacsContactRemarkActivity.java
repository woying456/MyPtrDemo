package com.android.gmacs.activity;

import android.os.Bundle;
import android.text.InputFilter;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;

import com.android.gmacs.R;
import com.android.gmacs.event.RemarkEvent;
import com.android.gmacs.logic.ContactLogic;
import com.android.gmacs.utils.InputLengthFilter;
import com.common.gmacs.core.ClientManager;
import com.common.gmacs.core.GmacsConstant;
import com.common.gmacs.parse.contact.Remark;
import com.common.gmacs.utils.ToastUtil;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

/**
 * Created by zhangxiaoshuang on 2015/12/24.联系人添加备注
 */
public class GmacsContactRemarkActivity extends BaseActivity {

    private EditText mRemarkName;
    private String userId;
    private int userSource;
    private Remark remark;
    private boolean isFriend;

    @Override
    protected void initView() {
        setTitle(getText(R.string.remark));
        mTitleBar.setRightText(getText(R.string.save));
        mRemarkName = (EditText) findViewById(R.id.et_remark_name);
        userId = getIntent().getStringExtra(GmacsConstant.EXTRA_USER_ID);
        userSource = getIntent().getIntExtra(GmacsConstant.EXTRA_USER_SOURCE, 0);
        remark = getIntent().getParcelableExtra(GmacsConstant.EXTRA_REMARK);
        isFriend = getIntent().getBooleanExtra(GmacsConstant.EXTRA_IS_FRIEND, false);
        if (TextUtils.isEmpty(userId)) {
            finish();
            return;
        }
        mRemarkName.setFilters(new InputFilter[]{new InputLengthFilter(30)});
        if (remark != null) {
            mRemarkName.setText(remark.remark_name);
            try {
                mRemarkName.setSelection(remark.remark_name == null ? 0 : remark.remark_name.length());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        mTitleBar.setRightTextListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (ClientManager.getInstance().getConnectionStatus() == GmacsConstant.STATUS_CONNECTED) {
                    if (remark == null) {
                        remark = new Remark();
                    }
                    remark.remark_name = mRemarkName.getText().toString().trim();
                    ContactLogic.getInstance().remark(userId, userSource, isFriend, remark.remark_name, remark);
                } else {
                    ToastUtil.showToast(R.string.connection_error_or_kickedoff);
                }
            }
        });
    }

    @Override
    protected void initData() {
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.gmacs_activity_contact_remark);
        EventBus.getDefault().register(this);
    }

    @Override
    protected void onDestroy() {
        EventBus.getDefault().unregister(this);
        super.onDestroy();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onRemark(RemarkEvent event) {
        finish();
    }
}
