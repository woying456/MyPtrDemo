package com.android.gmacs.view;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.Build;
import android.os.Handler;
import android.support.annotation.ArrayRes;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.support.annotation.StyleRes;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;

import com.android.gmacs.R;
import com.common.gmacs.utils.GmacsEnvi;
import com.common.gmacs.utils.GmacsUtils;

import java.lang.reflect.Field;

import static android.view.View.GONE;
import static com.android.gmacs.R.style.dialog;

public class GmacsDialog extends Dialog {

    private GmacsDialog(Context context) {
        super(context);
    }

    private GmacsDialog(Context context, boolean cancelable, OnCancelListener cancelListener) {
        super(context, cancelable, cancelListener);
    }

    private GmacsDialog(Context context, int theme) {
        super(context, theme);
    }

    public GmacsDialog setLayout(int width, int height) {
        Window window = getWindow();
        if (window != null) {
            window.setLayout(width, height);
        }
        return this;
    }

    public GmacsDialog setWindowAnimations(@StyleRes int resId) {
        Window window = getWindow();
        if (window != null) {
            window.setWindowAnimations(resId);
        }
        return this;
    }

    public static class Builder {

        /**
         * Dialog with a ListView.
         */
        public static final int DIALOG_TYPE_LIST_NO_BUTTON = 1;
        /**
         * Dialog with a neutral Button and a TextView for showing message.
         */
        public static final int DIALOG_TYPE_TEXT_NEU_BUTTON = 2;
        /**
         * Dialog with a negative Button，a positive Button and a TextView for showing message.
         */
        public static final int DIALOG_TYPE_TEXT_NEG_POS_BUTTON = 3;
        /**
         * Dialog with a ProgressBar and a TextView for showing message.
         */
        public static final int DIALOG_TYPE_TEXT_PROGRESSBAR_NO_BUTTON = 4;
        /**
         * Dialog's contentView can be replaced by your own View.
         */
        public static final int DIALOG_TYPE_CUSTOM_CONTENT_VIEW = 5;

        public static final int DIALOG_TYPE_LARGE_TEXT_NEU_BUTTON = 6;

        public static final int DIALOG_TYPE_LARGE_TEXT_NEG_POS_BUTTON = 7;
        private final int DISMISS = 0x43;
        private final int CANCEL = 0x44;
        private final int SHOW = 0x45;
        /**
         * The instance of GmacsDialog.
         */
        private GmacsDialog mDialog;
        private LinearLayout mLayout;
        private Context mContext;
        private int mDialogType, mDialogStyle;
        private boolean mCancelable = true;
        private OnCancelListener mOnCancelListener;
        private OnDismissListener mOnDismissListener;
        private OnShowListener mOnShowListener;
        private ListView mListView;
        private CharSequence[] mListTexts;
        private AdapterView.OnItemClickListener mOnItemClickListener;
        private FastScrollView mMsgScrollView;
        private TextView mTitle, mMsg;
        private LinearLayout mBtnLayout, mMessageLayout;
        private View mContentView;
        private TextView mNeuBtn, mPosBtn, mNegBtn;
        private CharSequence mTitleText, mMsgText, mNeuBtnText, mPosBtnText, mNegBtnText;
        private View.OnClickListener mNeuBtnListener, mPosBtnListener, mNegBtnListener;
        private View.OnLongClickListener mNegBtnLongListener, mPosBtnLongListener;
        private int dialogGravity = Gravity.CENTER;
        private int mWindowFlags;

        private Resources mResources;

        public Builder(Context context, int dialogType) {
            this.mContext = context;
            this.mDialogType = dialogType;
            if (dialogType != DIALOG_TYPE_TEXT_PROGRESSBAR_NO_BUTTON) {
                mDialogStyle = dialog;
            } else {
                mDialogStyle = R.style.publish_btn_dialog;
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                try {
                    mWindowFlags = ((Activity) context).getWindow().getDecorView().getSystemUiVisibility();
                } catch (ClassCastException ignored) {
                }
            }
            mResources = context.getResources();
        }

