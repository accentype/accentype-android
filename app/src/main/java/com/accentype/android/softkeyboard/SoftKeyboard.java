/*
 * Copyright (C) 2008-2009 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.accentype.android.softkeyboard;

import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.inputmethodservice.InputMethodService;
import android.inputmethodservice.Keyboard;
import android.inputmethodservice.KeyboardView;
import android.os.AsyncTask;
import android.os.IBinder;
import android.os.Vibrator;
import android.text.InputType;
import android.text.method.MetaKeyKeyListener;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.inputmethod.CompletionInfo;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputMethodManager;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Example of writing an input method for a soft keyboard.  This code is
 * focused on simplicity over completeness, so it should in no way be considered
 * to be a complete soft keyboard implementation.  Its purpose is to provide
 * a basic example for how you would get started writing an input method, to
 * be fleshed out as appropriate.
 */
public class SoftKeyboard extends InputMethodService
        implements KeyboardView.OnKeyboardActionListener {
    static final boolean DEBUG = false;
    
    /**
     * This boolean indicates the optional example code for performing
     * processing of hard keys in addition to regular text generation
     * from on-screen interaction.  It would be used for input methods that
     * perform language translations (such as converting text entered on 
     * a QWERTY keyboard to Chinese), but may not be used for input methods
     * that are primarily intended to be used for on-screen text entry.
     */
    static final boolean PROCESS_HARD_KEYS = true;

    private SharedPreferences mSharedPreferences;
    private InputMethodManager mInputMethodManager;

    private DataOutputStream mLocalModelBinaryWriter;
    private FileOutputStream mLocalModelOutputStream;

    private LatinKeyboardView mInputView;
    private CandidateView mCandidateView;
    private CompletionInfo[] mCompletions;
    
    private StringBuilder mComposing = new StringBuilder();
    private HashMap<Integer, HashMap<String, LocalModelItemData>> mLocalModel;
    private List<String> mPredictions;
    private String[][] mWordChoices;
    private boolean mPredictionOn;
    private boolean mCompletionOn;
    private int mLastDisplayWidth;
    private boolean mCapsLock;
    private boolean mCapsLocked;
    private long[] mShiftTimes = { 0, 0 };
    private int mShiftTimeIndex = 0;
    private long mMetaState;
    
    private LatinKeyboard mSymbolsKeyboard;
    private LatinKeyboard mSymbolsShiftedKeyboard;
    private LatinKeyboard mQwertyKeyboard;
    
    private LatinKeyboard mCurKeyboard;

    private Vibrator mVibrator;

    private String mWordSeparators;
    private String mSpecialSeparators;

    private static final List<String> EMPTY_LIST = new ArrayList<String>();
    /**
     * Main initialization of the input method component.  Be sure to call
     * to super class.
     */
    @Override public void onCreate() {
        super.onCreate();
        mInputMethodManager = (InputMethodManager)getSystemService(INPUT_METHOD_SERVICE);
        mVibrator = (Vibrator)getSystemService(VIBRATOR_SERVICE);
        mWordSeparators = getResources().getString(R.string.word_separators);
        mSpecialSeparators = getResources().getString(R.string.special_separators);
        mSharedPreferences = this.getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE);

        new LocalModelLoader(this).execute();
    }
    
    /**
     * This is the point where you can do all of your UI initialization.  It
     * is called after creation and any configuration change.
     */
    @Override public void onInitializeInterface() {
        if (mQwertyKeyboard != null) {
            // Configuration changes can happen after the keyboard gets recreated,
            // so we need to be able to re-build the keyboards if the available
            // space has changed.
            int displayWidth = getMaxWidth();
            if (displayWidth == mLastDisplayWidth) return;
            mLastDisplayWidth = displayWidth;
        }
        mQwertyKeyboard = new LatinKeyboard(this, R.xml.qwerty);
        mSymbolsKeyboard = new LatinKeyboard(this, R.xml.symbols);
        mSymbolsShiftedKeyboard = new LatinKeyboard(this, R.xml.symbols_shift);
    }
    
    /**
     * Called by the framework when your view for creating input needs to
     * be generated.  This will be called the first time your input method
     * is displayed, and every time it needs to be re-created such as due to
     * a configuration change.
     */
    @Override public View onCreateInputView() {
        mInputView = (LatinKeyboardView) getLayoutInflater().inflate(
                R.layout.input, null);
        mInputView.setOnKeyboardActionListener(this);
        setLatinKeyboard(mQwertyKeyboard);
        return mInputView;
    }

    private void setLatinKeyboard(LatinKeyboard nextKeyboard) {
        nextKeyboard.setCancelKeyVisibility(false);

        if (mSharedPreferences != null) {
            int languageCode = mSharedPreferences.getInt(
                    getString(R.string.preference_saved_language),
                    LatinKeyboard.LANGUAGE_VN);
            String languageMode = languageCode == LatinKeyboard.LANGUAGE_VN ?
                    getString(R.string.language_vn) :
                    getString(R.string.language_en);
            nextKeyboard.setLanguageLabel(languageMode);
        }

        mInputView.setKeyboard(nextKeyboard);
    }

    /**
     * Called by the framework when your view for showing candidates needs to
     * be generated, like {@link #onCreateInputView}.
     */
    @Override public View onCreateCandidatesView() {
        mCandidateView = new CandidateView(this);
        mCandidateView.setService(this);
        return mCandidateView;
    }

    /**
     * This is the main point where we do our initialization of the input method
     * to begin operating on an application.  At this point we have been
     * bound to the client, and are now receiving all of the detailed information
     * about the target of our edits.
     */
    @Override public void onStartInput(EditorInfo attribute, boolean restarting) {
        super.onStartInput(attribute, restarting);
        
        // Reset our state.  We want to do this even if restarting, because
        // the underlying state of the text editor could have changed in any way.
        mComposing.setLength(0);
        updatePredictions();
        updateCandidates();
        
        if (!restarting) {
            // Clear shift states.
            mMetaState = 0;
        }
        
        mPredictionOn = false;
        mCompletionOn = false;
        mCompletions = null;
        
        // We are now going to initialize our state based on the type of
        // text being edited.
        switch (attribute.inputType & InputType.TYPE_MASK_CLASS) {
            case InputType.TYPE_CLASS_NUMBER:
            case InputType.TYPE_CLASS_DATETIME:
                // Numbers and dates default to the symbols keyboard, with
                // no extra features.
                mCurKeyboard = mSymbolsKeyboard;
                break;
                
            case InputType.TYPE_CLASS_PHONE:
                // Phones will also default to the symbols keyboard, though
                // often you will want to have a dedicated phone keyboard.
                mCurKeyboard = mSymbolsKeyboard;
                break;
                
            case InputType.TYPE_CLASS_TEXT:
                // This is general text editing.  We will default to the
                // normal alphabetic keyboard, and assume that we should
                // be doing predictive text (showing candidates as the
                // user types).
                mCurKeyboard = mQwertyKeyboard;
                mPredictionOn = true;
                
                // We now look for a few special variations of text that will
                // modify our behavior.
                int variation = attribute.inputType & InputType.TYPE_MASK_VARIATION;
                if (variation == InputType.TYPE_TEXT_VARIATION_PASSWORD ||
                        variation == InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD) {
                    // Do not display predictions / what the user is typing
                    // when they are entering a password.
                    mPredictionOn = false;
                }
                
                if (variation == InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
                        || variation == InputType.TYPE_TEXT_VARIATION_URI
                        || variation == InputType.TYPE_TEXT_VARIATION_FILTER) {
                    // Our predictions are not useful for e-mail addresses
                    // or URIs.
                    mPredictionOn = false;
                }
                
                if ((attribute.inputType & InputType.TYPE_TEXT_FLAG_AUTO_COMPLETE) != 0) {
                    // If this is an auto-complete text view, then our predictions
                    // will not be shown and instead we will allow the editor
                    // to supply their own.  We only show the editor's
                    // candidates when in fullscreen mode, otherwise relying
                    // own it displaying its own UI.
                    mPredictionOn = false;
                    mCompletionOn = isFullscreenMode();
                }
                
                // We also want to look at the current state of the editor
                // to decide whether our alphabetic keyboard should start out
                // shifted.
                updateShiftKeyState(attribute);
                break;
                
            default:
                // For all unknown input types, default to the alphabetic
                // keyboard with no special features.
                mCurKeyboard = mQwertyKeyboard;
                updateShiftKeyState(attribute);
        }
        
        // Update the label on the enter key, depending on what the application
        // says it will do.
        mCurKeyboard.setImeOptions(getResources(), attribute.imeOptions);
    }

    /**
     * This is called when the user is done editing a field.  We can use
     * this to reset our state.
     */
    @Override public void onFinishInput() {
        super.onFinishInput();
        
        // Clear current composing text and candidates.
        mComposing.setLength(0);
        updatePredictions();
        updateCandidates();
        
        // We only hide the candidates window when finishing input on
        // a particular editor, to avoid popping the underlying application
        // up and down if the user is entering text into the bottom of
        // its window.
        setCandidatesViewShown(false);
        
        mCurKeyboard = mQwertyKeyboard;
        if (mInputView != null) {
            mInputView.closing();
        }
    }
    
    @Override public void onStartInputView(EditorInfo attribute, boolean restarting) {
        super.onStartInputView(attribute, restarting);
        // Apply the selected keyboard to the input view.
        setLatinKeyboard(mCurKeyboard);
        mInputView.closing();
    }

    /**
     * Deal with the editor reporting movement of its cursor.
     */
    @Override public void onUpdateSelection(int oldSelStart, int oldSelEnd,
            int newSelStart, int newSelEnd,
            int candidatesStart, int candidatesEnd) {
        super.onUpdateSelection(oldSelStart, oldSelEnd, newSelStart, newSelEnd,
                candidatesStart, candidatesEnd);
        
        // If the current selection in the text view changes, we should
        // clear whatever candidate text we have.
        if (mComposing.length() > 0 && (newSelStart != candidatesEnd
                || newSelEnd != candidatesEnd)) {
            mComposing.setLength(0);
            updatePredictions();
            updateCandidates();
            InputConnection ic = getCurrentInputConnection();
            if (ic != null) {
                ic.finishComposingText();
            }
        }
    }

    /**
     * This tells us about completions that the editor has determined based
     * on the current text in it.  We want to use this in fullscreen mode
     * to show the completions ourself, since the editor can not be seen
     * in that situation.
     */
    @Override public void onDisplayCompletions(CompletionInfo[] completions) {
        if (mCompletionOn) {
            mCompletions = completions;
            if (completions == null) {
                setSuggestions(null, null, false, false);
                return;
            }
            
            List<String> stringList = new ArrayList<String>();
            for (int i = 0; i < completions.length; i++) {
                CompletionInfo ci = completions[i];
                if (ci != null) stringList.add(ci.getText().toString());
            }
            setSuggestions(stringList, null, true, true);
        }
    }
    
    /**
     * This translates incoming hard key events in to edit operations on an
     * InputConnection.  It is only needed when using the
     * PROCESS_HARD_KEYS option.
     */
    private boolean translateKeyDown(int keyCode, KeyEvent event) {
        mMetaState = MetaKeyKeyListener.handleKeyDown(mMetaState,
                keyCode, event);
        int c = event.getUnicodeChar(MetaKeyKeyListener.getMetaState(mMetaState));
        mMetaState = MetaKeyKeyListener.adjustMetaAfterKeypress(mMetaState);
        InputConnection ic = getCurrentInputConnection();
        if (c == 0 || ic == null) {
            return false;
        }
        
        boolean dead = false;

        if ((c & KeyCharacterMap.COMBINING_ACCENT) != 0) {
            dead = true;
            c = c & KeyCharacterMap.COMBINING_ACCENT_MASK;
        }
        
        if (mComposing.length() > 0) {
            char accent = mComposing.charAt(mComposing.length() -1 );
            int composed = KeyEvent.getDeadChar(accent, c);

            if (composed != 0) {
                c = composed;
                mComposing.setLength(mComposing.length()-1);
            }
        }
        
        onKey(c, null);
        
        return true;
    }
    
    /**
     * Use this to monitor key events being delivered to the application.
     * We get first crack at them, and can either resume them or let them
     * continue to the app.
     */
    @Override public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_BACK:
                // The InputMethodService already takes care of the back
                // key for us, to dismiss the input method if it is shown.
                // However, our keyboard could be showing a pop-up window
                // that back should dismiss, so we first allow it to do that.
                if (event.getRepeatCount() == 0 && mInputView != null) {
                    if (mInputView.handleBack()) {
                        return true;
                    }
                }
                break;
                
            case KeyEvent.KEYCODE_DEL:
                // Special handling of the delete key: if we currently are
                // composing text for the user, we want to modify that instead
                // of let the application to the delete itself.
                if (mComposing.length() > 0) {
                    onKey(Keyboard.KEYCODE_DELETE, null);
                    return true;
                }
                break;
                
            case KeyEvent.KEYCODE_ENTER:
                // Let the underlying text editor always handle these.
                return false;
                
            default:
                // For all other keys, if we want to do transformations on
                // text being entered with a hard keyboard, we need to process
                // it and do the appropriate action.
                if (PROCESS_HARD_KEYS) {
                    if (keyCode == KeyEvent.KEYCODE_SPACE
                            && (event.getMetaState()&KeyEvent.META_ALT_ON) != 0) {
                        // A silly example: in our input method, Alt+Space
                        // is a shortcut for 'android' in lower case.
                        InputConnection ic = getCurrentInputConnection();
                        if (ic != null) {
                            // First, tell the editor that it is no longer in the
                            // shift state, since we are consuming this.
                            ic.clearMetaKeyStates(KeyEvent.META_ALT_ON);
                            keyDownUp(KeyEvent.KEYCODE_A);
                            keyDownUp(KeyEvent.KEYCODE_N);
                            keyDownUp(KeyEvent.KEYCODE_D);
                            keyDownUp(KeyEvent.KEYCODE_R);
                            keyDownUp(KeyEvent.KEYCODE_O);
                            keyDownUp(KeyEvent.KEYCODE_I);
                            keyDownUp(KeyEvent.KEYCODE_D);
                            // And we consume this event.
                            return true;
                        }
                    }
                    if (mPredictionOn && translateKeyDown(keyCode, event)) {
                        return true;
                    }
                }
        }
        
        return super.onKeyDown(keyCode, event);
    }

    /**
     * Use this to monitor key events being delivered to the application.
     * We get first crack at them, and can either resume them or let them
     * continue to the app.
     */
    @Override public boolean onKeyUp(int keyCode, KeyEvent event) {
        // If we want to do transformations on text being entered with a hard
        // keyboard, we need to process the up events to update the meta key
        // state we are tracking.
        if (PROCESS_HARD_KEYS) {
            if (mPredictionOn) {
                mMetaState = MetaKeyKeyListener.handleKeyUp(mMetaState,
                        keyCode, event);
            }
        }
        
        return super.onKeyUp(keyCode, event);
    }

    /**
     * Helper function to commit any text being composed in to the editor.
     */
    private void commitTyped(InputConnection inputConnection) {
        if (mComposing.length() > 0) {
            List<String> suggestions = mCandidateView != null ? mCandidateView.getSuggestions() : mPredictions;
            if (suggestions != null && suggestions.size() > 0) {
                String prediction = suggestions.get(0);
                inputConnection.commitText(prediction, prediction.length());
                addToLocalModel(mComposing.toString(), prediction);
            }
            else {
                inputConnection.commitText(mComposing, mComposing.length());
            }
            mComposing.setLength(0);
            updatePredictions();
            updateCandidates();
        }
    }

    private void commitPrediction(int index) {
        if (mCandidateView != null && mComposing.length() > 0) {
            List<String> suggestions = mCandidateView.getSuggestions();
            if (suggestions != null && index >= 0 && index < suggestions.size()) {
                String prediction = suggestions.get(index);
                getCurrentInputConnection().commitText(prediction, prediction.length());
                addToLocalModel(mComposing.toString(), prediction);
            }
        }
    }

    /**
     * Helper to update the shift state of our keyboard based on the initial
     * editor state.
     */
    private void updateShiftKeyState(EditorInfo attr) {
        if (attr != null 
                && mInputView != null && mQwertyKeyboard == mInputView.getKeyboard()) {
            int caps = 0;
            EditorInfo ei = getCurrentInputEditorInfo();
            if (ei != null && ei.inputType != InputType.TYPE_NULL) {
                caps = getCurrentInputConnection().getCursorCapsMode(attr.inputType);
            }
            mCapsLock = (mCapsLocked || caps != 0);
            mInputView.setShifted(mCapsLock);
        }
    }
    
    /**
     * Helper to determine if a given character code is alphabetic.
     */
    private boolean isAlphabet(int code) {
        if (Character.isLetter(code) || Character.isDigit(code)) {
            return true;
        } else {
            return false;
        }
    }
    
    /**
     * Helper to send a key down / key up pair to the current editor.
     */
    private void keyDownUp(int keyEventCode) {
        getCurrentInputConnection().sendKeyEvent(
                new KeyEvent(KeyEvent.ACTION_DOWN, keyEventCode));
        getCurrentInputConnection().sendKeyEvent(
                new KeyEvent(KeyEvent.ACTION_UP, keyEventCode));
    }
    
    /**
     * Helper to send a character to the editor as raw key events.
     */
    private void sendKey(int keyCode) {
        switch (keyCode) {
            case '\n':
                keyDownUp(KeyEvent.KEYCODE_ENTER);
                break;
            default:
                if (keyCode >= '0' && keyCode <= '9') {
                    keyDownUp(keyCode - '0' + KeyEvent.KEYCODE_0);
                } else {
                    getCurrentInputConnection().commitText(String.valueOf((char) keyCode), 1);
                }
                break;
        }
    }

    // Implementation of KeyboardViewListener

    public void onKey(int primaryCode, int[] keyCodes) {
        if (isWordSeparator(primaryCode)) {
            if (isSpecialSeparator(primaryCode)) {
                handleCharacter(primaryCode, keyCodes);
                if (mInputView.getKeyboard() != mQwertyKeyboard) {
                    setLatinKeyboard(mQwertyKeyboard);
                }
            }
            else {
                // Handle separator
                if (mComposing.length() > 0) {
                    commitTyped(getCurrentInputConnection());
                }
                sendKey(primaryCode);
                updateShiftKeyState(getCurrentInputEditorInfo());
            }
        }
        else if (primaryCode == Keyboard.KEYCODE_DELETE) {
            handleBackspace();
        } else if (primaryCode == Keyboard.KEYCODE_SHIFT) {
            handleShift();
        } else if (primaryCode == Keyboard.KEYCODE_CANCEL) {
            handleClose();
            return;
        } else if (primaryCode == LatinKeyboardView.KEYCODE_LANGUAGE_SWITCH) {
            handleLanguageSwitch();
            return;
        } else if (primaryCode == LatinKeyboardView.KEYCODE_INPUT_METHOD_SWITCH) {
            handleInputMethodSwitch();
            return;
        } else if (primaryCode == LatinKeyboardView.KEYCODE_OPTIONS) {
            // Show a menu or somethin'
        } else if (primaryCode == Keyboard.KEYCODE_MODE_CHANGE
                && mInputView != null) {
            Keyboard current = mInputView.getKeyboard();
            if (current == mSymbolsKeyboard || current == mSymbolsShiftedKeyboard) {
                setLatinKeyboard(mQwertyKeyboard);
            } else {
                setLatinKeyboard(mSymbolsKeyboard);
                mSymbolsKeyboard.setShifted(false);
            }
        } else {
            handleCharacter(primaryCode, keyCodes);
        }
    }

    public void onText(CharSequence text) {
        InputConnection ic = getCurrentInputConnection();
        if (ic == null) return;
        ic.beginBatchEdit();
        if (mComposing.length() > 0) {
            commitTyped(ic);
        }
        ic.commitText(text, 0);
        ic.endBatchEdit();
        updateShiftKeyState(getCurrentInputEditorInfo());
    }

    /**
     * Update the list of available candidates from the current composing
     * text.  This will need to be filled in by however you are determining
     * candidates.
     */
    private void updateCandidates() {
        if (!mCompletionOn) {
            if (mComposing.length() > 0) {
                ArrayList<String> list = new ArrayList<>();
                if (mPredictions == null || mPredictions.size() <= 0) {
                    list.add(mComposing.toString());
                }
                else
                {
                    for (int i = 0; i < mPredictions.size(); i++) {
                        list.add(mPredictions.get(i));
                    }
                }
                setSuggestions(list, mWordChoices, true, true);
            } else {
                setSuggestions(null, null, false, false);
            }
        }
    }

    private void updatePredictions() {
        if (mPredictionOn && getLanguageCode() == LatinKeyboard.LANGUAGE_VN && mComposing.length() > 0) {
            new Predictor().execute(mComposing.toString());
        }
        else {
            mPredictions = EMPTY_LIST;
            mWordChoices = null;
        }
    }

    public void setSuggestions(List<String> suggestions, String[][] wordChoices, boolean completions,
            boolean typedWordValid) {
        if (suggestions != null && suggestions.size() > 0) {
            setCandidatesViewShown(true);
        } else if (isExtractViewShown()) {
            setCandidatesViewShown(true);
        }
        if (mCandidateView != null) {
            mCandidateView.setSuggestions(suggestions, wordChoices, mComposing.toString(), completions, typedWordValid);
        }
    }
    
    private void handleBackspace() {
        final int length = mComposing.length();
        if (length > 1) {
            mComposing.delete(length - 1, length);
            updatePredictions();
            getCurrentInputConnection().setComposingText(mComposing, 1);
            updateCandidates();
        } else if (length > 0) {
            mComposing.setLength(0);
            updatePredictions();
            getCurrentInputConnection().commitText("", 0);
            updateCandidates();
        } else {
            keyDownUp(KeyEvent.KEYCODE_DEL);
        }
        updateShiftKeyState(getCurrentInputEditorInfo());
    }

    private void handleShift() {
        if (mInputView == null) {
            return;
        }
        
        Keyboard currentKeyboard = mInputView.getKeyboard();
        if (mQwertyKeyboard == currentKeyboard) {
            // Alphabet keyboard
            checkToggleCapsLock();
            mInputView.setShifted(mCapsLock);
        } else if (currentKeyboard == mSymbolsKeyboard) {
            mSymbolsKeyboard.setShifted(true);
            setLatinKeyboard(mSymbolsShiftedKeyboard);
            mSymbolsShiftedKeyboard.setShifted(true);
        } else if (currentKeyboard == mSymbolsShiftedKeyboard) {
            mSymbolsShiftedKeyboard.setShifted(false);
            setLatinKeyboard(mSymbolsKeyboard);
            mSymbolsKeyboard.setShifted(false);
        }
    }
    
    private void handleCharacter(int primaryCode, int[] keyCodes) {
        if (isInputViewShown()) {
            if (mInputView.isShifted()) {
                primaryCode = Character.toUpperCase(primaryCode);
            }
        }
        if (mPredictionOn && getLanguageCode() == LatinKeyboard.LANGUAGE_VN) {
            if (isAlphabet(primaryCode) || isSpecialSeparator(primaryCode)) {
                mComposing.append((char) primaryCode);
                updatePredictions();
                getCurrentInputConnection().setComposingText(mComposing, 1);
                updateCandidates();
            }
            else {
                commitTyped(getCurrentInputConnection());
                getCurrentInputConnection().commitText(
                        String.valueOf((char) primaryCode), 1);
            }
        } else {
            getCurrentInputConnection().commitText(
                    String.valueOf((char) primaryCode), 1);
        }
        updateShiftKeyState(getCurrentInputEditorInfo());
    }

    private void handleClose() {
        commitTyped(getCurrentInputConnection());
        requestHideSelf(0);
        mInputView.closing();
    }

    private IBinder getToken() {
        final Dialog dialog = getWindow();
        if (dialog == null) {
            return null;
        }
        final Window window = dialog.getWindow();
        if (window == null) {
            return null;
        }
        return window.getAttributes().token;
    }

    private void handleLanguageSwitch() {
        int languageCode = getLanguageCode();
        int languageResourceCode = R.string.language_vn;
        switch (languageCode) {
            case LatinKeyboard.LANGUAGE_VN:
                languageResourceCode = R.string.language_en;
                languageCode = LatinKeyboard.LANGUAGE_EN;
                break;
            case LatinKeyboard.LANGUAGE_EN:
                languageResourceCode = R.string.language_vn;
                languageCode = LatinKeyboard.LANGUAGE_VN;
                break;
        }

        if (languageCode == LatinKeyboard.LANGUAGE_VN) {
            updatePredictions();
        } else {
            commitTyped(getCurrentInputConnection());
        }

        if (mInputView != null) {
            LatinKeyboard currentKeyboard = (LatinKeyboard)mInputView.getKeyboard();
            if (currentKeyboard != null) {
                currentKeyboard.setLanguageLabel(getString(languageResourceCode));
            }
        }

        saveLanguageCode(languageCode);
    }

    private void handleInputMethodSwitch() {
        mInputMethodManager.showInputMethodPicker();
    }

    private void saveLanguageCode(int languageCode) {
        if (mSharedPreferences != null) {
            SharedPreferences.Editor editor = mSharedPreferences.edit();
            editor.putInt(getString(R.string.preference_saved_language), languageCode);
            editor.commit();
        }
    }

    private int getLanguageCode() {
        int defaultLanguageCode = LatinKeyboard.LANGUAGE_VN;
        if (mSharedPreferences != null) {
            return mSharedPreferences.getInt(getString(R.string.preference_saved_language), defaultLanguageCode);
        }
        return defaultLanguageCode;
    }

    private void checkToggleCapsLock() {
        mShiftTimes[mShiftTimeIndex] = System.currentTimeMillis();
        mShiftTimeIndex = 1 - mShiftTimeIndex;

        // double tap
        if (mCapsLock && Math.abs(mShiftTimes[0] - mShiftTimes[1]) <= 800) {
                mCapsLocked = true; // only lock if already in caps mode
        }
        else {
            mCapsLock = !mCapsLock; // otherwise toggle as usual
            mCapsLocked = false;
        }
    }
    
    public boolean isWordSeparator(int code) {
        return mWordSeparators.contains(String.valueOf((char)code));
    }

    public boolean isSpecialSeparator(int code) {
        return mSpecialSeparators.contains(String.valueOf((char)code));
    }

    public void pickDefaultCandidate() {
        pickSuggestionManually(0);
    }

    public void pickSuggestionManually(int index) {
        if (mCompletionOn && mCompletions != null && index >= 0
                && index < mCompletions.length) {
            CompletionInfo ci = mCompletions[index];
            getCurrentInputConnection().commitCompletion(ci);
            if (mCandidateView != null) {
                mCandidateView.clear();
            }
            updateShiftKeyState(getCurrentInputEditorInfo());
        } else if (mComposing.length() > 0) {
            commitPrediction(index);
        }
    }

    public void updateComposingText(String text) {
        if (text != null) {
            getCurrentInputConnection().setComposingText(text, 1);
        }
    }
    
    public void swipeRight() {
        if (mCompletionOn) {
            pickDefaultCandidate();
        }
    }
    
    public void swipeLeft() {
        handleBackspace();
    }

    public void swipeDown() {
        handleClose();
    }

    public void swipeUp() {
    }
    
    public void onPress(int primaryCode) {
        mVibrator.vibrate(20);
        if (primaryCode != 32 && primaryCode != -5) {
            mInputView.setPreviewEnabled(true);
        }
    }
    
    public void onRelease(int primaryCode) {
        mInputView.setPreviewEnabled(false);
    }

    private class Predictor extends AsyncTask<String, Void, PredictionData> {
        /** The system calls this to perform work in a worker thread and
         * delivers it the parameters given to AsyncTask.execute() */
        protected PredictionData doInBackground(String... composing) {
            StringBuilder query = new StringBuilder(composing[0]);
            String[][] choices = predict(query);

            String localPrediction = getFromLocalModel(composing[0]);

            if (choices != null) {
                List<String> predictions;
                if (choices.length < 10)
                {
                    int[] bins = new int[choices.length + 1];
                    int totalChoices = 1;
                    for (int i = 0; i < choices.length; i++) {
                        bins[i] = totalChoices;
                        totalChoices *= choices[i].length;
                        bins[i + 1] = totalChoices;
                    }
                    int maxNumPredictions = choices.length < 3 ? 10 : 5;
                    int numPredictions = Math.min(maxNumPredictions, totalChoices);

                    predictions = new ArrayList<>(numPredictions);
                    if (localPrediction != null) {
                        predictions.add(localPrediction);
                    }

                    for (int p = 0; p < numPredictions; p++) {
                        StringBuilder prediction = new StringBuilder(query);
                        int q = 0;
                        for (int i = 0; i < choices.length; i++) {
                            int ind = (p % bins[i + 1]) / bins[i];
                            String choice = choices[i][ind];
                            if (q >= prediction.length()) {
                                break;
                            }
                            while (Character.isWhitespace(prediction.charAt(q)))
                            {
                                q++;
                            }
                            prediction.replace(q, q + choice.length(), choice);
                            q += choice.length();
                        }
                        predictions.add(prediction.toString());
                    }
                }
                else
                {
                    StringBuilder prediction = new StringBuilder(query);
                    int q = 0;
                    for (int i = 0; i < choices.length; i++) {
                        String choice = choices[i][0];
                        while (Character.isWhitespace(prediction.charAt(q)))
                        {
                            q++;
                        }
                        prediction.replace(q, q + choice.length(), choice);
                        q += choice.length();
                    }
                    predictions = new ArrayList<>(1);
                    if (localPrediction != null) {
                        predictions.add(localPrediction);
                    }
                    predictions.add(prediction.toString());
                }

                PredictionData data = new PredictionData();
                data.Predictions = predictions;
                data.WordChoices = choices;
                return data;
            }
            return null;
        }

        /** The system calls this to perform work in the UI thread and delivers
         * the result from doInBackground() */
        protected void onPostExecute(PredictionData predictionData) {
            if (mPredictionOn && getLanguageCode() == LatinKeyboard.LANGUAGE_VN && predictionData != null) {
                mPredictions = predictionData.Predictions;
                mWordChoices = predictionData.WordChoices;
                if (mPredictions != null && mPredictions.size() > 0) {
                    getCurrentInputConnection().setComposingText(mPredictions.get(0), 1);
                    updateCandidates();
                }
            }
        }

        private String[][] predict(StringBuilder query) {
            String SERVERIP = "accentype.cloudapp.net";
//        String SERVERIP = "10.0.3.2";
            int SERVERPORT = 10100;

            try {
                InetAddress serverAddr = InetAddress.getByName(SERVERIP);
                DatagramSocket socket = new DatagramSocket();

                socket.setSoTimeout(500);

                byte[] buf = query.toString().getBytes("US-ASCII");
                DatagramPacket packet = new DatagramPacket(buf, buf.length, serverAddr, SERVERPORT);
                socket.send(packet);

                byte[] replyBuf = new byte[65536];
                DatagramPacket replyPacket = new DatagramPacket(replyBuf, replyBuf.length);
                socket.receive(replyPacket);

                int index = 0;
                byte numWords = replyBuf[index++];

                String[][] wordChoices = new String[numWords][];
                for (int i = 0; i < numWords; i++) {
                    byte numChoices = replyBuf[index++];
                    wordChoices[i] = new String[numChoices];
                    for (int j = 0; j < numChoices; j++) {
                        byte choiceByteLength = replyBuf[index++];
                        wordChoices[i][j] = new String(replyBuf, index, choiceByteLength, "UTF-8");
                        index += choiceByteLength;
                    }
                }
                socket.close();

                return wordChoices;
            }
            catch (Exception e) {
                // TODO: Handle exception
            }
            return null;
        }
    }

    private void addToLocalModel(String rawPhrase, String accentPhrase) {
        try {
            // TODO: for now skip input until model has been loaded
            if (mLocalModel != null && mLocalModelBinaryWriter != null) {
                String trimmedPhrase = rawPhrase.trim();
                String trimmedAccentPhrase = accentPhrase.trim();
                if (trimmedPhrase.length() == 0 || trimmedAccentPhrase.length() == 0) {
                    return;
                }
                int hashCode = trimmedPhrase.hashCode();
                HashMap<String, LocalModelItemData> hashEntryValue;
                LocalModelItemData localModelItem;
                if (mLocalModel.containsKey(hashCode)) {
                    hashEntryValue = mLocalModel.get(hashCode);
                } else {
                    hashEntryValue = new HashMap<>();
                }

                if (hashEntryValue.containsKey(trimmedAccentPhrase)) {
                    localModelItem = hashEntryValue.get(trimmedAccentPhrase);
                } else {
                    localModelItem = new LocalModelItemData();
                }
                localModelItem.count++;
                hashEntryValue.put(trimmedAccentPhrase, localModelItem);

                FileChannel fileChannel = mLocalModelOutputStream.getChannel();
                if (localModelItem.offset >= 0) {
                    fileChannel.position(localModelItem.offset);
                    mLocalModelBinaryWriter.writeInt(localModelItem.count);
                } else {
                    fileChannel.position(4);
                    mLocalModelBinaryWriter.writeInt(mLocalModel.size()); // update count

                    fileChannel.position(fileChannel.size()); // seek to end
                    mLocalModelBinaryWriter.writeInt(hashCode); // hash

                    byte[] accentBytes = trimmedAccentPhrase.getBytes("UTF-8"); // accent string
                    mLocalModelBinaryWriter.writeByte(accentBytes.length);
                    mLocalModelBinaryWriter.write(accentBytes);

                    // update offset in memory
                    localModelItem.offset = fileChannel.position();
                    mLocalModelBinaryWriter.writeInt(1); // count
                }

                // update model
                mLocalModel.put(hashCode, hashEntryValue);
            }
        }
        catch (IOException ex) { }
    }

    private String getFromLocalModel(String rawPhrase) {
        if (mLocalModel != null) {
            String trimmedPhrase = rawPhrase.trim();
            if (trimmedPhrase.length()  == 0) {
                return null;
            }
            int hashCode = trimmedPhrase.hashCode();
            if (mLocalModel.containsKey(hashCode)) {
                String topPrediction = null;

                HashMap<String, LocalModelItemData> hashEntryValue = mLocalModel.get(hashCode);

                int maxCount = -1;
                Iterator it = hashEntryValue.entrySet().iterator();
                while (it.hasNext()) {
                    Map.Entry pair = (Map.Entry)it.next();
                    LocalModelItemData localModelItemData = (LocalModelItemData)pair.getValue();
                    if (maxCount < localModelItemData.count) {
                        maxCount = localModelItemData.count;
                        topPrediction = (String)pair.getKey();
                    }
                }

                // pad prediction with whitespaces in the original phrase
                int i = 0;
                for (; i < rawPhrase.length(); i++) {
                    if (!Character.isWhitespace(rawPhrase.charAt(i))) {
                        break;
                    }
                }
                StringBuilder finalPrediction = new StringBuilder(rawPhrase);
                finalPrediction.replace(i, i + topPrediction.length(), topPrediction);

                return finalPrediction.toString();
            }
        }
        return null;
    }

    private class LocalModelLoader extends AsyncTask<Void, Void, HashMap<Integer, HashMap<String, LocalModelItemData>>> {
        private Context mContext;

        public LocalModelLoader(Context context) {
            mContext = context;
        }

        /** The system calls this to perform work in a worker thread and
         * delivers it the parameters given to AsyncTask.execute() */
        protected HashMap<Integer, HashMap<String, LocalModelItemData>> doInBackground(Void... params) {
            HashMap<Integer, HashMap<String, LocalModelItemData>> localModel = new HashMap<>();

            try
            {
                String localModelFileName = getString(R.string.model_file_name);
                File localModelFile = new File(mContext.getFilesDir(), localModelFileName);

                mLocalModelOutputStream = new FileOutputStream(localModelFile);
                mLocalModelBinaryWriter = new DataOutputStream(mLocalModelOutputStream);

                // if not exists, then write header information and return
                if (!localModelFile.exists()) {
                    mLocalModelBinaryWriter.writeInt(1); // model version
                    mLocalModelBinaryWriter.writeInt(0); // number of entries
                    return localModel;
                }

                CountingFileInputStream fileInputStream = new CountingFileInputStream(localModelFile);
                DataInputStream binaryReader = new DataInputStream(fileInputStream);

                try {
                    int modelVersion = binaryReader.readInt();
                    int numEntries = binaryReader.readInt();

                    for (int i = 0; i < numEntries; i++) {
                        // Read hash code
                        int hashCode = binaryReader.readInt();

                        // Read accent string
                        byte numUnicodeBytes = binaryReader.readByte();
                        byte[] unicodeBytes = new byte[numUnicodeBytes];
                        binaryReader.read(unicodeBytes);
                        String accentString = new String(unicodeBytes, "UTF-8");

                        HashMap<String, LocalModelItemData> hashEntryValue;
                        LocalModelItemData itemData;

                        if (localModel.containsKey(hashCode)) {
                            hashEntryValue = localModel.get(hashCode);
                        } else {
                            hashEntryValue = new HashMap<>();
                        }
                        if (hashEntryValue.containsKey(accentString)) {
                            itemData = hashEntryValue.get(accentString);
                        } else {
                            itemData = new LocalModelItemData();
                        }

                        // Set position to the count
                        itemData.offset = fileInputStream.getOffset();

                        // Read count
                        byte count = binaryReader.readByte();

                        itemData.count += count;

                        hashEntryValue.put(accentString, itemData);

                        localModel.put(hashCode, hashEntryValue);
                    }
                }
                finally {
                    binaryReader.close();
                }
            }
            catch (UnsupportedEncodingException ex) { }
            catch (IOException ex) { }

            return localModel;
        }

        /** The system calls this to perform work in the UI thread and delivers
         * the result from doInBackground() */
        protected void onPostExecute(HashMap<Integer, HashMap<String, LocalModelItemData>> localModelFile) {
            mLocalModel = localModelFile;
        }
    }

    private class PredictionData {
        public List<String> Predictions;
        public String[][] WordChoices;
    }

    private class LocalModelItemData {
        public LocalModelItemData() {
            offset = -1;
        }

        public byte count;
        public long offset;
    }
}