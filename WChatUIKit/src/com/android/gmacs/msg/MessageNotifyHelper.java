package com.android.gmacs.msg;

import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Vibrator;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.text.TextUtils;
import android.widget.ImageView;

import com.android.gmacs.R;
import com.android.gmacs.downloader.RequestManager;
import com.android.gmacs.downloader.VolleyError;
import com.android.gmacs.downloader.image.ImageLoader;
import com.android.gmacs.downloader.image.ImageRequest;
import com.android.gmacs.logic.MessageLogic;
import com.android.gmacs.logic.TalkLogic;
import com.android.gmacs.sound.SoundPlayer;
import com.android.gmacs.utils.GmacsUiUtil;
import com.android.gmacs.utils.MultiImageComposer;
import com.common.gmacs.core.ContactsManager;
import com.common.gmacs.core.GmacsConstant;
import com.common.gmacs.parse.Pair;
import com.common.gmacs.parse.contact.Contact;
import com.common.gmacs.parse.contact.GmacsUser;
import com.common.gmacs.parse.contact.Group;
import com.common.gmacs.parse.contact.GroupMember;
import com.common.gmacs.parse.contact.UserInfo;
import com.common.gmacs.parse.message.Message;
import com.common.gmacs.parse.talk.Talk;
import com.common.gmacs.parse.talk.TalkType;
import com.common.gmacs.utils.GmacsConfig;
import com.common.gmacs.utils.GmacsEnvi;
import com.common.gmacs.utils.GmacsUtils;

import java.util.Calendar;
import java.util.HashSet;
import java.util.List;

import static com.android.gmacs.view.NetworkImageView.IMG_RESIZE;

public class MessageNotifyHelper extends MessageLogic.NotifyHelper {

    private Vibrator vibrator;

    @Override
    protected void showMsgNotification(final Message message) {
        final int notifyId = Talk.getTalkId(message).hashCode();
        final NotificationContainer container = new NotificationContainer(getIntent(message), notifyId);
        fillUserInfo(container, message, new CallBack() {
            @Override
            public void done() {
                container.priority = getPriority();
                generateAvatarBitmap(container, new CallBack() {
                    @Override
                    public void done() {
                        Notification notification = configNotification(container.buildNotification());
                        if (notification != null) {
                            final NotificationManager notificationManager =
                                    (NotificationManager) GmacsEnvi.appContext.getSystemService(Context.NOTIFICATION_SERVICE);
                            notificationManager.notify(notifyId, notification);
                        }
                    }
                });
            }
        });
    }

