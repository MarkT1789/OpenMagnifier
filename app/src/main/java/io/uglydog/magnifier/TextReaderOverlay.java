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
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class TextReaderOverlay extends View {

    private static final float STROKE_WIDTH = 11.0f;
    private static final float TEXT_SIZE = 48.0f;
    private static final int OFFSET = 20;
    private final Rect mRect;
    private final Paint mBorderPaint;
    private final Paint mTextPaint;
    private final Handler mMainHandler;
    private final String mText;

    private boolean mShowCopyright;

    public TextReaderOverlay(@NonNull final Context context, @Nullable final AttributeSet attrs) {
        super(context, attrs);

        mBorderPaint = new Paint();
        mBorderPaint.setColor(Color.GREEN);
        mBorderPaint.setStyle(Paint.Style.STROKE);
        mBorderPaint.setStrokeWidth(STROKE_WIDTH);
        mBorderPaint.setAntiAlias(true);

        mTextPaint = new Paint();
        mTextPaint.setColor(Color.WHITE);
        mTextPaint.setTextSize(TEXT_SIZE);
        mTextPaint.setAntiAlias(true);

        mText = context.getString(R.string.translate_attribution);

        mRect = new Rect();
        mMainHandler = new Handler(Looper.getMainLooper());

        mShowCopyright = false;
    }

    public void clear() {
        set(null);
    }

    public void set(@Nullable final Rect rect) {
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

    @Override
    protected void onDraw(@NonNull final Canvas canvas) {
        super.onDraw(canvas);

        if (!mRect.isEmpty()) {
            canvas.drawRect(mRect, mBorderPaint);
        }
        drawCopyright(canvas);
    }

    @Override
    protected void onDetachedFromWindow() {
        mMainHandler.removeCallbacksAndMessages(null);
        super.onDetachedFromWindow();
    }

    public void showCopyright(final boolean enable) {
        mShowCopyright = enable;
    }

    private void drawCopyright(final Canvas canvas) {

        if (!mShowCopyright) {
            return;
        }

        final Rect bounds = new Rect();
        mTextPaint.getTextBounds(mText, 0, mText.length(), bounds);
        final int textHeight = bounds.height();

        final int marginLeft = OFFSET * 2;
        final int marginBottom = OFFSET;

        final float x = marginLeft;
        final float y = canvas.getHeight() - marginBottom - bounds.bottom;

        canvas.drawText(mText, x, y, mTextPaint);
    }
}
