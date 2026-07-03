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

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

import android.content.SharedPreferences;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class SettingsManagerTest {

    @Mock SharedPreferences mockPrefs;
    @Mock SharedPreferences.Editor mockEditor;

    private SettingsManager settingsManager;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        
        // Wire the mock editor to return itself for builder chaining pattern compliance
        when(mockPrefs.edit()).thenReturn(mockEditor);
        when(mockEditor.putString(anyString(), anyString())).thenReturn(mockEditor);
        
        // Define fallback behaviors for initialization
        when(mockPrefs.getString(anyString(), anyString())).thenAnswer(invocation -> invocation.getArgument(1));
    }

    @Test
    public void initialization_shouldLoadDefaultValues() {
        // Override one specific mocked setting value to check initialization logic
        when(mockPrefs.getString("zoom_setting", "1.0")).thenReturn("2.5");

        // Initialize targeting the dependencies
        settingsManager = new SettingsManager(mockPrefs);

        // Verify custom configurations vs fallback defaults are accurately retained
        assertEquals(2.5f, settingsManager.getZoom(), 0.0f);
        assertEquals(0.0f, settingsManager.getBrightness(), 0.0f);
    }

    @Test
    public void setBrightness_shouldSaveToPreferences() {
        settingsManager = new SettingsManager(mockPrefs);
        
        // Trigger modification logic
        settingsManager.setBrightness(0.85f);

        // Assert memory and storage persistence are called properly
        assertEquals(0.85f, settingsManager.getBrightness(), 0.0f);
        verify(mockEditor).putString("brightness_setting", "0.85");
        verify(mockEditor).apply();
    }
}
