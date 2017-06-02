package in.srain.cube.views.ptr.util;

import android.annotation.SuppressLint;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;


@SuppressLint("SimpleDateFormat")
public final class TimeUtil {
    public static SimpleDateFormat FMT           = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    public static SimpleDateFormat FMT_FOR_SHORT = new SimpleDateFormat("MM-dd HH:mm");
    public static SimpleDateFormat FMT_FOR_FULL  = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

    private TimeUtil() {
        //no instance
    }


    /**
     * 将给定格式的时间转化为long类型
     *
     * @param time
     * @return
     */
    public static Long getLongTime(String time) {
        Long longTime = 0L;
        try {
            Date parse = FMT.parse(time);
            longTime = parse.getTime();
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return longTime;
    }

    /**
     * 判断给定的畅聊包是否快过期
     *
     * @param endTime
     * @return
     */
    public static boolean isExpiration(String endTime) {
        Long currentTime = System.currentTimeMillis();
        if (getLongTime(endTime) - currentTime < 2 * 24 * 3600 * 1000) {
            return true;
        }
        return false;
    }


    /**
     * 时期比较：
     *
     * @param time1,time2 时期格式为HH:mm格式
     * @return >:1,=:0,<-1 --->Integer.MIN_VALUE:日期解析错误
     */
    public static int compareTime(String time1, String time2) {
        SimpleDateFormat format = new SimpleDateFormat("HH:mm");
        Calendar c = Calendar.getInstance();
        Calendar c2 = Calendar.getInstance();
        try {
            c.setTime(format.parse(time1));
            c2.setTime(format.parse(time2));
            return c.compareTo(c2);
        } catch (ParseException e) {
            return Integer.MIN_VALUE;
        }
    }

    /**
     * 获取月，日时间，格式：MM-dd
     *
     * @param time
     * @return
     */
    public static String getMonthAndDay(long time) {
        SimpleDateFormat format = new SimpleDateFormat("MM-dd");
        return format.format(new Date(time));
    }

    public static String getTime(long time) {
        SimpleDateFormat format = new SimpleDateFormat("M月d日 HH:mm");
        return format.format(new Date(time));
    }

    public static String getHourAndMin(long time) {
        SimpleDateFormat format = new SimpleDateFormat("HH:mm");
        return format.format(new Date(time));
    }

    public static String getChatTime(long timesamp) {
        String result = "";
        SimpleDateFormat sdf = new SimpleDateFormat("dd");
        Date today = new Date(System.currentTimeMillis());
        Date otherDay = new Date(timesamp);
        int temp = Integer.parseInt(sdf.format(today))
                - Integer.parseInt(sdf.format(otherDay));

        switch (temp) {
            case 0:
                result = "今天 " + getHourAndMin(timesamp);
                break;
            case 1:
                result = "昨天 " + getHourAndMin(timesamp);
                break;
            case 2:
                result = "前天 " + getHourAndMin(timesamp);
                break;

            default:
                result = getTime(timesamp);
                break;
        }

        return result;
    }

    public static String getCallTime(String time) {
        try {
            long timesamp = Long.parseLong(time);
            return getCallTime(timesamp);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    public static String getCallTime(long time) {
        String result = "";
        try {
            Calendar now = Calendar.getInstance();
            long ms = 1000 * (now.get(Calendar.HOUR_OF_DAY) * 3600 + now.get(Calendar.MINUTE) * 60 + now.get(Calendar.SECOND));
            long ms_now = now.getTimeInMillis();
            if (ms_now - time < ms) {
                result = getHourAndMin(time);
            } else if (ms_now - time < (ms + 24 * 3600 * 1000)) {
                result = "昨天";
            } else if (ms_now - time < (ms + 24 * 3600 * 1000 * 2)) {
                result = getWeekday(time);
            } else {
                result = getMonthAndDay(time);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    private static String getWeekday(long time) {
        SimpleDateFormat sdf = new SimpleDateFormat("E");
        return sdf.format(time);
    }

    /**
     * 将php返回的时间戳Str转为java时间戳（10位时间戳转13位时间戳）
     *
     * @param str
     * @return
     */
    public static Long getJavaTimestampByPhpTimestamp(String str) {
        long time = Long.parseLong(str) * 1000L;
        return time;
    }

    public static String getYearMonthAndDay() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy.MM.dd");
        return sdf.format(new Date());
    }

    /**
     * String转Date
     */
    public static Date string2Date(String arg, String format) {
        SimpleDateFormat sdf = null;
        String trimString = arg.trim();
        if (format != null) {
            sdf = new SimpleDateFormat(format);
        } else {
            if (trimString.length() > 14)
                sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            else
                sdf = new SimpleDateFormat("yyyy-MM-dd");
        }
        Date d = null;
        try {
            d = sdf.parse(trimString);
        } catch (ParseException e) {
            return null;
        }
        return d;
    }

    /**
     * 1、提问时间小于等于60秒，显示【刚刚】；2、提问时间小于等于1小时，显示：【&&分钟前】；
     * 3、提问时间小于等于24小时，显示：【&&小时前】；4、提问时间大于24h显示：【年-月-日】
     *
     * @return
     */
    public static String getDiffTime(Long timestamp, Long currentTime) {
        String ret = "";
        Long sub = Math.abs(timestamp - currentTime) / 1000L;
        if (sub <= 60) {
            ret = sub + "刚刚";
            return ret;
        }
        if (sub < 3600) {
            ret = sub / 60 + "分钟前";
            return ret;
        }
        if (sub < 86400) {
            ret = sub / 3600 + "小时前";
            return ret;
        }
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        ret = sdf.format(new Date(timestamp));
        return ret;
    }


    /**
     * 获取当前的日期时间字符串, 格式为yyyy-MM-dd HH:mm:ss
     *
     * @return
     */
    public static String getCurrentDateTimeString() {
        return FMT.format(new Date());
    }

    /**
     * 获取当前的日期时间字符串, 格式为yyyy-MM-dd HH:mm:ss.SSS
     *
     * @return
     */
    public static String getCurrentDateTimeFullString() {
        return FMT_FOR_FULL.format(new Date());
    }

    /**
     * 获取当前的日期字符串, 格式为yyyy-MM-dd
     *
     * @return
     */
    public static String getCurrentDateString() {
        return FMT.format(new Date()).split(" ")[0];
    }

    /**
     * 获取当前的时间字符串(全), 格式为: HH:mm:ss.SSS
     *
     * @return
     */
    public static String getCurrentTimeFullString() {
        return FMT_FOR_FULL.format(new Date()).split(" ")[1];
    }

    /**
     * 获取当前的日期时间, 格式为: MM-dd HH:mm
     *
     * @return
     */
    public static String getCurrentDateTimeShortString() {
        return FMT_FOR_SHORT.format(new Date());
    }


    public static String getCurrentDateStringWithDotSplit() {
        return getCurrentDateString().replaceAll("-", ".");
    }
}
