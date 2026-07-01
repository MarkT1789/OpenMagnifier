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

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.davemorrissey.labs.subscaleview.ImageSource;
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView;
import com.davemorrissey.labs.subscaleview.decoder.DecoderFactory;
import com.davemorrissey.labs.subscaleview.decoder.ImageRegionDecoder;
import com.davemorrissey.labs.subscaleview.decoder.SkiaImageRegionDecoder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ShareActivity extends AppCompatActivity implements InputHandler.InputActions {

    private static final String TAG = ShareActivity.class.getSimpleName();
    private static final String FILE = "shared_image.jpg";

    private SubsamplingScaleImageView mImageView;
    private SettingsProvider mSettingsProvider;
    private TextReader mTextReader;
    private TextReaderOverlay mTextReaderOverlay;

    private final ExecutorService mExecutorService = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        Log.i(TAG, "onCreate: shared activity");

        setupWindow();

        mImageView = findViewById(R.id.ivLastCapture);
        if (mImageView == null) {
            Log.e(TAG, "onCreate: no image view");
            finish();
            return;
        }
        mImageView.setMinimumScaleType(SubsamplingScaleImageView.SCALE_TYPE_CENTER_CROP);
        mImageView.setVisibility(View.VISIBLE);
        mImageView.setRegionDecoderFactory(new DecoderFactory<ImageRegionDecoder>() {
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
        mImageView.setMinimumTileDpi(120);
        mImageView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                mTextReader.stop();
                mTextReaderOverlay.showCopyright(false);
                mTextReaderOverlay.clearOverlay();
                return true;
            }
        });

        mSettingsProvider = new SettingsProvider(this);
        mTextReaderOverlay = findViewById(R.id.textOverlayView);
        mTextReaderOverlay.setSettingsProvider(mSettingsProvider);
        if (mTextReaderOverlay == null) {
            Log.e(TAG, "onCreate: no text overlay view");
            finish();
            return;
        }

        handleIntent(getIntent());
    }

    @Override
    protected void onNewIntent(final Intent intent) {
        super.onNewIntent(intent);

        Log.i(TAG, "onNewIntent: shared activity");

        setIntent(intent);
        handleIntent(intent);
    }

    private void handleIntent(final Intent intent) {
        if (intent == null) {
            Log.e(TAG, "handleIntent: no intent");
            finish();
            return;
        }

        final String action = intent.getAction();
        final String type = intent.getType();

        if (type == null || action == null) {
            Log.e(TAG, "handleIntent: bad intent");
            finish();
            return;
        }

        Log.i(TAG, "handleIntent: type=" + type + " action=" + action);

        if (Intent.ACTION_SEND.equals(action)) {
            if (type.startsWith("image/")) {
                Log.i(TAG, "handleIntent: single image");
                handleSingleImage(intent);
            } else {
                Log.e(TAG, "handleIntent: unknown type: " + type);
                finish();
            }
        } else {
            Log.e(TAG, "handleIntent: unknown action: " + action);
            finish();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (BuildConfig.DEBUG) Log.d(TAG, "onPause");

        if (mTextReader != null) {
            mTextReader.stop();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (BuildConfig.DEBUG) Log.d(TAG, "onResume");

        if (isFinishing() || isDestroyed()) {
            return;
        }

        if (mSettingsProvider != null) {
            mSettingsProvider.reload();
        }

        if (mTextReader != null) {
            mTextReader.start();
        }
    }

    @Override
    protected void onDestroy() {
        mExecutorService.shutdown();

        if (mTextReaderOverlay != null) {
            mTextReaderOverlay.close();
        }

        if (mTextReader != null) {
            mTextReader.destroy();
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
            Log.e(TAG, "error deleting cached photo", e);
        }

        super.onDestroy();
    }

    private void handleSingleImage(@NonNull final Intent intent) {
        final Uri imageUri;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            imageUri = intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri.class);
        } else {
            imageUri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
        }

        if (imageUri == null) {
            Log.e(TAG, "handleSingleImage: no image uri");
            finish();
            return;
        }

        mExecutorService.execute(new Runnable() {
            @Override
            public void run() {
                final boolean success = getFileFromContentUri(ShareActivity.this, imageUri);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (isFinishing() || isDestroyed()) return;

                        if (success) {
                            mTextReader = new TextReader(ShareActivity.this, mImageView, mTextReaderOverlay, FILE, mSettingsProvider);
                            mImageView.setImage(ImageSource.uri(Uri.fromFile(new File(getCacheDir(), FILE))));
                            mTextReader.start();
                        } else {
                            Log.e(TAG, "getFileFromContentUri failed");
                            finish();
                        }
                    }
                });
            }
        });
    }

    private void setupWindow() {
        EdgeToEdge.enable(this);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            getWindow().getAttributes().layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
        }

        final WindowInsetsControllerCompat controller = new WindowInsetsControllerCompat(getWindow(), getWindow().getDecorView());

        controller.hide(WindowInsetsCompat.Type.statusBars() | WindowInsetsCompat.Type.navigationBars());
        controller.setSystemBarsBehavior(WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
    }

    private boolean getFileFromContentUri(@NonNull final Context context, @NonNull final Uri contentUri) {
        Log.i(TAG, "getFileFromContentUri " + FILE);
        final File tempFile = new File(context.getCacheDir(), FILE);
        InputStream inputStream = null;
        FileOutputStream outputStream = null;
        boolean success = false;

        try {
            inputStream = context.getContentResolver().openInputStream(contentUri);
            if (inputStream == null) {
                return false;
            }
            outputStream = new FileOutputStream(tempFile);

            final byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            success = true;
        } catch (Exception e) {
            Log.e(TAG, "getFileFromContentUri: exception", e);
            success = false;
        } finally {
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (IOException e) {
                    Log.e(TAG, "getFileFromContentUri: error closing output stream", e);
                }
            }
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    Log.e(TAG, "getFileFromContentUri: error closing input stream", e);
                }
            }
        }
        return success;
    }

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
    public void onChangeBrightnessSetting(KeyEvent event) {}
    @Override
    public void onChangeColorFilterSetting(KeyEvent event) {}
    @Override
    public void onChangeContrastSetting(KeyEvent event) {}
    @Override
    public void onChangeFlashlightSetting(KeyEvent event) {}
    @Override
    public void onChangeSpeakSetting(KeyEvent event) {}
    @Override
    public void onChangePanSetting(KeyEvent event) {}
    @Override
    public void onChangeRotationSetting(KeyEvent event) {}
    @Override
    public void onChangeView() {}
    @Override
    public void onChangeZoomSetting(KeyEvent event) {}
    @Override
    public void onScrollViewport(KeyEvent event) {}
    @Override
    public boolean onVolumeChanged(KeyEvent event) {
        if (mImageView.getVisibility() != View.VISIBLE || mTextReader == null) {
            return false;
        }
        final int cmd = event.getKeyCode() == KeyEvent.KEYCODE_VOLUME_UP ? 0 : 1;
        return mTextReader.onVolumeChanged(cmd);
    }
    @Override
    public void onShowHelp() {}
    @Override
    public void onShowVersion() {}
}
