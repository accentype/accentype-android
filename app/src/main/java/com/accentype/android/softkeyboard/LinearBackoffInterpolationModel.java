package com.accentype.android.softkeyboard;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Debug;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;

/**
 * Linear model with backoff interpolation.
 */
public class LinearBackoffInterpolationModel implements BaseModel {

    private PhraseLookup model1 = null;
    private PhraseLookup model2 = null;
    private PhraseLookup model3 = null;

    private static LinearBackoffInterpolationModel instance = null;

    private float beta3 = 0.2f;
    private float beta2 = 0.15f;
    private float beta1 = 0.1f;

    // I/O manager
    private DataOutputStream mLocalModelBinaryWriter;
    private FileOutputStream mLocalModelOutputStream;

    private PhraseHistory mPhraseHistory;

    protected LinearBackoffInterpolationModel(Context context) {

        new LoadFromFile(context).execute();

        model1 = new PhraseLookup();
        model2 = new PhraseLookup();
        model3 = new PhraseLookup();
        mPhraseHistory = new PhraseHistory();
    }

    public static LinearBackoffInterpolationModel getInstance(Context context) {
        if(instance == null) {
            instance = new LinearBackoffInterpolationModel(context);
        }
        return instance;
    }

    @Override public String predict(String rawPhrase) {
        if (rawPhrase == null) {
            return null;
        }
        String[] words = rawPhrase.trim().toLowerCase().split("\\s+");

        StringBuilder sbPredictions = new StringBuilder();

        // for single-word query, take most likely
        if (words.length == 1) {
            HashMap<String, Short> accentsCountMap = model1.get(words[0]);
            if (accentsCountMap == null) {
                return null;
            }
            Short max = -1;
            String bestGuess = null;
            for (String v : accentsCountMap.keySet()) {
                if (max <= accentsCountMap.get(v)) {
                    max = accentsCountMap.get(v);
                    bestGuess = v;
                }
            }
            if (bestGuess == null) {
                return null;
            }
            sbPredictions.append(bestGuess);
            return StringUtil.normalizeStringCaseDottedTruncate(rawPhrase, sbPredictions);
        }
        else {
            boolean hasPredictions = false;
            for (int i = 0; i < words.length; i++) {
                HashMap<String, Double> accScoreMap = new HashMap<>();

                // only look at 2-gram & 3-gram
                ComputeAccentScore(words, i, beta3, model3, 3, accScoreMap);
                ComputeAccentScore(words, i, beta2, model2, 2, accScoreMap);

                String bestPrediction = null;
                if (accScoreMap.size() > 0) {
                    double bestScore = -1;
                    for (String accentPhrase : accScoreMap.keySet()) {
                        double score = accScoreMap.get(accentPhrase);
                        if (bestScore <= score) {
                            bestPrediction = accentPhrase;
                            bestScore = score;
                        }
                    }
                }
                if (bestPrediction != null) {
                    hasPredictions = true;
                    sbPredictions.append(bestPrediction);
                }
                else {
                    for (int c = 0; c < words[i].length(); c++) {
                        sbPredictions.append(".");
                    }
                }
            }
            if (!hasPredictions) {
                return null;
            }
            // don't normalize yet, wait until incorporating with the full prediction from server
            return sbPredictions.toString();
        }
    }

    @Override public void learn(String rawPhrase, String accentPhrase) {
        learnStatic(rawPhrase, accentPhrase, (short)1, model1, model2, model3, mPhraseHistory);
    }

    @Override public void dispose() {
        // TODO: stupid serialization scheme for now, implement efficient trie encoding instead
        try {
            // write from history to file
            // overwriting existing content but should be ok since history is only larger
            // this is inefficient, see todo above.

            mLocalModelBinaryWriter.writeInt(mPhraseHistory.size()); // update count

            for (String s : mPhraseHistory.keySet()) {
                byte[] accentBytes = s.getBytes("UTF-8"); // accent string
                mLocalModelBinaryWriter.writeByte(accentBytes.length);
                mLocalModelBinaryWriter.write(accentBytes);

                mLocalModelBinaryWriter.writeInt(mPhraseHistory.get(s));
            }
        }
        catch (IOException ex) { }
    }

