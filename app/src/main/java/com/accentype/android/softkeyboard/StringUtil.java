package com.accentype.android.softkeyboard;

public class StringUtil {
    /**
     * Replace a portion of a string with another specified string while preserving white-spaces
     * & cases in the original. Each dot in the replace string means to skip a non-whitespace
     * character. The string to return has length = length(original), however original & query
     * needs not be the same length. Example: original = "hello World", replace = ".....werl",
     * result = "hello Werld".
     *
     * @param original the original string.
     * @param replace the string to replace in the original string.
     * @return a modified string that preserves cases & whitespaces in the original string.
     */
    public static String replaceDottedPreserveCase(String original, StringBuilder replace) {
        StringBuilder sbOriginal = new StringBuilder(original);
        int j = 0;
        for (int i = 0; i < sbOriginal.length() && j < replace.length(); i++) {
            char c = sbOriginal.charAt(i);
            if (Character.isWhitespace(c)) {
                continue;
            }
            char p = replace.charAt(j);
            if (p != '.') {
                if (Character.isUpperCase(c)) {
                    sbOriginal.setCharAt(i, Character.toUpperCase(replace.charAt(j)));
                }
                else {
                    sbOriginal.setCharAt(i, replace.charAt(j));
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
