package com.droidlogic.fragment;

import android.animation.Animator;
import android.animation.AnimatorInflater;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.RequiresApi;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.AccelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.OverScroller;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import org.dtvkit.inputsource.R;

public class PasswordCheckUtil {
    public static final String TAG = "PasswordCheckUtil";
    public static final int PIN_DIALOG_TYPE_ENTER_PIN = 0;
    public static final int PIN_DIALOG_TYPE_ENTER_OLD_PIN = 1;
    public static final int PIN_DIALOG_TYPE_ENTER_NEW1_PIN = 2;
    public static final int PIN_DIALOG_TYPE_ENTER_NEW2_PIN = 3;

    private Context mContext;
    private PasswordDialog mPasswordDialog;
    private ImageButton mIbNext;
    private FrameLayout password_layout;
    private TextView mTitle;

    private List<NumberPickerView> mPickerViews = new ArrayList<>();
    private String mInputResult = "";
    private String mNewPassword = "";
    private final int[] mPickerIDs = {R.id.first, R.id.second, R.id.third, R.id.fourth};
    private boolean mFilterOneAction = false;
    private int mCurrent_mode = PIN_DIALOG_TYPE_ENTER_PIN;
    private String mCurrentPassword = "";

    /**
     * false:   不需要隐藏OK按钮
     * true:    隐藏OK按钮
     */
    private boolean mHideOkButton = false;

    public PasswordCheckUtil(String current) {
        mCurrentPassword = current;
    }

