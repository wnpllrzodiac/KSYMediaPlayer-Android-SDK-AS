package com.ksy.media.widget.ui.livereplay;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.text.SimpleDateFormat;
import java.util.Locale;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Color;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.OrientationEventListener;
import android.view.SurfaceHolder;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.ksy.media.player.IMediaPlayer;
import com.ksy.media.player.util.Constants;
import com.ksy.media.player.util.DRMKey;
import com.ksy.media.player.util.DRMRetrieverManager;
import com.ksy.media.player.util.DRMRetrieverResponseHandler;
import com.ksy.media.player.util.IDRMRetriverRequest;
import com.ksy.media.player.util.NetworkUtil;
import com.ksy.media.widget.ui.common.MediaPlayerBufferingView;
import com.ksy.media.widget.ui.common.MediaPlayerLoadingView;
import com.ksy.media.widget.ui.common.MediaPlayerMovieRatioView;
import com.ksy.media.widget.util.ControlDelay;
import com.ksy.media.widget.util.IPowerStateListener;
import com.ksy.media.widget.controller.MediaPlayerBaseControllerView;
import com.ksy.media.widget.controller.LiveReplayMediaPlayerControllerView;
import com.ksy.media.widget.videoview.LiveReplayMediaPlayerVideoView;
import com.ksy.media.widget.data.MediaPlayMode;
import com.ksy.media.widget.data.MediaPlayerUtils;
import com.ksy.media.widget.data.NetReceiver;
import com.ksy.media.widget.data.NetReceiver.NetState;
import com.ksy.media.widget.data.NetReceiver.NetStateChangedListener;
import com.ksy.media.widget.data.WakeLocker;
import com.ksy.mediaPlayer.widget.R;

