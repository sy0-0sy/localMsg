package com.sy.localMsg.util;

public class LocalMessageUtil {

    public static Long offsetTimestamp(long minutes) {
        return System.currentTimeMillis() + minutes * 60 * 1000;
    }
}