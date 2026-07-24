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
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.view.View;

import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView;
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

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLooper;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 33)
public class TextReaderTest {

    private Context mContext;
    private TextReader mTextReader;
    private File mFakeCacheFile;

    @Mock private SubsamplingScaleImageView mockImageView;
    @Mock private TextReaderOverlay mockOverlay;
    @Mock private SettingsManager mockSettings;
    @Mock private ITranslationManager mockTranslationManager;
    @Mock private TextRecognizer mockTextRecognizer;
    @Mock private Task<Text> mockTask;
    @Mock private TextToSpeech mockTts;

    @Before
    public void setUp() throws IOException {
        MockitoAnnotations.openMocks(this);
        mContext = RuntimeEnvironment.getApplication();

        mFakeCacheFile = new File(mContext.getCacheDir(), "test_file.jpg");
        createDummyImageFile(mFakeCacheFile);

        when(mockSettings.getSpeak()).thenReturn(1);
        when(mockSettings.getSource()).thenReturn(10);
        when(mockSettings.getDest()).thenReturn(20);
        when(mockSettings.getVolume()).thenReturn(1);
        when(mockSettings.getBanner()).thenReturn(1);

        mTextReader = new TextReader(
                mContext,
                mockImageView,
                mockOverlay,
                "test_file.jpg",
                mockSettings,
                mockTranslationManager
        );
    }

    @After
    public void tearDown() {
        if (mTextReader != null) {
            mTextReader.destroy();
        }
        if (mFakeCacheFile != null && mFakeCacheFile.exists()) {
            mFakeCacheFile.delete();
        }
    }

