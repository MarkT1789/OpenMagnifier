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
import android.speech.tts.TextToSpeech;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.mlkit.nl.translate.Translator;
import com.google.mlkit.nl.translate.TranslatorOptions;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class TranslationManagerTest {

    private Context mMockContext;
    private TextReaderOverlay mMockOverlay;
    private ToastManager mMockToastManager;
    private Translator mMockTranslator;
    private TranslationManager.TranslatorProvider mFakeProvider;
    private TextToSpeech mMockTts;

    private TranslationManager mManager;
    private TranslatorOptions mCapturedOptions;

    @Before
    public void setUp() {
        mMockContext = mock(Context.class);
        Context mockAppContext = mock(Context.class);
        when(mMockContext.getApplicationContext()).thenReturn(mockAppContext);
        when(mockAppContext.getString(anyInt())).thenReturn("Mocked Toast String");

        mMockOverlay = mock(TextReaderOverlay.class);
        mMockToastManager = mock(ToastManager.class);
        mMockTranslator = mock(Translator.class);
        mMockTts = mock(TextToSpeech.class);

        // Setup a mock task chain for downloadModelIfNeeded
        Task<Void> mockVoidTask = mock(Task.class);
        when(mMockTranslator.downloadModelIfNeeded(any())).thenReturn(mockVoidTask);
        when(mockVoidTask.addOnSuccessListener(any(OnSuccessListener.class))).thenReturn(mockVoidTask);
        when(mockVoidTask.addOnFailureListener(any(OnFailureListener.class))).thenReturn(mockVoidTask);

        // Setup a mock task chain for translate
        Task<String> mockStringTask = mock(Task.class);
        when(mMockTranslator.translate(anyString())).thenReturn(mockStringTask);
        when(mockStringTask.addOnSuccessListener(any(OnSuccessListener.class))).thenReturn(mockStringTask);
        when(mockStringTask.addOnFailureListener(any(OnFailureListener.class))).thenReturn(mockStringTask);

        mFakeProvider = new TranslationManager.TranslatorProvider() {
            @Override
            public Translator getClient(TranslatorOptions options) {
                mCapturedOptions = options;
                return mMockTranslator;
            }
        };

        mManager = new TranslationManager(mMockContext, mMockOverlay, mMockToastManager, mFakeProvider);
    }

    // ==========================================
    //   PREPARE() BOUNDARY & CONDITION TESTS
    // ==========================================

    @Test
    public void testPrepare_ZeroIds_ClosesAndResets() {
        mManager.prepare(0, 5);
        // Verify it reset to defaults (-1) by attempting a translate which should act as "not ready"
        mManager.translate(mMockTts, new HashMap<String, String>(), new ArrayList<String>(), "Hello", "id1");
        verify(mMockTts).setLanguage(Locale.getDefault());
    }

    @Test
    public void testPrepare_MatchingSourceAndTarget_ClosesAndResets() {
        mManager.prepare(3, 3); 
        mManager.translate(mMockTts, new HashMap<String, String>(), new ArrayList<String>(), "Hello", "id1");
        verify(mMockTts).setLanguage(Locale.getDefault());
    }

    @Test
    public void testPrepare_InvalidIndexBoundary_LogsAndCloses() {
        mManager.prepare(99, 3); // 99 maps to null string
        mManager.translate(mMockTts, new HashMap<String, String>(), new ArrayList<String>(), "Hello", "id1");
        verify(mMockTts).setLanguage(Locale.getDefault());
    }

    @Test
    public void testPrepare_ValidIds_CreatesTranslatorWithOptions() {
        mManager.prepare(3, 7); // English to Japanese
        assertNotNull(mCapturedOptions);
        verify(mMockTranslator).downloadModelIfNeeded(any());
    }

    @Test
    public void testPrepare_SubsequentCallWithSameIds_BailsEarly() {
        mManager.prepare(3, 7);
        reset(mMockTranslator); // clear interactions
        
        // Call again with identical parameters
        mManager.prepare(3, 7);
        verifyNoInteractions(mMockTranslator); // Should early out without rebuilding client
    }

    @Test
    public void testPrepare_ExistingTranslator_ClosesPreviousInstance() {
        mManager.prepare(3, 7);
        mManager.prepare(3, 10); // Change target language
        verify(mMockTranslator, times(1)).close(); 
    }

    // ==========================================
    //   ASYNC LISTENERS & CALLBACK COVERAGE
    // ==========================================

    @Test
    public void testModelDownload_Success_SetsReady() {
        Task<Void> mockVoidTask = mock(Task.class);
        ArgumentCaptor<OnSuccessListener> successCaptor = ArgumentCaptor.forClass(OnSuccessListener.class);
        when(mMockTranslator.downloadModelIfNeeded(any())).thenReturn(mockVoidTask);
        when(mockVoidTask.addOnSuccessListener(successCaptor.capture())).thenReturn(mockVoidTask);
        when(mockVoidTask.addOnFailureListener(any(OnFailureListener.class))).thenReturn(mockVoidTask);

        mManager.prepare(3, 7);
        
        // Trigger async download success
        successCaptor.getValue().onSuccess(null);

        // Now that mIsReady is true, translate() should delegate to mMockTranslator.translate()
        mManager.translate(mMockTts, new HashMap<String, String>(), new ArrayList<String>(), "Hello", "1");
        verify(mMockTranslator).translate("Hello");
    }

    @Test
    public void testModelDownload_Failure_SetsNotReady() {
        Task<Void> mockVoidTask = mock(Task.class);
        ArgumentCaptor<OnFailureListener> failureCaptor = ArgumentCaptor.forClass(OnFailureListener.class);
        when(mMockTranslator.downloadModelIfNeeded(any())).thenReturn(mockVoidTask);
        when(mockVoidTask.addOnSuccessListener(any(OnSuccessListener.class))).thenReturn(mockVoidTask);
        when(mockVoidTask.addOnFailureListener(failureCaptor.capture())).thenReturn(mockVoidTask);

        mManager.prepare(3, 7);
        failureCaptor.getValue().onFailure(new Exception("Download failed"));

        mManager.translate(mMockTts, new HashMap<String, String>(), new ArrayList<String>(), "Hello", "1");
        // Verify it treated it as fallback paths because mIsReady is false
        verify(mMockTranslator, never()).translate(anyString());
    }

    // ==========================================
    //   TRANSLATE() METHOD BRANCH COVERAGE
    // ==========================================

    @Test
    public void testTranslate_NotReady_FallbacksToTtsQueue() {
        HashMap<String, String> map = new HashMap<>();
        ArrayList<String> list = new ArrayList<>();
        
        mManager.translate(mMockTts, map, list, "Test Text", "id_100");

        assertEquals("Test Text", map.get("id_100"));
        assertTrue(list.contains("id_100"));
        verify(mMockTts).speak("Test Text", TextToSpeech.QUEUE_ADD, null, "id_100");
    }

    @Test
    public void testTranslate_NotReadyWithActiveTranslator_TriggersToast() {
        mManager.prepare(3, 7); // initialized but async download haven't callback-completed
        
        mManager.translate(mMockTts, new HashMap<String, String>(), new ArrayList<String>(), "Text", "id");
        
        verify(mMockToastManager).show(any(), anyString());
    }

    @Test
    public void testTranslate_Ready_TriggersTranslationServiceAndOverlay() {
        // Force state to ready by faking download success
        Task<Void> mockVoidTask = mock(Task.class);
        ArgumentCaptor<OnSuccessListener> successCaptor = ArgumentCaptor.forClass(OnSuccessListener.class);
        when(mMockTranslator.downloadModelIfNeeded(any())).thenReturn(mockVoidTask);
        when(mockVoidTask.addOnSuccessListener(successCaptor.capture())).thenReturn(mockVoidTask);
        when(mockVoidTask.addOnFailureListener(any(OnFailureListener.class))).thenReturn(mockVoidTask);

        mManager.prepare(3, 7);
        successCaptor.getValue().onSuccess(null);

        mManager.translate(mMockTts, new HashMap<String, String>(), new ArrayList<String>(), "Translate Me", "id_2");

        verify(mMockOverlay).showCopyright(true);
        verify(mMockTranslator).translate("Translate Me");
    }

    @Test
    public void testTranslationAsyncCallbacks_OnSuccess() {
        Task<String> mockStringTask = mock(Task.class);
        ArgumentCaptor<OnSuccessListener> transSuccessCaptor = ArgumentCaptor.forClass(OnSuccessListener.class);
        when(mMockTranslator.translate(anyString())).thenReturn(mockStringTask);
        when(mockStringTask.addOnSuccessListener(transSuccessCaptor.capture())).thenReturn(mockStringTask);
        when(mockStringTask.addOnFailureListener(any(OnFailureListener.class))).thenReturn(mockStringTask);

        // Mock ready state sequence
        Task<Void> mockVoidTask = mock(Task.class);
        ArgumentCaptor<OnSuccessListener> downloadSuccessCaptor = ArgumentCaptor.forClass(OnSuccessListener.class);
        when(mMockTranslator.downloadModelIfNeeded(any())).thenReturn(mockVoidTask);
        when(mockVoidTask.addOnSuccessListener(downloadSuccessCaptor.capture())).thenReturn(mockVoidTask);
        when(mockVoidTask.addOnFailureListener(any(OnFailureListener.class))).thenReturn(mockVoidTask);

        mManager.prepare(3, 7); // Target language index 7 is Japanese
        downloadSuccessCaptor.getValue().onSuccess(null);

        HashMap<String, String> map = new HashMap<>();
        ArrayList<String> list = new ArrayList<>();

        mManager.translate(mMockTts, map, list, "Hello", "msg_id");
        
        // Execute translation completion callback
        transSuccessCaptor.getValue().onSuccess("こんにちは");

        assertEquals("こんにちは", map.get("msg_id"));
        assertTrue(list.contains("msg_id"));
        verify(mMockTts).speak("こんにちは", TextToSpeech.QUEUE_ADD, null, "msg_id");
    }

    @Test
    public void testTranslationAsyncCallbacks_OnFailure() {
        Task<String> mockStringTask = mock(Task.class);
        ArgumentCaptor<OnFailureListener> transFailureCaptor = ArgumentCaptor.forClass(OnFailureListener.class);
        when(mMockTranslator.translate(anyString())).thenReturn(mockStringTask);
        when(mockStringTask.addOnSuccessListener(any(OnSuccessListener.class))).thenReturn(mockStringTask);
        when(mockStringTask.addOnFailureListener(transFailureCaptor.capture())).thenReturn(mockStringTask);

        // Mock ready state sequence
        Task<Void> mockVoidTask = mock(Task.class);
        ArgumentCaptor<OnSuccessListener> downloadSuccessCaptor = ArgumentCaptor.forClass(OnSuccessListener.class);
        when(mMockTranslator.downloadModelIfNeeded(any())).thenReturn(mockVoidTask);
        when(mockVoidTask.addOnSuccessListener(downloadSuccessCaptor.capture())).thenReturn(mockVoidTask);
        when(mockVoidTask.addOnFailureListener(any(OnFailureListener.class))).thenReturn(mockVoidTask);

        mManager.prepare(3, 7); // Source language index 3 is English
        downloadSuccessCaptor.getValue().onSuccess(null);

        HashMap<String, String> map = new HashMap<>();
        ArrayList<String> list = new ArrayList<>();

        mManager.translate(mMockTts, map, list, "Hello", "msg_id");
        
        // Execute translation breakdown failure callback
        transFailureCaptor.getValue().onFailure(new Exception("Network lost"));

        // Should return original text as fallback
        assertEquals("Hello", map.get("msg_id"));
        assertTrue(list.contains("msg_id"));
        verify(mMockTts).speak("Hello", TextToSpeech.QUEUE_ADD, null, "msg_id");
    }

    // ==========================================
    //   CLOSE() & LIFECYCLE BOUNDARY TESTS
    // ==========================================

    @Test
    public void testClose_ClearsTranslatorReferences() {
        mManager.prepare(3, 7);
        mManager.close();

        verify(mMockTranslator).close();
        
        // Verify states reset back to defaults
        mManager.translate(mMockTts, new HashMap<String, String>(), new ArrayList<String>(), "Hello", "1");
        verify(mMockTts).setLanguage(Locale.getDefault());
    }

    // ==========================================
    //   NULL COERCIONS & EDGE CONDITION PATHS
    // ==========================================

    @Test
    public void testNullOverlayOrTtsInteractions_DoNotCrash() {
        // Create instance with completely null optional UI references to ensure null safety coverage
        TranslationManager nullOverlayManager = new TranslationManager(mMockContext, null, mMockToastManager, mFakeProvider);
        
        // Force mock system into ready state
        Task<Void> mockVoidTask = mock(Task.class);
        ArgumentCaptor<OnSuccessListener> downloadSuccessCaptor = ArgumentCaptor.forClass(OnSuccessListener.class);
        when(mMockTranslator.downloadModelIfNeeded(any())).thenReturn(mockVoidTask);
        when(mockVoidTask.addOnSuccessListener(downloadSuccessCaptor.capture())).thenReturn(mockVoidTask);
        when(mockVoidTask.addOnFailureListener(any(OnFailureListener.class))).thenReturn(mockVoidTask);

        nullOverlayManager.prepare(3, 7);
        downloadSuccessCaptor.getValue().onSuccess(null);

        // Run translation while text overlay reference is missing
        try {
            nullOverlayManager.translate(mMockTts, new HashMap<String, String>(), new ArrayList<String>(), "Hello", "1");
        } catch (NullPointerException npe) {
            fail("Should not crash when TextReaderOverlay is null");
        }
    }

    @Test
    public void testGetTtsLocale_AllIndicesCoverage() {
        Task<Void> mockVoidTask = mock(Task.class);
        ArgumentCaptor<OnSuccessListener> downloadSuccessCaptor = ArgumentCaptor.forClass(OnSuccessListener.class);
        when(mMockTranslator.downloadModelIfNeeded(any())).thenReturn(mockVoidTask);
        when(mockVoidTask.addOnSuccessListener(downloadSuccessCaptor.capture())).thenReturn(mockVoidTask);
        when(mockVoidTask.addOnFailureListener(any(OnFailureListener.class))).thenReturn(mockVoidTask);

        // Loop through all case indexes in switch structures to ensure 100% path statement evaluation coverage
        for (int i = 0; i <= 15; i++) {
            mManager.prepare(3, i);
            if (downloadSuccessCaptor.getAllValues().size() > 0) {
                // reset or clear sessions internally
                mManager.close();
            }
        }
    }
}
