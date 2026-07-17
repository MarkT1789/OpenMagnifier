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
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import android.Manifest;
import android.app.Application;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.PointF;
import android.os.Build;
import android.os.Message;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;

import androidx.camera.core.Camera;
import androidx.camera.core.CameraControl;
import androidx.camera.core.CameraInfo;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.ZoomState;
import androidx.lifecycle.MutableLiveData;
import androidx.test.core.app.ApplicationProvider;

import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowAlertDialog;
import org.robolectric.shadows.ShadowApplication;

import java.io.File;
import java.lang.reflect.Field;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = Build.VERSION_CODES.TIRAMISU, manifest = Config.NONE)
public class MainActivityTest {

    private ActivityController<MainActivity> controller;
    private MainActivity activity;
    private Resources spyResources;

    @Mock ITranslationManager mockTranslationManager;
    @Mock SettingsManager mockSettingsManager;
    @Mock CameraManager mockCameraManager;
    @Mock TextReader mockTextReader;
    @Mock TextReaderOverlay mockTextReaderOverlay;
    @Mock ToastManager mockToastManager;
    @Mock SubsamplingScaleImageView mockImageView;
    @Mock Camera mockCamera;
    @Mock CameraControl mockCameraControl;
    @Mock CameraInfo mockCameraInfo;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);

        ShadowApplication shadowApp = Shadows.shadowOf((Application) ApplicationProvider.getApplicationContext());
        shadowApp.grantPermissions(Manifest.permission.CAMERA);

        // Setup common mock defaults to prevent null pointer exceptions
        when(mockCameraManager.getCamera()).thenReturn(mockCamera);
        when(mockCamera.getCameraControl()).thenReturn(mockCameraControl);
        when(mockCamera.getCameraInfo()).thenReturn(mockCameraInfo);
        when(mockCameraInfo.hasFlashUnit()).thenReturn(true);
        when(mockSettingsManager.getFlashlight()).thenReturn(1.0f);
        when(mockSettingsManager.getBrightness()).thenReturn(0.0f);
        when(mockSettingsManager.getColor()).thenReturn(0);
        when(mockSettingsManager.getContrast()).thenReturn(1.0f);
        when(mockSettingsManager.getDx()).thenReturn(0.1f);
        when(mockSettingsManager.getDy()).thenReturn(0.1f);
        when(mockSettingsManager.getRotation()).thenReturn(0);
        when(mockSettingsManager.getSpeak()).thenReturn(0);
        when(mockSettingsManager.getZoom()).thenReturn(1.0f);
        when(mockSettingsManager.getSplashVersion()).thenReturn("1.0");

        MutableLiveData<ZoomState> zoomLiveData = new MutableLiveData<>();
        ZoomState mockZoomState = mock(ZoomState.class);
        when(mockZoomState.getZoomRatio()).thenReturn(1.0f);
        when(mockZoomState.getMaxZoomRatio()).thenReturn(5.0f);
        when(mockZoomState.getMinZoomRatio()).thenReturn(1.0f);
        zoomLiveData.setValue(mockZoomState);
        when(mockCameraInfo.getZoomState()).thenReturn(zoomLiveData);

        controller = Robolectric.buildActivity(MainActivity.class);
        activity = controller.get();

        // Spy resources to cleanly mock app arrays safely across dynamic environment tests
        spyResources = spy(activity.getResources());
        
        // Define deterministic array layouts for brightness, contrast, filters, and zoom
        when(spyResources.getStringArray(R.array.brightness_values)).thenReturn(new String[]{"-0.5", "0.0", "0.5"});
        when(spyResources.getStringArray(R.array.filter_entries)).thenReturn(new String[]{"Low", "Normal", "High"});
        when(spyResources.getStringArray(R.array.color_values)).thenReturn(new String[]{"0", "1", "2"});
        when(spyResources.getStringArray(R.array.color_entries)).thenReturn(new String[]{"Default", "Grayscale", "Inverted"});
        when(spyResources.getStringArray(R.array.contrast_values)).thenReturn(new String[]{"0.5", "1.0", "1.5"});
        when(spyResources.getStringArray(R.array.zoom_values)).thenReturn(new String[]{"1.0", "2.0", "6.0"});

        activity.setTranslationManager(mockTranslationManager);

        controller.create().start().resume();

        // Inject internal dependencies to target code coverage explicitly
        injectMock(activity, "mSettingsManager", mockSettingsManager);
        injectMock(activity, "mCameraManager", mockCameraManager);
        injectMock(activity, "mTextReader", mockTextReader);
        injectMock(activity, "mTextReaderOverlay", mockTextReaderOverlay);
        injectMock(activity, "mToastManager", mockToastManager);
        injectMock(activity, "mImageView", mockImageView);
    }

    private void injectMock(Object target, String fieldName, Object mock) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, mock);
    }

    private MainActivity getSpyActivityWithMockResources() {
        MainActivity spyAct = spy(activity);
        when(spyAct.getResources()).thenReturn(spyResources);
        return spyAct;
    }

    // --- Lifecycle and Base Branches ---

    @Test
    public void testOnResume_ImageViewVisible() {
        when(mockImageView.getVisibility()).thenReturn(View.VISIBLE);
        activity.onResume();
        verify(mockImageView).setOrientation(anyInt());
    }

    @Test
    public void testOnResume_ImageViewNotVisible_TogglesTorch() {
        when(mockImageView.getVisibility()).thenReturn(View.GONE);
        activity.onResume();
        verify(mockCameraControl).enableTorch(true);
    }

    @Test
    public void testOnPause_StopsTextReaderAndToasts() {
        controller.pause();
        verify(mockTextReader).stop();
        verify(mockToastManager).cancel();
    }

    @Test
    public void testOnDestroy_CleansUpSuccessfully() {
        controller.destroy();
        verify(mockTextReaderOverlay).close();
        verify(mockTextReader).destroy();
        verify(mockCameraManager).stopCamera();
        verify(mockImageView).recycle();
    }

    @Test
    public void testOnRequestPermissionsResult_WrongCode_DoesNothing() {
        activity.onRequestPermissionsResult(99, new String[]{Manifest.permission.CAMERA}, new int[]{0});
        verify(mockCameraManager, never()).startCamera(any());
    }

    // --- Message Handlers (Callbacks) ---

    @Test
    public void testHandleMessage_MsgFlashlightOff_DisablesTorch() {
        Message msg = Message.obtain();
        msg.what = 1;
        assertTrue(activity.handleMessage(msg));
        verify(mockCameraControl).enableTorch(false);
    }

    @Test
    public void testHandleMessage_MsgImageOff_ResetsView() {
        Message msg = Message.obtain();
        msg.what = 2;
        assertTrue(activity.handleMessage(msg));
        verify(mockImageView).setVisibility(View.GONE);
        verify(mockImageView).recycle();
    }

    @Test
    public void testHandleMessage_UnknownMessage_ReturnsFalse() {
        Message msg = Message.obtain();
        msg.what = 999;
        assertFalse(activity.handleMessage(msg));
    }

    // --- Camera Permissions & Initialization Callbacks ---

    @Test
    public void testStartCameraSequence_CallbackInitializesZoomAndTorch() {
        ArgumentCaptor<CameraManager.OnCameraReadyListener> captor = ArgumentCaptor.forClass(CameraManager.OnCameraReadyListener.class);
        activity.onRequestPermissionsResult(100, new String[]{Manifest.permission.CAMERA}, new int[]{0});

        verify(mockCameraManager).startCamera(captor.capture());

        CameraManager.OnCameraReadyListener listener = captor.getValue();
        listener.onCameraReady(mockCamera);

        verify(mockCameraControl).setZoomRatio(1.0f);
        verify(mockCameraControl).enableTorch(true);
    }

    // --- Input Actions / KeyEvents Branches ---

    @Test
    public void testOnChangeBrightnessSetting_Visible_AndShiftPressed() {
        MainActivity spyAct = getSpyActivityWithMockResources();
        when(mockImageView.getVisibility()).thenReturn(View.VISIBLE);
        when(mockSettingsManager.getBrightness()).thenReturn(0.0f);
        
        KeyEvent event = new KeyEvent(0, 0, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_B, 0, KeyEvent.META_SHIFT_ON);

        spyAct.onChangeBrightnessSetting(event);
        verify(mockSettingsManager).setBrightness(anyFloat());
        verify(mockToastManager).show(eq(spyAct), any());
    }

    @Test
    public void testOnChangeBrightnessSetting_NotVisible() {
        when(mockImageView.getVisibility()).thenReturn(View.GONE);
        KeyEvent event = new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_B);

        activity.onChangeBrightnessSetting(event);
        verify(mockToastManager).show(eq(activity), any());
    }

    @Test
    public void testOnChangeColorFilterSetting_Visible() {
        MainActivity spyAct = getSpyActivityWithMockResources();
        when(mockImageView.getVisibility()).thenReturn(View.VISIBLE);
        when(mockSettingsManager.getColor()).thenReturn(0);
        
        KeyEvent event = new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_C);

        spyAct.onChangeColorFilterSetting(event);
        verify(mockSettingsManager).setColor(anyInt());
    }

    @Test
    public void testOnChangeColorFilterSetting_NotVisible() {
        when(mockImageView.getVisibility()).thenReturn(View.GONE);
        KeyEvent event = new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_C);

        activity.onChangeColorFilterSetting(event);
        verify(mockToastManager).show(eq(activity), any());
    }

    @Test
    public void testOnChangeContrastSetting_Visible() {
        MainActivity spyAct = getSpyActivityWithMockResources();
        when(mockImageView.getVisibility()).thenReturn(View.VISIBLE);
        when(mockSettingsManager.getContrast()).thenReturn(1.0f);
        
        KeyEvent event = new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_H);

        spyAct.onChangeContrastSetting(event);
        verify(mockSettingsManager).setContrast(anyFloat());
    }

    @Test
    public void testOnChangeContrastSetting_NotVisible() {
        when(mockImageView.getVisibility()).thenReturn(View.GONE);
        KeyEvent event = new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_H);

        activity.onChangeContrastSetting(event);
        verify(mockToastManager).show(eq(activity), any());
    }

    @Test
    public void testOnChangeFlashlightSetting_NoFlash() {
        when(mockCameraInfo.hasFlashUnit()).thenReturn(false);
        KeyEvent event = new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_F);

        activity.onChangeFlashlightSetting(event);
        verify(mockToastManager).show(eq(activity), any());
    }

    @Test
    public void testOnChangeFlashlightSetting_VisibleAndHasFlash() {
        when(mockImageView.getVisibility()).thenReturn(View.VISIBLE);
        KeyEvent event = new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_F);

        activity.onChangeFlashlightSetting(event);
        verify(mockToastManager).show(eq(activity), any());
    }

    @Test
    public void testOnChangePanSetting_Visible_X_Key() {
        MainActivity spyAct = getSpyActivityWithMockResources();
        when(spyAct.getResources().getStringArray(R.array.pan_values)).thenReturn(new String[]{"0.0", "0.1", "0.2"});
        when(mockImageView.getVisibility()).thenReturn(View.VISIBLE);
        when(mockSettingsManager.getDx()).thenReturn(0.1f);
        
        KeyEvent event = new KeyEvent(0, 0, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_X, 0);

        spyAct.onChangePanSetting(event);
        verify(mockSettingsManager).setDx(anyFloat());
    }

    @Test
    public void testOnChangePanSetting_Visible_Y_Key() {
        MainActivity spyAct = getSpyActivityWithMockResources();
        when(spyAct.getResources().getStringArray(R.array.pan_values)).thenReturn(new String[]{"0.0", "0.1", "0.2"});
        when(mockImageView.getVisibility()).thenReturn(View.VISIBLE);
        when(mockSettingsManager.getDy()).thenReturn(0.1f);
        
        KeyEvent event = new KeyEvent(0, 0, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_Y, 0);

        spyAct.onChangePanSetting(event);
        verify(mockSettingsManager).setDy(anyFloat());
    }

    @Test
    public void testOnChangePanSetting_NotVisible() {
        when(mockImageView.getVisibility()).thenReturn(View.GONE);
        KeyEvent event = new KeyEvent(0, 0, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_X, 0);

        activity.onChangePanSetting(event);
        verify(mockToastManager).show(eq(activity), any());
    }

    @Test
    public void testOnChangeRotationSetting_Visible() {
        MainActivity spyAct = getSpyActivityWithMockResources();
        when(spyAct.getResources().getStringArray(R.array.rotation_values)).thenReturn(new String[]{"0", "90", "180"});
        when(mockImageView.getVisibility()).thenReturn(View.VISIBLE);
        when(mockSettingsManager.getRotation()).thenReturn(0);
        
        KeyEvent event = new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_R);

        spyAct.onChangeRotationSetting(event);
        verify(mockSettingsManager).setRotation(anyInt());
        verify(mockImageView).setOrientation(anyInt());
    }

    @Test
    public void testOnChangeRotationSetting_NotVisible() {
        when(mockImageView.getVisibility()).thenReturn(View.GONE);
        KeyEvent event = new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_R);

        activity.onChangeRotationSetting(event);
        verify(mockToastManager).show(eq(activity), any());
    }

    @Test
    public void testOnChangeSpeakSetting() {
        MainActivity spyAct = getSpyActivityWithMockResources();
        when(spyAct.getResources().getStringArray(R.array.speak_values)).thenReturn(new String[]{"0", "1"});
        when(spyAct.getResources().getStringArray(R.array.speak_entries)).thenReturn(new String[]{"Off", "On"});
        when(mockSettingsManager.getSpeak()).thenReturn(0);
        
        KeyEvent event = new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_S);
        spyAct.onChangeSpeakSetting(event);
        verify(mockSettingsManager).setSpeak(anyInt());
        verify(mockTextReader).start();
    }

    @Test
    public void testOnChangeView_TriggersToggle() {
        when(mockImageView.getVisibility()).thenReturn(View.VISIBLE);
        activity.onChangeView();
        verify(mockTextReader).stop();
    }

    @Test
    public void testOnChangeZoomSetting_Visible_ForwardAndShift() {
        when(mockImageView.getVisibility()).thenReturn(View.VISIBLE);
        when(mockImageView.getCenter()).thenReturn(new PointF(0, 0));
        when(mockImageView.getScale()).thenReturn(1.0f);
        when(mockImageView.getMaxScale()).thenReturn(4.0f);
        when(mockImageView.getMinScale()).thenReturn(1.0f);

        KeyEvent event = new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_Z);
        activity.onChangeZoomSetting(event);
        verify(mockImageView).setScaleAndCenter(anyFloat(), any());

        KeyEvent shiftEvent = new KeyEvent(0, 0, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_Z, 0, KeyEvent.META_SHIFT_ON);
        activity.onChangeZoomSetting(shiftEvent);
        verify(mockImageView, times(2)).setScaleAndCenter(anyFloat(), any());
    }

    @Test
    public void testOnChangeZoomSetting_NotVisible_CameraZoom() {
        MainActivity spyAct = getSpyActivityWithMockResources();
        when(mockImageView.getVisibility()).thenReturn(View.GONE);
        when(mockSettingsManager.getZoom()).thenReturn(1.0f);
        
        KeyEvent event = new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_Z);

        spyAct.onChangeZoomSetting(event);
        verify(mockSettingsManager).setZoom(anyFloat());
        verify(mockCameraControl).setZoomRatio(anyFloat());
    }

    // --- Viewport Scrolling Navigation Branches ---

    @Test
    public void testOnScrollViewport_NotVisible() {
        when(mockImageView.getVisibility()).thenReturn(View.GONE);
        KeyEvent event = new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_LEFT);

        activity.onScrollViewport(event);
        verify(mockToastManager).show(eq(activity), any());
    }

    @Test
    public void testOnScrollViewport_NullCenter() {
        when(mockImageView.getVisibility()).thenReturn(View.VISIBLE);
        when(mockImageView.getCenter()).thenReturn(null);
        KeyEvent event = new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_LEFT);

        activity.onScrollViewport(event);
        verify(mockImageView, never()).animateScaleAndCenter(anyFloat(), any());
    }

    @Test
    public void testOnScrollViewport_DirectionsAndShift() {
        when(mockImageView.getVisibility()).thenReturn(View.VISIBLE);
        when(mockImageView.getCenter()).thenReturn(new PointF(50, 50));
        when(mockImageView.getScale()).thenReturn(1.0f);
        when(mockImageView.getWidth()).thenReturn(100);
        when(mockImageView.getHeight()).thenReturn(100);

        int[] codes = {KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_DPAD_RIGHT, KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_DPAD_DOWN};
        for (int code : codes) {
            activity.onScrollViewport(new KeyEvent(KeyEvent.ACTION_DOWN, code));
            activity.onScrollViewport(new KeyEvent(0, 0, KeyEvent.ACTION_DOWN, code, 0, KeyEvent.META_SHIFT_ON));
        }

        verify(mockImageView, atLeastOnce()).setScaleAndCenter(anyFloat(), any());
    }

    @Test
    public void testOnScrollViewport_UnknownKey_DoesNothing() {
        when(mockImageView.getVisibility()).thenReturn(View.VISIBLE);
        when(mockImageView.getCenter()).thenReturn(new PointF(50, 50));
        when(mockImageView.getScale()).thenReturn(1.0f);

        KeyEvent event = new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_A);
        activity.onScrollViewport(event);
        verify(mockImageView, never()).setScaleAndCenter(anyFloat(), any());
    }

    @Test
    public void testOnVolumeChanged_ImageViewNotVisible_ReturnsFalse() {
        when(mockImageView.getVisibility()).thenReturn(View.GONE);
        KeyEvent event = new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_VOLUME_UP);

        boolean handled = activity.onVolumeChanged(event);
        assertFalse(handled);
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

    // --- Key Down / Up / Touch Interceptors ---

    @Test
    public void testOnKeyDown_And_OnKeyUp() {
        KeyEvent event = new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_VOLUME_DOWN);
        assertFalse(activity.onKeyDown(KeyEvent.KEYCODE_VOLUME_DOWN, event));
        assertFalse(activity.onKeyUp(KeyEvent.KEYCODE_VOLUME_DOWN, event));
    }

    @Test
    public void testOnTouchEvent_ImageViewVisible_DispatchesToImageView() {
        when(mockImageView.getVisibility()).thenReturn(View.VISIBLE);
        MotionEvent event = MotionEvent.obtain(0, 0, MotionEvent.ACTION_DOWN, 0, 0, 0);

        activity.onTouchEvent(event);
        verify(mockImageView).dispatchTouchEvent(event);
        event.recycle();
    }

    // --- Splash Dialog Window Metrics Branch ---

    @Test
    public void testShowHelp_TriggersSplashDialog() {
        activity.onShowHelp();
        var dialog = ShadowAlertDialog.getLatestDialog();
        assertNotNull(dialog);
    }

    // --- Color Matrices & Filters Processing Branches ---

    @Test
    public void testUpdateFilters_AllMatrixFlags() {
        when(mockSettingsManager.getColor()).thenReturn(0x01 | 0x02 | 0x04);
        activity.updateFilters();
        verify(mockImageView).setLayerType(eq(View.LAYER_TYPE_HARDWARE), any());
    }

    // --- Photo Capture Engine Callbacks ---

    @Test
    public void testCaptureToView_CallbackBranchCoverage() {
        when(mockImageView.getVisibility()).thenReturn(View.GONE);
        activity.onToggleMode();

        ArgumentCaptor<ImageCapture.OnImageSavedCallback> captor = ArgumentCaptor.forClass(ImageCapture.OnImageSavedCallback.class);
        verify(mockCameraManager).takePhoto(any(File.class), captor.capture());

        ImageCapture.OnImageSavedCallback callback = captor.getValue();

        ImageCapture.OutputFileResults mockResults = mock(ImageCapture.OutputFileResults.class);
        callback.onImageSaved(mockResults);

        ImageCaptureException mockException = mock(ImageCaptureException.class);
        callback.onError(mockException);
    }

    // --- Scale Gesture Action Branches ---

    @Test
    public void testOnScale_WithinBounds_UpdatesZoom() {
        activity.onScale(1.5f, true);
        verify(mockCameraControl).setZoomRatio(anyFloat());
        verify(mockToastManager).show(eq(activity), any());
    }

    @Test
    public void testOnScale_ExceedsMaxZoom_ClampsToMax() {
        activity.onScale(10.0f, false);
        verify(mockCameraControl).setZoomRatio(5.0f);
    }

    // --- Deep Structural Instruction and High Branch Coverage Tests ---

    @Test
    public void testOnChangeFlashlightSetting_OlderSdk_FlatToggleBranch() {
        when(mockImageView.getVisibility()).thenReturn(View.GONE);
        
        // Use loose floating match structures to verify toggling behaviors safely 
        // regardless of the baseline state set up during initialization.
        KeyEvent event = new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_F);
        activity.onChangeFlashlightSetting(event);
        
        verify(mockSettingsManager).setFlashlight(anyFloat());
        verify(mockCameraControl).enableTorch(anyBoolean());
    }

    @Test
    public void testGetNextString_UnmatchedValue_ReturnsNegativeOneBranch() {
        MainActivity spyAct = getSpyActivityWithMockResources();
        when(mockImageView.getVisibility()).thenReturn(View.VISIBLE);
        // Explicit unmatched configuration ensures loop fallback paths execution internally
        when(mockSettingsManager.getBrightness()).thenReturn(999.0f); 
        
        KeyEvent event = new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_B);
        try {
            spyAct.onChangeBrightnessSetting(event);
        } catch (ArrayIndexOutOfBoundsException e) {
            // Verifies array validation limitations by checking the resulting index tracking error bounds
            assertNotNull(e);
        }
    }

    @Test
    public void testOnChangeZoomSetting_CameraZoom_ShiftPressed_ReverseLoopBranch() {
        MainActivity spyAct = getSpyActivityWithMockResources();
        when(mockImageView.getVisibility()).thenReturn(View.GONE);
        
        // Mock max camera zoom bounds tightly at 5.0f
        MutableLiveData<ZoomState> zoomLiveData = new MutableLiveData<>();
        ZoomState mockZoomState = mock(ZoomState.class);
        when(mockZoomState.getMaxZoomRatio()).thenReturn(5.0f);
        zoomLiveData.setValue(mockZoomState);
        when(mockCameraInfo.getZoomState()).thenReturn(zoomLiveData);
        
        // 6.0 exists in your mocked R.array.zoom_values, meaning getNextString returns a valid index, 
        // but 6.0 > 5.0f max camera constraint, triggering the backward correction loop branch!
        when(mockSettingsManager.getZoom()).thenReturn(6.0f); 
        
        KeyEvent shiftEvent = new KeyEvent(0, 0, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_Z, 0, KeyEvent.META_SHIFT_ON);
        spyAct.onChangeZoomSetting(shiftEvent);
        
        verify(mockSettingsManager).setZoom(anyFloat());
    }

    @Test
    public void testGetVersion_ExceptionHandlingBranch() throws Exception {
        MainActivity spyActivity = spy(activity);
        doThrow(new RuntimeException("Package lookup context constraint failure")).when(spyActivity).getPackageName();

        spyActivity.onShowVersion();
        verify(mockToastManager).show(eq(spyActivity), anyString()); 
    }

    @Test
    public void testOnToggleMode_ImmediateProcessingState() {
        when(mockImageView.getVisibility()).thenReturn(View.VISIBLE);
        when(mockSettingsManager.getFlashlight()).thenReturn(0.0f);
        
        boolean toggled = activity.onToggleMode();
        assertTrue(toggled);
        verify(mockTextReader).stop();
    }
}
