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
import android.os.Build;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class ClipboardUtilTest {

    @Mock
    private Context mockContext;
    @Mock
    private Context mockAppContext;
    @Mock
    private ToastManager mockToastManager;
    @Mock
    private ClipboardUtil.ClipboardService mockClipboardService;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        when(mockContext.getApplicationContext()).thenReturn(mockAppContext);
        when(mockContext.getString(anyInt())).thenReturn("Copied to clipboard");
    }

    // ==========================================
    // 1. Constructor Coverage Tests
    // ==========================================

    @Test
    public void testPublicConstructor_instantiatesSuccessfully() {
        // Covers public ClipboardUtil(Context, ToastManager) including the default ClipboardService inner class execution
        ClipboardUtil util = new ClipboardUtil(mockContext, mockToastManager);
        assertNotNull(util);

        // Execute copy to exercise the default inner class's setPrimaryClip implementation safely
        util.copy(Collections.singletonList("key"), Collections.singletonMap("key", "val"));
    }

    // ==========================================
    // 2. formatText Branch Coverage Tests
    // ==========================================

    @Test
    public void testFormatText_nullKeys_returnsEmpty() {
        String result = ClipboardUtil.formatText(null, new HashMap<>());
        assertEquals("", result);
    }

    @Test
    public void testFormatText_nullMap_returnsEmpty() {
        String result = ClipboardUtil.formatText(Arrays.asList("key1"), null);
        assertEquals("", result);
    }

    @Test
    public void testFormatText_bothNull_returnsEmpty() {
        String result = ClipboardUtil.formatText(null, null);
        assertEquals("", result);
    }

    @Test
    public void testFormatText_nullKeyInList_skipsKey() {
        List<String> keys = new ArrayList<>();
        keys.add(null);
        keys.add("key1");

        Map<String, String> map = new HashMap<>();
        map.put("key1", "value1");

        String result = ClipboardUtil.formatText(keys, map);
        assertEquals("value1", result);
    }

    @Test
    public void testFormatText_nullValueInMap_skipsValue() {
        List<String> keys = Arrays.asList("key1", "key2");
        Map<String, String> map = new HashMap<>();
        map.put("key1", null);
        map.put("key2", "value2");

        String result = ClipboardUtil.formatText(keys, map);
        assertEquals("value2", result);
    }

    @Test
    public void testFormatText_emptyValueInMap_skipsValue() {
        List<String> keys = Arrays.asList("key1", "key2");
        Map<String, String> map = new HashMap<>();
        map.put("key1", "");
        map.put("key2", "value2");

        String result = ClipboardUtil.formatText(keys, map);
        assertEquals("value2", result);
    }

    @Test
    public void testFormatText_multipleValidKeys_appendsNewlines() {
        List<String> keys = Arrays.asList("key1", "key2", "key3");
        Map<String, String> map = new HashMap<>();
        map.put("key1", "First");
        map.put("key2", "Second");
        map.put("key3", "Third");

        String result = ClipboardUtil.formatText(keys, map);
        assertEquals("First\nSecond\nThird", result);
    }

    // ==========================================
    // 3. copyToClipboard & Build Version Branch Tests
    // ==========================================

    @Test
    public void testCopy_preTiramisu_showsToast() throws Exception {
        ClipboardUtil util = new ClipboardUtil(mockContext, mockToastManager, mockClipboardService) {
            @Override
            int getBuildVersion() {
                return Build.VERSION_CODES.S; // API 31 (Pre-Tiramisu)
            }
        };

        List<String> keys = Collections.singletonList("key");
        Map<String, String> map = Collections.singletonMap("key", "Hello World");

        util.copy(keys, map);

        verify(mockClipboardService).setPrimaryClip(eq("OCR"), eq("Hello World"));
        verify(mockToastManager).show(eq(mockAppContext), any());
    }

    @Test
    public void testCopy_tiramisuOrGreater_doesNotShowToast() throws Exception {
        ClipboardUtil util = new ClipboardUtil(mockContext, mockToastManager, mockClipboardService) {
            @Override
            int getBuildVersion() {
                return Build.VERSION_CODES.TIRAMISU; // API 33 (Tiramisu)
            }
        };

        List<String> keys = Collections.singletonList("key");
        Map<String, String> map = Collections.singletonMap("key", "Hello World");

        util.copy(keys, map);

        verify(mockClipboardService).setPrimaryClip(eq("OCR"), eq("Hello World"));
        verify(mockToastManager, never()).show(any(), anyString());
    }

    @Test
    public void testCopy_clipboardServiceThrowsException_catchesExceptionGracefully() throws Exception {
        doThrow(new RuntimeException("System service unavailable"))
                .when(mockClipboardService).setPrimaryClip(anyString(), anyString());

        ClipboardUtil util = new ClipboardUtil(mockContext, mockToastManager, mockClipboardService);

        // Must complete safely without bubbling exception
        util.copy(Collections.singletonList("key"), Collections.singletonMap("key", "test"));

        verify(mockToastManager, never()).show(any(), anyString());
    }
}
