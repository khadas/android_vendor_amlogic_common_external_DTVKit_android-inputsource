package com.droidlogic.fragment.dialog;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import java.util.List;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.text.TextUtils;
import android.widget.Toast;

import com.droidlogic.fragment.ItemAdapter.ItemDetail;
import com.droidlogic.fragment.ParameterMananer;
//import com.droidlogic.fragment.R;
import com.droidlogic.fragment.ScanMainActivity;
import com.droidlogic.fragment.LnbWrap;
import com.droidlogic.fragment.SatelliteWrap;

import java.util.LinkedList;
import java.util.Timer;
import java.util.TimerTask;

import org.dtvkit.inputsource.R;

public class CustomDialog/* extends AlertDialog*/ {

    private static final String TAG = "CustomDialog";
    private String mDialogType;
    private String mDialogTitleText;
    private String mDialogKeyText;
    private DialogCallBack mDialogCallBack;
    private Context mContext;
    private ParameterMananer mParameterMananer;
    private AlertDialog mAlertDialog = null;
    private View mDialogView = null;
    private TextView mDialogTitle = null;
    private DialogItemListView mListView = null;
    private ProgressBar mStrengthProgressBar = null;
    private ProgressBar mQualityProgressBar = null;
    private TextView mStrengthTextView = null;
    private TextView mQualityTextView = null;
    private LinkedList<DialogItemAdapter.DialogItemDetail> mItemList = new LinkedList<DialogItemAdapter.DialogItemDetail>();
    private DialogItemAdapter mItemAdapter = null;
    private String mSpinnerValue = null;

    public static final String DIALOG_SAVING = "saving";
    public static final String DIALOG_ADD_TRANSPONDER = "add_transponder";
    public static final String DIALOG_EDIT_TRANSPONDER = "edit_transponder";
    public static final String DIALOG_ADD_SATELLITE = "add_satellite";
    public static final String DIALOG_EDIT_SATELLITE = "edit_satellite";
    public static final String DIALOG_CONFIRM = "confirm";

    public static final String DIALOG_SET_SELECT_SINGLE_ITEM = "select_single_item";
    public static final String DIALOG_SET_EDIT_SWITCH_ITEM = "edit_switch_item";
    public static final String DIALOG_SET_EDIT_ITEM = "edit_item";
    public static final String DIALOG_SET_PROGRESS_ITEM = "progress_item";

    public static final int DIALOG_SET_SELECT_SINGLE_ITEM_SATALLITE = R.string.list_type_satellite;
    public static final int DIALOG_SET_SELECT_SINGLE_ITEM_TRANOPONDER = R.string.list_type_transponder;
    public static final int DIALOG_SET_SELECT_SINGLE_ITEM_LNB_TYPE = R.string.parameter_lnb_type;
    public static final int DIALOG_SET_SELECT_SINGLE_ITEM_UNICABLE = R.string.parameter_unicable;
    public static final int DIALOG_SET_SELECT_SINGLE_ITEM_LNB_POWER = R.string.parameter_lnb_power;
    public static final int DIALOG_SET_SELECT_SINGLE_ITEM_22KHZ = R.string.parameter_22_khz;
    public static final int DIALOG_SET_SELECT_SINGLE_TONE_BURST = R.string.parameter_tone_burst;
    public static final int DIALOG_SET_SELECT_SINGLE_DISEQC1_0 = R.string.parameter_diseqc1_0;
    public static final int DIALOG_SET_SELECT_SINGLE_DISEQC1_1 = R.string.parameter_diseqc1_1;
    public static final int DIALOG_SET_SELECT_SINGLE_MOTOR = R.string.parameter_motor;
    public static final int[] ID_DIALOG_TITLE_COLLECTOR = {DIALOG_SET_SELECT_SINGLE_ITEM_SATALLITE, DIALOG_SET_SELECT_SINGLE_ITEM_LNB_TYPE, DIALOG_SET_SELECT_SINGLE_ITEM_UNICABLE,
            DIALOG_SET_SELECT_SINGLE_ITEM_LNB_POWER, DIALOG_SET_SELECT_SINGLE_ITEM_22KHZ, DIALOG_SET_SELECT_SINGLE_TONE_BURST, DIALOG_SET_SELECT_SINGLE_DISEQC1_0,
            DIALOG_SET_SELECT_SINGLE_DISEQC1_1, DIALOG_SET_SELECT_SINGLE_MOTOR};
    //public static final String[] DIALOG_SET_SELECT_SINGLE_ITEM_LNB_TYPE_LIST = {"5150", "9750/10600", "Customize"};
    public static final int[] DIALOG_SET_SELECT_SINGLE_ITEM_LNB_TYPE_LIST = {R.string.parameter_lnb_type_5150, R.string.parameter_lnb_type_9750, R.string.parameter_lnb_custom};
    //public static final String[] DIALOG_SET_SELECT_SINGLE_ITEM_LNB_CUSTOM_TYPE_LIST = {"first freq", "sencond freg"};
    public static final int[] DIALOG_SET_SELECT_SINGLE_ITEM_LNB_CUSTOM_TYPE_LIST = {R.string.parameter_lnb_custom_frequency1, R.string.parameter_lnb_custom_frequency2};
    //public static final String[] DIALOG_SET_SELECT_SINGLE_ITEM_UNICABLE_LIST = {"off", "on"};
    public static final int[] DIALOG_SET_SELECT_SINGLE_ITEM_UNICABLE_LIST = {R.string.parameter_unicable_switch_off, R.string.parameter_unicable_switch_on};
    public static final String[] DIALOG_SET_SELECT_SINGLE_ITEM_UNICABLE_BAND = {"0", "1", "2", "3", "4", "5", "6", "7"};
    //public static final String[] DIALOG_SET_SELECT_SINGLE_ITEM_UNICABLE_POSITION = {"disable", "enable"};
    public static final int[] DIALOG_SET_SELECT_SINGLE_ITEM_UNICABLE_POSITION = {R.string.parameter_position_disable, R.string.parameter_position_enable};
    public static final String[] DIALOG_SET_SELECT_SINGLE_ITEM_LNB_POWER_LIST = {/*"13V", "18V",*/"off", "on"/*, "13/18V"*/};
    //public static final String[] DIALOG_SET_SELECT_SINGLE_ITEM_22KHZ_LIST = {"on", "off"/*, "auto"*/};
    public static final int[] DIALOG_SET_SELECT_SINGLE_ITEM_22KHZ_LIST = {R.string.parameter_unicable_switch_off, R.string.parameter_unicable_switch_on};
    public static final String[] DIALOG_SET_SELECT_SINGLE_ITEM_TONE_BURST_LIST = {"none", "a", "b"};
    public static final String[] DIALOG_SET_SELECT_SINGLE_ITEM_DISEQC1_0_LIST = {"none", "1/4", "2/4", "3/4", "4/4"};
    public static final String[] DIALOG_SET_SELECT_SINGLE_ITEM_DISEQC1_1_LIST = {"none", "1/16", "2/16", "3/16", "4/16", "5/16", "6/16", "7/16", "8/16", "9/16", "10/16", "11/16", "12/16", "13/16", "14/16", "15/16", "16/16"};
    public static final String[] DIALOG_SET_SELECT_SINGLE_ITEM_MOTOR_LIST = {"none", "DiSEqc1.2", "DiSEqc1.3"};

