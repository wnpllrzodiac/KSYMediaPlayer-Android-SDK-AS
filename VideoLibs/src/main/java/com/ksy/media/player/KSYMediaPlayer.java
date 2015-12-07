package com.ksy.media.player;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Bitmap;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;

import com.ksy.media.player.KSYMediaMeta.IjkStreamMeta;
import com.ksy.media.player.annotations.AccessedByNative;
import com.ksy.media.player.annotations.CalledByNative;
import com.ksy.media.player.exception.Ks3ClientException;
import com.ksy.media.player.log.LogClient;
import com.ksy.media.player.option.AvFormatOption;
import com.ksy.media.player.pragma.DebugLog;
import com.ksy.media.player.util.Constants;
import com.ksy.media.player.util.IOUtils;

public final class KSYMediaPlayer extends BaseMediaPlayer {

	private static final int MP_STATE_END = 9;
	private static final int MEDIA_NOP = 0; // interface test message
	private static final int MEDIA_PREPARED = 1;
	private static final int MEDIA_PLAYBACK_COMPLETE = 2;
	private static final int MEDIA_BUFFERING_UPDATE = 3;
	private static final int MEDIA_SEEK_COMPLETE = 4;
	private static final int MEDIA_SET_VIDEO_SIZE = 5;
	private static final int MEDIA_TIMED_TEXT = 99;
	private static final int MEDIA_ERROR = 100;
	private static final int MEDIA_GET_DRM_KEY = 101;
	private static final int MEDIA_PLAYBACK_STATE_CHANGED = 102;
	private static final int MEDIA_GET_SPEED = 103;
	private static final int MEDIA_INFO = 200;
	protected static final int MEDIA_SET_VIDEO_SAR = 10001;
	protected static final int MEDIA_DEMUX_CACHE_DATA = 10002;
	protected static final int MEDIA_DECODE_CACHE_DATA = 10003;
	protected static final int MEDIA_PLAY_TIME = 10004;

	// public static final int MEDIA_ERROR_TIMEOUT = 10002;
	// public static final int MEDIA_ERROR_UNSUPPORTED = 10003;
	// public static final int MEDIA_ERROR_NOFILE = 10004;
	// public static final int MEDIA_ERROR_SEEKUNSUPPORT = 10005;
	public static final int MEDIA_ERROR_SEEKUNREACHABLE = 10006;
	// public static final int MEDIA_ERROR_DRM = 10007;
	// public static final int MEDIA_ERROR_MEMORY = 10008;
	// public static final int MEDIA_ERROR_WRONGPARAM = 10009;
	public static final int MEDIA_ERROR_DEMUX_FAIL = 10010;

	@AccessedByNative
	private long mNativeMediaPlayer;

	@AccessedByNative
	private int mNativeSurfaceTexture;

	@AccessedByNative
	private int mListenerContext;

	private SurfaceHolder mSurfaceHolder;
	private EventHandler mEventHandler;
	private PowerManager.WakeLock mWakeLock = null;
	private boolean mScreenOnWhilePlaying;
	private boolean mStayAwake;

	private int mVideoWidth;
	private int mVideoHeight;
	private int mVideoSarNum;
	private int mVideoSarDen;

	private String mDataSource;
	private String mFFConcatContent;

	private long prepare;
	private long start;
	private Context mContext;
	private int mGetUsageTime;

	private static KSYLibLoader sLocalLibLoader = new KSYLibLoader() {
		@Override
		public void loadLibrary(String libName) throws UnsatisfiedLinkError,
				SecurityException {
			System.loadLibrary(libName);
		}
	};

	private static volatile boolean mIsLibLoaded = false;

	public static void loadLibrariesOnce(KSYLibLoader libLoader) {
		synchronized (KSYMediaPlayer.class) {
			if (!mIsLibLoaded) {
				// libLoader.loadLibrary("gnustl_shared");
				libLoader.loadLibrary("ksyffmpeg");
				libLoader.loadLibrary("ksyutil");
				libLoader.loadLibrary("ksysdl");
				libLoader.loadLibrary("ksyplayer");
				mIsLibLoaded = true;
			}
		}
	}

	private static volatile boolean mIsNativeInitialized = false;

	private static void initNativeOnce() {
		synchronized (KSYMediaPlayer.class) {
			if (!mIsNativeInitialized) {
				// 不需要使用，空函数
				// native_init();
				mIsNativeInitialized = true;
			}
		}
	}

	public KSYMediaPlayer() {
		this(sLocalLibLoader);
	}

	public KSYMediaPlayer(Context context, int time, boolean isOpenSwitch,
			String companyKey) {
		this(sLocalLibLoader);
		mContext = context;

		// 是否打开日志开关 true:打开， false:关闭
		logClient.mSwitch = isOpenSwitch;
		// 设置key，需要自己传，只能是 字母（不分大小写），其他的都是无效的
		logRecord.setCompanyKey(companyKey);
		// 多久获取一次cpu占用率，内存占用等 实时数据
		mGetUsageTime = time;

		logClient = LogClient.getInstance(context.getApplicationContext());
		logClient.start();
		logClient.addData();

	}

	public KSYMediaPlayer(KSYLibLoader libLoader) {
		initPlayer(libLoader);
	}

