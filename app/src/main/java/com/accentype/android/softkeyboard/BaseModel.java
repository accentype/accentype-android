package com.accentype.android.softkeyboard;

/**
 * Base interface for language models.
 */
interface BaseModel {
    /**
     * Predicts the accented phrase for a given unaccented phrase.
     * @param rawPhrase The unaccented phrase to be predicted.
     */
    String predict(String rawPhrase);

    /**
     * Learns on the specified unaccented phrase and its one possible accented interpretation.
     * @param rawPhrase The unaccented phrase to learn on.
     * @param accentPhrase The associated accented phrase.
     */
    void learn(String rawPhrase, String accentPhrase);

    /**
     * Called when the model can safely dispose or flush any data to disk as needed.
     */
    void dispose();

    /**
     * Gets the version number for this model type.
     */
    int version();
}
