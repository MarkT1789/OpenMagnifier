package io.uglydog.magnifier;

import android.os.SystemClock;

/**
 * Production implementation using Android's SystemClock.
 */
public class AndroidSystemClock implements ISystemClock {
    @Override
    public long uptimeMillis() {
        return SystemClock.uptimeMillis();
    }
}