        public Builder(Context context, int dialogType, int dialogStyle) {
            this.mContext = context;
            this.mDialogType = dialogType;
            this.mDialogStyle = dialogStyle;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                try {
                    mWindowFlags = ((Activity) context).getWindow().getDecorView().getSystemUiVisibility();
                } catch (ClassCastException ignored) {
                }
            }
            mResources = context.getResources();
        }

        /**
         * Initialize the Dialog and the mDialogType must be <i>{@link #DIALOG_TYPE_LIST_NO_BUTTON}</i>.
         * Invoke this method before {@link #create()}.
         *
         * @param listener Handle your ListView onItemClick action.
         * @return Builder
         */
        public Builder initDialog(@NonNull AdapterView.OnItemClickListener listener) {
            if (mDialogType == DIALOG_TYPE_LIST_NO_BUTTON) {
                mOnItemClickListener = listener;
            }
            return this;
        }

        /**
         * Initialize the Dialog and the mDialogType must be <i>{@link #DIALOG_TYPE_TEXT_NEU_BUTTON}</i>.
         * Invoke this method before {@link #create()}.
         *
         * @param msgTextResId    The string resource id, which carries the text that will be regard as a message.
         * @param neuBtnTextResId The string resource id, which carries the text that will be added into neutral Button.
         * @param neuBtnListener  Handle your neutral Button's click action.
         * @return Builder
         */
        public Builder initDialog(@StringRes int msgTextResId, @StringRes int neuBtnTextResId, @NonNull View.OnClickListener neuBtnListener) {
            if (mDialogType == DIALOG_TYPE_TEXT_NEU_BUTTON || mDialogType == DIALOG_TYPE_LARGE_TEXT_NEU_BUTTON) {
                mMsgText = mResources.getText(msgTextResId);
                mNeuBtnText = mResources.getText(neuBtnTextResId);
                this.mNeuBtnListener = neuBtnListener;
            }
            return this;
        }

        /**
         * Initialize the Dialog and the mDialogType must be <i>{@link #DIALOG_TYPE_TEXT_NEU_BUTTON}</i>.
         * Invoke this method before {@link #create()}.
         *
         * @param msgTextString    The string that will be regard as a message.
         * @param neuBtnTextString The string that will be added into neutral Button.
         * @param neuBtnListener   Handle your neutral Button's click action.
         * @return Builder
         */
        public Builder initDialog(CharSequence msgTextString, CharSequence neuBtnTextString, @NonNull View.OnClickListener neuBtnListener) {
            if (mDialogType == DIALOG_TYPE_TEXT_NEU_BUTTON || mDialogType == DIALOG_TYPE_LARGE_TEXT_NEU_BUTTON) {
                mMsgText = checkNull(msgTextString);
                mNeuBtnText = checkNull(neuBtnTextString).length() != 0 ? neuBtnTextString : mContext.getText(R.string.ok);
                this.mNeuBtnListener = neuBtnListener;
            }
            return this;
        }

        /**
         * Initialize the Dialog and the mDialogType must be <i>{@link #DIALOG_TYPE_TEXT_NEG_POS_BUTTON}</i>.
         * Invoke this method before {@link #create()}.
         *
         * @param msgTextResId    The string resource id, which carries the text that will be regard as a message.
         * @param negBtnTextResId The string resource id, which carries the text that will be added into negative Button.
         * @param posBtnTextResId The string resource id, which carries the text that will be added into positive Button.
         * @param negBtnListener  Handle your negative Button's click action.
         * @param posBtnListener  Handle your positive Button's click action.
         * @return Builder
         */
        public Builder initDialog(@StringRes int msgTextResId, @StringRes int negBtnTextResId, @StringRes int posBtnTextResId,
                                  @NonNull View.OnClickListener negBtnListener, @NonNull View.OnClickListener posBtnListener) {
            if (mDialogType == DIALOG_TYPE_TEXT_NEG_POS_BUTTON || mDialogType == DIALOG_TYPE_LARGE_TEXT_NEG_POS_BUTTON) {
                mMsgText = mResources.getText(msgTextResId);
                mNegBtnText = mResources.getText(negBtnTextResId);
                mPosBtnText = mResources.getText(posBtnTextResId);
                this.mNegBtnListener = negBtnListener;
                this.mPosBtnListener = posBtnListener;
            }
            return this;
        }

