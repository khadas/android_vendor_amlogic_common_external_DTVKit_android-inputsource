package com.droidlogic.fragment;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.io.File;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.droidlogic.fragment.ItemAdapter.ItemDetail;
import com.droidlogic.fragment.dialog.CustomDialog;
import com.droidlogic.settings.ConstantManager;
import com.droidlogic.settings.PropSettingManager;

import android.content.Context;
import android.content.SharedPreferences;
//import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.os.Build.VERSION;

import org.droidlogic.dtvkit.DtvkitGlueClient;
import org.dtvkit.inputsource.DataMananer;
import org.dtvkit.inputsource.ISO639Data;
import org.dtvkit.inputsource.TargetRegionManager;

import com.droidlogic.app.DataProviderManager;
import com.droidlogic.settings.ConstantManager;

public class ParameterMananer {

    private static final String TAG = "ParameterMananer";
    private static final boolean DEBUG = PropSettingManager.getBoolean("vendor.sys.tv.debug.ParameterMananer", false);
    private Context mContext;
    private DtvkitGlueClient mDtvkitGlueClient;
    private DataMananer mDataMananer;
    private DvbsParameterManager mDvbsParaManager;

    public static final String ITEM_SATALLITE              = "satellite";
    public static final String ITEM_TRANSPONDER            = "transponder";
    public static final String ITEM_LNB                    = "lnb";
    public static final String ITEM_DIRECTION              = "left";
    public static final String ITEM_SATALLITE_OPTION       = "satallite_option";
    public static final String ITEM_TRANSPONDER_OPTION     = "tansponder_option";
    public static final String ITEM_OPTION                 = "option";

    public static final String SAVE_SATELITE_POSITION = "satellite_position";
    public static final String SAVE_TRANSPONDER_POSITION = "transponder_position";
    public static final String SAVE_CURRENT_LIST_TYPE = "current_list_type";

    public static final String KEY_SATELLITES = "key_satellites";
    public static final String KEY_SATALLITE = "key_satallite";
    public static final String KEY_TRANSPONDER = "key_transponder";
    public static final String KEY_CURRENT_TYPE = "key_current_type";
    public static final String KEY_CURRENT_DIRECTION = "key_current_direction";
    public static final String KEY_LNB_TYPE = "key_lnb_type";
    public static final String[] DIALOG_SET_SELECT_SINGLE_ITEM_LNB_TYPE_LIST = {"5150", "9750/10600", "Customize"};
    public static final int VALUE_LNB_TYPE_CUSTOM = 2;
    //unicable
    public static final String KEY_UNICABLE = "key_unicable";
    public static final String KEY_UNICABLE_SWITCH = "key_unicable_switch";
    public static final String[] DIALOG_SET_EDIT_SWITCH_ITEM_UNICABLE_SWITCH_LIST = {"off", "on"};
    public static final String[] DIALOG_SET_EDIT_SWITCH_ITEM_UNICABLE_USER_BAND_LIST = {"0", "1", "2", "3", "4", "5", "6", "7"};
    public static final String[] DIALOG_SET_EDIT_SWITCH_ITEM_UNICABLE_POSITION_LIST = {"off", "on"};
    public static final String KEY_USER_BAND = "key_user_band";
    public static final String KEY_UB_FREQUENCY = "key_ub_frequency";
    public static final String KEY_POSITION = "key_position";

    public static final String KEY_LNB_POWER = "key_lnb_power";
    public static final String KEY_LOW_BAND_LNB_POWER = "key_low_band_lnb_power";
    public static final String KEY_HIGH_BAND_LNB_POWER = "key_high_band_lnb_power";
    public static final String KEY_22_KHZ = "key_22_khz";
    public static final String KEY_LOW_BAND_22_KHZ = "key_low_band_22_khz";
    public static final String KEY_HIGH_BAND_22_KHZ = "key_high_band_22_khz";
    public static final String KEY_TONE_BURST = "key_tone_burst";
    public static final String KEY_DISEQC1_0 = "key_diseqc1_0";
    public static final String KEY_DISEQC1_1 = "key_diseqc1_1";
    public static final String KEY_MOTOR = "key_motor";
    public static final String KEY_DISEQC1_2 = "key_diseqc1_2";
    public static final String KEY_DISEQC1_2_DISH_LIMITS_STATUS = "key_dish_limit_status";
    public static final String KEY_DISEQC1_2_DISH_EAST_LIMITS = "key_dish_east_limit";
    public static final String KEY_DISEQC1_2_DISH_WEST_LIMITS = "key_dish_west_limit";
    public static final String KEY_DISEQC1_2_DISH_MOVE_DIRECTION = "key_dish_move_direction";
    public static final String KEY_DISEQC1_2_DISH_MOVE_STEP = "key_dish_move_step";
    public static final String KEY_DISEQC1_2_DISH_CURRENT_POSITION = "key_dish_current_position";
    public static final String KEY_DISEQC1_2_DISH_SAVE_POSITION = "key_dish_save_to_position";
    public static final String KEY_DISEQC1_2_DISH_MOVE_TO_POSITION = "key_dish_move_to_position";
    public static final String KEY_DISEQC1_3_LOCATION_STRING = "key_diseqc_location";

    public static final String[] ID_DIALOG_KEY_COLLECTOR = {KEY_SATELLITES, KEY_SATALLITE, KEY_TRANSPONDER,
            KEY_LNB_TYPE, KEY_UNICABLE_SWITCH/*KEY_UNICABLE*/, KEY_LNB_POWER,
            KEY_22_KHZ, KEY_TONE_BURST, KEY_DISEQC1_0, KEY_DISEQC1_1, KEY_MOTOR};
    public static final String KEY_LNB_CUSTOM = "key_lnb_custom";
    public static final String KEY_LNB_CUSTOM_SINGLE_DOUBLE = "key_lnb_custom_single_double";
    public static final int DEFAULT_LNB_CUSTOM_SINGLE_DOUBLE = 0;//SINGLE
    public static final String KEY_LNB_CUSTOM_LOW_MIN = "key_lnb_low_band_min";
    public static final String KEY_LNB_CUSTOM_LOW_MAX = "key_lnb_low_band_max";
    public static final String KEY_LNB_CUSTOM_LOW_BAND = "key_lnb_low_band";
    public static final String KEY_LNB_CUSTOM_HIGH_MIN = "key_lnb_high_band_min";
    public static final String KEY_LNB_CUSTOM_HIGH_MAX = "key_lnb_high_band_max";
    public static final String KEY_LNB_CUSTOM_HIGH_BAND = "key_lnb_high_band";
    public static final int VALUE_LNB_CUSTOM_MIN = 0;
    public static final int VALUE_LNB_CUSTOM_MAX = 11750;
    //default value
    public static final String KEY_SATALLITE_DEFAULT_VALUE = "null";
    public static final String KEY_TRANSPONDER_DEFAULT_VALUE = "null";
    public static final String KEY_LNB_TYPE_DEFAULT_VALUE = "9750/10600";
    public static final String KEY_LNB_TYPE_DEFAULT_SINGLE_VALUE = "9750";
    //unicable
    public static final String KEY_UNICABLE_DEFAULT_VALUE = "off";
    public static final String KEY_UNICABLE_SWITCH_DEFAULT_VALUE = "off";
    public static final String KEY_USER_BAND_DEFAULT_VALUE = "0";
    public static final String KEY_UB_FREQUENCY_DEFAULT_VALUE = "0";
    public static final String KEY_POSITION_DEFAULT_VALUE = "false";

    public static final String KEY_LNB_POWER_DEFAULT_VALUE = "on"/*18V"*/;
    public static final String KEY_22_KHZ_DEFAULT_VALUE = "auto";
    public static final String KEY_TONE_BURST_DEFAULT_VALUE = "none";
    public static final String KEY_DISEQC1_0_DEFAULT_VALUE = "none";
    public static final String KEY_DISEQC1_1_DEFAULT_VALUE = "none";
    public static final String KEY_MOTOR_DEFAULT_VALUE = "none";

    public static final String KEY_FUNCTION = "function";
    public static final String KEY_ADD_SATELLITE = "add_satellite";
    public static final String KEY_EDIT_SATELLITE = "edit_satellite";
    public static final String KEY_REMOVE_SATELLITE = "remove_satellite";
    public static final String KEY_ADD_TRANSPONDER = "add_transponder";
    public static final String KEY_EDIT_TRANSPONDER = "edit_transponder";
    public static final String KEY_REMOVE_TRANSPONDER = "remove_transponder";

    public static final String SECURITY_PASSWORD  = "security_password";
    public static final String TV_KEY_DTVKIT_SYSTEM = "tv_dtvkit_system";
    public static final String KEY_LASTWAHTCHED_CHANNELID = "key_lastwatched_channelid";
    public static final String KEY_ACTIVE_RECORD_COUNT = "key_active_record_count";
    public static final String KEY_RESET_DEFAULT_AUDIO_STREAM = "key_reset_default_audio_stream";

    //default value that is save by index
    public static final int KEY_SATALLITE_DEFAULT_VALUE_INDEX = 0;
    public static final int KEY_TRANSPONDER_DEFAULT_VALUE_INDEX = 0;
    public static final int KEY_LNB_TYPE_DEFAULT_INDEX_INDEX = 1;
    //unicable
    public static final int KEY_UNICABLE_DEFAULT_VALUE_INDEX = 0;
    public static final int KEY_UNICABLE_SWITCH_DEFAULT_VALUE_INDEX = 0;
    public static final int KEY_USER_BAND_DEFAULT_VALUE_INDEX = 0;
    public static final int KEY_UB_FREQUENCY_DEFAULT_VALUE_INDEX = 1284;
    public static final int KEY_POSITION_DEFAULT_VALUE_INDEX = 0;

    public static final int KEY_LNB_POWER_DEFAULT_VALUE_INDEX = 0;
    public static final int KEY_22_KHZ_DEFAULT_VALUE_INDEX = 1;
    public static final int KEY_TONE_BURST_DEFAULT_VALUE_INDEX = 0;
    public static final int KEY_DISEQC1_0_DEFAULT_VALUE_INDEX = 0;
    public static final int KEY_DISEQC1_1_DEFAULT_VALUE_INDEX = 0;
    public static final int KEY_MOTOR_DEFAULT_VALUE_INDEX = 0;
    public static final int KEY_DISEQC1_2_DISH_LIMITS_STATUS_DEFAULT_VALUE_INDEX = 0;
    public static final int KEY_DISEQC1_2_DISH_CURRENT_POSITION_DEFAULT_VALUE = 0;

    public static final int DTV_TYPE_DVBT = 0;
    public static final int DTV_TYPE_DVBC = 1;
    public static final int DTV_TYPE_ISDBT = 2;


    public static final int SIGNAL_QPSK = 1; // digital satellite
    public static final int SIGNAL_COFDM = 2; // digital terrestrial
    public static final int SIGNAL_QAM   = 4; // digital cable
    public static final int SIGNAL_ISDBT  = 5;
    public static final int SIGNAL_ANALOG = 8;

    public static final String[] DIALOG_SET_ITEM_UNICABLE_KEY_LIST = {KEY_UNICABLE_SWITCH, KEY_USER_BAND, KEY_UB_FREQUENCY, KEY_POSITION};

    /*public static final String KEY_DTVKIT_COUNTRY = "dtvkit_country";
    public static final String KEY_DTVKIT_MAIN_AUDIO_LANG = "main_audio_lang";
    public static final String KEY_DTVKIT_ASSIST_AUDIO_LANG = "assist_audio_lang";
    public static final String KEY_DTVKIT_MAIN_SUBTITLE_LANG = "main_subtitle_lang";
    public static final String KEY_DTVKIT_ASSIST_SUBTITLE_LANG = "assist_subtitle_lang";*///save in dtvkit

    //add for pvr record path setting
    public static final String KEY_PVR_RECORD_PATH = "pvr_record_path";

    //add for associate audio setting
    public static final String TV_KEY_AD_SWITCH = "ad_switch";

