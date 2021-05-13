package com.droidlogic.fragment;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.droidlogic.fragment.ItemAdapter.ItemDetail;
import com.droidlogic.fragment.dialog.CustomDialog;
import org.droidlogic.dtvkit.DtvkitGlueClient;

public class LnbWrap {

    private static final String TAG = "LnbWrap";
    private Context mContext;
    private DtvkitGlueClient mDtvkitGlueClient;

    public LnbWrap(Context context, DtvkitGlueClient glueClient) {
        this.mContext = context;
        this.mDtvkitGlueClient = glueClient;
    }

    public List<String> getLnbIdList() {
        JSONArray array = null;
        List<String> list = new ArrayList<String>();
        try {
            JSONObject jobj = mDtvkitGlueClient.request("Dvbs.getLnbs", new JSONArray());
            if (jobj != null) {
                array = (JSONArray)(jobj.get("data"));
            }
            if (array != null) {
                for (int i = 0; i < array.length(); i ++) {
                    String lnb_id = (String)(((JSONObject)(array.get(i))).get("lnb"));
                    list.add(lnb_id);
                }
            }
        } catch (Exception e) {
        }
        return list;
    }

    public Lnb getLnbById(String id) {
        Lnb lnb = new Lnb();
        JSONArray array = null;
        try {
            JSONObject jobj = mDtvkitGlueClient.request("Dvbs.getLnbs", new JSONArray());
            if (jobj != null) {
                array = (JSONArray)(jobj.get("data"));
            }
            if (array != null) {
                for (int i = 0; i < array.length(); i ++) {
                    JSONObject lnbObj = (JSONObject)(array.get(i));
                    String lnb_id = (String)(lnbObj.get("lnb"));
                    if (lnb_id.equals(id)) {
                        lnb.parseFromJson(lnbObj);
                        break;
                    }
                }
            }
        } catch (Exception e) {
        }
        return lnb;
    }

    public void addEmptyLnb() {
        try {
            JSONArray array = new Lnb().toJsonArray();
            mDtvkitGlueClient.request("Dvbs.addLnb", array);
        } catch (Exception e) {
        }
    }

    public void removeLnb(String id) {
        try {
            JSONArray array = new JSONArray();
            array.put(id);
            mDtvkitGlueClient.request("Dvbs.deleteLnb", array);
        } catch (Exception e) {
        }
    }

    public List<String> getDiseqcLocationNames() {
        List<String> locationList = new ArrayList<String>();
        try {
            JSONObject obj = mDtvkitGlueClient.request("Dvbs.getLocations", new JSONArray());
            if (obj != null) {
                JSONArray array = (JSONArray)(obj.get("data"));
                if (array != null) {
                    for (int i = 0; i < array.length(); i ++) {
                        JSONObject jLocation = (JSONObject)(array.get(i));
                        DiseqcLocation location = new DiseqcLocation();
                        location.parseFromJson(jLocation);
                        locationList.add(location.getName());
                    }
                }
            }
        } catch (Exception e) {
        }
        return locationList;
    }

    public DiseqcLocation getLocationInfoByName(String name) {
        DiseqcLocation ret = new DiseqcLocation();
        try {
            JSONObject obj = mDtvkitGlueClient.request("Dvbs.getLocations", new JSONArray());
            if (obj != null) {
                JSONArray array = (JSONArray)(obj.get("data"));
                if (array != null) {
                    for (int i = 0; i < array.length(); i ++) {
                        JSONObject jLocation = (JSONObject)(array.get(i));
                        DiseqcLocation location = new DiseqcLocation();
                        location.parseFromJson(jLocation);
                        if (location.getName().equals(name)) {
                            ret = location;
                            break;
                        }
                    }
                }
            }
        } catch (Exception e) {
        }
        return ret;
    }

    public DiseqcLocation getLocationInfoByIndex(int index) {
        DiseqcLocation ret = new DiseqcLocation();
        try {
            JSONObject obj = mDtvkitGlueClient.request("Dvbs.getLocations", new JSONArray());
            if (obj != null) {
                JSONArray array = (JSONArray)(obj.get("data"));
                if (array != null) {
                    if (index < (array.length() - 1)) {
                        JSONObject jLocation = (JSONObject)(array.get(index));
                        ret.parseFromJson(jLocation);
                    }
                }
            }
        } catch (Exception e) {
        }
        return ret;
    }

    public void actionToLocation(String sateName, boolean isEast, int longitude,
                                 boolean isNorth, int latitude) {
        try {
            JSONArray array = new JSONArray();
            array.put(sateName);
            array.put(isEast);
            array.put(longitude);
            array.put(isNorth);
            array.put(latitude);
            mDtvkitGlueClient.request("Dvbs.goToXX", array);
        } catch (Exception e) {
        }
    }

    public void editManualLocation(boolean isEast, int longitude,
                                 boolean isNorth, int latitude) {
        try {
            JSONArray array = new JSONArray();
            array.put("manual");
            array.put(isEast);
            array.put(longitude);
            array.put(isNorth);
            array.put(latitude);
            mDtvkitGlueClient.request("Dvbs.editLocation", array);
        } catch (Exception e) {
        }
    }

