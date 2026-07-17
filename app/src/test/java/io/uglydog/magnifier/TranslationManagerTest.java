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

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.mlkit.nl.translate.Translator;
import com.google.mlkit.nl.translate.TranslatorOptions;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedConstruction;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class TranslationManagerTest {

    private Context mMockContext;
    private Context mMockAppContext;
    private TextReaderOverlay mMockOverlay;
    private ToastManager mMockToastManager;
    private Translator mMockTranslator;
    private TranslationManager.TranslatorProvider mFakeProvider;
    private TextToSpeech mMockTts;

    private TranslationManager mManager;
    private TranslatorOptions mCapturedOptions;

    @Before
    @SuppressWarnings("unchecked")
    public void setUp() {
        mMockContext = mock(Context.class);
        mMockAppContext = mock(Context.class);
        when(mMockContext.getApplicationContext()).thenReturn(mMockAppContext);
        when(mMockAppContext.getString(anyInt())).thenReturn("Mocked Toast String");

        mMockOverlay = mock(TextReaderOverlay.class);
        mMockToastManager = mock(ToastManager.class);
        mMockTranslator = mock(Translator.class);
        mMockTts = mock(TextToSpeech.class);

        // Explicitly set up reusable mock task chains to prevent unexpected NullPointerExceptions
        Task<Void> mockVoidTask = mock(Task.class);
        when(mMockTranslator.downloadModelIfNeeded(any())).thenReturn(mockVoidTask);
        when(mockVoidTask.addOnSuccessListener(any(OnSuccessListener.class))).thenReturn(mockVoidTask);
        when(mockVoidTask.addOnFailureListener(any(OnFailureListener.class))).thenReturn(mockVoidTask);

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
        mManager.prepare(99, 3); 
        mManager.translate(mMockTts, new HashMap<String, String>(), new ArrayList<String>(), "Hello", "id1");
        verify(mMockTts).setLanguage(Locale.getDefault());
    }

    @Test
    public void testPrepare_ValidIds_CreatesTranslatorWithOptions() {
        mManager.prepare(3, 7); 
        assertNotNull(mCapturedOptions);
        verify(mMockTranslator).downloadModelIfNeeded(any());
    }

    @Test
    public void testPrepare_SubsequentCallWithSameIds_BailsEarly() {
        mManager.prepare(3, 7);
        reset(mMockTranslator); 
        
        mManager.prepare(3, 7);
        verifyNoInteractions(mMockTranslator); 
    }

    @Test
    public void testPrepare_ExistingTranslator_ClosesPreviousInstance() {
        mManager.prepare(3, 7);
        mManager.prepare(3, 10); 
        verify(mMockTranslator, times(1)).close(); 
    }

    // ==========================================
    //   ASYNC LISTENERS & CALLBACK COVERAGE
    // ==========================================

    @Test
    @SuppressWarnings("unchecked")
    public void testModelDownload_Success_SetsReady() {
        Task<Void> mockVoidTask = mock(Task.class);
        ArgumentCaptor<OnSuccessListener<Void>> successCaptor = ArgumentCaptor.forClass(OnSuccessListener.class);
        when(mMockTranslator.downloadModelIfNeeded(any())).thenReturn(mockVoidTask);
        when(mockVoidTask.addOnSuccessListener(successCaptor.capture())).thenReturn(mockVoidTask);

        mManager.prepare(3, 7);
        successCaptor.getValue().onSuccess(null);

        mManager.translate(mMockTts, new HashMap<String, String>(), new ArrayList<String>(), "Hello", "1");
        verify(mMockTranslator).translate("Hello");
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testModelDownload_Success_WithOutdatedSessionId_DoesNotSetReady() {
        Task<Void> mockVoidTask = mock(Task.class);
        ArgumentCaptor<OnSuccessListener<Void>> successCaptor = ArgumentCaptor.forClass(OnSuccessListener.class);
        when(mMockTranslator.downloadModelIfNeeded(any())).thenReturn(mockVoidTask);
        when(mockVoidTask.addOnSuccessListener(successCaptor.capture())).thenReturn(mockVoidTask);
        when(mockVoidTask.addOnFailureListener(any(OnFailureListener.class))).thenReturn(mockVoidTask);

        mManager.prepare(3, 7); // Session ID = 1
        OnSuccessListener<Void> staleListener = successCaptor.getValue();

        mManager.prepare(3, 10); // Session ID increments to 2

        staleListener.onSuccess(null);

        mManager.translate(mMockTts, new HashMap<String, String>(), new ArrayList<String>(), "Hello", "1");
        verify(mMockTranslator, never()).translate("Hello");
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testModelDownload_Failure_SetsNotReady() {
        Task<Void> mockVoidTask = mock(Task.class);
        ArgumentCaptor<OnFailureListener> failureCaptor = ArgumentCaptor.forClass(OnFailureListener.class);
        when(mMockTranslator.downloadModelIfNeeded(any())).thenReturn(mockVoidTask);
        when(mockVoidTask.addOnSuccessListener(any(OnSuccessListener.class))).thenReturn(mockVoidTask);
        when(mockVoidTask.addOnFailureListener(failureCaptor.capture())).thenReturn(mockVoidTask);

        mManager.prepare(3, 7);
        failureCaptor.getValue().onFailure(new Exception("Download failed"));

        mManager.translate(mMockTts, new HashMap<String, String>(), new ArrayList<String>(), "Hello", "1");
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
    public void testTranslate_NotReady_MapAlreadyContainsId_DoesNotDuplicate() {
        HashMap<String, String> map = new HashMap<>();
        ArrayList<String> list = new ArrayList<>();
        map.put("id_existing", "Pre-existing Value");
        
        mManager.translate(mMockTts, map, list, "New Text", "id_existing");

        assertEquals("Pre-existing Value", map.get("id_existing"));
        assertTrue(list.isEmpty()); 
    }

    @Test
    public void testTranslate_NotReadyWithActiveTranslator_TriggersToast() {
        mManager.prepare(3, 7); 
        mManager.translate(mMockTts, new HashMap<String, String>(), new ArrayList<String>(), "Text", "id");
        verify(mMockToastManager).show(any(), anyString());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testTranslate_Ready_TriggersTranslationServiceAndOverlay() {
        Task<Void> mockVoidTask = mock(Task.class);
        ArgumentCaptor<OnSuccessListener<Void>> successCaptor = ArgumentCaptor.forClass(OnSuccessListener.class);
        when(mMockTranslator.downloadModelIfNeeded(any())).thenReturn(mockVoidTask);
        when(mockVoidTask.addOnSuccessListener(successCaptor.capture())).thenReturn(mockVoidTask);

        mManager.prepare(3, 7);
        successCaptor.getValue().onSuccess(null);

        mManager.translate(mMockTts, new HashMap<String, String>(), new ArrayList<String>(), "Translate Me", "id_2");

        verify(mMockOverlay).showCopyright(true);
        verify(mMockTranslator).translate("Translate Me");
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testTranslationAsyncCallbacks_OnSuccess_HandlesExistingMapKey() {
        Task<String> mockStringTask = mock(Task.class);
        ArgumentCaptor<OnSuccessListener<String>> transSuccessCaptor = ArgumentCaptor.forClass(OnSuccessListener.class);
        when(mMockTranslator.translate(anyString())).thenReturn(mockStringTask);
        when(mockStringTask.addOnSuccessListener(transSuccessCaptor.capture())).thenReturn(mockStringTask);

        Task<Void> mockVoidTask = mock(Task.class);
        ArgumentCaptor<OnSuccessListener<Void>> downloadSuccessCaptor = ArgumentCaptor.forClass(OnSuccessListener.class);
        when(mMockTranslator.downloadModelIfNeeded(any())).thenReturn(mockVoidTask);
        when(mockVoidTask.addOnSuccessListener(downloadSuccessCaptor.capture())).thenReturn(mockVoidTask);

        mManager.prepare(3, 7); 
        downloadSuccessCaptor.getValue().onSuccess(null);

        HashMap<String, String> map = new HashMap<>();
        ArrayList<String> list = new ArrayList<>();
        map.put("msg_id", "Early Value");

        mManager.translate(mMockTts, map, list, "Hello", "msg_id");
        transSuccessCaptor.getValue().onSuccess("こんにちは");

        assertEquals("Early Value", map.get("msg_id"));
        assertFalse(list.contains("msg_id")); 
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testTranslationAsyncCallbacks_OnFailure_HandlesExistingMapKey() {
        Task<String> mockStringTask = mock(Task.class);
        ArgumentCaptor<OnFailureListener> transFailureCaptor = ArgumentCaptor.forClass(OnFailureListener.class);
        when(mMockTranslator.translate(anyString())).thenReturn(mockStringTask);
        when(mockStringTask.addOnSuccessListener(any(OnSuccessListener.class))).thenReturn(mockStringTask);
        when(mockStringTask.addOnFailureListener(transFailureCaptor.capture())).thenReturn(mockStringTask);

        Task<Void> mockVoidTask = mock(Task.class);
        ArgumentCaptor<OnSuccessListener<Void>> downloadSuccessCaptor = ArgumentCaptor.forClass(OnSuccessListener.class);
        when(mMockTranslator.downloadModelIfNeeded(any())).thenReturn(mockVoidTask);
        when(mockVoidTask.addOnSuccessListener(downloadSuccessCaptor.capture())).thenReturn(mockVoidTask);

        mManager.prepare(3, 7); 
        downloadSuccessCaptor.getValue().onSuccess(null);

        HashMap<String, String> map = new HashMap<>();
        ArrayList<String> list = new ArrayList<>();
        map.put("msg_id", "Early Value");

        mManager.translate(mMockTts, map, list, "Hello", "msg_id");
        transFailureCaptor.getValue().onFailure(new Exception("Network lost"));

        assertEquals("Early Value", map.get("msg_id"));
        assertFalse(list.contains("msg_id"));
    }

    // ==========================================
    //   TTS ENGINE AND LOCALIZATION BRANCHES
    // ==========================================

    @Test
    @SuppressWarnings("unchecked")
    public void testGetTtsLocale_ValidatesExactMappings() {
        Task<String> mockStringTask = mock(Task.class);
        ArgumentCaptor<OnFailureListener> transFailureCaptor = ArgumentCaptor.forClass(OnFailureListener.class);
        when(mMockTranslator.translate(anyString())).thenReturn(mockStringTask);
        when(mockStringTask.addOnSuccessListener(any(OnSuccessListener.class))).thenReturn(mockStringTask);
        when(mockStringTask.addOnFailureListener(transFailureCaptor.capture())).thenReturn(mockStringTask);

        Task<Void> mockVoidTask = mock(Task.class);
        ArgumentCaptor<OnSuccessListener<Void>> downloadSuccessCaptor = ArgumentCaptor.forClass(OnSuccessListener.class);
        when(mMockTranslator.downloadModelIfNeeded(any())).thenReturn(mockVoidTask);
        when(mockVoidTask.addOnSuccessListener(downloadSuccessCaptor.capture())).thenReturn(mockVoidTask);

        // Map of target switch index -> expected ISO language string
        HashMap<Integer, String> expectedLanguages = new HashMap<>();
        expectedLanguages.put(1, "bn");
        expectedLanguages.put(2, "zh");
        expectedLanguages.put(3, "en");
        expectedLanguages.put(4, "fr");
        expectedLanguages.put(5, "hi");
        expectedLanguages.put(6, "id");
        expectedLanguages.put(7, "ja");
        expectedLanguages.put(8, "mr");
        expectedLanguages.put(9, "pt");
        expectedLanguages.put(10, "es");
        expectedLanguages.put(11, "fil");
        expectedLanguages.put(12, "vi");
        expectedLanguages.put(13, "ar");
        expectedLanguages.put(14, "ur");

        for (int index : expectedLanguages.keySet()) {
            reset(mMockTts);
            
            TranslationManager testManager = new TranslationManager(mMockContext, mMockOverlay, mMockToastManager, mFakeProvider);
            testManager.prepare(index, 3); 
            downloadSuccessCaptor.getValue().onSuccess(null);

            testManager.translate(mMockTts, new HashMap<>(), new ArrayList<>(), "Text", "id");
            transFailureCaptor.getValue().onFailure(new Exception("Simulated fallback scenario"));

            ArgumentCaptor<Locale> localeCaptor = ArgumentCaptor.forClass(Locale.class);
            verify(mMockTts).setLanguage(localeCaptor.capture());
            
            assertEquals(expectedLanguages.get(index), localeCaptor.getValue().getLanguage());
        }
    }

    @Test
    public void testSetTtsLanguage_MissingOrUnsupportedData_StartsTtsInstallerActivity() {
        when(mMockTts.setLanguage(any(Locale.class))).thenReturn(TextToSpeech.LANG_MISSING_DATA);

        // Avoid local JVM environment crashes on Context.startActivity(Intent)
        doNothing().when(mMockAppContext).startActivity(any(Intent.class));

        // Intercept 'new Intent()' constructor calls using MockedConstruction
        try (MockedConstruction<Intent> mockedIntent = Mockito.mockConstruction(Intent.class)) {
            
            mManager.translate(mMockTts, new HashMap<>(), new ArrayList<>(), "Hello", "id");

            // Verify that startActivity was called on the context
            verify(mMockAppContext).startActivity(any(Intent.class));
            
            // Verify that the constructed intent mock had its action set correctly
            assertFalse(mockedIntent.constructed().isEmpty());
            Intent capturedMockIntent = mockedIntent.constructed().get(0);
            verify(capturedMockIntent).setAction(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA);
            verify(capturedMockIntent).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        }
    }

    @Test
    public void testSetTtsLanguage_ActivityNotFoundException_IsCaughtSafely() {
        when(mMockTts.setLanguage(any(Locale.class))).thenReturn(TextToSpeech.LANG_NOT_SUPPORTED);
        
        // Force the context to throw the target exception when it tries to start the activity
        doThrow(new ActivityNotFoundException()).when(mMockAppContext).startActivity(any(Intent.class));

        // Intercept 'new Intent()' constructor calls here as well
        try (MockedConstruction<Intent> mockedIntent = Mockito.mockConstruction(Intent.class)) {
            
            mManager.translate(mMockTts, new HashMap<>(), new ArrayList<>(), "Hello", "id");
            
            // If we reached here without throwing a RuntimeException or crashing, the catch block worked!
            verify(mMockAppContext).startActivity(any(Intent.class));
            
        } catch (Exception e) {
            fail("Exception should have been caught internally inside TranslationManager, but threw: " + e);
        }
    }

    @Test
    public void testClose_ClearsTranslatorReferences() {
        mManager.prepare(3, 7);
        mManager.close();

        verify(mMockTranslator).close();
        
        mManager.translate(mMockTts, new HashMap<String, String>(), new ArrayList<String>(), "Hello", "1");
        verify(mMockTts).setLanguage(Locale.getDefault());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testNullOverlayOrTtsInteractions_DoNotCrash() {
        TranslationManager nullOverlayManager = new TranslationManager(mMockContext, null, mMockToastManager, mFakeProvider);
        
        Task<Void> mockVoidTask = mock(Task.class);
        ArgumentCaptor<OnSuccessListener<Void>> downloadSuccessCaptor = ArgumentCaptor.forClass(OnSuccessListener.class);
        when(mMockTranslator.downloadModelIfNeeded(any())).thenReturn(mockVoidTask);
        when(mockVoidTask.addOnSuccessListener(downloadSuccessCaptor.capture())).thenReturn(mockVoidTask);

        nullOverlayManager.prepare(3, 7);
        downloadSuccessCaptor.getValue().onSuccess(null);

        try {
            nullOverlayManager.translate(mMockTts, new HashMap<String, String>(), new ArrayList<String>(), "Hello", "1");
        } catch (NullPointerException npe) {
            fail("Should not crash when TextReaderOverlay is null");
        }
    }

    @Test
    public void testAllIndices_SwitchCoverage() {
        for (int i = 0; i <= 15; i++) {
            mManager.prepare(i, (i == 1 ? 2 : 1)); 
            mManager.close();
        }

        for (int i = 0; i <= 15; i++) {
            TranslationManager loopManager = new TranslationManager(mMockContext, mMockOverlay, mMockToastManager, mFakeProvider);
            loopManager.prepare(3, i);
            loopManager.translate(mMockTts, new HashMap<>(), new ArrayList<>(), "Text", "id");
        }
    }
}
