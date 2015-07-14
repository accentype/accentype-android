package com.accentype.android.softkeyboard;

import android.content.Context;

/**
 * Linear model with backoff interpolation.
 */
public class LinearBackoffInterpolationModel implements BaseModel {
    private static LinearBackoffInterpolationModel instance = null;

    protected LinearBackoffInterpolationModel(Context context) {
    }

    public static LinearBackoffInterpolationModel getInstance(Context context) {
        if(instance == null) {
            instance = new LinearBackoffInterpolationModel(context);
        }
        return instance;
    }

    @Override public String predict(String rawPhrase) {
        return null;
    }

    @Override public void learn(String rawPhrase, String accentPhrase) {

    }

    @Override public int version() {
        return ModelVersion.LINEAR_BACKOFF_INTERPOLATION;
    }
}
