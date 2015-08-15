package com.accentype.android.softkeyboard;

/**
 * Represents a phrase of up to three words.
 */
public class Phrase {
    public static final String BeginMarker = "+";
    public static final String EndMarker = "-";

    private String w1 = "";
    private String w2 = "";
    private String w3 = "";

    public Phrase(String q1) {
        w1 = q1;
    }

    public Phrase(String q1, String q2) {
        w1 = q1;
        w2 = q2;
    }

    public Phrase(String q1, String q2, String q3) {
        w1 = q1;
        w2 = q2;
        w3 = q3;
    }

    public int hashCode() {
        return 29791 + 961 * w1.hashCode() + 31 * w2.hashCode() + w3.hashCode();
    }

    public boolean equals(Object o) {
        if ( o == null ) {
            return false;
        }
        if ( this.getClass() != o.getClass() ) {
            return false;
        }
        Phrase p = (Phrase)o;
        return w1.equals(p.w1) && w2.equals(p.w2) && w3.equals(p.w3);
    }
}
