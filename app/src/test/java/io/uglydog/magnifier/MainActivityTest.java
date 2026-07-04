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

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.Manifest;
import android.app.Application;
import android.content.Intent;
import android.os.Build;
import android.os.Message;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;

import androidx.camera.core.Camera;
import androidx.camera.core.CameraControl;
import androidx.camera.core.CameraInfo;
import androidx.camera.core.ZoomState;
import androidx.lifecycle.MutableLiveData;
import androidx.test.core.app.ApplicationProvider;

import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowApplication;

import java.io.File;
import java.lang.reflect.Field;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = Build.VERSION_CODES.TIRAMISU, manifest = Config.NONE)
public class MainActivityTest {

    private ActivityController<MainActivity> controller;
    private MainActivity activity;

    @Mock ITranslationManager mockTranslationManager;
    @Mock SettingsManager mockSettingsManager;
    @Mock CameraManager mockCameraManager;
    @Mock TextReader mockTextReader;
    @Mock ToastManager mockToastManager;
    @Mock SubsamplingScaleImageView mockImageView;
    @Mock Camera mockCamera;
    @Mock CameraControl mockCameraControl;
    @Mock CameraInfo mockCameraInfo;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);

        // Grant permissions and setup application context
        ShadowApplication shadowApp = Shadows.shadowOf((Application) ApplicationProvider.getApplicationContext());
        shadowApp.grantPermissions(Manifest.permission.CAMERA);

        // Default mock behaviors to avoid null pointers
        when(mockCameraManager.getCamera()).thenReturn(mockCamera);
        when(mockCamera.getCameraControl()).thenReturn(mockCameraControl);
        when(mockCamera.getCameraInfo()).thenReturn(mockCameraInfo);
        when(mockCameraInfo.hasFlashUnit()).thenReturn(true);
        when(mockSettingsManager.getFlashlight()).thenReturn(1.0f);
        
        MutableLiveData<ZoomState> zoomLiveData = new MutableLiveData<>();
        ZoomState mockZoomState = mock(ZoomState.class);
        when(mockZoomState.getZoomRatio()).thenReturn(1.0f);
        zoomLiveData.setValue(mockZoomState);
        when(mockCameraInfo.getZoomState()).thenReturn(zoomLiveData);

        controller = Robolectric.buildActivity(MainActivity.class);
        activity = controller.get();

        // Inject Dependency
        activity.setTranslationManager(mockTranslationManager);
        
        // Drive lifecycle
        controller.create().start().resume();

        // Inject internal dependencies
        injectMock(activity, "mSettingsManager", mockSettingsManager);
        injectMock(activity, "mCameraManager", mockCameraManager);
        injectMock(activity, "mTextReader", mockTextReader);
        injectMock(activity, "mToastManager", mockToastManager);
        injectMock(activity, "mImageView", mockImageView);
    }

    private void injectMock(Object target, String fieldName, Object mock) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, mock);
    }

    @Test
    public void testHandleMessage_MsgFlashlightOff_DisablesTorch() {
        Message msg = Message.obtain();
        msg.what = 1; // MSG_FLASHLIGHT_OFF constant from MainActivity
        
        boolean handled = activity.handleMessage(msg);
        
        assertTrue("Message should be handled", handled);
        verify(mockCameraControl).enableTorch(false);
    }

    @Test
    public void testHandleMessage_MsgImageOff_ResetsView() {
        Message msg = Message.obtain();
        msg.what = 2; // MSG_IMAGE_OFF constant from MainActivity
        
        boolean handled = activity.handleMessage(msg);
        
        assertTrue("Message should be handled", handled);
        verify(mockImageView).setVisibility(View.GONE);
        verify(mockImageView).recycle();
    }

    @Test
    public void testOnPause_StopsTextReaderAndToasts() {
        controller.pause();
        verify(mockTextReader).stop();
        verify(mockToastManager).cancel();
    }

    @Test
    public void testHandleMessage_UnknownMessage_ReturnsFalseAndDoesNothing() {
        Message msg = Message.obtain();
        msg.what = 999; // Not a recognized message type

        boolean handled = activity.handleMessage(msg);

        assertFalse("Unrecognized message should not be handled", handled);
    }

    @Test
    public void testOnScale_WithinBounds_UpdatesZoomRatioAndShowsToastWhenFinished() {
        MutableLiveData<ZoomState> zoomLiveData = new MutableLiveData<>();
        ZoomState zoomState = mock(ZoomState.class);
        when(zoomState.getZoomRatio()).thenReturn(2.0f);
        when(zoomState.getMaxZoomRatio()).thenReturn(10.0f);
        when(zoomState.getMinZoomRatio()).thenReturn(1.0f);
        zoomLiveData.setValue(zoomState);
        when(mockCameraInfo.getZoomState()).thenReturn(zoomLiveData);

        activity.onScale(1.5f, true);

        // 2.0 * 1.5 = 3.0, within [1.0, 10.0], so it should be applied
        verify(mockCameraControl).setZoomRatio(3.0f);
        verify(mockToastManager).show(eq(activity), any());
    }

    @Test
    public void testOnScale_ExceedsMaxZoom_ClampsToMaxAndDoesNotToastWhenNotFinished() {
        MutableLiveData<ZoomState> zoomLiveData = new MutableLiveData<>();
        ZoomState zoomState = mock(ZoomState.class);
        when(zoomState.getZoomRatio()).thenReturn(5.0f);
        when(zoomState.getMaxZoomRatio()).thenReturn(8.0f);
        when(zoomState.getMinZoomRatio()).thenReturn(1.0f);
        zoomLiveData.setValue(zoomState);
        when(mockCameraInfo.getZoomState()).thenReturn(zoomLiveData);

        // 5.0 * 3.0 = 15.0, exceeds max of 8.0, should clamp to 8.0
        activity.onScale(3.0f, false);

        verify(mockCameraControl).setZoomRatio(8.0f);
        verify(mockToastManager, org.mockito.Mockito.never()).show(any(), any());
    }

    @Test
    public void testOnVolumeChanged_ImageViewNotVisible_ReturnsFalseAndSkipsTextReader() {
        when(mockImageView.getVisibility()).thenReturn(View.GONE);
        KeyEvent event = new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_VOLUME_UP);

        boolean handled = activity.onVolumeChanged(event);

        assertFalse(handled);
        verify(mockTextReader, org.mockito.Mockito.never()).onVolumeChanged(org.mockito.ArgumentMatchers.anyInt());
    }

    @Test
    public void testOnVolumeChanged_ImageViewVisible_DelegatesToTextReader() {
        when(mockImageView.getVisibility()).thenReturn(View.VISIBLE);
        when(mockTextReader.onVolumeChanged(0)).thenReturn(true);
        KeyEvent event = new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_VOLUME_UP);

        boolean handled = activity.onVolumeChanged(event);

        assertTrue(handled);
        verify(mockTextReader).onVolumeChanged(0);
    }

    @Test
    public void testOnShowVersion_ShowsVersionToast() {
        activity.onShowVersion();

        verify(mockToastManager).show(eq(activity), any());
    }

    @Test
    public void testOnToggleMode_ImageViewVisible_StopsTextReaderAndClearsOverlay() {
        when(mockImageView.getVisibility()).thenReturn(View.VISIBLE);
        when(mockSettingsManager.getFlashlight()).thenReturn(1.0f);

        boolean handled = activity.onToggleMode();

        assertTrue("Toggling off freeze-frame view should be handled", handled);
        verify(mockTextReader).stop();
    }

    @Test
    public void testOnToggleMode_ImageViewNotVisibleAndNotProcessing_CapturesPhoto() {
        when(mockImageView.getVisibility()).thenReturn(View.GONE);

        boolean handled = activity.onToggleMode();

        assertTrue("Starting freeze-frame capture should be handled", handled);
        verify(mockToastManager).show(eq(activity), any());
        verify(mockImageView).recycle();
        verify(mockCameraManager).takePhoto(any(File.class), any());
    }

    @Test
    public void testOnRequestPermissionsResult_PermissionGranted_StartsCameraSequence() {
        // Note: onCreate's initial startCameraSequence() call happened against the
        // real CameraManager, before setUp() swapped mCameraManager for the mock via
        // reflection. So this call is the only one the mock should observe.
        activity.onRequestPermissionsResult(
                100, // CAMERA_PERMISSION_CODE
                new String[]{Manifest.permission.CAMERA},
                new int[]{0});

        verify(mockCameraManager, times(1)).startCamera(any());
    }

    @Test
    public void testOnOpenSettings_StartsSettingsActivity() {
        activity.onOpenSettings();

        Intent started = Shadows.shadowOf(activity).getNextStartedActivity();
        assertNotNull("An Intent should have been started", started);
        assertEquals(SettingsActivity.class.getName(), started.getComponent().getClassName());
    }

    @Test
    public void testOnTouchEvent_ImageViewVisible_DispatchesToImageView() {
        when(mockImageView.getVisibility()).thenReturn(View.VISIBLE);
        MotionEvent event = MotionEvent.obtain(0, 0, MotionEvent.ACTION_DOWN, 0, 0, 0);

        activity.onTouchEvent(event);

        verify(mockImageView).dispatchTouchEvent(event);
        event.recycle();
    }

    @Test
    public void testOnDestroy_StopsCameraAndDestroysTextReader() {
        controller.destroy();

        verify(mockCameraManager).stopCamera();
        verify(mockTextReader).destroy();
    }
}
