package com.ksy.media.demo;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.Toast;
import android.widget.VideoView;

import com.ksy.media.player.util.Constants;
import com.ksy.media.widget.util.IPowerStateListener;
import com.ksy.media.widget.ui.MediaPlayerView;

public class VideoPlayerActivity extends Activity implements
		MediaPlayerView.PlayerViewCallback {

	MediaPlayerView playerView;
	private boolean delay;
	private IPowerStateListener powerStateListener;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		Intent intent = getIntent();
		delay = intent.getBooleanExtra("is_delay", false);
		VideoView view = new VideoView(this);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		// 设置全屏
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
				WindowManager.LayoutParams.FLAG_FULLSCREEN);

		setContentView(R.layout.activity_player);
		registerPowerReceiver();
		playerView = (MediaPlayerView) findViewById(R.id.player_view);
		setPowerStateListener(playerView);
		final View dialogView = LayoutInflater.from(this).inflate(
				R.layout.dialog_input, null);
		final EditText editInput = (EditText) dialogView
				.findViewById(R.id.input);
		// startPlayer("");
		new AlertDialog.Builder(this).setTitle("User Input")
				.setView(dialogView)
				.setPositiveButton("Confirm", new OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						String inputString = editInput.getText().toString();
						if (!TextUtils.isEmpty(inputString)) {
							startPlayer(inputString);
						} else {
							Toast.makeText(VideoPlayerActivity.this,
									"Paht or URL can not be null",
									Toast.LENGTH_LONG).show();
						}

					}
				}).setNegativeButton("Cancel", new OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();
					}
				}).show();

	}

	public void registerPowerReceiver() {
		IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_OFF);
		filter.addAction(Intent.ACTION_SCREEN_ON);
	    filter.addAction(Intent.ACTION_USER_PRESENT);
		registerReceiver(mBatInfoReceiver, filter);
	}

	public void unregisterPowerReceiver() {
		if (mBatInfoReceiver != null) {
			try {
				unregisterReceiver(mBatInfoReceiver);
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
			} else if(Intent.ACTION_USER_PRESENT.equals(action)){
				if (isAppOnForeground()) {
					powerStateListener.onPowerState(Constants.USER_PRESENT);
				}
			}
		}
	};
	private boolean isAppShown;

	public boolean isAppOnForeground() {
		ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
		ComponentName cn = am.getRunningTasks(1).get(0).topActivity;
		String currentPackageName = cn.getPackageName();
		if (!TextUtils.isEmpty(currentPackageName)
				&& currentPackageName.equals(getPackageName())) {
			return true;
		}
		return false;
	}

	public void setPowerStateListener(IPowerStateListener powerStateListener) {
		this.powerStateListener = powerStateListener;
	}

	@Override
	protected void onResume() {
		powerStateListener.onPowerState(Constants.APP_SHOWN);
		super.onResume();
		playerView.onResume();
	}

	@Override
	protected void onPause() {
		powerStateListener.onPowerState(Constants.APP_HIDEN);
		super.onPause();
		playerView.onPause();
	}

	@Override
	protected void onDestroy() {

		Log.d(Constants.LOG_TAG, "VideoPlayerActivity ....onDestroy()......");
		super.onDestroy();
		unregisterPowerReceiver();
		playerView.onDestroy();
	}

	// master
	private void startPlayer(String url) {

		Log.d(Constants.LOG_TAG, "input url = " + url);

		playerView.setPlayerViewCallback(this);

		// String path = "rtmp://192.168.135.185:1935/myLive/guoyankai";
		// String path = "http://live.3gv.ifeng.com/zixun.m3u8"; // vod

		// String path
		// ="http://maichang.kssws.ks-cdn.com/upload20150716161913.mp4";
		// String path = "rtmp://uplive.ksyun.com/live/eflake_test";
		// String path = "rtmp://uplive.ksyun.com/live/eflake_delay2";
		// String path =
		// "rtmp://rtmp3.plu.cn/live/808fd766a92347868dd03b6da0a7f9ce?signature=63%2fPmV0cjLYOZbsyLPPwGHp%2fSTQ%3d&accesskey=yX5ga7SZ%2fKoMV97kiihh&expire=1445508643&nonce=b72b4d0c006d439bbbd7f538e69aa51d&public=0";
		// String path =
		// "rtmp://rtmp3.plu.cn/live/c74ec4f54bdc4cd19fb367aca416b6b3?signature=Iv28YKWJjNre6pA74E9OIfzd1gA%3d&accesskey=yX5ga7SZ%2fKoMV97kiihh&expire=1445576027&nonce=93b73226c55c4764ae73875ba8dac687&public=0";

		// String path = "http://picstat.waqu.com/is/v/k/bbkqcmt0a4rjg6nu/sd";
		// String path =
		// "rtsp://218.204.223.237:554/live/1/0547424F573B085C/gsfp90ef4k0a6iap.sdp";
		// String path =
		// "http://www.huajiao.com/live/share.php?liveid=581383&tab=2";
		// File file = new File(Environment.getExternalStorageDirectory(),
		// "Love.mp4");
		// String path = file.getAbsolutePath();

		// Love.mp4
		// avitest.avi
		// flvtest.flv
		// mkvtest.mkv
		// rmvbtest.rmvb
		// tstest.ts
		// wmvtest.wmv
		// playerView.play(path);
		playerView.play(url, delay);
		// playerView.play("http://maichang.kssws.ks-cdn.com/upload20150716161913.mp4");
	}

	@Override
	public void hideViews() {

	}

	@Override
	public void restoreViews() {

	}

	@Override
	public void onPrepared() {

	}

	@Override
	public void onQualityChanged() {

	}

	@Override
	public void onFinish(int playMode) {

		Log.i(Constants.LOG_TAG, "activity on finish ===========");
		// this.onBackPressed();
		this.finish();
	}

	@Override
	public void onError(int errorCode, String errorMsg) {

	}

	/**
	 * 返回键退出
	 */
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			finish();
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}

}
