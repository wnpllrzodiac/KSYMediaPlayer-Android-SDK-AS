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
        if (position == 0) {
            return ListFragment.newInstance(position + "", "");
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