    @Nullable
    private Intent getIntent(Message message) {
        Intent intent = null;
        try {
            intent = new Intent(GmacsEnvi.appContext, Class.forName(GmacsUiUtil.getAppMainClassName()));
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        if (intent != null) {
            Message.MessageUserInfo talkOtherUserInfo = message.getTalkOtherUserInfo();
            intent.putExtra(GmacsConstant.EXTRA_TALK_TYPE, message.mTalkType);
            intent.putExtra(GmacsConstant.EXTRA_USER_ID, talkOtherUserInfo.mUserId);
            intent.putExtra(GmacsConstant.EXTRA_USER_SOURCE, talkOtherUserInfo.mUserSource);
            intent.putExtra(GmacsConstant.EXTRA_REFER, message.getRefer());
            intent.putExtra(GmacsConstant.EXTRA_FROM_NOTIFY, true);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        }
        return intent;
    }

    private void generateAvatarBitmap(final NotificationContainer container, final CallBack cb) {
        final String[] avatar = container.avatar;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && avatar != null && avatar.length != 0) {
            GmacsUtils.getInstance().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (avatar.length == 1) {
                        RequestManager.getInstance().getImageLoader().get(avatar[0], new ImageLoader.ImageListener() {
                            @Override
                            public void onResponse(ImageLoader.ImageContainer response, boolean isImmediate) {
                                if (response.getBitmap() != null) {
                                    container.bitmap = response.getBitmap();
                                    cb.done();
                                }
                            }

                            @Override
                            public void onErrorResponse(VolleyError error) {
                                cb.done();
                            }
                        }, IMG_RESIZE, IMG_RESIZE, ImageView.ScaleType.CENTER_CROP, ImageRequest.DRAW_SHAPE_CIRCLE, 0);
                    } else {
                        RequestManager.getInstance().getImageLoader().get(avatar, new ImageLoader.MultiImageListener() {
                            @Override
                            public void onResponse(final ImageLoader.MultiImageContainer response, boolean isImmediate) {
                                if (response.getBitmap() != null) {
                                    container.bitmap = response.getBitmap();
                                    cb.done();
                                } else if (!isImmediate) {
                                    cb.done();
                                }
                            }
                        }, new MultiImageComposer(IMG_RESIZE, IMG_RESIZE), IMG_RESIZE, IMG_RESIZE, ImageView.ScaleType.CENTER_CROP, ImageRequest.DRAW_SHAPE_RECT, 0);
                    }
                }
            });
        } else {
            cb.done();
        }
    }

    @Override
    protected Notification configNotification(Notification notification) {
        if (notification == null) {
            return null;
        }

        // 声音、震动提醒方式是否在允许的时间段内
        int startOfTime = 8; //允许收到通知开始时间
        int endOfTime = 22; //允许收到通知结束时间
        boolean openSound = true; //是否开启声音
        boolean openVibrate = true; //是否震动

        Calendar now = Calendar.getInstance();
        Calendar startTime = (Calendar) now.clone();
        startTime.set(Calendar.MINUTE, 0);
        startTime.set(Calendar.HOUR_OF_DAY, startOfTime);
        Calendar endTime = (Calendar) now.clone();
        endTime.set(Calendar.MINUTE, 0);
        endTime.set(Calendar.HOUR_OF_DAY, endOfTime);
        if (now.before(startTime) || now.after(endTime)) {
            openSound = false;
            openVibrate = false;
        }

        notification.defaults = 0;
        if (openVibrate) {
            notification.defaults = Notification.DEFAULT_VIBRATE;
        }
        if (openSound) {
            notification.defaults |= Notification.DEFAULT_SOUND;
        }
        return notification;
    }

    protected boolean isForeground(Context context) {
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        ComponentName cn = am.getRunningTasks(1).get(0).topActivity;
        String currentPackageName = cn.getPackageName();
        return !TextUtils.isEmpty(currentPackageName) && currentPackageName.equals(context.getPackageName());
    }

    protected int getPriority() {
        return isForeground(GmacsEnvi.appContext) ? 0 : 1;
    }

    private void fillUserInfo(final NotificationContainer container, final Message message, final CallBack cb) {
        final Message.MessageUserInfo senderInfo = message.mSenderInfo;
        Message.MessageUserInfo talkOtherUserInfo = message.getTalkOtherUserInfo();
        if (TalkType.isUserRequestTalk(message) &&
                senderInfo.mUserId.equals("SYSTEM_FRIEND") && senderInfo.mUserSource == 1999) {
            container.ticker = "系统消息：您收到一条好友请求";
            container.contentText = container.ticker;
            cb.done();
        } else {
            ContactsManager.getInstance().getUserInfoAsync(talkOtherUserInfo.mUserId,
                    talkOtherUserInfo.mUserSource, new ContactsManager.GetUserInfoCb() {
                        @Override
                        public void done(int errorCode, String errorMessage, final UserInfo userInfo) {
                            if (userInfo instanceof Contact) {
                                container.avatar = new String[]{userInfo.avatar};
                                container.ticker = message.getMsgContent().getPlainText();
                                container.contentText = container.ticker;
                                container.contentTitle = userInfo.getNameToShow();
                                cb.done();
                            } else if (userInfo instanceof Group) {
                                final HashSet<Pair> users = new HashSet<>();
                                //群聊拼头像
                                for (int i = 0; i < ((Group) userInfo).getMembers().size() && i < 4; ++i) {
                                    GroupMember member = ((Group) userInfo).getMembers().get(i);
                                    users.add(new Pair(member.getId(), member.getSource()));
                                }
                                boolean isAtSelf = false;
                                if (message.atInfoArray != null) {
                                    HashSet<Pair> atUsers = new HashSet<>();
                                    for (Message.AtInfo atInfo : message.atInfoArray) {
                                        isAtSelf = atInfo.userSource >= 10000 || TextUtils.equals(atInfo.userId, GmacsUser.getInstance().getUserId())
                                                && atInfo.userSource == GmacsUser.getInstance().getSource();
                                        if (isAtSelf) {
                                            break;
                                        } else {
                                            atUsers.add(new Pair(atInfo.userId, atInfo.userSource));
                                        }
                                    }
                                    if (!isAtSelf && !atUsers.isEmpty()) {
                                        //是@消息，不是@自己，需要获取atInfo中的用户信息
                                        users.addAll(atUsers);
                                    }
                                }

                                //@自己或者展示发送方名称，需要获取senderInfo
                                if (message.isShowSenderName() || isAtSelf) {
                                    users.add(new Pair(senderInfo.mUserId, senderInfo.mUserSource));
                                }

                                final boolean finalIsAtSelf = isAtSelf;
                                String groupName = userInfo.getNameToShow();
                                final String title = TextUtils.isEmpty(groupName) ? "群聊" : groupName;
                                ContactsManager.getInstance().getUserInfoBatchAsync(users, new ContactsManager.UserInfoBatchCb() {
                                    @Override
                                    public void onGetUserInfoBatch(int errorCode, String errorMessage, List<UserInfo> userInfoList) {
                                        if (userInfoList != null) {
                                            String senderName = "";
                                            for (UserInfo info : userInfoList) {
                                                GroupMember hitGroupMember = null;
                                                boolean needSenderInfo = finalIsAtSelf || message.isShowSenderName();
                                                for (int i = 0; i < ((Group) userInfo).getMembers().size() && (needSenderInfo || i < 4); ++i) {
                                                    GroupMember member = ((Group) userInfo).getMembers().get(i);
                                                    if (TextUtils.equals(info.getId(), member.getId()) && info.getSource() == member.getSource()) {
                                                        member.updateFromContact((Contact) info);
                                                        hitGroupMember = member;
                                                        break;
                                                    }
                                                }
                                                if (needSenderInfo) {
                                                    if (TextUtils.equals(info.getId(), senderInfo.mUserId) && info.getSource() == senderInfo.mUserSource) {
                                                        if (hitGroupMember == null) {
                                                            senderName = info.getNameToShow();
                                                        } else {
                                                            senderName = hitGroupMember.getNameToShow();
                                                        }
                                                    }
                                                }
                                            }
                                            if (finalIsAtSelf) {
                                                if (TextUtils.isEmpty(senderName)) {
                                                    container.ticker = "有人在群聊@了你";
                                                } else {
                                                    container.ticker = senderName + "在群聊@了你";
                                                }
                                            } else {
                                                StringBuilder stringBuilder = new StringBuilder(message.getMsgContent().getPlainText());
                                                if (message.atInfoArray != null) {
                                                    for (Message.AtInfo atInfo : message.atInfoArray) {
                                                        for (GroupMember member : ((Group) userInfo).getMembers()) {
                                                            if (atInfo.userSource == member.getSource()
                                                                    && TextUtils.equals(atInfo.userId, member.getId())) {
                                                                String realName = member.getNameToShow();
                                                                if (!TextUtils.isEmpty(realName) && atInfo.startPosition >= 0 && atInfo.endPosition <= stringBuilder.length() && atInfo.startPosition < atInfo.endPosition) {
                                                                    stringBuilder.replace(atInfo.startPosition, atInfo.endPosition, "@" + realName + " ");
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                                //需要显示发送方名称
                                                if (message.isShowSenderName() && !TextUtils.isEmpty(senderName)) {
                                                    stringBuilder.insert(0, "：").insert(0, senderName);
                                                }

                                                container.ticker = stringBuilder.toString();
                                            }
                                            container.contentText = container.ticker;
                                            container.contentTitle = title;
                                            if (!TextUtils.isEmpty(userInfo.avatar)) {
                                                container.avatar = new String[]{userInfo.avatar};
                                            } else {
                                                container.avatar = TalkLogic.getInstance().getGroupTalkAvatar((Group) userInfo, IMG_RESIZE);
                                            }
                                            cb.done();
                                        } else {
                                            if (finalIsAtSelf) {
                                                container.ticker = "有人在群聊@了你";
                                            } else {
                                                container.ticker = message.getMsgContent().getPlainText();
                                            }
                                            container.contentText = container.ticker;
                                            container.contentTitle = title;
                                            if (!TextUtils.isEmpty(userInfo.avatar)) {
                                                container.avatar = new String[]{userInfo.avatar};
                                            }
                                            cb.done();
                                        }
                                    }
                                });
                            } else {
                                container.ticker = message.getMsgContent().getPlainText();
                                container.contentText = container.ticker;
                                container.contentTitle = "微聊";
                                cb.done();
                            }
                        }
                    });
        }
    }

    private void dummyNotification() {
        if ((boolean) GmacsConfig.UserConfig.getParam("openVibration", true)) {
            if (vibrator == null) {
                vibrator = (Vibrator) GmacsEnvi.appContext.getSystemService(Context.VIBRATOR_SERVICE);
            }
            if (vibrator.hasVibrator()) {
                vibrator.vibrate(new long[]{100, 400, 100, 400}, -1);
            }
        }
        if ((boolean) GmacsConfig.UserConfig.getParam("openSound", true)) {
            Uri notificationUri = RingtoneManager.getActualDefaultRingtoneUri(GmacsEnvi.appContext, RingtoneManager.TYPE_NOTIFICATION);
            SoundPlayer.getInstance().startPlaying(notificationUri);
        }
    }

    private interface CallBack {
        void done();
    }

    private class NotificationContainer {
        String[] avatar;
        String ticker;
        String contentTitle;
        String contentText;
        PendingIntent pendingIntent;
        int priority;
        Bitmap bitmap;

        NotificationContainer(Intent intent, int noticeId) {
            this.pendingIntent = PendingIntent.getActivity(GmacsEnvi.appContext,
                    noticeId, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        }

        Notification buildNotification() {
            Notification notification;
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                if (getPriority() != -1) {
                    notification = new NotificationCompat.Builder(
                            GmacsEnvi.appContext)
                            .setTicker(ticker)
                            .setContentTitle(contentTitle)
                            .setContentText(contentText)
                            .setSmallIcon(R.drawable.icon)
                            .setAutoCancel(true)
                            .setContentIntent(pendingIntent)
                            .setPriority(priority)
                            .build();
                } else {
                    dummyNotification();
                    return null;
                }
            } else {
                if (bitmap == null) {
                    notification = new NotificationCompat.Builder(
                            GmacsEnvi.appContext)
                            .setTicker(ticker)
                            .setContentTitle(contentTitle)
                            .setContentText(contentText)
                            .setSmallIcon(R.drawable.icon_transparent)
                            .setLargeIcon(BitmapFactory.decodeResource(GmacsEnvi.appContext.getResources(), R.drawable.icon))
                            .setAutoCancel(true)
                            .setContentIntent(pendingIntent)
                            .setPriority(priority)
                            .build();
                } else {
                    if (getPriority() != -1) {
                        notification = new NotificationCompat.Builder(
                                GmacsEnvi.appContext)
                                .setTicker(ticker)
                                .setContentTitle(contentTitle)
                                .setContentText(contentText)
                                .setSmallIcon(R.drawable.icon_transparent)
                                .setLargeIcon(bitmap)
                                .setAutoCancel(true)
                                .setContentIntent(pendingIntent)
                                .setPriority(priority)
                                .build();
                    } else {
                        dummyNotification();
                        return null;
                    }
                }
            }
            return notification;
        }
    }

}