    //add automatic searching setting
    public static final String AUTO_SEARCHING_MODE       = "auto_searching_mode";
    public static final String AUTO_SEARCHING_HOUR       = "auto_searching_hour";
    public static final String AUTO_SEARCHING_MINUTE     = "auto_searching_minute";
    public static final String AUTO_SEARCHING_REPTITION  = "auto_searching_reptition";
    public static final String AUTO_SEARCHING_SIGNALTYPE = "channel_signal_type";

    //defines related to LiveTv
    public static final String TV_CURRENT_INPUTID = "tv_current_inputid";
    public static final String TV_ADTV_KEY = "ADTVInputService";
    public static final String TV_AV1_KEY = "AV1InputService";
    public static final String TV_AV2_KEY = "AV2InputService";

    public ParameterMananer(Context context, DtvkitGlueClient client) {
        this.mContext = context;
        this.mDtvkitGlueClient = client;
        this.mDataMananer = new DataMananer(context);
        this.mDvbsParaManager = DvbsParameterManager.getInstance(context);
    }

    public void setDtvkitGlueClient(DtvkitGlueClient client) {
        if (mDtvkitGlueClient == null) {
            mDtvkitGlueClient = client;
        }
    }

    public DvbsParameterManager getDvbsParaManager() {
        return mDvbsParaManager;
    }

    public void saveIntParameters(String key, int value) {
        /*SharedPreferences sp = mContext.getSharedPreferences("dish_parameter", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        editor.putInt(key, value);
        editor.commit();*/
        //Settings.System.putInt(mContext.getContentResolver(), key, value);
        DataProviderManager.putIntValue(mContext, key, value);
    }

    public int getIntParameters(String key) {
        int defValue = 0;
        switch (key) {
            case KEY_LNB_TYPE:
                defValue = 1;
                break;
            case KEY_UNICABLE_SWITCH:
                defValue = KEY_UNICABLE_SWITCH_DEFAULT_VALUE_INDEX;
                break;
            case KEY_USER_BAND:
                defValue = KEY_USER_BAND_DEFAULT_VALUE_INDEX;
                break;
            case KEY_UB_FREQUENCY:
                defValue = KEY_UB_FREQUENCY_DEFAULT_VALUE_INDEX;
                break;
            case KEY_POSITION:
                defValue = KEY_POSITION_DEFAULT_VALUE_INDEX;
                break;
            case KEY_UNICABLE:
                defValue = KEY_UNICABLE_SWITCH_DEFAULT_VALUE_INDEX;
                break;
            case KEY_LNB_POWER:
                defValue = KEY_LNB_POWER_DEFAULT_VALUE_INDEX;
                break;
            case KEY_22_KHZ:
                defValue = KEY_22_KHZ_DEFAULT_VALUE_INDEX;
                break;
            case KEY_TONE_BURST:
                defValue = KEY_TONE_BURST_DEFAULT_VALUE_INDEX;
                break;
            case KEY_DISEQC1_0:
                defValue = KEY_DISEQC1_0_DEFAULT_VALUE_INDEX;
                break;
            case KEY_DISEQC1_1:
                defValue = KEY_DISEQC1_1_DEFAULT_VALUE_INDEX;
                break;
            case KEY_MOTOR:
                defValue = KEY_MOTOR_DEFAULT_VALUE_INDEX;
                break;
            case KEY_DISEQC1_2_DISH_LIMITS_STATUS:
                defValue = KEY_MOTOR_DEFAULT_VALUE_INDEX;
                break;
            case KEY_DISEQC1_2_DISH_EAST_LIMITS:
                defValue = 180;
                break;
            case KEY_DISEQC1_2_DISH_WEST_LIMITS:
                defValue = 180;
                break;
            case KEY_DISEQC1_2_DISH_MOVE_DIRECTION:
                defValue = 0;
                break;
            case KEY_DISEQC1_2_DISH_MOVE_STEP:
                defValue = 1;
                break;
            case KEY_DISEQC1_2_DISH_CURRENT_POSITION:
                defValue = 0;
                break;
            case KEY_LNB_CUSTOM_LOW_MIN:
            case KEY_LNB_CUSTOM_HIGH_MIN:
                defValue = VALUE_LNB_CUSTOM_MIN;
                break;
            case KEY_LNB_CUSTOM_LOW_MAX:
            case KEY_LNB_CUSTOM_HIGH_MAX:
                defValue = VALUE_LNB_CUSTOM_MAX;
                break;
            case AUTO_SEARCHING_MODE:
            case AUTO_SEARCHING_REPTITION:
                defValue = 0;
                break;
            default:
                defValue = 0;
                break;
        }
        /*SharedPreferences sp = mContext.getSharedPreferences("dish_parameter", Context.MODE_PRIVATE);
        return sp.getInt(key, defValue);*/
        //return Settings.System.getInt(mContext.getContentResolver(), key, defValue);
        return DataProviderManager.getIntValue(mContext, key, defValue);
    }

    public void saveStringParameters(String key, String value) {
        /*SharedPreferences sp = mContext.getSharedPreferences("dish_parameter", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        editor.putString(key, value);
        editor.commit();*/
        //Settings.System.putString(mContext.getContentResolver(), key, value);
        DataProviderManager.putStringValue(mContext, key, value);
    }

    public String getStringParameters(String key) {
        String defValue = "";
        if (key == null) {
            return null;
        }
        switch (key) {
            case KEY_CURRENT_TYPE:
                defValue = ITEM_SATALLITE;
                break;
            case KEY_CURRENT_DIRECTION:
                defValue = ITEM_DIRECTION;
                break;
            case KEY_SATALLITE:
                defValue = KEY_SATALLITE_DEFAULT_VALUE;//ALL_SATALLITE[0];
                break;
            case KEY_TRANSPONDER:
                defValue = KEY_TRANSPONDER_DEFAULT_VALUE;//ALL_TRANSPONDER[0];
                break;
            case KEY_LNB_TYPE:
                defValue = KEY_LNB_TYPE_DEFAULT_VALUE;
                break;
            case KEY_UNICABLE_SWITCH:
                defValue = KEY_UNICABLE_SWITCH_DEFAULT_VALUE;
                break;
            case KEY_USER_BAND:
                defValue = KEY_USER_BAND_DEFAULT_VALUE;
                break;
            case KEY_UB_FREQUENCY:
                defValue = KEY_UB_FREQUENCY_DEFAULT_VALUE;
                break;
            case KEY_POSITION:
                defValue = KEY_POSITION_DEFAULT_VALUE;
                break;
            case KEY_UNICABLE:
                defValue = KEY_UNICABLE_SWITCH_DEFAULT_VALUE;
                break;
            case KEY_LNB_POWER:
                defValue = KEY_LNB_POWER_DEFAULT_VALUE;
                break;
            case KEY_22_KHZ:
                defValue = KEY_22_KHZ_DEFAULT_VALUE;
                break;
            case KEY_TONE_BURST:
                defValue = KEY_TONE_BURST_DEFAULT_VALUE;
                break;
            case KEY_DISEQC1_0:
                defValue = KEY_DISEQC1_0_DEFAULT_VALUE;
                break;
            case KEY_DISEQC1_1:
                defValue = KEY_DISEQC1_1_DEFAULT_VALUE;
                break;
            case KEY_MOTOR:
                defValue = KEY_MOTOR_DEFAULT_VALUE;
                break;
            case KEY_LNB_CUSTOM:
                defValue = "";
                break;
            case AUTO_SEARCHING_HOUR:
                defValue = "4";
                break;
            case AUTO_SEARCHING_MINUTE:
                defValue = "30";
                break;
            case KEY_DISEQC1_3_LOCATION_STRING:
                defValue = "off";
                break;
            case SECURITY_PASSWORD:
                defValue = "";
                break;
            case TV_KEY_DTVKIT_SYSTEM:
                defValue = "DVB-T";
                break;
            default:
                defValue = "";
                break;
        }
        /*SharedPreferences sp = mContext.getSharedPreferences("dish_parameter", Context.MODE_PRIVATE);
        String result = sp.getString(key, defValue);*/
        //String result = Settings.System.getString(mContext.getContentResolver(), key);
        String result = DataProviderManager.getStringValue(mContext, key, defValue);
        Log.d(TAG, "getStringParameters key = " + key + ", result = " + result);
        if (TextUtils.isEmpty(result)) {
            result = defValue;
        }
        return result;
    }

    public int getStrengthStatus() {
        int result = 0;
        try {
            JSONArray args1 = new JSONArray();
            args1.put(0);
            JSONObject jsonObj = DtvkitGlueClient.getInstance().request("Dvb.getFrontend", args1);
            if (jsonObj != null) {
                Log.d(TAG, "getStrengthStatus resultObj:" + jsonObj.toString());
            } else {
                Log.d(TAG, "getStrengthStatus then get null");
            }
            JSONObject data = null;
            if (jsonObj != null) {
                data = (JSONObject)jsonObj.get("data");
            }
            if (data == null || data.length() == 0) {
                return result;
            } else {
                result = (int)(data.get("strength"));
                return result;
            }
        } catch (Exception e) {
            Log.d(TAG, "getStrengthStatus Exception " + e.getMessage() + ", trace=" + e.getStackTrace());
            e.printStackTrace();
        }
        return result;
    }

    public int getQualityStatus() {
        int result = 0;
        try {
            JSONArray args1 = new JSONArray();
            args1.put(0);
            JSONObject jsonObj = DtvkitGlueClient.getInstance().request("Dvb.getFrontend", args1);
            if (jsonObj != null) {
                Log.d(TAG, "getQualityStatus resultObj:" + jsonObj.toString());
            } else {
                Log.d(TAG, "getQualityStatus then get null");
            }
            JSONObject data = null;
            if (jsonObj != null) {
                data = (JSONObject)jsonObj.get("data");
            }
            if (data == null || data.length() == 0) {
                return result;
            } else {
                result = (int)(data.get("integrity"));
                return result;
            }
        } catch (Exception e) {
            Log.d(TAG, "getQualityStatus Exception " + e.getMessage() + ", trace=" + e.getStackTrace());
            e.printStackTrace();
        }
        return result;
    }

    public void dishMove(int direction, int step) {
        String derection = null;
        switch (direction) {
            case 0:
                derection = "east";
                break;
            case 1:
                derection = "center";
                break;
            case 2:
                derection = "west";
                break;
            default:
                derection = "center";
                break;
        }
        try {
            JSONArray args1 = new JSONArray();
            args1.put(derection);
            args1.put(step);
            JSONObject resultObj = DtvkitGlueClient.getInstance().request("Dvbs.dishMove", args1);
            if (resultObj != null) {
                Log.d(TAG, "dishMove resultObj:" + resultObj.toString());
            } else {
                Log.d(TAG, "dishMove then get null");
            }
        } catch (Exception e) {
            Log.d(TAG, "dishMove Exception " + e.getMessage() + ", trace=" + e.getStackTrace());
            e.printStackTrace();
        }
        Log.d(TAG, "dishMove " + derection + "->" + step);
    }

    public void stopDishMove() {
        try {
            JSONArray args1 = new JSONArray();
            JSONObject resultObj = DtvkitGlueClient.getInstance().request("Dvbs.stopDishMove", args1);
            if (resultObj != null) {
                Log.d(TAG, "stopDishMove resultObj:" + resultObj.toString());
            } else {
                Log.d(TAG, "stopDishMove then get null");
            }
        } catch (Exception e) {
            Log.d(TAG, "stopDishMove Exception " + e.getMessage() + ", trace=" + e.getStackTrace());
            e.printStackTrace();
        }
        Log.d(TAG, "stopDishMove");
    }

    public void storeDishPosition(int position) {
        try {
            JSONArray args1 = new JSONArray();
            args1.put(position);
            JSONObject resultObj = DtvkitGlueClient.getInstance().request("Dvbs.storeDishPosition", args1);
            if (resultObj != null) {
                Log.d(TAG, "storeDishPosition resultObj:" + resultObj.toString());
            } else {
                Log.d(TAG, "storeDishPosition then get null");
            }
        } catch (Exception e) {
            Log.d(TAG, "storeDishPosition Exception " + e.getMessage() + ", trace=" + e.getStackTrace());
            e.printStackTrace();
        }
        Log.d(TAG, "storeDishPosition->" + position);
    }