	private void initPlayer(KSYLibLoader libLoader) {

		loadLibrariesOnce(libLoader);
		initNativeOnce();

		Looper looper;
		if ((looper = Looper.myLooper()) != null) {
			mEventHandler = new EventHandler(this, looper);
		} else if ((looper = Looper.getMainLooper()) != null) {
			mEventHandler = new EventHandler(this, looper);
		} else {
			mEventHandler = null;
		}
		Log.d(Constants.LOG_TAG, "native_setup start ");
		native_setup(new WeakReference<KSYMediaPlayer>(this));
	}

	private native void _setVideoSurface(Surface surface);

	@Override
	public void setDisplay(SurfaceHolder sh) {

		mSurfaceHolder = sh;
		Surface surface;
		if (sh != null) {
			surface = sh.getSurface();
		} else {
			surface = null;
		}
		_setVideoSurface(surface);
		updateSurfaceScreenOn();
	}

	@Override
	public void setSurface(Surface surface) {

		if (mScreenOnWhilePlaying && surface != null) {
			DebugLog.w(Constants.LOG_TAG,
					"setScreenOnWhilePlaying(true) is ineffective for Surface");
		}
		mSurfaceHolder = null;
		_setVideoSurface(surface);
		updateSurfaceScreenOn();
	}

	@Override
	public void setDataSource(String path) throws IOException,
			IllegalArgumentException, SecurityException, IllegalStateException {

		mDataSource = path;
		_setDataSource(path, null, null);
	}

	private native void _setDataSource(String path, String[] keys,
			String[] values) throws IOException, IllegalArgumentException,
			SecurityException, IllegalStateException;

	@Override
	public void setDataSource(String path, Map<String, String> headers)
			throws IOException, IllegalArgumentException, SecurityException,
			IllegalStateException {
		StringBuffer buffer = new StringBuffer();
		if (headers != null && headers.size() > 0) {
			for (Entry<String, String> entry : headers.entrySet()) {
				buffer.append(entry.getKey().toString()).append(":")
						.append(entry.getValue().toString()).append("\r\n");
			}
		}
		mDataSource = path;
		_setDataSourceAndHeader(path, buffer.toString());
		Log.d(Constants.LOG_TAG, buffer.toString());
	}

	private native void _setDataSourceAndHeader(String path, String headers)
			throws IOException, IllegalArgumentException, SecurityException,
			IllegalStateException;

	@Override
	public String getDataSource() {

		return mDataSource;
	}

	@Override
	public void setCacheInPause(boolean useCacheInPause) throws IOException,
			IllegalArgumentException, SecurityException, IllegalStateException {
		_setCacheInPause(useCacheInPause);
	}

	private native void _setCacheInPause(boolean useCacheInPause)
			throws IOException, IllegalArgumentException, SecurityException,
			IllegalStateException;

	public void setDataSourceAsFFConcatContent(String ffConcatContent) {

		mFFConcatContent = ffConcatContent;
	}

	@Override
	public void prepareAsync() throws IllegalStateException {
		prepare = System.currentTimeMillis();

		if (logClient.mSwitch) {
			logClient.saveUsageData(mGetUsageTime);
			try {
				Log.d(Constants.LOG_TAG,
						"BaseData = " + logRecord.getBaseDataJson());
				// Log.d(Constants.LOG_TAG, "PlayStatus = " +
				// logRecord.getPlayStatusJson());
				logRecord.setDate(logClient.getInstance().logGetData
						.currentTimeGmt());
				logClient.put(logRecord.getBaseDataJson());
			} catch (Ks3ClientException e1) {
				e1.printStackTrace();
			}
		}

		if (TextUtils.isEmpty(mFFConcatContent)) {
			_prepareAsync();
		} else {
			_prepareAsync();
		}
	}

	@Override
	public void start() throws IllegalStateException {

		Log.d(Constants.LOG_TAG, "KSYMediaPlayer start()");
		stayAwake(true);

		// 获取playmete
		getMediaInfo();

		start = System.currentTimeMillis();
		logRecord.setFirstFrameTime(start - prepare);

		logRecord.setPlayStatus("ok");// 默认值

		if (logClient.mSwitch) {

			try {

				Log.d(Constants.LOG_TAG,
						"NetState = " + logRecord.getNetStateJson());
				logRecord.setDate(logClient.getInstance().logGetData
						.currentTimeGmt());
				logClient.put(logRecord.getFirstFrameTimeJson());
				logRecord.setDate(logClient.getInstance().logGetData
						.currentTimeGmt());
				logClient.put(logRecord.getNetStateJson());
				logRecord.setDate(logClient.getInstance().logGetData
						.currentTimeGmt());
				logClient.put(logRecord.getPlayStatusJson());

			} catch (Ks3ClientException e) {
				e.printStackTrace();
				Log.e(Constants.LOG_TAG, "firstTimer e = " + e);
			}
		}

		_start();
	}

	@Override
	public void pause() throws IllegalStateException {

		stayAwake(false);
		_pause();
	}

	@Override
	public void stop() throws IllegalStateException {

		stayAwake(false);

		_stop();

	}

