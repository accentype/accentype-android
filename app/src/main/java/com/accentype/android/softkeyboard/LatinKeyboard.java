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

import android.content.Context;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.graphics.drawable.Drawable;
import android.inputmethodservice.Keyboard;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;

import java.util.List;

public class LatinKeyboard extends Keyboard {

    private Key mEnterKey;
    private Key mSpaceKey;
    /**
     * Stores the current state of the mode change key. Its width will be dynamically updated to
     * match the region of {@link #mModeChangeKey} when {@link #mModeChangeKey} becomes invisible.
     */
    private Key mModeChangeKey;
    /**
     * Stores the current state of the cancel key.
     * When this key becomes invisible, its width will be shrunk to zero.
     */
    private Key mCancelKey;
    /**
     * Stores the current state of the comma key.
     */
    private Key mCommaKey;
    /**
     * Stores the current state of the language switch key (a.k.a. globe key). This should be
     * visible while {@link InputMethodManager#shouldOfferSwitchingToNextInputMethod(IBinder)}
     * returns true. When this key becomes invisible, its width will be shrunk to zero.
     */
    private Key mLanguageSwitchKey;
    /**
     * Stores the size and other information of {@link #mModeChangeKey} when
     * {@link #mCancelKey} is visible. This should be immutable and will be used only as a
     * reference size when the visibility of {@link #mCancelKey} is changed.
     */
    private Key mSavedModeChangeKey;
    /**
     * Stores the size and other information of {@link #mSpaceKey} when
     * {@link #mLanguageSwitchKey} is visible. This should be immutable and will be used only as a
     * reference size when the visibility of {@link #mLanguageSwitchKey} is changed.
     */
    private Key mSavedSpaceKey;
    /**
     * Stores the size and other information of {@link #mCancelKey} when it is visible.
     * This should be immutable and will be used only as a reference size when the visibility of
     * {@link #mCancelKey} is changed.
     */
    private Key mSavedLanguageSwitchKey;

    /**
     * Stores the size and other information of {@link #mLanguageSwitchKey} when it is visible.
     * This should be immutable and will be used only as a reference size when the visibility of
     * {@link #mLanguageSwitchKey} is changed.
     */
    private Key mSavedCancelKey;

    /**
     * Stores the current state of the comma key. This should be immutable and will be used only as a
     * reference size when the visibility of {@link #mCancelKey} is changed.
     */
    private Key mSavedCommaKey;

    static final int LANGUAGE_VN = 0;
    static final int LANGUAGE_EN = 1;

    public LatinKeyboard(Context context, int xmlLayoutResId) {
        super(context, xmlLayoutResId);
    }

    public LatinKeyboard(Context context, int layoutTemplateResId, 
            CharSequence characters, int columns, int horizontalPadding) {
        super(context, layoutTemplateResId, characters, columns, horizontalPadding);
    }

    @Override
    protected Key createKeyFromXml(Resources res, Row parent, int x, int y, 
            XmlResourceParser parser) {
        Key key = new LatinKey(res, parent, x, y, parser);
        if (key.codes[0] == 10) {
            mEnterKey = key;
        } else if (key.codes[0] == ' ') {
            mSpaceKey = key;
            mSavedSpaceKey = new LatinKey(res, parent, x, y, parser);
        } else if (key.codes[0] == Keyboard.KEYCODE_MODE_CHANGE) {
            mModeChangeKey = key;
            mSavedModeChangeKey = new LatinKey(res, parent, x, y, parser);
        } else if (key.codes[0] == LatinKeyboardView.KEYCODE_LANGUAGE_SWITCH) {
            mLanguageSwitchKey = key;
            mSavedLanguageSwitchKey = new LatinKey(res, parent, x, y, parser);
        } else if (key.codes[0] == Keyboard.KEYCODE_CANCEL) {
            mCancelKey = key;
            mSavedCancelKey = new LatinKey(res, parent, x, y, parser);
        } else if (key.codes[0] == 44) { // comma
            mCommaKey = key;
            mSavedCommaKey = new LatinKey(res, parent, x, y, parser);
        }
        return key;
    }

    @Override
    public int[] getNearestKeys(int x, int y) {
        List<Key> keys = getKeys();
        Key[] mKeys = keys.toArray(new Key[keys.size()]);
        int i = 0;
        for (Key key : mKeys) {
            if(key.isInside(x, y))
                return new int[]{i};
            i++;
        }
        return new int[0];
    }

    /**
     * Dynamically change the visibility of the language switch key (a.k.a. globe key).
     * @param visible True if the language switch key should be visible.
     */
    void setLanguageSwitchKeyVisibility(boolean visible) {
        updateKeyLayout(mCancelKey.width > 0, visible);
    }

    /**
     * Dynamically change the visibility of the cancel key.
     * @param visible True if the cancel key should be visible.
     */
    void setCancelKeyVisibility(boolean visible) {
        updateKeyLayout(visible, mLanguageSwitchKey.width > 0);
    }

    void setLanguageLabel(String languageMode) {
        mLanguageSwitchKey.label = languageMode;
    }

