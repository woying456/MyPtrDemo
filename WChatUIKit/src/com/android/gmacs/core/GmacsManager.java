package com.android.gmacs.core;

import com.android.gmacs.event.GetCaptchaEvent;
import com.android.gmacs.event.GetUserOnlineInfoEvent;
import com.android.gmacs.event.ValidateCaptchaEvent;
import com.android.gmacs.logic.CommandLogic;
import com.android.gmacs.logic.ContactLogic;
import com.android.gmacs.logic.MessageLogic;
import com.android.gmacs.logic.TalkLogic;
import com.common.gmacs.core.ClientManager;
import com.common.gmacs.core.ContactsManager;
import com.common.gmacs.parse.captcha.Captcha;
import com.common.gmacs.parse.contact.GmacsUser;
import com.common.gmacs.parse.contact.UserOnlineInfo;
import com.common.gmacs.parse.message.Message;
import com.common.gmacs.utils.GLog;
import com.common.gmacs.utils.ToastUtil;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

public class GmacsManager {
    private final String TAG = GmacsManager.class.getSimpleName();

    private GmacsManager() {
    }

    public static GmacsManager getInstance() {
        return LazyHolder.INSTANCE;
    }

    /**
     * 启动Gmacs
     *
     * @param notifyHelper
     */
    public void startGmacs(MessageLogic.NotifyHelper notifyHelper) {
        GLog.i(TAG, "startGmacs");
        TalkLogic.getInstance().init();
        ContactLogic.getInstance().init();
        MessageLogic.getInstance().init();
        CommandLogic.getInstance().init();
        if (notifyHelper != null) {
            MessageLogic.getInstance().setNotifyHelper(notifyHelper);
        }
        EventBus.getDefault().register(this);
    }

    /**
     * 停止Gmacs
     */
    public void stopGmacs() {
        GLog.i(TAG, "stopGmacs");
        ContactsManager.getInstance().unRegisterUserInfoChange(GmacsUser.getInstance().getUserId(), GmacsUser.getInstance().getSource(), ContactLogic.getInstance());
        ClientManager.getInstance().cleanup();
        TalkLogic.getInstance().destroy();
        ContactLogic.getInstance().destroy();
        MessageLogic.getInstance().destroy();
        CommandLogic.getInstance().destroy();
    }

    /**
     * 灌水主动拉取验证码
     *
     * @param message
     */
    public void getCaptcha(final Message message) {
        ClientManager.getInstance().getCaptcha(new ClientManager.GetCaptchaCb() {
            @Override
            public void onGetCaptcha(int errorCode, String errorMessage, Captcha captcha) {
                if (errorCode == 0) {
                    EventBus.getDefault().post(new GetCaptchaEvent(captcha, message));
                } else {
                    EventBus.getDefault().post(new GetCaptchaEvent(null, message));
                }
            }
        });
    }

    /**
     * 灌水手动获取验证码
     *
     * @param message
     */
    public void updateCaptcha(final Message message) {
        ClientManager.getInstance().updateCaptcha(new ClientManager.UpdateCaptchaCb() {
            @Override
            public void onUpdateCaptcha(int errorCode, String errorMessage, Captcha captcha) {
                if (errorCode == 0) {
                    EventBus.getDefault().post(new GetCaptchaEvent(captcha, message));
                } else {
                    EventBus.getDefault().post(new GetCaptchaEvent(null, message));
                }
            }
        });
    }

    public void validateCaptcha(String responseId, String captchaContent, final Message message) {
        ClientManager.getInstance().validateCaptcha(responseId, captchaContent,
                new ClientManager.CallBack() {
                    @Override
                    public void done(int errorCode, String errorMessage) {
                        if (errorCode == 0) {
                            EventBus.getDefault().post(new ValidateCaptchaEvent(true, message));
                        } else {
                            EventBus.getDefault().post(new ValidateCaptchaEvent(false, message));
                        }
                    }
                });

    }

    public void getUserOnlineInfo(final String userId, final int userSource) {
        ClientManager.getInstance().getUserOnlineInfo(userId, userSource,
                new ClientManager.GetUserOnlineInfoCb() {
                    @Override
                    public void onGetUserOnlineInfo(int errorCode, String errorMessage, UserOnlineInfo onlineInfo) {
                        if (errorCode == 0) {
                            EventBus.getDefault().post(new GetUserOnlineInfoEvent(userId, userSource, onlineInfo));
                        }
                    }
                });
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onError(String errorMessage) {
        ToastUtil.showToast(errorMessage);
    }

    private static class LazyHolder {
        private static final GmacsManager INSTANCE = new GmacsManager();
    }
}

