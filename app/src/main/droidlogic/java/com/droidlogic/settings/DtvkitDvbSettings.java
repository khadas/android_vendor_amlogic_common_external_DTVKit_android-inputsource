package com.droidlogic.settings;

import android.util.Log;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Spinner;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.text.TextUtils;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.util.TypedValue;
import android.view.WindowManager;
import android.os.Handler;
import android.os.Message;
import android.content.Context;
import android.widget.TextView;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.content.BroadcastReceiver;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

import com.droidlogic.fragment.ParameterMananer;

import org.droidlogic.dtvkit.DtvkitGlueClient;
import org.dtvkit.inputsource.R;

import android.content.ComponentName;
import android.media.tv.TvInputInfo;
import org.dtvkit.companionlibrary.EpgSyncJobService;
import org.dtvkit.inputsource.DtvkitEpgSync;

import com.droidlogic.settings.SysSettingManager;
import com.droidlogic.settings.PropSettingManager;

public class DtvkitDvbSettings extends Activity {

    private static final String TAG = "DtvkitDvbSettings";
    private static final boolean DEBUG = true;

    private DtvkitGlueClient mDtvkitGlueClient = DtvkitGlueClient.getInstance();
    private ParameterMananer mParameterMananer = null;
    private SysSettingManager mSysSettingManager = null;
    private List<String> mStoragePathList = new ArrayList<String>();
    private List<String> mStorageNameList = new ArrayList<String>();
    private Object mStorageLock = new Object();
    private boolean needClearAudioLangSetting = false;

    private FormatDialogCallBack mFormatDialogCallBack = null;
    private StorageStatusBroadcastReceiver mStorageStatusBroadcastReceiver = null;
    private String mCurrentFormattingDeviceId = null;
    private String mCurrentFormattingVolumeId = null;

    protected static final int MSG_DO_FORMAT = 1;
    protected static final int MSG_FORMAT_DONE = 2;
    protected static final int MSG_HIDE_DIALOG = 3;
    protected static final int MSG_REFRESH_UI = 4;

    protected static final int PERIOD_SHOW_DONE = 1000;
    protected static final int PERIOD_SHOW_DONE_TIME_OUT = 10000;
    protected static final int PERIOD_RIGHT_NOW = 0;

