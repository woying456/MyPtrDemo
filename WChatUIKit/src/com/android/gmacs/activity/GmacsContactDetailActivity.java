package com.android.gmacs.activity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.android.gmacs.R;
import com.android.gmacs.event.AddContactMsgEvent;
import com.android.gmacs.event.RemarkEvent;
import com.android.gmacs.event.StarEvent;
import com.android.gmacs.logic.ContactLogic;
import com.android.gmacs.utils.GmacsUiUtil;
import com.android.gmacs.view.GmacsDialog;
import com.android.gmacs.view.NetworkImageView;
import com.common.gmacs.core.ClientManager;
import com.common.gmacs.core.Gmacs;
import com.common.gmacs.core.GmacsConstant;
import com.common.gmacs.parse.contact.Contact;
import com.common.gmacs.parse.contact.GmacsUser;
import com.common.gmacs.utils.ImageUtil;
import com.common.gmacs.utils.ToastUtil;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import static com.android.gmacs.view.NetworkImageView.IMG_RESIZE;

/**
 * 联系人详细资料
 */
public class GmacsContactDetailActivity extends BaseActivity implements OnClickListener {

    public int talkType;
    protected String userId;
    protected int userSource;
    protected Contact userInfo;
    protected boolean mStar;
    private TextView mTvContactDetailName, mTvContactRemarkContent;
    private TextView mTvContactDetailPhoneNum;
    private TextView mTvContactDetailRename;
    private TextView mTvContactChatbtn;
    private NetworkImageView mIvAvatar;
    private RelativeLayout mRlContactDetailRemarkContentInfo;
    private RelativeLayout mRlContactDetailRemark;
    private LinearLayout mLlContactDetailPhoneAll;
    private View mContactLine;

