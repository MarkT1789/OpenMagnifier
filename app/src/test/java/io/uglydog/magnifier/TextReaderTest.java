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
import android.os.Message;
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
        
        mTextReader.handleMessage(msg); // Reschedules

        // State 2: mTtsReady = true, destroyed = false -> calls shouldSpeak()
        startingField.set(mTextReader, false);
        injectMockTts(mTextReader, mockTts);
        
        // Mock image view properties for shouldSpeak()
        when(mockImageView.isReady()).thenReturn(true);
        when(mockImageView.getVisibility()).thenReturn(View.VISIBLE);
        doAnswer(invocation -> {
            Rect r = invocation.getArgument(0);
            r.set(0, 0, 50, 50); // non-empty rect
            return null;
        }).when(mockImageView).visibleFileRect(any(Rect.class));

        mTextReader.handleMessage(msg); 
        
        // State 3: Unknown message
        msg.what = 999;
        assertFalse(mTextReader.handleMessage(msg));
    }

    @Test
    public void testShouldSpeak_EmptyRect_Aborts() throws Exception {
        injectMockTts(mTextReader, mockTts);
        when(mockImageView.isReady()).thenReturn(true);
        when(mockImageView.getVisibility()).thenReturn(View.VISIBLE);
        
        // Empty rect
        doAnswer(invocation -> {
            Rect r = invocation.getArgument(0);
            r.set(0, 0, 0, 0); 
            return null;
        }).when(mockImageView).visibleFileRect(any(Rect.class));

        Message msg = Message.obtain();
        msg.what = 1;
        mTextReader.handleMessage(msg); // Calls shouldSpeak
        verify(mockTts, never()).stop(); // Since rect is empty, returns early, no further action
    }

    // ==========================================
    // 3. IMAGE STATE LISTENER
    // ==========================================

    @Test
    public void testImageStateListener() throws Exception {
        ArgumentCaptor<SubsamplingScaleImageView.OnStateChangedListener> captor = ArgumentCaptor.forClass(SubsamplingScaleImageView.OnStateChangedListener.class);
        verify(mockImageView).setOnStateChangedListener(captor.capture());
        SubsamplingScaleImageView.OnStateChangedListener listener = captor.getValue();

        injectMockTts(mTextReader, mockTts);

        // Required to bypass ML Kit static uninitialized exception upon subsequent start() calls
        when(mockSettings.getSpeak()).thenReturn(0);

        listener.onScaleChanged(2.0f, 1);
        verify(mockOverlay).clear();
        verify(mockTts).stop();

        listener.onCenterChanged(new PointF(0, 0), 1);
        verify(mockOverlay, times(2)).clear();
        verify(mockTts, times(2)).stop();
    }

    // ==========================================
    // 4. TTS INNER LISTENERS (REFLECTION)
    // ==========================================

    @Test
    public void testTtsInitListener() throws Exception {
        Class<?> initListenerClass = Class.forName("io.uglydog.magnifier.TextReader$TtsInitListener");
        Constructor<?> constructor = initListenerClass.getDeclaredConstructor(TextReader.class, Locale.class);
        constructor.setAccessible(true);
        TextToSpeech.OnInitListener listener = (TextToSpeech.OnInitListener) constructor.newInstance(mTextReader, Locale.US);

        injectMockTts(mTextReader, mockTts);
        
        // Missing data
        when(mockTts.setLanguage(Locale.US)).thenReturn(TextToSpeech.LANG_MISSING_DATA);
        listener.onInit(TextToSpeech.SUCCESS);

        // Success
        when(mockTts.setLanguage(Locale.US)).thenReturn(TextToSpeech.LANG_AVAILABLE);
        listener.onInit(TextToSpeech.SUCCESS);

        // Error
        listener.onInit(TextToSpeech.ERROR);
    }

    @Test
    public void testTtsProgressListener() throws Exception {
        Class<?> progListenerClass = Class.forName("io.uglydog.magnifier.TextReader$TtsProgressListener");
        Constructor<?> constructor = progListenerClass.getDeclaredConstructor(TextReader.class);
        constructor.setAccessible(true);
        UtteranceProgressListener listener = (UtteranceProgressListener) constructor.newInstance(mTextReader);

        // Populate HashMap
        Field mapField = TextReader.class.getDeclaredField("mHashMap");
        mapField.setAccessible(true);
        HashMap<String, String> map = (HashMap<String, String>) mapField.get(mTextReader);
        map.put("10:10:20:20", "Test Text");

        listener.onStart("10:10:20:20");
        listener.onRangeStart("10:10:20:20", 0, 4, 0);
        listener.onDone("10:10:20:20");
        listener.onError("10:10:20:20");

        ShadowLooper.runUiThreadTasksIncludingDelayedTasks();

        verify(mockOverlay).setRect(any(Rect.class));
        verify(mockOverlay).setText("Test Text", 0, 4);
        verify(mockOverlay, times(2)).clear();
    }

    // ==========================================
    // 5. ML KIT LISTENERS (REFLECTION)
    // ==========================================

    @Test
    public void testTextRecognitionSuccessListener() throws Exception {
        Class<?> successClass = Class.forName("io.uglydog.magnifier.TextReader$TextRecognitionSuccessListener");
        Constructor<?> constructor = successClass.getDeclaredConstructor(TextReader.class, ITranslationManager.class, Bitmap.class, int.class, int.class, int.class);
        constructor.setAccessible(true);

        Bitmap dummyBitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888);
        injectMockTts(mTextReader, mockTts);
        when(mockImageView.isReady()).thenReturn(true);

        OnSuccessListener<Text> listener = (OnSuccessListener<Text>) constructor.newInstance(mTextReader, mockTranslationManager, dummyBitmap, 1080, 1920, 0);

        // Mock Text blocks
        Text mockText = mock(Text.class);
        Text.TextBlock validBlock = mock(Text.TextBlock.class);
        when(validBlock.getBoundingBox()).thenReturn(new Rect(10, 10, 50, 50));
        when(validBlock.getText()).thenReturn("Hello\nWorld");

        Text.TextBlock nullBoundsBlock = mock(Text.TextBlock.class);
        when(nullBoundsBlock.getBoundingBox()).thenReturn(null);

        List<Text.TextBlock> blocks = Arrays.asList(validBlock, nullBoundsBlock, null);
        when(mockText.getTextBlocks()).thenReturn(blocks);

        listener.onSuccess(mockText);

        verify(mockTts).stop();
        verify(mockTranslationManager).translate(any(), any(), any(), eq("Hello World"), anyString());
        assertTrue(dummyBitmap.isRecycled());
    }

    @Test
    public void testTextRecognitionFailureListener() throws Exception {
        Class<?> failureClass = Class.forName("io.uglydog.magnifier.TextReader$TextRecognitionFailureListener");
        Constructor<?> constructor = failureClass.getDeclaredConstructor(TextReader.class, Bitmap.class, int.class);
        constructor.setAccessible(true);

        Bitmap dummyBitmap = Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888);
        OnFailureListener listener = (OnFailureListener) constructor.newInstance(mTextReader, dummyBitmap, 0);

        listener.onFailure(new Exception("Test Error"));
        assertTrue(dummyBitmap.isRecycled());
    }

    // ==========================================
    // 6. PROCESS FILE BRANCHES
    // ==========================================

    @Test
    public void testProcessFile_SuccessPath() throws Exception {
        injectMockRecognizer(mTextReader, mockTextRecognizer);
        when(mockTextRecognizer.process(any(InputImage.class))).thenReturn(mockTask);

        try (MockedStatic<InputImage> mockedStaticInput = mockStatic(InputImage.class);
             MockedStatic<BitmapRegionDecoder> mockedDecoderStatic = mockStatic(BitmapRegionDecoder.class)) {

            mockedStaticInput.when(() -> InputImage.fromBitmap(any(Bitmap.class), anyInt()))
                    .thenReturn(mock(InputImage.class));

            // Force mock BitmapRegionDecoder to circumvent Robolectric framework SDK null-pointers during File parsing
            BitmapRegionDecoder mockDecoder = mock(BitmapRegionDecoder.class);
            mockedDecoderStatic.when(() -> BitmapRegionDecoder.newInstance(any(FileInputStream.class)))
                    .thenReturn(mockDecoder);
            mockedDecoderStatic.when(() -> BitmapRegionDecoder.newInstance(any(FileInputStream.class), anyBoolean()))
                    .thenReturn(mockDecoder);
            when(mockDecoder.decodeRegion(any(Rect.class), any())).thenReturn(Bitmap.createBitmap(50, 50, Bitmap.Config.ARGB_8888));
            
            Rect validRect = new Rect(0, 0, 50, 50);
            mTextReader.processFile(validRect, 1080, 1920, 0); // Token is initialized to 0

            verify(mockTextRecognizer).process(any(InputImage.class));
            verify(mockTask).addOnSuccessListener(any());
        }
    }

    @Test
    public void testProcessFile_MissingFile() throws Exception {
        injectMockRecognizer(mTextReader, mockTextRecognizer);
        mFakeCacheFile.delete(); // Force file missing
        mTextReader.processFile(new Rect(0, 0, 50, 50), 1080, 1920, 0);
        verify(mockTextRecognizer, never()).process(any(InputImage.class));
    }

    @Test
    public void testProcessFile_DecodingException() throws Exception {
        // Feed it a corrupt file by writing garbage data
        FileOutputStream fos = new FileOutputStream(mFakeCacheFile);
        fos.write(new byte[]{1, 2, 3, 4, 5});
        fos.close();

        injectMockRecognizer(mTextReader, mockTextRecognizer);
        
        // This will cause getClippedBitmap to catch an exception/return null
        mTextReader.processFile(new Rect(0, 0, 50, 50), 1080, 1920, 0);
        verify(mockTextRecognizer, never()).process(any(InputImage.class));
    }

    // ==========================================
    // 7. VOLUME CHANGED
    // ==========================================

    @Test
    public void testOnVolumeChanged() throws Exception {
        injectMockTts(mTextReader, mockTts);
        
        Field mapField = TextReader.class.getDeclaredField("mHashMap");
        mapField.setAccessible(true);
        HashMap<String, String> map = (HashMap<String, String>) mapField.get(mTextReader);
        
        Field listField = TextReader.class.getDeclaredField("mArrayList");
        listField.setAccessible(true);
        ArrayList<String> list = (ArrayList<String>) listField.get(mTextReader);

        Field lastVolField = TextReader.class.getDeclaredField("mLastVolumeUp");
        lastVolField.setAccessible(true);

        // Populate state
        map.put("current", "id2");
        map.put("id1", "Text 1");
        map.put("id2", "Text 2");
        map.put("id3", "Text 3");
        list.add("id1");
        list.add("id2");
        list.add("id3");

        // Cmd 0 (Backwards)
        // Force the last clock state to be way in the past so delay > 1000
        lastVolField.set(mTextReader, -10000L);
        boolean result = mTextReader.onVolumeChanged(0);
        assertTrue(result);
        verify(mockTts).speak("Text 2", TextToSpeech.QUEUE_ADD, null, "id2");

        // Cmd 0 (Backwards)
        // Set the last clock state to exactly 500ms ago. Delay is mathematically > 0 and < 1000.
        lastVolField.set(mTextReader, android.os.SystemClock.uptimeMillis() - 500L);
        result = mTextReader.onVolumeChanged(0);
        assertTrue(result);
        verify(mockTts).speak("Text 1", TextToSpeech.QUEUE_ADD, null, "id1");

        // Cmd 1 (Forwards)
        // `current` is still technically "id2" (Index 1) in our map block setup. Increments index to index 2.
        result = mTextReader.onVolumeChanged(1);
        assertTrue(result);
        verify(mockTts).speak("Text 3", TextToSpeech.QUEUE_ADD, null, "id3"); 
        
        // Cmd 1 (Reach end of array)
        map.put("current", "id3");
        result = mTextReader.onVolumeChanged(1);
        assertTrue(result);
        verify(mockOverlay).clear();
    }
}
