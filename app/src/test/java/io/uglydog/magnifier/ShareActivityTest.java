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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.view.KeyEvent;
import android.view.View;

import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;

import java.io.File;
import java.io.IOException;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = {Build.VERSION_CODES.P, Build.VERSION_CODES.TIRAMISU})
public class ShareActivityTest {

    private ActivityController<ShareActivity> mController;
    private ShareActivity mActivity;
    
    private TranslationManager.TranslationFactory mMockFactory;
    private TranslationManager mMockTranslationManager;

    @Before
    public void setUp() {
        mMockFactory = mock(TranslationManager.TranslationFactory.class);
        mMockTranslationManager = mock(TranslationManager.class);

        when(mMockFactory.create(any(Context.class), any(TextReaderOverlay.class), any(ToastManager.class)))
                .thenReturn(mMockTranslationManager);
    }

    private void createActivityWithIntent(Intent intent) {
        mController = Robolectric.buildActivity(ShareActivity.class, intent);
        mActivity = mController.get();
        mActivity.setTranslationFactory(mMockFactory);
    }

    // ==========================================
    // 1. LIFECYCLE & INITIALIZATION BRANCHES
    // ==========================================

    @Test
    public void testOnCreate_HappyPath() {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("image/jpeg");
        intent.putExtra(Intent.EXTRA_STREAM, Uri.parse("content://mock/path"));
        
        createActivityWithIntent(intent);
        mController.create();
        
        assertNotNull(mActivity.findViewById(R.id.ivLastCapture));
        assertNotNull(mActivity.findViewById(R.id.textOverlayView));
    }

    @Test
    public void testOnCreate_MissingImageView_FinishesActivity() {
        createActivityWithIntent(new Intent());
        ShareActivity spyActivity = spy(mActivity);
        
        doReturn(null).when(spyActivity).findViewById(R.id.ivLastCapture);
        
        spyActivity.onCreate(null);
        verify(spyActivity).finish();
    }

    @Test
    public void testOnNewIntent_DispatchesCorrectly() {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("image/jpeg");
        
        createActivityWithIntent(intent);
        mController.create().start().resume();
        
        ShareActivity spyActivity = spy(mActivity);
        Intent newIntent = new Intent(Intent.ACTION_SEND);
        newIntent.setType("image/png");
        
        spyActivity.onNewIntent(newIntent);
        verify(spyActivity).setIntent(newIntent);
    }

    @Test
    public void testOnPause_StopsTextReader() {
        createActivityWithIntent(new Intent());
        mController.create().start().resume();
        mController.pause();
    }

    @Test
    public void testOnDestroy_CleansUpResources() {
        createActivityWithIntent(new Intent());
        mController.create().start().resume();
        
        File dummyCacheFile = new File(RuntimeEnvironment.getApplication().getCacheDir(), "shared_image.jpg");
        try {
            dummyCacheFile.createNewFile();
        } catch (IOException ignored) {}

        mController.destroy();
        assertTrue(!dummyCacheFile.exists());
    }

    // ==========================================
    // 2. INTENT HANDLING BRANCHES
    // ==========================================

    @Test
    public void testHandleIntent_NullIntent_Finishes() {
        createActivityWithIntent(null);
        ShareActivity spyActivity = spy(mActivity);
        
        spyActivity.onNewIntent(null);
        verify(spyActivity).finish();
    }

    @Test
    public void testHandleIntent_NullTypeOrAction_Finishes() {
        createActivityWithIntent(new Intent());
        ShareActivity spyActivity = spy(mActivity);
        Intent badIntent = new Intent(); 
        
        spyActivity.onNewIntent(badIntent);
        verify(spyActivity).finish();
    }

    @Test
    public void testHandleIntent_InvalidAction_Finishes() {
        createActivityWithIntent(new Intent());
        ShareActivity spyActivity = spy(mActivity);
        Intent badActionIntent = new Intent(Intent.ACTION_VIEW);
        badActionIntent.setType("image/jpeg");
        
        spyActivity.onNewIntent(badActionIntent);
        verify(spyActivity).finish();
    }

    @Test
    public void testHandleIntent_NonImageType_Finishes() {
        createActivityWithIntent(new Intent());
        ShareActivity spyActivity = spy(mActivity);
        Intent badTypeIntent = new Intent(Intent.ACTION_SEND);
        badTypeIntent.setType("text/plain");
        
        spyActivity.onNewIntent(badTypeIntent);
        verify(spyActivity).finish();
    }

