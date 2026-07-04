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

import android.speech.tts.TextToSpeech;
import androidx.annotation.NonNull;
import java.util.ArrayList;
import java.util.HashMap;

public interface ITranslationManager {
    void prepare(final int sourceId, final int targetId);
    void translate(@NonNull final TextToSpeech tts, @NonNull final HashMap<String, String> hashMap, final ArrayList<String> arrayList, @NonNull final String text, @NonNull final String id);
    void close();
}
