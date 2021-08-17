package com.droidlogic.settings;

import android.util.Log;
import android.media.tv.TvTrackInfo;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;


import com.droidlogic.app.SystemControlManager;

public class ConstantManager {

    private static final String TAG = "ConstantManager";
    private static final boolean DEBUG = true;

    public static final String PI_FORMAT_KEY = "pi_format";
    public static final String KEY_AUDIO_CODES_DES = "audio_codes";
    public static final String KEY_TRACK_PID = "pid";
    public static final String KEY_INFO = "info";
    public static final String KEY_SIGNAL_STRENGTH = "signal_strength";
    public static final String KEY_SIGNAL_QUALITY = "signal_quality";

    public static final String EVENT_STREAM_PI_FORMAT = "event_pi_format";
    public static final String EVENT_RESOURCE_BUSY = "event_resource_busy";
    public static final String EVENT_SIGNAL_INFO = "event_signal_info";
    public static final String EVENT_RECORD_PROGRAM_URI = "event_record_program_uri";
    public static final String EVENT_RECORD_DATA_URI = "event_record_data_uri";
    public static final String EVENT_CA_MESSAGE = "event_ca_message";
    public static final String EVENT_PIP_INFO = "event_pip_info";
    public static final String KEY_PIP_ACTION = "event_pip_action";
    public static final String VALUE_PIP_ACTION_TUNE_NEXT = "tune_next";

    //show or hide overlay
    public static final String ACTION_TIF_CONTROL_OVERLAY = "tif_control_overlay";
    public static final String KEY_TIF_OVERLAY_SHOW_STATUS = "tif_overlay_show_status";
    public static final String ACTION_TIF_BEFORE_TUNE = "tif_before_tune";
    //surface related
    public static final String KEY_SURFACE = "surface";
    public static final String KEY_TV_STREAM_CONFIG = "config";

    //add for pvr delete control
    public static final String ACTION_REMOVE_ALL_DVR_RECORDS = "droidlogic.intent.action.remove_all_dvr_records";
    public static final String ACTION_DVR_RESPONSE = "droidlogic.intent.action.dvr_response";
    public static final String KEY_DVR_DELETE_RESULT_LIST = "dvr_delete_result_list";
    public static final String KEY_DTVKIT_SEARCH_TYPE = "dtvkit_search_type";//auto or manual
    public static final String KEY_DTVKIT_SEARCH_TYPE_AUTO = "dtvkit_auto_search";//auto
    public static final String KEY_DTVKIT_SEARCH_TYPE_MANUAL = "dtvkit_manual_search_type";//manual
    public static final String KEY_LIVETV_PVR_STATUS = "livetv_pvr_status";//manual
    public static final String VALUE_LIVETV_PVR_SCHEDULE_AVAILABLE = "livetv_pvr_schedule";
    public static final String VALUE_LIVETV_PVR_RECORD_PROGRAM_AVAILABLE = "livetv_pvr_program";

    public static final String CONSTANT_QAA = "qaa";//Original Audio flag
    public static final String CONSTANT_ORIGINAL_AUDIO = "Original Audio";
    public static final String CONSTANT_UND_FLAG = "und";//undefined flag
    public static final String CONSTANT_UND_VALUE = "Undefined";
    public static final String CONSTANT_QAD = "qad";
    public static final String CONSTANT_NAR = "nar";
    public static final String CONSTANT_FRENCH = "french";
    public static final String CONSTANT_NAR_VALUE = "Narrative";

