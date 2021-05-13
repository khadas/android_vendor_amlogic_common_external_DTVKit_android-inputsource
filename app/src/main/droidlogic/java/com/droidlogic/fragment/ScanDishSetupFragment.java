package com.droidlogic.fragment;

import java.util.LinkedList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import com.droidlogic.fragment.ItemAdapter.ItemDetail;
import com.droidlogic.fragment.ItemListView.ListItemFocusedListener;
import com.droidlogic.fragment.ItemListView.ListItemSelectedListener;
import com.droidlogic.fragment.ItemListView.ListSwitchedListener;
import com.droidlogic.fragment.ItemListView.ListTypeSwitchedListener;
//import com.droidlogic.fragment.R.color;
import com.droidlogic.fragment.dialog.CustomDialog;
import com.droidlogic.fragment.dialog.DialogCallBack;
import com.droidlogic.fragment.dialog.DialogManager;

import android.app.AlertDialog;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnFocusChangeListener;
import android.view.ViewGroup;
import android.widget.Adapter;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.media.tv.TvInputInfo;
import android.widget.ProgressBar;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.widget.Toast;
import android.content.ComponentName;

import org.dtvkit.companionlibrary.EpgSyncJobService;

import org.dtvkit.inputsource.DtvkitEpgSync;
import org.dtvkit.inputsource.R;
import org.dtvkit.inputsource.R.color;

public class ScanDishSetupFragment extends Fragment {

    private static final String TAG = "ScanDishSetupFragment";
    private ItemAdapter mItemAdapterItem = null;
    private ItemAdapter mItemAdapterOption = null;
    private ItemListView mListViewItem = null;
    private ItemListView mListViewOption = null;
    private LinkedList<ItemDetail> mItemDetailItem = new LinkedList<ItemDetail>();
    private LinkedList<ItemDetail> mItemDetailOption = new LinkedList<ItemDetail>();
    private String mCurrentListType = ParameterMananer.ITEM_LNB;
    private String mCurrentListFocus = ItemListView.LIST_LEFT;
    private ParameterMananer mParameterMananer;
    private LinearLayout mSatelliteQuickkey;
    private LinearLayout mSatelliteQuickkey1;
    private LinearLayout mSatelliteQuickkey2;
    private TextView mItemTitleTextView;
    private TextView mOptionTitleItemTextView;
    private LinearLayout mStrengthContainer;
    private LinearLayout mQualityContainer;
    private ProgressBar mStrengthProgressBar;
    private ProgressBar mQualityProgressBar;
    private TextView mStrengthTextView;
    private TextView mQualityTextView;
    private DialogManager mDialogManager = null;
    private boolean mStartTuneActionSuccessful = false;

    private TimerTask task = new TimerTask() {
        public void run() {
            ((ScanMainActivity)getActivity()).runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (mParameterMananer == null) {
                        return;
                    }
                    int strength = 0;
                    int quality = 0;
                    if (mStartTuneActionSuccessful) {
                        strength = mParameterMananer.getStrengthStatus();
                        quality = mParameterMananer.getQualityStatus();
                    }
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
    private Timer timer = new Timer();
    private boolean mScheduled = false;

    private HandlerThread mHandlerThread;
    private Handler  mThreadHandler;

    private final int MSG_START_TUNE_ACTION = 0;
    private final int MSG_STOP_TUNE_ACTION = 1;
    private final int MSG_STOP_RELEAS_ACTION = 2;
    private final int VALUE_START_TUNE_DELAY = 1200;
    private boolean mIsStarted = false;
    private boolean mIsReleasing = false;

    private void initHandlerThread() {
        mHandlerThread = new HandlerThread("check-message-coming");
        mHandlerThread.start();
        mThreadHandler = new Handler(mHandlerThread.getLooper()) {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case MSG_START_TUNE_ACTION:
                        if (mParameterMananer != null) {
                            mIsStarted = true;
                            if (null == mParameterMananer.startTuneAction()) {
                                Log.d(TAG, "mStartTuneActionSuccessful = false");
                                mStartTuneActionSuccessful = false;
                            } else {
                                Log.d(TAG, "mStartTuneActionSuccessful = true");
                                mStartTuneActionSuccessful = true;
                            }
                        }
                        break;
                    case MSG_STOP_TUNE_ACTION:
                        if (mParameterMananer != null) {
                            mIsStarted = false;
                            mParameterMananer.stopTuneAction();
                        }
                        break;
                    case MSG_STOP_RELEAS_ACTION:
                        if (!mThreadHandler.hasMessages(MSG_STOP_RELEAS_ACTION)) {
                            releaseHandler();
                        } else {
                            releaseMessage();
                        }
                        break;
                    default:
                        break;
                }
            }
        };
    }

    /*public static ScanDishSetupFragment newInstance() {
        return new ScanDishSetupFragment();
    }*/
    public ScanDishSetupFragment() {

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initHandlerThread();
        Log.d(TAG, "onCreate");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView");
        mParameterMananer = ((ScanMainActivity)getActivity()).getParameterMananer();
        mDialogManager = ((ScanMainActivity)getActivity()).getDialogManager();
        String currentlist = ParameterMananer.ITEM_LNB;
        View rootView = inflater.inflate(R.layout.fragment_dish_setup, container, false);
        mSatelliteQuickkey1 = (LinearLayout) rootView.findViewById(R.id.function_key1);
        mSatelliteQuickkey2 = (LinearLayout) rootView.findViewById(R.id.function_key2);
        creatFour1();
        creatFour2();

        mStrengthContainer = (LinearLayout) rootView.findViewById(R.id.strength_container);
        mQualityContainer = (LinearLayout) rootView.findViewById(R.id.quality_container);
        mStrengthProgressBar = (ProgressBar)rootView.findViewById(R.id.strength_progressbar);
        mQualityProgressBar = (ProgressBar)rootView.findViewById(R.id.quality_progressbar);
        mStrengthTextView = (TextView)rootView.findViewById(R.id.strength_percent);
        mQualityTextView = (TextView)rootView.findViewById(R.id.quality_percent);

        mListViewItem = (ItemListView) rootView.findViewById(R.id.listview_item);
        mListViewOption = (ItemListView) rootView.findViewById(R.id.listview_option);
        mItemDetailItem.addAll(mParameterMananer.getDvbsParaManager().getCurrentItemList());
        mItemAdapterItem = new ItemAdapter(mItemDetailItem, getActivity());
        mListViewItem.setAdapter(mItemAdapterItem);
        mListViewItem.setCurrentListSide(ItemListView.LIST_LEFT);

        mItemTitleTextView = (TextView) rootView.findViewById(R.id.listview_item_title);
        if (ItemListView.ITEM_SATALLITE.equals(currentlist)) {
            mItemTitleTextView.setText(R.string.list_type_satellite);
        } else if (ItemListView.ITEM_TRANSPONDER.equals(currentlist)) {
            mItemTitleTextView.setText(R.string.list_type_transponder);
        } else {
            mItemTitleTextView.setText(R.string.list_type_lnb);
        }

        mListViewItem.setListItemSelectedListener(mListItemSelectedListener);
        mListViewItem.setListItemFocusedListener(mListItemFocusedListener);
        mListViewItem.setListSwitchedListener(mListSwitchedListener);
        mListViewItem.setListTypeSwitchedListener(mListTypeSwitchedListener);
        mListViewItem.setDataCallBack(mSingleSelectDialogCallBack);
        mListViewItem.setDtvkitGlueClient(((ScanMainActivity)getActivity()).getDtvkitGlueClient());

        String currentLnb = mParameterMananer.getDvbsParaManager().getCurrentLnbId();
        mItemDetailOption.addAll(mParameterMananer.getDvbsParaManager().getLnbParamsWithId(currentLnb));
        mItemAdapterOption = new ItemAdapter(mItemDetailOption, getActivity());
        mListViewOption.setAdapter(mItemAdapterOption);
        mListViewItem.setSelection(0);

        mListViewOption.setListItemSelectedListener(mListItemSelectedListener);
        mListViewOption.setListItemFocusedListener(mListItemFocusedListener);
        mListViewOption.setListSwitchedListener(mListSwitchedListener);
        mListViewOption.setListTypeSwitchedListener(mListTypeSwitchedListener);
        mListViewOption.setListType(ItemListView.ITEM_OPTION);
        mListViewOption.setDataCallBack(mSingleSelectDialogCallBack);

        if ((TextUtils.equals(currentlist, ItemListView.ITEM_LNB) && mItemDetailItem.size() > 0)) {
            mListViewItem.requestFocus();
            mListViewOption.cleanChoosed();
            mParameterMananer.getDvbsParaManager().setCurrentListDirection("left");
            View selectedView = mListViewItem.getSelectedView();
            if (selectedView != null) {
                mListViewItem.setChoosed(selectedView);
            }
            mListViewItem.setListType(mParameterMananer.getDvbsParaManager().getCurrentListType());
        } else {
            mListViewItem.cleanChoosed();
            mCurrentListFocus = "right";
            mParameterMananer.getDvbsParaManager().setCurrentListDirection("right");
            View selectedView = mListViewOption.getSelectedView();
            if (selectedView != null) {
                mListViewOption.setChoosed(selectedView);
            }
        }

        mListViewOption.setOnItemSelectedListener(mListViewOption);
        mListViewItem.setOnItemSelectedListener(mListViewItem);
        initStrengthQualityUpdate();

        return rootView;
    }

