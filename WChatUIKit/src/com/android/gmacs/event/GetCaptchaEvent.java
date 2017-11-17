package com.android.gmacs.event;

import com.common.gmacs.parse.captcha.Captcha;
import com.common.gmacs.parse.message.Message;

public class GetCaptchaEvent {
    private Captcha captcha;
    private Message message;

    public GetCaptchaEvent(Captcha captcha, Message message) {
        this.captcha = captcha;
        this.message = message;
    }

    public Captcha getCaptcha() {
        return captcha;
    }

    public Message getMessage() {
        return message;
    }
}
