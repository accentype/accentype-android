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
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class CandidateView extends View {

    private static final int OUT_OF_BOUNDS = -1;

    private SoftKeyboard mService;
    private String mComposing;
    private List<String> mSuggestions;
    private List<String> mSecondarySuggestions;
    private int mFlingSuggestionIndex = -1;
    private int mFlingWordIndex = -1;
    private String[][] mWordChoices;
    private HashMap<String, String[]> mDictionary;
    private int mSelectedIndex;
    private int mTouchX = OUT_OF_BOUNDS;
    private Drawable mSelectionHighlight;
    private boolean mTypedWordValid;
    
    private Rect mBgPadding;

    private static final int MAX_SUGGESTIONS = 40;
    private static final int SCROLL_PIXELS = 20;
    
    private int[] mWordWidth = new int[MAX_SUGGESTIONS];
    private int[] mWordX = new int[MAX_SUGGESTIONS];

    private static final int X_GAP = 20;
    
    private static final List<String> EMPTY_LIST = new ArrayList<String>();

    private int mColorNormal;
    private int mColorRecommended;
    private int mColorOther;
    private int mVerticalPadding;
    private Paint mPaint;
    private boolean mScrolled;
    private int mTargetScrollX;
    
    private int mTotalWidth;
    
    private GestureDetector mGestureDetector;

    /**
     * Construct a CandidateView for showing suggested words for completion.
     * @param context
     * @param attrs
     */
    public CandidateView(Context context) {
        super(context);
        mSelectionHighlight = context.getResources().getDrawable(
                android.R.drawable.list_selector_background);
        mSelectionHighlight.setState(new int[] {
                android.R.attr.state_enabled,
                android.R.attr.state_focused,
                android.R.attr.state_window_focused,
                android.R.attr.state_pressed
        });

        Resources r = context.getResources();
        
        setBackgroundColor(r.getColor(R.color.candidate_background));
        
        mColorNormal = r.getColor(R.color.candidate_normal);
        mColorRecommended = r.getColor(R.color.candidate_recommended);
        mColorOther = r.getColor(R.color.candidate_other);
        mVerticalPadding = r.getDimensionPixelSize(R.dimen.candidate_vertical_padding);
        
        mPaint = new Paint();
        mPaint.setColor(mColorNormal);
        mPaint.setAntiAlias(true);
        mPaint.setTextSize(r.getDimensionPixelSize(R.dimen.candidate_font_height));
        mPaint.setStrokeWidth(0);

        new DictionaryLoader().execute();

        mGestureDetector = new GestureDetector(new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onScroll(MotionEvent e1, MotionEvent e2,
                    float distanceX, float distanceY) {
                mScrolled = true;

                int sx = (int)Math.max(0, Math.min((float)mTotalWidth - getWidth(), getScrollX() + distanceX));

                mTargetScrollX = sx;
                scrollTo(sx, getScrollY());
                invalidate();
                return true;
            }

            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY)
            {
                // Already displaying secondary suggestions so flinging should do nothing
                if (mSecondarySuggestions != null) {
                    return false;
                }
                if (velocityY > 100 && mWordChoices != null)
                {
                    TouchLocation touchLocation = getWordIndexBasedOnTouchPosition(e1.getX() + getScrollX());

                    int iWord = touchLocation.WordIndex;
                    if (iWord < 0) {
                        return false;
                    }
                    String[] additionalChoices = null;
                    if (mDictionary != null && mComposing != null) {
                        String[] rawWords = mComposing.trim().split("\\s+");
                        if (iWord < rawWords.length) {
                            String rawWord = rawWords[iWord];
                            String rawWordLower = rawWord.toLowerCase();
                            if (mDictionary.containsKey(rawWordLower)) {
                                List<Integer> upperCaseLocations = new ArrayList<>();
                                for (int i = 0; i < rawWord.length(); i++) {
                                    if (Character.isUpperCase(rawWord.charAt(i))) {
                                        upperCaseLocations.add(i);
                                    }
                                }
                                // Normalize case w.r.t raw word
                                String[] dictionaryChoices = mDictionary.get(rawWordLower);
                                additionalChoices = new String[dictionaryChoices.length];
                                for (int i = 0; i < dictionaryChoices.length; i++) {
                                    char[] choiceChars = dictionaryChoices[i].toCharArray();
                                    for (int j = 0; j < upperCaseLocations.size(); j++) {
                                        choiceChars[j] = Character.toUpperCase(choiceChars[j]);
                                    }
                                    additionalChoices[i] = new String(choiceChars);
                                }
                            }
                        }
                    }
                    mSecondarySuggestions = new ArrayList<>(Arrays.asList(mWordChoices[iWord]));
                    if (additionalChoices != null) {
                        for (int k = 0; k < additionalChoices.length; k++) {
                            if (!mSecondarySuggestions.contains(additionalChoices[k]))
                            {
                                mSecondarySuggestions.add(additionalChoices[k]);
                            }
                        }
                    }

                    mFlingSuggestionIndex = touchLocation.SuggestionIndex;
                    mFlingWordIndex = iWord;

                    scrollTo(0, 0);
                    mTargetScrollX = 0;
                    // Compute the total width
                    internalDraw(null);
                    invalidate();
                    requestLayout();

                    return true;
                }
                return false;
            }
        });
        setHorizontalFadingEdgeEnabled(true);
        setWillNotDraw(false);
        setHorizontalScrollBarEnabled(false);
        setVerticalScrollBarEnabled(false);
    }
    
    /**
     * A connection back to the service to communicate with the text field
     * @param listener
     */
    public void setService(SoftKeyboard listener) {
        mService = listener;
    }
    
    @Override
    public int computeHorizontalScrollRange() {
        return mTotalWidth;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int measuredWidth = resolveSize(50, widthMeasureSpec);
        
        // Get the desired height of the icon menu view (last row of items does
        // not have a divider below)
        Rect padding = new Rect();
        mSelectionHighlight.getPadding(padding);
        final int desiredHeight = ((int)mPaint.getTextSize()) + mVerticalPadding
                + padding.top + padding.bottom;
        
        // Maximum possible width and desired height
        setMeasuredDimension(measuredWidth,
                resolveSize(desiredHeight, heightMeasureSpec));
    }

    /**
     * If the canvas is null, then only touch calculations are performed to pick the target
     * candidate.
     */
    @Override
    protected void onDraw(Canvas canvas) {
        if (canvas != null) {
            super.onDraw(canvas);
        }
        this.internalDraw(canvas);
    }

    private void internalDraw(Canvas canvas) {
        mTotalWidth = 0;
        if (mSuggestions == null && mSecondarySuggestions == null) return;
        
        if (mBgPadding == null) {
            mBgPadding = new Rect(0, 0, 0, 0);
            if (getBackground() != null) {
                getBackground().getPadding(mBgPadding);
            }
        }
        int x = 0;
        List<String> suggestions = mSecondarySuggestions != null ? mSecondarySuggestions : mSuggestions;
        final int count = Math.min(MAX_SUGGESTIONS, suggestions.size());
        final int height = getHeight();
        final Rect bgPadding = mBgPadding;
        final Paint paint = mPaint;
        final int touchX = mTouchX;
        final int scrollX = getScrollX();
        final boolean scrolled = mScrolled;
        final boolean typedWordValid = mTypedWordValid;
        final int y = (int) (((height - mPaint.getTextSize()) / 2) - mPaint.ascent());

        for (int i = 0; i < count; i++) {
            String suggestion = suggestions.get(i);
            float textWidth = paint.measureText(suggestion);
            final int wordWidth = (int) textWidth + X_GAP * 2;

            mWordX[i] = x;
            mWordWidth[i] = wordWidth;
            paint.setColor(mColorNormal);
            if (touchX + scrollX >= x && touchX + scrollX < x + wordWidth && !scrolled) {
                if (canvas != null) {
                    canvas.translate(x, 0);
                    mSelectionHighlight.setBounds(0, bgPadding.top, wordWidth, height);
                    mSelectionHighlight.draw(canvas);
                    canvas.translate(-x, 0);
                }
                mSelectedIndex = i;
            }

            if (canvas != null) {
                if ((i == 1 && !typedWordValid) || (i == 0 && typedWordValid)) {
                    paint.setFakeBoldText(true);
                    paint.setColor(mColorRecommended);
                } else if (i != 0) {
                    paint.setColor(mColorOther);
                }
                canvas.drawText(suggestion, x + X_GAP, y, paint);
                paint.setColor(mColorOther); 
                canvas.drawLine(x + wordWidth + 0.5f, bgPadding.top, 
                        x + wordWidth + 0.5f, height + 1, paint);
                paint.setFakeBoldText(false);
            }
            x += wordWidth;
        }
        mTotalWidth = x;
        if (mTargetScrollX != getScrollX()) {
            scrollToTarget();
        }
    }
    
    private void scrollToTarget() {
        int sx = getScrollX();
        if (mTargetScrollX > sx) {
            sx += SCROLL_PIXELS;
            if (sx >= mTargetScrollX) {
                sx = mTargetScrollX;
                requestLayout();
            }
        } else {
            sx -= SCROLL_PIXELS;
            if (sx <= mTargetScrollX) {
                sx = mTargetScrollX;
                requestLayout();
            }
        }
        scrollTo(sx, getScrollY());
        invalidate();
    }
    
    public void setSuggestions(List<String> suggestions, String[][] wordChoices, String composing, boolean completions,
            boolean typedWordValid) {
        clear();
        if (suggestions != null) {
            mSuggestions = new ArrayList<>(suggestions);
            mWordChoices = wordChoices;
            mComposing = composing;
        }
        mTypedWordValid = typedWordValid;
        scrollTo(0, 0);
        mTargetScrollX = 0;
        // Compute the total width
        internalDraw(null);
        invalidate();
        requestLayout();
    }

    public List<String> getSuggestions()
    {
        return mSuggestions;
    }

    public void clear() {
        mSuggestions = EMPTY_LIST;
        mSecondarySuggestions = null;
        mWordChoices = null;
        mComposing = null;
        mTouchX = OUT_OF_BOUNDS;
        mSelectedIndex = -1;
        mFlingSuggestionIndex = -1;
        mFlingWordIndex = -1;
        invalidate();
    }
    
    @Override
    public boolean onTouchEvent(MotionEvent me) {

        if (mGestureDetector.onTouchEvent(me)) {
            return true;
        }

        int action = me.getAction();
        int x = (int) me.getX();
        int y = (int) me.getY();
        mTouchX = x;

        switch (action) {
        case MotionEvent.ACTION_DOWN:
            mScrolled = false;
            invalidate();
            break;
        case MotionEvent.ACTION_MOVE:
            if (y <= 0) {
                // Fling up!?
                if (mSelectedIndex >= 0) {
                    if (mSecondarySuggestions != null) {
                        pickSecondarySuggestionsManually(mSelectedIndex);
                    }
                    else {
                        mService.pickSuggestionManually(mSelectedIndex);
                    }
                    mSelectedIndex = -1;
                }
            }
            invalidate();
            break;
        case MotionEvent.ACTION_UP:
            if (!mScrolled) {
                if (mSelectedIndex >= 0) {
                    if (mSecondarySuggestions != null) {
                        pickSecondarySuggestionsManually(mSelectedIndex);
                    }
                    else {
                        mService.pickSuggestionManually(mSelectedIndex);
                    }
                }
            }
            mSelectedIndex = -1;
            removeHighlight();
            requestLayout();
            break;
        }
        return true;
    }
    
    /**
     * For flick through from keyboard, call this method with the x coordinate of the flick 
     * gesture.
     * @param x
     */
    public void takeSuggestionAt(float x) {
        mTouchX = (int) x;
        // To detect candidate
        internalDraw(null);
        if (mSelectedIndex >= 0) {
            mService.pickSuggestionManually(mSelectedIndex);
        }
        invalidate();
    }

    private void pickSecondarySuggestionsManually(int index) {
        if (mSuggestions == null ||
            mSuggestions.size() == 0 ||
            mFlingSuggestionIndex < 0 ||
            mFlingSuggestionIndex >= mSuggestions.size()) {
            return;
        }

        String chosenWord = mSecondarySuggestions.get(index);

        StringBuilder suggestion = new StringBuilder(mSuggestions.get(mFlingSuggestionIndex));

        boolean isWordCounted = false;
        int iWord = -1;
        for (int j = 0; j < suggestion.length(); j++) {
            if (!Character.isWhitespace(suggestion.charAt(j))) {
                if (!isWordCounted) {
                    iWord++;
                    isWordCounted = true;

                    if (iWord == mFlingWordIndex) {
                        suggestion.replace(j, j + chosenWord.length(), chosenWord);
                        mSuggestions.set(0, suggestion.toString()); // update the first suggestion
                        break;
                    }
                }
            } else {
                isWordCounted = false;
            }
        }

        mService.updateComposingText(suggestion.toString());

        mSecondarySuggestions = null;

        scrollTo(0, 0);
        mTargetScrollX = 0;
        // Compute the total width
        internalDraw(null);
        invalidate();
        requestLayout();
    }

    private void removeHighlight() {
        mTouchX = OUT_OF_BOUNDS;
        invalidate();
    }

    private TouchLocation getWordIndexBasedOnTouchPosition(float touchX) {
        TouchLocation touchLocation = new TouchLocation();
        touchLocation.WordIndex = -1;
        touchLocation.SuggestionIndex = -1;

        for (int i = 0; i < mSuggestions.size(); i++) {
            if (touchX >= mWordX[i] && touchX <= mWordX[i] + mWordWidth[i]) {
                touchLocation.SuggestionIndex = i;

                float offsetX = touchX - mWordX[i] - X_GAP;
                String suggestion = mSuggestions.get(i);
                boolean isWordCounted = false;
                for (int j = 0; j < suggestion.length(); j++) {
                    if (!Character.isWhitespace(suggestion.charAt(j))) {
                        if (!isWordCounted)
                        {
                            touchLocation.WordIndex++;
                            isWordCounted = true;
                        }
                    }
                    else {
                        isWordCounted = false;
                    }

                    float charWidth = mPaint.measureText(suggestion, j, j + 1);
                    if (offsetX <= charWidth)
                    {
                        return touchLocation;
                    }
                    offsetX -= charWidth;
                }
                break;
            }
        }
        return touchLocation;
    }

    private class TouchLocation {
        public int WordIndex;
        public int SuggestionIndex;
    }

    private class DictionaryLoader extends AsyncTask<Void, Void, HashMap<String, String[]>> {
        /** The system calls this to perform work in a worker thread and
         * delivers it the parameters given to AsyncTask.execute() */
        protected HashMap<String, String[]> doInBackground(Void... params) {
            try
            {
                InputStream accStream = getResources().openRawResource(R.raw.dictionary);
                BufferedReader accReader = new BufferedReader(new InputStreamReader(accStream, "UTF-8"));
                HashMap<String, String[]> dictionary = new HashMap<>();
                String line;
                while ((line = accReader.readLine()) != null) {
                    String[] words = line.split("\\s+");
                    dictionary.put(words[0], words);
                }
                return dictionary;
            }
            catch (UnsupportedEncodingException ex) { }
            catch (IOException ex) { }

            return null;
        }

        /** The system calls this to perform work in the UI thread and delivers
         * the result from doInBackground() */
        protected void onPostExecute(HashMap<String, String[]> dictionary) {
            mDictionary = dictionary;
        }
    }
}
