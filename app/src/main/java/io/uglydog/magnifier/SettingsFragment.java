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

import androidx.preference.ListPreference;
import androidx.preference.PreferenceFragmentCompat;

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
}
