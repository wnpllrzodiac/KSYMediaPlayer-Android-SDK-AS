package com.ksy.media.widget.ui.video;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

import com.ksy.media.widget.ui.video.dummy.VideoListAdapter;
import com.ksy.mediaPlayer.widget.R;
import com.ksy.media.widget.ui.video.dummy.DummyContent;

import java.util.ArrayList;


public class VideoCommentListFragment extends Fragment implements AbsListView.OnItemClickListener {

    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    private String mParam1;
    private String mParam2;

    private OnFragmentInteractionListener mListener;
    private ListView mListView;
    private VideoListAdapter mVideoAdapter;
    private ArrayList<VideoCommentItem> items;

    public VideoCommentListFragment() {
    }

    public static VideoCommentListFragment newInstance(String param1, String param2) {
        VideoCommentListFragment fragment = new VideoCommentListFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
        items = new ArrayList<>();
        for (int i = 0; i < 15; i++) {
            VideoCommentItem item = new VideoCommentItem();
            item.setComment(getString(R.string.video_commment_item_comment));
            item.setTime(getString(R.string.video_comment_item_time));
            item.setUser(getString(R.string.video_comment_item_user));
            items.add(item);
        }
        mVideoAdapter = new VideoListAdapter(getActivity(), items);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_item, container, false);
        // Set the adapter
        mListView = (ListView) view.findViewById(R.id.video_comment_list);
        mListView.setAdapter(mVideoAdapter);
        // Set OnItemClickListener so we can be notified on item clicks
        mListView.setOnItemClickListener(this);
        return view;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (OnFragmentInteractionListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        if (null != mListener) {
            mListener.onFragmentInteraction(DummyContent.ITEMS.get(position).id);
        }
    }

    public void setEmptyText(CharSequence emptyText) {
        View emptyView = mListView.getEmptyView();

        if (emptyView instanceof TextView) {
            ((TextView) emptyView).setText(emptyText);
        }
    }

    public interface OnFragmentInteractionListener {
        public void onFragmentInteraction(String id);
    }

}