    public void storeDishPosition(String sateName) {
        try {
            JSONArray args1 = new JSONArray();
            args1.put(sateName);
            JSONObject resultObj = DtvkitGlueClient.getInstance().request("Dvbs.storeDishPositionForSatellite", args1);
            if (resultObj != null) {
                Log.d(TAG, "storeDishPositionForSatellite resultObj:" + resultObj.toString());
            } else {
                Log.d(TAG, "storeDishPositionForSatellite then get null");
            }
        } catch (Exception e) {
            Log.d(TAG, "storeDishPositionForSatellite Exception " + e.getMessage() + ", trace=" + e.getStackTrace());
            e.printStackTrace();
        }
        Log.d(TAG, "storeDishPositionForSatellite:" + sateName);
    }

    public void moveDishToPosition(int position) {
        try {
            JSONArray args1 = new JSONArray();
            args1.put(position);
            JSONObject resultObj = DtvkitGlueClient.getInstance().request("Dvbs.moveDishToPosition", args1);
            if (resultObj != null) {
                Log.d(TAG, "moveDishToPosition resultObj:" + resultObj.toString());
            } else {
                Log.d(TAG, "moveDishToPosition then get null");
            }
        } catch (Exception e) {
            Log.d(TAG, "moveDishToPositions Exception " + e.getMessage() + ", trace=" + e.getStackTrace());
            e.printStackTrace();
        }
        Log.d(TAG, "moveDishToPosition->" + position);
    }

    public void moveDishToPosition(String sateName) {
        try {
            JSONArray args1 = new JSONArray();
            args1.put(sateName);
            JSONObject resultObj = DtvkitGlueClient.getInstance().request("Dvbs.moveDishToPositionForSatellite", args1);
            if (resultObj != null) {
                Log.d(TAG, "moveDishToPositionForSatellite resultObj:" + resultObj.toString());
            } else {
                Log.d(TAG, "moveDishToPositionForSatellite then get null");
            }
        } catch (Exception e) {
            Log.d(TAG, "moveDishToPositionForSatellite Exception " + e.getMessage() + ", trace=" + e.getStackTrace());
            e.printStackTrace();
        }
        Log.d(TAG, "moveDishToPositionForSatellite:" + sateName);
    }

    public void enableDishLimits(boolean status) {
        try {
            JSONArray args1 = new JSONArray();
            args1.put(status);
            JSONObject resultObj = DtvkitGlueClient.getInstance().request("Dvbs.enableDishLimits", args1);
            if (resultObj != null) {
                Log.d(TAG, "enableDishLimits resultObj:" + resultObj.toString());
            } else {
                Log.d(TAG, "enableDishLimits then get null");
            }
        } catch (Exception e) {
            Log.d(TAG, "enableDishLimits Exception " + e.getMessage() + ", trace=" + e.getStackTrace());
            e.printStackTrace();
        }
        Log.d(TAG, "enableDishLimits->" + status);
    }

    public void setDishELimits() {
        try {
            JSONObject resultObj = DtvkitGlueClient.getInstance().request("Dvbs.setDishEastLimits", new JSONArray());
            if (resultObj != null) {
                Log.d(TAG, "setDishELimits resultObj:" + resultObj.toString());
            } else {
                Log.d(TAG, "setDishELimits then get null");
            }
        } catch (Exception e) {
            Log.d(TAG, "setDishELimits Exception " + e.getMessage() + ", trace=" + e.getStackTrace());
            e.printStackTrace();
        }
        Log.d(TAG, "setDishELimits east");
    }

    public void setDishWLimits() {
        try {
            JSONObject resultObj = DtvkitGlueClient.getInstance().request("Dvbs.setDishWestLimits", new JSONArray());
            if (resultObj != null) {
                Log.d(TAG, "setDishWLimits resultObj:" + resultObj.toString());
            } else {
                Log.d(TAG, "setDishWLimits then get null");
            }
        } catch (Exception e) {
            Log.d(TAG, "setDishWLimits Exception " + e.getMessage() + ", trace=" + e.getStackTrace());
            e.printStackTrace();
        }
        Log.d(TAG, "setDishWLimits  west");
    }

    private JSONArray generateOrderedJSONArray(JSONObject obj) {
        JSONArray result = null;
        final String[] ALLKEY = {"name", "east", "long_pos", "unicable", "unicable_chan", "unicable_if", "unicable_position_b",
                                 "tone_burst", "c_switch", "u_switch", "motor_switch", "dish_pos", "lnb_type",
                                 "low_min_freq", "low_max_freq", "low_local_oscillator_frequency", "low_lnb_voltage", "low_tone_22k",
                                 "high_min_freq", "high_max_freq", "high_local_oscillator_frequency", "high_lnb_voltage", "high_tone_22k",
                                 "transponder"};
        if (obj != null && obj.length() > 0) {
            Object temp = null;
            try {
                for (int i = 0; i < ALLKEY.length; i++) {
                    temp = obj.get(ALLKEY[i]);
                    if (result == null) {
                        result = new JSONArray();
                    }
                    //Log.d(TAG, "generateOrderedJSONArray " + ALLKEY[i] + ":" + temp);
                    result.put(temp);
                }
            } catch (Exception e) {
                Log.d(TAG, "generateOrderedJSONArray Exception " + e.getMessage());
                result = null;
            }
        }
        return result;
    }

    private Object getValueFromJSONObject(JSONObject obj, String key, Object defaultValue) {
        Object result = null;
        if (obj != null && obj.length() > 0) {
            try {
                result = obj.get(key);
            } catch (Exception e) {
                Log.d(TAG, "getValueFromJSONObject Exception " + e.getMessage());
                result = defaultValue;
            }
        }
        return result;
    }

    public boolean importSatellitesAndLnbs() {
        boolean result = false;
        JSONObject resultObj = null;
        try {
            JSONArray args = new JSONArray();
            args.put(ConstantManager.DTVKIT_SATELLITE_DATA);
            args.put(ConstantManager.DTVKIT_LNBS_DATA);
            args.put(ConstantManager.DTVKIT_LOCATION_DATA);
            Log.d(TAG, "importDatabase->" + args.toString());
            resultObj = DtvkitGlueClient.getInstance().request("Dvbs.importDatabase", args);
            if (resultObj != null) {
                result = resultObj.getBoolean("accepted") && resultObj.getBoolean("data");
                Log.d(TAG, "importDatabase resultObj:" + resultObj.toString());
            } else {
                Log.d(TAG, "importDatabase result null");
            }
        } catch (Exception e) {
            Log.d(TAG, "importDatabase Exception " + e.getMessage() + ", trace=" + e.getStackTrace());
            e.printStackTrace();
        }
        return result;
    }

    public boolean importDatabase(String path) {
        boolean result = false;
        JSONObject resultObj = null;
        try {
            JSONArray args = new JSONArray();
            args.put(path);
            Log.d(TAG, "importDatabase->" + args.toString());
            resultObj = DtvkitGlueClient.getInstance().request("Dvbs.importDatabase", args);
            if (resultObj != null) {
                result = resultObj.getBoolean("accepted") && resultObj.getBoolean("data");
                Log.d(TAG, "importDatabase resultObj:" + resultObj.toString());
            } else {
                Log.d(TAG, "importDatabase result null");
            }
        } catch (Exception e) {
            Log.d(TAG, "importDatabase Exception " + e.getMessage() + ", trace=" + e.getStackTrace());
            e.printStackTrace();
        }
        return result;
    }

    public List<String> getCountryDisplayList() {
        List<String> result = new ArrayList<String>();
        try {
            JSONObject resultObj = getCountrys();
            Locale[] allLocale = Locale.getAvailableLocales();
            JSONArray data = null;
            if (resultObj != null) {
                data = (JSONArray)resultObj.get("data");
                if (data == null || data.length() == 0) {
                    return result;
                }
                for (int i = 0; i < data.length(); i++) {
                    String countryName = (String)(((JSONObject)(data.get(i))).get("country_name"));
                    if (allLocale != null && allLocale.length > 0) {
                        Log.d(TAG, "allLocale size = " + allLocale.length);
                        for (Locale one : allLocale) {
                            try {
                                String iso3Country = one.getISO3Country();
                                boolean isEqual = (countryName == null ? iso3Country == null :countryName.equalsIgnoreCase(iso3Country));
                                if (isEqual) {
                                    countryName = one.getDisplayCountry();
                                    break;
                                }
                            } catch (Exception e1) {
                                //Log.e(TAG, "getCountryDisplayList ios3 country Exception " + e1.getMessage() + ", trace=" + e1.getStackTrace() + " continue");
                                continue;
                            }
                        }
                    }
                    result.add(countryName);
                }

            } else {
                Log.d(TAG, "getCountryDisplayList then get null");
            }
        } catch (Exception e) {
            Log.d(TAG, "getCountryDisplayList Exception " + e.getMessage() + ", trace=" + e.getStackTrace());
            e.printStackTrace();
        }

        return result;
    }

    public List<Integer> getCountryCodeList() {
        List<Integer> result = new ArrayList<Integer>();
        try {
            JSONObject resultObj = getCountrys();
            JSONArray data = null;
            if (resultObj != null) {
                data = (JSONArray)resultObj.get("data");
                if (data == null || data.length() == 0) {
                    return result;
                }
                for (int i = 0; i < data.length(); i++) {
                    int countryCode = (int)(((JSONObject)(data.get(i))).get("country_code"));
                    result.add(countryCode);
                }

            } else {
                Log.d(TAG, "getCountryCodeList then get null");
            }
        } catch (Exception e) {
            Log.d(TAG, "getCountryCodeList Exception " + e.getMessage() + ", trace=" + e.getStackTrace());
            e.printStackTrace();
        }

        return result;
    }

    public int getCurrentCountryCode() {
        int result = -1;//-1 means can't find
        int[] currentInfo = getCurrentLangParameter();
        if (currentInfo != null && currentInfo[0] != -1) {
            result = currentInfo[0];
        }
        return result;
    }

    private JSONObject getCountrys() {
        JSONObject resultObj = null;
        try {
            JSONArray args1 = new JSONArray();
            resultObj = DtvkitGlueClient.getInstance().request("Dvb.getCountrys", args1);
            if (resultObj != null) {
                Log.d(TAG, "getCountrys resultObj:" + resultObj.toString());
            } else {
                Log.d(TAG, "getCountrys then get null");
            }
        } catch (Exception e) {
            Log.d(TAG, "getCountrys Exception " + e.getMessage() + ", trace=" + e.getStackTrace());
            e.printStackTrace();
        }
        return resultObj;
    }

    public List<String> getCurrentLangNameList() {
        List<String> result = new ArrayList<String>();
        int[] currentInfo = getCurrentLangParameter();
        if (currentInfo != null && currentInfo[0] != -1) {
            result = getLangNameList(currentInfo[0]);
        }
        return result;
    }

    public List<String> getCurrentSecondLangNameList() {
        List<String> result = new ArrayList<String>();
        int[] currentInfo = getCurrentLangParameter();
        if (currentInfo != null && currentInfo[0] != -1) {
            result = getSecondLangNameList(currentInfo[0]);
        }
        return result;
    }

    private List<String> getLangNameList(int countryCode) {
        List<String> result = new ArrayList<String>();
        try {
            JSONObject resultObj = getCountryLangs(countryCode);
            JSONArray data = null;
            if (resultObj != null) {
                data = (JSONArray)resultObj.get("data");
                if (data == null || data.length() == 0) {
                    return result;
                }
                for (int i = 0; i < data.length(); i++) {
                    String langName = (String)(((JSONObject)(data.get(i))).get("lang_ids"));
                    result.add(langName);
                }
            } else {
                Log.d(TAG, "getLangNameList then get null");
            }
        } catch (Exception e) {
            Log.d(TAG, "getLangNameList Exception " + e.getMessage() + ", trace=" + e.getStackTrace());
            e.printStackTrace();
        }
        return result;
    }