    void updateKeyLayout(boolean isCancelKeyVisible, boolean isLanguageSwitchKeyVisible)
    {
        if (isCancelKeyVisible) {
            // The cancel key should be visible. Restore the size of the space key
            // and language switch key and mode change key using the saved layout.
            mModeChangeKey.x = mSavedModeChangeKey.x;
            if (!isLanguageSwitchKeyVisible) { // [Cancel] [123] [,] [_____] [.]
                mLanguageSwitchKey.width = 0;
                mLanguageSwitchKey.icon = null;
                mLanguageSwitchKey.iconPreview = null;
                mSpaceKey.width = mSavedSpaceKey.width + mSavedLanguageSwitchKey.width;
                mSpaceKey.x = mSavedSpaceKey.x - mSavedLanguageSwitchKey.width;
                mCommaKey.x = mSavedCancelKey.width + mSavedModeChangeKey.width;
            }
            else { // [Cancel] [123] [VN] [,] [_____] [.]
                mLanguageSwitchKey.x = mSavedLanguageSwitchKey.x;
                mLanguageSwitchKey.width = mSavedLanguageSwitchKey.width;
                mLanguageSwitchKey.icon = mSavedLanguageSwitchKey.icon;
                mLanguageSwitchKey.iconPreview = mSavedLanguageSwitchKey.iconPreview;
                mSpaceKey.width = mSavedSpaceKey.width;
                mSpaceKey.x = mSavedSpaceKey.x;
                mCommaKey.x = mSavedCommaKey.x;
            }
            mCancelKey.width = mSavedCancelKey.width;
            mCancelKey.x = mSavedCancelKey.x;
            mCancelKey.icon = mSavedCancelKey.icon;
            mCancelKey.iconPreview = mSavedCancelKey.iconPreview;
        } else {
            // The cancel key should be hidden. Change the width of the space key
            // to fill the space of the cancel key so that the user will not see any strange gap.
            mModeChangeKey.x = mSavedModeChangeKey.x - mSavedCancelKey.width;
            if (!isLanguageSwitchKeyVisible) { // [123] [,] [_____] [.]
                mLanguageSwitchKey.width = 0;
                mLanguageSwitchKey.icon = null;
                mLanguageSwitchKey.iconPreview = null;
                mSpaceKey.width = mSavedSpaceKey.width + mSavedCancelKey.width + mSavedLanguageSwitchKey.width;
                mSpaceKey.x = mSavedSpaceKey.x - mSavedCancelKey.width - mSavedLanguageSwitchKey.width;
                mCommaKey.x = mSavedModeChangeKey.width;
            }
            else { // [123] [VN] [,] [_____] [.]
                mLanguageSwitchKey.x = mSavedLanguageSwitchKey.x - mSavedCancelKey.width;
                mLanguageSwitchKey.width = mSavedLanguageSwitchKey.width;
                mLanguageSwitchKey.icon = mSavedLanguageSwitchKey.icon;
                mLanguageSwitchKey.iconPreview = mSavedLanguageSwitchKey.iconPreview;
                mSpaceKey.width = mSavedSpaceKey.width + mSavedCancelKey.width;
                mSpaceKey.x = mSavedSpaceKey.x - mSavedCancelKey.width;
                mCommaKey.x = mSavedModeChangeKey.width + mSavedLanguageSwitchKey.width;
            }
            mCancelKey.width = 0;
            mCancelKey.icon = null;
            mCancelKey.iconPreview = null;
        }
    }

    /**
     * This looks at the ime options given by the current editor, to set the
     * appropriate label on the keyboard's enter key (if it has one).
     */
    void setImeOptions(Resources res, int options) {
        if (mEnterKey == null) {
            return;
        }

        switch (options&(EditorInfo.IME_MASK_ACTION|EditorInfo.IME_FLAG_NO_ENTER_ACTION)) {
            case EditorInfo.IME_ACTION_GO:
                mEnterKey.iconPreview = null;
                mEnterKey.icon = null;
                mEnterKey.label = res.getText(R.string.label_go_key);
                break;
            case EditorInfo.IME_ACTION_NEXT:
                mEnterKey.iconPreview = null;
                mEnterKey.icon = null;
                mEnterKey.label = res.getText(R.string.label_next_key);
                break;
            case EditorInfo.IME_ACTION_SEARCH:
                mEnterKey.icon = res.getDrawable(R.drawable.sym_keyboard_search_holo_dark);
                mEnterKey.label = null;
                break;
            case EditorInfo.IME_ACTION_SEND:
                mEnterKey.iconPreview = null;
                mEnterKey.icon = null;
                mEnterKey.label = res.getText(R.string.label_send_key);
                break;
            default:
                mEnterKey.icon = res.getDrawable(R.drawable.sym_keyboard_return_holo_dark);
                mEnterKey.label = null;
                break;
        }
    }

    void setSpaceIcon(final Drawable icon) {
        if (mSpaceKey != null) {
            mSpaceKey.icon = icon;
        }
    }

    static class LatinKey extends Keyboard.Key {
        
        public LatinKey(Resources res, Keyboard.Row parent, int x, int y,
                XmlResourceParser parser) {
            super(res, parent, x, y, parser);
        }
        
        /**
         * Overriding this method so that we can reduce the target area for the key that
         * closes the keyboard. 
         */
        @Override
        public boolean isInside(int x, int y) {
            return super.isInside(x, codes[0] == KEYCODE_CANCEL ? y - 10 : y);
        }
    }

}
