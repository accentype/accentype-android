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
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.inputmethodservice.Keyboard;
import android.inputmethodservice.Keyboard.Key;
import android.inputmethodservice.KeyboardView;
import android.util.AttributeSet;
import android.view.MotionEvent;

import java.util.List;

public class LatinKeyboardView extends KeyboardView {

    static final int KEYCODE_OPTIONS = -100;
    // TODO: Move this into android.inputmethodservice.Keyboard
    static final int KEYCODE_LANGUAGE_SWITCH = -101;
    static final int KEYCODE_INPUT_METHOD_SWITCH = -102;
    static final int KEYCODE_EMOJI = -103;

    static Paint foregroundPaint = new Paint();
    static final int spaceKeyMargin = 10;

    boolean mLastPredictionEnabled = false;
    boolean mLongPressedDelete = false;

    public LatinKeyboardView(Context context, AttributeSet attrs) {
        super(context, attrs);
        foregroundPaint.setColor(Color.WHITE);
    }

    public LatinKeyboardView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        foregroundPaint.setColor(Color.WHITE);
    }

    @Override
    protected boolean onLongPress(Key key) {
        if (key.codes[0] == Keyboard.KEYCODE_CANCEL) {
            getOnKeyboardActionListener().onKey(KEYCODE_OPTIONS, null);
            return true;
        } else if (key.codes[0] == KEYCODE_LANGUAGE_SWITCH) {
            getOnKeyboardActionListener().onKey(KEYCODE_INPUT_METHOD_SWITCH, null);
            return true;
        } else if (key.codes[0] == Keyboard.KEYCODE_DELETE) {
            // turn off server predictions during long pressing backspace key
            mLastPredictionEnabled = ((SoftKeyboard)getOnKeyboardActionListener()).turnOffPredictionsIfNeeded();
            mLongPressedDelete = true;
            return false;
        } else {
            return super.onLongPress(key);
        }
    }

    @Override
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        List<Key> keys = getKeyboard().getKeys();
        for (Key key : keys) {
            if (key.codes[0] == 32) {
                canvas.drawRect(
                    key.x + spaceKeyMargin,
                    key.y + key.height / 2,
                    key.x + key.width - spaceKeyMargin,
                    key.y + key.height - key.height / 4,
                    foregroundPaint);
                return;
            }
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent me) {
        switch (me.getAction()) {
            case MotionEvent.ACTION_UP:
                int x = (int) me.getX();
                int y = (int) me.getY();
                List<Key> keys = getKeyboard().getKeys();
                for (Key key : keys) {
                    if (key.codes[0] == Keyboard.KEYCODE_DELETE) {
                        if (mLongPressedDelete && x >= key.x && x <= key.x + key.width && y >= key.y && y <= key.y + key.height) {
                            // Restore server predictions on touch-up event
                            // because if backspace is long pressed, predictions are disabled.
                            ((SoftKeyboard)getOnKeyboardActionListener()).enablePredictions(mLastPredictionEnabled);
                            mLongPressedDelete = false;
                        }
                        break;
                    }
                }
                break;
        }
        return super.onTouchEvent(me);
    }
}
