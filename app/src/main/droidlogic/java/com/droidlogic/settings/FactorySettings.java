package com.droidlogic.settings;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import java.util.List;

import org.dtvkit.companionlibrary.EpgSyncJobService;
import org.dtvkit.inputsource.DtvkitEpgSync;
import org.dtvkit.inputsource.R;
import com.droidlogic.fragment.ParameterMananer;

public class FactorySettings {
    private static final String TAG = "FactorySettings";
    private Context mContext;
    private ParameterMananer mParameterManager = null;
    private String mInput = null;
    private FactorySettingsCallback mCallback;
    private TextView textView_Message;
    private HandlerThread mHandlerThread = null;
    private Handler mHandler = null, mThreadHandler = null;
    private boolean mStartSync = false;
    private String NotifyAppendText = "";

    private final static int MSG_DO_EXPORT_CHANNEL  = 0;
    private final static int MSG_DO_IMPORT_CHANNEL  = 1;
    private final static int MSG_DO_EXPORT_SATS     = 2;
    private final static int MSG_DO_IMPORT_SATS     = 3;
    private final static int MSG_DO_REST_TO_DEFAULT = 4;
    private final static int MSG_SYNC               = 5;
    private final static int MSG_FINISH             = 6;

    /**
     * @param context Need ActivityContext
     */
    public FactorySettings(final Context context, ParameterMananer parameter,
                               String input_id, FactorySettingsCallback callback) {
        mContext = context;
        mParameterManager = parameter;
        mInput = input_id;
        mCallback = callback;
        mHandlerThread = new HandlerThread("FactorySettingsThread");
        mHandlerThread.start();
        mHandler = new Handler();
        mThreadHandler = new Handler(mHandlerThread.getLooper()) {
            @Override
            public void handleMessage(Message msg) {
                boolean ret = false;
                updateNotifyMessage("working");
                switch (msg.what) {
                    case MSG_DO_EXPORT_CHANNEL: {
                        ret = doExportChannels();
                        Message message = mThreadHandler.obtainMessage(MSG_FINISH);
                        message.arg1 = ret?1:0;
                        mThreadHandler.sendMessageDelayed(message, 100);
                        break;
                    }
                    case MSG_DO_IMPORT_CHANNEL: {
                        ret = doImportChannels();
                        if (ret)
                            mThreadHandler.sendEmptyMessageDelayed(MSG_SYNC, 100);
                        else {
                            Message message = mThreadHandler.obtainMessage(MSG_FINISH);
                            message.arg1 = 0;
                            mThreadHandler.sendMessageDelayed(message, 100);
                        }
                        break;
                    }
                    case MSG_DO_EXPORT_SATS: {
                        ret = doExportSatellites();
                        Message message = mThreadHandler.obtainMessage(MSG_FINISH);
                        message.arg1 = ret?1:0;
                        mThreadHandler.sendMessageDelayed(message, 100);
                        break;
                    }
                    case MSG_DO_IMPORT_SATS: {
                        ret = doImportSatellites();
                        Message message = mThreadHandler.obtainMessage(MSG_FINISH);
                        message.arg1 = ret?1:0;
                        mThreadHandler.sendMessageDelayed(message, 100);
                        break;
                    }
                    case MSG_DO_REST_TO_DEFAULT: {
                        ret = doRestoreDefaults();
                        if (ret)
                            mThreadHandler.sendEmptyMessageDelayed(MSG_SYNC, 100);
                        else {
                            Message message = mThreadHandler.obtainMessage(MSG_FINISH);
                            message.arg1 = 0;
                            mThreadHandler.sendMessageDelayed(message, 100);
                        }
                        break;
                    }
                    case MSG_SYNC: {
                        startSync();
                        break;
                    }
                    case MSG_FINISH: {
                        stopSync();
                        if (msg.arg1 > 0)
                            updateNotifyMessage("done" + NotifyAppendText);
                        else
                            updateNotifyMessage("failed" + NotifyAppendText);
                        synchronized (this) {
                            NotifyAppendText = "";
                        }
                    }
                    default:
                        break;
                }
            }
        };
    }

    public interface FactorySettingsCallback {
        public void onDismiss();
    }

