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
import android.graphics.Bitmap;
import android.graphics.BitmapRegionDecoder;
import android.graphics.PointF;
import android.graphics.Rect;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.view.View;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions;
import com.google.mlkit.vision.text.devanagari.DevanagariTextRecognizerOptions;
import com.google.mlkit.vision.text.japanese.JapaneseTextRecognizerOptions;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView;

import java.io.File;
import java.io.FileInputStream;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;

public class TextReader implements Handler.Callback {

    private static final String TAG = TextReader.class.getSimpleName();
    private static final int OFFSET = 20;
    private static final int MSG_SPEAK = 1;

    private TextToSpeech mTts;
    private SubsamplingScaleImageView mImageView;
    private final File mFile;

    private TextReaderOverlay mTextReaderOverlay;

    private final Handler mMainHandler;
    private final Handler mBackgroundHandler;
    private final HandlerThread mBackgroundThread;
    private final SettingsManager mSettingsManager;
    private final Context mContext;

    private TextRecognizer mTextRecognizer;
    private final ITranslationManager mTranslationManager;

    private volatile boolean mTtsReady;
    private volatile boolean mIsDestroyed;
    private volatile boolean mTtsStarting;

    private volatile int mCurrentTaskToken = 0;

    private Task<Text> mActiveRecognitionTask = null;
    private int mSpeak;
    private final HashMap<String, String> mHashMap;
    private final ArrayList<String> mArrayList;
    private long mLastVolumeUp;

    private static class TtsInitListener implements TextToSpeech.OnInitListener {
        private final WeakReference<TextReader> mReaderRef;
        private final Locale mLocale;

        TtsInitListener(final TextReader reader, final Locale locale) {
            mReaderRef = new WeakReference<>(reader);
            mLocale = locale;
        }

