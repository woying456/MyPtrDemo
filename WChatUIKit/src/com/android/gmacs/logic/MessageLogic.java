package com.android.gmacs.logic;

import android.app.Notification;
import android.os.Handler;
import android.os.HandlerThread;

import com.android.gmacs.event.FriendUnreadCountEvent;
import com.android.gmacs.event.LoadAddFriendRequestsEvent;
import com.common.gmacs.core.ClientManager;
import com.common.gmacs.core.GmacsConstant;
import com.common.gmacs.core.MessageManager;
import com.common.gmacs.core.RecentTalkManager;
import com.common.gmacs.msg.data.IMImageMsg;
import com.common.gmacs.parse.message.Message;
import com.common.gmacs.parse.talk.Talk;

import org.greenrobot.eventbus.EventBus;

import java.util.List;

public class MessageLogic extends BaseLogic implements MessageManager.ReceiveMsgListener, MessageManager.SendIMMsgListener, IMImageMsg.UploadImageListener {

    private static volatile MessageLogic instance;
    private NotifyHelper notifyHelper;
    private MessageManager.SendIMMsgListener sendMessageListener;

    private MessageLogic() {
    }

    public static MessageLogic getInstance() {
        if (null == instance) {
            synchronized (MessageLogic.class) {
                if (null == instance) {
                    instance = new MessageLogic();
                }
            }
        }
        return instance;
    }

    public void getRequestFriendMessages(long lastMsgLocalId, int count) {
        MessageManager.getInstance().getHistoryAsync("SYSTEM_FRIEND", 1999, lastMsgLocalId, count, new MessageManager.GetHistoryMsgCb() {
            @Override
            public void done(int errorCode, String errorMessage, List<Message> msgList) {
                EventBus.getDefault().post(new LoadAddFriendRequestsEvent(msgList));
                if (errorCode != 0) {
                    EventBus.getDefault().post(errorMessage);
                }
            }
        });
    }

    /**
     * 删除消息
     */
    public void deleteMsgByLocalId(String otherId, int otherSource, long localId) {
        MessageManager.getInstance().deleteMsgByLocalIdAsync(otherId, otherSource, localId, new ClientManager.CallBack() {
            @Override
            public void done(int errorCode, String errorMessage) {
                if (errorCode != 0) {
                    EventBus.getDefault().post(errorMessage);
                }
            }
        });
    }

    /**
     * 更新消息播放状态
     */
    public void updatePlayStatusByLocalId(String otherId, int otherSource, long localId, int newStatus) {
        MessageManager.getInstance().updatePlayStatusBatchByLocalIdAsync(otherId, otherSource, new long[]{localId}, newStatus, true, null);
    }

    /**
     * 新的朋友未读消息数
     */
    public void getUnreadFriendCount() {
        RecentTalkManager.getInstance().getTalkByIdAsync("SYSTEM_FRIEND", 1999, new RecentTalkManager.GetTalkByIdCb() {
            @Override
            public void done(int errorCode, String errorMessage, Talk talk) {
                long count = 0;
                if (errorCode == 0 && talk != null) {
                    count = talk.mNoReadMsgCount;
                }
                EventBus.getDefault().post(new FriendUnreadCountEvent(count));
            }
        });
    }


    @Override
    public void init() {
        MessageManager.getInstance().regReceiveMsgListener(this);
    }

    @Override
    public void destroy() {
    }

    @Override
    public void msgReceived(Message msg) {
        EventBus.getDefault().post(msg);
        if (msg.getMsgContent().isNotice()
                && notifyHelper != null && !msg.isSilent()) {
            Talk talking = RecentTalkManager.getInstance().getTalking();
            // 排除当前正进行的会话
            boolean isNotify = !(talking != null && talking.hasTheSameTalkIdWith(msg)
                    && talking.isTalking());
            boolean read = GmacsConstant.MSG_READ == msg.getReadStatus();
            long loginTime = ClientManager.getInstance().getLoginTimeStamp();
            if (isNotify && !read && msg.mMsgUpdateTime >= loginTime) {
                notifyHelper.showNotify(msg);
            }
        }
    }

    public void setNotifyHelper(NotifyHelper notifyHelper) {
        this.notifyHelper = notifyHelper;
    }

    @Override
    public void onPreSaveMessage(Message message) {
        if (sendMessageListener != null) {
            sendMessageListener.onPreSaveMessage(message);
        }
    }

    @Override
    public void onAfterSaveMessage(Message message, int errorCode, String errorMessage) {
        if (sendMessageListener != null) {
            sendMessageListener.onAfterSaveMessage(message, errorCode, errorMessage);
        }
    }

    @Override
    public void onSendMessageResult(Message message, int errorCode, String errorMessage) {
        if (sendMessageListener != null) {
            sendMessageListener.onSendMessageResult(message, errorCode, errorMessage);
        }
    }

    public void setSendMessageListener(MessageManager.SendIMMsgListener sendMessageListener) {
        this.sendMessageListener = sendMessageListener;
    }

    @Override
    public void onUploading(Message message) {
        if (sendMessageListener != null) {
            sendMessageListener.onSendMessageResult(message, 0, "");
        }
    }

    public abstract static class NotifyHelper {

        HandlerThread handlerThread;
        Handler handler;

        private void showNotify(final Message msg) {
            if (msg == null) {
                return;
            }
            if (msg.getMsgContent() == null) {
                return;
            }
            if (handlerThread == null) {
                handlerThread = new HandlerThread("NotificationDispatcher");
                handlerThread.start();
                handler = new Handler(handlerThread.getLooper()) {
                    @Override
                    public void handleMessage(android.os.Message msg) {
                        super.handleMessage(msg);
                        showMsgNotification((Message) msg.obj);
                    }
                };
            }
            android.os.Message message = handler.obtainMessage();
            message.what = Talk.getTalkId(msg).hashCode();
            message.obj = msg;
            handler.removeMessages(message.what);
            handler.sendMessageDelayed(message, 50);
        }

        /**
         * 显示通知栏具体处理逻辑，具体由子类实现
         *
         * @param message
         */
        protected abstract void showMsgNotification(Message message);

        /**
         * 根据当前的提醒设置获取经过设定好了的notification，如果返回为null，说明当前不应该有通知栏提醒,具体由子类实现
         */
        protected abstract Notification configNotification(Notification notification);

    }
}
