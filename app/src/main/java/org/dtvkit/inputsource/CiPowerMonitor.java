package org.dtvkit.inputsource;

import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.PowerManager;
import android.util.Log;

import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;

import org.droidlogic.dtvkit.DtvkitGlueClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

class CiPowerMonitor {
    private String TAG = CiPowerMonitor.class.getSimpleName();
    private Context mContext;
    private boolean mIsStarted;
    private boolean mIsNeedNoticeResume;
    private static CiPowerMonitor mInstance;
    private static final String PROFILE_WAKE_LOCK_NAME = "dtvkit:dtv_ciplus_lock";
    private PowerManager.WakeLock mProfileWakeLock = null;
    private Handler mHandler;
    private final DtvkitGlueClient.SignalHandler mSignalHandler = new DtvkitGlueClient.SignalHandler() {
        @RequiresApi(api = Build.VERSION_CODES.M)
        @Override
        public void onSignal(String signal, JSONObject data) {
            if (("power_ok").equals(signal)) {
                stopCiplusPowerdownMonitor();
            }
        }
    };


    private CiPowerMonitor(@NonNull Context context) {
        mIsStarted = false;
        mIsNeedNoticeResume = false;
        mContext = context;
        mHandler = new Handler(mContext.getMainLooper());
    }

    public static CiPowerMonitor getInstance(@NonNull Context context) {
        if (mInstance == null) {
            mInstance = new CiPowerMonitor(context);
        }
        return mInstance;
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT_WATCH)
    public void onReceiveScreenOff() {
        if (!isPowerInterActive()) {
            startCiplusPowerdownMonitor();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT_WATCH)
    public void onReceiveScreenOn() {
        if (isPowerInterActive()) {
            stopTimeoutHandler();
            stopCiplusPowerdownMonitor();
            noticePowerResume();
        }
    }

    private void startCiplusPowerdownMonitor() {
        if (mIsStarted) {
            return;
        }
        DtvkitGlueClient.getInstance().registerSignalHandler(mSignalHandler);
        acquireCiplusWakeLock();
        noticePowerDown();
        startTimeoutHandler();
        mIsStarted = true;
    }

    private void stopCiplusPowerdownMonitor() {
        if (mIsStarted) {
            DtvkitGlueClient.getInstance().unregisterSignalHandler(mSignalHandler);
            releaseCiplusWakeLock();
            mIsStarted = false;
        }
    }

    private void startTimeoutHandler() {
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "timeout, release wakelock and go on");
                stopCiplusPowerdownMonitor();
            }
        }, 35*1000);
    }

    private void stopTimeoutHandler() {
        mHandler.removeCallbacksAndMessages(null);
    }

    private void noticePowerDown() {
        try {
            DtvkitGlueClient.getInstance().request("Player.enterStandby", new JSONArray());
            mIsNeedNoticeResume = true;
        } catch (Exception e) {
        }
        Log.d(TAG, "noticePowerDown");
    }

    private void noticePowerResume() {
        try {
            if (mIsNeedNoticeResume) {
                DtvkitGlueClient.getInstance().request("Player.leaveStandby", new JSONArray());
                mIsNeedNoticeResume = false;
            }
        } catch (Exception e) {
        }
    }

    private  synchronized void acquireCiplusWakeLock() {
        if (mProfileWakeLock == null) {
            PowerManager pm = (PowerManager) mContext.getSystemService(Context.POWER_SERVICE);
            mProfileWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, PROFILE_WAKE_LOCK_NAME);
            if (mProfileWakeLock != null) {
                Log.d(TAG, "acquireWakeLock " + PROFILE_WAKE_LOCK_NAME + " " + mProfileWakeLock);
                if (mProfileWakeLock.isHeld()) {
                    mProfileWakeLock.release();
                }
                mProfileWakeLock.acquire();
            }
        }
    }

    private synchronized void releaseCiplusWakeLock() {
        if (mProfileWakeLock != null) {
            Log.d(TAG, "releaseWakeLock " + PROFILE_WAKE_LOCK_NAME + " " + mProfileWakeLock);
            if (mProfileWakeLock.isHeld()) {
                mProfileWakeLock.release();
            }
            mProfileWakeLock = null;
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT_WATCH)
    private Boolean isPowerInterActive() {
        PowerManager powerManager = (PowerManager) mContext.getSystemService(Context.POWER_SERVICE);
        return powerManager.isInteractive();
    }
}


