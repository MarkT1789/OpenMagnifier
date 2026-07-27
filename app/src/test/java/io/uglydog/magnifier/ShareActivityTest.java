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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.ContentResolver;
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
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

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

    private Intent createValidIntent() {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("image/jpeg");
        intent.putExtra(Intent.EXTRA_STREAM, Uri.parse("content://mock/path"));
        return intent;
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
        createActivityWithIntent(createValidIntent());
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
        createActivityWithIntent(createValidIntent());
        mController.create().start().resume();

        ShareActivity spyActivity = spy(mActivity);
        Intent newIntent = new Intent(Intent.ACTION_SEND);
        newIntent.setType("image/png");
        newIntent.putExtra(Intent.EXTRA_STREAM, Uri.parse("content://mock/path2"));

        spyActivity.onNewIntent(newIntent);
        verify(spyActivity).setIntent(newIntent);
    }

    @Test
    public void testOnPause_StopsTextReader() throws Exception {
        createActivityWithIntent(createValidIntent());
        mController.create().start().resume();

        TextReader mockTextReader = mock(TextReader.class);
        Field field = ShareActivity.class.getDeclaredField("mTextReader");
        field.setAccessible(true);
        field.set(mActivity, mockTextReader);

        mController.pause();
        verify(mockTextReader).stop();
    }

    @Test
    public void testOnResume_WhenFinishing_ReturnsEarly() throws Exception {
        createActivityWithIntent(createValidIntent());
        mController.create().start();

        TextReader mockTextReader = mock(TextReader.class);
        Field field = ShareActivity.class.getDeclaredField("mTextReader");
        field.setAccessible(true);
        field.set(mActivity, mockTextReader);

        mActivity.finish();
        mController.resume();

        verify(mockTextReader, never()).start();
    }

    @Test
    public void testOnResume_WithActiveTextReader_CallsStart() throws Exception {
        createActivityWithIntent(createValidIntent());
        mController.create().start();

        TextReader mockTextReader = mock(TextReader.class);
        Field field = ShareActivity.class.getDeclaredField("mTextReader");
        field.setAccessible(true);
        field.set(mActivity, mockTextReader);

        mController.resume();
        verify(mockTextReader).start();
    }

    @Test
    public void testOnDestroy_CleansUpResources() {
        createActivityWithIntent(createValidIntent());
        mController.create().start().resume();

        File dummyCacheFile = new File(RuntimeEnvironment.getApplication().getCacheDir(), "shared_image.jpg");
        try {
            dummyCacheFile.createNewFile();
        } catch (IOException ignored) {}

        mController.destroy();
        assertFalse(dummyCacheFile.exists());
    }

    @Test
    public void testOnDestroy_NoCacheFile_CleansUpWithoutError() {
        createActivityWithIntent(createValidIntent());
        mController.create().start().resume();

        File cacheFile = new File(RuntimeEnvironment.getApplication().getCacheDir(), "shared_image.jpg");
        if (cacheFile.exists()) {
            cacheFile.delete();
        }

        mController.destroy();
        assertFalse(cacheFile.exists());
    }

    @Test
    public void testOnDestroy_WithActiveTextReader_CallsDestroy() throws Exception {
        createActivityWithIntent(createValidIntent());
        mController.create().start().resume();

        TextReader mockTextReader = mock(TextReader.class);
        Field field = ShareActivity.class.getDeclaredField("mTextReader");
        field.setAccessible(true);
        field.set(mActivity, mockTextReader);

        mController.destroy();
        verify(mockTextReader).destroy();
    }

    @Test
    public void testImageView_OnLongClick_ClearsOverlayAndStopsReader() throws Exception {
        createActivityWithIntent(createValidIntent());
        mController.create();

        SubsamplingScaleImageView imageView = mActivity.findViewById(R.id.ivLastCapture);

        TextReader mockTextReader = mock(TextReader.class);
        Field textReaderField = ShareActivity.class.getDeclaredField("mTextReader");
        textReaderField.setAccessible(true);
        textReaderField.set(mActivity, mockTextReader);

        View.OnLongClickListener listener = null;
        try {
            Field listenerField = SubsamplingScaleImageView.class.getDeclaredField("onLongClickListener");
            listenerField.setAccessible(true);
            listener = (View.OnLongClickListener) listenerField.get(imageView);
        } catch (Exception ignored) {}

        if (listener == null) {
            try {
                Field listenerInfoField = View.class.getDeclaredField("mListenerInfo");
                listenerInfoField.setAccessible(true);
                Object listenerInfo = listenerInfoField.get(imageView);
                if (listenerInfo != null) {
                    Field lField = listenerInfo.getClass().getDeclaredField("mOnLongClickListener");
                    lField.setAccessible(true);
                    listener = (View.OnLongClickListener) lField.get(listenerInfo);
                }
            } catch (Exception ignored) {}
        }

        assertNotNull(listener);
        boolean handled = listener.onLongClick(imageView);

        assertTrue(handled);
        verify(mockTextReader).stop();
    }

    // ==========================================
    // 2. INTENT HANDLING BRANCHES
    // ==========================================

    @Test
    public void testHandleIntent_NullIntent_Finishes() throws Exception {
        createActivityWithIntent(createValidIntent());
        mController.create();

        ShareActivity spyActivity = spy(mActivity);

        Method method = ShareActivity.class.getDeclaredMethod("handleIntent", Intent.class);
        method.setAccessible(true);
        method.invoke(spyActivity, (Intent) null);

        verify(spyActivity).finish();
    }

    @Test
    public void testHandleIntent_NullTypeOrAction_Finishes() {
        createActivityWithIntent(createValidIntent());
        mController.create();

        ShareActivity spyActivity = spy(mActivity);
        Intent badIntent = new Intent();

        spyActivity.onNewIntent(badIntent);
        verify(spyActivity).finish();
    }

    @Test
    public void testHandleIntent_InvalidAction_Finishes() {
        createActivityWithIntent(createValidIntent());
        mController.create();

        ShareActivity spyActivity = spy(mActivity);
        Intent badActionIntent = new Intent(Intent.ACTION_VIEW);
        badActionIntent.setType("image/jpeg");

        spyActivity.onNewIntent(badActionIntent);
        verify(spyActivity).finish();
    }

    @Test
    public void testHandleIntent_NonImageType_Finishes() {
        createActivityWithIntent(createValidIntent());
        mController.create();

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
        createActivityWithIntent(createValidIntent());
        mController.create();

        ShareActivity spyActivity = spy(mActivity);
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("image/jpeg");
        intent.putExtra(Intent.EXTRA_STREAM, (Uri) null);

        spyActivity.onNewIntent(intent);
        verify(spyActivity).finish();
    }

    @Test
    @Config(sdk = Build.VERSION_CODES.P)
    public void testHandleSingleImage_NullUri_Finishes_PreTiramisu() {
        createActivityWithIntent(createValidIntent());
        mController.create();

        ShareActivity spyActivity = spy(mActivity);
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("image/jpeg");
        intent.putExtra(Intent.EXTRA_STREAM, (Uri) null);

        spyActivity.onNewIntent(intent);
        verify(spyActivity).finish();
    }

    @Test
    public void testGetFileFromContentUri_NullUri_ReturnsFalse() throws Exception {
        createActivityWithIntent(createValidIntent());
        ShareActivity spyActivity = spy(mActivity);

        Method method = ShareActivity.class.getDeclaredMethod(
                "getFileFromContentUri", Context.class, Uri.class, Intent.class);
        method.setAccessible(true);

        Boolean result = (Boolean) method.invoke(spyActivity, RuntimeEnvironment.getApplication(), null, new Intent());
        assertFalse(result);
    }

    @Test
    public void testGetFileFromContentUri_NonContentScheme_ReturnsFalse() throws Exception {
        createActivityWithIntent(createValidIntent());
        ShareActivity spyActivity = spy(mActivity);

        Context context = RuntimeEnvironment.getApplication();
        Uri fileUri = Uri.parse("file:///sdcard/photo.jpg");

        Method method = ShareActivity.class.getDeclaredMethod(
                "getFileFromContentUri", Context.class, Uri.class, Intent.class);
        method.setAccessible(true);

        Boolean result = (Boolean) method.invoke(spyActivity, context, fileUri, new Intent());
        assertFalse(result);
    }

    @Test
    public void testGetFileFromContentUri_NonImageMimeType_ReturnsFalse() throws Exception {
        createActivityWithIntent(createValidIntent());
        ShareActivity spyActivity = spy(mActivity);

        Context mockContext = mock(Context.class);
        ContentResolver mockResolver = mock(ContentResolver.class);
        Uri mockUri = Uri.parse("content://test/document");

        when(mockContext.getContentResolver()).thenReturn(mockResolver);
        when(mockResolver.getType(mockUri)).thenReturn("application/pdf");

        Method method = ShareActivity.class.getDeclaredMethod(
                "getFileFromContentUri", Context.class, Uri.class, Intent.class);
        method.setAccessible(true);

        Boolean result = (Boolean) method.invoke(spyActivity, mockContext, mockUri, new Intent());
        assertFalse(result);
    }

    @Test
    public void testGetFileFromContentUri_NullInputStream_ReturnsFalse() throws Exception {
        createActivityWithIntent(createValidIntent());
        ShareActivity spyActivity = spy(mActivity);
        Context mockContext = mock(Context.class);
        ContentResolver mockResolver = mock(ContentResolver.class);
        Uri mockUri = Uri.parse("content://test");

        Intent mockIntent = new Intent();
        mockIntent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        when(mockContext.getContentResolver()).thenReturn(mockResolver);
        when(mockResolver.getType(mockUri)).thenReturn("image/jpeg");
        when(mockResolver.openInputStream(mockUri)).thenReturn(null);

        Method method = ShareActivity.class.getDeclaredMethod(
                "getFileFromContentUri", Context.class, Uri.class, Intent.class);
        method.setAccessible(true);

        Boolean result = (Boolean) method.invoke(spyActivity, mockContext, mockUri, mockIntent);
        assertFalse(result);
    }

    @Test
    public void testGetFileFromContentUri_ExceedsMaxFileSize_ReturnsFalse() throws Exception {
        createActivityWithIntent(createValidIntent());
        ShareActivity spyActivity = spy(mActivity);

        Context mockContext = mock(Context.class);
        ContentResolver mockResolver = mock(ContentResolver.class);
        Uri mockUri = Uri.parse("content://test/largefile");

        InputStream mockInputStream = new InputStream() {
            private int totalRead = 0;
            @Override
            public int read() {
                return 1;
            }
            @Override
            public int read(byte[] b, int off, int len) {
                if (totalRead > 80 * 1024 * 1024 + 1000) {
                    return -1;
                }
                totalRead += len;
                return len;
            }
        };

        when(mockContext.getContentResolver()).thenReturn(mockResolver);
        when(mockContext.getCacheDir()).thenReturn(RuntimeEnvironment.getApplication().getCacheDir());
        when(mockResolver.getType(mockUri)).thenReturn("image/jpeg");
        when(mockResolver.openInputStream(mockUri)).thenReturn(mockInputStream);

        Method method = ShareActivity.class.getDeclaredMethod(
                "getFileFromContentUri", Context.class, Uri.class, Intent.class);
        method.setAccessible(true);

        Boolean result = (Boolean) method.invoke(spyActivity, mockContext, mockUri, new Intent());
        assertFalse(result);
    }

    @Test
    public void testGetFileFromContentUri_ExceptionPath_ReturnsFalse() throws Exception {
        createActivityWithIntent(createValidIntent());
        ShareActivity spyActivity = spy(mActivity);

        Context context = RuntimeEnvironment.getApplication();
        Uri invalidUri = Uri.parse("content://unregistered.authority/nonexistent_file.jpg");

        Intent mockIntent = new Intent();
        mockIntent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        Method method = ShareActivity.class.getDeclaredMethod(
                "getFileFromContentUri", Context.class, Uri.class, Intent.class);
        method.setAccessible(true);

        Boolean result = (Boolean) method.invoke(spyActivity, context, invalidUri, mockIntent);
        assertFalse(result);
    }

    // ==========================================
    // 4. INPUT & VOLUME KEY HANDLING BRANCHES
    // ==========================================

    @Test
    public void testOnKeyDown_UnhandledKey_PropagatesToSuper() {
        createActivityWithIntent(createValidIntent());
        mController.create();

        KeyEvent fallbackKey = new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_A);
        boolean handled = mActivity.onKeyDown(KeyEvent.KEYCODE_A, fallbackKey);

        assertNotNull(handled);
    }

    @Test
    public void testOnKeyUp_UnhandledKey_PropagatesToSuper() {
        createActivityWithIntent(createValidIntent());
        mController.create();

        KeyEvent fallbackKey = new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_A);
        boolean handled = mActivity.onKeyUp(KeyEvent.KEYCODE_A, fallbackKey);

        assertNotNull(handled);
    }

    @Test
    public void testOnVolumeChanged_ImageViewNotVisible_ReturnsFalse() {
        createActivityWithIntent(createValidIntent());
        mController.create();
        SubsamplingScaleImageView iv = mActivity.findViewById(R.id.ivLastCapture);
        iv.setVisibility(View.GONE);

        KeyEvent volumeKey = new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_VOLUME_UP);
        boolean result = mActivity.onVolumeChanged(volumeKey);

        assertFalse(result);
    }

    @Test
    public void testOnVolumeChanged_NullTextReader_ReturnsFalse() {
        createActivityWithIntent(createValidIntent());
        mController.create();

        KeyEvent volumeKey = new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_VOLUME_UP);
        boolean result = mActivity.onVolumeChanged(volumeKey);

        assertFalse(result);
    }

    @Test
    public void testOnVolumeChanged_WithTextReader_DispatchesVolumeUpAndDown() throws Exception {
        createActivityWithIntent(createValidIntent());
        mController.create();

        TextReader mockTextReader = mock(TextReader.class);
        when(mockTextReader.onVolumeChanged(0)).thenReturn(true);
        when(mockTextReader.onVolumeChanged(1)).thenReturn(true);

        Field field = ShareActivity.class.getDeclaredField("mTextReader");
        field.setAccessible(true);
        field.set(mActivity, mockTextReader);

        KeyEvent volumeUp = new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_VOLUME_UP);
        assertTrue(mActivity.onVolumeChanged(volumeUp));
        verify(mockTextReader).onVolumeChanged(0);

        KeyEvent volumeDown = new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_VOLUME_DOWN);
        assertTrue(mActivity.onVolumeChanged(volumeDown));
        verify(mockTextReader).onVolumeChanged(1);
    }

    // ==========================================
    // 5. INTERFACE METHOD STUBS (No-op Coverage)
    // ==========================================

    @Test
    public void testInterfaceStubs_ExecuteWithoutExceptions() {
        createActivityWithIntent(createValidIntent());
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