    //add for trackinfo
    public static final String SYS_VIDEO_DECODE_PATH = "/sys/class/vdec/vdec_status";
    public static final String SYS_VIDEO_DECODE_VIDEO_WIDTH_PREFIX = "frame width :";
    public static final String SYS_VIDEO_DECODE_VIDEO_WIDTH_SUFFIX = "frame height";
    public static final String SYS_VIDEO_DECODE_VIDEO_HEIGHT_PREFIX = "frame height :";
    public static final String SYS_VIDEO_DECODE_VIDEO_HEIGHT_SUFFIX = "frame rate";
    public static final String SYS_VIDEO_DECODE_VIDEO_FRAME_RATE_PREFIX = "frame rate :";
    public static final String SYS_VIDEO_DECODE_VIDEO_FRAME_RATE_SUFFIX = "fps";
    //video
    public static final String KEY_TVINPUTINFO_VIDEO_WIDTH = "video_width";
    public static final String KEY_TVINPUTINFO_VIDEO_HEIGHT = "video_height";
    public static final String KEY_TVINPUTINFO_VIDEO_FRAME_RATE = "video_frame_rate";
    public static final String KEY_TVINPUTINFO_VIDEO_FORMAT = "video_format";
    public static final String KEY_TVINPUTINFO_VIDEO_FRAME_FORMAT = "video_frame_format";
    public static final String KEY_TVINPUTINFO_VIDEO_CODEC = "video_codec";
    public static final String VALUE_TVINPUTINFO_VIDEO_INTERLACE = "I";
    public static final String VALUE_TVINPUTINFO_VIDEO_PROGRESSIVE = "P";
    //audio
    public static final String KEY_TVINPUTINFO_AUDIO_INDEX = "audio_index";
    public static final String KEY_TVINPUTINFO_AUDIO_AD = "audio_ad";
    public static final String KEY_TVINPUTINFO_AUDIO_CODEC = "audio_codec";
    public static final String KEY_TVINPUTINFO_AUDIO_SAMPLING_RATE = "audio_sampling_rate";
    public static final String KEY_TVINPUTINFO_AUDIO_CHANNEL = "audio_channel";
    public static final String AUDIO_PATCH_COMMAND_GET_SAMPLING_RATE = "sample_rate";
    public static final String AUDIO_PATCH_COMMAND_GET_AUDIO_CHANNEL = "channel_nums";
    public static final String AUDIO_PATCH_COMMAND_GET_AUDIO_CHANNEL_CONFIGURE = "channel_configuration";
    //subtitle
    public static final String KEY_TVINPUTINFO_SUBTITLE_INDEX = "subtitle_index";
    public static final String KEY_TVINPUTINFO_SUBTITLE_IS_TELETEXT = "is_teletext";
    public static final String KEY_TVINPUTINFO_SUBTITLE_IS_HARD_HEARING = "is_hard_hearing";
    //ci plus
    public static final String ACTION_CI_PLUS_INFO = "ci_plus_info";
    public static final String CI_PLUS_COMMAND = "command";
    public static final String VALUE_CI_PLUS_COMMAND_SEARCH_REQUEST = "search_request";
    public static final String VALUE_CI_PLUS_COMMAND_SEARCH_FINISHED = "search_finished";
    public static final String VALUE_CI_PLUS_COMMAND_CHANNEL_UPDATED = "channel_updated";
    public static final String VALUE_CI_PLUS_COMMAND_HOST_CONTROL = "host_control";
    public static final String VALUE_CI_PLUS_COMMAND_IGNORE_INPUT = "ignore_input";
    public static final String VALUE_CI_PLUS_COMMAND_RECEIVE_INPUT = "receive_input";
    public static final String VALUE_CI_PLUS_SEARCH_MODULE = "search_module";
    public static final String VALUE_CI_PLUS_CHANNEL = "host_control_channel";
    public static final String VALUE_CI_PLUS_TUNE_TYPE = "tune_type";
    public static final String VALUE_CI_PLUS_TUNE_TYPE_SERVICE = "service_tune";
    public static final String VALUE_CI_PLUS_TUNE_TYPE_TRANSPORT= "transport_tune";
    public static final String VALUE_CI_PLUS_TUNE_QUIETLY = "tune_quietly";
    public static final String VALUE_CI_PLUS_EVENT_DETAIL = "event_detail";
    public static final String VALUE_CI_CARD_SLOT = "ci_card_slot";
    public static final String VALUE_CI_CAM_CARD_ID = "ci_cam_card_id";
    public static final String VALUE_CI_CAM_CARD_STATUS = "ci_cam_card_status";
    public static final String ACTION_CI_MENU_INFO = "ci_menu_info";
    public static final String COMMAND_CI_MENU = "command";
    public static final String VALUE_CI_MENU_INSERT_MODULE = "ci_menu_insert_module";
    public static final String VALUE_CI_MENU_OPEN_MODULE = "ci_menu_open_module";
    public static final String VALUE_CI_MENU_CLOSE_MODULE = "ci_menu_close_module";
    public static final String VALUE_CI_MENU_REMOVE_MODULE = "ci_menu_remove_module";

