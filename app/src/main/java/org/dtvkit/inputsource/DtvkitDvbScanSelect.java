package org.dtvkit.inputsource;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.widget.RadioGroup;
import android.widget.Button;
import android.media.tv.TvInputInfo;
import android.content.Context;
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
import android.widget.TextView;
import android.view.WindowManager;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.droidlogic.settings.ConstantManager;
import org.droidlogic.dtvkit.DtvkitGlueClient;
import com.droidlogic.fragment.ParameterMananer;

public class DtvkitDvbScanSelect extends Activity {
    private static final String TAG = "DtvkitDvbScanSelect";

    private static final int[] RES = {R.id.select_dvbc, R.id.select_dvbt, R.id.select_dvbs};

    public static final int REQUEST_CODE_START_DVBC_ACTIVITY = 1;
    public static final int REQUEST_CODE_START_DVBT_ACTIVITY = 2;
    public static final int REQUEST_CODE_START_DVBS_ACTIVITY = 3;
    public static final int REQUEST_CODE_START_SETTINGS_ACTIVITY = 4;
    public static final int REQUEST_CODE_START_ISDBT_ACTIVITY = 5;

    public static final int SEARCH_TYPE_MANUAL = 0;
    public static final int SEARCH_TYPE_AUTO = 1;
    public static final int SEARCH_TYPE_DVBC = 0;
    public static final int SEARCH_TYPE_DVBT = 1;
    public static final int SEARCH_TYPE_DVBS = 2;
    public static final int SEARCH_TYPE_ISDBT = 3;
    public static final String SEARCH_TYPE_MANUAL_AUTO = "search_manual_auto";
    public static final String SEARCH_TYPE_DVBS_DVBT_DVBC = "search_dvbs_dvbt_dvbc";
    public static final String SEARCH_FOUND_SERVICE_NUMBER = "service_number";
    public static final String SEARCH_FOUND_SERVICE_LIST = "service_list";
    public static final String SEARCH_FOUND_SERVICE_INFO = "service_info";
    public static final String SEARCH_FOUND_FIRST_SERVICE = "first_service";