    public static final int DIALOG_SET_SELECT_SINGLE_ITEM_DISEQC1_2_LIST_DISH_LIMITS = R.string.parameter_diseqc1_2_dish_limits_status;
    public static final int[] DIALOG_SET_SELECT_SINGLE_ITEM_DISEQC1_2_LIST_DISH_LIMITS_LIST = {R.string.parameter_unicable_switch_off, R.string.parameter_unicable_switch_on};
    public static final int DIALOG_SET_SELECT_SINGLE_ITEM_DISEQC1_2_LIST_SET_EAST_DISH_LIMITS = R.string.parameter_diseqc1_2_dish_limits_east;
    public static final int DIALOG_SET_SELECT_SINGLE_ITEM_DISEQC1_2_LIST_SET_WEST_DISH_LIMITS = R.string.parameter_diseqc1_2_dish_limits_west;
    public static final int DIALOG_SET_SELECT_SINGLE_ITEM_DISEQC1_2_LIST_DIRECTTION = R.string.parameter_diseqc1_2_move_direction;
    public static final int[] DIALOG_SET_SELECT_SINGLE_ITEM_DISEQC1_2_LIST_DIRECTTION_LIST = {R.string.parameter_diseqc1_2_move_direction_east, R.string.parameter_diseqc1_2_move_direction_center, R.string.parameter_diseqc1_2_move_direction_west};
    public static final int DIALOG_SET_SELECT_SINGLE_ITEM_DISEQC1_2_LIST_STEP = R.string.parameter_diseqc1_2_move_step;
    public static final int DIALOG_SET_SELECT_SINGLE_ITEM_DISEQC1_2_LIST_MOVE = R.string.parameter_diseqc1_2_move;
    public static final int DIALOG_SET_SELECT_SINGLE_ITEM_DISEQC1_2_LIST_POSITION = R.string.parameter_diseqc1_2_current_position;
    public static final int DIALOG_SET_SELECT_SINGLE_ITEM_DISEQC1_2_LIST_SAVE_TO_POSITION = R.string.parameter_diseqc1_2_save_to_position;
    public static final int DIALOG_SET_SELECT_SINGLE_ITEM_DISEQC1_2_LIST_MOVE_TO_POSITION = R.string.parameter_diseqc1_2_move_to_position;
    public static final int DIALOG_SET_SELECT_SINGLE_ITEM_DISEQC1_2_LIST_STRENGTH = R.string.dialog_diseqc_1_2_strength;
    public static final int DIALOG_SET_SELECT_SINGLE_ITEM_DISEQC1_2_LIST_QUALITY = R.string.dialog_diseqc_1_2_quality;
    public static final int DIALOG_SET_SELECT_SINGLE_ITEM_DISEQC1_2_SAVE = R.string.dialog_save;
    public static final int DIALOG_SET_SELECT_SINGLE_ITEM_DISEQC1_2_SCAN= R.string.dialog_scan;

    public static final /*String*/int[] DIALOG_SET_SELECT_SINGLE_ITEM_DISEQC1_2_LIST = {DIALOG_SET_SELECT_SINGLE_ITEM_SATALLITE, DIALOG_SET_SELECT_SINGLE_ITEM_TRANOPONDER, DIALOG_SET_SELECT_SINGLE_ITEM_DISEQC1_2_LIST_DISH_LIMITS,
            DIALOG_SET_SELECT_SINGLE_ITEM_DISEQC1_2_LIST_SET_EAST_DISH_LIMITS, DIALOG_SET_SELECT_SINGLE_ITEM_DISEQC1_2_LIST_SET_WEST_DISH_LIMITS, DIALOG_SET_SELECT_SINGLE_ITEM_DISEQC1_2_LIST_DIRECTTION,
            DIALOG_SET_SELECT_SINGLE_ITEM_DISEQC1_2_LIST_STEP, DIALOG_SET_SELECT_SINGLE_ITEM_DISEQC1_2_LIST_MOVE, DIALOG_SET_SELECT_SINGLE_ITEM_DISEQC1_2_LIST_POSITION,
            DIALOG_SET_SELECT_SINGLE_ITEM_DISEQC1_2_LIST_SAVE_TO_POSITION, DIALOG_SET_SELECT_SINGLE_ITEM_DISEQC1_2_LIST_MOVE_TO_POSITION, DIALOG_SET_SELECT_SINGLE_ITEM_DISEQC1_2_LIST_STRENGTH,
            DIALOG_SET_SELECT_SINGLE_ITEM_DISEQC1_2_LIST_QUALITY, DIALOG_SET_SELECT_SINGLE_ITEM_DISEQC1_2_SAVE, DIALOG_SET_SELECT_SINGLE_ITEM_DISEQC1_2_SCAN
    };

    public static final int DIALOG_SET_EDIT_SWITCH_ITEM_UNICABLE = R.string.parameter_unicable;
    public static final int DIALOG_SET_EDIT_SWITCH_ITEM_UNICABLE_SWITCH = R.string.parameter_unicable_switch;
    public static final int DIALOG_SET_EDIT_SWITCH_ITEM_USER_BAND = R.string.parameter_unicable_user_band;
    public static final int DIALOG_SET_EDIT_SWITCH_ITEM_UB_FREQUENCY = R.string.parameter_ub_frequency;
    public static final int DIALOG_SET_EDIT_SWITCH_ITEM_POSITION = R.string.parameter_position;
    public static final int[] DIALOG_SET_EDIT_SWITCH_ITEM_UNICABLE_LIST = {DIALOG_SET_EDIT_SWITCH_ITEM_UNICABLE_SWITCH, DIALOG_SET_EDIT_SWITCH_ITEM_USER_BAND, DIALOG_SET_EDIT_SWITCH_ITEM_UB_FREQUENCY, DIALOG_SET_EDIT_SWITCH_ITEM_POSITION};
    public static final int[] DIALOG_SET_EDIT_SWITCH_ITEM_UNICABLE_SWITCH_LIST = {R.string.parameter_unicable_switch_off, R.string.parameter_unicable_switch_on};
    public static final String[] DIALOG_SET_EDIT_SWITCH_ITEM_UNICABLE_USER_BAND_LIST = {"0", "1", "2", "3", "4", "5", "6", "7"};
    public static final int[] DIALOG_SET_EDIT_SWITCH_ITEM_UNICABLE_USER_BAND_FREQUENCY_LIST = {1284, 1400, 1516, 1632, 1748, 1864, 1980, 2096};
    public static final int[] DIALOG_SET_EDIT_SWITCH_ITEM_UNICABLE_POSITION_LIST = {R.string.parameter_unicable_switch_off, R.string.parameter_unicable_switch_on};

    public CustomDialog(Context context, String type, DialogCallBack callback, ParameterMananer mananer) {
        //super(context);
        this.mContext = context;
        this.mDialogType = type;
        this.mDialogCallBack = callback;
        this.mParameterMananer = mananer;
    }

    public interface DialogUiCallBack {
        void onStatusChange(View view, String dialogtype, Bundle data);
    }

    /*public boolean onKeyDown(int keyCode, KeyEvent event) {

        return super.onKeyDown(keyCode, event);
    }*/

    public String getDialogType() {
        return mDialogType;
    }

    public String getDialogKey() {
        return mDialogKeyText;
    }

    public String getDialogTitle() {
        return mDialogTitleText;
    }