	public native void _prepareAsync() throws IllegalStateException;

	private native void _start() throws IllegalStateException;

	private native void _pause() throws IllegalStateException;

	private native void _stop() throws IllegalStateException;

	@SuppressLint("Wakelock")
	@Override
	public void setWakeMode(Context context, int mode) {

		boolean washeld = false;
		if (mWakeLock != null) {
			if (mWakeLock.isHeld()) {
				washeld = true;
				mWakeLock.release();
			}
			mWakeLock = null;
		}

		PowerManager pm = (PowerManager) context
				.getSystemService(Context.POWER_SERVICE);
		mWakeLock = pm.newWakeLock(mode | PowerManager.ON_AFTER_RELEASE,
				KSYMediaPlayer.class.getName());
		mWakeLock.setReferenceCounted(false);
		if (washeld) {
			mWakeLock.acquire();
		}
	}

	@Override
	public void setScreenOnWhilePlaying(boolean screenOn) {

		if (mScreenOnWhilePlaying != screenOn) {
			if (screenOn && mSurfaceHolder == null) {
				DebugLog.w(Constants.LOG_TAG,
						"setScreenOnWhilePlaying(true) is ineffective without a SurfaceHolder");
			}
			mScreenOnWhilePlaying = screenOn;
			updateSurfaceScreenOn();
		}
	}

	@SuppressLint("Wakelock")
	private void stayAwake(boolean awake) {

		if (mWakeLock != null) {
			if (awake && !mWakeLock.isHeld()) {
				mWakeLock.acquire();
			} else if (!awake && mWakeLock.isHeld()) {
				mWakeLock.release();
			}
		}
		mStayAwake = awake;
		updateSurfaceScreenOn();
	}

	private void updateSurfaceScreenOn() {

		if (mSurfaceHolder != null) {
			mSurfaceHolder.setKeepScreenOn(mScreenOnWhilePlaying && mStayAwake);
		}
	}

	@Override
	public int getVideoWidth() {

		return mVideoWidth;
	}

	@Override
	public int getVideoHeight() {

		return mVideoHeight;
	}

	@Override
	public int getVideoSarNum() {

		return mVideoSarNum;
	}

	@Override
	public int getVideoSarDen() {

		return mVideoSarDen;
	}

	@Override
	public native boolean isPlaying();

	private native void _seekTo(long msec) throws IllegalStateException;

	@Override
	public native long getCurrentPosition();

	@Override
	public native long getDuration();

	@Override
	public void seekTo(long msec) {

		if (logClient.mSwitch) {
			try {
				// Log.d(Constants.LOG_TAG, "seekBegins =" +
				// logRecord.getSeekBeginJson());
				logRecord.setDate(logClient.getInstance().logGetData
						.currentTimeGmt());
				logClient.put(logRecord.getSeekBeginJson());

			} catch (Ks3ClientException e) {
				e.printStackTrace();
				Log.e(Constants.LOG_TAG, "firstTimer e = " + e);
			}
		}

		_seekTo(msec);
	}

	@Override
	public void release() {
		long current = System.currentTimeMillis();
		stayAwake(false);
		updateSurfaceScreenOn();
		resetListeners();

		if (logClient.mSwitch) {
			logClient.stop();

			try {
				Log.d(Constants.LOG_TAG,
						"BaseEndData = " + logRecord.getBaseDataEndJson());
				logRecord.setDate(logClient.getInstance().logGetData
						.currentTimeGmt());
				logClient.put(logRecord.getBaseDataEndJson());
			} catch (Ks3ClientException e) {
				e.printStackTrace();
			}
		}
		_release();
		Log.e(Constants.LOG_TAG,
				"player release cost :"
						+ String.valueOf(System.currentTimeMillis() - current));
	}

	@Override
	public void reset() {

		stayAwake(false);
		_reset();
		// make sure none of the listeners get called anymore
		mEventHandler.removeCallbacksAndMessages(null);

		mVideoWidth = 0;
		mVideoHeight = 0;
	}

	private native void _release();

	private native void _reset();

	@Override
	public native void setVolume(float leftVolume, float rightVolume);

