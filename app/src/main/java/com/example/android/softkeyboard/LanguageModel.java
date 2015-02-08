package com.example.android.softkeyboard;

import android.content.Context;

import java.io.IOException;
import java.io.InputStream;

/**
 * Created by lhoang on 2/8/2015.
 */
public class LanguageModel {

    private Context mContext;
    private InputStream mStream;

    private int mModelVersion;
    private int mNumBitPerAccStringIndex;
    private int mNumBitPerAccCountIndex;
    private int mNumBitPerHashEntryCount;
    private int mNumBitPerAccStringLength;

    public LanguageModel(Context context, String fileName)
    {
        mContext = context;
        Initialize(fileName);
    }

    private void Initialize(String fileName)
    {
        try
        {
            mStream = mContext.getAssets().open(fileName);
            byte[] bytes = new byte[4];

            mStream.read(bytes, 0, bytes.length);
            mModelVersion = byteArrayToInt(bytes);

            mStream.read(bytes, 0, bytes.length);
            mNumBitPerAccStringIndex = byteArrayToInt(bytes);

            mStream.read(bytes, 0, bytes.length);
            mNumBitPerAccCountIndex = byteArrayToInt(bytes);

            mStream.read(bytes, 0, bytes.length);
            mNumBitPerHashEntryCount = byteArrayToInt(bytes);

            mStream.read(bytes, 0, bytes.length);
            mNumBitPerAccStringLength = byteArrayToInt(bytes);
        }
        catch (IOException e) {
            // TODO: Handle exception
        }
    }

    private int byteArrayToInt(byte[] b)
    {
        return b[0] & 0xFF | (b[1] & 0xFF) << 8 |
                (b[2] & 0xFF) << 16 | (b[3] & 0xFF) << 24;
    }
}
