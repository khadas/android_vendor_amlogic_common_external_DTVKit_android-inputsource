package com.droidlogic.dtvkit.inputsource;

import android.media.tv.TvContract;
import android.media.tv.TvContentRating;
import android.net.Uri;
import android.util.Log;
import android.text.TextUtils;

import com.droidlogic.dtvkit.companionlibrary.EpgSyncJobService;
import com.droidlogic.dtvkit.companionlibrary.model.Channel;
import com.droidlogic.dtvkit.companionlibrary.model.EventPeriod;
import com.droidlogic.dtvkit.companionlibrary.model.InternalProviderData;
import com.droidlogic.dtvkit.companionlibrary.model.Program;
import org.droidlogic.dtvkit.DtvkitGlueClient;
import com.droidlogic.settings.PropSettingManager;

import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.HashMap;

public class DtvkitEpgSync extends EpgSyncJobService {
    private static final String TAG = "EpgSyncJobService";

    private final static HashMap<String, ArrayList<String>> genresMap = new HashMap<String, ArrayList<String>>();

    static {
        ArrayList<String> moviesList = new ArrayList<>();
        moviesList.add("movie/drama (general)");
        moviesList.add("detective/thriller");
        moviesList.add("adventure/western/war");
        moviesList.add("science fiction/fantasy/horror");
        moviesList.add("comedy");
        moviesList.add("soap/melodrama/folkloric");
        moviesList.add("romance");
        moviesList.add("serious/classical/religious/historical movie/drama");
        moviesList.add("adult movie/drama");
        genresMap.put("movies", moviesList);

        ArrayList<String> newsList = new ArrayList<>();
        newsList.add("news/current affairs (general)");
        newsList.add("news/weather report");
        newsList.add("news magazine");
        newsList.add("documentary");
        newsList.add("discussion/interview/debate");
        genresMap.put("news", newsList);

        ArrayList<String> entertainmentList = new ArrayList<>();
        entertainmentList.add("show/game show (general)");
        entertainmentList.add("game show/quiz/contest");
        entertainmentList.add("variety show");
        entertainmentList.add("talk show");
        genresMap.put("entertainment", entertainmentList);

        ArrayList<String> sportList = new ArrayList<>();
        sportList.add("sports (general)");
        sportList.add("special events (Olympic Games, World Cup, etc.)");
        sportList.add("sports magazines");
        sportList.add("football/soccer");
        sportList.add("tennis/squash");
        sportList.add("athletics");
        sportList.add("motor sport");
        sportList.add("water sport");
        sportList.add("winter sports");
        sportList.add("equestrian");
        sportList.add("martial sports");
        genresMap.put("sport", sportList);

        ArrayList<String> childrensList = new ArrayList<>();
        childrensList.add("children's/youth programmes (general)");
        childrensList.add("pre-school children's programmes");
        childrensList.add("entertainment programmes for 6 to14");
        childrensList.add("entertainment programmes for 10 to 16");
        childrensList.add("informational/educational/school programmes");
        childrensList.add("cartoons/puppets");
        genresMap.put("childrens", childrensList);

        ArrayList<String> musicList = new ArrayList<>();
        musicList.add("music/ballet/dance (general)");
        musicList.add("rock/pop");
        musicList.add("serious music/classical music");
        musicList.add("folk/traditional music");
        musicList.add("jazz");
        musicList.add("musical/opera");
        musicList.add("ballet");
        genresMap.put("music", musicList);

        ArrayList<String> artsList = new ArrayList<>();
        artsList.add("arts/culture (without music, general)");
        artsList.add("performing arts");
        artsList.add("religion");
        artsList.add("popular culture/traditional arts");
        artsList.add("literature");
        artsList.add("film/cinema");
        artsList.add("experimental film/video");
        artsList.add("broadcasting/press");
        artsList.add("new media");
        artsList.add("arts/culture magazines");
        artsList.add("fashion");
        genresMap.put("arts", artsList);

        ArrayList<String> socialList = new ArrayList<>();
        socialList.add("social/political issues/economics (general)");
        socialList.add("magazines/reports/documentary");
        socialList.add("economics/social advisory");
        socialList.add("remarkable people");
        genresMap.put("social", socialList);

        ArrayList<String> educationList = new ArrayList<>();
        educationList.add("education/science/factual topics (general)");
        educationList.add("nature/animals/environment");
        educationList.add("technology/natural sciences");
        educationList.add("medicine/physiology/psychology");
        educationList.add("foreign countries/expeditions");
        educationList.add("social/spiritual sciences");
        educationList.add("further education");
        educationList.add("languages");
        genresMap.put("education", educationList);

        ArrayList<String> leisureList = new ArrayList<>();
        leisureList.add("leisure hobbies (general)");
        leisureList.add("tourism/travel");
        leisureList.add("handicraft");
        leisureList.add("motoring");
        leisureList.add("fitness and health");
        leisureList.add("cooking");
        leisureList.add("advertisement/shopping");
        leisureList.add("gardening");
        genresMap.put("leisure", leisureList);

        ArrayList<String> specialList = new ArrayList<>();
        specialList.add("original language");
        specialList.add("black and white");
        specialList.add("unpublished");
        specialList.add("live broadcast");
        specialList.add("plano-stereoscopic");
        specialList.add("local or regional");
        genresMap.put("special", specialList);

        ArrayList<String> adultList = new ArrayList<>();
        adultList.add("adult (general)");
        genresMap.put("adult", adultList);
    }

