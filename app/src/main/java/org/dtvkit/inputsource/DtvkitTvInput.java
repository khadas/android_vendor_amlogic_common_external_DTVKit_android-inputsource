package org.dtvkit.inputsource;

import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.ContentObserver;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.PorterDuff;
import android.graphics.Paint;
import android.database.Cursor;
import android.media.PlaybackParams;
import android.media.tv.TvContentRating;
import android.media.tv.TvContract;
import android.media.tv.TvInputInfo;
import android.media.tv.TvInputManager;
import android.media.tv.TvInputService;
import android.media.tv.TvTrackInfo;
import android.graphics.Color;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.util.TypedValue;
import android.graphics.Typeface;
import android.widget.ImageView;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.RectF;
import android.graphics.drawable.ColorDrawable;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;

import android.content.IntentFilter;
import android.content.BroadcastReceiver;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.content.ContentProviderClient;
import android.content.ContentProviderOperation;
import android.content.ContentValues;
import android.media.tv.TvInputHardwareInfo;
import android.media.tv.TvInputManager.Hardware;
import android.media.tv.TvInputManager.HardwareCallback;
import android.media.tv.TvInputHardwareInfo;
import android.media.tv.TvStreamConfig;
import android.text.TextUtils;
import java.io.IOException;
import org.xmlpull.v1.XmlPullParserException;
import android.content.Intent;
import java.io.File;

import android.media.AudioManager;
import android.app.AlarmManager;
import android.app.PendingIntent;

import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Looper;
import android.os.Environment;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.util.Log;
import android.util.LongSparseArray;
import android.view.Surface;
import android.view.View;
import android.view.KeyEvent;
import android.os.HandlerThread;
import android.text.TextUtils;
import android.widget.FrameLayout;
import android.view.accessibility.CaptioningManager;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.view.WindowManager;
import android.widget.Button;
import android.os.SystemProperties;
import android.widget.Toast;
import java.util.Calendar;
import java.util.Date;

import org.dtvkit.companionlibrary.EpgSyncJobService;
import org.dtvkit.companionlibrary.model.Channel;
import org.dtvkit.companionlibrary.model.InternalProviderData;
import org.dtvkit.companionlibrary.model.Program;
import org.dtvkit.companionlibrary.model.RecordedProgram;
import org.dtvkit.companionlibrary.utils.TvContractUtils;
import org.droidlogic.dtvkit.DtvkitGlueClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.os.PowerManager;
import java.lang.reflect.Method;
import android.os.SystemClock;

/*
dtvkit
 */
import com.droidlogic.settings.PropSettingManager;
import com.droidlogic.settings.ConvertSettingManager;
import com.droidlogic.settings.SysSettingManager;
import com.droidlogic.settings.ConstantManager;
import com.droidlogic.fragment.ParameterMananer;

//import com.droidlogic.app.tv.TvControlManager;
import com.droidlogic.app.SystemControlManager;
import com.droidlogic.app.SystemControlEvent;
import com.droidlogic.app.CCSubtitleView;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.Objects;
import java.util.Iterator;
import java.util.Map;
import java.util.HashMap;
import java.util.Arrays;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.concurrent.TimeUnit;
import java.util.Comparator;
import java.util.Collections;

import android.media.MediaCodec;

import java.io.InputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.util.zip.ZipFile;
import java.util.zip.ZipException;
import java.util.zip.ZipEntry;
import java.util.Enumeration;


public class DtvkitTvInput extends TvInputService implements SystemControlEvent.DisplayModeListener  {
    private static final String TAG = "DtvkitTvInput";
    private LongSparseArray<Channel> mChannels;
    private ContentResolver mContentResolver;

    protected String mInputId = null;
   // private TvControlManager Tcm = null;
    private static final int MSG_DO_TRY_SCAN = 0;
    private static final int RETRY_TIMES = 10;
    private static final int ASPECT_MODE_AUTO = 0;
    private static final int ASPECT_MODE_CUSTOM = 5;
    private static final int DISPLAY_MODE_NORMAL = 6;
    private int retry_times = RETRY_TIMES;

    private static final int TTX_MODE_NORMAL = 0;
    private static final int TTX_MODE_TRANSPARENT = 1;
    private static final int TTX_MODE_SEPARATE = 2;

    protected TvInputInfo mTvInputInfo = null;
    protected TvInputInfo mPipTvInputInfo = null;
    protected Hardware mHardware;
    protected Hardware mPipHardware;
    private TvInputHardwareInfo mTvInputHardwareInfo = null;
    private TvInputHardwareInfo mPipTvInputHardwareInfo = null;
    protected TvStreamConfig[] mConfigs;
    protected TvStreamConfig[] mPipConfigs;
    private TvInputManager mTvInputManager;
    private SysSettingManager mSysSettingManager = null;
    //private DtvkitTvInputSession mSession;
    protected HandlerThread mHandlerThread = null;
    private SystemControlEvent mSystemControlEvent;
    protected Handler mInputThreadHandler = null;
    volatile private Semaphore mMainSessionSemaphore = new Semaphore(1);
    volatile private Semaphore mPipSessionSemaphore = new Semaphore(1);
    final private static int SEMAPHORE_TIME_OUT = 3000;

    /*associate audio*/
    protected boolean mAudioADAutoStart = false;
    protected int mAudioADMixingLevel = 50;
    protected int mAudioADVolume = 100;

    volatile private boolean mDvbNetworkChangeSearchStatus = false;
    private Channel mMainDvbChannel = null;
    private Channel mPipDvbChannel = null;
    // Mutex for all mutable shared state.
    private final Object mLock = new Object();

    //ci authentication
    private boolean mCiAuthenticatedStatus = false;

    private enum PlayerState {
        STOPPED, PLAYING
    }
    private enum RecorderState {
        STOPPED, STARTING, RECORDING
    }
    private enum State {
        NO, YES, DISABLED
    }
    private class AvailableState {
        AvailableState() { state = State.NO; }
        public void setYes() { setYes(false); }
        public void setNo() { setNo(false); }
        public void setYes(boolean force) { set(State.YES, force); }
        public void setNo(boolean force) { set(State.NO, force); }
        public void disable() { set(State.DISABLED, true); }
        public boolean isAvailable() { return state == State.YES; }
        public String toString() { return state == State.NO ? "no" : (state == State.YES ? "yes" : "disabled"); }

        private void set(State stat, boolean force) {
            if (force || state != State.DISABLED)
                state = stat;
        }
        private State state;
    }
    private RecorderState timeshiftRecorderState = RecorderState.STOPPED;
    private boolean timeshifting = false;
    private int numRecorders = 0;
    private int numActiveRecordings = 0;
    private boolean scheduleTimeshiftRecording = false;
    private Handler scheduleTimeshiftRecordingHandler = null;
    //private Runnable timeshiftRecordRunnable;
    private long mDtvkitTvInputSessionCount = 0;
    private long mDtvkitRecordingSessionCount = 0;
    private DataMananer mDataMananer;
    private ParameterMananer mParameterMananer;
    private MediaCodec mMediaCodec1;
    private MediaCodec mMediaCodec2;
    SystemControlManager mSystemControlManager;
    //Define file type
    private final int SYSFS = 0;
    private final int PROP = 1;

    private boolean recordingPending = false;

    private static String unZipDirStr = "vendorfont";
    private static String uncryptDirStr = "font";
    private static String fonts = "font";
    private String intenAction = "org.dtvkit.inputsource.AutomaticSearching";
    private AutomaticSearchingReceiver mAutomaticSearchingReceiver = null;
    public final int SUBCTL_HK_DVBSUB = 0x02;
    public final int SUBCTL_HK_TTXSUB = 0x04;
    public final int SUBCTL_HK_SCTE27 = 0x08;
    public final int SUBCTL_HK_TTXPAG = 0x10;
    public final int SUBCTL_HK_CC     = 0x20;
    public final int SUBCTL_HK_MHEG5S = 0x40;
    public final int Default_SubCtlFlg = (SUBCTL_HK_DVBSUB|SUBCTL_HK_SCTE27
                                         |SUBCTL_HK_TTXSUB|SUBCTL_HK_TTXPAG
                                         /*SUBCTL_HK_CC*/);

    //record all sessions by index
    private final Map<Long, DtvkitTvInputSession> mTunerSessions = new HashMap<>();
    private final Map<Long, DtvkitRecordingSession> mTunerRecordingSession = new HashMap<>();
    private final Object mTunerSessionLock = new Object();
    private final Object mTunerRecordSessionLock = new Object();

    private static final int INDEX_FOR_MAIN = 0;
    private static final int INDEX_FOR_PIP = 1;

    private String lastMhegUri = null;

    //stream change and update dialog
    private AlertDialog mStreamChangeUpdateDialog = null;

    private AlarmManager mAlarmManager = null;
    public DtvkitTvInput() {
        Log.i(TAG, "DtvkitTvInput");
    }

    protected final BroadcastReceiver mParentalControlsBroadcastReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(TvInputManager.ACTION_BLOCKED_RATINGS_CHANGED)
                   || action.equals(TvInputManager.ACTION_PARENTAL_CONTROLS_ENABLED_CHANGED)) {
                boolean isParentControlEnabled = mTvInputManager.isParentalControlsEnabled();
                Log.d(TAG, "BLOCKED_RATINGS_CHANGED isParentControlEnabled = " + isParentControlEnabled);
                //use osd to hide video instead
                /*if (isParentControlEnabled != getParentalControlOn()) {
                    setParentalControlOn(isParentControlEnabled);
                }
                if (isParentControlEnabled && mSession != null) {
                    mSession.syncParentControlSetting();
                }*/
            }
        }
    };

    protected final BroadcastReceiver mBootBroadcastReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent != null) {
                String action = intent.getAction();
                if (action.equals(Intent.ACTION_LOCKED_BOOT_COMPLETED)) {
                    Log.d(TAG, "onReceive ACTION_LOCKED_BOOT_COMPLETED");
                    //can't init here as storage may not be ready
                } else if (action.equals(Intent.ACTION_BOOT_COMPLETED )) {
                    Log.d(TAG, "onReceive ACTION_BOOT_COMPLETED");
                    sendEmptyMessageToInputThreadHandler(MSG_CHECK_TV_PROVIDER_READY);
                }
            }
        }
    };

    protected final BroadcastReceiver mStorageEventReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(Intent.ACTION_MEDIA_MOUNTED)) {
                String mountPath = intent.getData().getPath();
                Log.d(TAG, "mStorageEventReceiver mountPath = " + mountPath);
                if (getFeatureSupportNewTvApp() && SysSettingManager.isStoragePath(mountPath)) {
                    if (mInputThreadHandler != null) {
                        //mInputThreadHandler.removeMessages(MSG_ADD_DTVKIT_DISK_PATH);
                        Message mess = mInputThreadHandler.obtainMessage(MSG_ADD_DTVKIT_DISK_PATH, 0, 0, mountPath);
                        boolean info = mInputThreadHandler.sendMessageDelayed(mess, 0);
                        Log.d(TAG, "sendMessage MSG_ADD_DTVKIT_DISK_PATH " + info);
                    }
                }
            } else {
                Log.d(TAG, "mStorageEventReceiver other action");
            }
        }
    };

    protected final BroadcastReceiver mCiTestBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (PropSettingManager.getBoolean(PropSettingManager.CI_PROFILE_TEST, false)) {
                Log.d(TAG, "TEST_CASE intent = " + (intent != null ? intent.getExtras() : null));
                if (intent != null) {
                    try {
                        String action = intent.getAction();
                        if (ConstantManager.ACTION_CI_PLUS_INFO.equals(action)) {
                            String command = intent.getStringExtra(ConstantManager.CI_PLUS_COMMAND);
                            DtvkitTvInputSession mainSession = getMainTunerSession();
                            switch (command) {
                                case ConstantManager.VALUE_CI_PLUS_COMMAND_SEARCH_REQUEST:
                                    //op search request
                                    //am broadcast -a ci_plus_info --es command "search_request" --ei search_module 1
                                    showToast(ConstantManager.VALUE_CI_PLUS_COMMAND_SEARCH_REQUEST);
                                    if (mainSession != null) {
                                        Bundle request = new Bundle();
                                        request.putInt(ConstantManager.VALUE_CI_PLUS_SEARCH_MODULE, intent.getIntExtra(ConstantManager.VALUE_CI_PLUS_SEARCH_MODULE, -1));
                                        request.putString(ConstantManager.CI_PLUS_COMMAND, ConstantManager.VALUE_CI_PLUS_COMMAND_SEARCH_REQUEST);
                                        mainSession.sendBundleToAppByTif(action, request);
                                    }
                                    break;
                                case ConstantManager.VALUE_CI_PLUS_COMMAND_SEARCH_FINISHED:
                                    //op search finished
                                    //am broadcast -a ci_plus_info --es command "search_finished"
                                    showToast(ConstantManager.VALUE_CI_PLUS_COMMAND_SEARCH_FINISHED);
                                    if (mainSession != null) {
                                        Bundle request = new Bundle();
                                        request.putString(ConstantManager.CI_PLUS_COMMAND, ConstantManager.VALUE_CI_PLUS_COMMAND_SEARCH_FINISHED);
                                        mainSession.sendBundleToAppByTif(action, request);
                                    }
                                    break;
                                case ConstantManager.VALUE_CI_PLUS_COMMAND_CHANNEL_UPDATED:
                                    //op search finished
                                    //am broadcast -a ci_plus_info --es command "channel_updated"
                                    showToast(ConstantManager.VALUE_CI_PLUS_COMMAND_CHANNEL_UPDATED);
                                    if (mainSession != null) {
                                        Bundle request = new Bundle();
                                        request.putString(ConstantManager.CI_PLUS_COMMAND, ConstantManager.VALUE_CI_PLUS_COMMAND_CHANNEL_UPDATED);
                                        mainSession.sendBundleToAppByTif(action, request);
                                    }
                                    break;
                                case ConstantManager.VALUE_CI_PLUS_COMMAND_HOST_CONTROL:
                                    //op search finished
                                    //am broadcast -a ci_plus_info --es command "host_control" --el host_control_channel 1 --es tune_type "service_tune"
                                    showToast(ConstantManager.VALUE_CI_PLUS_COMMAND_HOST_CONTROL);
                                    if (mainSession != null) {
                                        Bundle request = new Bundle();
                                        request.putLong(ConstantManager.VALUE_CI_PLUS_CHANNEL, intent.getLongExtra(ConstantManager.VALUE_CI_PLUS_CHANNEL, -1));
                                        request.putString(ConstantManager.VALUE_CI_PLUS_TUNE_TYPE, intent.getStringExtra(ConstantManager.VALUE_CI_PLUS_TUNE_TYPE));
                                        request.putString(ConstantManager.CI_PLUS_COMMAND, ConstantManager.VALUE_CI_PLUS_COMMAND_HOST_CONTROL);
                                        mainSession.sendBundleToAppByTif(action, request);
                                    }
                                    break;
                                case ConstantManager.VALUE_CI_PLUS_COMMAND_IGNORE_INPUT:
                                    //op search finished
                                    //am broadcast -a ci_plus_info --es command "ignore_input"
                                    showToast(ConstantManager.VALUE_CI_PLUS_COMMAND_IGNORE_INPUT);
                                    if (mainSession != null) {
                                        Bundle request = new Bundle();
                                        request.putString(ConstantManager.CI_PLUS_COMMAND, ConstantManager.VALUE_CI_PLUS_COMMAND_IGNORE_INPUT);
                                        mainSession.sendBundleToAppByTif(action, request);
                                    }
                                    break;
                                case ConstantManager.VALUE_CI_PLUS_COMMAND_RECEIVE_INPUT:
                                    //op search finished
                                    //am broadcast -a ci_plus_info --es command "receive_input"
                                    showToast(ConstantManager.VALUE_CI_PLUS_COMMAND_RECEIVE_INPUT);
                                    if (mainSession != null) {
                                        Bundle request = new Bundle();
                                        request.putString(ConstantManager.CI_PLUS_COMMAND, ConstantManager.VALUE_CI_PLUS_COMMAND_RECEIVE_INPUT);
                                        mainSession.sendBundleToAppByTif(action, request);
                                    }
                                    break;
                                default:
                                    break;
                            }
                        }
                    } catch (Exception e) {
                        Log.d(TAG, "mCiTestBroadcastReceiver Exception = " + e.getMessage());
                    }
                }
            } else {
                Log.d(TAG, "TEST_CASE hasn't been enabled");
            }
        }
    };

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "onCreate");
        // Create background thread
        mHandlerThread = new HandlerThread("DtvkitInputWorker");
        mHandlerThread.start();
        initInputThreadHandler();
        mTvInputManager = (TvInputManager)this.getSystemService(Context.TV_INPUT_SERVICE);
        mContentResolver = getContentResolver();
        mContentResolver.registerContentObserver(TvContract.Channels.CONTENT_URI, true, mContentObserver);
        mContentResolver.registerContentObserver(TvContract.RecordedPrograms.CONTENT_URI, true, mRecordingsContentObserver);
        mAlarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(TvInputManager.ACTION_BLOCKED_RATINGS_CHANGED);
        intentFilter.addAction(TvInputManager.ACTION_PARENTAL_CONTROLS_ENABLED_CHANGED);
        registerReceiver(mParentalControlsBroadcastReceiver, intentFilter);

        IntentFilter intentFilter1 = new IntentFilter();
        intentFilter1.addAction(Intent.ACTION_LOCKED_BOOT_COMPLETED);
        intentFilter1.addAction(Intent.ACTION_BOOT_COMPLETED);
        registerReceiver(mBootBroadcastReceiver, intentFilter1);

        mAutomaticSearchingReceiver = new AutomaticSearchingReceiver();
        IntentFilter intentFilter2 = new IntentFilter();
        intentFilter2.addAction(intenAction);
        registerReceiver(mAutomaticSearchingReceiver, intentFilter2);

        IntentFilter intentFilter3 = new IntentFilter();
        intentFilter3.addAction(ConstantManager.ACTION_CI_PLUS_INFO);
        registerReceiver(mCiTestBroadcastReceiver, intentFilter3);

        IntentFilter intentFilter4 = new IntentFilter();
        intentFilter4.addAction(Intent.ACTION_MEDIA_MOUNTED);
        intentFilter4.addDataScheme(ContentResolver.SCHEME_FILE);
        registerReceiver(mStorageEventReceiver, intentFilter4);

        sendEmptyMessageToInputThreadHandler(MSG_START_CA_SETTINGS_SERVICE);
        sendEmptyMessageToInputThreadHandler(MSG_CHECK_TV_PROVIDER_READY);
    }

    //input work handler define
    //dtvkit init message 1~50
    protected static final int MSG_START_CA_SETTINGS_SERVICE = 1;
    protected static final int MSG_CHECK_TV_PROVIDER_READY = 2;
    protected static final int MSG_CHECK_DTVKIT_SATELLITE = 3;
    protected static final int MSG_ADD_DTVKIT_DISK_PATH = 4;

    protected static final int PERIOD_RIGHT_NOW = 0;
    protected static final int PERIOD_CHECK_TV_PROVIDER_DELAY = 10;
    protected static final int PERIOD_TIPS_FOR_TOO_MUCH_TIME_MAX = 1500;

    //dtvkit tune session message 51~100

    //dtvkit tune record session message 101~150
    protected static final int MSG_UPDATE_RECORDING_PROGRAM = 101;

    protected static final int MSG_UPDATE_RECORDING_PROGRAM_DELAY = 200;

    private void initInputThreadHandler() {
        mInputThreadHandler = new Handler(mHandlerThread.getLooper(), new Handler.Callback() {
            @Override
            public boolean handleMessage(Message msg) {
                //Log.d(TAG, "mInputThreadHandler ************* " + msg.what + " start");
                long startSystemMills = System.currentTimeMillis();
                switch (msg.what) {
                    case MSG_START_CA_SETTINGS_SERVICE:{
                        //startCaSettingServices();
                        break;
                    }
                    case MSG_CHECK_TV_PROVIDER_READY:{
                        if (!checkTvProviderAvailable()) {
                            sendDelayedEmptyMessageToInputThreadHandler(MSG_CHECK_TV_PROVIDER_READY, PERIOD_CHECK_TV_PROVIDER_DELAY);
                        } else {
                            initDtvkitTvInput();
                        }
                        break;
                    }
                    case MSG_CHECK_DTVKIT_SATELLITE:{
                        checkDtvkitSatelliteUpdateStatus();
                        break;
                    }
                    case MSG_UPDATE_RECORDING_PROGRAM:{
                        syncRecordingProgramsWithDtvkit((JSONObject)msg.obj);
                        break;
                    }
                    case MSG_ADD_DTVKIT_DISK_PATH:{
                        recordingAddDiskPath(SysSettingManager.convertStoragePathToMediaPath((String)msg.obj) + "/" + SysSettingManager.PVR_DEFAULT_FOLDER);
                        break;
                    }
                    default:
                        break;
                }
                long endSystemMills = System.currentTimeMillis();
                long cost = endSystemMills - startSystemMills;
                if (cost > PERIOD_TIPS_FOR_TOO_MUCH_TIME_MAX) {
                    Log.d(TAG, "mInputThreadHandler ------------- " + msg.what + " over cost = " + cost + " !!!!!!");
                } else {
                    //Log.d(TAG, "mInputThreadHandler ------------- " + msg.what + " over cost = " + cost);
                }
                return true;
            }
        });
    }

    private boolean sendMessageToInputThreadHandler(int what, int arg1, int arg2, Object obj, int delay) {
        boolean result = false;
        Message mess = null;
        if (mInputThreadHandler != null) {
            mess = mInputThreadHandler.obtainMessage(what, arg1, arg2, obj);
            result = mInputThreadHandler.sendMessageDelayed(mess, delay);
        }
        return result;
    }

    private boolean sendEmptyMessageToInputThreadHandler(int what) {
        boolean result = false;
        Message mess = null;
        if (mInputThreadHandler != null) {
            mess = mInputThreadHandler.obtainMessage(what, 0, 0, null);
            result = mInputThreadHandler.sendMessageDelayed(mess, 0);
        }
        return result;
    }

    private boolean sendDelayedEmptyMessageToInputThreadHandler(int what, int delay) {
        boolean result = false;
        Message mess = null;
        if (mInputThreadHandler != null) {
            mess = mInputThreadHandler.obtainMessage(what, 0, 0, null);
            result = mInputThreadHandler.sendMessageDelayed(mess, delay);
        }
        return result;
    }

    private void removeMessageInInputThreadHandler(int what) {
        if (mInputThreadHandler != null) {
            mInputThreadHandler.removeMessages(what);
        }
    }

    private boolean mIsInited = false;
    private int mCheckTvProviderTimeOut = 5 * 1000;//10s

    private boolean checkTvProviderAvailable() {
        boolean result = false;
        ContentProviderClient tvProvider = this.getContentResolver().acquireContentProviderClient(TvContract.AUTHORITY);
        if (tvProvider != null) {
            result = true;
            Log.d(TAG, "checkTvProviderAvailable ready");
        } else {
            mCheckTvProviderTimeOut -= 10;
            if (mCheckTvProviderTimeOut < 0) {
                Log.d(TAG, "checkTvProviderAvailable timeout");
            } else if (mCheckTvProviderTimeOut % 500 == 0) {//update per 500ms
                Log.d(TAG, "checkTvProviderAvailable wait count = " + ((5 * 1000 - mCheckTvProviderTimeOut) / 10));
            }
        }
        return result;
    }

    private int getSubtitleFlag() {
        int ret = Default_SubCtlFlg;
        if (mSystemControlManager != null) {
            ret = mSystemControlManager.getPropertyInt("persist.vendor.tif.subtitleflg", Default_SubCtlFlg);
        }

        return ret;
    }

    private synchronized void initDtvkitTvInput() {
        if (mIsInited) {
            Log.d(TAG, "initDtvkitTvInput already");
            return;
        }
        Log.d(TAG, "initDtvkitTvInput start");
        mSysSettingManager = new SysSettingManager(this);
        mDataMananer = new DataMananer(this);
        onChannelsChanged();
        onRecordingsChanged();
        mSystemControlManager = SystemControlManager.getInstance();
        mSystemControlEvent = new SystemControlEvent(this);
        mSystemControlEvent.setDisplayModeListener(this);
        mSystemControlManager.setListener(mSystemControlEvent);
        //DtvkitGlueClient.getInstance().setSystemControlHandler(mSysControlHandler);
        DtvkitGlueClient.getInstance().registerSignalHandler(mRecordingManagerHandler);
        mParameterMananer = new ParameterMananer(this, DtvkitGlueClient.getInstance());
        updateRecorderNumber();
        sendEmptyMessageToInputThreadHandler(MSG_CHECK_DTVKIT_SATELLITE);
        resetRecordingPath();
        int subFlg = getSubtitleFlag();
        if (subFlg >= SUBCTL_HK_DVBSUB) {
            DtvkitGlueClient.getInstance().attachSubtitleCtl(subFlg & 0xFF);
            if ((subFlg & SUBCTL_HK_CC) == SUBCTL_HK_CC) {
                initFont(); //init closed caption font
            }
        }

        Log.d(TAG, "initDtvkitTvInput end");
        mIsInited = true;
    }

    private void initFont() {
        File uncryteDir = new File(this.getDataDir(), uncryptDirStr);
        if (uncryteDir == null || uncryteDir.listFiles() == null || uncryteDir.listFiles().length == 0 ) {
            unzipUncry(uncryteDir);
         }
    }

    private void unzipUncry(File uncryteDir) {
        File zipFile = new File(this.getDataDir(), "fonts.zip");
        File unZipDir = new File(this.getDataDir(), unZipDirStr);
        try {
            copyToFileOrThrow(this.getAssets().open(fonts), zipFile);
            upZipFile(zipFile, unZipDir.getCanonicalPath());
            uncryteDir.mkdirs();
            DtvkitGlueClient.getInstance().doUnCrypt(unZipDir.getCanonicalPath()+"/", uncryteDir.getCanonicalPath()+"/");
        } catch(IOException ex) {
            ex.printStackTrace();
        }
    }

    private void copyToFileOrThrow(InputStream inputStream, File destFile)
            throws IOException {
        if (destFile.exists()) {
            destFile.delete();
        }
        FileOutputStream out = new FileOutputStream(destFile);
        try {
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) >= 0) {
                out.write(buffer, 0, bytesRead);
            }
        } finally {
            out.flush();
            try {
                out.getFD().sync();
            } catch (IOException e) {
            }
            out.close();
        }
    }

    private  void upZipFile(File zipFile, String folderPath)
            throws ZipException, IOException {
            File desDir = new File(folderPath);
            if (!desDir.exists()) {
                desDir.mkdirs();
            }
            ZipFile zf = new ZipFile(zipFile);
            for (Enumeration<?> entries = zf.entries(); entries.hasMoreElements();) {
                ZipEntry entry = ((ZipEntry) entries.nextElement());
                InputStream in = zf.getInputStream(entry);
                String str = folderPath;
                File desFile = new File(str, java.net.URLEncoder.encode(
                        entry.getName(), "UTF-8"));
                if (!desFile.exists()) {
                    File fileParentDir = desFile.getParentFile();
                    if (!fileParentDir.exists()) {
                        fileParentDir.mkdirs();
                    }
                }
                OutputStream out = new FileOutputStream(desFile);
                byte buffer[] = new byte[1024 * 1024];
                int realLength = in.read(buffer);
                while (realLength != -1) {
                    out.write(buffer, 0, realLength);
                    realLength = in.read(buffer);
                }

                out.close();
                in.close();

            }
    }

    private synchronized void updateRecorderNumber() {
        Log.d(TAG, "updateRecorderNumber start");
        numRecorders = recordingGetNumRecorders();
    }

    private synchronized void updateTunerNumber() {
        Log.d(TAG, "updateTunerNumber start");
        int realNumTuners = getNumTuners();
        int numTuners = realNumTuners;
        if (numTuners > 0) {
            boolean multiFrequencySupport = false;
            if (numTuners > 2) {
                //currently ohm_mxl258c has 4 tuner and can server for pip or fcc by configure, besides, it can support play multi frequency
                multiFrequencySupport = true;
                numTuners = 3;//match for one record and one play pip
            } else if (numTuners == 2) {
                //currently ohm has 2 tuner but can only match for single frequency, besides, it can only debug pip or fcc
                numTuners = 3;//match for one record and one play and pip
            } else {
                //one tuner and only can play and record within same frequency
                numTuners = 2;//match for one record and one play
            }
            if (mTvInputInfo != null && mTvInputInfo.getTunerCount() == numTuners) {
                Log.d(TAG, "updateTunerNumber same count " + numTuners);
                return;
            }
            Bundle extras = new Bundle();
            extras.putBoolean(PropSettingManager.ENABLE_PIP_SUPPORT, getFeatureSupportPip());
            extras.putBoolean(PropSettingManager.ENABLE_FCC_SUPPORT, getFeatureSupportFcc());
            extras.putBoolean(PropSettingManager.ENABLE_MULTI_FREQUENCY_SUPPORT, multiFrequencySupport);
            extras.putInt(PropSettingManager.ENABLE_TUNER_NUMBER, realNumTuners);
            mTvInputInfo = buildTvInputInfo(mTvInputHardwareInfo, numTuners, extras);
        } else {
            mTvInputInfo = buildTvInputInfo(mTvInputHardwareInfo, 1, null);
        }
        mTvInputManager.updateTvInputInfo(mTvInputInfo);
        Log.d(TAG, "updateTunerNumber end");
    }

    private void checkDtvkitSatelliteUpdateStatus() {
        if (mDataMananer != null && mParameterMananer != null) {
            if (mDataMananer.getIntParameters(DataMananer.DTVKIT_IMPORT_SATELLITE_FLAG) > 0) {
                Log.d(TAG, "checkDtvkitSatelliteUpdateStatus has imported already");
            } else {
                if (mParameterMananer.importSatellitesAndLnbs()) {
                    mDataMananer.saveIntParameters(DataMananer.DTVKIT_IMPORT_SATELLITE_FLAG, 1);
                }
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "onDestroy");
        unregisterReceiver(mParentalControlsBroadcastReceiver);
        unregisterReceiver(mBootBroadcastReceiver);
        unregisterReceiver(mAutomaticSearchingReceiver);
        unregisterReceiver(mCiTestBroadcastReceiver);
        unregisterReceiver(mStorageEventReceiver);
        mContentResolver.unregisterContentObserver(mContentObserver);
        mContentResolver.unregisterContentObserver(mRecordingsContentObserver);
        DtvkitGlueClient.getInstance().unregisterSignalHandler(mRecordingManagerHandler);
        //DtvkitGlueClient.getInstance().setSystemControlHandler(null);
        mHandlerThread.getLooper().quitSafely();
        mHandlerThread = null;
        mInputThreadHandler.removeCallbacksAndMessages(null);
        mInputThreadHandler = null;
        mMainSessionSemaphore.release();
        mPipSessionSemaphore.release();
    }

    private boolean setTVAspectMode(int mode) {
        boolean used=false;
        try {
            JSONArray args = new JSONArray();
            args.put(mode);
            used = DtvkitGlueClient.getInstance().request("Dvb.setTVAspectMode", args).getBoolean("data");
            Log.i(TAG, "dvb setTVAspectMode, used:" + used);
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }
        return used;
    }

    @Override
    public void onSetDisplayMode(int mode) {
        Log.i(TAG, "onSetDisplayMode " + mode);
        if (mode == DISPLAY_MODE_NORMAL)
            setTVAspectMode(ASPECT_MODE_AUTO);
        else
            setTVAspectMode(ASPECT_MODE_CUSTOM);
    }

    @Override
    public final Session onCreateSession(String inputId) {
        Log.i(TAG, "onCreateSession " + inputId);
        initDtvkitTvInput();
        mDtvkitTvInputSessionCount++;
        DtvkitTvInputSession session = new DtvkitTvInputSession(this);
        addTunerSession(session);
        //mSystemControlManager.SetDtvKitSourceEnable(1);
        return session;
    }

    private void addTunerSession(DtvkitTvInputSession session) {
        synchronized (mTunerSessionLock) {
            mTunerSessions.put(session.mCurrentDtvkitTvInputSessionIndex, session);
            Log.i(TAG, "addTunerSession index = " + session.mCurrentDtvkitTvInputSessionIndex);
        }
    }

    private void removeTunerSession(DtvkitTvInputSession session) {
        synchronized (mTunerSessionLock) {
            mTunerSessions.remove(session.mCurrentDtvkitTvInputSessionIndex);
            Log.i(TAG, "removeTunerSession index = " + session.mCurrentDtvkitTvInputSessionIndex);
        }
    }

    private DtvkitTvInputSession getTunerSession(long index) {
        DtvkitTvInputSession result = null;
        synchronized (mTunerSessionLock) {
            result = mTunerSessions.get(index);
            Log.i(TAG, "getTunerSession index = " + index);
        }
        return result;
    }

    private DtvkitTvInputSession getMainTunerSession() {
        DtvkitTvInputSession result = null;
        synchronized (mTunerSessionLock) {
            DtvkitTvInputSession session = null;
            long index = 0;
            for (Map.Entry<Long, DtvkitTvInputSession> entry : mTunerSessions.entrySet()) {
                index = entry.getKey();
                session = entry.getValue();
                if (!session.mIsPip && session.mTuned && !session.mReleased) {
                    result = session;
                    Log.i(TAG, "getMainTunerSession index = " + index);
                }
            }
        }
        return result;
    }

    private DtvkitTvInputSession getPipTunerSession() {
        DtvkitTvInputSession result = null;
        synchronized (mTunerSessionLock) {
            DtvkitTvInputSession session = null;
            long index = 0;
            for (Map.Entry<Long, DtvkitTvInputSession> entry : mTunerSessions.entrySet()) {
                index = entry.getKey();
                session = entry.getValue();
                if (session.mIsPip && session.mTuned && !session.mReleased) {
                    result = session;
                    Log.i(TAG, "getPipTunerSession index = " + index);
                }
            }
        }
        return result;
    }

    protected void setInputId(String name) {
        mInputId = name;
        Log.d(TAG, "set input id to " + mInputId);
    }

    private class DtvkitOverlayView extends FrameLayout {

        private NativeOverlayView nativeOverlayView;
        private CiMenuView ciOverlayView;
        private TextView mText;
        private ImageView mTuningImage;
        private RelativeLayout mRelativeLayout;
        private CCSubtitleView mCCSubView = null;
        private SubtitleServerView mSubServerView = null;
        private int w;
        private int h;

        private boolean mhegTookKey = false;
        private KeyEvent lastMhegKey = null;
        final private static int MHEG_KEY_INTERVAL = 65;

        public DtvkitOverlayView(Context context,       Handler mainHandler) {
            super(context);

            Log.i(TAG, "onCreateDtvkitOverlayView");

            nativeOverlayView = new NativeOverlayView(getContext());
            ciOverlayView = new CiMenuView(getContext());
            mSubServerView = new SubtitleServerView(getContext(), mainHandler);
            mCCSubView     = new CCSubtitleView(getContext());
            this.addView(mSubServerView);
            this.addView(nativeOverlayView);
            this.addView(mCCSubView);
            this.addView(ciOverlayView);
            initRelativeLayout();
        }

        public void destroy() {
            ciOverlayView.destroy();
            removeView(mSubServerView);
            removeView(nativeOverlayView);
            removeView(ciOverlayView);
            removeView(mRelativeLayout);
            removeView(mCCSubView);
        }

        public void hideOverLay() {
            if (mSubServerView != null) {
                mSubServerView.setVisibility(View.GONE);
            }
            if (nativeOverlayView != null) {
                nativeOverlayView.setVisibility(View.GONE);
            }
            if (ciOverlayView != null) {
                ciOverlayView.setVisibility(View.GONE);
            }
            if (mText != null) {
                mText.setVisibility(View.GONE);
            }
            if (mRelativeLayout != null) {
                mRelativeLayout.setVisibility(View.GONE);
            }

            if (mCCSubView != null) {
                mCCSubView.hide();
            }

            setVisibility(View.GONE);
        }

        public void showOverLay() {
            if (mSubServerView != null) {
                mSubServerView.setVisibility(View.VISIBLE);
            }

            if (nativeOverlayView != null) {
                nativeOverlayView.setVisibility(View.VISIBLE);
            }
            if (ciOverlayView != null) {
                ciOverlayView.setVisibility(View.VISIBLE);
            }
            if (mText != null) {
                mText.setVisibility(View.VISIBLE);
            }
            if (mRelativeLayout != null) {
                mRelativeLayout.setVisibility(View.VISIBLE);
            }

            if (mCCSubView != null) {
                mCCSubView.setVisibility(View.VISIBLE);
            }
            setVisibility(View.VISIBLE);
        }

        private void initRelativeLayout() {
            if (mText == null && mRelativeLayout == null) {
                Log.d(TAG, "initRelativeLayout");
                mRelativeLayout = new RelativeLayout(getContext());
                mText = new TextView(getContext());
                mTuningImage = new ImageView(getContext());
                ViewGroup.LayoutParams linearLayoutParams = new ViewGroup.LayoutParams(LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.FILL_PARENT);
                mRelativeLayout.setLayoutParams(linearLayoutParams);
                mRelativeLayout.setBackgroundColor(Color.TRANSPARENT);

                //add scrambled text
                RelativeLayout.LayoutParams textLayoutParams = new RelativeLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                textLayoutParams.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE);
                mText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 30);
                mText.setGravity(Gravity.CENTER);
                mText.setTypeface(Typeface.DEFAULT_BOLD);
                mText.setTextColor(Color.WHITE);
                //mText.setText("RelativeLayout");
                mText.setVisibility(View.GONE);
                //add tunong view
                RelativeLayout.LayoutParams imageLayoutParams = new RelativeLayout.LayoutParams(LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.FILL_PARENT);
                ColorDrawable colorDrawable = new ColorDrawable();
                colorDrawable.setColor(Color.argb(255, 0, 0, 0));
                mTuningImage.setImageDrawable(colorDrawable);
                mTuningImage.setVisibility(View.GONE);

                mRelativeLayout.addView(mTuningImage, imageLayoutParams);
                mRelativeLayout.addView(mText, textLayoutParams);
                this.addView(mRelativeLayout);
            } else {
                Log.d(TAG, "initRelativeLayout already");
            }
        }

        public void setTeletextMix(int status){
            int subFlg = getSubtitleFlag();
            if (subFlg >= SUBCTL_HK_DVBSUB) {
                mSubServerView.setTeletexTransparent(status == TTX_MODE_TRANSPARENT);
                if (status == TTX_MODE_SEPARATE) {
                    mSubServerView.setSize(1920/2, 0, 1920, 1080);
                } else {
                    mSubServerView.setSize(0, 0, 1920, 1080);
                }
            } else
                if (status == TTX_MODE_SEPARATE) {
                    nativeOverlayView.setSize(1920/2, 0, 1920, 1080);
                } else {
                    nativeOverlayView.setSize(0, 0, 1920, 1080);
                }
        }

        public void showScrambledText(String text) {
            if (mText != null && mRelativeLayout != null) {
                Log.d(TAG, "showText");
                mText.setText(text);
                if (mText.getVisibility() != View.VISIBLE) {
                    mText.setVisibility(View.VISIBLE);
                }
                //display black background
                showTuningImage(null);
            }
        }

        public void hideScrambledText() {
            if (mText != null && mRelativeLayout != null) {
                Log.d(TAG, "hideText");
                mText.setText("");
                if (mText.getVisibility() != View.GONE) {
                    mText.setVisibility(View.GONE);
                }
                //hide black background
                hideTuningImage();
            }
        }

        public void showTuningImage(Drawable drawable) {
            if (mTuningImage != null && mRelativeLayout != null) {
                Log.d(TAG, "showTuningImage");
                if (drawable != null) {
                    mTuningImage.setImageDrawable(drawable);
                }
                if (mTuningImage.getVisibility() != View.VISIBLE) {
                    mTuningImage.setVisibility(View.VISIBLE);
                }
            }
        }

        public void hideTuningImage() {
            if (mTuningImage != null && mRelativeLayout != null) {
                Log.d(TAG, "hideTuningImage");
                if (mTuningImage.getVisibility() != View.GONE) {
                    mTuningImage.setVisibility(View.GONE);
                }
            }
        }

        public void setSize(int width, int height) {
            w = width;
            h = height;
            nativeOverlayView.setSize(width, height);
            mSubServerView.setSize(width, height);
        }

        public void showCCSubtitle(String json) {
             if (mCCSubView != null) {
                 mCCSubView.showJsonStr(json);
             }
        }

        public void hideCCSubtitle() {
            if (mCCSubView != null) {
                mCCSubView.hide();
            }
        }

        public boolean checkMhegKeyLimit(KeyEvent event) {
            if (lastMhegKey == null)
                return false;
            if (lastMhegKey.getKeyCode() != event.getKeyCode())
                return false;
            if (event.getEventTime() - lastMhegKey.getEventTime() > MHEG_KEY_INTERVAL)
                return false;
            return true;
        }

        public boolean handleKeyDown(int keyCode, KeyEvent event) {
            boolean result;
            if (ciOverlayView.handleKeyDown(keyCode, event)) {
                mhegTookKey = false;
                result = true;
            }
            else if (!(checkMhegKeyLimit(event)) && mhegKeypress(keyCode)){
                mhegTookKey = true;
                result = true;
                lastMhegKey = event;
            }
            else {
                mhegTookKey = false;
                result = false;
            }
            return result;
        }

        public boolean handleKeyUp(int keyCode, KeyEvent event) {
            boolean result;

            if (ciOverlayView.handleKeyUp(keyCode, event) || mhegTookKey) {
                result = true;
            } else {
                result = false;
            }
            mhegTookKey = false;
            lastMhegKey = null;

            return result;
        }

        private boolean mhegKeypress(int keyCode) {
            boolean used=false;
            try {
                JSONArray args = new JSONArray();
                args.put(keyCode);
                used = DtvkitGlueClient.getInstance().request("Mheg.notifyKeypress", args).getBoolean("data");
                Log.i(TAG, "Mheg keypress, used:" + used);
            } catch (Exception e) {
                Log.e(TAG, e.getMessage());
            }
            return used;
        }
    }

    private boolean checkDtvkitRecordingsExists(String uri, JSONArray recordings) {
        boolean exists = false;
        if (recordings != null) {
            for (int i = 0; i < recordings.length(); i++) {
                try {
                    String u = recordings.getJSONObject(i).getString("uri");
                    if (uri.equals(u)) {
                        exists = true;
                        break;
                    }
                } catch (JSONException e) {
                    Log.e("TAG", "checkDtvkitRecordingsExists JSONException " + e.getMessage());
                }
            }
        }
        return exists;
    }

    private final DtvkitGlueClient.SignalHandler mRecordingManagerHandler = new DtvkitGlueClient.SignalHandler() {
        @RequiresApi(api = Build.VERSION_CODES.M)
        @Override
        public void onSignal(String signal, JSONObject data) {
            if (signal.equals("RecordingsChanged")) {
                Log.i(TAG, "mRecordingManagerHandler onSignal: " + signal + " : " + data.toString());
                if (mInputThreadHandler != null) {
                    mInputThreadHandler.removeMessages(MSG_UPDATE_RECORDING_PROGRAM);
                    Message mess = mInputThreadHandler.obtainMessage(MSG_UPDATE_RECORDING_PROGRAM, 0, 0, data);
                    boolean info = mInputThreadHandler.sendMessageDelayed(mess, MSG_UPDATE_RECORDING_PROGRAM_DELAY);
                    Log.d(TAG, "sendMessage MSG_UPDATE_RECORDING_PROGRAM " + info);
                }
            }
        }
    };

    private void syncRecordingProgramsWithDtvkit(JSONObject data) {
        ArrayList<RecordedProgram> recordingsInDB = new ArrayList();
        ArrayList<RecordedProgram> recordingsResetInvalidInDB = new ArrayList();
        ArrayList<RecordedProgram> recordingsResetValidInDB = new ArrayList();
        Cursor cursor = null;

        try {
            cursor = mContentResolver.query(TvContract.RecordedPrograms.CONTENT_URI, RecordedProgram.PROJECTION, null, null, TvContract.RecordedPrograms._ID + " DESC");
            while (null != cursor && cursor.moveToNext()) {
                recordingsInDB.add(RecordedProgram.fromCursor(cursor));
            }
        } catch (Exception e) {
            Log.e(TAG, "RecordingPrograms query Failed = " + e.getMessage());
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        JSONArray recordings = recordingGetListOfRecordings();

        Log.d(TAG, "recordings: db[" + recordingsInDB.size() + "] dtvkit[" + recordings.length() + "]");
        for (RecordedProgram rec : recordingsInDB) {
            Log.d(TAG, "db: " + rec.getRecordingDataUri());
        }
        for (int i = 0; i < recordings.length(); i++) {
            try {
                Log.d(TAG, "dtvkit: " + recordings.getJSONObject(i).getString("uri"));
            } catch (JSONException e) {
                Log.d(TAG, e.getMessage());
            }
        }

        if (recordingsInDB.size() != 0) {
            for (RecordedProgram recording : recordingsInDB) {
                String uri = recording.getRecordingDataUri();
                if (checkDtvkitRecordingsExists(uri, recordings)) {
                    if (recording.getRecordingDataBytes() <= 0) {
                        Log.d(TAG, "make recording valid: "+uri);
                        recordingsResetValidInDB.add(recording);
                    }
                } else {
                    if (recording.getRecordingDataBytes() > 0) {
                        Log.d(TAG, "make recording invalid: "+uri);
                        recordingsResetInvalidInDB.add(recording);
                    }
                }
            }

            ArrayList<ContentProviderOperation> ops = new ArrayList();
            for (RecordedProgram recording : recordingsResetInvalidInDB) {
                Uri uri = TvContract.buildRecordedProgramUri(recording.getId());
                String dataUri = recording.getRecordingDataUri();
                ContentValues update = new ContentValues();
                update.put(TvContract.RecordedPrograms.COLUMN_RECORDING_DATA_BYTES, 0l);
                ops.add(ContentProviderOperation.newUpdate(uri)
                    .withValues(update)
                    .build());
            }
            for (RecordedProgram recording : recordingsResetValidInDB) {
                Uri uri = TvContract.buildRecordedProgramUri(recording.getId());
                String dataUri = recording.getRecordingDataUri();
                ContentValues update = new ContentValues();
                update.put(TvContract.RecordedPrograms.COLUMN_RECORDING_DATA_BYTES, 1024 * 1024l);
                ops.add(ContentProviderOperation.newUpdate(uri)
                    .withValues(update)
                    .build());
            }
            try {
                mContentResolver.applyBatch(TvContract.AUTHORITY, ops);
                //notify it as background process can receive it imediately
                if (isSdkAfterAndroidQ()) {
                    getContentResolver().notifyChange(TvContract.RecordedPrograms.CONTENT_URI, null, 1 << 15/*ContentResolver.NOTIFY_NO_DELAY*/);
                }
            } catch (Exception e) {
                Log.e(TAG, "recordings DB update failed.");
            }
        }
    }

    class SubtitleServerView extends View {
        Bitmap overlay1 = null;
        Bitmap overlay2 = null;
        Bitmap overlay_update = null;
        Bitmap overlay_draw = null;
        Bitmap region = null;
        int region_width = 0;
        int region_height = 0;
        int left = 0;
        int top = 0;
        int width = 0;
        int height = 0;
        Rect src, dst;
        private Handler mHandler = null;

        int mPauseExDraw = 0;
        boolean mTtxTransparent = false;
        static final int SUBTITLE_SUB_TYPE_DVB  = 5;
        static final int SUBTITLE_SUB_TYPE_TTX  = 9;
        static final int SUBTITLE_SUB_TYPE_CLOSED_CATPTION = 10;
        static final int SUBTITLE_SUB_TYPE_SCTE = 11;
        static final int TTX_FIXED_WIDTH = 480;
        static final int TTX_FIXED_HEIGHT = 525;

        static final int MSG_SUBTITLE_SHOW_CLOSED_CAPTION = 5;
        protected static final int MSG_SET_TELETEXT_MIX_NORMAL = 6;
        protected static final int MSG_SET_TELETEXT_MIX_TRANSPARENT = 7;
        protected static final int MSG_SET_TELETEXT_MIX_SEPARATE = 8;

        Semaphore sem = new Semaphore(1);
        private final DtvkitGlueClient.SubtitleListener mSubListener = new DtvkitGlueClient.SubtitleListener() {
            @Override
            public void drawEx(int parserType, int src_width, int src_height, int dst_x, int dst_y, int dst_width, int dst_height, int[] data) {
                Log.v(TAG, "SubtitleServiceDraw: type= "+ parserType + ", srcw= " + src_width +
               ", srch= " + src_height + ", x= " + dst_x + ", y= " + dst_y +
               ", dstw= " + dst_width + ", dsth= " + dst_height + ", pause= " + mPauseExDraw);
                if (mPauseExDraw > 0) {
                    return;
                }

                if (overlay1 == null) {
                    /* TODO The overlay size should come from the tif (and be updated on onOverlayViewSizeChanged) */
                    /* Create 2 layers for double buffering */
                    overlay1 = Bitmap.createBitmap(1920, 1080, Bitmap.Config.ARGB_8888);
                    overlay2 = Bitmap.createBitmap(1920, 1080, Bitmap.Config.ARGB_8888);

                    overlay_draw = overlay1;
                    overlay_update = overlay2;

                    /* Clear the overlay that will be drawn initially */
                    Canvas canvas = new Canvas(overlay_draw);
                    canvas.drawColor(0, PorterDuff.Mode.CLEAR);
                }

                /* TODO Temporary private usage of API. Add explicit methods if keeping this mechanism */
                if (src_width == 0 || src_height == 0) {
                    if (dst_width == 9999) {
                        /* 9999 dst_width indicates the overlay should be cleared */
                        Canvas canvas = new Canvas(overlay_update);
                        canvas.drawColor(0, PorterDuff.Mode.CLEAR);
                    }
                    else if (dst_height == 9999) {
                        /* 9999 dst_height indicates the drawn regions should be displayed on the overlay */
                        /* The update layer is now ready to be displayed so switch the overlays
                         * and use the other one for the next update */
                        sem.acquireUninterruptibly();
                        Bitmap temp = overlay_draw;
                        overlay_draw = overlay_update;
                        src = new Rect(0, 0, overlay_draw.getWidth(), overlay_draw.getHeight());
                        overlay_update = temp;
                        sem.release();
                        postInvalidate();
                        return;
                    }
                    else {
                        /* 0 dst_width and 0 dst_height indicates to add the region to overlay */
                        if (region != null) {
                            float scaleX = (float)(overlay_draw.getWidth())/(float)(width);
                            float scaleY = (float)(overlay_draw.getHeight())/(float)(height);
                            float ttxScaleVideo = 1.0f;
                            float ttxScaleDst   = 1.0f;
                            boolean ttxPageTmpTrans = (dst_x == 1);//ttx not use x,y as coords
                            Canvas canvas = new Canvas(overlay_update);
                            Rect src_l = new Rect(0, 0, region_width, region_height);
                            Rect dst_l;
                            int l = left;
                            int t = top;
                            int w = region_width;
                            int h = region_height;
                            if (parserType == SUBTITLE_SUB_TYPE_TTX) {
                                l = (width - TTX_FIXED_WIDTH)/2;
                                t = (height - TTX_FIXED_HEIGHT)/2;
                                w = TTX_FIXED_WIDTH;
                                h= TTX_FIXED_HEIGHT;
                                if (playerIsTeletextOn() && !mTtxTransparent && !ttxPageTmpTrans) {
                                    canvas.drawColor(0xFF000000);
                                }
                            }
                            dst_l = new Rect((int)(l*scaleX), (int)(t*scaleY),
                                    (int)((w + l)*scaleX),
                                    (int)((h + t)*scaleY));
                            Log.d(TAG, "SubtitleServiceDraw: dst_l left=" + dst_l.left + ", top=" + dst_l.top
                                + ", right=" + dst_l.right + ", bottom=" + dst_l.bottom);

                            Paint paint = new Paint();
                            paint.setAntiAlias(true);
                            paint.setFilterBitmap(true);
                            paint.setDither(true);
                            canvas.drawBitmap(region, src_l, dst_l, paint);
                            region.recycle();
                            region = null;
                        }
                    }
                    return;
                }

                int part_bottom = 0;
                if (region == null) {
                    /* TODO Create temporary region buffer using region_width and overlay height */
                    region_width = src_width;
                    region_height = src_height;
                    region = Bitmap.createBitmap(region_width, region_height, Bitmap.Config.ARGB_8888);
                    left = dst_x;
                    top = dst_y;
                    width = (dst_width == 0)?region_width:dst_width;
                    height = (dst_height == 0)?region_height:dst_height;
                    if (parserType == SUBTITLE_SUB_TYPE_TTX) {
                        width = 720;
                        height  =576;
                    }
                }
                else {
                    part_bottom = region_height;
                    region_height += src_height;
                }

                /* Build an array of ARGB_8888 pixels as signed ints and add this part to the region */
                Bitmap part = Bitmap.createBitmap(data, 0, src_width, src_width, src_height, Bitmap.Config.ARGB_8888);
                Canvas canvas = new Canvas(region);
                canvas.drawBitmap(part, 0, 0, null);
                part.recycle();
            }

            @Override
            public void pauseEx(int pause) {
                mPauseExDraw = pause;
            }

            @Override
            public void drawCC(boolean bshow, String json) {
                if (mHandler != null) {
                    Message msg = mHandler.obtainMessage();
                    msg.what = MSG_SUBTITLE_SHOW_CLOSED_CAPTION;
                    msg.obj  = json;
                    msg.arg1 = bshow == false ? 0 : 1;
                    mHandler.sendMessage(msg);
                }
            }

            private int eventToMsg(int event) {
                switch (event) {
                    case 0:
                        return MSG_SET_TELETEXT_MIX_NORMAL;
                    case 1:
                        return MSG_SET_TELETEXT_MIX_TRANSPARENT;
                    case 2:
                        return MSG_SET_TELETEXT_MIX_SEPARATE;
                }
                return MSG_SET_TELETEXT_MIX_NORMAL;
            }

            @Override
            public void mixVideoEvent(int event) {
                if (mHandler != null) {
                    Message msg = mHandler.obtainMessage();
                    msg.what = eventToMsg(event);
                    mHandler.sendMessage(msg);
                }

            }
        };

        public SubtitleServerView(Context context, Handler mainHandler) {
            super(context);
            mHandler = mainHandler;
            //comment it as pip don't need subtitle for the moment
            if (!getFeatureSupportFullPipFccArchitecture()) {
                DtvkitGlueClient.getInstance().setSubtileListener(mSubListener);
            }
        }

        public void setSize(int width, int height) {
            dst = new Rect(0, 0, width, height);
            postInvalidate();
        }

        public void setSize(int left, int top, int right, int bottom) {
            dst = new Rect(left, top, right, bottom);
            postInvalidate();
        }

        public void setTeletexTransparent(boolean mode) {
            mTtxTransparent = mode;
        }

        public void setOverlaySubtitleListener(DtvkitGlueClient.SubtitleListener listener) {
            DtvkitGlueClient.getInstance().setSubtileListener(listener);
        }

        @Override
        protected void onDraw(Canvas canvas)
        {
            super.onDraw(canvas);
            sem.acquireUninterruptibly();
            if (overlay_draw != null) {
                canvas.drawBitmap(overlay_draw, src, dst, null);
            }
            sem.release();
        }
    }

    class NativeOverlayView extends View
    {
        Bitmap overlay1 = null;
        Rect src, dst;

        Semaphore sem = new Semaphore(1);

        private final DtvkitGlueClient.OverlayTarget mTarget = new DtvkitGlueClient.OverlayTarget() {
            @Override
            public void draw(int src_width, int src_height, int dst_x, int dst_y, int dst_width, int dst_height, int[] data) {
                if (overlay1 == null) {
                    /* TODO The overlay size should come from the tif (and be updated on onOverlayViewSizeChanged) */
                    overlay1 = Bitmap.createBitmap(1920, 1080, Bitmap.Config.ARGB_8888);
                    /* Clear the overlay that will be drawn initially */
                    Canvas canvas = new Canvas(overlay1);
                    canvas.drawColor(0, PorterDuff.Mode.CLEAR);
                }

                /* TODO Temporary private usage of API. Add explicit methods if keeping this mechanism */
                if (src_width == 0 || src_height == 0) {
                    if (dst_width == 9999) {
                        /* 9999 dst_width indicates the overlay should be cleared */
                        sem.acquireUninterruptibly();
                        Canvas canvas = new Canvas(overlay1);
                        canvas.drawColor(0, PorterDuff.Mode.CLEAR);
                        sem.release();
                    }
                } else {
                    if (data.length == 0)
                        return;
                    /* Build an array of ARGB_8888 pixels as signed ints and add this part to the region */
                    Rect overlay_dst = new Rect(0, 0, overlay1.getWidth(), overlay1.getHeight());
                    Bitmap region = Bitmap.createBitmap(data, 0, src_width, src_width, src_height, Bitmap.Config.ARGB_8888);
                    if ((dst_width == 0)
                        || (dst_width == 0)
                        || (dst_width < (src_width + dst_x))
                        || (dst_height < (src_height + dst_y))) {
                        overlay_dst.left = dst_x;
                        overlay_dst.top = dst_y;
                        overlay_dst.right = overlay1.getWidth() - overlay_dst.left;
                        overlay_dst.bottom = overlay1.getHeight() - overlay_dst.top;
                    } else {
                        if (dst_x > 0) {
                            float scaleX = (float)(overlay1.getWidth())/(float)(dst_width);
                            overlay_dst.left = (int)(scaleX*dst_x);
                            overlay_dst.right = (int)(scaleX*(src_width + dst_x));
                        }
                        if (dst_y > 0) {
                            float scaleY = (float)(overlay1.getHeight())/(float)(dst_height);
                            overlay_dst.top = (int)(scaleY*dst_y);
                            overlay_dst.bottom = (int)(scaleY*(src_height + dst_y));
                        }
                    }
                    sem.acquireUninterruptibly();
                    Canvas canvas = new Canvas(overlay1);
                    canvas.drawColor(0, PorterDuff.Mode.CLEAR);
                    canvas.drawBitmap(region, null, overlay_dst, null);
                    sem.release();
                    region.recycle();
                    postInvalidate();
                }
            }
        };

        public NativeOverlayView(Context context) {
            super(context);
            //comment it as pip don't need subtitle for the moment
            if (!getFeatureSupportFullPipFccArchitecture()) {
                DtvkitGlueClient.getInstance().setOverlayTarget(mTarget);
            }
        }

        public void setSize(int width, int height) {
            dst = new Rect(0, 0, width, height);
        }


        public void setSize(int left, int top, int right, int bottom) {
            dst = new Rect(left, top, right, bottom);
        }

        public void setOverlayTarge(DtvkitGlueClient.OverlayTarget target) {
            DtvkitGlueClient.getInstance().setOverlayTarget(target);
        }

        @Override
        protected void onDraw(Canvas canvas)
        {
            super.onDraw(canvas);
            sem.acquireUninterruptibly();
            if (overlay1 != null) {
                canvas.drawBitmap(overlay1, src, dst, null);
            }
            sem.release();
        }
    }

    // We do not indicate recording capabilities. TODO for recording.
    @RequiresApi(api = Build.VERSION_CODES.N)
    public TvInputService.RecordingSession onCreateRecordingSession(String inputId)
    {
        Log.i(TAG, "onCreateRecordingSession");
        initDtvkitTvInput();
        mDtvkitRecordingSessionCount++;
        return new DtvkitRecordingSession(this, inputId);
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    class DtvkitRecordingSession extends TvInputService.RecordingSession {
        private static final String TAG = "DtvkitRecordingSession";
        private Uri mChannel = null;
        private Uri mProgram = null;
        private Context mContext = null;
        private String mInputId = null;
        private long startRecordTimeMillis = 0;
        private long endRecordTimeMillis = 0;
        private long startRecordSystemTimeMillis = 0;
        private long endRecordSystemTimeMillis = 0;
        private String recordingUri = null;
        private Uri mRecordedProgramUri = null;
        private boolean tunedNotified = false;
        private boolean mTuned = false;
        private boolean mStarted = false;
        private int mPath = -1;
        protected HandlerThread mRecordingHandlerThread = null;
        private Handler mRecordingProcessHandler = null;
        private long mCurrentRecordIndex = 0;
        private boolean mRecordStopAndSaveReceived = false;
        private boolean mIsNonAvProgram = false;

        protected static final int MSG_RECORD_ONTUNE = 0;
        protected static final int MSG_RECORD_ONSTART = 1;
        protected static final int MSG_RECORD_ONSTOP = 2;
        protected static final int MSG_RECORD_UPDATE_RECORDING = 3;
        protected static final int MSG_RECORD_ONRELEASE = 4;
        protected static final int MSG_RECORD_DO_FINALRELEASE = 5;
        private final String[] MSG_STRING = {"RECORD_ONTUNE", "RECORD_ONSTART", "RECORD_ONSTOP", "RECORD_UPDATE_RECORDING", "RECORD_ONRELEASE", "DO_FINALRELEASE"};

        protected static final int MSG_RECORD_UPDATE_RECORD_PERIOD = 3000;

        @RequiresApi(api = Build.VERSION_CODES.N)
        public DtvkitRecordingSession(Context context, String inputId) {
            super(context);
            mContext = context;
            mInputId = inputId;
            mCurrentRecordIndex = mDtvkitRecordingSessionCount;
            if (numRecorders == 0) {
                updateRecorderNumber();
            }
            updateTunerNumber();
            initRecordWorkThread();
            Log.i(TAG, "DtvkitRecordingSession");
        }

        protected void initRecordWorkThread() {
            mRecordingHandlerThread = new HandlerThread("DtvkitRecordingSession " + mCurrentRecordIndex);
            mRecordingHandlerThread.start();
            mRecordingProcessHandler = new Handler(mRecordingHandlerThread.getLooper(), new Handler.Callback() {
                @Override
                public boolean handleMessage(Message msg) {
                    if (msg.what >= 0 && MSG_STRING.length > msg.what) {
                        Log.d(TAG, "handleMessage " + MSG_STRING[msg.what] + " start, index = " + mCurrentRecordIndex);
                    } else {
                        Log.d(TAG, "handleMessage " + msg.what + " start, index = " + mCurrentRecordIndex);
                    }
                    switch (msg.what) {
                        case MSG_RECORD_ONTUNE:{
                            Uri uri = (Uri)msg.obj;
                            doTune(uri);
                            break;
                        }
                        case MSG_RECORD_ONSTART:{
                            Uri uri = (Uri)msg.obj;
                            doStartRecording(uri);
                            break;
                        }
                        case MSG_RECORD_ONSTOP:{
                            doStopRecording();
                            break;
                        }
                        case MSG_RECORD_UPDATE_RECORDING:{
                            boolean insert = (msg.arg1 == 1);
                            boolean check = (msg.arg2 == 1);
                            updateRecordingToDb(insert, check, msg.obj);
                            break;
                        }
                        case MSG_RECORD_ONRELEASE:{
                            doRelease();
                            break;
                        }
                        case MSG_RECORD_DO_FINALRELEASE:{
                            doFinalReleaseByThread();
                            break;
                        }
                        default:
                            break;
                    }
                    if (msg.what >= 0 && MSG_STRING.length > msg.what) {
                        Log.d(TAG, "handleMessage " + MSG_STRING[msg.what] + " over , index = " + mCurrentRecordIndex);
                    } else {
                        Log.d(TAG, "handleMessage " + msg.what + " over , index = " + mCurrentRecordIndex);
                    }
                    return true;
                }
            });
        }

        @Override
        public void onTune(Uri uri) {
            if (mRecordingProcessHandler != null) {
                mRecordingProcessHandler.removeMessages(MSG_RECORD_ONTUNE);
                Message mess = mRecordingProcessHandler.obtainMessage(MSG_RECORD_ONTUNE, 0, 0, uri);
                boolean result = mRecordingProcessHandler.sendMessage(mess);
                Log.d(TAG, "onTune sendMessage result " + result + ", index = " + mCurrentRecordIndex);
            } else {
                Log.i(TAG, "onTune null mRecordingProcessHandler" + ", index = " + mCurrentRecordIndex);
            }
        }

        @Override
        public void onStartRecording(@Nullable Uri uri) {
            if (mRecordingProcessHandler != null) {
                mRecordingProcessHandler.removeMessages(MSG_RECORD_ONSTART);
                Message mess = mRecordingProcessHandler.obtainMessage(MSG_RECORD_ONSTART, 0, 0, uri);
                boolean result = mRecordingProcessHandler.sendMessage(mess);
                Log.d(TAG, "onStartRecording sendMessage result " + result + ", index = " + mCurrentRecordIndex);
            } else {
                Log.i(TAG, "onStartRecording null mRecordingProcessHandler" + ", index = " + mCurrentRecordIndex);
            }
        }

        @Override
        public void onStopRecording() {
            if (mRecordingProcessHandler != null) {
                mRecordingProcessHandler.removeMessages(MSG_RECORD_ONSTOP);
                boolean result = mRecordingProcessHandler.sendEmptyMessage(MSG_RECORD_ONSTOP);
                Log.d(TAG, "onStopRecording sendMessage result " + result + ", index = " + mCurrentRecordIndex);
            } else {
                Log.i(TAG, "onStopRecording null mRecordingProcessHandler" + ", index = " + mCurrentRecordIndex);
            }
        }

        @Override
        public void onRelease() {
            try {
            if (mRecordingProcessHandler != null) {
                mRecordingProcessHandler.removeMessages(MSG_RECORD_ONRELEASE);
                boolean result = mRecordingProcessHandler.sendEmptyMessage(MSG_RECORD_ONRELEASE);
                Log.d(TAG, "onRelease sendMessage result " + result + ", index = " + mCurrentRecordIndex);
            } else {
                Log.i(TAG, "onRelease null mRecordingProcessHandler" + ", index = " + mCurrentRecordIndex);
            }
            } catch (Exception e) {
                Log.d(TAG, "released already");
            }
        }

        public void doTune(Uri uri) {
            Log.i(TAG, "doTune for recording " + uri);
            if (ContentUris.parseId(uri) == -1) {
                Log.e(TAG, "DtvkitRecordingSession doTune invalid uri = " + uri);
                notifyError(TvInputManager.RECORDING_ERROR_UNKNOWN);
                return;
            }

            tunedNotified = false;
            mTuned = false;
            mStarted = false;

            DtvkitTvInputSession session = getMainTunerSession();
            boolean seiDesign = getFeatureSupportNewTvApp();
            if (session != null) {
                session.removeScheduleTimeshiftRecordingTask();
                //need to reset record path as path may be set suring playing
                if (getFeatureSupportNewTvApp()) {
                    resetRecordingPath();
                }
            } else {
                resetRecordingPath();
            }
            numActiveRecordings = recordingGetNumActiveRecordings();
            Log.i(TAG, "numActiveRecordings: " + numActiveRecordings);

            /*want of resource*/
            if (numActiveRecordings >= numRecorders
                || numActiveRecordings >= getNumRecordersLimit()) {
                if (getFeatureSupportTimeshifting()
                        && timeshiftRecorderState != RecorderState.STOPPED
                        && getFeatureTimeshiftingPriorityHigh()) {
                    Log.i(TAG, "No recording path available, no recorder");
                    Bundle event = new Bundle();
                    event.putString(ConstantManager.KEY_INFO, "No recording path available, no recorder");
                    notifySessionEvent(ConstantManager.EVENT_RESOURCE_BUSY, event);
                    notifyError(TvInputManager.RECORDING_ERROR_RESOURCE_BUSY);
                    return;
                } else {
                    recordingPending = true;

                    boolean returnToLive = timeshifting;
                    Log.i(TAG, "stopping timeshift [return live:"+returnToLive+"]");
                    timeshiftRecorderState = RecorderState.STOPPED;
                    scheduleTimeshiftRecording = false;
                    timeshifting = false;
                    playerStopTimeshiftRecording(returnToLive);
                }
            }

            boolean available = false;
            boolean tunerUsing = mParameterMananer.isTunerInputConflictwithDtvKit();
            if (tunerUsing) {
                Log.i(TAG, "doTune Stop record due to tuner resource busy");
                Bundle event = new Bundle();
                event.putString(ConstantManager.KEY_INFO, "Stop record due to tuner resource busy");
                notifySessionEvent(ConstantManager.EVENT_RESOURCE_BUSY, event);
                notifyError(TvInputManager.RECORDING_ERROR_RESOURCE_BUSY);
                return;
            }

            mChannel = uri;
            Channel channel = getChannel(uri);
            if (recordingCheckAvailability(getChannelInternalDvbUri(channel))) {
                Log.i(TAG, "recording path available");
                StringBuffer tuneResponse = new StringBuffer();

                DtvkitGlueClient.getInstance().registerSignalHandler(mRecordingHandler);

                JSONObject tuneStat = recordingTune(getChannelInternalDvbUri(channel), tuneResponse);
                if (tuneStat != null) {
                    mTuned = getRecordingTuned(tuneStat);
                    mPath = getRecordingTunePath(tuneStat);
                    if (mTuned) {
                        Log.i(TAG, "recording tuned ok.");
                        notifyTuned(uri);
                        tunedNotified = true;
                    } else {
                        Log.i(TAG, "recording (path:" + mPath + ") tunning...");
                    }
                    available = true;
                } else {
                    if (tuneResponse.toString().equals("Failed to get a tuner to record")) {
                        Log.i(TAG, "record error no tuner to record");
                    }
                    else if (tuneResponse.toString().equals("Invalid resource")) {
                        Log.i(TAG, "record error invalid channel");
                    }
                }
            }

            if (!available) {
                Log.i(TAG, "No recording path available, no tuner/demux");
                Bundle event = new Bundle();
                event.putString(ConstantManager.KEY_INFO, "No recording path available, no tuner/demux");
                notifySessionEvent(ConstantManager.EVENT_RESOURCE_BUSY, event);
                notifyError(TvInputManager.RECORDING_ERROR_RESOURCE_BUSY);
            }
        }

        private void doStartRecording(@Nullable Uri uri) {
            Log.i(TAG, "doStartRecording " + uri);
            mProgram = uri;

            String dvbUri;
            long durationSecs = 0;
            Program program = getProgram(uri);
            if (program != null) {
                startRecordTimeMillis = program.getStartTimeUtcMillis();
                startRecordSystemTimeMillis = System.currentTimeMillis();
                dvbUri = getProgramInternalDvbUri(program);
            } else {
                startRecordTimeMillis = PropSettingManager.getCurrentStreamTime(true);
                startRecordSystemTimeMillis = System.currentTimeMillis();
                dvbUri = getChannelInternalDvbUri(getChannel(mChannel));
                durationSecs = 3 * 60 * 60; // 3 hours is maximum recording duration for Android
            }
            StringBuffer recordingResponse = new StringBuffer();
            Log.i(TAG, "startRecording path:" + mPath);
            if (!recordingStartRecording(dvbUri, mPath, recordingResponse)) {
                if (recordingResponse.toString().equals("May not be enough space on disk")) {
                    Log.i(TAG, "record error insufficient space");
                    notifyError(TvInputManager.RECORDING_ERROR_INSUFFICIENT_SPACE);
                }
                else if (recordingResponse.toString().equals("Limited by minimum free disk space")) {
                    Log.i(TAG, "record error min free limited");
                    notifyError(TvInputManager.RECORDING_ERROR_INSUFFICIENT_SPACE);
                }
                else {
                    Log.i(TAG, "record error unknown");
                    notifyError(TvInputManager.RECORDING_ERROR_UNKNOWN);
                }
            }
            else
            {
                mStarted = true;
                recordingUri = recordingResponse.toString();
                Log.i(TAG, "Recording started:"+recordingUri);
                updateRecordProgramInfo(recordingUri);
            }
            recordingPending = false;
        }

        private void doStopRecording() {
            Log.i(TAG, "doStopRecording");

            DtvkitGlueClient.getInstance().unregisterSignalHandler(mRecordingHandler);
            //update record status firstly to get accurate duration time
            if (mRecordingProcessHandler != null) {
                mRecordingProcessHandler.removeMessages(MSG_RECORD_UPDATE_RECORDING);
                boolean result = mRecordingProcessHandler.sendMessage(mRecordingProcessHandler.obtainMessage(MSG_RECORD_UPDATE_RECORDING, 0, 0));
                Log.d(TAG, "doStopRecording sendMessage MSG_RECORD_UPDATE_RECORDING = " + result + ", index = " + mCurrentRecordIndex);
                if (result) {
                    mRecordStopAndSaveReceived = true;
                }
            } else {
                Log.i(TAG, "doStopRecording sendMessage MSG_RECORD_UPDATE_RECORDING null, index = " + mCurrentRecordIndex);
            }
            if (!recordingStopRecording(recordingUri)) {
                notifyError(TvInputManager.RECORDING_ERROR_UNKNOWN);
            } else {
                Log.i(TAG, "doStopRecording succeed, index = " + mCurrentRecordIndex);
            }
            mStarted = false;
            recordingPending = false;

            /*if there's a live play,
              should lock here, may run into a race condition*/
            if (!getFeatureSupportManualTimeshift()) {
                DtvkitTvInputSession session = getMainTunerSession();
                if ((session != null && session.mTunedChannel != null &&
                         session.mHandlerThreadHandle != null)) {
                   session.sendMsgTryStartTimeshift(500);
                }
            }
        }

        private void updateRecordingToDb(boolean insert, boolean check, Object obj) {
            endRecordTimeMillis = PropSettingManager.getCurrentStreamTime(true);
            endRecordSystemTimeMillis = System.currentTimeMillis();
            scheduleTimeshiftRecording = true;
            Log.d(TAG, "updateRecordingToDb:"+recordingUri);
            if (recordingUri != null)
            {
                long recordingDurationMillis = endRecordSystemTimeMillis - startRecordSystemTimeMillis;
                long floorMs = recordingDurationMillis % 1000;
                Log.d(TAG, "updateRecordingToDb recordingDurationMillis:" + recordingDurationMillis);
                //keep the nearest second
                if (floorMs >= 500) {
                    recordingDurationMillis = recordingDurationMillis + 1000 - floorMs;
                } else {
                    recordingDurationMillis = recordingDurationMillis - floorMs;
                }
                RecordedProgram.Builder builder = null;
                InternalProviderData data = null;
                Program program = getProgram(mProgram);
                Channel channel = getChannel(mChannel);
                if (program == null) {
                    program = getCurrentStreamProgram(mChannel, PropSettingManager.getCurrentStreamTime(true));
                }
                if (insert) {
                    if (program == null) {
                        long id = -1;
                        if (channel != null) {
                            id = channel.getId();
                        }
                        builder = new RecordedProgram.Builder()
                                .setChannelId(id)
                                .setTitle(channel != null ? channel.getDisplayName() : null)
                                .setStartTimeUtcMillis(startRecordTimeMillis)
                                .setEndTimeUtcMillis(startRecordTimeMillis + recordingDurationMillis/*endRecordTimeMillis*/);//stream card may playback
                    } else {
                        builder = new RecordedProgram.Builder(program);
                    }
                    data = new InternalProviderData();
                    String currentPath = mDataMananer.getStringParameters(DataMananer.KEY_PVR_RECORD_PATH);
                    int pathExist = 0;
                    if (mSysSettingManager.isDeviceExist(currentPath)) {
                        pathExist = 1;
                    }
                    if (SysSettingManager.isMediaPath(currentPath)) {
                        currentPath = SysSettingManager.convertMediaPathToMountedPath(currentPath);
                    }
                    try {
                        data.put(RecordedProgram.RECORD_FILE_PATH, currentPath);
                        data.put(RecordedProgram.RECORD_STORAGE_EXIST, pathExist);
                        if (channel.getServiceType().equals(TvContract.Channels.SERVICE_TYPE_AUDIO)) {
                            data.put(RecordedProgram.RECORD_SERVICE_TYPE, "audio_only");
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "updateRecordingToDb update InternalProviderData Exception = " + e.getMessage());
                    }
                }
                if (insert) {
                    Log.i(TAG, "updateRecordingToDb insert");
                    RecordedProgram recording = builder.setInputId(mInputId)
                            .setRecordingDataUri(recordingUri)
                            .setRecordingDataBytes(1024 * 1024l)
                            .setRecordingDurationMillis(recordingDurationMillis > 0 ? recordingDurationMillis : -1)
                            .setInternalProviderData(data)
                            .build();
                    mRecordedProgramUri = mContext.getContentResolver().insert(TvContract.RecordedPrograms.CONTENT_URI,
                        recording.toContentValues());
                    Bundle event = new Bundle();
                    event.putString(ConstantManager.EVENT_RECORD_DATA_URI, recordingUri != null ? recordingUri : null);
                    event.putString(ConstantManager.EVENT_RECORD_PROGRAM_URI, mRecordedProgramUri != null ? mRecordedProgramUri.toString() : null);
                    notifySessionEvent(ConstantManager.EVENT_RECORD_PROGRAM_URI, event);
                } else {
                    Log.i(TAG, "updateRecordingToDb update");
                    if (mRecordedProgramUri != null) {
                        ContentValues values = new ContentValues();
                        if (endRecordTimeMillis > -1) {
                            values.put(TvContract.RecordedPrograms.COLUMN_END_TIME_UTC_MILLIS, startRecordTimeMillis + recordingDurationMillis/*endRecordTimeMillis*/);//stream card may playback
                        }
                        if (recordingDurationMillis > 0) {
                            values.put(TvContract.RecordedPrograms.COLUMN_RECORDING_DURATION_MILLIS, recordingDurationMillis);
                        }
                        TvContentRating[] oldContentRatings = null;
                        TvContentRating[] currentContentRatings = null;
                        TvContentRating[] newContentRatings = null;
                        if (obj != null && obj instanceof RecordedProgram) {
                            oldContentRatings = ((RecordedProgram)obj).getContentRatings();
                        }
                        if (program != null) {
                            currentContentRatings = program.getContentRatings();
                        }
                        if (currentContentRatings != null) {
                            newContentRatings = updateRecordProgramRatings(oldContentRatings, currentContentRatings);
                        }
                        if (newContentRatings != null) {
                            values.put(TvContract.RecordedPrograms.COLUMN_CONTENT_RATING, TvContractUtils.contentRatingsToString(newContentRatings));
                        }
                        mContext.getContentResolver().update(mRecordedProgramUri,
                                values, null, null);
                    } else {
                        Log.i(TAG, "updateRecordingToDb update mRecordedProgramUri null");
                    }
                }

                //recordingUri = null;
                if (!check) {
                    boolean delete = false;
                    if (obj != null && obj instanceof Bundle) {
                        delete = ((Bundle)obj).getBoolean("delete", false);
                    }
                    if (delete) {
                        Log.d(TAG, "updateRecordingToDb delete" + " index = " + mCurrentRecordIndex);
                        recordingStopRecording(recordingUri);
                        recordingRemoveRecording(recordingUri);
                        if (mRecordedProgramUri != null)
                            mContext.getContentResolver().delete(mRecordedProgramUri, null, null);
                        if (mRecordingProcessHandler != null)
                            mRecordingProcessHandler.removeMessages(MSG_RECORD_UPDATE_RECORDING);
                    } else {
                        Log.d(TAG, "updateRecordingToDb notifystop mRecordedProgramUri = " + mRecordedProgramUri + " index = " + mCurrentRecordIndex);
                        notifyRecordingStopped(mRecordedProgramUri);
                        if (mRecordStopAndSaveReceived || mStarted) {
                            if (mRecordingProcessHandler != null) {
                                mRecordingProcessHandler.removeMessages(MSG_RECORD_UPDATE_RECORDING);
                                boolean result = mRecordingProcessHandler.sendEmptyMessage(MSG_RECORD_DO_FINALRELEASE);
                                Log.d(TAG, "updateRecordingToDb sendMessage result = " + result + ", index = " + mCurrentRecordIndex);
                            } else {
                                Log.i(TAG, "updateRecordingToDb sendMessage null mRecordingProcessHandler" + ", index = " + mCurrentRecordIndex);
                            }
                        }
                    }
                } else {
                    if (mRecordingProcessHandler != null) {
                        boolean result = mRecordingProcessHandler.sendMessageDelayed(mRecordingProcessHandler.obtainMessage(MSG_RECORD_UPDATE_RECORDING, 0, 1), MSG_RECORD_UPDATE_RECORD_PERIOD);
                        Log.d(TAG, "updateRecordingToDb continue sendMessage MSG_RECORD_UPDATE_RECORDING = " + result + ", index = " + mCurrentRecordIndex);
                    } else {
                        Log.i(TAG, "updateRecordingToDb sendMessage MSG_RECORD_UPDATE_RECORDING null, index = " + mCurrentRecordIndex);
                    }
                }
            }
            //PropSettingManager.resetRecordFrequencyFlag();
        }

        private void doRelease() {
            Log.i(TAG, "doRelease");

            recordingPending = false;

            String uri = "";
            if (mProgram != null) {
                uri = getProgramInternalDvbUri(getProgram(mProgram));
            } else if (mChannel != null) {
                uri = getChannelInternalDvbUri(getChannel(mChannel)) + ";0000";
            } else {
                return;
            }

            DtvkitGlueClient.getInstance().unregisterSignalHandler(mRecordingHandler);
            if ((mStarted && !mRecordStopAndSaveReceived) || mIsNonAvProgram) {
                if (mRecordingProcessHandler != null) {
                    Bundle b = null;
                    if (mIsNonAvProgram) {
                        b = new Bundle();
                        b.putBoolean("delete", true);
                        Log.d(TAG, "doRelease none Av Program");
                    }
                    //need update and stop record directly
                    updateRecordingToDb(false, false, b);
                    mRecordingProcessHandler.removeMessages(MSG_RECORD_UPDATE_RECORDING);
                    mRecordingProcessHandler.removeMessages(MSG_RECORD_DO_FINALRELEASE);
                    boolean result = mRecordingProcessHandler.sendEmptyMessage(MSG_RECORD_DO_FINALRELEASE);
                    Log.d(TAG, "doRelease sendMessage result = " + result + ", index = " + mCurrentRecordIndex);
                } else {
                    Log.i(TAG, "doRelease sendMessage null mRecordingProcessHandler" + ", index = " + mCurrentRecordIndex);
                }
            }
            if (!mStarted && mTuned) {
                recordingUntune(mPath);
                return;
            }

            JSONArray scheduledRecordings = recordingGetListOfScheduledRecordings();
            if (scheduledRecordings != null) {
                for (int i = 0; i < scheduledRecordings.length(); i++) {
                    try {
                        if (getScheduledRecordingUri(scheduledRecordings.getJSONObject(i)).equals(uri)) {
                            Log.i(TAG, "removing recording uri from schedule: " + uri);
                            recordingRemoveScheduledRecording(uri);
                            break;
                        }
                    } catch (JSONException e) {
                        Log.e(TAG, e.getMessage());
                    }
                }
            }
        }

        private void doFinalReleaseByThread() {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    Log.d(TAG, "doFinalReleaseByThread start index = " + mCurrentRecordIndex);
                    if (mRecordingHandlerThread != null) {
                        mRecordingHandlerThread.getLooper().quitSafely();
                        mRecordingHandlerThread = null;
                    }
                    if (mRecordingProcessHandler != null) {
                        mRecordingProcessHandler.removeCallbacksAndMessages(null);
                    }
                    mRecordingProcessHandler = null;
                    Log.d(TAG, "doFinalReleaseByThread end  index = " + mCurrentRecordIndex);
                }
            }).start();
        }

        private Program getProgram(Uri uri) {
            Program program = null;
            Cursor cursor = null;
            try {
                if (uri != null) {
                    cursor = mContext.getContentResolver().query(uri, Program.PROJECTION, null, null, null);
                }
                while (null != cursor && cursor.moveToNext()) {
                    program = Program.fromCursor(cursor);
                }
            } catch (Exception e) {
                Log.e(TAG, "getProgram query Failed = " + e.getMessage());
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
            return program;
        }

        private Channel getChannel(Uri uri) {
            Channel channel = null;
            Cursor cursor = null;
            try {
                if (uri != null) {
                    cursor = mContext.getContentResolver().query(uri, Channel.PROJECTION, null, null, null);
                }
                while (null != cursor && cursor.moveToNext()) {
                    channel = Channel.fromCursor(cursor);
                }
            } catch (Exception e) {
                Log.e(TAG, "getChannel query Failed = " + e.getMessage());
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
            return channel;
        }

        private void updateRecordProgramInfo(String recordDataUri) {
            RecordedProgram searchedRecordedProgram = getRecordProgram(recordDataUri);
            if (mRecordingProcessHandler != null) {
                mRecordingProcessHandler.removeMessages(MSG_RECORD_UPDATE_RECORDING);
                int arg1 = 0;
                int arg2 = 0;
                if (searchedRecordedProgram != null) {
                    Log.d(TAG, "updateRecordProgramInfo update " + recordDataUri);
                    arg1 = 0;
                    arg2 = 1;
                } else {
                    Log.d(TAG, "updateRecordProgramInfo insert " + recordDataUri);
                    arg1 = 1;
                    arg2 = 1;
                }
                boolean result = mRecordingProcessHandler.sendMessage(mRecordingProcessHandler.obtainMessage(MSG_RECORD_UPDATE_RECORDING, arg1, arg2, searchedRecordedProgram));
                Log.d(TAG, "updateRecordProgramInfo sendMessage MSG_RECORD_UPDATE_RECORDING = " + result + ", index = " + mCurrentRecordIndex);
            } else {
                Log.i(TAG, "updateRecordProgramInfo sendMessage MSG_RECORD_UPDATE_RECORDING null, index = " + mCurrentRecordIndex);
            }
        }

        private RecordedProgram getRecordProgram(String recordDataUri) {
            RecordedProgram result = null;
            Cursor cursor = null;
            try {
                if (recordDataUri != null) {
                    cursor = mContext.getContentResolver().query(TvContract.RecordedPrograms.CONTENT_URI, RecordedProgram.PROJECTION,
                        TvContract.RecordedPrograms.COLUMN_RECORDING_DATA_URI + "=?", new String[]{recordDataUri}, null);
                }
                while (null != cursor && cursor.moveToNext()) {
                    result = RecordedProgram.fromCursor(cursor);
                    break;
                }
            } catch (Exception e) {
                Log.e(TAG, "getRecordProgram query Failed = " + e.getMessage());
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
            return result;
        }

        private TvContentRating[] updateRecordProgramRatings(TvContentRating[] oldRatings, TvContentRating[] newRatings) {
            TvContentRating[] result = null;
            List<TvContentRating> allRatings = null;
            if (newRatings != null && newRatings.length > 0) {
                if (oldRatings == null || oldRatings.length == 0) {
                    result = newRatings;
                } else {
                    allRatings = new ArrayList(Arrays.asList(oldRatings));
                    for (TvContentRating tempNew : newRatings) {
                        if (!containsRating(oldRatings, tempNew)) {
                            allRatings.add(tempNew);
                        }
                    }
                    result = (TvContentRating[])(allRatings.toArray());
                }
            }
            return result;
        }

        private boolean containsRating(TvContentRating[] ratings, TvContentRating rating) {
            boolean result = false;
            if (ratings != null && ratings.length > 0 && rating != null) {
                for (TvContentRating temp : ratings) {
                    if (rating.equals(temp)) {
                        result = true;
                        break;
                    }
                }
            }
            return result;
        }

        private Program getCurrentProgram(Uri channelUri) {
            return TvContractUtils.getCurrentProgram(mContext.getContentResolver(), channelUri);
        }

        private Program getCurrentStreamProgram(Uri channelUri, long streamTime) {
            return TvContractUtils.getCurrentProgramExt(mContext.getContentResolver(), channelUri, streamTime);
        }

        private void stopRecording(boolean delete) {
            Bundle b = null;

            if (delete) {
                b = new Bundle();
                b.putBoolean("delete", true);
            }
            if (mRecordingProcessHandler != null) {
                mRecordingProcessHandler.removeMessages(MSG_RECORD_UPDATE_RECORDING);
                boolean result = mRecordingProcessHandler.sendMessage(mRecordingProcessHandler.obtainMessage(MSG_RECORD_UPDATE_RECORDING, 0, 0, b));
                Log.d(TAG, "stopRecording sendMessage MSG_RECORD_UPDATE_RECORDING = " + result + ", index = " + mCurrentRecordIndex);
                if (result) {
                    mRecordStopAndSaveReceived = true;
                }
            } else {
                Log.i(TAG, "stopRecording sendMessage MSG_RECORD_UPDATE_RECORDING null, index = " + mCurrentRecordIndex);
            }
        }

        private final DtvkitGlueClient.SignalHandler mRecordingHandler = new DtvkitGlueClient.SignalHandler() {
            @RequiresApi(api = Build.VERSION_CODES.M)
            @Override
            public void onSignal(String signal, JSONObject data) {
                Log.i(TAG, "recording onSignal: " + signal + " : " + data.toString());
                if (signal.equals("TuneStatusChanged")) {
                    String state = "fail";
                    int path = -1;
                    try {
                        state = data.getString("state");
                        path = data.getInt("path");
                    } catch (JSONException ignore) {
                    }
                    Log.i(TAG, "tune to: "+ path + ", " + state);
                    if (path != mPath)
                        return;

                    switch (state) {
                        case "ok":
                            if (!tunedNotified)
                                notifyTuned(mChannel);
                        break;
                    }
                } else if (signal.equals("RecordingStatusChanged")) {
                    if (!recordingIsRecordingPathActive(data, mPath)) {
                        Log.d(TAG, "RecordingStatusChanged, stopped[path:"+mPath+"]");
                        stopRecording(false);
                    }
                    else if (checkActiveRecordings(recordingGetActiveRecordings(data),
                        /*check the record_av_only rule*/
                        new Predicate<JSONObject> () {
                            @Override
                            public boolean test(JSONObject recording) {
                                if (TextUtils.equals(recording.optString("serviceuri", "null"), getChannelInternalDvbUri(getChannel(mChannel)).toString())
                                    && getFeatureSupportRecordAVServiceOnly()
                                    && !recording.optBoolean("is_av", true))
                                {
                                    return true;
                                }
                                return false;
                            }
                        }))
                    {
                        /*stop the recording*/
                        Log.i(TAG, "recording stopped due to non-AV service, feature rec_av_only enabled");
                        //stop recording in release by flag mIsNonAvProgram as record may be starting when receive this message
                        mIsNonAvProgram = true;
                        //stopRecording(true);

                        Bundle event = new Bundle();
                        event.putString(ConstantManager.KEY_INFO, "not allowed to record a non-av service");
                        notifySessionEvent(ConstantManager.EVENT_RESOURCE_BUSY, event);
                        notifyError(TvInputManager.RECORDING_ERROR_RESOURCE_BUSY);
                    }
                } else if (signal.equals("RecordingDiskFull")) {
                    //tell application to deal current running pvr
                    notifyError(TvInputManager.RECORDING_ERROR_INSUFFICIENT_SPACE);
                } else if (signal.equals("RecordingsDiskRm")) {
                    //tell application current pvr disk remove now
                    Bundle event = new Bundle();
                    event.putString(ConstantManager.KEY_INFO, "Stop record due to disk remove");
                    notifySessionEvent(ConstantManager.EVENT_RESOURCE_BUSY, event);
                } else if (signal.equals("TuneResourceBusy")) {
                    //tell application TuneResourceBusy and need to stop
                    Bundle event = new Bundle();
                    event.putString(ConstantManager.KEY_INFO, "Stop record due to tuner resource busy");
                    notifySessionEvent(ConstantManager.EVENT_RESOURCE_BUSY, event);
                    doStopRecording();
                }
            }
        };
    }

    class DtvkitTvInputSession extends TvInputService.Session               {
        private static final String TAG = "DtvkitTvInputSession";
        private boolean mhegTookKey = false;
        private Channel mPreviousTunedChannel = null;
        private Channel mTunedChannel = null;
        private List<TvTrackInfo> mTunedTracks = null;
        protected final Context mContext;
        private RecordedProgram recordedProgram = null;
        private long originalStartPosition = TvInputManager.TIME_SHIFT_INVALID_TIME;
        private long startPosition = TvInputManager.TIME_SHIFT_INVALID_TIME;
        private long currentPosition = TvInputManager.TIME_SHIFT_INVALID_TIME;
        private float playSpeed = 0;
        private PlayerState playerState = PlayerState.STOPPED;
        private AvailableState timeshiftAvailable = new AvailableState();
        private int timeshiftBufferSizeMins = 60;
        private int timeshiftBufferSizeMBs = 0;
        DtvkitOverlayView mView = null;
        private long mCurrentDtvkitTvInputSessionIndex = 0;
        protected HandlerThread mLivingHandlerThread = null;
        protected Handler mHandlerThreadHandle = null;
        protected Handler mMainHandle = null;
        private boolean mIsMain = false;
        private final CaptioningManager mCaptioningManager;
        private boolean mKeyUnlocked = false;
        private boolean mBlocked = false;
        private int mSignalStrength = 0;
        private int mSignalQuality = 0;
        private int m_surface_left = 0;
        private int m_surface_right = 0;
        private int m_surface_top = 0;
        private int m_surface_bottom = 0;
        private boolean mTeleTextMixNormal = true;
        private int dvrSubtitleFlag = 0;
        private MountEventReceiver mMediaReceiver;

        private boolean mTimeShiftInited = false;
        private boolean mTuned = false;
        private boolean mReleased = false;
        private boolean mIsPip = false;
        private Uri mPreviousBufferUri = null;
        private Uri mNextBufferUri = null;
        private Surface mSurface;
        private boolean mSurfaceSent = false;
        private int mWinWidth = 0;
        private int mWinHeight = 0;
        private boolean mReleaseHandleMessage = false;
        private boolean mAquireMainSemaphore = false;
        private boolean mAquirePipSemaphore = false;

        DtvkitTvInputSession(Context context) {
            super(context);
            mContext = context;
            mCurrentDtvkitTvInputSessionIndex = mDtvkitTvInputSessionCount;
            Log.i(TAG, "DtvkitTvInputSession creat");
            //setOverlayViewEnabled(true);
            mCaptioningManager =
                (CaptioningManager) context.getSystemService(Context.CAPTIONING_SERVICE);
            initWorkThread();
            mMediaReceiver = new MountEventReceiver();
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction(Intent.ACTION_MEDIA_REMOVED);
            intentFilter.addAction(Intent.ACTION_MEDIA_EJECT);
            intentFilter.addDataScheme("file");
            mContext.registerReceiver(mMediaReceiver, intentFilter);
        }

        private void initTimeShift() {
            if (!mTimeShiftInited) {
                mTimeShiftInited = true;
                numActiveRecordings = recordingGetNumActiveRecordings();
                Log.i(TAG, "numActiveRecordings: " + numActiveRecordings);

                if (numRecorders == 0) {
                    updateRecorderNumber();
                }
                updateTunerNumber();
                if (numActiveRecordings < numRecorders) {
                    timeshiftAvailable.setYes(true);
                } else {
                    timeshiftAvailable.setNo(true);
                }
                /*timeshiftRecordRunnable = new Runnable() {
                    @RequiresApi(api = Build.VERSION_CODES.M)
                    @Override
                    public void run() {
                        Log.i(TAG, "timeshiftRecordRunnable running");
                        resetRecordingPath();
                        tryStartTimeshifting();
                    }
                };*/

                playerSetTimeshiftBufferSize(getTimeshiftBufferSizeMins(), getTimeshiftBufferSizeMBs());
                resetRecordingPath();
            }
        }

        @Override
        public void onSetMain(boolean isMain) {
            Log.d(TAG, "onSetMain, isMain: " + isMain +" mCurrentDtvkitTvInputSessionIndex is " + mCurrentDtvkitTvInputSessionIndex);
            mIsMain = isMain;
            if (!getFeatureSupportFullPipFccArchitecture()) {
                //isMain status may be set to true by livetv after switch to luncher
                if (mCurrentDtvkitTvInputSessionIndex < mDtvkitTvInputSessionCount) {
                    mIsMain = false;
                }
                if (!mIsMain) {
                     writeSysFs("/sys/class/video/disable_video", "1");
                    //if (null != mView)
                        //layoutSurface(0, 0, mView.w, mView.h);
                } else {
                        writeSysFs("/sys/class/video/video_inuse", "1");
                }
            }
        }

        private void doDestroyOverlay() {
            Log.i(TAG, "doDestroyOverlayr index = " + mCurrentDtvkitTvInputSessionIndex);
            if (mView != null) {
                mView.destroy();
            }
        }

        @Override
        public boolean onSetSurface(Surface surface) {
            Log.i(TAG, "onSetSurface " + surface + ", mIsMain = " + mIsMain + ", mDtvkitTvInputSessionCount = " + mDtvkitTvInputSessionCount + ", mCurrentDtvkitTvInputSessionIndex = " + mCurrentDtvkitTvInputSessionIndex);
            if (!getFeatureSupportFullPipFccArchitecture()) {
                if (null != mHardware && mConfigs.length > 0) {
                    if (null == surface) {
                        //isMain status may be set to true by livetv after switch to luncher
                        if (mDtvkitTvInputSessionCount == mCurrentDtvkitTvInputSessionIndex || mIsMain) {
                            setOverlayViewEnabled(false);
                            mHardware.setSurface(null, null);
                            //sendSetSurfaceMessage(null, null);
                            Log.d(TAG, "onSetSurface null");
                            mSurface = null;
                            sendDoReleaseMessage();
                            writeSysFs("/sys/class/video/video_inuse", "0");
                            mSystemControlManager.SetDtvKitSourceEnable(0);
                        }
                    } else {
                        if (mSurface != null && mSurface != surface) {
                            Log.d(TAG, "TvView swithed,  onSetSurface null first");
                            sendDoReleaseMessage();
                            mHardware.setSurface(null, null);
                            //sendSetSurfaceMessage(null, null);
                        }
                        mHardware.setSurface(surface, mConfigs[0]);
                        //createDecoder();
                        //decoderRelease();
                        //sendSetSurfaceMessage(surface, mConfigs[0]);
                        mSurface = surface;
                        mSystemControlManager.SetDtvKitSourceEnable(1);
                    }
                }
                //set surface to mediaplayer
                //DtvkitGlueClient.getInstance().setDisplay(surface);
            } else {
                //support multi surface and save it only here, then set it when tuned according to pip or main
                if (surface == null && (mDtvkitTvInputSessionCount == mCurrentDtvkitTvInputSessionIndex || mIsMain || mIsPip)) {
                    //stop play when set surface null
                    //android r use hal to set tunnel id for each surface
                    if (isSdkAfterAndroidQ()) {
                        if (!mIsPip) {
                            sendSetSurfaceMessage(null, mConfigs[1]);
                        } else {
                            sendSetSurfaceMessage(null, mPipConfigs[0]);
                        }
                    }
                    sendDoReleaseMessage();
                }
                if (mSurface != surface) {
                    mSurfaceSent = false;
                    mSurface = surface;
                    Log.d(TAG, "onSetSurface will be set to dtvkit when tuning");
                }
            }
            return true;
        }

        private void doSetSurface(Map<String, Object> surfaceInfo) {
            Log.i(TAG, "doSetSurface index = " + mCurrentDtvkitTvInputSessionIndex);
            if (surfaceInfo == null) {
                Log.d(TAG, "doSetSurface null parameter");
                return;
            } else {
                Surface surface = (Surface)surfaceInfo.get(ConstantManager.KEY_SURFACE);
                TvStreamConfig config = (TvStreamConfig)surfaceInfo.get(ConstantManager.KEY_TV_STREAM_CONFIG);
                Log.d(TAG, "doSetSurface surface = " + surface + ", config = " + config);
                boolean isPipConfig = false;
                if (config != null) {
                    //pip stream config has been set to 3
                    isPipConfig = config.getStreamId() == 3;
                }
                if (isPipConfig) {
                    mPipHardware.setSurface(surface, config);
                } else {
                    mHardware.setSurface(surface, config);
                }
            }
        }

        private void sendSetSurfaceMessage(Surface surface, TvStreamConfig config) {
            Map<String, Object> surfaceInfo = new HashMap<String, Object>();
            surfaceInfo.put(ConstantManager.KEY_SURFACE, surface);
            surfaceInfo.put(ConstantManager.KEY_TV_STREAM_CONFIG, config);
            if (mHandlerThreadHandle != null) {
                boolean result = mHandlerThreadHandle.sendMessage(mHandlerThreadHandle.obtainMessage(MSG_SET_SURFACE, surfaceInfo));
                Log.d(TAG, "sendSetSurfaceMessage status = " + result + ", surface = " + surface + ", config = " + config + ", index = " + mCurrentDtvkitTvInputSessionIndex);
            } else {
                Log.d(TAG, "sendSetSurfaceMessage null mHandlerThreadHandle");
            }
        }

        private void fillSurfaceWithFixColor() {
            Rect frame = null;
            if (mView != null) {
                frame = getViewFrameOnScreen(mView);
            }
            if (mSurface != null) {
                boolean lockCanvas = false;
                Canvas canvas = null;
                try {
                    if (frame != null) {
                        canvas = mSurface.lockCanvas(frame);
                        lockCanvas = true;
                        if (canvas != null) {
                            int color = getFeatureSupportFillSurfaceColor();
                            Log.d(TAG, "fillSurfaceWithFixColor color = " + Integer.toHexString(color));
                            canvas.drawColor(color);
                        }
                    } else {
                        Log.d(TAG, "fillSurfaceWithFixColor null frame");
                    }
                } catch (Exception e) {
                    Log.d(TAG, "fillSurfaceWithFixColor Exception = " + e.getMessage());
                } finally {
                    if (lockCanvas) {
                        mSurface.unlockCanvasAndPost(canvas);
                    } else {
                        Log.d(TAG, "fillSurfaceWithFixColor fail to lock surface");
                    }
                }
            } else {
                Log.d(TAG, "fillSurfaceWithFixColor null surface");
            }
        }

        private Rect getViewFrameOnScreen(View view) {
            Rect frame = new Rect();
            view.getGlobalVisibleRect(frame);
            RectF frameF = new RectF(frame);
            view.getMatrix().mapRect(frameF);
            frameF.round(frame);
            return frame;
        }

        private void sendFillSurface() {
            if (mMainHandle != null) {
                mMainHandle.removeMessages(MSG_FILL_SURFACE);
                mMainHandle.sendEmptyMessage(MSG_FILL_SURFACE);
            }
        }

        @Override
        public void onSurfaceChanged(int format, int width, int height) {
            Log.i(TAG, "onSurfaceChanged " + format + ", " + width + ", " + height + ", index = " + mCurrentDtvkitTvInputSessionIndex);
            //playerSetRectangle(0, 0, width, height);
        }

        public View onCreateOverlayView() {
            Log.i(TAG, "onCreateOverlayView index = " + mCurrentDtvkitTvInputSessionIndex);
            if (mView == null) {
                mView = new DtvkitOverlayView(mContext, mMainHandle);
            }
            return mView;
        }

        @Override
        public void onOverlayViewSizeChanged(int width, int height) {
            Log.i(TAG, "onOverlayViewSizeChanged " + width + ", " + height + ", index = " + mCurrentDtvkitTvInputSessionIndex);
            if (mView == null) {
                mView = new DtvkitOverlayView(mContext, mMainHandle);
            }
            if (!getFeatureSupportFullPipFccArchitecture()) {
                playerSetRectangle(0, 0, width, height);
                mView.setSize(width, height);
            } else {
                if (mSurfaceSent) {
                    if (mIsPip) {
                        playerPipSetRectangle(0, 0, width, height);
                    } else {
                        mView.nativeOverlayView.setOverlayTarge(mView.nativeOverlayView.mTarget);
                        mView.mSubServerView.setOverlaySubtitleListener(mView.mSubServerView.mSubListener);
                        playerSetRectangle(0, 0, width, height);
                    }
                }
                mView.setSize(width, height);
            }
            mWinWidth = width;
            mWinHeight = height;
            setOverlayViewEnabled(true);
        }

        @Override
        public boolean onTune(Uri channelUri, Bundle params) {
            if (params != null) {
                if (getFeatureSupportPip()) {
                    mIsPip = params.getBoolean("is_pip");
                }
                String previous = params.getString("previous_buffer_uri");
                String next = params.getString("next_buffer_uri");
                Log.d(TAG, "onTune previous = " + previous + ", channelUri = " + channelUri + ", next = " + next);
                if (getFeatureSupportFcc()) {
                    mPreviousBufferUri = previous != null ? Uri.parse(previous) : null;
                    mNextBufferUri = next != null ? Uri.parse(next) : null;
                }
            }
            return onTune(channelUri);
        }

        @RequiresApi(api = Build.VERSION_CODES.M)
        @Override
        public boolean onTune(Uri channelUri) {
            Log.i(TAG, "onTune " + channelUri + ", index = " + mCurrentDtvkitTvInputSessionIndex);
            if (ContentUris.parseId(channelUri) == -1) {
                Log.e(TAG, "DtvkitTvInputSession onTune invalid channelUri = " + channelUri);
                return false;
            }
            if (mMainHandle != null) {
                mMainHandle.sendEmptyMessage(MSG_HIDE_SCAMBLEDTEXT);
                mMainHandle.removeMessages(MSG_SET_TELETEXT_MIX_NORMAL);
                mMainHandle.sendEmptyMessage(MSG_SET_TELETEXT_MIX_NORMAL);
            }
            if (mHandlerThreadHandle != null) {
                mHandlerThreadHandle.removeMessages(MSG_ON_TUNE);
                Message mess = mHandlerThreadHandle.obtainMessage(MSG_ON_TUNE, 0, 0, channelUri);
                boolean info = mHandlerThreadHandle.sendMessage(mess);
                Log.d(TAG, "sendMessage " + info);
            }

            mPreviousTunedChannel = mTunedChannel;
            mTunedChannel = getChannel(channelUri);

            Log.i(TAG, "onTune will be Done in onTuneByHandlerThreadHandle");
            return mTunedChannel != null;
        }

        protected boolean onTuneByHandlerThreadHandle(Uri channelUri, boolean mhegTune) {
            Log.i(TAG, "onTuneByHandlerThreadHandle " + channelUri + ", index = " + mCurrentDtvkitTvInputSessionIndex + ", mIsPip = " + mIsPip);

            if (ContentUris.parseId(channelUri) == -1) {
                Log.e(TAG, "onTuneByHandlerThreadHandle invalid channelUri = " + channelUri);
                return false;
            }

            try {
                if (!mTuned) {
                    if (!mIsPip) {
                        if (!mMainSessionSemaphore.tryAcquire(SEMAPHORE_TIME_OUT, TimeUnit.MILLISECONDS)) {
                            if (mReleased) {
                                Log.d(TAG, "onTuneByHandlerThreadHandle mMainSessionSemaphore timeout but session released");
                            } else {
                                Log.d(TAG, "onTuneByHandlerThreadHandle mMainSessionSemaphore timeout");
                            }
                            return false;
                        } else {
                            if (mReleased) {
                                Log.d(TAG, "onTuneByHandlerThreadHandle Acquire mMainSessionSemaphore ok but session released");
                                return false;
                            } else {
                                mAquireMainSemaphore = true;
                                Log.d(TAG, "onTuneByHandlerThreadHandle Acquire mMainSessionSemaphore ok");
                            }
                        }
                    } else {
                        if (!mPipSessionSemaphore.tryAcquire(SEMAPHORE_TIME_OUT, TimeUnit.MILLISECONDS)) {
                            if (mReleased) {
                                Log.d(TAG, "onTuneByHandlerThreadHandle mPipSessionSemaphore timeout but session released");
                            } else {
                                Log.d(TAG, "onTuneByHandlerThreadHandle mPipSessionSemaphore timeout");
                            }
                            return false;
                        } else {
                            if (mReleased) {
                                Log.d(TAG, "onTuneByHandlerThreadHandle Acquire mPipSessionSemaphore ok but session released");
                                return false;
                            } else {
                                mAquirePipSemaphore = true;
                                Log.d(TAG, "onTuneByHandlerThreadHandle Acquire mPipSessionSemaphore ok");
                            }
                        }
                    }
                }
            } catch (Exception e) {
                Log.d(TAG, "onTuneByHandlerThreadHandle tryAcquire Exception = " + e.getMessage());
                return false;
            }

            mTuned = true;
            boolean supportFullPipFccArchitecture = getFeatureSupportFullPipFccArchitecture();
            if (!mIsPip) {
                if (supportFullPipFccArchitecture && !mSurfaceSent && mSurface != null) {
                    mSurfaceSent = true;
                    mView.nativeOverlayView.setOverlayTarge(mView.nativeOverlayView.mTarget);
                    mView.mSubServerView.setOverlaySubtitleListener(mView.mSubServerView.mSubListener);
                    if (isSdkAfterAndroidQ()) {
                        mHardware.setSurface(mSurface, mConfigs[1]);
                        setSurfaceTunnelId(INDEX_FOR_MAIN, 1);
                    } else {
                        DtvkitGlueClient.getInstance().setMutilSurface(INDEX_FOR_MAIN, mSurface);
                    }
                    playerSetRectangle(0, 0, mWinWidth, mWinHeight);
                }
                removeScheduleTimeshiftRecordingTask();
                initTimeShift();
                if (timeshiftRecorderState != RecorderState.STOPPED) {
                    Log.i(TAG, "reset timeshiftState to STOPPED.");
                    timeshiftRecorderState = RecorderState.STOPPED;
                    timeshifting = false;
                    scheduleTimeshiftRecording = false;
                    playerStopTimeshiftRecording(false);
                }
                timeshiftAvailable.setYes(true);
            } else {
                if (!mSurfaceSent && mSurface != null) {
                    mSurfaceSent = true;
                    if (isSdkAfterAndroidQ()) {
                        mPipHardware.setSurface(mSurface, mPipConfigs[0]);
                        setSurfaceTunnelId(INDEX_FOR_PIP, 2);
                    } else {
                        DtvkitGlueClient.getInstance().setMutilSurface(INDEX_FOR_PIP, mSurface);
                    }
                    playerPipSetRectangle(0, 0, mWinWidth, mWinHeight);
                }
                Log.i(TAG, "onTuneByHandlerThreadHandle pip tune");
            }

            Channel tunedChannel  = getChannel(channelUri);
            String dvbUri = null;
            boolean isVirtual = false;
            if (tunedChannel != null) {
                isVirtual = TvContractUtils.getBooleanFromChannelInternalProviderData(tunedChannel, Channel.KEY_CHANNEL_CI_VIRTUAL_CHANNEL, false);
                //Log.d(TAG, "onTuneByHandlerThreadHandle isVirtual = " + isVirtual + ", getInternalProviderData = " + tunedChannel.getInternalProviderData());
            }
            if (isVirtual) {
                dvbUri = "vc://" + TvContractUtils.getStringFromChannelInternalProviderData(tunedChannel, Channel.KEY_CHANNEL_CI_NUMBER, null);
            } else {
                dvbUri = getChannelInternalDvbUri(tunedChannel);
            }
            boolean mainMuteStatus = false;
            notifyVideoUnavailable(TvInputManager.VIDEO_UNAVAILABLE_REASON_TUNING);

            if (!mIsPip) {
                mainMuteStatus = playerGetMute();
                if (tunedChannel != null) {
                    String previousCiNumber = null;
                    String previousCiProfileVersion = null;
                    String previousProfileName = null;
                    if (mPreviousTunedChannel != null) {
                        previousCiNumber = TvContractUtils.getStringFromChannelInternalProviderData(mPreviousTunedChannel, Channel.KEY_CHANNEL_CI_NUMBER, null);
                        previousProfileName = TvContractUtils.getStringFromChannelInternalProviderData(mPreviousTunedChannel, Channel.KEY_CHANNEL_PROFILE, null);
                        previousCiProfileVersion = TvContractUtils.getStringFromChannelInternalProviderData(mPreviousTunedChannel, Channel.KEY_CHANNEL_CI_PROFILE_VERSION, null);
                    }
                    String ciNumber = TvContractUtils.getStringFromChannelInternalProviderData(tunedChannel, Channel.KEY_CHANNEL_CI_NUMBER, null);
                    String profileName = TvContractUtils.getStringFromChannelInternalProviderData(tunedChannel, Channel.KEY_CHANNEL_PROFILE, null);
                    String ciProfileVersion = TvContractUtils.getStringFromChannelInternalProviderData(tunedChannel, Channel.KEY_CHANNEL_CI_PROFILE_VERSION, null);
                    if (!TextUtils.equals(previousCiNumber, ciNumber)) {
                        Log.i(TAG, "onTuneByHandlerThreadHandle ci changed = " + ciNumber + "(" + (ciNumber == null ? "not ci channel" : "ci channel)"));
                        if ("v2".equalsIgnoreCase(previousCiProfileVersion)) {
                            playerNotifyCiProfileEvent("leave", previousCiNumber, previousProfileName, "v2");
                        }
                        if ("v2".equalsIgnoreCase(ciProfileVersion)) {
                            playerNotifyCiProfileEvent("enter", ciNumber, profileName, "v2");
                        }
                    } else {
                        Log.i(TAG, "onTuneByHandlerThreadHandle ci (" + (ciNumber == null ? "not ci channel" : "same ciNumber") + ")");
                    }
                }
                if (mhegTune) {
                    mhegSuspend();
                    if (mhegGetNextTuneInfo(dvbUri) == 0)
                        notifyChannelRetuned(channelUri);
                } else {
                    mhegStop();
                }
                //playerStopTeletext();//no need to save teletext select status
                if (!supportFullPipFccArchitecture) {
                    playerStop();
                } else if (mPreviousBufferUri == null && mNextBufferUri == null) {
                    playerStop();
                } else {
                    Log.d(TAG, "onTuneByHandlerThreadHandle act ffc next and no need to stop ffc and main");
                    //playerStopAndKeepFfc();
                }
                playerSetSubtitlesOn(false);
                playerSetTeletextOn(false, -1);
                //setParentalControlOn(false);
                //playerResetAssociateDualSupport();
                userDataStatus(false);
            } else  {
                playerPipStop();
            }

            //comment it as need add pip
            //writeSysFs("/sys/class/video/video_global_output", "0");

            mKeyUnlocked = false;
            mDvbNetworkChangeSearchStatus = false;
            //parent control may need separate stream time for main and pip
            if (mTvInputManager != null) {
                boolean parentControlSwitch = mTvInputManager.isParentalControlsEnabled();
                if (parentControlSwitch) {
                    //init it to wait for update
                    mBlocked = true;
                    updateParentalControlExt();
                } else {
                    mBlocked=  false;
                    notifyContentAllowed();
                }
                /*boolean parentControlStatus = getParentalControlOn();
                if (parentControlSwitch != parentControlStatus) {
                    setParentalControlOn(parentControlSwitch);
                }
                if (parentControlSwitch) {
                    syncParentControlSetting();
                }*/
            }
            mAudioADAutoStart = mDataMananer.getIntParameters(DataMananer.TV_KEY_AD_SWITCH) == 1;
            if (mAudioADAutoStart) {
                setAdFunction(MSG_MIX_AD_SWITCH_ENABLE, 1);
            } else {
                setAdFunction(MSG_MIX_AD_SWITCH_ENABLE, 0);
            }
            boolean playResult = false;
            if (!mIsPip) {
                DtvkitGlueClient.getInstance().registerSignalHandler(mHandler);
                String previousUriStr = "";
                String nextUriStr = "";
                if (mPreviousBufferUri != null) {
                    previousUriStr = getChannelInternalDvbUriForFcc(mPreviousBufferUri);
                }
                if (mNextBufferUri != null) {
                    nextUriStr = getChannelInternalDvbUriForFcc(mNextBufferUri);
                }
                playResult = playerPlay(INDEX_FOR_MAIN, dvbUri, mAudioADAutoStart, mainMuteStatus, 0, previousUriStr, nextUriStr).equals("ok");
            } else {
                DtvkitGlueClient.getInstance().registerSignalHandler(mHandler, INDEX_FOR_PIP);
                playResult = playerPlay(INDEX_FOR_PIP, dvbUri, mAudioADAutoStart, true, 0, "", "").equals("ok");
            }
            if (playResult) {
                if (mHandlerThreadHandle != null) {
                    mHandlerThreadHandle.removeMessages(MSG_CHECK_PARENTAL_CONTROL);
                    mHandlerThreadHandle.sendEmptyMessageDelayed(MSG_CHECK_PARENTAL_CONTROL, MSG_CHECK_PARENTAL_CONTROL_PERIOD);
                }
                /*
                if (!getFeatureSupportCaptioningManager() || (mCaptioningManager != null && mCaptioningManager.isEnabled())) {
                    playerSetSubtitlesOn(true);
                }
                */
                if (!mIsPip) {
                    userDataStatus(true);
                    playerInitAssociateDualSupport();
                }

                try {
                    String signalType = tunedChannel.getInternalProviderData().get("channel_signal_type").toString();
                    mParameterMananer.saveStringParameters(ParameterMananer.AUTO_SEARCHING_SIGNALTYPE, signalType);
                } catch(Exception e) {
                    Log.i(TAG, "SignalType Exception " + e.getMessage());
                }
            } else {
                DtvkitGlueClient.getInstance().unregisterSignalHandler(mHandler);
                mTunedChannel = null;
                notifyVideoUnavailable(TvInputManager.VIDEO_UNAVAILABLE_REASON_UNKNOWN);
                notifyTimeShiftStatusChanged(TvInputManager.TIME_SHIFT_STATUS_UNAVAILABLE);

                Log.e(TAG, "No play path available");
                Bundle event = new Bundle();
                event.putString(ConstantManager.KEY_INFO, "No play path available");
                notifySessionEvent(ConstantManager.EVENT_RESOURCE_BUSY, event);
            }
            Log.i(TAG, "onTuneByHandlerThreadHandle Done");
            if (getFeatureSupportFillSurface() && !mIsPip) {
                sendFillSurface();
            }
            return playResult;
        }

        @Override
        public void onRelease() {
            Log.i(TAG, "onRelease index = " + mCurrentDtvkitTvInputSessionIndex + ", mIsPip = " + mIsPip);
            mReleased = true;
            //must destory mview,!!! we
            //will regist handle to client when
            //creat ciMenuView,so we need destory and
            //unregist handle.
            //if (!getFeatureSupportFullPipFccArchitecture()) {
                //mSystemControlManager.SetDtvKitSourceEnable(0);
                releaseSignalHandler();
                if (mDtvkitTvInputSessionCount == mCurrentDtvkitTvInputSessionIndex || mIsMain || mIsPip || mSurface == null) {
                    //release by message queue for current session
                    if (mMainHandle != null) {
                        mMainHandle.sendMessageAtFrontOfQueue(mMainHandle.obtainMessage(MSG_MAIN_HANDLE_DESTROY_OVERLAY));
                    } else {
                        Log.i(TAG, "onRelease mMainHandle == null");
                    }
                } else {
                    //release directly as new session has created
                    finalReleaseWorkThread(false, false);
                    doDestroyOverlay();
                }
                //send MSG_RELEASE_WORK_THREAD after dealing destroy overlay
            //}
            mContext.unregisterReceiver(mMediaReceiver);
            mMediaReceiver = null;
            hideStreamChangeUpdateDialog();
            Log.i(TAG, "onRelease over index = " + mCurrentDtvkitTvInputSessionIndex + ", mIsPip = " + mIsPip);
        }

        private void doRelease(boolean keepSession, boolean needUpdate) {
            Log.i(TAG, "doRelease index = " + mCurrentDtvkitTvInputSessionIndex + ", mIsPip = " + mIsPip + ", keepSession = " + keepSession + ", needUpdate = " + needUpdate);
            if (!keepSession) {
                removeTunerSession(this);
            }
            releaseSignalHandler();
            if (!mIsPip) {
                removeScheduleTimeshiftRecordingTask();
                scheduleTimeshiftRecording = false;
                timeshiftRecorderState = RecorderState.STOPPED;
                timeshifting = false;
                mhegStop();
                playerStopTimeshiftRecording(false);
                playerStop();
                playerSetSubtitlesOn(false);
                playerSetTeletextOn(false, -1);
            } else {
                playerPipStop();
            }
            finalReleaseWorkThread(keepSession, needUpdate);
            Log.i(TAG, "doRelease over index = " + mCurrentDtvkitTvInputSessionIndex + ", mIsPip = " + mIsPip);
        }

        private synchronized void finalReleaseWorkThread(boolean keepSession, boolean needUpdate) {
            Log.d(TAG, "finalReleaseWorkThread index = " + mCurrentDtvkitTvInputSessionIndex + ", mIsPip = " + mIsPip + ", keepSession = " + keepSession + ", needUpdate = " + needUpdate);
            mReleaseHandleMessage = true;
            if (mMainHandle != null) {
                mMainHandle.removeCallbacksAndMessages(null);
            }
            if (mHandlerThreadHandle != null) {
                mHandlerThreadHandle.removeCallbacksAndMessages(null);
            }
            //don't set it to none
            //mMainHandle = null;
            //mHandlerThreadHandle = null;
            if (keepSession && mHandlerThreadHandle != null) {
                mReleaseHandleMessage = false;
                if (needUpdate && !(mStreamChangeUpdateDialog != null && mStreamChangeUpdateDialog.isShowing())) {
                    mHandlerThreadHandle.removeMessages(MSG_SEND_DISPLAY_STREAM_CHANGE_DIALOG);
                    mHandlerThreadHandle.sendMessageDelayed(mHandlerThreadHandle.obtainMessage(MSG_SEND_DISPLAY_STREAM_CHANGE_DIALOG, (mIsPip ?  INDEX_FOR_PIP : INDEX_FOR_MAIN), 0), MSG_SHOW_STREAM_CHANGE_DELAY);
                }
            } else {
                if (mLivingHandlerThread != null) {
                    mLivingHandlerThread.getLooper().quitSafely();
                    mLivingHandlerThread = null;
                }
            }
            if (!mIsPip) {
                if (mAquireMainSemaphore) {
                    Log.d(TAG, "finalReleaseWorkThread mMainSessionSemaphore release");
                    mMainSessionSemaphore.release();
                } else {
                    Log.d(TAG, "finalReleaseWorkThread mMainSessionSemaphore not aquired");
                }
            } else {
                if (mAquirePipSemaphore) {
                    Log.d(TAG, "finalReleaseWorkThread mPipSessionSemaphore release");
                    mPipSessionSemaphore.release();
                } else {
                    Log.d(TAG, "finalReleaseWorkThread mPipSessionSemaphore not aquired");
                }
            }
            Log.d(TAG, "finalReleaseWorkThread over , mIsPip = " + mIsPip);
        }

        private void releaseSignalHandler() {
            Log.d(TAG, "releaseSignalHandler index = " + mCurrentDtvkitTvInputSessionIndex);
            DtvkitGlueClient.getInstance().unregisterSignalHandler(mHandler);
        }

        private void sendDoReleaseMessage() {
            if (mHandlerThreadHandle != null) {
                boolean result = mHandlerThreadHandle.sendMessage(mHandlerThreadHandle.obtainMessage(MSG_DO_RELEASE));
                Log.d(TAG, "sendDoReleaseMessage status = " + result + " index = " + mCurrentDtvkitTvInputSessionIndex);
            } else {
                Log.d(TAG, "sendDoReleaseMessage null mHandlerThreadHandle");
            }
        }

        private void sendDoReleaseSpecifiedSessionMessage(DtvkitTvInputSession session, boolean needKeep, boolean needUpdate) {
            if (mHandlerThreadHandle != null) {
                boolean result = mHandlerThreadHandle.sendMessage(mHandlerThreadHandle.obtainMessage(MSG_DO_RELEASE_SPECIFIELD_SESSION,
                        needKeep ? 1 : 0, needUpdate ? 1 : 0, session));
                Log.d(TAG, "sendDoReleaseSpecifiedSessionMessage status = " + result + " index = " + mCurrentDtvkitTvInputSessionIndex);
            } else {
                Log.d(TAG, "sendDoReleaseSpecifiedSessionMessage null mHandlerThreadHandle");
            }
        }

        private void doReleaseSpecifiedSession(DtvkitTvInputSession session, boolean needKeep, boolean needUpdate) {
            if (session != null) {
                Log.d(TAG, "doReleaseSpecifiedSession index = " + mCurrentDtvkitTvInputSessionIndex);
                session.doRelease(needKeep, needUpdate);
            } else {
                Log.d(TAG, "doReleaseSpecifiedSession null session");
            }
        }

        @Override
        public void onSetStreamVolume(float volume) {
            Log.i(TAG, "onSetStreamVolume " + volume + ", mute " + (volume == 0.0f) + "index = " + mCurrentDtvkitTvInputSessionIndex);
            //playerSetVolume((int) (volume * 100));
            if (mHandlerThreadHandle != null) {
                mHandlerThreadHandle.removeMessages(MSG_BLOCK_MUTE_OR_UNMUTE);
                Message mess = mHandlerThreadHandle.obtainMessage(MSG_BLOCK_MUTE_OR_UNMUTE, (volume == 0.0f ? 1 : 0), 0);
                mHandlerThreadHandle.sendMessageDelayed(mess, MSG_BLOCK_MUTE_OR_UNMUTE_PERIOD);
            }
        }

        @Override
        public void onSetCaptionEnabled(boolean enabled) {
            Log.i(TAG, "onSetCaptionEnabled " + enabled + ", index = " + mCurrentDtvkitTvInputSessionIndex);
            if (true) {
                Log.i(TAG, "caption switch will be controlled by mCaptionManager switch");
                return;
            }
            /*Log.i(TAG, "onSetCaptionEnabled " + enabled);
            // TODO CaptioningManager.getLocale()
            playerSetSubtitlesOn(enabled);//start it in select track or gettracks in onsignal*/
        }

        @Override
        public boolean onSelectTrack(int type, String trackId) {
            Log.i(TAG, "onSelectTrack " + type + ", " + trackId + ", index = " + mCurrentDtvkitTvInputSessionIndex);
            boolean result = false;
            if (mHandlerThreadHandle != null) {
                Message mess = mHandlerThreadHandle.obtainMessage(MSG_SELECT_TRACK, type, 0, trackId);
                result = mHandlerThreadHandle.sendMessage(mess);
                Log.d(TAG, "onSelectTrack sendMessage result " + result);
            }
            return result;
        }

        private boolean doSelectTrack(int type, String trackId) {
            boolean result = false;
            Log.i(TAG, "doSelectTrack " + type + ", " + trackId + ", index = " + mCurrentDtvkitTvInputSessionIndex);
            if (type == TvTrackInfo.TYPE_AUDIO) {
                if (playerSelectAudioTrack((null == trackId) ? 0xFFFF : Integer.parseInt(trackId))) {
                    notifyTrackSelected(type, trackId);
                    //check trackinfo update
                    if (mHandlerThreadHandle != null) {
                        mHandlerThreadHandle.removeMessages(MSG_UPDATE_TRACKINFO);
                        mHandlerThreadHandle.sendEmptyMessageDelayed(MSG_UPDATE_TRACKINFO, MSG_UPDATE_TRACKINFO_DELAY);
                    }
                    result = true;
                }
            } else if (type == TvTrackInfo.TYPE_SUBTITLE) {
                String sourceTrackId = trackId;
                int subType = 4;//default sub
                int isTele = 0;//default subtitle
                if (!TextUtils.isEmpty(trackId) && !TextUtils.isDigitsOnly(trackId)) {
                    String[] nameValuePairs = trackId.split("&");
                    if (nameValuePairs != null && nameValuePairs.length == 5) {
                        String[] nameValue = nameValuePairs[0].split("=");
                        String[] typeValue = nameValuePairs[1].split("=");
                        String[] teleValue = nameValuePairs[2].split("=");
                        if (nameValue != null && nameValue.length == 2 && TextUtils.equals(nameValue[0], "id") && TextUtils.isDigitsOnly(nameValue[1])) {
                            trackId = nameValue[1];//parse id
                        }
                        if (typeValue != null && typeValue.length == 2 && TextUtils.equals(typeValue[0], "type") && TextUtils.isDigitsOnly(typeValue[1])) {
                            subType = Integer.parseInt(typeValue[1]);//parse type
                        }
                        if (teleValue != null && teleValue.length == 2 && TextUtils.equals(teleValue[0], "teletext") && TextUtils.isDigitsOnly(teleValue[1])) {
                            isTele = Integer.parseInt(teleValue[1]);//parse type
                        }
                    }
                    if (TextUtils.isEmpty(trackId) || !TextUtils.isDigitsOnly(trackId)) {
                        //notifyTrackSelected(type, sourceTrackId);
                        Log.d(TAG, "need trackId that only contains number sourceTrackId = " + sourceTrackId + ", trackId = " + trackId);
                        result = false;
                        return result;
                    }
                }
                if ((!getFeatureSupportCaptioningManager() || mCaptioningManager.isEnabled()) && selectSubtitleOrTeletext(isTele, subType, trackId)) {
                    notifyTrackSelected(type, sourceTrackId);
                } else {
                    Log.d(TAG, "onSelectTrack mCaptioningManager closed or invlid sub");
                    notifyTrackSelected(type, null);
                }
                result = true;
            }
            return result;
        }

        private boolean selectSubtitleOrTeletext(int istele, int type, String indexId) {
            boolean result = false;
            Log.d(TAG, "selectSubtitleOrTeletext istele = " + istele + ", type = " + type + ", indexId = " + indexId);
            if (TextUtils.isEmpty(indexId)) {//stop
                if (playerGetSubtitlesOn()) {
                    playerSetSubtitlesOn(false);//close if opened
                    Log.d(TAG, "selectSubtitleOrTeletext off setSubOff");
                }
                if (playerIsTeletextOn()) {
                    boolean setTeleOff = playerSetTeletextOn(false, -1);//close if opened
                    Log.d(TAG, "selectSubtitleOrTeletext off setTeleOff = " + setTeleOff);
                }
                boolean stopSub = playerSelectSubtitleTrack(0xFFFF);
                boolean stopTele = playerSelectTeletextTrack(0xFFFF);
                Log.d(TAG, "selectSubtitleOrTeletext stopSub = " + stopSub + ", stopTele = " + stopTele);
                result = true;
            } else if (TextUtils.isDigitsOnly(indexId)) {
                if (type == 4) {//sub
                    if (playerIsTeletextOn()) {
                        boolean setTeleOff = playerSetTeletextOn(false, -1);
                        Log.d(TAG, "selectSubtitleOrTeletext onsub setTeleOff = " + setTeleOff);
                    }
                    if (!playerGetSubtitlesOn()) {
                        playerSetSubtitlesOn(true);
                        Log.d(TAG, "selectSubtitleOrTeletext onsub setSubOn");
                    }
                    boolean startSub = playerSelectSubtitleTrack(Integer.parseInt(indexId));
                    Log.d(TAG, "selectSubtitleOrTeletext startSub = " + startSub);
                } else if (type == 6) {//teletext
                    if (playerGetSubtitlesOn()) {
                        playerSetSubtitlesOn(false);
                        Log.d(TAG, "selectSubtitleOrTeletext ontele setSubOff");
                    }
                    if (!playerIsTeletextOn()) {
                        boolean setTeleOn = playerSetTeletextOn(true, Integer.parseInt(indexId));
                        Log.d(TAG, "selectSubtitleOrTeletext start setTeleOn = " + setTeleOn);
                    } else {
                        boolean startTele = false;
                        if ((getSubtitleFlag() & SUBCTL_HK_TTXPAG) == SUBCTL_HK_TTXPAG) {
                            startTele = playerSetTeletextOn(true, Integer.parseInt(indexId));
                        }else {
                            startTele = playerSelectTeletextTrack(Integer.parseInt(indexId));
                        }
                        Log.d(TAG, "selectSubtitleOrTeletext set setTeleOn = " + startTele);
                    }
                }
                result = true;
            } else {
                result = false;
                Log.d(TAG, "selectSubtitleOrTeletext unkown case");
            }
            return result;
        }

        private boolean initSubtitleOrTeletextIfNeed() {
            boolean isSubOn = playerGetSubtitlesOn();
            boolean isTeleOn = playerIsTeletextOn();
            String subTrackId = playerGetSelectedSubtitleTrackId();
            String teleTrackId = playerGetSelectedTeleTextTrackId();
            int subIndex = playerGetSelectedSubtitleTrack();
            int teleIndex = playerGetSelectedTeleTextTrack();
            Log.d(TAG, "initSubtitleOrTeletextIfNeed isSubOn = " + isSubOn + ", isTeleOn = " + isTeleOn + ", subTrackId = " + subTrackId + ", teleTrackId = " + teleTrackId);
            if (isSubOn) {
                notifyTrackSelected(TvTrackInfo.TYPE_SUBTITLE, subTrackId);
            } else if (isTeleOn) {
                //notifyTrackSelected(TvTrackInfo.TYPE_SUBTITLE, teleTrackId);//marked it, because dvbcore don't keep teletext on/off status;
            } else {
                notifyTrackSelected(TvTrackInfo.TYPE_SUBTITLE, null);
            }
            return true;
        }

        private void updateTrackAndSelect(int isDvrPlaying) {
            boolean retuneSubtile = true;

            if ((isDvrPlaying == 1) && (dvrSubtitleFlag == 1)) {
                retuneSubtile = false;
            }

            if (retuneSubtile
                && (!getFeatureSupportCaptioningManager() || (mCaptioningManager != null && mCaptioningManager.isEnabled()))) {
                    playerSetSubtitlesOn(true);
                    if (isDvrPlaying == 1) {
                        dvrSubtitleFlag = 1;
                    }
            }

            List<TvTrackInfo> tracks = playerGetTracks(mTunedChannel, false);
            if (!tracks.equals(mTunedTracks)) {
                mTunedTracks = tracks;
                Log.d(TAG, "updateTrackAndSelect update new mTunedTracks");
            }
            notifyTracksChanged(mTunedTracks);
            int audioTrack = playerGetSelectedAudioTrack();
            Log.i(TAG, "updateTrackAndSelect audio track selected: " + audioTrack);
            notifyTrackSelected(TvTrackInfo.TYPE_AUDIO, Integer.toString(audioTrack));
            initSubtitleOrTeletextIfNeed();
        }

        @Override
        public void onUnblockContent(TvContentRating unblockedRating) {
            super.onUnblockContent(unblockedRating);
            if (mHandlerThreadHandle != null) {
                mHandlerThreadHandle.removeMessages(MSG_SET_UNBLOCK);
                Message mess = mHandlerThreadHandle.obtainMessage(MSG_SET_UNBLOCK,unblockedRating);
                boolean result = mHandlerThreadHandle.sendMessage(mess);
                Log.d(TAG, "onUnblockContent sendMessage result " + result);
            }
        }

        @Override
        public void notifyVideoAvailable() {
            super.notifyVideoAvailable();
            if (mMainHandle != null) {
                mMainHandle.sendEmptyMessage(MSG_HIDE_SCAMBLEDTEXT);
                mMainHandle.sendEmptyMessage(MSG_HIDE_TUNING_IMAGE);
            }

            int subFlg = getSubtitleFlag();
            if (mMainHandle != null && subFlg >= SUBCTL_HK_DVBSUB) {
                Message msg = mMainHandle.obtainMessage(MSG_SUBTITLE_SHOW_CLOSED_CAPTION, 0, 0, null);
                mMainHandle.sendMessage(msg);
            }
        }

        @Override
        public void notifyVideoUnavailable(final int reason) {
            super.notifyVideoUnavailable(reason);
            if (TvInputManager.VIDEO_UNAVAILABLE_REASON_AUDIO_ONLY == reason) {
                if (mMainHandle != null) {
                    mMainHandle.sendEmptyMessage(MSG_HIDE_SCAMBLEDTEXT);
                    mMainHandle.sendEmptyMessage(MSG_HIDE_TUNING_IMAGE);
                }
            }
        }

        @Override
        public void onAppPrivateCommand(String action, Bundle data) {
            Log.i(TAG, "onAppPrivateCommand " + action + ", " + data + ", index = " + mCurrentDtvkitTvInputSessionIndex);
            if ("action_teletext_start".equals(action) && data != null) {
                boolean start = data.getBoolean("action_teletext_start", false);
                Log.d(TAG, "do private cmd: action_teletext_start: "+ start);
            } else if ("action_teletext_up".equals(action) && data != null) {
                boolean actionup = data.getBoolean("action_teletext_up", false);
                Log.d(TAG, "do private cmd: action_teletext_up: "+ actionup);
                playerNotifyTeletextEvent(16);
            } else if ("action_teletext_down".equals(action) && data != null) {
                boolean actiondown = data.getBoolean("action_teletext_down", false);
                Log.d(TAG, "do private cmd: action_teletext_down: "+ actiondown);
                playerNotifyTeletextEvent(15);
            } else if ("action_teletext_number".equals(action) && data != null) {
                int number = data.getInt("action_teletext_number", -1);
                Log.d(TAG, "do private cmd: action_teletext_number: "+ number);
                final int TT_EVENT_0 = 4;
                final int TT_EVENT_9 = 13;
                int hundred = (number % 1000) / 100;
                int decade = (number % 100) / 10;
                int unit = (number % 10);
                if (number >= 100) {
                    playerNotifyTeletextEvent(hundred + TT_EVENT_0);
                    playerNotifyTeletextEvent(decade + TT_EVENT_0);
                    playerNotifyTeletextEvent(unit + TT_EVENT_0);
                } else if (number >= 10 && number < 100) {
                    playerNotifyTeletextEvent(decade + TT_EVENT_0);
                    playerNotifyTeletextEvent(unit + TT_EVENT_0);
                } else if (number < 10) {
                    playerNotifyTeletextEvent(unit + TT_EVENT_0);
                }
            } else if ("action_teletext_country".equals(action) && data != null) {
                int number = data.getInt("action_teletext_country", -1);
                Log.d(TAG, "do private cmd: action_teletext_country: "+ number);
                final int TT_EVENT_0 = 4;
                final int TT_EVENT_9 = 13;
                int hundred = (number % 1000) / 100;
                int decade = (number % 100) / 10;
                int unit = (number % 10);
                if (number >= 100) {
                    playerNotifyTeletextEvent(hundred + TT_EVENT_0);
                    playerNotifyTeletextEvent(decade + TT_EVENT_0);
                    playerNotifyTeletextEvent(unit + TT_EVENT_0);
                } else if (number >= 10 && number < 100) {
                    playerNotifyTeletextEvent(decade + TT_EVENT_0);
                    playerNotifyTeletextEvent(unit + TT_EVENT_0);
                } else if (number < 10) {
                    playerNotifyTeletextEvent(unit + TT_EVENT_0);
                }
            } else if ("quick_navigate_1".equals(action) && data != null) {
                boolean actiondown = data.getBoolean("quick_navigate_1", false);
                Log.d(TAG, "do private cmd: quick_navigate_1: "+ actiondown);
                if (actiondown) {
                    playerNotifyTeletextEvent(0);
                }
            } else if ("quick_navigate_2".equals(action) && data != null) {
                boolean actiondown = data.getBoolean("quick_navigate_2", false);
                Log.d(TAG, "do private cmd: quick_navigate_2: "+ actiondown);
                if (actiondown) {
                    playerNotifyTeletextEvent(1);
                }
            } else if ("quick_navigate_3".equals(action) && data != null) {
                boolean actiondown = data.getBoolean("quick_navigate_3", false);
                Log.d(TAG, "do private cmd: quick_navigate_3: "+ actiondown);
                if (actiondown) {
                    playerNotifyTeletextEvent(2);
                }
            } else if ("quick_navigate_4".equals(action) && data != null) {
                boolean actiondown = data.getBoolean("quick_navigate_4", false);
                Log.d(TAG, "do private cmd: quick_navigate_4: "+ actiondown);
                if (actiondown) {
                    playerNotifyTeletextEvent(3);
                }
            } else if ("previous_page".equals(action) && data != null) {
                boolean actiondown = data.getBoolean("previous_page", false);
                Log.d(TAG, "do private cmd: previous_page: "+ actiondown);
                if (actiondown) {
                    playerNotifyTeletextEvent(16);
                }
            } else if ("next_page".equals(action) && data != null) {
                boolean actiondown = data.getBoolean("next_page", false);
                Log.d(TAG, "do private cmd: next_page: "+ actiondown);
                if (actiondown) {
                    playerNotifyTeletextEvent(15);
                }
            } else if ("index_page".equals(action) && data != null) {
                boolean actiondown = data.getBoolean("index_page", false);
                Log.d(TAG, "do private cmd: index_page: "+ actiondown);
                if (actiondown) {
                    playerNotifyTeletextEvent(14);
                }
            } else if ("next_sub_page".equals(action) && data != null) {
                boolean actiondown = data.getBoolean("next_sub_page", false);
                Log.d(TAG, "do private cmd: next_sub_page: "+ actiondown);
                if (actiondown) {
                    playerNotifyTeletextEvent(17);
                }
            } else if ("previous_sub_page".equals(action) && data != null) {
                boolean actiondown = data.getBoolean("previous_sub_page", false);
                Log.d(TAG, "do private cmd: previous_sub_page: "+ actiondown);
                if (actiondown) {
                    playerNotifyTeletextEvent(18);
                }
            } else if ("back_page".equals(action) && data != null) {
                boolean actiondown = data.getBoolean("back_page", false);
                Log.d(TAG, "do private cmd: back_page: "+ actiondown);
                if (actiondown) {
                    playerNotifyTeletextEvent(19);
                }
            } else if ("forward_page".equals(action) && data != null) {
                boolean actiondown = data.getBoolean("forward_page", false);
                Log.d(TAG, "do private cmd: forward_page: "+ actiondown);
                if (actiondown) {
                    playerNotifyTeletextEvent(20);
                }
            } else if ("hold".equals(action) && data != null) {
                boolean actiondown = data.getBoolean("hold", false);
                Log.d(TAG, "do private cmd: hold: "+ actiondown);
                if (actiondown) {
                    playerNotifyTeletextEvent(21);
                }
            } else if ("reveal".equals(action) && data != null) {
                boolean actiondown = data.getBoolean("reveal", false);
                Log.d(TAG, "do private cmd: reveal: "+ actiondown);
                if (actiondown) {
                    playerNotifyTeletextEvent(22);
                }
            } else if ("clear".equals(action) && data != null) {
                boolean actiondown = data.getBoolean("clear", false);
                Log.d(TAG, "do private cmd: clear: "+ actiondown);
                if (actiondown) {
                    playerNotifyTeletextEvent(23);
                }
            } else if ("mix_video".equals(action) && data != null) {
                boolean actiondown = data.getBoolean("mix_video", false);
                Log.d(TAG, "do private cmd: mix_video: "+ actiondown);
                if (actiondown) {
                    playerNotifyTeletextEvent(24);
                }
            } else if ("double_height".equals(action) && data != null) {
                boolean actiondown = data.getBoolean("double_height", false);
                Log.d(TAG, "do private cmd: double_height: "+ actiondown);
                if (actiondown) {
                    playerNotifyTeletextEvent(25);
                }
            } else if ("double_scroll_up".equals(action) && data != null) {
                boolean actiondown = data.getBoolean("double_scroll_up", false);
                Log.d(TAG, "do private cmd: double_scroll_up: "+ actiondown);
                if (actiondown) {
                    playerNotifyTeletextEvent(26);
                }
            } else if ("double_scroll_down".equals(action) && data != null) {
                boolean actiondown = data.getBoolean("double_scroll_down", false);
                Log.d(TAG, "do private cmd: double_scroll_down: "+ actiondown);
                if (actiondown) {
                    playerNotifyTeletextEvent(27);
                }
            } else if ("timer".equals(action) && data != null) {
                boolean actiondown = data.getBoolean("timer", false);
                Log.d(TAG, "do private cmd: timer: "+ actiondown);
                if (actiondown) {
                    playerNotifyTeletextEvent(28);
                }
            }  else if ("clock".equals(action) && data != null) {
                boolean actiondown = data.getBoolean("clock", false);
                Log.d(TAG, "do private cmd: clock: "+ actiondown);
                if (actiondown) {
                    playerNotifyTeletextEvent(29);
                }
            } else if (ConstantManager.ACTION_TIF_CONTROL_OVERLAY.equals(action)) {
                boolean show = data.getBoolean(ConstantManager.KEY_TIF_OVERLAY_SHOW_STATUS, false);
                Log.d(TAG, "do private cmd:"+ ConstantManager.ACTION_TIF_CONTROL_OVERLAY + ", show:" + show);
                //not needed at the moment
                /*if (!show) {
                    if (mView != null) {
                        mView.hideOverLay();
                    };
                } else {
                    if (mView != null) {
                        mView.showOverLay();
                    };
                }*/
            } else if (ConstantManager.ACTION_TIF_BEFORE_TUNE.equals(action)) {
                Log.d(TAG, "do private ACTION_TIF_BEFORE_TUNE");
                if (mView != null) {
                    mView.showTuningImage(null);
                }
            } else if (TextUtils.equals(DataMananer.ACTION_DTV_ENABLE_AUDIO_AD, action)) {
                mAudioADAutoStart = data.getInt(DataMananer.PARA_ENABLE) == 1;
                Log.d(TAG, "do private cmd: ACTION_DTV_ENABLE_AUDIO_AD: "+ mAudioADAutoStart);
                setAdFunction(MSG_MIX_AD_SET_ASSOCIATE, mAudioADAutoStart ? 1 : 0);
                setAdFunction(MSG_MIX_AD_SWITCH_ENABLE, mAudioADAutoStart ? 1 : 0);
                setAdFunction(MSG_MIX_AD_MIX_LEVEL, mAudioADMixingLevel);
            } else if (TextUtils.equals(DataMananer.ACTION_AD_MIXING_LEVEL, action)) {
                mAudioADMixingLevel = data.getInt(DataMananer.PARA_VALUE1);
                Log.d(TAG, "do private cmd: ACTION_AD_MIXING_LEVEL: "+ mAudioADMixingLevel);
                setAdFunction(MSG_MIX_AD_MIX_LEVEL, mAudioADMixingLevel);
            } else if (TextUtils.equals(DataMananer.ACTION_AD_VOLUME_LEVEL, action)) {
                mAudioADVolume = data.getInt(DataMananer.PARA_VALUE1);
                setAdFunction(MSG_MIX_AD_SET_VOLUME, mAudioADVolume);
            } else if (TextUtils.equals(PropSettingManager.ACTON_CONTROL_TIMESHIFT, action)) {
                if (data != null) {
                    boolean status = data.getBoolean(PropSettingManager.VALUE_CONTROL_TIMESHIFT, false);
                    if (status) {
                        sendMsgTryStartTimeshift(0);
                    } else {
                        sendMsgTryStopTimeshift(0);
                    }
                }
            } else if (TextUtils.equals("delete_profile", action)) {
                String ciNumber = data.getString("ci_number");
                Log.d(TAG, "do private cmd:"+ "delete_profile" + ", ciNumber:" + ciNumber);
            } else if (TextUtils.equals("focus_info", action)) {
                boolean isProfile = data.getBoolean("is_profile");
                String ciNumber = data.getString("ci_number");
                String profileName = data.getString("profile_name");
                String ciEnterStatus = data.getString("status");
                Log.d(TAG, "do private cmd:"+ "focus_info" + ", isProfile:" + isProfile + ", ciNumber:" + ciNumber + ", profileName:" + profileName + ", ciEnterStatus = " + ciEnterStatus);
                playerNotifyCiProfileEvent(ciEnterStatus, ciNumber, profileName, "v1");
            } else if (TextUtils.equals(ConstantManager.ACTION_CI_PLUS_INFO, action)) {
                String command = data.getString(ConstantManager.CI_PLUS_COMMAND);
                if (ConstantManager.VALUE_CI_PLUS_COMMAND_SEARCH_REQUEST.equals(command)) {
                    int module = data.getInt(ConstantManager.VALUE_CI_PLUS_SEARCH_MODULE);
                    Log.d(TAG, "do private cmd:"+ action + " module = " + module);
                    doOperatorSearch(module);
                } else {
                    Log.d(TAG, "do private cmd:"+ action + " none");
                }
            }
        }

        @RequiresApi(api = Build.VERSION_CODES.M)
        public void onTimeShiftPlay(Uri uri) {
            Log.i(TAG, "onTimeShiftPlay " + uri);
            if (mHandlerThreadHandle != null) {
                mHandlerThreadHandle.removeMessages(MSG_TIMESHIFT_PLAY);
                Message mess = mHandlerThreadHandle.obtainMessage(MSG_TIMESHIFT_PLAY, 0, 0, uri);
                boolean info = mHandlerThreadHandle.sendMessage(mess);
                Log.d(TAG, "onTimeShiftPlay sendMessage " + info);
            }

        }

        public void onTimeShiftPause() {
            if (mHandlerThreadHandle != null) {
                mHandlerThreadHandle.removeMessages(MSG_TIMESHIFT_PASUE);
                Message mess = mHandlerThreadHandle.obtainMessage(MSG_TIMESHIFT_PASUE, 0, 0, null);
                boolean info = mHandlerThreadHandle.sendMessage(mess);
                Log.d(TAG, "onTimeShiftPause sendMessage " + info);
            }
        }

        public void onTimeShiftResume() {
            if (mHandlerThreadHandle != null) {
                mHandlerThreadHandle.removeMessages(MSG_TIMESHIFT_RESUME);
                Message mess = mHandlerThreadHandle.obtainMessage(MSG_TIMESHIFT_RESUME, 0, 0, null);
                boolean info = mHandlerThreadHandle.sendMessage(mess);
                Log.d(TAG, "onTimeShiftResume sendMessage " + info);
            }
        }

        public void onTimeShiftSeekTo(long timeMs) {
            Log.i(TAG, "onTimeShiftSeekTo:  " + timeMs);
            if (mHandlerThreadHandle != null) {
                mHandlerThreadHandle.removeMessages(MSG_TIMESHIFT_SEEK);
                Message mess = mHandlerThreadHandle.obtainMessage(MSG_TIMESHIFT_SEEK, 0, 0, timeMs);
                boolean info = mHandlerThreadHandle.sendMessage(mess);
                Log.d(TAG, "onTimeShiftSeekTo sendMessage " + info);
            }
        }

        @RequiresApi(api = Build.VERSION_CODES.M)
        public void onTimeShiftSetPlaybackParams(PlaybackParams params) {
            Log.i(TAG, "onTimeShiftSetPlaybackParams");
            if (mHandlerThreadHandle != null) {
                mHandlerThreadHandle.removeMessages(MSG_TIMESHIFT_PLAY_SET_PLAYBACKPARAMS);
                Message mess = mHandlerThreadHandle.obtainMessage(MSG_TIMESHIFT_PLAY_SET_PLAYBACKPARAMS, 0, 0, params);
                boolean info = mHandlerThreadHandle.sendMessage(mess);
                Log.d(TAG, "onTimeShiftSetPlaybackParams sendMessage " + info);
            }
        }

        public long onTimeShiftGetStartPosition() {
            if (timeshiftRecorderState != RecorderState.STOPPED) {
                long truncated = playerGetElapsedAndTruncated()[1];
                long diff = PropSettingManager.getStreamTimeDiff();

                if (originalStartPosition != 0 && originalStartPosition != TvInputManager.TIME_SHIFT_INVALID_TIME) {
                    startPosition = originalStartPosition + truncated + diff;
                }
                Log.i(TAG, "timeshifting. start position: " + startPosition + ", (truncated:" + truncated + ", diff:" + diff + ")ms");
            }
            Log.i(TAG, "onTimeShiftGetStartPosition startPosition:" + startPosition + ", as date = " + ConvertSettingManager.convertLongToDate(startPosition));
            return startPosition;
        }

        public long onTimeShiftGetCurrentPosition() {
            if (startPosition == 0) /* Playing back recorded program */ {
                if (playerState == PlayerState.PLAYING) {
                    long e_t_l[] = playerGetElapsedAndTruncated();
                    long length = e_t_l[2];
                    currentPosition = e_t_l[0];
                    if ((length - currentPosition) < 1000)
                        currentPosition = recordedProgram.getRecordingDurationMillis();
                    Log.i(TAG, "playing back record program. current position: " + currentPosition);
                }
            } else if (timeshifting) {
                long e_t_l[] = playerGetElapsedAndTruncated();
                long elapsed = e_t_l[0];
                long length = e_t_l[2];
                long diff = PropSettingManager.getStreamTimeDiff();

                if ((length - elapsed) < 1000 && playSpeed < 0.0)
                   currentPosition = PropSettingManager.getCurrentStreamTime(true);
                else
                   currentPosition = elapsed + originalStartPosition + diff;
                Log.i(TAG, "timeshifting. current position: " + currentPosition + ", (elapsed:" + elapsed+ ", (length:" + length +  ", (playSpeed:" + playSpeed +  ", diff:" + diff + ")ms");
            } else if (startPosition == TvInputManager.TIME_SHIFT_INVALID_TIME) {
                currentPosition = TvInputManager.TIME_SHIFT_INVALID_TIME;
                Log.i(TAG, "Invalid time. Current position: " + currentPosition);
            } else {
                if (!mIsPip) {
                    currentPosition = /*System.currentTimeMillis()*/PropSettingManager.getCurrentStreamTime(true);
                } else {
                    currentPosition = PropSettingManager.getCurrentPipStreamTime(true);
                }
                Log.i(TAG, "live tv. current position: " + currentPosition);
            }
            Log.d(TAG, "onTimeShiftGetCurrentPosition currentPosition = " + currentPosition + ", as date = " + ConvertSettingManager.convertLongToDate(currentPosition));
            return currentPosition;
        }

        private RecordedProgram getRecordedProgram(Uri uri) {
            RecordedProgram recordedProgram = null;
            Cursor cursor = null;
            try {
                if (uri != null) {
                    cursor = mContext.getContentResolver().query(uri, RecordedProgram.PROJECTION, null, null, null);
                }
                while (null != cursor && cursor.moveToNext()) {
                    recordedProgram = RecordedProgram.fromCursor(cursor);
                }
            } catch (Exception e) {
                Log.e(TAG, "getRecordedProgram query Failed = " + e.getMessage());
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
            return recordedProgram;
        }

        @Override
        public boolean onKeyDown(int keyCode, KeyEvent event) {
            boolean used;

            Log.i(TAG, "onKeyDown " + event);
            if (mDvbNetworkChangeSearchStatus) {
                Log.i(TAG, "onKeyDown skip as search action is raised");
                return true;
            }
            if (mCiAuthenticatedStatus) {
                Log.i(TAG, "onKeyDown skip as ci Authentication");
                showToast(R.string.play_ci_authentication);
                return true;
            }
            /* It's possible for a keypress to be registered before the overlay is created */
            if (mView == null) {
                used = super.onKeyDown(keyCode, event);
            }
            else {
                if (mView.handleKeyDown(keyCode, event)) {
                    used = true;
                } else if (isTeletextNeedKeyCode(keyCode) && playerIsTeletextOn()) {
                    used = true;
                } else {
                   used = super.onKeyDown(keyCode, event);
                }
            }

            return used;
        }

        @Override
        public boolean onKeyUp(int keyCode, KeyEvent event) {
            boolean used;

            Log.i(TAG, "onKeyUp " + event);
            if (mDvbNetworkChangeSearchStatus) {
                Log.i(TAG, "onKeyUp skip as search action is raised");
                return true;
            }
            if (mCiAuthenticatedStatus) {
                Log.i(TAG, "onKeyDown skip as ci Authentication");
                showToast(R.string.play_ci_authentication);
                return true;
            }
            /* It's possible for a keypress to be registered before the overlay is created */
            if (mView == null) {
                used = super.onKeyUp(keyCode, event);
            }
            else {
                if (mView.handleKeyUp(keyCode, event)) {
                    used = true;
                } else if (isTeletextNeedKeyCode(keyCode) && playerIsTeletextOn()) {
                    dealTeletextKeyCode(keyCode);
                    used = true;
                } else {
                    used = super.onKeyUp(keyCode, event);
                }
            }
            return used;
        }

        private boolean isTeletextNeedKeyCode(int keyCode) {
            return keyCode == KeyEvent.KEYCODE_BACK ||
                    keyCode == KeyEvent.KEYCODE_PROG_RED ||
                    keyCode == KeyEvent.KEYCODE_PROG_GREEN ||
                    keyCode == KeyEvent.KEYCODE_PROG_YELLOW ||
                    keyCode == KeyEvent.KEYCODE_PROG_BLUE ||
                    keyCode == KeyEvent.KEYCODE_CHANNEL_UP ||
                    keyCode == KeyEvent.KEYCODE_DPAD_UP ||
                    keyCode == KeyEvent.KEYCODE_CHANNEL_DOWN ||
                    keyCode == KeyEvent.KEYCODE_DPAD_DOWN ||
                    keyCode == KeyEvent.KEYCODE_0 ||
                    keyCode == KeyEvent.KEYCODE_1 ||
                    keyCode == KeyEvent.KEYCODE_2 ||
                    keyCode == KeyEvent.KEYCODE_3 ||
                    keyCode == KeyEvent.KEYCODE_4 ||
                    keyCode == KeyEvent.KEYCODE_5 ||
                    keyCode == KeyEvent.KEYCODE_6 ||
                    keyCode == KeyEvent.KEYCODE_7 ||
                    keyCode == KeyEvent.KEYCODE_8 ||
                    keyCode == KeyEvent.KEYCODE_9;
        }

        private void dealTeletextKeyCode(int keyCode) {
            switch (keyCode) {
                case KeyEvent.KEYCODE_BACK:
                    Log.d(TAG, "dealTeletextKeyCode close teletext");
                    playerSetTeletextOn(false, -1);
                    notifyTrackSelected(TvTrackInfo.TYPE_SUBTITLE, null);
                    if (!mTeleTextMixNormal) {
                        mTeleTextMixNormal = true;
                        mView.setTeletextMix(TTX_MODE_NORMAL);
                        playerSetRectangle(0, 0, mWinWidth, mWinHeight);
                    }
                    break;
                case KeyEvent.KEYCODE_PROG_RED:
                    Log.d(TAG, "dealTeletextKeyCode quick_navigate_1");
                    playerNotifyTeletextEvent(0);
                    break;
                case KeyEvent.KEYCODE_PROG_GREEN:
                    Log.d(TAG, "dealTeletextKeyCode quick_navigate_2");
                    playerNotifyTeletextEvent(1);
                    break;
                case KeyEvent.KEYCODE_PROG_YELLOW:
                    Log.d(TAG, "dealTeletextKeyCode quick_navigate_3");
                    playerNotifyTeletextEvent(2);
                    break;
                case KeyEvent.KEYCODE_PROG_BLUE:
                    Log.d(TAG, "dealTeletextKeyCode quick_navigate_4");
                    playerNotifyTeletextEvent(3);
                    break;
                case KeyEvent.KEYCODE_CHANNEL_UP:
                case KeyEvent.KEYCODE_DPAD_UP:
                    Log.d(TAG, "dealTeletextKeyCode previous_page");
                    playerNotifyTeletextEvent(16);
                    break;
                case KeyEvent.KEYCODE_CHANNEL_DOWN:
                case KeyEvent.KEYCODE_DPAD_DOWN:
                    Log.d(TAG, "dealTeletextKeyCode next_page");
                    playerNotifyTeletextEvent(15);
                    break;
                case KeyEvent.KEYCODE_0:
                case KeyEvent.KEYCODE_1:
                case KeyEvent.KEYCODE_2:
                case KeyEvent.KEYCODE_3:
                case KeyEvent.KEYCODE_4:
                case KeyEvent.KEYCODE_5:
                case KeyEvent.KEYCODE_6:
                case KeyEvent.KEYCODE_7:
                case KeyEvent.KEYCODE_8:
                case KeyEvent.KEYCODE_9: {
                    final int TT_EVENT_0 = 4;
                    int number = keyCode - KeyEvent.KEYCODE_0;
                    Log.d(TAG, "dealTeletextKeyCode number = " + number);
                    playerNotifyTeletextEvent(number + TT_EVENT_0);
                    break;
                }
                default:
                    break;
            }
        }

        private final DtvkitGlueClient.SignalHandler mHandler = new DtvkitGlueClient.SignalHandler() {
            @RequiresApi(api = Build.VERSION_CODES.M)
            @Override
            public void onSignal(String signal, JSONObject data) {
                Log.i(TAG, "onSignal: " + signal + " : " + data.toString() + ", index = " + mCurrentDtvkitTvInputSessionIndex + ", mIsPip = " + mIsPip);
                // TODO notifyTracksChanged(List<TvTrackInfo> tracks)
                /*if (mIsPip && (!signal.equals("PlayerStatusChanged") && !signal.equals("AppVideoPosition"))) {
                    Log.d(TAG, "PIP only need PlayerStatusChanged or AppVideoPosition");
                    return;
                }*/
                if (signal.equals("PlayerStatusChanged")) {
                    String state = "off";
                    String dvbUri = "";
                    try {
                        state = data.getString("state");
                        dvbUri= data.getString("uri");
                    } catch (JSONException ignore) {
                    }
                    Log.i(TAG, "signal: "+state);
                    /*if (mIsPip && (!signal.equals("dvblive") && !signal.equals("badsignal") && !signal.equals("scambled"))) {
                        Log.d(TAG, "PIP only need dvblive badsignal scambled");
                        return;
                    }*/
                    switch (state) {
                        case "playing":
                            String type = "dvblive";
                            try {
                                type = data.getString("type");
                            } catch (JSONException e) {
                                Log.e(TAG, e.getMessage());
                            }
                            if (type.equals("dvblive")) {
                                if (mTunedChannel != null) {
                                    if (mTunedChannel.getServiceType().equals(TvContract.Channels.SERVICE_TYPE_AUDIO)) {
                                        notifyVideoUnavailable(TvInputManager.VIDEO_UNAVAILABLE_REASON_AUDIO_ONLY);
                                    } else {
                                        notifyVideoAvailable();
                                    }
                                } else {
                                    Log.d(TAG, "on signal dvblive null mTunedChannel");
                                }
                                if (mIsPip) {
                                    Log.d(TAG, "dvblive PIP only need video status");
                                    return;
                                }
                                //update track info in message queue
                                if (mHandlerThreadHandle != null) {
                                    mHandlerThreadHandle.removeMessages(MSG_UPDATE_TRACKS_AND_SELECT);
                                    Message msg = mHandlerThreadHandle.obtainMessage(MSG_UPDATE_TRACKS_AND_SELECT);
                                    msg.arg1 = 0;
                                    mHandlerThreadHandle.sendMessageDelayed(msg, 0);
                                }
                                if (mTunedChannel != null && mTunedChannel.getServiceType().equals(TvContract.Channels.SERVICE_TYPE_AUDIO_VIDEO)) {
                                    if (mHandlerThreadHandle != null) {
                                        mHandlerThreadHandle.removeMessages(MSG_CHECK_RESOLUTION);
                                        mHandlerThreadHandle.sendEmptyMessageDelayed(MSG_CHECK_RESOLUTION, MSG_CHECK_RESOLUTION_PERIOD);//check resolution later
                                    }
                                }
                                if (mHandlerThreadHandle != null) {
                                    mHandlerThreadHandle.removeMessages(MSG_GET_SIGNAL_STRENGTH);
                                    mHandlerThreadHandle.sendEmptyMessageDelayed(MSG_GET_SIGNAL_STRENGTH, MSG_GET_SIGNAL_STRENGTH_PERIOD);//check signal per 1s
                                    mHandlerThreadHandle.removeMessages(MSG_UPDATE_TRACKINFO);
                                    mHandlerThreadHandle.sendEmptyMessageDelayed(MSG_UPDATE_TRACKINFO, MSG_UPDATE_TRACKINFO_DELAY);
                                }
                                if (!getFeatureSupportManualTimeshift()) {
                                    monitorTimeshiftRecordingPathAndTryRestart(true, true, true);
                                }
                            }
                            else if (type.equals("dvbrecording")) {
                                setBlockMute(false);
                                startPosition = originalStartPosition = 0; // start position is always 0 when playing back recorded program
                                currentPosition = playerGetElapsedAndTruncated(data)[0];
                                Log.i(TAG, "dvbrecording currentPosition = " + currentPosition + "as date = " + ConvertSettingManager.convertLongToDate(startPosition));
                                notifyTimeShiftStatusChanged(TvInputManager.TIME_SHIFT_STATUS_AVAILABLE);
                                //update track info in message queue
                                if (mHandlerThreadHandle != null) {
                                    mHandlerThreadHandle.removeMessages(MSG_UPDATE_TRACKS_AND_SELECT);
                                    Message msg = mHandlerThreadHandle.obtainMessage(MSG_UPDATE_TRACKS_AND_SELECT);
                                    msg.arg1 = 1;
                                    mHandlerThreadHandle.sendMessageDelayed(msg, 0);
                                }
                            }
                            else if (type.equals("dvbtimeshifting")) {
                                if (mTunedChannel != null) {
                                    if (mTunedChannel.getServiceType().equals(TvContract.Channels.SERVICE_TYPE_AUDIO)) {
                                        notifyVideoUnavailable(TvInputManager.VIDEO_UNAVAILABLE_REASON_AUDIO_ONLY);
                                    } else {
                                        notifyVideoAvailable();
                                    }
                                } else {
                                    Log.d(TAG, "on signal dvbtimeshifting null mTunedChannel");
                                }
                            }
                            playerState = PlayerState.PLAYING;
                            break;
                        case "blocked":
                            String Rating = "";
                            try {
                                Rating = String.format("DVB_%d", data.getInt("rating"));
                            } catch (JSONException ignore) {
                                Log.e(TAG, ignore.getMessage());
                            }
                            notifyContentBlocked(TvContentRating.createRating("com.android.tv", "DVB", Rating));
                            break;
                        case "badsignal":
                            notifyVideoUnavailable(TvInputManager.VIDEO_UNAVAILABLE_REASON_WEAK_SIGNAL);
                            writeSysFs("/sys/class/video/disable_video", "2");
                            if (mIsPip) {
                                Log.d(TAG, "badsignal PIP only need video status");
                                return;
                            }
                            boolean isTeleOn = playerIsTeletextOn();
                            if (isTeleOn) {
                                playerSetTeletextOn(false, -1);
                                notifyTrackSelected(TvTrackInfo.TYPE_SUBTITLE, null);
                                Log.d(TAG, "close teletext when badsignal received");
                            }
                            if (mMainHandle != null) {
                                mMainHandle.removeMessages(MSG_SET_TELETEXT_MIX_NORMAL);
                                mMainHandle.sendEmptyMessage(MSG_SET_TELETEXT_MIX_NORMAL);
                            }
                            break;
                        case "off":
                            if (mIsPip) {
                                return;
                            }
                            if (timeshiftRecorderState != RecorderState.STOPPED) {
                                removeScheduleTimeshiftRecordingTask();
                                scheduleTimeshiftRecording = false;
                                playerStopTimeshiftRecording(false);
                                timeshiftRecorderState = RecorderState.STOPPED;
                                notifyTimeShiftStatusChanged(TvInputManager.TIME_SHIFT_STATUS_UNAVAILABLE);
                            }
                            playerState = PlayerState.STOPPED;
                            dvrSubtitleFlag = 0;
                            if (recordedProgram != null) {
                               /*trigger the playback exit*/
                               currentPosition = recordedProgram.getRecordingDurationMillis();
                            }

                            break;
                        case "starting":
                           boolean isAv = true;
                           try {
                               isAv = data.getJSONObject("content").getBoolean("is_av");
                           } catch (JSONException e) {
                               Log.d(TAG, "starting is_av JSONException = " + e.getMessage());
                               return;
                           }
                           if (mIsPip) {
                              Log.d(TAG, "starting no need to start mheg");
                              if (!isAv) {
                                  Log.d(TAG, "starting not av program and notify to pip view");
                                  Bundle pipTuningNext = new Bundle();
                                  pipTuningNext.putString(ConstantManager.KEY_PIP_ACTION, ConstantManager.VALUE_PIP_ACTION_TUNE_NEXT);
                                  notifySessionEvent(ConstantManager.EVENT_PIP_INFO, pipTuningNext);
                                  notifyChannelRetuned(null);
                              }
                              return;
                           } else {
                               Log.i(TAG, "starting mhegStart " + dvbUri);
                               if (mHandlerThreadHandle != null) {
                                  mHandlerThreadHandle.obtainMessage(MSG_START_MHEG5, 0/*mhegSsupend*/, 0, dvbUri).sendToTarget();
                               }
                           }
                           if (mTunedChannel != null) {
                                if (mTunedChannel.getServiceType().equals(TvContract.Channels.SERVICE_TYPE_AUDIO)) {
                                    notifyVideoUnavailable(TvInputManager.VIDEO_UNAVAILABLE_REASON_AUDIO_ONLY);
                                } else {
                                    notifyVideoAvailable();
                                }
                            } else {
                                Log.d(TAG, "on signal starting null mTunedChannel");
                            }
                           break;
                        case "scambled":
                            /*notify scambled*/
                            Log.i(TAG, "** scambled **");
                            if (mMainHandle != null) {
                                mMainHandle.sendEmptyMessage(MSG_SHOW_SCAMBLEDTEXT);
                            } else {
                                Log.d(TAG, "mMainHandle is null");
                            }
                            break;
                        default:
                            Log.i(TAG, "Unhandled state: " + state);
                            break;
                    }
                } else if (signal.equals("PlayerTimeshiftRecorderStatusChanged")) {
                    switch (playerGetTimeshiftRecorderState(data)) {
                        case "recording":
                            timeshiftRecorderState = RecorderState.RECORDING;
                            startPosition = /*System.currentTimeMillis()*/PropSettingManager.getCurrentStreamTime(true);
                            originalStartPosition = PropSettingManager.getCurrentStreamTime(false);//keep the original time
                            Log.i(TAG, "recording originalStartPosition as date = " + ConvertSettingManager.convertLongToDate(originalStartPosition) + ", startPosition = " + ConvertSettingManager.convertLongToDate(startPosition));
                            notifyTimeShiftStatusChanged(TvInputManager.TIME_SHIFT_STATUS_AVAILABLE);
                            break;
                        case "off":
                            timeshiftRecorderState = RecorderState.STOPPED;
                            startPosition = originalStartPosition = TvInputManager.TIME_SHIFT_INVALID_TIME;
                            timeshifting = false;
                            notifyTimeShiftStatusChanged(TvInputManager.TIME_SHIFT_STATUS_UNAVAILABLE);
                            playerSetSubtitlesOn(false);
                            playerStopTeletext();
                            dvrSubtitleFlag = 0;
                            break;
                    }
                } else if (signal.equals("RecordingStatusChanged")) {
                    JSONArray activeRecordings = recordingGetActiveRecordings(data);

                    if (activeRecordings != null && activeRecordings.length() < numRecorders &&
                            timeshiftRecorderState == RecorderState.STOPPED && scheduleTimeshiftRecording) {
                        timeshiftAvailable.setYes();
                        /*no scheduling, taken over by MSG_CHECK_REC_PATH*/
                        /*if (!getFeatureSupportManualTimeshift()) {
                            scheduleTimeshiftRecordingTask();
                        }*/
                    }

                    if (checkActiveRecordings(activeRecordings,
                        new Predicate<JSONObject> () {
                            @Override
                            public boolean test(JSONObject recording) {
                                if (TextUtils.equals(recording.optString("serviceuri", "null"), getChannelInternalDvbUri(mTunedChannel))
                                    && getFeatureSupportRecordAVServiceOnly()
                                    && !recording.optBoolean("is_av", true))
                                {
                                    return true;
                                }
                                return false;
                            }
                        }))
                    {
                        Log.i(TAG, "timeshift stopped due to non-AV Service, feature rec_av_only enabled.");

                        Bundle event = new Bundle();
                        event.putString(ConstantManager.KEY_INFO, "not allowed to record a non-av service");
                        notifySessionEvent(ConstantManager.EVENT_RESOURCE_BUSY, event);
                        /*notifyError(TvInputManager.RECORDING_ERROR_RESOURCE_BUSY);*/

                        /*timeshiftAvailable will be disabled further in this tune*/
                        timeshiftAvailable.disable();

                        boolean returnToLive = timeshifting;
                        Log.i(TAG, "stopping timeshift [return live:" + returnToLive + "]");
                        timeshiftRecorderState = RecorderState.STOPPED;
                        scheduleTimeshiftRecording = false;
                        timeshifting = false;
                        playerStopTimeshiftRecording(returnToLive);
                    }
                }
                else if (signal.equals("DvbUpdatedEventPeriods"))
                {
                    Log.i(TAG, "DvbUpdatedEventPeriods");
                    ComponentName sync = new ComponentName(mContext, DtvkitEpgSync.class);
                    checkAndUpdateLcn();
                    EpgSyncJobService.requestImmediateSync(mContext, mInputId, false, sync);
                }
                else if (signal.equals("DvbUpdatedEventNow"))
                {
                    Log.i(TAG, "DvbUpdatedEventNow");
                    ComponentName sync = new ComponentName(mContext, DtvkitEpgSync.class);
                    checkAndUpdateLcn();
                    EpgSyncJobService.requestImmediateSync(mContext, mInputId, true, sync);
                    //notify update parent contrl
                    if (mHandlerThreadHandle != null) {
                        mHandlerThreadHandle.removeMessages(MSG_CHECK_PARENTAL_CONTROL);
                        mHandlerThreadHandle.sendEmptyMessageDelayed(MSG_CHECK_PARENTAL_CONTROL, MSG_CHECK_PARENTAL_CONTROL_PERIOD);
                    }
                }
                else if (signal.equals("DvbUpdatedChannel"))
                {
                    Log.i(TAG, "DvbUpdatedChannel");
                    ComponentName sync = new ComponentName(mContext, DtvkitEpgSync.class);
                    checkAndUpdateLcn();
                    EpgSyncJobService.requestImmediateSync(mContext, mInputId, false, true, sync);
                }
                else if (signal.equals("CiplusUpdateService"))
                {
                    //update CiOpSearchRequest search result
                    Log.i(TAG, "CiplusUpdateService");
                    ComponentName sync = new ComponentName(mContext, DtvkitEpgSync.class);
                    EpgSyncJobService.requestImmediateSync(mContext, mInputId, false, false, sync);
                    //notify update result after 3s
                    if (mHandlerThreadHandle != null) {
                        mHandlerThreadHandle.removeMessages(MSG_CI_UPDATE_PROFILE_OVER);
                        mHandlerThreadHandle.sendEmptyMessageDelayed(MSG_CI_UPDATE_PROFILE_OVER, MSG_CI_UPDATE_PROFILE_OVER_DELAY);
                    }
                }
                else if (signal.equals("CiTuneServiceInfo"))
                {
                    //tuned by dtvkit directly and then notify app
                    int s_id = 0, t_id = 0, onet_id = 0;
                    String tune_type = "";
                    try {
                        s_id = data.getInt("s_id");
                        t_id = data.getInt("t_id");
                        onet_id = data.getInt("onet_id");
                        tune_type = data.getString("tune_type");
                    } catch (JSONException e) {
                        Log.e(TAG, "CiTuneServiceInfo JSONException = " + e.getMessage());
                    }
                    if (ConstantManager.VALUE_CI_PLUS_TUNE_TYPE_SERVICE.equals(tune_type)) {
                        Log.d(TAG, "CiTuneServiceInfo s_id " + s_id + " t_id " + t_id + " onet_id " + onet_id + " tune_type " + tune_type);
                        Channel foundChannelById = TvContractUtils.getChannelByNetworkTransportServiceId(mContext.getContentResolver(), onet_id, t_id, s_id);
                        if (foundChannelById != null) {
                            Bundle channelBundle = new Bundle();
                            channelBundle.putString(ConstantManager.CI_PLUS_COMMAND, ConstantManager.VALUE_CI_PLUS_COMMAND_HOST_CONTROL);
                            channelBundle.putString(ConstantManager.VALUE_CI_PLUS_TUNE_TYPE, ConstantManager.VALUE_CI_PLUS_TUNE_TYPE_SERVICE);
                            channelBundle.putLong(ConstantManager.VALUE_CI_PLUS_CHANNEL, foundChannelById.getId());
                            sendBundleToAppByTif(ConstantManager.ACTION_CI_PLUS_INFO, channelBundle);
                        } else {
                            Log.d(TAG, "CiTuneServiceInfo hasn't found such channel");
                        }
                    } else if (ConstantManager.VALUE_CI_PLUS_TUNE_TYPE_TRANSPORT.equals(tune_type)) {
                        Log.d(TAG, "CiTuneServiceInfo s_id transport_tune");
                        Bundle channelBundle = new Bundle();
                        channelBundle.putString(ConstantManager.CI_PLUS_COMMAND, ConstantManager.VALUE_CI_PLUS_COMMAND_HOST_CONTROL);
                        channelBundle.putString(ConstantManager.VALUE_CI_PLUS_TUNE_TYPE, ConstantManager.VALUE_CI_PLUS_TUNE_TYPE_TRANSPORT);
                        channelBundle.putLong(ConstantManager.VALUE_CI_PLUS_CHANNEL, -1);
                        sendBundleToAppByTif(ConstantManager.ACTION_CI_PLUS_INFO, channelBundle);
                    }
                }
                else if (signal.equals("CiOpSearchRequest")) {
                    //tell app to search related module
                    try {
                        int module = data.getInt("data");
                        Log.d(TAG, "Ci operator request, need yes/no. module no. " + module);
                        if (PropSettingManager.getBoolean(PropSettingManager.CI_PROFILE_SEARCH_TEST, false)) {
                            if (mMainHandle != null) {
                                mMainHandle.removeMessages(MSG_CI_UPDATE_PROFILE_CONFIRM);
                                mMainHandle.sendMessageDelayed(mMainHandle.obtainMessage(MSG_CI_UPDATE_PROFILE_CONFIRM, module, 0), MSG_SHOW_STREAM_CHANGE_DELAY);
                            }
                        } else {
                            Bundle searchBundle = new Bundle();
                            searchBundle.putString(ConstantManager.CI_PLUS_COMMAND, ConstantManager.VALUE_CI_PLUS_COMMAND_SEARCH_REQUEST);
                            searchBundle.putInt(ConstantManager.VALUE_CI_PLUS_SEARCH_MODULE, module);
                            sendBundleToAppByTif(ConstantManager.ACTION_CI_PLUS_INFO, searchBundle);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "CiOpSearchRequest Exception = " + e.getMessage());
                    }
                }
                else if (signal.equals("CiOpSearchFinished")) {
                    // only a status and skip it for the moment
                    try {
                        Log.d(TAG, "Ci operator search has finished");
                        Bundle finishBundle = new Bundle();
                        finishBundle.putString(ConstantManager.CI_PLUS_COMMAND, ConstantManager.VALUE_CI_PLUS_COMMAND_SEARCH_FINISHED);
                        sendBundleToAppByTif(ConstantManager.ACTION_CI_PLUS_INFO, finishBundle);
                    } catch (Exception e) {
                        Log.e(TAG, "CiOpSearchFinished Exception = " + e.getMessage());
                    }
                }
                else if (signal.equals("IgnoreUserInput")) {
                    //tell app to disable key event
                    try {
                        String event = data.getString("data");
                        Log.d(TAG, "System is doing " + event + " can not handle user input");
                        Bundle inputBundle = new Bundle();
                        inputBundle.putString(ConstantManager.CI_PLUS_COMMAND, ConstantManager.VALUE_CI_PLUS_COMMAND_IGNORE_INPUT);
                        inputBundle.putString(ConstantManager.VALUE_CI_PLUS_EVENT_DETAIL, event);
                        sendBundleToAppByTif(ConstantManager.ACTION_CI_PLUS_INFO, inputBundle);
                        mCiAuthenticatedStatus = true;
                    } catch (Exception e) {
                        Log.d(TAG, "IgnoreUserInput Exception = " + e.getMessage());
                    }
                }
                else if (signal.equals("ReceiveUserInput")) {
                    //tell app to key event can work normally
                    try {
                        Log.d(TAG, "Work complete, input can received now");
                        Bundle inputBundle = new Bundle();
                        inputBundle.putString(ConstantManager.CI_PLUS_COMMAND, ConstantManager.VALUE_CI_PLUS_COMMAND_RECEIVE_INPUT);
                        sendBundleToAppByTif(ConstantManager.ACTION_CI_PLUS_INFO, inputBundle);
                        mCiAuthenticatedStatus = false;
                    } catch (Exception e) {
                        Log.d(TAG, "IgnoreUserInput Exception = " + e.getMessage());
                    }
                }
                else if (signal.equals("DvbNetworkChange") || signal.equals("DvbUpdatedService"))
                {
                    Log.i(TAG, "DvbNetworkChange or DvbUpdatedService");
                    //currently support dvbc dvbt dvbt2 only
                    String channelSignalType = null;
                    if (mTunedChannel != null) {
                        try {
                            channelSignalType = mTunedChannel.getInternalProviderData().get("channel_signal_type").toString();
                        } catch (Exception e) {
                            Log.i(TAG, "DvbNetworkChange or DvbUpdatedService get channel_signal_type Exception " + e.getMessage());
                        }
                    }
                    if (!TextUtils.equals(channelSignalType, Channel.FIXED_SIGNAL_TYPE_DVBC) && !TextUtils.equals(channelSignalType, Channel.FIXED_SIGNAL_TYPE_DVBT) && !TextUtils.equals(channelSignalType, Channel.FIXED_SIGNAL_TYPE_DVBT2)) {
                        Log.d(TAG, "DvbNetworkChange or DvbUpdatedService not dvbc or dvbt and is " + channelSignalType);
                        return;
                    }
                    boolean needUpdate = false;
                    if (mIsPip && !mDvbNetworkChangeSearchStatus) {
                        mDvbNetworkChangeSearchStatus = true;
                        DtvkitTvInputSession mainSession = getMainTunerSession();
                        mPipDvbChannel = DtvkitTvInputSession.this.mTunedChannel;
                        if (mainSession != null) {
                            mMainDvbChannel = mainSession.mTunedChannel;
                            sendDoReleaseSpecifiedSessionMessage(mainSession, true, false);
                        } else {
                            Log.i(TAG, "DvbNetworkChange or DvbUpdatedService no mainSession");
                        }
                        needUpdate = true;
                    } else if ((!mIsPip && !mDvbNetworkChangeSearchStatus)) {
                        mDvbNetworkChangeSearchStatus = true;
                        DtvkitTvInputSession pipSession = getPipTunerSession();
                        mMainDvbChannel = DtvkitTvInputSession.this.mTunedChannel;
                        if (pipSession != null) {
                            mPipDvbChannel = pipSession.mTunedChannel;
                            sendDoReleaseSpecifiedSessionMessage(pipSession, true, false);
                        } else {
                            Log.i(TAG, "DvbNetworkChange or DvbUpdatedService no pipSession");
                        }
                        needUpdate = true;
                    }
                    if (needUpdate) {
                        sendDoReleaseSpecifiedSessionMessage(DtvkitTvInputSession.this, true, true);
                    }
                }
                else if (signal.equals("DvbUpdatedChannelData"))
                {
                    Log.i(TAG, "DvbUpdatedChannelData");
                    List<TvTrackInfo> tracks = playerGetTracks(mTunedChannel, false);
                    if (!tracks.equals(mTunedTracks)) {
                        mTunedTracks = tracks;
                        notifyTracksChanged(mTunedTracks);
                    }
                    Log.i(TAG, "audio track selected: " + playerGetSelectedAudioTrack());
                    notifyTrackSelected(TvTrackInfo.TYPE_AUDIO, Integer.toString(playerGetSelectedAudioTrack()));
                    initSubtitleOrTeletextIfNeed();
                    //check trackinfo update
                    if (mHandlerThreadHandle != null) {
                        //mHandlerThreadHandle.removeMessages(MSG_UPDATE_TRACKINFO);
                        mHandlerThreadHandle.sendEmptyMessageDelayed(MSG_UPDATE_TRACKINFO, MSG_UPDATE_TRACKINFO_DELAY);
                    }
                }
                else if (signal.equals("MhegAppStarted"))
                {
                   Log.i(TAG, "MhegAppStarted");
                   if (mTunedChannel != null) {
                        if (mTunedChannel.getServiceType().equals(TvContract.Channels.SERVICE_TYPE_AUDIO)) {
                            notifyVideoUnavailable(TvInputManager.VIDEO_UNAVAILABLE_REASON_AUDIO_ONLY);
                        } else {
                            notifyVideoAvailable();
                        }
                    } else {
                        Log.d(TAG, "on signal MhegAppStarted null mTunedChannel");
                    }
                }
                else if (signal.equals("AppVideoPosition"))
                {
                   Log.i(TAG, "AppVideoPosition");
                   SystemControlManager SysContManager = SystemControlManager.getInstance();
                   int UiSettingMode = SysContManager.GetDisplayMode(SystemControlManager.SourceInput.XXXX.toInt());
                   int left,top,right,bottom;
                   left = 0;
                   top = 0;
                   right = 1920;
                   bottom = 1080;
                   int voff0, hoff0, voff1, hoff1;
                   voff0 = 0;
                   hoff0 = 0;
                   voff1 = 0;
                   hoff1 = 0;
                   try {
                      left = data.getInt("left");
                      top = data.getInt("top");
                      right = data.getInt("right");
                      bottom = data.getInt("bottom");
                      voff0 = data.getInt("crop-voff0");
                      hoff0 = data.getInt("crop-hoff0");
                      voff1 = data.getInt("crop-voff1");
                      hoff1 = data.getInt("crop-hoff1");
                   } catch (JSONException e) {
                      Log.e(TAG, e.getMessage());
                   }
                   if (mHandlerThreadHandle != null) {
                       mHandlerThreadHandle.sendEmptyMessageDelayed(MSG_CHECK_RESOLUTION, MSG_CHECK_RESOLUTION_PERIOD);
                   }
                   //add for pip setting
                   if (!mIsPip/*mIsMain*/) {
                       String crop = new StringBuilder()
                           .append(voff0).append(" ")
                           .append(hoff0).append(" ")
                           .append(voff1).append(" ")
                           .append(hoff1).toString();

                           if (UiSettingMode != 6) {//not afd
                               Log.i(TAG, "Not AFD mode!");
                           } else {
                               Log.d(TAG, "AppVideoPosition crop:("+crop+")");
                               SysContManager.writeSysFs("/sys/class/video/crop", crop);
                           }
                       Log.d(TAG, "AppVideoPosition layoutSurface("+left+","+top+","+right+","+bottom+")(LTRB)");
                       layoutSurface(left,top,right,bottom);
                       m_surface_left = left;
                       m_surface_right = right;
                       m_surface_top = top;
                       m_surface_bottom = bottom;
                       if (mHandlerThreadHandle != null) {
                          mHandlerThreadHandle.removeMessages(MSG_ENABLE_VIDEO);
                          mHandlerThreadHandle.sendEmptyMessageDelayed(MSG_ENABLE_VIDEO, 40);
                       }
                   }
                }
                else if (signal.equals("ServiceRetuned"))
                {
                   String dvbUri = "";
                   Channel channel;
                   Uri retuneUri;
                   boolean found = false;
                   int i;
                   long id=0;
                   try {
                      dvbUri= data.getString("uri");
                   } catch (JSONException ignore) {
                   }
                   Log.i(TAG, "ServiceRetuned " + dvbUri);
                   //find the channel URI that matches the dvb uri of the retune
                   for (i = 0;i < mChannels.size();i++)
                   {
                      channel = mChannels.get(mChannels.keyAt(i));
                      if (dvbUri.equals(getChannelInternalDvbUri(channel))) {
                         found = true;
                         id = mChannels.keyAt(i);
                         break;
                      }
                   }
                   if (found)
                   {
                      //rebuild the Channel URI from the current channel + the new ID
                      retuneUri = Uri.parse("content://android.media.tv/channel");
                      retuneUri = ContentUris.withAppendedId(retuneUri,id);
                      Log.i(TAG, "Retuning to " + retuneUri);

                      if (mHandlerThreadHandle != null) {
                          mHandlerThreadHandle.obtainMessage(MSG_ON_TUNE, 1/*mhegTune*/, 0, retuneUri).sendToTarget();
                      }
                   }
                   else
                   {
                      //if we couldn't find the channel uri for some reason,
                      // try restarting MHEG on the new service anyway
                      //mhegSuspend();
                      //mhegStartService(dvbUri);
                      //dealt in message queue
                      if (mHandlerThreadHandle != null) {
                          mHandlerThreadHandle.obtainMessage(MSG_START_MHEG5, 1/*mhegSsupend*/, 0, dvbUri).sendToTarget();
                      }
                   }
                }
                else if (signal.equals("RecordingDiskFull"))
                {
                    /*free disk space excceds the property's setting*/
                    Bundle event = new Bundle();
                    event.putString(ConstantManager.KEY_INFO, "Stop timeshift due to insufficient storage");
                    notifySessionEvent(ConstantManager.EVENT_RESOURCE_BUSY, event);
                }
                else if (signal.equals("tt_mix_separate"))
                {
                    mMainHandle.sendEmptyMessage(MSG_SET_TELETEXT_MIX_SEPARATE);
                }
                else if (signal.equals("tt_mix_normal"))
                {
                    mMainHandle.sendEmptyMessage(MSG_SET_TELETEXT_MIX_NORMAL);
                }
                else if (signal.equals("SubtitleOpened"))
                {
                    mMainHandle.sendEmptyMessageDelayed(MSG_EVENT_SUBTITLE_OPENED, 2000);
                }
            }
        };

        protected static final int MSG_ON_TUNE = 1;
        protected static final int MSG_CHECK_RESOLUTION = 2;
        protected static final int MSG_CHECK_PARENTAL_CONTROL = 3;
        protected static final int MSG_BLOCK_MUTE_OR_UNMUTE = 4;
        protected static final int MSG_SET_SURFACE = 5;
        protected static final int MSG_DO_RELEASE = 6;
        protected static final int MSG_RELEASE_WORK_THREAD = 7;
        protected static final int MSG_GET_SIGNAL_STRENGTH = 8;
        protected static final int MSG_UPDATE_TRACKINFO = 9;
        protected static final int MSG_ENABLE_VIDEO = 10;
        protected static final int MSG_SET_UNBLOCK = 11;
        protected static final int MSG_CHECK_REC_PATH = 12;
        protected static final int MSG_SEND_DISPLAY_STREAM_CHANGE_DIALOG = 13;
        protected static final int MSG_TRY_START_TIMESHIFT = 14;
        protected static final int MSG_UPDATE_TRACKS_AND_SELECT = 15;
        protected static final int MSG_SELECT_TRACK = 16;
        protected static final int MSG_DO_RELEASE_SPECIFIELD_SESSION = 17;
        protected static final int MSG_TRY_STOP_TIMESHIFT = 18;

        //audio ad
        public static final int MSG_MIX_AD_DUAL_SUPPORT = 20;
        public static final int MSG_MIX_AD_MIX_SUPPORT = 21;
        public static final int MSG_MIX_AD_MIX_LEVEL = 22;
        public static final int MSG_MIX_AD_SET_MAIN = 23;
        public static final int MSG_MIX_AD_SET_ASSOCIATE = 24;
        public static final int MSG_MIX_AD_SWITCH_ENABLE = 25;
        public static final int MSG_MIX_AD_SET_VOLUME = 26;

        //timeshift
        protected static final int MSG_TIMESHIFT_PLAY = 30;
        protected static final int MSG_TIMESHIFT_RESUME = 31;
        protected static final int MSG_TIMESHIFT_PASUE = 32;
        protected static final int MSG_TIMESHIFT_SEEK = 33;
        protected static final int MSG_CHECK_REC_PATH_DIRECTLY = 34;
        protected static final int MSG_SCHEDULE_TIMESHIFT_RECORDING_TASK = 35;
        protected static final int MSG_TIMESHIFT_PLAY_SET_PLAYBACKPARAMS = 36;

        //mheg5
        protected static final int MSG_START_MHEG5 = 40;
        protected static final int MSG_STOP_MHEG5 = 41;

        //ci plus update
        protected static final int MSG_CI_UPDATE_PROFILE_OVER = 50;
        protected static final int MSG_CI_UPDATE_PROFILE_CONFIRM = 51;
        protected static final int MSG_CI_UPDATE_PROFILE_OVER_DELAY = 3000;//3S

        protected static final int MSG_CHECK_RESOLUTION_PERIOD = 1000;//MS
        protected static final int MSG_UPDATE_TRACKINFO_DELAY = 2000;//MS
        protected static final int MSG_CHECK_PARENTAL_CONTROL_PERIOD = 2000;//MS
        protected static final int MSG_BLOCK_MUTE_OR_UNMUTE_PERIOD = 100;//MS
        protected static final int MSG_GET_SIGNAL_STRENGTH_PERIOD = 1000;//MS
        protected static final int MSG_CHECK_REC_PATH_PERIOD = 1000;//MS
        protected static final int MSG_SHOW_STREAM_CHANGE_DELAY = 500;//MS

        protected static final int MSG_MAIN_HANDLE_DESTROY_OVERLAY = 1;
        protected static final int MSG_SHOW_SCAMBLEDTEXT = 2;
        protected static final int MSG_HIDE_SCAMBLEDTEXT = 3;
        protected static final int MSG_DISPLAY_STREAM_CHANGE_DIALOG = 4;
        protected static final int MSG_SUBTITLE_SHOW_CLOSED_CAPTION = 5;
        protected static final int MSG_SET_TELETEXT_MIX_NORMAL = 6;
        protected static final int MSG_SET_TELETEXT_MIX_TRANSPARENT = 7;
        protected static final int MSG_SET_TELETEXT_MIX_SEPARATE = 8;
        protected static final int MSG_FILL_SURFACE = 9;
        protected static final int MSG_EVENT_SUBTITLE_OPENED = 10;

        protected static final int MSG_SHOW_TUNING_IMAGE = 20;
        protected static final int MSG_HIDE_TUNING_IMAGE = 21;

        protected void initWorkThread() {
            Log.d(TAG, "initWorkThread");
            mLivingHandlerThread = new HandlerThread("DtvkitTvInputSession " + mCurrentDtvkitTvInputSessionIndex);
            mLivingHandlerThread.start();
            mHandlerThreadHandle = new Handler(mLivingHandlerThread.getLooper(), new Handler.Callback() {
                @Override
                public boolean handleMessage(Message msg) {
                    Log.d(TAG, "mHandlerThreadHandle [[[:" + msg.what + ", sessionIndex = " + mCurrentDtvkitTvInputSessionIndex);
                    if (mReleaseHandleMessage) {
                        Log.d(TAG, "mReleaseHandleMessage, and handle message stopped. index = " + mCurrentDtvkitTvInputSessionIndex);
                        return true;
                    }
                    switch (msg.what) {
                        case MSG_ON_TUNE:
                            Uri channelUri = (Uri)msg.obj;
                            boolean mhegTune = msg.arg1 == 0 ? false : true;
                            if (channelUri != null) {
                                onTuneByHandlerThreadHandle(channelUri, mhegTune);
                            }
                            break;
                        case MSG_CHECK_RESOLUTION:
                            if (!checkRealTimeResolution()) {
                                if (mHandlerThreadHandle != null)
                                    mHandlerThreadHandle.sendEmptyMessageDelayed(MSG_CHECK_RESOLUTION, MSG_CHECK_RESOLUTION_PERIOD);
                            }
                            break;
                        case MSG_CHECK_PARENTAL_CONTROL:
                            updateParentalControlExt();
                            if (mHandlerThreadHandle != null) {
                                mHandlerThreadHandle.removeMessages(MSG_CHECK_PARENTAL_CONTROL);
                                mHandlerThreadHandle.sendEmptyMessageDelayed(MSG_CHECK_PARENTAL_CONTROL, MSG_CHECK_PARENTAL_CONTROL_PERIOD);
                            }
                            break;
                        case MSG_BLOCK_MUTE_OR_UNMUTE:
                            boolean mute = msg.arg1 == 0 ? false : true;
                            setBlockMute(mute);
                            break;
                        case MSG_SET_SURFACE:
                            doSetSurface((Map<String, Object>)msg.obj);
                            break;
                        case MSG_DO_RELEASE:
                            doRelease(false, false);
                            break;
                        case MSG_RELEASE_WORK_THREAD:
                            finalReleaseWorkThread(false, false);
                            break;
                        case MSG_GET_SIGNAL_STRENGTH:
                            sendCurrentSignalInfomation();
                            if (mHandlerThreadHandle != null) {
                                mHandlerThreadHandle.removeMessages(MSG_GET_SIGNAL_STRENGTH);
                                mHandlerThreadHandle.sendEmptyMessageDelayed(MSG_GET_SIGNAL_STRENGTH, MSG_GET_SIGNAL_STRENGTH_PERIOD);//check signal per 1s
                            }
                            break;
                        case MSG_UPDATE_TRACKINFO:
                            if (!checkTrackInfoUpdate()) {
                                if (mHandlerThreadHandle != null) {
                                    mHandlerThreadHandle.removeMessages(MSG_UPDATE_TRACKINFO);
                                    mHandlerThreadHandle.sendEmptyMessageDelayed(MSG_UPDATE_TRACKINFO, MSG_UPDATE_TRACKINFO_DELAY);
                                }
                            }
                            break;
                        case MSG_ENABLE_VIDEO:
                             writeSysFs("/sys/class/video/video_global_output", "1");
                            break;
                        case MSG_SET_UNBLOCK:
                            if (msg.obj != null && msg.obj instanceof TvContentRating) {
                                setUnBlock((TvContentRating)msg.obj);
                            }
                            break;
                        case MSG_CHECK_REC_PATH:
                            if (resetRecordingPath() || (msg.arg2 == 1)/*start*/) {
                                /* path changed */
                                tryStartTimeshifting();
                            }
                            monitorTimeshiftRecordingPathAndTryRestart(true, false, false);
                            break;
                        case MSG_CHECK_REC_PATH_DIRECTLY:
                            //avoid message removed in some case
                            resetRecordingPath();
                            tryStartTimeshifting();
                            monitorTimeshiftRecordingPathAndTryRestart(true, false, false);
                            break;
                        case MSG_SEND_DISPLAY_STREAM_CHANGE_DIALOG:
                            if (mMainHandle != null) {
                                mMainHandle.removeMessages(MSG_DISPLAY_STREAM_CHANGE_DIALOG);
                                mMainHandle.sendMessage(mMainHandle.obtainMessage(MSG_DISPLAY_STREAM_CHANGE_DIALOG, (int)msg.arg1, 0));
                            }
                            break;
                        case MSG_TRY_START_TIMESHIFT:
                            resetRecordingPath();
                            tryStartTimeshifting();
                            break;
                        case MSG_TRY_STOP_TIMESHIFT:
                            tryStopTimeshifting();
                            break;
                        case MSG_UPDATE_TRACKS_AND_SELECT:
                            updateTrackAndSelect(msg.arg1);
                            break;
                        case MSG_SELECT_TRACK:
                            doSelectTrack(msg.arg1, (String)msg.obj);
                            break;
                        case MSG_DO_RELEASE_SPECIFIELD_SESSION:
                            doReleaseSpecifiedSession((DtvkitTvInputSession)msg.obj, msg.arg1 == 1 ? true : false, msg.arg2 == 1 ? true : false);
                            break;
                        case MSG_SCHEDULE_TIMESHIFT_RECORDING_TASK:
                            timeshiftRecordingTask();
                            break;
                        case MSG_TIMESHIFT_PLAY:
                            Uri TimeshiftUri = (Uri)msg.obj;
                            setTimeshiftPlay(TimeshiftUri);
                            break;
                        case MSG_TIMESHIFT_RESUME:
                            setTimeshiftResume();
                            break;
                        case MSG_TIMESHIFT_PASUE:
                            setTimeshiftPasue();
                            break;
                        case MSG_TIMESHIFT_SEEK:
                            long timeMs = (long)msg.obj;
                            setTimeshiftSeek(timeMs);
                            break;
                        case MSG_TIMESHIFT_PLAY_SET_PLAYBACKPARAMS:
                            PlaybackParams params = (PlaybackParams)msg.obj;
                            setTimeShiftSetPlaybackParams(params);
                            break;
                        case MSG_START_MHEG5:
                            String dvbUri = (String)msg.obj;
                            boolean mhegSuspendStatus = msg.arg1 == 1;
                            startMheg(dvbUri, mhegSuspendStatus);
                            break;
                        case MSG_STOP_MHEG5:
                            mhegStop();
                            break;
                        case MSG_CI_UPDATE_PROFILE_OVER:
                            Bundle updateEvent = new Bundle();
                            updateEvent.putString(ConstantManager.CI_PLUS_COMMAND, ConstantManager.VALUE_CI_PLUS_COMMAND_CHANNEL_UPDATED);
                            sendBundleToAppByTif(ConstantManager.ACTION_CI_PLUS_INFO, updateEvent);
                            break;
                        default:
                            Log.d(TAG, "mHandlerThreadHandle initWorkThread default");
                            break;
                    }
                    Log.d(TAG, "mHandlerThreadHandle    " + msg.what + ", sessionIndex" + mCurrentDtvkitTvInputSessionIndex + " done]]]");
                    return true;
                }
            });
            mMainHandle = new MainHandler();
        }

        //use osd to hide video instead
        /*private void syncParentControlSetting() {
            int current_age, setting_min_age;

            current_age = getParentalControlAge();
            setting_min_age = getCurrentMinAgeByBlockedRatings();
            if (setting_min_age == 0xFF && getParentalControlOn()) {
                setParentalControlOn(false);
                notifyContentAllowed();
            }else if (setting_min_age >= 4 && setting_min_age <= 18 && current_age != setting_min_age) {
                setParentalControlAge(setting_min_age);
                if (current_age < setting_min_age) {
                    Log.e(TAG, "rating changed, oldAge < newAge : [" +current_age+ " < " +setting_min_age+ "], so will allow");
                    notifyContentAllowed();
                }
            }
        }*/

        private int getContentRatingsOfCurrentProgram() {
            int age = 0;
            String pc_rating;
            String rating_system;
            long currentStreamTime = 0;
            TvContentRating[] ratings;

            //get current position as timeshift may seek to related position
            currentStreamTime = timeshifting ? onTimeShiftGetCurrentPosition() : PropSettingManager.getCurrentStreamTime(true);
            if (currentStreamTime == 0)
                return 0;
            Log.d(TAG, "currentStreamTime:("+currentStreamTime+")");
            if (mTunedChannel == null) {
                Log.d(TAG, "getContentRatingsOfCurrentProgram null mTunedChannel");
                return age;
            }
            Program program = TvContractUtils.getCurrentProgramExt(mContext.getContentResolver(), TvContract.buildChannelUri(mTunedChannel.getId()), currentStreamTime);

            ratings = program == null ? null : program.getContentRatings();
            if (ratings != null)
            {
               Log.d(TAG, "ratings:["+ratings[0].flattenToString()+"]");
               pc_rating = ratings[0].getMainRating();
               rating_system = ratings[0].getRatingSystem();
               if (rating_system.equals("DVB"))
               {
                   String[] ageArry = pc_rating.split("_", 2);
                   if (ageArry[0].equals("DVB"))
                   {
                       age = Integer.valueOf(ageArry[1]);
                   }
               }
            }

            return age;
        }

        private int getContentRatingsOfCurrentPlayingRecordedProgram() {
            int age = 0;
            String pc_rating;
            String rating_system;
            long currentStreamTime = 0;
            TvContentRating[] ratings;

            Log.d(TAG, "getContentRatingsOfCurrentPlayingRecordedProgram = " + recordedProgram);

            ratings = recordedProgram == null ? null : recordedProgram.getContentRatings();
            if (ratings != null)
            {
               Log.d(TAG, "ratings:["+ratings[0].flattenToString()+"]");
               pc_rating = ratings[0].getMainRating();
               rating_system = ratings[0].getRatingSystem();
               if (rating_system.equals("DVB"))
               {
                   String[] ageArry = pc_rating.split("_", 2);
                   if (ageArry[0].equals("DVB"))
                   {
                       age = Integer.valueOf(ageArry[1]);
                   }
               }
            }

            return age;
        }

        private void updateParentalControlExt() {
            int age = 0;
            int rating;
            boolean isParentControlEnabled = mTvInputManager.isParentalControlsEnabled();
            Log.d(TAG, "updateParentalControlExt isParentControlEnabled = " + isParentControlEnabled + ", mKeyUnlocked = " + mKeyUnlocked);
            if (isParentControlEnabled && !mKeyUnlocked) {
                try {
                    JSONArray args = new JSONArray();
                    rating = getCurrentMinAgeByBlockedRatings();
                    age = recordedProgram == null ? getContentRatingsOfCurrentProgram() : getContentRatingsOfCurrentPlayingRecordedProgram();
                    Log.e(TAG, "updateParentalControlExt current program age["+ age +"] setting_rating[" +rating+ "]");
                    if ((rating < 4 || rating > 18 || age < rating) && mBlocked)
                    {
                        notifyContentAllowed();
                        //change mute status by application
                        //setBlockMute(false);
                        mBlocked = false;
                    }
                    else if (rating >= 4 && rating <= 18 && (age > rating || (!getCurrentCountryIsNordig() && age == rating)))
                    {
                        String Rating = "";
                        Rating = String.format("DVB_%d", age);
                        notifyContentBlocked(TvContentRating.createRating("com.android.tv", "DVB", Rating));
                        if (!mBlocked)
                        {
                            //change mute status by application
                            //setBlockMute(true);
                            mBlocked = true;
                        }
                    }
                } catch (Exception e) {
                    Log.e(TAG, "updateParentalControlExt = " + e.getMessage());
                }
            }
            else if (mBlocked)
            {
                notifyContentAllowed();
                //change mute status by application
                //setBlockMute(false);
                mBlocked = false;
            }
        }

        //use osd to hide video instead
        /*private void updateParentalControl() {
            int age = 0;
            int rating;
            int pc_age;
            boolean isParentControlEnabled = mTvInputManager.isParentalControlsEnabled();
            if (isParentControlEnabled) {
                try {
                    JSONArray args = new JSONArray();
                    //age = DtvkitGlueClient.getInstance().request("Player.getCurrentProgramRatingAge", args).getInt("data");
                    rating = getCurrentMinAgeByBlockedRatings();
                    pc_age = getParentalControlAge();
                    age = recordedProgram == null ? getContentRatingsOfCurrentProgram() : getContentRatingsOfCurrentPlayingRecordedProgram();
                    Log.e(TAG, "updateParentalControl current program age["+ age +"] setting_rating[" +rating+ "] pc_age[" +pc_age+ "]");
                    if (getParentalControlOn()) {
                        if (rating < 4 || rating > 18 || age == 0) {
                            setParentalControlOn(false);
                            notifyContentAllowed();
                        }else if (rating >= 4 && rating <= 18 && age >= rating) {
                            if (pc_age != rating)
                                setParentalControlAge(rating);
                            }
                    }else {
                        if (rating >= 4 && rating <= 18 && age != 0) {
                            Log.e(TAG, "P_C false, but age isn't 0, so set P_C enbale rating:" + rating);
                            if (pc_age != rating || age >= rating) {
                                setParentalControlOn(true);
                                setParentalControlAge(rating);
                            }
                        }
                    }
                } catch (Exception e) {
                    Log.e(TAG, "updateParentalControl = " + e.getMessage());
                }
            }
        }*/

        private void setBlockMute(boolean mute) {
            Log.d(TAG, "setBlockMute = " + mute + ", index = " + mCurrentDtvkitTvInputSessionIndex + ", mIsPip = " + mIsPip);
            if (!getFeatureSupportFullPipFccArchitecture()) {
                playerSetMute(mute);
            } else {
                if (!mIsPip) {
                    playerSetMute(mute);
                } else {
                    playerSetPipMute(mute);
                }
            }
        }

        private void setUnBlock(TvContentRating unblockedRating) {
            Log.i(TAG, "setUnBlock " + unblockedRating);
            mKeyUnlocked = true;
            mBlocked = false;
            //hide by osd instead
            //setParentalControlOn(false);
            notifyContentAllowed();
        }

        private boolean playerInitAssociateDualSupport() {
            boolean result = false;
            //mAudioADAutoStart = mDataMananer.getIntParameters(DataMananer.TV_KEY_AD_SWITCH) == 1;
            mAudioADMixingLevel = mDataMananer.getIntParameters(DataMananer.TV_KEY_AD_MIX);
            mAudioADVolume = mDataMananer.getIntParameters(DataMananer.TV_KEY_AD_VOLUME);
            boolean adOn = playergetAudioDescriptionOn();
            Log.d(TAG, "playerInitAssociateDualSupport mAudioADAutoStart = " + mAudioADAutoStart + ", mAudioADMixingLevel = " + mAudioADMixingLevel + ", mAudioADVolume = " + mAudioADVolume);
            if (mAudioADAutoStart) {
                //setAdFunction(MSG_MIX_AD_DUAL_SUPPORT, 1);
                //setAdFunction(MSG_MIX_AD_MIX_SUPPORT, 1);
                //setAdFunction(MSG_MIX_AD_MIX_LEVEL, mAudioADMixingLevel);
                //if (!adOn) {
                    setAdFunction(MSG_MIX_AD_MIX_LEVEL, mAudioADMixingLevel);
                    setAdFunction(MSG_MIX_AD_SET_VOLUME, mAudioADVolume);
                    //setAdFunction(MSG_MIX_AD_SET_ASSOCIATE, 1);
                //}
            } else {
                //setAdFunction(MSG_MIX_AD_MIX_SUPPORT, 0);
                //setAdFunction(MSG_MIX_AD_DUAL_SUPPORT, 0);
                //if (adOn) {
                    //setAdFunction(MSG_MIX_AD_SET_ASSOCIATE, 0);
                //}
            }
            result = true;
            return result;
        }

        private boolean playerResetAssociateDualSupport() {
            boolean result = false;
            setAdFunction(MSG_MIX_AD_SET_ASSOCIATE, 0);
            //setAdFunction(MSG_MIX_AD_MIX_SUPPORT, 0);
            //setAdFunction(MSG_MIX_AD_DUAL_SUPPORT, 0);
            result = true;
            return result;
        }

        private class MainHandler extends Handler {
            public void handleMessage(Message msg) {
                Log.d(TAG, "MainHandler [[[:" + msg.what + ", sessionIndex = " + mCurrentDtvkitTvInputSessionIndex);
                switch (msg.what) {
                    case MSG_MAIN_HANDLE_DESTROY_OVERLAY:
                        doDestroyOverlay();
                        if (mHandlerThreadHandle != null) {
                            mHandlerThreadHandle.sendEmptyMessage(MSG_RELEASE_WORK_THREAD);
                        }
                        break;
                    case MSG_SHOW_SCAMBLEDTEXT:
                        if (mView != null) {
                            mView.showScrambledText(getString(R.string.play_scrambled));
                        }
                        break;
                    case MSG_HIDE_SCAMBLEDTEXT:
                        if (mView != null) {
                            mView.hideScrambledText();
                        }
                        break;
                    case MSG_SHOW_TUNING_IMAGE:
                        if (mView != null) {
                            mView.showTuningImage(null);
                        }
                        break;
                    case MSG_HIDE_TUNING_IMAGE:
                        if (mView != null) {
                            mView.hideTuningImage();
                        }
                        break;
                    case MSG_DISPLAY_STREAM_CHANGE_DIALOG:
                        if (mHandlerThreadHandle != null) {
                            mHandlerThreadHandle.removeCallbacksAndMessages(null);
                        }
                        showSearchConfirmDialog(DtvkitTvInput.this, mTunedChannel, (int)msg.arg1);
                        break;
                    case MSG_SET_TELETEXT_MIX_NORMAL:
                        if (!mTeleTextMixNormal) {
                            mTeleTextMixNormal = true;
                            mView.setTeletextMix(TTX_MODE_NORMAL);
                            playerSetRectangle(0, 0, mWinWidth, mWinHeight);
                        }
                        break;
                    case MSG_SET_TELETEXT_MIX_TRANSPARENT:
                        {
                            mTeleTextMixNormal = false;
                            mView.setTeletextMix(TTX_MODE_TRANSPARENT);
                            playerSetRectangle(0, 0, mWinWidth, mWinHeight);
                        }
                        break;
                    case MSG_SET_TELETEXT_MIX_SEPARATE:
                        {
                            mTeleTextMixNormal = false;
                            mView.setTeletextMix(TTX_MODE_SEPARATE);
                            playerSetRectangle(0, mWinHeight/4, mWinWidth/2, mWinHeight/2);
                        }
                        break;
                    case MSG_SUBTITLE_SHOW_CLOSED_CAPTION:
                        String ccData = (String)msg.obj;
                        boolean bshow = msg.arg1 == 0 ? false : true;
                        if (mView != null) {
                            if (bshow && ccData != null) {
                                mView.showCCSubtitle(ccData);
                            }
                            else {
                                mView.hideCCSubtitle();
                            }
                        }
                        break;
                    case MSG_FILL_SURFACE:
                        fillSurfaceWithFixColor();
                        break;
                    case MSG_EVENT_SUBTITLE_OPENED:
                        initSubtitleOrTeletextIfNeed();
                        break;
                    case MSG_CI_UPDATE_PROFILE_CONFIRM:
                        showOpSearchConfirmDialog(DtvkitTvInput.this, msg.arg1);
                        break;
                    default:
                        Log.d(TAG, "MainHandler default");
                        break;
                }
                Log.d(TAG, "MainHandler    " + msg.what + ", sessionIndex = " + mCurrentDtvkitTvInputSessionIndex + "done]]]");
            }
        }

        private boolean checkRealTimeResolution() {
            boolean result = false;
            if (mTunedChannel == null) {
                return true;
            }
            String serviceType = mTunedChannel.getServiceType();
            //update video track resolution
            if (TvContract.Channels.SERVICE_TYPE_AUDIO_VIDEO.equals(serviceType)) {
                int[] videoSize = playerGetDTVKitVideoSize();
                String realtimeVideoFormat = null;//mSysSettingManager.getVideoFormatFromSys();
                if (videoSize[0] > 0 && videoSize[1] > 0 && videoSize[2] > 0 && videoSize[3] >= 0 && videoSize[3] <= 1) {
                    realtimeVideoFormat = videoSize[1] + (videoSize[3] == 1 ? "P" : "I");
                }
                result = !TextUtils.isEmpty(realtimeVideoFormat);
                if (result) {
                    Bundle formatbundle = new Bundle();
                    formatbundle.putString(ConstantManager.PI_FORMAT_KEY, realtimeVideoFormat);
                    notifySessionEvent(ConstantManager.EVENT_STREAM_PI_FORMAT, formatbundle);
                    Log.d(TAG, "checkRealTimeResolution notify realtimeVideoFormat = " + realtimeVideoFormat + ", videoSize width = " + videoSize[0] + ", height = " + videoSize[1]);
                }
            }
            return result;
        }

        private boolean checkTrackInfoUpdate() {
            boolean result = false;
            if (mTunedChannel == null || mTunedTracks == null) {
                Log.d(TAG, "checkTrackinfoUpdate no need");
                return result;
            }
            List<TvTrackInfo> tracks = playerGetTracks(mTunedChannel, true);
            boolean needCheckAgain = false;
            if (tracks != null && tracks.size() > 0) {
                for (TvTrackInfo temp : tracks) {
                    int type = temp.getType();
                    switch (type) {
                        case TvTrackInfo.TYPE_AUDIO:
                            String currentAudioId = Integer.toString(playerGetSelectedAudioTrack());
                            if (TextUtils.equals(currentAudioId, temp.getId())) {
                                if (temp.getAudioSampleRate() == 0 || temp.getAudioChannelCount() == 0) {
                                    Log.d(TAG, "checkTrackinfoUpdate audio need check");
                                    needCheckAgain = true;
                                }
                            }
                            break;
                        case TvTrackInfo.TYPE_VIDEO:
                            if (temp.getVideoWidth() == 0 || temp.getVideoHeight() == 0 || temp.getVideoFrameRate() == 0f) {
                                Log.d(TAG, "checkTrackinfoUpdate video need check");
                                needCheckAgain = true;
                            }
                            break;
                        default:
                            break;
                    }
                }
            }
            if (tracks != null && tracks.size() > 0 && mTunedTracks != null && mTunedTracks.size() > 0) {
                if (!Objects.equals(mTunedTracks, tracks)) {
                    mTunedTracks = tracks;
                    notifyTracksChanged(mTunedTracks);
                    Log.d(TAG, "checkTrackinfoUpdate update new mTunedTracks");
                    result = true;
                }
            }
            if (needCheckAgain) {
                result = false;
            }
            return result;
        }

        private boolean sendCurrentSignalInfomation() {
            boolean result = false;
            if (mTunedChannel == null) {
                return result;
            }
            int[] signalInfo = getSignalStatus();
            if (true/*mSignalStrength != signalInfo[0] || mSignalQuality != signalInfo[1]*/) {
                mSignalStrength = signalInfo[0];
                mSignalQuality = signalInfo[1];
                Bundle signalbundle = new Bundle();
                signalbundle.putInt(ConstantManager.KEY_SIGNAL_STRENGTH, signalInfo[0]);
                signalbundle.putInt(ConstantManager.KEY_SIGNAL_QUALITY, signalInfo[1]);
                notifySessionEvent(ConstantManager.EVENT_SIGNAL_INFO, signalbundle);
                result = true;
                Log.d(TAG, "sendCurrentSignalInfomation notify signalStrength = " + signalInfo[0] + ", signalQuality = " + signalInfo[1]);
            }
            return result;
        }

        private boolean setAdFunction(int msg, int param1) {
            boolean result = false;
            AudioManager audioManager = null;
            if (mContext != null) {
                audioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
            }
            if (audioManager == null) {
                Log.i(TAG, "setAdFunction null audioManager");
                return result;
            }
            //Log.d(TAG, "setAdFunction msg = " + msg + ", param1 = " + param1);
            switch (msg) {
                case MSG_MIX_AD_DUAL_SUPPORT://dual_decoder_surport for ad & main mix on/off
                    if (param1 > 0) {
                        audioManager.setParameters("hal_param_dual_dec_support=1");
                    } else {
                        audioManager.setParameters("hal_param_dual_dec_support=0");
                    }
                    Log.d(TAG, "setAdFunction MSG_MIX_AD_DUAL_SUPPORT setParameters:"
                            + "hal_param_dual_dec_support=" + (param1 > 0 ? 1 : 0));
                    result = true;
                    break;
                case MSG_MIX_AD_MIX_SUPPORT://Associated audio mixing on/off
                    if (param1 > 0) {
                        audioManager.setParameters("hal_param_ad_mix_enable=1");
                    } else {
                        audioManager.setParameters("hal_param_ad_mix_enable=0");
                    }
                    Log.d(TAG, "setAdFunction MSG_MIX_AD_MIX_SUPPORT setParameters:"
                            + "hal_param_ad_mix_enable=" + (param1 > 0 ? 1 : 0));
                    result = true;
                    break;
                case MSG_MIX_AD_MIX_LEVEL://Associated audio mixing level
                    audioManager.setParameters("hal_param_dual_dec_mix_level=" + param1 + "");
                    Log.d(TAG, "setAdFunction MSG_MIX_AD_MIX_LEVEL setParameters:"
                            + "hal_param_dual_dec_mix_level=" + param1);
                    result = true;
                    break;
                case MSG_MIX_AD_SET_MAIN://set Main Audio by handle
                    result = playerSelectAudioTrack(param1);
                    Log.d(TAG, "setAdFunction MSG_MIX_AD_SET_MAIN result=" + result
                            + ", setAudioStream " + param1);
                    break;
                case MSG_MIX_AD_SET_ASSOCIATE://set Associate Audio by handle
                    result = playersetAudioDescriptionOn(param1 == 1);
                    Log.d(TAG, "setAdFunction MSG_MIX_AD_SET_ASSOCIATE result=" + result
                            + "setAudioDescriptionOn " + (param1 == 1));
                    break;
                case MSG_MIX_AD_SWITCH_ENABLE:
                    if (param1 > 0) {
                        audioManager.setParameters("ad_switch_enable=1");
                    } else {
                        audioManager.setParameters("ad_switch_enable=0");
                    }
                    Log.d(TAG, "setAdFunction MSG_MIX_AD_SWITCH_ENABLE setParameters:"
                            + "ad_switch_enable==" + (param1 > 0 ? 1 : 0));
                    result = true;
                    break;
                case MSG_MIX_AD_SET_VOLUME:
                    audioManager.setParameters("dual_decoder_advol_level=" + param1 + "");
                    Log.d(TAG, "setAdFunction MSG_MIX_AD_SET_VOLUME setParameters:"
                            + "dual_decoder_advol_level=" + param1);
                    result = true;
                    break;
                default:
                    Log.i(TAG,"setAdFunction unkown  msg = " + msg + ", param1 = " + param1);
                    break;
            }
            return result;
        }

        private void setTimeshiftPlay(Uri uri) {
            Log.i(TAG, "setTimeshiftPlay " + uri);

            recordedProgram = getRecordedProgram(uri);
            if (recordedProgram != null) {
                try {
                    if (!mTuned) {
                        if (!mMainSessionSemaphore.tryAcquire(SEMAPHORE_TIME_OUT, TimeUnit.MILLISECONDS)) {
                            if (mReleased) {
                                Log.d(TAG, "setTimeshiftPlay mMainSessionSemaphore timeout but session released");
                            } else {
                                Log.d(TAG, "setTimeshiftPlay mMainSessionSemaphore timeout");
                            }
                            return;
                        } else {
                            if (mReleased) {
                                Log.d(TAG, "setTimeshiftPlay Acquire mMainSessionSemaphore ok but session released");
                                return;
                            } else {
                                mAquireMainSemaphore = true;
                                Log.d(TAG, "setTimeshiftPlay Acquire mMainSessionSemaphore ok");
                            }
                        }
                    }
                } catch (Exception e) {
                    Log.d(TAG, "setTimeshiftPlay tryAcquire Exception = " + e.getMessage());
                    return;
                }
                mTuned = true;
                if (getFeatureSupportFullPipFccArchitecture() && !mSurfaceSent && mSurface != null) {
                    mSurfaceSent = true;
                    mView.nativeOverlayView.setOverlayTarge(mView.nativeOverlayView.mTarget);
                    mView.mSubServerView.setOverlaySubtitleListener(mView.mSubServerView.mSubListener);
                    if (isSdkAfterAndroidQ()) {
                        mHardware.setSurface(mSurface, mConfigs[1]);
                        setSurfaceTunnelId(INDEX_FOR_MAIN, 1);
                    } else {
                        DtvkitGlueClient.getInstance().setMutilSurface(INDEX_FOR_MAIN, mSurface);
                    }
                    playerSetRectangle(0, 0, mWinWidth, mWinHeight);
                }
                playerState = PlayerState.PLAYING;
                playerStop();
                playerSetSubtitlesOn(false);
                playerSetTeletextOn(false, -1);
                DtvkitGlueClient.getInstance().registerSignalHandler(mHandler);
                mAudioADAutoStart = mDataMananer.getIntParameters(DataMananer.TV_KEY_AD_SWITCH) == 1;
                if (playerPlay(INDEX_FOR_MAIN, recordedProgram.getRecordingDataUri(), mAudioADAutoStart, false, 0, "", "").equals("ok"))
                {
                    notifyChannelRetuned(uri);
                    if (mHandlerThreadHandle != null) {
                        mHandlerThreadHandle.removeMessages(MSG_CHECK_PARENTAL_CONTROL);
                        mHandlerThreadHandle.sendEmptyMessageDelayed(MSG_CHECK_PARENTAL_CONTROL, MSG_CHECK_PARENTAL_CONTROL_PERIOD);
                    }
                }
                else
                {
                    DtvkitGlueClient.getInstance().unregisterSignalHandler(mHandler);
                    notifyVideoUnavailable(TvInputManager.VIDEO_UNAVAILABLE_REASON_UNKNOWN);
                }
            }
        }

        private void setTimeshiftResume() {
            Log.i(TAG, "setTimeShiftResume ");
            playerState = PlayerState.PLAYING;
            if (playerResume())
            {
                playSpeed = 1;
            }
        }

        private void setTimeshiftPasue() {
            Log.i(TAG, "setTimeshiftPasue timeshiftRecorderState:"+timeshiftRecorderState+" timeshifting:"+timeshifting);
            if (timeshiftRecorderState == RecorderState.RECORDING && !timeshifting) {
                Log.i(TAG, "starting pause playback ");
                timeshifting = true;

                /*
                  The mheg may hold an external_control in the dtvkit,
                  which upset the normal av process following, so stop it first,
                  thus, mheg will not be valid since here to the next onTune.
                */
                mhegStop();

                playerPlayTimeshiftRecording(true, true);
            }
            else {
                Log.i(TAG, "player pause ");
                if (playerPause())
                {
                    playSpeed = 0;
                }
            }
        }

        private void setTimeshiftSeek(long timeMs) {
            Log.i(TAG, "setTimeShiftSeekTo:  " + timeMs);
            if (timeshiftRecorderState == RecorderState.RECORDING && !timeshifting) /* Watching live tv while recording */ {
                timeshifting = true;
                boolean seekToBeginning = false;

                if (timeMs == startPosition) {
                    seekToBeginning = true;
                }
                  /*
                  The mheg may hold an external_control in the dtvkit,
                  which upset the normal av process following, so stop it first,
                  thus, mheg will not be valid since here to the next onTune.
                */
                mhegStop();
                playerPlayTimeshiftRecording(false, !seekToBeginning);
            } else if (timeshiftRecorderState == RecorderState.RECORDING && timeshifting) {
                //diff time may change within 1s, add extra 1s to prevent it from playing previous program when seeking to the beginning of current in timeshift mode
                long rawMsPosition = (timeMs - (originalStartPosition + PropSettingManager.getStreamTimeDiff()));
                long floorPosition = rawMsPosition % 1000;
                long position =  rawMsPosition / 1000 + (floorPosition > 0 ? 1 : 0) + 1;
                playerSeekTo(position < 0 ? 0 : position);
            } else {
                playerSeekTo(timeMs / 1000);
            }

        }

        private void setTimeShiftSetPlaybackParams(PlaybackParams params) {
            Log.i(TAG, "setTimeShiftSetPlaybackParams:  " + params);
            float speed = params.getSpeed();
            Log.i(TAG, "speed: " + speed);
            if (speed != playSpeed) {
                if (timeshiftRecorderState == RecorderState.RECORDING && !timeshifting) {
                    timeshifting = true;
                    /*
                      The mheg may hold an external_control in the dtvkit,
                      which upset the normal av process following, so stop it first,
                      thus, mheg will not be valid since here to the next onTune.
                    */
                    mhegStop();
                    playerPlayTimeshiftRecording(false, true);
                }

                if (playerSetSpeed(speed)) {
                    playSpeed = speed;
                }
            }
        }

        private void tryStartTimeshifting() {
            if (getFeatureSupportTimeshifting()) {
                if (timeshiftRecorderState == RecorderState.STOPPED) {
                    numActiveRecordings = recordingGetNumActiveRecordings();
                    Log.i(TAG, "numActiveRecordings: " + numActiveRecordings);
                    if (recordingPending) {
                        numActiveRecordings += 1;
                        Log.i(TAG, "recordingPending: +1");
                    }
                    if (numActiveRecordings < numRecorders
                        && numActiveRecordings < getNumRecordersLimit()) {
                        timeshiftAvailable.setYes();
                    } else {
                        timeshiftAvailable.setNo();
                    }
                }
                Log.i(TAG, "tryStartTimeshifting timeshiftAvailable: " + timeshiftAvailable + ", timeshiftRecorderState: " + timeshiftRecorderState);
                if (timeshiftAvailable.isAvailable()) {
                    if (timeshiftRecorderState == RecorderState.STOPPED) {
                        if (playerStartTimeshiftRecording()) {
                            Log.d(TAG, "tryStartTimeshifting OK");
                            /*
                              The onSignal callback may be triggerd before here,
                              and changes the state to a further value.
                              so check the state first, in order to prevent getting it reset.
                            */
                            if (timeshiftRecorderState != RecorderState.RECORDING) {
                                timeshiftRecorderState = RecorderState.STARTING;
                            }
                        } else {
                            Log.d(TAG, "tryStartTimeshifting fail");
                            notifyTimeShiftStatusChanged(TvInputManager.TIME_SHIFT_STATUS_UNAVAILABLE);
                        }
                    }
                } else {
                    Log.d(TAG, "tryStartTimeshifting not available");
                    notifyTimeShiftStatusChanged(TvInputManager.TIME_SHIFT_STATUS_UNAVAILABLE);
                }
            } else {
                notifyTimeShiftStatusChanged(TvInputManager.TIME_SHIFT_STATUS_UNAVAILABLE);
            }
        }

        private void tryStopTimeshifting() {
            if (getFeatureSupportTimeshifting()) {
                Log.i(TAG, "tryStopTimeshifting timeshiftAvailable: " + timeshiftAvailable + ", timeshiftRecorderState = " + timeshiftRecorderState);
                if (timeshiftAvailable.isAvailable()) {
                    if ((timeshiftRecorderState == RecorderState.RECORDING)) {
                        if (playerStopTimeshiftRecording(timeshifting)) {
                            Log.d(TAG, "tryStopTimeshifting OK");
                        } else {
                            Log.d(TAG, "tryStopTimeshifting fail");
                            notifyTimeShiftStatusChanged(TvInputManager.TIME_SHIFT_STATUS_UNAVAILABLE);
                        }
                    }
                } else {
                    Log.d(TAG, "tryStopTimeshifting not available");
                    notifyTimeShiftStatusChanged(TvInputManager.TIME_SHIFT_STATUS_UNAVAILABLE);
                }
            } else {
                notifyTimeShiftStatusChanged(TvInputManager.TIME_SHIFT_STATUS_UNAVAILABLE);
            }
        }

        private void monitorTimeshiftRecordingPathAndTryRestart(boolean on, boolean now, boolean start) {
            /*monitor the rec path*/
            if (mHandlerThreadHandle != null) {
                Log.d(TAG, "monitorTimeshiftRecordingPathAndTryRestart on = " + on + ", now = " + now + ", start = " + start);
                if (mHandlerThreadHandle.hasMessages(MSG_CHECK_REC_PATH)) {
                    mHandlerThreadHandle.removeMessages(MSG_CHECK_REC_PATH);//remove pending message
                }
                if (on) {
                    Message mess = mHandlerThreadHandle.obtainMessage(now ? MSG_CHECK_REC_PATH_DIRECTLY : MSG_CHECK_REC_PATH, on ? 1 : 0, start ? 1 : 0);
                    boolean info = mHandlerThreadHandle.sendMessageDelayed(mess, now ? 0 : MSG_CHECK_REC_PATH_PERIOD);
                }
            }
        }

        public void sendMsgTryStartTimeshift(int delay) {
            if (mHandlerThreadHandle != null) {
                mHandlerThreadHandle.removeMessages(MSG_TRY_START_TIMESHIFT);
                mHandlerThreadHandle.sendEmptyMessageDelayed(MSG_TRY_START_TIMESHIFT, delay);
            }
        }

        public void sendMsgTryStopTimeshift(int delay) {
            if (mHandlerThreadHandle != null) {
                mHandlerThreadHandle.removeMessages(MSG_TRY_STOP_TIMESHIFT);
                mHandlerThreadHandle.sendEmptyMessageDelayed(MSG_TRY_STOP_TIMESHIFT, delay);
            }
        }

        private void scheduleTimeshiftRecordingTask() {
            final long SCHEDULE_TIMESHIFT_RECORDING_DELAY_MILLIS = 1000 * 2;
            Log.i(TAG, "calling scheduleTimeshiftRecordingTask");
            if (mHandlerThreadHandle != null) {
                mHandlerThreadHandle.removeMessages(MSG_SCHEDULE_TIMESHIFT_RECORDING_TASK);
                mHandlerThreadHandle.sendEmptyMessageDelayed(MSG_SCHEDULE_TIMESHIFT_RECORDING_TASK, SCHEDULE_TIMESHIFT_RECORDING_DELAY_MILLIS);
            }
        }

        private void removeScheduleTimeshiftRecordingTask() {
            Log.i(TAG, "calling removeScheduleTimeshiftRecordingTask");
            if (mHandlerThreadHandle != null) {
                mHandlerThreadHandle.removeMessages(MSG_SCHEDULE_TIMESHIFT_RECORDING_TASK);
            }
        }

        private void timeshiftRecordingTask() {
            Log.i(TAG, "timeshiftRecordingTask");
            resetRecordingPath();
            tryStartTimeshifting();
        }

        private int getTimeshiftBufferSizeMins() {
            return PropSettingManager.getInt("vendor.tv.dtv.tf.mins", timeshiftBufferSizeMins);
        }

        private int getTimeshiftBufferSizeMBs() {
            return PropSettingManager.getInt("vendor.tv.dtv.tf.mbs", timeshiftBufferSizeMBs);
        }

        public class MountEventReceiver extends BroadcastReceiver {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (action.equals(Intent.ACTION_MEDIA_REMOVED) || action.equals(Intent.ACTION_MEDIA_EJECT)) {
                    String mountPath = intent.getData().getPath();
                    String playingPath = null;
                    if (recordedProgram != null) {
                        try {
                            playingPath = recordedProgram.getInternalProviderData().get("record_file_path").toString();
                        } catch (Exception e) {
                        }
                    }
                    if (!TextUtils.isEmpty(playingPath) && TextUtils.equals(playingPath, mountPath)) {
                        if (timeshiftRecorderState == RecorderState.STOPPED && timeshifting == false) {
                            Log.d(TAG, "Pvr storage has been removed, need stop pvr playing.");
                            playerStop();
                        }
                    }
                }
            }
        }

        private void sendBundleToAppByTif(String action, Bundle event) {
            Log.d(TAG, "sendBundleToAppByTif action = " + action + ", event = " + event);
            notifySessionEvent(action, event);
        }
    }

    private boolean resetRecordingPath() {
        String newPath = mDataMananer.getStringParameters(DataMananer.KEY_PVR_RECORD_PATH);
        boolean changed = false;

        String path = SysSettingManager.convertMediaPathToMountedPath(newPath);
        if (!TextUtils.isEmpty(path) && !SysSettingManager.isMountedPathAvailable(path) && !getFeatureSupportNewTvApp()) {
            Log.d(TAG, "removable device has been moved and use default path");
            newPath = DataMananer.PVR_DEFAULT_PATH;
            mDataMananer.saveStringParameters(DataMananer.KEY_PVR_RECORD_PATH, newPath);
            changed = true;
        } else {
            Log.d(TAG, "resetRecordingPath newLiveTv can set default path");
        }
        if (getFeatureSupportNewTvApp()) {
            if (TextUtils.isEmpty(newPath) || DataMananer.PVR_DEFAULT_PATH.equals(newPath)) {
                Log.d(TAG, "sei livetv support removable storage only");
                return false;
            } else {
                Log.d(TAG, "resetRecordingPath newLiveTv newPath = " + newPath);
            }
        }
        recordingAddDiskPath(newPath);
        recordingSetDefaultDisk(newPath);
        return changed;
    }

    private void onChannelsChanged() {
        mChannels = TvContractUtils.buildChannelMap(mContentResolver, mInputId);
    }

    private Channel getChannel(Uri channelUri) {
        if (mChannels != null) {
            return mChannels.get(ContentUris.parseId(channelUri));
        } else {
            return null;
        }
    }

    private String getChannelInternalDvbUri(Channel channel) {
        if (channel == null) {
            return "dvb://0000.0000.0000";
        }
        try {
            return channel.getInternalProviderData().get("dvbUri").toString();
        } catch (Exception e) {
            Log.e(TAG, "getChannelInternalDvbUri1 Exception = " + e.getMessage());
            return "dvb://0000.0000.0000";
        }
    }

    private String getChannelInternalDvbUriForFcc(Uri channelUri) {
        String result = "";
        Channel channel = null;
        try {
            if (mChannels != null && channelUri != null) {
                channel = mChannels.get(ContentUris.parseId(channelUri));
            }
            if (channel != null) {
                result = channel.getInternalProviderData().get("dvbUri").toString();
            }
        } catch (Exception e) {
            Log.e(TAG, "getChannelInternalDvbUri2 Exception = " + e.getMessage());
        }
        return result;
    }

    private String getProgramInternalDvbUri(Program program) {
        try {
            String uri = program.getInternalProviderData().get("dvbUri").toString();
            return uri;
        } catch (InternalProviderData.ParseException e) {
            Log.e(TAG, "getChannelInternalDvbUri ParseException = " + e.getMessage());
            return "dvb://current";
        }
    }

    private void playerSetVolume(int volume) {
        playerSetVolume(volume, INDEX_FOR_MAIN);
    }

    private void playerSetPipVolume(int volume) {
        playerSetVolume(volume, INDEX_FOR_PIP);
    }

    private void playerSetVolume(int volume, int index) {
        try {
            JSONArray args = new JSONArray();
            args.put(index);
            args.put(volume);
            DtvkitGlueClient.getInstance().request("Player.setVolume", args);
        } catch (Exception e) {
            Log.e(TAG, "playerSetVolume = " + e.getMessage());
        }
    }

    private void playerSetMute(boolean mute) {
        playerSetMute(mute, INDEX_FOR_MAIN);
    }

    private void playerSetPipMute(boolean mute) {
        playerSetMute(mute, INDEX_FOR_PIP);
    }

    private void playerSetMute(boolean mute, int index) {
        synchronized (mLock) {
            try {
                JSONArray args = new JSONArray();
                args.put(index);
                args.put(mute);
                DtvkitGlueClient.getInstance().request("Player.setMute", args);
            } catch (Exception e) {
                Log.e(TAG, "playerSetMute = " + e.getMessage());
            }
        }
    }

    private boolean playerGetMute() {
        return playerGetMute(INDEX_FOR_MAIN);
    }

    private boolean playerGetMute(int index) {
        boolean result = false;
        try {
            JSONArray args = new JSONArray();
            args.put(index);
            JSONObject obj = DtvkitGlueClient.getInstance().request("Player.getMute", args);
            if (obj != null && obj.getBoolean("accepted") ) {
                result = obj.getBoolean("data");
            }
        } catch (Exception e) {
            Log.e(TAG, "playerGetMute = " + e.getMessage());
        }
        Log.d(TAG, "playerGetMute result = " + result);
        return result;
    }

    private void playerSetSubtitlesOn(boolean on) {
        playerSetSubtitlesOn(on, INDEX_FOR_MAIN);
    }

    private void playerSetPipSubtitlesOn(boolean on) {
        playerSetSubtitlesOn(on, INDEX_FOR_PIP);
    }

    private void playerSetSubtitlesOn(boolean on, int index) {
        try {
            JSONArray args = new JSONArray();
            args.put(index);
            args.put(on);
            DtvkitGlueClient.getInstance().request("Player.setSubtitlesOn", args);
            Log.i(TAG, "playerSetSubtitlesOn on =  " + on);
        } catch (Exception e) {
            Log.e(TAG, "playerSetSubtitlesOn = " + e.getMessage());
        }
    }

    private String playerPlay(int index, String dvbUri, boolean ad_enable, boolean disable_audio, int surface, String uri_cache1, String uri_cache2) {
        synchronized (mLock) {
            try {
                JSONArray args = new JSONArray();
                args.put(index);
                args.put(dvbUri);
                args.put(ad_enable);
                args.put(disable_audio);
                args.put(surface);
                args.put(uri_cache1);
                args.put(uri_cache2);
                Log.d(TAG, "player.play: "+dvbUri);

                JSONObject resp = DtvkitGlueClient.getInstance().request("Player.play", args);
                boolean ok = resp.optBoolean("data");
                if (ok)
                    return "ok";
                else
                    return resp.optString("data", "");
            } catch (Exception e) {
                Log.e(TAG, "playerPlay = " + e.getMessage());
                return "unknown error";
            }
        }
    }

    private void setSurfaceTunnelId(int index, int tunnelId) {
        try {
            JSONArray args = new JSONArray();
            args.put(index);
            args.put(tunnelId);
            JSONObject resp = DtvkitGlueClient.getInstance().request("Player.setSurface", args);
            boolean ok = resp.optBoolean("data");
            if (!resp.optBoolean("data")) {
                Log.d(TAG, "setSurfaceTunnelId resp: " + resp);
            }
        } catch (Exception e) {
            Log.e(TAG, "setSurfaceTunnelId = " + e.getMessage());
        }
    }

    private void playerStop() {
        playerStop(INDEX_FOR_MAIN, true);
        playerStop(INDEX_FOR_MAIN, false);
    }

    private void playerStopAndKeepFfc() {
        playerStop(INDEX_FOR_MAIN, false);
    }

    private void playerPipStop() {
        playerStop(INDEX_FOR_PIP, false);
    }

    private void playerStop(int index, boolean stopFcc) {
     synchronized (mLock) {
            try {
                JSONArray args = new JSONArray();
                args.put(index);
                args.put(stopFcc);
                DtvkitGlueClient.getInstance().request("Player.stop", args);
            } catch (Exception e) {
                Log.e(TAG, "playerStop = " + e.getMessage());
            }
        }
    }

    private boolean playerPause() {
        return playerPause(INDEX_FOR_MAIN);
    }

    private boolean playerPipPause() {
        return playerPause(INDEX_FOR_PIP);
    }

    private boolean playerPause(int index) {
        try {
            JSONArray args = new JSONArray();
            args.put(index);
            DtvkitGlueClient.getInstance().request("Player.pause", args);
            return true;
        } catch (Exception e) {
            Log.e(TAG, "playerPause = " + e.getMessage());
            return false;
        }
    }

    private boolean playerResume() {
        return playerResume(INDEX_FOR_MAIN);
    }

    private boolean playerPipResume() {
        return playerResume(INDEX_FOR_PIP);
    }

    private boolean playerResume(int index) {
        try {
            JSONArray args = new JSONArray();
            args.put(index);
            DtvkitGlueClient.getInstance().request("Player.resume", args);
            return true;
        } catch (Exception e) {
            Log.e(TAG, "playerResume = " + e.getMessage());
            return false;
        }
    }

    private void playerFastForward() {
        playerFastForward(INDEX_FOR_MAIN);
    }

    private void playerPipFastForward() {
        playerFastForward(INDEX_FOR_PIP);
    }

    private void playerFastForward(int index) {
        try {
            JSONArray args = new JSONArray();
            args.put(index);
            DtvkitGlueClient.getInstance().request("Player.fastForward", args);
        } catch (Exception e) {
            Log.e(TAG, "playerFastForward = " + e.getMessage());
        }
    }

    private void playerFastRewind() {
        playerFastRewind(INDEX_FOR_MAIN);
    }

    private void playerPipFastRewind() {
        playerFastRewind(INDEX_FOR_PIP);
    }

    private void playerFastRewind(int index) {
        try {
            JSONArray args = new JSONArray();
            args.put(index);
            DtvkitGlueClient.getInstance().request("Player.fastRewind", args);
        } catch (Exception e) {
            Log.e(TAG, "playerFastRewind = " + e.getMessage());
        }
    }

    private boolean playerSetSpeed(float speed) {
        return playerSetSpeed(INDEX_FOR_MAIN, speed);
    }

    private boolean playerPipSetSpeed(float speed) {
        return playerSetSpeed(INDEX_FOR_PIP, speed);
    }

    private boolean playerSetSpeed(int index, float speed) {
        try {
            JSONArray args = new JSONArray();
            args.put(index);
            args.put((long)(speed * 100.0));
            DtvkitGlueClient.getInstance().request("Player.setPlaySpeed", args);
            return true;
        } catch (Exception e) {
            Log.e(TAG, "playerSetSpeed = " + e.getMessage());
            return false;
        }
    }

    private boolean playerSeekTo(long positionSecs) {
        return playerSeekTo(INDEX_FOR_MAIN, positionSecs);
    }

    private boolean playerPipSeekTo(long positionSecs) {
        return playerSeekTo(INDEX_FOR_PIP, positionSecs);
    }

    private boolean playerSeekTo(int index, long positionSecs) {
        try {
            JSONArray args = new JSONArray();
            args.put(index);
            args.put(positionSecs);
            DtvkitGlueClient.getInstance().request("Player.seekTo", args);
            return true;
        } catch (Exception e) {
            Log.e(TAG, "playerSeekTo = " + e.getMessage());
            return false;
        }
    }

    private boolean playerStartTimeshiftRecording() {
        return playerStartTimeshiftRecording(INDEX_FOR_MAIN);
    }

    private boolean playerPipStartTimeshiftRecording() {
        return playerStartTimeshiftRecording(INDEX_FOR_PIP);
    }

    private boolean playerStartTimeshiftRecording(int index) {
        try {
            JSONArray args = new JSONArray();
            args.put(index);
            DtvkitGlueClient.getInstance().request("Player.startTimeshiftRecording", args);
            return true;
        } catch (Exception e) {
            Log.e(TAG, "playerStartTimeshiftRecording = " + e.getMessage());
            return false;
        }
    }

    private boolean playerStopTimeshiftRecording(boolean returnToLive) {
        return playerStopTimeshiftRecording(INDEX_FOR_MAIN, returnToLive);
    }

    private boolean playerPipStopTimeshiftRecording(boolean returnToLive) {
        return playerStopTimeshiftRecording(INDEX_FOR_PIP, returnToLive);
    }

    private boolean playerStopTimeshiftRecording(int index, boolean returnToLive) {
        try {
            JSONArray args = new JSONArray();
            args.put(index);
            args.put(returnToLive);
            DtvkitGlueClient.getInstance().request("Player.stopTimeshiftRecording", args);
            return true;
        } catch (Exception e) {
            Log.e(TAG, "playerStopTimeshiftRecording = " + e.getMessage());
            return false;
        }
    }

    private boolean playerPlayTimeshiftRecording(boolean startPlaybackPaused, boolean playFromCurrent) {
        return playerPlayTimeshiftRecording(INDEX_FOR_MAIN, startPlaybackPaused, playFromCurrent);
    }

    private boolean playerPipPlayTimeshiftRecording(boolean startPlaybackPaused, boolean playFromCurrent) {
        return playerPlayTimeshiftRecording(INDEX_FOR_PIP, startPlaybackPaused, playFromCurrent);
    }

    private boolean playerPlayTimeshiftRecording(int index, boolean startPlaybackPaused, boolean playFromCurrent) {
        try {
            JSONArray args = new JSONArray();
            args.put(index);
            args.put(startPlaybackPaused);
            args.put(playFromCurrent);
            DtvkitGlueClient.getInstance().request("Player.playTimeshiftRecording", args);
            return true;
        } catch (Exception e) {
            Log.e(TAG, "playerStopTimeshiftRecording = " + e.getMessage());
            return false;
        }
    }

    private void playerSetRectangle(int x, int y, int width, int height) {
        playerSetRectangle(INDEX_FOR_MAIN, x, y, width, height);
    }

    private void playerPipSetRectangle(int x, int y, int width, int height) {
        playerSetRectangle(INDEX_FOR_PIP, x, y, width, height);
    }

    private void playerSetRectangle(int index, int x, int y, int width, int height) {
        try {
            JSONArray args = new JSONArray();
            args.put(index);
            args.put(x);
            args.put(y);
            args.put(width);
            args.put(height);
            DtvkitGlueClient.getInstance().request("Player.setRectangle", args);
        } catch (Exception e) {
            Log.e(TAG, "playerSetRectangle = " + e.getMessage());
        }
    }

    private List<TvTrackInfo> playerGetTracks(Channel tunedChannel, boolean detailsAvailable) {
        List<TvTrackInfo> tracks = new ArrayList<>();
        tracks.addAll(getVideoTrackInfoList(tunedChannel, detailsAvailable));
        tracks.addAll(getAudioTrackInfoList(detailsAvailable));
        tracks.addAll(getSubtitleTrackInfoList());
        return tracks;
    }

    private List<TvTrackInfo> getVideoTrackInfoList(Channel tunedChannel, boolean detailsAvailable) {
        List<TvTrackInfo> tracks = new ArrayList<>();
        //video tracks
        try {
            TvTrackInfo.Builder track = new TvTrackInfo.Builder(TvTrackInfo.TYPE_VIDEO, Integer.toString(0));
            Bundle bundle = new Bundle();
            if (detailsAvailable) {
                //get values that need to get from sys and need wait for at least 1s after play
                /*String decodeInfo = mSysSettingManager.getVideodecodeInfo();
                int videoWidth = mSysSettingManager.parseWidthFromVdecStatus(decodeInfo);
                int videoHeight = mSysSettingManager.parseHeightFromVdecStatus(decodeInfo);
                float videoFrameRate = mSysSettingManager.parseFrameRateStrFromVdecStatus(decodeInfo);
                String videoFrameFormat = mSysSettingManager.parseFrameFormatStrFromDi0Path();*/
                //use dtvkit interface
                int[] videoStatus = playerGetDTVKitVideoSize();
                int videoWidth = videoStatus[0];
                int videoHeight = videoStatus[1];
                float videoFrameRate = (float)videoStatus[2];
                String videoFrameFormat = "";
                if (videoStatus[3] == 1) {//1 means progressive
                    videoFrameFormat = "P";
                } else if (videoStatus[3] == 0) {
                    videoFrameFormat = "I";
                } else {
                    Log.d(TAG, "getVideoTrackInfoList no videoFrameFormat");
                }
                //set value
                track.setVideoWidth(videoWidth);
                track.setVideoHeight(videoHeight);
                track.setVideoFrameRate(videoFrameRate);
                bundle.putInt(ConstantManager.KEY_TVINPUTINFO_VIDEO_WIDTH, videoWidth);
                bundle.putInt(ConstantManager.KEY_TVINPUTINFO_VIDEO_HEIGHT, videoHeight);
                bundle.putFloat(ConstantManager.KEY_TVINPUTINFO_VIDEO_FRAME_RATE, videoFrameRate);
                bundle.putString(ConstantManager.KEY_TVINPUTINFO_VIDEO_FRAME_FORMAT, videoFrameFormat != null ? videoFrameFormat : "");
                //video format framework example "VIDEO_FORMAT_360P" "VIDEO_FORMAT_576I"
                //comment it as video format will be updated in checkRealTimeResolution
                /*String videoFormat = tunedChannel != null ? tunedChannel.getVideoFormat() : "";
                if (tunedChannel != null) {
                    //update video format such as VIDEO_FORMAT_1080P VIDEO_FORMAT_1080I
                    String buildVideoFormat = "VIDEO_FORMAT_" + videoHeight + videoFrameFormat;
                    if (videoHeight > 0 && !TextUtils.equals(buildVideoFormat, tunedChannel.getVideoFormat())) {
                        videoFormat = buildVideoFormat;
                        bundle.putString(ConstantManager.KEY_TVINPUTINFO_VIDEO_FORMAT, videoFormat != null ? videoFormat : "");
                        TvContractUtils.updateSingleChannelColumn(DtvkitTvInput.this.getContentResolver(), tunedChannel.getId(), TvContract.Channels.COLUMN_VIDEO_FORMAT, buildVideoFormat);
                    }
                }*/
            }
            //get values from db
            String videoCodec = tunedChannel != null ? tunedChannel.getVideoCodec() : "";
            //set value
            bundle.putString(ConstantManager.KEY_TVINPUTINFO_VIDEO_CODEC, videoCodec != null ? videoCodec : "");
            track.setExtra(bundle);
            //buid track
            tracks.add(track.build());
            Log.d(TAG, "getVideoTrackInfoList track bundle = " + bundle.toString());
        } catch (Exception e) {
            Log.e(TAG, "getVideoTrackInfoList Exception = " + e.getMessage());
        }
        return tracks;
    }

    private List<TvTrackInfo> getAudioTrackInfoList(boolean detailsAvailable) {
        return getAudioTrackInfoList(INDEX_FOR_MAIN, detailsAvailable);
    }

    private List<TvTrackInfo> getPipAudioTrackInfoList(boolean detailsAvailable) {
        return getAudioTrackInfoList(INDEX_FOR_PIP, detailsAvailable);
    }

    private List<TvTrackInfo> getAudioTrackInfoList(int index, boolean detailsAvailable) {
        List<TvTrackInfo> tracks = new ArrayList<>();
        //audio tracks
        try {
            List<TvTrackInfo> audioTracks = new ArrayList<>();
            JSONArray args = new JSONArray();
            args.put(index);
            JSONArray audioStreams = DtvkitGlueClient.getInstance().request("Player.getListOfAudioStreams", args).getJSONArray("data");
            int undefinedIndex = 0;
            for (int i = 0; i < audioStreams.length(); i++)
            {
                JSONObject audioStream = audioStreams.getJSONObject(i);
                Log.d(TAG, "getAudioTrackInfoList audioStream = " + audioStream.toString());
                TvTrackInfo.Builder track = new TvTrackInfo.Builder(TvTrackInfo.TYPE_AUDIO, Integer.toString(audioStream.getInt("index")));
                Bundle bundle = new Bundle();
                String audioLang = ISO639Data.parse(audioStream.getString("language"));
                if (TextUtils.isEmpty(audioLang) || ConstantManager.CONSTANT_UND_FLAG.equals(audioLang)) {
                    audioLang = ConstantManager.CONSTANT_UND_VALUE + ((undefinedIndex>0)?undefinedIndex:"");
                    undefinedIndex++;
                } else if (ConstantManager.CONSTANT_QAA.equalsIgnoreCase(audioLang)) {
                    audioLang = ConstantManager.CONSTANT_ORIGINAL_AUDIO;
                } else if (ConstantManager.CONSTANT_QAD.equalsIgnoreCase(audioLang)) {
                    audioLang = ConstantManager.CONSTANT_FRENCH;
                }
                track.setLanguage(audioLang);
                bundle.putBoolean(ConstantManager.KEY_TVINPUTINFO_AUDIO_AD, false);
                if (audioStream.getBoolean("ad")) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        track.setDescription("AD");
                        bundle.putBoolean(ConstantManager.KEY_TVINPUTINFO_AUDIO_AD, true);
                    }
                }
                String codes = audioStream.getString("codec");
                int pid = audioStream.getInt("pid");
                if (!TextUtils.isEmpty(codes)) {
                    bundle.putString(ConstantManager.KEY_AUDIO_CODES_DES, codes);
                    bundle.putString(ConstantManager.KEY_TVINPUTINFO_AUDIO_CODEC, codes);
                } else {
                    bundle.putString(ConstantManager.KEY_TVINPUTINFO_AUDIO_CODEC, "");
                }
                bundle.putInt(ConstantManager.KEY_TRACK_PID, pid);
                if (detailsAvailable) {
                    //can only be gotten after decode finished
                    int sampleRate = getAudioSamplingRateFromAudioPatch(pid);
                    int audioChannel = getAudioChannelFromAudioPatch(pid);
                    bundle.putInt(ConstantManager.KEY_TVINPUTINFO_AUDIO_SAMPLING_RATE, sampleRate);
                    bundle.putInt(ConstantManager.KEY_TVINPUTINFO_AUDIO_CHANNEL, audioChannel);
                    bundle.putInt(ConstantManager.AUDIO_PATCH_COMMAND_GET_AUDIO_CHANNEL_CONFIGURE, getAudioChannelConfigureFromAudioPatch(pid));
                    track.setAudioChannelCount(audioChannel);
                    track.setAudioSampleRate(sampleRate);
                }
                bundle.putInt(ConstantManager.KEY_TVINPUTINFO_AUDIO_INDEX, audioStream.getInt("index"));
                track.setExtra(bundle);
                audioTracks.add(track.build());
                Log.d(TAG, "getAudioTrackInfoList track bundle = " + bundle.toString());
            }
            ConstantManager.ascendTrackInfoOderByPid(audioTracks);
            tracks.addAll(audioTracks);
        } catch (Exception e) {
            Log.e(TAG, "getAudioTrackInfoList Exception = " + e.getMessage());
        }
        return tracks;
    }

    private int getAudioSamplingRateFromAudioPatch(int audioPid) {
        int result = 0;
        AudioManager audioManager = (AudioManager) this.getSystemService(Context.AUDIO_SERVICE);
        String temp = audioManager.getParameters(ConstantManager.AUDIO_PATCH_COMMAND_GET_SAMPLING_RATE);
        //sampling rate result "sample_rate=4800"
        //Log.d(TAG, "getAudioSamplingRateFromAudioPatch result = " + temp);
        try {
            if (temp != null) {
                String[] splitInfo = temp.split("=");
                if (splitInfo != null && splitInfo.length == 2 && "sample_rate".equals(splitInfo[0])) {
                    result = Integer.valueOf(splitInfo[1]);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "getAudioSamplingRateFromAudioPatch Exception = " + e.getMessage());
        }
        Log.d(TAG, "getAudioSamplingRateFromAudioPatch result = " + result);
        return result;
    }

    private int getAudioChannelFromAudioPatch(int audioPid) {
        int result = 0;
        AudioManager audioManager = (AudioManager) this.getSystemService(Context.AUDIO_SERVICE);
        String temp = audioManager.getParameters(ConstantManager.AUDIO_PATCH_COMMAND_GET_AUDIO_CHANNEL);
        //channel num result "channel_nums=2"
        //Log.d(TAG, "getAudioChannelFromAudioPatch result = " + temp);
        try {
            if (temp != null) {
                String[] splitInfo = temp.split("=");
                if (splitInfo != null && splitInfo.length == 2 && "channel_nums".equals(splitInfo[0])) {
                    result = Integer.valueOf(splitInfo[1]);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "getAudioChannelFromAudioPatch Exception = " + e.getMessage());
        }
        Log.d(TAG, "getAudioChannelFromAudioPatch result = " + result);
        return result;
    }

    private int getAudioChannelConfigureFromAudioPatch(int audioPid) {
        int result = 0;
        //defines in audio patch
        //typedef enum {
        //    TIF_HAL_PLAYBACK_AUDIO_SOURCE_CHANNEL_CONFIGURATION_UNKNOWN = 0,
        //    TIF_HAL_PLAYBACK_AUDIO_SOURCE_CHANNEL_CONFIGURATION_C,/**< Center */
        //    TIF_HAL_PLAYBACK_AUDIO_SOURCE_CHANNEL_CONFIGURATION_MONO = TIF_HAL_PLAYBACK_AUDIO_SOURCE_CHANNEL_CONFIGURATION_C,
        //    TIF_HAL_PLAYBACK_AUDIO_SOURCE_CHANNEL_CONFIGURATION_L_R,/**< Left and right speakers */
        //    TIF_HAL_PLAYBACK_AUDIO_SOURCE_CHANNEL_CONFIGURATION_STEREO = TIF_HAL_PLAYBACK_AUDIO_SOURCE_CHANNEL_CONFIGURATION_L_R,
        //    TIF_HAL_PLAYBACK_AUDIO_SOURCE_CHANNEL_CONFIGURATION_L_C_R,/**< Left, center and right speakers */
        //    TIF_HAL_PLAYBACK_AUDIO_SOURCE_CHANNEL_CONFIGURATION_L_R_S,/**< Left, right and surround speakers */
        //    TIF_HAL_PLAYBACK_AUDIO_SOURCE_CHANNEL_CONFIGURATION_L_C_R_S,/**< Left,center right and surround speakers */
        //    TIF_HAL_PLAYBACK_AUDIO_SOURCE_CHANNEL_CONFIGURATION_L_R_SL_RS,/**< Left, right, surround left and surround right */
        //    TIF_HAL_PLAYBACK_AUDIO_SOURCE_CHANNEL_CONFIGURATION_L_C_R_SL_SR,/**< Left, center, right, surround left and surround right */
        //    TIF_HAL_PLAYBACK_AUDIO_SOURCE_CHANNEL_CONFIGURATION_L_C_R_SL_SR_LFE,/**< Left, center, right, surround left, surround right and lfe*/
        //    TIF_HAL_PLAYBACK_AUDIO_SOURCE_CHANNEL_CONFIGURATION_5_1 = TIF_HAL_PLAYBACK_AUDIO_SOURCE_CHANNEL_CONFIGURATION_L_C_R_SL_SR_LFE,
        //   TIF_HAL_PLAYBACK_AUDIO_SOURCE_CHANNEL_CONFIGURATION_L_C_R_SL_SR_RL_RR_LFE, /**< Left, center, right, surround left, surround right, rear left, rear right and lfe */
        //    TIF_HAL_PLAYBACK_AUDIO_SOURCE_CHANNEL_CONFIGURATION_7_1 = TIF_HAL_PLAYBACK_AUDIO_SOURCE_CHANNEL_CONFIGURATION_L_C_R_SL_SR_RL_RR_LFE
        //} TIF_HAL_Playback_AudioSourceChannelConfiguration_t;
        //channel_configuration result = "channel_configuration=1";
        AudioManager audioManager = (AudioManager) this.getSystemService(Context.AUDIO_SERVICE);
        String temp = audioManager.getParameters(ConstantManager.AUDIO_PATCH_COMMAND_GET_AUDIO_CHANNEL_CONFIGURE);
        //Log.d(TAG, "getAudioChannelConfigureFromAudioPatch need add command in audio patch");
        try {
            if (temp != null) {
                String[] splitInfo = temp.split("=");
                if (splitInfo != null && splitInfo.length == 2 && "channel_configuration".equals(splitInfo[0])) {
                    result = Integer.valueOf(splitInfo[1]);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "getAudioChannelConfigureFromAudioPatch Exception = " + e.getMessage());
        }
        Log.d(TAG, "getAudioChannelConfigureFromAudioPatch result = " + result);
        return result;
    }

    private List<TvTrackInfo> getSubtitleTrackInfoList() {
        return getSubtitleTrackInfoList(INDEX_FOR_MAIN);
    }

    private List<TvTrackInfo> getPipSubtitleTrackInfoList() {
        return getSubtitleTrackInfoList(INDEX_FOR_PIP);
    }

    private List<TvTrackInfo> getSubtitleTrackInfoList(int index) {
        List<TvTrackInfo> tracks = new ArrayList<>();
        //subtile tracks
        try {
            List<TvTrackInfo> subTracks = new ArrayList<>();
            JSONArray args = new JSONArray();
            args.put(index);
            JSONArray subtitleStreams = DtvkitGlueClient.getInstance().request("Player.getListOfSubtitleStreams", args).getJSONArray("data");
            int undefinedIndex = 0;
            for (int i = 0; i < subtitleStreams.length(); i++)
            {
                Bundle bundle = new Bundle();
                JSONObject subtitleStream = subtitleStreams.getJSONObject(i);
                Log.d(TAG, "getSubtitleTrackInfoList subtitleStream = " + subtitleStream.toString());
                String trackId = null;
                if (subtitleStream.getBoolean("teletext")) {
                    bundle.putBoolean(ConstantManager.KEY_TVINPUTINFO_SUBTITLE_IS_TELETEXT, true);
                    int teletexttype = subtitleStream.getInt("teletext_type");
                    if (teletexttype == ConstantManager.ADB_TELETEXT_TYPE_SUBTITLE) {
                        bundle.putBoolean(ConstantManager.KEY_TVINPUTINFO_SUBTITLE_IS_HARD_HEARING, false);
                        trackId = "id=" + Integer.toString(subtitleStream.getInt("index")) + "&" + "type=" + "4" + "&teletext=1&hearing=0&flag=TTX";
                    } else if (teletexttype == ConstantManager.ADB_TELETEXT_TYPE_SUBTITLE_HARD_HEARING) {
                        bundle.putBoolean(ConstantManager.KEY_TVINPUTINFO_SUBTITLE_IS_HARD_HEARING, true);
                        trackId = "id=" + Integer.toString(subtitleStream.getInt("index")) + "&" + "type=" + "4" + "&teletext=1&hearing=1&flag=TTX-H.O.H";
                    } else {
                        continue;
                    }
                } else {
                    bundle.putBoolean(ConstantManager.KEY_TVINPUTINFO_SUBTITLE_IS_TELETEXT, false);
                    int subtitletype = subtitleStream.getInt("subtitle_type");
                    bundle.putBoolean(ConstantManager.KEY_TVINPUTINFO_SUBTITLE_IS_HARD_HEARING, false);
                    if (subtitletype >= ConstantManager.ADB_SUBTITLE_TYPE_DVB && subtitletype <= ConstantManager.ADB_SUBTITLE_TYPE_DVB_HD) {
                        trackId = "id=" + Integer.toString(subtitleStream.getInt("index")) + "&" + "type=" + "4" + "&teletext=0&hearing=0&flag=none";//TYPE_DTV_CC
                    } else if (subtitletype >= ConstantManager.ADB_SUBTITLE_TYPE_DVB_HARD_HEARING && subtitletype <= ConstantManager.ADB_SUBTITLE_TYPE_DVB_HARD_HEARING_HD) {
                        bundle.putBoolean(ConstantManager.KEY_TVINPUTINFO_SUBTITLE_IS_HARD_HEARING, true);
                        trackId = "id=" + Integer.toString(subtitleStream.getInt("index")) + "&" + "type=" + "4" + "&teletext=0&hearing=1&flag=H.O.H";//TYPE_DTV_CC
                    } else {
                        trackId = "id=" + Integer.toString(subtitleStream.getInt("index")) + "&" + "type=" + "4" + "&teletext=0&hearing=0&flag=none";//TYPE_DTV_CC
                    }
                }
                TvTrackInfo.Builder track = new TvTrackInfo.Builder(TvTrackInfo.TYPE_SUBTITLE, trackId);
                String subLang = ISO639Data.parse(subtitleStream.getString("language"));
                if (TextUtils.isEmpty(subLang) || ConstantManager.CONSTANT_UND_FLAG.equals(subLang)) {
                    subLang = ConstantManager.CONSTANT_UND_VALUE + ((undefinedIndex>0)?undefinedIndex:"");
                    undefinedIndex++;
                }
                track.setLanguage(subLang);
                int pid = subtitleStream.getInt("pid");
                bundle.putInt(ConstantManager.KEY_TRACK_PID, pid);
                bundle.putInt(ConstantManager.KEY_TVINPUTINFO_SUBTITLE_INDEX, subtitleStream.getInt("index"));
                track.setExtra(bundle);
                subTracks.add(track.build());
            }
            ConstantManager.ascendTrackInfoOderByPid(subTracks);
            tracks.addAll(subTracks);
            List<TvTrackInfo> teleTracks = new ArrayList<>();
            JSONArray args1 = new JSONArray();
            args1.put(index);
            JSONArray teletextStreams = DtvkitGlueClient.getInstance().request("Player.getListOfTeletextStreams", args1).getJSONArray("data");
            undefinedIndex = 0;
            for (int i = 0; i < teletextStreams.length(); i++)
            {
                Bundle bundle = new Bundle();
                JSONObject teletextStream = teletextStreams.getJSONObject(i);
                Log.d(TAG, "getSubtitleTrackInfoList teletextStream = " + teletextStream.toString());
                String trackId = null;
                int teletextType = teletextStream.getInt("teletext_type");
                if (teletextType == 1 || teletextType == 3 || teletextType == 4) {
                    bundle.putBoolean(ConstantManager.KEY_TVINPUTINFO_SUBTITLE_IS_TELETEXT, true);
                    bundle.putBoolean(ConstantManager.KEY_TVINPUTINFO_SUBTITLE_IS_HARD_HEARING, false);
                    trackId = "id=" + Integer.toString(teletextStream.getInt("index")) + "&" + "type=" + "6" + "&teletext=1&hearing=0&flag=none";//TYPE_DTV_TELETEXT_IMG
                } else {
                    continue;
                }
                TvTrackInfo.Builder track = new TvTrackInfo.Builder(TvTrackInfo.TYPE_SUBTITLE, trackId);
                String teleLang = ISO639Data.parse(teletextStream.getString("language"));
                if (TextUtils.isEmpty(teleLang) || ConstantManager.CONSTANT_UND_FLAG.equals(teleLang)) {
                    teleLang = ConstantManager.CONSTANT_UND_VALUE + ((undefinedIndex>0)?undefinedIndex:"");
                    undefinedIndex++;
                }
                track.setLanguage(teleLang);
                int pid = teletextStream.getInt("pid");
                bundle.putInt(ConstantManager.KEY_TRACK_PID, pid);
                bundle.putInt(ConstantManager.KEY_TVINPUTINFO_SUBTITLE_INDEX, teletextStream.getInt("index"));
                track.setExtra(bundle);
                teleTracks.add(track.build());
            }
            ConstantManager.ascendTrackInfoOderByPid(teleTracks);
            tracks.addAll(teleTracks);
        } catch (Exception e) {
            Log.e(TAG, "getSubtitleTrackInfoList Exception = " + e.getMessage());
        }
        return tracks;
    }

    private boolean playerSelectAudioTrack(int index) {
        return playerSelectAudioTrack(INDEX_FOR_MAIN, index);
    }

    private boolean playerPipSelectAudioTrack(int index) {
        return playerSelectAudioTrack(INDEX_FOR_PIP, index);
    }

    private boolean playerSelectAudioTrack(int id, int index) {
        try {
            JSONArray args = new JSONArray();
            args.put(id);
            args.put(index);
            DtvkitGlueClient.getInstance().request("Player.setAudioStream", args);
        } catch (Exception e) {
            Log.e(TAG, "playerSelectAudioTrack = " + e.getMessage());
            return false;
        }
        return true;
    }

    private boolean playersetAudioDescriptionOn(boolean on) {
        return playersetAudioDescriptionOn(INDEX_FOR_MAIN, on);
    }

    private boolean playerPipsetAudioDescriptionOn(boolean on) {
        return playersetAudioDescriptionOn(INDEX_FOR_PIP, on);
    }

    private boolean playersetAudioDescriptionOn(int index, boolean on) {
        try {
            JSONArray args = new JSONArray();
            args.put(index);
            args.put(on);
            DtvkitGlueClient.getInstance().request("Player.setAudioDescriptionOn", args);
        } catch (Exception e) {
            Log.e(TAG, "playersetAudioDescriptionOn = " + e.getMessage());
            return false;
        }
        return true;
    }

    private boolean playergetAudioDescriptionOn() {
        return playergetAudioDescriptionOn(INDEX_FOR_MAIN);
    }

    private boolean playerPipgetAudioDescriptionOn() {
        return playergetAudioDescriptionOn(INDEX_FOR_PIP);
    }

    private boolean playergetAudioDescriptionOn(int index) {
        boolean result = false;
        try {
            JSONArray args = new JSONArray();
            args.put(index);
            result = DtvkitGlueClient.getInstance().request("Player.getAudioDescriptionOn", args).getBoolean("data");
        } catch (Exception e) {
            Log.e(TAG, "playergetAudioDescriptionOn = " + e.getMessage());
            return result;
        }
        return result;
    }

    private boolean playerHasDollyAssociateAudioTrack() {
        boolean result = false;
        List<TvTrackInfo> allAudioTrackList = getAudioTrackInfoList(false);
        if (allAudioTrackList == null || allAudioTrackList.size() < 2) {
            return result;
        }
        Iterator<TvTrackInfo> iterator = allAudioTrackList.iterator();
        while (iterator.hasNext()) {
            TvTrackInfo a = iterator.next();
            if (a != null) {
                Bundle trackBundle = a.getExtra();
                if (trackBundle != null) {
                    String audioCodec = trackBundle.getString(ConstantManager.KEY_TVINPUTINFO_AUDIO_CODEC, null);
                    boolean isAd = trackBundle.getBoolean(ConstantManager.KEY_TVINPUTINFO_AUDIO_AD, false);
                    if (isAd && (TextUtils.equals(audioCodec, "AC3") || TextUtils.equals(audioCodec, "E-AC3"))) {
                        result = true;
                        break;
                    }
                }
            } else {
                continue;
            }
        }
        return result;
    }

    private boolean playerAudioIndexIsAssociate(int index) {
        boolean result = false;
        List<TvTrackInfo> allAudioTrackList = getAudioTrackInfoList(false);
        if (allAudioTrackList == null || allAudioTrackList.size() <= index) {
            return result;
        }
        TvTrackInfo track = allAudioTrackList.get(index);
        if (track != null) {
            Bundle trackBundle = track.getExtra();
            if (trackBundle != null && trackBundle.getBoolean(ConstantManager.KEY_TVINPUTINFO_AUDIO_AD, false)) {
                result = true;
            }
        }
        return result;
    }

    private boolean playerSelectSubtitleTrack(int index) {
        return playerSelectSubtitleTrackById(INDEX_FOR_MAIN, index);
    }

    private boolean playerPipSelectSubtitleTrack(int index) {
        return playerSelectSubtitleTrackById(INDEX_FOR_PIP, index);
    }

    private boolean playerSelectSubtitleTrackById(int id, int index) {
        try {
            JSONArray args = new JSONArray();
            args.put(id);
            args.put(index);
            DtvkitGlueClient.getInstance().request("Player.setSubtitleStream", args);
        } catch (Exception e) {
            Log.e(TAG, "playerSelectSubtitleTrack = " + e.getMessage());
            return false;
        }
        return true;
    }

    private boolean playerSelectTeletextTrack(int index) {
        return playerSelectTeletextTrackById(INDEX_FOR_MAIN, index);
    }

    private boolean playerPipSelectTeletextTrack(int index) {
        return playerSelectTeletextTrackById(INDEX_FOR_PIP, index);
    }

    private boolean playerSelectTeletextTrackById(int id, int index) {
        try {
            JSONArray args = new JSONArray();
            args.put(id);
            args.put(index);
            DtvkitGlueClient.getInstance().request("Player.setTeletextStream", args);
        } catch (Exception e) {
            Log.e(TAG, "playerSelectTeletextTrack = " + e.getMessage());
            return false;
        }
        return true;
    }

    private boolean playerStartTeletext(int index) {
        return playerStartTeletextById(INDEX_FOR_MAIN, index);
    }

    private boolean playerPipStartTeletext(int index) {
        return playerStartTeletextById(INDEX_FOR_PIP, index);
    }

    //called when teletext on
    private boolean playerStartTeletextById(int id, int index) {
        try {
            JSONArray args = new JSONArray();
            args.put(id);
            args.put(index);
            DtvkitGlueClient.getInstance().request("Player.startTeletext", args);
        } catch (Exception e) {
            Log.e(TAG, "playerStartTeletext = " + e.getMessage());
            return false;
        }
        return true;
    }

    private boolean playerStopTeletext() {
        return playerStopTeletext(INDEX_FOR_MAIN);
    }

    private boolean playerPipStopTeletext() {
        return playerStopTeletext(INDEX_FOR_PIP);
    }

    //called when teletext off
    private boolean playerStopTeletext(int index) {
        try {
            JSONArray args = new JSONArray();
            args.put(index);
            DtvkitGlueClient.getInstance().request("Player.stopTeletext", args);
        } catch (Exception e) {
            Log.e(TAG, "playerStopTeletext = " + e.getMessage());
            return false;
        }
        return true;
    }

    private boolean playerPauseTeletext() {
        return playerPauseTeletext(INDEX_FOR_MAIN);
    }

    private boolean playerPipPauseTeletext() {
        return playerPauseTeletext(INDEX_FOR_PIP);
    }

    private boolean playerPauseTeletext(int index) {
        try {
            JSONArray args = new JSONArray();
            args.put(index);
            DtvkitGlueClient.getInstance().request("Player.pauseTeletext", args);
        } catch (Exception e) {
            Log.e(TAG, "playerPauseTeletext = " + e.getMessage());
            return false;
        }
        return true;
    }

    private boolean playerResumeTeletext() {
        return playerResumeTeletext(INDEX_FOR_MAIN);
    }

    private boolean playerPipResumeTeletext() {
        return playerResumeTeletext(INDEX_FOR_PIP);
    }

    private boolean playerResumeTeletext(int index) {
        try {
            JSONArray args = new JSONArray();
            args.put(index);
            DtvkitGlueClient.getInstance().request("Player.resumeTeletext", args);
        } catch (Exception e) {
            Log.e(TAG, "playerResumeTeletext = " + e.getMessage());
            return false;
        }
        return true;
    }

    private boolean playerIsTeletextDisplayed() {
        return playerIsTeletextDisplayed(INDEX_FOR_MAIN);
    }

    private boolean playerPipIsTeletextDisplayed() {
        return playerIsTeletextDisplayed(INDEX_FOR_PIP);
    }

    private boolean playerIsTeletextDisplayed(int index) {
        boolean on = false;
        try {
            JSONArray args = new JSONArray();
            args.put(index);
            on = DtvkitGlueClient.getInstance().request("Player.isTeletextDisplayed", args).getBoolean("data");
        } catch (Exception e) {
            Log.e(TAG, "playerResumeTeletext = " + e.getMessage());
        }
        return on;
    }

    private boolean playerIsTeletextStarted() {
        return playerIsTeletextStarted(INDEX_FOR_MAIN);
    }

    private boolean playerPipIsTeletextStarted() {
        return playerIsTeletextStarted(INDEX_FOR_PIP);
    }

    private boolean playerIsTeletextStarted(int index) {
        boolean on = false;
        try {
            JSONArray args = new JSONArray();
            args.put(index);
            on = DtvkitGlueClient.getInstance().request("Player.isTeletextStarted", args).getBoolean("data");
        } catch (Exception e) {
            Log.e(TAG, "playerIsTeletextStarted = " + e.getMessage());
        }
        Log.d(TAG, "playerIsTeletextStarted on = " + on);
        return on;
    }

    private boolean playerSetTeletextOn(boolean on, int index) {
        return playerSetTeletextOnById(INDEX_FOR_MAIN, on, index);
    }

    private boolean playerPipSetTeletextOn(boolean on, int index) {
        return playerSetTeletextOnById(INDEX_FOR_PIP, on, index);
    }

    private boolean playerSetTeletextOnById(int id, boolean on, int index) {
        boolean result = false;
        if (on) {
            result = playerStartTeletextById(id, index);
        } else {
            result = playerStopTeletext(id);
        }
        return result;
    }

    private boolean playerIsTeletextOn() {
        return playerIsTeletextOn(INDEX_FOR_MAIN);
    }

    private boolean playerPipIsTeletextOn() {
        return playerIsTeletextOn(INDEX_FOR_PIP);
    }

    private boolean playerIsTeletextOn(int index) {
        return playerIsTeletextStarted(index);
    }

    private boolean playerNotifyTeletextEvent(int event) {
        return playerNotifyTeletextEvent(INDEX_FOR_MAIN, event);
    }

    private boolean playerPipNotifyTeletextEvent(int event) {
        return playerNotifyTeletextEvent(INDEX_FOR_PIP, event);
    }

    private boolean playerNotifyTeletextEvent(int index, int event) {
        boolean on = false;
        try {
            JSONArray args = new JSONArray();
            args.put(index);
            args.put(event);
            DtvkitGlueClient.getInstance().request("Player.notifyTeletextEvent", args);
        } catch (Exception e) {
            Log.e(TAG, "playerNotifyTeletextEvent = " + e.getMessage());
            return false;
        }
        return true;
    }

    //status has four value: enter exit play
    private boolean playerNotifyCiProfileEvent(String action, String ciNumber, String profileName, String profileVersion) {
        boolean result = false;
        try {
            JSONArray args = new JSONArray();
            args.put(action);
            args.put(ciNumber);
            args.put(profileName);
            args.put(profileVersion);
            DtvkitGlueClient.getInstance().request("Player.notifyCiProfileEvent", args);
            result = true;
        } catch (Exception e) {
            Log.e(TAG, "playerNotifyCiProfileEvent = " + e.getMessage());
            result = false;
        }
        return result;
    }

    private boolean doOperatorSearch(int module) {
        boolean result = false;
        Log.d(TAG, "doOperatorSearch: " + module);
        try {
            JSONArray args = new JSONArray();
            args.put(module);
            JSONObject resp = DtvkitGlueClient.getInstance().request("Player.doOperatorSearch", args);
            if (resp != null) {
                result = resp.optBoolean("data");
            }
             if (!result) {
                Log.d(TAG, "doOperatorSearch not ok: " + resp.optString("data", ""));
             }
        } catch (Exception e) {
            Log.e(TAG, "doOperatorSearch Exception = " + e.getMessage());
        }
        return result;
    }

    private int[] playerGetDTVKitVideoSize() {
        return playerGetDTVKitVideoSize(INDEX_FOR_MAIN);
    }

    private int[] playerPipGetDTVKitVideoSize() {
        return playerGetDTVKitVideoSize(INDEX_FOR_PIP);
    }

    private int[] playerGetDTVKitVideoSize(int index) {
        int[] result = {0, 0, 0, 0};
        try {
            JSONArray args = new JSONArray();
            args.put(index);
            JSONObject videoStreams = DtvkitGlueClient.getInstance().request("Player.getDTVKitVideoResolution", args);
            if (videoStreams != null) {
                videoStreams = (JSONObject)videoStreams.get("data");
                if (!(videoStreams == null || videoStreams.length() == 0)) {
                    Log.d(TAG, "playerGetDTVKitVideoSize videoStreams = " + videoStreams.toString());
                    result[0] = (int)videoStreams.get("width");
                    result[1] = (int)videoStreams.get("height");
                    result[2] = (int)videoStreams.get("framerate");
                    result[3] = (int)videoStreams.get("progressive");//1 means progressive
                }
            } else {
                Log.d(TAG, "playerGetDTVKitVideoSize then get null");
            }
        } catch (Exception e) {
            Log.e(TAG, "playerGetDTVKitVideoSize = " + e.getMessage());
        }
        return result;
    }

    private String playerGetDTVKitVideoCodec() {
        return playerGetDTVKitVideoCodec(INDEX_FOR_MAIN);
    }

    private String playerPipGetDTVKitVideoCodec() {
        return playerGetDTVKitVideoCodec(INDEX_FOR_PIP);
    }

    private String playerGetDTVKitVideoCodec(int index) {
        String result = "";
        try {
            JSONArray args = new JSONArray();
            args.put(index);
            JSONObject videoStreams = DtvkitGlueClient.getInstance().request("Player.getDTVKitVideoCodec", args);
            if (videoStreams != null) {
                videoStreams = (JSONObject)videoStreams.get("data");
                if (!(videoStreams == null || videoStreams.length() == 0)) {
                    Log.d(TAG, "playerGetDTVKitVideoCodec videoStreams = " + videoStreams.toString());
                    //int videoPid = (int)videoStreams.get("video_pid");
                    result = videoStreams.getString("video_codec");
                }
            } else {
                Log.d(TAG, "playerGetDTVKitVideoCodec then get null");
            }
        } catch (Exception e) {
            Log.e(TAG, "playerGetDTVKitVideoCodec = " + e.getMessage());
        }
        return result;
    }

    private int playerGetSelectedSubtitleTrack() {
        return playerGetSelectedSubtitleTrack(INDEX_FOR_MAIN);
    }

    private int playerPipGetSelectedSubtitleTrack() {
        return playerGetSelectedSubtitleTrack(INDEX_FOR_PIP);
    }

    private int playerGetSelectedSubtitleTrack(int id) {
        int index = 0xFFFF;
        try {
            JSONArray args = new JSONArray();
            args.put(id);
            args.put(index);
            JSONArray subtitleStreams = DtvkitGlueClient.getInstance().request("Player.getListOfSubtitleStreams", args).getJSONArray("data");
            for (int i = 0; i < subtitleStreams.length(); i++)
            {
                JSONObject subtitleStream = subtitleStreams.getJSONObject(i);
                if (subtitleStream.getBoolean("selected")) {
                    boolean isTele = subtitleStream.getBoolean("teletext");
                    if (isTele) {
                        int teleType = subtitleStream.getInt("teletext_type");
                        if (teleType != ConstantManager.ADB_TELETEXT_TYPE_SUBTITLE && teleType != ConstantManager.ADB_TELETEXT_TYPE_SUBTITLE_HARD_HEARING) {
                            Log.i(TAG, "playerGetSelectedSubtitleTrack skip teletext");
                            continue;
                        }
                    }
                    index = subtitleStream.getInt("index");
                    Log.i(TAG, "playerGetSelectedSubtitleTrack index = " + index);
                    break;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "playerGetSelectedSubtitleTrack = " + e.getMessage());
        }
        Log.i(TAG, "playerGetSelectedSubtitleTrack index = " + index);
        return index;
    }

    private String playerGetSelectedSubtitleTrackId() {
        return playerGetSelectedSubtitleTrackId(INDEX_FOR_MAIN);
    }

    private String playerPipGetSelectedSubtitleTrackId() {
        return playerGetSelectedSubtitleTrackId(INDEX_FOR_PIP);
    }

    private String playerGetSelectedSubtitleTrackId(int index) {
        String trackId = null;
        try {
            JSONArray args = new JSONArray();
            args.put(index);
            JSONArray subtitleStreams = DtvkitGlueClient.getInstance().request("Player.getListOfSubtitleStreams", args).getJSONArray("data");
            for (int i = 0; i < subtitleStreams.length(); i++)
            {
                JSONObject subtitleStream = subtitleStreams.getJSONObject(i);
                if (subtitleStream.getBoolean("selected")) {
                    boolean isTele = subtitleStream.getBoolean("teletext");
                    if (isTele) {
                        int teletexttype = subtitleStream.getInt("teletext_type");
                        if (teletexttype == ConstantManager.ADB_TELETEXT_TYPE_SUBTITLE) {
                            trackId = "id=" + Integer.toString(subtitleStream.getInt("index")) + "&" + "type=" + "4" + "&teletext=1&hearing=0&flag=TTX";//tele sub
                        } else if (teletexttype == ConstantManager.ADB_TELETEXT_TYPE_SUBTITLE_HARD_HEARING) {
                            trackId = "id=" + Integer.toString(subtitleStream.getInt("index")) + "&" + "type=" + "4" + "&teletext=1&hearing=1&flag=TTX-H.O.H";//tele sub
                        } else {
                            continue;
                        }
                    } else {
                        int subtitletype = subtitleStream.getInt("subtitle_type");
                        if (subtitletype >= ConstantManager.ADB_SUBTITLE_TYPE_DVB && subtitletype <= ConstantManager.ADB_SUBTITLE_TYPE_DVB_HD) {
                            trackId = "id=" + Integer.toString(subtitleStream.getInt("index")) + "&" + "type=" + "4" + "&teletext=0&hearing=0&flag=none";//TYPE_DTV_CC
                        } else if (subtitletype >= ConstantManager.ADB_SUBTITLE_TYPE_DVB_HARD_HEARING && subtitletype <= ConstantManager.ADB_SUBTITLE_TYPE_DVB_HARD_HEARING_HD) {
                            trackId = "id=" + Integer.toString(subtitleStream.getInt("index")) + "&" + "type=" + "4" + "&teletext=0&hearing=1&flag=H.O.H";//TYPE_DTV_CC
                        } else {
                            trackId = "id=" + Integer.toString(subtitleStream.getInt("index")) + "&" + "type=" + "4" + "&teletext=0&hearing=0&flag=none";//TYPE_DTV_CC
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "playerGetSelectedSubtitleTrackId = " + e.getMessage());
        }
        Log.i(TAG, "playerGetSelectedTeleTextTrack trackId = " + trackId);
        return trackId;
    }

    private int playerGetSelectedTeleTextTrack() {
        return playerGetSelectedTeleTextTrack(INDEX_FOR_MAIN);
    }

    private int playerPipGetSelectedTeleTextTrack() {
        return playerGetSelectedTeleTextTrack(INDEX_FOR_PIP);
    }

    private int playerGetSelectedTeleTextTrack(int id) {
        int index = 0xFFFF;
        try {
            JSONArray args = new JSONArray();
            args.put(id);
            args.put(index);
            JSONArray teletextStreams = DtvkitGlueClient.getInstance().request("Player.getListOfTeletextStreams", args).getJSONArray("data");
            for (int i = 0; i < teletextStreams.length(); i++)
            {
                JSONObject teletextStream = teletextStreams.getJSONObject(i);
                if (teletextStream.getBoolean("selected")) {
                    int teleType = teletextStream.getInt("teletext_type");
                    if (teleType == ConstantManager.ADB_TELETEXT_TYPE_SUBTITLE || teleType == ConstantManager.ADB_TELETEXT_TYPE_SUBTITLE_HARD_HEARING) {
                        Log.i(TAG, "playerGetSelectedTeleTextTrack skip tele sub");
                        continue;
                    }
                    index = teletextStream.getInt("index");
                    Log.i(TAG, "playerGetSelectedTeleTextTrack index = " + index);
                    break;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "playerGetSelectedTeleTextTrack Exception = " + e.getMessage());
        }
        return index;
    }

    private String playerGetSelectedTeleTextTrackId() {
        return playerGetSelectedTeleTextTrackId(INDEX_FOR_MAIN);
    }

    private String playerPipGetSelectedTeleTextTrackId() {
        return playerGetSelectedTeleTextTrackId(INDEX_FOR_PIP);
    }

    private String playerGetSelectedTeleTextTrackId(int index) {
        String trackId = null;
        try {
            JSONArray args = new JSONArray();
            args.put(index);
            JSONArray teletextStreams = DtvkitGlueClient.getInstance().request("Player.getListOfTeletextStreams", args).getJSONArray("data");
            for (int i = 0; i < teletextStreams.length(); i++)
            {
                JSONObject teletextStream = teletextStreams.getJSONObject(i);
                if (teletextStream.getBoolean("selected")) {
                    int teleType = teletextStream.getInt("teletext_type");
                    if (teleType == ConstantManager.ADB_TELETEXT_TYPE_SUBTITLE || teleType == ConstantManager.ADB_TELETEXT_TYPE_SUBTITLE_HARD_HEARING) {
                        Log.i(TAG, "playerGetSelectedTeleTextTrackId skip tele sub");
                        continue;
                    }
                    trackId = "id=" + Integer.toString(teletextStream.getInt("index")) + "&type=6" + "&teletext=1&hearing=0&flag=TTX";//TYPE_DTV_TELETEXT_IMG
                    Log.i(TAG, "playerGetSelectedTeleTextTrackId trackId = " + trackId);
                    break;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "playerGetSelectedTeleTextTrackId Exception = " + e.getMessage());
        }
        Log.i(TAG, "playerGetSelectedTeleTextTrackId trackId = " + trackId);
        return trackId;
    }

    private int playerGetSelectedAudioTrack() {
        return playerGetSelectedAudioTrack(INDEX_FOR_MAIN);
    }

    private int playerPipGetSelectedAudioTrack() {
        return playerGetSelectedAudioTrack(INDEX_FOR_PIP);
    }

    private int playerGetSelectedAudioTrack(int id) {
        int index = 0xFFFF;
        try {
            JSONArray args = new JSONArray();
            args.put(id);
            args.put(index);
            JSONArray audioStreams = DtvkitGlueClient.getInstance().request("Player.getListOfAudioStreams", args).getJSONArray("data");
            for (int i = 0; i < audioStreams.length(); i++)
            {
                JSONObject audioStream = audioStreams.getJSONObject(i);
                if (audioStream.getBoolean("selected")) {
                    index = audioStream.getInt("index");
                    Log.i(TAG, "playerGetSelectedAudioTrack index = " + index);
                    break;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "playerGetSelectedAudioTrack = " + e.getMessage());
        }
        return index;
    }

    private boolean playerGetSubtitlesOn() {
        return playerGetSubtitlesOn(INDEX_FOR_MAIN);
    }

    private boolean playerPipGetSubtitlesOn() {
        return playerGetSubtitlesOn(INDEX_FOR_PIP);
    }

    private boolean playerGetSubtitlesOn(int index) {
        boolean on = false;
        try {
            JSONArray args = new JSONArray();
            args.put(index);
            on = DtvkitGlueClient.getInstance().request("Player.getSubtitlesOn", args).getBoolean("data");
        } catch (Exception e) {
            Log.e(TAG, "playerGetSubtitlesOn = " + e.getMessage());
        }
        Log.i(TAG, "playerGetSubtitlesOn on = " + on);
        return on;
    }

    private boolean getCurrentCountryIsNordig() {
        boolean is_nordig = false;

        try {
            JSONArray args = new JSONArray();
            is_nordig = DtvkitGlueClient.getInstance().request("Dvb.isNordigCountry", args).getBoolean("data");
            Log.i(TAG, "getCurrentCountryIsNordig:" + is_nordig);
        } catch (Exception e) {
            Log.e(TAG, "getCurrentCountryIsNordig " + e.getMessage());
        }
        return is_nordig;
    }

    private int getParentalControlAge() {
        int age = 0;

        try {
            JSONArray args = new JSONArray();
            age = DtvkitGlueClient.getInstance().request("Dvb.getParentalControlAge", args).getInt("data");
            Log.i(TAG, "getParentalControlAge:" + age);
        } catch (Exception e) {
            Log.e(TAG, "getParentalControlAge " + e.getMessage());
        }
        return age;
    }

    private void setParentalControlAge(int age) {

        try {
            JSONArray args = new JSONArray();
            args.put(age);
            DtvkitGlueClient.getInstance().request("Dvb.setParentalControlAge", args);
            Log.i(TAG, "setParentalControlAge:" + age);
        } catch (Exception e) {
            Log.e(TAG, "setParentalControlAge " + e.getMessage());
        }
    }

    private boolean getParentalControlOn() {
        boolean parentalctrl_enabled = false;
        try {
            JSONArray args = new JSONArray();
            parentalctrl_enabled = DtvkitGlueClient.getInstance().request("Dvb.getParentalControlOn", args).getBoolean("data");;
            Log.i(TAG, "getParentalControlOn:" + parentalctrl_enabled);
        } catch (Exception e) {
            Log.e(TAG, "getParentalControlOn " + e.getMessage());
        }
        return parentalctrl_enabled;
    }

    private void setParentalControlOn(boolean parentalctrl_enabled) {
        try {
            JSONArray args = new JSONArray();
            args.put(parentalctrl_enabled);
            DtvkitGlueClient.getInstance().request("Dvb.setParentalControlOn", args);
            Log.i(TAG, "setParentalControlOn:" + parentalctrl_enabled);
        } catch (Exception e) {
            Log.e(TAG, "setParentalControlOn " + e.getMessage());
        }
    }

    private int getCurrentMinAgeByBlockedRatings() {
        List<TvContentRating> ratingList = mTvInputManager.getBlockedRatings();
        String rating_system;
        String parentcontrol_rating;
        int min_age = 0xFF;
        for (int i = 0; i < ratingList.size(); i++)
        {
            parentcontrol_rating = ratingList.get(i).getMainRating();
            rating_system = ratingList.get(i).getRatingSystem();
            if (rating_system.equals("DVB"))
            {
                String[] ageArry = parentcontrol_rating.split("_", 2);
                if (ageArry[0].equals("DVB"))
                {
                   int age_temp = Integer.valueOf(ageArry[1]);
                   min_age = min_age < age_temp ? min_age : age_temp;
                }
            }
        }
        return min_age;
    }

    private void mhegSuspend() {
        Log.e(TAG, "Mheg suspending");
        try {
            JSONArray args = new JSONArray();
            DtvkitGlueClient.getInstance().request("Mheg.suspend", args);
            Log.e(TAG, "Mheg suspended");
        } catch (Exception e) {
            Log.e(TAG, "mhegSuspend" + e.getMessage());
        }
        lastMhegUri = null;
    }

    private int mhegGetNextTuneInfo(String dvbUri) {
        int quiet = -1;
        try {
            JSONArray args = new JSONArray();
            args.put(dvbUri);
            quiet = DtvkitGlueClient.getInstance().request("Mheg.getTuneInfo", args).getInt("data");
            Log.e(TAG, "Tune info: "+ quiet);
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }
        return quiet;
    }

    private JSONObject playerGetStatus() {
        return playerGetStatus(INDEX_FOR_MAIN);
    }

    private JSONObject playerPipGetStatus() {
        return playerGetStatus(INDEX_FOR_PIP);
    }

    private JSONObject playerGetStatus(int index) {
        JSONObject response = null;
        try {
            JSONArray args = new JSONArray();
            args.put(index);
            response = DtvkitGlueClient.getInstance().request("Player.getStatus", args).getJSONObject("data");
        } catch (Exception e) {
            Log.e(TAG, "playerGetStatus = " + e.getMessage());
        }
        return response;
    }

    private int[] getSignalStatus() {
        int[] result = {0, 0};
        try {
            JSONArray args1 = new JSONArray();
            args1.put(0);
            JSONObject jsonObj = DtvkitGlueClient.getInstance().request("Dvb.getFrontend", args1);
            if (jsonObj != null) {
                Log.d(TAG, "getSignalStatus resultObj:" + jsonObj.toString());
            } else {
                Log.d(TAG, "getSignalStatus then get null");
            }
            JSONObject data = null;
            if (jsonObj != null) {
                data = (JSONObject)jsonObj.get("data");
            }
            if (data == null || data.length() == 0) {
                return result;
            } else {
                result[0] = (int)(data.get("strength"));
                result[1] = (int)(data.get("integrity"));
                return result;
            }
        } catch (Exception e) {
            Log.d(TAG, "getSignalStatus Exception " + e.getMessage() + ", trace=" + e.getStackTrace());
            e.printStackTrace();
        }
        return result;
    }

    private long playerGetElapsed() {
        return playerGetElapsedAndTruncated(playerGetStatus())[0];
    }

    private long[] playerGetElapsedAndTruncated() {
        return playerGetElapsedAndTruncated(playerGetStatus());
    }

    private long[] playerGetElapsedAndTruncated(JSONObject playerStatus) {
        long[] result = {0, 0, 0};
        if (playerStatus != null) {
            try {
                JSONObject content = playerStatus.getJSONObject("content");
                if (content.has("elapsed")) {
                    result[0] = content.getLong("elapsed");
                }
                if (content.has("truncated")) {
                    result[1] = content.getLong("truncated");
                }
                if (content.has("length")) {
                    result[2] = content.getLong("length");
                }
            } catch (JSONException e) {
                Log.e(TAG, "playerGetElapsedAndTruncated = " + e.getMessage());
            }
        }
        return result;
    }

    private JSONObject playerGetTimeshiftRecorderStatus() {
        return playerGetTimeshiftRecorderStatus(INDEX_FOR_MAIN);
    }

    private JSONObject playerPipGetTimeshiftRecorderStatus() {
        return playerGetTimeshiftRecorderStatus(INDEX_FOR_PIP);
    }

    private JSONObject playerGetTimeshiftRecorderStatus(int index) {
        JSONObject response = null;
        try {
            JSONArray args = new JSONArray();
            args.put(index);
            response = DtvkitGlueClient.getInstance().request("Player.getTimeshiftRecorderStatus", args).getJSONObject("data");
        } catch (Exception e) {
            Log.e(TAG, "playerGetTimeshiftRecorderStatus = " + e.getMessage());
        }
        return response;
    }

    private JSONObject playerGetTimeshiftBufferSize() {
        JSONObject response = null;
        try {
            JSONArray args = new JSONArray();
            response = DtvkitGlueClient.getInstance().request("Player.getTimeshiftBufferSize", args);
        } catch (Exception e) {
            Log.e(TAG, "playerGetTimeshiftBufferSize = " + e.getMessage());
        }
        return response;
    }

    private int playerGetTimeshiftBufferSizeMins() {
        int timeshiftBufferSize = 0;
        JSONObject sizeObj = playerGetTimeshiftBufferSize();
        if (sizeObj != null) {
            try {
                if (sizeObj.has("mins"))
                    timeshiftBufferSize = sizeObj.getInt("mins");
            } catch (Exception e) {
                Log.e(TAG, "playerGetTimeshiftBufferSizeMins = " + e.getMessage());
            }
        }
        return timeshiftBufferSize;
    }

    private int playerGetTimeshiftBufferSizeMegabytes() {
        int timeshiftBufferSize = 0;
        JSONObject sizeObj = playerGetTimeshiftBufferSize();
        if (sizeObj != null) {
            try {
                if (sizeObj.has("mbs"))
                    timeshiftBufferSize = sizeObj.getInt("mbs");
            } catch (Exception e) {
                Log.e(TAG, "playerGetTimeshiftBufferSizeMegabytes = " + e.getMessage());
            }
        }
        return timeshiftBufferSize;
    }

    private boolean playerSetTimeshiftBufferSize(int timeshiftBufferSizeMins, int timeshiftBufferSizeMBs) {
        try {
            JSONArray args = new JSONArray();
            args.put(timeshiftBufferSizeMins);
            args.put(timeshiftBufferSizeMBs);
            DtvkitGlueClient.getInstance().request("Player.setTimeshiftBufferSize", args);
            return true;
        } catch (Exception e) {
            Log.e(TAG, "playerSetTimeshiftBufferSize = " + e.getMessage());
            return false;
        }
    }

    private boolean recordingSetDefaultDisk(String disk_path) {
        try {
            Log.d(TAG, "setDefaultDisk: " + disk_path);
            JSONArray args = new JSONArray();
            args.put(disk_path);
            DtvkitGlueClient.getInstance().request("Recording.setDefaultDisk", args);
            return true;
        } catch (Exception e) {
            Log.e(TAG, "recordingSetDefaultDisk = " + e.getMessage());
            return false;
        }
    }

    private boolean recordingAddDiskPath(String diskPath) {
        try {
            Log.d(TAG, "addDiskPath: " + diskPath);
            JSONArray args = new JSONArray();
            args.put(diskPath);
            DtvkitGlueClient.getInstance().request("Recording.addDiskPath", args);
            return true;
        } catch (Exception e) {
            Log.e(TAG, "recordingAddDiskPath = " + e.getMessage());
            return false;
        }
    }

    private String playerGetTimeshiftRecorderState(JSONObject playerTimeshiftRecorderStatus) {
        String timeshiftRecorderState = "off";
        if (playerTimeshiftRecorderStatus != null) {
            try {
                if (playerTimeshiftRecorderStatus.has("timeshiftrecorderstate")) {
                    timeshiftRecorderState = playerTimeshiftRecorderStatus.getString("timeshiftrecorderstate");
                }
            } catch (JSONException e) {
                Log.e(TAG, "playerGetTimeshiftRecorderState = " + e.getMessage());
            }
        }

        return timeshiftRecorderState;
    }

    private void startMheg(String dvbUri, boolean mhegSuspend) {
        Log.d(TAG, "Mheg dvbUri =" + dvbUri + "Mheg lastDvbUri = " + lastMhegUri);
        if (dvbUri.equals(lastMhegUri)) {
            Log.d(TAG, "startMheg mMhegStarted and mheg dvbUri is the same!!");
            return;
        } else if (lastMhegUri != null) {
            mhegStop();
            Log.d(TAG, "mheg stop in start mheg function");
        }
        if (mhegSuspend) {
            mhegSuspend();
        }
        if (dvbUri != null) {
            if (mhegStartService(dvbUri) != -1) {
                lastMhegUri = dvbUri;
                Log.d(TAG, "startMheg mhegStarted");
            } else {
                Log.d(TAG, "startMheg mheg failed to start");
            }
        } else {
            Log.d(TAG, "startMheg null dvbUri");
        }
    }

    private int mhegStartService(String dvbUri) {
        int quiet = -1;
        try {
            JSONArray args = new JSONArray();
            args.put(dvbUri);
            quiet = DtvkitGlueClient.getInstance().request("Mheg.start", args).getInt("data");
            Log.e(TAG, "Mheg started");
        } catch (Exception e) {
            Log.e(TAG, "mhegStartService = " + e.getMessage());
        }
        return quiet;
    }

    private void mhegStop() {
        try {
            JSONArray args = new JSONArray();
            DtvkitGlueClient.getInstance().request("Mheg.stop", args);
            lastMhegUri = null;
            Log.e(TAG, "Mheg stopped");
        } catch (Exception e) {
            Log.e(TAG, "mhegStop = " + e.getMessage());
        }
    }
    private boolean mhegKeypress(int keyCode) {
      boolean used=false;
        try {
            JSONArray args = new JSONArray();
            args.put(keyCode);
            used = DtvkitGlueClient.getInstance().request("Mheg.notifyKeypress", args).getBoolean("data");
            Log.e(TAG, "Mheg keypress, used:" + used);
        } catch (Exception e) {
            Log.e(TAG, "mhegKeypress = " + e.getMessage());
        }
        return used;
    }

    private final ContentObserver mContentObserver = new ContentObserver(new Handler()) {
        @Override
        public void onChange(boolean selfChange) {
            onChannelsChanged();
        }
    };

   private boolean recordingAddRecording(String dvbUri, boolean eventTriggered, long duration, StringBuffer response) {
       try {
           JSONArray args = new JSONArray();
           args.put(dvbUri);
           args.put(eventTriggered);
           args.put(duration);
           response.insert(0, DtvkitGlueClient.getInstance().request("Recording.addScheduledRecording", args).getString("data"));
           return true;
       } catch (Exception e) {
           Log.e(TAG, e.getMessage());
           response.insert(0, e.getMessage());
           return false;
       }
   }

   private boolean checkActiveRecording() {
        return checkActiveRecording(recordingGetStatus());
   }

   private boolean checkActiveRecording(JSONObject recordingStatus) {
        boolean active = false;

        if (recordingStatus != null) {
            try {
                active = recordingStatus.getBoolean("active");
            } catch (JSONException e) {
                Log.e(TAG, e.getMessage());
            }
        }
        return active;
   }

   private JSONObject recordingGetStatus() {
       JSONObject response = null;
       try {
           JSONArray args = new JSONArray();
           response = DtvkitGlueClient.getInstance().request("Recording.getStatus", args).getJSONObject("data");
       } catch (Exception e) {
           Log.e(TAG, e.getMessage());
       }
       return response;
   }

   private boolean recordingStartRecording(String dvbUri, int path, StringBuffer response) {
       try {
           JSONArray args = new JSONArray();
           args.put(dvbUri);
           args.put(path);
           response.insert(0, DtvkitGlueClient.getInstance().request("Recording.startRecording", args).getString("data"));
           return true;
       } catch (Exception e) {
           Log.e(TAG, e.getMessage());
           response.insert(0, e.getMessage());
           return false;
       }
   }

   private boolean recordingStopRecording(String dvbUri) {
       try {
           JSONArray args = new JSONArray();
           args.put(dvbUri);
           DtvkitGlueClient.getInstance().request("Recording.stopRecording", args);
       } catch (Exception e) {
           Log.e(TAG, e.getMessage());
           return false;
       }
       return true;
   }

   private boolean recordingCheckAvailability(String dvbUri) {
       try {
           JSONArray args = new JSONArray();
           args.put(dvbUri);
           DtvkitGlueClient.getInstance().request("Recording.checkAvailability", args);
       } catch (Exception e) {
           Log.e(TAG, e.getMessage());
           return false;
       }
       return true;
   }

   private JSONObject recordingTune(String dvbUri, StringBuffer response) {
       JSONObject tune = null;
       try {
           JSONArray args = new JSONArray();
           args.put(dvbUri);
           tune = DtvkitGlueClient.getInstance().request("Recording.tune", args).getJSONObject("data");
           Log.d(TAG, "recordingTune: "+ tune.toString());
       } catch (Exception e) {
           Log.e(TAG, e.getMessage());
           response.insert(0, e.getMessage());
       }
       return tune;
   }

   private boolean recordingUntune(int path) {
       try {
           JSONArray args = new JSONArray();
           args.put(path);
           DtvkitGlueClient.getInstance().request("Recording.unTune", args).get("data");
       } catch (Exception e) {
           Log.e(TAG, e.getMessage());
       }
       return true;
   }


   private int getRecordingTunePath(JSONObject tuneStat) {
      int path = -1;
      try {
          path = (tuneStat != null ? tuneStat.getInt("path") : 255);
      } catch (Exception e) {
      }
      return path;
   }

   private boolean getRecordingTuned(JSONObject tuneStat) {
      boolean tuned = false;
      try {
          tuned = (tuneStat != null ? tuneStat.getBoolean("tuned") : false);
      } catch (Exception e) {
      }
      return tuned;
   }

   private String getProgramInternalRecordingUri() {
        return getProgramInternalRecordingUri(recordingGetStatus());
   }

   private String getProgramInternalRecordingUri(JSONObject recordingStatus) {
        String uri = "dvb://0000.0000.0000.0000;0000";
        if (recordingStatus != null) {
           try {
               JSONArray activeRecordings = recordingStatus.getJSONArray("activerecordings");
               if (activeRecordings.length() == 1)
               {
                   uri = activeRecordings.getJSONObject(0).getString("uri");
               }
           } catch (JSONException e) {
               Log.e(TAG, e.getMessage());
           }
       }
       return uri;
   }

   private JSONArray recordingGetActiveRecordings() {
       return recordingGetActiveRecordings(recordingGetStatus());
   }

   private JSONArray recordingGetActiveRecordings(JSONObject recordingStatus) {
       JSONArray activeRecordings = null;
       if (recordingStatus != null) {
            try {
                activeRecordings = recordingStatus.getJSONArray("activerecordings");
            } catch (JSONException e) {
                Log.e(TAG, e.getMessage());
            }
       }
       return activeRecordings;
   }

   private boolean checkActiveRecordings(JSONArray activeRecordings, Predicate<JSONObject> check) {
       boolean checkedTrue = false;
       if (activeRecordings != null) {
            try {
                for (int i = 0; i < activeRecordings.length(); i++) {
                    if (check.test(activeRecordings.getJSONObject(i))) {
                        checkedTrue = true;
                        break;
                    }
                }
            } catch (JSONException e) {
                Log.e(TAG, e.getMessage());
            }
       }
       return checkedTrue;
   }

   private boolean recordingIsRecordingPathActive(JSONObject recordingStatus, int path) {
       boolean active = false;
       if (recordingStatus != null) {
            try {
                JSONArray activeRecordings = null;
                activeRecordings = recordingStatus.getJSONArray("activerecordings");
                if (activeRecordings != null) {
                    for (int i = 0; i < activeRecordings.length(); i++) {
                        int activePath = activeRecordings.getJSONObject(i).getInt("path");
                        if (activePath == path) {
                            active = true;
                            break;
                        }
                    }
                }
            } catch (JSONException e) {
                Log.e(TAG, e.getMessage());
            }
       }
       return active;
   }


   private int recordingGetNumActiveRecordings() {
        int numRecordings = 0;
        JSONArray activeRecordings = recordingGetActiveRecordings();
        if (activeRecordings != null) {
            numRecordings = activeRecordings.length();
        }
        return numRecordings;
   }

   private int recordingGetNumRecorders() {
       int numRecorders = 0;
       try {
           JSONArray args = new JSONArray();
           numRecorders = DtvkitGlueClient.getInstance().request("Recording.getNumberOfRecorders", args).getInt("data");
           Log.i(TAG, "numRecorders: " + numRecorders);
       } catch (Exception e) {
           Log.e(TAG, e.getMessage());
       }
       return numRecorders;
   }

   private JSONArray recordingGetListOfRecordings() {
       JSONArray recordings = null;
       try {
           JSONArray args = new JSONArray();
           recordings = DtvkitGlueClient.getInstance().request("Recording.getListOfRecordings", args).getJSONArray("data");
       } catch (Exception e) {
           Log.e(TAG, e.getMessage());
       }
       return recordings;
   }

   private boolean recordingRemoveRecording(String uri) {
       try {
           JSONArray args = new JSONArray();
           args.put(uri);
           DtvkitGlueClient.getInstance().request("Recording.removeRecording", args);
           return true;
       } catch (Exception e) {
           Log.e(TAG, e.getMessage());
           return false;
       }
   }

   private int getNumTuners() {
       int numTuners = 0;
       try {
           JSONArray args = new JSONArray();
           args = DtvkitGlueClient.getInstance().request("Dvb.getListOfFrontends", args).getJSONArray("data");
           if (args != null) {
               Log.d(TAG, "getNumTuners args = " + args);
               numTuners = args.length();
           }
           Log.i(TAG, "numTuners: " + numTuners);
       } catch (Exception e) {
           Log.e(TAG, "getNumTuners = " + e.getMessage());
       }
       return numTuners;
   }

   private boolean checkRecordingExists(String uri, Cursor cursor) {
        boolean recordingExists = false;
        if (cursor != null && cursor.moveToFirst()) {
            do {
                RecordedProgram recordedProgram = RecordedProgram.fromCursor(cursor);
                if (recordedProgram.getRecordingDataUri().equals(uri)) {
                    recordingExists = true;
                    break;
                }
            } while (cursor.moveToNext());
        }
        return recordingExists;
   }

   private JSONArray recordingGetListOfScheduledRecordings() {
       JSONArray scheduledRecordings = null;
       try {
           JSONArray args = new JSONArray();
           scheduledRecordings = DtvkitGlueClient.getInstance().request("Recording.getListOfScheduledRecordings", args).getJSONArray("data");
       } catch (Exception e) {
           Log.e(TAG, e.getMessage());
       }
       return scheduledRecordings;
   }

   private String getScheduledRecordingUri(JSONObject scheduledRecording) {
        String uri = "dvb://0000.0000.0000;0000";
        if (scheduledRecording != null) {
            try {
                uri = scheduledRecording.getString("uri");
            } catch (JSONException e) {
                Log.e(TAG, e.getMessage());
            }
        }
        return uri;
   }

   private boolean recordingRemoveScheduledRecording(String uri) {
       try {
           JSONArray args = new JSONArray();
           args.put(uri);
           DtvkitGlueClient.getInstance().request("Recording.removeScheduledRecording", args);
           return true;
       } catch (Exception e) {
           Log.e(TAG, e.getMessage());
           return false;
       }
   }

    private final ContentObserver mRecordingsContentObserver = new ContentObserver(new Handler()) {
        @RequiresApi(api = Build.VERSION_CODES.N)
        @Override
        public void onChange(boolean selfChange) {
            onRecordingsChanged();
        }
    };

   @RequiresApi(api = Build.VERSION_CODES.N)
   private void onRecordingsChanged() {
       Log.i(TAG, "onRecordingsChanged");

       new Thread(new Runnable() {
           @Override
           public void run() {
               Cursor cursor = null;
               JSONArray recordings = null;
               JSONArray activeRecordings = null;

               try {
                   cursor = mContentResolver.query(TvContract.RecordedPrograms.CONTENT_URI, RecordedProgram.PROJECTION, null, null, TvContract.RecordedPrograms._ID + " DESC");
                   recordings = recordingGetListOfRecordings();
                   activeRecordings = recordingGetActiveRecordings();

                   if (recordings != null && cursor != null) {
                       for (int i = 0; i < recordings.length(); i++) {
                           try {
                               String uri = recordings.getJSONObject(i).getString("uri");

                               if (activeRecordings != null && activeRecordings.length() > 0) {
                                   boolean activeRecording = false;
                                   for (int j = 0; j < activeRecordings.length(); j++) {
                                       if (uri.equals(activeRecordings.getJSONObject(j).getString("uri"))) {
                                           activeRecording = true;
                                           break;
                                       }
                                   }
                                   if (activeRecording) {
                                       continue;
                                   }
                               }

                               if (!checkRecordingExists(uri, cursor)) {
                                   Log.d(TAG, "remove invalid recording: "+uri);
                                   recordingRemoveRecording(uri);
                               }

                           } catch (JSONException e) {
                               Log.e(TAG, e.getMessage());
                           }
                       }
                   }
               } catch (Exception e) {
                   Log.e(TAG, "RecordedPrograms query Failed = " + e.getMessage());
               } finally {
                   if (cursor != null) {
                       cursor.close();
                   }
               }
           }
       }).start();
    }

    private HardwareCallback mHardwareCallback = new HardwareCallback(){
        @Override
        public void onReleased() {
            Log.d(TAG, "onReleased");
            mHardware = null;
        }

        @Override
        public void onStreamConfigChanged(TvStreamConfig[] configs) {
            Log.d(TAG, "onStreamConfigChanged");
            if (configs != null) {
                Log.d(TAG, "onStreamConfigChanged configs length = " + configs.length);
            } else {
                Log.d(TAG, "onStreamConfigChanged null");
            }
            mConfigs = configs;
        }
    };

    private HardwareCallback mPipHardwareCallback = new HardwareCallback(){
        @Override
        public void onReleased() {
            Log.d(TAG, "mPipHardwareCallback onReleased");
            mPipHardware = null;
        }

        @Override
        public void onStreamConfigChanged(TvStreamConfig[] configs) {
            Log.d(TAG, "mPipHardwareCallback onStreamConfigChanged");
            if (configs != null) {
                Log.d(TAG, "mPipHardwareCallback onStreamConfigChanged configs length = " + configs.length);
            } else {
                Log.d(TAG, "mPipHardwareCallback onStreamConfigChanged null");
            }
            mPipConfigs = configs;
        }
    };

    private TvInputInfo buildTvInputInfo(TvInputHardwareInfo hardwareInfo, int tunerCount, Bundle extras) {
        TvInputInfo result = null;
        try {
            result = new TvInputInfo.Builder(getApplicationContext(), new ComponentName(getApplicationContext(), DtvkitTvInput.class))
                    .setTvInputHardwareInfo(hardwareInfo)
                    .setLabel(null)
                    .setTunerCount(tunerCount)//will update it in code
                    .setExtras(extras)
                    .build();
        } catch (Exception e) {
            Log.d(TAG, "buildTvInputInfo Exception = " + e.getMessage());
        }
        return result;
    }

    public TvInputInfo onHardwareAdded(TvInputHardwareInfo hardwareInfo) {
        Log.d(TAG, "onHardwareAdded ," + "DeviceId :" + hardwareInfo.getDeviceId());
        TvInputInfo result = null;
        switch (hardwareInfo.getDeviceId()) {
            case 19:
                if (mTvInputInfo != null) {
                    return result;
                } else {
                    mTvInputHardwareInfo = hardwareInfo;
                    mTvInputInfo = buildTvInputInfo(hardwareInfo, 2, null);
                    setInputId(mTvInputInfo.getId());
                    mHardware = mTvInputManager.acquireTvInputHardware(19, mTvInputInfo, mHardwareCallback);
                    result = mTvInputInfo;
                }
                break;
            case 19 + 100:
                if (mTvInputHardwareInfo == null || mTvInputInfo == null) {
                    Log.d(TAG, "onHardwareAdded pip hardware need to copy main hardware info, but main is not ready");
                    return result;
                }
                if (mPipTvInputInfo != null) {
                    return result;
                } else {
                    mPipTvInputHardwareInfo = hardwareInfo;
                    mPipTvInputInfo = buildTvInputInfo(hardwareInfo, 2, null);
                    mPipHardware = mTvInputManager.acquireTvInputHardware(19 + 100, mTvInputInfo, mPipHardwareCallback);
                }
                //force set it to mTvInputInfo as only one source is needed
                result = mTvInputInfo;
                break;
            default:
                break;
        }
        return result;
    }

    public String onHardwareRemoved(TvInputHardwareInfo hardwareInfo) {
        Log.d(TAG, "onHardwareRemoved ," + "DeviceId :" + hardwareInfo.getDeviceId());
        String id = null;
        switch (hardwareInfo.getDeviceId()) {
            case 19:
                if (mTvInputInfo != null) {
                    id = mTvInputInfo.getId();
                    mTvInputInfo = null;
                    mTvInputHardwareInfo = null;
                    mConfigs = null;
                }
                break;
            case 19 + 100:
                if (mPipTvInputInfo != null) {
                    id = mInputId;
                    mPipTvInputInfo = null;
                    mPipTvInputHardwareInfo = null;
                    mPipConfigs = null;
                }
                break;
            default:
                break;
        }
        return id;
    }


    private boolean getFeatureSupportTimeshifting() {
        return !PropSettingManager.getBoolean(PropSettingManager.TIMESHIFT_DISABLE, false);
    }

    private boolean getFeatureCasReady() {
        return PropSettingManager.getBoolean("vendor.tv.dtv.cas.ready", false);
    }

    private boolean getFeatureCompliance() {
        return PropSettingManager.getBoolean("vendor.tv.dtv.compiliance", true)
            || getFeatureCasReady();
    }

    private boolean getFeatureDmxLimit() {
        return !PropSettingManager.getBoolean("vendor.tv.dtv.dmx.nolimit", true);
    }

    private boolean getFeatureSupportRecordAVServiceOnly() {
        return PropSettingManager.getBoolean("vendor.tv.dtv.dvr.rec_av_only", true);
    }

    private boolean getFeatureSupportPip() {
        return getFeatureSupportFullPipFccArchitecture() && PropSettingManager.getBoolean(PropSettingManager.ENABLE_PIP_SUPPORT, false);
    }

    private boolean getFeatureSupportFcc() {
        return getFeatureSupportFullPipFccArchitecture() && PropSettingManager.getBoolean(PropSettingManager.ENABLE_FCC_SUPPORT, false);
    }

    private boolean getFeatureSupportFullPipFccArchitecture() {
        return PropSettingManager.getBoolean(PropSettingManager.ENABLE_FULL_PIP_FCC_ARCHITECTURE, false);
    }

    private boolean getFeatureSupportFillSurface() {
        return PropSettingManager.getBoolean(PropSettingManager.ENABLE_FILL_SURFACE, false);
    }

    private boolean getFeatureSupportNewTvApp() {
        return isSdkAfterAndroidQ() && PropSettingManager.getBoolean(PropSettingManager.MATCH_NEW_TVAPP_ENABLE, true);
    }

    private boolean getFeatureSupportCaptioningManager() {
        return !getFeatureSupportNewTvApp() && PropSettingManager.getBoolean(PropSettingManager.CAPTIONING_MANAGER_ENABLE, true);
    }

    private boolean getFeatureSupportManualTimeshift() {
        return getFeatureSupportNewTvApp() || PropSettingManager.getBoolean(PropSettingManager.MANUAL_TIMESHIFT_ENABLE, false);
    }

    private boolean isSdkAfterAndroidQ() {
        return VERSION.SDK_INT > VERSION_CODES.P + 1;
    }

    /*
    * ff000000 represent alpha = 0Xff, R = 0, G = 0, B = O
    */
    private int getFeatureSupportFillSurfaceColor() {
        int result = -1;
        String colorStr = PropSettingManager.getString(PropSettingManager.ENABLE_FILL_SURFACE_COLOR, "0");
        try {
            result = (int)Long.parseLong(colorStr, 16);
        } catch (Exception e) {
            Log.d(TAG, "getFeatureSupportFillSurfaceColor Exception = " + e.getMessage());
        }
        return result;
    }

    private int getNumRecordersLimit() {
        if (numRecorders > 0)
            return getFeatureDmxLimit() ? 1 : numRecorders;
        return numRecorders;
    }

    private boolean getFeatureTimeshiftingPriorityHigh() {
        return PropSettingManager.getBoolean("vendor.tv.dtv.tf.priority_high", false);
    }

    private boolean createDecoder() {
        String str = "OMX.amlogic.avc.decoder.awesome.secure";
        try {
            mMediaCodec1 = MediaCodec.createByCodecName(str);
            } catch (Exception exception) {
            Log.e(TAG, "Exception during decoder creation", exception);
            decoderRelease();
            return false;
        }
        try {
            mMediaCodec2 = MediaCodec.createByCodecName(str);
            } catch (Exception exception) {
            Log.e(TAG, "Exception during decoder creation", exception);
            decoderRelease();
            return false;
        }
        Log.e(TAG, "createDecoder done");
        return true;
    }

    private void decoderRelease() {
        if (mMediaCodec1 != null) {
            try {
                mMediaCodec1.stop();
                } catch (IllegalStateException exception) {
                mMediaCodec1.reset();
                // IllegalStateException happens when decoder fail to start.
                Log.e(TAG, "IllegalStateException during decoder1 stop", exception);
                } finally {
                    try {
                        mMediaCodec1.release();
                    } catch (IllegalStateException exception) {
                        Log.e(TAG, "IllegalStateException during decoder1 release", exception);
                    }
                    mMediaCodec1 = null;
            }
        }

        if (mMediaCodec2 != null) {
            try {
                mMediaCodec2.stop();
                } catch (IllegalStateException exception) {
                mMediaCodec2.reset();
                // IllegalStateException happens when decoder fail to start.
                Log.e(TAG, "IllegalStateException during decoder2 stop", exception);
                } finally {
                    try {
                        mMediaCodec2.release();
                    } catch (IllegalStateException exception) {
                        Log.e(TAG, "IllegalStateException during decoder2 release", exception);
                    }
                    mMediaCodec2 = null;
            }
        }

        Log.e(TAG, "decoderRelease done");
    }

/*
    private final DtvkitGlueClient.SystemControlHandler mSysControlHandler = new DtvkitGlueClient.SystemControlHandler() {
        @RequiresApi(api = Build.VERSION_CODES.M)
        @Override
        public String onReadSysFs(int ftype, String name) {
            String value = null;
            if (mSystemControlManager != null) {
                if (ftype == SYSFS) {
                    value = mSystemControlManager.readSysFs(name);
                } else if (ftype == PROP) {
                    value = mSystemControlManager.getProperty(name);
                } else {
                    //printf error log
                }
           }
           return value;
        }

        @RequiresApi(api = Build.VERSION_CODES.M)
        @Override
        public void onWriteSysFs(int ftype, String name, String cmd) {
            if (mSystemControlManager != null) {
                if (ftype == SYSFS) {
                    mSystemControlManager.writeSysFs(name, cmd);
                } else if (ftype == PROP) {
                    mSystemControlManager.setProperty(name, cmd);
                } else {
                       //printf error log
                }
           }
        }
    }; */

    private void showSearchConfirmDialog(final Context context, final Channel channel, final int index) {
        if (context == null || channel == null) {
            Log.d(TAG, "showSearchConfirmDialog null context or input");
            return;
        }

        final AlertDialog.Builder builder = new AlertDialog.Builder(context);
        final AlertDialog alert = builder.create();
        mStreamChangeUpdateDialog = alert;
        final View dialogView = View.inflate(context, R.layout.confirm_search, null);
        final TextView title = (TextView) dialogView.findViewById(R.id.dialog_title);
        final Button confirm = (Button) dialogView.findViewById(R.id.confirm);
        final Button cancel = (Button) dialogView.findViewById(R.id.cancel);
        final int[] tempStatus = new int[1];//0 for flag that exit is pressed by user, 1 for exit by search over
        String tempChannelSignalType;
        int tempUpdateFrequency;
        try {
            tempChannelSignalType = channel.getInternalProviderData().get("channel_signal_type").toString();
        } catch (Exception e) {
            Log.i(TAG, "onMessageCallback channelSignalType Exception " + e.getMessage());
            tempChannelSignalType = null;
        }
        try {
            tempUpdateFrequency = Integer.valueOf(channel.getInternalProviderData().get("frequency").toString());
        } catch (Exception e) {
            Log.i(TAG, "onMessageCallback frequency Exception " + e.getMessage());
            tempUpdateFrequency = -1;
        }
        final String channelSignalType = tempChannelSignalType;
        final int updateFrequency = tempUpdateFrequency;
        if (!TextUtils.equals(channelSignalType, Channel.FIXED_SIGNAL_TYPE_DVBC) && !TextUtils.equals(channelSignalType, Channel.FIXED_SIGNAL_TYPE_DVBT) && !TextUtils.equals(channelSignalType, Channel.FIXED_SIGNAL_TYPE_DVBT2)) {
            Log.d(TAG, "showSearchConfirmDialog not dvbc or dvbt and is " + channelSignalType);
            return;
        }
        final DtvkitBackGroundSearch.BackGroundSearchCallback callback = new DtvkitBackGroundSearch.BackGroundSearchCallback() {
            @Override
            public void onMessageCallback(JSONObject mess) {
                if (mess != null) {
                    Log.d(TAG, "onMessageCallback " + mess.toString());
                    String status = null;
                    try {
                        status = mess.getString(DtvkitBackGroundSearch.SINGLE_FREQUENCY_STATUS_ITEM);
                    } catch (Exception e) {
                        Log.i(TAG, "onMessageCallback SINGLE_FREQUENCY_STATUS_ITEM Exception " + e.getMessage());
                    }
                    switch (status) {
                        case DtvkitBackGroundSearch.SINGLE_FREQUENCY_STATUS_SAVE_FINISH: {
                            tempStatus[0] = 1;
                            if (alert != null) {
                                alert.dismiss();
                            }
                            DtvkitTvInputSession mainSession = getMainTunerSession();
                            DtvkitTvInputSession pipSession = getPipTunerSession();
                            DtvkitTvInputSession updateSession = null;
                            DtvkitTvInputSession restoreSession = null;
                            Channel restoreChannel = null;
                            Channel newUpdateChannel = null;
                            if (INDEX_FOR_MAIN == index) {
                                updateSession = mainSession;
                                restoreSession = pipSession;
                                restoreChannel = mPipDvbChannel;
                                mainSession.mPreviousBufferUri = null;
                                mainSession.mNextBufferUri = null;
                            } else if (INDEX_FOR_PIP == index) {
                                updateSession = pipSession;
                                restoreSession = mainSession;
                                restoreChannel = mMainDvbChannel;
                            }
                            if (updateSession != null) {
                                Uri channelUri = null;
                                String serviceName0 = null;
                                try {
                                    serviceName0 = mess.getString(DtvkitBackGroundSearch.SINGLE_FREQUENCY_CHANNEL_NAME + 0);
                                } catch (Exception e) {
                                    Log.i(TAG, "onMessageCallback SINGLE_FREQUENCY_CHANNEL_NAME Exception " + e.getMessage());
                                }
                                if (!TextUtils.isEmpty(serviceName0)) {
                                    newUpdateChannel = TvContractUtils.getChannelByDisplayName(context.getContentResolver(), serviceName0, updateFrequency);
                                }
                                if (newUpdateChannel != null) {
                                    channelUri = TvContract.buildChannelUri(newUpdateChannel.getId());
                                }
                                if (channelUri != null) {
                                    updateSession.onTune(channelUri);
                                    updateSession.notifyChannelRetuned(channelUri);
                                    Log.d(TAG, "onMessageCallback notifyChannelRetuned " + channelUri);
                                } else {
                                    mDvbNetworkChangeSearchStatus = false;
                                    mMainDvbChannel = null;
                                    mPipDvbChannel = null;
                                    Log.d(TAG, "onMessageCallback none channels");
                                    Toast.makeText(
                                            DtvkitTvInput.this,
                                            R.string.dvb_network_change_search_no_result, Toast.LENGTH_SHORT)
                                            .show();
                                }
                            }
                            if (restoreSession != null && restoreChannel != null) {
                                int restoreFrequency = -1;
                                String serviceName1 = null;
                                try {
                                    restoreFrequency = Integer.valueOf(restoreChannel.getInternalProviderData().get("frequency").toString());
                                } catch (Exception e) {
                                    Log.i(TAG, "onMessageCallback restoreFrequency frequency Exception " + e.getMessage());
                                }
                                try {
                                    serviceName1 = mess.getString(DtvkitBackGroundSearch.SINGLE_FREQUENCY_CHANNEL_NAME + 1);
                                } catch (Exception e) {
                                    Log.i(TAG, "onMessageCallback serviceName1 SINGLE_FREQUENCY_CHANNEL_NAME Exception " + e.getMessage());
                                }
                                if (restoreFrequency != updateFrequency) {
                                    Uri restoreUri = TvContract.buildChannelUri(restoreChannel.getId());
                                    restoreSession.onTune(restoreUri);
                                    restoreSession.notifyChannelRetuned(restoreUri);
                                    Log.d(TAG, "onMessageCallback restoreSession notifyChannelRetuned " + restoreUri);
                                } else {
                                    restoreChannel = null;
                                    if (newUpdateChannel != null) {
                                        /*List<Channel> allChannels = TvContractUtils.getChannels(context.getContentResolver());
                                        List<Channel> sameFrequencyChannels = new ArrayList<Channel>();
                                        for (Channel single : allChannels) {
                                            int singleFrequency = Integer.valueOf(single.getInternalProviderData().get("frequency").toString());
                                            if (singleFrequency == updateFrequency && newUpdateChannel.getId() != single.getId()) {
                                                sameFrequencyChannels.add(single);
                                            }
                                        }
                                        Collections.sort(sameFrequencyChannels, new TvContractUtils.CompareDisplayNumber());
                                        if (sameFrequencyChannels != null && sameFrequencyChannels.size() > 0) {
                                            restoreChannel = sameFrequencyChannels.get(0);
                                        }*/
                                        if (!TextUtils.isEmpty(serviceName1)) {
                                            restoreChannel = TvContractUtils.getChannelByDisplayName(context.getContentResolver(), serviceName1, updateFrequency);
                                        }
                                    }
                                    if (restoreChannel != null) {
                                        Uri restoreUri = TvContract.buildChannelUri(restoreChannel.getId());
                                        restoreSession.onTune(restoreUri);
                                        restoreSession.notifyChannelRetuned(restoreUri);
                                        Log.d(TAG, "onMessageCallback restoreSession new channel notifyChannelRetuned " + restoreUri);
                                    } else {
                                        Uri restoreUri = TvContract.buildChannelUri(-1);
                                        restoreSession.notifyChannelRetuned(restoreUri);
                                        Log.d(TAG, "onMessageCallback restoreSession doesn't have available channel");
                                        Toast.makeText(
                                                DtvkitTvInput.this,
                                                R.string.dvb_network_change_search_channel_unavailable, Toast.LENGTH_SHORT)
                                                .show();
                                    }
                                }
                            }
                            break;
                        }
                        default:
                            break;
                    }
                }
            }
        };

        final DtvkitBackGroundSearch setup = new DtvkitBackGroundSearch(context, channelSignalType, updateFrequency, channel.getInputId(), callback);
        setup.startSearch();
        title.setText(R.string.dvb_network_change);
        confirm.setVisibility(View.GONE);
        cancel.setVisibility(View.GONE);
        /*confirm.requestFocus();
        cancel.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                alert.dismiss();
            }
        });
        confirm.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                alert.dismiss();
                Intent setupIntent = input.createSetupIntent();
                setupIntent.putExtra(TvInputInfo.EXTRA_INPUT_ID, input.getId());
                setupIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(setupIntent);
            }
        });*/
        alert.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                Log.d(TAG, "showSearchConfirmDialog onDismiss");
                if (tempStatus[0] != 1 && setup != null) {
                    Log.d(TAG, "showSearchConfirmDialog need to stop search");
                    setup.stopSearch();
                    EpgSyncJobService.cancelAllSyncRequests(DtvkitTvInput.this);
                    EpgSyncJobService.requestImmediateSyncSearchedChannel(DtvkitTvInput.this, mInputId, true, new ComponentName(DtvkitTvInput.this, DtvkitEpgSync.class));
                    mDvbNetworkChangeSearchStatus = false;
                    mMainDvbChannel = null;
                    mPipDvbChannel = null;
                }
            }
        });
        //prevent exit key
        alert.setCancelable(false);
        alert.setOnKeyListener(new DialogInterface.OnKeyListener() {

            @Override
            public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                if (event.getAction() == KeyEvent.ACTION_UP
                        && keyCode == KeyEvent.KEYCODE_BACK
                        && event.getRepeatCount() == 0) {
                    if (tempStatus[0] != 1 && setup != null) {
                        Log.d(TAG, "showSearchConfirmDialog searching");
                        Toast.makeText(
                                DtvkitTvInput.this,
                                R.string.dvb_network_change,
                                Toast.LENGTH_SHORT)
                                .show();
                    }
                }
                return true;
            }
        });
        alert.getWindow().setType(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY);
        alert.setView(dialogView);
        alert.show();
        WindowManager.LayoutParams params = alert.getWindow().getAttributes();
        params.width = (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 500, context.getResources().getDisplayMetrics());
        params.height = WindowManager.LayoutParams.WRAP_CONTENT;
        alert.getWindow().setAttributes(params);
        alert.getWindow().setBackgroundDrawableResource(R.drawable.dialog_background);
    }

    private void hideStreamChangeUpdateDialog() {
        if (mStreamChangeUpdateDialog != null && mStreamChangeUpdateDialog.isShowing()) {
            Log.d(TAG, "hideStreamChangeUpdateDialog dismiss");
            mStreamChangeUpdateDialog.dismiss();
            mStreamChangeUpdateDialog = null;
        } else {
            Log.d(TAG, "hideStreamChangeUpdateDialog no need");
        }
    }

    private void writeSysFs(String path, String value) {
        if (null != mSysSettingManager)
            mSysSettingManager.writeSysFs(path, value);
    }

    /*
    private static final String CA_SETTING_SERVICE_PACKAGE_NAME ="org.dtvkit.inputsource";
    private static final String CA_SETTING_SERVICE_ACTION ="org.dtvkit.inputsource.services.CaSettingService";
    private void startCaSettingServices () {
        Intent intent = new Intent();
        intent.setPackage(CA_SETTING_SERVICE_PACKAGE_NAME);
        intent.setAction(CA_SETTING_SERVICE_ACTION);
        Log.d(TAG, "startCaSettingServices");
        startService(intent);
    }*/

    private void userDataStatus(boolean status) {
        int subFlg = getSubtitleFlag();
        if (subFlg >= SUBCTL_HK_DVBSUB) {
            if (status) {
                DtvkitGlueClient.getInstance().openUserData(); //for video afd
            } else {
                DtvkitGlueClient.getInstance().closeUserData(); //for video afd
            }
        }
    }

    private void checkAndUpdateLcn() {
        Log.d(TAG, "checkAndUpdateLcn");
        JSONArray array = mParameterMananer.getConflictLcn();
        if (mParameterMananer.needConfirmLcnInfomation(array)) {
            Log.d(TAG, "checkAndUpdateLcn delect all default lcn");
            //select default for all conflict items
            mParameterMananer.dealRestLcnConflictAsDefault(array, 0);
        }
    }

    @RequiresApi(Build.VERSION_CODES.N)
    public class AutomaticSearchingReceiver extends BroadcastReceiver {
        private static final String TAG = "AutomaticSearchingReceiver";
        //hold wake lock for 5s to ensure the coming recording schedules
        private static final String WAKE_LOCK_NAME = "AutomaticSearchingReceiver";
        private static final long WAKE_LOCK_TIMEOUT = 5000;
        private PowerManager.WakeLock mWakeLock = null;
        private PendingIntent mAlarmIntent = null;

        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "Automatic searching onReceive");
            if (intent == null) return;

            String action = intent.getAction();
            Log.d(TAG, "Automatic searching action =" + action);
            if (action.equals(intenAction)) {
                String strMode = intent.getStringExtra("mode");
                int mode = Integer.parseInt(strMode);
                //if need to light the screen please run the interface below
                if (mode == 2) {//operate mode
                    checkSystemWakeUp(context);
                }
                //avoid suspend when excute appointed pvr record
                if (mode == 1) { //standby mode
                    acquireWakeLock(context);
                }

                String strReptition = intent.getStringExtra("repetition");
                int repetition = Integer.parseInt(strReptition);
                setNextAlarm(context);
                final DtvkitBackGroundSearch.BackGroundSearchCallback bgcallback = new DtvkitBackGroundSearch.BackGroundSearchCallback() {
                    @Override
                    public void onMessageCallback(JSONObject mess) {
                        if (mess != null) {
                            Log.d(TAG, "onMessageCallback " + mess.toString());
                            String status = null;
                            try {
                                status = mess.getString(DtvkitBackGroundSearch.SINGLE_FREQUENCY_STATUS_ITEM);
                            } catch (Exception e) {
                                Log.i(TAG, "onMessageCallback SINGLE_FREQUENCY_STATUS_ITEM Exception " + e.getMessage());
                            }

                            switch (status) {
                                case DtvkitBackGroundSearch.SINGLE_FREQUENCY_STATUS_SAVE_FINISH: {
                                    Log.d(TAG, "waiting for doing something");
                                    if (mode == 1) { //standby mode
                                        releaseWakeLock();
                                    }
                                    break;
                                }

                            }

                        }
                    }

                };

                String signalType = "";
                boolean bIsDvbt = false;
                signalType = mParameterMananer.getStringParameters(mParameterMananer.AUTO_SEARCHING_SIGNALTYPE);
                Log.d(TAG, "signalType = " + signalType);
                switch(signalType) {
                    case "DVB-T":
                    case "DVB-T2":
                        bIsDvbt = true;
                    break;
                    case "DVB-C":
                        bIsDvbt = false;
                    break;
                    default:
                        bIsDvbt = true;
                    break;
                }
                DtvkitBackGroundSearch dtvkitBgSearch = new DtvkitBackGroundSearch(context, bIsDvbt, mInputId, bgcallback);
                dtvkitBgSearch.startBackGroundAutoSearch();
            }
        }

        private void checkSystemWakeUp(Context context) {
            PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            boolean isScreenOpen = powerManager.isScreenOn();
            Log.d(TAG, "checkSystemWakeUp isScreenOpen = " + isScreenOpen);
            //Resume if the system is suspending
            if (!isScreenOpen) {
                Log.d(TAG, "checkSystemWakeUp wakeUp the android.");
                long time = SystemClock.uptimeMillis();
                wakeUp(powerManager, time);
            }
        }

        private void wakeUp(PowerManager powerManager, long time) {
             try {
                 Class<?> cls = Class.forName("android.os.PowerManager");
                 Method method = cls.getMethod("wakeUp", long.class);
                 method.invoke(powerManager, time);
             } catch(Exception e) {
                 e.printStackTrace();
                 Log.d(TAG, "wakeUp Exception = " + e.getMessage());
             }
        }

        private void goToSleep(PowerManager powerManager, long time) {
             try {
                 Class<?> cls = Class.forName("android.os.PowerManager");
                 Method method = cls.getMethod("goToSleep", long.class);
                 method.invoke(powerManager, time);
             } catch(Exception e) {
                 e.printStackTrace();
                 Log.d(TAG, "goToSleep Exception = " + e.getMessage());
             }
        }

        private  synchronized void acquireWakeLock(Context context) {
            if (mWakeLock == null) {
                PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
                mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, WAKE_LOCK_NAME);
                if (mWakeLock != null) {
                    Log.d(TAG, "acquireWakeLock " + WAKE_LOCK_NAME + " " + mWakeLock);
                    if (mWakeLock.isHeld()) {
                        mWakeLock.release();
                    }
                    mWakeLock.acquire();
                }
            }
        }


        private synchronized void releaseWakeLock() {
            if (mWakeLock != null) {
                Log.d(TAG, "releaseWakeLock " + WAKE_LOCK_NAME + " " + mWakeLock);
                if (mWakeLock.isHeld()) {
                    mWakeLock.release();
                }
                mWakeLock = null;
            }
        }

        private void setNextAlarm(Context context) {
            String hour = mParameterMananer.getStringParameters(mParameterMananer.AUTO_SEARCHING_HOUR);
            String minute = mParameterMananer.getStringParameters(mParameterMananer.AUTO_SEARCHING_MINUTE);
            int mode  = mParameterMananer.getIntParameters(mParameterMananer.AUTO_SEARCHING_MODE);
            int repetition = mParameterMananer.getIntParameters(mParameterMananer.AUTO_SEARCHING_REPTITION);
            Intent intent = new Intent(intenAction);
            intent.putExtra("mode", mode+"");
            intent.putExtra("repetition", repetition+"");
            if (mAlarmIntent != null) {
                mAlarmManager.cancel(mAlarmIntent);
            }
            mAlarmIntent = PendingIntent.getBroadcast(context, 0, intent, 0);
            Calendar cal = Calendar.getInstance();
            long current = System.currentTimeMillis();
            cal.setTimeInMillis(current);
            cal.set(Calendar.HOUR_OF_DAY, Integer.parseInt(hour));
            cal.set(Calendar.MINUTE, Integer.parseInt(minute));
            if (repetition == 0) {
                long alarmtime = cal.getTimeInMillis() + AlarmManager.INTERVAL_DAY;
                Log.d(TAG, "daily =" + new Date(alarmtime).toString());
                mAlarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, alarmtime/*wakeAt*/, mAlarmIntent);
            } else if (repetition == 1) {
                long alarmtime = cal.getTimeInMillis() + AlarmManager.INTERVAL_DAY * 7;
                Log.d(TAG, "weekly =" + new Date(alarmtime).toString());
                mAlarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, alarmtime/*wakeAt*/, mAlarmIntent);
            }
        }
    }

    private void showToast(int stringRes) {
        Toast.makeText(DtvkitTvInput.this, stringRes, Toast.LENGTH_SHORT).show();
    }

    private void showToast(String str) {
        Toast.makeText(DtvkitTvInput.this, str, Toast.LENGTH_SHORT).show();
    }

    private void showOpSearchConfirmDialog(final Context context, int module) {
        if (context == null || module == -1) {
            Log.d(TAG, "showOpSearchConfirmDialog null context or module");
            return;
        }
        final AlertDialog.Builder builder = new AlertDialog.Builder(context);
        final AlertDialog alert = builder.create();
        final View dialogView = View.inflate(context, R.layout.confirm_search, null);
        final TextView title = (TextView) dialogView.findViewById(R.id.dialog_title);
        final Button confirm = (Button) dialogView.findViewById(R.id.confirm);
        final Button cancel = (Button) dialogView.findViewById(R.id.cancel);

        title.setText(R.string.ci_profile_update_available);
        confirm.setText(R.string.ci_profile_channel_update_confirm);
        cancel.setText(R.string.ci_profile_channel_update_cancel);
        confirm.requestFocus();
        cancel.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                alert.dismiss();
            }
        });
        confirm.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                confirm.setEnabled(false);
                cancel.setEnabled(false);
                title.setText(R.string.ci_profile_channel_updating);
                doOperatorSearch(module);
                alert.dismiss();
            }
        });
        alert.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                Log.d(TAG, "showOpSearchConfirmDialog onDismiss");
            }
        });
        alert.getWindow().setType(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY);
        alert.setView(dialogView);
        alert.show();
        WindowManager.LayoutParams params = alert.getWindow().getAttributes();
        params.width = (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 500, context.getResources().getDisplayMetrics());
        params.height = WindowManager.LayoutParams.WRAP_CONTENT;
        alert.getWindow().setAttributes(params);
        alert.getWindow().setBackgroundDrawableResource(R.drawable.dialog_background);
    }
}
