package com.android.gmacs.logic;

import com.android.gmacs.event.KickedOutOfGroupEvent;
import com.common.gmacs.core.CommandManager;
import com.common.gmacs.core.Gmacs;
import com.common.gmacs.core.MessageManager;
import com.common.gmacs.msg.MsgContentType;
import com.common.gmacs.msg.data.IMGroupNotificationMsg;
import com.common.gmacs.parse.command.CallCommand;
import com.common.gmacs.parse.command.Command;
import com.common.gmacs.parse.command.EventCommand;
import com.common.gmacs.parse.command.KickedOutOfGroupCommand;
import com.common.gmacs.parse.message.Message;
import com.common.gmacs.utils.GLog;
import com.common.gmacs.utils.GmacsUtils;

import org.greenrobot.eventbus.EventBus;


public class CommandLogic extends BaseLogic implements CommandManager.OnReceivedCommandListener {

    private volatile static CommandLogic ourInstance;
    private WRTCProxy mWRTCProxy;
    private String mWRTCExtend;

    private CommandLogic() {
    }

    public static CommandLogic getInstance() {
        if (null == ourInstance) {
            synchronized (CommandLogic.class) {
                if (null == ourInstance) {
                    ourInstance = new CommandLogic();
                }
            }
        }
        return ourInstance;
    }

    public String getmWRTCExtend() {
        return mWRTCExtend;
    }

    public void setmWRTCExtend(String mWRTCExtend) {
        this.mWRTCExtend = mWRTCExtend;
    }

    public void setWRTCProxy(WRTCProxy mWRTCProxy) {
        this.mWRTCProxy = mWRTCProxy;
    }

    @Override
    public void init() {
        CommandManager.getInstance().registerOnReceivedCommandListener(this);
    }

    @Override
    public void destroy() {
    }

    public void startCall(final CallCommand callCommand) {
        if (mWRTCProxy != null) {
            GmacsUtils.getInstance().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mWRTCProxy.startCall(callCommand);
                }
            });
        }
    }

    public int getChattingType() {
        if (mWRTCProxy == null) {
            return -1;
        } else {
            return mWRTCProxy.getChattingType();
        }
    }

    public void finishCall() {
        if (isChatting()) {
            if (mWRTCProxy != null) {
                GmacsUtils.getInstance().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mWRTCProxy.finishCall();
                    }
                });
            }
        }
    }

    public boolean isChatting() {
        return getChattingType() >= 0;
    }

    @Override
    public void onReceivedCommand(Command command) {
        GLog.d(TAG, command + "");
        if (command instanceof CallCommand) {
            if (mWRTCProxy != null) {
                mWRTCProxy.onReceivedCall((CallCommand) command);
            }
        } else if (command instanceof KickedOutOfGroupCommand) {
            EventBus.getDefault().post(new KickedOutOfGroupEvent((KickedOutOfGroupCommand) command));

            Message.MessageUserInfo receiverInfo = new Message.MessageUserInfo();
            receiverInfo.mUserId = ((KickedOutOfGroupCommand) command).getOperatedGroupId();
            receiverInfo.mUserSource = ((KickedOutOfGroupCommand) command).getOperatedGroupSource();
            receiverInfo.mDeviceId = "";

            IMGroupNotificationMsg tipMessage = new IMGroupNotificationMsg();
            tipMessage.text = "您已被" + ((KickedOutOfGroupCommand) command).getOperatorName() + "踢出群";
            tipMessage.extra = "";
            tipMessage.showType = MsgContentType.TYPE_GROUP_NOTIFICATION;

            MessageManager.getInstance().insertLocalMessage(
                    Gmacs.TalkType.TALKTYPE_GROUP.getValue(),
                    Message.MessageUserInfo.createLoginUserInfo(),
                    receiverInfo, "", tipMessage, false, true, true,
                    new MessageManager.InsertLocalMessageCb() {
                        @Override
                        public void onInsertLocalMessage(int errorCode, String errorMessage, Message message) {
                            EventBus.getDefault().post(message);
                        }
                    });
        } else if (command instanceof EventCommand) {
            if (mWRTCProxy != null) {
                mWRTCProxy.updateCallState((EventCommand) command);
            }
        }
    }

    public interface WRTCProxy {
        void startCall(CallCommand callCommand);

        int getChattingType();

        void onReceivedCall(CallCommand callCommand);

        void updateCallState(EventCommand eventCommand);

        void finishCall();
    }
}