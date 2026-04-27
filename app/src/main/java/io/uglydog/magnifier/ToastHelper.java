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
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

public class ToastHelper {
    private static final String TAG = ToastHelper.class.getSimpleName();
    private static final Handler mHandler = new Handler(Looper.getMainLooper());
    private static Toast mToast;

    public static void show(@NonNull final Context context, @NonNull final String msg) {
        final Context appContext = context.getApplicationContext();
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                if (mToast != null) {
                    mToast.cancel();
                }
                if (BuildConfig.DEBUG) Log.d(TAG, "show: " + msg);
                mToast = Toast.makeText(appContext, msg, Toast.LENGTH_LONG);
                mToast.show();
            }
        });
    }
}
