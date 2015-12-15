package com.ksy.media.widget.ui.shortvideo;

import android.app.Activity;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Color;
import android.net.TrafficStats;
import android.os.Environment;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.ksy.media.player.IMediaPlayer;
import com.ksy.media.player.util.Constants;
import com.ksy.media.player.util.DRMKey;
import com.ksy.media.player.util.DRMRetrieverManager;
import com.ksy.media.player.util.DRMRetrieverResponseHandler;
import com.ksy.media.player.util.IDRMRetriverRequest;
import com.ksy.media.player.util.NetworkUtil;
import com.ksy.media.widget.controller.MediaPlayerBaseControllerView;
import com.ksy.media.widget.controller.MediaPlayerSmallControllerView;
import com.ksy.media.widget.controller.ShortVideoMediaPlayerControllerView;
import com.ksy.media.widget.data.MediaPlayMode;
import com.ksy.media.widget.data.MediaPlayerUtils;
import com.ksy.media.widget.data.NetReceiver;
import com.ksy.media.widget.data.NetReceiver.NetState;
import com.ksy.media.widget.data.NetReceiver.NetStateChangedListener;
import com.ksy.media.widget.data.WakeLocker;
import com.ksy.media.widget.ui.MediaPlayerBufferingView;
import com.ksy.media.widget.ui.MediaPlayerEventActionView;
import com.ksy.media.widget.ui.MediaPlayerLoadingView;
import com.ksy.media.widget.ui.MediaPlayerMovieRatioView;
import com.ksy.media.widget.util.IPowerStateListener;
import com.ksy.media.widget.videoview.ShortVideoMediaPlayerTextureVideoView;
import com.ksy.media.widget.videoview.ShortVideoMediaPlayerVideoView;
import com.ksy.mediaPlayer.widget.R;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

