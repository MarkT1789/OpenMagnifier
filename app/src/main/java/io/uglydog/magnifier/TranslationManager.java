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

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.speech.tts.TextToSpeech;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.mlkit.common.model.DownloadConditions;
import com.google.mlkit.nl.translate.TranslateLanguage;
import com.google.mlkit.nl.translate.Translation;
import com.google.mlkit.nl.translate.Translator;
import com.google.mlkit.nl.translate.TranslatorOptions;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Locale;

public class TranslationManager {

    private static final String TAG = TranslationManager.class.getSimpleName();

    @Nullable
    private Translator mTranslator;
    private boolean mIsReady;
    private boolean mIsDownloading;
    private int mSourceId;
    private int mTargetId;
    private int mTtsId;
    private final Context mContext;
    private final TextReaderOverlay mTextReaderOverlay;

    private long mActiveSessionId = 0;

    public TranslationManager(final Context context, TextReaderOverlay overlay) {
        mContext = context.getApplicationContext();
        mTextReaderOverlay = overlay;
        reset();
    }

    private static class ModelDownloadSuccessListener implements OnSuccessListener<Void> {
        private final WeakReference<TranslationManager> mManagerRef;
        private final long mSessionId;

        ModelDownloadSuccessListener(TranslationManager manager, long sessionId) {
            mManagerRef = new WeakReference<>(manager);
            mSessionId = sessionId;
        }

        @Override
        public void onSuccess(Void v) {
            final TranslationManager manager = mManagerRef.get();
            if (manager != null) {
                synchronized (manager) {
                    if (manager.mActiveSessionId == mSessionId) {
                        Log.i(TAG, "prepare: models are downloaded and ready");
                        manager.mIsReady = true;
                        if (manager.mIsDownloading) {
                            ToastHelper.show(manager.mContext, manager.mContext.getString(R.string.toast_translation_downloaded));
                            manager.mIsDownloading = false;
                        }
                    }
                }
            }
        }
    }

    private static class ModelDownloadFailureListener implements OnFailureListener {
        private final WeakReference<TranslationManager> mManagerRef;
        private final long mSessionId;

        ModelDownloadFailureListener(TranslationManager manager, long sessionId) {
            mManagerRef = new WeakReference<>(manager);
            mSessionId = sessionId;
        }

        @Override
        public void onFailure(@NonNull Exception e) {
            final TranslationManager manager = mManagerRef.get();
            if (manager != null) {
                synchronized (manager) {
                    if (manager.mActiveSessionId == mSessionId) {
                        Log.e(TAG, "prepare: models are not downloaded", e);
                        manager.mIsReady = false;
                    }
                }
            }
        }
    }

    private static class TranslationSuccessListener implements OnSuccessListener<String> {
        private final WeakReference<TranslationManager> mManagerRef;
        private final WeakReference<TextToSpeech> mTtsRef;
        private final String mTextId;
        private final int mTargetId;
        private final HashMap<String, String> mHashMap;

        TranslationSuccessListener(TranslationManager manager, TextToSpeech tts, HashMap<String, String> hashMap, String textId, int targetId) {
            mManagerRef = new WeakReference<>(manager);
            mTtsRef = new WeakReference<>(tts);
            mHashMap = hashMap;
            mTextId = textId;
            mTargetId = targetId;
        }

        @Override
        public void onSuccess(String translatedText) {
            final TranslationManager manager = mManagerRef.get();
            final TextToSpeech tts = mTtsRef.get();
            if (manager != null && tts != null) {
                if (BuildConfig.DEBUG) {
                    Log.d(TAG, "translate: success: " + translatedText);
                }
                manager.setTtsLanguage(mTargetId, tts);
                mHashMap.put(mTextId, translatedText);
                tts.speak(translatedText, TextToSpeech.QUEUE_ADD, null, mTextId);
            }
        }
    }

    private static class TranslationFailureListener implements OnFailureListener {
        private final WeakReference<TranslationManager> mManagerRef;
        private final WeakReference<TextToSpeech> mTtsRef;
        private final String mOriginalText;
        private final String mTextId;
        private final int mSourceId;
        private final HashMap<String, String> mHashMap;

        TranslationFailureListener(TranslationManager manager, TextToSpeech tts, HashMap<String, String> hashMap, String originalText, String textId, int sourceId) {
            mManagerRef = new WeakReference<>(manager);
            mTtsRef = new WeakReference<>(tts);
            mHashMap = hashMap;
            mOriginalText = originalText;
            mTextId = textId;
            mSourceId = sourceId;
        }

        @Override
        public void onFailure(@NonNull Exception e) {
            final TranslationManager manager = mManagerRef.get();
            final TextToSpeech tts = mTtsRef.get();
            if (manager != null && tts != null) {
                if (BuildConfig.DEBUG) {
                    Log.d(TAG, "translate: failed: " + e.getMessage());
                }
                manager.setTtsLanguage(mSourceId, tts);
                mHashMap.put(mTextId, mOriginalText);
                tts.speak(mOriginalText, TextToSpeech.QUEUE_ADD, null, mTextId);
            }
        }
    }

