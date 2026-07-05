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
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import com.google.common.util.concurrent.ListenableFuture;

public class AndroidCameraManagerFactory {
    
    public static CameraManager create(@NonNull AppCompatActivity activity, @NonNull PreviewView viewFinder) {
        IProcessCameraProvider productionWrapper = new IProcessCameraProvider() {
            @NonNull
            @Override
            public ListenableFuture<ProcessCameraProvider> getInstance(@NonNull Context context) {
                return ProcessCameraProvider.getInstance(context);
            }
        };
        
        return new CameraManager(
                activity, 
                viewFinder, 
                productionWrapper, 
                ContextCompat.getMainExecutor(activity)
        );
    }
}
