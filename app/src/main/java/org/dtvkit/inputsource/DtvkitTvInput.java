package org.dtvkit.inputsource;

import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentUris;
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

import android.content.IntentFilter;
import android.content.BroadcastReceiver;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.media.tv.TvInputHardwareInfo;
import android.media.tv.TvInputManager.Hardware;
import android.media.tv.TvInputManager.HardwareCallback;
import android.media.tv.TvInputHardwareInfo;
import android.media.tv.TvStreamConfig;
import android.text.TextUtils;
import java.io.IOException;
import org.xmlpull.v1.XmlPullParserException;
import android.content.Intent;

import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Looper;
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

import org.dtvkit.companionlibrary.EpgSyncJobService;
import org.dtvkit.companionlibrary.model.Channel;
import org.dtvkit.companionlibrary.model.InternalProviderData;
import org.dtvkit.companionlibrary.model.Program;
import org.dtvkit.companionlibrary.model.RecordedProgram;
import org.dtvkit.companionlibrary.utils.TvContractUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
/*
dtvkit
 */
import com.droidlogic.settings.PropSettingManager;
import com.droidlogic.settings.ConvertSettingManager;
import com.droidlogic.settings.SysSettingManager;
import com.droidlogic.settings.ConstantManager;

//import com.droidlogic.app.tv.TvControlManager;
import com.droidlogic.app.SystemControlManager;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.Objects;
import java.util.Iterator;
import java.util.Map;
import java.util.HashMap;

public class DtvkitTvInput extends TvInputService {
    private static final String TAG = "DtvkitTvInput";
    private LongSparseArray<Channel> mChannels;
    private ContentResolver mContentResolver;

    protected String mInputId = null;
   // private TvControlManager Tcm = null;
    private static final int MSG_DO_TRY_SCAN = 0;
    private static final int RETRY_TIMES = 10;
    private int retry_times = RETRY_TIMES;

    TvInputInfo mTvInputInfo = null;
    protected Hardware mHardware;
    protected TvStreamConfig[] mConfigs;
    private TvInputManager mTvInputManager;
    private Surface mSurface;
    private SysSettingManager mSysSettingManager = null;
    private DtvkitTvInputSession mSession;

