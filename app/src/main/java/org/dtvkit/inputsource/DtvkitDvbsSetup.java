package org.dtvkit.inputsource;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.tv.TvContract;
import android.media.tv.TvInputInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.content.ActivityNotFoundException;
import android.widget.Toast;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.LinearLayout;
import android.text.Editable;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.widget.Toast;

import org.dtvkit.companionlibrary.EpgSyncJobService;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.droidlogic.dtvkit.DtvkitGlueClient;

import com.droidlogic.settings.ConstantManager;
import com.droidlogic.fragment.ParameterMananer;
import com.droidlogic.fragment.DvbsParameterManager;

import java.util.List;
import java.util.Locale;

public class DtvkitDvbsSetup extends Activity {
    private static final String TAG = "DtvkitDvbsSetup";
    private static final int REQUEST_CODE_SET_UP_SETTINGS = 1;
    private DataMananer mDataMananer;
    private boolean mStartSync = false;
    private boolean mStartSearch = false;
    private int mFoundServiceNumber = 0;
    private JSONArray mServiceList = null;
    private int mSearchType = -1;// 0 manual 1 auto
    private int mSearchDvbsType = -1;
    private boolean mSetUp = false;
    private PvrStatusConfirmManager mPvrStatusConfirmManager = null;
    private ParameterMananer mParameterMananer;

    protected HandlerThread mHandlerThread = null;
    protected Handler mThreadHandler = null;

    private final static int MSG_FINISH_SEARCH = 1;
    private final static int MSG_ON_SIGNAL = 2;

