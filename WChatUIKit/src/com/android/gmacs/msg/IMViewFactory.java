package com.android.gmacs.msg;


import com.android.gmacs.msg.view.IMAudioMsgView;
import com.android.gmacs.msg.view.IMCallMsgView;
import com.android.gmacs.msg.view.IMGroupNotificationMsgView;
import com.android.gmacs.msg.view.IMImageMsgView;
import com.android.gmacs.msg.view.IMLocationMsgView;
import com.android.gmacs.msg.view.IMMessageView;
import com.android.gmacs.msg.view.IMTextMsgView;
import com.android.gmacs.msg.view.IMTipMsgView;
import com.android.gmacs.msg.view.IMUniversalCard3MsgView;
import com.android.gmacs.msg.view.IMUniversalCard4MsgView;
import com.android.gmacs.msg.view.IMUniversalCard1MsgView;
import com.android.gmacs.msg.view.IMUniversalCard2MsgView;
import com.common.gmacs.msg.IMMessage;
import com.common.gmacs.msg.MsgContentType;

/**
 * MsgView工厂
 * 根据消息类型创建相应的对象
 */

public class IMViewFactory {

    public IMMessageView createItemView(IMMessage msg) {
        IMMessageView resultView = null;
        switch (msg.showType) {
            case MsgContentType.TYPE_TEXT:
                resultView = new IMTextMsgView(msg);
                break;
            case MsgContentType.TYPE_IMAGE:
                resultView = new IMImageMsgView(msg);
                break;
            case MsgContentType.TYPE_AUDIO:
                resultView = new IMAudioMsgView(msg);
                break;
            case MsgContentType.TYPE_LOCATION:
                resultView = new IMLocationMsgView(msg);
                break;
            case MsgContentType.TYPE_TIP:
                resultView = new IMTipMsgView(msg);
                break;
            case MsgContentType.TYPE_GROUP_NOTIFICATION:
                resultView = new IMGroupNotificationMsgView(msg);
                break;
            case MsgContentType.TYPE_CALL:
                resultView = new IMCallMsgView(msg);
                break;
            case MsgContentType.TYPE_UNIVERSAL_CARD1:
                resultView = new IMUniversalCard1MsgView(msg);
                break;
            case MsgContentType.TYPE_UNIVERSAL_CARD2:
                resultView = new IMUniversalCard2MsgView(msg);
                break;
            case MsgContentType.TYPE_UNIVERSAL_CARD3:
                resultView = new IMUniversalCard3MsgView(msg);
                break;
            case MsgContentType.TYPE_UNIVERSAL_CARD4:
                resultView = new IMUniversalCard4MsgView(msg);
                break;
        }
        return resultView;
    }

}
