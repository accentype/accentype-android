package com.accentype.android.softkeyboard;

public class StringUtil {
    /**
     * Normalize cases for the query string w.r.t the original while preserving white-spaces in the original,
     * where each dot in the query means to skip a non-whitespace character. The string to return
     * has length = minLength(original, query), so it can be truncated if query is longer than original.
     * Example: original = "hello World", query = ".....werl", result = "..... Werl".
     *
     * @param original the original string.
     * @param query the query string.
     * @return new string copied from query while preserving cases that are in original.
     */
    public static String normalizeStringCaseDottedTruncate(String original, StringBuilder query) {
        StringBuilder sbOriginal = new StringBuilder(original);
        int j = 0;
        for (int i = 0; i < sbOriginal.length() && j < query.length(); i++) {
            char c = sbOriginal.charAt(i);
            if (Character.isWhitespace(c)) {
                continue;
            }
            char p = query.charAt(j);
            if (p != '.') {
                if (Character.isUpperCase(c)) {
                    sbOriginal.setCharAt(i, Character.toUpperCase(query.charAt(j)));
                }
                else {
                    sbOriginal.setCharAt(i, query.charAt(j));
                }
            }
            j++;
        }
        return sbOriginal.toString();
    }

    /**
     * Normalize cases for the query word w.r.t the original returning a word with length = length(query).
     * Input strings are assumed to have no whitespaces.
     * Example: original = "Hell", query = "hello", result = "Hello".
     *
     * @param original the original word.
     * @param query the query word.
     * @return new word copied from query while preserving cases in original.
     */
    public static String normalizeWordCasePreserve(String original, String query) {
        StringBuilder sbNormalized = new StringBuilder(query);
        for (int i = 0; i < query.length(); i++) {
            if (i >= original.length()) {
                break;
            }
            if (Character.isUpperCase(original.charAt(i))) {
                sbNormalized.setCharAt(i, Character.toUpperCase(sbNormalized.charAt(i)));
            }
        }
        return sbNormalized.toString();
    }
}
