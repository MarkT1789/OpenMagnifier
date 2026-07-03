package io.uglydog.magnifier;

import static org.mockito.Mockito.*;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import android.content.Context;

public class ToastManagerTest {

    @Mock Context mockContext;
    @Mock Context mockAppContext;
    @Mock ToastManager.ToastFactory mockFactory;
    @Mock IToast mockToast1;
    @Mock IToast mockToast2;

    private ToastManager toastManager;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        // Setup context mocking
        when(mockContext.getApplicationContext()).thenReturn(mockAppContext);
        
        // Initialize our manager with the mocked factory
        toastManager = new ToastManager(mockFactory);
    }

    @Test
    public void show_shouldCancelPreviousToast_andShowNewToast() {
        // Arrange: Prepare the factory to return toast 1, then toast 2
        when(mockFactory.create(any(), eq("First Message"), anyInt())).thenReturn(mockToast1);
        when(mockFactory.create(any(), eq("Second Message"), anyInt())).thenReturn(mockToast2);

        // Act: Show first toast
        toastManager.show(mockContext, "First Message");
        verify(mockToast1).show(); // Verify first toast showed

        // Act: Show second toast
        toastManager.show(mockContext, "Second Message");

        // Assert: Verify the first toast was cancelled before the second one showed
        verify(mockToast1).cancel();
        verify(mockToast2).show();
    }

    @Test
    public void cancel_shouldCancelActiveToast() {
        // Arrange
        when(mockFactory.create(any(), anyString(), anyInt())).thenReturn(mockToast1);
        toastManager.show(mockContext, "Hello");

        // Act
        toastManager.cancel();

        // Assert
        verify(mockToast1).cancel();
    }
}
