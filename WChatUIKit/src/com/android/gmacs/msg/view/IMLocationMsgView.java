package com.android.gmacs.msg.view;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.android.gmacs.R;
import com.android.gmacs.activity.GmacsMapActivity;
import com.android.gmacs.view.GmacsDialog;
import com.common.gmacs.msg.IMMessage;
import com.common.gmacs.msg.data.IMLocationMsg;
import com.common.gmacs.utils.GmacsEnvi;
import com.common.gmacs.utils.GmacsUtils;

public class IMLocationMsgView extends IMMessageView {
    public static final String LONGITUDE = "longitude";
    public static final String LATITUDE = "latitude";
    public static final String ADDRESS = "address";
    private static Bitmap mCachedLocationBitmapLeft;
    private static Bitmap mCachedLocationBitmapRight;
    private final int mMapSize = GmacsEnvi.appContext.getResources().getDimensionPixelOffset(R.dimen.im_chat_msg_pic_msg_width);
    private TextView mTvLocation;

    public IMLocationMsgView(IMMessage mIMMessage) {
        super(mIMMessage);
    }

    public static void releaseLocationBitmapCache() {
        mCachedLocationBitmapLeft = null;
        mCachedLocationBitmapRight = null;
    }

    @Override
    protected View initView(LayoutInflater inflater, ViewGroup parentView, int maxWidth) {
        if (mIMMessage.message.mIsSelfSendMsg) {
            mContentView = inflater.inflate(
                    R.layout.gmacs_adapter_msg_content_right_map, parentView, false);
        } else {
            mContentView = inflater.inflate(
                    R.layout.gmacs_adapter_msg_content_left_map, parentView, false);
        }
        RelativeLayout mRlLocation = (RelativeLayout) mContentView.findViewById(R.id.rl_location);
        mTvLocation = (TextView) mContentView.findViewById(R.id.tv_location);
        mRlLocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(mTvLocation.getContext(), GmacsMapActivity.class);
                intent.putExtra(LONGITUDE, ((IMLocationMsg) mIMMessage).mLongitude);
                intent.putExtra(LATITUDE, ((IMLocationMsg) mIMMessage).mLatitude);
                intent.putExtra(ADDRESS, ((IMLocationMsg) mIMMessage).mAddress);
                mTvLocation.getContext().startActivity(intent);
            }
        });
        mContentView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                final GmacsDialog.Builder dialog = new GmacsDialog.Builder(getContentView().getContext(), GmacsDialog.Builder.DIALOG_TYPE_LIST_NO_BUTTON);
                dialog.initDialog(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                        switch (position) {
                            case 0:// 删除消息
                                deleteIMMessageView();
                                dialog.dismiss();
                                break;
                            default:
                                break;
                        }
                        dialog.dismiss();
                    }
                }).setListTexts(new String[]{mChatActivity.getString(R.string.delete_message)}).create().show();
                return true;
            }
        });

        Drawable drawable;
        if (mIMMessage.message.mIsSelfSendMsg) {
            if (mCachedLocationBitmapRight != null && !mCachedLocationBitmapRight.isRecycled()) {
                ((ImageView) mContentView.findViewById(R.id.iv_location)).setImageBitmap(mCachedLocationBitmapRight);
                return mContentView;
            } else {
                drawable = mContentView.getResources().getDrawable(R.drawable.gmacs_bg_talk_view_layer_right);
            }
        } else {
            if (mCachedLocationBitmapLeft != null && !mCachedLocationBitmapLeft.isRecycled()) {
                ((ImageView) mContentView.findViewById(R.id.iv_location)).setImageBitmap(mCachedLocationBitmapLeft);
                return mContentView;
            } else {
                drawable = mContentView.getResources().getDrawable(R.drawable.gmacs_bg_talk_view_layer_left);
            }
        }
        Bitmap bitmap = BitmapFactory.decodeResource(mContentView.getResources(), R.drawable.gmacs_bg_location);
        Bitmap base = Bitmap.createBitmap(mMapSize, mMapSize, Bitmap.Config.ARGB_8888);
        drawable.setBounds(0, 0, mMapSize, mMapSize);
        Canvas canvas = new Canvas(base);
        drawable.draw(canvas);

        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_OVER));
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(mContentView.getResources().getColor(R.color.location_msg_background));
        canvas.drawRect(0, mMapSize - GmacsUtils.dipToPixel(54), mMapSize, mMapSize, paint);

        float scale;
        float dx = 0, dy = 0;
        if (bitmap.getWidth() * mMapSize > mMapSize * bitmap.getHeight()) {
            scale = (float) mMapSize / (float) bitmap.getHeight();
            dx = (mMapSize - bitmap.getWidth() * scale) * 0.5f;
        } else {
            scale = (float) mMapSize / (float) bitmap.getWidth();
            dy = (mMapSize - bitmap.getHeight() * scale) * 0.5f;
        }
        Matrix matrix = new Matrix();
        matrix.setScale(scale, scale);
        matrix.postTranslate(dx, dy);
        paint.reset();
        paint.setAntiAlias(true);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_OVER));
        canvas.drawBitmap(bitmap, matrix, paint);

        if (mIMMessage.message.mIsSelfSendMsg) {
            mCachedLocationBitmapRight = base;
        } else {
            mCachedLocationBitmapLeft = base;
        }

        ((ImageView) mContentView.findViewById(R.id.iv_location)).setImageBitmap(base);
        return mContentView;
    }

    @Override
    public void setDataForView(IMMessage imMessage) {
        super.setDataForView(imMessage);
        mTvLocation.setText(((IMLocationMsg) mIMMessage).mAddress);
    }
}