    public static final Map<String, String> PI_TO_VIDEO_FORMAT_MAP = new HashMap<>();
    static {
        PI_TO_VIDEO_FORMAT_MAP.put("interlace", "I");
        PI_TO_VIDEO_FORMAT_MAP.put("progressive", "P");
        PI_TO_VIDEO_FORMAT_MAP.put("interlace-top", "I");
        PI_TO_VIDEO_FORMAT_MAP.put("interlace-bottom", "I");
        PI_TO_VIDEO_FORMAT_MAP.put("Compressed", "P");
    }

    public static final String CONSTANT_FORMAT_PROGRESSIVE = "progressive";
    public static final String CONSTANT_FORMAT_INTERLACE = "interlace";
    public static final String CONSTANT_FORMAT_COMRPESSED = "Compressed";

    public static final String SYS_WIDTH_PATH = "/sys/class/video/frame_width";
    public static final String SYS_HEIGHT_PATH = "/sys/class/video/frame_height";
    public static final String SYS_PI_PATH = "/sys/class/deinterlace/di0/frame_format";//"/sys/class/video/frame_format";

    //add define for subtitle type
    public static final int ADB_SUBTITLE_TYPE_DVB  = 0x10;
    public static final int ADB_SUBTITLE_TYPE_DVB_4_3 = 0x11;
    public static final int ADB_SUBTITLE_TYPE_DVB_16_9 = 0x12;
    public static final int ADB_SUBTITLE_TYPE_DVB_221_1 = 0x13;
    public static final int ADB_SUBTITLE_TYPE_DVB_HD = 0x14;
    public static final int ADB_SUBTITLE_TYPE_DVB_HARD_HEARING = 0x20;
    public static final int ADB_SUBTITLE_TYPE_DVB_HARD_HEARING_4_3 = 0x21;
    public static final int ADB_SUBTITLE_TYPE_DVB_HARD_HEARING_16_9 = 0x22;
    public static final int ADB_SUBTITLE_TYPE_DVB_HARD_HEARING_221_1 = 0x23;
    public static final int ADB_SUBTITLE_TYPE_DVB_HARD_HEARING_HD = 0x24;

    //add define for telextet type
    public static final int ADB_TELETEXT_TYPE_INITIAL = 0x01;
    public static final int ADB_TELETEXT_TYPE_SUBTITLE = 0x02;
    public static final int ADB_TELETEXT_TYPE_ADDITIONAL_INFO = 0x03;
    public static final int ADB_TELETEXT_TYPE_SCHEDULE = 0x04;
    public static final int ADB_TELETEXT_TYPE_SUBTITLE_HARD_HEARING = 0x05;

    //add dtvkit satellite
    public static final String DTVKIT_SATELLITE_DATA = "/mnt/vendor/odm_ext/etc/tvconfig/dtvkit/satellite.json";
    public static final String DTVKIT_LNBS_DATA = "/mnt/vendor/odm_ext/etc/tvconfig/dtvkit/lnb.json";
    public static final String DTVKIT_LOCATION_DATA = "/mnt/vendor/odm_ext/etc/tvconfig/dtvkit/location.json";

    public static void ascendTrackInfoOderByPid(List<TvTrackInfo> list) {
        if (list != null) {
            Collections.sort(list, new PidAscendComparator());
        }
    }

    public static class PidAscendComparator implements Comparator<TvTrackInfo> {
        public int compare(TvTrackInfo o1, TvTrackInfo o2) {
            Integer pid1 = new Integer(o1.getExtra().getInt("pid", 0));
            Integer pid2 = new Integer(o2.getExtra().getInt("pid", 0));
            return pid1.compareTo(pid2);
        }
    }

    public static class PidDscendComparator implements Comparator<TvTrackInfo> {
        public int compare(TvTrackInfo o1, TvTrackInfo o2) {
            Integer pid1 = new Integer(o1.getExtra().getInt("pid", 0));
            Integer pid2 = new Integer(o2.getExtra().getInt("pid", 0));
            return pid2.compareTo(pid1);
        }
    }
}
