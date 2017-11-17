package com.android.gmacs.utils;

import android.content.Context;

public class FileProviderUtil {

    public static String getFileProviderAuthority(Context context) {
        return context.getPackageName() + ".WChatFileProvider";
    }
}
