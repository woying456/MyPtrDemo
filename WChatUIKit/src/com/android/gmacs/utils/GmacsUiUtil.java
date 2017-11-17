package com.android.gmacs.utils;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;

import com.android.gmacs.activity.GmacsWebViewActivity;
import com.common.gmacs.core.GmacsConstant;

import java.io.File;

public class GmacsUiUtil {

    public static final String SAVE_IMAGE_FILE_DIR = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "newbroker").getAbsolutePath();

    /**
     * 进入聊天界面时，得设置聊天界面类名，具体的扩展留给宿主程序来设置
     */
    private static String sChatClassName = "com.android.gmacs.activity.GmacsChatActivity";
    /**
     * 宿主程序的主界面，供跳转，具体的扩展留给宿主程序来设置
     */
    private static String sAppMainClassName = "com.android.gmacs.activity.GmacsChatActivity";
    /**
     * 宿主程序的浏览器界面，供跳转，具体的扩展留给宿主程序来设置
     */
    private static String sBrowserClassName = "com.android.gmacs.activity.GmacsWebViewActivity";
    /**
     * 宿主程序的联系人详情页，供跳转，具体的扩展留给宿主程序来设置
     */
    private static String sContactDetailActivityClassName = "com.android.gmacs.activity.GmacsContactDetailActivity";
    /**
     * 宿主程序的聊天页右上角按钮点击跳转页，供跳转，具体的扩展留给宿主程序来设置
     */
    private static String sTalkDetailActivityClassName = "com.android.gmacs.activity.GmacsContactDetailActivity";

    ////////////////////////////////////////////////////////////////////////////
    /**
     * 语音转文字
     */
    private static String sConvertToTextActivityClassName;

    public static String getChatClassName() {
        return sChatClassName;
    }

    /**
     * 宿主程序设置聊天类
     */
    public static void setChatClassName(String chatClassName) {
        sChatClassName = chatClassName;
    }

    ////////////////////////////////////////////////////////////////////////////

    /**
     * 构造到聊天界面的intent
     *
     * @param context
     * @param talkType
     * @param id
     * @param source
     */
    public static Intent createToChatActivity(Context context, int talkType, String id, int source, long focusMessageLocalId) {
        Intent intent = null;
        try {
            intent = new Intent(context, Class.forName(getChatClassName()));
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        if (intent != null) {
            intent.putExtra(GmacsConstant.EXTRA_TALK_TYPE, talkType);
            intent.putExtra(GmacsConstant.EXTRA_USER_ID, id);
            intent.putExtra(GmacsConstant.EXTRA_USER_SOURCE, source);
            intent.putExtra(GmacsConstant.EXTRA_FOCUS_MESSAGE_LOCAL_ID, focusMessageLocalId);
        }
        return intent;
    }

    public static Intent createToChatActivity(Context context, int talkType, String id, int source) {
        return createToChatActivity(context, talkType, id, source, -1);
    }

    public static String getAppMainClassName() {
        return sAppMainClassName;
    }

    ////////////////////////////////////////////////////////////////////////////

    /**
     * 宿主程序设置程序主页
     */
    public static void setAppMainClassName(String appMainClassName) {
        sAppMainClassName = appMainClassName;
    }

    public static String getBrowserClassName() {
        return sBrowserClassName;
    }

    /**
     * 宿主程序设置网页浏览器类
     */
    public static void setBrowserClassName(String browserClassName) {
        sBrowserClassName = browserClassName;
    }

    ////////////////////////////////////////////////////////////////////////////

    public static String getContactDetailActivityClassName() {
        return sContactDetailActivityClassName;
    }

    /**
     * 宿主程序设置联系人详情页面类
     */
    public static void setContactDetailActivityClassName(String contactDetailActivityClassName) {
        sContactDetailActivityClassName = contactDetailActivityClassName;
    }

    public static String getTalkDetailActivityClassName() {
        return sTalkDetailActivityClassName;
    }

    /**
     * 宿主程序设置聊天页右上角按钮点击跳转页
     */
    public static void setTalkDetailActivityClassName(String talkDetailActivityClassName) {
        sTalkDetailActivityClassName = talkDetailActivityClassName;
    }

    public static String getConvertToTextActivityClassName() {
        return sConvertToTextActivityClassName;
    }

    public static void setConvertToTextActivityClassName(String convertToTextActivityClassName) {
        sConvertToTextActivityClassName = convertToTextActivityClassName;

    }

    /**
     * @param activity
     * @param bundle   传递 url和 webViewtitle
     */
    public static void startBrowserActivity(Activity activity, Bundle bundle) {
        if (bundle != null) {
            String url = bundle.getString(GmacsWebViewActivity.EXTRA_URL);
            Intent intent = new Intent();
            intent.setAction(Intent.ACTION_VIEW);
            intent.setData(Uri.parse(url));
            try {
                activity.startActivity(intent);
            } catch (ActivityNotFoundException e) {
                try {
                    intent = new Intent(activity, Class.forName(GmacsUiUtil.getBrowserClassName()));
                    intent.putExtras(bundle);
                    activity.startActivity(intent);
                } catch (ClassNotFoundException e1) {
                    e1.printStackTrace();
                }
            }
        }

    }

}
