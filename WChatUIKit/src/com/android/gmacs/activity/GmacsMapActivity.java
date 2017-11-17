package com.android.gmacs.activity;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Point;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.android.gmacs.R;
import com.android.gmacs.view.GmacsDialog;
import com.baidu.location.BDLocation;
import com.baidu.location.BDLocationListener;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.BitmapDescriptor;
import com.baidu.mapapi.map.BitmapDescriptorFactory;
import com.baidu.mapapi.map.InfoWindow;
import com.baidu.mapapi.map.MapStatus;
import com.baidu.mapapi.map.MapStatusUpdate;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.MarkerOptions;
import com.baidu.mapapi.map.MyLocationConfiguration;
import com.baidu.mapapi.map.MyLocationData;
import com.baidu.mapapi.map.OverlayOptions;
import com.baidu.mapapi.map.Projection;
import com.baidu.mapapi.model.LatLng;
import com.baidu.mapapi.search.core.SearchResult;
import com.baidu.mapapi.search.geocode.GeoCodeResult;
import com.baidu.mapapi.search.geocode.GeoCoder;
import com.baidu.mapapi.search.geocode.OnGetGeoCoderResultListener;
import com.baidu.mapapi.search.geocode.ReverseGeoCodeOption;
import com.baidu.mapapi.search.geocode.ReverseGeoCodeResult;
import com.common.gmacs.utils.GLog;
import com.common.gmacs.utils.ToastUtil;

public class GmacsMapActivity extends BaseActivity implements OnGetGeoCoderResultListener {

    public static final String LONGITUDE = "longitude";
    public static final String LATITUDE = "latitude";
    public static final String ADDRESS = "address";
    private final int LOCATION_TIMEOUT = 20000;
    private RelativeLayout mContentView;
    private View mMarker, mViewShow, mProgressBar;
    private TextView mTextView;
    // 地图触摸事件监听器
    BaiduMap.OnMapTouchListener touchListener = new BaiduMap.OnMapTouchListener() {
        @Override
        public void onTouch(MotionEvent motionEvent) {
            switch (motionEvent.getAction()) {
                case MotionEvent.ACTION_MOVE:
                    mTextView.setText(null);
                    mViewShow.setVisibility(View.GONE);
                    break;
                case MotionEvent.ACTION_UP:
                    mMarker.startAnimation(AnimationUtils.loadAnimation(GmacsMapActivity.this, R.anim.gmacs_translate_up));
                    mViewShow.setVisibility(View.VISIBLE);
                    mTextView.setVisibility(View.GONE);
                    mProgressBar.setVisibility(View.VISIBLE);
                    break;
            }
        }
    };
    private MapView mMapView;
    private BaiduMap mBaiduMap;
    private boolean isOnlyShow;
    private Handler mHandler;
    private Runnable mRunnable;
    private GmacsDialog mProgressDialog;
    // MapView中央对于的屏幕坐标
    private Point mCenterPoint = null;
    // 当前经纬度
    private double mLatitude;
    private double mLongitude;
    private String mAddress;
    /**
     * 百度定位
     */
    private LocationClient mLocClient;
    private MyLocationListener mLocationListener = new MyLocationListener();
    private GeoCoder geoCoder = null;