    private void releaseHandler() {
        Log.d(TAG, "releaseHandler");
        mIsReleasing = true;
        getActivity().finish();
        if (mThreadHandler != null) {
            mThreadHandler.removeCallbacksAndMessages(null);
            mThreadHandler = null;
        }

        if (mHandlerThread != null) {
            mHandlerThread.quit();
            mHandlerThread = null;
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mScheduled) {
            abortStrengthQualityUpdate();
        }
        if (mIsStarted) {
            mIsStarted = false;
            stopTune();
        }
        if (!mIsReleasing) {
            mIsReleasing = true;
            releaseMessage();
        }
        Log.d(TAG, "onStop");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        //releaseHandler();
        //mParameterMananer.stopTuneAction();
        Log.d(TAG, "onDestroy");
    }

    private void changeSatelliteQuickkeyLayout() {
        mSatelliteQuickkey.removeAllViews();
        mSatelliteQuickkey.addView(mSatelliteQuickkey1);
        mSatelliteQuickkey.addView(mSatelliteQuickkey2);
    }

    private void creatFour1() {
        LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = (View) inflater.inflate(R.layout.four_display1, null);
        mSatelliteQuickkey1.removeAllViews();
        mSatelliteQuickkey1.addView(view);
    }

    private void creatFour2() {
        LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = (View) inflater.inflate(R.layout.four_display2, null);
        mSatelliteQuickkey2.removeAllViews();
        mSatelliteQuickkey2.addView(view);
    }

    private void creatConfirmandExit1() {
        LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = (View) inflater.inflate(R.layout.confirm_exit_display, null);
        mSatelliteQuickkey1.removeAllViews();
        mSatelliteQuickkey1.addView(view);
    }

    private void creatSatelliteandScan2() {
        LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = (View) inflater.inflate(R.layout.satellite_scan_display, null);
        mSatelliteQuickkey2.removeAllViews();
        mSatelliteQuickkey2.addView(view);
    }

    private void creatSetlimitandSetlocation1() {
        LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = (View) inflater.inflate(R.layout.limit_location_display, null);
        mSatelliteQuickkey1.removeAllViews();
        mSatelliteQuickkey1.addView(view);
    }

    private void creatEditandExit2() {
        LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = (View) inflater.inflate(R.layout.edit_exit_wheel_display, null);
        mSatelliteQuickkey2.removeAllViews();
        mSatelliteQuickkey2.addView(view);
    }

    private void initStrengthQualityUpdate() {
        startTune();
        if (mScheduled) {
            abortStrengthQualityUpdate();
        }
        timer.schedule(task, 1000, 1000);
        mScheduled = true;
        Log.d(TAG, "initStrengthQualityUpdate");
    }

    private void abortStrengthQualityUpdate() {
        if (mScheduled) {
            mScheduled = false;
            task.cancel();
            timer.cancel();
            Log.d(TAG, "abortStrengthQualityUpdate");
        }
    }

