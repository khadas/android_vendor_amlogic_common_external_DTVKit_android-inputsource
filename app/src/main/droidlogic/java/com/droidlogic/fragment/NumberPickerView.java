package com.droidlogic.fragment;

import android.animation.Animator;
import android.animation.AnimatorInflater;
import android.content.Context;
import android.content.res.Resources;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.OverScroller;
import android.widget.TextView;
import android.widget.Toast;

import org.dtvkit.inputsource.R;

public class NumberPickerView extends FrameLayout {
    private static final String TAG = NumberPickerView.class.getSimpleName();

    private static final int NUMBER_VIEWS_RES_ID[] = {
            R.id.previous2_number,
            R.id.previous_number,
            R.id.current_number,
            R.id.next_number,
            R.id.next2_number};
    private static final int CURRENT_NUMBER_VIEW_INDEX = 2;

    private static Animator sFocusedNumberEnterAnimator;
    private static Animator sFocusedNumberExitAnimator;
    private static Animator sAdjacentNumberEnterAnimator;
    private static Animator sAdjacentNumberExitAnimator;

    private static float sAlphaForFocusedNumber;
    private static float sAlphaForAdjacentNumber;

    private int mMinValue;
    private int mMaxValue;
    private int mCurrentValue;
    private int mNextValue;
    private int mNumberViewHeight;
    private NumberPickerView mNextNumberPicker;
    private boolean mCancelAnimation;

    /**
     * false:   按右键不能调到下一个秘密框上
     * true：   按右键可以跳到下一个输密码的框上
     */
    public static final boolean  DEFAULT_ENABLE_NEXT_PICKER = true;

    /**
     * false:   可以上下键切换数字
     * true：   只能按遥控器上的数字键进行输入密码
     */
    private boolean mOnlyInputNumber = true;

    private boolean mEnableNextPicker = DEFAULT_ENABLE_NEXT_PICKER;

    //NumberPickerView是否获得了焦点
    private boolean mIsFocused = false;

    private final View mNumberViewHolder;
    private final View mBackgroundView;
    private final TextView[] mNumberViews;
    private final OverScroller mScroller;
    private TextView mTvAsterisk;

    private Toast mToast = null;

    private Context mContext;

    public NumberPickerView(Context context) {
        this(context, null);
    }