	@Override
	public MediaInfo getMediaInfo() {

		MediaInfo mediaInfo = new MediaInfo();
		mediaInfo.mMediaPlayerName = "ksyplayer";

		String videoCodecInfo = _getVideoCodecInfo();
		logRecord.setVideoCodec(videoCodecInfo);

		if (!TextUtils.isEmpty(videoCodecInfo)) {
			String nodes[] = videoCodecInfo.split(",");
			if (nodes.length >= 2) {
				mediaInfo.mVideoDecoder = nodes[0];
				mediaInfo.mVideoDecoderImpl = nodes[1];
			} else if (nodes.length >= 1) {
				mediaInfo.mVideoDecoder = nodes[0];
				mediaInfo.mVideoDecoderImpl = "";
			}
		}

		String audioCodecInfo = _getAudioCodecInfo();
		logRecord.setAudioCodec(audioCodecInfo);

		if (!TextUtils.isEmpty(audioCodecInfo)) {
			String nodes[] = audioCodecInfo.split(",");
			if (nodes.length >= 2) {
				mediaInfo.mAudioDecoder = nodes[0];
				mediaInfo.mAudioDecoderImpl = nodes[1];
			} else if (nodes.length >= 1) {
				mediaInfo.mAudioDecoder = nodes[0];
				mediaInfo.mAudioDecoderImpl = "";
			}
		}

		try {
			// 直播点播、协议、格式、编码记录
			mediaInfo.mMeta = KSYMediaMeta.parse(_getMediaMeta());

			String serverIp = mediaInfo.mMeta.mServerIp;
			String httpType = mediaInfo.mMeta.mHttp;

			logRecord.setServerIp(serverIp);

			String playType = "";
			int playMode = mediaInfo.mMeta.mPlayType;

			if (playMode == 0) {
				// 出现错误
				playType = "未知";

			} else if (playMode == 1) {
				// 直播
				playType = "直播";
			} else if (playMode == 2) {
				// 点播
				playType = "点播";
			}

			String protocol = mediaInfo.mMeta.mProtocol;
			String format = mediaInfo.mMeta.mFormat;
			String codec = IjkStreamMeta.getCodecLongNameInline();

			Log.d(Constants.LOG_TAG, "format =" + format + "<>>codec=" + codec
					+ "<<<<>playType=" + playType + ">>><<<protocol="
					+ protocol);

			logRecord.setPlayType(playType);
			logRecord.setProtocol(protocol);
			logRecord.setFormat(format);

		} catch (Throwable e) {
			e.printStackTrace();
		}
		return mediaInfo;
	}

	private native String _getVideoCodecInfo();

	private native String _getAudioCodecInfo();

	public void setAvOption(AvFormatOption option) {

		setAvFormatOption(option.getName(), option.getValue());
	}

	public void setAvFormatOption(String name, String value) {

		_setAvFormatOption(name, value);
	}

	public void setAvCodecOption(String name, String value) {

		_setAvCodecOption(name, value);
	}

	public void setSwScaleOption(String name, String value) {

		_setSwScaleOption(name, value);
	}

	/**
	 * @param chromaFourCC
	 *            AvFourCC.SDL_FCC_RV16 AvFourCC.SDL_FCC_RV32
	 *            AvFourCC.SDL_FCC_YV12
	 */
	public void setOverlayFormat(int chromaFourCC) {

		_setOverlayFormat(chromaFourCC);
	}

	/**
	 * @param frameDrop
	 *            =0 do not drop any frame <0 drop as many frames as possible >0
	 *            display 1 frame per `frameDrop` continuous dropped frames,
	 */
	public void setFrameDrop(int frameDrop) {

		_setFrameDrop(frameDrop);
	}

	public void setMediaCodecEnabled(boolean enabled) {

		_setMediaCodecEnabled(enabled);
	}

	public void setOpenSLESEnabled(boolean enabled) {

		_setOpenSLESEnabled(enabled);
	}

	private native void _setAvFormatOption(String name, String value);

	private native void _setAvCodecOption(String name, String value);

	private native void _setSwScaleOption(String name, String value);

	private native void _setOverlayFormat(int chromaFourCC);

	private native void _setFrameDrop(int frameDrop);

	private native void _setMediaCodecEnabled(boolean enabled);

	private native void _setOpenSLESEnabled(boolean enabled);

	public Bundle getMediaMeta() {

		return _getMediaMeta();
	}

	private native Bundle _getMediaMeta();

	public static String getColorFormatName(int mediaCodecColorFormat) {

		return _getColorFormatName(mediaCodecColorFormat);
	}

	private static native final String _getColorFormatName(
			int mediaCodecColorFormat);

	@Override
	public void setAudioStreamType(int streamtype) {

		// do nothing
	}

	private static native final void native_init();

	private native final void native_setup(Object IjkMediaPlayer_this);

	private native final void native_finalize();

	private native final void native_message_loop(Object IjkMediaPlayer_this);

	// @Override
	// protected void finalize() {
	//
	// native_finalize();
	// }

	private static class EventHandler extends Handler {

		private final WeakReference<KSYMediaPlayer> mWeakPlayer;

		public EventHandler(KSYMediaPlayer mp, Looper looper) {

			super(looper);
			mWeakPlayer = new WeakReference<KSYMediaPlayer>(mp);
		}

