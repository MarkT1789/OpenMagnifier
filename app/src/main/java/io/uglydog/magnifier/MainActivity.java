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

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.graphics.PointF;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.camera2.interop.ExperimentalCamera2Interop;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraControl;
import androidx.camera.core.CameraInfo;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.ZoomState;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.lifecycle.LiveData;
import androidx.preference.PreferenceManager;

import com.davemorrissey.labs.subscaleview.ImageSource;
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView;
import com.davemorrissey.labs.subscaleview.decoder.DecoderFactory;
import com.davemorrissey.labs.subscaleview.decoder.ImageRegionDecoder;
import com.davemorrissey.labs.subscaleview.decoder.SkiaImageRegionDecoder;

import java.io.File;

@ExperimentalCamera2Interop
public class MainActivity extends AppCompatActivity implements GestureListener.GestureActions, ScaleListener.ScaleActions, InputHandler.InputActions, Handler.Callback {
    private static final String TAG = MainActivity.class.getSimpleName();
    private static final String FILE = "enhanced_text.jpg";
    private static final int CAMERA_PERMISSION_CODE = 100;

    private CameraManager mCameraManager;
    private SettingsManager mSettingsManager;
    private SubsamplingScaleImageView mImageView;
    private GestureDetector mGestureDetector;
    private ScaleGestureDetector mScaleGestureDetector;

    private final Paint mFilterPaint = new Paint();
    private final ColorMatrix mColorMatrix = new ColorMatrix();
    private final ColorMatrix mGrayscaleMatrix = new ColorMatrix();

    private final Handler mHandler = new Handler(Looper.getMainLooper(), this);
    private boolean mIsProcessing = false;

    private TextReader mTextReader;
    private TextReaderOverlay mTextReaderOverlay;
    private ToastManager mToastManager;

    private static final int MSG_FLASHLIGHT_OFF = 1;
    private static final int MSG_IMAGE_OFF = 2;
    private static final int FLASHLIGHT_OFF_TIMEOUT = 2000;
    private static final int IMAGE_OFF_TIMEOUT = 750;

    /*
     * Lifecycle
     */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setupWindow();

        mToastManager = new ToastManager(new AndroidToastManagerFactory());
        mImageView = findViewById(R.id.ivLastCapture);
        mImageView.setMinimumScaleType(SubsamplingScaleImageView.SCALE_TYPE_CENTER_CROP);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        mSettingsManager = new SettingsManager(prefs);
        mCameraManager = new CameraManager(this, findViewById(R.id.viewFinder));
        mGestureDetector = new GestureDetector(this, new GestureListener(this));
        mScaleGestureDetector = new ScaleGestureDetector(this, new ScaleListener(this, new AndroidSystemClock()));
        mTextReaderOverlay = findViewById(R.id.textOverlayView);
        mTextReaderOverlay.setSettingsManager(mSettingsManager);
        ITranslationManager translationManager = new AndroidTranslationManagerFactory().create(this, mTextReaderOverlay, mToastManager);
        mTextReader = new TextReader(this, mImageView, mTextReaderOverlay, FILE, mSettingsManager, translationManager);

        showSplashDialog(false);
        updateFilters();

        mImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (BuildConfig.DEBUG) Logger.d(TAG, "onClick");
                onToggleMode();
            }
        });
        mImageView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if (BuildConfig.DEBUG) Logger.d(TAG, "onLongClick");
                mTextReader.stop();
                mTextReaderOverlay.showCopyright(false);
                mTextReaderOverlay.clearOverlay();
                return true;
            }
        });

        if (allPermissionsGranted()) {
            startCameraSequence();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_CODE);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (BuildConfig.DEBUG) Logger.d(TAG, "onPause");
        mTextReader.stop();
        mToastManager.cancel();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (BuildConfig.DEBUG) Logger.d(TAG, "onResume");

        mSettingsManager.reload();
        mTextReader.start();
        mTextReaderOverlay.updateTextSize();

        if (mImageView.getVisibility() == View.VISIBLE) {
            mImageView.setOrientation(mSettingsManager.getRotation());
        } else {
            toggleFlashlight(true);
        }

        final Camera camera = mCameraManager.getCamera();
        if (camera != null) {
            final float savedZoom = mSettingsManager.getZoom();
            camera.getCameraControl().setZoomRatio(savedZoom);
        }

        updateFilters();
    }

    @Override
    protected void onDestroy() {
        if (mTextReaderOverlay != null) {
            mTextReaderOverlay.close();
        }
        if (mTextReader != null) {
            mTextReader.destroy();
        }
        mHandler.removeCallbacksAndMessages(null);

        if (mCameraManager != null) {
            mCameraManager.stopCamera();
        }
        if (mImageView != null) {
            mImageView.recycle();
        }
        try {
            final File photoFile = new File(getCacheDir(), FILE);
            if (photoFile.exists()) {
                photoFile.delete();
            }
        } catch (Exception e) {
            Logger.e(TAG, "Error deleting cached photo: " + e);
        }
        super.onDestroy();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_CODE && allPermissionsGranted()) {
            startCameraSequence();
        }
    }

    /*
     * ScaleListener.ScaleAction
     */

    @Override
    public void onScale(final float scale, boolean finished) {
        if (BuildConfig.DEBUG) Logger.d(TAG, "onScale " + scale);
        final Camera camera = mCameraManager.getCamera();
        final LiveData<ZoomState> zoomStateLiveData = camera.getCameraInfo().getZoomState();
        if (zoomStateLiveData.getValue() != null) {
            final ZoomState zoomState = zoomStateLiveData.getValue();
            final float zoom = zoomState.getZoomRatio();
            final float maxZoom = zoomState.getMaxZoomRatio();
            final float minZoom = zoomState.getMinZoomRatio();
            float next = zoom * scale;
            if (next > maxZoom) next = maxZoom;
            if (next < minZoom) next = minZoom;
            if (zoom != next) {
                camera.getCameraControl().setZoomRatio(next);
            }
            if (finished) {
                mToastManager.show(this, getString(R.string.toast_view_live_zoom, next));
            }
        }
    }

    /*
     * InputHandler.InputAction
     */

    @Override
    public void onChangeBrightnessSetting(@NonNull final KeyEvent event) {
        if (mImageView.getVisibility() == View.VISIBLE) {
            final int id = getNextString(R.array.brightness_values, String.valueOf(mSettingsManager.getBrightness()), !event.isShiftPressed());
            final String value = getStringItem(R.array.brightness_values, id);
            final String key = getStringItem(R.array.filter_entries, id);
            mSettingsManager.setBrightness(Float.parseFloat(value));
            mToastManager.show(this, getString(R.string.toast_brightness, key));
            updateFilters();
        } else {
            mToastManager.show(this, getString(R.string.toast_brightness_disabled));
        }
    }

    @Override
    public void onChangeColorFilterSetting(@NonNull final KeyEvent event) {
        if (mImageView.getVisibility() == View.VISIBLE) {
            final int id = getNextString(R.array.color_values, String.valueOf(mSettingsManager.getColor()), !event.isShiftPressed());
            final String value = getStringItem(R.array.color_values, id);
            final String key = getStringItem(R.array.color_entries, id);
            mSettingsManager.setColor(Integer.parseInt(value));
            mToastManager.show(this, getString(R.string.toast_color, key));
            updateFilters();
        } else {
            mToastManager.show(this, getString(R.string.toast_color_disabled));
        }
    }

    @Override
    public void onChangeContrastSetting(@NonNull final KeyEvent event) {
        if (mImageView.getVisibility() == View.VISIBLE) {
            final int id = getNextString(R.array.contrast_values, String.valueOf(mSettingsManager.getContrast()), !event.isShiftPressed());
            final String value = getStringItem(R.array.contrast_values, id);
            final String key = getStringItem(R.array.filter_entries, id);
            mSettingsManager.setContrast(Float.parseFloat(value));
            mToastManager.show(this, getString(R.string.toast_contrast, key));
            updateFilters();
        } else {
            mToastManager.show(this, getString(R.string.toast_contrast_disabled));
        }
    }

    @Override
    public void onChangeFlashlightSetting(@NonNull final KeyEvent event) {
        if (!hasFlash()) {
            mToastManager.show(this, getString(R.string.toast_flashlight_none));
            return;
        }

        if (mImageView.getVisibility() == View.VISIBLE) {
            mToastManager.show(this, getString(R.string.toast_flashlight_disabled));
        } else {
            if (!hasFlashLevels()) {
                final float flashlight = mSettingsManager.getFlashlight() == 0.0f ? 1.0f : 0.0f;
                final int id = mSettingsManager.getFlashlight() == 0.0f ? 5 : 0;
                final String key = getStringItem(R.array.flashlight_entries, id);
                mToastManager.show(this, getString(R.string.toast_flashlight, key));
                mSettingsManager.setFlashlight(flashlight);
                toggleFlashlight(flashlight != 0.0f);
            } else {
                final int id = getNextString(R.array.flashlight_values, String.valueOf(mSettingsManager.getFlashlight()), !event.isShiftPressed());
                final String value = getStringItem(R.array.flashlight_values, id);
                final String key = getStringItem(R.array.flashlight_entries, id);
                mSettingsManager.setFlashlight(Float.parseFloat(value));
                mToastManager.show(this, getString(R.string.toast_flashlight, key));
                final float flashlight = Float.parseFloat(value);
                toggleFlashlight(flashlight != 0.0f);
            }
        }
    }

    @Override
    public void onChangePanSetting(@NonNull final KeyEvent event) {
        if (mImageView.getVisibility() == View.VISIBLE) {
            final String current = String.valueOf(event.getKeyCode() == KeyEvent.KEYCODE_X ?
                mSettingsManager.getDx() : mSettingsManager.getDy());
            final int id = getNextString(R.array.pan_values, current, !event.isShiftPressed());
            final float value = Float.parseFloat(getStringItem(R.array.pan_values, id));

            if (event.getKeyCode() == KeyEvent.KEYCODE_X) {
                mSettingsManager.setDx(value);
                mToastManager.show(this, getString(R.string.toast_scroll_horizontal, (int)(value * 100)));
            } else {
                mSettingsManager.setDy(value);
                mToastManager.show(this, getString(R.string.toast_scroll_vertical, (int)(value * 100)));
            }
        } else {
            if (event.getKeyCode() == KeyEvent.KEYCODE_X) {
                mToastManager.show(this, getString(R.string.toast_scroll_horizontal_disabled));
            } else {
                mToastManager.show(this, getString(R.string.toast_scroll_vertical_disabled));
            }
        }
    }

    @Override
    public void onChangeRotationSetting(@NonNull final KeyEvent event) {
        if (mImageView.getVisibility() == View.VISIBLE) {
            final int id = getNextString(R.array.rotation_values, String.valueOf(mSettingsManager.getRotation()), !event.isShiftPressed());
            final String value = getStringItem(R.array.rotation_values, id);
            final int rotation = Integer.parseInt(value);
            mSettingsManager.setRotation(rotation);
            mImageView.setOrientation(rotation);
            mToastManager.show(this, getString(R.string.toast_rotation, rotation));
        } else {
            mToastManager.show(this, getString(R.string.toast_rotation_disabled));
        }
    }

    @Override
    public void onChangeSpeakSetting(@NonNull final KeyEvent event) {
        final int id = getNextString(R.array.speak_values, String.valueOf(mSettingsManager.getSpeak()), !event.isShiftPressed());
        final String value = getStringItem(R.array.speak_values, id);
        final String key = getStringItem(R.array.speak_entries, id);
        final int speak = Integer.parseInt(value);
        mSettingsManager.setSpeak(speak);
        mToastManager.show(this, getString(R.string.toast_speak, key));
        mTextReader.start();
    }

    @Override
    public void onChangeView() {
        onToggleMode();
    }

    @Override
    public void onChangeZoomSetting(@NonNull final KeyEvent event) {
        if (mImageView.getVisibility() == View.VISIBLE) {
            final PointF center = mImageView.getCenter();
            float scale = mImageView.getScale();
            float max = mImageView.getMaxScale();
            float min = mImageView.getMinScale();
            final int steps = 4;
            final float d = (max - min) / steps;
            int id = (int)((scale - min + d/4) / d);
            if (!event.isShiftPressed()) {
                id = (id + 1) % (steps + 1);
            } else {
                id = (id - 1 + steps + 1) % (steps + 1);
            }
            scale = min + id * d;
            mImageView.setScaleAndCenter(scale, center);
            mToastManager.show(this, getString(R.string.toast_view_freeze_frame_zoom, scale / mImageView.getMinScale()));
        } else {
            final Camera camera = mCameraManager.getCamera();
            final LiveData<ZoomState> zoomStateLiveData = camera.getCameraInfo().getZoomState();
            if (zoomStateLiveData.getValue() != null) {
                final ZoomState zoomState = zoomStateLiveData.getValue();
                final float maxZoom = zoomState.getMaxZoomRatio();
                final int id = getNextString(R.array.zoom_values, String.valueOf(mSettingsManager.getZoom()), !event.isShiftPressed());
                final String value = getStringItem(R.array.zoom_values, id);
                float zoom = Float.parseFloat(value);
                if (zoom > maxZoom) {
                    if (!event.isShiftPressed()) {
                        zoom = Float.parseFloat(getStringItem(R.array.zoom_values, 0));
                    } else {
                        for (int i = id - 1 ; i > -1 ; i--) {
                            zoom = Float.parseFloat(getStringItem(R.array.zoom_values, i));
                            if (zoom <= maxZoom) break;
                        }
                    }
                }
                mSettingsManager.setZoom(zoom);
                mToastManager.show(this, getString(R.string.toast_view_live_zoom, zoom));
                camera.getCameraControl().setZoomRatio(zoom);
            }
        }
    }

    @Override
    public void onScrollViewport(@NonNull final KeyEvent event) {
        if (mImageView.getVisibility() != View.VISIBLE) {
            mToastManager.show(MainActivity.this, getString(R.string.toast_scroll_disabled));
            return;
        }

        final PointF center = mImageView.getCenter();
        final float scale = mImageView.getScale();
        if (center == null || scale == 0) return;

        final float dx = (mImageView.getWidth() * mSettingsManager.getDx()) / scale;
        final float dy = (mImageView.getHeight() * mSettingsManager.getDy()) / scale;

        PointF result;
        switch(event.getKeyCode()) {
            case KeyEvent.KEYCODE_DPAD_LEFT:
                if (!event.isShiftPressed()) {
                    result = new PointF(center.x - dx, center.y);
                } else {
                    result = new PointF(0, center.y);
                }
            break;
            case KeyEvent.KEYCODE_DPAD_RIGHT:
                if (!event.isShiftPressed()) {
                    result = new PointF(center.x + dx, center.y);
                } else {
                    result = new PointF(mImageView.getWidth(), center.y);
                }
            break;
            case KeyEvent.KEYCODE_DPAD_UP:
                if (!event.isShiftPressed()) {
                    result = new PointF(center.x, center.y - dy);
                } else {
                    result = new PointF(center.x, 0);
                }
            break;
            case KeyEvent.KEYCODE_DPAD_DOWN:
                if (!event.isShiftPressed()) {
                    result = new PointF(center.x, center.y + dy);
                } else {
                    result = new PointF(center.x, mImageView.getHeight());
                }
            break;
            default:
                return;
        }

        final SubsamplingScaleImageView.AnimationBuilder builder =
            mImageView.animateScaleAndCenter(scale, result);

        if (builder != null) {
            builder.withDuration(75)
               .withEasing(SubsamplingScaleImageView.EASE_OUT_QUAD)
               .start();
        } else {
            mImageView.setScaleAndCenter(scale, result);
        }
    }

    @Override
    public boolean onVolumeChanged(KeyEvent event) {
        if (mImageView.getVisibility() != View.VISIBLE) {
            return false;
        }
        final int cmd = event.getKeyCode() == KeyEvent.KEYCODE_VOLUME_UP ? 0 : 1;
        return mTextReader.onVolumeChanged(cmd);
    }

    @Override
    public void onShowHelp() {
        showSplashDialog(true);
    }

    @Override
    public void onShowVersion() {
        mToastManager.show(this, getString(R.string.version_known, getVersion()));
    }

    /*
     * InputHandler.InputAction private helper
     */

    private int getNextString(final int id, @NonNull final String current, final boolean forward) {
        String[] values = getResources().getStringArray(id);
        for (int i = 0; i < values.length; i++) {
            if (values[i].equals(current)) {
                if (forward) {
                    i = (i + 1) % values.length;
                } else {
                    i = (i - 1 + values.length) % values.length;
                }
                return i;
            }
        }
        return -1;
    }

    private String getStringItem(final int id, final int index) {
        String[] values = getResources().getStringArray(id);
        return values[index];
    }

    private String getVersion() {
        try {
            return getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
        } catch (Exception e) {
            return getString(R.string.version_unknown);
        }
    }

    /*
     * GestureListener.GestureAction
     */

    @Override
    public boolean onToggleMode() {
        if (mImageView.getVisibility() == View.VISIBLE) {
            mHandler.removeMessages(MSG_IMAGE_OFF);
            final boolean immediate = mHandler.hasMessages(MSG_FLASHLIGHT_OFF) || (mSettingsManager.getFlashlight() == 0.0f);
            mHandler.sendEmptyMessageDelayed(MSG_IMAGE_OFF, immediate ? 0 : IMAGE_OFF_TIMEOUT);

            final Camera camera = mCameraManager.getCamera();
            final LiveData<ZoomState> zoomStateLiveData = camera.getCameraInfo().getZoomState();
            if (zoomStateLiveData.getValue() != null) {
                final ZoomState zoomState = zoomStateLiveData.getValue();
                final float zoom = zoomState.getZoomRatio();
                mToastManager.show(this, getString(R.string.toast_view_live_zoom, zoom));
            }
            toggleFlashlight(true);
            mTextReader.stop();
            mTextReaderOverlay.showCopyright(false);
            mTextReaderOverlay.clearOverlay();
            return true;
        }

        if (!mIsProcessing) {
            mToastManager.show(this, getString(R.string.toast_view_freeze_frame));
            captureToView();
            return true;
        }
        return false;
    }

    @Override
    public void onOpenSettings() {
        startActivity(new Intent(this, SettingsActivity.class));
    }

    /*
     * KeyEvent and MotionEvent Listeners
     */

    @Override
    public boolean onKeyDown(final int keyCode, @NonNull final KeyEvent event) {
        if (InputHandler.handleKey(event, this)) {
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyUp(final int keyCode, @NonNull final KeyEvent event) {
        if (InputHandler.handleKey(event, this)) {
            return true;
        }
        return super.onKeyUp(keyCode, event);
    }

    @Override
    public boolean onTouchEvent(@NonNull final MotionEvent event) {
        if (mImageView.getVisibility() == View.VISIBLE) {
            return mImageView.dispatchTouchEvent(event);
        }

        boolean handled = mScaleGestureDetector.onTouchEvent(event);
        handled |= mGestureDetector.onTouchEvent(event);
        return handled || super.onTouchEvent(event);
    }

    /*
     * helper functions
     */

    private void setupWindow() {
        EdgeToEdge.enable(this);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            getWindow().getAttributes().layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
        }

        WindowInsetsControllerCompat controller = new WindowInsetsControllerCompat(getWindow(), getWindow().getDecorView());

        controller.hide(WindowInsetsCompat.Type.statusBars() | WindowInsetsCompat.Type.navigationBars());
        controller.setSystemBarsBehavior(WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
    }

    private boolean allPermissionsGranted() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == 0;
    }

    public void updateFilters() {
        final float contrast = mSettingsManager.getContrast();
        final float brightness = mSettingsManager.getBrightness();
        final int color = mSettingsManager.getColor();

        final float offset = 128f * (1f - contrast) + brightness;

        mColorMatrix.set(new float[] {
            contrast, 0, 0, 0, offset,
            0, contrast, 0, 0, offset,
            0, 0, contrast, 0, offset,
            0, 0, 0, 1, 0
        });

        // grayscale
        if ((color & 0x01) == 0x01) {
            mGrayscaleMatrix.setSaturation(0);
            mColorMatrix.postConcat(mGrayscaleMatrix);
        }

        // inverse
        if ((color & 0x02) == 0x02) {
            ColorMatrix inverseMatrix = new ColorMatrix(new float[] {
               -1.0f,  0,     0,    0, 255,
                0,    -1.0f,  0,    0, 255,
                0,     0,    -1.0f, 0, 255,
                0,     0,     0,    1, 0
            });
            mColorMatrix.postConcat(inverseMatrix);
        }

        // Black-Yellow High Contrast (AMD Filter)
        if ((color & 0x04) == 0x04) {
            ColorMatrix yellowMatrix = new ColorMatrix(new float[] {
                0.213f, 0.715f, 0.072f, 0, 0, // Red channel (Yellow is R+G)
                0.213f, 0.715f, 0.072f, 0, 0, // Green channel
                0,      0,      0,      0, 0, // Blue channel (Zeroed for black background)
                0,      0,      0, 1, 0      // Alpha channel
            });
            mColorMatrix.postConcat(yellowMatrix);
        }

        mFilterPaint.setColorFilter(new ColorMatrixColorFilter(mColorMatrix));
        mImageView.setLayerType(View.LAYER_TYPE_HARDWARE, mFilterPaint);
    }

    private void startCameraSequence() {
        mCameraManager.startCamera(new CameraManager.OnCameraReadyListener() {
            @Override
            public void onCameraReady(Camera camera) {
                // Initialize zoom on first load
                final float savedZoom = mSettingsManager.getZoom();
                camera.getCameraControl().setZoomRatio(savedZoom);
                mToastManager.show(MainActivity.this, getString(R.string.toast_view_live_zoom, savedZoom));
                toggleFlashlight(true);
            }
        });
    }

    private void captureToView() {
        mIsProcessing = true;
        mImageView.recycle();
        final File photoFile = new File(getCacheDir(), FILE);

        mCameraManager.takePhoto(photoFile, new ImageCapture.OnImageSavedCallback() {
            @Override
            public void onImageSaved(@NonNull ImageCapture.OutputFileResults results) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mImageView.setAlpha(0.0f);
                        mImageView.setVisibility(View.VISIBLE);

                        mImageView.setOnImageEventListener(new SubsamplingScaleImageView.DefaultOnImageEventListener() {
                            @Override
                            public void onReady() {
                                mImageView.setAlpha(1.0f);
                                mImageView.setFocusable(true);
                                mImageView.setFocusableInTouchMode(true);
                                mImageView.requestFocus();
                                mHandler.removeMessages(MSG_FLASHLIGHT_OFF);
                                mHandler.sendEmptyMessageDelayed(MSG_FLASHLIGHT_OFF, FLASHLIGHT_OFF_TIMEOUT);
                                mTextReader.start();
                            }
                        });

                        mImageView.setRegionDecoderFactory(new DecoderFactory<>() {
                            @NonNull
                            @Override
                            public ImageRegionDecoder make() {
                                try {
                                    return new SkiaImageRegionDecoder(Bitmap.Config.ARGB_8888);
                                } catch (Exception e) {
                                    return new SkiaImageRegionDecoder();
                                }
                            }
                        });
                        mImageView.setOrientation(mSettingsManager.getRotation());
                        mImageView.setImage(ImageSource.uri(Uri.fromFile(photoFile)));
                        mImageView.setMinimumTileDpi(120);
                        mIsProcessing = false;
                    }
                });
            }

            @Override
            public void onError(@NonNull ImageCaptureException e) {
                Logger.e(TAG, "Photo capture failed: " + e);
                mIsProcessing = false;
            }
        });
    }

    private void showSplashDialog(final boolean always) {
        if (!always && getVersion().equals(mSettingsManager.getSplashVersion())) {
            return;
        }

        final LayoutInflater inflater = getLayoutInflater();
        final View dialogView = inflater.inflate(R.layout.dialog_help, null);
        final CheckBox checkBox = dialogView.findViewById(R.id.dont_show_again);

        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.splash_title, getVersion()))
               .setView(dialogView)
               .setPositiveButton(getString(R.string.button_ok), new DialogInterface.OnClickListener() {
                   @Override
                   public void onClick(@NonNull final DialogInterface dialog, final int which) {
                       if (checkBox.isChecked()) {
                           mSettingsManager.setSplashVersion(getVersion());
                       }
                       dialog.dismiss();
                   }
               });

        final AlertDialog dialog = builder.create();
        dialog.show();

        if (always) {
            checkBox.setVisibility(View.GONE);
        }

        final Button okButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
        if (okButton != null) {
            okButton.setFocusableInTouchMode(true);
            okButton.requestFocus();
        }

        if (dialog.getWindow() != null) {
            int width;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                // Modern Android 11+ (and Android 15) window metrics calculation
                android.view.WindowMetrics windowMetrics = getWindowManager().getCurrentWindowMetrics();
                android.graphics.Rect bounds = windowMetrics.getBounds();
                width = (int) (bounds.width() * 0.75);
            } else {
                // Fallback for older Android versions
                width = (int) (getResources().getDisplayMetrics().widthPixels * 0.75);
            }
            dialog.getWindow().setLayout(width, WindowManager.LayoutParams.WRAP_CONTENT);
        }
    }

    private boolean hasFlash() {
        final Camera camera = mCameraManager.getCamera();
        if (camera == null) {
            return false;
        }

        return camera.getCameraInfo().hasFlashUnit();
    }

    private boolean hasFlashLevels() {
        final Camera camera = mCameraManager.getCamera();
        if (camera == null) {
            return false;
        }
        final CameraInfo cameraInfo = camera.getCameraInfo();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return cameraInfo.getMaxTorchStrengthLevel() > 1;
        }
        return false;
    }

    @ExperimentalCamera2Interop
    private void toggleFlashlight(final boolean enable) {
        mHandler.removeMessages(MSG_FLASHLIGHT_OFF);
        final Camera camera = mCameraManager.getCamera();
        if (camera == null) {
            return;
        }

        final CameraControl cameraControl = camera.getCameraControl();
        final CameraInfo cameraInfo = camera.getCameraInfo();
        if (!cameraInfo.hasFlashUnit()) {
            return;
        }

        final float flashlight = mSettingsManager.getFlashlight();
        if (!enable || flashlight == 0.0f) {
            cameraControl.enableTorch(false);
        } else {
            int maxStrength = 1;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                maxStrength = cameraInfo.getMaxTorchStrengthLevel();
            }

            if (maxStrength > 1) {
                final int strength = (int)(maxStrength * flashlight);
                cameraControl.setTorchStrengthLevel(strength == 0 ? 1 : strength);
            }

            cameraControl.enableTorch(true);
        }
    }

    @Override
    public boolean handleMessage(@NonNull final Message msg) {
        switch (msg.what) {
            case MSG_FLASHLIGHT_OFF:
                toggleFlashlight(false);
                return true;
            case MSG_IMAGE_OFF:
                mImageView.setVisibility(View.GONE);
                mImageView.recycle();
                mIsProcessing = false;
                return true;
        }
        return false;
    }
}