    private CustomDialog mCurrentCustomDialog = null;
    private CustomDialog mCurrentSubCustomDialog = null;
    private DialogCallBack mSingleSelectDialogCallBack = new DialogCallBack() {
        @Override
        public void onStatusChange(View view, String parameterKey, Bundle data) {
            Log.d(TAG, "onStatusChange parameterKey = " + parameterKey + ", data = " + data);
            String lnb = mParameterMananer.getDvbsParaManager().getCurrentLnbId();
            switch (parameterKey) {
                case ParameterMananer.KEY_LNB_TYPE:
                    if (data != null && "selected".equals(data.getString("action"))) {
                        int lnbType = data.getInt("position");
                        if (lnbType == 2) lnbType = 3;
                        mParameterMananer.getDvbsParaManager()
                                .getLnbWrap().getLnbById(lnb).getLnbInfo()
                                .editLnbType(lnbType);
                        if (lnbType == 0 || lnbType == 3) {
                            mParameterMananer.getDvbsParaManager()
                                    .getLnbWrap().getLnbById(lnb).getLnbInfo()
                                    .editLnb22Khz(false);
                        } else if (lnbType == 1) {
                            mParameterMananer.getDvbsParaManager()
                                    .getLnbWrap().getLnbById(lnb).getLnbInfo()
                                    .editLnb22Khz(true);
                        }
                        if (lnbType == 0 || lnbType == 1) {
                            mParameterMananer.getDvbsParaManager()
                                    .getLnbWrap().getLnbById(lnb).getLnbInfo()
                                    .editLnbPower(false);
                        } else if (lnbType == 3) {
                            mParameterMananer.getDvbsParaManager()
                                    .getLnbWrap().getLnbById(lnb).getLnbInfo()
                                    .editLnbPower(true);
                        }
                        if (mCurrentCustomDialog != null && TextUtils.equals(parameterKey, mCurrentCustomDialog.getDialogKey())) {
                            mCurrentCustomDialog.updateListView(mCurrentCustomDialog.getDialogTitle(), mCurrentCustomDialog.getDialogKey(), data.getInt("position"));
                            //mItemAdapterOption.reFill(mParameterMananer.getDvbsParaManager().getLnbParamsWithId());
                        }
                        if (data.getInt("position") == 2) {
                            mCurrentSubCustomDialog = mDialogManager.buildLnbCustomedItemDialog(mSingleSelectDialogCallBack);
                            if (mCurrentSubCustomDialog != null) {
                                mCurrentSubCustomDialog.showDialog();
                            }
                        }
                        mItemAdapterOption.reFill(mParameterMananer.getDvbsParaManager().getLnbParamsWithId());
                    }
                    break;
                case ParameterMananer.KEY_LNB_CUSTOM:
                    if (data != null && "onClick".equals(data.getString("action"))) {
                        if ("ok".equals(data.getString("button"))) {
                            Log.d(TAG, "ok in clicked");
                            int lowMin = data.getInt("lowmin");
                            int lowMax = data.getInt("lowmax");
                            int lowLocal = data.getInt("lowlocal");
                            int highMin = data.getInt("highmin");
                            int highMax = data.getInt("highmax");
                            int highLocal = data.getInt("highlocal");
                            mParameterMananer.getDvbsParaManager()
                                    .getLnbWrap().getLnbById(lnb).getLnbInfo()
                                    .updateLnbTypeFreq(lowMin, lowMax, lowLocal, highMin, highMax, highLocal);
                        } else if ("cancel".equals(data.getString("button"))) {
                            Log.d(TAG, "cancel in clicked");
                        }
                        mItemAdapterOption.reFill(mParameterMananer.getDvbsParaManager().getLnbParamsWithId());
                    }
                    break;
                case ParameterMananer.KEY_UNICABLE: {
                    int unicable_position = data.getInt("position");
                    boolean isLeftAction = "left".equals(data.getString("action"));
                    boolean isRightAction = "right".equals(data.getString("action"));
                    if (unicable_position == 0 && (isLeftAction || isRightAction)) {
                        mParameterMananer.getDvbsParaManager()
                                .getLnbWrap().getLnbById(lnb).getUnicable()
                                .switchUnicable();
                    } else if (unicable_position == 3 && (isLeftAction || isRightAction)) {
                        mParameterMananer.getDvbsParaManager()
                                .getLnbWrap().getLnbById(lnb).getUnicable()
                                .switchUnicablePosition();
                    } else if (unicable_position == 1 && (isLeftAction || isRightAction)) {
                        int min = 0;
                        int max = CustomDialog.DIALOG_SET_SELECT_SINGLE_ITEM_UNICABLE_BAND.length - 1;
                        int unicableChannel = mParameterMananer.getDvbsParaManager().getCurrentLnbParaIntValue("unicable_chan");
                        int updateChannel = unicableChannel;
                        if (isLeftAction) {
                            if (updateChannel > min) {
                                updateChannel --;
                            } else {
                                updateChannel = max;
                            }
                        } else {
                            //right and select has same behavior
                            if (updateChannel < max) {
                                updateChannel ++;
                            } else {
                                updateChannel = min;
                            }
                        }
                        mParameterMananer.getDvbsParaManager()
                                .getLnbWrap().getLnbById(lnb).getUnicable()
                                .editUnicableChannel(updateChannel,
                                        CustomDialog.DIALOG_SET_EDIT_SWITCH_ITEM_UNICABLE_USER_BAND_FREQUENCY_LIST[updateChannel]);
                    }
                    if (mCurrentCustomDialog != null) {
                        mCurrentCustomDialog.updateListView(mCurrentCustomDialog.getDialogTitle(),
                                mCurrentCustomDialog.getDialogKey(), unicable_position);
                    }
                    mItemAdapterOption.reFill(mParameterMananer.getDvbsParaManager().getLnbParamsWithId());
                    break;
                }
                case ParameterMananer.KEY_UB_FREQUENCY:
                    break;
                case ParameterMananer.KEY_LNB_POWER: {
                    int position = data.getInt("position");
                    int lnbType = mParameterMananer.getDvbsParaManager()
                            .getLnbWrap().getLnbById(lnb).getLnbInfo().getType();
                    if (lnbType == 3 /*user defined*/ && "focused".equals(data.getString("action"))) {
                        mParameterMananer.getDvbsParaManager()
                                .getLnbWrap().getLnbById(lnb).getLnbInfo()
                                .editLnbPower((position > 0) ? true : false);
                    }
                    if (mCurrentCustomDialog != null) {
                        mCurrentCustomDialog.updateListView(mCurrentCustomDialog.getDialogTitle(),
                                mCurrentCustomDialog.getDialogKey(), position);
                    }
                    mItemAdapterOption.reFill(mParameterMananer.getDvbsParaManager().getLnbParamsWithId());
                    break;
                }
                case ParameterMananer.KEY_22_KHZ: {
                    int position = data.getInt("position");
                    int lnbType = mParameterMananer.getDvbsParaManager()
                            .getLnbWrap().getLnbById(lnb).getLnbInfo().getType();
                    if (lnbType == 3 /*user defined*/ && "focused".equals(data.getString("action"))) {
                        mParameterMananer.getDvbsParaManager()
                                .getLnbWrap().getLnbById(lnb).getLnbInfo()
                                .editLnb22Khz((position > 0) ? true : false);
                    }
                    if (mCurrentCustomDialog != null) {
                        mCurrentCustomDialog.updateListView(mCurrentCustomDialog.getDialogTitle(),
                                mCurrentCustomDialog.getDialogKey(), position);
                    }
                    mItemAdapterOption.reFill(mParameterMananer.getDvbsParaManager().getLnbParamsWithId());
                    break;
                }
                case ParameterMananer.KEY_TONE_BURST: {
                    int position = data.getInt("position");
                    if ("focused".equals(data.getString("action"))) {
                        if (position > (CustomDialog.DIALOG_SET_SELECT_SINGLE_ITEM_TONE_BURST_LIST.length - 1)) {
                            position = 0;
                        }
                        mParameterMananer.getDvbsParaManager()
                                .getLnbWrap().getLnbById(lnb)
                                .editToneBurst(CustomDialog.DIALOG_SET_SELECT_SINGLE_ITEM_TONE_BURST_LIST[position]);
                    }
                    if (mCurrentCustomDialog != null) {
                        mCurrentCustomDialog.updateListView(mCurrentCustomDialog.getDialogTitle(),
                                mCurrentCustomDialog.getDialogKey(), position);
                    }
                    mItemAdapterOption.reFill(mParameterMananer.getDvbsParaManager().getLnbParamsWithId());
                    break;
                }
                case ParameterMananer.KEY_DISEQC1_0: {
                    int position = data.getInt("position");
                    if ("focused".equals(data.getString("action"))) {
                        if (position > (CustomDialog.DIALOG_SET_SELECT_SINGLE_ITEM_DISEQC1_0_LIST.length - 1)) {
                            position = 0;
                        }
                        mParameterMananer.getDvbsParaManager()
                                .getLnbWrap().getLnbById(lnb)
                                .editCswitch(position);
                    }
                    if (mCurrentCustomDialog != null) {
                        mCurrentCustomDialog.updateListView(mCurrentCustomDialog.getDialogTitle(),
                                mCurrentCustomDialog.getDialogKey(), position);
                    }
                    mItemAdapterOption.reFill(mParameterMananer.getDvbsParaManager().getLnbParamsWithId());
                    break;
                }
                case ParameterMananer.KEY_DISEQC1_1: {
                    int position = data.getInt("position");
                    if ("focused".equals(data.getString("action"))) {
                        if (position > (CustomDialog.DIALOG_SET_SELECT_SINGLE_ITEM_DISEQC1_1_LIST.length - 1)) {
                            position = 0;
                        }
                        mParameterMananer.getDvbsParaManager()
                                .getLnbWrap().getLnbById(lnb)
                                .editUswitch(position);
                    }
                    if (mCurrentCustomDialog != null) {
                        mCurrentCustomDialog.updateListView(mCurrentCustomDialog.getDialogTitle(),
                                mCurrentCustomDialog.getDialogKey(), position);
                    }
                    mItemAdapterOption.reFill(mParameterMananer.getDvbsParaManager().getLnbParamsWithId());
                    break;
                }
                case ParameterMananer.KEY_MOTOR:
                    if (data != null && "selected".equals(data.getString("action"))) {
                        boolean isDiseqc1_3 = false;
                        mParameterMananer.getDvbsParaManager().getLnbWrap().getLnbById(lnb).editMotor(data.getInt("position"));
                        if (data.getInt("position") >= 1 && mCurrentCustomDialog != null && TextUtils.equals(parameterKey, mCurrentCustomDialog.getDialogKey())){
                            if (data.getInt("position") == 2) {
                                isDiseqc1_3 = true;
                            }
                            mCurrentSubCustomDialog = mDialogManager.buildDiseqc1_2_ItemDialog(isDiseqc1_3, mSingleSelectDialogCallBack);
                            if (mCurrentSubCustomDialog != null) {
                                mCurrentSubCustomDialog.showDialog();
                            }
                        }
                        mCurrentCustomDialog.updateListView(mCurrentCustomDialog.getDialogTitle(), mCurrentCustomDialog.getDialogKey(), data.getInt("position"));
                        mItemAdapterOption.reFill(mParameterMananer.getDvbsParaManager().getLnbParamsWithId());
                    }
                    break;
                case ParameterMananer.KEY_DISEQC1_2: {
                    int motor = mParameterMananer.getDvbsParaManager().getLnbWrap().getLnbById(lnb).getMotor();
                    boolean isDiseqc1_3 = (motor == 2);
                    if (data != null && "selected".equals(data.getString("action"))) {
                        int diseqc1_2_position = data.getInt("position");
                        switch (diseqc1_2_position) {
                            case 3://dish limts east
                                mParameterMananer.setDishELimits();
                                break;
                            case 4://dish limts west
                                mParameterMananer.setDishWLimits();
                                break;
                            case 7: /*move*/ {
                                int direction = mParameterMananer.getDvbsParaManager().getCurrentDiseqcValue("dish_dir");
                                int dishStep = mParameterMananer.getDvbsParaManager().getCurrentDiseqcValue("dish_step");
                                mParameterMananer.dishMove(direction, dishStep);
                                break;
                            }
                            case 9: /*save to position*/ {
                                List<String> satelist = mParameterMananer.getDvbsParaManager().getSatelliteNameListSelected();
                                int sateIdex = mParameterMananer.getDvbsParaManager().getCurrentDiseqcValue("sate_index");
                                String sateName = satelist.get(sateIdex);
                                int dishPos = mParameterMananer.getDvbsParaManager().getCurrentDiseqcValue("dish_pos");
                                mParameterMananer.getDvbsParaManager().getSatelliteWrap().editDishPos(sateName, dishPos);
                                mParameterMananer.storeDishPosition(dishPos);
                                break;
                            }
                            case 10: /*move to position*/ {
                                List<String> satelist = mParameterMananer.getDvbsParaManager().getSatelliteNameListSelected();
                                int sateIdex = mParameterMananer.getDvbsParaManager().getCurrentDiseqcValue("sate_index");
                                String sateName = satelist.get(sateIdex);
                                int dishPos = mParameterMananer.getDvbsParaManager().getCurrentDiseqcValue("dish_pos");
                                mParameterMananer.getDvbsParaManager().getSatelliteWrap().editDishPos(sateName, dishPos);
                                mParameterMananer.moveDishToPosition(dishPos);
                                break;
                            }
                            case 16: /*gotoxx*/ {
                                List<String> satelist = mParameterMananer.getDvbsParaManager().getSatelliteNameListSelected();
                                int sateIdex = mParameterMananer.getDvbsParaManager().getCurrentDiseqcValue("sate_index");
                                String sateName = satelist.get(sateIdex);
                                int locationIndex = mParameterMananer.getDvbsParaManager().getCurrentDiseqcValue("diseqc_location");
                                boolean locationIsEast = mParameterMananer.getDvbsParaManager().getLnbWrap()
                                        .getLocationInfoByIndex(locationIndex).isLongitudeEast();
                                int locationLongitude = mParameterMananer.getDvbsParaManager().getLnbWrap()
                                        .getLocationInfoByIndex(locationIndex).getLongitude();
                                boolean localtionIsNorth = mParameterMananer.getDvbsParaManager().getLnbWrap()
                                        .getLocationInfoByIndex(locationIndex).isLatitudeNorth();
                                int locationLatitude = mParameterMananer.getDvbsParaManager().getLnbWrap()
                                        .getLocationInfoByIndex(locationIndex).getLatitude();
                                mParameterMananer.getDvbsParaManager().getLnbWrap().actionToLocation(sateName,
                                        locationIsEast, locationLongitude, localtionIsNorth, locationLatitude);
                                break;
                            }
                            default:
                                break;
                        }
                    } else if (data != null && ("left".equals(data.getString("action")) || "right".equals(data.getString("action")))) {
                        int position = data.getInt("position");
                        boolean needbreak = false;
                        switch (position) {
                            case 0: /*satellite*/ {
                                List<String> satelist = mParameterMananer.getDvbsParaManager().getSatelliteNameListSelected();
                                int indexMax = satelist.size() - 1;
                                int sateIndex = mParameterMananer.getDvbsParaManager().getCurrentDiseqcValue("sate_index");
                                if ("left".equals(data.getString("action"))) {
                                    if (sateIndex != 0) {
                                        sateIndex = sateIndex - 1;
                                    } else {
                                        sateIndex = indexMax;
                                    }
                                } else {
                                    if (sateIndex < indexMax) {
                                        sateIndex = sateIndex + 1;
                                    } else {
                                        sateIndex = 0;
                                    }
                                }
                                mParameterMananer.getDvbsParaManager().setCurrentDiseqcValue("sate_index", sateIndex);
                                int dish_Pos = mParameterMananer.getDvbsParaManager()
                                        .getSatelliteWrap().getSatelliteByName(satelist.get(sateIndex)).getDishPos();
                                mParameterMananer.getDvbsParaManager().setCurrentDiseqcValue("dish_pos", dish_Pos);
                                break;
                            }
                            case 1: /*transponder*/ {
                                int sateIndex = mParameterMananer.getDvbsParaManager().getCurrentDiseqcValue("sate_index");
                                List<String> satelist = mParameterMananer.getDvbsParaManager().getSatelliteNameListSelected();
                                String sateName = satelist.get(sateIndex);
                                LinkedList<ItemDetail> tps = mParameterMananer.getDvbsParaManager().getTransponderList(sateName);
                                int tpIndx = mParameterMananer.getDvbsParaManager().getCurrentDiseqcValue("tp_index");
                                int indexMax = tps.size() - 1;
                                if ("left".equals(data.getString("action"))) {
                                    if (tpIndx != 0) {
                                        tpIndx = tpIndx - 1;
                                    } else {
                                        tpIndx = indexMax;
                                    }
                                } else {
                                    if (tpIndx < indexMax) {
                                        tpIndx = tpIndx + 1;
                                    } else {
                                        tpIndx = 0;
                                    }
                                }
                                mParameterMananer.getDvbsParaManager().setCurrentDiseqcValue("tp_index", tpIndx);
                                startTune();
                                break;
                            }
                            case 2: /*dish limts status*/ {
                                int limitState = mParameterMananer.getDvbsParaManager().getCurrentDiseqcValue("dishlimit_state");
                                if (limitState > 0) {
                                    limitState = 0;
                                } else {
                                    limitState = 1;
                                }
                                mParameterMananer.getDvbsParaManager().setCurrentDiseqcValue("dishlimit_state", limitState);
                                mParameterMananer.enableDishLimits(limitState == 1);
                                break;
                            }
                            case 5: /*dish move direction*/ {
                                int directionvalue = mParameterMananer.getDvbsParaManager().getCurrentDiseqcValue("dish_dir");
                                if ("left".equals(data.getString("action"))) {
                                    if (directionvalue != 0) {
                                        directionvalue = directionvalue - 1;
                                    } else {
                                        directionvalue = 2;
                                    }
                                } else {
                                    if (directionvalue < 2) {
                                        directionvalue = directionvalue + 1;
                                    } else {
                                        directionvalue = 0;
                                    }
                                }
                                mParameterMananer.getDvbsParaManager().setCurrentDiseqcValue("dish_dir", directionvalue);
                                break;
                            }
                            case 6: /*dish move step*/ {
                                int stepvalue = mParameterMananer.getDvbsParaManager().getCurrentDiseqcValue("dish_step");
                                if ("left".equals(data.getString("action"))) {
                                    if (stepvalue != 0) {
                                        stepvalue = stepvalue - 1;
                                    } else {
                                        stepvalue = 127;
                                    }
                                } else {
                                    if (stepvalue < 127) {
                                        stepvalue = stepvalue + 1;
                                    } else {
                                        stepvalue = 0;
                                    }
                                }
                                mParameterMananer.getDvbsParaManager().setCurrentDiseqcValue("dish_step", stepvalue);
                                break;
                            }
                            case 8: /*dish position*/ {
                                int positionvalue = mParameterMananer.getDvbsParaManager().getCurrentDiseqcValue("dish_pos");
                                if ("left".equals(data.getString("action"))) {
                                    if (positionvalue != 0) {
                                        positionvalue = positionvalue - 1;
                                    } else {
                                        positionvalue = 255;
                                    }
                                } else {
                                    if (positionvalue < 255) {
                                        positionvalue = positionvalue + 1;
                                    } else {
                                        positionvalue = 0;
                                    }
                                }
                                mParameterMananer.getDvbsParaManager().setCurrentDiseqcValue("dish_pos", positionvalue);
                                break;
                            }
                            case 11: /* location city */ {
                                int index = mParameterMananer.getDvbsParaManager().getCurrentDiseqcValue("diseqc_location");
                                List<String> locationNameList = mParameterMananer.getDvbsParaManager().getLnbWrap()
                                        .getDiseqcLocationNames();
                                int indexMax = locationNameList.size() - 1;
                                if ("left".equals(data.getString("action"))) {
                                    if (index != 0) {
                                        index = index - 1;
                                    } else {
                                        index = indexMax;
                                    }
                                } else {
                                    if (index < indexMax) {
                                        index = index + 1;
                                    } else {
                                        index = 0;
                                    }
                                }
                                mParameterMananer.getDvbsParaManager().setCurrentDiseqcValue("diseqc_location", index);
                                break;
                            }
                            case 12: /*location longitude direction*/
                            case 13: /*Location longitude*/
                            case 14: /*Location latitude direction*/
                            case 15: /*Location latitude*/{
                                int index = mParameterMananer.getDvbsParaManager().getCurrentDiseqcValue("diseqc_location");
                                List<String> locationNameList = mParameterMananer.getDvbsParaManager().getLnbWrap()
                                        .getDiseqcLocationNames();
                                if (index < (locationNameList.size() -1)) {
                                    if (("manual".equals(locationNameList.get(index)))) {
                                        boolean locationIsEast = mParameterMananer.getDvbsParaManager().getLnbWrap()
                                                .getLocationInfoByIndex(index).isLongitudeEast();
                                        int locationLongitude = mParameterMananer.getDvbsParaManager().getLnbWrap()
                                                .getLocationInfoByIndex(index).getLongitude();
                                        boolean localtionIsNorth = mParameterMananer.getDvbsParaManager().getLnbWrap()
                                                .getLocationInfoByIndex(index).isLatitudeNorth();
                                        int locationLatitude = mParameterMananer.getDvbsParaManager().getLnbWrap()
                                                .getLocationInfoByIndex(index).getLatitude();
                                        if (position == 12) {
                                            locationIsEast = !locationIsEast;
                                        } else if (position == 13 || position == 15) {
                                            int value = 0;
                                            if (position == 13) value = locationLongitude;
                                            else value = locationLatitude;
                                            if ("left".equals(data.getString("action"))) {
                                                if (value > 0) {
                                                    value --;
                                                } else {
                                                    value = 1800;
                                                }
                                            } else {
                                                if (value < 1800) {
                                                    value ++;
                                                } else {
                                                    value = 0;
                                                }
                                            }
                                            if (position == 13) locationLongitude = value;
                                            else locationLatitude = value;
                                        } else if (position == 14) {
                                            localtionIsNorth = !localtionIsNorth;
                                        }
                                        mParameterMananer.getDvbsParaManager().getLnbWrap().editManualLocation
                                                (locationIsEast, locationLongitude, localtionIsNorth, locationLatitude);
                                    }
                                }
                                break;
                            }
                            default:
                                needbreak = true;
                                break;
                        }
                        if (!needbreak && mCurrentSubCustomDialog != null && mCurrentSubCustomDialog.isShowing()) {
                            mCurrentSubCustomDialog.updateDiseqc1_2_Dialog(isDiseqc1_3);
                        } else {
                            Log.d(TAG, "mCurrentSubCustomDialog null or need break or not displayed");
                        }
                    }
                    break;
                }
                case ParameterMananer.KEY_FUNCTION:
                    if (data != null) {
                        String action = data.getString("action");
                        String listtype = data.getString("listtype");
                        switch (action) {
                            case "add": {
                                if (ItemListView.ITEM_SATALLITE.equals(data.getString("listtype"))) {
                                    mCurrentCustomDialog = mDialogManager.buildAddSatelliteDialogDialog(null, mSingleSelectDialogCallBack);
                                } else if (ItemListView.ITEM_TRANSPONDER.equals(data.getString("listtype"))) {
                                    mCurrentCustomDialog = mDialogManager.buildAddTransponderDialogDialog(null, mSingleSelectDialogCallBack);
                                } else if (ItemListView.ITEM_LNB.equals(data.getString("listtype"))) {
                                    mParameterMananer.getDvbsParaManager().getLnbWrap().addEmptyLnb();
                                    mItemAdapterItem.reFill(mParameterMananer.getDvbsParaManager().getCurrentItemList());
                                    mCurrentCustomDialog = null;
                                } else {
                                    Log.d(TAG, "not sure");
                                    mCurrentCustomDialog = null;
                                }
                                if (mCurrentCustomDialog != null) {
                                    mCurrentCustomDialog.showDialog();
                                }
                                break;
                            }
                            case "edit":
                                if (ItemListView.ITEM_SATALLITE.equals(data.getString("listtype"))) {
                                    mCurrentCustomDialog = mDialogManager.buildAddSatelliteDialogDialog(data.getString("parameter"), mSingleSelectDialogCallBack);
                                } else if (ItemListView.ITEM_TRANSPONDER.equals(data.getString("listtype"))) {
                                    String currentTransponder = mParameterMananer.getDvbsParaManager().getCurrentTransponder();
                                    mCurrentCustomDialog = mDialogManager.buildAddTransponderDialogDialog(currentTransponder, mSingleSelectDialogCallBack);
                                } else {
                                    Log.d(TAG, "not sure");
                                    mCurrentCustomDialog = null;
                                }
                                if (mCurrentCustomDialog != null) {
                                    mCurrentCustomDialog.showDialog();
                                }
                                break;
                            case "delete":
                                if (ItemListView.ITEM_SATALLITE.equals(data.getString("listtype"))) {
                                    mCurrentCustomDialog = mDialogManager.buildRemoveSatelliteDialogDialog(data.getString("parameter"), mSingleSelectDialogCallBack);
                                } else if (ItemListView.ITEM_TRANSPONDER.equals(data.getString("listtype"))) {
                                    mCurrentCustomDialog = mDialogManager.buildRemoveTransponderDialogDialog(data.getString("parameter"), mSingleSelectDialogCallBack);
                                } else if (ItemListView.ITEM_LNB.equals(data.getString("listtype"))) {
                                    String focusedLnb = mParameterMananer.getDvbsParaManager().getCurrentFocusedLnbId();
                                    String selectedLnb = mParameterMananer.getDvbsParaManager().getCurrentLnbId();
                                    mParameterMananer.getDvbsParaManager().getLnbWrap().removeLnb(focusedLnb);
                                    mListViewItem.cleanChoosed();
                                    LinkedList<ItemDetail> lnbs = mParameterMananer.getDvbsParaManager().getLnbIdList();
                                    if (lnbs != null && lnbs.size() > 0) {
                                        if (selectedLnb != null && selectedLnb.equals(focusedLnb)) {
                                            //select first lnb
                                            mParameterMananer.getDvbsParaManager().setCurrentLnbId(lnbs.get(0).getFirstText());
                                        } else {
                                            int selection = mListViewItem.getSelectedItemPosition();
                                            if (selection < lnbs.size()) {
                                                mParameterMananer.getDvbsParaManager().setCurrentFocusedLnbId(lnbs.get(selection).getFirstText());
                                                if (mListViewItem.getSelectedView() != null) {
                                                    mListViewItem.setChoosed(mListViewItem.getSelectedView());
                                                }
                                            }
                                        }
                                    } else {
                                        mParameterMananer.getDvbsParaManager().setCurrentLnbId("");
                                        mParameterMananer.getDvbsParaManager().setCurrentFocusedLnbId("");
                                    }
                                    mItemAdapterItem.reFill(mParameterMananer.getDvbsParaManager().getCurrentItemList());
                                    mItemAdapterOption.reFill(mParameterMananer.getDvbsParaManager().getLnbParamsWithId());
                                } else {
                                    Log.d(TAG, "not sure");
                                    mCurrentCustomDialog = null;
                                }
                                if (mCurrentCustomDialog != null) {
                                    mCurrentCustomDialog.showDialog();
                                }
                                break;
                            case "scan":
                                stopAndRelease();
                                break;
                            default:
                                break;
                        }
                    }
                    break;
                case ParameterMananer.KEY_ADD_SATELLITE:
                    if (data != null && "ok".equals(data.getString("button"))) {
                        String name_add = data.getString("value1");
                        boolean iseast_add = data.getBoolean("value2", true);
                        int position_add = Integer.valueOf(data.getString("value3"));
                        mParameterMananer.getDvbsParaManager().getSatelliteWrap().addSatellite(name_add, iseast_add, position_add);
                        mItemAdapterItem.reFill(mParameterMananer.getDvbsParaManager().getCurrentItemList());
                        switchtoLeftList();
                    }
                    break;
                case ParameterMananer.KEY_EDIT_SATELLITE:
                    if (data != null && "ok".equals(data.getString("button"))) {
                        String new_name_edit = data.getString("value1");
                        boolean iseast_edit = data.getBoolean("value2", true);
                        int position_edit = Integer.valueOf(data.getString("value3"));
                        String old_name_edit = data.getString("value4");
                        mParameterMananer.getDvbsParaManager().getSatelliteWrap().editSatellite(old_name_edit, new_name_edit, iseast_edit, position_edit);
                        if (old_name_edit.equals(mParameterMananer.getDvbsParaManager().getCurrentSatellite())) {
                            mParameterMananer.getDvbsParaManager().setCurrentSatellite(new_name_edit);
                            mItemAdapterItem.reFill(mParameterMananer.getDvbsParaManager().getCurrentItemList());
                            mItemAdapterOption.reFill(mParameterMananer.getDvbsParaManager().getLnbParamsWithId());
                        } else {
                            mItemAdapterItem.reFill(mParameterMananer.getDvbsParaManager().getCurrentItemList());
                        }
                    }
                    break;
                case ParameterMananer.KEY_REMOVE_SATELLITE:{
                    if (data != null && "ok".equals(data.getString("button"))) {
                        String name_remove = data.getString("value1");
                        mParameterMananer.getDvbsParaManager().getSatelliteWrap().removeSatellite(name_remove);
                        String inputId = getActivity().getIntent().getStringExtra(TvInputInfo.EXTRA_INPUT_ID);
                        if (inputId != null) {
                            Log.d(TAG, "KEY_REMOVE_SATELLITE sync inputId = " + inputId);
                            ComponentName sync = new ComponentName(getActivity(), DtvkitEpgSync.class);
                            EpgSyncJobService.requestImmediateSync(getActivity(), inputId, true, sync);
                        } else {
                            Log.d(TAG, "KEY_REMOVE_SATELLITE empty inputId");
                        }
                        //Log.d(TAG, "KEY_REMOVE_SATELLITE = " + mParameterMananer.getCurrentSatellite());
                        if (TextUtils.equals(name_remove, mParameterMananer.getDvbsParaManager().getCurrentSatellite())) {
                            mItemAdapterOption.reFill(mParameterMananer.getDvbsParaManager().getLnbParamsWithId());
                        }
                        if (mListViewItem.getSelectedView() != null) {
                            mListViewItem.cleanChoosed();
                            mListViewItem.setChoosed(mListViewItem.getSelectedView());
                        }
                        mItemAdapterItem.reFill(mParameterMananer.getDvbsParaManager().getCurrentItemList());
                    }
                    break;
                }
                case ParameterMananer.KEY_ADD_TRANSPONDER:
                case ParameterMananer.KEY_EDIT_TRANSPONDER:
                    if (data != null && "ok".equals(data.getString("button"))) {
                        String name_edit_t = data.getString("satellite");
                        String oldName = data.getString("oldName");
                        boolean isDvbs2 = data.getBoolean("system");
                        int new_frequency_edit_t = data.getInt("frequency");
                        String new_polarity_edit_t = data.getString("polarity");
                        int new_symbol_edit_t = data.getInt("symbol");
                        String new_fec_edit_t = data.getString("fec");
                        String new_modulation_edit_t = data.getString("modulation");
                        if (parameterKey == ParameterMananer.KEY_ADD_TRANSPONDER) {
                            mParameterMananer.getDvbsParaManager().getSatelliteWrap()
                                    .addTransponder(name_edit_t, new_frequency_edit_t,
                                            new_polarity_edit_t, new_symbol_edit_t, isDvbs2,
                                            new_modulation_edit_t, new_fec_edit_t, "auto");
                        } else {
                            mParameterMananer.getDvbsParaManager().getSatelliteWrap()
                                    .editTransponder(name_edit_t, oldName, new_frequency_edit_t,
                                            new_polarity_edit_t, new_symbol_edit_t, isDvbs2,
                                            new_modulation_edit_t, new_fec_edit_t, "auto");
                            mParameterMananer.getDvbsParaManager()
                                    .setCurrentTransponder(new_frequency_edit_t + new_polarity_edit_t + new_symbol_edit_t);
                        }
                        mItemAdapterItem.reFill(mParameterMananer.getDvbsParaManager().getCurrentItemList());
                    }
                    break;
                case ParameterMananer.KEY_REMOVE_TRANSPONDER:{
                    if (data != null && "ok".equals(data.getString("button"))) {
                        String sateName_remove_t = data.getString("satellite");
                        String tpName_remove_t = data.getString("transponder");
                        mParameterMananer.getDvbsParaManager().getSatelliteWrap()
                                .removeTransponder(sateName_remove_t, tpName_remove_t);
                        String inputId = getActivity().getIntent().getStringExtra(TvInputInfo.EXTRA_INPUT_ID);
                        if (inputId != null) {
                            Log.d(TAG, "KEY_REMOVE_TRANSPONDER sync inputId = " + inputId);
                            ComponentName sync = new ComponentName(getActivity(), DtvkitEpgSync.class);
                            EpgSyncJobService.requestImmediateSync(getActivity(), inputId, true, sync);
                        } else {
                            Log.d(TAG, "KEY_REMOVE_TRANSPONDER empty inputId");
                        }
                        if (mListViewItem.getSelectedView() != null) {
                            mListViewItem.cleanChoosed();
                            mListViewItem.setChoosed(mListViewItem.getSelectedView());
                        }
                        mItemAdapterItem.reFill(mParameterMananer.getDvbsParaManager().getCurrentItemList());
                    }
                    break;
                }
                default:
                    break;
            }
        }
    };

