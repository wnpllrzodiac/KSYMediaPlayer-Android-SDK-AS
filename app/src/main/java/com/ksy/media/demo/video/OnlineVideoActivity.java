package com.ksy.media.demo.video;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.ksy.media.demo.R;
import com.ksy.media.widget.ui.video.VideoMediaPlayerPagerAdapter;
import com.ksy.media.widget.ui.video.VideoMediaPlayerView;
import com.ksy.media.widget.ui.video.fragment.CommentListFragment;
import com.ksy.media.widget.util.Constants;
import com.ksy.media.widget.util.VideoViewConfig;

public class OnlineVideoActivity extends AppCompatActivity implements
        VideoMediaPlayerView.PlayerViewCallback, CommentListFragment.OnFragmentInteractionListener {
    VideoMediaPlayerView playerView;
    private ViewPager pager;
    private VideoMediaPlayerPagerAdapter pagerAdapter;
    private TabLayout tabLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_online_video);
        setupViews();
    }

    private void setupViews() {
        playerView = (VideoMediaPlayerView) findViewById(R.id.video_player_view);
        playerView.setVideoViewConfig(false, VideoViewConfig.INTERRUPT_MODE_RELEASE_CREATE);
        setupDialog();
        setUpPagerAndTabs();
    }

    private void setUpPagerAndTabs() {
        pager = (ViewPager) findViewById(R.id.pager);
        pagerAdapter = new VideoMediaPlayerPagerAdapter(OnlineVideoActivity.this, getSupportFragmentManager());
        pager.setAdapter(pagerAdapter);
        tabLayout = (TabLayout) findViewById(R.id.tab_layout);
        tabLayout.setTabsFromPagerAdapter(pagerAdapter);
        tabLayout.setupWithViewPager(pager);
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
                            Toast.makeText(OnlineVideoActivity.this,
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
        playerView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        playerView.onPause();
    }

    @Override
    protected void onDestroy() {

        Log.d(Constants.LOG_TAG, "VideoPlayerActivity ....onDestroy()......");
        super.onDestroy();
        playerView.onDestroy();
    }

    // master
    private void startPlayer(String url) {
        Log.d(Constants.LOG_TAG, "input url = " + url);
        playerView.setPlayerViewCallback(this);
        playerView.play(url, false);
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
    public void onFragmentInteraction(String id) {

    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        playerView.dispatchKeyEvent(event);
        return true;
    }

}
