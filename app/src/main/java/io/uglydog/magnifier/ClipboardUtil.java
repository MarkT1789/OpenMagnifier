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

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import java.util.List;
import java.util.Map;

public class ClipboardUtil {
    private static final String TAG = ClipboardUtil.class.getSimpleName();
    private static final String LABEL = "OCR";
    
    private final Context mContext;
    private final ToastManager mToastManager;
    private final ClipboardService mClipboardService;

    // Interface wrapper for Android system clipboard to allow mocking in tests
    public interface ClipboardService {
        void setPrimaryClip(@NonNull String label, @NonNull String text) throws Exception;
    }

    public ClipboardUtil(@NonNull final Context context, @NonNull final ToastManager toastManager) {
        this(context, toastManager, new ClipboardService() {
            @Override
            public void setPrimaryClip(@NonNull String label, @NonNull String text) {
                ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
                if (clipboard != null) {
                    ClipData clip = ClipData.newPlainText(label, text);
                    clipboard.setPrimaryClip(clip);
                }
            }
        });
    }

    // Constructor injection for testing
    @VisibleForTesting
    ClipboardUtil(@NonNull final Context context, 
                  @NonNull final ToastManager toastManager, 
                  @NonNull final ClipboardService clipboardService) {
        mContext = context.getApplicationContext();
        mToastManager = toastManager;
        mClipboardService = clipboardService;
    }

    public void copy(@Nullable List<String> keys, @Nullable Map<String, String> textMap) {
        String formattedText = formatText(keys, textMap);
        copyToClipboard(LABEL, formattedText);
    }

    /**
     * Pure Java helper function responsible solely for text formatting.
     * Easy to unit test directly without any Android framework objects.
     */
    @NonNull
    @VisibleForTesting
    static String formatText(@Nullable List<String> keys, @Nullable Map<String, String> textMap) {
        final StringBuilder sb = new StringBuilder();

        if (keys != null && textMap != null) {
            for (String key : keys) {
                if (key != null) {
                    final String value = textMap.get(key);
                    if (value != null && !value.isEmpty()) {
                        if (sb.length() > 0) {
                            sb.append("\n");
                        }
                        sb.append(value);
                    }
                }
            }
        }

        return sb.toString();
    }

    private void copyToClipboard(@NonNull final String label, @NonNull final String str) {
        try {
            mClipboardService.setPrimaryClip(label, str);

            if (getBuildVersion() < Build.VERSION_CODES.TIRAMISU) {
                mToastManager.show(mContext, mContext.getString(R.string.toast_copy_to_clipboard));
            }
        } catch (Exception e) {
            Logger.e(TAG, "copyToClipboard: failed: " + e.getMessage());
        }
    }

    @VisibleForTesting
    int getBuildVersion() {
        return Build.VERSION.SDK_INT;
    }
}
