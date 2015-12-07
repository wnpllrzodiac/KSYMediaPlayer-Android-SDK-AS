package com.ksy.recordlib.service.core;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Camera;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.ViewGroup;

import com.ksy.recordlib.service.exception.KsyRecordException;
import com.ksy.recordlib.service.recoder.RecoderAudioSource;
import com.ksy.recordlib.service.recoder.RecoderVideoSource;
import com.ksy.recordlib.service.recoder.RecoderVideoTempSource;
import com.ksy.recordlib.service.rtmp.KSYRtmpFlvClient;
import com.ksy.recordlib.service.util.CameraUtil;
import com.ksy.recordlib.service.util.Constants;
import com.ksy.recordlib.service.util.OrientationActivity;

import java.io.IOException;
import java.util.List;

/**
 * Created by eflakemac on 15/6/17.
 */
public class KsyRecordClient implements KsyRecord {


    private static final String TAG = "KsyRecordClient";
    private static KsyRecordClient mInstance;
    private RecordHandler mRecordHandler;
    private Context mContext;
    private int mEncodeMode = Constants.ENCODE_MODE_MEDIA_RECORDER;
    private static KsyRecordClientConfig mConfig;
    private Camera mCamera;
    private KSYRtmpFlvClient mKsyRtmpFlvClient;
    private SurfaceView mSurfaceView;
    private TextureView mTextureView;
    private RecoderVideoSource mVideoSource;
    private KsyMediaSource mAudioSource;
    private KsyMediaSource mVideoTempSource;

    private KsyRecordSender ksyRecordSender;

    private OrientationActivity orientationActivity;

    private STATE clientState = STATE.STOP;

    private int displayOrientation;
    private int currentCameraId;
    private CameraSizeChangeListener mCameraSizeChangedListener;
    private NetworkChangeListener mNetworkChangeListener;
    private PushStreamStateListener mPushStreamStateListener;
    private SwitchCameraStateListener mSwitchCameraStateListener;
    public static final int NETWORK_UNAVAILABLE = -1;
    public static final int NETWORK_WIFI = 1;
    public static final int NETWORK_MOBILE = 0;
    private boolean mSwitchCameraLock = false;


    private enum STATE {
        RECORDING, STOP, PAUSE, ERROR
    }

    public interface CameraSizeChangeListener {
        void onCameraSizeChanged(int width, int height);

        void onCameraPreviewSize(int width, int height);
    }

    public interface NetworkChangeListener {
        void onNetworkChanged(int state);
    }

    public interface PushStreamStateListener {
        void onPushStreamState(int state);
    }
    public interface SwitchCameraStateListener {
        void onSwitchCameraDisable();
        void onSwitchCameraEnable();
    }

    private KsyRecordClient() {
    }

    private KsyRecordClient(Context context) {
        this.mContext = context;
        mRecordHandler = new RecordHandler();
        ksyRecordSender = KsyRecordSender.getRecordInstance();
        ksyRecordSender.setStateMonitor(mRecordHandler);
        // Remove old network monitor
        // NetworkMonitor.start(context);
    }

    public void registerNetworkMonitor() {
        // Monitor network
        IntentFilter networkFilter = new IntentFilter();
        networkFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        this.mContext.registerReceiver(mReceiver, networkFilter);
    }

    public void unregisterNetworkMonitor() {
        if (mReceiver != null) {
            this.mContext.unregisterReceiver(mReceiver);
        }
    }


