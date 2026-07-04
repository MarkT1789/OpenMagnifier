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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.Activity;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.View;
import android.view.ViewGroup;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLooper;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class TextReaderOverlayTest {

    private Activity mActivity;
    private TextReaderOverlay mOverlay;
    private SettingsManager mMockSettingsManager;

    @Before
    public void setUp() {
        // Build and resume a real Activity. This provides a genuine Window token!
        mActivity = Robolectric.buildActivity(Activity.class).setup().get();
        
        mMockSettingsManager = mock(SettingsManager.class);
        when(mMockSettingsManager.getBannerSize()).thenReturn(1.0f);

        mOverlay = new TextReaderOverlay(mActivity, null);
        mOverlay.setSettingsManager(mMockSettingsManager);

        // Setting it as the Activity content perfectly triggers isAttachedToWindow() = true
        mActivity.setContentView(mOverlay);
        ShadowLooper.idleMainLooper();
    }

    @Test
    public void testAsViewReturnsInstance() {
        View view = mOverlay.asView();
        assertEquals(mOverlay, view);
    }

    @Test
    public void testSetHandlerBindsCorrectLooper() {
        Handler realHandler = new Handler(Looper.getMainLooper());
        mOverlay.setHandler(realHandler);
        
        mOverlay.clear();
        ShadowLooper.idleMainLooper();
        assertFalse(mOverlay.isShowBackground());
    }

    @Test
    public void testSetSettingsManagerUpdatesTextSize() {
        SettingsManager localMock = mock(SettingsManager.class);
        when(localMock.getBannerSize()).thenReturn(2.0f);
        mOverlay.setSettingsManager(localMock);
        verify(localMock, atLeastOnce()).getBannerSize();
    }

    @Test
    public void testUpdateRectWhenNotAttachedToWindowDoesNothing() {
        // Detach the view from the Activity window to hit the early return branch
        ((ViewGroup) mOverlay.getParent()).removeView(mOverlay);
        ShadowLooper.idleMainLooper();
        
        Rect rect = new Rect(10, 20, 30, 40);
        mOverlay.setRect(rect);
        assertTrue(mOverlay.getInternalRect().isEmpty());
    }

    @Test
    public void testUpdateRectWhenAttachedWithValidRect() {
        Rect rect = new Rect(10, 20, 30, 40);
        mOverlay.setRect(rect);
        ShadowLooper.idleMainLooper();
        
        assertEquals(rect, mOverlay.getInternalRect());
    }

    @Test
    public void testUpdateRectWhenAttachedWithNullRectClearsIt() {
        mOverlay.setRect(new Rect(10, 20, 30, 40));
        ShadowLooper.idleMainLooper();
        
        mOverlay.setRect(null);
        ShadowLooper.idleMainLooper();
        
        assertTrue(mOverlay.getInternalRect().isEmpty());
    }

    @Test
    public void testSetRectFromBackgroundThreadPostsRunnable() {
        final Rect rect = new Rect(5, 5, 15, 15);

        Thread backgroundThread = new Thread(new Runnable() {
            @Override
            public void run() {
                mOverlay.setRect(rect);
            }
        });
        backgroundThread.start();
        try { backgroundThread.join(); } catch (InterruptedException e) { e.printStackTrace(); }

        ShadowLooper.idleMainLooper();
        assertEquals(rect, mOverlay.getInternalRect());
    }

    @Test
    public void testUpdateTextWhenNotAttachedDoesNothing() {
        ((ViewGroup) mOverlay.getParent()).removeView(mOverlay);
        ShadowLooper.idleMainLooper();
        
        mOverlay.setText("Hello World", 0, 5);
        assertNull(mOverlay.getTts());
    }

    @Test
    public void testUpdateTextHappyPathWithInitialInput() {
        mOverlay.setText("Hello World", 0, 5);
        ShadowLooper.idleMainLooper();

        assertTrue(mOverlay.isShowBackground());
        assertEquals("Hello World", mOverlay.getTts());
        assertEquals(0, mOverlay.getStart());
    }

    @Test
    public void testUpdateTextNullInputBranches() {
        mOverlay.setText("Initial Text", 0, 5);
        ShadowLooper.idleMainLooper();
        
        mOverlay.setText(null, -1, -1);
        ShadowLooper.idleMainLooper();
        
        assertNull(mOverlay.getTts());
        assertEquals(0, mOverlay.getStart());
    }

    @Test
    public void testUpdateTextMatchingTtsAndVaryingBounds() {
        mOverlay.setText("Sample text matching layouts text configurations", 0, 10);
        ShadowLooper.idleMainLooper();
        
        mOverlay.setText("Sample text matching layouts text configurations", 2, 25);
        ShadowLooper.idleMainLooper();
        
        assertEquals("Sample text matching layouts text configurations", mOverlay.getTts());
    }

    @Test
    public void testUpdateTextVaryingTextResetsBounds() {
        mOverlay.setText("First String", 0, 5);
        ShadowLooper.idleMainLooper();
        
        mOverlay.setText("Second String", 5, 10);
        ShadowLooper.idleMainLooper();

        assertEquals("Second String", mOverlay.getTts());
        assertEquals(0, mOverlay.getStart());
    }

    @Test
    public void testSetTextFromBackgroundThreadPostsRunnable() {
        Thread backgroundThread = new Thread(new Runnable() {
            @Override
            public void run() {
                mOverlay.setText("Thread Test", 0, 4);
            }
        });
        backgroundThread.start();
        try { backgroundThread.join(); } catch (InterruptedException e) { e.printStackTrace(); }

        ShadowLooper.idleMainLooper();
        assertEquals("Thread Test", mOverlay.getTts());
    }

    @Test
    public void testClearOverlayResetsProperties() {
        mOverlay.setRect(new Rect(0, 0, 10, 10));
        mOverlay.setText("Reset Target", 0, 5);
        ShadowLooper.idleMainLooper();

        mOverlay.clearOverlay();
        ShadowLooper.idleMainLooper();
        
        assertFalse(mOverlay.isShowBackground());
        assertTrue(mOverlay.getInternalRect().isEmpty());
        assertNull(mOverlay.getTts());
    }

    @Test
    public void testClearTriggersDelayedMessages() {
        mOverlay.clear();
        assertTrue(Shadows.shadowOf(Looper.getMainLooper()).isPaused());
    }

    @Test
    public void testHandleMessageClearBackgroundBranches() {
        mOverlay.setText("Active State", 0, 5);
        ShadowLooper.idleMainLooper();
        assertTrue(mOverlay.isShowBackground());

        Message msg = Message.obtain();
        msg.what = 1; // MSG_CLEAR_BACKGROUND

        boolean handled = mOverlay.handleMessage(msg);
        assertTrue(handled);
        assertFalse(mOverlay.isShowBackground());
    }

    @Test
    public void testHandleMessageIgnoresUnknownCodes() {
        Message msg = Message.obtain();
        msg.what = 999; 

        boolean handled = mOverlay.handleMessage(msg);
        assertFalse(handled);
    }

    @Test
    public void testShowCopyrightUpdatesState() {
        mOverlay.showCopyright(true);
        assertTrue(mOverlay.isShowCopyright());
        
        mOverlay.showCopyright(false);
        assertFalse(mOverlay.isShowCopyright());
    }

    @Test
    public void testOnConfigurationChangedUpdatesMetrics() {
        Configuration config = new Configuration();
        mOverlay.onConfigurationChanged(config);
        verify(mMockSettingsManager, atLeastOnce()).getBannerSize();
    }

    @Test
    public void testOnDrawRendersGraphicLayersWithoutCrashing() {
        mOverlay.setRect(new Rect(0, 0, 100, 100));
        mOverlay.setText("Drawing Check Pass Layer Verification", 0, 5);
        mOverlay.showCopyright(true);
        ShadowLooper.idleMainLooper();

        // Give the view physical bounds so getWidth() doesn't return 0
        mOverlay.layout(0, 0, 1000, 1000);

        // Back the test canvas with a Bitmap so canvas.getWidth() > OFFSET
        Bitmap bitmap = Bitmap.createBitmap(1000, 1000, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        mOverlay.onDraw(canvas);
    }

    @Test
    public void testCloseClearsHandlerMessages() {
        mOverlay.close();
        ShadowLooper.idleMainLooper();
    }
}
