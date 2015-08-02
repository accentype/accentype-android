package com.accentype.android.softkeyboard;

import android.util.Log;

import java.io.PrintWriter;
import java.io.StringWriter;

public class LogUtil {
    public static void LogError(String className, String customMessage, Exception ex) {
        Log.d("accentypelog_" + className, customMessage + ": " + getStackTrace(ex));
    }

    public static void LogMessage(String className, String customMessage) {
        Log.d("accentypelog_" + className, customMessage);
    }

    private static String getStackTrace(final Throwable throwable) {
        final StringWriter sw = new StringWriter();
        final PrintWriter pw = new PrintWriter(sw, true);
        throwable.printStackTrace(pw);
        return sw.getBuffer().toString();
    }
}
