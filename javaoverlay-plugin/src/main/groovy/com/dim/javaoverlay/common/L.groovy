package com.dim.javaoverlay.common;

/**
 * Created by dim on 17/1/11.
 */

public class L {
    public static final String TAG = "[ JavaOverlay ]";
    public static boolean verbose = false;

    public static void i(String content) {
        if (verbose)
            println(TAG + content)
    }

    public static void d(String content) {
        if (verbose)
            println(TAG + content)
    }

    public static void v(String content) {
            println(content)
    }
}
