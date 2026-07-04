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
