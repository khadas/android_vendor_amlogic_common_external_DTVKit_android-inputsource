package com.droidlogic.fragment;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;

import com.droidlogic.dtvkit.inputsource.R;

public class PasswordDialog extends Dialog {
    private Context mContext;
    private FrameLayout password_layout;

    public PasswordDialog(Context context, int theme) {
        super(context, theme);

        mContext = context;

        View inflate = LayoutInflater.from(mContext).inflate(R.layout.dialog_reset_password, null);
        setContentView(inflate);

        password_layout = (FrameLayout) findViewById(R.id.new_password_layout);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    public void show() {
        super.show();
    }
}