    public static KsyRecordClient getInstance(Context context) {
        if (mInstance == null) {
            mInstance = new KsyRecordClient(context);
        }
        return mInstance;
    }

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
                ConnectivityManager mConnMgr = (ConnectivityManager)
                        context.getSystemService(Context.CONNECTIVITY_SERVICE);
                NetworkInfo aActiveInfo = mConnMgr.getActiveNetworkInfo();
                if (aActiveInfo != null && aActiveInfo.isAvailable()) {
                    // Network available
                    int type = aActiveInfo.getType();
                    if (type == ConnectivityManager.TYPE_WIFI) {
                        // Wifi available
                        NetworkInfo wifiInfo = mConnMgr.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
                        mNetworkChangeListener.onNetworkChanged(KsyRecordClient.NETWORK_WIFI);
                    } else if (type == ConnectivityManager.TYPE_MOBILE) {
                        // Mobile Network available
                        NetworkInfo mobileInfo = mConnMgr.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
                        mNetworkChangeListener.onNetworkChanged(KsyRecordClient.NETWORK_MOBILE);
                    } else {
                        // Other network
                    }

                } else {
                    // Network unavailable
                    stopRecord();
                    mNetworkChangeListener.onNetworkChanged(KsyRecordClient.NETWORK_UNAVAILABLE);
                }
            }
        }
    };

    public KsyRecordClient setOrientationActivity(OrientationActivity activity) {
        this.orientationActivity = activity;
        return this;
    }

    public void setCameraSizeChangedListener(CameraSizeChangeListener listener) {
        this.mCameraSizeChangedListener = listener;
    }

    public void setNetworkChangeListener(NetworkChangeListener listener) {
        this.mNetworkChangeListener = listener;
    }

    public void setPushStreamStateListener(PushStreamStateListener mPushStreamStateListener) {
        this.mPushStreamStateListener = mPushStreamStateListener;
    }

    public void setSwitchCameraStateListener(SwitchCameraStateListener mSwitchCameraStateListener) {
        this.mSwitchCameraStateListener = mSwitchCameraStateListener;
    }

    /*
            *
            * Ks3 Record API
            * */
    @Override
    public void startRecord() throws KsyRecordException {
        if (clientState == STATE.RECORDING) {
            return;
        }
        mEncodeMode = judgeEncodeMode(mContext);
        try {
            mConfig.setOrientationActivity(orientationActivity);
            ksyRecordSender.start(mContext);
        } catch (IOException e) {
            e.printStackTrace();
            Log.e(Constants.LOG_TAG, "startRecord() : e =" + e);
        }

        if (checkConfig()) {
            // Here we begin
            if (mEncodeMode == Constants.ENCODE_MODE_MEDIA_RECORDER) {
                setUpMp4Config(mRecordHandler);
            } else {
//                startRecordStep();
            }
        } else {
            throw new KsyRecordException("Check KsyRecordClient Configuration, param should be correct");
        }
        clientState = STATE.RECORDING;
    }

    private void startRecordStep() {
        setUpCamera(true);
        setUpEncoder();
    }


    private void setUpMp4Config(RecordHandler mRecordHandler) {
        setUpCamera(true);
        if (mVideoTempSource == null) {
            mVideoTempSource = new RecoderVideoTempSource(mCamera, mConfig, mSurfaceView, mRecordHandler, mContext);
            mVideoTempSource.start();
        }
    }

    private void startRtmpFlvClient() throws KsyRecordException {
        mKsyRtmpFlvClient = new KSYRtmpFlvClient(mConfig.getUrl());
        try {
            mKsyRtmpFlvClient.start();
        } catch (IOException e) {
            throw new KsyRecordException("start muxer failed");
        }
    }

    private boolean checkConfig() throws KsyRecordException {
        if (mConfig == null) {
            throw new KsyRecordException("should set KsyRecordConfig first");
        }
        return mConfig.validateParam();
    }


    private void setUpCamera(boolean needPreview) {

        if (mCamera == null) {
            int numberOfCameras = Camera.getNumberOfCameras();
            if (numberOfCameras > 0) {
                Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
                for (int i = 0; i < numberOfCameras; i++) {
                    Camera.getCameraInfo(i, cameraInfo);
                    if (cameraInfo.facing == mConfig.getCameraType()) {
                        mCamera = Camera.open(i);
                        currentCameraId = i;
                    }
                }
            } else {
                mCamera = Camera.open();
            }
            displayOrientation = CameraUtil.getDisplayOrientation(0, currentCameraId);
            KsyRecordClientConfig.previewOrientation = displayOrientation;
            Log.d(Constants.LOG_TAG_EF, "current displayOrientation = " + displayOrientation);
            mCamera.setDisplayOrientation(displayOrientation);
            Camera.Parameters parameters = mCamera.getParameters();
            if (mCameraSizeChangedListener != null)
                mCameraSizeChangedListener.onCameraPreviewSize(parameters.getPreviewSize().width, parameters.getPreviewSize().height);
            parameters.setRotation(0);
            List<Camera.Size> mSupportedPreviewSizes = parameters.getSupportedPreviewSizes();
            Camera.Size optimalSize = CameraHelper.getOptimalPreviewSize(mSupportedPreviewSizes,
                    mSurfaceView.getHeight(), mSurfaceView.getWidth());
            ViewGroup.LayoutParams params = mSurfaceView.getLayoutParams();

            if (parameters.getSupportedFocusModes().contains(
                    Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
                parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
            }
            mCamera.setParameters(parameters);
            if (needPreview) {
                params.height = optimalSize.height;
                params.width = optimalSize.width;
                mSurfaceView.setLayoutParams(params);
                parameters.setPreviewSize(optimalSize.width, optimalSize.height);
                try {
                    if (mSurfaceView != null) {
                        mCamera.setPreviewDisplay(mSurfaceView.getHolder());
                    } else if (mTextureView != null) {
                        mCamera.setPreviewTexture(mTextureView.getSurfaceTexture());
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        // Here we reuse camera, just unlock it
        mCamera.unlock();
    }

    private void setUpEncoder() {
        switch (mEncodeMode) {
            case Constants.ENCODE_MODE_MEDIA_RECORDER:
                DealWithMediaRecorder();
                break;
            case Constants.ENCODE_MODE_MEDIA_CODEC:
                DealWithMediaCodec();
                break;
            case Constants.ENCODE_MODE_WEBRTC:
                DealWithWebRTC();
                break;
            default:
                break;
        }
    }

    // Encode using MediaRecorder
    private void DealWithMediaRecorder() {
        Log.d(Constants.LOG_TAG, "DealWithMediaRecorder");
        // Video Source
        if (mVideoSource == null) {
            mVideoSource = new RecoderVideoSource(mCamera, mConfig, mSurfaceView, mRecordHandler, mContext);
            mVideoSource.start();
        }

        // Audio Source
        if (mAudioSource == null) {
            mAudioSource = new RecoderAudioSource(mConfig, mRecordHandler, mContext);
            mAudioSource.start();
        }

    }

    // Encode using MediaCodec
    // to do
    private void DealWithMediaCodec() {
        Log.d(Constants.LOG_TAG, "DealWithMediaCodec");

    }

    // Encode using WebRTC
    // to do
    private void DealWithWebRTC() {
        Log.d(Constants.LOG_TAG, "DealWithWebRTC");

    }

    private int judgeEncodeMode(Context context) {
        // to do
        return Constants.ENCODE_MODE_MEDIA_RECORDER;
    }


    @Override
    public void stopRecord() {
        if (clientState != STATE.RECORDING) {
            return;
        }
        if (mVideoSource != null) {
            mVideoSource.stop();
            mVideoSource = null;
        }
        if (mVideoTempSource != null) {
            mVideoTempSource.stop();
            mVideoTempSource = null;
        }
        if (mAudioSource != null) {
            mAudioSource.stop();
            mAudioSource = null;
        }
        if (mCamera != null) {
            mCamera.release();
            mCamera = null;
        }
        ksyRecordSender.disconnect();
        clientState = STATE.STOP;
    }

    @Override
    public void release() {
        if (mVideoSource != null) {
            mVideoSource.release();
            mVideoSource = null;
        }
        if (mVideoTempSource != null) {
            mVideoTempSource.release();
            mVideoTempSource = null;
        }
        if (mAudioSource != null) {
            mAudioSource.release();
            mAudioSource = null;
        }
        if (mCamera != null) {
            mCamera.release();
            mCamera = null;
        }
    }

    @Override
    public void switchCamera() {
        if (!mSwitchCameraLock) {
            setSwitchCameraState(true);
            mSwitchCameraStateListener.onSwitchCameraDisable();
            if (mVideoSource != null) {
                mVideoSource.close();
                mVideoSource = null;
            }
            if (mVideoTempSource != null) {
                mVideoTempSource.release();
                mVideoTempSource = null;
            }
            if (mCamera != null) {
                mCamera.release();
                mCamera = null;
            }
            if (mConfig.getCameraType() == Camera.CameraInfo.CAMERA_FACING_BACK) {
                mConfig.setmCameraType(Camera.CameraInfo.CAMERA_FACING_FRONT);
            } else {
                mConfig.setmCameraType(Camera.CameraInfo.CAMERA_FACING_BACK);
            }

            RecoderVideoSource.sync.setForceSyncFlay(true);
            startRecordStep();
//        ksyRecordSender.clearData();
            KsyRecordSender.getRecordInstance().needResetTs = true;
        } else {
            //current is switching
        }
    }

    @Override
    public int getNewtWorkStatusType() {
        return 0;
    }

    @Override
    public void setDisplayPreview(SurfaceView surfaceView) {
        if (mConfig == null) {
            throw new IllegalStateException("should set KsyRecordConfig before invoke setDisplayPreview");
        }
        this.mSurfaceView = surfaceView;
        this.mTextureView = null;
    }

    @Override
    public void setDisplayPreview(TextureView textureView) {
        if (mConfig == null) {
            throw new IllegalStateException("should set KsyRecordConfig before invoke setDisplayPreview");
        }
        this.mTextureView = textureView;
        this.mSurfaceView = null;
    }

    public class RecordHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case Constants.MESSAGE_MP4CONFIG_FINISH:
                    Log.d(Constants.LOG_TAG, "back to continue");
                    //release();
                    // just release tem
                    if (mVideoTempSource != null) {
                        // already release
                        mVideoTempSource.release();
                        mVideoTempSource = null;
                    }
                    startRecordStep();
                    break;
                case Constants.MESSAGE_MP4CONFIG_START_PREVIEW:
                    break;
                case Constants.MESSAGE_SWITCH_CAMERA_FINISH:
                    setSwitchCameraState(false);
                    mSwitchCameraStateListener.onSwitchCameraEnable();
                    break;
                case Constants.MESSAGE_SENDER_PUSH_FAILED:
                    Log.d(Constants.LOG_TAG_EF, "server send push fail");
                    if (mPushStreamStateListener != null) {
                        mPushStreamStateListener.onPushStreamState(Constants.PUSH_STATE_FAILED);
                    }
                    break;
                default:
                    break;
            }
        }
    }

    public static void setConfig(KsyRecordClientConfig mConfig) {
        KsyRecordClient.mConfig = mConfig;
    }

    public void setSwitchCameraState(boolean switchCameraState) {
        mSwitchCameraLock = switchCameraState;
    }
}