    private void startTune() {
        if (mThreadHandler != null) {
            if (mThreadHandler.hasMessages(MSG_START_TUNE_ACTION)) {
                mThreadHandler.removeMessages(MSG_START_TUNE_ACTION);
            }
            Log.d(TAG, "sendEmptyMessage startTune");
            mThreadHandler.sendEmptyMessageDelayed(MSG_START_TUNE_ACTION, VALUE_START_TUNE_DELAY);
        }
    }

    private void stopTune() {
        if (mThreadHandler != null) {
            if (mThreadHandler.hasMessages(MSG_START_TUNE_ACTION)) {
                mThreadHandler.removeMessages(MSG_START_TUNE_ACTION);
            }
            Log.d(TAG, "sendEmptyMessage stopTune");
            mThreadHandler.sendEmptyMessage(MSG_STOP_TUNE_ACTION);
        }
    }

    private void stopAndRelease() {
        Log.d(TAG, "stopAndRelease");
        abortStrengthQualityUpdate();
        stopTune();
        mParameterMananer.getDvbsParaManager().resetSelections();
        releaseMessage();
    }

    private void releaseMessage() {
        Log.d(TAG, "releaseMessage");
        if (mThreadHandler != null) {
            if (mThreadHandler.hasMessages(MSG_STOP_RELEAS_ACTION)) {
                mThreadHandler.removeMessages(MSG_STOP_RELEAS_ACTION);
            }
            mThreadHandler.sendEmptyMessage(MSG_STOP_RELEAS_ACTION);
        }
    }

