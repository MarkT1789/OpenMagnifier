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
import android.widget.Toast;

import androidx.annotation.NonNull;

public class AndroidToastManagerFactory implements ToastManager.ToastFactory {
    @Override
    public IToast create(@NonNull Context context, @NonNull String msg, int duration) {
        final Toast mToast = Toast.makeText(context, msg, duration);
        return new IToast() {
            @Override
            public void show() {
                mToast.show();
            }
            @Override
            public void cancel() {
                mToast.cancel();
            }
        };
    }
}
