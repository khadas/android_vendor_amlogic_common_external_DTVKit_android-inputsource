// ILanguageSetting.aidl
package com.droidlogic.dtvkit;

// Declare any non-default types here with import statements

interface IDtvkitSetting {
    /**
     * Demonstrates some basic types that you can use as parameters
     * and return values in AIDL.
     */
    int getCurrentMainAudioLanguageId();

    int getCurrentMainSubtitleLangId();

    int getCurrentSecondAudioLanguageId();

    int getSecondSubtitleLangId();

    List<String> getCurrentLangNameList();

    List<String> getCurrentSecondLangNameList();

    void setPrimaryAudioLangByPosition(int position);

    void setPrimarySubtitleLangByPosition(int position);

    void setSecondaryAudioLangByPosition(int position);

    void setSecondarySubtitleLangByPosition(int position);

    String getStoragePath();

    void setStoragePath(String path);

    boolean getHearingImpairedSwitchStatus();

    void setHearingImpairedSwitchStatus(boolean on);

    void setCustomParameter(String key, String newJsonValue);

    String getCustomParameter(String key, String defaultJsonValue);
}