    @Override public int version() {
        return ModelVersion.LINEAR_BACKOFF_INTERPOLATION;
    }

    private static void learnStatic(String rawPhrase, String accentPhrase, short count,
        PhraseLookup m1, PhraseLookup m2, PhraseLookup m3, HashMap<String, Integer> hist)
    {
        if (rawPhrase == null || accentPhrase == null) {
            return;
        }

        String[] rawWords = rawPhrase.trim().toLowerCase().split("\\s+");

        String lowerAccentPhrase = accentPhrase.trim().toLowerCase();
        if (hist.containsKey(lowerAccentPhrase)) {
            hist.put(lowerAccentPhrase, hist.get(lowerAccentPhrase) + count);
        }
        else {
            hist.put(lowerAccentPhrase, (int)count);
        }

        String[] accentWords = lowerAccentPhrase.split("\\s+");

        // for single word query, simply do look up
        if (rawWords.length == 1) {
            m1.add(rawWords[0], accentWords[0], count);
            return;
        }

        for (int i = 0; i < rawWords.length - 1; i++) {
            // TODO: use composite hash key instead of concatenating string with spaces
            m2.add(
                    String.format("%s %s", rawWords[i], rawWords[i + 1]),
                    String.format("%s %s", accentWords[i], accentWords[i + 1]),
                    count
            );
        }

        for (int i = 0; i < rawWords.length - 2; i++) {
            m3.add(
                    String.format("%s %s %s", rawWords[i], rawWords[i + 1], rawWords[i + 2]),
                    String.format("%s %s %s", accentWords[i], accentWords[i + 1], accentWords[i + 2]),
                    count
            );
        }
    }

    private void ComputeAccentScore (
        String[] words,
        int iW,
        double weight,
        PhraseLookup model,
        int n,
        HashMap<String, Double> accentScoreMap) {

        if (model.isEmpty()) {
            return;
        }

        int g = n - 1;

        // compute accent probability for this word
        int g3Start = Math.max(iW - g, 0);
        int g3End = Math.min(iW + g, words.length - 1);

        for (int jW = g3Start; jW <= g3End - g; jW++) {
            String segment =
                (g == 2) ? String.format("%s %s %s", words[jW], words[jW + 1], words[jW + 2]) :
                (g == 1) ? String.format("%s %s", words[jW], words[jW + 1]) : words[jW];

            HashMap<String, Short> accentsCountMap = model.lookup(segment);
            if (accentsCountMap == null) {
                continue;
            }

            double count = 0;
            for (Short c : accentsCountMap.values()) {
                count += c;
            }

            for (String accents : accentsCountMap.keySet()) {
                String[] accWords = accents.split("\\s+");
                String accentedWord = accWords[iW - jW];
                double accScore = (accentsCountMap.get(accents) / count) * weight;

                if (!accentScoreMap.containsKey(accentedWord)) {
                    accentScoreMap.put(accentedWord, 0.0);
                }
                accentScoreMap.put(accentedWord, accentScoreMap.get(accentedWord) + accScore);
            }
        }
    }

    class PhraseLookup extends HashMap<String, HashMap<String, Short>> {
        public boolean isEmpty() {
            return this.size() == 0;
        }

        public HashMap<String, Short> lookup(String rawPhrase) {
            if (this.containsKey(rawPhrase)) {
                return this.get((rawPhrase));
            }
            return null;
        }

        public void add(String rawPhrase, String accentPhrase, short count) {
            if (this.containsKey(rawPhrase)) {
                HashMap<String, Short> accentPairs = this.get(rawPhrase);
                if (accentPairs.containsKey(accentPhrase)) {
                    accentPairs.put(accentPhrase, (short)(accentPairs.get(accentPhrase) + count));
                }
                else {
                    accentPairs.put(accentPhrase, (short)0);
                }
            }
            else {
                HashMap<String, Short> accentPairs = new HashMap<>();
                accentPairs.put(accentPhrase, count);
                this.put(rawPhrase, accentPairs);
            }
        }

        public void merge(PhraseLookup pl) {
            if (pl == null) {
                return;
            }
            for (String rawPhrase : pl.keySet()) {
                for (String accentPhrase : pl.get(rawPhrase).keySet()) {
                    this.add(rawPhrase, accentPhrase, pl.get(rawPhrase).get(accentPhrase));
                }
            }
        }
    }

