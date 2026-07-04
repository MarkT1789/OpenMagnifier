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

import android.view.ScaleGestureDetector;
import androidx.annotation.NonNull;

public class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
    private static final String TAG = ScaleListener.class.getSimpleName();
    private final ScaleActions mActions;
    private final ISystemClock mClock; // Injected dependency
    private long mTime;
    private float mScale;

    public interface ScaleActions {
        void onScale(float scale, boolean finished);
    }

    // Java 7 compliant DI Constructor
    public ScaleListener(@NonNull final ScaleActions actions, @NonNull final ISystemClock clock) {
        mActions = actions;
        mClock = clock;
    }

    @Override
    public boolean onScaleBegin(@NonNull ScaleGestureDetector detector) {
        if (BuildConfig.DEBUG) Logger.d(TAG, "onScaleBegin");
        mTime = 0;
        mScale = 1.0f;
        return true;
    }

    @Override
    public boolean onScale(@NonNull ScaleGestureDetector detector) {
        final float scale = detector.getScaleFactor();
        if (BuildConfig.DEBUG) Logger.d(TAG, "onScale " + scale);
        
        // Using the injected interface instead of Android's static SystemClock
        final long current = mClock.uptimeMillis();
        final long delay = current - mTime;
        mScale *= scale;
        
        if (delay > 30 || mScale > 1.05f || mScale < 0.95f)  {
            mActions.onScale(mScale, false);
            mTime = current;
            mScale = 1.0f;
        }
        return true;
    }

    @Override
    public void onScaleEnd(@NonNull ScaleGestureDetector detector) {
        if (BuildConfig.DEBUG) Logger.d(TAG, "onScaleEnd");
        mActions.onScale(mScale, true);
        // FIXME super.onScaleEnd(detector);
    }
}
