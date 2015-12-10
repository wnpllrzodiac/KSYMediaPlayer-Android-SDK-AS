package com.ksy.media.widget.ui.video;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

public class MediaPlayerPagerAdapter extends FragmentPagerAdapter {

    public static final int PAGER_COUNT = 3;

    public MediaPlayerPagerAdapter(FragmentManager fm) {
        super(fm);
    }

    @Override
    public Fragment getItem(int position) {
        switch (position) {
            case 0:
                return VideoCommentListFragment.newInstance(position + "", "");
            case 1:
                return VideoDetailFragment.newInstance(position + "", "");
            case 2:
                return VideoRecommendListFragment.newInstance(position + "", "");
        }

        return BlankFragment.newInstance(position + "", "");
    }

    @Override
    public int getCount() {
        return PAGER_COUNT;
    }

    @Override
    public CharSequence getPageTitle(int position) {
        return "Page" + position;
    }
}
