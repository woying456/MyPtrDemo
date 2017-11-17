package com.android.gmacs.activity;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;

import com.android.gmacs.R;
import com.android.gmacs.event.ContactReportEvent;
import com.android.gmacs.logic.ContactLogic;
import com.common.gmacs.core.GmacsConstant;
import com.common.gmacs.utils.ToastUtil;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

/**
 * Created by zhangxiaoshuang on 2016/1/25.举报联系人
 */
public class GmacsContactReportActivity extends BaseActivity {
    private EditText mEtReportInfo;
    private String reportInfo, userId;
    private int userSource;

    @Override
    protected void initView() {
        setTitle(getText(R.string.report));
        mTitleBar.setRightText(getText(R.string.report_submit));
        mEtReportInfo = (EditText) findViewById(R.id.et_report_info);
        userId = getIntent().getStringExtra(GmacsConstant.EXTRA_USER_ID);
        userSource = getIntent().getIntExtra(GmacsConstant.EXTRA_USER_SOURCE, 0);
        EventBus.getDefault().register(this);
    }

    @Override
    protected void initData() {
        mTitleBar.setRightTextListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                reportInfo = mEtReportInfo.getText().toString().trim();
                if (TextUtils.isEmpty(reportInfo)) {
                    ToastUtil.showToast(R.string.report_submit_error);
                } else {
                    ContactLogic.getInstance().reportUser(userId, userSource, reportInfo);
                }
            }
        });

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.gmacs_activity_contact_report);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onReportSucceed(ContactReportEvent event) {
        ToastUtil.showToast(R.string.report_submit_ok);
        finish();
    }

    @Override
    protected void onDestroy() {
        EventBus.getDefault().unregister(this);
        super.onDestroy();
    }
}

