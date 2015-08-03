package com.accentype.android.softkeyboard;

/**
 * Represents a phrase of up to three words.
 */
public class Phrase {
    private String w1 = null;
    private String w2 = null;
    private String w3 = null;

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
        int hashCode = 1;
        hashCode = (w1 == null) ? 31 * hashCode : 31 * hashCode + w1.hashCode();
        hashCode = (w2 == null) ? 31 * hashCode : 31 * hashCode + w2.hashCode();
        hashCode = (w3 == null) ? 31 * hashCode : 31 * hashCode + w3.hashCode();
        return hashCode;
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