    private LinkedList<DialogItemAdapter.DialogItemDetail> buildLnbItem(int select) {
        LinkedList<DialogItemAdapter.DialogItemDetail> items = new LinkedList<DialogItemAdapter.DialogItemDetail>();
        DialogItemAdapter.DialogItemDetail item = null;
        for (int i = 0; i < DIALOG_SET_SELECT_SINGLE_ITEM_LNB_TYPE_LIST.length; i++) {
            boolean needSelect = select == i ? true : false;
            item = new DialogItemAdapter.DialogItemDetail(DialogItemAdapter.DialogItemDetail.ITEM_SELECT, mContext.getString(DIALOG_SET_SELECT_SINGLE_ITEM_LNB_TYPE_LIST[i]), "", needSelect);
            items.add(item);
        }
        return items;
    }

    private LinkedList<DialogItemAdapter.DialogItemDetail> buildUnicableItem() {
        LinkedList<DialogItemAdapter.DialogItemDetail> items = new LinkedList<DialogItemAdapter.DialogItemDetail>();
        DialogItemAdapter.DialogItemDetail item = null;
        int channel = mParameterMananer.getDvbsParaManager().getCurrentLnbParaIntValue("unicable_chan");
        if (channel > (DIALOG_SET_EDIT_SWITCH_ITEM_UNICABLE_USER_BAND_FREQUENCY_LIST.length - 1)) {
            channel = 0;
        }
        for (int i = 0; i < DIALOG_SET_EDIT_SWITCH_ITEM_UNICABLE_LIST.length; i++) {
            switch (i) {
                case 0: {
                    int unicable = mParameterMananer.getDvbsParaManager().getCurrentLnbParaIntValue("unicable");
                    item = new DialogItemAdapter.DialogItemDetail(DialogItemAdapter.DialogItemDetail.ITEM_EDIT_SWITCH,
                            mContext.getString(DIALOG_SET_EDIT_SWITCH_ITEM_UNICABLE_LIST[i]),
                            mContext.getString(DIALOG_SET_SELECT_SINGLE_ITEM_UNICABLE_LIST[unicable]), false);
                    break;
                }
                case 1:
                    item = new DialogItemAdapter.DialogItemDetail(DialogItemAdapter.DialogItemDetail.ITEM_EDIT_SWITCH,
                            mContext.getString(DIALOG_SET_EDIT_SWITCH_ITEM_UNICABLE_LIST[i]), channel + "", false);
                    break;
                case 2://need to type in frequency
                    item = new DialogItemAdapter.DialogItemDetail(DialogItemAdapter.DialogItemDetail.ITEM_DISPLAY,
                            mContext.getString(DIALOG_SET_EDIT_SWITCH_ITEM_UNICABLE_LIST[i]),
                            DIALOG_SET_EDIT_SWITCH_ITEM_UNICABLE_USER_BAND_FREQUENCY_LIST[channel] + "MHz", false);
                    break;
                case 3: {
                    int position = mParameterMananer.getDvbsParaManager().getCurrentLnbParaIntValue("unicable_position_b");
                    item = new DialogItemAdapter.DialogItemDetail(DialogItemAdapter.DialogItemDetail.ITEM_EDIT_SWITCH,
                            mContext.getString(DIALOG_SET_EDIT_SWITCH_ITEM_UNICABLE_LIST[i]),
                            mContext.getString(DIALOG_SET_SELECT_SINGLE_ITEM_UNICABLE_POSITION[position]), false);
                    break;
                }
                default:
                    break;
            }
            items.add(item);
        }
        return items;
    }

    private LinkedList<DialogItemAdapter.DialogItemDetail> buildLnbPowerItem(int select) {
        LinkedList<DialogItemAdapter.DialogItemDetail> items = new LinkedList<DialogItemAdapter.DialogItemDetail>();
        DialogItemAdapter.DialogItemDetail item = null;
        for (int i = 0; i < DIALOG_SET_SELECT_SINGLE_ITEM_LNB_POWER_LIST.length; i++) {
            boolean needSelect = select == i ? true : false;
            item = new DialogItemAdapter.DialogItemDetail(DialogItemAdapter.DialogItemDetail.ITEM_SELECT, DIALOG_SET_SELECT_SINGLE_ITEM_LNB_POWER_LIST[i], "", needSelect);
            items.add(item);
        }
        return items;
    }

    private LinkedList<DialogItemAdapter.DialogItemDetail> build22KhzItem(int select) {
        LinkedList<DialogItemAdapter.DialogItemDetail> items = new LinkedList<DialogItemAdapter.DialogItemDetail>();
        DialogItemAdapter.DialogItemDetail item = null;
        for (int i = 0; i < DIALOG_SET_SELECT_SINGLE_ITEM_22KHZ_LIST.length; i++) {
            boolean needSelect = select == i ? true : false;
            item = new DialogItemAdapter.DialogItemDetail(DialogItemAdapter.DialogItemDetail.ITEM_SELECT, mContext.getString(DIALOG_SET_SELECT_SINGLE_ITEM_22KHZ_LIST[i]), "", needSelect);
            items.add(item);
        }
        return items;
    }

    private LinkedList<DialogItemAdapter.DialogItemDetail> buildToneBurstItem(int select) {
        LinkedList<DialogItemAdapter.DialogItemDetail> items = new LinkedList<DialogItemAdapter.DialogItemDetail>();
        DialogItemAdapter.DialogItemDetail item = null;
        for (int i = 0; i < DIALOG_SET_SELECT_SINGLE_ITEM_TONE_BURST_LIST.length; i++) {
            boolean needSelect = select == i ? true : false;
            item = new DialogItemAdapter.DialogItemDetail(DialogItemAdapter.DialogItemDetail.ITEM_SELECT, DIALOG_SET_SELECT_SINGLE_ITEM_TONE_BURST_LIST[i], "", needSelect);
            items.add(item);
        }
        return items;
    }

    private LinkedList<DialogItemAdapter.DialogItemDetail> buildDiseqc1_0_Item(int select) {
        LinkedList<DialogItemAdapter.DialogItemDetail> items = new LinkedList<DialogItemAdapter.DialogItemDetail>();
        DialogItemAdapter.DialogItemDetail item = null;
        for (int i = 0; i < DIALOG_SET_SELECT_SINGLE_ITEM_DISEQC1_0_LIST.length; i++) {
            boolean needSelect = select == i ? true : false;
            item = new DialogItemAdapter.DialogItemDetail(DialogItemAdapter.DialogItemDetail.ITEM_SELECT, DIALOG_SET_SELECT_SINGLE_ITEM_DISEQC1_0_LIST[i], "", needSelect);
            items.add(item);
        }
        return items;
    }

    private LinkedList<DialogItemAdapter.DialogItemDetail> buildDiseqc1_1_Item(int select) {
        LinkedList<DialogItemAdapter.DialogItemDetail> items = new LinkedList<DialogItemAdapter.DialogItemDetail>();
        DialogItemAdapter.DialogItemDetail item = null;
        for (int i = 0; i < DIALOG_SET_SELECT_SINGLE_ITEM_DISEQC1_1_LIST.length; i++) {
            boolean needSelect = select == i ? true : false;
            item = new DialogItemAdapter.DialogItemDetail(DialogItemAdapter.DialogItemDetail.ITEM_SELECT, DIALOG_SET_SELECT_SINGLE_ITEM_DISEQC1_1_LIST[i], "", needSelect);
            items.add(item);
        }
        return items;
    }

