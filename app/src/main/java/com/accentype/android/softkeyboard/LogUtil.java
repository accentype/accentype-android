package com.accentype.android.softkeyboard;

import android.util.Log;

public class LogUtil {
    public static void Log(String className, String message) {
        Log.d("accentypelog_" + className, message);
    }
}