    @Override
    public List<Channel> getChannels(boolean syncCurrent) {
        List<Channel> channels = new ArrayList<>();

        Log.i(TAG, "Get channels for epg sync, current: " + syncCurrent);

        try {
            String request = "Dvb.getListOfServices";
            if (!syncCurrent) {
                request = "Dvb.getFullListOfServices";
            }
            JSONObject obj = DtvkitGlueClient.getInstance().request(request, new JSONArray());

            Log.i(TAG, "getChannels=" + obj.toString());

            JSONArray services = obj.getJSONArray("data");

            for (int i = 0; i < services.length(); i++)
            {
                JSONObject service = services.getJSONObject(i);
                String uri = service.getString("uri");

                InternalProviderData data = new InternalProviderData();
                data.put("dvbUri", uri);
                data.put("hidden", service.getBoolean("hidden"));
                data.put("network_id", service.getInt("network_id"));
                data.put("frequency", service.getInt("freq"));
                JSONObject satellite = new JSONObject();
                satellite.put("satellite_info_name", service.getString("sate_name"));
                data.put("satellite_info", satellite.toString());
                JSONObject transponder = new JSONObject();
                String tranponderDisplay = service.getString("transponder");
                transponder.put("transponder_info_display_name", tranponderDisplay);
                if (tranponderDisplay != null) {
                    String[] splitTransponder = tranponderDisplay.split("/");
                    if (splitTransponder != null && splitTransponder.length == 3) {
                        transponder.put("transponder_info_satellite_name", service.getString("sate_name"));
                        transponder.put("transponder_info_frequency", splitTransponder[0]);
                        transponder.put("transponder_info_polarity", splitTransponder[1]);
                        transponder.put("transponder_info_symbol", splitTransponder[2]);
                    }
                }
                data.put("transponder_info", transponder.toString());
                data.put("video_pid", service.getInt("video_pid"));
                data.put("video_codec", service.getString("video_codec"));
                data.put("is_data", service.getBoolean("is_data"));
                data.put("channel_signal_type", service.getString("sig_name"));
                data.put("scrambled", service.getBoolean("scrambled")?1:0);//to match with droidlogic_tv.jar
                if (service.has("mod")) {
                    data.put("modulation", parseModulationInt(service.getString("mod")));
                } else {
                    data.put("modulation", parseModulationInt("auto"));
                }
                if (service.has("fec")) {
                    data.put("fec", service.getString("fec"));
                } else {
                    data.put("fec", "auto");
                }
                if (PropSettingManager.getBoolean(PropSettingManager.CI_PROFILE_ADD_TEST, false) && (i % 4 != 0)) {
                    int countFlag = i % 4;
                    data.put("ci_number", countFlag);
                    data.put("profile_name", "profile_name" + countFlag);
                    data.put("slot_id", "slot_id" + countFlag);
                    data.put("tune_quietly", 0);
                    if (countFlag == 1) {
                        if (i < 4) {
                            data.put("is_virtual_channel", true);
                        } else {
                            data.put("is_virtual_channel", false);
                        }
                        data.put("profile_selectable", "true");
                        data.put("service_type_label", "op test");
                    } else if (countFlag == 2) {
                        data.put("profile_ver", "v1");
                        if (i < 4) {
                            data.put("profile_selectable", "true");
                        } else {
                            data.put("profile_selectable", "false");
                        }
                    } else if (countFlag == 3) {
                        data.put("profile_ver", "v2");
                        if (i < 4) {
                            data.put("profile_selectable", "true");
                        } else {
                            data.put("profile_selectable", "false");
                        }
                    }
                } else {
                    tryToPutStringToInternalProviderData(data, "ci_number", service, "ci_number");
                    tryToPutStringToInternalProviderData(data, "profile_name", service, "profile_name");
                    tryToPutStringToInternalProviderData(data, "profile_selectable", service, "profile_selectable");
                    tryToPutStringToInternalProviderData(data, "slot_id", service, "slot_id");
                    tryToPutIntToInternalProviderData(data, "tune_quietly", service, "tune_quietly");
                    tryToPutStringToInternalProviderData(data, "profile_ver", service, "profile_ver");
                    tryToPutBooleanToInternalProviderData(data, "is_virtual_channel", service, "is_virtual_channel");
                    tryToPutStringToInternalProviderData(data, "service_type_label", service, "service_type_label");
                }
                if (service.has("raw_name")) {
                    data.put("raw_displayname", service.getString("raw_name"));
                } else {
                    data.put("raw_displayname", service.getString("name"));
                }
                if (service.has("raw_lcn")) {
                    data.put("raw_displaynumber", String.format(Locale.ENGLISH, "%d", service.getInt("raw_lcn")));
                } else {
                    data.put("raw_displaynumber", String.format(Locale.ENGLISH, "%d", service.getInt("lcn")));
                }
                String channelType = TvContract.Channels.TYPE_OTHER;
                String signal_type = service.optString("sig_name", "TYPE_OTHER");
                switch (signal_type) {
                    case "DVB-T":
                        channelType = TvContract.Channels.TYPE_DVB_T;
                        break;
                    case "DVB-T2":
                        channelType = TvContract.Channels.TYPE_DVB_T2;
                        break;
                    case "DVB-C":
                        channelType = TvContract.Channels.TYPE_DVB_C;
                        break;
                    case "DVB-S":
                        channelType = TvContract.Channels.TYPE_DVB_S;
                        break;
                    case "DVB-S2":
                        channelType = TvContract.Channels.TYPE_DVB_S2;
                        break;
                    case "ISDB-T":
                        channelType = TvContract.Channels.TYPE_ISDB_T;
                        break;
                    default:
                        break;
                }
                channels.add(new Channel.Builder()
                        .setDisplayName(service.getString("name"))
                        .setType(channelType)
                        .setDisplayNumber(String.format(Locale.ENGLISH, "%d", service.getInt("lcn")))
                        .setServiceType(service.getBoolean("is_data") ? TvContract.Channels.SERVICE_TYPE_OTHER : (service.getBoolean("radio") ? TvContract.Channels.SERVICE_TYPE_AUDIO :
                                TvContract.Channels.SERVICE_TYPE_AUDIO_VIDEO))
                        .setOriginalNetworkId(Integer.parseInt(uri.substring(6, 10), 16))
                        .setTransportStreamId(Integer.parseInt(uri.substring(11, 15), 16))
                        .setServiceId(Integer.parseInt(uri.substring(16, 20), 16))
                        .setInternalProviderData(data)
                        .build());
            }

        } catch (Exception e) {
            Log.e(TAG, "getChannels Exception = " + e.getMessage());
        }

        return channels;
    }