    private List<String> getSecondLangNameList(int countryCode) {
        List<String> result = new ArrayList<String>();
        try {
            JSONObject resultObj = getCountryLangs(countryCode);
            JSONArray data = null;
            if (resultObj != null) {
                data = (JSONArray)resultObj.get("data");
                if (data == null || data.length() == 0) {
                    return result;
                }
                for (int i = 0; i < data.length(); i++) {
                    String langName = (String)(((JSONObject)(data.get(i))).get("lang_ids"));
                    result.add(langName);
                }
                result.add("None");
            } else {
                Log.d(TAG, "getSecondLangNameList then get null");
            }
        } catch (Exception e) {
            Log.d(TAG, "getSecondLangNameList Exception " + e.getMessage() + ", trace=" + e.getStackTrace());
            e.printStackTrace();
        }
        return result;
    }

    private JSONObject getCountryLangs(int countryCode) {
        JSONObject resultObj = null;
        try {
            JSONArray args1 = new JSONArray();
            args1.put(countryCode);
            resultObj = DtvkitGlueClient.getInstance().request("Dvb.getCountryLangs", args1);
            if (resultObj != null) {
                if (DEBUG) {
                    Log.d(TAG, "getCountryLangs resultObj:" + resultObj.toString());
                }
            } else {
                Log.d(TAG, "getCountryLangs then get null");
            }
        } catch (Exception e) {
            Log.d(TAG, "getCountryLangs Exception " + e.getMessage() + ", trace=" + e.getStackTrace());
            e.printStackTrace();
        }
        return resultObj;
    }

    public JSONObject setCountryCodeByIndex(int index) {
        JSONObject resultObj = null;
        try {
            resultObj = getCountrys();
            JSONArray data = null;
            if (resultObj != null) {
                data = (JSONArray)resultObj.get("data");
                if (data == null || data.length() == 0) {
                    return resultObj;
                }
                resultObj = (JSONObject)(data.get(index));
                if (resultObj != null) {
                    Log.d(TAG, "setCountryCodeByIndex resultObj = " + resultObj.toString());
                    setCountry((int)resultObj.get("country_code"));
                }
            } else {
                Log.d(TAG, "setCountryCodeByIndex then get null");
            }
        } catch (Exception e) {
            Log.d(TAG, "setCountryCodeByIndex Exception " + e.getMessage() + ", trace=" + e.getStackTrace());
            e.printStackTrace();
        }
        return resultObj;
    }

    public int getCurrentCountryIndex() {
        int result = 0;
        int currentcountrycode = getCurrentCountryCode();
        try {
            JSONObject resultObj = getCountrys();
            JSONArray data = null;
            if (resultObj != null) {
                data = (JSONArray)resultObj.get("data");
                if (data == null || data.length() == 0) {
                    return result;
                }
                for (int i = 0; i < data.length(); i++) {
                    int countryCode = (int)(((JSONObject)(data.get(i))).get("country_code"));
                    if (countryCode == currentcountrycode) {
                        result = i;
                        break;
                    }
                }

            } else {
                Log.d(TAG, "getCurrentCountryIndex then get null");
            }
        } catch (Exception e) {
            Log.d(TAG, "getCurrentCountryIndex Exception " + e.getMessage() + ", trace=" + e.getStackTrace());
            e.printStackTrace();
        }
        return result;
    }

    public String getCurrentCountryIso3Name() {
        String result = null;
        int currentcountrycode = getCurrentCountryCode();
        try {
            JSONObject resultObj = getCountrys();
            JSONArray data = null;
            if (resultObj != null) {
                data = (JSONArray)resultObj.get("data");
                if (data == null || data.length() == 0) {
                    return result;
                }
                for (int i = 0; i < data.length(); i++) {
                    int countryCode = (int)(((JSONObject)(data.get(i))).get("country_code"));
                    if (countryCode == currentcountrycode) {
                        result = (String)(((JSONObject)(data.get(i))).get("country_name"));;
                        break;
                    }
                }
            } else {
                Log.d(TAG, "getCurrentCountryIso3Name then get null");
            }
        } catch (Exception e) {
            Log.d(TAG, "getCurrentCountryIso3Name Exception " + e.getMessage() + ", trace=" + e.getStackTrace());
            e.printStackTrace();
        }
        return result;
    }

    public int getCountryCodeByIndex(int index) {
        int countrycode = 0;
        JSONObject resultObj = null;
        try {
            resultObj = getCountrys();
            JSONArray data = null;
            if (resultObj != null) {
                data = (JSONArray)resultObj.get("data");
                if (data == null || data.length() == 0) {
                    return countrycode;
                }
                resultObj = (JSONObject)(data.get(index));
                if (resultObj != null) {
                    Log.d(TAG, "getCountryCodeByIndex resultObj = " + resultObj.toString());
                    countrycode = ((int)resultObj.get("country_code"));
                }
            } else {
                Log.d(TAG, "getCountryCodeByIndex then get null");
            }
        } catch (Exception e) {
            Log.d(TAG, "getCountryCodeByIndex Exception " + e.getMessage() + ", trace=" + e.getStackTrace());
            e.printStackTrace();
        }
        return countrycode;
    }

    private JSONObject setCountry(int countryCode) {
        JSONObject resultObj = null;
        try {
            JSONArray args1 = new JSONArray();
            args1.put(countryCode);
            resultObj = DtvkitGlueClient.getInstance().request("Dvb.setCountry", args1);
            if (resultObj != null) {
                Log.d(TAG, "setCountry resultObj:" + resultObj.toString());
            } else {
                Log.d(TAG, "setCountry then get null");
            }
        } catch (Exception e) {
            Log.d(TAG, "setCountry Exception " + e.getMessage() + ", trace=" + e.getStackTrace());
            e.printStackTrace();
        }
        return resultObj;
    }

    private int getLangPositionByIndex(int index) {
        int result = 0;
        try {
            JSONObject resultObj = getCountryLangs(getCurrentCountryCode());
            JSONArray data = null;
            if (resultObj != null) {
                data = (JSONArray)resultObj.get("data");
                if (data == null || data.length() == 0) {
                    return result;
                }
                for (int i = 0; i < data.length(); i++) {
                    int langIndex = (int)(((JSONObject)(data.get(i))).get("lang_index"));
                    if (index == langIndex) {
                        result = i;
                    }
                }
            } else {
                Log.d(TAG, "getLangPositionByIndex then get null");
            }
        } catch (Exception e) {
            Log.d(TAG, "getLangPositionByIndex Exception " + e.getMessage() + ", trace=" + e.getStackTrace());
            e.printStackTrace();
        }
        return result;
    }

    private int getSecondLangPositionByIndex(int index) {
        int result = 0;
        try {
            JSONObject resultObj = getCountryLangs(getCurrentCountryCode());
            JSONArray data = null;
            if (resultObj != null) {
                data = (JSONArray)resultObj.get("data");
                if (data == null || data.length() == 0) {
                    return result;
                }
                if (index >= data.length()) {
                    result = data.length();
                } else {
                    for (int i = 0; i < data.length(); i++) {
                        int langIndex = (int)(((JSONObject)(data.get(i))).get("lang_index"));
                        if (index == langIndex) {
                            result = i;
                        }
                    }
                }
            } else {
                Log.d(TAG, "getSecondLangPositionByIndex then get null");
            }
        } catch (Exception e) {
            Log.d(TAG, "getSecondLangPositionByIndex Exception " + e.getMessage() + ", trace=" + e.getStackTrace());
            e.printStackTrace();
        }
        return result;
    }

    private String getLangNameByIndex(int index) {
        String result = null;
        try {
            JSONObject resultObj = getCountryLangs(getCurrentCountryCode());
            JSONArray data = null;
            if (resultObj != null) {
                data = (JSONArray)resultObj.get("data");
                if (data == null || data.length() == 0) {
                    return result;
                }
                if (index < data.length()) {
                    result = (String)(((JSONObject)(data.get(index))).get("lang_ids"));
                }
            } else {
                Log.d(TAG, "getLangNameByIndex then get null");
            }
        } catch (Exception e) {
            Log.d(TAG, "getLangNameByIndex Exception " + e.getMessage() + ", trace=" + e.getStackTrace());
            e.printStackTrace();
        }
        return result;
    }

    public int getCurrentMainAudioLangId() {
        int id = 0;
        int[] currentInfo = getCurrentLangParameter();
        if (currentInfo != null && currentInfo[1] != -1) {
            id = getLangPositionByIndex(currentInfo[1]);
        }
        return id;
    }

    public String getCurrentMainAudioLangName() {
        String result = null;
        int[] currentInfo = getCurrentLangParameter();
        if (currentInfo != null && currentInfo[1] != -1) {
            result = getLangNameByIndex(currentInfo[1]);
        }
        return result;
    }

    public int getCurrentSecondAudioLangId() {
        int id = 0;
        int[] currentInfo = getCurrentLangParameter();
        if (currentInfo != null && currentInfo[2] != -1) {
            id = getSecondLangPositionByIndex(currentInfo[2]);
        }
        return id;
    }

    public String getCurrentSecondAudioLangName() {
        String result = null;
        int[] currentInfo = getCurrentLangParameter();
        if (currentInfo != null && currentInfo[2] != -1) {
            result = getLangNameByIndex(currentInfo[2]);
        }
        return result;
    }

    public int getCurrentMainSubLangId() {
        int id = 0;
        int[] currentInfo = getCurrentLangParameter();
        if (currentInfo != null && currentInfo[3] != -1) {
            id = getLangPositionByIndex(currentInfo[3]);
        }
        return id;
    }

    public int getCurrentSecondSubLangId() {
        int id = 0;
        int[] currentInfo = getCurrentLangParameter();
        if (currentInfo != null && currentInfo[4] != -1) {
            id = getSecondLangPositionByIndex(currentInfo[4]);
        }
        return id;
    }

    public int[] getCurrentLangParameter() {
        int[] result = {-1, -1, -1, -1, -1};
        JSONObject resultObj = null;
        try {
            JSONArray args1 = new JSONArray();
            JSONObject data = null;
            resultObj = DtvkitGlueClient.getInstance().request("Dvb.getcurrentCountryInfos", args1);
            if (resultObj != null) {
                data = (JSONObject)resultObj.get("data");
                if (data == null || data.length() == 0) {
                    return result;
                }
                resultObj = data;
                if (resultObj != null) {
                    Log.d(TAG, "getCurrentLangParameter resultObj = " + resultObj.toString());
                    result[0] = (int)(resultObj.get("country_code"));
                    result[1] = (int)(resultObj.get("a_pri_lang_id"));
                    result[2] = (int)(resultObj.get("a_sec_lang_id"));
                    result[3] = (int)(resultObj.get("t_pri_lang_id"));
                    result[4] = (int)(resultObj.get("t_sec_lang_id"));
                }
            } else {
                Log.d(TAG, "getCurrentLangParameter then get null");
            }
        } catch (Exception e) {
            Log.d(TAG, "getCurrentLangParameter Exception " + e.getMessage() + ", trace=" + e.getStackTrace());
            e.printStackTrace();
        }
        return result;
    }

    public int getLangIndexCodeByPosition(int position) {
        int langIndex = 0;
        try {
            int currentCountryCode = getCurrentCountryCode();
            JSONObject resultObj = getCountryLangs(currentCountryCode);
            JSONArray data = null;
            if (resultObj != null) {
                data = (JSONArray)resultObj.get("data");
                if (data == null || data.length() == 0) {
                    return langIndex;
                }
                resultObj = (JSONObject)(data.get(position));
                if (resultObj != null) {
                    Log.d(TAG, "getLangIndexCodeByIndex resultObj = " + resultObj.toString());
                    langIndex = ((int)resultObj.get("lang_index"));
                }
            } else {
                Log.d(TAG, "getLangIndexCodeByPosition then get null");
            }
        } catch (Exception e) {
            Log.d(TAG, "getLangIndexCodeByPosition Exception " + e.getMessage() + ", trace=" + e.getStackTrace());
            e.printStackTrace();
        }
        return langIndex;
    }

