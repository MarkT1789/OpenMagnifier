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
import android.os.Bundle;
import android.view.View;
import androidx.core.view.OnApplyWindowInsetsListener;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.graphics.Insets;
import androidx.preference.ListPreference;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class SettingsFragmentTest {

    private MockedStatic<AndroidSettingsFragmentFactory> mockedFactory;
    
    @Mock private ISettingsHelper mockSettingsHelper;
    @Mock private ListPreference mockFlashlightPref;
    @Mock private ListPreference mockSourcePref;
    @Mock private ListPreference mockDestPref;

    private SettingsFragment fragment;
    private AutoCloseable mocks;

    @Before
    public void setUp() {
        mocks = MockitoAnnotations.openMocks(this);
        
        // Mock the static factory before instantiating the fragment
        mockedFactory = mockStatic(AndroidSettingsFragmentFactory.class);
        mockedFactory.when(AndroidSettingsFragmentFactory::getSettingsHelper).thenReturn(mockSettingsHelper);

        fragment = spy(new SettingsFragment());
        doReturn(mock(Context.class)).when(fragment).requireContext();
        
        // Use nullable(String.class) so it catches the null rootKey parameter seamlessly
        doNothing().when(fragment).setPreferencesFromResource(anyInt(), nullable(String.class));
        
        // Default findPreference to return null unless explicitly overridden in individual tests
        doReturn(null).when(fragment).findPreference(anyString());
    }

    @After
    public void tearDown() throws Exception {
        if (mockedFactory != null) {
            mockedFactory.close();
        }
        if (mocks != null) {
            mocks.close();
        }
    }

    // ==========================================
    // METHOD: onCreatePreferences() Branches
    // ==========================================

    @Test
    public void testOnCreatePreferences_FlashlightNull_GooglePlayTrue() {
        doReturn(null).when(fragment).findPreference("flashlight_setting");
        when(mockSettingsHelper.isGooglePlayDevice(any())).thenReturn(true);

        fragment.onCreatePreferences(new Bundle(), "root");

        verify(fragment).setPreferencesFromResource(R.xml.root_preferences, "root");
        verify(fragment, never()).findPreference("source_setting");
    }

    @Test
    public void testOnCreatePreferences_NoFlash_DisablesFlashlightPreference() {
        doReturn(mockFlashlightPref).when(fragment).findPreference("flashlight_setting");
        when(mockSettingsHelper.hasFlash(any())).thenReturn(false);
        when(mockSettingsHelper.isGooglePlayDevice(any())).thenReturn(true);

        fragment.onCreatePreferences(null, null);

        verify(mockFlashlightPref).setEnabled(false);
        verify(mockSettingsHelper, never()).isFlashAdjustable(any());
    }

    @Test
    public void testOnCreatePreferences_HasFlash_FlashNotAdjustable_TriggersDeleteEntries() {
        doReturn(mockFlashlightPref).when(fragment).findPreference("flashlight_setting");
        when(mockSettingsHelper.hasFlash(any())).thenReturn(true);
        when(mockSettingsHelper.isFlashAdjustable(any())).thenReturn(false);
        when(mockSettingsHelper.isGooglePlayDevice(any())).thenReturn(true);

        CharSequence[] entries = {"0", "1", "2", "3", "4", "5"};
        CharSequence[] values = {"V0", "V1", "V2", "V3", "V4", "V5"};
        when(mockFlashlightPref.getEntries()).thenReturn(entries);
        when(mockFlashlightPref.getEntryValues()).thenReturn(values);
        when(mockFlashlightPref.getValue()).thenReturn("V4"); 

        fragment.onCreatePreferences(null, null);

        verify(mockFlashlightPref).setEntries(any(CharSequence[].class));
        verify(mockFlashlightPref).setEntryValues(any(CharSequence[].class));
        verify(mockFlashlightPref).setValue("V0"); 
    }

    @Test
    public void testOnCreatePreferences_NotGooglePlayDevice_PrefsExist_DisablesThem() {
        doReturn(null).when(fragment).findPreference("flashlight_setting");
        when(mockSettingsHelper.isGooglePlayDevice(any())).thenReturn(false);
        doReturn(mockSourcePref).when(fragment).findPreference("source_setting");
        doReturn(mockDestPref).when(fragment).findPreference("dest_setting");

        fragment.onCreatePreferences(null, null);

        verify(mockSourcePref).setEnabled(false);
        verify(mockDestPref).setEnabled(false);
    }

    @Test
    public void testOnCreatePreferences_NotGooglePlayDevice_PrefsNull_NoCrash() {
        doReturn(null).when(fragment).findPreference("flashlight_setting");
        when(mockSettingsHelper.isGooglePlayDevice(any())).thenReturn(false);
        doReturn(null).when(fragment).findPreference("source_setting");
        doReturn(null).when(fragment).findPreference("dest_setting");

        fragment.onCreatePreferences(null, null);

        verify(fragment).findPreference("source_setting");
        verify(fragment).findPreference("dest_setting");
    }

    // ==========================================
    // METHOD: deleteEntries() Hidden Branches
    // ==========================================

    @Test
    public void testDeleteEntries_EntriesOrValuesNull_ReturnsEarly() {
        doReturn(mockFlashlightPref).when(fragment).findPreference("flashlight_setting");
        when(mockSettingsHelper.hasFlash(any())).thenReturn(true);
        when(mockSettingsHelper.isFlashAdjustable(any())).thenReturn(false);
        when(mockSettingsHelper.isGooglePlayDevice(any())).thenReturn(true);

        // Case A: Entries null
        when(mockFlashlightPref.getEntries()).thenReturn(null);
        when(mockFlashlightPref.getEntryValues()).thenReturn(new CharSequence[]{"1"});
        fragment.onCreatePreferences(null, null);
        verify(mockFlashlightPref, never()).setEntries(any());

        // Case B: Entry Values null
        when(mockFlashlightPref.getEntries()).thenReturn(new CharSequence[]{"1"});
        when(mockFlashlightPref.getEntryValues()).thenReturn(null);
        fragment.onCreatePreferences(null, null);
        verify(mockFlashlightPref, never()).setEntries(any());
    }

    @Test
    public void testDeleteEntries_SizeLessThanOrEqualToFive_DoesNotDelete() {
        doReturn(mockFlashlightPref).when(fragment).findPreference("flashlight_setting");
        when(mockSettingsHelper.hasFlash(any())).thenReturn(true);
        when(mockSettingsHelper.isFlashAdjustable(any())).thenReturn(false);
        when(mockSettingsHelper.isGooglePlayDevice(any())).thenReturn(true);

        CharSequence[] entries = {"0", "1", "2", "3", "4"}; 
        when(mockFlashlightPref.getEntries()).thenReturn(entries);
        when(mockFlashlightPref.getEntryValues()).thenReturn(entries);

        fragment.onCreatePreferences(null, null);

        verify(mockFlashlightPref, never()).setEntries(any());
    }

    @Test
    public void testDeleteEntries_CurrentValueNullOrNotMatched() {
        doReturn(mockFlashlightPref).when(fragment).findPreference("flashlight_setting");
        when(mockSettingsHelper.hasFlash(any())).thenReturn(true);
        when(mockSettingsHelper.isFlashAdjustable(any())).thenReturn(false);
        when(mockSettingsHelper.isGooglePlayDevice(any())).thenReturn(true);

        CharSequence[] entries = {"0", "1", "2", "3", "4", "5"};
        CharSequence[] values = {"V0", "V1", "V2", "V3", "V4", "V5"};
        when(mockFlashlightPref.getEntries()).thenReturn(entries);
        when(mockFlashlightPref.getEntryValues()).thenReturn(values);
        when(mockFlashlightPref.getValue()).thenReturn(null); 

        fragment.onCreatePreferences(null, null);

        verify(mockFlashlightPref).setEntries(any(CharSequence[].class));
        verify(mockFlashlightPref, never()).setValue(anyString());
    }

    // ==========================================
    // METHOD: onViewCreated() & Insets Listener Branches
    // ==========================================

    @Test
    public void testOnViewCreated_ListViewNull() {
        SettingsFragment rawFragment = new SettingsFragment();
        View mockView = mock(View.class);

        rawFragment.onViewCreated(mockView, null);

        assertNull(rawFragment.getListView());
    }

    @Test
    public void testOnViewCreated_ListViewNotNull_AppliesWindowInsets() {
        android.widget.FrameLayout container = new android.widget.FrameLayout(androidx.test.core.app.ApplicationProvider.getApplicationContext());
        androidx.recyclerview.widget.RecyclerView realRecyclerView = new androidx.recyclerview.widget.RecyclerView(container.getContext());
        realRecyclerView.setId(androidx.preference.R.id.recycler_view);
        container.addView(realRecyclerView);

        doReturn(realRecyclerView).when(fragment).getListView();

        try (MockedStatic<ViewCompat> mockedViewCompat = mockStatic(ViewCompat.class)) {
            fragment.onViewCreated(container, null);

            assertFalse(realRecyclerView.getClipToPadding());

            ArgumentCaptor<OnApplyWindowInsetsListener> listenerCaptor = 
                    ArgumentCaptor.forClass(OnApplyWindowInsetsListener.class);
            
            mockedViewCompat.verify(() -> 
                ViewCompat.setOnApplyWindowInsetsListener(eq(realRecyclerView), listenerCaptor.capture())
            );

            OnApplyWindowInsetsListener capturedListener = listenerCaptor.getValue();
            assertNotNull(capturedListener);

            View mockTargetView = mock(View.class);
            WindowInsetsCompat mockWindowInsets = mock(WindowInsetsCompat.class);
            Insets mockInsets = Insets.of(10, 20, 30, 40);

            when(mockWindowInsets.getInsets(WindowInsetsCompat.Type.systemBars())).thenReturn(mockInsets);
            when(mockTargetView.getPaddingLeft()).thenReturn(5);
            when(mockTargetView.getPaddingTop()).thenReturn(6);
            when(mockTargetView.getPaddingRight()).thenReturn(7);

            WindowInsetsCompat resultInsets = capturedListener.onApplyWindowInsets(mockTargetView, mockWindowInsets);

            verify(mockTargetView).setPadding(5, 6, 7, 40);
            assertEquals(mockWindowInsets, resultInsets);
        }
    }
}
