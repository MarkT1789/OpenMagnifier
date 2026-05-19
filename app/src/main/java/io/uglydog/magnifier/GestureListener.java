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

import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;

import androidx.annotation.NonNull;

public class GestureListener extends GestureDetector.SimpleOnGestureListener {
    private static final String TAG = GestureListener.class.getSimpleName();

    public interface GestureActions {
        boolean onToggleMode();
        void onOpenSettings();
        void onShowHelp();
    }

    private final GestureActions mActions;

    public GestureListener(@NonNull final GestureActions actions) {
        mActions = actions;
    }

    @Override
    public boolean onSingleTapConfirmed(@NonNull final MotionEvent event) {
        if (BuildConfig.DEBUG) Log.d(TAG, "onSingleTapConfirmed");
        return mActions.onToggleMode();
    }

    @Override
    public boolean onDoubleTap(@NonNull final MotionEvent event) {
        if (BuildConfig.DEBUG) Log.d(TAG, "onDoubleTap");
        mActions.onOpenSettings();
        return true;
    }

    @Override
    public void onLongPress(@NonNull final MotionEvent event) {
        if (BuildConfig.DEBUG) Log.d(TAG, "onLongPress");
        mActions.onShowHelp();
    }
}
