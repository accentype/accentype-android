package com.accentype.android.softkeyboard;

import android.os.AsyncTask;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collection;

/**
 * EN Dictionary as a simple trie for auto completion.
 */
public class DictionaryEN {
    private AutoCompleteTrie mDictionary = new AutoCompleteTrie();

    private static DictionaryEN instance = null;

    protected DictionaryEN(InputStream dictFileStream) {
        new FromFileLoader().execute(dictFileStream);
    }

    public static DictionaryEN getInstance(InputStream dictFileStream) {
        if(instance == null) {
            instance = new DictionaryEN(dictFileStream);
        }
        return instance;
    }

    /**
     * Gets the list of words that begin with the specified prefix.
     * @param prefix The prefix.
     * @return A list of words that begin with the specified prefix.
     */
    public Collection<String> complete(String prefix) {
        return mDictionary.autoComplete(prefix);
    }

    private class FromFileLoader extends AsyncTask<InputStream, Void, AutoCompleteTrie> {
        protected AutoCompleteTrie doInBackground(InputStream... dictFileStreams) {
            try
            {
                InputStream accStream = dictFileStreams[0];
                BufferedReader accReader = new BufferedReader(new InputStreamReader(accStream, "UTF-8"));
                AutoCompleteTrie dictionary = new AutoCompleteTrie();
                String line;
                while ((line = accReader.readLine()) != null) {
                    dictionary.insert(line.trim());
                }
                return dictionary;
            }
            catch (Exception ex) {
                LogUtil.LogError(this.getClass().getName(), "Cannot load auto-complete trie for English", ex);
            }

            return new AutoCompleteTrie();
        }

        protected void onPostExecute(AutoCompleteTrie dictionary) {
            mDictionary = dictionary;
        }
    }
}