        /**
         * Initialize the Dialog and the mDialogType must be <i>{@link #DIALOG_TYPE_TEXT_NEG_POS_BUTTON}</i>.
         * Invoke this method before {@link #create()}.
         *
         * @param msgTextString    The string that will be regard as a message.
         * @param negBtnTextString The string that will be added into negative Button.
         * @param posBtnTextString The string that will be added into positive Button.
         * @param negBtnListener   Handle your negative Button's click action.
         * @param posBtnListener   Handle your positive Button's click action.
         * @return Builder
         */
        public Builder initDialog(CharSequence msgTextString, CharSequence negBtnTextString, CharSequence posBtnTextString,
                                  @NonNull View.OnClickListener negBtnListener, @NonNull View.OnClickListener posBtnListener) {
            if (mDialogType == DIALOG_TYPE_TEXT_NEG_POS_BUTTON || mDialogType == DIALOG_TYPE_LARGE_TEXT_NEG_POS_BUTTON) {
                mMsgText = checkNull(msgTextString);
                mNegBtnText = checkNull(negBtnTextString).length() != 0 ? negBtnTextString : mContext.getText(R.string.cancel);
                mPosBtnText = checkNull(posBtnTextString).length() != 0 ? posBtnTextString : mContext.getText(R.string.ok);
                this.mNegBtnListener = negBtnListener;
                this.mPosBtnListener = posBtnListener;
            }
            return this;
        }

        /**
         * Initialize the Dialog and the mDialogType must be <i>{@link #DIALOG_TYPE_TEXT_NEG_POS_BUTTON}</i>.
         * Invoke this method before {@link #create()}.
         *
         * @param msgTextString    The string that will be regard as a message.
         * @param negBtnTextString The string that will be added into negative Button.
         * @param posBtnTextString The string that will be added into positive Button.
         * @param negBtnListener   Handle your negative Button's click action.
         * @param posBtnListener   Handle your positive Button's click action.
         * @return Builder
         */
        public Builder initDialog(CharSequence msgTextString, CharSequence negBtnTextString, CharSequence posBtnTextString,
                                  @NonNull View.OnClickListener negBtnListener, @NonNull View.OnClickListener posBtnListener,
                                  @NonNull View.OnLongClickListener negBtnLongListener, @NonNull View.OnLongClickListener posBtnLongListener) {
            if (mDialogType == DIALOG_TYPE_TEXT_NEG_POS_BUTTON || mDialogType == DIALOG_TYPE_LARGE_TEXT_NEG_POS_BUTTON) {
                mMsgText = checkNull(msgTextString);
                mNegBtnText = checkNull(negBtnTextString).length() != 0 ? negBtnTextString : mContext.getText(R.string.cancel);
                mPosBtnText = checkNull(posBtnTextString).length() != 0 ? posBtnTextString : mContext.getText(R.string.ok);
                this.mNegBtnListener = negBtnListener;
                this.mPosBtnListener = posBtnListener;
                this.mNegBtnLongListener = negBtnLongListener;
                this.mPosBtnLongListener = posBtnLongListener;
            }
            return this;
        }

        /**
         * Initialize the Dialog and the mDialogType must be <i>{@link #DIALOG_TYPE_TEXT_PROGRESSBAR_NO_BUTTON}</i>.
         * Invoke this method before {@link #create()}.
         *
         * @param msgTextResId The string resource id, which carries the text that will be regard as a message.
         * @return Builder
         */
        public Builder initDialog(@StringRes int msgTextResId) {
            if (mDialogType == DIALOG_TYPE_TEXT_PROGRESSBAR_NO_BUTTON) {
                mMsgText = mResources.getText(msgTextResId);
            }
            return this;
        }

        /**
         * Initialize the Dialog and the mDialogType must be <i>{@link #DIALOG_TYPE_TEXT_PROGRESSBAR_NO_BUTTON}</i>.
         * Invoke this method before {@link #create()}.
         *
         * @param msgTextString The string that will be regard as a message.
         * @return Builder
         */
        public Builder initDialog(CharSequence msgTextString) {
            if (mDialogType == DIALOG_TYPE_TEXT_PROGRESSBAR_NO_BUTTON) {
                mMsgText = checkNull(msgTextString).length() != 0 ? msgTextString : mContext.getText(R.string.wait);
            }
            return this;
        }