    private LinkedList<DialogItemAdapter.DialogItemDetail> buildMotorItem(int select) {
        LinkedList<DialogItemAdapter.DialogItemDetail> items = new LinkedList<DialogItemAdapter.DialogItemDetail>();
        DialogItemAdapter.DialogItemDetail item = null;
        for (int i = 0; i < DIALOG_SET_SELECT_SINGLE_ITEM_MOTOR_LIST.length; i++) {
            boolean needSelect = select == i ? true : false;
            item = new DialogItemAdapter.DialogItemDetail(DialogItemAdapter.DialogItemDetail.ITEM_SELECT, DIALOG_SET_SELECT_SINGLE_ITEM_MOTOR_LIST[i], "", needSelect);
            items.add(item);
        }
        return items;
    }

    private LinkedList<DialogItemAdapter.DialogItemDetail> getSelectSingleItemsByKey(String title, String key, int select) {
        LinkedList<DialogItemAdapter.DialogItemDetail> items = new LinkedList<DialogItemAdapter.DialogItemDetail>();
        switch (key) {
            case ParameterMananer.KEY_LNB_TYPE:
                items.addAll(buildLnbItem(select));
                break;
            case ParameterMananer.KEY_UNICABLE:
                items.addAll(buildUnicableItem());
                break;
            case ParameterMananer.KEY_LNB_POWER:
                items.addAll(buildLnbPowerItem(select));
                break;
            case ParameterMananer.KEY_22_KHZ:
                items.addAll(build22KhzItem(select));
                break;
            case ParameterMananer.KEY_TONE_BURST:
                items.addAll(buildToneBurstItem(select));
                break;
            case ParameterMananer.KEY_DISEQC1_0:
                items.addAll(buildDiseqc1_0_Item(select));
                break;
            case ParameterMananer.KEY_DISEQC1_1:
                items.addAll(buildDiseqc1_1_Item(select));
                break;
            case ParameterMananer.KEY_MOTOR:
                items.addAll(buildMotorItem(select));
                break;
            default:
                break;
        }
        return items;
    }

    public void initListView(String title, String key, int select) {
        initListDialog(mContext);
        LinkedList<DialogItemAdapter.DialogItemDetail> itemlist = getSelectSingleItemsByKey(title, key, select);
        mItemAdapter = new DialogItemAdapter(itemlist, mContext);
        mListView.setAdapter(mItemAdapter);
        if (!ParameterMananer.KEY_UNICABLE_SWITCH.equals(key) && select >= 0 && select < itemlist.size()) {
            mListView.setSelection(select);
        } else {
            mListView.setSelection(0);
        }
        mDialogTitle.setText(title);
        mDialogTitleText = title;
        mListView.setKey(key);
        mDialogKeyText = key;
        mListView.setDialogCallBack(mDialogCallBack);
        mAlertDialog.setView(mDialogView);
    }

    public void updateListView(String title, String key, int select) {
        LinkedList<DialogItemAdapter.DialogItemDetail> itemlist = getSelectSingleItemsByKey(title, key, select);
        if (select >= 0 && select < itemlist.size()) {
            mListView.setSelection(select);
        } else {
            mListView.setSelection(0);
        }
        mItemAdapter.reFill(itemlist);
    }

