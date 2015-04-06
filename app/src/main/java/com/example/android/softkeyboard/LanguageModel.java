package com.example.android.softkeyboard;

import android.content.Context;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

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

    private ArrayList<String> mAccentCodeStrings;
    private ArrayList<Integer> mAccentCodeCounts;

    public LanguageModel(Context context, InputStream stream)
    {
        mStream = stream;

        this.initialize();
    }

    private void initialize()
    {
        try {
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

            LanguageConstruct languageConstruct = LanguageConstruct.getInstance();

            // Load accent code string lookups
            mAccentCodeStrings = new ArrayList<>();
            mStream.read(bytes, 0, bytes.length);
            int numAsciiCodeStrings = byteArrayToInt(bytes);
            for (int i = 0; i < numAsciiCodeStrings; i++) {
                mStream.read(bytes, 0, 1);

                byte[] asciiCodeBytes = new byte[bytes[0]];
                mStream.read(asciiCodeBytes, 0, asciiCodeBytes.length);

                String asciiCodeString = new String(asciiCodeBytes, "US-ASCII");

                String accentString = "";
                for (int j = 0; j < asciiCodeString.length(); j++)
                {
                    accentString += languageConstruct.AsciiToAccentMap.get((int)asciiCodeString.charAt(j));
                }

                mAccentCodeStrings.add(accentString);
            }

            // Load accent code count lookups
            mAccentCodeCounts = new ArrayList<>();
            mStream.read(bytes, 0, bytes.length);
            int numAsciiCodeCounts = byteArrayToInt(bytes);
            for (int i = 0; i < numAsciiCodeCounts; i++) {
                mStream.read(bytes, 0, bytes.length);
                mAccentCodeCounts.add(byteArrayToInt(bytes));
            }
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
