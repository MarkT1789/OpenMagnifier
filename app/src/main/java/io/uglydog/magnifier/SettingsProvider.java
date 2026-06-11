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
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.preference.PreferenceManager;

public class SettingsProvider {
    private static final String TAG = SettingsProvider.class.getSimpleName();

    private static final String KEY_BRIGHTNESS = "brightness_setting";
    private static final String KEY_COLOR = "color_setting";
    private static final String KEY_CONTRAST = "contrast_setting";
    private static final String KEY_DX = "dx_setting";
    private static final String KEY_DY = "dy_setting";
    private static final String KEY_ROTATION = "rotation_setting";
    private static final String KEY_SPLASH_VERSION = "splash_version_setting";
    private static final String KEY_ZOOM = "zoom_setting";
    private static final String KEY_FLASHLIGHT = "flashlight_setting";
    private static final String KEY_SPEAK = "speak_setting";
    private static final String KEY_SOURCE = "source_setting";
    private static final String KEY_DEST = "dest_setting";

    private final SharedPreferences mPrefs;
    private float mBrightness, mContrast, mDx, mDy, mZoom, mFlashlight;
    private int mRotation, mColor, mSpeak, mSource, mDest;
    private String mSplashVersion;

    public SettingsProvider(@NonNull final Context context) {
        mPrefs = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());
        reload();
    }

    public synchronized void reload() {
        mBrightness = getFloat(KEY_BRIGHTNESS, 0.0f);
        mColor = getInt(KEY_COLOR, 0);
        mContrast = getFloat(KEY_CONTRAST, 1.0f);
        mDx = getFloat(KEY_DX, 0.25f);
        mDy = getFloat(KEY_DY, 0.25f);
        mRotation =  getInt(KEY_ROTATION, 0);
        mSplashVersion = getString(KEY_SPLASH_VERSION, "-1");
        mZoom = getFloat(KEY_ZOOM, 1.0f);
        mFlashlight = getFloat(KEY_FLASHLIGHT, 0.0f);
        mSpeak = getInt(KEY_SPEAK, 0);
        mSource = getInt(KEY_SOURCE, 0);
        mDest = getInt(KEY_DEST, 0);
    }

    public synchronized float getBrightness() {
        return mBrightness;
    }

    public synchronized void setBrightness(final float brightness) {
        mBrightness = brightness;
        setFloat(KEY_BRIGHTNESS, brightness);
    }

    public synchronized int getColor() {
        return mColor;
    }

    public synchronized void setColor(final int color) {
        mColor = color;
        setInt(KEY_COLOR, color);
    }

    public synchronized float getContrast() {
        return mContrast;
    }

    public synchronized void setContrast(final float contrast) {
        mContrast = contrast;
        setFloat(KEY_CONTRAST, contrast);
    }

    public synchronized float getDx() {
        return mDx;
    }

    public synchronized void setDx(final float dx) {
        mDx = dx;
        setFloat(KEY_DX, dx);
    }

    public synchronized float getDy() {
        return mDy;
    }

    public synchronized void setDy(final float dy) {
        mDy = dy;
        setFloat(KEY_DY, dy);
    }

    public synchronized int getRotation() {
        return mRotation;
    }

    public synchronized void setRotation(final int rotation) {
        mRotation = rotation;
        setInt(KEY_ROTATION, rotation);
    }

    public synchronized String getSplashVersion() {
        return mSplashVersion;
    }

    public synchronized void setSplashVersion(@NonNull final String version) {
        mSplashVersion = version;
        setString(KEY_SPLASH_VERSION, version);
    }

    public synchronized float getZoom() {
        return mZoom;
    }

    public synchronized void setZoom(final float zoom) {
        mZoom = zoom;
        setFloat(KEY_ZOOM, zoom);
    }

    public synchronized float getFlashlight() {
        return mFlashlight;
    }

    public synchronized void setFlashlight(final float flashlight) {
        mFlashlight = flashlight;
        setFloat(KEY_FLASHLIGHT, flashlight);
    }

    public synchronized int getSpeak() {
        return mSpeak;
    }

    public synchronized void setSpeak(final int speak) {
        mSpeak = speak;
        setInt(KEY_SPEAK, speak);
    }

    public synchronized int getSource() {
        return mSource;
    }

    public synchronized void setSource(final int source) {
        mSource = source;
        setInt(KEY_SOURCE, source);
    }

    public synchronized int getDest() {
        return mDest;
    }

    public synchronized void setDest(final int dest) {
        mDest = dest;
        setInt(KEY_DEST, dest);
    }

    /***************************************************/

    private String getString(@NonNull final String key, @NonNull final String defaultValue) {
        String value = mPrefs.getString(key, defaultValue);
        if (BuildConfig.DEBUG) Log.d(TAG, "getString: " + key + " = " + value);
        return value;
    }

    private void setString(@NonNull final String key, @NonNull final String value) {
        if (BuildConfig.DEBUG) Log.d(TAG, "setString: " + key + " = " + value);
        mPrefs.edit().putString(key, value).apply();
    }

    private float getFloat(@NonNull final String key, @NonNull final float defaultValue) {
        try {
            final float value = Float.parseFloat(mPrefs.getString(key, String.valueOf(defaultValue)));
            if (BuildConfig.DEBUG) Log.d(TAG, "getFloat: " + key + " = " + value);
            return value;
        } catch (Exception e) {
            Log.e(TAG, "getFloat: error: " + key + " : " + e);
            return defaultValue;
        }
    }

    private void setFloat(@NonNull final String key, float value) {
        if (BuildConfig.DEBUG) Log.d(TAG, "setFloat: " + key + " = " + value);
        mPrefs.edit().putString(key, String.valueOf(value)).apply();
    }

    private int getInt(@NonNull final String key, final int defaultValue) {
        try {
            final int value = Integer.parseInt(mPrefs.getString(key, String.valueOf(defaultValue)));
            if (BuildConfig.DEBUG) Log.d(TAG, "getInt: " + key + " = " + value);
            return value;
        } catch (Exception e) {
            Log.e(TAG, "getInt: error: " + key + " : " + e);
            return defaultValue;
        }
    }

    private void setInt(@NonNull final String key, final int value) {
        if (BuildConfig.DEBUG) Log.d(TAG, "setInt: " + key + " = " + value);
        mPrefs.edit().putString(key, String.valueOf(value)).apply();
    }
}
