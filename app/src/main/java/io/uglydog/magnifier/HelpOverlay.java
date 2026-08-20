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
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class HelpOverlay extends View implements Handler.Callback {

    private final String mTextLive;
    private final String mTextFrozen;
    private final TextPaint mTextPaint;
    private final Handler mHandler;

    private String mText;
    private StaticLayout mStaticLayout;
    private int mWidth;

    public final static int MODE_NONE = 0;
    public final static int MODE_LIVE = 1;
    public final static int MODE_FROZEN = 2;

    public final static int MSG_CLEAR_BACKGROUND = 1;
    public final static int CLEAR_BACKGROUND_TIMER = 15000;

    public HelpOverlay(@NonNull final Context context, @Nullable final AttributeSet attrs) {
        super(context, attrs);

        mTextLive = context.getString(R.string.live_view_help);
        mTextFrozen = context.getString(R.string.frozen_view_help);

        mTextPaint = new TextPaint();
        mTextPaint.setAntiAlias(true);
        mTextPaint.setColor(Color.WHITE);
        mTextPaint.setTextSize(48f);

        mHandler = new Handler(Looper.getMainLooper(), this);
        setText(MODE_NONE);
    }

    public void setText(final int mode) {
        switch(mode) {
            case MODE_NONE:
                mText = "";
            break;
            case MODE_LIVE:
                mText = mTextLive;
            break;
            case MODE_FROZEN:
                mText = mTextFrozen;
            break;
        }
    }

    public void setMode(final int mode) {
        if (!isAttachedToWindow()) {
            return;
        }
        if (mode != MODE_NONE) {
            mHandler.removeMessages(MSG_CLEAR_BACKGROUND);
            mHandler.sendEmptyMessageDelayed(MSG_CLEAR_BACKGROUND, CLEAR_BACKGROUND_TIMER);
        }
        setText(mode);
        setLayout();
        invalidate();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        mWidth = w;
        setLayout();
    }

    private void setLayout() {
        final int availableWidth = mWidth - getPaddingLeft() - getPaddingRight();
        if (availableWidth <= 0) {
            return;
        }

        final boolean isRtl = getLayoutDirection() == LAYOUT_DIRECTION_RTL;

        final Layout.Alignment alignment = isRtl ? Layout.Alignment.ALIGN_OPPOSITE : Layout.Alignment.ALIGN_NORMAL;

        mStaticLayout = StaticLayout.Builder.obtain(mText, 0, mText.length(), mTextPaint, availableWidth)
            .setAlignment(alignment)
            .setIncludePad(false)
            .build();
    }

    @Override
    protected void onDraw(@NonNull final Canvas canvas) {
        super.onDraw(canvas);

        if (mStaticLayout == null || mText.isEmpty()) {
            return;
        }

        final float x = getPaddingLeft();
        final float y = getHeight() - getPaddingBottom() - mStaticLayout.getHeight();

        canvas.save();
        canvas.translate(x, y);
        mStaticLayout.draw(canvas);
        canvas.restore();
    }

    @Override
    public boolean handleMessage(@NonNull final Message msg) {
        setMode(MODE_NONE);
        return false;
    }
}