    private Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            Log.d(TAG, "handleMessage start = " + msg.what);
            switch (msg.what) {
                case MSG_DO_FORMAT:
                    String rawPath = (String)msg.obj;
                    formatStorage(rawPath);
                    sendMessageToHandler(MSG_FORMAT_DONE, 0, 0, null, PERIOD_RIGHT_NOW);
                    break;
                case MSG_FORMAT_DONE:
                    showDoneInFormatConfirmDialog();
                    sendMessageToHandler(MSG_HIDE_DIALOG, 0, 0, null, PERIOD_SHOW_DONE_TIME_OUT);
                    break;
                case MSG_HIDE_DIALOG:
                    if (msg.obj != null) {
                        String volumeId = (String)msg.obj;
                        String formattedPath = mSysSettingManager.getStorageRawPathByVolumeId(volumeId);
                        if (!TextUtils.isEmpty(formattedPath)) {
                            mParameterMananer.saveStringParameters(ParameterMananer.KEY_PVR_RECORD_PATH, formattedPath);
                        }
                    }
                    hideFormatConfirmDialog();
                    break;
                case MSG_REFRESH_UI:
                    updateStorageList();
                    initLayout(true);
                    break;
                default:
                    Log.d(TAG, "default");
                    break;
            }
            Log.d(TAG, "handleMessage  end = " + msg.what);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.lanuage_settings);
        mParameterMananer = new ParameterMananer(this, mDtvkitGlueClient);
        mSysSettingManager = new SysSettingManager(this);
        updateStorageList();
        initLayout(false);
    }

    @Override
    protected void onResume() {
        super.onResume();
        needClearAudioLangSetting = false;
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (needClearAudioLangSetting) {
            mParameterMananer.clearUserAudioSelect();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mHandler != null) {
            mHandler.removeCallbacksAndMessages(null);
        }
        unRegisterStorageStatusReceiver();
        hideFormatConfirmDialog();
    }

    private void updatingGuide() {
        EpgSyncJobService.cancelAllSyncRequests(this);
        String inputId = this.getIntent().getStringExtra(TvInputInfo.EXTRA_INPUT_ID);
        Log.i(TAG, String.format("inputId: %s", inputId));
        EpgSyncJobService.requestImmediateSync(this, inputId, true, new ComponentName(this, DtvkitEpgSync.class));
    }

    private void initLayout(boolean update) {
        Spinner country = (Spinner)findViewById(R.id.country_spinner);
        Spinner main_audio = (Spinner)findViewById(R.id.main_audio_spinner);
        Spinner assist_audio = (Spinner)findViewById(R.id.assist_audio_spinner);
        Spinner main_subtitle = (Spinner)findViewById(R.id.main_subtitle_spinner);
        Spinner assist_subtitle = (Spinner)findViewById(R.id.assist_subtitle_spinner);
        Spinner teletext_charset = (Spinner)findViewById(R.id.teletext_charset_spinner);
        Spinner hearing_impaired = (Spinner)findViewById(R.id.hearing_impaired_spinner);
        Spinner pvr_path = (Spinner)findViewById(R.id.storage_select_spinner);
        Button refresh = (Button)findViewById(R.id.storage_refresh);
        CheckBox recordFrequency = (CheckBox)findViewById(R.id.record_full_frequency);
        CheckBox adSupport = (CheckBox)findViewById(R.id.ad_audio_checkbox);
        Button networkUpdate = (Button)findViewById(R.id.network_update_button);
        initSpinnerParameter();
        if (update) {
            Log.d(TAG, "initLayout update");
            return;
        }
        country.setOnItemSelectedListener(new OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                Log.d(TAG, "country onItemSelected position = " + position);
                int saveCountryCode = mParameterMananer.getCurrentCountryCode();
                int selectCountryCode = mParameterMananer.getCountryCodeByIndex(position);
                String previousMainAudioName = mParameterMananer.getCurrentMainAudioLangName();
                String previousAssistAudioName = mParameterMananer.getCurrentSecondAudioLangName();
                if (saveCountryCode == selectCountryCode) {
                    Log.d(TAG, "country onItemSelected same position");
                    return;
                }
                mParameterMananer.setCountryCodeByIndex(position);
                updatingGuide();
                initLayout(true);
                String currentMainAudioName = mParameterMananer.getCurrentMainAudioLangName();
                String currentAssistAudioName = mParameterMananer.getCurrentSecondAudioLangName();
                if (!TextUtils.equals(previousMainAudioName, currentMainAudioName) || !TextUtils.equals(previousAssistAudioName, currentAssistAudioName)) {
                    needClearAudioLangSetting = true;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // TODO Auto-generated method stub
            }
        });
        main_audio.setOnItemSelectedListener(new OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                Log.d(TAG, "main_audio onItemSelected position = " + position);
                int currentMainAudio = mParameterMananer.getCurrentMainAudioLangId();
                int savedMainAudio = mParameterMananer.getLangIndexCodeByPosition(position);
                if (currentMainAudio == savedMainAudio) {
                    Log.d(TAG, "main_audio onItemSelected same position");
                    return;
                }
                mParameterMananer.setPrimaryAudioLangByPosition(position);
                updatingGuide();
                needClearAudioLangSetting = true;
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // TODO Auto-generated method stub
            }
        });
        assist_audio.setOnItemSelectedListener(new OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                Log.d(TAG, "assist_audio onItemSelected position = " + position);
                int currentAssistAudio = mParameterMananer.getCurrentSecondAudioLangId();
                int savedAssistAudio = mParameterMananer.getSecondLangIndexCodeByPosition(position);
                if (currentAssistAudio == savedAssistAudio) {
                    Log.d(TAG, "assist_audio onItemSelected same position");
                    return;
                }
                mParameterMananer.setSecondaryAudioLangByPosition(position);
                updatingGuide();
                needClearAudioLangSetting = true;
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // TODO Auto-generated method stub
            }
        });
        main_subtitle.setOnItemSelectedListener(new OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                Log.d(TAG, "main_subtitle onItemSelected position = " + position);
                int currentMainSub = mParameterMananer.getCurrentMainSubLangId();
                int savedMainSub = mParameterMananer.getLangIndexCodeByPosition(position);
                if (currentMainSub == savedMainSub) {
                    Log.d(TAG, "main_subtitle onItemSelected same position");
                    return;
                }
                mParameterMananer.setPrimaryTextLangByPosition(position);
                updatingGuide();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // TODO Auto-generated method stub
            }
        });
        assist_subtitle.setOnItemSelectedListener(new OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                Log.d(TAG, "assist_subtitle onItemSelected position = " + position);
                int currentAssistSub = mParameterMananer.getCurrentSecondSubLangId();
                int savedAssistSub = mParameterMananer.getSecondLangIndexCodeByPosition(position);
                if (currentAssistSub == savedAssistSub) {
                    Log.d(TAG, "assist_subtitle onItemSelected same position");
                    return;
                }
                mParameterMananer.setSecondaryTextLangByPosition(position);
                updatingGuide();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // TODO Auto-generated method stub
            }
        });
        teletext_charset.setOnItemSelectedListener(new OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                Log.d(TAG, "teletext_charset onItemSelected position = " + position);
                int savedCharSet = mParameterMananer.getCurrentTeletextCharsetIndex();
                if (savedCharSet == position) {
                    Log.d(TAG, "teletext_charset onItemSelected same position");
                    return;
                }
                mParameterMananer.setCurrentTeletextCharsetByPosition(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // TODO Auto-generated method stub
            }
        });
        hearing_impaired.setOnItemSelectedListener(new OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                Log.d(TAG, "hearing_impaired onItemSelected position = " + position);
                int saved = mParameterMananer.getHearingImpairedSwitchStatus() ? 1 : 0;
                if (saved == position) {
                    Log.d(TAG, "hearing_impaired onItemSelected same position");
                    return;
                }
                mParameterMananer.setHearingImpairedSwitchStatus(position == 1);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // TODO Auto-generated method stub
            }
        });
        pvr_path.setOnItemSelectedListener(new OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String saved = mParameterMananer.getStringParameters(ParameterMananer.KEY_PVR_RECORD_PATH);
                String current = getCurrentStoragePath(position);
                Log.d(TAG, "pvr_path onItemSelected previous = " + saved + ", new = " + current);
                if (mSysSettingManager.isMediaPath(current) && !SysSettingManager.isStorageFatFormat(mSysSettingManager.getStorageFsType(current))) {
                    Log.d(TAG, "pvr_path onItemSelected is not fat and need to be formatted");
                    showFormatConfirmDialog(DtvkitDvbSettings.this, current);
                    return;
                }
                if (TextUtils.equals(current, saved)) {
                    Log.d(TAG, "pvr_path onItemSelected same path");
                    return;
                }
                mParameterMananer.saveStringParameters(ParameterMananer.KEY_PVR_RECORD_PATH, current);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // TODO Auto-generated method stub
            }
        });
        refresh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "refresh storage device");
                updateStorageList();
                initLayout(true);
            }
        });
        recordFrequency.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (recordFrequency.isChecked()) {
                    PropSettingManager.setProp(PropSettingManager.PVR_RECORD_MODE, PropSettingManager.PVR_RECORD_MODE_FREQUENCY);
                } else {
                    PropSettingManager.setProp(PropSettingManager.PVR_RECORD_MODE, PropSettingManager.PVR_RECORD_MODE_CHANNEL);
                }
            }
        });
        adSupport.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                mParameterMananer.saveIntParameters(ParameterMananer.TV_KEY_AD_SWITCH, adSupport.isChecked() ? 1 : 0);
            }
        });
        networkUpdate.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                showNetworkInfoConfirmDialog(DtvkitDvbSettings.this, mParameterMananer.getNetworksOfRegion());
            }
        });
    }

    private void initSpinnerParameter() {
        Spinner country = (Spinner)findViewById(R.id.country_spinner);
        Spinner main_audio = (Spinner)findViewById(R.id.main_audio_spinner);
        Spinner assist_audio = (Spinner)findViewById(R.id.assist_audio_spinner);
        Spinner main_subtitle = (Spinner)findViewById(R.id.main_subtitle_spinner);
        Spinner assist_subtitle = (Spinner)findViewById(R.id.assist_subtitle_spinner);
        Spinner teletext_charset = (Spinner)findViewById(R.id.teletext_charset_spinner);
        Spinner hearing_impaired = (Spinner)findViewById(R.id.hearing_impaired_spinner);
        Spinner pvr_path = (Spinner)findViewById(R.id.storage_select_spinner);
        CheckBox recordFrequency = (CheckBox)findViewById(R.id.record_full_frequency);
        CheckBox adSupport = (CheckBox)findViewById(R.id.ad_audio_checkbox);
        LinearLayout networkUpdateContainer = (LinearLayout)findViewById(R.id.network_update);
        List<String> list = null;
        ArrayAdapter<String> adapter = null;
        int select = 0;
        //add country
        list = mParameterMananer.getCountryDisplayList();
        adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, list);
        adapter.setDropDownViewResource(android.R.layout.simple_list_item_single_choice);
        country.setAdapter(adapter);
        select = mParameterMananer.getCurrentCountryIndex();
        country.setSelection(select);
        //add main audio
        list = mParameterMananer.getCurrentLangNameList();
        adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, list);
        adapter.setDropDownViewResource(android.R.layout.simple_list_item_single_choice);
        main_audio.setAdapter(adapter);
        select = mParameterMananer.getCurrentMainAudioLangId();
        main_audio.setSelection(select);
        //add second audio
        list = mParameterMananer.getCurrentSecondLangNameList();
        adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, list);
        adapter.setDropDownViewResource(android.R.layout.simple_list_item_single_choice);
        assist_audio.setAdapter(adapter);
        select = mParameterMananer.getCurrentSecondAudioLangId();
        assist_audio.setSelection(select);
        //add main subtitle
        list = mParameterMananer.getCurrentLangNameList();
        adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, list);
        adapter.setDropDownViewResource(android.R.layout.simple_list_item_single_choice);
        main_subtitle.setAdapter(adapter);
        select = mParameterMananer.getCurrentMainSubLangId();
        main_subtitle.setSelection(select);
        //add second subtitle
        list = mParameterMananer.getCurrentSecondLangNameList();
        adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, list);
        adapter.setDropDownViewResource(android.R.layout.simple_list_item_single_choice);
        assist_subtitle.setAdapter(adapter);
        select = mParameterMananer.getCurrentSecondSubLangId();
        assist_subtitle.setSelection(select);
        //add teletext charset
        list = mParameterMananer.getTeletextCharsetNameList();
        adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, list);
        adapter.setDropDownViewResource(android.R.layout.simple_list_item_single_choice);
        teletext_charset.setAdapter(adapter);
        select = mParameterMananer.getCurrentTeletextCharsetIndex();
        teletext_charset.setSelection(select);
        //add hearing impaired
        list = getHearingImpairedOption();
        adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, list);
        adapter.setDropDownViewResource(android.R.layout.simple_list_item_single_choice);
        hearing_impaired.setAdapter(adapter);
        select = mParameterMananer.getHearingImpairedSwitchStatus() ? 1 : 0;
        hearing_impaired.setSelection(select);
        //add pvr path select
        list = mStorageNameList;
        adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, list);
        adapter.setDropDownViewResource(android.R.layout.simple_list_item_single_choice);
        pvr_path.setAdapter(adapter);
        String devicePath = mParameterMananer.getStringParameters(ParameterMananer.KEY_PVR_RECORD_PATH);
        if (!SysSettingManager.isDeviceExist(devicePath)) {
            select = 0;
            mParameterMananer.saveStringParameters(ParameterMananer.KEY_PVR_RECORD_PATH, SysSettingManager.PVR_DEFAULT_PATH);
        } else {
            select = getCurrentStoragePosition(devicePath);
        }
        pvr_path.setSelection(select);
        String pvrRecordMode = PropSettingManager.getString(PropSettingManager.PVR_RECORD_MODE, PropSettingManager.PVR_RECORD_MODE_CHANNEL);
        recordFrequency.setChecked(PropSettingManager.PVR_RECORD_MODE_FREQUENCY.equals(pvrRecordMode) ? true : false);
        adSupport.setChecked(mParameterMananer.getIntParameters(ParameterMananer.TV_KEY_AD_SWITCH) == 1 ? true : false);
        if (mParameterMananer.needConfirmNetWorkInfomation(mParameterMananer.getNetworksOfRegion())) {
            networkUpdateContainer.setVisibility(View.VISIBLE);
        } else {
            networkUpdateContainer.setVisibility(View.GONE);
        }
    }

    private List<String> getHearingImpairedOption() {
        List<String> result = new ArrayList<String>();
        result.add(getString(R.string.strSettingsHearingImpairedOff));
        result.add(getString(R.string.strSettingsHearingImpairedOn));
        return result;
    }

    private void updateStorageList() {
        synchronized(mStorageLock) {
            mStorageNameList = mSysSettingManager.getStorageDeviceNameList();
            mStoragePathList = mSysSettingManager.getStorageDevicePathList();
        }
    }

    private int getCurrentStoragePosition(String name) {
        int result = 0;
        boolean found = false;
        synchronized(mStorageLock) {
            if (mStoragePathList != null && mStoragePathList.size() > 0) {
                for (int i = 0; i < mStoragePathList.size(); i++) {
                    if (TextUtils.equals(mStoragePathList.get(i), name)) {
                        result = i;
                        found = true;
                        break;
                    }
                }
            }
        }
        if (!found) {
            mParameterMananer.saveStringParameters(ParameterMananer.KEY_PVR_RECORD_PATH, mSysSettingManager.getAppDefaultPath());
        }
        return result;
    }

    private String getCurrentStoragePath(int position) {
        String result = null;
        synchronized(mStorageLock) {
            result = mStoragePathList.get(position);
        }
        return result;
    }

    private void sendMessageToHandler(int msg, int arg1, int arg2, Object obj, int delay) {
        if (mHandler != null) {
            mHandler.removeMessages(msg);
            Message mess = mHandler.obtainMessage(msg, arg1, arg2, obj);
            boolean info = mHandler.sendMessageDelayed(mess, delay);
        }
    }

    private void formatStorage(String rawPath) {
        if (mSysSettingManager != null && mSysSettingManager.isMediaPath(rawPath)) {
            registerStorageStatusReceiver();
            mCurrentFormattingDeviceId = mSysSettingManager.getStorageDeviceId(rawPath);
            mCurrentFormattingVolumeId = mSysSettingManager.getStorageVolumeId(rawPath);
            mSysSettingManager.formatStorageByDiskId(mCurrentFormattingDeviceId);
        } else {
            Log.d(TAG, "formatStorage null volumeId");
        }
    }

    private void showFormatConfirmDialog(final Context context, final String rawPath) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(context);
        final AlertDialog alert = builder.create();
        final View dialogView = View.inflate(context, R.layout.confirm_search, null);
        final TextView title = (TextView) dialogView.findViewById(R.id.dialog_title);
        final Button confirm = (Button) dialogView.findViewById(R.id.confirm);
        final Button cancel = (Button) dialogView.findViewById(R.id.cancel);
        final FormatDialogCallBack callBack = new FormatDialogCallBack() {
            public void onDialogMiss() {
                if (alert != null) {
                    alert.dismiss();
                }
            }

            public void onFormatDone() {
                if (title != null) {
                    title.setText(R.string.strSettingsFormatDialog_formatting_done);
                }
            }
        };
        setFormatDialogCallBack(callBack);
        title.setText(R.string.strSettingsFormatDialog);
        confirm.setText(R.string.strSettingsFormatDialog_ok);
        cancel.setText(R.string.strSettingsFormatDialog_cancel);
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
                confirm.setVisibility(View.GONE);
                cancel.setVisibility(View.GONE);
                title.setText(R.string.strSettingsFormatDialog_formatting);
                sendMessageToHandler(MSG_DO_FORMAT, 0, 0, rawPath, PERIOD_RIGHT_NOW);
            }
        });
        alert.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                Log.d(TAG, "showFormatConfirmDialog onDismiss");
                sendMessageToHandler(MSG_REFRESH_UI, 0, 0, null, PERIOD_RIGHT_NOW);
            }
        });
        alert.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
        alert.setView(dialogView);
        alert.show();
        WindowManager.LayoutParams params = alert.getWindow().getAttributes();
        params.width = (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 500, context.getResources().getDisplayMetrics());
        params.height = WindowManager.LayoutParams.WRAP_CONTENT;
        alert.getWindow().setAttributes(params);
        alert.getWindow().setBackgroundDrawableResource(R.drawable.dialog_background);
    }

    private void setFormatDialogCallBack(FormatDialogCallBack callBack) {
        mFormatDialogCallBack = callBack;
    }

    private void showDoneInFormatConfirmDialog() {
        if (mFormatDialogCallBack != null) {
            mFormatDialogCallBack.onFormatDone();
        }
    }

    private void hideFormatConfirmDialog() {
        if (mFormatDialogCallBack != null) {
            mFormatDialogCallBack.onDialogMiss();
            unRegisterStorageStatusReceiver();
            mFormatDialogCallBack = null;
            mCurrentFormattingDeviceId = null;
            mCurrentFormattingVolumeId = null;
        }
    }

    private interface FormatDialogCallBack {
        void onFormatDone();
        void onDialogMiss();
    }

    private final class StorageStatusBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "onReceive " + intent.toURI());
            if (intent != null) {
                if ("android.os.storage.action.VOLUME_STATE_CHANGED".equals(intent.getAction())) {
                    int state = intent.getIntExtra("android.os.storage.extra.VOLUME_STATE"/*VolumeInfo.EXTRA_VOLUME_STATE*/, -1);
                    String volumeId = intent.getStringExtra("android.os.storage.extra.VOLUME_ID"/*VolumeInfo.EXTRA_VOLUME_ID*/);
                    if (state == 2/*VolumeInfo.STATE_MOUNTED*/ && volumeId != null && volumeId.equals(mCurrentFormattingVolumeId)) {
                        sendMessageToHandler(MSG_HIDE_DIALOG, 0, 0, volumeId, 0);
                    }
                }
            }
        }
    }

    private void registerStorageStatusReceiver() {
        try {
            if (mStorageStatusBroadcastReceiver == null) {
                IntentFilter filter = new IntentFilter();
                filter.addAction("android.os.storage.action.VOLUME_STATE_CHANGED");
                mStorageStatusBroadcastReceiver = new StorageStatusBroadcastReceiver();
                registerReceiver(mStorageStatusBroadcastReceiver, filter);
            }
        } catch (Exception e) {
            Log.d(TAG, "registerStorageStatusReceiver Exception " + e.getMessage());
        }
    }

    private void unRegisterStorageStatusReceiver() {
        try {
            if (mStorageStatusBroadcastReceiver != null) {
                unregisterReceiver(mStorageStatusBroadcastReceiver);
                mStorageStatusBroadcastReceiver = null;
            }
        } catch (Exception e) {
            Log.d(TAG, "unRegisterStorageStatusReceiver Exception " + e.getMessage());
        }
    }

    private void showNetworkInfoConfirmDialog(final Context context, final JSONArray networkArray) {
        Log.d(TAG, "showNetworkInfoConfirmDialog networkArray = " + networkArray);
        final AlertDialog.Builder builder = new AlertDialog.Builder(context);
        final AlertDialog alert = builder.create();
        final View dialogView = View.inflate(context, R.layout.confirm_region_network, null);
        final TextView title = (TextView) dialogView.findViewById(R.id.dialog_title);
        final ListView listView = (ListView) dialogView.findViewById(R.id.dialog_listview);
        final List<HashMap<String, Object>> dataList = new ArrayList<HashMap<String, Object>>();
        if (networkArray != null && networkArray.length() > 0) {
            JSONObject networkObj = null;
            String name = null;
            for (int i = 0; i < networkArray.length(); i++) {
                HashMap<String, Object> map = new HashMap<String, Object>();
                networkObj = mParameterMananer.getJSONObjectFromJSONArray(networkArray, i);
                name = mParameterMananer.getNetworkName(networkObj);
                map.put("name", name);
                dataList.add(map);
            }
        } else {
            Log.d(TAG, "showNetworkInfoConfirmDialog no networkArray");
            return;
        }
        SimpleAdapter adapter = new SimpleAdapter(context, dataList,
                R.layout.region_network_list,
                new String[] {"name"},
                new int[] {R.id.name});

        listView.setAdapter(adapter);
        listView.setSelection(mParameterMananer.getCurrentNetworkIndex(networkArray));
        title.setText(R.string.search_confirm_network);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, final int position, long id) {
                int currentNetWorkIndex = mParameterMananer.getCurrentNetworkIndex(networkArray);
                if (currentNetWorkIndex == position) {
                    Log.d(TAG, "showNetworkInfoConfirmDialog same position = " + position);
                    alert.dismiss();
                    return;
                }
                Log.d(TAG, "showNetworkInfoConfirmDialog onItemClick position = " + position);
                mParameterMananer.setNetworkPreferedOfRegion(mParameterMananer.getNetworkId(networkArray, position));
                updatingGuide();
                alert.dismiss();
            }
        });
        alert.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                Log.d(TAG, "showNetworkInfoConfirmDialog onDismiss");
            }
        });

        alert.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
        alert.setView(dialogView);
        alert.show();
        WindowManager.LayoutParams params = alert.getWindow().getAttributes();
        params.width = (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 500, context.getResources().getDisplayMetrics());
        params.height = WindowManager.LayoutParams.WRAP_CONTENT;
        alert.getWindow().setAttributes(params);
        //alert.getWindow().setBackgroundDrawableResource(R.drawable.dialog_background);
    }
}