    public synchronized void prepare(final int sourceId, final int targetId) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "prepare: source=" + sourceId + " targetId=" + targetId);
        }

        if (sourceId == targetId || sourceId == 0 || targetId == 0) {
            close();
            return;
        }

        if (sourceId == mSourceId && targetId == mTargetId) {
            return;
        }

        final String sourceStr = getTranslationString(sourceId);
        final String targetStr = getTranslationString(targetId);

        if (sourceStr == null || targetStr == null) {
            Log.e(TAG, "prepare: cancelled");
            close();
            return;
        }

        if (mTranslator != null) {
            mTranslator.close();
        }

        mSourceId = sourceId;
        mTargetId = targetId;
        mIsReady = false;
        mIsDownloading = false;
        mActiveSessionId++;

        final TranslatorOptions options = new TranslatorOptions.Builder()
                .setSourceLanguage(sourceStr)
                .setTargetLanguage(targetStr)
                .build();

        mTranslator = Translation.getClient(options);

        final DownloadConditions conditions = new DownloadConditions.Builder()
                .build();

        mTranslator.downloadModelIfNeeded(conditions)
                .addOnSuccessListener(new ModelDownloadSuccessListener(this, mActiveSessionId))
                .addOnFailureListener(new ModelDownloadFailureListener(this, mActiveSessionId));
    }

    public synchronized void translate(@NonNull final TextToSpeech tts, @NonNull final HashMap<String, String> hashMap, @NonNull final String text, @NonNull final String id) {
        if (!mIsReady || mTranslator == null) {
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "translate: not ready: " + text);
            }
            setTtsLanguage(0, tts);
            hashMap.put(id, text);
            if (!mIsReady && mTranslator != null) {
                ToastHelper.show(mContext, mContext.getString(R.string.toast_translation_downloading));
                mIsDownloading = true;
            }
            tts.speak(text, TextToSpeech.QUEUE_ADD, null, id);
            return;
        }

        if (mTextReaderOverlay != null) {
            mTextReaderOverlay.showCopyright(true);
        }

        mTranslator.translate(text)
                .addOnSuccessListener(new TranslationSuccessListener(this, tts, hashMap, id, mTargetId))
                .addOnFailureListener(new TranslationFailureListener(this, tts, hashMap, text, id, mSourceId));
    }

    public synchronized void close() {
        if (mTranslator != null) {
            mTranslator.close();
        }
        reset();
    }

    private void reset() {
        mTranslator = null;
        mIsReady = false;
        mIsDownloading = false;
        mSourceId = -1;
        mTargetId = -1;
        mTtsId = -1;
        mActiveSessionId++;
    }

    private void setTtsLanguage(final int index, final TextToSpeech tts) {
        if (index == mTtsId) {
            return;
        }
        mTtsId = index;

        final Locale locale = getTtsLocale(index);
        if (locale != null && tts != null) {
            try {
                final int result = tts.setLanguage(locale);
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    final Intent intent = new Intent();
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    intent.setAction(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA);
                    mContext.startActivity(intent);
                } else {
                    Log.i(TAG, "setTtsLanguage: success: " + locale.getDisplayName());
                }
            } catch (ActivityNotFoundException e) {
                Log.e(TAG, "setTtsLanguage: engine installation activity missing", e);
            }
        }
    }

    @Nullable
    private String getTranslationString(final int index) {
        switch (index) {
            case 1:  return TranslateLanguage.BENGALI;
            case 2:  return TranslateLanguage.CHINESE;
            case 3:  return TranslateLanguage.ENGLISH;
            case 4:  return TranslateLanguage.FRENCH;
            case 5:  return TranslateLanguage.HINDI;
            case 6:  return TranslateLanguage.INDONESIAN;
            case 7:  return TranslateLanguage.JAPANESE;
            case 8:  return TranslateLanguage.MARATHI;
            case 9:  return TranslateLanguage.PORTUGUESE;
            case 10: return TranslateLanguage.SPANISH;
            default:
                Log.e(TAG, "getTranslationString: error: " + index);
                return null;
        }
    }

    @Nullable
    private Locale getTtsLocale(final int index) {
        switch (index) {
            case 0:  return Locale.getDefault();
            case 1:  return new Locale("bn", "BD");
            case 2:  return Locale.SIMPLIFIED_CHINESE;
            case 3:  return Locale.ENGLISH;
            case 4:  return Locale.FRENCH;
            case 5:  return new Locale("hi", "IN");
            case 6:  return new Locale("id");
            case 7:  return Locale.JAPANESE;
            case 8:  return new Locale("mr", "IN");
            case 9:  return new Locale("pt");
            case 10: return new Locale("es");
            default:
                Log.e(TAG, "getTtsLocale: error: " + index);
                return null;
        }
    }
}