    public int getSecondLangIndexCodeByPosition(int position) {
        int langIndex = 0;
        try {
            int currentCountryCode = getCurrentCountryCode();
            JSONObject resultObj = getCountryLangs(currentCountryCode);
            JSONArray data = null;
            if (resultObj != null) {
                data = (JSONArray)resultObj.get("data");
                if (data == null || data.length() == 0) {
                    return langIndex;
                }
                if (position >= data.length()) {
                    langIndex = 255;
                } else {
                    resultObj = (JSONObject)(data.get(position));
                    if (resultObj != null) {
                        Log.d(TAG, "getSecondLangIndexCodeByPosition resultObj = " + resultObj.toString());
                        langIndex = ((int)resultObj.get("lang_index"));
                    }
                }
            } else {
                Log.d(TAG, "getSecondLangIndexCodeByPosition then get null");
            }
        } catch (Exception e) {
            Log.d(TAG, "getSecondLangIndexCodeByPosition Exception " + e.getMessage() + ", trace=" + e.getStackTrace());
            e.printStackTrace();
        }
        return langIndex;
    }

    public JSONObject setPrimaryAudioLangByPosition(int position) {
        JSONObject resultObj = null;
        try {
            int langIndex = getLangIndexCodeByPosition(position);
            resultObj = setPrimaryAudioLangId(langIndex);
            if (resultObj != null) {
                Log.d(TAG, "setPrimaryAudioLangByIndex resultObj:" + resultObj.toString());
            } else {
                Log.d(TAG, "setPrimaryAudioLangByIndex then get null");
            }
        } catch (Exception e) {
            Log.d(TAG, "setPrimaryAudioLangByIndex Exception " + e.getMessage() + ", trace=" + e.getStackTrace());
            e.printStackTrace();
        }
        return resultObj;
    }

    public JSONObject setPrimaryAudioLangId(int langIndex) {
        JSONObject resultObj = null;
        try {
            JSONArray args1 = new JSONArray();
            args1.put(langIndex);
            resultObj = DtvkitGlueClient.getInstance().request("Dvb.setPrimaryAudioLangId", args1);
            if (resultObj != null) {
                Log.d(TAG, "setPrimaryAudioLangId resultObj:" + resultObj.toString());
            } else {
                Log.d(TAG, "setPrimaryAudioLangId then get null");
            }
        } catch (Exception e) {
            Log.d(TAG, "setPrimaryAudioLangId Exception " + e.getMessage() + ", trace=" + e.getStackTrace());
            e.printStackTrace();
        }
        return resultObj;
    }

    public JSONObject setSecondaryAudioLangByPosition(int position) {
        JSONObject resultObj = null;
        try {
            int langIndex = getSecondLangIndexCodeByPosition(position);
            resultObj = setSecondaryAudioLangId(langIndex);
            if (resultObj != null) {
                Log.d(TAG, "setSecondaryAudioLangByIndex resultObj:" + resultObj.toString());
            } else {
                Log.d(TAG, "setSecondaryAudioLangByIndex then get null");
            }
        } catch (Exception e) {
            Log.d(TAG, "setSecondaryAudioLangByIndex Exception " + e.getMessage() + ", trace=" + e.getStackTrace());
            e.printStackTrace();
        }
        return resultObj;
    }

    public JSONObject setSecondaryAudioLangId(int langIndex) {
        JSONObject resultObj = null;
        try {
            JSONArray args1 = new JSONArray();
            args1.put(langIndex);
            resultObj = DtvkitGlueClient.getInstance().request("Dvb.setSecondaryAudioLangId", args1);
            if (resultObj != null) {
                Log.d(TAG, "setSecondaryAudioLangId resultObj:" + resultObj.toString());
            } else {
                Log.d(TAG, "setSecondaryAudioLangId then get null");
            }
        } catch (Exception e) {
            Log.d(TAG, "setSecondaryAudioLangId Exception " + e.getMessage() + ", trace=" + e.getStackTrace());
            e.printStackTrace();
        }
        return resultObj;
    }

    public JSONObject clearUserAudioSelect() {
        JSONObject resultObj = null;
        try {
            JSONArray args1 = new JSONArray();
            resultObj = DtvkitGlueClient.getInstance().request("Dvb.ClearUserAudioSelect", args1);
            if (resultObj != null) {
                Log.d(TAG, "clearUserAudioSelect resultObj:" + resultObj.toString());
            } else {
                Log.d(TAG, "clearUserAudioSelect then get null");
            }
        } catch (Exception e) {
            Log.d(TAG, "clearUserAudioSelect Exception " + e.getMessage() + ", trace=" + e.getStackTrace());
            e.printStackTrace();
        }
        return resultObj;
    }

    public JSONObject setPrimaryTextLangByPosition(int position) {
        JSONObject resultObj = null;
        try {
            int langIndex = getLangIndexCodeByPosition(position);
            resultObj = setPrimaryTextLangId(langIndex);
            if (resultObj != null) {
                Log.d(TAG, "setPrimaryTextLangByIndex resultObj:" + resultObj.toString());
            } else {
                Log.d(TAG, "setPrimaryTextLangByIndex then get null");
            }
        } catch (Exception e) {
            Log.d(TAG, "setPrimaryTextLangByIndex Exception " + e.getMessage() + ", trace=" + e.getStackTrace());
            e.printStackTrace();
        }
        return resultObj;
    }

    private JSONObject setPrimaryTextLangId(int langIndex) {
        JSONObject resultObj = null;
        try {
            JSONArray args1 = new JSONArray();
            args1.put(langIndex);
            resultObj = DtvkitGlueClient.getInstance().request("Dvb.setPrimaryTextLangId", args1);
            if (resultObj != null) {
                Log.d(TAG, "setPrimaryTextLangId resultObj:" + resultObj.toString());
            } else {
                Log.d(TAG, "setPrimaryTextLangId then get null");
            }
        } catch (Exception e) {
            Log.d(TAG, "setPrimaryTextLangId Exception " + e.getMessage() + ", trace=" + e.getStackTrace());
            e.printStackTrace();
        }
        return resultObj;
    }

    public JSONObject setSecondaryTextLangByPosition(int position) {
        JSONObject resultObj = null;
        try {
            int langIndex = getSecondLangIndexCodeByPosition(position);
            resultObj = setSecondaryTextLangId(langIndex);
            if (resultObj != null) {
                Log.d(TAG, "setSecondaryTextLangByIndex resultObj:" + resultObj.toString());
            } else {
                Log.d(TAG, "setSecondaryTextLangByIndex then get null");
            }
        } catch (Exception e) {
            Log.d(TAG, "setSecondaryTextLangByIndex Exception " + e.getMessage() + ", trace=" + e.getStackTrace());
            e.printStackTrace();
        }
        return resultObj;
    }

    private JSONObject setSecondaryTextLangId(int langIndex) {
        JSONObject resultObj = null;
        try {
            JSONArray args1 = new JSONArray();
            args1.put(langIndex);
            resultObj = DtvkitGlueClient.getInstance().request("Dvb.setSecondaryTextLangId", args1);
            if (resultObj != null) {
                Log.d(TAG, "setSecondaryTextLangId resultObj:" + resultObj.toString());
            } else {
                Log.d(TAG, "setSecondaryTextLangId then get null");
            }
        } catch (Exception e) {
            Log.d(TAG, "setSecondaryTextLangId Exception " + e.getMessage() + ", trace=" + e.getStackTrace());
            e.printStackTrace();
        }
        return resultObj;
    }

    private JSONObject getTeletextCharset() {
        JSONObject resultObj = null;
        try {
            JSONArray args = new JSONArray();
            resultObj = DtvkitGlueClient.getInstance().request("Dvb.getTeletextCharacterDesignation", args);
            if (resultObj != null) {
                Log.d(TAG, "getTeletextCharset resultObj:" + resultObj.toString());
            } else {
                Log.d(TAG, "getTeletextCharset then get null");
            }
        } catch (Exception e) {
            Log.d(TAG, "getTeletextCharset Exception " + e.getMessage() + ", trace=" + e.getStackTrace());
        }
        return resultObj;
    }

    public List<String> getTeletextCharsetNameList() {
        List<String> result = new ArrayList<String>();
        try {
            JSONObject resultObj = getTeletextCharset();
            JSONArray data = null;
            if (resultObj != null) {
                data = (JSONArray)resultObj.get("data");
                if (data == null || data.length() == 0) {
                    return result;
                }
                String temp;
                for (int i = 0; i < data.length(); i++) {
                    temp = (String)(((JSONObject)(data.get(i))).get("tt_chara_design"));
                    result.add(temp);
                }
            } else {
                Log.d(TAG, "getTeletextCharsetNameList then get null");
            }
        } catch (Exception e) {
            Log.e(TAG, "getTeletextCharsetNameList Exception = " + e.getMessage());
            return result;
        }
        return result;
    }

    public int getCurrentTeletextCharsetIndex() {
        int result = 0;
        try {
            JSONObject resultObj = getTeletextCharset();
            JSONArray data = null;
            if (resultObj != null) {
                data = (JSONArray)resultObj.get("data");
                if (data == null || data.length() == 0) {
                    return result;
                }
                JSONObject temp;
                boolean select;
                for (int i = 0; i < data.length(); i++) {
                    temp = data.getJSONObject(i);
                    if (temp != null && temp.length() > 0) {
                        select = temp.getBoolean("selected");
                        if (select) {
                            result = i;
                            break;
                        }
                    }
                }
            } else {
                Log.d(TAG, "getCurrentTeletextCharsetIndex then get null");
            }
        } catch (Exception e) {
            Log.d(TAG, "getCurrentTeletextCharsetIndex Exception " + e.getMessage() + ", trace=" + e.getStackTrace());
        }
        return result;
    }

    private String getTeletextCharsetByPosition(int position) {
        String result = null;
        try {
            JSONObject resultObj = getTeletextCharset();
            JSONArray data = null;
            if (resultObj != null) {
                data = (JSONArray)resultObj.get("data");
                if (data == null || data.length() == 0) {
                    return result;
                }
                JSONObject temp;
                boolean select;
                if (position >= 0 && position < data.length()) {
                    temp = data.getJSONObject(position);
                    if (temp != null && temp.length() > 0) {
                        result = temp.getString("tt_chara_design");
                    }
                }
            } else {
                Log.d(TAG, "getCurrentTeletextCharsetByPosition then get null");
            }
        } catch (Exception e) {
            Log.d(TAG, "getCurrentTeletextCharsetByPosition Exception " + e.getMessage() + ", trace=" + e.getStackTrace());
        }
        return result;
    }

    private JSONObject setCurrentTeletextCharset(String charset) {
        JSONObject resultObj = null;
        try {
            JSONArray args = new JSONArray();
            args.put(charset);
            resultObj = DtvkitGlueClient.getInstance().request("Dvb.setTeletextCharacterDesignation", args);
            if (resultObj != null) {
                Log.d(TAG, "setCurrentTeletextCharset resultObj:" + resultObj.toString());
            } else {
                Log.d(TAG, "setCurrentTeletextCharset then get null");
            }
        } catch (Exception e) {
            Log.d(TAG, "setCurrentTeletextCharset Exception " + e.getMessage() + ", trace=" + e.getStackTrace());
        }
        return resultObj;
    }

    public JSONObject setCurrentTeletextCharsetByPosition(int position) {
        JSONObject resultObj = null;
        String charSet = getTeletextCharsetByPosition(position);
        if (charSet != null) {
            resultObj = setCurrentTeletextCharset(charSet);
        } else {
            Log.d(TAG, "setCurrentTeletextCharsetByPosition null charSet");
        }
        return resultObj;
    }

    public List<String> getChannelTable(int countryCode, boolean isDvbt, boolean isDvbt2) {
        int dtvType = isDvbt?DTV_TYPE_DVBT:DTV_TYPE_DVBC;
        return getRfChannelTable(countryCode, dtvType, isDvbt2);
    }

