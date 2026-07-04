package io.uglydog.magnifier;

import android.view.ScaleGestureDetector;
import android.content.Context;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

public class ScaleListenerTest {

    private FakeClock mFakeClock;
    private TrackingScaleActions mTrackingActions;
    private ScaleListener mScaleListener;
    private StubScaleGestureDetector mMockDetector;

    // --- Test Doubles (Java 7 Compatible) ---

    private static class FakeClock implements ISystemClock {
        long time = 0;
        @Override
        public long uptimeMillis() {
            return time;
        }
    }

    private static class TrackingScaleActions implements ScaleListener.ScaleActions {
        boolean wasCalled = false;
        float lastScale = 0.0f;
        boolean lastFinished = false;
        int callCount = 0;

        @Override
        public void onScale(float scale, boolean finished) {
            wasCalled = true;
            lastScale = scale;
            lastFinished = finished;
            callCount++;
        }
    }

    // A stubbed detector to bypass Android framework framework limitations in local JUnit tests
    private static class StubScaleGestureDetector extends ScaleGestureDetector {
        float scaleFactor = 1.0f;

        public StubScaleGestureDetector() {
            // Passing null context since we are overriding the framework method completely
            super(null, new ScaleGestureDetector.SimpleOnScaleGestureListener());
        }

        @Override
        public float getScaleFactor() {
            return scaleFactor;
        }
    }

    @Before
    public void setUp() {
        mFakeClock = new FakeClock();
        mTrackingActions = new TrackingScaleActions();
        mScaleListener = new ScaleListener(mTrackingActions, mFakeClock);
        mMockDetector = new StubScaleGestureDetector();
    }

    // --- 1. Test null input (Edge Cases) ---

    @Test
    public void testOnScaleBegin_WithNullDetector() {
        // Checking robustness against null inputs
        try {
            boolean result = mScaleListener.onScaleBegin(null);
            assertTrue("onScaleBegin should return true even if detector is null", result);
        } catch (Exception e) {
            fail("onScaleBegin should handled null safely: " + e.getMessage());
        }
    }

    @Test
    public void testOnScaleEnd_WithNullDetector() {
        try {
            mScaleListener.onScaleEnd(null);
            // If it reached here without a NullPointerException, it's successful.
        } catch (Exception e) {
            fail("onScaleEnd should handled null safely: " + e.getMessage());
        }
    }

    // --- 2. Test onScaleBegin (Happy Path) ---

    @Test
    public void testOnScaleBegin_ResetsInternalState() {
        boolean result = mScaleListener.onScaleBegin(mMockDetector);
        assertTrue(result);
        
        // Asserting that internal accumulation logic starts cleanly
        // We verify this by running an onScale event immediately following it
        mMockDetector.scaleFactor = 1.02f;
        mScaleListener.onScale(mMockDetector);
        
        // The scale should be exactly 1.02f because internal mScale was reset to 1.0f
        // Let's force an action trigger via a large scale jump to read the state
        mMockDetector.scaleFactor = 2.0f; // total scale accumulated becomes 2.04f (> 1.05f threshold)
        mScaleListener.onScale(mMockDetector);
        
        assertEquals(2.04f, mTrackingActions.lastScale, 0.001f);
    }

    // --- 3. Test onScale Branch Coverage ---

    @Test
    public void testOnScale_DoesNotTriggerActions_WhenThresholdsAreNotMet() {
        mScaleListener.onScaleBegin(mMockDetector);
        
        // Time delay is 0, scale accumulation is 1.01f (within 0.95f - 1.05f)
        mFakeClock.time = 10; 
        mMockDetector.scaleFactor = 1.01f;
        
        mScaleListener.onScale(mMockDetector);
        
        assertFalse("ScaleActions shouldn't be called if no threshold is broken", mTrackingActions.wasCalled);
    }

    @Test
    public void testOnScale_TriggersActions_WhenTimeDelayThresholdExceeded() {
        mScaleListener.onScaleBegin(mMockDetector);
        
        // Advance time past 30ms threshold
        mFakeClock.time = 35; 
        mMockDetector.scaleFactor = 1.01f;
        
        mScaleListener.onScale(mMockDetector);
        
        assertTrue(mTrackingActions.wasCalled);
        assertEquals(1.01f, mTrackingActions.lastScale, 0.001f);
        assertFalse(mTrackingActions.lastFinished);
    }

    @Test
    public void testOnScale_TriggersActions_WhenUpperScaleThresholdExceeded() {
        mScaleListener.onScaleBegin(mMockDetector);
        
        // Time delay is small (10ms), but scale goes over 1.05f
        mFakeClock.time = 10;
        mMockDetector.scaleFactor = 1.06f;
        
        mScaleListener.onScale(mMockDetector);
        
        assertTrue(mTrackingActions.wasCalled);
        assertEquals(1.06f, mTrackingActions.lastScale, 0.001f);
        assertFalse(mTrackingActions.lastFinished);
    }

    @Test
    public void testOnScale_TriggersActions_WhenLowerScaleThresholdExceeded() {
        mScaleListener.onScaleBegin(mMockDetector);
        
        // Time delay is small (10ms), but scale goes below 0.95f
        mFakeClock.time = 10;
        mMockDetector.scaleFactor = 0.94f;
        
        mScaleListener.onScale(mMockDetector);
        
        assertTrue(mTrackingActions.wasCalled);
        assertEquals(0.94f, mTrackingActions.lastScale, 0.001f);
        assertFalse(mTrackingActions.lastFinished);
    }

    @Test
    public void testOnScale_ResetsScaleAccumulation_AfterTriggering() {
        mScaleListener.onScaleBegin(mMockDetector);
        
        // Trigger action via upper threshold
        mMockDetector.scaleFactor = 1.10f;
        mScaleListener.onScale(mMockDetector);
        assertEquals(1, mTrackingActions.callCount);
        
        // Next scale event shouldn't multiply with the previous 1.10f because internal mScale resets to 1.0f
        mMockDetector.scaleFactor = 1.01f;
        mFakeClock.time = 5; // not breaking time threshold
        mScaleListener.onScale(mMockDetector);
        
        // Count remains 1 since the second call didn't breach any thresholds
        assertEquals(1, mTrackingActions.callCount);
    }

    // --- 4. Test onScaleEnd (Happy Path) ---

    @Test
    public void testOnScaleEnd_TriggersActionsWithFinishedTrue() {
        mScaleListener.onScaleBegin(mMockDetector);
        
        // Accumulate some scale changes without triggering an updates
        mMockDetector.scaleFactor = 1.02f;
        mScaleListener.onScale(mMockDetector); 
        
        // End the gesture
        mScaleListener.onScaleEnd(mMockDetector);
        
        assertTrue(mTrackingActions.wasCalled);
        assertEquals(1.02f, mTrackingActions.lastScale, 0.001f);
        assertTrue("Finished flag must be true when gesture ends", mTrackingActions.lastFinished);
    }
}