        /**
         * Initialize the Dialog and the mDialogType must be <i>{@link #DIALOG_TYPE_CUSTOM_CONTENT_VIEW}</i>.
         * Invoke this method before {@link #create()}.
         *
         * @param contentView
         * @return
         */
        public Builder initDialog(View contentView) {
            if (mDialogType == DIALOG_TYPE_CUSTOM_CONTENT_VIEW) {
                mContentView = contentView;
            }
            return this;
        }

        public View getContentView() {
            if (mDialogType == DIALOG_TYPE_CUSTOM_CONTENT_VIEW) {
                return mContentView;
            } else {
                return null;
            }
        }

        /**
         * Set the title of Dialog. <br><b>The title would be shown, unless you invoke this method.</b></br>
         * Invoke this method before {@link #create()}, if possible.
         *
         * @param titleTextResId The string resource id, which carries the text that will be regard as a title.
         * @return Builder
         */
        public Builder setTitle(@StringRes int titleTextResId) {
            mTitleText = mResources.getText(titleTextResId);
            return this;
        }

        /**
         * Set the title of Dialog. <br><b>The title would be shown, unless you invoke this method.</b></br>
         * Invoke this method before {@link #create()}, if possible.
         *
         * @param titleTextString The string that will be regard as a title.
         * @return Builder
         */
        public Builder setTitle(CharSequence titleTextString) {
            mTitleText = checkNull(titleTextString);
            return this;
        }

        /**
         * Set the texts which will be shown on ListView.
         * Invoke this method before {@link #create()}, if possible.
         *
         * @param listTextsResId The string-array resource id, which carries the texts that will be added into ListView.
         * @return Builder
         */
        public Builder setListTexts(@ArrayRes int listTextsResId) {
            if (mDialogType == DIALOG_TYPE_LIST_NO_BUTTON) {
                mListTexts = mResources.getTextArray(listTextsResId);
            }
            return this;
        }

        /**
         * Set the texts which will be shown on ListView.
         * Invoke this method before {@link #create()}, if possible.
         *
         * @param listTextsStringArray The string array that will be added into ListView.
         * @return Builder
         * @throws NullPointerException Throws it if text array is null.
         */
        public Builder setListTexts(CharSequence[] listTextsStringArray) {
            if (mDialogType == DIALOG_TYPE_LIST_NO_BUTTON) {
                if (listTextsStringArray != null) {
                    int length = listTextsStringArray.length;
                    for (int i = 0; i < length; i++) {
                        listTextsStringArray[i] = checkNull(listTextsStringArray[i]);
                    }
                    mListTexts = listTextsStringArray;
                } else {
                    throw new NullPointerException("GmacsDialog -> Adapter text array null");
                }
            }
            return this;
        }

        /**
         * Set the gravity of the window, as per the Gravity constants.
         * This controls how the window manager is positioned in the overall window;
         * it is only useful when using WRAP_CONTENT for the layout width or height.
         *
         * @param gravity The desired gravity constant.
         */
        public Builder setGravity(int gravity) {
            dialogGravity = gravity;
            return this;
        }

        /**
         * The default cancelable value is true.
         * Invoke this method before {@link #create()}, if possible.
         *
         * @param cancelable
         * @return Builder
         */
        public Builder setCancelable(boolean cancelable) {
            this.mCancelable = cancelable;
            return this;
        }