    private void initListDialog(Context context) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        mAlertDialog = builder.create();
        mDialogView = View.inflate(mContext, R.layout.select_single_item_dialog, null);
        mDialogTitle = (TextView) mDialogView.findViewById(R.id.dialog_title);
        mListView = (DialogItemListView) mDialogView.findViewById(R.id.select_single_item_listview);
        mAlertDialog.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);
    }

    public void showDialog() {
        if (mAlertDialog != null && !mAlertDialog.isShowing()) {
            mAlertDialog.show();
        }
    }

    public boolean isShowing() {
        return mAlertDialog != null && mAlertDialog.isShowing();
    }

    public void dismissDialog() {
        if (mAlertDialog != null && mAlertDialog.isShowing()) {
            mAlertDialog.dismiss();
        }
    }

    public void initLnbCustomedItemDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        mAlertDialog = builder.create();
        mDialogView = View.inflate(mContext, R.layout.set_custom_lnb, null);
        mDialogTitle = (TextView) mDialogView.findViewById(R.id.dialog_title);

        mDialogTitle.setText(DIALOG_SET_SELECT_SINGLE_ITEM_LNB_TYPE_LIST[2]);
        final EditText editText1 = (EditText)mDialogView.findViewById(R.id.edittext_frequency1);
        final EditText lowMineditText = (EditText)mDialogView.findViewById(R.id.edittext_low_min_frequency);
        final EditText lowMaxeditText = (EditText)mDialogView.findViewById(R.id.edittext_low_max_frequency);
        final EditText editText2 = (EditText)mDialogView.findViewById(R.id.edittext_frequency2);
        final EditText highMineditText = (EditText)mDialogView.findViewById(R.id.edittext_high_min_frequency);
        final EditText highMaxeditText = (EditText)mDialogView.findViewById(R.id.edittext_high_max_frequency);
        final LinearLayout lowBandContainer = (LinearLayout)mDialogView.findViewById(R.id.low_band_container);
        final LinearLayout highBandContainer = (LinearLayout)mDialogView.findViewById(R.id.high_band_container);
        final Spinner lowHighBandSpinner = (Spinner)mDialogView.findViewById(R.id.spinner_band_type);

        String lnb = mParameterMananer.getDvbsParaManager().getCurrentLnbId();
        int lowBandMin = mParameterMananer.getDvbsParaManager().getLnbWrap().getLnbById(lnb).getLnbInfo().lowMin();
        int lowBandMax = mParameterMananer.getDvbsParaManager().getLnbWrap().getLnbById(lnb).getLnbInfo().lowMax();
        int highBandMin = mParameterMananer.getDvbsParaManager().getLnbWrap().getLnbById(lnb).getLnbInfo().highMin();
        int highBandMax = mParameterMananer.getDvbsParaManager().getLnbWrap().getLnbById(lnb).getLnbInfo().highMax();
        int lowLocalIf = mParameterMananer.getDvbsParaManager().getLnbWrap().getLnbById(lnb).getLnbInfo().lowLocalFreq();
        int highLocalIf = mParameterMananer.getDvbsParaManager().getLnbWrap().getLnbById(lnb).getLnbInfo().highLocalFreq();
        boolean customSingle = (lowLocalIf == 0 || highLocalIf == 0);
        int selection = customSingle ? 0 : 1;

        editText1.setText("" + lowLocalIf);
        editText2.setText("" + highLocalIf);
        lowMineditText.setText("" + lowBandMin);
        lowMaxeditText.setText("" + lowBandMax);
        highMineditText.setText("" + highBandMin);
        highMaxeditText.setText("" + highBandMax);

        if (customSingle) {
            highBandContainer.setVisibility(View.GONE);
        } else {
            highBandContainer.setVisibility(View.VISIBLE);
        }

        lowHighBandSpinner.setSelection(selection);
        lowHighBandSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position == ParameterMananer.DEFAULT_LNB_CUSTOM_SINGLE_DOUBLE) {
                    highBandContainer.setVisibility(View.GONE);
                } else {
                    highBandContainer.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        final Button ok = (Button) mDialogView.findViewById(R.id.button1);
        final Button cancel = (Button) mDialogView.findViewById(R.id.button2);

        ok.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mDialogCallBack != null) {
                    Bundle bundle = new Bundle();
                    bundle.putString("action", "onClick");
                    bundle.putString("key", ParameterMananer.KEY_LNB_CUSTOM);
                    bundle.putString("button", "ok");
                    String lowLocal = (!TextUtils.isEmpty(editText1.getText()) ? editText1.getText().toString() : "0");
                    String highLocal = (!TextUtils.isEmpty(editText2.getText()) ? editText2.getText().toString() : "0");
                    String lowMinstr = (!TextUtils.isEmpty(lowMineditText.getText()) ? lowMineditText.getText().toString() : "0");
                    String lowMaxstr = (!TextUtils.isEmpty(lowMaxeditText.getText()) ? lowMaxeditText.getText().toString() : "0");
                    String highMaxstr = (!TextUtils.isEmpty(highMaxeditText.getText()) ? highMaxeditText.getText().toString() : "0");
                    String highMinstr = (!TextUtils.isEmpty(highMineditText.getText()) ? highMineditText.getText().toString() : "0");
                    Log.d(TAG, "initLnbCustomedItemDialog lowMinstr = " + lowMinstr + ", lowMaxstr = " + lowMaxstr + ",highMinstr = " + highMinstr + ", highMaxstr = " + highMaxstr);
                    if ((TextUtils.isEmpty(editText1.getText()) || TextUtils.isEmpty(lowMineditText.getText()) || TextUtils.isEmpty(lowMaxeditText.getText())) ||
                            (!customSingle && (TextUtils.isEmpty(editText2.getText()) || TextUtils.isEmpty(highMineditText.getText()) || TextUtils.isEmpty(highMaxeditText.getText())))) {
                        Toast.makeText(mContext, R.string.dialog_parameter_not_complete, Toast.LENGTH_SHORT).show();
                        return;
                    }
                    bundle.putInt("lowmin", Integer.parseInt(lowMinstr));
                    bundle.putInt("lowmax", Integer.parseInt(lowMaxstr));
                    bundle.putInt("lowlocal", Integer.parseInt(lowLocal));
                    if (lowHighBandSpinner.getSelectedItemPosition() > 0 ) {
                        bundle.putInt("highmin", Integer.parseInt(highMinstr));
                        bundle.putInt("highmax", Integer.parseInt(highMaxstr));
                        bundle.putInt("highlocal", Integer.parseInt(highLocal));
                    } else {
                        bundle.putInt("highlocal", 0);
                    }
                    mDialogCallBack.onStatusChange(ok, ParameterMananer.KEY_LNB_CUSTOM, bundle);
                    if (mAlertDialog != null) {
                        mAlertDialog.dismiss();
                    }
                }
            }
        });
        cancel.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mAlertDialog != null) {
                    mAlertDialog.dismiss();
                }
            }
        });

        String value = mParameterMananer.getStringParameters(ParameterMananer.KEY_LNB_CUSTOM);
        String[] resultValue = {"", ""};
        if (value != null) {
            String[] allvalue = value.split(",");
            if (allvalue != null && allvalue.length > 0 && allvalue.length <= 2) {
                for (int i = 0; i < allvalue.length; i++) {
                    resultValue[i] = allvalue[i];
                }
            }
        }

        editText1.setHint(resultValue[0]);
        editText2.setHint(resultValue[1]);
        mAlertDialog.setView(mDialogView);
        mAlertDialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
    }

    public void initDiseqc1_2_ItemDialog(boolean isDiseqc1_3) {
        initDiseqc1_2_ItemDialog(isDiseqc1_3, true);
    }

    public void initDiseqc1_2_ItemDialog(boolean isDiseqc1_3, boolean init) {
        if (init) {
            AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
            mAlertDialog = builder.create();
            mDialogView = View.inflate(mContext, R.layout.set_diseqc_1_2, null);
            mDialogTitle = (TextView) mDialogView.findViewById(R.id.dialog_title);
            mListView = (DialogItemListView) mDialogView.findViewById(R.id.switch_edit_item_list);
            mStrengthProgressBar = (ProgressBar)mDialogView.findViewById(R.id.strength_progressbar);
            mQualityProgressBar = (ProgressBar)mDialogView.findViewById(R.id.quality_progressbar);
            mStrengthTextView = (TextView)mDialogView.findViewById(R.id.strength_percent);
            mQualityTextView = (TextView)mDialogView.findViewById(R.id.quality_percent);
            //update diseqc caches
            mParameterMananer.getDvbsParaManager().setCurrentDiseqcValue("sate_index", 0);
            mParameterMananer.getDvbsParaManager().setCurrentDiseqcValue("tp_index", 0);
            int limitStats = mParameterMananer.getIntParameters(ParameterMananer.KEY_DISEQC1_2_DISH_LIMITS_STATUS);
            mParameterMananer.getDvbsParaManager().setCurrentDiseqcValue("dishlimit_state", limitStats);
            int dishDirection = mParameterMananer.getIntParameters(ParameterMananer.KEY_DISEQC1_2_DISH_MOVE_DIRECTION);
            mParameterMananer.getDvbsParaManager().setCurrentDiseqcValue("dish_dir", dishDirection);
            int dishStep = mParameterMananer.getIntParameters(ParameterMananer.KEY_DISEQC1_2_DISH_MOVE_STEP);
            mParameterMananer.getDvbsParaManager().setCurrentDiseqcValue("dish_step", dishStep);
            List<String> satelist = mParameterMananer.getDvbsParaManager().getSatelliteNameListSelected();
            String sateName = satelist.get(0);
            int dish_Pos = mParameterMananer.getDvbsParaManager().getSatelliteWrap().getSatelliteByName(sateName).getDishPos();
            mParameterMananer.getDvbsParaManager().setCurrentDiseqcValue("dish_pos", dish_Pos);
            String diseqcLocation = mParameterMananer.getStringParameters(ParameterMananer.KEY_DISEQC1_3_LOCATION_STRING);
            int diseqcLocationIndex = mParameterMananer.getDvbsParaManager().getDisEqcLocationIndex(diseqcLocation);
            mParameterMananer.getDvbsParaManager().setCurrentDiseqcValue("diseqc_location", diseqcLocationIndex);
        }
        final TimerTask task = new TimerTask() {
            public void run() {
                ((ScanMainActivity)mContext).runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        int strength = mParameterMananer.getStrengthStatus();
                        int quality = mParameterMananer.getQualityStatus();
                        if (mStrengthProgressBar != null && mQualityProgressBar != null &&
                                mStrengthTextView != null && mQualityTextView != null) {
                            mStrengthProgressBar.setProgress(strength);
                            mQualityProgressBar.setProgress(quality);
                            mStrengthTextView.setText(strength + "%");
                            mQualityTextView.setText(quality + "%");
                            //Log.d(TAG, "run task get strength and quality");
                        }
                    }
                });
            }
        };
        final Timer timer = new Timer();

        mStrengthProgressBar.setProgress(mParameterMananer.getStrengthStatus());
        mQualityProgressBar.setProgress(mParameterMananer.getQualityStatus());
        mStrengthTextView.setText(mParameterMananer.getStrengthStatus() + "%");
        mQualityTextView.setText(mParameterMananer.getQualityStatus() + "%");
        //mListView.setSelection(mParameterMananer.getIntParameters(ParameterMananer.KEY_DISEQC1_2));

        LinkedList<DialogItemAdapter.DialogItemDetail> itemlist = new LinkedList<DialogItemAdapter.DialogItemDetail>();

        for (int i = 0; i < DIALOG_SET_SELECT_SINGLE_ITEM_DISEQC1_2_LIST.length; i++) {
            int type = DialogItemAdapter.DialogItemDetail.ITEM_EDIT_SWITCH;
            String title = "";
            String value = "";
            boolean isSelect = false;
            switch (i) {
                case 0: {
                    List<String> satelist = mParameterMananer.getDvbsParaManager().getSatelliteNameListSelected();
                    int pos = mParameterMananer.getDvbsParaManager().getCurrentDiseqcValue("sate_index");
                    if (pos > satelist.size() -1) {
                        pos = 0;
                        mParameterMananer.getDvbsParaManager().setCurrentDiseqcValue("sate_index", pos);
                    }
                    value = satelist.get(pos);
                    break;
                }
                case 1: {
                    List<String> satelist = mParameterMananer.getDvbsParaManager().getSatelliteNameListSelected();
                    int satePos = mParameterMananer.getDvbsParaManager().getCurrentDiseqcValue("sate_index");
                    String sateName = satelist.get(satePos);
                    LinkedList<ItemDetail> tps = mParameterMananer.getDvbsParaManager().getTransponderList(sateName);
                    int tpPos = mParameterMananer.getDvbsParaManager().getCurrentDiseqcValue("tp_index");
                    if (tpPos > tps.size() -1) {
                        tpPos = 0;
                        mParameterMananer.getDvbsParaManager().setCurrentDiseqcValue("tp_index", tpPos);
                    }
                    value = tps.get(tpPos).getFirstText();
                    break;
                }
                case 2: {
                    int dishLimitStat = mParameterMananer.getDvbsParaManager().getCurrentDiseqcValue("dishlimit_state");
                    value = mContext.getString(DIALOG_SET_SELECT_SINGLE_ITEM_DISEQC1_2_LIST_DISH_LIMITS_LIST[dishLimitStat]);
                    break;
                }
                case 3: {
                    type = DialogItemAdapter.DialogItemDetail.ITEM_DISPLAY;
                    value = mContext.getString(R.string.parameter_diseqc1_2_press_to_limit_east);
                    break;
                }
                case 4: {
                    type = DialogItemAdapter.DialogItemDetail.ITEM_DISPLAY;
                    value = mContext.getString(R.string.parameter_diseqc1_2_press_to_limit_west);
                    break;
                }
                case 5: {
                    int dishMoveDir = mParameterMananer.getDvbsParaManager().getCurrentDiseqcValue("dish_dir");
                    value = mContext.getString(DIALOG_SET_SELECT_SINGLE_ITEM_DISEQC1_2_LIST_DIRECTTION_LIST[dishMoveDir]);
                    break;
                }
                case 6: {
                    int dishMoveStep = mParameterMananer.getDvbsParaManager().getCurrentDiseqcValue("dish_step");
                    value = String.valueOf(dishMoveStep);
                    break;
                }
                case 7: {
                    type = DialogItemAdapter.DialogItemDetail.ITEM_DISPLAY;
                    value = mContext.getString(R.string.parameter_diseqc1_2_press_to_move);
                    break;
                }
                case 8: {
                    int dish_Pos = mParameterMananer.getDvbsParaManager().getCurrentDiseqcValue("dish_pos");
                    value = String.valueOf(dish_Pos);
                    break;
                }
                case 9: {
                    value = mContext.getString(R.string.parameter_diseqc1_2_press_to_save);
                    type = DialogItemAdapter.DialogItemDetail.ITEM_DISPLAY;
                    break;
                }
                case 10: {
                    type = DialogItemAdapter.DialogItemDetail.ITEM_DISPLAY;
                    value = mContext.getString(R.string.parameter_diseqc1_2_press_to_move);
                    break;
                }
                default:
                    Log.d(TAG, "initDiseqc1_2_ItemDialog unkown key");
                    break;
            }
            if (!TextUtils.isEmpty(value)) {
                DialogItemAdapter.DialogItemDetail item =
                        new DialogItemAdapter.DialogItemDetail(type,
                                mContext.getString(DIALOG_SET_SELECT_SINGLE_ITEM_DISEQC1_2_LIST[i]), value, false);
                itemlist.add(item);
            }
        }
        //add location info for diseqc1.3
        if (isDiseqc1_3) {
            int locationIndex = mParameterMananer.getDvbsParaManager().getCurrentDiseqcValue("diseqc_location");
            List<String> locations = mParameterMananer.getDvbsParaManager().getLnbWrap().getDiseqcLocationNames();
            itemlist.add(new DialogItemAdapter.DialogItemDetail(DialogItemAdapter.DialogItemDetail.ITEM_EDIT_SWITCH,
                    "Location", locations.get(locationIndex), false));
            int locationParamType = DialogItemAdapter.DialogItemDetail.ITEM_DISPLAY;
            if ("manual".equals(locations.get(locationIndex))) {
                locationParamType = DialogItemAdapter.DialogItemDetail.ITEM_EDIT_SWITCH;
            }
            boolean isEast = mParameterMananer.getDvbsParaManager().getLnbWrap()
                    .getLocationInfoByIndex(locationIndex).isLongitudeEast();
            int longitude = mParameterMananer.getDvbsParaManager().getLnbWrap()
                    .getLocationInfoByIndex(locationIndex).getLongitude();
            boolean isNorth = mParameterMananer.getDvbsParaManager().getLnbWrap()
                    .getLocationInfoByIndex(locationIndex).isLatitudeNorth();
            int latitude = mParameterMananer.getDvbsParaManager().getLnbWrap()
                    .getLocationInfoByIndex(locationIndex).getLatitude();
            itemlist.add(new DialogItemAdapter.DialogItemDetail(locationParamType,
                    "Longitude Direction", isEast ? "East" : "West", false));
            itemlist.add(new DialogItemAdapter.DialogItemDetail(locationParamType,
                    "Longitude Angle", "" + longitude, false));
            itemlist.add(new DialogItemAdapter.DialogItemDetail(locationParamType,
                    "Latitude Direction", isNorth ? "North" : "South", false));
            itemlist.add(new DialogItemAdapter.DialogItemDetail(locationParamType,
                    "Latitude Angle", "" + latitude, false));
            itemlist.add(new DialogItemAdapter.DialogItemDetail(DialogItemAdapter.DialogItemDetail.ITEM_DISPLAY,
                    "Action for Location", "Press to take action", false));
        }

        if (init) {
            mItemAdapter = new DialogItemAdapter(itemlist, mContext);
            mListView.setAdapter(mItemAdapter);
            timer.schedule(task, 1000, 1000);
        } else {
            mItemAdapter.reFill(itemlist);
            task.cancel();
            timer.cancel();
            return;
        }

        if (isDiseqc1_3) {
            mDialogTitle.setText(CustomDialog.DIALOG_SET_SELECT_SINGLE_ITEM_MOTOR_LIST[2]);
            mDialogTitleText = CustomDialog.DIALOG_SET_SELECT_SINGLE_ITEM_MOTOR_LIST[2];
        } else {
            mDialogTitle.setText(CustomDialog.DIALOG_SET_SELECT_SINGLE_ITEM_MOTOR_LIST[1]);
            mDialogTitleText = CustomDialog.DIALOG_SET_SELECT_SINGLE_ITEM_MOTOR_LIST[1];
        }
        mListView.setKey(ParameterMananer.KEY_DISEQC1_2);
        mDialogKeyText = ParameterMananer.KEY_DISEQC1_2;
        mListView.setDialogCallBack(mDialogCallBack);

        mAlertDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                mParameterMananer.saveIntParameters(mParameterMananer.KEY_DISEQC1_2_DISH_LIMITS_STATUS,
                        mParameterMananer.getDvbsParaManager().getCurrentDiseqcValue("dishlimit_state"));
                mParameterMananer.saveIntParameters(mParameterMananer.KEY_DISEQC1_2_DISH_MOVE_DIRECTION,
                        mParameterMananer.getDvbsParaManager().getCurrentDiseqcValue("dish_dir"));
                mParameterMananer.saveIntParameters(mParameterMananer.KEY_DISEQC1_2_DISH_MOVE_STEP,
                        mParameterMananer.getDvbsParaManager().getCurrentDiseqcValue("dish_step"));
                int locationIndex = mParameterMananer.getDvbsParaManager().getCurrentDiseqcValue("diseqc_location");
                String locationName = mParameterMananer.getDvbsParaManager().getLnbWrap()
                        .getLocationInfoByIndex(locationIndex).getName();
                mParameterMananer.saveStringParameters(ParameterMananer.KEY_DISEQC1_3_LOCATION_STRING, locationName);
                timer.cancel();
            }
        });

        mAlertDialog.setView(mDialogView);
    }

    public void updateDiseqc1_2_Dialog(boolean isDiseqc1_3) {
        initDiseqc1_2_ItemDialog(isDiseqc1_3, false);
    }

    public CustomDialog creatSavingDialog() {
        //setContentView(R.layout.saving);
        return this;
    }

    public void initAddSatelliteDialog(final String name) {
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        mAlertDialog = builder.create();
        mDialogView = View.inflate(mContext, R.layout.add_satellite, null);
        mDialogTitle = (TextView) mDialogView.findViewById(R.id.dialog_title);
        if (name != null) {
            mDialogTitle.setText(R.string.dialog_edit_satellite);
        } else {
            mDialogTitle.setText(R.string.dialog_add_satellite);
        }
        mSpinnerValue = "";

        final EditText satellite = (EditText)mDialogView.findViewById(R.id.edittext_satellite);
        final Spinner spinner = (Spinner)mDialogView.findViewById(R.id.spinner_direction);
        final EditText longitude = (EditText)mDialogView.findViewById(R.id.edittext_longitude);
        if (name != null) {
            boolean isEast = mParameterMananer.getDvbsParaManager().getSatelliteWrap().getSatelliteByName(name).getDrirection();
            String longitude1 = mParameterMananer.getDvbsParaManager().getSatelliteWrap().getSatelliteByName(name).getLongitude() + "";
            satellite.setHint(name);
            longitude.setHint(longitude1);
            mSpinnerValue = isEast ? "east" : "west";
            spinner.setSelection(isEast ? 0 : 1);
            longitude.setHint(longitude1);
        }
        final Button ok = (Button) mDialogView.findViewById(R.id.button1);
        final Button cancel = (Button) mDialogView.findViewById(R.id.button2);
        final String[] SPINNER_VALUES = {"east", "west"};
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                mSpinnerValue = SPINNER_VALUES[position];
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        ok.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mDialogCallBack != null) {
                    Bundle bundle = new Bundle();
                    bundle.putString("action", "onClick");
                    bundle.putString("key", ParameterMananer.KEY_ADD_SATELLITE);
                    bundle.putString("button", "ok");
                    String value1 = !TextUtils.isEmpty(satellite.getText()) ? satellite.getText().toString() : (satellite.getHint() != null ? satellite.getHint().toString() : "");
                    boolean value2 = "east".equals(mSpinnerValue != null ? mSpinnerValue : "east");
                    String value3 = !TextUtils.isEmpty(longitude.getText()) ? longitude.getText().toString() : (longitude.getHint() != null ? longitude.getHint().toString() : "");
                    //Log.d(TAG, "initAddSatelliteDialog value1 = " + value1 + ", value2 = " + value2 + ", value3 = " + value3);
                    if (TextUtils.isEmpty(value1) || TextUtils.isEmpty(value3)) {
                        Toast.makeText(mContext, R.string.dialog_parameter_not_complete, Toast.LENGTH_SHORT).show();
                        return;
                    }
                    bundle.putString("value1", value1);
                    bundle.putBoolean("value2", value2);
                    bundle.putString("value3", value3);
                    if (name != null) {
                        bundle.putString("value4", name);
                        mDialogCallBack.onStatusChange(ok, ParameterMananer.KEY_EDIT_SATELLITE, bundle);
                    } else {
                        mDialogCallBack.onStatusChange(ok, ParameterMananer.KEY_ADD_SATELLITE, bundle);
                    }
                    if (mAlertDialog != null) {
                        mAlertDialog.dismiss();
                    }
                }
            }
        });
        cancel.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mAlertDialog != null) {
                    mAlertDialog.dismiss();
                }
            }
        });

        mAlertDialog.setView(mDialogView);
    }

    public void initRemoveSatelliteDialog(final String name) {
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        mAlertDialog = builder.create();
        mDialogView = View.inflate(mContext, R.layout.remove_satellite, null);
        mDialogTitle = (TextView) mDialogView.findViewById(R.id.dialog_title);
        mDialogTitle.setText(mContext.getString(R.string.dialog_remove_satellite) + ": " + name);

        final Button ok = (Button) mDialogView.findViewById(R.id.button1);
        final Button cancel = (Button) mDialogView.findViewById(R.id.button2);

        ok.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mDialogCallBack != null) {
                    Bundle bundle = new Bundle();
                    bundle.putString("action", "onClick");
                    bundle.putString("key", ParameterMananer.KEY_REMOVE_SATELLITE);
                    bundle.putString("button", "ok");
                    bundle.putString("value1", name != null ? name : "");
                    mDialogCallBack.onStatusChange(ok, ParameterMananer.KEY_REMOVE_SATELLITE, bundle);
                    if (mAlertDialog != null) {
                        mAlertDialog.dismiss();
                    }
                }
            }
        });
        cancel.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mAlertDialog != null) {
                    mAlertDialog.dismiss();
                }
            }
        });

        mAlertDialog.setView(mDialogView);
    }

    private int getSpinnerIndex(Spinner spinner, String value) {
        int index = 0;
        if (spinner == null) {
            return index;
        }
        for (int i = 0; i < spinner.getCount(); i ++) {
            if (spinner.getItemAtPosition(i).equals(value)){
                index = i;
                break;
            }
        }
        return index;
    }

    public void initAddTransponderDialog(final String parameter) {
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        mAlertDialog = builder.create();
        mDialogView = View.inflate(mContext, R.layout.add_transponder, null);
        mDialogTitle = (TextView) mDialogView.findViewById(R.id.dialog_title);
        if (parameter != null) {
            mDialogTitle.setText(R.string.dialog_edit_transponder);
        } else {
            mDialogTitle.setText(R.string.dialog_add_transponder);
        }
        final TextView satellite = (TextView)mDialogView.findViewById(R.id.edittext_satellite);
        CheckBox dvbs2 = (CheckBox)mDialogView.findViewById(R.id.dvbs2);
        final EditText frequency = (EditText)mDialogView.findViewById(R.id.edittext_frequency);
        final Spinner spinner = (Spinner)mDialogView.findViewById(R.id.spinner_polarity);
        final EditText symbol = (EditText)mDialogView.findViewById(R.id.edittext_symbol);
        Spinner fecmode = (Spinner)mDialogView.findViewById(R.id.fec_mode);
        Spinner modulationmode = (Spinner)mDialogView.findViewById(R.id.modulation_mode);
        final Button ok = (Button) mDialogView.findViewById(R.id.button1);
        final Button cancel = (Button) mDialogView.findViewById(R.id.button2);
        final String[] SPINNER_VALUES = {"H", "V"};

        String satellitename1 = mParameterMananer.getDvbsParaManager().getCurrentSatellite();
        if (parameter != null) {
            String dvbsystem = mParameterMananer.getDvbsParaManager().
                    getSatelliteWrap().getTransponderByName(satellitename1, parameter).getSystem();
            boolean isDvbs2 = ("DVBS2".equals(dvbsystem)) ? true:false;
            dvbs2.setChecked(isDvbs2);
            int freq = mParameterMananer.getDvbsParaManager().
                    getSatelliteWrap().getTransponderByName(satellitename1, parameter).getFreq();
            frequency.setHint("" + freq);
            String polarity = mParameterMananer.getDvbsParaManager().
                    getSatelliteWrap().getTransponderByName(satellitename1, parameter).getPolarity();
            mSpinnerValue = polarity;
            spinner.setSelection("H".equals(polarity) ? 0 : 1);
            int symbol_rate = mParameterMananer.getDvbsParaManager().
                    getSatelliteWrap().getTransponderByName(satellitename1, parameter).getSymbol();
            symbol.setHint("" + symbol_rate);
            String fec_mode = mParameterMananer.getDvbsParaManager().
                    getSatelliteWrap().getTransponderByName(satellitename1, parameter).getFecMode();
            fecmode.setSelection(getSpinnerIndex(fecmode, fec_mode));
            String modulation = mParameterMananer.getDvbsParaManager().
                    getSatelliteWrap().getTransponderByName(satellitename1, parameter).getModulation();
            modulationmode.setSelection(getSpinnerIndex(modulationmode, modulation));
        }
        satellite.setText(satellitename1);

        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                mSpinnerValue = SPINNER_VALUES[position];
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        ok.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mDialogCallBack != null) {
                    Bundle bundle = new Bundle();
                    bundle.putString("action", "onClick");
                    bundle.putString("key", ParameterMananer.KEY_EDIT_TRANSPONDER);
                    bundle.putString("button", "ok");
                    String sate_name = !TextUtils.isEmpty(satellite.getText()) ? satellite.getText().toString() : (satellite.getHint() != null ? satellite.getHint().toString() : "");
                    String freq_edit_str = !TextUtils.isEmpty(frequency.getText()) ? frequency.getText().toString() : (frequency.getHint() != null ? frequency.getHint().toString() : "0");
                    String symbol_edit_str = !TextUtils.isEmpty(symbol.getText()) ? symbol.getText().toString() : (symbol.getHint() != null ? symbol.getHint().toString() : "0");
                    if (TextUtils.isEmpty(sate_name) || TextUtils.isEmpty(freq_edit_str) || TextUtils.isEmpty(symbol_edit_str)) {
                        Toast.makeText(mContext, R.string.dialog_parameter_not_complete, Toast.LENGTH_SHORT).show();
                        return;
                    }
                    int freq_edit = 0;
                    int symbol_edit = 0;
                    try {
                        freq_edit = Integer.parseInt(freq_edit_str);
                        symbol_edit = Integer.parseInt(symbol_edit_str);
                    } catch (Exception e) {
                    }
                    boolean isDvbs2 = dvbs2.isChecked();
                    final String[] fecModes = mContext.getResources().getStringArray(R.array.fec_mode_entries);
                    String fecMode = fecModes[fecmode.getSelectedItemPosition()];
                    final String[] modulations = mContext.getResources().getStringArray(R.array.modulation_mode_entries);
                    String modulation = modulations[fecmode.getSelectedItemPosition()];
                    bundle.putString("satellite", sate_name);
                    bundle.putString("oldName", parameter);
                    bundle.putBoolean("system", isDvbs2);
                    bundle.putInt("frequency", freq_edit);
                    bundle.putString("polarity", mSpinnerValue != null ? mSpinnerValue : "");
                    bundle.putInt("symbol", symbol_edit);
                    bundle.putString("fec", fecMode);
                    bundle.putString("modulation", modulation);

                    if (parameter != null) {
                        mDialogCallBack.onStatusChange(ok, ParameterMananer.KEY_EDIT_TRANSPONDER, bundle);
                    } else {
                        mDialogCallBack.onStatusChange(ok, ParameterMananer.KEY_ADD_TRANSPONDER, bundle);
                    }
                    if (mAlertDialog != null) {
                        mAlertDialog.dismiss();
                    }
                }
            }
        });
        cancel.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mAlertDialog != null) {
                    mAlertDialog.dismiss();
                }
            }
        });

        mAlertDialog.setView(mDialogView);
    }

    public void initRemoveTransponderDialog(final String parameter) {
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        mAlertDialog = builder.create();
        mDialogView = View.inflate(mContext, R.layout.remove_transponder, null);
        mDialogTitle = (TextView) mDialogView.findViewById(R.id.dialog_title);
        mDialogTitle.setText(mContext.getString(R.string.dialog_remove_transponder) + ": " + parameter);

        final Button ok = (Button) mDialogView.findViewById(R.id.button1);
        final Button cancel = (Button) mDialogView.findViewById(R.id.button2);

        ok.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mDialogCallBack != null) {
                    Bundle bundle = new Bundle();
                    bundle.putString("action", "onClick");
                    bundle.putString("key", ParameterMananer.KEY_REMOVE_TRANSPONDER);
                    bundle.putString("button", "ok");
                    bundle.putString("satellite", mParameterMananer.getDvbsParaManager().getCurrentSatellite());
                    bundle.putString("transponder", parameter);
                    mDialogCallBack.onStatusChange(ok, ParameterMananer.KEY_REMOVE_TRANSPONDER, bundle);
                    if (mAlertDialog != null) {
                        mAlertDialog.dismiss();
                    }
                }
            }
        });
        cancel.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mAlertDialog != null) {
                    mAlertDialog.dismiss();
                }
            }
        });

        mAlertDialog.setView(mDialogView);
    }

    public CustomDialog creatEditTransponderDialog() {
        AlertDialog dialog = null;
        AlertDialog.Builder builder = null;

        return this;
    }

    public CustomDialog creatAddSatelliteDialog() {
        AlertDialog dialog = null;
        AlertDialog.Builder builder = null;

        return this;
    }

    public CustomDialog creatEditSatelliteDialog() {
        AlertDialog dialog = null;
        AlertDialog.Builder builder = null;

        return this;
    }

    public AlertDialog creatConfirmDialog() {
        AlertDialog dialog = null;
        AlertDialog.Builder builder = null;
        dialog = (AlertDialog) builder.setPositiveButton(mContext.getString(R.string.dialog_ok), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Bundle bundle = new Bundle();
                bundle.putInt("which", which);
                mDialogCallBack.onStatusChange(null, mDialogType, bundle);
            }
        })
        .setNegativeButton(mContext.getString(R.string.dialog_cancel), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Bundle bundle = new Bundle();
                bundle.putInt("which", which);
                mDialogCallBack.onStatusChange(null, mDialogType, bundle);
            }
        }).create();
        //dialog.setContentView(R.layout.confirm);
        return dialog;
    }
}