    private void switchtoLeftList() {
        if (ItemListView.LIST_RIGHT.equals(mCurrentListFocus)) {
            mCurrentListFocus = ItemListView.LIST_LEFT;
        }
        if (ItemListView.LIST_LEFT.equals(mCurrentListFocus)) {
            mListViewOption.cleanChoosed();
            mListViewItem.cleanChoosed();
            mListViewItem.requestFocus();
            if (mListViewItem.getSelectedView() != null) {
                mListViewItem.setChoosed(mListViewItem.getSelectedView());
            }
            creatFour1();
            creatFour2();
            mParameterMananer.getDvbsParaManager().setCurrentListDirection(ItemListView.LIST_LEFT);
        }
    }

    ListItemSelectedListener mListItemSelectedListener = new ListItemSelectedListener() {

        @Override
        public void onListItemSelected(int position, String type, boolean selected) {
            Log.d(TAG, "onListItemSelected position = " + position + ", type = " + type + ", selected = " + selected);
            if (ItemListView.LIST_LEFT.equals(mCurrentListFocus)) {
                String listtype = mParameterMananer.getDvbsParaManager().getCurrentListType();
                if (ItemListView.ITEM_SATALLITE.equals(listtype)) {
                    LinkedList<ItemDetail> sateAllList = mParameterMananer.getDvbsParaManager().getSatelliteNameList();
                    String selectedSatellite = "";
                    if (sateAllList != null && sateAllList.size() > 0) {
                        selectedSatellite = sateAllList.get(position).getFirstText();
                    }
                    if (selectedSatellite != null || !(selectedSatellite.isEmpty())) {
                        String lnb = mParameterMananer.getDvbsParaManager().getCurrentLnbId();
                        mParameterMananer.getDvbsParaManager().getSatelliteWrap().getSatelliteByName(selectedSatellite).boundLnb(lnb, selected);
                    }
                } else if (ParameterMananer.ITEM_TRANSPONDER.equals(listtype)) {
                    LinkedList<ItemDetail> tps = mParameterMananer.getDvbsParaManager().getTransponderList();
                    String selectTp = "";
                    if (tps != null && tps.size() > 0) {
                        selectTp = tps.get(position).getFirstText();
                    }
                    if (selectTp != null || !(selectTp.isEmpty())) {
                        String sateName = mParameterMananer.getDvbsParaManager().getCurrentSatellite();
                        mParameterMananer.getDvbsParaManager().getSatelliteWrap().selectTransponder(sateName, selectTp, selected);
                    }
                    startTune();
                } else if (ParameterMananer.ITEM_LNB.equals(listtype)) {
                    LinkedList<ItemDetail> lnbList = mParameterMananer.getDvbsParaManager().getLnbIdList();
                    String currentLnb = mParameterMananer.getDvbsParaManager().getCurrentLnbId();
                    if (lnbList != null && lnbList.size() > 0) {
                        String selectedLnb = lnbList.get(position).getFirstText();
                        if (!currentLnb.equals(selectedLnb)) {
                            mParameterMananer.getDvbsParaManager().setCurrentLnbId(selectedLnb);
                        }
                    }
                }
                mItemAdapterItem.reFill(mParameterMananer.getDvbsParaManager().getCurrentItemList());
                mItemAdapterOption.reFill(mParameterMananer.getDvbsParaManager().getLnbParamsWithId());

            } else if (ItemListView.LIST_RIGHT.equals(mCurrentListFocus)) {
                if (position == 0) {
                    String listtype = ParameterMananer.ITEM_SATALLITE;
                    mItemAdapterItem.reFill(mParameterMananer.getDvbsParaManager().getCurrentItemList());
                    mListViewItem.switchListToType(listtype);
                    return;
                }
                String lnb = mParameterMananer.getDvbsParaManager().getCurrentLnbId();
                int lnbType = mParameterMananer.getDvbsParaManager().getLnbWrap().getLnbById(lnb).getLnbInfo().getType();
                if ((position == 3 || position == 4)) {
                    if (lnbType != 3 /*user defined*/) {
                        Log.d(TAG, "onListItemSelected KEY_LNB_POWER or KEY_22_KHZ can only be set under customed lnb type");
                        return;
                    } else {
                        Log.d(TAG, "onListItemSelected KEY_LNB_POWER or KEY_22_KHZ can be set");
                    }
                }
                mCurrentCustomDialog = mDialogManager.buildItemDialogById(position, mSingleSelectDialogCallBack);
                if (mCurrentCustomDialog != null) {
                    mCurrentCustomDialog.showDialog();
                }
            }
        }
    };