		@Override
		public void handleMessage(Message msg) {
			KSYMediaPlayer player = mWeakPlayer.get();
			if (player == null || player.mNativeMediaPlayer == 0) {
				DebugLog.w(Constants.LOG_TAG,
						"IjkMediaPlayer went away with unhandled events");
				return;
			}

			switch (msg.what) {
			case MEDIA_PREPARED:
				player.notifyOnPrepared();
				return;

			case MEDIA_PLAYBACK_COMPLETE:
				player.notifyOnCompletion();
				player.stayAwake(false);
				return;

			case MEDIA_BUFFERING_UPDATE:
				long bufferPosition = msg.arg1;
				if (bufferPosition < 0) {
					bufferPosition = 0;
				}

				long percent = 0;
				long duration = player.getDuration();
				// Log.d(Constants.LOG_TAG, "bufferPosition =" + bufferPosition
				// + "<<>>>>duration=" + duration);

				if (duration > 0) {
					percent = bufferPosition * 100 / duration;
				}
				if (percent >= 100) {
					percent = 100;
				}

				// DebugLog.efmt(Constants.LOG_TAG, "Buffer (%d%%) %d/%d",
				// percent,
				// bufferPosition, duration);
				player.notifyOnBufferingUpdate((int) percent);

				return;

			case MEDIA_SEEK_COMPLETE:
				player.notifyOnSeekComplete();
				return;

			case MEDIA_SET_VIDEO_SIZE:
				player.mVideoWidth = msg.arg1;
				player.mVideoHeight = msg.arg2;
				player.notifyOnVideoSizeChanged(player.mVideoWidth,
						player.mVideoHeight, player.mVideoSarNum,
						player.mVideoSarDen);
				return;

			case MEDIA_ERROR:
				// Log.d(Constants.LOG_TAG, "MEDIA_ERROR =msg.arg1=" + msg.arg1
				// + "<>>>msg.arg2=" + msg.arg2);
				if (msg.arg1 == MEDIA_ERROR_SEEKUNREACHABLE) {
					logRecord.setSeekStatus("fail");

					if (msg.arg2 < 0) {
						logRecord.setSeekMessage(String.valueOf(msg.arg2));

						if (logClient.mSwitch) {
							try {
								// Log.d(Constants.LOG_TAG, "seekend =" +
								// logRecord.getSeekEndJson());
								logRecord
										.setDate(logClient.getInstance().logGetData
												.currentTimeGmt());
								LogClient.getInstance().put(
										logRecord.getSeekEndJson());

							} catch (Ks3ClientException e) {
								e.printStackTrace();
								Log.e(Constants.LOG_TAG, "BaseMediaPlayer e = "
										+ e);
							}
						}
					}

				} else {

					if (msg.arg2 < 0) {
						// 都返回错误码就行
						logRecord.setPlayStatus(String.valueOf(msg.arg2));

						if (logClient.mSwitch) {
							try {
								logRecord
										.setDate(logClient.getInstance().logGetData
												.currentTimeGmt());
								logClient.put(logRecord.getPlayStatusJson());
							} catch (Ks3ClientException e) {
								e.printStackTrace();
							}
						}
					}
				}

				Log.e(Constants.LOG_TAG, "Error (" + msg.arg1 + "," + msg.arg2
						+ ")");
				if (!player.notifyOnError(msg.arg1, msg.arg2)) {
					player.notifyOnCompletion();
				}
				player.stayAwake(false);
				return;

			case MEDIA_INFO:
				if (msg.arg1 != MEDIA_INFO_VIDEO_TRACK_LAGGING) {
					Log.e(Constants.LOG_TAG, "===============Info (" + msg.arg1
							+ "," + msg.arg2 + ")");
				}
				player.notifyOnInfo(msg.arg1, msg.arg2);
				// No real default action so far.
				return;
			case MEDIA_TIMED_TEXT:
				// do nothing
				break;

			case MEDIA_NOP: // interface test message - ignore
				break;

			case MEDIA_SET_VIDEO_SAR:
				player.mVideoSarNum = msg.arg1;
				player.mVideoSarDen = msg.arg2;
				player.notifyOnVideoSizeChanged(player.mVideoWidth,
						player.mVideoHeight, player.mVideoSarNum,
						player.mVideoSarDen);
				break;

			case MEDIA_GET_DRM_KEY:
				Log.e(Constants.LOG_TAG, "MEDIA_GET_DRM_KEY");
				String version = (String) msg.obj;
				if (!TextUtils.isEmpty(version)) {
					player.notifyOnDRMRequired(msg.arg1, msg.arg2, version);
				} else {
					DebugLog.e(Constants.LOG_TAG, "version is null" + version);
				}
				break;
			case MEDIA_PLAYBACK_STATE_CHANGED:
				Log.d(Constants.LOG_TAG, "current 102" + ",arg1 = " + msg.arg1
						+ "msg2 = " + msg.arg2);
				if (msg.arg1 == MP_STATE_END || msg.arg2 == MP_STATE_END) {
					Log.d(Constants.LOG_TAG, "current release ok");
				}
				break;

			case MEDIA_GET_SPEED:
				// Log.e(Constants.LOG_TAG, "MEDIA_GET_SPEED " +
				// msg.what+",arg1 = "+msg.arg1+">>>>>msg2 = "+msg.arg2);
				player.notifyOnNetSpeedUpdate(msg.arg1, msg.arg2);
				break;

			case MEDIA_DEMUX_CACHE_DATA:
				player.notifyOnDebugInfo(MEDIA_DEMUX_CACHE_DATA, msg.arg1,
						msg.arg2);
				// Log.e(Constants.LOG_TAG, "MEDIA_DEMUX_CACHE_DATA" +
				// msg.what+",video demux cache frame num = "+msg.arg1+">>audio demux cache frame num = "+msg.arg2);
				break;
			case MEDIA_DECODE_CACHE_DATA:
				player.notifyOnDebugInfo(MEDIA_DECODE_CACHE_DATA, msg.arg1,
						msg.arg2);
				// Log.e(Constants.LOG_TAG, "MEDIA_DECODE_CACHE_DATA " +
				// msg.what+",video decode cache frame num = "+msg.arg1+">>audio decode cache frame num = "+msg.arg2);
				break;
			case MEDIA_PLAY_TIME:
				player.notifyOnDebugInfo(MEDIA_PLAY_TIME, msg.arg1, msg.arg2);
				// Log.e(Constants.LOG_TAG, "MEDIA_PLAY_TIME" +
				// msg.what+",abs(vpts-apts) = "+msg.arg1 + ">>>>msg.arg2=" +
				// msg.arg2);
				break;
			default:
				// DebugLog.e(Constants.LOG_TAG, "Unknown message type " +
				// msg.what);
				return;
			}
		}
	}

