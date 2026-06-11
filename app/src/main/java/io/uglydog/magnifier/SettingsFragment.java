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
import android.content.pm.PackageManager;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.Insets;
import androidx.core.view.OnApplyWindowInsetsListener;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.preference.ListPreference;
import androidx.preference.PreferenceFragmentCompat;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SettingsFragment extends PreferenceFragmentCompat {
    private static final String TAG = SettingsFragment.class.getSimpleName();

    @Override
    public void onCreatePreferences(final Bundle savedInstanceState, final String rootKey) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey);

        final PackageManager pm = requireContext().getPackageManager();
        final boolean hasFlash = pm.hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH);

        final ListPreference flashlightPreference = findPreference("flashlight_setting");
        if (flashlightPreference == null) {
            return;
        }
        if (!hasFlash) {
            flashlightPreference.setEnabled(false);
        } else {
            if (!isFlashAdjustable(getContext())) {
                int[] entries = {4, 3, 2, 1};
                deleteEntries(flashlightPreference, entries);
            }
        }
        if (!isGooglePlayDevice(getContext())) {
            Log.i(TAG, "onCreatePreference: not Google Play device");
            final ListPreference sourcePreference = findPreference("source_setting");
            final ListPreference destPreference = findPreference("dest_setting");
            if (sourcePreference != null) {
                sourcePreference.setEnabled(false);
            }
            if (destPreference != null) {
                destPreference.setEnabled(false);
            }
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        final ViewGroup listView = (ViewGroup) getListView();
        if (listView != null) {
            listView.setClipToPadding(false);

            ViewCompat.setOnApplyWindowInsetsListener(listView, new OnApplyWindowInsetsListener() {
                @NonNull
                @Override
                public WindowInsetsCompat onApplyWindowInsets(@NonNull View v, @NonNull WindowInsetsCompat windowInsets) {
                    final Insets systemBars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
                    v.setPadding(
                        v.getPaddingLeft(),
                        v.getPaddingTop(),
                        v.getPaddingRight(),
                        systemBars.bottom
                    );
                    return windowInsets;
                }
            });
        }
    }

    private void deleteEntries(ListPreference listPreference, int[] indexesToRemove) {
        final CharSequence[] entriesArray = listPreference.getEntries();
        final CharSequence[] entryValuesArray = listPreference.getEntryValues();
        if (entriesArray == null || entryValuesArray == null) {
            return;
        }

        final List<CharSequence> entries = new ArrayList<>(Arrays.asList(entriesArray));
        final List<CharSequence> entryValues = new ArrayList<>(Arrays.asList(entryValuesArray));

        if (entries.size() > 5) {
            final String currentValue = listPreference.getValue();
            boolean currentDeleted = false;

            for (int index : indexesToRemove) {
                if (currentValue != null && currentValue.equals(entryValues.get(index).toString())) {
                    currentDeleted = true;
                }
                entries.remove(index);
                entryValues.remove(index);
            }

            listPreference.setEntries(entries.toArray(new CharSequence[0]));
            listPreference.setEntryValues(entryValues.toArray(new CharSequence[0]));

            if (currentDeleted) {
                listPreference.setValue(entryValues.get(0).toString());
            }
        }
    }

    private boolean isFlashAdjustable(final Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return false;
        }

        try {
            final CameraManager cameraManager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
            if (cameraManager == null) {
                return false;
            }

            for (final String cameraId : cameraManager.getCameraIdList()) {
                final CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(cameraId);

                final Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);
                if (facing == null || facing != CameraCharacteristics.LENS_FACING_BACK) {
                    continue;
                }

                final Boolean hasFlash = characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE);
                if (hasFlash == null || !hasFlash) {
                    continue;
                }

                final Integer maxStrength = characteristics.get(CameraCharacteristics.FLASH_INFO_STRENGTH_MAXIMUM_LEVEL);
                if (maxStrength != null && maxStrength > 1) {
                    return true;
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "Error checking flash adjustability", e);
        }

        return false;
    }

    private boolean isGooglePlayDevice(final Context context) {
        final GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        final int resultCode = apiAvailability.isGooglePlayServicesAvailable(context);

        return resultCode == ConnectionResult.SUCCESS;
    }
}