    ListItemFocusedListener mListItemFocusedListener = new ListItemFocusedListener() {

        @Override
        public void onListItemFocused(View parent, int position, String type) {
            Log.d(TAG, "onListItemFocused position = " + position + ", type = " + type + ", mfocuse= " + mCurrentListFocus);
            /*if (ItemListView.LIST_LEFT.equals(mCurrentListFocus) && ItemListView.isRightList(type)) {
                mListViewOption.cleanChoosed();
            }*/
            boolean isOptionType = ItemListView.ITEM_OPTION.equals(type);
            if (ItemListView.LIST_LEFT.equals(mCurrentListFocus)) {
                /*mItemDetailOption.clear();
                mItemAdapterOption.reFill(mParameterMananer.getDvbsParaManager().getLnbParamsWithId());
                if (mItemAdapterOption.getCount() > 0) {
                    mListViewOption.setSelection(0);
                }*/
                mListViewItem.cleanChoosed();
                mListViewOption.cleanChoosed();
                View selectedView = mListViewItem.getSelectedView();
                if (selectedView != null) {
                    selectedView.requestFocus();
                    mListViewItem.setChoosed(selectedView);
                }
                if (!isOptionType) {
                    String listtype = mParameterMananer.getDvbsParaManager().getCurrentListType();
                    if (ItemListView.ITEM_SATALLITE.equals(listtype)) {
                        LinkedList<ItemDetail> sates = mParameterMananer.getDvbsParaManager().getSatelliteNameList();
                        if (sates != null && sates.size() > 0) {
                            mParameterMananer.getDvbsParaManager().setCurrentSatellite(sates.get(position).getFirstText());
                        }
                    } else if (ItemListView.ITEM_TRANSPONDER.equals(listtype)) {
                        LinkedList<ItemDetail> tps = mParameterMananer.getDvbsParaManager().getTransponderList();
                        if (tps != null && tps.size() > 0) {
                            mParameterMananer.getDvbsParaManager().setCurrentTransponder(tps.get(position).getFirstText());
                        }
                    } else if (ItemListView.ITEM_LNB.equals(listtype)) {
                        LinkedList<ItemDetail> lnbs = mParameterMananer.getDvbsParaManager().getLnbIdList();
                        if (lnbs != null && lnbs.size() > 0) {
                            mParameterMananer.getDvbsParaManager().setCurrentFocusedLnbId(lnbs.get(position).getFirstText());
                        }
                    }
                }
            } else if (ItemListView.LIST_RIGHT.equals(mCurrentListFocus)) {
                mListViewItem.cleanChoosed();
                if (isOptionType && position > 0 && !(ParameterMananer.ITEM_LNB.equals(mCurrentListType))) {
                    String listtype = ParameterMananer.ITEM_LNB;
                    mItemAdapterItem.reFill(mParameterMananer.getDvbsParaManager().getCurrentItemList());
                    mListViewItem.switchListToType(listtype);
                }
            }
        }
    };

