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
import android.graphics.Bitmap;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.graphics.PointF;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
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
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.ZoomState;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.lifecycle.LiveData;

import com.davemorrissey.labs.subscaleview.ImageSource;
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView;
import com.davemorrissey.labs.subscaleview.decoder.DecoderFactory;
import com.davemorrissey.labs.subscaleview.decoder.ImageRegionDecoder;
import com.davemorrissey.labs.subscaleview.decoder.SkiaImageRegionDecoder;

import java.io.File;

@ExperimentalCamera2Interop
public class MainActivity extends AppCompatActivity implements GestureListener.GestureActions, ScaleListener.ScaleActions, InputHandler.InputActions {
    private static final String TAG = MainActivity.class.getSimpleName();
    private static final int CAMERA_PERMISSION_CODE = 100;

    private CameraManager mCameraManager;
    private SettingsProvider mSettingsProvider;
    private SubsamplingScaleImageView mImageView;
    private GestureDetector mGestureDetector;
    private ScaleGestureDetector mScaleGestureDetector;

    private final Paint mFilterPaint = new Paint();
    private final ColorMatrix mColorMatrix = new ColorMatrix();
    private final ColorMatrix mGrayscaleMatrix = new ColorMatrix();

    private boolean mIsProcessing = false;

    /*
     * Lifecycle
     */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setupWindow();

        mImageView = findViewById(R.id.ivLastCapture);
        mImageView.setMinimumScaleType(SubsamplingScaleImageView.SCALE_TYPE_CENTER_CROP);

        mSettingsProvider = new SettingsProvider(this);
        mCameraManager = new CameraManager(this, findViewById(R.id.viewFinder));
        mGestureDetector = new GestureDetector(this, new GestureListener(this));
        mScaleGestureDetector = new ScaleGestureDetector(this, new ScaleListener(this));

        showSplashDialog(false);
        updateFilters();

        mImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (BuildConfig.DEBUG) Log.d(TAG, "onClick");
                onToggleMode();
            }
        });

        if (allPermissionsGranted()) {
            startCameraSequence();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_CODE);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (BuildConfig.DEBUG) Log.d(TAG, "onResume");

        mSettingsProvider.reload();

        if (mImageView.getVisibility() == View.VISIBLE) {
            mImageView.setOrientation(mSettingsProvider.getRotation());
        }

        final Camera camera = mCameraManager.getCamera();
        if (camera != null) {
            final float savedZoom = mSettingsProvider.getZoom();
            camera.getCameraControl().setZoomRatio(savedZoom);
        }

        updateFilters();
    }

    @Override
    protected void onDestroy() {
        if (mCameraManager != null) {
            mCameraManager.stopCamera();
        }
        if (mImageView != null) {
            mImageView.recycle();
        }
        try {
            final File photoFile = new File(getCacheDir(), "enhanced_text.jpg");
            if (photoFile.exists()) {
                photoFile.delete();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error deleting cached photo", e);
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
        if (BuildConfig.DEBUG) Log.d(TAG, "onScale " + scale);
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
                ToastHelper.show(this, getString(R.string.toast_view_live_zoom, next));
            }
        }
    }

    /*
     * InputHandler.InputAction
     */

    @Override
    public void onChangeBrightnessSetting(@NonNull final KeyEvent event) {
        if (mImageView.getVisibility() == View.VISIBLE) {
            final int id = getNextString(R.array.brightness_values, String.valueOf(mSettingsProvider.getBrightness()), !event.isShiftPressed());
            final String value = getStringItem(R.array.brightness_values, id);
            final String key = getStringItem(R.array.filter_entries, id);
            mSettingsProvider.setBrightness(Float.parseFloat(value));
            ToastHelper.show(this, getString(R.string.toast_brightness, key));
            updateFilters();
        } else {
            ToastHelper.show(this, getString(R.string.toast_brightness_disabled));
        }
    }

    @Override
    public void onChangeColorFilterSetting(@NonNull final KeyEvent event) {
        if (mImageView.getVisibility() == View.VISIBLE) {
            final int id = getNextString(R.array.color_values, String.valueOf(mSettingsProvider.getColor()), !event.isShiftPressed());
            final String value = getStringItem(R.array.color_values, id);
            final String key = getStringItem(R.array.color_entries, id);
            mSettingsProvider.setColor(Integer.parseInt(value));
            ToastHelper.show(this, getString(R.string.toast_color, key));
            updateFilters();
        } else {
            ToastHelper.show(this, getString(R.string.toast_color_disabled));
        }
    }

    @Override
    public void onChangeContrastSetting(@NonNull final KeyEvent event) {
        if (mImageView.getVisibility() == View.VISIBLE) {
            final int id = getNextString(R.array.contrast_values, String.valueOf(mSettingsProvider.getContrast()), !event.isShiftPressed());
            final String value = getStringItem(R.array.contrast_values, id);
            final String key = getStringItem(R.array.filter_entries, id);
            mSettingsProvider.setContrast(Float.parseFloat(value));
            ToastHelper.show(this, getString(R.string.toast_contrast, key));
            updateFilters();
        } else {
            ToastHelper.show(this, getString(R.string.toast_contrast_disabled));
        }
    }

    @Override
    public void onChangePanSetting(@NonNull final KeyEvent event) {
        if (mImageView.getVisibility() == View.VISIBLE) {
            final String current = String.valueOf(event.getKeyCode() == KeyEvent.KEYCODE_X ?
                mSettingsProvider.getDx() : mSettingsProvider.getDy());
            final int id = getNextString(R.array.pan_values, current, !event.isShiftPressed());
            final float value = Float.parseFloat(getStringItem(R.array.pan_values, id));

            if (event.getKeyCode() == KeyEvent.KEYCODE_X) {
                mSettingsProvider.setDx(value);
                ToastHelper.show(this, getString(R.string.toast_scroll_horizontal, (int)(value * 100)));
            } else {
                mSettingsProvider.setDy(value);
                ToastHelper.show(this, getString(R.string.toast_scroll_vertical, (int)(value * 100)));
            }
        } else {
            if (event.getKeyCode() == KeyEvent.KEYCODE_X) {
                ToastHelper.show(this, getString(R.string.toast_scroll_horizontal_disabled));
            } else {
                ToastHelper.show(this, getString(R.string.toast_scroll_vertical_disabled));
            }
        }
    }

    @Override
    public void onChangeRotationSetting(@NonNull final KeyEvent event) {
        if (mImageView.getVisibility() == View.VISIBLE) {
            final int id = getNextString(R.array.rotation_values, String.valueOf(mSettingsProvider.getRotation()), !event.isShiftPressed());
            final String value = getStringItem(R.array.rotation_values, id);
            final int rotation = Integer.parseInt(value);
            mSettingsProvider.setRotation(rotation);
            mImageView.setOrientation(rotation);
            ToastHelper.show(this, getString(R.string.toast_rotation, rotation));
        } else {
            ToastHelper.show(this, getString(R.string.toast_rotation_disabled));
        }
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
            ToastHelper.show(this, getString(R.string.toast_view_freeze_frame_zoom, scale / mImageView.getMinScale()));
        } else {
            final Camera camera = mCameraManager.getCamera();
            final LiveData<ZoomState> zoomStateLiveData = camera.getCameraInfo().getZoomState();
            if (zoomStateLiveData.getValue() != null) {
                final ZoomState zoomState = zoomStateLiveData.getValue();
                final float maxZoom = zoomState.getMaxZoomRatio();
                final int id = getNextString(R.array.zoom_values, String.valueOf(mSettingsProvider.getZoom()), !event.isShiftPressed());
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
                mSettingsProvider.setZoom(zoom);
                ToastHelper.show(this, getString(R.string.toast_view_live_zoom, zoom));
                camera.getCameraControl().setZoomRatio(zoom);
            }
        }
    }

    @Override
    public void onScrollViewport(@NonNull final KeyEvent event) {
        if (mImageView.getVisibility() != View.VISIBLE) {
            ToastHelper.show(MainActivity.this, getString(R.string.toast_scroll_disabled));
            return;
        }

        final PointF center = mImageView.getCenter();
        final float scale = mImageView.getScale();
        if (center == null || scale == 0) return;

        final float dx = (mImageView.getWidth() * mSettingsProvider.getDx()) / scale;
        final float dy = (mImageView.getHeight() * mSettingsProvider.getDy()) / scale;

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
    public void onShowHelp() {
        showSplashDialog(true);
    }

    @Override
    public void onShowVersion() {
        ToastHelper.show(this, getString(R.string.version_known, getVersion()));
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
            mImageView.setVisibility(View.GONE);
            mImageView.recycle();
            mIsProcessing = false;

            final Camera camera = mCameraManager.getCamera();
            final LiveData<ZoomState> zoomStateLiveData = camera.getCameraInfo().getZoomState();
            if (zoomStateLiveData.getValue() != null) {
                final ZoomState zoomState = zoomStateLiveData.getValue();
                final float zoom = zoomState.getZoomRatio();
                ToastHelper.show(this, getString(R.string.toast_view_live_zoom, zoom));
            }
            //FIXME ToastHelper.show(this, getString(R.string.toast_view_live_zoom, mSettingsProvider.getZoom()));
            return true;
        }

        if (!mIsProcessing) {
            ToastHelper.show(this, getString(R.string.toast_view_freeze_frame));
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
        final float contrast = mSettingsProvider.getContrast();
        final float brightness = mSettingsProvider.getBrightness();
        final int color = mSettingsProvider.getColor();

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
                final float savedZoom = mSettingsProvider.getZoom();
                camera.getCameraControl().setZoomRatio(savedZoom);
                ToastHelper.show(MainActivity.this, getString(R.string.toast_view_live_zoom, savedZoom));
            }
        });
    }

    private void captureToView() {
        mIsProcessing = true;
        mImageView.recycle();
        final File photoFile = new File(getCacheDir(), "enhanced_text.jpg");

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
                        mImageView.setOrientation(mSettingsProvider.getRotation());
                        mImageView.setImage(ImageSource.uri(Uri.fromFile(photoFile)));
                        mImageView.setMinimumTileDpi(120);
                        mIsProcessing = false;
                    }
                });
            }

            @Override
            public void onError(@NonNull ImageCaptureException e) {
                Log.e(TAG, "Photo capture failed", e);
                mIsProcessing = false;
            }
        });
    }

    private void showSplashDialog(final boolean always) {
        if (!always && getVersion().equals(mSettingsProvider.getSplashVersion())) {
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
                           mSettingsProvider.setSplashVersion(getVersion());
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
}
