package com.p2aau2021.virtualworkout.utils;

import io.agora.rtc.RtcEngine;

public class Constant {

    public static final String MEDIA_SDK_VERSION;
    static {
        String sdk = "undefined";
        try {
            sdk = RtcEngine.getSdkVersion();
        } catch (Throwable e) {
        }
        MEDIA_SDK_VERSION = sdk;
    }

    public static boolean SHOW_VIDEO_INFO = true;

    public static final String USER_STATE_OPEN = "Open";
    public static final String USER_STATE_LOCK = "Lock";
}
