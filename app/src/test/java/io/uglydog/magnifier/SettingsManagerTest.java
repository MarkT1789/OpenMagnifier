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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.SharedPreferences;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class SettingsManagerTest {

    @Mock
    private SharedPreferences mockPrefs;

    @Mock
    private SharedPreferences.Editor mockEditor;

    private SettingsManager settingsManager;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        
        // Mocking SharedPreferences.Editor fluent API chaining
        when(mockPrefs.edit()).thenReturn(mockEditor);
        when(mockEditor.putString(anyString(), anyString())).thenReturn(mockEditor);

        // Pre-populate mock preferences with default fallback strings so reload() handles it happily
        stubDefaultPreferences();

        // Instantiate the system under test (this triggers initial reload)
        settingsManager = new SettingsManager(mockPrefs);
    }

    private void stubDefaultPreferences() {
        when(mockPrefs.getString(eq("brightness_setting"), anyString())).thenReturn("0.0");
        when(mockPrefs.getString(eq("color_setting"), anyString())).thenReturn("0");
        when(mockPrefs.getString(eq("contrast_setting"), anyString())).thenReturn("1.0");
        when(mockPrefs.getString(eq("dx_setting"), anyString())).thenReturn("0.25");
        when(mockPrefs.getString(eq("dy_setting"), anyString())).thenReturn("0.25");
        when(mockPrefs.getString(eq("rotation_setting"), anyString())).thenReturn("0");
        when(mockPrefs.getString(eq("splash_version_setting"), anyString())).thenReturn("-1");
        when(mockPrefs.getString(eq("zoom_setting"), anyString())).thenReturn("1.0");
        when(mockPrefs.getString(eq("flashlight_setting"), anyString())).thenReturn("0.0");
        when(mockPrefs.getString(eq("speak_setting"), anyString())).thenReturn("0");
        when(mockPrefs.getString(eq("source_setting"), anyString())).thenReturn("0");
        when(mockPrefs.getString(eq("dest_setting"), anyString())).thenReturn("0");
        when(mockPrefs.getString(eq("banner_setting"), anyString())).thenReturn("0");
        when(mockPrefs.getString(eq("volume_setting"), anyString())).thenReturn("0");
        when(mockPrefs.getString(eq("banner_size_setting"), anyString())).thenReturn("1.0");
    }

    // ==========================================
    // HAPPY PATHS: GETTERS & RELOAD
    // ==========================================

    @Test
    public void testReloadAndGetters_withCustomValues() {
        // Arrange custom stored values
        when(mockPrefs.getString(eq("brightness_setting"), anyString())).thenReturn("0.8");
        when(mockPrefs.getString(eq("color_setting"), anyString())).thenReturn("2");
        when(mockPrefs.getString(eq("contrast_setting"), anyString())).thenReturn("1.5");
        when(mockPrefs.getString(eq("dx_setting"), anyString())).thenReturn("0.5");
        when(mockPrefs.getString(eq("dy_setting"), anyString())).thenReturn("0.6");
        when(mockPrefs.getString(eq("rotation_setting"), anyString())).thenReturn("90");
        when(mockPrefs.getString(eq("splash_version_setting"), anyString())).thenReturn("2.1.0");
        when(mockPrefs.getString(eq("zoom_setting"), anyString())).thenReturn("3.5");
        when(mockPrefs.getString(eq("flashlight_setting"), anyString())).thenReturn("1.0");
        when(mockPrefs.getString(eq("speak_setting"), anyString())).thenReturn("1");
        when(mockPrefs.getString(eq("source_setting"), anyString())).thenReturn("3");
        when(mockPrefs.getString(eq("dest_setting"), anyString())).thenReturn("4");
        when(mockPrefs.getString(eq("banner_setting"), anyString())).thenReturn("5");
        when(mockPrefs.getString(eq("volume_setting"), anyString())).thenReturn("7");
        when(mockPrefs.getString(eq("banner_size_setting"), anyString())).thenReturn("2.0");

        // Act
        settingsManager.reload();

        // Assert
        assertEquals(0.8f, settingsManager.getBrightness(), 0.0f);
        assertEquals(2, settingsManager.getColor());
        assertEquals(1.5f, settingsManager.getContrast(), 0.0f);
        assertEquals(0.5f, settingsManager.getDx(), 0.0f);
        assertEquals(0.6f, settingsManager.getDy(), 0.0f);
        assertEquals(90, settingsManager.getRotation());
        assertEquals("2.1.0", settingsManager.getSplashVersion());
        assertEquals(3.5f, settingsManager.getZoom(), 0.0f);
        assertEquals(1.0f, settingsManager.getFlashlight(), 0.0f);
        assertEquals(1, settingsManager.getSpeak());
        assertEquals(3, settingsManager.getSource());
        assertEquals(4, settingsManager.getDest());
        assertEquals(5, settingsManager.getBanner());
        assertEquals(7, settingsManager.getVolume());
        assertEquals(2.0f, settingsManager.getBannerSize(), 0.0f);
    }

    // ==========================================
    // HAPPY PATHS: SETTERS
    // ==========================================

    @Test
    public void testSetters_updatesFieldsAndSharedPreferences() {
        // Act & Assert for Float Type
        settingsManager.setBrightness(0.75f);
        assertEquals(0.75f, settingsManager.getBrightness(), 0.0f);
        verify(mockEditor).putString("brightness_setting", "0.75");

        // Act & Assert for Integer Type
        settingsManager.setColor(12);
        assertEquals(12, settingsManager.getColor());
        verify(mockEditor).putString("color_setting", "12");

        // Act & Assert for remaining elements to complete coverage
        settingsManager.setContrast(2.5f);
        assertEquals(2.5f, settingsManager.getContrast(), 0.0f);
        verify(mockEditor).putString("contrast_setting", "2.5");

        settingsManager.setDx(0.12f);
        assertEquals(0.12f, settingsManager.getDx(), 0.0f);
        verify(mockEditor).putString("dx_setting", "0.12");

        settingsManager.setDy(0.88f);
        assertEquals(0.88f, settingsManager.getDy(), 0.0f);
        verify(mockEditor).putString("dy_setting", "0.88");

        settingsManager.setRotation(180);
        assertEquals(180, settingsManager.getRotation());
        verify(mockEditor).putString("rotation_setting", "180");

        settingsManager.setSplashVersion("1.0.5");
        assertEquals("1.0.5", settingsManager.getSplashVersion());
        verify(mockEditor).putString("splash_version_setting", "1.0.5");

        settingsManager.setZoom(4.0f);
        assertEquals(4.0f, settingsManager.getZoom(), 0.0f);
        verify(mockEditor).putString("zoom_setting", "4.0");

        settingsManager.setFlashlight(0.5f);
        assertEquals(0.5f, settingsManager.getFlashlight(), 0.0f);
        verify(mockEditor).putString("flashlight_setting", "0.5");

        settingsManager.setSpeak(3);
        assertEquals(3, settingsManager.getSpeak());
        verify(mockEditor).putString("speak_setting", "3");

        settingsManager.setSource(1);
        assertEquals(1, settingsManager.getSource());
        verify(mockEditor).putString("source_setting", "1");

        settingsManager.setDest(2);
        assertEquals(2, settingsManager.getDest());
        verify(mockEditor).putString("dest_setting", "2");

        settingsManager.setBanner(8);
        assertEquals(8, settingsManager.getBanner());
        verify(mockEditor).putString("banner_setting", "8");

        settingsManager.setVolume(10);
        assertEquals(10, settingsManager.getVolume());
        verify(mockEditor).putString("volume_setting", "10");

        settingsManager.setBannerSize(1.5f);
        assertEquals(1.5f, settingsManager.getBannerSize(), 0.0f);
        verify(mockEditor).putString("banner_size_setting", "1.5");

        // Verify total editor apply updates triggered
        verify(mockEditor, times(15)).apply();
    }

    // ==========================================
    // EDGE CASES & EXCEPTION BRANCHES (TRY-CATCH)
    // ==========================================

    @Test
    public void testGetFloat_whenExceptionThrown_returnsDefaultValue() {
        // Arrange: Make preference retrieval fail parsing by forcing a bad string configuration
        when(mockPrefs.getString(eq("brightness_setting"), anyString())).thenReturn("not_a_float");

        // Act: Reloading triggers getFloat inside try-catch block
        settingsManager.reload();

        // Assert: Catches NumberFormatException and falls back to defaultValue passed in reload() (0.0f)
        assertEquals(0.0f, settingsManager.getBrightness(), 0.0f);
    }

    @Test
    public void testGetInt_whenExceptionThrown_returnsDefaultValue() {
        // Arrange: Make preference retrieval fail parsing by forcing a bad string configuration
        when(mockPrefs.getString(eq("color_setting"), anyString())).thenReturn("12.54_bad_int");

        // Act: Reloading triggers getInt inside try-catch block
        settingsManager.reload();

        // Assert: Catches NumberFormatException and falls back to defaultValue passed in reload() (0)
        assertEquals(0, settingsManager.getColor());
    }

    // ==========================================
    // NULL INPUT HANDLING
    // ==========================================

    @Test
    public void testSetSplashVersion_withNullValue() {
        // Arrange
        // While marked @NonNull, we explicitly verify null safety resilience 
        when(mockEditor.putString(anyString(), nullable(String.class))).thenReturn(mockEditor);
        
        // Act
        settingsManager.setSplashVersion(null);

        // Assert
        assertEquals(null, settingsManager.getSplashVersion());
        verify(mockEditor).putString("splash_version_setting", null);
    }
}