    ListSwitchedListener mListSwitchedListener = new ListSwitchedListener() {

        @Override
        public void onListSwitched(String direction) {
            Log.d(TAG, "onListSwitched direction = " + direction);
            if (direction != null) {
                mCurrentListFocus = direction;
            }
            if (ItemListView.LIST_LEFT.equals(mCurrentListFocus)) {
                mListViewOption.cleanChoosed();
                mListViewItem.requestFocus();
                if (mListViewItem.getSelectedView() != null) {
                    mListViewItem.setChoosed(mListViewItem.getSelectedView());
                }
                creatFour1();
                creatFour2();
                mParameterMananer.getDvbsParaManager().setCurrentListDirection("left");
            } else if (ItemListView.LIST_RIGHT.equals(mCurrentListFocus)) {
                mListViewItem.cleanChoosed();
                mListViewOption.requestFocus();
                mListViewOption.setChoosed(mListViewOption.getSelectedView());
                creatConfirmandExit1();
                creatSatelliteandScan2();
                mParameterMananer.getDvbsParaManager().setCurrentListDirection("right");
            }
            if (ParameterMananer.ITEM_SATALLITE.equals(mCurrentListType)) {
                mListViewItem.setSelection(mParameterMananer.getDvbsParaManager().getCurrentSatelliteIndex());
            } else if (ParameterMananer.ITEM_TRANSPONDER.equals(mCurrentListType)) {
                //mListViewItem.setSelection(mParameterMananer.getDvbsParaManager().getCurrentTransponderIndex());
            }
        }
    };