    @Override
    protected void initView() {
        setTitle(getText(R.string.contact_detail));
        mIvAvatar = (NetworkImageView) findViewById(R.id.iv_avatar);
        mTvContactDetailName = (TextView) findViewById(R.id.tv_contact_detail_name);
        mTvContactDetailRename = (TextView) findViewById(R.id.tv_contact_detail_rename);
        mTvContactChatbtn = (TextView) findViewById(R.id.tv_contact_chatbtn);
        RelativeLayout mLlContactDetailPhone = (RelativeLayout) findViewById(R.id.rl_contact_detail_phone);
        mTvContactDetailPhoneNum = (TextView) findViewById(R.id.tv_contact_detail_phone_num);
        mTvContactRemarkContent = (TextView) findViewById(R.id.tv_contact_remark_content);
        mLlContactDetailPhoneAll = (LinearLayout) findViewById(R.id.ll_contact_detail_phone_all);
        mRlContactDetailRemarkContentInfo = (RelativeLayout) findViewById(R.id.rl_contact_detail_remark_content_info);
        RelativeLayout mRlContactDetailRemarkContent = (RelativeLayout) findViewById(R.id.rl_contact_detail_remark_content);
        mRlContactDetailRemark = (RelativeLayout) findViewById(R.id.rl_contact_detail_remark);
        mContactLine = findViewById(R.id.contact_line);
        if (mTvContactChatbtn != null) {
            mTvContactChatbtn.setOnClickListener(this);
        }
        mLlContactDetailPhone.setOnClickListener(this);
        mRlContactDetailRemarkContent.setOnClickListener(this);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.gmacs_activity_contact_detail);
    }

    @Override
    protected void initData() {
        userId = getIntent().getStringExtra(GmacsConstant.EXTRA_USER_ID);
        userSource = getIntent().getIntExtra(GmacsConstant.EXTRA_USER_SOURCE, 0);
        if (userId == null) {
            mRlContactDetailRemark.setVisibility(View.GONE);
        }
        talkType = getIntent().getIntExtra(GmacsConstant.EXTRA_TALK_TYPE, Gmacs.TalkType.TALKTYPE_NORMAL.getValue());
        EventBus.getDefault().register(this);
        ContactLogic.getInstance().getUserInfo(userId, userSource);
    }

    @Override
    protected void onDestroy() {
        EventBus.getDefault().unregister(this);
        super.onDestroy();
    }

    @Override
    public void onClick(View v) {
        int i = v.getId();
        if (i == R.id.tv_contact_chatbtn) { // 聊天
            Intent intent = GmacsUiUtil.createToChatActivity(this, talkType, userId, userSource);
            if (intent != null) {
                startActivity(intent);
                overridePendingTransition(R.anim.gmacs_push_left_in, R.anim.gmacs_push_left_out);
            }
        } else if (i == R.id.rl_contact_detail_phone) { // 打电话
            Intent intent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + mTvContactDetailPhoneNum.getText()));
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        } else if (i == R.id.rl_contact_detail_remark_content) { // 备注
            Intent intent = new Intent(this, GmacsContactRemarkActivity.class);
            intent.putExtra(GmacsConstant.EXTRA_USER_ID, userId);
            intent.putExtra(GmacsConstant.EXTRA_USER_SOURCE, userSource);
            if (userInfo != null) {
                intent.putExtra(GmacsConstant.EXTRA_REMARK, userInfo.remark);
            }
            startActivity(intent);
        }
    }

    protected void updateTitleBar(final boolean isContact) {
        mTitleBar.setRightImageView(R.drawable.gmacs_ic_contact_edit);
        mTitleBar.setSubRightImageView(isContact ? (mStar ? R.drawable.gmacs_ic_stars : R.drawable.gmacs_ic_unstars) : 0);
        mTitleBar.setRightImageViewListener(new OnClickListener() {//编辑
            @Override
            public void onClick(View v) {
                final GmacsDialog.Builder dialog = new GmacsDialog.Builder(v.getContext(), GmacsDialog.Builder.DIALOG_TYPE_LIST_NO_BUTTON);
                if (isContact) {
                    dialog.initDialog(new AdapterView.OnItemClickListener() {
                        Intent intent;

                        @Override
                        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                            switch (position) {
                                case 0: // 备注
                                    intent = new Intent(GmacsContactDetailActivity.this, GmacsContactRemarkActivity.class);
                                    intent.putExtra(GmacsConstant.EXTRA_USER_ID, userId);
                                    intent.putExtra(GmacsConstant.EXTRA_USER_SOURCE, userSource);
                                    if (userInfo != null) {
                                        intent.putExtra("remark", userInfo.remark);
                                    }
                                    startActivity(intent);
                                    dialog.dismiss();
                                    break;
                                case 1: // 举报
                                    intent = new Intent(GmacsContactDetailActivity.this, GmacsContactReportActivity.class);
                                    intent.putExtra(GmacsConstant.EXTRA_USER_ID, userId);
                                    intent.putExtra(GmacsConstant.EXTRA_USER_SOURCE, userSource);
                                    startActivity(intent);
                                    dialog.dismiss();
                                    break;
                                case 2: // 删除
                                    ContactLogic.getInstance().delContact(userId, userSource);
                                    dialog.dismiss();
                                    finish();
                                    break;
                            }
                        }
                    }).setListTexts(new String[]{
                            getString(R.string.edit_remark),
                            getString(R.string.report),
                            getString(R.string.delete_contact)}).create().show();
                } else {
                    dialog.initDialog(new AdapterView.OnItemClickListener() {
                        @Override
                        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                            switch (position) {
                                case 0: // 添加
                                    ContactLogic.getInstance().requestContact(userId, userSource, "", "");
                                    dialog.dismiss();
                                    break;
                            }
                        }
                    }).setListTexts(new String[]{getString(R.string.add_contact)}).create().show();
                }
            }
        });
        mTitleBar.setSubRightImageViewListener(new OnClickListener() { // 星标
            @Override
            public void onClick(View v) {
                if (ClientManager.getInstance().getConnectionStatus() == GmacsConstant.STATUS_CONNECTED) {
                    if (mStar) {
                        ContactLogic.getInstance().unStar(userId, userSource);
                    } else {
                        ContactLogic.getInstance().star(userId, userSource);
                    }
                } else {
                    ToastUtil.showToast(R.string.connection_error_or_kickedoff);
                }
            }
        });
    }

    private void updateRemarkInfo() {
        if (TextUtils.isEmpty(userInfo.remark.remark_telephone) && TextUtils.isEmpty(userInfo.remark.remark_info)) {
            mRlContactDetailRemark.setVisibility(View.GONE);
        } else {
            mRlContactDetailRemark.setVisibility(View.VISIBLE);
        }
        // 备注名字
        if (!TextUtils.isEmpty(userInfo.remark.remark_name)) {
            mTvContactDetailRename.setVisibility(View.VISIBLE);
            mTvContactDetailRename.setText(userInfo.remark.remark_name);
        } else {
            mTvContactDetailRename.setVisibility(View.GONE);
        }
        // 备注电话
        if (!TextUtils.isEmpty(userInfo.remark.remark_telephone)) {
            mLlContactDetailPhoneAll.setVisibility(View.VISIBLE);
            mTvContactDetailPhoneNum.setText(userInfo.remark.remark_telephone);
        } else {
            mLlContactDetailPhoneAll.setVisibility(View.GONE);
        }
        // 备注信息
        if (!TextUtils.isEmpty(userInfo.remark.remark_info)) {
            mRlContactDetailRemarkContentInfo.setVisibility(View.VISIBLE);
            mTvContactRemarkContent.setText(userInfo.remark.remark_info);
            mContactLine.setVisibility(TextUtils.isEmpty(userInfo.remark.remark_telephone) ? View.GONE : View.VISIBLE);
        } else {
            mRlContactDetailRemarkContentInfo.setVisibility(View.GONE);
            mContactLine.setVisibility(View.GONE);
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onStarEvent(StarEvent event) {
        if (userId.equals(event.getUserId()) && userSource == event.getUserSource()) {
            if (event.isStar()) {
                ToastUtil.showToast(R.string.starred_ok);
                mTitleBar.setSubRightImageView(R.drawable.gmacs_ic_stars);
                mStar = true;
            } else {
                ToastUtil.showToast(R.string.unstarred_ok);
                mTitleBar.setSubRightImageView(R.drawable.gmacs_ic_unstars);
                mStar = false;
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onGetOtherUserInfo(Contact contact) {
        if (userId.equals(contact.getId())
                && userSource == contact.getSource()) {
            userInfo = contact;
            mStar = userInfo.isStar();
            String showName = userInfo.getName();
            mTvContactDetailName.setText(showName);
            mIvAvatar.setDefaultImageResId(R.drawable.gmacs_ic_default_avatar)
                    .setErrorImageResId(R.drawable.gmacs_ic_default_avatar)
                    .setImageUrl(ImageUtil.makeUpUrl(userInfo.getAvatar(), IMG_RESIZE, IMG_RESIZE));

            if (!userId.equals(GmacsUser.getInstance().getUserId())
                    || userSource != GmacsUser.getInstance().getSource()) {
                updateTitleBar(userInfo.isContact());
                mTvContactChatbtn.setVisibility(View.VISIBLE);
            } else {
                mTitleBar.setSubRightImageView(0);
                mTitleBar.setRightImageView(0);
                mTvContactChatbtn.setVisibility(View.GONE);
            }

            if (Gmacs.TalkType.TALKTYPE_OFFICIAL.getValue() == talkType) {
                mTvContactDetailRename.setVisibility(View.GONE);
                mRlContactDetailRemark.setVisibility(View.GONE);
            } else {
                updateRemarkInfo();
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onRemark(RemarkEvent event) {
        if (userId.equals(event.getUserId()) && userSource == event.getUserSource()) {
            if (Gmacs.TalkType.TALKTYPE_OFFICIAL.getValue() != talkType) {
                /*
                  This remark.remark_spell is invalid!
                 */
                userInfo.remark = event.getRemark();
                userInfo.remark.remark_spell = "";
                updateRemarkInfo();
            }
        }
    }

    /**
     * Get the valid remark_spell.
     */
    /*
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onContactsChanged(ContactsEvent event) {
        if (event != null && event.getContactList() != null && event.getContactList().size() > 0) {
            ArrayList<Contact> contactsList = new ArrayList<>(event.getContactList());
            for (Contact contact : contactsList) {
                if (userId.equals(contact.getId()) && userSource == contact.getSource()) {
                    userInfo.remark = contact.remark;
                    updateRemarkInfo();
                }
            }
        }
    }
    */
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onAddContactMsg(AddContactMsgEvent event) {
        if (event.getAcceptFriendMessage() == null) {
            ToastUtil.showToast(R.string.add_contact_sent);
        } else {
            updateTitleBar(true);
            mTitleBar.setSubRightImageView(R.drawable.gmacs_ic_unstars);
        }
    }
}

