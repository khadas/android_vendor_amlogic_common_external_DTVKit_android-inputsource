package org.dtvkit.inputsource.service;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteException;
import android.provider.Settings;
import android.util.Log;
import android.content.ComponentName;

import org.dtvkit.IDtvkitSetting;
import org.dtvkit.companionlibrary.utils.TvContractUtils;
import com.droidlogic.fragment.ParameterMananer;
import org.droidlogic.dtvkit.DtvkitGlueClient;
import org.dtvkit.companionlibrary.EpgSyncJobService;
import org.dtvkit.inputsource.DtvkitEpgSync;
import com.droidlogic.settings.SysSettingManager;

import java.io.File;
import java.util.List;

public class DtvkitSettingService extends Service {
    private static final String TAG = "DtvkitSettingService";
    protected ParameterMananer mParameterManager;

    @Override
    public void onCreate() {
        super.onCreate();
        mParameterManager = new ParameterMananer(this, DtvkitGlueClient.getInstance());
    }

    @Override
    public IBinder onBind(Intent intent) {
        return new DtvkitSettingBinder();
    }

    private class DtvkitSettingBinder extends IDtvkitSetting.Stub {

        @Override
        public int getCurrentMainAudioLanguageId() throws RemoteException {
            return mParameterManager.getCurrentMainAudioLangId();
        }

        @Override
        public int getCurrentMainSubtitleLangId() throws RemoteException {
            return mParameterManager.getCurrentMainSubLangId();
        }

        @Override
        public int getCurrentSecondAudioLanguageId() throws RemoteException {
            return mParameterManager.getCurrentSecondAudioLangId();
        }

        @Override
        public int getSecondSubtitleLangId() throws RemoteException {
            return mParameterManager.getCurrentSecondSubLangId();
        }

        @Override
        public List<String> getCurrentLangNameList() throws RemoteException {
            return mParameterManager.getCurrentLangNameList();
        }

        @Override
        public List<String> getCurrentSecondLangNameList() throws RemoteException {
            return mParameterManager.getCurrentSecondLangNameList();
        }

        @Override
        public void setPrimaryAudioLangByPosition(int position) throws RemoteException {
            mParameterManager.clearUserAudioSelect();
            mParameterManager.setPrimaryAudioLangByPosition(position);
            updatingGuide();
        }

        @Override
        public void setPrimarySubtitleLangByPosition(int position) throws RemoteException {
            mParameterManager.setPrimaryTextLangByPosition(position);
            updatingGuide();
        }

        @Override
        public void setSecondaryAudioLangByPosition(int position) throws RemoteException {
            mParameterManager.clearUserAudioSelect();
            mParameterManager.setSecondaryAudioLangByPosition(position);
            updatingGuide();
        }

        @Override
        public void setSecondarySubtitleLangByPosition(int position) throws RemoteException {
            mParameterManager.setSecondaryTextLangByPosition(position);
            updatingGuide();
        }

        @Override
        public String getStoragePath() throws RemoteException {
            String savedPath = mParameterManager.getStringParameters(ParameterMananer.KEY_PVR_RECORD_PATH);
            if (SysSettingManager.isMediaPath(savedPath)) {
                savedPath = SysSettingManager.convertMediaPathToMountedPath(savedPath);
            }
            if (savedPath != null && savedPath.indexOf(SysSettingManager.PVR_DEFAULT_FOLDER) != -1) {
                savedPath = savedPath.substring(0, savedPath.indexOf(SysSettingManager.PVR_DEFAULT_FOLDER) - 1);
            }
            Log.d(TAG, "getStoragePath = " + savedPath);
            return savedPath;
        }

        @Override
        public void setStoragePath(String path) throws RemoteException {
            Log.d(TAG, "setStoragePath = " + path);
            if (SysSettingManager.isStoragePath(path)) {
                path = SysSettingManager.convertStoragePathToMediaPath(path);
            }
            if (path != null && !path.endsWith(SysSettingManager.PVR_DEFAULT_FOLDER)) {
                path += "/" + SysSettingManager.PVR_DEFAULT_FOLDER;
            }
            if (path != null) {
                mParameterManager.saveStringParameters(ParameterMananer.KEY_PVR_RECORD_PATH, path);
            } else {
                Log.d(TAG, "setStoragePath invalid /storage path");
            }
        }

        @Override
        public boolean getHearingImpairedSwitchStatus() throws RemoteException {
            return mParameterManager.getHearingImpairedSwitchStatus();
        }

        @Override
        public void setHearingImpairedSwitchStatus(boolean on) throws RemoteException {
            mParameterManager.setHearingImpairedSwitchStatus(on);
        }

        @Override
        public String getCustomParameter(String key, String newJsonValue) throws RemoteException {
            return mParameterManager.getCustomParameter(key, newJsonValue);
        }

        @Override
        public void setCustomParameter(String key, String defaultJsonValue) throws RemoteException {
            mParameterManager.setCustomParameter(key, defaultJsonValue);
        }
    }

    private void updatingGuide() {
        EpgSyncJobService.cancelAllSyncRequests(this);
        String inputId = "org.dtvkit.inputsource/.DtvkitTvInput/HW19";
        EpgSyncJobService.requestImmediateSync(this, inputId, true, new ComponentName(this, DtvkitEpgSync.class));
    }

    private boolean checkAndCreatPvrFolderInStorage(String storagePath) {
        boolean folderReady = false;
        try {
            File file = new File(storagePath);
            if (!file.exists()) {
                folderReady = file.mkdirs();
                Log.d(TAG, "checkAndCreatPvrFolderInStorage folderReady = " + folderReady + ", storagePath = " + storagePath);
            } else {
                folderReady = true;
            }
        } catch (Exception e) {
            folderReady = false;
            Log.d(TAG, "checkAndCreatPvrFolderInStorage Exception = " + e.getMessage());
        }
        return folderReady;
    }
}