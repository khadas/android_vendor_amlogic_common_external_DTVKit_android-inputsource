// IDTVKit.aidl
package com.droidlogic.dtvkit;

import com.droidlogic.dtvkit.ISignalHandler;
import com.droidlogic.dtvkit.IOverlayTarget;

interface IDTVKit {
    String request(String method, String json);
    void registerSignalHandler(ISignalHandler handler);
    void registerOverlayTarget(IOverlayTarget target);
}
