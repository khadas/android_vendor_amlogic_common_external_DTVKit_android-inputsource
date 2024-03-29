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
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.text.Editable;
import android.text.TextUtils;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.view.KeyEvent;
import android.widget.Toast;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.view.WindowManager;
import android.util.TypedValue;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;

import org.dtvkit.companionlibrary.EpgSyncJobService;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Locale;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

import com.droidlogic.fragment.ParameterMananer;
import com.droidlogic.settings.ConstantManager;
import org.droidlogic.dtvkit.DtvkitGlueClient;

public class DtvkitDvbtSetup extends Activity {
    private static final String TAG = "DtvkitDvbtSetup";

    private boolean mIsDvbt = false;
    private DataMananer mDataMananer;
    private ParameterMananer mParameterMananer = null;
    private boolean mStartSync = false;
    private boolean mStartSearch = false;
    private boolean mFinish = false;
    private JSONArray mServiceList = null;
    private int mFoundServiceNumber = 0;
    private int mSearchManualAutoType = -1;// 0 manual 1 auto
    private int mSearchDvbcDvbtType = -1;
    private PvrStatusConfirmManager mPvrStatusConfirmManager = null;

    private AutoNumberEditText mDvbcNitAutoEdit = null;
    private AutoNumberEditText mDvbcSymAutoEdit = null;
    private AutoNumberEditText mDvbcFreqAutoEdit = null;
    private String[] DVBC_AUTO_SCANTYPE = {"network", "quick", "full", "blind"};

    protected HandlerThread mHandlerThread = null;
    protected Handler mThreadHandler = null;

    private final static int MSG_START_SEARCH = 1;
    private final static int MSG_STOP_SEARCH = 2;
    private final static int MSG_FINISH_SEARCH = 3;
    private final static int MSG_ON_SIGNAL = 4;
    private final static int MSG_FINISH = 5;
    private final static int MSG_RELEASE= 6;

