package com.accentype.android.softkeyboard;

import android.content.Context;

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

    protected LinearBackoffInterpolationModel(Context context) {
        model1 = new PhraseLookup();
        model2 = new PhraseLookup();
        model3 = new PhraseLookup();
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
        for (int i = 0; i < words.length; i++) {
            HashMap<String, Double> accScoreMap = new HashMap<>();

            ComputeAccentScore(words, i, beta3, model3, 3, accScoreMap);
            ComputeAccentScore(words, i, beta2, model2, 2, accScoreMap);
            ComputeAccentScore(words, i, beta1, model1, 1, accScoreMap);

            if (accScoreMap != null && accScoreMap.size() > 0) {
                String bestPrediction = null;
                double bestScore = -1;
                for (String accentPhrase : accScoreMap.keySet()) {
                    double score = accScoreMap.get(accentPhrase);
                    if (bestScore <= score) {
                        bestPrediction = accentPhrase;
                        bestScore = score;
                    }
                }
                if (bestPrediction != null) {
                    sbPredictions.append(bestPrediction);
                }
                else {
                    sbPredictions.append(words[i]);
                }
            }
        }

        if (sbPredictions.length() == 0) {
            return null;
        }
        // normalize raw string with accented string
        StringBuilder sbReturn = new StringBuilder(rawPhrase);
        int j = 0;
        for (int i = 0; i < sbReturn.length() && j < sbPredictions.length(); i++) {
            char c = sbReturn.charAt(i);
            if (Character.isWhitespace(c)) {
                continue;
            }
            if (Character.isUpperCase(c)) {
                sbReturn.setCharAt(i, Character.toUpperCase(sbPredictions.charAt(j)));
            }
            else {
                sbReturn.setCharAt(i, sbPredictions.charAt(j));
            }
            j++;
        }

        return sbReturn.toString();
    }

    @Override public void learn(String rawPhrase, String accentPhrase) {
        if (rawPhrase == null || accentPhrase == null) {
            return;
        }

        String[] rawWords = rawPhrase.trim().toLowerCase().split("\\s+");
        String[] accentWords = accentPhrase.trim().toLowerCase().split("\\s+");

        for (int i = 0; i < rawWords.length; i++) {
            model1.add(rawWords[i], accentWords[i]);
        }

        for (int i = 0; i < rawWords.length - 1; i++) {
            // TODO: use composite hash key instead of concatenating string with spaces
            model2.add(
                String.format("%s %s", rawWords[i], rawWords[i + 1]),
                String.format("%s %s", accentWords[i], accentWords[i + 1])
            );
        }

        for (int i = 0; i < rawWords.length - 2; i++) {
            model3.add(
                String.format("%s %s %s", rawWords[i], rawWords[i + 1], rawWords[i + 2]),
                String.format("%s %s %s", accentWords[i], accentWords[i + 1], accentWords[i + 2])
            );
        }
    }

    @Override public int version() {
        return ModelVersion.LINEAR_BACKOFF_INTERPOLATION;
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

            HashMap<String, Short> accentsCountMap = model.get(segment);
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

    class PhraseLookup {
        private HashMap<String, HashMap<String, Short>> predictions = null;

        public PhraseLookup() {
            predictions = new HashMap<>();
        }

        public boolean isEmpty() {
            return predictions.size() == 0;
        }

        public HashMap<String, Short> get(String rawPhrase) {
            if (predictions.containsKey(rawPhrase)) {
                return predictions.get((rawPhrase));
            }
            return null;
        }

        public void add(String rawPhrase, String accentPhrase) {
            if (predictions.containsKey(rawPhrase)) {
                HashMap<String, Short> accentPairs = predictions.get(rawPhrase);
                if (accentPairs.containsKey(accentPhrase)) {
                    accentPairs.put(accentPhrase, (short)(accentPairs.get(accentPhrase) + 1));
                }
                else {
                    accentPairs.put(accentPhrase, (short)0);
                }
            }
            else {
                HashMap<String, Short> accentPairs = new HashMap<>();
                accentPairs.put(accentPhrase, (short)1);
                predictions.put(rawPhrase, accentPairs);
            }
        }
    }
}