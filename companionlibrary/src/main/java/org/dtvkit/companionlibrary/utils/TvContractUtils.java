/*
 * Copyright 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Modifications copyright (C) 2018 DTVKit
 */

package com.droidlogic.dtvkit.companionlibrary.utils;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.media.tv.TvContentRating;
import android.media.tv.TvContract;
import android.media.tv.TvContract.Channels;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.Log;
import android.util.LongSparseArray;
import android.util.SparseArray;
import android.content.ContentProviderOperation;
import android.content.OperationApplicationException;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.os.PersistableBundle;

import com.droidlogic.dtvkit.companionlibrary.model.Channel;
import com.droidlogic.dtvkit.companionlibrary.model.Program;
import com.droidlogic.dtvkit.companionlibrary.model.InternalProviderData;
import com.droidlogic.dtvkit.companionlibrary.EpgSyncJobService;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Comparator;
import java.util.Collections;

/**
 * Static helper methods for working with {@link android.media.tv.TvContract}.
 */
public class TvContractUtils {
    public static final String PREFERENCES_FILE_KEY = "com.droidlogic.dtvkit.companionlibrary";
    private static final String TAG = "TvContractUtils";
    private static final boolean DEBUG = false;
    private static final int BATCH_OPERATION_COUNT = 50;

    private static final String[] USER_SETTING_FLAG_KEY = {Channel.KEY_SET_FAVOURITE, Channel.KEY_SET_HIDDEN, Channel.KEY_IS_FAVOURITE, Channel.KEY_FAVOURITE_INFO, Channel.KEY_HIDDEN,
                                                       Channel.KEY_SET_DISPLAYNAME, Channel.KEY_NEW_DISPLAYNAME, Channel.KEY_SET_DISPLAYNUMBER, Channel.KEY_NEW_DISPLAYNUMBER, Channel.KEY_RAW_DISPLAYNAME, Channel.KEY_RAW_DISPLAYNUMBER};
    private static final String[] USER_SETTING_FLAG_DEFAULT = {"0", "0", "0", "", "false",
                                                          "0", "" ,"0", "","", ""};

    /**
     * Updates the list of available channels.
     *
     * @param context The application's context.
     * @param inputId The ID of the TV input service that provides this TV channel.
     * @param channels The updated list of channels.
     * @hide
     */

    public static void updateChannels(Context context, String inputId, boolean isSearched, List<Channel> channels, String updateChannelType, PersistableBundle extras) {
        // Create a map from original network ID to channel row ID for existing channels.
        ArrayMap<String, Long> channelMap = new ArrayMap<>();
        ArrayMap<String, ArrayMap<String, String>> channelUseSettingValueMap = new ArrayMap<>();
        Map<Uri, String> logos = new HashMap<>();
        ArrayList<ContentProviderOperation> ops = new ArrayList<>();

        //1.fisrt get existed channels in tv.db
        cacheRelatedChannel(channelMap, channelUseSettingValueMap,
            context, inputId, isSearched, channels, updateChannelType, extras);
        //2.find new channel and channels that need to be updated
        addRelatedChannelToContentProviderOperation(ops, channelMap, channelUseSettingValueMap, logos,
            context, inputId, isSearched, channels, updateChannelType, extras);
        //3.delete channels that don't exist in dtvkit db anymore
        // Deletes channels which don't exist in the new feed firstly.
        addRelatedChannelToContentProviderOperationAndDelete(channelMap, context);
        //4.deal insert or update channels to tv.db
        dealUpdateOrInsertInContentProviderOperation(ops, logos, context);

        //notify immediately as livetv may be in background and android r sends such contentprovider notification after 10s in ContentService
        if (VERSION.SDK_INT > VERSION_CODES.P + 1) {
            context.getContentResolver().notifyChange(Channels.CONTENT_URI, null, 1 << 15/*ContentResolver.NOTIFY_NO_DELAY*/);
        }
    }