public class ShortVideoMediaPlayerView extends RelativeLayout implements
        IPowerStateListener {
    private static final int QUALITY_BEST = 100;
    private static final String CAPUTRE_SCREEN_PATH = "KSY_SDK_SCREENSHOT";
    private Activity mActivity;
    private LayoutInflater mLayoutInflater;
    private Window mWindow;
    private volatile boolean mWindowActived = false;

    private ViewGroup mRootView;
    private ShortVideoMediaPlayerTextureVideoView mMediaPlayerVideoView;

    private ShortVideoMediaPlayerControllerView mMediaPlayerSmallControllerView;
    private MediaPlayerBufferingView mMediaPlayerBufferingView;
    private MediaPlayerLoadingView mMediaPlayerLoadingView;
    private MediaPlayerEventActionView mMediaPlayerEventActionView;

    private PlayerViewCallback mPlayerViewCallback;

    private volatile int mPlayMode = MediaPlayMode.PLAYMODE_FULLSCREEN;
    private volatile boolean mLockMode = false;
    private volatile boolean mScreenLockMode = false;
    private volatile boolean mScreenshotPreparing = false;

    private boolean mVideoReady = false;

    private boolean mStartAfterPause = false;

    private int mPausePosition = 0;

    private LayoutParams mMediaPlayerControllerViewSmallParams;

    private boolean mDeviceNavigationBarExist;

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

    private RelativeLayout layoutPop;
    private Handler mHandler = new Handler();

    private TextView mTextViewSpeed;
    private TextView mTextViewDemux;
    private TextView mTextViewDecode;
    private TextView mTextViewTime;

    private TextView mTextViewTotal;
    private TextView mTextViewNet;

    private Timer totalTimer;
    long uidSizeTemp;
    private volatile boolean mStart = false;
    private Context mContext;
    private IPowerStateListener powerStateListener;

    public ShortVideoMediaPlayerView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mContext = context;
        init(context, attrs, defStyle);

    }

    public ShortVideoMediaPlayerView(Context context, AttributeSet attrs) {

        super(context, attrs);
        mContext = context;
        init(context, attrs, -1);

    }

    public ShortVideoMediaPlayerView(Context context) {

        super(context);
        mContext = context;
        init(context, null, -1);

    }

    private void init(Context context, AttributeSet attrs, int defStyle)
            throws IllegalArgumentException, NullPointerException {

        if (null == context)
            throw new NullPointerException("Context can not be null !");

        TypedArray typedArray = context.obtainStyledAttributes(attrs,
                R.styleable.PlayerView);

        this.mLockMode = typedArray.getBoolean(R.styleable.PlayerView_lockmode,
                false);
        typedArray.recycle();

        this.mLayoutInflater = LayoutInflater.from(context);
        this.mActivity = (Activity) context;
        this.mWindow = mActivity.getWindow();

        this.setBackgroundColor(Color.BLACK);
        this.mDeviceNavigationBarExist = MediaPlayerUtils
                .hasNavigationBar(mWindow);

		/* 初始化UI组件 */
        this.mRootView = (ViewGroup) mLayoutInflater.inflate(
                R.layout.short_video_blue_media_player_view, null);

        this.layoutPop = (RelativeLayout) mRootView
                .findViewById(R.id.layoutPop);

//        mTextViewSpeed = (TextView) mRootView.findViewById(R.id.player_speed);
        // mTextViewDemux = (TextView)
        // mRootView.findViewById(R.id.player_demux);
        // mTextViewDecode = (TextView)
        // mRootView.findViewById(R.id.player_decode);
        // mTextViewTime = (TextView) mRootView.findViewById(R.id.player_time);

        // mTextViewTotal = (TextView)
        // mRootView.findViewById(R.id.player_total);
        // mTextViewNet = (TextView) mRootView.findViewById(R.id.player_net);

        this.mMediaPlayerVideoView = (ShortVideoMediaPlayerTextureVideoView) mRootView
                .findViewById(R.id.ks_camera_video_view);
        this.mMediaPlayerBufferingView = (MediaPlayerBufferingView) mRootView
                .findViewById(R.id.ks_camera_buffering_view);
        this.mMediaPlayerLoadingView = (MediaPlayerLoadingView) mRootView
                .findViewById(R.id.ks_camera_loading_view);
        this.mMediaPlayerEventActionView = (MediaPlayerEventActionView) mRootView
                .findViewById(R.id.ks_camera_event_action_view);
        this.mMediaPlayerSmallControllerView = (ShortVideoMediaPlayerControllerView) mRootView
                .findViewById(R.id.media_player_controller_view_small);

		/* 设置播放器监听器 */
        this.mMediaPlayerVideoView.setOnPreparedListener(mOnPreparedListener);
        this.mMediaPlayerVideoView
                .setOnBufferingUpdateListener(mOnPlaybackBufferingUpdateListener);
        this.mMediaPlayerVideoView
                .setOnCompletionListener(mOnCompletionListener);
        this.mMediaPlayerVideoView.setOnInfoListener(mOnInfoListener);
        this.mMediaPlayerVideoView
                .setOnDRMRequiredListener(mOnDRMRequiredListener);
        this.mMediaPlayerVideoView.setOnErrorListener(mOnErrorListener);
        this.mMediaPlayerVideoView.setOnSurfaceListener(mOnSurfaceListener);
        this.mMediaPlayerVideoView
                .setMediaPlayerController(mMediaPlayerController);
        this.mMediaPlayerVideoView
                .setOnSpeedListener(mOnPlaybackNetSpeedListener);
        this.mMediaPlayerVideoView.setOnDebugInfoListener(mOnDebugListener);

        this.mMediaPlayerVideoView.setFocusable(false);
        this.mMediaPlayerVideoView.setCallBack(mStop);
        setPowerStateListener(this.mMediaPlayerVideoView);


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

        // 截图成功layout
        // RelativeLayout.LayoutParams mediaPlayerPopViewParams = new
        // LayoutParams(
        // LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);

        LayoutParams mediaPlayerPopViewParams = new LayoutParams(
                240, 230);
        mediaPlayerPopViewParams.addRule(RelativeLayout.CENTER_IN_PARENT);

		/* 设置eventActionView UI 参数 */
        LayoutParams mediaPlayereventActionViewParams = new LayoutParams(
                LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
        mediaPlayereventActionViewParams
                .addRule(RelativeLayout.CENTER_IN_PARENT);

        this.mMediaPlayerControllerViewSmallParams = new LayoutParams(
                LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);

		/* 设置eventActionView callback */
        this.mMediaPlayerEventActionView
                .setCallback(new MediaPlayerEventActionView.EventActionViewCallback() {

                    @Override
                    public void onActionPlay() {
                        if (NetworkUtil.isNetworkAvailable(mContext)) {
                            mIsComplete = false;
                            Log.i(Constants.LOG_TAG,
                                    "event action  view action play");
                            mMediaPlayerEventActionView.hide();
                            mMediaPlayerLoadingView.hide();
                            mMediaPlayerVideoView.start();
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
                            mMediaPlayerEventActionView.hide();
                            mIsComplete = false;
                            if (mMediaPlayerController != null) {
                                mMediaPlayerController.start();
                            } else {
                                mMediaPlayerVideoView.start();
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
                            mMediaPlayerEventActionView.hide();
                            mMediaPlayerSmallControllerView.hide();
                            mMediaPlayerLoadingView.show();
                            mMediaPlayerVideoView.setVideoPath(url);
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
                        mMediaPlayerController.onBackPress(mPlayMode);
                    }
                });

		/* 初始化:ControllerViewLarge */
        this.mMediaPlayerSmallControllerView
                .setMediaPlayerController(mMediaPlayerController);
        this.mMediaPlayerSmallControllerView.setHostWindow(mWindow);
        this.mMediaPlayerSmallControllerView
                .setDeviceNavigationBarExist(mDeviceNavigationBarExist);
        this.mMediaPlayerSmallControllerView.setNeedGestureDetector(true);
        this.mMediaPlayerSmallControllerView.setNeedGestureAction(false, false,
                false);

        removeAllViews();
        mRootView.removeView(mMediaPlayerVideoView);
        mRootView.removeView(mMediaPlayerBufferingView);
        mRootView.removeView(mMediaPlayerLoadingView);
        mRootView.removeView(mMediaPlayerEventActionView);
        mRootView.removeView(mMediaPlayerSmallControllerView);
        mRootView.removeView(layoutPop);
//        mRootView.removeView(mTextViewSpeed);
        // mRootView.removeView(mTextViewDemux);
        // mRootView.removeView(mTextViewDecode);
        // mRootView.removeView(mTextViewTime);

        // mRootView.removeView(mTextViewTotal);
        // mRootView.removeView(mTextViewNet);

		/* 添加全屏或者是窗口模式初始状态下所需的view */
        addView(mMediaPlayerVideoView, mediaPlayerVideoViewParams);
        addView(mMediaPlayerBufferingView, mediaPlayerBufferingViewParams);
        addView(mMediaPlayerLoadingView, mediaPlayerLoadingViewParams);
        addView(mMediaPlayerEventActionView, mediaPlayereventActionViewParams);
        addView(layoutPop, mediaPlayerPopViewParams);
//        addView(mTextViewSpeed);
        addView(mMediaPlayerSmallControllerView,
                mMediaPlayerControllerViewSmallParams);
        mMediaPlayerSmallControllerView.hide();
        mMediaPlayerBufferingView.hide();
        mMediaPlayerLoadingView.hide();
        mMediaPlayerEventActionView.hide();
        // Default not use,if need it ,open it
        // initOrientationEventListener(context);

   /*     post(new Runnable() {

            @Override
            public void run() {

                if (MediaPlayerUtils.isWindowMode(mPlayMode)) {
                }
            }
        });*/

        mNetReceiver = NetReceiver.getInstance();
        mNetChangedListener = new NetStateChangedListener() {

            @Override
            public void onNetStateChanged(NetState netCode) {

                switch (netCode) {

                    case NET_NO:
                        Log.i(Constants.LOG_TAG, "网络断了");

                        // mTextViewNet.setText(getResources().getString(R.string.play_net)
                        // + " " + "网络断了");

                        // Toast.makeText(getContext(), "网络变化了:没有网络连接",
                        // Toast.LENGTH_LONG).show();
                        break;
                    case NET_2G:
                        Log.i(Constants.LOG_TAG, "2g网络");

                        // mTextViewNet.setText(getResources().getString(R.string.play_net)
                        // + " " + "2G");

                        // Toast.makeText(getContext(), "网络变化了:2g网络",
                        // Toast.LENGTH_LONG).show();
                        break;
                    case NET_3G:
                        Log.i(Constants.LOG_TAG, "3g网络");

                        // mTextViewNet.setText(getResources().getString(R.string.play_net)
                        // + " " + "3G");

                        // Toast.makeText(getContext(), "网络变化了:3g网络",
                        // Toast.LENGTH_LONG).show();
                        break;
                    case NET_4G:
                        Log.i(Constants.LOG_TAG, "4g网络");

                        // mTextViewNet.setText(getResources().getString(R.string.play_net)
                        // + " " + "4G");

                        // Toast.makeText(getContext(), "网络变化了:4g网络",
                        // Toast.LENGTH_LONG).show();
                        break;
                    case NET_WIFI:
                        Log.i(Constants.LOG_TAG, "WIFI网络");

                        // mTextViewNet.setText(getResources().getString(R.string.play_net)
                        // + " " + "Wi-Fi");

                        // Toast.makeText(getContext(), "网络变化了:WIFI网络",
                        // Toast.LENGTH_LONG).show();
                        break;

                    case NET_UNKNOWN:
                        Log.i(Constants.LOG_TAG, "未知网络");

                        // mTextViewNet.setText(getResources().getString(R.string.play_net)
                        // + " " + "未知网络");

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

        // int uid = getUid();
        // getTotalBytes(uid);
    }

    private String url = null;

    public void setPowerStateListener(IPowerStateListener powerStateListener) {
        this.powerStateListener = powerStateListener;
    }

    public void play(String path, boolean isDelay) {

        if (this.mMediaPlayerVideoView != null) {
            Log.d(Constants.LOG_TAG, "play() path =" + path);
            url = path;
            this.mMediaPlayerVideoView.setVideoPath(url);
        }
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (mMediaPlayerEventActionView.isShowing()) {
            return mMediaPlayerEventActionView.dispatchTouchEvent(ev);
        }

        if (mVideoReady && !mMediaPlayerEventActionView.isShowing()) {
            Log.d("eflake", "touch");
            return mMediaPlayerSmallControllerView.dispatchTouchEvent(ev);
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
            if (mPlayerViewCallback != null)
                mPlayerViewCallback.onFinish(mPlayMode);
            return true;

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

    public void onResume() {
        mWindowActived = true;
        mNetReceiver.registNetBroadCast(getContext());
        mNetReceiver.addNetStateChangeListener(mNetChangedListener);
    }

    public void onPause() {

        mNetReceiver.remoteNetStateChangeListener(mNetChangedListener);
        mNetReceiver.unRegistNetBroadCast(getContext());
        mWindowActived = false;

        mPausePosition = mMediaPlayerController.getCurrentPosition();

        if (mMediaPlayerController.isPlaying()) {
            mMediaPlayerController.pause();
            mStartAfterPause = true;
        }
        WakeLocker.release();
    }

    public void onDestroy() {
        mIsComplete = false;
        Log.d(Constants.LOG_TAG, "MediaPlayerView   onDestroy....");
    }

    private void updateVideoInfo2Controller() {

        mMediaPlayerSmallControllerView.updateVideoTitle(getResources().getString(R.string.short_video_title));
        mMediaPlayerEventActionView.updateVideoTitle(getResources().getString(R.string.short_video_title));
    }

    private void changeMovieRatio() {
        /*
         * if (mDisplaySizeMode >
		 * MediaPlayerMovieRatioView.MOVIE_RATIO_MODE_ORIGIN) { mDisplaySizeMode
		 * = MediaPlayerMovieRatioView.MOVIE_RATIO_MODE_16_9; }
		 */

        if (mDisplaySizeMode > MediaPlayerMovieRatioView.MOVIE_RATIO_MODE_4_3) {
            mDisplaySizeMode = MediaPlayerMovieRatioView.MOVIE_RATIO_MODE_16_9;
        }

        mMediaPlayerVideoView.setVideoLayout(mDisplaySizeMode);
        // mDisplaySizeMode++;
    }

    IMediaPlayer.OnPreparedListener mOnPreparedListener = new IMediaPlayer.OnPreparedListener() {

        @Override
        public void onPrepared(IMediaPlayer mp) {
            Log.d(Constants.LOG_TAG,
                    "IMediaPlayer.OnPreparedListener onPrepared");
            int duration = 0;
            if (mMediaPlayerController != null)
                duration = mMediaPlayerController.getDuration();

            if (mIsComplete) {
                mMediaPlayerSmallControllerView.hide();
                mMediaPlayerEventActionView
                        .updateEventMode(
                                MediaPlayerEventActionView.EVENT_ACTION_VIEW_MODE_COMPLETE,
                                null);
                mMediaPlayerEventActionView.show();
                WakeLocker.release();
            }
            if (mPausePosition > 0 && duration > 0) {
                if (!mIsComplete) {
                    mMediaPlayerController.pause();
                    mMediaPlayerController.seekTo(mPausePosition);
                    mPausePosition = 0;
                }

            }
            if (!WakeLocker.isScreenOn(getContext())
                    && mMediaPlayerController.canPause()) {
                if (!mIsComplete) {
                    mMediaPlayerController.pause();
                }
            }
            updateVideoInfo2Controller();
            mMediaPlayerLoadingView.hide();

            if (!mIsComplete) {
                mMediaPlayerVideoView.start();
            }

            // mMediaPlayerEventActionView.updateEventMode(
            // MediaPlayerEventActionView.EVENT_ACTION_VIEW_MODE_WAIT,
            // null);
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
                mMediaPlayerEventActionView.hide();
                if (mMediaPlayerController != null) {
                    mMediaPlayerController.start();
                } else {
                    mMediaPlayerVideoView.start();
                }
            } else {
                mIsComplete = true;
                mMediaPlayerSmallControllerView.hide();
                mMediaPlayerEventActionView
                        .updateEventMode(
                                MediaPlayerEventActionView.EVENT_ACTION_VIEW_MODE_COMPLETE,
                                null);
                mMediaPlayerEventActionView.show();
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

                    mMediaPlayerVideoView.setDRMKey(version, cek);
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
//            mTextViewSpeed.setText(getResources().getString(R.string.net_speed)
//                    + " " + arg2 + " bit/s");
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
            mMediaPlayerSmallControllerView.hide();
            mMediaPlayerBufferingView.hide();
            mMediaPlayerLoadingView.hide();
            mMediaPlayerEventActionView.updateEventMode(
                    MediaPlayerEventActionView.EVENT_ACTION_VIEW_MODE_ERROR,
                    what + "," + extra);
            mMediaPlayerEventActionView.show();
            return true;
        }
    };

    IMediaPlayer.OnSurfaceListener mOnSurfaceListener = new IMediaPlayer.OnSurfaceListener() {

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {

            Log.i(Constants.LOG_TAG, "surfaceDestroyed");
            mVideoReady = false;
            mMediaPlayerSmallControllerView.hide();
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

    private final MediaPlayerBaseControllerView.MediaPlayerController mMediaPlayerController = new MediaPlayerBaseControllerView.MediaPlayerController() {

        private Bitmap bitmap;

        @Override
        public void start() {
            Log.i(Constants.LOG_TAG, " MediaPlayerView  start()  canStart()="
                    + canStart());
            if (canStart()) {
                mMediaPlayerVideoView.start();
                WakeLocker.acquire(getContext());
            }
        }

        @Override
        public void pause() {
            Log.i(Constants.LOG_TAG, " MediaPlayerView  pause() ");
            if (canPause()) {
                mMediaPlayerVideoView.pause();
                WakeLocker.release();
            }

        }

        @Override
        public int getDuration() {

            return mMediaPlayerVideoView.getDuration();
        }

        @Override
        public int getCurrentPosition() {
            if (mIsComplete) {
                return getDuration();
            }
            return mMediaPlayerVideoView.getCurrentPosition();
        }

        @Override
        public void seekTo(long pos) {
            Log.i(Constants.LOG_TAG, " MediaPlayerView  seekTo ");
            if (canSeekBackward() && canSeekForward()) {
                mMediaPlayerVideoView.seekTo(pos);
            } else {
                Toast.makeText(getContext(),
                        "current is real stream, seek is unSupported !",
                        Toast.LENGTH_SHORT).show();
            }
        }

        @Override
        public boolean isPlaying() {

            return mMediaPlayerVideoView.isPlaying();
        }

        @Override
        public int getBufferPercentage() {

            return mMediaPlayerVideoView.getBufferPercentage();
        }

        @Override
        public boolean canPause() {

            Log.i(Constants.LOG_TAG,
                    "can pause ? " + (mMediaPlayerVideoView.canPause()));
            return mMediaPlayerVideoView.canPause();
        }

        @Override
        public boolean canSeekBackward() {

            Log.i(Constants.LOG_TAG, " can Seek Backward ? "
                    + (mMediaPlayerVideoView.canSeekBackward()));
            return mMediaPlayerVideoView.canSeekBackward();
        }

        @Override
        public boolean canSeekForward() {

            Log.i(Constants.LOG_TAG, " can Seek Forward ? "
                    + (mMediaPlayerVideoView.canSeekForward()));
            return mMediaPlayerVideoView.canSeekForward();
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

            mMediaPlayerVideoView.setVideoPath(url);
            return true;
        }

        @Override
        public int getPlayMode() {

            return mPlayMode;
        }

        @Override
        public void onRequestPlayMode(int requestPlayMode) {
            Log.d("eflake","onRequestPlayMode"+requestPlayMode);
        }

        @Override
        public void onBackPress(int playMode) {
            Log.i(Constants.LOG_TAG,
                    "========playerview back pressed ==============playMode :"
                            + playMode + ", mPlayerViewCallback is null "
                            + (mPlayerViewCallback == null));
            if (mPlayerViewCallback != null)
                mPlayerViewCallback.onFinish(playMode);
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
                    "can Start ? " + mMediaPlayerVideoView.canStart());
            return mMediaPlayerVideoView.canStart();
        }

        @Override
        public void onPlay() {

            Log.i(Constants.LOG_TAG, "on play called");
            mMediaPlayerEventActionView.hide();
            mMediaPlayerSmallControllerView.updateVideoPlaybackState(true);

        }

        @Override
        public void onPause() {

            Log.i(Constants.LOG_TAG, "on pause called");
            mMediaPlayerEventActionView.hide();
            mMediaPlayerSmallControllerView.updateVideoPlaybackState(false);

        }

        @Override
        public void onMovieRatioChange(int screenSize) {

            mMediaPlayerVideoView.setVideoLayout(screenSize);
            // changeMovieRatio();
        }

        @Override
        public void onMoviePlayRatioUp() {

            Log.d(Constants.LOG_TAG, "speed up");
            if (mMediaPlayerController != null
                    && mMediaPlayerController.isPlaying()) {
                if (mCurrentPlayingRatio == MAX_PLAYING_RATIO) {
                    Log.d(Constants.LOG_TAG, "current playing ratio is max");
                    return;
                } else {
                    mCurrentPlayingRatio = mCurrentPlayingRatio + 0.5f;
                    mMediaPlayerVideoView.setVideoRate(mCurrentPlayingRatio);
                    Log.d(Constants.LOG_TAG, "set playing ratio to --->"
                            + mCurrentPlayingRatio);
                }
            }

            Log.d(Constants.LOG_TAG,
                    "current video is not playing , set ratio unsupported");

        }

        @Override
        public void onMoviePlayRatioDown() {

            if (mMediaPlayerController != null
                    && mMediaPlayerController.isPlaying()) {
                if (mCurrentPlayingRatio == 0) {
                    Log.d(Constants.LOG_TAG, "current playing ratio is 0");
                    return;
                } else {
                    mCurrentPlayingRatio = mCurrentPlayingRatio - 0.5f;
                    mMediaPlayerVideoView.setVideoRate(mCurrentPlayingRatio);
                    Log.d(Constants.LOG_TAG, "set playing ratio to --->"
                            + mCurrentPlayingRatio);
                    return;
                }
            }

            Log.d(Constants.LOG_TAG,
                    "current video is not playing , set ratio unsupported");
        }

        @Override
        public void onMovieCrop() {
            if (!mScreenshotPreparing) {
                mScreenshotPreparing = true;
                bitmap = Bitmap.createBitmap(
                        mMediaPlayerVideoView.getVideoWidth(),
                        mMediaPlayerVideoView.getVideoHeight(),
                        Config.ARGB_8888);
                if (bitmap != null) {
                    mMediaPlayerVideoView.getCurrentFrame(bitmap);
                    compressAndSaveBitmapToSDCard(bitmap, getCurrentTime(),
                            ShortVideoMediaPlayerView.QUALITY_BEST);
                    /*
                     * Toast.makeText( getContext(),
					 * "screenshoot saved in path :/storage/emulated/0/KSY_SDK_SCREENSHOT"
					 * , Toast.LENGTH_SHORT).show();
					 */

                    layoutPop.setVisibility(View.VISIBLE);
                    mHandler.postDelayed(runnableCrop, 1000);

                    mScreenshotPreparing = false;
                } else {
                    Log.d(Constants.LOG_TAG, "bitmap is null");
                }
            }

        }

        @Override
        public void onVolumeDown() {
            Log.d(Constants.LOG_TAG, "audio down");
            if (mMediaPlayerController != null
                    && mMediaPlayerController.isPlaying()) {
                if (mCurrentPlayingVolumeRatio == 0) {
                    Log.d(Constants.LOG_TAG, "current playing volume is 0");
                    return;
                } else {
                    mCurrentPlayingVolumeRatio = mCurrentPlayingVolumeRatio - 0.5f;
                    mMediaPlayerVideoView
                            .setAudioAmplify(mCurrentPlayingVolumeRatio);
                    Log.d(Constants.LOG_TAG, "set playing volume to --->"
                            + mCurrentPlayingVolumeRatio);
                    return;
                }
            }
        }

        @Override
        public void onVolumeUp() {
            Log.d(Constants.LOG_TAG, "audio up");
            if (mMediaPlayerController != null
                    && mMediaPlayerController.isPlaying()) {
                if (mCurrentPlayingVolumeRatio == MAX_PLAYING_VOLUME_RATIO) {
                    Log.d(Constants.LOG_TAG, "current playing ratio is max");
                    return;
                } else {
                    mCurrentPlayingVolumeRatio = mCurrentPlayingVolumeRatio + 0.5f;
                    mMediaPlayerVideoView
                            .setAudioAmplify(mCurrentPlayingVolumeRatio);
                    Log.d(Constants.LOG_TAG, "set playing volume to --->"
                            + mCurrentPlayingVolumeRatio);
                }
            }
        }
    };

    // 延迟操作
    Runnable runnableCrop = new Runnable() {
        @Override
        public void run() {
            layoutPop.setVisibility(View.GONE);
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
                + File.separator + ShortVideoMediaPlayerView.CAPUTRE_SCREEN_PATH);
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

    // start get total bytes
    public void getTotalBytes(final int id) {
        if (mStart) {
            return;
        }
        mStart = true;

        totalTimer = new Timer();
        totalTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                try {

                    uidSizeTemp = TrafficStats.getUidRxBytes(id);
                    Log.d(Constants.LOG_TAG, "uidSizeTemp=" + uidSizeTemp);
                    String totalSize = convertFileSize(uidSizeTemp);
                    Log.d(Constants.LOG_TAG, "totalSize==" + totalSize);
                    // mTextViewTotal.setText(getResources().getString(R.string.consumption_flow)
                    // + " " + totalSize);

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }, 1000, 1000);
    }

    // stop timer
    public void stopTotalTimer() {
        if (!mStart) {
            return;
        }

        if (null != totalTimer) {
            totalTimer.cancel();
        }

        mStart = false;
    }

    // convert size
    public static String convertFileSize(long size) {
        long kb = 1024;
        long mb = kb * 1024;
        long gb = mb * 1024;

        if (size >= gb) {
            return String.format("%.1f GB", (float) size / gb);
        } else if (size >= mb) {
            float f = (float) size / mb;
            return String.format(f > 100 ? "%.0f MB" : "%.1f MB", f);
        } else if (size >= kb) {
            float f = (float) size / kb;
            return String.format(f > 100 ? "%.0f KB" : "%.1f KB", f);
        } else {
            return String.format("%d B", size);
        }
    }

    public interface IStop {
        void stopTimer();
    }

    // stop timer
    IStop mStop = new IStop() {
        @Override
        public void stopTimer() {
            stopTotalTimer();

            uidSizeTemp = 0;
        }
    };

    // get app uid
    public int getUid() {
        int uid;

        try {
            PackageManager pm = mContext.getPackageManager();
            ApplicationInfo ai = pm.getApplicationInfo("com.ksy.media.demo",
                    PackageManager.GET_ACTIVITIES);
            uid = ai.uid;
        } catch (NameNotFoundException e) {
            e.printStackTrace();
            uid = -1;
        }

        return uid;
    }

    @Override
    public void onPowerState(int state) {
        if (powerStateListener != null) {
            this.powerStateListener.onPowerState(state);
        }
    }

}