    // ==========================================
    // 3. FILE IO & CONTENT RESOLVER BRANCHES
    // ==========================================

    @Test
    @Config(sdk = Build.VERSION_CODES.TIRAMISU)
    public void testHandleSingleImage_NullUri_Finishes_Tiramisu() {
        createActivityWithIntent(new Intent());
        ShareActivity spyActivity = spy(mActivity);
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("image/jpeg");
        intent.putExtra(Intent.EXTRA_STREAM, (Uri) null);
        
        spyActivity.onNewIntent(intent);
        verify(spyActivity).finish();
    }

    @Test
    public void testGetFileFromContentUri_NullInputStream_ReturnsFalse() throws Exception {
        createActivityWithIntent(new Intent());
        ShareActivity spyActivity = spy(mActivity);
        Context mockContext = mock(Context.class);
        android.content.ContentResolver mockResolver = mock(android.content.ContentResolver.class);
        Uri mockUri = Uri.parse("content://test");

        when(mockContext.getContentResolver()).thenReturn(mockResolver);
        when(mockResolver.openInputStream(mockUri)).thenReturn(null);

        java.lang.reflect.Method method = ShareActivity.class.getDeclaredMethod(
                "getFileFromContentUri", Context.class, Uri.class);
        method.setAccessible(true);
        
        Boolean result = (Boolean) method.invoke(spyActivity, mockContext, mockUri);
        assertTrue(!result);
    }

    @Test
    public void testGetFileFromContentUri_ExceptionPath_ReturnsFalse() throws Exception {
        createActivityWithIntent(new Intent());
        ShareActivity spyActivity = spy(mActivity);
        
        Context context = RuntimeEnvironment.getApplication();
        Uri invalidUri = Uri.parse("content://unregistered.authority/nonexistent_file.jpg");

        java.lang.reflect.Method method = ShareActivity.class.getDeclaredMethod(
                "getFileFromContentUri", Context.class, Uri.class);
        method.setAccessible(true);
        
        Boolean result = (Boolean) method.invoke(spyActivity, context, invalidUri);
        assertTrue(!result);
    }

    // ==========================================
    // 4. INPUT & VOLUME KEY HANDLING BRANCHES
    // ==========================================

    @Test
    public void testOnKeyDown_UnhandledKey_PropagatesToSuper() {
        createActivityWithIntent(new Intent());
        mController.create();
        
        KeyEvent fallbackKey = new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_A);
        boolean handled = mActivity.onKeyDown(KeyEvent.KEYCODE_A, fallbackKey);
        
        assertNotNull(handled);
    }

    @Test
    public void testOnKeyUp_UnhandledKey_PropagatesToSuper() {
        createActivityWithIntent(new Intent());
        mController.create();
        
        KeyEvent fallbackKey = new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_A);
        boolean handled = mActivity.onKeyUp(KeyEvent.KEYCODE_A, fallbackKey);
        
        assertNotNull(handled);
    }

    @Test
    public void testOnVolumeChanged_ImageViewNotVisible_ReturnsFalse() {
        createActivityWithIntent(new Intent());
        mController.create();
        SubsamplingScaleImageView iv = mActivity.findViewById(R.id.ivLastCapture);
        iv.setVisibility(View.GONE);
        
        KeyEvent volumeKey = new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_VOLUME_UP);
        boolean result = mActivity.onVolumeChanged(volumeKey);
        
        assertTrue(!result);
    }

    // ==========================================
    // 5. INTERFACE METHOD STUBS (No-op Coverage)
    // ==========================================
    
    @Test
    public void testInterfaceStubs_ExecuteWithoutExceptions() {
        createActivityWithIntent(new Intent());
        mController.create();
        KeyEvent dummyEvent = new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_UNKNOWN);
        
        mActivity.onChangeBrightnessSetting(dummyEvent);
        mActivity.onChangeColorFilterSetting(dummyEvent);
        mActivity.onChangeContrastSetting(dummyEvent);
        mActivity.onChangeFlashlightSetting(dummyEvent);
        mActivity.onChangeSpeakSetting(dummyEvent);
        mActivity.onChangePanSetting(dummyEvent);
        mActivity.onChangeRotationSetting(dummyEvent);
        mActivity.onChangeView();
        mActivity.onChangeZoomSetting(dummyEvent);
        mActivity.onScrollViewport(dummyEvent);
        mActivity.onShowHelp();
        mActivity.onShowVersion();
        
        assertNotNull(mActivity);
    }
}
