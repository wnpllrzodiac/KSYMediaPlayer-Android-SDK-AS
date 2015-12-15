package com.ksy.media.widget.ui.common;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.ksy.mediaPlayer.widget.R;


public class LiveExitDialog extends Dialog {
    private Context mContext;
    private Button mConfirm;
    private Button mCancel;
    public LiveExitDialog(Context context) {
        super(context,R.style.ExitDialog);
        mContext=context;
    }

    public LiveExitDialog(Context context, int theme) {
        super(context, theme);
        mContext=context;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.live_layout_dialog);

        this.setCanceledOnTouchOutside(false);

        mConfirm= (Button) findViewById(R.id.dialog_confirm);
        mCancel= (Button) findViewById(R.id.dialog_cancel);

        mConfirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                System.exit(0);
//                LiveExitDialog.this.dismiss();
            }
        });

        mCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                LiveExitDialog.this.dismiss();
            }
        });
    }

}