    private final DtvkitGlueClient.SignalHandler mHandler = new DtvkitGlueClient.SignalHandler() {
        @Override
        public void onSignal(String signal, JSONObject data) {
            if (signal.equals("DvbsStatusChanged")) {
                int progress = getSearchProcess(data);
                Log.d(TAG, "onSignal progress = " + progress);
                if (mThreadHandler != null) {
                    mThreadHandler.removeMessages(MSG_ON_SIGNAL);
                    Message msg = mThreadHandler.obtainMessage(MSG_ON_SIGNAL, progress, 0, null);
                    boolean info = mThreadHandler.sendMessageDelayed(msg, 0);
                }
            }
        }
    };

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
       @Override
       public void onReceive(Context context, final Intent intent)
       {
          String status = intent.getStringExtra(EpgSyncJobService.SYNC_STATUS);
          if (status.equals(EpgSyncJobService.SYNC_FINISHED))
          {
             setSearchStatus("Finished");
             setStrengthAndQualityStatus("",  "");
             mStartSync = false;
             finish();
          }
       }
    };

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (mStartSync) {
            Toast.makeText(DtvkitDvbsSetup.this, R.string.sync_tv_provider, Toast.LENGTH_SHORT).show();
            return true;
        }
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (mStartSearch) {
                onSearchFinished();
                return true;
            } else {
                stopMonitoringSearch();
                stopSearch(false);
                finish();
                return true;
            }
        }

        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.satsetup);

        initHandler();
        mDataMananer = new DataMananer(this);
        mPvrStatusConfirmManager = new PvrStatusConfirmManager(this, mDataMananer);
        mParameterMananer = new ParameterMananer(this, DtvkitGlueClient.getInstance());
        Intent intent = getIntent();
        if (intent != null) {
            String status = intent.getStringExtra(ConstantManager.KEY_LIVETV_PVR_STATUS);
            mPvrStatusConfirmManager.setPvrStatus(status);
            Log.d(TAG, "onCreate pvr status = " + status);
        }

        final Button search = (Button)findViewById(R.id.startsearch);
        final Button stop = (Button)findViewById(R.id.stopsearch);
        final Button setup = (Button)findViewById(R.id.setup);
        final Button importSatellite = (Button)findViewById(R.id.import_satellite);
        search.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                mPvrStatusConfirmManager.setSearchType(ConstantManager.KEY_DTVKIT_SEARCH_TYPE_MANUAL);
                boolean checkPvr = mPvrStatusConfirmManager.needDeletePvrRecordings();
                if (checkPvr) {
                    mPvrStatusConfirmManager.showDialogToAppoint(DtvkitDvbsSetup.this, false);
                } else {
                    mPvrStatusConfirmManager.sendDvrCommand(DtvkitDvbsSetup.this);
                    search.setEnabled(false);
                    stop.setEnabled(true);
                    stop.requestFocus();
                    startSearch();
                }
            }
        });
        search.requestFocus();

        stop.setEnabled(false);
        stop.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                stop.setEnabled(false);
                search.setEnabled(true);
                search.requestFocus();
                stopSearch(true);
            }
        });

        setup.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                mPvrStatusConfirmManager.setSearchType(ConstantManager.KEY_DTVKIT_SEARCH_TYPE_MANUAL);
                boolean checkPvr = mPvrStatusConfirmManager.needDeletePvrRecordings();
                if (checkPvr) {
                    mPvrStatusConfirmManager.showDialogToAppoint(DtvkitDvbsSetup.this, false);
                } else {
                    mPvrStatusConfirmManager.sendDvrCommand(DtvkitDvbsSetup.this);
                    setUp();
                }
            }
        });

        //ui set visibility gone as will be excuted after first boot
        importSatellite.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                mParameterMananer.importDatabase(ConstantManager.DTVKIT_SATELLITE_DATA);
            }
        });

        CheckBox nit = (CheckBox)findViewById(R.id.network);
        CheckBox clear = (CheckBox)findViewById(R.id.clear_old);
        CheckBox dvbs2 = (CheckBox)findViewById(R.id.dvbs2);
        Spinner searchmode = (Spinner)findViewById(R.id.search_mode);
        Spinner fecmode = (Spinner)findViewById(R.id.fec_mode);
        Spinner modulationmode = (Spinner)findViewById(R.id.modulation_mode);
        final LinearLayout blindContainer = (LinearLayout)findViewById(R.id.blind_frequency_container);
        EditText edit_start_freq = (EditText)findViewById(R.id.edit_start_freq);
        EditText edit_end_freq = (EditText)findViewById(R.id.edit_end_freq);
        edit_start_freq.setText(DataMananer.VALUE_BLIND_DEFAULT_START_FREQUENCY + "");
        edit_end_freq.setText(DataMananer.VALUE_BLIND_DEFAULT_END_FREQUENCY + "");

        nit.setChecked(mDataMananer.getIntParameters(DataMananer.KEY_DVBS_NIT) == 1 ? true : false);
        clear.setChecked(mDataMananer.getIntParameters(DataMananer.KEY_CLEAR) == 1 ? true : false);
        dvbs2.setChecked(mDataMananer.getIntParameters(DataMananer.KEY_DVBS2) == 1 ? true : false);
        nit.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (nit.isChecked()) {
                    mDataMananer.saveIntParameters(DataMananer.KEY_DVBS_NIT, 1);
                } else {
                    mDataMananer.saveIntParameters(DataMananer.KEY_DVBS_NIT, 0);
                }
            }
        });
        clear.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (clear.isChecked()) {
                    mDataMananer.saveIntParameters(DataMananer.KEY_CLEAR, 1);
                } else {
                    mDataMananer.saveIntParameters(DataMananer.KEY_CLEAR, 0);
                }
            }
        });
        dvbs2.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (dvbs2.isChecked()) {
                    mDataMananer.saveIntParameters(DataMananer.KEY_DVBS2, 1);
                } else {
                    mDataMananer.saveIntParameters(DataMananer.KEY_DVBS2, 0);
                }
            }
        });
        searchmode.setOnItemSelectedListener(new OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                Log.d(TAG, "searchmode onItemSelected position = " + position);
                mDataMananer.saveIntParameters(DataMananer.KEY_SEARCH_MODE, position);
                //no need to add limits now
                /*if (position == DataMananer.VALUE_SEARCH_MODE_BLIND) {
                    blindContainer.setVisibility(View.VISIBLE);
                    nit.setVisibility(View.GONE);
                    mDataMananer.saveStringParameters(DataMananer.KEY_TRANSPONDER, "null");
                } else {
                    blindContainer.setVisibility(View.GONE);
                    nit.setVisibility(View.VISIBLE);
                }*/
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // TODO Auto-generated method stub
            }
        });
        fecmode.setOnItemSelectedListener(new OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                Log.d(TAG, "fecmode onItemSelected position = " + position);
                mDataMananer.saveIntParameters(DataMananer.KEY_FEC_MODE, position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // TODO Auto-generated method stub
            }
        });
        modulationmode.setOnItemSelectedListener(new OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                Log.d(TAG, "modulationmode onItemSelected position = " + position);
                mDataMananer.saveIntParameters(DataMananer.KEY_MODULATION_MODE, position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // TODO Auto-generated method stub
            }
        });
        int searchmodeValue = mDataMananer.getIntParameters(DataMananer.KEY_SEARCH_MODE);
        searchmode.setSelection(searchmodeValue);
        //no need to add limits now
        /*if (searchmodeValue == DataMananer.VALUE_SEARCH_MODE_BLIND) {
            blindContainer.setVisibility(View.VISIBLE);
            nit.setVisibility(View.GONE);
        } else {
            blindContainer.setVisibility(View.GONE);
            nit.setVisibility(View.VISIBLE);
        }*/
        fecmode.setSelection(mDataMananer.getIntParameters(DataMananer.KEY_FEC_MODE));
        modulationmode.setSelection(mDataMananer.getIntParameters(DataMananer.KEY_MODULATION_MODE));
        Spinner channelTypeSpinner = (Spinner) findViewById(R.id.channel_type);
        channelTypeSpinner.setSelection(getCurrentChannelType());
        channelTypeSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                updateChannelType(i);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });
        Spinner serviceTypeSpinner = (Spinner) findViewById(R.id.service_type);
        serviceTypeSpinner.setSelection(getCurrentServiceType());
        serviceTypeSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                updateServiceType(i);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });
    }

    private void initHandler() {
        Log.d(TAG, "initHandler");
        mHandlerThread = new HandlerThread("DtvkitDvbtSetup");
        mHandlerThread.start();
        mThreadHandler = new Handler(mHandlerThread.getLooper(), new Handler.Callback() {
            @Override
            public boolean handleMessage(Message msg) {
                Log.d(TAG, "mThreadHandler handleMessage " + msg.what + " start");
                switch (msg.what) {
                    case MSG_FINISH_SEARCH: {
                        onSearchFinished();
                        break;
                    }
                    case MSG_ON_SIGNAL: {
                        dealOnSignal(msg.arg1);
                        break;
                    }
                    default:
                        break;
                }
                Log.d(TAG, "mThreadHandler handleMessage " + msg.what + " over");
                return true;
            }
        });
    }

    private void dealOnSignal(int progress) {
        Log.d(TAG, "onSignal progress = " + progress);
        setSearchProgress(progress);
        int found = getFoundServiceNumber();
        int sstatus = mParameterMananer.getStrengthStatus();
        int qstatus = mParameterMananer.getQualityStatus();
        setSearchStatus(String.format(Locale.ENGLISH, "Searching (%d%%)", progress), String.format(Locale.ENGLISH, "Found %d services", found));
        setStrengthAndQualityStatus(String.format(Locale.ENGLISH, "Strength: %d%%", sstatus), String.format(Locale.ENGLISH, "Quality: %d%%", qstatus));
        if (progress >= 100) {
            sendFinishSearch();
        }
    }

    private void sendFinishSearch() {
        if (mThreadHandler != null) {
            mThreadHandler.removeMessages(MSG_FINISH_SEARCH);
            Message mess = mThreadHandler.obtainMessage(MSG_FINISH_SEARCH, 0, 0, null);
            boolean info = mThreadHandler.sendMessageDelayed(mess, 1000);
        }
    }

    private void setSearchProgress(final int progress) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                final ProgressBar bar = (ProgressBar) findViewById(R.id.searchprogress);
                bar.setProgress(progress);
            }
        });
    }

    private void releaseHandler() {
        Log.d(TAG, "releaseHandler");
        mHandlerThread.getLooper().quitSafely();
        mThreadHandler.removeCallbacksAndMessages(null);
        mHandlerThread = null;
        mThreadHandler = null;
    }

    private int getFilterServiceTypeInSearch() {
        int ret = 0;
        try {
            JSONArray array = new JSONArray();
            array.put(ParameterMananer.SIGNAL_QPSK);
            JSONObject obj = DtvkitGlueClient.getInstance().request("Dvb.GetFilterServiceTypeInSearch", array);
            if (obj != null) {
                ret = obj.optInt("data", 0);
            }
        } catch (Exception e) {
            Log.w(TAG, "Dvb.GetFilterServiceTypeInSearch failed");
        }
        return ret;
    }

    private void SetFilterServiceTypeInSearch(int type) {
        if (type == 0x1f || type == 0x18 || type == 0x07) {
            //filter all, just equals no filter
            type = 0;
        }
        try {
            JSONArray array = new JSONArray();
            array.put(ParameterMananer.SIGNAL_QPSK);
            array.put(type);
            DtvkitGlueClient.getInstance().request("Dvb.SetFilterServiceTypeInSearch", array);
        } catch (Exception e) {
            Log.w(TAG, "Dvb.SetFilterServiceTypeInSearch failed");
        }
    }

    /*
    * Filter Type Free channel: 0x01
    * Filter Type Scamble channel: 0x02
    * Filter Type network :        0x04 //not support in ui now
    * */
    private int getCurrentChannelType() {
        int ret = 0;//match ui type
        int filterType = getFilterServiceTypeInSearch();
        int channelType = filterType >> 3;
        if ((channelType & 0x03) == 0x03 || channelType == 0) {
            ret = 0;
        } else {
            ret = (channelType & 0x03);
        }
        return ret;
    }

    /*param range: 0 - 2*/
    private void updateChannelType(int type) {
        if (type == 0) {
            type = 0x03;
        }
        int filterType = getFilterServiceTypeInSearch();
        if ( type != 7 && filterType == 0) {
            filterType = 0x1f;
        }
        SetFilterServiceTypeInSearch((filterType & 0x07) + (type << 3));
    }

    /*
     * Filter Type service tv: 0x01
     * Filter Type service radio: 0x02
     * Filter Type service other : 0x04 //not support in ui now
     * */
    private int getCurrentServiceType() {
        int ret = 0;
        int filterType = getFilterServiceTypeInSearch();
        int serviceType = filterType & 0x07;
        if ((serviceType & 0x03) == 0x03 || serviceType == 0) {
            ret = 0;
        } else {
            ret = (serviceType & 0x03);
        }
        return ret;
    }

    /*param range: 0 - 2*/
    private void updateServiceType(int type) {
        if (type == 0) {
            type = 0x07;
        }
        int filterType = getFilterServiceTypeInSearch();
        if ( type != 7 && filterType == 0) {
            filterType = 0x1f;
        }
        SetFilterServiceTypeInSearch((filterType & 0x18) + type);
    }

    @Override
    public void finish() {
        //send search info to livetv if found any
        Log.d(TAG, "finish");
        if (mFoundServiceNumber > 0) {
            Intent intent = new Intent();
            intent.putExtra(DtvkitDvbScanSelect.SEARCH_TYPE_MANUAL_AUTO, mSearchType);
            intent.putExtra(DtvkitDvbScanSelect.SEARCH_TYPE_DVBS_DVBT_DVBC, mSearchDvbsType);
            intent.putExtra(DtvkitDvbScanSelect.SEARCH_FOUND_SERVICE_NUMBER, mFoundServiceNumber);
            intent.putExtra(DtvkitDvbScanSelect.SEARCH_FOUND_SERVICE_NUMBER, mFoundServiceNumber);
            String serviceListJsonArray = (mServiceList != null && mServiceList.length() > 0) ? mServiceList.toString() : "";
            String firstServiceName = "";
            try {
                if (mServiceList != null && mServiceList.length() > 0) {
                    firstServiceName = mServiceList.getJSONObject(0).getString("name");
                    for (int i = 0;i < mServiceList.length();i++) {
                        if (mServiceList.getJSONObject(i).getBoolean("hidden") == false) {
                            firstServiceName = mServiceList.getJSONObject(i).getString("name");
                            break;
                        }
                    }
                }
            } catch (JSONException e) {
                Log.e(TAG, "finish JSONException = " + e.getMessage());
            }
            intent.putExtra(DtvkitDvbScanSelect.SEARCH_FOUND_SERVICE_LIST, serviceListJsonArray);
            intent.putExtra(DtvkitDvbScanSelect.SEARCH_FOUND_FIRST_SERVICE, firstServiceName);
            Log.d(TAG, "finish firstServiceName = " + firstServiceName);
            setResult(RESULT_OK, intent);
        } else {
            setResult(RESULT_CANCELED);
        }
        super.finish();
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");
        mSetUp = false;
        mPvrStatusConfirmManager.registerCommandReceiver();
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d(TAG, "onPause");
        mPvrStatusConfirmManager.unRegisterCommandReceiver();
    }

    @Override
    public void onStop() {
        super.onStop();
        Log.d(TAG, "onStop");
        if (mSetUp) {//set up menu and no need to update
            return;
        }
        if (mStartSearch) {
            onSearchFinished();
        } else {
            stopMonitoringSearch();
            stopSearch(false);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");
        releaseHandler();
        stopMonitoringSearch();
        stopMonitoringSync();
    }

    private void setUp() {
        try {
            Intent intent = new Intent();
            intent.setClassName("org.dtvkit.inputsource", "com.droidlogic.fragment.ScanMainActivity");
            String inputId = this.getIntent().getStringExtra(TvInputInfo.EXTRA_INPUT_ID);
            if (inputId != null) {
                intent.putExtra(TvInputInfo.EXTRA_INPUT_ID, inputId);
            }
            startActivityForResult(intent, REQUEST_CODE_SET_UP_SETTINGS);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, getString(R.string.strSetUpNotFound), Toast.LENGTH_SHORT).show();
            return;
        }
        mSetUp = true;
    }

    private JSONArray initSearchParameter() {
        JSONArray args = new JSONArray();
        /*[scanmode, network, {lnblist: [{lnb:1},{lnb:2},..]}]*/
        String searchmode = DataMananer.KEY_SEARCH_MODE_LIST[mDataMananer.getIntParameters(DataMananer.KEY_SEARCH_MODE)];
        args.put(searchmode);//arg1
        args.put(mDataMananer.getIntParameters(DataMananer.KEY_DVBS_NIT) == 1 ? true : false);//arg2
        List<String> lnbList = DvbsParameterManager.getInstance(DtvkitDvbsSetup.this).getLnbWrap().getLnbIdList();
        JSONObject lnbArgs = new JSONObject();
        JSONArray lnbArgs_array = new JSONArray();
        try {
            for (String id : lnbList) {
                JSONObject obj = new JSONObject();
                if (DvbsParameterManager.getInstance(DtvkitDvbsSetup.this)
                        .getSatelliteNameListSelected(id).size() > 0) {
                    obj.put("lnb", id);
                    lnbArgs_array.put(obj);
                }
            }
            if (lnbArgs_array.length() > 0) {
                lnbArgs.put("lnblist", lnbArgs_array);
            } else {
                return null;
            }
        } catch (Exception e) {
            return null;
        }
        args.put(lnbArgs.toString());//arg3

        return args;
    }

    private int[] getBlindFrequency() {
        int[] result = {-1, -1};
        EditText edit_start_freq = (EditText)findViewById(R.id.edit_start_freq);
        EditText edit_end_freq = (EditText)findViewById(R.id.edit_end_freq);
        Editable edit_start_freq_edit = edit_start_freq.getText();
        Editable edit_end_freq_edit = edit_end_freq.getText();
        if (edit_start_freq_edit != null && edit_end_freq_edit != null) {
            String edit_start_freq_value = edit_start_freq_edit.toString();
            String edit_end_freq_value = edit_end_freq_edit.toString();
            Log.d(TAG, "getBlindFrequency edit_start_freq_value = " + edit_start_freq_value +
                ", edit_end_freq_value = " + edit_end_freq_value);
            if (!TextUtils.isEmpty(edit_start_freq_value) && TextUtils.isDigitsOnly(edit_start_freq_value)) {
                result[0] = Integer.valueOf(edit_start_freq_value);
            }
            if (!TextUtils.isEmpty(edit_end_freq_value) && TextUtils.isDigitsOnly(edit_end_freq_value)) {
                result[1] = Integer.valueOf(edit_end_freq_value);
            }
        }
        return result;
    }

     private void testAddSatallite() {
        try {
            JSONArray args = new JSONArray();
            args.put("test1");
            args.put(true);
            args.put(1200);
            Log.d(TAG, "addSatallite->" + args.toString());
            DtvkitGlueClient.getInstance().request("Dvbs.addSatellite", args);
            JSONArray args1 = new JSONArray();
            JSONObject resultObj = DtvkitGlueClient.getInstance().request("Dvbs.getSatellites", args1);
            if (resultObj != null) {
                Log.d(TAG, "addSatallite resultObj:" + resultObj.toString());
            } else {
                Log.d(TAG, "addSatallite then get null");
            }
        } catch (Exception e) {
            Log.d(TAG, "addSatallite Exception " + e.getMessage() + ", trace=" + e.getStackTrace());
            e.printStackTrace();
        }
    }

    private void testAddTransponder() {
        try {
            JSONArray args = new JSONArray();
            args.put("test1");
            args.put(498);
            args.put("H");
            args.put(6875);
            Log.d(TAG, "addTransponder->" + args.toString());
            DtvkitGlueClient.getInstance().request("Dvbs.addTransponder", args);
            JSONArray args1 = new JSONArray();
            JSONObject resultObj = DtvkitGlueClient.getInstance().request("Dvbs.getSatellites", args1);
            if (resultObj != null) {
                Log.d(TAG, "addTransponder resultObj:" + resultObj.toString());
            } else {
                Log.d(TAG, "addTransponder then get null");
            }
        } catch (Exception e) {
            Log.d(TAG, "addTransponder Exception " + e.getMessage() + ", trace=" + e.getStackTrace());
            e.printStackTrace();
        }
    }

    private void startSearch() {
        setSearchStatus("Searching", "");
        setStrengthAndQualityStatus("Strength:",  "Quality:");
        getProgressBar().setIndeterminate(false);
        startMonitoringSearch();

        /*CheckBox network = (CheckBox)findViewById(R.id.network);
        EditText frequency = (EditText)findViewById(R.id.frequency);
        Spinner satellite = (Spinner)findViewById(R.id.satellite);
        Spinner polarity = (Spinner)findViewById(R.id.polarity);
        EditText symbolrate = (EditText)findViewById(R.id.symbolrate);
        Spinner fec = (Spinner)findViewById(R.id.fec);
        CheckBox dvbs2 = (CheckBox)findViewById(R.id.dvbs2);
        Spinner modulation = (Spinner)findViewById(R.id.modulation);*/

        try {
            /*JSONArray args = new JSONArray();
            args.put(network.isChecked());
            args.put(Integer.parseInt(frequency.getText().toString()));
            args.put(satellite.getSelectedItem().toString());
            args.put(polarity.getSelectedItem().toString());
            args.put(Integer.parseInt(symbolrate.getText().toString()));
            args.put(fec.getSelectedItem().toString());
            args.put(dvbs2.isChecked());
            args.put(modulation.getSelectedItem().toString());

            Log.i(TAG, args.toString());

            DtvkitGlueClient.getInstance().request("Dvbs.startManualSearch", args);*/
            JSONArray args = initSearchParameter();
            if (args != null) {
                Log.i(TAG, "search parameter:" + args.toString());
                //DtvkitGlueClient.getInstance().request("Dvbs.startSearch", args);
                //prevent ui not fresh on time
                doSearchByThread(args);
            } else {
                stopMonitoringSearch();
                setSearchStatus(getString(R.string.invalid_parameter), "");
                setStrengthAndQualityStatus("",  "");
                enableSearchButton(true);
                stopSearch(true);
                return;
            }
        } catch (Exception e) {
            stopMonitoringSearch();
            setSearchStatus(e.getMessage(), "");
            setStrengthAndQualityStatus("",  "");
            stopSearch(true);
        }
    }

    private void stopSearch(boolean sync) {
        mStartSearch = false;
        enableSearchButton(true);
        enableStopButton(false);
        setSearchStatus("Finishing search");
        setStrengthAndQualityStatus("",  "");
        getProgressBar().setIndeterminate(sync);
        stopMonitoringSearch();
        try {
            JSONArray args = new JSONArray();
            args.put(true); // Commit
            DtvkitGlueClient.getInstance().request("Dvbs.finishSearch", args);
        } catch (Exception e) {
            setSearchStatus("Failed to finish search:" + e.getMessage());
            return;
        }
        //setSearchStatus(getString(R.string.strSearchNotStarted));
        if (!sync) {
            return;
        }
        setSearchStatus("Updating guide");
        startMonitoringSync();
        // By default, gets all channels and 1 hour of programs (DEFAULT_IMMEDIATE_EPG_DURATION_MILLIS)
        EpgSyncJobService.cancelAllSyncRequests(this);
        String inputId = this.getIntent().getStringExtra(TvInputInfo.EXTRA_INPUT_ID);
        Log.i(TAG, String.format("inputId: %s", inputId));
        Bundle parameters = new Bundle();
        parameters.putString(EpgSyncJobService.BUNDLE_KEY_SYNC_SEARCHED_MODE, EpgSyncJobService.BUNDLE_VALUE_SYNC_SEARCHED_MODE_MANUAL);
        parameters.putString(EpgSyncJobService.BUNDLE_KEY_SYNC_SEARCHED_SIGNAL_TYPE, "DVB-S");
        EpgSyncJobService.requestImmediateSyncSearchedChannelWitchParameters(this, inputId, (mFoundServiceNumber > 0),new ComponentName(this, DtvkitEpgSync.class), parameters);
    }

    private void doSearchByThread(final JSONArray args) {
        new Thread(new Runnable() {
            public void run() {
                try {
                    mSearchDvbsType = DtvkitDvbScanSelect.SEARCH_TYPE_DVBS;
                    DtvkitGlueClient.getInstance().request("Dvbs.startSearchEx", args);
                    mStartSearch = true;
                    mFoundServiceNumber = 0;
                    mServiceList = null;
                    mParameterMananer.saveChannelIdForSource(-1);
                } catch (Exception e) {
                    doStopByUiThread(e);
                }
                enableSearchButton(true);
            }
        }).start();
    }

    private void doStopByUiThread(final Exception e) {
        runOnUiThread(new Runnable() {
            public void run() {
                stopMonitoringSearch();
                setSearchStatus(e.getMessage());
                setStrengthAndQualityStatus("",  "");
            }
        });
    }

    private void enableSetupButton(boolean enable) {
        runOnUiThread(new Runnable() {
            public void run() {
                Button setup = (Button)findViewById(R.id.setup);
                setup.setEnabled(enable);
            }
        });
    }

    private void enableSearchButton(boolean enable) {
        runOnUiThread(new Runnable() {
            public void run() {
                Button search = (Button)findViewById(R.id.startsearch);
                search.setEnabled(enable);
            }
        });
    }

    private void enableStopButton(boolean enable) {
        runOnUiThread(new Runnable() {
            public void run() {
                Button stop = (Button)findViewById(R.id.stopsearch);
                stop.setEnabled(enable);
            }
        });
    }

    private void onSearchFinished() {
        mStartSearch = false;
        enableSearchButton(false);
        enableStopButton(false);
        enableSetupButton(false);
        setSearchStatus("Finishing search");
        setStrengthAndQualityStatus("",  "");
        getProgressBar().setIndeterminate(true);
        stopMonitoringSearch();
        try {
            JSONArray args = new JSONArray();
            args.put(true); // Commit
            DtvkitGlueClient.getInstance().request("Dvbs.finishSearch", args);
        } catch (Exception e) {
            stopMonitoringSearch();
            setSearchStatus(e.getMessage());
            return;
        }
        //update search results as After the search is finished, the lcn will be reordered
        mServiceList = getServiceList();
        mFoundServiceNumber = getFoundServiceNumber();
        if (mFoundServiceNumber == 0 && mServiceList != null && mServiceList.length() > 0) {
            Log.d(TAG, "mFoundServiceNumber erro use mServiceList length = " + mServiceList.length());
            mFoundServiceNumber = mServiceList.length();
        }

        setSearchStatus("Updating guide");
        startMonitoringSync();
        // By default, gets all channels and 1 hour of programs (DEFAULT_IMMEDIATE_EPG_DURATION_MILLIS)
        EpgSyncJobService.cancelAllSyncRequests(this);
        String inputId = this.getIntent().getStringExtra(TvInputInfo.EXTRA_INPUT_ID);
        Log.i(TAG, String.format("inputId: %s", inputId));
        //EpgSyncJobService.requestImmediateSync(this, inputId, true, new ComponentName(this, DtvkitEpgSync.class)); // 12 hours
        Bundle parameters = new Bundle();
        parameters.putString(EpgSyncJobService.BUNDLE_KEY_SYNC_SEARCHED_MODE, EpgSyncJobService.BUNDLE_VALUE_SYNC_SEARCHED_MODE_MANUAL);
        parameters.putString(EpgSyncJobService.BUNDLE_KEY_SYNC_SEARCHED_SIGNAL_TYPE, "DVB-S");
        EpgSyncJobService.requestImmediateSyncSearchedChannelWitchParameters(this, inputId, (mFoundServiceNumber > 0),new ComponentName(this, DtvkitEpgSync.class), parameters);
    }

    private void startMonitoringSearch() {
        DtvkitGlueClient.getInstance().registerSignalHandler(mHandler);
    }

    private void stopMonitoringSearch() {
        DtvkitGlueClient.getInstance().unregisterSignalHandler(mHandler);
    }

    private void startMonitoringSync() {
        mStartSync = true;
        LocalBroadcastManager.getInstance(this).registerReceiver(mReceiver,
                new IntentFilter(EpgSyncJobService.ACTION_SYNC_STATUS_CHANGED));
    }

    private void stopMonitoringSync() {
        mStartSync = false;
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mReceiver);
    }

    private void setSearchStatus(final String status) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Log.i(TAG, String.format("Search status \"%s\"", status));
                final TextView text = (TextView) findViewById(R.id.searchstatus);
                text.setText(status);
            }
        });
    }

    private void setSearchStatus(final String status, final String description) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Log.i(TAG, String.format("Search status \"%s\"", status));
                final TextView text = (TextView) findViewById(R.id.searchstatus);
                text.setText(status);

                final TextView text2 = (TextView) findViewById(R.id.description);
                text2.setText(description);
            }
        });
    }

    private ProgressBar getProgressBar() {
        return (ProgressBar) findViewById(R.id.searchprogress);
    }

    private int getFoundServiceNumber() {
        int found = 0;
        try {
            JSONObject obj = DtvkitGlueClient.getInstance().request("Dvb.getNumberOfServices", new JSONArray());
            found = obj.getInt("data");
            Log.i(TAG, "getFoundServiceNumber found = " + found);
        } catch (Exception ignore) {
            Log.e(TAG, "getFoundServiceNumber Exception = " + ignore.getMessage());
        }
        return found;
    }

    private int getSearchProcess(JSONObject data) {
        int progress = 0;
        if (data == null) {
            return progress;
        }
        try {
            progress = data.getInt("progress");
        } catch (JSONException ignore) {
            Log.e(TAG, "getSearchProcess Exception = " + ignore.getMessage());
        }
        return progress;
    }

    private JSONArray getServiceList() {
        JSONArray result = null;
        try {
            JSONObject obj = DtvkitGlueClient.getInstance().request("Dvb.getListOfServices", new JSONArray());
            JSONArray services = obj.getJSONArray("data");
            result = services;
            for (int i = 0; i < services.length(); i++) {
                JSONObject service = services.getJSONObject(i);
                //Log.i(TAG, "getServiceList service = " + service.toString());
            }

        } catch (Exception e) {
            Log.e(TAG, "getServiceList Exception = " + e.getMessage());
        }
        return result;
    }

    private void setStrengthAndQualityStatus(final String sstatus, final String qstatus) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Log.i(TAG, String.format("Strength: %s", sstatus));
                final TextView strengthText = (TextView) findViewById(R.id.strengthstatus);
                strengthText.setText(sstatus);

                Log.i(TAG, String.format("Quality: %s", qstatus));
                final TextView qualityText = (TextView) findViewById(R.id.qualitystatus);
                qualityText.setText(qstatus);
            }
        });
    }
}

