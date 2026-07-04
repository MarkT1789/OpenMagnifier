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

import android.view.View;
import java.lang.reflect.Method;

public class ViewTestUtils {
    public static void setAttached(View view, boolean attached) {
        try {
            Method method = View.class.getDeclaredMethod(
                attached ? "dispatchAttachedToWindow" : "dispatchDetachedFromWindow"
            );
            method.setAccessible(true);
            method.invoke(view);
        } catch (Exception e) {
            try {
                // Alternately fallback onto alternative internal framework indicators depending on targets
                Method alternateMethod = View.class.getDeclaredMethod(
                    attached ? "onAttachedToWindow" : "onDetachedFromWindow"
                );
                alternateMethod.setAccessible(true);
                alternateMethod.invoke(view);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }
}