    @Override
    protected void initView() {
        mContentView = (RelativeLayout) findViewById(R.id.map_content_view);
        setTitle(getText(R.string.location_info));
        mMarker = findViewById(R.id.imageview);
        mMapView = (MapView) findViewById(R.id.mapview);

        mTitleBar.setRightText(getText(R.string.send));
        mTitleBar.setRightTextListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                rightViewClickTodo();
            }
        });

        // 地图初始化
        mBaiduMap = mMapView.getMap();
        MapStatusUpdate msu = MapStatusUpdateFactory.zoomTo(17.0f);
        mBaiduMap.setMapStatus(msu);

        mBaiduMap.setOnMapLoadedCallback(new BaiduMap.OnMapLoadedCallback() {
            @Override
            public void onMapLoaded() {
                // 初始化当前MapView中心屏幕坐标，初始化当前地理坐标
                mCenterPoint = new Point();
                mCenterPoint.x = mMapView.getWidth() / 2;
                mCenterPoint.y = mMapView.getHeight() / 2;

                MapStatus mapStatus = new MapStatus.Builder()
                        .targetScreen(mCenterPoint)
                        .build();
                MapStatusUpdate mapStatusUpdate = MapStatusUpdateFactory.newMapStatus(mapStatus);
                mBaiduMap.setMapStatus(mapStatusUpdate);
            }
        });
    }

    @Override
    protected void initData() {
        Intent intent = getIntent();
        mAddress = intent.getStringExtra(ADDRESS);
        double longitude = intent.getDoubleExtra(LONGITUDE, -1F);
        double latitude = intent.getDoubleExtra(LATITUDE, -1F);
        isOnlyShow = longitude != -1F && latitude != -1F;
        if (isOnlyShow) {
            mViewShow = getLayoutInflater().inflate(R.layout.gmacs_widget_map_pop, null);
            mTextView = (TextView) mViewShow.findViewById(R.id.textview_map_pop);
            mProgressBar = mViewShow.findViewById(R.id.progressbar_map_pop);

            mTitleBar.setRightText(getText(R.string.location_detail));
            //增加图层
            mBaiduMap.clear();
            LatLng latLng = new LatLng(latitude, longitude);
            BitmapDescriptor mIconMaker = BitmapDescriptorFactory.fromResource(R.drawable.gmacs_ic_map_marker);
            OverlayOptions overlayOptions = new MarkerOptions().position(latLng).icon(mIconMaker).zIndex(5);
            mBaiduMap.addOverlay(overlayOptions);
            InfoWindow mInfoWindow = new InfoWindow(mViewShow, latLng, -getResources().getDrawable(R.drawable.gmacs_ic_map_marker).getIntrinsicHeight());//-47
            // 显示InfoWindow
            mBaiduMap.showInfoWindow(mInfoWindow);
            mViewShow.setBackgroundResource(R.drawable.gmacs_bg_map_pop);
            //地图移动到相应位置
            MapStatusUpdate u = MapStatusUpdateFactory.newLatLng(latLng);
            mBaiduMap.animateMapStatus(u);
            //设置相应值
            if (TextUtils.isEmpty(mAddress)) {
                reverseGeoCode(latLng);
            } else {
                mTextView.setText(mAddress);
            }
        } else {
            mViewShow = getLayoutInflater().inflate(R.layout.gmacs_widget_map_pop, null);
            mViewShow.setBackgroundResource(R.drawable.gmacs_bg_map_pop);
            RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            layoutParams.addRule(RelativeLayout.CENTER_HORIZONTAL);
            layoutParams.addRule(RelativeLayout.ABOVE, R.id.imageview);
            mContentView.addView(mViewShow, layoutParams);
            mTextView = (TextView) mViewShow.findViewById(R.id.textview_map_pop);
            mProgressBar = mViewShow.findViewById(R.id.progressbar_map_pop);

            mHandler = new Handler();
            mRunnable = new Runnable() {
                @Override
                public void run() {
                    if (mProgressDialog != null) {
                        mProgressDialog.dismiss();
                    }
                    ToastUtil.showToast(R.string.location_unavailable);
                    finish();
                }
            };
            mHandler.postDelayed(mRunnable, LOCATION_TIMEOUT);
            mMarker.setVisibility(View.VISIBLE);
            mTitleBar.mRightTextView.setClickable(false);
            mProgressDialog = new GmacsDialog.Builder(this, GmacsDialog.Builder.DIALOG_TYPE_TEXT_PROGRESSBAR_NO_BUTTON)
                    .initDialog(getText(R.string.requesting)).setOnCancelListener(new DialogInterface.OnCancelListener() {
                        @Override
                        public void onCancel(DialogInterface dialog) {
                            onBackPressed();
                        }
                    }).create();
            mProgressDialog.show();
            initMyLocation();
            mBaiduMap.setOnMapTouchListener(touchListener);
            mBaiduMap.setOnMapStatusChangeListener(new BaiduMap.OnMapStatusChangeListener() {
                @Override
                public void onMapStatusChangeStart(MapStatus mapStatus) {
                }

                @Override
                public void onMapStatusChange(MapStatus mapStatus) {
                }

                @Override
                public void onMapStatusChangeFinish(MapStatus mapStatus) {
                    if (isFinishing()) {
                        return;
                    }
                    // 获取当前MapView中心屏幕坐标对应的地理坐标
                    Projection projection = mBaiduMap.getProjection();
                    if (projection == null) {
                        return;
                    }
                    reverseGeoCode(projection.fromScreenLocation(mCenterPoint));
                }
            });
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.gmacs_activity_map);
    }

    /**
     * 初始化定位
     */
    private void initMyLocation() {
        // 开启定位图层
        mBaiduMap.setMyLocationEnabled(true);
        // 定位初始化
        mLocClient = new LocationClient(this);
        mLocClient.registerLocationListener(mLocationListener);
        LocationClientOption option = new LocationClientOption();
        option.setOpenGps(true);// 打开gps
        option.setCoorType("bd09ll"); // 设置坐标类型
        option.setScanSpan(1000);//设置发起定位请求的间隔时间为1000ms
        option.setIsNeedAddress(true);//返回的定位结果包含地址信息
        option.setNeedDeviceDirect(true);//返回的定位结果包含手机机头的方向
        mLocClient.setLocOption(option);//设置定位参数
        mLocClient.start();
    }

    private void showAddress() {
        mViewShow.setVisibility(View.VISIBLE);
        if (mProgressDialog != null && mProgressDialog.isShowing()) {
            mProgressDialog.dismiss();
        }

        mTextView.setVisibility(View.VISIBLE);
        mProgressBar.setVisibility(View.GONE);
        if (TextUtils.isEmpty(mAddress)) {
            mTextView.setText(mLatitude + "," + mLongitude);
        } else {
            mTextView.setText(mAddress);
        }

        mTitleBar.mRightTextView.setClickable(true);
        if (mHandler != null && mRunnable != null) {
            mHandler.removeCallbacks(mRunnable);
        }

        if (mLocClient != null && mLocationListener != null) {
            mLocClient.stop();
            mLocClient.unRegisterLocationListener(mLocationListener);
        }
    }

    private void rightViewClickTodo() {
        if (isOnlyShow) {
            try {
                int MAP_SCALE = 16;
                Uri uri = Uri.parse("geo:" + mLatitude + "," + mLongitude + "?z=" + MAP_SCALE);
                Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                startActivity(intent);
            } catch (Exception e) {
                ToastUtil.showToast(R.string.action_not_handle);
            }
        } else {
            if (mProgressDialog != null) {
                mProgressDialog.dismiss();
            }
            Intent intent = new Intent();
            intent.putExtra(ADDRESS, mAddress);
            intent.putExtra(LONGITUDE, mLongitude);
            intent.putExtra(LATITUDE, mLatitude);
            setResult(RESULT_OK, intent);
            finish();
        }
    }

    @Override
    public void onGetGeoCodeResult(GeoCodeResult geoCodeResult) {
        if (geoCodeResult == null
                || geoCodeResult.error != SearchResult.ERRORNO.NO_ERROR) {
            // 没有检测到结果
        }
    }

    @Override
    public void onGetReverseGeoCodeResult(ReverseGeoCodeResult reverseGeoCodeResult) {
        if (reverseGeoCodeResult == null
                || reverseGeoCodeResult.error != SearchResult.ERRORNO.NO_ERROR) {
            // 没有检测到结果
            mAddress = null;
        } else {
            mAddress = reverseGeoCodeResult.getAddress();
//            String city = reverseGeoCodeResult.getAddressDetail().city;
//            String mRegion = reverseGeoCodeResult.getAddressDetail().district;
        }
        showAddress();
    }

    private void reverseGeoCode(LatLng latLng) {
        if (isFinishing()) {
            return;
        }
        if (geoCoder == null) {
            geoCoder = GeoCoder.newInstance();
        }
        mLatitude = latLng.latitude;
        mLongitude = latLng.longitude;

        // 设置地理编码检索监听者
        geoCoder.setOnGetGeoCodeResultListener(this);
        geoCoder.reverseGeoCode(new ReverseGeoCodeOption().location(latLng));
    }

    @Override
    protected void onDestroy() {
        if (geoCoder != null) {
            // 释放地理编码检索实例
            geoCoder.destroy();
        }
        // 退出时销毁定位
        if (mLocClient != null) {
            mLocClient.stop();
        }
        mBaiduMap.setMyLocationEnabled(false);
        mMapView.onDestroy();
        if (mHandler != null && mRunnable != null) {
            mHandler.removeCallbacks(mRunnable);
        }
        super.onDestroy();
    }

    /**
     * 实现实位回调监听
     */
    private class MyLocationListener implements BDLocationListener {

        @Override
        public void onReceiveLocation(BDLocation location) {
            // map view 销毁后不在处理新接收的位置
            if (location == null || mMapView == null) {
                ToastUtil.showToast(R.string.locate_failed);
                return;
            }
            try {
                MyLocationData data = new MyLocationData.Builder()//
                        // .direction(mCurrentX)//
                        .accuracy(location.getRadius())//
                        .latitude(location.getLatitude())//
                        .longitude(location.getLongitude())//
                        .build();
                mBaiduMap.setMyLocationData(data);
                // 设置自定义图标
                MyLocationConfiguration config = new MyLocationConfiguration(
                        MyLocationConfiguration.LocationMode.NORMAL, true, null);
                mBaiduMap.setMyLocationConfigeration(config);

                LatLng latlng = new LatLng(location.getLatitude(), location.getLongitude());
                MapStatusUpdate u = MapStatusUpdateFactory.newLatLng(latlng);
                mBaiduMap.animateMapStatus(u);
                mLocClient.unRegisterLocationListener(mLocationListener);
                mLocClient.stop();
                reverseGeoCode(latlng);
            } catch (Exception e) {
                GLog.e(TAG, e.getMessage());
                ToastUtil.showToast(R.string.baidu_map_init_failed);
                finish();
            }
        }
    }
}
