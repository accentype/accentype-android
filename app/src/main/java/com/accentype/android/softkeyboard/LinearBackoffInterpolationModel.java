package com.accentype.android.softkeyboard;

import android.content.Context;
import android.os.AsyncTask;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.HashMap;

/**
 * Linear model with backoff interpolation.
 */
public class LinearBackoffInterpolationModel implements BaseModel {

    private PhraseMap model1 = null;
    private PhraseMap model2 = null;
    private PhraseMap model3 = null;

    private static LinearBackoffInterpolationModel instance = null;

    private float beta3 = 0.2f;
    private float beta2 = 0.15f;
    private float beta1 = 0.1f;

    private PhraseHistory mPhraseHistory;
    private Context mContext;

    protected LinearBackoffInterpolationModel(Context context) {
        mContext = context;
        new LoadFromFile().execute();

        model1 = new PhraseMap();
        model2 = new PhraseMap();
        model3 = new PhraseMap();
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
            HashMap<String, Short> accentsCountMap = model1.lookup(new Phrase(words[0]));
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
            return StringUtil.replaceDottedPreserveCase(rawPhrase, sbPredictions);
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

            String localModelFileName = mContext.getString(R.string.model_file_name);
            File localModelFile = new File(mContext.getFilesDir(), localModelFileName);

            FileOutputStream localModelOutputStream = new FileOutputStream(localModelFile); // truncates file if exists
            DataOutputStream localModelBinaryWriter = new DataOutputStream(localModelOutputStream);

            localModelBinaryWriter.writeInt(version()); // model version
            localModelBinaryWriter.writeInt(mPhraseHistory.size()); // update count

            for (String s : mPhraseHistory.keySet()) {
                byte[] accentBytes = s.getBytes("UTF-8"); // accent string
                localModelBinaryWriter.writeByte(accentBytes.length);
                localModelBinaryWriter.write(accentBytes);

                localModelBinaryWriter.writeInt(mPhraseHistory.get(s));
            }

            LogUtil.LogMessage(this.getClass().getName(),
                    MessageFormat.format("Serialized model with {0} unique phrases", mPhraseHistory.size())
            );
        }
        catch (IOException ex) {
            LogUtil.LogError(this.getClass().getName(), "Error in async server predict", ex);
        }
    }

    @Override public int version() {
        return ModelVersion.LINEAR_BACKOFF_INTERPOLATION;
    }

    private static void learnStatic(String rawPhrase, String accentPhrase, short count,
        PhraseMap m1, PhraseMap m2, PhraseMap m3, HashMap<String, Integer> hist)
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
            m1.add(new Phrase(rawWords[0]), accentWords[0], count);
            return;
        }

        for (int i = 0; i < rawWords.length - 1; i++) {
            // TODO: use composite hash key instead of concatenating string with spaces
            m2.add(
                new Phrase(rawWords[i], rawWords[i + 1]),
                String.format("%s %s", accentWords[i], accentWords[i + 1]),
                count
            );
        }

        for (int i = 0; i < rawWords.length - 2; i++) {
            m3.add(
                new Phrase(rawWords[i], rawWords[i + 1], rawWords[i + 2]),
                String.format("%s %s %s", accentWords[i], accentWords[i + 1], accentWords[i + 2]),
                count
            );
        }
    }

    private void ComputeAccentScore (
        String[] words,
        int iW,
        double weight,
        PhraseMap model,
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
            Phrase segment =
                (g == 2) ? new Phrase(words[jW], words[jW + 1], words[jW + 2]) :
                (g == 1) ? new Phrase(words[jW], words[jW + 1]) : new Phrase(words[jW]);

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

    private class LoadFromFile extends AsyncTask<Void, Void, LocalModelItemData> {
        /** The system calls this to perform work in a worker thread and
         * delivers it the parameters given to AsyncTask.execute() */
        protected LocalModelItemData doInBackground(Void... params) {
            LocalModelItemData localModel = new LocalModelItemData();

            try
            {
                String localModelFileName = mContext.getString(R.string.model_file_name);
                File localModelFile = new File(mContext.getFilesDir(), localModelFileName);

                // ok if file not exists, will be created & written to on destroy
                if (!localModelFile.exists() || localModelFile.length() == 0) {
                    return localModel;
                }

                CountingFileInputStream fileInputStream = new CountingFileInputStream(localModelFile);
                DataInputStream binaryReader = new DataInputStream(fileInputStream);

                try {
                    int modelVersion = binaryReader.readInt();
                    if (modelVersion != version()) {
                        // ok if older version, will be truncated & written to on destroy
                        // TODO: convert from older version to maintain history
                        return localModel;
                    }
                    int numEntries = binaryReader.readInt();

                    PhraseMap m1 = new PhraseMap();
                    PhraseMap m2 = new PhraseMap();
                    PhraseMap m3 = new PhraseMap();
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

                        LogUtil.LogMessage(this.getClass().getName(), "Successfully loaded local model file.");
                    }
                }
                finally {
                    binaryReader.close();
                }
            }
            catch (Exception ex)
            {
                // If any exception occurs when reading, the local model file will be recreated
                // with new history on destroy.
                LogUtil.LogError(this.getClass().getName(), "Error in async local model load", ex);
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
        PhraseMap model1;
        PhraseMap model2;
        PhraseMap model3;
        PhraseHistory history;
    }
}