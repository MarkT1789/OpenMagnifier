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
import androidx.annotation.VisibleForTesting;

public class AndroidSettingsFragmentFactory {
    private static volatile ISettingsHelper helper = new SettingsHelper();

    public static ISettingsHelper getSettingsHelper() {
        return helper;
    }

    // Used for unit testing to inject a mock
    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    static void setSettingsHelper(ISettingsHelper mockHelper) {
        if (BuildConfig.DEBUG) {
            helper = mockHelper;
        }
    }
}