    private void createDummyImageFile(File file) throws IOException {
        Bitmap bmp = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888);
        FileOutputStream fos = new FileOutputStream(file);
        bmp.compress(Bitmap.CompressFormat.JPEG, 100, fos);
        fos.flush();
        fos.close();
    }

    private void injectMockTts(TextReader reader, TextToSpeech tts) throws Exception {
        Field ttsField = TextReader.class.getDeclaredField("mTts");
        ttsField.setAccessible(true);
        ttsField.set(reader, tts);

        Field readyField = TextReader.class.getDeclaredField("mTtsReady");
        readyField.setAccessible(true);
        readyField.set(reader, true);
    }

    private void injectMockRecognizer(TextReader reader, TextRecognizer recognizer) throws Exception {
        Field recField = TextReader.class.getDeclaredField("mTextRecognizer");
        recField.setAccessible(true);
        recField.set(reader, recognizer);
    }

    // ==========================================
    // 1. SETUP TEXT RECOGNIZER BRANCHES
    // ==========================================

    @Test
    public void testSetupTextRecognizer_AllLanguages() throws Exception {
        try (MockedStatic<TextRecognition> mockedStatic = mockStatic(TextRecognition.class)) {
            mockedStatic.when(() -> TextRecognition.getClient(any(TextRecognizerOptions.class))).thenReturn(mockTextRecognizer);
            mockedStatic.when(() -> TextRecognition.getClient(any(ChineseTextRecognizerOptions.class))).thenReturn(mockTextRecognizer);
            mockedStatic.when(() -> TextRecognition.getClient(any(DevanagariTextRecognizerOptions.class))).thenReturn(mockTextRecognizer);
            mockedStatic.when(() -> TextRecognition.getClient(any(JapaneseTextRecognizerOptions.class))).thenReturn(mockTextRecognizer);

            // 1. Latin
            when(mockSettings.getSpeak()).thenReturn(1);
            mTextReader.start();
            verify(mockTranslationManager).prepare(10, 20);

            // 2. Chinese
            when(mockSettings.getSpeak()).thenReturn(2);
            mTextReader.start();

            // 3. Devanagari
            when(mockSettings.getSpeak()).thenReturn(3);
            mTextReader.start();

            // 4. Japanese
            when(mockSettings.getSpeak()).thenReturn(4);
            mTextReader.start();

            // Speak = 0 (Off)
            when(mockSettings.getSpeak()).thenReturn(0);
            mTextReader.start();

            // Same speak ID (Early Return)
            mTextReader.start();
            assertTrue(true);
        }
    }

    // ==========================================
    // 2. MESSAGE HANDLING BRANCHES
    // ==========================================

    @Test
    public void testHandleMessage_VariousStates() throws Exception {
        Message msg = Message.obtain();
        msg.what = 1; // MSG_SPEAK

        // State 1: mTtsStarting = true, mTtsReady = false
        Field startingField = TextReader.class.getDeclaredField("mTtsStarting");
        startingField.setAccessible(true);
        startingField.set(mTextReader, true);

        mTextReader.handleMessage(msg);

        // State 2: mTtsReady = true, destroyed = false -> calls shouldSpeak()
        startingField.set(mTextReader, false);
        injectMockTts(mTextReader, mockTts);

        when(mockImageView.isReady()).thenReturn(true);
        when(mockImageView.getVisibility()).thenReturn(View.VISIBLE);
        doAnswer(invocation -> {
            Rect r = invocation.getArgument(0);
            r.set(0, 0, 50, 50);
            return null;
        }).when(mockImageView).visibleFileRect(any(Rect.class));

        mTextReader.handleMessage(msg);

        // State 3: Unknown message
        msg.what = 999;
        assertFalse(mTextReader.handleMessage(msg));
    }

    @Test
    public void testShouldSpeak_BypassConditions() throws Exception {
        injectMockTts(mTextReader, mockTts);

        Message msg = Message.obtain();
        msg.what = 1;

        // Sub-case A: ImageView not ready
        when(mockImageView.isReady()).thenReturn(false);
        when(mockImageView.getVisibility()).thenReturn(View.VISIBLE);

        mTextReader.handleMessage(msg);
        verify(mockTts, atLeastOnce()).stop();

        // Sub-case B: Invisible View
        when(mockImageView.isReady()).thenReturn(true);
        when(mockImageView.getVisibility()).thenReturn(View.GONE);

        mTextReader.handleMessage(msg);
        verify(mockTts, times(2)).stop();

        // Sub-case C: Empty Rect
        when(mockImageView.getVisibility()).thenReturn(View.VISIBLE);
        doAnswer(invocation -> {
            Rect r = invocation.getArgument(0);
            r.set(0, 0, 0, 0);
            return null;
        }).when(mockImageView).visibleFileRect(any(Rect.class));

        mTextReader.handleMessage(msg);
    }

    // ==========================================
    // 3. IMAGE STATE LISTENER & DESTROY BRANCHES
    // ==========================================

    @Test
    public void testImageStateListener_AndDestroyedState() throws Exception {
        ArgumentCaptor<SubsamplingScaleImageView.OnStateChangedListener> captor =
                ArgumentCaptor.forClass(SubsamplingScaleImageView.OnStateChangedListener.class);
        verify(mockImageView).setOnStateChangedListener(captor.capture());
        SubsamplingScaleImageView.OnStateChangedListener listener = captor.getValue();

        injectMockTts(mTextReader, mockTts);
        when(mockSettings.getSpeak()).thenReturn(0);

        listener.onScaleChanged(2.0f, 1);
        verify(mockOverlay).clear();
        verify(mockTts).stop();

        listener.onCenterChanged(new PointF(0, 0), 1);
        verify(mockOverlay, times(2)).clear();
        verify(mockTts, times(2)).stop();

        // Destroy and verify listener ignores callbacks when reader is destroyed
        mTextReader.destroy();
        listener.onScaleChanged(3.0f, 1);
        listener.onCenterChanged(new PointF(10, 10), 1);
    }

    // ==========================================
    // 4. TTS INNER LISTENERS (REFLECTION)
    // ==========================================

    @Test
    public void testTtsInitListener_AllBranches() throws Exception {
        Class<?> initListenerClass = Class.forName("io.uglydog.magnifier.TextReader$TtsInitListener");
        Constructor<?> constructor = initListenerClass.getDeclaredConstructor(TextReader.class, Locale.class);
        constructor.setAccessible(true);

        // Test normal lifecycle
        TextToSpeech.OnInitListener listener = (TextToSpeech.OnInitListener) constructor.newInstance(mTextReader, Locale.US);
        injectMockTts(mTextReader, mockTts);

        // LANG_MISSING_DATA
        when(mockTts.setLanguage(Locale.US)).thenReturn(TextToSpeech.LANG_MISSING_DATA);
        listener.onInit(TextToSpeech.SUCCESS);

        // LANG_NOT_SUPPORTED
        when(mockTts.setLanguage(Locale.US)).thenReturn(TextToSpeech.LANG_NOT_SUPPORTED);
        listener.onInit(TextToSpeech.SUCCESS);

        // SUCCESS branch
        when(mockTts.setLanguage(Locale.US)).thenReturn(TextToSpeech.LANG_AVAILABLE);
        listener.onInit(TextToSpeech.SUCCESS);

        // ERROR branch
        listener.onInit(TextToSpeech.ERROR);

        // WeakReference GC / Destroyed branch
        TextReader nullReader = new TextReader(mContext, mockImageView, mockOverlay, "test_file.jpg", mockSettings, mockTranslationManager);
        TextToSpeech.OnInitListener listenerNull = (TextToSpeech.OnInitListener) constructor.newInstance(nullReader, Locale.US);
        nullReader.destroy();
        listenerNull.onInit(TextToSpeech.SUCCESS);
    }

    @Test
    public void testTtsProgressListener_AllCallbacks() throws Exception {
        Class<?> progListenerClass = Class.forName("io.uglydog.magnifier.TextReader$TtsProgressListener");
        Constructor<?> constructor = progListenerClass.getDeclaredConstructor(TextReader.class);
        constructor.setAccessible(true);
        UtteranceProgressListener listener = (UtteranceProgressListener) constructor.newInstance(mTextReader);

        Field mapField = TextReader.class.getDeclaredField("mHashMap");
        mapField.setAccessible(true);
        HashMap<String, String> map = (HashMap<String, String>) mapField.get(mTextReader);

        // Set mIsRunning to true so onRangeStart condition passes
        Field runningField = TextReader.class.getDeclaredField("mIsRunning");
        runningField.setAccessible(true);
        runningField.set(mTextReader, true);

        // Standard test
        map.put("10:10:20:20", "Test Banner Text");
        listener.onStart("10:10:20:20");
        listener.onRangeStart("10:10:20:20", 0, 4, 0);
        listener.onDone("10:10:20:20");
        listener.onError("10:10:20:20");

        ShadowLooper.runUiThreadTasksIncludingDelayedTasks();

        verify(mockOverlay).setRect(any(Rect.class));
        verify(mockOverlay).setText("Test Banner Text", 0, 4);
        verify(mockOverlay, times(2)).clear();

        // Edge case: banner = 0, invalid coordinates length != 4
        when(mockSettings.getBanner()).thenReturn(0);
        listener.onStart("invalid_id");
        listener.onRangeStart("10:10:20:20", 0, 4, 0);

        // WeakReference / null / destroyed branch
        TextReader destroyedReader = new TextReader(mContext, mockImageView, mockOverlay, "test_file.jpg", mockSettings, mockTranslationManager);
        UtteranceProgressListener destroyedListener = (UtteranceProgressListener) constructor.newInstance(destroyedReader);
        destroyedReader.destroy();

        destroyedListener.onStart("10:10:20:20");
        destroyedListener.onRangeStart("10:10:20:20", 0, 4, 0);
        destroyedListener.onDone("10:10:20:20");
        destroyedListener.onError("10:10:20:20");

        ShadowLooper.runUiThreadTasksIncludingDelayedTasks();
    }

    // ==========================================
    // 5. ML KIT LISTENERS & SORTING MATRIX
    // ==========================================

    @Test
    public void testTextRecognitionSuccessListener_SortingAndEdgeCases() throws Exception {
        Class<?> successClass = Class.forName("io.uglydog.magnifier.TextReader$TextRecognitionSuccessListener");
        Constructor<?> constructor = successClass.getDeclaredConstructor(TextReader.class, ITranslationManager.class, Bitmap.class, int.class, int.class, int.class);
        constructor.setAccessible(true);

        Bitmap dummyBitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888);
        injectMockTts(mTextReader, mockTts);
        when(mockImageView.isReady()).thenReturn(true);

        OnSuccessListener<Text> listener = (OnSuccessListener<Text>) constructor.newInstance(mTextReader, mockTranslationManager, dummyBitmap, 1080, 1920, 0);

        // Mock Blocks, Lines, Corner Points
        Text mockText = mock(Text.class);

        Text.TextBlock block1 = mock(Text.TextBlock.class);
        Text.TextBlock block2 = mock(Text.TextBlock.class);
        Text.Line line1 = mock(Text.Line.class);

        when(block1.getBoundingBox()).thenReturn(new Rect(10, 10, 50, 50));
        when(block1.getText()).thenReturn("Text One");
        when(block1.getCornerPoints()).thenReturn(new Point[]{new Point(0, 0), new Point(10, 0), new Point(10, 10), new Point(0, 10)});
        when(block1.getLines()).thenReturn(Collections.singletonList(line1));
        when(line1.getCornerPoints()).thenReturn(new Point[]{new Point(0, 0), new Point(10, 2)}); // skewed line

        when(block2.getBoundingBox()).thenReturn(new Rect(10, 60, 50, 100));
        when(block2.getText()).thenReturn("Text Two");
        when(block2.getCornerPoints()).thenReturn(null); // Force fallback bounding box midpoints calculation
        when(block2.getLines()).thenReturn(Collections.emptyList());

        List<Text.TextBlock> blocks = new ArrayList<>(Arrays.asList(block1, block2, null));
        when(mockText.getTextBlocks()).thenReturn(blocks);

        listener.onSuccess(mockText);

        verify(mockTts).stop();
        verify(mockTranslationManager, atLeastOnce()).translate(any(), any(), any(), anyString(), anyString());
        assertTrue(dummyBitmap.isRecycled());
    }

    @Test
    public void testTextRecognitionSuccessListener_BypassAndTokenMismatch() throws Exception {
        Class<?> successClass = Class.forName("io.uglydog.magnifier.TextReader$TextRecognitionSuccessListener");
        Constructor<?> constructor = successClass.getDeclaredConstructor(TextReader.class, ITranslationManager.class, Bitmap.class, int.class, int.class, int.class);
        constructor.setAccessible(true);

        Bitmap dummyBitmap = Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888);

        // Token mismatch (listener token = 999, currentTaskToken = 0)
        OnSuccessListener<Text> listener = (OnSuccessListener<Text>) constructor.newInstance(mTextReader, mockTranslationManager, dummyBitmap, 1080, 1920, 999);
        listener.onSuccess(mock(Text.class));

        assertTrue(dummyBitmap.isRecycled());
    }

    @Test
    public void testTextRecognitionFailureListener_AllBranches() throws Exception {
        Class<?> failureClass = Class.forName("io.uglydog.magnifier.TextReader$TextRecognitionFailureListener");
        Constructor<?> constructor = failureClass.getDeclaredConstructor(TextReader.class, Bitmap.class, int.class);
        constructor.setAccessible(true);

        Bitmap dummyBitmap = Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888);
        OnFailureListener listener = (OnFailureListener) constructor.newInstance(mTextReader, dummyBitmap, 0);

        listener.onFailure(new Exception("ML Kit Error"));
        assertTrue(dummyBitmap.isRecycled());

        // Token Mismatch / Destroyed branch
        Bitmap dummyBitmap2 = Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888);
        OnFailureListener listenerMismatch = (OnFailureListener) constructor.newInstance(mTextReader, dummyBitmap2, 999);
        listenerMismatch.onFailure(new Exception("Mismatch"));
        assertTrue(dummyBitmap2.isRecycled());
    }

    // ==========================================
    // 6. PROCESS FILE & BITMAP DECODER BRANCHES
    // ==========================================

    @Test
    public void testProcessFile_SuccessAndLegacySdk() throws Exception {
        injectMockRecognizer(mTextReader, mockTextRecognizer);
        when(mockTextRecognizer.process(any(InputImage.class))).thenReturn(mockTask);

        try (MockedStatic<InputImage> mockedStaticInput = mockStatic(InputImage.class);
             MockedStatic<BitmapRegionDecoder> mockedDecoderStatic = mockStatic(BitmapRegionDecoder.class)) {

            mockedStaticInput.when(() -> InputImage.fromBitmap(any(Bitmap.class), anyInt()))
                    .thenReturn(mock(InputImage.class));

            BitmapRegionDecoder mockDecoder = mock(BitmapRegionDecoder.class);
            mockedDecoderStatic.when(() -> BitmapRegionDecoder.newInstance(any(FileInputStream.class)))
                    .thenReturn(mockDecoder);
            mockedDecoderStatic.when(() -> BitmapRegionDecoder.newInstance(any(FileInputStream.class), anyBoolean()))
                    .thenReturn(mockDecoder);
            when(mockDecoder.decodeRegion(any(Rect.class), any())).thenReturn(Bitmap.createBitmap(50, 50, Bitmap.Config.ARGB_8888));

            Rect validRect = new Rect(0, 0, 50, 50);
            mTextReader.processFile(validRect, 1080, 1920, 0);

            verify(mockTextRecognizer).process(any(InputImage.class));
            verify(mockTask).addOnSuccessListener(any());
        }
    }

    @Test
    public void testProcessFile_ExceptionsAndNullBitmap() throws Exception {
        injectMockRecognizer(mTextReader, mockTextRecognizer);

        // Sub-case 1: Missing file
        mFakeCacheFile.delete();
        mTextReader.processFile(new Rect(0, 0, 50, 50), 1080, 1920, 0);
        verify(mockTextRecognizer, never()).process(any(InputImage.class));

        // Sub-case 2: Corrupted file causing decoding exception/null
        createDummyImageFile(mFakeCacheFile);
        FileOutputStream fos = new FileOutputStream(mFakeCacheFile);
        fos.write(new byte[]{0, 1, 2, 3});
        fos.close();

        mTextReader.processFile(new Rect(0, 0, 50, 50), 1080, 1920, 0);
        verify(mockTextRecognizer, never()).process(any(InputImage.class));
    }

    // ==========================================
    // 7. VOLUME CONTROL & NAVIGATION BRANCHES
    // ==========================================

    @Test
    public void testOnVolumeChanged_AllBranches() throws Exception {
        injectMockTts(mTextReader, mockTts);

        Field mapField = TextReader.class.getDeclaredField("mHashMap");
        mapField.setAccessible(true);
        HashMap<String, String> map = (HashMap<String, String>) mapField.get(mTextReader);

        Field listField = TextReader.class.getDeclaredField("mArrayList");
        listField.setAccessible(true);
        ArrayList<String> list = (ArrayList<String>) listField.get(mTextReader);

        Field lastVolField = TextReader.class.getDeclaredField("mLastVolumeUp");
        lastVolField.setAccessible(true);

        // Sub-case A: Disabled volume / speak setting
        when(mockSettings.getVolume()).thenReturn(0);
        assertFalse(mTextReader.onVolumeChanged(0));

        when(mockSettings.getVolume()).thenReturn(1);
        when(mockSettings.getSpeak()).thenReturn(0);
        assertFalse(mTextReader.onVolumeChanged(0));

        when(mockSettings.getSpeak()).thenReturn(1);

        // Sub-case B: Missing current ID or index missing
        assertFalse(mTextReader.onVolumeChanged(0));

        map.put("current", "missing_id");
        assertFalse(mTextReader.onVolumeChanged(0));

        // Populate state
        map.put("current", "id2");
        map.put("id1", "Text 1");
        map.put("id2", "Text 2");
        map.put("id3", "Text 3");
        list.add("id1");
        list.add("id2");
        list.add("id3");

        // Cmd 0 (Backwards with delay > 1000)
        lastVolField.set(mTextReader, -10000L);
        assertTrue(mTextReader.onVolumeChanged(0));
        verify(mockTts).speak("Text 2", TextToSpeech.QUEUE_ADD, null, "id2");

        // Cmd 0 (Backwards double-press with delay < 1000)
        lastVolField.set(mTextReader, SystemClock.uptimeMillis() - 300L);
        assertTrue(mTextReader.onVolumeChanged(0));
        verify(mockTts).speak("Text 1", TextToSpeech.QUEUE_ADD, null, "id1");

        // Cmd 1 (Forwards)
        assertTrue(mTextReader.onVolumeChanged(1));
        verify(mockTts).speak("Text 3", TextToSpeech.QUEUE_ADD, null, "id3");

        // Cmd 1 (Reached array end)
        map.put("current", "id3");
        assertTrue(mTextReader.onVolumeChanged(1));
        verify(mockOverlay).clear();
    }

    // ==========================================
    // 8. STOP & DESTROY LIFECYCLE BRANCHES
    // ==========================================

    @Test
    public void testStopAndDestroy() throws Exception {
        injectMockTts(mTextReader, mockTts);
        injectMockRecognizer(mTextReader, mockTextRecognizer);

        mTextReader.stop();

        verify(mockTextRecognizer).close();
        verify(mockTranslationManager).close();
        verify(mockTts).stop();
        verify(mockOverlay).clearOverlay();

        mTextReader.destroy();

        verify(mockTts).shutdown();
    }
}