    private DataMananer mDataMananer;
    private Intent mIntent = null;
    private ParameterMananer mParameterMananer;
    private Context mContext;
    private int currentIndex = 0;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.select_search_activity);
        mContext = this;
        mIntent = getIntent();
        mDataMananer = new DataMananer(this);
        mParameterMananer = new ParameterMananer(this, DtvkitGlueClient.getInstance());
        initLayout();
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_CODE_START_DVBC_ACTIVITY:
            case REQUEST_CODE_START_DVBT_ACTIVITY:
            case REQUEST_CODE_START_DVBS_ACTIVITY:
            case REQUEST_CODE_START_SETTINGS_ACTIVITY:
            case REQUEST_CODE_START_ISDBT_ACTIVITY:
                if (resultCode == RESULT_OK) {
                    setResult(RESULT_OK, data);
                    finish();
                } else {
                    setResult(RESULT_CANCELED);
                }
                break;
            default:
                // do nothing
                Log.d(TAG, "onActivityResult other request");
        }
    }

    private void initLayout() {
        int index = mDataMananer.getIntParameters(DataMananer.KEY_SELECT_SEARCH_ACTIVITY);
        Button dvbc = (Button)findViewById(R.id.select_dvbc);
        final Intent intent = new Intent();
        final String inputId = mIntent.getStringExtra(TvInputInfo.EXTRA_INPUT_ID);
        final String pvrStatus = mIntent.getStringExtra(ConstantManager.KEY_LIVETV_PVR_STATUS);
        if (inputId != null) {
            intent.putExtra(TvInputInfo.EXTRA_INPUT_ID, inputId);
        }
        //init pvr set flag when inited
        String firstPvrFlag = PvrStatusConfirmManager.read(this, PvrStatusConfirmManager.KEY_PVR_CLEAR_FLAG);
        if (!PvrStatusConfirmManager.KEY_PVR_CLEAR_FLAG_FIRST.equals(firstPvrFlag)) {
            PvrStatusConfirmManager.store(this, PvrStatusConfirmManager.KEY_PVR_CLEAR_FLAG, PvrStatusConfirmManager.KEY_PVR_CLEAR_FLAG_FIRST);
        }
        dvbc.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                /*
                if (mParameterMananer.checkIsGermanyCountry()) {
                    JSONArray operTypeList = mParameterMananer.getOperatorsTypeList(ParameterMananer.SIGNAL_QAM);
                    showOperatorTypeConfirmDialog(ParameterMananer.SIGNAL_QAM, mContext, operTypeList);
                }*/
                String pvrFlag = PvrStatusConfirmManager.read(DtvkitDvbScanSelect.this, PvrStatusConfirmManager.KEY_PVR_CLEAR_FLAG);
                if (pvrStatus != null && PvrStatusConfirmManager.KEY_PVR_CLEAR_FLAG_FIRST.equals(pvrFlag)) {
                    intent.putExtra(ConstantManager.KEY_LIVETV_PVR_STATUS, pvrStatus);
                } else {
                    intent.putExtra(ConstantManager.KEY_LIVETV_PVR_STATUS, "");
                }
                intent.setClassName(DataMananer.KEY_PACKAGE_NAME, DataMananer.KEY_ACTIVITY_DVBT);
                intent.putExtra(DataMananer.KEY_IS_DVBT, false);
                startActivityForResult(intent, REQUEST_CODE_START_DVBC_ACTIVITY);
                mDataMananer.saveIntParameters(DataMananer.KEY_SELECT_SEARCH_ACTIVITY, DataMananer.SELECT_DVBC);
                Log.d(TAG, "select_dvbc inputId = " + inputId);
            }
        });
        Button dvbt = (Button)findViewById(R.id.select_dvbt);
        dvbt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String pvrFlag = PvrStatusConfirmManager.read(DtvkitDvbScanSelect.this, PvrStatusConfirmManager.KEY_PVR_CLEAR_FLAG);
                if (pvrStatus != null && PvrStatusConfirmManager.KEY_PVR_CLEAR_FLAG_FIRST.equals(pvrFlag)) {
                    intent.putExtra(ConstantManager.KEY_LIVETV_PVR_STATUS, pvrStatus);
                } else {
                    intent.putExtra(ConstantManager.KEY_LIVETV_PVR_STATUS, "");
                }
                intent.setClassName(DataMananer.KEY_PACKAGE_NAME, DataMananer.KEY_ACTIVITY_DVBT);
                intent.putExtra(DataMananer.KEY_IS_DVBT, true);
                startActivityForResult(intent, REQUEST_CODE_START_DVBT_ACTIVITY);
                mDataMananer.saveIntParameters(DataMananer.KEY_SELECT_SEARCH_ACTIVITY, DataMananer.SELECT_DVBT);
                Log.d(TAG, "select_dvbt inputId = " + inputId);
            }
        });
        Button dvbs = (Button)findViewById(R.id.select_dvbs);
        dvbs.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mParameterMananer.checkIsGermanyCountry()) {
                    JSONArray operTypeList = mParameterMananer.getOperatorsTypeList(ParameterMananer.SIGNAL_QPSK);
                    showOperatorTypeConfirmDialog(ParameterMananer.SIGNAL_QPSK, mContext, operTypeList);
                }
                String pvrFlag = PvrStatusConfirmManager.read(DtvkitDvbScanSelect.this, PvrStatusConfirmManager.KEY_PVR_CLEAR_FLAG);
                if (pvrStatus != null && PvrStatusConfirmManager.KEY_PVR_CLEAR_FLAG_FIRST.equals(pvrFlag)) {
                    intent.putExtra(ConstantManager.KEY_LIVETV_PVR_STATUS, pvrStatus);
                } else {
                    intent.putExtra(ConstantManager.KEY_LIVETV_PVR_STATUS, "");
                }
                intent.setClassName(DataMananer.KEY_PACKAGE_NAME, DataMananer.KEY_ACTIVITY_DVBS);
                startActivityForResult(intent, REQUEST_CODE_START_DVBS_ACTIVITY);
                mDataMananer.saveIntParameters(DataMananer.KEY_SELECT_SEARCH_ACTIVITY, DataMananer.SELECT_DVBS);
                Log.d(TAG, "select_dvbs inputId = " + inputId);
            }
        });
        Button isdbt = (Button)findViewById(R.id.select_isdbt);
        isdbt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String pvrFlag = PvrStatusConfirmManager.read(DtvkitDvbScanSelect.this, PvrStatusConfirmManager.KEY_PVR_CLEAR_FLAG);
                if (pvrStatus != null && PvrStatusConfirmManager.KEY_PVR_CLEAR_FLAG_FIRST.equals(pvrFlag)) {
                    intent.putExtra(ConstantManager.KEY_LIVETV_PVR_STATUS, pvrStatus);
                } else {
                    intent.putExtra(ConstantManager.KEY_LIVETV_PVR_STATUS, "");
                }
                intent.setClassName(DataMananer.KEY_PACKAGE_NAME, DataMananer.KEY_ACTIVITY_ISDBT);
                intent.putExtra(DataMananer.KEY_IS_DVBT, true);
                startActivityForResult(intent, REQUEST_CODE_START_ISDBT_ACTIVITY);
                mDataMananer.saveIntParameters(DataMananer.KEY_SELECT_SEARCH_ACTIVITY, DataMananer.SELECT_ISDBT);
                Log.d(TAG, "select_isdbt inputId = " + inputId);
            }
        });
        if (!DtvkitGlueClient.getInstance().isdbtSupport()) {
            Log.d(TAG, "Unsupport isdb-t.");
            isdbt.setVisibility(View.GONE);
        }
        Button settings = (Button)findViewById(R.id.select_setting);
        settings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String pvrFlag = PvrStatusConfirmManager.read(DtvkitDvbScanSelect.this, PvrStatusConfirmManager.KEY_PVR_CLEAR_FLAG);
                if (pvrStatus != null && PvrStatusConfirmManager.KEY_PVR_CLEAR_FLAG_FIRST.equals(pvrFlag)) {
                    intent.putExtra(ConstantManager.KEY_LIVETV_PVR_STATUS, pvrStatus);
                } else {
                    intent.putExtra(ConstantManager.KEY_LIVETV_PVR_STATUS, "");
                }
                intent.setClassName(DataMananer.KEY_PACKAGE_NAME, DataMananer.KEY_ACTIVITY_SETTINGS);
                startActivityForResult(intent, REQUEST_CODE_START_SETTINGS_ACTIVITY);
                mDataMananer.saveIntParameters(DataMananer.KEY_SELECT_SEARCH_ACTIVITY, DataMananer.SELECT_SETTINGS);
                Log.d(TAG, "start to set related language");
            }
        });
        switch (index) {
            case DataMananer.SELECT_DVBC:
                dvbc.requestFocus();
                break;
            case DataMananer.SELECT_DVBT:
                dvbt.requestFocus();
                break;
            case DataMananer.SELECT_DVBS:
                dvbs.requestFocus();
                break;
            case DataMananer.SELECT_ISDBT:
                isdbt.requestFocus();
                break;
            case DataMananer.SELECT_SETTINGS:
                settings.requestFocus();
                break;
            default:
                dvbs.requestFocus();
                break;
        }
    }

    private void showOperatorTypeConfirmDialog(final int tunerType, final Context context, final JSONArray operatorsTypeArray) {
        Log.d(TAG, "showOperatorTypeConfirmDialog");
        final AlertDialog.Builder builder = new AlertDialog.Builder(context);
        final AlertDialog alert = builder.create();
        final View dialogView = View.inflate(context, R.layout.operator_type_setting, null);
        final TextView title = (TextView) dialogView.findViewById(R.id.dialog_title);
        final ListView listView = (ListView) dialogView.findViewById(R.id.dialog_listview);
        final List<HashMap<String, Object>> dataList = new ArrayList<HashMap<String, Object>>();
        if (operatorsTypeArray != null && operatorsTypeArray.length() > 0) {
            JSONObject operTypeObj = null;
            String name = null;
            for (int i = 0; i < operatorsTypeArray.length(); i++) {
                HashMap<String, Object> map = new HashMap<String, Object>();
                try {
                    operTypeObj = operatorsTypeArray.getJSONObject(i);
                    name = operTypeObj.getString("operators_name");
                    map.put("name", name);
                    dataList.add(map);
                } catch (Exception e) {
                    Log.d(TAG, "get operator type data Exception = " + e.getMessage());
                }
            }
        } else {
            Log.d(TAG, "showOperatorTypeConfirmDialog no operatorsTypeArray");
            return;
        }

        SimpleAdapter adapter = new SimpleAdapter(context, dataList,
                R.layout.operator_type_list,
                new String[] {"name"},
                new int[] {R.id.name});

        listView.setAdapter(adapter);
        listView.setSelection(currentIndex);
        title.setText(R.string.operator_type_setting);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, final int position, long id) {
                Log.d(TAG, "showOperatorTypeConfirmDialog onItemClick position = " + position);
                int index = mParameterMananer.getOperatorTypeIndex(tunerType);
                if (index == position) {
                    Log.d(TAG, "showOperatorTypeConfirmDialog same position = " + position);
                    alert.dismiss();
                    return;
                }
                Log.d(TAG, "showOperatorTypeConfirmDialog onItemClick position = " + position);
                mParameterMananer.setOperatorType(tunerType, position);
                currentIndex = position;
                alert.dismiss();
            }
        });
        alert.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                Log.d(TAG, "showOperatorTypeConfirmDialog onDismiss");
            }
        });

        alert.getWindow().setType(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY);
        alert.setView(dialogView);
        alert.show();
        WindowManager.LayoutParams params = alert.getWindow().getAttributes();
        params.width = (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 500, context.getResources().getDisplayMetrics());
        params.height = WindowManager.LayoutParams.WRAP_CONTENT;
        alert.getWindow().setAttributes(params);

    }
}