    //1.fisrt get existed channels in tv.db
    private static void cacheRelatedChannel(ArrayMap<String, Long> channelMap, ArrayMap<String, ArrayMap<String, String>> channelUseSettingValueMap,
            Context context, String inputId, boolean isSearched, List<Channel> channels, String updateChannelType, PersistableBundle extras) {
        Uri channelsUri = TvContract.buildChannelsUriForInput(inputId);
        String[] projection = {Channels._ID, Channels.COLUMN_TYPE, Channels.COLUMN_ORIGINAL_NETWORK_ID, Channels.COLUMN_TRANSPORT_STREAM_ID,
                Channels.COLUMN_SERVICE_ID, Channels.COLUMN_DISPLAY_NAME, Channels.COLUMN_DISPLAY_NUMBER,
                Channels.COLUMN_INTERNAL_PROVIDER_DATA};
        ContentResolver resolver = context.getContentResolver();
        Cursor cursor = null;
        try {
            cursor = resolver.query(channelsUri, projection, null, null, null);
            InternalProviderData internalProviderData = null;
            byte[] internalProviderByteData = null;
            String displayName = null;
            String displayNumber = null;
            String rawDisplayName = null;
            String rawDisplayNumber = null;
            boolean setRawDisplayNumber = false;
            boolean setRawDisplayName = false;
            while (cursor != null && cursor.moveToNext()) {
                long rowId = cursor.getLong(0);
                String channelType = cursor.getString(1);
                int originalNetworkId = cursor.getInt(2);
                int transportStreamId = cursor.getInt(3);
                int serviceId = cursor.getInt(4);
                displayName = cursor.getString(5);
                displayNumber = cursor.getString(6);
                int type = cursor.getType(7);//InternalProviderData type
                int frequency = 0;
                String ciNumber = null;
                if (type == Cursor.FIELD_TYPE_BLOB) {
                    internalProviderByteData = cursor.getBlob(7);
                    if (internalProviderByteData != null) {
                        internalProviderData = new InternalProviderData(internalProviderByteData);
                    }
                } else {
                    if (DEBUG) Log.i(TAG, "COLUMN_INTERNAL_PROVIDER_DATA other type");
                }
                if (internalProviderData != null) {
                    if (DEBUG) Log.i(TAG, "internalProviderData = " + internalProviderData.toString());
                    frequency = Integer.valueOf((String)internalProviderData.get(Channel.KEY_FREQUENCY));
                    ciNumber = (String)internalProviderData.get(Channel.KEY_CHANNEL_CI_NUMBER);
                    rawDisplayName = (String)internalProviderData.get(Channel.KEY_RAW_DISPLAYNAME);
                    rawDisplayNumber = (String)internalProviderData.get(Channel.KEY_RAW_DISPLAYNUMBER);
                    setRawDisplayNumber = "1".equals((String)internalProviderData.get(Channel.KEY_SET_DISPLAYNUMBER));
                    setRawDisplayName = "1".equals((String)internalProviderData.get(Channel.KEY_SET_DISPLAYNAME));
                    if (setRawDisplayNumber) {
                        internalProviderData.put(Channel.KEY_NEW_DISPLAYNUMBER, displayNumber);
                    }
                    if (setRawDisplayName) {
                        internalProviderData.put(Channel.KEY_NEW_DISPLAYNAME, displayName);
                    }
                }
                String uniqueStr = getUniqueStrForChannel(internalProviderData, channelType, originalNetworkId, transportStreamId, serviceId, frequency, ciNumber, rawDisplayNumber);
                if (uniqueStr == null) {
                    continue;
                }
                //factory set will use "full" signalType to clear all channel in db
                String signalType = extras.getString(EpgSyncJobService.BUNDLE_KEY_SYNC_SEARCHED_SIGNAL_TYPE, null);
                if (TextUtils.isEmpty(updateChannelType) && searchSignalTypeToChannelType(signalType) != null) {
                    updateChannelType = searchSignalTypeToChannelType(signalType);
                }
                if (!("full".equals(signalType)) && !isChannelTypeMatchs(updateChannelType, channelType)) {
                    if (DEBUG) Log.i(TAG, "Skip unmatch type channels (" + updateChannelType + ":" + channelType + ")");
                    continue;
                }
                channelMap.put(uniqueStr, rowId);
                String searchMode = extras.getString(EpgSyncJobService.BUNDLE_KEY_SYNC_SEARCHED_MODE, null);
                int searchFrequency = extras.getInt(EpgSyncJobService.BUNDLE_KEY_SYNC_SEARCHED_FREQUENCY, 0);
                if (!isSearched
                        || (frequency != 0 && EpgSyncJobService.BUNDLE_VALUE_SYNC_SEARCHED_MODE_MANUAL.equals(searchMode) && searchFrequency != frequency)) {
                    saveRawUseSettingValuesToMap(uniqueStr, channelUseSettingValueMap, internalProviderData);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "updateChannels query Failed = " + e.getMessage());
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    //2.find new channel and channels that need to be updated
    private static void addRelatedChannelToContentProviderOperation(ArrayList<ContentProviderOperation> ops, ArrayMap<String, Long> channelMap, ArrayMap<String, ArrayMap<String, String>> channelUseSettingValueMap, Map<Uri, String> logos,
            Context context, String inputId, boolean isSearched, List<Channel> channels, String updateChannelType, PersistableBundle extras) {
        // If a channel exists, update it. If not, insert a new one.
        //Map<Uri, String> logos = new HashMap<>();
        for (Channel channel : channels) {
            ContentValues values = new ContentValues();
            values.put(Channels.COLUMN_INPUT_ID, inputId);
            values.putAll(channel.toContentValues());
            // If some required fields are not populated, the app may crash, so defaults are used
            if (channel.getPackageName() == null) {
                // If channel does not include package name, it will be added
                values.put(Channels.COLUMN_PACKAGE_NAME, context.getPackageName());
            }
            if (channel.getInputId() == null) {
                // If channel does not include input id, it will be added
                values.put(Channels.COLUMN_INPUT_ID, inputId);
            }
            if (channel.getType() == null) {
                // If channel does not include type it will be added
                values.put(Channels.COLUMN_TYPE, Channels.TYPE_OTHER);
            }

            String channelType = channel.getType();
            int originalNetworkId = channel.getOriginalNetworkId();
            int transportStreamId = channel.getTransportStreamId();
            int serviceId = channel.getServiceId();
            InternalProviderData internalProviderData = channel.getInternalProviderData();
            int frequency = 0;
            String ciNumber = null;
            String rawDisplayNumber = null;
            if (internalProviderData != null) {
                try {
                    frequency = Integer.valueOf((String)internalProviderData.get(Channel.KEY_FREQUENCY));
                } catch (Exception e) {
                    Log.d(TAG, "updateChannels no frequency info Exception = " + e.getMessage());
                }
                try {
                    ciNumber = (String)internalProviderData.get(Channel.KEY_CHANNEL_CI_NUMBER);
                } catch (Exception e) {
                    //Log.d(TAG, "updateChannels no ciNumber info Exception = " + e.getMessage());
                }
                try {
                    rawDisplayNumber = (String)internalProviderData.get(Channel.KEY_RAW_DISPLAYNUMBER);
                } catch (Exception e) {
                    Log.d(TAG, "updateChannels no rawDisplayNumber info Exception = " + e.getMessage());
                }
            } else {
                Log.d(TAG, "updateChannels no frequency info");
                continue;
            }
            String uniqueStr = getUniqueStrForChannel(internalProviderData, channelType, originalNetworkId, transportStreamId, serviceId, frequency, ciNumber, rawDisplayNumber);
            if (uniqueStr == null) {
                continue;
            }
            String searchMode = extras.getString(EpgSyncJobService.BUNDLE_KEY_SYNC_SEARCHED_MODE, null);
            Long rowId = channelMap.get(uniqueStr);
            if (EpgSyncJobService.BUNDLE_VALUE_SYNC_SEARCHED_MODE_AUTO.equals(searchMode)) {
                rowId = null;
            }
            byte[] dataByte = null;
            if (internalProviderData == null) {
                internalProviderData = new InternalProviderData();
            }
            restoreRawUseSettingValuesToInternalProviderData(uniqueStr, channelUseSettingValueMap, internalProviderData);
            dataByte = internalProviderData.toString().getBytes();
            if (dataByte != null && dataByte.length > 0) {
                values.put(TvContract.Channels.COLUMN_INTERNAL_PROVIDER_DATA, dataByte);
            } else {
                values.putNull(TvContract.Channels.COLUMN_INTERNAL_PROVIDER_DATA);
            }
            if (DEBUG) {
                Log.d(TAG, String.format("Mapping %s to %d", uniqueStr, rowId));
            }
            ArrayMap<String, String> singleUserSettings = channelUseSettingValueMap.get(uniqueStr);
            if (singleUserSettings != null && singleUserSettings.size() > 0) {
                if ("1".equals(singleUserSettings.get(Channel.KEY_SET_DISPLAYNUMBER))) {
                    values.put(TvContract.Channels.COLUMN_DISPLAY_NUMBER, singleUserSettings.get(Channel.KEY_NEW_DISPLAYNUMBER));
                }
                if ("1".equals(singleUserSettings.get(Channel.KEY_SET_DISPLAYNAME))) {
                    values.put(TvContract.Channels.COLUMN_DISPLAY_NAME, singleUserSettings.get(Channel.KEY_NEW_DISPLAYNAME));
                }
            }
            Uri uri = null;
            if (rowId == null) {
                //uri = resolver.insert(TvContract.Channels.CONTENT_URI, values);
                ops.add(ContentProviderOperation.newInsert(
                                    TvContract.Channels.CONTENT_URI)
                                    .withValues(values)
                                    .build());
                if (DEBUG) {
                    Log.d(TAG, "Adding channel " + channel.getDisplayName() + " at " + uri);
                }
            } else {
                values.put(Channels._ID, rowId);
                uri = TvContract.buildChannelUri(rowId);
                if (DEBUG) {
                    Log.d(TAG, "Updating channel " + channel.getDisplayName() + " at " + uri);
                }
                //resolver.update(uri, values, null, null);
                ops.add(ContentProviderOperation.newUpdate(
                                    TvContract.buildChannelUri(rowId))
                                    .withValues(values)
                                    .build());
                channelMap.remove(uniqueStr);
            }
            /*if (channel.getChannelLogo() != null && !TextUtils.isEmpty(channel.getChannelLogo())) {
                logos.put(TvContract.buildChannelLogoUri(uri), channel.getChannelLogo());
            }*/
        }
    }

    //3.delete channels that don't exist in dtvkit db anymore
    private static void addRelatedChannelToContentProviderOperationAndDelete(ArrayMap<String, Long> channelMap, Context context) {
        int size = channelMap.size();
        ArrayList<ContentProviderOperation> deleteOps = new ArrayList<>();
        ContentResolver resolver = context.getContentResolver();
        for (int i = 0; i < size; ++i) {
            Long rowId = channelMap.valueAt(i);
            if (DEBUG) {
                Log.d(TAG, " dealDelete add Deleting channel " + rowId);
            }
            deleteOps.add(ContentProviderOperation.newDelete(
                                    TvContract.buildChannelUri(rowId))
                                    .build());
            //resolver.delete(TvContract.buildChannelUri(rowId), null, null);
            /*SharedPreferences.Editor editor = context.getSharedPreferences(
                    PREFERENCES_FILE_KEY, Context.MODE_PRIVATE).edit();
            editor.apply();*/
        }
        for (int i = 0; i < deleteOps.size(); i += BATCH_OPERATION_COUNT) {
            int toIndex =
                    (i + BATCH_OPERATION_COUNT) > deleteOps.size()
                            ? deleteOps.size()
                            : (i + BATCH_OPERATION_COUNT);
            ArrayList<ContentProviderOperation> batchOps =
                    new ArrayList<>(deleteOps.subList(i, toIndex));
            if (DEBUG) {
                Log.d(TAG, "dealDelete deleteChannels from fromIndex " + i + " to " + toIndex);
            }
            try {
                resolver.applyBatch(TvContract.AUTHORITY, batchOps);
            } catch (Exception e) {
                Log.e(TAG, "dealDelete deleteChannels updateChannels Failed = " + e.getMessage());
            }
        }
        deleteOps.clear();
    }

    //4.deal insert or update channels to tv.db
    private static void dealUpdateOrInsertInContentProviderOperation(ArrayList<ContentProviderOperation> ops, Map<Uri, String> logos, Context context) {
        ContentResolver resolver = context.getContentResolver();
        for (int i = 0; i < ops.size(); i += BATCH_OPERATION_COUNT) {
            int toIndex =
                    (i + BATCH_OPERATION_COUNT) > ops.size()
                            ? ops.size()
                            : (i + BATCH_OPERATION_COUNT);
            ArrayList<ContentProviderOperation> batchOps =
                    new ArrayList<>(ops.subList(i, toIndex));
            if (DEBUG) {
                Log.d(TAG, "updateChannels from fromIndex " + i + " to " + toIndex);
            }
            try {
                resolver.applyBatch(TvContract.AUTHORITY, batchOps);
            } catch (Exception e) {
                Log.e(TAG, "updateChannels Failed = " + e.getMessage());
            }
        }
        ops.clear();
        if (!logos.isEmpty()) {
            new InsertLogosTask(context).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, logos);
        }
    }

    //cache use settings
    private static void saveRawUseSettingValuesToMap(String uniqueKey, ArrayMap<String, ArrayMap<String, String>> map, InternalProviderData internalProviderData) {
        if (USER_SETTING_FLAG_KEY.length != USER_SETTING_FLAG_DEFAULT.length || uniqueKey == null || map == null || internalProviderData == null) {
            Log.d(TAG, "saveRawUseSettingValuesToMap USER_SETTING_FLAG and USER_SETTING_FLAG_DEFAULT are not same length or null container");
            return;
        }
        String tempStr;
        ArrayMap<String, String> child = new ArrayMap<String, String>();
        for (int i = 0; i < USER_SETTING_FLAG_KEY.length; i++) {
            try {
                tempStr = (String)internalProviderData.get(USER_SETTING_FLAG_KEY[i]);
                if (tempStr == null) {
                    tempStr = USER_SETTING_FLAG_DEFAULT[i];
                }
                if (DEBUG) {
                    Log.d(TAG, "saveRawUseSettingValuesToMap no." + i + "->" + USER_SETTING_FLAG_KEY[i] + ":" + tempStr);
                }
                child.put(USER_SETTING_FLAG_KEY[i], tempStr);
            } catch (Exception e) {
                if (DEBUG) {
                    Log.i(TAG, "saveRawUseSettingValuesToMap can't get " + USER_SETTING_FLAG_KEY[i]);
                }
            }
        }
        map.put(uniqueKey, child);
    }

    //restore use settings
    private static void restoreRawUseSettingValuesToInternalProviderData(String uniqueKey, ArrayMap<String, ArrayMap<String, String>> map, InternalProviderData internalProviderData) {
        if (USER_SETTING_FLAG_KEY.length != USER_SETTING_FLAG_DEFAULT.length || uniqueKey == null || map == null || internalProviderData == null) {
            Log.d(TAG, "restoreRawUseSettingValuesToInternalProviderData USER_SETTING_FLAG and USER_SETTING_FLAG_DEFAULT are not same length or null container");
            return;
        }
        String tempStr;
        ArrayMap<String, String> child = (ArrayMap<String, String>)map.get(uniqueKey);
        for (int i = 0; i < USER_SETTING_FLAG_KEY.length; i++) {
            try {
                tempStr = (String)child.get(USER_SETTING_FLAG_KEY[i]);
                if (Channel.KEY_HIDDEN.equals(USER_SETTING_FLAG_KEY[i])) {
                    tempStr = (String)internalProviderData.get(USER_SETTING_FLAG_KEY[i]);
                } else if (tempStr == null) {
                    tempStr = USER_SETTING_FLAG_DEFAULT[i];
                }
                if (DEBUG) {
                    Log.d(TAG, "restoreRawUseSettingValuesToInternalProviderData no." + i + "->" + USER_SETTING_FLAG_KEY[i] + ":" + tempStr);
                }
                internalProviderData.put(USER_SETTING_FLAG_KEY[i], tempStr);
            } catch (Exception e) {
                if (DEBUG) {
                    Log.i(TAG, "restoreRawUseSettingValuesToInternalProviderData can't get " + USER_SETTING_FLAG_KEY[i]);
                }
            }
        }
    }

    public static boolean isChannelTypeMatchs(String sourceType, String targetType) {
        boolean ret = false;
        if (sourceType != null && targetType != null && targetType.contains(sourceType)) {
            ret = true;
        }
        return ret;
    }

    public static String getUniqueStrForChannel(InternalProviderData internalProviderData, String channelType, int originalNetworkId, int transportStreamId, int serviceId, int frequency, String ciNumber, String rawDisplayNumber) {
        String result = null;
        try {
            if (internalProviderData != null) {
                result = (String)internalProviderData.get(Channel.KEY_DTVKIT_URI);
            } else {
                result = channelType + "-" + String.valueOf(frequency / 1000000) + "-" + rawDisplayNumber + "-" + String.valueOf(originalNetworkId) + "-" + String.valueOf(transportStreamId) + "-" + String.valueOf(serviceId) + "-" + ciNumber;
            }
        } catch (Exception e) {
            Log.d(TAG, "getUniqueStrForChannel Exception = " + e.getMessage());
        }
        return result;
    }

    public static boolean updateSingleChannelColumn(ContentResolver contentResolver, long id, String columnKey, Object value) {
        boolean ret = false;
        if (id == -1 || TextUtils.isEmpty(columnKey) || contentResolver == null) {
            return ret;
        }
        String[] projection = {columnKey};
        Uri channelsUri = TvContract.buildChannelUri(id);
        Cursor cursor = null;
        ContentValues values = null;
        try {
            cursor = contentResolver.query(channelsUri, projection, Channels._ID + "=?", new String[]{String.valueOf(id)}, null);
            while (cursor != null && cursor.moveToNext()) {
                values = new ContentValues();
                if (value instanceof byte[]) {
                    values.put(columnKey, (byte[])value);
                } else if (value instanceof String) {
                    values.put(columnKey, (String)value);
                } else if (value instanceof Integer) {
                    values.put(columnKey, (Integer)value);
                } else {
                    Log.i(TAG, "updateChannelInternalProviderData unkown data type");
                    return ret;
                }
                ret = true;
                contentResolver.update(channelsUri, values, null, null);
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.i(TAG, "updateSingleColumn mContentResolver operation Exception = " + e.getMessage());
        }
        try {
            if (cursor != null) {
                cursor.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.i(TAG, "updateSingleColumn cursor.close() Exception = " + e.getMessage());
        }
        if (DEBUG)
            Log.d(TAG, "updateSingleColumn " + (ret ? "found" : "notfound")
                    + " _id:" + id + " key:" + columnKey + " value:" + value);
        return ret;
    }

    /**
     * Builds a map of available channels.
     *
     * @param resolver Application's ContentResolver.
     * @param inputId The ID of the TV input service that provides this TV channel.
     * @return LongSparseArray mapping each channel's {@link TvContract.Channels#_ID} to the
     * Channel object.
     * @hide
     */
    public static LongSparseArray<Channel> buildChannelMap(@NonNull ContentResolver resolver,
            @NonNull String inputId) {
        Uri uri = TvContract.buildChannelsUriForInput(inputId);
        LongSparseArray<Channel> channelMap = new LongSparseArray<>();
        Cursor cursor = null;
        try {
            cursor = resolver.query(uri, Channel.PROJECTION, null, null, null);
            if (cursor == null || cursor.getCount() == 0) {
                if (DEBUG) {
                    Log.d(TAG, "Cursor is null or found no results");
                }
                return null;
            }

            while (cursor.moveToNext()) {
                Channel nextChannel = Channel.fromCursor(cursor);
                channelMap.put(nextChannel.getId(), nextChannel);
            }
        } catch (Exception e) {
            Log.d(TAG, "Content provider query: " + Arrays.toString(e.getStackTrace()));
            return null;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return channelMap;
    }

    /**
     * Returns the current list of channels your app provides.
     *
     * @param resolver Application's ContentResolver.
     * @return List of channels.
     */
    public static List<Channel> getChannels(ContentResolver resolver) {
        List<Channel> channels = new ArrayList<>();
        // TvProvider returns programs in chronological order by default.
        Cursor cursor = null;
        try {
            cursor = resolver.query(Channels.CONTENT_URI, Channel.PROJECTION, null, null, null);
            if (cursor == null || cursor.getCount() == 0) {
                return channels;
            }
            while (cursor.moveToNext()) {
                channels.add(Channel.fromCursor(cursor));
            }
        } catch (Exception e) {
            Log.w(TAG, "Unable to get channels", e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return channels;
    }

    /**
     * Returns the {@link Channel} with specified channel URI.
     * @param resolver {@link ContentResolver} used to query database.
     * @param channelUri URI of channel.
     * @return An channel object with specified channel URI.
     * @hide
     */
    public static Channel getChannel(ContentResolver resolver, Uri channelUri) {
        Cursor cursor = null;
        try {
            cursor = resolver.query(channelUri, Channel.PROJECTION, null, null, null);
            if (cursor == null || cursor.getCount() == 0) {
                Log.w(TAG, "No channel matches " + channelUri);
                return null;
            }
            cursor.moveToNext();
            return Channel.fromCursor(cursor);
        } catch (Exception e) {
            Log.w(TAG, "Unable to get the channel with URI " + channelUri, e);
            return null;
        } finally {
            if (cursor != null) {
                cursor.close();

            }
        }
    }

    /**
     * Returns the {@link Channel} with specified displayName.
     * @param resolver {@link ContentResolver} used to query database.
     * @param displayName of channel.
     * @return An channel object with specified displayName.
     */
    public static Channel getChannelByDisplayName(ContentResolver resolver, String displayName, int frequency) {
        Cursor cursor = null;
        String selection = TvContract.Channels.COLUMN_DISPLAY_NAME + "=?";
        String[] selectionArgs = new String[]{displayName};
        try {
            cursor = resolver.query(TvContract.Channels.CONTENT_URI, Channel.PROJECTION, selection, selectionArgs, null);
            if (cursor == null || cursor.getCount() == 0) {
                Log.w(TAG, "getChannelByDisplayName No channel matches " + displayName);
                return null;
            }
            Channel channel = null;
            int foundFrequency = 0;
            while (cursor != null && cursor.moveToNext()) {
                channel = Channel.fromCursor(cursor);
                foundFrequency = Integer.valueOf(channel.getInternalProviderData().get("frequency").toString());
                if (frequency <= 0) {
                    break;
                } else if (frequency == foundFrequency) {
                    break;
                }
            }
            return channel;
        } catch (Exception e) {
            Log.w(TAG, "displayName Unable to get the channel " + displayName, e);
            return null;
        } finally {
            if (cursor != null) {
                cursor.close();

            }
        }
    }

    /**
     * Returns the {@link Channel} with specified id.
     * @param resolver {@link ContentResolver} used to query database.
     * @param originalNetworkId of channel.
     * @param transportStreamId of channel.
     * @param serviceId of channel.
     * @return An channel object with specified id.
     */
    public static Channel getChannelByNetworkTransportServiceId(ContentResolver resolver, int originalNetworkId, int transportStreamId, int serviceId) {
        Channel result = null;
        Cursor cursor = null;
        try {
            cursor = resolver.query(Channels.CONTENT_URI, Channel.PROJECTION, null, null, null);
            if (cursor == null || cursor.getCount() == 0) {
                Log.w(TAG, "getChannelByNetworkTransportServiceId No channels");
                return result;
            }
            Channel channel = null;
            int neworkId = 0;
            int streamId = 0;
            int service = 0;
            while (cursor.moveToNext()) {
                channel = Channel.fromCursor(cursor);
                if (channel != null) {
                    neworkId = channel.getOriginalNetworkId();
                    streamId = channel.getTransportStreamId();
                    service = channel.getServiceId();
                    if (neworkId == originalNetworkId && streamId == transportStreamId && serviceId == service) {
                        result = channel;
                        break;
                    }
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "getChannelByNetworkTransportServiceId Exception " + e.getMessage());
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return result;
    }

    /**
     * Returns the current list of programs on a given channel.
     *
     * @param resolver Application's ContentResolver.
     * @param channelUri Channel's Uri.
     * @return List of programs.
     * @hide
     */
    public static List<Program> getPrograms(ContentResolver resolver, Uri channelUri) {
        if (channelUri == null) {
            return null;
        }
        Uri uri = TvContract.buildProgramsUriForChannel(channelUri);
        List<Program> programs = new ArrayList<>();
        // TvProvider returns programs in chronological order by default.
        Cursor cursor = null;
        try {
            cursor = resolver.query(uri, Program.PROJECTION, null, null, null);
            if (cursor == null || cursor.getCount() == 0) {
                return programs;
            }
            while (cursor.moveToNext()) {
                programs.add(Program.fromCursor(cursor));
            }
        } catch (Exception e) {
            Log.w(TAG, "Unable to get programs for " + channelUri, e);
        } finally {
            if (cursor != null) {
                cursor.close();

            }
        }
        return programs;
    }

    /**
     * Returns the program that is scheduled to be playing now on a given channel.
     *
     * @param resolver Application's ContentResolver.
     * @param channelUri Channel's Uri.
     * @return The program that is scheduled for now in the EPG.
     */
    public static Program getCurrentProgram(ContentResolver resolver, Uri channelUri) {
        List<Program> programs = getPrograms(resolver, channelUri);
        if (programs == null) {
            return null;
        }
        long nowMs = System.currentTimeMillis();
        for (Program program : programs) {
            if (program.getStartTimeUtcMillis() <= nowMs && program.getEndTimeUtcMillis() > nowMs) {
                return program;
            }
        }
        return null;
    }

    public static Program getCurrentProgramExt(ContentResolver resolver, Uri channelUri, long nowMs) {
        List<Program> programs = getPrograms(resolver, channelUri);
        if (programs == null) {
            return null;
        }
        //long nowMs = System.currentTimeMillis();
        for (Program program : programs) {
            if (program.getStartTimeUtcMillis() <= nowMs && program.getEndTimeUtcMillis() > nowMs) {
                return program;
            }
        }
        return null;
    }


    /**
     * Returns the program that is scheduled to be playing after a given program on a given channel.
     *
     * @param resolver Application's ContentResolver.
     * @param channelUri Channel's Uri.
     * @param currentProgram Program which plays before the desired program.If null, returns current
     *                       program
     * @return The program that is scheduled after given program in the EPG.
     */
    public static Program getNextProgram(ContentResolver resolver, Uri channelUri,
                                         Program currentProgram) {
        if (currentProgram == null) {
            return getCurrentProgram(resolver, channelUri);
        }
        List<Program> programs = getPrograms(resolver, channelUri);
        if (programs == null) {
            return null;
        }
        int currentProgramIndex = programs.indexOf(currentProgram);
        if (currentProgramIndex + 1 < programs.size()) {
            return programs.get(currentProgramIndex + 1);
        }
        return null;
    }

    private static void insertUrl(Context context, Uri contentUri, URL sourceUrl) {
        if (DEBUG) {
            Log.d(TAG, "Inserting " + sourceUrl + " to " + contentUri);
        }
        InputStream is = null;
        OutputStream os = null;
        try {
            is = sourceUrl.openStream();
            os = context.getContentResolver().openOutputStream(contentUri);
            copy(is, os);
        } catch (IOException ioe) {
            Log.e(TAG, "Failed to write " + sourceUrl + "  to " + contentUri, ioe);
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    // Ignore exception.
                }
            }
            if (os != null) {
                try {
                    os.close();
                } catch (IOException e) {
                    // Ignore exception.
                }
            }
        }
    }

    private static void copy(InputStream is, OutputStream os) throws IOException {
        byte[] buffer = new byte[1024];
        int len;
        while ((len = is.read(buffer)) != -1) {
            os.write(buffer, 0, len);
        }
    }

    /**
     * Parses a string of comma-separated ratings into an array of {@link TvContentRating}.
     *
     * @param commaSeparatedRatings String containing various ratings, separated by commas.
     * @return An array of TvContentRatings.
     * @hide
     */
    public static TvContentRating[] stringToContentRatings(String commaSeparatedRatings) {
        if (TextUtils.isEmpty(commaSeparatedRatings)) {
            return null;
        }
        String[] ratings = commaSeparatedRatings.split("\\s*,\\s*");
        TvContentRating[] contentRatings = new TvContentRating[ratings.length];
        for (int i = 0; i < contentRatings.length; ++i) {
            contentRatings[i] = TvContentRating.unflattenFromString(ratings[i]);
        }
        return contentRatings;
    }

    /**
     * Flattens an array of {@link TvContentRating} into a String to be inserted into a database.
     *
     * @param contentRatings An array of TvContentRatings.
     * @return A comma-separated String of ratings.
     * @hide
     */
    public static String contentRatingsToString(TvContentRating[] contentRatings) {
        if (contentRatings == null || contentRatings.length == 0) {
            return null;
        }
        final String DELIMITER = ",";
        StringBuilder ratings = new StringBuilder(contentRatings[0].flattenToString());
        for (int i = 1; i < contentRatings.length; ++i) {
            ratings.append(DELIMITER);
            ratings.append(contentRatings[i].flattenToString());
        }
        return ratings.toString();
    }

    private TvContractUtils() {
    }

    private static class InsertLogosTask extends AsyncTask<Map<Uri, String>, Void, Void> {
        private final Context mContext;

        InsertLogosTask(Context context) {
            mContext = context;
        }

        @Override
        public Void doInBackground(Map<Uri, String>... logosList) {
            for (Map<Uri, String> logos : logosList) {
                for (Uri uri : logos.keySet()) {
                    try {
                        insertUrl(mContext, uri, new URL(logos.get(uri)));
                    } catch (MalformedURLException e) {
                        Log.e(TAG, "Can't load " + logos.get(uri), e);
                    }
                }
            }
            return null;
        }
    }

    public static class CompareDisplayNumber implements Comparator<Channel> {

        @Override
        public int compare(Channel o1, Channel o2) {
            int result = compareString(o1.getDisplayNumber(), o2.getDisplayNumber());
            return result;
        }

        private int compareString(String a, String b) {
            if (a == null) {
                return b == null ? 0 : -1;
            }
            if (b == null) {
                return 1;
            }

            int[] disnumbera = getMajorAndMinor(a);
            int[] disnumberb = getMajorAndMinor(b);
            if (disnumbera[0] != disnumberb[0]) {
                return (disnumbera[0] - disnumberb[0]) > 0 ? 1 : -1;
            } else if (disnumbera[1] != disnumberb[1]) {
                return (disnumbera[1] - disnumberb[1]) > 0 ? 1 : -1;
            }
            return 0;
        }

        private int[] getMajorAndMinor(String disnumber) {
            int[] result = {-1, -1};//major, minor
            String[] splitone = (disnumber != null ? disnumber.split("-") : null);
            if (splitone != null && splitone.length > 0) {
                int length = 2;
                if (splitone.length <= 2) {
                    length = splitone.length;
                } else {
                    Log.d(TAG, "informal disnumber");
                    return result;
                }
                for (int i = 0; i < length; i++) {
                    try {
                       result[i] = Integer.valueOf(splitone[i]);
                    } catch (NumberFormatException e) {
                        Log.d(TAG, splitone[i] + " not integer:" + e.getMessage());
                    }
                }
            }
            return result;
        }
    }

    public static String getStringFromChannelInternalProviderData(Channel channel, String key, String defaultVal) {
        String result = defaultVal;
        try {
            if (channel != null) {
                result = (String)channel.getInternalProviderData().get(key);
            }
        } catch(Exception e) {
            Log.i(TAG, "getStringFromChannelInternalProviderData Exception " + e.getMessage());
        }
        return result;
    }

    public static boolean getBooleanFromChannelInternalProviderData(Channel channel, String key, boolean defaultVal) {
        boolean result = defaultVal;
        try {
            if (channel != null) {
                result = Boolean.valueOf((String)channel.getInternalProviderData().get(key));
            }
        } catch(Exception e) {
            Log.i(TAG, "getBooleanFromChannelInternalProviderData Exception " + e.getMessage());
        }
        return result;
    }

    public static int getIntFromChannelInternalProviderData(Channel channel, String key, int defaultVal) {
        int result = defaultVal;
        try {
            if (channel != null) {
                result = Integer.valueOf((String)channel.getInternalProviderData().get(key));
            }
        } catch(Exception e) {
            Log.i(TAG, "getIntFromChannelInternalProviderData Exception " + e.getMessage());
        }
        return result;
    }

    private static String searchSignalTypeToChannelType(String searchSignalType) {
        String result = null;

        if (TextUtils.isEmpty(searchSignalType)) {
            return null;
        }
        switch (searchSignalType) {
            case "DVB-T":
                result = "TYPE_DVB_T";
                break;
            case "DVB-C":
                result = "TYPE_DVB_C";
                break;
            case "DVB-S":
                result = "TYPE_DVB_S";
                break;
            case "ISDB-T":
                result = "TYPE_ISDB_T";
                break;
        }
        return result;
    }
}
