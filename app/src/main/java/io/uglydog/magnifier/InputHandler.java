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

import android.view.KeyEvent;

import androidx.annotation.NonNull;

public class InputHandler {
    private static final String TAG = InputHandler.class.getSimpleName();

    public interface InputActions {
        void onChangeBrightnessSetting(KeyEvent event);
        void onChangeColorFilterSetting(KeyEvent event);
        void onChangeContrastSetting(KeyEvent event);
        void onChangeFlashlightSetting(KeyEvent event);
        void onChangeSpeakSetting(KeyEvent event);
        void onChangePanSetting(KeyEvent event);
        void onChangeRotationSetting(KeyEvent event);
        void onChangeView();
        void onChangeZoomSetting(KeyEvent event);
        void onScrollViewport(KeyEvent event);
        boolean onVolumeChanged(KeyEvent event);
        void onShowHelp();
        void onShowVersion();
    }

    public static boolean handleKey(@NonNull final KeyEvent event, @NonNull final InputActions actions) {

        final boolean isKeyDown = event.getAction() == KeyEvent.ACTION_DOWN;
        final boolean isFirstKeyDown = isKeyDown && event.getRepeatCount() == 0;

        if (isFirstKeyDown) {
            if (BuildConfig.DEBUG) Logger.d(TAG, "handleKey: " + KeyEvent.keyCodeToString(event.getKeyCode()));
        }

        switch (event.getKeyCode()) {
            case KeyEvent.KEYCODE_B:
                if (isFirstKeyDown)
                    actions.onChangeBrightnessSetting(event);
                return true;
            case KeyEvent.KEYCODE_C:
                if (isFirstKeyDown)
                    actions.onChangeContrastSetting(event);
                return true;
            case KeyEvent.KEYCODE_F:
                if (isFirstKeyDown)
                    actions.onChangeColorFilterSetting(event);
                return true;
            case KeyEvent.KEYCODE_H:
                if (isFirstKeyDown)
                    actions.onShowHelp();
                return true;
            case KeyEvent.KEYCODE_L:
                if (isFirstKeyDown)
                    actions.onChangeFlashlightSetting(event);
                return true;
            case KeyEvent.KEYCODE_R:
                if (isFirstKeyDown)
                    actions.onChangeRotationSetting(event);
                return true;
            case KeyEvent.KEYCODE_S:
                if (isFirstKeyDown)
                    actions.onChangeSpeakSetting(event);
                return true;
            case KeyEvent.KEYCODE_V:
                if (isFirstKeyDown)
                    actions.onShowVersion();
                return true;
            case KeyEvent.KEYCODE_X:
            case KeyEvent.KEYCODE_Y:
                if (isFirstKeyDown)
                    actions.onChangePanSetting(event);
                return true;
            case KeyEvent.KEYCODE_Z:
                if (isFirstKeyDown)
                    actions.onChangeZoomSetting(event);
                return true;

            case KeyEvent.KEYCODE_SPACE:
                if (isFirstKeyDown)
                    actions.onChangeView();
                return true;

            case KeyEvent.KEYCODE_DPAD_LEFT:
            case KeyEvent.KEYCODE_DPAD_RIGHT:
            case KeyEvent.KEYCODE_DPAD_UP:
            case KeyEvent.KEYCODE_DPAD_DOWN:
                if (isKeyDown)
                    actions.onScrollViewport(event);
                return true;

            case KeyEvent.KEYCODE_VOLUME_UP:
            case KeyEvent.KEYCODE_VOLUME_DOWN:
                if (isKeyDown)
                    return actions.onVolumeChanged(event);
                return false;
        }
        return false;
    }
}