	@CalledByNative
	private static void postEventFromNative(Object weakThiz, int what,
			int arg1, int arg2, Object obj) {

		if (weakThiz == null)
			return;

		@SuppressWarnings("rawtypes")
		KSYMediaPlayer mp = (KSYMediaPlayer) ((WeakReference) weakThiz).get();
		if (mp == null) {
			return;
		}

		if (what == MEDIA_INFO && arg1 == MEDIA_INFO_STARTED_AS_NEXT) {
			// this acquires the wakelock if needed, and sets the client side
			// state
			mp.start();
		}
		if (mp.mEventHandler != null) {
			Message m = mp.mEventHandler.obtainMessage(what, arg1, arg2, obj);
			mp.mEventHandler.sendMessage(m);
		}
	}

	private OnControlMessageListener mOnControlMessageListener;

	public void setOnControlMessageListener(OnControlMessageListener listener) {

		mOnControlMessageListener = listener;
	}

	public static interface OnControlMessageListener {

		public int onControlResolveSegmentCount();

		public String onControlResolveSegmentUrl(int segment);

		public String onControlResolveSegmentOfflineMrl(int segment);

		public int onControlResolveSegmentDuration(int segment);
	}

	@CalledByNative
	private static int onControlResolveSegmentCount(Object weakThiz) {

		DebugLog.ifmt(Constants.LOG_TAG, "onControlResolveSegmentCount");
		if (weakThiz == null || !(weakThiz instanceof WeakReference<?>))
			return -1;

		@SuppressWarnings("unchecked")
		WeakReference<KSYMediaPlayer> weakPlayer = (WeakReference<KSYMediaPlayer>) weakThiz;
		KSYMediaPlayer player = weakPlayer.get();
		if (player == null)
			return -1;

		OnControlMessageListener listener = player.mOnControlMessageListener;
		if (listener == null)
			return -1;

		return listener.onControlResolveSegmentCount();
	}

	@CalledByNative
	private static String onControlResolveSegmentUrl(Object weakThiz,
			int segment) {

		DebugLog.ifmt(Constants.LOG_TAG, "onControlResolveSegmentUrl %d",
				segment);
		if (weakThiz == null || !(weakThiz instanceof WeakReference<?>))
			return null;

		@SuppressWarnings("unchecked")
		WeakReference<KSYMediaPlayer> weakPlayer = (WeakReference<KSYMediaPlayer>) weakThiz;
		KSYMediaPlayer player = weakPlayer.get();
		if (player == null)
			return null;

		OnControlMessageListener listener = player.mOnControlMessageListener;
		if (listener == null)
			return null;

		return listener.onControlResolveSegmentUrl(segment);
	}

	@CalledByNative
	private static String onControlResolveSegmentOfflineMrl(Object weakThiz,
			int segment) {

		DebugLog.ifmt(Constants.LOG_TAG,
				"onControlResolveSegmentOfflineMrl %d", segment);
		if (weakThiz == null || !(weakThiz instanceof WeakReference<?>))
			return null;

		@SuppressWarnings("unchecked")
		WeakReference<KSYMediaPlayer> weakPlayer = (WeakReference<KSYMediaPlayer>) weakThiz;
		KSYMediaPlayer player = weakPlayer.get();
		if (player == null)
			return null;

		OnControlMessageListener listener = player.mOnControlMessageListener;
		if (listener == null)
			return null;

		return listener.onControlResolveSegmentOfflineMrl(segment);
	}

	@CalledByNative
	private static int onControlResolveSegmentDuration(Object weakThiz,
			int segment) {

		DebugLog.ifmt(Constants.LOG_TAG, "onControlResolveSegmentDuration %d",
				segment);
		if (weakThiz == null || !(weakThiz instanceof WeakReference<?>))
			return -1;

		@SuppressWarnings("unchecked")
		WeakReference<KSYMediaPlayer> weakPlayer = (WeakReference<KSYMediaPlayer>) weakThiz;
		KSYMediaPlayer player = weakPlayer.get();
		if (player == null)
			return -1;

		OnControlMessageListener listener = player.mOnControlMessageListener;
		if (listener == null)
			return -1;

		return listener.onControlResolveSegmentDuration(segment);
	}

	public static interface OnMediaCodecSelectListener {

		public String onMediaCodecSelect(IMediaPlayer mp, String mimeType,
										 int profile, int level);
	}

	private OnMediaCodecSelectListener mOnMediaCodecSelectListener;

	public void setOnMediaCodecSelectListener(
			OnMediaCodecSelectListener listener) {

		mOnMediaCodecSelectListener = listener;
	}

