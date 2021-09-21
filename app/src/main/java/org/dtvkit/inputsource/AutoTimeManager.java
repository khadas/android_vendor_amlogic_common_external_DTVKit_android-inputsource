package com.droidlogic.dtvkit.inputsource;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.os.Build;
import android.os.Handler;
import android.provider.Settings;
import android.util.Log;

public class AutoTimeManager {
    private Context mContext;
    private ContentObserver mContentObserver;
    private ContentResolver mContentResolver;
    private BroadcastReceiver mBroadcastReceiver;
    private AutoManagerCallback mCb;
    private boolean mHasChangedBySys;
    private boolean mHasTriggledNetTime;

    public AutoTimeManager(Context context, AutoManagerCallback cb) {
        mContext = context;
        mCb = cb;
        if (mContext != null) {
            mContentResolver = mContext.getContentResolver();
            mContentObserver = new ContentObserver(new Handler()) {
                @Override
                public void onChange(boolean selfChange) {
                    if (mCb != null) {
                        mCb.enableDtvTimeSync(
                                isAutomaticTimeEnabled() && (mHasChangedBySys == false));
                    }
                }
            };
            mBroadcastReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    if (Intent.ACTION_TIME_CHANGED.equals(intent.getAction())) {
                        if (mCb != null) {
                            long systemTime = System.currentTimeMillis();
                            long dtvTime = mCb.getDtvRealTime();
                            long diff = systemTime - dtvTime;
                            if (diff > 10*1000 || diff < -10*1000) {
                                mHasChangedBySys = true;
                                mCb.onTimeChangedBySystem();
                            } else {
                                    //its a bad idea, but we cannot get if time has been changed
                                    //by network in the booting stage.
                                    //Since networtimeservice observing "Settings.Global.AUTO_TIME",
                                    //so we switch it to triggle the networktime do a sync whend dtv
                                    //changed the system time. If network time is avaliavle, it will
                                    //fix the time.
                                    //And this will just run only once.
                                if (!mHasTriggledNetTime && !mHasChangedBySys) {
                                    tryTriggleNetworkTimeSync();
                                    mHasTriggledNetTime = true;
                                }
                            }
                        }
                    }
                }
            };
        }
        if (mCb != null) {
            mCb.enableDtvTimeSync(isAutomaticTimeEnabled());
        }
    }

    public void start() {
        if (mContext != null) {
            if (mContentResolver != null && mContentObserver != null) {
                mContentResolver.registerContentObserver(
                        Settings.Global.getUriFor(Settings.Global.AUTO_TIME),
                        false, mContentObserver);
            }
            if (mBroadcastReceiver != null) {
                IntentFilter filter = new IntentFilter();
                filter.addAction(Intent.ACTION_TIME_CHANGED);
                mContext.registerReceiver(mBroadcastReceiver, filter);
            }
        }
    }

    private boolean isAutomaticTimeEnabled() {
        int value = 0;
        if (mContentResolver != null) {
            value = Settings.Global.getInt(mContentResolver, Settings.Global.AUTO_TIME, 0);
        }
        return (value != 0);
    }

    public void release() {
        if (mContentResolver != null) {
            mContentResolver.unregisterContentObserver(mContentObserver);
        }
        if (mContext != null && mBroadcastReceiver != null) {
            mContext.unregisterReceiver(mBroadcastReceiver);
        }
    }

    private void tryTriggleNetworkTimeSync() {
        if (mContentResolver != null && isAutomaticTimeEnabled()) {
            if (mContentObserver != null) {
                mContentResolver.unregisterContentObserver(mContentObserver);
            }
            int originalValue =
                Settings.Global.getInt(mContentResolver, Settings.Global.AUTO_TIME, 0);
            Settings.Global.putInt(mContentResolver, Settings.Global.AUTO_TIME, 0);
            Settings.Global.putInt(mContentResolver, Settings.Global.AUTO_TIME, originalValue);
            if (mContentObserver != null) {
                mContentResolver.registerContentObserver(
                        Settings.Global.getUriFor(Settings.Global.AUTO_TIME),
                        false, mContentObserver);
            }
        }
    }

    public interface AutoManagerCallback {
        long getDtvRealTime();
        void enableDtvTimeSync(boolean enable);
        /*time changed by network, telephyservice, or manual set*/
        void onTimeChangedBySystem();
    }
}
