/*
 * Copyright (C) 2026  Mark Tamura
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package io.uglydog.magnifier;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.text.BreakIterator;

public class TextReaderOverlay extends View implements Handler.Callback {

    private static final String TAG = TextReaderOverlay.class.getSimpleName();
    private static final float STROKE_WIDTH = 11.0f;
    private static final float TEXT_SIZE = 23.0f;
    private static final int OFFSET = 40;
    private static final int MSG_CLEAR_BACKGROUND = 1;
    private static final int CLEAR_BACKGROUND_TIMER = 2000;

    private final Rect mRect;
    private final Paint mBorderPaint;
    private final Paint mTextPaint;
    private final Paint mCopyrightPaint;
    private final Paint mBackgroundPaint;
    private final Handler mMainHandler;
    private final String mText;
    private final float[] mWidth;
    private final BreakIterator mBreakIterator;

    private SettingsProvider mSettingsProvider;

    private int mBackgroundHeight;

    private String mTts;
    private int mCount;
    private int mStart;

    private boolean mShowCopyright;
    private boolean mShowBackground;

    public TextReaderOverlay(@NonNull final Context context, @Nullable final AttributeSet attrs) {
        super(context, attrs);

        mBorderPaint = new Paint();
        mBorderPaint.setColor(Color.GREEN);
        mBorderPaint.setStyle(Paint.Style.STROKE);
        mBorderPaint.setStrokeWidth(STROKE_WIDTH);
        mBorderPaint.setAntiAlias(true);

        mTextPaint = new Paint();
        mTextPaint.setColor(Color.WHITE);
        mTextPaint.setAntiAlias(true);

        mCopyrightPaint = new Paint();
        mCopyrightPaint.setColor(Color.WHITE);
        mCopyrightPaint.setAntiAlias(true);
        mCopyrightPaint.setTextSize(64f);

        mBackgroundPaint = new Paint();
        mBackgroundPaint.setColor(Color.BLACK);
        mBackgroundPaint.setStyle(Paint.Style.FILL);

        mText = context.getString(R.string.translate_attribution);

        mRect = new Rect();
        mMainHandler = new Handler(Looper.getMainLooper(), this);

        mShowCopyright = false;
        mShowBackground = false;


        mWidth = new float[1];
        mBreakIterator = BreakIterator.getWordInstance();

        updateTextSize();
    }

    public void setSettingsProvider(SettingsProvider settingsProvider) {
        mSettingsProvider = settingsProvider;
        updateTextSize();
    }

    public void clear() {
        setRect(null);
        setText(null, -1, -1);
        if (!mMainHandler.hasMessages(MSG_CLEAR_BACKGROUND)) {
            mMainHandler.sendEmptyMessageDelayed(MSG_CLEAR_BACKGROUND, CLEAR_BACKGROUND_TIMER);
        }
    }

    public void setRect(@Nullable final Rect rect) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            updateRect(rect);
        } else {
            final Rect copy = (rect == null) ? null : new Rect(rect);
            mMainHandler.post(new Runnable() {
                @Override
                public void run() {
                    updateRect(copy);
                }
            });
        }
    }

    public void setText(String text, int start, int end) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            updateText(text, start, end);
        } else {
            mMainHandler.post(new Runnable() {
                @Override
                public void run() {
                    updateText(text, start, end);
                }
            });
        }
    }

    private void updateRect(@Nullable final Rect rect) {
        if (!isAttachedToWindow()) {
            return;
        }

        if (rect == null) {
            mRect.setEmpty();
        } else {
            mRect.set(rect);
        }
       invalidate();
    }

    private void updateText(String text, int start, int end) {
        if (!isAttachedToWindow()) {
            return;
        }

        if (text != null) {
            mShowBackground = true;
            mMainHandler.removeMessages(MSG_CLEAR_BACKGROUND);
        }

        if (text != null && mTts != null) {
            if (mTts.equals(text)) {
                if (mCount != 0) {
                    int count = end - mStart;
                    if (count > mCount || start < mStart) {
                        mStart = start;
                    }
                }
            } else {
                mTts = text;
                mStart = 0;
            }
        } else if (text != null || mTts != null) {
            mTts = text;
            mStart = 0;
        }

        invalidate();
    }

    @Override
    protected void onDraw(@NonNull final Canvas canvas) {
        super.onDraw(canvas);

        if (!mRect.isEmpty()) {
            canvas.drawRect(mRect, mBorderPaint);
        }
        drawCopyright(canvas);
        drawText(canvas);
    }

    @Override
    protected void onDetachedFromWindow() {
        close();
        super.onDetachedFromWindow();
    }

    public void showCopyright(final boolean enable) {
        mShowCopyright = enable;
    }

    private void drawCopyright(final Canvas canvas) {
        if (mShowCopyright) {
            canvas.drawText(mText, OFFSET * 2, canvas.getHeight() - OFFSET, mCopyrightPaint);
        }
    }

    private void drawText(final Canvas canvas) {
        if (mShowBackground) {
            canvas.drawRect(0, 0, getWidth(), mBackgroundHeight, mBackgroundPaint);
        }
        if (mTts != null) {
            final String str = mTts.substring(mStart);

            mCount = mTextPaint.breakText(str, true, getWidth() - OFFSET, mWidth);

            if (mCount != str.length()) {
                mBreakIterator.setText(str);
                if (!mBreakIterator.isBoundary(mCount)) {
                    final int precedingBoundary = mBreakIterator.preceding(mCount);
                    if (precedingBoundary != 0 && precedingBoundary != BreakIterator.DONE) {
                        mCount = precedingBoundary;
                    }
                }
            }

            canvas.drawText(str, 0, mCount, OFFSET, mBackgroundHeight - OFFSET, mTextPaint);
        }
    }

    @Override
    public boolean handleMessage(@NonNull final Message msg) {
        switch (msg.what) {
            case MSG_CLEAR_BACKGROUND:
                if (mShowBackground) {
                    mShowBackground = false;
                    invalidate();
                }
                return true;
        }
        return false;
    }

    public void close() {
        mMainHandler.removeCallbacksAndMessages(null);
    }

    public void clearOverlay() {
        mShowBackground = false;
        setRect(null);
        setText(null, -1, -1);
        invalidate();
    }

    @Override
    protected void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        updateTextSize();
        invalidate();
    }

    public void updateTextSize() {
        final float banner_size = mSettingsProvider != null ? mSettingsProvider.getBannerSize() : 1.0f;
        final float pxSize = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_SP,
                TEXT_SIZE * banner_size,
                getContext().getResources().getDisplayMetrics()
        );
        mTextPaint.setTextSize(pxSize);

        final Paint.FontMetrics metrics = mTextPaint.getFontMetrics();
        final int textHeight = (int)(metrics.descent - metrics.ascent);
        mBackgroundHeight = textHeight + OFFSET * 2;
    }
}