        @Override
        public void onInit(final int status) {
            final TextReader reader = mReaderRef.get();
            if (reader == null || reader.mIsDestroyed) return;
            reader.mTtsStarting = false;

            if (status == TextToSpeech.SUCCESS) {
                final int result = reader.mTts.setLanguage(mLocale);
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Logger.e(TAG, "TextToSpeech: locale not supported or missing data locale=" + mLocale);
                } else {
                    Logger.i(TAG, "TextToSpeech: ready locale=" + mLocale);
                    reader.mTtsReady = true;
                }
            } else {
                Logger.e(TAG, "TextToSpeech: initialization failed status=" + status);
            }
        }
    }

    private static class TtsProgressListener extends UtteranceProgressListener {
        private final WeakReference<TextReader> mReaderRef;

        TtsProgressListener(final TextReader reader) {
            mReaderRef = new WeakReference<>(reader);
        }

        @Override
        public void onRangeStart(final String utteranceId, final int start, final int end, final int frame) {
            final TextReader reader = mReaderRef.get();
            if (reader == null || reader.mIsDestroyed) return;

            reader.mMainHandler.post(new Runnable() {
                @Override
                public void run() {
                    final TextReader r = mReaderRef.get();
                    if (r == null || r.mIsDestroyed || r.mTextReaderOverlay == null || r.mSettingsManager.getBanner() == 0) return;

                    final String text = r.mHashMap.get(utteranceId);
                    if (text != null) {
                        r.mTextReaderOverlay.setText(text, start, end);
                    }
                }
            });
        }

        @Override
        public void onStart(final String utteranceId) {
            final TextReader reader = mReaderRef.get();
            if (reader == null || reader.mIsDestroyed) return;

            reader.mMainHandler.post(new Runnable() {
                @Override
                public void run() {
                    final TextReader r = mReaderRef.get();
                    if (r == null || r.mIsDestroyed || r.mTextReaderOverlay == null) return;

                    final String[] coordinates = utteranceId.split(":");
                    if (coordinates.length == 4) {
                        final int left = Integer.parseInt(coordinates[0]);
                        final int top = Integer.parseInt(coordinates[1]);
                        final int right = Integer.parseInt(coordinates[2]);
                        final int bottom = Integer.parseInt(coordinates[3]);

                        final Rect rect = new Rect(left, top, right, bottom);
                        r.mTextReaderOverlay.setRect(rect);
                    }
                    r.mHashMap.put("current", utteranceId);
                }
            });
        }

        @Override
        public void onDone(final String utteranceId) {
            final TextReader reader = mReaderRef.get();
            if (reader == null) return;
            reader.mMainHandler.post(new Runnable() {
                @Override
                public void run() {
                    final TextReader r = mReaderRef.get();
                    if (r != null && r.mTextReaderOverlay != null) {
                        r.mTextReaderOverlay.clear();
                    }
                }
            });
        }

        @Override
        public void onError(final String utteranceId) {
            final TextReader reader = mReaderRef.get();
            if (reader == null) return;
            reader.mMainHandler.post(new Runnable() {
                @Override
                public void run() {
                    final TextReader r = mReaderRef.get();
                    if (r != null && r.mTextReaderOverlay != null) {
                        r.mTextReaderOverlay.clear();
                    }
                }
            });
        }
    }

    private static class ImageStateListener implements SubsamplingScaleImageView.OnStateChangedListener {
        private final WeakReference<TextReader> mReaderRef;

        ImageStateListener(final TextReader reader) {
            mReaderRef = new WeakReference<>(reader);
        }

        @Override
        public void onScaleChanged(float newScale, final int origin) {
            final TextReader reader = mReaderRef.get();
            if (reader == null || reader.mIsDestroyed) return;
            if (reader.mTextReaderOverlay != null) {
                reader.mTextReaderOverlay.clear();
            }
            if (reader.mTts != null) {
                reader.mTts.stop();
            }
            reader.start();
        }

        @Override
        public void onCenterChanged(final PointF newCenter, final int origin) {
            final TextReader reader = mReaderRef.get();
            if (reader == null || reader.mIsDestroyed) return;
            if (reader.mTextReaderOverlay != null) {
                reader.mTextReaderOverlay.clear();
            }
            if (reader.mTts != null) {
                reader.mTts.stop();
            }
            reader.start();
        }
    }

    private static class TextRecognitionSuccessListener implements OnSuccessListener<Text> {
        private final WeakReference<TextReader> mReaderRef;
        private final WeakReference<ITranslationManager> mTranslationManagerRef;
        private final Bitmap mBitmap;
        private final int mViewWidth;
        private final int mViewHeight;
        private final int mToken;

        TextRecognitionSuccessListener(final TextReader reader, final ITranslationManager translationManager, final Bitmap bitmap, final int viewWidth, final int viewHeight, final int token) {
            mReaderRef = new WeakReference<>(reader);
            mTranslationManagerRef = new WeakReference<>(translationManager);
            mBitmap = bitmap;
            mViewWidth = viewWidth;
            mViewHeight = viewHeight;
            mToken = token;
        }

        @Override
        public void onSuccess(final Text visionText) {
            final TextReader reader = mReaderRef.get();
            final ITranslationManager translationManager = mTranslationManagerRef.get();
            if (reader == null || translationManager == null || reader.mIsDestroyed || mToken != reader.mCurrentTaskToken || reader.mImageView == null || !reader.mImageView.isReady()) {
                if (mBitmap != null && !mBitmap.isRecycled()) {
                    mBitmap.recycle();
                }
                return;
            }
            try {
                reader.mTts.stop();

                final float scaleX = (float) mViewWidth / mBitmap.getWidth();
                final float scaleY = (float) mViewHeight / mBitmap.getHeight();

                for (Text.TextBlock block : visionText.getTextBlocks()) {
                    if (block == null) {
                        continue;
                    }
                    final Rect bounds = block.getBoundingBox();
                    if (bounds == null) {
                        continue;
                    }
                    final Rect scaledBounds = new Rect(
                        (int) (bounds.left * scaleX) - OFFSET,
                        (int) (bounds.top * scaleY) - OFFSET,
                        (int) (bounds.right * scaleX) + 2 * OFFSET,
                        (int) (bounds.bottom * scaleY) + 2 * OFFSET
                    );

                    final String id = String.format(Locale.US, "%d:%d:%d:%d", scaledBounds.left, scaledBounds.top, scaledBounds.right, scaledBounds.bottom);
                    final String text = block.getText().replaceAll("[\\r\\n]+", " ");

                    translationManager.translate(reader.mTts, reader.mHashMap, reader.mArrayList, text, id);
                    if (BuildConfig.DEBUG) Logger.d(TAG, "TextRecognition: text=" + text + " bounds=" + bounds + " id=" + id);
                }
            } finally {
                if (mBitmap != null && !mBitmap.isRecycled()) {
                    mBitmap.recycle();
                }
            }
        }
    }

    private static class TextRecognitionFailureListener implements OnFailureListener {
        private final WeakReference<TextReader> mReaderRef;
        private final Bitmap mBitmap;
        private final int mToken;

        TextRecognitionFailureListener(final TextReader reader, final Bitmap bitmap, final int token) {
            mReaderRef = new WeakReference<>(reader);
            mBitmap = bitmap;
            mToken = token;
        }

        @Override
        public void onFailure(@NonNull Exception e) {
            final TextReader reader = mReaderRef.get();
            if (reader == null || reader.mIsDestroyed || mToken != reader.mCurrentTaskToken) {
                if (mBitmap != null && !mBitmap.isRecycled()) {
                    mBitmap.recycle();
                }
                return;
            }
            Logger.e(TAG, "TextRecognition: failed: " + e);
            if (mBitmap != null && !mBitmap.isRecycled()) {
                mBitmap.recycle();
            }
        }
    }

    // Constructor modified to support Dependency Injection via the interface
    public TextReader(final Context context, final SubsamplingScaleImageView imageView, final TextReaderOverlay overlay, final String file, final SettingsManager settings, final ITranslationManager translationManager) {
        mContext = context;
        mImageView = imageView;
        mTextReaderOverlay = overlay;
        mFile = new File(context.getCacheDir(), file);
        mSettingsManager = settings;
        mTtsReady = false;
        mTtsStarting = false;
        mIsDestroyed = false;
        mTextRecognizer = null;
        mTranslationManager = translationManager;
        mTts = null;
        mSpeak = -1;
        mHashMap = new HashMap<String, String>();
        mArrayList = new ArrayList<String>();

        mBackgroundThread = new HandlerThread("TextReaderBackground");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());

        mMainHandler = new Handler(Looper.getMainLooper(), this);

        mImageView.setOnStateChangedListener(new ImageStateListener(this));
    }

    private void setupTextRecognizer() {
        final int speak = mSettingsManager.getSpeak();
        if (speak > 0) {
            mHashMap.clear();
            mArrayList.clear();
            mTranslationManager.prepare(mSettingsManager.getSource(), mSettingsManager.getDest());
        }
        if (speak == mSpeak) {
            return;
        }

        mSpeak = speak;

        if (mTextRecognizer != null) {
            mTextRecognizer.close();
            mTextRecognizer = null;
        }

        switch(mSpeak) {
            case 1: /* Latin */
                Logger.i(TAG, "TextRecognizer Latin");
                mTextRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
            break;
            case 2: /* Chinese */
                Logger.i(TAG, "TextRecognizer Chinese");
                mTextRecognizer = TextRecognition.getClient(new ChineseTextRecognizerOptions.Builder().build());
            break;
            case 3: /* Devanagari */
                Logger.i(TAG, "TextRecognizer Devanagari");
                mTextRecognizer = TextRecognition.getClient(new DevanagariTextRecognizerOptions.Builder().build());
            break;
            case 4: /* Japanese */
                Logger.i(TAG, "TextRecognizer Japanese");
                mTextRecognizer = TextRecognition.getClient(new JapaneseTextRecognizerOptions.Builder().build());
            break;
        }

        if (mTts == null && mSpeak > 0) {
            mTtsStarting = true;
            mTts = new TextToSpeech(mContext.getApplicationContext(), new TtsInitListener(this, Locale.getDefault()));
            mTts.setOnUtteranceProgressListener(new TtsProgressListener(this));
        }
    }

    @Override
    public boolean handleMessage(@NonNull final Message msg) {
        if (msg.what == MSG_SPEAK) {
            if (BuildConfig.DEBUG) Logger.d(TAG, "handleMessage: MSG_SPEAK");
            if (mTtsStarting && !mTtsReady && !mIsDestroyed) {
                if (BuildConfig.DEBUG) Logger.d(TAG, "handleMessage: MSG_SPEAK tts not ready");
                mMainHandler.sendEmptyMessageDelayed(MSG_SPEAK, 250);
            }
            if (mTtsReady && !mIsDestroyed) {
                shouldSpeak();
            }
            return true;
        }
        return false;
    }

    public void stop() {
        if (BuildConfig.DEBUG) Logger.d(TAG, "stop");
        mMainHandler.removeMessages(MSG_SPEAK);
        mBackgroundHandler.removeCallbacksAndMessages(null);
        if (mTextRecognizer != null) {
            mTextRecognizer.close();
            mTextRecognizer = null;
            mSpeak = -1;
        }
        if (mTranslationManager != null) {
            mTranslationManager.close();
        }
        if (mTtsReady && !mIsDestroyed) {
            mTts.stop();
        }
        if (mTextReaderOverlay != null) {
            mTextReaderOverlay.clearOverlay();
        }
    }

    public void start() {
        if (BuildConfig.DEBUG) Logger.d(TAG, "start");
        setupTextRecognizer();
        mMainHandler.removeMessages(MSG_SPEAK);
        if (mSettingsManager.getSpeak() != 0 && !mIsDestroyed) {
            mMainHandler.sendEmptyMessageDelayed(MSG_SPEAK, 2000);
        }
    }

    private void shouldSpeak() {
        if (mIsDestroyed || mImageView == null || !mImageView.isReady() || mImageView.getVisibility() != View.VISIBLE) {
            mTts.stop();
            return;
        }

        final Rect visibleRect = new Rect();
        mImageView.visibleFileRect(visibleRect);

        if (visibleRect.isEmpty()) {
            return;
        }

        final int viewWidth = mImageView.getWidth();
        final int viewHeight = mImageView.getHeight();

        final int token;
        synchronized (this) {
            mCurrentTaskToken++;
            token = mCurrentTaskToken;
        }

        mBackgroundHandler.post(new Runnable() {
            @Override
            public void run() {
                processFile(visibleRect, viewWidth, viewHeight, token);
            }
        });
    }

    private Bitmap getClippedBitmap(@NonNull final Rect visibleRect) {
        if (mIsDestroyed) {
            return null;
        }

        Bitmap croppedBitmap = null;
        FileInputStream fileInputStream = null;
        BitmapRegionDecoder decoder = null;

        try {
            fileInputStream = new FileInputStream(mFile);

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                decoder = BitmapRegionDecoder.newInstance(fileInputStream);
            } else {
                decoder = BitmapRegionDecoder.newInstance(fileInputStream, false);
            }

            if (decoder != null) {
                croppedBitmap = decoder.decodeRegion(visibleRect, null);
            }

        } catch (Exception e) {
            Logger.e(TAG, "getClippedBitmap: failed: " + e);
        } catch (OutOfMemoryError oom) {
            Logger.e(TAG, "getClippedBitmap: system ran out of memory decoding region: " + oom);
        } finally {
            try {
                if (decoder != null) {
                    decoder.recycle();
                }
                if (fileInputStream != null) {
                    fileInputStream.close();
                }
            } catch (Exception e) {
                Logger.e(TAG, "getClippedBitmap: cleanup failed: " + e);
            }
        }

        return croppedBitmap;
    }

    public void processFile(@NonNull final Rect visibleRect, final int viewWidth, final int viewHeight, final int token) {
        if (BuildConfig.DEBUG) Logger.d(TAG, "processFile");

        if (mIsDestroyed || token != mCurrentTaskToken || mTextRecognizer == null) {
            return;
        }

        if (!mFile.exists()) {
            Logger.e(TAG, "processFile: file does not exist");
            return;
        }

        final Bitmap bitmap = getClippedBitmap(visibleRect);
        if (bitmap == null) {
            Logger.e(TAG, "processFile: failed to decode image file");
            return;
        }

        try {
            final InputImage image = InputImage.fromBitmap(bitmap, 0);

            mActiveRecognitionTask = mTextRecognizer.process(image);
            mActiveRecognitionTask.addOnSuccessListener(new TextRecognitionSuccessListener(this, mTranslationManager, bitmap, viewWidth, viewHeight, token))
                .addOnFailureListener(new TextRecognitionFailureListener(this, bitmap, token));

        } catch (Exception e) {
            Logger.e(TAG, "TextRecognition: error: " + e);
            if (!bitmap.isRecycled()) {
                bitmap.recycle();
            }
        }
    }

    public boolean onVolumeChanged(final int cmd) {
        if (mSettingsManager.getVolume() == 0 || mSettingsManager.getSpeak() == 0) return false;

        final String currentId = mHashMap.get("current");
        if (currentId == null) return false;

        int index = mArrayList.indexOf(currentId);
        if (index == -1) return false;

        mTts.stop();

        switch(cmd) {
            case 0:
                final long time = SystemClock.uptimeMillis();
                final long delay = time - mLastVolumeUp;
                if (delay > 0 && delay < 1000 && index > 0) {
                    index--;
                }
                mLastVolumeUp = time;
            break;
            case 1:
                index++;
                if (index == mArrayList.size()) {
                    mTextReaderOverlay.clear();
                    return true;
                }
            break;
        }

        final String utteranceId = mArrayList.get(index);
        final String utterance = mHashMap.get(utteranceId);
        mTts.speak(utterance, TextToSpeech.QUEUE_ADD, null, utteranceId);

        return true;
    }

    public void destroy() {
        mIsDestroyed = true;
        mTtsReady = false;
        mTtsStarting = false;

        if (mImageView != null) {
            mImageView.setOnStateChangedListener(null);
            mImageView = null;
        }

        mBackgroundHandler.removeCallbacksAndMessages(null);
        mMainHandler.removeCallbacksAndMessages(null);

        mBackgroundThread.quitSafely();

        if (mTranslationManager != null) {
            mTranslationManager.close();
        }

        if (mTextReaderOverlay != null) {
            mTextReaderOverlay.clear();
            mTextReaderOverlay = null;
        }
        if (mTts != null) {
            mTts.stop();
            mTts.shutdown();
        }

        if (mTextRecognizer != null) {
            mTextRecognizer.close();
        }
    }
}