    private enum PlayerState {
        STOPPED, PLAYING
    }
    private enum RecorderState {
        STOPPED, STARTING, RECORDING
    }
    private RecorderState timeshiftRecorderState = RecorderState.STOPPED;
    private boolean timeshifting = false;
    private int numRecorders = 0;
    private int numActiveRecordings = 0;
    private boolean scheduleTimeshiftRecording = false;
    private Handler scheduleTimeshiftRecordingHandler = null;
    private Runnable timeshiftRecordRunnable;
    private long mDtvkitTvInputSessionCount = 0;
    private DataMananer mDataMananer;

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
               /*if (isParentControlEnabled != getParentalControlOn()) {
                   setParentalControlOn(isParentControlEnabled);
               }
               if (isParentControlEnabled && mSession != null) {
                   mSession.syncParentControlSetting();
               }*/
            }
        }
    };

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "onCreate");
        mTvInputManager = (TvInputManager)this.getSystemService(Context.TV_INPUT_SERVICE);
        mContentResolver = getContentResolver();
        mContentResolver.registerContentObserver(TvContract.Channels.CONTENT_URI, true, mContentObserver);
        onChannelsChanged();

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(TvInputManager.ACTION_BLOCKED_RATINGS_CHANGED);
        intentFilter.addAction(TvInputManager.ACTION_PARENTAL_CONTROLS_ENABLED_CHANGED);
        registerReceiver(mParentalControlsBroadcastReceiver, intentFilter);

        TvInputInfo.Builder builder = new TvInputInfo.Builder(getApplicationContext(), new ComponentName(getApplicationContext(), DtvkitTvInput.class));
        numRecorders = recordingGetNumRecorders();
        if (numRecorders > 0) {
            builder.setCanRecord(true)
                    .setTunerCount(numRecorders);
            mContentResolver.registerContentObserver(TvContract.RecordedPrograms.CONTENT_URI, true, mRecordingsContentObserver);
            onRecordingsChanged();
        } else {
            builder.setCanRecord(false)
                    .setTunerCount(1);
        }
        getApplicationContext().getSystemService(TvInputManager.class).updateTvInputInfo(builder.build());
        mSysSettingManager = new SysSettingManager(this);
        mDataMananer = new DataMananer(this);
    }

    @Override
    public void onDestroy() {
        Log.i(TAG, "onDestroy");
        unregisterReceiver(mParentalControlsBroadcastReceiver);
        mContentResolver.unregisterContentObserver(mContentObserver);
        mContentResolver.unregisterContentObserver(mRecordingsContentObserver);
        DtvkitGlueClient.getInstance().disConnectDtvkitClient();
        super.onDestroy();
    }

    @Override
    public final Session onCreateSession(String inputId) {
        Log.i(TAG, "onCreateSession " + inputId);
        mSession = new DtvkitTvInputSession(this);
        SystemControlManager mSystemControlManager = SystemControlManager.getInstance();
        mSystemControlManager.SetDtvKitSourceEnable(1);
        return mSession;
    }

    protected void setInputId(String name) {
        mInputId = name;
        Log.d(TAG, "set input id to " + mInputId);
    }

    private class DtvkitOverlayView extends FrameLayout {

        private NativeOverlayView nativeOverlayView;
        private CiMenuView ciOverlayView;
        private int w;
        private int h;

        private boolean mhegTookKey = false;

        public DtvkitOverlayView(Context context) {
            super(context);

            Log.i(TAG, "onCreateDtvkitOverlayView");

            nativeOverlayView = new NativeOverlayView(getContext());
            ciOverlayView = new CiMenuView(getContext());

            this.addView(nativeOverlayView);
            this.addView(ciOverlayView);
        }

        public void destroy() {
            ciOverlayView.destroy();
            removeView(nativeOverlayView);
            removeView(ciOverlayView);
        }

        public void hideOverLay() {
            if (nativeOverlayView != null) {
                nativeOverlayView.setVisibility(View.GONE);
            }
            if (ciOverlayView != null) {
                ciOverlayView.setVisibility(View.GONE);
            }
            setVisibility(View.GONE);
        }

        public void showOverLay() {
            if (nativeOverlayView != null) {
                nativeOverlayView.setVisibility(View.VISIBLE);
            }
            if (ciOverlayView != null) {
                ciOverlayView.setVisibility(View.VISIBLE);
            }
            setVisibility(View.VISIBLE);
        }

        public void setSize(int width, int height) {
            w = width;
            h = height;
            nativeOverlayView.setSize(width, height);
        }

        public boolean handleKeyDown(int keyCode, KeyEvent event) {
            boolean result;
            if (ciOverlayView.handleKeyDown(keyCode, event)) {
                mhegTookKey = false;
                result = true;
            }
            else if (mhegKeypress(keyCode)){
                mhegTookKey = true;
                result = true;
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
            }
            else {
                result = false;
            }
            mhegTookKey = false;

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

    class NativeOverlayView extends View
    {
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

        Semaphore sem = new Semaphore(1);

        private final DtvkitGlueClient.OverlayTarget mTarget = new DtvkitGlueClient.OverlayTarget() {
            @Override
            public void draw(int src_width, int src_height, int dst_x, int dst_y, int dst_width, int dst_height, byte[] data) {
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
                            Canvas canvas = new Canvas(overlay_update);
                            Rect src = new Rect(0, 0, region_width, region_height);
                            Rect dst = new Rect(left, top, left + width, top + height);
                            Paint paint = new Paint();
                            paint.setAntiAlias(true);
                            paint.setFilterBitmap(true);
                            paint.setDither(true);
                            canvas.drawBitmap(region, src, dst, paint);
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
                    region = Bitmap.createBitmap(1920, 1080, Bitmap.Config.ARGB_8888);
                    left = dst_x;
                    top = dst_y;
                    width = dst_width;
                    height = dst_height;
                }
                else {
                    part_bottom = region_height;
                    region_height += src_height;
                }

                /* Build an array of ARGB_8888 pixels as signed ints and add this part to the region */
                int[] colors = new int[src_width * src_height];
                for (int i = 0, j = 0; i < src_width * src_height; i++, j += 4) {
                   colors[i] = (((int)data[j]&0xFF) << 24) | (((int)data[j+1]&0xFF) << 16) |
                      (((int)data[j+2]&0xFF) << 8) | ((int)data[j+3]&0xFF);
                }
                Bitmap part = Bitmap.createBitmap(colors, 0, src_width, src_width, src_height, Bitmap.Config.ARGB_8888);
                Canvas canvas = new Canvas(region);
                canvas.drawBitmap(part, 0, part_bottom, null);
            }
        };

        public NativeOverlayView(Context context) {
            super(context);
            DtvkitGlueClient.getInstance().setOverlayTarget(mTarget);
        }

        public void setSize(int width, int height) {
            dst = new Rect(0, 0, width, height);
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

    // We do not indicate recording capabilities. TODO for recording.
    @RequiresApi(api = Build.VERSION_CODES.N)
    public TvInputService.RecordingSession onCreateRecordingSession(String inputId)
    {
        Log.i(TAG, "onCreateRecordingSession");
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
        private String recordingUri = null;

        @RequiresApi(api = Build.VERSION_CODES.N)
        public DtvkitRecordingSession(Context context, String inputId) {
            super(context);
            mContext = context;
            mInputId = inputId;
            Log.i(TAG, "DtvkitRecordingSession");
        }

        @Override
        public void onTune(Uri uri) {
            Log.i(TAG, "onTune for recording " + uri);
            if (ContentUris.parseId(uri) == -1) {
                Log.e(TAG, "DtvkitRecordingSession onTune invalid uri = " + uri);
                notifyError(TvInputManager.RECORDING_ERROR_UNKNOWN);
                return;
            }

            removeScheduleTimeshiftRecordingTask();
            numActiveRecordings = recordingGetNumActiveRecordings();
            Log.i(TAG, "numActiveRecordings: " + numActiveRecordings);
            if (numActiveRecordings >= numRecorders) {
                if (getFeatureSupportTimeshifting()
                        && timeshiftRecorderState != RecorderState.STOPPED) {
                    Log.i(TAG, "No recording path available, no recorder");
                    notifyError(TvInputManager.RECORDING_ERROR_RESOURCE_BUSY);

                    Bundle event = new Bundle();
                    event.putString(ConstantManager.KEY_INFO, "No recording path available, no recorder");
                    notifySessionEvent(ConstantManager.EVENT_RESOURCE_BUSY, event);
                    return;
                } else {
                    boolean returnToLive = timeshifting;
                    Log.i(TAG, "stopping timeshift [return live:"+returnToLive+"]");
                    timeshiftRecorderState = RecorderState.STOPPED;
                    scheduleTimeshiftRecording = false;
                    timeshifting = false;
                    playerStopTimeshiftRecording(returnToLive);
                }
            }

            mChannel = uri;
            Channel channel = getChannel(uri);
            if (recordingCheckAvailability(getChannelInternalDvbUri(channel))) {
                Log.i(TAG, "recording path available");
                notifyTuned(uri);
            } else {
                Log.i(TAG, "No recording path available, no tuner/demux");
                notifyError(TvInputManager.RECORDING_ERROR_RESOURCE_BUSY);

                Bundle event = new Bundle();
                event.putString(ConstantManager.KEY_INFO, "No recording path available, no tuner/demux");
                notifySessionEvent(ConstantManager.EVENT_RESOURCE_BUSY, event);
            }
        }

        @Override
        public void onStartRecording(@Nullable Uri uri) {
            Log.i(TAG, "onStartRecording " + uri);
            mProgram = uri;

            String dvbUri;
            long durationSecs = 0;
            Program program = getProgram(uri);
            if (program != null) {
                startRecordTimeMillis = program.getStartTimeUtcMillis();
                long currentTimeMillis = System.currentTimeMillis();
                if (currentTimeMillis > startRecordTimeMillis) {
                    startRecordTimeMillis = currentTimeMillis;
                }
                dvbUri = getProgramInternalDvbUri(program);
            } else {
                startRecordTimeMillis = System.currentTimeMillis();
                dvbUri = getChannelInternalDvbUri(getChannel(mChannel));
                durationSecs = 3 * 60 * 60; // 3 hours is maximum recording duration for Android
            }
            StringBuffer recordingResponse = new StringBuffer();
            if (!recordingStartRecording(dvbUri, recordingResponse)) {
                if (recordingResponse.toString().equals("May not be enough space on disk")) {
                    Log.i(TAG, "record error insufficient space");
                    notifyError(TvInputManager.RECORDING_ERROR_INSUFFICIENT_SPACE);
                }
                else if (recordingResponse.toString().equals("Failed to get a tuner to record")) {
                    Log.i(TAG, "record error no tuner to record");
                    notifyError(TvInputManager.RECORDING_ERROR_RESOURCE_BUSY);
                }
                else {
                    Log.i(TAG, "record error unknown");
                    notifyError(TvInputManager.RECORDING_ERROR_UNKNOWN);
                }
            }
            else
            {
                recordingUri = recordingResponse.toString();
                Log.i(TAG, "Recording started:"+recordingUri);
            }
        }

        @Override
        public void onStopRecording() {
            Log.i(TAG, "onStopRecording");

            endRecordTimeMillis = System.currentTimeMillis();
            scheduleTimeshiftRecording = true;
            Log.d(TAG, "stop Recording:"+recordingUri);
            if (!recordingStopRecording(recordingUri)) {
                notifyError(TvInputManager.RECORDING_ERROR_UNKNOWN);
            } else {
                long recordingDurationMillis = endRecordTimeMillis - startRecordTimeMillis;
                RecordedProgram.Builder builder;
                Program program = getProgram(mProgram);
                if (program == null) {
                    program = getCurrentProgram(mChannel);
                    if (program == null) {
                        builder = new RecordedProgram.Builder()
                                .setStartTimeUtcMillis(startRecordTimeMillis)
                                .setEndTimeUtcMillis(endRecordTimeMillis);
                    } else {
                        builder = new RecordedProgram.Builder(program);
                    }
                } else {
                    builder = new RecordedProgram.Builder(program);
                }
                RecordedProgram recording = builder.setInputId(mInputId)
                        .setRecordingDataUri(recordingUri)
                        .setRecordingDurationMillis(recordingDurationMillis > 0 ? recordingDurationMillis : -1)
                        .build();
                notifyRecordingStopped(mContext.getContentResolver().insert(TvContract.RecordedPrograms.CONTENT_URI,
                        recording.toContentValues()));
            }
        }

        @Override
        public void onRelease() {
            Log.i(TAG, "onRelease");

            String uri = "";
            if (mProgram != null) {
                uri = getProgramInternalDvbUri(getProgram(mProgram));
            } else if (mChannel != null) {
                uri = getChannelInternalDvbUri(getChannel(mChannel)) + ";0000";
            } else {
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

        private Program getProgram(Uri uri) {
            Program program = null;
            if (uri != null) {
                Cursor cursor = mContext.getContentResolver().query(uri, Program.PROJECTION, null, null, null);
                if (cursor != null) {
                    while (cursor.moveToNext()) {
                        program = Program.fromCursor(cursor);
                    }
                }
            }
            return program;
        }

        private Program getCurrentProgram(Uri channelUri) {
            return TvContractUtils.getCurrentProgram(mContext.getContentResolver(), channelUri);
        }
    }

    class DtvkitTvInputSession extends TvInputService.Session               {
        private static final String TAG = "DtvkitTvInputSession";
        private static final int ADEC_START_DECODE = 1;
        private static final int ADEC_PAUSE_DECODE = 2;
        private static final int ADEC_RESUME_DECODE = 3;
        private static final int ADEC_STOP_DECODE = 4;
        private static final int ADEC_SET_DECODE_AD = 5;
        private static final int ADEC_SET_VOLUME = 6;
        private static final int ADEC_SET_MUTE = 7;
        private static final int ADEC_SET_OUTPUT_MODE = 8;
        private static final int ADEC_SET_PRE_GAIN = 9;
        private static final int ADEC_SET_PRE_MUTE = 10;
        private boolean mhegTookKey = false;
        private Channel mTunedChannel = null;
        private List<TvTrackInfo> mTunedTracks = null;
        protected final Context mContext;
        private RecordedProgram recordedProgram = null;
        private long originalStartPosition = TvInputManager.TIME_SHIFT_INVALID_TIME;
        private long startPosition = TvInputManager.TIME_SHIFT_INVALID_TIME;
        private long currentPosition = TvInputManager.TIME_SHIFT_INVALID_TIME;
        private float playSpeed = 0;
        private PlayerState playerState = PlayerState.STOPPED;
        private boolean timeshiftAvailable = false;
        private int timeshiftBufferSizeMins = 60;
        DtvkitOverlayView mView = null;
        private long mCurrentDtvkitTvInputSessionIndex = 0;
        protected HandlerThread mHandlerThread = null;
        protected Handler mHandlerThreadHandle = null;
        protected Handler mMainHandle = null;
        private boolean mIsMain = false;
        private final CaptioningManager mCaptioningManager;
        private boolean mKeyUnlocked = false;
        private boolean mBlocked = false;

        DtvkitTvInputSession(Context context) {
            super(context);
            mContext = context;
            Log.i(TAG, "DtvkitTvInputSession creat");
            setOverlayViewEnabled(true);
            mCaptioningManager =
                (CaptioningManager) context.getSystemService(Context.CAPTIONING_SERVICE);
            numActiveRecordings = recordingGetNumActiveRecordings();
            Log.i(TAG, "numActiveRecordings: " + numActiveRecordings);

            if (numActiveRecordings < numRecorders) {
                timeshiftAvailable = true;
            } else {
                timeshiftAvailable = false;
            }

            timeshiftRecordRunnable = new Runnable() {
                @RequiresApi(api = Build.VERSION_CODES.M)
                @Override
                public void run() {
                    Log.i(TAG, "timeshiftRecordRunnable running");
                    if (timeshiftAvailable) {
                        if (timeshiftRecorderState == RecorderState.STOPPED) {
                            if (playerStartTimeshiftRecording()) {
                                /*
                                  The onSignal callback may be triggerd before here,
                                  and changes the state to a further value.
                                  so check the state first, in order to prevent getting it reset.
                                */
                                if (timeshiftRecorderState != RecorderState.RECORDING) {
                                    timeshiftRecorderState = RecorderState.STARTING;
                                }
                            } else {
                                notifyTimeShiftStatusChanged(TvInputManager.TIME_SHIFT_STATUS_UNAVAILABLE);
                            }
                        }
                    }
                }
            };

            playerSetTimeshiftBufferSize(timeshiftBufferSizeMins);
            recordingAddDiskPath(mDataMananer.getStringParameters(DataMananer.KEY_PVR_RECORD_PATH)/*"/data/data/org.dtvkit.inputsource"*/);
            recordingSetDefaultDisk(mDataMananer.getStringParameters(DataMananer.KEY_PVR_RECORD_PATH)/*"/data/data/org.dtvkit.inputsource"*/);
            mDtvkitTvInputSessionCount++;
            mCurrentDtvkitTvInputSessionIndex = mDtvkitTvInputSessionCount;
            initWorkThread();
        }

        @Override
        public void onSetMain(boolean isMain) {
            Log.d(TAG, "onSetMain, isMain: " + isMain +" mCurrentDtvkitTvInputSessionIndex is " + mCurrentDtvkitTvInputSessionIndex);
            mIsMain = isMain;
            if (!mIsMain) {
                if (null != mSysSettingManager)
                    mSysSettingManager.writeSysFs("/sys/class/video/disable_video", "1");
                if (null != mView)
                    layoutSurface(0, 0, mView.w, mView.h);
            }
        }

        @Override
        public void onRelease() {
            Log.i(TAG, "onRelease");
            //must destory mview,!!! we
            //will regist handle to client when
            //creat ciMenuView,so we need destory and
            //unregist handle.
            if (mMainHandle != null) {
                mMainHandle.sendEmptyMessage(MSG_MAIN_HANDLE_DESTROY_OVERLAY);
            }
            //send MSG_RELEASE_WORK_THREAD after dealing destroy overlay
        }

        private void doDestroyOverlay() {
            Log.i(TAG, "doDestroyOverlay");
            if (mView != null) {
                mView.destroy();
            }
        }

        private void doRelease() {
            Log.i(TAG, "doRelease");
            removeScheduleTimeshiftRecordingTask();
            scheduleTimeshiftRecording = false;
            timeshiftRecorderState = RecorderState.STOPPED;
            timeshifting = false;
            mhegStop();
            playerStopTimeshiftRecording(false);
            playerStop();
            playerSetSubtitlesOn(false);
            playerSetTeletextOn(false, -1);
            DtvkitGlueClient.getInstance().unregisterSignalHandler(mHandler);
        }

        private void doSetSurface(Map<String, Object> surfaceInfo) {
            if (surfaceInfo == null) {
                Log.d(TAG, "doSetSurface null parameter");
                return;
            } else {
                Surface surface = (Surface)surfaceInfo.get(ConstantManager.KEY_SURFACE);
                TvStreamConfig config = (TvStreamConfig)surfaceInfo.get(ConstantManager.KEY_TV_STREAM_CONFIG);
                Log.d(TAG, "doSetSurface surface = " + surface + ", config = " + config);
                mHardware.setSurface(surface, config);
            }
        }

        private void sendSetSurfaceMessage(Surface surface, TvStreamConfig config) {
            Map<String, Object> surfaceInfo = new HashMap<String, Object>();
            surfaceInfo.put(ConstantManager.KEY_SURFACE, surface);
            surfaceInfo.put(ConstantManager.KEY_TV_STREAM_CONFIG, config);
            if (mHandlerThreadHandle != null) {
                boolean result = mHandlerThreadHandle.sendMessage(mHandlerThreadHandle.obtainMessage(MSG_SET_SURFACE, surfaceInfo));
                Log.d(TAG, "sendDoReleaseMessage status = " + result + ", surface = " + surface + ", config = " + config);
            } else {
                Log.d(TAG, "sendSetSurfaceMessage null mHandlerThreadHandle");
            }
        }

        private void sendDoReleaseMessage() {
            if (mHandlerThreadHandle != null) {
                boolean result = mHandlerThreadHandle.sendEmptyMessage(MSG_DO_RELEASE);
                Log.d(TAG, "sendDoReleaseMessage status = " + result);
            } else {
                Log.d(TAG, "sendDoReleaseMessage null mHandlerThreadHandle");
            }
        }

        @Override
        public boolean onSetSurface(Surface surface) {
            Log.i(TAG, "onSetSurface " + surface + ", mDtvkitTvInputSessionCount = " + mDtvkitTvInputSessionCount + ", mCurrentDtvkitTvInputSessionIndex = " + mCurrentDtvkitTvInputSessionIndex);
            if (null != mHardware && mConfigs.length > 0) {
                if (null == surface) {
                    if (mIsMain) {
                        setOverlayViewEnabled(false);
                        //mHardware.setSurface(null, null);
                        sendSetSurfaceMessage(null, null);
                        Log.d(TAG, "onSetSurface null");
                        mSurface = null;
                        //doRelease();
                        sendDoReleaseMessage();
                    }
                } else {
                    if (mSurface != surface) {
                        Log.d(TAG, "TvView swithed,  onSetSurface null first");
                        //doRelease();
                        sendDoReleaseMessage();
                        //mHardware.setSurface(null, null);
                        sendSetSurfaceMessage(null, null);
                    }
                    //mHardware.setSurface(surface, mConfigs[0]);
                    sendSetSurfaceMessage(surface, mConfigs[0]);
                    surface = mSurface;
                    Log.d(TAG, "onSetSurface ok");
                }
            }
            return true;
        }

        @Override
        public void onSurfaceChanged(int format, int width, int height) {
            Log.i(TAG, "onSurfaceChanged " + format + ", " + width + ", " + height);
            //playerSetRectangle(0, 0, width, height);
        }

        public View onCreateOverlayView() {
            if (mView == null) {
                mView = new DtvkitOverlayView(mContext);
            }
            return mView;
        }

        @Override
        public void onOverlayViewSizeChanged(int width, int height) {
            Log.i(TAG, "onOverlayViewSizeChanged " + width + ", " + height);
            if (mView == null) {
                mView = new DtvkitOverlayView(mContext);
            }
            Platform platform = new Platform();
            playerSetRectangle(platform.getSurfaceX(), platform.getSurfaceY(), width, height);
            mView.setSize(width, height);
        }

        @RequiresApi(api = Build.VERSION_CODES.M)
        @Override
        public boolean onTune(Uri channelUri) {
            Log.i(TAG, "onTune " + channelUri);
            if (ContentUris.parseId(channelUri) == -1) {
                Log.e(TAG, "DtvkitTvInputSession onTune invalid channelUri = " + channelUri);
                return false;
            }

            if (mHandlerThreadHandle != null)
                mHandlerThreadHandle.obtainMessage(MSG_ON_TUNE, 0, 0, channelUri).sendToTarget();

            mTunedChannel = getChannel(channelUri);

            Log.i(TAG, "onTune will be Done in onTuneByHandlerThreadHandle");
            return mTunedChannel != null;
        }

        // For private app params. Default calls onTune
        //public boolean onTune(Uri channelUri, Bundle params)

        @Override
        public void onSetStreamVolume(float volume) {
            Log.i(TAG, "onSetStreamVolume " + volume + ", mute " + (volume == 0.0f));
            //playerSetVolume((int) (volume * 100));
            if (mHandlerThreadHandle != null) {
                mHandlerThreadHandle.removeMessages(MSG_BLOCK_MUTE_OR_UNMUTE);
                Message mess = mHandlerThreadHandle.obtainMessage(MSG_BLOCK_MUTE_OR_UNMUTE, (volume == 0.0f ? 1 : 0), 0);
                mHandlerThreadHandle.sendMessageDelayed(mess, MSG_BLOCK_MUTE_OR_UNMUTE_PERIOD);
            }
        }

        @Override
        public void onSetCaptionEnabled(boolean enabled) {
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
            Log.i(TAG, "onSelectTrack " + type + ", " + trackId);
            if (type == TvTrackInfo.TYPE_AUDIO) {
                if (playerSelectAudioTrack((null == trackId) ? 0xFFFF : Integer.parseInt(trackId))) {
                    notifyTrackSelected(type, trackId);
                    return true;
                }
            } else if (type == TvTrackInfo.TYPE_SUBTITLE) {
                String sourceTrackId = trackId;
                int subType = 4;//default sub
                int isTele = 0;//default subtitle
                if (!TextUtils.isEmpty(trackId) && !TextUtils.isDigitsOnly(trackId)) {
                    String[] nameValuePairs = trackId.split("&");
                    if (nameValuePairs != null && nameValuePairs.length == 3) {
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
                        return false;
                    }
                }
                if (mCaptioningManager.isEnabled() && selectSubtitleOrTeletext(isTele, subType, trackId)) {
                    notifyTrackSelected(type, sourceTrackId);
                } else {
                    Log.d(TAG, "onSelectTrack mCaptioningManager closed or invlid sub");
                    notifyTrackSelected(type, null);
                }
            }
            return false;
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
                        boolean startTele = playerSelectTeletextTrack(Integer.parseInt(indexId));
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
                notifyTrackSelected(TvTrackInfo.TYPE_SUBTITLE, teleTrackId);
            } else {
                notifyTrackSelected(TvTrackInfo.TYPE_SUBTITLE, null);
            }
            return true;
        }

        @Override
        public void onUnblockContent(TvContentRating unblockedRating) {
            super.onUnblockContent(unblockedRating);
            Log.i(TAG, "onUnblockContent " + unblockedRating);
            setParentalControlOn(false);
            notifyContentAllowed();
            setBlockMute(false);
            mKeyUnlocked = true;
            mBlocked = false;
        }

        @Override
        public void onAppPrivateCommand(String action, Bundle data) {
            Log.i(TAG, "onAppPrivateCommand " + action + ", " + data);
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
            }
        }

        @RequiresApi(api = Build.VERSION_CODES.M)
        public void onTimeShiftPlay(Uri uri) {
            Log.i(TAG, "onTimeShiftPlay " + uri);

            recordedProgram = getRecordedProgram(uri);
            if (recordedProgram != null) {
                playerState = PlayerState.PLAYING;
                playerStop();
                if (playerPlay(recordedProgram.getRecordingDataUri()).equals("ok"))
                {
                    DtvkitGlueClient.getInstance().registerSignalHandler(mHandler);
                }
                else
                {
                    notifyVideoUnavailable(TvInputManager.VIDEO_UNAVAILABLE_REASON_UNKNOWN);
                }
            }
        }

        public void onTimeShiftPause() {
            Log.i(TAG, "onTimeShiftPause timeshiftRecorderState:"+timeshiftRecorderState+" timeshifting:"+timeshifting);
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

        public void onTimeShiftResume() {
            Log.i(TAG, "onTimeShiftResume ");
            playerState = PlayerState.PLAYING;
            if (playerResume())
            {
                playSpeed = 1;
            }
        }

        public void onTimeShiftSeekTo(long timeMs) {
            Log.i(TAG, "onTimeShiftSeekTo:  " + timeMs);
            if (timeshiftRecorderState == RecorderState.RECORDING && !timeshifting) /* Watching live tv while recording */ {
                timeshifting = true;
                boolean seekToBeginning = false;

                if (timeMs == startPosition) {
                    seekToBeginning = true;
                }
                playerPlayTimeshiftRecording(false, !seekToBeginning);
            } else if (timeshiftRecorderState == RecorderState.RECORDING && timeshifting) {
                playerSeekTo((timeMs - (originalStartPosition + PropSettingManager.getStreamTimeDiff())) / 1000);
            } else {
                playerSeekTo(timeMs / 1000);
            }
        }

        @RequiresApi(api = Build.VERSION_CODES.M)
        public void onTimeShiftSetPlaybackParams(PlaybackParams params) {
            Log.i(TAG, "onTimeShiftSetPlaybackParams");

            float speed = params.getSpeed();
            Log.i(TAG, "speed: " + speed);
            if (speed != playSpeed) {
                if (timeshiftRecorderState == RecorderState.RECORDING && !timeshifting) {
                    timeshifting = true;
                    playerPlayTimeshiftRecording(false, true);
                }

                if (playerSetSpeed(speed))
                {
                    playSpeed = speed;
                }
            }
        }

        public long onTimeShiftGetStartPosition() {
            if (timeshiftRecorderState != RecorderState.STOPPED) {
                Log.i(TAG, "requesting timeshift recorder status");
                long length = 0;
                JSONObject timeshiftRecorderStatus = playerGetTimeshiftRecorderStatus();
                if (originalStartPosition != 0 && originalStartPosition != TvInputManager.TIME_SHIFT_INVALID_TIME) {
                    startPosition = originalStartPosition + PropSettingManager.getStreamTimeDiff();
                }
                if (timeshiftRecorderStatus != null) {
                    try {
                        length = timeshiftRecorderStatus.getLong("length");
                    } catch (JSONException e) {
                        Log.e(TAG, e.getMessage());
                    }
                }

                if (length > (timeshiftBufferSizeMins * 60)) {
                    startPosition = originalStartPosition + ((length - (timeshiftBufferSizeMins * 60)) * 1000);
                    Log.i(TAG, "new start position: " + startPosition);
                }
            }
            Log.i(TAG, "onTimeShiftGetStartPosition startPosition:" + startPosition + ", as date = " + ConvertSettingManager.convertLongToDate(startPosition));
            return startPosition;
        }

        public long onTimeShiftGetCurrentPosition() {
            if (startPosition == 0) /* Playing back recorded program */ {
                if (playerState == PlayerState.PLAYING) {
                    currentPosition = playerGetElapsed() * 1000;
                    Log.i(TAG, "playing back record program. current position: " + currentPosition);
                }
            } else if (timeshifting) {
                currentPosition = (playerGetElapsed() * 1000) + originalStartPosition + PropSettingManager.getStreamTimeDiff();
                Log.i(TAG, "timeshifting. current position: " + currentPosition);
            } else if (startPosition == TvInputManager.TIME_SHIFT_INVALID_TIME) {
                currentPosition = TvInputManager.TIME_SHIFT_INVALID_TIME;
                Log.i(TAG, "Invalid time. Current position: " + currentPosition);
            } else {
                currentPosition = /*System.currentTimeMillis()*/PropSettingManager.getCurrentStreamTime(true);
                Log.i(TAG, "live tv. current position: " + currentPosition);
            }
            Log.d(TAG, "onTimeShiftGetCurrentPosition currentPosition = " + currentPosition + ", as date = " + ConvertSettingManager.convertLongToDate(currentPosition));
            return currentPosition;
        }

        private RecordedProgram getRecordedProgram(Uri uri) {
            RecordedProgram recordedProgram = null;
            if (uri != null) {
                Cursor cursor = mContext.getContentResolver().query(uri, RecordedProgram.PROJECTION, null, null, null);
                if (cursor != null) {
                    while (cursor.moveToNext()) {
                        recordedProgram = RecordedProgram.fromCursor(cursor);
                    }
                }
            }
            return recordedProgram;
        }

        @Override
        public boolean onKeyDown(int keyCode, KeyEvent event) {
            boolean used;

            Log.i(TAG, "onKeyDown " + keyCode);

            /* It's possible for a keypress to be registered before the overlay is created */
            if (mView == null) {
                used = super.onKeyDown(keyCode, event);
            }
            else {
                if (mView.handleKeyDown(keyCode, event)) {
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

            Log.i(TAG, "onKeyUp " + keyCode);

            /* It's possible for a keypress to be registered before the overlay is created */
            if (mView == null) {
                used = super.onKeyUp(keyCode, event);
            }
            else {
                if (mView.handleKeyUp(keyCode, event)) {
                    used = true;
                } else {
                    used = super.onKeyUp(keyCode, event);
                }
            }

            return used;
        }

        private final DtvkitGlueClient.SignalHandler mHandler = new DtvkitGlueClient.SignalHandler() {
            @RequiresApi(api = Build.VERSION_CODES.M)
            @Override
            public void onSignal(String signal, JSONObject data) {
                Log.i(TAG, "onSignal: " + signal + " : " + data.toString());
                // TODO notifyTracksChanged(List<TvTrackInfo> tracks)
                if (signal.equals("PlayerStatusChanged")) {
                    String state = "off";
                    String dvbUri = "";
                    try {
                        state = data.getString("state");
                        dvbUri= data.getString("uri");
                    } catch (JSONException ignore) {
                    }
                    Log.i(TAG, "signal: "+state);
                    switch (state) {
                        case "playing":
                            String type = "dvblive";
                            try {
                                type = data.getString("type");
                            } catch (JSONException e) {
                                Log.e(TAG, e.getMessage());
                            }
                            if (type.equals("dvblive")) {
                                if (mTunedChannel.getServiceType().equals(TvContract.Channels.SERVICE_TYPE_AUDIO)) {
                                    notifyVideoUnavailable(TvInputManager.VIDEO_UNAVAILABLE_REASON_AUDIO_ONLY);
                                } else {
                                    notifyVideoAvailable();
                                }
                                List<TvTrackInfo> tracks = playerGetTracks();
                                if (!tracks.equals(mTunedTracks)) {
                                    mTunedTracks = tracks;
                                    // TODO Also for service changed event
                                    Log.d(TAG, "update new mTunedTracks");
                                }
                                notifyTracksChanged(mTunedTracks);

                                if (mTunedChannel.getServiceType().equals(TvContract.Channels.SERVICE_TYPE_AUDIO_VIDEO)) {
                                    if (mHandlerThreadHandle != null) {
                                        mHandlerThreadHandle.sendEmptyMessageDelayed(MSG_CHECK_RESOLUTION, MSG_CHECK_RESOLUTION_PERIOD);//check resolution later
                                    }
                                }
                                Log.i(TAG, "audio track selected: " + playerGetSelectedAudioTrack());
                                notifyTrackSelected(TvTrackInfo.TYPE_AUDIO, Integer.toString(playerGetSelectedAudioTrack()));
                                initSubtitleOrTeletextIfNeed();

                                if (getFeatureSupportTimeshifting()) {
                                    if (timeshiftRecorderState == RecorderState.STOPPED) {
                                        numActiveRecordings = recordingGetNumActiveRecordings();
                                        Log.i(TAG, "numActiveRecordings: " + numActiveRecordings);
                                        if (numActiveRecordings < numRecorders) {
                                            timeshiftAvailable = true;
                                        } else {
                                            timeshiftAvailable = false;
                                        }
                                    }
                                    Log.i(TAG, "timeshiftAvailable: " + timeshiftAvailable);
                                    Log.i(TAG, "timeshiftRecorderState: " + timeshiftRecorderState);
                                    if (timeshiftAvailable) {
                                        if (timeshiftRecorderState == RecorderState.STOPPED) {
                                            if (playerStartTimeshiftRecording()) {
                                                timeshiftRecorderState = RecorderState.STARTING;
                                            } else {
                                                notifyTimeShiftStatusChanged(TvInputManager.TIME_SHIFT_STATUS_UNAVAILABLE);
                                            }
                                        }
                                    }
                                }
                            }
                            else if (type.equals("dvbrecording")) {
                                startPosition = originalStartPosition = 0; // start position is always 0 when playing back recorded program
                                currentPosition = playerGetElapsed(data) * 1000;
                                Log.i(TAG, "dvbrecording currentPosition = " + currentPosition + "as date = " + ConvertSettingManager.convertLongToDate(startPosition));
                                notifyTimeShiftStatusChanged(TvInputManager.TIME_SHIFT_STATUS_AVAILABLE);
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
                            boolean isTeleOn = playerIsTeletextOn();
                            if (isTeleOn) {
                                playerSetTeletextOn(false, -1);
                                notifyTrackSelected(TvTrackInfo.TYPE_SUBTITLE, null);
                                Log.d(TAG, "close teletext when badsignal received");
                            }
                            break;
                        case "off":
                            if (timeshiftRecorderState != RecorderState.STOPPED) {
                                removeScheduleTimeshiftRecordingTask();
                                scheduleTimeshiftRecording = false;
                                playerStopTimeshiftRecording(false);
                                timeshiftRecorderState = RecorderState.STOPPED;
                                notifyTimeShiftStatusChanged(TvInputManager.TIME_SHIFT_STATUS_UNAVAILABLE);
                            }
                            playerState = PlayerState.STOPPED;
                            if (recordedProgram != null) {
                                currentPosition = recordedProgram.getEndTimeUtcMillis();
                            }

                            break;
                        case "starting":
                           Log.i(TAG, "mhegStart " + dvbUri);
                           if (mhegStartService(dvbUri) != -1)
                           {
                              Log.i(TAG, "mhegStarted");
                           }
                           else
                           {
                              Log.i(TAG, "mheg failed to start");
                           }
                           break;
                        default:
                            Log.i(TAG, "Unhandled state: " + state);
                            break;
                    }
                } else if (signal.equals("AudioParamCB")) {
                    int cmd = 0, param1 = 0, param2 = 0;
                    AudioManager audioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
                    try {
                        cmd = data.getInt("audio_status");
                        param1 = data.getInt("audio_param1");
                        param2 = data.getInt("audio_param2");
                    } catch (JSONException ignore) {
                        Log.e(TAG, ignore.getMessage());
                    }
                    Log.d(TAG, "cmd ="+cmd+" param1 ="+param1+" param2 ="+param2);
                    switch (cmd) {
                        case ADEC_START_DECODE:
                            audioManager.setParameters("fmt="+param1);
                            audioManager.setParameters("has_dtv_video="+param2);
                            audioManager.setParameters("cmd="+cmd);
                            break;
                        case ADEC_PAUSE_DECODE:
                            audioManager.setParameters("cmd="+cmd);
                            break;
                        case ADEC_RESUME_DECODE:
                            audioManager.setParameters("cmd="+cmd);
                            break;
                        case ADEC_STOP_DECODE:
                            audioManager.setParameters("cmd="+cmd);
                            break;
                        case ADEC_SET_DECODE_AD:
                            audioManager.setParameters("cmd="+cmd);
                            audioManager.setParameters("fmt="+param1);
                            audioManager.setParameters("pid="+param2);
                            break;
                        case ADEC_SET_VOLUME:
                            audioManager.setParameters("cmd="+cmd);
                            audioManager.setParameters("vol="+param1);
                            break;
                        case ADEC_SET_MUTE:
                            audioManager.setParameters("cmd="+cmd);
                            audioManager.setParameters("mute="+param1);
                            break;
                        case ADEC_SET_OUTPUT_MODE:
                            audioManager.setParameters("cmd="+cmd);
                            audioManager.setParameters("mode="+param1);
                            break;
                        case ADEC_SET_PRE_GAIN:
                            audioManager.setParameters("cmd="+cmd);
                            audioManager.setParameters("gain="+param1);
                            break;
                        case ADEC_SET_PRE_MUTE:
                            audioManager.setParameters("cmd="+cmd);
                            audioManager.setParameters("mute="+param1);
                            break;
                        default:
                            Log.i(TAG,"unkown audio cmd!");
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
                            notifyTimeShiftStatusChanged(TvInputManager.TIME_SHIFT_STATUS_UNAVAILABLE);
                            break;
                    }
                } else if (signal.equals("RecordingStatusChanged")) {
                    JSONArray activeRecordings = recordingGetActiveRecordings(data);
                    if (activeRecordings != null && activeRecordings.length() < numRecorders &&
                            timeshiftRecorderState == RecorderState.STOPPED && scheduleTimeshiftRecording) {
                        timeshiftAvailable = true;
                        scheduleTimeshiftRecordingTask();
                    }
                }
                else if (signal.equals("DvbUpdatedEventPeriods"))
                {
                    Log.i(TAG, "DvbUpdatedEventPeriods");
                    ComponentName sync = new ComponentName(mContext, DtvkitEpgSync.class);
                    EpgSyncJobService.requestImmediateSync(mContext, mInputId, false, sync);
                }
                else if (signal.equals("DvbUpdatedEventNow"))
                {
                    Log.i(TAG, "DvbUpdatedEventNow");
                    ComponentName sync = new ComponentName(mContext, DtvkitEpgSync.class);
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
                    EpgSyncJobService.requestImmediateSync(mContext, mInputId, false, true, sync);
                }/*
                else if (signal.equals("DvbUpdatedChannelData"))
                {
                    Log.i(TAG, "DvbUpdatedChannelData");
                    List<TvTrackInfo> tracks = playerGetTracks();
                    if (!tracks.equals(mTunedTracks)) {
                        mTunedTracks = tracks;
                        notifyTracksChanged(mTunedTracks);
                    }
                    Log.i(TAG, "audio track selected: " + playerGetSelectedAudioTrack());
                    notifyTrackSelected(TvTrackInfo.TYPE_AUDIO, Integer.toString(playerGetSelectedAudioTrack()));
                    initSubtitleOrTeletextIfNeed();
                }*/
                else if (signal.equals("MhegAppStarted"))
                {
                   Log.i(TAG, "MhegAppStarted");
                   notifyVideoAvailable();
                }
                else if (signal.equals("AppVideoPosition"))
                {
                   Log.i(TAG, "AppVideoPosition");
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
                   if (mIsMain) {
                       String crop = new StringBuilder()
                           .append(voff0).append(" ")
                           .append(hoff0).append(" ")
                           .append(voff1).append(" ")
                           .append(hoff1).toString();
                       SystemControlManager mSystemControlManager = SystemControlManager.getInstance();
                       mSystemControlManager.writeSysFs("/sys/class/video/crop", crop);
                       layoutSurface(left,top,right,bottom);
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

                      if (mHandlerThreadHandle != null)
                          mHandlerThreadHandle.obtainMessage(MSG_ON_TUNE, 1/*mhegTune*/, 0, retuneUri).sendToTarget();
                   }
                   else
                   {
                      //if we couldn't find the channel uri for some reason,
                      // try restarting MHEG on the new service anyway
                      mhegSuspend();
                      mhegStartService(dvbUri);
                   }
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

        protected static final int MSG_CHECK_RESOLUTION_PERIOD = 1000;//MS
        protected static final int MSG_CHECK_PARENTAL_CONTROL_PERIOD = 2000;//MS
        protected static final int MSG_BLOCK_MUTE_OR_UNMUTE_PERIOD = 100;//MS

        protected static final int MSG_MAIN_HANDLE_DESTROY_OVERLAY = 1;

        protected void initWorkThread() {
            Log.d(TAG, "initWorkThread");
            if (mHandlerThread == null) {
                mHandlerThread = new HandlerThread("DtvkitInputWorker");
                mHandlerThread.start();
                mHandlerThreadHandle = new Handler(mHandlerThread.getLooper(), new Handler.Callback() {
                    @Override
                    public boolean handleMessage(Message msg) {
                        Log.d(TAG, "mHandlerThreadHandle handleMessage:"+msg.what);
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
                                doRelease();
                                break;
                            case MSG_RELEASE_WORK_THREAD:
                                releaseWorkThread();
                                break;
                            default:
                                Log.d(TAG, "initWorkThread default");
                                break;
                        }
                        return true;
                    }
                });
                mMainHandle = new MainHandler();
            }
        }

        private void syncParentControlSetting() {
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
        }

        private int getContentRatingsOfCurrentProgram() {
            int age = 0;
            String pc_rating;
            String rating_system;
            long currentStreamTime = 0;
            TvContentRating[] ratings;

            currentStreamTime = PropSettingManager.getCurrentStreamTime(true);
            if (currentStreamTime == 0)
                return 0;
            Log.d(TAG, "currentStreamTime:("+currentStreamTime+")");
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

        private void updateParentalControlExt() {
            int age = 0;
            int rating;
            boolean isParentControlEnabled = mTvInputManager.isParentalControlsEnabled();
            if (isParentControlEnabled && !mKeyUnlocked) {
                try {
                    JSONArray args = new JSONArray();
                    rating = getCurrentMinAgeByBlockedRatings();
                    age = getContentRatingsOfCurrentProgram();
                    Log.e(TAG, "updateParentalControlExt current program age["+ age +"] setting_rating[" +rating+ "]");
                    if ((rating < 4 || rating > 18 || age < rating) && mBlocked)
                    {
                        notifyContentAllowed();
                        setBlockMute(false);
                        mBlocked = false;
                    }
                    else if (rating >= 4 && rating <= 18 && age >= rating)
                    {
                        String Rating = "";
                        Rating = String.format("DVB_%d", age);
                        notifyContentBlocked(TvContentRating.createRating("com.android.tv", "DVB", Rating));
                        if (!mBlocked)
                        {
                            setBlockMute(true);
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
                setBlockMute(false);
                mBlocked = false;
            }
        }

        private void updateParentalControl() {
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
                    age = getContentRatingsOfCurrentProgram();
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
        }

        private void setBlockMute(boolean mute) {
            AudioManager audioManager = null;
            if (mContext != null) {
                audioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
            }
            if (audioManager != null) {
                Log.d(TAG, "setBlockMute = " + mute);
                if (mute) {
                    audioManager.setParameters("parental_control_av_mute=true");
                } else {
                    audioManager.setParameters("parental_control_av_mute=false");
                }
            } else {
                Log.i(TAG, "setBlockMute can't get audioManager");
            }
        }

        private class MainHandler extends Handler {
            public void handleMessage(Message msg) {
                Log.d(TAG, "MainHandler handleMessage:"+msg.what);
                switch (msg.what) {
                    case MSG_MAIN_HANDLE_DESTROY_OVERLAY:
                        doDestroyOverlay();
                        if (mHandlerThreadHandle != null) {
                            mHandlerThreadHandle.sendEmptyMessage(MSG_RELEASE_WORK_THREAD);
                        }
                        break;
                    default:
                        Log.d(TAG, "MainHandler default");
                        break;
                }
            }
        }

        protected void releaseWorkThread() {
            Log.d(TAG, "releaseWorkThread");
            if (mHandlerThreadHandle != null) {
                mHandlerThreadHandle.removeCallbacksAndMessages(null);
            }
            if (mHandlerThread != null) {
                mHandlerThread.quit();
            }
            if (mMainHandle != null) {
                mMainHandle.removeCallbacksAndMessages(null);
            }
            mHandlerThread = null;
            mHandlerThreadHandle = null;
            mMainHandle = null;
        }

        protected boolean onTuneByHandlerThreadHandle(Uri channelUri, boolean mhegTune) {
            Log.i(TAG, "onTuneByHandlerThreadHandle " + channelUri);
            if (ContentUris.parseId(channelUri) == -1) {
                Log.e(TAG, "onTuneByHandlerThreadHandle invalid channelUri = " + channelUri);
                return false;
            }
            removeScheduleTimeshiftRecordingTask();
            if (timeshiftRecorderState != RecorderState.STOPPED) {
                Log.i(TAG, "reset timeshiftState to STOPPED.");
                timeshiftRecorderState = RecorderState.STOPPED;
                timeshifting = false;
                scheduleTimeshiftRecording = false;
                playerStopTimeshiftRecording(false);
            }

            mTunedChannel = getChannel(channelUri);
            final String dvbUri = getChannelInternalDvbUri(mTunedChannel);

            if (mhegTune) {
                mhegSuspend();
                if (mhegGetNextTuneInfo(dvbUri) == 0)
                    notifyChannelRetuned(channelUri);
            } else {
                mhegStop();
            }

            notifyVideoUnavailable(TvInputManager.VIDEO_UNAVAILABLE_REASON_TUNING);
            playerStopTeletext();//no need to save teletext select status
            playerStop();
            playerSetSubtitlesOn(false);
            playerSetTeletextOn(false, -1);
            setParentalControlOn(false);
            mKeyUnlocked = false;
            if (mBlocked)
            {
                notifyContentAllowed();
                setBlockMute(false);
                mBlocked = false;
            }
            if (mTvInputManager != null) {
                boolean parentControlSwitch = mTvInputManager.isParentalControlsEnabled();
                if (parentControlSwitch)
                {
                    updateParentalControlExt();
                }
                /*boolean parentControlStatus = getParentalControlOn();
                if (parentControlSwitch != parentControlStatus) {
                    setParentalControlOn(parentControlSwitch);
                }
                if (parentControlSwitch) {
                    syncParentControlSetting();
                }*/
            }

            if (playerPlay(dvbUri).equals("ok")) {
                if (mHandlerThreadHandle != null) {
                    mHandlerThreadHandle.removeMessages(MSG_CHECK_PARENTAL_CONTROL);
                    mHandlerThreadHandle.sendEmptyMessageDelayed(MSG_CHECK_PARENTAL_CONTROL, MSG_CHECK_PARENTAL_CONTROL_PERIOD);
                }
                DtvkitGlueClient.getInstance().registerSignalHandler(mHandler);
                if (mCaptioningManager != null && mCaptioningManager.isEnabled()) {
                    playerSetSubtitlesOn(true);
                }
            } else {
                mTunedChannel = null;
                notifyVideoUnavailable(TvInputManager.VIDEO_UNAVAILABLE_REASON_UNKNOWN);
                notifyTimeShiftStatusChanged(TvInputManager.TIME_SHIFT_STATUS_UNAVAILABLE);

                Log.e(TAG, "No play path available");
                Bundle event = new Bundle();
                event.putString(ConstantManager.KEY_INFO, "No play path available");
                notifySessionEvent(ConstantManager.EVENT_RESOURCE_BUSY, event);
            }
            Log.i(TAG, "onTuneByHandlerThreadHandle Done");
            return mTunedChannel != null;
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
                String realtimeVideoFormat = mSysSettingManager.getVideoFormatFromSys();
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
    }

    private void onChannelsChanged() {
        mChannels = TvContractUtils.buildChannelMap(mContentResolver, mInputId);
    }

    private Channel getChannel(Uri channelUri) {
        return mChannels.get(ContentUris.parseId(channelUri));
    }

    private String getChannelInternalDvbUri(Channel channel) {
        try {
            return channel.getInternalProviderData().get("dvbUri").toString();
        } catch (Exception e) {
            Log.e(TAG, "getChannelInternalDvbUri Exception = " + e.getMessage());
            return "dvb://0000.0000.0000";
        }
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
        try {
            JSONArray args = new JSONArray();
            args.put(volume);
            DtvkitGlueClient.getInstance().request("Player.setVolume", args);
        } catch (Exception e) {
            Log.e(TAG, "playerSetVolume = " + e.getMessage());
        }
    }

    private void playerSetMute(boolean mute) {
        try {
            JSONArray args = new JSONArray();
            args.put(mute);
            DtvkitGlueClient.getInstance().request("Player.setMute", args);
        } catch (Exception e) {
            Log.e(TAG, "playerSetMute = " + e.getMessage());
        }
    }

    private void playerSetSubtitlesOn(boolean on) {
        try {
            JSONArray args = new JSONArray();
            args.put(on);
            DtvkitGlueClient.getInstance().request("Player.setSubtitlesOn", args);
            Log.i(TAG, "playerSetSubtitlesOn on =  " + on);
        } catch (Exception e) {
            Log.e(TAG, "playerSetSubtitlesOn=  " + e.getMessage());
        }
    }

    private String playerPlay(String dvbUri) {
        try {
            JSONArray args = new JSONArray();
            args.put(dvbUri);
            AudioManager audioManager = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
            audioManager.setParameters("tuner_in=dtv");
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

    private void playerStop() {
        try {
            JSONArray args = new JSONArray();
            DtvkitGlueClient.getInstance().request("Player.stop", args);
        } catch (Exception e) {
            Log.e(TAG, "playerStop = " + e.getMessage());
        }
    }

    private boolean playerPause() {
        try {
            JSONArray args = new JSONArray();
            DtvkitGlueClient.getInstance().request("Player.pause", args);
            return true;
        } catch (Exception e) {
            Log.e(TAG, "playerPause = " + e.getMessage());
            return false;
        }
    }

    private boolean playerResume() {
        try {
            JSONArray args = new JSONArray();
            DtvkitGlueClient.getInstance().request("Player.resume", args);
            return true;
        } catch (Exception e) {
            Log.e(TAG, "playerResume = " + e.getMessage());
            return false;
        }
    }

    private void playerFastForward() {
        try {
            JSONArray args = new JSONArray();
            DtvkitGlueClient.getInstance().request("Player.fastForward", args);
        } catch (Exception e) {
            Log.e(TAG, "playerFastForwards" + e.getMessage());
        }
    }

    private void playerFastRewind() {
        try {
            JSONArray args = new JSONArray();
            DtvkitGlueClient.getInstance().request("Player.fastRewind", args);
        } catch (Exception e) {
            Log.e(TAG, "playerFastRewind = " + e.getMessage());
        }
    }

    private boolean playerSetSpeed(float speed) {
        try {
            JSONArray args = new JSONArray();
            args.put((long)(speed * 100.0));
            DtvkitGlueClient.getInstance().request("Player.setPlaySpeed", args);
            return true;
        } catch (Exception e) {
            Log.e(TAG, "playerSetSpeed = " + e.getMessage());
            return false;
        }
    }

    private boolean playerSeekTo(long positionSecs) {
        try {
            JSONArray args = new JSONArray();
            args.put(positionSecs);
            DtvkitGlueClient.getInstance().request("Player.seekTo", args);
            return true;
        } catch (Exception e) {
            Log.e(TAG, "playerSeekTo = " + e.getMessage());
            return false;
        }
    }

    private boolean playerStartTimeshiftRecording() {
        try {
            JSONArray args = new JSONArray();
            DtvkitGlueClient.getInstance().request("Player.startTimeshiftRecording", args);
            return true;
        } catch (Exception e) {
            Log.e(TAG, "playerStartTimeshiftRecording = " + e.getMessage());
            return false;
        }
    }

    private boolean playerStopTimeshiftRecording(boolean returnToLive) {
        try {
            JSONArray args = new JSONArray();
            args.put(returnToLive);
            DtvkitGlueClient.getInstance().request("Player.stopTimeshiftRecording", args);
            return true;
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
            return false;
        }
    }

    private boolean playerPlayTimeshiftRecording(boolean startPlaybackPaused, boolean playFromCurrent) {
        try {
            JSONArray args = new JSONArray();
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
        try {
            JSONArray args = new JSONArray();
            args.put(x);
            args.put(y);
            args.put(width);
            args.put(height);
            DtvkitGlueClient.getInstance().request("Player.setRectangle", args);
        } catch (Exception e) {
            Log.e(TAG, "playerSetRectangle = " + e.getMessage());
        }
    }

    private List<TvTrackInfo> playerGetTracks() {
        List<TvTrackInfo> tracks = new ArrayList<>();
        try {
            List<TvTrackInfo> audioTracks = new ArrayList<>();
            JSONArray args = new JSONArray();
            JSONArray audioStreams = DtvkitGlueClient.getInstance().request("Player.getListOfAudioStreams", args).getJSONArray("data");
            int undefinedIndex = 1;
            for (int i = 0; i < audioStreams.length(); i++)
            {
                JSONObject audioStream = audioStreams.getJSONObject(i);
                Log.d(TAG, "getListOfAudioStreams audioStream = " + audioStream.toString());
                TvTrackInfo.Builder track = new TvTrackInfo.Builder(TvTrackInfo.TYPE_AUDIO, Integer.toString(audioStream.getInt("index")));
                String audioLang = audioStream.getString("language");
                if (TextUtils.isEmpty(audioLang) || ConstantManager.CONSTANT_UND_FLAG.equals(audioLang)) {
                    audioLang = ConstantManager.CONSTANT_UND_VALUE + undefinedIndex;
                    undefinedIndex++;
                } else if (ConstantManager.CONSTANT_QAA.equalsIgnoreCase(audioLang)) {
                    audioLang = ConstantManager.CONSTANT_ORIGINAL_AUDIO;
                }
                track.setLanguage(audioLang);
                if (audioStream.getBoolean("ad")) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        track.setDescription("AD");
                    }
                }
                String codes = audioStream.getString("codec");
                int pid = audioStream.getInt("pid");
                Bundle bundle = new Bundle();
                if (!TextUtils.isEmpty(codes)) {
                    bundle.putString(ConstantManager.KEY_AUDIO_CODES_DES, codes);
                }
                bundle.putInt(ConstantManager.KEY_TRACK_PID, pid);
                track.setExtra(bundle);
                audioTracks.add(track.build());
            }
            ConstantManager.ascendTrackInfoOderByPid(audioTracks);
            tracks.addAll(audioTracks);
        } catch (Exception e) {
            Log.e(TAG, "getListOfAudioStreams = " + e.getMessage());
        }
        try {
            List<TvTrackInfo> subTracks = new ArrayList<>();
            JSONArray args = new JSONArray();
            JSONArray subtitleStreams = DtvkitGlueClient.getInstance().request("Player.getListOfSubtitleStreams", args).getJSONArray("data");
            int undefinedIndex = 1;
            for (int i = 0; i < subtitleStreams.length(); i++)
            {
                JSONObject subtitleStream = subtitleStreams.getJSONObject(i);
                Log.d(TAG, "getListOfSubtitleStreams subtitleStream = " + subtitleStream.toString());
                String trackId = null;
                if (subtitleStream.getBoolean("teletext")) {
                    int teletexttype = subtitleStream.getInt("teletext_type");
                    if (teletexttype == 2 || teletexttype == 5) {
                        trackId = "id=" + Integer.toString(subtitleStream.getInt("index")) + "&" + "type=" + "4" + "&teletext=1";
                    } else {
                        continue;
                    }
                } else {
                    trackId = "id=" + Integer.toString(subtitleStream.getInt("index")) + "&" + "type=" + "4" + "&teletext=0";//TYPE_DTV_CC
                }
                TvTrackInfo.Builder track = new TvTrackInfo.Builder(TvTrackInfo.TYPE_SUBTITLE, trackId);
                String subLang = subtitleStream.getString("language");
                if (TextUtils.isEmpty(subLang) || ConstantManager.CONSTANT_UND_FLAG.equals(subLang)) {
                    subLang = ConstantManager.CONSTANT_UND_VALUE + undefinedIndex;
                    undefinedIndex++;
                }
                track.setLanguage(subLang);
                int pid = subtitleStream.getInt("pid");
                Bundle bundle = new Bundle();
                bundle.putInt(ConstantManager.KEY_TRACK_PID, pid);
                track.setExtra(bundle);
                subTracks.add(track.build());
            }
            ConstantManager.ascendTrackInfoOderByPid(subTracks);
            tracks.addAll(subTracks);
            List<TvTrackInfo> teleTracks = new ArrayList<>();
            JSONArray args1 = new JSONArray();
            JSONArray teletextStreams = DtvkitGlueClient.getInstance().request("Player.getListOfTeletextStreams", args1).getJSONArray("data");
            undefinedIndex = 1;
            for (int i = 0; i < teletextStreams.length(); i++)
            {
                JSONObject teletextStream = teletextStreams.getJSONObject(i);
                Log.d(TAG, "getListOfTeletextStreams teletextStream = " + teletextStream.toString());
                String trackId = null;
                int teletextType = teletextStream.getInt("teletext_type");
                if (teletextType == 1 || teletextType == 3 || teletextType == 4) {
                    trackId = "id=" + Integer.toString(teletextStream.getInt("index")) + "&" + "type=" + "6" + "&teletext=1";//TYPE_DTV_TELETEXT_IMG
                } else {
                    continue;
                }
                TvTrackInfo.Builder track = new TvTrackInfo.Builder(TvTrackInfo.TYPE_SUBTITLE, trackId);
                String teleLang = teletextStream.getString("language");
                if (TextUtils.isEmpty(teleLang) || ConstantManager.CONSTANT_UND_FLAG.equals(teleLang)) {
                    teleLang = ConstantManager.CONSTANT_UND_VALUE + undefinedIndex;
                    undefinedIndex++;
                }
                track.setLanguage(teleLang);
                int pid = teletextStream.getInt("pid");
                Bundle bundle = new Bundle();
                bundle.putInt(ConstantManager.KEY_TRACK_PID, pid);
                track.setExtra(bundle);
                teleTracks.add(track.build());
            }
            ConstantManager.ascendTrackInfoOderByPid(teleTracks);
            tracks.addAll(teleTracks);
        } catch (Exception e) {
            Log.e(TAG, "getListOfSubtitleStreams = " + e.getMessage());
        }
        return tracks;
    }

    private boolean playerSelectAudioTrack(int index) {
        try {
            JSONArray args = new JSONArray();
            args.put(index);
            DtvkitGlueClient.getInstance().request("Player.setAudioStream", args);
        } catch (Exception e) {
            Log.e(TAG, "playerSelectAudioTrack = " + e.getMessage());
            return false;
        }
        return true;
    }

    private boolean playerSelectSubtitleTrack(int index) {
        try {
            JSONArray args = new JSONArray();
            args.put(index);
            DtvkitGlueClient.getInstance().request("Player.setSubtitleStream", args);
        } catch (Exception e) {
            Log.e(TAG, "playerSelectSubtitleTrack = " + e.getMessage());
            return false;
        }
        return true;
    }

    private boolean playerSelectTeletextTrack(int index) {
        try {
            JSONArray args = new JSONArray();
            args.put(index);
            DtvkitGlueClient.getInstance().request("Player.setTeletextStream", args);
        } catch (Exception e) {
            Log.e(TAG, "playerSelectTeletextTrack = " + e.getMessage());
            return false;
        }
        return true;
    }

    //called when teletext on
    private boolean playerStartTeletext(int index) {
        try {
            JSONArray args = new JSONArray();
            args.put(index);
            DtvkitGlueClient.getInstance().request("Player.startTeletext", args);
        } catch (Exception e) {
            Log.e(TAG, "playerStartTeletext = " + e.getMessage());
            return false;
        }
        return true;
    }

    //called when teletext off
    private boolean playerStopTeletext() {
        try {
            JSONArray args = new JSONArray();
            DtvkitGlueClient.getInstance().request("Player.stopTeletext", args);
        } catch (Exception e) {
            Log.e(TAG, "playerStopTeletext = " + e.getMessage());
            return false;
        }
        return true;
    }

    private boolean playerPauseTeletext() {
        try {
            JSONArray args = new JSONArray();
            DtvkitGlueClient.getInstance().request("Player.pauseTeletext", args);
        } catch (Exception e) {
            Log.e(TAG, "playerPauseTeletext = " + e.getMessage());
            return false;
        }
        return true;
    }

    private boolean playerResumeTeletext() {
        try {
            JSONArray args = new JSONArray();
            DtvkitGlueClient.getInstance().request("Player.resumeTeletext", args);
        } catch (Exception e) {
            Log.e(TAG, "playerResumeTeletext = " + e.getMessage());
            return false;
        }
        return true;
    }

    private boolean playerIsTeletextDisplayed() {
        boolean on = false;
        try {
            JSONArray args = new JSONArray();
            on = DtvkitGlueClient.getInstance().request("Player.isTeletextDisplayed", args).getBoolean("data");
        } catch (Exception e) {
            Log.e(TAG, "playerResumeTeletext = " + e.getMessage());
        }
        return on;
    }

    private boolean playerIsTeletextStarted() {
        boolean on = false;
        try {
            JSONArray args = new JSONArray();
            on = DtvkitGlueClient.getInstance().request("Player.isTeletextStarted", args).getBoolean("data");
        } catch (Exception e) {
            Log.e(TAG, "playerIsTeletextStarted = " + e.getMessage());
        }
        Log.d(TAG, "playerIsTeletextStarted on = " + on);
        return on;
    }

    private boolean playerSetTeletextOn(boolean on, int index) {
        boolean result = false;
        if (on) {
            result = playerStartTeletext(index);
        } else {
            result = playerStopTeletext();
        }
        return result;
    }

    private boolean playerIsTeletextOn() {
        return playerIsTeletextStarted();
    }

    private boolean playerNotifyTeletextEvent(int event) {
        boolean on = false;
        try {
            JSONArray args = new JSONArray();
            args.put(event);
            DtvkitGlueClient.getInstance().request("Player.notifyTeletextEvent", args);
        } catch (Exception e) {
            Log.e(TAG, "playerNotifyTeletextEvent = " + e.getMessage());
            return false;
        }
        return true;
    }

    private int[] playerGetDTVKitVideoSize() {
        int[] result = {0, 0};
        try {
            JSONArray args = new JSONArray();
            JSONObject videoStreams = DtvkitGlueClient.getInstance().request("Player.getDTVKitVideoResolution", args);
            if (videoStreams != null) {
                videoStreams = (JSONObject)videoStreams.get("data");
                if (!(videoStreams == null || videoStreams.length() == 0)) {
                    Log.d(TAG, "playerGetDTVKitVideoSize videoStreams = " + videoStreams.toString());
                    result[0] = (int)videoStreams.get("width");
                    result[1] = (int)videoStreams.get("height");
                }
            } else {
                Log.d(TAG, "playerGetDTVKitVideoSize then get null");
            }
        } catch (Exception e) {
            Log.e(TAG, "playerGetDTVKitVideoSize = " + e.getMessage());
        }
        return result;
    }

    private int playerGetSelectedSubtitleTrack() {
        int index = 0xFFFF;
        try {
            JSONArray args = new JSONArray();
            JSONArray subtitleStreams = DtvkitGlueClient.getInstance().request("Player.getListOfSubtitleStreams", args).getJSONArray("data");
            for (int i = 0; i < subtitleStreams.length(); i++)
            {
                JSONObject subtitleStream = subtitleStreams.getJSONObject(i);
                if (subtitleStream.getBoolean("selected")) {
                    boolean isTele = subtitleStream.getBoolean("teletext");
                    int teleType = subtitleStream.getInt("teletext_type");
                    if (isTele && (teleType != 2 && teleType != 5)) {
                        Log.i(TAG, "playerGetSelectedSubtitleTrack skip teletext");
                        continue;
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
        String trackId = null;
        try {
            JSONArray args = new JSONArray();
            JSONArray subtitleStreams = DtvkitGlueClient.getInstance().request("Player.getListOfSubtitleStreams", args).getJSONArray("data");
            for (int i = 0; i < subtitleStreams.length(); i++)
            {
                JSONObject subtitleStream = subtitleStreams.getJSONObject(i);
                if (subtitleStream.getBoolean("selected")) {
                    boolean isTele = subtitleStream.getBoolean("teletext");
                    int teleType = subtitleStream.getInt("teletext_type");
                    if (isTele) {
                        if ((teleType != 2) && (teleType != 5)) {
                            Log.i(TAG, "playerGetSelectedSubtitleTrack not tele sub");
                            continue;
                        }
                    }
                    trackId = "id=" + Integer.toString(subtitleStream.getInt("index")) + "&" + "type=" + "4" + (isTele ? "&teletext=1" : "&teletext=0");//TYPE_DTV_CC or tele sub
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "playerGetSelectedSubtitleTrackId = " + e.getMessage());
        }
        Log.i(TAG, "playerGetSelectedTeleTextTrack trackId = " + trackId);
        return trackId;
    }

    private int playerGetSelectedTeleTextTrack() {
        int index = 0xFFFF;
        try {
            JSONArray args = new JSONArray();
            JSONArray teletextStreams = DtvkitGlueClient.getInstance().request("Player.getListOfTeletextStreams", args).getJSONArray("data");
            for (int i = 0; i < teletextStreams.length(); i++)
            {
                JSONObject teletextStream = teletextStreams.getJSONObject(i);
                if (teletextStream.getBoolean("selected")) {
                    int teleType = teletextStream.getInt("teletext_type");
                    if (teleType == 2 || teleType == 5) {
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
        String trackId = null;
        try {
            JSONArray args = new JSONArray();
            JSONArray teletextStreams = DtvkitGlueClient.getInstance().request("Player.getListOfTeletextStreams", args).getJSONArray("data");
            for (int i = 0; i < teletextStreams.length(); i++)
            {
                JSONObject teletextStream = teletextStreams.getJSONObject(i);
                if (teletextStream.getBoolean("selected")) {
                    int teleType = teletextStream.getInt("teletext_type");
                    if (teleType == 2 || teleType == 5) {
                        Log.i(TAG, "playerGetSelectedTeleTextTrackId skip tele sub");
                        continue;
                    }
                    trackId = "id=" + Integer.toString(teletextStream.getInt("index")) + "&type=6" + "&teletext=1";//TYPE_DTV_TELETEXT_IMG
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
        int index = 0xFFFF;
        try {
            JSONArray args = new JSONArray();
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
        boolean on = false;
        try {
            JSONArray args = new JSONArray();
            on = DtvkitGlueClient.getInstance().request("Player.getSubtitlesOn", args).getBoolean("data");
        } catch (Exception e) {
            Log.e(TAG, "playerGetSubtitlesOn = " + e.getMessage());
        }
        Log.i(TAG, "playerGetSubtitlesOn on = " + on);
        return on;
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
            Log.e(TAG, e.getMessage());
        }
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
        JSONObject response = null;
        try {
            JSONArray args = new JSONArray();
            response = DtvkitGlueClient.getInstance().request("Player.getStatus", args).getJSONObject("data");
        } catch (Exception e) {
            Log.e(TAG, "playerGetStatus = " + e.getMessage());
        }
        return response;
    }

    private long playerGetElapsed() {
        return playerGetElapsed(playerGetStatus());
    }

    private long playerGetElapsed(JSONObject playerStatus) {
        long elapsed = 0;
        if (playerStatus != null) {
            try {
                JSONObject content = playerStatus.getJSONObject("content");
                if (content.has("elapsed")) {
                    elapsed = content.getLong("elapsed");
                }
            } catch (JSONException e) {
                Log.e(TAG, "playerGetElapsed = " + e.getMessage());
            }
        }
        return elapsed;
    }

    private JSONObject playerGetTimeshiftRecorderStatus() {
        JSONObject response = null;
        try {
            JSONArray args = new JSONArray();
            response = DtvkitGlueClient.getInstance().request("Player.getTimeshiftRecorderStatus", args).getJSONObject("data");
        } catch (Exception e) {
            Log.e(TAG, "playerGetTimeshiftRecorderStatus = " + e.getMessage());
        }
        return response;
    }

    private int playerGetTimeshiftBufferSize() {
        int timeshiftBufferSize = 0;
        try {
            JSONArray args = new JSONArray();
            timeshiftBufferSize = DtvkitGlueClient.getInstance().request("Player.getTimeshiftBufferSize", args).getInt("data");
        } catch (Exception e) {
            Log.e(TAG, "playerGetTimeshiftBufferSize = " + e.getMessage());
        }
        return timeshiftBufferSize;
    }

    private boolean playerSetTimeshiftBufferSize(int timeshiftBufferSize) {
        try {
            JSONArray args = new JSONArray();
            args.put(timeshiftBufferSize);
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

   private boolean recordingStartRecording(String dvbUri, StringBuffer response) {
       try {
           JSONArray args = new JSONArray();
           args.put(dvbUri);
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
               Cursor cursor = mContentResolver.query(TvContract.RecordedPrograms.CONTENT_URI, RecordedProgram.PROJECTION, null, null, TvContract.RecordedPrograms._ID + " DESC");
               JSONArray recordings = recordingGetListOfRecordings();
               JSONArray activeRecordings = recordingGetActiveRecordings();

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
           }
       }).start();
    }

    private void scheduleTimeshiftRecordingTask() {
       final long SCHEDULE_TIMESHIFT_RECORDING_DELAY_MILLIS = 1000 * 2;
       Log.i(TAG, "calling scheduleTimeshiftRecordingTask");
       if (scheduleTimeshiftRecordingHandler == null) {
            scheduleTimeshiftRecordingHandler = new Handler(Looper.getMainLooper());
       } else {
            scheduleTimeshiftRecordingHandler.removeCallbacks(timeshiftRecordRunnable);
       }
       scheduleTimeshiftRecordingHandler.postDelayed(timeshiftRecordRunnable, SCHEDULE_TIMESHIFT_RECORDING_DELAY_MILLIS);
    }

    private void removeScheduleTimeshiftRecordingTask() {
        Log.i(TAG, "calling removeScheduleTimeshiftRecordingTask");
        if (scheduleTimeshiftRecordingHandler != null) {
            scheduleTimeshiftRecordingHandler.removeCallbacks(timeshiftRecordRunnable);
        }
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
            mConfigs = configs;
        }
    };

    public ResolveInfo getResolveInfo(String cls_name) {
        if (TextUtils.isEmpty(cls_name))
            return null;
        ResolveInfo ret_ri = null;
        PackageManager pm = getApplicationContext().getPackageManager();
        List<ResolveInfo> services = pm.queryIntentServices(new Intent(TvInputService.SERVICE_INTERFACE),
                PackageManager.GET_SERVICES | PackageManager.GET_META_DATA);
        for (ResolveInfo ri : services) {
            ServiceInfo si = ri.serviceInfo;
            if (!android.Manifest.permission.BIND_TV_INPUT.equals(si.permission)) {
                continue;
            }
            Log.d(TAG, "cls_name = " + cls_name + ", si.name = " + si.name);
            if (cls_name.equals(si.name)) {
                ret_ri = ri;
                break;
            }
        }
        return ret_ri;
    }

    public TvInputInfo onHardwareAdded(TvInputHardwareInfo hardwareInfo) {
        Log.d(TAG, "onHardwareAdded ," + "DeviceId :" + hardwareInfo.getDeviceId());
        if (hardwareInfo.getDeviceId() != 19)
            return null;
        ResolveInfo rinfo = getResolveInfo(DtvkitTvInput.class.getName());
        if (rinfo != null) {
            try {
            mTvInputInfo = TvInputInfo.createTvInputInfo(getApplicationContext(), rinfo, hardwareInfo, null, null);
            } catch (XmlPullParserException e) {
                //TODO: handle exception
            } catch (IOException e) {
                //TODO: handle exception
            }
        }
        setInputId(mTvInputInfo.getId());
        mHardware = mTvInputManager.acquireTvInputHardware(19,mHardwareCallback,mTvInputInfo);
        return mTvInputInfo;
    }

    public String onHardwareRemoved(TvInputHardwareInfo hardwareInfo) {
        Log.d(TAG, "onHardwareRemoved");
        if (hardwareInfo.getDeviceId() != 19)
            return null;
        String id = null;
        if (mTvInputInfo != null) {
            id = mTvInputInfo.getId();
            mTvInputInfo = null;
        }
        return id;
    }

    private boolean getFeatureSupportTimeshifting() {
        return !PropSettingManager.getBoolean("tv.dtv.tf.disable", false);
    }
}
