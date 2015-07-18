package com.accentype.android.softkeyboard;

import android.content.Context;

/**
 * Created by lhoang on 7/17/2015.
 */
public class ModelFactory {
    public static final BaseModel create(int version, Context context) {
        switch (version) {
            case ModelVersion.LOOKUP:
                return LookupModel.getInstance(context);
            case ModelVersion.LINEAR_BACKOFF_INTERPOLATION:
                return LinearBackoffInterpolationModel.getInstance(context);
            default:
                return null;
        }
    }
}
