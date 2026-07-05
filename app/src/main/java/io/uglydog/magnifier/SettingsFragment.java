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

import android.os.Bundle;
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SettingsFragment extends PreferenceFragmentCompat {
    private static final String TAG = SettingsFragment.class.getSimpleName();

    // Inject the helper via the factory
    private final ISettingsHelper settingsHelper = AndroidSettingsFragmentFactory.getSettingsHelper();

    @Override
    public void onCreatePreferences(final Bundle savedInstanceState, final String rootKey) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey);

        final ListPreference flashlightPreference = findPreference("flashlight_setting");
        if (flashlightPreference != null) {
            if (!settingsHelper.hasFlash(requireContext())) {
                flashlightPreference.setEnabled(false);
            } else if (!settingsHelper.isFlashAdjustable(requireContext())) {
                deleteEntries(flashlightPreference, new int[]{4, 3, 2, 1});
            }
        }

        if (!settingsHelper.isGooglePlayDevice(requireContext())) {
            Logger.i(TAG, "onCreatePreference: not Google Play device");
            final ListPreference sourcePreference = findPreference("source_setting");
            final ListPreference destPreference = findPreference("dest_setting");
            if (sourcePreference != null) sourcePreference.setEnabled(false);
            if (destPreference != null) destPreference.setEnabled(false);
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
}