    ListTypeSwitchedListener mListTypeSwitchedListener = new ListTypeSwitchedListener() {
        @Override
        public void onListTypeSwitched(String listtype) {
            Log.d(TAG, "onListTypeSwitched listtype = " + listtype);
            mCurrentListType = listtype;
            mParameterMananer.getDvbsParaManager().setCurrentListType(mCurrentListType);
            mItemAdapterItem.reFill(mParameterMananer.getDvbsParaManager().getCurrentItemList());
            mListViewItem.setAdapter(mItemAdapterItem);
            mListViewItem.cleanChoosed();
            int position = 0;
            if (ItemListView.ITEM_SATALLITE.equals(listtype)) {
                position = mParameterMananer.getDvbsParaManager().getCurrentSatelliteIndex();
                String lnbKey = mParameterMananer.getDvbsParaManager().getCurrentLnbId();
                String itemText = getContext().getString(R.string.list_type_satellite);
                String text = itemText + "(LNB" + lnbKey + ")";
                mItemTitleTextView.setText(text);
                mStrengthContainer.setVisibility(View.INVISIBLE);
                mQualityContainer.setVisibility(View.INVISIBLE);
            } else if (ItemListView.ITEM_TRANSPONDER.equals(listtype)) {
                String lnbKey = mParameterMananer.getDvbsParaManager().getCurrentLnbId();
                String sate = mParameterMananer.getDvbsParaManager().getCurrentSatellite();
                String itemText = getContext().getString(R.string.list_type_transponder);
                String text = itemText + "(LNB" + lnbKey + ":" + sate + ")";
                mItemTitleTextView.setText(text);
                mStrengthContainer.setVisibility(View.VISIBLE);
                mQualityContainer.setVisibility(View.VISIBLE);
            } else {
                position = mParameterMananer.getDvbsParaManager().getCurrentLnbIndex();
                mItemTitleTextView.setText(R.string.list_type_lnb);
                mStrengthContainer.setVisibility(View.INVISIBLE);
                mQualityContainer.setVisibility(View.INVISIBLE);
                //mParameterMananer.getDvbsParaManager().setCurrentSatellite("");
                //mParameterMananer.getDvbsParaManager().setCurrentTransponder("");
            }
            if (ItemListView.LIST_LEFT.equals(mCurrentListFocus)) {
                mListViewItem.setSelection(position);
                View selectedView = mListViewItem.getSelectedView();
                if (selectedView != null) {
                    selectedView.requestFocus();
                    mListViewItem.setChoosed(selectedView);
                }
            }
        }
    };

    /*public boolean dispatchKeyEvent (KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            switch (event.getKeyCode()) {
                case KeyEvent.KEYCODE_DPAD_UP:
                    if (selectedPosition == 0)
                        return true;
                    break;
                case KeyEvent.KEYCODE_DPAD_DOWN:
                    if (selectedPosition == getAdapter().getCount() - 1)
                        return true;
                    break;
                case KeyEvent.KEYCODE_DPAD_LEFT:
                    if (mListViewItem.requestFocus()) {
                        return true;
                    }
                    break;
                case KeyEvent.KEYCODE_DPAD_RIGHT:
                    if (mListViewOption.requestFocus())
                        return true;
                    break;
            }

            View selectedView = getSelectedView();
            switch (event.getKeyCode()) {
                case KeyEvent.KEYCODE_DPAD_UP:
                case KeyEvent.KEYCODE_DPAD_DOWN:
                    if ( selectedView != null) {
                        clearChoosed(selectedView);
                    }
                case KeyEvent.KEYCODE_DPAD_LEFT:
                case KeyEvent.KEYCODE_DPAD_RIGHT:
                    if ( selectedView != null) {
                        setItemTextColor(selectedView, false);
                    }
                    break;
            }
        }

        return super.dispatchKeyEvent(event);
    }*/
}

