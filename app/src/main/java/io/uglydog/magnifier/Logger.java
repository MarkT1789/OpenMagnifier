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

public class Logger {
    public static void d(final String tag, final String message) {
        try {
            android.util.Log.d(tag, message);
        } catch (RuntimeException e) {
            System.out.println("[" + tag + "] DEBUG: " + message);
        }
    }

    public static void i(final String tag, final String message) {
        try {
            android.util.Log.i(tag, message);
        } catch (RuntimeException e) {
            System.out.println("[" + tag + "] INFO: " + message);
        }
    }

    public static void w(final String tag, final String message) {
        try {
            android.util.Log.w(tag, message);
        } catch (RuntimeException e) {
            System.out.println("[" + tag + "] WARN: " + message);
        }
    }
    public static void e(final String tag, final String message) {
        try {
            android.util.Log.e(tag, message);
        } catch (RuntimeException e) {
            System.err.println("[" + tag + "] ERROR: " + message);
        }
    }
}