    public class Unicable {
        private boolean onOff = false;
        private int channel = 0;
        private int band_freq = 1284;
        private boolean isPostionB = false;
        private Lnb mLnb = null;

        public Unicable(Lnb lnb) {
            mLnb = lnb;
        }

        public void parseFromJson(JSONObject json) {
            try {
                if (json != null) {
                    onOff = (boolean)(json.get("unicable"));
                    channel = (int)(json.get("unicable_chan"));
                    band_freq = (int)(json.get("unicable_if"));
                    isPostionB = (boolean)(json.get("unicable_position_b"));
                }
            } catch (Exception e) {
            }
            if (channel == 65535 || band_freq == 65535) {
                channel = 0;
                band_freq = 1284;
            }
        }

        public JSONArray toJsonArray() {
            JSONArray array = new JSONArray();
            try {
                array.put(onOff);
                array.put(channel);
                array.put(band_freq);
                array.put(isPostionB);
            } catch (Exception e) {
            }
            return array;
        }

        public boolean getOnoff() {
            return onOff;
        }

        public int getChannel() {
            return channel;
        }

        public int getBandFreq() {
            return band_freq;
        }

        public boolean isPostionB() {
            return isPostionB;
        }

        public boolean switchUnicable() {
            onOff = !onOff;
            mLnb.updateToDtvkit();
            return true;
        }

        public boolean switchUnicablePosition() {
            isPostionB = !isPostionB;
            mLnb.updateToDtvkit();
            return true;
        }

        public boolean editUnicableChannel(int channel, int freq) {
            if (this.channel != channel) {
                this.channel = channel;
                this.band_freq = freq;
                mLnb.updateToDtvkit();
                return true;
            }
            return false;
        }
    }

    public class LnbInfo {
        private int lnb_type = 0;
        private int low_freq_min = 0;
        private int low_freq_max = 11750;
        private int low_freq_local = 5150;
        private String lnb_power = "on";
        private boolean tone_22k = false;
        private int high_freq_min = 0;
        private int high_freq_max = 11750;
        private int high_freq_local = 0;
        private Lnb mLnb = null;

        public LnbInfo(Lnb lnb) {
            mLnb = lnb;
        }

        public void parseFromJson(JSONObject json) {
            try {
                if (json != null) {
                    lnb_type = (int)(json.get("lnb_type"));
                    low_freq_min = (int)(json.get("low_min_freq"));
                    low_freq_max = (int)(json.get("low_max_freq"));
                    low_freq_local = (int)(json.get("low_local_oscillator_frequency"));
                    lnb_power = (String)(json.get("low_lnb_voltage"));
                    tone_22k = (boolean)(json.get("low_tone_22k"));
                    high_freq_min = (int)(json.get("high_min_freq"));
                    high_freq_max = (int)(json.get("high_max_freq"));
                    high_freq_local = (int)(json.get("high_local_oscillator_frequency"));
                    if (low_freq_max == 0 || low_freq_max == 65535) {
                        low_freq_max = 11750;
                    }
                    if (high_freq_max == 0 || high_freq_max == 65535) {
                        high_freq_max = 11750;
                    }
                }
            } catch (Exception e) {
            }
        }

        public JSONArray toJsonArray() {
            JSONArray array = new JSONArray();
            try {
                array.put(lnb_type);
                array.put(low_freq_min);
                array.put(low_freq_max);
                array.put(low_freq_local);
                array.put(lnb_power);
                array.put(tone_22k);
                array.put(high_freq_min);
                array.put(high_freq_max);
                array.put(high_freq_local);
                array.put(lnb_power);
                array.put(tone_22k);
            } catch (Exception e) {
            }
            return array;
        }

        public int getType() {
            return lnb_type;
        }

        public String getLnbPower() {
            return lnb_power;
        }

        public boolean get22Khz() {
            return tone_22k;
        }

        public boolean isSingle() {
            return high_freq_local == 0;
        }

        public int lowMin() {
            return low_freq_min;
        }

        public int lowMax() {
            return low_freq_max;
        }

        public int highMin() {
            return high_freq_min;
        }

        public int highMax() {
            return high_freq_max;
        }

        public int highLocalFreq() {
            return high_freq_local;
        }

        public int lowLocalFreq() {
            return low_freq_local;
        }

        public boolean editLnbType(int type) {
            if (lnb_type != type) {
                lnb_type = type;
                if (lnb_type == 0) {
                    low_freq_local = 5150;
                    high_freq_local = 0;
                } else if (lnb_type == 1) {
                    low_freq_local = 9750;
                    high_freq_local = 10600;
                }
                mLnb.updateToDtvkit();
                return true;
            }
            return false;
        }

        public boolean editLnb22Khz(boolean onoff) {
            if (tone_22k != onoff) {
                tone_22k = onoff;
                mLnb.updateToDtvkit();
                return true;
            }
            return false;
        }

        public boolean editLnbPower(boolean onoff) {
            boolean current_lnbPower = (lnb_power.equals("off")) ? false : true;
            if (current_lnbPower != onoff) {
                lnb_power = onoff ? "on" : "off";
                mLnb.updateToDtvkit();
                return true;
            }
            return false;
        }

