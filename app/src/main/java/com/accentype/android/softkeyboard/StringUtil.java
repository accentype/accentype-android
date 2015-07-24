package com.accentype.android.softkeyboard;

public class StringUtil {
    // normalize case for the query string w.r.t the original while preserving white-spaces in the original,
    // where each dot in the query means to skip a non-whitespace character
    // example: original = "hello World", query = ".....werl", result = "..... Werl"
    public static String normalizeStringCase(String rawPhrase, StringBuilder sbPrediction) {
        StringBuilder sbOriginal = new StringBuilder(rawPhrase);
        int j = 0;
        for (int i = 0; i < sbOriginal.length() && j < sbPrediction.length(); i++) {
            char c = sbOriginal.charAt(i);
            if (Character.isWhitespace(c)) {
                continue;
            }
            char p = sbPrediction.charAt(j);
            if (p != '.') {
                if (Character.isUpperCase(c)) {
                    sbOriginal.setCharAt(i, Character.toUpperCase(sbPrediction.charAt(j)));
                }
                else {
                    sbOriginal.setCharAt(i, sbPrediction.charAt(j));
                }
            }
            j++;
        }
        return sbOriginal.toString();
    }
}
