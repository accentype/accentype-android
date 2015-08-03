package com.accentype.android.softkeyboard;

import java.util.HashMap;

/**
 * Represents a history of phrases as a map from phrase to count.
 */
public class PhraseHistory extends HashMap<String, Integer> {

    /**
     * Merges the current history with another history object.
     *
     * @param ph The phrase to merge with.
     */
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