    public List<String> getIsdbtChannelTable(int countryCode) {
        return getRfChannelTable(countryCode, DTV_TYPE_ISDBT, false);
    }
    public List<String> getRfChannelTable(int countryCode, int dtvType, boolean isDvbt2) {
        List<String> result = new ArrayList<String>();
        try {
            JSONObject resultObj = null;
            JSONArray args = new JSONArray();
            if (dtvType == DTV_TYPE_DVBT) {
                args.put(isDvbt2);
                args.put(countryCode);
                resultObj = DtvkitGlueClient.getInstance().request("Dvbt.getTerRfChannelTable", args);
            } else if (dtvType == DTV_TYPE_DVBC) {
                args.put(countryCode);
                resultObj = DtvkitGlueClient.getInstance().request("Dvbc.getCabRfChannelTable", args);
            } else if (dtvType == DTV_TYPE_ISDBT) {
                args.put(countryCode);
                resultObj = DtvkitGlueClient.getInstance().request("Isdbt.getIsdbRfChannelTable", args);
            }

            JSONArray data = null;
            if (resultObj != null) {
                data = (JSONArray)resultObj.get("data");
                if (data == null || data.length() == 0) {
                    return result;
                }
                for (int i = 0; i < data.length(); i++) {
                    int index = (int)(((JSONObject)(data.get(i))).get("index"));
                    String name = (String)(((JSONObject)(data.get(i))).get("name"));
                    int frequency = (int)(((JSONObject)(data.get(i))).get("freq"));
                    String collection = String.valueOf(index) + "," + name + "," + String.valueOf(frequency);
                    result.add(collection);
                }
            } else {
                Log.d(TAG, "getChannelTable then get null");
            }
        } catch (Exception e) {
            Log.e(TAG, "getChannelTable Exception = " + e.getMessage());
            return result;
        }
        return result;
    }

    public JSONObject startTuneAction() {
        JSONObject resultObj = null;
        try {
            JSONArray args = initTuneActionData();
            if (args == null || args.length() == 0) {
                Log.d(TAG, "startTuneAction null args");
                return null;
            }
            Log.i(TAG, "startTuneAction:" + args.toString());
            resultObj = DtvkitGlueClient.getInstance().request("Dvbs.tuneActionStartEx", args);
            if (resultObj != null) {
                Log.d(TAG, "startTuneAction resultObj:" + resultObj.toString());
            } else {
                Log.d(TAG, "startTuneAction then get null");
            }
        } catch (Exception e) {
            Log.d(TAG, "startTuneAction Exception " + e.getMessage() + ", trace=" + e.getStackTrace());
            e.printStackTrace();
        }
        return resultObj;
    }

    public JSONObject stopTuneAction() {
        JSONObject resultObj = null;
        try {
            JSONArray args = new JSONArray();
            Log.i(TAG, "stopTuneAction:" + args.toString());
            resultObj = DtvkitGlueClient.getInstance().request("Dvbs.tuneActionStop", args);
            if (resultObj != null) {
                Log.d(TAG, "stopTuneAction resultObj:" + resultObj.toString());
            } else {
                Log.d(TAG, "stopTuneAction then get null");
            }
        } catch (Exception e) {
            Log.d(TAG, "stopTuneAction Exception " + e.getMessage() + ", trace=" + e.getStackTrace());
            e.printStackTrace();
        }
        return resultObj;
    }

    private JSONArray initTuneActionData() {
        JSONArray result = new JSONArray();
        String lnb = getDvbsParaManager().getCurrentLnbId();
        String sate = getDvbsParaManager().getCurrentSatellite();
        String tp = getDvbsParaManager().getCurrentTransponder();
        int tpFreq = getDvbsParaManager().getSatelliteWrap().getTransponderByName(sate, tp).getFreq();
        String tpPolarity = getDvbsParaManager().getSatelliteWrap().getTransponderByName(sate, tp).getPolarity();
        int tpSymbol = getDvbsParaManager().getSatelliteWrap().getTransponderByName(sate, tp).getSymbol();
        String tpFec = getDvbsParaManager().getSatelliteWrap().getTransponderByName(sate, tp).getFecMode();
        String tpSystem = getDvbsParaManager().getSatelliteWrap().getTransponderByName(sate, tp).getSystem();
        boolean tpIsDvbs2 = "DVBS2".equals(tpSystem) ? true : false;
        String modulation = getDvbsParaManager().getSatelliteWrap().getTransponderByName(sate, tp).getModulation();

        result.put(sate);
        result.put(tpFreq);
        result.put(tpPolarity);
        result.put(tpSymbol);
        result.put(tpFec);
        result.put(tpIsDvbs2);
        result.put(modulation);
        return result;
    }

    public boolean getHearingImpairedSwitchStatus() {
        boolean result = false;
        try {
            JSONArray args = new JSONArray();
            result = DtvkitGlueClient.getInstance().request("Player.getSubtitleHardHearingOn", args).getBoolean("data");
        } catch (Exception e) {
            Log.d(TAG, "getHearingImpairedSwitchStatus Exception " + e.getMessage() + ", trace=" + e.getStackTrace());
            e.printStackTrace();
        }
        Log.i(TAG, "getHearingImpairedSwitchStatus on = " + result);
        return result;
    }

    public void setHearingImpairedSwitchStatus(boolean on) {
        try {
            JSONArray args = new JSONArray();
            args.put(on);
            DtvkitGlueClient.getInstance().request("Player.setSubtitleHardHearingOn", args);
            Log.i(TAG, "setHearingImpairedSwitchStatus:" + on);
        } catch (Exception e) {
            Log.e(TAG, "setHearingImpairedSwitchStatus " + e.getMessage());
            e.printStackTrace();
        }
    }

    /*
    * obj: {"accepted":true,"data":[{"favourite":true,"name":"StreamSpark Network_1","net_id":1000}, {"favourite":false,"name":"StreamSpark Network_2","net_id":12000}]}
    */
    public JSONArray getNetworksOfRegion() {
        JSONArray result = null;
        if (PropSettingManager.getBoolean("vendor.sys.tv.debug.networks", false)) {
            result = creatTestNetworks();
            return result;
        }
        try {
            JSONObject obj = DtvkitGlueClient.getInstance().request("Dvb.getNetworksOfRegion", new JSONArray());
            if (obj != null) {
                Log.d(TAG, "getNetworksOfRegion resultObj:" + obj.toString());
            } else {
                Log.d(TAG, "getNetworksOfRegion then get null");
                return result;
            }
            JSONArray netWorks = obj.getJSONArray("data");
            result = netWorks;
            for (int i = 0; i < netWorks.length(); i++) {
                JSONObject netWork = netWorks.getJSONObject(i);
                Log.i(TAG, "getNetworksOfRegion netWork = " + netWork.toString());
            }
        } catch (Exception e) {
            Log.e(TAG, "getNetworksOfRegion Exception = " + e.getMessage());
        }
        return result;
    }

    private JSONArray creatTestNetworks() {
        JSONArray result = new JSONArray();
        for (int i = 0; i < 3; i++) {
            try {
                JSONObject obj = new JSONObject();
                if (i == 1) {
                    obj.put("favourite", true);
                } else {
                    obj.put("favourite", false);
                }
                obj.put("name", "StreamSpark Network_" + i);
                obj.put("net_id", 1000 + i);
                Log.i(TAG, "creatTestNetworks netWork = " + obj.toString());
                result.put(obj);
            } catch (Exception e) {
                Log.d(TAG, "creatTestNetworks Exception = " + e.getMessage());
            }
        }
        return result;
    }

    public JSONObject setNetworkPreferedOfRegion(int networkId) {
        JSONObject resultObj = null;
        if (networkId == -1) {
            Log.d(TAG, "setNetworkPreferedOfRegion invalid networkId");
            return resultObj;
        } else {
            Log.d(TAG, "setNetworkPreferedOfRegion networkId = " + networkId);
        }
        try {
            JSONArray args = new JSONArray();
            args.put(networkId);
            resultObj = DtvkitGlueClient.getInstance().request("Dvb.setNetworkPreferedOfRegion", args);
            if (resultObj != null) {
                Log.d(TAG, "setNetworkPreferedOfRegion resultObj:" + resultObj.toString());
            } else {
                Log.d(TAG, "setNetworkPreferedOfRegion then get null");
            }
        } catch (Exception e) {
            Log.d(TAG, "setNetworkPreferedOfRegion Exception " + e.getMessage() + ", trace=" + e.getStackTrace());
            e.printStackTrace();
        }
        return resultObj;
    }

    public String getNetworkName(JSONObject obj) {
        String result = null;
        if (obj != null && obj.length() > 0) {
            try {
                result = obj.getString("name");
            } catch (Exception e) {
                Log.d(TAG, "getNetworkName Exception = " + e.getMessage());
            }
        }
        return result;
    }

    public int getNetworkId(JSONObject obj) {
        int result = -1;//invalid
        if (obj != null && obj.length() > 0) {
            try {
                result = obj.getInt("net_id");
            } catch (Exception e) {
                Log.d(TAG, "getNetworkId1 Exception = " + e.getMessage());
            }
        }
        return result;
    }

    public int getNetworkId(JSONArray array, int index) {
        int result = -1;//invalid
        if (array != null && array.length() > 0) {
            try {
                result = array.getJSONObject(index).getInt("net_id");
            } catch (Exception e) {
                Log.d(TAG, "getNetworkId2 Exception = " + e.getMessage());
            }
        }
        return result;
    }

    public int getCurrentNetworkId(JSONArray array) {
        int result = -1;//invalid
        if (array != null && array.length() > 0) {
            for (int i = 0; i < array.length(); i++) {
                try {
                    if (array.getJSONObject(i).getBoolean("favourite")) {
                        result = array.getJSONObject(i).getInt("net_id");
                        break;
                    }
                } catch (Exception e) {
                    Log.d(TAG, "getDefaultNetworkId Exception = " + e.getMessage());
                    break;
                }
            }
        }
        return result;
    }

    public int getCurrentNetworkIndex(JSONArray array) {
        int result = -1;//invalid
        if (array != null && array.length() > 0) {
            for (int i = 0; i < array.length(); i++) {
                try {
                    if (array.getJSONObject(i).getBoolean("favourite")) {
                        result = i;
                        break;
                    }
                } catch (Exception e) {
                    Log.d(TAG, "getDefaultNetworkIndex Exception = " + e.getMessage());
                    break;
                }
            }
        }
        return result;
    }

    public JSONObject getJSONObjectFromJSONArray(JSONArray array, int index) {
        JSONObject result = null;
        if (array != null && array.length() > 0) {
            try {
                result = array.getJSONObject(index);
            } catch (Exception e) {
                Log.d(TAG, "getJSONObject Exception = " + e.getMessage());
            }
        }
        return result;
    }

    public boolean needConfirmNetWorkInfomation(JSONArray array) {
        boolean result = false;
        final String NORWAY_ISO3_NAME = "nor";
        String currentCountryName = getCurrentCountryIso3Name();
        if (NORWAY_ISO3_NAME.equalsIgnoreCase(currentCountryName)) {
            if (array != null && array.length() > 1) {
                result = true;
            }
        }
        return result;
    }

    public boolean checkIsGermanyCountry() {
        final String GERMANY_ISO3_NAME = "deu";
        String currentCountryName = getCurrentCountryIso3Name();
        if (GERMANY_ISO3_NAME.equalsIgnoreCase(currentCountryName)) {
            return true;
        }
        return false;
    }

    // operatorsType = [{"operators_name":"KDG","operators_value":0},{"operators_name":"Unitymedia","operators_value":1},{"operators_name":"Other Operators","operators_value":2}]
    public JSONArray getOperatorsTypeList(int tunerType) {
        JSONArray operatorsType = null;
        try {
            JSONArray args = new JSONArray();
            args.put(tunerType);
            JSONObject obj = DtvkitGlueClient.getInstance().request("Dvb.GetOperatorsTypeList",args);
        if (obj != null) {
            Log.d(TAG, "getOperatorsTypeList resultObj:" + obj.toString());
        } else {
            Log.d(TAG, "getOperatorsTypeList then get null");
            return operatorsType;
        }
            operatorsType = obj.getJSONArray("data");
            for (int i = 0; i < operatorsType.length(); i++) {
                JSONObject netWork = operatorsType.getJSONObject(i);
                Log.i(TAG, "getOperatorsTypeList operatorsType = " + operatorsType.toString());
            }
        } catch (Exception e) {
            Log.e(TAG, "getOperatorsTypeList Exception = " + e.getMessage());
        }
            return operatorsType;
    }

