package com.ksy.media.demo.shortvideo;

import android.animation.ObjectAnimator;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.AnimationUtils;
import android.view.animation.LayoutAnimationController;
import android.widget.AbsListView;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.ksy.media.demo.R;
import com.ksy.media.player.util.Constants;
import com.ksy.media.widget.ui.shortvideo.MediaPlayerViewShortVideo;
import com.ksy.media.widget.ui.shortvideo.ShortMovieItem;
import com.ksy.media.widget.ui.shortvideo.ShortVideoListAdapter;
import com.ksy.media.widget.util.IPowerStateListener;

import java.util.ArrayList;

public class ShortVideoActivity extends AppCompatActivity implements
        MediaPlayerViewShortVideo.PlayerViewCallback, AbsListView.OnScrollListener {
    private static final int STATE_UP = 1;
    private static final int STATE_DOWN = 0;
    MediaPlayerViewShortVideo playerViewShortMovie;
    private boolean delay;
    private IPowerStateListener powerStateListener;
    private View headView;
    private ListView listView;
    private ShortVideoListAdapter adapter;
    private ArrayList<ShortMovieItem> items;
    private boolean scrollFlag;
    private int lastVisibleItemPosition;
    private int currentState;
    private int lastState;
    private View commentLayout;
    private int mHeight;
    private PopupWindow popupWindow;
    private RelativeLayout container;
    private int mWidth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_short_movie);
        setupViews();
        setupScreenSize();
    }

    private void setupScreenSize() {
        WindowManager wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();

        DisplayMetrics dm = new DisplayMetrics();
        display.getMetrics(dm);
        mHeight = dm.heightPixels;
        mWidth = dm.widthPixels;
    }

    private void setupViews() {
        container = (RelativeLayout) findViewById(R.id.container);
        container.setLayoutAnimation(new LayoutAnimationController(AnimationUtils.loadAnimation(ShortVideoActivity.this, R.anim.pop_show)));
        container.startLayoutAnimation();
        headView = LayoutInflater.from(ShortVideoActivity.this).inflate(R.layout.short_movie_head_view, null);
        listView = (ListView) findViewById(R.id.short_video_list);
//        commentLayout = (LinearLayout) findViewById(R.id.comment_layout);
        commentLayout = LayoutInflater.from(ShortVideoActivity.this).inflate(
                R.layout.short_video_pop_layout, null);
        items = new ArrayList<>();
        for (int i = 0; i < 15; i++) {
            ShortMovieItem item = new ShortMovieItem();
            item.setComment(getString(R.string.short_video_item_comment));
            item.setFav(getString(R.string.short_video_item_fav));
            item.setInfo(getString(R.string.short_video_item_info));
            items.add(item);
        }
        adapter = new ShortVideoListAdapter(ShortVideoActivity.this, items);
        listView.addHeaderView(headView);
        listView.setAdapter(adapter);
        listView.setOnScrollListener(this);
        playerViewShortMovie = (MediaPlayerViewShortVideo) findViewById(R.id.player_view_short_movie);
        registerPowerReceiver();
        setPowerStateListener(playerViewShortMovie);
        setupDialog();
        setupToolbar();
    }

    private void setupToolbar() {
        Toolbar mToolbar = (Toolbar) findViewById(R.id.toolbar);
        if (mToolbar != null) {
//            mToolbar.setNavigationIcon(getResources().getDrawable(R.mipmap.ksy_logo));
            setSupportActionBar(mToolbar);
            mToolbar.setNavigationOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // Handle your drawable state here
                }
            });
        }
    }

    private void setupDialog() {
        final View dialogView = LayoutInflater.from(this).inflate(
                R.layout.dialog_input, null);
        final EditText editInput = (EditText) dialogView
                .findViewById(R.id.input);
        new AlertDialog.Builder(this).setTitle("User Input")
                .setView(dialogView)
                .setPositiveButton("Confirm", new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String inputString = editInput.getText().toString();
                        if (!TextUtils.isEmpty(inputString)) {
                            startPlayer(inputString);
                        } else {
                            Toast.makeText(ShortVideoActivity.this,
                                    "Paht or URL can not be null",
                                    Toast.LENGTH_LONG).show();
                        }
                    }
                }).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        }).show();
    }

    @Override
    protected void onResume() {
        powerStateListener.onPowerState(Constants.APP_SHOWN);
        super.onResume();
        playerViewShortMovie.onResume();
    }

    @Override
    protected void onPause() {
        powerStateListener.onPowerState(Constants.APP_HIDEN);
        super.onPause();
        playerViewShortMovie.onPause();
    }

    @Override
    protected void onDestroy() {

        Log.d(Constants.LOG_TAG, "VideoPlayerActivity ....onDestroy()......");
        super.onDestroy();
        unregisterPowerReceiver();
        playerViewShortMovie.onDestroy();
    }

    // master
    private void startPlayer(String url) {
        Log.d(Constants.LOG_TAG, "input url = " + url);
        playerViewShortMovie.setPlayerViewCallback(this);
        playerViewShortMovie.play(url, delay);
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
        this.finish();
    }

    @Override
    public void onError(int errorCode, String errorMsg) {

    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            finish();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    /*
    *
    * For power state
    * */
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
            } else if (Intent.ACTION_USER_PRESENT.equals(action)) {
                if (isAppOnForeground()) {
                    powerStateListener.onPowerState(Constants.USER_PRESENT);
                }
            }
        }
    };

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
    public void onScrollStateChanged(AbsListView view, int scrollState) {
        if (scrollState == AbsListView.OnScrollListener.SCROLL_STATE_TOUCH_SCROLL || scrollState == AbsListView.OnScrollListener.SCROLL_STATE_FLING) {
            scrollFlag = true;
        } else {
            scrollFlag = false;
        }
    }

    @Override
    public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
        Log.d("eflake", "firstVisibleItem：：" + firstVisibleItem + ":visibleItemCount:" + visibleItemCount + ":totalItemCount:" + totalItemCount);
        if (scrollFlag) {
            if (firstVisibleItem > lastVisibleItemPosition) {
                //Up
                currentState = STATE_UP;
            }
            if (firstVisibleItem < lastVisibleItemPosition) {
                //Down
                currentState = STATE_DOWN;
            }
            if (lastState > currentState) {
                hideCommentLayout();
            } else if (lastState < currentState) {
                showCommentLayout();
            } else {

            }
            if (firstVisibleItem == lastVisibleItemPosition) {
                return;
            }
            lastVisibleItemPosition = firstVisibleItem;
            lastState = currentState;
        }
    }

    private void hideCommentLayout() {
        Log.d("ok", "hide");
        container.removeView(commentLayout);
    }

    private void showCommentLayout() {
        Log.d("ok", "show");
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, getResources().getDimensionPixelSize(R.dimen.short_movie_comment_distance));
        params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        container.addView(commentLayout, params);
    }


}
