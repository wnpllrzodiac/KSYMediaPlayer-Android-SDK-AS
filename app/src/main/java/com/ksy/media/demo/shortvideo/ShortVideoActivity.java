package com.ksy.media.demo.shortvideo;

import android.animation.LayoutTransition;
import android.animation.ObjectAnimator;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.AbsListView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.ksy.media.demo.R;
import com.ksy.media.player.util.Constants;
import com.ksy.media.widget.ui.shortvideo.ShortMovieItem;
import com.ksy.media.widget.ui.shortvideo.ShortVideoListAdapter;
import com.ksy.media.widget.ui.shortvideo.ShortVideoMediaPlayerView;
import com.ksy.media.widget.util.VideoViewConfig;

import java.util.ArrayList;

public class ShortVideoActivity extends AppCompatActivity implements
        ShortVideoMediaPlayerView.PlayerViewCallback, AbsListView.OnScrollListener {

    private View headView;
    private View commentLayout;
    private RelativeLayout container;
    private Toolbar mToolbar;
    private ListView listView;
    private ArrayList<ShortMovieItem> items;
    private ShortVideoMediaPlayerView playerViewShortMovie;
    private int lastVisibleItemPosition;
    private boolean scrollFlag;
    private int currentState;
    private int lastState;
    private int mHeight;
    private int mWidth;
    private static final int STATE_UP = 1;
    private static final int STATE_DOWN = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_short_movie);
        setupScreenSize();
        setupViews();
    }

    private void setupViews() {
        container = (RelativeLayout) findViewById(R.id.container);
        headView = LayoutInflater.from(ShortVideoActivity.this).inflate(R.layout.short_movie_head_view, null);
        listView = (ListView) findViewById(R.id.short_video_list);
        commentLayout = LayoutInflater.from(ShortVideoActivity.this).inflate(
                R.layout.short_video_pop_layout, null);
        setupCommentList();
        setupDialog();
        setupAnimation();
        setupToolbar();
    }

    private void setupCommentList() {
        makeContents();
        ShortVideoListAdapter adapter = new ShortVideoListAdapter(ShortVideoActivity.this, items);
        listView.addHeaderView(headView);
        listView.setAdapter(adapter);
        listView.setOnScrollListener(this);
        playerViewShortMovie = (ShortVideoMediaPlayerView) headView.findViewById(R.id.player_view_short_movie);
        playerViewShortMovie.setVideoViewConfig(false, VideoViewConfig.INTERRUPT_MODE_PAUSE_RESUME);

    }

    private void setupAnimation() {
        LayoutTransition transition = new LayoutTransition();
        container.setLayoutTransition(transition);
        ObjectAnimator enter_animator = ObjectAnimator.ofInt(commentLayout, "y", mHeight, mHeight - commentLayout.getHeight());
        ObjectAnimator exit_animator = ObjectAnimator.ofInt(commentLayout, "y", mHeight - commentLayout.getHeight(), mHeight);
        transition.setAnimator(LayoutTransition.APPEARING, enter_animator);
        transition.setAnimator(LayoutTransition.DISAPPEARING, exit_animator);
    }

    private void setupScreenSize() {
        WindowManager wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();

        DisplayMetrics dm = new DisplayMetrics();
        display.getMetrics(dm);
        mHeight = dm.heightPixels;
        mWidth = dm.widthPixels;
    }

    private void makeContents() {
        items = new ArrayList<>();
        for (int i = 0; i < 15; i++) {
            ShortMovieItem item = new ShortMovieItem();
            item.setComment(getString(R.string.short_video_item_comment));
            item.setFav(getString(R.string.short_video_item_fav));
            item.setInfo(getString(R.string.short_video_item_info));
            items.add(item);
        }
    }

    private void setupToolbar() {
        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        if (mToolbar != null) {
            setSupportActionBar(mToolbar);
            mToolbar.setTitle(getResources().getString(R.string.short_video_title));
            mToolbar.setTitleTextColor(Color.BLACK);
            mToolbar.setNavigationOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
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
        super.onResume();
        playerViewShortMovie.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        playerViewShortMovie.onPause();
    }

    @Override
    protected void onDestroy() {

        Log.d(Constants.LOG_TAG, "VideoPlayerActivity ....onDestroy()......");
        super.onDestroy();
        playerViewShortMovie.onDestroy();
    }

    // master
    private void startPlayer(String url) {
        Log.d(Constants.LOG_TAG, "input url = " + url);
        playerViewShortMovie.setPlayerViewCallback(this);
        playerViewShortMovie.play(url, false);
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
        this.finish();
    }

    @Override
    public void onError(int errorCode, String errorMsg) {

    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        playerViewShortMovie.dispatchKeyEvent(event);
        return true;
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
        container.removeView(commentLayout);
    }

    private void showCommentLayout() {
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, getResources().getDimensionPixelSize(R.dimen.short_movie_comment_distance));
        params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        container.addView(commentLayout, params);
    }


}