    public NumberPickerView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public NumberPickerView(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public NumberPickerView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        mContext = context;
        View view = inflate(context, R.layout.number_picker, this);
        mNumberViewHolder = view.findViewById(R.id.number_view_holder);
        mBackgroundView = view.findViewById(R.id.focused_background);
        mTvAsterisk = (TextView) view.findViewById(R.id.unfocused_front_panel);
        mNumberViews = new TextView[NUMBER_VIEWS_RES_ID.length];
        for (int i = 0; i < NUMBER_VIEWS_RES_ID.length; ++i) {
            mNumberViews[i] = (TextView) view.findViewById(NUMBER_VIEWS_RES_ID[i]);
        }

        if (mOnlyInputNumber == true) {
            mNumberViews[0].setVisibility(View.INVISIBLE);
            mNumberViews[1].setVisibility(View.INVISIBLE);
            mNumberViews[3].setVisibility(View.INVISIBLE);
            mNumberViews[4].setVisibility(View.INVISIBLE);
        }

        Resources resources = context.getResources();
        mNumberViewHeight = resources.getDimensionPixelOffset(R.dimen.pin_number_picker_text_view_height);

        mScroller = new OverScroller(context);

        mNumberViewHolder.setOnFocusChangeListener(new OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    mIsFocused = true;
                } else {
                    mIsFocused = false;
                }
                updateFocus();
            }
        });

        if (mOnlyInputNumber == false) {
            mNumberViewHolder.setOnKeyListener(new OnKeyListener() {
                @Override
                public boolean onKey(View v, int keyCode, KeyEvent event) {
                    if (event.getAction() == KeyEvent.ACTION_DOWN) {
                        switch (keyCode) {
                            case KeyEvent.KEYCODE_DPAD_UP:
                            case KeyEvent.KEYCODE_DPAD_DOWN: {
                                if (!mScroller.isFinished() || mCancelAnimation) {
                                    endScrollAnimation();
                                }
                                if (mScroller.isFinished() || mCancelAnimation) {
                                    mCancelAnimation = false;
                                    if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
                                        mNextValue = adjustValueInValidRange(mCurrentValue + 1);
                                        startScrollAnimation(true);
                                        mScroller.startScroll(0, 0, 0, mNumberViewHeight, getResources().getInteger(R.integer.pin_number_scroll_duration));
                                    } else {
                                        mNextValue = adjustValueInValidRange(mCurrentValue - 1);
                                        startScrollAnimation(false);
                                        mScroller.startScroll(0, 0, 0, -mNumberViewHeight, getResources().getInteger(R.integer.pin_number_scroll_duration));
                                    }
                                    updateText();
                                    invalidate();
                                }
                                return true;
                            }
                        }
                    } else if (event.getAction() == KeyEvent.ACTION_UP) {
                        switch (keyCode) {
                            case KeyEvent.KEYCODE_DPAD_UP:
                            case KeyEvent.KEYCODE_DPAD_DOWN: {
                                mCancelAnimation = true;
                                return true;
                            }
                        }
                    }
                    return false;
                }
            });
        }
        mNumberViewHolder.setScrollY(mNumberViewHeight);
    }

    public static void loadResources(Context context) {
        if (sFocusedNumberEnterAnimator == null) {
            TypedValue outValue = new TypedValue();
            context.getResources().getValue(R.dimen.pin_alpha_for_focused_number, outValue, true);
            sAlphaForFocusedNumber = outValue.getFloat();
            context.getResources().getValue(R.dimen.pin_alpha_for_adjacent_number, outValue, true);
            sAlphaForAdjacentNumber = outValue.getFloat();

            sFocusedNumberEnterAnimator = AnimatorInflater.loadAnimator(context, R.animator.pin_focused_number_enter);
            sFocusedNumberExitAnimator = AnimatorInflater.loadAnimator(context, R.animator.pin_focused_number_exit);
            sAdjacentNumberEnterAnimator = AnimatorInflater.loadAnimator(context, R.animator.pin_adjacent_number_enter);
            sAdjacentNumberExitAnimator = AnimatorInflater.loadAnimator(context, R.animator.pin_adjacent_number_exit);
        }
    }

    @Override
    public void computeScroll() {
        super.computeScroll();
        if (mScroller.computeScrollOffset()) {
            mNumberViewHolder.setScrollY(mScroller.getCurrY() + mNumberViewHeight);
            updateText();
            invalidate();
        } else if (mCurrentValue != mNextValue) {
            mCurrentValue = mNextValue;
        }
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_UP) {
            int keyCode = event.getKeyCode();

            if (keyCode >= KeyEvent.KEYCODE_0 && keyCode <= KeyEvent.KEYCODE_9) {
                int value = keyCode - KeyEvent.KEYCODE_0;

                mEnableNextPicker = true;

                if (value < mMinValue || value > mMaxValue) {
                    showToast("Value is not set");
                    return true;
                }
                setNextValue(keyCode - KeyEvent.KEYCODE_0);
            } else {
                return super.dispatchKeyEvent(event);
            }

            if (mNextNumberPicker == null) {
                mCurrentValue = mNextValue;
                mConfigDone.nextFocusAndGetResult();
                Log.d(TAG, TAG + ".Last input!");
            } else {
                mNextNumberPicker.requestFocus();
            }
            return true;
        }
        return super.dispatchKeyEvent(event);
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        mNumberViewHolder.setFocusable(enabled);
        for (int i = 0; i < NUMBER_VIEWS_RES_ID.length; ++i) {
            mNumberViews[i].setEnabled(enabled);
        }
    }

    public boolean getFocusedStatus() {
        return mIsFocused;
    }

    void startScrollAnimation(boolean scrollUp) {
        if (scrollUp) {
            sAdjacentNumberExitAnimator.setTarget(mNumberViews[1]);
            sFocusedNumberExitAnimator.setTarget(mNumberViews[2]);
            sFocusedNumberEnterAnimator.setTarget(mNumberViews[3]);
            sAdjacentNumberEnterAnimator.setTarget(mNumberViews[4]);
        } else {
            sAdjacentNumberEnterAnimator.setTarget(mNumberViews[0]);
            sFocusedNumberEnterAnimator.setTarget(mNumberViews[1]);
            sFocusedNumberExitAnimator.setTarget(mNumberViews[2]);
            sAdjacentNumberExitAnimator.setTarget(mNumberViews[3]);
        }
        sAdjacentNumberExitAnimator.start();
        sFocusedNumberExitAnimator.start();
        sFocusedNumberEnterAnimator.start();
        sAdjacentNumberEnterAnimator.start();
    }

    void endScrollAnimation() {
        sAdjacentNumberExitAnimator.end();
        sFocusedNumberExitAnimator.end();
        sFocusedNumberEnterAnimator.end();
        sAdjacentNumberEnterAnimator.end();
        mCurrentValue = mNextValue;
        mNumberViews[1].setAlpha(sAlphaForAdjacentNumber);
        mNumberViews[2].setAlpha(sAlphaForFocusedNumber);
        mNumberViews[3].setAlpha(sAlphaForAdjacentNumber);
    }

    public void setValueRange(int min, int max) {
        if (min > max) {
            throw new IllegalArgumentException("The min value should be greater than or equal to the max value");
        }
        mMinValue = min;
        mMaxValue = max;
        mNextValue = mCurrentValue = mMinValue - 1;
        clearText();
        mNumberViews[CURRENT_NUMBER_VIEW_INDEX].setText("—");
    }

    public void setNextNumberPicker(NumberPickerView picker) {
        mNextNumberPicker = picker;
    }

    public int getValue() {
        if (mCurrentValue < mMinValue || mCurrentValue > mMaxValue) {
            throw new IllegalStateException("Value is not set");
        }
        return mCurrentValue;
    }

    public boolean getEnableNextPicker() {
        return mEnableNextPicker;
    }

    public void setEnableNextPicker(boolean enable) {
        mEnableNextPicker = enable;
    }

    // Will take effect when the focus is updated.
    void setNextValue(int value) {
        if (value < mMinValue || value > mMaxValue) {
            throw new IllegalStateException("Value is not set");
        }
        mNextValue = adjustValueInValidRange(value);
    }

    void updateFocus() {
        endScrollAnimation();
        if (mNumberViewHolder.isFocused()) {
            mBackgroundView.setVisibility(View.VISIBLE);
            mTvAsterisk.setVisibility(View.GONE);
            updateText();

            /**
             * 加上这行代码的效果就是，焦点所在的密码格始终显示的是"—",这样就看不到用户输入的是什么数字，更安全。
             */
            mNumberViews[CURRENT_NUMBER_VIEW_INDEX].setText("—");
        } else {
            mBackgroundView.setVisibility(View.GONE);
            mTvAsterisk.setVisibility(View.VISIBLE);
            if (!mScroller.isFinished()) {
                mCurrentValue = mNextValue;
                mScroller.abortAnimation();
            }
            clearText();
            mNumberViewHolder.setScrollY(mNumberViewHeight);
        }
    }

    private void clearText() {
        for (int i = 0; i < NUMBER_VIEWS_RES_ID.length; ++i) {
            if (i != CURRENT_NUMBER_VIEW_INDEX) {
                mNumberViews[i].setText("");
            } else if (mCurrentValue >= mMinValue && mCurrentValue <= mMaxValue) {
                mNumberViews[i].setText(String.valueOf(mCurrentValue));
            }
        }
    }

    public void updateText() {
        if (mNumberViewHolder.isFocused()) {
            if (mCurrentValue < mMinValue || mCurrentValue > mMaxValue) {
                mNextValue = mCurrentValue = mMinValue;
            }
            int value = adjustValueInValidRange(mCurrentValue - CURRENT_NUMBER_VIEW_INDEX);
            for (int i = 0; i < NUMBER_VIEWS_RES_ID.length; ++i) {
                mNumberViews[i].setText(String.valueOf(adjustValueInValidRange(value)));
                value = adjustValueInValidRange(value + 1);
            }
        }
    }

    private int adjustValueInValidRange(int value) {
        int interval = mMaxValue - mMinValue + 1;
        if (value < mMinValue - interval || value > mMaxValue + interval) {
            throw new IllegalArgumentException("The value( " + value + ") is too small or too big to adjust");
        }
        return (value < mMinValue) ? value + interval : (value > mMaxValue) ? value - interval : value;
    }

    public interface InputCompleted {
        void nextFocusAndGetResult();
    }

    private InputCompleted mConfigDone;

    public void setAfterLastInputFocus(InputCompleted configDone) {
        mConfigDone = configDone;
    }

    private void showToast(String text) {
        /*if (null == mToast) {
            mToast = Toast.makeText(mContext, text, Toast.LENGTH_SHORT);
        } else {
            mToast.setText(text);
            mToast.setDuration(Toast.LENGTH_SHORT);
        }
        mToast.show();*/

        Toast.makeText(mContext, text, Toast.LENGTH_SHORT);
    }
}
