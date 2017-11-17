package com.android.gmacs.album;

import com.common.gmacs.parse.message.Message;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import static com.android.gmacs.album.AlbumConstant.MAX_IMAGE_AMOUNT_IN_ROW;
import static com.android.gmacs.album.AlbumConstant.MAX_ROW_AMOUNT_PER_GROUP;

public class WChatAlbumUtil {

    static final int SUNDAY = Calendar.SUNDAY;
    private static final Calendar calendar = Calendar.getInstance();
    private static final int FLAG_THIS_WEEK = 1;
    private static final int FLAG_THIS_MONTH = 2;
    static int THIS_YEAR = calendar.get(Calendar.YEAR);
    static int THIS_MONTH = calendar.get(Calendar.MONTH) + 1;
    static int THIS_WEEK = calendar.get(Calendar.WEEK_OF_MONTH);
    static int THIS_DAY_OF_WEEK = calendar.get(Calendar.DAY_OF_WEEK);
    private static int msgCountPerRow;
    private static int lastFlag = -1;

    private static ArrayList<Message> tempRowList;

    /* add messages into adapter, which are split into several groups by the same timestamp */
    public static void split(boolean showAllMsg, List<Message> messageList,
                             ArrayList<ArrayList<Message>> resultList) {
        updateCurrentTime();
        for (Message message : messageList) {
            setTimeInMillis(message.mMsgUpdateTime);
            if (THIS_YEAR == getYear() && THIS_MONTH == getMonth()) {
                int week = getWeekOfMonth();
                if (THIS_WEEK == week) {
                    if (getDayOfWeek() == SUNDAY) { // last sunday
                        checkAndAdd(FLAG_THIS_MONTH, message, resultList);
                    } else {
                        checkAndAdd(FLAG_THIS_WEEK, message, resultList);
                    }
                } else if (THIS_WEEK == week + 1) {
                    if (THIS_DAY_OF_WEEK == SUNDAY) { // this sunday
                        checkAndAdd(FLAG_THIS_WEEK, message, resultList);
                    } else {
                        checkAndAdd(FLAG_THIS_MONTH, message, resultList);
                    }
                } else {
                    checkAndAdd(FLAG_THIS_MONTH, message, resultList);
                }
                continue;
            }
            checkAndAdd(getYear() << 2 + getMonth(), message, resultList);
        }

        if (!showAllMsg && resultList.size() > MAX_ROW_AMOUNT_PER_GROUP) {
            ArrayList<ArrayList<Message>> subList = new ArrayList<>(resultList.subList(0, MAX_ROW_AMOUNT_PER_GROUP));
            resultList.clear();
            resultList.addAll(subList);
        }

        if (resultList.size() == 1 && resultList.get(0).size() == 0) {
            resultList.clear();
        }

        msgCountPerRow = 0;
        lastFlag = -1;
        tempRowList = null;
    }

    static void setTimeInMillis(long timeInMillis) {
        calendar.setTimeInMillis(timeInMillis);
    }

    static int getYear() {
        return calendar.get(Calendar.YEAR);
    }

    static int getMonth() {
        return calendar.get(Calendar.MONTH) + 1;
    }

    static int getWeekOfMonth() {
        return calendar.get(Calendar.WEEK_OF_MONTH);
    }

    static int getDayOfWeek() {
        return calendar.get(Calendar.DAY_OF_WEEK);
    }

    private static void checkAndAdd(int flag, Message message,
                                    ArrayList<ArrayList<Message>> resultList) {
        if (lastFlag != flag) { // timestamp group changed
            if (tempRowList != null && !resultList.contains(tempRowList)) {
                resultList.add(tempRowList);
            }
            msgCountPerRow = 1;
            tempRowList = new ArrayList<>();
            resultList.add(tempRowList);
        } else {
            if (++msgCountPerRow % (MAX_IMAGE_AMOUNT_IN_ROW + 1) == 0) {
                msgCountPerRow = 1;
                tempRowList = new ArrayList<>();
                resultList.add(tempRowList);
            }
        }
        tempRowList.add(message);
        lastFlag = flag;
    }

    private static void updateCurrentTime() {
        setTimeInMillis(System.currentTimeMillis());
        THIS_YEAR = calendar.get(Calendar.YEAR);
        THIS_MONTH = calendar.get(Calendar.MONTH) + 1;
        THIS_WEEK = calendar.get(Calendar.WEEK_OF_MONTH);
        THIS_DAY_OF_WEEK = calendar.get(Calendar.DAY_OF_WEEK);
    }

}
