package io.uglydog.magnifier;

import android.content.Context;
import androidx.annotation.NonNull;

public class AndroidTranslationManagerFactory implements TranslationManager.TranslationFactory {
    @Override
    public TranslationManager create(@NonNull final Context context, final TextReaderOverlay overlay, final ToastManager toastManager) {
        return new TranslationManager(context, overlay, toastManager);
    }
}