    private final DtvkitGlueClient.SignalHandler mHandler = new DtvkitGlueClient.SignalHandler() {
        @Override
        public void onSignal(String signal, JSONObject data) {
            Map<String, Object> map = new HashMap<String,Object>();
            map.put("signal", signal);
            map.put("data", data);
            sendOnSignal(map);
        }
    };

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, final Intent intent) {
            String status = intent.getStringExtra(EpgSyncJobService.SYNC_STATUS);
            if (status.equals(EpgSyncJobService.SYNC_FINISHED)) {
                setSearchStatus("Finished", "");
                mStartSync = false;
                //finish();
                sendFinish();
            }
        }
    };

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (mStartSync) {
            Toast.makeText(DtvkitDvbtSetup.this, R.string.sync_tv_provider, Toast.LENGTH_SHORT).show();
            return true;
        }
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (mStartSearch) {
                //onSearchFinished();
                sendFinishSearch(false);
                return true;
            } else {
                stopMonitoringSearch();
                //stopSearch();
                sendStopSearch();
                //finish();
                sendFinish();
                return true;
            }
        }

        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.autosetup);
        mParameterMananer = new ParameterMananer(this, DtvkitGlueClient.getInstance());
        final View startSearch = findViewById(R.id.terrestrialstartsearch);
        final View stopSearch = findViewById(R.id.terrestrialstopsearch);

        startSearch.setEnabled(true);
        startSearch.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                int searchmode = mDataMananer.getIntParameters(DataMananer.KEY_PUBLIC_SEARCH_MODE);
                boolean autoSearch = (DataMananer.VALUE_PUBLIC_SEARCH_MODE_AUTO == searchmode);
                mPvrStatusConfirmManager.setSearchType(autoSearch ? ConstantManager.KEY_DTVKIT_SEARCH_TYPE_AUTO : ConstantManager.KEY_DTVKIT_SEARCH_TYPE_MANUAL);
                boolean checkPvr = mPvrStatusConfirmManager.needDeletePvrRecordings();
                if (checkPvr) {
                    mPvrStatusConfirmManager.showDialogToAppoint(DtvkitDvbtSetup.this, autoSearch);
                } else {
                    mPvrStatusConfirmManager.sendDvrCommand(DtvkitDvbtSetup.this);
                    startSearch.setEnabled(false);
                    stopSearch.setEnabled(true);
                    stopSearch.requestFocus();
                    //startSearch();
                    sendStartSearch();
                }
            }
        });
        startSearch.requestFocus();
        mDataMananer = new DataMananer(this);
        mPvrStatusConfirmManager = new PvrStatusConfirmManager(this, mDataMananer);
        Intent intent = getIntent();
        if (intent != null) {
            mIsDvbt = intent.getBooleanExtra(DataMananer.KEY_IS_DVBT, false);
            String status = intent.getStringExtra(ConstantManager.KEY_LIVETV_PVR_STATUS);
            mPvrStatusConfirmManager.setPvrStatus(status);
            Log.d(TAG, "onCreate mIsDvbt = " + mIsDvbt + ", status = " + status);
        }
        ((TextView)findViewById(R.id.description)).setText(mIsDvbt ? R.string.strSearchDvbtDescription : R.string.strSearchDvbcDescription);

        stopSearch.setEnabled(false);
        stopSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //onSearchFinished();
                sendFinishSearch(false);
            }
        });

        initOrUpdateView(true);
        initHandler();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");
        mPvrStatusConfirmManager.registerCommandReceiver();
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause");
        mPvrStatusConfirmManager.unRegisterCommandReceiver();
    }

    @Override
    public void finish() {
        //send search info to livetv if found any
        Log.d(TAG, "finish");
        if (mFoundServiceNumber > 0) {
            Intent intent = new Intent();
            intent.putExtra(DtvkitDvbScanSelect.SEARCH_TYPE_MANUAL_AUTO, mSearchManualAutoType);
            intent.putExtra(DtvkitDvbScanSelect.SEARCH_TYPE_DVBS_DVBT_DVBC, mSearchDvbcDvbtType);
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
    public void onStop() {
        super.onStop();
        Log.d(TAG, "onStop");
        if (mStartSearch) {
            //onSearchFinished();
            sendFinishSearch(true);
        } else if (!mFinish) {
            stopMonitoringSearch();
            //stopSearch();
            sendStopSearch();
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

    private void initHandler() {
        Log.d(TAG, "initHandler");
        mHandlerThread = new HandlerThread("DtvkitDvbtSetup");
        mHandlerThread.start();
        mThreadHandler = new Handler(mHandlerThread.getLooper(), new Handler.Callback() {
            @Override
            public boolean handleMessage(Message msg) {
                Log.d(TAG, "mThreadHandler handleMessage " + msg.what + " start");
                switch (msg.what) {
                    case MSG_START_SEARCH: {
                        startSearch();
                        break;
                    }
                    case MSG_STOP_SEARCH: {
                        stopSearch();
                        break;
                    }
                    case MSG_FINISH_SEARCH: {
                        prepareSearchFinished(msg.arg1 == 1);
                        break;
                    }
                    case MSG_ON_SIGNAL: {
                        dealOnSignal((Map<String, Object>)msg.obj);
                        break;
                    }
                    case MSG_FINISH: {
                        finish();
                        break;
                    }
                    case MSG_RELEASE: {
                        releaseInThread();
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

    private void sendRelease() {
        if (mThreadHandler != null) {
            mThreadHandler.removeMessages(MSG_RELEASE);
            Message mess = mThreadHandler.obtainMessage(MSG_RELEASE, 0, 0, null);
            boolean info = mThreadHandler.sendMessageDelayed(mess, 0);
            Log.d(TAG, "sendMessage MSG_RELEASE " + info);
        }
    }

    private void releaseInThread() {
        Log.d(TAG, "releaseInThread");
        new Thread(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "releaseInThread start");
                releaseHandler();
                Log.d(TAG, "releaseInThread end");
            }
        }).start();
    }

    private void releaseHandler() {
        Log.d(TAG, "releaseHandler");
        mHandlerThread.getLooper().quitSafely();
        mThreadHandler.removeCallbacksAndMessages(null);
        mHandlerThread = null;
        mThreadHandler = null;
    }

    private void initOrUpdateView(boolean init) {
        LinearLayout public_typein_containner = (LinearLayout)findViewById(R.id.public_typein_containner);
        TextView public_type_in = (TextView)findViewById(R.id.public_typein_text);
        EditText public_type_edit = (EditText)findViewById(R.id.public_typein_edit);
        LinearLayout dvbt_bandwidth_containner = (LinearLayout)findViewById(R.id.dvbt_bandwidth_containner);
        Spinner dvbt_bandwidth_spinner = (Spinner)findViewById(R.id.dvbt_bandwidth_spinner);
        LinearLayout dvbt_mode_containner = (LinearLayout)findViewById(R.id.dvbt_mode_containner);
        Spinner dvbt_mode_spinner = (Spinner)findViewById(R.id.dvbt_mode_spinner);
        LinearLayout dvbt_type_containner = (LinearLayout)findViewById(R.id.dvbt_type_containner);
        Spinner dvbt_type_spinner = (Spinner)findViewById(R.id.dvbt_type_spinner);
        LinearLayout dvbc_mode_containner = (LinearLayout)findViewById(R.id.dvbc_mode_containner);
        Spinner dvbc_mode_spinner = (Spinner)findViewById(R.id.dvbc_mode_spinner);
        LinearLayout dvbc_symbol_containner = (LinearLayout)findViewById(R.id.dvbc_symbol_containner);
        EditText dvbc_symbol_edit = (EditText)findViewById(R.id.dvbc_symbol_edit);
        LinearLayout public_search_mode_containner = (LinearLayout)findViewById(R.id.public_search_mode_containner);
        Spinner public_search_mode_spinner = (Spinner)findViewById(R.id.public_search_mode_spinner);
        LinearLayout frequency_channel_container = (LinearLayout)findViewById(R.id.frequency_channel_container);
        Spinner frequency_channel_spinner = (Spinner)findViewById(R.id.frequency_channel_spinner);
        LinearLayout public_search_channel_name_containner = (LinearLayout)findViewById(R.id.public_search_channel_containner);
        Spinner public_search_channel_name_spinner = (Spinner)findViewById(R.id.public_search_channel_spinner);
        Button search = (Button)findViewById(R.id.terrestrialstartsearch);
        CheckBox nit = (CheckBox)findViewById(R.id.network);
        CheckBox checkBoxLcn = (CheckBox)findViewById(R.id.lcnonoff);
        LinearLayout dvbc_operator_containner = (LinearLayout)findViewById(R.id.dvbc_operator_containner);
        LinearLayout dvbc_autoscantype_containner = (LinearLayout)findViewById(R.id.dvbc_autoscantype_containner);
        LinearLayout dvbc_networkid_containner = (LinearLayout)findViewById(R.id.dvbc_newtworkid_containner);
        LinearLayout dvbc_frequency_containner = (LinearLayout) findViewById(R.id.dvbc_freqency_containner);
        Spinner operator_spinner = (Spinner) findViewById(R.id.dvbc_operator_spinner);
        Spinner dvbc_autoscantype_spinner = (Spinner) findViewById(R.id.dvbc_autoscantype_spinner);
        EditText dvbc_networkid_editText = (EditText) findViewById(R.id.dvbc_networkid_edit);
        EditText dvbc_frequency_editText = (EditText) findViewById(R.id.dvbc_freqency_edit);

        int isFrequencyMode = mDataMananer.getIntParameters(DataMananer.KEY_IS_FREQUENCY);
        if (isFrequencyMode == DataMananer.VALUE_FREQUENCY_MODE) {
            public_type_in.setText(R.string.search_frequency);
            public_type_edit.setHint(R.string.search_frequency_hint);
            public_typein_containner.setVisibility(View.VISIBLE);
            public_search_channel_name_containner.setVisibility(View.GONE);
        } else {
            //public_type_in.setText(R.string.search_number);
            //public_type_edit.setHint(R.string.search_number_hint);//not needed
            public_typein_containner.setVisibility(View.GONE);
            public_search_channel_name_containner.setVisibility(View.VISIBLE);
            updateChannelNameContainer();
        }
        public_type_edit.setText("");
        int value = mDataMananer.getIntParameters(DataMananer.KEY_PUBLIC_SEARCH_MODE);
        public_search_mode_spinner.setSelection(value);
        nit.setVisibility(View.GONE);//dvbt,dvbc not use this any more
        if (DataMananer.VALUE_PUBLIC_SEARCH_MODE_AUTO == value) {
            if (mIsDvbt) {
                dvbc_mode_containner.setVisibility(View.GONE);
                dvbc_symbol_containner.setVisibility(View.GONE);
                frequency_channel_container.setVisibility(View.GONE);
                public_search_channel_name_containner.setVisibility(View.GONE);
            } else {
                //dvbc_operator_containner.setVisibility(View.VISIBLE);
                dvbc_autoscantype_containner.setVisibility(View.VISIBLE);
                int autoScanTypePos = dvbc_autoscantype_spinner.getSelectedItemPosition();
                int visibility = View.GONE;
                if (autoScanTypePos < 2) {
                    visibility = View.VISIBLE;
                }
                dvbc_mode_containner.setVisibility(visibility);
                dvbc_mode_spinner.setSelection(getResources().getStringArray(R.array.dvbc_mode_entries).length - 1);
                dvbc_networkid_containner.setVisibility(visibility);
                dvbc_symbol_containner.setVisibility(visibility);
                dvbc_frequency_containner.setVisibility(visibility);
            }
            checkBoxLcn.setVisibility(View.VISIBLE);
            checkBoxLcn.setChecked(mParameterMananer.getAutomaticOrderingEnabled());
            dvbt_bandwidth_containner.setVisibility(View.GONE);
            dvbt_mode_containner.setVisibility(View.GONE);
            dvbt_type_containner.setVisibility(View.GONE);
            dvbc_operator_containner.setVisibility(View.GONE);
            frequency_channel_container.setVisibility(View.GONE);
            public_search_channel_name_containner.setVisibility(View.GONE);
            public_typein_containner.setVisibility(View.GONE);
            search.setText(R.string.strAutoSearch);
        } else {
            search.setText(R.string.strManualSearch);
            if (isFrequencyMode == DataMananer.VALUE_FREQUENCY_MODE) {
                public_typein_containner.setVisibility(View.VISIBLE);
                public_search_channel_name_containner.setVisibility(View.GONE);
            } else {
                public_typein_containner.setVisibility(View.GONE);
                public_search_channel_name_containner.setVisibility(View.VISIBLE);
            }
            frequency_channel_container.setVisibility(View.VISIBLE);
            frequency_channel_spinner.setSelection(mDataMananer.getIntParameters(DataMananer.KEY_IS_FREQUENCY));
            if (mIsDvbt) {
                dvbt_bandwidth_containner.setVisibility(View.VISIBLE);
                dvbt_mode_containner.setVisibility(View.VISIBLE);
                dvbt_type_containner.setVisibility(View.VISIBLE);
                dvbc_symbol_containner.setVisibility(View.GONE);
                dvbc_mode_containner.setVisibility(View.GONE);
                dvbt_bandwidth_spinner.setSelection(mDataMananer.getIntParameters(DataMananer.KEY_DVBT_BANDWIDTH));
                dvbt_mode_spinner.setSelection(mDataMananer.getIntParameters(DataMananer.KEY_DVBT_MODE));
                dvbt_type_spinner.setSelection(mDataMananer.getIntParameters(DataMananer.KEY_DVBT_TYPE));
            } else {
                dvbt_bandwidth_containner.setVisibility(View.GONE);
                dvbt_mode_containner.setVisibility(View.GONE);
                dvbt_type_containner.setVisibility(View.GONE);
                dvbc_symbol_containner.setVisibility(View.VISIBLE);
                dvbc_mode_containner.setVisibility(View.VISIBLE);
                dvbc_mode_spinner.setSelection(mDataMananer.getIntParameters(DataMananer.KEY_DVBC_MODE));
                //dvbc_symbol_edit.setText(mDataMananer.getIntParameters(DataMananer.KEY_DVBC_SYMBOL_RATE) + "");
            }
            checkBoxLcn.setVisibility(View.GONE);
            dvbc_operator_containner.setVisibility(View.GONE);
            dvbc_autoscantype_containner.setVisibility(View.GONE);
            dvbc_networkid_containner.setVisibility(View.GONE);
            dvbc_frequency_containner.setVisibility(View.GONE);
        }
        if (init) {//init one time
            dvbt_bandwidth_spinner.setOnItemSelectedListener(new OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    Log.d(TAG, "dvbt_bandwidth_spinner onItemSelected position = " + position);
                    mDataMananer.saveIntParameters(DataMananer.KEY_DVBT_BANDWIDTH, position);
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                    // TODO Auto-generated method stub
                }
            });
            dvbt_mode_spinner.setOnItemSelectedListener(new OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    Log.d(TAG, "dvbt_mode_spinner onItemSelected position = " + position);
                    mDataMananer.saveIntParameters(DataMananer.KEY_DVBT_MODE, position);
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                    // TODO Auto-generated method stub
                }
            });
            dvbt_type_spinner.setOnItemSelectedListener(new OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    if (mDataMananer.getIntParameters(DataMananer.KEY_DVBT_TYPE) == position) {
                        Log.d(TAG, "dvbt_type_spinner same position = " + position);
                        return;
                    }
                    Log.d(TAG, "dvbt_type_spinner onItemSelected position = " + position);
                    mDataMananer.saveIntParameters(DataMananer.KEY_DVBT_TYPE, position);
                    initOrUpdateView(false);
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                    // TODO Auto-generated method stub
                }
            });
            dvbc_mode_spinner.setOnItemSelectedListener(new OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    Log.d(TAG, "dvbc_mode_spinner onItemSelected position = " + position);
                    mDataMananer.saveIntParameters(DataMananer.KEY_DVBC_MODE, position);
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                    // TODO Auto-generated method stub
                }
            });
            public_search_mode_spinner.setOnItemSelectedListener(new OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    if (position == mDataMananer.getIntParameters(DataMananer.KEY_PUBLIC_SEARCH_MODE)) {
                        Log.d(TAG, "public_search_mode_spinner select same position = " + position);
                        return;
                    }
                    Log.d(TAG, "public_search_mode_spinner onItemSelected position = " + position);
                    mDataMananer.saveIntParameters(DataMananer.KEY_PUBLIC_SEARCH_MODE, position);
                    initOrUpdateView(false);
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                    // TODO Auto-generated method stub
                }
            });
            frequency_channel_spinner.setOnItemSelectedListener(new OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    if (position == mDataMananer.getIntParameters(DataMananer.KEY_IS_FREQUENCY)) {
                        Log.d(TAG, "frequency_channel_container select same position = " + position);
                        return;
                    }
                    Log.d(TAG, "frequency_channel_container onItemSelected position = " + position);
                    mDataMananer.saveIntParameters(DataMananer.KEY_IS_FREQUENCY, position);
                    initOrUpdateView(false);
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                    // TODO Auto-generated method stub
                }
            });
            public_search_channel_name_spinner.setOnItemSelectedListener(new OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    Log.d(TAG, "public_search_channel_name_spinner onItemSelected position = " + position);
                    if (mIsDvbt) {
                        mDataMananer.saveIntParameters(DataMananer.KEY_SEARCH_DVBT_CHANNEL_NAME, position);
                    } else {
                        mDataMananer.saveIntParameters(DataMananer.KEY_SEARCH_DVBC_CHANNEL_NAME, position);
                    }
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                    // TODO Auto-generated method stub
                }
            });
            checkBoxLcn.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    if (checkBoxLcn.isChecked()) {
                        mParameterMananer.setAutomaticOrderingEnabled(true);
                    } else {
                        mParameterMananer.setAutomaticOrderingEnabled(false);
                    }
                }
            });
            if (!mIsDvbt) {
                dvbc_autoscantype_spinner.setOnItemSelectedListener(new OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                        initOrUpdateView(false);
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> adapterView) {
                    }
                });
                try {
                    InvalidEditInputListener listener = new InvalidEditInputListener() {
                        @Override
                        public void onInputInvalid() {
                            Toast.makeText(DtvkitDvbtSetup.this, "Invalid input numbers!", Toast.LENGTH_SHORT).show();
                        }
                    };
                    mDvbcNitAutoEdit = new AutoNumberEditText(dvbc_networkid_editText,
                            0, 1, 99999, "", listener);
                    mDvbcSymAutoEdit = new AutoNumberEditText(dvbc_symbol_edit,
                            0, 1, 10000, "", listener);
                    mDvbcFreqAutoEdit = new AutoNumberEditText(dvbc_frequency_editText,
                            0, 44, 870, "MHz", listener);
                } catch (Exception e) {
                }
                JSONArray operatorList = mParameterMananer.getOperatorsTypeList(ParameterMananer.SIGNAL_QAM);
                List<String> operatorStrList = new ArrayList<>();
                try {
                    if (operatorList != null && operatorList.length() > 0) {
                        for (int i = 0; i < operatorList.length(); i++) {
                            JSONObject operator = (JSONObject) operatorList.get(i);
                            if (operator != null) {
                                String name = operator.optString("operators_name");
                                if (!TextUtils.isEmpty(name)) {
                                    operatorStrList.add(name);
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                }
                if (operatorStrList.size() > 0) {
                    ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, operatorStrList);
                    operator_spinner.setAdapter(adapter);
                    operator_spinner.setOnItemSelectedListener(new OnItemSelectedListener() {
                        @Override
                        public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                            updateDvbcOperator(i);
                        }

                        @Override
                        public void onNothingSelected(AdapterView<?> adapterView) {
                        }
                    });
                }
            }
        }
        if (!mIsDvbt) {
            if (DataMananer.VALUE_PUBLIC_SEARCH_MODE_AUTO == value) {
                updateDvbcOperator(operator_spinner.getSelectedItemPosition());
                if (operator_spinner.getAdapter() != null) {
                    dvbc_operator_containner.setVisibility(View.VISIBLE);
                }
            } else {
                mDvbcSymAutoEdit.updateDefault(mDataMananer.getIntParameters(DataMananer.KEY_DVBC_SYMBOL_RATE));
            }
        }
    }

    private void updateDvbcOperator(int index) {
        JSONArray operatorList = mParameterMananer.getOperatorsTypeList(ParameterMananer.SIGNAL_QAM);
        if (operatorList != null && operatorList.length() > index) {
            JSONObject operator = operatorList.optJSONObject(index);
            if (operator != null) {
                int networkID = operator.optInt("Networkid", 0);
                int symbolrate = operator.optInt("SymbolRate", 0);
                int freq = operator.optInt("Freqency", 0);
                freq = freq / 1000;
                updateDvbcOperator(networkID, symbolrate, freq);
            } else {
                updateDvbcOperator(0, 0, 0);
            }
        }
    }

    private void updateDvbcOperator(int networkid, int symbolerate, int freq) {
        if (mDvbcNitAutoEdit != null) {
            mDvbcNitAutoEdit.updateDefault(networkid);
        }
        if (mDvbcSymAutoEdit != null) {
            mDvbcSymAutoEdit.updateDefault(symbolerate);
        }
        if (mDvbcFreqAutoEdit != null) {
            mDvbcFreqAutoEdit.updateDefault(freq);
        }
    }

    private JSONArray initDvbcScanParamsEx() {
        Spinner dvbc_autoscantype_spinner = (Spinner) findViewById(R.id.dvbc_autoscantype_spinner);
        String scanType = DVBC_AUTO_SCANTYPE[dvbc_autoscantype_spinner.getSelectedItemPosition()];
        Spinner operator_spinner = (Spinner) findViewById(R.id.dvbc_operator_spinner);
        ArrayAdapter<String> operatorAdpater = (ArrayAdapter<String>) operator_spinner.getAdapter();
        String operator = "";
        if (operatorAdpater != null) {
            operator = (String) operatorAdpater.getItem(operator_spinner.getSelectedItemPosition());
        }

        JSONArray array = new JSONArray();
        array.put(scanType);
        array.put(operator);
        array.put(true);//retune, clear db
        array.put(DataMananer.VALUE_DVBC_MODE_LIST[mDataMananer.getIntParameters(DataMananer.KEY_DVBC_MODE)]);
        if (mDvbcNitAutoEdit != null) {
            array.put(mDvbcNitAutoEdit.getValue());
        } else {
            array.put(0);
        }
        if (mDvbcFreqAutoEdit != null) {
            array.put(mDvbcFreqAutoEdit.getValue() * 1000 *1000);//hz
        } else {
            array.put(0);
        }
        if (mDvbcSymAutoEdit != null) {
            array.put(mDvbcSymAutoEdit.getValue());
        } else {
            array.put(0);
        }
        return array;
    }

    private void updateChannelNameContainer() {
        LinearLayout public_search_channel_name_containner = (LinearLayout)findViewById(R.id.public_search_channel_containner);
        Spinner public_search_channel_name_spinner = (Spinner)findViewById(R.id.public_search_channel_spinner);

        List<String> list = null;
        List<String> newlist = new ArrayList<String>();
        ArrayAdapter<String> adapter = null;
        int select = mIsDvbt ? mDataMananer.getIntParameters(DataMananer.KEY_SEARCH_DVBT_CHANNEL_NAME) :
                mDataMananer.getIntParameters(DataMananer.KEY_SEARCH_DVBC_CHANNEL_NAME);
        list = mParameterMananer.getChannelTable(mParameterMananer.getCurrentCountryCode(), mIsDvbt, mDataMananer.getIntParameters(DataMananer.KEY_DVBT_TYPE) == 1);
        for (String one : list) {
            String[] parameter = one.split(",");//first number, second string, third number
            if (parameter != null && parameter.length == 3) {
                String result = "NO." + parameter[0] + "  " + parameter[1] + "  " + parameter[2] + "Hz";
                newlist.add(result);
            }
        }
        if (list == null) {
            Log.d(TAG, "updateChannelNameContainer can't find channel freq table");
            return;
        }
        adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, newlist);
        adapter.setDropDownViewResource(android.R.layout.simple_list_item_single_choice);
        public_search_channel_name_spinner.setAdapter(adapter);
        select = (select < list.size()) ? select : 0;
        public_search_channel_name_spinner.setSelection(select);
    }

    private JSONArray initSearchParameter(JSONArray args) {
        if (args != null) {
            if (!(DataMananer.VALUE_PUBLIC_SEARCH_MODE_AUTO == mDataMananer.getIntParameters(DataMananer.KEY_PUBLIC_SEARCH_MODE))) {
                String parameter = getParameter();
                if (!TextUtils.isEmpty(parameter)) {
                    int isfrequencysearch = mDataMananer.getIntParameters(DataMananer.KEY_IS_FREQUENCY);
                    if (mIsDvbt) {
                        if (DataMananer.VALUE_FREQUENCY_MODE == isfrequencysearch) {
                            args.put(false);//nit not used, default false
                            args.put(Integer.valueOf(parameter) * 1000);//khz to hz
                            args.put(DataMananer.VALUE_DVBT_BANDWIDTH_LIST[mDataMananer.getIntParameters(DataMananer.KEY_DVBT_BANDWIDTH)]);
                            args.put(DataMananer.VALUE_DVBT_MODE_LIST[mDataMananer.getIntParameters(DataMananer.KEY_DVBT_MODE)]);
                            args.put(DataMananer.VALUE_DVBT_TYPE_LIST[mDataMananer.getIntParameters(DataMananer.KEY_DVBT_TYPE)]);
                        } else {
                            parameter = getChannelIndex();
                            if (parameter == null) {
                                Log.d(TAG, "initSearchParameter dvbt search can't find channel index");
                                return null;
                            }
                            args.put(Integer.valueOf(parameter));
                        }
                    } else {
                        if (DataMananer.VALUE_FREQUENCY_MODE == isfrequencysearch) {
                            args.put(false);//nit not used, default false
                            args.put(Integer.valueOf(parameter) * 1000);//khz to hz
                            args.put(DataMananer.VALUE_DVBC_MODE_LIST[mDataMananer.getIntParameters(DataMananer.KEY_DVBC_MODE)]);
                            args.put(getUpdatedDvbcSymbolRate());
                        } else {
                            parameter = getChannelIndex();
                            if (parameter == null) {
                                Log.d(TAG, "initSearchParameter dvbc search can't find channel index");
                                return null;
                            }
                            args.put(Integer.valueOf(parameter));
                        }
                    }
                    return args;
                } else {
                    return null;
                }
            } else {
                args.put(false);//nit not used, default false
                return args;
            }
        } else {
            return null;
        }
    }

    private String getParameter() {
        String parameter = null;
        EditText public_type_edit = (EditText)findViewById(R.id.public_typein_edit);
        Editable editable = public_type_edit.getText();
        int isfrequencysearch = mDataMananer.getIntParameters(DataMananer.KEY_IS_FREQUENCY);
        if (DataMananer.VALUE_FREQUENCY_MODE != isfrequencysearch) {
            parameter = getChannelIndex();
        } else if (editable != null) {
            String value = editable.toString();
            if (!TextUtils.isEmpty(value)/* && TextUtils.isDigitsOnly(value)*/) {
                //float for frequency
                float toFloat = Float.valueOf(value);
                int toInt = (int)(toFloat * 1000.0f);//khz
                parameter = String.valueOf(toInt);
            }
        }

        return parameter;
    }

    private int getUpdatedDvbcSymbolRate() {
        int parameter = DataMananer.VALUE_DVBC_SYMBOL_RATE;
        if (mDvbcSymAutoEdit != null) {
            parameter = mDvbcSymAutoEdit.getValue();
        }
        mDataMananer.saveIntParameters(DataMananer.KEY_DVBC_SYMBOL_RATE, parameter);

        return parameter;
    }

    private String getChannelIndex() {
        String result = null;
        int index = mIsDvbt ? mDataMananer.getIntParameters(DataMananer.KEY_SEARCH_DVBT_CHANNEL_NAME) :
                mDataMananer.getIntParameters(DataMananer.KEY_SEARCH_DVBC_CHANNEL_NAME);
        List<String> list = mParameterMananer.getChannelTable(mParameterMananer.getCurrentCountryCode(), mIsDvbt, mDataMananer.getIntParameters(DataMananer.KEY_DVBT_TYPE) == 1);
        String channelInfo = (index < list.size()) ? list.get(index) : null;
        if (channelInfo != null) {
            String[] parameter = channelInfo.split(",");//first number, second string, third number
            if (parameter != null && parameter.length == 3 && TextUtils.isDigitsOnly(parameter[0])) {
                result = parameter[0];
                Log.d(TAG, "getChannelIndex channel index = " + parameter[0] + ", name = " + parameter[1] + ", freq = " + parameter[2]);
            }
        }
        return result;
    }

    private void sendStartSearch() {
        if (mThreadHandler != null) {
            mThreadHandler.removeMessages(MSG_START_SEARCH);
            Message mess = mThreadHandler.obtainMessage(MSG_START_SEARCH, 0, 0, null);
            boolean info = mThreadHandler.sendMessageDelayed(mess, 0);
            Log.d(TAG, "sendMessage MSG_START_SEARCH " + info);
        }
    }

    private void startSearch() {
        setSearchStatus("Searching", "");
        setSearchProgressIndeterminate(false);
        startMonitoringSearch();
        mFoundServiceNumber = 0;
        try {
            JSONArray args = new JSONArray();
            args.put(false); // Commit
            DtvkitGlueClient.getInstance().request(mIsDvbt ? "Dvbt.finishSearch" : "Dvbc.finishSearch", args);
        } catch (Exception e) {
            setSearchStatus("Failed to finish search", e.getMessage());
            return;
        }

        try {
            JSONArray args = new JSONArray();
            args.put(true); // retune
            args = initSearchParameter(args);
            if (args != null) {
                String command = null;
                int searchmode = mDataMananer.getIntParameters(DataMananer.KEY_PUBLIC_SEARCH_MODE);
                int isfrequencysearch = mDataMananer.getIntParameters(DataMananer.KEY_IS_FREQUENCY);
                if (!(DataMananer.VALUE_PUBLIC_SEARCH_MODE_AUTO == searchmode)) {
                    if (isfrequencysearch == DataMananer.VALUE_FREQUENCY_MODE) {
                        command = (mIsDvbt ? "Dvbt.startManualSearchByFreq" : "Dvbc.startManualSearchByFreq");
                    } else {
                        command = (mIsDvbt ? "Dvbt.startManualSearchById" : "Dvbc.startManualSearchById");
                    }
                    mSearchManualAutoType = DtvkitDvbScanSelect.SEARCH_TYPE_MANUAL;
                } else {
                    command = (mIsDvbt ? "Dvbt.startSearch" : "Dvbc.startSearchEx");
                    mSearchManualAutoType = DtvkitDvbScanSelect.SEARCH_TYPE_AUTO;
                    if (!mIsDvbt) {
                        args = initDvbcScanParamsEx();
                    }
                }
                if (mIsDvbt) {
                    mSearchDvbcDvbtType = DtvkitDvbScanSelect.SEARCH_TYPE_DVBT;
                } else {
                    mSearchDvbcDvbtType = DtvkitDvbScanSelect.SEARCH_TYPE_DVBC;
                }
                Log.d(TAG, "command = " + command + ", args = " + args.toString());
                DtvkitGlueClient.getInstance().request(command, args);
                mStartSearch = true;
                mParameterMananer.saveChannelIdForSource(-1);
            } else {
                stopMonitoringSearch();
                setSearchStatus("parameter not complete", "");
                stopSearch();
            }
        } catch (Exception e) {
            stopMonitoringSearch();
            setSearchStatus("Failed to start search", e.getMessage());
            stopSearch();
        }
    }

    private void sendStopSearch() {
        if (mThreadHandler != null) {
            mThreadHandler.removeMessages(MSG_STOP_SEARCH);
            Message mess = mThreadHandler.obtainMessage(MSG_STOP_SEARCH, 0, 0, null);
            boolean info = mThreadHandler.sendMessageDelayed(mess, 0);
            Log.d(TAG, "sendMessage MSG_STOP_SEARCH " + info);
        }
    }

    private void stopSearch() {
        mStartSearch = false;
        enableSearchButton(true);
        try {
            JSONArray args = new JSONArray();
            args.put(true); // Commit
            DtvkitGlueClient.getInstance().request(mIsDvbt ? "Dvbt.finishSearch" : "Dvbc.finishSearch", args);
        } catch (Exception e) {
            setSearchStatus("Failed to finish search", e.getMessage());
            return;
        }
    }

    private void sendFinishSearch(boolean skipConfirmNetwork) {
        if (mThreadHandler != null) {
            mThreadHandler.removeMessages(MSG_FINISH_SEARCH);
            Message mess = mThreadHandler.obtainMessage(MSG_FINISH_SEARCH, skipConfirmNetwork ? 1 : 0, 0, null);
            boolean info = mThreadHandler.sendMessageDelayed(mess, 1000);
            Log.d(TAG, "sendMessage MSG_FINISH_SEARCH " + info);
        }
    }

    private void prepareSearchFinished(boolean skipConfirmNetwork) {
        boolean needSetTargetRegion = false;
        boolean autoSearch = mDataMananer.getIntParameters(DataMananer.KEY_PUBLIC_SEARCH_MODE) == DataMananer.VALUE_PUBLIC_SEARCH_MODE_AUTO;
        JSONArray array = mParameterMananer.getTargetRegions(TargetRegionManager.TARGET_REGION_COUNTRY, -1, -1, -1);
        if (mParameterMananer.needConfirmTargetRegion(array)) {
            needSetTargetRegion = true;
        }
        if (autoSearch && needSetTargetRegion && !skipConfirmNetwork) {
            final TargetRegionManager regionManager = new TargetRegionManager(DtvkitDvbtSetup.this);
            regionManager.setRegionCallback(new TargetRegionManager.TargetRegionsCallbacks() {
                @Override
                public Map<String, Integer> requestRegionList(int target_id) {
                    HashMap<String, Integer> map = new HashMap<String, Integer>();
                    JSONArray array = null;
                    switch (target_id) {
                        case TargetRegionManager.TARGET_REGION_COUNTRY:
                            array = mParameterMananer.getTargetRegions(target_id, -1, -1, -1);
                            break;
                        case TargetRegionManager.TARGET_REGION_PRIMARY:
                            array = mParameterMananer.getTargetRegions(target_id,
                                        regionManager.getRegionCode(regionManager.TARGET_REGION_COUNTRY),
                                        -1, -1);
                            break;
                        case TargetRegionManager.TARGET_REGION_SECONDARY:
                            array = mParameterMananer.getTargetRegions(target_id,
                                        regionManager.getRegionCode(regionManager.TARGET_REGION_COUNTRY),
                                        regionManager.getRegionCode(regionManager.TARGET_REGION_PRIMARY),
                                        -1);
                            break;
                        case TargetRegionManager.TARGET_REGION_TERTIARY:
                            array = mParameterMananer.getTargetRegions(target_id,
                                        regionManager.getRegionCode(regionManager.TARGET_REGION_COUNTRY),
                                        regionManager.getRegionCode(regionManager.TARGET_REGION_PRIMARY),
                                        regionManager.getRegionCode(regionManager.TARGET_REGION_SECONDARY));
                            break;
                    }
                    if (array != null && array.length() > 0) {
                        JSONObject region = null;
                        String region_name = null;
                        int region_code = -1;
                        for (int i = 0; i < array.length(); i++) {
                            region = mParameterMananer.getJSONObjectFromJSONArray(array, i);
                            region_name = mParameterMananer.getTargetRegionName(target_id, region);
                            region_code = mParameterMananer.getTargetRegionCode(target_id, region);
                            map.put(region_name, region_code);
                        }
                    } else {
                        Log.d(TAG, "No regions for target_id " + target_id);
                    }
                    if (map.size() > 0)
                        return map;
                    return null;
                }

                @Override
                public boolean onRegionSelected(int target_id, int selection_id) {
                    return true;
                }

                @Override
                public void onFinishWithSelections(int country, int primary, int secondary, int tertiary) {
                    if (country != -1) {
                        mParameterMananer.setTargetRegionSelection(
                            TargetRegionManager.TARGET_REGION_COUNTRY, country);
                    }

                    if (primary != -1) {
                        mParameterMananer.setTargetRegionSelection(
                            TargetRegionManager.TARGET_REGION_PRIMARY, primary);
                    }
                    if (secondary != -1) {
                        mParameterMananer.setTargetRegionSelection(
                            TargetRegionManager.TARGET_REGION_SECONDARY, secondary);
                    }
                    if (tertiary != -1) {
                        mParameterMananer.setTargetRegionSelection(
                            TargetRegionManager.TARGET_REGION_TERTIARY, tertiary);
                    }
                    onSearchFinished(skipConfirmNetwork);
                }
            });
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    regionManager.start();
                }
            });
        } else {
            onSearchFinished(skipConfirmNetwork);
        }

    }

    private void onSearchFinished(boolean skipConfirmNetwork) {
        mStartSearch = false;
        enableStopSearchButton(false);
        setSearchStatus("Finishing search", "");
        setSearchProgressIndeterminate(true);
        stopMonitoringSearch();

        try {
            JSONArray args = new JSONArray();
            args.put(true); // Commit
            DtvkitGlueClient.getInstance().request(mIsDvbt ? "Dvbt.finishSearch" : "Dvbc.finishSearch", args);
        } catch (Exception e) {
            setSearchStatus("Failed to finish search", e.getMessage());
            return;
        }
        boolean networksNeedUpdate = false;
        boolean lcnConflictNeedUpdate = false;
        boolean autoSearch = mDataMananer.getIntParameters(DataMananer.KEY_PUBLIC_SEARCH_MODE) == DataMananer.VALUE_PUBLIC_SEARCH_MODE_AUTO;
        JSONArray array = mParameterMananer.getConflictLcn();
        if (mParameterMananer.needConfirmLcnInfomation(array)) {
            lcnConflictNeedUpdate = true;
        } else {
            array = mParameterMananer.getNetworksOfRegion();
            if (mParameterMananer.needConfirmNetWorkInfomation(array)) {
                networksNeedUpdate = true;
            }
        }
        if (lcnConflictNeedUpdate) {
            final JSONArray conflictLcnArray = array;
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    showLcnConflictServiceConfirmDialog(DtvkitDvbtSetup.this, conflictLcnArray);
                }
            });
        } else if (autoSearch && !skipConfirmNetwork && networksNeedUpdate) {
            final JSONArray networlArray = array;
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    showNetworkInfoConfirmDialog(DtvkitDvbtSetup.this, networlArray);
                }
            });
        } else {
            updateChannelList();
        }
    }

    private void updateChannelList() {
        updateChannelListAndCheckLcn(false);
    }

    private void updateChannelListAndCheckLcn(boolean needCheckLcn) {
        //update search results as After the search is finished, the lcn will be reordered
        mServiceList = getServiceList();
        mFoundServiceNumber = getFoundServiceNumber();
        if (mFoundServiceNumber == 0 && mServiceList != null && mServiceList.length() > 0) {
            Log.d(TAG, "mFoundServiceNumber erro use mServiceList length = " + mServiceList.length());
            mFoundServiceNumber = mServiceList.length();
        }
        setSearchStatus("Updating guide", "");
        startMonitoringSync();
        // By default, gets all channels and 1 hour of programs (DEFAULT_IMMEDIATE_EPG_DURATION_MILLIS)
        EpgSyncJobService.cancelAllSyncRequests(this);

        // If the intent that started this activity is from Live Channels app
        String inputId = this.getIntent().getStringExtra(TvInputInfo.EXTRA_INPUT_ID);
        Log.i(TAG, String.format("inputId: %s", inputId));
        //EpgSyncJobService.requestImmediateSync(this, inputId, true, new ComponentName(this, DtvkitEpgSync.class)); // 12 hours
        Bundle parameters = new Bundle();
        if (needCheckLcn) {
            parameters.putBoolean(EpgSyncJobService.BUNDLE_KEY_SYNC_SEARCHED_LCN_CONFLICT, false);
        }
        int searchmode = mDataMananer.getIntParameters(DataMananer.KEY_PUBLIC_SEARCH_MODE);
        parameters.putString(EpgSyncJobService.BUNDLE_KEY_SYNC_SEARCHED_MODE, DataMananer.VALUE_PUBLIC_SEARCH_MODE_AUTO == searchmode ? EpgSyncJobService.BUNDLE_VALUE_SYNC_SEARCHED_MODE_AUTO : EpgSyncJobService.BUNDLE_VALUE_SYNC_SEARCHED_MODE_MANUAL);
        parameters.putString(EpgSyncJobService.BUNDLE_KEY_SYNC_SEARCHED_SIGNAL_TYPE, mIsDvbt ? "DVB-T" : "DVB-C");
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

    private void setSearchProgress(final int progress) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                final ProgressBar bar = (ProgressBar) findViewById(R.id.searchprogress);
                bar.setProgress(progress);
            }
        });
    }

    private void setSearchProgressIndeterminate(final Boolean indeterminate) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                final ProgressBar bar = (ProgressBar) findViewById(R.id.searchprogress);
                bar.setIndeterminate(indeterminate);
            }
        });
    }

    private void enableSearchButton(final boolean enable) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                findViewById(R.id.terrestrialstartsearch).setEnabled(enable);
            }
        });
    }

    private void enableStopSearchButton(final boolean enable) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                findViewById(R.id.terrestrialstopsearch).setEnabled(enable);
            }
        });
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

    private void sendOnSignal(final Map<String, Object> map) {
        if (mThreadHandler != null) {
            mThreadHandler.removeMessages(MSG_ON_SIGNAL);
            Message mess = mThreadHandler.obtainMessage(MSG_ON_SIGNAL, 0, 0, map);
            boolean info = mThreadHandler.sendMessageDelayed(mess, 0);
            Log.d(TAG, "sendMessage MSG_ON_SIGNAL " + info);
        }
    }

    private void dealOnSignal(final Map<String, Object> map) {
        Log.d(TAG, "dealOnSignal map = " + map);
        if (map == null) {
            Log.d(TAG, "dealOnSignal null map");
            return;
        }
        String signal = (String)map.get("signal");
        JSONObject data = (JSONObject)map.get("data");
        if (signal != null && ((mIsDvbt && signal.equals("DvbtStatusChanged")) || (!mIsDvbt && signal.equals("DvbcStatusChanged")))) {
            int progress = getSearchProcess(data);
            Log.d(TAG, "onSignal progress = " + progress);
            int found = getFoundServiceNumber();
            setSearchProgress(progress);
            setSearchStatus(String.format(Locale.ENGLISH, "Searching (%d%%)", progress), String.format(Locale.ENGLISH, "Found %d services", found));
            if (progress >= 100) {
                //onSearchFinished();
                sendFinishSearch(false);
            }
        }
    }

    private void sendFinish() {
        if (mThreadHandler != null) {
            mFinish = true;
            mThreadHandler.removeMessages(MSG_FINISH);
            Message mess = mThreadHandler.obtainMessage(MSG_FINISH, 0, 0, null);
            boolean info = mThreadHandler.sendMessageDelayed(mess, 0);
            Log.d(TAG, "sendMessage MSG_FINISH " + info);
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
                //excute by child thread
                mThreadHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        Log.d(TAG, "showNetworkInfoConfirmDialog onItemClick position = " + position);
                        mParameterMananer.setNetworkPreferedOfRegion(mParameterMananer.getNetworkId(networkArray, position));
                        //ui need to be updated in main handler
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                alert.dismiss();
                            }
                        });
                    }
                });
            }
        });
        alert.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                Log.d(TAG, "showNetworkInfoConfirmDialog onDismiss");
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        updateChannelList();
                    }
                });
            }
        });

        alert.getWindow().setType(WindowManager.LayoutParams.TYPE_APPLICATION_ATTACHED_DIALOG);
        alert.setView(dialogView);
        alert.show();
        WindowManager.LayoutParams params = alert.getWindow().getAttributes();
        params.width = (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 500, context.getResources().getDisplayMetrics());
        params.height = WindowManager.LayoutParams.WRAP_CONTENT;
        alert.getWindow().setAttributes(params);
        //alert.getWindow().setBackgroundDrawableResource(R.drawable.dialog_background);
    }

    private void showLcnConflictServiceConfirmDialog(final Context context, final JSONArray lcnConflictArray) {
        Log.d(TAG, "showLcnConflictServiceConfirmDialog lcnConflictArray = " + lcnConflictArray);
        final AlertDialog.Builder builder = new AlertDialog.Builder(context);
        final AlertDialog alert = builder.create();
        final View dialogView = View.inflate(context, R.layout.confirm_region_network, null);
        final TextView title = (TextView) dialogView.findViewById(R.id.dialog_title);
        final ListView listView = (ListView) dialogView.findViewById(R.id.dialog_listview);
        final List<HashMap<String, Object>> DATALIST = new ArrayList<HashMap<String, Object>>();
        final int[] DEALTINDEX = {0, 0};
        final int[] FINALINDEX = {0};
        final boolean[] NEEDSHOWNEXT = {false};
        if (lcnConflictArray != null && lcnConflictArray.length() > 0) {
            int[] updateDefaultIndex = getLcnConflictDataList(DATALIST, lcnConflictArray, DEALTINDEX[0]);
            DEALTINDEX[0] = updateDefaultIndex[0];
            DEALTINDEX[1] = updateDefaultIndex[1];
        } else {
            Log.d(TAG, "showLcnConflictServiceConfirmDialog no lcnConflictArray");
            return;
        }

        listView.setAdapter(getLcnConflictDataListSimpleAdapter(context, DATALIST));
        title.setText(R.string.search_confirm_lcn);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, final int position, long id) {
                boolean finalSelect = false;
                if (DATALIST.size() < position) {
                    Log.d(TAG, "showLcnConflictServiceConfirmDialog invalid position = " + position);
                    return;
                } else if (DATALIST.size() - 1 == position) {
                    //select end
                    Log.d(TAG, "showLcnConflictServiceConfirmDialog finalSelect position = " + position);
                    finalSelect = true;
                }
                int realIndex = DEALTINDEX[0] + position;
                JSONObject serviceObj = mParameterMananer.getJSONObjectFromJSONArray(lcnConflictArray, realIndex);
                if (serviceObj == null) {
                    Log.d(TAG, "showLcnConflictServiceConfirmDialog service not found");
                    return;
                } else {
                    Log.d(TAG, "showLcnConflictServiceConfirmDialog serviceObj = " + serviceObj);
                }
                FINALINDEX[0] = realIndex;
                //excute by child thread
                mThreadHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        Log.d(TAG, "showLcnConflictServiceConfirmDialog onItemClick position = " + position + ", DEALTINDEX[0] = " + DEALTINDEX[0] + ", DEALTINDEX[1] = " + DEALTINDEX[1] + ", FINALINDEX[0] = " + FINALINDEX[0]);
                        String uri = null;
                        String tunerType = null;
                        try {
                            uri = serviceObj.getString("uri");
                            tunerType = serviceObj.getString("sig_name");
                        } catch (Exception e) {
                            Log.d(TAG, "showLcnConflictServiceConfirmDialog get uri Exception = " + e.getMessage());
                        }
                        if (uri != null && tunerType != null) {
                            Log.d(TAG, "showLcnConflictServiceConfirmDialog selectServiceKeepConflictLcn");
                            mParameterMananer.selectServiceKeepConflictLcn(uri, tunerType);
                            //ui need to be updated in main handler
                            if (mParameterMananer.hasSelectToEnd(lcnConflictArray, FINALINDEX[0])) {
                                NEEDSHOWNEXT[0] = false;
                            } else {
                                NEEDSHOWNEXT[0] = true;
                            }
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    //turn to next lcn
                                    if (NEEDSHOWNEXT[0]) {
                                        Log.d(TAG, "showLcnConflictServiceConfirmDialog onItemClick select next");
                                        int[] updateDefaultIndex = getLcnConflictDataList(DATALIST, lcnConflictArray, DEALTINDEX[1]);
                                        DEALTINDEX[0] = updateDefaultIndex[0];
                                        DEALTINDEX[1] = updateDefaultIndex[1];
                                        listView.setAdapter(getLcnConflictDataListSimpleAdapter(context, DATALIST));
                                    } else {
                                        Log.d(TAG, "showLcnConflictServiceConfirmDialog onItemClick select over");
                                        alert.dismiss();
                                    }
                                }
                            });
                        }
                    }
                });
            }
        });
        alert.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                if (mParameterMananer.hasSelectToEnd(lcnConflictArray, FINALINDEX[0])) {
                    Log.d(TAG, "showLcnConflictServiceConfirmDialog select over onDismiss");
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            updateChannelList();
                        }
                    });
                } else {
                    Log.d(TAG, "showLcnConflictServiceConfirmDialog select default for the rest");
                    mThreadHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            mParameterMananer.dealRestLcnConflictAsDefault(lcnConflictArray, DEALTINDEX[0]);
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    updateChannelList();
                                }
                            });
                        }
                    });
                }
            }
        });

        alert.getWindow().setType(WindowManager.LayoutParams.TYPE_APPLICATION_ATTACHED_DIALOG);
        alert.setView(dialogView);
        alert.show();
        WindowManager.LayoutParams params = alert.getWindow().getAttributes();
        params.width = (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 500, context.getResources().getDisplayMetrics());
        params.height = WindowManager.LayoutParams.WRAP_CONTENT;
        alert.getWindow().setAttributes(params);
        //alert.getWindow().setBackgroundDrawableResource(R.drawable.dialog_background);
    }

    /*
    * return start index and next new index that need to be dealt
    */
    private int[] getLcnConflictDataList(List<HashMap<String, Object>> source, JSONArray values, int index) {
        int[] result = {-1, -1};
        result[0] = index;
        if (source != null && values != null && values.length() > 0 && values.length() > index && index > -1) {
            source.clear();
            JSONObject lcnConflictObj = null;
            String name = null;
            int lcn = -1;
            int previousLcn = -1;
            for (int i = index; i < values.length(); i++) {
                HashMap<String, Object> map = new HashMap<String, Object>();
                lcnConflictObj = mParameterMananer.getJSONObjectFromJSONArray(values, i);
                name = mParameterMananer.getLcnServiceName(lcnConflictObj);
                lcn = mParameterMananer.getLcnServiceLcnValue(lcnConflictObj);
                if (previousLcn != -1 && previousLcn != lcn) {
                    //record new start index
                    result[1] = i;
                    break;
                } else if (i == (values.length() - 1)) {
                    //search to the end
                    result[1] = values.length() - 1;
                }
                previousLcn = lcn;
                map.put("name", lcn + "  " + name);
                source.add(map);
            }
        }
        Log.d(TAG, "getLcnConflictDataList result[0] = " + result[0] + ", result[1] = " + result[1]);
        return result;
    }

    private SimpleAdapter getLcnConflictDataListSimpleAdapter(Context context, List<HashMap<String, Object>> source) {
        SimpleAdapter result = null;
        if (context != null && source != null && source.size() > 0) {
            result = new SimpleAdapter(context, source,
                R.layout.region_network_list,
                new String[] {"name"},
                new int[] {R.id.name});
        }
        return result;
    }

    /*private boolean hasSelectToEnd(JSONArray lcnConflictArray, int selectEndIndex) {
        boolean result = false;
        if (lcnConflictArray != null && lcnConflictArray.length() > 0) {
            if (selectEndIndex >= lcnConflictArray.length() - 1) {
                result = true;
                return result;
            }
            int finalLcn = -1;
            JSONObject lcnConflictObj = null;
            int conflictLcn = -1;
            for (int i = selectEndIndex; i < lcnConflictArray.length(); i++) {
                lcnConflictObj = mParameterMananer.getJSONObjectFromJSONArray(lcnConflictArray, i);
                conflictLcn = mParameterMananer.getLcnServiceLcnValue(lcnConflictObj);
                if (selectEndIndex == i) {
                    finalLcn = conflictLcn;
                    continue;
                }
                if (finalLcn != conflictLcn) {
                    result = false;
                    break;
                } else {
                    result = true;
                }
            }
        }
        Log.d(TAG, "hasSelectToEnd result = " + result);
        return result;
    }

    private void dealRestLcnConflictAsDefault(JSONArray lcnConflictArray, int restStartIndex) {
        if (lcnConflictArray != null && lcnConflictArray.length() > 0) {
            JSONObject lcnConflictObj = null;
            int lcn = -1;
            String tunerType = null;
            int previousLcn = -1;
            boolean needUpdate = false;
            for (int i = restStartIndex; i < lcnConflictArray.length(); i++) {
                lcnConflictObj = mParameterMananer.getJSONObjectFromJSONArray(lcnConflictArray, i);
                lcn = mParameterMananer.getLcnServiceLcnValue(lcnConflictObj);
                tunerType = mParameterMananer.getLcnServiceTunerType(lcnConflictObj);
                if (previousLcn == -1) {
                    previousLcn = lcn;
                    needUpdate = true;
                } else if (previousLcn != lcn) {
                    previousLcn = lcn;
                    needUpdate = true;
                } else {
                    needUpdate = false;
                }
                if (needUpdate) {
                    Log.d(TAG, "dealRestLcnConflictAsDefault needUpdate previousLcn = " + previousLcn + ", tunerType = " + tunerType);
                    mParameterMananer.selectDefaultLcnConflictProcess(previousLcn, tunerType);
                }
            }
        }
    }*/

    interface InvalidEditInputListener {
        public void onInputInvalid();
    }

    class AutoNumberEditText {
        private EditText mEditText;
        private int mDefault;
        private String mDefaultHint;
        private int mMinNumber;
        private int mMaxNumber;
        private String mUnit = "";
        private InvalidEditInputListener mListener;
        public AutoNumberEditText(EditText editText, int defaultVal, int min, int max, String unit, InvalidEditInputListener listener) throws Exception {
            if (editText != null) {
                mEditText = editText;
                if (defaultVal < min || defaultVal > max)
                    defaultVal = 0;
                if (min > max) {
                    min = max - 1;
                }
                if (unit == null) {
                    unit = "";
                }
                mDefault = defaultVal;
                mMinNumber = min;
                mMaxNumber = max;
                mListener = listener;
                mUnit = unit;
                mDefaultHint = (defaultVal == 0) ? "Auto" : defaultVal + " " + unit;
                editText.setHint(mDefaultHint);
                editText.setText("");
            } else {
                throw new Exception("Cannot use null EditText here.");
            }
            mEditText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                @Override
                public void onFocusChange(View v, boolean hasFocus) {
                    if (hasFocus) {
                        mEditText.setHint("" + mMinNumber + "~" + mMaxNumber + " " + mUnit);
                    } else {
                        String strValue = mEditText.getText().toString();
                        int value = mDefault;
                        try {
                            value = Integer.parseInt(strValue);
                        } catch (Exception e) {
                        }
                        if (value > mMaxNumber || value < mMinNumber) {
                            if (mListener != null && value != mDefault) {
                                mListener.onInputInvalid();
                            }
                            mEditText.setHint(mDefaultHint);
                            mEditText.setText("");
                        }else {
                            if (mDefault != value) {
                                mEditText.setHint(strValue);
                            } else {
                                mEditText.setHint(mDefaultHint);
                            }
                        }
                    }
                }
            });
        }
        public void updateDefault(int defaultVal) {
            if (defaultVal < mMinNumber || defaultVal > mMaxNumber)
                defaultVal = 0;
            if (defaultVal != mDefault) {
                mDefault = defaultVal;
                mDefaultHint = (defaultVal == 0) ? "Auto" : defaultVal + " " + mUnit;
            }
            mEditText.setHint(mDefaultHint);
            mEditText.setText("");
        }
        public int getValue() {
            int ret = mDefault;
            String strValue = mEditText.getText().toString();
            if (TextUtils.isEmpty(strValue)) {
                return ret;
            }
            try {
                ret = Integer.parseInt(strValue);
            } catch (Exception e) {
            }
            if (ret > mMaxNumber || ret < mMinNumber) {
                ret = mDefault;
            }
            return ret;
        }
    }
}
