package com.example.android.softkeyboard;

import android.content.Context;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

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

    private static final Map<Character, Character> mAccentToRawMap;
    static
    {
        mAccentToRawMap = new HashMap<>();
        mAccentToRawMap.put('a', 'a');
        mAccentToRawMap.put('á', 'a');
        mAccentToRawMap.put('à', 'a');
        mAccentToRawMap.put('ạ', 'a');
        mAccentToRawMap.put('ả', 'a');
        mAccentToRawMap.put('ã', 'a');

        mAccentToRawMap.put('ă', 'a');
        mAccentToRawMap.put('ắ', 'a');
        mAccentToRawMap.put('ằ', 'a');
        mAccentToRawMap.put('ặ', 'a');
        mAccentToRawMap.put('ẳ', 'a');
        mAccentToRawMap.put('ẵ', 'a');

        mAccentToRawMap.put('â', 'a');
        mAccentToRawMap.put('ấ', 'a');
        mAccentToRawMap.put('ầ', 'a');
        mAccentToRawMap.put('ậ', 'a');
        mAccentToRawMap.put('ẩ', 'a');
        mAccentToRawMap.put('ẫ', 'a');

        mAccentToRawMap.put('e', 'e');
        mAccentToRawMap.put('é', 'e');
        mAccentToRawMap.put('è', 'e');
        mAccentToRawMap.put('ẹ', 'e');
        mAccentToRawMap.put('ẻ', 'e');
        mAccentToRawMap.put('ẽ', 'e');

        mAccentToRawMap.put('ê', 'e');
        mAccentToRawMap.put('ế', 'e');
        mAccentToRawMap.put('ề', 'e');
        mAccentToRawMap.put('ệ', 'e');
        mAccentToRawMap.put('ể', 'e');
        mAccentToRawMap.put('ễ', 'e');

        mAccentToRawMap.put('i', 'i');
        mAccentToRawMap.put('í', 'i');
        mAccentToRawMap.put('ì', 'i');
        mAccentToRawMap.put('ị', 'i');
        mAccentToRawMap.put('ỉ', 'i');
        mAccentToRawMap.put('ĩ', 'i');

        mAccentToRawMap.put('o', 'o');
        mAccentToRawMap.put('ó', 'o');
        mAccentToRawMap.put('ò', 'o');
        mAccentToRawMap.put('ọ', 'o');
        mAccentToRawMap.put('ỏ', 'o');
        mAccentToRawMap.put('õ', 'o');

        mAccentToRawMap.put('ô', 'o');
        mAccentToRawMap.put('ố', 'o');
        mAccentToRawMap.put('ồ', 'o');
        mAccentToRawMap.put('ộ', 'o');
        mAccentToRawMap.put('ổ', 'o');
        mAccentToRawMap.put('ỗ', 'o');

        mAccentToRawMap.put('ơ', 'o');
        mAccentToRawMap.put('ớ', 'o');
        mAccentToRawMap.put('ờ', 'o');
        mAccentToRawMap.put('ợ', 'o');
        mAccentToRawMap.put('ở', 'o');
        mAccentToRawMap.put('ỡ', 'o');

        mAccentToRawMap.put('u', 'u');
        mAccentToRawMap.put('ú', 'u');
        mAccentToRawMap.put('ù', 'u');
        mAccentToRawMap.put('ụ', 'u');
        mAccentToRawMap.put('ủ', 'u');
        mAccentToRawMap.put('ũ', 'u');

        mAccentToRawMap.put('ư', 'u');
        mAccentToRawMap.put('ứ', 'u');
        mAccentToRawMap.put('ừ', 'u');
        mAccentToRawMap.put('ự', 'u');
        mAccentToRawMap.put('ử', 'u');
        mAccentToRawMap.put('ữ', 'u');

        mAccentToRawMap.put('y', 'y');
        mAccentToRawMap.put('ý', 'y');
        mAccentToRawMap.put('ỳ', 'y');
        mAccentToRawMap.put('ỵ', 'y');
        mAccentToRawMap.put('ỷ', 'y');
        mAccentToRawMap.put('ỹ', 'y');

        mAccentToRawMap.put('d', 'd');
        mAccentToRawMap.put('đ', 'd');
    }

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

    private String ConvertToRaw(String word)
    {
        StringBuilder rawBuilder = new StringBuilder();
        for (int i = 0; i < word.length(); i++) {
            Character c = word.charAt(i);
            if (mAccentToRawMap.containsKey(c)) {
                rawBuilder.append(mAccentToRawMap.get(c));
            }
            else {
                rawBuilder.append(c);
            }
        }
        return rawBuilder.toString();
    }
}
