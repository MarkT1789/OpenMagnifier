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

import android.hardware.camera2.CaptureRequest;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.camera2.interop.Camera2Interop;
import androidx.camera.camera2.interop.ExperimentalCamera2Interop;
import androidx.camera.core.AspectRatio;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;

import java.io.File;
import java.util.concurrent.ExecutionException;

@ExperimentalCamera2Interop
public class CameraManager {
    private final AppCompatActivity mActivity;
    private final PreviewView mViewFinder;
    private ImageCapture mImageCapture;
    private Camera mCamera;

    public interface OnCameraReadyListener {
        void onCameraReady(Camera camera);
    }

    public CameraManager(@NonNull final AppCompatActivity activity, @NonNull final PreviewView viewFinder) {
        mActivity = activity;
        mViewFinder = viewFinder;
    }

    public void startCamera(@NonNull final OnCameraReadyListener listener) {
        final ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
                ProcessCameraProvider.getInstance(mActivity);

        cameraProviderFuture.addListener(new Runnable() {
            @Override
            public void run() {
                try {
                    final ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                    bindUseCases(cameraProvider);
                    listener.onCameraReady(mCamera);
                } catch (ExecutionException | InterruptedException e) {
                    Log.e("CameraManager", "Camera initialization failed", e);
                }
            }
        }, ContextCompat.getMainExecutor(mActivity));
    }

    private void bindUseCases(@NonNull final ProcessCameraProvider cameraProvider) {
        final Preview.Builder previewBuilder = new Preview.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_16_9);

        final Camera2Interop.Extender<Preview> previewExtender = new Camera2Interop.Extender<>(previewBuilder);
        previewExtender.setCaptureRequestOption(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);

        final Preview preview = previewBuilder.build();
        preview.setSurfaceProvider(mViewFinder.getSurfaceProvider());

        final ImageCapture.Builder captureBuilder = new ImageCapture.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_16_9)
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY);

        mImageCapture = captureBuilder.build();
        cameraProvider.unbindAll();
        mCamera = cameraProvider.bindToLifecycle(mActivity, CameraSelector.DEFAULT_BACK_CAMERA, preview, mImageCapture);
    }

    public void takePhoto(@NonNull final File file, @NonNull final ImageCapture.OnImageSavedCallback callback) {
        if (mImageCapture == null) return;
        final ImageCapture.OutputFileOptions options = new ImageCapture.OutputFileOptions.Builder(file).build();
        mImageCapture.takePicture(options, ContextCompat.getMainExecutor(mActivity), callback);
    }

    public Camera getCamera() {
        return mCamera;
    }

    public void stopCamera() {
        final ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
                ProcessCameraProvider.getInstance(mActivity);

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                cameraProvider.unbindAll();
                // This ensures the camera hardware is released immediately
                mCamera = null;
            } catch (ExecutionException | InterruptedException e) {
                Log.e("CameraManager", "Error stopping camera", e);
            }
        }, ContextCompat.getMainExecutor(mActivity));
    }
}
