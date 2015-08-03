package com.accentype.android.softkeyboard;

import java.util.HashMap;

/**
 * Mapping from unaccented phrase to a map of accented phrases and counts.
 */
public class PhraseMap extends HashMap<Phrase, HashMap<String, Short>> {
    public boolean isEmpty() {
        return this.size() == 0;
    }

    /**
     * Look up a map of accented phrases & counts by the specified phrase.
     *
     * @param rawPhrase The unaccented phrase to look up.
     */
    public HashMap<String, Short> lookup(Phrase rawPhrase) {
        if (this.containsKey(rawPhrase)) {
            return this.get((rawPhrase));
        }
        return null;
    }

    /**
     * Add a correspondence between an unaccented phrase and its accented version & count.
     *
     * @param rawPhrase The unaccented phrase.
     * @param accentPhrase The corresponding accented phrase.
     * @param count The # of occurrences of the accented phrase.
     */
    public void add(Phrase rawPhrase, String accentPhrase, short count) {
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

    /**
     * Merges the current mapping with another.
     *
     * @param pl The mapping to merge with.
     */
    public void merge(PhraseMap pl) {
        if (pl == null) {
            return;
        }
        for (Phrase rawPhrase : pl.keySet()) {
            for (String accentPhrase : pl.get(rawPhrase).keySet()) {
                this.add(rawPhrase, accentPhrase, pl.get(rawPhrase).get(accentPhrase));
            }
        }
    }
}
