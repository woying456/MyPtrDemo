package com.android.gmacs.album;

public final class AlbumConstant {

    /* 相册 */
    public static final String KEY_SELECTED_IMG_DATA = "selected_img_data";
    public static final String RAW = "raw";
    public static final String IS_PREVIEW = "isPreview";
    public static final String FROM_CAMERA = "fromCamera";
    public static final String EXTRA_PHOTO_MAX_COUNT = "photoMaxCount";
    /* 微聊相册 */
    public static final int RESULT_CODE_IMAGE_DELETED = 0x2000;
    public static final String DELETING_MSG_LOCAL_ID = "deletingMsgLocalId";
    public static final String IMAGE_LOCAL_ID = "imageLocalId";
    public static final String IMAGE_COUNT = "imageCount";
    public static final String BEGIN_LOCAL_ID = "beginLocalId";
    public static final String ALBUM_TITLE = "albumTitle";
    /* 相册 */
    static final int REQUEST_CODE_ALBUM = 0x1000;
    static final int REQUEST_CODE_ALBUM_BROWSER = 0x1001;
    static final String KEY_IMG_POSITION = "img_position";
    static final String FUNC = "func";
    static final String FUNC_UPDATE = "update";
    static final String FUNC_OK = "ok";
    static final String FUNC_CANCEL = "cancel";
    static final String DIR_PATH = "dirPath";
    /* 微聊相册 */
    static final int REQUEST_CODE_WCHAT_ALBUM_PREVIEW = 0x2001;
    static final int REQUEST_CODE_WCHAT_ALBUM_BROWSER = 0x2002;
    static final int MAX_IMAGE_AMOUNT_IN_ROW = 4;
    static final int MAX_ROW_AMOUNT_PER_GROUP = 3;
    static final int MSG_COUNT_PER_FETCHING = MAX_IMAGE_AMOUNT_IN_ROW * MAX_ROW_AMOUNT_PER_GROUP;
    static final String LAUNCHED_FROM_ALBUM = "launchedFromAlbum";

}
