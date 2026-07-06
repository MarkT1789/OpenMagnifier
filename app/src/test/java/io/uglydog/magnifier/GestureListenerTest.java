package io.uglydog.magnifier;

import android.view.MotionEvent;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

public class GestureListenerTest {

    @Mock
    private GestureListener.GestureActions mockActions;

    @Mock
    private MotionEvent mockEvent;

    private GestureListener gestureListener;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        gestureListener = new GestureListener(mockActions);
    }

    // --- Happy Path Tests ---

    @Test
    public void onSingleTapConfirmed_happyPath_returnsTrueWhenActionReturnsTrue() {
        // Arrange
        when(mockActions.onToggleMode()).thenReturn(true);

        // Act
        boolean result = gestureListener.onSingleTapConfirmed(mockEvent);

        // Assert
        assertTrue(result);
        verify(mockActions).onToggleMode();
    }

    @Test
    public void onSingleTapConfirmed_happyPath_returnsFalseWhenActionReturnsFalse() {
        // Arrange
        when(mockActions.onToggleMode()).thenReturn(false);

        // Act
        boolean result = gestureListener.onSingleTapConfirmed(mockEvent);

        // Assert
        assertFalse(result);
        verify(mockActions).onToggleMode();
    }

    @Test
    public void onDoubleTap_happyPath_callsOpenSettingsAndReturnsTrue() {
        // Act
        boolean result = gestureListener.onDoubleTap(mockEvent);

        // Assert
        assertTrue(result);
        verify(mockActions).onOpenSettings();
    }

    @Test
    public void onLongPress_happyPath_callsShowHelp() {
        // Act
        gestureListener.onLongPress(mockEvent);

        // Assert
        verify(mockActions).onShowHelp();
    }

    // --- Edge Cases / Null Input Tests ---

    @Test
    public void onSingleTapConfirmed_nullMotionEvent_stillExecutesCorrectly() {
        // Arrange
        when(mockActions.onToggleMode()).thenReturn(true);

        // Act
        boolean result = gestureListener.onSingleTapConfirmed(null);

        // Assert
        assertTrue(result);
        verify(mockActions).onToggleMode();
    }

    @Test
    public void onDoubleTap_nullMotionEvent_stillExecutesCorrectly() {
        // Act
        boolean result = gestureListener.onDoubleTap(null);

        // Assert
        assertTrue(result);
        verify(mockActions).onOpenSettings();
    }

    @Test
    public void onLongPress_nullMotionEvent_stillExecutesCorrectly() {
        // Act
        gestureListener.onLongPress(null);

        // Assert
        verify(mockActions).onShowHelp();
    }
}