    class PhraseHistory extends HashMap<String, Integer> {
        public void merge(PhraseHistory ph) {
            if (ph == null) {
                return;
            }
            for (String phrase : ph.keySet()) {
                if (this.containsKey(phrase)) {
                    this.put(phrase, ph.get(phrase) + this.get(phrase));
                }
                else {
                    this.put(phrase, ph.get(phrase));
                }
            }
        }
    }

    private class LoadFromFile extends AsyncTask<Void, Void, LocalModelItemData> {
        private Context mContext;

        public LoadFromFile(Context context) {
            mContext = context;
        }

        /** The system calls this to perform work in a worker thread and
         * delivers it the parameters given to AsyncTask.execute() */
        protected LocalModelItemData doInBackground(Void... params) {
            LocalModelItemData localModel = new LocalModelItemData();

            try
            {
                Debug.waitForDebugger();
                String localModelFileName = mContext.getString(R.string.model_file_name);
                File localModelFile = new File(mContext.getFilesDir(), localModelFileName);

                mLocalModelOutputStream = new FileOutputStream(localModelFile);
                mLocalModelBinaryWriter = new DataOutputStream(mLocalModelOutputStream);

                // if not exists, then write header information and return
                if (!localModelFile.exists()) {
                    mLocalModelBinaryWriter.writeInt(version()); // model version
                    mLocalModelBinaryWriter.writeInt(0); // number of entries
                    return localModel;
                }

                CountingFileInputStream fileInputStream = new CountingFileInputStream(localModelFile);
                DataInputStream binaryReader = new DataInputStream(fileInputStream);

                try {
                    int modelVersion = binaryReader.readInt();
                    if (modelVersion != version()) {
                        // TODO: convert from older versions to this version instead of deleting
                        mLocalModelOutputStream.getChannel().truncate(0);
                        mLocalModelBinaryWriter.writeInt(version()); // model version
                        mLocalModelBinaryWriter.writeInt(0); // number of entries
                        return localModel;
                    }
                    int numEntries = binaryReader.readInt();

                    PhraseLookup m1 = new PhraseLookup();
                    PhraseLookup m2 = new PhraseLookup();
                    PhraseLookup m3 = new PhraseLookup();
                    PhraseHistory hist = new PhraseHistory();

                    for (int i = 0; i < numEntries; i++) {
                        // Read accent string
                        byte numUnicodeBytes = binaryReader.readByte();
                        byte[] unicodeBytes = new byte[numUnicodeBytes];
                        binaryReader.read(unicodeBytes);
                        String accentPhrase = new String(unicodeBytes, "UTF-8");
                        StringBuilder rawString = new StringBuilder();

                        char[] accentChars = accentPhrase.toCharArray();
                        for (char c : accentChars) {
                            if (LanguageConstruct.AccentToRawMap.containsKey(c)) {
                                rawString.append(LanguageConstruct.AccentToRawMap.get(c));
                            }
                            else {
                                rawString.append(c);
                            }
                        }

                        // Read # occurrences for this accent string
                        int count = binaryReader.readInt();

                        learnStatic(rawString.toString(), accentPhrase, (short)count, m1, m2, m3, hist);

                        localModel.model1 = m1;
                        localModel.model2 = m2;
                        localModel.model3 = m3;
                        localModel.history = hist;
                    }
                }
                finally {
                    binaryReader.close();
                }
            }
            catch (UnsupportedEncodingException ex) { }
            catch (IOException ex)
            {
                String s = ex.toString();
                if (s.length() > 0) {
                    int x = 10;
                    x++;
                    s += x;
                }
            }

            return localModel;
        }

        /** The system calls this to perform work in the UI thread and delivers
         * the result from doInBackground() */
        protected void onPostExecute(LocalModelItemData md) {
            model1.merge(md.model1);
            model2.merge(md.model2);
            model3.merge(md.model3);
            mPhraseHistory.merge(md.history);
        }
    }

    private class LocalModelItemData {
        PhraseLookup model1;
        PhraseLookup model2;
        PhraseLookup model3;
        PhraseHistory history;
    }
}