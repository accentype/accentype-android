package com.accentype.android.softkeyboard;

import android.content.Context;

/**
 * Created by lhoang on 7/17/2015.
 */
public class ModelFactory {
    public static final BaseModel create(int version, String fileName, String fileDir) {
        switch (version) {
            case ModelVersion.LOOKUP:
                return LookupModel.getInstance(fileName, fileDir);
            case ModelVersion.LINEAR_BACKOFF_INTERPOLATION:
                return LinearBackoffInterpolationModel.getInstance(fileName, fileDir);
            default:
                return null;
        }
    }
}
