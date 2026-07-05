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

import android.view.KeyEvent;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class InputHandlerTest {

    private InputHandler inputHandler;
    
    @Mock private IKeyEvent mockKeyEventFormatter;
    @Mock private InputHandler.InputActions mockActions;
    @Mock private KeyEvent mockEvent;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        // Inject the IKeyEvent mock implementation to decouple the unit test
        inputHandler = new InputHandler(mockKeyEventFormatter);
        when(mockKeyEventFormatter.getKeyCodeString(any(KeyEvent.class))).thenReturn("MOCK_KEY_STRING");
    }

    // --- NULL INPUT / EDGE CASES ---

    @Test
    public void testHandleKey_NullActionsThrowsException() {
        when(mockEvent.getAction()).thenReturn(KeyEvent.ACTION_DOWN);
        when(mockEvent.getRepeatCount()).thenReturn(0);
        when(mockEvent.getKeyCode()).thenReturn(KeyEvent.KEYCODE_B);

        try {
            inputHandler.handleKey(mockEvent, null);
            fail("Expected NullPointerException for null actions mapping");
        } catch (NullPointerException e) {
            // Expected outcome
        }
    }

    @Test
    public void testHandleKey_NullEventThrowsException() {
        try {
            inputHandler.handleKey(null, mockActions);
            fail("Expected NullPointerException for null key event");
        } catch (NullPointerException e) {
            // Expected outcome
        }
    }

    @Test
    public void testHandleKey_UnhandledKeyCodeReturnsFalse() {
        when(mockEvent.getAction()).thenReturn(KeyEvent.ACTION_DOWN);
        when(mockEvent.getRepeatCount()).thenReturn(0);
        when(mockEvent.getKeyCode()).thenReturn(KeyEvent.KEYCODE_UNKNOWN);

        boolean handled = inputHandler.handleKey(mockEvent, mockActions);
        
        assertFalse(handled);
        verifyNoInteractions(mockActions);
    }

    // --- REPEAT COUNT / ACTION UP BRANCHES (EDGE CASES) ---

    @Test
    public void testHandleKey_ActionUp_DoesNotTriggerAction() {
        when(mockEvent.getAction()).thenReturn(KeyEvent.ACTION_UP);
        when(mockEvent.getRepeatCount()).thenReturn(0);
        when(mockEvent.getKeyCode()).thenReturn(KeyEvent.KEYCODE_B);

        boolean handled = inputHandler.handleKey(mockEvent, mockActions);

        assertTrue(handled); // Evaluates code blocks inside switch, but condition restricts inner actions
        verifyNoInteractions(mockActions);
    }

    @Test
    public void testHandleKey_RepeatCountGreaterThanZero_DoesNotTriggerAction() {
        when(mockEvent.getAction()).thenReturn(KeyEvent.ACTION_DOWN);
        when(mockEvent.getRepeatCount()).thenReturn(1);
        when(mockEvent.getKeyCode()).thenReturn(KeyEvent.KEYCODE_B);

        boolean handled = inputHandler.handleKey(mockEvent, mockActions);

        assertTrue(handled); 
        verifyNoInteractions(mockActions);
    }

    // --- HAPPY PATHS (ALL KEYCODE SWITCH BRANCHES) ---

    @Test
    public void testHandleKey_Brightness() {
        setupFirstKeyDown(KeyEvent.KEYCODE_B);
        assertTrue(inputHandler.handleKey(mockEvent, mockActions));
        verify(mockActions).onChangeBrightnessSetting(mockEvent);
    }

    @Test
    public void testHandleKey_Contrast() {
        setupFirstKeyDown(KeyEvent.KEYCODE_C);
        assertTrue(inputHandler.handleKey(mockEvent, mockActions));
        verify(mockActions).onChangeContrastSetting(mockEvent);
    }

    @Test
    public void testHandleKey_ColorFilter() {
        setupFirstKeyDown(KeyEvent.KEYCODE_F);
        assertTrue(inputHandler.handleKey(mockEvent, mockActions));
        verify(mockActions).onChangeColorFilterSetting(mockEvent);
    }

    @Test
    public void testHandleKey_ShowHelp() {
        setupFirstKeyDown(KeyEvent.KEYCODE_H);
        assertTrue(inputHandler.handleKey(mockEvent, mockActions));
        verify(mockActions).onShowHelp();
    }

    @Test
    public void testHandleKey_Flashlight() {
        setupFirstKeyDown(KeyEvent.KEYCODE_L);
        assertTrue(inputHandler.handleKey(mockEvent, mockActions));
        verify(mockActions).onChangeFlashlightSetting(mockEvent);
    }

    @Test
    public void testHandleKey_Rotation() {
        setupFirstKeyDown(KeyEvent.KEYCODE_R);
        assertTrue(inputHandler.handleKey(mockEvent, mockActions));
        verify(mockActions).onChangeRotationSetting(mockEvent);
    }

    @Test
    public void testHandleKey_Speak() {
        setupFirstKeyDown(KeyEvent.KEYCODE_S);
        assertTrue(inputHandler.handleKey(mockEvent, mockActions));
        verify(mockActions).onChangeSpeakSetting(mockEvent);
    }

    @Test
    public void testHandleKey_ShowVersion() {
        setupFirstKeyDown(KeyEvent.KEYCODE_V);
        assertTrue(inputHandler.handleKey(mockEvent, mockActions));
        verify(mockActions).onShowVersion();
    }

    @Test
    public void testHandleKey_PanX() {
        setupFirstKeyDown(KeyEvent.KEYCODE_X);
        assertTrue(inputHandler.handleKey(mockEvent, mockActions));
        verify(mockActions).onChangePanSetting(mockEvent);
    }

    @Test
    public void testHandleKey_PanY() {
        setupFirstKeyDown(KeyEvent.KEYCODE_Y);
        assertTrue(inputHandler.handleKey(mockEvent, mockActions));
        verify(mockActions).onChangePanSetting(mockEvent);
    }

    @Test
    public void testHandleKey_Zoom() {
        setupFirstKeyDown(KeyEvent.KEYCODE_Z);
        assertTrue(inputHandler.handleKey(mockEvent, mockActions));
        verify(mockActions).onChangeZoomSetting(mockEvent);
    }

    @Test
    public void testHandleKey_Space() {
        setupFirstKeyDown(KeyEvent.KEYCODE_SPACE);
        assertTrue(inputHandler.handleKey(mockEvent, mockActions));
        verify(mockActions).onChangeView();
    }

    // --- DPAD BRANCHES (Requires isKeyDown, ignores repeat count) ---

    @Test
    public void testHandleKey_DpadUp() {
        when(mockEvent.getAction()).thenReturn(KeyEvent.ACTION_DOWN);
        when(mockEvent.getRepeatCount()).thenReturn(3); // Simulates repeated scrolling
        when(mockEvent.getKeyCode()).thenReturn(KeyEvent.KEYCODE_DPAD_UP);

        assertTrue(inputHandler.handleKey(mockEvent, mockActions));
        verify(mockActions).onScrollViewport(mockEvent);
    }

    @Test
    public void testHandleKey_DpadDown() {
        when(mockEvent.getAction()).thenReturn(KeyEvent.ACTION_DOWN);
        when(mockEvent.getKeyCode()).thenReturn(KeyEvent.KEYCODE_DPAD_DOWN);

        assertTrue(inputHandler.handleKey(mockEvent, mockActions));
        verify(mockActions).onScrollViewport(mockEvent);
    }

    @Test
    public void testHandleKey_DpadLeft() {
        when(mockEvent.getAction()).thenReturn(KeyEvent.ACTION_DOWN);
        when(mockEvent.getKeyCode()).thenReturn(KeyEvent.KEYCODE_DPAD_LEFT);

        assertTrue(inputHandler.handleKey(mockEvent, mockActions));
        verify(mockActions).onScrollViewport(mockEvent);
    }

    @Test
    public void testHandleKey_DpadRight() {
        when(mockEvent.getAction()).thenReturn(KeyEvent.ACTION_DOWN);
        when(mockEvent.getKeyCode()).thenReturn(KeyEvent.KEYCODE_DPAD_RIGHT);

        assertTrue(inputHandler.handleKey(mockEvent, mockActions));
        verify(mockActions).onScrollViewport(mockEvent);
    }

    // --- VOLUME BRANCHES (Requires isKeyDown, passes boolean return up) ---

    @Test
    public void testHandleKey_VolumeUp_ReturnsTrue() {
        when(mockEvent.getAction()).thenReturn(KeyEvent.ACTION_DOWN);
        when(mockEvent.getKeyCode()).thenReturn(KeyEvent.KEYCODE_VOLUME_UP);
        when(mockActions.onVolumeChanged(mockEvent)).thenReturn(true);

        assertTrue(inputHandler.handleKey(mockEvent, mockActions));
        verify(mockActions).onVolumeChanged(mockEvent);
    }

    @Test
    public void testHandleKey_VolumeDown_ReturnsFalse() {
        when(mockEvent.getAction()).thenReturn(KeyEvent.ACTION_DOWN);
        when(mockEvent.getKeyCode()).thenReturn(KeyEvent.KEYCODE_VOLUME_DOWN);
        when(mockActions.onVolumeChanged(mockEvent)).thenReturn(false);

        assertFalse(inputHandler.handleKey(mockEvent, mockActions));
        verify(mockActions).onVolumeChanged(mockEvent);
    }

    @Test
    public void testHandleKey_Volume_ActionUp_ReturnsFalse() {
        when(mockEvent.getAction()).thenReturn(KeyEvent.ACTION_UP);
        when(mockEvent.getKeyCode()).thenReturn(KeyEvent.KEYCODE_VOLUME_UP);

        assertFalse(inputHandler.handleKey(mockEvent, mockActions));
        verifyNoInteractions(mockActions);
    }

    // --- PRODUCTION FACTORY VALIDATION ---

    @Test
    public void testProductionFactoryCreation() {
        InputHandler productionHandler = InputHandler.createProductionHandler();
        assertNotNull(productionHandler);
    }

    // --- HELPER METHODS ---

    private void setupFirstKeyDown(int keyCode) {
        when(mockEvent.getAction()).thenReturn(KeyEvent.ACTION_DOWN);
        when(mockEvent.getRepeatCount()).thenReturn(0);
        when(mockEvent.getKeyCode()).thenReturn(keyCode);
    }
}
