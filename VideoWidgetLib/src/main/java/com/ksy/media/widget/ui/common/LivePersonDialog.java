package com.ksy.media.widget.ui.common;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;

import com.ksy.mediaPlayer.widget.R;


public class LivePersonDialog extends Dialog {
    private Context mContext;
    private ImageView closeImageView;

    public LivePersonDialog(Context context) {
        super(context,R.style.ExitDialog);
        mContext=context;
    }

    public LivePersonDialog(Context context, int theme) {
        super(context, theme);
        mContext=context;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.live_layout_person_dialog);

        closeImageView = (ImageView) findViewById(R.id.imageViewClose);
        closeImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                LivePersonDialog.this.dismiss();
            }
        });

        this.setCanceledOnTouchOutside(false);

    }

}