        public boolean updateLnbTypeFreq(int lowMin, int lowMax, int lowLocal,
                                      int highMin, int highMax, int highLocal) {
            boolean result = false;
            if (low_freq_min != lowMin) {
                low_freq_min = lowMin;
                result = true;
            }
            if (low_freq_max != lowMax) {
                low_freq_max = lowMax;
                result = true;
            }
            if (low_freq_local != lowLocal) {
                low_freq_local = lowLocal;
                result = true;
            }
            if (high_freq_min != highMin) {
                high_freq_min = highMin;
                result = true;
            }
            if (high_freq_max != highMax) {
                high_freq_max = highMax;
                result = true;
            }
            if (high_freq_local != highLocal) {
                high_freq_local = highLocal;
                result = true;
            }
            if (result) {
                mLnb.updateToDtvkit();
            }
            return result;
        }
    }

    public class DiseqcLocation {
        private String name = "manual";
        private boolean isLongitudeEast = true;
        private int longitude = 0;
        private boolean isLatitudeNorth = true;
        private int latitude = 0;

        public DiseqcLocation() {
        }

        public void parseFromJson(JSONObject json) {
            try {
                if (json != null) {
                    name = (String)(json.get("name"));
                    isLongitudeEast = (boolean) (json.get("east"));
                    longitude = (int)(json.get("longitude"));
                    isLatitudeNorth = (boolean)(json.get("north"));
                    latitude = (int)(json.get("latitude"));
                }
            } catch (Exception e) {
            }
        }

        public String getName() {
            return name;
        }

        public boolean isLongitudeEast() {
            return isLongitudeEast;
        }

        public int getLongitude() {
            return longitude;
        }

        public boolean isLatitudeNorth() {
            return isLatitudeNorth;
        }

        public int getLatitude() {
            return latitude;
        }
    }

    public class Lnb {
        private String lnbId = "1";
        private Unicable unicable = new Unicable(this);
        private String tone_burst = "none";
        private int c_switch = 0;
        private int u_switch = 0;
        private int motor    = 0;
        private LnbInfo lnbInfo = new LnbInfo(this);

        public Lnb(){
        }

        public void parseFromJson(JSONObject json) {
            try {
                if (json != null) {
                    lnbId = (String)(json.get("lnb"));
                    unicable.parseFromJson(json);
                    tone_burst = (String)(json.get("tone_burst"));
                    c_switch = (int)(json.get("c_switch"));
                    u_switch = (int)(json.get("u_switch"));
                    motor = (int)(json.get("motor_switch"));
                    lnbInfo.parseFromJson(json);
                }
            } catch (Exception e) {
            }
        }

        public JSONArray toJsonArray() {
            JSONArray array = new JSONArray();
            try {
                //not pass lnbid
                JSONArray tempArray = unicable.toJsonArray();
                for (int i = 0; i < tempArray.length(); i ++) {
                    array.put(tempArray.get(i));
                }
                array.put(tone_burst);
                array.put(c_switch);
                array.put(u_switch);
                array.put(motor);
                tempArray = lnbInfo.toJsonArray();
                for (int i = 0; i < tempArray.length(); i ++) {
                    array.put(tempArray.get(i));
                }
            } catch (Exception e) {
            }
            return array;
        }

        public String getId() {
            return lnbId;
        }

        public LnbInfo getLnbInfo() {
            return lnbInfo;
        }

        public Unicable getUnicable() {
            return unicable;
        }

        public String getToneBurst() {
            return tone_burst;
        }

        public int getCswitch() {
            return c_switch;
        }

        public int getUswitch() {
            return u_switch;
        }

        public int getMotor() {
            return motor;
        }

        public boolean editToneBurst(String val) {
            if (val == null || val.equals(tone_burst)) {
                return false;
            }
            if ("a".equals(val) || "b".equals(val)) {
                tone_burst = val;
            } else {
                tone_burst = "none";
            }
            updateToDtvkit();
            return true;
        }

        public boolean editCswitch(int val) {
            if (val > 4 || val < 0) {
                val = 0;
            }
            if (c_switch == val) {
                return false;
            }
            c_switch = val;
            updateToDtvkit();
            return true;
        }

        public boolean editUswitch(int val) {
            if (val > 16 || val < 0) {
                val = 0;
            }
            if (u_switch == val) {
                return false;
            }
            u_switch = val;
            updateToDtvkit();
            return true;
        }

        public boolean editMotor(int val) {
            if (val > 2 || val < 0) {
                val = 0;
            }
            if (motor == val) {
                return false;
            }
            motor = val;
            updateToDtvkit();
            return true;
        }

        public boolean updateToDtvkit() {
            try {
                JSONArray array = new JSONArray();
                array.put(lnbId);
                JSONArray lnbArray = toJsonArray();
                for (int i = 0; i < lnbArray.length(); i ++) {
                    array.put(lnbArray.get(i));
                }
                mDtvkitGlueClient.request("Dvbs.editLnb", array);
            } catch (Exception e) {
            }
            return true;
        }
    }
}