    private final BroadcastReceiver syncReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, final Intent intent) {
            String status = intent.getStringExtra(EpgSyncJobService.SYNC_STATUS);
            if (status.equals(EpgSyncJobService.SYNC_FINISHED)) {
                mThreadHandler.removeMessages(MSG_FINISH);
                Message message = mThreadHandler.obtainMessage(MSG_FINISH);
                message.arg1 = 1;
                mThreadHandler.sendMessage(message);
            }
        }
    };

    private void startSync() {
        synchronized (this) {
            if (!mStartSync) {
                mStartSync = true;
                LocalBroadcastManager.getInstance(mContext).registerReceiver(syncReceiver,
                        new IntentFilter(EpgSyncJobService.ACTION_SYNC_STATUS_CHANGED));
                EpgSyncJobService.cancelAllSyncRequests(mContext);
                /*EpgSyncJobService.requestImmediateSyncSearchedChannelWitchParameters(mContext,
                    mInput, true, new ComponentName(mContext, DtvkitEpgSync.class), new Bundle());*/
                Bundle parameters = new Bundle();
                parameters.putString(EpgSyncJobService.BUNDLE_KEY_SYNC_SEARCHED_MODE, EpgSyncJobService.BUNDLE_VALUE_SYNC_SEARCHED_MODE_AUTO);
                parameters.putString(EpgSyncJobService.BUNDLE_KEY_SYNC_SEARCHED_SIGNAL_TYPE, null);
                EpgSyncJobService.requestImmediateSyncSearchedChannelWitchParameters(mContext, mInput, false,new ComponentName(mContext, DtvkitEpgSync.class), parameters);
           }
        }
    }

    private void stopSync() {
        synchronized (this) {
            if (mStartSync) {
                mStartSync = false;
                LocalBroadcastManager.getInstance(mContext).unregisterReceiver(syncReceiver);
            }
        }
    }

    private View.OnClickListener listener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.btn_export_db: {
                    mThreadHandler.removeMessages(MSG_DO_EXPORT_CHANNEL);
                    mThreadHandler.sendEmptyMessageDelayed(MSG_DO_EXPORT_CHANNEL, 200);
                    break;
                }
                case R.id.btn_import_db: {
                    mThreadHandler.removeMessages(MSG_DO_IMPORT_CHANNEL);
                    mThreadHandler.sendEmptyMessageDelayed(MSG_DO_IMPORT_CHANNEL, 200);
                    break;
                }
                case R.id.btn_export_sat: {
                    mThreadHandler.removeMessages(MSG_DO_EXPORT_SATS);
                    mThreadHandler.sendEmptyMessageDelayed(MSG_DO_EXPORT_SATS, 200);
                    break;
                }
                case R.id.btn_import_sat: {
                    mThreadHandler.removeMessages(MSG_DO_IMPORT_SATS);
                    mThreadHandler.sendEmptyMessageDelayed(MSG_DO_IMPORT_SATS, 200);
                    break;
                }
                case R.id.btn_rest_default: {
                    mThreadHandler.removeMessages(MSG_DO_REST_TO_DEFAULT);
                    mThreadHandler.sendEmptyMessageDelayed(MSG_DO_REST_TO_DEFAULT, 200);
                    break;
                }
                default:
                    break;
            }
        }
    };

    public void start() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        final AlertDialog alert = builder.create();
        final View dialogView = View.inflate(mContext, R.layout.factory_settings, null);
        Button exportChannels = dialogView.findViewById(R.id.btn_export_db);
        Button importChannels = dialogView.findViewById(R.id.btn_import_db);
        Button exportSatellites = dialogView.findViewById(R.id.btn_export_sat);
        Button importSatellites = dialogView.findViewById(R.id.btn_import_sat);
        Button restoreToDefault = dialogView.findViewById(R.id.btn_rest_default);
        textView_Message = dialogView.findViewById(R.id.tv_message);
        exportChannels.setOnClickListener(listener);
        importChannels.setOnClickListener(listener);
        exportSatellites.setOnClickListener(listener);
        importSatellites.setOnClickListener(listener);
        restoreToDefault.setOnClickListener(listener);
        exportChannels.requestFocus();

        alert.setView(dialogView);
        WindowManager.LayoutParams params = alert.getWindow().getAttributes();
        params.width = (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 500, mContext.getResources().getDisplayMetrics());
        params.height = WindowManager.LayoutParams.WRAP_CONTENT;
        alert.getWindow().setAttributes(params);
        alert.show();
        alert.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                synchronized (this) {
                    mThreadHandler.removeCallbacksAndMessages(null);
                    mHandlerThread.getLooper().quitSafely();
                    mThreadHandler = null;
                    mHandlerThread = null;
                    mHandler.removeCallbacksAndMessages(null);
                    mHandler = null;
                    if (mCallback != null) {
                        mCallback.onDismiss();
                    }
                }
            }
        });
    }

    private void updateNotifyMessage(final String msg) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                textView_Message.setText(msg);
            }
        });
    }

    private String getValidExternalStoragePath() {
        SysSettingManager sysSettingManager = new SysSettingManager(mContext);
        List<String> storageList = sysSettingManager.getStorageDevicePathList();
        String removeablePath = null;
        for (String path: storageList) {
            if (sysSettingManager.isMediaPath(path)) {
                String mountedPath = sysSettingManager.convertMediaPathToMountedPath(path);
                if (sysSettingManager.isMountedPathAvailable(mountedPath)) {
                    removeablePath = path;
                    break;
                } else {
                    removeablePath = null;
                }
            }
        }
        sysSettingManager = null;
        return removeablePath;
    }

    private boolean doRestoreDefaults() {
        boolean ret = false;
        synchronized (this) {
            Log.d(TAG, "doRestoreDefaults");
            if (mParameterManager != null) {
                ret = mParameterManager.restoreToDefault();
            }
        }
        return ret;
    }

    private boolean doImportSatellites() {
        boolean ret = false;
        synchronized (this) {
            Log.d(TAG, "doImportSatellites");
            String path = getValidExternalStoragePath();
            if (path == null) {
                Log.e(TAG, "No external storage exists, faild do export.");
                NotifyAppendText = ", No storage exists!";
            } else {
                if (mParameterManager != null) {
                    ret = mParameterManager.importSatellites(path + "/dtvkit_satellites.json");
                    if (ret) {
                        NotifyAppendText = ", successfully import satellites from " + path + "/dtvkit_satellites.json";
                    }
                }
            }
        }
        return ret;
    }

    private boolean doExportSatellites() {
        boolean ret = false;
        synchronized (this) {
            Log.d(TAG, "doExportSatellites");
            String path = getValidExternalStoragePath();
            if (path == null) {
                Log.e(TAG, "No external storage exists, faild do export.");
                NotifyAppendText = ", No storage exists!";
            } else {
                if (mParameterManager != null) {
                    ret = mParameterManager.exportSatellites(path + "/dtvkit_satellites.json");
                    if (ret) {
                        NotifyAppendText = ", Satellites have been exported to " + path + "/dtvkit_satellites.json";
                    }
                }
            }
        }
        return ret;
    }

    private boolean doImportChannels() {
        boolean ret = false;
        synchronized (this) {
            Log.d(TAG, "doImportChannels");
            String path = getValidExternalStoragePath();
            if (path == null) {
                Log.e(TAG, "No external storage exists, faild do import.");
                NotifyAppendText = ", No storage exists!";
            } else {
                if (mParameterManager != null) {
                    ret = mParameterManager.importChannels(path + "/dtvkit_channels.xml");
                    if (ret) {
                        NotifyAppendText = ", successfully import channels from " + path + "/dtvkit_channels.xml";
                    }
                }
            }
        }
        return ret;
    }

    private boolean doExportChannels() {
        boolean ret = false;
        synchronized (this) {
            Log.d(TAG, "doExportChannels");
            String path = getValidExternalStoragePath();
            if (path == null) {
                Log.e(TAG, "No external storage exists, faild do export.");
                NotifyAppendText = ", No storage exists!";
            } else {
                if (mParameterManager != null) {
                    ret = mParameterManager.exportChannels(path + "/dtvkit_channels.xml");
                    if (ret) {
                        NotifyAppendText = ", Channels have been exported to " + path + "/dtvkit_channels.xml";
                    }
                }
            }
        }
        return ret;
    }
}