    @Override
    public List<Program> getProgramsForChannel(Uri channelUri, Channel channel, long startMs, long endMs) {
        List<Program> programs = new ArrayList<>();
        long starttime, endtime;
        int parental_rating;
        int content_value;
        try {
            String dvbUri = getDtvkitChannelUri(channel);

            Log.i(TAG, String.format("Get channel programs for epg sync. Uri %s, startMs %d, endMs %d",
                dvbUri, startMs, endMs));

            JSONArray args = new JSONArray();
            args.put(dvbUri); // uri
            args.put(startMs/1000);
            args.put(endMs/1000);
            JSONArray events = DtvkitGlueClient.getInstance().request("Dvb.getListOfEvents", args).getJSONArray("data");

            for (int i = 0; i < events.length(); i++)
            {
                JSONObject event = events.getJSONObject(i);

                InternalProviderData data = new InternalProviderData();
                data.put("dvbUri", dvbUri);
                starttime = event.getLong("startutc") * 1000;
                endtime = event.getLong("endutc") * 1000;
                parental_rating = event.getInt("rating");
                content_value = event.optInt("content_value");
                if (starttime >= endMs || endtime <= startMs) {
                    Log.i(TAG, "Skip##  startMs:endMs=["+startMs+":"+endMs+"]  event:startT:endT=["+starttime+":"+endtime+"]");
                    continue;
                }else{
                    if (content_value > 0xff) {
                        String genre_l = getGenreL2Desc(event.getString("genre"), content_value);
                        if (!TextUtils.isEmpty(genre_l)) {
                            data.put("genre", genre_l);
                        }
                    }
                    Program pro = new Program.Builder()
                            .setChannelId(channel.getId())
                            .setTitle(event.getString("name"))
                            .setStartTimeUtcMillis(starttime)
                            .setEndTimeUtcMillis(endtime)
                            .setDescription(event.getString("description"))
                            .setCanonicalGenres(getGenres(event.getString("genre"), content_value))
                            .setInternalProviderData(data)
                            .setContentRatings(parental_rating == 0 ? null : parseParentalRatings(parental_rating, event.getString("name")))
                            .build();
                    programs.add(pro);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }

        return programs;
    }

    @Override
    public List<Program> getAllProgramsForChannel(Uri channelUri, Channel channel) {
        List<Program> programs = new ArrayList<>();
        int parental_rating;

        try {
            String dvbUri = getDtvkitChannelUri(channel);

            Log.i(TAG, String.format("Get channel programs for epg sync. Uri %s", dvbUri));

            JSONArray args = new JSONArray();
            args.put(dvbUri); // uri
            //starttime\endtime use min and max.
            JSONArray events = DtvkitGlueClient.getInstance().request("Dvb.getListOfEvents", args).getJSONArray("data");

            for (int i = 0; i < events.length(); i++)
            {
                JSONObject event = events.getJSONObject(i);

                InternalProviderData data = new InternalProviderData();
                data.put("dvbUri", dvbUri);
                int content_value = event.optInt("content_value");
                if (content_value > 0xff) {
                    String genre_l = getGenreL2Desc(event.getString("genre"), content_value);
                    if (!TextUtils.isEmpty(genre_l)) {
                        data.put("genre", genre_l);
                    }
                }
                parental_rating = event.getInt("rating");
                Program pro = new Program.Builder()
                        .setChannelId(channel.getId())
                        .setTitle(event.getString("name"))
                        .setStartTimeUtcMillis(event.getLong("startutc") * 1000)
                        .setEndTimeUtcMillis(event.getLong("endutc") * 1000)
                        .setDescription(event.getString("description"))
                        .setLongDescription(event.getString("description_extern"))
                        .setCanonicalGenres(getGenres(event.getString("genre"), content_value))
                        .setInternalProviderData(data)
                        .setContentRatings(parental_rating == 0 ? null : parseParentalRatings(parental_rating, event.getString("name")))
                        .build();
                programs.add(pro);
            }
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }
        Log.i(TAG, "## programs["+ programs.size() +"] ##");

        return programs;
    }

    public List<Program> getNowNextProgramsForChannel(Uri channelUri, Channel channel) {
        List<Program> programs = new ArrayList<>();

        try {
            String dvbUri = getDtvkitChannelUri(channel);

            Log.i(TAG, "Get channel now next programs for epg sync. Uri " + dvbUri);

            JSONArray args = new JSONArray();
            args.put(dvbUri); // uri
            JSONObject events = DtvkitGlueClient.getInstance().request("Dvb.getNowNextEvents", args).getJSONObject("data");

            JSONObject now = events.optJSONObject("now");
            if (now != null) {
                InternalProviderData data = new InternalProviderData();
                data.put("dvbUri", dvbUri);
                int content_value = now.optInt("content_value");
                if (content_value > 0xff) {
                    String genre_l = getGenreL2Desc(now.getString("genre"), content_value);
                    if (!TextUtils.isEmpty(genre_l)) {
                        data.put("genre", genre_l);
                    }
                }
                programs.add(new Program.Builder()
                        .setChannelId(channel.getId())
                        .setTitle(now.getString("name"))
                        .setStartTimeUtcMillis(now.getLong("startutc") * 1000)
                        .setEndTimeUtcMillis(now.getLong("endutc") * 1000)
                        .setDescription(now.getString("description"))
                        .setCanonicalGenres(getGenres(now.getString("genre"), content_value))
                        .setInternalProviderData(data)
                        .build());
            }

            JSONObject next = events.optJSONObject("next");
            if (next != null) {
                InternalProviderData data = new InternalProviderData();
                data.put("dvbUri", dvbUri);
                int content_value = next.optInt("content_value");
                if (content_value > 0xff) {
                    String genre_l = getGenreL2Desc(next.getString("genre"), content_value);
                    if (!TextUtils.isEmpty(genre_l)) {
                        data.put("genre", genre_l);
                    }
                }
                programs.add(new Program.Builder()
                        .setChannelId(channel.getId())
                        .setTitle(next.getString("name"))
                        .setStartTimeUtcMillis(next.getLong("startutc") * 1000)
                        .setEndTimeUtcMillis(next.getLong("endutc") * 1000)
                        .setDescription(next.getString("description"))
                        .setCanonicalGenres(getGenres(next.getString("genre"), content_value))
                        .setInternalProviderData(data)
                        .build());
            }
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }

        return programs;
    }

    @Override
    public List<EventPeriod> getListOfUpdatedEventPeriods()
    {
        List<EventPeriod> eventPeriods = new ArrayList<>();

        Log.i(TAG, "getListOfUpdatedEventPeriods");

        try {
            JSONArray periods = DtvkitGlueClient.getInstance().request("Dvb.getListOfUpdatedEventPeriods", new JSONArray()).getJSONArray("data");

            Log.i(TAG, periods.toString());

            for (int i = 0; i < periods.length(); i++)
            {
                JSONObject period = periods.getJSONObject(i);

                eventPeriods.add(new EventPeriod(period.getString("uri"),period.getLong("startutc"),period.getLong("endutc")));
            }
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }

        return eventPeriods;
    }

    private String[] getGenres(String genre, int content_value) {
        if (content_value > 0xff) {
            //tv provider don't support level 2
            return new String[]{};
        } else {
            switch (genre)
            {
                case "movies":
                    return new String[]{TvContract.Programs.Genres.MOVIES};
                case "news":
                    return new String[]{TvContract.Programs.Genres.NEWS};
                case "entertainment":
                    return new String[]{TvContract.Programs.Genres.ENTERTAINMENT};
                case "sport":
                    return new String[]{TvContract.Programs.Genres.SPORTS};
                case "childrens":
                    return new String[]{TvContract.Programs.Genres.FAMILY_KIDS};
                case "music":
                    return new String[]{TvContract.Programs.Genres.MUSIC};
                case "arts":
                    return new String[]{TvContract.Programs.Genres.ARTS};
                case "social":
                    return new String[]{TvContract.Programs.Genres.LIFE_STYLE};
                case "education":
                    return new String[]{TvContract.Programs.Genres.EDUCATION};
                case "leisure":
                    return new String[]{TvContract.Programs.Genres.LIFE_STYLE};
                default:
                    return new String[]{};
            }
        }
    }

    private String getGenreL2Desc(String genre, int content_value) {
        if (content_value > 0xff) {
            int lvl_2 = content_value & 0x0f;
            String value = "";
            try {
                value = genresMap.get(genre).get(lvl_2);
            } catch (Exception e) {
            }
            return value;
        } else {
            return "";
        }
    }

    private TvContentRating[] parseParentalRatings(int parentalRating, String title)
    {
        TvContentRating ratings_arry[];
        String ratingSystemDefinition = "DVB";
        String ratingDomain = "com.android.tv";
        String DVB_ContentRating[] = {"DVB_4", "DVB_5", "DVB_6", "DVB_7", "DVB_8", "DVB_9", "DVB_10", "DVB_11", "DVB_12", "DVB_13", "DVB_14", "DVB_15", "DVB_16", "DVB_17", "DVB_18"};

        ratings_arry = new TvContentRating[1];
        parentalRating += 3; //minimum age = rating + 3 years
        Log.d(TAG, "parseParentalRatings parentalRating:"+ parentalRating + ", title = " + title);
        if (parentalRating >= 4 && parentalRating <= 18) {
            TvContentRating r = TvContentRating.createRating(ratingDomain, ratingSystemDefinition, DVB_ContentRating[parentalRating-4], null);
            if (r != null) {
                ratings_arry[0] = r;
                Log.d(TAG, "parse ratings add rating:"+r.flattenToString()  + ", title = " + title);
            }
        }else {
            ratings_arry = null;
        }

        return ratings_arry;
    }

    private boolean tryToPutStringToInternalProviderData(InternalProviderData data, String key1, JSONObject obj, String key2) {
        boolean result = false;
        if (data != null && obj != null && key1 != null && key2 != null) {
            try {
                data.put(key1, obj.getString(key2));
                result = true;
            } catch (Exception e) {
                //Log.e(TAG, "tryToPutStringToInternalProviderData key2 = " + key2 + "Exception = " + e.getMessage());
            }
        }
        return result;
    }

    private boolean tryToPutIntToInternalProviderData(InternalProviderData data, String key1, JSONObject obj, String key2) {
        boolean result = false;
        if (data != null && obj != null && key1 != null && key2 != null) {
            try {
                data.put(key1, obj.getInt(key2));
                result = true;
            } catch (Exception e) {
                //Log.e(TAG, "tryToPutIntToInternalProviderData key2 = " + key2 + "Exception = " + e.getMessage());
            }
        }
        return result;
    }

    private boolean tryToPutBooleanToInternalProviderData(InternalProviderData data, String key1, JSONObject obj, String key2) {
        boolean result = false;
        if (data != null && obj != null && key1 != null && key2 != null) {
            try {
                data.put(key1, obj.getBoolean(key2));
                result = true;
            } catch (Exception e) {
                //Log.e(TAG, "tryToPutBooleanToInternalProviderData key2 = " + key2 + "Exception = " + e.getMessage());
            }
        }
        return result;
    }

    private String getDtvkitChannelUri(Channel channel) {
        String result = null;
        if (channel != null) {
            try {
                InternalProviderData data = channel.getInternalProviderData();
                if (data != null) {
                    result = (String)data.get(Channel.KEY_DTVKIT_URI);
                } else {
                    result = String.format("dvb://%04x.%04x.%04x", channel.getOriginalNetworkId(), channel.getTransportStreamId(), channel.getServiceId());
                }
            } catch (Exception e) {
                Log.e(TAG, "getDtvkitChannelUri Exception = " + e.getMessage());
            }
        }
        return result;
    }

    private int parseModulationInt(String modulation) {
        /*modulation int values refs to: droidlogic_tv.jar: TvChannelParams.java*/
        int mod = 6;//MODULATION_QAM_AUTO
        if (TextUtils.isEmpty(modulation)) {
            return mod;
        }
        switch (modulation) {
            case "qpsk":
                mod = 0;//MODULATION_QPSK
                break;
            case "16qam":
                mod = 1;//MODULATION_QAM_16
                break;
            case "8psk":
                mod = 9;//MODULATION_PSK_8
                break;
            case "16apsk":
                mod = 10;//MODULATION_APSK_16
                break;
            case "32apsk":
                mod = 11;//MODULATION_APSK_32
                break;
            case "dqpsk":
                mod = 12;//MODULATION_DQPSK
                break;
            case "32qam":
                mod = 2;//MODULATION_QAM_32
                break;
            case "64qam":
                mod = 3;//MODULATION_QAM_64
                break;
            case "128qam":
                mod = 4;//MODULATION_QAM_128
                break;
            case "256qam":
                mod = 5;//MODULATION_QAM_256
                break;
            default:
                mod = 6;//MODULATION_QAM_AUTO
                break;
        }
        return mod;
    }
}