        private void resetPreviousWindowFlags() {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && mContext instanceof Activity) {
                try {
                    ((Activity) mContext).getWindow().getDecorView().setSystemUiVisibility(mWindowFlags);
                } catch (ClassCastException ignored) {
                }
            }
        }

        /**
         * Invoke this method before {@link #create()}, if possible.
         *
         * @param onCancelListener
         * @return Builder
         */
        public Builder setOnCancelListener(OnCancelListener onCancelListener) {
            this.mOnCancelListener = onCancelListener;
            return this;
        }

        /**
         * Invoke this method before {@link #create()}, if possible.
         *
         * @param onDismissListener
         * @return Builder
         */
        public Builder setOnDismissListener(OnDismissListener onDismissListener) {
            this.mOnDismissListener = onDismissListener;
            return this;
        }

        /**
         * Invoke this method before {@link #create()}, if possible.
         *
         * @param onShowListener
         * @return Builder
         */
        public Builder setOnShowListener(OnShowListener onShowListener) {
            this.mOnShowListener = onShowListener;
            return this;
        }

        private CharSequence checkNull(CharSequence charSequence) {
            if (charSequence != null) {
                return charSequence;
            } else {
                return "";
            }
        }

        /**
         * Create Dialog.
         * <br><b>Invoke this method after everything has been prepared for showing Dialog.</b></br>
         *
         * @return GmacsDialog
         * @throws NullPointerException Throws it when click listeners never registered (mOnCancelListener excluded).
         */
        public GmacsDialog create() {
            final GmacsDialog dialog = new GmacsDialog(mContext, mDialogStyle);
            this.mDialog = dialog;
            if (dialog.getWindow() != null) {
                dialog.getWindow().setGravity(dialogGravity);
            }
            mLayout = (LinearLayout) LayoutInflater.from(mContext).inflate(R.layout.gmacs_dialog, null);

            if (mDialogType != DIALOG_TYPE_CUSTOM_CONTENT_VIEW) {
                dialog.setContentView(mLayout, new ViewGroup.LayoutParams(GmacsEnvi.screenWidth * 3 / 4, ViewGroup.LayoutParams.WRAP_CONTENT));
            } else {
                dialog.setContentView(mLayout, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            }
            dialog.setCancelable(mCancelable);
            if (mOnCancelListener != null) {
                dialog.setOnCancelListener(new OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        resetPreviousWindowFlags();
                        mOnCancelListener.onCancel(dialog);

                    }
                });
            }
            if (mOnDismissListener != null) {
                dialog.setOnDismissListener(new OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        resetPreviousWindowFlags();
                        mOnDismissListener.onDismiss(dialog);
                    }
                });
            }
            if (mOnShowListener != null) {
                dialog.setOnShowListener(new OnShowListener() {
                    @Override
                    public void onShow(DialogInterface dialog) {
                        mOnShowListener.onShow(dialog);
                    }
                });
            }

            mMsgScrollView = (FastScrollView) mLayout.findViewById(R.id.dialog_scrollview);
            mListView = (ListView) mLayout.findViewById(R.id.dialog_list);
            mTitle = (TextView) mLayout.findViewById(R.id.dialog_title);
            mMessageLayout = (LinearLayout) mLayout.findViewById(R.id.dialog_message_layout);
            mMsg = (TextView) mLayout.findViewById(R.id.dialog_text);
            mBtnLayout = (LinearLayout) mLayout.findViewById(R.id.dialog_btns_layout);
            mNeuBtn = (TextView) mLayout.findViewById(R.id.dialog_neu_btn);

            if (mTitleText != null) {
                mTitle.setText(mTitleText);
            } else {
                mTitle.setVisibility(GONE);
            }

            switch (mDialogType) {
                case DIALOG_TYPE_LIST_NO_BUTTON:
                    mMessageLayout.setVisibility(GONE);
                    mMsg.setVisibility(GONE);
                    mNeuBtn.setVisibility(GONE);
                    mBtnLayout.setVisibility(GONE);
                    mListView.setAdapter(new DialogAdapter(mContext));
                    mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                        @Override
                        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                            if (mOnItemClickListener != null) {
                                mOnItemClickListener.onItemClick(parent, view, position, id);
                            } else {
                                throw new NullPointerException("GmacsDialog -> OnItemClickListener null");
                            }
                            dialog.dismiss();
                        }
                    });
                    break;
                case DIALOG_TYPE_LARGE_TEXT_NEU_BUTTON: {
                    ViewGroup.LayoutParams layoutParams = mMsgScrollView.getLayoutParams();
                    layoutParams.height = (int) (GmacsEnvi.screenHeight * 0.6f);
                    mMsgScrollView.setLayoutParams(layoutParams);
                }
                case DIALOG_TYPE_TEXT_NEU_BUTTON: {
                    mListView.setVisibility(GONE);
                    mBtnLayout.setVisibility(GONE);
                    LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams) mMessageLayout.getLayoutParams();
                    int marginSize = GmacsUtils.dipToPixel(18);
                    layoutParams.setMargins(marginSize, marginSize, marginSize, GmacsUtils.dipToPixel(50));
                    mMsg.setText(mMsgText);
                    mMsg.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                        @Override
                        public void onGlobalLayout() {
                            mMsgScrollView.fullScroll(ScrollView.FOCUS_DOWN);
                        }
                    });
                    mNeuBtn.setText(mNeuBtnText);
                    mNeuBtn.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            if (mNeuBtnListener != null) {
                                mNeuBtnListener.onClick(v);
                            } else {
                                dialog.dismiss();
                                throw new NullPointerException("GmacsDialog -> NeuBtn OnClickListener null");
                            }
                        }
                    });
                    break;
                }
                case DIALOG_TYPE_LARGE_TEXT_NEG_POS_BUTTON: {
                    ViewGroup.LayoutParams layoutParams = mMsgScrollView.getLayoutParams();
                    layoutParams.height = (int) (GmacsEnvi.screenHeight * 0.6f);
                    mMsgScrollView.setLayoutParams(layoutParams);
                }
                case DIALOG_TYPE_TEXT_NEG_POS_BUTTON: {
                    mListView.setVisibility(GONE);
                    mNeuBtn.setVisibility(GONE);
                    mNegBtn = (TextView) mLayout.findViewById(R.id.dialog_neg_btn);
                    mPosBtn = (TextView) mLayout.findViewById(R.id.dialog_pos_btn);
                    LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams) mMessageLayout.getLayoutParams();
                    int marginSize = GmacsUtils.dipToPixel(18);
                    layoutParams.setMargins(marginSize, marginSize, marginSize, GmacsUtils.dipToPixel(50));
                    mMsg.setText(mMsgText);
                    mMsg.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                        @Override
                        public void onGlobalLayout() {
                            mMsgScrollView.fullScroll(ScrollView.FOCUS_DOWN);
                        }
                    });
                    mNegBtn.setText(mNegBtnText);
                    mPosBtn.setText(mPosBtnText);
                    mNegBtn.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            if (mNegBtnListener != null) {
                                mNegBtnListener.onClick(v);
                            } else {
                                dialog.cancel();
                                throw new NullPointerException("GmacsDialog -> NegBtn OnClickListener null");
                            }
                        }
                    });
                    mPosBtn.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            if (mPosBtnListener != null) {
                                mPosBtnListener.onClick(v);
                            } else {
                                dialog.dismiss();
                                throw new NullPointerException("GmacsDialog -> PosBtn OnClickListener null");
                            }
                        }
                    });
                    mNegBtn.setOnLongClickListener(mNegBtnLongListener);
                    mPosBtn.setOnLongClickListener(mPosBtnLongListener);
                    break;
                }
                case DIALOG_TYPE_TEXT_PROGRESSBAR_NO_BUTTON: {
                    ProgressBar pb = (ProgressBar) mLayout.findViewById(R.id.dialog_progressbar);
                    LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams) mMessageLayout.getLayoutParams();
                    layoutParams.leftMargin = 0;
                    mMessageLayout.setGravity(Gravity.CENTER_HORIZONTAL);
                    pb.setVisibility(ProgressBar.VISIBLE);

                    dialog.setContentView(mLayout, new ViewGroup.LayoutParams(GmacsEnvi.screenWidth / 2, ViewGroup.LayoutParams.WRAP_CONTENT));
                    mLayout.setBackgroundColor(mResources.getColor(R.color.dark_grey));
                    mTitle.setTextColor(mResources.getColor(R.color.white));
                    mMsg.setText(mMsgText);
                    mMsg.setTextColor(mResources.getColor(R.color.white));
                    mMsg.getLayoutParams().width = LinearLayout.LayoutParams.WRAP_CONTENT;
                    mBtnLayout.setVisibility(GONE);
                    mNeuBtn.setVisibility(GONE);
                    break;
                }
                case DIALOG_TYPE_CUSTOM_CONTENT_VIEW:
                    mListView.setVisibility(GONE);
                    mNeuBtn.setVisibility(GONE);
                    mBtnLayout.setVisibility(GONE);
                    mMessageLayout.removeAllViews();
                    mMessageLayout.addView(mContentView);
                    mLayout.setBackgroundColor(Color.TRANSPARENT);
                    LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams) mMessageLayout.getLayoutParams();
                    layoutParams.topMargin = 0;
                    layoutParams.bottomMargin = 0;
                    break;
            }
            return dialog;
        }

        /**
         * Start the dialog and display it on screen.
         */
        public void show() {
            if (mDialog != null) {
                mDialog.show();
            }
        }

        /**
         * @return Whether the dialog is currently showing.
         */
        public boolean isShowing() {
            return mDialog != null && mDialog.isShowing();
        }

        /**
         * Dismiss this mDialog, removing it from the screen. This method can be invoked safely from any thread.
         */
        public void dismiss() {
            if (mDialog.isShowing()) {
                mDialog.dismiss();
                mContext = null;
            }
            try {
                Field field = mDialog.getClass().getSuperclass().getDeclaredField("mListenersHandler");
                field.setAccessible(true);
                Handler mListenersHandler = (Handler) field.get(mDialog);
                mListenersHandler.removeMessages(CANCEL, mOnDismissListener);
                mListenersHandler.removeMessages(DISMISS, mOnCancelListener);
                mListenersHandler.removeMessages(SHOW, mOnShowListener);
            } catch (NoSuchFieldException ignored) {
            } catch (IllegalAccessException ignored) {
            }
            mDialog.setOnCancelListener(null);
            mDialog.setOnDismissListener(null);
            mDialog.setOnShowListener(null);
        }

        /**
         * Cancel this mDialog and it will also call your DialogInterface.OnCancelListener (if registered).
         */
        public void cancel() {
            if (mDialog.isShowing()) {
                mDialog.cancel();
                mContext = null;
            }
            try {
                Field field = mDialog.getClass().getSuperclass().getDeclaredField("mListenersHandler");
                field.setAccessible(true);
                Handler mListenersHandler = (Handler) field.get(mDialog);
                mListenersHandler.removeMessages(CANCEL, mOnDismissListener);
                mListenersHandler.removeMessages(DISMISS, mOnCancelListener);
                mListenersHandler.removeMessages(SHOW, mOnShowListener);
            } catch (NoSuchFieldException ignored) {
            } catch (IllegalAccessException ignored) {
            }
            mDialog.setOnCancelListener(null);
            mDialog.setOnDismissListener(null);
            mDialog.setOnShowListener(null);
        }

        private class DialogAdapter extends BaseAdapter {

            private LayoutInflater li;
            private ViewHolder vh;

            DialogAdapter(Context context) {
                li = LayoutInflater.from(context);
            }

            @Override
            public int getCount() {
                return mListTexts.length;
            }

            @Override
            public Object getItem(int position) {
                return mListTexts[position];
            }

            @Override
            public long getItemId(int position) {
                return position;
            }

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                vh = null;
                if (convertView == null) {
                    vh = new ViewHolder();
                    convertView = li.inflate(R.layout.gmacs_dialog_list_item, null);
                    if (position == 0) {
                        if (getCount() == 1) {
                            convertView.setBackgroundResource(R.drawable.gmacs_bg_dialog_list_item_all_corner);
                        } else {
                            convertView.setBackgroundResource(R.drawable.gmacs_bg_dialog_list_item_top_corner);
                        }
                    } else if (position == getCount() - 1 && getCount() > 1) {
                        convertView.setBackgroundResource(R.drawable.gmacs_bg_dialog_list_item_bottom_corner);
                    } else {
                        convertView.setBackgroundResource(R.drawable.gmacs_bg_dialog_list_item);
                    }
                    vh.tv = (TextView) convertView.findViewById(R.id.dialog_list_item_text);
                    convertView.setTag(vh);
                } else {
                    vh = (ViewHolder) convertView.getTag();
                }

                vh.tv.setText((CharSequence) getItem(position));
                return convertView;
            }

            private final class ViewHolder {
                TextView tv;
            }

        }

    }

}