    public int getOperatorTypeIndex(int tunerType) {
        int index = 0;
        try {
            JSONArray args = new JSONArray();
            args.put(tunerType);
            JSONObject obj = DtvkitGlueClient.getInstance().request("Dvb.GetOperatorsType", args);
            if (obj != null) {
                index = obj.getInt("data");
                Log.d(TAG, "get operator type index =" + index);
            } else {
                Log.d(TAG, "can't get operator Type");
                return index;
            }
        } catch (Exception e) {
            Log.e(TAG, "getOperatorTypeIndex Exception = " + e.getMessage());
        }
        return index;
    }

    public JSONObject setOperatorType(int tunerType, int operatorType) {
        JSONObject resultObj = null;
        try {
            JSONArray args = new JSONArray();
            args.put(tunerType);
            args.put(operatorType);
            resultObj = DtvkitGlueClient.getInstance().request("Dvb.SetOperatorsType", args);
            if (resultObj != null) {
                Log.d(TAG, "setOperatorType resultObj:" + resultObj.toString());
            } else {
                Log.d(TAG, "setOperatorType then get null");
            }
        } catch (Exception e) {
            Log.d(TAG, "setOperatorType Exception " + e.getMessage() + ", trace=" + e.getStackTrace());
            e.printStackTrace();
        }
        return resultObj;
    }

    public boolean checkIsItalyCountry() {
       final String ITALY_ISO3_NAME = "ita";
       String currentCountryName = getCurrentCountryIso3Name();
       if (ITALY_ISO3_NAME.equalsIgnoreCase(currentCountryName)) {
           return true;
       }
       return false;
    }