	@Override
	public void resetListeners() {

		super.resetListeners();
		mOnMediaCodecSelectListener = null;
	}

	@CalledByNative
	private static String onSelectCodec(Object weakThiz, String mimeType,
			int profile, int level) {

		if (weakThiz == null || !(weakThiz instanceof WeakReference<?>))
			return null;

		@SuppressWarnings("unchecked")
		WeakReference<KSYMediaPlayer> weakPlayer = (WeakReference<KSYMediaPlayer>) weakThiz;
		KSYMediaPlayer player = weakPlayer.get();
		if (player == null)
			return null;

		OnMediaCodecSelectListener listener = player.mOnMediaCodecSelectListener;
		if (listener == null)
			listener = DefaultMediaCodecSelector.sInstance;

		return listener.onMediaCodecSelect(player, mimeType, profile, level);
	}

	public static class DefaultMediaCodecSelector implements
			OnMediaCodecSelectListener {

		public static DefaultMediaCodecSelector sInstance = new DefaultMediaCodecSelector();

		@Override
		@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
		public String onMediaCodecSelect(IMediaPlayer mp, String mimeType,
				int profile, int level) {

			if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN)
				return null;

			if (TextUtils.isEmpty(mimeType))
				return null;

			Log.i(Constants.LOG_TAG, String.format(Locale.US,
					"onSelectCodec: mime=%s, profile=%d, level=%d", mimeType,
					profile, level));
			TreeMap<Integer, KSYMediaCodecInfo> candidateCodecList = new TreeMap<Integer, KSYMediaCodecInfo>();
			int numCodecs = MediaCodecList.getCodecCount();
			for (int i = 0; i < numCodecs; i++) {
				MediaCodecInfo codecInfo = MediaCodecList.getCodecInfoAt(i);
				Log.d(Constants.LOG_TAG,
						String.format(Locale.US, "  found codec: %s",
								codecInfo.getName()));
				if (codecInfo.isEncoder())
					continue;

				String[] types = codecInfo.getSupportedTypes();
				if (types == null)
					continue;

				for (String type : types) {
					if (TextUtils.isEmpty(type))
						continue;

					Log.d(Constants.LOG_TAG,
							String.format(Locale.US, "    mime: %s", type));
					if (!type.equalsIgnoreCase(mimeType))
						continue;

					KSYMediaCodecInfo candidate = KSYMediaCodecInfo
							.setupCandidate(codecInfo, mimeType);
					if (candidate == null)
						continue;

					candidateCodecList.put(candidate.mRank, candidate);
					Log.i(Constants.LOG_TAG, String.format(Locale.US,
							"candidate codec: %s rank=%d", codecInfo.getName(),
							candidate.mRank));
					candidate.dumpProfileLevels(mimeType);
				}
			}

			Entry<Integer, KSYMediaCodecInfo> bestEntry = candidateCodecList
					.lastEntry();
			if (bestEntry == null)
				return null;

			KSYMediaCodecInfo bestCodec = bestEntry.getValue();
			if (bestCodec == null || bestCodec.mCodecInfo == null)
				return null;

			if (bestCodec.mRank < KSYMediaCodecInfo.RANK_LAST_CHANCE) {
				Log.w(Constants.LOG_TAG,
						String.format(Locale.US, "unaccetable codec: %s",
								bestCodec.mCodecInfo.getName()));
				return null;
			}

			Log.i(Constants.LOG_TAG, String.format(Locale.US,
					"selected codec: %s rank=%d",
					bestCodec.mCodecInfo.getName(), bestCodec.mRank));
			return bestCodec.mCodecInfo.getName();
		}
	}

	// P1 Added Interface
	@Override
	public void setAudioAmplify(float ratio) {

		if (ratio <= 0) {
			ratio = MEDIA_AUDIO_AMPLIFY_DEFAULT;
			Log.w(Constants.LOG_TAG, "unsupported audio amplify ratio :"
					+ ratio + ",replace the default size :"
					+ MEDIA_AUDIO_AMPLIFY_DEFAULT);
		}
		_setAudioAmplify(ratio);
	}

	@Override
	public void setVideoRate(float rate) {

		if (rate <= 0) {
			rate = MEDIA_VIDEO_RATE_DEFAULT;
			Log.w(Constants.LOG_TAG, "unsupported video rate :" + rate
					+ ",replace the default size :" + MEDIA_VIDEO_RATE_DEFAULT);
		}
		_setVideoRate(rate);
	}

	@Override
	public void getCurrentFrame(Bitmap bitmap) {

		if (bitmap != null) {
			_getCurrentFrame(bitmap);
		} else {
			Log.w(Constants.LOG_TAG,
					"get current failed, bitmap can not be null ");
		}
	}

	@Override
	public void setBufferSize(int size) {

		if (size <= 0) {
			size = MEDIA_BUFFERSIZE_DEFAULT;
			Log.w(Constants.LOG_TAG, "unsupported buffer size :" + size
					+ ",replace the default size :" + MEDIA_BUFFERSIZE_DEFAULT);
		}
		_setBufferSize(size);

		// 缓存大小
		logRecord.setCacheBufferSize(size);
	};

	@Override
	public void setAnalyseDuration(int duration) {

		if (duration <= 0) {
			Log.w(Constants.LOG_TAG, "unsupported analyse duration :"
					+ duration + ",replace the default size :"
					+ MEDIA_ANALYSE_DURATION_DEFAULT);
			duration = MEDIA_ANALYSE_DURATION_DEFAULT;
		}
		_setAnalyseDuration(duration);
	};

	@Override
	public void setDRMKey(String version, String key) {

		if (checkDRMKey(version, key)) {
			Log.i(Constants.LOG_TAG, " DRM  version :" + version + ",Cek:"
					+ key);
			_setDRMKey(version, key);
		} else {
			Log.w(Constants.LOG_TAG, "DRM failed with error");
		}
	}

	@Override
	public void setTimeout(int timeout) {

		if (timeout <= 0) {
			Log.w(Constants.LOG_TAG, "unsupported time out  :" + timeout
					+ ",replace the default time out :"
					+ IMediaPlayer.MEDIA_TIME_OUT_DEFAULT);
			timeout = IMediaPlayer.MEDIA_TIME_OUT_DEFAULT;
		}
		_setTimeout(timeout);
	}

	// need to implements
	private boolean checkDRMKey(String version, String key) {

		if (TextUtils.isEmpty(version) || TextUtils.isEmpty(key)) {
			Log.w(Constants.LOG_TAG,
					"DRM version & key can not be null or empty");
			return false;
		} else {
			return true;
		}
	}

	/**
	 * 设置缓存目录
	 * 
	 * @param cachedPath
	 *            必须为文件夹路径
	 */
	@Override
	public boolean setCachedDir(String cachedPath) {
		Log.d(Constants.LOG_TAG, "cachedPath = " + cachedPath);
		if (null == cachedPath || "".equals(cachedPath)) {
			Log.e(Constants.LOG_TAG,
					"the cached path is null , so the streaming cached function failure");
			return false;
		}
		File file = new File(cachedPath);
		if (file.isFile()) {
			Log.e(Constants.LOG_TAG,
					"the cached path must be a forder , so the streaming cached function failure");
			return false;
		}

		if (!file.exists()) {
			if (!file.mkdirs()) {
				Log.e(Constants.LOG_TAG,
						"the cached forder create fail , so the streaming cached function failure");
				return false;
			}
		} else {
			Log.e(Constants.LOG_TAG, "1064  !file.exists() .......");
		}

		Log.i(Constants.LOG_TAG,
				"the cached forder create success , streaming will cached with path :"
						+ cachedPath);
		_setCachedDir(cachedPath);
		return true;
	}

	/**
	 * 清空缓存文件夹 以及文件夹所有层级目录的文件，缓存文件夹会会重新创建
	 * 
	 * @param cachedPath
	 *            需要清空的缓存目录
	 * 
	 * @exception 文件夹不存在
	 *                ，路劲指向文件
	 */

	@Override
	public boolean clearCachedFiles(String cachedPath) {

		if (null == cachedPath || "".equals(cachedPath)) {
			Log.e(Constants.LOG_TAG, "the cached path is null , clear nothing");
			return false;
		}
		File file = new File(cachedPath);
		if (file.isFile()) {
			Log.e(Constants.LOG_TAG,
					"the cached path must be a forder , clear nothing");
			return false;
		}

		if (!file.exists()) {
			Log.e(Constants.LOG_TAG, "the cached path not exist, clear nothing");
			return false;
		}

		boolean success = IOUtils.deleteDir(new File(cachedPath));
		if (!success) {
			Log.e(Constants.LOG_TAG, "clear the cached path occur an problem !");
			return false;
		}

		if (!file.mkdirs()) {
			Log.e(Constants.LOG_TAG, "the cached forder recreate failed !");
			return true;
		}

		Log.e(Constants.LOG_TAG,
				"the cached forder clear success , recreate cached forder success");
		return true;
	}

	@Override
	public void setLowDelayEnabled(int mode,
			int start_drop_frame_threshold, int stop_drop_frame_threshold) {
		_setLowDelayEnabled(mode, start_drop_frame_threshold,
				stop_drop_frame_threshold);
	}

	private native void _setAudioAmplify(float ratio);

	private native void _setVideoRate(float rate);

	private native void _setAnalyseDuration(int duration);

	private native void _setBufferSize(int Size);

	private native void _getCurrentFrame(Bitmap bitmap);

	private native void _setDRMKey(String version, String key);

	private native void _setTimeout(int timeout);

	private native void _setCachedDir(String cachedPath);

	private native void _setLowDelayEnabled(int mode,
			int start_drop_frame_threshold, int stop_drop_frame_threshold);

	private native void _setRetryCount(int retry_count);

	private native void _setRetryAlways();

	@Override
	public void setRetryCount(int retry_count) {
		_setRetryCount(retry_count);
	}

	@Override
	public void setRetryAlways() {
		_setRetryAlways();
	}

	@Override
	public void setLogEnabled(boolean enable) {

	}

	@Override
	public boolean isPlayable() {
		return true;
	}

	@Override
	public void setKeepInBackground(boolean keepInBackground) {

	}

}
