package com.accentype.android.softkeyboard;

import android.os.AsyncTask;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * VN Dictionary as a map from unaccented to a list of accented words.
 */
public class DictionaryVN {
    private static DictionaryVN instance = null;
    private HashMap<String, String[]> mDictionary = null;

    protected DictionaryVN(InputStream dictFileStream) {
        new FromFileLoader().execute(dictFileStream);
    }

    public static DictionaryVN getInstance(InputStream dictFileStream) {
        if(instance == null) {
            instance = new DictionaryVN(dictFileStream);
        }
        return instance;
    }

    /**
     * Gets the list of accented words that correspond to the first word of the specified query.
     * @param query The query phrase.
     * @return A list of accented words corresponding to the first word in the query phrase.
     */
    public String[] get(String query) {
        return get(query, 0);
    }

    /**
     * Gets the list of accented words that correspond to the specified word of the specified query.
     * @param query The query phrase.
     * @param iWord The index of the word in the query to consider.
     * @return A list of accented words corresponding to the specified word in the query phrase.
     */
    public String[] get(String query, int iWord) {
        if (mDictionary != null && query != null) {
            String[] rawWords = query.trim().split("\\s+");
            if (iWord < rawWords.length) {
                String rawWord = rawWords[iWord];
                String rawWordLower = rawWord.toLowerCase();
                if (mDictionary.containsKey(rawWordLower)) {
                    List<Integer> upperCaseLocations = new ArrayList<>();
                    for (int i = 0; i < rawWord.length(); i++) {
                        if (Character.isUpperCase(rawWord.charAt(i))) {
                            upperCaseLocations.add(i);
                        }
                    }
                    // Normalize case w.r.t raw word
                    String[] dictionaryChoices = mDictionary.get(rawWordLower);
                    String[] additionalChoices = new String[dictionaryChoices.length];
                    for (int i = 0; i < dictionaryChoices.length; i++) {
                        char[] choiceChars = dictionaryChoices[i].toCharArray();
                        for (int j = 0; j < upperCaseLocations.size(); j++) {
                            choiceChars[j] = Character.toUpperCase(choiceChars[j]);
                        }
                        additionalChoices[i] = new String(choiceChars);
                    }
                    return additionalChoices;
                }
            }
        }
        return null;
    }

    private class FromFileLoader extends AsyncTask<InputStream, Void, HashMap<String, String[]>> {
        protected HashMap<String, String[]> doInBackground(InputStream... dictFileStreams) {
            try {
                InputStream accStream = dictFileStreams[0];
                BufferedReader accReader = new BufferedReader(new InputStreamReader(accStream, "UTF-8"));
                HashMap<String, String[]> dictionary = new HashMap<>();
                String line;
                while ((line = accReader.readLine()) != null) {
                    String[] words = line.split("\\s+");
                    dictionary.put(words[0], words);
                }
                return dictionary;
            }
            catch (Exception ex) {
                LogUtil.LogError(this.getClass().getName(), "Error in async vn dict load", ex);
            }
            return null;
        }

        protected void onPostExecute(HashMap<String, String[]> dictionary) {
            mDictionary = dictionary;
        }
    }
}