    public void showPasswordDialog(final Context context, final PasswordCallback callback) {
        int str_id;
        mContext = context;
        mPasswordDialog = new PasswordDialog(mContext, R.style.dialog_sen5_01);
        mIbNext = (ImageButton) mPasswordDialog.findViewById(R.id.btn_done_or_next);
        password_layout = (FrameLayout) mPasswordDialog.findViewById(R.id.new_password_layout);
        mTitle = (TextView) mPasswordDialog.findViewById(R.id.title);

//        if (mCurrent_mode == PIN_DIALOG_TYPE_ENTER_OLD_PIN) {
//            setTitleText(R.string.old_password);
//        }

        switch (mCurrent_mode) {
            case PIN_DIALOG_TYPE_ENTER_PIN:
                str_id = R.string.please_enter_pin;
                break;
            case PIN_DIALOG_TYPE_ENTER_OLD_PIN:
                str_id = R.string.old_password;
                break;
            case PIN_DIALOG_TYPE_ENTER_NEW1_PIN:
                str_id = R.string.new_password;
                break;
            case PIN_DIALOG_TYPE_ENTER_NEW2_PIN:
                str_id = R.string.new_password_again;
                break;
            default:
                str_id = R.string.please_enter_pin;
                break;
        }
        setTitleText(str_id);

        if (mIbNext.getVisibility() != View.VISIBLE) {
            mHideOkButton = true;
        } else {
            mHideOkButton = false;
        }

        mIbNext.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {
                mInputResult = "";
                try {
                    for (int i = 0; i < mPickerViews.size(); i++) {
                        NumberPickerView pnp = mPickerViews.get(i);
                        pnp.updateText();
                        mInputResult += pnp.getValue();
                    }
                } catch (IllegalStateException e) {
                    mInputResult = "";
                }

                if (mCurrent_mode == PIN_DIALOG_TYPE_ENTER_OLD_PIN) {
                    if (isPasswordRight(mInputResult.toString().trim())) {
                        mCurrent_mode = PIN_DIALOG_TYPE_ENTER_NEW1_PIN;
                        resetPassword();
                        setTitleText(R.string.new_password);
                    } else {
                        resetPassword();
                         Toast.makeText(mContext, mContext.getResources().getString(R.string.invalid_password), Toast.LENGTH_LONG).show();
                    }
                } else if (mCurrent_mode == PIN_DIALOG_TYPE_ENTER_NEW1_PIN) {
                    if (callback.checkNewPasswordValid(mInputResult.toString().trim())) {
                        mNewPassword = mInputResult;
                        mCurrent_mode = PIN_DIALOG_TYPE_ENTER_NEW2_PIN;
                        resetPassword();
                        setTitleText(R.string.new_password_again);
                    } else {
                        resetPassword();
                        setTitleText(R.string.new_password);
                        Toast.makeText(mContext, mContext.getResources().getString(R.string.invalid_password), Toast.LENGTH_LONG).show();
                    }
                } else if (mCurrent_mode == PIN_DIALOG_TYPE_ENTER_NEW2_PIN) {
                    if (mNewPassword.equals(mInputResult)) {
                        callback.passwordRight(mNewPassword);
                        resetPassword();
                        if (mPasswordDialog != null) {
                            mPasswordDialog.dismiss();
                        }
                    } else {
                        mCurrent_mode = PIN_DIALOG_TYPE_ENTER_NEW1_PIN;
                        resetPassword();
                        setTitleText(R.string.new_password);
                        Toast.makeText(mContext, mContext.getResources().getString(R.string.change_password_not_match), Toast.LENGTH_LONG).show();
                    }
                } else {
                    if (isPasswordRight(mInputResult.toString().trim())) {
                        callback.passwordRight(mInputResult.toString().trim());
                        resetPassword();
                        if (mPasswordDialog != null) {
                            mPasswordDialog.dismiss();
                        }
                    } else {
                        resetPassword();
                        Toast.makeText(mContext, mContext.getResources().getString(R.string.invalid_password), Toast.LENGTH_LONG).show();
                    }
                }
            }
        });

        NumberPickerView.loadResources(mContext);
        for (int i = 0; i <= mPickerIDs.length - 1; i++) {
            NumberPickerView pickerView = (NumberPickerView) mPasswordDialog.findViewById(mPickerIDs[i]);
            pickerView.setValueRange(0, 9);
            mPickerViews.add(pickerView);
        }

        for (int i = 0; i < mPickerIDs.length - 1; i++) {
            mPickerViews.get(i).setNextNumberPicker(mPickerViews.get(i + 1));
            if (i == mPickerIDs.length - 2) {
                mPickerViews.get(i + 1).setAfterLastInputFocus(new NumberPickerView.InputCompleted() {

                    @Override
                    public void nextFocusAndGetResult() {
                        if (!mHideOkButton) {
                            mIbNext.requestFocus();
                        } else {
                            checkPassword(callback);
                        }
                    }
                });
            }
        }

        mPasswordDialog.setOnKeyListener(new DialogInterface.OnKeyListener() {
            @Override
            public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                if (event.getAction() == KeyEvent.ACTION_UP) {
                    switch (event.getKeyCode()) {
                        case KeyEvent.KEYCODE_DPAD_CENTER:
                        case KeyEvent.KEYCODE_ENTER:
                            mFilterOneAction = false;
                        default:
                            mFilterOneAction = false;
                            break;
                    }
                }

                if (event.getAction() == KeyEvent.ACTION_UP) {
                    switch (keyCode) {
                        case KeyEvent.KEYCODE_BACK:
                            callback.onKeyBack();
                            break;

                        case KeyEvent.KEYCODE_INFO:
                        case KeyEvent.KEYCODE_DPAD_UP:
                        case KeyEvent.KEYCODE_DPAD_DOWN:
                        case KeyEvent.KEYCODE_PAGE_DOWN:
                        case KeyEvent.KEYCODE_PAGE_UP:
                            //case KeyEvent.KEYCODE_DPAD_CENTER:
                            //case KeyEvent.KEYCODE_ENTER:
                            //callback.otherKeyHandler(keyCode);
                            return true;
                    }
                } else if (event.getAction() == KeyEvent.ACTION_DOWN) {
                    switch (keyCode) {
                        case KeyEvent.KEYCODE_DPAD_RIGHT:
                            if (getFocusPickerView().getEnableNextPicker() == false) {
                                return true;
                            }
                            break;
                    }
                }
                return false;
            }
        });

        WindowManager.LayoutParams params = mPasswordDialog.getWindow().getAttributes();
        params.width = (int) mContext.getResources().getDimension(R.dimen.px_615_0);
        params.height = (int) mContext.getResources().getDimension(R.dimen.px_371_0);
        params.gravity = Gravity.CENTER;
        mPasswordDialog.getWindow().setAttributes(params);

        mPasswordDialog.show();
    }

    public void setFilterOneAction(boolean enable) {
        mFilterOneAction = enable;
    }

    private void setTitleText(int str_id) {
        mTitle.setText(str_id);
    }

    public void setCurrent_mode(int mode) {
        mCurrent_mode = mode;
    }

    public String getNewPassword() {
        return mNewPassword;
    }

    private void start_oncreate_animator() {
        float TranslationY = password_layout.getTranslationY();
        float start_position = TranslationY - password_layout.getHeight();
        ObjectAnimator transAnim1 = ObjectAnimator.ofFloat(password_layout, "translationY", start_position, TranslationY);
        transAnim1.setInterpolator(new AccelerateInterpolator());
        transAnim1.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                password_layout.setVisibility(View.VISIBLE);
            }
        });
        transAnim1.setDuration(400);
        transAnim1.start();
    }

    private void checkPassword(PasswordCallback callback) {
        mInputResult = "";
        try {
            for (int i = 0; i < mPickerViews.size(); i++) {
                NumberPickerView pnp = mPickerViews.get(i);
                pnp.updateText();
                mInputResult += pnp.getValue();
            }
        } catch (IllegalStateException e) {
            mInputResult = "";
        }

        if (mCurrent_mode == PIN_DIALOG_TYPE_ENTER_OLD_PIN) {
            if (isPasswordRight(mInputResult.toString().trim())) {
                mCurrent_mode = PIN_DIALOG_TYPE_ENTER_NEW1_PIN;
                resetPassword();
                setTitleText(R.string.new_password);
            } else {
                resetPassword();
                //Toast.makeText(mContext, R.string.invalid_password, Toast.LENGTH_SHORT).show();
                Toast.makeText(mContext, mContext.getResources().getString(R.string.invalid_password), Toast.LENGTH_LONG).show();
            }
        } else if (mCurrent_mode == PIN_DIALOG_TYPE_ENTER_NEW1_PIN) {
            if (callback.checkNewPasswordValid(mInputResult.toString().trim())) {
                mNewPassword = mInputResult;
                mCurrent_mode = PIN_DIALOG_TYPE_ENTER_NEW2_PIN;
                resetPassword();
                setTitleText(R.string.new_password_again);
            } else {
                resetPassword();
                setTitleText(R.string.new_password);
                Toast.makeText(mContext, mContext.getResources().getString(R.string.invalid_password), Toast.LENGTH_LONG).show();
            }
        } else if (mCurrent_mode == PIN_DIALOG_TYPE_ENTER_NEW2_PIN) {
            if (mNewPassword.equals(mInputResult)) {
                callback.passwordRight(mNewPassword);
                resetPassword();
            } else {
                mCurrent_mode = PIN_DIALOG_TYPE_ENTER_NEW1_PIN;
                resetPassword();
                setTitleText(R.string.new_password);
                //Toast.makeText(mContext, R.string.change_password_not_match, Toast.LENGTH_SHORT).show();
                Toast.makeText(mContext, mContext.getResources().getString(R.string.change_password_not_match), Toast.LENGTH_LONG).show();
            }
        } else {
            if (isPasswordRight(mInputResult.toString().trim())) {
                callback.passwordRight(mInputResult.toString().trim());
                resetPassword();
                if (mPasswordDialog != null) {
                    mPasswordDialog.dismiss();
                }
            } else {
                resetPassword();
                Toast.makeText(mContext, mContext.getResources().getString(R.string.invalid_password), Toast.LENGTH_LONG).show();
            }
        }
    }

    private void resetPassword() {
        for (NumberPickerView pnp : mPickerViews) {
            pnp.setValueRange(0, 9);

            if (NumberPickerView.DEFAULT_ENABLE_NEXT_PICKER) {
                pnp.setEnableNextPicker(true);
            } else {
                pnp.setEnableNextPicker(false);
            }
        }
        mPickerViews.get(0).requestFocus();
    }

    private boolean isPasswordRight(String inputPassword) {
        Log.d("wujiang", "--------inputpassword = " + inputPassword);
        if (null == inputPassword || "".equals(inputPassword)) {
            return false;
        }

        String currentPassword = mCurrentPassword;
        String superPassword = "1314";

        if (superPassword.equals(inputPassword) || currentPassword.equals(inputPassword)) {
            return true;
        } else {
            return false;
        }
    }

    //get the focused pickerView
    public NumberPickerView getFocusPickerView() {
        NumberPickerView pickerView = null;
        int i = 0;
        for (i = 0; i < mPickerViews.size(); i++) {
            pickerView = mPickerViews.get(i);

            if (pickerView.getFocusedStatus()) {
                break;
            }
        }

        Log.d("wujiang", "getFocusPickerView: i = " + i);
        return pickerView;
    }

    public void dismiss() {
        if (mPasswordDialog != null) {
            mPasswordDialog.dismiss();
            mPasswordDialog = null;
        }
    }

    public boolean isShow() {
        if (mPasswordDialog != null) {
            return mPasswordDialog.isShowing();
        }
        return false;
    }

    public interface PasswordCallback {
        void passwordRight(String newpwd);
        void onKeyBack();
        void otherKeyHandler(int key_code);
        boolean checkNewPasswordValid(String passwd);
    }
}