public class MediaPlayerViewLiveReplay extends RelativeLayout implements
        IPowerStateListener {
    private static final int QUALITY_BEST = 100;
    private static final String CAPUTRE_SCREEN_PATH_LIVE_REPLAY = "KSY_SDK_SCREENSHOT";
    private Activity mActivity;
    private LayoutInflater mLayoutInflater;
    private Window mWindow;
    private ViewGroup mRootView;
    //	private MediaPlayerTexutureVideoView mLiveReplayMediaPlayerVideoView;
    private LiveReplayMediaPlayerVideoView mLiveReplayMediaPlayerVideoView;
    private LiveReplayMediaPlayerControllerView mLiveReplayMediaPlayerControllerView;

    private MediaPlayerBufferingView mMediaPlayerBufferingView;
    private MediaPlayerLoadingView mMediaPlayerLoadingView;
    private MediaPlayerEventActionViewLiveReplay mMediaPlayerEventActionViewLiveReplay;

    private PlayerViewCallback mPlayerViewCallback;

    private final int ORIENTATION_UNKNOWN = -2;
    private final int ORIENTATION_HORIZON = -1;
    private final int ORIENTATION_PORTRAIT_NORMAL = 0;
    private final int ORIENTATION_LANDSCAPE_REVERSED = 90;
    private final int ORIENTATION_PORTRAIT_REVERSED = 180;
    private final int ORIENTATION_LANDSCAPE_NORMAL = 270;

    private volatile int mScreenOrientation = ORIENTATION_UNKNOWN;
    private volatile int mPlayMode = MediaPlayMode.PLAYMODE_FULLSCREEN;
    private volatile boolean mLockMode = false;
    private volatile boolean mScreenLockMode = false;
    private volatile boolean mScreenshotPreparing = false;

    private boolean mVideoReady = false;
    private int mPausePosition = 0;

    private OrientationEventListener mOrientationEventListener;
    private ViewGroup.LayoutParams mLayoutParamWindowMode;
    private LayoutParams mMediaPlayerControllerViewLargeParams;
    private LayoutParams mMediaPlayerControllerViewSmallParams;

    private volatile boolean mWindowActived = false;
    private boolean mDeviceNaturalOrientationLandscape;
    private boolean mCanLayoutSystemUI;
    private boolean mDeviceNavigationBarExist;
    private int mFullScreenNavigationBarHeight;
    private int mDeviceNavigationType = MediaPlayerUtils.DEVICE_NAVIGATION_TYPE_UNKNOWN;
    private int mDisplaySizeMode = MediaPlayerMovieRatioView.MOVIE_RATIO_MODE_16_9;

    private NetReceiver mNetReceiver;
    private NetStateChangedListener mNetChangedListener;
    private boolean mIsComplete = false;

    private float mCurrentPlayingRatio = 1f;
    private float mCurrentPlayingVolumeRatio = 1f;
    public static float MAX_PLAYING_RATIO = 4f;
    public static float MAX_PLAYING_VOLUME_RATIO = 3.0f;
    // add for replay
    private boolean mRecyclePlay = false;
    private DRMRetrieverManager mDrmManager;
    private DRMRetrieverResponseHandler mDrmHandler;

    private Handler mHandler = new Handler();

    private ControlDelay controlDelay = ControlDelay.getInstance();
    private Context mContext;
    private IPowerStateListener powerStateListener;

    public MediaPlayerViewLiveReplay(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mContext = context;
        init(context, attrs, defStyle);

    }

    public MediaPlayerViewLiveReplay(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        init(context, attrs, -1);

    }

    public MediaPlayerViewLiveReplay(Context context) {
        super(context);
        mContext = context;
        init(context, null, -1);

    }

    private void init(Context context, AttributeSet attrs, int defStyle)
            throws IllegalArgumentException, NullPointerException {

        if (null == context) {
            throw new NullPointerException("Context can not be null !");
        }

        registerPowerReceiver();
        setPowerStateListener(this);

        TypedArray typedArray = context.obtainStyledAttributes(attrs,
                R.styleable.PlayerView);
        int playmode = typedArray.getInt(R.styleable.PlayerView_playmode,
                MediaPlayMode.PLAYMODE_FULLSCREEN);
        if (playmode == 0) {
            this.mPlayMode = MediaPlayMode.PLAYMODE_FULLSCREEN;
        } else if (playmode == 1) {
            this.mPlayMode = MediaPlayMode.PLAYMODE_WINDOW;
        }
        this.mLockMode = typedArray.getBoolean(R.styleable.PlayerView_lockmode,
                false);
        typedArray.recycle();

        this.mLayoutInflater = LayoutInflater.from(context);
        this.mActivity = (Activity) context;
        this.mWindow = mActivity.getWindow();

        this.setBackgroundColor(Color.BLACK);
        this.mDeviceNavigationBarExist = MediaPlayerUtils
                .hasNavigationBar(mWindow);
        this.mDeviceNaturalOrientationLandscape = (MediaPlayerUtils
                .getDeviceNaturalOrientation(mWindow) == MediaPlayerUtils.DEVICE_NATURAL_ORIENTATION_LANDSCAPE ? true
                : false);
        this.mCanLayoutSystemUI = Build.VERSION.SDK_INT >= 16 ? true : false;
        if (mDeviceNavigationBarExist
                && MediaPlayerUtils.isFullScreenMode(mPlayMode)) {
            this.mFullScreenNavigationBarHeight = MediaPlayerUtils
                    .getNavigationBarHeight(mWindow);
            this.mDeviceNavigationType = MediaPlayerUtils
                    .getDeviceNavigationType(mWindow);
        }

		/* 初始化UI组件 */
        this.mRootView = (ViewGroup) mLayoutInflater.inflate(
                R.layout.live_replay_blue_media_player_view, null);
        this.mLiveReplayMediaPlayerVideoView = (LiveReplayMediaPlayerVideoView) mRootView
                .findViewById(R.id.live_replay_ks_camera_video_view);
        this.mMediaPlayerBufferingView = (MediaPlayerBufferingView) mRootView
                .findViewById(R.id.ks_camera_buffering_view);
        this.mMediaPlayerLoadingView = (MediaPlayerLoadingView) mRootView
                .findViewById(R.id.ks_camera_loading_view);

        this.mMediaPlayerEventActionViewLiveReplay = (MediaPlayerEventActionViewLiveReplay) mRootView
                .findViewById(R.id.ks_camera_event_action_view_live_replay);
        this.mLiveReplayMediaPlayerControllerView = (LiveReplayMediaPlayerControllerView) mRootView
                .findViewById(R.id.media_player_controller_view_live_replay);

		/* 设置播放器监听器 */
        this.mLiveReplayMediaPlayerVideoView.setOnPreparedListener(mOnPreparedListener);
        this.mLiveReplayMediaPlayerVideoView
                .setOnBufferingUpdateListener(mOnPlaybackBufferingUpdateListener);
        this.mLiveReplayMediaPlayerVideoView
                .setOnCompletionListener(mOnCompletionListener);
        this.mLiveReplayMediaPlayerVideoView.setOnInfoListener(mOnInfoListener);
        this.mLiveReplayMediaPlayerVideoView
                .setOnDRMRequiredListener(mOnDRMRequiredListener);
        this.mLiveReplayMediaPlayerVideoView.setOnErrorListener(mOnErrorListener);
        this.mLiveReplayMediaPlayerVideoView.setOnSurfaceListener(mOnSurfaceListener);
        this.mLiveReplayMediaPlayerVideoView
                .setMediaPlayerController(mLiveReplayMediaPlayerController);
        this.mLiveReplayMediaPlayerVideoView
                .setOnSpeedListener(mOnPlaybackNetSpeedListener);
        this.mLiveReplayMediaPlayerVideoView.setOnDebugInfoListener(mOnDebugListener);
        this.mLiveReplayMediaPlayerVideoView.setFocusable(false);

        setPowerStateListener(this.mLiveReplayMediaPlayerVideoView);
        /* 设置playerVideoView UI 参数 */
        LayoutParams mediaPlayerVideoViewParams = new LayoutParams(
                LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
        mediaPlayerVideoViewParams.addRule(RelativeLayout.CENTER_IN_PARENT);

		/* 设置playerVideoView UI 参数 */
        LayoutParams mediaPlayerBufferingViewParams = new LayoutParams(
                LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        mediaPlayerBufferingViewParams.addRule(RelativeLayout.CENTER_IN_PARENT);
        this.mMediaPlayerBufferingView.hide();

		/* 设置loading UI 参数 */
        LayoutParams mediaPlayerLoadingViewParams = new LayoutParams(
                LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
        mediaPlayerLoadingViewParams.addRule(RelativeLayout.CENTER_IN_PARENT);
        this.mMediaPlayerLoadingView.hide();

        LayoutParams mediaPlayerPopViewParams = new LayoutParams(
                240, 230);
        mediaPlayerPopViewParams.addRule(RelativeLayout.CENTER_IN_PARENT);

		/* 设置eventActionView UI 参数 */
        LayoutParams mediaPlayereventActionViewParams = new LayoutParams(
                LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
        mediaPlayereventActionViewParams
                .addRule(RelativeLayout.CENTER_IN_PARENT);

		/* 设置eventActionView callback */
        this.mMediaPlayerEventActionViewLiveReplay
                .setCallback(new MediaPlayerEventActionViewLiveReplay.EventActionViewCallback() {
                    @Override
                    public void onActionPlay() {
                        if (NetworkUtil.isNetworkAvailable(mContext)) {
                            mIsComplete = false;
                            Log.i(Constants.LOG_TAG,
                                    "event action  view action play");
                            mMediaPlayerEventActionViewLiveReplay.hide();
                            mMediaPlayerLoadingView.hide();
                            mLiveReplayMediaPlayerVideoView.start();
                        } else {
                            Toast.makeText(mContext, "no network",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onActionReplay() {
                        if (NetworkUtil.isNetworkAvailable(mContext)) {
                            Log.i(Constants.LOG_TAG,
                                    "event action  view action replay");
                            mMediaPlayerEventActionViewLiveReplay.hide();
                            mIsComplete = false;
                            if (mLiveReplayMediaPlayerController != null) {
                                mLiveReplayMediaPlayerController.start();
                            } else {
                                mLiveReplayMediaPlayerVideoView.start();
                            }
                        } else {
                            Toast.makeText(mContext, "no network",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onActionError() {
                        if (NetworkUtil.isNetworkAvailable(mContext)) {
                            mIsComplete = false;
                            Log.i(Constants.LOG_TAG,
                                    "event action  view action error");
                            mMediaPlayerEventActionViewLiveReplay.hide();
//                            mLiveReplayMediaPlayerControllerView.hide();
                            mMediaPlayerLoadingView.show();
                            mLiveReplayMediaPlayerVideoView.setVideoPath(url);
                        } else {
                            Toast.makeText(mContext, "no network",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onActionBack() {
                        mIsComplete = false;

                        Log.i(Constants.LOG_TAG,
                                "event action  view action back");
                        mLiveReplayMediaPlayerController.onBackPress(mPlayMode);
                    }
                });

		/* 初始化:ControllerViewLarge */
        this.mMediaPlayerControllerViewLargeParams = new LayoutParams(
                LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
        this.mMediaPlayerControllerViewLargeParams
                .addRule(RelativeLayout.ALIGN_PARENT_LEFT);
        this.mMediaPlayerControllerViewLargeParams
                .addRule(RelativeLayout.ALIGN_PARENT_TOP);
        if (mDeviceNavigationBarExist && mCanLayoutSystemUI
                && mFullScreenNavigationBarHeight > 0) {
            if (mDeviceNavigationType == MediaPlayerUtils.DEVICE_NAVIGATION_TYPE_HANDSET) {
                mMediaPlayerControllerViewLargeParams.rightMargin = mFullScreenNavigationBarHeight;
            } else if (mDeviceNavigationType == MediaPlayerUtils.DEVICE_NAVIGATION_TYPE_TABLET) {
                mMediaPlayerControllerViewLargeParams.bottomMargin = mFullScreenNavigationBarHeight;
            }
        }

		/* 初始化:ControllerViewLarge */
        this.mLiveReplayMediaPlayerControllerView
                .setMediaPlayerController(mLiveReplayMediaPlayerController);
        /*this.mLiveReplayMediaPlayerControllerView.setHostWindow(mWindow);
        this.mLiveReplayMediaPlayerControllerView
                .setDeviceNavigationBarExist(mDeviceNavigationBarExist);
        this.mLiveReplayMediaPlayerControllerView.setNeedGestureDetector(true);
        this.mLiveReplayMediaPlayerControllerView.setNeedGestureAction(false, false,
                false);*/
        this.mMediaPlayerControllerViewSmallParams = new LayoutParams(
                LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);

		/* 移除掉所有的view */
        removeAllViews();
        mRootView.removeView(mLiveReplayMediaPlayerVideoView);
        mRootView.removeView(mMediaPlayerBufferingView);
        mRootView.removeView(mMediaPlayerLoadingView);
        mRootView.removeView(mMediaPlayerEventActionViewLiveReplay);
        mRootView.removeView(mLiveReplayMediaPlayerControllerView);

		/* 添加全屏或者是窗口模式初始状态下所需的view */
        addView(mLiveReplayMediaPlayerVideoView, mediaPlayerVideoViewParams);
        addView(mMediaPlayerBufferingView, mediaPlayerBufferingViewParams);
        addView(mMediaPlayerLoadingView, mediaPlayerLoadingViewParams);
        addView(mMediaPlayerEventActionViewLiveReplay, mediaPlayereventActionViewParams);

        if (MediaPlayerUtils.isWindowMode(mPlayMode)) {
            addView(mLiveReplayMediaPlayerControllerView,
                    mMediaPlayerControllerViewSmallParams);
//			mMediaPlayerLiveReplayControllerView.hide();
        }

        mMediaPlayerBufferingView.hide();
        mMediaPlayerLoadingView.hide();
        mMediaPlayerEventActionViewLiveReplay.hide();

        post(new Runnable() {
            @Override
            public void run() {
                if (MediaPlayerUtils.isWindowMode(mPlayMode)) {
                    mLayoutParamWindowMode = getLayoutParams();
                }

                try {
                    @SuppressWarnings("unchecked")
                    Class<? extends LayoutParams> parentLayoutParamClazz = (Class<? extends LayoutParams>) getLayoutParams()
                            .getClass();
                    Constructor<? extends LayoutParams> constructor = parentLayoutParamClazz
                            .getDeclaredConstructor(int.class, int.class);

                } catch (Exception e) {
                    e.printStackTrace();
                }

            }
        });
        // Default not use,if need it ,open it
        // initOrientationEventListener(context);

        mNetReceiver = NetReceiver.getInstance();
        mNetChangedListener = new NetStateChangedListener() {
            @Override
            public void onNetStateChanged(NetState netCode) {
                switch (netCode) {
                    case NET_NO:
                        Log.i(Constants.LOG_TAG, "网络断了");

                        // Toast.makeText(getContext(), "网络变化了:没有网络连接",
                        // Toast.LENGTH_LONG).show();
                        break;
                    case NET_2G:
                        Log.i(Constants.LOG_TAG, "2g网络");

                        // Toast.makeText(getContext(), "网络变化了:2g网络",
                        // Toast.LENGTH_LONG).show();
                        break;
                    case NET_3G:
                        Log.i(Constants.LOG_TAG, "3g网络");

                        // Toast.makeText(getContext(), "网络变化了:3g网络",
                        // Toast.LENGTH_LONG).show();
                        break;
                    case NET_4G:
                        Log.i(Constants.LOG_TAG, "4g网络");

                        // Toast.makeText(getContext(), "网络变化了:4g网络",
                        // Toast.LENGTH_LONG).show();
                        break;
                    case NET_WIFI:
                        Log.i(Constants.LOG_TAG, "WIFI网络");

                        // Toast.makeText(getContext(), "网络变化了:WIFI网络",
                        // Toast.LENGTH_LONG).show();
                        break;

                    case NET_UNKNOWN:
                        Log.i(Constants.LOG_TAG, "未知网络");

                        // Toast.makeText(getContext(), "网络变化了:未知网络",
                        // Toast.LENGTH_LONG).show();
                        break;
                    default:
                        Log.i(Constants.LOG_TAG, "不知道什么情况~>_<~");
                        // Toast.makeText(getContext(), "网络变化了:不知道什么情况~>_<~",
                        // Toast.LENGTH_LONG).show();
                }
            }
        };

    }

    private String url = null;

    public void setPowerStateListener(IPowerStateListener powerStateListener) {
        this.powerStateListener = powerStateListener;
    }

    public void play(String path, boolean isDelay) {

        if (this.mLiveReplayMediaPlayerVideoView != null) {
            controlDelay.setDelay(isDelay);
            Log.d(Constants.LOG_TAG, "play() path =" + path);
            url = path;
            this.mLiveReplayMediaPlayerVideoView.setVideoPath(url);
        }
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (mMediaPlayerEventActionViewLiveReplay.isShowing()) {
            return mMediaPlayerEventActionViewLiveReplay.dispatchTouchEvent(ev);
        }

        if (mVideoReady && !mMediaPlayerEventActionViewLiveReplay.isShowing()) {
            if (MediaPlayerUtils.isWindowMode(mPlayMode)) {
                Log.d("lixp","MediaPlayerUtils.isWindowMode(mPlayMode) =" + MediaPlayerUtils.isWindowMode(mPlayMode));
                return mLiveReplayMediaPlayerControllerView.dispatchTouchEvent(ev);
            }
        }

        return true;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {

        return super.onTouchEvent(event);
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {

        if (event.getKeyCode() == KeyEvent.KEYCODE_BACK
                && event.getAction() == KeyEvent.ACTION_DOWN) {
            if (mScreenLockMode) {
                return true;
            }

            if (MediaPlayerUtils.isWindowMode(mPlayMode)) {
//				if (mPlayerViewCallback != null)
//					mPlayerViewCallback.onFinish(mPlayMode);
//                return true;
                return false;
            }

        } else if (event.getKeyCode() == KeyEvent.KEYCODE_MENU
                || event.getKeyCode() == KeyEvent.KEYCODE_SEARCH) {
            if (mScreenLockMode) {
                return true;
            }
        }
        return false;
    }

    public void setPlayerViewCallback(PlayerViewCallback callback) {

        this.mPlayerViewCallback = callback;
    }

    public void setmRecyclePlay(boolean mRecyclePlay) {
        this.mRecyclePlay = mRecyclePlay;
    }

    public int getPlayMode() {

        return this.mPlayMode;
    }

    private boolean requestPlayMode(int requestPlayMode) {

        if (mPlayMode == requestPlayMode)
            return false;

            // 请求窗口模式
        else if (MediaPlayerUtils.isWindowMode(requestPlayMode)) {

            if (mLayoutParamWindowMode == null)
                return false;

            addView(mLiveReplayMediaPlayerControllerView,
                    mMediaPlayerControllerViewSmallParams);
            this.setLayoutParams(mLayoutParamWindowMode);
//			mMediaPlayerLiveReplayControllerView.hide();

            if (mPlayerViewCallback != null) {
                mPlayerViewCallback.restoreViews();
            }
            mWindow.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN
                    | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
            if (mDeviceNavigationBarExist) {
                MediaPlayerUtils.showSystemUI(mWindow, false);
            }
            mPlayMode = requestPlayMode;
            return true;

        }

        return false;
    }

    public void onResume() {

        mWindowActived = true;
        powerStateListener.onPowerState(Constants.APP_SHOWN);
        enableOrientationEventListener();
        mNetReceiver.registNetBroadCast(getContext());
        mNetReceiver.addNetStateChangeListener(mNetChangedListener);
    }

    public void onPause() {
        powerStateListener.onPowerState(Constants.APP_HIDEN);
        mNetReceiver.remoteNetStateChangeListener(mNetChangedListener);
        mNetReceiver.unRegistNetBroadCast(getContext());
        mWindowActived = false;
        mPausePosition = mLiveReplayMediaPlayerController.getCurrentPosition();

        disableOrientationEventListener();

        if (mLiveReplayMediaPlayerController.isPlaying()) {
            mLiveReplayMediaPlayerController.pause();
        }
        WakeLocker.release();
    }

    public void onDestroy() {
        mIsComplete = false;
        unregisterPowerReceiver();
        Log.d(Constants.LOG_TAG, "MediaPlayerView   onDestroy....");
    }

    private void initOrientationEventListener(Context context) {

        if (null == context)
            return;

        if (null == mOrientationEventListener) {
            mOrientationEventListener = new OrientationEventListener(context,
                    SensorManager.SENSOR_DELAY_NORMAL) {

                @Override
                public void onOrientationChanged(int orientation) {

                    int preScreenOrientation = mScreenOrientation;
                    mScreenOrientation = convertAngle2Orientation(orientation);
                    if (mScreenLockMode)
                        return;
                    if (!mWindowActived)
                        return;

                    if (preScreenOrientation == ORIENTATION_UNKNOWN)
                        return;
                    if (mScreenOrientation == ORIENTATION_UNKNOWN)
                        return;
                    if (mScreenOrientation == ORIENTATION_HORIZON)
                        return;

                    if (preScreenOrientation != mScreenOrientation) {
                        if (!MediaPlayerUtils.checkSystemGravity(getContext()))
                            return;
                        if (MediaPlayerUtils.isWindowMode(mPlayMode)) {
                            Log.i(Constants.LOG_TAG, " Window to FullScreen ");
                            if (mScreenOrientation == ORIENTATION_LANDSCAPE_NORMAL
                                    || mScreenOrientation == ORIENTATION_LANDSCAPE_REVERSED) {
                                if (!mLockMode) {
                                    boolean requestResult = requestPlayMode(MediaPlayMode.PLAYMODE_FULLSCREEN);
                                    if (requestResult) {
                                        doScreenOrientationRotate(mScreenOrientation);
                                    }
                                }
                            }
                        }
                    }
                }
            };
            enableOrientationEventListener();
        }

    }

    private int convertAngle2Orientation(int angle) {

        int screentOrientation = ORIENTATION_HORIZON;

        if ((angle >= 315 && angle <= 359) || (angle >= 0 && angle < 45)) {
            screentOrientation = ORIENTATION_PORTRAIT_NORMAL;
            if (mDeviceNaturalOrientationLandscape) {
                screentOrientation = ORIENTATION_LANDSCAPE_NORMAL;
            }
        } else if (angle >= 45 && angle < 135) {
            screentOrientation = ORIENTATION_LANDSCAPE_REVERSED;
            if (mDeviceNaturalOrientationLandscape) {
                screentOrientation = ORIENTATION_PORTRAIT_NORMAL;
            }
        } else if (angle >= 135 && angle < 225) {
            screentOrientation = ORIENTATION_PORTRAIT_REVERSED;
            if (mDeviceNaturalOrientationLandscape) {
                screentOrientation = ORIENTATION_LANDSCAPE_REVERSED;
            }
        } else if (angle >= 225 && angle < 315) {
            screentOrientation = ORIENTATION_LANDSCAPE_NORMAL;
            if (mDeviceNaturalOrientationLandscape) {
                screentOrientation = ORIENTATION_PORTRAIT_REVERSED;
            }
        }

        return screentOrientation;

    }

    private void doScreenOrientationRotate(int screenOrientation) {

        switch (screenOrientation) {
            case ORIENTATION_PORTRAIT_NORMAL:
//			mActivity
//					.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                break;
            case ORIENTATION_LANDSCAPE_REVERSED:
//			mActivity
//					.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE);
                if (mDeviceNavigationBarExist
                        && mFullScreenNavigationBarHeight <= 0
                        && MediaPlayerUtils.isFullScreenMode(mPlayMode)) {
                    this.mFullScreenNavigationBarHeight = MediaPlayerUtils
                            .getNavigationBarHeight(mWindow);
                    this.mDeviceNavigationType = MediaPlayerUtils
                            .getDeviceNavigationType(mWindow);
                    if (mCanLayoutSystemUI && mFullScreenNavigationBarHeight > 0) {
//					if (mDeviceNavigationType == MediaPlayerUtils.DEVICE_NAVIGATION_TYPE_HANDSET) {
//						mMediaPlayerControllerViewLargeParams.rightMargin = mFullScreenNavigationBarHeight;
//					} else if (mDeviceNavigationType == MediaPlayerUtils.DEVICE_NAVIGATION_TYPE_TABLET) {
//						mMediaPlayerControllerViewLargeParams.bottomMargin = mFullScreenNavigationBarHeight;
//					}
                    }
                }
                break;
            case ORIENTATION_PORTRAIT_REVERSED:
                mActivity
                        .setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT);
                break;
            case ORIENTATION_LANDSCAPE_NORMAL:
                mActivity
                        .setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                if (mDeviceNavigationBarExist
                        && mFullScreenNavigationBarHeight <= 0
                        && MediaPlayerUtils.isFullScreenMode(mPlayMode)) {
                    this.mFullScreenNavigationBarHeight = MediaPlayerUtils
                            .getNavigationBarHeight(mWindow);
                    this.mDeviceNavigationType = MediaPlayerUtils
                            .getDeviceNavigationType(mWindow);
                    if (mCanLayoutSystemUI && mFullScreenNavigationBarHeight > 0) {
//					if (mDeviceNavigationType == MediaPlayerUtils.DEVICE_NAVIGATION_TYPE_HANDSET) {
//						mMediaPlayerControllerViewLargeParams.rightMargin = mFullScreenNavigationBarHeight;
//					} else if (mDeviceNavigationType == MediaPlayerUtils.DEVICE_NAVIGATION_TYPE_TABLET) {
//						mMediaPlayerControllerViewLargeParams.bottomMargin = mFullScreenNavigationBarHeight;
//					}
                    }
                }
                break;
        }

    }

    private void enableOrientationEventListener() {

        if (mOrientationEventListener != null
                && mOrientationEventListener.canDetectOrientation()) {
            mOrientationEventListener.enable();
        }
    }

    private void disableOrientationEventListener() {

        if (mOrientationEventListener != null) {
            mOrientationEventListener.disable();
            mScreenOrientation = OrientationEventListener.ORIENTATION_UNKNOWN;
        }
    }

    private void updateVideoInfo2Controller() {

//		mMediaPlayerLiveReplayControllerView.updateVideoTitle(url);
        mMediaPlayerEventActionViewLiveReplay.updateVideoTitle(url);
    }

    private void changeMovieRatio() {
        if (mDisplaySizeMode > MediaPlayerMovieRatioView.MOVIE_RATIO_MODE_4_3) {
            mDisplaySizeMode = MediaPlayerMovieRatioView.MOVIE_RATIO_MODE_16_9;
        }

        mLiveReplayMediaPlayerVideoView.setVideoLayout(mDisplaySizeMode);
        // mDisplaySizeMode++;
    }

    IMediaPlayer.OnPreparedListener mOnPreparedListener = new IMediaPlayer.OnPreparedListener() {
        @Override
        public void onPrepared(IMediaPlayer mp) {
            Log.d(Constants.LOG_TAG,
                    "IMediaPlayer.OnPreparedListener onPrepared");
            int duration = 0;
            if (mLiveReplayMediaPlayerController != null) {
                duration = mLiveReplayMediaPlayerController.getDuration();
            }

            if (mIsComplete) {
//                mLiveReplayMediaPlayerControllerView.hide();
                mMediaPlayerEventActionViewLiveReplay
                        .updateEventMode(
                                MediaPlayerEventActionViewLiveReplay.EVENT_ACTION_VIEW_MODE_COMPLETE,
                                null);
                mMediaPlayerEventActionViewLiveReplay.show();
                WakeLocker.release();
            }
            if (mPausePosition > 0 && duration > 0) {
                if (!mIsComplete) {
                    mLiveReplayMediaPlayerController.pause();
                    mLiveReplayMediaPlayerController.seekTo(mPausePosition);
                    mPausePosition = 0;
                }

            }
            if (!WakeLocker.isScreenOn(getContext())
                    && mLiveReplayMediaPlayerController.canPause()) {
                if (!mIsComplete) {
                    mLiveReplayMediaPlayerController.pause();
                }
            }
            updateVideoInfo2Controller();
            mMediaPlayerLoadingView.hide();

            if (!mIsComplete) {
                mLiveReplayMediaPlayerVideoView.start();
            }

            mVideoReady = true;
            if (mPlayerViewCallback != null)
                mPlayerViewCallback.onPrepared();
        }

    };

    IMediaPlayer.OnCompletionListener mOnCompletionListener = new IMediaPlayer.OnCompletionListener() {

        @Override
        public void onCompletion(IMediaPlayer mp) {
            Log.i(Constants.LOG_TAG, "================onCompletion============");
            if (mRecyclePlay) {
                Log.i(Constants.LOG_TAG, "==replay==");
                mMediaPlayerEventActionViewLiveReplay.hide();
                if (mLiveReplayMediaPlayerController != null) {
                    mLiveReplayMediaPlayerController.start();
                } else {
                    mLiveReplayMediaPlayerVideoView.start();
                }
            } else {
                mIsComplete = true;
//                mLiveReplayMediaPlayerControllerView.hide();
                mMediaPlayerEventActionViewLiveReplay
                        .updateEventMode(
                                MediaPlayerEventActionViewLiveReplay.EVENT_ACTION_VIEW_MODE_COMPLETE,
                                null);
                mMediaPlayerEventActionViewLiveReplay.show();
                WakeLocker.release();
            }
        }
    };

    IMediaPlayer.OnInfoListener mOnInfoListener = new IMediaPlayer.OnInfoListener() {

        @Override
        public boolean onInfo(IMediaPlayer mp, int what, int extra) {
            switch (what) {
                case IMediaPlayer.MEDIA_INFO_METADATA_SPEED:
                    // Log.i(Constants.LOG_TAG, "MEDIA_INFO_METADATA_SPEED:"
                    // +extra);
                    break;
                // 视频缓冲开始
                case IMediaPlayer.MEDIA_INFO_BUFFERING_START:
                    Log.i(Constants.LOG_TAG, "MEDIA_INFO_BUFFERING_START");
                    mMediaPlayerBufferingView.show();
                    break;
                // 视频缓冲结束
                case IMediaPlayer.MEDIA_INFO_BUFFERING_END:
                    Log.i(Constants.LOG_TAG, "MEDIA_INFO_BUFFERING_END");
                    mMediaPlayerBufferingView.hide();
                    break;
                default:
                    break;
            }
            return true;
        }
    };

    IMediaPlayer.OnDRMRequiredListener mOnDRMRequiredListener = new IMediaPlayer.OnDRMRequiredListener() {

        @Override
        public void OnDRMRequired(IMediaPlayer mp, int what, int extra,
                                  String version) {

            Toast.makeText(getContext(),
                    "begin drm retriving..version :" + version,
                    Toast.LENGTH_SHORT).show();
            requestDRMKey(version);
        }
    };

    private void requestDRMKey(final String version) {

        if (mDrmManager == null)
            mDrmManager = DRMRetrieverManager.getInstance();
        if (mDrmHandler == null) {
            mDrmHandler = new DRMRetrieverResponseHandler() {

                private static final long serialVersionUID = 1L;

                @Override
                public void onSuccess(String version, String cek) {

                    mLiveReplayMediaPlayerVideoView.setDRMKey(version, cek);
                    Toast.makeText(
                            getContext(),
                            "DRM KEY retrieve success,ver :" + version
                                    + ", key :" + cek, Toast.LENGTH_SHORT)
                            .show();
                }

                @Override
                public void onFailure(int arg0, String arg1, Throwable arg2) {
                    Log.e(Constants.LOG_TAG,
                            "drm retrieve failed !!!!!!!!!!!!!!");
                    Toast.makeText(getContext(), "DRM KEY retrieve failed",
                            Toast.LENGTH_SHORT).show();
                }

            };
        }

        IDRMRetriverRequest request = new IDRMRetriverRequest(version, url) {

            private static final long serialVersionUID = 1L;

            @Override
            public DRMKey retriveDRMKeyFromAppServer(String cekVersion,
                                                     String cekUrl) {

                return null;
            }

            @Override
            public DRMFullURL retriveDRMFullUrl(String cekVersion, String cekUrl)
                    throws Exception {

                DRMFullURL fullURL = new DRMFullURL("2HITWMQXL2VBB3XMAEHQ",
                        "ilZQ9p/NHAK1dOYA/dTKKeIqT/t67rO6V2PrXUNr", cekUrl,
                        cekVersion);

                return fullURL;

            }
        };
        mDrmManager.retrieveDRM(request, mDrmHandler);
    }

    IMediaPlayer.OnBufferingUpdateListener mOnPlaybackBufferingUpdateListener = new IMediaPlayer.OnBufferingUpdateListener() {

        @Override
        public void onBufferingUpdate(IMediaPlayer mp, int percent) {

            if (percent > 0 && percent <= 100) {
            } else {
            }

        }
    };

    IMediaPlayer.OnNetSpeedListener mOnPlaybackNetSpeedListener = new IMediaPlayer.OnNetSpeedListener() {
        @Override
        public void onNetSpeedUpdate(IMediaPlayer mp, int arg1, int arg2) {
            // arg2 = arg2 / 1024 / 8; KB/s
//			mTextViewSpeed.setText(getResources().getString(R.string.net_speed)
//					+ " " + arg2 + " bit/s");
        }
    };

    IMediaPlayer.OnDebugInfoListener mOnDebugListener = new IMediaPlayer.OnDebugInfoListener() {

        @Override
        public void onDebugInfo(IMediaPlayer mp, int type, int arg1, int arg2) {

            // if (type == 10002) {
            // mTextViewDemux.setText("demux:" + arg1 + " , " + arg2);
            // } else if (type == 10003) {
            // mTextViewDecode.setText("decode:" + arg1 + " , " + arg2);
            // } else if (type == 10004) {
            // mTextViewTime.setText("time:" + arg1 + " , " + arg2);
            // }
        }
    };

    IMediaPlayer.OnErrorListener mOnErrorListener = new IMediaPlayer.OnErrorListener() {

        @Override
        public boolean onError(IMediaPlayer mp, int what, int extra) {

            Log.e(Constants.LOG_TAG, "On Native Error,what :" + what
                    + " , extra :" + extra);
//			mMediaPlayerLiveReplayControllerView.hide();
            mMediaPlayerBufferingView.hide();
            mMediaPlayerLoadingView.hide();
            mMediaPlayerEventActionViewLiveReplay.updateEventMode(
                    MediaPlayerEventActionViewLiveReplay.EVENT_ACTION_VIEW_MODE_ERROR,
                    what + "," + extra);
            mMediaPlayerEventActionViewLiveReplay.show();
            return true;
        }
    };

    IMediaPlayer.OnSurfaceListener mOnSurfaceListener = new IMediaPlayer.OnSurfaceListener() {

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {

            Log.i(Constants.LOG_TAG, "surfaceDestroyed");
            mVideoReady = false;
//			mMediaPlayerLiveReplayControllerView.hide();
            mMediaPlayerBufferingView.hide();
            mMediaPlayerLoadingView.hide();
        }

        @Override
        public void surfaceCreated(SurfaceHolder holder) {

            Log.i(Constants.LOG_TAG, "MediaPlayerView surfaceCreated");
        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int w,
                                   int h) {

        }
    };

    public interface PlayerViewCallback {

        void hideViews();

        void restoreViews();

        void onPrepared();

        void onQualityChanged();

        void onFinish(int playMode);

        void onError(int errorCode, String errorMsg);
    }

    private final LiveReplayMediaPlayerControllerView.LiveReplayMediaPlayerController mLiveReplayMediaPlayerController = new LiveReplayMediaPlayerControllerView.LiveReplayMediaPlayerController() {

        private Bitmap bitmap;

        @Override
        public void start() {
            Log.i(Constants.LOG_TAG, " MediaPlayerView  start()  canStart()="
                    + canStart());
            if (canStart()) {
                mLiveReplayMediaPlayerVideoView.start();
                WakeLocker.acquire(getContext());
            }
        }

        @Override
        public void pause() {
            Log.i(Constants.LOG_TAG, " MediaPlayerView  pause() ");
            if (canPause()) {
                mLiveReplayMediaPlayerVideoView.pause();
                WakeLocker.release();
            }

        }

        @Override
        public int getDuration() {

            return mLiveReplayMediaPlayerVideoView.getDuration();
        }

        @Override
        public int getCurrentPosition() {
            if (mIsComplete) {
                return getDuration();
            }
            return mLiveReplayMediaPlayerVideoView.getCurrentPosition();
        }

        @Override
        public void seekTo(long pos) {
            Log.i(Constants.LOG_TAG, " MediaPlayerView  seekTo ");
            if (canSeekBackward() && canSeekForward()) {
                mLiveReplayMediaPlayerVideoView.seekTo(pos);
            } else {
                Toast.makeText(getContext(),
                        "current is real stream, seek is unSupported !",
                        Toast.LENGTH_SHORT).show();
            }
        }

        @Override
        public boolean isPlaying() {

            return mLiveReplayMediaPlayerVideoView.isPlaying();
        }

        @Override
        public int getBufferPercentage() {

            return mLiveReplayMediaPlayerVideoView.getBufferPercentage();
        }

        @Override
        public boolean canPause() {

            Log.i(Constants.LOG_TAG,
                    "can pause ? " + (mLiveReplayMediaPlayerVideoView.canPause()));
            return mLiveReplayMediaPlayerVideoView.canPause();
        }

        @Override
        public boolean canSeekBackward() {

            Log.i(Constants.LOG_TAG, " can Seek Backward ? "
                    + (mLiveReplayMediaPlayerVideoView.canSeekBackward()));
            return mLiveReplayMediaPlayerVideoView.canSeekBackward();
        }

        @Override
        public boolean canSeekForward() {

            Log.i(Constants.LOG_TAG, " can Seek Forward ? "
                    + (mLiveReplayMediaPlayerVideoView.canSeekForward()));
            return mLiveReplayMediaPlayerVideoView.canSeekForward();
        }

        @Override
        public boolean supportQuality() {

            return true;
        }

        @Override
        public boolean supportVolume() {

            return true;
        }

        @Override
        public boolean playVideo(String url) {

            mLiveReplayMediaPlayerVideoView.setVideoPath(url);
            return true;
        }

        @Override
        public int getPlayMode() {

            return mPlayMode;
        }

        @Override
        public void onRequestPlayMode(int requestPlayMode) {

            if (mPlayMode == requestPlayMode)
                return;
            if (mLockMode) {
                return;
            }
            // 请求窗口模式
            if (MediaPlayerUtils.isWindowMode(requestPlayMode)) {
                boolean requestResult = requestPlayMode(requestPlayMode);
                if (requestResult) {
                    doScreenOrientationRotate(ORIENTATION_PORTRAIT_NORMAL);
                }
            }
        }

        @Override
        public void onBackPress(int playMode) {
            Log.i(Constants.LOG_TAG,
                    "========playerview back pressed ==============playMode :"
                            + playMode + ", mPlayerViewCallback is null "
                            + (mPlayerViewCallback == null));
            if (MediaPlayerUtils.isFullScreenMode(playMode)) {
                if (mLockMode) {
                    if (mPlayerViewCallback != null)
                        mPlayerViewCallback.onFinish(playMode);
                } else {
                    mLiveReplayMediaPlayerController
                            .onRequestPlayMode(MediaPlayMode.PLAYMODE_WINDOW);
                }
            } else if (MediaPlayerUtils.isWindowMode(playMode)) {
                if (mPlayerViewCallback != null)
                    mPlayerViewCallback.onFinish(playMode);
            }
        }

        @Override
        public void onControllerShow(int playMode) {

        }

        @Override
        public void onControllerHide(int playMode) {

        }

        @Override
        public void onRequestLockMode(boolean lockMode) {

            if (mScreenLockMode != lockMode) {
                mScreenLockMode = lockMode;

                // 加锁:屏幕操作锁
                if (mScreenLockMode) {
                }
                // 解锁:屏幕操作锁
                else {
                }
            }
        }

        @Override
        public void onVideoPreparing() {
            Log.i(Constants.LOG_TAG, "on video preparing");
            mMediaPlayerLoadingView.setLoadingTip("loading ...");
            mMediaPlayerLoadingView.show();
        }

        @Override
        public boolean canStart() {

            Log.i(Constants.LOG_TAG,
                    "can Start ? " + mLiveReplayMediaPlayerVideoView.canStart());
            return mLiveReplayMediaPlayerVideoView.canStart();
        }

        @Override
        public void onPlay() {
            Log.i(Constants.LOG_TAG, "on play called");
            mMediaPlayerEventActionViewLiveReplay.hide();
            mLiveReplayMediaPlayerControllerView.updateVideoPlaybackState(true);

        }

        @Override
        public void onPause() {
            Log.i(Constants.LOG_TAG, "on pause called");
            mMediaPlayerEventActionViewLiveReplay.hide();
            mLiveReplayMediaPlayerControllerView.updateVideoPlaybackState(false);

        }

        @Override
        public void onMovieRatioChange(int screenSize) {
            mLiveReplayMediaPlayerVideoView.setVideoLayout(screenSize);
            // changeMovieRatio();
        }

        @Override
        public void onMoviePlayRatioUp() {
            Log.d(Constants.LOG_TAG, "speed up");
            if (mLiveReplayMediaPlayerController != null
                    && mLiveReplayMediaPlayerController.isPlaying()) {
                if (mCurrentPlayingRatio == MAX_PLAYING_RATIO) {
                    Log.d(Constants.LOG_TAG, "current playing ratio is max");
                    return;
                } else {
                    mCurrentPlayingRatio = mCurrentPlayingRatio + 0.5f;
                    mLiveReplayMediaPlayerVideoView.setVideoRate(mCurrentPlayingRatio);
                    Log.d(Constants.LOG_TAG, "set playing ratio to --->"
                            + mCurrentPlayingRatio);
                }
            }

            Log.d(Constants.LOG_TAG,
                    "current video is not playing , set ratio unsupported");

        }

        @Override
        public void onMoviePlayRatioDown() {

            if (mLiveReplayMediaPlayerController != null
                    && mLiveReplayMediaPlayerController.isPlaying()) {
                if (mCurrentPlayingRatio == 0) {
                    Log.d(Constants.LOG_TAG, "current playing ratio is 0");
                    return;
                } else {
                    mCurrentPlayingRatio = mCurrentPlayingRatio - 0.5f;
                    mLiveReplayMediaPlayerVideoView.setVideoRate(mCurrentPlayingRatio);
                    Log.d(Constants.LOG_TAG, "set playing ratio to --->"
                            + mCurrentPlayingRatio);
                    return;
                }
            }

            Log.d(Constants.LOG_TAG,
                    "current video is not playing , set ratio unsupported");
        }

    };

    // 延迟操作
    Runnable runnableCrop = new Runnable() {
        @Override
        public void run() {
//			layoutPop.setVisibility(View.GONE);
        }
    };

    private String getCurrentTime() {

        StringBuffer buffer = new StringBuffer();
        SimpleDateFormat sDateFormat = new SimpleDateFormat(
                "yyyy-MM-dd_hh:mm:ss", Locale.US);
        buffer.append(sDateFormat.format(new java.util.Date())).append(".")
                .append("png");
        return buffer.toString();
    }

    private void compressAndSaveBitmapToSDCard(Bitmap rawBitmap,
                                               String fileName, int quality) {

        File directory = new File(Environment.getExternalStorageDirectory()
                + File.separator + MediaPlayerViewLiveReplay.CAPUTRE_SCREEN_PATH_LIVE_REPLAY);
        if (!directory.exists()) {
            directory.mkdir();
        }
        File saveFile = new File(directory, fileName);
        if (!saveFile.exists()) {
            try {
                FileOutputStream fileOutputStream = new FileOutputStream(
                        saveFile);
                if (fileOutputStream != null) {
                    rawBitmap.compress(Bitmap.CompressFormat.PNG, quality,
                            fileOutputStream);
                }
                fileOutputStream.flush();
                fileOutputStream.close();
            } catch (IOException e) {
                e.printStackTrace();

            }
        } else {
            Log.d(Constants.LOG_TAG, "too frequently screen shot");
        }
    }


    @Override
    public void onPowerState(int state) {
        if (powerStateListener != null) {
            this.powerStateListener.onPowerState(state);
        }
    }

    public void registerPowerReceiver() {
        IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_OFF);
        filter.addAction(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_USER_PRESENT);
        mContext.registerReceiver(mBatInfoReceiver, filter);
    }

    public void unregisterPowerReceiver() {
        if (mBatInfoReceiver != null) {
            try {
                mContext.unregisterReceiver(mBatInfoReceiver);
            } catch (Exception e) {
                Log.e(Constants.LOG_TAG,
                        "unregisterReceiver mBatInfoReceiver failure :"
                                + e.getCause());
            }
        }
    }

    private final BroadcastReceiver mBatInfoReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(final Context context, final Intent intent) {
            final String action = intent.getAction();
            if (Intent.ACTION_SCREEN_OFF.equals(action)) {
                Log.d(Constants.LOG_TAG, "screen off");
                if (powerStateListener != null) {
                    powerStateListener.onPowerState(Constants.POWER_OFF);
                }
            } else if (Intent.ACTION_SCREEN_ON.equals(action)) {
                Log.d(Constants.LOG_TAG, "screen on");
                if (powerStateListener != null) {
                    if (isAppOnForeground()) {
                        powerStateListener.onPowerState(Constants.POWER_ON);
                    }
                }
            } else if (Intent.ACTION_USER_PRESENT.equals(action)) {
                if (isAppOnForeground()) {
                    powerStateListener.onPowerState(Constants.USER_PRESENT);
                }
            }
        }
    };
    private boolean isAppShown;

    public boolean isAppOnForeground() {
        ActivityManager am = (ActivityManager) mContext.getSystemService(Context.ACTIVITY_SERVICE);
        ComponentName cn = am.getRunningTasks(1).get(0).topActivity;
        String currentPackageName = cn.getPackageName();
        if (!TextUtils.isEmpty(currentPackageName)
                && currentPackageName.equals(mContext.getPackageName())) {
            return true;
        }
        return false;
    }

}