    //Norway, Finland, Sweden, Iceland, Denmark are Nordig;
    public boolean checkIsNordigCountry() {
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


    public boolean getAutomaticOrderingEnabled() {
        boolean result = false;
        try {
            JSONObject obj = DtvkitGlueClient.getInstance().request("Dvb.GetAutomaticOrderingEnabled", new JSONArray());
            if (obj != null) {
                result = obj.getBoolean("data");
            } else {
                Log.d(TAG, "getAutomaticOrderingEnabled obj is null");
            }
        } catch (Exception e) {
            Log.d(TAG, "getAutomaticOrderingEnabled Exception " + e.getMessage() + ", trace=" + e.getStackTrace());
        }
        return result;
    }

    public void setAutomaticOrderingEnabled(boolean bOrdering) {
        JSONObject resultObj = null;
        try {
            JSONArray args = new JSONArray();
            args.put(bOrdering);
            JSONObject obj = DtvkitGlueClient.getInstance().request("Dvb.SetAutomaticOrderingEnabled", args);
            if (resultObj != null) {
                Log.d(TAG, "setAutomaticOrderingEnabled resultObj:" + resultObj.toString());
            } else {
                Log.d(TAG, "setAutomaticOrderingEnabled then get null");
            }

        } catch (Exception e) {
            Log.d(TAG, "setAutomaticOrderingEnabled Exception " + e.getMessage() + ", trace=" + e.getStackTrace());
        }
    }
    /*
    * obj: {"accepted":true,"data":[{"blocked":false,"freq":706000000,"hidden":false,"is_data":false,"lcn":1,"name":"Supernova","network_id":12455,"radio":false,"sate_name":"","sig_name":"DVB-T","subtitles":false,"transponder":"","uri":"dvb:\/\/217c.6fd4.000e","video_codec":"MPEG","video_pid":0}]}
    */
    public JSONArray getConflictLcn() {
        JSONArray result = null;
        if (PropSettingManager.getBoolean("vendor.sys.tv.debug.conflictlcn", false)) {
            result = creatTestConflictLcn();
            return result;
        }
        try {
            JSONObject obj = DtvkitGlueClient.getInstance().request("Dvb.getLcnConflictServiceList", new JSONArray());
            if (obj != null) {
                Log.d(TAG, "getConflictLcn resultObj:" + obj.toString());
            } else {
                Log.d(TAG, "getConflictLcn then get null");
                return result;
            }
            JSONArray netWorks = obj.getJSONArray("data");
            result = netWorks;
            for (int i = 0; i < netWorks.length(); i++) {
                JSONObject netWork = netWorks.getJSONObject(i);
                Log.i(TAG, "getConflictLcn netWork = " + netWork.toString());
            }
        } catch (Exception e) {
            Log.e(TAG, "getConflictLcn Exception = " + e.getMessage());
        }
        return result;
    }

    private JSONArray creatTestConflictLcn() {
        JSONArray result = new JSONArray();
        int lcn = 0;
        for (int i = 0; i < 4; i++) {
            try {
                JSONObject obj = new JSONObject();
                obj.put("name", "name_" + i);
                if (i % 2 == 0) {
                    lcn++;
                }
                obj.put("lcn", lcn);
                obj.put("sig_name", "DVB-T");
                obj.put("uri", "dvb://0000.0000.000" + i);
                Log.i(TAG, "creatTestConflictLcn lcn = " + obj.toString());
                result.put(obj);
            } catch (Exception e) {
                Log.d(TAG, "creatTestConflictLcn Exception = " + e.getMessage());
            }
        }
        return result;
    }

    public boolean needConfirmLcnInfomation(JSONArray array) {
        boolean result = false;
        final String ITALY_ISO3_NAME = "ita";
        String currentCountryName = getCurrentCountryIso3Name();
        if (ITALY_ISO3_NAME.equalsIgnoreCase(currentCountryName)) {
            if (array != null && array.length() > 1) {
                result = true;
            }
        }
        return result;
    }

    public JSONObject selectServiceKeepConflictLcn(String serviceUri, String tunerType) {
        JSONObject resultObj = null;
        if (serviceUri == null || tunerType == null) {
            Log.d(TAG, "selectServiceKeepConflictLcn invalid serviceUri or tunerType");
            return resultObj;
        } else {
            Log.d(TAG, "selectServiceKeepConflictLcn serviceUri = " + serviceUri + ", tunerType = " + tunerType);
        }
        try {
            JSONArray args = new JSONArray();
            args.put(serviceUri);
            args.put(tunerType);
            resultObj = DtvkitGlueClient.getInstance().request("Dvb.SelectServiceKeepConflictLcn", args);
            if (resultObj != null) {
                Log.d(TAG, "selectServiceKeepConflictLcn resultObj:" + resultObj.toString());
            } else {
                Log.d(TAG, "selectServiceKeepConflictLcn then get null");
            }
        } catch (Exception e) {
            Log.d(TAG, "selectServiceKeepConflictLcn Exception " + e.getMessage() + ", trace=" + e.getStackTrace());
            e.printStackTrace();
        }
        return resultObj;
    }

    public JSONObject selectDefaultLcnConflictProcess(int lcn, String tunerType) {
        JSONObject resultObj = null;
        if (lcn == -1 || tunerType == null) {
            Log.d(TAG, "selectDefaultLcnConflictProcess invalid lcn or tunerType");
            return resultObj;
        } else {
            Log.d(TAG, "selectDefaultLcnConflictProcess lcn = " + lcn + ", tunerType = " + tunerType);
        }
        try {
            JSONArray args = new JSONArray();
            args.put(lcn);
            args.put(tunerType);
            resultObj = DtvkitGlueClient.getInstance().request("Dvb.SelectDefaultLcnConflictProcess", args);
            if (resultObj != null) {
                Log.d(TAG, "selectDefaultLcnConflictProcess resultObj:" + resultObj.toString());
            } else {
                Log.d(TAG, "selectDefaultLcnConflictProcess then get null");
            }
        } catch (Exception e) {
            Log.d(TAG, "selectDefaultLcnConflictProcess Exception " + e.getMessage() + ", trace=" + e.getStackTrace());
            e.printStackTrace();
        }
        return resultObj;
    }

    public JSONObject getLcnServiceByUri(JSONArray conflictLcnServices, String uri) {
        JSONObject result = null;
        if (conflictLcnServices != null && conflictLcnServices.length() > 0 && uri != null && uri.length() > 0) {
            try {
                JSONObject serviceObj = null;
                for (int i = 0; i < conflictLcnServices.length(); i++) {
                    serviceObj = conflictLcnServices.getJSONObject(i);
                    if (uri.equals(serviceObj.getString("uri"))) {
                        result = serviceObj;
                        break;
                    }
                }
            } catch (Exception e) {
                Log.d(TAG, "getLcnServiceByUri Exception = " + e.getMessage());
            }
        }
        return result;
    }

    public String getLcnServiceName(JSONObject obj) {
        String result = null;
        if (obj != null && obj.length() > 0) {
            try {
                result = obj.getString("name");
            } catch (Exception e) {
                Log.d(TAG, "getLcnServiceName Exception = " + e.getMessage());
            }
        }
        return result;
    }

    public int getLcnServiceLcnValue(JSONObject obj) {
        int result = -1;
        if (obj != null && obj.length() > 0) {
            try {
                result = obj.getInt("lcn");
            } catch (Exception e) {
                Log.d(TAG, "getLcnServiceLcnValue Exception = " + e.getMessage());
            }
        }
        return result;
    }

    public String getLcnServiceUri(JSONObject obj) {
        String result = null;
        if (obj != null && obj.length() > 0) {
            try {
                result = obj.getString("uri");
            } catch (Exception e) {
                Log.d(TAG, "getLcnServiceUri Exception = " + e.getMessage());
            }
        }
        return result;
    }

    public String getLcnServiceTunerType(JSONObject obj) {
        String result = null;
        if (obj != null && obj.length() > 0) {
            try {
                result = obj.getString("sig_name");
            } catch (Exception e) {
                Log.d(TAG, "getLcnServiceTunerType Exception = " + e.getMessage());
            }
        }
        return result;
    }

    public boolean hasSelectToEnd(JSONArray lcnConflictArray, int selectEndIndex) {
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
                lcnConflictObj = getJSONObjectFromJSONArray(lcnConflictArray, i);
                conflictLcn = getLcnServiceLcnValue(lcnConflictObj);
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

    public void dealRestLcnConflictAsDefault(JSONArray lcnConflictArray, int restStartIndex) {
        if (lcnConflictArray != null && lcnConflictArray.length() > 0) {
            JSONObject lcnConflictObj = null;
            int lcn = -1;
            String tunerType = null;
            int previousLcn = -1;
            boolean needUpdate = false;
            for (int i = restStartIndex; i < lcnConflictArray.length(); i++) {
                lcnConflictObj = getJSONObjectFromJSONArray(lcnConflictArray, i);
                lcn = getLcnServiceLcnValue(lcnConflictObj);
                tunerType = getLcnServiceTunerType(lcnConflictObj);
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
                    selectDefaultLcnConflictProcess(previousLcn, tunerType);
                }
            }
        }
    }

    public String getCustomParameter(String key, String defaultJsonValue) {
        String result = null;
        if (TextUtils.isEmpty(key)) {
            return result;
        }

        switch (key) {
            case KEY_LASTWAHTCHED_CHANNELID:
                result = "" + getChannelIdForSource();
                break;
            case KEY_ACTIVE_RECORD_COUNT:
                result = String.valueOf(recordingGetNumActiveRecordings());
                break;
            default:
                result = defaultJsonValue;
                break;
        }

        return result;
    }

    public void setCustomParameter(String key, String newJsonValues) {
        Log.d(TAG, "setCustomParameter need to add related code for extends key = " + key + ", newJsonValues = " + newJsonValues);
        if (TextUtils.isEmpty(key)) {
            return;
        }

        switch (key) {
            case KEY_RESET_DEFAULT_AUDIO_STREAM:
                resetToDefaultAudioStream();
                break;
            default:
                break;
        }
    }

    public boolean playerGetSubtitlesOn(int index) {
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

    public void playerSetSubtitlesOn(boolean on, int index) {
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

    public boolean needConfirmTargetRegion(JSONArray array) {
        boolean result = false;
        final String ENG_ISO3_NAME = "gbr";
        String currentCountryName = getCurrentCountryIso3Name();
        if (ENG_ISO3_NAME.equalsIgnoreCase(currentCountryName)) {
            if (array != null && array.length() > 1) {
                result = true;
            }
        }
        return result;
    }

    public JSONArray getTargetRegions(int target_id, int country, int primary, int secondary) {
        JSONArray result = null;
        String request = null;
        JSONArray args = new JSONArray();
        switch (target_id) {
            case TargetRegionManager.TARGET_REGION_COUNTRY:
                request = "Dvb.getGetNetworkTargetCountries";
                break;
            case TargetRegionManager.TARGET_REGION_PRIMARY:
                request = "Dvb.getNetworkPrimaryTargetRegions";
                if (country > 0) {
                    args.put(country);
                }
                break;
            case TargetRegionManager.TARGET_REGION_SECONDARY:
                request = "Dvb.getNetworkSecondaryTargetRegions";
                if (country > 0 && primary >= 0) {
                    args.put(country);
                    args.put(primary);
                }
                break;
            case TargetRegionManager.TARGET_REGION_TERTIARY:
                request = "Dvb.getNetworkTertiaryTargetRegions";
                if (country > 0 && primary >= 0 && secondary >= 0) {
                    args.put(country);
                    args.put(primary);
                    args.put(secondary);
                }

                break;
            default:
                request = "Dvb.getNetworkPrimaryTargetRegions";
                break;
        }
        try {
            JSONObject obj = DtvkitGlueClient.getInstance().request(request, args);
            if (obj != null) {
                Log.d(TAG, "getTargetRegions resultObj:" + obj.toString());
            } else {
                Log.d(TAG, "getTargetRegions then get null");
                return result;
            }
            result = obj.getJSONArray("data");
        } catch (Exception e) {
            Log.e(TAG, "getTargetRegions Exception = " + e.getMessage());
        }
        return result;
    }

    public JSONObject setTargetRegionSelection(int target_id, int selection) {
        JSONObject resultObj = null;
        String request = null;
        if (selection == -1) {
            Log.d(TAG, "setTargetRegionSelection invalid region");
            return resultObj;
        } else {
            Log.d(TAG, "setTargetRegionSelection target=" + target_id + ", region = " + selection);
        }
        switch (target_id) {
            case TargetRegionManager.TARGET_REGION_COUNTRY:
                request = "Dvb.SetNetworkTargetCountry";
                break;
            case TargetRegionManager.TARGET_REGION_PRIMARY:
                request = "Dvb.setNetworkPrimaryTargetRegion";
                break;
            case TargetRegionManager.TARGET_REGION_SECONDARY:
                request = "Dvb.setNetworkSecondaryTargetRegion";
                break;
            case TargetRegionManager.TARGET_REGION_TERTIARY:
                request = "Dvb.setNetworkTertiaryTargetRegion";
                break;
            default:
                break;
        }
        if (request == null) return resultObj;
        try {
            JSONArray args = new JSONArray();
            args.put(selection);
            resultObj = DtvkitGlueClient.getInstance().request(request, args);
            if (resultObj != null) {
                Log.d(TAG, "setTargetRegionSelection resultObj:" + resultObj.toString());
            } else {
                Log.d(TAG, "setTargetRegionSelection then get null");
            }
        } catch (Exception e) {
            Log.d(TAG, "setTargetRegionSelection Exception " + e.getMessage() + ", trace=" + e.getStackTrace());
            e.printStackTrace();
        }
        return resultObj;
    }

    public String getTargetRegionName(int region_id, JSONObject obj) {
        String result = null;
        int country_code = -1;
        if (obj != null && obj.length() > 0) {
            try {
                if (region_id == TargetRegionManager.TARGET_REGION_COUNTRY) {
                    country_code = obj.getInt("country_code");
                    if (country_code > 0) {
                        byte[] tmp = {(byte)((country_code & 0xff0000) >> 16),
                                      (byte)((country_code & 0x00ff00) >> 8),
                                      (byte)((country_code & 0x0000ff))};
                        String lan_iso3166_a3 = new String(tmp);
                        result = ISO639Data.getCountryNameFromCode(lan_iso3166_a3);
                    }
                } else {
                    result = obj.getString("region_name");
                }
            } catch (Exception e) {
                Log.d(TAG, "getTargetRegionName Exception = " + e.getMessage());
            }
        }
        return result;
    }

    public int getTargetRegionCode(int region_id, JSONObject obj) {
        int result = -1;//invalid
        if (obj != null && obj.length() > 0) {
            try {
                if (region_id == TargetRegionManager.TARGET_REGION_COUNTRY) {
                    result = obj.getInt("country_code");
                } else {
                    result = obj.getInt("region_code");
                }
            } catch (Exception e) {
                Log.d(TAG, "getTargetRegionCode Exception = " + e.getMessage());
            }
        }
        return result;
    }

    public boolean restSatellites() {
        boolean result = false;
        try {
            JSONArray args = new JSONArray();
            args.put(ConstantManager.DTVKIT_SATELLITE_DATA);
            args.put(ConstantManager.DTVKIT_LNBS_DATA);
            args.put(ConstantManager.DTVKIT_LOCATION_DATA);
            result = DtvkitGlueClient.getInstance().request("Dvbs.resetSatellites", args).getBoolean("data");
        } catch (Exception e) {
            Log.e(TAG, "restSatellites = " + e.getMessage());
        }
        mDataMananer.saveIntParameters(DataMananer.KEY_SEARCH_MODE, 0);

        return result;
    }

    public boolean restoreToDefault() {
        boolean result = false;
        String defaultCountry = "deu"; //todo: not defined in trunk, so use german here
        try {
            result = DtvkitGlueClient.getInstance().request("Dvb.resetoreDefault", new JSONArray()).getBoolean("data");
            if (result) {
                JSONArray args = new JSONArray();
                args.put(ConstantManager.DTVKIT_SATELLITE_DATA);
                args.put(ConstantManager.DTVKIT_LNBS_DATA);
                args.put(ConstantManager.DTVKIT_LOCATION_DATA);
                result = DtvkitGlueClient.getInstance().request("Dvbs.resetSatellites", args).getBoolean("data");
                mDataMananer.saveIntParameters(DataMananer.DTVKIT_IMPORT_SATELLITE_FLAG, 0);
            }
            if (result) {
                JSONArray args = new JSONArray();
                int countryCode = ((int)(defaultCountry.charAt(0))<<16)
                                + ((int)(defaultCountry.charAt(1))<<8)
                                + (int)(defaultCountry.charAt(2));
                args.put(countryCode);
                result = DtvkitGlueClient.getInstance().request("Dvb.setCountry", args).getBoolean("data");
            }
        } catch (Exception e) {
            Log.e(TAG, "restoreToDefault = " + e.getMessage());
        }
        if (result) {
            if (!DataMananer.PVR_DEFAULT_PATH.equals(getStringParameters(KEY_PVR_RECORD_PATH)))
                saveStringParameters(KEY_PVR_RECORD_PATH, DataMananer.PVR_DEFAULT_PATH);
            if (getIntParameters(AUTO_SEARCHING_MODE) != 0)
                saveIntParameters(AUTO_SEARCHING_MODE, 0);
            saveStringParameters(DataMananer.KEY_SATALLITE, DataMananer.KEY_SATALLITE_DEFAULT_VALUE);
        }

        return result;
    }

    public boolean exportChannels(String path) {
        boolean result = false;
        try {
            JSONArray args = new JSONArray();
            args.put(path);
            result = DtvkitGlueClient.getInstance().request("Dvb.exportChannels", args).getBoolean("data");
        } catch (Exception e) {
            Log.e(TAG, "exportChannels = " + e.getMessage());
        }
        return result;
    }

    public boolean importChannels(String path) {
        boolean result = false;
        try {
            JSONArray args = new JSONArray();
            args.put(path);
            result = DtvkitGlueClient.getInstance().request("Dvb.importChannels", args).getBoolean("data");
        } catch (Exception e) {
            Log.e(TAG, "importChannels = " + e.getMessage());
        }
        return result;
    }

    public boolean exportSatellites(String path) {
        boolean result = false;
        try {
            JSONArray args = new JSONArray();
            args.put(path);
            result = DtvkitGlueClient.getInstance().request("Dvbs.exportSatellites", args).getBoolean("data");
        } catch (Exception e) {
            Log.e(TAG, "exportSatellites = " + e.getMessage());
        }
        return result;
    }

    public boolean importSatellites(String path) {
        boolean result = false;
        try {
            JSONArray args = new JSONArray();
            args.put(path);
            result = DtvkitGlueClient.getInstance().request("Dvbs.reImportFromPath", args).getBoolean("data");
        } catch (Exception e) {
            Log.e(TAG, "importSatellites = " + e.getMessage());
        }
        return result;
    }

    //currently dtvkit can't with atv and av as they need to use same adc module in android r
    public boolean isTunerInputConflictwithDtvKit() {
        String currentPlayingInput = getStringParameters(TV_CURRENT_INPUTID);
        if (currentPlayingInput != null && VERSION.SDK_INT == 30 &&
                (currentPlayingInput.contains(TV_ADTV_KEY) ||
                currentPlayingInput.contains(TV_AV1_KEY) ||
                currentPlayingInput.contains(TV_AV2_KEY))) {
            return true;
        }
        return false;
    }

    public void getUbFreqs() {
        for (int channel = 0; channel < 8; channel ++) {
            //only support 8 brands for unicable
            int freq = getIntParameters(KEY_UB_FREQUENCY + channel);
            if (freq >= 950 && freq <= 2150) {
                mDvbsParaManager.setUbFrequency(channel, freq);
            }
        }
    }

    public void saveUbFreqs() {
        for (int channel = 0; channel < 8; channel ++) {
            //only support 8 brands for unicable
            saveIntParameters(KEY_UB_FREQUENCY + channel, mDvbsParaManager.getUbFrequency(channel));
        }
    }

    public int getCurrentDvbSource() {
        int source = ParameterMananer.SIGNAL_COFDM;
        try {
            JSONObject sourceReq = DtvkitGlueClient.getInstance().request("Dvb.GetDvbSource", new JSONArray());
            if (sourceReq != null) {
                source = sourceReq.optInt("data");
            }
        } catch (Exception e) {
        }
        return source;
    }

    public void saveChannelIdForSource(long channelId) {
        try {
            JSONArray args = new JSONArray();
            args.put(channelId);
            DtvkitGlueClient.getInstance().request("Dvb.saveDefaultChannelId", args);
        } catch (Exception e) {
        }
    }

    public long getChannelIdForSource() {
        long channelId = -1;

        try {
            channelId = DtvkitGlueClient.getInstance()
                .request("Dvb.getDefaultChannelId", new JSONArray())
                .getLong("data");
        } catch (Exception e) {
        }

        return channelId;
    }

    private int recordingGetNumActiveRecordings() {
        int numRecordings = 0;
        JSONArray activeRecordings = recordingGetActiveRecordings();
        if (activeRecordings != null) {
            numRecordings = activeRecordings.length();
        }
        return numRecordings;
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

    public String casSessionRequest(String request) {
        String result = null;

        if (TextUtils.isEmpty(request)) {
            return result;
        }
        JSONArray args = new JSONArray();
        String cmd = "Player.setCADescramblerIoctl";
        try
        {
            args.put(request);
            JSONObject obj =  DtvkitGlueClient.getInstance().request(cmd, args);
            result = obj.optString("data", "");
        } catch (Exception e) {
        }
        return result;
    }

    public void noticeStandby() {
        try {
            DtvkitGlueClient.getInstance().request("Player.enterStandby", new JSONArray());
        } catch (Exception e) {
        }
    }

    public void noticeResume() {
        try {
            DtvkitGlueClient.getInstance().request("Player.leaveStandby", new JSONArray());
        } catch (Exception e) {
        }
    }

    private void resetToDefaultAudioStream () {
        try {
            JSONArray args = new JSONArray();
            DtvkitGlueClient.getInstance().request("Player.resetToDefaultAudioStream", args);
            Log.d(TAG, "resetToDefaultAudioStream");
        } catch (Exception e) {
            Log.i(TAG, "resetToDefaultAudioStream Exception = " + e.getMessage());
        }
    }
}
